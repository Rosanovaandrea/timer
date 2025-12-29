package com.rosanova.iot.timer.timer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.NotNull;

@AllArgsConstructor
@Setter
@Getter
@NoArgsConstructor
public class TimerInsertDto {

    @NotNull
    String timerName;

    @NotNull
    @Min(value = 420000)
    @Max(value = 86280000)
    Integer timer;


}
