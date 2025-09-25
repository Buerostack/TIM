package buerostack.jwt.service;
import buerostack.config.JwtSignerService; import buerostack.jwt.entity.*; import buerostack.jwt.repo.*; import com.nimbusds.jwt.SignedJWT; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.time.Instant; import java.util.*;
@Service public class CustomJwtService {
 private final JwtSignerService signer; private final CustomDenylistRepo denylistRepo; private final CustomJwtMetadataRepo metaRepo;
 public CustomJwtService(JwtSignerService s, CustomDenylistRepo d, CustomJwtMetadataRepo m){ this.signer=s; this.denylistRepo=d; this.metaRepo=m; }
 public String generate(String jwtName, Map<String,Object> claims, String issuer, String audience, long ttl) throws Exception {
   String token = signer.sign(claims, issuer, audience, ttl);
   var jwt = SignedJWT.parse(token); var jti = java.util.UUID.fromString(jwt.getJWTClaimsSet().getJWTID());
   var meta = new CustomJwtMetadata(); meta.setJwtUuid(jti); meta.setIssuedAt(jwt.getJWTClaimsSet().getIssueTime().toInstant());
   meta.setExpiresAt(jwt.getJWTClaimsSet().getExpirationTime().toInstant()); meta.setClaimKeys(String.join(",", claims.keySet())); metaRepo.save(meta); return token; }
 public boolean isRevoked(String token){ try{ var jwt = SignedJWT.parse(token); var jti = java.util.UUID.fromString(jwt.getJWTClaimsSet().getJWTID());
   return denylistRepo.findById(jti).isPresent(); }catch(Exception e){ return true; } }
 @Transactional public void denylist(String token) throws Exception { var jwt = SignedJWT.parse(token); var jti = java.util.UUID.fromString(jwt.getJWTClaimsSet().getJWTID());
   var dl = new CustomDenylist(); dl.setJwtUuid(jti); dl.setDenylistedAt(Instant.now()); dl.setExpiresAt(jwt.getJWTClaimsSet().getExpirationTime().toInstant()); denylistRepo.save(dl); }
}