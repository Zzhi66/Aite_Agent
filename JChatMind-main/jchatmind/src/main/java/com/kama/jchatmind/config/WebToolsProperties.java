package com.kama.jchatmind.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 联网搜索与网页抓取工具配置。
 */
@Data
@ConfigurationProperties(prefix = "jchatmind.web")
public class WebToolsProperties {

    /** 是否启用联网工具 */
    private boolean enabled = true;

    private Search search = new Search();

    private Fetch fetch = new Fetch();

    @Data
    public static class Search {
        /** 单次搜索最大结果数 */
        private int maxResults = 5;
        /** Tavily 搜索深度：basic | advanced */
        private String searchDepth = "basic";
        /** HTTP 超时（秒） */
        private int timeoutSeconds = 15;
        /** 是否请求 Tavily 综合摘要 */
        private boolean includeAnswer = true;
    }

    @Data
    public static class Fetch {
        /** 返回正文最大字符数 */
        private int maxContentChars = 12000;
        /** HTTP 超时（秒） */
        private int timeoutSeconds = 10;
        /** 响应体最大字节数 */
        private long maxResponseBytes = 2_097_152L;
        /** 请求 User-Agent */
        private String userAgent = "JChatMind-Agent/1.0";
        /** 禁止访问的主机名（逗号分隔配置） */
        private List<String> blockedHosts = List.of("localhost", "127.0.0.1", "0.0.0.0", "::1");
        /** 允许的 URL 协议 */
        private List<String> allowedSchemes = List.of("https", "http");
    }
}
