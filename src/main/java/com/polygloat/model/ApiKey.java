package com.polygloat.model;

import com.polygloat.constants.ApiScope;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"key"}, name = "api_key_unique"),
})
@EqualsAndHashCode(callSuper = true, of = {"id", "key", "scopes"})
@NoArgsConstructor
@AllArgsConstructor
@ToString(of = {"key"})
@Builder
@Data
public class ApiKey extends AuditModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    private UserAccount userAccount;

    @NotNull
    @ManyToOne
    private Repository repository;

    @NotEmpty
    @NotNull
    private String key;

    @NotBlank
    @NotNull
    private String scopes;

    public Set<ApiScope> getScopes() {
        return Arrays.stream(this.scopes.split(";")).map(ApiScope::fromValue).collect(Collectors.toSet());
    }

    public void setScopes(Set<ApiScope> scopes) {
        this.scopes = stringifyScopes(scopes);
    }

    @SuppressWarnings("unused")
    public static class ApiKeyBuilder {
        private String scopes;

        public ApiKeyBuilder scopes(Set<ApiScope> scopes) {
            this.scopes = stringifyScopes(scopes);
            return this;
        }
    }

    private static String stringifyScopes(Set<ApiScope> scopes) {
        return scopes.stream().map(ApiScope::getValue).collect(Collectors.joining(";"));
    }
}
