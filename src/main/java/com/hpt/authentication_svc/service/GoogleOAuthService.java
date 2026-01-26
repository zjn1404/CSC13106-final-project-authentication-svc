package com.hpt.authentication_svc.service;

import com.hpt.authentication_svc.config.GoogleOAuthConfig;
import com.hpt.authentication_svc.dto.response.GoogleTokenResponse;
import com.hpt.authentication_svc.dto.response.GoogleUserInfo;
import com.hpt.authentication_svc.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Service for handling Google OAuth 2.0 operations.
 * Implements the authorization code flow:
 * 1. Exchange authorization code for access token
 * 2. Use access token to retrieve user information
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final GoogleOAuthConfig googleOAuthConfig;
    private final WebClient.Builder webClientBuilder;

    /**
     * Exchange authorization code for Google access token.
     * 
     * @param code The authorization code from Google's OAuth consent screen
     * @param redirectUri The redirect URI used in the authorization request
     * @return GoogleTokenResponse containing access token and other token info
     */
    public GoogleTokenResponse exchangeCodeForToken(String code, String redirectUri) {
        log.info("Exchanging authorization code for Google access token");

        String effectiveRedirectUri = redirectUri != null ? redirectUri : googleOAuthConfig.getRedirectUri();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("code", code);
        formData.add("client_id", googleOAuthConfig.getClientId());
        formData.add("client_secret", googleOAuthConfig.getClientSecret());
        formData.add("redirect_uri", effectiveRedirectUri);
        formData.add("grant_type", "authorization_code");

        try {
            GoogleTokenResponse tokenResponse = webClientBuilder.build()
                    .post()
                    .uri(googleOAuthConfig.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(GoogleTokenResponse.class)
                    .block();

            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                log.error("Failed to get access token from Google");
                throw new BadRequestException("Failed to authenticate with Google");
            }

            log.info("Successfully obtained Google access token");
            return tokenResponse;

        } catch (WebClientResponseException e) {
            log.error("Google token exchange failed: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BadRequestException("Failed to authenticate with Google: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during Google token exchange", e);
            throw new BadRequestException("Failed to authenticate with Google");
        }
    }

    /**
     * Retrieve user information from Google using the access token.
     * 
     * @param accessToken The access token obtained from token exchange
     * @return GoogleUserInfo containing user's profile information
     */
    public GoogleUserInfo getUserInfo(String accessToken) {
        log.info("Retrieving user info from Google");

        try {
            GoogleUserInfo userInfo = webClientBuilder.build()
                    .get()
                    .uri(googleOAuthConfig.getUserInfoUri())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(GoogleUserInfo.class)
                    .block();

            if (userInfo == null || userInfo.getEmail() == null) {
                log.error("Failed to get user info from Google");
                throw new BadRequestException("Failed to get user information from Google");
            }

            log.info("Successfully retrieved Google user info for: {}", userInfo.getEmail());
            return userInfo;

        } catch (WebClientResponseException e) {
            log.error("Google user info retrieval failed: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BadRequestException("Failed to get user information from Google");
        } catch (Exception e) {
            log.error("Unexpected error during Google user info retrieval", e);
            throw new BadRequestException("Failed to get user information from Google");
        }
    }
}

