package com.kama.jchatmind.controller;

import com.kama.jchatmind.model.common.ApiResponse;
import com.kama.jchatmind.model.request.SaveUserMailConfigRequest;
import com.kama.jchatmind.model.response.GetUserMailConfigResponse;
import com.kama.jchatmind.service.UserMailConfigFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户个人发件邮箱配置 API
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserMailConfigController {

    private final UserMailConfigFacadeService userMailConfigFacadeService;

    /** 获取当前用户的邮箱配置（不含授权码明文） */
    @GetMapping("/user-mail-config")
    public ApiResponse<GetUserMailConfigResponse> getMyConfig() {
        return ApiResponse.success(userMailConfigFacadeService.getMyConfig());
    }

    /** 保存或更新当前用户的邮箱配置 */
    @PutMapping("/user-mail-config")
    public ApiResponse<Void> saveMyConfig(@RequestBody SaveUserMailConfigRequest request) {
        userMailConfigFacadeService.saveMyConfig(request);
        return ApiResponse.success();
    }

    /** 向自己的发件邮箱发送一封测试邮件 */
    @PostMapping("/user-mail-config/test")
    public ApiResponse<Void> sendTestMail() {
        userMailConfigFacadeService.sendTestMail();
        return ApiResponse.success(null, "测试邮件已发送，请查收收件箱");
    }
}
