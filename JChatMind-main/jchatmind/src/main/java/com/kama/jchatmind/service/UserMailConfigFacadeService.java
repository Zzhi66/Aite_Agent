package com.kama.jchatmind.service;

import com.kama.jchatmind.model.request.SaveUserMailConfigRequest;
import com.kama.jchatmind.model.response.GetUserMailConfigResponse;

public interface UserMailConfigFacadeService {

    GetUserMailConfigResponse getMyConfig();

    void saveMyConfig(SaveUserMailConfigRequest request);

    void sendTestMail();
}
