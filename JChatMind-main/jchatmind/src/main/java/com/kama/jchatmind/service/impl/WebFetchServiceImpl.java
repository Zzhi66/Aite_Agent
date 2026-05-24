package com.kama.jchatmind.service.impl;

import com.kama.jchatmind.config.WebClientConfig;
import com.kama.jchatmind.config.WebToolsProperties;
import com.kama.jchatmind.service.WebFetchService;
import com.kama.jchatmind.service.web.HtmlTextExtractor;
import com.kama.jchatmind.service.web.UrlSafetyValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.time.Duration;

/**
 * 网页抓取实现：SSRF 校验 + WebClient 下载 + Jsoup 正文提取。
 */
@Slf4j
@Service
public class WebFetchServiceImpl implements WebFetchService {

  private final WebClient baseWebClient;
  private final WebToolsProperties webToolsProperties;
  private final UrlSafetyValidator urlSafetyValidator;

  public WebFetchServiceImpl(
      @Qualifier(WebClientConfig.WEB_FETCH_CLIENT) WebClient baseWebClient,
      WebToolsProperties webToolsProperties,
      UrlSafetyValidator urlSafetyValidator
  ) {
    this.baseWebClient = baseWebClient;
    this.webToolsProperties = webToolsProperties;
    this.urlSafetyValidator = urlSafetyValidator;
  }

  @Override
  public String fetch(String url) {
    if (!webToolsProperties.isEnabled()) {
      return "错误：网页抓取功能已禁用";
    }

    String safetyError = urlSafetyValidator.validateOrNull(url);
    if (safetyError != null) {
      return safetyError;
    }

    String normalizedUrl = url.trim();
    WebToolsProperties.Fetch fetchConfig = webToolsProperties.getFetch();

    try {
      // 禁用自动重定向，手动跟随并在每一步重新校验 URL
      HttpClient httpClient = HttpClient.create()
          .responseTimeout(Duration.ofSeconds(fetchConfig.getTimeoutSeconds()))
          .followRedirect(false);

      WebClient fetchClient = baseWebClient.mutate()
          .clientConnector(new ReactorClientHttpConnector(httpClient))
          .build();

      String html = fetchWithRedirect(fetchClient, normalizedUrl, 5);
      if (html == null) {
        return "错误：网页抓取失败（重定向次数过多或目标不可达）";
      }

      HtmlTextExtractor.ExtractedPage page = HtmlTextExtractor.extract(html, normalizedUrl);
      String body = page.bodyText();
      if (!StringUtils.hasText(body)) {
        return "错误：未能从页面提取到正文内容";
      }

      int maxChars = fetchConfig.getMaxContentChars();
      if (body.length() > maxChars) {
        body = body.substring(0, maxChars) + "\n...(正文已截断)";
      }

      return """
          URL: %s
          标题: %s
          正文:
          %s
          """.formatted(normalizedUrl, page.title(), body).trim();
    } catch (WebClientResponseException e) {
      log.error("网页抓取 HTTP 失败: url={}, status={}", normalizedUrl, e.getStatusCode(), e);
      return "错误：网页抓取失败（HTTP " + e.getStatusCode().value() + "）";
    } catch (Exception e) {
      log.error("网页抓取异常: url={}", normalizedUrl, e);
      return "错误：网页抓取失败 - " + e.getMessage();
    }
  }

  /**
   * 手动处理重定向，每次跳转前重新做 SSRF 校验。
   */
  private String fetchWithRedirect(WebClient client, String url, int maxRedirects) throws Exception {
    String currentUrl = url;
    for (int i = 0; i <= maxRedirects; i++) {
      String safetyError = urlSafetyValidator.validateOrNull(currentUrl);
      if (safetyError != null) {
        throw new IllegalStateException(safetyError.replace("错误：", ""));
      }

      var response = client.get()
          .uri(URI.create(currentUrl))
          .header(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,text/plain;q=0.9,*/*;q=0.8")
          .exchangeToMono(clientResponse -> {
            if (clientResponse.statusCode().is3xxRedirection()) {
              String location = clientResponse.headers().asHttpHeaders().getFirst(HttpHeaders.LOCATION);
              return clientResponse.releaseBody()
                  .then(Mono.just("REDIRECT:" + location));
            }
            return clientResponse.bodyToMono(String.class);
          })
          .block();

      if (response == null) {
        return null;
      }
      if (response.startsWith("REDIRECT:")) {
        String location = response.substring("REDIRECT:".length());
        if (!StringUtils.hasText(location)) {
          return null;
        }
        currentUrl = resolveRedirectUrl(currentUrl, location.trim());
        continue;
      }
      return response;
    }
    return null;
  }

  private static String resolveRedirectUrl(String baseUrl, String location) throws Exception {
    URI base = URI.create(baseUrl);
    URI resolved = base.resolve(location);
    return resolved.toString();
  }
}
