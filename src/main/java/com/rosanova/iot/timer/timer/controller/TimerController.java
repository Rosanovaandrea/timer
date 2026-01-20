package com.rosanova.iot.timer.timer.controller;

import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.error.TimerServiceException;
import com.rosanova.iot.timer.timer.Timer;
import com.rosanova.iot.timer.timer.service.TimerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/timers")
@RequiredArgsConstructor
public class TimerController {

    private final TimerService timerService;

    @PostMapping
    public ResponseEntity<?> createTimer(@RequestParam String name, @RequestParam int time, @RequestParam int symphonyDuration) {
        try {
            Result result = timerService.insertTimer(name, time, symphonyDuration);
            if (result == Result.SUCCESS) {
                return ResponseEntity.status(HttpStatus.CREATED).body("Timer creato: " + name);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore logico durante l'inserimento");
        } catch (TimerServiceException e) {
            // Cattura l'eccezione specifica del tuo servizio
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Errore Servizio: " + e.getMessage());
        } catch (Exception e) {
            // Cattura errori imprevisti (es. database down)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore Generico: " + e.getMessage());
        }
    }

    @DeleteMapping()
    public ResponseEntity<?> deleteTimer(@RequestParam Long id) {
        try {
            Result result = timerService.removeTimer(id);
            if (result == Result.SUCCESS) {
                return ResponseEntity.ok("Timer " + id + " rimosso correttamente");
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Errore durante la rimozione");
        } catch (TimerServiceException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Impossibile rimuovere: " + e.getMessage());
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
