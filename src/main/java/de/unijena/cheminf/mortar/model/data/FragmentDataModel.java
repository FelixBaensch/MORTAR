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
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesParser;

import java.util.Map;

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
    private String algorithmName;
    private Map<Object, Object> properties;
    //</editor-fold>
    //
    /**
     * Constructor, sets absolute frequency to 1
     *
     * @param anUniqueSmiles - unique SMILES
     * @param anAtomContainer - IAtomContainer
     */
    public FragmentDataModel(String anUniqueSmiles, IAtomContainer anAtomContainer){
        this.uniqueSmiles = anUniqueSmiles;
        this.absoluteFrequency = 1;
        this.properties = anAtomContainer.getProperties();
    }
    //
    public void incrementAbsoluteFrequency(){
        this.absoluteFrequency += 1;
    }
    //
    public void incrementMoleculeFrequency(){
        this.moleculeFrequency += 1;
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
    public IAtomContainer getAtomContainer() throws CDKException {
        SmilesParser tmpSmiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        IAtomContainer tmpAtomContainer = tmpSmiPar.parseSmiles(this.uniqueSmiles);
        tmpAtomContainer.addProperties(this.properties);
        return tmpAtomContainer;
    }
    //
    /**
     * Returns frequency of this fragment
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
     * Returns frequency of this fragment
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
    public ImageView getStructure() throws CDKException{
        return new ImageView(DepictionUtil.depictImage(this.getAtomContainer()));
    }
    //
    /**
     * Returns property map of this fragment
     * @return property map
     */
    public Map getProperties() {
        return this.properties;
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
    //
    /**
     *
     * @param aValue
     */
    public void setAbsolutePercentage(double aValue){
        this.absolutePercentage = aValue;
    }
    //
    /**
     *
     * @param aValue
     */
    public void setMoleculePercentage(double aValue){
        this.moleculePercentage = aValue;
    }
    //</editor-fold>
}
