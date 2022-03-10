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
import javafx.scene.image.ImageView;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Model class for fragment data
 */
public class FragmentDataModel extends MoleculeDataModel {
    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * Absolute frequency of the fragment.
     */
    private int absoluteFrequency;
    //
    /**
     * Absolute frequency of the fragment as a percentage.
     */
    private double absolutePercentage;
    //
    /**
     * Molecule frequency of the fragment.
     */
    private int moleculeFrequency;
    //
    /**
     * Molecule frequency of the fragment as a percentage.
     */
    private double moleculePercentage;
    //
    /**
     * List of parent molecules, which contains this fragment
     */
    private List<MoleculeDataModel> parentMolecules;
    //
    /**
     * First or random parent molecule which contains this fragment
     */
    private MoleculeDataModel parentMolecule;
    //</editor-fold>
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(FragmentDataModel.class.getName());
    //
    /**
     * Constructor, sets absolute frequency to 1. Molecular information is taken from the given unique SMILES code. The
     * data is not kept as atom container.
     *
     * @param aUniqueSmiles unique SMILES code
     * @param aName name of the molecule
     * @param aPropertyMap property map of the molecule
     * @throws NullPointerException if given SMILES string is null
     */
    public FragmentDataModel(String aUniqueSmiles, String aName, Map<Object, Object> aPropertyMap) throws NullPointerException {
        super(aUniqueSmiles, aName, aPropertyMap);
        this.absoluteFrequency = 0;
        this.absolutePercentage = 0.;
        this.moleculeFrequency = 0;
        this.moleculePercentage = 0.;
        this.parentMolecules = new LinkedList<>();
    }
    //
    /**
     * Constructor, sets absolute frequency to 1. Retains the given data as atom container.
     *
     * @param anAtomContainer AtomContainer of the molecule
     * @throws NullPointerException if given SMILES string is null
     */
    public FragmentDataModel(IAtomContainer anAtomContainer) throws NullPointerException {
        super(anAtomContainer);
        this.absoluteFrequency = 0;
        this.absolutePercentage = 0.;
        this.moleculeFrequency = 0;
        this.moleculePercentage = 0.;
        this.parentMolecules = new LinkedList<>();
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
     * Returns list of parent molecules which contains this fragment
     * @return list of parent molecules
     */
    public List<MoleculeDataModel> getParentMolecules(){
        return this.parentMolecules;
    }
    //
    /**
     * Creates and returns an ImageView of first parent molecule as 2D structure of this fragment
     *
     * NOTE: Do not delete or comment this method, it is used by reflection
     * @return ImageView
     */
    public ImageView getParentMoleculeStructure() {
        if(this.parentMolecules.size() < 1){
            return null;
        }
        if(this.parentMolecule == null){
            this.parentMolecule = this.parentMolecules.get(0);
        }
        try {
            IAtomContainer tmpAtomContainer = this.parentMolecule.getAtomContainer();
            return new ImageView(DepictionUtil.depictImageWithHeight(tmpAtomContainer, super.getStructureImageHeight()));
        } catch (CDKException aCDKException) {
            FragmentDataModel.LOGGER.getLogger(MoleculeDataModel.class.getName()).log(Level.SEVERE, aCDKException.toString() + "_" + this.parentMolecule.getName(), aCDKException);
            return new ImageView(DepictionUtil.depictErrorImage(aCDKException.getMessage(), 250, 250));
        }
    }
    //
    /**
     * Returns the name of first parent molecule of this fragment
     *
     * @return String
     */
    public String getParentMoleculeName(){
        if(this.parentMolecules.size() < 1){
            return null;
        }
        if(this.parentMolecule == null){
           this.parentMolecule = this.parentMolecules.get(0);
        }
        return this.parentMolecule.getName();
    }
    //
    /**
     * Sets the absolute frequency
     *
     * @param aValue absolute frequency
     */
    public void setAbsoluteFrequency(int aValue) {
        if(aValue < 0 ){
            throw new IllegalArgumentException("aValue must be positive or zero.");
        }
        this.absoluteFrequency = aValue;
    }
    //
    /**
     * Sets the molecule frequency
     *
     * @param aValue molecule frequency
     */
    public void setMoleculeFrequency(int aValue) {
        if(aValue < 0 ){
            throw new IllegalArgumentException("aValue must be positive or zero.");
        }
        this.moleculeFrequency = aValue;
    }
    //
    /**
     * Sets the absolute frequency percentage
     *
     * @param aValue absolute frequency percentage
     */
    public void setAbsolutePercentage(double aValue){
        if(aValue < 0.0 ){
            throw new IllegalArgumentException("aValue must be positive or zero.");
        }
        if(!Double.isFinite(aValue)){
            throw new IllegalArgumentException("aValue must be finite.");
        }
        this.absolutePercentage = aValue;
    }
    //
    /**
     * Sets the molecule frequency percentage
     *
     * @param aValue molecule frequency percentage
     */
    public void setMoleculePercentage(double aValue){
        if(aValue < 0.0 ){
            throw new IllegalArgumentException("aValue must be positive or zero.");
        }
        if(!Double.isFinite(aValue)){
            throw new IllegalArgumentException("aValue must be finite.");
        }
        this.moleculePercentage = aValue;
    }
    //
    /**
     * Sets the parent molecule of this fragment
     *
     * @param aParentMolecule parent molecule
     */
    public void setParentMolecule(MoleculeDataModel aParentMolecule){
        Objects.requireNonNull(aParentMolecule,"aParentMolecule must not be null.");
        this.parentMolecule = aParentMolecule;
    }
    //</editor-fold>
}
