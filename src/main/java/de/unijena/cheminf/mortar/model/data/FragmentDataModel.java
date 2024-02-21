/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2024  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
 *
 * Source code is available at <https://github.com/FelixBaensch/MORTAR>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.unijena.cheminf.mortar.model.data;

import de.unijena.cheminf.mortar.model.depict.DepictionUtil;

import javafx.scene.image.ImageView;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Model class for fragment data.
 *
 * @author Felix Baensch, Samuel Behr
 * @version 1.0.0.0
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
    private Set<MoleculeDataModel> parentMolecules;
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
     * Constructor, sets absolute frequency to 0. Molecular information is taken from the given unique SMILES code. The
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
        this.parentMolecules = ConcurrentHashMap.newKeySet(); // sounds weird but to set the number of the total molecule set kills the performance
    }
    //
    /**
     * Constructor, sets absolute frequency to 0. Retains the given data as atom container.
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
        this.parentMolecules = ConcurrentHashMap.newKeySet(); // sounds weird but to set the number of the total molecule set kills the performance
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
    public Set<MoleculeDataModel> getParentMolecules(){
        return this.parentMolecules;
    }
    //
    /**
     * Returns the MoleculeDataModel that occurs at first pos in the list of parent molecules
     *
     * @return MoleculeDataModel first molecule of list of parent molecules
     */
    public MoleculeDataModel getFirstParentMolecule() {
        if(this.parentMolecules.size() < 1){
            return null;
        }
        if(this.parentMolecule == null){
            this.parentMolecule = this.parentMolecules.stream().findFirst().get();
        }
        return this.parentMolecule;
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
            this.parentMolecule = this.parentMolecules.stream().findFirst().get();
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
           this.parentMolecule = this.parentMolecules.stream().findFirst().get();
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
