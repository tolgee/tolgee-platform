package com.polygloat.dtos.request;


import com.polygloat.dtos.PathDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteSourceDTO {
    @NotBlank
    private String fullPathString;

    public PathDTO getFullPathDTO() {
        return PathDTO.fromFullPath(fullPathString);
    }

}
