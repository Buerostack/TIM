package buerostack.jwt.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JwtExtendRequest {

    @JsonProperty("token")
    private String token;

    @JsonProperty("expirationInMinutes")
    private Integer expirationInMinutes;

    @JsonProperty("setCookie")
    private Boolean setCookie;

    public JwtExtendRequest() {}

    public JwtExtendRequest(String token, Integer expirationInMinutes) {
        this.token = token;
        this.expirationInMinutes = expirationInMinutes;
        this.setCookie = false;
    }

    public JwtExtendRequest(String token, Integer expirationInMinutes, Boolean setCookie) {
        this.token = token;
        this.expirationInMinutes = expirationInMinutes;
        this.setCookie = setCookie;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Integer getExpirationInMinutes() { return expirationInMinutes; }
    public void setExpirationInMinutes(Integer expirationInMinutes) { this.expirationInMinutes = expirationInMinutes; }

    public Boolean getSetCookie() { return setCookie != null ? setCookie : false; }
    public void setSetCookie(Boolean setCookie) { this.setCookie = setCookie; }
}