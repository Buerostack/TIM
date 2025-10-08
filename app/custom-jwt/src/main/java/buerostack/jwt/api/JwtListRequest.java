package buerostack.jwt.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JwtListRequest {

    @JsonProperty("status")
    private String status;

    @JsonProperty("issued_after")
    private String issuedAfter;

    @JsonProperty("issued_before")
    private String issuedBefore;

    @JsonProperty("expires_after")
    private String expiresAfter;

    @JsonProperty("expires_before")
    private String expiresBefore;

    @JsonProperty("jwt_name")
    private String jwtName;

    @JsonProperty("limit")
    private Integer limit;

    @JsonProperty("offset")
    private Integer offset;

    @JsonProperty("subject")
    private String subject;

    public JwtListRequest() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getIssuedAfter() { return issuedAfter; }
    public void setIssuedAfter(String issuedAfter) { this.issuedAfter = issuedAfter; }

    public String getIssuedBefore() { return issuedBefore; }
    public void setIssuedBefore(String issuedBefore) { this.issuedBefore = issuedBefore; }

    public String getExpiresAfter() { return expiresAfter; }
    public void setExpiresAfter(String expiresAfter) { this.expiresAfter = expiresAfter; }

    public String getExpiresBefore() { return expiresBefore; }
    public void setExpiresBefore(String expiresBefore) { this.expiresBefore = expiresBefore; }

    public String getJwtName() { return jwtName; }
    public void setJwtName(String jwtName) { this.jwtName = jwtName; }

    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }

    public Integer getOffset() { return offset; }
    public void setOffset(Integer offset) { this.offset = offset; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
}