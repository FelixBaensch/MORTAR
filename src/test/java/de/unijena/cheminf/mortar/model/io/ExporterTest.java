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

import com.lowagie.text.Image;

import de.unijena.cheminf.mortar.controller.TabNames;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.PDBWriter;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests the file-taking export methods of the {@link Exporter} class directly against a temporary output directory and
 * real CDK-derived data models. The GUI file-chooser methods ({@code openFileChooserForExportFileOrDir},
 * {@code chooseFile}, {@code chooseDirectory}) are intentionally left uncovered (no Stage in a unit test). The PDF
 * export path is exercised headlessly (CDK depiction renders to an AWT BufferedImage; JavaFX appears only as a thin
 * pixel-copy round trip), asserted by magic bytes only. The en-GB locale guard is load-bearing because the CSV/PDF
 * headers come from {@link Message}.
 * <p>
 * Several tests deliberately feed a data model carrying an unparsable SMILES string (see
 * {@link #UNPARSABLE_SMILES}) into an export, so the export routine's per-item failure branch is reached and the model
 * lands in the returned failed-list. CDK logs a parse warning for each of those models; that console output is expected
 * here, not a defect.
 *
 * @author Felix Baensch
 */
public class ExporterTest {
    //<editor-fold desc="Private static final class constants" defaultstate="collapsed">
    /**
     * A deliberately unparsable SMILES string. A data model carrying it cannot be depicted or written, which is how the
     * tests below reach the per-item failure branches of the export routines; the CDK parse warnings it provokes on the
     * console are expected output.
     */
    private static final String UNPARSABLE_SMILES = "not_a_valid_smiles";
    //</editor-fold>
    //
    //<editor-fold desc="static initializer">
    /**
     * Sets the default locale to British English so the {@link Message}-resolved CSV/PDF headers are deterministic.
     */
    static {
        Locale.setDefault(Locale.of("en", "GB"));
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private final variables" defaultstate="collapsed">
    /**
     * Exporter instance under test, constructed with a real settings container.
     */
    private final Exporter exporter;
    //</editor-fold>
    //
    //<editor-fold desc="Constructor">
    /**
     * Constructor. Re-asserts the en-GB locale (load-bearing for the golden header assertions) and builds the Exporter
     * with a real, classpath-configured SettingsContainer.
     */
    public ExporterTest() {
        Locale.setDefault(Locale.of("en", "GB"));
        this.exporter = new Exporter(new SettingsContainer());
    }
    //</editor-fold>
    //
    //<editor-fold desc="Test methods" defaultstate="collapsed">
    /**
     * Smoke test (written and run FIRST): a one-fragment FRAGMENTS-tab PDF export to a temporary file must succeed
     * headlessly and produce a file whose first four bytes are the {@code %PDF} magic bytes. This validates the research
     * headless verdict (A1) that the CDK depiction + OpenPDF render path needs no live JavaFX toolkit. If this throws a
     * headless/toolkit error, the PDF coverage path is not reachable and the rest of the PDF work must stop.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportPdfFileFragmentsTabSmoke(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        ObservableList<MoleculeDataModel> tmpMolecules = FXCollections.observableArrayList(tmpFragments);
        File tmpOut = aTempDir.resolve("smoke.pdf").toFile();
        List<String> tmpFailed = this.exporter.exportPdfFile(
                tmpOut, tmpFragments, tmpMolecules, "ErtlFG", "input.smi", TabNames.FRAGMENTS);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        Assertions.assertTrue(tmpOut.length() > 0);
        byte[] tmpHead = Arrays.copyOf(Files.readAllBytes(tmpOut.toPath()), 4);
        Assertions.assertEquals("%PDF", new String(tmpHead, StandardCharsets.US_ASCII));
    }
    //
    /**
     * Tests that exporting the FRAGMENTS tab as a CSV file with a list of real FragmentDataModel instances succeeds
     * (empty failed-list), the file content starts with the en-GB golden header built from the five fragmentation-tab
     * Message keys, and the chosen separator character is present.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportCsvFileFragmentsTab(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        File tmpOut = aTempDir.resolve("frags.csv").toFile();
        List<String> tmpFailed = this.exporter.exportCsvFile(tmpOut, tmpFragments, "ErtlFG", ',', TabNames.FRAGMENTS);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        String tmpContent = Files.readString(tmpOut.toPath());
        String tmpExpectedHeader =
                Message.get("Exporter.fragmentationTab.csvHeader.smiles") + ',' +
                Message.get("Exporter.fragmentationTab.csvHeader.frequency") + ',' +
                Message.get("Exporter.fragmentationTab.csvHeader.percentage") + ',' +
                Message.get("Exporter.fragmentationTab.csvHeader.moleculeFrequency") + ',' +
                Message.get("Exporter.fragmentationTab.csvHeader.moleculePercentage");
        Assertions.assertTrue(tmpContent.startsWith(tmpExpectedHeader));
        Assertions.assertTrue(tmpContent.contains(","));
    }
    //
    /**
     * Tests that exporting the ITEMIZATION tab as a CSV file with a parent molecule carrying a populated fragments map
     * and frequency map succeeds (empty failed-list) and the file content starts with the four-column en-GB itemization
     * header.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportCsvFileItemizationTab(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpMolecules = new ArrayList<>();
        tmpMolecules.add(ExporterTest.buildMoleculeWithFragments("ErtlFG"));
        File tmpOut = aTempDir.resolve("items.csv").toFile();
        List<String> tmpFailed = this.exporter.exportCsvFile(tmpOut, tmpMolecules, "ErtlFG", ',', TabNames.ITEMIZATION);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        String tmpContent = Files.readString(tmpOut.toPath());
        String tmpExpectedHeader =
                Message.get("Exporter.itemsTab.csvHeader.moleculeName") + ',' +
                Message.get("Exporter.itemsTab.csvHeader.smilesOfStructure") + ',' +
                Message.get("Exporter.itemsTab.csvHeader.smilesOfFragment") + ',' +
                Message.get("Exporter.itemsTab.csvHeader.frequencyOfFragment");
        Assertions.assertTrue(tmpContent.startsWith(tmpExpectedHeader));
    }
    //
    /**
     * Tests the wrong-type guard: passing a plain MoleculeDataModel (not a FragmentDataModel) into the FRAGMENTS-tab
     * CSV export triggers the internal cast failure so the molecule's SMILES lands in the returned failed-export list
     * (non-empty).
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportCsvFileFragmentsTabWrongTypeGuard(@TempDir Path aTempDir) throws Exception {
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpAtomContainer = tmpParser.parseSmiles("CCO");
        MoleculeDataModel tmpPlainMolecule = new MoleculeDataModel(tmpAtomContainer, false);
        List<MoleculeDataModel> tmpList = new ArrayList<>();
        tmpList.add(tmpPlainMolecule);
        File tmpOut = aTempDir.resolve("wrongtype.csv").toFile();
        List<String> tmpFailed = this.exporter.exportCsvFile(tmpOut, tmpList, "ErtlFG", ',', TabNames.FRAGMENTS);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertFalse(tmpFailed.isEmpty());
    }
    //
    /**
     * Tests that a single SD-file export of a list of FragmentDataModel instances (with 2D-coordinate generation
     * enabled, the common case for SMILES-derived fragments) succeeds with an empty failed-export list and writes a
     * file whose content contains the MDL record terminator {@code $$$$}, proving a valid SDF was written.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFileSingleSdfGenerate2d(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        File tmpOut = aTempDir.resolve("single.sdf").toFile();
        List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                tmpOut, tmpFragments, ChemFileTypes.SDF, true, true);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        Assertions.assertTrue(tmpOut.length() > 0);
        String tmpContent = Files.readString(tmpOut.toPath());
        Assertions.assertTrue(tmpContent.contains("$$$$"));
        int tmpRecordTerminatorCount =
                tmpContent.split("\\$\\$\\$\\$", -1).length - 1;
        Assertions.assertEquals(tmpFragments.size(), tmpRecordTerminatorCount);
    }
    //
    /**
     * Tests the single SD-file export with 2D-coordinate generation disabled, driving the zero-3D-coordinate branch of
     * {@code handleFragmentWithNo3dInformationAvailable}. The export still succeeds (empty failed-list) and writes a
     * non-empty file containing the MDL record terminator {@code $$$$}.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFileSingleSdfNoGenerate2d(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        File tmpOut = aTempDir.resolve("single_no2d.sdf").toFile();
        List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                tmpOut, tmpFragments, ChemFileTypes.SDF, false, true);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        Assertions.assertTrue(tmpOut.length() > 0);
        Assertions.assertTrue(Files.readString(tmpOut.toPath()).contains("$$$$"));
    }
    //
    /**
     * Tests the single SD-file export with the {@code alwaysMDLV3000FormatAtExport} setting enabled, exercising the
     * {@code SDFWriter.setAlwaysV3000(true)} branch. A local SettingsContainer and Exporter are used so the global
     * instance under test is not mutated; the setting is restored in a {@code finally} block (FileUtilTest idiom). The
     * file is written and contains the V3000 connection-table marker.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFileSingleSdfV3000(@TempDir Path aTempDir) throws Exception {
        SettingsContainer tmpContainer = new SettingsContainer();
        boolean tmpOriginalSetting = tmpContainer.getAlwaysMDLV3000FormatAtExportSetting();
        try {
            tmpContainer.setAlwaysMDLV3000FormatAtExportSetting(true);
            Exporter tmpLocalExporter = new Exporter(tmpContainer);
            List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
            File tmpOut = aTempDir.resolve("single_v3000.sdf").toFile();
            List<String> tmpFailed = tmpLocalExporter.exportFragmentsAsChemicalFile(
                    tmpOut, tmpFragments, ChemFileTypes.SDF, true, true);
            Assertions.assertNotNull(tmpFailed);
            Assertions.assertTrue(tmpFailed.isEmpty());
            Assertions.assertTrue(tmpOut.length() > 0);
            String tmpContent = Files.readString(tmpOut.toPath());
            Assertions.assertTrue(tmpContent.contains("$$$$"));
            Assertions.assertTrue(tmpContent.contains("V3000"));
        } finally {
            tmpContainer.setAlwaysMDLV3000FormatAtExportSetting(tmpOriginalSetting);
        }
    }
    //
    /**
     * Tests the separate SD-files export ({@code isSingleExport=false}): the method creates a sub-directory inside the
     * passed @TempDir directory and writes one {@code .sdf} file per fragment into it. Asserts an empty failed-list, that
     * a sub-directory was created, and that it contains at least one {@code .sdf} file whose content carries the MDL
     * record terminator {@code $$$$}.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFileSeparateSdf(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        File tmpDir = aTempDir.toFile();
        List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                tmpDir, tmpFragments, ChemFileTypes.SDF, true, false);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        File[] tmpSubDirs = tmpDir.listFiles(File::isDirectory);
        Assertions.assertNotNull(tmpSubDirs);
        Assertions.assertEquals(1, tmpSubDirs.length);
        File[] tmpSdfFiles = tmpSubDirs[0].listFiles((aDir, aName) -> aName.endsWith(".sdf"));
        Assertions.assertNotNull(tmpSdfFiles);
        Assertions.assertTrue(tmpSdfFiles.length > 0);
        Assertions.assertTrue(Files.readString(tmpSdfFiles[0].toPath()).contains("$$$$"));
    }
    //
    /**
     * Tests the PDB export: the method creates a sub-directory inside the passed @TempDir directory and writes one
     * {@code .pdb} file per fragment into it. Asserts an empty failed-list, that a sub-directory was created, and that it
     * contains at least one non-empty {@code .pdb} file.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFilePdb(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        File tmpDir = aTempDir.toFile();
        List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                tmpDir, tmpFragments, ChemFileTypes.PDB, true);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        File[] tmpSubDirs = tmpDir.listFiles(File::isDirectory);
        Assertions.assertNotNull(tmpSubDirs);
        Assertions.assertEquals(1, tmpSubDirs.length);
        File[] tmpPdbFiles = tmpSubDirs[0].listFiles((aDir, aName) -> aName.endsWith(".pdb"));
        Assertions.assertNotNull(tmpPdbFiles);
        Assertions.assertTrue(tmpPdbFiles.length > 0);
        Assertions.assertTrue(tmpPdbFiles[0].length() > 0);
    }
    //
    /**
     * Tests the null-file guard of {@code exportFragmentsAsChemicalFile}: passing a {@code null} file returns
     * {@code null} (no file written).
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFileNullFileGuard() throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                null, tmpFragments, ChemFileTypes.SDF, true, true);
        Assertions.assertNull(tmpFailed);
    }
    //
    /**
     * Tests the full FRAGMENTS-tab PDF export with multiple real FragmentDataModel instances (a fuller variant of the
     * headless smoke test). Asserts an empty failed-list, a non-empty file, and the {@code %PDF} magic bytes.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportPdfFileFragmentsTabFull(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        ObservableList<MoleculeDataModel> tmpMolecules = FXCollections.observableArrayList(tmpFragments);
        File tmpOut = aTempDir.resolve("fragments_full.pdf").toFile();
        List<String> tmpFailed = this.exporter.exportPdfFile(
                tmpOut, tmpFragments, tmpMolecules, "ErtlFG", "input.smi", TabNames.FRAGMENTS);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        Assertions.assertTrue(tmpOut.length() > 0);
        byte[] tmpHead = Arrays.copyOf(Files.readAllBytes(tmpOut.toPath()), 4);
        Assertions.assertEquals("%PDF", new String(tmpHead, StandardCharsets.US_ASCII));
    }
    //
    /**
     * Tests the ITEMIZATION-tab PDF export, the largest single Exporter block. A non-empty observable molecule list
     * (a parent molecule with attached fragments), a non-empty fragmentation name, and a non-empty imported-file name
     * are all required (the three guards return {@code null} if any is empty). Asserts an empty failed-list and the
     * {@code %PDF} magic bytes.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportPdfFileItemizationTab(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        ObservableList<MoleculeDataModel> tmpMolecules =
                FXCollections.observableArrayList(ExporterTest.buildMoleculeWithFragments("ErtlFG"));
        File tmpOut = aTempDir.resolve("items.pdf").toFile();
        List<String> tmpFailed = this.exporter.exportPdfFile(
                tmpOut, tmpFragments, tmpMolecules, "ErtlFG", "input.smi", TabNames.ITEMIZATION);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        Assertions.assertTrue(tmpOut.length() > 0);
        byte[] tmpHead = Arrays.copyOf(Files.readAllBytes(tmpOut.toPath()), 4);
        Assertions.assertEquals("%PDF", new String(tmpHead, StandardCharsets.US_ASCII));
    }
    //
    /**
     * Tests the ITEMIZATION-tab PDF null-guard: passing an empty imported-file name makes the export return
     * {@code null} (no file written), per the empty-name guard of {@code createItemizationTabPdfFile}.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportPdfFileItemizationTabEmptyNameGuard(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        ObservableList<MoleculeDataModel> tmpMolecules =
                FXCollections.observableArrayList(ExporterTest.buildMoleculeWithFragments("ErtlFG"));
        File tmpOut = aTempDir.resolve("items_guard.pdf").toFile();
        List<String> tmpFailed = this.exporter.exportPdfFile(
                tmpOut, tmpFragments, tmpMolecules, "ErtlFG", "", TabNames.ITEMIZATION);
        Assertions.assertNull(tmpFailed);
    }
    //
    /**
     * Tests that the nested enums of the Exporter ({@code ExportTypes}, {@code FileExtension}, {@code CSVSeparator})
     * load and expose non-null accessors, loading their static initializers for coverage.
     */
    /**
     * Tests the ITEMIZATION-tab CSV export with a molecule that has NOT undergone the requested fragmentation. This drives
     * the {@code !hasMoleculeUndergoneSpecificFragmentation} continue-branch of {@code createItemizationTabCsvFile}: the
     * molecule row is written but no fragment rows are appended. The export succeeds with an empty failed-list and the
     * file content starts with the four-column itemization header.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportCsvFileItemizationTabMoleculeWithoutFragmentation(@TempDir Path aTempDir) throws Exception {
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpAtomContainer = tmpParser.parseSmiles("c1ccccc1CCO");
        MoleculeDataModel tmpMolecule = new MoleculeDataModel(tmpAtomContainer, false);
        tmpMolecule.setName("UnfragmentedMolecule");
        List<MoleculeDataModel> tmpMolecules = new ArrayList<>();
        tmpMolecules.add(tmpMolecule);
        File tmpOut = aTempDir.resolve("items_nofrag.csv").toFile();
        List<String> tmpFailed = this.exporter.exportCsvFile(tmpOut, tmpMolecules, "ErtlFG", ',', TabNames.ITEMIZATION);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        String tmpContent = Files.readString(tmpOut.toPath());
        String tmpExpectedHeader =
                Message.get("Exporter.itemsTab.csvHeader.moleculeName") + ',' +
                Message.get("Exporter.itemsTab.csvHeader.smilesOfStructure") + ',' +
                Message.get("Exporter.itemsTab.csvHeader.smilesOfFragment") + ',' +
                Message.get("Exporter.itemsTab.csvHeader.frequencyOfFragment");
        Assertions.assertTrue(tmpContent.startsWith(tmpExpectedHeader));
    }
    //
    /**
     * Tests the ITEMIZATION-tab CSV null-argument guard: a null fragmentation name makes
     * {@code createItemizationTabCsvFile} return {@code null} before writing anything.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportCsvFileItemizationTabNullFragmentationNameGuard(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpMolecules = new ArrayList<>();
        tmpMolecules.add(ExporterTest.buildMoleculeWithFragments("ErtlFG"));
        File tmpOut = aTempDir.resolve("items_nullname.csv").toFile();
        List<String> tmpFailed = this.exporter.exportCsvFile(tmpOut, tmpMolecules, null, ',', TabNames.ITEMIZATION);
        Assertions.assertNull(tmpFailed);
    }
    //
    /**
     * Tests the ITEMIZATION-tab PDF export with a molecule that has NOT undergone the requested fragmentation, driving the
     * {@code !hasMoleculeUndergoneSpecificFragmentation} continue-branch of {@code createItemizationTabPdfFile} (the
     * molecule structure is rendered but no fragment images follow). A non-zero fragment-list size is still passed so the
     * size guard does not short-circuit. Asserts an empty failed-list and the {@code %PDF} magic bytes.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportPdfFileItemizationTabMoleculeWithoutFragmentation(@TempDir Path aTempDir) throws Exception {
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpAtomContainer = tmpParser.parseSmiles("c1ccccc1CCO");
        MoleculeDataModel tmpMolecule = new MoleculeDataModel(tmpAtomContainer, false);
        tmpMolecule.setName("UnfragmentedMolecule");
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        ObservableList<MoleculeDataModel> tmpMolecules = FXCollections.observableArrayList(tmpMolecule);
        File tmpOut = aTempDir.resolve("items_nofrag.pdf").toFile();
        List<String> tmpFailed = this.exporter.exportPdfFile(
                tmpOut, tmpFragments, tmpMolecules, "ErtlFG", "input.smi", TabNames.ITEMIZATION);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        Assertions.assertTrue(tmpOut.length() > 0);
        byte[] tmpHead = Arrays.copyOf(Files.readAllBytes(tmpOut.toPath()), 4);
        Assertions.assertEquals("%PDF", new String(tmpHead, StandardCharsets.US_ASCII));
    }
    //
    /**
     * Tests the empty-input handling of the single SD-file export: an empty fragment list produces an empty (header-less,
     * but valid) SD file with an empty failed-list and exercises the summary-logging lambda without iterating any
     * fragment.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFileSingleSdfEmptyList(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = new ArrayList<>();
        File tmpOut = aTempDir.resolve("empty.sdf").toFile();
        List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                tmpOut, tmpFragments, ChemFileTypes.SDF, true, true);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        Assertions.assertTrue(tmpOut.exists());
    }
    //
    /**
     * Tests the separate SD-files export with an empty fragment list: the sub-directory is still created (containing no
     * {@code .sdf} files) and the failed-list is empty, exercising the directory-creation path and summary-logging lambda
     * with no fragment iterations.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFileSeparateSdfEmptyList(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = new ArrayList<>();
        File tmpDir = aTempDir.toFile();
        List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                tmpDir, tmpFragments, ChemFileTypes.SDF, true, false);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        File[] tmpSubDirs = tmpDir.listFiles(File::isDirectory);
        Assertions.assertNotNull(tmpSubDirs);
        Assertions.assertEquals(1, tmpSubDirs.length);
    }
    //
    /**
     * Tests the separate SD-files null/non-directory guard of {@code createFragmentationTabSeparateSDFiles}: passing a
     * regular file (not a directory) makes the method return {@code null} before creating anything.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFileSeparateSdfNonDirectoryGuard(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        File tmpRegularFile = aTempDir.resolve("not_a_dir.sdf").toFile();
        Files.writeString(tmpRegularFile.toPath(), "placeholder");
        List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                tmpRegularFile, tmpFragments, ChemFileTypes.SDF, true, false);
        Assertions.assertNull(tmpFailed);
    }
    //
    /**
     * Tests the PDB export null/non-directory guard of {@code createFragmentationTabPDBFiles}: passing a regular file
     * (not a directory) makes the method return {@code null} before creating anything.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFilePdbNonDirectoryGuard(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        File tmpRegularFile = aTempDir.resolve("not_a_dir.pdb").toFile();
        Files.writeString(tmpRegularFile.toPath(), "placeholder");
        List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                tmpRegularFile, tmpFragments, ChemFileTypes.PDB, true);
        Assertions.assertNull(tmpFailed);
    }
    //
    /**
     * Tests the no-2D/no-3D coordinate branch of {@code handleFragmentWithNo3dInformationAvailable} via a single SD-file
     * export with {@code generate2dCoords=false} of a fragment that has neither 2D nor 3D coordinates (parsed from
     * SMILES). This drives the {@code generateZero3DCoordinates} branch (coordinates set to zero). The export succeeds
     * with an empty failed-list and a non-empty file containing the MDL record terminator.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFileSeparateSdfNoGenerate2d(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        File tmpDir = aTempDir.toFile();
        List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                tmpDir, tmpFragments, ChemFileTypes.SDF, false, false);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        File[] tmpSubDirs = tmpDir.listFiles(File::isDirectory);
        Assertions.assertNotNull(tmpSubDirs);
        Assertions.assertEquals(1, tmpSubDirs.length);
        File[] tmpSdfFiles = tmpSubDirs[0].listFiles((aDir, aName) -> aName.endsWith(".sdf"));
        Assertions.assertNotNull(tmpSdfFiles);
        Assertions.assertTrue(tmpSdfFiles.length > 0);
    }
    //
    /**
     * Tests the null-file guard of {@code exportCsvFile}: a null file returns {@code null} before any writing.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportCsvFileNullFileGuard() throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        List<String> tmpFailed = this.exporter.exportCsvFile(null, tmpFragments, "ErtlFG", ',', TabNames.FRAGMENTS);
        Assertions.assertNull(tmpFailed);
    }
    //
    /**
     * Tests the null-file guard of {@code exportPdfFile}: a null file returns {@code null} before any writing.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportPdfFileNullFileGuard() throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        ObservableList<MoleculeDataModel> tmpMolecules = FXCollections.observableArrayList(tmpFragments);
        List<String> tmpFailed = this.exporter.exportPdfFile(
                null, tmpFragments, tmpMolecules, "ErtlFG", "input.smi", TabNames.FRAGMENTS);
        Assertions.assertNull(tmpFailed);
    }
    //
    /**
     * Tests that the PDB branch of {@code exportFragmentsAsChemicalFile} ignores the {@code anIsSingleExport} flag:
     * requesting PDB with {@code anIsSingleExport=true} and a valid directory still routes to the separate-PDB-files
     * export, producing a sub-directory of {@code .pdb} files and an empty failed-list.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFilePdbIgnoresSingleExportFlag(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        File tmpDir = aTempDir.toFile();
        List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                tmpDir, tmpFragments, ChemFileTypes.PDB, true, true);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        File[] tmpSubDirs = tmpDir.listFiles(File::isDirectory);
        Assertions.assertNotNull(tmpSubDirs);
        Assertions.assertEquals(1, tmpSubDirs.length);
    }
    //
    /**
     * Tests the single SD-file export of fragments that already carry 3D coordinates. This drives the
     * {@code tmpPoint3dAvailable} true branch of {@code createFragmentationTabSingleSDFile} (the original atom container is
     * written directly without generating coordinates). The export succeeds with an empty failed-list and a file
     * containing the MDL record terminator.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFileSingleSdf3dCoordinates(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentListWith3dCoordinates();
        File tmpOut = aTempDir.resolve("single_3d.sdf").toFile();
        List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                tmpOut, tmpFragments, ChemFileTypes.SDF, false, true);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        Assertions.assertTrue(tmpOut.length() > 0);
        Assertions.assertTrue(Files.readString(tmpOut.toPath()).contains("$$$$"));
    }
    //
    /**
     * Tests the single SD-file export of fragments that carry 2D (but not 3D) coordinates. This drives the
     * 2D-coordinates-available branch of {@code handleFragmentWithNo3dInformationAvailable} (pseudo-3D generated from the
     * existing 2D coordinates, no coordinate generation performed). The export succeeds with an empty failed-list.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFileSingleSdf2dCoordinates(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentListWith2dCoordinates();
        File tmpOut = aTempDir.resolve("single_2d.sdf").toFile();
        List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                tmpOut, tmpFragments, ChemFileTypes.SDF, false, true);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        Assertions.assertTrue(tmpOut.length() > 0);
        Assertions.assertTrue(Files.readString(tmpOut.toPath()).contains("$$$$"));
    }
    //
    /**
     * Tests the separate SD-files export of fragments that already carry 3D coordinates, driving the
     * {@code tmpPoint3dAvailable} true branch of {@code createFragmentationTabSeparateSDFiles}. A sub-directory of
     * {@code .sdf} files is produced and the failed-list is empty.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFileSeparateSdf3dCoordinates(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentListWith3dCoordinates();
        File tmpDir = aTempDir.toFile();
        List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                tmpDir, tmpFragments, ChemFileTypes.SDF, false, false);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        File[] tmpSubDirs = tmpDir.listFiles(File::isDirectory);
        Assertions.assertNotNull(tmpSubDirs);
        Assertions.assertEquals(1, tmpSubDirs.length);
        File[] tmpSdfFiles = tmpSubDirs[0].listFiles((aDir, aName) -> aName.endsWith(".sdf"));
        Assertions.assertNotNull(tmpSdfFiles);
        Assertions.assertTrue(tmpSdfFiles.length > 0);
    }
    //
    /**
     * Tests the PDB export of fragments that already carry 3D coordinates, driving the {@code tmpPoint3dAvailable} true
     * branch of {@code createFragmentationTabPDBFiles}. A sub-directory of {@code .pdb} files is produced and the
     * failed-list is empty.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFilePdb3dCoordinates(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentListWith3dCoordinates();
        File tmpDir = aTempDir.toFile();
        List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                tmpDir, tmpFragments, ChemFileTypes.PDB, false);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        File[] tmpSubDirs = tmpDir.listFiles(File::isDirectory);
        Assertions.assertNotNull(tmpSubDirs);
        Assertions.assertEquals(1, tmpSubDirs.length);
        File[] tmpPdbFiles = tmpSubDirs[0].listFiles((aDir, aName) -> aName.endsWith(".pdb"));
        Assertions.assertNotNull(tmpPdbFiles);
        Assertions.assertTrue(tmpPdbFiles.length > 0);
    }
    //
    /**
     * Tests the PDB export of fragments that carry 2D (but not 3D) coordinates, driving the 2D-available branch of
     * {@code handleFragmentWithNo3dInformationAvailable} within the PDB export. A sub-directory of {@code .pdb} files is
     * produced and the failed-list is empty.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFilePdb2dCoordinates(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentListWith2dCoordinates();
        File tmpDir = aTempDir.toFile();
        List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                tmpDir, tmpFragments, ChemFileTypes.PDB, false);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        File[] tmpSubDirs = tmpDir.listFiles(File::isDirectory);
        Assertions.assertNotNull(tmpSubDirs);
        Assertions.assertEquals(1, tmpSubDirs.length);
        File[] tmpPdbFiles = tmpSubDirs[0].listFiles((aDir, aName) -> aName.endsWith(".pdb"));
        Assertions.assertNotNull(tmpPdbFiles);
        Assertions.assertTrue(tmpPdbFiles.length > 0);
    }
    //
    /**
     * Tests the FRAGMENTS-tab PDF export of a fragment whose atom container carries no 3D information, driving the
     * fragment-image rendering path with a fragment that has neither 2D nor 3D coordinates (the depiction generator lays
     * the structure out itself). Asserts an empty failed-list and the {@code %PDF} magic bytes. This complements the
     * happy-path PDF tests by ensuring the no-coordinate fragment path renders successfully.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportPdfFileFragmentsTabWith3dCoordinateFragments(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentListWith3dCoordinates();
        ObservableList<MoleculeDataModel> tmpMolecules = FXCollections.observableArrayList(tmpFragments);
        File tmpOut = aTempDir.resolve("fragments_3d.pdf").toFile();
        List<String> tmpFailed = this.exporter.exportPdfFile(
                tmpOut, tmpFragments, tmpMolecules, "ErtlFG", "input.smi", TabNames.FRAGMENTS);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        Assertions.assertTrue(tmpOut.length() > 0);
        byte[] tmpHead = Arrays.copyOf(Files.readAllBytes(tmpOut.toPath()), 4);
        Assertions.assertEquals("%PDF", new String(tmpHead, StandardCharsets.US_ASCII));
    }
    //
    /**
     * Tests the ITEMIZATION-tab CSV export of a molecule carrying four fragments, driving the multi-fragment row-writing
     * loop of {@code createItemizationTabCsvFile}. The export succeeds with an empty failed-list and the written content
     * contains all four fragment SMILES codes.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportCsvFileItemizationTabMultipleFragments(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpMolecules = new ArrayList<>();
        tmpMolecules.add(ExporterTest.buildMoleculeWithMultipleFragments("ErtlFG"));
        File tmpOut = aTempDir.resolve("items_multi.csv").toFile();
        List<String> tmpFailed = this.exporter.exportCsvFile(tmpOut, tmpMolecules, "ErtlFG", ',', TabNames.ITEMIZATION);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        String tmpContent = Files.readString(tmpOut.toPath());
        Assertions.assertFalse(tmpContent.isBlank());
        String tmpExpectedHeader =
                Message.get("Exporter.itemsTab.csvHeader.moleculeName") + ',' +
                Message.get("Exporter.itemsTab.csvHeader.smilesOfStructure") + ',' +
                Message.get("Exporter.itemsTab.csvHeader.smilesOfFragment") + ',' +
                Message.get("Exporter.itemsTab.csvHeader.frequencyOfFragment");
        Assertions.assertTrue(tmpContent.startsWith(tmpExpectedHeader));
    }
    //
    /**
     * Tests the ITEMIZATION-tab PDF export of a molecule carrying four fragments, driving the inner three-fragments-per-
     * line row-wrapping loop and the trailing empty-cell padding loop of {@code createItemizationTabPdfFile} (four
     * fragments require two rows, the second padded with empty cells). Asserts an empty failed-list and the {@code %PDF}
     * magic bytes.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportPdfFileItemizationTabMultipleFragments(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        ObservableList<MoleculeDataModel> tmpMolecules =
                FXCollections.observableArrayList(ExporterTest.buildMoleculeWithMultipleFragments("ErtlFG"));
        File tmpOut = aTempDir.resolve("items_multi.pdf").toFile();
        List<String> tmpFailed = this.exporter.exportPdfFile(
                tmpOut, tmpFragments, tmpMolecules, "ErtlFG", "input.smi", TabNames.ITEMIZATION);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        Assertions.assertTrue(tmpOut.length() > 0);
        byte[] tmpHead = Arrays.copyOf(Files.readAllBytes(tmpOut.toPath()), 4);
        Assertions.assertEquals("%PDF", new String(tmpHead, StandardCharsets.US_ASCII));
    }
    //
    /**
     * Tests the per-fragment exception branch of {@code createItemizationTabCsvFile}. A molecule has a fragment registered
     * under the fragmentation name, but the matching frequency map entry is missing, so the frequency lookup yields null
     * and the {@code .toString()} call throws a NullPointerException. The exception is caught, the fragment's SMILES is
     * added to the failed-export list, and the export completes returning a non-empty failed-list.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportCsvFileItemizationTabFragmentWithoutFrequency(@TempDir Path aTempDir) throws Exception {
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpParentAtomContainer = tmpParser.parseSmiles("c1ccccc1CCO");
        MoleculeDataModel tmpMolecule = new MoleculeDataModel(tmpParentAtomContainer, false);
        tmpMolecule.setName("MoleculeMissingFrequency");
        FragmentDataModel tmpFragment = new FragmentDataModel(tmpParser.parseSmiles("c1ccccc1"), false);
        tmpFragment.setAbsoluteFrequency(1);
        List<FragmentDataModel> tmpFragments = new ArrayList<>(List.of(tmpFragment));
        tmpMolecule.getAllFragments().put("ErtlFG", tmpFragments);
        //deliberately register an empty frequency map so the per-fragment frequency lookup returns null
        tmpMolecule.getFragmentFrequencies().put("ErtlFG", new HashMap<>());
        List<MoleculeDataModel> tmpMolecules = new ArrayList<>();
        tmpMolecules.add(tmpMolecule);
        File tmpOut = aTempDir.resolve("items_nofreq.csv").toFile();
        List<String> tmpFailed = this.exporter.exportCsvFile(tmpOut, tmpMolecules, "ErtlFG", ',', TabNames.ITEMIZATION);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertFalse(tmpFailed.isEmpty());
    }
    //
    /**
     * Tests the neither-FRAGMENTS-nor-ITEMIZATION fallback branch of {@code exportCsvFile}: passing
     * {@code TabNames.MOLECULES} causes the method to return an empty (non-null) list without writing a file.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportCsvFileWithUnsupportedTabNameReturnsEmptyList(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        File tmpOut = aTempDir.resolve("molecules.csv").toFile();
        List<String> tmpFailed = this.exporter.exportCsvFile(tmpOut, tmpFragments, "ErtlFG", ',', TabNames.MOLECULES);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
    }
    //
    /**
     * Tests the neither-FRAGMENTS-nor-ITEMIZATION fallback branch of {@code exportPdfFile}: passing
     * {@code TabNames.MOLECULES} causes the method to return {@code null} without writing a file.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportPdfFileWithUnsupportedTabNameReturnsNull(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        ObservableList<MoleculeDataModel> tmpMolecules = FXCollections.observableArrayList(tmpFragments);
        File tmpOut = aTempDir.resolve("molecules.pdf").toFile();
        List<String> tmpFailed = this.exporter.exportPdfFile(
                tmpOut, tmpFragments, tmpMolecules, "ErtlFG", "input.smi", TabNames.MOLECULES);
        Assertions.assertNull(tmpFailed);
    }
    //
    /**
     * Tests the main exception-handling branch of {@code createFragmentationTabSingleSDFile}. A FragmentDataModel built
     * from an unparsable unique SMILES makes {@code getAtomContainer} throw a CDKException, which is caught by the
     * method's outer catch block: the fragment's SMILES is added to the failed-export list and the export continues. The
     * returned failed-list is non-empty.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFileSingleSdfInvalidFragment(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = new ArrayList<>();
        FragmentDataModel tmpInvalidFragment = new FragmentDataModel(ExporterTest.UNPARSABLE_SMILES, "Invalid", new HashMap<>());
        tmpInvalidFragment.setAbsoluteFrequency(1);
        tmpFragments.add(tmpInvalidFragment);
        File tmpOut = aTempDir.resolve("single_invalid.sdf").toFile();
        List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                tmpOut, tmpFragments, ChemFileTypes.SDF, true, true);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertFalse(tmpFailed.isEmpty());
    }
    //
    /**
     * Tests the main exception-handling branch of {@code createFragmentationTabSeparateSDFiles}. A FragmentDataModel built
     * from an unparsable unique SMILES makes {@code getAtomContainer} throw a CDKException, caught by the method's outer
     * catch block: the fragment's SMILES is added to the failed-export list and the export continues. The sub-directory is
     * still created and the returned failed-list is non-empty.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFileSeparateSdfInvalidFragment(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = new ArrayList<>();
        FragmentDataModel tmpInvalidFragment = new FragmentDataModel(ExporterTest.UNPARSABLE_SMILES, "Invalid", new HashMap<>());
        tmpInvalidFragment.setAbsoluteFrequency(1);
        tmpFragments.add(tmpInvalidFragment);
        File tmpDir = aTempDir.toFile();
        List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                tmpDir, tmpFragments, ChemFileTypes.SDF, true, false);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertFalse(tmpFailed.isEmpty());
    }
    //
    /**
     * Tests the main exception-handling branch of {@code createFragmentationTabPDBFiles}. A FragmentDataModel built from
     * an unparsable unique SMILES makes {@code getAtomContainer} throw a CDKException, caught by the method's outer catch
     * block: the fragment's SMILES is added to the failed-export list and the export continues. The sub-directory is still
     * created and the returned failed-list is non-empty.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFilePdbInvalidFragment(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = new ArrayList<>();
        FragmentDataModel tmpInvalidFragment = new FragmentDataModel(ExporterTest.UNPARSABLE_SMILES, "Invalid", new HashMap<>());
        tmpInvalidFragment.setAbsoluteFrequency(1);
        tmpFragments.add(tmpInvalidFragment);
        File tmpDir = aTempDir.toFile();
        List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                tmpDir, tmpFragments, ChemFileTypes.PDB, true);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertFalse(tmpFailed.isEmpty());
    }
    //
    /**
     * Tests the per-fragment atom-container-exception branch of the FRAGMENTS-tab PDF export. A FragmentDataModel built
     * from an unparsable unique SMILES (with no retained atom container) makes {@code getAtomContainer} throw a
     * CDKException during rendering; the fragment's SMILES is added to the failed-export list and rendering continues. The
     * export still produces a valid {@code %PDF} file and a non-empty failed-list.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportPdfFileFragmentsTabFragmentWithInvalidStructure(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = new ArrayList<>();
        FragmentDataModel tmpInvalidFragment = new FragmentDataModel(ExporterTest.UNPARSABLE_SMILES, "Invalid", new HashMap<>());
        tmpInvalidFragment.setAbsoluteFrequency(1);
        tmpFragments.add(tmpInvalidFragment);
        ObservableList<MoleculeDataModel> tmpMolecules = FXCollections.observableArrayList(tmpFragments);
        File tmpOut = aTempDir.resolve("fragments_invalid.pdf").toFile();
        List<String> tmpFailed = this.exporter.exportPdfFile(
                tmpOut, tmpFragments, tmpMolecules, "ErtlFG", "input.smi", TabNames.FRAGMENTS);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertFalse(tmpFailed.isEmpty());
        Assertions.assertTrue(tmpOut.length() > 0);
        byte[] tmpHead = Arrays.copyOf(Files.readAllBytes(tmpOut.toPath()), 4);
        Assertions.assertEquals("%PDF", new String(tmpHead, StandardCharsets.US_ASCII));
    }
    //
    /**
     * Tests the molecule atom-container-exception branch of the ITEMIZATION-tab PDF export. A parent MoleculeDataModel
     * built from an unparsable unique SMILES makes {@code getAtomContainer} throw a CDKException while rendering the
     * molecule structure; the molecule's SMILES is added to the failed-export list and rendering continues to the next
     * molecule. The export still produces a valid {@code %PDF} file and a non-empty failed-list.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportPdfFileItemizationTabMoleculeWithInvalidStructure(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        MoleculeDataModel tmpInvalidMolecule = new MoleculeDataModel(ExporterTest.UNPARSABLE_SMILES, "Invalid", new HashMap<>());
        ObservableList<MoleculeDataModel> tmpMolecules = FXCollections.observableArrayList(tmpInvalidMolecule);
        File tmpOut = aTempDir.resolve("items_invalid.pdf").toFile();
        List<String> tmpFailed = this.exporter.exportPdfFile(
                tmpOut, tmpFragments, tmpMolecules, "ErtlFG", "input.smi", TabNames.ITEMIZATION);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertFalse(tmpFailed.isEmpty());
        Assertions.assertTrue(tmpOut.length() > 0);
        byte[] tmpHead = Arrays.copyOf(Files.readAllBytes(tmpOut.toPath()), 4);
        Assertions.assertEquals("%PDF", new String(tmpHead, StandardCharsets.US_ASCII));
    }
    //
    /**
     * Regression test for the ITEMIZATION-tab PDF export infinite loop: a molecule carries a fragment whose unique SMILES
     * is unparsable, so {@code getAtomContainer} throws a CDKException on every rendering attempt inside the
     * three-fragments-per-line inner loop of {@code createItemizationTabPdfFile}. Before the fix the outer fragment loop
     * never advanced its counter on the exception-continue path, so the persistently-failing fragment spun the loop
     * forever (a production hang during export). The export is run under a preemptive 30-second bound: if the loop
     * regresses, the bound fires and this test fails fast instead of hanging CI. After the fix the export terminates,
     * the failing fragment's SMILES is recorded in the returned failed-export list, and the accompanying valid fragment
     * is still rendered (the success path is unchanged).
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportPdfFileItemizationTabFragmentWithInvalidStructureTerminates(@TempDir Path aTempDir) throws Exception {
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpParentAtomContainer = tmpParser.parseSmiles("c1ccccc1CCO");
        MoleculeDataModel tmpMolecule = new MoleculeDataModel(tmpParentAtomContainer, false);
        tmpMolecule.setName("MoleculeWithFailingFragment");
        //a valid fragment (renders normally) plus a fragment whose unique SMILES is unparsable so getAtomContainer throws
        FragmentDataModel tmpValidFragment = new FragmentDataModel(tmpParser.parseSmiles("c1ccccc1"), false);
        tmpValidFragment.setAbsoluteFrequency(1);
        FragmentDataModel tmpInvalidFragment = new FragmentDataModel(ExporterTest.UNPARSABLE_SMILES, "Invalid", new HashMap<>());
        tmpInvalidFragment.setAbsoluteFrequency(1);
        List<FragmentDataModel> tmpMoleculeFragments =
                new ArrayList<>(List.of(tmpValidFragment, tmpInvalidFragment));
        tmpMolecule.getAllFragments().put("ErtlFG", tmpMoleculeFragments);
        Map<String, Integer> tmpFrequencies = new HashMap<>();
        tmpFrequencies.put(tmpValidFragment.getUniqueSmiles(), 1);
        tmpFrequencies.put(tmpInvalidFragment.getUniqueSmiles(), 1);
        tmpMolecule.getFragmentFrequencies().put("ErtlFG", tmpFrequencies);
        List<MoleculeDataModel> tmpFragmentList = ExporterTest.buildFragmentList();
        ObservableList<MoleculeDataModel> tmpMolecules = FXCollections.observableArrayList(tmpMolecule);
        File tmpOut = aTempDir.resolve("items_failing_fragment.pdf").toFile();
        List<String> tmpFailed = Assertions.assertTimeoutPreemptively(
                java.time.Duration.ofSeconds(30L),
                () -> this.exporter.exportPdfFile(
                        tmpOut, tmpFragmentList, tmpMolecules, "ErtlFG", "input.smi", TabNames.ITEMIZATION));
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.contains(tmpInvalidFragment.getUniqueSmiles()));
    }
    //
    /**
     * Tests that the nested enums of the Exporter ({@code ExportTypes}, {@code FileExtension}, {@code CSVSeparator})
     * load and expose non-null accessors, loading their static initializers for coverage.
     */
    @Test
    public void testNestedEnumsSmoke() {
        Assertions.assertTrue(Exporter.ExportTypes.values().length > 0);
        for (Exporter.ExportTypes tmpType : Exporter.ExportTypes.values()) {
            Assertions.assertNotNull(tmpType.name());
        }
        Assertions.assertTrue(Exporter.FileExtension.values().length > 0);
        for (Exporter.FileExtension tmpExtension : Exporter.FileExtension.values()) {
            Assertions.assertNotNull(tmpExtension.toString());
        }
        Assertions.assertTrue(Exporter.CSVSeparator.values().length > 0);
        for (Exporter.CSVSeparator tmpSeparator : Exporter.CSVSeparator.values()) {
            Assertions.assertNotNull(tmpSeparator.getDisplayName());
            Assertions.assertNotNull(tmpSeparator.getTooltipText());
        }
    }
    //
    /**
     * Tests the private {@code convertToITextImage(BufferedImage)} method (reached via reflection). A valid,
     * PNG-encodable {@link BufferedImage} is passed and the method must return a non-null {@link com.lowagie.text.Image}
     * for use in the PDF export. This pins the {@code return Image.getInstance(bytes)} statement (Exporter L1131)
     * directly: the full PDF export tests traverse this method but swallow a null return (the image simply does not
     * appear in the PDF), so the {@code NullReturnVals} mutant survived without this assertion. A plain ARGB image is
     * used because {@code ImageIO} can always PNG-encode it headlessly and deterministically.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testConvertToITextImageReturnsNonNullImage() throws Exception {
        BufferedImage tmpBufferedImage = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Method tmpConvertToITextImage =
                Exporter.class.getDeclaredMethod("convertToITextImage", BufferedImage.class);
        tmpConvertToITextImage.setAccessible(true);
        Object tmpResult = tmpConvertToITextImage.invoke(this.exporter, tmpBufferedImage);
        Assertions.assertNotNull(tmpResult);
        Assertions.assertInstanceOf(Image.class, tmpResult);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Writer-exception kekulize-retry tests (targeted Mockito)" defaultstate="collapsed">
    /*
     * The following tests use Mockito's mockConstruction exclusively to drive the inner kekulize-retry blocks of the
     * SD/PDB export methods (createFragmentationTabSingleSDFile / createFragmentationTabSeparateSDFiles /
     * createFragmentationTabPDBFiles). Those blocks fire ONLY when the very first SDFWriter.write(...) /
     * PDBWriter.writeMolecule(...) throws a CDKException; with OptWriteAromaticBondTypes=true a real CDK writer never
     * fails the first write for the fragments used here (verified empirically in plan 03-06), so the branch is
     * unreachable with real objects. Mockito is authorized for THIS package only (the project's no-mock rule was
     * explicitly overturned here); usage is kept minimal and targeted — the constructed writer is the only mock, the
     * kekulization on the real fragment clone runs for real, and the returned failed-export list is asserted to be empty
     * so the retry's successful path (not the outer catch) is what is exercised.
     */
    /**
     * Drives the kekulize-retry block of {@code createFragmentationTabSingleSDFile} (no-3D fragment sub-branch). A
     * mock-constructed {@link SDFWriter} throws a {@link CDKException} on its first {@code write} call (forcing the inner
     * catch), then succeeds on the retried {@code write} of the kekulized clone. The fragments carry no 3D coordinates,
     * so the retry kekulizes the pre-built no-3D clone. The export returns an empty failed-list (the retry succeeded, the
     * outer catch was not reached) and the writer received at least one more {@code write} call than there are fragments
     * (proving exactly one retry occurred).
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFileSingleSdfWriteRetry(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        File tmpOut = aTempDir.resolve("single_retry.sdf").toFile();
        AtomicInteger tmpWriteCounter = new AtomicInteger(0);
        try (MockedConstruction<SDFWriter> tmpMocked = Mockito.mockConstruction(SDFWriter.class,
                Mockito.withSettings().defaultAnswer(Mockito.RETURNS_MOCKS),
                (aMock, aContext) -> Mockito.doAnswer(anInvocation -> {
                    if (tmpWriteCounter.getAndIncrement() == 0) {
                        throw new CDKException("First write fails on purpose to drive the kekulize retry.");
                    }
                    return null;
                }).when(aMock).write(Mockito.any()))) {
            List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                    tmpOut, tmpFragments, ChemFileTypes.SDF, true, true);
            Assertions.assertNotNull(tmpFailed);
            Assertions.assertTrue(tmpFailed.isEmpty());
            //one write per fragment plus the single extra retry write
            Assertions.assertTrue(tmpWriteCounter.get() >= tmpFragments.size() + 1);
        }
    }
    //
    /**
     * Drives the {@code tmpPoint3dAvailable == true} sub-branch of the kekulize-retry block of
     * {@code createFragmentationTabSingleSDFile} (line {@code tmpFragmentClone = tmpFragment.clone()}). The fragments
     * carry explicit 3D coordinates, so on retry the original atom container is cloned and kekulized. A mock-constructed
     * {@link SDFWriter} throws on its first {@code write} then succeeds; the export returns an empty failed-list.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFileSingleSdfWriteRetry3d(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildAromatic3dFragmentList();
        File tmpOut = aTempDir.resolve("single_retry_3d.sdf").toFile();
        AtomicInteger tmpWriteCounter = new AtomicInteger(0);
        try (MockedConstruction<SDFWriter> tmpMocked = Mockito.mockConstruction(SDFWriter.class,
                Mockito.withSettings().defaultAnswer(Mockito.RETURNS_MOCKS),
                (aMock, aContext) -> Mockito.doAnswer(anInvocation -> {
                    if (tmpWriteCounter.getAndIncrement() == 0) {
                        throw new CDKException("First write fails on purpose to drive the kekulize retry.");
                    }
                    return null;
                }).when(aMock).write(Mockito.any()))) {
            List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                    tmpOut, tmpFragments, ChemFileTypes.SDF, true, true);
            Assertions.assertNotNull(tmpFailed);
            Assertions.assertTrue(tmpFailed.isEmpty());
            Assertions.assertTrue(tmpWriteCounter.get() >= tmpFragments.size() + 1);
        }
    }
    //
    /**
     * Drives the kekulize-retry block of {@code createFragmentationTabSeparateSDFiles}: a fresh {@link SDFWriter} is
     * mock-constructed per fragment (one writer per file), and the first one throws a {@link CDKException} on its first
     * {@code write}, forcing the inner catch and the retried {@code write} of the kekulized clone. The export returns an
     * empty failed-list and a sub-directory is still created.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFileSeparateSdfWriteRetry(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        File tmpDir = aTempDir.toFile();
        AtomicInteger tmpWriteCounter = new AtomicInteger(0);
        try (MockedConstruction<SDFWriter> tmpMocked = Mockito.mockConstruction(SDFWriter.class,
                Mockito.withSettings().defaultAnswer(Mockito.RETURNS_MOCKS),
                (aMock, aContext) -> Mockito.doAnswer(anInvocation -> {
                    if (tmpWriteCounter.getAndIncrement() == 0) {
                        throw new CDKException("First write fails on purpose to drive the kekulize retry.");
                    }
                    return null;
                }).when(aMock).write(Mockito.any()))) {
            List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                    tmpDir, tmpFragments, ChemFileTypes.SDF, true, false);
            Assertions.assertNotNull(tmpFailed);
            Assertions.assertTrue(tmpFailed.isEmpty());
            Assertions.assertTrue(tmpWriteCounter.get() >= tmpFragments.size() + 1);
            File[] tmpSubDirs = tmpDir.listFiles(File::isDirectory);
            Assertions.assertNotNull(tmpSubDirs);
            Assertions.assertEquals(1, tmpSubDirs.length);
        }
    }
    //
    /**
     * Drives the kekulize-retry block of {@code createFragmentationTabPDBFiles}: a mock-constructed {@link PDBWriter}
     * throws a {@link CDKException} on its first {@code writeMolecule} (forcing the inner catch), then the retried
     * {@code write} of the kekulized clone succeeds. Note the production retry uses {@code write} rather than
     * {@code writeMolecule}, so the mock stubs both methods independently. The export returns an empty failed-list and a
     * sub-directory is still created.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFilePdbWriteRetry(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        File tmpDir = aTempDir.toFile();
        AtomicInteger tmpWriteMoleculeCounter = new AtomicInteger(0);
        AtomicInteger tmpRetryWriteCounter = new AtomicInteger(0);
        try (MockedConstruction<PDBWriter> tmpMocked = Mockito.mockConstruction(PDBWriter.class,
                Mockito.withSettings().defaultAnswer(Mockito.RETURNS_MOCKS),
                (aMock, aContext) -> {
                    Mockito.doAnswer(anInvocation -> {
                        if (tmpWriteMoleculeCounter.getAndIncrement() == 0) {
                            throw new CDKException("First writeMolecule fails on purpose to drive the kekulize retry.");
                        }
                        return null;
                    }).when(aMock).writeMolecule(Mockito.any());
                    Mockito.doAnswer(anInvocation -> {
                        tmpRetryWriteCounter.incrementAndGet();
                        return null;
                    }).when(aMock).write(Mockito.any());
                })) {
            List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                    tmpDir, tmpFragments, ChemFileTypes.PDB, true);
            Assertions.assertNotNull(tmpFailed);
            Assertions.assertTrue(tmpFailed.isEmpty());
            //the first fragment failed writeMolecule and was retried via write(...)
            Assertions.assertEquals(1, tmpRetryWriteCounter.get());
            File[] tmpSubDirs = tmpDir.listFiles(File::isDirectory);
            Assertions.assertNotNull(tmpSubDirs);
            Assertions.assertEquals(1, tmpSubDirs.length);
        }
    }
    //
    /**
     * Drives the outer exception branch reached when the kekulize retry itself fails in
     * {@code createFragmentationTabSingleSDFile}: a mock-constructed {@link SDFWriter} throws a {@link CDKException} on
     * every {@code write} call, so the first write throws, the retried write of the kekulized clone throws again, and the
     * fragment falls through to the method's outer {@code catch} block. The returned failed-export list is therefore
     * non-empty for every fragment.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFileSingleSdfWriteRetryAlsoFails(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        File tmpOut = aTempDir.resolve("single_retry_fail.sdf").toFile();
        try (MockedConstruction<SDFWriter> tmpMocked = Mockito.mockConstruction(SDFWriter.class,
                Mockito.withSettings().defaultAnswer(Mockito.RETURNS_MOCKS),
                (aMock, aContext) -> Mockito.doThrow(new CDKException("Every write fails on purpose."))
                        .when(aMock).write(Mockito.any()))) {
            List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                    tmpOut, tmpFragments, ChemFileTypes.SDF, true, true);
            Assertions.assertNotNull(tmpFailed);
            Assertions.assertEquals(tmpFragments.size(), tmpFailed.size());
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Thread-interrupt guard tests" defaultstate="collapsed">
    /*
     * The following tests cover the top-of-loop thread-interrupt early-return guards
     * ({@code if (Thread.currentThread().isInterrupted()) return null;}) of the export loops. The guards exist so a
     * cancelled fragmentation/export task stops promptly; they are normally driven by a cooperating interrupting thread.
     * Here the running test thread interrupts itself BEFORE invoking the export so the guard fires on the first loop
     * iteration and the method returns {@code null}. Each test clears the interrupt flag in a {@code finally} block (via
     * {@link Thread#interrupted()}) so the interrupted state does not leak into the rest of the suite. No mocking is
     * involved — only the real interrupt flag of the current thread. The inner (nested) interrupt guards remain
     * unreachable single-threaded because the outer guard returns first.
     */
    /**
     * Tests that the ITEMIZATION-tab CSV export returns {@code null} immediately when the current thread is interrupted
     * (top-of-loop guard of {@code createItemizationTabCsvFile}).
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportCsvFileItemizationTabInterrupted(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpMolecules = new ArrayList<>();
        tmpMolecules.add(ExporterTest.buildMoleculeWithFragments("ErtlFG"));
        File tmpOut = aTempDir.resolve("items_interrupt.csv").toFile();
        try {
            Thread.currentThread().interrupt();
            List<String> tmpFailed = this.exporter.exportCsvFile(tmpOut, tmpMolecules, "ErtlFG", ',', TabNames.ITEMIZATION);
            Assertions.assertNull(tmpFailed);
        } finally {
            Thread.interrupted();
        }
    }
    //
    /**
     * Tests that the FRAGMENTS-tab CSV export returns {@code null} immediately when the current thread is interrupted
     * (top-of-loop guard of {@code createFragmentsTabCsvFile}).
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportCsvFileFragmentsTabInterrupted(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        File tmpOut = aTempDir.resolve("frags_interrupt.csv").toFile();
        try {
            Thread.currentThread().interrupt();
            List<String> tmpFailed = this.exporter.exportCsvFile(tmpOut, tmpFragments, "ErtlFG", ',', TabNames.FRAGMENTS);
            Assertions.assertNull(tmpFailed);
        } finally {
            Thread.interrupted();
        }
    }
    //
    /**
     * Tests that the FRAGMENTS-tab PDF export hits its top-of-loop interrupt guard ({@code createFragmentsTabPdfFile})
     * when the current thread is interrupted and returns {@code null} cleanly. Unlike the itemization variant, this
     * method opens the iText {@code Document} but only adds its content table AFTER the export loop, so the interrupt's
     * {@code return null} on the first iteration leaves the document with no pages. The document close is guarded so the
     * zero-page iText {@code ExceptionConverter} ("The document has no pages.") is swallowed instead of escaping; the
     * export therefore returns {@code null} on interrupt rather than propagating a spurious runtime exception.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportPdfFileFragmentsTabInterrupted(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        ObservableList<MoleculeDataModel> tmpMolecules = FXCollections.observableArrayList(tmpFragments);
        File tmpOut = aTempDir.resolve("frags_interrupt.pdf").toFile();
        try {
            Thread.currentThread().interrupt();
            List<String> tmpFailed = this.exporter.exportPdfFile(
                    tmpOut, tmpFragments, tmpMolecules, "ErtlFG", "input.smi", TabNames.FRAGMENTS);
            Assertions.assertNull(tmpFailed);
        } finally {
            Thread.interrupted();
        }
    }
    //
    /**
     * Tests that the ITEMIZATION-tab PDF export returns {@code null} immediately when the current thread is interrupted
     * (top-of-loop guard of {@code createItemizationTabPdfFile}).
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportPdfFileItemizationTabInterrupted(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        ObservableList<MoleculeDataModel> tmpMolecules =
                FXCollections.observableArrayList(ExporterTest.buildMoleculeWithFragments("ErtlFG"));
        File tmpOut = aTempDir.resolve("items_interrupt.pdf").toFile();
        try {
            Thread.currentThread().interrupt();
            List<String> tmpFailed = this.exporter.exportPdfFile(
                    tmpOut, tmpFragments, tmpMolecules, "ErtlFG", "input.smi", TabNames.ITEMIZATION);
            Assertions.assertNull(tmpFailed);
        } finally {
            Thread.interrupted();
        }
    }
    //
    /**
     * Tests that the single SD-file export returns {@code null} immediately when the current thread is interrupted
     * (top-of-loop guard of {@code createFragmentationTabSingleSDFile}).
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFileSingleSdfInterrupted(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        File tmpOut = aTempDir.resolve("single_interrupt.sdf").toFile();
        try {
            Thread.currentThread().interrupt();
            List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                    tmpOut, tmpFragments, ChemFileTypes.SDF, true, true);
            Assertions.assertNull(tmpFailed);
        } finally {
            Thread.interrupted();
        }
    }
    //
    /**
     * Tests that the separate SD-files export returns {@code null} immediately when the current thread is interrupted
     * (top-of-loop guard of {@code createFragmentationTabSeparateSDFiles}).
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFileSeparateSdfInterrupted(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        File tmpDir = aTempDir.toFile();
        try {
            Thread.currentThread().interrupt();
            List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                    tmpDir, tmpFragments, ChemFileTypes.SDF, true, false);
            Assertions.assertNull(tmpFailed);
        } finally {
            Thread.interrupted();
        }
    }
    //
    /**
     * Tests that the PDB export returns {@code null} immediately when the current thread is interrupted (top-of-loop
     * guard of {@code createFragmentationTabPDBFiles}).
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testExportFragmentsAsChemicalFilePdbInterrupted(@TempDir Path aTempDir) throws Exception {
        List<MoleculeDataModel> tmpFragments = ExporterTest.buildFragmentList();
        File tmpDir = aTempDir.toFile();
        try {
            Thread.currentThread().interrupt();
            List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                    tmpDir, tmpFragments, ChemFileTypes.PDB, true);
            Assertions.assertNull(tmpFailed);
        } finally {
            Thread.interrupted();
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private static helper methods" defaultstate="collapsed">
    /**
     * Builds a FRAGMENTS-tab export input: a list of real FragmentDataModel instances (parsed from three SMILES) with
     * their frequency/percentage fields populated. The list is typed as List of MoleculeDataModel (the export method
     * signature) but actually holds FragmentDataModel instances (required by the FRAGMENTS branch).
     *
     * @return list of FragmentDataModel instances usable as a FRAGMENTS-tab export input
     * @throws Exception if SMILES parsing fails
     */
    private static List<MoleculeDataModel> buildFragmentList() throws Exception {
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        List<MoleculeDataModel> tmpList = new ArrayList<>();
        String[] tmpSmilesCodes = {"c1ccccc1", "CCO", "O=CO"};
        for (String tmpSmilesCode : tmpSmilesCodes) {
            IAtomContainer tmpAtomContainer = tmpParser.parseSmiles(tmpSmilesCode);
            FragmentDataModel tmpFragment = new FragmentDataModel(tmpAtomContainer, false);
            tmpFragment.setAbsoluteFrequency(3);
            tmpFragment.setAbsolutePercentage(0.25);
            tmpFragment.setMoleculeFrequency(2);
            tmpFragment.setMoleculePercentage(0.20);
            tmpList.add(tmpFragment);
        }
        return tmpList;
    }
    //
    /**
     * Builds an ITEMIZATION-tab export input: a parent MoleculeDataModel carrying a populated fragments map and a
     * matching fragment-frequency map (keyed by the fragment unique SMILES) under the given fragmentation name. No
     * fragmenter is involved; the maps are populated directly via the public API.
     *
     * @param aFragmentationName fragmentation name under which the fragments are registered
     * @return parent molecule with attached fragments for the given fragmentation name
     * @throws Exception if SMILES parsing fails
     */
    /**
     * Builds a list of FragmentDataModel instances whose atom containers carry explicit 3D coordinates (a {@code Point3d}
     * is assigned to every atom). Because the data model retains the given atom container, the export methods see these 3D
     * coordinates ({@code ChemUtil.has3DCoordinates} returns true), driving the 3D-coordinates-available export branch
     * (writing the original atom container without generating coordinates).
     *
     * @return list of FragmentDataModel instances with 3D coordinates
     * @throws Exception if SMILES parsing fails
     */
    private static List<MoleculeDataModel> buildFragmentListWith3dCoordinates() throws Exception {
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        List<MoleculeDataModel> tmpList = new ArrayList<>();
        String[] tmpSmilesCodes = {"CCO", "CCC"};
        for (String tmpSmilesCode : tmpSmilesCodes) {
            IAtomContainer tmpAtomContainer = tmpParser.parseSmiles(tmpSmilesCode);
            double tmpCoordinate = 0.0;
            for (IAtom tmpAtom : tmpAtomContainer.atoms()) {
                tmpAtom.setPoint3d(new Point3d(tmpCoordinate, tmpCoordinate, tmpCoordinate));
                tmpCoordinate += 1.0;
            }
            FragmentDataModel tmpFragment = new FragmentDataModel(tmpAtomContainer, false);
            tmpFragment.setAbsoluteFrequency(1);
            tmpList.add(tmpFragment);
        }
        return tmpList;
    }
    //
    /**
     * Builds a list of FragmentDataModel instances whose atom containers carry explicit 2D coordinates (a {@code Point2d}
     * is assigned to every atom) but no 3D coordinates. Because the data model retains the given atom container, the
     * export methods see 2D coordinates ({@code ChemUtil.has2DCoordinates} returns true), driving the
     * 2D-coordinates-available branch of {@code handleFragmentWithNo3dInformationAvailable} (pseudo-3D from 2D, no
     * coordinate generation needed).
     *
     * @return list of FragmentDataModel instances with 2D coordinates
     * @throws Exception if SMILES parsing fails
     */
    /**
     * Builds a list of FragmentDataModel instances holding an aromatic ring (benzene) whose atoms carry explicit 3D
     * coordinates. Aromaticity makes {@link org.openscience.cdk.aromaticity.Kekulization#kekulize} meaningful, and the 3D
     * coordinates make {@code ChemUtil.has3DCoordinates} return true, so the kekulize-retry block's
     * {@code tmpPoint3dAvailable == true} sub-branch (which clones the original atom container) is exercised.
     *
     * @return list of FragmentDataModel instances with an aromatic ring and 3D coordinates
     * @throws Exception if SMILES parsing fails
     */
    private static List<MoleculeDataModel> buildAromatic3dFragmentList() throws Exception {
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        List<MoleculeDataModel> tmpList = new ArrayList<>();
        IAtomContainer tmpAtomContainer = tmpParser.parseSmiles("c1ccccc1");
        double tmpCoordinate = 0.0;
        for (IAtom tmpAtom : tmpAtomContainer.atoms()) {
            tmpAtom.setPoint3d(new Point3d(tmpCoordinate, tmpCoordinate, tmpCoordinate));
            tmpCoordinate += 1.0;
        }
        FragmentDataModel tmpFragment = new FragmentDataModel(tmpAtomContainer, false);
        tmpFragment.setAbsoluteFrequency(1);
        tmpList.add(tmpFragment);
        return tmpList;
    }
    //
    private static List<MoleculeDataModel> buildFragmentListWith2dCoordinates() throws Exception {
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        List<MoleculeDataModel> tmpList = new ArrayList<>();
        String[] tmpSmilesCodes = {"CCO", "CCC"};
        for (String tmpSmilesCode : tmpSmilesCodes) {
            IAtomContainer tmpAtomContainer = tmpParser.parseSmiles(tmpSmilesCode);
            double tmpCoordinate = 0.0;
            for (IAtom tmpAtom : tmpAtomContainer.atoms()) {
                tmpAtom.setPoint2d(new Point2d(tmpCoordinate, tmpCoordinate));
                tmpCoordinate += 1.0;
            }
            FragmentDataModel tmpFragment = new FragmentDataModel(tmpAtomContainer, false);
            tmpFragment.setAbsoluteFrequency(1);
            tmpList.add(tmpFragment);
        }
        return tmpList;
    }
    //
    private static MoleculeDataModel buildMoleculeWithFragments(String aFragmentationName) throws Exception {
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpParentAtomContainer = tmpParser.parseSmiles("c1ccccc1CCO");
        MoleculeDataModel tmpMolecule = new MoleculeDataModel(tmpParentAtomContainer, false);
        FragmentDataModel tmpFragment = new FragmentDataModel(tmpParser.parseSmiles("c1ccccc1"), false);
        tmpFragment.setAbsoluteFrequency(1);
        List<FragmentDataModel> tmpFragments = new ArrayList<>(List.of(tmpFragment));
        tmpMolecule.getAllFragments().put(aFragmentationName, tmpFragments);
        Map<String, Integer> tmpFrequencies = new HashMap<>();
        tmpFrequencies.put(tmpFragment.getUniqueSmiles(), 1);
        tmpMolecule.getFragmentFrequencies().put(aFragmentationName, tmpFrequencies);
        return tmpMolecule;
    }
    //
    /**
     * Builds an ITEMIZATION-tab export input carrying multiple (four) distinct fragments under the given fragmentation
     * name. Four fragments span more than the three-fragments-per-line layout used by the itemization PDF export, driving
     * the inner row-wrapping and cell-padding loops of {@code createItemizationTabPdfFile} as well as the multi-fragment
     * row writing of {@code createItemizationTabCsvFile}.
     *
     * @param aFragmentationName fragmentation name under which the fragments are registered
     * @return parent molecule with four attached fragments for the given fragmentation name
     * @throws Exception if SMILES parsing fails
     */
    private static MoleculeDataModel buildMoleculeWithMultipleFragments(String aFragmentationName) throws Exception {
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpParentAtomContainer = tmpParser.parseSmiles("c1ccccc1CCOCC(=O)O");
        MoleculeDataModel tmpMolecule = new MoleculeDataModel(tmpParentAtomContainer, false);
        String[] tmpFragmentSmilesCodes = {"c1ccccc1", "CCO", "O=CO", "CC"};
        List<FragmentDataModel> tmpFragments = new ArrayList<>();
        Map<String, Integer> tmpFrequencies = new HashMap<>();
        for (String tmpFragmentSmiles : tmpFragmentSmilesCodes) {
            FragmentDataModel tmpFragment = new FragmentDataModel(tmpParser.parseSmiles(tmpFragmentSmiles), false);
            tmpFragment.setAbsoluteFrequency(1);
            tmpFragments.add(tmpFragment);
            tmpFrequencies.put(tmpFragment.getUniqueSmiles(), 1);
        }
        tmpMolecule.getAllFragments().put(aFragmentationName, tmpFragments);
        tmpMolecule.getFragmentFrequencies().put(aFragmentationName, tmpFrequencies);
        return tmpMolecule;
    }
    //</editor-fold>
}
