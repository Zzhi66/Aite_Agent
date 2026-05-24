package com.kama.jchatmind.service.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HTML 正文提取单元测试。
 */
class HtmlTextExtractorTest {

  @Test
  void shouldExtractTitleAndBody() {
    String html = """
        <!DOCTYPE html>
        <html><head><title>测试页面</title></head>
        <body>
          <script>alert(1)</script>
          <article><p>这是正文内容。</p></article>
        </body></html>
        """;

    HtmlTextExtractor.ExtractedPage page = HtmlTextExtractor.extract(html, "https://example.com");

    assertTrue(page.title().contains("测试页面"));
    assertTrue(page.bodyText().contains("这是正文内容"));
    assertTrue(!page.bodyText().contains("alert"));
  }
}
