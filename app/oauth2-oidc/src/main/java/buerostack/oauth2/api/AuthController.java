package buerostack.oauth2.api;

import buerostack.oauth2.config.OAuth2ProvidersConfig.OAuth2ProvidersProperties;
import buerostack.oauth2.service.OAuth2AuthenticationService;
import buerostack.oauth2.service.OAuth2AuthenticationService.AuthenticationInitiation;
import buerostack.oauth2.service.OAuth2AuthenticationService.CallbackValidation;
import buerostack.oauth2.service.OAuth2AuthenticationService.ProviderInfo;
import buerostack.oauth2.service.OAuth2TokenService;
import buerostack.oauth2.service.OAuth2TokenService.TokenExchangeResult;
import buerostack.oauth2.service.JwtValidationService;
import buerostack.oauth2.service.JwtValidationService.JwtValidationResult;
import buerostack.oauth2.service.SessionManagementService;
import buerostack.oauth2.service.SessionManagementService.SessionValidationResult;
import buerostack.oauth2.service.UserProfileService;
import buerostack.oauth2.model.AuthSession;
import buerostack.oauth2.model.UserProfile;
import jakarta.servlet.http.HttpServletRequest;
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
    private final OAuth2TokenService tokenService;
    private final JwtValidationService jwtValidationService;
    private final SessionManagementService sessionService;
    private final UserProfileService userProfileService;

    @Autowired
    public AuthController(OAuth2ProvidersProperties providersProperties,
                         OAuth2AuthenticationService authService,
                         OAuth2TokenService tokenService,
                         JwtValidationService jwtValidationService,
                         SessionManagementService sessionService,
                         UserProfileService userProfileService) {
        this.providersProperties = providersProperties;
        this.authService = authService;
        this.tokenService = tokenService;
        this.jwtValidationService = jwtValidationService;
        this.sessionService = sessionService;
        this.userProfileService = userProfileService;
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
                                          @RequestParam(required = false) String error_description,
                                          HttpServletRequest request) {
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

            // Exchange authorization code for tokens
            TokenExchangeResult tokenResult = tokenService.exchangeCodeForTokens(
                    providerId, validation.getCode(), validation.getNonce());

            if (!tokenResult.isSuccess()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "token_exchange_failed",
                                   "message", tokenResult.getMessage(),
                                   "provider", providerId,
                                   "details", tokenResult.getErrorDetails() != null ?
                                            tokenResult.getErrorDetails() : Map.of()));
            }

            // Validate ID token if present
            JwtValidationResult jwtResult = null;
            UserProfile userProfile = null;

            if (tokenResult.getTokenResponse().getIdToken() != null) {
                jwtResult = jwtValidationService.validateIdToken(
                        providerId, tokenResult.getTokenResponse().getIdToken(), validation.getNonce());

                if (!jwtResult.isValid()) {
                    logger.warn("ID token validation failed for provider {}: {}", providerId, jwtResult.getMessage());
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "invalid_id_token",
                                       "message", jwtResult.getMessage(),
                                       "provider", providerId));
                }

                // Extract user profile from validated ID token
                try {
                    ProviderConfig providerConfig = providersProperties.getProvider(providerId);
                    String[] scopes = providerConfig.getScopes().toArray(new String[0]);
                    userProfile = userProfileService.extractUserProfile(
                            providerId, jwtResult.getClaimsSet(), null, scopes);
                } catch (Exception e) {
                    logger.warn("Error extracting user profile for provider {}: {}", providerId, e.getMessage());
                }
            }

            // Create session
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            AuthSession session = sessionService.createSession(
                    providerId,
                    tokenResult.getTokenResponse(),
                    jwtResult != null ? jwtResult.getClaimsSet() : null,
                    ipAddress,
                    userAgent
            );

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "authentication_success");
            response.put("provider", providerId);
            response.put("session_id", session.getSessionId());
            response.put("expires_at", session.getExpiresAt());

            if (userProfile != null) {
                response.put("user_profile", userProfile);
            }

            // Don't expose raw tokens in response (they're stored in session)
            response.put("message", "Authentication completed successfully");

            logger.info("OAuth2 authentication completed successfully for provider: {}", providerId);

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

    /**
     * Validate session
     * GET /auth/session/validate
     */
    @GetMapping("/session/validate")
    public ResponseEntity<?> validateSession(@RequestParam String session_id) {
        try {
            SessionValidationResult validation = sessionService.validateSession(session_id);

            if (!validation.isValid()) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "invalid_session",
                                   "message", validation.getMessage()));
            }

            AuthSession session = validation.getSession();
            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("session_id", session.getSessionId());
            response.put("user_id", session.getUserId());
            response.put("provider", session.getProvider());
            response.put("expires_at", session.getExpiresAt());
            response.put("last_activity", session.getLastActivity());

            if (session.getSessionMetadata() != null) {
                response.put("authentication_method", session.getSessionMetadata().getAuthenticationMethod());
                response.put("level_of_assurance", session.getSessionMetadata().getLevelOfAssurance());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error validating session: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "validation_failed",
                               "message", "Failed to validate session"));
        }
    }

    /**
     * Get user profile from session
     * GET /auth/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(@RequestParam String session_id) {
        try {
            SessionValidationResult validation = sessionService.validateSession(session_id);

            if (!validation.isValid()) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "invalid_session",
                                   "message", validation.getMessage()));
            }

            AuthSession session = validation.getSession();

            // TODO: Extract full user profile from stored tokens
            // For now, return basic session information
            Map<String, Object> profile = new HashMap<>();
            profile.put("user_id", session.getUserId());
            profile.put("provider", session.getProvider());

            if (session.getSessionMetadata() != null) {
                profile.put("authentication_method", session.getSessionMetadata().getAuthenticationMethod());
                profile.put("level_of_assurance", session.getSessionMetadata().getLevelOfAssurance());
            }

            return ResponseEntity.ok(profile);

        } catch (Exception e) {
            logger.error("Error getting user profile: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "profile_failed",
                               "message", "Failed to get user profile"));
        }
    }

    /**
     * Logout session
     * POST /auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam String session_id,
                                   @RequestParam(required = false) String reason) {
        try {
            boolean revoked = sessionService.revokeSession(session_id,
                    reason != null ? reason : "User initiated logout");

            if (!revoked) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "session_not_found",
                                   "message", "Session not found or already revoked"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "logged_out");
            response.put("message", "Session logged out successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error during logout: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "logout_failed",
                               "message", "Failed to logout session"));
        }
    }

    /**
     * Helper method to extract client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}