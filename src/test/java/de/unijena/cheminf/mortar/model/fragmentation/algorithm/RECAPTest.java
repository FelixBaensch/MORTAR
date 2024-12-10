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
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class RECAPTest extends RECAP {
    @Test
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
        Assertions.assertTrue(directChildrenSmilesSet.containsAll(List.of("*C1CC1", "*c1ncc(OC)cc1", "*c1ccccc1-c2ncc(OC)cc2", "*c1ccccc1OC2CC2")));

        Set<String> allDescendantsSmilesSet = HashSet.newHashSet(node.getAllDescendants().size());
        for (HierarchyNode child : node.getAllDescendants()) {
            allDescendantsSmilesSet.add(smiGen.create(child.getStructure()));
        }
        Assertions.assertEquals(5, allDescendantsSmilesSet.size());
        //corresponds to SMILES codes given in RDKit doc
        Assertions.assertTrue(allDescendantsSmilesSet.containsAll(List.of("*C1CC1", "*c1ncc(OC)cc1", "*c1ccccc1*", "*c1ccccc1-c2ncc(OC)cc2", "*c1ccccc1OC2CC2")));

        Set<String> terminalDescendantsSmilesSet = HashSet.newHashSet(node.getOnlyTerminalDescendants().size());
        for (HierarchyNode child : node.getOnlyTerminalDescendants()) {
            terminalDescendantsSmilesSet.add(smiGen.create(child.getStructure()));
        }
        Assertions.assertEquals(3, terminalDescendantsSmilesSet.size());
        //corresponds to SMILES codes given in RDKit doc
        Assertions.assertTrue(terminalDescendantsSmilesSet.containsAll(List.of("*C1CC1", "*c1ncc(OC)cc1", "*c1ccccc1*")));
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
        Assertions.assertTrue(terminalDescendantsSmilesSet.containsAll(List.of("*c1ccc(F)cc1", "*CCC*", "*NC1CCN(*)CC1OC", "*OC(=O)c1cc(Cl)c(N)cc1OC")));
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
        Assertions.assertTrue(terminalDescendantsSmilesSet.containsAll(List.of("*C(*)*", "*c1ccccc1")));
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
        //another difference from the RDKit implementation which filters all
        // simple alkyls up to propyl (not butyl like in the RECAP paper!) if no
        // minimum fragment size is defined (default set to 0)
        Assertions.assertEquals(0, node.getChildren().size());
    }
    /**
     *
     */
    @Test
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
        Assertions.assertEquals("*CCC", smiGen.create(node.getChildren().getFirst().getStructure()));
        //one side only ethyl now
        mol = smiPar.parseSmiles("CCOCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        node = recap.buildHierarchy(mol, 3);
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
    /**
     *
     */
    @Test
    void testAmideRuleIndividually() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);

        IAtomContainer mol = smiPar.parseSmiles("CCCCC(=O)N(*)*");
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
        //match this simple example
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
        //do match even though n is in a ring and aromatic
        Assertions.assertTrue(RECAP.AMIDE.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("CCCCCN(CCCC)C(=O)N(CCCC)CCCC");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        //do not match urea
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
        node = recap.buildHierarchy(mol);
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
        //do not match because O is terminal
        Assertions.assertFalse(RECAP.ESTER.getEductPattern().matches(mol));

        mol = smiPar.parseSmiles("C1(=O)OCCCCCC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        //do not match because of the ring
        Assertions.assertFalse(RECAP.ESTER.getEductPattern().matches(mol));
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
    void testUreaRule() throws Exception {
        SmilesParser smiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer mol = smiPar.parseSmiles("C1CC1NC(=O)NC1OC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        CycleFinder cycles = Cycles.cdkAromaticSet();
        Aromaticity arom = new Aromaticity(ElectronDonation.cdk(), cycles);
        cycles.find(mol);
        arom.apply(mol);
        RECAP recap = new RECAP();
        HierarchyNode node = recap.buildHierarchy(mol);
        //TODO: so far, we are in agreement with RDKit, but throwing away the whole keto group is highly questionable
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
        Assertions.assertEquals(1, node.getMaximumLevelOfAllDescendants());
        Assertions.assertEquals(2, node.getChildren().size());

        //urea in a ring - no match!
        mol = smiPar.parseSmiles("C1CCNC(=O)NC1C");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        node = recap.buildHierarchy(mol);
        Assertions.assertTrue(node.isTerminal());

        //TODO rules "aromatic N to aliphatic C", cyclic amines, and amide also match here (some the products)
        // but the products should only be *n1cccc1 (level 1) and *NC1OC1 (level 1)
        mol = smiPar.parseSmiles("c1cccn1C(=O)NC1OC1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        node = recap.buildHierarchy(mol);

        //TODO: this also matches too many rules, the products should only be *n1cccc1 (level 1) and *n1cccc1C (level 1)
        mol = smiPar.parseSmiles("c1cccn1C(=O)n1c(C)ccc1");
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
        cycles.find(mol);
        arom.apply(mol);
        node = recap.buildHierarchy(mol);
    }

    @Test
    void testAmineRule() throws Exception {
        //carry on here...
    }

    private void printTree(HierarchyNode node) throws Exception {
        SmilesGenerator smiGen = new SmilesGenerator(SmiFlavor.Unique | SmiFlavor.UseAromaticSymbols);
        int i = 0;
        System.out.println(i + ": " + smiGen.create(node.getStructure()) + " (level " + node.getLevel() + ")");
        Assertions.assertTrue(node.getMaximumLevelOfAllDescendants() <= 4);
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
                        Assertions.assertTrue(childFourthLevel.isTerminal());
                    }
                }
            }
        }
    }
}
