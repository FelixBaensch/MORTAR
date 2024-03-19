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

import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import de.unijena.cheminf.mortar.model.util.LogUtil;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.aromaticity.Kekulization;
import org.openscience.cdk.exception.CDKException;
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Importer.
 *
 * @author Felix Baensch, Samuel Behr, Jonas Schaub
 * @version 1.0.0.0
 */
public class Importer {
    //<editor-fold desc="public static final class constants">
    /**
     * Property key that is used to store the detected molecule names on the imported atom containers.
     */
    public static final String MOLECULE_NAME_PROPERTY_KEY = "MORTAR_IMPORTER_NAME";
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
     * CSV file will be treated
     * as a SMILES file. If the respective setting is activated, incomplete valences of the imported atoms are filled
     * with implicit hydrogen atoms. If no molecule name or ID is given in the input file, the file name with an appended
     * counter is used as such and added to the returned atom containers as a property.
     *
     * @param aFile File to import
     * @return IAtomContainerSet which contains the imported molecules as AtomContainers or null if the file chooser was
     * closed by the user or a not importable file type was chosen
     * @throws CDKException if the given file cannot be parsed
     * @throws IOException if the given file cannot be found or read
     * @throws NullPointerException if the given file is null
     */
    public IAtomContainerSet importMoleculeFile(File aFile) throws NullPointerException, IOException, CDKException {
        Objects.requireNonNull(aFile, "aFile is null");
        String tmpRecentDirFromContainer = this.settingsContainer.getRecentDirectoryPathSetting();
        if(tmpRecentDirFromContainer == null || tmpRecentDirFromContainer.isEmpty()) {
            this.settingsContainer.setRecentDirectoryPathSetting(SettingsContainer.RECENT_DIRECTORY_PATH_SETTING_DEFAULT);
            Importer.LOGGER.log(Level.INFO, "Recent directory could not be read, resetting to default.");
        }
        String tmpFilePath = aFile.getPath();
        String tmpFileExtension = FileUtil.getFileExtension(tmpFilePath);
        this.fileName = aFile.getName();
        IAtomContainerSet tmpImportedMoleculesSet;
        switch (tmpFileExtension) {
            case ".mol":
                tmpImportedMoleculesSet = this.importMolFile(aFile);
                break;
            case ".sdf":
                tmpImportedMoleculesSet = this.importSDFile(aFile);
                break;
            //Needs more work before it can be made available
            /*case ".pdb":
                tmpImportedMoleculesSet = this.importPDBFile(aFile);
                break;*/
            case ".smi", ".txt", ".csv":
                tmpImportedMoleculesSet = this.importSMILESFile(aFile);
                break;
            default:
                return null;
        }
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
        //to make PDB available, add "*.pdb" here
        tmpFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Molecules", "*.mol", "*.sdf", "*.smi", "*.txt", "*.csv"));
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
                   Message.get("Importer.FileImportExceptionAlert.Text") + "\n" +
                           LogUtil.getLogFileDirectoryPath(),
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
    private IAtomContainerSet importMolFile(File aFile) throws IOException, CDKException {
        IAtomContainerSet tmpAtomContainerSet = new AtomContainerSet();
        BufferedInputStream tmpInputStream = new BufferedInputStream(new FileInputStream(aFile));
        FormatFactory tmpFactory = new FormatFactory();
        IChemFormat tmpFormat = tmpFactory.guessFormat(tmpInputStream);
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
        tmpAtomContainerSet.addAtomContainer(tmpAtomContainer);
        tmpInputStream.close();
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
    private IAtomContainerSet importSDFile(File aFile) throws IOException {
        IAtomContainerSet tmpAtomContainerSet = new AtomContainerSet();
        /*the IteratingSDFReader is not set to skip erroneous input molecules in its constructor to be able to log them*/
        try (IteratingSDFReader tmpSDFReader = new IteratingSDFReader(new FileInputStream(aFile), SilentChemObjectBuilder.getInstance())) {
            int tmpCounter = 0;
            while (!Thread.currentThread().isInterrupted()) {
                //end of file or encountered erroneous entry
                if (!tmpSDFReader.hasNext()) {
                    //skip if it is an erroneous entry
                    tmpSDFReader.setSkip(true);
                    if (!tmpSDFReader.hasNext()) {
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
                tmpAtomContainerSet.addAtomContainer(tmpAtomContainer);
                tmpCounter++;
            }
            int tmpFailedImportsCount = tmpCounter - tmpAtomContainerSet.getAtomContainerCount();
            if (tmpFailedImportsCount > 0) {
                Importer.LOGGER.log(Level.WARNING, "The import from SD file failed for a total of {0} structure(s).", tmpFailedImportsCount);
            }
            return tmpAtomContainerSet;
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
     * @param aFile a SMILES codes-containing *.txt, *.csv, or *.smi file
     * @return the imported molecules in an IAtomContainerSet
     * @throws IOException if the given file does not fit to the expected format of a SMILES file
     * @author Samuel Behr, Jonas Schaub
     */
    private IAtomContainerSet importSMILESFile(File aFile) throws IOException {
        DynamicSMILESFileFormat tmpFormat = DynamicSMILESFileReader.detectFormat(aFile);
        DynamicSMILESFileReader tmpReader = new DynamicSMILESFileReader();
        // checks whether thread has been interrupted, logs faulty structures, and assigns names like the other methods
        IAtomContainerSet tmpAtomContainerSet = tmpReader.readFile(aFile, tmpFormat);
        if (tmpReader.getSkippedLinesCounter() > 0) {
            Importer.LOGGER.log(Level.WARNING, "The import from SMILES file failed for a total of {0} structures.",
                    tmpReader.getSkippedLinesCounter());
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
        if (tmpName == null  && keySet.stream().anyMatch(k -> (k.toLowerCase().contains("name")) && !k.equalsIgnoreCase("Database_Name"))){
            Optional<String> tmpKeyOptional = keySet.stream().filter(k -> k.toLowerCase().contains("name")).findFirst();
            if (tmpKeyOptional.isPresent()) {
                String key = tmpKeyOptional.get();
                tmpName = anAtomContainer.getProperty(key);
            }
        }
        if((tmpName == null || tmpName.equalsIgnoreCase("None")) && keySet.stream().anyMatch(k -> k.toLowerCase().contains("id"))){
            Optional<String> tmpKeyOptional = keySet.stream().filter(k -> k.toLowerCase().contains("id")).findFirst();
            if (tmpKeyOptional.isPresent()) {
                String key = tmpKeyOptional.get();
                tmpName = anAtomContainer.getProperty(key);
            }
        }
        if(tmpName != null && (tmpName.equalsIgnoreCase("None"))) {
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
     *         are discarded after molecule set import and molecular information only represented by SMILES codes in
     *         the molecule data models. Nevertheless, it is done here to ensure that the generated SMILES codes are correct.
     *
     * @param aMoleculeSet the molecule set to process; may be empty but not null
     * @throws NullPointerException if the given molecule set is null
     */
    private void preprocessMoleculeSet(IAtomContainerSet aMoleculeSet) throws NullPointerException {
        Objects.requireNonNull(aMoleculeSet, "given molecule set is null.");
        if (aMoleculeSet.isEmpty()) {
            return;
        }
        int tmpExceptionsCounter = 0;
        for (IAtomContainer tmpMolecule : aMoleculeSet.atomContainers()) {
            try {
                AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpMolecule);
                if (this.settingsContainer.getAddImplicitHydrogensAtImportSetting()) {
                    CDKHydrogenAdder.getInstance(tmpMolecule.getBuilder()).addImplicitHydrogens(tmpMolecule);
                }
                /* note: the doc says: "Suppress any explicit hydrogens in the provided container. Only hydrogens that
                can be represented as a hydrogen count value on the atom are suppressed." Therefore, there will
                still be some explicit hydrogen atoms!
                 */
                AtomContainerManipulator.suppressHydrogens(tmpMolecule);
                //might throw exceptions if the implicit hydrogen count is unset or kekulization is impossible
                Kekulization.kekulize(tmpMolecule);
            } catch (Exception anException) {
                Importer.LOGGER.log(Level.WARNING,
                        String.format("%s molecule name: %s", anException.toString(), tmpMolecule.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY)),
                        anException);
                tmpExceptionsCounter++;
            }
        }
        Importer.LOGGER.log(Level.INFO, "Imported and preprocessed molecule set. {0} exceptions occurred while processing.", tmpExceptionsCounter);
    }
    //</editor-fold>
}
