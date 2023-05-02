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

/**
 * TODO:
 * -
 */

import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.io.Importer;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.ChemUtil;
import de.unijena.cheminf.mortar.model.util.CollectionUtil;
import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.tools.ErtlFunctionalGroupsFinder;
import org.openscience.cdk.tools.ErtlFunctionalGroupsFinderUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper class that makes the
 * <a href="https://doi.org/10.1186/s13321-017-0225-z">Ertl algorithm for automated identification and extraction of functional groups</a>
 * available in MORTAR, using the <a href="https://doi.org/10.1186/s13321-019-0361-8">ErtlFunctionalGroupsFinder</a>
 * CDK implementation of the algorithm.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class ErtlFunctionalGroupsFinderFragmenter implements IMoleculeFragmenter {
    //<editor-fold desc="Enum FGEnvOption">
    /**
     * Enum for options concerning the environment of returned functional group fragments.
     */
    public static enum FGEnvOption {
        /**
         * Generalize environments of functional groups.
         */
        GENERALIZATION(ErtlFunctionalGroupsFinder.Mode.DEFAULT),

        /**
         * Do not generalize but give the full environment of functional groups.
         */
        FULL_ENVIRONMENT(ErtlFunctionalGroupsFinder.Mode.NO_GENERALIZATION),

        /**
         * Return only the marked atoms of a functional group, no environment. The EFGF mode for generalization is
         * associated but the returned FG need additional processing to only return the marked atoms.
         */
        NO_ENVIRONMENT(ErtlFunctionalGroupsFinder.Mode.DEFAULT);

        /**
         * The ErtlFunctionalGroupsFinder mode to use in the respective cases.
         */
        private final ErtlFunctionalGroupsFinder.Mode mode;

        /**
         * Constructor.
         *
         * @param aMode the EFGF mode to use with the respective option; the internal EFGF instance will be set with this
         *              mode when the option is chosen
         */
        private FGEnvOption(ErtlFunctionalGroupsFinder.Mode aMode) {
            this.mode = aMode;
        }

        /**
         * Returns the EFGF mode to use with the respective option.
         *
         * @return EFGF mode to use with this option
         */
        public ErtlFunctionalGroupsFinder.Mode getAssociatedEFGFMode() {
            return this.mode;
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
    public static enum ElectronDonationModelOption {
        /**
         * Daylight electron donation model.
         */
        DAYLIGHT,

        /**
         * CDK electron donation model.
         */
        CDK,

        /**
         * CDK electron donation model that additionally allows exocyclic bonds to contribute electrons to the aromatic system.
         */
        CDK_ALLOWING_EXOCYCLIC,

        /**
         * Pi bonds electron donation model.
         */
        PI_BONDS;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Enum EFGFFragmenterReturnedFragmentsOption">
    /**
     * Enum for defining which fragments should be returned by the fragmentation methods, only the functional groups,
     * only the alkane fragments, or both.
     */
    public static enum EFGFFragmenterReturnedFragmentsOption {
        /**
         * Option to return only the identified functional groups of a molecule after fragmentation.
         */
        ONLY_FUNCTIONAL_GROUPS,

        /**
         * Option to return only the non-functional-group alkane fragments of a molecule after fragmentation.
         */
        ONLY_ALKANE_FRAGMENTS,

        /**
         * Option to return both, functional groups and alkane fragments, after fragmentation.
         */
        ALL_FRAGMENTS;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Enum CycleFinderOption">
    /**
     * Enum for defining which cycle finder algorithm should be used to define an aromaticity model. The electron
     * donation model is set via the respective option. See CDK class "Cycles" for more detailed descriptions of the
     * available cycle finders.
     */
    public static enum CycleFinderOption {
        /**
         * Algorithm that tries to find all possible rings in a given structure. Might cause IntractableException.
         */
        ALL,

        /**
         * Algorithm that looks for cycles usually checked by the CDK when detecting aromaticity.
         */
        CDK_AROMATIC_SET,

        /**
         * Gives the shortest cycles through each edge.
         */
        EDGE_SHORT,

        /**
         * Unique set of essential cycles of a molecule.
         */
        ESSENTIAL,

        /**
         * Minimum Cycle Basis (MCB, aka. SSSR - smallest set of smallest rings).
         */
        MCB,

        /**
         * Union of all possible MCB cycle sets of a molecule.
         */
        RELEVANT,

        /**
         *  Shortest cycle through each triple of vertices.
         */
        TRIPLET_SHORT,

        /**
         * Shortest cycles through each vertex.
         */
        VERTEX_SHORT;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public static final constants">
    /**
     * Name of the algorithm used in this fragmenter.
     */
    public static final String ALGORITHM_NAME = "Ertl algorithm";

    /**
     * Key for an index property that is used internally for unique identification of atoms in a given molecule.
     */
    public static final String INTERNAL_INDEX_PROPERTY_KEY = "EFGFFragmenter.INDEX";

    /**
     * Default electron donation model for aromaticity detection.
     */
    public static final ElectronDonationModelOption Electron_Donation_MODEL_OPTION_DEFAULT = ElectronDonationModelOption.DAYLIGHT;

    /**
     * Default functional group environment option.
     */
    public static final FGEnvOption ENVIRONMENT_MODE_OPTION_DEFAULT = FGEnvOption.GENERALIZATION;

    /**
     * Default returned fragments option.
     */
    public static final EFGFFragmenterReturnedFragmentsOption RETURNED_FRAGMENTS_OPTION_DEFAULT = EFGFFragmenterReturnedFragmentsOption.ALL_FRAGMENTS;

    /**
     * Default option for the cycle finder algorithm employed for aromaticity detection.
     */
    public static final CycleFinderOption CYCLE_FINDER_OPTION_DEFAULT = CycleFinderOption.CDK_AROMATIC_SET;

    /**
     * Default option for whether to filter single-atom molecules from inputs.
     */
    public static final boolean FILTER_SINGLE_ATOMS_OPTION_DEFAULT = true;

    /**
     * Cycle finder algorithm that is used should the set option cause an IntractableException.
     */
    public static final CycleFinder AUXILIARY_CYCLE_FINDER = Cycles.cdkAromaticSet();

    /**
     * Functional group fragments will be assigned this value for the property with key IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY.
     */
    public static final String FRAGMENT_CATEGORY_FUNCTIONAL_GROUP_VALUE = "EFGFFragmenter.FunctionalGroup";

    /**
     * Alkane fragments will be assigned this value for the property with key IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY.
     */
    public static final String FRAGMENT_CATEGORY_ALKANE_VALUE = "EFGFFragmenter.Alkane";
    //</editor-fold>
    //
    //<editor-fold desc="Private variables">
    /**
     * Instance of ErtlfFunctionalGroupsFinder class used to do the extraction of functional groups. If the FG
     * environment setting changes, this object needs to be reset in most cases with the respectively needed mode.
     */
    private ErtlFunctionalGroupsFinder ertlFGFInstance;

    /**
     * The aromaticity model used for preprocessing prior to FG extraction. Constructed from the set electron donation
     * model and cycle finder algorithm.
     */
    private Aromaticity aromaticityModelInstance;

    /**
     * A cycle finder instance for construction of the aromaticity model.
     */
    private CycleFinder cycleFinderInstance;

    /**
     * An electron donation instance for construction of the aromaticity model.
     */
    private ElectronDonation electronDonationInstance;
    //</editor-fold>
    //
    //<editor-fold desc="Private final variables">
    /**
     * A property that has a constant name from the FGEnvOption enum as value.
     */
    private final SimpleEnumConstantNameProperty environmentModeSetting;

    /**
     * A property that has a constant name from the ElectronDonationModelOption enum as value.
     */
    private final SimpleEnumConstantNameProperty electronDonationModelSetting;

    /**
     * A property that has a constant name from the IMoleculeFragmenter.FragmentSaturationOption enum as value.
     */
    private final SimpleEnumConstantNameProperty fragmentSaturationSetting;

    /**
     * A property that has a constant name from the EFGFFragmenterReturnedFragmentsOption enum as value.
     */
    private final SimpleEnumConstantNameProperty returnedFragmentsSetting;

    /**
     * A property that has a constant name from the CycleFinderOption enum as value.
     */
    private final SimpleEnumConstantNameProperty cycleFinderSetting;

    /**
     * A property that has a boolean as value saying whether single-atom molecules should be filtered from inputs.
     */
    private final SimpleBooleanProperty filterSingleAtomsSetting;

    /**
     * All settings of this fragmenter, encapsulated in JavaFX properties for binding in GUI.
     */
    private final List<Property> settings;

    /**
     * Map to store pairs of {@literal <setting name, tooltip text>}.
     */
    private final HashMap<String, String> settingNameTooltipTextMap;

    /**
     * Logger of this class.
     */
    private final Logger logger = Logger.getLogger(ErtlFunctionalGroupsFinderFragmenter.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="Constructor">
    /**
     * Constructor, all settings are initialised with their default values as declared in the respective public constants.
     */
    public ErtlFunctionalGroupsFinderFragmenter() {
        int tmpNumberOfSettingsForTooltipMapSize= 6;
        int tmpInitialCapacityForSettingNameTooltipTextMap = CollectionUtil.calculateInitialHashCollectionCapacity(
                tmpNumberOfSettingsForTooltipMapSize,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.settingNameTooltipTextMap = new HashMap(tmpInitialCapacityForSettingNameTooltipTextMap, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.fragmentSaturationSetting = new SimpleEnumConstantNameProperty(this, "Fragment saturation setting",
                IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT.name(), IMoleculeFragmenter.FragmentSaturationOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ErtlFunctionalGroupsFinderFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        this.settingNameTooltipTextMap.put(this.fragmentSaturationSetting.getName(),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.fragmentSaturationSetting.tooltip"));
        this.environmentModeSetting = new SimpleEnumConstantNameProperty(this, "Environment mode setting",
                ErtlFunctionalGroupsFinderFragmenter.ENVIRONMENT_MODE_OPTION_DEFAULT.name(), ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ErtlFunctionalGroupsFinderFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                //throws no exception if super.set() throws no exception
                ErtlFunctionalGroupsFinderFragmenter.this.setErtlFGFInstance(FGEnvOption.valueOf(newValue));
            }
        };
        this.settingNameTooltipTextMap.put(this.environmentModeSetting.getName(),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.environmentModeSetting.tooltip"));
        //initialisation of EFGF instance
        this.setErtlFGFInstance(FGEnvOption.valueOf(this.environmentModeSetting.get()));
        this.returnedFragmentsSetting = new SimpleEnumConstantNameProperty(this, "Returned fragments setting",
                ErtlFunctionalGroupsFinderFragmenter.RETURNED_FRAGMENTS_OPTION_DEFAULT.name(), EFGFFragmenterReturnedFragmentsOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ErtlFunctionalGroupsFinderFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        this.settingNameTooltipTextMap.put(this.returnedFragmentsSetting.getName(),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.returnedFragmentsSetting.tooltip"));
        //note: cycle finder and electron donation model have to be set prior to setting the aromaticity model!
        this.cycleFinderSetting = new SimpleEnumConstantNameProperty(this, "Cycle finder algorithm setting",
                ErtlFunctionalGroupsFinderFragmenter.CYCLE_FINDER_OPTION_DEFAULT.name(),
                ErtlFunctionalGroupsFinderFragmenter.CycleFinderOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ErtlFunctionalGroupsFinderFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                //throws no exception if super.set() throws no exception
                ErtlFunctionalGroupsFinderFragmenter.this.setCycleFinderInstance(CycleFinderOption.valueOf(newValue));
                ErtlFunctionalGroupsFinderFragmenter.this.setAromaticityInstance(
                        ErtlFunctionalGroupsFinderFragmenter.this.electronDonationInstance,
                        ErtlFunctionalGroupsFinderFragmenter.this.cycleFinderInstance);
            }
        };
        this.settingNameTooltipTextMap.put(this.cycleFinderSetting.getName(),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.cycleFinderSetting.tooltip"));
        this.setCycleFinderInstance(CycleFinderOption.valueOf(this.cycleFinderSetting.get()));
        this.electronDonationModelSetting = new SimpleEnumConstantNameProperty(this, "Electron donation model setting",
                ErtlFunctionalGroupsFinderFragmenter.Electron_Donation_MODEL_OPTION_DEFAULT.name(),
                ElectronDonationModelOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ErtlFunctionalGroupsFinderFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                //throws no exception if super.set() throws no exception
                ErtlFunctionalGroupsFinderFragmenter.this.setElectronDonationInstance(ElectronDonationModelOption.valueOf(newValue));
                ErtlFunctionalGroupsFinderFragmenter.this.setAromaticityInstance(
                        ErtlFunctionalGroupsFinderFragmenter.this.electronDonationInstance,
                        ErtlFunctionalGroupsFinderFragmenter.this.cycleFinderInstance);
            }
        };
        this.settingNameTooltipTextMap.put(this.electronDonationModelSetting.getName(),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.electronDonationModelSetting.tooltip"));
        this.setElectronDonationInstance(ElectronDonationModelOption.valueOf(this.electronDonationModelSetting.get()));
        this.setAromaticityInstance(
                ErtlFunctionalGroupsFinderFragmenter.this.electronDonationInstance,
                ErtlFunctionalGroupsFinderFragmenter.this.cycleFinderInstance
        );
        this.filterSingleAtomsSetting = new SimpleBooleanProperty(this, "Filter single atoms setting",
                ErtlFunctionalGroupsFinderFragmenter.FILTER_SINGLE_ATOMS_OPTION_DEFAULT);
        this.settingNameTooltipTextMap.put(this.filterSingleAtomsSetting.getName(),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.filterSingleAtomsSetting.tooltip"));
        this.settings = new ArrayList<Property>(6);
        this.settings.add(this.fragmentSaturationSetting);
        this.settings.add(this.electronDonationModelSetting);
        this.settings.add(this.cycleFinderSetting);
        this.settings.add(this.environmentModeSetting);
        this.settings.add(this.returnedFragmentsSetting);
        this.settings.add(this.filterSingleAtomsSetting);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties get">
    /**
     * Returns the string representation of the currently set option for the environment mode setting.
     *
     * @return enum constant name of the set option
     */
    public String getEnvironmentModeSetting() {
        return this.environmentModeSetting.get();
    }

    /**
     * Returns the property object of the environment mode setting that can be used to configure this setting.
     *
     * @return property object of the environment mode setting
     */
    public SimpleEnumConstantNameProperty environmentModeSettingProperty() {
        return this.environmentModeSetting;
    }

    /**
     * Returns the enum constant currently set as option for the environment mode setting.
     *
     * @return enum constant for environment mode setting
     */
    public FGEnvOption getEnvironmentModeSettingConstant() {
        return FGEnvOption.valueOf(this.environmentModeSetting.get());
    }

    /**
     * Returns the string representation of the currently set option for the electron donation model setting used for
     * aromaticity detection together with the set cycle finder algorithm.
     *
     * @return enum constant name of the set option
     */
    public String getElectronDonationModelSetting() {
        return this.electronDonationModelSetting.get();
    }

    /**
     * Returns the property object of the electron donation model setting that can be used to configure this setting.
     *
     * @return property object of the electron donation model setting
     */
    public SimpleEnumConstantNameProperty electronDonationModelSettingProperty() {
        return this.electronDonationModelSetting;
    }

    /**
     * Returns the enum constant currently set as option for the electron donation model setting.
     *
     * @return enum constant for electron donation model setting
     */
    public ElectronDonationModelOption getElectronDonationModelSettingConstant() {
        return ElectronDonationModelOption.valueOf(this.electronDonationModelSetting.get());
    }

    /**
     * Returns the string representation of the currently set option for the returned fragments setting
     *
     * @return enum constant name of the set option
     */
    public String getReturnedFragmentsSetting() {
        return this.returnedFragmentsSetting.get();
    }

    /**
     * Returns the property object of the returned fragments setting that can be used to configure this setting.
     *
     * @return property object of the returned fragments setting
     */
    public SimpleEnumConstantNameProperty returnedFragmentsSettingProperty() {
        return this.returnedFragmentsSetting;
    }

    /**
     * Returns the enum constant currently set as option for the returned fragments setting.
     *
     * @return enum constant for returned fragments setting
     */
    public EFGFFragmenterReturnedFragmentsOption getReturnedFragmentsSettingConstant() {
        return EFGFFragmenterReturnedFragmentsOption.valueOf(this.returnedFragmentsSetting.get());
    }

    /**
     * Returns the string representation of the currently set option for the cycle finder setting used for aromaticity
     * detection together with the electron donation model setting.
     *
     * @return enum constant name of the set option
     */
    public String getCycleFinderSetting() {
        return this.cycleFinderSetting.get();
    }

    /**
     * Returns the property object of the cycle finder setting that can be used to configure this setting.
     *
     * @return property object of the cycle finder setting
     */
    public SimpleEnumConstantNameProperty cycleFinderSettingProperty() {
        return this.cycleFinderSetting;
    }

    /**
     * Returns the enum constant currently set as option for the cycle finder setting.
     *
     * @return enum constant for cycle finder setting
     */
    public CycleFinderOption getCycleFinderSettingConstant() {
        return CycleFinderOption.valueOf(this.cycleFinderSetting.get());
    }

    /**
     * Returns the boolean value of the filter single atoms setting.
     *
     * @return true if single atoms are currently filtered from input molecules
     */
    public boolean getFilterSingleAtomsSetting() {
        return this.filterSingleAtomsSetting.get();
    }

    /**
     * Returns the property object of the filter single atoms setting that can be used to configure this setting.
     *
     * @return property object of the filter single atoms setting
     */
    public SimpleBooleanProperty filterSingleAtomsSettingProperty() {
        return this.filterSingleAtomsSetting;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties set">
    /**
     * Sets the environment mode setting defining whether the returned functional group fragments should have full environments,
     * generalized environments or no environments.
     *
     * @param anOptionName name of a constant from the FGEnvOption enum
     * @throws NullPointerException if the given string is null
     * @throws IllegalArgumentException if the given string is not an enum constant name
     */
    public void setEnvironmentModeSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        FGEnvOption tmpConstant = FGEnvOption.valueOf(anOptionName);
        this.setEnvironmentModeSetting(tmpConstant);
    }

    /**
     * Sets the environment mode setting defining whether the returned functional group fragments should have full environments,
     * generalized environments or no environments.
     *
     * @param anOption a constant from the FGEnvOption enum
     * @throws NullPointerException if the given parameter is null
     */
    public void setEnvironmentModeSetting(FGEnvOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        //synchronisation with EFGF instance done in overridden set() function of the property
        this.environmentModeSetting.set(anOption.name());
    }

    /**
     * Sets the electron donation model setting. The set electron donation model is used for aromaticity detection in
     * preprocessing together with the set cycle finder algorithm.
     *
     * @param anOptionName name of a constant from the ElectronDonationModelOption enum
     * @throws NullPointerException if the given string is null
     * @throws IllegalArgumentException if the given string is not an enum constant name
     */
    public void setElectronDonationModelSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        ElectronDonationModelOption tmpConstant = ElectronDonationModelOption.valueOf(anOptionName);
        this.setElectronDonationModelSetting(tmpConstant);
    }

    /**
     * Sets the electron donation model setting. The set electron donation model is used for aromaticity detection in
     * preprocessing together with the set cycle finder algorithm.
     *
     * @param anOption a constant from the ElectronDonationModelOption enum
     * @throws NullPointerException is the given parameter is null
     */
    public void setElectronDonationModelSetting(ElectronDonationModelOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        //synchronisation with aromaticity model instance done in overridden set() function of the property
        this.electronDonationModelSetting.set(anOption.name());
    }

    /**
     * Sets the returned fragments setting, defining whether only functional groups, only alkane fragments, or both should
     * be returned.
     *
     * @param anOptionName name of a constant from the EFGFFragmenterReturnedFragmentsOption enum
     * @throws NullPointerException if the given string is null
     * @throws IllegalArgumentException if the given string is not an enum constant name
     */
    public void setReturnedFragmentsSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        EFGFFragmenterReturnedFragmentsOption tmpConstant = EFGFFragmenterReturnedFragmentsOption.valueOf(anOptionName);
        this.setReturnedFragmentsSetting(tmpConstant);
    }

    /**
     * Sets the returned fragments setting, defining whether only functional groups, only alkane fragments, or both should
     * be returned.
     *
     * @param anOption a constant from the EFGFFragmenterReturnedFragmentsOption enum
     * @throws NullPointerException if the given parameter is null
     */
    public void setReturnedFragmentsSetting(EFGFFragmenterReturnedFragmentsOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        this.returnedFragmentsSetting.set(anOption.name());
    }

    /**
     * Sets the cycle finder setting. The chosen cycle finder algorithm is used for aromaticity detection in
     * preprocessing together with the set electron donation model.
     *
     * @param anOptionName name of a constant from the CycleFinderOption enum
     * @throws NullPointerException if the given string is null
     * @throws IllegalArgumentException if the given string is not an enum constant name
     */
    public void setCycleFinderSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        CycleFinderOption tmpConstant = CycleFinderOption.valueOf(anOptionName);
        this.setCycleFinderSetting(tmpConstant);
    }

    /**
     * Sets the cycle finder setting. The chosen cycle finder algorithm is used for aromaticity detection in
     * preprocessing together with the set electron donation model.
     *
     * @param anOption a constant from the CycleFinderOption enum
     * @throws NullPointerException if the given parameter is null
     */
    public void setCycleFinderSetting(CycleFinderOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        this.cycleFinderSetting.set(anOption.name());
    }

    /**
     * Sets the filter single atoms setting. If true, molecules consisting of only one atom are filtered from the input
     * molecules and no functional groups determined for them.
     *
     * @param aBoolean true if single-atom molecules should not be fragmented; false otherwise
     */
    public void setFilterSingleAtomsSetting(boolean aBoolean) {
        this.filterSingleAtomsSetting.set(aBoolean);
    }
    //</editor-fold>
    //
    //<editor-fold desc="IMoleculeFragmenter methods">
    //without the empty line, the code folding does not work properly here...

    @Override
    public List<Property> settingsProperties() {
        return this.settings;
    }

    @Override
    public Map<String, String> getSettingNameToTooltipTextMap() {
        return this.settingNameTooltipTextMap;
    }

    @Override
    public String getFragmentationAlgorithmName() {
        return ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME;
    }

    @Override
    public String getFragmentSaturationSetting() {
        return this.fragmentSaturationSetting.get();
    }

    @Override
    public SimpleEnumConstantNameProperty fragmentSaturationSettingProperty() {
        return this.fragmentSaturationSetting;
    }

    @Override
    public FragmentSaturationOption getFragmentSaturationSettingConstant() {
        return FragmentSaturationOption.valueOf(this.fragmentSaturationSetting.get());
    }

    @Override
    public void setFragmentSaturationSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given saturation option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        FragmentSaturationOption tmpConstant = FragmentSaturationOption.valueOf(anOptionName);
        this.fragmentSaturationSetting.set(tmpConstant.name());
    }

    @Override
    public void setFragmentSaturationSetting(FragmentSaturationOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given saturation option is null.");
        this.fragmentSaturationSetting.set(anOption.name());
    }

    @Override
    public IMoleculeFragmenter copy() {
        ErtlFunctionalGroupsFinderFragmenter tmpCopy = new ErtlFunctionalGroupsFinderFragmenter();
        tmpCopy.setEnvironmentModeSetting(this.environmentModeSetting.get());
        tmpCopy.setCycleFinderSetting(this.cycleFinderSetting.get());
        tmpCopy.setElectronDonationModelSetting(this.electronDonationModelSetting.get());
        tmpCopy.setFragmentSaturationSetting(this.fragmentSaturationSetting.get());
        tmpCopy.setReturnedFragmentsSetting(this.returnedFragmentsSetting.get());
        tmpCopy.setFilterSingleAtomsSetting(this.filterSingleAtomsSetting.get());
        return tmpCopy;
    }

    @Override
    public void restoreDefaultSettings() {
        this.environmentModeSetting.set(ErtlFunctionalGroupsFinderFragmenter.ENVIRONMENT_MODE_OPTION_DEFAULT.name());
        //this.EFGFInstance is set in the method
        this.setErtlFGFInstance(FGEnvOption.valueOf(this.environmentModeSetting.get()));
        this.cycleFinderSetting.set(ErtlFunctionalGroupsFinderFragmenter.CYCLE_FINDER_OPTION_DEFAULT.name());
        this.setCycleFinderSetting(CycleFinderOption.valueOf(this.cycleFinderSetting.get()));
        this.electronDonationModelSetting.set(ErtlFunctionalGroupsFinderFragmenter.Electron_Donation_MODEL_OPTION_DEFAULT.name());
        //this.aromaticityModel is set in the method
        this.setAromaticityInstance(this.electronDonationInstance, this.cycleFinderInstance);
        this.fragmentSaturationSetting.set(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT.name());
        this.returnedFragmentsSetting.set(ErtlFunctionalGroupsFinderFragmenter.RETURNED_FRAGMENTS_OPTION_DEFAULT.name());
        this.filterSingleAtomsSetting.set(ErtlFunctionalGroupsFinderFragmenter.FILTER_SINGLE_ATOMS_OPTION_DEFAULT);
    }

    @Override
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule)
            throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        //<editor-fold desc="Parameter tests">
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        boolean tmpCanBeFragmented = this.canBeFragmented(aMolecule);
        if (!tmpCanBeFragmented) {
            throw new IllegalArgumentException("Given molecule cannot be fragmented but should be filtered or preprocessed first.");
        }
        //</editor-fold>
        IAtomContainer tmpMoleculeClone = aMolecule.clone();
        try {
            ErtlFunctionalGroupsFinderUtility.perceiveAtomTypesAndConfigureAtoms(tmpMoleculeClone);
            ErtlFunctionalGroupsFinderUtility.applyAromaticityDetection(tmpMoleculeClone, this.aromaticityModelInstance);
        } catch (CDKException anException) {
            this.logger.log(Level.WARNING, anException.toString(), anException);
            throw new IllegalArgumentException("Unexpected error at aromaticity detection: " + anException.toString());
        }
        int tmpInitialCapacityForIdToAtomMap = CollectionUtil.calculateInitialHashCollectionCapacity(tmpMoleculeClone.getAtomCount(), BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        HashMap<Integer, IAtom> tmpIdToAtomMap = new HashMap<>(tmpInitialCapacityForIdToAtomMap, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        for (int i = 0; i < tmpMoleculeClone.getAtomCount(); i++) {
            IAtom tmpAtom = tmpMoleculeClone.getAtom(i);
            tmpAtom.setProperty(ErtlFunctionalGroupsFinderFragmenter.INTERNAL_INDEX_PROPERTY_KEY, i);
            tmpIdToAtomMap.put(i, tmpAtom);
        }
        List<IAtomContainer> tmpFunctionalGroupFragments = null;
        List<IAtomContainer> tmpNonFGFragments = null;
        try {
            //generate FG fragments using EFGF
            if (this.environmentModeSetting.get().equals(FGEnvOption.NO_ENVIRONMENT.name())) {
                //extract only marked atoms, use implemented utility method from EFGFUtilities
                tmpFunctionalGroupFragments = ErtlFunctionalGroupsFinderUtility.findMarkedAtoms(tmpMoleculeClone);
            } else {
                //generalization or full environment, can both be handled by EFGF alone
                tmpFunctionalGroupFragments = this.ertlFGFInstance.find(tmpMoleculeClone, false);
            }
            if (!tmpFunctionalGroupFragments.isEmpty()) {
                for (IAtomContainer tmpFunctionalGroup : tmpFunctionalGroupFragments) {
                    //post-processing FG fragments
                    tmpFunctionalGroup.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                            ErtlFunctionalGroupsFinderFragmenter.FRAGMENT_CATEGORY_FUNCTIONAL_GROUP_VALUE);
                    if (this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION.name())) {
                        ChemUtil.saturateWithHydrogen(tmpFunctionalGroup);
                    }
                    ChemUtil.checkAndCorrectElectronConfiguration(tmpFunctionalGroup);
                    //FG fragments are removed from molecule to generate alkane fragments
                    if (this.returnedFragmentsSetting.get().equals(EFGFFragmenterReturnedFragmentsOption.ALL_FRAGMENTS.name())
                            || this.returnedFragmentsSetting.get().equals(EFGFFragmenterReturnedFragmentsOption.ONLY_ALKANE_FRAGMENTS.name())) {
                        for (IAtom tmpAtom : tmpFunctionalGroup.atoms()) {
                            //FG fragments contain new atoms added by EFGF, they must not be removed from the original molecule
                            if (!Objects.isNull(tmpAtom.getProperty(ErtlFunctionalGroupsFinderFragmenter.INTERNAL_INDEX_PROPERTY_KEY))) {
                                int tmpIndex = tmpAtom.getProperty("EFGFFragmenter.INDEX");
                                tmpMoleculeClone.removeAtom(tmpIdToAtomMap.get(tmpIndex));
                            }
                        }
                    }
                }
                //Partition unconnected alkane fragments in distinct atom containers
                if (this.returnedFragmentsSetting.get().equals(EFGFFragmenterReturnedFragmentsOption.ALL_FRAGMENTS.name())
                        || this.returnedFragmentsSetting.get().equals(EFGFFragmenterReturnedFragmentsOption.ONLY_ALKANE_FRAGMENTS.name())) {
                    if (!tmpMoleculeClone.isEmpty()) {
                        IAtomContainerSet tmpPartitionedMoietiesSet = ConnectivityChecker.partitionIntoMolecules(tmpMoleculeClone);
                        tmpNonFGFragments = new ArrayList<>(tmpPartitionedMoietiesSet.getAtomContainerCount());
                        for (IAtomContainer tmpContainer : tmpPartitionedMoietiesSet.atomContainers()) {
                            //post-processing of alkane fragments
                            tmpContainer.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                                    ErtlFunctionalGroupsFinderFragmenter.FRAGMENT_CATEGORY_ALKANE_VALUE);
                            if (this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION.name())) {
                                ChemUtil.saturateWithHydrogen(tmpContainer);
                            }
                            ChemUtil.checkAndCorrectElectronConfiguration(tmpContainer);
                            tmpNonFGFragments.add(tmpContainer);
                        }
                    } else {
                        tmpNonFGFragments = new ArrayList<>(0);
                    }
                }
            } else {
                //no FG identified
                List<IAtomContainer> tmpReturnList = new ArrayList<IAtomContainer>(1);
                if (this.returnedFragmentsSetting.get().equals(EFGFFragmenterReturnedFragmentsOption.ALL_FRAGMENTS.name())
                        || this.returnedFragmentsSetting.get().equals(EFGFFragmenterReturnedFragmentsOption.ONLY_ALKANE_FRAGMENTS.name())) {
                    tmpReturnList.add(0, tmpMoleculeClone);
                    tmpMoleculeClone.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                            ErtlFunctionalGroupsFinderFragmenter.FRAGMENT_CATEGORY_ALKANE_VALUE);
                }
                //if only FG should be returned (but none have been detected), this list is empty
                return tmpReturnList;
            }
        } catch(Exception anException) {
            throw new IllegalArgumentException("An error occurred during fragmentation: " + anException.toString() + " Molecule Name: " + aMolecule.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
        }
        List<IAtomContainer> tmpFragments;
        if (this.returnedFragmentsSetting.get().equals(EFGFFragmenterReturnedFragmentsOption.ALL_FRAGMENTS.name())) {
            tmpFragments = new ArrayList<IAtomContainer>(tmpFunctionalGroupFragments.size() + (tmpNonFGFragments == null ? 0 : tmpNonFGFragments.size()));
            tmpFragments.addAll(tmpFunctionalGroupFragments);
            tmpFragments.addAll(tmpNonFGFragments);
        } else if (this.returnedFragmentsSetting.get().equals(EFGFFragmenterReturnedFragmentsOption.ONLY_FUNCTIONAL_GROUPS.name())) {
            tmpFragments = new ArrayList<IAtomContainer>(tmpFunctionalGroupFragments.size());
            tmpFragments.addAll(tmpFunctionalGroupFragments);
        } else if (this.returnedFragmentsSetting.get().equals(EFGFFragmenterReturnedFragmentsOption.ONLY_ALKANE_FRAGMENTS.name())) {
            tmpFragments = new ArrayList<IAtomContainer>(tmpNonFGFragments.size());
            tmpFragments.addAll(tmpNonFGFragments);
        } else {
            throw new IllegalStateException("Unknown return fragments setting option has been set.");
        }
        return tmpFragments;
    }

    @Override
    public boolean shouldBeFiltered(IAtomContainer aMolecule) {
        if (Objects.isNull(aMolecule) || aMolecule.isEmpty()) {
            return true;
        }
        //throws NullpointerException if molecule is null
        return ErtlFunctionalGroupsFinderUtility.shouldBeFiltered(aMolecule, this.filterSingleAtomsSetting.get());
    }

    @Override
    public boolean shouldBePreprocessed(IAtomContainer aMolecule) throws NullPointerException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        //throws NullpointerException if molecule is null
        return ErtlFunctionalGroupsFinderUtility.shouldBePreprocessed(aMolecule);
    }

    @Override
    public boolean canBeFragmented(IAtomContainer aMolecule) throws NullPointerException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        boolean tmpShouldBeFiltered = this.shouldBeFiltered(aMolecule);
        boolean tmpShouldBePreprocessed = this.shouldBePreprocessed(aMolecule);
        if (tmpShouldBeFiltered || tmpShouldBePreprocessed) {
            return false;
        }
        //throws NullpointerException if molecule is null
        return ErtlFunctionalGroupsFinderUtility.isValidArgumentForFindMethod(aMolecule, this.filterSingleAtomsSetting.get());
    }

    @Override
    public IAtomContainer applyPreprocessing(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        boolean tmpShouldBeFiltered = this.shouldBeFiltered(aMolecule);
        if (tmpShouldBeFiltered) {
            throw new IllegalArgumentException("The given molecule cannot be preprocessed but should be filtered.");
        }
        if (!this.shouldBePreprocessed(aMolecule)) {
            return aMolecule.clone();
        }
        IAtomContainer tmpPreprocessedMolecule = aMolecule.clone();
        if (ErtlFunctionalGroupsFinderUtility.isStructureUnconnected(tmpPreprocessedMolecule)) {
            tmpPreprocessedMolecule = ErtlFunctionalGroupsFinderUtility.selectBiggestUnconnectedComponent(tmpPreprocessedMolecule);
        }
        if (ErtlFunctionalGroupsFinderUtility.isMoleculeCharged(tmpPreprocessedMolecule)) {
            try {
                ErtlFunctionalGroupsFinderUtility.neutralizeCharges(tmpPreprocessedMolecule);
            } catch (CDKException anException) {
                this.logger.log(Level.WARNING, anException.toString(), anException);
                throw new IllegalArgumentException("Unexpected error at aromaticity detection: " + anException.toString());
            }
        }
        if (Objects.isNull(tmpPreprocessedMolecule)) {
            throw new IllegalArgumentException("The given molecule cannot be preprocessed but should be filtered.");
        } else {
            return tmpPreprocessedMolecule;
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private methods">
    /**
     * Sets only the instance, not the property! So it is safe for the property to call this method when overriding set().
     */
    private void setAromaticityInstance(ElectronDonation anElectronDonation, CycleFinder aCycleFinder) throws NullPointerException {
        Objects.requireNonNull(anElectronDonation, "Given electron donation model is null.");
        Objects.requireNonNull(aCycleFinder, "Given cycle finder algorithm is null.");
        this.aromaticityModelInstance = new Aromaticity(anElectronDonation, aCycleFinder);
    }

    /**
     * Calling method needs to update the aromaticity model!
     */
    private void setCycleFinderInstance(CycleFinderOption anOption) throws NullPointerException {
        //Developer comment: the switch way is used instead of having the CycleFinder objects as variables of the enum constants
        // to not have static objects becoming bottlenecks in parallelization.
        Objects.requireNonNull(anOption, "Given option is null.");
        switch (anOption) {
            case ALL:
                this.cycleFinderInstance = Cycles.or(Cycles.all(), ErtlFunctionalGroupsFinderFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case MCB:
                this.cycleFinderInstance = Cycles.or(Cycles.mcb(), ErtlFunctionalGroupsFinderFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case RELEVANT:
                this.cycleFinderInstance = Cycles.or(Cycles.relevant(), ErtlFunctionalGroupsFinderFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case ESSENTIAL:
                this.cycleFinderInstance = Cycles.or(Cycles.essential(), ErtlFunctionalGroupsFinderFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case EDGE_SHORT:
                this.cycleFinderInstance = Cycles.or(Cycles.edgeShort(), ErtlFunctionalGroupsFinderFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case VERTEX_SHORT:
                this.cycleFinderInstance = Cycles.or(Cycles.vertexShort(), ErtlFunctionalGroupsFinderFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case TRIPLET_SHORT:
                this.cycleFinderInstance = Cycles.or(Cycles.tripletShort(), ErtlFunctionalGroupsFinderFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case CDK_AROMATIC_SET:
                this.cycleFinderInstance = Cycles.cdkAromaticSet();
                break;
            default:
                throw new IllegalArgumentException("Undefined cycle finder option.");
        }
    }

    /**
     * Calling method needs to update the aromaticity model!
     */
    private void setElectronDonationInstance(ElectronDonationModelOption anOption) throws NullPointerException {
        //Developer comment: the switch way is used instead of having the CycleFinder objects as variables of the enum constants
        // to not have static objects becoming bottlenecks in parallelization.
        Objects.requireNonNull(anOption, "Given option is null.");
        switch (anOption) {
            case CDK:
                this.electronDonationInstance = ElectronDonation.cdk();
                break;
            case DAYLIGHT:
                this.electronDonationInstance = ElectronDonation.daylight();
                break;
            case CDK_ALLOWING_EXOCYCLIC:
                this.electronDonationInstance = ElectronDonation.cdkAllowingExocyclic();
                break;
            case PI_BONDS:
                this.electronDonationInstance = ElectronDonation.piBonds();
                break;
            default:
                throw new IllegalArgumentException("Undefined electron donation model option.");
        }
    }

    /**
     * Sets only the instance, not the property! So it is safe for the property to call this method when overriding set().
     */
    private void setErtlFGFInstance(FGEnvOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        this.ertlFGFInstance = new ErtlFunctionalGroupsFinder(anOption.getAssociatedEFGFMode());
    }
    //</editor-fold>
}
