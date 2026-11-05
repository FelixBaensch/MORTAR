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

package de.unijena.cheminf.mortar.model.fragmentation.algorithm;

import org.glycoinfo.MolWURCS.io.WURCSWriter;
import org.glycoinfo.MolWURCS.util.analysis.MoleculeNormalizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.aromaticity.Kekulization;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Test class for {@link MolWURCSFragmenter}, mostly contains experiments and documents corner cases encountered during development.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class MolWURCSFragmenterTest {

    /**
     * Constructor that sets the default locale to British English, which is important for the correct functioning of the
     * fragmenter because the settings tooltips are imported from the message.properties file.
     */
    public MolWURCSFragmenterTest() {
        Locale.setDefault(Locale.of("en", "GB"));
    }

    /**
     * Documents some corner cases of WURCS fragmentation encountered during development.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    void testWURCSCornerCases() throws Exception {
        List<String> tmpStructures = new ArrayList<>(10);
        // all BIOFACQUIM compounds:
        //was not fragmented - SDU would also not detect it because too few oxygen atoms
        // WURCS says "No carbon chain in the molecule."
        String inosine = "C([C@@H]1[C@H]([C@H]([C@H](N2C=NC3=C2N=CNC3=O)O1)O)O)O";
        tmpStructures.add(inosine);
        // was not fragmented
        // WURCS says "No carbon chain in the molecule."
        String Vitexin = "C1=C(C=CC(=C1)O)C2=CC(=O)C3=C(C=C(C(=C3O2)[C@H]4[C@@H]([C@H]([C@@H]([C@@H](CO)O4)O)O)O)O)O";
        tmpStructures.add(Vitexin);
        // was not fragmented
        // no output from WURCS whatsoever, so no sugars detected, apparently -> most likely because of triple-bond!
        String Prunasin = "C1=CC=C(C=C1)[C@H](C#N)O[C@H]2[C@@H]([C@H]([C@@H]([C@@H](CO)O2)O)O)O";
        tmpStructures.add(Prunasin);
        // benzene was extracted along with the sugar moiety
        // WURCS says "A modification is removed as aglycone."
        String CNP0383187_1 = "C1=CC=C(C=C1)C(=O)OC[C@@H]2[C@@H]([C@@H]([C@H]([C@@H](O2)OC3=C(C4=CC=C(C(=C4)O)O)OC5=CC(=CC(=C5C3=O)O)O)O)O)O";
        tmpStructures.add(CNP0383187_1);
        // was not fragmented
        // WURCS says "No carbon chain in the molecule."
        String Isovitexin = "C1=C(C=CC(=C1)O)C2=CC(=O)C3=C(C(=C(C=C3O2)O)[C@H]4[C@@H]([C@H]([C@@H]([C@@H](CO)O4)O)O)O)O";
        tmpStructures.add(Isovitexin);
        // shows that non-terminal sugars are extracted as well
        String CNP0080325_1 = "CCCCCC(CCCCCCCCCC(=O)O)O[C@H]1[C@@H]([C@H]([C@@H]([C@@H](C)O1)O)O)O[C@@H]2C[C@H](CO)[C@@H](C)[C@@H]([C@H]2O[C@H]3[C@@H]([C@@H]([C@H]([C@H](C)O3)O[C@H]4[C@@H]([C@H]([C@@H]([C@@H](C)O4)O)O)O)O)O)O";
        tmpStructures.add(CNP0080325_1);
        // was not fragmented
        // "Error in WURCS conversion - java.lang.NullPointerException: Cannot read field "x" because "t1" is null"
        // -> this issue is fixed now!
        String Pestalotin_4_O_methyl_beta_mannopyranoside = "CCCC[C@@H]([C@@H]1CC(=CC(=O)O1)OC)O[C@H]2[C@H]([C@H]([C@@H]([C@@H](CO)O2)OC)O)O";
        tmpStructures.add(Pestalotin_4_O_methyl_beta_mannopyranoside);
        // sugar contains fatty acid
        String PESCAPROSIDE_A = "CCCCCCCCCCCC(=O)O[C@@H]1[C@@H]([C@H]([C@H](C)O[C@H]1O[C@H]2[C@H](C)O[C@H]([C@@H]([C@@H]2O)O)O[C@@H]3[C@H]([C@H]([C@@H](C)O[C@H]3O[C@@H](CCCCC)CCCCCCCCCC(=O)OC)O)O)O[C@H]4[C@@H]([C@@H]([C@H]([C@H](C)O4)O)O)O)O[C@H]5[C@@H]([C@@H]([C@H]([C@H](C)O5)O)O)O";
        tmpStructures.add(PESCAPROSIDE_A);
        // sugar contains macrocycle structure - would not be detectable by SDU!
        String stoloniferinIII = "CCCCCCCCCC(=O)O[C@@H]1[C@@H]([C@H]([C@H](C)O[C@H]1O[C@H]2[C@H](C)O[C@@H]3[C@@H]([C@@H]2OC(=O)CCCCCCCCC[C@H](CCCCC)O[C@H]4[C@@H]([C@H]([C@H]([C@@H](C)O4)O)O)O3)O)O[C@H]5[C@@H]([C@@H]([C@H]([C@H](C)O5)OC(=O)[C@@H](C)CC)O)O)O[C@H]6[C@@H]([C@@H]([C@H]([C@H](C)O6)O)O)O";
        tmpStructures.add(stoloniferinIII);
        // see above
        String PurginosideI = "CCCCCCCCCC(=O)O[C@@H]1[C@@H]([C@H]([C@H](C)O[C@H]1O[C@H]2[C@H](C)O[C@@H]3[C@@H]([C@@H]2O)OC(=O)CCCCCCCCC[C@H](CCCCC)O[C@H]4[C@@H]([C@H]([C@H]([C@@H](C)O4)O)O)O3)O[C@H]5[C@@H]([C@@H]([C@H]([C@H](C)O5)OC(=O)[C@@H](C)CC)O)OC(=O)/C=C/C6=CC=CC=C6)O[C@H]7[C@@H]([C@H]([C@@H]([C@@H](CO)O7)O)O)O";
        tmpStructures.add(PurginosideI);
        // see above
        String PurginosideII = "CCCCCCCCCC(=O)O[C@@H]1[C@@H]([C@H]([C@H](C)O[C@H]1O[C@H]2[C@H](C)O[C@@H]3[C@@H]([C@@H]2O)OC(=O)CCCCCCCCC[C@H](CCCCC)O[C@H]4[C@@H]([C@H]([C@H]([C@@H](C)O4)O)O)O3)O[C@H]5[C@@H]([C@@H]([C@H]([C@H](C)O5)OC(=O)CCCCC)OC(=O)/C=C/C6=CC=CC=C6)O)O[C@H]7[C@@H]([C@H]([C@@H]([C@@H](CO)O7)O)O)O";
        tmpStructures.add(PurginosideII);
        // see above
        String purginI = "CCCCCCCCCCCC(=O)O[C@@H]1[C@@H]([C@H]([C@H](C)O[C@H]1O[C@H]2[C@H](C)O[C@H]([C@@H]([C@@H]2O)O)O[C@@H]3[C@H]([C@H]([C@@H](C)O[C@H]3O[C@@H](CCCCC)CCCCCCCCCC(=O)O[C@@H]4[C@@H](CO)O[C@H]([C@@H]([C@H]4O)O)O[C@@H]5[C@H]([C@H](C)O[C@H]([C@@H]5OC(=O)CCCCCCCCCCC)O[C@H]6[C@H](C)O[C@@H]7[C@@H]([C@@H]6O)OC(=O)CCCCCCCCC[C@H](CCCCC)O[C@H]8[C@@H]([C@H]([C@H]([C@@H](C)O8)O)O)O7)O[C@H]9[C@@H]([C@@H]([C@H]([C@H](C)O9)OC(=O)[C@@H](C)CC)O)OC(=O)/C=C/C%10=CC=CC=C%10)O)O)O[C@H]%11[C@@H]([C@@H]([C@H]([C@H](C)O%11)OC(=O)[C@@H](C)CC)OC(=O)/C=C/C%12=CC=CC=C%12)O)O[C@H]%13[C@@H]([C@H]([C@@H]([C@@H](CO)O%13)O)O)O";
        tmpStructures.add(purginI);
        // one sugar moiety is missed
        // WURCS says "A modification is removed as aglycone."
        String CNP0608644_1 = "CCC[C@H](CCCCCCC[C@H](CC(=O)O)[C@@H]1[C@@H]([C@H]([C@@H]([C@@H](C)O1)O)O)O)O[C@H]2[C@@H]([C@H]([C@@H]([C@@H](C)O2)O)O)O[C@H]3[C@@H]([C@H]([C@@H]([C@@H](CO)O3)O)O)O[C@H]4[C@@H]([C@@H]([C@H]([C@H](C)O4)O[C@H]5[C@@H]([C@H]([C@H]([C@@H](C)O5)O)O)O)O[C@@H]6[C@H]([C@@H]([C@H]([C@H](CO)O6)O)O)O[C@@H]7[C@H]([C@@H]([C@H]([C@H](C)O7)O)O)O)O";
        tmpStructures.add(CNP0608644_1);
        //CNP0260701.1 - COUMERMYCIN SODIUM
        // generated WURCS cannot be retranslated - kekulization error
        //same for Coumamycin, coumermycin a1, and CNP0260701.4 (which is also a coumermycin variant)
        // also for CNP0363492.1, CNP0605337.1, CNP0497497.1, CNP0346170.2, CNP0170667.1, CNP0351010.0 (Vanillobiocin), CNP0101190.1 (N-(1-Deoxy-1-fructosyl)histidine)
        // -> Pyrrole seems to be the problem here
//        String CoumerMycinSodium = "CO[C@@H]1[C@@H](OC(=O)C2=CC=C(C)N2)[C@@H](O)[C@@H](OC2=CC=C3C(O)=C(NC(=O)C4=CNC(C(=O)NC5=C(O)C6=CC=C(O[C@H]7OC(C)(C)[C@H](OC)[C@@H](OC(=O)C8=CC=C(C)N8)[C@H]7O)C(C)=C6OC5=O)=C4C)C(=O)OC3=C2C)OC1(C)C";
//        tmpStructures.add(CoumerMycinSodium);
//        //CNP0170667.1 - icas#9
//        // generated WURCS cannot be retranslated - kekulization error
//        String icas9 = "C[C@H](CCC(=O)O)O[C@@H]1O[C@@H](C)[C@H](OC(=O)C2=CNC3=CC=CC=C23)C[C@H]1O";
//        tmpStructures.add(icas9);
//        //CNP0363492.1 - Clorobiocin
//        // generated WURCS cannot be retranslated - kekulization error
//        String clorobiocin = "CO[C@@H]1[C@@H](OC(=O)C2=CC=C(C)N2)[C@@H](O)[C@H](OC2=CC=C3C(O)=C(NC(=O)C4=CC=C(O)C(CC=C(C)C)=C4)C(=O)OC3=C2Cl)OC1(C)C";
//        tmpStructures.add(clorobiocin);
//        //CNP0101819.1 - isoscutellarein 7-O-beta-xylopyranoside-2''-O-sulphate
//        //WURCS says: "An atom can not be typed by charge restoration."
//        String isoscutellareinSulphate = "O=C1C=C(C2=CC=C(O)C=C2)OC2=C(O)C(O[C@@H]3OC[C@H](O)[C@@H](O)[C@@H]3O[SH](=O)=O)=CC(O)=C12";
//        tmpStructures.add(isoscutellareinSulphate);
        //CNP0432972.1 - isoscutellarein 7-O-β-xyloside-2′′-O-sulfate
        // correct form of isoscutellarein sulphate above -> this works, so the problem with the above structure should not be put on WURCS
        String CNP0432972_1 = "O=C1C=C(C2=CC=C(O)C=C2)OC2=C(O)C(O[C@@H]3OC[C@@H](O)[C@H](O)[C@H]3OS(=O)(=O)O)=CC(O)=C12";
        tmpStructures.add(CNP0432972_1);

        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        SmilesGenerator tmpSmiGen = new SmilesGenerator(SmiFlavor.Absolute | SmiFlavor.UseAromaticSymbols);
        MolWURCSFragmenter tmpMolWURCSFragmenter = new MolWURCSFragmenter();
        for (String tmpSmi : tmpStructures) {
            IAtomContainer tmpMol = tmpSmiPar.parseSmiles(tmpSmi);
            List<IAtomContainer> tmpFragments;
            if (tmpMolWURCSFragmenter.canBeFragmented(tmpMol)) {
                tmpFragments = tmpMolWURCSFragmenter.fragmentMolecule(tmpMol);
            } else {
                continue;
            }
            for (IAtomContainer tmpFrag : tmpFragments) {
                String fragSmiles = tmpSmiGen.create(tmpFrag);
                System.out.println("Fragment: " + fragSmiles);
            }
        }
    }

    /**
     * Basic example of WURCS generation from a SMILES string.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    void basicWURCSGenerationExample() throws Exception {
        //CNP0201883.5
        //1-Caffeoyl-beta-D-glucose
        String smiles = "O=C(/C=C/C1=CC=C(O)C(O)=C1)O[C@@H]1OC(CO)[C@@H](O)[C@H](O)C1O";
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpMol = tmpSmiPar.parseSmiles(smiles);
        StringWriter tmpStringWriter = new StringWriter();
        WURCSWriter tmpWURCSWriter = new WURCSWriter(tmpStringWriter);
        tmpWURCSWriter.setDoDoubleCheck(true);
        tmpWURCSWriter.writeAtomContainer(tmpMol);
        String tmpWURCSString = tmpStringWriter.toString();
        System.out.println("WURCS: " + tmpWURCSString);
    }

    /**
     * Documents a kekulization problem encountered during development. Show that WURC's problem with aromatic
     * N hetero cycles apparently does not result from the preprocessing (normalization).
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    void testKekulizationProblem() throws Exception {
        //CHEBI:79028 - icas#9
        String smiles = "C[C@H](CCC(=O)O)O[C@@H]1O[C@@H](C)[C@H](OC(=O)c2cnc3ccccc23)C[C@H]1O";
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        tmpSmiPar.kekulise(false);
        IAtomContainer tmpMol = tmpSmiPar.parseSmiles(smiles);
        //this will throw a Kekulization exception because the aromatic n in the SMILES code needs to have an explicit hydrogen atom
        IAtomContainer finalTmpMol = tmpMol;
        Assertions.assertThrows(CDKException.class, () -> Kekulization.kekulize(finalTmpMol));
        //CNP0170667.1 - icas#9
        smiles = "C[C@H](CCC(=O)O)O[C@@H]1O[C@@H](C)[C@H](OC(=O)C2=CNC3=CC=CC=C23)C[C@H]1O";
        tmpMol = tmpSmiPar.parseSmiles(smiles);
        MoleculeNormalizer norm = new MoleculeNormalizer();
        norm.setDoAromatize(true);
        norm.normalize(tmpMol);
        //this works because the newly created SMILES code with aromaticity encoding has the hydrogen on the n
        Kekulization.kekulize(tmpMol);
        SmilesGenerator smiGen = new SmilesGenerator(SmiFlavor.Absolute | SmiFlavor.UseAromaticSymbols);
        System.out.println(smiGen.create(tmpMol));
    }
}
