package com.rosanova.iot.timer.cleanup;


import com.rosanova.iot.timer.monitor.Monitor;
import com.rosanova.iot.timer.monitor.repository.MonitorRepository;
import com.rosanova.iot.timer.timer.Timer;
import com.rosanova.iot.timer.timer.repository.TimerRepository;
import com.rosanova.iot.timer.utils.TimerUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class CleanUpIntegrationTest {

    @Autowired private TimerRepository timerRepository;
    @Autowired private MonitorRepository monitorRepository;
    @Autowired private ReentrantLock ioLock;
    @Autowired private ExecutorService executorService;

    // Questo "disabilita" il bean reale creato da Spring ed evita l'errore delle String
    private CleanUp cleanUpMock;

    private TimerUtils timerUtils;

    private CleanUp cleanUp; // Questa è l'istanza REALE che testeremo

    @TempDir
    Path rootDir;

    private Path timerPath;
    private Path monitorPath;
    private Path tmpPath;

    @BeforeEach
    void setup() throws IOException {
        // Creazione cartelle dinamiche
        timerPath = Files.createDirectory(rootDir.resolve("timer"));
        monitorPath = Files.createDirectory(rootDir.resolve("monitor"));
        tmpPath = Files.createDirectory(rootDir.resolve("tmp"));
        timerUtils = Mockito.mock(TimerUtils.class);

        // Inizializzazione MANUALE dell'oggetto da testare
        cleanUp = new CleanUp(
                ioLock,
                monitorRepository,
                timerRepository,
                tmpPath.toString(),
                timerPath.toString(),
                monitorPath.toString(),
                executorService,
                timerUtils
        );


        // monitorRepository.deleteAll(); // Se necessario
    }



    @Test
    void cleanUpMethod_Integration_HappyPath() throws IOException, InterruptedException {
        Random random = new Random();

        // --- PREPARA DATI TIMER (20 validi + 10 spazzatura) ---
        for (int i = 0; i < 20; i++) {
            int timeId = 1000 + i;
            // Salva nel DB: endTime deve corrispondere al nome file per la logica HashMapInt
            Timer t = new Timer(random.nextLong(), "Timer-" + i, timeId - 20000, timeId + 20000);
            timerRepository.insert(t);
            // Crea file corrispondente
            Files.createFile(timerPath.resolve(timeId + ".timer"));
        }
        // Aggiungi file spazzatura nella cartella timer (non presenti nel DB)
        Files.createFile(timerPath.resolve("9999.timer")); // Valido come formato, ma non nel DB
        Files.createFile(timerPath.resolve("garbage.txt")); // Estensione errata
        Files.createDirectory(timerPath.resolve("subfolder"));// Una cartella (stato inconsistente)



        // --- PREPARA DATI MONITOR (2 file) ---
        int startId = 5000;
        int stopId = 6000;
        Monitor m = new Monitor(1L, startId, stopId);
        monitorRepository.save(m);

        Files.createFile(monitorPath.resolve(startId + ".timer"));
        Files.createFile(monitorPath.resolve(stopId + ".timer"));
        Files.createFile(monitorPath.resolve("7777.timer")); // Da eliminare

        // --- PREPARA DATI TMP (Tutto deve essere eliminato) ---
        Files.createFile(tmpPath.resolve("8888.timer"));
        Files.createFile(tmpPath.resolve("old_cache.timer")); // Nome non numerico: loggherà errore ma andrà avanti

        // --- EXECUTE ---
        cleanUp.cleanUpMethod();

        // --- VERIFY TIMER DIR ---
        for (int i = 0; i < 20; i++) {
            assertTrue(Files.exists(timerPath.resolve((1000 + i) + ".timer")), "Il timer valido " + i + " dovrebbe esistere");
        }
        assertFalse(Files.exists(timerPath.resolve("9999.timer")), "Il file non a DB dovrebbe essere rimosso");
        assertTrue(Files.exists(timerPath.resolve("garbage.txt")), "File con estensione diversa non dovrebbero essere toccati dal parsing numerico");
        assertTrue(Files.exists(timerPath.resolve("subfolder")), "Le directory non dovrebbero essere rimosse dal Files.delete()");

        // --- VERIFY MONITOR DIR ---
        assertTrue(Files.exists(monitorPath.resolve(startId + ".timer")));
        assertTrue(Files.exists(monitorPath.resolve(stopId + ".timer")));
        assertFalse(Files.exists(monitorPath.resolve("7777.timer")));

        // --- VERIFY TMP DIR ---
        assertFalse(Files.exists(tmpPath.resolve("8888.timer")), "La cartella TMP deve essere svuotata dai .timer");

        // --- VERIFY SYSTEM SYNC ---
        verify(timerUtils).timerReload();
    }
}
