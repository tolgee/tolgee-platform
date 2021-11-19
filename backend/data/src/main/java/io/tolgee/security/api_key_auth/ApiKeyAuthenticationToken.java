package io.tolgee.security.api_key_auth;

import io.tolgee.dtos.cacheable.UserAccountDto;
import io.tolgee.model.ApiKey;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Collections;

public class ApiKeyAuthenticationToken extends UsernamePasswordAuthenticationToken {

    private final ApiKey apiKey;

    public ApiKeyAuthenticationToken(ApiKey apiKey) {
        super(UserAccountDto.Companion.fromEntity(apiKey.getUserAccount()), null, Collections.singleton(() -> "api"));
        this.apiKey = apiKey;
    }

    public ApiKey getApiKey() {
        return apiKey;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ApiKeyAuthenticationToken)) return false;
        final ApiKeyAuthenticationToken other = (ApiKeyAuthenticationToken) o;
        if (!other.canEqual(this)) return false;
        if (!super.equals(o)) return false;
        final Object this$apiKey = this.getApiKey();
        final Object other$apiKey = other.getApiKey();
        return this$apiKey == null ? other$apiKey == null : this$apiKey.equals(other$apiKey);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ApiKeyAuthenticationToken;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        final Object $apiKey = this.getApiKey();
        result = result * PRIME + ($apiKey == null ? 43 : $apiKey.hashCode());
        return result;
    }
}
