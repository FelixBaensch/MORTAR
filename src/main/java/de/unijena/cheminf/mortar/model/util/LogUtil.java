/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2020  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas-schaub@uni-jena.de)
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

import de.unijena.cheminf.mortar.message.Message;

import javax.swing.JOptionPane;
import java.io.File;
import java.util.Objects;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.LogManager;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

/**
 * Logging utilities. The Java-own logging API is employed.
 *
 * @author Jonas Schaub
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
    //</editor-fold>
    //
    // <editor-fold defaultstate="collapsed" desc="Public static synchronized methods">
    /**
     * Configures the root logger called by all other loggers in the application not to print to console but to write
     * all logs to the log file specified in preferences. Also logs session start and sets as
     * default uncaught exception handler for all threads an object calling the root logger upon invocation. This method
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
            String tmpLogFilePathName = FileUtil.getAppDirPath() + File.separator
                    + BasicDefinitions.LOG_FILES_DIRECTORY + File.separator;
            File tmpLogFile = new File(tmpLogFilePathName);
            //If the directories do not exist already they are created
            if (!tmpLogFile.exists()) {
                FileUtil.createDirectory(tmpLogFile.getAbsolutePath());
            }
            tmpLogFilePathName += BasicDefinitions.LOG_FILE_NAME
                    + "_"
                    + FileUtil.getTimeStampFileNameExtension();
            tmpLogFilePathName = FileUtil.getNonExistingFilePath(tmpLogFilePathName);
            tmpLogFilePathName += ".txt";
            tmpLogFile = new File(tmpLogFilePathName);
            boolean tmpFileWasCreated = FileUtil.createEmptyFile(tmpLogFile.getAbsolutePath());
            if (!tmpFileWasCreated) {
                throw new Exception("Log file " + tmpLogFilePathName + " could not be created.");
            }
            if (!tmpLogFile.isFile() || !tmpLogFile.canWrite()) {
                throw new Exception("The designated log file " + tmpLogFilePathName + " is not a file or can not be written to.");
            }
            LogUtil.logFile = tmpLogFile;
            LogUtil.fileHandler = new FileHandler(tmpLogFilePathName, true);
            LogUtil.fileHandler.setFormatter(new SimpleFormatter());
            LogUtil.ROOT_LOGGER.addHandler(LogUtil.fileHandler);

            //Start new logging session
            LogUtil.ROOT_LOGGER.info(String.format(BasicDefinitions.MORTAR_SESSION_START_FORMAT, BasicDefinitions.MORTAR_VERSION));

            //TODO: This will also come into affect if a key is missing in the language bundle!
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread aThread, Throwable aThrowable) {
                    Logger.getLogger(aThread.getClass().getName()).log(Level.SEVERE, aThrowable.toString(), aThrowable);
                    if (aThread.getName().equals("main")) {
                        JOptionPane.showMessageDialog(
                                null,
                                Message.get("Error.UnknownError"),
                                Message.get("Error.Notification.Title"),
                                JOptionPane.ERROR_MESSAGE
                        );
                        System.exit(-1);
                    }
                }
            });
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
                return LogUtil.initializeLoggingEnvironment();
            } else {
                return false;
            }
        } catch (Exception anException) {
            LogUtil.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return false;
        }
    }
    // </editor-fold>
}
