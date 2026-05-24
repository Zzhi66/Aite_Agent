package com.kama.jchatmind.mapper;

import com.kama.jchatmind.model.entity.UserMailConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMailConfigMapper {

    UserMailConfig selectByUserId(String userId);

    int insert(UserMailConfig config);

    int updateByUserId(UserMailConfig config);

    int deleteByUserId(String userId);
}
