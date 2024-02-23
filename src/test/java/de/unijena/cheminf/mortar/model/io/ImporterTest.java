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

package de.unijena.cheminf.mortar.model.io;

import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.ChemUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;

import java.net.URL;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Test class for the Importer class.
 *
 * @author Samuel Behr
 * @version 1.0.0.0
 */
public class ImporterTest extends Importer {

    /**
     * Static initializer to set default locale to british english which is important for the correct functioning of
     * the settings container because tooltips for its settings are imported from the message.properties file.
     */
    static {
        Locale.setDefault(new Locale("en", "GB"));
    }
    //
    /**
     * Constructor, calls super() with a new SettingsContainer instance.
     */
    public ImporterTest() {
        super(new SettingsContainer());
    }
    //
    /**
     * The ImportSMILESFile() method expects one parsable SMILES code per line of the file and
     * an optional second element, which is interpreted as the molecule's ID or name and is
     * separated from the SMILES code by one of the tokens tab, semicolon, comma or space.
     * After finding this structure in the file's second or third line (skipping the first line
     * as potential headline), the file is declared to be a SMILES file and read from the first
     * line till the last line, ignoring blank lines as well as lines with invalid SMILES code
     * or invalid structure. If the second or third line is found to be null, the first line is
     * not skipped at the first part.
     * <br>NOTE: The importer logs on the console and the results have to be manually checked against what is
     * given here in the code comments!
     *
     * @throws Exception if anything goes wrong
     * @author Samuel Behr
     */
    @Test
    public void ImportSMILESFileTest() throws Exception{
        /*
        Expected output:    3 parsable lines
                            3 invalid lines
        Test file's specifications:
        - .txt file
        - with headline
        - SMILES code only (no ID or name)
        - including blank lines
         */
        URL tmpURL = this.getClass().getResource("SMILESTestFileOne.txt");
        IAtomContainerSet tmpMolSet = this.importSMILESFile(Paths.get(tmpURL.toURI()).toFile());
        Assertions.assertEquals(3, tmpMolSet.getAtomContainerCount());
        Assertions.assertEquals("SMILESTestFileOne1", tmpMolSet.getAtomContainer(0).getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));


        /*
        Expected output:    5 parsable lines
                            0 invalid lines
        Test file's specifications:
        - .smi file
        - no headline
        - ID first in line
        - used separator: "\t"
         */
        tmpURL = this.getClass().getResource("SMILESTestFileTwo.smi");
        tmpMolSet = this.importSMILESFile(Paths.get(tmpURL.toURI()).toFile());
        Assertions.assertEquals(5, tmpMolSet.getAtomContainerCount());
        Assertions.assertEquals("CNP0337481", tmpMolSet.getAtomContainer(4).getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));

        /*
        Expected output:    3 parsable lines
                            3 invalid lines
        Test file's specifications:
        - "NAME" second in line and containing spaces
        - used separator: ";"
        - two lines with invalid SMILES code
         */
        tmpURL = this.getClass().getResource("SMILESTestFileThree.txt");
        tmpMolSet = this.importSMILESFile(Paths.get(tmpURL.toURI()).toFile());
        Assertions.assertEquals(3, tmpMolSet.getAtomContainerCount());
        Assertions.assertEquals("Valdiazen", tmpMolSet.getAtomContainer(2).getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));

        /*
        Expected output:    1 parsable lines
                            0 invalid lines
        Test file's specifications:
        - one single line only
        - ID first in line
        - used separator: " "
         */
        tmpURL = this.getClass().getResource("SMILESTestFileFour.txt");
        tmpMolSet = this.importSMILESFile(Paths.get(tmpURL.toURI()).toFile());
        Assertions.assertEquals(1, tmpMolSet.getAtomContainerCount());
        Assertions.assertEquals("CNP0356547", tmpMolSet.getAtomContainer(0).getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));

        /*
        Expected output:    2 parsable lines
                            2 invalid lines
        Test file's specifications:
        - headline and blank line first
        - three elements per line
        - SMILES first in line, ID second and a neglectable third element
        - third element in line
        - used separator: "\t"
         */
        tmpURL = this.getClass().getResource("SMILESTestFileFive.txt");
        IAtomContainerSet tmpAtomContainerSet = this.importSMILESFile(Paths.get(tmpURL.toURI()).toFile());
        //
        String[] tmpTestFileFiveSmiles = new String[] {"OC=1C=C(O)C=C(C1)C=2OC=3C=CC=CC3C2", "OC=1C=C(O)C(=C(C1)C(C)C(O)C)C"};
        String[] tmpTestFileFiveIDs = new String[] {"CNP0192622", "CNP0262448"};
        int i = 0;
        for (IAtomContainer tmpAtomContainer :
                tmpAtomContainerSet.atomContainers()) {
            Assertions.assertEquals(tmpTestFileFiveSmiles[i],ChemUtil.createUniqueSmiles(tmpAtomContainer));
            Assertions.assertEquals(tmpTestFileFiveIDs[i],tmpAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
            i++;
        }

        /*
        Expected output:    50 parsable lines
                            1 invalid lines (header)
        Test file's specifications:
        - 51 lines, 50 with structures, 1 header line
        - ID second in line
        - used separator: " "
        - multiple garbage columns after the first 2
         */
        tmpURL = this.getClass().getResource("SMILESTestFileSix.smi");
        tmpMolSet = this.importSMILESFile(Paths.get(tmpURL.toURI()).toFile());
        Assertions.assertEquals(50, tmpMolSet.getAtomContainerCount());
        Assertions.assertEquals("CNP0000001", tmpMolSet.getAtomContainer(0).getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
    }

    @Test
    public void test() throws Exception {
        //TODO this is the problem because the parsing does not fail here, only the first part up to \t is parsed successfully!
        ChemUtil.parseSmilesToAtomContainer("CCCCOCCC\tlfdsklhfdfvdbgvb");
        //Same here:
        System.out.println(ChemUtil.parseSmilesToAtomContainer("CCCCOCCC lfdsklhfdfvdbgvb").getTitle());
        //output: lfdsklhfdfvdbgvb
        // the remaining string is parsed as a title!
    }

    @Test
    public void containsOnlySMILESValidCharactersTest() throws Exception {
        Assertions.assertFalse(Importer.containsOnlySMILESValidCharacters("CCCCOCCC\tlfdsklhfdfvdbgvb"));
        Assertions.assertFalse(Importer.containsOnlySMILESValidCharacters("CCCCOCCC lfdsklhfdfvdbgvb"));
        Assertions.assertFalse(Importer.containsOnlySMILESValidCharacters(""));
        Assertions.assertFalse(Importer.containsOnlySMILESValidCharacters("\t"));
        for (String tmpSeparator : BasicDefinitions.POSSIBLE_SMILES_FILE_SEPARATORS) {
            Assertions.assertFalse(Importer.containsOnlySMILESValidCharacters(tmpSeparator), "was true for " + tmpSeparator);
        }
    }
}
