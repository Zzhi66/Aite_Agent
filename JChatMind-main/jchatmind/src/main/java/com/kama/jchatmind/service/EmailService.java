package com.kama.jchatmind.service;

/**
 * 邮件服务接口（按用户个人 SMTP 配置发信）
 */
public interface EmailService {

    /**
     * 异步发送邮件（使用指定用户的邮箱配置）
     *
     * @param userId  发件用户 ID
     * @param to      收件人邮箱地址
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    void sendEmailAsync(String userId, String to, String subject, String content);

    /**
     * 同步发送邮件（用于配置测试等需要立即反馈的场景）
     */
    void sendEmailSync(String userId, String to, String subject, String content);
}
