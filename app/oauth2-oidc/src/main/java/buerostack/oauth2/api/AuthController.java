package buerostack.oauth2.api;

import buerostack.oauth2.config.OAuth2ProvidersConfig.OAuth2ProvidersProperties;
import buerostack.oauth2.service.OAuth2AuthenticationService;
import buerostack.oauth2.service.OAuth2AuthenticationService.AuthenticationInitiation;
import buerostack.oauth2.service.OAuth2AuthenticationService.CallbackValidation;
import buerostack.oauth2.service.OAuth2AuthenticationService.ProviderInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for OAuth2/OIDC authentication endpoints
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final OAuth2ProvidersProperties providersProperties;
    private final OAuth2AuthenticationService authService;

    @Autowired
    public AuthController(OAuth2ProvidersProperties providersProperties,
                         OAuth2AuthenticationService authService) {
        this.providersProperties = providersProperties;
        this.authService = authService;
    }

    /**
     * List available OAuth2 providers
     * GET /auth/providers
     */
    @GetMapping("/providers")
    public ResponseEntity<?> listProviders() {
        try {
            Map<String, ProviderInfo> providers = authService.getAvailableProviders();

            Map<String, Object> response = new HashMap<>();
            response.put("providers", providers);
            response.put("total", providers.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error listing providers: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "internal_error",
                               "message", "Failed to list providers"));
        }
    }

    /**
     * Get specific provider information
     * GET /auth/providers/{providerId}
     */
    @GetMapping("/providers/{providerId}")
    public ResponseEntity<?> getProviderInfo(@PathVariable String providerId) {
        try {
            if (!providersProperties.isProviderAvailable(providerId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "provider_not_found",
                                   "message", "Provider not found or disabled: " + providerId,
                                   "available_providers", authService.getAvailableProviders().keySet()));
            }

            Map<String, ProviderInfo> providers = authService.getAvailableProviders();
            ProviderInfo providerInfo = providers.get(providerId);

            if (providerInfo == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "provider_not_found",
                                   "message", "Provider not found: " + providerId));
            }

            return ResponseEntity.ok(providerInfo);

        } catch (Exception e) {
            logger.error("Error getting provider info for {}: {}", providerId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "internal_error",
                               "message", "Failed to get provider information"));
        }
    }

    /**
     * Initiate OAuth2 authentication
     * GET /auth/login/{providerId}
     */
    @GetMapping("/login/{providerId}")
    public ResponseEntity<?> initiateAuth(@PathVariable String providerId,
                                         @RequestParam(required = false) String redirect_uri) {
        try {
            // Validate provider
            if (!providersProperties.isProviderAvailable(providerId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "provider_not_available",
                                   "message", "Provider not found or disabled: " + providerId,
                                   "available_providers", authService.getAvailableProviders().keySet()));
            }

            // Initiate authentication
            AuthenticationInitiation initiation = authService.initiateAuthentication(providerId, redirect_uri);

            // Return redirect response
            Map<String, Object> response = new HashMap<>();
            response.put("authorization_url", initiation.getAuthorizationUrl());
            response.put("provider", providerId);
            response.put("state", initiation.getState());

            logger.info("Authentication initiated for provider: {}", providerId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error initiating authentication for provider {}: {}", providerId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "authentication_failed",
                               "message", "Failed to initiate authentication: " + e.getMessage()));
        }
    }

    /**
     * Handle OAuth2 callback
     * GET /auth/callback/{providerId}
     */
    @GetMapping("/callback/{providerId}")
    public ResponseEntity<?> handleCallback(@PathVariable String providerId,
                                          @RequestParam(required = false) String code,
                                          @RequestParam(required = false) String state,
                                          @RequestParam(required = false) String error,
                                          @RequestParam(required = false) String error_description) {
        try {
            logger.info("Handling OAuth2 callback for provider: {}", providerId);

            // Validate provider
            if (!providersProperties.isProviderAvailable(providerId)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "provider_not_available",
                                   "message", "Provider not found or disabled: " + providerId));
            }

            // Handle OAuth2 error responses
            if (error != null) {
                logger.warn("OAuth2 error in callback: {} - {}", error, error_description);
                return ResponseEntity.badRequest()
                        .body(Map.of("error", error,
                                   "message", error_description != null ? error_description : "Authentication failed",
                                   "provider", providerId));
            }

            // Validate callback parameters
            CallbackValidation validation = authService.validateCallback(providerId, code, state, error);

            if (!validation.isValid()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "invalid_callback",
                                   "message", validation.getMessage(),
                                   "provider", providerId));
            }

            // TODO: Exchange authorization code for tokens
            // For now, return success with code (will implement token exchange next)
            Map<String, Object> response = new HashMap<>();
            response.put("status", "callback_success");
            response.put("provider", providerId);
            response.put("code", validation.getCode());
            response.put("message", "Authorization callback processed successfully");

            logger.info("OAuth2 callback processed successfully for provider: {}", providerId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error handling callback for provider {}: {}", providerId, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "callback_failed",
                               "message", "Failed to process callback: " + e.getMessage(),
                               "provider", providerId));
        }
    }

    /**
     * Health check endpoint
     * GET /auth/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("service", "oauth2-authentication");
        health.put("timestamp", System.currentTimeMillis());

        Map<String, ProviderInfo> providers = authService.getAvailableProviders();
        health.put("available_providers", providers.size());
        health.put("provider_ids", providers.keySet());

        return ResponseEntity.ok(health);
    }
}