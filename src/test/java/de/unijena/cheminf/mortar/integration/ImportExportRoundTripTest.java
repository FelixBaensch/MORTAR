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

package de.unijena.cheminf.mortar.integration;

import de.unijena.cheminf.mortar.controller.TabNames;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.io.ChemFileTypes;
import de.unijena.cheminf.mortar.model.io.Exporter;
import de.unijena.cheminf.mortar.model.io.Importer;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * INT-03 integration test wiring the Phase 3 {@link Importer} and {@link Exporter} subsystems end-to-end. It verifies
 * that molecules survive a full import &rarr; export &rarr; re-import cycle without loss of structural identity. Because
 * {@link Importer} populates every model's {@code getUniqueSmiles()} via the same {@code ChemUtil.createUniqueSmiles}
 * contract on every pass, structural identity is checkable as unique-SMILES set equality without any golden literals
 * (D-04/D-05). All three legs — SD/MOL, SMILES and CSV — perform a real round trip; the CSV one works because
 * {@code .csv} is a valid import extension that {@code Importer} routes to its SMILES reader, which picks the SMILES
 * code out of either of the first two columns of a row. Fixtures are reused in place from the {@code model/io}
 * resources by absolute classpath path (D-06), since {@code getClass().getResource} resolves relative to this test's own
 * package. The en-GB locale guard is load-bearing for Message-resolved headers.
 * <p>
 * <strong>Round-tripping is a test device here, not a MORTAR use case.</strong> The application's real data flow is
 * uni-directional: molecules are imported and fragments are exported, and the exported fragments are not meant to be
 * read back in. Feeding an export back into the importer is done purely because it makes structural identity across the
 * import and export routines checkable in one assertion; nothing in this class should be read as a statement about how
 * MORTAR is used.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class ImportExportRoundTripTest {
    //<editor-fold desc="Private final variables">
    /**
     * Importer instance under test, constructed with a real settings container.
     */
    private final Importer importer;
    /**
     * Exporter instance under test, constructed with a real settings container.
     */
    private final Exporter exporter;
    //</editor-fold>
    //
    //<editor-fold desc="Constructor">
    /**
     * Constructor. Sets the en-GB locale (load-bearing for Message-resolved export headers) and builds plain Importer and
     * Exporter instances with real, classpath-configured settings containers. Plain instances suffice because INT-03 only
     * needs the public {@link Importer#importMoleculeFile(File, boolean, boolean, boolean)} entry point.
     */
    public ImportExportRoundTripTest() {
        Locale.setDefault(Locale.of("en", "GB"));
        this.importer = new Importer(new SettingsContainer());
        this.exporter = new Exporter(new SettingsContainer());
    }
    //</editor-fold>
    //
    //<editor-fold desc="Test methods">
    /**
     * SD/MOL round-trip identity: imports the multi-record SDF fixture (by absolute classpath path), collects unique-SMILES
     * set A, exports the imported models to a single {@code @TempDir} SDF via
     * {@link Exporter#exportFragmentsAsChemicalFile(File, List, ChemFileTypes, boolean, boolean)}, asserts the returned
     * failed-list is empty and the exported file exists, re-imports the exported file, collects unique-SMILES set B, and
     * asserts the two sets are equal. Identity is set-to-set equality only; no golden SMILES literals are asserted.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void roundTripSdfPreservesUniqueSmilesIdentity(@TempDir Path aTempDir) throws Exception {
        File tmpIn = Paths.get(this.getClass().getResource(
                "/de/unijena/cheminf/mortar/model/io/MultiRecord.sdf").toURI()).toFile();
        List<MoleculeDataModel> tmpImported = this.importer.importMoleculeFile(tmpIn, false, true, false);
        Assertions.assertNotNull(tmpImported);
        Assertions.assertFalse(tmpImported.isEmpty());
        Set<String> tmpSetA = new HashSet<>();
        for (MoleculeDataModel tmpModel : tmpImported) {
            tmpSetA.add(tmpModel.getUniqueSmiles());
        }
        File tmpOut = aTempDir.resolve("roundtrip.sdf").toFile();
        List<String> tmpFailed = this.exporter.exportFragmentsAsChemicalFile(
                tmpOut, tmpImported, ChemFileTypes.SDF, true, true);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        Assertions.assertTrue(tmpOut.exists());
        List<MoleculeDataModel> tmpReimported = this.importer.importMoleculeFile(tmpOut, false, true, false);
        Assertions.assertNotNull(tmpReimported);
        Set<String> tmpSetB = new HashSet<>();
        for (MoleculeDataModel tmpModel : tmpReimported) {
            tmpSetB.add(tmpModel.getUniqueSmiles());
        }
        Assertions.assertEquals(tmpSetA, tmpSetB);
    }
    //
    /**
     * SMILES round-trip identity: imports the five-molecule {@code SMILESTestFileTwo.smi} fixture (by absolute classpath
     * path), asserts every imported model carries a non-blank unique SMILES, builds the unique-SMILES set of the import,
     * writes that set (one unique SMILES per line) into a {@code @TempDir} {@code .smi} file, re-imports it, builds the
     * unique-SMILES set of the re-import, and asserts the two sets are equal. Identity is set-to-set equality only: a List
     * size is never compared against a Set size, because two input records may legitimately canonicalize to the same unique
     * SMILES. No golden SMILES literals are asserted.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void roundTripSmilesPreservesUniqueSmilesIdentity(@TempDir Path aTempDir) throws Exception {
        File tmpIn = Paths.get(this.getClass().getResource(
                "/de/unijena/cheminf/mortar/model/io/SMILESTestFileTwo.smi").toURI()).toFile();
        List<MoleculeDataModel> tmpImported = this.importer.importMoleculeFile(tmpIn, false, true, false);
        Assertions.assertNotNull(tmpImported);
        Assertions.assertFalse(tmpImported.isEmpty());
        Set<String> tmpSetA = new HashSet<>();
        for (MoleculeDataModel tmpModel : tmpImported) {
            Assertions.assertFalse(tmpModel.getUniqueSmiles().isBlank());
            tmpSetA.add(tmpModel.getUniqueSmiles());
        }
        File tmpOut = aTempDir.resolve("roundtrip.smi").toFile();
        StringBuilder tmpContent = new StringBuilder();
        for (String tmpUniqueSmiles : tmpSetA) {
            tmpContent.append(tmpUniqueSmiles).append(System.lineSeparator());
        }
        Files.writeString(tmpOut.toPath(), tmpContent.toString(), StandardCharsets.UTF_8);
        List<MoleculeDataModel> tmpReimported = this.importer.importMoleculeFile(tmpOut, false, true, false);
        Assertions.assertNotNull(tmpReimported);
        Set<String> tmpSetB = new HashSet<>();
        for (MoleculeDataModel tmpModel : tmpReimported) {
            tmpSetB.add(tmpModel.getUniqueSmiles());
        }
        Assertions.assertEquals(tmpSetA, tmpSetB);
    }
    //
    /**
     * CSV round-trip identity: builds a list of real {@link FragmentDataModel} instances (mirroring the
     * {@code ExporterTest.buildFragmentList()} idiom), collects unique-SMILES set A, exports them to a {@code @TempDir}
     * CSV via {@link Exporter#exportCsvFile(File, List, String, char, TabNames)} on the FRAGMENTS tab, asserts the
     * returned failed-list is empty and the file exists and is non-empty, re-imports the exported file, collects
     * unique-SMILES set B, and asserts the two sets are equal. Real {@code FragmentDataModel} instances are required
     * because the FRAGMENTS branch casts every element to {@code FragmentDataModel} (a plain imported
     * {@code MoleculeDataModel} would land in the failed-list and write no data rows).
     * <p>
     * The re-import goes through the very same {@link Importer#importMoleculeFile(File, boolean, boolean, boolean)}
     * entry point as the other two legs: {@code .csv} is a valid import extension that {@code Importer} routes to its
     * SMILES reader, which finds a SMILES code in either of the first two columns of each row — and the fragments-tab
     * CSV writes the unique SMILES into column one.
     *
     * @param aTempDir per-test temporary directory (auto-deleted)
     * @throws Exception if anything goes wrong
     */
    @Test
    public void roundTripCsvFragmentsTabPreservesUniqueSmilesIdentity(@TempDir Path aTempDir) throws Exception {
        SmilesParser tmpParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        List<MoleculeDataModel> tmpFragmentList = new ArrayList<>();
        String[] tmpSmilesCodes = {"c1ccccc1", "CCO", "O=CO"};
        for (String tmpSmilesCode : tmpSmilesCodes) {
            IAtomContainer tmpAtomContainer = tmpParser.parseSmiles(tmpSmilesCode);
            FragmentDataModel tmpFragment = new FragmentDataModel(tmpAtomContainer, false);
            tmpFragment.setAbsoluteFrequency(3);
            tmpFragment.setAbsolutePercentage(0.25);
            tmpFragment.setMoleculeFrequency(2);
            tmpFragment.setMoleculePercentage(0.20);
            tmpFragmentList.add(tmpFragment);
        }
        Set<String> tmpSetA = new HashSet<>();
        for (MoleculeDataModel tmpFragment : tmpFragmentList) {
            tmpSetA.add(tmpFragment.getUniqueSmiles());
        }
        File tmpOut = aTempDir.resolve("out.csv").toFile();
        List<String> tmpFailed = this.exporter.exportCsvFile(tmpOut, tmpFragmentList, "INT03", ',', TabNames.FRAGMENTS);
        Assertions.assertNotNull(tmpFailed);
        Assertions.assertTrue(tmpFailed.isEmpty());
        Assertions.assertTrue(tmpOut.exists());
        Assertions.assertTrue(tmpOut.length() > 0);
        List<MoleculeDataModel> tmpReimported = this.importer.importMoleculeFile(tmpOut, false, true, false);
        Assertions.assertNotNull(tmpReimported);
        Set<String> tmpSetB = new HashSet<>();
        for (MoleculeDataModel tmpModel : tmpReimported) {
            tmpSetB.add(tmpModel.getUniqueSmiles());
        }
        Assertions.assertEquals(tmpSetA, tmpSetB);
    }
    //</editor-fold>
}
