package com.rosanova.iot.timer.user.repository;

import com.rosanova.iot.timer.user.User;

import java.util.List;

public interface UserRepository {
    void insertUser(User user);
    void deleteUserById(long id);
    User getByUsername(String username);
    List<User> findAllUsers();
    User findById(long id);
}
