package com.rosanova.iot.timer.timer.service.impl;

import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.error.TimerServiceException;
import com.rosanova.iot.timer.timer.Timer;
import com.rosanova.iot.timer.timer.dto.CheckTimerInsertValidity;
import com.rosanova.iot.timer.timer.repository.TimerRepository;
import com.rosanova.iot.timer.timer.service.TimerService;
import com.rosanova.iot.timer.utils.TimerUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
public class TimerServiceImpl implements TimerService {

    private final TimerRepository repository;
    private final TimerUtils timerUtils;
    private final int pillowTime = 120_000;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result insertTimer(String name, int start, int end) {

        int step = 0;
        Result result = Result.SUCCESS;
        String nameFile = String.valueOf(end);

        int startPillowTime = start-pillowTime;
        if(startPillowTime < 0){ startPillowTime = 0; }

        try {
            CheckTimerInsertValidity check = repository.countOverlapsAndMaxTimers(startPillowTime, end+pillowTime);

            if (check.getOverlaps() > 0 || check.getTotal() >= 20) throw new RuntimeException();

            Timer timer = new Timer();
            timer.setEndTime(end);
            timer.setStartTime(start);
            timer.setTimerName(name);

            repository.insert(timer);

            result = timerUtils.createSystemdTimerUnit(nameFile, nameFile);

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
    public Result removeTimer(int id) {
        return null;
    }

    @Override
    public List<Timer> getAllTimers(int start) {
        return List.of();
    }
}
