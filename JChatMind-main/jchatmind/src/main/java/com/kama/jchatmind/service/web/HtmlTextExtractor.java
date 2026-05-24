package com.kama.jchatmind.service.web;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.util.StringUtils;

/**
 * 使用 Jsoup 从 HTML 提取可读正文。
 */
public final class HtmlTextExtractor {

  private HtmlTextExtractor() {
  }

  /**
   * 解析 HTML 并提取标题与正文纯文本。
   */
  public static ExtractedPage extract(String html, String pageUrl) {
    Document document = Jsoup.parse(html, pageUrl);
    document.select("script, style, noscript, iframe, svg").remove();

    String title = document.title();
    if (!StringUtils.hasText(title)) {
      Element h1 = document.selectFirst("h1");
      if (h1 != null) {
        title = h1.text();
      }
    }

    Element main = document.selectFirst("article, main, [role=main]");
    String bodyText;
    if (main != null) {
      bodyText = main.text();
    } else {
      Element body = document.body();
      bodyText = body != null ? body.text() : document.text();
    }

    return new ExtractedPage(
        StringUtils.hasText(title) ? title.trim() : "无标题",
        bodyText != null ? bodyText.trim() : ""
    );
  }

  public record ExtractedPage(String title, String bodyText) {
  }
}
