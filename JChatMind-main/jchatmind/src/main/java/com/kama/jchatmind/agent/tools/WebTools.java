package com.kama.jchatmind.agent.tools;

import com.kama.jchatmind.service.WebFetchService;
import com.kama.jchatmind.service.WebSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 联网搜索与网页抓取 Agent 工具。
 */
@Slf4j
@Component
public class WebTools implements Tool {

  private final WebSearchService webSearchService;
  private final WebFetchService webFetchService;

  public WebTools(WebSearchService webSearchService, WebFetchService webFetchService) {
    this.webSearchService = webSearchService;
    this.webFetchService = webFetchService;
  }

  @Override
  public String getName() {
    return "webTool";
  }

  @Override
  public String getDescription() {
    return "联网搜索与网页抓取。webSearch 用于检索实时互联网信息；fetchUrl 用于读取指定网页正文。需在服务端配置 TAVILY_API_KEY 后方可搜索。";
  }

  @Override
  public ToolType getType() {
    return ToolType.OPTIONAL;
  }

  /**
   * 使用 Tavily 执行联网搜索。
   */
  @org.springframework.ai.tool.annotation.Tool(
      name = "webSearch",
      description = "在互联网上搜索实时信息。参数 query：搜索关键词或自然语言问题。适用于新闻、天气、股价、最新事件等知识库中没有的内容。"
  )
  public String webSearch(String query) {
    log.info("执行联网搜索: query={}", query);
    return webSearchService.search(query);
  }

  /**
   * 抓取并提取指定 URL 的网页正文。
   */
  @org.springframework.ai.tool.annotation.Tool(
      name = "fetchUrl",
      description = "抓取指定网页并提取正文纯文本。参数 url：完整 HTTP/HTTPS 地址。适用于 webSearch 摘要不足、需要阅读单页详情时使用。"
  )
  public String fetchUrl(String url) {
    log.info("执行网页抓取: url={}", url);
    return webFetchService.fetch(url);
  }
}
