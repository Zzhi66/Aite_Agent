package com.kama.jchatmind.mapper;

import com.kama.jchatmind.model.entity.EmailApprovalRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EmailApprovalMapper {
    int insert(EmailApprovalRecord record);
    EmailApprovalRecord selectOwned(@Param("id") String id, @Param("userId") String userId);
    int expirePending(@Param("id") String id, @Param("userId") String userId);
    int claimPending(@Param("id") String id, @Param("userId") String userId);
    int cancelPending(@Param("id") String id, @Param("userId") String userId);
    int finishSending(@Param("id") String id, @Param("userId") String userId, @Param("status") String status);
}
