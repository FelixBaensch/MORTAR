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
import de.unijena.cheminf.mortar.configuration.IConfiguration;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Static test helper that redirects the application data directory of {@link FileUtil} into a temporary directory, so
 * that no test ever reads or writes the real MORTAR data directory of the developer or CI account running it.
 * <p>
 * Setting the {@code user.home} system property alone is not sufficient: {@link FileUtil#getAppDirPath()} only falls
 * back to {@code user.home} on Linux and macOS and reads the {@code AppData} environment variable on Windows, which a
 * test cannot change. This helper therefore additionally pins the private static {@code appDirPath} cache of
 * {@link FileUtil} (reflectively) to the temporary directory, which makes the redirect effective on every operating
 * system. The pinned path mirrors exactly what {@link FileUtil#getAppDirPath()} would compute for the temporary home,
 * including the {@code Library/Application Support} nesting on macOS, so the resolved paths are unchanged on the
 * platforms where the {@code user.home} redirect already worked.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public final class AppDirTestUtil {
    //<editor-fold desc="Private static final class constants" defaultstate="collapsed">
    /**
     * Name of the private static field of FileUtil that caches the resolved application data directory path.
     */
    private static final String APP_DIR_PATH_FIELD_NAME = "appDirPath";
    //</editor-fold>
    //
    //<editor-fold desc="Private constructor" defaultstate="collapsed">
    /**
     * Private parameter-less constructor; this class is a static utility class.
     */
    private AppDirTestUtil() {
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public static methods" defaultstate="collapsed">
    /**
     * Redirects the application data directory into the given temporary directory: the {@code user.home} system
     * property is set to it and the {@code appDirPath} cache of {@link FileUtil} is pinned to the data directory
     * underneath it, which is created if it does not exist yet. Every subsequent call to
     * {@link FileUtil#getAppDirPath()} and its derivatives returns that temporary location, on every operating system.
     * Must be paired with {@link #restoreAppDirPath(String)} in a finally block.
     *
     * @param aTempHome temporary directory to be used as a fake user home
     * @throws Exception if the temporary data directory cannot be created or the cache field cannot be written
     */
    public static void redirectAppDirPath(Path aTempHome) throws Exception {
        System.setProperty("user.home", aTempHome.toString());
        IConfiguration tmpConfiguration = Configuration.getInstance();
        Path tmpAppDirPath = aTempHome;
        if (System.getProperty("os.name").toUpperCase(Locale.ENGLISH).contains("MAC")) {
            tmpAppDirPath = tmpAppDirPath.resolve("Library").resolve("Application Support");
        }
        tmpAppDirPath = tmpAppDirPath.resolve(tmpConfiguration.getProperty("mortar.vendor.name"))
                .resolve(tmpConfiguration.getProperty("mortar.dataDirectory.name"));
        Files.createDirectories(tmpAppDirPath);
        AppDirTestUtil.setAppDirPathCache(tmpAppDirPath.toAbsolutePath().toString());
    }
    //
    /**
     * Restores the given original {@code user.home} system property (if it is not null) and clears the pinned
     * {@code appDirPath} cache of {@link FileUtil}, so the next call to {@link FileUtil#getAppDirPath()} resolves the
     * real application data directory again.
     *
     * @param anOldUserHome the {@code user.home} value captured before the redirect; ignored if null
     * @throws Exception if the cache field cannot be written
     */
    public static void restoreAppDirPath(String anOldUserHome) throws Exception {
        if (anOldUserHome != null) {
            System.setProperty("user.home", anOldUserHome);
        }
        AppDirTestUtil.setAppDirPathCache(null);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private static methods" defaultstate="collapsed">
    /**
     * Reflectively writes the private static {@code appDirPath} cache of {@link FileUtil}.
     *
     * @param anAppDirPath path to pin the cache to or null to clear it
     * @throws Exception if the field cannot be accessed or written
     */
    private static void setAppDirPathCache(String anAppDirPath) throws Exception {
        Field tmpField = FileUtil.class.getDeclaredField(AppDirTestUtil.APP_DIR_PATH_FIELD_NAME);
        tmpField.setAccessible(true);
        tmpField.set(null, anAppDirPath);
    }
    //</editor-fold>
}
