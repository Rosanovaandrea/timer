package com.rosanova.iot.timer.monitor;

import com.rosanova.iot.timer.error.MonitorServiceException;
import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.monitor.repository.MonitorRepository;
import com.rosanova.iot.timer.utils.impl.MonitorTimerShutdownUtilImpl;
import com.rosanova.iot.timer.utils.impl.MonitorTimerUtilImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class MonitorStartupUnitTest {

    @Mock
    MonitorRepository repository;

    @Mock
    MonitorTimerUtilImpl monitorTurnOnUtils;

    @Mock
    MonitorTimerShutdownUtilImpl monitorTurnOffUtils;

    MonitorStartup monitorStartup;

    int start = 8 * 60 * 60 * 1000;
    int stop = 20 * 60 * 60 * 1000;
    String literalStart = "08:00:00";
    String literalStop = "20:00:00";
    String startString = String.valueOf(start);
    String stopString = String.valueOf(stop);
    String startStringSecond = startString.substring(0,startString.length()-3);
    String stopStringSecond = stopString.substring(0,stopString.length()-3);

    @BeforeEach
    void setUp() {
        monitorStartup = new MonitorStartup(repository,monitorTurnOnUtils, monitorTurnOffUtils);
    }

    @Test
    void createMonitorAllRight() {


        ArgumentCaptor<Monitor> captor = ArgumentCaptor.forClass(Monitor.class);

        Mockito.doReturn(null).when(repository).getMonitor();
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).createSystemdTimerUnit(Mockito.anyString(),Mockito.anyString(),Mockito.anyString());
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).createSystemdTimerUnit(Mockito.anyString(),Mockito.anyString(),Mockito.anyString());
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).timerReload();
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).activateSystemdTimer(Mockito.anyString());
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).activateSystemdTimer(Mockito.anyString());
        Mockito.doReturn(0).when(repository).save(Mockito.any(Monitor.class));

        Assertions.assertEquals(Result.SUCCESS,monitorStartup.createMonitor());

        Mockito.verify(monitorTurnOnUtils).createSystemdTimerUnit(Mockito.eq(startString),Mockito.eq(literalStart),Mockito.eq(startStringSecond));
        Mockito.verify(monitorTurnOffUtils).createSystemdTimerUnit(Mockito.eq(stopString),Mockito.eq(literalStop),Mockito.eq(stopStringSecond));
        Mockito.verify(monitorTurnOnUtils).timerReload();
        Mockito.verify(monitorTurnOnUtils).activateSystemdTimer(Mockito.eq(String.valueOf(start)));
        Mockito.verify(monitorTurnOffUtils).activateSystemdTimer(Mockito.eq(String.valueOf(stop)));

        Mockito.verify(monitorTurnOnUtils,Mockito.times(0)).reversSystemdTimerUnitInsert(Mockito.any());
        Mockito.verify(monitorTurnOffUtils,Mockito.times(0)).reversSystemdTimerUnitInsert(Mockito.any());
        Mockito.verify(monitorTurnOnUtils,Mockito.times(0)).deactivateSystemdTimer(Mockito.anyString());
        Mockito.verify(monitorTurnOffUtils,Mockito.times(0)).deactivateSystemdTimer(Mockito.anyString());


        Mockito.verify(repository).save(captor.capture());
        Monitor monitor = captor.getValue();
        Assertions.assertNotNull(monitor);
        Assertions.assertEquals(start,monitor.getStart());
        Assertions.assertEquals(stop,monitor.getStop());

    }

    @Test
    void createMonitorAlreadyExists() {

        Mockito.doReturn(new Monitor()).when(repository).getMonitor();

        Assertions.assertEquals(Result.SUCCESS,monitorStartup.createMonitor());

        Mockito.verify(monitorTurnOnUtils,Mockito.times(0)).createSystemdTimerUnit(Mockito.any(),Mockito.any(),Mockito.any());
        Mockito.verify(monitorTurnOffUtils,Mockito.times(0)).createSystemdTimerUnit(Mockito.any(),Mockito.any(),Mockito.any());
        Mockito.verify(monitorTurnOnUtils,Mockito.times(0)).timerReload();
        Mockito.verify(monitorTurnOnUtils,Mockito.times(0)).activateSystemdTimer(Mockito.any());
        Mockito.verify(monitorTurnOffUtils,Mockito.times(0)).activateSystemdTimer(Mockito.any());

        Mockito.verify(monitorTurnOnUtils,Mockito.times(0)).reversSystemdTimerUnitInsert(Mockito.any());
        Mockito.verify(monitorTurnOffUtils,Mockito.times(0)).reversSystemdTimerUnitInsert(Mockito.any());
        Mockito.verify(monitorTurnOnUtils,Mockito.times(0)).deactivateSystemdTimer(Mockito.anyString());
        Mockito.verify(monitorTurnOffUtils,Mockito.times(0)).deactivateSystemdTimer(Mockito.anyString());

    }

    @Test
    void createMonitorErrorOnCreateStartTimer() {

        int start = 8 * 60 * 60 * 1000;
        int stop = 20 * 60 * 60 * 1000;
        String literalStart = "08:00:00";
        String literalStop = "20:00:00";

        Mockito.doReturn(null).when(repository).getMonitor();
        Mockito.doReturn(Result.ERROR).when(monitorTurnOnUtils).createSystemdTimerUnit(Mockito.anyString(),Mockito.anyString(),Mockito.any());

        Assertions.assertThrows(MonitorServiceException.class,()->{monitorStartup.createMonitor();});

        Mockito.verify(monitorTurnOnUtils).createSystemdTimerUnit(Mockito.eq(startString),Mockito.eq(literalStart),Mockito.eq(startStringSecond));
        Mockito.verify(monitorTurnOffUtils,Mockito.times(0)).createSystemdTimerUnit(Mockito.eq(stopString),Mockito.eq(literalStop),Mockito.eq(stopStringSecond));
        Mockito.verify(monitorTurnOnUtils,Mockito.times(1)).timerReload();
        Mockito.verify(monitorTurnOnUtils,Mockito.times(0)).activateSystemdTimer(Mockito.eq(String.valueOf(start)));
        Mockito.verify(monitorTurnOffUtils,Mockito.times(0)).activateSystemdTimer(Mockito.eq(String.valueOf(stop)));

        Mockito.verify(monitorTurnOnUtils,Mockito.times(1)).reversSystemdTimerUnitInsert(Mockito.eq(String.valueOf(start)));
        Mockito.verify(monitorTurnOffUtils,Mockito.times(0)).reversSystemdTimerUnitInsert(Mockito.any());
        Mockito.verify(monitorTurnOnUtils,Mockito.times(0)).deactivateSystemdTimer(Mockito.anyString());
        Mockito.verify(monitorTurnOffUtils,Mockito.times(0)).deactivateSystemdTimer(Mockito.anyString());

    }

    @Test
    void createMonitorErrorOnCreateStopTimer() {

        int start = 8 * 60 * 60 * 1000;
        int stop = 20 * 60 * 60 * 1000;
        String literalStart = "08:00:00";
        String literalStop = "20:00:00";

        Mockito.doReturn(null).when(repository).getMonitor();
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).createSystemdTimerUnit(Mockito.anyString(),Mockito.anyString(),Mockito.anyString());
        Mockito.doReturn(Result.ERROR).when(monitorTurnOffUtils).createSystemdTimerUnit(Mockito.anyString(),Mockito.anyString(),Mockito.anyString());

        Assertions.assertThrows(MonitorServiceException.class,()->{monitorStartup.createMonitor();});

        Mockito.verify(monitorTurnOnUtils).createSystemdTimerUnit(Mockito.eq(startString),Mockito.eq(literalStart),Mockito.eq(startStringSecond));
        Mockito.verify(monitorTurnOffUtils,Mockito.times(1)).createSystemdTimerUnit(Mockito.eq(stopString),Mockito.eq(literalStop),Mockito.eq(stopStringSecond));
        Mockito.verify(monitorTurnOnUtils,Mockito.times(1)).timerReload();
        Mockito.verify(monitorTurnOnUtils,Mockito.times(0)).activateSystemdTimer(Mockito.eq(String.valueOf(start)));
        Mockito.verify(monitorTurnOffUtils,Mockito.times(0)).activateSystemdTimer(Mockito.eq(String.valueOf(stop)));

        Mockito.verify(monitorTurnOnUtils,Mockito.times(1)).reversSystemdTimerUnitInsert(Mockito.eq(String.valueOf(start)));
        Mockito.verify(monitorTurnOffUtils,Mockito.times(1)).reversSystemdTimerUnitInsert(Mockito.eq(String.valueOf(stop)));
        Mockito.verify(monitorTurnOnUtils,Mockito.times(0)).deactivateSystemdTimer(Mockito.anyString());
        Mockito.verify(monitorTurnOffUtils,Mockito.times(0)).deactivateSystemdTimer(Mockito.anyString());

    }


    @Test
    void createMonitorErrorOnTimerReload() {

        int start = 8 * 60 * 60 * 1000;
        int stop = 20 * 60 * 60 * 1000;
        String literalStart = "08:00:00";
        String literalStop = "20:00:00";

        Mockito.doReturn(null).when(repository).getMonitor();
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).createSystemdTimerUnit(Mockito.anyString(),Mockito.anyString(),Mockito.any());
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).createSystemdTimerUnit(Mockito.anyString(),Mockito.anyString(),Mockito.any());
        Mockito.doReturn(Result.ERROR).when(monitorTurnOnUtils).timerReload();

        Assertions.assertThrows(MonitorServiceException.class,()->{monitorStartup.createMonitor();});

        Mockito.verify(monitorTurnOnUtils).createSystemdTimerUnit(Mockito.eq(startString),Mockito.eq(literalStart),Mockito.eq(startStringSecond));
        Mockito.verify(monitorTurnOffUtils).createSystemdTimerUnit(Mockito.eq(stopString),Mockito.eq(literalStop),Mockito.eq(stopStringSecond));
        Mockito.verify(monitorTurnOnUtils,Mockito.times(2)).timerReload();
        Mockito.verify(monitorTurnOnUtils,Mockito.times(0)).activateSystemdTimer(Mockito.eq(String.valueOf(start)));
        Mockito.verify(monitorTurnOffUtils,Mockito.times(0)).activateSystemdTimer(Mockito.eq(String.valueOf(stop)));

        Mockito.verify(monitorTurnOnUtils,Mockito.times(1)).reversSystemdTimerUnitInsert(Mockito.eq(String.valueOf(start)));
        Mockito.verify(monitorTurnOffUtils,Mockito.times(1)).reversSystemdTimerUnitInsert(Mockito.eq(String.valueOf(stop)));
        Mockito.verify(monitorTurnOnUtils,Mockito.times(0)).deactivateSystemdTimer(Mockito.anyString());
        Mockito.verify(monitorTurnOffUtils,Mockito.times(0)).deactivateSystemdTimer(Mockito.anyString());

    }

    @Test
    void createMonitorErrorOnActivateTimerStart() {

        int start = 8 * 60 * 60 * 1000;
        int stop = 20 * 60 * 60 * 1000;
        String literalStart = "08:00:00";
        String literalStop = "20:00:00";

        Mockito.doReturn(null).when(repository).getMonitor();
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).createSystemdTimerUnit(Mockito.anyString(),Mockito.anyString(),Mockito.any());
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).createSystemdTimerUnit(Mockito.anyString(),Mockito.anyString(),Mockito.any());
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).timerReload();
        Mockito.doReturn(Result.ERROR).when(monitorTurnOnUtils).activateSystemdTimer(Mockito.anyString());


        Assertions.assertThrows(MonitorServiceException.class,()->{monitorStartup.createMonitor();});

        Mockito.verify(monitorTurnOnUtils).createSystemdTimerUnit(Mockito.eq(startString),Mockito.eq(literalStart),Mockito.eq(startStringSecond));
        Mockito.verify(monitorTurnOffUtils).createSystemdTimerUnit(Mockito.eq(stopString),Mockito.eq(literalStop),Mockito.eq(stopStringSecond));
        Mockito.verify(monitorTurnOnUtils,Mockito.times(2)).timerReload();
        Mockito.verify(monitorTurnOnUtils,Mockito.times(1)).activateSystemdTimer(Mockito.eq(String.valueOf(start)));
        Mockito.verify(monitorTurnOffUtils,Mockito.times(0)).activateSystemdTimer(Mockito.eq(String.valueOf(stop)));

        Mockito.verify(monitorTurnOnUtils,Mockito.times(1)).reversSystemdTimerUnitInsert(Mockito.eq(String.valueOf(start)));
        Mockito.verify(monitorTurnOffUtils,Mockito.times(1)).reversSystemdTimerUnitInsert(Mockito.eq(String.valueOf(stop)));
        Mockito.verify(monitorTurnOnUtils,Mockito.times(1)).deactivateSystemdTimer(Mockito.anyString());
        Mockito.verify(monitorTurnOffUtils,Mockito.times(0)).deactivateSystemdTimer(Mockito.anyString());

    }

    @Test
    void createMonitorErrorOnActivateTimerStop() {

        int start = 8 * 60 * 60 * 1000;
        int stop = 20 * 60 * 60 * 1000;
        String literalStart = "08:00:00";
        String literalStop = "20:00:00";

        Mockito.doReturn(null).when(repository).getMonitor();
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).createSystemdTimerUnit(Mockito.anyString(),Mockito.anyString(),Mockito.anyString());
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).createSystemdTimerUnit(Mockito.anyString(),Mockito.anyString(),Mockito.anyString());
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).timerReload();
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).activateSystemdTimer(Mockito.anyString());
        Mockito.doReturn(Result.ERROR).when(monitorTurnOffUtils).activateSystemdTimer(Mockito.anyString());


        Assertions.assertThrows(MonitorServiceException.class,()->{monitorStartup.createMonitor();});

        Mockito.verify(monitorTurnOnUtils).createSystemdTimerUnit(Mockito.eq(startString),Mockito.eq(literalStart),Mockito.eq(startStringSecond));
        Mockito.verify(monitorTurnOffUtils).createSystemdTimerUnit(Mockito.eq(stopString),Mockito.eq(literalStop),Mockito.eq(stopStringSecond));
        Mockito.verify(monitorTurnOnUtils,Mockito.times(2)).timerReload();
        Mockito.verify(monitorTurnOnUtils,Mockito.times(1)).activateSystemdTimer(Mockito.eq(String.valueOf(start)));
        Mockito.verify(monitorTurnOffUtils,Mockito.times(1)).activateSystemdTimer(Mockito.eq(String.valueOf(stop)));

        Mockito.verify(monitorTurnOnUtils,Mockito.times(1)).reversSystemdTimerUnitInsert(Mockito.eq(String.valueOf(start)));
        Mockito.verify(monitorTurnOffUtils,Mockito.times(1)).reversSystemdTimerUnitInsert(Mockito.eq(String.valueOf(stop)));
        Mockito.verify(monitorTurnOnUtils,Mockito.times(1)).deactivateSystemdTimer(Mockito.anyString());
        Mockito.verify(monitorTurnOffUtils,Mockito.times(1)).deactivateSystemdTimer(Mockito.anyString());

    }

    @Test
    void createMonitorErrorOnInsertDatabase() {

        int start = 8 * 60 * 60 * 1000;
        int stop = 20 * 60 * 60 * 1000;
        String literalStart = "08:00:00";
        String literalStop = "20:00:00";

        Mockito.doReturn(null).when(repository).getMonitor();
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).createSystemdTimerUnit(Mockito.anyString(),Mockito.anyString(),Mockito.any());
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).createSystemdTimerUnit(Mockito.anyString(),Mockito.anyString(),Mockito.any());
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).timerReload();
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).activateSystemdTimer(Mockito.anyString());
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).activateSystemdTimer(Mockito.anyString());
        Mockito.doThrow(new DataIntegrityViolationException("errore database")).when(repository).save(Mockito.any(Monitor.class));

        Assertions.assertThrows(MonitorServiceException.class,()->{monitorStartup.createMonitor();});

        Mockito.verify(monitorTurnOnUtils).createSystemdTimerUnit(Mockito.eq(startString),Mockito.eq(literalStart),Mockito.eq(startStringSecond));
        Mockito.verify(monitorTurnOffUtils).createSystemdTimerUnit(Mockito.eq(stopString),Mockito.eq(literalStop),Mockito.eq(stopStringSecond));
        Mockito.verify(monitorTurnOnUtils,Mockito.times(2)).timerReload();
        Mockito.verify(monitorTurnOnUtils,Mockito.times(1)).activateSystemdTimer(Mockito.eq(String.valueOf(start)));
        Mockito.verify(monitorTurnOffUtils,Mockito.times(1)).activateSystemdTimer(Mockito.eq(String.valueOf(stop)));

        Mockito.verify(monitorTurnOnUtils,Mockito.times(1)).reversSystemdTimerUnitInsert(Mockito.eq(String.valueOf(start)));
        Mockito.verify(monitorTurnOffUtils,Mockito.times(1)).reversSystemdTimerUnitInsert(Mockito.eq(String.valueOf(stop)));
        Mockito.verify(monitorTurnOnUtils,Mockito.times(1)).deactivateSystemdTimer(Mockito.anyString());
        Mockito.verify(monitorTurnOffUtils,Mockito.times(1)).deactivateSystemdTimer(Mockito.anyString());
        Mockito.verify(repository).save(Mockito.any(Monitor.class));

    }

    @Test
    void createMonitorErrorOnRestore() {

        int start = 8 * 60 * 60 * 1000;
        int stop = 20 * 60 * 60 * 1000;
        String literalStart = "08:00:00";
        String literalStop = "20:00:00";

        Mockito.doReturn(null).when(repository).getMonitor();
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).createSystemdTimerUnit(Mockito.anyString(),Mockito.anyString(),Mockito.anyString());
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).createSystemdTimerUnit(Mockito.anyString(),Mockito.anyString(),Mockito.anyString());
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).timerReload();
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).activateSystemdTimer(Mockito.anyString());
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).activateSystemdTimer(Mockito.anyString());
        Mockito.doThrow(new DataIntegrityViolationException("errore database")).when(repository).save(Mockito.any(Monitor.class));
        Mockito.doReturn(Result.ERROR).when(monitorTurnOffUtils).reversSystemdTimerUnitInsert(Mockito.eq(String.valueOf(stop)));

        Assertions.assertThrows(MonitorServiceException.class,()->{monitorStartup.createMonitor();});

        Mockito.verify(monitorTurnOnUtils).createSystemdTimerUnit(Mockito.eq(startString),Mockito.eq(literalStart),Mockito.eq(startStringSecond));
        Mockito.verify(monitorTurnOffUtils).createSystemdTimerUnit(Mockito.eq(stopString),Mockito.eq(literalStop),Mockito.eq(stopStringSecond));
        Mockito.verify(monitorTurnOnUtils,Mockito.times(1)).timerReload();
        Mockito.verify(monitorTurnOnUtils,Mockito.times(1)).activateSystemdTimer(Mockito.eq(String.valueOf(start)));
        Mockito.verify(monitorTurnOffUtils,Mockito.times(1)).activateSystemdTimer(Mockito.eq(String.valueOf(stop)));

        Mockito.verify(monitorTurnOnUtils,Mockito.times(0)).reversSystemdTimerUnitInsert(Mockito.eq(String.valueOf(start)));
        Mockito.verify(monitorTurnOffUtils,Mockito.times(1)).reversSystemdTimerUnitInsert(Mockito.eq(String.valueOf(stop)));
        Mockito.verify(monitorTurnOnUtils,Mockito.times(1)).deactivateSystemdTimer(Mockito.anyString());
        Mockito.verify(monitorTurnOffUtils,Mockito.times(1)).deactivateSystemdTimer(Mockito.anyString());
        Mockito.verify(repository).save(Mockito.any(Monitor.class));

    }

    @Test
    void createMonitorErrorOnRestoreException() {

        int start = 8 * 60 * 60 * 1000;
        int stop = 20 * 60 * 60 * 1000;
        String literalStart = "08:00:00";
        String literalStop = "20:00:00";

        Mockito.doReturn(null).when(repository).getMonitor();
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).createSystemdTimerUnit(Mockito.anyString(),Mockito.anyString(),Mockito.anyString());
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).createSystemdTimerUnit(Mockito.anyString(),Mockito.anyString(),Mockito.anyString());
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).timerReload();
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).activateSystemdTimer(Mockito.anyString());
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).activateSystemdTimer(Mockito.anyString());
        Mockito.doThrow(new DataIntegrityViolationException("errore database")).when(repository).save(Mockito.any(Monitor.class));
        Mockito.doThrow(new RuntimeException("errore irreversibile")).when(monitorTurnOffUtils).reversSystemdTimerUnitInsert(Mockito.eq(String.valueOf(stop)));

        Assertions.assertThrows(MonitorServiceException.class,()->{monitorStartup.createMonitor();});

        Mockito.verify(monitorTurnOnUtils).createSystemdTimerUnit(Mockito.eq(startString),Mockito.eq(literalStart),Mockito.eq(startStringSecond));
        Mockito.verify(monitorTurnOffUtils).createSystemdTimerUnit(Mockito.eq(stopString),Mockito.eq(literalStop),Mockito.eq(stopStringSecond));
        Mockito.verify(monitorTurnOnUtils,Mockito.times(1)).timerReload();
        Mockito.verify(monitorTurnOnUtils,Mockito.times(1)).activateSystemdTimer(Mockito.eq(String.valueOf(start)));
        Mockito.verify(monitorTurnOffUtils,Mockito.times(1)).activateSystemdTimer(Mockito.eq(String.valueOf(stop)));

        Mockito.verify(monitorTurnOnUtils,Mockito.times(0)).reversSystemdTimerUnitInsert(Mockito.eq(String.valueOf(start)));
        Mockito.verify(monitorTurnOffUtils,Mockito.times(1)).reversSystemdTimerUnitInsert(Mockito.eq(String.valueOf(stop)));
        Mockito.verify(monitorTurnOnUtils,Mockito.times(1)).deactivateSystemdTimer(Mockito.anyString());
        Mockito.verify(monitorTurnOffUtils,Mockito.times(1)).deactivateSystemdTimer(Mockito.anyString());
        Mockito.verify(repository).save(Mockito.any(Monitor.class));

    }

    @Test
    void createMonitorErrorOnFirstDatabaseAccess() {
        Mockito.doThrow(new RuntimeException("DB Connection Error")).when(repository).getMonitor();

        Assertions.assertThrows(RuntimeException.class, () -> monitorStartup.createMonitor());


        Mockito.verify(monitorTurnOnUtils, Mockito.times(0)).createSystemdTimerUnit(Mockito.any(), Mockito.any(),Mockito.any());
        Mockito.verify(monitorTurnOnUtils,Mockito.times(0)).reversSystemdTimerUnitInsert(Mockito.any());
        Mockito.verify(monitorTurnOffUtils,Mockito.times(0)).reversSystemdTimerUnitInsert(Mockito.any());
        Mockito.verify(monitorTurnOnUtils,Mockito.times(0)).deactivateSystemdTimer(Mockito.anyString());
        Mockito.verify(monitorTurnOffUtils,Mockito.times(0)).deactivateSystemdTimer(Mockito.anyString());

    }

}