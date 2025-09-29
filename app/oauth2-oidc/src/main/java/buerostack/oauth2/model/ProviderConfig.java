package buerostack.oauth2.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Configuration model for OAuth2/OIDC providers
 */
public class ProviderConfig {

    @NotBlank
    private String id;

    @NotBlank
    private String name;

    @NotBlank
    @JsonProperty("discovery_url")
    private String discoveryUrl;

    @NotBlank
    @JsonProperty("client_id")
    private String clientId;

    @NotBlank
    @JsonProperty("client_secret")
    private String clientSecret;

    @NotNull
    private Boolean enabled = true;

    @NotEmpty
    private List<String> scopes;

    @JsonProperty("claim_mappings")
    private Map<String, String> claimMappings;

    @JsonProperty("custom_claims")
    private List<String> customClaims;

    @JsonProperty("custom_parameters")
    private Map<String, String> customParameters;

    @JsonProperty("adapter_class")
    private String adapterClass;

    @JsonProperty("health_check_interval")
    private Integer healthCheckInterval = 300; // seconds

    @JsonProperty("token_validation")
    private TokenValidationConfig tokenValidation;

    // Default constructor
    public ProviderConfig() {}

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDiscoveryUrl() { return discoveryUrl; }
    public void setDiscoveryUrl(String discoveryUrl) { this.discoveryUrl = discoveryUrl; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public List<String> getScopes() { return scopes; }
    public void setScopes(List<String> scopes) { this.scopes = scopes; }

    public Map<String, String> getClaimMappings() { return claimMappings; }
    public void setClaimMappings(Map<String, String> claimMappings) { this.claimMappings = claimMappings; }

    public List<String> getCustomClaims() { return customClaims; }
    public void setCustomClaims(List<String> customClaims) { this.customClaims = customClaims; }

    public Map<String, String> getCustomParameters() { return customParameters; }
    public void setCustomParameters(Map<String, String> customParameters) { this.customParameters = customParameters; }

    public String getAdapterClass() { return adapterClass; }
    public void setAdapterClass(String adapterClass) { this.adapterClass = adapterClass; }

    public Integer getHealthCheckInterval() { return healthCheckInterval; }
    public void setHealthCheckInterval(Integer healthCheckInterval) { this.healthCheckInterval = healthCheckInterval; }

    public TokenValidationConfig getTokenValidation() { return tokenValidation; }
    public void setTokenValidation(TokenValidationConfig tokenValidation) { this.tokenValidation = tokenValidation; }

    /**
     * Token validation configuration
     */
    public static class TokenValidationConfig {
        @JsonProperty("clock_skew_seconds")
        private Integer clockSkewSeconds = 60;

        @JsonProperty("cache_ttl_seconds")
        private Integer cacheTtlSeconds = 3600;

        public Integer getClockSkewSeconds() { return clockSkewSeconds; }
        public void setClockSkewSeconds(Integer clockSkewSeconds) { this.clockSkewSeconds = clockSkewSeconds; }

        public Integer getCacheTtlSeconds() { return cacheTtlSeconds; }
        public void setCacheTtlSeconds(Integer cacheTtlSeconds) { this.cacheTtlSeconds = cacheTtlSeconds; }
    }
}