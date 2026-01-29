package com.rosanova.iot.timer.user.service;

import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.user.dto.LoginReturnDto;

public interface UserService {
    LoginReturnDto login(String username, String password);
    Result changePassword( String oldPassword, String NewPassword);
}
