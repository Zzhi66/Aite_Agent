package com.kama.jchatmind.agent.tools;

import com.kama.jchatmind.security.UserContext;
import com.kama.jchatmind.service.EmailApprovalService;
import com.kama.jchatmind.service.UserMailConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailTools implements Tool {

    private final EmailApprovalService emailApprovalService;
    private final UserMailConfigService userMailConfigService;

    public EmailTools(EmailApprovalService emailApprovalService, UserMailConfigService userMailConfigService) {
        this.emailApprovalService = emailApprovalService;
        this.userMailConfigService = userMailConfigService;
    }

    @Override
    public String getName() {
        return "emailTool";
    }

    @Override
    public String getDescription() {
        return "准备邮件并请求用户确认；用户在确认卡片点击发送前，不会发送邮件。需先配置个人 SMTP。";
    }

    @Override
    public ToolType getType() {
        return ToolType.OPTIONAL;
    }

    /**
     * 只创建待确认邮件，不执行发送。确认接口不注册为模型工具。
     */
    @org.springframework.ai.tool.annotation.Tool(
            name = "sendEmail",
            description = "准备待确认邮件，不直接发送。参数：to（收件人）、subject（主题）、content（正文），均必填。用户须先配置 SMTP，再在界面确认卡片上点击发送。"
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

        var approval = emailApprovalService.create(to, subject, content);
        // ID is server-generated UUID; draft contents are retrieved through an owner-checked API.
        return "{\"type\":\"" + EmailApprovalService.TYPE + "\",\"approvalId\":\""
                + approval.id() + "\",\"message\":\"邮件尚未发送，请用户在确认卡片中确认或取消。\"}";
    }
}
