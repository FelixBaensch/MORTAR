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

import de.unijena.cheminf.mortar.controller.TabNames;
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
 * Tests the structure (molecules and fragments) export functionalities of the {@link Exporter} class, especially
 * the export of the MORTAR IDs.
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
     * Tests exporting molecules to a CSV file with SMILES, name, and ID columns.
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
        Assertions.assertEquals("SMILES;Name;ID", tmpLines.get(0));
        Assertions.assertEquals("c1ccccc1;Benzene;Test_1", tmpLines.get(1));
        Assertions.assertEquals("CCO;Eth_anol;Test_2", tmpLines.get(2));
    }
    //
    /**
     * Tests exporting fragments to a CSV file including the molecule frequency and molecule percentage columns but
     * without the name column; the ID has to be in the second column instead.
     *
     * @param aTempDir temporary directory provided by JUnit
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsCsvFileWithFrequencies(@TempDir Path aTempDir) throws Exception {
        Exporter tmpExporter = new Exporter(new SettingsContainer());
        File tmpFile = aTempDir.resolve("fragments.csv").toFile();
        List<String> tmpFailed = tmpExporter.exportStructuresAsCsvFile(tmpFile, this.getFragments(), ',', true, false);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        List<String> tmpLines = Files.readAllLines(tmpFile.toPath());
        Assertions.assertEquals("SMILES,ID,MoleculeFrequency,MoleculePercentage", tmpLines.get(0));
        Assertions.assertEquals("c1ccccc1,Ertl_algorithm_1,3,0.7500", tmpLines.get(1));
        Assertions.assertEquals("CO,Ertl_algorithm_2,1,0.2500", tmpLines.get(2));
    }
    //
    /**
     * Tests exporting a mixed list of molecules and fragments to a CSV file; fragments have to be written with their
     * ID as name.
     *
     * @param aTempDir temporary directory provided by JUnit
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportMixedStructuresAsCsvFile(@TempDir Path aTempDir) throws Exception {
        Exporter tmpExporter = new Exporter(new SettingsContainer());
        List<MoleculeDataModel> tmpList = new ArrayList<>(this.getMolecules());
        tmpList.addAll(this.getFragments());
        File tmpFile = aTempDir.resolve("mixed.csv").toFile();
        tmpExporter.exportStructuresAsCsvFile(tmpFile, tmpList, ';', false);
        List<String> tmpLines = Files.readAllLines(tmpFile.toPath());
        Assertions.assertEquals("c1ccccc1;Ertl_algorithm_1;Ertl_algorithm_1", tmpLines.get(3));
    }
    //
    /**
     * Tests the fragments tab CSV export, which has to contain the fragment IDs in the second column.
     *
     * @param aTempDir temporary directory provided by JUnit
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsTabCsvFile(@TempDir Path aTempDir) throws Exception {
        Exporter tmpExporter = new Exporter(new SettingsContainer());
        File tmpFile = aTempDir.resolve("fragmentsTab.csv").toFile();
        List<String> tmpFailed = tmpExporter.exportCsvFile(tmpFile, this.getFragments(), "Ertl algorithm", ';', TabNames.FRAGMENTS);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        List<String> tmpLines = Files.readAllLines(tmpFile.toPath());
        Assertions.assertEquals("SMILES;ID;Frequency;Percentage;MoleculeFrequency;MoleculePercentage", tmpLines.get(0));
        Assertions.assertTrue(tmpLines.get(1).startsWith("c1ccccc1;Ertl_algorithm_1;"));
    }
    //
    /**
     * Tests that fragments exported from the fragments tab to a CSV file get their IDs as names when the file is
     * re-imported into MORTAR (and not their frequencies, see GitHub issue #47), and that the re-imported molecules
     * get new IDs derived from the file name.
     *
     * @param aTempDir temporary directory provided by JUnit
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testReImportOfExportedFragments(@TempDir Path aTempDir) throws Exception {
        SettingsContainer tmpSettingsContainer = new SettingsContainer();
        Exporter tmpExporter = new Exporter(tmpSettingsContainer);
        File tmpFile = aTempDir.resolve("Exported Fragments.csv").toFile();
        tmpExporter.exportCsvFile(tmpFile, this.getFragments(), "Ertl algorithm", ';', TabNames.FRAGMENTS);
        Importer tmpImporter = new Importer(tmpSettingsContainer);
        List<MoleculeDataModel> tmpReImported = tmpImporter.importMoleculeFile(tmpFile, false, false, false);
        Assertions.assertEquals(2, tmpReImported.size());
        Assertions.assertEquals("Ertl_algorithm_1", tmpReImported.get(0).getName());
        Assertions.assertEquals("Ertl_algorithm_2", tmpReImported.get(1).getName());
        Assertions.assertEquals("Exported_Fragments_1", tmpReImported.get(0).getId());
        Assertions.assertEquals("Exported_Fragments_2", tmpReImported.get(1).getId());
    }
    //
    /**
     * Tests exporting molecules and fragments to a single SD file with generated 2D coordinates, the names (fragments:
     * IDs) as titles, and the IDs as data field.
     *
     * @param aTempDir temporary directory provided by JUnit
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportStructuresAsSDFile(@TempDir Path aTempDir) throws Exception {
        Exporter tmpExporter = new Exporter(new SettingsContainer());
        List<MoleculeDataModel> tmpList = new ArrayList<>(this.getMolecules());
        tmpList.addAll(this.getFragments());
        File tmpFile = aTempDir.resolve("structures.sdf").toFile();
        List<String> tmpFailed = tmpExporter.exportStructuresAsSDFile(tmpFile, tmpList);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        List<IAtomContainer> tmpReadStructures = new ArrayList<>();
        try (IteratingSDFReader tmpReader = new IteratingSDFReader(new FileReader(tmpFile), SilentChemObjectBuilder.getInstance())) {
            while (tmpReader.hasNext()) {
                tmpReadStructures.add(tmpReader.next());
            }
        }
        Assertions.assertEquals(4, tmpReadStructures.size());
        Assertions.assertEquals("Benzene", tmpReadStructures.get(0).getTitle());
        Assertions.assertEquals("Test_1", tmpReadStructures.get(0).getProperty(Exporter.ID_PROPERTY_KEY));
        Assertions.assertEquals(6, tmpReadStructures.get(0).getAtomCount());
        Assertions.assertNotNull(tmpReadStructures.get(0).getAtom(0).getPoint2d());
        Assertions.assertEquals("Eth;anol", tmpReadStructures.get(1).getTitle());
        Assertions.assertEquals("Ertl_algorithm_1", tmpReadStructures.get(2).getTitle());
        Assertions.assertEquals("Ertl_algorithm_1", tmpReadStructures.get(2).getProperty(Exporter.ID_PROPERTY_KEY));
    }
    //
    /**
     * Tests that the actual export destination is the newly created subdirectory for exports into separate files and
     * the chosen file for exports into a single file.
     *
     * @param aTempDir temporary directory provided by JUnit
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testActualExportDestination(@TempDir Path aTempDir) throws Exception {
        File tmpChosenDirectory = aTempDir.toFile();
        Exporter tmpSeparateFilesExporter = new Exporter(new SettingsContainer());
        tmpSeparateFilesExporter.exportFragmentsAsChemicalFile(tmpChosenDirectory, this.getFragments(), ChemFileTypes.SDF, true, false);
        File tmpDestination = tmpSeparateFilesExporter.getActualExportDestination(tmpChosenDirectory);
        Assertions.assertTrue(tmpDestination.isDirectory());
        Assertions.assertEquals(tmpChosenDirectory, tmpDestination.getParentFile());
        Assertions.assertEquals(2, tmpDestination.listFiles().length);
        Exporter tmpSingleFileExporter = new Exporter(new SettingsContainer());
        File tmpFile = aTempDir.resolve("fragments.sdf").toFile();
        tmpSingleFileExporter.exportFragmentsAsChemicalFile(tmpFile, this.getFragments(), ChemFileTypes.SDF, true, true);
        Assertions.assertEquals(tmpFile, tmpSingleFileExporter.getActualExportDestination(tmpFile));
    }
    //</editor-fold>
    //
    //<editor-fold desc="private methods">
    /**
     * Returns two test molecules with IDs, one of them with a CSV separator char in its name.
     *
     * @return list of molecule data models
     */
    private List<MoleculeDataModel> getMolecules() {
        List<MoleculeDataModel> tmpList = new ArrayList<>(2);
        MoleculeDataModel tmpBenzene = new MoleculeDataModel("c1ccccc1", "Benzene", new HashMap<>());
        tmpBenzene.setId("Test_1");
        tmpList.add(tmpBenzene);
        MoleculeDataModel tmpEthanol = new MoleculeDataModel("CCO", "Eth;anol", new HashMap<>());
        tmpEthanol.setId("Test_2");
        tmpList.add(tmpEthanol);
        return tmpList;
    }
    //
    /**
     * Returns two test fragments without names but with IDs and frequencies.
     *
     * @return list of fragment data models
     */
    private List<MoleculeDataModel> getFragments() {
        List<MoleculeDataModel> tmpList = new ArrayList<>(2);
        FragmentDataModel tmpBenzene = new FragmentDataModel("c1ccccc1", null, new HashMap<>());
        tmpBenzene.setId("Ertl_algorithm_1");
        tmpBenzene.setMoleculeFrequency(3);
        tmpBenzene.setMoleculePercentage(0.75);
        tmpList.add(tmpBenzene);
        FragmentDataModel tmpMethanol = new FragmentDataModel("CO", null, new HashMap<>());
        tmpMethanol.setId("Ertl_algorithm_2");
        tmpMethanol.setMoleculeFrequency(1);
        tmpMethanol.setMoleculePercentage(0.25);
        tmpList.add(tmpMethanol);
        return tmpList;
    }
    //</editor-fold>
}
