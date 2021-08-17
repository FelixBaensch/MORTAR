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

package de.unijena.cheminf.mortar.model.fragmentation.algorithm;

/**
 * TODO:
 * -
 */

import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;
import javafx.beans.property.Property;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.util.List;
import java.util.Objects;

/**
 * Central interface for implementing wrapper classes for fragmentation algorithms.
 *
 * @author Jonas Schaub
 */
public interface IMoleculeFragmenter {
    //<editor-fold desc="FragmentSaturationOption enum">
    /**
     * Enumeration of different ways to saturate free valences of returned fragment molecules
     */
    public static enum FragmentSaturationOption {
        /**
         * Do not saturate free valences or use default of the respective fragmenter.
         */
        NO_SATURATION,

        /**
         * Saturate free valences with (implicit) hydrogen atoms.
         */
        HYDROGEN_SATURATION;
    }
    //</editor-fold>
    //<editor-fold desc="Public static final constants">
    /**
     * Property key/name to assign a category to a fragment, represented by an IAtomContainer, e.g. 'aglycon' or
     * 'functional group'.
     */
    public static final String FRAGMENT_CATEGORY_PROPERTY_KEY = "IMoleculeFragmenter.Category";

    /**
     * Default option for saturating free valences on the returned fragment molecules.
     */
    public static final FragmentSaturationOption FRAGMENT_SATURATION_OPTION_DEFAULT = FragmentSaturationOption.HYDROGEN_SATURATION;
    //</editor-fold>
    //
    //<editor-fold desc="Public properties">
    /**
     * Returns a list of all available settings represented by properties for the given fragmentation algorithm.
     *
     * @return list of settings represented by properties
     */
    public List<Property> settingsProperties();

    /**
     * Returns a string representation of the algorithm name, e.g. "ErtlFunctionalGroupsFinder" or "Ertl algorithm".
     *
     * @return algorithm name
     */
    public String getFragmentationAlgorithmName();

    /**
     * Returns the currently set option for saturating free valences on returned fragment molecules.
     *
     * @return the set option
     */
    public String getFragmentSaturationSetting();

    /**
     * Returns the property representing the setting for fragment saturation.
     *
     * @return setting property for fragment saturation
     */
    public SimpleEnumConstantNameProperty fragmentSaturationSettingProperty();

    /**
     * Returns the currently set fragment saturation option as the respective enum constant.
     *
     * @return fragment saturation setting enum constant
     */
    public FragmentSaturationOption getFragmentSaturationSettingConstant();

    /**
     * Set the option for saturating free valences on returned fragment molecules.
     *
     * @param anOptionName constant name (use name()) from FragmentSaturationOption enum
     * @throws NullPointerException if the given name is null
     * @throws IllegalArgumentException if the given string does not represent an enum constant
     */
    public void setFragmentSaturationSetting(String anOptionName) throws NullPointerException, IllegalArgumentException;

    /**
     * Set the option for saturating free valences on returned fragment molecules.
     *
     * @param anOption the saturation option to use
     * @throws NullPointerException if the given option is null
     */
    public void setFragmentSaturationSetting(FragmentSaturationOption anOption) throws NullPointerException;

    /**
     * Returns a new instance of the respective fragmenter with the same settings as this instance. Intended for
     * multi-threaded work where every thread needs its own fragmenter instance.
     */
    public IMoleculeFragmenter copy();

    /**
     * Restore all settings of the fragmenter to their default values.
     */
    public void restoreDefaultSettings();
    //</editor-fold>
    //
    //<editor-fold desc="Public methods">
    /**
     * Fragments a clone(!) of the given molecule according to the respective algorithm and returns the resulting fragments.
     *
     * @param aMolecule to fragment
     * @return a list of fragments (might be empty, but the fragments should not be!)
     * @throws NullPointerException if aMolecule is null
     * @throws IllegalArgumentException if the given molecule cannot be fragmented but should be filtered or preprocessed
     * @throws CloneNotSupportedException if cloning the given molecule fails
     */
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule)
            throws NullPointerException, IllegalArgumentException, CloneNotSupportedException;

    //TODO: Is this method necessary? The "returned fragments option" makes it very complicated. A check of the returned list should be sufficient
    /**
     * Returns true if the fragmented molecule has e.g. functional groups or sugar moieties that are detected by the respective
     * algorithm. The Ertl functional groups fragmenter, for example, returns the unchanged molecule as an alkane fragment
     * if no functional groups can be identified. In this case, this method would return false. Other fragmenters might
     * return no fragment at all in such a case. For harmonising these different behaviours, this method is implemented.
     * Also, the type of returned fragments may depend on the fragmenter settings.
     *
     * @param aFragmentList a list of fragments resulting from one molecule through fragmentation by this fragmenter
     * @return true if the fragmenter identified its specific type of molecular moieties, e.g. functional groups or
     * sugar moieties
     * @throws NullPointerException if fragment list is null
     * @throws IllegalArgumentException if the given list did not result from a fragmentation done by this class
     */
    //public boolean hasFragments(List<IAtomContainer> aFragmentList) throws NullPointerException, IllegalArgumentException;

    /**
     * Returns true if the given molecule cannot be fragmented by the respective algorithm, even after preprocessing.
     * If the molecule is null, true is returned and no exception thrown.
     *
     * @param aMolecule the molecule to check
     * @return true if the given molecule is not acceptable as input for the fragmentation algorithm, even if it is
     * preprocessed
     */
    public boolean shouldBeFiltered(IAtomContainer aMolecule);

    /**
     * Returns true if the given molecule can be fragmented by the respective algorithm after preprocessing. Returns
     * false if the given molecule can be directly fragmented by the algorithm without preprocessing.
     * Does not check whether the molecule should be filtered! But throws an exception if it is null.
     *
     * @param aMolecule the molecule to check
     * @return true if the molecule needs to be preprocessed, false if it can be fragmented directly
     * @throws NullPointerException if the molecule is null
     */
    public boolean shouldBePreprocessed(IAtomContainer aMolecule) throws NullPointerException;

    /**
     * Returns true only if the given molecule can be passed to the central fragmentation method without any preprocessing
     * and without causing an exception. If 'false' is returned, check the methods for filtering and preprocessing.
     *
     * @param aMolecule the molecule to check
     * @return true if the molecule can be directly fragmented
     * @throws NullPointerException if the molecule is null
     */
    public boolean canBeFragmented(IAtomContainer aMolecule) throws NullPointerException;

    /**
     * Applies the needed preprocessing for fragmentation to the given molecule. Throws an exception if the molecule
     * should be filtered.
     *
     * @param aMolecule the molecule to preprocess
     * @return a copy of the given molecule that has been preprocessed
     * @throws NullPointerException if the molecule is null
     * @throws IllegalArgumentException if the molecule should be filtered, i.e. it cannot be fragmented even after
     * preprocessing
     * @throws CloneNotSupportedException if cloning the given molecule fails
     */
    public IAtomContainer applyPreprocessing(IAtomContainer aMolecule)
            throws NullPointerException, IllegalArgumentException, CloneNotSupportedException;
    //</editor-fold>
    //<editor-fold desc="Static methods">
    /**
     * Saturates free valences in the returned fragment molecules with implicit hydrogen atoms.
     *
     * @param aFragment to saturate
     * @throws NullPointerException if the given fragment is null
     * @throws CDKException if atom types cannot be assigned to all atoms of the fragment molecule
     */
    public static void saturateWithHydrogen(IAtomContainer aFragment) throws NullPointerException, CDKException {
        Objects.requireNonNull(aFragment, "Given molecule fragment is null.");
        if (aFragment.isEmpty()) {
            //fragments might be empty on purpose, e.g. when there is no aglycon in a molecule
            return;
        }
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(aFragment);
        CDKHydrogenAdder.getInstance(DefaultChemObjectBuilder.getInstance()).addImplicitHydrogens(aFragment);
    }
    //</editor-fold>
}
