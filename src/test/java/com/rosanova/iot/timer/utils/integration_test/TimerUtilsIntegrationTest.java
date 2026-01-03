package com.rosanova.iot.timer.utils.integration_test;

import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.utils.impl.TimerUtilsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TimerUtilsIntegrationTest {

    private TimerUtilsImpl timerUtils;

    @TempDir
    Path tempFolder; // Questa sarà la nostra root temporanea

    private Path tmpDir;
    private Path systemDir;
    private final String SERVICE_NAME = "alarm.service";

    @BeforeEach
    void setUp() throws IOException {
        // Creiamo sottocartelle nella cartella temporanea di JUnit
        tmpDir = tempFolder.resolve("tmp");
        systemDir = tempFolder.resolve("systemd");

        Files.createDirectories(tmpDir);
        Files.createDirectories(systemDir);

        // Inizializziamo l'impl reale con i percorsi temporanei
        timerUtils = new TimerUtilsImpl(
                tmpDir.toString(),
                systemDir.toString(),
                SERVICE_NAME
        );
    }

    @Test
    void testFullTimerCreationFlow() throws IOException {
        String timerName = "test-unit";
        String onCalendar = "12:00:00";

        // Esecuzione del metodo reale
        Result result = timerUtils.createSystemdTimerUnit(timerName, onCalendar);

        // 1. Verifica il risultato
        assertEquals(Result.SUCCESS, result);

        // 2. Verifica che il file sia stato rimosso dalla cartella temporanea (dopo il move)
        Path expectedTmpFile = tmpDir.resolve(timerName + ".timer");
        assertFalse(Files.exists(expectedTmpFile), "Il file temporaneo dovrebbe essere stato spostato");

        // 3. Verifica che il file esista nella cartella di destinazione
        Path expectedFinalFile = systemDir.resolve(timerName + ".timer");
        assertTrue(Files.exists(expectedFinalFile), "Il file unit dovrebbe esistere nella destinazione");

        // 4. Verifica il contenuto del file generato
        String content = Files.readString(expectedFinalFile);
        assertTrue(content.contains("OnCalendar= *-*-* 12:00:00"));
        assertTrue(content.contains("Description=Custom Timer for " + SERVICE_NAME));
    }

    @Test
    @DisplayName("Idempotenza: moveTimer deve ritornare SUCCESS se il file sorgente non esiste")
    void testMoveTimerIdempotency() {
        Path source = tmpDir.resolve("non-existent.timer");
        Path target = systemDir.resolve("target.timer");

        // Pre-condizione: il file non esiste
        assertFalse(Files.exists(source));

        // Esecuzione
        Result result = timerUtils.moveTimer(source, target);

        // Verifica idempotenza
        assertEquals(Result.SUCCESS, result, "Deve tornare SUCCESS anche se il file non c'è");

        // Verifica post-interazione: il file sorgente NON deve esistere
        assertFalse(Files.exists(source), "Post-condizione: il file sorgente non deve esistere");
    }

    @Test
    void testWriteTimerOverwritesExisting() throws IOException {
        Path file = tmpDir.resolve("overwrite.timer");
        Files.writeString(file, "vecchio contenuto");

        // Esecuzione scrittura
        Result result = timerUtils.writeTimer(file, "nuovo contenuto");

        assertEquals(Result.SUCCESS, result);
        assertEquals("nuovo contenuto", Files.readString(file));
    }
}