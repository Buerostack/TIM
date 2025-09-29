package buerostack.oauth2.config;

import buerostack.oauth2.model.ProviderConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for OAuth2/OIDC providers loaded from YAML
 */
@Configuration
public class OAuth2ProvidersConfig {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2ProvidersConfig.class);

    @Bean
    public OAuth2ProvidersProperties oauth2ProvidersProperties() {
        return new OAuth2ProvidersProperties();
    }

    /**
     * Properties class for OAuth2 providers configuration
     */
    @Component
    @ConfigurationProperties(prefix = "oauth2")
    public static class OAuth2ProvidersProperties {

        private Map<String, ProviderConfig> providers = new HashMap<>();

        @PostConstruct
        public void loadProvidersFromYaml() {
            try {
                ClassPathResource resource = new ClassPathResource("oauth2-providers.yml");
                if (resource.exists()) {
                    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                    ProvidersConfigWrapper wrapper = mapper.readValue(resource.getInputStream(), ProvidersConfigWrapper.class);

                    if (wrapper.getProviders() != null) {
                        // Set the ID for each provider based on the map key
                        wrapper.getProviders().forEach((id, config) -> {
                            config.setId(id);
                            this.providers.put(id, config);
                        });
                        logger.info("Loaded {} OAuth2 providers from configuration", this.providers.size());
                    }
                } else {
                    logger.warn("oauth2-providers.yml not found in classpath, using empty provider configuration");
                }
            } catch (IOException e) {
                logger.error("Failed to load OAuth2 providers configuration", e);
                throw new RuntimeException("Failed to load OAuth2 providers configuration", e);
            }
        }

        public Map<String, ProviderConfig> getProviders() {
            return providers;
        }

        public void setProviders(Map<String, ProviderConfig> providers) {
            this.providers = providers;
        }

        /**
         * Get a provider by ID
         */
        public ProviderConfig getProvider(String providerId) {
            return providers.get(providerId);
        }

        /**
         * Check if a provider exists and is enabled
         */
        public boolean isProviderAvailable(String providerId) {
            ProviderConfig provider = providers.get(providerId);
            return provider != null && provider.getEnabled();
        }

        /**
         * Get all enabled provider IDs
         */
        public Map<String, ProviderConfig> getEnabledProviders() {
            Map<String, ProviderConfig> enabledProviders = new HashMap<>();
            providers.forEach((id, config) -> {
                if (config.getEnabled()) {
                    enabledProviders.put(id, config);
                }
            });
            return enabledProviders;
        }
    }

    /**
     * Wrapper class for YAML deserialization
     */
    private static class ProvidersConfigWrapper {
        private Map<String, ProviderConfig> providers;

        public Map<String, ProviderConfig> getProviders() {
            return providers;
        }

        public void setProviders(Map<String, ProviderConfig> providers) {
            this.providers = providers;
        }
    }
}