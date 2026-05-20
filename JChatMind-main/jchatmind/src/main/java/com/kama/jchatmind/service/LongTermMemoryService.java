package com.kama.jchatmind.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.List;

public interface LongTermMemoryService {

    void ingestFromUserInput(String agentId, String sessionId, String userInput);

    List<RecallMemory> recallForPrompt(String agentId, String sessionId, String query);

    @Getter
    @AllArgsConstructor
    enum MemoryType {
        PREFERENCE("PREFERENCE"),
        FACT("FACT");

        private final String code;
    }

    @Getter
    @AllArgsConstructor
    enum RecallScope {
        /** 按用户全量记忆池召回（默认） */
        USER("USER"),
        /** @deprecated 已由 USER 替代 */
        SESSION("SESSION"),
        /** @deprecated 已由 USER 替代 */
        AGENT("AGENT");

        private final String code;

        public boolean isUserScope() {
            return this == USER;
        }
    }

    @Data
    @Builder
    class RecallMemory {
        private MemoryType type;
        private String content;
        private Double distance;
    }
}
