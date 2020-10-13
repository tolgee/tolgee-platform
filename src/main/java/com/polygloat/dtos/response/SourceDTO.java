package com.polygloat.dtos.response;


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
public class SourceDTO {
    @NotBlank
    @Size(min = 1, max = 300)
    private String fullPathString;

    public PathDTO getPathDto() {
        return PathDTO.fromFullPath(fullPathString);
    }
}
