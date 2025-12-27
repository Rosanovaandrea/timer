package com.rosanova.iot.timer.timer.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CheckTimerInsertValidity {
    int total;
    int overlaps;
}
