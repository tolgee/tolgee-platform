package com.polygloat.dtos.request;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Data
public class ResetPassword {
    @NotBlank
    String email;

    @NotBlank
    String code;

    @Min(8)
    @Max(100)
    String password;
}
