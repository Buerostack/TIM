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
import java.time.format.DateTimeParseException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import buerostack.jwt.api.JwtListRequest;
import buerostack.jwt.api.JwtListResponse;
import buerostack.jwt.api.JwtTokenSummary;
@Service public class CustomJwtService {
 private final JwtSignerService signer; private final CustomDenylistRepo denylistRepo; private final CustomJwtMetadataRepo metaRepo;
 public CustomJwtService(JwtSignerService s, CustomDenylistRepo d, CustomJwtMetadataRepo m){ this.signer=s; this.denylistRepo=d; this.metaRepo=m; }
 public String generate(String jwtName, Map<String,Object> claims, String issuer, List<String> audiences, long ttl) throws Exception {
   // Add token_type claim for introspection
   Map<String,Object> claimsWithType = new HashMap<>(claims);
   claimsWithType.put("token_type", "custom_jwt");

   String token = signer.sign(claimsWithType, issuer, audiences, ttl);
   var jwt = SignedJWT.parse(token);
   var jti = java.util.UUID.fromString(jwt.getJWTClaimsSet().getJWTID());

   var meta = new CustomJwtMetadata(
       jti,
       String.join(",", claims.keySet()),
       jwt.getJWTClaimsSet().getIssueTime().toInstant(),
       jwt.getJWTClaimsSet().getExpirationTime().toInstant(),
       jti // For new tokens, original_jwt_uuid is the same as jwt_uuid
   );
   meta.setSubject(jwt.getJWTClaimsSet().getSubject());
   meta.setJwtName(jwtName);
   meta.setIssuer(jwt.getJWTClaimsSet().getIssuer());
   meta.setAudience(jwt.getJWTClaimsSet().getAudience() != null ? String.join(",", jwt.getJWTClaimsSet().getAudience()) : null);
   metaRepo.save(meta);
   return token;
 }
 public boolean isRevoked(String token){ try{ var jwt = SignedJWT.parse(token); var jti = java.util.UUID.fromString(jwt.getJWTClaimsSet().getJWTID());
   return denylistRepo.findById(jti).isPresent(); }catch(Exception e){ return true; } }
 @Transactional public boolean denylist(String token) throws Exception { return denylist(token, null); }

 @Transactional public boolean denylist(String token, String reason) throws Exception { var jwt = SignedJWT.parse(token); var jti = java.util.UUID.fromString(jwt.getJWTClaimsSet().getJWTID());

   // Check if already revoked
   if (denylistRepo.findById(jti).isPresent()) {
       return false; // Already revoked
   }

   var dl = new CustomDenylist(); dl.setJwtUuid(jti); dl.setDenylistedAt(Instant.now()); dl.setExpiresAt(jwt.getJWTClaimsSet().getExpirationTime().toInstant()); dl.setReason(reason); denylistRepo.save(dl);
   return true; // Newly revoked
 }

 @Transactional public Map<String, Object> bulkDenylist(List<String> tokens) {
   return bulkDenylist(tokens, null);
 }

 @Transactional public Map<String, Object> bulkDenylist(List<String> tokens, String reason) {
   Map<String, Object> result = new HashMap<>();
   List<String> newlyRevoked = new ArrayList<>();
   List<String> alreadyRevoked = new ArrayList<>();
   List<Map<String, String>> failed = new ArrayList<>();

   for (String token : tokens) {
     try {
       boolean wasNewlyRevoked = denylist(token, reason);
       String tokenPrefix = token.substring(0, Math.min(20, token.length())) + "...";
       if (wasNewlyRevoked) {
         newlyRevoked.add(tokenPrefix);
       } else {
         alreadyRevoked.add(tokenPrefix);
       }
     } catch (Exception e) {
       Map<String, String> failure = new HashMap<>();
       failure.put("token", token.substring(0, Math.min(20, token.length())) + "...");
       failure.put("reason", e.getMessage());
       failed.add(failure);
     }
   }

   result.put("total", tokens.size());
   result.put("newly_revoked", newlyRevoked.size());
   result.put("already_revoked", alreadyRevoked.size());
   result.put("failed", failed.size());
   result.put("newly_revoked_tokens", newlyRevoked);
   result.put("already_revoked_tokens", alreadyRevoked);
   result.put("failed_tokens", failed);

   return result;
 }

 @Transactional public String extend(String oldToken, String issuer, List<String> audiences, long ttl) throws Exception {
   // Parse and validate the old token
   var jwt = SignedJWT.parse(oldToken);
   var claims = jwt.getJWTClaimsSet();
   var oldJti = java.util.UUID.fromString(jwt.getJWTClaimsSet().getJWTID());

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

   // Find the current version of this JWT to get the original JWT UUID
   var currentMeta = metaRepo.findCurrentVersionByJwtUuid(oldJti)
       .orElseThrow(() -> new Exception("JWT metadata not found"));

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
   var newJwt = SignedJWT.parse(newToken);
   var newJti = java.util.UUID.fromString(newJwt.getJWTClaimsSet().getJWTID());

   // Create new metadata record for extended token (INSERT operation)
   var extendedMeta = new CustomJwtMetadata(
       newJti, // New JWT UUID for the extended token
       String.join(",", existingClaims.keySet()),
       newJwt.getJWTClaimsSet().getIssueTime().toInstant(),
       newJwt.getJWTClaimsSet().getExpirationTime().toInstant(),
       currentMeta.getOriginalJwtUuid() // Reference to the original JWT in the chain
   );
   extendedMeta.setSupersedes(currentMeta.getId()); // Reference to the previous version
   extendedMeta.setSubject(newJwt.getJWTClaimsSet().getSubject());
   extendedMeta.setJwtName(existingClaims.get("jwt_name") != null ? existingClaims.get("jwt_name").toString() : null);
   extendedMeta.setAudience(newJwt.getJWTClaimsSet().getAudience() != null ? String.join(",", newJwt.getJWTClaimsSet().getAudience()) : null);
   extendedMeta.setIssuer(newJwt.getJWTClaimsSet().getIssuer());
   metaRepo.save(extendedMeta);

   // Add old token to denylist (INSERT operation)
   denylist(oldToken);

   return newToken;
 }

 public JwtListResponse listUserTokens(String subject, JwtListRequest request) {
   try {
     // Parse date filters
     Instant issuedAfter = parseInstant(request.getIssuedAfter());
     Instant issuedBefore = parseInstant(request.getIssuedBefore());
     Instant expiresAfter = parseInstant(request.getExpiresAfter());
     Instant expiresBefore = parseInstant(request.getExpiresBefore());

     // Create pagination
     int page = request.getOffset() != null ? request.getOffset() : 0;
     int size = request.getLimit() != null ? request.getLimit() : 20;
     Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "issuedAt"));

     // Query with filters - temporarily use simple method
     Page<CustomJwtMetadata> resultPage = metaRepo.findBySubject(subject, pageable);

     // Convert to response format
     List<JwtTokenSummary> tokens = resultPage.getContent().stream()
       .map(this::convertToTokenSummary)
       .toList();

     // Build response
     JwtListResponse response = new JwtListResponse();
     response.setTokens(tokens);

     JwtListResponse.PaginationInfo pagination = new JwtListResponse.PaginationInfo();
     pagination.setTotal(resultPage.getTotalElements());
     pagination.setPage(resultPage.getNumber());
     pagination.setSize(resultPage.getSize());
     pagination.setTotalPages(resultPage.getTotalPages());
     response.setPagination(pagination);

     return response;
   } catch (Exception e) {
     // Return empty response on error
     JwtListResponse response = new JwtListResponse();
     response.setTokens(new ArrayList<>());
     JwtListResponse.PaginationInfo pagination = new JwtListResponse.PaginationInfo();
     pagination.setTotal(0L);
     pagination.setPage(0);
     pagination.setSize(0);
     pagination.setTotalPages(0);
     response.setPagination(pagination);
     return response;
   }
 }

 private JwtTokenSummary convertToTokenSummary(CustomJwtMetadata metadata) {
   JwtTokenSummary summary = new JwtTokenSummary();
   summary.setJti(metadata.getJwtUuid().toString());
   summary.setSubject(metadata.getSubject());
   summary.setJwtName(metadata.getJwtName());
   summary.setIssuedAt(metadata.getIssuedAt());
   summary.setExpiresAt(metadata.getExpiresAt());
   summary.setIssuer(metadata.getIssuer());
   summary.setAudience(metadata.getAudience());

   // Determine status
   boolean isExpired = metadata.getExpiresAt().isBefore(Instant.now());
   boolean isRevoked = denylistRepo.findById(metadata.getJwtUuid()).isPresent();

   if (isRevoked) {
     summary.setStatus("revoked");
     var denylistEntry = denylistRepo.findById(metadata.getJwtUuid()).orElse(null);
     if (denylistEntry != null) {
       summary.setRevokedAt(denylistEntry.getDenylistedAt());
       summary.setRevocationReason(denylistEntry.getReason());
     }
   } else if (isExpired) {
     summary.setStatus("expired");
   } else {
     summary.setStatus("active");
   }

   return summary;
 }

 private Instant parseInstant(String dateString) {
   if (dateString == null || dateString.trim().isEmpty()) {
     return null;
   }
   try {
     return Instant.parse(dateString);
   } catch (DateTimeParseException e) {
     return null;
   }
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
