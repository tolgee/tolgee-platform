package com.polygloat.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponseDTO {

    private Long id;

    private String name;

    private String username;

    public static UserResponseDTO fromEntity(com.polygloat.model.UserAccount user) {
        return UserResponseDTO.builder().username(user.getUsername()).name(user.getName()).id(user.getId()).build();
    }
}
