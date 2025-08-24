// exception/ResourceNotFoundException.java
package com.springwarden.user.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}