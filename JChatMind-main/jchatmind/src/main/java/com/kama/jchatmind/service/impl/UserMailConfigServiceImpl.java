package com.kama.jchatmind.service.impl;

import com.kama.jchatmind.exception.BizException;
import com.kama.jchatmind.mapper.UserMailConfigMapper;
import com.kama.jchatmind.model.entity.UserMailConfig;
import com.kama.jchatmind.security.MailCredentialEncryptor;
import com.kama.jchatmind.service.UserMailConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserMailConfigServiceImpl implements UserMailConfigService {

    private final UserMailConfigMapper userMailConfigMapper;
    private final MailCredentialEncryptor mailCredentialEncryptor;

    @Override
    public UserMailConfig getByUserId(String userId) {
        return userMailConfigMapper.selectByUserId(userId);
    }

    @Override
    public boolean isConfigured(String userId) {
        return getByUserId(userId) != null;
    }

    @Override
    public void saveOrUpdate(String userId, String fromEmail, String smtpHost, Integer smtpPort,
                             String smtpPasswordPlain, Boolean useSsl) {
        if (!StringUtils.hasText(fromEmail) || !fromEmail.contains("@")) {
            throw new BizException("发件邮箱格式不正确");
        }
        if (!StringUtils.hasText(smtpHost)) {
            throw new BizException("SMTP 服务器不能为空");
        }
        if (smtpPort == null || smtpPort <= 0) {
            throw new BizException("SMTP 端口无效");
        }

        UserMailConfig existing = getByUserId(userId);
        String passwordEnc;
        if (StringUtils.hasText(smtpPasswordPlain)) {
            passwordEnc = mailCredentialEncryptor.encrypt(smtpPasswordPlain.trim());
        } else if (existing != null) {
            // 更新时未填写授权码则保留原密文
            passwordEnc = existing.getSmtpPasswordEnc();
        } else {
            throw new BizException("首次配置必须填写 SMTP 授权码");
        }

        // 465/587 与 SSL 开关对齐，避免端口与加密方式不匹配导致认证失败
        boolean ssl = resolveUseSsl(smtpPort, useSsl);
        LocalDateTime now = LocalDateTime.now();

        if (existing == null) {
            UserMailConfig config = UserMailConfig.builder()
                    .userId(userId)
                    .fromEmail(fromEmail.trim())
                    .smtpHost(smtpHost.trim())
                    .smtpPort(smtpPort)
                    .smtpPasswordEnc(passwordEnc)
                    .useSsl(ssl)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            if (userMailConfigMapper.insert(config) <= 0) {
                throw new BizException("保存邮箱配置失败");
            }
        } else {
            UserMailConfig config = UserMailConfig.builder()
                    .userId(userId)
                    .fromEmail(fromEmail.trim())
                    .smtpHost(smtpHost.trim())
                    .smtpPort(smtpPort)
                    .smtpPasswordEnc(passwordEnc)
                    .useSsl(ssl)
                    .build();
            if (userMailConfigMapper.updateByUserId(config) <= 0) {
                throw new BizException("更新邮箱配置失败");
            }
        }
    }

    @Override
    public void deleteByUserId(String userId) {
        userMailConfigMapper.deleteByUserId(userId);
    }

    /** 按常见端口推断是否使用隐式 SSL */
    private boolean resolveUseSsl(Integer smtpPort, Boolean useSsl) {
        if (smtpPort != null && smtpPort == 465) {
            return true;
        }
        if (smtpPort != null && smtpPort == 587) {
            return false;
        }
        return useSsl == null || useSsl;
    }
}
