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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository repository;

    @Mock
    private HMACSHA256SignatureUtil hashToken;

    @InjectMocks
    @Spy // Lo spy ci permette di fare "override" dei metodi interni della stessa classe
    private UserServiceImpl userService;

    private User testUser;
    private final String username = "mario.rossi";
    private final String rawPassword = "passwordSegreta";
    private final String encodedPassword = "encoded_password_hash";

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(100);
        testUser.setUsername(username);
        testUser.setPassword(encodedPassword);
    }

    // --- TEST METODO login() ---

    @Test
    @DisplayName("Login: fallisce se l'utente non esiste a database")
    void login_UserNotFound() {
        when(repository.getByUsername(username)).thenReturn(null);

        LoginReturnDto result = userService.login(username, rawPassword);

        assertEquals(Result.BAD_REQUEST, result.getResult());
        assertNull(result.getToken());
    }

    @Test
    @DisplayName("Login: fallisce se la password è errata")
    void login_WrongPassword() {
        when(repository.getByUsername(username)).thenReturn(testUser);
        // Mockiamo il metodo dello spy per simulare password errata
        doReturn(false).when(userService).checkPassword(rawPassword, encodedPassword);

        LoginReturnDto result = userService.login(username, rawPassword);

        assertEquals(Result.BAD_REQUEST, result.getResult());
        assertNull(result.getToken());
    }

    @Test
    @DisplayName("Login: successo con generazione token e padding timestamp")
    void login_Success() {
        String mockHmac = "generated_hmac_token";
        when(repository.getByUsername(username)).thenReturn(testUser);
        doReturn(true).when(userService).checkPassword(rawPassword, encodedPassword);
        when(hashToken.computeHMACSHA256(anyString())).thenReturn(mockHmac);

        LoginReturnDto result = userService.login(username, rawPassword);

        assertEquals(Result.SUCCESS, result.getResult());
        // Verifichiamo che il payload inviato a HMAC abbia esattamente 13 caratteri (padding logic)
        verify(hashToken).computeHMACSHA256(argThat(payload -> payload.length() == 13));
    }

    // --- TEST METODO changePassword() ---

    @Test
    @DisplayName("ChangePassword: fallisce se l'ID utente non esiste")
    void changePassword_UserNotFound() {
        when(repository.getByUsername("root")).thenReturn(null);

        Result result = userService.changePassword( "old", "new");

        assertEquals(Result.BAD_REQUEST, result);
        verify(repository, never()).updateUser(anyLong(), anyString());
    }

    @Test
    @DisplayName("ChangePassword: fallisce se la vecchia password non corrisponde")
    void changePassword_WrongOldPassword() {
        when(repository.getByUsername("root")).thenReturn(testUser);
        doReturn(false).when(userService).checkPassword("wrongOld", encodedPassword);

        Result result = userService.changePassword( "wrongOld", "new");

        assertEquals(Result.BAD_REQUEST, result);
        verify(repository, never()).updateUser(anyLong(), anyString());
    }

    @Test
    @DisplayName("ChangePassword: successo e salvataggio nuova password hashata")
    void changePassword_Success() {
        String newPasswordRaw = "newPass123";
        String newPasswordHashed = "new_hash_456";

        when(repository.getByUsername("root")).thenReturn(testUser);
        doReturn(true).when(userService).checkPassword("oldPass", encodedPassword);
        doReturn(newPasswordHashed).when(userService).hashPassword(newPasswordRaw);

        Result result = userService.changePassword("oldPass", newPasswordRaw);

        assertEquals(Result.SUCCESS, result);
        // Verifichiamo che venga chiamato l'update con l'hash corretto
        verify(repository).updateUser(100L, newPasswordHashed);
    }
}
