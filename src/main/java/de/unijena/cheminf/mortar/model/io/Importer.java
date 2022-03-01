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

import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import de.unijena.cheminf.mortar.model.util.LogUtil;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Kekulization;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
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
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Importer
 *
 * @author Felix Bänsch, Samuel Behr, Jonas Schaub
 */
public class Importer {
    //<editor-fold desc="public static final class constants">
    /**
     * Property key that is used to store the detected molecule names on the imported atom containers.
     */
    public static final String MOLECULE_NAME_PROPERTY_KEY = "NAME";
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
    private SettingsContainer settingsContainer;
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
        if(tmpRecentDirFromContainer == null || tmpRecentDirFromContainer.isEmpty()) {
            this.settingsContainer.setRecentDirectoryPathSetting(SettingsContainer.RECENT_DIRECTORY_PATH_SETTING_DEFAULT);
        }
        this.fileName = null;
    }
    //</editor-fold>
    //
    //<editor-fold desc="public methods" defaultstate="collapsed">
    /**
     * Imports a molecule file, user can choose between four types - mol, sdf, pdb, and smi. A text file will be treated
     * as a SMILES file. If the respective setting is activated, incomplete valences of the imported atoms are filled
     * with implicit hydrogen atoms. If no molecule name or ID is given in the input file, the file name with an appended
     * counter is used as such and added to the returned atom containers as a property.
     *
     * @param aFile
     * @return IAtomContainerSet which contains the imported molecules as AtomContainers or null if the file chooser was
     * closed by the user or a not importable file type was chosen
     * @throws CDKException if the given file cannot be parsed
     * @throws IOException if the given file cannot be found or read
     * @throws NullPointerException if the given file is null
     * @throws Exception if something goes wrong
     */
    public IAtomContainerSet importMoleculeFile(File aFile) throws NullPointerException, Exception {
        Objects.requireNonNull(aFile, "aFile is null");
        String tmpRecentDirFromContainer = this.settingsContainer.getRecentDirectoryPathSetting();
        if(tmpRecentDirFromContainer == null || tmpRecentDirFromContainer.isEmpty()) {
            this.settingsContainer.setRecentDirectoryPathSetting(SettingsContainer.RECENT_DIRECTORY_PATH_SETTING_DEFAULT);
        }
        String tmpFilePath = aFile.getPath();
        String tmpFileExtension = FileUtil.getFileExtension(tmpFilePath);
        this.fileName = aFile.getName();
        IAtomContainerSet tmpImportedMoleculesSet = null;
        //yes, the method is called inside a new parallel thread by the main view controller but still starts its own new
        // parallel thread internally. This was necessary, don't ask us why
        FutureTask<IAtomContainerSet> tmpFutureTask = new FutureTask<IAtomContainerSet>(
                () -> {
                    switch (tmpFileExtension) {
                        case ".mol":
                            return importMolFile(aFile);
                        case ".sdf":
                            return importSDFile(aFile);
                        case ".pdb":
                            return importPDBFile(aFile);
                        case ".smi":
                        case ".txt":
                            return importSMILESFile(aFile);
                        default:
                            return null;
                    }
                }
        );
        Thread tmpThread = new Thread(tmpFutureTask);
        tmpThread.setUncaughtExceptionHandler(LogUtil.getUncaughtExceptionHandler());
        tmpThread.setPriority(Thread.currentThread().getPriority() - 2); //magic number
        tmpThread.start();
        //no handling of exceptions here because this gets called inside another task/thread in the main view controller
        tmpImportedMoleculesSet = tmpFutureTask.get();
        this.preprocessMoleculeSet(tmpImportedMoleculesSet);
        return tmpImportedMoleculesSet;
    }
    //
    /**
     * Returns the name of the last imported file. Might be null if no file was imported yet.
     *
     * @return file name
     */
    public String getFileName(){
        return this.fileName;
    }
    //</editor-fold>
    //
    //<editor-fold desc="private methods" defaultstate="collapsed">
    /**
     * Opens a file chooser and loads the chosen file.
     *
     * @param aParentStage Stage where FileChooser should be shown
     * @return File which should contain molecules or null if no file is imported
     * @throws NullPointerException if the given stage is null
     */
    public File loadFile(Stage aParentStage) throws NullPointerException {
        Objects.requireNonNull(aParentStage, "aParentStage (instance of Stage) is null");
        FileChooser tmpFileChooser = new FileChooser();
        tmpFileChooser.setTitle(Message.get("Importer.fileChooser.title"));
        tmpFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Molecules", "*.mol", "*.sdf", "*.pdb", "*.smi", "*.txt"));
        File tmpRecentDirectory = new File(this.settingsContainer.getRecentDirectoryPathSetting());
        if(!tmpRecentDirectory.isDirectory()) {
            tmpRecentDirectory = new File(SettingsContainer.RECENT_DIRECTORY_PATH_SETTING_DEFAULT);
            this.settingsContainer.setRecentDirectoryPathSetting(SettingsContainer.RECENT_DIRECTORY_PATH_SETTING_DEFAULT);
        }
        tmpFileChooser.setInitialDirectory(tmpRecentDirectory);
        File tmpFile = null;
        try {
           tmpFile = tmpFileChooser.showOpenDialog(aParentStage);
           if (tmpFile != null) {
               this.settingsContainer.setRecentDirectoryPathSetting(tmpFile.getParent());
           }
        } catch (Exception anException){
           Importer.LOGGER.log(Level.SEVERE, anException.toString(), anException);
           GuiUtil.guiExceptionAlert(
                   Message.get("Error.ExceptionAlert.Title"),
                   Message.get("Importer.FileImportExceptionAlert.Header"),
                   Message.get("Importer.FileImportExceptionAlert.Text") + "\n" +
                           FileUtil.getAppDirPath() + File.separator + BasicDefinitions.LOG_FILES_DIRECTORY + File.separator,
                   anException);
        } finally {
            return tmpFile;
        }
    }
    //
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
    private IAtomContainerSet importMolFile(File aFile) throws IOException, CDKException {
        IAtomContainerSet tmpAtomContainerSet = new AtomContainerSet();
        BufferedInputStream tmpInputStream = new BufferedInputStream(new FileInputStream(aFile));
        FormatFactory tmpFactory = new FormatFactory();
        IChemFormat tmpFormat = tmpFactory.guessFormat(tmpInputStream);
        IAtomContainer tmpAtomContainer;
        if(tmpFormat.getFormatName().equalsIgnoreCase(MDLV2000Format.getInstance().getFormatName())){
            MDLV2000Reader tmpReader = new MDLV2000Reader(new FileInputStream(aFile), IChemObjectReader.Mode.RELAXED);
            tmpAtomContainer = tmpReader.read(new AtomContainer());
        }
        else if(tmpFormat.getFormatName().equalsIgnoreCase(MDLV3000Format.getInstance().getFormatName())){
            MDLV3000Reader tmpReader = new MDLV3000Reader(new FileInputStream(aFile), IChemObjectReader.Mode.RELAXED);
            tmpAtomContainer = tmpReader.read(new AtomContainer());
        }
        else{
            throw new CDKException("The mol file does not correspond to either the MDLV2000 or the MDLV3000 format and " +
                    "therefore cannot be imported.");
        }
        String tmpName = this.findMoleculeName(tmpAtomContainer);
        if(tmpName == null){
            BufferedReader tmpBufferedReader = new BufferedReader(new FileReader(aFile));
            tmpName = tmpBufferedReader.readLine();
            if(tmpName == null || tmpName.isBlank() || tmpName.isEmpty())
                tmpName = FileUtil.getFileNameWithoutExtension(aFile);
            tmpBufferedReader.close();
        }
        tmpAtomContainer.setProperty(Importer.MOLECULE_NAME_PROPERTY_KEY, tmpName);
        tmpAtomContainerSet.addAtomContainer(tmpAtomContainer);
        tmpInputStream.close();
        return tmpAtomContainerSet;
    }
    //
    /**
     * Imports an SD file.
     *
     * @param aFile sdf
     * @return the imported molecules in an IAtomContainerSet
     * @throws FileNotFoundException if a file input stream cannot be opened for the given file
     */
    private IAtomContainerSet importSDFile(File aFile) throws FileNotFoundException {
        IAtomContainerSet tmpAtomContainerSet = new AtomContainerSet();
        IteratingSDFReader tmpSDFReader = new IteratingSDFReader(new FileInputStream(aFile),
                DefaultChemObjectBuilder.getInstance());
        int tmpCounter = 0;
        while(tmpSDFReader.hasNext()){
            IAtomContainer tmpAtomContainer = tmpSDFReader.next();
            String tmpName = this.findMoleculeName(tmpAtomContainer);
            if(tmpName == null || tmpName.isBlank() || tmpName.isEmpty())
                tmpName = FileUtil.getFileNameWithoutExtension(aFile) + tmpCounter;
            tmpAtomContainer.setProperty(Importer.MOLECULE_NAME_PROPERTY_KEY, tmpName);
            tmpAtomContainerSet.addAtomContainer(tmpAtomContainer);
            tmpCounter++;
        }
        return tmpAtomContainerSet;
    }
    //
    /**
     * Imports a PDB file.
     *
     * @param aFile PDB file
     * @return the imported molecules in an IAtomContainerSet
     * @throws CDKException if the given PDB file cannot be read
     * @throws FileNotFoundException if a file input stream cannot be opened for the given file
     */
    private IAtomContainerSet importPDBFile(File aFile) throws FileNotFoundException, CDKException {
        IAtomContainerSet tmpAtomContainerSet = new AtomContainerSet();
        PDBReader tmpPDBReader = new PDBReader(new FileInputStream(aFile));
        IAtomContainer tmpAtomContainer = tmpPDBReader.read(new AtomContainer());
        String tmpName = this.findMoleculeName(tmpAtomContainer);
        if(tmpName == null || tmpName.isBlank() || tmpName.isEmpty())
            tmpName = FileUtil.getFileNameWithoutExtension(aFile);
        tmpAtomContainer.setProperty(Importer.MOLECULE_NAME_PROPERTY_KEY, tmpName);
        tmpAtomContainerSet.addAtomContainer(tmpAtomContainer);
        return tmpAtomContainerSet;
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
        if(tmpName == null  && keySet.stream().anyMatch(k -> k.contains("name") || k.contains("Name") || k.contains("NAME"))){
            String key = keySet.stream().filter(k -> k.contains("name") || k.contains("Name") || k.contains("NAME")).findFirst().get();
            tmpName = anAtomContainer.getProperty(key);
        }
        if((tmpName == null || tmpName.equals("None") || tmpName.equals("NONE")) && keySet.stream().anyMatch(k -> k.contains("id") || k.contains("iD") || k.contains("Id") || k.contains("ID"))){
            String key = keySet.stream().filter(k -> k.contains("id") || k.contains("iD") || k.contains("Id") || k.contains("ID")).findFirst().get();
            tmpName = anAtomContainer.getProperty(key);
        } //TODO: add keys, maybe SMILES
        if(tmpName != null && (tmpName.equals("None") || tmpName.equals("NONE")))
            tmpName = null;
        return tmpName;
    }
    //
    /**
     * Iterates over the given molecule set once to do general preprocessing, i.e. assigning atom types and bond orders
     * (kekulization) and suppressing explicit hydrogen atoms that can also be represented as an implicit hydrogen count
     * on the respective atom. If the respective setting is activated, empty valences on the atom are completed with implicit
     * hydrogen atoms as well. Molecules that cause an exception in the routine are logged but remain in the given set.
     *
     * @param aMoleculeSet the molecule set to process; may be empty but not null
     * @throws NullPointerException if the given molecule set is null
     */
    private void preprocessMoleculeSet(IAtomContainerSet aMoleculeSet) throws NullPointerException {
        Objects.requireNonNull(aMoleculeSet, "given molecule set is null.");
        if (aMoleculeSet.isEmpty()) {
            return;
        }
        /* note: Things like assigning bond orders and atom types here is redundant if the atom containers
        are discarded after molecule set import and molecular information only represented by SMILES codes in
        the molecule data models. Nevertheless it is done here to ensure that the generated SMILES codes are correct.
         */
        int tmpExceptionsCounter = 0;
        for (IAtomContainer tmpMolecule : aMoleculeSet.atomContainers()) {
            try {
                AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpMolecule);
                /* note: the doc says: "Suppress any explicit hydrogens in the provided container. Only hydrogens that
                can be represented as a hydrogen count value on the atom are suppressed." Therefore, there will
                still be some explicit hydrogen atoms!
                 */
                AtomContainerManipulator.suppressHydrogens(tmpMolecule);
                if (this.settingsContainer.getAddImplicitHydrogensAtImportSetting()) {
                    CDKHydrogenAdder.getInstance(tmpMolecule.getBuilder()).addImplicitHydrogens(tmpMolecule);
                }
                //might throw exceptions if the implicit hydrogen count is unset or kekulization is impossible
                Kekulization.kekulize(tmpMolecule);
            } catch (Exception anException) {
                Importer.LOGGER.log(Level.WARNING, anException.toString() + " molecule name: "
                        + tmpMolecule.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY), anException);
                tmpExceptionsCounter++;
            }
        }
        Importer.LOGGER.log(Level.INFO, "Imported and preprocessed molecule set. " + tmpExceptionsCounter + " exceptions occurred.");
    }
    //</editor-fold>
    //
    //<editor-fold desc="protected methods" defaultstate="collapsed">
    /**
     * Imports a SMILES file. This method is able to parse different types of SMILES files, e.g. with and without header
     * or with only one column or two (SMILES and name/ID, which is in which column is detected).
     * Protected and not private for testing in class ImporterTest.
     *
     * @param aFile a SMILES codes-containing *.txt or *.smi file
     * @return the imported molecules in an IAtomContainerSet
     * @throws IOException if the given file does not fit to the expected format of a SMILES file
     * @author Samuel Behr
     */
    protected IAtomContainerSet importSMILESFile(File aFile) throws IOException {
        try (
                FileReader tmpSmilesFileReader = new FileReader(aFile);
                BufferedReader tmpSmilesFileBufferedReader = new BufferedReader(tmpSmilesFileReader, BasicDefinitions.BUFFER_SIZE)
        ) {
            IAtomContainerSet tmpAtomContainerSet = new AtomContainerSet();
            //AtomContainer to save the parsed SMILES in
            IAtomContainer tmpMolecule = new AtomContainer();
            SmilesParser tmpSmilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
            String tmpSmilesFileNextLine = "";
            String tmpSmilesFileDeterminedSeparator = "";
            String[] tmpProcessedLineArray;
            int tmpSmilesCodeExpectedPosition = 0;
            int tmpIDExpectedPosition = 0;
            int tmpSmilesFileParsableLinesCounter = 0;
            int tmpSmilesFileInvalidLinesCounter = 0;
            //marking the BufferedReader to reset the reader after checking the format and determining the separator
            tmpSmilesFileBufferedReader.mark(BasicDefinitions.BUFFER_SIZE);
            //as potential headline the first line should be avoided for separator determination
            String tmpSmilesFileFirstLine = tmpSmilesFileBufferedReader.readLine();
            /*  first block
                Checking for parsable SMILES code and saving the determined separator (if one is used).
                If no parsable SMILES code is found in the second and third line of the file, tmpMolecule stays empty
                and the file is assumed to be no SMILES file -> return null
             */
            int tmpFilesLine = 2;
            findSeparatorLoop:
            while (tmpFilesLine <= 3) {
                if ((tmpSmilesFileNextLine = tmpSmilesFileBufferedReader.readLine()) == null) {
                    //if the file's end is reached at this point, the first line is used to determine the separator
                    if (tmpSmilesFileFirstLine != null || !tmpSmilesFileFirstLine.isEmpty()) {
                        tmpSmilesFileNextLine = tmpSmilesFileFirstLine;
                        tmpSmilesFileFirstLine = null;
                    } else {
                        break;
                    }
                }
                for (String tmpSeparator : BasicDefinitions.POSSIBLE_SMILES_FILE_SEPARATORS) {
                    //maximum of two array elements expected, otherwise the separator or the line itself are assumed to be invalid
                    tmpProcessedLineArray = tmpSmilesFileNextLine.split(tmpSeparator, 3);
                    if (tmpProcessedLineArray.length > 2) {
                        continue;
                    }
                    int tmpIndex = 0;
                    for (String tmpNextElementOfLine : tmpProcessedLineArray) {
                        if (tmpNextElementOfLine.isEmpty()) {
                            continue;
                        }
                        try {
                            tmpMolecule = tmpSmilesParser.parseSmiles(tmpNextElementOfLine);
                            if (!tmpMolecule.isEmpty()) {
                                tmpSmilesFileDeterminedSeparator = tmpSeparator;
                                tmpSmilesCodeExpectedPosition = tmpIndex;
                                if (tmpProcessedLineArray.length > 1) {
                                    if (tmpSmilesCodeExpectedPosition == 0) {
                                        tmpIDExpectedPosition = 1;
                                    } else {
                                        tmpIDExpectedPosition = 0;
                                    }
                                }
                                break findSeparatorLoop;
                            }
                        } catch (InvalidSmilesException anException) {
                            tmpIndex++;
                        }
                    }
                }
                tmpFilesLine++;
            }
            if (tmpMolecule.isEmpty()) {
                throw new IOException("Chosen file does not fit to the expected format of a SMILES file.");
            }
            //resetting the BufferedReader to the first line of the file
            tmpSmilesFileBufferedReader.reset();
            tmpSmilesFileBufferedReader.mark(0);    //to avoid the memory of unnecessary data
            /*  second block
                Reading the file line by line and adding an AtomContainer to the AtomContainerSet for each line with parsable SMILES code
             */
            while ((tmpSmilesFileNextLine = tmpSmilesFileBufferedReader.readLine()) != null) {
                //trying to parse as SMILES code
                try {
                    tmpProcessedLineArray = tmpSmilesFileNextLine.split(tmpSmilesFileDeterminedSeparator, 2);
                    if (!tmpProcessedLineArray[tmpSmilesCodeExpectedPosition].isEmpty()) {
                        tmpMolecule = tmpSmilesParser.parseSmiles(tmpProcessedLineArray[tmpSmilesCodeExpectedPosition]);
                        tmpSmilesFileParsableLinesCounter++;
                    } else {
                        tmpSmilesFileInvalidLinesCounter++;
                        continue;
                    }
                } catch (InvalidSmilesException | IndexOutOfBoundsException anException) {  //case: invalid line or SMILES code
                    tmpSmilesFileInvalidLinesCounter++;
                    continue;
                }
                //setting the name of the atom container
                String tmpName = "";
                if (tmpProcessedLineArray.length > 1 && !tmpProcessedLineArray[tmpIDExpectedPosition].isEmpty()) {
                    tmpName = tmpProcessedLineArray[tmpIDExpectedPosition];
                } else {
                    tmpName = FileUtil.getFileNameWithoutExtension(aFile) + tmpSmilesFileParsableLinesCounter;
                }
                tmpMolecule.setProperty(Importer.MOLECULE_NAME_PROPERTY_KEY, tmpName);
                //adding tmpMolecule to the AtomContainerSet
                tmpAtomContainerSet.addAtomContainer(tmpMolecule);
            }
            Importer.LOGGER.log(Level.INFO, "\tSmilesFile ParsableLinesCounter:\t" + tmpSmilesFileParsableLinesCounter +
                    "\n\tSmilesFile InvalidLinesCounter:\t\t" + tmpSmilesFileInvalidLinesCounter);
            return tmpAtomContainerSet;
        }
    }
    //</editor-fold>
}
