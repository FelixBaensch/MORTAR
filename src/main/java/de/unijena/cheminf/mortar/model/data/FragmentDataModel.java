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
import javafx.scene.image.ImageView;
import org.openscience.cdk.interfaces.IAtomContainer;

/**
 * Model class for fragment data
 */
public class FragmentDataModel {

    //<editor-fold desc="private class variables" defaultstate="collapsed">
    private String iD;
    private String smiles;
    private int frequency;
    private double percentage;
    private IAtomContainer atomContainer;
    //</editor-fold>
    //
    /**
     * Constructor
     *
     * @param anId - unique identifier
     * @param anAtomContainer - IAtomContainer
     * @param aFrequency - frequency of this fragment
     * @param aPercentage - percentage amount of this fragment
     */
    public FragmentDataModel(String anId, IAtomContainer anAtomContainer, int aFrequency, double aPercentage){
        this.iD = anId;
        this.atomContainer = anAtomContainer;
        this.frequency = aFrequency;
        this.percentage = aPercentage;
        this.smiles = this.atomContainer.getProperty("SMILES");
    }
    //
    //<editor-fold desc="public properties" defaultstate="collapsed">
    /**
     * Returns string unique identifier
     * @return String ID
     */
    public String getId() {
        return this.iD;
    }
    //
    /**
     * Returns IAtomContainer
     * @return IAtomContainer
     */
    public IAtomContainer getAtomContainer() {
        return this.atomContainer;
    }
    //
    /**
     * Returns SMILES as String
     * @return String SMILES
     */
    public String getSmiles() {
        return this.smiles;
    }
    //
    /**
     * Returns frequency of this framgent
     * @return int frequency
     */
    public int getFrequency() {
        return this.frequency;
    }
    //
    /**
     * Returns percentage frequency of this fragment
     * @return double percentage
     */
    public double getPercentage() {
        return this.percentage;
    }
    //
    /**
     * Creates and returns ImageView of this fragment
     * @return ImageView of this fragment
     */
    public ImageView getStructure(){
        return new ImageView(DepictionUtil.depictImage(this.atomContainer));
    }
    //</editor-fold>
}
