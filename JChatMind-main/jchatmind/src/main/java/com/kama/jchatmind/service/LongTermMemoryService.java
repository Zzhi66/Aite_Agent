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
        SESSION("SESSION"),
        AGENT("AGENT");

        private final String code;
    }

    @Data
    @Builder
    class RecallMemory {
        private MemoryType type;
        private String content;
        private Double distance;
    }
}
