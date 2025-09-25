package buerostack.tara.entity; import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="oauth_state", schema="tara") public class TaraOauthState {
 @Id @Column(name="state") private String state; @Column(name="created_at") private Instant createdAt; @Column(name="pkce_verifier") private String pkceVerifier;
 public String getState(){return state;} public void setState(String v){this.state=v;} public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){this.createdAt=v;}
 public String getPkceVerifier(){return pkceVerifier;} public void setPkceVerifier(String v){this.pkceVerifier=v;} }