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

package de.unijena.cheminf.mortar.model.data;

import de.unijena.cheminf.mortar.model.depict.DepictionUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.ImageView;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesParser;

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
    //TODO: Omit? Is not the unique SMILES the id for us?
    private String iD;
    private String name;
    private String uniqueSmiles;
    private BooleanProperty selection;
    private HashMap<String, Boolean> hasFragmentsMap; // HashMap<FragmentationAlgorithmName, hasFragments>
    private HashMap<String, List<FragmentDataModel>> fragments; // HashMap<FragmentationAlgorithmName, List<Fragments>>
    private HashMap<String, HashMap<String, Integer>> fragmentFrequencies; // HashMap<FragmentationAlgorithmName, Map<uniqueSMILES, frequency in this molecule>>
    private Map<Object, Object> properties;
    //</editor-fold>
    //
    /**
     * Constructor for MoleculeDataModel. From the atom container, only the properties map is retained, and
     * the molecular information is taken from the given unique SMILES code.
     *
     * @param anID - unique identifier
     * @param anAtomContainer - IAtomContainer
     * @param aUniqueSmiles - unique SMILES representation of the molecule
     */
    public MoleculeDataModel(String aUniqueSmiles, IAtomContainer anAtomContainer, String anID){
        this.iD = anID;
        this.name = anAtomContainer.getTitle();
        this.properties = anAtomContainer.getProperties();
        this.uniqueSmiles = aUniqueSmiles;
        this.selection = new SimpleBooleanProperty(true);
        this.fragments = new HashMap<>(5);
        this.fragmentFrequencies = new HashMap<>(5);
        this.hasFragmentsMap = new HashMap<>(5); //TODO: magic number go to definitions
    }
    //
    //<editor-fold desc="public properties">
    /**
     * Returns unique identifier as String
     * @return String unique identifier
     */
    public String getId(){
        return this.iD;
    }
    //
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
     * Returns IAtomContainer which represents the molecule
     * @return IAtomContainer
     */
    public IAtomContainer getAtomContainer() throws CDKException {
        SmilesParser tmpSmiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        //TODO: Necessary here? For fragments definitely necessary, but here also?
        tmpSmiPar.kekulise(false);
        IAtomContainer tmpAtomContainer = tmpSmiPar.parseSmiles(this.uniqueSmiles);
        tmpAtomContainer.addProperties(this.properties);
        return tmpAtomContainer;
    }
    //
    /**
     * Returns unique SMILES
     * @return String uniqueSmiles
     */
    public String getUniqueSmiles(){
        return this.uniqueSmiles;
    }
    //
    //TODO: should this not be getSelection(), following the properties naming convention?
    /**
     * Returns boolean telling whether molecule is selected or not
     * @returns true if molecule is selected
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
     * @return HashMap<fragmentationName, List<FragmentDataModel>>
     */
    public HashMap<String, List<FragmentDataModel>> getAllFragments(){
        return this.fragments;
    }
    //
    /**
     * Returns a list of unique fragments that resulted from the fragmentation of the molecule with the given name.
     * @param aKey String specifies fragmentation or fragmentation algorithm
     * @return List<FragmentDataModel>
     */
    public List<FragmentDataModel> getFragmentsOfSpecificAlgorithm(String aKey){
        Objects.requireNonNull(aKey, "Key must not be null");
        if(this.fragments.containsKey(aKey)){
            return this.fragments.get(aKey);
        }
        else{
            //TODO
            return null;
        }
    }
    //
    /**
     * Returns the fragment frequencies map of this molecule data model. The map contains as keys names of fragmentations
     * done to the molecule and as values maps that in turn contain as keys unique SMILES representations of fragments
     * that resulted from the respective fragmentation and as objects the frequencies of the specific fragment in the molecule.
     * @return HashMap<fragmentationName, HashMap<uniqueSmiles, frequency>>
     */
    public HashMap<String, HashMap<String, Integer>> getFragmentFrequencies(){
        return this.fragmentFrequencies;
    }
    //
    /**
     * Returns the fragment frequencies map of a specific fragmentation with the given name. Keys of the map are unique
     * SMILES representations of the fragments and values are the frequencies of the respective fragments in the molecule.
     * @param aKey String specifies fragmentation or fragmentation algorithm
     * @return HashMap<uniqueSmiles, frequency>
     */
    public HashMap<String, Integer> getFragmentFrequencyOfSpecificAlgorithm(String aKey){
        Objects.requireNonNull(aKey, "Key must not be null");
        if(this.fragmentFrequencies.containsKey(aKey)){
            return this.fragmentFrequencies.get(aKey);
        }
        else{
            //TODO
            return null;
        }
    }
    //
    //TODO: Is the map written to in the fragmentation tasks?
    /**
     * Returns a map that contains fragmentation names as keys and boolean values that specify whether the molecule has
     * fragments resulting from the respective fragmentation process or not.
     * @return HashMap<fragmentationName, hasFragments>
     */
    public HashMap<String, Boolean> getHasFragmentsMap(){
        return this.hasFragmentsMap;
    }
    //
    /**
     * Specifies whether the molecule has fragments resulting from the fragmentation process with the given name or not.
     * @param aKey fragmentation name
     * @return true if the molecule has fragments resulting from the specified fragmentation 
     */
    public Boolean hasMoleculeFragmentsForSpecificAlgorithm(String aKey){
        Objects.requireNonNull(aKey, "aKey must not be null");
        if(this.hasFragmentsMap.containsKey(aKey)){
            return this.hasFragmentsMap.get(aKey);
        }
        else {
            //TODO
            return null;
        }
    }
    //
    /**
     * Creates and returns an ImageView of this molecule as 2D structure
     * @return ImageView
     */
    public ImageView getStructure() {
        try {
            IAtomContainer tmpAtomContainer = this.getAtomContainer();
            return new ImageView(DepictionUtil.depictImage(tmpAtomContainer));
        } catch (CDKException aCDKException) {
            Logger.getLogger(MoleculeDataModel.class.getName()).log(Level.SEVERE, aCDKException.toString(), aCDKException);
            return new ImageView(DepictionUtil.createErrorImage(aCDKException.getMessage(), 250, 250));
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
    //</editor-fold>
}
