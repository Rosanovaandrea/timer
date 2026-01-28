package com.rosanova.iot.timer.monitor.service;

import com.rosanova.iot.timer.error.MonitorServiceException;
import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.monitor.Monitor;
import com.rosanova.iot.timer.monitor.repository.MonitorRepository;
import com.rosanova.iot.timer.utils.TimerUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class MonitorServiceImpl {

    private final MonitorRepository repository;
    private final TimerUtils monitorTurnOnUtils;
    private final TimerUtils monitorTurnOffUtils;
    private final ReentrantLock sharedLock;

    public MonitorServiceImpl(ReentrantLock sharedLock, MonitorRepository repository, @Qualifier("monitorOn") TimerUtils monitorTurnOnUtils, @Qualifier("monitorShutdown") TimerUtils monitorTurnOffUtils) {
        this.repository = repository;
        this.monitorTurnOnUtils = monitorTurnOnUtils;
        this.monitorTurnOffUtils = monitorTurnOffUtils;
        this.sharedLock = sharedLock;
    }

    public Result updateMonitorStartSynchronized( int start ){

        boolean resultLock = false;
        try {
            resultLock = sharedLock.tryLock(100L, TimeUnit.MILLISECONDS);
            if(!resultLock) return Result.ERROR;
            updateMonitorStart(start);
            return Result.SUCCESS;
        }catch (Exception e){
            return Result.ERROR;
        } finally {
            if(resultLock) sharedLock.unlock();
        }

    }

    public Result updateMonitorStopSynchronized( int stop ){

        boolean resultLock = false;

        try {
            resultLock = sharedLock.tryLock(100L, TimeUnit.MILLISECONDS);
            if( ! resultLock ) return Result.ERROR;
            updateMonitorStop(stop);
            return Result.SUCCESS;
        }catch (Exception e){
            return Result.ERROR;
        } finally {
            if ( resultLock ) sharedLock.unlock();
        }

    }

    public Result updateMonitorStart(int start) {

        int step = 0;

        Monitor monitor;

        try{
         monitor = repository.getMonitor();
        }catch (Exception e) {
            System.err.println("ERRORE DATABASE UPDATE MONITOR:" + e.getMessage());
            throw new MonitorServiceException("ERRORE DATABASE UPDATE MONITOR");
        }

        if (monitor == null) throw new MonitorServiceException("Monitor è nullo");

        if (start >= monitor.getStop()) throw new MonitorServiceException(" valore di start non valido");

        if (start == monitor.getStart()) return Result.SUCCESS;

        String prevStart = String.valueOf(monitor.getStart());
        String nowStart = String.valueOf(start);

        Duration duration = Duration.ofMillis(start);

        String startDate = String.format("%02d:%02d:%02d",
                duration.toHours(),
                duration.toMinutesPart(),
                duration.toSecondsPart());

        try {



            step++;

            if (monitorTurnOnUtils.deactivateSystemdTimer(prevStart) == Result.ERROR)
                throw new MonitorServiceException("errore nella disattivazione del vecchio timer di accenzione");


            step++;

            if (monitorTurnOnUtils.deleteSystemdTimerUnit(prevStart) == Result.ERROR)
                throw new MonitorServiceException("errore nella cancellazione del vecchio timer di accenzione");


            step++;

            if (monitorTurnOnUtils.createSystemdTimerUnit(nowStart, startDate, nowStart.substring(0,nowStart.length()-3)) == Result.ERROR)
                throw new MonitorServiceException("errore nella creazione del nuovo timer di accenzione");

            step++;

            if (monitorTurnOnUtils.timerReload() == Result.ERROR)
                throw new MonitorServiceException("errore nel system reload");

            step++;

            if (monitorTurnOnUtils.activateSystemdTimer(nowStart) == Result.ERROR)
                throw new MonitorServiceException("errore nell' attivazione del nuovo timer");

            step++;

            repository.updateStart(monitor.getId(), start);

            step++;


            return Result.SUCCESS;

        } catch (Exception e) {
            System.err.println(e.getMessage());
            throw new MonitorServiceException("errore nel servizio di inserimento start monitor");
        } finally {
            if (step < 7) {
                try {

                    if (step >= 5 && monitorTurnOnUtils.deactivateSystemdTimer(nowStart) == Result.ERROR)
                        throw new MonitorServiceException("disattivazione nuovo timer");

                    if (step >= 3 && monitorTurnOnUtils.reversSystemdTimerUnitInsert(nowStart) == Result.ERROR)
                        throw new MonitorServiceException("cancellazione nuovo timer");

                    if (step >= 2 && monitorTurnOnUtils.reverseDeleteSystemdTimerUnit(prevStart) == Result.ERROR)
                        throw new MonitorServiceException("errore ripristino vecchio timer");

                    if (step >= 1 && monitorTurnOnUtils.activateSystemdTimer(prevStart) == Result.ERROR)
                        throw new MonitorServiceException("errore riattivazione vecchio timer");

                    if(monitorTurnOnUtils.timerReload() == Result.ERROR)
                        throw new MonitorServiceException("errore rimer reload");

                } catch (Exception rollbackError) {
                    System.err.println("ERRORE CRITICO: Fallimento durante il rollback: " + rollbackError.getMessage());
                }

            }

        }
    }

    public Monitor getMonitor(){
        return repository.getMonitor();
    }

    public Result updateMonitorStop(int stop) {

        int step = 0;

        Monitor monitor;


        try{
            monitor = repository.getMonitor();
        }catch (Exception e) {
            System.err.println("ERRORE DATABASE UPDATE MONITOR:" + e.getMessage());
            throw new MonitorServiceException("ERRORE DATABASE UPDATE MONITOR");
        }

        if (monitor == null) throw new MonitorServiceException("Monitor è null");

        if (stop <= monitor.getStart()) throw new MonitorServiceException("valore di stop non valido");

        if (stop == monitor.getStop()) return Result.SUCCESS;

        String prevStop = String.valueOf(monitor.getStop());
        String nowStop = String.valueOf(stop);

        Duration duration = Duration.ofMillis(stop);

        String stopDate = String.format("%02d:%02d:%02d",
                duration.toHours(),
                duration.toMinutesPart(),
                duration.toSecondsPart());

        try {



            step++;

            if (monitorTurnOffUtils.deactivateSystemdTimer(prevStop) == Result.ERROR)
                throw new MonitorServiceException("error");


            step++;

            if (monitorTurnOffUtils.deleteSystemdTimerUnit(prevStop) == Result.ERROR)
                throw new MonitorServiceException("error");


            step++;

            if (monitorTurnOffUtils.createSystemdTimerUnit(nowStop, stopDate,nowStop.substring(0,nowStop.length()-3)) == Result.ERROR)
                throw new MonitorServiceException("error");

            step++;

            if (monitorTurnOffUtils.timerReload() == Result.ERROR)
                throw new MonitorServiceException("error");

            step++;

            if (monitorTurnOffUtils.activateSystemdTimer(nowStop) == Result.ERROR)
                throw new MonitorServiceException("error");

            step++;

            repository.updateStop(monitor.getId(), stop);

            step++;


            return Result.SUCCESS;

        } catch (Exception e) {
            System.err.println(e.getMessage());
            throw new MonitorServiceException("errore nel servizio di inserimento stop monitor");
        } finally {
            if (step < 7) {
                try {

                    if (step >= 5 && monitorTurnOffUtils.deactivateSystemdTimer(nowStop) == Result.ERROR)
                        throw new MonitorServiceException("error");

                    if (step >= 3 && monitorTurnOffUtils.reversSystemdTimerUnitInsert(nowStop) == Result.ERROR)
                        throw new MonitorServiceException("error");

                    if (step >= 2 && monitorTurnOffUtils.reverseDeleteSystemdTimerUnit(prevStop) == Result.ERROR)
                        throw new MonitorServiceException("error");

                    if (step >= 1 && monitorTurnOffUtils.activateSystemdTimer(prevStop) == Result.ERROR)
                        throw new MonitorServiceException("error");

                    if(monitorTurnOffUtils.timerReload() == Result.ERROR)
                        throw new MonitorServiceException("error");

                } catch (Exception rollbackError) {
                    System.err.println("ERRORE CRITICO: Fallimento durante il rollback: " + rollbackError.getMessage());
                }

            }

        }
    }
}
