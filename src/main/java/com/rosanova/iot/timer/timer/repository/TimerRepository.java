package com.rosanova.iot.timer.timer.repository;

import com.rosanova.iot.timer.timer.Timer;
import com.rosanova.iot.timer.timer.dto.CheckTimerInsertValidity;
import com.rosanova.iot.timer.utils.impl.HashMapInt;

import java.util.List;

public interface TimerRepository {
    Timer findById(long id);
    CheckTimerInsertValidity countOverlapsAndMaxTimers(int startToCheck, int endToCheck);
    void addEndTimesToMap(HashMapInt targetMap);
    void insert(Timer timer);
    void deleteById(long id);
    List<Timer> findAll();
}
