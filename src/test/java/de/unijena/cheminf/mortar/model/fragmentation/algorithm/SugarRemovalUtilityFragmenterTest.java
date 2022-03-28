/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2022  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
 *
 * Source code is available at <https://github.com/FelixBaensch/MORTAR>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.cheminf.mortar.model.fragmentation.algorithm;

import javafx.beans.property.Property;
import org.junit.Assert;
import org.junit.Test;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;

import java.util.List;
import java.util.Locale;

/**
 * Class to test the correct working of
 * {@link de.unijena.cheminf.mortar.model.fragmentation.algorithm.SugarRemovalUtilityFragmenter}.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class SugarRemovalUtilityFragmenterTest {

    /**
     * Constructor that sets the default locale to british english, which is important for the correct functioning of the
     * fragmenter because the settings tooltips are imported from the message.properties file.
     */
    public SugarRemovalUtilityFragmenterTest() {
        Locale.setDefault(new Locale("en", "GB"));
    }
    //
    /**
     * Tests instantiation and basic settings retrieval.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void basicTest() throws Exception {
        SugarRemovalUtilityFragmenter tmpFragmenter = new SugarRemovalUtilityFragmenter();
        System.out.println(tmpFragmenter.getFragmentationAlgorithmName());
        System.out.println(tmpFragmenter.getSugarTypeToRemoveSetting());
        for (Property tmpSetting : tmpFragmenter.settingsProperties()) {
            System.out.println(tmpSetting.getName());
        }
    }
    //
    /**
     * Does a test fragmentation on the COCONUT natural product CNP0151033 and prints the results.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void fragmentationTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        SmilesGenerator tmpSmiGen = new SmilesGenerator((SmiFlavor.Canonical));
        IAtomContainer tmpOriginalMolecule;
        List<IAtomContainer> tmpFragmentList;
        String tmpSmilesCode;
        SugarRemovalUtilityFragmenter tmpSRUFragmenter = new SugarRemovalUtilityFragmenter();
        tmpSRUFragmenter.setReturnedFragmentsSetting(SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption.ALL_FRAGMENTS);
        tmpOriginalMolecule = tmpSmiPar.parseSmiles(
                //CNP0151033
                "O=C(OC1C(OCC2=COC(OC(=O)CC(C)C)C3C2CC(O)C3(O)COC(=O)C)OC(CO)C(O)C1O)C=CC4=CC=C(O)C=C4");
        Assert.assertFalse(tmpSRUFragmenter.shouldBeFiltered(tmpOriginalMolecule));
        Assert.assertFalse(tmpSRUFragmenter.shouldBePreprocessed(tmpOriginalMolecule));
        Assert.assertTrue(tmpSRUFragmenter.canBeFragmented(tmpOriginalMolecule));
        tmpFragmentList = tmpSRUFragmenter.fragmentMolecule(tmpOriginalMolecule);
        tmpSmilesCode = tmpSmiGen.create(tmpFragmentList.get(0));
        System.out.println(tmpSmilesCode + " " + tmpFragmentList.get(0).getProperty(
                IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY));
        //The sugar ring is not terminal and should not be removed, so the molecule remains unchanged
        Assert.assertEquals("O=C(OC1C(OCC2=COC(OC(=O)CC(C)C)C3C2CC(O)C3(O)COC(=O)C)OC(CO)C(O)C1O)C=CC4=CC=C(O)C=C4", tmpSmilesCode);
        tmpSRUFragmenter.setRemoveOnlyTerminalSugarsSetting(false);
        tmpFragmentList = tmpSRUFragmenter.fragmentMolecule(tmpOriginalMolecule);
        tmpSmilesCode = tmpSmiGen.create(tmpFragmentList.get(0));
        System.out.println(tmpSmilesCode + " " + tmpFragmentList.get(0).getProperty(
                IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY));
        //Now that all sugars are removed, the sugar ring is removed and an unconnected structure remains
        // the unconnected fragments are separated into different atom containers in the returned list
        Assert.assertEquals("O=C(OCC1(O)C(O)CC2C(=COC(OC(=O)CC(C)C)C21)CO)C", tmpSmilesCode);
        Assert.assertEquals("O=C(O)C=CC1=CC=C(O)C=C1", tmpSmiGen.create(tmpFragmentList.get(1)));
        System.out.println(tmpSmiGen.create(tmpFragmentList.get(2)) + " " + tmpFragmentList.get(2).getProperty(
                IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY));
        tmpSRUFragmenter.setRemoveOnlyTerminalSugarsSetting(true);
        Assert.assertFalse(tmpSRUFragmenter.shouldBeFiltered(tmpFragmentList.get(0)));
        Assert.assertFalse(tmpSRUFragmenter.shouldBePreprocessed(tmpFragmentList.get(0)));
        Assert.assertTrue(tmpSRUFragmenter.canBeFragmented(tmpFragmentList.get(0)));
        IAtomContainer tmpAfterPreprocessing = tmpSRUFragmenter.applyPreprocessing(tmpFragmentList.get(0));
        Assert.assertTrue(tmpSRUFragmenter.canBeFragmented(tmpAfterPreprocessing));
    }
}
