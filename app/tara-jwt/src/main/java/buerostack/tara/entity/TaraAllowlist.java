package buerostack.tara.entity; import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="allowlist", schema="tara") public class TaraAllowlist {
 @Id @Column(name="jwt_hash") private String jwtHash; @Column(name="expires_at") private Instant expiresAt;
 public String getJwtHash(){return jwtHash;} public void setJwtHash(String v){this.jwtHash=v;}
 public Instant getExpiresAt(){return expiresAt;} public void setExpiresAt(Instant v){this.expiresAt=v;} }