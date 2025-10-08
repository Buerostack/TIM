package buerostack.oauth2.service;

import buerostack.oauth2.config.OAuth2ProvidersConfig.OAuth2ProvidersProperties;
import buerostack.oauth2.model.OidcDiscovery;
import buerostack.oauth2.model.ProviderConfig;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Service for validating JWT tokens from OAuth2/OIDC providers
 */
@Service
public class JwtValidationService {

    private static final Logger logger = LoggerFactory.getLogger(JwtValidationService.class);

    private final OAuth2ProvidersProperties providersProperties;
    private final OidcDiscoveryService discoveryService;
    private final WebClient webClient;

    @Autowired
    public JwtValidationService(OAuth2ProvidersProperties providersProperties,
                               OidcDiscoveryService discoveryService) {
        this.providersProperties = providersProperties;
        this.discoveryService = discoveryService;
        this.webClient = WebClient.builder().build();
    }

    /**
     * Validate an ID token from an OAuth2/OIDC provider
     */
    public JwtValidationResult validateIdToken(String providerId, String idToken, String nonce) {
        logger.debug("Validating ID token for provider: {}", providerId);

        try {
            // Get provider configuration
            ProviderConfig providerConfig = providersProperties.getProvider(providerId);
            if (providerConfig == null) {
                return new JwtValidationResult(false, "Provider not found: " + providerId, null);
            }

            // Parse JWT
            SignedJWT signedJWT = SignedJWT.parse(idToken);
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

            // Get discovery document for validation parameters
            OidcDiscovery discovery = discoveryService.getDiscovery(providerId, providerConfig);

            // Validate JWT signature
            if (!validateSignature(signedJWT, discovery.getJwksUri())) {
                return new JwtValidationResult(false, "Invalid JWT signature", null);
            }

            // Validate issuer
            if (!discovery.getIssuer().equals(claimsSet.getIssuer())) {
                return new JwtValidationResult(false,
                    String.format("Invalid issuer. Expected: %s, Got: %s",
                        discovery.getIssuer(), claimsSet.getIssuer()), null);
            }

            // Validate audience
            List<String> audience = claimsSet.getAudience();
            if (audience == null || !audience.contains(providerConfig.getClientId())) {
                return new JwtValidationResult(false,
                    "Invalid audience. Token not intended for this client", null);
            }

            // Validate expiration
            Date expirationTime = claimsSet.getExpirationTime();
            if (expirationTime == null || expirationTime.before(new Date())) {
                return new JwtValidationResult(false, "Token has expired", null);
            }

            // Validate not before (if present)
            Date notBeforeTime = claimsSet.getNotBeforeTime();
            if (notBeforeTime != null && notBeforeTime.after(new Date())) {
                return new JwtValidationResult(false, "Token not yet valid (nbf claim)", null);
            }

            // Validate issued at time (with clock skew tolerance)
            Date issuedAtTime = claimsSet.getIssueTime();
            if (issuedAtTime != null) {
                int clockSkewSeconds = providerConfig.getTokenValidation() != null ?
                    providerConfig.getTokenValidation().getClockSkewSeconds() : 60;

                long clockSkewMs = clockSkewSeconds * 1000L;
                Date now = new Date();
                if (issuedAtTime.getTime() > now.getTime() + clockSkewMs) {
                    return new JwtValidationResult(false, "Token issued in the future", null);
                }
            }

            // Validate nonce (if provided)
            if (nonce != null) {
                String tokenNonce = claimsSet.getStringClaim("nonce");
                if (!nonce.equals(tokenNonce)) {
                    return new JwtValidationResult(false, "Invalid nonce", null);
                }
            }

            logger.debug("ID token validation successful for provider: {}", providerId);

            return new JwtValidationResult(true, "Token validation successful", claimsSet);

        } catch (ParseException e) {
            logger.error("Failed to parse JWT token for provider {}: {}", providerId, e.getMessage());
            return new JwtValidationResult(false, "Invalid JWT format: " + e.getMessage(), null);
        } catch (Exception e) {
            logger.error("Error validating JWT token for provider {}: {}", providerId, e.getMessage());
            return new JwtValidationResult(false, "Token validation failed: " + e.getMessage(), null);
        }
    }

    /**
     * Validate JWT signature using provider's JWKS
     */
    private boolean validateSignature(SignedJWT signedJWT, String jwksUri) {
        try {
            // Get the key ID from JWT header
            String keyId = signedJWT.getHeader().getKeyID();

            // Fetch JWKS and find the matching key
            JWKSet jwkSet = fetchJwks(jwksUri);
            JWK jwk = jwkSet.getKeyByKeyId(keyId);

            if (jwk == null) {
                // If no specific key ID, try the first RSA key
                for (JWK key : jwkSet.getKeys()) {
                    if (key instanceof RSAKey) {
                        jwk = key;
                        break;
                    }
                }
            }

            if (jwk == null || !(jwk instanceof RSAKey)) {
                logger.error("No suitable RSA key found in JWKS for key ID: {}", keyId);
                return false;
            }

            RSAKey rsaKey = (RSAKey) jwk;
            RSAPublicKey publicKey = rsaKey.toRSAPublicKey();

            // Verify signature
            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            return signedJWT.verify(verifier);

        } catch (Exception e) {
            logger.error("Error validating JWT signature: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Fetch JWKS from provider with caching
     */
    @Cacheable(value = "jwks-keys", key = "#jwksUri")
    private JWKSet fetchJwks(String jwksUri) {
        try {
            logger.debug("Fetching JWKS from: {}", jwksUri);

            String jwksJson = webClient.get()
                    .uri(jwksUri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (jwksJson == null) {
                throw new RuntimeException("JWKS response is null");
            }

            return JWKSet.parse(jwksJson);

        } catch (Exception e) {
            logger.error("Failed to fetch JWKS from {}: {}", jwksUri, e.getMessage());
            throw new RuntimeException("Failed to fetch JWKS", e);
        }
    }

    /**
     * Extract standard claims from JWT
     */
    public Map<String, Object> extractStandardClaims(JWTClaimsSet claimsSet) {
        try {
            return Map.of(
                "sub", claimsSet.getSubject() != null ? claimsSet.getSubject() : "",
                "iss", claimsSet.getIssuer() != null ? claimsSet.getIssuer() : "",
                "aud", claimsSet.getAudience() != null ? claimsSet.getAudience() : List.of(),
                "exp", claimsSet.getExpirationTime() != null ? claimsSet.getExpirationTime().toInstant() : null,
                "iat", claimsSet.getIssueTime() != null ? claimsSet.getIssueTime().toInstant() : null,
                "nbf", claimsSet.getNotBeforeTime() != null ? claimsSet.getNotBeforeTime().toInstant() : null
            );
        } catch (Exception e) {
            logger.error("Error extracting standard claims: {}", e.getMessage());
            return Map.of();
        }
    }

    // Result class
    public static class JwtValidationResult {
        private final boolean valid;
        private final String message;
        private final JWTClaimsSet claimsSet;

        public JwtValidationResult(boolean valid, String message, JWTClaimsSet claimsSet) {
            this.valid = valid;
            this.message = message;
            this.claimsSet = claimsSet;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public JWTClaimsSet getClaimsSet() { return claimsSet; }
    }
}