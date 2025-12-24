package com.rosanova.iot.timer.utils;

import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class TimerUtilsImpl {
    public static final int SUCCESS = 0;
    public static final int ERROR = 1;

    private static final String TIMER_FILE_EXTENSION = ".timer";
    private static final String[] COMMAND_PREFIX = {"/bin/sh", "-c"};

    //file
    private static final String[] FILE_STATIC = {"[Unit]\nDescription=Custom Timer for ", "\n\n[Timer]\nOnCalendar= *-*-* ", "\nUnit=", "\n\n[Install]\nWantedBy=timers.target\n"};

    //command
    private static final String[] COMMAND = {"sudo /usr/bin/systemctl daemon-reload", " && sudo /usr/bin/systemctl enable ", " && sudo /usr/bin/systemctl start "};

    //deactivate-command
    private static final String[] COMMAND_DEACTIVATION = { "sudo /usr/bin/systemctl stop ", " && sudo /usr/bin/systemctl disable "};

    //Errors
    private static final String ERROR_IO_TIMER_WRITE = "ERROR: Failed to create or move timer file";
    private static final String ERROR_SYSTEMCTL = "ERROR: Systemctl sequence failed.";
    private static final String ERROR_SYSTEMCTL_IO = "ERROR: IOException during process execution ";
    private static final String ERROR_SYSTEMCTL_THREAD= "ERROR: Process execution interrupted for process command ";

    private final Path tempDir;

    private final Path targetDir;

    private final String serviceFileName;

    /**
     * @param systemdTimerDir directory di sistema per i file .timer, esempio: /etc/systemd/system/
     * @param serviceFileName nome del service da far partire con il timer, compreso di estensione .service
     * @code tempDIr directory temporanea del sistema operativo
     * **/
    public TimerUtilsImpl(@Value("${tmp.directory}" )String tmpDir,@Value("${systemd.directory}" )String systemdTimerDir, @Value("${systemd.service.name}") String serviceFileName){
            tempDir = Paths.get(tmpDir);
            targetDir = Paths.get(systemdTimerDir);
            this.serviceFileName = serviceFileName;
    }

    /**
     * Crea il contenuto per un file unit Systemd .timer, lo scrive in temporanea
     * e simula lo spostamento nella cartella di sistema.
     * @param timerBaseName Il nome base del file (es. "myjob").
     * @param onCalendar L'orario di esecuzione configurabile.
     * @return Codice di stato: 0 (SUCCESS), 1 (ERROR).
     */
    public int createSystemdTimerUnit(String timerBaseName, String onCalendar) {

        String fullTimerName = timerBaseName + TIMER_FILE_EXTENSION;

        StringBuilder timerContent = new StringBuilder(150);

        timerContent.append(FILE_STATIC[0]).append(serviceFileName)
                .append(FILE_STATIC[1]).append(onCalendar)
                .append(FILE_STATIC[2]).append(serviceFileName)
                .append(FILE_STATIC[3]);


            Path tempTimerFile = tempDir.resolve(fullTimerName);
            Path targetTimerFile = targetDir.resolve(fullTimerName);

            if(writeTimer(tempTimerFile,timerContent.toString()) != SUCCESS) {
                return ERROR;
            }

            if(moveTimer(tempTimerFile, targetTimerFile) != SUCCESS) {
                return ERROR;
            }

            return SUCCESS;

    }

    /**
     * Elimina un file di configurazione .timer spostandolo nella cartella temporanea
     * @param timerBaseName Il nome base del file (es. "myjob").
     * @return Codice di stato: 0 (SUCCESS), 1 (ERROR).
     */
    public int deleteSystemdTimerUnit(String timerBaseName) {

        String fullTimerName = timerBaseName + TIMER_FILE_EXTENSION;


            Path tempTimerFile = tempDir.resolve(fullTimerName);
            Path targetTimerFile = targetDir.resolve(fullTimerName);

            return moveTimer(targetTimerFile, tempTimerFile);

    }

    /**
     * funzione di reverse dell'inserimento basata sulla cancellazione
     * @param timerBaseName Il nome base del file (es. "myjob").
     * @return Codice di stato: 0 (SUCCESS), 1 (ERROR).
     */
    public int reversSystemdTimerUnitInsert(String timerBaseName) {

        String fullTimerName = timerBaseName + TIMER_FILE_EXTENSION;


        Path tempTimerFile = targetDir.resolve(fullTimerName);
        Path targetTimerFile = tempDir.resolve(fullTimerName);

        return moveTimer(targetTimerFile, tempTimerFile);

    }



    public int writeTimer(Path tempTimerFile, String timerContent) {
        try {
            Files.writeString(tempTimerFile, timerContent);
            return SUCCESS;
        } catch (IOException e) {
            System.err.println(ERROR_IO_TIMER_WRITE);
            return ERROR;
        }
    }

    public int moveTimer(Path source,Path destination) {
        try {
            if(Files.exists(source)) {
                Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return SUCCESS;

        } catch (IOException e) {
            return ERROR;
        }
    }



    /**
     * Attivazione del .timer
     * Esegue tutti i comandi 'systemctl' concatenati con '&&' in una singola shell.
     * I comandi vengono eseguiti con 'sudo' e l'output di errore è reindirizzato al Journald.
     *
     * @param timerBaseName Il nome base del file .timer senza estensione (es. "myjob").
     * @return Codice di stato: 0 (SUCCESS), 1 (ERROR).
     */
    public int activateSystemdTimer(String timerBaseName) {

        final String fullTimerName = timerBaseName + TIMER_FILE_EXTENSION;


        StringBuilder commandBuilder = new StringBuilder(150);

        commandBuilder.append(COMMAND[0])
                .append(COMMAND[1]).append(fullTimerName)
                .append(COMMAND[2]).append(fullTimerName);

        String[] fullCommand = new String[3];
        fullCommand[0] = COMMAND_PREFIX[0]; // /bin/sh
        fullCommand[1] = COMMAND_PREFIX[1]; // -c
        fullCommand[2] = commandBuilder.toString();

        try {
            ProcessBuilder pb = new ProcessBuilder(fullCommand);

            Process process = pb.start();

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.println(ERROR_SYSTEMCTL);
                return ERROR;
            }

        } catch (IOException e) {
            System.err.println(ERROR_SYSTEMCTL_IO);
            return ERROR;
        } catch (InterruptedException e) {
            System.err.println(ERROR_SYSTEMCTL_THREAD);
            return ERROR;
        }

        return SUCCESS;
    }

    /**
     * Disattivazione del .timer
     * Esegue tutti i comandi 'systemctl' concatenati con '&&' in una singola shell.
     * I comandi vengono eseguiti con 'sudo' e l'output di errore è reindirizzato al Journald.
     *
     * @param timerBaseName Il nome baIO_ERROR), 2 (INVALID_PARAM_se del file .timer senza estensione (es. "myjob").
     * @return Codice di stato: 0 (SUCCESS), 1 (ERROR).
     */
    public int deactivateSystemdTimer(String timerBaseName) {

        final String fullTimerName = timerBaseName + TIMER_FILE_EXTENSION;


        StringBuilder commandBuilder = new StringBuilder(150);

        commandBuilder.append(COMMAND_DEACTIVATION[0])
                .append(COMMAND_DEACTIVATION[1]).append(fullTimerName);

        String[] fullCommand = new String[3];
        fullCommand[0] = COMMAND_PREFIX[0]; // /bin/sh
        fullCommand[1] = COMMAND_PREFIX[1]; // -c
        fullCommand[2] = commandBuilder.toString();

        try {
            ProcessBuilder pb = new ProcessBuilder(fullCommand);

            Process process = pb.start();

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.println(ERROR_SYSTEMCTL);
                return ERROR;
            }

        } catch (IOException e) {
            System.err.println(ERROR_SYSTEMCTL_IO);
            return ERROR;
        } catch (InterruptedException e) {
            System.err.println(ERROR_SYSTEMCTL_THREAD);
            return ERROR;
        }

        return SUCCESS;
    }
}
