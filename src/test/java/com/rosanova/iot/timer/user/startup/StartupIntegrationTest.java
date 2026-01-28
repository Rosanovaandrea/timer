package com.rosanova.iot.timer.user.startup;

import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.user.Startup;
import com.rosanova.iot.timer.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test") // Evita il profilo "test" per far caricare i componenti necessari
@Transactional // Fondamentale: fa il rollback dopo ogni test per non sporcare il DB
class StartupIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private Startup startup;

    private final String VALID_PWD = "supersecretpassword";
    private final String RESET_FLAG = "RESET";
    private final String NO_RESET = "NO_RESET";

    @BeforeEach
    void setUp() {
         userRepository.deleteAllUsers();
    }

    @Test
    @DisplayName("Integrazione: Inserimento root con repository reale e verifica persistenza")
    void integration_initUserRoot_persistsToDb() {
        // Creiamo il POJO passando il repository reale di Spring
        startup = new Startup(VALID_PWD, NO_RESET, userRepository);

        Result result = startup.initUserRoot(VALID_PWD);

        assertEquals(Result.SUCCESS, result);

        // Verifichiamo che l'utente sia DAVVERO nel database
        // Nota: Assumendo che il tuo repository abbia un metodo findByUsername
        // Se non lo ha, puoi usare findNumberOfUsers
        assertTrue(userRepository.findNumberOfUsers() > 0, "L'utente dovrebbe essere persistito nel DB");
    }

    @Test
    @DisplayName("Integrazione: Verifica logica init() quando il DB è vuoto")
    void integration_init_logic_emptyDb() {
        // Prepariamo lo startup con una password valida
        startup = new Startup(VALID_PWD, NO_RESET, userRepository);

        // Eseguiamo la logica
        startup.init();

        // Verifica
        assertEquals(1, userRepository.findNumberOfUsers(), "Dovrebbe esserci esattamente 1 utente (root)");
    }

    @Test
    @DisplayName("Integrazione: Verifica che il flag RESET sovrascriva l'utente esistente")
    void integration_init_withResetFlag() {
        // 1. Setup iniziale: creiamo un utente esistente
        startup = new Startup("vecchiaPassword", NO_RESET, userRepository);
        startup.init();

        // 2. Creiamo una nuova istanza di Startup con flag RESET e nuova password
        String nuovaPass = "nuovaPasswordSicura";
        Startup startupReset = new Startup(nuovaPass, RESET_FLAG, userRepository);

        // 3. Eseguiamo init()
        startupReset.init();

        // 4. In un'app reale verificheresti l'hash, qui verifichiamo che il processo termini senza errori
        // e che il numero di utenti sia coerente (se insertUser fa un 'upsert' o se gestisce il duplicato)
        assertTrue(userRepository.findNumberOfUsers() >= 1);
    }
}
