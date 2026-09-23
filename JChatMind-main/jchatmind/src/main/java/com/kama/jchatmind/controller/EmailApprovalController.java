package com.kama.jchatmind.controller;

import com.kama.jchatmind.model.common.ApiResponse;
import com.kama.jchatmind.service.EmailApprovalService;
import com.kama.jchatmind.service.EmailApprovalService.EmailApproval;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Confirmation is an authenticated user action, deliberately not exposed as an Agent tool. */
@RestController
@RequestMapping("/api/email-approvals")
@RequiredArgsConstructor
public class EmailApprovalController {
    private final EmailApprovalService emailApprovalService;

    @GetMapping("/{id}")
    public ApiResponse<EmailApproval> get(@PathVariable String id) {
        return ApiResponse.success(emailApprovalService.get(id));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<EmailApproval> confirm(@PathVariable String id) {
        return ApiResponse.success(emailApprovalService.confirm(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<EmailApproval> cancel(@PathVariable String id) {
        return ApiResponse.success(emailApprovalService.cancel(id));
    }
}
