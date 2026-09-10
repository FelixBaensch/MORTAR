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
import de.unijena.cheminf.mortar.gui.views.SettingsView;
import de.unijena.cheminf.mortar.model.fragmentation.FragmentationService;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.IMoleculeFragmenter;

import javafx.beans.property.Property;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Headless unit tests for {@link FragmentationSettingsViewController} (COV-08). Unlike the four blocking modal
 * controllers of this phase, this controller's constructor ends in the NON-blocking {@code fragmentationSettingsViewStage.show()}
 * (see {@code FragmentationSettingsViewController.openFragmentationSettingsView}), so a plain
 * {@link AbstractFxTestCase#runAndWait(Runnable)} over the construction returns immediately and no modal-driving helper
 * is required. Each test therefore constructs the controller on the JavaFX Application Thread with a real
 * {@link FragmentationService} fragmenter array and an offscreen owner {@link Stage} from {@link FxTestUtil}, then
 * reaches the controller's {@code private settingsView} / {@code fragmentationSettingsViewStage} fields via reflection
 * (mirroring the harness's own {@code FileUtil} reflection, so no production code is widened) to fire each registered
 * handler in isolation: the apply button (closes the stage), the cancel button (restores every fragmenter's recent
 * properties, then closes), the default button (restores the selected tab's fragmenter defaults), and the stage
 * close-request handler (restores the selected fragmenter's recent properties, then closes). Every test closes the
 * stage in a {@code finally} so no window leaks into a sibling test.
 * <p>
 * Assertions are behavioral only — one tab per registered fragmenter, a flipped setting restored by the restoring
 * handlers, and the stage's showing state after each handler — and never pin exact fragmenter-derived display strings.
 * Real objects only apart from the sanctioned {@code controller/} static-mock allowance (not needed here, as none of
 * these handlers reaches a {@code GuiUtil} alert).
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class FragmentationSettingsViewControllerTest extends AbstractFxTestCase {
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Default no-argument constructor; all headless setup (toolkit boot, en-GB locale, {@code user.home} isolation) is
     * inherited from {@link AbstractFxTestCase}.
     */
    public FragmentationSettingsViewControllerTest() {
    }
    //</editor-fold>
    //
    //<editor-fold desc="Test methods" defaultstate="collapsed">
    /**
     * Constructs the controller and asserts the non-blocking open built exactly one settings tab per registered
     * fragmenter and left the stage showing, exercising the constructor, {@code openFragmentationSettingsView} (scene,
     * icon, title, tab loop, selection) and {@code addListener} registration.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void constructionBuildsOneTabPerFragmenterTest() throws Exception {
        FragmentationService tmpService = new FragmentationService();
        FragmentationSettingsViewController tmpController = this.constructController(tmpService);
        try {
            Assertions.assertNotNull(tmpController);
            SettingsView tmpView = this.getSettingsView(tmpController);
            Assertions.assertNotNull(tmpView);
            Assertions.assertEquals(tmpService.getFragmenters().length, tmpView.getTabPane().getTabs().size());
            Stage tmpStage = this.getStage(tmpController);
            Assertions.assertNotNull(tmpStage);
            Assertions.assertTrue(tmpStage.isShowing());
        } finally {
            this.closeQuietly(tmpController);
        }
    }
    //
    /**
     * Fires the apply button and asserts its handler closed the stage.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void applyButtonClosesStageTest() throws Exception {
        FragmentationSettingsViewController tmpController = this.constructController(new FragmentationService());
        try {
            SettingsView tmpView = this.getSettingsView(tmpController);
            Stage tmpStage = this.getStage(tmpController);
            AbstractFxTestCase.runAndWait(() -> tmpView.getApplyButton().fire());
            AbstractFxTestCase.waitForFxEvents();
            Assertions.assertFalse(tmpStage.isShowing());
        } finally {
            this.closeQuietly(tmpController);
        }
    }
    //
    /**
     * Fires the cancel button and asserts its handler restored the recent properties of every fragmenter and closed the
     * stage, exercising {@code setRecentProperties} across all fragmenters. One boolean setting per fragmenter is
     * flipped away from the value the controller snapshotted at construction time, so the restore is actually
     * observable rather than merely assumed.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void cancelButtonRestoresAndClosesStageTest() throws Exception {
        FragmentationService tmpService = new FragmentationService();
        FragmentationSettingsViewController tmpController = this.constructController(tmpService);
        try {
            SettingsView tmpView = this.getSettingsView(tmpController);
            Stage tmpStage = this.getStage(tmpController);
            List<FlippedSetting> tmpFlipped = new ArrayList<>();
            for (IMoleculeFragmenter tmpFragmenter : tmpService.getFragmenters()) {
                tmpFlipped.add(FragmentationSettingsViewControllerTest.flipFirstBooleanSetting(tmpFragmenter));
            }
            AbstractFxTestCase.runAndWait(() -> tmpView.getCancelButton().fire());
            AbstractFxTestCase.waitForFxEvents();
            for (FlippedSetting tmpSetting : tmpFlipped) {
                Assertions.assertEquals(tmpSetting.originalValue(), tmpSetting.property().getValue(),
                        "Cancel did not restore the recent value of setting " + tmpSetting.property().getName());
            }
            Assertions.assertFalse(tmpStage.isShowing());
        } finally {
            this.closeQuietly(tmpController);
        }
    }
    //
    /**
     * Fires the default button and asserts its handler restored the selected tab's fragmenter defaults without throwing
     * or closing the stage (the default handler does not close), exercising the {@code restoreDefaultSettings} branch.
     * A boolean setting of the selected fragmenter is flipped first so the restore is observable, and a second
     * fragmenter is flipped too and asserted to be left alone, which pins the handler's selected-tab-only scope.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void defaultButtonRestoresSelectedFragmenterTest() throws Exception {
        FragmentationService tmpService = new FragmentationService();
        FragmentationSettingsViewController tmpController = this.constructController(tmpService);
        try {
            SettingsView tmpView = this.getSettingsView(tmpController);
            Stage tmpStage = this.getStage(tmpController);
            IMoleculeFragmenter tmpSelected = this.getSelectedFragmenter(tmpService);
            //the service is freshly constructed under an isolated user home, so no persisted settings exist and the
            //value captured here is the fragmenter's default - which is what restoreDefaultSettings must restore
            FlippedSetting tmpSelectedSetting = FragmentationSettingsViewControllerTest.flipFirstBooleanSetting(tmpSelected);
            FlippedSetting tmpUntouchedSetting = null;
            for (IMoleculeFragmenter tmpFragmenter : tmpService.getFragmenters()) {
                if (tmpFragmenter != tmpSelected) {
                    tmpUntouchedSetting = FragmentationSettingsViewControllerTest.flipFirstBooleanSetting(tmpFragmenter);
                    break;
                }
            }
            AbstractFxTestCase.runAndWait(() -> tmpView.getDefaultButton().fire());
            AbstractFxTestCase.waitForFxEvents();
            Assertions.assertEquals(tmpSelectedSetting.originalValue(), tmpSelectedSetting.property().getValue(),
                    "The default button did not restore the selected fragmenter's default for setting "
                            + tmpSelectedSetting.property().getName());
            if (tmpUntouchedSetting != null) {
                Assertions.assertEquals(!tmpUntouchedSetting.originalValue(), tmpUntouchedSetting.property().getValue(),
                        "The default button must only touch the selected tab's fragmenter, but it also reset "
                                + tmpUntouchedSetting.property().getName());
            }
            Assertions.assertTrue(tmpStage.isShowing());
        } finally {
            this.closeQuietly(tmpController);
        }
    }
    //
    /**
     * Invokes the stage close-request handler and asserts it restored the selected fragmenter's recent properties and
     * closed the stage, exercising the close-request lambda body and {@code setRecentProperties}.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void closeRequestHandlerRestoresAndClosesStageTest() throws Exception {
        FragmentationService tmpService = new FragmentationService();
        FragmentationSettingsViewController tmpController = this.constructController(tmpService);
        try {
            Stage tmpStage = this.getStage(tmpController);
            FlippedSetting tmpSetting = FragmentationSettingsViewControllerTest.flipFirstBooleanSetting(
                    this.getSelectedFragmenter(tmpService));
            AbstractFxTestCase.runAndWait(() -> tmpStage.getOnCloseRequest().handle(
                    new WindowEvent(tmpStage, WindowEvent.WINDOW_CLOSE_REQUEST)));
            AbstractFxTestCase.waitForFxEvents();
            Assertions.assertEquals(tmpSetting.originalValue(), tmpSetting.property().getValue(),
                    "The close-request handler did not restore the recent value of setting "
                            + tmpSetting.property().getName());
            Assertions.assertFalse(tmpStage.isShowing());
        } finally {
            this.closeQuietly(tmpController);
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private helper methods" defaultstate="collapsed">
    /**
     * Constructs a {@link FragmentationSettingsViewController} on the JavaFX Application Thread using the given service's
     * real fragmenter array and its currently selected fragmenter display name, over an offscreen owner stage, then
     * drains the FX event queue so the non-blocking open completes.
     *
     * @param aFragmentationService source of the real {@link IMoleculeFragmenter} array and the selected display name
     * @return the constructed controller
     * @throws Exception if construction fails on the FX thread
     */
    private FragmentationSettingsViewController constructController(FragmentationService aFragmentationService) throws Exception {
        IMoleculeFragmenter[] tmpFragmenters = aFragmentationService.getFragmenters();
        String tmpSelectedDisplayName = aFragmentationService.getSelectedFragmenterDisplayName();
        Configuration tmpConfiguration = Configuration.getInstance();
        AtomicReference<FragmentationSettingsViewController> tmpReference = new AtomicReference<>();
        AbstractFxTestCase.runAndWait(() -> {
            Stage tmpOwner = FxTestUtil.newOffscreenStage();
            tmpReference.set(new FragmentationSettingsViewController(
                    tmpOwner, tmpFragmenters, tmpSelectedDisplayName, tmpConfiguration));
        });
        AbstractFxTestCase.waitForFxEvents();
        return tmpReference.get();
    }
    //
    /**
     * Reflectively reads the controller's {@code private settingsView} field (no production code is widened).
     *
     * @param aController the controller instance
     * @return the controller's {@link SettingsView}
     * @throws Exception if the field cannot be accessed
     */
    private SettingsView getSettingsView(FragmentationSettingsViewController aController) throws Exception {
        Field tmpField = FragmentationSettingsViewController.class.getDeclaredField("settingsView");
        tmpField.setAccessible(true);
        return (SettingsView) tmpField.get(aController);
    }
    //
    /**
     * Reflectively reads the controller's {@code private fragmentationSettingsViewStage} field.
     *
     * @param aController the controller instance
     * @return the controller's settings-view {@link Stage}
     * @throws Exception if the field cannot be accessed
     */
    private Stage getStage(FragmentationSettingsViewController aController) throws Exception {
        Field tmpField = FragmentationSettingsViewController.class.getDeclaredField("fragmentationSettingsViewStage");
        tmpField.setAccessible(true);
        return (Stage) tmpField.get(aController);
    }
    //
    /**
     * Returns the fragmenter of the given service whose display name matches the service's currently selected one -
     * the fragmenter the default button and the close-request handler operate on.
     *
     * @param aFragmentationService service holding the fragmenter array and the selected display name
     * @return the selected fragmenter
     */
    private IMoleculeFragmenter getSelectedFragmenter(FragmentationService aFragmentationService) {
        String tmpSelectedDisplayName = aFragmentationService.getSelectedFragmenterDisplayName();
        for (IMoleculeFragmenter tmpFragmenter : aFragmentationService.getFragmenters()) {
            if (tmpFragmenter.getFragmentationAlgorithmDisplayName().equals(tmpSelectedDisplayName)) {
                return tmpFragmenter;
            }
        }
        throw new IllegalStateException("No fragmenter matches the selected display name " + tmpSelectedDisplayName);
    }
    //
    /**
     * Flips the first boolean setting of the given fragmenter away from its current value and returns the property
     * together with the value it held before. This is what makes the restore assertions meaningful: the controller
     * snapshots every fragmenter's settings at construction time, so a flipped setting must be back to the
     * snapshotted value once a restoring handler has run. Asserting only that the stage closed would pass even if
     * the restore never happened.
     *
     * @param aFragmenter fragmenter whose first boolean setting is flipped
     * @return the flipped property and its value before the flip
     * @throws IllegalStateException if the fragmenter exposes no boolean setting
     */
    @SuppressWarnings("unchecked")
    private static FlippedSetting flipFirstBooleanSetting(IMoleculeFragmenter aFragmenter) {
        for (Property<?> tmpProperty : aFragmenter.settingsProperties()) {
            if (tmpProperty.getValue() instanceof Boolean tmpOriginalValue) {
                Property<Boolean> tmpBooleanProperty = (Property<Boolean>) tmpProperty;
                tmpBooleanProperty.setValue(!tmpOriginalValue);
                return new FlippedSetting(tmpBooleanProperty, tmpOriginalValue);
            }
        }
        throw new IllegalStateException("Fragmenter " + aFragmenter.getFragmentationAlgorithmDisplayName()
                + " exposes no boolean setting to flip.");
    }
    //
    /**
     * A boolean fragmenter setting that a test flipped, paired with the value it held before the flip.
     *
     * @param property the flipped setting
     * @param originalValue the value the setting held before the flip
     */
    private record FlippedSetting(Property<Boolean> property, boolean originalValue) {
    }
    //
    /**
     * Closes the controller's stage on the FX thread if it is still showing, so no window leaks into a sibling test.
     *
     * @param aController the controller whose stage should be closed
     * @throws Exception if the stage cannot be reached or closed on the FX thread
     */
    private void closeQuietly(FragmentationSettingsViewController aController) throws Exception {
        if (aController == null) {
            return;
        }
        Stage tmpStage = this.getStage(aController);
        if (tmpStage != null) {
            AbstractFxTestCase.runAndWait(tmpStage::close);
        }
    }
    //</editor-fold>
}
