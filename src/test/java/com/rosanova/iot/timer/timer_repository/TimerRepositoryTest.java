package com.rosanova.iot.timer.timer_repository;

import com.rosanova.iot.timer.timer.Timer;
import com.rosanova.iot.timer.timer.dto.CheckTimerInsertValidity;
import com.rosanova.iot.timer.timer.repository.impl.TimerRepositoryImpl;
import com.rosanova.iot.timer.utils.impl.HashMapInt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@ActiveProfiles("test")
@Import(TimerRepositoryImpl.class) // Carica il tuo Repository
public class TimerRepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TimerRepositoryImpl timerRepository;

    // RISOLUZIONE ERRORE: Configura un CacheManager per i test
    @TestConfiguration
    static class CacheTestConfig {
        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("timers");
        }
    }

    @BeforeEach
    void setUp() {
        // Pulisce il database prima di ogni test
        jdbcTemplate.execute("DELETE FROM timer");
    }

    @Test
    void testInsertAndFindAll() {
        Timer timer = new Timer();
        timer.setTimerName("Test Timer");
        timer.setStartTime(100);
        timer.setEndTime(200);

        timerRepository.insert(timer);

        List<Timer> timers = timerRepository.findAll();
        assertFalse(timers.isEmpty());
        assertEquals("Test Timer", timers.get(0).getTimerName());
    }


    @Test
    @DisplayName("Dovrebbe inserire un timer e trovarlo per ID")
    void testInsertAndFindById() {
        Timer timer = new Timer(0, "Luce Giardino", 1000, 2000);
        timerRepository.insert(timer);

        // Recuperiamo l'ID generato (essendo l'unico sarà 1 con H2)
        Integer generatedId = jdbcTemplate.queryForObject("SELECT id FROM timer LIMIT 1", Integer.class);

        Timer found = timerRepository.findById(generatedId);

        assertNotNull(found);
        assertEquals("Luce Giardino", found.getTimerName());
    }

    @Test
    @DisplayName("Dovrebbe restituire tutti i timer inseriti")
    void testFindAll() {
        timerRepository.insert(new Timer(0, "T1", 10, 20));
        timerRepository.insert(new Timer(0, "T2", 30, 40));

        List<Timer> list = timerRepository.findAll();
        assertEquals(2, list.size());
        assertEquals("T1", list.get(0).getTimerName());
        assertEquals("T2", list.get(1).getTimerName());

    }

    @Test
    @DisplayName("Dovrebbe eliminare un timer correttamente")
    void testDeleteById() {
        timerRepository.insert(new Timer(0, "Da Eliminare", 50, 60));
        Integer id = jdbcTemplate.queryForObject("SELECT id FROM timer WHERE timer_name = 'Da Eliminare'", Integer.class);

        timerRepository.deleteById(id);

        Timer found = timerRepository.findById(id);
        assertNull(found);
    }

    @Test
    @DisplayName("Dovrebbe calcolare correttamente sovrapposizioni e totale")
    void testCountOverlapsAndMaxTimers() {
        // Inseriamo un timer dalle 10:00 alle 12:00 (espresse in secondi: 36000 - 43200)
        timerRepository.insert(new Timer(0, "Timer Esistente", 36000, 43200));

        // Caso A: Sovrapposizione parziale (testiamo 40000 - 45000)
        CheckTimerInsertValidity overlap = timerRepository.countOverlapsAndMaxTimers(40000, 45000);
        assertEquals(1, overlap.getTotal(), "Il totale dei timer dovrebbe essere 1");
        assertEquals(1, overlap.getOverlaps(), "Dovrebbe esserci 1 sovrapposizione");

        // Caso B: Nessuna sovrapposizione (testiamo 50000 - 60000)
        CheckTimerInsertValidity noOverlap = timerRepository.countOverlapsAndMaxTimers(50000, 60000);
        assertEquals(0, noOverlap.getOverlaps(), "Non dovrebbero esserci sovrapposizioni");
    }

    @Test
    @DisplayName("Dovrebbe aggiungere correttamente i tempi mediani alla HashMapInt")
    void testAddEndTimesToMap() {
        timerRepository.insert(new Timer(0, "T1", 0, 150_000));
        timerRepository.insert(new Timer(0, "T2", 200_000, 450_000));

        HashMapInt customMap = new HashMapInt();
        timerRepository.addEndTimesToMap(customMap);

        // Verifica basata sull'implementazione della tua HashMapInt
        // Se HashMapInt stampa i valori o ha un metodo size/get:
        assertNotNull(customMap);
        assertTrue(customMap.search(130_000));
        assertTrue(customMap.search(220_000));
        // Nota: Assicurati che HashMapInt funzioni come previsto dal test
    }
}
