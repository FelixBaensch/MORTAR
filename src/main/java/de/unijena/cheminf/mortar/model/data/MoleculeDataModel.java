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
import org.openscience.cdk.interfaces.IAtomContainer;
import java.util.ArrayList;
import java.util.List;

/**
 * Model class for molecule data
 */
public class MoleculeDataModel {

    //<editor-fold desc="private class variables" defaultstate="collapsed">
    private String iD;
    private String name;
    private String smiles;
    private IAtomContainer atomContainer;
    private BooleanProperty selection;
    private List<FragmentDataModel> fragments;
    //</editor-fold>
    //
    /**
     * Constructor for MoleculeDataModel
     *
     * @param anID - unique identifier
     * @param anAtomContainer - IAtomContainer
     */
    public MoleculeDataModel(String anID, IAtomContainer anAtomContainer){
        this.iD = anID;
        this.atomContainer = anAtomContainer;
        this.name = this.atomContainer.getTitle();
        this.smiles = this.atomContainer.getProperty("SMILES");
        this.selection = new SimpleBooleanProperty(true);
        this.fragments = new ArrayList<>();
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
    public IAtomContainer getAtomContainer(){
        return this.atomContainer;
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
    public List<FragmentDataModel> getFragments(){
        return this.fragments;
    }
    //
    /**
     * Creates and returns an ImageView of this molecule as 2D structure
     * @return ImageView
     */
    public ImageView getStructure(){
        return new ImageView(DepictionUtil.depictImage(this.atomContainer));
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
