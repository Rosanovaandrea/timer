package com.rosanova.iot.timer.timer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Timer {


    private long id;

    private String timerName;

    private int startTime;

    private int endTime;
}
