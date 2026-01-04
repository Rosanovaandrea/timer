package com.rosanova.iot.timer.user.service;

import com.rosanova.iot.timer.error.Result;

public interface UserService {
    Result login(String username,String password);
}
