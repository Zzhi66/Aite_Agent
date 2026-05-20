package com.kama.jchatmind.service.impl;

import com.kama.jchatmind.converter.ChatSessionConverter;
import com.kama.jchatmind.exception.BizException;
import com.kama.jchatmind.mapper.ChatSessionMapper;
import com.kama.jchatmind.model.entity.ChatSession;
import com.kama.jchatmind.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * 聊天会话归属断言单元测试。
 */
@ExtendWith(MockitoExtension.class)
class ChatSessionFacadeServiceImplTest {

    @Mock
    private ChatSessionMapper chatSessionMapper;

    @Mock
    private ChatSessionConverter chatSessionConverter;

    @InjectMocks
    private ChatSessionFacadeServiceImpl chatSessionFacadeService;

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void assertSessionOwnedByCurrentUser_allowsOwner() {
        UserContext.setUserId("user-1");
        ChatSession session = ChatSession.builder()
                .id("session-1")
                .userId("user-1")
                .build();
        when(chatSessionMapper.selectById("session-1")).thenReturn(session);

        assertDoesNotThrow(() ->
                chatSessionFacadeService.assertSessionOwnedByCurrentUser("session-1"));
    }

    @Test
    void assertSessionOwnedByCurrentUser_rejectsOtherUser() {
        UserContext.setUserId("user-1");
        ChatSession session = ChatSession.builder()
                .id("session-1")
                .userId("user-2")
                .build();
        when(chatSessionMapper.selectById("session-1")).thenReturn(session);

        BizException ex = assertThrows(BizException.class, () ->
                chatSessionFacadeService.assertSessionOwnedByCurrentUser("session-1"));
        assertEquals("无权操作该会话", ex.getMessage());
    }

    @Test
    void assertSessionOwnedByCurrentUser_rejectsNullUserIdOnSession() {
        UserContext.setUserId("user-1");
        ChatSession session = ChatSession.builder()
                .id("session-1")
                .userId(null)
                .build();
        when(chatSessionMapper.selectById("session-1")).thenReturn(session);

        BizException ex = assertThrows(BizException.class, () ->
                chatSessionFacadeService.assertSessionOwnedByCurrentUser("session-1"));
        assertEquals("无权操作该会话", ex.getMessage());
    }

    @Test
    void assertSessionOwnedByCurrentUser_rejectsMissingSession() {
        UserContext.setUserId("user-1");
        when(chatSessionMapper.selectById("missing")).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () ->
                chatSessionFacadeService.assertSessionOwnedByCurrentUser("missing"));
        assertEquals("聊天会话不存在: missing", ex.getMessage());
    }

    @Test
    void assertSessionOwnedByCurrentUser_requiresLogin() {
        ChatSession session = ChatSession.builder()
                .id("session-1")
                .userId("user-1")
                .build();
        when(chatSessionMapper.selectById("session-1")).thenReturn(session);

        assertThrows(IllegalStateException.class, () ->
                chatSessionFacadeService.assertSessionOwnedByCurrentUser("session-1"));
    }
}
