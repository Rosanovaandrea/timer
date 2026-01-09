package com.rosanova.iot.timer.monitor.service;

import com.rosanova.iot.timer.monitor.repository.MonitorRepository;
import com.rosanova.iot.timer.utils.TimerUtils;
import lombok.RequiredArgsConstructor;


public class MonitorServiceImpl {

    private final MonitorRepository repository;
    private final TimerUtils monitorTurnOnUtils;
    private final TimerUtils monitorTurnOffUtils;

    public MonitorServiceImpl(MonitorRepository repository, TimerUtils monitorTurnOnUtils, TimerUtils monitorTurnOffUtils) {
        this.repository = repository;
        this.monitorTurnOnUtils = monitorTurnOnUtils;
        this.monitorTurnOffUtils = monitorTurnOffUtils;
    }




}
