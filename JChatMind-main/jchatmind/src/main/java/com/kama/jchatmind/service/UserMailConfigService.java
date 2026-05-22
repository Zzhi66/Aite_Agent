package com.kama.jchatmind.service;

import com.kama.jchatmind.model.entity.UserMailConfig;

/**
 * 用户邮箱配置持久化
 */
public interface UserMailConfigService {

    UserMailConfig getByUserId(String userId);

    boolean isConfigured(String userId);

    void saveOrUpdate(String userId, String fromEmail, String smtpHost, Integer smtpPort,
                      String smtpPasswordPlain, Boolean useSsl);

    void deleteByUserId(String userId);
}
