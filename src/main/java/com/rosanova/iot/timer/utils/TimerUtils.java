package com.rosanova.iot.timer.utils;

import com.rosanova.iot.timer.error.Result;

public interface TimerUtils {

    Result createSystemdTimerUnit(String timerBaseName, String onCalendar, String parameter) ;
    Result deleteSystemdTimerUnit(String timerBaseName);
    Result reversSystemdTimerUnitInsert(String timerBaseName);
    Result timerReload();
    Result activateSystemdTimer(String timerBaseName);
    Result deactivateSystemdTimer(String timerBaseName);
    Result reverseDeleteSystemdTimerUnit(String timerBaseName);
}
