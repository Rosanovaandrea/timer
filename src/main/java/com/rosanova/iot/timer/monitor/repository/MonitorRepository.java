package com.rosanova.iot.timer.monitor.repository;

import com.rosanova.iot.timer.monitor.Monitor;

public interface MonitorRepository {
    int save(Monitor monitor);
    int count();
    Monitor getMonitor();
    int updateStart(long id, int newStart);
    int updateStop(long id, int newStop);
    int deleteById(long id);
    boolean existsMonitor();
    boolean isStartBeforeStop(long id);
    boolean isStopAfterStart(long id);
    void deleteAll();
}
