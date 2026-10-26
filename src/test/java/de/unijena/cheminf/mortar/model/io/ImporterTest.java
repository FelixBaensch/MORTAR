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
import de.unijena.cheminf.mortar.model.util.TestUtil;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Stream;

/**
 * Tests some functionalities of the {@link Importer} class.
 *
 * @author Jonas Schaub
 */
public class ImporterTest extends Importer {
    //<editor-fold desc="Locale setup and teardown" defaultstate="collapsed">
    /**
     * Default locale before this test class ran, restored after all tests.
     */
    private static Locale originalLocale;
    //
    /**
     * Sets the default locale to British English for this test class, remembering the original default locale.
     */
    @BeforeAll
    public static void setLocale() {
        ImporterTest.originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.of("en", "GB"));
    }
    //
    /**
     * Restores the default locale that was in place before this test class ran.
     */
    @AfterAll
    public static void restoreLocale() {
        Locale.setDefault(ImporterTest.originalLocale);
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
     * Tests the end-to-end import dispatch for a SMILES (.smi) file. Loading {@code SMILESTestFileTwo.smi} and importing
     * it through {@link Importer#importMoleculeFile(File, boolean, boolean, boolean)} exercises the extension dispatch to
     * {@code importSMILESFile} as well as {@code parse}, {@code findMoleculeName}, and {@code getFileName} transitively.
     * The fixture contains five SMILES codes, so a list with five molecules is expected and the stored file name must
     * match the imported file's base name.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testImportMoleculeFileWithSMILESFile() throws Exception {
        URL tmpURL = this.getClass().getResource("SMILESTestFileTwo.smi");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        List<MoleculeDataModel> tmpResultList = this.importMoleculeFile(tmpResourceFile, false, true, false);
        Assertions.assertNotNull(tmpResultList);
        Assertions.assertEquals(5, tmpResultList.size());
        Assertions.assertEquals("SMILESTestFileTwo.smi", this.getFileName());
        for (MoleculeDataModel tmpMolecule : tmpResultList) {
            Assertions.assertNotNull(tmpMolecule.getUniqueSmiles());
            Assertions.assertFalse(tmpMolecule.getUniqueSmiles().isBlank());
        }
    }
    /**
     * Tests the end-to-end import dispatch for a single MOL (.mol) file. Loading {@code Mirabilin_B.mol} and importing it
     * through {@link Importer#importMoleculeFile(File, boolean, boolean, boolean)} exercises the extension dispatch to
     * {@code importMolFile}. A single molecule with a non-null, non-blank unique SMILES is expected.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testImportMoleculeFileWithMOLFile() throws Exception {
        URL tmpURL = this.getClass().getResource("Mirabilin_B.mol");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        List<MoleculeDataModel> tmpResultList = this.importMoleculeFile(tmpResourceFile, false, true, false);
        Assertions.assertNotNull(tmpResultList);
        Assertions.assertEquals(1, tmpResultList.size());
        Assertions.assertNotNull(tmpResultList.get(0).getUniqueSmiles());
        Assertions.assertFalse(tmpResultList.get(0).getUniqueSmiles().isBlank());
        Assertions.assertEquals("Mirabilin_B.mol", this.getFileName());
    }
    /**
     * Tests the end-to-end import dispatch for a multi-record SD (.sdf) file. Loading {@code MultiRecord.sdf} and
     * importing it through {@link Importer#importMoleculeFile(File, boolean, boolean, boolean)} exercises the extension dispatch
     * to {@code importSDFile}, the iterating SDF reader, and the molecule-name fallback for records lacking a title. The
     * fixture contains three valid records in order: the titled {@code Ethanol}, an untitled ethane record, and the
     * titled {@code Benzene}. A list with three molecules is expected and the exact ordered names must be
     * {@code ["Ethanol", "MultiRecord1", "Benzene"]}: the middle record has no title, so {@code importSDFile} falls back
     * to {@code FileUtil.getFileNameWithoutExtension(aFile) + tmpCounter}. The counter value {@code 1} is the index of
     * that record in the file, so this assertion pins the post-increment of the molecule counter: a
     * mutated (removed/incremented) counter would change the embedded index and this assertion would fail.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testImportMoleculeFileWithSDFile() throws Exception {
        URL tmpURL = this.getClass().getResource("MultiRecord.sdf");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        List<MoleculeDataModel> tmpResultList = this.importMoleculeFile(tmpResourceFile, false, true, false);
        Assertions.assertNotNull(tmpResultList);
        Assertions.assertEquals(3, tmpResultList.size());
        Assertions.assertEquals("MultiRecord.sdf", this.getFileName());
        for (MoleculeDataModel tmpMolecule : tmpResultList) {
            Assertions.assertNotNull(tmpMolecule.getName());
            Assertions.assertFalse(tmpMolecule.getName().isBlank());
        }
        //exact ordered names: the untitled middle record's fallback name embeds the counter value (its file index = 1)
        Assertions.assertEquals("Ethanol", tmpResultList.get(0).getName());
        Assertions.assertEquals("MultiRecord1", tmpResultList.get(1).getName());
        Assertions.assertEquals("Benzene", tmpResultList.get(2).getName());
    }
    /**
     * Tests the erroneous-entry skip-counter increment of {@code importSDFile} through molecule naming. Loading
     * {@code MultiRecordUnnamedWithError.sdf} — an untitled valid record, a deliberately broken record, and a second
     * untitled valid record — makes the iterating reader skip the broken middle record and continue. Both surviving
     * records lack a title, so their names fall back to {@code FileUtil.getFileNameWithoutExtension(aFile) + tmpCounter}.
     * The first valid record's index is {@code 0}, and the second valid record's index is {@code 2} (NOT {@code 1}):
     * the counter was incremented once inside the erroneous-entry skip branch for the skipped record.
     * Asserting the second molecule is named {@code MultiRecordUnnamedWithError2} therefore pins that skip-branch
     * increment — negating or removing it would yield {@code MultiRecordUnnamedWithError1} and fail this assertion.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testImportMoleculeFileWithUnnamedErroneousSDFilePinsSkipCounter() throws Exception {
        URL tmpURL = this.getClass().getResource("MultiRecordUnnamedWithError.sdf");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        List<MoleculeDataModel> tmpResultList = this.importMoleculeFile(tmpResourceFile, false, true, false);
        Assertions.assertNotNull(tmpResultList);
        Assertions.assertEquals(2, tmpResultList.size());
        Assertions.assertEquals("MultiRecordUnnamedWithError.sdf", this.getFileName());
        Assertions.assertEquals("MultiRecordUnnamedWithError0", tmpResultList.get(0).getName());
        //the skipped broken record consumed index 1, so the second valid record's fallback name uses index 2
        Assertions.assertEquals("MultiRecordUnnamedWithError2", tmpResultList.get(1).getName());
    }
    /**
     * Tests that importing a file with an unsupported extension returns null. This exercises the
     * {@code tmpInputFileType == null} branch in {@link Importer#importMoleculeFile(File, boolean, boolean, boolean)} that is
     * reached when the file extension does not match any of the valid import file types.
     *
     * @param aTempDir temporary directory provided by JUnit; auto-deleted after the test
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testImportMoleculeFileWithUnknownExtensionReturnsNull(@TempDir Path aTempDir) throws Exception {
        Path tmpUnknownFilePath = aTempDir.resolve("unsupported.xyz");
        Files.writeString(tmpUnknownFilePath, "C\n");
        File tmpUnknownFile = tmpUnknownFilePath.toFile();
        List<MoleculeDataModel> tmpResultList = this.importMoleculeFile(tmpUnknownFile, false, true, false);
        Assertions.assertNull(tmpResultList);
    }
    /**
     * Tests the else-branch of {@link Importer#preprocessMoleculeSet(IAtomContainerSet, boolean)} that is reached when
     * {@code isFillOpenValencesWithImplH} is false. In this case open valences are not saturated; instead unset implicit
     * hydrogen counts are set to zero. The method must complete without throwing, leave the molecule in the set, and,
     * unlike the {@code true} path (which saturates them to methyl groups), leave the two open-valence carbons with their
     * explicitly given hydrogen counts of two and one.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testPreprocessMoleculeSetWithoutFillingOpenValences() throws Exception {
        SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        IAtomContainer tmpMolecule = tmpSmiPar.parseSmiles("[CH2]CCCC([CH])CCC");
        IAtomContainerSet tmpSet = new AtomContainerSet();
        tmpSet.addAtomContainer(tmpMolecule);
        Assertions.assertDoesNotThrow(() -> this.preprocessMoleculeSet(tmpSet, false));
        Assertions.assertEquals(1, tmpSet.getAtomContainerCount());
        //atom 0 is the [CH2], atom 5 the [CH]; with filling enabled both would carry three implicit hydrogens
        Assertions.assertEquals(2, tmpMolecule.getAtom(0).getImplicitHydrogenCount());
        Assertions.assertEquals(1, tmpMolecule.getAtom(5).getImplicitHydrogenCount());
    }
    /**
     * Tests the end-to-end import of an MDL V3000 MOL file. Loading {@code MolV3000.mol} and importing it through
     * {@link Importer#importMoleculeFile(File, boolean, boolean, boolean)} exercises the V3000-format detection branch of
     * {@code importMolFile} that uses the {@code MDLV3000Reader} (as opposed to the V2000 reader covered by the
     * Mirabilin test). A single molecule with a non-null, non-blank unique SMILES is expected.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testImportMoleculeFileWithV3000MolFile() throws Exception {
        URL tmpURL = this.getClass().getResource("MolV3000.mol");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        List<MoleculeDataModel> tmpResultList = this.importMoleculeFile(tmpResourceFile, false, true, false);
        Assertions.assertNotNull(tmpResultList);
        Assertions.assertEquals(1, tmpResultList.size());
        Assertions.assertNotNull(tmpResultList.get(0).getUniqueSmiles());
        Assertions.assertFalse(tmpResultList.get(0).getUniqueSmiles().isBlank());
        Assertions.assertEquals("MolV3000.mol", this.getFileName());
    }
    /**
     * Tests the molecule-name fallback branch of {@code importMolFile}. Loading {@code UnnamedMol.mol}, whose title line
     * is blank and which carries no name/ID property, makes {@code findMoleculeName} return null; the importer then falls
     * back to reading the first line and, since it is blank, to the file name without extension. A single molecule named
     * {@code UnnamedMol} (the fallback name) is expected.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testImportMoleculeFileWithUnnamedMolFileUsesNameFallback() throws Exception {
        URL tmpURL = this.getClass().getResource("UnnamedMol.mol");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        List<MoleculeDataModel> tmpResultList = this.importMoleculeFile(tmpResourceFile, false, true, false);
        Assertions.assertNotNull(tmpResultList);
        Assertions.assertEquals(1, tmpResultList.size());
        Assertions.assertEquals("UnnamedMol", tmpResultList.get(0).getName());
    }
    /**
     * Tests the import of an SD file that contains a deliberately broken record between two valid records. Loading
     * {@code SDFwithError.sdf} exercises the erroneous-entry skip path of {@code importSDFile}: the iterating reader skips
     * the broken record (logging it) and continues, so the failed-import-count branch is reached. The two valid records
     * must still be imported.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testImportMoleculeFileWithErroneousSDFile() throws Exception {
        URL tmpURL = this.getClass().getResource("SDFwithError.sdf");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        List<MoleculeDataModel> tmpResultList = this.importMoleculeFile(tmpResourceFile, false, true, false);
        Assertions.assertNotNull(tmpResultList);
        Assertions.assertEquals(2, tmpResultList.size());
        Assertions.assertEquals("SDFwithError.sdf", this.getFileName());
    }
    /**
     * Tests the {@code parse} branch that keeps the atom container inside the data model. The
     * {@code keepAtomContainerInDataModelSetting} is a deprecated dummy whose getter always returns false (its setter is
     * a no-op), so the branch cannot be reached through the public setter; a settings container whose getter is
     * overridden to return true is used instead. {@code parse} then constructs the MoleculeDataModel from the atom
     * container directly (rather than from the SMILES string). The import must still produce the expected number of
     * molecules with valid unique SMILES, and every model must report that it keeps its atom container.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testImportMoleculeFileKeepingAtomContainerInDataModel() throws Exception {
        SettingsContainer tmpSettingsContainer = new SettingsContainer() {
            @Override
            public boolean getKeepAtomContainerInDataModelSetting() {
                return true;
            }
        };
        Importer tmpImporter = new Importer(tmpSettingsContainer);
        URL tmpURL = this.getClass().getResource("SMILESTestFileTwo.smi");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        List<MoleculeDataModel> tmpResultList = tmpImporter.importMoleculeFile(tmpResourceFile, false, true, false);
        Assertions.assertNotNull(tmpResultList);
        Assertions.assertEquals(5, tmpResultList.size());
        for (MoleculeDataModel tmpMolecule : tmpResultList) {
            Assertions.assertNotNull(tmpMolecule.getUniqueSmiles());
            Assertions.assertFalse(tmpMolecule.getUniqueSmiles().isBlank());
            Assertions.assertTrue(tmpMolecule.isKeepAtomContainer());
        }
    }
    /**
     * Tests the early-return branch of {@link Importer#preprocessMoleculeSet(IAtomContainerSet, boolean)} that is reached
     * when the given molecule set is empty: the method returns immediately without processing. The set must remain empty
     * and no exception must be thrown.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testPreprocessMoleculeSetWithEmptySet() throws Exception {
        IAtomContainerSet tmpSet = new AtomContainerSet();
        Assertions.assertDoesNotThrow(() -> this.preprocessMoleculeSet(tmpSet, true));
        Assertions.assertEquals(0, tmpSet.getAtomContainerCount());
    }
    /**
     * Tests the unset-implicit-hydrogen-count branch of the {@code isFillOpenValencesWithImplH == false} path in
     * {@link Importer#preprocessMoleculeSet(IAtomContainerSet, boolean)}. A manually built atom container whose atoms have
     * an unset implicit hydrogen count is processed without filling open valences, so the method sets the unset implicit
     * hydrogen counts to zero. The atom's implicit hydrogen count must be zero afterwards.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testPreprocessMoleculeSetSetsUnsetImplicitHydrogenCountToZero() throws Exception {
        IAtomContainer tmpMolecule = new AtomContainer();
        IAtom tmpCarbon = new Atom("C");
        tmpCarbon.setImplicitHydrogenCount(null);
        IAtom tmpOxygen = new Atom("O");
        tmpOxygen.setImplicitHydrogenCount(null);
        tmpMolecule.addAtom(tmpCarbon);
        tmpMolecule.addAtom(tmpOxygen);
        tmpMolecule.addBond(0, 1, org.openscience.cdk.interfaces.IBond.Order.SINGLE);
        IAtomContainerSet tmpSet = new AtomContainerSet();
        tmpSet.addAtomContainer(tmpMolecule);
        this.preprocessMoleculeSet(tmpSet, false);
        for (IAtom tmpAtom : tmpSet.getAtomContainer(0).atoms()) {
            Assertions.assertEquals(0, tmpAtom.getImplicitHydrogenCount());
        }
    }
    /**
     * Tests the exception-handling branch of {@link Importer#preprocessMoleculeSet(IAtomContainerSet, boolean)}. An empty
     * atom container (no atoms, no bonds) that cannot be kekulized causes an exception inside the per-molecule processing
     * loop. The exception is caught and logged (exactly one WARNING record carrying the exception and naming the
     * molecule), the molecule remains in the set, and the method completes without propagating the exception.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testPreprocessMoleculeSetLogsPerMoleculeException() throws Exception {
        IAtomContainer tmpInvalidMolecule = new AtomContainer();
        IAtom tmpAromaticCarbon = new Atom("C");
        tmpAromaticCarbon.setIsAromatic(true);
        tmpInvalidMolecule.addAtom(tmpAromaticCarbon);
        tmpInvalidMolecule.setProperty(Importer.MOLECULE_NAME_PROPERTY_KEY, "InvalidAromaticMolecule");
        IAtomContainerSet tmpSet = new AtomContainerSet();
        tmpSet.addAtomContainer(tmpInvalidMolecule);
        List<LogRecord> tmpRecords = TestUtil.captureLogRecords(Importer.class.getName(), Level.WARNING,
                () -> this.preprocessMoleculeSet(tmpSet, true));
        Assertions.assertEquals(1, tmpSet.getAtomContainerCount());
        Assertions.assertEquals(1, tmpRecords.size());
        Assertions.assertNotNull(tmpRecords.getFirst().getThrown());
        Assertions.assertTrue(tmpRecords.getFirst().getMessage().contains("InvalidAromaticMolecule"));
    }
    /**
     * Tests the end-to-end import of a SMILES file that contains some unparsable lines mixed with valid SMILES codes.
     * Loading {@code SmilesWithSomeInvalid.smi} drives the skipped-lines warning path of {@code importSMILESFile}: the
     * underlying reader skips the invalid lines (incrementing its skipped-lines counter) and the importer logs the
     * skipped count. The three valid SMILES codes must still be imported.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testImportMoleculeFileWithPartiallyInvalidSmilesFile() throws Exception {
        URL tmpURL = this.getClass().getResource("SmilesWithSomeInvalid.smi");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        List<MoleculeDataModel> tmpResultList = this.importMoleculeFile(tmpResourceFile, false, true, false);
        Assertions.assertNotNull(tmpResultList);
        Assertions.assertEquals(3, tmpResultList.size());
        Assertions.assertEquals("SmilesWithSomeInvalid.smi", this.getFileName());
    }
    /**
     * Tests the relaxed-parse fallback path of the SMILES file format detection (via {@code importSMILESFile} ->
     * {@code DynamicSMILESFileReader.detectFormat}). Loading {@code SingleColumnRelaxedSmiles.smi}, whose single-column
     * SMILES codes parse only when kekulization is disabled (they fail the initial kekulization-enabled parse), drives the
     * detect-format catch-and-retry-relaxed branch as well as the no-ID-column header-detection retry.
     * <p>
     * The fixture holds three lines, all of them all-carbon aromatic rings that cannot be kekulized as neutral species
     * — a five-membered and a seven-membered ring (cyclopentadienyl and tropylium written without their charge) — which
     * is exactly why they force the relaxed retry. All three lines must be imported: asserting only that the result is
     * non-empty would still pass if the relaxed retry recovered just one of them. Note that the first and third line
     * canonicalise to the same structure, so the imported models are three but the distinct structures are two.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testImportMoleculeFileWithSingleColumnRelaxedSmiles() throws Exception {
        URL tmpURL = this.getClass().getResource("SingleColumnRelaxedSmiles.smi");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        List<MoleculeDataModel> tmpResultList = this.importMoleculeFile(tmpResourceFile, false, true, false);
        Assertions.assertNotNull(tmpResultList);
        Assertions.assertEquals(3, tmpResultList.size(),
                "every line of the fixture must survive the relaxed-parse retry");
        Set<String> tmpDistinctStructures = new HashSet<>();
        for (MoleculeDataModel tmpModel : tmpResultList) {
            Assertions.assertFalse(tmpModel.getUniqueSmiles().isBlank());
            tmpDistinctStructures.add(tmpModel.getUniqueSmiles());
        }
        //the five-membered ring appears twice in the fixture, written once plainly and once with an explicit [cH]
        Assertions.assertEquals(2, tmpDistinctStructures.size());
        Assertions.assertEquals("SingleColumnRelaxedSmiles.smi", this.getFileName());
    }
    /**
     * Tests the first-and-only-structure-failed warning path of {@code importSDFile}. Loading
     * {@code SingleBrokenRecord.sdf}, whose single record cannot be parsed, makes the iterating reader yield no structures
     * and triggers the "import failed for first and only structure" branch. The returned list is empty.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testImportMoleculeFileWithSingleBrokenSDRecord() throws Exception {
        URL tmpURL = this.getClass().getResource("SingleBrokenRecord.sdf");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        List<MoleculeDataModel> tmpResultList = this.importMoleculeFile(tmpResourceFile, false, true, false);
        Assertions.assertNotNull(tmpResultList);
        Assertions.assertTrue(tmpResultList.isEmpty());
        Assertions.assertEquals("SingleBrokenRecord.sdf", this.getFileName());
    }
    /**
     * Tests the undeterminable-format branch of {@code importMolFile} (reached via reflection). Loading
     * {@code UnrecognizedFormat.mol}, whose content is not a recognizable chemistry file format, makes the format factory
     * return null, so {@code importMolFile} throws a CDKException ("file type could not be determined"). The reflective
     * invocation wraps that exception in an InvocationTargetException whose cause is the CDKException.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testImportMolFileWithUndeterminableFormatThrows() throws Exception {
        URL tmpURL = this.getClass().getResource("UnrecognizedFormat.mol");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        Method tmpImportMolFile = Importer.class.getDeclaredMethod("importMolFile", File.class);
        tmpImportMolFile.setAccessible(true);
        java.lang.reflect.InvocationTargetException tmpThrown = Assertions.assertThrows(
                java.lang.reflect.InvocationTargetException.class,
                () -> tmpImportMolFile.invoke(this, tmpResourceFile));
        Assertions.assertInstanceOf(org.openscience.cdk.exception.CDKException.class, tmpThrown.getCause());
    }
    /**
     * Tests the null/empty-set early-return branch of the private {@code parse} method (reached via reflection). When the
     * given atom container set is null, {@code parse} returns an empty (non-null) list without iterating.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testParseWithNullSetReturnsEmptyList() throws Exception {
        Method tmpParse = Importer.class.getDeclaredMethod("parse", IAtomContainerSet.class, boolean.class, boolean.class);
        tmpParse.setAccessible(true);
        Object tmpResult = tmpParse.invoke(this, null, false, false);
        Assertions.assertInstanceOf(List.class, tmpResult);
        Assertions.assertTrue(((List<?>) tmpResult).isEmpty());
    }
    /**
     * Tests the empty-set early-return branch of the private {@code parse} method (reached via reflection). When the given
     * atom container set is empty, {@code parse} returns an empty (non-null) list.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testParseWithEmptySetReturnsEmptyList() throws Exception {
        Method tmpParse = Importer.class.getDeclaredMethod("parse", IAtomContainerSet.class, boolean.class, boolean.class);
        tmpParse.setAccessible(true);
        Object tmpResult = tmpParse.invoke(this, new AtomContainerSet(), false, false);
        Assertions.assertInstanceOf(List.class, tmpResult);
        Assertions.assertTrue(((List<?>) tmpResult).isEmpty());
    }
    /**
     * Tests the branches of the private {@code findMoleculeName} method (reached via reflection) on an atom container
     * built from the given title and properties:
     * <ul>
     *     <li>"id property": no title and no property whose key contains 'name', but one whose key contains 'id'; the
     *     value of that ID property is returned as the molecule name.</li>
     *     <li>"name property when no title": no title but a property whose key contains 'name'; its value is returned.
     *     This pins the title-null guard and the name-key detection predicate.</li>
     *     <li>"ignores database name key": the name-key detection explicitly excludes the 'Database_Name' key, so a
     *     container carrying only that key (no title, no id) resolves no name and null is returned. This pins the
     *     {@code !k.equalsIgnoreCase("Database_Name")} exclusion predicate: dropping it would wrongly return the
     *     database name.</li>
     *     <li>"NONE resets to null": the resolved name equals 'None' (case-insensitive) and no usable ID property is
     *     present, so the returned name is reset to null.</li>
     *     <li>"id branch ignores distractor": no title, exactly one key containing 'id' ({@code Compound_ID}) and exactly
     *     one distractor key containing neither 'id' nor 'name' ({@code Weight}). Both the real predicate and its
     *     negation select deterministically regardless of HashMap iteration order, so expecting the id value
     *     {@code CID-777} (not {@code 180.16}) pins the id-branch {@code anyMatch} and {@code filter} lambdas.</li>
     *     <li>"name branch ignores distractor": no title, exactly one key containing 'name' ({@code Molecule_Name}) and
     *     exactly one distractor key containing neither 'name' nor 'id' ({@code Comment}). Expecting the name value
     *     {@code Glucose} (not {@code note}) pins the name-branch {@code filter} lambda.</li>
     * </ul>
     *
     * @param aCaseName name of the case (display only)
     * @param aTitle title to set on the atom container, or null to leave it unset
     * @param aProperties key-value pairs set as properties on the atom container, in order
     * @param anExpectedName expected return value of findMoleculeName
     * @throws Exception if anything goes wrong
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("findMoleculeNameCases")
    public void testFindMoleculeName(String aCaseName, String aTitle, String[][] aProperties, String anExpectedName)
            throws Exception {
        IAtomContainer tmpAtomContainer = new AtomContainer();
        if (aTitle != null) {
            tmpAtomContainer.setTitle(aTitle);
        }
        for (String[] tmpProperty : aProperties) {
            tmpAtomContainer.setProperty(tmpProperty[0], tmpProperty[1]);
        }
        Object tmpResult = this.invokeFindMoleculeName(tmpAtomContainer);
        Assertions.assertEquals(anExpectedName, tmpResult);
    }
    //
    /**
     * Provides the cases for {@link #testFindMoleculeName}: case name, title (or null), properties, expected name.
     *
     * @return the findMoleculeName cases
     */
    private static Stream<Arguments> findMoleculeNameCases() {
        return Stream.of(
                Arguments.of("id property", null, new String[][] {{"Compound_ID", "CID-12345"}}, "CID-12345"),
                Arguments.of("name property when no title", null, new String[][] {{"Molecule_Name", "Glucose"}}, "Glucose"),
                Arguments.of("ignores database name key", null, new String[][] {{"Database_Name", "ChEBI"}}, null),
                Arguments.of("NONE resets to null", "None", new String[][] {}, null),
                Arguments.of("id branch ignores distractor", null,
                        new String[][] {{"Weight", "180.16"}, {"Compound_ID", "CID-777"}}, "CID-777"),
                Arguments.of("name branch ignores distractor", null,
                        new String[][] {{"Comment", "note"}, {"Molecule_Name", "Glucose"}}, "Glucose"));
    }
    //
    /**
     * Invokes the private {@code findMoleculeName} method of {@link Importer} on this instance via reflection.
     *
     * @param anAtomContainer atom container to find the name of
     * @return the return value of findMoleculeName
     * @throws Exception if the reflective lookup or invocation fails
     */
    private Object invokeFindMoleculeName(IAtomContainer anAtomContainer) throws Exception {
        Method tmpFindMoleculeName = Importer.class.getDeclaredMethod("findMoleculeName", IAtomContainer.class);
        tmpFindMoleculeName.setAccessible(true);
        return tmpFindMoleculeName.invoke(this, anAtomContainer);
    }
    /**
     * Tests the deprecated, currently-unused private {@code importPDBFile} method (reached via reflection). Although it is
     * not wired into the public import dispatch, the method is still functional: it reads a small valid PDB fixture
     * ({@code Glycine.pdb}) and returns a non-empty atom container set whose first molecule carries a non-blank name
     * property. This exercises the PDB reader configuration loop and the molecule-name fallback.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testImportPDBFile() throws Exception {
        URL tmpURL = this.getClass().getResource("Glycine.pdb");
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        Method tmpImportPDBFile = Importer.class.getDeclaredMethod("importPDBFile", File.class);
        tmpImportPDBFile.setAccessible(true);
        Object tmpResult = tmpImportPDBFile.invoke(this, tmpResourceFile);
        Assertions.assertInstanceOf(IAtomContainerSet.class, tmpResult);
        IAtomContainerSet tmpAtomContainerSet = (IAtomContainerSet) tmpResult;
        Assertions.assertTrue(tmpAtomContainerSet.getAtomContainerCount() > 0);
        IAtomContainer tmpFirstAtomContainer = tmpAtomContainerSet.getAtomContainer(0);
        Object tmpNameProperty = tmpFirstAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY);
        Assertions.assertNotNull(tmpNameProperty);
        Assertions.assertFalse(tmpNameProperty.toString().isBlank());
    }
    /**
     * Tests the thread-interrupt guards of the private {@code importPDBFile} ("pdb") and {@code importSDFile} ("sdf")
     * methods (reached via reflection). The current thread is interrupted before the call. In {@code importPDBFile}, after
     * the PDB file is read the per-atom-container loop hits its {@code if (Thread.currentThread().isInterrupted()) break;}
     * on the first iteration and adds no molecule; in {@code importSDFile} the
     * {@code while (!Thread.currentThread().isInterrupted())} loop is never entered. Either way the returned atom
     * container set is empty. The interrupt flag is cleared in a {@code finally} block so it does not leak into the rest
     * of the suite. This drives the guards with the real interrupt flag (no mocking) and documents that cancelling an
     * import stops it promptly.
     *
     * @param aCaseName name of the case (display only)
     * @param aResourceName name of the test resource file to import
     * @param aMethodName name of the private import method taking a File
     * @throws Exception if anything goes wrong
     */
    @ParameterizedTest(name = "interrupted, {0}")
    @CsvSource({"pdb, Glycine.pdb, importPDBFile", "sdf, MultiRecord.sdf, importSDFile"})
    public void testImportFileInterrupted(String aCaseName, String aResourceName, String aMethodName) throws Exception {
        URL tmpURL = this.getClass().getResource(aResourceName);
        File tmpResourceFile = Paths.get(tmpURL.toURI()).toFile();
        Method tmpImportMethod = Importer.class.getDeclaredMethod(aMethodName, File.class);
        tmpImportMethod.setAccessible(true);
        try {
            Thread.currentThread().interrupt();
            Object tmpResult = tmpImportMethod.invoke(this, tmpResourceFile);
            Assertions.assertInstanceOf(IAtomContainerSet.class, tmpResult);
            Assertions.assertEquals(0, ((IAtomContainerSet) tmpResult).getAtomContainerCount());
        } finally {
            Thread.interrupted();
        }
    }
    //</editor-fold>
}
