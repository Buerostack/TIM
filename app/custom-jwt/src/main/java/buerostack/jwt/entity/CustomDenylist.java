package buerostack.jwt.entity; import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="denylist", schema="custom") public class CustomDenylist {
 @Id @Column(name="jwt_uuid") private UUID jwtUuid; @Column(name="denylisted_at") private Instant denylistedAt; @Column(name="expires_at") private Instant expiresAt;
 public UUID getJwtUuid(){return jwtUuid;} public void setJwtUuid(UUID v){this.jwtUuid=v;}
 public Instant getDenylistedAt(){return denylistedAt;} public void setDenylistedAt(Instant v){this.denylistedAt=v;}
 public Instant getExpiresAt(){return expiresAt;} public void setExpiresAt(Instant v){this.expiresAt=v;} }