package com.kama.jchatmind.service.impl;

import com.kama.jchatmind.config.WebToolsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 联网搜索服务单元测试（无 API Key 场景）。
 */
@ExtendWith(MockitoExtension.class)
class WebSearchServiceImplTest {

  @Mock
  private WebClient tavilyWebClient;

  private WebSearchServiceImpl webSearchService;

  @BeforeEach
  void setUp() {
    WebToolsProperties properties = new WebToolsProperties();
    webSearchService = new WebSearchServiceImpl(tavilyWebClient, properties);
    ReflectionTestUtils.setField(webSearchService, "tavilyApiKey", "");
  }

  @Test
  void shouldReturnErrorWhenApiKeyMissing() {
    String result = webSearchService.search("今日新闻");
    assertTrue(result.contains("TAVILY_API_KEY"));
  }

  @Test
  void shouldReturnErrorWhenQueryEmpty() {
    ReflectionTestUtils.setField(webSearchService, "tavilyApiKey", "tvly-test");
    String result = webSearchService.search("  ");
    assertTrue(result.contains("不能为空"));
  }
}
