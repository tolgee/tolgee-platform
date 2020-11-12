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
public class EditSourceDTO {
    @NotBlank
    private String oldFullPathString;

    @NotBlank
    private String newFullPathString;

    public PathDTO getOldPathDto() {
        return PathDTO.fromFullPath(oldFullPathString);
    }

    public PathDTO getNewPathDto() {
        return PathDTO.fromFullPath(newFullPathString);
    }
}
