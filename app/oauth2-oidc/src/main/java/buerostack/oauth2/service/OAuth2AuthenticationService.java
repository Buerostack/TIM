package buerostack.oauth2.service;

import buerostack.oauth2.config.OAuth2ProvidersConfig.OAuth2ProvidersProperties;
import buerostack.oauth2.model.OidcDiscovery;
import buerostack.oauth2.model.ProviderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core OAuth2 authentication service handling the authorization code flow
 */
@Service
public class OAuth2AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationService.class);

    private final OAuth2ProvidersProperties providersProperties;
    private final OidcDiscoveryService discoveryService;
    private final SecureRandom secureRandom = new SecureRandom();

    // Temporary storage for state and nonce values (in production, use Redis or database)
    private final Map<String, StateInfo> stateStorage = new ConcurrentHashMap<>();

    @Autowired
    public OAuth2AuthenticationService(OAuth2ProvidersProperties providersProperties,
                                     OidcDiscoveryService discoveryService) {
        this.providersProperties = providersProperties;
        this.discoveryService = discoveryService;
    }

    /**
     * Initiate OAuth2 authentication flow
     */
    public AuthenticationInitiation initiateAuthentication(String providerId, String clientRedirectUri) {
        logger.info("Initiating OAuth2 authentication for provider: {}", providerId);

        // Validate provider
        ProviderConfig providerConfig = providersProperties.getProvider(providerId);
        if (providerConfig == null || !providerConfig.getEnabled()) {
            throw new RuntimeException("Provider not found or disabled: " + providerId);
        }

        // Get discovery document
        OidcDiscovery discovery = discoveryService.getDiscovery(providerId, providerConfig);

        // Generate state and nonce
        String state = generateSecureRandomString(32);
        String nonce = generateSecureRandomString(32);

        // Store state information for validation
        StateInfo stateInfo = new StateInfo(providerId, nonce, clientRedirectUri, System.currentTimeMillis());
        stateStorage.put(state, stateInfo);

        // Build authorization URL
        String authUrl = buildAuthorizationUrl(discovery, providerConfig, state, nonce, clientRedirectUri);

        logger.info("Generated authorization URL for provider {}: {}", providerId,
                   authUrl.replaceAll("client_id=[^&]+", "client_id=***"));

        return new AuthenticationInitiation(authUrl, state, nonce);
    }

    /**
     * Build OAuth2 authorization URL
     */
    private String buildAuthorizationUrl(OidcDiscovery discovery, ProviderConfig config,
                                       String state, String nonce, String redirectUri) {

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(discovery.getAuthorizationEndpoint())
                .queryParam("response_type", "code")
                .queryParam("client_id", config.getClientId())
                .queryParam("redirect_uri", getCallbackUrl(config.getId()))
                .queryParam("scope", String.join(" ", config.getScopes()))
                .queryParam("state", state)
                .queryParam("nonce", nonce);

        // Add custom parameters if configured
        if (config.getCustomParameters() != null) {
            config.getCustomParameters().forEach(builder::queryParam);
        }

        return builder.build().toUriString();
    }

    /**
     * Get the callback URL for a provider
     */
    private String getCallbackUrl(String providerId) {
        // TODO: Make this configurable based on environment
        return "http://localhost:8085/auth/callback/" + providerId;
    }

    /**
     * Validate authorization callback
     */
    public CallbackValidation validateCallback(String providerId, String code, String state, String error) {
        logger.info("Validating OAuth2 callback for provider: {}", providerId);

        if (error != null) {
            logger.warn("OAuth2 error received in callback: {}", error);
            return new CallbackValidation(false, "Authorization failed: " + error, null, null);
        }

        if (code == null || code.trim().isEmpty()) {
            logger.warn("No authorization code received in callback");
            return new CallbackValidation(false, "No authorization code received", null, null);
        }

        if (state == null || state.trim().isEmpty()) {
            logger.warn("No state parameter received in callback");
            return new CallbackValidation(false, "No state parameter received", null, null);
        }

        // Validate state
        StateInfo stateInfo = stateStorage.remove(state);
        if (stateInfo == null) {
            logger.warn("Invalid or expired state parameter: {}", state);
            return new CallbackValidation(false, "Invalid or expired state parameter", null, null);
        }

        if (!stateInfo.getProviderId().equals(providerId)) {
            logger.warn("State provider mismatch. Expected: {}, Got: {}", stateInfo.getProviderId(), providerId);
            return new CallbackValidation(false, "State provider mismatch", null, null);
        }

        // Check state expiration (5 minutes)
        if (System.currentTimeMillis() - stateInfo.getTimestamp() > 300_000) {
            logger.warn("State parameter expired");
            return new CallbackValidation(false, "State parameter expired", null, null);
        }

        logger.info("OAuth2 callback validation successful for provider: {}", providerId);
        return new CallbackValidation(true, "Validation successful", code, stateInfo.getNonce());
    }

    /**
     * Generate a cryptographically secure random string
     */
    private String generateSecureRandomString(int length) {
        byte[] buffer = new byte[length];
        secureRandom.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    /**
     * Get available providers
     */
    public Map<String, ProviderInfo> getAvailableProviders() {
        Map<String, ProviderInfo> providers = new HashMap<>();

        providersProperties.getEnabledProviders().forEach((id, config) -> {
            providers.put(id, new ProviderInfo(
                config.getId(),
                config.getName(),
                config.getScopes(),
                true // For now, assume all enabled providers are available
            ));
        });

        return providers;
    }

    // Data classes for service responses

    public static class AuthenticationInitiation {
        private final String authorizationUrl;
        private final String state;
        private final String nonce;

        public AuthenticationInitiation(String authorizationUrl, String state, String nonce) {
            this.authorizationUrl = authorizationUrl;
            this.state = state;
            this.nonce = nonce;
        }

        public String getAuthorizationUrl() { return authorizationUrl; }
        public String getState() { return state; }
        public String getNonce() { return nonce; }
    }

    public static class CallbackValidation {
        private final boolean valid;
        private final String message;
        private final String code;
        private final String nonce;

        public CallbackValidation(boolean valid, String message, String code, String nonce) {
            this.valid = valid;
            this.message = message;
            this.code = code;
            this.nonce = nonce;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public String getCode() { return code; }
        public String getNonce() { return nonce; }
    }

    public static class ProviderInfo {
        private final String id;
        private final String name;
        private final List<String> scopes;
        private final boolean available;

        public ProviderInfo(String id, String name, List<String> scopes, boolean available) {
            this.id = id;
            this.name = name;
            this.scopes = scopes;
            this.available = available;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public List<String> getScopes() { return scopes; }
        public boolean isAvailable() { return available; }
    }

    // Internal state storage class
    private static class StateInfo {
        private final String providerId;
        private final String nonce;
        private final String redirectUri;
        private final long timestamp;

        public StateInfo(String providerId, String nonce, String redirectUri, long timestamp) {
            this.providerId = providerId;
            this.nonce = nonce;
            this.redirectUri = redirectUri;
            this.timestamp = timestamp;
        }

        public String getProviderId() { return providerId; }
        public String getNonce() { return nonce; }
        public String getRedirectUri() { return redirectUri; }
        public long getTimestamp() { return timestamp; }
    }
}