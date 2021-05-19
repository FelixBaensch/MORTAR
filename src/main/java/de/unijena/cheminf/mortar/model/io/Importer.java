/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2021  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas-schaub@uni-jena.de)
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
import de.unijena.cheminf.mortar.model.util.FileUtil;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.io.IChemObjectReader;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.io.PDBReader;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.*;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Level.INFO;

/**
 * Importer
 *
 * @author Felix Bänsch, Jonas Schaub
 */
public class Importer {

    //<editor-fold defaultstate="collapsed" desc="Public static final class constants">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(Importer.class.getName());
    //</editor-fold>
    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * String path to the recent directory
     */
    private String recentDirectoryPath;
    private String fileName;
    //</editor-fold>
    //
    /**
     * Constructor
     */
    public Importer(){
        if(this.recentDirectoryPath == null || this.recentDirectoryPath.isEmpty())
            this.recentDirectoryPath =  System.getProperty("user.dir");
            //TODO: recent directory string preferences
    }
    //<editor-fold desc="public methods" defaultstate="collapsed">
    /**
     * Imports a molecule file, user can choose between three types - mol, sdf, pdb.
     * @param aParentStage Stage to show the FileChooser
     * @return IAtomContainerSet which contains the imported molecules as AtomContainers
     */
    public IAtomContainerSet Import(Stage aParentStage){
        Objects.requireNonNull(aParentStage, "aParentStage (instance of Stage) is null");
        if(Objects.isNull(this.recentDirectoryPath) || this.recentDirectoryPath.isEmpty()){
            this.recentDirectoryPath =  System.getProperty("user.dir");
        }
        File tmpFile = this.loadFile(aParentStage);
        if(tmpFile == null)
            return null;
        String tmpFileExtension = FileUtil.getFileExtension(tmpFile.getPath());
        this.fileName = tmpFile.getName();
        switch (tmpFileExtension){
            case ".mol":
                return this.ImportMolFile(tmpFile);
            case ".sdf":
                return this.ImportSDFile(tmpFile);
            case ".pdb":
                return this.ImportPDBFile(tmpFile);
            case ".smi":
            case ".txt":
                return this.ImportSMILESFile(tmpFile);
            default:
                return null;
        }
    }
    //</editor-fold>
    public String getFileName(){
        return this.fileName;
    }
    //<editor-fold desc="private methods" defaultstate="collapsed">
    /**
     * Opens a file chooser and loads the chosen file
     * @param aParentStage Stage where FileChooser should be shown
     * @return File which contains molecules
     */
    private File loadFile(Stage aParentStage){
        Objects.requireNonNull(aParentStage, "aParentStage (instance of Stage) is null");
        FileChooser tmpFileChooser = new FileChooser();
        tmpFileChooser.setTitle(Message.get("Importer.fileChooser.title"));
        tmpFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Molecules", "*.mol", "*.sdf", "*.pdb", "*.smi", "*.txt"));
        File tmpRecentDirectory = new File(this.recentDirectoryPath);
        if(!tmpRecentDirectory.isDirectory())
            tmpRecentDirectory = new File(System.getProperty("user.dir"));
        tmpFileChooser.setInitialDirectory(tmpRecentDirectory);
        File tmpFile = null;
        try{
           tmpFile = tmpFileChooser.showOpenDialog(aParentStage);
           if(tmpFile != null){
               this.recentDirectoryPath = tmpFile.getParent();
               //TODO: set recentDirectoryPath to preference
           }
        } catch(Exception anException){
           Importer.LOGGER.log(Level.SEVERE, anException.toString(), anException);
           //TODO handle exception
        } finally {
            return tmpFile;
        }
    }
    //
    /**
     * Imports a mol file as AtomContainer and adds the first line of the mol file (name of the
     * molecule in most cases) as "name-property"
     * @param aFile mol file
     */
    private IAtomContainerSet ImportMolFile(File aFile){
        try{
            IAtomContainerSet tmpAtomContainerSet = new AtomContainerSet();
            MDLV2000Reader tmpReader = new MDLV2000Reader(new FileInputStream(aFile), IChemObjectReader.Mode.RELAXED);
            IAtomContainer tmpAtomContainer = tmpReader.read(new AtomContainer());
            //TODO: add a preference depending if clause to add implicit hydrogens or not
            String tmpName = this.findMoleculeName(tmpAtomContainer);
            if(tmpName == null){
                BufferedReader tmpBufferedReader = new BufferedReader(new FileReader(aFile));
                tmpName = tmpBufferedReader.readLine();
                if(tmpName == null || tmpName.isBlank() || tmpName.isEmpty())
                    tmpName = FileUtil.getFileNameWithoutExtension(aFile);
                tmpBufferedReader.close();
            }
            tmpAtomContainer.setProperty("NAME", tmpName);
            tmpAtomContainerSet.addAtomContainer(tmpAtomContainer);
            return tmpAtomContainerSet;
        }catch(CDKException | IOException anException){
            Importer.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return null;
        }
    }
    //
    /**
     * Imports a SD file
     * @param aFile sdf
     */
    private IAtomContainerSet ImportSDFile(File aFile){
        try{
            IAtomContainerSet tmpAtomContainerSet = new AtomContainerSet();
            IteratingSDFReader tmpSDFReader = new IteratingSDFReader(new FileInputStream(aFile),
                    DefaultChemObjectBuilder.getInstance());
            int tmpCounter = 0;
            while(tmpSDFReader.hasNext()){
                IAtomContainer tmpAtomContainer = tmpSDFReader.next();
                String tmpName = this.findMoleculeName(tmpAtomContainer);
                if(tmpName == null || tmpName.isBlank() || tmpName.isEmpty())
                    tmpName = FileUtil.getFileNameWithoutExtension(aFile) + tmpCounter;
                //TODO: add a preference depending if clause to add implicit hydrogens or not
                tmpAtomContainer.setProperty("NAME", tmpName);
                tmpAtomContainerSet.addAtomContainer(tmpAtomContainer);
                tmpCounter++;
            }
            return tmpAtomContainerSet;
        } catch (FileNotFoundException anException) {
            Importer.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return null;
        }
    }
    //
    /**
     * Imports a PDB file
     * @param aFile PDB file
     */
    private IAtomContainerSet ImportPDBFile(File aFile){
        try{
            IAtomContainerSet tmpAtomContainerSet = new AtomContainerSet();
            PDBReader tmpPDBReader = new PDBReader(new FileInputStream(aFile));
            IAtomContainer tmpAtomContainer = tmpPDBReader.read(new AtomContainer());
            String tmpName = this.findMoleculeName(tmpAtomContainer);
            if(tmpName == null || tmpName.isBlank() || tmpName.isEmpty())
                tmpName = FileUtil.getFileNameWithoutExtension(aFile);
            tmpAtomContainer.setProperty("NAME", tmpName);
            tmpAtomContainerSet.addAtomContainer(tmpAtomContainer);
            return  tmpAtomContainerSet;
        } catch (FileNotFoundException | CDKException anException) {
            Importer.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return null;
        }
    }
    //
    /**
     * Imports a SMILES file.   TODO: specify ImportSMILESFile doc String
     * @param aFile SMILES containing *.txt or *.smi file
     */
    private IAtomContainerSet ImportSMILESFile(File aFile) {
        try {
            IAtomContainerSet tmpAtomContainerSet = new AtomContainerSet();
            FileReader tmpSmilesFileReader = new FileReader(aFile);
            BufferedReader tmpSmilesFileBufferedReader = new BufferedReader(tmpSmilesFileReader);
            SmilesParser tmpSmilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
            String tmpSmilesFileNextLine = "";
            String[] tmpSeparators = {"\t", ";", ",", " "};     //TODO: add other possible separators, if there are any
            int tmpExpectedSmilesCodePosition = 0;
            int tmpExpectedIDPosition = 0;
            int tmpSmilesFileParsableLinesCounter = 0;
            int tmpSmilesFileInvalidLinesCounter = 0;
            String tmpSmilesFileFirstLine = tmpSmilesFileBufferedReader.readLine(); //first line gets special treatment (could be headline)
            IAtomContainer tmpMolecule = new AtomContainer();   //AtomContainer to safe the parsed SMILES in
            String tmpInTheFileUsedSeparator = "";
            String[] tmpProcessedLine = new String[3];
            int tmpFilesLine = 2;
            //test, if there is a parsable SMILES code in the second or third line of the file and check for the in this file used separator
            //else: tmpMolecule stays empty and the file is assumed to be no SMILES file -> return null
            findSeparatorLoop:
                while ((tmpSmilesFileNextLine = tmpSmilesFileBufferedReader.readLine()) != null && tmpFilesLine <= 3) {
                    for (String tmpSeparator : tmpSeparators) {
                        tmpProcessedLine = tmpSmilesFileNextLine.split(tmpSeparator, 3);
                        if (tmpProcessedLine.length > 2) {
                            continue;
                        }
                        int tmpIndex = 0;
                        for (String tmpNextLineElement : tmpProcessedLine) {
                            if (tmpNextLineElement.isEmpty()) {     //TODO: does this safe runtime? Or is it unnecessary?
                                continue;
                            }
                            try {
                                tmpMolecule = tmpSmilesParser.parseSmiles(tmpNextLineElement);
                                if (!tmpMolecule.isEmpty()) {   //TODO: probably unnecessary (should be avoided by the if statement)
                                    tmpSmilesFileParsableLinesCounter++;
                                    tmpInTheFileUsedSeparator = tmpSeparator;
                                    tmpExpectedSmilesCodePosition = tmpIndex;
                                    if (tmpProcessedLine.length > 1) {
                                        if (tmpExpectedSmilesCodePosition == 0) {
                                            tmpExpectedIDPosition = 1;
                                        } else {
                                            tmpExpectedIDPosition = 0;
                                        }
                                    }
                                    break findSeparatorLoop;
                                }
                            } catch (InvalidSmilesException anException) {
                                tmpIndex++;
                            }
                        }
                    }
                    tmpSmilesFileInvalidLinesCounter++;
                    tmpFilesLine++;
            }
            if (tmpMolecule.isEmpty()) {
                Importer.LOGGER.log(Level.SEVERE, "Given file is no SMILES file or couldn't be read as one.");    //TODO: should this String be written in Message (resources)?
                GuiUtil.GuiMessageAlert(Alert.AlertType.ERROR,
                        Message.get("Error.Notification.Title"),
                        null,
                        Message.get("Importer.ImportSMILESFile.UnsupportedFileFormat"));
                return null;    //TODO: give feedback to the user!!
            }
            //from this point on: line by line
            while (true) {
                //get the molecules ID
                if (tmpProcessedLine.length >= 2) {
                    if (!tmpMolecule.isEmpty() && !tmpProcessedLine[tmpExpectedIDPosition].isEmpty()) {
                        tmpMolecule.setProperty("NAME", tmpProcessedLine[tmpExpectedIDPosition]);   //TODO: do I need to avoid the same NAME for two AtomContainerSets?
                    }
                }
                //finally add tmpMolecule to the AtomContainerSet
                tmpAtomContainerSet.addAtomContainer(tmpMolecule);
                //get the files next line
                if ((tmpSmilesFileNextLine = tmpSmilesFileBufferedReader.readLine()) == null) {
                    if (!tmpSmilesFileFirstLine.isEmpty() && !tmpSmilesFileFirstLine.contains("ID")) {
                        tmpSmilesFileNextLine = tmpSmilesFileFirstLine;
                        tmpSmilesFileFirstLine = "";
                    } else {
                        //end of file
                        break;
                    }
                }
                //try to parse
                try {
                    tmpProcessedLine = tmpSmilesFileNextLine.split(tmpInTheFileUsedSeparator, 2);
                    //parse Smiles
                    if (!tmpProcessedLine[tmpExpectedSmilesCodePosition].isEmpty()) {
                        tmpMolecule = tmpSmilesParser.parseSmiles(tmpProcessedLine[tmpExpectedSmilesCodePosition]);
                        tmpSmilesFileParsableLinesCounter++;
                        if (tmpSmilesFileParsableLinesCounter % 5000 == 0) {    //TODO: Method runtime lowers extremely at around 230,000 parsed lines. After some time and around 240,000 parsed lines an OutOfMemoryException is thrown
                            System.out.println("Lines have been parsed:\t" + tmpSmilesFileParsableLinesCounter);
                        }
                    } else {
                        tmpSmilesFileInvalidLinesCounter++;
                    }
                } catch (InvalidSmilesException | IndexOutOfBoundsException anException) {
                    tmpSmilesFileInvalidLinesCounter++;
                }
            }
            Importer.LOGGER.log(INFO, "\tSmilesFile ParsableLinesCounter:\t" + tmpSmilesFileParsableLinesCounter + "\n" +
                    "\t\tSmilesFile InvalidLinesCounter:\t\t" + tmpSmilesFileInvalidLinesCounter);
            return tmpAtomContainerSet;
        } catch (IOException anException) {
            //IOException gets thrown when an error occurred while reading the files next line
            Importer.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return null;
        }
    }
    //
    /**
     * Searches the properties of the given atom container for a property
     * containing either 'name' or 'ID' and
     * returns the corresponding value as a string.
     * If nothing is found or value equals 'None', null is returned.
     * @param anAtomContainer IAtomContainer
     * @return String or null
     */
    private String findMoleculeName(IAtomContainer anAtomContainer){
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
    //</editor-fold>
}
