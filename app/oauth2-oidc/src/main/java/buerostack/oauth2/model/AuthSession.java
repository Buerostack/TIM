package buerostack.oauth2.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

/**
 * Authentication session model
 */
public class AuthSession {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("user_id")
    private String userId;

    private String provider;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("last_activity")
    private Instant lastActivity;

    @JsonProperty("expires_at")
    private Instant expiresAt;

    private TokenData tokens;

    @JsonProperty("session_metadata")
    private SessionMetadata sessionMetadata;

    private String status; // active, expired, revoked

    // Default constructor
    public AuthSession() {}

    // Constructor
    public AuthSession(String sessionId, String userId, String provider) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.provider = provider;
        this.createdAt = Instant.now();
        this.lastActivity = Instant.now();
        this.status = "active";
    }

    // Getters and setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getLastActivity() { return lastActivity; }
    public void setLastActivity(Instant lastActivity) { this.lastActivity = lastActivity; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public TokenData getTokens() { return tokens; }
    public void setTokens(TokenData tokens) { this.tokens = tokens; }

    public SessionMetadata getSessionMetadata() { return sessionMetadata; }
    public void setSessionMetadata(SessionMetadata sessionMetadata) { this.sessionMetadata = sessionMetadata; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    /**
     * Token data (encrypted in storage)
     */
    public static class TokenData {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("id_token")
        private String idToken;

        @JsonProperty("token_expires_at")
        private Instant tokenExpiresAt;

        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

        public String getIdToken() { return idToken; }
        public void setIdToken(String idToken) { this.idToken = idToken; }

        public Instant getTokenExpiresAt() { return tokenExpiresAt; }
        public void setTokenExpiresAt(Instant tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }
    }

    /**
     * Session metadata
     */
    public static class SessionMetadata {
        @JsonProperty("ip_address")
        private String ipAddress;

        @JsonProperty("user_agent")
        private String userAgent;

        @JsonProperty("authentication_method")
        private String authenticationMethod;

        @JsonProperty("level_of_assurance")
        private String levelOfAssurance;

        @JsonProperty("custom_attributes")
        private Map<String, Object> customAttributes;

        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

        public String getAuthenticationMethod() { return authenticationMethod; }
        public void setAuthenticationMethod(String authenticationMethod) { this.authenticationMethod = authenticationMethod; }

        public String getLevelOfAssurance() { return levelOfAssurance; }
        public void setLevelOfAssurance(String levelOfAssurance) { this.levelOfAssurance = levelOfAssurance; }

        public Map<String, Object> getCustomAttributes() { return customAttributes; }
        public void setCustomAttributes(Map<String, Object> customAttributes) { this.customAttributes = customAttributes; }
    }
}