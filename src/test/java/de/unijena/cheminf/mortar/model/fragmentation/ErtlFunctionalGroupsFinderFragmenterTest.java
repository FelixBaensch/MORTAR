/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2020  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.model.fragmentation;

import org.junit.Assert;
import org.junit.Test;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.ErtlFunctionalGroupsFinder;

import java.util.List;

public class ErtlFunctionalGroupsFinderFragmenterTest {
    @Test
    public void basicTest() throws Exception {
        ErtlFunctionalGroupsFinderFragmenter tmpFragmenter = new ErtlFunctionalGroupsFinderFragmenter();
        System.out.println(tmpFragmenter.getFragmentationAlgorithmName());
        System.out.println(tmpFragmenter.getAromaticityModel());
        System.out.println(tmpFragmenter.getMode());
    }

    @Test
    public void fragmentationTest() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        SmilesGenerator tmpSmiGen = new SmilesGenerator((SmiFlavor.Canonical));
        IAtomContainer tmpOriginalMolecule;
        List<IAtomContainer> tmpFragmentList;
        ErtlFunctionalGroupsFinderFragmenter tmpFragmenter = new ErtlFunctionalGroupsFinderFragmenter(ErtlFunctionalGroupsFinder.Mode.NO_GENERALIZATION);
        tmpOriginalMolecule = tmpSmiPar.parseSmiles(
                //CNP0151033
                "O=C(OC1C(OCC2=COC(OC(=O)CC(C)C)C3C2CC(O)C3(O)COC(=O)C)OC(CO)C(O)C1O)C=CC4=CC=C(O)C=C4");
        Assert.assertFalse(tmpFragmenter.shouldBeFiltered(tmpOriginalMolecule));
        Assert.assertFalse(tmpFragmenter.shouldBePreprocessed(tmpOriginalMolecule));
        Assert.assertTrue(tmpFragmenter.canBeFragmented(tmpOriginalMolecule));
        tmpFragmentList = tmpFragmenter.fragmentMolecule(tmpOriginalMolecule);
        Assert.assertTrue(tmpFragmenter.hasFragments(tmpFragmentList));
        for (IAtomContainer tmpFragment : tmpFragmentList) {
            System.out.println(tmpSmiGen.create(tmpFragment) + " " + tmpFragment.getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY));
        }
    }
}
