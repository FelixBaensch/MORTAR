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

import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.io.Importer;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.ChemUtil;
import de.unijena.cheminf.mortar.model.util.CollectionUtil;
import de.unijena.cheminf.mortar.model.util.IDisplayEnum;
import de.unijena.cheminf.mortar.model.util.SimpleIDisplayEnumConstantProperty;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;

import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.fragment.FunctionalGroupsFinder;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

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
    public static enum FGEnvOption implements IDisplayEnum {
        /**
         * Generalize environments of functional groups.
         */
        GENERALIZATION(
                Message.get("ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.Generalization.displayName"),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.Generalization.tooltip")),
        /**
         * Do not generalize but give the full environment of functional groups.
         */
        FULL_ENVIRONMENT(
                Message.get("ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.NoGeneralization.displayName"),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.NoGeneralization.tooltip")),
        /**
         * Return only the marked atoms of a functional group, no environment.
         */
        NO_ENVIRONMENT(
                Message.get("ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.OnlyMarkedAtoms.displayName"),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.OnlyMarkedAtoms.tooltip"));
        /**
         * Language-specific name for display in GUI.
         */
        private final String displayName;
        /**
         * Language-specific tooltip text for display in GUI.
         */
        private final String tooltip;
        /**
         * Constructor.
         *
         * @param aDisplayName display name
         * @param aTooltip tooltip text
         */
        private FGEnvOption(String aDisplayName, String aTooltip) {
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
    //<editor-fold desc="Enum EFGFFragmenterReturnedFragmentsOption">
    /**
     * Enum for defining which fragments should be returned by the fragmentation methods, only the functional groups,
     * only the alkane fragments, or both.
     */
    public static enum EFGFFragmenterReturnedFragmentsOption implements IDisplayEnum {
        /**
         * Option to return only the identified functional groups of a molecule after fragmentation.
         */
        ONLY_FUNCTIONAL_GROUPS(
                Message.get("ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption.OnlyFunctionalGroups.displayName"),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption.OnlyFunctionalGroups.tooltip")),
        /**
         * Option to return only the non-functional-group alkane fragments of a molecule after fragmentation.
         */
        ONLY_ALKANE_FRAGMENTS(
                Message.get("ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption.OnlyAlkanes.displayName"),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption.OnlyAlkanes.tooltip")),
        /**
         * Option to return both, functional groups and alkane fragments, after fragmentation.
         */
        ALL_FRAGMENTS(
                Message.get("ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption.All.displayName"),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption.All.tooltip"));
        /**
         * Language-specific name for display in GUI.
         */
        private final String displayName;
        /**
         * Language-specific tooltip text for display in GUI.
         */
        private final String tooltip;
        /**
         * Constructor.
         *
         * @param aDisplayName display name
         * @param aTooltip tooltip text
         */
        private EFGFFragmenterReturnedFragmentsOption(String aDisplayName, String aTooltip) {
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
     * Name of the algorithm used in this fragmenter.
     */
    public static final String ALGORITHM_NAME = "Ertl algorithm";

    /**
     * Default electron donation model for aromaticity detection.
     */
    public static final IMoleculeFragmenter.ElectronDonationModelOption Electron_Donation_MODEL_OPTION_DEFAULT =
            IMoleculeFragmenter.ElectronDonationModelOption.DAYLIGHT;

    /**
     * Default functional group environment option.
     */
    public static final ErtlFunctionalGroupsFinderFragmenter.FGEnvOption ENVIRONMENT_MODE_OPTION_DEFAULT =
            ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.GENERALIZATION;

    /**
     * Default returned fragments option.
     */
    public static final ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption RETURNED_FRAGMENTS_OPTION_DEFAULT
            = ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption.ALL_FRAGMENTS;

    /**
     * Default option for the cycle finder algorithm employed for aromaticity detection.
     */
    public static final IMoleculeFragmenter.CycleFinderOption CYCLE_FINDER_OPTION_DEFAULT =
            IMoleculeFragmenter.CycleFinderOption.CDK_AROMATIC_SET;

    /**
     * Default option for whether to filter single-atom molecules from inputs.
     */
    public static final boolean FILTER_SINGLE_ATOMS_OPTION_DEFAULT = true;

    /**
     * Default option for whether input restrictions (no metal, metalloids, pseudo atoms, charges or unconnected
     * structures) should be applied.
     */
    public static final boolean APPLY_INPUT_RESTRICTIONS_OPTION_DEFAULT = false;

    /**
     * Cycle finder algorithm that is used should the set option cause an IntractableException.
     */
    public static final CycleFinder AUXILIARY_CYCLE_FINDER = Cycles.cdkAromaticSet();

    /**
     * Functional group fragments will be assigned this value for the property with key
     * IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY.
     */
    public static final String FRAGMENT_CATEGORY_FUNCTIONAL_GROUP_VALUE = "EFGFFragmenter.FunctionalGroup";

    /**
     * Alkane fragments will be assigned this value for the property with key
     * IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY.
     */
    public static final String FRAGMENT_CATEGORY_ALKANE_VALUE = "EFGFFragmenter.Alkane";
    //</editor-fold>
    //
    //<editor-fold desc="Private variables">
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

    //note: since Java 21, the javadoc build complains about "double comments" when there is a comment
    // for the get() method of the property and the private property itself as well
    private final SimpleIDisplayEnumConstantProperty environmentModeSetting;

    private final SimpleIDisplayEnumConstantProperty electronDonationModelSetting;

    private final SimpleIDisplayEnumConstantProperty fragmentSaturationSetting;

    private final SimpleIDisplayEnumConstantProperty returnedFragmentsSetting;

    private final SimpleIDisplayEnumConstantProperty cycleFinderSetting;

    private final SimpleBooleanProperty filterSingleAtomsSetting;

    private final SimpleBooleanProperty applyInputRestrictionsSetting;

    /**
     * All settings of this fragmenter, encapsulated in JavaFX properties for binding in GUI.
     */
    private final List<Property<?>> settings;

    /**
     * Map to store pairs of {@literal <setting name, tooltip text>}.
     */
    private final HashMap<String, String> settingNameTooltipTextMap;

    /**
     * Map to store pairs of {@literal <setting name, display name>}.
     */
    private final HashMap<String, String> settingNameDisplayNameMap;

    /**
     * Instance of ErtlfFunctionalGroupsFinder class used to do the extraction of functional groups.
     */
    private FunctionalGroupsFinder ertlFGFInstance;

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(ErtlFunctionalGroupsFinderFragmenter.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="Constructor">
    /**
     * Constructor, all settings are initialised with their default values as declared in the respective public constants.
     */
    public ErtlFunctionalGroupsFinderFragmenter() {
        int tmpNumberOfSettingsForTooltipMapSize= 7;
        int tmpInitialCapacityForSettingNameTooltipTextMap = CollectionUtil.calculateInitialHashCollectionCapacity(
                tmpNumberOfSettingsForTooltipMapSize,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.settingNameTooltipTextMap = new HashMap<>(tmpInitialCapacityForSettingNameTooltipTextMap, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.settingNameDisplayNameMap = new HashMap<>(tmpInitialCapacityForSettingNameTooltipTextMap, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        //these names are for internal use, the language-specific display names for the GUI are stored in the map
        this.fragmentSaturationSetting = new SimpleIDisplayEnumConstantProperty(this, "Fragment saturation setting",
                IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT, IMoleculeFragmenter.FragmentSaturationOption.class) {
            @Override
            public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ErtlFunctionalGroupsFinderFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            anException.toString(),
                            anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        this.settingNameTooltipTextMap.put(this.fragmentSaturationSetting.getName(),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.fragmentSaturationSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.fragmentSaturationSetting.getName(),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.fragmentSaturationSetting.displayName"));
        this.setErtlFGFInstance(ErtlFunctionalGroupsFinderFragmenter.ENVIRONMENT_MODE_OPTION_DEFAULT);
        this.environmentModeSetting = new SimpleIDisplayEnumConstantProperty(this, "Environment mode setting",
                ErtlFunctionalGroupsFinderFragmenter.ENVIRONMENT_MODE_OPTION_DEFAULT, ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.class) {
            @Override
            public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ErtlFunctionalGroupsFinderFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            anException.toString(),
                            anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                //throws no exception if super.set() throws no exception
                ErtlFunctionalGroupsFinderFragmenter.this.setErtlFGFInstance((ErtlFunctionalGroupsFinderFragmenter.FGEnvOption) this.get());
            }
        };
        this.settingNameTooltipTextMap.put(this.environmentModeSetting.getName(),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.environmentModeSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.environmentModeSetting.getName(),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.environmentModeSetting.displayName"));
        //initialisation of EFGF instance
        this.setErtlFGFInstance((ErtlFunctionalGroupsFinderFragmenter.FGEnvOption) this.environmentModeSetting.get());
        this.returnedFragmentsSetting = new SimpleIDisplayEnumConstantProperty(this, "Returned fragments setting",
                ErtlFunctionalGroupsFinderFragmenter.RETURNED_FRAGMENTS_OPTION_DEFAULT, ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption.class) {
            @Override
            public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ErtlFunctionalGroupsFinderFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            anException.toString(),
                            anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        this.settingNameTooltipTextMap.put(this.returnedFragmentsSetting.getName(),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.returnedFragmentsSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.returnedFragmentsSetting.getName(),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.returnedFragmentsSetting.displayName"));
        //note: cycle finder and electron donation model have to be set prior to setting the aromaticity model!
        this.cycleFinderSetting = new SimpleIDisplayEnumConstantProperty(this, "Cycle finder algorithm setting",
                ErtlFunctionalGroupsFinderFragmenter.CYCLE_FINDER_OPTION_DEFAULT,
                IMoleculeFragmenter.CycleFinderOption.class) {
            @Override
            public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ErtlFunctionalGroupsFinderFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            anException.toString(),
                            anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                //throws no exception if super.set() throws no exception
                ErtlFunctionalGroupsFinderFragmenter.this.setCycleFinderInstance((IMoleculeFragmenter.CycleFinderOption) this.get());
                ErtlFunctionalGroupsFinderFragmenter.this.setAromaticityInstance(
                        ErtlFunctionalGroupsFinderFragmenter.this.electronDonationInstance,
                        ErtlFunctionalGroupsFinderFragmenter.this.cycleFinderInstance);
            }
        };
        this.settingNameTooltipTextMap.put(this.cycleFinderSetting.getName(),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.cycleFinderSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.cycleFinderSetting.getName(),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.cycleFinderSetting.displayName"));
        this.setCycleFinderInstance((IMoleculeFragmenter.CycleFinderOption) this.cycleFinderSetting.get());
        this.electronDonationModelSetting = new SimpleIDisplayEnumConstantProperty(this, "Electron donation model setting",
                ErtlFunctionalGroupsFinderFragmenter.Electron_Donation_MODEL_OPTION_DEFAULT,
                IMoleculeFragmenter.ElectronDonationModelOption.class) {
            @Override
            public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ErtlFunctionalGroupsFinderFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            anException.toString(),
                            anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                //throws no exception if super.set() throws no exception
                ErtlFunctionalGroupsFinderFragmenter.this.setElectronDonationInstance((IMoleculeFragmenter.ElectronDonationModelOption) this.get());
                ErtlFunctionalGroupsFinderFragmenter.this.setAromaticityInstance(
                        ErtlFunctionalGroupsFinderFragmenter.this.electronDonationInstance,
                        ErtlFunctionalGroupsFinderFragmenter.this.cycleFinderInstance);
            }
        };
        this.settingNameTooltipTextMap.put(this.electronDonationModelSetting.getName(),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.electronDonationModelSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.electronDonationModelSetting.getName(),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.electronDonationModelSetting.displayName"));
        this.setElectronDonationInstance((IMoleculeFragmenter.ElectronDonationModelOption) this.electronDonationModelSetting.get());
        this.setAromaticityInstance(
                ErtlFunctionalGroupsFinderFragmenter.this.electronDonationInstance,
                ErtlFunctionalGroupsFinderFragmenter.this.cycleFinderInstance
        );
        this.filterSingleAtomsSetting = new SimpleBooleanProperty(this, "Filter single atoms setting",
                ErtlFunctionalGroupsFinderFragmenter.FILTER_SINGLE_ATOMS_OPTION_DEFAULT);
        this.settingNameTooltipTextMap.put(this.filterSingleAtomsSetting.getName(),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.filterSingleAtomsSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.filterSingleAtomsSetting.getName(),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.filterSingleAtomsSetting.displayName"));
        this.applyInputRestrictionsSetting = new SimpleBooleanProperty(this, "Apply input restrictions setting",
                ErtlFunctionalGroupsFinderFragmenter.APPLY_INPUT_RESTRICTIONS_OPTION_DEFAULT);
        this.settingNameTooltipTextMap.put(this.applyInputRestrictionsSetting.getName(),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.applyInputRestrictionsSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.applyInputRestrictionsSetting.getName(),
                Message.get("ErtlFunctionalGroupsFinderFragmenter.applyInputRestrictionsSetting.displayName"));
        this.settings = new ArrayList<>(7);
        this.settings.add(this.fragmentSaturationSetting);
        this.settings.add(this.electronDonationModelSetting);
        this.settings.add(this.cycleFinderSetting);
        this.settings.add(this.environmentModeSetting);
        this.settings.add(this.returnedFragmentsSetting);
        this.settings.add(this.filterSingleAtomsSetting);
        this.settings.add(this.applyInputRestrictionsSetting);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties get">
    /**
     * Returns the currently set option for the environment mode setting.
     *
     * @return enum constant of the set option
     */
    public ErtlFunctionalGroupsFinderFragmenter.FGEnvOption getEnvironmentModeSetting() {
        return (ErtlFunctionalGroupsFinderFragmenter.FGEnvOption) this.environmentModeSetting.get();
    }

    /**
     * Returns the property object of the environment mode setting that can be used to configure this setting.
     *
     * @return property object of the environment mode setting
     */
    public SimpleIDisplayEnumConstantProperty environmentModeSettingProperty() {
        return this.environmentModeSetting;
    }

    /**
     * Returns the currently set option for the electron donation model setting used for
     * aromaticity detection together with the set cycle finder algorithm.
     *
     * @return enum constant of the set option
     */
    public IMoleculeFragmenter.ElectronDonationModelOption getElectronDonationModelSetting() {
        return (IMoleculeFragmenter.ElectronDonationModelOption) this.electronDonationModelSetting.get();
    }

    /**
     * Returns the property object of the electron donation model setting that can be used to configure this setting.
     *
     * @return property object of the electron donation model setting
     */
    public SimpleIDisplayEnumConstantProperty electronDonationModelSettingProperty() {
        return this.electronDonationModelSetting;
    }

    /**
     * Returns the currently set option for the returned fragments setting
     *
     * @return enum constant of the set option
     */
    public ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption getReturnedFragmentsSetting() {
        return (ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption) this.returnedFragmentsSetting.get();
    }

    /**
     * Returns the property object of the returned fragments setting that can be used to configure this setting.
     *
     * @return property object of the returned fragments setting
     */
    public SimpleIDisplayEnumConstantProperty returnedFragmentsSettingProperty() {
        return this.returnedFragmentsSetting;
    }

    /**
     * Returns the currently set option for the cycle finder setting used for aromaticity
     * detection together with the electron donation model setting.
     *
     * @return enum constant of the set option
     */
    public IMoleculeFragmenter.CycleFinderOption getCycleFinderSetting() {
        return (IMoleculeFragmenter.CycleFinderOption) this.cycleFinderSetting.get();
    }

    /**
     * Returns the property object of the cycle finder setting that can be used to configure this setting.
     *
     * @return property object of the cycle finder setting
     */
    public SimpleIDisplayEnumConstantProperty cycleFinderSettingProperty() {
        return this.cycleFinderSetting;
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

    /**
     * Returns the boolean value of the apply strict input restrictions setting.
     *
     * @return true if strict input restrictions are applied to the input molecules
     */
    public boolean getApplyInputRestrictionsSetting() {
        return this.applyInputRestrictionsSetting.get();
    }

    /**
     * Returns the property object of the apply strict input restrictions setting that can be used to configure this setting.
     *
     * @return property object of the apply strict input restrictions setting
     */
    public SimpleBooleanProperty applyInputRestrictionsSettingProperty() {
        return this.applyInputRestrictionsSetting;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties set">
    /**
     * Sets the environment mode setting defining whether the returned functional group fragments should have full environments,
     * generalized environments or no environments.
     *
     * @param anOption a constant from the FGEnvOption enum
     * @throws NullPointerException if the given parameter is null
     */
    public void setEnvironmentModeSetting(ErtlFunctionalGroupsFinderFragmenter.FGEnvOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        //synchronisation with EFGF instance done in overridden set() function of the property
        this.environmentModeSetting.set(anOption);
    }

    /**
     * Sets the electron donation model setting. The set electron donation model is used for aromaticity detection in
     * preprocessing together with the set cycle finder algorithm.
     *
     * @param anOption a constant from the IMoleculeFragmenter.ElectronDonationModelOption enum
     * @throws NullPointerException is the given parameter is null
     */
    public void setElectronDonationModelSetting(IMoleculeFragmenter.ElectronDonationModelOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        //synchronisation with aromaticity model instance done in overridden set() function of the property
        this.electronDonationModelSetting.set(anOption);
    }

    /**
     * Sets the returned fragments setting, defining whether only functional groups, only alkane fragments, or both should
     * be returned.
     *
     * @param anOption a constant from the EFGFFragmenterReturnedFragmentsOption enum
     * @throws NullPointerException if the given parameter is null
     */
    public void setReturnedFragmentsSetting(ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        this.returnedFragmentsSetting.set(anOption);
    }

    /**
     * Sets the cycle finder setting. The chosen cycle finder algorithm is used for aromaticity detection in
     * preprocessing together with the set electron donation model.
     *
     * @param anOption a constant from the IMoleculeFragmenter.CycleFinderOption enum
     * @throws NullPointerException if the given parameter is null
     */
    public void setCycleFinderSetting(IMoleculeFragmenter.CycleFinderOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        this.cycleFinderSetting.set(anOption);
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

    /**
     * Sets the apply strict input restrictions setting. If true, molecules containing metal, metalloid, or pseudo atoms,
     * formal charges, or multiple unconnected parts are filtered from the input
     * molecules and no functional groups are determined for them.
     *
     * @param aBoolean true if strict input restrictions should be applied; false otherwise
     */
    public void setApplyInputRestrictionsSetting(boolean aBoolean) {
        this.applyInputRestrictionsSetting.set(aBoolean);
    }
    //</editor-fold>
    //
    //<editor-fold desc="IMoleculeFragmenter methods">
    //without the empty line, the code folding does not work properly here...

    @Override
    public List<Property<?>> settingsProperties() {
        return this.settings;
    }

    @Override
    public Map<String, String> getSettingNameToTooltipTextMap() {
        return this.settingNameTooltipTextMap;
    }

    @Override
    public Map<String, String> getSettingNameToDisplayNameMap() {
        return this.settingNameDisplayNameMap;
    }

    @Override
    public String getFragmentationAlgorithmName() {
        return ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME;
    }

    @Override
    public String getFragmentationAlgorithmDisplayName() {
        return Message.get("ErtlFunctionalGroupsFinderFragmenter.displayName");
    }

    @Override
    public IMoleculeFragmenter.FragmentSaturationOption getFragmentSaturationSetting() {
        return (IMoleculeFragmenter.FragmentSaturationOption) this.fragmentSaturationSetting.get();
    }

    @Override
    public SimpleIDisplayEnumConstantProperty fragmentSaturationSettingProperty() {
        return this.fragmentSaturationSetting;
    }

    @Override
    public void setFragmentSaturationSetting(FragmentSaturationOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given saturation option is null.");
        this.fragmentSaturationSetting.set(anOption);
    }

    @Override
    public IMoleculeFragmenter copy() {
        ErtlFunctionalGroupsFinderFragmenter tmpCopy = new ErtlFunctionalGroupsFinderFragmenter();
        tmpCopy.setEnvironmentModeSetting((ErtlFunctionalGroupsFinderFragmenter.FGEnvOption) this.environmentModeSetting.get());
        tmpCopy.setCycleFinderSetting((IMoleculeFragmenter.CycleFinderOption) this.cycleFinderSetting.get());
        tmpCopy.setElectronDonationModelSetting((IMoleculeFragmenter.ElectronDonationModelOption) this.electronDonationModelSetting.get());
        tmpCopy.setFragmentSaturationSetting((IMoleculeFragmenter.FragmentSaturationOption) this.fragmentSaturationSetting.get());
        tmpCopy.setReturnedFragmentsSetting((ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption) this.returnedFragmentsSetting.get());
        tmpCopy.setFilterSingleAtomsSetting(this.filterSingleAtomsSetting.get());
        tmpCopy.setApplyInputRestrictionsSetting(this.applyInputRestrictionsSetting.get());
        return tmpCopy;
    }

    @Override
    public void restoreDefaultSettings() {
        this.environmentModeSetting.set(ErtlFunctionalGroupsFinderFragmenter.ENVIRONMENT_MODE_OPTION_DEFAULT);
        //this.EFGFInstance is set in the method
        this.setErtlFGFInstance(ErtlFunctionalGroupsFinderFragmenter.ENVIRONMENT_MODE_OPTION_DEFAULT);
        this.cycleFinderSetting.set(ErtlFunctionalGroupsFinderFragmenter.CYCLE_FINDER_OPTION_DEFAULT);
        this.setCycleFinderSetting(ErtlFunctionalGroupsFinderFragmenter.CYCLE_FINDER_OPTION_DEFAULT);
        this.electronDonationModelSetting.set(ErtlFunctionalGroupsFinderFragmenter.Electron_Donation_MODEL_OPTION_DEFAULT);
        //this.aromaticityModel is set in the method
        this.setAromaticityInstance(this.electronDonationInstance, this.cycleFinderInstance);
        this.fragmentSaturationSetting.set(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT);
        this.returnedFragmentsSetting.set(ErtlFunctionalGroupsFinderFragmenter.RETURNED_FRAGMENTS_OPTION_DEFAULT);
        this.filterSingleAtomsSetting.set(ErtlFunctionalGroupsFinderFragmenter.FILTER_SINGLE_ATOMS_OPTION_DEFAULT);
        this.applyInputRestrictionsSetting.set(ErtlFunctionalGroupsFinderFragmenter.APPLY_INPUT_RESTRICTIONS_OPTION_DEFAULT);
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
        List<IAtomContainer> tmpFunctionalGroupFragments;
        List<IAtomContainer> tmpNonFGFragments = null;
        try {
            //Applies the always necessary preprocessing for functional group detection. Atom types are set and aromaticity detected in the input molecule.
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpMoleculeClone);
            Aromaticity.clear(tmpMoleculeClone);
            this.aromaticityModelInstance.apply(tmpMoleculeClone);
            //generate FG fragments using EFGF
            tmpFunctionalGroupFragments = this.ertlFGFInstance.extract(tmpMoleculeClone, this.applyInputRestrictionsSetting.get());
            if (!tmpFunctionalGroupFragments.isEmpty()) {
                for (IAtomContainer tmpFunctionalGroup : tmpFunctionalGroupFragments) {
                    //post-processing FG fragments
                    tmpFunctionalGroup.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                            ErtlFunctionalGroupsFinderFragmenter.FRAGMENT_CATEGORY_FUNCTIONAL_GROUP_VALUE);
                    if (this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION)) {
                        ChemUtil.saturateWithHydrogen(tmpFunctionalGroup);
                    }
                    ChemUtil.checkAndCorrectElectronConfiguration(tmpFunctionalGroup);
                }
                if (this.returnedFragmentsSetting.get().equals(ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption.ALL_FRAGMENTS)
                        || this.returnedFragmentsSetting.get().equals(ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption.ONLY_ALKANE_FRAGMENTS)) {
                    //FG fragments are removed from molecule to generate alkane fragments
                    //note: only removes marked atoms, so atoms added as env C atoms to the FGs are duplicated
                    //also note: yes, we have to do the detection again using the indices-returning method, sadly...
                    int[] tmpFunctionalGroupIndices = new int[tmpMoleculeClone.getAtomCount()];
                    this.ertlFGFInstance.find(tmpFunctionalGroupIndices, tmpMoleculeClone);
                    //use map of index to atom because indices will change whenever an atom is removed
                    int tmpInitialCapacityForIdToAtomMap = CollectionUtil.calculateInitialHashCollectionCapacity(
                            tmpMoleculeClone.getAtomCount(),
                            BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
                    HashMap<Integer, IAtom> tmpIndexToAtomMap = new HashMap<>(
                                    tmpInitialCapacityForIdToAtomMap,
                                    BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
                    for (IAtom tmpAtom : tmpMoleculeClone.atoms()) {
                        tmpIndexToAtomMap.put(tmpAtom.getIndex(), tmpAtom);
                    }
                    //now, remove FG atoms from the molecule to generate alkane fragments
                    for (Map.Entry<Integer, IAtom> tmpEntry : tmpIndexToAtomMap.entrySet()) {
                        if (tmpFunctionalGroupIndices[tmpEntry.getKey()] != -1) {
                            tmpMoleculeClone.removeAtom(tmpEntry.getValue());
                        }
                    }
                    if (!tmpMoleculeClone.isEmpty()) {
                        //Partition unconnected alkane fragments in distinct atom containers
                        IAtomContainerSet tmpPartitionedMoietiesSet = ConnectivityChecker.partitionIntoMolecules(tmpMoleculeClone);
                        tmpNonFGFragments = new ArrayList<>(tmpPartitionedMoietiesSet.getAtomContainerCount());
                        for (IAtomContainer tmpContainer : tmpPartitionedMoietiesSet.atomContainers()) {
                            //post-processing of alkane fragments
                            tmpContainer.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                                    ErtlFunctionalGroupsFinderFragmenter.FRAGMENT_CATEGORY_ALKANE_VALUE);
                            if (this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION)) {
                                ChemUtil.saturateWithHydrogen(tmpContainer);
                            }
                            ChemUtil.checkAndCorrectElectronConfiguration(tmpContainer);
                            tmpNonFGFragments.add(tmpContainer);
                        }
                    } else {
                        // molecule clone is empty after FG removal, no alkane fragments
                        tmpNonFGFragments = new ArrayList<>(0);
                    }
                }
            } else {
                //no FG identified
                List<IAtomContainer> tmpReturnList = new ArrayList<>(1);
                if (this.returnedFragmentsSetting.get().equals(ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption.ALL_FRAGMENTS)
                        || this.returnedFragmentsSetting.get().equals(ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption.ONLY_ALKANE_FRAGMENTS)) {
                    tmpReturnList.addFirst(tmpMoleculeClone);
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
        if (this.returnedFragmentsSetting.get().equals(ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption.ALL_FRAGMENTS)) {
            tmpFragments = new ArrayList<>(tmpFunctionalGroupFragments.size() + (tmpNonFGFragments == null ? 0 : tmpNonFGFragments.size()));
            tmpFragments.addAll(tmpFunctionalGroupFragments);
            tmpFragments.addAll(tmpNonFGFragments);
        } else if (this.returnedFragmentsSetting.get().equals(ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption.ONLY_FUNCTIONAL_GROUPS)) {
            tmpFragments = new ArrayList<>(tmpFunctionalGroupFragments.size());
            tmpFragments.addAll(tmpFunctionalGroupFragments);
        } else if (this.returnedFragmentsSetting.get().equals(ErtlFunctionalGroupsFinderFragmenter.EFGFFragmenterReturnedFragmentsOption.ONLY_ALKANE_FRAGMENTS)) {
            if (tmpNonFGFragments != null) {
                tmpFragments = new ArrayList<>(tmpNonFGFragments.size());
                tmpFragments.addAll(tmpNonFGFragments);
            } else {
                tmpFragments = new ArrayList<>(0);
            }
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
        if (this.filterSingleAtomsSetting.get() && this.isAtomOrBondCountZero(aMolecule)) {
            return true;
        }
        if (this.applyInputRestrictionsSetting.get()) {
            return !FunctionalGroupsFinder.checkConstraints(aMolecule);
        }
        return false;
    }

    @Override
    public boolean shouldBePreprocessed(IAtomContainer aMolecule) throws NullPointerException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        return false;
    }

    @Override
    public boolean canBeFragmented(IAtomContainer aMolecule) throws NullPointerException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        boolean tmpShouldBeFiltered = this.shouldBeFiltered(aMolecule);
        boolean tmpShouldBePreprocessed = this.shouldBePreprocessed(aMolecule);
        return !(tmpShouldBeFiltered || tmpShouldBePreprocessed);
    }

    @Override
    public IAtomContainer applyPreprocessing(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        boolean tmpShouldBeFiltered = this.shouldBeFiltered(aMolecule);
        if (tmpShouldBeFiltered) {
            throw new IllegalArgumentException("The given molecule cannot be preprocessed but should be filtered.");
        }
        return aMolecule.clone();
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
    private void setCycleFinderInstance(IMoleculeFragmenter.CycleFinderOption anOption) throws NullPointerException {
        //Developer comment: the switch way is used instead of having the CycleFinder objects as variables of the enum constants
        // to not have static objects becoming bottlenecks in parallelization.
        Objects.requireNonNull(anOption, "Given option is null.");
        switch (anOption) {
            case IMoleculeFragmenter.CycleFinderOption.ALL:
                this.cycleFinderInstance = Cycles.or(Cycles.all(), ErtlFunctionalGroupsFinderFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case IMoleculeFragmenter.CycleFinderOption.MCB:
                this.cycleFinderInstance = Cycles.or(Cycles.mcb(), ErtlFunctionalGroupsFinderFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case IMoleculeFragmenter.CycleFinderOption.RELEVANT:
                this.cycleFinderInstance = Cycles.or(Cycles.relevant(), ErtlFunctionalGroupsFinderFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case IMoleculeFragmenter.CycleFinderOption.ESSENTIAL:
                this.cycleFinderInstance = Cycles.or(Cycles.essential(), ErtlFunctionalGroupsFinderFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case IMoleculeFragmenter.CycleFinderOption.EDGE_SHORT:
                this.cycleFinderInstance = Cycles.or(Cycles.edgeShort(), ErtlFunctionalGroupsFinderFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case IMoleculeFragmenter.CycleFinderOption.VERTEX_SHORT:
                this.cycleFinderInstance = Cycles.or(Cycles.vertexShort(), ErtlFunctionalGroupsFinderFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case IMoleculeFragmenter.CycleFinderOption.TRIPLET_SHORT:
                this.cycleFinderInstance = Cycles.or(Cycles.tripletShort(), ErtlFunctionalGroupsFinderFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case IMoleculeFragmenter.CycleFinderOption.CDK_AROMATIC_SET:
                this.cycleFinderInstance = Cycles.cdkAromaticSet();
                break;
            default:
                throw new IllegalArgumentException("Undefined cycle finder option.");
        }
    }

    /**
     * Calling method needs to update the aromaticity model!
     */
    private void setElectronDonationInstance(IMoleculeFragmenter.ElectronDonationModelOption anOption) throws NullPointerException {
        //Developer comment: the switch way is used instead of having the CycleFinder objects as variables of the enum constants
        // to not have static objects becoming bottlenecks in parallelization.
        Objects.requireNonNull(anOption, "Given option is null.");
        switch (anOption) {
            case IMoleculeFragmenter.ElectronDonationModelOption.CDK:
                this.electronDonationInstance = ElectronDonation.cdk();
                break;
            case IMoleculeFragmenter.ElectronDonationModelOption.DAYLIGHT:
                this.electronDonationInstance = ElectronDonation.daylight();
                break;
            case IMoleculeFragmenter.ElectronDonationModelOption.CDK_ALLOWING_EXOCYCLIC:
                this.electronDonationInstance = ElectronDonation.cdkAllowingExocyclic();
                break;
            case IMoleculeFragmenter.ElectronDonationModelOption.PI_BONDS:
                this.electronDonationInstance = ElectronDonation.piBonds();
                break;
            default:
                throw new IllegalArgumentException("Undefined electron donation model option.");
        }
    }

    /**
     * Sets only the instance, not the property! So it is safe for the property to call this method when overriding set().
     */
    private void setErtlFGFInstance(ErtlFunctionalGroupsFinderFragmenter.FGEnvOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        switch (anOption) {
            case ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.GENERALIZATION
                    -> this.ertlFGFInstance = FunctionalGroupsFinder.withGeneralEnvironment();
            case ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.FULL_ENVIRONMENT
                    -> this.ertlFGFInstance = FunctionalGroupsFinder.withFullEnvironment();
            case ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.NO_ENVIRONMENT
                    -> this.ertlFGFInstance = FunctionalGroupsFinder.withNoEnvironment();
        }
    }
    /**
     * Checks whether the atom count or bond count of the given molecule is zero. The FunctionalGroupsFinder
     * would still accept these molecules, but it is not recommended to pass them on (simply makes not much sense).
     *
     * @param aMolecule the molecule to check
     * @return true, if the atom or bond count of the molecule is zero
     * @throws NullPointerException if the given molecule is 'null'
     */
    private boolean isAtomOrBondCountZero(IAtomContainer aMolecule) throws NullPointerException {
        Objects.requireNonNull(aMolecule, "Given molecule is 'null'.");
        int tmpAtomCount = aMolecule.getAtomCount();
        int tmpBondCount = aMolecule.getBondCount();
        return (tmpAtomCount == 0 || tmpBondCount == 0);
    }
    //</editor-fold>
}
