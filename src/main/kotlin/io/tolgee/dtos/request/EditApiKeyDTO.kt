package io.tolgee.dtos.request;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.tolgee.constants.ApiScope;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Set;
import java.util.stream.Collectors;

public class EditApiKeyDTO {
    @NotNull
    private Long id;

    @NotEmpty
    private Set<ApiScope> scopes;

    public EditApiKeyDTO(@NotNull Long id, @NotEmpty Set<ApiScope> scopes) {
        this.id = id;
        this.scopes = scopes;
    }

    public EditApiKeyDTO() {
    }

    public static EditApiKeyDTOBuilder builder() {
        return new EditApiKeyDTOBuilder();
    }

    @JsonSetter("scopes")
    public void jsonSetScopes(Set<String> scopes) {
        this.scopes = scopes.stream().map(ApiScope::fromValue).collect(Collectors.toSet());
    }

    @JsonGetter("scopes")
    public Set<String> jsonGetScopes() {
        return this.scopes.stream().map(ApiScope::getValue).collect(Collectors.toSet());
    }

    public @NotNull Long getId() {
        return this.id;
    }

    public @NotEmpty Set<ApiScope> getScopes() {
        return this.scopes;
    }

    public void setId(@NotNull Long id) {
        this.id = id;
    }

    public void setScopes(@NotEmpty Set<ApiScope> scopes) {
        this.scopes = scopes;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof EditApiKeyDTO)) return false;
        final EditApiKeyDTO other = (EditApiKeyDTO) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$scopes = this.getScopes();
        final Object other$scopes = other.getScopes();
        if (this$scopes == null ? other$scopes != null : !this$scopes.equals(other$scopes)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof EditApiKeyDTO;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $scopes = this.getScopes();
        result = result * PRIME + ($scopes == null ? 43 : $scopes.hashCode());
        return result;
    }

    public String toString() {
        return "EditApiKeyDTO(id=" + this.getId() + ", scopes=" + this.getScopes() + ")";
    }

    public static class EditApiKeyDTOBuilder {
        private @NotNull Long id;
        private @NotEmpty Set<ApiScope> scopes;

        EditApiKeyDTOBuilder() {
        }

        public EditApiKeyDTO.EditApiKeyDTOBuilder id(@NotNull Long id) {
            this.id = id;
            return this;
        }

        public EditApiKeyDTO.EditApiKeyDTOBuilder scopes(@NotEmpty Set<ApiScope> scopes) {
            this.scopes = scopes;
            return this;
        }

        public EditApiKeyDTO build() {
            return new EditApiKeyDTO(id, scopes);
        }

        public String toString() {
            return "EditApiKeyDTO.EditApiKeyDTOBuilder(id=" + this.id + ", scopes=" + this.scopes + ")";
        }
    }
}
