package com.rosanova.iot.timer.utils.impl;

import com.rosanova.iot.timer.utils.TimerUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Qualifier("monitorOn")
public class MonitorTimerUtilImpl extends TimerUtilsImpl implements TimerUtils {

    public MonitorTimerUtilImpl(@Value("${tmp.directory}" )String tmpDir,@Value("${systemd.monitor.directory}" )String systemdTimerDir, @Value("${systemd.monitor.service.on.name}") String serviceFileName) {
        super(tmpDir, systemdTimerDir, serviceFileName,false);
    }
}
