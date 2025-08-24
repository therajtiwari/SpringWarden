package com.springwarden.common.event;
import com.springwarden.common.dto.UserDto;
public record UserEvent(String eventType, UserDto user, long timestamp) {}