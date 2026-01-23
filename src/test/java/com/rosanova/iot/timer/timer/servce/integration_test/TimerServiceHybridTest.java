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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest
@ActiveProfiles("test")
class TimerServiceHybridTest {

    @TempDir
    static Path tempFolder;

    @Autowired
    private TimerService timerService;

    @Autowired
    private TimerRepository repository;

    @Autowired
    private TimerUtils timerUtilsSpy; // Questo è lo spy creato nella configurazione

    @TestConfiguration
    static class HybridConfig {
        @Bean
        @Primary
        public TimerUtils timerUtils() throws IOException {
            Path tmp = tempFolder.resolve("tmp");
            Path system = tempFolder.resolve("systemd");
            Files.createDirectories(tmp);
            Files.createDirectories(system);

            // Istanza reale configurata con i path temporanei di JUnit
            TimerUtilsImpl realImpl = new TimerUtilsImpl(
                    tmp.toString(),
                    system.toString(),
                    "alarm.service",
                    false
            );
            return spy(realImpl);
        }

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("timers");
        }


        @Bean
        public TimerService timerService(TimerRepository repository, TimerUtils timerUtils) {
            return new TimerServiceImpl(repository, timerUtils);
        }
    }

    @BeforeEach
    void setupStubs() {
        // Reset delle interazioni precedenti
        reset(timerUtilsSpy);

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
        Timer t = new Timer(0, "ToRemove", 100, 500);
        repository.insert(t);
        long id = repository.findAll().get(0).getId();

        // Creiamo il file reale per simulare la presenza dell'unit
        Path file = tempFolder.resolve("systemd").resolve("500.timer");
        Files.writeString(file, "unit content");

        // Act
        Result result = timerService.removeTimer(id);

        // Assert
        assertEquals(Result.SUCCESS, result);
        assertNull(repository.findById(id), "Il record DB deve essere cancellato");
        assertFalse(Files.exists(file), "Il file fisico deve essere stato rimosso");
    }
}