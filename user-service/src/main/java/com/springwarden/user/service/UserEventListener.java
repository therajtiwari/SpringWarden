package com.springwarden.user.service;

import com.springwarden.common.dto.UserDto;
import com.springwarden.common.event.UserEvent;
import com.springwarden.common.model.Role;
import com.springwarden.user.entity.UserProfile;
import com.springwarden.user.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);
    private final UserProfileRepository userProfileRepository;

    @Autowired
    public UserEventListener(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @KafkaListener(topics = "user-events", groupId = "user-service-group")
    @Transactional
    public void handleUserEvent(UserEvent event) {
        log.info("Received user event: Type='{}', UserEmail='{}'", event.eventType(), event.user().email());
        UserDto userDto = event.user();

        switch (event.eventType()) {
            case "CREATED" -> createUserProfile(userDto);
            case "UPDATED" -> updateUserProfile(userDto);
            case "DELETED" -> deleteUserProfile(userDto.id());
            default -> log.warn("Received unknown event type: {}", event.eventType());
        }
    }

    private void createUserProfile(UserDto userDto) {
        if (userProfileRepository.existsById(userDto.id())) {
            log.warn("Attempted to create a user profile that already exists. ID: {}", userDto.id());
            // This might happen in at-least-once delivery scenarios. We can treat it as an update.
            updateUserProfile(userDto);
            return;
        }

        Set<Role> roles = userDto.roles().stream().map(Role::valueOf).collect(Collectors.toSet());
        UserProfile profile = new UserProfile(
                userDto.id(),
                userDto.email(),
                userDto.firstName(),
                userDto.lastName(),
                roles,
                userDto.enabled()
        );

        userProfileRepository.save(profile);
        log.info("Successfully created user profile for email: {}", profile.getEmail());
    }

    private void updateUserProfile(UserDto userDto) {
        UserProfile profile = userProfileRepository.findById(userDto.id())
                .orElseGet(() -> {
                    // Handle case where an UPDATE event arrives before CREATE (rare but possible)
                    log.warn("Received UPDATE for non-existent user profile. Creating new profile. ID: {}", userDto.id());
                    return new UserProfile();
                });

        profile.setId(userDto.id());
        profile.setEmail(userDto.email());
        profile.setFirstName(userDto.firstName());
        profile.setLastName(userDto.lastName());
        profile.setEnabled(userDto.enabled());
        profile.setRoles(userDto.roles().stream().map(Role::valueOf).collect(Collectors.toSet()));

        userProfileRepository.save(profile);
        log.info("Successfully updated user profile for email: {}", profile.getEmail());
    }

    private void deleteUserProfile(Long userId) {
        if (userProfileRepository.existsById(userId)) {
            userProfileRepository.deleteById(userId);
            log.info("Successfully deleted user profile with ID: {}", userId);
        } else {
            log.warn("Attempted to delete a non-existent user profile. ID: {}", userId);
        }
    }
}