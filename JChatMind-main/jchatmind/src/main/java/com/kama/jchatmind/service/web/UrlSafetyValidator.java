package com.kama.jchatmind.service.web;

import com.kama.jchatmind.config.WebToolsProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * URL 安全校验，防止 SSRF（内网、本地、非 HTTP 协议等）。
 */
@Component
public class UrlSafetyValidator {

  private static final Set<Integer> ALLOWED_PORTS = Set.of(-1, 80, 443);

  private final Set<String> blockedHostsLower;
  private final Set<String> allowedSchemes;

  public UrlSafetyValidator(WebToolsProperties properties) {
    this.blockedHostsLower = properties.getFetch().getBlockedHosts().stream()
        .map(h -> h.toLowerCase(Locale.ROOT))
        .collect(Collectors.toSet());
    this.allowedSchemes = properties.getFetch().getAllowedSchemes().stream()
        .map(s -> s.toLowerCase(Locale.ROOT))
        .collect(Collectors.toSet());
  }

  /**
   * 校验 URL 是否允许抓取。
   *
   * @return 若不允许则返回错误说明，允许则返回 null
   */
  public String validateOrNull(String url) {
    if (!StringUtils.hasText(url)) {
      return "错误：URL 不能为空";
    }
    URI uri;
    try {
      uri = new URI(url.trim());
    } catch (URISyntaxException e) {
      return "错误：URL 格式无效 - " + e.getMessage();
    }

    String scheme = uri.getScheme();
    if (scheme == null || !allowedSchemes.contains(scheme.toLowerCase(Locale.ROOT))) {
      return "错误：仅允许 HTTP/HTTPS 协议";
    }

    String host = uri.getHost();
    if (!StringUtils.hasText(host)) {
      return "错误：URL 缺少主机名";
    }

    String hostLower = host.toLowerCase(Locale.ROOT);
    if (blockedHostsLower.contains(hostLower)) {
      return "错误：禁止访问该主机 - " + host;
    }

    int port = uri.getPort();
    if (!ALLOWED_PORTS.contains(port)) {
      return "错误：仅允许 80/443 端口";
    }

    try {
      for (InetAddress address : InetAddress.getAllByName(host)) {
        if (isPrivateOrLocal(address)) {
          return "错误：禁止访问内网或本地地址 - " + host;
        }
      }
    } catch (UnknownHostException e) {
      return "错误：无法解析主机 - " + host;
    }

    return null;
  }

  private static boolean isPrivateOrLocal(InetAddress address) {
    if (address.isAnyLocalAddress()
        || address.isLoopbackAddress()
        || address.isLinkLocalAddress()
        || address.isSiteLocalAddress()) {
      return true;
    }
    byte[] bytes = address.getAddress();
    if (bytes.length == 4) {
      int b0 = bytes[0] & 0xFF;
      int b1 = bytes[1] & 0xFF;
      // 10.0.0.0/8
      if (b0 == 10) {
        return true;
      }
      // 172.16.0.0/12
      if (b0 == 172 && b1 >= 16 && b1 <= 31) {
        return true;
      }
      // 192.168.0.0/16
      if (b0 == 192 && b1 == 168) {
        return true;
      }
      // 169.254.0.0/16 link-local
      if (b0 == 169 && b1 == 254) {
        return true;
      }
    }
    return false;
  }
}
