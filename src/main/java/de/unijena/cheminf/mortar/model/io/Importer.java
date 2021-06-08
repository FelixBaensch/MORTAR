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

import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.io.*;
import org.openscience.cdk.io.formats.IChemFormat;
import org.openscience.cdk.io.formats.MDLV2000Format;
import org.openscience.cdk.io.formats.MDLV3000Format;
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
    public IAtomContainerSet Import(Stage aParentStage) throws CDKException, IOException {
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
     * @throws CDKException
     * @throws IOException
     */
    private IAtomContainerSet ImportMolFile(File aFile) throws CDKException, IOException {
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
            throw new CDKException("The mol file does not correspond to either the MDLV2000 or the MDLV3000 format and therefore cannot be imported.");
        }
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
        tmpInputStream.close();
        return tmpAtomContainerSet;
    }
    //
    /**
     * Imports a SD file
     * @param aFile sdf
     * @throws FileNotFoundException
     */
    private IAtomContainerSet ImportSDFile(File aFile) throws FileNotFoundException {
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
    }
    //
    /**
     * Imports a PDB file
     * @param aFile PDB file
     * @throws CDKException
     * @throws FileNotFoundException
     */
    private IAtomContainerSet ImportPDBFile(File aFile) throws CDKException, FileNotFoundException {
        IAtomContainerSet tmpAtomContainerSet = new AtomContainerSet();
        PDBReader tmpPDBReader = new PDBReader(new FileInputStream(aFile));
        IAtomContainer tmpAtomContainer = tmpPDBReader.read(new AtomContainer());
        String tmpName = this.findMoleculeName(tmpAtomContainer);
        if(tmpName == null || tmpName.isBlank() || tmpName.isEmpty())
            tmpName = FileUtil.getFileNameWithoutExtension(aFile);
        tmpAtomContainer.setProperty("NAME", tmpName);
        tmpAtomContainerSet.addAtomContainer(tmpAtomContainer);
        return  tmpAtomContainerSet;
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
    //<editor-fold desc="protected methods" defaultstate="collapsed">
    /**
     * Imports a SMILES file.   TODO: specify ImportSMILESFile doc String
     * @param aFile a SMILES codes containing *.txt or *.smi file
     * @throws IOException if the given file does not fit to the expected format of a SMILES file
     * @author Samuel Behr
     */
    protected IAtomContainerSet ImportSMILESFile(File aFile) throws IOException {
        try (
                FileReader tmpSmilesFileReader = new FileReader(aFile);
                BufferedReader tmpSmilesFileBufferedReader = new BufferedReader(tmpSmilesFileReader, BasicDefinitions.BUFFER_SIZE)
        ) {
            IAtomContainerSet tmpAtomContainerSet = new AtomContainerSet();
            IAtomContainer tmpMolecule = new AtomContainer();   //AtomContainer to safe the parsed SMILES in
            SmilesParser tmpSmilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
            String tmpSmilesFileNextLine = "";
            String tmpSmilesFileDeterminedSeparator = "";
            String[] tmpProcessedLineArray;
            int tmpSmilesCodeExpectedPosition = 0;
            int tmpIDExpectedPosition = 0;
            int tmpSmilesFileParsableLinesCounter = 0;
            int tmpSmilesFileInvalidLinesCounter = 0;
            tmpSmilesFileBufferedReader.mark(BasicDefinitions.BUFFER_SIZE);   //marking the BufferedReader to reset the reader after checking the format and determining the separator
            String tmpSmilesFileFirstLine = tmpSmilesFileBufferedReader.readLine();     //as potential headline the first line should be avoided for separator determination
            /*  first block
            Checking for parsable SMILES code and saving the determined separator (if one is used).
            If no parsable SMILES code is found in the second and third line of the file, tmpMolecule stays empty and the file is assumed to be no SMILES file -> return null
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
                    tmpProcessedLineArray = tmpSmilesFileNextLine.split(tmpSeparator, 3);   //maximum of two array elements expected, otherwise the separator or the line itself are assumed to be invalid
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
            //resetting the BufferedReader to the file's first line
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
                //setting the AtomContainer's "NAME"
                String tmpName = "";
                if (tmpProcessedLineArray.length > 1 && !tmpProcessedLineArray[tmpIDExpectedPosition].isEmpty()) {
                    tmpName = tmpProcessedLineArray[tmpIDExpectedPosition];
                } else {
                    tmpName = FileUtil.getFileNameWithoutExtension(aFile) + tmpSmilesFileParsableLinesCounter;
                }
                tmpMolecule.setProperty("NAME", tmpName);
                //adding tmpMolecule to the AtomContainerSet
                tmpAtomContainerSet.addAtomContainer(tmpMolecule);
            }
            Importer.LOGGER.log(INFO, "\tSmilesFile ParsableLinesCounter:\t" + tmpSmilesFileParsableLinesCounter +
                    "\n\tSmilesFile InvalidLinesCounter:\t\t" + tmpSmilesFileInvalidLinesCounter);
            return tmpAtomContainerSet;
        }
    }
    //</editor-fold>
}
