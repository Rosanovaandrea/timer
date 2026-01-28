package com.rosanova.iot.timer.security;

import com.rosanova.iot.timer.utils.impl.HMACSHA256SignatureUtilImpl;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ScurityMvcIntegrationTest {


    private MockMvc mockMvc;

    @Autowired
    private HMACSHA256SignatureUtilImpl hashingUtil;

    private String validTimestamp;
    private String validHash;
    private String validToken;

    @Autowired
    private SecurityFilter securityFilter; // Iniettalo qui

    @BeforeEach
    void setup(WebApplicationContext context) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(securityFilter) // REGISTRAZIONE MANUALE NEL TEST
                .build();
        // Generiamo un token valido per i test di successo
        validTimestamp = String.valueOf(System.currentTimeMillis());
        validHash = hashingUtil.computeHMACSHA256(validTimestamp);
        validToken = validTimestamp + validHash;
    }

    @Test
    void whenValidTokenProvided_thenReturns200() throws Exception {
        mockMvc.perform(get("/authenticated/data")
                        .cookie(new Cookie("TIMER_SESSION_TOKEN", validToken)))
                .andExpect(status().isOk())
                .andExpect(content().string("Accesso Garantito!"));
    }

    @Test
    void whenNoCookieProvided_thenReturns401() throws Exception {
        mockMvc.perform(get("/authenticated/data"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenTokenIsTampered_thenReturns401() throws Exception {
        // Manomettiamo l'ultimo carattere dell'hash
        String tamperedToken = validToken.substring(0, validToken.length() - 1) + (validToken.endsWith("z") ? "a" : "z");

        mockMvc.perform(get("/authenticated/data")
                        .cookie(new Cookie("TIMER_SESSION_TOKEN", tamperedToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenTokenIsExpired_thenReturns403() throws Exception {
        // Creiamo un token vecchio di 6 minuti
        long expiredTime = System.currentTimeMillis() - (360_000);
        String expiredTimestamp = String.valueOf(expiredTime);
        String expiredHash = hashingUtil.computeHMACSHA256(expiredTimestamp);
        String expiredToken = expiredTimestamp + expiredHash;

        mockMvc.perform(get("/authenticated/data")
                        .cookie(new Cookie("TIMER_SESSION_TOKEN", expiredToken)))
                .andExpect(status().isForbidden());
    }
}
