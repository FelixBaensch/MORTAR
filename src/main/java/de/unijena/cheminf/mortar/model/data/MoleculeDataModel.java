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
import org.openscience.cdk.interfaces.IAtomContainerSet;

public class MoleculeDataModel {

    private String iD;
    private String name;
    private String smiles;
    private IAtomContainer atomContainer;
    private BooleanProperty selection;
    private IAtomContainerSet fragments;

    public MoleculeDataModel(String anID, IAtomContainer anAtomContainer){
        this.iD = anID;
        this.atomContainer = anAtomContainer;
        this.name = this.atomContainer.getTitle();
        this.smiles = this.atomContainer.getProperty("SMILES");
        this.selection = new SimpleBooleanProperty(true);
    }

    public String getId(){
        return this.iD;
    }
    public String getName(){
        if(this.name == null || this.name.isEmpty())
            this.name = "NoName";
        return this.name;
    }
    public IAtomContainer getAtomContainer(){
        return this.atomContainer;
    }
    public String getSmiles(){
        return this.smiles;
    }
    public boolean isSelected(){
        return this.selection.get();
    }
    public BooleanProperty selectionProperty(){
        return selection;
    }
    public IAtomContainerSet getFragments(){
        return this.fragments;
    }
    public ImageView getStructure(){
        return new ImageView(DepictionUtil.depictImage(this.atomContainer));
    }

    public void setName(String aName){
        this.name = aName;
    }
    public void setSelection(boolean aValue){
        this.selection.set(aValue);
    }

}
