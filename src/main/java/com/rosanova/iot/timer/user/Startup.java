package com.rosanova.iot.timer.user;

import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.error.UserServiceException;
import com.rosanova.iot.timer.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.UnknownServiceException;

@Component
@Profile("!test")
public class Startup {
    private static final String USERNAME = "root";
    private static final String RESET = "RESET";
    private String password;
    private String reset;
    private UserRepository repository;

    public Startup(@Value("${user.password}") String password, @Value("${user.reset}") String reset, @Autowired UserRepository repository){
            this.password = password;
            this.reset = reset;
            this.repository = repository;
    };

    @PostConstruct
    @Transactional
    public void init(){
        int isPresentAdmin = repository.findNumberOfUsers();

        Result result = Result.SUCCESS;

        try {

            if (isPresentAdmin == 0) {
                result = initUserRoot(password);
            } else if (isPresentAdmin > 0 && reset.equals(RESET)) {
                result = initUserRoot(password);
            }

            if(result == Result.ERROR) throw new UserServiceException("error");

        }catch (Exception e){
            System.out.println("errore nell' inizializzazione dell'user root");
        }


    }

    public Result initUserRoot(String password){

        if(password.length() < 8) return Result.ERROR;

        repository.deleteAllUsers();

        User root = new User();
        root.setUsername(USERNAME);
        root.setPassword(hashPassword(password));

        repository.insertUser(root);

        return Result.SUCCESS;
    }

    public String hashPassword(String password){
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

}
