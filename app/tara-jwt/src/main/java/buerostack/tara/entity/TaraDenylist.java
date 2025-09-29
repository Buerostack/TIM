package buerostack.tara.entity; import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="denylist", schema="tara") public class TaraDenylist {
 @Id @Column(name="jwt_uuid") private UUID jwtUuid; @Column(name="denylisted_at") private Instant denylistedAt; @Column(name="expires_at") private Instant expiresAt; @Column(name="reason") private String reason;
 public UUID getJwtUuid(){return jwtUuid;} public void setJwtUuid(UUID v){this.jwtUuid=v;}
 public Instant getDenylistedAt(){return denylistedAt;} public void setDenylistedAt(Instant v){this.denylistedAt=v;}
 public Instant getExpiresAt(){return expiresAt;} public void setExpiresAt(Instant v){this.expiresAt=v;}
 public String getReason(){return reason;} public void setReason(String v){this.reason=v;} }