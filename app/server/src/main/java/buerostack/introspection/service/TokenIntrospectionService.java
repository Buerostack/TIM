package buerostack.introspection.service;

import buerostack.introspection.dto.IntrospectionRequest;
import buerostack.introspection.dto.IntrospectionResponse;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TokenIntrospectionService {

    private static final Logger logger = LoggerFactory.getLogger(TokenIntrospectionService.class);

    private final Map<String, TokenValidator> validators = new HashMap<>();
    private final List<TokenValidator> tokenValidators;

    public TokenIntrospectionService(List<TokenValidator> tokenValidators) {
        this.tokenValidators = tokenValidators;
    }

    @PostConstruct
    private void registerValidators() {
        logger.info("Registering token validators...");

        for (TokenValidator validator : tokenValidators) {
            validators.put(validator.getTokenType(), validator);
            logger.info("Registered validator for token type: {}", validator.getTokenType());
        }

        logger.info("Token introspection service initialized with {} validators", validators.size());
    }

    /**
     * Introspect a token according to RFC 7662
     */
    public IntrospectionResponse introspect(IntrospectionRequest request) {
        String token = request.getToken();

        if (token == null || token.trim().isEmpty()) {
            logger.debug("Empty or null token provided");
            return IntrospectionResponse.inactive();
        }

        try {
            // Extract token type from JWT claims
            String tokenType = extractTokenType(token);

            if (tokenType == null) {
                logger.debug("Unable to determine token type");
                return IntrospectionResponse.inactive();
            }

            // Get appropriate validator
            TokenValidator validator = validators.get(tokenType);

            if (validator == null) {
                logger.debug("No validator found for token type: {}", tokenType);
                return IntrospectionResponse.inactive();
            }

            // Validate the token
            return validator.introspect(token);

        } catch (Exception e) {
            logger.error("Error during token introspection: {}", e.getMessage());
            return IntrospectionResponse.inactive();
        }
    }

    /**
     * Extract token_type claim from JWT
     */
    private String extractTokenType(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            Object tokenTypeClaim = jwt.getJWTClaimsSet().getClaim("token_type");

            if (tokenTypeClaim instanceof String) {
                return (String) tokenTypeClaim;
            }

            // Fallback: try to detect based on issuer and other claims
            return detectTokenTypeByHeuristics(jwt);

        } catch (Exception e) {
            logger.debug("Failed to parse JWT for token type extraction: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fallback token type detection for tokens without explicit token_type claim
     */
    private String detectTokenTypeByHeuristics(SignedJWT jwt) {
        try {
            String issuer = jwt.getJWTClaimsSet().getIssuer();

            // If issued by TIM, likely a custom JWT
            if ("TIM".equals(issuer)) {
                // Check for tim-audience to confirm it's our custom token
                if (jwt.getJWTClaimsSet().getAudience() != null &&
                    jwt.getJWTClaimsSet().getAudience().contains("tim-audience")) {
                    return "custom_jwt";
                }
            }

            // External OAuth2 tokens (Google, GitHub, etc.)
            if (issuer != null && !issuer.equals("TIM")) {
                return "oauth2_access";
            }

            return null;

        } catch (Exception e) {
            logger.debug("Error in token type heuristics: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get information about supported token types
     */
    public Map<String, String> getSupportedTokenTypes() {
        Map<String, String> supportedTypes = new HashMap<>();
        for (String tokenType : validators.keySet()) {
            supportedTypes.put(tokenType, validators.get(tokenType).getClass().getSimpleName());
        }
        return supportedTypes;
    }
}