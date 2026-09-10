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
import de.unijena.cheminf.mortar.model.util.FileUtil;

import javafx.stage.Stage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Headless unit and characterization tests for {@link MainViewController} (COV-01). Unlike the Phase 15 settings
 * controllers, this root controller's constructor ends in a NON-blocking {@code primaryStage.show()} (not
 * {@code showAndWait}), so the controller is constructed with a plain {@link AbstractFxTestCase#runAndWait(Runnable)}
 * over the shared {@link FxTestUtil#newMainViewController(Stage, String)} seam (reused by the sibling
 * {@code MainViewController} test classes of plans 16-02 and 16-03) rather than
 * {@link FxTestUtil#runAndDriveModal(java.util.concurrent.Callable, java.util.function.Consumer)}. The passed real
 * {@link Stage} is always hidden in a {@code finally} block; because {@code Stage.hide()} does not fire the window
 * close-request handler, the controller's {@code closeApplication}/{@code System.exit} path is never reached and the
 * test JVM fork survives.
 * <p>
 * This class covers the close-persist tail {@code persistSettingsAndStopTasks} that Phase 16 Plan 1 lifted into a
 * package-private method on the controller. The export seams lifted by the same plan — the precondition guard
 * {@code areExportPreconditionsMet} and the export dispatch {@code buildExportResult} — are covered in full by
 * {@code MainViewControllerExportTest}, which drives every precondition branch and every resolvable export type, so
 * no narrower pin of the same behavior is kept here. Assertions are behavioral invariants, never exact CDK-derived
 * strings, because the CDK snapshot is a moving target.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class MainViewControllerTest extends AbstractFxTestCase {
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Default no-argument constructor; all headless setup (toolkit boot, en-GB locale, {@code user.home} isolation) is
     * inherited from {@link AbstractFxTestCase}.
     */
    public MainViewControllerTest() {
    }
    //</editor-fold>
    //
    //<editor-fold desc="Test methods" defaultstate="collapsed">
    /**
     * Unit test for the extracted close-persist tail (seam E4): calls {@code persistSettingsAndStopTasks()} directly
     * (never {@code closeApplication}, which would reach {@code System.exit} and kill the fork) and asserts it completes
     * without throwing and produces the observable settings-directory side effect under the isolated temporary
     * {@code user.home}. The whole test class still runs to completion, proving no {@code System.exit} was reached.
     *
     * @throws Exception if anything goes wrong on the FX thread
     */
    @Test
    public void persistSettingsAndStopTasksPersistsWithoutReachingSystemExitTest() throws Exception {
        AtomicReference<Stage> tmpStageReference = new AtomicReference<>();
        try {
            MainViewController tmpController = MainViewControllerTestSupport.constructController(tmpStageReference);
            AbstractFxTestCase.runAndWait(() -> {
                try (MockedStatic<GuiUtil> tmpGuiUtilMock = FxTestUtil.mockGuiAlerts()) {
                    tmpController.persistSettingsAndStopTasks();
                }
            });
            //observable side effect: preserveSettings created the settings directory under the isolated user.home
            Assertions.assertTrue(new File(FileUtil.getSettingsDirPath()).isDirectory());
        } finally {
            MainViewControllerTestSupport.hideStage(tmpStageReference);
        }
    }
    //</editor-fold>
}
