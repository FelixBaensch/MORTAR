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

package de.unijena.cheminf.mortar.model.fragmentation.algorithm;

import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.fragmentation.FragmentationService;
import de.unijena.cheminf.mortar.model.util.IDisplayEnum;
import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;
import de.unijena.cheminf.mortar.model.util.SimpleIDisplayEnumConstantProperty;

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
 * available ones, two special Property classes are implemented in MORTAR, {@link SimpleEnumConstantNameProperty}
 * and {@link SimpleIDisplayEnumConstantProperty}. The
 * options to choose from must be implemented as enum constants and the setting property linked to the enum. If changes
 * to the settings done in the GUI must be tested, it is recommended to override the Property.set() method and implement
 * the parameter test logic there. Tooltip texts for the settings must be given in a HashMap with setting (property) names
 * as keys and tooltip text as values (see {@link IMoleculeFragmenter#getSettingNameToTooltipTextMap()}).
 * Similarly, names for the settings that are language-specific and can be displayed in the GUI must be given. One setting that
 * must be available is the fragment saturation setting that is already laid out in this interface, see below.
 * <br>
 * <br>More details can be found in the method documentations of this interface.
 * Examples for how to implement this interface to make a new fragmentation algorithm available, can be found in
 * the classes {@link ErtlFunctionalGroupsFinderFragmenter}, {@link SugarRemovalUtilityFragmenter}, and
 * {@link ScaffoldGeneratorFragmenter}.
 * <br>
 * <br>Language-specific texts that are displayed in the GUI are fetched via the
 * {@link de.unijena.cheminf.mortar.message.Message} class from a resource file in
 * /src/main/resources/de/unijena/cheminf/mortar/message/.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public interface IMoleculeFragmenter {
    //<editor-fold desc="Enum FragmentSaturationOption">
    /**
     * Enumeration of different ways to saturate free valences of returned fragment molecules.
     */
    enum FragmentSaturationOption implements IDisplayEnum {
        /**
         * Do not saturate free valences or use default of the respective fragmenter.
         */
        NO_SATURATION(Message.get("IMoleculeFragmenter.FragmentSaturationOption.noSaturation.displayName"),
                Message.get("IMoleculeFragmenter.FragmentSaturationOption.noSaturation.tooltip")),
        /**
         * Saturate free valences with (implicit) hydrogen atoms.
         */
        HYDROGEN_SATURATION(Message.get("IMoleculeFragmenter.FragmentSaturationOption.hydrogenSaturation.displayName"),
                Message.get("IMoleculeFragmenter.FragmentSaturationOption.hydrogenSaturation.tooltip"));
        /**
         * Language-specific name for display in GUI.
         */
        private final String displayName;
        /**
         * Language-specific tooltip text for display in GUI.
         */
        private final String tooltip;
        /**
         * Constructor setting the display name and tooltip.
         *
         * @param aDisplayName display name
         * @param aTooltip tooltip text
         */
        private FragmentSaturationOption(String aDisplayName, String aTooltip) {
            this.displayName = aDisplayName;
            this.tooltip = aTooltip;
        }
        //
        @Override
        public String getDisplayName() {
            return this.displayName;
        }
        //
        @Override
        public String getTooltipText() {
            return this.tooltip;
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Enum ElectronDonationModelOption">
    /**
     * Enum for available electron donation models that combined with a cycle finder algorithm is used to define an
     * aromaticity model to use. Utility for defining the options in a GUI. The electron
     * donation model specified in the constant name is used and a cycle finder algorithm set via the respective option.
     */
    enum ElectronDonationModelOption implements IDisplayEnum {
        /**
         * Daylight electron donation model.
         */
        DAYLIGHT(Message.get("IMoleculeFragmenter.ElectronDonationModelOption.daylight.displayName"),
                Message.get("IMoleculeFragmenter.ElectronDonationModelOption.daylight.tooltip")),
        /**
         * CDK electron donation model.
         */
        CDK(Message.get("IMoleculeFragmenter.ElectronDonationModelOption.cdk.displayName"),
                Message.get("IMoleculeFragmenter.ElectronDonationModelOption.cdk.tooltip")),
        /**
         * CDK electron donation model that additionally allows exocyclic bonds to contribute electrons to the aromatic system.
         */
        CDK_ALLOWING_EXOCYCLIC(Message.get("IMoleculeFragmenter.ElectronDonationModelOption.cdkAllowingExocyclic.displayName"),
                Message.get("IMoleculeFragmenter.ElectronDonationModelOption.cdkAllowingExocyclic.tooltip")),
        /**
         * Pi bonds electron donation model.
         */
        PI_BONDS(Message.get("IMoleculeFragmenter.ElectronDonationModelOption.piBonds.displayName"),
                Message.get("IMoleculeFragmenter.ElectronDonationModelOption.piBonds.tooltip"));
        /**
         * Language-specific name for display in GUI.
         */
        private final String displayName;
        /**
         * Language-specific tooltip text for display in GUI.
         */
        private final String tooltip;
        /**
         * Constructor setting the display name and tooltip.
         *
         * @param aDisplayName display name
         * @param aTooltip tooltip text
         */
        private ElectronDonationModelOption(String aDisplayName, String aTooltip) {
            this.displayName = aDisplayName;
            this.tooltip = aTooltip;
        }
        //
        @Override
        public String getDisplayName() {
            return this.displayName;
        }
        //
        @Override
        public String getTooltipText() {
            return this.tooltip;
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Enum CycleFinderOption">
    /**
     * Enum for defining which cycle finder algorithm should be used to define an aromaticity model. The electron
     * donation model is set via the respective option. See CDK class "Cycles" for more detailed descriptions of the
     * available cycle finders.
     */
    enum CycleFinderOption implements IDisplayEnum {
        /**
         * Algorithm that tries to find all possible rings in a given structure. Might cause IntractableException.
         */
        ALL(Message.get("IMoleculeFragmenter.CycleFinderOption.all.displayName"),
                Message.get("IMoleculeFragmenter.CycleFinderOption.all.tooltip")),
        /**
         * Algorithm that looks for cycles usually checked by the CDK when detecting aromaticity.
         */
        CDK_AROMATIC_SET(Message.get("IMoleculeFragmenter.CycleFinderOption.cdkAromaticSet.displayName"),
                Message.get("IMoleculeFragmenter.CycleFinderOption.cdkAromaticSet.tooltip")),
        /**
         * Gives the shortest cycles through each edge.
         */
        EDGE_SHORT(Message.get("IMoleculeFragmenter.CycleFinderOption.edgeShort.displayName"),
                Message.get("IMoleculeFragmenter.CycleFinderOption.edgeShort.tooltip")),
        /**
         * Unique set of essential cycles of a molecule.
         */
        ESSENTIAL(Message.get("IMoleculeFragmenter.CycleFinderOption.essential.displayName"),
                Message.get("IMoleculeFragmenter.CycleFinderOption.essential.tooltip")),
        /**
         * Minimum Cycle Basis (MCB, aka. SSSR - smallest set of smallest rings).
         */
        MCB(Message.get("IMoleculeFragmenter.CycleFinderOption.mcb.displayName"),
                Message.get("IMoleculeFragmenter.CycleFinderOption.mcb.tooltip")),
        /**
         * Union of all possible MCB cycle sets of a molecule.
         */
        RELEVANT(Message.get("IMoleculeFragmenter.CycleFinderOption.relevant.displayName"),
                Message.get("IMoleculeFragmenter.CycleFinderOption.relevant.tooltip")),
        /**
         *  Shortest cycle through each triple of vertices.
         */
        TRIPLET_SHORT(Message.get("IMoleculeFragmenter.CycleFinderOption.tripletShort.displayName"),
                Message.get("IMoleculeFragmenter.CycleFinderOption.tripletShort.tooltip")),
        /**
         * Shortest cycles through each vertex.
         */
        VERTEX_SHORT(Message.get("IMoleculeFragmenter.CycleFinderOption.vertexShort.displayName"),
                Message.get("IMoleculeFragmenter.CycleFinderOption.vertexShort.tooltip"));
        /**
         * Language-specific name for display in GUI.
         */
        private final String displayName;
        /**
         * Language-specific tooltip text for display in GUI.
         */
        private final String tooltip;
        /**
         * Constructor setting the display name and tooltip.
         *
         * @param aDisplayName display name
         * @param aTooltip tooltip text
         */
        private CycleFinderOption(String aDisplayName, String aTooltip) {
            this.displayName = aDisplayName;
            this.tooltip = aTooltip;
        }
        //
        @Override
        public String getDisplayName() {
            return this.displayName;
        }
        //
        @Override
        public String getTooltipText() {
            return this.tooltip;
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public static final constants">
    /**
     * Property key/name to assign a category to a fragment, represented by an IAtomContainer, e.g. 'aglycone' or
     * 'functional group'.
     */
    String FRAGMENT_CATEGORY_PROPERTY_KEY = "IMoleculeFragmenter.Category";

    /**
     * Default option for saturating free valences on the returned fragment molecules.
     */
    FragmentSaturationOption FRAGMENT_SATURATION_OPTION_DEFAULT = FragmentSaturationOption.HYDROGEN_SATURATION;
    //</editor-fold>
    //
    //<editor-fold desc="Public properties">
    /**
     * Returns a list of all available settings represented by properties for the given fragmentation algorithm.
     *
     * @return list of settings represented by properties
     */
    List<Property<?>> settingsProperties();

    /**
     * Returns a map containing descriptive texts (values) for the settings with the given names (keys) to be used as
     * tooltips in the GUI.
     *
     * @return map with tooltip texts
     */
    Map<String, String> getSettingNameToTooltipTextMap();

    /**
     * Returns a map containing language-specific names (values) for the settings with the given names (keys) to be used
     * in the GUI.
     *
     * @return map with display names
     */
    Map<String, String> getSettingNameToDisplayNameMap();

    /**
     * Returns a string representation of the algorithm name, e.g. "ErtlFunctionalGroupsFinder" or "Ertl algorithm".
     * The given name must be unique among the available fragmentation algorithms! It is mostly used internal for
     * persistence. For other functionalities, the display name (see below) is used.
     *
     * @return algorithm name
     */
    String getFragmentationAlgorithmName();

    /**
     * Returns a language-specific name of the fragmenter to be used in the GUI.
     * The given name must be unique among the available fragmentation algorithms!
     *
     * @return language-specific name for display in GUI
     */
    String getFragmentationAlgorithmDisplayName();

    /**
     * Returns the currently set option for saturating free valences on returned fragment molecules.
     *
     * @return the set option
     */
    FragmentSaturationOption getFragmentSaturationSetting();

    /**
     * Returns the property representing the setting for fragment saturation.
     *
     * @return setting property for fragment saturation
     */
    SimpleIDisplayEnumConstantProperty fragmentSaturationSettingProperty();

    /**
     * Sets the option for saturating free valences on returned fragment molecules.
     *
     * @param anOption the saturation option to use
     * @throws NullPointerException if the given option is null
     */
    void setFragmentSaturationSetting(FragmentSaturationOption anOption) throws NullPointerException;

    /**
     * Returns a new instance of the respective fragmenter with the same settings as this instance. Intended for
     * multi-threaded work where every thread needs its own fragmenter instance.
     *
     * @return new fragmenter instance with the same settings
     */
    IMoleculeFragmenter copy();

    /**
     * Restore all settings of the fragmenter to their default values.
     */
    void restoreDefaultSettings();
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
    List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule)
            throws NullPointerException, IllegalArgumentException, CloneNotSupportedException;

    /**
     * Returns true if the given molecule cannot be fragmented by the respective algorithm, even after preprocessing.
     * If the molecule is null, true is returned and no exception thrown.
     *
     * @param aMolecule the molecule to check
     * @return true if the given molecule is not acceptable as input for the fragmentation algorithm, even if it would be
     * preprocessed
     */
    boolean shouldBeFiltered(IAtomContainer aMolecule);

    /**
     * Returns true if the given molecule can be fragmented by the respective algorithm *after preprocessing*.
     * Does not check whether the molecule should be filtered! It is advised to check via shouldBeFiltered() whether
     * the given molecule should be discarded anyway before calling this function.
     *
     * @param aMolecule the molecule to check
     * @return true if the molecule needs to be preprocessed
     * @throws NullPointerException if the molecule is null
     */
    boolean shouldBePreprocessed(IAtomContainer aMolecule) throws NullPointerException;

    /**
     * Returns true only if the given molecule can be passed to the central fragmentation method without any preprocessing
     * and without causing an exception. If 'false' is returned, check the methods for filtering and preprocessing.
     *
     * @param aMolecule the molecule to check
     * @return true if the molecule can be directly fragmented
     * @throws NullPointerException if the molecule is null
     */
    boolean canBeFragmented(IAtomContainer aMolecule) throws NullPointerException;

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
    IAtomContainer applyPreprocessing(IAtomContainer aMolecule)
            throws NullPointerException, IllegalArgumentException, CloneNotSupportedException;
    //</editor-fold>
}
