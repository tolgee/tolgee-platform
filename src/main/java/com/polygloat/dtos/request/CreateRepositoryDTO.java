package com.polygloat.dtos.request;

import com.polygloat.dtos.request.validators.annotations.RepositoryRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Set;

@RepositoryRequest
@NoArgsConstructor
public class CreateRepositoryDTO extends AbstractRepositoryDTO {
    @NotEmpty
    @Getter
    @Setter
    Set<LanguageDTO> languages;

    public CreateRepositoryDTO(@NotNull String name, @NotEmpty Set<LanguageDTO> languages) {
        this.name = name;
        this.languages = languages;
    }
}
