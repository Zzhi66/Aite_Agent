package com.kama.jchatmind.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kama.jchatmind.mapper.LongTermMemoryMapper;
import com.kama.jchatmind.model.entity.LongTermMemory;
import com.kama.jchatmind.security.UserContext;
import com.kama.jchatmind.service.LongTermMemoryService;
import com.kama.jchatmind.service.RagService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 长期记忆 USER 范围召回单元测试：仅按 user_id 检索，不按 agent/session 过滤。
 */
@ExtendWith(MockitoExtension.class)
class LongTermMemoryServiceImplTest {

    @Mock
    private LongTermMemoryMapper longTermMemoryMapper;

    @Mock
    private RagService ragService;

    private LongTermMemoryServiceImpl longTermMemoryService;

    @BeforeEach
    void setUp() {
        longTermMemoryService = new LongTermMemoryServiceImpl(
                longTermMemoryMapper,
                ragService,
                new ObjectMapper()
        );
        ReflectionTestUtils.setField(longTermMemoryService, "enabled", true);
        ReflectionTestUtils.setField(longTermMemoryService, "recallLimit", 6);
        ReflectionTestUtils.setField(longTermMemoryService, "maxDistance", 1.1);
        ReflectionTestUtils.setField(longTermMemoryService, "recallScope", "USER");
        ReflectionTestUtils.setField(longTermMemoryService, "includeTypes", "PREFERENCE,FACT");
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void recallForPrompt_usesUserScopeNotAgentOrSession() {
        UserContext.setUserId("user-a");
        when(ragService.embed("请叫我什么")).thenReturn(new float[]{0.5f, 0.6f});

        LongTermMemory owned = LongTermMemory.builder()
                .memoryType("PREFERENCE")
                .content("请叫我小明")
                .distance(0.2)
                .build();
        when(longTermMemoryMapper.similaritySearchByUser(
                eq("user-a"),
                anyString(),
                anyList(),
                eq(6)
        )).thenReturn(List.of(owned));

        List<LongTermMemoryService.RecallMemory> recalls = longTermMemoryService.recallForPrompt(
                "agent-b",
                "session-other",
                "请叫我什么"
        );

        assertEquals(1, recalls.size());
        assertEquals("请叫我小明", recalls.get(0).getContent());
        assertEquals(LongTermMemoryService.MemoryType.PREFERENCE, recalls.get(0).getType());

        ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(longTermMemoryMapper).similaritySearchByUser(
                userIdCaptor.capture(),
                anyString(),
                anyList(),
                eq(6)
        );
        assertEquals("user-a", userIdCaptor.getValue());
    }

    @Test
    void recallForPrompt_returnsEmptyWhenNotLoggedIn() {
        List<LongTermMemoryService.RecallMemory> recalls = longTermMemoryService.recallForPrompt(
                "agent-1",
                "session-1",
                "你好"
        );
        assertTrue(recalls.isEmpty());
        verify(longTermMemoryMapper, never())
                .similaritySearchByUser(anyString(), anyString(), anyList(), anyInt());
    }

    @Test
    void recallForPrompt_filtersByMaxDistance() {
        UserContext.setUserId("user-a");
        when(ragService.embed("查询")).thenReturn(new float[]{0.1f});

        LongTermMemory near = LongTermMemory.builder()
                .memoryType("FACT")
                .content("我住在上海")
                .distance(0.5)
                .build();
        LongTermMemory far = LongTermMemory.builder()
                .memoryType("FACT")
                .content("无关记忆")
                .distance(1.5)
                .build();
        when(longTermMemoryMapper.similaritySearchByUser(
                eq("user-a"),
                anyString(),
                anyList(),
                eq(6)
        )).thenReturn(List.of(near, far));

        List<LongTermMemoryService.RecallMemory> recalls = longTermMemoryService.recallForPrompt(
                null,
                null,
                "查询"
        );

        assertEquals(1, recalls.size());
        assertEquals("我住在上海", recalls.get(0).getContent());
    }
}
