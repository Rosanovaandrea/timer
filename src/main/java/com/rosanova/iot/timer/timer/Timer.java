package com.rosanova.iot.timer.timer;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "timer",
        indexes = {
                @Index(name = "idx_timer_temporal", columnList = "start_time, end_time")
        }
)
public class Timer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true, nullable = false)
    private String timerName;

    @Column(name = "start_time", nullable = false,unique = true)
    private int startTime;

    @Column(name = "end_time", nullable = false,unique = true)
    private int endTime;
}
