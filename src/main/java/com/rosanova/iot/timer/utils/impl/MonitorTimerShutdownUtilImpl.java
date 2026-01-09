package com.rosanova.iot.timer.utils.impl;

import com.rosanova.iot.timer.utils.TimerUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Qualifier("monitorTimerShutdown")
public class MonitorTimerShutdownUtilImpl extends TimerUtilsImpl implements TimerUtils {

    public MonitorTimerShutdownUtilImpl(@Value("${tmp.directory}" )String tmpDir, @Value("${systemd_monitor.directory}") String systemdTimerDir, @Value("${systemd.monitor_shutdown.service.name}") String serviceFileName) {
        super(tmpDir, systemdTimerDir, serviceFileName);
    }
}
