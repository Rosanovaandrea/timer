package com.rosanova.iot.timer.user.service.impl;

import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.error.UserServiceException;
import com.rosanova.iot.timer.user.User;
import com.rosanova.iot.timer.user.dto.LoginReturnDto;
import com.rosanova.iot.timer.user.repository.UserRepository;
import com.rosanova.iot.timer.user.repository.impl.UserRepositoryImpl;
import com.rosanova.iot.timer.user.service.UserService;
import com.rosanova.iot.timer.utils.HMACSHA256SignatureUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final HMACSHA256SignatureUtil hashToken;

    @Override
    @Transactional
    public LoginReturnDto login(String username, String password) {

            LoginReturnDto loginReturnDto = new LoginReturnDto();

            User login = repository.getByUsername(username);

            if (login == null) {
                loginReturnDto.setResult(Result.BAD_REQUEST);
                return loginReturnDto;
            }

            if (!checkPassword(password, login.getPassword())) {
                loginReturnDto.setResult(Result.BAD_REQUEST);
                return loginReturnDto;
            }

            String time = String.valueOf(System.currentTimeMillis());
            StringBuilder timeToken = new StringBuilder(13);
            int padding = 13 - time.length();
            for (int i = 0; i < padding; i++) {
                timeToken.append('0');
            }
            timeToken.append(time);

            loginReturnDto.setResult(Result.SUCCESS);
            loginReturnDto.setToken(hashToken.computeHMACSHA256(timeToken.toString()));

            return loginReturnDto;


    }

    @Override
    @Transactional
    public Result changePassword(Long id, String oldPassword, String newPassword) {

        User user = repository.findById(id);

        if (user == null) {
            return Result.ERROR;
        }

        if (!checkPassword(oldPassword, user.getPassword())) {
            return Result.ERROR;
        }

        repository.updateUser(id,hashPassword(newPassword));
        return Result.SUCCESS;

    }

    public String hashPassword(String password){
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public boolean checkPassword(String password, String hashPassword){
        return BCrypt.checkpw(password,hashPassword);
    }
}
