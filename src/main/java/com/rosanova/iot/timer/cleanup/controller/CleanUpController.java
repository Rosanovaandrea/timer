package com.rosanova.iot.timer.cleanup.controller;

import com.rosanova.iot.timer.cleanup.CleanUp;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cleanup")
@RequiredArgsConstructor
public class CleanUpController {

    private final CleanUp cleanUp;

    @PostMapping("/run")
    public ResponseEntity<String> triggerManualCleanup() {
        try {
            cleanUp.cleanUpMethod();
            return ResponseEntity.ok("Operazione di pulizia avviata con successo.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Errore durante l'esecuzione della pulizia: " + e.getMessage());
        }
    }
}