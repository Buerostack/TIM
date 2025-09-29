package buerostack.oauth2.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

/**
 * Standardized user profile model
 */
public class UserProfile {

    private String sub;
    private String provider;
    private ProfileInfo profile;

    @JsonProperty("custom_claims")
    private Map<String, Object> customClaims;

    @JsonProperty("token_info")
    private TokenInfo tokenInfo;

    // Default constructor
    public UserProfile() {}

    // Constructor
    public UserProfile(String sub, String provider, ProfileInfo profile) {
        this.sub = sub;
        this.provider = provider;
        this.profile = profile;
    }

    // Getters and setters
    public String getSub() { return sub; }
    public void setSub(String sub) { this.sub = sub; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public ProfileInfo getProfile() { return profile; }
    public void setProfile(ProfileInfo profile) { this.profile = profile; }

    public Map<String, Object> getCustomClaims() { return customClaims; }
    public void setCustomClaims(Map<String, Object> customClaims) { this.customClaims = customClaims; }

    public TokenInfo getTokenInfo() { return tokenInfo; }
    public void setTokenInfo(TokenInfo tokenInfo) { this.tokenInfo = tokenInfo; }

    /**
     * Profile information
     */
    public static class ProfileInfo {
        private String firstName;
        private String lastName;
        private String email;
        private Boolean emailVerified;
        private String phoneNumber;
        private Boolean phoneVerified;
        private String avatarUrl;
        private String locale;

        // TARA-specific
        private String nationalId;
        private String authenticationMethod;
        private String levelOfAssurance;

        // Azure AD specific
        private String userPrincipalName;
        private String objectId;

        // Default constructor
        public ProfileInfo() {}

        // Getters and setters
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public Boolean getEmailVerified() { return emailVerified; }
        public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public Boolean getPhoneVerified() { return phoneVerified; }
        public void setPhoneVerified(Boolean phoneVerified) { this.phoneVerified = phoneVerified; }

        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

        public String getLocale() { return locale; }
        public void setLocale(String locale) { this.locale = locale; }

        public String getNationalId() { return nationalId; }
        public void setNationalId(String nationalId) { this.nationalId = nationalId; }

        public String getAuthenticationMethod() { return authenticationMethod; }
        public void setAuthenticationMethod(String authenticationMethod) { this.authenticationMethod = authenticationMethod; }

        public String getLevelOfAssurance() { return levelOfAssurance; }
        public void setLevelOfAssurance(String levelOfAssurance) { this.levelOfAssurance = levelOfAssurance; }

        public String getUserPrincipalName() { return userPrincipalName; }
        public void setUserPrincipalName(String userPrincipalName) { this.userPrincipalName = userPrincipalName; }

        public String getObjectId() { return objectId; }
        public void setObjectId(String objectId) { this.objectId = objectId; }
    }

    /**
     * Token information
     */
    public static class TokenInfo {
        @JsonProperty("issued_at")
        private Instant issuedAt;

        @JsonProperty("expires_at")
        private Instant expiresAt;

        private String[] scopes;

        // Default constructor
        public TokenInfo() {}

        // Getters and setters
        public Instant getIssuedAt() { return issuedAt; }
        public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }

        public Instant getExpiresAt() { return expiresAt; }
        public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

        public String[] getScopes() { return scopes; }
        public void setScopes(String[] scopes) { this.scopes = scopes; }
    }
}