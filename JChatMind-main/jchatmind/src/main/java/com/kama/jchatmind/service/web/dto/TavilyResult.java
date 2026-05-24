package com.kama.jchatmind.service.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Tavily 单条搜索结果。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TavilyResult(
    String title,
    String url,
    String content,
    Double score
) {
}
