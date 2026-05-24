package com.kama.jchatmind.service.impl;

import com.kama.jchatmind.config.WebClientConfig;
import com.kama.jchatmind.config.WebToolsProperties;
import com.kama.jchatmind.service.WebSearchService;
import com.kama.jchatmind.service.web.WebSearchResultFormatter;
import com.kama.jchatmind.service.web.dto.TavilySearchRequest;
import com.kama.jchatmind.service.web.dto.TavilySearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * 基于 Tavily REST API 的联网搜索实现。
 */
@Slf4j
@Service
public class WebSearchServiceImpl implements WebSearchService {

  private final WebClient tavilyWebClient;
  private final WebToolsProperties webToolsProperties;

  @Value("${spring.ai.tavily.api-key:}")
  private String tavilyApiKey;

  public WebSearchServiceImpl(
      @Qualifier(WebClientConfig.TAVILY_CLIENT) WebClient tavilyWebClient,
      WebToolsProperties webToolsProperties
  ) {
    this.tavilyWebClient = tavilyWebClient;
    this.webToolsProperties = webToolsProperties;
  }

  @Override
  public String search(String query) {
    if (!webToolsProperties.isEnabled()) {
      return "错误：联网搜索功能已禁用";
    }
    if (!StringUtils.hasText(query)) {
      return "错误：搜索关键词不能为空";
    }
    if (!StringUtils.hasText(tavilyApiKey)) {
      return "错误：未配置 TAVILY_API_KEY，无法执行联网搜索。请在环境变量或 application.yaml 中设置 spring.ai.tavily.api-key";
    }

    WebToolsProperties.Search searchConfig = webToolsProperties.getSearch();
    TavilySearchRequest request = new TavilySearchRequest(
        tavilyApiKey.trim(),
        query.trim(),
        searchConfig.getSearchDepth(),
        searchConfig.getMaxResults(),
        searchConfig.isIncludeAnswer()
    );

    try {
      TavilySearchResponse response = tavilyWebClient.post()
          .uri("/search")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .retrieve()
          .bodyToMono(TavilySearchResponse.class)
          .block();

      return WebSearchResultFormatter.format(response);
    } catch (WebClientResponseException e) {
      log.error("Tavily API 调用失败: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
      return "错误：联网搜索请求失败（HTTP " + e.getStatusCode().value() + "）";
    } catch (Exception e) {
      log.error("联网搜索异常: query={}", query, e);
      return "错误：联网搜索失败 - " + e.getMessage();
    }
  }
}
