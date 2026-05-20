package com.kama.jchatmind.controller;

import com.kama.jchatmind.model.common.ApiResponse;
import com.kama.jchatmind.model.request.CreateLongTermMemoryRequest;
import com.kama.jchatmind.model.request.UpdateLongTermMemoryRequest;
import com.kama.jchatmind.model.response.CreateLongTermMemoryResponse;
import com.kama.jchatmind.model.response.GetLongTermMemoriesResponse;
import com.kama.jchatmind.model.response.GetLongTermMemoryResponse;
import com.kama.jchatmind.service.LongTermMemoryFacadeService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class LongTermMemoryController {

    private final LongTermMemoryFacadeService longTermMemoryFacadeService;

    /** 列出当前用户的全部长期记忆，可按类型筛选 */
    @GetMapping("/long-term-memories")
    public ApiResponse<GetLongTermMemoriesResponse> listMemories(
            @RequestParam(required = false) String memoryType
    ) {
        return ApiResponse.success(longTermMemoryFacadeService.listMemories(memoryType));
    }

    @GetMapping("/long-term-memories/{memoryId}")
    public ApiResponse<GetLongTermMemoryResponse> getMemory(@PathVariable String memoryId) {
        return ApiResponse.success(longTermMemoryFacadeService.getMemory(memoryId));
    }

    @PostMapping("/long-term-memories")
    public ApiResponse<CreateLongTermMemoryResponse> createMemory(
            @RequestBody CreateLongTermMemoryRequest request
    ) {
        return ApiResponse.success(longTermMemoryFacadeService.createMemory(request));
    }

    @PatchMapping("/long-term-memories/{memoryId}")
    public ApiResponse<Void> updateMemory(
            @PathVariable String memoryId,
            @RequestBody UpdateLongTermMemoryRequest request
    ) {
        longTermMemoryFacadeService.updateMemory(memoryId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/long-term-memories/{memoryId}")
    public ApiResponse<Void> deleteMemory(@PathVariable String memoryId) {
        longTermMemoryFacadeService.deleteMemory(memoryId);
        return ApiResponse.success();
    }
}
