package com.rosanova.iot.timer.user.service.unit_test;

import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.user.User;
import com.rosanova.iot.timer.user.dto.LoginReturnDto;
import com.rosanova.iot.timer.user.repository.UserRepository;
import com.rosanova.iot.timer.user.service.impl.UserServiceImpl;
import com.rosanova.iot.timer.utils.HMACSHA256SignatureUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrtionTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HMACSHA256SignatureUtil hmacUtil;

    private UserServiceImpl userService;

    private final String USERNAME = "integration.user";
    private final String PASSWORD_RAW = "password12345";

    @BeforeEach
    void setUp() {
        // Istanziamo il POJO manualmente iniettando le dipendenze reali di Spring
        userService = new UserServiceImpl(userRepository, hmacUtil);

        // Prepariamo un utente reale nel database per i test
        User user = new User();
        user.setUsername(USERNAME);
        // Usiamo il metodo reale di hash per salvare la password corretta
        user.setPassword(userService.hashPassword(PASSWORD_RAW));

        userRepository.insertUser(user);
    }

    // --- TEST LOGIN ---

    @Test
    @DisplayName("Integrazione Login: Successo con password reale hashata e generazione token")
    void login_Integration_Success() {
        // Eseguiamo il login con la password in chiaro
        LoginReturnDto result = userService.login(USERNAME, PASSWORD_RAW);

        assertEquals(Result.SUCCESS, result.getResult());
        assertNotNull(result.getToken());
        // Verifichiamo che il token sia un HMAC valido (non nullo e non vuoto)
        assertTrue(result.getToken().length() > 0);
    }

    @Test
    @DisplayName("Integrazione Login: Fallimento se la password non corrisponde all'hash")
    void login_Integration_WrongPassword() {
        LoginReturnDto result = userService.login(USERNAME, "wrong_password");

        assertEquals(Result.BAD_REQUEST, result.getResult());
        assertNull(result.getToken());
    }

    // --- TEST CHANGE PASSWORD ---

    @Test
    @DisplayName("Integrazione ChangePassword: Verifica persistenza nuova password hashata")
    void changePassword_Integration_Success() {
        // Recuperiamo l'ID dell'utente appena inserito
        User savedUser = userRepository.getByUsername(USERNAME);
        Long userId = (long) savedUser.getId();
        String newPasswordRaw = "newSuperPassword99";

        // Cambiamo la password
        Result result = userService.changePassword(userId, PASSWORD_RAW, newPasswordRaw);

        assertEquals(Result.SUCCESS, result);

        // Verifica finale: proviamo a fare login con la NUOVA password
        // Questo conferma che il database è stato aggiornato con l'hash corretto
        LoginReturnDto loginResult = userService.login(USERNAME, newPasswordRaw);
        assertEquals(Result.SUCCESS, loginResult.getResult(), "Il login con la nuova password dovrebbe funzionare");
    }

    @Test
    @DisplayName("Integrazione ChangePassword: Fallimento se la vecchia password è errata")
    void changePassword_Integration_WrongOldPassword() {
        User savedUser = userRepository.getByUsername(USERNAME);
        Long userId = (long) savedUser.getId();

        Result result = userService.changePassword(userId, "password_sbagliata", "newPass");

        assertEquals(Result.ERROR, result);

        // Verifichiamo che il login funzioni ancora con la vecchia password (nulla è cambiato)
        LoginReturnDto loginResult = userService.login(USERNAME, PASSWORD_RAW);
        assertEquals(Result.SUCCESS, loginResult.getResult());
    }
}
