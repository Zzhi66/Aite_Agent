package com.kama.jchatmind.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kama.jchatmind.exception.BizException;
import com.kama.jchatmind.mapper.LongTermMemoryMapper;
import com.kama.jchatmind.model.entity.LongTermMemory;
import com.kama.jchatmind.model.request.CreateLongTermMemoryRequest;
import com.kama.jchatmind.model.request.UpdateLongTermMemoryRequest;
import com.kama.jchatmind.model.response.CreateLongTermMemoryResponse;
import com.kama.jchatmind.model.response.GetLongTermMemoryResponse;
import com.kama.jchatmind.security.UserContext;
import com.kama.jchatmind.service.RagService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 长期记忆 Facade 归属校验与创建行为单元测试。
 */
@ExtendWith(MockitoExtension.class)
class LongTermMemoryFacadeServiceImplTest {

    @Mock
    private LongTermMemoryMapper longTermMemoryMapper;

    @Mock
    private RagService ragService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private LongTermMemoryFacadeServiceImpl facadeService;

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void getMemory_rejectsOtherUser() {
        UserContext.setUserId("user-a");
        LongTermMemory memory = LongTermMemory.builder()
                .id("mem-1")
                .userId("user-b")
                .memoryType("PREFERENCE")
                .content("请叫我小明")
                .build();
        when(longTermMemoryMapper.selectById("mem-1")).thenReturn(memory);

        BizException ex = assertThrows(BizException.class, () -> facadeService.getMemory("mem-1"));
        assertEquals("无权操作该记忆", ex.getMessage());
    }

    @Test
    void getMemory_allowsOwner() {
        UserContext.setUserId("user-a");
        LongTermMemory memory = LongTermMemory.builder()
                .id("mem-1")
                .userId("user-a")
                .memoryType("FACT")
                .content("我住在上海")
                .build();
        when(longTermMemoryMapper.selectById("mem-1")).thenReturn(memory);

        GetLongTermMemoryResponse response = facadeService.getMemory("mem-1");
        assertEquals("mem-1", response.getMemory().getId());
        assertEquals("我住在上海", response.getMemory().getContent());
    }

    @Test
    void createMemory_withoutAgentId_writesUserScopedRow() {
        UserContext.setUserId("user-a");
        when(ragService.embed("喜欢深色主题")).thenReturn(new float[]{0.1f, 0.2f});
        when(longTermMemoryMapper.insert(any(LongTermMemory.class))).thenAnswer(invocation -> {
            LongTermMemory row = invocation.getArgument(0);
            row.setId("new-mem-id");
            return 1;
        });

        CreateLongTermMemoryRequest request = new CreateLongTermMemoryRequest();
        request.setMemoryType("PREFERENCE");
        request.setContent("喜欢深色主题");

        CreateLongTermMemoryResponse response = facadeService.createMemory(request);
        assertEquals("new-mem-id", response.getMemoryId());

        ArgumentCaptor<LongTermMemory> captor = ArgumentCaptor.forClass(LongTermMemory.class);
        verify(longTermMemoryMapper).insert(captor.capture());
        LongTermMemory inserted = captor.getValue();
        assertEquals("user-a", inserted.getUserId());
        assertNull(inserted.getAgentId());
        assertNull(inserted.getSessionId());
        assertEquals("PREFERENCE", inserted.getMemoryType());
    }

    @Test
    void deleteMemory_rejectsOtherUser() {
        UserContext.setUserId("user-a");
        LongTermMemory memory = LongTermMemory.builder()
                .id("mem-2")
                .userId("user-b")
                .build();
        when(longTermMemoryMapper.selectById("mem-2")).thenReturn(memory);

        BizException ex = assertThrows(BizException.class, () -> facadeService.deleteMemory("mem-2"));
        assertEquals("无权操作该记忆", ex.getMessage());
    }

    @Test
    void updateMemory_rejectsNullUserIdOnLegacyRow() {
        UserContext.setUserId("user-a");
        LongTermMemory memory = LongTermMemory.builder()
                .id("legacy-mem")
                .userId(null)
                .content("旧数据")
                .memoryType("FACT")
                .build();
        when(longTermMemoryMapper.selectById("legacy-mem")).thenReturn(memory);

        UpdateLongTermMemoryRequest request = new UpdateLongTermMemoryRequest();
        request.setContent("新内容");

        BizException ex = assertThrows(BizException.class, () ->
                facadeService.updateMemory("legacy-mem", request));
        assertEquals("无权操作该记忆", ex.getMessage());
    }
}
