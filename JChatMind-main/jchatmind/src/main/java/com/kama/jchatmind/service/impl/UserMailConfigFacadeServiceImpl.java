package com.kama.jchatmind.service.impl;

import com.kama.jchatmind.exception.BizException;
import com.kama.jchatmind.model.entity.UserMailConfig;
import com.kama.jchatmind.model.request.SaveUserMailConfigRequest;
import com.kama.jchatmind.model.response.GetUserMailConfigResponse;
import com.kama.jchatmind.model.vo.UserMailConfigVO;
import com.kama.jchatmind.security.UserContext;
import com.kama.jchatmind.service.EmailService;
import com.kama.jchatmind.service.UserMailConfigFacadeService;
import com.kama.jchatmind.service.UserMailConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserMailConfigFacadeServiceImpl implements UserMailConfigFacadeService {

    private final UserMailConfigService userMailConfigService;
    private final EmailService emailService;

    @Override
    public GetUserMailConfigResponse getMyConfig() {
        String userId = UserContext.requireUserId();
        UserMailConfig config = userMailConfigService.getByUserId(userId);
        if (config == null) {
            return GetUserMailConfigResponse.builder()
                    .config(UserMailConfigVO.builder()
                            .configured(false)
                            .passwordSet(false)
                            .build())
                    .build();
        }
        return GetUserMailConfigResponse.builder()
                .config(UserMailConfigVO.builder()
                        .configured(true)
                        .fromEmail(config.getFromEmail())
                        .smtpHost(config.getSmtpHost())
                        .smtpPort(config.getSmtpPort())
                        .useSsl(config.getUseSsl())
                        .passwordSet(true)
                        .build())
                .build();
    }

    @Override
    public void saveMyConfig(SaveUserMailConfigRequest request) {
        String userId = UserContext.requireUserId();
        String host = request.getSmtpHost() != null ? request.getSmtpHost() : "smtp.qq.com";
        Integer port = request.getSmtpPort() != null ? request.getSmtpPort() : 465;
        userMailConfigService.saveOrUpdate(
                userId,
                request.getFromEmail(),
                host,
                port,
                request.getSmtpPassword(),
                request.getUseSsl()
        );
    }

    @Override
    public void sendTestMail() {
        String userId = UserContext.requireUserId();
        UserMailConfig config = userMailConfigService.getByUserId(userId);
        if (config == null) {
            throw new BizException("请先配置发件邮箱");
        }
        // 同步发送测试邮件，便于用户立即确认配置是否正确
        emailService.sendEmailSync(
                userId,
                config.getFromEmail(),
                "JChatMind 邮箱配置测试",
                "这是一封测试邮件。若您能收到，说明 SMTP 配置正确，智能体将使用您的邮箱地址发信。"
        );
    }
}
