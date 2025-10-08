package buerostack.jwt.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomJwtGenerateRequest {

    @JsonProperty("JWTName")
    private String jwtName = "JWTTOKEN";

    @JsonProperty("content")
    private Map<String, Object> content = new HashMap<>();

    @JsonProperty("expirationInMinutes")
    private Long expirationInMinutes = 60L;

    @JsonProperty("setCookie")
    private Boolean setCookie = false;

    @JsonProperty("audience")
    private Object audience; // Can be String or List<String>

    public CustomJwtGenerateRequest() {}

    public String getJwtName() {
        return jwtName;
    }

    public void setJwtName(String jwtName) {
        this.jwtName = jwtName;
    }

    public Map<String, Object> getContent() {
        return content;
    }

    public void setContent(Map<String, Object> content) {
        this.content = content;
    }

    public Long getExpirationInMinutes() {
        return expirationInMinutes;
    }

    public void setExpirationInMinutes(Long expirationInMinutes) {
        this.expirationInMinutes = expirationInMinutes;
    }

    public Boolean getSetCookie() {
        return setCookie;
    }

    public void setSetCookie(Boolean setCookie) {
        this.setCookie = setCookie;
    }

    public Object getAudience() {
        return audience;
    }

    public void setAudience(Object audience) {
        this.audience = audience;
    }

    @SuppressWarnings("unchecked")
    public List<String> getAudienceAsList() {
        if (audience == null) {
            return null;
        }
        if (audience instanceof String) {
            return List.of((String) audience);
        }
        if (audience instanceof List) {
            return (List<String>) audience;
        }
        return List.of(audience.toString());
    }
}