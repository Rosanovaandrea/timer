package com.rosanova.iot.timer.user.startup;

import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.user.Startup;
import com.rosanova.iot.timer.user.User;
import com.rosanova.iot.timer.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCrypt; // Importante per la verifica

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class startupUnitTest {

    @Mock
    private UserRepository repository;

    private Startup startup;

    // --- TEST METODO initUserRoot ---

    @Test
    @DisplayName("initUserRoot: fallisce se la password è più corta di 8 caratteri")
    void initUserRoot_PasswordTooShort() {
        startup = new Startup("123", "NO_RESET", repository);

        Result result = startup.initUserRoot("123");

        assertEquals(Result.ERROR, result);
        verify(repository, never()).insertUser(any(User.class));
    }

    @Test
    @DisplayName("initUserRoot: successo se la password è valida e viene criptata")
    void initUserRoot_Success() {
        String passwordChiaro = "passwordValida";
        startup = new Startup(passwordChiaro, "NO_RESET", repository);

        Result result = startup.initUserRoot(passwordChiaro);

        assertEquals(Result.SUCCESS, result);

        // Catturiamo l'utente salvato per ispezionare la password
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(repository).insertUser(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("root", savedUser.getUsername());

        // MODIFICA: Verifichiamo che la password NON sia in chiaro e che sia un hash valido
        assertNotEquals(passwordChiaro, savedUser.getPassword());
        assertTrue(BCrypt.checkpw(passwordChiaro, savedUser.getPassword()), "L'hash della password non corrisponde alla password in chiaro");
    }

    // --- TEST METODO init() - Analisi degli Stati ---

    @Test
    @DisplayName("init: crea root se il database è vuoto")
    void init_DatabaseEmpty_CreatesRoot() {
        when(repository.findNumberOfUsers()).thenReturn(0);
        startup = new Startup("rootPassword", "NO_RESET", repository);

        startup.init();

        // Verifichiamo che l'utente creato abbia lo username corretto
        verify(repository).insertUser(argThat(u -> u.getUsername().equals("root")));
    }

    @Test
    @DisplayName("init: non fa nulla se utenti presenti e reset disabilitato")
    void init_UsersPresent_NoReset_DoesNothing() {
        when(repository.findNumberOfUsers()).thenReturn(1);
        startup = new Startup("rootPassword", "NO_RESET", repository);

        startup.init();

        verify(repository, never()).insertUser(any(User.class));
    }

    @Test
    @DisplayName("init: forza il reset e cripta la nuova password se richiesto")
    void init_UsersPresent_WithResetFlag_CreatesRoot() {
        when(repository.findNumberOfUsers()).thenReturn(5);
        String newPassword = "newRootPassword";
        startup = new Startup(newPassword, "RESET", repository);

        startup.init();

        // MODIFICA: Usiamo checkpw all'interno del matcher argThat
        verify(repository).insertUser(argThat(u ->
                u.getUsername().equals("root") && BCrypt.checkpw(newPassword, u.getPassword())
        ));
    }

    @Test
    @DisplayName("init: cattura l'eccezione se la password di reset è troppo corta")
    void init_ExceptionCatching() {
        when(repository.findNumberOfUsers()).thenReturn(0);
        startup = new Startup("123", "NO_RESET", repository);

        assertDoesNotThrow(() -> startup.init());

        verify(repository, never()).insertUser(any(User.class));
    }
}
