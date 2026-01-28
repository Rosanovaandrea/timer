package com.rosanova.iot.timer.timer.service;

import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.timer.Timer;

import java.util.List;

public interface TimerService {
    List<Timer> getAllTimers(int start);
    Result removeTimerSynchronized(long id);
    Result insertTimerSynchronized(String name, int time, int symphony);
}
