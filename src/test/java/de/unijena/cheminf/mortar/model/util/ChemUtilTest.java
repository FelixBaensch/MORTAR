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

package de.unijena.cheminf.mortar.model.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.HydrogenState;

import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Tests for the utility functions in ChemUtil.
 *
 * @author Jonas Schaub
 */
class ChemUtilTest {
    /**
     * Round trip test for a few molecules randomly picked from COCONUT. Uses ChemUtil methods to parse the given
     * CDK canonical SMILES and re-generate SMILES with stereo chemistry from the resulting atom containers.
     */
    @Test
    void testParseAndCreateUniqueSmilesRoundTrip() throws Exception {
        String[] tmpSmilesCodes = new String[] {
                "C/C=C(\\C)/C(=O)O[C@H]1C[C@@H]2C[C@@H](C[C@H]1N2C)OC(=O)/C=C(\\C)/C(=O)OCC", //CNP0315572.1
                "C/C(=C\\C[C@@H]([C@@H](C)[C@H]1CC[C@@]2(C)C3=CC[C@H]4C(C)(C)[C@@H](CC[C@]4(C)C3=CC[C@]12C)O)OC(=O)C)/C(=O)O", //CNP0219624.2
                "CC1=C2C(=O)C=C(C)C2[C@@H]3C(CC1)C(=C)C(=O)O3", //CNP0408686.4
                "C[C@@]12CC[C@@H](C[C@H]2CC[C@@H]3[C@@H]1[C@@H](C[C@]4(C)[C@@H](CC[C@]34O)C5=COC(=O)C=C5)O)OC(=O)CCCCCCC(=O)O", //CNP0222895.4
                "N#CS", //CNP0590020.0
                "CCCCCCC/C=C\\[C@@H](C#CC1=C2C(=CC3=CC(=C(C=C31)O)OC)CO[C@@H]2C=C)O", //CNP0532297.1
                "C[C@@H]1[C@@H]2[C@H](C[C@@H](O1)O[C@H]3CC[C@]4(C=O)[C@H]5CC[C@]6(C)[C@H](CC[C@@]6([C@@H]5CC[C@@]4(C3)O)O)C7=CC(=O)OC7)OC(C)(C)O2", //CNP0300894.8
                "CC1(C)CC[C@@]2(CC[C@]3(C)C(=CC[C@@H]4[C@@]5(C)CC[C@@H](C(C)(C)[C@@H]5CC[C@]43C)O[C@H]6[C@@H]([C@H]([C@@H]([C@@H](CO)O6)O)O[C@H]7[C@@H]([C@H]([C@@H]([C@@H](CO)O7)O)O)O)O[C@H]8[C@@H]([C@H]([C@@H]([C@@H](CO)O8)O)O)O)[C@@H]2C1)C(=O)O", //CNP0232655.2
                "CC1(C)[C@H]2C[C@@H]([C@]1(C)C[C@@H]2O)O[C@@H]3[C@@H]([C@@H]([C@@H]([C@H](CO[C@@H]4[C@@H]([C@](CO)(CO4)O)O)O3)O)O)O", //CNP0192219.5
                "C[C@@H](C1=CC=C2C(=C1)CC[C@H]3[C@H]2CC(=O)[C@]4(CC=CC(=O)[C@]34C)O)[C@@H]5C[C@H]6[C@@H](C(=O)O5)O6", //CNP0584929.1
                "CC1=C2C(=C(C(=C1)Cl)O)C(=O)C3=C(N(C(=C3)Cl)[C@@H]4[C@@H]([C@H]([C@@H]([C@@H](CO)O4)OC)O)O)O2", //CNP0110463.2
                "O(C1=CC(C=CC2=CC=CC=C2)=CC(OC)=C1CC=C(C)C)C", //CNP0421031.0
                "C#C[C@]1(CC[C@H]2[C@@H]3CCC4=C/C(=N/OCC(=O)N[C@H](C5=CC=CC=C5)C(=O)OC)/CC[C@]4(C)[C@H]3CC[C@@]21C)O", //CNP0232059.4
                "O=C(OC1=CC=C2C(=O)C3=CC=CC=C3C(=O)C2=C1O)C", //CNP0202328.0
                "CCCCC/C=C/C=C/C(=O)O[C@@H]1[C@@H](C)O[C@H](C[C@H]1O)C2=CC=C3C(=C2O)C(=O)C4=CC5=C(C=C(C)C=C5C(=C4C3=O)O)O", //CNP0275621.2
                "CC1=CCC[C@@]2(C)[C@@H]1O[C@@H]3C[C@H]([C@@]2(C)[C@@]43CO4)OC(=O)C", //CNP0086400.1
                "CC(C)C(=O)N[C@@H]1CCCN1C(=O)[C@@H]2[C@@H](C3=CC=CC=C3)[C@@]4(C5=CC=C(C=C5)OC)[C@H]([C@@]2(C6=C(C7=C(C=C6O4)OCO7)OC)O)OC(=O)C", //CNP0271679.1
                "CC(=CCC[C@H](C)C1=CC(=O)C(=C(C1=O)O)C)C", //CNP0223969.1
                "CC1[C@@H](C([C@@H]([C@@H](O1)OC2=CC(=CC(=C2O)O)C3=C(C(=O)C4=C(C=C(C=C4O3)O)O)O)O)O)O", //CNP0121867.3
                "CC(=O)O[C@@H]1C[C@@H]2[C@@]3(C)CCCC(C)(C)[C@@H]3CC[C@@]2(C)[C@@H]4CC=C5CO[C@H]([C@@H]5[C@]41C)O" //COCONUT CNP0276098.2 12-Epi-Deoxoscalarin
        };
        for (String tmpSmilesCode : tmpSmilesCodes) {
            IAtomContainer tmpMolecule = ChemUtil.parseSmilesToAtomContainer(tmpSmilesCode, false, false);
            String tmpSmilesCodeOutput = ChemUtil.createUniqueSmiles(tmpMolecule, true);
            Assertions.assertEquals(tmpSmilesCode, tmpSmilesCodeOutput);
        }
    }
    //
    /**
     * Test importing a MOL file containing a molecule with radicals and verifying that these are fixed correctly.
     */
    @Test
    void testFixRadicals() throws Exception {
        URL tmpURL = this.getClass().getResource("Mirabilin_B.mol");
        File tmpResourceFile = Paths.get(Objects.requireNonNull(tmpURL, "Failed to fetch resource file Mirabilin_B.mol").toURI()).toFile();
        MDLV2000Reader tmpReader = new MDLV2000Reader(new FileReader(tmpResourceFile));
        IAtomContainer tmpMolecule = tmpReader.read(SilentChemObjectBuilder.getInstance().newAtomContainer());
        tmpReader.close();
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpMolecule);
        ChemUtil.fixRadicals(tmpMolecule);
        SmilesGenerator smiGen = new SmilesGenerator(SmiFlavor.Canonical);
        Assertions.assertEquals("N=C1N=C2C3=C(N1)CCC3CC(C)C2CCCC", smiGen.create(tmpMolecule));
    }
    //
    /**
     * Makes sure that the Regex pattern in ChemUtil.fixAromaticNitrogenAndCreateSMILES() does not match 'n' in '[Sn]'.
     */
    @Test
    void testFixAromaticNitrogenAndCreateSMILESPattern() throws Exception {
        //PubChem CID	16682804
        String tmpSmiles = "CC(=O)O[Sn](C1=CC=CC=C1)(C2=CC=CC=C2)C3=CC=CC=C3";
        Pattern tmpNPattern = Pattern.compile(ChemUtil.AROMATIC_N_REGEX);
        Assertions.assertFalse(tmpNPattern.matcher(tmpSmiles).find());
    }
    //
    /**
     * Tests fixing a molecule imported from an aromatic SMILES string that is missing an explicit H on an aromatic N.
     */
    @Test
    void testFixAromaticNitrogenAndCreateSMILES() throws Exception {
        //CHEBI:929 - one n needs to be fixed
        String tmpSmilesCode = "Nc1nc(N[C@@H]2O[C@H](COP(=O)(O)OP(=O)(O)OP(=O)(O)O)[C@@H](O)[C@H]2O)c(N)c(=O)n1";
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        tmpSmiPar.kekulise(false);
        IAtomContainer tmpMolecule = tmpSmiPar.parseSmiles(tmpSmilesCode);
        String tmpFixedSmiles = ChemUtil.fixAromaticNitrogenAndCreateSMILES(tmpMolecule);
        Assertions.assertNotNull(tmpFixedSmiles);
        tmpSmiPar.kekulise(true);
        Assertions.assertDoesNotThrow(() -> tmpSmiPar.parseSmiles(tmpFixedSmiles));
    }
    //
    /**
     * Tests fixing a molecule imported from an aromatic SMILES string that is missing explicit Hs on multiple aromatic N.
     */
    @Test
    void testFixAromaticNitrogensAndCreateSMILES() throws Exception {
        //CHEBI:10048 - two n need to be fixed (without creating an uncharged(!) tetravalent N)
        String tmpSmilesCode = "O=c1nc(=O)c2ncn([C@@H]3O[C@H](COP(=O)(O)OP(=O)(O)O)[C@@H](O)[C@H]3O)c2n1";
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        tmpSmiPar.kekulise(false);
        IAtomContainer tmpMolecule = tmpSmiPar.parseSmiles(tmpSmilesCode);
        String tmpFixedSmiles = ChemUtil.fixAromaticNitrogenAndCreateSMILES(tmpMolecule);
        Assertions.assertNotNull(tmpFixedSmiles);
        tmpSmiPar.kekulise(true);
        Assertions.assertDoesNotThrow(() -> tmpSmiPar.parseSmiles(tmpFixedSmiles));
    }
    //
    /**
     * Tests fixing a molecule imported from an aromatic SMILES string that is missing an explicit H on a charged aromatic N.
     */
    @Test
    void testFixAromaticChargedNitrogenAndCreateSMILES() throws Exception {
        //CHEBI:20794 - one aromatic n is charged and therefore needs to be tetravalent in the solution
        String tmpSmilesCode = "C[n+]1cn([C@@H]2O[C@H](CO)[C@@H](O)[C@H]2O)c2nc(N)nc(=O)c21";
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        tmpSmiPar.kekulise(false);
        IAtomContainer tmpMolecule = tmpSmiPar.parseSmiles(tmpSmilesCode);
        String tmpFixedSmiles = ChemUtil.fixAromaticNitrogenAndCreateSMILES(tmpMolecule);
        Assertions.assertNotNull(tmpFixedSmiles);
        tmpSmiPar.kekulise(true);
        Assertions.assertDoesNotThrow(() -> tmpSmiPar.parseSmiles(tmpFixedSmiles));
        Assertions.assertEquals("C[n+]1cn([C@@H]2O[C@H](CO)[C@@H](O)[C@H]2O)c3[nH]c(N)nc(=O)c31", tmpFixedSmiles);
    }
    //
    /**
     * Tests fixing a molecule imported from an aromatic SMILES string that is missing an explicit H on an aromatic N
     * based on a bulk of ChEBI molecules with this issue.
     */
    @Test
    void testFixAromaticNitrogenAndCreateSMILESBulk() throws Exception {
        String tmpChEBISmiles = """
                CHEBI:10048	O=c1nc(=O)c2ncn([C@@H]3O[C@H](COP(=O)(O)OP(=O)(O)O)[C@@H](O)[C@H]3O)c2n1
                CHEBI:10049	O=c1nc(=O)c2ncn([C@@H]3O[C@H](COP(=O)(O)OP(=O)(O)OP(=O)(O)O)[C@@H](O)[C@H]3O)c2n1
                CHEBI:10110	Cc1cn([C@H]2C[C@H](N=[N+]=[N-])[C@@H](CO)O2)c(=O)nc1=O
                CHEBI:102257	Cc1nn([C@H]2C[C@H](O)[C@@H](COP(=O)(O)O)O2)c(=O)nc1=O
                CHEBI:102485	O=C(O)c1cn([C@H]2C[C@H](O)[C@@H](CO)O2)c(=O)nc1=O
                CHEBI:102517	O=C(O)c1cn([C@H]2C[C@H](O)[C@@H](COP(=O)(O)O)O2)c(=O)nc1=O
                CHEBI:10502	Cc1cn([C@H]2C[C@H](O)[C@@H](COP(=O)(O)OP(=O)(O)O[C@H]3O[C@H](C)C[C@H](N)[C@H]3O)O2)c(=O)nc1=O
                CHEBI:10525	Cc1cn([C@H]2C[C@H](O)[C@@H](COP(=O)(O)OP(=O)(O)O[C@@H]3C[C@@](C)(O)[C@@H](O)[C@H](C)O3)O2)c(=O)nc1=O
                CHEBI:111511	O=c1nc(=O)n([C@H]2C[C@H](O)[C@@H](COP(=O)(O)O)O2)cc1CO[C@@H]1O[C@H](CO)[C@@H](O)[C@H](O)[C@H]1O
                CHEBI:111513	O=c1nc(=O)n([C@H]2C[C@H](O)[C@@H](CO)O2)cc1CO[C@@H]1O[C@H](CO)[C@@H](O)[C@H](O)[C@H]1O
                CHEBI:11515	[H]C(=O)Nc1c(N[C@@H]2O[C@H](COP(=O)(O)O)[C@@H](O)[C@H]2O)nc(N)nc1=O
                CHEBI:115218	O=C(O)CNCc1cn([C@@H]2O[C@H](COP(=O)(O)O)[C@@H](O)[C@H]2O)c(=S)nc1=O
                CHEBI:131188	Nc1ncnc2c([C@@H]3O[C@@H]4COP(=O)(O)O[C@H]4[C@H]3O)nnc12
                CHEBI:131522	CN[C@H]1CC[C@@H](OP(=O)(O)OP(=O)(O)OC[C@H]2O[C@@H](n3cc(C)c(=O)nc3=O)C[C@@H]2O)O[C@@H]1C
                CHEBI:131566	Nc1ncnc2c1ncn2[C@@H]1O[C@H](COP(=O)(O)O)[C@@H](OC(=O)[C@@H](N)Cc2cncn2)[C@H]1O
                CHEBI:131575	Nc1ncnc2c1ncn2[C@@H]1O[C@H](COP(=O)(O)O)[C@@H](OC(=O)[C@@H](N)Cc2cnc3ccccc23)[C@H]1O
                CHEBI:131580	Cc1nn([C@H]2C[C@H](O)[C@@H](CO)O2)c(=O)nc1=O
                CHEBI:131616	Nc1nc(=O)ncc1CO[C@@H]1O[C@H](CO)[C@@H](O)[C@H](O)[C@H]1O
                CHEBI:131828	[H][C@]12O[C@H](C[C@@H]1O)n1c(nc3c(=O)nc(N)nc31)[C@H]2O
                CHEBI:131829	[H][C@]12O[C@H](C[C@@H]1O)n1c(nc3c(=O)nc(N)nc31)[C@H]2OP(=O)(O)O""";
        SmilesParser tmpSmipar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        tmpSmipar.kekulise(false);
        List<String> tmpFailedMols = new ArrayList<>(10);
        for (String tmpLine : tmpChEBISmiles.split("\n")) {
            String tmpFixedSmilesString = ChemUtil.fixAromaticNitrogenAndCreateSMILES(tmpSmipar.parseSmiles(tmpLine.split("\t")[1]));
            if (tmpFixedSmilesString == null) {
                tmpFailedMols.add(tmpLine);
            }
        }
        if (!tmpFailedMols.isEmpty()) {
            StringBuilder failMsg = new StringBuilder("Failed molecules:\n");
            for (String tmpLine : tmpFailedMols) {
                failMsg.append(tmpLine).append('\n');
            }
            Assertions.fail(failMsg.toString());
        }
    }
    //
    /**
     * Tests the generation of molecular formulae on a few examples.
     */
    @Test
    void testMolecularFormulaGeneration() throws Exception {
        String[] tmpSmilesCodes = new String[]{
                "C", // methane
                "CCO", // ethanol
                "c1ccccc1", // benzene
                "CC(=O)O", // acetic acid
                "N[C@@H](Cc1ccccc1)C(=O)O" // phenylalanine
        };
        String[] tmpExpectedFormulas = new String[]{
                "CH4",
                "C2H6O",
                "C6H6",
                "C2H4O2",
                "C9H11NO2"
        };
        for (int i = 0; i < tmpSmilesCodes.length; i++) {
            IAtomContainer tmpMolecule = ChemUtil.parseSmilesToAtomContainer(tmpSmilesCodes[i], true, true);
            String tmpFormula = ChemUtil.generateMolecularFormula(tmpMolecule);
            Assertions.assertEquals(tmpExpectedFormulas[i], tmpFormula);
            // make sure molecular formula generation is independent of hydrogen state
            AtomContainerManipulator.normalizeHydrogens(tmpMolecule, HydrogenState.Explicit);
            tmpFormula = ChemUtil.generateMolecularFormula(tmpMolecule);
            Assertions.assertEquals(tmpExpectedFormulas[i], tmpFormula);
        }
    }
}
