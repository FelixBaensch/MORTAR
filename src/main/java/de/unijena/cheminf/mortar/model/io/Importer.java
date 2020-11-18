/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2020  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas-schaub@uni-jena.de)
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
import java.util.HashMap;
import java.util.Objects;
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
    private IAtomContainerSet atomContainerSet;
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
        File tmpFile = this.LoadFile(aParentStage);
        if(tmpFile == null)
            return null;
        String tmpFileExtension = FileUtil.getFileExtension(tmpFile.getPath());
        this.atomContainerSet = new AtomContainerSet();
        switch (tmpFileExtension){
            case ".mol":
                this.ImportMolFile(tmpFile);
                break;
            case ".sdf":
                this.ImportSDFile(tmpFile);
                break;
            case ".pdb":
                this.ImportPDBFile(tmpFile);
                break;
        }
        return this.atomContainerSet;
    }
    //</editor-fold>
    //<editor-fold desc="private methods" defaultstate="collapsed">
    /**
     * Opens a file chooser and loads the chosen file
     * @param aParentStage Stage where FileChooser should be shown
     * @return File which contains molecules
     */
    private File LoadFile(Stage aParentStage){
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
    private void ImportMolFile(File aFile){
        try{
            MDLV2000Reader tmpReader = new MDLV2000Reader(new FileInputStream(aFile), IChemObjectReader.Mode.RELAXED);
            IAtomContainer tmpAtomContainer = tmpReader.read(new AtomContainer());
            //TODO: add a preference depending if clause to add implicit hydrogens or not
            BufferedReader tmpBufferedReader = new BufferedReader(new FileReader(aFile));
            String tmpMolName = tmpBufferedReader.readLine();
            tmpBufferedReader.close();
            tmpAtomContainer.setProperty("NAME", tmpMolName);
            this.atomContainerSet.addAtomContainer(tmpAtomContainer);

        }catch(CDKException | IOException anException){
            Importer.LOGGER.log(Level.SEVERE, anException.toString(), anException);
        }
    }
    //
    /**
     * Imports a SD file
     * @param aFile sdf
     */
    private void ImportSDFile(File aFile){
        try{
            IteratingSDFReader tmpSDFReader = new IteratingSDFReader(new FileInputStream(aFile),
                    DefaultChemObjectBuilder.getInstance());
            while(tmpSDFReader.hasNext()){
                IAtomContainer tmpAtomContainer = tmpSDFReader.next();
                //TODO: add a preference depending if clause to add implicit hydrogens or not
                this.atomContainerSet.addAtomContainer(tmpAtomContainer);
            }
        } catch (FileNotFoundException anException) {
            Importer.LOGGER.log(Level.SEVERE, anException.toString(), anException);
        }
    }
    //
    /**
     * Imports a PDB file
     * @param aFile PDB file
     */
    private void ImportPDBFile(File aFile){
        try{
            PDBReader tmpPDBReader = new PDBReader(new FileInputStream(aFile));
            IAtomContainer tmpAtomContainer = tmpPDBReader.read(new AtomContainer());
        } catch (FileNotFoundException | CDKException anException) {
            Importer.LOGGER.log(Level.SEVERE, anException.toString(), anException);
        }
    }
    //</editor-fold>
}
