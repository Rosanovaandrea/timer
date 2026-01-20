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
class monitorServiceImplUnitTestUpdateStart {

    @Mock
    MonitorRepository repository;

    @Mock
    MonitorTimerUtilImpl monitorTurnOnUtils;

    @Mock
    MonitorTimerShutdownUtilImpl monitorTurnOffUtils;

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
        Mockito.verify(monitorTurnOnUtils, Mockito.times(0)).deactivateSystemdTimer(Mockito.anyString());
        Mockito.verify(repository, Mockito.times(0)).updateStart(Mockito.anyInt(), Mockito.anyInt());
    }

    @Test
    void updateMonitorStartHappyPath() {
        // Variabili locali
        int id = 1;
        int prevStart = 8 * 60 * 60 * 1000;
        int newStart = 10 * 60 * 60 * 1000;
        int stop = 20 * 60 * 60 * 1000;
        String prevStartStr = String.valueOf(prevStart);
        String newStartStr = String.valueOf(newStart);
        String newStartLiteral = "10:00:00";
        String newStartSec = newStartStr.substring(0, newStartStr.length() - 3);

        Monitor monitor = new Monitor(id, prevStart, stop);

        // Definizione comportamento
        Mockito.doReturn(monitor).when(repository).getMonitor();
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).deactivateSystemdTimer(prevStartStr);
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).deleteSystemdTimerUnit(prevStartStr);
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).createSystemdTimerUnit(newStartStr, newStartLiteral, newStartSec);
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).timerReload();
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).activateSystemdTimer(newStartStr);
        Mockito.doReturn(1).when(repository).updateStart(id, newStart);

        // Esecuzione
        Assertions.assertEquals(Result.SUCCESS, monitorService.updateMonitorStart(newStart));

        // Verifiche
        Mockito.verify(monitorTurnOnUtils).deactivateSystemdTimer(prevStartStr);
        Mockito.verify(monitorTurnOnUtils).activateSystemdTimer(newStartStr);
        Mockito.verify(repository).updateStart(id, newStart);
    }

    @Test
    void updateMonitorStartRollbackOnStep3Error() {
        // Variabili locali
        int prevStart = 8 * 60 * 60 * 1000;
        int newStart = 9 * 60 * 60 * 1000;
        String prevStartStr = String.valueOf(prevStart);
        String newStartStr = String.valueOf(newStart);
        String newStartLiteral = "09:00:00";
        String newStartSec = newStartStr.substring(0, newStartStr.length() - 3);

        Monitor monitor = new Monitor(1, prevStart, 20 * 60 * 60 * 1000);

        Mockito.doReturn(monitor).when(repository).getMonitor();
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).deactivateSystemdTimer(prevStartStr);
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).deleteSystemdTimerUnit(prevStartStr);

        // Errore al passo 3 (creazione unità)
        Mockito.doReturn(Result.ERROR).when(monitorTurnOnUtils).createSystemdTimerUnit(newStartStr, newStartLiteral, newStartSec);

        // Mocks per il Rollback (step 1, 2,3)
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).reverseDeleteSystemdTimerUnit(prevStartStr);
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).activateSystemdTimer(prevStartStr);
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).reversSystemdTimerUnitInsert(newStartStr);
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).timerReload();

        Assertions.assertThrows(MonitorServiceException.class, () -> monitorService.updateMonitorStart(newStart));

        // Verifiche rollback eseguiti
        Mockito.verify(monitorTurnOnUtils).reverseDeleteSystemdTimerUnit(prevStartStr);
        Mockito.verify(monitorTurnOnUtils).activateSystemdTimer(prevStartStr);
        Mockito.verify(monitorTurnOnUtils).reversSystemdTimerUnitInsert(newStartStr);
    }

    @Test
    void updateMonitorStartRollbackOnDatabaseUpdateError() {
        // Variabili locali
        int id = 1;
        int prevStart = 8 * 60 * 60 * 1000;
        int newStart = 9 * 60 * 60 * 1000;
        String prevStartStr = String.valueOf(prevStart);
        String newStartStr = String.valueOf(newStart);

        Monitor monitor = new Monitor(id, prevStart, 20 * 60 * 60 * 1000);

        Mockito.doReturn(monitor).when(repository).getMonitor();
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).deactivateSystemdTimer(Mockito.anyString());
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).deleteSystemdTimerUnit(Mockito.anyString());
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).createSystemdTimerUnit(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.doReturn(Result.SUCCESS,Result.SUCCESS).when(monitorTurnOnUtils).timerReload();
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).activateSystemdTimer(Mockito.anyString());

        // Errore all'ultimo passo (salvataggio DB)
        Mockito.doThrow(new RuntimeException("DB Update Failure")).when(repository).updateStart(id, newStart);

        // Mocks per Rollback completo
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).deactivateSystemdTimer(newStartStr);
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).reversSystemdTimerUnitInsert(newStartStr);
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).reverseDeleteSystemdTimerUnit(prevStartStr);
        Mockito.doReturn(Result.SUCCESS).when(monitorTurnOnUtils).activateSystemdTimer(prevStartStr);
        Assertions.assertThrows(MonitorServiceException.class, () -> monitorService.updateMonitorStart(newStart));

        Mockito.verify(monitorTurnOnUtils).activateSystemdTimer(newStartStr);

        // Verifica che l'intero rollback sia stato tentato
        Mockito.verify(monitorTurnOnUtils).deactivateSystemdTimer(newStartStr);
        Mockito.verify(monitorTurnOnUtils).reversSystemdTimerUnitInsert(newStartStr);
        Mockito.verify(monitorTurnOnUtils).reverseDeleteSystemdTimerUnit(prevStartStr);
        Mockito.verify(monitorTurnOnUtils).activateSystemdTimer(prevStartStr);
    }

    @Test
    void updateMonitorStartCriticalErrorInRollback() {
        // Variabili locali
        int prevStart = 8 * 60 * 60 * 1000;
        int newStart = 9 * 60 * 60 * 1000;
        String prevStartStr = String.valueOf(prevStart);

        Monitor monitor = new Monitor(1, prevStart, 20 * 60 * 60 * 1000);

        Mockito.doReturn(monitor).when(repository).getMonitor();

        // Fallimento immediato al passo 1
        Mockito.doReturn(Result.ERROR).when(monitorTurnOnUtils).deactivateSystemdTimer(prevStartStr);

        // Durante il rollback del passo 1, lanciamo un'eccezione imprevista
        Mockito.doThrow(new RuntimeException("Systemd Crash")).when(monitorTurnOnUtils).activateSystemdTimer(prevStartStr);

        // Il metodo deve comunque terminare lanciando la MonitorServiceException
        Assertions.assertThrows(MonitorServiceException.class, () -> monitorService.updateMonitorStart(newStart));

        // Verifica che il tentativo di rollback sia avvenuto
        Mockito.verify(monitorTurnOnUtils).activateSystemdTimer(prevStartStr);
    }

    @Test
    void updateMonitorStartCriticalErrorInRollbackExceptionInHappyPath() {
        // Variabili locali
        int prevStart = 8 * 60 * 60 * 1000;
        int newStart = 9 * 60 * 60 * 1000;
        String prevStartStr = String.valueOf(prevStart);

        Monitor monitor = new Monitor(1, prevStart, 20 * 60 * 60 * 1000);

        Mockito.doReturn(monitor).when(repository).getMonitor();

        // Fallimento immediato al passo 1
        Mockito.doThrow(new RuntimeException("errore imprevisto")).when(monitorTurnOnUtils).deactivateSystemdTimer(prevStartStr);

        // Durante il rollback del passo 1, lanciamo un'eccezione imprevista
        Mockito.doThrow(new RuntimeException("Systemd Crash")).when(monitorTurnOnUtils).activateSystemdTimer(prevStartStr);

        // Il metodo deve comunque terminare lanciando la MonitorServiceException
        Assertions.assertThrows(MonitorServiceException.class, () -> monitorService.updateMonitorStart(newStart));

        // Verifica che il tentativo di rollback sia avvenuto
        Mockito.verify(monitorTurnOnUtils).activateSystemdTimer(prevStartStr);
    }

    @Test
    void updateMonitorStartErrorWhenStartIsGreaterThanStop() {
        // Variabili locali
        int id = 1;
        int currentStart = 8 * 60 * 60 * 1000;  // 08:00
        int currentStop = 20 * 60 * 60 * 1000;  // 20:00
        int invalidNewStart = 21 * 60 * 60 * 1000; // 21:00 (Maggiore di stop)

        Monitor monitor = new Monitor(id, currentStart, currentStop);

        // Mock del database che restituisce il monitor esistente
        Mockito.doReturn(monitor).when(repository).getMonitor();

        // Verifica che venga lanciata MonitorServiceException per validazione fallita
        MonitorServiceException exception = Assertions.assertThrows(
                MonitorServiceException.class,
                () -> monitorService.updateMonitorStart(invalidNewStart)
        );

        // Verifica opzionale del messaggio d'errore
        Assertions.assertEquals("Invalid start value", exception.getMessage());

        // Verifica che non sia stata tentata alcuna operazione sui timer o sul database
        Mockito.verify(monitorTurnOnUtils, Mockito.times(0)).deactivateSystemdTimer(Mockito.anyString());
        Mockito.verify(monitorTurnOnUtils, Mockito.times(0)).createSystemdTimerUnit(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(repository, Mockito.times(0)).updateStart(Mockito.anyInt(), Mockito.anyInt());
    }

    @Test
    void updateMonitorStartErrorWhenStartIsEqualToStop() {
        // Variabili locali
        int id = 1;
        int currentStop = 20 * 60 * 60 * 1000; // 20:00
        int invalidNewStart = 20 * 60 * 60 * 1000; // 20:00 (Uguale a stop)

        Monitor monitor = new Monitor(id, 8 * 60 * 60 * 1000, currentStop);

        Mockito.doReturn(monitor).when(repository).getMonitor();

        // Il codice usa start >= monitor.getStop(), quindi anche l'uguaglianza deve fallire
        Assertions.assertThrows(MonitorServiceException.class,
                () -> monitorService.updateMonitorStart(invalidNewStart));

        Mockito.verifyNoInteractions(monitorTurnOnUtils);
    }
}