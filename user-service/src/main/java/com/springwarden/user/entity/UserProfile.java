package com.springwarden.user.entity;

import com.springwarden.common.model.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "user_profiles", indexes = {
        @Index(name = "idx_userprofile_email", columnList = "email", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class UserProfile {

    @Id
    // CRITICAL DESIGN NOTE: There is no @GeneratedValue on purpose.
    // The ID is set explicitly from the UserEvent originating from the auth-service.
    // This makes auth-service the single source of truth for user identity and ensures
    // that the UserProfile ID matches the User ID in the auth-service.
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String firstName;
    private String lastName;

    @Enumerated(EnumType.STRING)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_profile_roles", joinColumns = @JoinColumn(name = "user_profile_id"))
    @Column(name = "role")
    private Set<Role> roles;

    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Custom Constructor
    public UserProfile(Long id, String email, String firstName, String lastName, Set<Role> roles, boolean enabled) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.roles = roles;
        this.enabled = enabled;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}