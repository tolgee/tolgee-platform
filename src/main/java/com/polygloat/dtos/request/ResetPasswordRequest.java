package com.polygloat.dtos.request;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class ResetPasswordRequest {
    @NotBlank
    String callbackUrl;

    @Email
    String email;
}
