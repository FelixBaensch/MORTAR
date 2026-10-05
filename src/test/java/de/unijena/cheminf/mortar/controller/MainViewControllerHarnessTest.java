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
import de.unijena.cheminf.mortar.gui.controls.GridTabForTableView;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.gui.views.IDataTableView;
import de.unijena.cheminf.mortar.gui.views.MainView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.fragmentation.FragmentationService;
import de.unijena.cheminf.mortar.model.io.Exporter;
import de.unijena.cheminf.mortar.model.io.Importer;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.Desktop;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Headless harness-drive tests for the large event-handler regions of {@link MainViewController}: construction
 * guard branches, the {@code addListener} event/menu/key lambdas, the direct import flow and its {@code Task}
 * callbacks, the status bar, the {@code interrupt*} methods, {@code isFragmentationStopAndDataLossConfirmed}, and the
 * GUARDED {@code closeApplication} early-return. This class is the primary coverage driver for everything that only
 * needs a constructed controller plus mocked alerts/{@code Desktop}, without touching the native file chooser or
 * {@code System.exit}. The auxiliary views, the fragmentation flow and the result-tab builders are driven here as
 * well and are documented on their test methods.
 * <p>
 * The controller's constructor ends in a NON-blocking {@code primaryStage.show()}, so it is constructed with a plain
 * {@link AbstractFxTestCase#runAndWait(Runnable)} over the shared {@link FxTestUtil#newMainViewController(Stage, String)}
 * seam (also used by the sibling {@code MainViewControllerTest}) and the caller-owned {@link Stage} is always hidden in
 * a {@code finally}; because {@code Stage.hide()} does not fire the window close-request handler, the
 * {@code closeApplication}/{@code System.exit} path is never reached and the test JVM fork survives. Consequently, this
 * class NEVER fires the Exit menu item, the window-close event, or an empty-list {@code closeApplication}; the only
 * {@code closeApplication} coverage is its guarded early-return (a non-empty molecule list plus a confirmation alert
 * mocked to {@code CANCEL}, which returns before the exit tail). Every {@link MockedStatic} over {@link GuiUtil} is
 * opened INSIDE the FX-thread body because a Mockito static mock is thread-confined and handler code runs on the JavaFX
 * Application Thread. Assertions are behavioral invariants (list sizes, non-null, alert routing) and never pin exact
 * CDK-derived strings, since CDK 2.12 is a moving snapshot.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class MainViewControllerHarnessTest extends AbstractFxTestCase {
    //<editor-fold desc="Private static final class constants" defaultstate="collapsed">
    /**
     * Three valid SMILES lines, so a re-import over already present data can be asserted count-exact: a molecule
     * count that differs from the single-molecule first import proves the guard chain cleared and replaced the data
     * instead of merely leaving it populated.
     */
    private static final String THREE_MOLECULES_SMILES_LINES = "c1ccccc1 benzene\nCCO ethanol\nCC(=O)O aceticAcid\n";
    /**
     * A SMILES-file line that no valid molecule can be parsed from, so the importer rejects the file with an
     * {@code IOException} and the import task's failure branch is exercised.
     */
    private static final String INVALID_SMILES_LINE = "this-is-not-a-valid-smiles-token\n";
    //</editor-fold>
    //
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Default no-argument constructor; all headless setup (toolkit boot, en-GB locale, {@code user.home} isolation) is
     * inherited from {@link AbstractFxTestCase}.
     */
    public MainViewControllerHarnessTest() {
    }
    //</editor-fold>
    //
    //<editor-fold desc="Test methods" defaultstate="collapsed">
    /**
     * Pins the constructor guard branches: a null {@link Stage}, a null {@link MainView} and a null
     * {@link de.unijena.cheminf.mortar.configuration.IConfiguration} each throw {@link NullPointerException}, and an
     * application-directory path that does not denote an existing directory throws {@link IllegalArgumentException}.
     * Each failing construction throws before {@code primaryStage.show()}, so no stage is shown.
     *
     * @throws Exception if anything unexpected happens on the FX thread
     */
    @Test
    public void constructorGuardsThrowTest() throws Exception {
        AtomicReference<Class<?>> tmpNullStage = new AtomicReference<>();
        AtomicReference<Class<?>> tmpNullView = new AtomicReference<>();
        AtomicReference<Class<?>> tmpNullConfig = new AtomicReference<>();
        AtomicReference<Class<?>> tmpBadDir = new AtomicReference<>();
        AbstractFxTestCase.runAndWait(() -> {
            String tmpAppDir = System.getProperty("user.home");
            try {
                MainView tmpView = new MainView(Configuration.getInstance());
                new MainViewController(null, tmpView, tmpAppDir, Configuration.getInstance());
            } catch (Throwable anException) {
                tmpNullStage.set(anException.getClass());
            }
            try {
                new MainViewController(new Stage(), null, tmpAppDir, Configuration.getInstance());
            } catch (Throwable anException) {
                tmpNullView.set(anException.getClass());
            }
            try {
                MainView tmpView = new MainView(Configuration.getInstance());
                new MainViewController(new Stage(), tmpView, tmpAppDir, null);
            } catch (Throwable anException) {
                tmpNullConfig.set(anException.getClass());
            }
            try {
                MainView tmpView = new MainView(Configuration.getInstance());
                new MainViewController(new Stage(), tmpView, tmpAppDir + File.separator + "does-not-exist-xyz",
                        Configuration.getInstance());
            } catch (Throwable anException) {
                tmpBadDir.set(anException.getClass());
            }
        });
        Assertions.assertEquals(NullPointerException.class, tmpNullStage.get(), "a null stage must throw NPE");
        Assertions.assertEquals(NullPointerException.class, tmpNullView.get(), "a null main view must throw NPE");
        Assertions.assertEquals(NullPointerException.class, tmpNullConfig.get(), "a null configuration must throw NPE");
        Assertions.assertEquals(IllegalArgumentException.class, tmpBadDir.get(),
                "an application directory that does not exist must throw IllegalArgumentException");
    }
    //
    /**
     * Drives a real single-molecule SMILES import through {@code importMoleculeFile(File)} (joining the background
     * importer thread for determinism, then draining the nested success callbacks) and asserts the molecule list is
     * populated and the molecules tab was built and selected. This covers the import guard chain, the success callback
     * and {@code openMoleculesTab}. Then it fires the pagination key-press event filter (END/HOME/RIGHT/LEFT/PAGE_UP/
     * PAGE_DOWN) against the now-present molecules tab so all four branches of the filter run.
     *
     * @param aTempDir per-test temporary directory for the SMILES fixture
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void importPopulatesListBuildsTabAndKeyFilterTest(@TempDir Path aTempDir) throws Exception {
        File tmpSmilesFile = Files.writeString(aTempDir.resolve("in.smi"),
                MainViewControllerTestSupport.BENZENE_SMILES_LINE).toFile();
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            MainViewControllerTestSupport.importFileAndDrain(tmpController, tmpSmilesFile);
            Assertions.assertFalse(MainViewControllerTestSupport.getMoleculeList(tmpController).isEmpty(),
                    "the molecule list must be populated after a successful import");
            //fire the pagination key-press filter against the now-present, selected molecules tab (all four branches)
            AbstractFxTestCase.runAndWait(() -> {
                Scene tmpScene = (Scene) MainViewControllerTestSupport.getField(tmpController, "scene");
                for (KeyCode tmpKeyCode : new KeyCode[]{KeyCode.END, KeyCode.HOME, KeyCode.RIGHT, KeyCode.LEFT,
                        KeyCode.PAGE_UP, KeyCode.PAGE_DOWN}) {
                    tmpScene.getRoot().fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", tmpKeyCode,
                            false, false, false, false));
                }
            });
            AbstractFxTestCase.waitForFxEvents();
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Drives an import of a SMILES file from which no valid molecule can be parsed. The importer rejects such a file
     * with an {@code IOException}, so the import task fails and its failure callback runs: the exception alert is raised
     * (verified, with the file-import header), the empty-import warning is not, and the molecule list stays empty.
     *
     * @param aTempDir per-test temporary directory for the SMILES fixture
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void importInvalidFileHitsFailureAlertBranchTest(@TempDir Path aTempDir) throws Exception {
        File tmpSmilesFile = Files.writeString(aTempDir.resolve("invalid.smi"),
                MainViewControllerHarnessTest.INVALID_SMILES_LINE).toFile();
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            MainViewControllerTestSupport.importFileAndDrain(tmpController, tmpSmilesFile, null, aGuiUtilMock -> {
                aGuiUtilMock.verify(() -> GuiUtil.guiExceptionAlert(Mockito.anyString(),
                        Mockito.eq(Message.get("Importer.FileImportExceptionAlert.Header")), Mockito.anyString(),
                        Mockito.any()));
                aGuiUtilMock.verify(() -> GuiUtil.guiMessageAlert(Mockito.any(), Mockito.anyString(),
                        Mockito.eq(Message.get("Importer.FileImportEmptyAlert.Header")), Mockito.anyString()),
                        Mockito.never());
            });
            Assertions.assertTrue(MainViewControllerTestSupport.getMoleculeList(tmpController).isEmpty(),
                    "the molecule list must stay empty when the import fails");
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Drives an import whose importer returns an empty molecule list (a mocked {@link Importer}, because no real file
     * format yields zero molecules without the importer throwing), so the success callback takes the empty-import
     * branch: the empty-import warning alert is raised (verified), the exception alert is not, the status bar reports
     * the failed import and the molecule list stays empty.
     *
     * @param aTempDir per-test temporary directory for the (unread) input file
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void importYieldingNoMoleculesHitsEmptyWarningBranchTest(@TempDir Path aTempDir) throws Exception {
        File tmpSmilesFile = Files.writeString(aTempDir.resolve("empty.smi"), "").toFile();
        Importer tmpImporter = Mockito.mock(Importer.class);
        Mockito.when(tmpImporter.importMoleculeFile(Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean(),
                Mockito.anyBoolean())).thenReturn(new ArrayList<>());
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            MainViewControllerTestSupport.importFileAndDrain(tmpController, tmpSmilesFile, tmpImporter, aGuiUtilMock -> {
                aGuiUtilMock.verify(() -> GuiUtil.guiMessageAlert(Mockito.eq(Alert.AlertType.WARNING),
                        Mockito.anyString(), Mockito.eq(Message.get("Importer.FileImportEmptyAlert.Header")),
                        Mockito.anyString()));
                aGuiUtilMock.verify(() -> GuiUtil.guiExceptionAlert(Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString(), Mockito.any()), Mockito.never());
            });
            Assertions.assertTrue(MainViewControllerTestSupport.getMoleculeList(tmpController).isEmpty(),
                    "the molecule list must stay empty when the importer yields no molecule");
            MainView tmpMainView = (MainView) MainViewControllerTestSupport.getField(tmpController, "mainView");
            Assertions.assertEquals(Message.get("Status.importFailed"),
                    tmpMainView.getStatusBar().getStatusLabel().getText(),
                    "the status bar must report the failed import");
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Imports a molecule set and then imports a DIFFERENTLY sized set while data is already present, so the second
     * import runs the "existing data" guard chain ({@code !moleculeDataModelList.isEmpty()} with a confirmation alert
     * mocked to {@code OK}) that clears the fragmentation cache before re-importing. The second file holds three
     * molecules instead of one, so the assertion is count-exact and proves the previous data was cleared and replaced
     * rather than merely still being there.
     *
     * @param aTempDir per-test temporary directory for the SMILES fixtures
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void importWithExistingDataRunsGuardChainTest(@TempDir Path aTempDir) throws Exception {
        File tmpFirstSmilesFile = Files.writeString(aTempDir.resolve("first.smi"),
                MainViewControllerTestSupport.BENZENE_SMILES_LINE).toFile();
        File tmpSecondSmilesFile = Files.writeString(aTempDir.resolve("second.smi"),
                MainViewControllerHarnessTest.THREE_MOLECULES_SMILES_LINES).toFile();
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            MainViewControllerTestSupport.importFileAndDrain(tmpController, tmpFirstSmilesFile);
            Assertions.assertEquals(1, MainViewControllerTestSupport.getMoleculeList(tmpController).size(),
                    "the first import must have produced exactly its one molecule");
            MainViewControllerTestSupport.importFileAndDrain(tmpController, tmpSecondSmilesFile);
            Assertions.assertEquals(3, MainViewControllerTestSupport.getMoleculeList(tmpController).size(),
                    "the confirmed second import must have replaced the data with exactly its three molecules");
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Fires the Open, Cancel-Import and Cancel-Export menu items so their handler lambdas run, and covers the
     * {@code interrupt*} methods. Opening delegates to {@code chooseAndImportMoleculeFile} and on down to
     * {@code Importer.openFile}, whose native file chooser the headless Monocle glass platform does not implement;
     * {@code Importer.openFile} catches that failure, raises the exception alert (verified, so the delegation down to
     * the chooser is asserted rather than assumed) and returns null, so the import takes its early-return without
     * importing. The cancel-import/cancel-export handlers require non-null task/thread fields, which are set
     * reflectively to unstarted stand-ins before firing.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void openAndCancelHandlersRunTest() throws Exception {
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            AbstractFxTestCase.runAndWait(() -> {
                try (MockedStatic<GuiUtil> tmpGuiUtilMock = FxTestUtil.mockGuiAlerts()) {
                    MainView tmpMainView = (MainView) MainViewControllerTestSupport.getField(tmpController, "mainView");
                    //Open: the handler delegates down to the native file chooser, which the headless Monocle glass
                    //platform does not implement. Importer.openFile catches that failure itself, raises the exception
                    //alert verified right here - which is what proves the delegation reached the chooser - and returns
                    //null, so the import early-returns without a file.
                    tmpMainView.getMainMenuBar().getOpenMenuItem().fire();
                    tmpGuiUtilMock.verify(() -> GuiUtil.guiExceptionAlert(Mockito.anyString(), Mockito.anyString(),
                            Mockito.anyString(), Mockito.any()));
                    //set unstarted task/thread stand-ins so the interrupt handlers do not dereference null
                    MainViewControllerTestSupport.setField(tmpController, "importTask", MainViewControllerHarnessTest.noOpTask());
                    MainViewControllerTestSupport.setField(tmpController, "importerThread", new Thread(() -> { }));
                    MainViewControllerTestSupport.setField(tmpController, "exportTask", MainViewControllerHarnessTest.noOpTask());
                    MainViewControllerTestSupport.setField(tmpController, "exporterThread", new Thread(() -> { }));
                    tmpMainView.getMainMenuBar().getCancelImportMenuItem().fire();
                    tmpMainView.getMainMenuBar().getCancelExportMenuItem().fire();
                }
            });
            AbstractFxTestCase.waitForFxEvents();
            //the chooser never yielded a file, so nothing was imported
            Assertions.assertTrue(MainViewControllerTestSupport.getMoleculeList(tmpController).isEmpty(),
                    "an Open that never got a file from the chooser must not import any molecule");
            //the cancel-import/cancel-export handlers ran interruptImport/interruptExport, cancelling the stand-in tasks
            Assertions.assertTrue(
                    ((Task<?>) MainViewControllerTestSupport.getField(tmpController, "importTask")).isCancelled(),
                    "the cancel-import handler must cancel the import task");
            Assertions.assertTrue(
                    ((Task<?>) MainViewControllerTestSupport.getField(tmpController, "exportTask")).isCancelled(),
                    "the cancel-export handler must cancel the export task");
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Covers {@code interruptFragmentation}: after a real import builds the fragmentation/cancel buttons (via
     * {@code openMoleculesTab}), a stand-in fragmentation task is set reflectively and {@code interruptFragmentation}
     * is invoked, which cancels the task and resets the two buttons. Behavioral assertion: the cancel-fragmentation
     * button is hidden and the fragmentation button re-enabled afterwards.
     *
     * @param aTempDir per-test temporary directory for the SMILES fixture
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void interruptFragmentationResetsButtonsTest(@TempDir Path aTempDir) throws Exception {
        File tmpSmilesFile = Files.writeString(aTempDir.resolve("in.smi"),
                MainViewControllerTestSupport.BENZENE_SMILES_LINE).toFile();
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            MainViewControllerTestSupport.importFileAndDrain(tmpController, tmpSmilesFile);
            AbstractFxTestCase.runAndWait(() -> {
                MainViewControllerTestSupport.setField(tmpController, "parallelFragmentationMainTask",
                        MainViewControllerHarnessTest.noOpTask());
                try {
                    Method tmpMethod = MainViewController.class.getDeclaredMethod("interruptFragmentation");
                    tmpMethod.setAccessible(true);
                    tmpMethod.invoke(tmpController);
                } catch (ReflectiveOperationException anException) {
                    throw new RuntimeException(anException);
                }
            });
            AbstractFxTestCase.waitForFxEvents();
            Button tmpCancelButton = (Button)
                    MainViewControllerTestSupport.getField(tmpController, "cancelFragmentationButton");
            Button tmpFragmentButton = (Button)
                    MainViewControllerTestSupport.getField(tmpController, "fragmentationButton");
            Assertions.assertFalse(tmpCancelButton.isVisible(), "the cancel-fragmentation button must be hidden");
            Assertions.assertFalse(tmpFragmentButton.isDisabled(), "the fragmentation button must be re-enabled");
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Exercises {@code updateStatusBar} (add-thread, remove-last-thread and remaining-thread branches) and the pure
     * {@code getStatusMessageByThreadType} switch for every thread type. Threads are named with valid
     * {@link MainViewController.ThreadType} names so the reverse lookup in the remaining-thread branch succeeds.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void statusBarAndStatusMessageBranchesTest() throws Exception {
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            AbstractFxTestCase.runAndWait(() -> {
                Assertions.assertEquals(Message.get("Status.running"),
                        tmpController.getStatusMessageByThreadType(MainViewController.ThreadType.FRAGMENTATION_THREAD));
                Assertions.assertEquals(Message.get("Status.importing"),
                        tmpController.getStatusMessageByThreadType(MainViewController.ThreadType.IMPORT_THREAD));
                Assertions.assertEquals(Message.get("Status.exporting"),
                        tmpController.getStatusMessageByThreadType(MainViewController.ThreadType.EXPORT_THREAD));
                Thread tmpImportThread = new Thread(() -> { });
                tmpImportThread.setName(MainViewController.ThreadType.IMPORT_THREAD.getThreadName());
                Thread tmpFragmentationThread = new Thread(() -> { });
                tmpFragmentationThread.setName(MainViewController.ThreadType.FRAGMENTATION_THREAD.getThreadName());
                MainView tmpMainView = (MainView) MainViewControllerTestSupport.getField(tmpController, "mainView");
                Label tmpStatusLabel = tmpMainView.getStatusBar().getStatusLabel();
                ProgressBar tmpProgressBar = tmpMainView.getStatusBar().getProgressBar();
                //add-thread branch: the given message is shown together with the progress bar
                tmpController.updateStatusBar(tmpImportThread, "import added");
                Assertions.assertEquals("import added", tmpStatusLabel.getText());
                Assertions.assertTrue(tmpStatusLabel.isVisible(), "the status label must be shown for a running thread");
                Assertions.assertTrue(tmpProgressBar.isVisible(), "the progress bar must be shown for a running thread");
                //add a second thread, then remove the first -> remaining-thread branch, which ignores the given
                //message and shows the status message of the last remaining thread's type instead
                tmpController.updateStatusBar(tmpFragmentationThread, "fragmentation added");
                Assertions.assertEquals("fragmentation added", tmpStatusLabel.getText());
                tmpController.updateStatusBar(tmpImportThread, "import removed");
                Assertions.assertEquals(Message.get("Status.running"), tmpStatusLabel.getText(),
                        "with a fragmentation thread remaining, its status message must be shown");
                Assertions.assertTrue(tmpProgressBar.isVisible(), "the progress bar must stay shown while a thread runs");
                //remove the last remaining thread -> empty-list branch: the given message, progress bar hidden
                tmpController.updateStatusBar(tmpFragmentationThread, "fragmentation removed");
                Assertions.assertEquals("fragmentation removed", tmpStatusLabel.getText());
                Assertions.assertFalse(tmpProgressBar.isVisible(), "the progress bar must be hidden once no thread runs");
            });
            AbstractFxTestCase.waitForFxEvents();
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Covers both message branches of {@code isFragmentationStopAndDataLossConfirmed} (fragmentation-running and
     * data-loss) for both answers: with the confirmation alert mocked to {@code OK} it returns {@code true}, with it
     * mocked to {@code CANCEL} it returns {@code false}. Each branch is also verified to have raised its own dialog
     * (identified by its content text), so a swapped or merged branch fails.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void isFragmentationStopAndDataLossConfirmedBothBranchesTest() throws Exception {
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        //the two dialogs share their title and header, only their content text tells them apart
        String tmpDataLossContent = Message.get("MainViewController.Warning.DataLoss.Content");
        String tmpRunningContent = Message.get("MainViewController.Warning.FragmentationRunning.Content");
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            AbstractFxTestCase.runAndWait(() -> {
                try (MockedStatic<GuiUtil> tmpGuiUtilMock = FxTestUtil.mockGuiAlerts()) {
                    //data-loss branch (fragmentation not running)
                    Assertions.assertTrue(tmpController.isFragmentationStopAndDataLossConfirmed(),
                            "an OK-confirmed data-loss dialog must return true");
                    tmpGuiUtilMock.verify(() -> GuiUtil.guiConfirmationAlert(Mockito.anyString(),
                            Mockito.anyString(), Mockito.eq(tmpDataLossContent)));
                    //fragmentation-running branch
                    MainViewControllerTestSupport.setField(tmpController, "isFragmentationRunning", Boolean.TRUE);
                    Assertions.assertTrue(tmpController.isFragmentationStopAndDataLossConfirmed(),
                            "an OK-confirmed fragmentation-running dialog must return true");
                    tmpGuiUtilMock.verify(() -> GuiUtil.guiConfirmationAlert(Mockito.anyString(),
                            Mockito.anyString(), Mockito.eq(tmpRunningContent)));
                }
                try (MockedStatic<GuiUtil> tmpGuiUtilMock = MainViewControllerHarnessTest.mockGuiAlertsConfirmCancel()) {
                    //fragmentation-running branch, declined
                    Assertions.assertFalse(tmpController.isFragmentationStopAndDataLossConfirmed(),
                            "a cancelled fragmentation-running dialog must return false");
                    //data-loss branch, declined
                    MainViewControllerTestSupport.setField(tmpController, "isFragmentationRunning", Boolean.FALSE);
                    Assertions.assertFalse(tmpController.isFragmentationStopAndDataLossConfirmed(),
                            "a cancelled data-loss dialog must return false");
                    tmpGuiUtilMock.verify(() -> GuiUtil.guiConfirmationAlert(Mockito.anyString(),
                            Mockito.anyString(), Mockito.eq(tmpRunningContent)));
                    tmpGuiUtilMock.verify(() -> GuiUtil.guiConfirmationAlert(Mockito.anyString(),
                            Mockito.anyString(), Mockito.eq(tmpDataLossContent)));
                }
            });
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Covers the GUARDED {@code closeApplication} early-return WITHOUT reaching {@code System.exit}: with a non-empty
     * molecule list and the confirmation alert mocked to {@code CANCEL}, {@code closeApplication} returns before the
     * persist/exit tail. {@code closeApplication} is invoked reflectively (it stays private) so the Exit menu item and
     * the window-close event are never fired. That the whole suite keeps running proves no {@code System.exit} was
     * reached.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void closeApplicationGuardedCancelEarlyReturnTest() throws Exception {
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            AbstractFxTestCase.runAndWait(() -> {
                //make the molecule list non-empty so the guard's first operand is true; unlike the import drives above
                //this adds the molecule directly, because all this test needs is a non-empty list and a real import
                //would additionally build and select the molecules tab, which the close guard does not read
                MainViewControllerTestSupport.getMoleculeList(tmpController)
                        .add(new MoleculeDataModel("c1ccccc1", "Benzene", new HashMap<>()));
                try (MockedStatic<GuiUtil> tmpGuiUtilMock = MainViewControllerHarnessTest.mockGuiAlertsConfirmCancel()) {
                    Method tmpMethod = MainViewController.class.getDeclaredMethod("closeApplication", int.class);
                    tmpMethod.setAccessible(true);
                    tmpMethod.invoke(tmpController, 0);
                } catch (ReflectiveOperationException anException) {
                    throw new RuntimeException(anException);
                }
            });
            //the fork is still alive: the list is unchanged (persist/exit tail was not reached)
            Assertions.assertEquals(1, MainViewControllerTestSupport.getMoleculeList(tmpController).size(),
                    "the guarded closeApplication must return early, leaving state untouched");
            Assertions.assertTrue(tmpStageReference.get().isShowing(),
                    "the main view must still be showing after the declined close");
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Drives a real single-algorithm fragmentation flow end to end: imports one molecule, selects it, calls
     * {@code startFragmentation()}, joins the background fragmentation thread and drains the nested success callback so
     * {@code addFragmentationResultTabs}/{@code createFragmentsTab}/{@code createItemsTab} build the result tabs.
     * Behavioral assertion: a fragmentation result list is present in the controller's fragment map afterwards.
     *
     * @param aTempDir per-test temporary directory for the SMILES fixture
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void startFragmentationBuildsResultTabsTest(@TempDir Path aTempDir) throws Exception {
        File tmpSmilesFile = Files.writeString(aTempDir.resolve("in.smi"),
                MainViewControllerTestSupport.BENZENE_SMILES_LINE).toFile();
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            MainViewControllerTestSupport.importFileAndDrain(tmpController, tmpSmilesFile);
            AbstractFxTestCase.runAndWait(() -> {
                try (MockedStatic<GuiUtil> tmpGuiUtilMock = FxTestUtil.mockGuiAlerts()) {
                    MainViewControllerTestSupport.getMoleculeList(tmpController).get(0).setSelection(true);
                    tmpController.startFragmentation();
                }
            });
            MainViewControllerTestSupport.joinThreadField(tmpController, "fragmentationThread");
            //two drains, then a barrier: the first runs the task's success callback, which itself nests a further
            //Platform.runLater (the result-tab build) that only the second drain executes; the empty runAndWait then
            //returns after everything both drains queued has been processed
            AbstractFxTestCase.waitForFxEvents();
            AbstractFxTestCase.waitForFxEvents();
            AbstractFxTestCase.runAndWait(() -> { });
            //benzene yields a fragment here because the Ertl algorithm is the default fragmenter and its default
            //settings return the non-functional-group fragments as well; with only functional groups returned, a
            //molecule without one would produce no fragment at all and this assertion would not hold
            Assertions.assertFalse(MainViewControllerTestSupport.getFragmentMap(tmpController).isEmpty(),
                    "a fragmentation result list must be present after a completed fragmentation");
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Drives the pipelining branch of {@code startFragmentation(boolean)} (a molecule imported and selected, then
     * {@code startFragmentation(true)}), joining the background thread and draining the callbacks. Behavioral
     * assertion: the flow completes without a fork crash and a fragmentation result list is present.
     *
     * @param aTempDir per-test temporary directory for the SMILES fixture
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void startPipeliningFragmentationFlowTest(@TempDir Path aTempDir) throws Exception {
        File tmpSmilesFile = Files.writeString(aTempDir.resolve("in.smi"),
                MainViewControllerTestSupport.BENZENE_SMILES_LINE).toFile();
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            MainViewControllerTestSupport.importFileAndDrain(tmpController, tmpSmilesFile);
            AbstractFxTestCase.runAndWait(() -> {
                try (MockedStatic<GuiUtil> tmpGuiUtilMock = FxTestUtil.mockGuiAlerts()) {
                    MainViewControllerTestSupport.getMoleculeList(tmpController).get(0).setSelection(true);
                    tmpController.startFragmentation(true);
                }
            });
            MainViewControllerTestSupport.joinThreadField(tmpController, "fragmentationThread");
            //two drains plus a barrier, for the same reason as in startFragmentationBuildsResultTabsTest above
            AbstractFxTestCase.waitForFxEvents();
            AbstractFxTestCase.waitForFxEvents();
            AbstractFxTestCase.runAndWait(() -> { });
            //as above, benzene yields a fragment because of the Ertl defaults; additionally, this holds only because
            //the default pipeline is a single Ertl step, so the pipeline result is that same fragmentation
            Assertions.assertFalse(MainViewControllerTestSupport.getFragmentMap(tmpController).isEmpty(),
                    "a fragmentation result list must be present after a completed pipeline fragmentation");
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Builds the fragment/itemization result tabs directly from an EMPTY fragment list, so the empty-list disable
     * branches of {@code createFragmentsTab} and {@code createItemsTab} run. Behavioral assertion: the two result tabs
     * are added to the tab pane AND every view button they carry (overview plus histogram on the fragments tab,
     * histogram on the itemization tab) is disabled.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void emptyFragmentListDisablesResultTabButtonsTest() throws Exception {
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            AbstractFxTestCase.runAndWait(() -> {
                MainViewControllerTestSupport.getFragmentMap(tmpController)
                        .put("EmptyFragmentation", FXCollections.observableArrayList());
                tmpController.addFragmentationResultTabs("EmptyFragmentation");
            });
            AbstractFxTestCase.waitForFxEvents();
            int tmpTabCount = MainViewControllerTestSupport.getTabPaneSize(tmpController);
            Assertions.assertTrue(tmpTabCount >= 2, "the fragments and itemization result tabs must be added");
            AtomicReference<List<Button>> tmpViewButtons = new AtomicReference<>();
            AbstractFxTestCase.runAndWait(() ->
                    tmpViewButtons.set(MainViewControllerHarnessTest.collectResultTabViewButtons(tmpController)));
            Assertions.assertEquals(3, tmpViewButtons.get().size(),
                    "the fragments tab must carry the overview and histogram button and the itemization tab the histogram button");
            Assertions.assertTrue(tmpViewButtons.get().stream().allMatch(Button::isDisable),
                    "an empty fragment list must disable every result-tab view button");
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Selects a different fragmentation algorithm via its {@link RadioMenuItem} in the fragmentation-algorithm menu so
     * the selected-toggle listener registered by {@code addFragmentationAlgorithmCheckMenuItems} fires. Behavioral
     * assertion: the {@link FragmentationService}'s selected fragmenter display name equals the toggled item's text.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void fragmentationAlgorithmToggleUpdatesSelectedFragmenterTest() throws Exception {
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            AtomicReference<String> tmpToggledText = new AtomicReference<>();
            AbstractFxTestCase.runAndWait(() -> {
                MainView tmpMainView = (MainView) MainViewControllerTestSupport.getField(tmpController, "mainView");
                List<MenuItem> tmpItems = tmpMainView.getMainMenuBar().getFragmentationAlgorithmMenu().getItems();
                for (MenuItem tmpItem : tmpItems) {
                    RadioMenuItem tmpRadioItem = (RadioMenuItem) tmpItem;
                    if (!tmpRadioItem.isSelected()) {
                        tmpToggledText.set(tmpRadioItem.getText());
                        tmpRadioItem.setSelected(true);
                        break;
                    }
                }
            });
            AbstractFxTestCase.waitForFxEvents();
            FragmentationService tmpService =
                    (FragmentationService) MainViewControllerTestSupport.getField(tmpController, "fragmentationService");
            Assertions.assertEquals(tmpToggledText.get(),
                    tmpService.getSelectedFragmenter().getFragmentationAlgorithmDisplayName(),
                    "the selected fragmenter must match the toggled algorithm menu item");
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Opens the fragmentation-settings (non-blocking {@code show}), pipeline-settings and global-settings (both blocking
     * {@code showAndWait}) auxiliary views via
     * {@link FxTestUtil#runAndDriveModal(java.util.concurrent.Callable, java.util.function.Consumer)}, which opens each
     * and always closes it, so none leaks or hangs. Alerts and {@link java.awt.Desktop} are mocked on the FX thread
     * inside the driver. No import precedes the global-settings open: its {@code Platform.runLater} body applies
     * nothing as long as neither the rows-per-page nor the keep-atom-container setting has changed, so a populated tab
     * would make no difference here; the apply body itself is driven directly, with both change flags set, by
     * {@link #applyGlobalSettingsChangesAppliesToTabsAndDataModelsTest()}. Behavioral assertion: each open showed its own
     * stage (identified by its title), the driver closed it again, and the primary stage is still showing afterwards.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void settingsAuxiliaryModalsOpenAndCloseTest() throws Exception {
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            MainViewControllerHarnessTest.assertOpenedAndClosed(
                    this.driveModalOpenAndCapture(tmpController::openFragmentationSettingsView),
                    "FragmentationSettingsView.title.text");
            MainViewControllerHarnessTest.assertOpenedAndClosed(
                    this.driveModalOpenAndCapture(tmpController::openPipelineSettingsView),
                    "PipelineSettingsView.title.text");
            MainViewControllerHarnessTest.assertOpenedAndClosed(
                    this.driveModalOpenAndCapture(tmpController::openGlobalSettingsView),
                    "SettingsView.title.default.text");
            AbstractFxTestCase.waitForFxEvents();
            Assertions.assertTrue(tmpStageReference.get().isShowing(),
                    "the primary stage must still be showing after the three auxiliary opens");
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Builds and selects a fragments result tab, then opens the histogram view (non-blocking {@code show}) through the
     * modal driver, which waits for its queued driver, so the view is opened and always closed. Behavioral assertion:
     * the histogram stage (identified by its title) was shown and is closed again afterwards.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void openHistogramViewModalTest() throws Exception {
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            AbstractFxTestCase.runAndWait(() ->
                    MainViewControllerTestSupport.setUpSelectedFragmentsTab(tmpController, "TestFragmentation"));
            AbstractFxTestCase.waitForFxEvents();
            MainViewControllerHarnessTest.assertOpenedAndClosed(
                    this.driveModalOpenAndCapture(tmpController::openHistogramView), "HistogramView.title");
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Opens the About view via its menu item through the modal driver ({@link java.awt.Desktop} mocked so the
     * open-GitHub/tutorial handlers would not throw a {@code HeadlessException} if triggered), asserting it opens and
     * closes without a fork crash.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void openAboutViewModalTest() throws Exception {
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            AtomicReference<Stage> tmpAboutModal = new AtomicReference<>();
            FxTestUtil.runAndDriveModal(
                    () -> {
                        try (MockedStatic<GuiUtil> tmpGuiUtilMock = FxTestUtil.mockGuiAlerts();
                                MockedStatic<Desktop> tmpDesktopMock = FxTestUtil.mockDesktop()) {
                            MainView tmpMainView = (MainView) MainViewControllerTestSupport.getField(tmpController, "mainView");
                            tmpMainView.getMainMenuBar().getAboutViewMenuItem().fire();
                        }
                        return null;
                    },
                    tmpAboutModal::set);
            AbstractFxTestCase.waitForFxEvents();
            Assertions.assertNotNull(tmpAboutModal.get(), "the About view stage must have opened");
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Drives the molecules data-source branch of {@code openOverviewView} (with the molecules tab selected) through the
     * modal driver, and the internally-caught {@link IllegalStateException} branch (a fragments data source while the
     * molecules tab is selected, which is caught and logged so the method returns without opening a modal). A fresh
     * controller is used per overview test because the {@code OverviewViewController} reuses one {@code OverviewView}
     * instance across opens, so a single controller may open the overview only once.
     *
     * @param aTempDir per-test temporary directory for the SMILES fixture
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void openOverviewViewMoleculesBranchAndIllegalStateCatchTest(@TempDir Path aTempDir) throws Exception {
        File tmpSmilesFile = Files.writeString(aTempDir.resolve("in.smi"),
                MainViewControllerTestSupport.BENZENE_SMILES_LINE).toFile();
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            //mismatched data source with the molecules tab selected -> internally-caught IllegalStateException (no modal)
            MainViewControllerTestSupport.importFileAndDrain(tmpController, tmpSmilesFile);
            AbstractFxTestCase.runAndWait(() ->
                    tmpController.openOverviewView(OverviewViewController.DataSources.FRAGMENTS_TAB));
            AbstractFxTestCase.waitForFxEvents();
            //the caught IllegalStateException path returned without opening a modal, so state is untouched and populated
            Assertions.assertFalse(MainViewControllerTestSupport.getMoleculeList(tmpController).isEmpty(),
                    "the caught IllegalStateException path must leave the imported molecule list intact");
            //molecules tab selected -> MOLECULES_TAB branch (the single overview open for this controller)
            Stage tmpOverviewModal = this.driveModalOpenAndCapture(() ->
                    tmpController.openOverviewView(OverviewViewController.DataSources.MOLECULES_TAB));
            Assertions.assertNotNull(tmpOverviewModal, "the molecules-branch overview stage must have opened");
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Drives the fragments data-source branch of {@code openOverviewView} with a fragments tab built and selected,
     * through the modal driver. A fresh controller is used (see
     * {@link #openOverviewViewMoleculesBranchAndIllegalStateCatchTest(Path)}) so this is the controller's single
     * overview open. Behavioral assertion: an overview stage was actually shown.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void openOverviewViewFragmentsBranchTest() throws Exception {
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            AbstractFxTestCase.runAndWait(() ->
                    MainViewControllerTestSupport.setUpSelectedFragmentsTab(tmpController, "TestFragmentation"));
            AbstractFxTestCase.waitForFxEvents();
            Stage tmpOverviewModal = this.driveModalOpenAndCapture(() ->
                    tmpController.openOverviewView(OverviewViewController.DataSources.FRAGMENTS_TAB));
            Assertions.assertNotNull(tmpOverviewModal, "the fragments-branch overview stage must have opened");
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Fires each of the seven fragment/item export menu items with the molecules tab selected after a real import, so
     * every export menu handler lambda body runs and each delegates to {@code exportFile}, which aborts at the
     * molecules-tab-selected precondition guard (a confirmation alert mocked to {@code OK}). Because the molecules tab is
     * selected the native file chooser is never reached. Behavioral assertion: firing completes without a fork crash.
     *
     * @param aTempDir per-test temporary directory for the SMILES fixture
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void exportMenuItemHandlerLambdasFireTest(@TempDir Path aTempDir) throws Exception {
        File tmpSmilesFile = Files.writeString(aTempDir.resolve("in.smi"),
                MainViewControllerTestSupport.BENZENE_SMILES_LINE).toFile();
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            MainViewControllerTestSupport.importFileAndDrain(tmpController, tmpSmilesFile);
            AbstractFxTestCase.runAndWait(() -> {
                try (MockedStatic<GuiUtil> tmpGuiUtilMock = FxTestUtil.mockGuiAlerts()) {
                    MainView tmpMainView = (MainView) MainViewControllerTestSupport.getField(tmpController, "mainView");
                    tmpMainView.getMainMenuBar().getFragmentsExportToCSVMenuItem().fire();
                    tmpMainView.getMainMenuBar().getFragmentsExportToPDBMenuItem().fire();
                    tmpMainView.getMainMenuBar().getFragmentsExportToPDFMenuItem().fire();
                    tmpMainView.getMainMenuBar().getFragmentsExportToSingleSDFMenuItem().fire();
                    tmpMainView.getMainMenuBar().getFragmentsExportToSeparateSDFsMenuItem().fire();
                    tmpMainView.getMainMenuBar().getItemsExportToCSVMenuItem().fire();
                    tmpMainView.getMainMenuBar().getItemsExportToPDFMenuItem().fire();
                }
            });
            AbstractFxTestCase.waitForFxEvents();
            //every export aborted at the molecules-tab-selected precondition guard, so no export task was ever launched
            Assertions.assertNull(MainViewControllerTestSupport.getField(tmpController, "exportTask"),
                    "an export fired with the molecules tab selected must abort at the guard without launching a task");
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * The positive counterpart to {@link #exportMenuItemHandlerLambdasFireTest(Path)}: fires the fragments-CSV export
     * menu item with a POPULATED fragments result tab selected, so {@code exportFile} passes every precondition guard
     * instead of aborting at one and runs on into the export file chooser. The {@link Exporter} that
     * {@code exportFile} constructs is intercepted with a construction mock whose chooser answers {@code null} (as a
     * cancelled dialog does), so the test verifies that the chooser was asked exactly once, over the primary stage, for
     * the fragments-CSV type and the selected fragmentation's name, without opening a native dialog. Neither the
     * molecules-tab confirmation alert nor the no-data message alert (the two guard aborts) may have been raised, and
     * because the chooser yielded no file the export returns before launching a task. Everything beyond the chooser is
     * covered without the GUI by {@code MainViewControllerExportTest.buildExportResultDispatchesEachResolvableTypeTest}
     * and {@code MainViewControllerExportTest.launchExportTaskCleanBranchWritesFileTest}.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void exportMenuItemPassesPreconditionsAndReachesChooserTest() throws Exception {
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            AbstractFxTestCase.runAndWait(() ->
                    MainViewControllerTestSupport.setUpPopulatedFragmentsAndItems(tmpController, "TestFragmentation"));
            AbstractFxTestCase.waitForFxEvents();
            AbstractFxTestCase.runAndWait(() -> {
                //both static/construction mocks are thread-confined, so they are opened on the FX thread that fires
                try (MockedStatic<GuiUtil> tmpGuiUtilMock = FxTestUtil.mockGuiAlerts();
                        MockedConstruction<Exporter> tmpExporterMock = Mockito.mockConstruction(Exporter.class)) {
                    MainView tmpMainView = (MainView) MainViewControllerTestSupport.getField(tmpController, "mainView");
                    tmpMainView.getMainMenuBar().getFragmentsExportToCSVMenuItem().fire();
                    Assertions.assertEquals(1, tmpExporterMock.constructed().size(),
                            "the export must have passed its guards and constructed exactly one exporter");
                    Mockito.verify(tmpExporterMock.constructed().getFirst()).openFileChooserForExportFileOrDir(
                            tmpStageReference.get(), Exporter.ExportTypes.FRAGMENT_CSV_FILE, "TestFragmentation");
                    //no guard aborted the export, so neither guard's alert was raised
                    tmpGuiUtilMock.verify(() -> GuiUtil.guiConfirmationAlert(
                            Mockito.eq(Message.get("Exporter.confirmationAlert.moleculesTabSelected.title")),
                            Mockito.anyString(), Mockito.anyString()), Mockito.never());
                    tmpGuiUtilMock.verify(() -> GuiUtil.guiMessageAlert(Mockito.any(),
                            Mockito.eq(Message.get("Exporter.MessageAlert.NoDataAvailable.title")),
                            Mockito.any(), Mockito.any()), Mockito.never());
                }
            });
            AbstractFxTestCase.waitForFxEvents();
            Assertions.assertNull(MainViewControllerTestSupport.getField(tmpController, "exportTask"),
                    "an export whose chooser never yielded a file must not launch an export task");
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Fires the overview-view menu item with the molecules tab selected after a real import, so the menu handler lambda
     * takes its molecules-tab branch and opens the overview via {@link FxTestUtil#runAndDriveModal(java.util.concurrent.Callable,
     * java.util.function.Consumer)} (which always closes it). Behavioral assertion: the drive completes without a fork
     * crash.
     *
     * @param aTempDir per-test temporary directory for the SMILES fixture
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void overviewMenuItemMoleculesBranchFiresTest(@TempDir Path aTempDir) throws Exception {
        File tmpSmilesFile = Files.writeString(aTempDir.resolve("in.smi"),
                MainViewControllerTestSupport.BENZENE_SMILES_LINE).toFile();
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            MainViewControllerTestSupport.importFileAndDrain(tmpController, tmpSmilesFile);
            Stage tmpOverviewModal = this.driveModalOpenAndCapture(() -> {
                MainView tmpMainView = (MainView) MainViewControllerTestSupport.getField(tmpController, "mainView");
                tmpMainView.getMainMenuBar().getOverviewViewMenuItem().fire();
            });
            Assertions.assertNotNull(tmpOverviewModal, "the molecules-branch overview stage must have opened");
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Fires the overview-view menu item with a fragments result tab selected, so the menu handler lambda takes its
     * fragments-tab branch and opens the overview through the modal driver. A fresh controller is used because the
     * {@code OverviewViewController} reuses one {@code OverviewView} instance across opens.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void overviewMenuItemFragmentsBranchFiresTest() throws Exception {
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            AbstractFxTestCase.runAndWait(() ->
                    MainViewControllerTestSupport.setUpSelectedFragmentsTab(tmpController, "TestFragmentation"));
            AbstractFxTestCase.waitForFxEvents();
            Stage tmpOverviewModal = this.driveModalOpenAndCapture(() -> {
                MainView tmpMainView = (MainView) MainViewControllerTestSupport.getField(tmpController, "mainView");
                tmpMainView.getMainMenuBar().getOverviewViewMenuItem().fire();
            });
            Assertions.assertNotNull(tmpOverviewModal, "the fragments-branch overview stage must have opened");
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Drives the parent-molecules-sample branch of {@code openOverviewView}: with a fragments result tab selected and a
     * single cell selected in the fragments table, {@code openOverviewView(PARENT_MOLECULES_SAMPLE)} collects the
     * selected fragment plus its parent-molecule sample and opens the overview through the modal driver.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void overviewParentMoleculesSampleBranchTest() throws Exception {
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            AbstractFxTestCase.runAndWait(() -> {
                MainViewControllerTestSupport.setUpSelectedFragmentsTab(tmpController, "TestFragmentation");
                MainViewControllerHarnessTest.selectFirstCellOfSelectedTab(tmpController);
            });
            AbstractFxTestCase.waitForFxEvents();
            Stage tmpOverviewModal = this.driveModalOpenAndCapture(() ->
                    tmpController.openOverviewView(OverviewViewController.DataSources.PARENT_MOLECULES_SAMPLE));
            Assertions.assertNotNull(tmpOverviewModal, "the parent-molecules-sample overview stage must have opened");
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Drives the item-with-fragments-sample branch of {@code openOverviewView}: with a populated itemization result tab
     * selected and a single cell selected in the itemization table, {@code openOverviewView(ITEM_WITH_FRAGMENTS_SAMPLE)}
     * collects the selected molecule plus its fragment sample and opens the overview through the modal driver.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void overviewItemWithFragmentsSampleBranchTest() throws Exception {
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            AbstractFxTestCase.runAndWait(() -> {
                MainViewControllerTestSupport.setUpPopulatedFragmentsAndItems(tmpController, "TestFragmentation");
                MainViewControllerHarnessTest.selectItemizationTab(tmpController);
                MainViewControllerHarnessTest.selectFirstCellOfSelectedTab(tmpController);
            });
            AbstractFxTestCase.waitForFxEvents();
            Stage tmpOverviewModal = this.driveModalOpenAndCapture(() ->
                    tmpController.openOverviewView(OverviewViewController.DataSources.ITEM_WITH_FRAGMENTS_SAMPLE));
            Assertions.assertNotNull(tmpOverviewModal, "the item-with-fragments-sample overview stage must have opened");
            Assertions.assertFalse(MainViewControllerTestSupport.getMoleculeList(tmpController).isEmpty(),
                    "the populated itemization fixture must have added a molecule to the molecule list");
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Fires the fragmentation-settings, global-settings, pipeline-settings and histogram menu items (with a fragments
     * result tab present for the histogram) so their menu handler lambda bodies run and each opens its auxiliary view
     * through the modal driver (which always closes it). Behavioral assertion: each menu item opened its own view stage
     * (identified by its title), which the driver closed again.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void settingsAndHistogramMenuItemHandlerLambdasFireTest() throws Exception {
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            AbstractFxTestCase.runAndWait(() ->
                    MainViewControllerTestSupport.setUpSelectedFragmentsTab(tmpController, "TestFragmentation"));
            AbstractFxTestCase.waitForFxEvents();
            MainView tmpMainView = (MainView) MainViewControllerTestSupport.getField(tmpController, "mainView");
            MainViewControllerHarnessTest.assertOpenedAndClosed(this.driveModalOpenAndCapture(
                    () -> tmpMainView.getMainMenuBar().getFragmentationSettingsMenuItem().fire()),
                    "FragmentationSettingsView.title.text");
            MainViewControllerHarnessTest.assertOpenedAndClosed(this.driveModalOpenAndCapture(
                    () -> tmpMainView.getMainMenuBar().getGlobalSettingsMenuItem().fire()),
                    "SettingsView.title.default.text");
            MainViewControllerHarnessTest.assertOpenedAndClosed(this.driveModalOpenAndCapture(
                    () -> tmpMainView.getMainMenuBar().getPipelineSettingsMenuItem().fire()),
                    "PipelineSettingsView.title.text");
            MainViewControllerHarnessTest.assertOpenedAndClosed(this.driveModalOpenAndCapture(
                    () -> tmpMainView.getMainMenuBar().getHistogramViewerMenuItem().fire()),
                    "HistogramView.title");
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Fires a key-press event on the scene while no result tab is present (a freshly constructed controller), so the
     * pagination key-press filter takes its null-selected-tab branch (consume and return). Behavioral assertion: the
     * event is dispatched without a fork crash.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void paginationKeyFilterNullTabBranchTest() throws Exception {
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            AbstractFxTestCase.runAndWait(() -> {
                Scene tmpScene = (Scene) MainViewControllerTestSupport.getField(tmpController, "scene");
                tmpScene.getRoot().fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.END,
                        false, false, false, false));
            });
            AbstractFxTestCase.waitForFxEvents();
            //a freshly constructed controller has no result tab, so the key filter took its null-selected-tab branch
            Assertions.assertEquals(0, MainViewControllerTestSupport.getTabPaneSize(tmpController),
                    "no result tab must be present, exercising the null-selected-tab branch of the key filter");
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //
    /**
     * Directly drives the extracted {@code applyGlobalSettingsChanges} apply body with both change flags set on a fully
     * populated fragments/itemization state, so the rows-per-page pagination recompute (over every result tab) and the
     * keep-atom-container propagation (over every molecule and fragment) both run.
     * <p>
     * The two tabs' item lists are padded to different sizes (five fragments, three items) and the rows-per-page setting is
     * lowered to two, so each pagination must end at its own recomputed page count (three and two) instead of the
     * single page it was built with. Every data model is first set to keep its atom container; the setting getter
     * currently always answers {@code false}, so after the apply every molecule and fragment must have been switched
     * to {@code false}.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void applyGlobalSettingsChangesAppliesToTabsAndDataModelsTest() throws Exception {
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            AbstractFxTestCase.runAndWait(() -> {
                MainViewControllerTestSupport.setUpPopulatedFragmentsAndItems(tmpController, "TestFragmentation");
                TabPane tmpTabPane = (TabPane) MainViewControllerTestSupport.getField(tmpController, "mainTabPane");
                for (Tab tmpTab : tmpTabPane.getTabs()) {
                    GridTabForTableView tmpGridTab = (GridTabForTableView) tmpTab;
                    Assertions.assertEquals(1, tmpGridTab.getPagination().getPageCount(),
                            "a single-item tab must start with a single page");
                    List<MoleculeDataModel> tmpItems = ((IDataTableView) tmpGridTab.getTableView()).getItemsList();
                    int tmpTargetSize = TabNames.FRAGMENTS.name().equals(tmpTab.getId()) ? 5 : 3;
                    //repeat the tab's fully set-up fixture item, so a rendered page never meets a half-built model
                    while (tmpItems.size() < tmpTargetSize) {
                        tmpItems.add(tmpItems.getFirst());
                    }
                }
                MainViewControllerTestSupport.getSettingsContainer(tmpController).setRowsPerPageSetting(2);
                for (MoleculeDataModel tmpMolecule : MainViewControllerTestSupport.getMoleculeList(tmpController)) {
                    tmpMolecule.setKeepAtomContainer(true);
                }
                for (List<FragmentDataModel> tmpFragments :
                        MainViewControllerTestSupport.getFragmentMap(tmpController).values()) {
                    for (FragmentDataModel tmpFragment : tmpFragments) {
                        tmpFragment.setKeepAtomContainer(true);
                    }
                }
                tmpController.applyGlobalSettingsChanges(true, true);
            });
            AbstractFxTestCase.waitForFxEvents();
            AbstractFxTestCase.runAndWait(() -> {
                TabPane tmpTabPane = (TabPane) MainViewControllerTestSupport.getField(tmpController, "mainTabPane");
                Assertions.assertEquals(2, tmpTabPane.getTabs().size(), "the fragments and itemization tabs must be present");
                for (Tab tmpTab : tmpTabPane.getTabs()) {
                    int tmpExpectedPageCount = TabNames.FRAGMENTS.name().equals(tmpTab.getId()) ? 3 : 2;
                    Assertions.assertEquals(tmpExpectedPageCount,
                            ((GridTabForTableView) tmpTab).getPagination().getPageCount(),
                            "the page count of tab " + tmpTab.getId() + " must be recomputed for two rows per page");
                }
            });
            for (MoleculeDataModel tmpMolecule : MainViewControllerTestSupport.getMoleculeList(tmpController)) {
                Assertions.assertFalse(tmpMolecule.isKeepAtomContainer(),
                        "the keep-atom-container setting must be propagated to every molecule");
            }
            for (List<FragmentDataModel> tmpFragments :
                    MainViewControllerTestSupport.getFragmentMap(tmpController).values()) {
                for (FragmentDataModel tmpFragment : tmpFragments) {
                    Assertions.assertFalse(tmpFragment.isKeepAtomContainer(),
                            "the keep-atom-container setting must be propagated to every fragment");
                }
            }
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private helper methods" defaultstate="collapsed">
    /**
     * Builds a static mock of {@link GuiUtil} identical to {@link FxTestUtil#mockGuiAlerts()} except that
     * {@code guiConfirmationAlert} returns {@link ButtonType#CANCEL}, so a guarded confirmation is declined. This is
     * required for the guarded {@code closeApplication} early-return, where an {@code OK} answer would instead fall
     * through to the {@code System.exit} tail and kill the fork.
     *
     * @return a static mock of {@link GuiUtil} whose confirmation alert answers {@code CANCEL}
     */
    private static MockedStatic<GuiUtil> mockGuiAlertsConfirmCancel() {
        return Mockito.mockStatic(GuiUtil.class, anInvocation -> switch (anInvocation.getMethod().getName()) {
            case "guiMessageAlert", "guiMessageAlertWithHyperlink" -> Optional.empty();
            case "guiConfirmationAlert", "guiYesNoCancelConfirmationAlert" -> ButtonType.CANCEL;
            case "guiExceptionAlert", "guiExpandableAlert" -> null;
            default -> anInvocation.callRealMethod();
        });
    }
    //
    /**
     * Creates an unstarted, do-nothing {@link Task} used as a stand-in for the controller's import/export/fragmentation
     * task fields so the {@code interrupt*} methods can be driven without a live background operation.
     *
     * @return a new no-op task
     */
    private static Task<Void> noOpTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                return null;
            }
        };
    }
    //
    /**
     * Opens an auxiliary view through {@link FxTestUtil#runAndDriveModal(java.util.concurrent.Callable,
     * java.util.function.Consumer)} so it is opened on the FX thread and ALWAYS closed (no orphan window, no hang), and
     * returns the stage the driver detected and closed, so a test can assert a view stage was actually shown rather
     * than merely that the open handler ran without throwing. The {@link GuiUtil} alerts and the {@link Desktop} static
     * are mocked INSIDE the construct (thread-confined) so no real alert or OS launch is reached. Works for both
     * blocking {@code showAndWait} and non-blocking {@code show} views, because the modal driver waits for its queued
     * driver in either case. Returns {@code null} if no window became visible during the open.
     *
     * @param aOpenAction the controller open call to drive
     * @return the modal stage that was shown and closed, or {@code null} if none opened
     */
    private Stage driveModalOpenAndCapture(Runnable aOpenAction) {
        AtomicReference<Stage> tmpShownStage = new AtomicReference<>();
        FxTestUtil.runAndDriveModal(
                () -> {
                    try (MockedStatic<GuiUtil> tmpGuiUtilMock = FxTestUtil.mockGuiAlerts();
                            MockedStatic<Desktop> tmpDesktopMock = FxTestUtil.mockDesktop()) {
                        aOpenAction.run();
                    }
                    return null;
                },
                tmpShownStage::set);
        return tmpShownStage.get();
    }
    //
    /**
     * Asserts that the modal driver detected a view stage, that the stage carries the title of the expected view and
     * that the driver closed it again.
     *
     * @param aStage the stage returned by {@link #driveModalOpenAndCapture(Runnable)}, null if none was shown
     * @param aTitleKey message-bundle key of the expected view's title
     */
    private static void assertOpenedAndClosed(Stage aStage, String aTitleKey) {
        Assertions.assertNotNull(aStage, "the view with title key " + aTitleKey + " must have shown a stage");
        Assertions.assertEquals(Message.get(aTitleKey), aStage.getTitle(), "the shown stage must be the expected view");
        Assertions.assertFalse(aStage.isShowing(), "the modal driver must have closed the view stage again");
    }
    //
    /**
     * Collects the overview/histogram view buttons of every result tab of the controller's main tab pane. These are
     * the buttons that {@code createFragmentsTab} and {@code createItemsTab} put into the right-aligned view-button
     * {@link HBox} of the tab's grid pane; they are identified by their message-bundle text so that no unrelated
     * button (an export button, or one from a pagination skin) is picked up. Must be called on the JavaFX Application
     * Thread.
     *
     * @param aController the controller under test
     * @return every result-tab view button, in tab order
     */
    private static List<Button> collectResultTabViewButtons(MainViewController aController) {
        TabPane tmpTabPane = (TabPane) MainViewControllerTestSupport.getField(aController, "mainTabPane");
        List<Button> tmpViewButtons = new ArrayList<>();
        for (Tab tmpTab : tmpTabPane.getTabs()) {
            for (Node tmpGridChild : ((GridPane) tmpTab.getContent()).getChildren()) {
                if (!(tmpGridChild instanceof HBox tmpHBox)) {
                    continue;
                }
                for (Node tmpBoxChild : tmpHBox.getChildren()) {
                    if (tmpBoxChild instanceof Button tmpButton
                            && (Message.get("MainView.showOverviewViewButton.text").equals(tmpButton.getText())
                                    || Message.get("MainView.showHistogramViewButton.text").equals(tmpButton.getText()))) {
                        tmpViewButtons.add(tmpButton);
                    }
                }
            }
        }
        return tmpViewButtons;
    }
    //
    /**
     * Selects the itemization result tab of the controller's main tab pane. Must be called on the JavaFX Application
     * Thread.
     *
     * @param aController the controller under test
     */
    private static void selectItemizationTab(MainViewController aController) {
        TabPane tmpTabPane = (TabPane) MainViewControllerTestSupport.getField(aController, "mainTabPane");
        tmpTabPane.getTabs().stream()
                .filter(aTab -> TabNames.ITEMIZATION.name().equals(aTab.getId()))
                .findFirst()
                .ifPresent(aTab -> tmpTabPane.getSelectionModel().select(aTab));
    }
    //
    /**
     * Enables cell selection on the table of the currently selected result tab and selects the first cell of the first
     * column, so the overview parent/item-sample branches (which read a single selected cell) can be driven. Must be
     * called on the JavaFX Application Thread.
     *
     * @param aController the controller under test
     */
    private static void selectFirstCellOfSelectedTab(MainViewController aController) {
        TabPane tmpTabPane = (TabPane) MainViewControllerTestSupport.getField(aController, "mainTabPane");
        TableView<?> tmpTableView = (TableView<?>)
                ((GridTabForTableView) tmpTabPane.getSelectionModel().getSelectedItem()).getTableView();
        tmpTableView.getSelectionModel().setCellSelectionEnabled(true);
        tmpTableView.getSelectionModel().clearSelection();
        tmpTableView.getSelectionModel().select(0, (TableColumn) tmpTableView.getColumns().getFirst());
    }
    //</editor-fold>
}
