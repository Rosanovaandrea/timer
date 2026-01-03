package com.rosanova.iot.timer.error;

public class UserServiceException extends RuntimeException {
    public UserServiceException(String message) {
        super(message, null, false, false);;
    }
}
