package com.hpt.authentication_svc.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.Getter;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Google OAuth 2.0 settings.
 * Loads configuration from environment variables via dotenv.
 */
@Getter
@Configuration
public class GoogleOAuthConfig {

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String tokenUri;
    private final String userInfoUri;

    public GoogleOAuthConfig(Dotenv dotenv) {
        this.clientId = dotenv.get("GOOGLE_CLIENT_ID", "");
        this.clientSecret = dotenv.get("GOOGLE_CLIENT_SECRET", "");
        this.redirectUri = dotenv.get("GOOGLE_REDIRECT_URI", "http://localhost:3000/auth/google/callback");
        this.tokenUri = "https://oauth2.googleapis.com/token";
        this.userInfoUri = "https://www.googleapis.com/oauth2/v3/userinfo";
    }
}

