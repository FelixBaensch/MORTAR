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

import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Shared, package-private test-support seam for the {@link MainViewController} headless test classes
 * ({@code MainViewControllerTest}, {@code MainViewControllerHarnessTest} and {@code MainViewControllerExportTest}).
 * Before this class existed, the construction seam, the private-field reflection convention, the bounded background
 * thread join, the import-and-drain drive and the fragments/itemization result-tab fixtures were copy-pasted across all
 * three test classes and had begun to drift; consolidating them here means a change to any of those conventions is made
 * once. Every helper is {@code static} and reads/writes the controller's private state via reflection, so NO production
 * visibility is widened beyond what Phase 16 already exposes (see {@code MainViewControllerExportTest} IN-02 note).
 * <p>
 * The FX-thread contract of the original per-class copies is preserved verbatim: {@link #constructController},
 * {@link #hideStage} and {@link #importFileAndDrain} marshal onto the JavaFX Application Thread themselves (via
 * {@link AbstractFxTestCase#runAndWait(Runnable)}), while the state-shaping fixtures
 * ({@link #setUpSelectedFragmentsTab}, {@link #setUpPopulatedFragmentsAndItems}) and the pure reflection accessors MUST
 * be invoked by the caller on the JavaFX Application Thread. This class is {@code final} with a private no-argument
 * constructor and only static members, following the MORTAR utility-class convention, and intentionally carries NO
 * {@code Test} suffix so JUnit does not treat it as a test class.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
final class MainViewControllerTestSupport {
    //<editor-fold desc="Private static final class constants" defaultstate="collapsed">
    /**
     * A minimal, valid single-molecule SMILES line (benzene) so a real import produces exactly one molecule without
     * depending on any committed test resource. Package-private and shared: this is the single definition used by
     * every {@link MainViewController} test class of this package, which each write it to their own temporary
     * {@code .smi} fixture.
     */
    static final String BENZENE_SMILES_LINE = "c1ccccc1 benzene\n";
    /**
     * Bounded wait (in milliseconds) applied when joining a background thread, mirroring the harness's own 10-second
     * bound so a stuck operation fails fast instead of hanging the CI build.
     */
    private static final long JOIN_TIMEOUT_MILLIS = 10_000L;
    //</editor-fold>
    //
    //<editor-fold desc="Private constructor" defaultstate="collapsed">
    /**
     * Private constructor that prevents instantiation of this static-only utility class.
     */
    private MainViewControllerTestSupport() {
    }
    //</editor-fold>
    //
    //<editor-fold desc="Construction and stage lifecycle" defaultstate="collapsed">
    /**
     * Constructs a {@link MainViewController} on the JavaFX Application Thread over a fresh, caller-owned {@link Stage}
     * (stored into the given reference so the caller can hide it in a {@code finally} block) via the shared
     * {@link FxTestUtil#newMainViewController(Stage, String)} seam. The application directory is the per-test isolated
     * {@code user.home} (redirected to a {@code @TempDir} by {@link AbstractFxTestCase}), which is guaranteed to exist.
     * <p>
     * NOTE: this support class does NOT extend {@link AbstractFxTestCase}, so it cannot set that redirect up itself.
     * The calling test class MUST extend {@link AbstractFxTestCase}; only then is the toolkit booted, is
     * {@code user.home} pointed at a per-test temporary directory, and does this method construct the controller
     * against an isolated application directory instead of the developer's real MORTAR data directory.
     *
     * @param aStageReference sink that receives the created primary stage so it can be hidden after the test
     * @return the constructed root controller
     * @throws Exception if construction fails on the FX thread
     */
    static MainViewController constructController(AtomicReference<Stage> aStageReference) throws Exception {
        AtomicReference<MainViewController> tmpControllerReference = new AtomicReference<>();
        AbstractFxTestCase.runAndWait(() -> {
            Stage tmpPrimaryStage = new Stage();
            aStageReference.set(tmpPrimaryStage);
            try {
                tmpControllerReference.set(FxTestUtil.newMainViewController(tmpPrimaryStage, System.getProperty("user.home")));
            } catch (IOException anException) {
                throw new RuntimeException(anException);
            }
        });
        return tmpControllerReference.get();
    }
    //
    /**
     * Hides the primary stage held by the given reference on the JavaFX Application Thread. {@code Stage.hide()} does
     * NOT fire the window close-request handler, so the controller's {@code closeApplication}/{@code System.exit} path
     * is never reached.
     *
     * @param aStageReference reference to the primary stage to hide (may hold null if construction failed)
     * @throws Exception if hiding fails on the FX thread
     */
    static void hideStage(AtomicReference<Stage> aStageReference) throws Exception {
        AbstractFxTestCase.runAndWait(() -> {
            Stage tmpStage = aStageReference.get();
            if (tmpStage != null) {
                tmpStage.hide();
            }
        });
    }
    //</editor-fold>
    //
    //<editor-fold desc="Import drive and background thread join" defaultstate="collapsed">
    /**
     * Drives {@code importMoleculeFile(File)} for the given file on the FX thread (alerts mocked), joins the background
     * importer thread for determinism, then drains the nested {@code Platform.runLater} success/failure callbacks.
     *
     * @param aController the controller under test
     * @param aFile the molecule file to import
     * @throws Exception if anything goes wrong on the FX thread
     */
    static void importFileAndDrain(MainViewController aController, File aFile) throws Exception {
        AbstractFxTestCase.runAndWait(() -> {
            try (MockedStatic<GuiUtil> tmpGuiUtilMock = FxTestUtil.mockGuiAlerts()) {
                aController.importMoleculeFile(aFile);
            }
        });
        MainViewControllerTestSupport.joinThreadField(aController, "importerThread");
        AbstractFxTestCase.waitForFxEvents();
        AbstractFxTestCase.waitForFxEvents();
        AbstractFxTestCase.runAndWait(() -> { });
    }
    //
    /**
     * Reflectively joins a named background thread field of the controller (bounded), so a started import/fragmentation
     * is complete before the FX-thread success callback is drained (removes the async-callback race).
     *
     * @param aController the controller under test
     * @param aFieldName the name of the {@link Thread} field to join
     * @throws Exception if the join is interrupted
     */
    static void joinThreadField(MainViewController aController, String aFieldName) throws Exception {
        Thread tmpThread = (Thread) MainViewControllerTestSupport.getField(aController, aFieldName);
        if (tmpThread != null) {
            tmpThread.join(MainViewControllerTestSupport.JOIN_TIMEOUT_MILLIS);
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private-field reflection accessors" defaultstate="collapsed">
    /**
     * Reflectively reads the value of a private field of the given controller (no production code is widened).
     *
     * @param aController the controller under test
     * @param aFieldName the name of the field to read
     * @return the current field value
     */
    static Object getField(MainViewController aController, String aFieldName) {
        try {
            Field tmpField = MainViewController.class.getDeclaredField(aFieldName);
            tmpField.setAccessible(true);
            return tmpField.get(aController);
        } catch (ReflectiveOperationException anException) {
            throw new RuntimeException("Could not read field " + aFieldName + " via reflection", anException);
        }
    }
    //
    /**
     * Reflectively sets the value of a private field of the given controller (no production code is widened), so the
     * interrupt/close/export paths can be driven with prepared stand-in state.
     *
     * @param aController the controller under test
     * @param aFieldName the name of the field to write
     * @param aValue the value to set
     */
    static void setField(MainViewController aController, String aFieldName, Object aValue) {
        try {
            Field tmpField = MainViewController.class.getDeclaredField(aFieldName);
            tmpField.setAccessible(true);
            tmpField.set(aController, aValue);
        } catch (ReflectiveOperationException anException) {
            throw new RuntimeException("Could not write field " + aFieldName + " via reflection", anException);
        }
    }
    //
    /**
     * Reflectively reads the controller's private {@code moleculeDataModelList} field (no production code is widened),
     * so a test can assert or manipulate the imported-molecule invariant.
     *
     * @param aController the controller instance
     * @return the controller's observable molecule data model list
     */
    @SuppressWarnings("unchecked")
    static List<MoleculeDataModel> getMoleculeList(MainViewController aController) {
        return (List<MoleculeDataModel>) MainViewControllerTestSupport.getField(aController, "moleculeDataModelList");
    }
    //
    /**
     * Reflectively reads the controller's private {@code mapOfFragmentDataModelLists} field (no production code is
     * widened), so a test can assert on or populate the fragmentation results.
     *
     * @param aController the controller instance
     * @return the controller's map of fragmentation-name to fragment list
     */
    @SuppressWarnings("unchecked")
    static Map<String, ObservableList<FragmentDataModel>> getFragmentMap(MainViewController aController) {
        return (Map<String, ObservableList<FragmentDataModel>>)
                MainViewControllerTestSupport.getField(aController, "mapOfFragmentDataModelLists");
    }
    //
    /**
     * Reflectively reads the controller's private {@code settingsContainer} field (no production code is widened), so a
     * test can construct an {@link de.unijena.cheminf.mortar.model.io.Exporter} exactly as the controller does.
     *
     * @param aController the controller instance
     * @return the controller's settings container
     */
    static SettingsContainer getSettingsContainer(MainViewController aController) {
        return (SettingsContainer) MainViewControllerTestSupport.getField(aController, "settingsContainer");
    }
    //
    /**
     * Reflectively reads the number of tabs in the controller's private {@code mainTabPane} (no production code is
     * widened).
     *
     * @param aController the controller instance
     * @return the current tab count of the main tab pane
     */
    static int getTabPaneSize(MainViewController aController) {
        return ((TabPane) MainViewControllerTestSupport.getField(aController, "mainTabPane")).getTabs().size();
    }
    //</editor-fold>
    //
    //<editor-fold desc="Result-tab fixtures" defaultstate="collapsed">
    /**
     * Populates the controller's fragment map with a single real fragment (carrying a parent molecule, which the
     * fragments-tab width listener dereferences) under the given fragmentation name and builds/selects the fragments
     * and itemization tabs via {@code addFragmentationResultTabs}. Must be called on the JavaFX Application Thread. This
     * provides the selected fragments-tab state the histogram/overview/export drives read, without a live fragmentation
     * run.
     *
     * @param aController the controller to set up
     * @param aFragmentationName the fragmentation name used as the tab title suffix and map key
     */
    static void setUpSelectedFragmentsTab(MainViewController aController, String aFragmentationName) {
        FragmentDataModel tmpFragment = new FragmentDataModel("c1ccccc1", "Benzene", new HashMap<>());
        tmpFragment.getParentMolecules().add(new MoleculeDataModel("c1ccccc1", "BenzeneParent", new HashMap<>()));
        ObservableList<FragmentDataModel> tmpFragmentList = FXCollections.observableArrayList(tmpFragment);
        MainViewControllerTestSupport.getFragmentMap(aController).put(aFragmentationName, tmpFragmentList);
        aController.addFragmentationResultTabs(aFragmentationName);
    }
    //
    /**
     * Sets up a fully populated fragments/itemization state: a molecule that has undergone the given fragmentation
     * (with both its fragment list and its unique-SMILES-keyed fragment-frequency map set) is added to the molecule
     * data model list and a non-empty fragment list to the fragment map BEFORE the result tabs are built, so the
     * itemization tab is populated (its items are the molecules filtered by
     * {@code hasMoleculeUndergoneSpecificFragmentation}). The fragments tab is selected. Must be called on the JavaFX
     * Application Thread. This provides the pass-through state the export precondition true branches, the export
     * dispatch and the global-settings apply body read.
     *
     * @param aController the controller to set up
     * @param aFragmentationName the fragmentation name used as the tab title suffix and map key
     */
    static void setUpPopulatedFragmentsAndItems(MainViewController aController, String aFragmentationName) {
        //the molecule that underwent the fragmentation, so the itemization tab is populated; both its fragment list
        //and its fragment-frequency map (keyed by unique SMILES) are needed for the itemization exports, and it is
        //also the fragment's parent molecule, so the two tabs describe one consistent fragmentation
        MoleculeDataModel tmpMolecule = new MoleculeDataModel("c1ccccc1", "Benzene", new HashMap<>());
        FragmentDataModel tmpFragment = new FragmentDataModel("c1ccccc1", "Benzene", new HashMap<>());
        tmpFragment.getParentMolecules().add(tmpMolecule);
        ObservableList<FragmentDataModel> tmpFragmentList = FXCollections.observableArrayList(tmpFragment);
        tmpMolecule.getAllFragments().put(aFragmentationName, new ArrayList<>(tmpFragmentList));
        Map<String, Integer> tmpFragmentFrequencies = new HashMap<>();
        tmpFragmentFrequencies.put(tmpFragment.getUniqueSmiles(), 1);
        tmpMolecule.getFragmentFrequencies().put(aFragmentationName, tmpFragmentFrequencies);
        MainViewControllerTestSupport.getMoleculeList(aController).add(tmpMolecule);
        MainViewControllerTestSupport.getFragmentMap(aController).put(aFragmentationName, tmpFragmentList);
        aController.addFragmentationResultTabs(aFragmentationName);
    }
    //</editor-fold>
}
