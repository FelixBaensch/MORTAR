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

import org.junit.jupiter.api.Assertions;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Static helpers shared by the non-GUI test classes, holding the three pieces of test infrastructure that would
 * otherwise be copied into every test class that needs them: invoking the private constructor of a static utility
 * class, capturing the log records a production method publishes, and releasing the log file handlers a test rooted in
 * a temporary user home may have created.
 * <p>
 * The JavaFX controller tests get the equivalent handler cleanup from {@code AbstractFxTestCase}; this class exists so
 * the toolkit-free tests, which cannot extend that base class, do not have to fall back on a JVM-wide
 * {@code LogManager.reset()} (see {@link #releaseRootLoggerFileHandlers()}).
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public final class TestUtil {
    //<editor-fold desc="Private constructor" defaultstate="collapsed">
    /**
     * Private parameter-less constructor; this class only holds static members and must not be instantiated.
     */
    private TestUtil() {
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public static methods" defaultstate="collapsed">
    /**
     * Asserts that the private, parameter-less constructor of the given static utility class can be invoked
     * reflectively and yields an instance. MORTAR declares its utility classes final with a private constructor, which
     * no production code ever calls; this drives that constructor so the class is fully covered.
     *
     * @param aUtilityClass the utility class whose private constructor is invoked
     * @throws Exception if the constructor cannot be found or invoked
     */
    public static void assertPrivateConstructorIsInvocable(Class<?> aUtilityClass) throws Exception {
        Constructor<?> tmpConstructor = aUtilityClass.getDeclaredConstructor();
        tmpConstructor.setAccessible(true);
        Assertions.assertNotNull(tmpConstructor.newInstance(),
                "the private constructor of " + aUtilityClass.getSimpleName() + " must yield an instance");
    }
    //
    /**
     * Runs the given runnable with a capturing {@link Handler} attached to the named logger and returns every
     * {@link LogRecord} of at least the given level that was published while it ran. This makes the branch a production
     * method took observable without depending on the text of a CDK exception message, which is not stable across the
     * moving CDK snapshot: a method that logs {@code SEVERE} in its catch block publishes exactly one record when the
     * failure branch is taken and none when it is not.
     * <p>
     * The handler is always removed again and the logger's original {@code useParentHandlers} flag restored, so no
     * capture leaks into a sibling test.
     *
     * @param aLoggerName name of the logger to capture from, usually {@code SomeClass.class.getName()}
     * @param aLevel minimum level a record must have to be captured
     * @param aRunnable the work to run while capturing
     * @return the captured records, in publication order; never null
     */
    public static List<LogRecord> captureLogRecords(String aLoggerName, Level aLevel, Runnable aRunnable) {
        List<LogRecord> tmpRecords = new ArrayList<>();
        Logger tmpLogger = Logger.getLogger(aLoggerName);
        Handler tmpCapturingHandler = new Handler() {
            @Override
            public void publish(LogRecord aRecord) {
                if (aRecord.getLevel().intValue() >= aLevel.intValue()) {
                    tmpRecords.add(aRecord);
                }
            }
            //
            @Override
            public void flush() {
            }
            //
            @Override
            public void close() {
            }
        };
        boolean tmpUsedParentHandlers = tmpLogger.getUseParentHandlers();
        tmpLogger.addHandler(tmpCapturingHandler);
        //keep the captured records off the console so a deliberately provoked failure does not look like a real one
        tmpLogger.setUseParentHandlers(false);
        try {
            aRunnable.run();
        } finally {
            tmpLogger.removeHandler(tmpCapturingHandler);
            tmpLogger.setUseParentHandlers(tmpUsedParentHandlers);
        }
        return tmpRecords;
    }
    //
    /**
     * Closes and removes every {@link FileHandler} on the root logger, releasing any log file a test rooted in a
     * temporary user home has opened so the temporary directory can be deleted (notably on Windows).
     * <p>
     * This is deliberately narrower than {@code LogManager.getLogManager().reset()}, which closes and removes the
     * handlers of every logger in the entire JVM and never restores them, so the first test to call it silences logging
     * for every test that runs after it in the same JVM. {@code AbstractFxTestCase} does exactly this for the JavaFX
     * tests; this method is the same cleanup for the toolkit-free ones.
     */
    public static void releaseRootLoggerFileHandlers() {
        Logger tmpRootLogger = LogManager.getLogManager().getLogger("");
        if (tmpRootLogger == null) {
            return;
        }
        for (Handler tmpHandler : tmpRootLogger.getHandlers()) {
            if (tmpHandler instanceof FileHandler) {
                tmpHandler.close();
                tmpRootLogger.removeHandler(tmpHandler);
            }
        }
    }
    //</editor-fold>
}
