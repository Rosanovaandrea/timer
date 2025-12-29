package com.rosanova.iot.timer.error;

public class TimerServiceException extends RuntimeException {
    public TimerServiceException(String message) {
        super(message, null, false, false);
    }
}
