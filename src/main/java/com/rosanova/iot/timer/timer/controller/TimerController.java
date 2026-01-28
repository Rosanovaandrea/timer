package com.rosanova.iot.timer.timer.controller;

import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.timer.Timer;
import com.rosanova.iot.timer.timer.dto.TimerInsertDto;
import com.rosanova.iot.timer.timer.service.TimerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/timers")
@RequiredArgsConstructor
@CrossOrigin("*")
public class TimerController {

    private final TimerService timerService;

    @PostMapping
    public ResponseEntity<?> createTimer(@RequestBody @Valid TimerInsertDto timer) {
        try {
            Result result = timerService.insertTimerSynchronized(timer.getName(), timer.getTime(), timer.getSymphonyDuration());
            if (result == Result.SUCCESS) {
                return ResponseEntity.status(HttpStatus.CREATED).body("Timer creato: " + timer.getName());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore logico durante l'inserimento");
        } catch (Exception e) {
            // Cattura errori imprevisti (es. database down)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore Generico: " + e.getMessage());
        }
    }

    @DeleteMapping()
    public ResponseEntity<?> deleteTimer(@RequestParam Long id) {
        try {
            Result result = timerService.removeTimerSynchronized(id);
            if (result == Result.SUCCESS) {
                return ResponseEntity.ok("Timer " + id + " rimosso correttamente");
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore durante la rimozione");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllTimers(@RequestParam(defaultValue = "0") int start) {
        try {
            List<Timer> timers = timerService.getAllTimers(start);
            return ResponseEntity.ok(timers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
