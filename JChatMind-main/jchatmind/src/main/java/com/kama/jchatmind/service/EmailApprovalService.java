package com.kama.jchatmind.service;

import com.kama.jchatmind.exception.BizException;
import com.kama.jchatmind.mapper.EmailApprovalMapper;
import com.kama.jchatmind.model.entity.EmailApprovalRecord;
import com.kama.jchatmind.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.Instant;
import java.util.UUID;

/** Durable drafts; the database claims each send once, independently of the SMTP operation. */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class EmailApprovalService {
    public static final String TYPE = "EMAIL_APPROVAL_REQUIRED";
    private final EmailService emailService;
    private final EmailApprovalMapper mapper;

    public enum Status { PENDING, SENDING, SENT, CANCELLED, EXPIRED, FAILED }
    public record EmailApproval(String id, String to, String subject, String content,
                                Instant expiresAt, Status status) {}

    public EmailApproval create(String to, String subject, String content) {
        String userId = UserContext.requireUserId();
        if (!StringUtils.hasText(to) || !to.contains("@") || !StringUtils.hasText(subject)
                || !StringUtils.hasText(content)) {
            throw new BizException("请提供有效的收件人、主题和正文");
        }
        EmailApprovalRecord record = new EmailApprovalRecord();
        record.setId(UUID.randomUUID().toString());
        record.setUserId(userId);
        record.setTo(to.trim());
        record.setSubject(subject.trim());
        record.setContent(content.trim());
        if (mapper.insert(record) != 1) {
            throw new BizException("保存邮件确认请求失败");
        }
        return get(record.getId());
    }

    public EmailApproval get(String id) {
        return snapshot(requireOwned(id));
    }

    public EmailApproval confirm(String id) {
        EmailApprovalRecord draft = requireOwned(id);
        // Conditional UPDATE uses database time and commits before SMTP, even across instances.
        if (mapper.claimPending(id, draft.getUserId()) == 1) {
            Status result;
            try {
                emailService.sendEmailSync(draft.getUserId(), draft.getTo(), draft.getSubject(), draft.getContent());
                result = Status.SENT;
            } catch (Exception e) {
                // SMTP may already have accepted the email. Never automatically retry.
                result = Status.FAILED;
            }
            // Failed result writes leave SENDING; they must never make the email sendable again.
            if (mapper.finishSending(id, draft.getUserId(), result.name()) != 1) {
                throw new BizException("邮件发送结果未能保存，请核对发件箱，勿重复发起");
            }
        }
        return get(id);
    }

    public EmailApproval cancel(String id) {
        EmailApprovalRecord draft = requireOwned(id);
        mapper.cancelPending(id, draft.getUserId());
        return get(id);
    }

    private EmailApprovalRecord requireOwned(String id) {
        String userId = UserContext.requireUserId();
        try {
            UUID.fromString(id);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BizException("确认请求不存在或无权访问");
        }
        mapper.expirePending(id, userId);
        EmailApprovalRecord record = mapper.selectOwned(id, userId);
        if (record == null) {
            throw new BizException("确认请求不存在或无权访问");
        }
        return record;
    }

    private EmailApproval snapshot(EmailApprovalRecord record) {
        return new EmailApproval(record.getId(), record.getTo(), record.getSubject(), record.getContent(),
                record.getExpiresAt(), Status.valueOf(record.getStatus()));
    }
}
