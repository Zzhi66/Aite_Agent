package com.kama.jchatmind.service;

import com.kama.jchatmind.exception.BizException;
import com.kama.jchatmind.security.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import com.kama.jchatmind.mapper.EmailApprovalMapper;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.kama.jchatmind.service.EmailApprovalService.Status.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailApprovalServiceTest {
    private final EmailService sender = mock(EmailService.class);
    private static final String OWNER = "00000000-0000-0000-0000-000000000001";
    private static final String OTHER = "00000000-0000-0000-0000-000000000002";
    private EmailApprovalService service;
    private EmailApprovalMapper mapper;
    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbc;
    private DataSourceTransactionManager transactions;

    @BeforeEach
    void setup() throws Exception {
        dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE app_user (id UUID PRIMARY KEY)");
        jdbc.update("INSERT INTO app_user(id) VALUES (CAST(? AS uuid)), (CAST(? AS uuid))", OWNER, OTHER);
        try (var connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("email-approval-ddl.sql"));
        }
        transactions = new DataSourceTransactionManager(dataSource);
        mapper = newMapper();
        service = newService(mapper);
        UserContext.setUserId(OWNER);
    }

    private EmailApprovalMapper newMapper() throws Exception {
        var factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(new ClassPathResource("mapper/EmailApprovalMapper.xml"));
        return new SqlSessionTemplate(factory.getObject()).getMapper(EmailApprovalMapper.class);
    }

    private EmailApprovalService newService(EmailApprovalMapper repository) {
        var proxy = new ProxyFactory(new EmailApprovalService(sender, repository));
        proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
        return (EmailApprovalService) proxy.getProxy();
    }

    @AfterEach
    void cleanup() {
        UserContext.clear();
        if (jdbc != null) jdbc.execute("SHUTDOWN");
    }

    private String draft() {
        return service.create(" receiver@example.com ", " subject ", " body ").id();
    }

    @Test
    void preparingAndReadingNeverSends() {
        var request = service.get(draft());
        assertEquals(PENDING, request.status());
        assertEquals("receiver@example.com", request.to());
        assertTrue(request.expiresAt().isAfter(Instant.now().plusSeconds(590)));
        assertTrue(request.expiresAt().isBefore(Instant.now().plusSeconds(610)));
        verifyNoInteractions(sender);
    }

    @Test
    void confirmationSendsStoredDraftOnlyOnce() {
        String id = draft();
        assertEquals(SENT, service.confirm(id).status());
        assertEquals(SENT, service.confirm(id).status());
        assertEquals(SENT, service.cancel(id).status());
        verify(sender, times(1)).sendEmailSync(OWNER, "receiver@example.com", "subject", "body");
        verifyNoMoreInteractions(sender);
    }

    @Test
    void otherUserCannotReadConfirmOrCancel() {
        String id = draft();
        UserContext.setUserId(OTHER);
        assertThrows(BizException.class, () -> service.get(id));
        assertThrows(BizException.class, () -> service.confirm(id));
        assertThrows(BizException.class, () -> service.cancel(id));
        UserContext.setUserId(OWNER);
        assertEquals(PENDING, service.get(id).status());
        verifyNoInteractions(sender);
    }

    @Test
    void cancellationPreventsSending() {
        String id = draft();
        assertEquals(CANCELLED, service.cancel(id).status());
        assertEquals(CANCELLED, service.confirm(id).status());
        verifyNoInteractions(sender);
    }

    @Test
    void expiresAtDeadlineAndCannotBeConfirmed() {
        String id = draft();
        jdbc.update("UPDATE email_approval SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1' SECOND WHERE id = CAST(? AS uuid)", id);
        assertEquals(EXPIRED, service.confirm(id).status());
        verifyNoInteractions(sender);
    }

    @Test
    void failureIsNotRetried() {
        String id = draft();
        doThrow(new RuntimeException("SMTP outcome unknown")).when(sender)
                .sendEmailSync(anyString(), anyString(), anyString(), anyString());
        assertEquals(FAILED, service.confirm(id).status());
        assertEquals(FAILED, service.confirm(id).status());
        verify(sender, times(1)).sendEmailSync(OWNER, "receiver@example.com", "subject", "body");
    }

    @Test
    void concurrentConfirmationWhileSendingDoesNotSendAgain() throws Exception {
        String id = draft();
        var anotherInstance = newService(newMapper());
        CountDownLatch sending = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        doAnswer(invocation -> {
            sending.countDown();
            assertTrue(finish.await(5, TimeUnit.SECONDS));
            return null;
        }).when(sender).sendEmailSync(anyString(), anyString(), anyString(), anyString());
        var executor = Executors.newSingleThreadExecutor();
        try {
            var first = executor.submit(() -> {
                UserContext.setUserId(OWNER);
                try { return service.confirm(id); }
                finally { UserContext.clear(); }
            });
            assertTrue(sending.await(5, TimeUnit.SECONDS));
            assertEquals(SENDING, anotherInstance.confirm(id).status());
            assertEquals(SENDING, service.cancel(id).status());
            finish.countDown();
            assertEquals(SENT, first.get(5, TimeUnit.SECONDS).status());
            verify(sender, times(1)).sendEmailSync(OWNER, "receiver@example.com", "subject", "body");
        } finally {
            finish.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void missingOrUnauthenticatedRequestsNeverSend() {
        assertThrows(BizException.class, () -> service.confirm("missing"));
        String id = draft();
        UserContext.clear();
        assertThrows(IllegalStateException.class, () -> service.confirm(id));
        assertThrows(IllegalStateException.class, this::draft);
        verifyNoInteractions(sender);
    }

    @Test
    void freshServiceAndConnectionsRestorePendingAndCompletedRecords() throws Exception {
        String id = draft();
        var restarted = newService(newMapper());
        assertEquals(PENDING, restarted.get(id).status());
        assertEquals("body", restarted.get(id).content());
        assertEquals(SENT, restarted.confirm(id).status());
        assertEquals(SENT, newService(newMapper()).confirm(id).status());
        verify(sender, times(1)).sendEmailSync(OWNER, "receiver@example.com", "subject", "body");
    }

    @Test
    void outerTransactionRollbackCannotUndoTheSendClaim() {
        String id = draft();
        new TransactionTemplate(transactions).executeWithoutResult(transaction -> {
            assertEquals(SENT, service.confirm(id).status());
            transaction.setRollbackOnly();
        });
        assertEquals(SENT, service.confirm(id).status());
        verify(sender, times(1)).sendEmailSync(OWNER, "receiver@example.com", "subject", "body");
    }

    @Test
    void lostResultWriteKeepsDurableClaimAndPreventsRetry() {
        String id = draft();
        var failingMapper = spy(mapper);
        doThrow(new RuntimeException("database unavailable"))
                .when(failingMapper).finishSending(id, OWNER, "SENT");
        assertThrows(RuntimeException.class, () -> newService(failingMapper).confirm(id));
        assertEquals(SENDING, service.get(id).status());
        assertEquals(SENDING, service.confirm(id).status());
        verify(sender, times(1)).sendEmailSync(OWNER, "receiver@example.com", "subject", "body");
    }

    @Test
    void migrationCanBeAppliedAgainWithoutLosingHistory() throws Exception {
        String id = draft();
        service.cancel(id);
        try (var connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new FileSystemResource("email-approval-ddl.sql"));
        }
        assertEquals(CANCELLED, service.get(id).status());
        verifyNoInteractions(sender);
    }
}
