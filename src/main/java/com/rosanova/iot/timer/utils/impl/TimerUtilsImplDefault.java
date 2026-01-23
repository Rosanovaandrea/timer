package com.rosanova.iot.timer.utils.impl;

import com.rosanova.iot.timer.utils.TimerUtils;
import org.springframework.beans.factory.annotation.Value;

public class TimerUtilsImplDefault extends TimerUtilsImpl implements TimerUtils {

    public TimerUtilsImplDefault(@Value("${tmp.directory}" )String tmpDir, @Value("${systemd_monitor.directory}") String systemdTimerDir, @Value("${systemd.monitor.service.name}") String serviceFileName) {
        super(tmpDir, systemdTimerDir, serviceFileName,true);
    }
}
