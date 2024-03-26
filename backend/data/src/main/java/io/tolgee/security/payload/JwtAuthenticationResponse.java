package io.tolgee.security.payload;

public class JwtAuthenticationResponse {
    private String accessToken;

    private final String tokenType = "Bearer";

    public JwtAuthenticationResponse(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public String getTokenType() {
        return this.tokenType;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
