package com.kama.jchatmind.service;

/**
 * 网页正文抓取服务。
 */
public interface WebFetchService {

  /**
   * 抓取 URL 页面并提取正文文本。
   *
   * @param url 目标网页地址（须通过 SSRF 校验）
   * @return 格式化正文；失败时返回以「错误：」开头的说明
   */
  String fetch(String url);
}
