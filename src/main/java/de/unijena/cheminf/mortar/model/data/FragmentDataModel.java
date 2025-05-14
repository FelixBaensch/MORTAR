/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2025  Felix Baensch, Jonas Schaub (felix.j.baensch@gmail.com, jonas.schaub@uni-jena.de)
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
import java.util.concurrent.atomic.AtomicInteger;
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
    private AtomicInteger absoluteFrequency;
    //
    /**
     * Absolute frequency of the fragment as a percentage.
     */
    private double absolutePercentage;
    //
    /**
     * Molecule frequency of the fragment.
     */
    private AtomicInteger moleculeFrequency;
    //
    /**
     * Molecule frequency of the fragment as a percentage.
     */
    private double moleculePercentage;
    //
    /**
     * List of parent molecules, which contain this fragment.
     */
    private final Set<MoleculeDataModel> parentMolecules;
    //
    /**
     * First or random parent molecule which contains this fragment.
     */
    private MoleculeDataModel parentMolecule;
    //</editor-fold>
    //
    //<editor-fold desc="private static final class constants">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(FragmentDataModel.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="constructors">
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
        this.absoluteFrequency = new AtomicInteger(0);
        this.absolutePercentage = 0.;
        this.moleculeFrequency = new AtomicInteger(0);
        this.moleculePercentage = 0.;
        // sounds weird but to set the number of the total molecule set as expected size kills the performance
        this.parentMolecules = ConcurrentHashMap.newKeySet();
    }
    //
    /**
     * Constructor, sets absolute frequency to 0. Retains the given data as atom container.
     *
     * @param anAtomContainer AtomContainer of the molecule
     * @param isStereoChemEncoded whether stereochemistry should be retained in the unique SMILES code encoding the structure
     * @throws NullPointerException if given SMILES string is null
     */
    public FragmentDataModel(IAtomContainer anAtomContainer, boolean isStereoChemEncoded) throws NullPointerException {
        super(anAtomContainer, isStereoChemEncoded);
        this.absoluteFrequency = new AtomicInteger(0);
        this.absolutePercentage = 0.;
        this.moleculeFrequency = new AtomicInteger(0);
        this.moleculePercentage = 0.;
        // sounds weird but to set the number of the total molecule set as expected size kills the performance
        this.parentMolecules = ConcurrentHashMap.newKeySet();
    }
    //</editor-fold>
    //
    //<editor-fold desc="public methods">
    /**
     * Increases the absolute frequency by one and returns it.
     * This operation is atomic because AtomicInteger is used internally.
     *
     * @return incremented absolute frequency
     */
    public final int incrementAbsoluteFrequency() {
        return this.absoluteFrequency.incrementAndGet();
    }
    //
    /**
     * Increases the molecule frequency by one and returns it.
     * This operation is atomic because AtomicInteger is used internally.
     *
     * @return incremented molecule frequency
     */
    public final int incrementMoleculeFrequency() {
        return this.moleculeFrequency.incrementAndGet();
    }
    //</editor-fold>
    //
    //<editor-fold desc="public properties get" defaultstate="collapsed">
    /**
     * Returns absolute frequency of this fragment.
     * This operation is atomic because AtomicInteger is used internally.
     * <br>NOTE: Do not delete or rename this method, it is used by reflection (in FragmentsDataTableView, the
     * CellValueFactory of the frequency column is set to a PropertyValueFactory that uses "absoluteFrequency" as
     * property string to invoke this method; see also DataModelPropertiesForTableView enum).
     *
     * @return int absoluteFrequency
     */
    public int getAbsoluteFrequency() {
        return this.absoluteFrequency.get();
    }
    //
    /**
     * Returns absolute frequency of this fragment as a percentage.
     * <br>NOTE: Do not delete or rename this method, it is used by reflection (in FragmentsDataTableView, the
     * CellValueFactory of the percentage column is set to a PropertyValueFactory that uses "absolutePercentage" as
     * property string to invoke this method; see also DataModelPropertiesForTableView enum).
     *
     * @return double absolutePercentage
     */
    public double getAbsolutePercentage() {
        return this.absolutePercentage;
    }
    //
    /**
     * Returns molecule frequency of this fragment.
     * This operation is atomic because AtomicInteger is used internally.
     * <br>NOTE: Do not delete or rename this method, it is used by reflection (in FragmentsDataTableView, the
     * CellValueFactory of the molecule frequency column is set to a PropertyValueFactory that uses "moleculeFrequency" as
     * property string to invoke this method; see also DataModelPropertiesForTableView enum).
     *
     * @return int moleculeFrequency
     */
    public int getMoleculeFrequency() {
        return this.moleculeFrequency.get();
    }
    //
    /**
     * Returns molecule frequency of this fragment as a percentage.
     * <br>NOTE: Do not delete or rename this method, it is used by reflection (in FragmentsDataTableView, the
     * CellValueFactory of the molecule percentage column is set to a PropertyValueFactory that uses "moleculePercentage" as
     * property string to invoke this method; see also DataModelPropertiesForTableView enum).
     *
     * @return double moleculePercentage
     */
    public double getMoleculePercentage() {
        return this.moleculePercentage;
    }
    //
    /**
     * Returns list of parent molecules which contains this fragment.
     *
     * @return set of parent molecules
     */
    public Set<MoleculeDataModel> getParentMolecules() {
        return this.parentMolecules;
    }
    //
    /**
     * Returns the MoleculeDataModel that occurs at first pos in the list of parent molecules. Returns null if no parent
     * molecules are set.
     *
     * @return MoleculeDataModel first molecule of list of parent molecules
     */
    public MoleculeDataModel getFirstParentMolecule() {
        if (this.parentMolecules.isEmpty()) {
            return null;
        }
        if (this.parentMolecule == null) {
            this.parentMolecule = this.parentMolecules.stream().findFirst().orElse(null);
        }
        return this.parentMolecule;
    }
    //
    /**
     * Creates and returns an ImageView of first parent molecule as 2D structure of this fragment.
     * <br>NOTE: Do not delete or rename this method, it is used by reflection (in FragmentsDataTableView, the
     * CellValueFactory of the parents column is set to a PropertyValueFactory that uses "parentMoleculeStructure" as
     * property string to invoke this method; see also DataModelPropertiesForTableView enum).
     *
     * @return ImageView of the first parent molecule or error image if none is set
     */
    public ImageView getParentMoleculeStructure() throws NullPointerException {
        if (this.parentMolecules.isEmpty()) {
            return new ImageView(DepictionUtil.depictErrorImage("No parent molecules", 250, 250));
        }
        if (this.parentMolecule == null) {
            this.parentMolecule = this.parentMolecules.stream().findFirst().orElse(null);
        }
        try {
            // throws NullPointerException if parent molecule is null
            IAtomContainer tmpAtomContainer = this.parentMolecule.getAtomContainer();
            return new ImageView(DepictionUtil.depictImageWithHeight(tmpAtomContainer, super.getStructureImageHeight()));
        } catch (CDKException | NullPointerException anException) {
            FragmentDataModel.LOGGER.log(
                    Level.SEVERE,
                    String.format("Molecule name: %s; exception: %s", this.parentMolecule.getName(), anException.toString()),
                    anException);
            return new ImageView(DepictionUtil.depictErrorImage(anException.getMessage(), 250, 250));
        }
    }
    //
    /**
     * Returns the name of first parent molecule of this fragment.
     * <br>NOTE: Do not delete or rename this method, it is used by reflection (in FragmentsDataTableView, the
     * CellValueFactory of the parent mol name column is set to a PropertyValueFactory that uses "parentMoleculeName" as
     * property string to invoke this method; see also DataModelPropertiesForTableView enum).
     *
     * @return parent molecule name or an empty string if no parent molecule is set
     */
    public String getParentMoleculeName() {
        if (this.parentMolecules.isEmpty()) {
            return "";
        }
        if (this.parentMolecule == null) {
            this.parentMolecule = this.parentMolecules.stream().findFirst().orElse(null);
        }
        return this.parentMolecule == null ? "" : this.parentMolecule.getName();
    }
    //</editor-fold>
    //
    //<editor-fold desc="public properties set">
    /**
     * Sets the absolute frequency.
     * This operation is NOT atomic because of parameter checks.
     *
     * @param aValue absolute frequency
     */
    public void setAbsoluteFrequency(int aValue) {
        if (aValue < 0 ) {
            throw new IllegalArgumentException("aValue must be positive or zero.");
        }
        this.absoluteFrequency.set(aValue);
    }
    //
    /**
     * Sets the molecule frequency.
     * This operation is NOT atomic because of parameter checks.
     *
     * @param aValue molecule frequency
     */
    public void setMoleculeFrequency(int aValue) {
        if (aValue < 0 ) {
            throw new IllegalArgumentException("aValue must be positive or zero.");
        }
        this.moleculeFrequency.set(aValue);
    }
    //
    /**
     * Sets the absolute frequency percentage.
     *
     * @param aValue absolute frequency percentage
     */
    public void setAbsolutePercentage(double aValue) {
        if (aValue < 0.0 ) {
            throw new IllegalArgumentException("aValue must be positive or zero.");
        }
        if (!Double.isFinite(aValue)) {
            throw new IllegalArgumentException("aValue must be finite.");
        }
        this.absolutePercentage = aValue;
    }
    //
    /**
     * Sets the molecule frequency percentage.
     *
     * @param aValue molecule frequency percentage
     */
    public void setMoleculePercentage(double aValue) {
        if (aValue < 0.0 ) {
            throw new IllegalArgumentException("aValue must be positive or zero.");
        }
        if (!Double.isFinite(aValue)) {
            throw new IllegalArgumentException("aValue must be finite.");
        }
        this.moleculePercentage = aValue;
    }
    //
    /**
     * Sets the parent molecule of this fragment.
     *
     * @param aParentMolecule parent molecule
     */
    public void setParentMolecule(MoleculeDataModel aParentMolecule){
        Objects.requireNonNull(aParentMolecule,"aParentMolecule must not be null.");
        this.parentMolecule = aParentMolecule;
    }
    //</editor-fold>
}
