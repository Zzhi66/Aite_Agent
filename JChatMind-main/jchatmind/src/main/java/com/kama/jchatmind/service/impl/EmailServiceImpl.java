package com.kama.jchatmind.service.impl;

import com.kama.jchatmind.exception.BizException;
import com.kama.jchatmind.model.entity.UserMailConfig;
import com.kama.jchatmind.service.EmailService;
import com.kama.jchatmind.service.UserMailConfigService;
import com.kama.jchatmind.service.UserMailSenderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final UserMailConfigService userMailConfigService;
    private final UserMailSenderFactory userMailSenderFactory;

    @Override
    @Async
    public void sendEmailAsync(String userId, String to, String subject, String content) {
        doSend(userId, to, subject, content, false);
    }

    @Override
    public void sendEmailSync(String userId, String to, String subject, String content) {
        doSend(userId, to, subject, content, true);
    }

    /**
     * 使用用户个人 SMTP 配置发送邮件
     */
    private void doSend(String userId, String to, String subject, String content, boolean throwOnError) {
        UserMailConfig config = userMailConfigService.getByUserId(userId);
        if (config == null) {
            log.warn("用户 {} 未配置发件邮箱，无法发送邮件", userId);
            if (throwOnError) {
                throw new BizException("请先配置发件邮箱");
            }
            return;
        }
        try {
            JavaMailSender mailSender = userMailSenderFactory.create(config);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(config.getFromEmail());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("邮件发送成功，用户: {}, 发件人: {}, 收件人: {}, 主题: {}",
                    userId, config.getFromEmail(), to, subject);
        } catch (Exception e) {
            log.error("邮件发送失败，用户: {}, 收件人: {}, 主题: {}, 错误: {}",
                    userId, to, subject, e.getMessage(), e);
            if (throwOnError) {
                String hint = buildSendFailureHint(e);
                throw new BizException(hint);
            }
        }
    }

    /** 根据异常类型给出更易排查的中文提示 */
    private String buildSendFailureHint(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        String lower = msg.toLowerCase();
        if (lower.contains("authentication") || lower.contains("auth")) {
            return "SMTP 认证失败：请确认 (1) 填写的是 QQ 邮箱「授权码」而非登录密码；"
                    + "(2) 已在 QQ 邮箱网页版 → 设置 → 账户 中开启 SMTP 并生成授权码；"
                    + "(3) 发件邮箱与授权码所属账号一致。"
                    + "推荐配置：服务器 smtp.qq.com，端口 587，关闭 SSL；或端口 465，开启 SSL。"
                    + "保存时请重新填写授权码后再点「发送测试邮件」。";
        }
        return "邮件发送失败: " + msg;
    }
}
