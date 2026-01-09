package com.rosanova.iot.timer.timer.service.impl;

import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.error.TimerServiceException;
import com.rosanova.iot.timer.timer.Timer;
import com.rosanova.iot.timer.timer.dto.CheckTimerInsertValidity;
import com.rosanova.iot.timer.timer.repository.TimerRepository;
import com.rosanova.iot.timer.timer.service.TimerService;
import com.rosanova.iot.timer.utils.TimerUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TimerServiceImpl implements TimerService {

    private final TimerRepository repository;

    private final TimerUtils timerUtils;

    private final int pillowTime = 120_000;
    private final int intervalTime = 300_000;

    public TimerServiceImpl(@Autowired  TimerRepository repository, @Qualifier("alarmTimer") TimerUtils timerUtils) {
        this.repository = repository;
        this.timerUtils = timerUtils;
    }

    @Transactional(rollbackFor = Exception.class)
    public Result insertTimer(String name, int time) {

        int step = 0;
        Result result = Result.SUCCESS;
        int start = time-intervalTime;
        int end = time;
        String onCalendar = LocalTime.ofNanoOfDay((long)start * 1000_000).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String nameFile = String.valueOf(time);



        int startPillowTime = start - pillowTime;
        int endPillowTime = end + pillowTime;



        try {
            CheckTimerInsertValidity check = repository.countOverlapsAndMaxTimers(startPillowTime, endPillowTime);

            if (check.getOverlaps() > 0 || check.getTotal() >= 20) throw new RuntimeException();

            Timer timer = new Timer();
            timer.setEndTime(end);
            timer.setStartTime(start);
            timer.setTimerName(name);

            repository.insert(timer);

            result = timerUtils.createSystemdTimerUnit(nameFile, onCalendar);

            step++;

            if (result == Result.ERROR) throw new RuntimeException();



            result = timerUtils.timerReload();

            step++;

            if (result == Result.ERROR) throw new RuntimeException();


            result = timerUtils.activateSystemdTimer(nameFile);

            step++;

            if (result == Result.ERROR) throw new RuntimeException();



            return result;

        } catch (Exception e) {
            throw new TimerServiceException(e.getMessage());
        }finally {

            Result reverseError = Result.SUCCESS;

            try {
                if(result == Result.ERROR) {

                    if (step > 2 && timerUtils.deactivateSystemdTimer(nameFile) == Result.ERROR) reverseError = Result.ERROR;
                    if (step > 0 && timerUtils.reversSystemdTimerUnitInsert(nameFile) == Result.ERROR) reverseError = Result.ERROR;
                    if (step > 1 && timerUtils.timerReload() == Result.ERROR) reverseError = Result.ERROR;


                    if(reverseError == Result.ERROR) throw new RuntimeException();
                }
            }catch (Exception e){
                System.err.println("fatal Error Rollback"+e.getMessage());
            }

        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result removeTimer(int id) {
        Result result = Result.SUCCESS;
        int step = 0;
        String filename = null;

        try{
        Timer timerToDelete = repository.findById(id);

        if (timerToDelete == null) throw new TimerServiceException("error");

        filename = String.valueOf(timerToDelete.getEndTime());
        repository.deleteById(id);

        result = timerUtils.deactivateSystemdTimer(filename);
        step++;

        if (result == Result.ERROR) throw new TimerServiceException("error");

        result = timerUtils.deleteSystemdTimerUnit(filename);
        step++;

        if (result == Result.ERROR) throw new TimerServiceException("error");


        result = timerUtils.timerReload();


        if (result == Result.ERROR) throw new TimerServiceException("error");

        return result;

        } catch (Exception e) {
            throw new TimerServiceException(e.getMessage());
        }finally {

            Result reverseError = Result.SUCCESS;

            try {
                if(result == Result.ERROR) {

                    if (step > 1 && timerUtils.reverseDeleteSystemdTimerUnit(filename) == Result.ERROR) reverseError = Result.ERROR;
                    if (step > 0 && timerUtils.activateSystemdTimer(filename) == Result.ERROR) reverseError = Result.ERROR;
                    if ( timerUtils.timerReload() == Result.ERROR) reverseError = Result.ERROR;


                    if(reverseError == Result.ERROR) throw new RuntimeException();
                }
            }catch (Exception e){
                System.err.println("fatal Error Rollback"+e.getMessage());
            }

        }


    }

    @Override
    public List<Timer> getAllTimers(int start) {
        return repository.findAll();
    }
}
