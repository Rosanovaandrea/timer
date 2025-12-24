package com.rosanova.iot.timer.timer.service;

public interface timerService {
    void insertTimer(int start, int end);
    void removeTimer(int id);
    void getNextTimer(int start);
}
