package com.kama.jchatmind.service.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Tavily Search API 请求体。
 */
public record TavilySearchRequest(
    @JsonProperty("api_key") String apiKey,
    String query,
    @JsonProperty("search_depth") String searchDepth,
    @JsonProperty("max_results") int maxResults,
    @JsonProperty("include_answer") boolean includeAnswer
) {
}
