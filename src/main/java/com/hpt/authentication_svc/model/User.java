package com.hpt.authentication_svc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String password;

    private String firstName;

    private String lastName;

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private AccountType accountType = AccountType.STANDARD;

    // OAuth provider information
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    private String providerId; // Google user ID when using OAuth

    private String profilePictureUrl; // Profile picture from OAuth provider

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    /**
     * Check if user registered via OAuth (Google, etc.)
     */
    public boolean isOAuthUser() {
        return authProvider != null && authProvider != AuthProvider.LOCAL;
    }

    /**
     * Check if user has a password set (local registration or linked account)
     */
    public boolean hasPassword() {
        return password != null && !password.isEmpty();
    }
}

