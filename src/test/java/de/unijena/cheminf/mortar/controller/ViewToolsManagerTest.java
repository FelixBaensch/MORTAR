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

package de.unijena.cheminf.mortar.controller;

import de.unijena.cheminf.mortar.configuration.Configuration;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import de.unijena.cheminf.mortar.model.util.AppDirTestUtil;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import de.unijena.cheminf.mortar.model.util.TestUtil;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Direct, headless unit tests for {@link ViewToolsManager}. The manager constructs its two sub-controllers
 * ({@code HistogramViewController} and {@code OverviewViewController}), validates their settings via the private
 * {@code checkViewTools()} routine, persists/reloads those settings to and from preference files, and exposes a couple
 * of cached-index delegations. None of that requires a booted JavaFX {@code Stage}, so this test deliberately does
 * <em>not</em> extend {@link AbstractFxTestCase} (honoring the "no FX harness dependency" lock for the
 * no-toolkit-reachable classes). It only pins the {@code en-GB} default locale and bootstraps the {@link Configuration}
 * singleton so the message-bundle lookups inside the sub-controller constructors resolve deterministically.
 * <p>
 * Every filesystem-touching test is isolated to a JUnit {@link TempDir} by redirecting the {@code user.home} system
 * property and reflectively resetting the private static {@code appDirPath} cache of {@link FileUtil}, with the original
 * state always restored and the log manager reset in a finally block, so the real {@code ~/MORTAR} directory is never
 * written to or deleted.
 * <p>
 * The two GUI-alert error branches of {@code persistViewToolsSettings} (the non-writable-directory guard and the
 * per-view-tool persistence-failure catch) cannot complete headless because they construct a JavaFX {@code Alert},
 * which throws "Toolkit not initialized" without a booted toolkit. Those two branches are driven with
 * {@link org.mockito.MockedStatic} neutralizing the static {@code GuiUtil} alert calls (the project's no-mock default is
 * extended to {@code controller/} under the QUAL-02 allowance for GUI-bound error branches), while the failing
 * condition itself is provoked with a real filesystem trap (a read-only directory, and a directory placed where a
 * preference file is expected). The Stage-taking delegations ({@code openHistogramView}/{@code openOverviewView}) and
 * the constructor's {@code checkViewTools} catch remain uncovered: they require a live {@code Stage} or an injected
 * validation failure and are outside the no-toolkit scope. The covered set exceeds the 80% line-coverage target.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class ViewToolsManagerTest {
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Constructor that sets the default locale to en-GB (so the sub-controller settings display names, which are
     * resolved from the message.properties file during instantiation, are deterministic) and bootstraps the
     * Configuration singleton from the classpath (no data directory is touched by this).
     *
     * @throws Exception if the Configuration singleton cannot be initialized
     */
    public ViewToolsManagerTest() throws Exception {
        Locale.setDefault(Locale.of("en", "GB"));
        Configuration.getInstance();
    }
    //</editor-fold>
    //
    //<editor-fold desc="Test methods" defaultstate="collapsed">
    /**
     * Tests the settings persist/reload round-trip under {@link TempDir} isolation. A manager is constructed headless
     * (exercising the constructor, both sub-controller constructions and the {@code checkViewTools} happy path), the
     * first boolean setting of its first sub-controller is flipped to a non-default value, and the manager persists all
     * view-tool settings to preference files under the temp-dir-isolated settings directory. A fresh manager then
     * reloads those settings, and the flipped value must round-trip back through the reloaded instance's matching
     * property. The {@code user.home} system property and the {@code appDirPath} cache are always restored and the log
     * manager reset in a finally block, so the real {@code ~/MORTAR} directory is never touched and no logger handler
     * leaks into sibling tests.
     *
     * @param aTempHome temporary directory used as a fake user home
     * @throws Exception if anything goes wrong
     */
    @Test
    public void persistAndReloadRoundTrip(@TempDir Path aTempHome) throws Exception {
        String tmpOldHome = System.getProperty("user.home");
        try {
            AppDirTestUtil.redirectAppDirPath(aTempHome);
            ViewToolsManager tmpManager = new ViewToolsManager(Configuration.getInstance(), new SettingsContainer());
            //locate the first boolean setting of the first sub-controller and flip it to a non-default value
            IViewToolController tmpFirstTool = tmpManager.getViewToolControllers()[0];
            BooleanProperty tmpFlippedProperty = ViewToolsManagerTest.findFirstBooleanProperty(tmpFirstTool.settingsProperties());
            Assertions.assertNotNull(tmpFlippedProperty, "Expected at least one boolean setting on the first view tool.");
            String tmpFlippedName = tmpFlippedProperty.getName();
            boolean tmpMutatedValue = !tmpFlippedProperty.get();
            tmpFlippedProperty.set(tmpMutatedValue);
            tmpManager.persistViewToolsSettings();
            //fresh manager reloads from the temp-dir-isolated settings directory
            ViewToolsManager tmpReloaded = new ViewToolsManager(Configuration.getInstance(), new SettingsContainer());
            tmpReloaded.reloadViewToolsSettings();
            BooleanProperty tmpReloadedProperty = ViewToolsManagerTest.findBooleanPropertyByName(
                    tmpReloaded.getViewToolControllers()[0].settingsProperties(), tmpFlippedName);
            Assertions.assertNotNull(tmpReloadedProperty, "Reloaded view tool must expose the same boolean setting.");
            Assertions.assertEquals(tmpMutatedValue, tmpReloadedProperty.get());
        } finally {
            AppDirTestUtil.restoreAppDirPath(tmpOldHome);
            TestUtil.releaseRootLoggerFileHandlers();
        }
    }
    //
    /**
     * Tests that reloading when no settings files have been persisted yet leaves every view tool at its defaults and
     * completes without throwing (exercising the "no persisted settings" branch of {@code reloadViewToolsSettings} for
     * both view tools). The default value of the first boolean setting is captured before and after the reload and must
     * be unchanged. Isolated to a {@link TempDir} exactly as the round-trip test.
     *
     * @param aTempHome temporary directory used as a fake user home
     * @throws Exception if anything goes wrong
     */
    @Test
    public void reloadWithoutPersistedFilesKeepsDefaults(@TempDir Path aTempHome) throws Exception {
        String tmpOldHome = System.getProperty("user.home");
        try {
            AppDirTestUtil.redirectAppDirPath(aTempHome);
            ViewToolsManager tmpManager = new ViewToolsManager(Configuration.getInstance(), new SettingsContainer());
            BooleanProperty tmpProperty = ViewToolsManagerTest.findFirstBooleanProperty(
                    tmpManager.getViewToolControllers()[0].settingsProperties());
            Assertions.assertNotNull(tmpProperty);
            boolean tmpDefault = tmpProperty.get();
            //no files exist under the settings dir, so reload must fall back to defaults for every view tool
            tmpManager.reloadViewToolsSettings();
            Assertions.assertEquals(tmpDefault, tmpProperty.get());
        } finally {
            AppDirTestUtil.restoreAppDirPath(tmpOldHome);
            TestUtil.releaseRootLoggerFileHandlers();
        }
    }
    //
    /**
     * Tests the cached-structure-index delegations, which are pure field accesses on the overview sub-controller and
     * require no toolkit: on a freshly constructed manager the cached index is -1 (no return-to-structure event
     * occurred), and after a reset it is still -1. No filesystem access, but {@code user.home} is still isolated so the
     * manager construction cannot touch the real data directory.
     *
     * @param aTempHome temporary directory used as a fake user home
     * @throws Exception if anything goes wrong
     */
    @Test
    public void cachedStructureIndexDelegationsAreHeadless(@TempDir Path aTempHome) throws Exception {
        String tmpOldHome = System.getProperty("user.home");
        try {
            AppDirTestUtil.redirectAppDirPath(aTempHome);
            ViewToolsManager tmpManager = new ViewToolsManager(Configuration.getInstance(), new SettingsContainer());
            Assertions.assertEquals(-1, tmpManager.getCachedIndexOfStructureInMoleculeDataModelList());
            tmpManager.resetCachedIndexOfStructureInMoleculeDataModelList();
            Assertions.assertEquals(-1, tmpManager.getCachedIndexOfStructureInMoleculeDataModelList());
        } finally {
            AppDirTestUtil.restoreAppDirPath(tmpOldHome);
            TestUtil.releaseRootLoggerFileHandlers();
        }
    }
    //
    /**
     * Tests that persisting twice in a row exercises the directory-already-exists branch of
     * {@code persistViewToolsSettings} (which deletes the previous files before re-writing). After the second persist a
     * fresh manager must still reload the mutated boolean setting unchanged. Isolated to a {@link TempDir}.
     *
     * @param aTempHome temporary directory used as a fake user home
     * @throws Exception if anything goes wrong
     */
    @Test
    public void secondPersistOverwritesExistingFiles(@TempDir Path aTempHome) throws Exception {
        String tmpOldHome = System.getProperty("user.home");
        try {
            AppDirTestUtil.redirectAppDirPath(aTempHome);
            ViewToolsManager tmpManager = new ViewToolsManager(Configuration.getInstance(), new SettingsContainer());
            BooleanProperty tmpProperty = ViewToolsManagerTest.findFirstBooleanProperty(
                    tmpManager.getViewToolControllers()[0].settingsProperties());
            Assertions.assertNotNull(tmpProperty);
            String tmpName = tmpProperty.getName();
            boolean tmpMutatedValue = !tmpProperty.get();
            tmpProperty.set(tmpMutatedValue);
            //first persist creates the settings subfolder, second persist takes the else (delete-existing) branch
            tmpManager.persistViewToolsSettings();
            tmpManager.persistViewToolsSettings();
            ViewToolsManager tmpReloaded = new ViewToolsManager(Configuration.getInstance(), new SettingsContainer());
            tmpReloaded.reloadViewToolsSettings();
            BooleanProperty tmpReloadedProperty = ViewToolsManagerTest.findBooleanPropertyByName(
                    tmpReloaded.getViewToolControllers()[0].settingsProperties(), tmpName);
            Assertions.assertNotNull(tmpReloadedProperty);
            Assertions.assertEquals(tmpMutatedValue, tmpReloadedProperty.get());
        } finally {
            AppDirTestUtil.restoreAppDirPath(tmpOldHome);
            TestUtil.releaseRootLoggerFileHandlers();
        }
    }
    //
    /**
     * Tests that a corrupt persisted settings file is logged and skipped during reload without aborting the reload of
     * the remaining view tools (exercising the {@code IllegalArgumentException | IOException} catch of
     * {@code reloadViewToolsSettings}). A manager persists valid files, one of them (the first view tool's file) is
     * overwritten with content that cannot be parsed as a preference container, and a fresh manager reloads: the reload
     * must complete without throwing. Isolated to a {@link TempDir}.
     *
     * @param aTempHome temporary directory used as a fake user home
     * @throws Exception if anything goes wrong
     */
    @Test
    public void reloadWithCorruptSettingsFileIsSkipped(@TempDir Path aTempHome) throws Exception {
        String tmpOldHome = System.getProperty("user.home");
        try {
            AppDirTestUtil.redirectAppDirPath(aTempHome);
            ViewToolsManager tmpManager = new ViewToolsManager(Configuration.getInstance(), new SettingsContainer());
            tmpManager.persistViewToolsSettings();
            //corrupt the first view tool's persisted file so its reload throws and is caught/skipped
            String tmpViewToolsDirPath = FileUtil.getSettingsDirPath()
                    + ViewToolsManager.VIEW_TOOLS_SETTINGS_SUBFOLDER_NAME + File.separator;
            String tmpFirstToolClassName = tmpManager.getViewToolControllers()[0].getClass().getSimpleName();
            File tmpCorruptFile = new File(tmpViewToolsDirPath
                    + tmpFirstToolClassName
                    + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION);
            Files.writeString(tmpCorruptFile.toPath(), "not a valid preference container", StandardCharsets.UTF_8);
            ViewToolsManager tmpReloaded = new ViewToolsManager(Configuration.getInstance(), new SettingsContainer());
            //must complete without throwing despite the corrupt file
            tmpReloaded.reloadViewToolsSettings();
            Assertions.assertNotNull(tmpReloaded.getViewToolControllers());
        } finally {
            AppDirTestUtil.restoreAppDirPath(tmpOldHome);
            TestUtil.releaseRootLoggerFileHandlers();
        }
    }
    //
    /**
     * Tests the non-writable-directory guard of {@code persistViewToolsSettings}: when the view-tools settings
     * directory cannot be written, the method must warn the user via {@code GuiUtil.guiMessageAlert} and return without
     * writing any files. The directory is made read-only on the real (temp-isolated) filesystem to provoke the branch,
     * and {@link org.mockito.MockedStatic} neutralizes the JavaFX {@code Alert} construction (which would otherwise
     * throw headless). The alert call is verified. The directory is made writable again in the finally block so the
     * {@link TempDir} can be deleted.
     *
     * @param aTempHome temporary directory used as a fake user home
     * @throws Exception if anything goes wrong
     */
    @Test
    public void persistToNonWritableDirectoryShowsMessageAlert(@TempDir Path aTempHome) throws Exception {
        String tmpOldHome = System.getProperty("user.home");
        File tmpViewToolsDir = null;
        try {
            AppDirTestUtil.redirectAppDirPath(aTempHome);
            ViewToolsManager tmpManager = new ViewToolsManager(Configuration.getInstance(), new SettingsContainer());
            //pre-create the settings subfolder and make it read-only so persist hits the canWrite() guard
            String tmpViewToolsDirPath = FileUtil.getSettingsDirPath()
                    + ViewToolsManager.VIEW_TOOLS_SETTINGS_SUBFOLDER_NAME;
            tmpViewToolsDir = new File(tmpViewToolsDirPath);
            Files.createDirectories(tmpViewToolsDir.toPath());
            //Windows/NTFS ignores the POSIX write bit for directories and File.setWritable(false, false) returns
            //false there, so the guarded branch cannot be driven at all on that platform; skip instead of failing
            Assumptions.assumeTrue(tmpViewToolsDir.setWritable(false, false),
                    "The settings directory could not be made non-writable (e.g. on Windows); "
                            + "cannot exercise the non-writable guard.");
            //when running as root, File.canWrite() ignores the cleared write bit, so the canWrite() guard never fires;
            //skip rather than falsely fail in that environment
            Assumptions.assumeFalse(tmpViewToolsDir.canWrite(),
                    "Directory still writable (likely running as root); cannot exercise the non-writable guard.");
            try (MockedStatic<GuiUtil> tmpGuiUtilMock = Mockito.mockStatic(GuiUtil.class)) {
                tmpManager.persistViewToolsSettings();
                tmpGuiUtilMock.verify(() -> GuiUtil.guiMessageAlert(
                        Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()));
            }
        } finally {
            if (tmpViewToolsDir != null) {
                tmpViewToolsDir.setWritable(true, false);
            }
            AppDirTestUtil.restoreAppDirPath(tmpOldHome);
            TestUtil.releaseRootLoggerFileHandlers();
        }
    }
    //
    /**
     * Tests the per-view-tool persistence-failure catch of {@code persistViewToolsSettings}: when writing a single view
     * tool's preference file fails, the failure must be logged and surfaced via {@code GuiUtil.guiExceptionAlert}. The
     * failure is provoked by placing a directory exactly where the first view tool's preference file is expected, so
     * {@code writeRepresentation} throws an {@code IOException}; {@link org.mockito.MockedStatic} neutralizes the
     * JavaFX {@code Alert} construction (which would otherwise throw headless). The alert call is verified. Isolated to
     * a {@link TempDir}.
     *
     * @param aTempHome temporary directory used as a fake user home
     * @throws Exception if anything goes wrong
     */
    @Test
    public void persistFailureShowsExceptionAlert(@TempDir Path aTempHome) throws Exception {
        String tmpOldHome = System.getProperty("user.home");
        try {
            AppDirTestUtil.redirectAppDirPath(aTempHome);
            ViewToolsManager tmpManager = new ViewToolsManager(Configuration.getInstance(), new SettingsContainer());
            String tmpViewToolsDirPath = FileUtil.getSettingsDirPath()
                    + ViewToolsManager.VIEW_TOOLS_SETTINGS_SUBFOLDER_NAME + File.separator;
            File tmpViewToolsDir = new File(tmpViewToolsDirPath);
            Files.createDirectories(tmpViewToolsDir.toPath());
            //place a directory where the first view tool's preference file is expected: writeRepresentation() will fail
            String tmpFirstToolClassName = tmpManager.getViewToolControllers()[0].getClass().getSimpleName();
            File tmpTrapDirectory = new File(tmpViewToolsDirPath
                    + tmpFirstToolClassName
                    + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION);
            Files.createDirectories(tmpTrapDirectory.toPath());
            try (MockedStatic<GuiUtil> tmpGuiUtilMock = Mockito.mockStatic(GuiUtil.class)) {
                tmpManager.persistViewToolsSettings();
                tmpGuiUtilMock.verify(() -> GuiUtil.guiExceptionAlert(
                        Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any()));
            }
        } finally {
            AppDirTestUtil.restoreAppDirPath(tmpOldHome);
            TestUtil.releaseRootLoggerFileHandlers();
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private methods" defaultstate="collapsed">
    //
    /**
     * Returns the first {@link BooleanProperty} in the given settings list, or null if there is none.
     *
     * @param aSettings the settings properties of a view tool
     * @return the first boolean property, or null
     */
    private static BooleanProperty findFirstBooleanProperty(List<Property<?>> aSettings) {
        for (Property<?> tmpProperty : aSettings) {
            if (tmpProperty instanceof BooleanProperty tmpBooleanProperty) {
                return tmpBooleanProperty;
            }
        }
        return null;
    }
    //
    /**
     * Returns the {@link BooleanProperty} in the given settings list whose name matches the given name, or null if
     * there is none.
     *
     * @param aSettings the settings properties of a view tool
     * @param aName the property name to match
     * @return the matching boolean property, or null
     */
    private static BooleanProperty findBooleanPropertyByName(List<Property<?>> aSettings, String aName) {
        for (Property<?> tmpProperty : aSettings) {
            if (tmpProperty instanceof BooleanProperty tmpBooleanProperty && aName.equals(tmpProperty.getName())) {
                return tmpBooleanProperty;
            }
        }
        return null;
    }
    //</editor-fold>
}
