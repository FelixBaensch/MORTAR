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

package de.unijena.cheminf.mortar.model.io;

import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import de.unijena.cheminf.mortar.model.util.MiscUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

/**
 * Tests some functionalities of the {@link Importer} class.
 *
 * @author Jonas Schaub
 */
public class ImporterTest extends Importer {
    //<editor-fold desc="static initializer">
    /**
     * Sets the default locale to British English.
     */
    static {
        Locale.setDefault(Locale.of("en", "GB"));
    }
    //</editor-fold>
    //
    // <editor-fold desc="constructor">
    /**
     * Default constructor for the ImporterTest class.
     */
    public ImporterTest() {
        super(new SettingsContainer());
    }
    //</editor-fold>
    //
    // <editor-fold desc="test methods">
    /**
     * Tests that imported molecules are assigned IDs consisting of the file name and a consecutive number that counts
     * only the molecules that were actually imported, so that structures that could not be imported leave no gaps.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testMoleculeIDs() throws Exception {
        URL tmpURL = this.getClass().getResource("SMILESTestFileThree.txt");
        File tmpFile = Paths.get(tmpURL.toURI()).toFile();
        List<MoleculeDataModel> tmpMolecules = this.importMoleculeFile(tmpFile, false, false, false);
        //the first structure in the file is invalid and must not consume an ID
        Assertions.assertEquals("tert-Butanol", tmpMolecules.getFirst().getName());
        Assertions.assertEquals("SMILESTestFileThree_1", tmpMolecules.getFirst().getId());
        Assertions.assertEquals("SMILESTestFileThree_2", tmpMolecules.get(1).getId());
        Assertions.assertFalse(this.isIDPrefixTruncated());
        tmpURL = this.getClass().getResource("Mirabilin_B.mol");
        tmpFile = Paths.get(tmpURL.toURI()).toFile();
        tmpMolecules = this.importMoleculeFile(tmpFile, false, false, false);
        Assertions.assertEquals("Mirabilin_B_1", tmpMolecules.getFirst().getId());
    }
    //
    /**
     * Tests that empty lines in a SMILES file do not lead to gaps in the molecule IDs.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testMoleculeIDsWithEmptyLines() throws Exception {
        URL tmpURL = this.getClass().getResource("SMILESTestFileOne.txt");
        File tmpFile = Paths.get(tmpURL.toURI()).toFile();
        List<MoleculeDataModel> tmpMolecules = this.importMoleculeFile(tmpFile, false, false, false);
        Assertions.assertTrue(tmpMolecules.size() > 1);
        for (int i = 0; i < tmpMolecules.size(); i++) {
            Assertions.assertEquals("SMILESTestFileOne_" + (i + 1), tmpMolecules.get(i).getId());
        }
    }
    //
    /**
     * Tests that several faulty records in a row in an SD file do not lead to gaps in the molecule IDs.
     *
     * @param aTempDir temporary directory provided by JUnit
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testMoleculeIDsWithFaultySDFRecords(@TempDir Path aTempDir) throws Exception {
        String tmpMethane = """
                Methane
                  test

                  1  0  0  0  0  0  0  0  0  0999 V2000
                    0.0000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0
                M  END
                $$$$
                """;
        String tmpEthane = """
                Ethane
                  test

                  2  1  0  0  0  0  0  0  0  0999 V2000
                    0.0000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0
                    1.5000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0
                  1  2  1  0  0  0  0
                M  END
                $$$$
                """;
        String tmpFaulty = """
                Faulty
                  test

                this is not a counts line
                M  END
                $$$$
                """;
        File tmpFile = aTempDir.resolve("Faulty_Records.sdf").toFile();
        Files.writeString(tmpFile.toPath(), tmpMethane + tmpFaulty + tmpFaulty + tmpEthane);
        List<MoleculeDataModel> tmpMolecules = this.importMoleculeFile(tmpFile, false, false, false);
        Assertions.assertEquals(2, tmpMolecules.size());
        Assertions.assertEquals("Faulty_Records_1", tmpMolecules.get(0).getId());
        Assertions.assertEquals("Ethane", tmpMolecules.get(1).getName());
        Assertions.assertEquals("Faulty_Records_2", tmpMolecules.get(1).getId());
    }
    //
    /**
     * Tests that long file names are truncated for the molecule IDs and that the truncation flag is reset at the next
     * import, also if that import is aborted because of an unsupported file type.
     *
     * @param aTempDir temporary directory provided by JUnit
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testIDPrefixTruncation(@TempDir Path aTempDir) throws Exception {
        String tmpLongName = "A_very_long_file_name_that_exceeds_the_maximum_length_of_ID_prefixes";
        File tmpFile = aTempDir.resolve(tmpLongName + ".smi").toFile();
        Files.writeString(tmpFile.toPath(), "CCO ethanol\nCCN ethylamine\n");
        List<MoleculeDataModel> tmpMolecules = this.importMoleculeFile(tmpFile, false, false, false);
        Assertions.assertTrue(this.isIDPrefixTruncated());
        String tmpExpectedPrefix = MiscUtil.truncateIDPrefix(tmpLongName, MiscUtil.ID_PREFIX_MAX_LENGTH);
        Assertions.assertTrue(tmpExpectedPrefix.length() <= MiscUtil.ID_PREFIX_MAX_LENGTH);
        Assertions.assertEquals(tmpExpectedPrefix + "_1", tmpMolecules.getFirst().getId());
        File tmpUnsupportedFile = aTempDir.resolve("Unsupported.xyz").toFile();
        Files.writeString(tmpUnsupportedFile.toPath(), "CCO");
        Assertions.assertNull(this.importMoleculeFile(tmpUnsupportedFile, false, false, false));
        Assertions.assertFalse(this.isIDPrefixTruncated());
    }
    //
    /**
     * Test importing a MOL file containing a molecules with radicals and testing whether these are fixed correctly.
     * This is basically an integration test for the ChemUtil functionality used internally by the Importer.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testHydrogenSaturationOnMOLfile() throws Exception {
        URL tmpURL = this.getClass().getResource("Mirabilin_B.mol");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        MDLV2000Reader tmpReader = new MDLV2000Reader(new FileReader(tmpResourceFile));
        IAtomContainer tmpMolecule = tmpReader.read(SilentChemObjectBuilder.getInstance().newAtomContainer());
        tmpReader.close();
        IAtomContainerSet tmpSet = new AtomContainerSet();
        tmpSet.addAtomContainer(tmpMolecule);
        this.preprocessMoleculeSet(tmpSet, true);
        SmilesGenerator smiGen = new SmilesGenerator(SmiFlavor.Canonical);
        Assertions.assertEquals("N=C1N=C2C3=C(N1)CCC3CC(C)C2CCCC", smiGen.create(tmpMolecule));
    }
    /**
     * Test importing a SMILES string containing a molecule with some explicit incomplete valences and testing whether
     * these are fixed correctly.
     * This is NOT an integration test for underlying ChemUtil functionalities because these explicit valences do not
     * create single electrons in the generated atom container. Hence, the importer method that saturates these "simple"
     * open valences is tested directly.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testHydrogenSaturationOnSMILES() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpMolecule = tmpSmiPar.parseSmiles("[CH2]CCCC([CH])CCC");
        IAtomContainerSet tmpSet = new AtomContainerSet();
        tmpSet.addAtomContainer(tmpMolecule);
        this.preprocessMoleculeSet(tmpSet, true);
        SmilesGenerator smiGen = new SmilesGenerator(SmiFlavor.Canonical);
        Assertions.assertEquals("CCCCC(C)CCC", smiGen.create(tmpMolecule));
    }
    //</editor-fold>
}
