package com.rosanova.iot.timer.timer.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckTimerInsertValidity {
    int total;
    int overlaps;
}
