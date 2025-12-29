package com.rosanova.iot.timer.timer.service.impl;

import com.rosanova.iot.timer.Result;
import com.rosanova.iot.timer.timer.Timer;
import com.rosanova.iot.timer.timer.repository.TimerRepository;
import com.rosanova.iot.timer.timer.service.TimerService;
import com.rosanova.iot.timer.utils.TimerUtils;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class TimerServiceImpl implements TimerService {

    private final TimerRepository repository;
    private final TimerUtils timerUtils;

    @Override
    public Result insertTimer(String name, int start, int end) {
        return null;
    }

    @Override
    public Result removeTimer(int id) {
        return null;
    }

    @Override
    public List<Timer> getAllTimers(int start) {
        return List.of();
    }
}
