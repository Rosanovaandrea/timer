package com.rosanova.iot.timer.monitor.repository;


import com.rosanova.iot.timer.monitor.Monitor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MonitorRepositoryImpl implements MonitorRepository {

    private final JdbcTemplate jdbcTemplate;

    public MonitorRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Aggiungere un monitor
    public int save(Monitor monitor) {
        String sql = "INSERT INTO monitor_timer (start, stop) VALUES (?, ?)";
        return jdbcTemplate.update(sql, monitor.getStart(), monitor.getStop());
    }

    // Vedere quanti monitor ci sono
    public int count() {
        String sql = "SELECT COUNT(*) FROM monitor_timer";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    // ottenere il monitor
    public Monitor getMonitor() {
        String sql = "SELECT id, start, stop FROM monitor_timer LIMIT 1";
        List<Monitor> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Monitor m = new Monitor();
            m.setId(rs.getLong("id"));
            m.setStart(rs.getInt("start"));
            m.setStop(rs.getInt("stop"));
            return m;
        });

        return results.isEmpty() ? null : results.get(0);
    }

    // Modificare lo start
    public int updateStart(long id, int newStart) {
        String sql = "UPDATE monitor_timer SET start = ? WHERE id = ?";
        return jdbcTemplate.update(sql, newStart, id);
    }

    // Modificare lo stop
    public int updateStop(long id, int newStop) {
        String sql = "UPDATE monitor_timer SET stop = ? WHERE id = ?";
        return jdbcTemplate.update(sql, newStop, id);
    }

    // Cancellazione per ID
    public int deleteById(long id) {
        String sql = "DELETE FROM monitor_timer WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }

    public boolean existsMonitor() {
        String sql = "SELECT COUNT(*) FROM monitor_timer";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null && count > 0;
    }

    // Metodo per vedere se uno start è prima di uno stop (Logica DB)
    public boolean isStartBeforeStop(long id) {
        String sql = "SELECT (start < stop) FROM monitor_timer WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, Boolean.class, id);
    }

    // Metodo per vedere se uno stop è dopo uno start (Logica DB)
    public boolean isStopAfterStart(long id) {
        String sql = "SELECT (stop > start) FROM monitor_timer WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, Boolean.class, id);
    }

    // Metodo per vedere se uno stop è dopo uno start (Logica DB)
    public void deleteAll() {
        String sql = "DELETE FROM monitor_timer";
         jdbcTemplate.update(sql);
    }
}