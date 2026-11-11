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

import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.ChemUtil;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import de.unijena.cheminf.mortar.model.util.LogUtil;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.aromaticity.Kekulization;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.io.FormatFactory;
import org.openscience.cdk.io.IChemObjectReader;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.io.MDLV3000Reader;
import org.openscience.cdk.io.PDBReader;
import org.openscience.cdk.io.formats.IChemFormat;
import org.openscience.cdk.io.formats.MDLV2000Format;
import org.openscience.cdk.io.formats.MDLV3000Format;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.io.setting.IOSetting;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;
import org.openscience.cdk.tools.manipulator.HydrogenState;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Importer.
 *
 * @author Felix Baensch
 * @author Samuel Behr
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class Importer {
    //<editor-fold desc="Enum ValidImportFileTypes">
    /**
     * Enum of file types that can be imported with their specific file extensions.
     */
    public enum ValidImportFileTypes {
        /**
         * MDL MOL file V2000 or V3000.
         */
        MOL_FILE(".mol"),
        /**
         * Structure-data format file, concatenation of MOL files.
         */
        STRUCTURE_DATA_FORMAT_FILE(".sdf"),
        /**
         * A SMILES file, i.e. each line of the text-based file should contain one SMILES code. This is the minimum
         * requirement, all additional elements in the file will be parsed or not according to the DynamicSMILESFileReader.
         */
        SMILES_FILE(".smi"),
        /**
         * Text file that will be read as SMILES / delimiter-separated value file. Each line should contain one SMILES code,
         * all additional elements in the file will be parsed or not according to the DynamicSMILESFileReader.
         */
        TEXT_FILE(".txt"),
        /**
         * Comma-separated value file where each line should contain one SMILES code,
         * all additional elements in the file will be parsed or not according to the DynamicSMILESFileReader.
         */
        COMMA_SEPARATED_VALUES_FILE(".csv"),
        /**
         * Tab-separated value file where each line should contain one SMILES code,
         * all additional elements in the file will be parsed or not according to the DynamicSMILESFileReader.
         */
        TAB_SEPARATED_VALUES_FILE(".tsv"),
        ;
        /**
         * File extension of the respective file type.
         */
        private final String fileExtension;
        /**
         * Constructor.
         */
        private ValidImportFileTypes(String aFileExtension) {
            this.fileExtension = aFileExtension;
        }
        /**
         * Get the file extension of this file type, formatted as ".xyz".
         *
         * @return associated file extension as ".xyz"
         */
        public String getFileExtension() {
            return this.fileExtension;
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Enum ImportPhase">
    /**
     * Enum of the consecutive phases of a molecule file import for which the progress is reported to an
     * {@link ImportProgressListener}.
     */
    public enum ImportPhase {
        /**
         * Counting the entries of an SD file or the lines of a SMILES file to know the total number of entries to
         * import. The total is unknown in this phase and therefore reported as -1.
         */
        COUNTING("Status.importing.counting"),
        /**
         * Reading, preprocessing, and converting the entries into MORTAR-internal data models, one entry after the other.
         */
        IMPORTING("Status.importing.importing"),
        ;
        /**
         * Key of the status bar message describing this phase.
         */
        private final String messageKey;
        /**
         * Constructor.
         *
         * @param aMessageKey key of the status bar message describing this phase
         */
        private ImportPhase(String aMessageKey) {
            this.messageKey = aMessageKey;
        }
        /**
         * Returns the key of the status bar message describing this phase.
         *
         * @return message key
         */
        public String getMessageKey() {
            return this.messageKey;
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Interface ImportProgressListener">
    /**
     * Listener that is notified about the progress of a molecule file import.
     */
    @FunctionalInterface
    public interface ImportProgressListener {
        /**
         * Called repeatedly during each import phase, but not necessarily for every single entry, and always at the
         * end of each phase. Called in the importing thread.
         *
         * @param aPhase current import phase
         * @param aProcessedCount number of entries processed in the current phase so far
         * @param aTotalCount total number of entries to process in the current phase or -1 if unknown
         */
        void progressChanged(ImportPhase aPhase, int aProcessedCount, int aTotalCount);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private static class ImportStatistics" defaultstate="collapsed">
    /**
     * Counts the molecules passed through the import routine and the noteworthy events while processing them, for the
     * summarising log messages at the end of an import.
     */
    private static final class ImportStatistics {
        /**
         * Number of molecules that were read from the file and processed.
         */
        private int processedMoleculesCount = 0;
        /**
         * Number of molecules that caused an exception while being preprocessed.
         */
        private int preprocessingExceptionsCount = 0;
        /**
         * Number of molecules with radicals that were fixed and saturated with implicit hydrogen atoms.
         */
        private int fixedRadicalsCount = 0;
        /**
         * Number of molecules for which no SMILES code could be created, i.e. that are missing in the import result.
         */
        private int failedSmilesCount = 0;
    }
    //</editor-fold>
    //
    //<editor-fold desc="public static final class constants">
    /**
     * Property key that is used to store the detected molecule names on the imported atom containers.
     */
    public static final String MOLECULE_NAME_PROPERTY_KEY = "MORTAR_IMPORTER_NAME";
    /**
     * Unmodifiable Set of valid files extensions for file import.
     */
    public static final Set<String> VALID_IMPORT_FILE_EXTENSIONS_SET;
    static {
        HashSet<String> tmpSet = new HashSet<>(10, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        for (Importer.ValidImportFileTypes tmpType : Importer.ValidImportFileTypes.values()) {
            tmpSet.add(tmpType.getFileExtension());
        }
        VALID_IMPORT_FILE_EXTENSIONS_SET = Collections.unmodifiableSet(tmpSet);
    }
    //</editor-fold>
    //
    //<editor-fold desc="private static final class constants" defaultstate="collapsed">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(Importer.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * Name of the last imported file.
     */
    private String fileName;
    /**
     * Container of general MORTAR settings, providing the recent directory path and other import-related settings.
     */
    private final SettingsContainer settingsContainer;
    /**
     * Listener that is notified about the import progress, may be null.
     */
    private ImportProgressListener progressListener;
    //</editor-fold>
    //
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Constructor. Should the recent directory path provided by the settings container be faulty, it is set to its default value.
     *
     * @param aSettingsContainer the MORTAR general settings container providing a recent directory path and other
     *                           import-related settings
     * @throws NullPointerException if the settings container is null
     */
    public Importer(SettingsContainer aSettingsContainer) throws NullPointerException {
        Objects.requireNonNull(aSettingsContainer, "Given settings container is null.");
        this.settingsContainer = aSettingsContainer;
        String tmpRecentDirFromContainer = this.settingsContainer.getRecentDirectoryPathSetting();
        if (tmpRecentDirFromContainer == null || tmpRecentDirFromContainer.isEmpty()) {
            this.settingsContainer.setRecentDirectoryPathSetting(SettingsContainer.RECENT_DIRECTORY_PATH_SETTING_DEFAULT);
            Importer.LOGGER.log(Level.INFO, "Recent directory could not be read, resetting to default.");
        }
        this.fileName = null;
    }
    //</editor-fold>
    //
    //<editor-fold desc="public methods" defaultstate="collapsed">
    /**
     * Imports a molecule file, user can choose between three types - mol, sdf, and smi. A text file (.txt) and a
     * CSV/TSV/DSV file will be treated
     * as a SMILES file. If the respective setting is activated, incomplete valences of the imported atoms are filled
     * with implicit hydrogen atoms. If no molecule name or ID is given in the input file, the file name with an appended
     * counter is used as such and added to the returned atom containers as a property.
     *
     * @param aFile File to import
     * @param isRegardStereo whether stereochemistry should be encoded in the SMILES strings
     * @param isFillOpenValencesWithImplH whether open valences in the imported molecules should be filled with implicit hydrogen atoms
     * @param isKekulizationEnforced whether imported molecules should always be kekulized, which means aromaticity
     *                               will not(!) be encoded in the internal SMILES strings
     * @return List of MoleculeDataModels which contains the imported molecules or null if the file chooser was
     * closed by the user or a not importable file type was chosen
     * @throws CDKException if the given file cannot be parsed
     * @throws IOException if the given file cannot be found or read
     * @throws NullPointerException if the given file is null
     */
    public List<MoleculeDataModel> importMoleculeFile(File aFile, boolean isRegardStereo, boolean isFillOpenValencesWithImplH, boolean isKekulizationEnforced)
            throws NullPointerException, IOException, CDKException {
        Objects.requireNonNull(aFile, "aFile is null");
        String tmpRecentDirFromContainer = this.settingsContainer.getRecentDirectoryPathSetting();
        if (tmpRecentDirFromContainer == null || tmpRecentDirFromContainer.isEmpty()) {
            this.settingsContainer.setRecentDirectoryPathSetting(SettingsContainer.RECENT_DIRECTORY_PATH_SETTING_DEFAULT);
            Importer.LOGGER.log(Level.INFO, "Recent directory could not be read, resetting to default.");
        }
        String tmpFilePath = aFile.getPath();
        String tmpFileExtension = FileUtil.getFileExtension(tmpFilePath);
        Importer.ValidImportFileTypes tmpInputFileType = null;
        for (Importer.ValidImportFileTypes tmpType : Importer.ValidImportFileTypes.values()) {
            if (tmpType.getFileExtension().equals(tmpFileExtension)) {
                tmpInputFileType = tmpType;
            }
        }
        if (tmpInputFileType == null) {
            return null;
        }
        List<MoleculeDataModel> tmpReturnList = new ArrayList<>();
        Importer.ImportStatistics tmpStatistics = new Importer.ImportStatistics();
        //every molecule is preprocessed and turned into a data model right after it was read, so that its atom container
        // can be garbage-collected immediately instead of keeping all atom containers in memory until the end of the file
        Consumer<IAtomContainer> tmpMoleculeProcessor = aMolecule -> {
            tmpStatistics.processedMoleculesCount++;
            this.preprocessMolecule(aMolecule, isFillOpenValencesWithImplH, tmpStatistics);
            MoleculeDataModel tmpMoleculeDataModel = this.createMoleculeDataModel(aMolecule, isRegardStereo, isKekulizationEnforced);
            if (tmpMoleculeDataModel == null) {
                tmpStatistics.failedSmilesCount++;
            } else {
                tmpReturnList.add(tmpMoleculeDataModel);
            }
        };
        switch (tmpInputFileType) {
            case Importer.ValidImportFileTypes.MOL_FILE:
                this.importMolFile(aFile, tmpMoleculeProcessor);
                break;
            case Importer.ValidImportFileTypes.STRUCTURE_DATA_FORMAT_FILE:
                this.importSDFile(aFile, tmpMoleculeProcessor);
                break;
            //Needs more work before it can be made available
            /*case ".pdb":
                tmpImportedMoleculesSet = this.importPDBFile(aFile);
                break;*/
            case Importer.ValidImportFileTypes.SMILES_FILE,
                 Importer.ValidImportFileTypes.TEXT_FILE,
                 Importer.ValidImportFileTypes.COMMA_SEPARATED_VALUES_FILE,
                 Importer.ValidImportFileTypes.TAB_SEPARATED_VALUES_FILE:
                this.importSMILESFile(aFile, tmpMoleculeProcessor);
                break;
            default:
                throw new UnsupportedOperationException(String.format("Input file type %s is defined but not treated " +
                        "in Importer.importMoleculeFile() yet.", tmpInputFileType.toString()));
        }
        this.fileName = aFile.getName();
        if (tmpStatistics.processedMoleculesCount > 0) {
            this.logPreprocessingSummary(tmpStatistics, isFillOpenValencesWithImplH);
            Importer.LOGGER.log(Level.INFO, () -> String.format("Successfully imported %d molecules from file: %s; " +
                    "%d molecules could not be parsed into the internal data model (SMILES code generation failed). " +
                    "See above how many molecules could not be read from the input file at all or produced exceptions while preprocessing.",
                    tmpReturnList.size(), this.getFileName(), tmpStatistics.failedSmilesCount));
        }
        return tmpReturnList;
    }
    //
    /**
     * Returns the name of the last successfully imported file. Might be null if no file was imported yet.
     *
     * @return file name
     */
    public String getFileName(){
        return this.fileName;
    }
    //
    /**
     * Sets the listener that is notified about the progress of subsequent imports. Null removes the current listener.
     *
     * @param aProgressListener listener to notify or null
     */
    public void setProgressListener(ImportProgressListener aProgressListener) {
        this.progressListener = aProgressListener;
    }
    //
    /**
     * Opens a file chooser and loads the chosen file.
     *
     * @param aParentStage Stage where FileChooser should be shown
     * @return File which should contain molecules or null if no file is imported
     * @throws NullPointerException if the given stage is null
     */
    public File openFile(Stage aParentStage) throws NullPointerException {
        Objects.requireNonNull(aParentStage, "aParentStage (instance of Stage) is null");
        FileChooser tmpFileChooser = new FileChooser();
        tmpFileChooser.setTitle(Message.get("Importer.fileChooser.title"));
        List<String> tmpFormattedExtensionsList = new ArrayList<>(10);
        for (String tmpUnformattedExtension : Importer.VALID_IMPORT_FILE_EXTENSIONS_SET) {
            tmpFormattedExtensionsList.add("*" + tmpUnformattedExtension);
        }
        tmpFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                Message.get("Importer.fileChooser.MoleculeFilter.Description"),
                tmpFormattedExtensionsList));
        File tmpRecentDirectory = new File(this.settingsContainer.getRecentDirectoryPathSetting());
        if (!tmpRecentDirectory.isDirectory()) {
            tmpRecentDirectory = new File(SettingsContainer.RECENT_DIRECTORY_PATH_SETTING_DEFAULT);
            this.settingsContainer.setRecentDirectoryPathSetting(SettingsContainer.RECENT_DIRECTORY_PATH_SETTING_DEFAULT);
            Importer.LOGGER.log(Level.INFO, "Recent directory could not be read, resetting to default.");
        }
        tmpFileChooser.setInitialDirectory(tmpRecentDirectory);
        File tmpFile;
        try {
           tmpFile = tmpFileChooser.showOpenDialog(aParentStage);
           if (tmpFile != null) {
               this.settingsContainer.setRecentDirectoryPathSetting(tmpFile.getParent() + File.separator);
           }
           return tmpFile;
        } catch (Exception anException){
           Importer.LOGGER.log(Level.SEVERE, anException.toString(), anException);
           GuiUtil.guiExceptionAlert(
                   Message.get("Error.ExceptionAlert.Title"),
                   Message.get("Importer.FileImportExceptionAlert.Header"),
                   Message.get("Importer.FileImportExceptionAlert.Text") + "\n" + LogUtil.getLogFileDirectoryPath(),
                   anException);
           return null;
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="private methods" defaultstate="collapsed">
    /**
     * Imports a mol file as AtomContainer and adds the first line of the mol file (name of the
     * molecule in most cases) as "name-property". MDL v2000 and v3000 MOL files are accepted and the used format
     * determined automatically.
     *
     * @param aFile mol file
     * @param aMoleculeConsumer consumer that the imported molecule is passed to
     * @throws CDKException if the given mol file cannot be read
     * @throws IOException if the given file cannot be found or read
     */
    private void importMolFile(File aFile, Consumer<IAtomContainer> aMoleculeConsumer) throws IOException, CDKException {
        this.reportProgress(Importer.ImportPhase.IMPORTING, 0, 1, false);
        IChemFormat tmpFormat;
        try (BufferedInputStream tmpInputStream = new BufferedInputStream(new FileInputStream(aFile))) {
            FormatFactory tmpFactory = new FormatFactory();
            tmpFormat = tmpFactory.guessFormat(tmpInputStream);
        }
        if (tmpFormat == null) {
            throw new CDKException("The given file type could not be determined");
        }
        IAtomContainer tmpAtomContainer;
        if (tmpFormat.getFormatName().equalsIgnoreCase(MDLV2000Format.getInstance().getFormatName())) {
            try (MDLV2000Reader tmpReader = new MDLV2000Reader(new FileInputStream(aFile), IChemObjectReader.Mode.RELAXED)) {
                tmpAtomContainer = tmpReader.read(new AtomContainer());
            }
        } else if (tmpFormat.getFormatName().equalsIgnoreCase(MDLV3000Format.getInstance().getFormatName())) {
            try (MDLV3000Reader tmpReader = new MDLV3000Reader(new FileInputStream(aFile), IChemObjectReader.Mode.RELAXED)) {
                tmpAtomContainer = tmpReader.read(new AtomContainer());
            }
        } else {
            throw new CDKException("The mol file does not correspond to either the MDLV2000 or the MDLV3000 format and " +
                    "therefore cannot be imported.");
        }
        String tmpName = this.findMoleculeName(tmpAtomContainer);
        if (tmpName == null) {
            try (BufferedReader tmpBufferedReader = new BufferedReader(new FileReader(aFile))) {
                tmpName = tmpBufferedReader.readLine();
                if (tmpName == null || tmpName.isBlank() || tmpName.isEmpty()) {
                    tmpName = FileUtil.getFileNameWithoutExtension(aFile);
                }
            }
        }
        tmpAtomContainer.setProperty(Importer.MOLECULE_NAME_PROPERTY_KEY, tmpName);
        aMoleculeConsumer.accept(tmpAtomContainer);
        this.reportProgress(Importer.ImportPhase.IMPORTING, 1, 1, true);
    }
    //
    /**
     * Imports an SD file. If no name can be detected for a structure, the file name extended with the index of the
     * structure in the file is used as name of the structure.
     * NOTE: if multiple erroneous entries in a row are there in the input file, they are skipped together and not
     * logged individually!
     *
     * @param aFile sdf
     * @param aMoleculeConsumer consumer that each successfully read molecule is passed to, in the order of the file
     * @throws IOException if a file input stream cannot be opened or closed for the given file
     */
    private void importSDFile(File aFile, Consumer<IAtomContainer> aMoleculeConsumer) throws IOException {
        //the extra pass over the file is only worth it if someone is interested in the progress
        int tmpTotalCount = Objects.isNull(this.progressListener) ? -1
                : this.countMatchingLines(aFile, aLine -> aLine.startsWith("$$$$"));
        /*the IteratingSDFReader is not set to skip erroneous input molecules in its constructor to be able to log them*/
        try (IteratingSDFReader tmpSDFReader = new IteratingSDFReader(new FileInputStream(aFile), SilentChemObjectBuilder.getInstance())) {
            int tmpCounter = 0;
            int tmpReadMoleculesCount = 0;
            while (!Thread.currentThread().isInterrupted()) {
                //a last entry without delimiter is not counted, so the total must not fall behind the counter
                this.reportProgress(Importer.ImportPhase.IMPORTING, tmpCounter, Math.max(tmpTotalCount, tmpCounter), false);
                //end of file or encountered erroneous entry
                if (!tmpSDFReader.hasNext()) {
                    //skip if it is an erroneous entry
                    tmpSDFReader.setSkip(true);
                    if (!tmpSDFReader.hasNext()) {
                        if (tmpCounter == 0) {
                            Importer.LOGGER.log(Level.WARNING, "Import failed for first and only structure in the file");
                        }
                        // there is no next, end of file!
                        break;
                    }
                    // molecule just could not be read and has therefore been skipped, restore skip setting for next iteration
                    tmpSDFReader.setSkip(false);
                    Importer.LOGGER.log(Level.WARNING, "Import failed for structure: {0} (index of structure in file).", tmpCounter);
                    tmpCounter++;
                }
                IAtomContainer tmpAtomContainer = tmpSDFReader.next();
                String tmpName = this.findMoleculeName(tmpAtomContainer);
                if (tmpName == null || tmpName.isBlank()) {
                    // the counter here equals the index of the structure in the file
                    tmpName = FileUtil.getFileNameWithoutExtension(aFile) + tmpCounter;
                }
                tmpAtomContainer.setProperty(Importer.MOLECULE_NAME_PROPERTY_KEY, tmpName);
                aMoleculeConsumer.accept(tmpAtomContainer);
                tmpReadMoleculesCount++;
                tmpCounter++;
            }
            this.reportProgress(Importer.ImportPhase.IMPORTING, tmpCounter, Math.max(tmpTotalCount, tmpCounter), true);
            int tmpFailedImportsCount = tmpCounter - tmpReadMoleculesCount;
            if (tmpFailedImportsCount > 0) {
                Importer.LOGGER.log(Level.WARNING, "The import from SD file failed for a total of {0} structure(s).", tmpFailedImportsCount);
            }
        }
    }
    //
    /**
     * Counts the lines of the given file that match the given filter, e.g. the lines starting with the record
     * delimiter "$$$$" in an SD file. This is much faster than parsing the entries. Stops early if the current thread
     * is interrupted.
     *
     * @param aFile file to scan
     * @param aLineFilter filter returning true for the lines to count
     * @return number of matching lines in the file
     * @throws IOException if the file cannot be read
     */
    private int countMatchingLines(File aFile, Predicate<String> aLineFilter) throws IOException {
        int tmpCount = 0;
        this.reportProgress(Importer.ImportPhase.COUNTING, tmpCount, -1, false);
        try (BufferedReader tmpReader = new BufferedReader(new FileReader(aFile))) {
            String tmpLine;
            while ((tmpLine = tmpReader.readLine()) != null && !Thread.currentThread().isInterrupted()) {
                if (aLineFilter.test(tmpLine)) {
                    tmpCount++;
                    this.reportProgress(Importer.ImportPhase.COUNTING, tmpCount, -1, false);
                }
            }
        }
        this.reportProgress(Importer.ImportPhase.COUNTING, tmpCount, -1, true);
        return tmpCount;
    }
    //
    /**
     * Notifies the progress listener, if one is set. To keep the overhead low for large files, the listener is only
     * notified every time another thousandth of the total count has been processed (or another 1,000 entries if the
     * total is unknown) or if the report is forced, e.g. at the end of a phase.
     *
     * @param aPhase current import phase
     * @param aProcessedCount number of entries processed in the current phase so far
     * @param aTotalCount total number of entries to process in the current phase or -1 if unknown
     * @param isReportForced whether the listener should be notified regardless of the processed count
     */
    private void reportProgress(Importer.ImportPhase aPhase, int aProcessedCount, int aTotalCount, boolean isReportForced) {
        if (Objects.isNull(this.progressListener)) {
            return;
        }
        int tmpStep = aTotalCount > 0 ? Math.max(1, aTotalCount / 1000) : 1000;
        if (isReportForced || aProcessedCount % tmpStep == 0) {
            this.progressListener.progressChanged(aPhase, aProcessedCount, aTotalCount);
        }
    }
    //
    /**
     * Imports a SMILES file. This method is able to parse differently formatted SMILES files, e.g. with and without header
     * and with one up to many columns (SMILES, name/ID, and additional columns). The method identifies the used
     * format by reading the first three lines of the file and then applying the detected format on all further lines.
     * SMILES and name/ID strings are expected to be in the first two columns. Files that do not fit to the expected
     * format or lack a parsable SMILES string in the first ten lines are classified as not being a SMILES file and
     * an exception gets thrown. If no name can be detected for a structure, the structure
     * is assigned the name of the file extended with the index of the structure in the file as name.
     *
     * @param aFile a SMILES codes-containing *.txt, *.csv, *.tsv, or *.smi file
     * @param aMoleculeConsumer consumer that each successfully parsed molecule is passed to, in the order of the file
     * @throws IOException if the given file does not fit to the expected format of a SMILES file
     * @author Samuel Behr
     * @author Jonas Schaub
     */
    private void importSMILESFile(File aFile, Consumer<IAtomContainer> aMoleculeConsumer) throws IOException {
        DynamicSMILESFileFormat tmpFormat = DynamicSMILESFileReader.detectFormat(aFile);
        DynamicSMILESFileReader tmpReader = new DynamicSMILESFileReader();
        //the extra pass over the file is only worth it if someone is interested in the progress
        int tmpTotalCount = Objects.isNull(this.progressListener) ? -1
                : Math.max(0, this.countMatchingLines(aFile, aLine -> true) - (tmpFormat.hasHeaderLine() ? 1 : 0));
        //every line after the headline is either passed on as a molecule or counted as skipped by the reader
        AtomicInteger tmpReadMoleculesCount = new AtomicInteger(0);
        this.reportProgress(Importer.ImportPhase.IMPORTING, 0, tmpTotalCount, false);
        // checks whether thread has been interrupted, logs faulty structures, and assigns names like the other methods
        tmpReader.readFile(aFile, tmpFormat, aMolecule -> {
            aMoleculeConsumer.accept(aMolecule);
            int tmpProcessedLinesCount = tmpReadMoleculesCount.incrementAndGet() + tmpReader.getSkippedLinesCounter();
            this.reportProgress(Importer.ImportPhase.IMPORTING, tmpProcessedLinesCount, Math.max(tmpTotalCount, tmpProcessedLinesCount), false);
        });
        int tmpProcessedLinesCount = tmpReadMoleculesCount.get() + tmpReader.getSkippedLinesCounter();
        this.reportProgress(Importer.ImportPhase.IMPORTING, tmpProcessedLinesCount, Math.max(tmpTotalCount, tmpProcessedLinesCount), true);
        if (tmpReader.getSkippedLinesCounter() > 0) {
            Importer.LOGGER.log(Level.WARNING, "The import from SMILES file failed for a total of {0} structures.",
                    tmpReader.getSkippedLinesCounter());
        }
    }
    //
    /**
     * Imports a PDB file.
     *
     * @param aFile PDB file
     * @return the imported molecules in an IAtomContainerSet
     * @throws CDKException if the given PDB file cannot be read
     * @throws IOException if a file input stream cannot be opened or closed for the given file
     * @deprecated Currently out of use! Needs more work before it can be made available. See importMoleculeFile() and loadFile()
     */
    @Deprecated
    private IAtomContainerSet importPDBFile(File aFile) throws IOException, CDKException {
        IAtomContainerSet tmpAtomContainerSet = new AtomContainerSet();
        try (PDBReader tmpPDBReader = new PDBReader(new FileInputStream(aFile))) {
            for (IOSetting setting : tmpPDBReader.getIOSettings()) {
                if (setting.getName().equals("UseRebondTool")) {
                    //default false
                    //CDK seems unable to read all info in the "CONECT" block, and often it is not there at all; therefore, we
                    // re-bond the whole molecule based on the atom distances with this setting
                    // BUT this is unable to re-create double bonds!
                    setting.setSetting("true");
                }
                if (setting.getName().equals("ReadConnectSection")) {
                    //default true
                }
                if (setting.getName().equals("UseHetDictionary")) {
                    //default false
                }
            }
            ChemFile tmpChemFile = tmpPDBReader.read(new ChemFile());
            int tmpCounter = 0;
            for (IAtomContainer tmpAtomContainer : ChemFileManipulator.getAllAtomContainers(tmpChemFile)) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                String tmpName = this.findMoleculeName(tmpAtomContainer);
                if(tmpName == null || tmpName.isBlank() || tmpName.isEmpty())
                    tmpName = FileUtil.getFileNameWithoutExtension(aFile) + tmpCounter;
                tmpAtomContainer.setProperty(Importer.MOLECULE_NAME_PROPERTY_KEY, tmpName);
                tmpAtomContainerSet.addAtomContainer(tmpAtomContainer);
                tmpCounter++;
            }
            return tmpAtomContainerSet;
        }
    }
    //
    /**
     * Searches the properties of the given atom container for a property
     * containing either 'name' or 'ID' in its key string and
     * returns the corresponding value as a string.
     * If nothing is found or value equals 'None', null is returned.
     *
     * @param anAtomContainer IAtomContainer
     * @return the identified molecule name or null
     */
    private String findMoleculeName(IAtomContainer anAtomContainer) {
        String tmpName = anAtomContainer.getTitle();
        Set<String> keySet = (Set<String>)(Set<?>)anAtomContainer.getProperties().keySet();
        if (tmpName == null  && keySet.stream().anyMatch(k -> (k.toLowerCase().contains("name")) && !k.equalsIgnoreCase("Database_Name"))) {
            Optional<String> tmpKeyOptional = keySet.stream().filter(k -> k.toLowerCase().contains("name")).findFirst();
            if (tmpKeyOptional.isPresent()) {
                String key = tmpKeyOptional.get();
                tmpName = anAtomContainer.getProperty(key);
            }
        }
        if ((tmpName == null || tmpName.equalsIgnoreCase("None")) && keySet.stream().anyMatch(k -> k.toLowerCase().contains("id"))) {
            Optional<String> tmpKeyOptional = keySet.stream().filter(k -> k.toLowerCase().contains("id")).findFirst();
            if (tmpKeyOptional.isPresent()) {
                String key = tmpKeyOptional.get();
                tmpName = anAtomContainer.getProperty(key);
            }
        }
        if (tmpName != null && (tmpName.equalsIgnoreCase("None"))) {
            tmpName = null;
        }
        return tmpName;
    }
    //
    /**
     * Iterates over the given molecule set once to do general preprocessing, i.e. assigning atom types and bond orders
     * (kekulization) and suppressing explicit hydrogen atoms that can also be represented as an implicit hydrogen count
     * on the respective atom. If the respective setting is activated, empty valences on the atom are completed with implicit
     * hydrogen atoms as well. Molecules that cause an exception in the routine are logged but remain in the given set.
     * note: Things like assigning bond orders and atom types here is redundant if the atom containers
     * are discarded after molecule set import and molecular information only represented by SMILES codes in
     * the molecule data models. Nevertheless, it is done here to ensure that the generated SMILES codes are correct.
     *
     * @param aMoleculeSet the molecule set to process; may be empty but not null
     * @param isFillOpenValencesWithImplH whether open valences in the imported molecules should be filled with implicit
     *                                    hydrogen atoms
     * @throws NullPointerException if the given molecule set is null
     */
    protected void preprocessMoleculeSet(IAtomContainerSet aMoleculeSet, boolean isFillOpenValencesWithImplH) throws NullPointerException {
        Objects.requireNonNull(aMoleculeSet, "given molecule set is null.");
        if (aMoleculeSet.isEmpty()) {
            return;
        }
        Importer.ImportStatistics tmpStatistics = new Importer.ImportStatistics();
        for (IAtomContainer tmpMolecule : aMoleculeSet.atomContainers()) {
            this.preprocessMolecule(tmpMolecule, isFillOpenValencesWithImplH, tmpStatistics);
        }
        this.logPreprocessingSummary(tmpStatistics, isFillOpenValencesWithImplH);
    }
    //
    /**
     * Does the general preprocessing described in {@link #preprocessMoleculeSet(IAtomContainerSet, boolean)} for a
     * single molecule. An exception in the routine is logged and counted, but the molecule is not discarded.
     *
     * @param aMolecule the molecule to process
     * @param isFillOpenValencesWithImplH whether open valences in the molecule should be filled with implicit hydrogen
     *                                    atoms
     * @param aStatistics statistics to count exceptions and fixed radicals in
     */
    private void preprocessMolecule(IAtomContainer aMolecule, boolean isFillOpenValencesWithImplH, Importer.ImportStatistics aStatistics) {
        try {
            // perceive atom types and configure atoms is always done as preprocessing
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(aMolecule);
            //if open valences should be filled with implicit hydrogens, fix radicals first and then saturate everything
            if (isFillOpenValencesWithImplH) {
                if (aMolecule.getSingleElectronCount() > 0) {
                    ChemUtil.fixRadicals(aMolecule);
                    aStatistics.fixedRadicalsCount++;
                }
                CDKHydrogenAdder.getInstance(aMolecule.getBuilder()).addImplicitHydrogens(aMolecule);
            //otherwise, just set implicit hydrogen counts to zero if unset to prevent exceptions
            } else {
                for (IAtom tmpAtom : aMolecule.atoms()) {
                    if (tmpAtom.getImplicitHydrogenCount() == CDKConstants.UNSET
                            || tmpAtom.getImplicitHydrogenCount() == null) {
                        tmpAtom.setImplicitHydrogenCount(0);
                    }
                }
            }
            /* note: the doc says: "All explicit hydrogens that can safely be converted to a count on their attached
            atom will be suppressed." Therefore, there will still be some explicit hydrogen atoms! */
            AtomContainerManipulator.normalizeHydrogens(aMolecule, HydrogenState.Minimal);
            //might throw exceptions if the implicit hydrogen count is unset or kekulization is impossible
            Kekulization.kekulize(aMolecule);
        } catch (Exception anException) {
            Importer.LOGGER.log(Level.WARNING,
                    String.format("%s molecule name: %s", anException.toString(), aMolecule.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY)),
                    anException);
            aStatistics.preprocessingExceptionsCount++;
            // note: an exception here does not lead to the causing molecule being discarded!
        }
    }
    //
    /**
     * Logs how many exceptions occurred while preprocessing and, if open valences were filled, how many molecules with
     * radicals were fixed.
     *
     * @param aStatistics statistics of the preprocessed molecules
     * @param isFillOpenValencesWithImplH whether open valences were filled with implicit hydrogen atoms
     */
    private void logPreprocessingSummary(Importer.ImportStatistics aStatistics, boolean isFillOpenValencesWithImplH) {
        if (!isFillOpenValencesWithImplH) {
            Importer.LOGGER.log(Level.INFO, "Imported and preprocessed molecule set. {0} exceptions occurred while processing.",
                    aStatistics.preprocessingExceptionsCount);
        } else {
            Importer.LOGGER.log(Level.INFO, "Imported and preprocessed molecule set. {0} exceptions occurred while processing, " +
                            "{1} molecules with radicals were fixed and saturated with implicit hydrogens.",
                    new Object[]{aStatistics.preprocessingExceptionsCount, aStatistics.fixedRadicalsCount});
        }
    }
    //
    /**
     * Creates the MORTAR-internal data model of the given preprocessed molecule. The most time-consuming step of the
     * import is the SMILES generation done here, especially if stereochemistry is regarded because then, the InChI
     * numbering algorithm is used.
     *
     * @param anAtomContainer the preprocessed molecule
     * @param isRegardStereo whether stereochemistry should be encoded in the SMILES string
     * @param isKekulizationEnforced whether the molecule should always be kekulized, which means aromaticity
     *                               will not(!) be encoded in the internal SMILES string (if false, aromaticity will be(!) encoded)
     * @return the data model or null if no SMILES code could be created for the molecule
     */
    private MoleculeDataModel createMoleculeDataModel(IAtomContainer anAtomContainer, boolean isRegardStereo, boolean isKekulizationEnforced) {
        //returns null if no SMILES code could be created
        String tmpSmiles = ChemUtil.createUniqueSmiles(anAtomContainer, isRegardStereo, !isKekulizationEnforced);
        if (tmpSmiles == null || tmpSmiles.isBlank()) {
            return null;
        }
        MoleculeDataModel tmpMoleculeDataModel;
        if (this.settingsContainer.getKeepAtomContainerInDataModelSetting()) {
            tmpMoleculeDataModel = new MoleculeDataModel(anAtomContainer, isRegardStereo);
        } else {
            tmpMoleculeDataModel = new MoleculeDataModel(tmpSmiles, anAtomContainer.getTitle(), anAtomContainer.getProperties());
        }
        tmpMoleculeDataModel.setName(anAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
        return tmpMoleculeDataModel;
    }
    //</editor-fold>
}
