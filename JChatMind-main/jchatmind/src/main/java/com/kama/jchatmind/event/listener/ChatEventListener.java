package com.kama.jchatmind.event.listener;

import com.kama.jchatmind.agent.JChatMind;
import com.kama.jchatmind.agent.JChatMindFactory;
import com.kama.jchatmind.event.ChatEvent;
import com.kama.jchatmind.security.UserContext;
import lombok.AllArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@AllArgsConstructor
public class ChatEventListener {

    private final JChatMindFactory jChatMindFactory;

    @Async
    @EventListener
    public void handle(ChatEvent event) {
        // 异步线程中恢复用户上下文，供邮件工具等读取当前用户 ID
        if (StringUtils.hasText(event.getUserId())) {
            UserContext.setUserId(event.getUserId());
        }
        try {
            JChatMind jChatMind = jChatMindFactory.create(
                    event.getAgentId(),
                    event.getSessionId(),
                    event.getUserInput()
            );
            jChatMind.run();
        } finally {
            UserContext.clear();
        }
    }
}
