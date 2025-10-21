package com.example.demo.presentation.controller;

import com.example.demo.application.dto.UserDto;
import com.example.demo.application.mapper.UserMapper;
import com.example.demo.application.service.UserService;
import com.example.demo.domain.entity.User;
import com.example.demo.infrastructure.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Authentication operations.
 * Handles login, registration, and JWT token management.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    
    @Autowired
    public AuthController(AuthenticationManager authenticationManager,
                         UserService userService,
                         JwtTokenProvider jwtTokenProvider,
                         UserMapper userMapper) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userMapper = userMapper;
    }
    
    /**
     * User login endpoint.
     * @param loginRequest the login credentials
     * @return JWT token and user information
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(),
                    loginRequest.getPassword()
                )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            User user = (User) authentication.getPrincipal();
            String token = jwtTokenProvider.generateToken(user.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", userMapper.toDto(user));
            response.put("type", "Bearer");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid username or password"));
        }
    }
    
    /**
     * User registration endpoint.
     * @param registerRequest the registration data
     * @return JWT token and user information
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            // Check if username already exists
            if (userService.findByUsername(registerRequest.getUsername()).isPresent()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username already exists"));
            }
            
            // Check if email already exists
            if (userService.findByEmail(registerRequest.getEmail()).isPresent()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email already exists"));
            }
            
            // Create new user
            User user = new User(
                registerRequest.getUsername(),
                registerRequest.getEmail(),
                registerRequest.getPassword(),
                registerRequest.getFirstName(),
                registerRequest.getLastName()
            );
            
            User createdUser = userService.createUser(user);
            String token = jwtTokenProvider.generateToken(createdUser.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", userMapper.toDto(createdUser));
            response.put("type", "Bearer");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get current user information.
     * @param user the authenticated user
     * @return user information
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal User user) {
        UserDto userDto = userMapper.toDto(user);
        return ResponseEntity.ok(userDto);
    }
    
    /**
     * Refresh JWT token.
     * @param user the authenticated user
     * @return new JWT token
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@AuthenticationPrincipal User user) {
        String token = jwtTokenProvider.generateToken(user.getUsername());
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("type", "Bearer");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Change user password.
     * @param changePasswordRequest the password change data
     * @param user the authenticated user
     * @return success message
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest,
            @AuthenticationPrincipal User user) {
        try {
            // Verify current password
            if (!userService.verifyPassword(user, changePasswordRequest.getCurrentPassword())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Current password is incorrect"));
            }
            
            // Change password
            userService.changePassword(user.getId(), changePasswordRequest.getNewPassword());
            
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Inner class for login request.
     */
    public static class LoginRequest {
        private String username;
        private String password;
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }
    
    /**
     * Inner class for registration request.
     */
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
        
        public String getFirstName() {
            return firstName;
        }
        
        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }
        
        public String getLastName() {
            return lastName;
        }
        
        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }
    
    /**
     * Inner class for password change request.
     */
    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
        
        public String getCurrentPassword() {
            return currentPassword;
        }
        
        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }
        
        public String getNewPassword() {
            return newPassword;
        }
        
        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}
