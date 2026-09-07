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
import de.unijena.cheminf.mortar.gui.views.OverviewView;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Pagination;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * Headless coverage tests for the Stage-dependent residual of {@link OverviewViewController} (COV-02). This is the
 * additive counterpart to the Phase 14 characterization pins in {@code OverviewViewControllerTest} (which pin the pure
 * pagination-math and the toolkit-free {@code IViewToolController} members and are left completely untouched here);
 * JaCoCo aggregates both classes, so this class only needs to drive the parts a headless run could not reach before:
 * the whole {@code initializeAndShowOverviewView} flow, the listeners registered in {@code addListeners}, the page
 * factory ({@code createOverviewViewPage}), the grid-configuration apply/default handlers, the structure context-menu
 * item actions, the enlarged-structure view, and both window-close paths.
 * <p>
 * {@code initializeAndShowOverviewView} ends in a BLOCKING {@code overviewViewStage.showAndWait()}
 * ({@code OverviewViewController.java:466}); a plain {@code runAndWait} over it would hang to the harness timeout, so
 * every drive goes through {@link FxTestUtil#runAndDriveModal(java.util.concurrent.Callable, java.util.function.Consumer)}:
 * the construct builds an offscreen owner {@link Stage} plus a fresh controller on the FX thread and opens the modal,
 * and the driver — scheduled to run INSIDE the nested event loop — resolves the {@link OverviewView} from the modal
 * stage's scene root ({@code (OverviewView) stage.getScene().getRoot()}, no production widening) and fires the reachable
 * handlers before the helper's {@code finally} always closes the stage. Every drive is wrapped in a
 * {@code try (MockedStatic<GuiUtil> ...)} so no handler reaches a real headless {@code Alert}. The enlarged-structure
 * view opens a SECOND {@link Stage} — window-modal like the overview stage and owned by it, but shown with a
 * non-blocking {@code show()} rather than {@code showAndWait()}, so the driver is not suspended by it; the same window
 * listener detects and closes it, and the driver guards against being re-entered for that sub-stage (its scene root is
 * not an {@link OverviewView}).
 * <p>
 * Every drive runs against a molecule list in which the model at index 1 deliberately carries an unparsable SMILES
 * string, so {@code createOverviewViewPage} exercises its depiction-failure error-label branch (see
 * {@link #buildMolecules(int)}). That structure is the source of the {@code InvalidSmilesException} warnings this class
 * logs to the console; they are expected output, not a defect. Index 0 is always a valid structure so the click and
 * context-menu drives have a real depiction to target.
 * <p>
 * Private state that production never widens (the cached structure index, the structure context menu, and the
 * {@code createOverviewViewPage}/{@code showEnlargedStructureView} entry points) is reached via reflection, mirroring
 * the harness's own {@code FileUtil.appDirPath} reflection. Assertions are behavioral only (the view/stage is present,
 * a page count relation holds, no exception escapes) and never pin exact CDK-derived output, since CDK 2.12 is a moving
 * snapshot. No production code is modified.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class OverviewViewControllerHarnessTest extends AbstractFxTestCase {
    //<editor-fold desc="Private static final class constants" defaultstate="collapsed">
    /**
     * A small pool of valid SMILES codes used to build the molecule list driven into the overview view.
     */
    private static final String[] SMILES_POOL = {"c1ccccc1", "CCO", "CCC", "CCN", "c1ccncc1", "O=C=O", "CCOCC"};
    /**
     * Number of molecules built for each drive; larger than the default 25 per page so the pagination has two pages.
     */
    private static final int MOLECULE_COUNT = 30;
    //</editor-fold>
    //
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Default no-argument constructor; all headless setup (toolkit boot, en-GB locale, {@code user.home} isolation) is
     * inherited from {@link AbstractFxTestCase}.
     */
    public OverviewViewControllerHarnessTest() {
    }
    //</editor-fold>
    //
    //<editor-fold desc="Test methods" defaultstate="collapsed">
    /**
     * Drives the MOLECULES_TAB data source through the full happy path: forces a page with structure images, changes the
     * pagination page, applies a valid and an invalid (zero) grid configuration, restores the default grid, fires the
     * three copy context-menu items, and opens the enlarged-structure view. The stage is left open so the helper's
     * {@code finally} closes it (exercising the plain {@code Stage.close()} path rather than the close-request filter,
     * which the other drives cover). Behavioral assertion: the view exposes a pagination with at least two pages.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void moleculesTabHappyPathDrivesAllStructureHandlersTest() throws Exception {
        AtomicReference<Integer> tmpPageCount = new AtomicReference<>(-1);
        this.driveOverview(OverviewViewController.DataSources.MOLECULES_TAB, "Molecules", (aController, aView) -> {
            tmpPageCount.set(aView.getPagination().getPageCount());
            //force the page factory to actually render structure images (the very first factory call renders an empty
            // grid and only then flips the createStructureImages flag to true)
            OverviewViewControllerHarnessTest.setBooleanField(aController, "createStructureImages", true);
            OverviewViewControllerHarnessTest.invokeCreateOverviewViewPage(aController, 0, 5, 5);
            //exercise the mouse-enter/exit style handlers and the context-menu-request handler on a rendered structure
            OverviewViewControllerHarnessTest.fireStructureMouseEvent(aView, MouseEvent.MOUSE_ENTERED);
            OverviewViewControllerHarnessTest.fireStructureMouseEvent(aView, MouseEvent.MOUSE_EXITED);
            OverviewViewControllerHarnessTest.fireStructureDrag(aView);
            OverviewViewControllerHarnessTest.fireStructureContextMenuRequest(aView);
            //single click on the first structure schedules the deferred single-click action
            OverviewViewControllerHarnessTest.fireStructureClick(aView, 1);
            //render a page whose image dimensions fall below the minimum so the below-limit branch is exercised
            OverviewViewControllerHarnessTest.invokeCreateOverviewViewPage(aController, 0, 100, 100);
            OverviewViewControllerHarnessTest.invokeCreateOverviewViewPage(aController, 0, 5, 5);
            //exercise the pagination key handlers on the scene (last, first, next, previous page)
            OverviewViewControllerHarnessTest.firePaginationKeys(aView);
            Pagination tmpPagination = aView.getPagination();
            //change a text field then switch page to exercise the page-change text-field reset listener
            aView.getColumnsPerPageTextField().setText("9");
            aView.getRowsPerPageTextField().setText("9");
            if (tmpPagination.getPageCount() > 1) {
                tmpPagination.setCurrentPageIndex(1);
                tmpPagination.setCurrentPageIndex(0);
            }
            //valid grid configuration change
            aView.getColumnsPerPageTextField().setText("3");
            aView.getRowsPerPageTextField().setText("4");
            aView.getApplyButton().fire();
            //grid configuration exceeding the displayable maximum -> values clamped to the maximum
            aView.getColumnsPerPageTextField().setText("999");
            aView.getRowsPerPageTextField().setText("999");
            aView.getApplyButton().fire();
            //invalid grid configuration -> message alert branch (alert is neutralized by the static mock)
            aView.getColumnsPerPageTextField().setText("0");
            aView.getApplyButton().fire();
            //restore the default grid configuration
            aView.getDefaultButton().fire();
            //fire the copy context-menu items with a valid cached structure index
            OverviewViewControllerHarnessTest.setIntField(aController, "cachedIndexOfStructureInMoleculeDataModelList", 0);
            ContextMenu tmpContextMenu = (ContextMenu) OverviewViewControllerHarnessTest.getField(aController, "structureContextMenu");
            for (MenuItem tmpItem : tmpContextMenu.getItems()) {
                //skip the show-in-main-view item here (it closes the view); it is covered by a dedicated test
                if (tmpItem.getOnAction() != null && !tmpItem.getText().equals(
                        de.unijena.cheminf.mortar.message.Message.get("OverviewView.contextMenu.showInMainViewMenuItem.molecules"))) {
                    tmpItem.fire();
                }
            }
            //open the enlarged-structure view directly (its second stage is auto-closed by the window listener) and
            //resize it so its structure-depiction resize listener runs
            Stage tmpEnlargedStage = OverviewViewControllerHarnessTest.invokeShowEnlargedStructureView(aController,
                    OverviewViewControllerHarnessTest.firstModel(aController), aView);
            if (tmpEnlargedStage != null) {
                tmpEnlargedStage.setWidth(tmpEnlargedStage.getWidth() + 80.0);
                tmpEnlargedStage.setHeight(tmpEnlargedStage.getHeight() + 80.0);
            }
        });
        Assertions.assertTrue(tmpPageCount.get() >= 2,
                "30 molecules at the default 25 per page should paginate into at least two pages");
    }
    //
    /**
     * Drives the MOLECULES_TAB data source and simulates a single- then double-click on the first rendered structure.
     * The single click schedules the enlarged-view single-click action; the double click cancels it and, because the
     * molecules tab enables the show-in-main-view option, sets the return-to-structure flag and closes the view via
     * {@code closeOverviewViewEvent}. Behavioral assertion: after the return-to-structure double click, the cached index
     * reads back as the clicked structure's index (0) rather than the -1 marker.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void moleculesTabStructureClickReturnsToStructureTest() throws Exception {
        AtomicReference<OverviewViewController> tmpControllerRef = new AtomicReference<>();
        this.driveOverview(OverviewViewController.DataSources.MOLECULES_TAB, "Molecules", (aController, aView) -> {
            tmpControllerRef.set(aController);
            OverviewViewControllerHarnessTest.setBooleanField(aController, "createStructureImages", true);
            OverviewViewControllerHarnessTest.invokeCreateOverviewViewPage(aController, 0, 5, 5);
            //single click schedules the deferred single-click action; double click cancels it and returns to structure
            OverviewViewControllerHarnessTest.fireStructureClick(aView, 1);
            OverviewViewControllerHarnessTest.fireStructureClick(aView, 2);
        });
        //the controller instance survives (only its GUI caches are cleared on close); because the return-to-structure
        //double click sets returnToStructureEventOccurred before closing, closeOverviewViewEvent does NOT reset the
        //cached index, so it must read back as the clicked structure's index (0) rather than the -1 marker
        int tmpCachedIndex = (int) OverviewViewControllerHarnessTest.getField(
                tmpControllerRef.get(), "cachedIndexOfStructureInMoleculeDataModelList");
        Assertions.assertEquals(0, tmpCachedIndex,
                "the return-to-structure double click must cache the clicked structure's index (0), not the -1 marker");
    }
    //
    /**
     * Drives the FRAGMENTS_TAB data source (a distinct title branch and the "fragments" show-in-main-view menu-item
     * text) and fires the show-in-main-view context-menu item, which sets the return-to-structure flag and closes the
     * view. Behavioral assertion: the drive completes without an exception escaping the FX thread.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void fragmentsTabShowInMainViewMenuItemClosesViewTest() throws Exception {
        this.driveOverview(OverviewViewController.DataSources.FRAGMENTS_TAB, "Fragments", (aController, aView) -> {
            OverviewViewControllerHarnessTest.setIntField(aController, "cachedIndexOfStructureInMoleculeDataModelList", 0);
            ContextMenu tmpContextMenu = (ContextMenu) OverviewViewControllerHarnessTest.getField(aController, "structureContextMenu");
            //last item is the enabled show-in-main-view item for a tab data source
            MenuItem tmpShowInMainViewItem = tmpContextMenu.getItems().get(tmpContextMenu.getItems().size() - 1);
            tmpShowInMainViewItem.fire();
        });
    }
    //
    /**
     * Drives the PARENT_MOLECULES_SAMPLE data source (which highlights the first structure and disables the
     * show-in-main-view option) and closes the view through a fired {@code WINDOW_CLOSE_REQUEST}, exercising the
     * close-request event filter and {@code closeOverviewViewEvent}. Behavioral assertion: the drive completes without
     * an exception escaping the FX thread.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void parentMoleculesSampleClosesViaWindowCloseRequestTest() throws Exception {
        this.driveOverview(OverviewViewController.DataSources.PARENT_MOLECULES_SAMPLE, null, (aController, aView) -> {
            OverviewViewControllerHarnessTest.setBooleanField(aController, "createStructureImages", true);
            OverviewViewControllerHarnessTest.invokeCreateOverviewViewPage(aController, 0, 5, 5);
            Stage tmpStage = (Stage) aView.getScene().getWindow();
            tmpStage.fireEvent(new WindowEvent(tmpStage, WindowEvent.WINDOW_CLOSE_REQUEST));
        });
    }
    //
    /**
     * Drives the ITEM_WITH_FRAGMENTS_SAMPLE data source (its own title branch) and closes the view via the close button,
     * exercising the close-button action handler and {@code closeOverviewViewEvent}. Behavioral assertion: the drive
     * completes without an exception escaping the FX thread.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void itemWithFragmentsSampleClosesViaCloseButtonTest() throws Exception {
        this.driveOverview(OverviewViewController.DataSources.ITEM_WITH_FRAGMENTS_SAMPLE, null, (aController, aView) -> {
            OverviewViewControllerHarnessTest.setBooleanField(aController, "createStructureImages", true);
            OverviewViewControllerHarnessTest.invokeCreateOverviewViewPage(aController, 0, 5, 5);
            aView.getCloseButton().fire();
        });
    }
    //
    /**
     * Pins the null-argument guards of {@code initializeAndShowOverviewView}: a null main stage, a null data source, and
     * a null molecule list each throw {@link NullPointerException} before the blocking {@code showAndWait} is reached, so
     * the guards are exercised on the FX thread without opening any modal.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void initializeAndShowOverviewViewNullArgumentsThrowTest() throws Exception {
        AtomicBoolean tmpNullMainStageThrew = new AtomicBoolean(false);
        AtomicBoolean tmpNullDataSourceThrew = new AtomicBoolean(false);
        AtomicBoolean tmpNullListThrew = new AtomicBoolean(false);
        List<MoleculeDataModel> tmpMolecules = OverviewViewControllerHarnessTest.buildMolecules(2);
        Configuration tmpConfiguration = Configuration.getInstance();
        AbstractFxTestCase.runAndWait(() -> {
            OverviewViewController tmpController = new OverviewViewController(tmpConfiguration, new SettingsContainer());
            try {
                tmpController.initializeAndShowOverviewView(null, OverviewViewController.DataSources.MOLECULES_TAB,
                        "Molecules", tmpMolecules);
            } catch (NullPointerException anException) {
                tmpNullMainStageThrew.set(true);
            }
            try {
                tmpController.initializeAndShowOverviewView(FxTestUtil.newOffscreenStage(), null, "Molecules", tmpMolecules);
            } catch (NullPointerException anException) {
                tmpNullDataSourceThrew.set(true);
            }
            try {
                tmpController.initializeAndShowOverviewView(FxTestUtil.newOffscreenStage(),
                        OverviewViewController.DataSources.MOLECULES_TAB, "Molecules", null);
            } catch (NullPointerException anException) {
                tmpNullListThrew.set(true);
            }
        });
        Assertions.assertTrue(tmpNullMainStageThrew.get(), "a null main stage must throw NullPointerException");
        Assertions.assertTrue(tmpNullDataSourceThrew.get(), "a null data source must throw NullPointerException");
        Assertions.assertTrue(tmpNullListThrew.get(), "a null molecule list must throw NullPointerException");
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private helper methods" defaultstate="collapsed">
    /**
     * Opens the overview view for the given data source through the blocking-modal driver and runs the supplied driver
     * against the shown modal stage (skipping any auto-opened sub-stage whose scene root is not an {@link OverviewView}).
     * The driver is wrapped in a static {@link GuiUtil} mock so no handler reaches a real headless alert, and any
     * throwable escaping the driver is captured and re-surfaced on the test thread as an {@link AssertionError}. The FX
     * event queue is drained afterwards so any handler-scheduled work (including the auto-close of a sub-stage) completes.
     *
     * @param aDataSource the data source to drive
     * @param aTabName the tab name passed to the controller (may be null for the sample data sources)
     * @param aDriver the callback fired on the resolved controller and view inside the nested loop
     */
    private void driveOverview(OverviewViewController.DataSources aDataSource, String aTabName,
            BiConsumer<OverviewViewController, OverviewView> aDriver) {
        AtomicReference<OverviewViewController> tmpControllerRef = new AtomicReference<>();
        AtomicReference<Throwable> tmpDriverError = new AtomicReference<>();
        List<MoleculeDataModel> tmpMolecules = OverviewViewControllerHarnessTest.buildMolecules(
                OverviewViewControllerHarnessTest.MOLECULE_COUNT);
        FxTestUtil.runAndDriveModal(
                () -> {
                    OverviewViewController tmpController = new OverviewViewController(Configuration.getInstance(), new SettingsContainer());
                    tmpControllerRef.set(tmpController);
                    tmpController.initializeAndShowOverviewView(FxTestUtil.newOffscreenStage(), aDataSource, aTabName,
                            tmpMolecules);
                    return tmpController;
                },
                aStage -> {
                    if (!(aStage.getScene().getRoot() instanceof OverviewView tmpView)) {
                        //an auto-opened sub-stage (e.g. the enlarged-structure view); let the helper close it
                        return;
                    }
                    try (MockedStatic<GuiUtil> tmpGuiUtilMock = FxTestUtil.mockGuiAlerts()) {
                        aDriver.accept(tmpControllerRef.get(), tmpView);
                    } catch (Throwable anError) {
                        tmpDriverError.set(anError);
                    }
                });
        AbstractFxTestCase.waitForFxEvents();
        if (tmpDriverError.get() != null) {
            throw new AssertionError("Overview driver failed on the JavaFX Application Thread", tmpDriverError.get());
        }
    }
    //
    /**
     * Builds a list of the given number of {@link MoleculeDataModel} instances from a small pool of valid SMILES codes,
     * with one deliberate exception: if the requested count is greater than one, the model at index 1 is replaced by one
     * carrying an unparsable SMILES string, so {@code createOverviewViewPage} exercises its depiction-failure
     * error-label branch. That model is what makes this class log {@code InvalidSmilesException} warnings to the
     * console; the warnings are expected. Index 0 always stays a valid structure so the click and context-menu drives
     * have a real depiction to target.
     *
     * @param aCount number of molecule data models to build
     * @return a mutable list of molecule data models
     */
    private static List<MoleculeDataModel> buildMolecules(int aCount) {
        List<MoleculeDataModel> tmpList = new ArrayList<>(aCount);
        for (int i = 0; i < aCount; i++) {
            String tmpSmiles = OverviewViewControllerHarnessTest.SMILES_POOL[i % OverviewViewControllerHarnessTest.SMILES_POOL.length];
            tmpList.add(new MoleculeDataModel(tmpSmiles, "Molecule" + i, new HashMap<>()));
        }
        //inject a molecule with an unparsable SMILES at index 1 (index 0 stays a valid structure for click targeting)
        //so that createOverviewViewPage exercises its depiction-failure error-label branch
        if (aCount > 1) {
            tmpList.set(1, new MoleculeDataModel("this_is_not_a_valid_smiles", "BadMolecule", new HashMap<>()));
        }
        return tmpList;
    }
    //
    /**
     * Returns the first {@link MoleculeDataModel} of the controller's current molecule list via reflection.
     *
     * @param aController the controller under test
     * @return the first molecule data model in the controller's list
     */
    @SuppressWarnings("unchecked")
    private static MoleculeDataModel firstModel(OverviewViewController aController) {
        try {
            List<MoleculeDataModel> tmpList =
                    (List<MoleculeDataModel>) OverviewViewControllerHarnessTest.getField(aController, "moleculeDataModelList");
            return tmpList.get(0);
        } catch (RuntimeException anException) {
            throw anException;
        }
    }
    //
    /**
     * Fires a synthesized primary-button {@link MouseEvent} of the given click count at the first child node of the
     * view's structure grid pane, so the grid pane's mouse-click handler is exercised with a real target node. Does
     * nothing if the grid pane is empty (e.g. if the headless layout produced no rendered structure images).
     *
     * @param aView the overview view whose structure grid pane is clicked
     * @param aClickCount the click count to simulate (1 for single click, 2 for double click)
     */
    private static void fireStructureClick(OverviewView aView, int aClickCount) {
        GridPane tmpStructureGridPane = aView.getStructureGridPane();
        if (tmpStructureGridPane.getChildren().isEmpty()) {
            return;
        }
        Node tmpTargetNode = tmpStructureGridPane.getChildren().get(0);
        MouseEvent tmpBaseEvent = new MouseEvent(MouseEvent.MOUSE_CLICKED, 1.0, 1.0, 1.0, 1.0, MouseButton.PRIMARY,
                aClickCount, false, false, false, false, true, false, false, true, false, false, null);
        //copyFor sets the event target to the structure node so the grid pane's click handler runs its structure branch
        MouseEvent tmpTargetedEvent = tmpBaseEvent.copyFor(tmpTargetNode, tmpTargetNode);
        if (tmpStructureGridPane.getOnMouseClicked() != null) {
            tmpStructureGridPane.getOnMouseClicked().handle(tmpTargetedEvent);
        }
    }
    //
    /**
     * Fires a synthesized primary-button mouse event of the given type at the first child node of the view's structure
     * grid pane. Used to exercise the mouse-enter/exit style handlers registered on the rendered structure nodes. Does
     * nothing if the grid pane is empty.
     *
     * @param aView the overview view whose first structure node receives the event
     * @param anEventType the mouse event type to fire (e.g. {@code MOUSE_ENTERED}, {@code MOUSE_EXITED})
     */
    private static void fireStructureMouseEvent(OverviewView aView, EventType<MouseEvent> anEventType) {
        GridPane tmpStructureGridPane = aView.getStructureGridPane();
        if (tmpStructureGridPane.getChildren().isEmpty()) {
            return;
        }
        Node tmpTargetNode = tmpStructureGridPane.getChildren().get(0);
        MouseEvent tmpMouseEvent = new MouseEvent(anEventType, 1.0, 1.0, 1.0, 1.0, MouseButton.PRIMARY, 0,
                false, false, false, false, true, false, false, true, false, false, null);
        Event.fireEvent(tmpTargetNode, tmpMouseEvent);
    }
    //
    /**
     * Fires a synthesized primary-button drag event at the structure grid pane so the drag-detection handler sets its
     * drag flag. Does nothing if the grid pane is empty.
     *
     * @param aView the overview view whose structure grid pane receives the drag event
     */
    private static void fireStructureDrag(OverviewView aView) {
        GridPane tmpStructureGridPane = aView.getStructureGridPane();
        MouseEvent tmpMouseEvent = new MouseEvent(MouseEvent.MOUSE_DRAGGED, 1.0, 1.0, 1.0, 1.0, MouseButton.PRIMARY, 0,
                false, false, false, false, true, false, false, true, false, false, null);
        Event.fireEvent(tmpStructureGridPane, tmpMouseEvent);
    }
    //
    /**
     * Fires a synthesized {@link ContextMenuEvent} at the first child node of the view's structure grid pane so the
     * context-menu-request handler is exercised. Does nothing if the grid pane is empty.
     *
     * @param aView the overview view whose first structure node receives the context-menu request
     */
    private static void fireStructureContextMenuRequest(OverviewView aView) {
        GridPane tmpStructureGridPane = aView.getStructureGridPane();
        if (tmpStructureGridPane.getChildren().isEmpty()) {
            return;
        }
        Node tmpTargetNode = tmpStructureGridPane.getChildren().get(0);
        ContextMenuEvent tmpBaseEvent = new ContextMenuEvent(ContextMenuEvent.CONTEXT_MENU_REQUESTED,
                1.0, 1.0, 1.0, 1.0, false, null);
        //copyFor sets the event target to the structure node so the grid pane's context-menu handler runs
        ContextMenuEvent tmpTargetedEvent = tmpBaseEvent.copyFor(tmpTargetNode, tmpTargetNode);
        if (tmpStructureGridPane.getOnContextMenuRequested() != null) {
            try {
                tmpStructureGridPane.getOnContextMenuRequested().handle(tmpTargetedEvent);
            } catch (Throwable anIgnoredPopupFailure) {
                //showing the popup context menu may fail on a headless host; the handler body up to the show() call
                //is still exercised and the popup failure must not abort the drive
            }
        }
    }
    //
    /**
     * Fires the four pagination-control key events (last page, first page, next page, previous page) on the view's
     * scene so the scene-level {@code KEY_PRESSED} handler is exercised on each branch.
     *
     * @param aView the overview view whose scene receives the key events
     */
    private static void firePaginationKeys(OverviewView aView) {
        KeyCode[] tmpKeyCodes = {KeyCode.END, KeyCode.HOME, KeyCode.RIGHT, KeyCode.LEFT};
        for (KeyCode tmpKeyCode : tmpKeyCodes) {
            KeyEvent tmpKeyEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", tmpKeyCode,
                    false, false, false, false);
            Event.fireEvent(aView.getScene(), tmpKeyEvent);
        }
    }
    //
    /**
     * Reflectively invokes the private {@code createOverviewViewPage(int, int, int)} method so a page with rendered
     * structure images is produced synchronously.
     *
     * @param aController the controller under test
     * @param aPageIndex the page index to create
     * @param aRowsPerPage the number of rows per page
     * @param aColumnsPerPage the number of columns per page
     */
    private static void invokeCreateOverviewViewPage(OverviewViewController aController, int aPageIndex,
            int aRowsPerPage, int aColumnsPerPage) {
        try {
            Method tmpMethod = OverviewViewController.class.getDeclaredMethod("createOverviewViewPage",
                    int.class, int.class, int.class);
            tmpMethod.setAccessible(true);
            tmpMethod.invoke(aController, aPageIndex, aRowsPerPage, aColumnsPerPage);
        } catch (ReflectiveOperationException anException) {
            throw new RuntimeException("Could not invoke createOverviewViewPage via reflection", anException);
        }
    }
    //
    /**
     * Reflectively invokes the private {@code showEnlargedStructureView(MoleculeDataModel, Stage)} method so the
     * enlarged-structure view is opened, and returns the newly shown enlarged {@link Stage} (identified as the showing
     * stage that was not present before the call and whose scene root is not an {@link OverviewView}), or null if it
     * could not be resolved.
     *
     * @param aController the controller under test
     * @param aMoleculeDataModel the molecule to depict in the enlarged view
     * @param aView the overview view whose stage owns the enlarged view
     * @return the newly opened enlarged-structure-view stage, or null if it could not be resolved
     */
    private static Stage invokeShowEnlargedStructureView(OverviewViewController aController,
            MoleculeDataModel aMoleculeDataModel, OverviewView aView) {
        List<Window> tmpWindowsBefore = new ArrayList<>(Window.getWindows());
        try {
            Stage tmpOwnerStage = (Stage) aView.getScene().getWindow();
            Method tmpMethod = OverviewViewController.class.getDeclaredMethod("showEnlargedStructureView",
                    MoleculeDataModel.class, Stage.class);
            tmpMethod.setAccessible(true);
            tmpMethod.invoke(aController, aMoleculeDataModel, tmpOwnerStage);
        } catch (ReflectiveOperationException anException) {
            throw new RuntimeException("Could not invoke showEnlargedStructureView via reflection", anException);
        }
        for (Window tmpWindow : Window.getWindows()) {
            if (tmpWindow instanceof Stage tmpStage && tmpStage.isShowing() && !tmpWindowsBefore.contains(tmpWindow)
                    && !(tmpStage.getScene().getRoot() instanceof OverviewView)) {
                return tmpStage;
            }
        }
        return null;
    }
    //
    /**
     * Reflectively reads the value of a private field of the given controller.
     *
     * @param aController the controller under test
     * @param aFieldName the name of the field to read
     * @return the current field value
     */
    private static Object getField(OverviewViewController aController, String aFieldName) {
        try {
            Field tmpField = OverviewViewController.class.getDeclaredField(aFieldName);
            tmpField.setAccessible(true);
            return tmpField.get(aController);
        } catch (ReflectiveOperationException anException) {
            throw new RuntimeException("Could not read field " + aFieldName + " via reflection", anException);
        }
    }
    //
    /**
     * Reflectively sets a private {@code int} field of the given controller.
     *
     * @param aController the controller under test
     * @param aFieldName the name of the field to set
     * @param aValue the value to set
     */
    private static void setIntField(OverviewViewController aController, String aFieldName, int aValue) {
        try {
            Field tmpField = OverviewViewController.class.getDeclaredField(aFieldName);
            tmpField.setAccessible(true);
            tmpField.setInt(aController, aValue);
        } catch (ReflectiveOperationException anException) {
            throw new RuntimeException("Could not set field " + aFieldName + " via reflection", anException);
        }
    }
    //
    /**
     * Reflectively sets a private {@code boolean} field of the given controller.
     *
     * @param aController the controller under test
     * @param aFieldName the name of the field to set
     * @param aValue the value to set
     */
    private static void setBooleanField(OverviewViewController aController, String aFieldName, boolean aValue) {
        try {
            Field tmpField = OverviewViewController.class.getDeclaredField(aFieldName);
            tmpField.setAccessible(true);
            tmpField.setBoolean(aController, aValue);
        } catch (ReflectiveOperationException anException) {
            throw new RuntimeException("Could not set field " + aFieldName + " via reflection", anException);
        }
    }
    //</editor-fold>
}
