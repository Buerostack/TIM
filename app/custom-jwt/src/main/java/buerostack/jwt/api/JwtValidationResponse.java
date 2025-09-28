package buerostack.jwt.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

public class JwtValidationResponse {

    @JsonProperty("valid")
    private boolean valid;

    @JsonProperty("active")
    private boolean active;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("issuer")
    private String issuer;

    @JsonProperty("audience")
    private String audience;

    @JsonProperty("expires_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant expiresAt;

    @JsonProperty("issued_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant issuedAt;

    @JsonProperty("jwt_id")
    private String jwtId;

    @JsonProperty("claims")
    private Map<String, Object> claims;

    public JwtValidationResponse() {}

    public JwtValidationResponse(boolean valid, boolean active, String reason) {
        this.valid = valid;
        this.active = active;
        this.reason = reason;
    }

    // Getters and setters
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }

    public String getJwtId() { return jwtId; }
    public void setJwtId(String jwtId) { this.jwtId = jwtId; }

    public Map<String, Object> getClaims() { return claims; }
    public void setClaims(Map<String, Object> claims) { this.claims = claims; }
}