package com.rosanova.iot.timer.monitor.service;


import com.rosanova.iot.timer.error.MonitorServiceException;
import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.monitor.Monitor;
import com.rosanova.iot.timer.monitor.repository.MonitorRepository;
import com.rosanova.iot.timer.utils.impl.MonitorTimerShutdownUtilImpl;
import com.rosanova.iot.timer.utils.impl.MonitorTimerUtilImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class monitorServiceImplUnitTestUpdateStop {

    @Mock
    MonitorRepository repository;

    @Mock
    MonitorTimerUtilImpl monitorTurnOffUtils;

    @Mock
    MonitorTimerShutdownUtilImpl monitorTurnOnUtils;

    MonitorServiceImpl monitorService;

    @BeforeEach
    void setUp() {
        monitorService = new MonitorServiceImpl(repository, monitorTurnOnUtils, monitorTurnOffUtils);
    }

    @Test
    void updateMonitorStartFirstDatabaseAccessError() {
        // Setup locale
        int newStart = 9 * 60 * 60 * 1000;

        // Mock dell'errore al primo accesso
        Mockito.doThrow(new RuntimeException("DB Connection Error")).when(repository).getMonitor();

        // Verifica che venga lanciata la MonitorServiceException con il messaggio corretto
        Assertions.assertThrows(MonitorServiceException.class, () -> monitorService.updateMonitorStart(newStart));

        // Verifica che nessuna operazione systemd sia stata tentata
        Mockito.verify(monitorTurnOffUtils, Mockito.times(0)).deactivateSystemdTimer(Mockito.anyString());
        Mockito.verify(repository, Mockito.times(0)).updateStart(Mockito.anyInt(), Mockito.anyInt());
    }

    @Test
    void updateMonitorStartHappyPath() {
        // Variabili locali
        int id = 1;
        int start = 8 * 60 * 60 * 1000;
        int newStop = 22 * 60 * 60 * 1000;
        int stop = 20 * 60 * 60 * 1000;
        String prevStopStr = String.valueOf(stop);
        String newStopStr = String.valueOf(newStop);
        String newStopLiteral = "22:00:00";
        String newStopSec = newStopStr.substring(0, newStopStr.length() - 3);

        Monitor monitor = new Monitor(id, start, stop);

        // Definizione comportamento
        Mockito.doReturn(monitor).when(repository).getMonitor();
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).deactivateSystemdTimer(prevStopStr);
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).deleteSystemdTimerUnit(prevStopStr);
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).createSystemdTimerUnit(newStopStr, newStopLiteral, newStopSec);
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).timerReload();
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).activateSystemdTimer(newStopStr);
        Mockito.doReturn(1).when(repository).updateStop(id, newStop);

        // Esecuzione
        Assertions.assertEquals(Result.SUCCESS, monitorService.updateMonitorStop(newStop));

        // Verifiche
        Mockito.verify(monitorTurnOffUtils).deactivateSystemdTimer(prevStopStr);
        Mockito.verify(monitorTurnOffUtils).activateSystemdTimer(newStopStr);
        Mockito.verify(repository).updateStop(id, newStop);
    }

    @Test
    void updateMonitorStartRollbackOnStep3Error() {
        // Variabili locali
        int prevStop = 20 * 60 * 60 * 1000;
        int newStop = 22 * 60 * 60 * 1000;
        String prevStopStr = String.valueOf(prevStop);
        String newStopStr = String.valueOf(newStop);
        String newStopLiteral = "09:00:00";
        String newStopSec = newStopStr.substring(0, newStopStr.length() - 3);

        Monitor monitor = new Monitor(1, 8 * 60 * 60 * 1000, prevStop);

        Mockito.doReturn(monitor).when(repository).getMonitor();
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).deactivateSystemdTimer(prevStopStr);
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).deleteSystemdTimerUnit(prevStopStr);

        // Errore al passo 3 (creazione unità)
        Mockito.doReturn(Result.ERROR).when(monitorTurnOffUtils).createSystemdTimerUnit(newStopStr, newStopLiteral, newStopSec);

        // Mocks per il Rollback (step 1, 2,3)
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).reverseDeleteSystemdTimerUnit(prevStopStr);
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).activateSystemdTimer(prevStopStr);
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).reversSystemdTimerUnitInsert(newStopStr);
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).timerReload();

        Assertions.assertThrows(MonitorServiceException.class, () -> monitorService.updateMonitorStop(newStop));

        // Verifiche rollback eseguiti
        Mockito.verify(monitorTurnOffUtils).reverseDeleteSystemdTimerUnit(prevStopStr);
        Mockito.verify(monitorTurnOffUtils).activateSystemdTimer(prevStopStr);
        Mockito.verify(monitorTurnOffUtils).reversSystemdTimerUnitInsert(newStopStr);
    }

    @Test
    void updateMonitorStartRollbackOnDatabaseUpdateError() {
        // Variabili locali
        int id = 1;
        int prevStop = 20 * 60 * 60 * 1000;
        int newStop = 22 * 60 * 60 * 1000;
        String prevStopStr = String.valueOf(prevStop);
        String newStopStr = String.valueOf(newStop);

        Monitor monitor = new Monitor(id, 8 * 60 * 60 * 1000,prevStop) ;

        Mockito.doReturn(monitor).when(repository).getMonitor();
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).deactivateSystemdTimer(Mockito.anyString());
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).deleteSystemdTimerUnit(Mockito.anyString());
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).createSystemdTimerUnit(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(Result.SUCCESS,Result.SUCCESS).when(monitorTurnOffUtils).timerReload();
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).activateSystemdTimer(Mockito.anyString());

        // Errore all'ultimo passo (salvataggio DB)
        Mockito.doThrow(new RuntimeException("DB Update Failure")).when(repository).updateStop(id, newStop);

        // Mocks per Rollback completo
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).deactivateSystemdTimer(newStopStr);
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).reversSystemdTimerUnitInsert(newStopStr);
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).reverseDeleteSystemdTimerUnit(prevStopStr);
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOffUtils).activateSystemdTimer(prevStopStr);
        Assertions.assertThrows(MonitorServiceException.class, () -> monitorService.updateMonitorStop(newStop));

        Mockito.verify(monitorTurnOffUtils).activateSystemdTimer(newStopStr);

        // Verifica che l'intero rollback sia stato tentato
        Mockito.verify(monitorTurnOffUtils).deactivateSystemdTimer(newStopStr);
        Mockito.verify(monitorTurnOffUtils).reversSystemdTimerUnitInsert(newStopStr);
        Mockito.verify(monitorTurnOffUtils).reverseDeleteSystemdTimerUnit(prevStopStr);
        Mockito.verify(monitorTurnOffUtils).activateSystemdTimer(prevStopStr);
    }

    @Test
    void updateMonitorStartCriticalErrorInRollback() {
        // Variabili locali
        int prevStop = 20 * 60 * 60 * 1000;
        int newStop = 22 * 60 * 60 * 1000;
        String prevStopStr = String.valueOf(prevStop);

        Monitor monitor = new Monitor(1, 8 * 60 * 60 * 1000,prevStop) ;

        Mockito.doReturn(monitor).when(repository).getMonitor();

        // Fallimento immediato al passo 1
        Mockito.doReturn(Result.ERROR).when(monitorTurnOffUtils).deactivateSystemdTimer(prevStopStr);

        // Durante il rollback del passo 1, lanciamo un'eccezione imprevista
        Mockito.doThrow(new RuntimeException("Systemd Crash")).when(monitorTurnOffUtils).activateSystemdTimer(prevStopStr);

        // Il metodo deve comunque terminare lanciando la MonitorServiceException
        Assertions.assertThrows(MonitorServiceException.class, () -> monitorService.updateMonitorStop(newStop));

        // Verifica che il tentativo di rollback sia avvenuto
        Mockito.verify(monitorTurnOffUtils).activateSystemdTimer(prevStopStr);
    }

    @Test
    void updateMonitorStartCriticalErrorInRollbackExceptionInHappyPath() {
        // Variabili locali
        int prevStop = 20 * 60 * 60 * 1000;
        int newStop = 22 * 60 * 60 * 1000;
        String prevStopStr = String.valueOf(prevStop);

        Monitor monitor = new Monitor(1, 8 * 60 * 60 * 1000, prevStop );

        Mockito.doReturn(monitor).when(repository).getMonitor();

        // Fallimento immediato al passo 1
        Mockito.doThrow(new RuntimeException("errore imprevisto")).when(monitorTurnOffUtils).deactivateSystemdTimer(prevStopStr);

        // Durante il rollback del passo 1, lanciamo un'eccezione imprevista
        Mockito.doThrow(new RuntimeException("Systemd Crash")).when(monitorTurnOffUtils).activateSystemdTimer(prevStopStr);

        // Il metodo deve comunque terminare lanciando la MonitorServiceException
        Assertions.assertThrows(MonitorServiceException.class, () -> monitorService.updateMonitorStop(newStop));

        // Verifica che il tentativo di rollback sia avvenuto
        Mockito.verify(monitorTurnOffUtils).activateSystemdTimer(prevStopStr);
    }

    @Test
    void updateMonitorStartErrorWhenStartIsGreaterThanStop() {
        // Variabili locali
        int id = 1;
        int currentStart = 9 * 60 * 60 * 1000;  // 08:00
        int currentStop = 20 * 60 * 60 * 1000;  // 20:00
        int invalidNewStop = 8 * 60 * 60 * 1000; // 21:00 (Maggiore di stop)

        Monitor monitor = new Monitor(id, currentStart, currentStop);

        // Mock del database che restituisce il monitor esistente
        Mockito.doReturn(monitor).when(repository).getMonitor();

        // Verifica che venga lanciata MonitorServiceException per validazione fallita
        MonitorServiceException exception = Assertions.assertThrows(
                MonitorServiceException.class,
                () -> monitorService.updateMonitorStop(invalidNewStop)
        );

        // Verifica opzionale del messaggio d'errore
        Assertions.assertEquals("Invalid start value", exception.getMessage());

        // Verifica che non sia stata tentata alcuna operazione sui timer o sul database
        Mockito.verify(monitorTurnOffUtils, Mockito.times(0)).deactivateSystemdTimer(Mockito.anyString());
        Mockito.verify(monitorTurnOffUtils, Mockito.times(0)).createSystemdTimerUnit(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(repository, Mockito.times(0)).updateStop(Mockito.anyInt(), Mockito.anyInt());
    }

    @Test
    void updateMonitorStartErrorWhenStartIsEqualToStop() {
        // Variabili locali
        int id = 1;
        int currentStop = 20 * 60 * 60 * 1000; // 20:00
        int invalidNewStop = 8 * 60 * 60 * 1000; // 20:00 (Uguale a stop)

        Monitor monitor = new Monitor(id, 9 * 60 * 60 * 1000, currentStop);

        Mockito.doReturn(monitor).when(repository).getMonitor();

        // Il codice usa start >= monitor.getStop(), quindi anche l'uguaglianza deve fallire
        Assertions.assertThrows(MonitorServiceException.class,
                () -> monitorService.updateMonitorStop(invalidNewStop));

        Mockito.verifyNoInteractions(monitorTurnOffUtils);
    }
}