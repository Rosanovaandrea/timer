package com.rosanova.iot.timer.monitor.service.integration_test;


import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.monitor.Monitor;
import com.rosanova.iot.timer.monitor.repository.MonitorRepository;
import com.rosanova.iot.timer.monitor.service.MonitorServiceImpl;
import com.rosanova.iot.timer.utils.TimerUtils;
import com.rosanova.iot.timer.utils.impl.MonitorTimerShutdownUtilImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MonitorServiceUpdateStartIntegrationTest {

    @Autowired
    private MonitorRepository repository;

    @TempDir
    Path sharedTempDir;

    private Path tmpDirectory;
    private Path monitorDirectory;

    private MonitorServiceImpl monitorService;
    private TimerUtils monitorTurnOnUtilsSpy;
    private TimerUtils monitorTurnOffUtilsMock; // Non usato nel test dello start ma richiesto nel costruttore
    private ReentrantLock lockMock;

    @BeforeAll
    static void initDatabase(@Autowired MonitorRepository repository) {
        // Inseriamo il monitor solo se il database è vuoto
        repository.deleteAll();
        if (repository.getMonitor() == null) {
            Monitor initialMonitor = new Monitor(1L, 5000, 15000);
            repository.save(initialMonitor);
        }
    }

    @BeforeEach
    void setUp() throws IOException, InterruptedException {
        // 1. Creazione cartelle temporanee
        tmpDirectory = Files.createDirectories(sharedTempDir.resolve("tmp"));
        monitorDirectory = Files.createDirectories(sharedTempDir.resolve("systemd_monitor"));

        // 2. Configurazione dello SPY per MonitorTimerShutdownUtilImpl
        // Passiamo i path delle cartelle temporanee create
        MonitorTimerShutdownUtilImpl realUtil = new MonitorTimerShutdownUtilImpl(
                tmpDirectory.toString(),
                monitorDirectory.toString(),
                "monitor-on"
        );

        monitorTurnOnUtilsSpy = Mockito.spy(realUtil);

        // 3. Mocking metodi che interagiscono con systemctl (per non toccare il sistema ospite)
        doReturn(Result.SUCCESS).when(monitorTurnOnUtilsSpy).activateSystemdTimer(anyString());
        doReturn(Result.SUCCESS).when(monitorTurnOnUtilsSpy).deactivateSystemdTimer(anyString());
        doReturn(Result.SUCCESS).when(monitorTurnOnUtilsSpy).timerReload();

        // 4. Mocking del Lock
        lockMock = mock(ReentrantLock.class);
        // Default: il lock viene acquisito con successo
        when(lockMock.tryLock(anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(true);

        // Creazione file .timer fisico iniziale nella cartella fittizia di systemd
        Files.createFile(monitorDirectory.resolve("5000.timer"));

        // 6. Istanziazione del servizio
        monitorTurnOffUtilsMock = mock(TimerUtils.class);
        monitorService = new MonitorServiceImpl(lockMock, repository, monitorTurnOnUtilsSpy, monitorTurnOffUtilsMock);
    }

    @Test
    @Order(2)
    void testUpdateMonitorStart_Success() throws IOException, InterruptedException {
        int newStart = 7000;

        // Esecuzione
        Result result = monitorService.updateMonitorStartSynchronized(newStart);

        // Verifiche
        assertEquals(Result.SUCCESS, result);

        // Verifica DB aggiornato
        Monitor updated = repository.getMonitor();
        assertEquals(newStart, updated.getStart());

        // Verifica Filesystem: il vecchio file 5000.timer deve essere sparito, il nuovo 7000.timer deve esserci
        assertEquals(false, Files.exists(monitorDirectory.resolve("5000.timer")), "Il vecchio file dovrebbe essere eliminato");
        assertEquals(true, Files.exists(monitorDirectory.resolve("7000.timer")), "Il nuovo file dovrebbe essere creato");

        // Verifica interazioni systemd
        verify(monitorTurnOnUtilsSpy, times(1)).activateSystemdTimer("7000");
        verify(monitorTurnOnUtilsSpy, times(1)).deactivateSystemdTimer("5000");
    }

    @Test
   @Order(1)
    void testUpdateMonitorStart_LockFailure() throws InterruptedException {
        // Forza il fallimento del lock
        when(lockMock.tryLock(anyLong(), eq(TimeUnit.MILLISECONDS))).thenReturn(false);

        int newStart = 8000;
        Result result = monitorService.updateMonitorStartSynchronized(newStart);

        // Verifiche
        assertEquals(Result.ERROR, result);

        // Verifica che il DB NON sia stato toccato
        Monitor notUpdated = repository.getMonitor();
        assertEquals(5000, notUpdated.getStart());

        // Verifica che non siano stati chiamati i metodi di utilità
        verify(monitorTurnOnUtilsSpy, never()).createSystemdTimerUnit(anyString(), anyString(), anyString());
    }
}
