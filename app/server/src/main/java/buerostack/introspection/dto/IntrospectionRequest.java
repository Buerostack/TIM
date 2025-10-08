package buerostack.introspection.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IntrospectionRequest {

    @JsonProperty("token")
    private String token;

    @JsonProperty("token_type_hint")
    private String tokenTypeHint;

    public IntrospectionRequest() {}

    public IntrospectionRequest(String token) {
        this.token = token;
    }

    public IntrospectionRequest(String token, String tokenTypeHint) {
        this.token = token;
        this.tokenTypeHint = tokenTypeHint;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenTypeHint() {
        return tokenTypeHint;
    }

    public void setTokenTypeHint(String tokenTypeHint) {
        this.tokenTypeHint = tokenTypeHint;
    }
}