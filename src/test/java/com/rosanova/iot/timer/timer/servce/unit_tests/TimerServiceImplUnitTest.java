package com.rosanova.iot.timer.timer.servce.unit_tests;

import com.rosanova.iot.timer.Result;
import com.rosanova.iot.timer.timer.Timer;
import com.rosanova.iot.timer.timer.dto.CheckTimerInsertValidity;
import com.rosanova.iot.timer.timer.repository.TimerRepository;
import com.rosanova.iot.timer.timer.service.TimerService;
import com.rosanova.iot.timer.timer.service.impl.TimerServiceImpl;
import com.rosanova.iot.timer.utils.TimerUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TimerServiceImplUnitTest {

    @Mock
    TimerRepository repository;

    @Mock
    TimerUtils utils;

    @InjectMocks
    TimerServiceImpl timerServiceImpl;

    @Test
    void insertTimerRightTest(){

        int start = 0;
        int end = 1234;
        String name = "first alarm";

        Mockito.doReturn(Result.SUCCESS).when(utils).createSystemdTimerUnit(String.valueOf(end),String.valueOf(end));
        Mockito.doNothing().when(repository).insert(Mockito.any(Timer.class));
        Mockito.doReturn(Result.SUCCESS).when(utils).activateSystemdTimer(Mockito.anyString());

        CheckTimerInsertValidity overlap = new CheckTimerInsertValidity();
        overlap.setTotal(18);
        overlap.setOverlaps(0);

        Mockito.doReturn(overlap).when(repository).countOverlapsAndMaxTimers(Mockito.eq(start),Mockito.eq(end));

        Result result = timerServiceImpl.insertTimer(name,start,end);

        Assertions.assertEquals(Result.SUCCESS, result);

        
    }
}
