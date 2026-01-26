package com.hpt.authentication_svc.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Google OAuth authentication.
 * Contains the authorization code received from Google's OAuth consent screen.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAuthRequest {

    @NotBlank(message = "Authorization code is required")
    private String code;

    /**
     * Optional: The redirect URI used in the authorization request.
     * Must match the redirect URI configured in Google Cloud Console.
     */
    private String redirectUri;
}

