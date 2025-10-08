package buerostack.oauth2.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * OIDC Discovery Document model
 * Based on RFC 8414 and OpenID Connect Discovery 1.0
 */
public class OidcDiscovery {

    private String issuer;

    @JsonProperty("authorization_endpoint")
    private String authorizationEndpoint;

    @JsonProperty("token_endpoint")
    private String tokenEndpoint;

    @JsonProperty("userinfo_endpoint")
    private String userinfoEndpoint;

    @JsonProperty("jwks_uri")
    private String jwksUri;

    @JsonProperty("registration_endpoint")
    private String registrationEndpoint;

    @JsonProperty("scopes_supported")
    private List<String> scopesSupported;

    @JsonProperty("response_types_supported")
    private List<String> responseTypesSupported;

    @JsonProperty("grant_types_supported")
    private List<String> grantTypesSupported;

    @JsonProperty("subject_types_supported")
    private List<String> subjectTypesSupported;

    @JsonProperty("id_token_signing_alg_values_supported")
    private List<String> idTokenSigningAlgSupported;

    @JsonProperty("claims_supported")
    private List<String> claimsSupported;

    @JsonProperty("code_challenge_methods_supported")
    private List<String> codeChallengeMethodsSupported;

    @JsonProperty("revocation_endpoint")
    private String revocationEndpoint;

    @JsonProperty("end_session_endpoint")
    private String endSessionEndpoint;

    // Default constructor
    public OidcDiscovery() {}

    // Getters and setters
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public String getAuthorizationEndpoint() { return authorizationEndpoint; }
    public void setAuthorizationEndpoint(String authorizationEndpoint) { this.authorizationEndpoint = authorizationEndpoint; }

    public String getTokenEndpoint() { return tokenEndpoint; }
    public void setTokenEndpoint(String tokenEndpoint) { this.tokenEndpoint = tokenEndpoint; }

    public String getUserinfoEndpoint() { return userinfoEndpoint; }
    public void setUserinfoEndpoint(String userinfoEndpoint) { this.userinfoEndpoint = userinfoEndpoint; }

    public String getJwksUri() { return jwksUri; }
    public void setJwksUri(String jwksUri) { this.jwksUri = jwksUri; }

    public String getRegistrationEndpoint() { return registrationEndpoint; }
    public void setRegistrationEndpoint(String registrationEndpoint) { this.registrationEndpoint = registrationEndpoint; }

    public List<String> getScopesSupported() { return scopesSupported; }
    public void setScopesSupported(List<String> scopesSupported) { this.scopesSupported = scopesSupported; }

    public List<String> getResponseTypesSupported() { return responseTypesSupported; }
    public void setResponseTypesSupported(List<String> responseTypesSupported) { this.responseTypesSupported = responseTypesSupported; }

    public List<String> getGrantTypesSupported() { return grantTypesSupported; }
    public void setGrantTypesSupported(List<String> grantTypesSupported) { this.grantTypesSupported = grantTypesSupported; }

    public List<String> getSubjectTypesSupported() { return subjectTypesSupported; }
    public void setSubjectTypesSupported(List<String> subjectTypesSupported) { this.subjectTypesSupported = subjectTypesSupported; }

    public List<String> getIdTokenSigningAlgSupported() { return idTokenSigningAlgSupported; }
    public void setIdTokenSigningAlgSupported(List<String> idTokenSigningAlgSupported) { this.idTokenSigningAlgSupported = idTokenSigningAlgSupported; }

    public List<String> getClaimsSupported() { return claimsSupported; }
    public void setClaimsSupported(List<String> claimsSupported) { this.claimsSupported = claimsSupported; }

    public List<String> getCodeChallengeMethodsSupported() { return codeChallengeMethodsSupported; }
    public void setCodeChallengeMethodsSupported(List<String> codeChallengeMethodsSupported) { this.codeChallengeMethodsSupported = codeChallengeMethodsSupported; }

    public String getRevocationEndpoint() { return revocationEndpoint; }
    public void setRevocationEndpoint(String revocationEndpoint) { this.revocationEndpoint = revocationEndpoint; }

    public String getEndSessionEndpoint() { return endSessionEndpoint; }
    public void setEndSessionEndpoint(String endSessionEndpoint) { this.endSessionEndpoint = endSessionEndpoint; }
}