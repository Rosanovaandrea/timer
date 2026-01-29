package com.rosanova.iot.timer.cleanup;


import com.rosanova.iot.timer.monitor.Monitor;
import com.rosanova.iot.timer.monitor.repository.MonitorRepository;
import com.rosanova.iot.timer.timer.repository.TimerRepository;
import com.rosanova.iot.timer.utils.TimerUtils;
import com.rosanova.iot.timer.utils.impl.HashMapInt;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Profile("!test")
public class CleanUp implements SmartInitializingSingleton {

    private final ExecutorService threadPool;

    private final MonitorRepository monitorRepository;

    private final TimerRepository timerRepository;

    private final Path tmpDir;

    private final Path timerDir;

    private final Path monitorDir;
    private final ReentrantLock ioLock;

    private final static String EXTENSION = ".timer" ;
    private final TimerUtils fileSystemUtils;

    public CleanUp(ReentrantLock ioLock,
                   @Autowired MonitorRepository monitorRepository,
                   @Autowired TimerRepository timerRepository,
                   @Value("${tmp.directory}") String tmpDir,
                   @Value("${systemd.directory}") String timerDirectory,
                   @Value("${systemd.monitor.directory}") String monitorDirectory,
                   ExecutorService executor,
                   @Qualifier("timerDefault") TimerUtils fileSystemUtils) {

        this.monitorRepository = monitorRepository;
        this.timerRepository = timerRepository;
        this.threadPool = executor;
        this.tmpDir = Paths.get(tmpDir);
        this.timerDir = Paths.get(timerDirectory);
        this.monitorDir = Paths.get(monitorDirectory);
        this.ioLock = ioLock;
        this.fileSystemUtils = fileSystemUtils;

    }
    //metodo di pulizia richiamato all' avvio e che viene avviato solo dopo che tutto i @PostConstruct dei bean singleton sono avviati
    @Override
    public void afterSingletonsInstantiated() {

        System.out.println("SISTEMA PRONTO: Controllo integrità e Cleanup in corso...");
        cleanUpMethod();
    }


    @Scheduled(cron = "0 0 21 * * *")
    public void cleanUpMethod() {
        boolean lock = false;
        try {

            lock = ioLock.tryLock(10L,TimeUnit.SECONDS);

            if(!lock) {
                System.err.println("IL SISTEMA DI PULIZIA NON é RIUSCITO AD ACQUISIRE IL LOCK SUL FILESYSTEM");
                return;
            }

            HashMapInt timerToRemain = new HashMapInt();
            HashMapInt timerMonitoRoRemain = new HashMapInt();

            timerRepository.addEndTimesToMap(timerToRemain);
            Monitor monitor = monitorRepository.getMonitor();
            timerMonitoRoRemain.add(monitor.getStart());
            timerMonitoRoRemain.add(monitor.getStop());

            CountDownLatch latch = getLatch(3);

            threadPool.submit(() -> {
                try {
                    deleteFiledFromTimerDirectory(timerDir, timerToRemain, EXTENSION);
                } finally {
                    latch.countDown();
                }
            });
            threadPool.submit(() -> {
                try {
                    deleteFiledFromTimerDirectory(monitorDir, timerMonitoRoRemain, EXTENSION);
                } finally {
                    latch.countDown();
                }
            });
            threadPool.submit(() -> {
                try {
                    deleteFiledFromTmpDirectory(tmpDir, EXTENSION);
                } finally {
                    latch.countDown();
                }
            });

            if(!latch.await(10L, TimeUnit.SECONDS)) System.err.println("ERRORE IL CLEANUP NON é STATO COMPLETATO NEL TEMPO LIMITE");

        }catch (InterruptedException e){
            System.err.println( "ERRORE DURANTE IL CLEANUP:" + e.getMessage());
            interruptThread();

        } catch (Exception e){
            System.err.println( "ERRORE DURANTE IL CLEANUP:" + e.getMessage());
        }
        finally {

            try {
                if(lock)  fileSystemUtils.timerReload();
            }catch (Exception e){
                System.err.println("ERRORE NELLA SINCRONIZZAZIONE DEL GESTORE TIMERS DURANTE LA PULIZIA AUTOMATICA" + e.getMessage());
            }

           if(lock) ioLock.unlock();
        }
    }

    public CountDownLatch getLatch (int n){
        return new CountDownLatch(n);
    }

    public void  interruptThread(){
        Thread.currentThread().interrupt();
    }


    public void deleteFiledFromTimerDirectory(Path dir, HashMapInt myMap, String extension) {

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {

            String filename;

            for (Path entry : stream) {

                if (Files.isDirectory(entry)){
                    System.err.println("STATO INCONSISTENTE DUANTE LA CANCELLAZIONE; LA CARTELLA NON DOVREBBE CONTENERE SOTTOCARTELLE; DOVREI CONTROLLARE");
                    continue;
                }


                    try {

                        filename = entry.getFileName().toString();

                        if (filename.length() > extension.length() && filename.regionMatches(filename.length() - extension.length(), extension, 0, extension.length())) {

                            filename = filename.substring(0, filename.length() - extension.length());

                            int fileId = Integer.parseInt(filename);

                            if (!myMap.search(fileId)) {
                                Files.delete(entry);
                            }

                        } else {
                            System.err.println("STATO INCONSISTENTE DURANTE LA CANCELLAZIONE: TROVATO FILE CON NOME TROPPO CORTO");
                        }

                    } catch (NumberFormatException e) {
                        System.err.println("STATO INCONSISTENTE DURANTE LA CANCELLAZIONE: LA CARTELLA DOVREBBE CONTENERE SOLO ELEMENTI CON NOMI NUMERICI; DOVRESTI CONTROLLARE");
                    } catch (IOException e){
                        System.err.println("ERRORE DURANTE LE OPERAZIONI SU SINGOLO FILE");
                    }

            }
        } catch (Exception e) {
            System.err.println("Errore durante la lettura della directory: " + e.getMessage());
        }
    }

    public void deleteFiledFromTmpDirectory(Path dir,String extension) {

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {

            String filename;

            for (Path entry : stream) {

                if (Files.isDirectory(entry)){
                    System.err.println("STATO INCONSISTENTE DUANTE LA CANCELLAZIONE; LA CARTELLA NON DOVREBBE CONTENERE SOTTOCARTELLE; DOVREI CONTROLLARE");
                    continue;
                }


                try {

                    filename = entry.getFileName().toString();

                    filename = entry.getFileName().toString();

                    if (filename.length() > extension.length() && filename.regionMatches(filename.length() - extension.length(), extension, 0, extension.length())) {

                        filename = filename.substring(0, filename.length() - extension.length());

                        int fileId = Integer.parseInt(filename);


                        Files.delete(entry);


                    } else {
                        System.err.println("STATO INCONSISTENTE DURANTE LA CANCELLAZIONE: TROVATO FILE NON CONFORME");
                    }

                } catch (NumberFormatException e) {
                    System.err.println("STATO INCONSISTENTE DURANTE LA CANCELLAZIONE: LA CARTELLA DOVREBBE CONTENERE SOLO ELEMENTI CON NOMI NUMERICI; DOVRESTI CONTROLLARE");
                } catch (IOException e){
                    System.err.println("ERRORE DURANTE LE OPERAZIONI SU SINGOLO FILE");
                }

            }
        } catch (Exception e) {
            System.err.println("Errore durante la lettura della directory: " + e.getMessage());
        }
    }

}
