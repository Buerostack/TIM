package buerostack.introspection.service;

import buerostack.introspection.dto.IntrospectionResponse;
import buerostack.jwt.service.CustomJwtService;
import buerostack.config.JwtSignerService;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class CustomJwtTokenValidator implements TokenValidator {

    private static final Logger logger = LoggerFactory.getLogger(CustomJwtTokenValidator.class);

    private final CustomJwtService customJwtService;
    private final JwtSignerService jwtSignerService;

    public CustomJwtTokenValidator(CustomJwtService customJwtService, JwtSignerService jwtSignerService) {
        this.customJwtService = customJwtService;
        this.jwtSignerService = jwtSignerService;
    }

    @Override
    public IntrospectionResponse introspect(String token) {
        try {
            // Parse JWT
            SignedJWT jwt = SignedJWT.parse(token);

            // Verify signature
            if (!jwtSignerService.verify(token)) {
                logger.debug("Custom JWT signature verification failed");
                return IntrospectionResponse.inactive();
            }

            // Check if revoked in denylist
            if (customJwtService.isRevoked(token)) {
                logger.debug("Custom JWT is revoked");
                return IntrospectionResponse.inactive();
            }

            // Check expiration
            if (jwt.getJWTClaimsSet().getExpirationTime().toInstant().isBefore(Instant.now())) {
                logger.debug("Custom JWT is expired");
                return IntrospectionResponse.inactive();
            }

            // Build successful response
            IntrospectionResponse response = IntrospectionResponse.active();

            // Standard claims
            response.setSub(jwt.getJWTClaimsSet().getSubject());
            response.setIss(jwt.getJWTClaimsSet().getIssuer());
            response.setJti(jwt.getJWTClaimsSet().getJWTID());
            response.setExp(jwt.getJWTClaimsSet().getExpirationTime().getTime() / 1000);
            response.setIat(jwt.getJWTClaimsSet().getIssueTime().getTime() / 1000);

            // Audience (can be string or array)
            if (jwt.getJWTClaimsSet().getAudience() != null && !jwt.getJWTClaimsSet().getAudience().isEmpty()) {
                if (jwt.getJWTClaimsSet().getAudience().size() == 1) {
                    response.setAud(jwt.getJWTClaimsSet().getAudience().get(0));
                } else {
                    response.setAud(jwt.getJWTClaimsSet().getAudience());
                }
            }

            // Token type
            response.setTokenType(getTokenType());

            // Extract custom claims (excluding standard JWT claims)
            Map<String, Object> extraClaims = new HashMap<>();
            Map<String, Object> allClaims = jwt.getJWTClaimsSet().getClaims();

            // Standard JWT claims to exclude
            String[] standardClaims = {"iss", "sub", "aud", "exp", "iat", "jti", "token_type"};

            for (Map.Entry<String, Object> entry : allClaims.entrySet()) {
                if (!Arrays.asList(standardClaims).contains(entry.getKey())) {
                    extraClaims.put(entry.getKey(), entry.getValue());
                }
            }

            if (!extraClaims.isEmpty()) {
                response.setExtraClaims(extraClaims);
            }

            logger.debug("Custom JWT introspection successful for jti: {}", jwt.getJWTClaimsSet().getJWTID());
            return response;

        } catch (Exception e) {
            logger.error("Error during custom JWT introspection: {}", e.getMessage());
            return IntrospectionResponse.inactive();
        }
    }

    @Override
    public String getTokenType() {
        return "custom_jwt";
    }
}