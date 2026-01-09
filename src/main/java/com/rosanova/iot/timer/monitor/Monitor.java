package com.rosanova.iot.timer.monitor;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "monitor_timer")
public class Monitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @Column( unique = true, nullable = false)
    int start;

    @Column(nullable = false)
    int stop;
}
