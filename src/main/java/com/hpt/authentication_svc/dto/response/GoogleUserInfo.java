package com.hpt.authentication_svc.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Google user info.
 * Represents the user information retrieved from Google's userinfo endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleUserInfo {

    /**
     * Google's unique user ID (sub claim)
     */
    private String sub;

    /**
     * User's email address
     */
    private String email;

    /**
     * Whether the email has been verified by Google
     */
    @JsonProperty("email_verified")
    private Boolean emailVerified;

    /**
     * User's full name
     */
    private String name;

    /**
     * User's given (first) name
     */
    @JsonProperty("given_name")
    private String givenName;

    /**
     * User's family (last) name
     */
    @JsonProperty("family_name")
    private String familyName;

    /**
     * URL to user's profile picture
     */
    private String picture;

    /**
     * User's locale (e.g., "en")
     */
    private String locale;
}

