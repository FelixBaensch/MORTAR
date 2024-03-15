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

import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.CollectionUtil;
import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;

import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.scaffold.ScaffoldGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper class that makes the <a href="https://github.com/cdk/cdk-scaffold">CDK Scaffold module</a> functionality
 * available in MORTAR.
 *
 * @author Julian Zander, Jonas Schaub (zanderjulian@gmx.de, jonas.schaub@uni-jena.de)
 * @version 1.0.0.0
 */
public class ScaffoldGeneratorFragmenter implements IMoleculeFragmenter {
    //<editor-fold desc="Enum SmilesGeneratorOption">
    /**
     * Enum for defining which SmiFlavor is used for the SmilesGenerator.
     * The SmilesGenerator is used for the enumerative fragmentation.
     */
    public static enum SmilesGeneratorOption {
        /**
         * Output canonical SMILES without stereochemistry and without atomic masses.
         */
        UNIQUE_WITHOUT_STEREO,

        /**
         * Output canonical SMILES with stereochemistry and without atomic masses.
         */
        UNIQUE_WITH_STEREO;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Enum SideChainOption">
    /**
     * Enum that defines whether only scaffolds, only side chains, or both are to be generated.
     */
    public static enum SideChainOption {
        /**
         * Generate only the scaffold without side chains.
         */
        ONLY_SCAFFOLDS,

        /**
         * Generate only the side chains without scaffolds.
         */
        ONLY_SIDE_CHAINS,

        /**
         * Generate scaffolds and side chains.
         */
        SCAFFOLDS_AND_SIDE_CHAINS;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Enum FragmentationTypeOption">
    /**
     * Enum for defining which kind of fragmentation is used and shows how much fragmentation is to take place.
     */
    public static enum FragmentationTypeOption {
        /**
         * {@link ScaffoldGenerator#applyEnumerativeRemoval(IAtomContainer)} is used.
         */
        ENUMERATIVE_FRAGMENTATION,

        /**
         * {@link ScaffoldGenerator#applySchuffenhauerRules(IAtomContainer)} is used.
         */
        SCHUFFENHAUER_FRAGMENTATION,

        /**
         * {@link ScaffoldGenerator#getScaffold(IAtomContainer, boolean)} is used.
         */
        SCAFFOLD_ONLY,

        /**
         * {@link ScaffoldGenerator#getRings(IAtomContainer, boolean)} is used.
         */
        RING_DISSECTION;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public static final constants">
    /**
     * Name of the algorithm used in this fragmenter.
     */
    public static final String ALGORITHM_NAME = "Scaffold Generator";

    /**
     * Default option for the cycle finder algorithm employed for aromaticity detection.
     */
    public static final IMoleculeFragmenter.CycleFinderOption CYCLE_FINDER_OPTION_DEFAULT =
            IMoleculeFragmenter.CycleFinderOption.CDK_AROMATIC_SET;

    /**
     * Cycle finder algorithm that is used should the set option cause an IntractableException.
     */
    public static final CycleFinder AUXILIARY_CYCLE_FINDER = Cycles.cdkAromaticSet();

    /**
     * Default electron donation model for aromaticity detection.
     */
    public static final IMoleculeFragmenter.ElectronDonationModelOption Electron_Donation_MODEL_OPTION_DEFAULT
            = IMoleculeFragmenter.ElectronDonationModelOption.CDK;

    /**
     * Default SmiFlavor for the default SmilesGenerator.
     */
    public static final ScaffoldGeneratorFragmenter.SmilesGeneratorOption SMILES_GENERATOR_OPTION_DEFAULT = SmilesGeneratorOption.UNIQUE_WITHOUT_STEREO;

    /**
     * Default fragmentation type.
     */
    public static final FragmentationTypeOption FRAGMENTATION_TYPE_OPTION_DEFAULT = FragmentationTypeOption.SCHUFFENHAUER_FRAGMENTATION;

    /**
     * Default side chain option.
     */
    public static final SideChainOption SIDE_CHAIN_OPTION_DEFAULT = SideChainOption.ONLY_SCAFFOLDS;

    /**
     * Scaffolds will be assigned this value for the property with key IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY.
     */
    public static final String FRAGMENT_CATEGORY_SCAFFOLD_VALUE = "SGFragmenter.Scaffold";

    /**
     * Side chains will be assigned this value for the property with key IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY.
     */
    public static final String FRAGMENT_CATEGORY_SIDE_CHAIN_VALUE = "SGFragmenter.Sidechain";

    /**
     * Parent scaffolds will be assigned this value for the property with key IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY.
     */
    public static final String FRAGMENT_CATEGORY_PARENT_SCAFFOLD_VALUE = "SGFragmenter.ParentScaffold";
    //</editor-fold>
    //
    //<editor-fold desc="Private variables">
    /**
     * SmilesGenerator instance for the enumerative fragmentation.
     */
    private SmilesGenerator smilesGeneratorInstance;

    /**
     * A cycle finder instance for construction of the aromaticity model.
     */
    private CycleFinder cycleFinderInstance;

    /**
     * A cycle finder instance for construction of the aromaticity model.
     */
    private ElectronDonation electronDonationInstance;
    //</editor-fold>
    //
    //<editor-fold desc="Private final variables">

    private final SimpleEnumConstantNameProperty scaffoldModeSetting;

    private final SimpleBooleanProperty determineAromaticitySetting;

    private final SimpleEnumConstantNameProperty smilesGeneratorSetting;

    private final SimpleBooleanProperty ruleSevenAppliedSetting;

    private final SimpleBooleanProperty retainOnlyHybridisationsAtAromaticBondsSetting;

    private final SimpleEnumConstantNameProperty sideChainSetting;

    private final SimpleEnumConstantNameProperty fragmentationTypeSetting;

    private final SimpleEnumConstantNameProperty cycleFinderSetting;

    /**
     * A property that has a constant name from the IMoleculeFragmenter.FragmentSaturationOption enum as value.
     */
    private final SimpleEnumConstantNameProperty fragmentSaturationSetting;

    private final SimpleEnumConstantNameProperty electronDonationModelSetting;

    /**
     * All settings of this fragmenter, encapsulated in JavaFX properties for binding in GUI.
     */
    private final List<Property<?>> settings;

    /**
     * Map to store pairs of {@literal <setting name, tooltip text>}.
     */
    private final HashMap<String, String> settingNameTooltipTextMap;

    /**
     * Instance of the ScaffoldGenerator class used to do generate fragments or scaffolds from the molecules.
     */
    private final ScaffoldGenerator scaffoldGeneratorInstance;

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(ScaffoldGeneratorFragmenter.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="Constructor">
    /**
     * Constructor, all settings are initialised with their default values as declared in the respective public constants.
     */
    public ScaffoldGeneratorFragmenter() {
        this.scaffoldGeneratorInstance = new ScaffoldGenerator();
        int tmpNumberOfSettingsForTooltipMapSize= 10;
        int tmpInitialCapacityForSettingNameTooltipTextMap = CollectionUtil.calculateInitialHashCollectionCapacity(
                tmpNumberOfSettingsForTooltipMapSize,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.settingNameTooltipTextMap = new HashMap<>(tmpInitialCapacityForSettingNameTooltipTextMap, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.fragmentSaturationSetting = new SimpleEnumConstantNameProperty(this, "Fragment saturation setting",
                IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT.name(), IMoleculeFragmenter.FragmentSaturationOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ScaffoldGeneratorFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
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
                Message.get("ScaffoldGeneratorFragmenter.fragmentSaturationSetting.tooltip"));
        this.scaffoldModeSetting = new SimpleEnumConstantNameProperty(this, "Scaffold mode setting",
                ScaffoldGenerator.SCAFFOLD_MODE_OPTION_DEFAULT.name(), ScaffoldGenerator.ScaffoldModeOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //valueOf() throws IllegalArgumentException
                    ScaffoldGenerator.ScaffoldModeOption tmpEnumConstant = ScaffoldGenerator.ScaffoldModeOption.valueOf(newValue);
                    ScaffoldGeneratorFragmenter.this.scaffoldGeneratorInstance.setScaffoldModeSetting(tmpEnumConstant);
                } catch (IllegalArgumentException | NullPointerException anException) {
                    ScaffoldGeneratorFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
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
        this.settingNameTooltipTextMap.put(this.scaffoldModeSetting.getName(),
                Message.get("ScaffoldGeneratorFragmenter.scaffoldModeSetting.tooltip"));
        this.determineAromaticitySetting = new SimpleBooleanProperty(this,
                "Determine aromaticity setting", ScaffoldGenerator.DETERMINE_AROMATICITY_SETTING_DEFAULT) {
            @Override
            public void set(boolean newValue) {
                //throws no exceptions
                ScaffoldGeneratorFragmenter.this.scaffoldGeneratorInstance.setDetermineAromaticitySetting(newValue);
                super.set(newValue);
            }
        };
        this.settingNameTooltipTextMap.put(this.determineAromaticitySetting.getName(),
                Message.get("ScaffoldGeneratorFragmenter.determineAromaticitySetting.tooltip"));
        //note: cycle finder and electron donation model have to be set prior to setting the aromaticity model!
        this.cycleFinderSetting = new SimpleEnumConstantNameProperty(this, "Cycle finder algorithm setting",
                ScaffoldGeneratorFragmenter.CYCLE_FINDER_OPTION_DEFAULT.name(),
                IMoleculeFragmenter.CycleFinderOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ScaffoldGeneratorFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            anException.toString(),
                            anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                //throws no exception if super.set() throws no exception
                ScaffoldGeneratorFragmenter.this.setCycleFinderInstance(
                        IMoleculeFragmenter.CycleFinderOption.valueOf(newValue));
                Aromaticity tmpAromaticity = new Aromaticity(ScaffoldGeneratorFragmenter.this.electronDonationInstance, ScaffoldGeneratorFragmenter.this.cycleFinderInstance);
                ScaffoldGeneratorFragmenter.this.scaffoldGeneratorInstance.setAromaticityModelSetting(tmpAromaticity);
            }
        };
        this.settingNameTooltipTextMap.put(this.cycleFinderSetting.getName(),
                Message.get("ScaffoldGeneratorFragmenter.cycleFinderSetting.tooltip"));
        this.setCycleFinderInstance(IMoleculeFragmenter.CycleFinderOption.valueOf(this.cycleFinderSetting.get()));
        this.electronDonationModelSetting = new SimpleEnumConstantNameProperty(this, "Electron donation model setting",
                ScaffoldGeneratorFragmenter.Electron_Donation_MODEL_OPTION_DEFAULT.name(),
                IMoleculeFragmenter.ElectronDonationModelOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ScaffoldGeneratorFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            anException.toString(),
                            anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                //throws no exception if super.set() throws no exception
                ScaffoldGeneratorFragmenter.this.setElectronDonationInstance(IMoleculeFragmenter.ElectronDonationModelOption.valueOf(newValue));
                Aromaticity tmpAromaticity = new Aromaticity(ScaffoldGeneratorFragmenter.this.electronDonationInstance, ScaffoldGeneratorFragmenter.this.cycleFinderInstance);
                ScaffoldGeneratorFragmenter.this.scaffoldGeneratorInstance.setAromaticityModelSetting(tmpAromaticity);
            }
        };
        this.settingNameTooltipTextMap.put(this.electronDonationModelSetting.getName(),
                Message.get("ScaffoldGeneratorFragmenter.electronDonationModelSetting.tooltip"));
        this.setElectronDonationInstance(IMoleculeFragmenter.ElectronDonationModelOption.valueOf(this.electronDonationModelSetting.get()));
        Aromaticity tmpAromaticity = new Aromaticity(ScaffoldGeneratorFragmenter.this.electronDonationInstance, ScaffoldGeneratorFragmenter.this.cycleFinderInstance);
        ScaffoldGeneratorFragmenter.this.scaffoldGeneratorInstance.setAromaticityModelSetting(tmpAromaticity);
        this.smilesGeneratorSetting = new SimpleEnumConstantNameProperty(this, "Smiles generator setting",
                ScaffoldGeneratorFragmenter.SMILES_GENERATOR_OPTION_DEFAULT.name(), ScaffoldGeneratorFragmenter.SmilesGeneratorOption.class) {
            @Override
            public  void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ScaffoldGeneratorFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            anException.toString(),
                            anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                ScaffoldGeneratorFragmenter.this.setSmilesGeneratorInstance(ScaffoldGeneratorFragmenter.SmilesGeneratorOption.valueOf(newValue));
                ScaffoldGeneratorFragmenter.this.scaffoldGeneratorInstance.setSmilesGeneratorSetting(smilesGeneratorInstance);
            }
        };
        this.settingNameTooltipTextMap.put(this.smilesGeneratorSetting.getName(),
                Message.get("ScaffoldGeneratorFragmenter.smilesGeneratorSetting.tooltip"));
        this.setSmilesGeneratorInstance(ScaffoldGeneratorFragmenter.SmilesGeneratorOption.valueOf(this.smilesGeneratorSetting.get()));
        ScaffoldGeneratorFragmenter.this.scaffoldGeneratorInstance.setSmilesGeneratorSetting(ScaffoldGeneratorFragmenter.this.smilesGeneratorInstance);
        this.ruleSevenAppliedSetting = new SimpleBooleanProperty(this,
                "Rule seven setting", ScaffoldGenerator.RULE_SEVEN_APPLIED_SETTING_DEFAULT) {
            @Override
            public void set(boolean newValue) {
                //throws no exceptions
                ScaffoldGeneratorFragmenter.this.scaffoldGeneratorInstance.setRuleSevenAppliedSetting(newValue);
                super.set(newValue);
            }
        };
        this.settingNameTooltipTextMap.put(this.ruleSevenAppliedSetting.getName(),
                Message.get("ScaffoldGeneratorFragmenter.ruleSevenAppliedSetting.tooltip"));
        this.retainOnlyHybridisationsAtAromaticBondsSetting = new SimpleBooleanProperty(this,
                "Retain only hybridisations at aromatic bonds setting", ScaffoldGenerator.RETAIN_ONLY_HYBRIDISATIONS_AT_AROMATIC_BONDS_SETTING_DEFAULT) {
            @Override
            public void set(boolean newValue) {
                //throws no exceptions
                ScaffoldGeneratorFragmenter.this.scaffoldGeneratorInstance.setRetainOnlyHybridisationsAtAromaticBondsSetting(newValue);
                super.set(newValue);
            }
        };
        this.settingNameTooltipTextMap.put(this.retainOnlyHybridisationsAtAromaticBondsSetting.getName(),
                Message.get("ScaffoldGeneratorFragmenter.retainOnlyHybridisationsAtAromaticBondsSetting.tooltip"));
        this.fragmentationTypeSetting = new SimpleEnumConstantNameProperty(this, "Fragmentation type setting",
                ScaffoldGeneratorFragmenter.FRAGMENTATION_TYPE_OPTION_DEFAULT.name(), ScaffoldGeneratorFragmenter.FragmentationTypeOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ScaffoldGeneratorFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            anException.toString(),
                            anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        this.settingNameTooltipTextMap.put(this.fragmentationTypeSetting.getName(),
                Message.get("ScaffoldGeneratorFragmenter.fragmentationTypeSetting.tooltip"));
        this.sideChainSetting = new SimpleEnumConstantNameProperty(this, "Side chain setting",
                ScaffoldGeneratorFragmenter.SIDE_CHAIN_OPTION_DEFAULT.name(), ScaffoldGeneratorFragmenter.SideChainOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ScaffoldGeneratorFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            anException.toString(),
                            anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        this.settingNameTooltipTextMap.put(this.sideChainSetting.getName(),
                Message.get("ScaffoldGeneratorFragmenter.sideChainSetting.tooltip"));
        this.settings = new ArrayList<>(10);
        this.settings.add(this.fragmentationTypeSetting);
        this.settings.add(this.fragmentSaturationSetting);
        this.settings.add(this.scaffoldModeSetting);
        this.settings.add(this.determineAromaticitySetting);
        this.settings.add(this.electronDonationModelSetting);
        this.settings.add(this.cycleFinderSetting);
        this.settings.add(this.smilesGeneratorSetting);
        this.settings.add(this.ruleSevenAppliedSetting);
        this.settings.add(this.retainOnlyHybridisationsAtAromaticBondsSetting);
        this.settings.add(this.sideChainSetting);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties set">
    /**
     * Sets the scaffold mode setting, defining which form of scaffold is to be created.
     *
     * @param anOptionName name of a constant from the ScaffoldGenerator.ScaffoldModeOption enum
     * @throws NullPointerException if the given string is null
     * @throws IllegalArgumentException if the given string is not an enum constant name
     */
    public void setScaffoldModeSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        ScaffoldGenerator.ScaffoldModeOption tmpConstant = ScaffoldGenerator.ScaffoldModeOption.valueOf(anOptionName);
        this.setScaffoldModeSetting(tmpConstant);
    }

    /**
     * Sets the scaffold mode setting, defining which form of scaffold is to be created.
     *
     * @param anOption a constant from the ScaffoldGenerator.ScaffoldModeOption enum
     * @throws NullPointerException if the given parameter is null
     */
    public void setScaffoldModeSetting(ScaffoldGenerator.ScaffoldModeOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        this.scaffoldModeSetting.set(anOption.name());
    }

    /**
     * Sets the setting defining whether aromaticity should be determined.
     *
     * @param aBoolean true, if aromaticity should be determined
     */
    public void setDetermineAromaticitySetting(boolean aBoolean) {
        //synchronisation with ScaffoldGenerator instance done in overridden set() function of the property
        this.determineAromaticitySetting.set(aBoolean);
    }

    /**
     * Sets the electron donation model setting. The set electron donation model is used for aromaticity detection in
     * preprocessing together with the set cycle finder algorithm.
     *
     * @param anOptionName name of a constant from the IMoleculeFragmenter.ElectronDonationModelOption enum
     * @throws NullPointerException if the given string is null
     * @throws IllegalArgumentException if the given string is not an enum constant name
     */
    public void setElectronDonationModelSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        IMoleculeFragmenter.ElectronDonationModelOption tmpConstant =
                IMoleculeFragmenter.ElectronDonationModelOption.valueOf(anOptionName);
        this.setElectronDonationModelSetting(tmpConstant);
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
        this.electronDonationModelSetting.set(anOption.name());
    }

    /**
     * Sets the cycle finder setting. The chosen cycle finder algorithm is used for aromaticity detection in
     * preprocessing together with the set electron donation model.
     *
     * @param anOptionName name of a constant from the IMoleculeFragmenter.CycleFinderOption enum
     * @throws NullPointerException if the given string is null
     * @throws IllegalArgumentException if the given string is not an enum constant name
     */
    public void setCycleFinderSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        IMoleculeFragmenter.CycleFinderOption tmpConstant = IMoleculeFragmenter.CycleFinderOption.valueOf(anOptionName);
        this.setCycleFinderSetting(tmpConstant);
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
        this.cycleFinderSetting.set(anOption.name());
    }

    /**
     * Sets the SMILES generator, defining which smiles generator should be used.
     *
     * @param anOptionName name of a constant from the SmilesGeneratorOption enum
     * @throws NullPointerException if the given string is null
     * @throws IllegalArgumentException if the given string is not an enum constant name
     */
    public void setSmilesGeneratorSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        SmilesGeneratorOption tmpConstant = SmilesGeneratorOption.valueOf(anOptionName);
        this.setSmilesGeneratorSetting(tmpConstant);
    }

    /**
     * Sets the SMILES generator setting, defining which form of smiles generator is to be created.
     *
     * @param anOption a constant from the SmilesGeneratorOption enum
     * @throws NullPointerException if the given parameter is null
     */
    public void setSmilesGeneratorSetting(SmilesGeneratorOption anOption) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOption, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        this.smilesGeneratorSetting.set(anOption.name());
    }

    /**
     * Sets the setting defining whether Schuffenhauer rule seven should be applied.
     *
     * @param aBoolean true, if rule seven should be applied
     */
    public void setRuleSevenAppliedSetting(boolean aBoolean) {
        //synchronisation with ScaffoldGenerator instance done in overridden set() function of the property
        this.ruleSevenAppliedSetting.set(aBoolean);
    }

    /**
     * Sets the setting defining whether only hybridisation at aromatic bonds should be retained.
     *
     * @param aBoolean true, if only hybridisation at aromatic bonds should be retained.
     */
    public void setRetainOnlyHybridisationAtAromaticBondsSetting(boolean aBoolean) {
        //synchronisation with ScaffoldGenerator instance done in overridden set() function of the property
        this.retainOnlyHybridisationsAtAromaticBondsSetting.set(aBoolean);
    }

    /**
     * Sets the FragmentationType setting, defining which type of fragmentation is applied to the input molecule.
     *
     * @param anOptionName name of a constant from the FragmentationType enum
     * @throws NullPointerException if the given string is null
     * @throws IllegalArgumentException if the given string is not an enum constant name
     */
    public void setFragmentationTypeSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        FragmentationTypeOption tmpConstant = FragmentationTypeOption.valueOf(anOptionName);
        this.setFragmentationTypeSetting(tmpConstant);
    }

    /**
     * Sets the FragmentationType setting, defining which type of fragmentation is applied to the input molecule.
     *
     * @param anOption a constant from the FragmentationType enum
     * @throws NullPointerException if the given parameter is null
     */
    public void setFragmentationTypeSetting(FragmentationTypeOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given type of fragmentation to remove is null.");
        this.fragmentationTypeSetting.set(anOption.name());
    }

    /**
     * Sets the SideChain setting, defining whether only scaffolds, only side chains or both are to be generated.
     *
     * @param anOptionName name of a constant from the SideChainOption enum
     * @throws NullPointerException if the given string is null
     * @throws IllegalArgumentException if the given string is not an enum constant name
     */
    public void setSideChainSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        SideChainOption tmpConstant = SideChainOption.valueOf(anOptionName);
        this.setSideChainSetting(tmpConstant);
    }

    /**
     *Sets the SideChain setting, defining whether only scaffolds, only side chains or both are to be generated.
     *
     * @param anOption a constant from the SideChainOption enum
     * @throws NullPointerException if the given parameter is null
     */
    public void setSideChainSetting(SideChainOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given type of side chain option is null.");
        this.sideChainSetting.set(anOption.name());
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties get">
    /**
     * Returns the boolean value of the Scaffold Generator setting whether hybridisations
     * should only be retained at aromatic bonds (true) or all bonds (false).
     *
     * @return true if hybridisations should only be retained at aromatic bonds
     */
    public boolean getRetainOnlyHybridisationsAtAromaticBondsSetting() {
        return this.retainOnlyHybridisationsAtAromaticBondsSetting.get();
    }

    /**
     * Returns the property object of the Scaffold Generator setting whether hybridisations
     * should only be retained at aromatic bonds (true) or all bonds (false).
     *
     * @return property object wrapping boolean value if hybridisations should only be retained at aromatic bonds
     */
    public SimpleBooleanProperty retainOnlyHybridisationsAtAromaticBondsSetting() {
        return this.retainOnlyHybridisationsAtAromaticBondsSetting;
    }

    /**
     * Returns the string representation of the currently set option for the side chain.
     *
     * @return enum constant name of the set option
     */
    public String getSideChainSetting() {
        return this.sideChainSetting.get();
    }

    /**
     * Returns the property object of the side chain setting that can be used to configure this setting.
     * Property that has a constant name from SideChainOption enum as value.
     *
     * @return property object of the returned side chain setting
     */
    public SimpleEnumConstantNameProperty sideChainSettingProperty() {
        return this.sideChainSetting;
    }

    /**
     * Returns the enum constant currently set as option for the side chain.
     *
     * @return enum constant for side chain setting
     */
    public SideChainOption getSideChainSettingConstant() {
        return SideChainOption.valueOf(this.sideChainSetting.get());
    }

    /**
     * Returns the string representation of the currently set option for the fragmentation type.
     *
     * @return enum constant name of the set option
     */
    public String getFragmentationTypeSetting() {
        return this.fragmentationTypeSetting.get();
    }

    /**
     * Returns the property object of the fragmentation type setting that can be used to configure this setting.
     * Property that has a constant name from FragmentationTypeOption enum as value.
     *
     * @return property object of the returned fragmentation type setting
     */
    public SimpleEnumConstantNameProperty fragmentationTypeSettingProperty() {
        return this.fragmentationTypeSetting;
    }

    /**
     * Returns the enum constant currently set as option for the fragmentation type.
     *
     * @return enum constant for fragmentation type setting
     */
    public FragmentationTypeOption getFragmentationTypeSettingConstant() {
        return FragmentationTypeOption.valueOf(this.fragmentationTypeSetting.get());
    }

    /**
     * Returns the current state of retain only hybridisation at aromatic bonds setting
     *
     * @return true if only the hybridisation at aromatic bonds is retained
     */
    public boolean getRetainOnlyHybridisationAtAromaticBondsSetting() {
        return this.retainOnlyHybridisationsAtAromaticBondsSetting.get();
    }

    /**
     * Returns the property object of retain only hybridisation at aromatic bonds setting that can be used to
     * configure this setting.
     *
     * @return property object of retain only hybridisation at aromatic bonds setting
     */
    public SimpleBooleanProperty retainOnlyHybridisationAtAromaticBondsSettingProperty() {
        return this.retainOnlyHybridisationsAtAromaticBondsSetting;
    }


    /**
     * Returns the current state of rule seven applied setting.
     *
     * @return true if rule seven should be applied
     */
    public boolean getRuleSevenAppliedSetting() {
        return this.ruleSevenAppliedSetting.get();
    }

    /**
     * Returns the property object of the rule seven applied setting that can be used to
     * configure this setting.
     *
     * @return property object of rule seven applied setting
     */
    public SimpleBooleanProperty ruleSevenAppliedSettingProperty() {
        return this.ruleSevenAppliedSetting;
    }

    /**
     * Returns the string representation of the currently set option for the SMILES generator.
     *
     * @return enum constant name of the set option
     */
    public String getSmilesGeneratorSetting() {
        return this.smilesGeneratorSetting.get();
    }

    /**
     * Returns the property object of the returned smiles generator setting that can be used to configure this setting.
     *
     * @return property object of the returned scaffold mode setting
     */
    public SimpleEnumConstantNameProperty smilesGeneratorSettingProperty() {
        return this.smilesGeneratorSetting;
    }

    /**
     * Returns the enum constant currently set as option for the SMILES generator.
     *
     * @return enum constant for smiles generator setting
     */
    public SmilesGeneratorOption getSmilesGeneratorSettingConstant() {
        return SmilesGeneratorOption.valueOf(this.smilesGeneratorSetting.get());
    }

    /**
     * Returns the current state of determine aromaticity setting.
     *
     * @return true if aromaticity should be determined
     */
    public boolean getDetermineAromaticitySetting() {
        return this.determineAromaticitySetting.get();
    }

    /**
     * Returns the property object of determine aromaticity setting that can be used to
     * configure this setting.
     *
     * @return property object of determine aromaticity setting
     */
    public SimpleBooleanProperty determineAromaticitySettingProperty() {
        return this.determineAromaticitySetting;
    }

    /**
     * Returns the string representation of the currently set option for the returned scaffold mode.
     *
     * @return enum constant name of the set option
     */
    public String getScaffoldModeSetting() {
        return this.scaffoldModeSetting.get();
    }

    /**
     * Returns the property object of the returned scaffold mode setting that can be used to configure this setting.
     *
     * @return property object of the returned scaffold mode setting
     */
    public SimpleEnumConstantNameProperty scaffoldModeSettingProperty() {
        return this.scaffoldModeSetting;
    }

    /**
     * Returns the enum constant currently set as option for the returned scaffold mode setting.
     *
     * @return enum constant for returned scaffold mode setting
     */
    public ScaffoldGenerator.ScaffoldModeOption getScaffoldModeSettingConstant() {
        return ScaffoldGenerator.ScaffoldModeOption.valueOf(this.scaffoldModeSetting.get());
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
    public IMoleculeFragmenter.ElectronDonationModelOption getElectronDonationModelSettingConstant() {
        return IMoleculeFragmenter.ElectronDonationModelOption.valueOf(this.electronDonationModelSetting.get());
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
     * A property that has a constant name from the IMoleculeFragmenter.CycleFinderOption enum as value.
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
    public IMoleculeFragmenter.CycleFinderOption getCycleFinderSettingConstant() {
        return IMoleculeFragmenter.CycleFinderOption.valueOf(this.cycleFinderSetting.get());
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
        return ScaffoldGeneratorFragmenter.ALGORITHM_NAME;
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
        ScaffoldGeneratorFragmenter tmpCopy = new ScaffoldGeneratorFragmenter();
        tmpCopy.setFragmentSaturationSetting(this.fragmentSaturationSetting.get());
        tmpCopy.setScaffoldModeSetting(this.scaffoldGeneratorInstance.getScaffoldModeSetting().name());
        tmpCopy.setDetermineAromaticitySetting(this.determineAromaticitySetting.get());
        tmpCopy.setCycleFinderSetting(this.cycleFinderSetting.get());
        tmpCopy.setElectronDonationModelSetting(this.electronDonationModelSetting.get());
        tmpCopy.setSmilesGeneratorSetting(this.smilesGeneratorSetting.get());
        tmpCopy.setRuleSevenAppliedSetting(this.ruleSevenAppliedSetting.get());
        tmpCopy.setRetainOnlyHybridisationAtAromaticBondsSetting(this.retainOnlyHybridisationsAtAromaticBondsSetting.get());
        tmpCopy.setFragmentationTypeSetting(this.fragmentationTypeSetting.get());
        tmpCopy.setSideChainSetting(this.sideChainSetting.get());
        return tmpCopy;
    }

    @Override
    public void restoreDefaultSettings() {
        this.fragmentSaturationSetting.set(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT.name());
        this.scaffoldModeSetting.set(ScaffoldGenerator.SCAFFOLD_MODE_OPTION_DEFAULT.name());
        this.determineAromaticitySetting.set(ScaffoldGenerator.DETERMINE_AROMATICITY_SETTING_DEFAULT);
        this.cycleFinderSetting.set(ScaffoldGeneratorFragmenter.CYCLE_FINDER_OPTION_DEFAULT.name());
        this.setCycleFinderSetting(IMoleculeFragmenter.CycleFinderOption.valueOf(this.cycleFinderSetting.get()));
        this.electronDonationModelSetting.set(ScaffoldGeneratorFragmenter.Electron_Donation_MODEL_OPTION_DEFAULT.name());
        this.smilesGeneratorSetting.set(ScaffoldGeneratorFragmenter.SMILES_GENERATOR_OPTION_DEFAULT.name());
        this.ruleSevenAppliedSetting.set(ScaffoldGenerator.RULE_SEVEN_APPLIED_SETTING_DEFAULT);
        this.retainOnlyHybridisationsAtAromaticBondsSetting.set(ScaffoldGenerator.RETAIN_ONLY_HYBRIDISATIONS_AT_AROMATIC_BONDS_SETTING_DEFAULT);
        this.setFragmentationTypeSetting(ScaffoldGeneratorFragmenter.FRAGMENTATION_TYPE_OPTION_DEFAULT.name());
        this.setSideChainSetting(ScaffoldGeneratorFragmenter.SIDE_CHAIN_OPTION_DEFAULT.name());
    }

    @Override
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        /*Parameter test*/
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        boolean tmpCanBeFragmented = this.canBeFragmented(aMolecule);
        if (!tmpCanBeFragmented) {
            throw new IllegalArgumentException("Given molecule cannot be fragmented but should be filtered or preprocessed first.");
        }
        /*Generate fragments*/
        List<IAtomContainer> tmpReturnList = new ArrayList<>();
        List<IAtomContainer> tmpSideChainList = new ArrayList<>();
        IAtomContainer tmpMoleculeClone = aMolecule.clone();
        try {
            //Hotfix for aromatic SMILES loader bug:
            //Kekulization.kekulize(tmpMoleculeClone);
            /*Generate side chains*/
            if(this.sideChainSetting.get().equals(SideChainOption.ONLY_SIDE_CHAINS.name()) ||
                    this.sideChainSetting.get().equals(SideChainOption.SCAFFOLDS_AND_SIDE_CHAINS.name())) {
                boolean tmpSaturateWithHydrogen = this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION.name());
                tmpSideChainList = this.scaffoldGeneratorInstance.getSideChains(tmpMoleculeClone, tmpSaturateWithHydrogen);
                /*Add side chain Property*/
                for(IAtomContainer tmpSideChain : tmpSideChainList) {
                    tmpSideChain.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                            ScaffoldGeneratorFragmenter.FRAGMENT_CATEGORY_SIDE_CHAIN_VALUE);
                }
            }
            /*Return only the side chains*/
            if(this.sideChainSetting.get().equals(SideChainOption.ONLY_SIDE_CHAINS.name())) {
                return tmpSideChainList;
            }
            /*Decomposition according to the Schuffenhauer rules*/
            if(this.fragmentationTypeSetting.get().equals(FragmentationTypeOption.SCHUFFENHAUER_FRAGMENTATION.name())) {
                List<IAtomContainer> tmpFragmentList = this.scaffoldGeneratorInstance.applySchuffenhauerRules(tmpMoleculeClone);
                /*Set fragment category property*/
                boolean tmpIsFirstFragment = true;
                for(IAtomContainer tmpFragment : tmpFragmentList) {
                    /*First fragment is the scaffold*/
                    if(tmpIsFirstFragment) {
                        tmpIsFirstFragment = false;
                        tmpFragment.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                                ScaffoldGeneratorFragmenter.FRAGMENT_CATEGORY_SCAFFOLD_VALUE);
                        continue;
                    }
                    /*All other fragments are the parents*/
                    tmpFragment.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                            ScaffoldGeneratorFragmenter.FRAGMENT_CATEGORY_PARENT_SCAFFOLD_VALUE);
                }
                tmpReturnList.addAll(tmpFragmentList);
            }
            /*Enumerative decomposition*/
            if(this.fragmentationTypeSetting.get().equals(FragmentationTypeOption.ENUMERATIVE_FRAGMENTATION.name())) {
                List<IAtomContainer> tmpFragmentList = this.scaffoldGeneratorInstance.applyEnumerativeRemoval(tmpMoleculeClone);
                /*Set fragment category property*/
                boolean tmpIsFirstFragment = true;
                for(IAtomContainer tmpFragment : tmpFragmentList) {
                    /*First fragment is the scaffold*/
                    if(tmpIsFirstFragment) {
                        tmpIsFirstFragment = false;
                        tmpFragment.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                                ScaffoldGeneratorFragmenter.FRAGMENT_CATEGORY_SCAFFOLD_VALUE);
                        continue;
                    }
                    /*All other fragments are the parents*/
                    tmpFragment.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                            ScaffoldGeneratorFragmenter.FRAGMENT_CATEGORY_PARENT_SCAFFOLD_VALUE);
                }
                tmpReturnList.addAll(tmpFragmentList);
            }
            /*Generate the scaffold only*/
            if(this.fragmentationTypeSetting.get().equals(FragmentationTypeOption.SCAFFOLD_ONLY.name())) {
                boolean tmpSaturateWithHydrogen = this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION.name());
                IAtomContainer tmpScaffold = this.scaffoldGeneratorInstance.getScaffold(tmpMoleculeClone, tmpSaturateWithHydrogen);
                //Set Scaffold Property
                tmpScaffold.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                        ScaffoldGeneratorFragmenter.FRAGMENT_CATEGORY_SCAFFOLD_VALUE);
                tmpReturnList.add(tmpScaffold);
            }
            if(this.fragmentationTypeSetting.get().equals(FragmentationTypeOption.RING_DISSECTION.name())) {
                boolean tmpSaturateWithHydrogen = this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION.name());
                IAtomContainer tmpScaffold = this.scaffoldGeneratorInstance.getScaffold(tmpMoleculeClone, tmpSaturateWithHydrogen);
                tmpScaffold.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                        ScaffoldGeneratorFragmenter.FRAGMENT_CATEGORY_SCAFFOLD_VALUE);
                tmpReturnList.add(tmpScaffold);
                List<IAtomContainer> tmpFragmentList = this.scaffoldGeneratorInstance.getRings(tmpMoleculeClone, tmpSaturateWithHydrogen);
                for(IAtomContainer tmpFragment : tmpFragmentList) {
                    tmpFragment.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                            ScaffoldGeneratorFragmenter.FRAGMENT_CATEGORY_PARENT_SCAFFOLD_VALUE);
                    tmpReturnList.add(tmpFragment);
                }
            }
        } catch (Exception anException) {
            throw new IllegalArgumentException("An error occurred during fragmentation: " + anException.toString());
        }
        tmpReturnList.addAll(tmpSideChainList);
        /*Remove all empty fragments*/
        if (!tmpReturnList.isEmpty()) {
            for(int i = 0; i < tmpReturnList.size(); i++) {
                IAtomContainer tmpReturnMolecule = tmpReturnList.get(i);
                if(tmpReturnMolecule.isEmpty()){
                    tmpReturnList.remove(tmpReturnMolecule);
                    i--;
                }
            }
        }
        return tmpReturnList;
    }

    @Override
    public boolean shouldBeFiltered(IAtomContainer aMolecule) {
        return (Objects.isNull(aMolecule) || aMolecule.isEmpty());
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
        IAtomContainer tmpClonedMolecule;
        boolean tmpShouldBeFiltered = this.shouldBeFiltered(aMolecule);
        if (tmpShouldBeFiltered) {
            throw new IllegalArgumentException("The given molecule cannot be preprocessed but should be filtered.");
        }
        try {
            tmpClonedMolecule = aMolecule.clone();
        } catch(CloneNotSupportedException anException) {
            throw new CloneNotSupportedException("An error occurred during cloning: " + anException.toString());
        }
        return tmpClonedMolecule;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private methods">
    /**
     * Calling method needs to update the aromaticity model!
     */
    private void setCycleFinderInstance(IMoleculeFragmenter.CycleFinderOption anOption) throws NullPointerException {
        //Developer comment: the switch way is used instead of having the CycleFinder objects as variables of the enum constants
        // to not have static objects becoming bottlenecks in parallelization.
        Objects.requireNonNull(anOption, "Given option is null.");
        switch (anOption) {
            case IMoleculeFragmenter.CycleFinderOption.ALL:
                this.cycleFinderInstance = Cycles.or(Cycles.all(), ScaffoldGeneratorFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case IMoleculeFragmenter.CycleFinderOption.MCB:
                this.cycleFinderInstance = Cycles.or(Cycles.mcb(), ScaffoldGeneratorFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case IMoleculeFragmenter.CycleFinderOption.RELEVANT:
                this.cycleFinderInstance = Cycles.or(Cycles.relevant(), ScaffoldGeneratorFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case IMoleculeFragmenter.CycleFinderOption.ESSENTIAL:
                this.cycleFinderInstance = Cycles.or(Cycles.essential(), ScaffoldGeneratorFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case IMoleculeFragmenter.CycleFinderOption.EDGE_SHORT:
                this.cycleFinderInstance = Cycles.or(Cycles.edgeShort(), ScaffoldGeneratorFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case IMoleculeFragmenter.CycleFinderOption.VERTEX_SHORT:
                this.cycleFinderInstance = Cycles.or(Cycles.vertexShort(), ScaffoldGeneratorFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case IMoleculeFragmenter.CycleFinderOption.TRIPLET_SHORT:
                this.cycleFinderInstance = Cycles.or(Cycles.tripletShort(), ScaffoldGeneratorFragmenter.AUXILIARY_CYCLE_FINDER);
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
     * Calling method needs to update the SmilesGenerator!
     */
    private void setSmilesGeneratorInstance(ScaffoldGeneratorFragmenter.SmilesGeneratorOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        switch (anOption) {
            case ScaffoldGeneratorFragmenter.SmilesGeneratorOption.UNIQUE_WITH_STEREO:
                this.smilesGeneratorInstance = new SmilesGenerator(SmiFlavor.Unique | SmiFlavor.UseAromaticSymbols);
                break;
            case ScaffoldGeneratorFragmenter.SmilesGeneratorOption.UNIQUE_WITHOUT_STEREO:
                this.smilesGeneratorInstance = new SmilesGenerator(SmiFlavor.Unique);
                break;
            default:
                throw new IllegalArgumentException("Undefined electron donation model option.");
        }
    }
    //</editor-fold>
}
