package com.rosanova.iot.timer.utils.unit_test;

import com.rosanova.iot.timer.utils.TimerUtilsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.nio.file.Paths;

@ExtendWith(MockitoExtension.class)
class TimerUtilsImplUnitTest {

    final String TMP_DIR = "tmpDir";
    final String SYSTEM_DIR = "systemDir";
    final String SERVICE_FILE_NAME = "alarm.service";
    final String[] FILE_STATIC = {"[Unit]\nDescription=Custom Timer for ", "\n\n[Timer]\nOnCalendar= *-*-* ", "\nUnit=", "\n\n[Install]\nWantedBy=timers.target\n"};

    @Spy
    TimerUtilsImpl timerUtilsImpl = new TimerUtilsImpl(TMP_DIR,SYSTEM_DIR,SERVICE_FILE_NAME);

    @Test
    void createTimerTest() {
        String timerName ="123";
        Path tmpTargetFile = Paths.get(TMP_DIR).resolve(timerName+".timer");
        Path targetTimerFile = Paths.get(SYSTEM_DIR).resolve(timerName+".timer");

        Mockito.doReturn(0).when(timerUtilsImpl).writeTimer( Mockito.any(),Mockito.any());
        Mockito.doReturn(0).when(timerUtilsImpl).moveTimer(Mockito.any(),Mockito.any());

        String onCalendar = "10:00:00";
        String tempFile = FILE_STATIC[0]+SERVICE_FILE_NAME+FILE_STATIC[1]+onCalendar+FILE_STATIC[2]+SERVICE_FILE_NAME+ FILE_STATIC[3];

        timerUtilsImpl.createSystemdTimerUnit(timerName,onCalendar);

        Mockito.verify(timerUtilsImpl).writeTimer(Mockito.eq(tmpTargetFile),Mockito.eq(tempFile));
        Mockito.verify(timerUtilsImpl).moveTimer(Mockito.eq(tmpTargetFile),Mockito.eq(targetTimerFile));
    }

}