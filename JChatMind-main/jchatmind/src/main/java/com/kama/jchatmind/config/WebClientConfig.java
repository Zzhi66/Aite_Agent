package com.kama.jchatmind.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * 联网工具专用 WebClient 配置。
 */
@Configuration
@EnableConfigurationProperties(WebToolsProperties.class)
public class WebClientConfig {

  public static final String WEB_FETCH_CLIENT = "webFetchWebClient";
  public static final String TAVILY_CLIENT = "tavilyWebClient";

  /**
   * 网页抓取客户端：限制内存、配置超时与 User-Agent。
   */
  @Bean(name = WEB_FETCH_CLIENT)
  public WebClient webFetchWebClient(WebToolsProperties properties) {
    WebToolsProperties.Fetch fetch = properties.getFetch();
    HttpClient httpClient = HttpClient.create()
        .responseTimeout(Duration.ofSeconds(fetch.getTimeoutSeconds()));

    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .defaultHeader("User-Agent", fetch.getUserAgent())
        .codecs(configurer -> configurer
            .defaultCodecs()
            .maxInMemorySize((int) Math.min(fetch.getMaxResponseBytes(), Integer.MAX_VALUE)))
        .build();
  }

  /**
   * Tavily Search API 客户端。
   */
  @Bean(name = TAVILY_CLIENT)
  public WebClient tavilyWebClient(WebToolsProperties properties) {
    HttpClient httpClient = HttpClient.create()
        .responseTimeout(Duration.ofSeconds(properties.getSearch().getTimeoutSeconds()));

    return WebClient.builder()
        .baseUrl("https://api.tavily.com")
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
  }
}
