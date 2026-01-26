package com.hpt.authentication_svc.service;

import com.hpt.authentication_svc.dto.request.ChangePasswordRequest;
import com.hpt.authentication_svc.dto.request.GoogleAuthRequest;
import com.hpt.authentication_svc.dto.request.LoginRequest;
import com.hpt.authentication_svc.dto.request.RefreshTokenRequest;
import com.hpt.authentication_svc.dto.request.RegisterRequest;
import com.hpt.authentication_svc.dto.request.UpgradeAccountRequest;
import com.hpt.authentication_svc.dto.response.AuthResponse;
import com.hpt.authentication_svc.dto.response.GoogleTokenResponse;
import com.hpt.authentication_svc.dto.response.GoogleUserInfo;
import com.hpt.authentication_svc.dto.response.UserProfileResponse;
import com.hpt.authentication_svc.exception.BadRequestException;
import com.hpt.authentication_svc.exception.UnauthorizedException;
import com.hpt.authentication_svc.model.AccountType;
import com.hpt.authentication_svc.model.AuthProvider;
import com.hpt.authentication_svc.model.BlacklistedToken;
import com.hpt.authentication_svc.model.User;
import com.hpt.authentication_svc.repository.BlacklistedTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final GoogleOAuthService googleOAuthService;

    public AuthResponse register(RegisterRequest request) {
        if (userService.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .enabled(true)
                .build();

        user = userService.save(user);

        UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            throw new UnauthorizedException("Invalid email or password");
        }

        User user = userService.findByEmail(request.getEmail());
        UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtService.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        String email = jwtService.extractUsername(refreshToken);
        User user = userService.findByEmail(email);
        UserDetails userDetails = userService.loadUserByUsername(email);

        String newAccessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    public void changePassword(String email, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        User user = userService.findByEmail(email);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userService.save(user);
    }

    public UserProfileResponse getCurrentUserProfile(String email) {
        User user = userService.findByEmail(email);
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .accountType(user.getAccountType())
                .authProvider(user.getAuthProvider())
                .profilePictureUrl(user.getProfilePictureUrl())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public void logout(String token) {
        try {
            String email = jwtService.extractUsername(token);
            Instant expiresAt = jwtService.extractExpiration(token).toInstant();

            BlacklistedToken blacklistedToken = BlacklistedToken.builder()
                    .token(token)
                    .userEmail(email)
                    .expiresAt(expiresAt)
                    .blacklistedAt(Instant.now())
                    .build();

            blacklistedTokenRepository.save(blacklistedToken);
        } catch (Exception e) {
            // Token might be invalid, but we still consider logout successful
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokenRepository.existsByToken(token);
    }

    public UserProfileResponse upgradeAccount(String email, UpgradeAccountRequest request) {
        User user = userService.findByEmail(email);

        if (user.getAccountType() == AccountType.VIP && request.getAccountType() == AccountType.VIP) {
            throw new BadRequestException("Account is already VIP");
        }

        user.setAccountType(request.getAccountType());
        user = userService.save(user);

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .accountType(user.getAccountType())
                .authProvider(user.getAuthProvider())
                .profilePictureUrl(user.getProfilePictureUrl())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * Authenticate user via Google OAuth 2.0 authorization code flow.
     *
     * Handles the following scenarios:
     * 1. New user: Creates a new account with Google provider
     * 2. Existing Google user: Logs in the user
     * 3. Existing local user with same email: Links Google account to existing account
     *
     * @param request The Google auth request containing the authorization code
     * @return AuthResponse with JWT tokens and user info
     */
    public AuthResponse loginWithGoogle(GoogleAuthRequest request) {
        log.info("Processing Google OAuth login");

        // Step 1: Exchange authorization code for access token
        GoogleTokenResponse tokenResponse = googleOAuthService.exchangeCodeForToken(
                request.getCode(),
                request.getRedirectUri()
        );

        // Step 2: Get user info from Google
        GoogleUserInfo googleUserInfo = googleOAuthService.getUserInfo(tokenResponse.getAccessToken());

        // Step 3: Check if user exists
        Optional<User> existingUserOpt = userService.findByEmailOptional(googleUserInfo.getEmail());

        User user;
        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            user = handleExistingUser(user, googleUserInfo);
        } else {
            // New user - create account with Google provider
            user = createGoogleUser(googleUserInfo);
        }

        // Step 4: Generate JWT tokens
        UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        log.info("Google OAuth login successful for user: {}", user.getEmail());
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    /**
     * Handle existing user during Google OAuth login.
     *
     * Scenarios:
     * 1. User already linked with Google: Just log them in
     * 2. User registered locally with same email: Link Google account
     */
    private User handleExistingUser(User user, GoogleUserInfo googleUserInfo) {
        if (user.getAuthProvider() == AuthProvider.GOOGLE) {
            // User already registered with Google - just update profile if needed
            log.info("Existing Google user logging in: {}", user.getEmail());
            return updateGoogleUserProfile(user, googleUserInfo);
        } else if (user.getAuthProvider() == AuthProvider.LOCAL) {
            // User registered locally - link Google account
            log.info("Linking Google account to existing local user: {}", user.getEmail());
            return linkGoogleAccount(user, googleUserInfo);
        } else {
            // Unknown provider - treat as Google login
            log.warn("Unknown auth provider for user: {}, treating as Google login", user.getEmail());
            return linkGoogleAccount(user, googleUserInfo);
        }
    }

    /**
     * Create a new user from Google OAuth information.
     */
    private User createGoogleUser(GoogleUserInfo googleUserInfo) {
        log.info("Creating new user from Google OAuth: {}", googleUserInfo.getEmail());

        User user = User.builder()
                .email(googleUserInfo.getEmail())
                .firstName(googleUserInfo.getGivenName() != null ? googleUserInfo.getGivenName() : "")
                .lastName(googleUserInfo.getFamilyName() != null ? googleUserInfo.getFamilyName() : "")
                .authProvider(AuthProvider.GOOGLE)
                .providerId(googleUserInfo.getSub())
                .profilePictureUrl(googleUserInfo.getPicture())
                .enabled(true)
                .accountType(AccountType.STANDARD)
                .build();

        return userService.save(user);
    }

    /**
     * Link Google account to an existing local user.
     * This allows users who registered with email/password to also use Google login.
     */
    private User linkGoogleAccount(User user, GoogleUserInfo googleUserInfo) {
        // Keep the existing auth provider as LOCAL but add Google provider info
        // This way user can still login with password if they want
        user.setProviderId(googleUserInfo.getSub());
        user.setProfilePictureUrl(googleUserInfo.getPicture());

        // Update name if not set
        if (user.getFirstName() == null || user.getFirstName().isEmpty()) {
            user.setFirstName(googleUserInfo.getGivenName());
        }
        if (user.getLastName() == null || user.getLastName().isEmpty()) {
            user.setLastName(googleUserInfo.getFamilyName());
        }

        // Change auth provider to GOOGLE since they're now using Google login
        // They can still use password login if they have a password set
        user.setAuthProvider(AuthProvider.GOOGLE);

        return userService.save(user);
    }

    /**
     * Update Google user's profile with latest info from Google.
     */
    private User updateGoogleUserProfile(User user, GoogleUserInfo googleUserInfo) {
        boolean updated = false;

        // Update profile picture if changed
        if (googleUserInfo.getPicture() != null &&
            !googleUserInfo.getPicture().equals(user.getProfilePictureUrl())) {
            user.setProfilePictureUrl(googleUserInfo.getPicture());
            updated = true;
        }

        // Update provider ID if not set
        if (user.getProviderId() == null && googleUserInfo.getSub() != null) {
            user.setProviderId(googleUserInfo.getSub());
            updated = true;
        }

        if (updated) {
            return userService.save(user);
        }
        return user;
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration())
                .user(AuthResponse.UserResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .accountType(user.getAccountType())
                        .authProvider(user.getAuthProvider())
                        .profilePictureUrl(user.getProfilePictureUrl())
                        .build())
                .build();
    }
}

