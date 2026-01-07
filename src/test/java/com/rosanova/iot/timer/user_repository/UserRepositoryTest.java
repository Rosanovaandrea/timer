package com.rosanova.iot.timer.user_repository;
import com.rosanova.iot.timer.user.User;
import com.rosanova.iot.timer.user.repository.impl.UserRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@JdbcTest // Configura un DB in-memory e JdbcTemplate
@Import(UserRepositoryImpl.class) // Importa il repository perché @JdbcTest non scansiona i @Repository
class UserRepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepositoryImpl userRepository;

    // RISOLUZIONE ERRORE: Configura un CacheManager per i test
    @TestConfiguration
    static class CacheTestConfig {
        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("users");
        }
    }

    @BeforeEach
    void setup() {
        // Pulisce la tabella prima di ogni test per garantire l'isolamento
        jdbcTemplate.execute("DELETE FROM user_timer");
    }

    @Test
    @DisplayName("Dovrebbe inserire correttamente un utente")
    void testInsertUser() {
        User user = new User();
        user.setUsername("mario_rossi");
        user.setPassword("secret123");

        userRepository.insertUser(user);

        User savedUser = userRepository.getByUsername("mario_rossi");
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("mario_rossi");
    }

    @Test
    @DisplayName("Dovrebbe trovare tutti gli utenti")
    void testFindAllUsers() {
        jdbcTemplate.update("INSERT INTO user_timer (user_name, password) VALUES (?, ?)", "user1", "p1");
        jdbcTemplate.update("INSERT INTO user_timer (user_name, password) VALUES (?, ?)", "user2", "p2");

        List<User> users = userRepository.findAllUsers();

        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getUsername).containsExactlyInAnyOrder("user1", "user2");
    }

    @Test
    @DisplayName("Dovrebbe trovare il  numero di utenti")
    void countUsers() {
        jdbcTemplate.update("INSERT INTO user_timer (user_name, password) VALUES (?, ?)", "user1", "p1");
        jdbcTemplate.update("INSERT INTO user_timer (user_name, password) VALUES (?, ?)", "user2", "p2");

        int number = userRepository.findNumberOfUsers();

        assertEquals(2, number);

    }

    @Test
    @DisplayName("Dovrebbe trovare il  numero di utenti a 0")
    void countUsersZero() {

        int number = userRepository.findNumberOfUsers();

        assertEquals(0, number);

    }

    @Test
    @DisplayName("Dovrebbe trovare un utente tramite ID")
    void testFindById() {
        jdbcTemplate.update("INSERT INTO user_timer (user_name, password) VALUES (?, ?)", "test_user", "pass");
        // Recuperiamo l'ID generato automaticamente
        Long id = jdbcTemplate.queryForObject("SELECT id FROM user_timer WHERE user_name = 'test_user'", Long.class);

        User user = userRepository.findById(id);

        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isEqualTo("test_user");
    }

    @Test
    @DisplayName("Dovrebbe cambiare la password")
    void testPasswordChange() {
        jdbcTemplate.update("INSERT INTO user_timer (user_name, password) VALUES (?, ?)", "test_user", "pass");
        // Recuperiamo l'ID generato automaticamente
        Long id = jdbcTemplate.queryForObject("SELECT id FROM user_timer WHERE user_name = 'test_user'", Long.class);

        userRepository.updateUser(id, "new_pass");

        User user = userRepository.findById(id);

        assertThat(user).isNotNull();
        assertThat(user.getPassword()).isEqualTo("new_pass");
    }

    @Test
    @DisplayName("Dovrebbe eliminare un utente tramite ID")
    void testDeleteUserById() {
        jdbcTemplate.update("INSERT INTO user_timer (user_name, password) VALUES (?, ?)", "delete_me", "pass");
        Long id = jdbcTemplate.queryForObject("SELECT id FROM user_timer WHERE user_name = 'delete_me'", Long.class);

        userRepository.deleteUserById(id);

        User user = userRepository.getByUsername("delete_me");
        assertThat(user).isNull();
    }

    @Test
    @DisplayName("Dovrebbe restituire null se l'username non esiste")
    void testGetByUsernameNotFound() {
        User user = userRepository.getByUsername("non_esistente");
        assertThat(user).isNull();
    }
}
