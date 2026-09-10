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

package de.unijena.cheminf.mortar.model.fragmentation;

import de.unijena.cheminf.mortar.configuration.Configuration;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.ErtlFunctionalGroupsFinderFragmenter;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import de.unijena.cheminf.mortar.model.util.ChemUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

/**
 * Direct, headless unit tests for the deprecated {@link FragmentationThread}. This thread is {@code @Deprecated} and is
 * never instantiated by {@link FragmentationService}; therefore it needs its own driver to be exercised. Its
 * {@code call()} method blocks (it builds an {@code ExecutorService}, runs {@code invokeAll} and {@code Future.get}, then
 * computes the percentages) and returns the fully populated {@link Hashtable} of fragments, so all assertions are made
 * directly after the call with no sleep, latch, or {@code Platform.runLater} wait. Only real CDK objects, a real
 * {@link SettingsContainer}, and a real {@link ErtlFunctionalGroupsFinderFragmenter} are used (no mocking). Constructing
 * a {@code SettingsContainer} only reads the {@code user.home} system property for its recent-directory default and does
 * not write the real {@code ~/MORTAR} settings directory (that only happens on explicit persist/reload), so no temp-dir
 * isolation is required here.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class FragmentationThreadTest {
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Constructor that sets the default locale to en-GB (so the fragmenter settings tooltips and display names, which
     * are resolved from the message.properties file during fragmenter instantiation, are deterministic) and bootstraps
     * the Configuration singleton from the classpath (no data directory is touched by this).
     *
     * @throws Exception if the Configuration singleton cannot be initialized
     */
    public FragmentationThreadTest() throws Exception {
        Locale.setDefault(Locale.of("en", "GB"));
        Configuration.getInstance();
    }
    //</editor-fold>
    //
    //<editor-fold desc="Test methods" defaultstate="collapsed">
    /**
     * Drives {@link FragmentationThread#call()} directly with a single task over a small molecule list and a real
     * {@link SettingsContainer} and {@link ErtlFunctionalGroupsFinderFragmenter}. Asserts the returned hashtable is
     * non-null and non-empty and that every fragment has an absolute frequency {@literal >=} 1 and an absolute
     * percentage in (0.0, 1.0]. The call blocks and returns with the table fully populated, so the assertions follow
     * directly with no sleep/latch.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void callReturnsPopulatedTableTest() throws Exception {
        List<MoleculeDataModel> tmpMols = new ArrayList<>(2);
        tmpMols.add(FragmentationThreadTest.buildMDM("c1ccccc1"));
        tmpMols.add(FragmentationThreadTest.buildMDM("O=C(O)CCCC(=O)O"));
        SettingsContainer tmpSettings = new SettingsContainer();
        FragmentationThread tmpThread = new FragmentationThread(
                tmpMols, 1, "ThreadTest", new ErtlFunctionalGroupsFinderFragmenter(), tmpSettings);
        Hashtable<String, FragmentDataModel> tmpFragments = tmpThread.call();
        Assertions.assertNotNull(tmpFragments);
        Assertions.assertFalse(tmpFragments.isEmpty());
        for (FragmentDataModel tmpFragment : tmpFragments.values()) {
            Assertions.assertTrue(tmpFragment.getAbsoluteFrequency() >= 1);
            Assertions.assertTrue(tmpFragment.getAbsolutePercentage() > 0.0 && tmpFragment.getAbsolutePercentage() <= 1.0);
        }
    }
    //
    /**
     * Drives {@link FragmentationThread#call()} with {@code aNumberOfTasks = 2} over a {@literal >=}2-molecule list to
     * exercise the multi-task split branch. Asserts the returned hashtable is populated and the call completes without
     * throwing.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void callWithMultipleTasksTest() throws Exception {
        List<MoleculeDataModel> tmpMols = new ArrayList<>(3);
        tmpMols.add(FragmentationThreadTest.buildMDM("O=C(O)CC"));
        tmpMols.add(FragmentationThreadTest.buildMDM("O=C(O)CCC"));
        tmpMols.add(FragmentationThreadTest.buildMDM("O=C(O)CCCC"));
        SettingsContainer tmpSettings = new SettingsContainer();
        FragmentationThread tmpThread = new FragmentationThread(
                tmpMols, 2, "ThreadTestMultiTask", new ErtlFunctionalGroupsFinderFragmenter(), tmpSettings);
        Hashtable<String, FragmentDataModel> tmpFragments = tmpThread.call();
        Assertions.assertNotNull(tmpFragments);
        Assertions.assertFalse(tmpFragments.isEmpty());
        for (FragmentDataModel tmpFragment : tmpFragments.values()) {
            Assertions.assertTrue(tmpFragment.getAbsoluteFrequency() >= 1);
            Assertions.assertTrue(tmpFragment.getAbsolutePercentage() > 0.0 && tmpFragment.getAbsolutePercentage() <= 1.0);
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private static methods" defaultstate="collapsed">
    /**
     * Builds a {@link MoleculeDataModel} from the given SMILES string using {@link ChemUtil#parseSmilesToAtomContainer}
     * and the (IAtomContainer, boolean) constructor without stereochemistry encoding.
     *
     * @param aSmiles SMILES string to parse
     * @return a MoleculeDataModel wrapping the parsed atom container
     * @throws Exception if parsing or unique-SMILES creation fails
     */
    private static MoleculeDataModel buildMDM(String aSmiles) throws Exception {
        IAtomContainer tmpAtomContainer = ChemUtil.parseSmilesToAtomContainer(aSmiles, false, false);
        return new MoleculeDataModel(tmpAtomContainer, false);
    }
    //</editor-fold>
}
