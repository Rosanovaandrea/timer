package com.rosanova.iot.timer.utils.impl;

import com.rosanova.iot.timer.utils.TimerUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Qualifier("timerDefault")
public class MonitorTimerUtilImpl extends TimerUtilsImpl implements TimerUtils {

    public MonitorTimerUtilImpl(@Value("${tmp.directory}" )String tmpDir,@Value("${systemd.directory}" )String systemdTimerDir, @Value("${systemd.monitor.service.name}") String serviceFileName) {
        super(tmpDir, systemdTimerDir, serviceFileName,false);
    }
}
