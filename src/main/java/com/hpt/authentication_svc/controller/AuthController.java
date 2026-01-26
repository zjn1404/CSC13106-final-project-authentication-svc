package com.hpt.authentication_svc.controller;

import com.hpt.authentication_svc.dto.request.ChangePasswordRequest;
import com.hpt.authentication_svc.dto.request.GoogleAuthRequest;
import com.hpt.authentication_svc.dto.request.LoginRequest;
import com.hpt.authentication_svc.dto.request.RefreshTokenRequest;
import com.hpt.authentication_svc.dto.request.RegisterRequest;
import com.hpt.authentication_svc.dto.request.UpgradeAccountRequest;
import com.hpt.authentication_svc.dto.response.ApiResponse;
import com.hpt.authentication_svc.dto.response.AuthResponse;
import com.hpt.authentication_svc.dto.response.UserProfileResponse;
import com.hpt.authentication_svc.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * Google OAuth 2.0 login endpoint.
     * Exchanges the authorization code for tokens and authenticates/registers the user.
     *
     * Flow:
     * 1. Frontend redirects user to Google OAuth consent screen
     * 2. User grants permission, Google redirects back with authorization code
     * 3. Frontend sends the code to this endpoint
     * 4. Backend exchanges code for access token, gets user info, and returns JWT tokens
     *
     * Handles cases:
     * - New user: Creates account with Google provider
     * - Existing Google user: Logs in
     * - Existing local user with same email: Links Google account
     */
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithGoogle(@Valid @RequestBody GoogleAuthRequest request) {
        AuthResponse response = authService.loginWithGoogle(request);
        return ResponseEntity.ok(ApiResponse.success("Google login successful", response));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        authService.changePassword(email, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserProfileResponse profile = authService.getCurrentUserProfile(email);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authService.logout(token);
        }
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @PutMapping("/upgrade-account")
    public ResponseEntity<ApiResponse<UserProfileResponse>> upgradeAccount(
            @Valid @RequestBody UpgradeAccountRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        UserProfileResponse profile = authService.upgradeAccount(email, request);
        return ResponseEntity.ok(ApiResponse.success("Account upgraded successfully", profile));
    }
}

