/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2025  Felix Baensch, Jonas Schaub (felix.j.baensch@gmail.com, jonas.schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.model.fragmentation.algorithm;

import javafx.beans.property.Property;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.fragment.FunctionalGroupsFinder;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Class to test the correct working of
 * {@link de.unijena.cheminf.mortar.model.fragmentation.algorithm.ErtlFunctionalGroupsFinderFragmenter}.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class ErtlFunctionalGroupsFinderFragmenterTest {

    /**
     * Constructor that sets the default locale to british english, which is important for the correct functioning of the
     * fragmenter because the settings tooltips are imported from the message.properties file.
     */
    public ErtlFunctionalGroupsFinderFragmenterTest() {
        Locale.setDefault(Locale.of("en", "GB"));
    }
    //
    /**
     * Tests instantiation and basic settings retrieval.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void basicTest() throws Exception {
        ErtlFunctionalGroupsFinderFragmenter tmpFragmenter = new ErtlFunctionalGroupsFinderFragmenter();
        Assertions.assertDoesNotThrow(tmpFragmenter::getFragmentationAlgorithmName);
        Assertions.assertDoesNotThrow(tmpFragmenter::getFragmentationAlgorithmDisplayName);
        Assertions.assertDoesNotThrow(tmpFragmenter::getElectronDonationModelSetting);
        Assertions.assertDoesNotThrow(tmpFragmenter::getEnvironmentModeSetting);
        for (Property<?> tmpSetting : tmpFragmenter.settingsProperties()) {
            Assertions.assertDoesNotThrow(tmpSetting::getName);
        }
    }
    //
    /**
     * Does a test fragmentation on the COCONUT natural product CNP0151033.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void fragmentationTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        SmilesGenerator tmpSmiGen = new SmilesGenerator(SmiFlavor.Canonical);
        IAtomContainer tmpOriginalMolecule;
        List<IAtomContainer> tmpFragmentList;
        ErtlFunctionalGroupsFinderFragmenter tmpFragmenter = new ErtlFunctionalGroupsFinderFragmenter();
        tmpFragmenter.setEnvironmentModeSetting(ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.NO_ENVIRONMENT);
        tmpFragmenter.setEnvironmentModeSetting(ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.GENERALIZATION);
        tmpFragmenter.environmentModeSettingProperty().set(ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.FULL_ENVIRONMENT);
        tmpFragmenter.setFragmentSaturationSetting(IMoleculeFragmenter.FragmentSaturationOption.HYDROGEN_SATURATION);
        tmpFragmenter.setElectronDonationModelSetting(ErtlFunctionalGroupsFinderFragmenter.ElectronDonationModelOption.CDK);
        tmpFragmenter.setReturnedFragmentsSetting(ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption.ALL_FRAGMENTS);
        tmpOriginalMolecule = tmpSmiPar.parseSmiles(
                //CNP0151033
                "O=C(OC1C(OCC2=COC(OC(=O)CC(C)C)C3C2CC(O)C3(O)COC(=O)C)OC(CO)C(O)C1O)C=CC4=CC=C(O)C=C4");
        Assertions.assertFalse(tmpFragmenter.shouldBeFiltered(tmpOriginalMolecule));
        Assertions.assertFalse(tmpFragmenter.shouldBePreprocessed(tmpOriginalMolecule));
        Assertions.assertTrue(tmpFragmenter.canBeFragmented(tmpOriginalMolecule));
        tmpFragmentList = tmpFragmenter.fragmentMolecule(tmpOriginalMolecule);
        for (IAtomContainer tmpFragment : tmpFragmentList) {
            Assertions.assertDoesNotThrow(() -> tmpSmiGen.create(tmpFragment));
            Assertions.assertNotNull(tmpFragment.getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY));
        }
    }
    //
    /**
     *
     */
    @Test
    public void testAlkylExtraction() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer mol = tmpSmiPar.parseSmiles(
                //CNP0151033
                "O=C(OC1C(OCC2=COC(OC(=O)CC(C)C)C3C2CC(O)C3(O)COC(=O)C)OC(CO)C(O)C1O)C=CC4=CC=C(O)C=C4");
        FunctionalGroupsFinder fgf = FunctionalGroupsFinder.withGeneralEnvironment();
        int[] groups = new int[mol.getAtomCount()];
        fgf.find(groups, mol);
        for (IAtom atom : mol.atoms())
            atom.setMapIdx(groups[atom.getIndex()]+1);
        String smi = new SmilesGenerator(SmiFlavor.AtomAtomMap).create(mol);
        System.out.println(smi);
        HashMap<Integer, IAtom>  indexToAtomMap = new HashMap<>();
        for (IAtom atom : mol.atoms()) {
            indexToAtomMap.put(atom.getIndex(), atom);
        }
        for (Integer key : indexToAtomMap.keySet()) {
            if (groups[key] != -1) {
                mol.removeAtom(indexToAtomMap.get(key));
            }
        }
        SmilesGenerator tmpSmiGen = new SmilesGenerator(SmiFlavor.Canonical);
        System.out.println(tmpSmiGen.create(mol));
    }
    //
    /**
     *
     */
    @Test
    public void fragmentChargesTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer mol = tmpSmiPar.parseSmiles("[N+](C)(C)(C)CC(O)O");
        FunctionalGroupsFinder fgf = FunctionalGroupsFinder.withNoEnvironment();
        List<IAtomContainer> fragments = fgf.extract(mol);
        SmilesGenerator tmpSmiGen = new SmilesGenerator(SmiFlavor.Canonical);
        for (IAtomContainer fragment : fragments) {
            System.out.println(tmpSmiGen.create(fragment));
        }
    }
}
