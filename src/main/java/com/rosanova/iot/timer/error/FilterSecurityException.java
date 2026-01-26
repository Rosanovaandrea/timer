package com.rosanova.iot.timer.error;

public class FilterSecurityException extends RuntimeException {
    public FilterSecurityException(String message) {
        super(message, null, false, false);;
    }
}
