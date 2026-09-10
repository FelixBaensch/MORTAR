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
import de.unijena.cheminf.mortar.gui.views.SettingsView;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;

import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Headless unit tests for {@link SettingsViewController} (COV-07). This controller is the reference BLOCKING modal of
 * this phase: its constructor ends in {@code settingsViewStage.showAndWait()} (see
 * {@code SettingsViewController.showSettingsView}), which blocks the JavaFX Application Thread in a nested event loop
 * until the stage is closed. A plain {@link AbstractFxTestCase#runAndWait(Runnable)} over the construction would
 * therefore hang to the harness timeout, so every test constructs the controller through
 * {@link FxTestUtil#runAndDriveModal(java.util.concurrent.Callable, java.util.function.Consumer)}: the construct builds
 * a real {@link SettingsContainer} and an offscreen owner {@link Stage} on the FX thread and returns the controller,
 * and the driver — scheduled to run INSIDE the nested loop, after the controller's pre-{@code showAndWait}
 * {@code Platform.runLater} (which registers the button handlers and populates the recent-properties map) has already
 * drained — obtains the {@link SettingsView} via the modal stage's scene root ({@code (SettingsView)
 * stage.getScene().getRoot()}, no production widening) and fires exactly one handler branch. The driver opens the
 * {@code try (MockedStatic<GuiUtil> ...)} on the JavaFX Application Thread — where a Mockito {@code MockedStatic} must
 * be created to intercept the FX-thread alert calls, since it is thread-local — so no code path can reach a real JavaFX
 * {@code Alert} when run headless.
 * <p>
 * The apply, cancel, default, and stage-close-request handlers are each exercised in a fresh controller instance (so no
 * closing handler cross-contaminates a sibling), and both change-flag getters
 * ({@code hasRowsPerPageChanged}/{@code hasKeepAtomContainerInDataModelChanged}) are asserted on both a true and a false
 * path. Two behaviors of the production apply handler are pinned exactly as written:
 * <ul>
 *   <li>The rows-per-page flag reflects a genuine comparison: it is {@code false} when the rows-per-page property still
 *       equals the value captured into the controller's recent-properties map, and {@code true} once the property is
 *       changed away from it before apply. The property is pinned in the driver immediately before firing apply so the
 *       view's bidirectional {@code TextFormatter} binding cannot drift the value between the set and the comparison.</li>
 *   <li>The keep-atom-container flag is unconditionally {@code true} after apply: the {@code keepAtomContainerInDataModel}
 *       setting is {@code @Deprecated} ("currently not in use, returns always false") and is excluded from
 *       {@code SettingsContainer.settingsProperties()}, so the view never records it into the recent-properties map;
 *       the apply handler then compares a non-null {@code Boolean} against {@code recentProperties.get(name) == null},
 *       which is always unequal. Its {@code false} path is therefore only reachable when apply never fires (the cancel,
 *       default, and close-request handlers leave the flag at its {@code false} default).</li>
 * </ul>
 * Assertions are behavioral only and never pin exact CDK-derived strings, because the CDK 2.12 snapshot is a moving
 * target.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class SettingsViewControllerTest extends AbstractFxTestCase {
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Default no-argument constructor; all headless setup (toolkit boot, en-GB locale, {@code user.home} isolation) is
     * inherited from {@link AbstractFxTestCase}.
     */
    public SettingsViewControllerTest() {
    }
    //</editor-fold>
    //
    //<editor-fold desc="Test methods" defaultstate="collapsed">
    /**
     * Pins the rows-per-page setting to the value captured before the view was built (the same value the controller
     * snapshotted into its recent-properties map) and fires the apply button, asserting {@code hasRowsPerPageChanged}
     * is {@code false} because the comparison found no difference. The pin resets any incidental value the view's
     * bidirectional {@code TextFormatter} binding applied during {@code addTab}, so the false branch is deterministic.
     * {@code hasKeepAtomContainerInDataModelChanged} is asserted {@code true}, pinning the deprecated-setting behavior
     * where the excluded keep-atom-container property is compared against a {@code null} recent value. Exercises the
     * constructor, {@code showSettingsView}, the {@code addListeners} registration, the apply handler (rows false branch
     * plus the keep computation), and both getters.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void applyWithUnchangedRowsPerPageLeavesRowsFlagFalseTest() throws Exception {
        SettingsViewController tmpController = this.driveModal((aStage, aView, aContainer, aBaseline) -> {
            aContainer.rowsPerPageSettingProperty().setValue(aBaseline.rowsPerPage());
            aView.getApplyButton().fire();
        });
        Assertions.assertNotNull(tmpController);
        Assertions.assertFalse(tmpController.hasRowsPerPageChanged());
        Assertions.assertTrue(tmpController.hasKeepAtomContainerInDataModelChanged());
    }
    //
    /**
     * Sets the rows-per-page property to a value that differs from the captured baseline immediately before firing the
     * apply button, asserting {@code hasRowsPerPageChanged} is {@code true} (pinning the rows-per-page true branch) and
     * {@code hasKeepAtomContainerInDataModelChanged} is {@code true} (the deprecated keep-atom-container setting is
     * always flagged after apply).
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void applyWithChangedRowsPerPageSetsRowsFlagTrueTest() throws Exception {
        SettingsViewController tmpController = this.driveModal((aStage, aView, aContainer, aBaseline) -> {
            aContainer.rowsPerPageSettingProperty().setValue(aBaseline.rowsPerPage() + 1);
            aView.getApplyButton().fire();
        });
        Assertions.assertNotNull(tmpController);
        Assertions.assertTrue(tmpController.hasRowsPerPageChanged());
        Assertions.assertTrue(tmpController.hasKeepAtomContainerInDataModelChanged());
    }
    //
    /**
     * Moves the rows-per-page setting away from the captured baseline and then fires the cancel button, asserting that
     * the {@code setRecentProperties} restore actually put the setting back — without the deliberate change first, the
     * restore would be indistinguishable from doing nothing. Both change-flag getters are still {@code false}, because
     * the cancel handler restores and closes without ever computing them; this is also the false path for
     * {@code hasKeepAtomContainerInDataModelChanged}, which apply cannot reach.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void cancelRestoresAndClosesLeavesBothFlagsFalseTest() throws Exception {
        AtomicReference<SettingsContainer> tmpContainerReference = new AtomicReference<>();
        AtomicReference<Integer> tmpBaselineReference = new AtomicReference<>();
        SettingsViewController tmpController = this.driveModal((aStage, aView, aContainer, aBaseline) -> {
            tmpContainerReference.set(aContainer);
            tmpBaselineReference.set(aBaseline.rowsPerPage());
            aContainer.rowsPerPageSettingProperty().setValue(aBaseline.rowsPerPage() + 1);
            aView.getCancelButton().fire();
        });
        Assertions.assertNotNull(tmpController);
        //the restore runs in a Platform.runLater, which driveModal drains before returning
        Assertions.assertEquals(tmpBaselineReference.get().intValue(), tmpContainerReference.get().getRowsPerPageSetting(),
                "cancelling must restore the rows-per-page setting the view was opened with");
        Assertions.assertFalse(tmpController.hasRowsPerPageChanged());
        Assertions.assertFalse(tmpController.hasKeepAtomContainerInDataModelChanged());
    }
    //
    /**
     * Moves the rows-per-page setting away from its default and then fires the default button, asserting that the
     * handler's {@code restoreDefaultSettings} branch actually put the setting back to
     * {@link SettingsContainer#ROWS_PER_PAGE_SETTING_DEFAULT} — without the deliberate change first, the assertion would
     * hold on a fresh container regardless of what the handler does. The handler does not close the stage; the
     * harness's driver always closes it afterwards. Neither change flag is computed by this handler, so both stay
     * {@code false}.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void defaultRestoresDefaultSettingsTest() throws Exception {
        AtomicReference<SettingsContainer> tmpContainerReference = new AtomicReference<>();
        SettingsViewController tmpController = this.driveModal((aStage, aView, aContainer, aBaseline) -> {
            tmpContainerReference.set(aContainer);
            aContainer.rowsPerPageSettingProperty().setValue(SettingsContainer.ROWS_PER_PAGE_SETTING_DEFAULT + 1);
            aView.getDefaultButton().fire();
        });
        Assertions.assertNotNull(tmpController);
        Assertions.assertEquals(SettingsContainer.ROWS_PER_PAGE_SETTING_DEFAULT,
                tmpContainerReference.get().getRowsPerPageSetting(),
                "the default button must restore the rows-per-page setting to its default");
        Assertions.assertFalse(tmpController.hasRowsPerPageChanged());
        Assertions.assertFalse(tmpController.hasKeepAtomContainerInDataModelChanged());
    }
    //
    /**
     * Moves the rows-per-page setting away from the captured baseline and then invokes the stage close-request handler,
     * asserting that its {@code setRecentProperties} restore actually put the setting back — as with cancel, the
     * restore is only observable if the setting was changed first. Neither change flag is computed by this handler, so
     * both stay {@code false}.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void closeRequestHandlerRestoresAndClosesTest() throws Exception {
        AtomicReference<SettingsContainer> tmpContainerReference = new AtomicReference<>();
        AtomicReference<Integer> tmpBaselineReference = new AtomicReference<>();
        SettingsViewController tmpController = this.driveModal((aStage, aView, aContainer, aBaseline) -> {
            tmpContainerReference.set(aContainer);
            tmpBaselineReference.set(aBaseline.rowsPerPage());
            aContainer.rowsPerPageSettingProperty().setValue(aBaseline.rowsPerPage() + 1);
            aStage.getOnCloseRequest().handle(new WindowEvent(aStage, WindowEvent.WINDOW_CLOSE_REQUEST));
        });
        Assertions.assertNotNull(tmpController);
        //the restore runs in a Platform.runLater, which driveModal drains before returning
        Assertions.assertEquals(tmpBaselineReference.get().intValue(), tmpContainerReference.get().getRowsPerPageSetting(),
                "the close request must restore the rows-per-page setting the view was opened with");
        Assertions.assertFalse(tmpController.hasRowsPerPageChanged());
        Assertions.assertFalse(tmpController.hasKeepAtomContainerInDataModelChanged());
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private helper methods" defaultstate="collapsed">
    /**
     * Constructs a {@link SettingsViewController} through the blocking-modal driver and fires exactly one handler branch
     * via the supplied {@link ModalDriver}. The construct — invoked on the JavaFX Application Thread by
     * {@link FxTestUtil#runAndDriveModal(java.util.concurrent.Callable, java.util.function.Consumer)} — builds a real
     * {@link SettingsContainer}, captures its {@link Baseline} (so the driver can pin the rows-per-page setting relative
     * to the recorded recent value), and an offscreen owner {@link Stage}, then returns the controller whose constructor
     * calls {@code showAndWait}. The driver, run inside the nested event loop after the controller's pre-{@code showAndWait}
     * {@code Platform.runLater} has registered the handlers and populated the recent-properties map, resolves the
     * {@link SettingsView} from the modal stage's scene root and delegates to the caller. The driver opens a
     * {@link MockedStatic} over {@link GuiUtil} on the JavaFX Application Thread (where it must be created to intercept
     * the FX-thread alert calls, as a Mockito static mock is thread-local) so no alert reaches a real headless
     * {@code Alert}, and the FX event queue is drained afterwards so any {@code setRecentProperties}
     * {@code Platform.runLater} restore work completes before the method returns.
     *
     * @param aDriver the single-branch driver to run against the shown modal stage, view, container, and baseline
     * @return the constructed controller (after its {@code showAndWait} has returned)
     * @throws Exception if the {@link Configuration} singleton cannot be obtained or the modal construct fails
     */
    private SettingsViewController driveModal(ModalDriver aDriver) throws Exception {
        Configuration tmpConfiguration = Configuration.getInstance();
        AtomicReference<SettingsContainer> tmpContainerReference = new AtomicReference<>();
        AtomicReference<Baseline> tmpBaselineReference = new AtomicReference<>();
        SettingsViewController tmpController = FxTestUtil.runAndDriveModal(
                () -> {
                    SettingsContainer tmpContainer = new SettingsContainer();
                    tmpContainerReference.set(tmpContainer);
                    tmpBaselineReference.set(new Baseline(tmpContainer.getRowsPerPageSetting()));
                    Stage tmpOwner = FxTestUtil.newOffscreenStage();
                    return new SettingsViewController(tmpOwner, tmpContainer, tmpConfiguration);
                },
                aStage -> {
                    //open the GuiUtil static mock INSIDE the driver, on the JavaFX Application Thread: a Mockito
                    //MockedStatic is thread-local and only intercepts calls made on the thread that created it, so a
                    //mock opened on the test thread would be inert against the alert calls the handlers make here
                    try (MockedStatic<GuiUtil> tmpGuiUtilMock = FxTestUtil.mockGuiAlerts()) {
                        SettingsView tmpView = (SettingsView) aStage.getScene().getRoot();
                        aDriver.drive(aStage, tmpView, tmpContainerReference.get(), tmpBaselineReference.get());
                    }
                });
        AbstractFxTestCase.waitForFxEvents();
        //surface any throwable an async Platform.runLater raised on the JavaFX Application Thread
        //(e.g. the setRecentProperties restore) — waitForFxEvents drains but does not read FX_UNCAUGHT
        AbstractFxTestCase.runAndWait(() -> { });
        return tmpController;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Nested types" defaultstate="collapsed">
    /**
     * Snapshot of the rows-per-page setting captured from the fresh {@link SettingsContainer} BEFORE the view is built.
     * This is the same value the controller records into its private recent-properties map, so a driver can pin the
     * property back to its baseline (false path) or deliberately away from it (true path) without depending on any
     * incidental value the view's bidirectional {@code TextFormatter} binding applies. The keep-atom-container setting
     * is intentionally not captured here: it is {@code @Deprecated}, excluded from {@code settingsProperties()}, and so
     * never recorded into the recent map, which makes its apply-time flag unconditionally {@code true}.
     *
     * @param rowsPerPage the baseline rows-per-page setting value
     */
    private record Baseline(int rowsPerPage) {
    }
    //
    /**
     * Callback that fires a single {@link SettingsViewController} handler branch against the shown modal stage, its
     * {@link SettingsView} scene root, the real {@link SettingsContainer} backing the controller, and the pre-view
     * {@link Baseline} (so a test can pin or deliberately change the rows-per-page setting before firing the apply
     * button to reach a given change-flag path).
     */
    @FunctionalInterface
    private interface ModalDriver {
        /**
         * Drives one handler branch of the settings view.
         *
         * @param aStage the shown modal settings-view stage
         * @param aView the settings view (the stage's scene root)
         * @param aContainer the real settings container backing the controller
         * @param aBaseline the rows-per-page snapshot captured before the view was built
         */
        void drive(Stage aStage, SettingsView aView, SettingsContainer aContainer, Baseline aBaseline);
    }
    //</editor-fold>
}
