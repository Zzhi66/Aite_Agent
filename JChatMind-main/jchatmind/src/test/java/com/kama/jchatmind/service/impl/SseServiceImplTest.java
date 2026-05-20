package com.kama.jchatmind.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kama.jchatmind.exception.BizException;
import com.kama.jchatmind.service.ChatSessionFacadeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * SSE 连接前会话归属校验单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SseServiceImplTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ChatSessionFacadeService chatSessionFacadeService;

    @InjectMocks
    private SseServiceImpl sseService;

    @Test
    void connect_verifiesSessionOwnershipBeforeEmitter() {
        SseEmitter emitter = sseService.connect("session-1");

        verify(chatSessionFacadeService).assertSessionOwnedByCurrentUser("session-1");
        assertNotNull(emitter);
    }

    @Test
    void connect_propagatesOwnershipFailure() {
        doThrow(new BizException("无权操作该会话"))
                .when(chatSessionFacadeService)
                .assertSessionOwnedByCurrentUser("other-session");

        assertThrows(BizException.class, () -> sseService.connect("other-session"));
        verify(chatSessionFacadeService).assertSessionOwnedByCurrentUser("other-session");
    }
}
