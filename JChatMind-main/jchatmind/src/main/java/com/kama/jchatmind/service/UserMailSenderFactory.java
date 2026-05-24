package com.kama.jchatmind.service;

import com.kama.jchatmind.model.entity.UserMailConfig;
import com.kama.jchatmind.security.MailCredentialEncryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * 根据用户 SMTP 配置动态创建 JavaMailSender
 */
@Component
@RequiredArgsConstructor
public class UserMailSenderFactory {

    private final MailCredentialEncryptor mailCredentialEncryptor;

    /**
     * 为指定用户配置构建邮件发送器
     */
    public JavaMailSender create(UserMailConfig config) {
        String password = mailCredentialEncryptor.decrypt(config.getSmtpPasswordEnc());

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(config.getSmtpHost());
        sender.setPort(config.getSmtpPort());
        // QQ/163 等要求用户名必须为完整邮箱地址
        sender.setUsername(config.getFromEmail().trim());
        sender.setPassword(password);
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        applySmtpProperties(props, config.getSmtpHost(), config.getSmtpPort(), config.getUseSsl());

        return sender;
    }

    /**
     * 按端口配置 SSL / STARTTLS（与 Spring Boot 官方 QQ 邮箱示例保持一致）
     */
    private void applySmtpProperties(Properties props, String host, int port, Boolean useSsl) {
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", "15000");
        props.put("mail.smtp.timeout", "15000");
        props.put("mail.smtp.writetimeout", "15000");
        props.put("mail.smtp.ssl.trust", host);

        if (port == 465) {
            // 465：隐式 SSL（QQ 邮箱常用）
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.fallback", "false");
            return;
        }

        if (port == 587) {
            // 587：STARTTLS（与 application.yaml 中 spring.mail 配置一致）
            props.put("mail.smtp.ssl.enable", "false");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            return;
        }

        // 其它端口：尊重用户「使用 SSL」开关
        boolean ssl = useSsl == null || useSsl;
        if (ssl) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", String.valueOf(port));
            props.put("mail.smtp.socketFactory.fallback", "false");
        } else {
            props.put("mail.smtp.ssl.enable", "false");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }
    }
}
