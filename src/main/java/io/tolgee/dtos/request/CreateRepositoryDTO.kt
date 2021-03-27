package io.tolgee.dtos.request;

import io.tolgee.dtos.request.validators.annotations.RepositoryRequest;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Set;

@RepositoryRequest
public class CreateRepositoryDTO extends AbstractRepositoryDTO {
    @NotEmpty
    Set<LanguageDTO> languages;

    public CreateRepositoryDTO(@NotNull String name, @NotEmpty Set<LanguageDTO> languages) {
        this.name = name;
        this.languages = languages;
    }

    public CreateRepositoryDTO() {
    }

    public @NotEmpty Set<LanguageDTO> getLanguages() {
        return this.languages;
    }

    public void setLanguages(@NotEmpty Set<LanguageDTO> languages) {
        this.languages = languages;
    }
}
