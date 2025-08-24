package com.springwarden.auth.service;

import com.springwarden.auth.entity.User;
import com.springwarden.auth.exception.EmailAlreadyExistsException;
import com.springwarden.auth.exception.InvalidCredentialsException;
import com.springwarden.auth.exception.InvalidTokenException;
import com.springwarden.auth.exception.ResourceNotFoundException;
import com.springwarden.auth.repository.UserRepository;
import com.springwarden.common.dto.*;
import com.springwarden.common.event.UserEvent;
import com.springwarden.common.model.Role;
import com.springwarden.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final long accessExpiration;

    @Autowired
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtUtil jwtUtil,
                       KafkaTemplate<String, Object> kafkaTemplate,
                       @Qualifier("accessExpiration") long accessExpiration) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.kafkaTemplate = kafkaTemplate;
        this.accessExpiration = accessExpiration;
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Invalid email or password provided.");
        }

        User user = (User) authentication.getPrincipal();
        Set<String> roleNames = user.getRoles().stream().map(Role::name).collect(Collectors.toSet());

        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), roleNames);
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        return new AuthResponse(accessToken, refreshToken, user.getEmail(), roleNames, accessExpiration);
    }

    @Transactional
    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email '" + request.email() + "' is already in use.");
        }

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName(),
                Set.of(Role.USER) // Default role
        );

        User savedUser = userRepository.save(user);
        UserDto userDto = convertToDto(savedUser);

        // Publish user creation event to Kafka
        UserEvent event = new UserEvent("CREATED", userDto, System.currentTimeMillis());
        kafkaTemplate.send("user-events", event);

        return userDto;
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new InvalidTokenException("Refresh token is invalid or expired.");
        }

        String email = jwtUtil.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User associated with refresh token not found."));

        Set<String> roleNames = user.getRoles().stream().map(Role::name).collect(Collectors.toSet());

        String newAccessToken = jwtUtil.generateAccessToken(email, roleNames);
        // Optionally, generate a new refresh token for rotation
        String newRefreshToken = jwtUtil.generateRefreshToken(email);

        return new AuthResponse(newAccessToken, newRefreshToken, email, roleNames, accessExpiration);
    }

    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    @Transactional(readOnly = true)
    public UserDto getUserFromToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            throw new InvalidTokenException("Access token is invalid or expired.");
        }
        String email = jwtUtil.extractUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User from token not found."));

        return convertToDto(user);
    }

    private UserDto convertToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRoles().stream().map(Role::name).collect(Collectors.toSet()),
                user.isEnabled()
        );
    }
}