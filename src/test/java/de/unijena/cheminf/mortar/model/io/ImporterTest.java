/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2021  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
 *
 * Source code is available at <https://github.com/FelixBaensch/MORTAR>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.cheminf.mortar.model.io;

import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import org.junit.Test;

import java.net.URL;
import java.nio.file.Paths;

/**
 * Test class for the ImportSMILESFile() method of the Importer class.
 *
 * @author Samuel Behr
 */
public class ImporterTest extends Importer {
    /**
     *
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
        this.importSMILESFile(Paths.get(tmpURL.toURI()).toFile());

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
        this.importSMILESFile(Paths.get(tmpURL.toURI()).toFile());

        /*
        Expected output:    3 parsable lines
                            3 invalid lines
        Test file's specifications:
        - "NAME" second in line and containing spaces
        - used separator: ";"
        - two lines with invalid SMILES code
         */
        tmpURL = this.getClass().getResource("SMILESTestFileThree.txt");
        this.importSMILESFile(Paths.get(tmpURL.toURI()).toFile());

        /*
        Expected output:    1 parsable lines
                            0 invalid lines
        Test file's specifications:
        - one single line only
        - ID first in line
        - used separator: " "
         */
        tmpURL = this.getClass().getResource("SMILESTestFileFour.txt");
        this.importSMILESFile(Paths.get(tmpURL.toURI()).toFile());
    }
}
