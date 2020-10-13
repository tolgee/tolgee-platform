package com.polygloat.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SignUpDto {
    @NotBlank
    String name;

    @Email
    @NotBlank
    String email;

    @Length(min = 8, max = 100)
    @NotBlank
    String password;

    String invitationCode;
}