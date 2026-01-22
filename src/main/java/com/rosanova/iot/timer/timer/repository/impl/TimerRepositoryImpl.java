package com.rosanova.iot.timer.timer.repository.impl;

import com.rosanova.iot.timer.timer.Timer;
import com.rosanova.iot.timer.timer.dto.CheckTimerInsertValidity;
import com.rosanova.iot.timer.timer.repository.TimerRepository;
import com.rosanova.iot.timer.utils.impl.HashMapInt;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class TimerRepositoryImpl implements TimerRepository {

    private final JdbcTemplate jdbcTemplate;

    public TimerRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @CacheEvict(value = "timers", allEntries = true)
    public void insert(Timer timer) {
        String sql = "INSERT INTO timer (timer_name, start_time, end_time) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, timer.getTimerName(), timer.getStartTime(), timer.getEndTime());
    }

    /**
     * Elimina un timer tramite ID primitivo.
     * Anche qui svuotiamo la cache.
     */
    @CacheEvict(value = "timers", allEntries = true)
    public void deleteById(long id) {
        String sql = "DELETE FROM timer WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    // --- OPERAZIONI DI LETTURA (Utilizza la cache) ---

    /**
     * Ritorna la lista di tutti i timer.
     * Se la cache "timers" è piena, il metodo non viene nemmeno eseguito.
     */
    @Cacheable(value = "timers")
    public List<Timer> findAll() {
        String sql = "SELECT * FROM timer";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRowToTimer(rs));
    }

    public Timer findById(long id) {
        String sql = "SELECT * FROM timer WHERE id = ?";
        List<Timer> results = jdbcTemplate.query(sql, (rs, rowNum) -> mapRowToTimer(rs), id);
        return results.isEmpty() ? null : results.get(0);
    }
    /**
     * Ritorna un CheckTimerInsertValidity per il conteggio delle sovrapposizioni e il numero massimo di timers
     */
    public CheckTimerInsertValidity countOverlapsAndMaxTimers(int startToCheck, int endToCheck) {
        CheckTimerInsertValidity validity = new CheckTimerInsertValidity();
        String sql = "SELECT COUNT(*) AS total_timers, COUNT(CASE WHEN ? <= end_time AND ? >= start_time THEN 1 END) AS overlaps FROM timer";
        return jdbcTemplate.query(sql, (rs) -> {
            if (rs.next()) {
                validity.setTotal(rs.getInt("total_timers")); // Usa l'underscore come nel SQL
                validity.setOverlaps(rs.getInt("overlaps"));
            }
            return validity;
            }, startToCheck, endToCheck);

    }



    /**
     * Preleva i dati e li inserisce in una HashMap esistente tramite ResultSet
     */
    public void addEndTimesToMap(HashMapInt targetMap) {
        String sql = "SELECT id, start_time, end_time FROM timer";
        jdbcTemplate.query(sql, rs -> {

            int start = rs.getInt("start_time");
            int end = rs.getInt("end_time");
            int median = start == 0 ? end - 20_000 : start + 20_000;
            targetMap.add(median);
        });
    }

    // Helper per mappare l'oggetto intero
    private Timer mapRowToTimer(ResultSet rs) throws SQLException {
        Timer t = new Timer();
        t.setId(rs.getLong("id"));
        t.setTimerName(rs.getString("timer_name"));
        t.setStartTime(rs.getInt("start_time"));
        t.setEndTime(rs.getInt("end_time"));
        return t;
    }
}
