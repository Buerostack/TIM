package buerostack.jwt.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class JwtTokenSummary {

    @JsonProperty("jti")
    private String jti;

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("status")
    private String status;

    @JsonProperty("issued_at")
    private Instant issuedAt;

    @JsonProperty("expires_at")
    private Instant expiresAt;

    @JsonProperty("revoked_at")
    private Instant revokedAt;

    @JsonProperty("revocation_reason")
    private String revocationReason;

    @JsonProperty("jwt_name")
    private String jwtName;

    @JsonProperty("issuer")
    private String issuer;

    @JsonProperty("audience")
    private String audience;

    @JsonProperty("claims")
    private String claims;

    public JwtTokenSummary() {}

    // Getters and setters
    public String getJti() { return jti; }
    public void setJti(String jti) { this.jti = jti; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }

    public String getRevocationReason() { return revocationReason; }
    public void setRevocationReason(String revocationReason) { this.revocationReason = revocationReason; }

    public String getJwtName() { return jwtName; }
    public void setJwtName(String jwtName) { this.jwtName = jwtName; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }

    public String getClaims() { return claims; }
    public void setClaims(String claims) { this.claims = claims; }
}