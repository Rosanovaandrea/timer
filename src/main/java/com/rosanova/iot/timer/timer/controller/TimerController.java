package com.rosanova.iot.timer.timer.controller;

import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.timer.Timer;
import com.rosanova.iot.timer.timer.dto.TimerInsertDto;
import com.rosanova.iot.timer.timer.service.TimerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/authenticated/api/v1/timers")
@RequiredArgsConstructor
@CrossOrigin("*")
@RegisterReflectionForBinding(Timer.class)
public class TimerController {

    private final TimerService timerService;

    @PostMapping
    public ResponseEntity<?> createTimer(@RequestBody @Valid TimerInsertDto timer) {

            Result result = timerService.insertTimerSynchronized(timer.getName(), timer.getTime(), timer.getSymphonyDuration());
            if (result == Result.SUCCESS) {
                return ResponseEntity.status(HttpStatus.CREATED).body("Timer creato: " + timer.getName());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore logico durante l'inserimento");

    }

    @DeleteMapping()
    public ResponseEntity<?> deleteTimer(@RequestParam Long id) {

            Result result = timerService.removeTimerSynchronized(id);
            if (result == Result.SUCCESS) {
                return ResponseEntity.ok("Timer " + id + " rimosso correttamente");
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore durante la rimozione");

    }

    @GetMapping
    public ResponseEntity<?> getAllTimers() {
        try {
            List<Timer> timers = timerService.getAllTimers();
            return ResponseEntity.ok(timers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
