package com.hpt.authentication_svc.dto.response;

import com.hpt.authentication_svc.model.AccountType;
import com.hpt.authentication_svc.model.AuthProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private boolean enabled;
    private AccountType accountType;
    private AuthProvider authProvider;
    private String profilePictureUrl;
    private Instant createdAt;
    private Instant updatedAt;
}

