package buerostack.jwt.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jwt_metadata", schema = "custom")
public class CustomJwtMetadata {

    @Id
    @Column(name = "jwt_uuid")
    private UUID jwtUuid;

    @Column(name = "claim_keys", nullable = false)
    private String claimKeys;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public CustomJwtMetadata() {}

    public CustomJwtMetadata(String claimKeys, Instant issuedAt, Instant expiresAt) {
        this.claimKeys = claimKeys;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public UUID getJwtUuid() { return jwtUuid; }
    public void setJwtUuid(UUID jwtUuid) { this.jwtUuid = jwtUuid; }

    public String getClaimKeys() { return claimKeys; }
    public void setClaimKeys(String claimKeys) { this.claimKeys = claimKeys; }

    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}