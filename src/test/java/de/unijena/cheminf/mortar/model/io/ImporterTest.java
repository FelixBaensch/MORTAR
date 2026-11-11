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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    /**
     * Test importing an SD file with three entries and testing whether the import progress is reported for every
     * import phase and finally reaches the total number of entries in each phase. The total is unknown while counting.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testImportProgressReachesTotalInEachPhaseForSDFile() throws Exception {
        URL tmpURL = this.getClass().getResource("ChEBI_three_entries.sdf");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        Importer tmpImporter = new Importer(new SettingsContainer());
        Map<Importer.ImportPhase, int[]> tmpLastProgressMap = new EnumMap<>(Importer.ImportPhase.class);
        tmpImporter.setProgressListener((aPhase, aProcessedCount, aTotalCount) ->
                tmpLastProgressMap.put(aPhase, new int[]{aProcessedCount, aTotalCount}));
        tmpImporter.importMoleculeFile(tmpResourceFile, false, true, false);
        Assertions.assertArrayEquals(new int[]{3, -1}, tmpLastProgressMap.get(Importer.ImportPhase.COUNTING));
        Assertions.assertArrayEquals(new int[]{3, 3}, tmpLastProgressMap.get(Importer.ImportPhase.READING));
        Assertions.assertArrayEquals(new int[]{3, 3}, tmpLastProgressMap.get(Importer.ImportPhase.PREPROCESSING));
        Assertions.assertArrayEquals(new int[]{3, 3}, tmpLastProgressMap.get(Importer.ImportPhase.SMILES_GENERATION));
    }
    /**
     * Test importing an SD file whose last entry is not closed by the record delimiter "$$$$" and testing whether the
     * reported reading progress never exceeds the reported total, although counting the delimiters yields one entry less.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testReadingProgressStaysWithinTotalForSDFileWithoutFinalDelimiter() throws Exception {
        URL tmpURL = this.getClass().getResource("ChEBI_three_entries_no_final_delimiter.sdf");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        Importer tmpImporter = new Importer(new SettingsContainer());
        List<int[]> tmpReadingProgressList = new ArrayList<>();
        tmpImporter.setProgressListener((aPhase, aProcessedCount, aTotalCount) -> {
            if (aPhase == Importer.ImportPhase.READING) {
                tmpReadingProgressList.add(new int[]{aProcessedCount, aTotalCount});
            }
        });
        tmpImporter.importMoleculeFile(tmpResourceFile, false, true, false);
        Assertions.assertFalse(tmpReadingProgressList.isEmpty());
        for (int[] tmpProgress : tmpReadingProgressList) {
            Assertions.assertTrue(tmpProgress[0] <= tmpProgress[1],
                    String.format("Reported progress %d exceeds total %d", tmpProgress[0], tmpProgress[1]));
        }
        Assertions.assertArrayEquals(new int[]{3, 3}, tmpReadingProgressList.getLast());
    }
    /**
     * Test importing an SD file with default settings and testing the names and unique SMILES codes of the resulting
     * data models, including a generic structure with pseudo atoms.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testImportedNamesAndSmilesOfSDFile() throws Exception {
        List<MoleculeDataModel> tmpMolecules = this.importTestResource("ChEBI_three_entries.sdf");
        Assertions.assertEquals(List.of("(-)-epicatechin", "(1S,4R)-fenchone", "1-alkyl-2-acylglycerol"),
                tmpMolecules.stream().map(MoleculeDataModel::getName).toList());
        Assertions.assertEquals(List.of(
                "C1=CC(=C(C=C1[C@@H]2[C@@H](CC3=C(C=C(C=C3O2)O)O)O)O)O",
                "CC1(C)[C@@H]2CC[C@@](C)(C2)C1=O",
                "*OCC(OC(*)=O)CO"),
                tmpMolecules.stream().map(MoleculeDataModel::getUniqueSmiles).toList());
    }
    /**
     * Test importing a SMILES file with a headline, empty lines, and no name column with default settings and testing
     * the names (file name plus line index) and unique SMILES codes of the resulting data models.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testImportedNamesAndSmilesOfSMILESFileWithEmptyLines() throws Exception {
        List<MoleculeDataModel> tmpMolecules = this.importTestResource("SMILESTestFileOne.txt");
        Assertions.assertEquals(List.of("SMILESTestFileOne1", "SMILESTestFileOne3", "SMILESTestFileOne5"),
                tmpMolecules.stream().map(MoleculeDataModel::getName).toList());
        Assertions.assertEquals(List.of(
                "O=C1OC2=CC(=CC(OC3OC(CO)C(O)C(O)C3O)=C2C4=C1CCC4)C",
                "O=C1C=C(OC=2C1=C(O)C=C(O)C2C(C=3C=CC=CC3)CC(=O)OC)C=4C=CC=CC4",
                "O=C(NC1OCOC2C1OC(CC(OC)CO)C(C)(C)C2OC)C(O)C3(OC)OC(C)C(C(=C)C3)C"),
                tmpMolecules.stream().map(MoleculeDataModel::getUniqueSmiles).toList());
    }
    /**
     * Test importing a SMILES file with a headline, a name column, and invalid SMILES codes with default settings and
     * testing that only the valid lines result in data models with the given names.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testImportedNamesAndSmilesOfSMILESFileWithInvalidLines() throws Exception {
        List<MoleculeDataModel> tmpMolecules = this.importTestResource("SMILESTestFileThree.txt");
        Assertions.assertEquals(List.of("tert-Butanol", "Istanbulin A", "Valdiazen"),
                tmpMolecules.stream().map(MoleculeDataModel::getName).toList());
        Assertions.assertEquals(List.of(
                "OC(C)(C)C",
                "O=C1OC2(O)C(=C1C)CC3(C)C(C(=O)CCC3C)C2",
                "[O-][N+](=NO)C(CO)C(C)C"),
                tmpMolecules.stream().map(MoleculeDataModel::getUniqueSmiles).toList());
    }
    /**
     * Test importing a MOL file with default settings and testing the name and unique SMILES code of the resulting
     * data model.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testImportedNameAndSmilesOfMOLFile() throws Exception {
        List<MoleculeDataModel> tmpMolecules = this.importTestResource("Mirabilin_B.mol");
        Assertions.assertEquals(1, tmpMolecules.size());
        Assertions.assertEquals("Mirabilin B", tmpMolecules.getFirst().getName());
        Assertions.assertEquals("N=C1N=C2C3=C(N1)CCC3CC(C)C2CCCC", tmpMolecules.getFirst().getUniqueSmiles());
    }
    //</editor-fold>
    //
    // <editor-fold desc="private methods">
    /**
     * Imports the given test resource file with the default import settings, i.e. regarding stereochemistry, filling
     * open valences with implicit hydrogen atoms, and not enforcing kekulization.
     *
     * @param aResourceName name of the test resource file in the package of this class
     * @return the imported molecule data models
     * @throws Exception if anything goes wrong
     */
    private List<MoleculeDataModel> importTestResource(String aResourceName) throws Exception {
        URL tmpURL = this.getClass().getResource(aResourceName);
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        return new Importer(new SettingsContainer()).importMoleculeFile(tmpResourceFile, true, true, false);
    }
    //</editor-fold>
}
