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

/**
 * TODO:
 * -
 */

import de.unijena.cheminf.deglycosylation.SugarRemovalUtility;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.ChemUtil;
import de.unijena.cheminf.mortar.model.util.CollectionUtil;
import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;

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
    public static enum SugarTypeToRemoveOption {
        /**
         * Remove/detect only circular sugars.
         */
        CIRCULAR,

        /**
         * Remove/detect only linear sugars.
         */
        LINEAR,

        /**
         * Remove/detect both circular and linear sugars.
         */
        CIRCULAR_AND_LINEAR;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Enum SRUFragmenterReturnedFragmentsOption">
    /**
     * Enum for defining which fragments should be returned by the fragmentation methods, only the sugars,
     * only the aglycones, or both.
     */
    public static enum SRUFragmenterReturnedFragmentsOption {
        /**
         * Option to return only the identified sugar moieties of a molecule after fragmentation.
         */
        ONLY_SUGAR_MOIETIES,

        /**
         * Option to return only the aglycone of a molecule after fragmentation.
         */
        ONLY_AGLYCONE,

        /**
         * Option to return both, aglycone and sugar moieties, after fragmentation.
         */
        ALL_FRAGMENTS;
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
    public static final SugarTypeToRemoveOption SUGAR_TYPE_TO_REMOVE_OPTION_DEFAULT = SugarTypeToRemoveOption.CIRCULAR_AND_LINEAR;

    /**
     * Default returned fragments option.
     */
    public static final SRUFragmenterReturnedFragmentsOption RETURNED_FRAGMENTS_OPTION_DEFAULT = SRUFragmenterReturnedFragmentsOption.ALL_FRAGMENTS;
    //</editor-fold>
    //
    //<editor-fold desc="Private variables">
    /**
     * Instance of the Sugar Removal Utility used internally to detect and remove the sugar moieties.
     */
    private SugarRemovalUtility sugarRUInstance;
    //</editor-fold>
    //<editor-fold desc="Private final variables">

    private final SimpleEnumConstantNameProperty returnedFragmentsSetting;

    private final SimpleEnumConstantNameProperty sugarTypeToRemoveSetting;

    /**
     * A property that has a constant name from the IMoleculeFragmenter.FragmentSaturationOption enum as value.
     */
    private final SimpleEnumConstantNameProperty fragmentSaturationSetting;

    private final SimpleBooleanProperty detectCircularSugarsOnlyWithGlycosidicBondSetting;

    private final SimpleBooleanProperty removeOnlyTerminalSugarsSetting;

    private final SimpleEnumConstantNameProperty preservationModeSetting;

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
     * Logger of this class.
     */
    private final Logger logger = Logger.getLogger(SugarRemovalUtilityFragmenter.class.getName());
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
        this.returnedFragmentsSetting = new SimpleEnumConstantNameProperty(this, "Returned fragments setting",
                SugarRemovalUtilityFragmenter.RETURNED_FRAGMENTS_OPTION_DEFAULT.name(), SugarRemovalUtilityFragmenter.SRUFragmenterReturnedFragmentsOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    SugarRemovalUtilityFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        this.settings.add(this.returnedFragmentsSetting);
        this.settingNameTooltipTextMap.put(this.returnedFragmentsSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.returnedFragmentsSetting.tooltip"));
        this.fragmentSaturationSetting = new SimpleEnumConstantNameProperty(this, "Fragment saturation setting",
                IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT.name(), IMoleculeFragmenter.FragmentSaturationOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    SugarRemovalUtilityFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        this.settings.add(this.fragmentSaturationSetting);
        this.settingNameTooltipTextMap.put(this.fragmentSaturationSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.fragmentSaturationSetting.tooltip"));
        this.sugarTypeToRemoveSetting = new SimpleEnumConstantNameProperty(this, "Sugar type to remove setting",
                SugarRemovalUtilityFragmenter.SUGAR_TYPE_TO_REMOVE_OPTION_DEFAULT.name(),
                SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    SugarRemovalUtilityFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        this.settings.add(this.sugarTypeToRemoveSetting);
        this.settingNameTooltipTextMap.put(this.sugarTypeToRemoveSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.sugarTypeToRemoveSetting.tooltip"));
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
        this.preservationModeSetting = new SimpleEnumConstantNameProperty(this, "Preservation mode setting",
                this.sugarRUInstance.getPreservationModeSetting().name(), SugarRemovalUtility.PreservationModeOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //valueOf() throws IllegalArgumentException
                    SugarRemovalUtility.PreservationModeOption tmpEnumConstant = SugarRemovalUtility.PreservationModeOption.valueOf(newValue);
                    SugarRemovalUtilityFragmenter.this.sugarRUInstance.setPreservationModeSetting(tmpEnumConstant);
                    //when the preservation mode is changed, the threshold is set to the default value of the chosen mode internally within the SRU!
                    SugarRemovalUtilityFragmenter.this.preservationModeThresholdSetting.set(
                            SugarRemovalUtilityFragmenter.this.sugarRUInstance.getPreservationModeThresholdSetting());
                } catch (IllegalArgumentException | NullPointerException anException) {
                    SugarRemovalUtilityFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                super.set(newValue);
            }
        };
        this.settings.add(this.preservationModeSetting);
        this.settingNameTooltipTextMap.put(this.preservationModeSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.preservationModeSetting.tooltip"));
        this.preservationModeThresholdSetting = new SimpleIntegerProperty(this, "Preservation mode threshold setting",
                this.sugarRUInstance.getPreservationModeThresholdSetting()) {
            @Override
            public void set(int newValue) throws IllegalArgumentException{
                try {
                    //throws IllegalArgumentException
                    SugarRemovalUtilityFragmenter.this.sugarRUInstance.setPreservationModeThresholdSetting(newValue);
                }catch(IllegalArgumentException anException){
                    SugarRemovalUtilityFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                super.set(newValue);
            }
        };
        this.settings.add(this.preservationModeThresholdSetting);
        this.settingNameTooltipTextMap.put(this.preservationModeThresholdSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.preservationModeThresholdSetting.tooltip"));
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
        this.exocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting = new SimpleDoubleProperty(this,
                "Exocyclic oxygen atoms to atoms in ring ratio threshold setting",
                this.sugarRUInstance.getExocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting()) {
            @Override
            public void set(double newValue) throws IllegalArgumentException {
                try {
                    //throws IllegalArgumentException
                    SugarRemovalUtilityFragmenter.this.sugarRUInstance.setExocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting(newValue);
                } catch (IllegalArgumentException anException) {
                    SugarRemovalUtilityFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                super.set(newValue);
            }
        };
        this.settings.add(this.exocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting);
        this.settingNameTooltipTextMap.put(this.exocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.exocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting.tooltip"));
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
        this.linearSugarCandidateMinimumSizeSetting = new SimpleIntegerProperty(this,
                "Linear sugar candidate minimum size setting",
                this.sugarRUInstance.getLinearSugarCandidateMinSizeSetting()) {
            @Override
            public void set(int newValue) {
                try {
                    //throws IllegalArgumentException
                    SugarRemovalUtilityFragmenter.this.sugarRUInstance.setLinearSugarCandidateMinSizeSetting(newValue);
                } catch (IllegalArgumentException anException) {
                    SugarRemovalUtilityFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                super.set(newValue);
            }
        };
        this.settings.add(this.linearSugarCandidateMinimumSizeSetting);
        this.settingNameTooltipTextMap.put(this.linearSugarCandidateMinimumSizeSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.linearSugarCandidateMinimumSizeSetting.tooltip"));
        this.linearSugarCandidateMaximumSizeSetting = new SimpleIntegerProperty(this,
                "Linear sugar candidate maximum size setting",
                this.sugarRUInstance.getLinearSugarCandidateMaxSizeSetting()) {
            @Override
            public void set(int newValue) {
                try {
                    //throws IllegalArgumentException
                    SugarRemovalUtilityFragmenter.this.sugarRUInstance.setLinearSugarCandidateMaxSizeSetting(newValue);
                } catch (IllegalArgumentException anException) {
                    SugarRemovalUtilityFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                super.set(newValue);
            }
        };
        this.settings.add(this.linearSugarCandidateMaximumSizeSetting);
        this.settingNameTooltipTextMap.put(this.linearSugarCandidateMaximumSizeSetting.getName(),
                Message.get("SugarRemovalUtilityFragmenter.linearSugarCandidateMaximumSizeSetting.tooltip"));
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
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties get">
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
     * A property that has a constant name from SRUFragmenterReturnedFragmentsOption enum as value.
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
    public SRUFragmenterReturnedFragmentsOption getReturnedFragmentsSettingConstant() {
        return SRUFragmenterReturnedFragmentsOption.valueOf(this.returnedFragmentsSetting.get());
    }

    /**
     * Returns the string representation of the currently set option for the sugar type to remove setting.
     *
     * @return enum constant name of the set option
     */
    public String getSugarTypeToRemoveSetting() {
        return this.sugarTypeToRemoveSetting.get();
    }

    /**
     * Returns the property object of the sugar type to remove setting that can be used to configure this setting.
     * A property that has a constant name from SugarTypeToRemoveOption enum as value.
     *
     * @return property object of the sugar type to remove setting
     */
    public SimpleEnumConstantNameProperty sugarTypeToRemoveSettingProperty() {
        return this.sugarTypeToRemoveSetting;
    }

    /**
     * Returns the enum constant currently set as option for the sugar type to remove setting.
     *
     * @return enum constant for sugar type to remove setting
     */
    public SugarTypeToRemoveOption getSugarTypeToRemoveSettingConstant() {
        return SugarTypeToRemoveOption.valueOf(this.sugarTypeToRemoveSetting.get());
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
     * Returns the string representation of the currently set option for the preservation mode setting.
     *
     * @return enum constant name of the set option
     */
    public String getPreservationModeSetting() {
        return this.preservationModeSetting.get();
    }

    /**
     * Returns the property object of the preservation mode setting that can be used to configure this setting.
     * It has a constant from the SugarRemovalUtility.PreservationModeOption enum as value.
     * @return property object of the preservation mode setting
     */
    public SimpleEnumConstantNameProperty preservationModeSettingProperty() {
        return this.preservationModeSetting;
    }

    /**
     * Returns the enum constant currently set as option for the preservation mode setting.
     *
     * @return enum constant for preservation mode setting
     */
    public SugarRemovalUtility.PreservationModeOption getPreservationModeSettingConstant() {
        return SugarRemovalUtility.PreservationModeOption.valueOf(this.preservationModeSetting.get());
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
     * @param anOptionName name of a constant from the SRUFragmenterReturnedFragmentsOption enum
     * @throws NullPointerException if the given string is null
     * @throws IllegalArgumentException if the given string is not an enum constant name
     */
    public void setReturnedFragmentsSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        SRUFragmenterReturnedFragmentsOption tmpConstant = SRUFragmenterReturnedFragmentsOption.valueOf(anOptionName);
        this.setReturnedFragmentsSetting(tmpConstant);
    }

    /**
     * Sets the returned fragments setting, defining whether only sugar moieties, only the aglycone, or both should
     * be returned.
     *
     * @param anOption a constant from the SRUFragmenterReturnedFragmentsOption enum
     * @throws NullPointerException if the given parameter is null
     */
    public void setReturnedFragmentsSetting(SRUFragmenterReturnedFragmentsOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        this.returnedFragmentsSetting.set(anOption.name());
    }

    /**
     * Sets the sugar type to remove setting, defining whether only circular, only linear, or both kinds of sugar
     * moieties should be detected/removed.
     *
     * @param anOptionName name of a constant from the SugarTypeToRemoveOption enum
     * @throws NullPointerException if the given string is null
     * @throws IllegalArgumentException if the given string is not an enum constant name
     */
    public void setSugarTypeToRemoveSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        SugarTypeToRemoveOption tmpConstant = SugarTypeToRemoveOption.valueOf(anOptionName);
        this.setSugarTypeToRemoveSetting(tmpConstant);
    }

    /**
     * Sets the sugar type to remove setting, defining whether only circular, only linear, or both kinds of sugar
     * moieties should be detected/removed.
     *
     * @param aSugarTypeToRemoveOption a constant from the SugarTypeToRemoveOption enum
     * @throws NullPointerException if the given parameter is null
     */
    public void setSugarTypeToRemoveSetting(SugarTypeToRemoveOption aSugarTypeToRemoveOption) throws NullPointerException {
        Objects.requireNonNull(aSugarTypeToRemoveOption, "Given type of sugars to remove is null.");
        this.sugarTypeToRemoveSetting.set(aSugarTypeToRemoveOption.name());
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
     * whether a fragment is 'big enough' to  be kept and not discarded.
     *
     * @param anOptionName name of a constant from the SugarRemovalUtility.PreservationModeOption enum
     * @throws NullPointerException if the given string is null
     * @throws IllegalArgumentException if the given string is not an enum constant name
     */
    public void setPreservationModeSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        SugarRemovalUtility.PreservationModeOption tmpConstant = SugarRemovalUtility.PreservationModeOption.valueOf(anOptionName);
        this.setPreservationModeSetting(tmpConstant);
    }

    /**
     * Sets the preservation mode setting, defining what molecular characteristic should be considered when judging
     * whether a fragment is 'big enough' to be kept and not discarded.
     *
     * @param anOption a constant from the SugarRemovalUtility.PreservationModeOption enum
     * @throws NullPointerException if the given parameter is null
     */
    public void setPreservationModeSetting(SugarRemovalUtility.PreservationModeOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        //synchronisation with SRU instance done in overridden set() function of the property
        this.preservationModeSetting.set(anOption.name());
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
    public String getFragmentationAlgorithmName() {
        return SugarRemovalUtilityFragmenter.ALGORITHM_NAME;
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
        SugarRemovalUtilityFragmenter tmpCopy = new SugarRemovalUtilityFragmenter();
        tmpCopy.setReturnedFragmentsSetting(this.returnedFragmentsSetting.get());
        tmpCopy.setSugarTypeToRemoveSetting(this.sugarTypeToRemoveSetting.get());
        tmpCopy.setFragmentSaturationSetting(this.fragmentSaturationSetting.get());
        tmpCopy.setDetectCircularSugarsOnlyWithGlycosidicBondSetting(this.detectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting.get());
        tmpCopy.setRemoveOnlyTerminalSugarsSetting(this.removeOnlyTerminalSugarsSetting.get());
        tmpCopy.setPreservationModeSetting(this.preservationModeSetting.get());
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
        this.returnedFragmentsSetting.set(SugarRemovalUtilityFragmenter.RETURNED_FRAGMENTS_OPTION_DEFAULT.name());
        this.sugarTypeToRemoveSetting.set(SugarRemovalUtilityFragmenter.SUGAR_TYPE_TO_REMOVE_OPTION_DEFAULT.name());
        this.fragmentSaturationSetting.set(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT.name());
        this.sugarRUInstance.restoreDefaultSettings();
        this.detectCircularSugarsOnlyWithGlycosidicBondSetting.set(this.sugarRUInstance.areOnlyCircularSugarsWithOGlycosidicBondDetected());
        this.removeOnlyTerminalSugarsSetting.set(this.sugarRUInstance.areOnlyTerminalSugarsRemoved());
        this.preservationModeSetting.set(this.sugarRUInstance.getPreservationModeSetting().name());
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
            List<IAtomContainer> tmpReturnList = new ArrayList<IAtomContainer>(1);
            tmpReturnList.add(0, aMolecule.clone());
            aMolecule.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                    SugarRemovalUtilityFragmenter.FRAGMENT_CATEGORY_DEGLYCOSYLATED_CORE_VALUE);
            return tmpReturnList;
        }
        boolean tmpCanBeFragmented = this.canBeFragmented(aMolecule);
        if (!tmpCanBeFragmented) {
            throw new IllegalArgumentException("Given molecule cannot be fragmented but should be filtered or preprocessed first.");
        }
        List<IAtomContainer> tmpFragments;
        SugarTypeToRemoveOption tmpOption = SugarTypeToRemoveOption.valueOf(this.sugarTypeToRemoveSetting.get());
        try {
            switch (tmpOption) {
                case CIRCULAR:
                    tmpFragments = this.sugarRUInstance.removeAndReturnCircularSugars(aMolecule, true);
                    break;
                case LINEAR:
                    tmpFragments = this.sugarRUInstance.removeAndReturnLinearSugars(aMolecule, true);
                    break;
                case CIRCULAR_AND_LINEAR:
                    tmpFragments = this.sugarRUInstance.removeAndReturnCircularAndLinearSugars(aMolecule, true);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + this.sugarTypeToRemoveSetting);
            }
        } catch (IllegalArgumentException | CloneNotSupportedException anException) {
            throw new IllegalArgumentException("An error occurred during fragmentation: " + anException.toString());
        }
        //post-processing of aglycone, it is always saturated with implicit hydrogen atoms (might be empty)
        IAtomContainer tmpAglycone = tmpFragments.get(0);
        tmpAglycone.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                SugarRemovalUtilityFragmenter.FRAGMENT_CATEGORY_DEGLYCOSYLATED_CORE_VALUE);
        boolean tmpSugarsWereDetected = (tmpFragments.size() > 1);
        if (this.returnedFragmentsSetting.get().equals(SRUFragmenterReturnedFragmentsOption.ALL_FRAGMENTS.name())
                || this.returnedFragmentsSetting.get().equals(SRUFragmenterReturnedFragmentsOption.ONLY_AGLYCONE.name())) {
            if (!tmpAglycone.isEmpty()) {
                if (!ConnectivityChecker.isConnected(tmpAglycone)) {
                    List<IAtomContainer> tmpAglyconeFragments = SugarRemovalUtility.partitionAndSortUnconnectedFragments(tmpAglycone);
                    for (IAtomContainer tmpAglyconeFragment : tmpAglyconeFragments) {
                        tmpAglyconeFragment.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                                SugarRemovalUtilityFragmenter.FRAGMENT_CATEGORY_DEGLYCOSYLATED_CORE_VALUE);
                    }
                    tmpFragments.remove(0);
                    tmpFragments.addAll(0, tmpAglyconeFragments);
                }
            } else {
                tmpFragments.remove(0);
            }
            //else: only sugars are returned, dispose of aglycone
        } else {
            tmpFragments.remove(0);
        }
        //sugars were detected, postprocessing
        if (tmpSugarsWereDetected) {
            if (this.returnedFragmentsSetting.get().equals(SRUFragmenterReturnedFragmentsOption.ALL_FRAGMENTS.name())
                    || this.returnedFragmentsSetting.get().equals(SRUFragmenterReturnedFragmentsOption.ONLY_SUGAR_MOIETIES.name())) {
                for (int i = 0; i < tmpFragments.size(); i++) {
                    IAtomContainer tmpSugarFragment = tmpFragments.get(i);
                    if (!Objects.isNull(tmpSugarFragment.getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY))
                            && ((String) tmpSugarFragment.getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY))
                                    .equals(SugarRemovalUtilityFragmenter.FRAGMENT_CATEGORY_DEGLYCOSYLATED_CORE_VALUE)) {
                        continue;
                    }
                    if (Objects.isNull(tmpSugarFragment.getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY))) {
                        tmpSugarFragment.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                                SugarRemovalUtilityFragmenter.FRAGMENT_CATEGORY_SUGAR_MOIETY_VALUE);
                    }
                    try {
                        if (this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION.name())) {
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
                    if (!Objects.isNull(tmpFragment.getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY))
                            && ((String) tmpFragment.getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY))
                            .equals(SugarRemovalUtilityFragmenter.FRAGMENT_CATEGORY_DEGLYCOSYLATED_CORE_VALUE)) {
                        continue;
                    } else {
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
            if (!tmpIsConnected) {
                return true;
            }
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
        //Todo I (Jonas) would like to remove any preprocessing done by the fragmenters as soon as possible, i.e. as soon as we have central preprocessing functionalities available
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
