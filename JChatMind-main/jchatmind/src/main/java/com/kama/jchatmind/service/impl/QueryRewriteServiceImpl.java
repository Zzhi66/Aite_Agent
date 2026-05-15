package com.kama.jchatmind.service.impl;

import com.kama.jchatmind.config.ChatClientRegistry;
import com.kama.jchatmind.service.QueryRewriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 查询重写服务实现。
 *
 * <p>通过一个独立的 LLM 调用，将用户自然语言问题改写为更适合检索的 query。
 * 当前实现强调“稳定、短、可检索”，并对输出做基本清洗与长度限制。</p>
 */
@Slf4j
@Service
public class QueryRewriteServiceImpl implements QueryRewriteService {

    /**
     * 用于获取配置好的 ChatClient（不同模型）。
     */
    private final ChatClientRegistry chatClientRegistry;

    /**
     * 是否启用查询重写。
     */
    @Value("${jchatmind.query-rewrite.enabled:true}")
    private boolean enabled;

    /**
     * 使用哪个模型做查询重写（来自 ChatClientRegistry 的 key）。
     */
    @Value("${jchatmind.query-rewrite.model:deepseek-chat}")
    private String model;

    /**
     * 重写结果最大字符数（防止输出太长污染上下文）。
     */
    @Value("${jchatmind.query-rewrite.max-chars:120}")
    private int maxChars;

    /**
     * 当输入过短时是否跳过重写（过短通常不需要重写，或可直接用原文）。
     */
    @Value("${jchatmind.query-rewrite.skip-if-shorter-than:6}")
    private int skipIfShorterThan;

    public QueryRewriteServiceImpl(ChatClientRegistry chatClientRegistry) {
        this.chatClientRegistry = chatClientRegistry;
    }

    @Override
    public String rewrite(String userQuery) {
        // 兜底：空输入直接返回空字符串，避免下游 NPE
        if (!StringUtils.hasText(userQuery)) {
            return "";
        }

        // 关闭开关时，直接返回清洗后的原文
        String cleaned = cleanup(userQuery);
        if (!enabled) {
            return truncate(cleaned);
        }

        // 输入太短时，通常没有重写价值，直接返回原文（减少一次 LLM 调用）
        if (cleaned.length() < skipIfShorterThan) {
            return truncate(cleaned);
        }

        ChatClient chatClient = chatClientRegistry.get(model);
        if (chatClient == null) {
            log.warn("Query rewrite model not found in registry: {}", model);
            return truncate(cleaned);
        }

        try {
            // system：固定约束，保证输出稳定可解析（只输出 query，不要解释）
            SystemMessage system = new SystemMessage("""
                    你是“检索查询重写器”，任务是把用户问题改写为适合检索的 Query。
                    要求：
                    1) 输出一行中文 Query（不要换行、不要编号、不要解释、不要加引号）。
                    2) 保留关键实体/约束（人名、产品名、版本、时间、地点、数量等），补全指代。
                    3) 删除寒暄与无关背景，避免泛化词（例如“帮我看看”“怎么样”）。
                    4) Query 尽量短（<= 25 个中文词或 <= 120 字符），必要时用空格分隔关键短语。
                    """);

            // user：只提供原始问题（当前模块先不引入历史上下文，后续可扩展）
            UserMessage user = new UserMessage("用户问题：\n" + cleaned + "\n\n请输出重写后的检索 Query：");

            Prompt prompt = Prompt.builder()
                    .messages(List.of(system, user))
                    .build();

            String rewritten = chatClient
                    .prompt(prompt)
                    .call()
                    .content();

            // 若模型输出异常，回退原文
            if (!StringUtils.hasText(rewritten)) {
                return truncate(cleaned);
            }
            return truncate(cleanup(rewritten));
        } catch (Exception e) {
            log.warn("Query rewrite failed, fallback to original. model={}, userQuery={}", model, abbreviate(cleaned), e);
            return truncate(cleaned);
        }
    }

    /**
     * 基础清洗：去首尾空白、压缩空白、去掉首尾引号等。
     */
    private String cleanup(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String t = text.trim();
        // 把多空白压成单空格，避免 prompt 里出现很“脏”的输入
        t = t.replaceAll("\\s+", " ");
        // 去掉常见的首尾引号（模型有时会输出 "xxx" 或 ‘xxx’）
        if (t.length() >= 2) {
            char first = t.charAt(0);
            char last = t.charAt(t.length() - 1);
            if ((first == '"' && last == '"')
                    || (first == '“' && last == '”')
                    || (first == '\'' && last == '\'')
                    || (first == '‘' && last == '’')) {
                t = t.substring(1, t.length() - 1).trim();
            }
        }
        // 如果模型输出了多行，取第一行（保证“单行 query”约束）
        int newlineIdx = t.indexOf('\n');
        if (newlineIdx >= 0) {
            t = t.substring(0, newlineIdx).trim();
        }
        return t;
    }

    /**
     * 截断输出，避免过长。
     */
    private String truncate(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String t = text.trim();
        if (maxChars > 0 && t.length() > maxChars) {
            return t.substring(0, maxChars).trim();
        }
        return t;
    }

    /**
     * 日志缩略，避免把用户全文打进日志。
     */
    private String abbreviate(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String t = text.trim();
        int limit = Math.min(60, t.length());
        return t.substring(0, limit);
    }
}

