package buerostack.jwt.api;

import buerostack.jwt.config.JwtCustomConfig;
import buerostack.jwt.service.CustomJwtService;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/jwt/custom")
public class CustomJwtController {

    private final CustomJwtService customJwtService;
    private final JwtCustomConfig jwtConfig;

    public CustomJwtController(CustomJwtService customJwtService, JwtCustomConfig jwtConfig) {
        this.customJwtService = customJwtService;
        this.jwtConfig = jwtConfig;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody CustomJwtGenerateRequest request) throws Exception {
        // Determine audience
        List<String> requestedAudiences = request.getAudienceAsList();
        List<String> finalAudiences;

        if (jwtConfig.isValidationEnabled()) {
            if (requestedAudiences == null || requestedAudiences.isEmpty()) {
                // Use default audience when validation is enabled but no audience specified
                finalAudiences = List.of(jwtConfig.getDefaultAudience());
            } else {
                // Validate requested audiences against allowed list
                if (!jwtConfig.isAudienceAllowed(requestedAudiences)) {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "invalid_audience");
                    errorResponse.put("message", "One or more requested audiences are not allowed");
                    errorResponse.put("allowed_audiences", jwtConfig.getAllowedAudiences());
                    return ResponseEntity.badRequest().body(errorResponse);
                }
                finalAudiences = requestedAudiences;
            }
        } else {
            // When validation is disabled, use requested audience or fallback to legacy default
            if (requestedAudiences == null || requestedAudiences.isEmpty()) {
                finalAudiences = List.of("tim-audience"); // Legacy default for backward compatibility
            } else {
                finalAudiences = requestedAudiences;
            }
        }

        String token = customJwtService.generate(
            request.getJwtName(),
            request.getContent(),
            jwtConfig.getIssuer(),
            finalAudiences,
            request.getExpirationInMinutes() * 60
        );

        Instant expiresAt = Instant.now().plus(request.getExpirationInMinutes(), ChronoUnit.MINUTES);
        TokenResponse tokenResponse = new TokenResponse("created", request.getJwtName(), token, expiresAt);

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();
        if (request.getSetCookie()) {
            responseBuilder.header(HttpHeaders.SET_COOKIE, request.getJwtName() + "=" + token + "; Path=/; HttpOnly");
        }

        return responseBuilder.body(tokenResponse);
    }

    @PostMapping("/validate")
    public ResponseEntity<JwtValidationResponse> validate(@RequestBody JwtValidationRequest request) {
        try {
            if (request.getToken() == null || request.getToken().trim().isEmpty()) {
                JwtValidationResponse errorResponse = new JwtValidationResponse(false, false, "Token is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            JwtValidationResponse response = customJwtService.validate(
                request.getToken(),
                request.getAudience(),
                request.getIssuer()
            );

            // Return appropriate HTTP status based on validation result
            if (response.isValid()) {
                return ResponseEntity.ok(response);
            } else {
                // Invalid token but request was valid - return 401 Unauthorized
                return ResponseEntity.status(401).body(response);
            }
        } catch (Exception e) {
            JwtValidationResponse errorResponse = new JwtValidationResponse(false, false, "Invalid token: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/validate/boolean")
    public ResponseEntity<String> validateBoolean(@RequestBody JwtValidationRequest request) {
        try {
            if (request.getToken() == null || request.getToken().trim().isEmpty()) {
                return ResponseEntity
                    .badRequest()
                    .header("Content-Type", "text/plain")
                    .body("false");
            }

            JwtValidationResponse response = customJwtService.validate(
                request.getToken(),
                request.getAudience(),
                request.getIssuer()
            );

            boolean isValid = response.isValid() && response.isActive();

            return ResponseEntity
                .status(isValid ? 200 : 401)
                .header("Content-Type", "text/plain")
                .body(isValid ? "true" : "false");
        } catch (Exception e) {
            return ResponseEntity
                .badRequest()
                .header("Content-Type", "text/plain")
                .body("false");
        }
    }

    @PostMapping("/revoke")
    public ResponseEntity<?> revoke(@RequestBody JwtValidationRequest request) {
        try {
            if (request.getToken() == null || request.getToken().trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "invalid_request");
                errorResponse.put("message", "Token is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            customJwtService.denylist(request.getToken());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "revoked");
            response.put("message", "Token has been successfully revoked");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "revocation_failed");
            errorResponse.put("message", "Failed to revoke token: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/extend")
    public ResponseEntity<?> extend(@RequestBody JwtExtendRequest request) {
        try {
            // Validate request
            if (request.getToken() == null || request.getToken().trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "invalid_request");
                errorResponse.put("message", "Token is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Default expiration to 60 minutes if not specified
            Integer expirationMinutes = request.getExpirationInMinutes() != null ?
                request.getExpirationInMinutes() : 60;

            if (expirationMinutes <= 0) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "invalid_request");
                errorResponse.put("message", "Expiration time must be positive");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Extract original audience from the token to preserve it
            List<String> finalAudiences;
            try {
                var jwt = SignedJWT.parse(request.getToken());
                var claims = jwt.getJWTClaimsSet();
                List<String> originalAudiences = claims.getAudience();

                if (originalAudiences != null && !originalAudiences.isEmpty()) {
                    finalAudiences = originalAudiences;
                } else {
                    // Fallback to default if no audience in original token
                    if (jwtConfig.isValidationEnabled()) {
                        finalAudiences = List.of(jwtConfig.getDefaultAudience());
                    } else {
                        finalAudiences = List.of("tim-audience"); // Legacy default
                    }
                }
            } catch (Exception e) {
                // If parsing fails, it will be caught by extend method anyway
                // Use default audience as fallback
                if (jwtConfig.isValidationEnabled()) {
                    finalAudiences = List.of(jwtConfig.getDefaultAudience());
                } else {
                    finalAudiences = List.of("tim-audience"); // Legacy default
                }
            }

            // Extend the token
            String newToken = customJwtService.extend(
                request.getToken(),
                jwtConfig.getIssuer(),
                finalAudiences,
                expirationMinutes * 60
            );

            Instant expiresAt = Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES);
            TokenResponse tokenResponse = new TokenResponse("extended", "EXTENDED_TOKEN", newToken, expiresAt);

            ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();
            if (request.getSetCookie()) {
                responseBuilder.header(HttpHeaders.SET_COOKIE, "EXTENDED_TOKEN=" + newToken + "; Path=/; HttpOnly");
            }

            return responseBuilder.body(tokenResponse);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();

            // Determine appropriate error type based on exception message
            if (e.getMessage().contains("expired") || e.getMessage().contains("revoked") ||
                e.getMessage().contains("Invalid signature")) {
                errorResponse.put("error", "extend_denied");
                errorResponse.put("message", e.getMessage());
                return ResponseEntity.status(401).body(errorResponse);
            } else {
                errorResponse.put("error", "extend_failed");
                errorResponse.put("message", "Failed to extend token: " + e.getMessage());
                return ResponseEntity.badRequest().body(errorResponse);
            }
        }
    }
}
