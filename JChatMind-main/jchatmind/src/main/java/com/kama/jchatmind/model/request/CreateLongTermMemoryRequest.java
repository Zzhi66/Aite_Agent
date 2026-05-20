package com.kama.jchatmind.model.request;

import lombok.Data;

@Data
public class CreateLongTermMemoryRequest {
    /** 必填：PREFERENCE / FACT */
    private String memoryType;
    /** 必填：记忆正文 */
    private String content;
    /** 可选：来源智能体，仅溯源 */
    private String sourceAgentId;
    /** 可选：来源会话，仅溯源 */
    private String sourceSessionId;
}
