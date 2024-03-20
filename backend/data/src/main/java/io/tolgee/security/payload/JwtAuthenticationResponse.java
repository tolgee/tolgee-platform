package io.tolgee.security.payload;

public class JwtAuthenticationResponse {
    private String accessToken;

    private Long userId;

    private final String tokenType = "Bearer";

    public JwtAuthenticationResponse(String accessToken, long userId) {
        this.accessToken = accessToken;
        this.userId = userId;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
