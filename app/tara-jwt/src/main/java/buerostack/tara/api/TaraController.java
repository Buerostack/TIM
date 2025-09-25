package buerostack.tara.api; import buerostack.config.BuildConstants; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*; import java.net.URI; import java.util.Map;
@RestController @RequestMapping("/tara") public class TaraController {
 @GetMapping("/login") public ResponseEntity<?> login(){ return ResponseEntity.status(302).location(URI.create("/tara/callback?code=dummy&state=dummy")).build(); }
 @GetMapping("/callback") public ResponseEntity<?> cb(@RequestParam String code,@RequestParam String state){ return ResponseEntity.ok(Map.of("status","callback-received","cookieName", BuildConstants.TIM_TARA_JWT_NAME)); }
 @PostMapping("/logout") public ResponseEntity<?> logout(){ return ResponseEntity.ok(Map.of("status","logged-out")); } }