package buerostack.jwt.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JwtValidationRequest {

    @JsonProperty("token")
    private String token;

    @JsonProperty("audience")
    private String audience;

    @JsonProperty("issuer")
    private String issuer;

    public JwtValidationRequest() {}

    public JwtValidationRequest(String token, String audience) {
        this.token = token;
        this.audience = audience;
    }

    public JwtValidationRequest(String token, String audience, String issuer) {
        this.token = token;
        this.audience = audience;
        this.issuer = issuer;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
}