package com.rosanova.iot.timer.user.controller;

import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.user.dto.LoginRequestDto;
import com.rosanova.iot.timer.user.dto.LoginReturnDto;
import com.rosanova.iot.timer.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginController {

    private final String NAME_TOKEN = "TIMER_SESSION_TOKEN";
    private final long DURATION= 300L;

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginReturnDto> login(@RequestBody LoginRequestDto request) {
        try {

            LoginReturnDto dto = userService.login(request.getUsername(), request.getPassword());

            if (dto.getResult() == Result.SUCCESS) {

            }
                ResponseCookie cookie = ResponseCookie.from(NAME_TOKEN, dto.getToken())
                        .httpOnly(true)
                        .secure(true)
                        .path("/")
                        .maxAge(DURATION)
                        .sameSite("Strict")
                        .build();


            if (dto.getResult() == Result.SUCCESS) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, cookie.toString())
                        .body(dto);
            } else if (dto.getResult() == Result.BAD_REQUEST) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(dto);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(dto);
            }
        } catch (Exception e) {
            LoginReturnDto errorDto = new LoginReturnDto();
            errorDto.setResult(Result.ERROR);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDto);
        }
    }
}
