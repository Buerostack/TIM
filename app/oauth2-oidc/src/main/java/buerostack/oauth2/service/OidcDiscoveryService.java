package buerostack.oauth2.service;

import buerostack.oauth2.model.OidcDiscovery;
import buerostack.oauth2.model.ProviderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service for discovering and caching OAuth2/OIDC provider configurations
 */
@Service
public class OidcDiscoveryService {

    private static final Logger logger = LoggerFactory.getLogger(OidcDiscoveryService.class);

    private final WebClient webClient;
    private final ConcurrentMap<String, OidcDiscovery> discoveryCache = new ConcurrentHashMap<>();

    public OidcDiscoveryService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
                .build();
    }

    /**
     * Discover OIDC configuration for a provider
     */
    @Cacheable(value = "oidc-discovery", key = "#providerId")
    public OidcDiscovery discoverProvider(String providerId, ProviderConfig config) {
        logger.info("Discovering OIDC configuration for provider: {}", providerId);

        try {
            OidcDiscovery discovery = webClient.get()
                    .uri(config.getDiscoveryUrl())
                    .retrieve()
                    .bodyToMono(OidcDiscovery.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                            .maxBackoff(Duration.ofSeconds(10)))
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (discovery == null) {
                throw new RuntimeException("Discovery document is null");
            }

            validateDiscoveryDocument(discovery, providerId);
            discoveryCache.put(providerId, discovery);

            logger.info("Successfully discovered OIDC configuration for provider: {} (issuer: {})",
                    providerId, discovery.getIssuer());

            return discovery;

        } catch (WebClientResponseException e) {
            logger.error("HTTP error discovering OIDC configuration for provider {}: {} - {}",
                    providerId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to discover OIDC configuration for provider: " + providerId, e);
        } catch (Exception e) {
            logger.error("Error discovering OIDC configuration for provider {}: {}", providerId, e.getMessage());
            throw new RuntimeException("Failed to discover OIDC configuration for provider: " + providerId, e);
        }
    }

    /**
     * Get cached discovery document or fetch if not cached
     */
    public OidcDiscovery getDiscovery(String providerId, ProviderConfig config) {
        OidcDiscovery cached = discoveryCache.get(providerId);
        if (cached != null) {
            return cached;
        }
        return discoverProvider(providerId, config);
    }

    /**
     * Validate discovery document has required fields
     */
    private void validateDiscoveryDocument(OidcDiscovery discovery, String providerId) {
        if (discovery.getIssuer() == null || discovery.getIssuer().trim().isEmpty()) {
            throw new RuntimeException("Discovery document for " + providerId + " missing required field: issuer");
        }

        if (discovery.getAuthorizationEndpoint() == null || discovery.getAuthorizationEndpoint().trim().isEmpty()) {
            throw new RuntimeException("Discovery document for " + providerId + " missing required field: authorization_endpoint");
        }

        if (discovery.getTokenEndpoint() == null || discovery.getTokenEndpoint().trim().isEmpty()) {
            throw new RuntimeException("Discovery document for " + providerId + " missing required field: token_endpoint");
        }

        if (discovery.getJwksUri() == null || discovery.getJwksUri().trim().isEmpty()) {
            throw new RuntimeException("Discovery document for " + providerId + " missing required field: jwks_uri");
        }

        // Validate required grant type support
        if (discovery.getGrantTypesSupported() == null ||
            !discovery.getGrantTypesSupported().contains("authorization_code")) {
            throw new RuntimeException("Discovery document for " + providerId + " does not support authorization_code grant type");
        }

        // Validate required response type support
        if (discovery.getResponseTypesSupported() == null ||
            !discovery.getResponseTypesSupported().contains("code")) {
            throw new RuntimeException("Discovery document for " + providerId + " does not support code response type");
        }

        logger.debug("Discovery document validation passed for provider: {}", providerId);
    }

    /**
     * Check if a provider's discovery document is cached and valid
     */
    public boolean isDiscoveryCached(String providerId) {
        return discoveryCache.containsKey(providerId);
    }

    /**
     * Clear cached discovery for a provider (useful for testing or manual refresh)
     */
    public void clearDiscoveryCache(String providerId) {
        discoveryCache.remove(providerId);
        logger.info("Cleared discovery cache for provider: {}", providerId);
    }

    /**
     * Clear all discovery cache
     */
    public void clearAllDiscoveryCache() {
        discoveryCache.clear();
        logger.info("Cleared all discovery cache");
    }

    /**
     * Test connectivity to a provider's discovery endpoint
     */
    public Mono<Boolean> testProviderConnectivity(ProviderConfig config) {
        return webClient.get()
                .uri(config.getDiscoveryUrl())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .map(response -> response != null && !response.trim().isEmpty())
                .onErrorReturn(false)
                .doOnNext(success -> {
                    if (success) {
                        logger.debug("Connectivity test passed for provider: {}", config.getId());
                    } else {
                        logger.warn("Connectivity test failed for provider: {}", config.getId());
                    }
                });
    }
}