package io.polygloat.dtos.request;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.polygloat.constants.ApiScope;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Set;
import java.util.stream.Collectors;

public class CreateApiKeyDTO {
    @NotNull
    private Long repositoryId;

    @JsonIgnore
    @NotEmpty
    private Set<ApiScope> scopes;

    public CreateApiKeyDTO(@NotNull Long repositoryId, @NotEmpty Set<ApiScope> scopes) {
        this.repositoryId = repositoryId;
        this.scopes = scopes;
    }

    public CreateApiKeyDTO() {
    }

    public static CreateApiKeyDTOBuilder builder() {
        return new CreateApiKeyDTOBuilder();
    }

    @JsonSetter("scopes")
    public void jsonSetScopes(Set<String> scopes) {
        this.scopes = scopes.stream().map(ApiScope::fromValue).collect(Collectors.toSet());
    }

    @JsonGetter("scopes")
    public Set<String> jsonGetScopes() {
        return this.scopes.stream().map(ApiScope::getValue).collect(Collectors.toSet());
    }

    public @NotNull Long getRepositoryId() {
        return this.repositoryId;
    }

    public @NotEmpty Set<ApiScope> getScopes() {
        return this.scopes;
    }

    public void setRepositoryId(@NotNull Long repositoryId) {
        this.repositoryId = repositoryId;
    }

    public void setScopes(@NotEmpty Set<ApiScope> scopes) {
        this.scopes = scopes;
    }

    public static class CreateApiKeyDTOBuilder {
        private @NotNull Long repositoryId;
        private @NotEmpty Set<ApiScope> scopes;

        CreateApiKeyDTOBuilder() {
        }

        public CreateApiKeyDTO.CreateApiKeyDTOBuilder repositoryId(@NotNull Long repositoryId) {
            this.repositoryId = repositoryId;
            return this;
        }

        public CreateApiKeyDTO.CreateApiKeyDTOBuilder scopes(@NotEmpty Set<ApiScope> scopes) {
            this.scopes = scopes;
            return this;
        }

        public CreateApiKeyDTO build() {
            return new CreateApiKeyDTO(repositoryId, scopes);
        }

        public String toString() {
            return "CreateApiKeyDTO.CreateApiKeyDTOBuilder(repositoryId=" + this.repositoryId + ", scopes=" + this.scopes + ")";
        }
    }
}
