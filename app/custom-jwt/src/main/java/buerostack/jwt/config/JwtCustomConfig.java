package buerostack.jwt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.logging.Logger;

@Configuration
@ConfigurationProperties(prefix = "jwt.custom")
public class JwtCustomConfig {

    private static final Logger logger = Logger.getLogger(JwtCustomConfig.class.getName());

    private String issuer = "TIM";
    private Audience audience = new Audience();

    public static class Audience {
        private Validation validation = new Validation();
        private String allowed = "";
        private String defaultAudience = "tim-service";

        public Validation getValidation() { return validation; }
        public void setValidation(Validation validation) { this.validation = validation; }
        public String getAllowed() { return allowed; }
        public void setAllowed(String allowed) { this.allowed = allowed; }
        public String getDefaultAudience() { return defaultAudience; }
        public void setDefaultAudience(String defaultAudience) { this.defaultAudience = defaultAudience; }
    }

    @PostConstruct
    public void logConfiguration() {
        logger.info("JWT Issuer configured as: '" + issuer + "'");

        if (!audience.validation.enabled) {
            logger.warning("JWT Audience validation is DISABLED. This may allow token misuse across services. " +
                          "Consider enabling 'jwt.custom.audience.validation.enabled=true' for production environments.");
        } else {
            logger.info("JWT Audience validation is ENABLED. Allowed audiences: " + getAllowedAudiences());
        }
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Audience getAudience() {
        return audience;
    }

    public void setAudience(Audience audience) {
        this.audience = audience;
    }

    public boolean isValidationEnabled() {
        return audience.validation.enabled;
    }

    public List<String> getAllowedAudiences() {
        if (audience.allowed == null || audience.allowed.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(audience.allowed.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public String getDefaultAudience() {
        return audience.defaultAudience;
    }

    public boolean isAudienceAllowed(String aud) {
        if (!audience.validation.enabled) {
            return true; // Allow any audience when validation is disabled
        }

        List<String> allowedList = getAllowedAudiences();
        return allowedList.isEmpty() || allowedList.contains(aud);
    }

    public boolean isAudienceAllowed(List<String> audiences) {
        if (!audience.validation.enabled) {
            return true; // Allow any audience when validation is disabled
        }

        List<String> allowedList = getAllowedAudiences();
        if (allowedList.isEmpty()) {
            return true; // No restrictions when allowed list is empty
        }

        return audiences.stream().allMatch(allowedList::contains);
    }

    public static class Validation {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}