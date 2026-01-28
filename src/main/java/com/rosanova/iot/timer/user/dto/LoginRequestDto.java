package com.rosanova.iot.timer.user.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDto {
    @NotBlank(message = "Il nome non può essere vuoto")
    @Size(min =4,max = 4, message = "Il nome è troppo lungo")
    private String username;

    @NotBlank(message = "Il nome non può essere vuoto")
    @Size(max = 10, min =8, message = "Il nome è troppo lungo")
    private String password;
}
