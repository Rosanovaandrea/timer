package com.rosanova.iot.timer.timer.service;

import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.timer.Timer;

import java.util.List;

public interface TimerService {
    Result insertTimer(String name, int start, int end);
    Result removeTimer(int id);
    List<Timer> getAllTimers(int start);
}
