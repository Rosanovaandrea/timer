package com.rosanova.iot.timer.timer.repository;

import com.rosanova.iot.timer.timer.Timer;
import com.rosanova.iot.timer.utils.HashMapInt;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class TimerRepository {

    private final JdbcTemplate jdbcTemplate;

    public TimerRepository(JdbcTemplate jdbcTemplate) {
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

    @Cacheable(value = "timers", key = "#id")
    public Timer findById(long id) {
        String sql = "SELECT * FROM timer WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapRowToTimer(rs), id);
    }
    /**
     * Ritorna un valore primitivo (int) per il conteggio delle sovrapposizioni
     */
    public int countOverlaps(int startToCheck, int endToCheck) {
        String sql = "SELECT COUNT(*) FROM timer WHERE ? <= end_time AND ? >= start_time";
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, startToCheck, endToCheck);
        return (result != null) ? result : 0;
    }

    /**
     * Ritorna un boolean primitivo basato sulla sovrapposizione
     */
    public boolean existsOverlap(int startToCheck, int endToCheck) {
        return countOverlaps(startToCheck, endToCheck) > 0;
    }

    /**
     * Preleva i dati e li inserisce in una HashMap esistente tramite ResultSet
     */
    public void addEndTimesToMap(HashMapInt targetMap) {
        String sql = "SELECT id, end_time FROM timer";
        jdbcTemplate.query(sql, rs -> {
            // Estrazione primitiva dal ResultSet e aggiunta alla mappa
            int endTime = rs.getInt("end_time");
            targetMap.add(endTime);
        });
    }

    // Helper per mappare l'oggetto intero
    private Timer mapRowToTimer(ResultSet rs) throws SQLException {
        Timer t = new Timer();
        t.setId(rs.getInt("id"));
        t.setTimerName(rs.getString("timer_name"));
        t.setStartTime(rs.getInt("start_time"));
        t.setEndTime(rs.getInt("end_time"));
        return t;
    }
}
