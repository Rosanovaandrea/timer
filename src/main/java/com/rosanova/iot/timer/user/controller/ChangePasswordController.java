package com.rosanova.iot.timer.user.controller;

import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.user.dto.ChangePasswordDto;
import com.rosanova.iot.timer.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/authorized/api/auth")
public class ChangePasswordController {

    private final UserService userService;

    public ChangePasswordController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordDto request) {

        Result result = userService.changePassword(
                request.getOldPassword(),
                request.getNewPassword()
        );

        if (result == Result.SUCCESS) {
            return ResponseEntity.ok("Password aggiornata con successo");
        } else {
            return ResponseEntity.badRequest().body("Errore durante il cambio password: dati non validi");
        }
    }
}
