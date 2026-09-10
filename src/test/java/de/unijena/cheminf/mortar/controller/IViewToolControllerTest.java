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
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contract (lock) tests for {@link IViewToolController} (COV-10).
 * <p>
 * <strong>Coverage note:</strong> {@link IViewToolController} is a bodyless interface — it declares four methods
 * ({@code settingsProperties()}, {@code getViewToolNameForDisplay()}, {@code restoreDefaultSettings()},
 * {@code canBeUsedOnTab(TabNames)}) and contains NO executable statements, so it has 0/0 coverable lines in the JaCoCo
 * report (as recorded in the Phase 12 controller-coverage baseline). Its {@literal >=}80% line-coverage requirement is
 * therefore <em>vacuously satisfied</em>: there are no lines to miss. Rather than contort a test to "cover" an
 * interface with no body, this class exercises the interface transitively through its two concrete implementors and
 * pins the shared contract, guarding the seam against silent drift.
 * <p>
 * Both {@link OverviewViewController} and {@link HistogramViewController} implement {@link IViewToolController} and
 * (per the Phase 14 finding) construct toolkit-free from an {@code IConfiguration} alone, so no scene graph is built
 * and no JavaFX toolkit boot is required for these assertions; the class still extends {@link AbstractFxTestCase} for
 * en-GB locale determinism and {@code user.home} isolation, consistent with the other Phase 15 controller tests.
 * Assertions are behavioral only.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class IViewToolControllerTest extends AbstractFxTestCase {
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Default no-argument constructor; all headless setup is inherited from {@link AbstractFxTestCase}.
     */
    public IViewToolControllerTest() {
    }
    //</editor-fold>
    //
    //<editor-fold desc="Test methods" defaultstate="collapsed">
    /**
     * Asserts that both concrete implementors honour the {@link IViewToolController} contract: a non-null settings list,
     * a non-blank display name, and a {@code restoreDefaultSettings()} that runs without throwing. Viewing them through
     * the interface reference exercises {@link IViewToolController} transitively.
     *
     * @throws Exception if a controller cannot be constructed
     */
    @Test
    public void bothImplementorsHonourSharedContractTest() throws Exception {
        IViewToolController tmpOverview = new OverviewViewController(Configuration.getInstance(), new SettingsContainer());
        IViewToolController tmpHistogram = new HistogramViewController(Configuration.getInstance());
        Assertions.assertNotNull(tmpOverview.settingsProperties());
        Assertions.assertNotNull(tmpHistogram.settingsProperties());
        Assertions.assertFalse(tmpOverview.getViewToolNameForDisplay().isBlank());
        Assertions.assertFalse(tmpHistogram.getViewToolNameForDisplay().isBlank());
        Assertions.assertDoesNotThrow(tmpOverview::restoreDefaultSettings);
        Assertions.assertDoesNotThrow(tmpHistogram::restoreDefaultSettings);
    }
    //
    /**
     * Pins the {@code canBeUsedOnTab(TabNames)} contract per implementor: the overview view is usable on the molecules
     * and fragments tabs but not on the itemization tab, whereas the histogram view is usable on the fragments and
     * itemization tabs but not on the molecules tab.
     *
     * @throws Exception if a controller cannot be constructed
     */
    @Test
    public void canBeUsedOnTabDiffersPerImplementorTest() throws Exception {
        IViewToolController tmpOverview = new OverviewViewController(Configuration.getInstance(), new SettingsContainer());
        IViewToolController tmpHistogram = new HistogramViewController(Configuration.getInstance());
        Assertions.assertTrue(tmpOverview.canBeUsedOnTab(TabNames.MOLECULES));
        Assertions.assertTrue(tmpOverview.canBeUsedOnTab(TabNames.FRAGMENTS));
        Assertions.assertFalse(tmpOverview.canBeUsedOnTab(TabNames.ITEMIZATION));
        Assertions.assertFalse(tmpHistogram.canBeUsedOnTab(TabNames.MOLECULES));
        Assertions.assertTrue(tmpHistogram.canBeUsedOnTab(TabNames.FRAGMENTS));
        Assertions.assertTrue(tmpHistogram.canBeUsedOnTab(TabNames.ITEMIZATION));
    }
    //</editor-fold>
}
