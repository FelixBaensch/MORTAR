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

import de.unijena.cheminf.mortar.model.util.ChemUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Test class for the DynamicSMILESFileReader class.
 *
 * @author Samuel Behr
 * @author Jonas Schaub
 * @version 2.0.0.0
 */
public class DynamicSMILESFileReaderTest {
    //<editor-fold desc="static initializer">
    /**
     * Sets the default locale to British English.
     */
    static {
        Locale.setDefault(Locale.of("en", "GB"));
    }
    //</editor-fold>
    //
    /**
     * Test containsOnlySMILESValidCharacters() for false-positives, e.g. two tab-separated strings, some of which can
     * be interpreted by the CDK SmilesParser (it only parses the first part up to the first whitespace character and
     * does not throw an error but interprets the rest as title of the structure).
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void containsOnlySMILESValidCharactersTest() throws Exception {
        Assertions.assertFalse(DynamicSMILESFileReader.containsOnlySMILESValidCharacters("CCCCOCCC\tlfdsklhfdfvdbgvb"));
        Assertions.assertFalse(DynamicSMILESFileReader.containsOnlySMILESValidCharacters("CCCCOCCC lfdsklhfdfvdbgvb"));
        Assertions.assertFalse(DynamicSMILESFileReader.containsOnlySMILESValidCharacters(""));
        Assertions.assertFalse(DynamicSMILESFileReader.containsOnlySMILESValidCharacters("\t"));
        for (String tmpSeparator : DynamicSMILESFileReader.POSSIBLE_SMILES_FILE_SEPARATORS) {
            Assertions.assertFalse(DynamicSMILESFileReader.containsOnlySMILESValidCharacters(tmpSeparator), "was true for " + tmpSeparator);
        }
    }
    //
    /**
     * Test file's specifications:
     * - .txt file
     * - with headline
     * - SMILES code column only (no ID or name)
     * - including some blank lines between
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void smilesFormatDetectionOnOneColumnFileWithBlankLinesAndHeadlineTest() throws Exception {
        URL tmpURL = this.getClass().getResource("SMILESTestFileOne.txt");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        DynamicSMILESFileFormat tmpFormat = DynamicSMILESFileReader.detectFormat(tmpResourceFile);
        Assertions.assertTrue(tmpFormat.hasHeaderLine());
        Assertions.assertEquals(0, tmpFormat.getSMILESCodeColumnPosition());
        Assertions.assertFalse(tmpFormat.hasIDColumn());
        Assertions.assertEquals(DynamicSMILESFileFormat.PLACEHOLDER_SEPARATOR_CHAR, tmpFormat.getSeparatorChar());
    }
    //
    /**
     * Test file's specifications:
     * - .txt file
     * - with headline
     * - SMILES code column only (no ID or name)
     * - including some blank lines between
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void smilesFileImportOnOneColumnFileWithBlankLinesAndHeadlineTest() throws Exception {
        URL tmpURL = this.getClass().getResource("SMILESTestFileOne.txt");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        DynamicSMILESFileFormat tmpFormat = DynamicSMILESFileReader.detectFormat(tmpResourceFile);
        DynamicSMILESFileReader tmpReader = new DynamicSMILESFileReader();
        IAtomContainerSet tmpMolSet = tmpReader.readFile(tmpResourceFile, tmpFormat);
        Assertions.assertEquals(3, tmpMolSet.getAtomContainerCount());
        Assertions.assertEquals("SMILESTestFileOne1", tmpMolSet.getAtomContainer(0).getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
        Assertions.assertEquals("SMILESTestFileOne5", tmpMolSet.getAtomContainer(2).getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
        Assertions.assertEquals(2, tmpReader.getSkippedLinesCounter());
    }
    //
    /**
     * Test file's specifications:
     * - .smi file
     * - no headline
     * - ID first in line
     * - used separator: "\t"
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void smilesFormatDetectionOnTwoColumnFileTabSeparatedAndNoHeadlineTest() throws Exception {
        URL tmpURL = this.getClass().getResource("SMILESTestFileTwo.smi");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        DynamicSMILESFileFormat tmpFormat = DynamicSMILESFileReader.detectFormat(tmpResourceFile);
        Assertions.assertFalse(tmpFormat.hasHeaderLine());
        Assertions.assertEquals(0, tmpFormat.getIDColumnPosition());
        Assertions.assertEquals(1, tmpFormat.getSMILESCodeColumnPosition());
        Assertions.assertTrue(tmpFormat.hasIDColumn());
        Assertions.assertEquals('\t', tmpFormat.getSeparatorChar());
    }
    //
    /**
     * Test file's specifications:
     * - .smi file
     * - no headline
     * - ID first in line
     * - used separator: "\t"
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void smilesFileImportOnTwoColumnFileTabSeparatedAndNoHeadlineTest() throws Exception {
        URL tmpURL = this.getClass().getResource("SMILESTestFileTwo.smi");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        DynamicSMILESFileFormat tmpFormat = DynamicSMILESFileReader.detectFormat(tmpResourceFile);
        DynamicSMILESFileReader tmpReader = new DynamicSMILESFileReader();
        IAtomContainerSet tmpMolSet = tmpReader.readFile(tmpResourceFile, tmpFormat);
        Assertions.assertEquals(5, tmpMolSet.getAtomContainerCount());
        Assertions.assertEquals("CNP0337481", tmpMolSet.getAtomContainer(4).getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
        Assertions.assertEquals(0, tmpReader.getSkippedLinesCounter());
    }
    //
    /**
     * Test file's specifications:
     * - Headline
     * - "NAME" second in line and containing spaces
     * - used separator: ";"
     * - two lines with invalid SMILES code
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void smilesFormatDetectionOnTwoColumnFileSemicolonSeparatedWithHeadlineTwoInvalidLinesTest() throws Exception {
        URL tmpURL = this.getClass().getResource("SMILESTestFileThree.txt");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        DynamicSMILESFileFormat tmpFormat = DynamicSMILESFileReader.detectFormat(tmpResourceFile);
        Assertions.assertTrue(tmpFormat.hasHeaderLine());
        Assertions.assertEquals(1, tmpFormat.getIDColumnPosition());
        Assertions.assertEquals(0, tmpFormat.getSMILESCodeColumnPosition());
        Assertions.assertTrue(tmpFormat.hasIDColumn());
        Assertions.assertEquals(';', tmpFormat.getSeparatorChar());
    }
    //
    /**
     * Test file's specifications:
     * - Headline
     * - "NAME" second in line and containing spaces
     * - used separator: ";"
     * - two lines with invalid SMILES code
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void smilesFileImportOnTwoColumnFileSemicolonSeparatedWithHeadlineTwoInvalidLinesTest() throws Exception {
        URL tmpURL = this.getClass().getResource("SMILESTestFileThree.txt");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        DynamicSMILESFileFormat tmpFormat = DynamicSMILESFileReader.detectFormat(tmpResourceFile);
        DynamicSMILESFileReader tmpReader = new DynamicSMILESFileReader();
        IAtomContainerSet tmpMolSet = tmpReader.readFile(tmpResourceFile, tmpFormat);
        Assertions.assertEquals(3, tmpMolSet.getAtomContainerCount());
        Assertions.assertEquals("Istanbulin A", tmpMolSet.getAtomContainer(1).getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
        Assertions.assertEquals("Valdiazen", tmpMolSet.getAtomContainer(2).getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
        Assertions.assertEquals(2, tmpReader.getSkippedLinesCounter());
    }
    //
    /**
     * Test file's specifications:
     * - one single line only
     * - ID first in line
     * - used separator: " "
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void smilesFormatDetectionOnTwoColumnFileSpaceSeparatedWithOnlyOneLineTest() throws Exception {
        URL tmpURL = this.getClass().getResource("SMILESTestFileFour.txt");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        DynamicSMILESFileFormat tmpFormat = DynamicSMILESFileReader.detectFormat(tmpResourceFile);
        Assertions.assertFalse(tmpFormat.hasHeaderLine());
        Assertions.assertEquals(0, tmpFormat.getIDColumnPosition());
        Assertions.assertEquals(1, tmpFormat.getSMILESCodeColumnPosition());
        Assertions.assertTrue(tmpFormat.hasIDColumn());
        Assertions.assertEquals(' ', tmpFormat.getSeparatorChar());
    }
    //
    /**
     * Test file's specifications:
     * - one single line only
     * - ID first in line
     * - used separator: " "
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void smilesFileImportOnTwoColumnFileSpaceSeparatedWithOnlyOneLineTest() throws Exception {
        URL tmpURL = this.getClass().getResource("SMILESTestFileFour.txt");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        DynamicSMILESFileFormat tmpFormat = DynamicSMILESFileReader.detectFormat(tmpResourceFile);
        DynamicSMILESFileReader tmpReader = new DynamicSMILESFileReader();
        IAtomContainerSet tmpMolSet = tmpReader.readFile(tmpResourceFile, tmpFormat);
        Assertions.assertEquals(1, tmpMolSet.getAtomContainerCount());
        Assertions.assertEquals("CNP0356547", tmpMolSet.getAtomContainer(0).getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
        Assertions.assertEquals(0, tmpReader.getSkippedLinesCounter());
    }
    //
    /**
     * Test file's specifications:
     * - headline and blank line first
     * - three elements per line
     * - SMILES first in line, ID second and a neglectable third element
     * - third element in line
     * - used separator: "\t"
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void smilesFormatDetectionOnThreeColumnFileWithHeadlineTabSeparatedTest() throws Exception {
        URL tmpURL = this.getClass().getResource("SMILESTestFileFive.txt");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        DynamicSMILESFileFormat tmpFormat = DynamicSMILESFileReader.detectFormat(tmpResourceFile);
        Assertions.assertTrue(tmpFormat.hasHeaderLine());
        Assertions.assertEquals(1, tmpFormat.getIDColumnPosition());
        Assertions.assertEquals(0, tmpFormat.getSMILESCodeColumnPosition());
        Assertions.assertTrue(tmpFormat.hasIDColumn());
        Assertions.assertEquals('\t', tmpFormat.getSeparatorChar());
    }
    //
    /**
     * Test file's specifications:
     * - headline and blank line first
     * - three elements per line
     * - SMILES first in line, ID second and a neglectable third element
     * - third element in line
     * - used separator: "\t"
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void smilesFileImportOnThreeColumnFileWithHeadlineTabSeparatedTest() throws Exception {
        URL tmpURL = this.getClass().getResource("SMILESTestFileFive.txt");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        DynamicSMILESFileFormat tmpFormat = DynamicSMILESFileReader.detectFormat(tmpResourceFile);
        DynamicSMILESFileReader tmpReader = new DynamicSMILESFileReader();
        IAtomContainerSet tmpMolSet = tmpReader.readFile(tmpResourceFile, tmpFormat);
        String[] tmpTestFileFiveSmiles = new String[] {"OC=1C=C(O)C=C(C1)C=2OC=3C=CC=CC3C2", "OC=1C=C(O)C(=C(C1)C(C)C(O)C)C"};
        String[] tmpTestFileFiveIDs = new String[] {"CNP0192622", "CNP0262448"};
        int i = 0;
        for (IAtomContainer tmpAtomContainer : tmpMolSet.atomContainers()) {
            Assertions.assertEquals(tmpTestFileFiveSmiles[i],ChemUtil.createUniqueSmiles(tmpAtomContainer, false));
            Assertions.assertEquals(tmpTestFileFiveIDs[i],tmpAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
            i++;
        }
        Assertions.assertEquals(1, tmpReader.getSkippedLinesCounter());
    }
    //
    /**
     * Test file's specifications:
     * - 51 lines, 50 with structures, 1 header line
     * - ID second in line
     * - used separator: " "
     * - multiple garbage columns after the first 2
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void smilesFormatDetectionOnCOCONUTFileWithMultipleColumnsTest() throws Exception {
        URL tmpURL = this.getClass().getResource("SMILESTestFileSix.smi");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        DynamicSMILESFileFormat tmpFormat = DynamicSMILESFileReader.detectFormat(tmpResourceFile);
        Assertions.assertTrue(tmpFormat.hasHeaderLine());
        Assertions.assertEquals(1, tmpFormat.getIDColumnPosition());
        Assertions.assertEquals(0, tmpFormat.getSMILESCodeColumnPosition());
        Assertions.assertTrue(tmpFormat.hasIDColumn());
        Assertions.assertEquals(' ', tmpFormat.getSeparatorChar());
    }
    //
    /**
     * Test file's specifications:
     * - 51 lines, 50 with structures, 1 header line
     * - ID second in line
     * - used separator: " "
     * - multiple garbage columns after the first 2
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void smilesFileImportOnCOCONUTFileWithMultipleColumnsTest() throws Exception {
        URL tmpURL = this.getClass().getResource("SMILESTestFileSix.smi");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        DynamicSMILESFileFormat tmpFormat = DynamicSMILESFileReader.detectFormat(tmpResourceFile);
        DynamicSMILESFileReader tmpReader = new DynamicSMILESFileReader();
        IAtomContainerSet tmpMolSet = tmpReader.readFile(tmpResourceFile, tmpFormat);
        Assertions.assertEquals(50, tmpMolSet.getAtomContainerCount());
        Assertions.assertEquals("CNP0000001", tmpMolSet.getAtomContainer(0).getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
        Assertions.assertEquals(0, tmpReader.getSkippedLinesCounter());
    }
    //
    /**
     * Test file's specifications:
     * - 37 lines
     * - no headline
     * - no faulty lines
     * - ID first in line, SMILES second
     * - used separator: tab
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void smilesFormatDetectionOnChEBIFileTest() throws Exception {
        URL tmpURL = this.getClass().getResource("SMILESTestFileSeven.txt");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        DynamicSMILESFileFormat tmpFormat = DynamicSMILESFileReader.detectFormat(tmpResourceFile);
        Assertions.assertTrue(tmpFormat.hasIDColumn());
        Assertions.assertFalse(tmpFormat.hasHeaderLine());
        Assertions.assertEquals(1, tmpFormat.getSMILESCodeColumnPosition());
        Assertions.assertEquals(0, tmpFormat.getIDColumnPosition());
        Assertions.assertEquals('\t', tmpFormat.getSeparatorChar());
    }
    //
    /**
     * Test file's specifications:
     * - 37 lines
     * - no headline
     * - no faulty lines
     * - ID first in line, SMILES second
     * - used separator: tab
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void smilesFileImportOnChEBIFileTest() throws Exception {
        URL tmpURL = this.getClass().getResource("SMILESTestFileSeven.txt");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        DynamicSMILESFileFormat tmpFormat = DynamicSMILESFileReader.detectFormat(tmpResourceFile);
        DynamicSMILESFileReader tmpReader = new DynamicSMILESFileReader();
        IAtomContainerSet tmpMolSet = tmpReader.readFile(tmpResourceFile, tmpFormat);
        Assertions.assertEquals(37, tmpMolSet.getAtomContainerCount());
        Assertions.assertEquals("cmnpd_id_10213", tmpMolSet.getAtomContainer(0).getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
        Assertions.assertEquals("cmnpd_id_11687", tmpMolSet.getAtomContainer(36).getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
        Assertions.assertEquals(0, tmpReader.getSkippedLinesCounter());
    }
    //
    /**
     * Tests a file with aromatic fragments created using the Ertl algorithm fragmenter.
     * Test file's specifications:
     * - 10 structures
     * - no headline
     * - no faulty lines
     * - SMILES first in line, name second
     * - used separator: space
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testAromatics() throws Exception {
        URL tmpURL = this.getClass().getResource("Aromatics.smi");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        DynamicSMILESFileFormat tmpFormat = DynamicSMILESFileReader.detectFormat(tmpResourceFile);
        DynamicSMILESFileReader tmpReader = new DynamicSMILESFileReader();
        IAtomContainerSet tmpMolSet = tmpReader.readFile(tmpResourceFile, tmpFormat);
        Assertions.assertEquals(10, tmpMolSet.getAtomContainerCount());
        Assertions.assertEquals("derivat-(1S)-6,8-dihydroxy-1-methyl-1,2-dihydrocyclopenta[c]isochromene-3,5-dione", tmpMolSet.getAtomContainer(0).getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
        Assertions.assertEquals("derivat-6-chloro-3-(3-methylisoxazol-5-yl)-4-phenylquinolin-2(1H)-one", tmpMolSet.getAtomContainer(9).getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
        Assertions.assertEquals(0, tmpReader.getSkippedLinesCounter());
    }
    //
    /**
     * Tests that detectFormat() throws an IOException when given an empty (0-byte) file, since no parsable SMILES code
     * can be found and the file therefore does not fit the expected SMILES file format. An empty file is written
     * on-the-fly into a temporary directory.
     *
     * @param aTempDir temporary directory provided by JUnit
     * @throws Exception if anything goes wrong
     */
    @Test
    public void detectFormatOnEmptyFileThrowsIOExceptionTest(@TempDir Path aTempDir) throws Exception {
        File tmpEmptyFile = aTempDir.resolve("EmptyOnTheFly.txt").toFile();
        try (FileWriter tmpWriter = new FileWriter(tmpEmptyFile)) {
            // intentionally write nothing to keep the file empty
        }
        Assertions.assertTrue(tmpEmptyFile.exists());
        Assertions.assertEquals(0, tmpEmptyFile.length());
        Assertions.assertThrows(IOException.class, () -> DynamicSMILESFileReader.detectFormat(tmpEmptyFile));
    }
    //
    /**
     * Tests that detectFormat() throws an IOException when given the empty-file classpath fixture (EmptyFile.txt), since
     * no parsable SMILES code can be found and the file therefore does not fit the expected SMILES file format.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void detectFormatOnEmptyFixtureThrowsIOExceptionTest() throws Exception {
        URL tmpURL = this.getClass().getResource("EmptyFile.txt");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        Assertions.assertEquals(0, tmpResourceFile.length());
        Assertions.assertThrows(IOException.class, () -> DynamicSMILESFileReader.detectFormat(tmpResourceFile));
    }
    //
    /**
     * Tests that detectFormat() throws an IOException when given a file that does not exist on disk. This covers the
     * FileNotFoundException outer catch block in detectFormat(), which rethrows as IOException. The non-existent file is
     * resolved against a temporary directory.
     *
     * @param aTempDir temporary directory provided by JUnit
     * @throws Exception if anything goes wrong
     */
    @Test
    public void detectFormatOnMissingFileThrowsIOExceptionTest(@TempDir Path aTempDir) throws Exception {
        File tmpMissingFile = aTempDir.resolve("DoesNotExist.txt").toFile();
        Assertions.assertFalse(tmpMissingFile.exists());
        Assertions.assertThrows(IOException.class, () -> DynamicSMILESFileReader.detectFormat(tmpMissingFile));
    }
    //
    /**
     * Tests that detectFormat() throws an IOException when given a file whose lines are all unparsable as SMILES codes
     * (AllInvalidSmiles.txt classpath fixture), since no parsable SMILES code can be found and the file therefore does
     * not fit the expected SMILES file format.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void detectFormatOnAllInvalidFileThrowsIOExceptionTest() throws Exception {
        URL tmpURL = this.getClass().getResource("AllInvalidSmiles.txt");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        Assertions.assertThrows(IOException.class, () -> DynamicSMILESFileReader.detectFormat(tmpResourceFile));
    }
    //
    /**
     * Tests that readFile() skips every data line and returns an empty atom container set when given a file that has a
     * header line followed by data lines that are all invalid SMILES codes. The skipped-lines counter must equal the
     * number of (invalid) data lines, since the header line is not counted. The file is written on-the-fly into a
     * temporary directory and the format is supplied directly (single column, with a header line) so that detectFormat()
     * is not exercised here.
     *
     * @param aTempDir temporary directory provided by JUnit
     * @throws Exception if anything goes wrong
     */
    @Test
    public void readFileOnHeaderWithAllInvalidDataLinesSkipsAllAndReturnsEmptySetTest(@TempDir Path aTempDir) throws Exception {
        File tmpFile = aTempDir.resolve("HeaderWithInvalidData.txt").toFile();
        try (FileWriter tmpWriter = new FileWriter(tmpFile)) {
            tmpWriter.write("SMILES" + System.lineSeparator());
            tmpWriter.write("not_a_valid_smiles_one" + System.lineSeparator());
            tmpWriter.write("not_a_valid_smiles_two" + System.lineSeparator());
            tmpWriter.write("not_a_valid_smiles_three" + System.lineSeparator());
        }
        DynamicSMILESFileFormat tmpFormat = new DynamicSMILESFileFormat(true);
        DynamicSMILESFileReader tmpReader = new DynamicSMILESFileReader();
        IAtomContainerSet tmpMolSet = tmpReader.readFile(tmpFile, tmpFormat);
        Assertions.assertEquals(0, tmpMolSet.getAtomContainerCount());
        Assertions.assertEquals(3, tmpReader.getSkippedLinesCounter());
    }
}
