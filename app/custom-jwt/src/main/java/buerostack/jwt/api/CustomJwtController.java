package buerostack.jwt.api;
import buerostack.config.JwtSignerService; import buerostack.jwt.service.CustomJwtService; import com.nimbusds.jwt.SignedJWT;
import org.springframework.http.HttpHeaders; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/jwt/custom") public class CustomJwtController {
 private final CustomJwtService service; private final JwtSignerService signer; public CustomJwtController(CustomJwtService s, JwtSignerService j){ this.service=s; this.signer=j; }
 @PostMapping("/generate") public ResponseEntity<?> generate(@RequestBody Map<String,Object> body) throws Exception {
   String jwtName = (String) body.getOrDefault("JWTName","JWTTOKEN"); Map<String,Object> content=(Map<String,Object>)body.getOrDefault("content", new HashMap<>());
   long ttlMin = ((Number) body.getOrDefault("expirationInMinutes",60)).longValue(); String token = service.generate(jwtName, content, "tim", "tim-audience", ttlMin*60);
   return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, jwtName + "=" + token + "; Path=/; HttpOnly").body(Map.of("status","ok")); }
 @PostMapping("/verify") public ResponseEntity<?> verify(@RequestHeader(value="Cookie",required=false) String cookie,@RequestHeader(value="Authorization",required=false) String auth){
   String token = extract(cookie, auth); if(token==null || !signer.verify(token) || service.isRevoked(token)) return ResponseEntity.status(401).body(Map.of("valid",false)); return ResponseEntity.ok(Map.of("valid",true)); }
 @PostMapping("/userinfo") public ResponseEntity<?> userinfo(@RequestHeader(value="Cookie",required=false) String cookie,@RequestHeader(value="Authorization",required=false) String auth) throws Exception {
   String token = extract(cookie, auth); if(token==null || !signer.verify(token) || service.isRevoked(token)) return ResponseEntity.status(401).body(Map.of("error","invalid_token"));
   var claims = SignedJWT.parse(token).getJWTClaimsSet().getClaims(); return ResponseEntity.ok(claims); }
 @PostMapping("/extend") public ResponseEntity<?> extend(@RequestHeader(value="Cookie",required=false) String cookie,@RequestHeader(value="Authorization",required=false) String auth) throws Exception {
   String token = extract(cookie, auth); if(token==null || !signer.verify(token) || service.isRevoked(token)) return ResponseEntity.status(401).body(Map.of("error","invalid_token"));
   var claims = SignedJWT.parse(token).getJWTClaimsSet().getClaims(); Map<String,Object> identity = new HashMap<>(claims); identity.keySet().removeAll(Set.of("iss","iat","exp","aud","jti"));
   String jwtName = cookieName(cookie); String newToken = service.generate(jwtName, identity, "tim", "tim-audience", 3600);
   return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, jwtName + "=" + newToken + "; Path=/; HttpOnly").body(Map.of("status","extended")); }
 @PostMapping("/blacklist") public ResponseEntity<?> blacklist(@RequestHeader(value="Cookie",required=false) String cookie,@RequestHeader(value="Authorization",required=false) String auth) throws Exception {
   String token = extract(cookie, auth); if(token==null || !signer.verify(token)) return ResponseEntity.status(401).body(Map.of("error","invalid_token")); service.denylist(token); return ResponseEntity.ok(Map.of("status","denylisted")); }
 private static String extract(String cookie,String auth){ if(auth!=null && auth.startsWith("Bearer ")) return auth.substring(7); if(cookie!=null){ for(String c:cookie.split(";")){ String[] kv=c.trim().split("=",2); if(kv.length==2) return kv[1]; } } return null; }
 private static String cookieName(String cookie){ if(cookie!=null){ String[] c=cookie.trim().split("=",2); return c[0].trim(); } return "JWTTOKEN"; }
}