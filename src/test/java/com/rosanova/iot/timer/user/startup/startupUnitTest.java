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
    @DisplayName("initUserRoot: successo se la password è valida")
    void initUserRoot_Success() {
        startup = new Startup("passwordValida", "NO_RESET", repository);

        Result result = startup.initUserRoot("passwordValida");

        assertEquals(Result.SUCCESS, result);

        // Verifichiamo che l'utente creato sia corretto
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(repository).insertUser(userCaptor.capture());
        assertEquals("root", userCaptor.getValue().getUsername());
        assertEquals("passwordValida", userCaptor.getValue().getPassword());
    }

    // --- TEST METODO init() - Analisi degli Stati ---

    @Test
    @DisplayName("init: crea root se il database è vuoto")
    void init_DatabaseEmpty_CreatesRoot() {
        // Mock: database vuoto
        when(repository.findNumberOfUsers()).thenReturn(0);

        // Istanza con password valida
        startup = new Startup("rootPassword", "NO_RESET", repository);

        // Eseguiamo manualmente il metodo annotato con @PostConstruct
        startup.init();

        verify(repository).insertUser(argThat(u -> u.getUsername().equals("root")));
    }

    @Test
    @DisplayName("init: non fa nulla se utenti presenti e reset disabilitato")
    void init_UsersPresent_NoReset_DoesNothing() {
        // Mock: utenti già presenti
        when(repository.findNumberOfUsers()).thenReturn(1);

        startup = new Startup("rootPassword", "NO_RESET", repository);

        startup.init();

        verify(repository, never()).insertUser(any(User.class));
    }

    @Test
    @DisplayName("init: forza la creazione/reset se richiesto dal flag RESET")
    void init_UsersPresent_WithResetFlag_CreatesRoot() {
        // Mock: utenti presenti ma vogliamo resettare
        when(repository.findNumberOfUsers()).thenReturn(5);

        startup = new Startup("newRootPassword", "RESET", repository);

        startup.init();

        // Verifica che initUserRoot venga chiamato nonostante la presenza di utenti
        verify(repository).insertUser(argThat(u -> u.getPassword().equals("newRootPassword")));
    }

    @Test
    @DisplayName("init: cattura l'eccezione se la password di reset è troppo corta")
    void init_ExceptionCatching() {
        // Database vuoto, ma password configurata male (es. 3 caratteri)
        when(repository.findNumberOfUsers()).thenReturn(0);

        // Passiamo una password non valida (< 8 caratteri)
        startup = new Startup("123", "NO_RESET", repository);

        // Non deve lanciare eccezioni verso l'esterno (le logga internamente)
        assertDoesNotThrow(() -> startup.init());

        // Verifichiamo che non sia stato inserito nulla a causa dell'errore
        verify(repository, never()).insertUser(any(User.class));
    }

}
