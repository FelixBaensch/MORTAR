/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2023  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
 *
 * Source code is available at <https://github.com/FelixBaensch/MORTAR>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.cheminf.mortar.model.util;

import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;

import javafx.scene.control.Alert;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Logging utilities. The Java-own logging API is employed.
 *
 * @author Jonas Schaub, Samuel Behr
 * @version 1.0.0.0
 */
public final class LogUtil {
    //<editor-fold defaultstate="collapsed" desc="Private static final class constants">
    /**
     * Root logger
     */
    private static final Logger ROOT_LOGGER = LogManager.getLogManager().getLogger("");

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(LogUtil.class.getName());

    /**
     * Uncaught exception handler to be used in MORTAR. IMPORTANT: Threads running parallel to the JavaFX GUI thread
     * must be assigned this uncaught exception handler manually. BUT this handler tries to display an exception
     * alert dialog to the user and it cannot do this outside the JavaFX main GUI thread. Therefore, methods like
     * setOnFailed() in the Task class that are called in the main thread again must call the uncaught exception handler.
     * The GUI thread is assigned this handler in initializeLoggingEnvironment().
     */
    private static final Thread.UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER = (aThread, aThrowable) -> {
        Logger.getLogger(aThread.getClass().getName()).log(Level.SEVERE, aThrowable.toString(), aThrowable);
        boolean tmpIsError = aThrowable instanceof Error;
        //error means out of memory or stack overflow
        //note: might be obsolete because error gets wrapped in an exception where it occurs, e.g. an ExecutionException for tasks
        if (tmpIsError) {
                GuiUtil.guiMessageAlert(
                        Alert.AlertType.ERROR,
                        Message.get("Error.Notification.Title"),
                        Message.get("Error.SevereError"),
                        aThrowable.toString());
                System.exit(-1);
        } else {
            //the JavaFx GUI thread deals with such exceptions by resetting the binding to a previous value. No need to intervene here
            if (aThrowable.getMessage().equals("Bidirectional binding failed, setting to the previous value")) {
                return;
            }
            //it is an exception (runtime- or IO-), no error
            if (aThread.getThreadGroup().getName().equals("main")) {
                    GuiUtil.guiExceptionAlert(
                            Message.get("Error.Notification.Title"),
                            Message.get("Error.UnexpectedError.Header"),
                            Message.get("Error.UnexpectedError.Content"),
                            (Exception) aThrowable);
            } else {
                //logging is enough in this case
            }
        }
    };
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Private static class variables">
    /**
     * File handler added to the root logger
     */
    private static FileHandler fileHandler;

    /**
     * Log file that is currently logged in
     */
    private static File logFile;

    /**
     * Storage for exceptions thrown in the process of managing the log-files' folder
     */
    private static ArrayList<Exception> storedExceptions;
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public static synchronized methods">
    /**
     * Configures the root logger called by all other loggers in the application not to print to console but to write
     * all logs to the log file specified in preferences. Also logs session start and sets as
     * default uncaught exception handler for the main JavaFX GUI thread an object calling the root logger upon invocation. This method
     * should be invoked once upon starting the application. Then logging can be done by the individual class loggers
     * that will pass their logging messages to the root logger as default.
     * <p>
     * NOTE: Log file related methods need to be synchronized.
     *
     * @return true, if all actions were successfully executed; false, if an exception was thrown; in latter case the
     * root logger will be reset to default configuration
     */
    public static synchronized boolean initializeLoggingEnvironment() {
        try {
            //Configure root logger
            //Remove default console handler or a closed file handler after reset
            Handler[] tmpHandlers = LogUtil.ROOT_LOGGER.getHandlers();
            for (Handler tmpHandler : tmpHandlers) {
                LogUtil.ROOT_LOGGER.removeHandler(tmpHandler);
            }
            //Messages of levels INFO, WARNING and SEVERE will be logged only
            LogUtil.ROOT_LOGGER.setLevel(Level.INFO);
            String tmpLoggingDirectoryPathName = FileUtil.getAppDirPath() + File.separator
                    + BasicDefinitions.LOG_FILES_DIRECTORY + File.separator;
            File tmpLoggingDirectoryFile = new File(tmpLoggingDirectoryPathName);
            //If the directories do not exist already they are created
            if (!tmpLoggingDirectoryFile.exists()) {
                FileUtil.createDirectory(tmpLoggingDirectoryFile.getAbsolutePath());
            }
            String tmpLogFilePathName = tmpLoggingDirectoryPathName + BasicDefinitions.LOG_FILE_NAME
                    + "_"
                    + FileUtil.getTimeStampFileNameExtension();
            String tmpFinalLogFilePathName = FileUtil.getNonExistingFilePath(tmpLogFilePathName, BasicDefinitions.LOG_FILE_NAME_EXTENSION);
            File tmpLogFile = new File(tmpFinalLogFilePathName);
            boolean tmpFileWasCreated = FileUtil.createEmptyFile(tmpLogFile.getAbsolutePath());
            if (!tmpFileWasCreated) {
                throw new Exception("Log file " + tmpFinalLogFilePathName + " could not be created.");
            }
            if (!tmpLogFile.isFile() || !tmpLogFile.canWrite()) {
                throw new Exception("The designated log file " + tmpFinalLogFilePathName + " is not a file or can not be written to.");
            }
            LogUtil.logFile = tmpLogFile;
            LogUtil.fileHandler = new FileHandler(tmpFinalLogFilePathName, true);
            LogUtil.fileHandler.setFormatter(new SimpleFormatter());
            LogUtil.ROOT_LOGGER.addHandler(LogUtil.fileHandler);
            //sets the uncaught exception handler configured above as default for this thread (the main/JavaFX thread)
            Thread.setDefaultUncaughtExceptionHandler(LogUtil.UNCAUGHT_EXCEPTION_HANDLER);
            //exceptions that occurred during managing log files at start up are logged now
            if (LogUtil.storedExceptions != null && !LogUtil.storedExceptions.isEmpty()) {
                for (Exception tmpException : LogUtil.storedExceptions) {
                    LogUtil.LOGGER.log(Level.WARNING, tmpException.toString(), tmpException);
                }
                LogUtil.storedExceptions.clear();
            }
            return true;
        } catch (Exception anException) {
            LogManager.getLogManager().reset();
            LogUtil.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return false;
        }
    }

    /**
     * Reset log file. LogUtils.initializeLoggingEnvironment() will be called to reset the logging environment.
     * <p>
     * NOTE: Log file related methods need to be synchronized.
     *
     * @return true, if all actions were successfully executed; false, if an exception was thrown or the log file in use
     * is not a file
     */
    public static synchronized boolean resetLogFile() {
        try {
            if (Objects.isNull(LogUtil.logFile) || !LogUtil.logFile.isFile()) {
                return false;
            }
            if (Objects.isNull(LogUtil.fileHandler)) {
                return false;
            }
            LogUtil.fileHandler.flush();
            LogUtil.fileHandler.close();
            boolean tmpFileWasDeleted = FileUtil.deleteSingleFile(LogUtil.logFile.getAbsolutePath());
            if (tmpFileWasDeleted) {
                boolean tmpWasLogEnvInitialized = LogUtil.initializeLoggingEnvironment();
                if (tmpWasLogEnvInitialized) {
                    LogUtil.LOGGER.info("Log file was reset.");
                }
                return true;
            } else {
                return false;
            }
        } catch (Exception anException) {
            LogUtil.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return false;
        }
    }
    // </editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public static methods">
    /**
     * Manages the folder in which the log files get saved if it exists.
     * If the folder contains more *.txt files than a specific limit or a minimum of *.txt files while exceeding a maximum
     * limit of bytes used, the older half of the *.txt files gets deleted and the method is called again. Remaining LCK-files
     * (suffix "*.txt.lck") are generally deleted out of the log-files folder.
     * Exceptions occurring in the process are statically stored in this class and logged after initializing the logging
     * environment. The method is intended to be called before startup of the application and before the logging
     * environment is initialized (because the created lock-file will be deleted!).
     *
     * @author Samuel Behr
     */
    public static void manageLogFilesFolderIfExists() {
        Path tmpLogFileDirectory = Paths.get(FileUtil.getAppDirPath() + File.separator + BasicDefinitions.LOG_FILES_DIRECTORY);
        if (!(Files.exists(tmpLogFileDirectory) && Files.isDirectory(tmpLogFileDirectory))) {
            return;
        }
        LogUtil.storedExceptions = new ArrayList<>();
        //deleting all of the *.txt.lck files out of the log-files' folder
        try (DirectoryStream<Path> tmpLCKFilePaths = Files.newDirectoryStream(tmpLogFileDirectory, "*.txt.lck")) {
            for (Path tmpLCKFilePath : tmpLCKFilePaths) {
                try {
                    Files.delete(tmpLogFileDirectory.resolve(tmpLCKFilePath));
                } catch (IOException anException) {
                    LogUtil.storedExceptions.add(anException);
                }
            }
        } catch (IOException anException) {
            LogUtil.storedExceptions.add(anException);
        }
        File [] tmpLogFiles = tmpLogFileDirectory.toFile().listFiles((dir, name) -> name.endsWith(".txt"));
        if (tmpLogFiles == null) {
            return;
        }
        int tmpTotalOfBytesUsed = 0;
        for (File tmpLogFile : tmpLogFiles) {
            tmpTotalOfBytesUsed += tmpLogFile.length();
        }
        //managing the log-files if the limits are exceeded
        //the parameters of this if statement's condition should be changed with caution or otherwise an infinite loop is risked
        if (tmpLogFiles.length > BasicDefinitions.UPPER_LIMIT_OF_LOG_FILES
                || (tmpTotalOfBytesUsed > BasicDefinitions.LIMIT_OF_BYTES_USED_BY_LOG_FILES && tmpLogFiles.length > BasicDefinitions.LOWER_LIMIT_OF_LOG_FILES)) {
            Arrays.sort(tmpLogFiles, Comparator.comparingLong(File::lastModified));
            //trimming the log-files folder by deleting the oldest files
            for (int i = 0; i < (tmpLogFiles.length * BasicDefinitions.FACTOR_TO_TRIM_LOG_FILE_FOLDER); i++) {
                try {
                    Files.delete(tmpLogFileDirectory.resolve(tmpLogFiles[i].toPath()));
                } catch (IOException anException) {
                    LogUtil.storedExceptions.add(anException);
                }
            }
            //calling the method again to check whether the limits are still exceeded
            LogUtil.manageLogFilesFolderIfExists();
        }
    }
    //
    /**
     * Checks the log file directory for .lck files. They indicate that another MORTAR instance is already running or
     * are leftovers from an application crash.
     *
     * @return true if .lck file(s) are found in the logging directory
     * @author Jonas Schaub
     */
    public static boolean checkForLCKFileInLogDir() {
        String tmpLoggingDirPath = LogUtil.getLogFileDirectoryPath();
        File tmpLoggingDirFile = new File(tmpLoggingDirPath);
        if (!tmpLoggingDirFile.exists() || !tmpLoggingDirFile.isDirectory()) {
            return false;
        }
        return tmpLoggingDirFile.listFiles(new FilenameFilter() {
           @Override
           public boolean accept(File dir, String name) {
               return FileUtil.getFileExtension(dir + File.separator + name).equals(".lck");
           }
        }).length > 0;
    }
    // </editor-fold>
    //
    //<editor-fold desc="public properties" defaultstate="collapsed">
    /**
     * Returns the path to directory in which log files are stored
     *
     * @return path (String) to log file directory
     */
    public static String getLogFileDirectoryPath(){
        return FileUtil.getAppDirPath() + File.separator
                + BasicDefinitions.LOG_FILES_DIRECTORY + File.separator;

    }

    /**
     * Returns the specifically configured uncaught exception handler to be used in all MORTAR threads. It logs the
     * exception, shuts down the application if it is a severe error, and displays a warning dialog to the user.
     * IMPORTANT: Threads running parallel to the JavaFX GUI thread
     * must be assigned this uncaught exception handler manually. BUT this handler tries to display an exception
     * alert dialog to the user and it cannot do this outside the JavaFX main GUI thread. Therefore, methods like
     * setOnFailed() in the Task class that are called in the main thread again must call the uncaught exception handler.
     * The GUI thread is assigned this handler in initializeLoggingEnvironment().
     *
     * @return configured uncaught exception handler
     */
    public static Thread.UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return LogUtil.UNCAUGHT_EXCEPTION_HANDLER;
    }
    //</editor-fold>
}
