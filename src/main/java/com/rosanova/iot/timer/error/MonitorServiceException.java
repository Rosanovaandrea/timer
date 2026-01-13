package com.rosanova.iot.timer.error;

public class MonitorServiceException extends RuntimeException {
    public MonitorServiceException(String message) {
        super(message, null, false, false);;
    }
}
