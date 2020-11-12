package io.polygloat.dtos.request;


import io.polygloat.dtos.PathDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

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
