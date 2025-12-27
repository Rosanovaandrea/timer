package com.rosanova.iot.timer.utils;

import com.rosanova.iot.timer.Result;

public interface TimerUtils {

    Result createSystemdTimerUnit(String timerBaseName, String onCalendar);
    Result deleteSystemdTimerUnit(String timerBaseName);
    Result reversSystemdTimerUnitInsert(String timerBaseName);
    Result activateSystemdTimer(String timerBaseName);
    Result deactivateSystemdTimer(String timerBaseName);
}
