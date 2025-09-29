package buerostack.oauth2.service;

import buerostack.oauth2.config.OAuth2ProvidersConfig.OAuth2ProvidersProperties;
import buerostack.oauth2.model.ProviderConfig;
import buerostack.oauth2.model.UserProfile;
import com.nimbusds.jwt.JWTClaimsSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for extracting and standardizing user profiles from OAuth2/OIDC tokens
 */
@Service
public class UserProfileService {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileService.class);

    private final OAuth2ProvidersProperties providersProperties;

    @Autowired
    public UserProfileService(OAuth2ProvidersProperties providersProperties) {
        this.providersProperties = providersProperties;
    }

    /**
     * Extract user profile from ID token claims and userinfo
     */
    public UserProfile extractUserProfile(String providerId, JWTClaimsSet idTokenClaims,
                                        Map<String, Object> userInfo, String[] scopes) {

        logger.debug("Extracting user profile for provider: {}", providerId);

        ProviderConfig providerConfig = providersProperties.getProvider(providerId);
        if (providerConfig == null) {
            throw new RuntimeException("Provider not found: " + providerId);
        }

        try {
            String subject = idTokenClaims.getSubject();
            UserProfile userProfile = new UserProfile(subject, providerId, new UserProfile.ProfileInfo());

            // Extract standard profile information
            UserProfile.ProfileInfo profileInfo = extractStandardProfile(idTokenClaims, userInfo, providerConfig);
            userProfile.setProfile(profileInfo);

            // Extract custom claims
            Map<String, Object> customClaims = extractCustomClaims(idTokenClaims, userInfo, providerConfig);
            userProfile.setCustomClaims(customClaims);

            // Add token information
            UserProfile.TokenInfo tokenInfo = new UserProfile.TokenInfo();
            if (idTokenClaims.getIssueTime() != null) {
                tokenInfo.setIssuedAt(idTokenClaims.getIssueTime().toInstant());
            }
            if (idTokenClaims.getExpirationTime() != null) {
                tokenInfo.setExpiresAt(idTokenClaims.getExpirationTime().toInstant());
            }
            tokenInfo.setScopes(scopes);
            userProfile.setTokenInfo(tokenInfo);

            logger.debug("User profile extracted successfully for provider: {}", providerId);

            return userProfile;

        } catch (Exception e) {
            logger.error("Error extracting user profile for provider {}: {}", providerId, e.getMessage());
            throw new RuntimeException("Failed to extract user profile", e);
        }
    }

    /**
     * Extract standard profile information using claim mappings
     */
    private UserProfile.ProfileInfo extractStandardProfile(JWTClaimsSet idTokenClaims,
                                                          Map<String, Object> userInfo,
                                                          ProviderConfig providerConfig) {

        UserProfile.ProfileInfo profileInfo = new UserProfile.ProfileInfo();
        Map<String, String> claimMappings = providerConfig.getClaimMappings();

        if (claimMappings == null) {
            // Use default mappings if none configured
            claimMappings = getDefaultClaimMappings();
        }

        // Merge claims from ID token and userinfo (userinfo takes precedence)
        Map<String, Object> allClaims = new HashMap<>();
        if (idTokenClaims != null) {
            allClaims.putAll(idTokenClaims.getClaims());
        }
        if (userInfo != null) {
            allClaims.putAll(userInfo);
        }

        // Map claims to standard profile fields
        profileInfo.setFirstName(mapClaim(allClaims, claimMappings.get("firstName"), String.class));
        profileInfo.setLastName(mapClaim(allClaims, claimMappings.get("lastName"), String.class));
        profileInfo.setEmail(mapClaim(allClaims, claimMappings.get("email"), String.class));
        profileInfo.setEmailVerified(mapClaim(allClaims, claimMappings.get("emailVerified"), Boolean.class));
        profileInfo.setPhoneNumber(mapClaim(allClaims, claimMappings.get("phoneNumber"), String.class));
        profileInfo.setPhoneVerified(mapClaim(allClaims, claimMappings.get("phoneVerified"), Boolean.class));
        profileInfo.setAvatarUrl(mapClaim(allClaims, claimMappings.get("avatarUrl"), String.class));
        profileInfo.setLocale(mapClaim(allClaims, claimMappings.get("locale"), String.class));

        // Provider-specific mappings
        profileInfo.setNationalId(mapClaim(allClaims, claimMappings.get("nationalId"), String.class));
        profileInfo.setAuthenticationMethod(mapComplexClaim(allClaims, claimMappings.get("authenticationMethod")));
        profileInfo.setLevelOfAssurance(mapClaim(allClaims, claimMappings.get("levelOfAssurance"), String.class));
        profileInfo.setUserPrincipalName(mapClaim(allClaims, claimMappings.get("userPrincipalName"), String.class));
        profileInfo.setObjectId(mapClaim(allClaims, claimMappings.get("objectId"), String.class));

        return profileInfo;
    }

    /**
     * Extract custom claims based on provider configuration
     */
    private Map<String, Object> extractCustomClaims(JWTClaimsSet idTokenClaims,
                                                   Map<String, Object> userInfo,
                                                   ProviderConfig providerConfig) {

        Map<String, Object> customClaims = new HashMap<>();
        List<String> customClaimNames = providerConfig.getCustomClaims();

        if (customClaimNames == null || customClaimNames.isEmpty()) {
            return customClaims;
        }

        // Merge claims from ID token and userinfo
        Map<String, Object> allClaims = new HashMap<>();
        if (idTokenClaims != null) {
            allClaims.putAll(idTokenClaims.getClaims());
        }
        if (userInfo != null) {
            allClaims.putAll(userInfo);
        }

        // Extract specified custom claims
        for (String claimName : customClaimNames) {
            Object claimValue = allClaims.get(claimName);
            if (claimValue != null) {
                customClaims.put(claimName, claimValue);
            }
        }

        return customClaims;
    }

    /**
     * Map a claim value to a specific type
     */
    @SuppressWarnings("unchecked")
    private <T> T mapClaim(Map<String, Object> claims, String claimPath, Class<T> targetType) {
        if (claimPath == null || claims == null) {
            return null;
        }

        Object value = claims.get(claimPath);
        if (value == null) {
            return null;
        }

        try {
            if (targetType.isInstance(value)) {
                return (T) value;
            }

            // Type conversion
            if (targetType == String.class) {
                return (T) value.toString();
            } else if (targetType == Boolean.class) {
                if (value instanceof String) {
                    return (T) Boolean.valueOf((String) value);
                } else if (value instanceof Boolean) {
                    return (T) value;
                }
            }

            return null;
        } catch (Exception e) {
            logger.warn("Error mapping claim {} to type {}: {}", claimPath, targetType.getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * Map complex claims (e.g., array[0] notation)
     */
    private String mapComplexClaim(Map<String, Object> claims, String claimPath) {
        if (claimPath == null || claims == null) {
            return null;
        }

        try {
            // Handle array notation like "amr[0]"
            if (claimPath.contains("[") && claimPath.contains("]")) {
                String baseClaimName = claimPath.substring(0, claimPath.indexOf("["));
                String indexStr = claimPath.substring(claimPath.indexOf("[") + 1, claimPath.indexOf("]"));
                int index = Integer.parseInt(indexStr);

                Object claimValue = claims.get(baseClaimName);
                if (claimValue instanceof List) {
                    List<?> list = (List<?>) claimValue;
                    if (index < list.size()) {
                        Object item = list.get(index);
                        return item != null ? item.toString() : null;
                    }
                }
                return null;
            }

            // Simple claim mapping
            return mapClaim(claims, claimPath, String.class);

        } catch (Exception e) {
            logger.warn("Error mapping complex claim {}: {}", claimPath, e.getMessage());
            return null;
        }
    }

    /**
     * Get default claim mappings for standard OAuth2/OIDC claims
     */
    private Map<String, String> getDefaultClaimMappings() {
        Map<String, String> defaultMappings = new HashMap<>();
        defaultMappings.put("firstName", "given_name");
        defaultMappings.put("lastName", "family_name");
        defaultMappings.put("email", "email");
        defaultMappings.put("emailVerified", "email_verified");
        defaultMappings.put("phoneNumber", "phone_number");
        defaultMappings.put("phoneVerified", "phone_number_verified");
        defaultMappings.put("avatarUrl", "picture");
        defaultMappings.put("locale", "locale");
        return defaultMappings;
    }
}