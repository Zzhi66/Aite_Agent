package com.kama.jchatmind.service.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Tavily Search API 响应体。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TavilySearchResponse(
    String query,
    String answer,
    List<TavilyResult> results
) {
}
