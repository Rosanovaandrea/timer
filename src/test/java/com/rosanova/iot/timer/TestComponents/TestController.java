package com.rosanova.iot.timer.TestComponents;


import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("test")
public class TestController {

    @GetMapping("/authenticated/data")
    public String getProtectedData() {
        return "Accesso Garantito!";
    }

    @GetMapping("/public/status")
    public String getPublicStatus() {
        return "Tutto ok";
    }
}
