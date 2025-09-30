package buerostack.introspection.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class IntrospectionResponse {

    @JsonProperty("active")
    private boolean active;

    @JsonProperty("scope")
    private String scope;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("exp")
    private Long exp;

    @JsonProperty("iat")
    private Long iat;

    @JsonProperty("nbf")
    private Long nbf;

    @JsonProperty("sub")
    private String sub;

    @JsonProperty("aud")
    private Object aud;

    @JsonProperty("iss")
    private String iss;

    @JsonProperty("jti")
    private String jti;

    @JsonProperty("extra_claims")
    private Map<String, Object> extraClaims;

    public IntrospectionResponse() {}

    public static IntrospectionResponse inactive() {
        IntrospectionResponse response = new IntrospectionResponse();
        response.setActive(false);
        return response;
    }

    public static IntrospectionResponse active() {
        IntrospectionResponse response = new IntrospectionResponse();
        response.setActive(true);
        return response;
    }

    // Getters and Setters
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExp() {
        return exp;
    }

    public void setExp(Long exp) {
        this.exp = exp;
    }

    public Long getIat() {
        return iat;
    }

    public void setIat(Long iat) {
        this.iat = iat;
    }

    public Long getNbf() {
        return nbf;
    }

    public void setNbf(Long nbf) {
        this.nbf = nbf;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public Object getAud() {
        return aud;
    }

    public void setAud(Object aud) {
        this.aud = aud;
    }

    public String getIss() {
        return iss;
    }

    public void setIss(String iss) {
        this.iss = iss;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public Map<String, Object> getExtraClaims() {
        return extraClaims;
    }

    public void setExtraClaims(Map<String, Object> extraClaims) {
        this.extraClaims = extraClaims;
    }
}