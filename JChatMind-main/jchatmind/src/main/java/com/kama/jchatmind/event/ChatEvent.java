package com.kama.jchatmind.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatEvent {
    private String agentId;
    private String sessionId;
    private String userInput;
    /** 发起对话的用户 ID，供异步 Agent 链路使用个人邮箱等资源 */
    private String userId;
}
