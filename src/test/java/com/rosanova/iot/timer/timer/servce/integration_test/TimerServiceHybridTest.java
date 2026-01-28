package com.rosanova.iot.timer.timer.servce.integration_test;

import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.timer.Timer;
import com.rosanova.iot.timer.timer.repository.TimerRepository;
import com.rosanova.iot.timer.timer.service.TimerService;
import com.rosanova.iot.timer.timer.service.impl.TimerServiceImpl;
import com.rosanova.iot.timer.utils.TimerUtils;
import com.rosanova.iot.timer.utils.impl.TimerUtilsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest
@ActiveProfiles("test")
class TimerServiceHybridTest {

    @TempDir
    static Path tempFolder;

    private TimerServiceImpl timerService;

    @Autowired
    private TimerRepository repository;

    private TimerUtils timerUtilsSpy; // Questo è lo spy creato nella configurazione

    private ReentrantLock sharedLock;

    @TestConfiguration
    static class HybridConfig {

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("timers");
        }
    }

    @BeforeEach
    void setupStubs() throws IOException {
        // Reset delle interazioni precedenti
        Path tmp = tempFolder.resolve("tmp");
        Path system = tempFolder.resolve("systemd");
        Files.createDirectories(tmp);
        Files.createDirectories(system);

        // Istanza reale configurata con i path temporanei di JUnit
        timerUtilsSpy = Mockito.spy(new TimerUtilsImpl(
                tmp.toString(),
                system.toString(),
                "alarm.service",
                false
        ));
        sharedLock = Mockito.mock(ReentrantLock.class);

        timerService = Mockito.spy(new TimerServiceImpl(repository, timerUtilsSpy,sharedLock));



        // STUB dei metodi critici: devono ritornare SUCCESS per far procedere il Service
        // Usiamo doReturn per evitare l'esecuzione reale del codice dentro lo Spy
        doReturn(Result.SUCCESS).when(timerUtilsSpy).timerReload();
        doReturn(Result.SUCCESS).when(timerUtilsSpy).activateSystemdTimer(anyString());
        doReturn(Result.SUCCESS).when(timerUtilsSpy).deactivateSystemdTimer(anyString());
    }

    @Test
    @DisplayName("Il service deve ritornare SUCCESS quando tutti gli step dello spy sono stubbati positivamente")
    void testInsertTimer_ShouldReturnSuccess() {
        // Arrange
        String name = "Test_Success";
        int time = 800000; // Questo genererà il nome file "800000"

        // Act
        Result result = timerService.insertTimer(name, time, 30);

        // Assert
        assertEquals(Result.SUCCESS, result, "Il service dovrebbe ritornare SUCCESS");

        // Verifica DB
        assertTrue(repository.findAll().stream().anyMatch(t -> t.getTimerName().equals(name)));

        // Verifica Filesystem (il metodo createSystemdTimerUnit NON è stubbato, quindi viene eseguito davvero)
        Path expectedFile = tempFolder.resolve("systemd").resolve(time + ".timer");
        assertTrue(Files.exists(expectedFile), "Il file deve essere stato creato fisicamente");
    }

    @Test
    @DisplayName("Rimozione: deve ritornare SUCCESS e pulire DB e Filesystem")
    void testRemoveTimer_ShouldReturnSuccess() throws IOException {
        // Arrange
        Timer t = new Timer(0, "ToRemove", 10000, 50000);
        repository.insert(t);
        String uniqueName = "ToRemove";


        // Recuperiamo il timer appena inserito per avere l'ID reale generato dal DB
        Timer savedTimer = repository.findAll().stream()
                .filter(timer -> timer.getTimerName().equals(uniqueName))
                .findFirst()
                .orElseThrow();

        long id = savedTimer.getId();

        // Creiamo il file reale per simulare la presenza dell'unit
        Path file = tempFolder.resolve("systemd").resolve("30000.timer");
        Files.writeString(file, "unit content");

        // Act
        Result result = timerService.removeTimer(id);

        // Assert
        assertEquals(Result.SUCCESS, result);
        assertNull(repository.findById(id), "Il record DB deve essere cancellato");
        assertFalse(Files.exists(file), "Il file fisico deve essere stato rimosso");
    }

    @Test
    @DisplayName("insertTimerSynchronized: Successo quando il lock è disponibile")
    void testInsertTimerSynchronized_LockAcquired() throws InterruptedException {
        // Arrange
        String name = "Sync_Success";
        int time = 900000;
        // Simuliamo l'acquisizione del lock con successo
        when(sharedLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Act
        Result result = timerService.insertTimerSynchronized(name, time, 30);

        // Assert
        assertEquals(Result.SUCCESS, result);

        // Verifica che la logica di integrazione sia avvenuta (DB e Filesystem)
        assertTrue(repository.findAll().stream().anyMatch(t -> t.getTimerName().equals(name)));
        Path expectedFile = tempFolder.resolve("systemd").resolve(time + ".timer");
        assertTrue(Files.exists(expectedFile));

        // Verifica il ciclo di vita del lock
        verify(sharedLock).tryLock(100L, TimeUnit.MILLISECONDS);
        verify(sharedLock).unlock();
    }

    @Test
    @DisplayName("insertTimerSynchronized: Ritorna ERROR se il lock non viene acquisito")
    void testInsertTimerSynchronized_LockBusy() throws InterruptedException {
        // Arrange
        String name = "Sync_Fail";
        int time = 950000;
        // Simuliamo che il lock sia occupato da un altro thread
        when(sharedLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(false);

        // Act
        Result result = timerService.insertTimerSynchronized(name, time, 30);

        // Assert
        assertEquals(Result.ERROR, result);

        // Verifica che la logica interna NON sia stata eseguita
        // 1. Nessun record nel DB
        assertFalse(repository.findAll().stream().anyMatch(t -> t.getTimerName().equals(name)));
        // 2. Il metodo insertTimer (core) non deve essere stato chiamato
        verify(timerService, never()).insertTimer(anyString(), anyInt(), anyInt());
        // 3. Non deve sbloccare un lock mai acquisito
        verify(sharedLock, never()).unlock();
    }

    @Test
    @DisplayName("removeTimerSynchronized: Successo e pulizia completa")
    void testRemoveTimerSynchronized_Success() throws InterruptedException, IOException {
        // Arrange
        int timeToDelete = 30_000;
        Timer t = new Timer(0, "ToRemoveSync", 0, timeToDelete);
        repository.insert(t);

        Timer savedTimer = repository.findAll().stream()
                .filter(timer -> timer.getTimerName().equals("ToRemoveSync"))
                .findFirst()
                .orElseThrow();

        long id = savedTimer.getId();

        Path file = tempFolder.resolve("systemd").resolve( "10000.timer");
        Files.writeString(file, "content");

        when(sharedLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Act
        Result result = timerService.removeTimerSynchronized(id);

        // Assert
        assertEquals(Result.SUCCESS, result);
        assertNull(repository.findById(id));
        assertFalse(Files.exists(file));

        verify(sharedLock).unlock();
    }

    @Test
    @DisplayName("removeTimerSynchronized: Fallimento se il lock è occupato")
    void testRemoveTimerSynchronized_LockBusy() throws InterruptedException {
        // Arrange
        when(sharedLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(false);

        // Act
        Result result = timerService.removeTimerSynchronized(1L);

        // Assert
        assertEquals(Result.ERROR, result);
        verify(timerService, never()).removeTimer(anyLong());
        verify(sharedLock, never()).unlock();
    }
}