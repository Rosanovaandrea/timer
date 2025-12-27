package com.rosanova.iot.timer.utils.unit_test;

import com.rosanova.iot.timer.Result;
import com.rosanova.iot.timer.utils.impl.TimerUtilsImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimerUtilsImplUnitTest {

    final String TMP_DIR = "tmpDir";
    final String SYSTEM_DIR = "systemDir";
    final String SERVICE_FILE_NAME = "alarm.service";
    final String[] FILE_STATIC = {"[Unit]\nDescription=Custom Timer for ", "\n\n[Timer]\nOnCalendar= *-*-* ", "\nUnit=", "\n\n[Install]\nWantedBy=timers.target\n"};

    final String CMD_ACTIVATE = "sudo /usr/bin/systemctl daemon-reload && sudo /usr/bin/systemctl enable 123.timer && sudo /usr/bin/systemctl start 123.timer";
    final String CMD_DEACTIVATE = "sudo /usr/bin/systemctl stop 123.timer && sudo /usr/bin/systemctl disable 123.timer";

    @Spy
    TimerUtilsImpl timerUtilsImpl = new TimerUtilsImpl(TMP_DIR,SYSTEM_DIR,SERVICE_FILE_NAME);

    @Test
    void createTimerTest() {
        String timerName ="123";
        Path tmpTargetFile = Paths.get(TMP_DIR).resolve(timerName+".timer");
        Path targetTimerFile = Paths.get(SYSTEM_DIR).resolve(timerName+".timer");

        doReturn(Result.SUCCESS).when(timerUtilsImpl).writeTimer( Mockito.any(),Mockito.any());
        doReturn(Result.SUCCESS).when(timerUtilsImpl).moveTimer(Mockito.any(),Mockito.any());

        String onCalendar = "10:00:00";
        String tempFile = FILE_STATIC[0]+SERVICE_FILE_NAME+FILE_STATIC[1]+onCalendar+FILE_STATIC[2]+SERVICE_FILE_NAME+ FILE_STATIC[3];

        timerUtilsImpl.createSystemdTimerUnit(timerName,onCalendar);

        verify(timerUtilsImpl).writeTimer(Mockito.eq(tmpTargetFile),Mockito.eq(tempFile));
        verify(timerUtilsImpl).moveTimer(Mockito.eq(tmpTargetFile),Mockito.eq(targetTimerFile));
    }

    @Test
    void createTimerErrorWriteTest() {
        String timerName ="123";
        Path tmpTargetFile = Paths.get(TMP_DIR).resolve(timerName+".timer");
        Path targetTimerFile = Paths.get(SYSTEM_DIR).resolve(timerName+".timer");

        doReturn(Result.ERROR).when(timerUtilsImpl).writeTimer( Mockito.any(),Mockito.any());

        String onCalendar = "10:00:00";
        String tempFile = FILE_STATIC[0]+SERVICE_FILE_NAME+FILE_STATIC[1]+onCalendar+FILE_STATIC[2]+SERVICE_FILE_NAME+ FILE_STATIC[3];

        Assertions.assertEquals(Result.ERROR, timerUtilsImpl.createSystemdTimerUnit(timerName,onCalendar));

        verify(timerUtilsImpl).writeTimer(Mockito.eq(tmpTargetFile),Mockito.eq(tempFile));
        verify(timerUtilsImpl,Mockito.times(0)).moveTimer(Mockito.any(),Mockito.any());
    }

    @Test
    void createTimerErrorMoveTest() {
        String timerName ="123";
        Path tmpTargetFile = Paths.get(TMP_DIR).resolve(timerName+".timer");
        Path targetTimerFile = Paths.get(SYSTEM_DIR).resolve(timerName+".timer");

        doReturn(Result.SUCCESS).when(timerUtilsImpl).writeTimer( Mockito.any(),Mockito.any());
        doReturn(Result.ERROR).when(timerUtilsImpl).moveTimer(Mockito.any(),Mockito.any());

        String onCalendar = "10:00:00";
        String tempFile = FILE_STATIC[0]+SERVICE_FILE_NAME+FILE_STATIC[1]+onCalendar+FILE_STATIC[2]+SERVICE_FILE_NAME+ FILE_STATIC[3];

        Assertions.assertEquals(Result.ERROR, timerUtilsImpl.createSystemdTimerUnit(timerName,onCalendar));

        verify(timerUtilsImpl).writeTimer(Mockito.eq(tmpTargetFile),Mockito.eq(tempFile));
        verify(timerUtilsImpl).moveTimer(Mockito.eq(tmpTargetFile),Mockito.eq(targetTimerFile));
    }

    @Test
    void activateSystemdTimerSuccessTest() throws Exception {
        String timerName = "123";

        // Mocking della catena ProcessBuilder -> Process
        ProcessBuilder mockPb = mock(ProcessBuilder.class);
        Process mockProcess = mock(Process.class);

        doReturn(mockPb).when(timerUtilsImpl).getProcessBuilder(any());
        when(mockPb.start()).thenReturn(mockProcess);
        when(mockProcess.waitFor()).thenReturn(0); // Successo

        // Esecuzione
        Result result = timerUtilsImpl.activateSystemdTimer(timerName);

        Assertions.assertEquals(Result.SUCCESS, result);

        // Verifica dei parametri passati al ProcessBuilder (Array di 3 stringhe)
        String[] expectedFullCommand = {"/bin/sh", "-c", CMD_ACTIVATE};
        verify(timerUtilsImpl).getProcessBuilder(eq(expectedFullCommand));
        verify(mockProcess).waitFor();
    }

    @Test
    void activateSystemdTimerErrorExitCodeTest() throws Exception {
        String timerName = "123";
        ProcessBuilder mockPb = mock(ProcessBuilder.class);
        Process mockProcess = mock(Process.class);

        doReturn(mockPb).when(timerUtilsImpl).getProcessBuilder(any());
        when(mockPb.start()).thenReturn(mockProcess);
        when(mockProcess.waitFor()).thenReturn(1); // Errore di sistema

        Result result = timerUtilsImpl.activateSystemdTimer(timerName);

        Assertions.assertEquals(Result.ERROR, result);
        verify(timerUtilsImpl).getProcessBuilder(any());
    }

    @Test
    void activateSystemdTimerExceptionTest() throws Exception {
        String timerName = "123";
        ProcessBuilder mockPb = mock(ProcessBuilder.class);

        doReturn(mockPb).when(timerUtilsImpl).getProcessBuilder(any());
        // Simuliamo un fallimento fisico dell'esecuzione (es. comando non trovato)
        when(mockPb.start()).thenThrow(new IOException("Simulated IO Error"));

        Result result = timerUtilsImpl.activateSystemdTimer(timerName);

        Assertions.assertEquals(Result.ERROR, result);
        verify(timerUtilsImpl).getProcessBuilder(any());
    }

    @Test
    void deactivateSystemdTimerSuccessTest() throws Exception {
        String timerName = "123";
        ProcessBuilder mockPb = mock(ProcessBuilder.class);
        Process mockProcess = mock(Process.class);

        doReturn(mockPb).when(timerUtilsImpl).getProcessBuilder(any());
        when(mockPb.start()).thenReturn(mockProcess);
        when(mockProcess.waitFor()).thenReturn(0);

        Result result = timerUtilsImpl.deactivateSystemdTimer(timerName);

        Assertions.assertEquals(Result.SUCCESS, result);

        // Verifica specifica del comando di disattivazione
        String[] expectedFullCommand = {"/bin/sh", "-c", CMD_DEACTIVATE};
        verify(timerUtilsImpl).getProcessBuilder(eq(expectedFullCommand));
    }

    @Test
    void deactivateSystemdTimerInterruptedTest() throws Exception {
        String timerName = "123";
        ProcessBuilder mockPb = mock(ProcessBuilder.class);
        Process mockProcess = mock(Process.class);

        doReturn(mockPb).when(timerUtilsImpl).getProcessBuilder(any());
        when(mockPb.start()).thenReturn(mockProcess);
        // Simuliamo l'interruzione del thread durante l'attesa del processo
        when(mockProcess.waitFor()).thenThrow(new InterruptedException());

        Result result = timerUtilsImpl.deactivateSystemdTimer(timerName);

        Assertions.assertEquals(Result.ERROR, result);
    }

}