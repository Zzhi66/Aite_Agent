package com.kama.jchatmind.service.web;

import com.kama.jchatmind.service.web.dto.TavilySearchResponse;
import org.springframework.util.StringUtils;

/**
 * 将 Tavily 搜索结果格式化为便于 LLM 阅读的文本。
 */
public final class WebSearchResultFormatter {

  private WebSearchResultFormatter() {
  }

  public static String format(TavilySearchResponse response) {
    if (response == null || response.results() == null || response.results().isEmpty()) {
      return "未找到相关搜索结果。";
    }

    StringBuilder builder = new StringBuilder();
    builder.append("## 搜索结果（共 ").append(response.results().size()).append(" 条）\n");

    int index = 1;
    for (var result : response.results()) {
      builder.append(index++).append(". ");
      if (StringUtils.hasText(result.title())) {
        builder.append("[").append(result.title()).append("]");
      } else {
        builder.append("[无标题]");
      }
      if (StringUtils.hasText(result.url())) {
        builder.append("(").append(result.url()).append(")");
      }
      builder.append("\n");
      if (StringUtils.hasText(result.content())) {
        builder.append("   摘要: ").append(result.content().trim()).append("\n");
      }
    }

    if (StringUtils.hasText(response.answer())) {
      builder.append("---\n综合摘要: ").append(response.answer().trim());
    }

    return builder.toString().trim();
  }
}
