package com.kama.jchatmind.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kama.jchatmind.security.UserContext;
import com.kama.jchatmind.service.EmailApprovalService;
import com.kama.jchatmind.service.EmailService;
import com.kama.jchatmind.service.UserMailConfigService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailToolsTest {
    @AfterEach
    void cleanup() { UserContext.clear(); }

    @Test
    void modelCallOnlyCreatesPendingDraft() throws Exception {
        EmailService sender = mock(EmailService.class);
        var approvals = mock(EmailApprovalService.class);
        when(approvals.create("to@example.com", "标题", "正文")).thenReturn(
                new EmailApprovalService.EmailApproval("draft-id", "to@example.com", "标题", "正文",
                        java.time.Instant.now().plusSeconds(600), EmailApprovalService.Status.PENDING));
        var config = mock(UserMailConfigService.class);
        UserContext.setUserId("owner");
        when(config.isConfigured("owner")).thenReturn(true);
        var tool = new EmailTools(approvals, config);
        var response = new ObjectMapper().readTree(tool.sendEmail("to@example.com", "标题", "正文"));
        assertEquals(EmailApprovalService.TYPE, response.get("type").asText());
        assertEquals("draft-id", response.get("approvalId").asText());
        verify(approvals).create("to@example.com", "标题", "正文");
        verifyNoMoreInteractions(approvals);
        verifyNoInteractions(sender);
    }

    @Test
    void invalidOrUnconfiguredCallsNeverCreateDraft() {
        var approvals = mock(EmailApprovalService.class);
        var config = mock(UserMailConfigService.class);
        var tool = new EmailTools(approvals, config);
        assertTrue(tool.sendEmail("to@example.com", "主题", "正文").contains("错误"));
        UserContext.setUserId("owner");
        assertTrue(tool.sendEmail("to@example.com", "主题", "正文").contains("尚未配置"));
        when(config.isConfigured("owner")).thenReturn(true);
        assertTrue(tool.sendEmail("invalid", "主题", "正文").contains("错误"));
        verifyNoInteractions(approvals);
    }
}
