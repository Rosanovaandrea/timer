package com.rosanova.iot.timer.timer.servce.unit_tests;

import com.rosanova.iot.timer.Result;
import com.rosanova.iot.timer.timer.Timer;
import com.rosanova.iot.timer.timer.repository.TimerRepository;
import com.rosanova.iot.timer.timer.service.TimerService;
import com.rosanova.iot.timer.timer.service.impl.TimerServiceImpl;
import com.rosanova.iot.timer.utils.TimerUtils;
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
        Mockito.doReturn(Result.SUCCESS).when(utils).createSystemdTimerUnit(Mockito.anyString(),Mockito.anyString());
        Mockito.doNothing().when(repository).insert(Mockito.any(Timer.class));
        Mockito.doReturn(Result.SUCCESS).when(utils).activateSystemdTimer(Mockito.anyString());


    }
}
