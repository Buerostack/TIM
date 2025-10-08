package buerostack.jwt.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class JwtListResponse {

    @JsonProperty("tokens")
    private List<JwtTokenSummary> tokens;

    @JsonProperty("pagination")
    private PaginationInfo pagination;

    public JwtListResponse() {}

    public JwtListResponse(List<JwtTokenSummary> tokens, PaginationInfo pagination) {
        this.tokens = tokens;
        this.pagination = pagination;
    }

    public List<JwtTokenSummary> getTokens() { return tokens; }
    public void setTokens(List<JwtTokenSummary> tokens) { this.tokens = tokens; }

    public PaginationInfo getPagination() { return pagination; }
    public void setPagination(PaginationInfo pagination) { this.pagination = pagination; }

    public static class PaginationInfo {
        @JsonProperty("total")
        private Long total;

        @JsonProperty("limit")
        private Integer limit;

        @JsonProperty("offset")
        private Integer offset;

        @JsonProperty("has_more")
        private Boolean hasMore;

        @JsonProperty("page")
        private Integer page;

        @JsonProperty("size")
        private Integer size;

        @JsonProperty("total_pages")
        private Integer totalPages;

        public PaginationInfo() {}

        public PaginationInfo(Long total, Integer limit, Integer offset, Boolean hasMore) {
            this.total = total;
            this.limit = limit;
            this.offset = offset;
            this.hasMore = hasMore;
        }

        public Long getTotal() { return total; }
        public void setTotal(Long total) { this.total = total; }

        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }

        public Integer getOffset() { return offset; }
        public void setOffset(Integer offset) { this.offset = offset; }

        public Boolean getHasMore() { return hasMore; }
        public void setHasMore(Boolean hasMore) { this.hasMore = hasMore; }

        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }

        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }

        public Integer getTotalPages() { return totalPages; }
        public void setTotalPages(Integer totalPages) { this.totalPages = totalPages; }
    }
}