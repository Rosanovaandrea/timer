package com.rosanova.iot.timer.monitor.service;

import com.rosanova.iot.timer.error.MonitorServiceException;
import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.monitor.Monitor;
import com.rosanova.iot.timer.monitor.repository.MonitorRepository;
import com.rosanova.iot.timer.utils.TimerUtils;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;


public class MonitorServiceImpl {

    private final MonitorRepository repository;
    private final TimerUtils monitorTurnOnUtils;
    private final TimerUtils monitorTurnOffUtils;

    public MonitorServiceImpl(MonitorRepository repository, TimerUtils monitorTurnOnUtils, TimerUtils monitorTurnOffUtils) {
        this.repository = repository;
        this.monitorTurnOnUtils = monitorTurnOnUtils;
        this.monitorTurnOffUtils = monitorTurnOffUtils;
    }

    public Result updateMonitorStart(int start) {

        int step = 0;


        Monitor monitor = repository.getMonitor();

        if (monitor == null) throw new MonitorServiceException("Monitor is null");

        if (start >= monitor.getStop()) throw new MonitorServiceException("Invalid start value");

        if (start == monitor.getStart()) return Result.SUCCESS;

        String prevStart = String.valueOf(monitor.getStart());
        String nowStart = String.valueOf(start);

        Duration duration = Duration.ofMillis(start);

        String startDate = String.format("%02d:%02d:%02d",
                duration.toHours(),
                duration.toMinutesPart(),
                duration.toSecondsPart());

        try {

            repository.updateStart(monitor.getId(), start);

            step++;

            if (monitorTurnOnUtils.deactivateSystemdTimer(prevStart) == Result.ERROR)
                throw new MonitorServiceException("error");


            step++;

            if (monitorTurnOnUtils.deleteSystemdTimerUnit(prevStart) == Result.ERROR)
                throw new MonitorServiceException("error");


            step++;

            if (monitorTurnOnUtils.createSystemdTimerUnit(nowStart, startDate) == Result.ERROR)
                throw new MonitorServiceException("error");

            step++;

            if (monitorTurnOnUtils.timerReload() == Result.ERROR)
                throw new MonitorServiceException("error");

            step++;

            if (monitorTurnOnUtils.activateSystemdTimer(nowStart) == Result.ERROR)
                throw new MonitorServiceException("error");

            step++;


            return Result.SUCCESS;

        } catch (Exception e) {
            System.err.println(e.getMessage());
            throw e;
        } finally {
            if (step < 6) {
                try {

                    if (step >= 5 && monitorTurnOnUtils.deactivateSystemdTimer(nowStart) == Result.ERROR)
                        throw new MonitorServiceException("error");

                    if (step >= 3 && monitorTurnOnUtils.reversSystemdTimerUnitInsert(nowStart) == Result.ERROR)
                        throw new MonitorServiceException("error");

                    if (step >= 2 && monitorTurnOnUtils.reverseDeleteSystemdTimerUnit(prevStart) == Result.ERROR)
                        throw new MonitorServiceException("error");

                    if (step >= 1 && monitorTurnOnUtils.activateSystemdTimer(prevStart) == Result.ERROR)
                        throw new MonitorServiceException("error");

                    if(monitorTurnOnUtils.timerReload() == Result.ERROR)
                        throw new MonitorServiceException("error");

                } catch (Exception rollbackError) {
                    System.err.println("ERRORE CRITICO: Fallimento durante il rollback: " + rollbackError.getMessage());
                }

            }

        }
    }

    public Result updateMonitorStop(int stop) {

        int step = 0;


        Monitor monitor = repository.getMonitor();

        if (monitor == null) throw new MonitorServiceException("Monitor is null");

        if (stop <= monitor.getStart()) throw new MonitorServiceException("Invalid start value");

        if (stop == monitor.getStop()) return Result.SUCCESS;

        String prevStop = String.valueOf(monitor.getStop());
        String nowStop = String.valueOf(stop);

        Duration duration = Duration.ofMillis(stop);

        String stopDate = String.format("%02d:%02d:%02d",
                duration.toHours(),
                duration.toMinutesPart(),
                duration.toSecondsPart());

        try {

            repository.updateStart(monitor.getId(), stop);

            step++;

            if (monitorTurnOnUtils.deactivateSystemdTimer(prevStop) == Result.ERROR)
                throw new MonitorServiceException("error");


            step++;

            if (monitorTurnOnUtils.deleteSystemdTimerUnit(prevStop) == Result.ERROR)
                throw new MonitorServiceException("error");


            step++;

            if (monitorTurnOnUtils.createSystemdTimerUnit(nowStop, stopDate) == Result.ERROR)
                throw new MonitorServiceException("error");

            step++;

            if (monitorTurnOnUtils.timerReload() == Result.ERROR)
                throw new MonitorServiceException("error");

            step++;

            if (monitorTurnOnUtils.activateSystemdTimer(nowStop) == Result.ERROR)
                throw new MonitorServiceException("error");

            step++;


            return Result.SUCCESS;

        } catch (Exception e) {
            System.err.println(e.getMessage());
            throw e;
        } finally {
            if (step < 6) {
                try {

                    if (step >= 5 && monitorTurnOnUtils.deactivateSystemdTimer(nowStop) == Result.ERROR)
                        throw new MonitorServiceException("error");

                    if (step >= 3 && monitorTurnOnUtils.reversSystemdTimerUnitInsert(nowStop) == Result.ERROR)
                        throw new MonitorServiceException("error");

                    if (step >= 2 && monitorTurnOnUtils.reverseDeleteSystemdTimerUnit(prevStop) == Result.ERROR)
                        throw new MonitorServiceException("error");

                    if (step >= 1 && monitorTurnOnUtils.activateSystemdTimer(prevStop) == Result.ERROR)
                        throw new MonitorServiceException("error");

                    if(monitorTurnOnUtils.timerReload() == Result.ERROR)
                        throw new MonitorServiceException("error");

                } catch (Exception rollbackError) {
                    System.err.println("ERRORE CRITICO: Fallimento durante il rollback: " + rollbackError.getMessage());
                }

            }

        }
    }
}
