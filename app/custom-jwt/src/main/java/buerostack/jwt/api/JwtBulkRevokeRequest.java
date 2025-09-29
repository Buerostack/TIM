package buerostack.jwt.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class JwtBulkRevokeRequest {

    @JsonProperty("tokens")
    private List<String> tokens;

    @JsonProperty("reason")
    private String reason;

    public JwtBulkRevokeRequest() {}

    public JwtBulkRevokeRequest(List<String> tokens) {
        this.tokens = tokens;
    }

    public JwtBulkRevokeRequest(List<String> tokens, String reason) {
        this.tokens = tokens;
        this.reason = reason;
    }

    public List<String> getTokens() { return tokens; }
    public void setTokens(List<String> tokens) { this.tokens = tokens; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}