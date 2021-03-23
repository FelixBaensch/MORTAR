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
    private String uniqueSmiles;
    private int absoluteFrequency;
    private double absolutePercentage;
    private int moleculeFrequency;
    private double moleculePercentage;
    private IAtomContainer atomContainer;
    private String algorithmName;
    //</editor-fold>
    //
    /**
     * Constructor
     *
     * @param anUniqueSmiles - unique SMILES
     * @param anAtomContainer - IAtomContainer
     */
    public FragmentDataModel(String anUniqueSmiles, IAtomContainer anAtomContainer){
        this.uniqueSmiles = anUniqueSmiles;
        this.atomContainer = anAtomContainer;
    }
    //
    //<editor-fold desc="public properties" defaultstate="collapsed">
    /**
     * Returns string unique SMILES
     * @return String uniqueSmiles
     */
    public String getUniqueSmiles() {
        return this.uniqueSmiles;
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
     * Returns frequency of this framgent
     * @return int frequency
     */
    public int getAbsoluteFrequency() {
        return this.absoluteFrequency;
    }
    //
    /**
     * Returns percentage frequency of this fragment
     * @return double percentage
     */
    public double getAbsolutePercentage() {
        return this.absolutePercentage;
    }
    //
    /**
     * Returns frequency of this framgent
     * @return int frequency
     */
    public int getMoleculeFrequency() {
        return this.moleculeFrequency;
    }
    //
    /**
     * Returns percentage frequency of this fragment
     * @return double percentage
     */
    public double getMoleculePercentage() {
        return this.moleculePercentage;
    }
    //
    /**
     * Creates and returns ImageView of this fragment
     * @return ImageView of this fragment
     */
    public ImageView getStructure(){
        return new ImageView(DepictionUtil.depictImage(this.atomContainer));
    }
    //
    /**
     *
     * @param aValue
     */
    public void setAbsoluteFrequency(int aValue)
    {
        this.absoluteFrequency = aValue;
    }
    /**
     *
     * @param aValue
     */
    public void setMoleculeFrequency(int aValue)
    {
        this.moleculeFrequency = aValue;
    }
    //</editor-fold>
}
