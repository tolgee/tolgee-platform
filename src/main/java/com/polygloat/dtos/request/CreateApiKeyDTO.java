package com.polygloat.dtos.request;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.polygloat.constants.ApiScope;
import lombok.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Set;
import java.util.stream.Collectors;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateApiKeyDTO {
    @NotNull
    @Getter
    @Setter
    private Long repositoryId;

    @JsonIgnore
    @NotEmpty
    @Getter
    @Setter
    private Set<ApiScope> scopes;

    @JsonSetter("scopes")
    public void jsonSetScopes(Set<String> scopes) {
        this.scopes = scopes.stream().map(ApiScope::fromValue).collect(Collectors.toSet());
    }

    @JsonGetter("scopes")
    public Set<String> jsonGetScopes() {
        return this.scopes.stream().map(ApiScope::getValue).collect(Collectors.toSet());
    }
}
