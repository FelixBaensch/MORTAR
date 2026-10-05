/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2026  Felix Baensch, Jonas Schaub (felix.j.baensch@gmail.com, jonas.schaub@uni-jena.de)
 *
 * Source code is available at <https://github.com/FelixBaensch/MORTAR>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.unijena.cheminf.mortar.model.util;

import de.unijena.cheminf.mortar.configuration.Configuration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Tests for the logging utilities in LogUtil. Every environment-coupled test uses the mandatory isolation technique:
 * {@link AppDirTestUtil#redirectAppDirPath(java.nio.file.Path)} points the application data directory at a JUnit
 * {@link TempDir} (on every operating system, including Windows), and in a finally block the original
 * {@code user.home} is restored, the {@code FileUtil.appDirPath} cache is cleared again, and
 * every {@link java.util.logging.FileHandler} the test opened on the root logger is closed and removed again
 * (via {@link TestUtil#releaseRootLoggerFileHandlers()}, deliberately narrower than a JVM-wide
 * {@code LogManager.reset()}, which would silence logging for every later test in the same JVM). Because
 * {@code initializeLoggingEnvironment()} also removes every root handler (including the console handler), sets the
 * root level and installs the JVM default uncaught-exception handler, the root handlers, the root level and the
 * default uncaught-exception handler are snapshotted before each test and restored after it. As a result no real
 * {@code ~/MORTAR} directory is created and no global logging state leaks into other tests. Only the safe
 * logging-only paths are exercised: the GUI / error / {@code System.exit} branches of the uncaught-exception handler
 * are never driven (they would block a headless run or kill the JVM).
 *
 * @author Felix Baensch
 */
class LogUtilTest {
    //<editor-fold desc="Private static final class constants" defaultstate="collapsed">
    /**
     * Message marker that makes the uncaught-exception handler return early (the JavaFX GUI thread deals with such
     * bidirectional-binding failures itself, so the handler must not intervene).
     */
    private static final String BINDING_FAILURE_MESSAGE = "Bidirectional binding failed, setting to the previous value";
    /**
     * Name of the logger the uncaught-exception handler logs to: the class name of the thread it is handed.
     */
    private static final String THREAD_LOGGER_NAME = Thread.class.getName();
    //</editor-fold>
    //
    //<editor-fold desc="Private static class variables" defaultstate="collapsed">
    /**
     * Default locale before this test class ran, restored after all tests.
     */
    private static Locale originalLocale;
    //</editor-fold>
    //
    //<editor-fold desc="Private class variables" defaultstate="collapsed">
    /**
     * Root logger handlers before the current test, restored after it.
     */
    private Handler[] originalRootHandlers;
    /**
     * Root logger level before the current test, restored after it.
     */
    private Level originalRootLevel;
    /**
     * JVM default uncaught-exception handler before the current test, restored after it.
     */
    private Thread.UncaughtExceptionHandler originalDefaultUncaughtExceptionHandler;
    //</editor-fold>
    //
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Constructor that bootstraps the Configuration singleton from the classpath (no data directory is touched by
     * this).
     *
     * @throws Exception if the Configuration singleton cannot be initialized
     */
    public LogUtilTest() throws Exception {
        Configuration.getInstance();
    }
    //</editor-fold>
    //
    //<editor-fold desc="Setup and teardown" defaultstate="collapsed">
    /**
     * Sets the default locale to en-GB (so any message-bundle resolution is deterministic), remembering the original.
     */
    @BeforeAll
    public static void setLocale() {
        LogUtilTest.originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.of("en", "GB"));
    }
    //
    /**
     * Restores the default locale that was in place before this test class ran.
     */
    @AfterAll
    public static void restoreLocale() {
        Locale.setDefault(LogUtilTest.originalLocale);
    }
    //
    /**
     * Snapshots the root logger handlers, the root logger level and the JVM default uncaught-exception handler, all of
     * which initializeLoggingEnvironment() overwrites.
     */
    @BeforeEach
    public void snapshotLoggingState() {
        Logger tmpRootLogger = LogManager.getLogManager().getLogger("");
        this.originalRootHandlers = tmpRootLogger.getHandlers();
        this.originalRootLevel = tmpRootLogger.getLevel();
        this.originalDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    }
    //
    /**
     * Closes the file handlers a test opened and restores the root logger handlers, the root logger level and the JVM
     * default uncaught-exception handler snapshotted before the test.
     */
    @AfterEach
    public void restoreLoggingState() {
        TestUtil.releaseRootLoggerFileHandlers();
        Logger tmpRootLogger = LogManager.getLogManager().getLogger("");
        for (Handler tmpHandler : tmpRootLogger.getHandlers()) {
            tmpRootLogger.removeHandler(tmpHandler);
        }
        for (Handler tmpHandler : this.originalRootHandlers) {
            tmpRootLogger.addHandler(tmpHandler);
        }
        tmpRootLogger.setLevel(this.originalRootLevel);
        Thread.setDefaultUncaughtExceptionHandler(this.originalDefaultUncaughtExceptionHandler);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Test methods" defaultstate="collapsed">
    /**
     * Tests that initializeLoggingEnvironment, driven against a redirected temporary user home, returns true, creates
     * the log file directory under the temporary home (verified through getLogFileDirectoryPath), and that a log file is
     * actually created there. The global logging state is fully restored after the test.
     *
     * @param aTempHome temporary directory used as a fake user home
     */
    @Test
    public void testInitializeLoggingEnvironment(@TempDir Path aTempHome) throws Exception {
        String tmpOldHome = System.getProperty("user.home");
        try {
            AppDirTestUtil.redirectAppDirPath(aTempHome);
            boolean tmpInitialized = LogUtil.initializeLoggingEnvironment();
            Assertions.assertTrue(tmpInitialized);
            String tmpLogDirPath = LogUtil.getLogFileDirectoryPath();
            Assertions.assertTrue(tmpLogDirPath.startsWith(aTempHome.toString()),
                    "Log file directory was not resolved under the temporary home: " + tmpLogDirPath);
            File tmpLogDir = new File(tmpLogDirPath);
            Assertions.assertTrue(tmpLogDir.isDirectory());
            File[] tmpLogFiles = tmpLogDir.listFiles((dir, name) -> name.endsWith(".txt"));
            Assertions.assertNotNull(tmpLogFiles);
            Assertions.assertTrue(tmpLogFiles.length > 0, "No log file was created in the temporary log directory.");
        } finally {
            AppDirTestUtil.restoreAppDirPath(tmpOldHome);
        }
    }
    //
    /**
     * Tests that resetLogFile, after initializing the logging environment against a redirected temporary user home,
     * returns true and a log file still exists afterwards. The global logging state is fully restored in the finally
     * block.
     *
     * @param aTempHome temporary directory used as a fake user home
     */
    @Test
    public void testResetLogFile(@TempDir Path aTempHome) throws Exception {
        String tmpOldHome = System.getProperty("user.home");
        try {
            AppDirTestUtil.redirectAppDirPath(aTempHome);
            Assertions.assertTrue(LogUtil.initializeLoggingEnvironment());
            boolean tmpReset = LogUtil.resetLogFile();
            Assertions.assertTrue(tmpReset);
            File tmpLogDir = new File(LogUtil.getLogFileDirectoryPath());
            File[] tmpLogFiles = tmpLogDir.listFiles((dir, name) -> name.endsWith(".txt"));
            Assertions.assertNotNull(tmpLogFiles);
            Assertions.assertTrue(tmpLogFiles.length > 0, "No log file existed after reset.");
        } finally {
            AppDirTestUtil.restoreAppDirPath(tmpOldHome);
        }
    }
    //
    /**
     * Tests manageLogFilesFolderIfExists against a redirected temporary user home: it returns early without throwing
     * when the log directory does not exist; when the directory holds more .txt log files than the upper limit plus a
     * leftover .lck file, it deletes the .lck file and trims the folder by deleting the oldest
     * {@link BasicDefinitions#FACTOR_TO_TRIM_LOG_FILE_FOLDER} share of the log files (by last-modified time), keeping
     * the newest ones. The global logging state is fully restored after the test.
     *
     * @param aTempHome temporary directory used as a fake user home
     */
    @Test
    public void testManageLogFilesFolderIfExists(@TempDir Path aTempHome) throws Exception {
        String tmpOldHome = System.getProperty("user.home");
        try {
            AppDirTestUtil.redirectAppDirPath(aTempHome);
            //early-return path: log directory does not exist yet
            File tmpLogDir = new File(LogUtil.getLogFileDirectoryPath());
            Assertions.assertFalse(tmpLogDir.exists());
            Assertions.assertDoesNotThrow(LogUtil::manageLogFilesFolderIfExists);
            Assertions.assertFalse(tmpLogDir.exists());
            //populate the log directory with one file more than the upper limit, oldest first, plus a .lck leftover
            Assertions.assertTrue(FileUtil.createDirectory(tmpLogDir.getAbsolutePath()));
            int tmpFileCount = BasicDefinitions.UPPER_LIMIT_OF_LOG_FILES + 1;
            long tmpBaseTime = System.currentTimeMillis() - 1_000_000L;
            for (int i = 0; i < tmpFileCount; i++) {
                File tmpLogFile = new File(tmpLogDir, LogUtilTest.logFileName(i));
                Assertions.assertTrue(tmpLogFile.createNewFile());
                Assertions.assertTrue(tmpLogFile.setLastModified(tmpBaseTime + i * 10_000L));
            }
            File tmpLckFile = new File(tmpLogDir, LogUtilTest.logFileName(0) + ".lck");
            Assertions.assertTrue(tmpLckFile.createNewFile());
            LogUtil.manageLogFilesFolderIfExists();
            Assertions.assertFalse(tmpLckFile.exists(), "the .lck leftover was not deleted");
            //one pass deletes ceil(count * factor) of the oldest files; the remainder is within the limit
            int tmpExpectedDeleted = (int) Math.ceil(tmpFileCount * BasicDefinitions.FACTOR_TO_TRIM_LOG_FILE_FOLDER);
            String[] tmpRemaining = tmpLogDir.list((dir, name) -> name.endsWith(".txt"));
            Assertions.assertNotNull(tmpRemaining);
            Set<String> tmpRemainingNames = Arrays.stream(tmpRemaining).collect(Collectors.toSet());
            Assertions.assertEquals(tmpFileCount - tmpExpectedDeleted, tmpRemainingNames.size());
            for (int i = 0; i < tmpFileCount; i++) {
                Assertions.assertEquals(i >= tmpExpectedDeleted, tmpRemainingNames.contains(LogUtilTest.logFileName(i)),
                        "unexpected presence state of " + LogUtilTest.logFileName(i));
            }
        } finally {
            AppDirTestUtil.restoreAppDirPath(tmpOldHome);
        }
    }
    //
    /**
     * Tests checkForLCKFileInLogDir against a redirected temporary user home: an existing log directory without any
     * .lck file returns false, and after creating a *.lck file it returns true. The global logging state is fully
     * restored after the test.
     *
     * @param aTempHome temporary directory used as a fake user home
     */
    @Test
    public void testCheckForLCKFileInLogDir(@TempDir Path aTempHome) throws Exception {
        String tmpOldHome = System.getProperty("user.home");
        try {
            AppDirTestUtil.redirectAppDirPath(aTempHome);
            File tmpLogDir = new File(LogUtil.getLogFileDirectoryPath());
            Assertions.assertTrue(FileUtil.createDirectory(tmpLogDir.getAbsolutePath()));
            //no .lck file -> false
            Assertions.assertFalse(LogUtil.checkForLCKFileInLogDir());
            //create a .lck file -> true
            Assertions.assertTrue(new File(tmpLogDir, "MORTAR_Log_test.txt.lck").createNewFile());
            Assertions.assertTrue(LogUtil.checkForLCKFileInLogDir());
        } finally {
            AppDirTestUtil.restoreAppDirPath(tmpOldHome);
        }
    }
    //
    /**
     * Tests that getUncaughtExceptionHandler returns a non-null handler.
     */
    @Test
    public void testGetUncaughtExceptionHandler() throws Exception {
        Assertions.assertNotNull(LogUtil.getUncaughtExceptionHandler());
    }
    //
    /**
     * Tests the two safe, logging-only paths of the uncaught-exception handler. The handler is invoked with a thread
     * that belongs to a dedicated, non-"main" thread group (so the generic-exception case logs only and never reaches
     * the JavaFX GUI / {@code System.exit} branch, which is reserved for the main thread group): a throwable carrying
     * the bidirectional-binding-failure marker message hits the early-return path, and a generic (non-error) exception
     * hits the SEVERE-log path. Both paths log exactly one SEVERE record carrying the handed throwable to the logger
     * named after the thread's class. The error branch and the main-thread branch are intentionally never driven.
     */
    @Test
    public void testUncaughtExceptionHandlerLoggingPaths() throws Exception {
        Thread.UncaughtExceptionHandler tmpHandler = LogUtil.getUncaughtExceptionHandler();
        //a dedicated thread group named "test" (NOT "main") keeps the generic-exception case on the logging-only path
        ThreadGroup tmpTestGroup = new ThreadGroup("test");
        Thread tmpWorkerThread = new Thread(tmpTestGroup, () -> { }, "logUtilTestWorker");
        Assertions.assertEquals("test", tmpWorkerThread.getThreadGroup().getName());
        //binding-failure marker -> early return, logging only
        RuntimeException tmpBindingFailure = new RuntimeException(LogUtilTest.BINDING_FAILURE_MESSAGE);
        List<LogRecord> tmpBindingRecords = TestUtil.captureLogRecords(LogUtilTest.THREAD_LOGGER_NAME, Level.SEVERE,
                () -> tmpHandler.uncaughtException(tmpWorkerThread, tmpBindingFailure));
        Assertions.assertEquals(1, tmpBindingRecords.size());
        Assertions.assertSame(tmpBindingFailure, tmpBindingRecords.getFirst().getThrown());
        //generic non-main-thread-group exception -> SEVERE log path, no GUI / no exit
        RuntimeException tmpGenericException = new RuntimeException("generic test exception");
        List<LogRecord> tmpGenericRecords = TestUtil.captureLogRecords(LogUtilTest.THREAD_LOGGER_NAME, Level.SEVERE,
                () -> tmpHandler.uncaughtException(tmpWorkerThread, tmpGenericException));
        Assertions.assertEquals(1, tmpGenericRecords.size());
        Assertions.assertEquals(Level.SEVERE, tmpGenericRecords.getFirst().getLevel());
        Assertions.assertSame(tmpGenericException, tmpGenericRecords.getFirst().getThrown());
        Assertions.assertEquals(tmpGenericException.toString(), tmpGenericRecords.getFirst().getMessage());
    }
    //
    /**
     * Tests that the private parameter-less constructor of the LogUtil utility class can be invoked reflectively.
     */
    @Test
    public void privateConstructorTest() throws Exception {
        TestUtil.assertPrivateConstructorIsInvocable(LogUtil.class);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private methods" defaultstate="collapsed">
    /**
     * Returns the name of the i-th synthetic log file, zero-padded so names sort like their indices.
     *
     * @param anIndex index of the log file
     * @return file name of the log file
     */
    private static String logFileName(int anIndex) {
        return String.format(Locale.ROOT, "MORTAR_Log_test_%03d.txt", anIndex);
    }
    //</editor-fold>
}
