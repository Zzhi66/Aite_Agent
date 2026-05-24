package com.kama.jchatmind.service.web;

import com.kama.jchatmind.service.web.dto.TavilyResult;
import com.kama.jchatmind.service.web.dto.TavilySearchResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 搜索结果格式化单元测试。
 */
class WebSearchResultFormatterTest {

  @Test
  void shouldFormatResultsWithAnswer() {
    TavilySearchResponse response = new TavilySearchResponse(
        "测试查询",
        "综合回答内容",
        List.of(
            new TavilyResult("标题一", "https://a.com", "摘要一", 0.9),
            new TavilyResult("标题二", "https://b.com", "摘要二", 0.8)
        )
    );

    String formatted = WebSearchResultFormatter.format(response);

    assertTrue(formatted.contains("## 搜索结果（共 2 条）"));
    assertTrue(formatted.contains("[标题一](https://a.com)"));
    assertTrue(formatted.contains("综合摘要: 综合回答内容"));
  }

  @Test
  void shouldHandleEmptyResults() {
    String formatted = WebSearchResultFormatter.format(
        new TavilySearchResponse("q", null, List.of())
    );
    assertTrue(formatted.contains("未找到"));
  }
}
