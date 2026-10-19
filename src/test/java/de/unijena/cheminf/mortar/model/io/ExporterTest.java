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

import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Tests the structure (molecules and fragments) export functionalities of the {@link Exporter} class.
 *
 * @author Manuel Schauer
 */
public class ExporterTest {
    //<editor-fold desc="static initializer">
    /**
     * Sets the default locale to British English.
     */
    static {
        Locale.setDefault(Locale.of("en", "GB"));
    }
    //</editor-fold>
    //
    // <editor-fold desc="test methods">
    /**
     * Tests exporting molecules to a CSV file without frequency columns.
     *
     * @param aTempDir temporary directory provided by JUnit
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportMoleculesAsCsvFile(@TempDir Path aTempDir) throws Exception {
        Exporter tmpExporter = new Exporter(new SettingsContainer());
        File tmpFile = aTempDir.resolve("molecules.csv").toFile();
        List<String> tmpFailed = tmpExporter.exportStructuresAsCsvFile(tmpFile, this.getMolecules(), ';', false);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        List<String> tmpLines = Files.readAllLines(tmpFile.toPath());
        Assertions.assertEquals(3, tmpLines.size());
        Assertions.assertEquals("SMILES;Name", tmpLines.get(0));
        Assertions.assertEquals("c1ccccc1;Benzene", tmpLines.get(1));
        Assertions.assertEquals("CCO;Eth_anol", tmpLines.get(2));
    }
    //
    /**
     * Tests exporting fragments to a CSV file including the molecule frequency and molecule percentage columns but
     * without the name column.
     *
     * @param aTempDir temporary directory provided by JUnit
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsCsvFileWithFrequencies(@TempDir Path aTempDir) throws Exception {
        Exporter tmpExporter = new Exporter(new SettingsContainer());
        FragmentDataModel tmpFragment = new FragmentDataModel("c1ccccc1", null, new HashMap<>());
        tmpFragment.setMoleculeFrequency(3);
        tmpFragment.setMoleculePercentage(0.75);
        List<MoleculeDataModel> tmpList = new ArrayList<>(List.of(tmpFragment));
        File tmpFile = aTempDir.resolve("fragments.csv").toFile();
        List<String> tmpFailed = tmpExporter.exportStructuresAsCsvFile(tmpFile, tmpList, ',', true, false);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        List<String> tmpLines = Files.readAllLines(tmpFile.toPath());
        Assertions.assertEquals("SMILES,MoleculeFrequency,MoleculePercentage", tmpLines.get(0));
        Assertions.assertEquals("c1ccccc1,3,0.7500", tmpLines.get(1));
    }
    //
    /**
     * Tests exporting molecules to a single SD file with generated 2D coordinates and the names as titles.
     *
     * @param aTempDir temporary directory provided by JUnit
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportMoleculesAsSDFile(@TempDir Path aTempDir) throws Exception {
        Exporter tmpExporter = new Exporter(new SettingsContainer());
        File tmpFile = aTempDir.resolve("molecules.sdf").toFile();
        List<String> tmpFailed = tmpExporter.exportStructuresAsSDFile(tmpFile, this.getMolecules());
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        List<IAtomContainer> tmpReadMolecules = new ArrayList<>();
        try (IteratingSDFReader tmpReader = new IteratingSDFReader(new FileReader(tmpFile), SilentChemObjectBuilder.getInstance())) {
            while (tmpReader.hasNext()) {
                tmpReadMolecules.add(tmpReader.next());
            }
        }
        Assertions.assertEquals(2, tmpReadMolecules.size());
        Assertions.assertEquals("Benzene", tmpReadMolecules.get(0).getTitle());
        Assertions.assertEquals(6, tmpReadMolecules.get(0).getAtomCount());
        Assertions.assertNotNull(tmpReadMolecules.get(0).getAtom(0).getPoint2d());
        Assertions.assertEquals("Eth;anol", tmpReadMolecules.get(1).getTitle());
    }
    //</editor-fold>
    //
    //<editor-fold desc="private methods">
    /**
     * Returns two test molecules, one of them with a CSV separator char in its name.
     *
     * @return list of molecule data models
     */
    private List<MoleculeDataModel> getMolecules() {
        List<MoleculeDataModel> tmpList = new ArrayList<>(2);
        tmpList.add(new MoleculeDataModel("c1ccccc1", "Benzene", new HashMap<>()));
        tmpList.add(new MoleculeDataModel("CCO", "Eth;anol", new HashMap<>()));
        return tmpList;
    }
    //</editor-fold>
}
