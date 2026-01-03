package com.hpt.authentication_svc.dto.request;

import com.hpt.authentication_svc.model.AccountType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpgradeAccountRequest {

    @NotNull(message = "Account type is required")
    private AccountType accountType;
}

