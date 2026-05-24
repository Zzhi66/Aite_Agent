package com.kama.jchatmind.service;

/**
 * 联网搜索服务（Tavily）。
 */
public interface WebSearchService {

  /**
   * 执行联网搜索并返回格式化文本。
   *
   * @param query 搜索关键词或自然语言问题
   * @return 供 LLM 使用的搜索结果文本；失败时返回以「错误：」开头的说明
   */
  String search(String query);
}
