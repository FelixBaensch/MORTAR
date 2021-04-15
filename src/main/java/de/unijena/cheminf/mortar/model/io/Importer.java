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
import de.unijena.cheminf.mortar.model.util.FileUtil;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.io.IChemObjectReader;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.io.PDBReader;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import java.io.*;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        tmpFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Molecules", "*.mol", "*.sdf", "*.pdb"));
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
    //TODO: import smiles file
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
