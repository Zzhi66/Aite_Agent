package com.kama.jchatmind.service;

import com.kama.jchatmind.model.request.CreateLongTermMemoryRequest;
import com.kama.jchatmind.model.request.UpdateLongTermMemoryRequest;
import com.kama.jchatmind.model.response.CreateLongTermMemoryResponse;
import com.kama.jchatmind.model.response.GetLongTermMemoriesResponse;
import com.kama.jchatmind.model.response.GetLongTermMemoryResponse;

public interface LongTermMemoryFacadeService {

    GetLongTermMemoriesResponse listMemories(String memoryType);

    GetLongTermMemoryResponse getMemory(String memoryId);

    CreateLongTermMemoryResponse createMemory(CreateLongTermMemoryRequest request);

    void updateMemory(String memoryId, UpdateLongTermMemoryRequest request);

    void deleteMemory(String memoryId);
}
