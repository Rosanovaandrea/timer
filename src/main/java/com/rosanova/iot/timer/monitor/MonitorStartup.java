package com.rosanova.iot.timer.monitor;


import com.rosanova.iot.timer.error.MonitorServiceException;
import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.error.UserServiceException;
import com.rosanova.iot.timer.monitor.repository.MonitorRepository;
import com.rosanova.iot.timer.utils.TimerUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.beans.Transient;
import java.time.Duration;

@Component
public class MonitorStartup {
    private final MonitorRepository repository;
    private final TimerUtils monitorTurnOnUtils;
    private final TimerUtils monitorTurnOffUtils;
    private static final int START = 8 * 60 * 60 * 1000; //08:00:00
    private static final int STOP = 20 * 60 * 60 * 1000; //20:00:00

    public MonitorStartup(MonitorRepository repository, TimerUtils monitorTurnOnUtils, TimerUtils monitorTurnOffUtils) {
        this.repository = repository;
        this.monitorTurnOnUtils = monitorTurnOnUtils;
        this.monitorTurnOffUtils = monitorTurnOffUtils;
    }

    @PostConstruct
    @Transactional(rollbackFor = Exception.class)
    public Result createMonitor() {

        int step = 0;
        Monitor monitor;

        try {
             monitor = repository.getMonitor();
        }catch (RuntimeException e) {
            System.err.println("ERRORE DATABASE CONTROLLO ESISTENZA MONITOR:" + e.getMessage());
            throw new MonitorServiceException("errore startup Monitor");
        }


        if (monitor != null) return Result.SUCCESS;

        monitor = new Monitor(0,START,STOP);


        String stopString = String.valueOf(STOP);
        String startString = String.valueOf(START);

        Duration durationStart = Duration.ofMillis(START);

        String startDate = String.format("%02d:%02d:%02d",
                durationStart.toHours(),
                durationStart.toMinutesPart(),
                durationStart.toSecondsPart());

        Duration durationStop = Duration.ofMillis(STOP);

        String stopDate = String.format("%02d:%02d:%02d",
                durationStop.toHours(),
                durationStop.toMinutesPart(),
                durationStop.toSecondsPart());

        try {



            step++;

            if (monitorTurnOnUtils.createSystemdTimerUnit(startString,startDate) == Result.ERROR)
                throw new MonitorServiceException("errore creazione timer start");


            step++;

            if (monitorTurnOffUtils.createSystemdTimerUnit(stopString,stopDate) == Result.ERROR)
                throw new MonitorServiceException("errore creazione timer stop");


            step++;

            if (monitorTurnOnUtils.timerReload() == Result.ERROR)
                throw new MonitorServiceException("errore daemon reload");

            step++;

            if (monitorTurnOnUtils.activateSystemdTimer(startString) == Result.ERROR)
                throw new MonitorServiceException("errore attivazione timer start");

            step++;

            if (monitorTurnOffUtils.activateSystemdTimer(stopString) == Result.ERROR)
                throw new MonitorServiceException("errore attivazione timer stop");

            step++;

            repository.save(monitor);

            step++;


            return Result.SUCCESS;

        } catch (Exception e) {
            System.err.println("ERRORE INIZIALIZZAZIONE MONITOR: "+e.getMessage());
            throw new MonitorServiceException("errore startup Monitor");
        } finally {
            if (step < 7) {
                try {

                    if (step >= 5 && monitorTurnOffUtils.deactivateSystemdTimer(stopString)== Result.ERROR)
                        throw new MonitorServiceException("errore disattivazione timer stop");

                    if (step >= 4 && monitorTurnOnUtils.deactivateSystemdTimer(startString) == Result.ERROR)
                        throw new MonitorServiceException("errore disattivazione timer start");

                    if (step >= 2 && monitorTurnOffUtils.reversSystemdTimerUnitInsert(stopString) == Result.ERROR)
                        throw new MonitorServiceException("errore cancellazione timer stop");

                    if (step >= 1 && monitorTurnOnUtils.reversSystemdTimerUnitInsert(startString) == Result.ERROR)
                        throw new MonitorServiceException("error cancellazione timer start");

                    if(monitorTurnOnUtils.timerReload() == Result.ERROR)
                        throw new MonitorServiceException("error daemon reaload");

                } catch (Exception rollbackError) {
                    System.err.println("ERRORE CRITICO: Fallimento durante il rollback: " + rollbackError.getMessage());
                }

            }

        }
    }
}
