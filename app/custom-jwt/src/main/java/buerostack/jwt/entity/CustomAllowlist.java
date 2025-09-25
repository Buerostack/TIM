package buerostack.jwt.entity; import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="allowlist", schema="custom") public class CustomAllowlist {
 @Id @Column(name="jwt_hash") private String jwtHash; @Column(name="expires_at") private Instant expiresAt;
 public String getJwtHash(){return jwtHash;} public void setJwtHash(String v){this.jwtHash=v;}
 public Instant getExpiresAt(){return expiresAt;} public void setExpiresAt(Instant v){this.expiresAt=v;} }