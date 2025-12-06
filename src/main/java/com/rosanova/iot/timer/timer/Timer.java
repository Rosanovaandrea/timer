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
public class Timer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    long id;

    @Column(unique = true)
    String name;

    @Column(unique = true)
    String timerName;

    @Column(unique = true)
    LocalDateTime start;

    @Column(unique = true)
    LocalDateTime end;


}
