package io.polygloat.dtos.request;

import io.polygloat.dtos.request.validators.annotations.RepositoryRequest;

import javax.validation.constraints.NotNull;

@RepositoryRequest
public class EditRepositoryDTO extends AbstractRepositoryDTO {
    @NotNull
    Long repositoryId;


    public EditRepositoryDTO(Long repositoryId, String name) {
        this.repositoryId = repositoryId;
        this.name = name;
    }

    public EditRepositoryDTO() {
    }

    public @NotNull Long getRepositoryId() {
        return this.repositoryId;
    }

    public void setRepositoryId(@NotNull Long repositoryId) {
        this.repositoryId = repositoryId;
    }
}
