package buerostack.introspection.api;

import buerostack.introspection.dto.IntrospectionRequest;
import buerostack.introspection.dto.IntrospectionResponse;
import buerostack.introspection.service.TokenIntrospectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * RFC 7662 compliant token introspection endpoint
 */
@RestController
@RequestMapping("/introspect")
public class TokenIntrospectionController {

    private static final Logger logger = LoggerFactory.getLogger(TokenIntrospectionController.class);

    private final TokenIntrospectionService introspectionService;

    public TokenIntrospectionController(TokenIntrospectionService introspectionService) {
        this.introspectionService = introspectionService;
    }

    /**
     * RFC 7662 Token Introspection Endpoint
     *
     * POST /introspect
     * Content-Type: application/x-www-form-urlencoded
     *
     * Required parameter: token
     * Optional parameter: token_type_hint
     */
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IntrospectionResponse> introspect(
            @RequestParam("token") String token,
            @RequestParam(value = "token_type_hint", required = false) String tokenTypeHint) {

        logger.debug("Token introspection request received");

        try {
            IntrospectionRequest request = new IntrospectionRequest(token, tokenTypeHint);
            IntrospectionResponse response = introspectionService.introspect(request);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error processing introspection request: {}", e.getMessage());
            return ResponseEntity.ok(IntrospectionResponse.inactive());
        }
    }

    /**
     * Alternative JSON endpoint for easier client integration
     * (Not part of RFC 7662 but commonly supported)
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IntrospectionResponse> introspectJson(
            @RequestBody IntrospectionRequest request) {

        logger.debug("Token introspection request received (JSON)");

        try {
            IntrospectionResponse response = introspectionService.introspect(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error processing JSON introspection request: {}", e.getMessage());
            return ResponseEntity.ok(IntrospectionResponse.inactive());
        }
    }

    /**
     * Get supported token types (informational endpoint)
     */
    @GetMapping("/types")
    public ResponseEntity<Map<String, String>> getSupportedTokenTypes() {
        Map<String, String> supportedTypes = introspectionService.getSupportedTokenTypes();
        return ResponseEntity.ok(supportedTypes);
    }
}