package com.kama.jchatmind.service;

import com.kama.jchatmind.message.SseMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseService {
    /**
     * 建立 SSE 连接；连接前会校验当前用户是否拥有该 chatSessionId。
     */
    SseEmitter connect(String chatSessionId);

    void send(String chatSessionId, SseMessage message);
}
