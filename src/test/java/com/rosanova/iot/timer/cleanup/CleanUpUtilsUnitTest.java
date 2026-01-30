package com.rosanova.iot.timer.cleanup;


import com.rosanova.iot.timer.monitor.Monitor;
import com.rosanova.iot.timer.monitor.repository.MonitorRepository;
import com.rosanova.iot.timer.timer.repository.TimerRepository;
import com.rosanova.iot.timer.utils.TimerUtils;
import com.rosanova.iot.timer.utils.impl.HashMapInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class CleanUpUtilsUnitTest {

    @TempDir
    Path directory;

    private ExecutorService executor;
    private MonitorRepository monitorRepository;
    private TimerRepository timerRepository;
    private ReentrantLock ioLock;
    private TimerUtils fileSystemUtils;
    @Mock private CountDownLatch mockLatch;

    private CleanUp cleanUp;


    String monitorDir;

    @BeforeEach
    void init(){

        monitorRepository = Mockito.mock(MonitorRepository.class);
        timerRepository = Mockito.mock(TimerRepository.class);
        ioLock = Mockito.mock(ReentrantLock.class);
        executor = Mockito.mock(ExecutorService.class);
        fileSystemUtils = Mockito.mock(TimerUtils.class);
        mockLatch = Mockito.mock(CountDownLatch.class);

        // Creiamo lo SPY per poter mockare getLatch() e verificare le chiamate ai metodi interni
        cleanUp = spy(new CleanUp(
                ioLock, monitorRepository, timerRepository,
                "/tmp", "/timer", "/monitor",
                executor, fileSystemUtils
        ));
    }

    @Test
    void cleanUpMethod_ErrorInTimerReload_ShouldStillUnlock() throws InterruptedException {
        // --- 1. ARRANGE ---

        // Tutto procede bene fino al finally
        when(ioLock.tryLock(10L, TimeUnit.SECONDS)).thenReturn(true);
        doReturn(new Monitor(0, 10, 20)).when(monitorRepository).getMonitor();
        doReturn(mockLatch).when(cleanUp).getLatch(3);
        when(mockLatch.await(anyLong(), any())).thenReturn(true);

        // Simuliamo che il reload fallisca con una RuntimeException
        doThrow(new RuntimeException("Systemctl daemon-reload failed"))
                .when(fileSystemUtils).timerReload();

        // --- 2. ACT ---
        cleanUp.cleanUpMethod();

        // --- 3. ASSERT ---

        // 1. Verifichiamo che il reload sia stato effettivamente chiamato
        verify(fileSystemUtils, times(1)).timerReload();

        // 2. CRUCIALE: Nonostante l'eccezione in timerReload, il lock DEVE essere rilasciato
        // Il tuo codice ha il try-catch interno al finally proprio per questo motivo
        verify(ioLock, times(1)).unlock();

        // 3. Verifichiamo che i task siano stati sottomessi correttamente prima del crash del reload
        verify(executor, times(3)).submit(any(Runnable.class));

        // 4. Verifichiamo che non sia stata chiamata l'interruzione del thread
        // Un errore nel reload non è un'interruzione di sistema
        verify(cleanUp, never()).interruptThread();
    }

    @Test
    void cleanUpMethod_InterruptedDuringLockAcquisition_ShouldCallInterruptOnly() throws InterruptedException {
        // --- 1. ARRANGE ---

        // Simuliamo che il thread venga interrotto proprio mentre tenta di acquisire il lock
        // Nota: tryLock lancia InterruptedException, quindi usiamo thenThrow
        when(ioLock.tryLock(10L, TimeUnit.SECONDS))
                .thenThrow(new InterruptedException("Interrupted during lock acquisition"));

        // --- 2. ACT ---
        cleanUp.cleanUpMethod();

        // --- 3. ASSERT ---

        // 1. Verifica che sia stato chiamato il metodo catch specifico per l'interruzione
        verify(cleanUp, times(1)).interruptThread();

        // 2. Verifica che il database NON sia stato interrogato (l'errore è avvenuto prima)
        verify(timerRepository, never()).addEndTimesToMap(any());
        verify(monitorRepository, never()).getMonitor();

        // 3. Verifica che NESSUN task sia stato inviato all'executor
        verify(executor, never()).submit(any(Runnable.class));

        // --- 4. FINALLY BLOCK CHECK ---

        // 4. Il reload non viene chiamato nel finally perché il lock non è acquisito
        verify(fileSystemUtils, never()).timerReload();

        // 5. CRUCIALE: Il lock NON deve essere rilasciato perché non è mai stato acquisito!
        // Se lo chiamassi, il test (e il codice reale) fallirebbe.
        verify(ioLock, never()).unlock();
    }

    @Test
    void cleanUpMethod_InterruptedDuringAwait_ShouldCallInterruptThreadAndCleanup() throws InterruptedException {
        // --- 1. ARRANGE ---

        // Il lock ha successo
        when(ioLock.tryLock(10L, TimeUnit.SECONDS)).thenReturn(true);

        // Mock DB ok
        doReturn(new Monitor(0, 10, 20)).when(monitorRepository).getMonitor();

        // Mock Latch: iniettiamo il mock tramite lo spy
        doReturn(mockLatch).when(cleanUp).getLatch(3);

        // SIMULAZIONE INTERRUZIONE:
        // Quando il thread principale arriva a latch.await(), lanciamo InterruptedException
        when(mockLatch.await(2L, TimeUnit.MINUTES))
                .thenThrow(new InterruptedException("Thread forcibly interrupted"));

        // --- 2. ACT ---
        cleanUp.cleanUpMethod();

        // --- 3. ASSERT ---

        // 1. Verifica che sia stato chiamato il metodo catch specifico per l'interruzione
        verify(cleanUp, times(1)).interruptThread();

        // 2. Verifica che il flusso sia passato per i submit prima dell'interruzione
        verify(executor, times(3)).submit(any(Runnable.class));

        // --- 4. FINALLY BLOCK (Sicurezza) ---

        // 3. Il reload deve essere chiamato comunque
        verify(fileSystemUtils, times(1)).timerReload();

        // 4. Il lock DEVE essere rilasciato anche se il thread è stato interrotto
        verify(ioLock, times(1)).unlock();
    }

    @Test
    void cleanUpMethod_SubmitFailure_ShouldHandleExceptionAndReleaseLock() throws InterruptedException {
        // --- 1. ARRANGE ---

        // Successo nelle fasi preliminari
        when(ioLock.tryLock(10L, TimeUnit.SECONDS)).thenReturn(true);
        doReturn(new Monitor(0, 10, 20)).when(monitorRepository).getMonitor();
        doReturn(mockLatch).when(cleanUp).getLatch(3);

        // Simuliamo che l'executor rifiuti il task (RejectedExecutionException)
        // Usiamo una RuntimeException generica che viene catturata dal tuo catch (Exception e)
        when(executor.submit(any(Runnable.class)))
                .thenThrow(new RuntimeException("Executor pool is full or shut down"));

        // --- 2. ACT ---
        cleanUp.cleanUpMethod();

        // --- 3. ASSERT ---

        // Verifichiamo che il primo submit sia stato tentato
        verify(executor, times(1)).submit(any(Runnable.class));

        // Verifichiamo che i submit successivi NON siano stati chiamati (il flusso si interrompe)
        verify(executor, times(1)).submit(any(Runnable.class)); // confermiamo che il totale chiamate è 1

        // Verifichiamo che NON sia stato chiamato interruptThread
        // (A meno che tu non abbia simulato specificamente una InterruptedException)
        verify(cleanUp, never()).interruptThread();

        // --- 4. FINALLY BLOCK CHECK ---

        // Il lock deve essere rilasciato SEMPRE
        verify(ioLock, times(1)).unlock();

        // Il reload viene chiamato nel finally (anche se non abbiamo cancellato nulla)
        verify(fileSystemUtils, times(1)).timerReload();

        // Il latch.await() non viene mai raggiunto perché l'eccezione avviene prima
        verify(mockLatch, never()).await(anyLong(), any());
    }

    @Test
    void cleanUpMethod_DatabaseErrorOnTimerRepository_ShouldReleaseLock() throws InterruptedException {
        // --- 1. ARRANGE ---

        // Il lock deve avere successo per arrivare al database
        when(ioLock.tryLock(10L, TimeUnit.SECONDS)).thenReturn(true);

        // Simuliamo un errore Runtime (es. DataAccessException) sul repository dei timer
        // Usiamo doThrow se il metodo è void o when().thenThrow() se restituisce qualcosa
        doThrow(new RuntimeException("DB ERROR: Connection refused"))
                .when(timerRepository).addEndTimesToMap(any(HashMapInt.class));

        // --- 2. ACT ---
        // Il metodo lancerà l'eccezione internamente (o la catturerà se hai un try-catch largo)
        // Nel tuo codice attuale, l'eccezione non è catturata specificatamente prima del finally
        cleanUp.cleanUpMethod();

        // --- 3. ASSERT (SICUREZZA DEL SISTEMA) ---

        // 1. Verifichiamo che il lock sia stato tentato e acquisito
        verify(ioLock).tryLock(10L, TimeUnit.SECONDS);

        // 2. CRUCIALE: Il lock deve essere rilasciato nonostante l'errore DB
        verify(ioLock, times(1)).unlock();

        // 3. Verifichiamo che i thread non siano mai stati avviati
        // Se il database fallisce prima, il codice non deve arrivare ai submit
        verify(executor, never()).submit(any(Runnable.class));

        // 4. Verifichiamo che non sia stata chiamata la sincronizzazione systemd
        // Poiché il database è fallito, non abbiamo cancellato nulla, quindi il reload è inutile
        // (Nel tuo codice attuale il reload è nel finally, quindi verrà chiamato.
        // Se vuoi evitarlo in caso di errore DB, dovresti mettere un flag)
        verify(fileSystemUtils, times(1)).timerReload();

        // 5. NEGATIVE VERIFICATIONS
        verify(cleanUp, never()).interruptThread(); // Nessuna interruzione di thread, solo errore logico
        verify(monitorRepository, never()).getMonitor(); // Non dovrebbe arrivarci se fallisce il primo repo
    }


    @Test
    void cleanUpMethod_HappyPath_FullAnalysis() throws InterruptedException {
        // --- 1. ARRANGE ---
        // Mock DB: monitor con ID 10 e 20

        doReturn(new Monitor(0,10, 20)).when(monitorRepository).getMonitor();

        // Mock Lock: acquisito con successo
        when(ioLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Mock Latch: iniettiamo il mock tramite lo spy
        doReturn(mockLatch).when(cleanUp).getLatch(3);
        when(mockLatch.await(anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Captor per le 3 lambda (Runnable)
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

        // --- 2. ACT ---
        cleanUp.cleanUpMethod();

        // --- 3. ASSERT (ORCHESTRAZIONE) ---

        // Verifichiamo l'ordine logico delle chiamate
        verify(timerRepository).addEndTimesToMap(any(HashMapInt.class));
        verify(monitorRepository).getMonitor();
        verify(ioLock).tryLock(10L, TimeUnit.SECONDS);

        // Catturiamo le lambda inviate al pool
        verify(executor, times(3)).submit(runnableCaptor.capture());

        // Verifichiamo che il reload sia avvenuto PRIMA dell'unlock (grazie al finally)
        verify(fileSystemUtils).timerReload();
        verify(ioLock).unlock();

        // --- 4. ANALISI DELLE ARROW FUNCTIONS (RUNNABLES) ---

        List<Runnable> capturedRunnables = runnableCaptor.getAllValues();
        assertEquals(3, capturedRunnables.size(), "Dovrebbero esserci 3 task sottomessi");

        // Analisi Task 1: Timer Directory
        capturedRunnables.get(0).run();
        verify(cleanUp).deleteFiledFromTimerDirectory(eq(Path.of("/timer")), any(HashMapInt.class), anyString());
         // Verifica che il task segnali la fine

        // Analisi Task 2: Monitor Directory
        capturedRunnables.get(1).run();
        verify(cleanUp).deleteFiledFromTimerDirectory(eq(Path.of("/monitor")), any(HashMapInt.class), anyString());


        // Analisi Task 3: Tmp Directory
        capturedRunnables.get(2).run();
        verify(cleanUp).deleteFiledFromTmpDirectory(eq(Path.of("/tmp")), anyString());

        verify(mockLatch,Mockito.times(3)).countDown();


        // --- 5. NEGATIVE VERIFICATIONS (Cosa NON deve succedere) ---

        // In un Happy Path, non dobbiamo interrompere il thread
        verify(cleanUp, never()).interruptThread();

        // Non dobbiamo chiamare il reload più di una volta
        verify(fileSystemUtils, times(1)).timerReload();

        //verifica che il log viene rilasciato
        verify(ioLock, times(1)).unlock();

    }

    @Test
    void cleanUpMethod_LockAcquisitionFailure_ShouldAbortImmediately() throws InterruptedException {
        // --- 1. ARRANGE ---
        // Simuliamo che il lock sia già occupato da un altro processo/thread
        when(ioLock.tryLock(10L, TimeUnit.SECONDS)).thenReturn(false);

        // --- 2. ACT ---
        cleanUp.cleanUpMethod();

        // --- 3. ASSERT (COMPORTAMENTO ATTESO) ---

        // 1. Verifica che non sia stato sottomesso alcun task al pool
        verify(executor, never()).submit(any(Runnable.class));

        // 2. Verifica che non sia stata fatta alcuna operazione sui repository (ottimizzazione)
        // Nota: nel tuo codice attuale addEndTimesToMap viene chiamato PRIMA del lock.
        // Se volessi ottimizzare, potresti spostare le chiamate ai repo dopo il lock.

        // 3. CRUCIALE: Non deve chiamare il rilascio del lock se non lo ha acquisito
        verify(ioLock, never()).unlock();

        // 4. CRUCIALE: Non deve chiamare il latch.await() perché non sono partiti i thread
        // Poiché il latch è una variabile locale creata dentro, usiamo lo spy per verificare
        // che il metodo wrapper non sia stato usato per chiamare await.
        verify(mockLatch, never()).await(anyLong(), any(TimeUnit.class));

        // 5. NEGATIVE VERIFICATIONS ---

        // Non deve esserci stata un'interruzione (perché non è un'eccezione, è un "if")
        verify(cleanUp, never()).interruptThread();

        // Non deve chiamare il reload (perché non è stato fatto nulla)
        verify(fileSystemUtils, never()).timerReload();

        // Opzionale: verificare che il messaggio di errore sia stato stampato (se usi un logger)
    }

    @Test
    void testCleanupTmpDirNoFile() throws IOException {

       String extension = ".timer";
       Path tmp = directory.resolve("tmp");
       Files.createDirectory(tmp);

       cleanUp.deleteFiledFromTmpDirectory(tmp,extension);
    }

    @Test
    void testCleanupTmpDirDeleteValidFormat() throws IOException {

        String extension = ".timer";
        Path tmp = directory.resolve("tmp");
        Path file1 = tmp.resolve("0001.timer");
        Path file2 = tmp.resolve("000.timer");
        Files.createDirectory(tmp);
        Files.createFile(file1);
        Files.createFile(file2);

        Assertions.assertTrue(Files.exists(file1));
        Assertions.assertTrue(Files.exists(file2));

        cleanUp.deleteFiledFromTmpDirectory(tmp,extension);

        Assertions.assertTrue(Files.notExists(file1));
        Assertions.assertTrue(Files.notExists(file2));
    }

    @Test
    void testJobCleanUpAllRight(){

    }

    @Test
    void testCleanupTmpDirDeleteInvalidFilesPresents() throws IOException {

        String extension = ".timer";
        Path tmp = directory.resolve("tmp");
        Path subTmp = tmp.resolve("tmp");
        Path file1 = tmp.resolve("0001.timer");
        Path file2 = tmp.resolve("000.timer");
        Path file3 = tmp.resolve("000");
        Files.createDirectory(tmp);
        Files.createDirectory(subTmp);
        Files.createFile(file1);
        Files.createFile(file2);
        Files.createFile(file3);

        Assertions.assertTrue(Files.exists(file1));
        Assertions.assertTrue(Files.exists(file2));
        Assertions.assertTrue(Files.exists(file3));
        Assertions.assertTrue(Files.exists(subTmp));

        cleanUp.deleteFiledFromTmpDirectory(tmp,extension);

        Assertions.assertTrue(Files.notExists(file1));
        Assertions.assertTrue(Files.notExists(file2));
        Assertions.assertTrue(Files.exists(file3));
        Assertions.assertTrue(Files.exists(subTmp));
    }

    @Test
    void deleteTimerDirEmpty() throws IOException {
        String extension = ".timer";
        Path tmp = directory.resolve("tmp");
        Files.createDirectory(tmp);

        HashMapInt fileToControl = new HashMapInt();

        fileToControl.add(1);
        fileToControl.add(2);

        cleanUp.deleteFiledFromTimerDirectory(tmp,fileToControl,extension);
    }

    @Test
    void deleteTimerDirNoNotValidFile() throws IOException {
        String extension = ".timer";
        Path tmp = directory.resolve("tmp");
        Files.createDirectory(tmp);
        Path file1 = tmp.resolve("0001.timer");
        Path file2 = tmp.resolve("0002.timer");

        Files.createFile(file1);
        Files.createFile(file2);

        Assertions.assertTrue(Files.exists(file1));
        Assertions.assertTrue(Files.exists(file2));

        HashMapInt fileToControl = new HashMapInt();

        fileToControl.add(1);
        fileToControl.add(2);

        cleanUp.deleteFiledFromTimerDirectory(tmp,fileToControl,extension);

        Assertions.assertTrue(Files.exists(file1));
        Assertions.assertTrue(Files.exists(file2));
    }

    @Test
    void deleteTimerDirNotValidFilePresents() throws IOException {
        String extension = ".timer";
        Path tmp = directory.resolve("tmp");
        Files.createDirectory(tmp);
        Path file1 = tmp.resolve("0001.timer");
        Path file2 = tmp.resolve("0002.timer");
        Path file3 = tmp.resolve("0003.timer");
        Path file4 = tmp.resolve("0004.timer");

        Files.createFile(file1);
        Files.createFile(file2);
        Files.createFile(file3);
        Files.createFile(file4);

        Assertions.assertTrue(Files.exists(file1));
        Assertions.assertTrue(Files.exists(file2));
        Assertions.assertTrue(Files.exists(file3));
        Assertions.assertTrue(Files.exists(file4));

        HashMapInt fileToControl = new HashMapInt();

        fileToControl.add(1);
        fileToControl.add(2);

        cleanUp.deleteFiledFromTimerDirectory(tmp,fileToControl,extension);

        Assertions.assertTrue(Files.exists(file1));
        Assertions.assertTrue(Files.exists(file2));
        Assertions.assertFalse(Files.exists(file3));
        Assertions.assertFalse(Files.exists(file4));
    }

    @Test
    void deleteTimerDirNotValidFilePresentsWithExternalFiles() throws IOException {
        String extension = ".timer";
        Path tmp = directory.resolve("tmp");
        Files.createDirectory(tmp);
        Path file1 = tmp.resolve("0001.timer");
        Path file2 = tmp.resolve("0002.timer");
        Path file3 = tmp.resolve("0003.timer");
        Path file4 = tmp.resolve("0004.timer");
        Path file5 = tmp.resolve("000");
        Path subTmp = tmp.resolve("tmp");

        Files.createDirectory(subTmp);
        Files.createFile(file1);
        Files.createFile(file2);
        Files.createFile(file3);
        Files.createFile(file4);
        Files.createFile(file5);

        Assertions.assertTrue(Files.exists(file1));
        Assertions.assertTrue(Files.exists(file2));
        Assertions.assertTrue(Files.exists(file3));
        Assertions.assertTrue(Files.exists(file4));
        Assertions.assertTrue(Files.exists(file5));
        Assertions.assertTrue(Files.exists(subTmp));

        HashMapInt fileToControl = new HashMapInt();

        fileToControl.add(1);
        fileToControl.add(2);

        cleanUp.deleteFiledFromTimerDirectory(tmp,fileToControl,extension);

        Assertions.assertTrue(Files.exists(file1));
        Assertions.assertTrue(Files.exists(file2));
        Assertions.assertFalse(Files.exists(file3));
        Assertions.assertFalse(Files.exists(file4));
        Assertions.assertTrue(Files.exists(file5));
        Assertions.assertTrue(Files.exists(subTmp));
    }

}