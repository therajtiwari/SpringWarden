package com.springwarden.common.dto;
import java.util.Set;
public record UserDto(Long id, String email, String firstName, String lastName, Set<String> roles, boolean enabled) {}