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
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.ChemUtil;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.ImageView;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Model class for molecule data.
 *
 * @author Felix Baensch, Samuel Behr
 * @version 1.0.1.0
 */
public class MoleculeDataModel {
    //<editor-fold desc="private (final) class variables" defaultstate="collapsed">
    /**
     * Name of molecule.
     */
    private String name;
    //
    /**
     * Unique SMILES code of molecule.
     */
    private final String uniqueSmiles;
    //
    private final BooleanProperty selection;
    //
    /**
     * Boolean, whether to keep the atom container of the molecule.
     */
    private boolean keepAtomContainer;
    //
    /**
     * Atom container of the molecule. Only initialized, if the atom container should be kept.
     */
    private IAtomContainer atomContainer;
    //
    /**
     * Fragments map containing names of fragmentations done to the molecule as keys and lists of
     * {@link de.unijena.cheminf.mortar.model.data.FragmentDataModel} objects that resulted from these fragmentations
     * as values. {@literal Map<FragmentationName, List<Fragments>>}
     */
    private final Map<String, List<FragmentDataModel>> fragments;
    //
    /**
     * Fragment frequencies map of a specific fragmentation with the given name. Keys of the map are unique SMILES
     * representations of the fragments and values are the frequencies of the respective fragments in the molecule.
     * {@literal Map<FragmentationName, Map<uniqueSMILES, frequency in this molecule>>}
     */
    private final Map<String, Map<String, Integer>> fragmentFrequencies;
    //
    /**
     * Property map of this molecule.
     */
    private final Map<Object, Object> properties;
    //
    /**
     * Height value for image of structure.
     */
    private double structureImageHeight;
    //
    /**
     * Width value for image of structure.
     */
    private double structureImageWidth;
    //</editor-fold>
    //
    //<editor-fold desc="constructors">
    /**
     * Constructor for MoleculeDataModel. Molecular information is taken from the given unique SMILES code. The data
     * is not kept as atom container.
     *
     * @param aUniqueSmiles unique SMILES representation of the molecule
     * @param aName name of the molecule
     * @param aPropertyMap property map of the molecule
     * @throws NullPointerException if given SMILES string is null
     */
    public MoleculeDataModel(String aUniqueSmiles, String aName, Map<Object, Object> aPropertyMap) throws NullPointerException {
        Objects.requireNonNull(aUniqueSmiles, "SMILES is null");
        this.keepAtomContainer = false;
        this.name = aName;
        this.properties = aPropertyMap;
        this.uniqueSmiles = aUniqueSmiles;
        this.selection = new SimpleBooleanProperty(true);
        this.fragments = new HashMap<>(BasicDefinitions.DEFAULT_INITIAL_MAP_CAPACITY,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.fragmentFrequencies = new HashMap<>(BasicDefinitions.DEFAULT_INITIAL_MAP_CAPACITY,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
    }
    //
    /**
     * Constructor for MoleculeDataModel. Retains the given atom container.
     *
     * @param anAtomContainer AtomContainer of the molecule
     * @throws NullPointerException if given SMILES string is null
     */
    public MoleculeDataModel(IAtomContainer anAtomContainer) throws NullPointerException {
        this(ChemUtil.createUniqueSmiles(anAtomContainer), anAtomContainer.getTitle(), anAtomContainer.getProperties());
        this.keepAtomContainer = true;
        this.atomContainer = anAtomContainer;
    }
    //</editor-fold>
    //
    //<editor-fold desc="public properties get/set">
    /**
     * Returns name (String) of the molecule. If it is null, "NoName" will be returned.
     * <br>NOTE: Do not delete or rename this method, it is used by reflection (in MoleculesDataTableView, the
     * CellValueFactory of the name column is set to a PropertyValueFactory that uses "name" as
     * property string to invoke this method; same usage in ItemizationDataTableView; see also
     * DataModelPropertiesForTableView enum).
     *
     * @return String name of molecule
     */
    public String getName() {
        if (this.name == null || this.name.isEmpty()) {
            this.name = "NoName";
        }
        return this.name;
    }
    //
    /**
     * Returns IAtomContainer which represents the molecule. Depending on the preference, the atom container is saved
     * as class variable. If it is re-created from the SMILES code, it is attempted(!) to assign bond types and atom types
     * to it (the former through kekulization). Aromaticity flags are set if there is aromaticity information present in the
     * SMILES code.
     *
     * @return IAtomContainer atom container of the molecule
     * @throws CDKException if SMILES parsing fails
     */
    public IAtomContainer getAtomContainer() throws CDKException {
        if (this.atomContainer != null) {
            return this.atomContainer;
        }
        IAtomContainer tmpAtomContainer;
        try{
            tmpAtomContainer = ChemUtil.parseSmilesToAtomContainer(this.uniqueSmiles, true, true);
        } catch (CDKException aCdkException){
            //no logging, this happens too often, e.g. for fragments of aromatic rings
            tmpAtomContainer = ChemUtil.parseSmilesToAtomContainer(this.uniqueSmiles, false, false);
        }
        tmpAtomContainer.addProperties(this.properties);
        if (this.keepAtomContainer) {
            this.atomContainer = tmpAtomContainer;
        }
        return tmpAtomContainer;
    }
    //
    /**
     * Returns unique SMILES.
     *
     * @return String uniqueSmiles
     */
    public String getUniqueSmiles() {
        return this.uniqueSmiles;
    }
    //
    /**
     * Returns boolean telling whether molecule is selected or not.
     *
     * @return true if molecule is selected
     */
    public boolean isSelected() {
        return this.selection.get();
    }
    //
    /**
     * Returns BooleanProperty whether molecule is selected or not.
     *
     * @return BooleanProperty
     */
    public BooleanProperty selectionProperty() {
        return this.selection;
    }
    //
    /**
     * Returns the fragments map of this molecule data model. The map contains as keys names of fragmentations done to
     * the molecule and as values lists of {@link de.unijena.cheminf.mortar.model.data.FragmentDataModel} objects that
     * resulted from these fragmentations.
     *
     * @return Map {@literal <}fragmentationName, List {@literal <}FragmentDataModel {@literal >>}
     */
    public Map<String, List<FragmentDataModel>> getAllFragments() {
        return this.fragments;
    }
    //
    /**
     * Returns a list of unique fragments that resulted from the fragmentation of the molecule with the given name.
     *
     * @param aKey String specifies fragmentation
     * @return List {@literal <}FragmentDataModel {@literal >}
     */
    public List<FragmentDataModel> getFragmentsOfSpecificFragmentation(String aKey) {
        Objects.requireNonNull(aKey, "Key must not be null");
        return this.fragments.get(aKey);
    }
    //
    /**
     * Returns the fragment frequencies map of this molecule data model. The map contains as keys names of fragmentations
     * done to the molecule and as values maps that in turn contain as keys unique SMILES representations of fragments
     * that resulted from the respective fragmentation and as objects the frequencies of the specific fragment in the molecule.
     *
     * @return Map {@literal <}fragmentationName, Map {@literal <}uniqueSmiles, frequency {@literal >>}
     */
    public Map<String, Map<String, Integer>> getFragmentFrequencies() {
        return this.fragmentFrequencies;
    }
    //
    /**
     * Returns the fragment frequencies map of a specific fragmentation with the given name. Keys of the map are unique
     * SMILES representations of the fragments and values are the frequencies of the respective fragments in the molecule.
     *
     * @param aKey String specifies fragmentation
     * @return Map {@literal <}uniqueSmiles, frequency {@literal >}
     */
    public Map<String, Integer> getFragmentFrequencyOfSpecificFragmentation(String aKey) {
        Objects.requireNonNull(aKey, "Key must not be null");
        return this.fragmentFrequencies.get(aKey);
    }
    //
    /**
     * Specifies whether the molecule has fragments resulting from the fragmentation process with the given name or not.
     *
     * @param aKey fragmentation name
     * @return true if the molecule has undergone the fragmentation with the specified name
     */
    public boolean hasMoleculeUndergoneSpecificFragmentation(String aKey) {
        Objects.requireNonNull(aKey, "aKey must not be null");
        return this.fragments.containsKey(aKey);
    }
    //
    /**
     * Creates and returns an ImageView of this molecule as 2D structure.
     * <br>NOTE: Do not delete or rename this method, it is used by reflection (in MoleculesDataTableView, the
     * CellValueFactory of the structure column is set to a PropertyValueFactory that uses "name" as
     * property string to invoke this method; same usage in ItemizationDataTableView molecule structure column; see also
     * DataModelPropertiesForTableView enum).
     *
     * @return ImageView
     */
    public ImageView getStructure() {
        try {
            IAtomContainer tmpAtomContainer = this.getAtomContainer();
            boolean tmpFillToFit = tmpAtomContainer.getAtomCount() > 6;
            return new ImageView(DepictionUtil.depictImageWithZoomAndFillToFit(tmpAtomContainer, 1, this.getStructureImageWidth(), this.getStructureImageHeight(), tmpFillToFit));
        } catch (CDKException aCDKException) {
            Logger.getLogger(MoleculeDataModel.class.getName()).log(Level.SEVERE, aCDKException.toString(), aCDKException);
            return new ImageView(DepictionUtil.depictErrorImage(aCDKException.getMessage(), 250, 250));
        }
    }
    //
    /**
     * Creates and returns an ImageView of this molecule as 2D structure with the given text below the structure.
     * Mainly used for fragments in items tab.
     *
     * @param aText to show below structure
     * @return ImageView with text
     */
    public ImageView getStructureWithText(String aText){
        try {
            IAtomContainer tmpAtomContainer = this.getAtomContainer();
            return new ImageView(DepictionUtil.depictImageWithText(tmpAtomContainer, 1, this.getStructureImageWidth(), this.getStructureImageHeight(), aText));
        } catch (CDKException aCDKException) {
            Logger.getLogger(MoleculeDataModel.class.getName()).log(Level.SEVERE, aCDKException.toString(), aCDKException);
            return new ImageView(DepictionUtil.depictErrorImage(aCDKException.getMessage(), 250, 250));
        }
    }
    //
    /**
     * Returns property map of this molecule.
     *
     * @return property map
     */
    public Map<Object, Object> getProperties() {
        return this.properties;
    }
    //
    /**
     * Returns whether the atom container of the molecule should be kept or not.
     *
     * @return boolean keepAtomContainer
     */
    public boolean isKeepAtomContainer() {
        return this.keepAtomContainer;
    }
    //
    /**
     * Sets given String as name of this molecule.
     *
     * @param aName String
     */
    public void setName(String aName) {
        this.name = aName;
    }
    //
    /**
     * Sets selection state of this molecule depending on the specified boolean.
     *
     * @param aValue boolean
     */
    public void setSelection(boolean aValue) {
        this.selection.set(aValue);
    }
    //
    /**
     * Sets whether the molecule's atom container should be kept. If not, the atom container is set to null.
     *
     * @param aValue boolean
     */
    public void setKeepAtomContainer(boolean aValue) {
        this.keepAtomContainer = aValue;
        if (!this.keepAtomContainer) {
            this.atomContainer = null;
        }
    }
    //
    /**
     * Returns the height of the image of the molecular structure.
     * Returns {@link BasicDefinitions#DEFAULT_IMAGE_HEIGHT_DEFAULT} if value equals 0.0.
     *
     * @return height of image
     */
    public double getStructureImageHeight() {
        if (this.structureImageHeight == 0.0) {
            return BasicDefinitions.DEFAULT_IMAGE_HEIGHT_DEFAULT;
        } else {
            return this.structureImageHeight;
        }
    }
    //
    /**
     * Returns the width of the image of the molecular structure.
     * Returns {@link BasicDefinitions#DEFAULT_IMAGE_WIDTH_DEFAULT} if value equals 0.0.
     *
     * @return width of image
     */
    public double getStructureImageWidth() {
        if (this.structureImageWidth == 0.0) {
            return BasicDefinitions.DEFAULT_IMAGE_WIDTH_DEFAULT;
        } else {
            return this.structureImageWidth;
        }
    }
    //
    /**
     * Sets the height of the image of the molecular structure.
     *
     * @param aStructureImageHeight double
     */
    public void setStructureImageHeight(double aStructureImageHeight) {
        this.structureImageHeight = aStructureImageHeight;
    }
    //
    /**
     * Sets the width of the image of the molecular structure.
     *
     * @param aStructureImageWidth double
     */
    public void setStructureImageWidth(double aStructureImageWidth) {
        this.structureImageWidth = aStructureImageWidth;
    }
    //</editor-fold>
}
