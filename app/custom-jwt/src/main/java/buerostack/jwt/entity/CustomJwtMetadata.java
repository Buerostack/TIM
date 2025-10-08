package buerostack.jwt.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jwt_metadata", schema = "custom_jwt")
public class CustomJwtMetadata {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "jwt_uuid", nullable = false)
    private UUID jwtUuid;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "claim_keys", nullable = false)
    private String claimKeys;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "subject")
    private String subject;

    @Column(name = "jwt_name")
    private String jwtName;

    @Column(name = "audience")
    private String audience;

    @Column(name = "issuer")
    private String issuer;

    @Column(name = "supersedes")
    private UUID supersedes;

    @Column(name = "original_jwt_uuid", nullable = false)
    private UUID originalJwtUuid;

    public CustomJwtMetadata() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
    }

    public CustomJwtMetadata(UUID jwtUuid, String claimKeys, Instant issuedAt, Instant expiresAt, UUID originalJwtUuid) {
        this();
        this.jwtUuid = jwtUuid;
        this.claimKeys = claimKeys;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.originalJwtUuid = originalJwtUuid;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getJwtUuid() { return jwtUuid; }
    public void setJwtUuid(UUID jwtUuid) { this.jwtUuid = jwtUuid; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getClaimKeys() { return claimKeys; }
    public void setClaimKeys(String claimKeys) { this.claimKeys = claimKeys; }

    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getJwtName() { return jwtName; }
    public void setJwtName(String jwtName) { this.jwtName = jwtName; }

    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public UUID getSupersedes() { return supersedes; }
    public void setSupersedes(UUID supersedes) { this.supersedes = supersedes; }

    public UUID getOriginalJwtUuid() { return originalJwtUuid; }
    public void setOriginalJwtUuid(UUID originalJwtUuid) { this.originalJwtUuid = originalJwtUuid; }
}