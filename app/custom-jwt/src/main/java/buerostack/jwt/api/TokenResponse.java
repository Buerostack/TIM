package buerostack.jwt.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

public class TokenResponse {
    private String status;
    private String name;
    private String token;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant expiresAt;

    public TokenResponse() {}

    public TokenResponse(String status, String name, String token, Instant expiresAt) {
        this.status = status;
        this.name = name;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
