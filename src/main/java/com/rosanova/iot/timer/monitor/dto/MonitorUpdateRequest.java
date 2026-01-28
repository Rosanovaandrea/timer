package com.rosanova.iot.timer.monitor.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class MonitorUpdateRequest {
    @Min(value = 1000, message = "Il valore deve essere almeno 1000 millisecondi")
    @Max(value = 86_400_000, message = "in valore deve essere inferiore alle 24 ore")
    private int value;
}
