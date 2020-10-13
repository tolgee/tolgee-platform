package com.polygloat.dtos.request;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.polygloat.constants.ApiScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EditApiKeyDTO {
    @NotNull
    private Long id;

    @NotEmpty
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
