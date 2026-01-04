package com.rosanova.iot.timer.user.service.impl;

import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.user.repository.UserRepository;
import com.rosanova.iot.timer.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCrypt;

@RequiredArgsConstructor
public class UserServiceImpl implements UserService {


    private final UserRepository repository;



    @Override
    public Result login(String username, String password) {

    }

    public String hashPassword(String password){
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public boolean checkPassword(String password, String hashPassword){
        return BCrypt.checkpw(password,hashPassword);
    }
}
