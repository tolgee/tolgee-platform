package io.polygloat.model;

import io.polygloat.constants.ApiScope;

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

    public ApiKey(Long id, @NotNull UserAccount userAccount, @NotNull Repository repository, @NotEmpty @NotNull String key, @NotBlank @NotNull String scopes) {
        this.id = id;
        this.userAccount = userAccount;
        this.repository = repository;
        this.key = key;
        this.scopes = scopes;
    }

    public ApiKey() {
    }

    public static ApiKeyBuilder builder() {
        return new ApiKeyBuilder();
    }

    public Set<ApiScope> getScopes() {
        return Arrays.stream(this.scopes.split(";")).map(ApiScope::fromValue).collect(Collectors.toSet());
    }

    public void setScopes(Set<ApiScope> scopes) {
        this.scopes = stringifyScopes(scopes);
    }

    public Long getId() {
        return this.id;
    }

    public @NotNull UserAccount getUserAccount() {
        return this.userAccount;
    }

    public @NotEmpty @NotNull String getKey() {
        return this.key;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUserAccount(@NotNull UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    public void setRepository(@NotNull Repository repository) {
        this.repository = repository;
    }

    public void setKey(@NotEmpty @NotNull String key) {
        this.key = key;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ApiKey)) return false;
        final ApiKey other = (ApiKey) o;
        if (!other.canEqual((Object) this)) return false;
        if (!super.equals(o)) return false;
        final Object this$id = this.getId();
        final Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final Object this$key = this.getKey();
        final Object other$key = other.getKey();
        if (this$key == null ? other$key != null : !this$key.equals(other$key)) return false;
        final Object this$scopes = this.getScopes();
        final Object other$scopes = other.getScopes();
        if (this$scopes == null ? other$scopes != null : !this$scopes.equals(other$scopes)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ApiKey;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        final Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final Object $key = this.getKey();
        result = result * PRIME + ($key == null ? 43 : $key.hashCode());
        final Object $scopes = this.getScopes();
        result = result * PRIME + ($scopes == null ? 43 : $scopes.hashCode());
        return result;
    }

    public String toString() {
        return "ApiKey(key=" + this.getKey() + ")";
    }

    @SuppressWarnings("unused")
    public static class ApiKeyBuilder {
        private String scopes;
        private Long id;
        private @NotNull UserAccount userAccount;
        private @NotNull Repository repository;
        private @NotEmpty @NotNull String key;

        ApiKeyBuilder() {
        }

        public ApiKeyBuilder scopes(Set<ApiScope> scopes) {
            this.scopes = stringifyScopes(scopes);
            return this;
        }

        public ApiKeyBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ApiKeyBuilder userAccount(@NotNull UserAccount userAccount) {
            this.userAccount = userAccount;
            return this;
        }

        public ApiKeyBuilder repository(@NotNull Repository repository) {
            this.repository = repository;
            return this;
        }

        public ApiKeyBuilder key(@NotEmpty @NotNull String key) {
            this.key = key;
            return this;
        }

        public ApiKey build() {
            return new ApiKey(id, userAccount, repository, key, scopes);
        }

        public String toString() {
            return "ApiKey.ApiKeyBuilder(scopes=" + this.scopes + ", id=" + this.id + ", userAccount=" + this.userAccount + ", repository=" + this.repository + ", key=" + this.key + ")";
        }
    }

    public Repository getRepository() {
        return repository;
    }

    private static String stringifyScopes(Set<ApiScope> scopes) {
        return scopes.stream().map(ApiScope::getValue).collect(Collectors.joining(";"));
    }
}
