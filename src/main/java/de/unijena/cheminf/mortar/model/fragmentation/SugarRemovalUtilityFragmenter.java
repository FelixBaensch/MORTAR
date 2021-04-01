/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2020  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.model.fragmentation;

/**
 * TODO:
 * - write doc
 */

import de.unijena.cheminf.deglycosylation.SugarRemovalUtility;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO
 */
public class SugarRemovalUtilityFragmenter implements IMoleculeFragmenter {
    //<editor-fold desc="Enum SugarTypeToRemoveOption">
    /**
     *
     */
    public static enum SugarTypeToRemoveOption {
        /**
         *
         */
        CIRCULAR,

        /**
         *
         */
        LINEAR,

        /**
         *
         */
        CIRCULAR_AND_LINEAR;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public static final constants">
    /**
     *
     */
    public static final String ALGORITHM_NAME = "Sugar Removal Utility";

    /**
     *
     */
    public static final String FRAGMENT_CATEGORY_DEGLYCOSYLATED_CORE_VALUE = "SRUFragmenter.DeglycosylatedCore";

    /**
     *
     */
    public static final String FRAGMENT_CATEGORY_SUGAR_MOIETY_VALUE = "SRUFragmenter.SugarMoiety";

    /**
     *
     */
    public static final SugarTypeToRemoveOption SUGAR_TYPE_TO_REMOVE_OPTION_DEFAULT = SugarTypeToRemoveOption.CIRCULAR_AND_LINEAR;
    //</editor-fold>
    //
    //<editor-fold desc="Private variables">
    /**
     *
     */
    private SugarRemovalUtility sugarRUInstance;
    //</editor-fold>
    //<editor-fold desc="Private final variables">
    /**
     *
     */
    private final SimpleEnumConstantNameProperty sugarTypeToRemoveSetting;

    /**
     *
     */
    private final SimpleEnumConstantNameProperty fragmentSaturationSetting;

    /**
     *
     */
    private final SimpleBooleanProperty detectCircularSugarsOnlyWithGlycosidicBondSetting;

    /**
     *
     */
    private final SimpleBooleanProperty removeOnlyTerminalSugarsSetting;

    /**
     *
     */
    private final SimpleEnumConstantNameProperty preservationModeSetting;

    /**
     * The threshold is actually an integer in the SRU but wrapped in a double here to make setup of
     * settings window easier
     */
    private final SimpleIntegerProperty preservationModeThresholdSetting;

    /**
     *
     */
    private final SimpleBooleanProperty detectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting;

    /**
     *
     */
    private final SimpleDoubleProperty exocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting;

    /**
     *
     */
    private final SimpleBooleanProperty detectLinearSugarsInRingsSetting;

    /**
     *
     */
    private final SimpleIntegerProperty linearSugarCandidateMinimumSizeSetting;

    /**
     *
     */
    private final SimpleIntegerProperty linearSugarCandidateMaximumSizeSetting;

    /**
     *
     */
    private final SimpleBooleanProperty detectLinearAcidicSugarsSetting;

    /**
     *
     */
    private final SimpleBooleanProperty detectSpiroRingsAsCircularSugarsSetting;

    /**
     *
     */
    private final SimpleBooleanProperty detectCircularSugarsWithKetoGroupsSetting;

    /**
     *
     */
    private final List<Property> settings;

    /**
     * Logger of this class
     */
    private final Logger logger = Logger.getLogger(SugarRemovalUtilityFragmenter.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="Constructor">
    /**
     * TODO
     * all settings in default, calls the SRU sole constructor
     */
    public SugarRemovalUtilityFragmenter() {
        this.sugarRUInstance = new SugarRemovalUtility();
        this.settings = new ArrayList<>(14);
        this.fragmentSaturationSetting = new SimpleEnumConstantNameProperty(this, "Fragment saturation setting",
                IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT.name(), IMoleculeFragmenter.FragmentSaturationOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    SugarRemovalUtilityFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.GuiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        this.settings.add(this.fragmentSaturationSetting);
        this.sugarTypeToRemoveSetting = new SimpleEnumConstantNameProperty(this, "Sugar type to remove setting",
                SugarRemovalUtilityFragmenter.SUGAR_TYPE_TO_REMOVE_OPTION_DEFAULT.name(),
                SugarRemovalUtilityFragmenter.SugarTypeToRemoveOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    SugarRemovalUtilityFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.GuiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        this.settings.add(this.sugarTypeToRemoveSetting);
        this.detectCircularSugarsOnlyWithGlycosidicBondSetting = new SimpleBooleanProperty(this,
                "detectCircularSugarsOnlyWithGlycosidicBondSetting",
                this.sugarRUInstance.areOnlyCircularSugarsWithOGlycosidicBondDetected()) {
            @Override
            public void set(boolean newValue) {
                //throws no exceptions
                SugarRemovalUtilityFragmenter.this.sugarRUInstance.setDetectCircularSugarsOnlyWithOGlycosidicBondSetting(newValue);
                super.set(newValue);
            }
        };
        this.settings.add(this.detectCircularSugarsOnlyWithGlycosidicBondSetting);
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
        this.preservationModeSetting = new SimpleEnumConstantNameProperty(this, "Preservation mode setting",
                this.sugarRUInstance.getPreservationModeSetting().name(), SugarRemovalUtility.PreservationModeOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //valueOf() throws IllegalArgumentException
                    SugarRemovalUtility.PreservationModeOption tmpEnumConstant = SugarRemovalUtility.PreservationModeOption.valueOf(newValue);
                    SugarRemovalUtilityFragmenter.this.sugarRUInstance.setPreservationModeSetting(tmpEnumConstant);
                } catch (IllegalArgumentException | NullPointerException anException) {
                    SugarRemovalUtilityFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.GuiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                super.set(newValue);
            }
        };
        this.settings.add(this.preservationModeSetting);
        this.preservationModeThresholdSetting = new SimpleIntegerProperty(this, "Preservation mode threshold setting",
                this.sugarRUInstance.getPreservationModeThresholdSetting()) {
            @Override
            public void set(int newValue) throws IllegalArgumentException{
                try {
                    //throws IllegalArgumentException
                    SugarRemovalUtilityFragmenter.this.sugarRUInstance.setPreservationModeThresholdSetting(newValue);
                }catch(IllegalArgumentException anException){
                    SugarRemovalUtilityFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.GuiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                super.set(newValue);
            }
        };
        this.settings.add(this.preservationModeThresholdSetting);
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
                    GuiUtil.GuiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                super.set(newValue);
            }
        };
        this.settings.add(this.exocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting);
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
                    GuiUtil.GuiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                super.set(newValue);
            }
        };
        this.settings.add(this.linearSugarCandidateMinimumSizeSetting);
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
                    GuiUtil.GuiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                super.set(newValue);
            }
        };
        this.settings.add(this.linearSugarCandidateMaximumSizeSetting);
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
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties get">
    /**
     *
     * @return
     */
    public String getSugarTypeToRemoveSetting() {
        return this.sugarTypeToRemoveSetting.get();
    }

    /**
     *
     */
    public SimpleEnumConstantNameProperty sugarTypeToRemoveSettingProperty() {
        return this.sugarTypeToRemoveSetting;
    }

    /**
     *
     */
    public SugarTypeToRemoveOption getSugarTypeToRemoveSettingConstant() {
        return SugarTypeToRemoveOption.valueOf(this.sugarTypeToRemoveSetting.get());
    }

    /**
     *
     */
    public boolean getDetectCircularSugarsOnlyWithGlycosidicBondSetting() {
        return this.detectCircularSugarsOnlyWithGlycosidicBondSetting.get();
    }

    /**
     *
     */
    public SimpleBooleanProperty detectCircularSugarsOnlyWithGlycosidicBondSettingProperty() {
        return this.detectCircularSugarsOnlyWithGlycosidicBondSetting;
    }

    /**
     *
     */
    public boolean getRemoveOnlyTerminalSugarsSetting() {
        return this.removeOnlyTerminalSugarsSetting.get();
    }

    /**
     *
     */
    public SimpleBooleanProperty removeOnlyTerminalSugarsSettingProperty() {
        return this.removeOnlyTerminalSugarsSetting;
    }

    /**
     *
     */
    public String getPreservationModeSetting() {
        return this.preservationModeSetting.get();
    }

    /**
     *
     */
    public SimpleEnumConstantNameProperty preservationModeSettingProperty() {
        return this.preservationModeSetting;
    }

    /**
     *
     */
    public SugarRemovalUtility.PreservationModeOption getPreservationModeSettingConstant() {
        return SugarRemovalUtility.PreservationModeOption.valueOf(this.preservationModeSetting.get());
    }

    /**
     *
     */
    public double getPreservationModeThresholdSetting() {
        return this.preservationModeThresholdSetting.get();
    }

    /**
     *
     */
    public SimpleIntegerProperty preservationModeThresholdSettingProperty() {
        return this.preservationModeThresholdSetting;
    }

    /**
     *
     */
    public boolean getDetectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting() {
        return this.detectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting.get();
    }

    /**
     *
     */
    public SimpleBooleanProperty detectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSettingProperty() {
        return this.detectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting;
    }

    /**
     *
     */
    public double getExocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting() {
        return this.exocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting.get();
    }

    /**
     *
     */
    public SimpleDoubleProperty exocyclicOxygenAtomsToAtomsInRingRatioThresholdSettingProperty() {
        return this.exocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting;
    }

    /**
     *
     */
    public boolean getDetectLinearSugarsInRingsSetting() {
        return this.detectLinearSugarsInRingsSetting.get();
    }

    /**
     *
     */
    public SimpleBooleanProperty detectLinearSugarsInRingsSettingProperty() {
        return this.detectLinearSugarsInRingsSetting;
    }

    /**
     *
     */
    public double getLinearSugarCandidateMinimumSizeSetting() {
        return this.linearSugarCandidateMinimumSizeSetting.get();
    }

    /**
     *
     */
    public SimpleIntegerProperty linearSugarCandidateMinimumSizeSettingProperty() {
        return this.linearSugarCandidateMinimumSizeSetting;
    }

    /**
     *
     */
    public double getLinearSugarCandidateMaximumSizeSetting() {
        return this.linearSugarCandidateMaximumSizeSetting.get();
    }

    /**
     *
     */
    public SimpleIntegerProperty linearSugarCandidateMaximumSizeSettingProperty() {
        return this.linearSugarCandidateMaximumSizeSetting;
    }

    /**
     *
     */
    public boolean getDetectLinearAcidicSugarsSetting() {
        return this.detectLinearAcidicSugarsSetting.get();
    }

    /**
     *
     */
    public SimpleBooleanProperty detectLinearAcidicSugarsSettingProperty() {
        return this.detectLinearAcidicSugarsSetting;
    }

    /**
     *
     */
    public boolean getDetectSpiroRingsAsCircularSugarsSetting() {
        return this.detectSpiroRingsAsCircularSugarsSetting.get();
    }

    /**
     *
     */
    public SimpleBooleanProperty detectSpiroRingsAsCircularSugarsSettingProperty() {
        return this.detectSpiroRingsAsCircularSugarsSetting;
    }

    /**
     *
     */
    public boolean getDetectCircularSugarsWithKetoGroupsSetting() {
        return this.detectCircularSugarsWithKetoGroupsSetting.get();
    }

    /**
     *
     */
    public SimpleBooleanProperty detectCircularSugarsWithKetoGroupsSettingProperty() {
        return this.detectCircularSugarsWithKetoGroupsSetting;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties set">
    /**
     *
     */
    public void setSugarTypeToRemoveSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        SugarTypeToRemoveOption tmpConstant = SugarTypeToRemoveOption.valueOf(anOptionName);
        this.setSugarTypeToRemoveSetting(tmpConstant);
    }

    /**
     *
     */
    public void setSugarTypeToRemoveSetting(SugarTypeToRemoveOption aSugarTypeToRemoveOption) throws NullPointerException {
        Objects.requireNonNull(aSugarTypeToRemoveOption, "Given type of sugars to remove is null.");
        this.sugarTypeToRemoveSetting.set(aSugarTypeToRemoveOption.name());
    }

    /**
     *
     * @return
     */
    public void setDetectCircularSugarsOnlyWithGlycosidicBondSetting(boolean aBoolean) {
        //synchronisation with SRU instance done in overridden set() function of the property
        this.detectCircularSugarsOnlyWithGlycosidicBondSetting.set(aBoolean);
    }

    /**
     *
     * @return
     */
    public void setRemoveOnlyTerminalSugarsSetting(boolean aBoolean) {
        //synchronisation with SRU instance done in overridden set() function of the property
        this.removeOnlyTerminalSugarsSetting.set(aBoolean);
    }

    /**
     *
     * @return
     */
    public void setPreservationModeSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        SugarRemovalUtility.PreservationModeOption tmpConstant = SugarRemovalUtility.PreservationModeOption.valueOf(anOptionName);
        this.setPreservationModeSetting(tmpConstant);
    }

    /**
     *
     * @return
     */
    public void setPreservationModeSetting(SugarRemovalUtility.PreservationModeOption anOption)
            throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOption, "Given option is null.");
        //synchronisation with SRU instance done in overridden set() function of the property
        this.preservationModeSetting.set(anOption.name());
    }

    /**
     *
     * @return
     */
    public void setPreservationModeThresholdSetting(int aThreshold) throws IllegalArgumentException {
        //parameter test, conversion, and synchronisation with SRU instance in overridden set() method of the property
        this.preservationModeThresholdSetting.set(aThreshold);
    }

    /**
     *
     * @return
     */
    public void setDetectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting(boolean aBoolean) {
        //synchronisation with SRU instance in overridden set() method
        this.detectCircularSugarsOnlyWithEnoughExocyclicOxygenAtomsSetting.set(aBoolean);
    }

    /**
     *
     * @return
     */
    public void setExocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting(double aThreshold) throws IllegalArgumentException {
        //synchronisation with SRU instance and parameter test done in overridden set() method
        this.exocyclicOxygenAtomsToAtomsInRingRatioThresholdSetting.set(aThreshold);
    }

    /**
     *
     * @return
     */
    public void setDetectLinearSugarsInRingsSetting(boolean aBoolean) {
        //synchronisation with SRU instance done in overridden set() method
        this.detectLinearSugarsInRingsSetting.set(aBoolean);
    }

    /**
     *
     * @return
     */
    public void setLinearSugarCandidateMinimumSizeSetting(int aMinSize) throws IllegalArgumentException {
        //parameter test, conversion to int, and synchronisation with SRU instance done in overridden set() method
        this.linearSugarCandidateMinimumSizeSetting.set(aMinSize);
    }

    /**
     *
     * @return
     */
    public void setLinearSugarCandidateMaximumSizeSetting(int aMaxSize) throws IllegalArgumentException {
        //parameter test, conversion to int, and synchronisation with SRU instance done in overridden set() method
        this.linearSugarCandidateMaximumSizeSetting.set(aMaxSize);
    }

    /**
     *
     * @return
     */
    public void setDetectLinearAcidicSugarsSetting(boolean aBoolean) {
        //synchronisation with SRU instance done in overridden set() method
        this.detectLinearAcidicSugarsSetting.set(aBoolean);
    }

    /**
     *
     * @return
     */
    public void setDetectSpiroRingsAsCircularSugarsSetting(boolean aBoolean) {
        //synchronisation with SRU instance done in overridden set() method
        this.detectSpiroRingsAsCircularSugarsSetting.set(aBoolean);
    }

    /**
     *
     * @return
     */
    public void setDetectCircularSugarsWithKetoGroupsSetting(boolean aBoolean) {
        //synchronisation with SRU instance done in overridden set() method
        this.detectCircularSugarsWithKetoGroupsSetting.set(aBoolean);
    }
    //</editor-fold>
    //
    //<editor-fold desc="IMoleculeFragmenter methods">

    @Override
    public List<Property> settingsProperties() {
        return this.settings;
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

    /**
     *
     * @param aMolecule
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
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
        //post-processing of aglycon, it is always saturated with implicit hydrogen atoms
        IAtomContainer tmpAglycon = tmpFragments.get(0);
        tmpAglycon.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                SugarRemovalUtilityFragmenter.FRAGMENT_CATEGORY_DEGLYCOSYLATED_CORE_VALUE);
        boolean tmpSugarsWereDetected = (tmpFragments.size() > 1);
        if (!ConnectivityChecker.isConnected(tmpAglycon)) {
            List<IAtomContainer> tmpAglyconFragments = SugarRemovalUtility.partitionAndSortUnconnectedFragments(tmpAglycon);
            for (IAtomContainer tmpAglyconFragment : tmpAglyconFragments) {
                tmpAglyconFragment.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                        SugarRemovalUtilityFragmenter.FRAGMENT_CATEGORY_DEGLYCOSYLATED_CORE_VALUE);
            }
            tmpFragments.remove(0);
            tmpFragments.addAll(0, tmpAglyconFragments);
        }
        //sugars were detected, postprocessing
        if (tmpSugarsWereDetected) {
            for (int i = 1; i < tmpFragments.size(); i++) {
                IAtomContainer tmpSugarFragment = tmpFragments.get(i);
                if (Objects.isNull(tmpSugarFragment.getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY))) {
                    tmpSugarFragment.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                            SugarRemovalUtilityFragmenter.FRAGMENT_CATEGORY_SUGAR_MOIETY_VALUE);
                }
                if (this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION.name())) {
                    try {
                        IMoleculeFragmenter.saturateWithHydrogen(tmpSugarFragment);
                    } catch (CDKException aCDKException) {
                        Logger.getLogger(SugarRemovalUtilityFragmenter.class.getName()).log(Level.WARNING, "Fragment saturation failed.");
                    }
                }
            }
        }
        return tmpFragments;
    }

    /**
     *
     * @param aFragmentList
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    @Override
    public boolean hasFragments(List<IAtomContainer> aFragmentList) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(aFragmentList, "Given fragment list is null.");
        if (aFragmentList.size() == 0) {
            throw new IllegalArgumentException("Given fragment list is empty.");
        }
        if (Objects.isNull(aFragmentList.get(0))) {
            throw new IllegalArgumentException("Object at position 0 is null, should be the deglycosylated molecule.");
        }
        String tmpCategory = aFragmentList.get(0).getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY);
        if (Objects.isNull(tmpCategory) || tmpCategory.isEmpty() || !tmpCategory.equals(SugarRemovalUtilityFragmenter.FRAGMENT_CATEGORY_DEGLYCOSYLATED_CORE_VALUE)) {
            throw new IllegalArgumentException("Object at position 0 has no or an incorrect fragment category property, should be the deglycosylated molecule.");
        }
        return !(aFragmentList.size() == 1);
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
