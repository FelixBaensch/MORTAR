/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2024  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.Transform;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.smirks.Smirks;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

class RECAPTest extends RECAP {
    @Test
    void test1() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer mol = smiPar.parseSmiles("C1CC1Oc1ccccc1-c1ncc(OC)cc1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);
        cycles.find(mol);
        arom.apply(mol);
        RECAP recap = new RECAP();
        HierarchyNode node = recap.buildHierarchy(mol, 1);
        SmilesGenerator smiGen = new SmilesGenerator(SmiFlavor.Unique | SmiFlavor.UseAromaticSymbols);
        int i = 0;
        System.out.println(i + ": " + smiGen.create(node.getStructure()));
        for (HierarchyNode childFirstLevel : node.getChildren()) {
            i++;
            System.out.println(i + ":\t " + smiGen.create(childFirstLevel.getStructure()));
            for (HierarchyNode childSecondLevel : childFirstLevel.getChildren()) {
                i++;
                System.out.println(i + ":\t\t " + smiGen.create(childSecondLevel.getStructure()));
                for (HierarchyNode childThirdLevel : childSecondLevel.getChildren()) {
                    i++;
                    System.out.println(i + ":\t\t\t " + smiGen.create(childThirdLevel.getStructure()));
                }
            }
        }
        Assertions.assertEquals(6, node.getChildren().size());
        Assertions.assertEquals(26, node.getAllDescendants().size());
        Assertions.assertEquals(18, node.getOnlyTerminalDescendants().size());
        Assertions.assertTrue(node.getParents().isEmpty());

        node = recap.buildHierarchy(mol, 3); //TODO this should be 2
        Assertions.assertEquals(4, node.getChildren().size());
        Assertions.assertEquals(8, node.getAllDescendants().size());
        Assertions.assertEquals(6, node.getOnlyTerminalDescendants().size());
        Assertions.assertTrue(node.getParents().isEmpty());

        node = recap.buildHierarchy(mol, 5); //TODO this should be 4
        Assertions.assertEquals(2, node.getChildren().size());
        Assertions.assertEquals(2, node.getAllDescendants().size());
        Assertions.assertEquals(2, node.getOnlyTerminalDescendants().size());
        Assertions.assertTrue(node.getParents().isEmpty());
    }

    @Test
    void testEgon() throws Exception {
        SmilesParser parser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        SmilesGenerator generator = new SmilesGenerator(SmiFlavor.Unique);
        Transform neutralAcid = Smirks.compile("[O-:1]>>[O;+0:1][H]");
        IAtomContainer cdkStruct = parser.parseSmiles("CC(=O)[O-]");
        Iterable<IAtomContainer> iterable = neutralAcid.apply(cdkStruct, Transform.Mode.Exclusive);
        for (IAtomContainer neutral : iterable) {
            String neutralSmiles = generator.createSMILES(neutral);
            System.out.println(neutralSmiles);
        }

    }
}
