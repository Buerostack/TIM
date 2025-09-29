package buerostack.oauth2.service;

import buerostack.oauth2.config.OAuth2ProvidersConfig.OAuth2ProvidersProperties;
import buerostack.oauth2.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for OAuth2 token operations (exchange, validation, refresh)
 */
@Service
public class OAuth2TokenService {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2TokenService.class);

    private final OAuth2ProvidersProperties providersProperties;
    private final OidcDiscoveryService discoveryService;
    private final WebClient webClient;

    @Autowired
    public OAuth2TokenService(OAuth2ProvidersProperties providersProperties,
                             OidcDiscoveryService discoveryService) {
        this.providersProperties = providersProperties;
        this.discoveryService = discoveryService;
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }

    /**
     * Exchange authorization code for tokens
     */
    public TokenExchangeResult exchangeCodeForTokens(String providerId, String code, String nonce) {
        logger.info("Exchanging authorization code for tokens with provider: {}", providerId);

        try {
            // Get provider configuration
            ProviderConfig providerConfig = providersProperties.getProvider(providerId);
            if (providerConfig == null) {
                throw new RuntimeException("Provider not found: " + providerId);
            }

            // Get discovery document
            OidcDiscovery discovery = discoveryService.getDiscovery(providerId, providerConfig);

            // Prepare token request
            MultiValueMap<String, String> tokenRequest = new LinkedMultiValueMap<>();
            tokenRequest.add("grant_type", "authorization_code");
            tokenRequest.add("client_id", providerConfig.getClientId());
            tokenRequest.add("client_secret", providerConfig.getClientSecret());
            tokenRequest.add("redirect_uri", getCallbackUrl(providerId));
            tokenRequest.add("code", code);

            // Make token request
            TokenResponse tokenResponse = webClient.post()
                    .uri(discovery.getTokenEndpoint())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(tokenRequest)
                    .retrieve()
                    .bodyToMono(TokenResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (tokenResponse == null) {
                throw new RuntimeException("Token response is null");
            }

            logger.info("Successfully exchanged authorization code for tokens with provider: {}", providerId);

            return new TokenExchangeResult(true, "Token exchange successful", tokenResponse, null);

        } catch (WebClientResponseException e) {
            logger.error("HTTP error during token exchange for provider {}: {} - {}",
                    providerId, e.getStatusCode(), e.getResponseBodyAsString());

            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("status_code", e.getStatusCode().value());
            errorDetails.put("response_body", e.getResponseBodyAsString());

            return new TokenExchangeResult(false, "Token exchange failed: " + e.getMessage(), null, errorDetails);

        } catch (Exception e) {
            logger.error("Error during token exchange for provider {}: {}", providerId, e.getMessage());
            return new TokenExchangeResult(false, "Token exchange failed: " + e.getMessage(), null, null);
        }
    }

    /**
     * Get the callback URL for a provider
     */
    private String getCallbackUrl(String providerId) {
        // TODO: Make this configurable based on environment
        return "http://localhost:8085/auth/callback/" + providerId;
    }

    /**
     * Validate an access token with the provider's userinfo endpoint
     */
    public UserInfoResult getUserInfo(String providerId, String accessToken) {
        logger.debug("Getting user info from provider: {}", providerId);

        try {
            // Get provider configuration
            ProviderConfig providerConfig = providersProperties.getProvider(providerId);
            if (providerConfig == null) {
                throw new RuntimeException("Provider not found: " + providerId);
            }

            // Get discovery document
            OidcDiscovery discovery = discoveryService.getDiscovery(providerId, providerConfig);

            if (discovery.getUserinfoEndpoint() == null) {
                logger.warn("Provider {} does not have userinfo endpoint", providerId);
                return new UserInfoResult(false, "Provider does not support userinfo endpoint", null);
            }

            // Make userinfo request
            Map<String, Object> userInfo = webClient.get()
                    .uri(discovery.getUserinfoEndpoint())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (userInfo == null) {
                throw new RuntimeException("UserInfo response is null");
            }

            logger.debug("Successfully retrieved user info from provider: {}", providerId);

            return new UserInfoResult(true, "UserInfo retrieval successful", userInfo);

        } catch (WebClientResponseException e) {
            logger.error("HTTP error during userinfo request for provider {}: {} - {}",
                    providerId, e.getStatusCode(), e.getResponseBodyAsString());
            return new UserInfoResult(false, "UserInfo request failed: " + e.getMessage(), null);

        } catch (Exception e) {
            logger.error("Error during userinfo request for provider {}: {}", providerId, e.getMessage());
            return new UserInfoResult(false, "UserInfo request failed: " + e.getMessage(), null);
        }
    }

    // Result classes

    public static class TokenExchangeResult {
        private final boolean success;
        private final String message;
        private final TokenResponse tokenResponse;
        private final Map<String, Object> errorDetails;

        public TokenExchangeResult(boolean success, String message, TokenResponse tokenResponse, Map<String, Object> errorDetails) {
            this.success = success;
            this.message = message;
            this.tokenResponse = tokenResponse;
            this.errorDetails = errorDetails;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public TokenResponse getTokenResponse() { return tokenResponse; }
        public Map<String, Object> getErrorDetails() { return errorDetails; }
    }

    public static class UserInfoResult {
        private final boolean success;
        private final String message;
        private final Map<String, Object> userInfo;

        public UserInfoResult(boolean success, String message, Map<String, Object> userInfo) {
            this.success = success;
            this.message = message;
            this.userInfo = userInfo;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Map<String, Object> getUserInfo() { return userInfo; }
    }
}