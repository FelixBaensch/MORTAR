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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Tests for the logging utilities in LogUtil. Every environment-coupled test uses the mandatory isolation technique:
 * {@link AppDirTestUtil#redirectAppDirPath(java.nio.file.Path)} points the application data directory at a JUnit
 * {@link TempDir} (on every operating system, including Windows), and in a finally block the original
 * {@code user.home} is restored, the {@code FileUtil.appDirPath} cache is cleared again, and
 * every {@link java.util.logging.FileHandler} the test opened on the root logger is closed and removed again
 * (via {@link TestUtil#releaseRootLoggerFileHandlers()}, deliberately narrower than a JVM-wide
 * {@code LogManager.reset()}, which would silence logging for every later test in the same JVM). As a result no real
 * {@code ~/MORTAR} directory is created and no global logging handler leaks into other tests. Only the safe
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
    //</editor-fold>
    //
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Constructor that sets the default locale to en-GB (so any message-bundle resolution is deterministic) and
     * bootstraps the Configuration singleton from the classpath (no data directory is touched by this).
     *
     * @throws Exception if the Configuration singleton cannot be initialized
     */
    public LogUtilTest() throws Exception {
        Locale.setDefault(Locale.of("en", "GB"));
        Configuration.getInstance();
    }
    //</editor-fold>
    //
    //<editor-fold desc="Test methods" defaultstate="collapsed">
    /**
     * Tests that initializeLoggingEnvironment, driven against a redirected temporary user home, returns true, creates
     * the log file directory under the temporary home (verified through getLogFileDirectoryPath), and that a log file is
     * actually created there. The global logging state is fully restored in the finally block.
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
            this.restoreGlobalState(tmpOldHome);
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
            this.restoreGlobalState(tmpOldHome);
        }
    }
    //
    /**
     * Tests manageLogFilesFolderIfExists against a redirected temporary user home: it does not throw when the log
     * directory exists and contains a few .txt files, and it returns early (also without throwing) when the log
     * directory does not exist. The global logging state is fully restored in the finally block.
     *
     * @param aTempHome temporary directory used as a fake user home
     */
    @Test
    public void testManageLogFilesFolderIfExists(@TempDir Path aTempHome) throws Exception {
        String tmpOldHome = System.getProperty("user.home");
        try {
            AppDirTestUtil.redirectAppDirPath(aTempHome);
            //early-return path: log directory does not exist yet (only the data dir is created by getLogFileDirectoryPath's parents)
            Assertions.assertDoesNotThrow(LogUtil::manageLogFilesFolderIfExists);
            //populate the log directory with a few .txt files, then call again -> no throw
            File tmpLogDir = new File(LogUtil.getLogFileDirectoryPath());
            Assertions.assertTrue(FileUtil.createDirectory(tmpLogDir.getAbsolutePath()));
            for (int i = 0; i < 3; i++) {
                Assertions.assertTrue(new File(tmpLogDir, "MORTAR_Log_test_" + i + ".txt").createNewFile());
            }
            Assertions.assertDoesNotThrow(LogUtil::manageLogFilesFolderIfExists);
        } finally {
            this.restoreGlobalState(tmpOldHome);
        }
    }
    //
    /**
     * Tests checkForLCKFileInLogDir against a redirected temporary user home: an existing log directory without any
     * .lck file returns false, and after creating a *.lck file it returns true. The global logging state is fully
     * restored in the finally block.
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
            this.restoreGlobalState(tmpOldHome);
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
     * hits the SEVERE-log path. The error branch and the main-thread branch are intentionally never driven.
     */
    @Test
    public void testUncaughtExceptionHandlerLoggingPaths() throws Exception {
        Thread.UncaughtExceptionHandler tmpHandler = LogUtil.getUncaughtExceptionHandler();
        //a dedicated thread group named "test" (NOT "main") keeps the generic-exception case on the logging-only path
        ThreadGroup tmpTestGroup = new ThreadGroup("test");
        Thread tmpWorkerThread = new Thread(tmpTestGroup, () -> { }, "logUtilTestWorker");
        Assertions.assertEquals("test", tmpWorkerThread.getThreadGroup().getName());
        //binding-failure marker -> early return, logging only
        Assertions.assertDoesNotThrow(
                () -> tmpHandler.uncaughtException(tmpWorkerThread, new RuntimeException(BINDING_FAILURE_MESSAGE)));
        //generic non-main-thread-group exception -> SEVERE log path, no GUI / no exit
        Assertions.assertDoesNotThrow(
                () -> tmpHandler.uncaughtException(tmpWorkerThread, new RuntimeException("generic test exception")));
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
    //
    /**
     * Restores the global state mutated by an environment-coupled test: restores the original {@code user.home} system
     * property, nulls the FileUtil app-dir cache, and releases the root logger file handlers so none leaks into other
     * tests.
     *
     * @param anOldUserHome the original value of the {@code user.home} system property
     * @throws Exception if the FileUtil cache field cannot be accessed
     */
    private void restoreGlobalState(String anOldUserHome) throws Exception {
        AppDirTestUtil.restoreAppDirPath(anOldUserHome);
        TestUtil.releaseRootLoggerFileHandlers();
    }
    //</editor-fold>
}
