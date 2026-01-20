package com.rosanova.iot.timer.timer.service.impl;

import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.error.TimerServiceException;
import com.rosanova.iot.timer.timer.Timer;
import com.rosanova.iot.timer.timer.dto.CheckTimerInsertValidity;
import com.rosanova.iot.timer.timer.repository.TimerRepository;
import com.rosanova.iot.timer.timer.service.TimerService;
import com.rosanova.iot.timer.utils.TimerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
public class TimerServiceImpl implements TimerService {

    private final TimerRepository repository;

    private final TimerUtils timerUtils;

    private final int PILLOW_TIME = 20_000;

    private final int MAX_MILLS_DAY = 86_400_000;

    public TimerServiceImpl(@Autowired  TimerRepository repository, @Qualifier("alarmTimer") TimerUtils timerUtils) {
        this.repository = repository;
        this.timerUtils = timerUtils;
    }

    @Transactional(rollbackFor = Exception.class)
    public Result insertTimer(String name, int time, int symphony) {

        int step = 0;
        Result result = Result.SUCCESS;
        int start = Math.max(0, time - PILLOW_TIME );
        int end = Math.min(MAX_MILLS_DAY, time + PILLOW_TIME);

        String symphonyDuration = String.valueOf(symphony);

        Duration duration = Duration.ofMillis(time);

// Trasforma in formato HH:mm:ss senza fusi orari di mezzo
        String onCalendar = String.format("%02d:%02d:%02d",
                duration.toHours(),
                duration.toMinutesPart(),
                duration.toSecondsPart());


        String nameFile = String.valueOf(time);




        try {
            CheckTimerInsertValidity check = repository.countOverlapsAndMaxTimers(start, end);

            if (check.getOverlaps() > 0 || check.getTotal() >= 40) throw new RuntimeException();

            Timer timer = new Timer();
            timer.setEndTime(end);
            timer.setStartTime(start);
            timer.setTimerName(name);

            repository.insert(timer);

            result = timerUtils.createSystemdTimerUnit(nameFile, onCalendar,symphonyDuration);

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
    public Result removeTimer(long id) {
        Result result = Result.SUCCESS;
        int step = 0;
        String filename = null;

        try{
        Timer timerToDelete = repository.findById(id);

        if (timerToDelete == null) throw new TimerServiceException("errore timer non trovato");

        filename = String.valueOf(timerToDelete.getEndTime());
        repository.deleteById(id);

        result = timerUtils.deactivateSystemdTimer(filename);
        step++;

        if (result == Result.ERROR) throw new TimerServiceException("timer non disattivato");

        result = timerUtils.deleteSystemdTimerUnit(filename);
        step++;

        if (result == Result.ERROR) throw new TimerServiceException("timer fisico non cancellato");


        result = timerUtils.timerReload();


        if (result == Result.ERROR) throw new TimerServiceException("errore durante daemon-Reload");

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
