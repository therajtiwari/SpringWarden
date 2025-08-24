package com.springwarden.user.service;

import com.springwarden.common.dto.UserDto;
import com.springwarden.common.model.Role;
import com.springwarden.user.entity.UserProfile;
import com.springwarden.user.exception.ResourceNotFoundException;
import com.springwarden.user.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserService {

    @Autowired
    private UserProfileRepository userProfileRepository;

    public List<UserDto> getAllUsers() {
        return userProfileRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public UserDto getUserById(Long id) {
        return userProfileRepository.findById(id)
                .map(this::convertToDto)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found with ID: " + id));
    }

    public UserDto getUserByEmail(String email) {
        return userProfileRepository.findByEmail(email)
                .map(this::convertToDto)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found with email: " + email));
    }

    public List<UserDto> getActiveUsers() {
        return userProfileRepository.findByEnabledTrue().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private UserDto convertToDto(UserProfile profile) {
        Set<String> roleNames = profile.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.toSet());

        return new UserDto(
                profile.getId(),
                profile.getEmail(),
                profile.getFirstName(),
                profile.getLastName(),
                roleNames,
                profile.isEnabled()
        );
    }
}