package com.rosanova.iot.timer.timer.service;

import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.timer.Timer;

import java.util.List;

public interface TimerService {
    public Result insertTimer(String name, int time, int symphony);
    Result removeTimer(long id);
    List<Timer> getAllTimers(int start);
}
