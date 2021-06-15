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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Model class for fragment data
 */
public class FragmentDataModel implements IMolecularDataModel {

    //<editor-fold desc="private class variables" defaultstate="collapsed">
    private String uniqueSmiles;
    private int absoluteFrequency;
    private double absolutePercentage;
    private int moleculeFrequency;
    private double moleculePercentage;
    //TODO: omit?
    private String algorithmName;
    private Map<Object, Object> properties;
    //</editor-fold>
    //
    /**
     * Constructor, sets absolute frequency to 1. From the atom container, only the properties map is retained, and
     * the molecular information is taken from the given unique SMILES code.
     *
     * @param aUniqueSmiles - unique SMILES code
     * @param anAtomContainer - IAtomContainer
     */
    public FragmentDataModel(String aUniqueSmiles, IAtomContainer anAtomContainer){
        this.uniqueSmiles = aUniqueSmiles;
        this.absoluteFrequency = 1;
        //TODO: Set other frequencies to 0?
        this.properties = anAtomContainer.getProperties();
    }
    //
    /**
     * Increases the absolute frequency by one.
     */
    public void incrementAbsoluteFrequency(){
        this.absoluteFrequency += 1;
    }
    //
    /**
     * Increases the molecule frequency by one.
     */
    public void incrementMoleculeFrequency(){
        this.moleculeFrequency += 1;
    }
    //
    //<editor-fold desc="public properties" defaultstate="collapsed">
    /**
     * Returns string unique SMILES
     * @return String uniqueSmiles
     */
    @Override
    public String getUniqueSmiles() {
        return this.uniqueSmiles;
    }
    //
    /**
     * Returns IAtomContainer
     * @return IAtomContainer
     */
    @Override
    public IAtomContainer getAtomContainer() throws CDKException {
        SmilesParser tmpSmiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        tmpSmiPar.kekulise(false);
        IAtomContainer tmpAtomContainer = tmpSmiPar.parseSmiles(this.uniqueSmiles);
        tmpAtomContainer.addProperties(this.properties);
        return tmpAtomContainer;
    }
    //
    /**
     * Returns absolute frequency of this fragment
     * @return int absoluteFrequency
     */
    public int getAbsoluteFrequency() {
        return this.absoluteFrequency;
    }
    //
    /**
     * Returns absolute frequency of this fragment as a percentage
     * @return double absolutePercentage
     */
    public double getAbsolutePercentage() {
        return this.absolutePercentage;
    }
    //
    /**
     * Returns molecule frequency of this fragment
     * @return int moleculeFrequency
     */
    public int getMoleculeFrequency() {
        return this.moleculeFrequency;
    }
    //
    /**
     * Returns molecule frequency of this fragment as a percentage
     * @return double moleculePercentage
     */
    public double getMoleculePercentage() {
        return this.moleculePercentage;
    }
    //
    /**
     * Creates and returns ImageView of this fragment
     * @return ImageView of this fragment
     */
    @Override
    public ImageView getStructure() {
        try {
            IAtomContainer tmpFragmentAtomContainer = this.getAtomContainer();
            return new ImageView(DepictionUtil.depictImage(tmpFragmentAtomContainer));
        } catch (CDKException aCDKException) {
            Logger.getLogger(FragmentDataModel.class.getName()).log(Level.SEVERE, aCDKException.toString(), aCDKException);
            return new ImageView(DepictionUtil.createErrorImage(aCDKException.getMessage(), 250,250));
        }
    }
    //
    /**
     * Returns property map of this fragment
     * @return property map
     */
    @Override
    public Map getProperties() {
        return this.properties;
    }
    //
    /**
     * Sets the absolute frequency
     * @param aValue absolute frequency
     */
    public void setAbsoluteFrequency(int aValue)
    {
        this.absoluteFrequency = aValue;
    }
    //
    /**
     * Sets the molecule frequency
     * @param aValue molecule frequency
     */
    public void setMoleculeFrequency(int aValue)
    {
        this.moleculeFrequency = aValue;
    }
    //
    //TODO: parameter tests
    /**
     * Sets the absolute frequency percentage
     * @param aValue absolute frequency percentage
     */
    public void setAbsolutePercentage(double aValue){
        this.absolutePercentage = aValue;
    }
    //
    /**
     * Sets the molecule frequency percentage
     * @param aValue molecule frequency percentage
     */
    public void setMoleculePercentage(double aValue){
        this.moleculePercentage = aValue;
    }
    //</editor-fold>
}
