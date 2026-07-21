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
import de.unijena.cheminf.mortar.model.util.MORTARException;

import javafx.application.Platform;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

    private ExecutorService executorService;
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
        List<IAtomContainer> tmpImportedMoleculesSet = switch (tmpInputFileType) {
            case ValidImportFileTypes.MOL_FILE -> this.importMolFile(aFile);
            case ValidImportFileTypes.STRUCTURE_DATA_FORMAT_FILE -> this.importSDFile(aFile);
            //Needs more work before it can be made available
            /*case ".pdb":
                tmpImportedMoleculesSet = this.importPDBFile(aFile);
                break;*/
            case ValidImportFileTypes.SMILES_FILE, ValidImportFileTypes.TEXT_FILE,
                 ValidImportFileTypes.COMMA_SEPARATED_VALUES_FILE, ValidImportFileTypes.TAB_SEPARATED_VALUES_FILE ->
                    this.importSMILESFile(aFile);
        };
        this.preprocessMoleculeSet(tmpImportedMoleculesSet, isFillOpenValencesWithImplH);
        this.fileName = aFile.getName();
        // TODO: properly look at the mainview controller and take inspiration how the parallel startFragmentation
        //       method gets called and try-catched!
        List<MoleculeDataModel> tmpReturnList = null;
        // tmpReturnList = this.parse(tmpImportedMoleculesSet, isRegardStereo, isKekulizationEnforced);
        try {
            // TODO: properly formulate this comment!
            // NOTE: The estimation of the number of threads was done on a 32 GB RAM and 16 Core laptop and
            //       was determined to be around 8 threads with an efficiency of about 80 percent.
            tmpReturnList =  this.parseParallel(tmpImportedMoleculesSet, isRegardStereo, isKekulizationEnforced, settingsContainer.getNumberOfTasksForFragmentationSetting());
        } catch (Exception anException) {
            // TODO: What to do with the interrupted Exception (maybe look at the fragmentation service code?)
            // NOTE:
            GuiUtil.guiExceptionAlert(Message.get("Importer.FileImportExceptionAlert.Title"),
                    Message.get("Importer.FileImportExceptionAlert.Header"),
                    Message.get("Importer.FileImportOOME.Content"),
                    anException);
        }
        return tmpReturnList;
    }
    //
    /**
     * Parses an atom container set into a list of the MORTAR-internal MoleculeDataModel instances. If the parameter is null or empty, an empty
     * list is returned. Most time-consuming step is the SMILES generation, especially if stereochemistry is regarded because
     * then, the InChI numbering algorithm is used. Logs the size of the input data set and the number of exceptions that occurred during
     * SMILES generation (leads to molecule not being parsed into MoleculeDataModel).
     *
     * @param anAtomContainerSet the set to parse
     * @param isRegardStereo whether stereochemistry should be encoded in the SMILES strings
     * @param isKekulizationEnforced whether imported molecules should always be kekulized, which means aromaticity
     *                               will not(!) be encoded in the internal SMILES strings (if false, aromaticity will be(!) encoded)
     * @return list of MoleculeDataModel instances or empty list if the input set is empty or null
     */
    private List<MoleculeDataModel> parse(List<IAtomContainer> anAtomContainerSet, boolean isRegardStereo, boolean isKekulizationEnforced) {
        if (anAtomContainerSet == null || anAtomContainerSet.isEmpty()) {
            return new ArrayList<>(0);
        }
        List<MoleculeDataModel> tmpReturnList = new ArrayList<>(anAtomContainerSet.size());
        int tmpExceptionCount = 0;
        for (IAtomContainer tmpAtomContainer : anAtomContainerSet) {
            //returns null if no SMILES code could be created
            String tmpSmiles = ChemUtil.createUniqueSmiles(tmpAtomContainer, isRegardStereo, !isKekulizationEnforced);
            if (tmpSmiles == null || tmpSmiles.isBlank()) {
                tmpExceptionCount++;
                continue;
            }
            MoleculeDataModel tmpMoleculeDataModel;
            if (this.settingsContainer.getKeepAtomContainerInDataModelSetting()) {
                tmpMoleculeDataModel = new MoleculeDataModel(tmpAtomContainer, isRegardStereo);
            } else {
                tmpMoleculeDataModel = new MoleculeDataModel(tmpSmiles, tmpAtomContainer.getTitle(), tmpAtomContainer.getProperties());
            }
            tmpMoleculeDataModel.setName(tmpAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
            tmpReturnList.add(tmpMoleculeDataModel);
        }
        int finalTmpExceptionCount = tmpExceptionCount;
        Importer.LOGGER.log(Level.INFO, () -> String.format("Successfully imported %d molecules from file: %s; " +
                "%d molecules could not be parsed into the internal data model (SMILES code generation failed). " +
                "See above how many molecules could not be read from the input file at all or produced exceptions while preprocessing.",
                anAtomContainerSet.size() - finalTmpExceptionCount, this.getFileName(), finalTmpExceptionCount));
        return tmpReturnList;
    }
    //
    private record ParseResult(int exceptionCount, List<MoleculeDataModel> resultList) {}
    //
    /**
     * Parallelized parsing of a list of atom containers into a list of the MORTAR-internal MoleculeDataModel instances.
     * If the parameter is null or empty, an empty list is returned. Most time-consuming step is the SMILES generation,
     * especially if stereochemistry is regarded because then, the InChI numbering algorithm is used.
     * Logs the size of the input data set and the number of exceptions that occurred during
     * SMILES generation (leads to molecule not being parsed into MoleculeDataModel).
     *
     * @param isRegardStereo whether stereochemistry should be encoded in the SMILES strings
     * @param isKekulizationEnforced whether imported molecules should always be kekulized, which means aromaticity
     *                               will not(!) be encoded in the internal SMILES strings (if false, aromaticity will be(!) encoded)
     * @return list of MoleculeDataModel instances or empty list if the input set is empty or null
     */
    private List<MoleculeDataModel> parseParallel(List<IAtomContainer> aListOfMolecules,
                                                  boolean isRegardStereo,
                                                  boolean isKekulizationEnforced,
                                                  int aNumberOfTasks)
            throws InterruptedException {
        if (aListOfMolecules.isEmpty() || aNumberOfTasks == 0) {
            return new ArrayList<>(0);
        }
        int tmpNumberOfTasks = aNumberOfTasks;
        List<MoleculeDataModel> tmpMoleculeResultList = new ArrayList<>(aListOfMolecules.size());
        if (aListOfMolecules.size() < tmpNumberOfTasks) {
            tmpNumberOfTasks = aListOfMolecules.size();
        }
        int tmpMoleculesPerTask = aListOfMolecules.size() / tmpNumberOfTasks;
        int tmpMoleculeModulo = aListOfMolecules.size() % tmpNumberOfTasks;
        int tmpFromIndex = 0; //low endpoint (inclusive) of the subList
        int tmpToIndex = tmpMoleculesPerTask; //high endpoint (exclusive) of the subList
        if(tmpMoleculeModulo > 0){
            tmpToIndex++;
            tmpMoleculeModulo--;
        }
        this.executorService = Executors.newFixedThreadPool(tmpNumberOfTasks, tmpThreadFactory -> {
            // note: the Callables used as threads here catch basically everything
            // and wrap it in an ExecutionException; setting the UncaughtExceptionHandler
            // anyway just to be sure
            Thread tmpThread = new Thread(tmpThreadFactory);
            tmpThread.setUncaughtExceptionHandler(LogUtil.getUncaughtExceptionHandler());
            return tmpThread;
        });
        /* Explicit version that can be used to override methods:
        this.executorService =  new ThreadPoolExecutor(tmpNumberOfTasks, tmpNumberOfTasks, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>()) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
            }
        }; */
        List<Callable<ParseResult>> tmpFragmentationTaskList = new LinkedList<>();
        for (int i = 1; i <= tmpNumberOfTasks; i++) {
            List<IAtomContainer> tmpMoleculesForTask = aListOfMolecules.subList(tmpFromIndex, tmpToIndex);
            tmpFragmentationTaskList.add(() -> {
                int tmpExceptionCount = 0;
                ArrayList<MoleculeDataModel> tmpReturnList = new ArrayList<>(tmpMoleculesForTask.size());
                for (IAtomContainer tmpAtomContainer : tmpMoleculesForTask) {
                    String tmpSmiles = ChemUtil.createUniqueSmiles(tmpAtomContainer, isRegardStereo, !isKekulizationEnforced);
                    if (tmpSmiles == null || tmpSmiles.isBlank()) {
                        tmpExceptionCount++;
                        continue;
                    }
                    MoleculeDataModel tmpMoleculeDataModel;
                    if (settingsContainer.getKeepAtomContainerInDataModelSetting()) {
                        tmpMoleculeDataModel = new MoleculeDataModel(tmpAtomContainer, isRegardStereo);
                    } else {
                        tmpMoleculeDataModel = new MoleculeDataModel(tmpSmiles, tmpAtomContainer.getTitle(), tmpAtomContainer.getProperties());
                    }
                    tmpMoleculeDataModel.setName(tmpAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
                    tmpReturnList.add(tmpMoleculeDataModel);
                }
                return new ParseResult(tmpExceptionCount, tmpReturnList);
            });
            tmpFromIndex = tmpToIndex;
            tmpToIndex = tmpFromIndex + tmpMoleculesPerTask;
            if(tmpMoleculeModulo > 0){
                tmpToIndex++;
                tmpMoleculeModulo--;
            }
            if (i == tmpNumberOfTasks - 1 ) {
                tmpToIndex = aListOfMolecules.size();
            }
        }
        List<Future<ParseResult>> tmpFuturesList;
        long tmpMemoryConsumption = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024*1024);
        Importer.LOGGER.log(Level.INFO, "Parsing-thread starting. Current memory consumption: {0} MB", tmpMemoryConsumption);
        long tmpStartTime = System.currentTimeMillis();
        int tmpExceptionsCounter = 0;
        tmpFuturesList = this.executorService.invokeAll(tmpFragmentationTaskList);
        if (this.executorService.isShutdown() || this.executorService.isTerminated()) {
            Importer.LOGGER.log(Level.INFO, "Fragmentation cancelled");
            return null;
        }
        List<Exception> tmpFutureExceptionsList = new ArrayList<>(tmpFuturesList.size());
        for (Future<ParseResult> tmpFuture : tmpFuturesList) {
            try {
                ParseResult tmpResult = tmpFuture.get();
                if (!Objects.isNull(tmpResult)) {
                    tmpExceptionsCounter += tmpResult.exceptionCount;
                    tmpMoleculeResultList.addAll(tmpResult.resultList);
                } else {
                    // go to catch
                    throw new ExecutionException(new MORTARException("Result of parallel computation task was null for unknown reason."));
                }
            } catch (CancellationException | InterruptedException | ExecutionException aCancellationOrInterruptionException) {
                Importer.LOGGER.log(Level.SEVERE, aCancellationOrInterruptionException.toString(), aCancellationOrInterruptionException);
                tmpFutureExceptionsList.add(aCancellationOrInterruptionException);
                // probably does nothing because a thread can interrupt itself any time
                Thread.currentThread().interrupt();
                //continue;
            }
        }
        if (!tmpFutureExceptionsList.isEmpty()) {
            int tmpOOMEIndex = -1;
            for (int i = 0; i < tmpFutureExceptionsList.size(); i++) {
                Exception tmpCaughtException = tmpFutureExceptionsList.get(i);
                if (tmpCaughtException.getCause() instanceof OutOfMemoryError) {
                    tmpOOMEIndex = i;
                    break;
                }
            }
            if (tmpOOMEIndex != -1) {
                int finalTmpOOMEIndex = tmpOOMEIndex;
                Platform.runLater(() -> {
                    GuiUtil.guiExceptionAlert(Message.get("Importer.FileImportExceptionAlert.Title"),
                            Message.get("Importer.FileImportExceptionAlert.Header"),
                            Message.get("Importer.FileImportOOME.Content"),
                            tmpFutureExceptionsList.get(finalTmpOOMEIndex));
                });
            } else {
                Platform.runLater(() -> {
                    GuiUtil.guiExceptionAlert(Message.get("Importer.FileImportExceptionAlert.Title"),
                            Message.get("Importer.FileImportExceptionAlert.Header"),
                            Message.get("Importer.FileImportOOME.Content"),
                            tmpFutureExceptionsList.getFirst());
                });
            }
        }
        if (tmpExceptionsCounter > 0) {
            Importer.LOGGER.log(Level.WARNING,
                    "{0} molecules caused exceptions during import.",
                    new Object[]{tmpExceptionsCounter});
        }
        this.executorService.shutdown();
        tmpMemoryConsumption = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024*1024);
        long tmpEndTime = System.currentTimeMillis();
        long tmpDuration = tmpEndTime - tmpStartTime;
        Importer.LOGGER.log(Level.INFO,
                "Parsing of {0} molecules complete. It took {1} ms. Current memory consumption: {2} MB",
                new Object[]{aListOfMolecules.size(), tmpDuration, tmpMemoryConsumption});
        return tmpMoleculeResultList;
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
     * @return the imported molecule in an IAtomContainerSet
     * @throws CDKException if the given mol file cannot be read
     * @throws IOException if the given file cannot be found or read
     */
    private List<IAtomContainer> importMolFile(File aFile) throws IOException, CDKException {
        List<IAtomContainer> tmpAtomContainerSet = new ArrayList<>(4096);
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
        tmpAtomContainerSet.add(tmpAtomContainer);
        return tmpAtomContainerSet;
    }
    //
    /**
     * Imports an SD file. If no name can be detected for a structure, the file name extended with the index of the
     * structure in the file is used as name of the structure.
     * NOTE: if multiple erroneous entries in a row are there in the input file, they are skipped together and not
     * logged individually!
     *
     * @param aFile sdf
     * @return the imported molecules in an IAtomContainerSet
     * @throws IOException if a file input stream cannot be opened or closed for the given file
     */
    private List<IAtomContainer> importSDFile(File aFile) throws IOException {
        List<IAtomContainer> tmpAtomContainerList = new ArrayList<>(4096);
        /*the IteratingSDFReader is not set to skip erroneous input molecules in its constructor to be able to log them*/
        try (IteratingSDFReader tmpSDFReader = new IteratingSDFReader(new FileInputStream(aFile), SilentChemObjectBuilder.getInstance())) {
            int tmpCounter = 0;
            while (!Thread.currentThread().isInterrupted()) {
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
                tmpAtomContainerList.add(tmpAtomContainer);
                tmpCounter++;
            }
            int tmpFailedImportsCount = tmpCounter - tmpAtomContainerList.size();
            if (tmpFailedImportsCount > 0) {
                Importer.LOGGER.log(Level.WARNING, "The import from SD file failed for a total of {0} structure(s).", tmpFailedImportsCount);
            }
            return tmpAtomContainerList;
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
     * @return the imported molecules in an IAtomContainerSet
     * @throws IOException if the given file does not fit to the expected format of a SMILES file
     * @author Samuel Behr
     * @author Jonas Schaub
     */
    private List<IAtomContainer> importSMILESFile(File aFile) throws IOException {
        DynamicSMILESFileFormat tmpFormat = DynamicSMILESFileReader.detectFormat(aFile);
        DynamicSMILESFileReader tmpReader = new DynamicSMILESFileReader();
        // checks whether thread has been interrupted, logs faulty structures, and assigns names like the other methods
        List<IAtomContainer> tmpAtomContainerList = tmpReader.readFile(aFile, tmpFormat);
        if (tmpReader.getSkippedLinesCounter() > 0) {
            Importer.LOGGER.log(Level.WARNING, "The import from SMILES file failed for a total of {0} structures.",
                    tmpReader.getSkippedLinesCounter());
        }
        return tmpAtomContainerList;
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
     * @param aMoleculeList the molecule set to process; may be empty but not null
     * @param isFillOpenValencesWithImplH whether open valences in the imported molecules should be filled with implicit
     *                                    hydrogen atoms
     * @throws NullPointerException if the given molecule set is null
     */
    protected void preprocessMoleculeSet(List<IAtomContainer> aMoleculeList, boolean isFillOpenValencesWithImplH) throws NullPointerException {
        Objects.requireNonNull(aMoleculeList, "given molecule set is null.");
        if (aMoleculeList.isEmpty()) {
            return;
        }
        int tmpExceptionsCounter = 0;
        int tmpMoleculesWithRadicalsCounter = 0;
        for (IAtomContainer tmpMolecule : aMoleculeList) {
            try {
                // perceive atom types and configure atoms is always done as preprocessing
                AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpMolecule);
                //if open valences should be filled with implicit hydrogens, fix radicals first and then saturate everything
                if (isFillOpenValencesWithImplH) {
                    if (tmpMolecule.getSingleElectronCount() > 0) {
                        ChemUtil.fixRadicals(tmpMolecule);
                        tmpMoleculesWithRadicalsCounter++;
                    }
                    CDKHydrogenAdder.getInstance(tmpMolecule.getBuilder()).addImplicitHydrogens(tmpMolecule);
                //otherwise, just set implicit hydrogen counts to zero if unset to prevent exceptions
                } else {
                    for (IAtom tmpAtom : tmpMolecule.atoms()) {
                        if (tmpAtom.getImplicitHydrogenCount() == CDKConstants.UNSET
                                || tmpAtom.getImplicitHydrogenCount() == null) {
                            tmpAtom.setImplicitHydrogenCount(0);
                        }
                    }
                }
                /* note: the doc says: "All explicit hydrogens that can safely be converted to a count on their attached
                atom will be suppressed." Therefore, there will still be some explicit hydrogen atoms! */
                AtomContainerManipulator.normalizeHydrogens(tmpMolecule, HydrogenState.Minimal);
                //might throw exceptions if the implicit hydrogen count is unset or kekulization is impossible
                Kekulization.kekulize(tmpMolecule);
            } catch (Exception anException) {
                Importer.LOGGER.log(Level.WARNING,
                        String.format("%s molecule name: %s", anException.toString(), tmpMolecule.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY)),
                        anException);
                tmpExceptionsCounter++;
                // note: an exception here does not lead to the causing molecule being removed from the input set!
            }
        }
        if (!isFillOpenValencesWithImplH) {
            Importer.LOGGER.log(Level.INFO, "Imported and preprocessed molecule set. {0} exceptions occurred while processing.",
                    tmpExceptionsCounter);
        } else {
            Importer.LOGGER.log(Level.INFO, "Imported and preprocessed molecule set. {0} exceptions occurred while processing, " +
                            "{1} molecules with radicals were fixed and saturated with implicit hydrogens.",
                    new Object[]{tmpExceptionsCounter, tmpMoleculesWithRadicalsCounter});
        }
    }
    //</editor-fold>
}
