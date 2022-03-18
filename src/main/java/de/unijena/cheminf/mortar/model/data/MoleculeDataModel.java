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

package de.unijena.cheminf.mortar.model.data;

import de.unijena.cheminf.mortar.model.depict.DepictionUtil;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.ChemUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.ImageView;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Kekulization;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Model class for molecule data
 */
public class MoleculeDataModel {
    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * Name of molecule.
     */
    private String name;
    //
    /**
     * Unique SMILES code of molecule.
     */
    private String uniqueSmiles;
    //
    /**
     * Property whether the molecule is selected or not.
     */
    private BooleanProperty selection;
    //
    /**
     * Boolean, whether to keep the atom container of the molecule.
     */
    private boolean keepAtomContainer;
    //
    /**
     * Atom container of the molecule. Only initialized, if the atom container should be kept.
     */
    private IAtomContainer atomContainer;
    //
    /**
     * Fragments map containing names of fragmentations done to the molecule as keys and lists of
     * {@link de.unijena.cheminf.mortar.model.data.FragmentDataModel} objects that resulted from these fragmentations
     * as values.
     */
    private HashMap<String, List<FragmentDataModel>> fragments; // HashMap<FragmentationAlgorithmName, List<Fragments>>
    //
    /**
     * Fragment frequencies map of a specific fragmentation with the given name. Keys of the map are unique SMILES
     * representations of the fragments and values are the frequencies of the respective fragments in the molecule.
     */
    private HashMap<String, HashMap<String, Integer>> fragmentFrequencies; // HashMap<FragmentationAlgorithmName, Map<uniqueSMILES, frequency in this molecule>>
    //
    /**
     * Property map of this molecule.
     */
    private Map<Object, Object> properties;
    //
    /**
     * Height value for image of structure
     */
    private double structureImageHeight;
    //</editor-fold>
    //
    /**
     * Constructor for MoleculeDataModel. Molecular information is taken from the given unique SMILES code. The data
     * is not kept as atom container.
     *
     * @param aUniqueSmiles unique SMILES representation of the molecule
     * @param aName name of the molecule
     * @param aPropertyMap property map of the molecule
     * @throws NullPointerException if given SMILES string is null
     */
    public MoleculeDataModel(String aUniqueSmiles, String aName, Map<Object, Object> aPropertyMap) throws NullPointerException {
        Objects.requireNonNull(aUniqueSmiles, "SMILES is null");
        this.keepAtomContainer = false;
        this.name = aName;
        this.properties = aPropertyMap;
        this.uniqueSmiles = aUniqueSmiles;
        this.selection = new SimpleBooleanProperty(true);
        this.fragments = new HashMap<>(BasicDefinitions.DEFAULT_INITIAL_MAP_CAPACITY);
        this.fragmentFrequencies = new HashMap<>(BasicDefinitions.DEFAULT_INITIAL_MAP_CAPACITY);
    }
    //
    /**
     * Constructor for MoleculeDataModel. Retains the given atom container.
     *
     * @param anAtomContainer AtomContainer of the molecule
     * @throws NullPointerException if given SMILES string is null
     */
    public MoleculeDataModel(IAtomContainer anAtomContainer) throws NullPointerException {
        this(ChemUtil.createUniqueSmiles(anAtomContainer), anAtomContainer.getTitle(), anAtomContainer.getProperties());
        this.keepAtomContainer = true;
        this.atomContainer = anAtomContainer;
    }
    //
    //<editor-fold desc="public properties">
    /**
     * Returns name (String) of the molecule, if it is null, "NoName" will be returned
     * @return String name of molecule
     */
    public String getName(){
        if(this.name == null || this.name.isEmpty())
            this.name = "NoName";
        return this.name;
    }
    //
    /**
     * Returns IAtomContainer which represents the molecule. Depending on the preference, the atom container is saved
     * as class variable. If it is re-created from the SMILES code, bond types and atom types are assigned to it (the
     * former through kekulization). Aromaticity flags are set if there is aromaticity information present in the
     * SMILES code.
     *
     * @return IAtomContainer atom container of the molecule
     * @throws CDKException if SMILES parsing, kekulization, or atom type matching fails
     */
    public IAtomContainer getAtomContainer() throws CDKException {
        SmilesParser tmpSmiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        if(this.keepAtomContainer){
            if(this.atomContainer == null){
                tmpSmiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
                tmpSmiPar.kekulise(false);
                this.atomContainer = tmpSmiPar.parseSmiles(this.uniqueSmiles);
                this.atomContainer.addProperties(this.properties);
                AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(this.atomContainer);
                Kekulization.kekulize(this.atomContainer);
            }
            return this.atomContainer;
        }
        //kekulization done separately below
        tmpSmiPar.kekulise(false);
        IAtomContainer tmpAtomContainer = tmpSmiPar.parseSmiles(this.uniqueSmiles);
        tmpAtomContainer.addProperties(this.properties);
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpAtomContainer);
        Kekulization.kekulize(tmpAtomContainer);
        return tmpAtomContainer;
    }
    //
    /**
     * Returns unique SMILES
     *
     * @return String uniqueSmiles
     */
    public String getUniqueSmiles(){
        return this.uniqueSmiles;
    }
    //
    /**
     * Returns boolean telling whether molecule is selected or not
     * @return true if molecule is selected
     */
    public boolean isSelected(){
        return this.selection.get();
    }
    //
    /**
     * Returns BooleanProperty whether molecule is selected or not
     * @return BooleanProperty
     */
    public BooleanProperty selectionProperty(){
        return selection;
    }
    //
    /**
     * Returns the fragments map of this molecule data model. The map contains as keys names of fragmentations done to
     * the molecule and as values lists of {@link de.unijena.cheminf.mortar.model.data.FragmentDataModel} objects that
     * resulted from these fragmentations.
     *
     * @return HashMap {@literal <}fragmentationName, List {@literal <}FragmentDataModel {@literal >>}
     */
    public HashMap<String, List<FragmentDataModel>> getAllFragments(){
        return this.fragments;
    }
    //
    /**
     * Returns a list of unique fragments that resulted from the fragmentation of the molecule with the given name.
     * @param aKey String specifies fragmentation or fragmentation algorithm
     * @return List {@literal <}FragmentDataModel {@literal >}
     */
    public List<FragmentDataModel> getFragmentsOfSpecificAlgorithm(String aKey){
        Objects.requireNonNull(aKey, "Key must not be null");
        return this.fragments.get(aKey);
        //TODO: Maybe better return an empty list to avoid exceptions
    }
    //
    /**
     * Returns the fragment frequencies map of this molecule data model. The map contains as keys names of fragmentations
     * done to the molecule and as values maps that in turn contain as keys unique SMILES representations of fragments
     * that resulted from the respective fragmentation and as objects the frequencies of the specific fragment in the molecule.
     *
     * @return HashMap {@literal <}fragmentationName, HashMap {@literal <}uniqueSmiles, frequency {@literal >>}
     */
    public HashMap<String, HashMap<String, Integer>> getFragmentFrequencies(){
        return this.fragmentFrequencies;
    }
    //
    /**
     * Returns the fragment frequencies map of a specific fragmentation with the given name. Keys of the map are unique
     * SMILES representations of the fragments and values are the frequencies of the respective fragments in the molecule.
     *
     * @param aKey String specifies fragmentation or fragmentation algorithm
     * @return HashMap {@literal <}uniqueSmiles, frequency {@literal >}
     */
    public HashMap<String, Integer> getFragmentFrequencyOfSpecificAlgorithm(String aKey){
        Objects.requireNonNull(aKey, "Key must not be null");
        return this.fragmentFrequencies.get(aKey);
        //TODO: Maybe better return an empty list to avoid exceptions
    }
    //
    /**
     * Specifies whether the molecule has fragments resulting from the fragmentation process with the given name or not.
     * 
     * @param aKey fragmentation name
     * @return true if the molecule has undergone the fragmentation with the specified name
     */
    public boolean hasMoleculeUndergoneSpecificFragmentation(String aKey){
        Objects.requireNonNull(aKey, "aKey must not be null");
        return this.fragments.containsKey(aKey);
    }
    //
    /**
     * Creates and returns an ImageView of this molecule as 2D structure
     * @return ImageView
     */
    public ImageView getStructure() {
        try {
            IAtomContainer tmpAtomContainer = this.getAtomContainer();
            return new ImageView(DepictionUtil.depictImage(tmpAtomContainer, BasicDefinitions.DEFAULT_IMAGE_WIDTH_DEFAULT, this.structureImageHeight));
        } catch (CDKException aCDKException) {
            Logger.getLogger(MoleculeDataModel.class.getName()).log(Level.SEVERE, aCDKException.toString(), aCDKException);
            return new ImageView(DepictionUtil.depictErrorImage(aCDKException.getMessage(), 250, 250));
        }
    }
    //
    /**
     * Creates and returns an ImageView of this molecule as 2D structure with the given text below the structure
     *
     * @param aText to show below structure
     * @return ImageView with text
     */
    public ImageView getStructureWithText(String aText){
        try {
            IAtomContainer tmpAtomContainer = this.getAtomContainer();
            return new ImageView(DepictionUtil.depictImageWithText(tmpAtomContainer, 1, BasicDefinitions.DEFAULT_IMAGE_WIDTH_DEFAULT, this.structureImageHeight, aText));
        } catch (CDKException aCDKException) {
            Logger.getLogger(MoleculeDataModel.class.getName()).log(Level.SEVERE, aCDKException.toString(), aCDKException);
            return new ImageView(DepictionUtil.depictErrorImage(aCDKException.getMessage(), 250, 250));
        }
    }
    //
    /**
     * Returns property map of this molecule
     * @return property map
     */
    public Map getProperties() {
        return this.properties;
    }
    //
    /**
     * Returns whether the atom container of the molecule should be kept or not
     * @return boolean keepAtomContainer
     */
    public boolean isKeepAtomContainer(){
        return this.keepAtomContainer;
    }
    //
    /**
     * Sets given String as name of this molecule
     * @param aName String
     */
    public void setName(String aName){
        this.name = aName;
    }
    //
    /**
     * Sets selection state of this molecule depending on the specified boolean
     * @param aValue boolean
     */
    public void setSelection(boolean aValue){
        this.selection.set(aValue);
    }
    //
    /**
     * Sets whether the molecule's atom container should be kept. If not, the atom container is set to null
     * @param aValue boolean
     */
    public void setKeepAtomContainer(boolean aValue){
        if(!(this.keepAtomContainer = aValue)){
            this.atomContainer = null;
        }
    }
    //
    /**
     * Returns the height of the image of the molecular structure
     *
     * @return height of image
     */
    public double getStructureImageHeight() {
        return structureImageHeight;
    }
    //
    /**
     * Sets the height of the image of the molecular structure
     *
     * @param aStructureImageHeight double
     */
    public void setStructureImageHeight(double aStructureImageHeight) {
        this.structureImageHeight = aStructureImageHeight;
    }
    //</editor-fold>
}
