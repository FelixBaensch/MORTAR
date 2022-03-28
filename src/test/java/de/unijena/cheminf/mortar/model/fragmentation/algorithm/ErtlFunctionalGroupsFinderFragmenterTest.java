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
        ErtlFunctionalGroupsFinderFragmenter tmpFragmenter = new ErtlFunctionalGroupsFinderFragmenter();
        System.out.println(tmpFragmenter.getFragmentationAlgorithmName());
        System.out.println(tmpFragmenter.getElectronDonationModelSetting());
        System.out.println(tmpFragmenter.getEnvironmentModeSetting());
        for (Property tmpSetting : tmpFragmenter.settingsProperties()) {
            System.out.println(tmpSetting.getName());
        }
        tmpFragmenter.settingsProperties().get(3).setValue(ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.FULL_ENVIRONMENT.name());
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
        ErtlFunctionalGroupsFinderFragmenter tmpFragmenter = new ErtlFunctionalGroupsFinderFragmenter();
        tmpFragmenter.setEnvironmentModeSetting(ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.NO_ENVIRONMENT);
        tmpFragmenter.setEnvironmentModeSetting(ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.GENERALIZATION);
        tmpFragmenter.environmentModeSettingProperty().set(ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.FULL_ENVIRONMENT.name());
        tmpFragmenter.setFragmentSaturationSetting(IMoleculeFragmenter.FragmentSaturationOption.HYDROGEN_SATURATION);
        tmpFragmenter.setElectronDonationModelSetting(ErtlFunctionalGroupsFinderFragmenter.ElectronDonationModelOption.CDK);
        tmpFragmenter.setReturnedFragmentsSetting(ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption.ALL_FRAGMENTS);
        tmpOriginalMolecule = tmpSmiPar.parseSmiles(
                //CNP0151033
                "O=C(OC1C(OCC2=COC(OC(=O)CC(C)C)C3C2CC(O)C3(O)COC(=O)C)OC(CO)C(O)C1O)C=CC4=CC=C(O)C=C4");
        Assert.assertFalse(tmpFragmenter.shouldBeFiltered(tmpOriginalMolecule));
        Assert.assertFalse(tmpFragmenter.shouldBePreprocessed(tmpOriginalMolecule));
        Assert.assertTrue(tmpFragmenter.canBeFragmented(tmpOriginalMolecule));
        tmpFragmentList = tmpFragmenter.fragmentMolecule(tmpOriginalMolecule);
        for (IAtomContainer tmpFragment : tmpFragmentList) {
            System.out.println(tmpSmiGen.create(tmpFragment) + " " + tmpFragment.getProperty(
                    IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY));
        }
    }
}
