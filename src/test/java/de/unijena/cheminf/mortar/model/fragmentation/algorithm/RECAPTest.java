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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class RECAPTest extends RECAP {
    /**
     *
     */
    @Test
    void testAmideRuleIndividually() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);

        IAtomContainer mol = smiPar.parseSmiles("*C(=O)N(*)*");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match amide connected to pseudo atoms, as to not match results of other cleavages
        Assertions.assertFalse(RECAP.AMIDE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCC(=O)N(*)*");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match N connected to a pseudo atom, as to not match results of other cleavages
        Assertions.assertFalse(RECAP.AMIDE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCC(=O)N(*)CCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match N connect to a pseudo atom, as to not match results of other cleavages
        Assertions.assertFalse(RECAP.AMIDE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("*C(=O)N(CCCCCCCCCCCC)CCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match C connect to a pseudo atom, as to not match results of other cleavages
        Assertions.assertFalse(RECAP.AMIDE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCC(=O)N(CCCCCCCCC)CCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match this simple example
        Assertions.assertTrue(RECAP.AMIDE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCC(=O)NCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match N of D2
        Assertions.assertTrue(RECAP.AMIDE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C(=O)N(CCCCCCCCC)CCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match because C is terminal
        Assertions.assertFalse(RECAP.AMIDE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCC(=O)N");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match because N is terminal
        Assertions.assertFalse(RECAP.AMIDE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1(=O)NCCCCCC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match because of the ring, this is a lactam!
        Assertions.assertFalse(RECAP.AMIDE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCC(=O)N(CCC2)CCCCC2");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do match even though N is in a ring
        Assertions.assertTrue(RECAP.AMIDE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCC(=O)n(cn1)cc1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do match even though n is in a ring and aromatic
        Assertions.assertTrue(RECAP.AMIDE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCN(CCCC)C(=O)N(CCCC)CCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match urea
        Assertions.assertFalse(RECAP.AMIDE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("ClC(=O)N(CCCC)CCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match C connected to another hetero atom
        Assertions.assertFalse(RECAP.AMIDE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCOC(=O)N(CCCC)CCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match carbamate ester
        Assertions.assertFalse(RECAP.AMIDE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCC(=O)NC(=O)CCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match imide
        Assertions.assertFalse(RECAP.AMIDE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("c1ccccc1C(=O)N(CCCCCCCCCC)CCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do match with an aromatic ring connected to C
        Assertions.assertTrue(RECAP.AMIDE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("O=CC(=O)NCCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match because of connected aldehyde functionality
        Assertions.assertFalse(RECAP.AMIDE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("OCC(=O)NCCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //the alcohol in the immediate environment is ok
        Assertions.assertTrue(RECAP.AMIDE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CC(=O)NC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do match this very basic example
        Assertions.assertTrue(RECAP.AMIDE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CC(=O)N(C)C");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do match this very basic example
        Assertions.assertTrue(RECAP.AMIDE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CC(=O)N(C)C=O");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match imide variant
        Assertions.assertFalse(RECAP.AMIDE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CC(=O)N(C)CCl");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //this chlorine is ok
        Assertions.assertTrue(RECAP.AMIDE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("*CC(=O)N(C*)C*");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //in these positions, pseudo atoms are allowed
        Assertions.assertTrue(RECAP.AMIDE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCC(=O)N=CCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match because of N being connected via a double bond to C not in FG
        Assertions.assertFalse(RECAP.AMIDE.getEductPattern().matches(mol));
    }
    /**
     *
     */
    @Test
    void testAmideRuleIntegration() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);
        RECAP recap = new RECAP();
        SmilesGenerator smiGen = new SmilesGenerator(SmiFlavor.Unique | SmiFlavor.UseAromaticSymbols);

        IAtomContainer mol = smiPar.parseSmiles("C1CC1C(=O)NC1OC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        HierarchyNode node = recap.buildHierarchy(mol);
        Assertions.assertEquals(2, node.getChildren().size());
        Set<String> terminalDescendantsSmilesSet = HashSet.newHashSet(node.getOnlyTerminalDescendants().size());
        for (HierarchyNode child : node.getOnlyTerminalDescendants()) {
            terminalDescendantsSmilesSet.add(smiGen.create(child.getStructure()));
        }
        //one match - the amide rule, two products
        Assertions.assertEquals(2, terminalDescendantsSmilesSet.size());
        Assertions.assertTrue(terminalDescendantsSmilesSet.containsAll(List.of("*OC(=O)C1CC1", "*NC1OC1")));

        // N is now of degree three because of additional methyl group
        mol = smiPar.parseSmiles("C1CC1C(=O)N(C)C1OC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        node = recap.buildHierarchy(mol);
        //one match - the amide rule, two products
        Assertions.assertEquals(2, node.getChildren().size());
        terminalDescendantsSmilesSet = HashSet.newHashSet(node.getOnlyTerminalDescendants().size());
        for (HierarchyNode child : node.getOnlyTerminalDescendants()) {
            terminalDescendantsSmilesSet.add(smiGen.create(child.getStructure()));
        }
        Assertions.assertEquals(2, terminalDescendantsSmilesSet.size());
        Assertions.assertTrue(terminalDescendantsSmilesSet.containsAll(List.of("*OC(=O)C1CC1", "*N(C)C1OC1")));

        mol = smiPar.parseSmiles("C1CC1C(=O)n1cccc1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        node = recap.buildHierarchy(mol);
        // one match - the amide rule, two products
        Assertions.assertEquals(2, node.getChildren().size());
        terminalDescendantsSmilesSet = HashSet.newHashSet(node.getOnlyTerminalDescendants().size());
        for (HierarchyNode child : node.getOnlyTerminalDescendants()) {
            terminalDescendantsSmilesSet.add(smiGen.create(child.getStructure()));
        }
        Assertions.assertEquals(2, terminalDescendantsSmilesSet.size());
        Assertions.assertTrue(terminalDescendantsSmilesSet.containsAll(List.of("*OC(=O)C1CC1", "*n1cccc1")));


        mol = smiPar.parseSmiles("CC(=O)N");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        node = recap.buildHierarchy(mol);
        //no match because the amide group is terminal (N must not be of order 1,
        // as defined in the SMIRKS string)
        Assertions.assertTrue(node.getChildren().isEmpty());

        mol = smiPar.parseSmiles("CC(=O)NC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        node = recap.buildHierarchy(mol, 2);
        Assertions.assertEquals(2, node.getChildren().size());
        terminalDescendantsSmilesSet = HashSet.newHashSet(node.getOnlyTerminalDescendants().size());
        for (HierarchyNode child : node.getOnlyTerminalDescendants()) {
            terminalDescendantsSmilesSet.add(smiGen.create(child.getStructure()));
        }
        //here, the N is not terminal and the fragmentation is "allowed" because the fragments are not just alkyl fragments
        Assertions.assertEquals(2, terminalDescendantsSmilesSet.size());
        Assertions.assertTrue(terminalDescendantsSmilesSet.containsAll(List.of("*OC(=O)C", "*NC")));

        //disagreement with RDKit: we do not cleave terminal amide
        mol = smiPar.parseSmiles("C(=O)NCCNC(=O)CC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        node = recap.buildHierarchy(mol);
        Assertions.assertEquals(2, node.getChildren().size());
        terminalDescendantsSmilesSet = HashSet.newHashSet(node.getOnlyTerminalDescendants().size());
        for (HierarchyNode child : node.getOnlyTerminalDescendants()) {
            terminalDescendantsSmilesSet.add(smiGen.create(child.getStructure()));
        }
        //molecule can be cleaved at one position
        Assertions.assertEquals(2, terminalDescendantsSmilesSet.size());
        Assertions.assertTrue(terminalDescendantsSmilesSet.containsAll(List.of("*NCCNC=O", "*OC(=O)CC")));
    }

    @Test
    void testEsterRuleIndividually() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);

        IAtomContainer mol = smiPar.parseSmiles("CCCCC(=O)O*");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match O connected to a pseudo atom, as to not match results of other cleavages
        Assertions.assertFalse(RECAP.ESTER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("*C(=O)OCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match C connect to a pseudo atom, as to not match results of other cleavages
        Assertions.assertFalse(RECAP.ESTER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("*C(=O)O*");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match pseudo atoms in all environmental positions, as to not match results of other cleavages
        Assertions.assertFalse(RECAP.ESTER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CC(=O)OC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match this simple example
        Assertions.assertTrue(RECAP.ESTER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCC(=O)OCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match this simple example
        Assertions.assertTrue(RECAP.ESTER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C(=O)OCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match because C is terminal
        Assertions.assertFalse(RECAP.ESTER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCC(=O)O");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match because O is terminal, carboxylic acid
        Assertions.assertFalse(RECAP.ESTER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1(=O)OCCCCCC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match because of the ring
        Assertions.assertFalse(RECAP.ESTER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCOC(=O)OCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match carbonate
        Assertions.assertFalse(RECAP.ESTER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCC(=O)OC(=O)CCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match organic acid anhydride
        Assertions.assertFalse(RECAP.ESTER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCNC(=O)OCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match carbamate ester
        Assertions.assertFalse(RECAP.ESTER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("c1ccccc1C(=O)OCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do match with an aromatic ring connected to C
        Assertions.assertTrue(RECAP.ESTER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("O=CC(=O)OCCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match because of connected aldehyde functionality
        Assertions.assertFalse(RECAP.ESTER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("OCC(=O)OCCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //the alcohol in the immediate environment is ok
        Assertions.assertTrue(RECAP.ESTER.getEductPattern().matches(mol));
    }

    @Test
    void testEsterRuleIntegration() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer mol = smiPar.parseSmiles("C1CC1C(=O)OC1OC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);
        cycles.find(mol);
        arom.apply(mol);
        RECAP recap = new RECAP();
        HierarchyNode node = recap.buildHierarchy(mol);
        SmilesGenerator smiGen = new SmilesGenerator(SmiFlavor.Unique | SmiFlavor.UseAromaticSymbols);
        Set<String> terminalDescendantsSmilesSet = HashSet.newHashSet(node.getOnlyTerminalDescendants().size());
        for (HierarchyNode child : node.getOnlyTerminalDescendants()) {
            terminalDescendantsSmilesSet.add(smiGen.create(child.getStructure()));
        }
        Assertions.assertEquals(1, node.getMaximumLevelOfAllDescendants());
        Assertions.assertEquals(2, node.getChildren().size());
        Assertions.assertEquals(2, terminalDescendantsSmilesSet.size());
        //in agreement with RDKit except for the carboxylic acid on one side, instead of an aldehyde
        Assertions.assertTrue(terminalDescendantsSmilesSet.containsAll(List.of("*OC(=O)C1CC1", "*OC1OC1")));

        mol = smiPar.parseSmiles("C1CCC(=O)OC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        node = recap.buildHierarchy(mol);
        //ester group is in ring, no match
        Assertions.assertTrue(node.isTerminal());
    }

    @Test
    void testSecondaryAmineRuleIndividually() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);

        IAtomContainer mol = smiPar.parseSmiles("CCCCCCCCCN");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match primary amine
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CNC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match secondary amine
        Assertions.assertTrue(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCNCCCCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match secondary amine
        Assertions.assertTrue(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCN(CCCCCCCCCCC)CCCCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match tertiary amine
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCN*");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match pseudo atom
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCN(*)CCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match pseudo atom
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("*N*");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match pseudo atom
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("*N(*)*");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match pseudo atom
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCC(=O)NCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match amide
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCNC(=O)NCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match urea
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1(=O)NCCCCCC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match lactam
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCC[N+](CCCCCCCCCC)(CCCCCCC)CCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match quaternary nitrogen
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCn(cn1)cc1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match aromatic n to aliphatic C
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCCNS(=O)(=O)CCCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match sulphonamide
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCCN=CNCCCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match amidine
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCCNC(=NCCCCCC)NCCCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match guanidine
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCC(=O)NC(=O)CCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match imide
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCOC(=O)NCCCCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match carbamate ester
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCC(=NNCCCCCCC)CCCCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match hydrazone
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCC(=NCCCCCCC)CCCCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match imine
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCC=NCCCCCCCCCCCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match imine
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCCN=NCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match azo compounds
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCCOC#N");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match cyanate
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCCN=C=O");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match iso cyanate
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCC#N");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match nitrile
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCCON=O");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match nitrite
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCCN(=O)=O");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match nitro/nitroso
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCCN=O");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match nitro/nitroso
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCC[N+](=O)[O-]");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match nitro/nitroso
        Assertions.assertFalse(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1CCCCCCCCCC1NC2CCCCCCCCCCCC2");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do match secondary amine connecting two rings
        Assertions.assertTrue(RECAP.SECONDARY_AMINE.getEductPattern().matches(mol));
    }

    @Test
    void testTertiaryAmineRuleIndividually() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);

        IAtomContainer mol = smiPar.parseSmiles("CCCCCCCCCN");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match primary amine
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCNCCCCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match secondary amine
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CN(C)C");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match tertiary amine
        Assertions.assertTrue(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCN(CCCCCCCCCCC)CCCCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match tertiary amine
        Assertions.assertTrue(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCN*");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match pseudo atom
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCN(*)CCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match pseudo atom
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("*N*");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match pseudo atom
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("*N(*)*");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match pseudo atom
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCC(=O)N(CCCCCCCCC)CCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match amide
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCN(CCCC)C(=O)N(CCCC)CCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match urea
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1(=O)N(CCCCCCCCCC)CCCCCC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match lactam
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCC[N+](CCCCCCCCCC)(CCCCCCC)CCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match quaternary nitrogen
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCn(cn1)cc1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match aromatic n to aliphatic C
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCCN(CCCCCCCCCC)S(=O)(=O)CCCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match sulphonamide
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCCN(CCCCCCCCCCC)=CN(CCCCCCCCCC)CCCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match amidine
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCCN(CCCCCCCCCC)C(=NCCCCCC)N(CCCCCCC)CCCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match guanidine
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCC(=O)N(CCCCCCCCCC)C(=O)CCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match imide
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCOC(=O)NCCCCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match carbamate ester
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCC(=NNCCCCCCC)CCCCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match hydrazone
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCC(=NCCCCCCC)CCCCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match imine
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCC=NCCCCCCCCCCCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match imine
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCCN(CCCCCCCC)=N(CCCCCCCCCCCCCCC)CCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match azo compounds
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCCOC#N");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match cyanate
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCCN=C=O");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match iso cyanate
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCC#N");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match nitrile
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCCON=O");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match nitrite
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCCN(=O)=O");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match nitro/nitroso
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCCN=O");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match nitro/nitroso
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCC[N+](=O)[O-]");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match nitro/nitroso
        Assertions.assertFalse(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1CCCCCCCCCC1N(C2CCCCCCCCCCCC2)C3CCCCCCCCCCC3");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do match tertiary amine connecting three rings
        Assertions.assertTrue(RECAP.TERTIARY_AMINE.getEductPattern().matches(mol));
    }

    @Test
    void testCyclicTertiaryAmineRuleIndividually() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);

        IAtomContainer mol = smiPar.parseSmiles("CCCCCCCCCN");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match primary amine
        Assertions.assertFalse(RECAP.CYCLIC_TERTIARY_AMINES_ALIPHATIC.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1(=O)N(CCCCCCCCCCC)CCCCCC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match lactam
        Assertions.assertFalse(RECAP.CYCLIC_TERTIARY_AMINES_ALIPHATIC.getEductPattern().matches(mol));


        mol = smiPar.parseSmiles("N1(CCCC)CCCCC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do match this simple example
        Assertions.assertTrue(RECAP.CYCLIC_TERTIARY_AMINES_ALIPHATIC.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("N1(C)CCCCC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do match this simple example
        Assertions.assertTrue(RECAP.CYCLIC_TERTIARY_AMINES_ALIPHATIC.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("N1(*)CCCCC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match pseudo atom
        Assertions.assertFalse(RECAP.CYCLIC_TERTIARY_AMINES_ALIPHATIC.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("N1CCCCC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match, the amine is secondary and the whole group in a ring
        Assertions.assertFalse(RECAP.CYCLIC_TERTIARY_AMINES_ALIPHATIC.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("N1(c2ccccc2)CCCCC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do match, this illustrates that the C connected to N can also be aromatic
        Assertions.assertTrue(RECAP.CYCLIC_TERTIARY_AMINES_ALIPHATIC.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("N1(C(=O)CCCCCC)CCCCC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match this amide
        Assertions.assertFalse(RECAP.CYCLIC_TERTIARY_AMINES_ALIPHATIC.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("N1(C(=N)CCCCCC)CCCCC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match this amidine
        Assertions.assertFalse(RECAP.CYCLIC_TERTIARY_AMINES_ALIPHATIC.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1(=O)CCC(=O)N1CCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match succinimide
        Assertions.assertFalse(RECAP.CYCLIC_TERTIARY_AMINES_ALIPHATIC.getEductPattern().matches(mol));
    }

    @Test
    void testAmineRulesIntegration() throws Exception {
        //TODO
    }

    @Test
    void testUreaRuleIndividually() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);

        IAtomContainer mol = smiPar.parseSmiles("CN(C)C(=O)N(C)C");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match this simple example
        Assertions.assertTrue(RECAP.UREA.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("*N(*)C(=O)N(*)*");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match pseudo atoms
        Assertions.assertFalse(RECAP.UREA.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CC(=O)N(C)C");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match amide
        Assertions.assertFalse(RECAP.UREA.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1N(C1)C(=O)N(C2)C2");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match with N in ring
        Assertions.assertTrue(RECAP.UREA.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CNC(=O)NC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match N of D2
        Assertions.assertTrue(RECAP.UREA.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("NC(=O)N");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match terminal group
        Assertions.assertFalse(RECAP.UREA.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CN(C)C(=O)N(C)C");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match this simple example
        Assertions.assertTrue(RECAP.UREA.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCNC(=O)n(cn1)cc1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do match even though one n is in a ring and aromatic
        Assertions.assertTrue(RECAP.UREA.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCNC(=O)NC(=O)NCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match this bigger FG
        Assertions.assertFalse(RECAP.UREA.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCNC(=O)NC(Cl)CCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do match with the chlorine substituent connected to the environment C
        Assertions.assertTrue(RECAP.UREA.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCNC(=O)NC=CCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match because of double bond at environment C
        Assertions.assertFalse(RECAP.UREA.getEductPattern().matches(mol));
    }

    @Test
    void testUreaRuleIntegration() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer mol = smiPar.parseSmiles("C1CC1NC(=O)NC1OC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);
        cycles.find(mol);
        arom.apply(mol);
        RECAP recap = new RECAP();
        HierarchyNode node = recap.buildHierarchy(mol);
        Assertions.assertEquals(1, node.getMaximumLevelOfAllDescendants());
        Assertions.assertEquals(2, node.getChildren().size());
        SmilesGenerator smiGen = new SmilesGenerator(SmiFlavor.Unique | SmiFlavor.UseAromaticSymbols);
        HashSet<String> childrenSmilesSet = HashSet.newHashSet(node.getChildren().size());
        for (HierarchyNode child : node.getChildren()) {
            childrenSmilesSet.add(smiGen.create(child.getStructure()));
        }
        Assertions.assertTrue(childrenSmilesSet.containsAll(List.of("*NC1CC1", "*NC1OC1")));

        //one nitrogen has an additional methyl group now
        mol = smiPar.parseSmiles("C1CC1NC(=O)N(C)C1OC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        node = recap.buildHierarchy(mol);
        Assertions.assertEquals(1, node.getMaximumLevelOfAllDescendants());
        Assertions.assertEquals(2, node.getChildren().size());
        childrenSmilesSet = HashSet.newHashSet(node.getChildren().size());
        for (HierarchyNode child : node.getChildren()) {
            childrenSmilesSet.add(smiGen.create(child.getStructure()));
        }
        Assertions.assertTrue(childrenSmilesSet.containsAll(List.of("*NC1CC1", "*N(C)C1OC1")));

        //urea in a ring - no match!
        mol = smiPar.parseSmiles("C1CCNC(=O)NC1C");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        node = recap.buildHierarchy(mol);
        Assertions.assertTrue(node.isTerminal());

        mol = smiPar.parseSmiles("c1cccn1C(=O)NC1OC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        node = recap.buildHierarchy(mol);
        Assertions.assertEquals(1, node.getMaximumLevelOfAllDescendants());
        Assertions.assertEquals(2, node.getChildren().size());
        childrenSmilesSet = HashSet.newHashSet(node.getChildren().size());
        for (HierarchyNode child : node.getChildren()) {
            childrenSmilesSet.add(smiGen.create(child.getStructure()));
        }
        Assertions.assertTrue(childrenSmilesSet.containsAll(List.of("*n1cccc1", "*NC1OC1")));

        mol = smiPar.parseSmiles("c1cccn1C(=O)n1c(C)ccc1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        node = recap.buildHierarchy(mol);
        Assertions.assertEquals(1, node.getMaximumLevelOfAllDescendants());
        Assertions.assertEquals(2, node.getChildren().size());
        childrenSmilesSet = HashSet.newHashSet(node.getChildren().size());
        for (HierarchyNode child : node.getChildren()) {
            childrenSmilesSet.add(smiGen.create(child.getStructure()));
        }
        Assertions.assertTrue(childrenSmilesSet.containsAll(List.of("*n1cccc1", "*n1cccc1C")));
    }

    @Test
    void testEtherRuleIndividually() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);

        IAtomContainer mol = smiPar.parseSmiles("O");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match water
        Assertions.assertFalse(RECAP.ETHER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCO");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match primary alcohol
        Assertions.assertFalse(RECAP.ETHER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("COC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match this simply example
        Assertions.assertTrue(RECAP.ETHER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("*O*");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match pseudo atoms
        Assertions.assertFalse(RECAP.ETHER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCO*");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not pseudo atom
        Assertions.assertFalse(RECAP.ETHER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCOC*");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //note that the pseudo atom in this position is ok
        Assertions.assertTrue(RECAP.ETHER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1CCCCOCCCC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match in ring
        Assertions.assertFalse(RECAP.ETHER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1CCCCC(CCCC1)OC2CCCCCCCCC2");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //this ring configuration is ok, ether connects the rings
        Assertions.assertTrue(RECAP.ETHER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCC(=O)OCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match ester
        Assertions.assertFalse(RECAP.ETHER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCOC(=O)OCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match carbonate
        Assertions.assertFalse(RECAP.ETHER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCOOCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match peroxide
        Assertions.assertFalse(RECAP.ETHER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCOO");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match hydroxyperoxide
        Assertions.assertFalse(RECAP.ETHER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCC(=O)OC(=O)CCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match organic acid anhydride
        Assertions.assertFalse(RECAP.ETHER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCOC#N");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match cyanate
        Assertions.assertFalse(RECAP.ETHER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCOC(=O)NCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match carbamate ester
        Assertions.assertFalse(RECAP.ETHER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCC(OCCCC)O");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match hemiacetal
        Assertions.assertTrue(RECAP.ETHER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCC(OCCCC)(O)CCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match hemiketal
        Assertions.assertTrue(RECAP.ETHER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCC(OCCCC)OCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match acetal
        Assertions.assertTrue(RECAP.ETHER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCC(OCCCC)(OCCCCCCCC)CCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match acetal/ketal
        Assertions.assertTrue(RECAP.ETHER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCC(OCCCC)(OCCCCCCCC)CCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match acetal/ketal
        Assertions.assertTrue(RECAP.ETHER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCC(OCCCC)(OCCCCCCCC)OCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match orthoester
        Assertions.assertTrue(RECAP.ETHER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCOC(OCCCC)(OCCCCCCCC)OCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match orthocarbonateester
        Assertions.assertTrue(RECAP.ETHER.getEductPattern().matches(mol));
    }

    @Test
    void testEtherRuleIntegration() throws Exception {
        //TODO
    }

    @Test
    void testOlefinRuleIndividually() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);

        IAtomContainer mol = smiPar.parseSmiles("C=C");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match ethylene
        Assertions.assertFalse(RECAP.OLEFIN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCC=C");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match because one side is terminal
        Assertions.assertFalse(RECAP.OLEFIN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCC=C*");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match pseudo atom
        Assertions.assertFalse(RECAP.OLEFIN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("*C=C*");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match pseudo atoms
        Assertions.assertFalse(RECAP.OLEFIN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCC=CCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match this simple example
        Assertions.assertTrue(RECAP.OLEFIN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCC(CCCCC)=C(CCCCCC)CCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match with D3 carbons
        Assertions.assertTrue(RECAP.OLEFIN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCC=C(O)CCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match enol
        Assertions.assertFalse(RECAP.OLEFIN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCC=C(*)CCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not pseudo atom
        Assertions.assertFalse(RECAP.OLEFIN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1CCCCC=CCCCCCC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match because of ring
        Assertions.assertFalse(RECAP.OLEFIN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1CCCCC1=C2CCCCCC2");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //this ring configuration is ok
        Assertions.assertTrue(RECAP.OLEFIN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCC=CC(=O)CCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match conjugated system
        Assertions.assertFalse(RECAP.OLEFIN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCC=CC(Cl)CCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //the chlorine substituent on the environmental C is ok
        Assertions.assertTrue(RECAP.OLEFIN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CC=CC=CC=CC=CC=CC=C");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match conjugated system
        Assertions.assertFalse(RECAP.OLEFIN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCC=C=CCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match the sp3 carbon
        Assertions.assertFalse(RECAP.OLEFIN.getEductPattern().matches(mol));
    }

    @Test
    void testOlefinRuleIntegration() throws Exception {
        //TODO
    }

    @Test
    void testQuaternaryNitrogenRuleIndividually() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);

        IAtomContainer mol = smiPar.parseSmiles("[NH4+]");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match ammonia
        Assertions.assertFalse(RECAP.QUATERNARY_NITROGEN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("*[N+](*)(*)*");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match pseudo atoms
        Assertions.assertFalse(RECAP.QUATERNARY_NITROGEN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCC[N+](CCCCCCCCC)(CCCCCCCC)*");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match pseudo atom
        Assertions.assertFalse(RECAP.QUATERNARY_NITROGEN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCC[NH+](CCCCCCCCC)CCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match tertiary amine with a positive charge
        Assertions.assertFalse(RECAP.QUATERNARY_NITROGEN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCC[NH3+]");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match primary amine with a positive charge
        Assertions.assertFalse(RECAP.QUATERNARY_NITROGEN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C[N+](C)(C)C");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match this simple example
        Assertions.assertTrue(RECAP.QUATERNARY_NITROGEN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCC[N+](CCCCCCCC)(CCCCCCC)CCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match this simple example
        Assertions.assertTrue(RECAP.QUATERNARY_NITROGEN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1[N+](C1)(C)C");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match in ring
        Assertions.assertFalse(RECAP.QUATERNARY_NITROGEN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1CCCCCCCCCCC[N+]1(C)(C)C");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match in ring
        Assertions.assertFalse(RECAP.QUATERNARY_NITROGEN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1CCCCCCCC1[N+](C)(C)C");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //the environmental Cs can be in rings
        Assertions.assertTrue(RECAP.QUATERNARY_NITROGEN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("c1ccccc1[N+](C)(C)C");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //the environmental Cs can be in rings, even aromatic rings
        Assertions.assertTrue(RECAP.QUATERNARY_NITROGEN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("c1[nH+]cc[nH]1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match charged imidazole
        Assertions.assertFalse(RECAP.QUATERNARY_NITROGEN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCCN(=O)=O");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match nitro/nitroso
        Assertions.assertFalse(RECAP.QUATERNARY_NITROGEN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCCN=O");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match nitro/nitroso
        Assertions.assertFalse(RECAP.QUATERNARY_NITROGEN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCC[N+](=O)[O-]");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match nitro/nitroso
        Assertions.assertFalse(RECAP.QUATERNARY_NITROGEN.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCOC#[NH+]");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match charged cyanate
        Assertions.assertFalse(RECAP.QUATERNARY_NITROGEN.getEductPattern().matches(mol));
    }

    @Test
    void testQuaternaryNitrogenRuleIntegration() throws Exception {
        //TODO
    }

    @Test
    void testAromaticNitrogenToAliphaticCarbonRuleIndividually() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);

        IAtomContainer mol = smiPar.parseSmiles("CCCCCCCCCCCN");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match primary amine
        Assertions.assertFalse(RECAP.AROMATIC_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCCCNCCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match secondary amine
        Assertions.assertFalse(RECAP.AROMATIC_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCCCCCCCN(CCCCCCCCCCC)CCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match tertiary amine
        Assertions.assertFalse(RECAP.AROMATIC_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1(=O)N(CCCCCCCCCCC)CCCCCC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match lactam
        Assertions.assertFalse(RECAP.AROMATIC_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("N1(CCCC)CCCCC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match aliphatic N to aliphatic C
        Assertions.assertFalse(RECAP.AROMATIC_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("c1ccc[nH]1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match pyrrole without any exocyclic groups
        Assertions.assertFalse(RECAP.AROMATIC_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("c1cccn1*");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match pseudo atom
        Assertions.assertFalse(RECAP.AROMATIC_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("c1cccn1CCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match this simple example
        Assertions.assertTrue(RECAP.AROMATIC_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("c1ncnn1C(C)(C)C");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match this example from the paper
        Assertions.assertTrue(RECAP.AROMATIC_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("c1ncnn1C(=O)CCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match this kind of amine
        Assertions.assertFalse(RECAP.AROMATIC_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("c12ncnn1CCCCCC2");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match this ring system
        Assertions.assertFalse(RECAP.AROMATIC_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1CCCCN1c2ccccc2");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match because N is aliphatic and C aromatic
        Assertions.assertFalse(RECAP.AROMATIC_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("c1ncnn1C2CCCCC2");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match these two rings, the cleaved bond is not in a any ring but connecting them
        Assertions.assertTrue(RECAP.AROMATIC_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));
    }

    @Test
    void testAromaticNitrogenToAliphaticCarbonRuleIntegration() throws Exception {
        //TODO
    }

    @Test
    void testLactamNitrogenToAliphaticCarbonRuleIndividually() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);

        IAtomContainer mol = smiPar.parseSmiles("C1CCCC(=O)N1CCCCCCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match this simple example
        Assertions.assertTrue(RECAP.LACTAM_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1CCCC(=O)N1*");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match pseudo atom
        Assertions.assertFalse(RECAP.LACTAM_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1CCCC(=O)N1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match terminal group (no side chain connected to the N)
        Assertions.assertFalse(RECAP.LACTAM_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1CCCC(=O)N1N");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match because of connected amine / hetero atom
        Assertions.assertFalse(RECAP.LACTAM_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCC(=O)N(CCCCC)CCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match acyclic amide
        Assertions.assertFalse(RECAP.LACTAM_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1CCN(CCCCC)C(=O)N1CCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match cyclic urea
        Assertions.assertFalse(RECAP.LACTAM_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1(=O)CCCC(=O)N1CCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match cyclic imide
        Assertions.assertFalse(RECAP.LACTAM_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1CCOC(=O)N1CCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match cyclic carbamate ester
        Assertions.assertFalse(RECAP.LACTAM_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1CCCCN1CCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match tertiary amine in ring
        Assertions.assertFalse(RECAP.LACTAM_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1CC=CC(=O)N1CCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match because environmental C has a double bond inside the ring
        Assertions.assertFalse(RECAP.LACTAM_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1CCC(=C)C(=O)N1CCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match because environmental C has a double bond outside the ring
        Assertions.assertFalse(RECAP.LACTAM_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1CCCC(=O)N1C2CCCCCC2");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match even though the connected C is in a ring
        Assertions.assertTrue(RECAP.LACTAM_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1CCCC(=O)N1c2ccccc2");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match because the connected C is in a benzene ring (aromatic!)
        Assertions.assertFalse(RECAP.LACTAM_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1(N)CCCC(=O)N1CCCCCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match even though one environmental C is connected to a hetero atom
        Assertions.assertTrue(RECAP.LACTAM_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1CCCC(=O)N1C=O");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match because the connected C is an aldehyde
        Assertions.assertFalse(RECAP.LACTAM_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1CCCC(=O)N1C*");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //note that this pseudo atom is ok, could also be a hetero atom
        Assertions.assertTrue(RECAP.LACTAM_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("O=c1n(CCCCCCCCCCC)c2c(cccc2)c3c1cccc3");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match aromatic lactam
        Assertions.assertTrue(RECAP.LACTAM_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1CCC2CC(=O)N1CCCCCC2");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match because the bond supposed to be cleaved is in a ring
        Assertions.assertFalse(RECAP.LACTAM_NITROGEN_TO_ALIPHATIC_CARBON.getEductPattern().matches(mol));
    }

    @Test
    void testLactamNitrogenToAliphaticCarbonRuleIntegration() throws Exception {
        //TODO
    }

    @Test
    void testAromaticCarbonToAromaticCarbonRuleIndividually() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);

        IAtomContainer mol = smiPar.parseSmiles("c1ccccc1c2ccccc2");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match this simple example
        Assertions.assertTrue(RECAP.AROMATIC_CARBON_TO_AROMATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("c1ccccc1*");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match connection to pseudo atom
        Assertions.assertFalse(RECAP.AROMATIC_CARBON_TO_AROMATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1=CC=CC=C1C2=CC=CC=C2");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match this kekulised structure, proves that the exception for aromatic double bonds works
        Assertions.assertTrue(RECAP.AROMATIC_CARBON_TO_AROMATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("c1cnccc1c2ccccc2");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match this example from the paper
        Assertions.assertTrue(RECAP.AROMATIC_CARBON_TO_AROMATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("c1cccnc1c2ccccc2");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //match this, even though the N is now a direct neighbor of one of the bond Cs
        Assertions.assertTrue(RECAP.AROMATIC_CARBON_TO_AROMATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("c1cnccc1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match a single benzene
        Assertions.assertFalse(RECAP.AROMATIC_CARBON_TO_AROMATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("c1cnccc1C");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match toluene
        Assertions.assertFalse(RECAP.AROMATIC_CARBON_TO_AROMATIC_CARBON.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C12=CC=CC=C1C3=CC=CC=C32");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match because there are two connections of the rings
        Assertions.assertFalse(RECAP.AROMATIC_CARBON_TO_AROMATIC_CARBON.getEductPattern().matches(mol));
    }

    @Test
    void testAromaticCarbonToAromaticCarbonRuleIntegration() throws Exception {
        //TODO
    }

    @Test
    void testSulphonamideRuleIndividually() throws Exception {

    }

    @Test
    void testSulphonamideRuleIntegration() throws Exception {

    }

    @Test
    void testAromaticNitrogenToAromaticCarbonRuleIndividually() throws Exception {

    }

    @Test
    void testAromaticNitrogenToAromaticCarbonRuleIntegration() throws Exception {

    }

    @Test
    void recapPaperExampleTest() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer mol = smiPar.parseSmiles("FC1=CC=C(OCCCN2CCC(NC(C3=CC(Cl)=C(N)C=C3OC)=O)C(OC)C2)C=C1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);
        cycles.find(mol);
        arom.apply(mol);
        RECAP recap = new RECAP();
        HierarchyNode node = recap.buildHierarchy(mol);
        SmilesGenerator smiGen = new SmilesGenerator(SmiFlavor.Unique | SmiFlavor.UseAromaticSymbols);
        Set<String> terminalDescendantsSmilesSet = HashSet.newHashSet(node.getOnlyTerminalDescendants().size());
        for (HierarchyNode child : node.getOnlyTerminalDescendants()) {
            terminalDescendantsSmilesSet.add(smiGen.create(child.getStructure()));
        }
        Assertions.assertEquals(4, terminalDescendantsSmilesSet.size());
        //generally corresponds to RECAP paper, except for the Fluorphenole described there,
        // but it is not described how this came to be; plus, we make a carboxylic acid and an amine out of the amide, not an aldehyde
        Assertions.assertTrue(terminalDescendantsSmilesSet.containsAll(List.of("*Oc1ccc(F)cc1", "*OCCCN*", "*NC1CCN(*)CC1OC", "*OC(=O)c1cc(Cl)c(N)cc1OC")));
    }

    //TODO
    //@Test
    void testMinimumFragmentSizeChanges() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer mol = smiPar.parseSmiles("C1CC1Oc1ccccc1-c1ncc(OC)cc1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);
        cycles.find(mol);
        arom.apply(mol);
        RECAP recap = new RECAP();
        HierarchyNode node = recap.buildHierarchy(mol, 1); //get full hierarchy without size restriction
        Assertions.assertEquals(3, node.getMaximumLevelOfAllDescendants());
        SmilesGenerator smiGen = new SmilesGenerator(SmiFlavor.Unique | SmiFlavor.UseAromaticSymbols);
        int i = 0;
        System.out.println(i + ": " + smiGen.create(node.getStructure()) + " (level " + node.getLevel() + ")");
        for (HierarchyNode childFirstLevel : node.getChildren()) {
            i++;
            System.out.println(i + ":\t " + smiGen.create(childFirstLevel.getStructure()) + " (level " + childFirstLevel.getLevel() + ")");
            for (HierarchyNode childSecondLevel : childFirstLevel.getChildren()) {
                i++;
                System.out.println(i + ":\t\t " + smiGen.create(childSecondLevel.getStructure()) + " (level " + childSecondLevel.getLevel() + ")");
                for (HierarchyNode childThirdLevel : childSecondLevel.getChildren()) {
                    i++;
                    System.out.println(i + ":\t\t\t " + smiGen.create(childThirdLevel.getStructure()) + " (level " + childThirdLevel.getLevel() + ")");
                    Assertions.assertTrue(childThirdLevel.isTerminal());
                }
            }
        }
        Assertions.assertEquals(6, node.getChildren().size());
        Assertions.assertEquals(26, node.getAllDescendants().size());
        Assertions.assertEquals(18, node.getOnlyTerminalDescendants().size());
        Assertions.assertTrue(node.getParents().isEmpty());

        node = recap.buildHierarchy(mol, 2); //do not cleave off the methyl-ether
        Assertions.assertEquals(4, node.getChildren().size());
        Assertions.assertEquals(8, node.getAllDescendants().size());
        Assertions.assertEquals(6, node.getOnlyTerminalDescendants().size());
        Assertions.assertTrue(node.getParents().isEmpty());

        node = recap.buildHierarchy(mol, 4); //do not cleave off the cyclo-propane either BUT it is cyclic, so actually no change!
        Assertions.assertEquals(4, node.getChildren().size());
        Assertions.assertEquals(8, node.getAllDescendants().size());
        Assertions.assertEquals(6, node.getOnlyTerminalDescendants().size());
        Assertions.assertTrue(node.getParents().isEmpty());

        node = recap.buildHierarchy(mol, 5); //default they talk about in the paper
        Assertions.assertEquals(4, node.getChildren().size());
        Assertions.assertEquals(8, node.getAllDescendants().size());
        Assertions.assertEquals(6, node.getOnlyTerminalDescendants().size());
        Assertions.assertTrue(node.getParents().isEmpty());
    }

    /**
     *
     */
    //TODO
    //@Test
    void testMinFragmentSize() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer mol = smiPar.parseSmiles("CCCOCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);
        cycles.find(mol);
        arom.apply(mol);
        RECAP recap = new RECAP();
        HierarchyNode node = recap.buildHierarchy(mol);
        //no cleavage with default minimum fragment size 5 carbons
        Assertions.assertTrue(node.getChildren().isEmpty());
        node = recap.buildHierarchy(mol, 3);
        //but successful cleavage with min fragment size of 3
        Assertions.assertEquals(2, node.getChildren().size());
        SmilesGenerator smiGen = new SmilesGenerator(SmiFlavor.Unique | SmiFlavor.UseAromaticSymbols);
        Assertions.assertEquals("*OCCC", smiGen.create(node.getChildren().getFirst().getStructure()));
        //one side only ethyl now
        mol = smiPar.parseSmiles("CCOCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        node = recap.buildHierarchy(mol, 4);
        //no cleavage even with minimum fragment size 3 carbons because one
        // fragment would be forbidden
        Assertions.assertTrue(node.getChildren().isEmpty());
        mol = smiPar.parseSmiles("CCCOCCOC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        node = recap.buildHierarchy(mol, 2);
        //one cleavage with minimum fragment size 2 carbons is possible now but
        // the other ether stays intact, *CCC and *CCOC
        Assertions.assertEquals(2, node.getChildren().size());
        //all in agreement with RDKit
    }

    /**
     * Corresponds to the example given in the doc of the RDKit implementation
     * and their first test.
     *
     * @throws Exception
     */
    @Test
    void rdkitDocExampleTest() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer mol = smiPar.parseSmiles("C1CC1Oc1ccccc1-c1ncc(OC)cc1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);
        cycles.find(mol);
        arom.apply(mol);
        RECAP recap = new RECAP();
        HierarchyNode node = recap.buildHierarchy(mol);
        SmilesGenerator smiGen = new SmilesGenerator(SmiFlavor.Unique | SmiFlavor.UseAromaticSymbols);

        Set<String> directChildrenSmilesSet = HashSet.newHashSet(node.getChildren().size());
        for (HierarchyNode child : node.getChildren()) {
            directChildrenSmilesSet.add(smiGen.create(child.getStructure()));
        }
        Assertions.assertEquals(4, directChildrenSmilesSet.size());
        //corresponds to SMILES codes given in RDKit doc
        Assertions.assertTrue(directChildrenSmilesSet.containsAll(List.of("*OC1CC1", "*c1ncc(OC)cc1", "*Oc1ccccc1-c2ncc(OC)cc2", "*c1ccccc1OC2CC2")));

        Set<String> allDescendantsSmilesSet = HashSet.newHashSet(node.getAllDescendants().size());
        for (HierarchyNode child : node.getAllDescendants()) {
            allDescendantsSmilesSet.add(smiGen.create(child.getStructure()));
        }
        Assertions.assertEquals(5, allDescendantsSmilesSet.size());
        //corresponds to SMILES codes given in RDKit doc
        Assertions.assertTrue(allDescendantsSmilesSet.containsAll(List.of("*OC1CC1", "*c1ncc(OC)cc1", "*Oc1ccccc1*", "*Oc1ccccc1-c2ncc(OC)cc2", "*c1ccccc1OC2CC2")));

        Set<String> terminalDescendantsSmilesSet = HashSet.newHashSet(node.getOnlyTerminalDescendants().size());
        for (HierarchyNode child : node.getOnlyTerminalDescendants()) {
            terminalDescendantsSmilesSet.add(smiGen.create(child.getStructure()));
        }
        Assertions.assertEquals(3, terminalDescendantsSmilesSet.size());
        //corresponds to SMILES codes given in RDKit doc
        Assertions.assertTrue(terminalDescendantsSmilesSet.containsAll(List.of("*OC1CC1", "*c1ncc(OC)cc1", "*Oc1ccccc1*")));
    }

    /**
     * Corresponds to RDKit implementation test 2.
     *
     * @throws Exception
     */
    @Test
    void testDipropylEther() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer mol = smiPar.parseSmiles("CCCOCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);
        cycles.find(mol);
        arom.apply(mol);
        RECAP recap = new RECAP();
        HierarchyNode node = recap.buildHierarchy(mol);
        // with the default minimum fragment size = 5, this molecule should not be cleaved
        Assertions.assertTrue(node.getChildren().isEmpty());
        node = recap.buildHierarchy(mol, 3);
        SmilesGenerator smiGen = new SmilesGenerator(SmiFlavor.Unique | SmiFlavor.UseAromaticSymbols);
        int i = 0;
        System.out.println(i + ": " + smiGen.create(node.getStructure()));
        for (HierarchyNode childFirstLevel : node.getChildren()) {
            i++;
            System.out.println(i + ":\t " + smiGen.create(childFirstLevel.getStructure()));
        }
        // now, it is cleaved into two times *CCC
        Assertions.assertEquals(2, node.getChildren().size());
    }
    /**
     *
     */
    @Test
    void test2PhenylPyridine() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer mol = smiPar.parseSmiles("c1ccccc1-c1ncccc1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);
        cycles.find(mol);
        arom.apply(mol);
        RECAP recap = new RECAP();
        HierarchyNode node = recap.buildHierarchy(mol);
        Assertions.assertEquals(2, node.getChildren().size());
    }
    /**
     *
     */
    @Test
    void testTriphenoxymethane() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer mol = smiPar.parseSmiles("c1ccccc1OC(Oc1ccccc1)Oc1ccccc1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);
        cycles.find(mol);
        arom.apply(mol);
        RECAP recap = new RECAP();
        HierarchyNode node = recap.buildHierarchy(mol);
        SmilesGenerator smiGen = new SmilesGenerator(SmiFlavor.Unique | SmiFlavor.UseAromaticSymbols);
        Set<String> terminalDescendantsSmilesSet = HashSet.newHashSet(node.getOnlyTerminalDescendants().size());
        for (HierarchyNode child : node.getOnlyTerminalDescendants()) {
            terminalDescendantsSmilesSet.add(smiGen.create(child.getStructure()));
        }
        Assertions.assertEquals(2, terminalDescendantsSmilesSet.size());
        // major difference to RDKit implementation which filters also small
        // non-terminal(!) alkyl fragments (so forbids *C(*)*); it reports
        // fragments *c1ccccc1 and *C(*)Oc1ccccc1
        Assertions.assertTrue(terminalDescendantsSmilesSet.containsAll(List.of("*OC(O*)O*", "*Oc1ccccc1")));
    }
    /**
     *
     */
    @Test
    void test1Butylpiperidine() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer mol = smiPar.parseSmiles("C1CCCCN1CCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);
        cycles.find(mol);
        arom.apply(mol);
        RECAP recap = new RECAP();
        HierarchyNode node = recap.buildHierarchy(mol);
        Assertions.assertEquals(2, node.getChildren().size());
        SmilesGenerator smiGen = new SmilesGenerator(SmiFlavor.Unique | SmiFlavor.UseAromaticSymbols);
        Set<String> childrenSmilesSet = HashSet.newHashSet(node.getOnlyTerminalDescendants().size());
        for (HierarchyNode child : node.getChildren()) {
            childrenSmilesSet.add(smiGen.create(child.getStructure()));
        }
        Assertions.assertTrue(childrenSmilesSet.containsAll(List.of("*N1CCCCC1", "*NCCCC")));
    }

    /**
     *
     */
    @Test
    void noMatchTest() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer mol = smiPar.parseSmiles("C1CC1C(=O)CC1OC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);
        cycles.find(mol);
        arom.apply(mol);
        RECAP recap = new RECAP();
        HierarchyNode node = recap.buildHierarchy(mol);
        Assertions.assertTrue(node.getChildren().isEmpty());

        mol = smiPar.parseSmiles("C1CCC(=O)NC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        node = recap.buildHierarchy(mol);
        Assertions.assertTrue(node.getChildren().isEmpty());
    }

    private void printTree(HierarchyNode node) throws Exception {
        SmilesGenerator smiGen = new SmilesGenerator(SmiFlavor.Unique | SmiFlavor.UseAromaticSymbols);
        int i = 0;
        System.out.println(i + ": " + smiGen.create(node.getStructure()) + " (level " + node.getLevel() + ")");
        Assertions.assertTrue(node.getMaximumLevelOfAllDescendants() <= 5);
        for (HierarchyNode childFirstLevel : node.getChildren()) {
            i++;
            System.out.println(i + ":\t " + smiGen.create(childFirstLevel.getStructure()) + " (level " + childFirstLevel.getLevel() + ")");
            for (HierarchyNode childSecondLevel : childFirstLevel.getChildren()) {
                i++;
                System.out.println(i + ":\t\t " + smiGen.create(childSecondLevel.getStructure()) + " (level " + childSecondLevel.getLevel() + ")");
                for (HierarchyNode childThirdLevel : childSecondLevel.getChildren()) {
                    i++;
                    System.out.println(i + ":\t\t\t " + smiGen.create(childThirdLevel.getStructure()) + " (level " + childThirdLevel.getLevel() + ")");
                    for (HierarchyNode childFourthLevel : childThirdLevel.getChildren()) {
                        i++;
                        System.out.println(i + ":\t\t\t\t " + smiGen.create(childFourthLevel.getStructure()) + " (level " + childFourthLevel.getLevel() + ")");
                        for (HierarchyNode childFifthLevel : childFourthLevel.getChildren()) {
                            i++;
                            System.out.println(i + ":\t\t\t\t\t " + smiGen.create(childFifthLevel.getStructure()) + " (level " + childFifthLevel.getLevel() + ")");
                            Assertions.assertTrue(childFifthLevel.isTerminal());
                        }
                    }
                }
            }
        }
    }
}
