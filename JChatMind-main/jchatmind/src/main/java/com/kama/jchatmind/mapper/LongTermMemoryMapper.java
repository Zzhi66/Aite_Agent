package com.kama.jchatmind.mapper;

import com.kama.jchatmind.model.entity.LongTermMemory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LongTermMemoryMapper {

    int insert(LongTermMemory longTermMemory);

    LongTermMemory selectById(@Param("id") String id);

    List<LongTermMemory> selectByUserId(
            @Param("userId") String userId,
            @Param("memoryType") String memoryType
    );

    int updateById(LongTermMemory longTermMemory);

    int deleteById(@Param("id") String id);

    /** 按用户全量记忆池做向量相似度检索 */
    List<LongTermMemory> similaritySearchByUser(
            @Param("userId") String userId,
            @Param("vectorLiteral") String vectorLiteral,
            @Param("types") List<String> types,
            @Param("limit") int limit
    );

    /** @deprecated 已改为 USER 维度召回，保留仅供兼容 */
    @Deprecated
    List<LongTermMemory> similaritySearchBySession(
            @Param("agentId") String agentId,
            @Param("sessionId") String sessionId,
            @Param("vectorLiteral") String vectorLiteral,
            @Param("types") List<String> types,
            @Param("limit") int limit
    );

    /** @deprecated 已改为 USER 维度召回，保留仅供兼容 */
    @Deprecated
    List<LongTermMemory> similaritySearchByAgent(
            @Param("agentId") String agentId,
            @Param("vectorLiteral") String vectorLiteral,
            @Param("types") List<String> types,
            @Param("limit") int limit
    );
}
