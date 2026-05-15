package com.kama.jchatmind.mapper;

import com.kama.jchatmind.model.entity.LongTermMemory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LongTermMemoryMapper {

    int insert(LongTermMemory longTermMemory);

    List<LongTermMemory> similaritySearchBySession(
            @Param("agentId") String agentId,
            @Param("sessionId") String sessionId,
            @Param("vectorLiteral") String vectorLiteral,
            @Param("types") List<String> types,
            @Param("limit") int limit
    );

    List<LongTermMemory> similaritySearchByAgent(
            @Param("agentId") String agentId,
            @Param("vectorLiteral") String vectorLiteral,
            @Param("types") List<String> types,
            @Param("limit") int limit
    );
}
