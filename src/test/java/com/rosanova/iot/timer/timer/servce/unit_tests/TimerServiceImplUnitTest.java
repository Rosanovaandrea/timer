package com.rosanova.iot.timer.timer.servce.unit_tests;

import com.rosanova.iot.timer.error.Result;
import com.rosanova.iot.timer.error.TimerServiceException;
import com.rosanova.iot.timer.timer.Timer;
import com.rosanova.iot.timer.timer.dto.CheckTimerInsertValidity;
import com.rosanova.iot.timer.timer.repository.TimerRepository;
import com.rosanova.iot.timer.timer.service.impl.TimerServiceImpl;
import com.rosanova.iot.timer.utils.TimerUtils;
import org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TimerServiceImplUnitTest {

    @Mock
    private TimerRepository repository;
    @Mock
    private TimerUtils timerUtils;
    @Mock
    private ReentrantLock sharedLock;

    private TimerServiceImpl timerService;

    private final String NAME = "TestTimer";
    private final int TIME = 600_000; // 10 minuti (start sarà 5 min)
    private final String FILE_NAME = "600000";

    @BeforeEach
    void setUp() {
        // Creiamo l'istanza reale con i mock
        TimerServiceImpl realService = new TimerServiceImpl(repository, timerUtils, sharedLock);
        // Creiamo lo Spy dell'istanza reale
        timerService = spy(realService);
    }

    @Test
    void insertTimerSynchronized_Success() throws InterruptedException {
        // GIVEN
        when(sharedLock.tryLock(100L, TimeUnit.MILLISECONDS)).thenReturn(true);
        // Mockiamo il metodo interno dello spy
        doReturn(Result.SUCCESS).when(timerService).insertTimer(anyString(), anyInt(), anyInt());

        // WHEN
        Result res = timerService.insertTimerSynchronized(NAME, TIME, 30);

        // THEN
        assertEquals(Result.SUCCESS, res);
        verify(sharedLock).tryLock(100L, TimeUnit.MILLISECONDS);
        verify(timerService).insertTimer(NAME, TIME, 30); // Verifica che sia stato chiamato il metodo interno
        verify(sharedLock).unlock(); // Fondamentale: deve rilasciare il lock
    }

    @Test
    void insertTimerSynchronized_LockFail_ShouldReturnError() throws InterruptedException {
        // GIVEN
        when(sharedLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(false);

        // WHEN
        Result res = timerService.insertTimerSynchronized(NAME, TIME, 30);

        // THEN
        assertEquals(Result.ERROR, res);
        verify(timerService, never()).insertTimer(anyString(), anyInt(), anyInt()); // Non deve mai entrare nel metodo interno
        verify(sharedLock, never()).unlock(); // Non deve sbloccare se non ha acquisito
    }

    @Test
    void insertTimerSynchronized_InternalCrash_ShouldStillUnlock() throws InterruptedException {
        // GIVEN
        when(sharedLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
        // Simuliamo un errore imprevisto (es. RuntimeException)
        doThrow(new RuntimeException("Crash")).when(timerService).insertTimer(anyString(), anyInt(), anyInt());

        // WHEN
        Result res = timerService.insertTimerSynchronized(NAME, TIME, 30);

        // THEN
        assertEquals(Result.ERROR, res);
        verify(sharedLock).unlock(); // Il lock deve essere rilasciato nonostante l'eccezione
    }

    @Test
    void removeTimerSynchronized_Success() throws InterruptedException {
        // GIVEN
        long id = 100L;
        when(sharedLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
        doReturn(Result.SUCCESS).when(timerService).removeTimer(id);

        // WHEN
        Result res = timerService.removeTimerSynchronized(id);

        // THEN
        assertEquals(Result.SUCCESS, res);
        verify(timerService).removeTimer(id);
        verify(sharedLock).unlock();
    }

    @Test
    void insertTimer_Success_ShouldExecuteEverything() {
        // GIVEN
        CheckTimerInsertValidity ok = new CheckTimerInsertValidity(5, 0);
        when(repository.countOverlapsAndMaxTimers(anyInt(), anyInt())).thenReturn(ok);
        when(timerUtils.createSystemdTimerUnit(anyString(), anyString(),anyString())).thenReturn(Result.SUCCESS);
        when(timerUtils.timerReload()).thenReturn(Result.SUCCESS);
        when(timerUtils.activateSystemdTimer(anyString())).thenReturn(Result.SUCCESS);

        // WHEN
        Result res = timerService.insertTimer(NAME, TIME,30);

        // THEN
        assertEquals(Result.SUCCESS, res);
        verify(repository).insert(any(Timer.class));
        verify(timerUtils).activateSystemdTimer(FILE_NAME);
    }

    @Test
    void insertTimer_DbOverlap_ShouldThrowAndNotTouchUtils() {
        // GIVEN: 1 sovrapposizione trovata
        CheckTimerInsertValidity fail = new CheckTimerInsertValidity(1, 10);
        when(repository.countOverlapsAndMaxTimers(anyInt(), anyInt())).thenReturn(fail);

        // WHEN & THEN
        assertThrows(TimerServiceException.class, () -> timerService.insertTimer(NAME, TIME,30));
        verify(repository, never()).insert(any());
        verifyNoInteractions(timerUtils);
    }

    @Test
    void insertTimer_MaxLimitReached_ShouldThrow() {
        // GIVEN: 20 timer totali già presenti
        CheckTimerInsertValidity fail = new CheckTimerInsertValidity(40, 0);
        when(repository.countOverlapsAndMaxTimers(anyInt(), anyInt())).thenReturn(fail);

        assertThrows(TimerServiceException.class, () -> timerService.insertTimer(NAME, TIME,30));
        verify(repository, never()).insert(any());
    }

    @Test
    void insertTimer_FailStep1_ShouldRollbackOnlyFile() {
        // GIVEN
        setupValidDbCheck();
        // Fallisce la creazione del file (Step 1)
        when(timerUtils.createSystemdTimerUnit(anyString(), anyString(),anyString())).thenReturn(Result.ERROR);

        // WHEN & THEN
        assertThrows(TimerServiceException.class, () -> timerService.insertTimer(NAME, TIME,30));

        // Verifichiamo il rollback nel finally (step sarà 1)
        verify(timerUtils).reversSystemdTimerUnitInsert(FILE_NAME);
        verify(timerUtils, never()).deactivateSystemdTimer(anyString());
        verify(timerUtils, never()).timerReload();
    }

    @Test
    void insertTimer_FailStep2_ShouldRollbackFileAndReload() {
        // GIVEN
        setupValidDbCheck();
        when(timerUtils.createSystemdTimerUnit(anyString(), anyString(),anyString())).thenReturn(Result.SUCCESS);
        // Fallisce il reload (Step 2)
        when(timerUtils.timerReload()).thenReturn(Result.ERROR);

        // WHEN & THEN
        assertThrows(TimerServiceException.class, () -> timerService.insertTimer(NAME, TIME,30));

        // In questo caso step = 2. Il finally esegue:
        verify(timerUtils).reversSystemdTimerUnitInsert(FILE_NAME); // step > 0
        verify(timerUtils, Mockito.times(2)).timerReload(); // step > 1 (chiamata di rollback)
        verify(timerUtils, never()).deactivateSystemdTimer(anyString());
    }

    @Test
    void insertTimer_FailStep3_ShouldRollbackAll() {
        // GIVEN
        setupValidDbCheck();
        when(timerUtils.createSystemdTimerUnit(anyString(), anyString(),anyString())).thenReturn(Result.SUCCESS);
        when(timerUtils.timerReload()).thenReturn(Result.SUCCESS);
        // Fallisce attivazione (Step 3)
        when(timerUtils.activateSystemdTimer(anyString())).thenReturn(Result.ERROR);

        // WHEN & THEN
        assertThrows(TimerServiceException.class, () -> timerService.insertTimer(NAME, TIME,30));

        // Step = 3. Il finally deve fare tutto:
        verify(timerUtils).deactivateSystemdTimer(FILE_NAME); // step > 2
        verify(timerUtils).reversSystemdTimerUnitInsert(FILE_NAME); // step > 0
        verify(timerUtils, Mockito.times(2)).timerReload(); // Una per lo step 2, una nel finally (step > 1)
    }



    private void setupValidDbCheck() {
        CheckTimerInsertValidity ok = new CheckTimerInsertValidity(0, 0);
        when(repository.countOverlapsAndMaxTimers(anyInt(), anyInt())).thenReturn(ok);
    }

    @Test
    void testDeleteTimerRight(){
        int id = 1;
        Timer timerToDelete = new Timer(id,"alarm_to_delete",80_000,120_000 );
        String filename=String.valueOf(100_000 /* median 80_000, 120_000*/);
        doReturn(timerToDelete).when(repository).findById(id);

        doNothing().when(repository).deleteById(id);
        doReturn(Result.SUCCESS).when(timerUtils).deactivateSystemdTimer(filename);
        doReturn(Result.SUCCESS).when(timerUtils).deleteSystemdTimerUnit(filename);
        doReturn(Result.SUCCESS).when(timerUtils).timerReload();

        Result result = timerService.removeTimer(id);

        assertEquals(Result.SUCCESS, result);

        verify(repository).findById(id);
        verify(repository).deleteById(id);
        verify(timerUtils).deactivateSystemdTimer(filename);
        verify(timerUtils).deleteSystemdTimerUnit(filename);

    }

    @Test
    void testDeleteTimerIdNotFound(){

        int id = 1;
        doReturn(null).when(repository).findById(id);

        assertThrows(TimerServiceException.class, () -> timerService.removeTimer(id));

        verify(repository,times(1)).findById(id);
        verify(repository,times(0)).deleteById(anyInt());
        verify(timerUtils,times(0)).deactivateSystemdTimer(anyString());
        verify(timerUtils,times(0)).deleteSystemdTimerUnit(anyString());

    }

    @Test
    void testDeleteTimerFailureAtDeactivate() {
        int id = 1;
        Timer timerToDelete = new Timer(id, "alarm", 80_000, 120_000);
        String filename = String.valueOf(100_000/*median value 80_000 120_000*/);

        doReturn(timerToDelete).when(repository).findById(id);

        doReturn(Result.ERROR).when(timerUtils).deactivateSystemdTimer(filename);

        doReturn(Result.SUCCESS).when(timerUtils).timerReload();

        assertThrows(TimerServiceException.class, () -> timerService.removeTimer(id));

        verify(timerUtils).timerReload();
        verify(timerUtils).activateSystemdTimer(filename);
        verify(timerUtils, times(0)).reverseDeleteSystemdTimerUnit(anyString());
    }

    @Test
    void testDeleteTimerFailureAtDeleteUnit() {
        int id = 1;
        Timer timerToDelete = new Timer(id, "alarm", 80_000, 120_000);
        String filename = String.valueOf(100_000/*median value 80_000 120_000 */);

        doReturn(timerToDelete).when(repository).findById(id);
        doReturn(Result.SUCCESS).when(timerUtils).deactivateSystemdTimer(filename);
        // Fallimento al secondo step
        doReturn(Result.ERROR).when(timerUtils).deleteSystemdTimerUnit(filename);

        // Mock delle azioni di ripristino nel finally
        doReturn(Result.SUCCESS).when(timerUtils).reverseDeleteSystemdTimerUnit(filename);
        doReturn(Result.SUCCESS).when(timerUtils).activateSystemdTimer(filename);
        doReturn(Result.SUCCESS).when(timerUtils).timerReload();

        assertThrows(TimerServiceException.class, () -> timerService.removeTimer(id));

        // Verifico che il rollback abbia provato a riattivare il timer
        verify(timerUtils).activateSystemdTimer(filename);
        verify(timerUtils).reverseDeleteSystemdTimerUnit(anyString());
        verify(timerUtils).timerReload();
    }

    @Test
    void insertTimer_BoundaryLower_ShouldClampToZero() {
        // GIVEN: Inseriamo un timer a 10 secondi (10.000 ms)
        // Calcolo atteso: 10.000 - 40.000 = -30.000 -> Math.max(0, -30.000) = 0
        int lowTime = 10_000;
        setupValidDbCheck();

        // Mockiamo le risposte di successo per arrivare alla fine
        when(timerUtils.createSystemdTimerUnit(anyString(), anyString(), anyString())).thenReturn(Result.SUCCESS);
        when(timerUtils.timerReload()).thenReturn(Result.SUCCESS);
        when(timerUtils.activateSystemdTimer(anyString())).thenReturn(Result.SUCCESS);

        // WHEN
        timerService.insertTimer("EarlyTimer", lowTime, 30);

        // THEN
        ArgumentCaptor<Timer> timerCaptor = ArgumentCaptor.forClass(Timer.class);
        verify(repository).insert(timerCaptor.capture());

        assertEquals(0, timerCaptor.getValue().getStartTime(), "Start time dovrebbe essere bloccato a 0");
        assertEquals(lowTime + 20_000, timerCaptor.getValue().getEndTime());
    }

    @Test
    void insertTimer_BoundaryUpper_ShouldClampToMaxMills() {
        // GIVEN: Inseriamo un timer a 23h 59min 50s (86.390.000 ms)
        // Calcolo atteso: 86.390.000 + 40.000 = 86.430.000 -> Math.min(86.400.000, ...) = 86.400.000
        int highTime = 86_390_000;
        setupValidDbCheck();

        when(timerUtils.createSystemdTimerUnit(anyString(), anyString(), anyString())).thenReturn(Result.SUCCESS);
        when(timerUtils.timerReload()).thenReturn(Result.SUCCESS);
        when(timerUtils.activateSystemdTimer(anyString())).thenReturn(Result.SUCCESS);

        // WHEN
        timerService.insertTimer("LateTimer", highTime, 30);

        // THEN
        ArgumentCaptor<Timer> timerCaptor = ArgumentCaptor.forClass(Timer.class);
        verify(repository).insert(timerCaptor.capture());

        assertEquals(86_400_000, timerCaptor.getValue().getEndTime(), "End time dovrebbe essere bloccato a 86.400.000");
        assertEquals(highTime - 20_000, timerCaptor.getValue().getStartTime());
    }

    @Test
    void testDeleteTimerFailureAtTimeReload() {
        int id = 1;
        Timer timerToDelete = new Timer(id, "alarm", 80_000, 120_000);
        String filename = String.valueOf(100_000 /*median value 80_000 120_000*/);

        doReturn(timerToDelete).when(repository).findById(id);
        doReturn(Result.SUCCESS).when(timerUtils).deactivateSystemdTimer(filename);
        // Fallimento al secondo step
        doReturn(Result.SUCCESS).when(timerUtils).deleteSystemdTimerUnit(filename);

        // Mock delle azioni di ripristino nel finally
        doReturn(Result.SUCCESS).when(timerUtils).reverseDeleteSystemdTimerUnit(filename);
        doReturn(Result.SUCCESS).when(timerUtils).activateSystemdTimer(filename);
        doReturn(Result.ERROR,Result.SUCCESS).when(timerUtils).timerReload();


        assertThrows(TimerServiceException.class, () -> timerService.removeTimer(id));

        // Verifico che il rollback abbia provato a riattivare il timer
        verify(timerUtils).activateSystemdTimer(filename);
        verify(timerUtils).reverseDeleteSystemdTimerUnit(anyString());
        verify(timerUtils,Mockito.times(2)).timerReload();
    }

    @Test
    void testDeleteTimerFatalErrorDuringRollback() {
        int id = 1;
        Timer timerToDelete = new Timer(id, "alarm", 80_000, 120_000);
        String filename = String.valueOf(100_000/* median 80_000 120_000*/);

        doReturn(timerToDelete).when(repository).findById(id);
        doReturn(Result.ERROR).when(timerUtils).deactivateSystemdTimer(filename);

        // Il rollback fallisce miseramente
        doReturn(Result.ERROR).when(timerUtils).timerReload();

        assertThrows(TimerServiceException.class, () -> timerService.removeTimer(id));

        verify(timerUtils, atLeastOnce()).timerReload();
    }

}
