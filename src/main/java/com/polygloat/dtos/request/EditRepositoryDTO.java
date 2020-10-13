package com.polygloat.dtos.request;

import com.polygloat.dtos.request.validators.annotations.RepositoryRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@RepositoryRequest
@NoArgsConstructor
public class EditRepositoryDTO extends AbstractRepositoryDTO {
    @Getter
    @Setter
    @NotNull
    Long repositoryId;


    public EditRepositoryDTO(Long repositoryId, String name) {
        this.repositoryId = repositoryId;
        this.name = name;
    }
}
