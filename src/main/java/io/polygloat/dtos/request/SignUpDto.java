package io.polygloat.dtos.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

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

    @Size(min = 8, max = 100)
    @NotBlank
    String password;

    String invitationCode;
}