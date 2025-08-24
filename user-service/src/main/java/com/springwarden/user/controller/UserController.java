package com.springwarden.user.controller;

import com.springwarden.common.dto.UserDto;
import com.springwarden.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // ----- General Endpoints (accessible by USER, MANAGER, ADMIN) -----

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserDto> getCurrentUserProfile(@RequestHeader("X-User-Email") String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    // ----- Admin-only Endpoints (protected by ADMIN role in API Gateway) -----

    @GetMapping("/admin/all")
    public ResponseEntity<List<UserDto>> getAllUsersForAdmin() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/admin/active")
    public ResponseEntity<List<UserDto>> getActiveUsersForAdmin() {
        return ResponseEntity.ok(userService.getActiveUsers());
    }
}