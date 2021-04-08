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

import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.model.depict.DepictionUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.ImageView;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.CDK;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Model class for molecule data
 */
public class MoleculeDataModel {

    //<editor-fold desc="private class variables" defaultstate="collapsed">
    private String iD;
    private String name;
    private String smiles;
    private BooleanProperty selection;
    private HashMap<String, Boolean> hasFragmentsMap; // HashMap<FragmentationAlgorithmName, hasFragments>
    private HashMap<String, List<FragmentDataModel>> fragments; // HashMap<FragmentationAlgorithmName, List<Fragments>>
    private HashMap<String, HashMap<String, Integer>> fragmentFrequencies; // HashMap<FragmentationAlgorithmName, Map<uniqueSMILES, frequency in this molecule>>
    //</editor-fold>
    //
    /**
     * Constructor for MoleculeDataModel
     *
     * @param anID - unique identifier
     * @param anAtomContainer - IAtomContainer
     */
    public MoleculeDataModel(String anID, IAtomContainer anAtomContainer, String aUniqueSmiles){
        this.iD = anID;
        this.name = anAtomContainer.getTitle();
        this.smiles = aUniqueSmiles;
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
     * @return String name of moleucle
     */
    public String getName(){
        if(this.name == null || this.name.isEmpty())
            this.name = "NoName";
        return this.name;
    }
    //
    /**
     * Returns IAtomContainer which holds the molecule
     * @return IAtomContainer
     */
    public IAtomContainer getAtomContainer() throws CDKException {
        SmilesParser tmpSmiPa = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        return tmpSmiPa.parseSmiles(this.smiles);
    }
    //
    /**
     * Returns SMILES
     * @return String SMILES
     */
    public String getSmiles(){
        return this.smiles;
    }
    //
    /**
     * Returns boolean whether molecule is selected or not
     * @returns boolean
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
     * Returns a list of {@link de.unijena.cheminf.mortar.model.data.FragmentDataModel} from which this molecule is composed
     * @return List<FragmentDataModel>
     */
    public HashMap<String, List<FragmentDataModel>> getAllFragments(){
        return this.fragments;
    }
    //
    /**
     * Returns a list of unique fragments fragmented by specific algorithm with a given name
     * @param aKey String specifies fragmentation algorithm
     * @return List<FragmentDataModel>
     */
    public List<FragmentDataModel> getFragmentsOfSpecificAlgorithm(String aKey){
        Objects.requireNonNull(aKey, "Key must not be null");
        if(this.fragments.containsKey(aKey)){
            return this.fragments.get(aKey);
        }
        else{
            //ToDO
            return null;
        }
    }
    //
    public HashMap<String, HashMap<String, Integer>> getFragmentFrequencies(){
        return this.fragmentFrequencies;
    }
    //
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
    public HashMap<String, Boolean> getHasFragmentsMap(){
        return this.hasFragmentsMap;
    }
    //
    public Boolean hasMoleculeFragmentsForSpecificAlgorithm(String aKey){
        Objects.requireNonNull(aKey, "aKy must not be null");
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
    public ImageView getStructure() throws CDKException {
        return new ImageView(DepictionUtil.depictImage(this.getAtomContainer()));
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
