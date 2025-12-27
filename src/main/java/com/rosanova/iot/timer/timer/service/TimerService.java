package com.rosanova.iot.timer.timer.service;

import com.rosanova.iot.timer.Result;
import com.rosanova.iot.timer.timer.Timer;

public interface TimerService {
    Result insertTimer(String name, int start, int end);
    Result removeTimer(int id);
    Timer getNextTimer(int start);
}
