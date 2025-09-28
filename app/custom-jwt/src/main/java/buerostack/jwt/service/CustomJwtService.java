package buerostack.jwt.service;
import buerostack.config.JwtSignerService;
import buerostack.jwt.api.JwtValidationResponse;
import buerostack.jwt.entity.*;
import buerostack.jwt.repo.*;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
@Service public class CustomJwtService {
 private final JwtSignerService signer; private final CustomDenylistRepo denylistRepo; private final CustomJwtMetadataRepo metaRepo;
 public CustomJwtService(JwtSignerService s, CustomDenylistRepo d, CustomJwtMetadataRepo m){ this.signer=s; this.denylistRepo=d; this.metaRepo=m; }
 public String generate(String jwtName, Map<String,Object> claims, String issuer, List<String> audiences, long ttl) throws Exception {
   String token = signer.sign(claims, issuer, audiences, ttl);
   var jwt = SignedJWT.parse(token); var jti = java.util.UUID.fromString(jwt.getJWTClaimsSet().getJWTID());
   var meta = new CustomJwtMetadata(); meta.setJwtUuid(jti); meta.setIssuedAt(jwt.getJWTClaimsSet().getIssueTime().toInstant());
   meta.setExpiresAt(jwt.getJWTClaimsSet().getExpirationTime().toInstant()); meta.setClaimKeys(String.join(",", claims.keySet())); metaRepo.save(meta); return token; }
 public boolean isRevoked(String token){ try{ var jwt = SignedJWT.parse(token); var jti = java.util.UUID.fromString(jwt.getJWTClaimsSet().getJWTID());
   return denylistRepo.findById(jti).isPresent(); }catch(Exception e){ return true; } }
 @Transactional public void denylist(String token) throws Exception { var jwt = SignedJWT.parse(token); var jti = java.util.UUID.fromString(jwt.getJWTClaimsSet().getJWTID());
   var dl = new CustomDenylist(); dl.setJwtUuid(jti); dl.setDenylistedAt(Instant.now()); dl.setExpiresAt(jwt.getJWTClaimsSet().getExpirationTime().toInstant()); denylistRepo.save(dl); }

 @Transactional public String extend(String oldToken, String issuer, List<String> audiences, long ttl) throws Exception {
   // Parse and validate the old token
   var jwt = SignedJWT.parse(oldToken);
   var claims = jwt.getJWTClaimsSet();

   // Validate the old token (must be valid but can be close to expiration)
   if (!signer.verify(oldToken)) {
     throw new Exception("Invalid signature - cannot extend");
   }

   if (claims.getExpirationTime().before(new java.util.Date())) {
     throw new Exception("Token expired - cannot extend");
   }

   if (isRevoked(oldToken)) {
     throw new Exception("Token revoked - cannot extend");
   }

   // Extract existing claims from old token (preserve custom claims)
   Map<String, Object> existingClaims = new HashMap<>(claims.getClaims());

   // Remove standard claims that will be regenerated
   existingClaims.remove("iss"); // Issuer
   existingClaims.remove("aud"); // Audience
   existingClaims.remove("exp"); // Expiration
   existingClaims.remove("iat"); // Issued at
   existingClaims.remove("jti"); // JWT ID

   // Generate new token with existing claims
   String newToken = signer.sign(existingClaims, issuer, audiences, ttl);

   // Store metadata for new token
   var newJwt = SignedJWT.parse(newToken);
   var newJti = java.util.UUID.fromString(newJwt.getJWTClaimsSet().getJWTID());

   var meta = new CustomJwtMetadata();
   meta.setJwtUuid(newJti);
   meta.setIssuedAt(newJwt.getJWTClaimsSet().getIssueTime().toInstant());
   meta.setExpiresAt(newJwt.getJWTClaimsSet().getExpirationTime().toInstant());
   meta.setClaimKeys(String.join(",", existingClaims.keySet()));
   metaRepo.save(meta);

   // Revoke the old token
   denylist(oldToken);

   return newToken;
 }

 public JwtValidationResponse validate(String token, String expectedAudience, String expectedIssuer) throws Exception {
   try {
     // Parse JWT and extract claims
     var jwt = SignedJWT.parse(token);
     var claims = jwt.getJWTClaimsSet();

     // Check signature first
     if (!signer.verify(token)) {
       return new JwtValidationResponse(false, false, "Invalid signature");
     }

     // Check expiration
     if (claims.getExpirationTime().before(new java.util.Date())) {
       return new JwtValidationResponse(false, false, "Token expired");
     }

     // Check if token is revoked
     if (isRevoked(token)) {
       return new JwtValidationResponse(false, false, "Token revoked");
     }

     // Check audience if specified
     if (expectedAudience != null) {
       List<String> tokenAudiences = claims.getAudience();
       if (tokenAudiences == null || !tokenAudiences.contains(expectedAudience)) {
         return new JwtValidationResponse(false, false, "Invalid audience");
       }
     }

     // Check issuer if specified
     if (expectedIssuer != null && !expectedIssuer.equals(claims.getIssuer())) {
       return new JwtValidationResponse(false, false, "Invalid issuer");
     }

     // Token is valid - build success response
     var response = new JwtValidationResponse(true, true, "Valid");
     response.setSubject(claims.getSubject());
     response.setIssuer(claims.getIssuer());
     List<String> audiences = claims.getAudience();
     response.setAudience(audiences != null && !audiences.isEmpty() ? String.join(",", audiences) : null);
     response.setExpiresAt(claims.getExpirationTime().toInstant());
     response.setIssuedAt(claims.getIssueTime().toInstant());
     response.setJwtId(claims.getJWTID());
     response.setClaims(claims.getClaims());

     return response;
   } catch (java.text.ParseException e) {
     return new JwtValidationResponse(false, false, "Invalid token format");
   } catch (Exception e) {
     // Log full error for debugging but return clean message
     System.err.println("JWT validation error: " + e.getMessage());
     return new JwtValidationResponse(false, false, "Invalid token");
   }
 }
}
