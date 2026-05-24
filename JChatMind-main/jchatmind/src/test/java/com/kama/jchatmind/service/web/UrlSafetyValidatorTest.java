package com.kama.jchatmind.service.web;

import com.kama.jchatmind.config.WebToolsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * URL 安全校验单元测试。
 */
class UrlSafetyValidatorTest {

  private UrlSafetyValidator validator;

  @BeforeEach
  void setUp() {
    WebToolsProperties properties = new WebToolsProperties();
    validator = new UrlSafetyValidator(properties);
  }

  @Test
  void shouldRejectLocalhost() {
    assertNotNull(validator.validateOrNull("http://localhost/page"));
  }

  @Test
  void shouldRejectPrivateIp() {
    assertNotNull(validator.validateOrNull("http://192.168.1.1/page"));
  }

  @Test
  void shouldRejectNonHttpScheme() {
    assertNotNull(validator.validateOrNull("file:///etc/passwd"));
  }

  @Test
  void shouldAllowPublicHttps() {
    assertNull(validator.validateOrNull("https://www.example.com/article"));
  }
}
