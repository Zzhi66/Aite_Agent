package com.kama.jchatmind.model.request;

import lombok.Data;

@Data
public class UpdateLongTermMemoryRequest {
    private String memoryType;
    private String content;
}
