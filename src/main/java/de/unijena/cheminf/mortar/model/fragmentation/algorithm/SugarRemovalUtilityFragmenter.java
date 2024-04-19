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

package de.unijena.cheminf.mortar.model.fragmentation.algorithm;

import de.unijena.cheminf.deglycosylation.SugarRemovalUtility;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.ChemUtil;
import de.unijena.cheminf.mortar.model.util.CollectionUtil;
import de.unijena.cheminf.mortar.model.util.IDisplayEnum;
import de.unijena.cheminf.mortar.model.util.SimpleIDisplayEnumConstantProperty;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper class that makes the
 * <a href="https://doi.org/10.1186/s13321-020-00467-y">Sugar Removal Utility for in-silico deglycosylation</a>
 * available in MORTAR. For additional documentation see the linked journal article or the original
 * SugarRemovalUtility class.
 *
 * @author Jonas Schaub
 * @version 1.0.0.1
 */
public class SugarRemovalUtilityFragmenter implements IMoleculeFragmenter {
    //<editor-fold desc="Enum SugarTypeToRemoveOption">
    /**
     * Enum for options concerning the type of sugars to remove or detect.
     */
    public static enum SugarTypeToRemoveOption implements IDisplayEnum {
        /**
         * Remove/detect only circular sugars.
         */
        CIRCULAR(Message.get("SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption.Circular.displayName"),
                Message.get("SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption.Circular.tooltip")),
        /**
         * Remove/detect only linear sugars.
         */
        LINEAR(Message.get("SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption.Linear.displayName"),
                Message.get("SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption.Linear.tooltip")),
        /**
         * Remove/detect both circular and linear sugars.
         */
        CIRCULAR_AND_LINEAR(Message.get("SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption.Both.displayName"),
                Message.get("SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption.Both.tooltip"));
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
        private SugarTypeToRemoveOption(String aDisplayName, String aTooltip) {
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
    //<editor-fold desc="Enum SRUFragmenterReturnedFragmentsOption">
    /**
     * Enum for defining which fragments should be returned by the fragmentation methods, only the sugars,
     * only the aglycones, or both.
     */
    public static enum SRUFragmenterReturnedFragmentsOption implements IDisplayEnum {
        /**
         * Option to return only the identified sugar moieties of a molecule after fragmentation.
         */
        ONLY_SUGAR_MOIETIES(Message.get("SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption.OnlySugars.displayName"),
                Message.get("SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption.OnlySugars.tooltip")),
        /**
         * Option to return only the aglycone of a molecule after fragmentation.
         */
        ONLY_AGLYCONE(Message.get("SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption.OnlyAglycone.displayName"),
                Message.get("SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption.OnlyAglycone.tooltip")),
        /**
         * Option to return both, aglycone and sugar moieties, after fragmentation.
         */
        ALL_FRAGMENTS(Message.get("SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption.All.displayName"),
                Message.get("SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption.All.tooltip"));
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
        private SRUFragmenterReturnedFragmentsOption(String aDisplayName, String aTooltip) {
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
    //<editor-fold desc="Enum SRUFragmenterPreservationModeOption">
    /**
     * Enum with options for how to determine whether a substructure that gets disconnected from the molecule during the
     * removal of a sugar moiety should be preserved or can get removed along with the sugar.
     * Wraps the enum PreservationModeOption from SugarRemovalUtility to be able to add display name and tooltip here.
     */
    public static enum SRUFragmenterPreservationModeOption implements IDisplayEnum {
        /**
         * Specifies that all structures should be preserved. Note that if this option is combined with the removal of
         * only terminal moieties, even the smallest attached structure will prevent the removal of a sugar. The most
         * important consequence is that circular sugars with any hydroxy groups will not be removed because these are
         * not considered as part of the sugar moiety.
         */
        ALL(SugarRemovalUtility.PreservationModeOption.ALL,
                Message.get("SugarRemovalUtilityFragmenter.SRUFragmenterPreservationModeOption.All.displayName"),
                Message.get("SugarRemovalUtilityFragmenter.SRUFragmenterPreservationModeOption.All.tooltip")),
        /**
         * Specifies that whether a structure is worth preserving will be judged by its heavy atom count. The default
         * threshold to preserve a structure is set to 5 heavy atoms (inclusive).
         */
        HEAVY_ATOM_COUNT(SugarRemovalUtility.PreservationModeOption.HEAVY_ATOM_COUNT,
                Message.get("SugarRemovalUtilityFragmenter.SRUFragmenterPreservationModeOption.HAC.displayName"),
                Message.get("SugarRemovalUtilityFragmenter.SRUFragmenterPreservationModeOption.HAC.tooltip")),
        /**
         * Specifies that whether a structure is worth preserving will be judged by its molecular weight. The default
         * threshold to preserve a structure is set to 60 Da (= 5 carbon atoms, inclusive).
         */
        MOLECULAR_WEIGHT (SugarRemovalUtility.PreservationModeOption.MOLECULAR_WEIGHT,
                Message.get("SugarRemovalUtilityFragmenter.SRUFragmenterPreservationModeOption.MW.displayName"),
                Message.get("SugarRemovalUtilityFragmenter.SRUFragmenterPreservationModeOption.MW.tooltip"));
        /**
         * Wrapped enum constant from the analogous SRU enum.
         */
        private final SugarRemovalUtility.PreservationModeOption wrappedOption;
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
         * @param anOption the wrapped enum constant from the original enum
         * @param aDisplayName display name
         * @param aTooltip tooltip text
         */
        private SRUFragmenterPreservationModeOption(SugarRemovalUtility.PreservationModeOption anOption, String aDisplayName, String aTooltip) {
            this.wrappedOption = anOption;
            this.displayName = aDisplayName;
            this.tooltip = aTooltip;
        }
        /**
         * Returns the enum constant from the SRU PreservationModeOption enum that is wrapped in this instance.
         *
         * @return wrapped constant
         */
        public SugarRemovalUtility.PreservationModeOption getWrappedSRUPreservationMode() {
            return this.wrappedOption;
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
    public static final String ALGORITHM_NAME = "Sugar Removal Utility";

    /**
     * Aglycone fragments will be assigned this value for the property with key IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY.
     */
    public static final String FRAGMENT_CATEGORY_DEGLYCOSYLATED_CORE_VALUE = "SRUFragmenter.DeglycosylatedCore";

    /**
     * Sugar moiety fragments will be assigned this value for the property with key IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY.
     */
    public static final String FRAGMENT_CATEGORY_SUGAR_MOIETY_VALUE = "SRUFragmenter.SugarMoiety";

    /**
     * Default option for the sugar type to remove setting.
     */
    public static final SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption SUGAR_TYPE_TO_REMOVE_OPTION_DEFAULT =
            SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption.CIRCULAR_AND_LINEAR;

    /**
     * Default returned fragments option.
     */
    public static final SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption RETURNED_FRAGMENTS_OPTION_DEFAULT =
            SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption.ALL_FRAGMENTS;

    /**
     * Default preservation mode setting.
     */
    public static final SugarRemovalUtilityFragmenter.SRUFragmenterPreservationModeOption PRESERVATION_MODE_DEFAULT =
            SRUFragmenterPreservationModeOption.HEAVY_ATOM_COUNT;
    //</editor-fold>
    //
    //<editor-fold desc="Private final variables">
    /**
     * Instance of the Sugar Removal Utility used internally to detect and remove the sugar moieties.
     */
    private final SugarRemovalUtility sugarRUInstance;

    private final SimpleIDisplayEnumConstantProperty returnedFragmentsSetting;

    private final SimpleIDisplayEnumConstantProperty sugarTypeToRemoveSetting;

    /**
     * A property that has a constant from the IMoleculeFragmenter.FragmentSaturationOption enum as value.
     */
    private final SimpleIDisplayEnumConstantProperty fragmentSaturationSetting;

    private final SimpleBooleanProperty detectCircularSugarsOnlyWithGlycosidicBondSetting;

    private final SimpleBooleanProperty removeOnlyTerminalSugarsSetting;

    private final SimpleIDisplayEnumConstantProperty preservationModeSetting;

    private final SimpleIntegerProperty preservationModeThresholdSetting;

    private final SimpleBooleanProperty detectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting;

    private final SimpleDoubleProperty exocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting;

    private final SimpleBooleanProperty detectLinearSugarsInRingsSetting;

    private final SimpleIntegerProperty linearSugarCandidateMinimumSizeSetting;

    private final SimpleIntegerProperty linearSugarCandidateMaximumSizeSetting;

    private final SimpleBooleanProperty detectLinearAcidicSugarsSetting;

    private final SimpleBooleanProperty detectSpiroRingsAsCircularSugarsSetting;

    private final SimpleBooleanProperty detectCircularSugarsWithKetoGroupsSetting;

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
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(SugarRemovalUtilityFragmenter.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="Constructor">
    /**
     * Constructor, all settings are initialised with their default values as declared in the respective public constants.
     */
    public SugarRemovalUtilityFragmenter() {
        this.sugarRUInstance = new SugarRemovalUtility(SilentChemObjectBuilder.getInstance());
        int tmpNumberOfSettings = 15;
        this.settings = new ArrayList<>(tmpNumberOfSettings);
        int tmpInitialCapacityForSettingNameTooltipTextMap = CollectionUtil.calculateInitialHashCollectionCapacity(
                tmpNumberOfSettings,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.settingNameTooltipTextMap = new HashMap<>(tmpInitialCapacityForSettingNameTooltipTextMap, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.settingNameDisplayNameMap = new HashMap<>(tmpInitialCapacityForSettingNameTooltipTextMap, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.returnedFragmentsSetting = new SimpleIDisplayEnumConstantProperty(this, "Returned fragments setting",
                SugarRemovalUtilityFragmenter.RETURNED_FRAGMENTS_OPTION_DEFAULT, SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption.class) {
            @Override
            public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    SugarRemovalUtilityFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            anException.toString(),
                            anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        this.settings.add(this.returnedFragmentsSetting);
        this.settingNameTooltipTextMap.put(this.returnedFragmentsSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.returnedFragmentsSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.returnedFragmentsSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.returnedFragmentsSetting.displayName"));
        this.fragmentSaturationSetting = new SimpleIDisplayEnumConstantProperty(this, "Fragment saturation setting",
                IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT, IMoleculeFragmenter.FragmentSaturationOption.class) {
            @Override
            public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    SugarRemovalUtilityFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            anException.toString(),
                            anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        this.settings.add(this.fragmentSaturationSetting);
        this.settingNameTooltipTextMap.put(this.fragmentSaturationSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.fragmentSaturationSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.fragmentSaturationSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.fragmentSaturationSetting.displayName"));
        this.sugarTypeToRemoveSetting = new SimpleIDisplayEnumConstantProperty(this, "Sugar type to remove setting",
                SugarRemovalUtilityFragmenter.SUGAR_TYPE_TO_REMOVE_OPTION_DEFAULT,
                SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption.class) {
            @Override
            public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    SugarRemovalUtilityFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            anException.toString(),
                            anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        this.settings.add(this.sugarTypeToRemoveSetting);
        this.settingNameTooltipTextMap.put(this.sugarTypeToRemoveSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.sugarTypeToRemoveSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.sugarTypeToRemoveSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.sugarTypeToRemoveSetting.displayName"));
        this.detectCircularSugarsOnlyWithGlycosidicBondSetting = new SimpleBooleanProperty(this,
                "Detect circular sugars only with glycosidic bond setting",
                this.sugarRUInstance.areOnlyCircularSugarsWithOGlycosidicBondDetected()) {
            @Override
            public void set(boolean newValue) {
                //throws no exceptions
                SugarRemovalUtilityFragmenter.this.sugarRUInstance.setDetectCircularSugarsOnlyWithOGlycosidicBondSetting(newValue);
                super.set(newValue);
            }
        };
        this.settings.add(this.detectCircularSugarsOnlyWithGlycosidicBondSetting);
        this.settingNameTooltipTextMap.put(this.detectCircularSugarsOnlyWithGlycosidicBondSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.detectCircularSugarsOnlyWithGlycosidicBondSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.detectCircularSugarsOnlyWithGlycosidicBondSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.detectCircularSugarsOnlyWithGlycosidicBondSetting.displayName"));
        this.removeOnlyTerminalSugarsSetting = new SimpleBooleanProperty(this, "Remove only terminal sugars setting",
                this.sugarRUInstance.areOnlyTerminalSugarsRemoved()) {
            @Override
            public void set(boolean newValue) {
                //throws no exceptions
                SugarRemovalUtilityFragmenter.this.sugarRUInstance.setRemoveOnlyTerminalSugarsSetting(newValue);
                super.set(newValue);
            }
        };
        this.settings.add(this.removeOnlyTerminalSugarsSetting);
        this.settingNameTooltipTextMap.put(this.removeOnlyTerminalSugarsSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.removeOnlyTerminalSugarsSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.removeOnlyTerminalSugarsSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.removeOnlyTerminalSugarsSetting.displayName"));
        this.preservationModeSetting = new SimpleIDisplayEnumConstantProperty(this, "Preservation mode setting",
                SugarRemovalUtilityFragmenter.PRESERVATION_MODE_DEFAULT, SugarRemovalUtilityFragmenter.SRUFragmenterPreservationModeOption.class) {
            @Override
            public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    SugarRemovalUtilityFragmenter.this.sugarRUInstance.setPreservationModeSetting(
                            ((SugarRemovalUtilityFragmenter.SRUFragmenterPreservationModeOption) newValue).getWrappedSRUPreservationMode());
                    //when the preservation mode is changed, the threshold is set to the default value of the chosen mode internally within the SRU!
                    SugarRemovalUtilityFragmenter.this.preservationModeThresholdSetting.set(
                            SugarRemovalUtilityFragmenter.this.sugarRUInstance.getPreservationModeThresholdSetting());
                } catch (IllegalArgumentException | NullPointerException anException) {
                    SugarRemovalUtilityFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            anException.toString(),
                            anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                super.set(newValue);
            }
        };
        this.settings.add(this.preservationModeSetting);
        this.settingNameTooltipTextMap.put(this.preservationModeSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.preservationModeSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.preservationModeSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.preservationModeSetting.displayName"));
        this.preservationModeThresholdSetting = new SimpleIntegerProperty(this, "Preservation mode threshold setting",
                this.sugarRUInstance.getPreservationModeThresholdSetting()) {
            @Override
            public void set(int newValue) throws IllegalArgumentException{
                try {
                    //throws IllegalArgumentException
                    SugarRemovalUtilityFragmenter.this.sugarRUInstance.setPreservationModeThresholdSetting(newValue);
                }catch(IllegalArgumentException anException){
                    SugarRemovalUtilityFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            anException.toString(),
                            anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                super.set(newValue);
            }
        };
        this.settings.add(this.preservationModeThresholdSetting);
        this.settingNameTooltipTextMap.put(this.preservationModeThresholdSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.preservationModeThresholdSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.preservationModeThresholdSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.preservationModeThresholdSetting.displayName"));
        this.detectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting = new SimpleBooleanProperty(this,
                "Detect circular sugars only with enough exocyclic oxygen atoms setting",
                this.sugarRUInstance.areOnlyCircularSugarsWithEnoughExocyclicOxygenAtomsDetected()) {
            @Override
            public void set(boolean newValue) {
                //throws no exceptions
                SugarRemovalUtilityFragmenter.this.sugarRUInstance.setDetectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting(newValue);
                super.set(newValue);
            }
        };
        this.settings.add(this.detectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting);
        this.settingNameTooltipTextMap.put(this.detectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.detectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.detectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.detectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting.displayName"));
        this.exocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting = new SimpleDoubleProperty(this,
                "Exocyclic oxygen atoms to atoms in ring ratio threshold setting",
                this.sugarRUInstance.getExocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting()) {
            @Override
            public void set(double newValue) throws IllegalArgumentException {
                try {
                    //throws IllegalArgumentException
                    SugarRemovalUtilityFragmenter.this.sugarRUInstance.setExocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting(newValue);
                } catch (IllegalArgumentException anException) {
                    SugarRemovalUtilityFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            anException.toString(),
                            anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                super.set(newValue);
            }
        };
        this.settings.add(this.exocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting);
        this.settingNameTooltipTextMap.put(this.exocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.exocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.exocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.exocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting.displayName"));
        this.detectLinearSugarsInRingsSetting = new SimpleBooleanProperty(this, "Detect linear sugars in rings setting",
                this.sugarRUInstance.areLinearSugarsInRingsDetected()) {
            @Override
            public void set(boolean newValue) {
                //throws no exceptions
                SugarRemovalUtilityFragmenter.this.sugarRUInstance.setDetectLinearSugarsInRingsSetting(newValue);
                super.set(newValue);
            }
        };
        this.settings.add(this.detectLinearSugarsInRingsSetting);
        this.settingNameTooltipTextMap.put(this.detectLinearSugarsInRingsSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.detectLinearSugarsInRingsSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.detectLinearSugarsInRingsSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.detectLinearSugarsInRingsSetting.displayName"));
        this.linearSugarCandidateMinimumSizeSetting = new SimpleIntegerProperty(this,
                "Linear sugar candidate minimum size setting",
                this.sugarRUInstance.getLinearSugarCandidateMinSizeSetting()) {
            @Override
            public void set(int newValue) {
                try {
                    //throws IllegalArgumentException
                    SugarRemovalUtilityFragmenter.this.sugarRUInstance.setLinearSugarCandidateMinSizeSetting(newValue);
                } catch (IllegalArgumentException anException) {
                    SugarRemovalUtilityFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            anException.toString(),
                            anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                super.set(newValue);
            }
        };
        this.settings.add(this.linearSugarCandidateMinimumSizeSetting);
        this.settingNameTooltipTextMap.put(this.linearSugarCandidateMinimumSizeSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.linearSugarCandidateMinimumSizeSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.linearSugarCandidateMinimumSizeSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.linearSugarCandidateMinimumSizeSetting.displayName"));
        this.linearSugarCandidateMaximumSizeSetting = new SimpleIntegerProperty(this,
                "Linear sugar candidate maximum size setting",
                this.sugarRUInstance.getLinearSugarCandidateMaxSizeSetting()) {
            @Override
            public void set(int newValue) {
                try {
                    //throws IllegalArgumentException
                    SugarRemovalUtilityFragmenter.this.sugarRUInstance.setLinearSugarCandidateMaxSizeSetting(newValue);
                } catch (IllegalArgumentException anException) {
                    SugarRemovalUtilityFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            anException.toString(),
                            anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                super.set(newValue);
            }
        };
        this.settings.add(this.linearSugarCandidateMaximumSizeSetting);
        this.settingNameTooltipTextMap.put(this.linearSugarCandidateMaximumSizeSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.linearSugarCandidateMaximumSizeSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.linearSugarCandidateMaximumSizeSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.linearSugarCandidateMaximumSizeSetting.displayName"));
        this.detectLinearAcidicSugarsSetting = new SimpleBooleanProperty(this,
                "Detect linear acidic sugars setting",
                this.sugarRUInstance.areLinearAcidicSugarsDetected()) {
            @Override
            public void set(boolean newValue) {
                //throws no exceptions
                SugarRemovalUtilityFragmenter.this.sugarRUInstance.setDetectLinearAcidicSugarsSetting(newValue);
                super.set(newValue);
            }
        };
        this.settings.add(this.detectLinearAcidicSugarsSetting);
        this.settingNameTooltipTextMap.put(this.detectLinearAcidicSugarsSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.detectLinearAcidicSugarsSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.detectLinearAcidicSugarsSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.detectLinearAcidicSugarsSetting.displayName"));
        this.detectSpiroRingsAsCircularSugarsSetting = new SimpleBooleanProperty(this,
                "Detect spiro rings as circular sugars setting",
                this.sugarRUInstance.areSpiroRingsDetectedAsCircularSugars()) {
            @Override
            public void set(boolean newValue) {
                //throws no exceptions
                SugarRemovalUtilityFragmenter.this.sugarRUInstance.setDetectSpiroRingsAsCircularSugarsSetting(newValue);
                super.set(newValue);
            }
        };
        this.settings.add(this.detectSpiroRingsAsCircularSugarsSetting);
        this.settingNameTooltipTextMap.put(this.detectSpiroRingsAsCircularSugarsSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.detectSpiroRingsAsCircularSugarsSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.detectSpiroRingsAsCircularSugarsSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.detectSpiroRingsAsCircularSugarsSetting.displayName"));
        this.detectCircularSugarsWithKetoGroupsSetting = new SimpleBooleanProperty(this,
                "Detect circular sugars with keto groups setting",
                this.sugarRUInstance.areCircularSugarsWithKetoGroupsDetected()) {
            @Override
            public void set(boolean newValue) {
                //throws no exceptions
                SugarRemovalUtilityFragmenter.this.sugarRUInstance.setDetectCircularSugarsWithKetoGroupsSetting(newValue);
                super.set(newValue);
            }
        };
        this.settings.add(this.detectCircularSugarsWithKetoGroupsSetting);
        this.settingNameTooltipTextMap.put(this.detectCircularSugarsWithKetoGroupsSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.detectCircularSugarsWithKetoGroupsSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.detectCircularSugarsWithKetoGroupsSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.detectCircularSugarsWithKetoGroupsSetting.displayName"));
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties get">
    /**
     * Returns the currently set option for the returned fragments setting.
     *
     * @return enum constant of the set option
     */
    public SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption getReturnedFragmentsSetting() {
        return (SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption) this.returnedFragmentsSetting.get();
    }

    /**
     * Returns the property object of the returned fragments setting that can be used to configure this setting.
     * A property that has a constant from SRUFragmenterReturnedFragmentsOption enum as value.
     *
     * @return property object of the returned fragments setting
     */
    public SimpleIDisplayEnumConstantProperty returnedFragmentsSettingProperty() {
        return this.returnedFragmentsSetting;
    }

    /**
     * Returns the currently set option for the sugar type to remove setting.
     *
     * @return enum constant of the set option
     */
    public SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption getSugarTypeToRemoveSetting() {
        return (SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption) this.sugarTypeToRemoveSetting.get();
    }

    /**
     * Returns the property object of the sugar type to remove setting that can be used to configure this setting.
     * A property that has a constant from SugarTypeToRemoveOption enum as value.
     *
     * @return property object of the sugar type to remove setting
     */
    public SimpleIDisplayEnumConstantProperty sugarTypeToRemoveSettingProperty() {
        return this.sugarTypeToRemoveSetting;
    }

    /**
     * Returns the current state of the detect circular sugars only with glycosidic bond setting.
     *
     * @return true if circular sugars should be detected/removed only if they have a glycosidic bond
     */
    public boolean getDetectCircularSugarsOnlyWithGlycosidicBondSetting() {
        return this.detectCircularSugarsOnlyWithGlycosidicBondSetting.get();
    }

    /**
     * Returns the property object of the detect circular sugars only with glycosidic bond setting that can be used to
     * configure this setting.
     *
     * @return property object of the detect circular sugars only with glycosidic bond setting
     */
    public SimpleBooleanProperty detectCircularSugarsOnlyWithGlycosidicBondSettingProperty() {
        return this.detectCircularSugarsOnlyWithGlycosidicBondSetting;
    }

    /**
     * Returns the current state of the remove only terminal sugars setting.
     *
     * @return true if only terminal sugars should be detected/removed
     */
    public boolean getRemoveOnlyTerminalSugarsSetting() {
        return this.removeOnlyTerminalSugarsSetting.get();
    }

    /**
     * Returns the property object of the remove only terminal sugars setting that can be used to configure this setting.
     *
     * @return property object of the remove only terminal sugars setting
     */
    public SimpleBooleanProperty removeOnlyTerminalSugarsSettingProperty() {
        return this.removeOnlyTerminalSugarsSetting;
    }

    /**
     * Returns the currently set option for the preservation mode setting.
     *
     * @return enum constant of the set option
     */
    public SugarRemovalUtilityFragmenter.SRUFragmenterPreservationModeOption getPreservationModeSetting() {
        return (SugarRemovalUtilityFragmenter.SRUFragmenterPreservationModeOption) this.preservationModeSetting.get();
    }

    /**
     * Returns the property object of the preservation mode setting that can be used to configure this setting.
     * It has a constant from the SugarRemovalUtility.PreservationModeOption enum as value.
     *
     * @return property object of the preservation mode setting
     */
    public SimpleIDisplayEnumConstantProperty preservationModeSettingProperty() {
        return this.preservationModeSetting;
    }

    /**
     * Returns the current value of the preservation mode threshold setting.
     *
     * @return value of preservation mode threshold setting
     */
    public int getPreservationModeThresholdSetting() {
        return this.preservationModeThresholdSetting.get();
    }

    /**
     * Returns the property object of the preservation mode threshold setting that can be used to configure this setting.
     *
     * @return property object of the preservation mode threshold setting
     */
    public SimpleIntegerProperty preservationModeThresholdSettingProperty() {
        return this.preservationModeThresholdSetting;
    }

    /**
     * Returns the current state of the detect circular sugars only with enough exocyclic oxygen atoms setting.
     *
     * @return true if circular sugars should be detected/removed only if they have a sufficient number of exocyclic
     * oxygen atoms connected to the central ring
     */
    public boolean getDetectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting() {
        return this.detectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting.get();
    }

    /**
     * Returns the property object of the detect circular sugars only with enough exocyclic oxygen atoms setting
     * that can be used to configure this setting.
     *
     * @return property object of the detect circular sugars only with enough exocyclic oxygen atoms setting
     */
    public SimpleBooleanProperty detectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSettingProperty() {
        return this.detectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting;
    }

    /**
     * Returns the current value of the exocyclic oxygen atoms to atoms in ring ratio threshold setting.
     *
     * @return value of exocyclic oxygen atoms to atoms in ring ratio threshold setting
     */
    public double getExocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting() {
        return this.exocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting.get();
    }

    /**
     * Returns the property object of the exocyclic oxygen atoms to atoms in ring ratio threshold setting that can be
     * used to configure this setting.
     *
     * @return property object of the exocyclic oxygen atoms to atoms in ring ratio threshold setting
     */
    public SimpleDoubleProperty exocyclicOxygenAtomsToAtomsInRingRatioThresholdSettingProperty() {
        return this.exocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting;
    }

    /**
     * Returns the current state of the detect linear sugars in rings setting.
     *
     * @return true if linear sugars in rings should be detected/removed
     */
    public boolean getDetectLinearSugarsInRingsSetting() {
        return this.detectLinearSugarsInRingsSetting.get();
    }

    /**
     * Returns the property object of the detect linear sugars in rings setting that can be used to configure this setting.
     *
     * @return property object of the detect linear sugars in rings setting
     */
    public SimpleBooleanProperty detectLinearSugarsInRingsSettingProperty() {
        return this.detectLinearSugarsInRingsSetting;
    }

    /**
     * Returns the current value of the linear sugar candidate minimum size setting.
     *
     * @return value of linear sugar candidate minimum size setting
     */
    public int getLinearSugarCandidateMinimumSizeSetting() {
        return this.linearSugarCandidateMinimumSizeSetting.get();
    }

    /**
     * Returns the property object of the linear sugar candidate minimum size setting that can be used to configure this
     * setting.
     *
     * @return property object of the linear sugar candidate minimum size setting.
     */
    public SimpleIntegerProperty linearSugarCandidateMinimumSizeSettingProperty() {
        return this.linearSugarCandidateMinimumSizeSetting;
    }

    /**
     * Returns the current value of the linear sugar candidate maximum size setting.
     *
     * @return value of linear sugar candidate maximum size setting
     */
    public int getLinearSugarCandidateMaximumSizeSetting() {
        return this.linearSugarCandidateMaximumSizeSetting.get();
    }

    /**
     * Returns the property object of the linear sugar candidate maximum size setting that can be used to configure this
     * setting.
     *
     * @return property object of the linear sugar candidate maximum size setting.
     */
    public SimpleIntegerProperty linearSugarCandidateMaximumSizeSettingProperty() {
        return this.linearSugarCandidateMaximumSizeSetting;
    }

    /**
     * Returns the current state of the detect linear acidic sugars setting.
     *
     * @return true if linear acidic sugars should be detected/removed
     */
    public boolean getDetectLinearAcidicSugarsSetting() {
        return this.detectLinearAcidicSugarsSetting.get();
    }

    /**
     * Returns the property object of the detect linear acidic sugars setting that can be used to configure this setting.
     *
     * @return property object of the detect linear acidic sugars setting
     */
    public SimpleBooleanProperty detectLinearAcidicSugarsSettingProperty() {
        return this.detectLinearAcidicSugarsSetting;
    }

    /**
     * Returns the current state of the detect spiro rings as circular sugars setting.
     *
     * @return true if spiro rings should be considered when detecting sugar rings
     */
    public boolean getDetectSpiroRingsAsCircularSugarsSetting() {
        return this.detectSpiroRingsAsCircularSugarsSetting.get();
    }

    /**
     * Returns the property object of the detect spiro rings as circular sugars setting that can be used to configure
     * this setting.
     *
     * @return property object of the detect spiro rings as circular sugars setting
     */
    public SimpleBooleanProperty detectSpiroRingsAsCircularSugarsSettingProperty() {
        return this.detectSpiroRingsAsCircularSugarsSetting;
    }

    /**
     * Returns the current state of the detect circular sugars with keto groups setting.
     *
     * @return true if potential sugar rings with keto groups should be considered when detecting circular sugars
     */
    public boolean getDetectCircularSugarsWithKetoGroupsSetting() {
        return this.detectCircularSugarsWithKetoGroupsSetting.get();
    }

    /**
     * Returns the property object of the detect circular sugars with keto groups setting that can be used to configure
     * this setting.
     *
     * @return property object of the detect circular sugars with keto groups setting
     */
    public SimpleBooleanProperty detectCircularSugarsWithKetoGroupsSettingProperty() {
        return this.detectCircularSugarsWithKetoGroupsSetting;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties set">
    /**
     * Sets the returned fragments setting, defining whether only sugar moieties, only the aglycone, or both should
     * be returned.
     *
     * @param anOption a constant from the SRUFragmenterReturnedFragmentsOption enum
     * @throws NullPointerException if the given parameter is null
     */
    public void setReturnedFragmentsSetting(SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        this.returnedFragmentsSetting.set(anOption);
    }

    /**
     * Sets the sugar type to remove setting, defining whether only circular, only linear, or both kinds of sugar
     * moieties should be detected/removed.
     *
     * @param aSugarTypeToRemoveOption a constant from the SugarTypeToRemoveOption enum
     * @throws NullPointerException if the given parameter is null
     */
    public void setSugarTypeToRemoveSetting(SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption aSugarTypeToRemoveOption) throws NullPointerException {
        Objects.requireNonNull(aSugarTypeToRemoveOption, "Given type of sugars to remove is null.");
        this.sugarTypeToRemoveSetting.set(aSugarTypeToRemoveOption);
    }

    /**
     * Sets the setting defining whether circular sugars should only be detected/removed if they have an O-glycosidic bond.
     *
     * @param aBoolean true, if on circular sugar moieties with a glycosidic bonds should be removed
     */
    public void setDetectCircularSugarsOnlyWithGlycosidicBondSetting(boolean aBoolean) {
        //synchronisation with SRU instance done in overridden set() function of the property
        this.detectCircularSugarsOnlyWithGlycosidicBondSetting.set(aBoolean);
    }

    /**
     * Sets the setting defining whether only terminal sugar moieties should be detected/removed.
     *
     * @param aBoolean true, if only terminal sugars should be detected/removed
     */
    public void setRemoveOnlyTerminalSugarsSetting(boolean aBoolean) {
        //synchronisation with SRU instance done in overridden set() function of the property
        this.removeOnlyTerminalSugarsSetting.set(aBoolean);
    }

    /**
     * Sets the preservation mode setting, defining what molecular characteristic should be considered when judging
     * whether a fragment is 'big enough' to be kept and not discarded.
     *
     * @param anOption a constant from the SugarRemovalUtility.PreservationModeOption enum
     * @throws NullPointerException if the given parameter is null
     */
    public void setPreservationModeSetting(SugarRemovalUtilityFragmenter.SRUFragmenterPreservationModeOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        //synchronisation with SRU instance done in overridden set() function of the property
        this.preservationModeSetting.set(anOption);
    }

    /**
     * Sets the preservation mode setting, defining how 'big' a fragment has to be in order to be kept and not discarded. E.g.,
     * if the preservation mode is set to HEAVY_ATOM_COUNT, the threshold defines how many heavy atoms a fragment needs
     * to have to be considered as 'big enough'.
     *
     * @param aThreshold a new threshold, e.g. a number of heavy atoms
     * @throws IllegalArgumentException if the preservation mode is currently set to preserve all structures or the
     * threshold is negative
     */
    public void setPreservationModeThresholdSetting(int aThreshold) throws IllegalArgumentException {
        //parameter test, conversion, and synchronisation with SRU instance in overridden set() method of the property
        this.preservationModeThresholdSetting.set(aThreshold);
    }

    /**
     * Sets the setting defining whether circular sugar candidates should only be detected/removed if they have a
     * sufficient number of attached, exocyclic, single-bonded oxygen atoms.
     *
     * @param aBoolean true, if only circular sugars with a sufficient number of exocyclic oxygen atoms should be
     *                 detected/removed
     */
    public void setDetectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting(boolean aBoolean) {
        //synchronisation with SRU instance in overridden set() method
        this.detectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting.set(aBoolean);
    }

    /**
     * Sets the exocyclic oxygen atoms to atoms in ring ratio threshold setting, defining how many attached, exocyclic
     * single-bonded oxygen atoms a circular sugar candidate needs to have in relation to its ring size to be
     * detected as a sugar moiety.
     *
     * @param aThreshold the new ratio threshold
     * @throws IllegalArgumentException if the given number is infinite, 'NaN' or smaller than 0 or if the ratio is not
     * evaluated under the current settings and a non-zero value is passed
     */
    public void setExocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting(double aThreshold) throws IllegalArgumentException {
        //synchronisation with SRU instance and parameter test done in overridden set() method
        this.exocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting.set(aThreshold);
    }

    /**
     * Sets the detect linear sugars in rings setting, defining whether substructures of rings that match the SRU's
     * linear sugar patterns are detected as linear sugars.
     *
     * @param aBoolean true if substructures of rings should be considered in linear sugar detection
     */
    public void setDetectLinearSugarsInRingsSetting(boolean aBoolean) {
        //synchronisation with SRU instance done in overridden set() method
        this.detectLinearSugarsInRingsSetting.set(aBoolean);
    }

    /**
     * Sets the linear sugar candidate minimum size setting, defining the minimum number of carbon atoms a linear sugar
     * candidate must have in order to be detected as a sugar moiety.
     *
     * @param aMinSize the new minimum size (inclusive) of linear sugars detected, interpreted as carbon atom count
     * @throws IllegalArgumentException if the given size is smaller than one
     */
    public void setLinearSugarCandidateMinimumSizeSetting(int aMinSize) throws IllegalArgumentException {
        //parameter test, conversion to int, and synchronisation with SRU instance done in overridden set() method
        this.linearSugarCandidateMinimumSizeSetting.set(aMinSize);
    }

    /**
     * Sets the linear sugar candidate maximum size setting, defining the maximum number of carbon atoms a linear sugar
     * candidate can have in order to be detected as a sugar moiety.
     *
     * @param aMaxSize the new maximum size (inclusive) of linear sugars detected, interpreted as carbon atom count
     * @throws IllegalArgumentException if the given size is smaller than one
     */
    public void setLinearSugarCandidateMaximumSizeSetting(int aMaxSize) throws IllegalArgumentException {
        //parameter test, conversion to int, and synchronisation with SRU instance done in overridden set() method
        this.linearSugarCandidateMaximumSizeSetting.set(aMaxSize);
    }

    /**
     * Sets the detect linear acidic sugars setting, defining whether patterns for linear sugar acids should also be
     * included in the initial linear sugar detection.
     *
     * @param aBoolean true, if linear acidic sugars should also be detected
     */
    public void setDetectLinearAcidicSugarsSetting(boolean aBoolean) {
        //synchronisation with SRU instance done in overridden set() method
        this.detectLinearAcidicSugarsSetting.set(aBoolean);
    }

    /**
     * Sets the detect spiro rings as circular sugars setting, defining whether spiro rings systems should also be
     * considered at circular sugar detection.
     *
     * @param aBoolean true, if spiro rings should be detectable as circular sugars
     */
    public void setDetectSpiroRingsAsCircularSugarsSetting(boolean aBoolean) {
        //synchronisation with SRU instance done in overridden set() method
        this.detectSpiroRingsAsCircularSugarsSetting.set(aBoolean);
    }

    /**
     * Sets the detect circular sugars with keto groups setting, defining whether circular sugar candidates with keto
     * groups should be considered at circular sugar detection.
     *
     * @param aBoolean true, if circular sugars with keto groups should be detected
     */
    public void setDetectCircularSugarsWithKetoGroupsSetting(boolean aBoolean) {
        //synchronisation with SRU instance done in overridden set() method
        this.detectCircularSugarsWithKetoGroupsSetting.set(aBoolean);
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
        return SugarRemovalUtilityFragmenter.ALGORITHM_NAME;
    }

    @Override
    public String getFragmentationAlgorithmDisplayName() {
        return Message.get("SugarRemovalUtilityFragmenter.displayName");
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
        SugarRemovalUtilityFragmenter tmpCopy = new SugarRemovalUtilityFragmenter();
        tmpCopy.setReturnedFragmentsSetting((SRUFragmenterReturnedFragmentsOption) this.returnedFragmentsSetting.get());
        tmpCopy.setSugarTypeToRemoveSetting((SugarTypeToRemoveOption) this.sugarTypeToRemoveSetting.get());
        tmpCopy.setFragmentSaturationSetting((FragmentSaturationOption) this.fragmentSaturationSetting.get());
        tmpCopy.setDetectCircularSugarsOnlyWithGlycosidicBondSetting(this.detectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting.get());
        tmpCopy.setRemoveOnlyTerminalSugarsSetting(this.removeOnlyTerminalSugarsSetting.get());
        tmpCopy.setPreservationModeSetting((SRUFragmenterPreservationModeOption) this.preservationModeSetting.get());
        tmpCopy.setPreservationModeThresholdSetting(this.preservationModeThresholdSetting.get());
        tmpCopy.setDetectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting(this.detectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting.get());
        tmpCopy.setExocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting(this.exocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting.get());
        tmpCopy.setDetectLinearSugarsInRingsSetting(this.detectLinearSugarsInRingsSetting.get());
        tmpCopy.setLinearSugarCandidateMinimumSizeSetting(this.linearSugarCandidateMinimumSizeSetting.get());
        tmpCopy.setLinearSugarCandidateMaximumSizeSetting(this.linearSugarCandidateMaximumSizeSetting.get());
        tmpCopy.setDetectLinearAcidicSugarsSetting(this.detectLinearAcidicSugarsSetting.get());
        tmpCopy.setDetectSpiroRingsAsCircularSugarsSetting(this.detectSpiroRingsAsCircularSugarsSetting.get());
        tmpCopy.setDetectCircularSugarsWithKetoGroupsSetting(this.detectCircularSugarsWithKetoGroupsSetting.get());
        return tmpCopy;
    }

    @Override
    public void restoreDefaultSettings() {
        this.returnedFragmentsSetting.set(SugarRemovalUtilityFragmenter.RETURNED_FRAGMENTS_OPTION_DEFAULT);
        this.sugarTypeToRemoveSetting.set(SugarRemovalUtilityFragmenter.SUGAR_TYPE_TO_REMOVE_OPTION_DEFAULT);
        this.fragmentSaturationSetting.set(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT);
        this.sugarRUInstance.restoreDefaultSettings();
        this.detectCircularSugarsOnlyWithGlycosidicBondSetting.set(this.sugarRUInstance.areOnlyCircularSugarsWithOGlycosidicBondDetected());
        this.removeOnlyTerminalSugarsSetting.set(this.sugarRUInstance.areOnlyTerminalSugarsRemoved());
        this.preservationModeSetting.set(SugarRemovalUtilityFragmenter.PRESERVATION_MODE_DEFAULT);
        this.preservationModeThresholdSetting.set(this.sugarRUInstance.getPreservationModeThresholdSetting());
        this.detectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting.set(this.sugarRUInstance.areOnlyCircularSugarsWithEnoughExocyclicOxygenAtomsDetected());
        this.exocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting.set(this.sugarRUInstance.getExocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting());
        this.detectLinearSugarsInRingsSetting.set(this.sugarRUInstance.areLinearSugarsInRingsDetected());
        this.linearSugarCandidateMinimumSizeSetting.set(this.sugarRUInstance.getLinearSugarCandidateMinSizeSetting());
        this.linearSugarCandidateMaximumSizeSetting.set(this.sugarRUInstance.getLinearSugarCandidateMaxSizeSetting());
        this.detectLinearAcidicSugarsSetting.set(this.sugarRUInstance.areLinearAcidicSugarsDetected());
        this.detectSpiroRingsAsCircularSugarsSetting.set(this.sugarRUInstance.areSpiroRingsDetectedAsCircularSugars());
        this.detectCircularSugarsWithKetoGroupsSetting.set(this.sugarRUInstance.areCircularSugarsWithKetoGroupsDetected());
    }

    @Override
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        if (aMolecule.isEmpty()) {
            List<IAtomContainer> tmpReturnList = new ArrayList<>(1);
            tmpReturnList.addFirst(aMolecule.clone());
            aMolecule.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                    SugarRemovalUtilityFragmenter.FRAGMENT_CATEGORY_DEGLYCOSYLATED_CORE_VALUE);
            return tmpReturnList;
        }
        boolean tmpCanBeFragmented = this.canBeFragmented(aMolecule);
        if (!tmpCanBeFragmented) {
            throw new IllegalArgumentException("Given molecule cannot be fragmented but should be filtered or preprocessed first.");
        }
        List<IAtomContainer> tmpFragments;
        SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption tmpOption = (SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption) this.sugarTypeToRemoveSetting.get();
        try {
            tmpFragments = switch (tmpOption) {
                case SugarTypeToRemoveOption.CIRCULAR ->
                        this.sugarRUInstance.removeAndReturnCircularSugars(aMolecule, true);
                case SugarTypeToRemoveOption.LINEAR ->
                        this.sugarRUInstance.removeAndReturnLinearSugars(aMolecule, true);
                case SugarTypeToRemoveOption.CIRCULAR_AND_LINEAR ->
                        this.sugarRUInstance.removeAndReturnCircularAndLinearSugars(aMolecule, true);
                default ->
                        throw new IllegalStateException("Unexpected value: " + this.sugarTypeToRemoveSetting.get());
            };
        } catch (IllegalArgumentException | CloneNotSupportedException anException) {
            throw new IllegalArgumentException("An error occurred during fragmentation: " + anException.toString());
        }
        //post-processing of aglycone, it is always saturated with implicit hydrogen atoms (might be empty)
        IAtomContainer tmpAglycone = tmpFragments.getFirst();
        tmpAglycone.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                SugarRemovalUtilityFragmenter.FRAGMENT_CATEGORY_DEGLYCOSYLATED_CORE_VALUE);
        boolean tmpSugarsWereDetected = (tmpFragments.size() > 1);
        if (this.returnedFragmentsSetting.get().equals(SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption.ALL_FRAGMENTS)
                || this.returnedFragmentsSetting.get().equals(SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption.ONLY_AGLYCONE)) {
            if (!tmpAglycone.isEmpty()) {
                if (!ConnectivityChecker.isConnected(tmpAglycone)) {
                    List<IAtomContainer> tmpAglyconeFragments = SugarRemovalUtility.partitionAndSortUnconnectedFragments(tmpAglycone);
                    for (IAtomContainer tmpAglyconeFragment : tmpAglyconeFragments) {
                        tmpAglyconeFragment.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                                SugarRemovalUtilityFragmenter.FRAGMENT_CATEGORY_DEGLYCOSYLATED_CORE_VALUE);
                    }
                    tmpFragments.removeFirst();
                    tmpFragments.addAll(0, tmpAglyconeFragments);
                }
            } else {
                tmpFragments.removeFirst();
            }
            //else: only sugars are returned, dispose of aglycone
        } else {
            tmpFragments.removeFirst();
        }
        //sugars were detected, postprocessing
        if (tmpSugarsWereDetected) {
            if (this.returnedFragmentsSetting.get().equals(SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption.ALL_FRAGMENTS)
                    || this.returnedFragmentsSetting.get().equals(SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption.ONLY_SUGAR_MOIETIES)) {
                for (IAtomContainer tmpSugarFragment : tmpFragments) {
                    if (!Objects.isNull(tmpSugarFragment.getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY))
                            && tmpSugarFragment.getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY)
                            .equals(SugarRemovalUtilityFragmenter.FRAGMENT_CATEGORY_DEGLYCOSYLATED_CORE_VALUE)) {
                        continue;
                    }
                    if (Objects.isNull(tmpSugarFragment.getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY))) {
                        tmpSugarFragment.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                                SugarRemovalUtilityFragmenter.FRAGMENT_CATEGORY_SUGAR_MOIETY_VALUE);
                    }
                    try {
                        if (this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION)) {
                            ChemUtil.saturateWithHydrogen(tmpSugarFragment);
                        }
                        ChemUtil.checkAndCorrectElectronConfiguration(tmpSugarFragment);
                    } catch (CDKException aCDKException) {
                        Logger.getLogger(SugarRemovalUtilityFragmenter.class.getName()).log(Level.WARNING, "Fragment saturation failed.");
                    }
                }
            //else: only aglycone is returned, dispose of sugars
            } else {
                for (int i = 0; i < tmpFragments.size(); i++) {
                    IAtomContainer tmpFragment = tmpFragments.get(i);
                    if (Objects.isNull(tmpFragment.getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY))
                            || !tmpFragment.getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY)
                            .equals(SugarRemovalUtilityFragmenter.FRAGMENT_CATEGORY_DEGLYCOSYLATED_CORE_VALUE)) {
                                tmpFragments.remove(i);
                                i--;
                    }
                }
            }
        }
        return tmpFragments;
    }

    @Override
    public boolean shouldBeFiltered(IAtomContainer aMolecule) {
        return (Objects.isNull(aMolecule) || aMolecule.isEmpty());
    }

    @Override
    public boolean shouldBePreprocessed(IAtomContainer aMolecule) throws NullPointerException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        if (this.sugarRUInstance.areOnlyTerminalSugarsRemoved()) {
            boolean tmpIsConnected = ConnectivityChecker.isConnected(aMolecule);
            return !tmpIsConnected;
        }
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
        if (!this.shouldBePreprocessed(aMolecule)) {
            return aMolecule.clone();
        }
        if (this.sugarRUInstance.areOnlyTerminalSugarsRemoved()) {
            boolean tmpIsConnected = ConnectivityChecker.isConnected(aMolecule);
            if (!tmpIsConnected) {
                return SugarRemovalUtility.selectBiggestUnconnectedFragment(aMolecule.clone());
            }
        }
        return aMolecule.clone();
    }
    //</editor-fold>
}
