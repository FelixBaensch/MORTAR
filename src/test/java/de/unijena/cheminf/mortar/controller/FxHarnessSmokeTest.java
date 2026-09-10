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

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;

/**
 * Smoke test that proves the headless JavaFX harness works: the toolkit boots once (inherited from
 * {@link AbstractFxTestCase}), an offscreen {@link Stage} plus a trivial control construct on the FX thread inside a
 * bounded {@code runAndWait} without throwing, and {@link FxTestUtil#mockGuiAlerts()} neutralizes a {@link GuiUtil}
 * alert call so no real {@code Alert} is created and the call returns without hanging. This is the first test in the
 * suite to actually construct live JavaFX objects headless; Phase 14+ controller tests extend the same base.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class FxHarnessSmokeTest extends AbstractFxTestCase {
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Default no-argument constructor. All setup (toolkit boot, locale guard, isolation) is inherited from
     * {@link AbstractFxTestCase}.
     */
    public FxHarnessSmokeTest() {
    }
    //</editor-fold>
    //
    //<editor-fold desc="Test methods" defaultstate="collapsed">
    /**
     * Constructs an offscreen {@link Stage} plus a trivial {@link Button} on the JavaFX Application Thread inside the
     * bounded {@code runAndWait} and asserts the construction does not throw within the timeout, proving the toolkit is
     * up and live FX objects can be built headless.
     *
     * @throws Exception if the FX runnable does not complete within the bounded timeout
     */
    @Test
    public void offscreenStageConstructsOnFxThreadTest() throws Exception {
        Assertions.assertDoesNotThrow(() -> AbstractFxTestCase.runAndWait(() -> {
            Stage tmpStage = FxTestUtil.newOffscreenStage();
            Button tmpButton = new Button("smoke");
            Assertions.assertNotNull(tmpStage);
            Assertions.assertNotNull(tmpStage.getScene());
            Assertions.assertEquals("smoke", tmpButton.getText());
        }));
    }
    //
    /**
     * Opens {@link FxTestUtil#mockGuiAlerts()} in a try-with-resources block and asserts that a {@link GuiUtil} alert
     * call returns the neutralized result ({@link Optional#empty()}) without constructing a real {@code Alert} and
     * without hanging, proving the shared alert-neutralization entry point works headless.
     */
    @Test
    public void guiAlertsAreNeutralizedTest() {
        Assertions.assertDoesNotThrow(() -> {
            try (MockedStatic<GuiUtil> tmpGuiUtilMock = FxTestUtil.mockGuiAlerts()) {
                Optional<ButtonType> tmpResult = GuiUtil.guiMessageAlert(Alert.AlertType.INFORMATION, "t", "h", "c");
                Assertions.assertTrue(tmpResult.isEmpty());
                Assertions.assertNotNull(tmpGuiUtilMock);
            }
        });
    }
    //</editor-fold>
}
