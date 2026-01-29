package com.rosanova.iot.timer.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordDto {

    @NotBlank(message = "La vecchia password è obbligatoria")
    @Size(min = 8, max = 10, message = "La vecchia password deve essere tra 8 e 10 caratteri")
    private String oldPassword;

    @NotBlank(message = "La nuova password è obbligatoria")
    @Size(min = 8, max = 10, message = "La nuova password deve essere tra 8 e 10 caratteri")
    private String newPassword;

}
