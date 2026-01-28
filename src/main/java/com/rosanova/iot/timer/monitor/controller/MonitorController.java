package com.rosanova.iot.timer.monitor.controller;


import com.rosanova.iot.timer.monitor.dto.MonitorUpdateRequest;
import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.monitor.Monitor;
import com.rosanova.iot.timer.monitor.service.MonitorServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin("*")
@RestController
@RequestMapping("/authenticated/api/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorServiceImpl monitorService;

    @PutMapping("/start")
    public ResponseEntity<?> updateStart(@Valid @RequestBody MonitorUpdateRequest request) {



        // Chiamata al metodo sincronizzato
        Result result = monitorService.updateMonitorStartSynchronized(request.getValue());

        return buildServiceResponse(result, "Start monitor aggiornato con successo", "Errore nell'aggiornamento dello start (Lock occupato o errore di sistema)");
    }

    @PutMapping("/stop")
    public ResponseEntity<?> updateStop(@Valid @RequestBody MonitorUpdateRequest request) {



        // Chiamata al metodo sincronizzato
        Result result = monitorService.updateMonitorStopSynchronized(request.getValue());

        return buildServiceResponse(result, "Stop monitor aggiornato con successo", "Errore nell'aggiornamento dello stop (Lock occupato o errore di sistema)");
    }

    @GetMapping("/monitor")
    public ResponseEntity<Monitor> getMonitor() {
        return ResponseEntity.ok( monitorService.getMonitor());
    }

    // Helper per costruire la risposta basata sul Result del Service
    private ResponseEntity<String> buildServiceResponse(Result result, String successMsg, String errorMsg) {
        if (result == Result.SUCCESS) {
            return ResponseEntity.ok(successMsg);
        } else {
            // Ritorna 409 Conflict o 500 a seconda della gravità (qui usiamo 500 per ERROR generico)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMsg);
        }
    }

}
