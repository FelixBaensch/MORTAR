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
import de.unijena.cheminf.mortar.gui.views.PipelineSettingsView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.fragmentation.FragmentationService;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Headless unit tests for {@link PipelineSettingsViewController} (COV-04). This controller is the heaviest in-scope
 * settings class and, like {@link SettingsViewController}, ends its constructor in the BLOCKING
 * {@code pipelineSettingsViewStage.showAndWait()} (see {@code PipelineSettingsViewController.showPipelineSettingsView}),
 * which parks the JavaFX Application Thread in a nested event loop until the stage is closed. A plain
 * {@link AbstractFxTestCase#runAndWait(Runnable)} over the construction would therefore hang to the harness timeout, so
 * every test constructs the controller through
 * {@link FxTestUtil#runAndDriveModal(java.util.concurrent.Callable, java.util.function.Consumer)}: the construct builds a
 * real {@link FragmentationService} (no-arg constructor) whose registered fragmenter array supplies the combo-box items
 * and the default pipeline, and an offscreen owner {@link Stage} on the FX thread, then returns the controller. The
 * driver — scheduled to run INSIDE the nested loop, after the controller's pre-{@code showAndWait}
 * {@code Platform.runLater} (which builds the grid, registers the button handlers, adds the initial choice row and sets
 * the fragment-button disable state) has already drained — resolves the {@link PipelineSettingsView} from the modal
 * stage's scene root ({@code (PipelineSettingsView) stage.getScene().getRoot()}, so no production code is widened) and
 * fires exactly one handler group. The driver opens a {@code try (MockedStatic<GuiUtil> ...)} on the JavaFX Application
 * Thread — where a Mockito {@code MockedStatic} must be created to intercept the FX-thread alert calls, since it is
 * thread-local — so no code path can reach a real JavaFX {@code Alert} when run headless, and after the modal returns a
 * no-op {@code runAndWait} surfaces any throwable the driver raised on the FX thread.
 * <p>
 * Because {@code Stage.close()} (invoked by the harness driver's {@code finally}) does not fire the window's
 * close-request handler, the controller's private {@code selectedPipelineFragmentersList} and {@code algorithmCounter}
 * survive the drive, so the grid-mechanics test reflects them (mirroring the harness's own {@code FileUtil} reflection)
 * on the returned controller to pin the add/remove-row bookkeeping. Assertions are behavioral only — the fragmentation
 * flag, the pipeline name round-trip, the service receiving the name and fragmenter array, and the row counter/list size
 * as computed — and never pin exact CDK-derived strings, because the CDK 2.12 snapshot is a moving target.
 * <p>
 * The gear settings button's action (which opens a nested {@code FragmentationSettingsViewController} modal the single
 * driver could not also drive and close) is intentionally NOT fired to avoid a nested-modal hang; its creation is still
 * covered by the initial grid build. The two defensive fragmenter-not-found {@code IllegalArgumentException} branches are
 * unreachable with a real service (every combo-box display name resolves to a registered fragmenter) and are left
 * uncovered by design.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class PipelineSettingsViewControllerTest extends AbstractFxTestCase {
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Default no-argument constructor; all headless setup (toolkit boot, en-GB locale, {@code user.home} isolation) is
     * inherited from {@link AbstractFxTestCase}.
     */
    public PipelineSettingsViewControllerTest() {
    }
    //</editor-fold>
    //
    //<editor-fold desc="Test methods" defaultstate="collapsed">
    /**
     * Constructs the controller with molecule data loaded and asserts the blocking open built a grid with the initial
     * choice row (at least one combo box) and left the fragment button ENABLED, exercising the constructor,
     * {@code showPipelineSettingsView} (scene, icon, title, the pre-{@code showAndWait} {@code Platform.runLater} body),
     * {@code addListenerAndBindings} registration, the initial {@code addNewChoiceRow} (GUI-only, false branch),
     * {@code cancelChangesInFragmenterList} and the enabled branch of the fragment-button disable expression.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void constructionBuildsGridWithInitialRowTest() throws Exception {
        AtomicInteger tmpComboCount = new AtomicInteger(-1);
        AtomicBoolean tmpFragmentButtonDisabled = new AtomicBoolean(true);
        AtomicReference<FragmentationService> tmpServiceReference = new AtomicReference<>();
        PipelineSettingsViewController tmpController = this.driveModal(true, false, tmpServiceReference,
                (aStage, aView) -> {
                    tmpComboCount.set(PipelineSettingsViewControllerTest.countComboBoxes(aView.getGridPane()));
                    tmpFragmentButtonDisabled.set(aView.getFragmentButton().isDisabled());
                });
        Assertions.assertNotNull(tmpController);
        Assertions.assertTrue(tmpComboCount.get() >= 1);
        Assertions.assertFalse(tmpFragmentButtonDisabled.get());
        Assertions.assertFalse(tmpController.isFragmentationStarted());
    }
    //
    /**
     * Constructs the controller with NO molecule data loaded and asserts the fragment button was disabled, exercising the
     * disabled branch of the {@code !isMoleculeDataLoaded || isFragmentationRunning} expression.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void fragmentButtonDisabledWhenNoMoleculeDataLoadedTest() throws Exception {
        AtomicBoolean tmpFragmentButtonDisabled = new AtomicBoolean(false);
        AtomicReference<FragmentationService> tmpServiceReference = new AtomicReference<>();
        PipelineSettingsViewController tmpController = this.driveModal(false, false, tmpServiceReference,
                (aStage, aView) -> tmpFragmentButtonDisabled.set(aView.getFragmentButton().isDisabled()));
        Assertions.assertNotNull(tmpController);
        Assertions.assertTrue(tmpFragmentButtonDisabled.get());
    }
    //
    /**
     * Sets a known pipeline name into the text field and fires the fragment/run button, asserting the fragmentation flag
     * flipped to {@code true} and the service received both the pipeline name and a non-empty fragmenter array, exercising
     * the fragment-button handler.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void fragmentButtonStartsFragmentationAndPushesToServiceTest() throws Exception {
        AtomicReference<FragmentationService> tmpServiceReference = new AtomicReference<>();
        PipelineSettingsViewController tmpController = this.driveModal(true, false, tmpServiceReference,
                (aStage, aView) -> {
                    aView.getTextField().setText("myPipeline");
                    aView.getFragmentButton().fire();
                });
        Assertions.assertNotNull(tmpController);
        Assertions.assertTrue(tmpController.isFragmentationStarted());
        FragmentationService tmpService = tmpServiceReference.get();
        Assertions.assertEquals("myPipeline", tmpService.getPipeliningFragmentationName());
        Assertions.assertNotNull(tmpService.getPipelineFragmenter());
        Assertions.assertTrue(tmpService.getPipelineFragmenter().length >= 1);
    }
    //
    /**
     * Sets a known pipeline name into the text field and fires the apply button, asserting the service received the name
     * and a non-empty fragmenter array while the fragmentation flag stayed {@code false} (apply does not start a
     * fragmentation), exercising the apply-button handler.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void applyButtonPushesToServiceWithoutStartingFragmentationTest() throws Exception {
        AtomicReference<FragmentationService> tmpServiceReference = new AtomicReference<>();
        PipelineSettingsViewController tmpController = this.driveModal(true, false, tmpServiceReference,
                (aStage, aView) -> {
                    aView.getTextField().setText("appliedPipeline");
                    aView.getApplyButton().fire();
                });
        Assertions.assertNotNull(tmpController);
        Assertions.assertFalse(tmpController.isFragmentationStarted());
        FragmentationService tmpService = tmpServiceReference.get();
        Assertions.assertEquals("appliedPipeline", tmpService.getPipeliningFragmentationName());
        Assertions.assertNotNull(tmpService.getPipelineFragmenter());
        Assertions.assertTrue(tmpService.getPipelineFragmenter().length >= 1);
    }
    //
    /**
     * Fires the cancel button and asserts the controller was constructed and driven without a throwable surfacing and
     * without starting a fragmentation, exercising the cancel handler and its {@code cancelChangesInFragmenterList}
     * restore branch.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void cancelButtonRestoresAndClosesTest() throws Exception {
        AtomicReference<FragmentationService> tmpServiceReference = new AtomicReference<>();
        PipelineSettingsViewController tmpController = this.driveModal(true, false, tmpServiceReference,
                (aStage, aView) -> aView.getCancelButton().fire());
        Assertions.assertNotNull(tmpController);
        Assertions.assertFalse(tmpController.isFragmentationStarted());
    }
    //
    /**
     * Fires the default button and asserts the pipeline name was reset to the empty string, exercising the default
     * handler and the {@code reset} branch (which clears the grid rows, empties the fragmenter list, resets the counter
     * and re-adds a single choice row for the selected fragmenter).
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void defaultButtonResetsPipelineTest() throws Exception {
        AtomicReference<FragmentationService> tmpServiceReference = new AtomicReference<>();
        PipelineSettingsViewController tmpController = this.driveModal(true, false, tmpServiceReference,
                (aStage, aView) -> aView.getDefaultButton().fire());
        Assertions.assertNotNull(tmpController);
        Assertions.assertEquals("", tmpController.getPipelineName());
        Assertions.assertEquals(1, PipelineSettingsViewControllerTest.getAlgorithmCounter(tmpController));
        Assertions.assertEquals(1, PipelineSettingsViewControllerTest.getSelectedFragmentersList(tmpController).size());
    }
    //
    /**
     * Invokes the stage close-request handler and asserts the controller was constructed and driven without a throwable
     * surfacing, exercising the close-request lambda body and its {@code cancelChangesInFragmenterList} restore branch.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void closeRequestHandlerRestoresAndClosesTest() throws Exception {
        AtomicReference<FragmentationService> tmpServiceReference = new AtomicReference<>();
        PipelineSettingsViewController tmpController = this.driveModal(true, false, tmpServiceReference,
                (aStage, aView) -> aStage.getOnCloseRequest().handle(
                        new WindowEvent(aStage, WindowEvent.WINDOW_CLOSE_REQUEST)));
        Assertions.assertNotNull(tmpController);
        Assertions.assertFalse(tmpController.isFragmentationStarted());
    }
    //
    /**
     * Drives the grid mechanics in one modal: fires the add-row button twice (growing the pipeline to three rows), then
     * commits a combo-box selection on the first row (firing the combo box's action to re-set that list entry), then
     * fires the remove-row button (dropping the last row and, because more than one row remains, re-adding a remove
     * button). Reflects the controller's private {@code algorithmCounter} and {@code selectedPipelineFragmentersList}
     * afterwards (they survive the harness's {@code close()}, which does not fire the close-request handler) and asserts
     * both settled at two, exercising {@code addNewChoiceRow} (update-list branch), {@code addAddRowButton}'s handler,
     * {@code newFragmenterComboBox}'s selection handler, and {@code addRemoveRowButton}'s handler including its
     * more-than-one-row re-add branch.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void gridAddRemoveRowAndComboBoxSelectionTest() throws Exception {
        AtomicReference<FragmentationService> tmpServiceReference = new AtomicReference<>();
        PipelineSettingsViewController tmpController = this.driveModal(true, false, tmpServiceReference,
                (aStage, aView) -> {
                    GridPane tmpGrid = aView.getGridPane();
                    PipelineSettingsViewControllerTest.findButtonByText(tmpGrid,
                            Message.get("PipelineSettingsView.addNewRowButton.text")).fire();
                    PipelineSettingsViewControllerTest.findButtonByText(tmpGrid,
                            Message.get("PipelineSettingsView.addNewRowButton.text")).fire();
                    ComboBox<String> tmpComboBox = PipelineSettingsViewControllerTest.findFirstComboBox(tmpGrid);
                    tmpComboBox.getSelectionModel().select(0);
                    tmpComboBox.getOnAction().handle(new ActionEvent());
                    PipelineSettingsViewControllerTest.findButtonByText(tmpGrid,
                            Message.get("PipelineSettingsView.removeRowButton.text")).fire();
                });
        Assertions.assertNotNull(tmpController);
        Assertions.assertEquals(2, PipelineSettingsViewControllerTest.getAlgorithmCounter(tmpController));
        Assertions.assertEquals(2, PipelineSettingsViewControllerTest.getSelectedFragmentersList(tmpController).size());
    }
    //
    /**
     * Constructs the controller (firing no handler) and then exercises the pipeline-name public API on the returned
     * controller: {@code setPipelineName} followed by {@code getPipelineName} and {@code pipelineNameProperty().get()},
     * asserting the round-trip preserves the value.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void pipelineNameRoundTripTest() throws Exception {
        AtomicReference<FragmentationService> tmpServiceReference = new AtomicReference<>();
        PipelineSettingsViewController tmpController = this.driveModal(true, false, tmpServiceReference, null);
        Assertions.assertNotNull(tmpController);
        AbstractFxTestCase.runAndWait(() -> tmpController.setPipelineName("roundTripName"));
        Assertions.assertEquals("roundTripName", tmpController.getPipelineName());
        Assertions.assertEquals("roundTripName", tmpController.pipelineNameProperty().get());
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private helper methods" defaultstate="collapsed">
    /**
     * Constructs a {@link PipelineSettingsViewController} through the blocking-modal driver and fires the supplied
     * {@link ModalDriver} against the shown stage and its {@link PipelineSettingsView} scene root. The construct —
     * invoked on the JavaFX Application Thread by
     * {@link FxTestUtil#runAndDriveModal(java.util.concurrent.Callable, java.util.function.Consumer)} — builds a real
     * {@link FragmentationService} (stored into {@code aServiceReference} so a test can assert what the handlers pushed
     * to it) and an offscreen owner {@link Stage}, then returns the controller whose constructor calls
     * {@code showAndWait}. The driver, run inside the nested event loop after the controller's pre-{@code showAndWait}
     * {@code Platform.runLater} has built the grid and registered the handlers, resolves the {@link PipelineSettingsView}
     * from the modal stage's scene root and delegates to the caller (checked exceptions are rethrown unchecked so the
     * harness's FX-uncaught capture surfaces them). The driver opens a {@link MockedStatic} over {@link GuiUtil} on the
     * JavaFX Application Thread (where it must be created to intercept the FX-thread alert calls, as a Mockito static
     * mock is thread-local) so no alert reaches a real headless {@code Alert}; the FX event queue is drained afterwards
     * and a no-op {@code runAndWait} surfaces any throwable the driver raised on the FX thread.
     *
     * @param anIsMoleculeDataLoaded whether molecule data is loaded (controls the fragment-button enable state)
     * @param anIsFragmentationRunning whether a fragmentation is running (also disables the fragment button)
     * @param aServiceReference sink that receives the real {@link FragmentationService} built inside the construct
     * @param aDriver the handler group to run against the shown modal, or null to just open and close
     * @return the constructed controller (after its {@code showAndWait} has returned)
     * @throws Exception if the {@link Configuration} singleton cannot be obtained or the modal construct/driver fails
     */
    private PipelineSettingsViewController driveModal(boolean anIsMoleculeDataLoaded,
                                                      boolean anIsFragmentationRunning,
                                                      AtomicReference<FragmentationService> aServiceReference,
                                                      ModalDriver aDriver) throws Exception {
        Configuration tmpConfiguration = Configuration.getInstance();
        PipelineSettingsViewController tmpController = FxTestUtil.runAndDriveModal(
                () -> {
                    FragmentationService tmpService = new FragmentationService();
                    aServiceReference.set(tmpService);
                    Stage tmpOwner = FxTestUtil.newOffscreenStage();
                    return new PipelineSettingsViewController(
                            tmpOwner, tmpService, anIsMoleculeDataLoaded, anIsFragmentationRunning, tmpConfiguration);
                },
                aStage -> {
                    if (aDriver != null) {
                        //open the GuiUtil static mock INSIDE the driver, on the JavaFX Application Thread: a Mockito
                        //MockedStatic is thread-local and only intercepts calls made on the thread that created it, so a
                        //mock opened on the test thread would be inert against the alert calls the handlers make here
                        try (MockedStatic<GuiUtil> tmpGuiUtilMock = FxTestUtil.mockGuiAlerts()) {
                            PipelineSettingsView tmpView = (PipelineSettingsView) aStage.getScene().getRoot();
                            aDriver.drive(aStage, tmpView);
                        } catch (Exception anException) {
                            throw new RuntimeException(anException);
                        }
                    }
                });
        AbstractFxTestCase.waitForFxEvents();
        //surface any throwable the driver raised on the JavaFX Application Thread
        AbstractFxTestCase.runAndWait(() -> { });
        return tmpController;
    }
    //
    /**
     * Counts the {@link ComboBox} children currently in the given grid.
     *
     * @param aGridPane the grid to scan
     * @return the number of combo boxes in the grid
     */
    private static int countComboBoxes(GridPane aGridPane) {
        int tmpCount = 0;
        for (Node tmpNode : aGridPane.getChildren()) {
            if (tmpNode instanceof ComboBox<?>) {
                tmpCount++;
            }
        }
        return tmpCount;
    }
    //
    /**
     * Returns the first {@link ComboBox} child of the given grid.
     *
     * @param aGridPane the grid to scan
     * @return the first combo box, or null if none is present
     */
    @SuppressWarnings("unchecked")
    private static ComboBox<String> findFirstComboBox(GridPane aGridPane) {
        for (Node tmpNode : aGridPane.getChildren()) {
            if (tmpNode instanceof ComboBox<?> tmpComboBox) {
                return (ComboBox<String>) tmpComboBox;
            }
        }
        return null;
    }
    //
    /**
     * Returns the first {@link Button} child of the given grid whose text equals the given text.
     *
     * @param aGridPane the grid to scan
     * @param aText the button text to match
     * @return the matching button, or null if none is present
     */
    private static Button findButtonByText(GridPane aGridPane, String aText) {
        for (Node tmpNode : aGridPane.getChildren()) {
            if (tmpNode instanceof Button tmpButton && aText.equals(tmpButton.getText())) {
                return tmpButton;
            }
        }
        return null;
    }
    //
    /**
     * Reflectively reads the controller's private {@code algorithmCounter} field (no production code is widened).
     *
     * @param aController the controller instance
     * @return the current algorithm counter
     * @throws Exception if the field cannot be accessed
     */
    private static int getAlgorithmCounter(PipelineSettingsViewController aController) throws Exception {
        Field tmpField = PipelineSettingsViewController.class.getDeclaredField("algorithmCounter");
        tmpField.setAccessible(true);
        return tmpField.getInt(aController);
    }
    //
    /**
     * Reflectively reads the controller's private {@code selectedPipelineFragmentersList} field.
     *
     * @param aController the controller instance
     * @return the internal pipeline fragmenter list
     * @throws Exception if the field cannot be accessed
     */
    private static List<?> getSelectedFragmentersList(PipelineSettingsViewController aController) throws Exception {
        Field tmpField = PipelineSettingsViewController.class.getDeclaredField("selectedPipelineFragmentersList");
        tmpField.setAccessible(true);
        return (List<?>) tmpField.get(aController);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Nested types" defaultstate="collapsed">
    /**
     * Callback that fires one handler group of the pipeline settings view against the shown modal stage and its
     * {@link PipelineSettingsView} scene root.
     */
    @FunctionalInterface
    private interface ModalDriver {
        /**
         * Drives one handler group of the pipeline settings view.
         *
         * @param aStage the shown modal pipeline-settings-view stage
         * @param aView the pipeline settings view (the stage's scene root)
         * @throws Exception if the driven handler fails
         */
        void drive(Stage aStage, PipelineSettingsView aView) throws Exception;
    }
    //</editor-fold>
}
