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
    //</editor-fold>
}
