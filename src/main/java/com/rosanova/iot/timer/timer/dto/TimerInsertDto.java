package com.rosanova.iot.timer.timer.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class TimerInsertDto {

    @NotBlank(message = "Il nome non può essere vuoto")
    @Size(max = 53, message = "Il nome è troppo lungo")
    private String name;

    @NotNull(message = "il timer non può essere null")
    @Min(value = 1000, message = "Il tempo minimo è 1 secondo (1000ms)")
    @Max(value = 86400000, message = "Il tempo massimo è 24 ore")
    private Integer time;

    @NotNull(message = "La durata della sinfonia è obbligatoria")
    @Min(value = 1, message = "La durata minima della sinfonia è 1")
    @Max(value = 30, message = "La durata massima della sinfonia è 30")
    private Integer symphonyDuration;
}
