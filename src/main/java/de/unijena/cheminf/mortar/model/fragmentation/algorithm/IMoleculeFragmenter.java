/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2023  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

import de.unijena.cheminf.mortar.model.fragmentation.FragmentationService;
import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;

import javafx.beans.property.Property;

import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.List;
import java.util.Map;

/**
 * Central interface for implementing wrapper classes for fragmentation algorithms. To make a new fragmentation algorithm
 * available in MORTAR, a class implementing this interface and the algorithm logic must be given and added in the
 * {@link FragmentationService} class (instantiated in the constructor
 * and added to the <i>fragmenters</i> array).
 * <br>The algorithm logic must be implemented in the {@link IMoleculeFragmenter#fragmentMolecule(IAtomContainer)} method.
 * <br>The methods {@link IMoleculeFragmenter#shouldBeFiltered(IAtomContainer)},
 * {@link IMoleculeFragmenter#shouldBePreprocessed(IAtomContainer)}, and
 * {@link IMoleculeFragmenter#canBeFragmented(IAtomContainer)} must implement tests for specific requirements of the
 * molecules to fragment if there are any, e.g. if metal or metalloid atoms cannot be handled or counter-ions should be
 * removed before the fragmentation. Preprocessing steps like the latter one must be implemented in
 * {@link IMoleculeFragmenter#applyPreprocessing(IAtomContainer)}.
 * <br>
 * <br><b>Settings</b>:
 * <br>All fragmentation settings that are supposed to be available in the GUI must be implemented as
 * {@link javafx.beans.property.Property} and returned in a list by the interface method
 * {@link IMoleculeFragmenter#settingsProperties()}. Boolean settings must be implemented as
 * {@link javafx.beans.property.SimpleBooleanProperty}, integer settings as
 * {@link javafx.beans.property.SimpleIntegerProperty} etc. For settings where an option must be chosen from multiple
 * available ones, a special Property class is implemented in MORTAR, {@link SimpleEnumConstantNameProperty}. The
 * options to choose from must be implemented as enum constants and the setting property linked to the enum. If changes
 * to the settings done in the GUI must be tested, it is recommended to override the Property.set() method and implement
 * the parameter test logic there. Tooltip texts for the settings must be given in a HashMap with setting (property) names
 * as keys and tooltip text as values (see {@link IMoleculeFragmenter#getSettingNameToTooltipTextMap()}). One setting that
 * must be available is the fragment saturation setting that is already laid out in this interface, see below.
 * <br>
 * <br>More details can be found in the method documentations of this interface.
 * Examples for how to implement this interface to make a new fragmentation algorithm available, can be found in
 * the classes {@link ErtlFunctionalGroupsFinderFragmenter}, {@link SugarRemovalUtilityFragmenter}, and
 * {@link ScaffoldGeneratorFragmenter}.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
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
     * Property key/name to assign a category to a fragment, represented by an IAtomContainer, e.g. 'aglycone' or
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
     * Returns a map containing descriptive texts (values) for the settings with the given names (keys) to be used as
     * tooltips in the GUI.
     *
     * @return map with tooltip texts
     */
    public Map<String, String> getSettingNameToTooltipTextMap();

    /**
     * Returns a string representation of the algorithm name, e.g. "ErtlFunctionalGroupsFinder" or "Ertl algorithm".
     * The given name must be unique among the available fragmentation algorithms!
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
     * Sets the option for saturating free valences on returned fragment molecules.
     *
     * @param anOptionName constant name (use name()) from FragmentSaturationOption enum
     * @throws NullPointerException if the given name is null
     * @throws IllegalArgumentException if the given string does not represent an enum constant
     */
    public void setFragmentSaturationSetting(String anOptionName) throws NullPointerException, IllegalArgumentException;

    /**
     * Sets the option for saturating free valences on returned fragment molecules.
     *
     * @param anOption the saturation option to use
     * @throws NullPointerException if the given option is null
     */
    public void setFragmentSaturationSetting(FragmentSaturationOption anOption) throws NullPointerException;

    /**
     * Returns a new instance of the respective fragmenter with the same settings as this instance. Intended for
     * multi-threaded work where every thread needs its own fragmenter instance.
     *
     * @return new fragmenter instance with the same settings
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
     * @return a list of fragments (the list may be empty if no fragments are extracted, but the fragments should not be!)
     * @throws NullPointerException if aMolecule is null
     * @throws IllegalArgumentException if the given molecule cannot be fragmented but should be filtered or preprocessed
     * @throws CloneNotSupportedException if cloning the given molecule fails
     */
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule)
            throws NullPointerException, IllegalArgumentException, CloneNotSupportedException;

    /**
     * Returns true if the given molecule cannot be fragmented by the respective algorithm, even after preprocessing.
     * If the molecule is null, true is returned and no exception thrown.
     *
     * @param aMolecule the molecule to check
     * @return true if the given molecule is not acceptable as input for the fragmentation algorithm, even if it would be
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
}
