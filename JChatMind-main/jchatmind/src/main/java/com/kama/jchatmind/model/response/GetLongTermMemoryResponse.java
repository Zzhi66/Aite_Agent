package com.kama.jchatmind.model.response;

import com.kama.jchatmind.model.vo.LongTermMemoryVO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetLongTermMemoryResponse {
    private LongTermMemoryVO memory;
}
