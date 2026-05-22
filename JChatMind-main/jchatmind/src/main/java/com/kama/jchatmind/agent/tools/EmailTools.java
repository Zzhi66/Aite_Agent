package com.kama.jchatmind.agent.tools;

import com.kama.jchatmind.security.UserContext;
import com.kama.jchatmind.service.EmailService;
import com.kama.jchatmind.service.UserMailConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailTools implements Tool {

    private final EmailService emailService;
    private final UserMailConfigService userMailConfigService;

    public EmailTools(EmailService emailService, UserMailConfigService userMailConfigService) {
        this.emailService = emailService;
        this.userMailConfigService = userMailConfigService;
    }

    @Override
    public String getName() {
        return "emailTool";
    }

    @Override
    public String getDescription() {
        return "使用当前登录用户已配置的个人邮箱发送邮件。用户需先在「邮箱设置」中配置 SMTP；邮件异步发送。";
    }

    @Override
    public ToolType getType() {
        return ToolType.OPTIONAL;
    }

    /**
     * 发送邮件（异步执行，发件人为用户自己配置的邮箱）
     */
    @org.springframework.ai.tool.annotation.Tool(
            name = "sendEmail",
            description = "使用当前用户的个人邮箱发送邮件。参数：to（收件人，必填）、subject（主题，必填）、content（正文，必填）。用户须已配置 SMTP。"
    )
    public String sendEmail(String to, String subject, String content) {
        String userId;
        try {
            userId = UserContext.requireUserId();
        } catch (IllegalStateException e) {
            return "错误：无法识别当前用户，请重新登录后再试";
        }

        if (!userMailConfigService.isConfigured(userId)) {
            return "错误：您尚未配置个人发件邮箱。请在应用内打开「邮箱设置」，填写邮箱地址与 SMTP 授权码后再使用发信功能。";
        }

        if (to == null || to.trim().isEmpty()) {
            return "错误：收件人邮箱地址不能为空";
        }
        if (subject == null || subject.trim().isEmpty()) {
            return "错误：邮件主题不能为空";
        }
        if (content == null || content.trim().isEmpty()) {
            return "错误：邮件内容不能为空";
        }
        if (!to.contains("@")) {
            return "错误：收件人邮箱地址格式不正确";
        }

        emailService.sendEmailAsync(userId, to.trim(), subject.trim(), content.trim());

        log.info("邮件已提交异步发送，用户: {}, 收件人: {}, 主题: {}", userId, to, subject);
        return String.format(
                "邮件已提交发送（将使用您配置的个人邮箱作为发件人）！\n收件人: %s\n主题: %s\n正在后台发送...",
                to, subject
        );
    }
}
