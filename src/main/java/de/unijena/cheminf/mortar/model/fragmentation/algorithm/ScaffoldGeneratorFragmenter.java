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
import de.unijena.cheminf.mortar.model.util.CollectionUtil;
import de.unijena.cheminf.mortar.model.util.IDisplayEnum;
import de.unijena.cheminf.mortar.model.util.SimpleIDisplayEnumConstantProperty;

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
 * @author Julian Zander (zanderjulian@gmx.de)
 * @author Jonas Schaub (jonas.schaub@uni-jena.de)
 * @version 1.0.0.0
 */
public class ScaffoldGeneratorFragmenter implements IMoleculeFragmenter {
    //<editor-fold desc="Enum SmilesGeneratorOption">
    /**
     * Enum for defining which SmiFlavor is used for the SmilesGenerator.
     * The SmilesGenerator is used for the enumerative fragmentation.
     */
    public static enum SmilesGeneratorOption implements IDisplayEnum {
        /**
         * Output canonical SMILES without stereochemistry and without atomic masses.
         */
        UNIQUE_WITHOUT_STEREO(Message.get("ScaffoldGeneratorFragmenter.SmilesGeneratorOption.UniqueWithoutStereo.displayName"),
                Message.get("ScaffoldGeneratorFragmenter.SmilesGeneratorOption.UniqueWithoutStereo.tooltip")),
        /**
         * Output canonical SMILES with stereochemistry and without atomic masses.
         */
        UNIQUE_WITH_STEREO(Message.get("ScaffoldGeneratorFragmenter.SmilesGeneratorOption.UniqueWithStereo.displayName"),
                Message.get("ScaffoldGeneratorFragmenter.SmilesGeneratorOption.UniqueWithStereo.tooltip"));
        /**
         * Language-specific name for each constant to display in the GUI.
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
        private SmilesGeneratorOption(String aDisplayName, String aTooltip) {
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
    //<editor-fold desc="Enum SideChainOption">
    /**
     * Enum that defines whether only scaffolds, only side chains, or both are to be generated.
     */
    public static enum SideChainOption implements IDisplayEnum {
        /**
         * Generate only the scaffold without side chains.
         */
        ONLY_SCAFFOLDS(Message.get("ScaffoldGeneratorFragmenter.SideChainOption.OnlyScaffolds.displayName"),
                Message.get("ScaffoldGeneratorFragmenter.SideChainOption.OnlyScaffolds.tooltip")),
        /**
         * Generate only the side chains without scaffolds.
         */
        ONLY_SIDE_CHAINS(Message.get("ScaffoldGeneratorFragmenter.SideChainOption.OnlySideChains.displayName"),
                Message.get("ScaffoldGeneratorFragmenter.SideChainOption.OnlySideChains.tooltip")),
        /**
         * Generate scaffolds and side chains.
         */
        BOTH(Message.get("ScaffoldGeneratorFragmenter.SideChainOption.ScaffoldsAndSideChains.displayName"),
                Message.get("ScaffoldGeneratorFragmenter.SideChainOption.ScaffoldsAndSideChains.tooltip"));
        /**
         * Language-specific name for each constant to display in the GUI.
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
        private SideChainOption(String aDisplayName, String aTooltip) {
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
    //<editor-fold desc="Enum FragmentationTypeOption">
    /**
     * Enum for defining which kind of fragmentation is used and shows how much fragmentation is to take place.
     */
    public static enum FragmentationTypeOption implements IDisplayEnum {
        /**
         * {@link ScaffoldGenerator#applyEnumerativeRemoval(IAtomContainer)} is used.
         */
        ENUMERATIVE(Message.get("ScaffoldGeneratorFragmenter.FragmentationTypeOption.Enumerative.displayName"),
                Message.get("ScaffoldGeneratorFragmenter.FragmentationTypeOption.Enumerative.tooltip")),
        /**
         * {@link ScaffoldGenerator#applySchuffenhauerRules(IAtomContainer)} is used.
         */
        SCHUFFENHAUER(Message.get("ScaffoldGeneratorFragmenter.FragmentationTypeOption.Schuffenhauer.displayName"),
                Message.get("ScaffoldGeneratorFragmenter.FragmentationTypeOption.Schuffenhauer.tooltip")),
        /**
         * {@link ScaffoldGenerator#getScaffold(IAtomContainer, boolean)} is used.
         */
        SCAFFOLD_ONLY(Message.get("ScaffoldGeneratorFragmenter.FragmentationTypeOption.Scaffold.displayName"),
                Message.get("ScaffoldGeneratorFragmenter.FragmentationTypeOption.Scaffold.tooltip")),
        /**
         * {@link ScaffoldGenerator#getRings(IAtomContainer, boolean)} is used.
         */
        RING_DISSECTION(Message.get("ScaffoldGeneratorFragmenter.FragmentationTypeOption.RingDissection.displayName"),
                Message.get("ScaffoldGeneratorFragmenter.FragmentationTypeOption.RingDissection.tooltip"));
        /**
         * Language-specific name for each constant to display in the GUI.
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
        private FragmentationTypeOption(String aDisplayName, String aTooltip) {
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
    //<editor-fold desc="Enum SGFragmenterScaffoldModeOption">
    /**
     * Enum with which the type of scaffolds to be generated can be set. It mirrors ScaffoldGenerator.ScaffoldModeOption
     * to be able to have display names and tooltips for the options.
     */
    public enum SGFragmenterScaffoldModeOption implements IDisplayEnum {
        /**
         * Terminal side chains of the molecule are removed except for any atoms non-single bonded
         * directly to linkers or rings, as it is e.g. defined in <a href="https://doi.org/10.1021/ci600338x">
         * "The Scaffold Tree − Visualization of the Scaffold Universe by Hierarchical Scaffold Classification"</a>.
         */
        SCAFFOLD(ScaffoldGenerator.ScaffoldModeOption.SCAFFOLD,
                Message.get("ScaffoldGeneratorFragmenter.SGFragmenterScaffoldModeOption.Scaffold.displayName"),
                Message.get("ScaffoldGeneratorFragmenter.SGFragmenterScaffoldModeOption.Scaffold.tooltip")),
        /**
         * Murcko frameworks are generated. Based on <a href="https://doi.org/10.1021/jm9602928">
         * "The Properties of Known Drugs. 1. Molecular Frameworks"</a> by Bemis and Murcko 1996.
         * All terminal side chains are removed and only linkers and rings are retained.
         */
        MURCKO_FRAMEWORK(ScaffoldGenerator.ScaffoldModeOption.MURCKO_FRAMEWORK,
                Message.get("ScaffoldGeneratorFragmenter.SGFragmenterScaffoldModeOption.Murcko.displayName"),
                Message.get("ScaffoldGeneratorFragmenter.SGFragmenterScaffoldModeOption.Murcko.tooltip")),
        /**
         * All side chains are removed, all bonds are converted into single bonds, and all atoms are converted into carbons.
         * Naming is based on <a href="https://doi.org/10.1186/s13321-021-00526-y">
         * "Molecular Anatomy: a new multi‑dimensional hierarchical scaffold analysis tool"</a>
         * by Manelfi et al. 2021.
         */
        BASIC_WIRE_FRAME(ScaffoldGenerator.ScaffoldModeOption.BASIC_WIRE_FRAME,
                Message.get("ScaffoldGeneratorFragmenter.SGFragmenterScaffoldModeOption.BasicWireFrame.displayName"),
                Message.get("ScaffoldGeneratorFragmenter.SGFragmenterScaffoldModeOption.BasicWireFrame.tooltip")),
        /**
         * All side chains are removed and multiple bonds are converted to single bonds, but the atomic elements remain.
         */
        ELEMENTAL_WIRE_FRAME(ScaffoldGenerator.ScaffoldModeOption.ELEMENTAL_WIRE_FRAME,
                Message.get("ScaffoldGeneratorFragmenter.SGFragmenterScaffoldModeOption.ElementalWireFrame.displayName"),
                Message.get("ScaffoldGeneratorFragmenter.SGFragmenterScaffoldModeOption.ElementalWireFrame.tooltip")),
        /**
         * All side chains are removed and all atoms are converted into carbons. The order of the remaining bonds is not changed.
         * Naming is based on <a href="https://doi.org/10.1186/s13321-021-00526-y">
         * "Molecular Anatomy: a new multi‑dimensional hierarchical scaffold analysis tool"</a>
         * by Manelfi et al. 2021.
         */
        BASIC_FRAMEWORK(ScaffoldGenerator.ScaffoldModeOption.BASIC_FRAMEWORK,
                Message.get("ScaffoldGeneratorFragmenter.SGFragmenterScaffoldModeOption.BasicFramework.displayName"),
                Message.get("ScaffoldGeneratorFragmenter.SGFragmenterScaffoldModeOption.BasicFramework.tooltip"));
        /**
         * Mirrored scaffold mode option from ScaffoldGenerator.
         */
        private final ScaffoldGenerator.ScaffoldModeOption scaffoldModeOption;
        /**
         * Language-specific name for each constant to display in the GUI.
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
        private SGFragmenterScaffoldModeOption(ScaffoldGenerator.ScaffoldModeOption anOption, String aDisplayName, String aTooltip) {
            this.scaffoldModeOption = anOption;
            this.displayName = aDisplayName;
            this.tooltip = aTooltip;
        }
        /**
         * Returns the wrapped scaffold mode option from ScaffoldGenerator.
         *
         * @return scaffold mode option
         */
        public ScaffoldGenerator.ScaffoldModeOption getScaffoldModeOption() {
            return this.scaffoldModeOption;
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
    public static final ScaffoldGeneratorFragmenter.SmilesGeneratorOption SMILES_GENERATOR_OPTION_DEFAULT =
            ScaffoldGeneratorFragmenter.SmilesGeneratorOption.UNIQUE_WITHOUT_STEREO;

    /**
     * Default fragmentation type.
     */
    public static final ScaffoldGeneratorFragmenter.FragmentationTypeOption FRAGMENTATION_TYPE_OPTION_DEFAULT =
            ScaffoldGeneratorFragmenter.FragmentationTypeOption.SCHUFFENHAUER;

    /**
     * Default side chain option.
     */
    public static final ScaffoldGeneratorFragmenter.SideChainOption SIDE_CHAIN_OPTION_DEFAULT =
            ScaffoldGeneratorFragmenter.SideChainOption.ONLY_SCAFFOLDS;

    /**
     * Default scaffold mode option.
     */
    public static final ScaffoldGeneratorFragmenter.SGFragmenterScaffoldModeOption SCAFFOLD_MODE_OPTION_DEFAULT =
            SGFragmenterScaffoldModeOption.SCAFFOLD;

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

    //note: since Java 21, the javadoc build complains about "double comments" when there is a comment
    // for the get() method of the property and the private property itself as well
    private final SimpleIDisplayEnumConstantProperty scaffoldModeSetting;

    private final SimpleBooleanProperty determineAromaticitySetting;

    private final SimpleIDisplayEnumConstantProperty smilesGeneratorSetting;

    private final SimpleBooleanProperty ruleSevenAppliedSetting;

    private final SimpleBooleanProperty retainOnlyHybridisationsAtAromaticBondsSetting;

    private final SimpleIDisplayEnumConstantProperty sideChainSetting;

    private final SimpleIDisplayEnumConstantProperty fragmentationTypeSetting;

    private final SimpleIDisplayEnumConstantProperty cycleFinderSetting;

    private final SimpleIDisplayEnumConstantProperty fragmentSaturationSetting;

    private final SimpleIDisplayEnumConstantProperty electronDonationModelSetting;

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
        this.settingNameDisplayNameMap = new HashMap<>(tmpInitialCapacityForSettingNameTooltipTextMap, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.fragmentSaturationSetting = new SimpleIDisplayEnumConstantProperty(this, "Fragment saturation setting",
                IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT, IMoleculeFragmenter.FragmentSaturationOption.class) {
            @Override
            public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
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
        this.settingNameDisplayNameMap.put(this.fragmentSaturationSetting.getName(),
                Message.get("ScaffoldGeneratorFragmenter.fragmentSaturationSetting.displayName"));
        this.scaffoldModeSetting = new SimpleIDisplayEnumConstantProperty(this, "Scaffold mode setting",
                ScaffoldGeneratorFragmenter.SCAFFOLD_MODE_OPTION_DEFAULT, ScaffoldGeneratorFragmenter.SGFragmenterScaffoldModeOption.class) {
            @Override
            public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    super.set(newValue);
                    ScaffoldGeneratorFragmenter.this.scaffoldGeneratorInstance.setScaffoldModeSetting(((ScaffoldGeneratorFragmenter.SGFragmenterScaffoldModeOption) this.get()).getScaffoldModeOption());
                } catch (IllegalArgumentException | NullPointerException anException) {
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
        this.settingNameTooltipTextMap.put(this.scaffoldModeSetting.getName(),
                Message.get("ScaffoldGeneratorFragmenter.scaffoldModeSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.scaffoldModeSetting.getName(),
                Message.get("ScaffoldGeneratorFragmenter.scaffoldModeSetting.displayName"));
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
        this.settingNameDisplayNameMap.put(this.determineAromaticitySetting.getName(),
                Message.get("ScaffoldGeneratorFragmenter.determineAromaticitySetting.displayName"));
        //note: cycle finder and electron donation model have to be set prior to setting the aromaticity model!
        this.cycleFinderSetting = new SimpleIDisplayEnumConstantProperty(this, "Cycle finder algorithm setting",
                ScaffoldGeneratorFragmenter.CYCLE_FINDER_OPTION_DEFAULT,
                IMoleculeFragmenter.CycleFinderOption.class) {
            @Override
            public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
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
                        (IMoleculeFragmenter.CycleFinderOption) this.get());
                Aromaticity tmpAromaticity = new Aromaticity(ScaffoldGeneratorFragmenter.this.electronDonationInstance, ScaffoldGeneratorFragmenter.this.cycleFinderInstance);
                ScaffoldGeneratorFragmenter.this.scaffoldGeneratorInstance.setAromaticityModelSetting(tmpAromaticity);
            }
        };
        this.settingNameTooltipTextMap.put(this.cycleFinderSetting.getName(),
                Message.get("ScaffoldGeneratorFragmenter.cycleFinderSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.cycleFinderSetting.getName(),
                Message.get("ScaffoldGeneratorFragmenter.cycleFinderSetting.displayName"));
        this.setCycleFinderInstance((IMoleculeFragmenter.CycleFinderOption) this.cycleFinderSetting.get());
        this.electronDonationModelSetting = new SimpleIDisplayEnumConstantProperty(this, "Electron donation model setting",
                ScaffoldGeneratorFragmenter.Electron_Donation_MODEL_OPTION_DEFAULT,
                IMoleculeFragmenter.ElectronDonationModelOption.class) {
            @Override
            public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
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
                ScaffoldGeneratorFragmenter.this.setElectronDonationInstance((IMoleculeFragmenter.ElectronDonationModelOption) this.get());
                Aromaticity tmpAromaticity = new Aromaticity(ScaffoldGeneratorFragmenter.this.electronDonationInstance, ScaffoldGeneratorFragmenter.this.cycleFinderInstance);
                ScaffoldGeneratorFragmenter.this.scaffoldGeneratorInstance.setAromaticityModelSetting(tmpAromaticity);
            }
        };
        this.settingNameTooltipTextMap.put(this.electronDonationModelSetting.getName(),
                Message.get("ScaffoldGeneratorFragmenter.electronDonationModelSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.electronDonationModelSetting.getName(),
                Message.get("ScaffoldGeneratorFragmenter.electronDonationModelSetting.displayName"));
        this.setElectronDonationInstance((IMoleculeFragmenter.ElectronDonationModelOption) this.electronDonationModelSetting.get());
        Aromaticity tmpAromaticity = new Aromaticity(ScaffoldGeneratorFragmenter.this.electronDonationInstance, ScaffoldGeneratorFragmenter.this.cycleFinderInstance);
        ScaffoldGeneratorFragmenter.this.scaffoldGeneratorInstance.setAromaticityModelSetting(tmpAromaticity);
        this.smilesGeneratorSetting = new SimpleIDisplayEnumConstantProperty(this, "SMILES generator setting",
                ScaffoldGeneratorFragmenter.SMILES_GENERATOR_OPTION_DEFAULT, ScaffoldGeneratorFragmenter.SmilesGeneratorOption.class) {
            @Override
            public  void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
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
                ScaffoldGeneratorFragmenter.this.setSmilesGeneratorInstance((ScaffoldGeneratorFragmenter.SmilesGeneratorOption) this.get());
                ScaffoldGeneratorFragmenter.this.scaffoldGeneratorInstance.setSmilesGeneratorSetting(smilesGeneratorInstance);
            }
        };
        this.settingNameTooltipTextMap.put(this.smilesGeneratorSetting.getName(),
                Message.get("ScaffoldGeneratorFragmenter.smilesGeneratorSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.smilesGeneratorSetting.getName(),
                Message.get("ScaffoldGeneratorFragmenter.smilesGeneratorSetting.displayName"));
        this.setSmilesGeneratorInstance((ScaffoldGeneratorFragmenter.SmilesGeneratorOption) this.smilesGeneratorSetting.get());
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
        this.settingNameDisplayNameMap.put(this.ruleSevenAppliedSetting.getName(),
                Message.get("ScaffoldGeneratorFragmenter.ruleSevenAppliedSetting.displayName"));
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
        this.settingNameDisplayNameMap.put(this.retainOnlyHybridisationsAtAromaticBondsSetting.getName(),
                Message.get("ScaffoldGeneratorFragmenter.retainOnlyHybridisationsAtAromaticBondsSetting.displayName"));
        this.fragmentationTypeSetting = new SimpleIDisplayEnumConstantProperty(this, "Fragmentation type setting",
                ScaffoldGeneratorFragmenter.FRAGMENTATION_TYPE_OPTION_DEFAULT, ScaffoldGeneratorFragmenter.FragmentationTypeOption.class) {
            @Override
            public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
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
        this.settingNameDisplayNameMap.put(this.fragmentationTypeSetting.getName(),
                Message.get("ScaffoldGeneratorFragmenter.fragmentationTypeSetting.displayName"));
        this.sideChainSetting = new SimpleIDisplayEnumConstantProperty(this, "Side chain setting",
                ScaffoldGeneratorFragmenter.SIDE_CHAIN_OPTION_DEFAULT, ScaffoldGeneratorFragmenter.SideChainOption.class) {
            @Override
            public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
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
        this.settingNameDisplayNameMap.put(this.sideChainSetting.getName(),
                Message.get("ScaffoldGeneratorFragmenter.sideChainSetting.displayName"));
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
     * @param anOption a constant from the ScaffoldGeneratorFragmenter.SGFragmenterScaffoldModeOption enum
     * @throws NullPointerException if the given parameter is null
     */
    public void setScaffoldModeSetting(ScaffoldGeneratorFragmenter.SGFragmenterScaffoldModeOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option name is null.");
        this.scaffoldModeSetting.set(anOption);
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
     * @param anOption a constant from the IMoleculeFragmenter.ElectronDonationModelOption enum
     * @throws NullPointerException is the given parameter is null
     */
    public void setElectronDonationModelSetting(IMoleculeFragmenter.ElectronDonationModelOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        //synchronisation with aromaticity model instance done in overridden set() function of the property
        this.electronDonationModelSetting.set(anOption);
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
     * Sets the SMILES generator setting, defining which form of smiles generator is to be created.
     *
     * @param anOption a constant from the SmilesGeneratorOption enum
     * @throws NullPointerException if the given parameter is null
     */
    public void setSmilesGeneratorSetting(ScaffoldGeneratorFragmenter.SmilesGeneratorOption anOption) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOption, "Given option name is null.");
        this.smilesGeneratorSetting.set(anOption);
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
     * @param anOption a constant from the FragmentationType enum
     * @throws NullPointerException if the given parameter is null
     */
    public void setFragmentationTypeSetting(ScaffoldGeneratorFragmenter.FragmentationTypeOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given type of fragmentation to remove is null.");
        this.fragmentationTypeSetting.set(anOption);
    }

    /**
     *Sets the SideChain setting, defining whether only scaffolds, only side chains or both are to be generated.
     *
     * @param anOption a constant from the SideChainOption enum
     * @throws NullPointerException if the given parameter is null
     */
    public void setSideChainSetting(ScaffoldGeneratorFragmenter.SideChainOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given type of side chain option is null.");
        this.sideChainSetting.set(anOption);
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
     * Returns the currently set option for the side chain.
     *
     * @return enum constant of the set option
     */
    public ScaffoldGeneratorFragmenter.SideChainOption getSideChainSetting() {
        return (ScaffoldGeneratorFragmenter.SideChainOption) this.sideChainSetting.get();
    }

    /**
     * Returns the property object of the side chain setting that can be used to configure this setting.
     * Property that has a constant from SideChainOption enum as value.
     *
     * @return property object of the returned side chain setting
     */
    public SimpleIDisplayEnumConstantProperty sideChainSettingProperty() {
        return this.sideChainSetting;
    }

    /**
     * Returns the currently set option for the fragmentation type.
     *
     * @return enum constant of the set option
     */
    public ScaffoldGeneratorFragmenter.FragmentationTypeOption getFragmentationTypeSetting() {
        return (ScaffoldGeneratorFragmenter.FragmentationTypeOption) this.fragmentationTypeSetting.get();
    }

    /**
     * Returns the property object of the fragmentation type setting that can be used to configure this setting.
     * Property that has a constant from FragmentationTypeOption enum as value.
     *
     * @return property object of the returned fragmentation type setting
     */
    public SimpleIDisplayEnumConstantProperty fragmentationTypeSettingProperty() {
        return this.fragmentationTypeSetting;
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
     * Returns the currently set option for the SMILES generator.
     *
     * @return enum constant of the set option
     */
    public ScaffoldGeneratorFragmenter.SmilesGeneratorOption getSmilesGeneratorSetting() {
        return (ScaffoldGeneratorFragmenter.SmilesGeneratorOption) this.smilesGeneratorSetting.get();
    }

    /**
     * Returns the property object of the returned smiles generator setting that can be used to configure this setting.
     *
     * @return property object of the returned scaffold mode setting
     */
    public SimpleIDisplayEnumConstantProperty smilesGeneratorSettingProperty() {
        return this.smilesGeneratorSetting;
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
     * @return enum constant of the set option
     */
    public ScaffoldGeneratorFragmenter.SGFragmenterScaffoldModeOption getScaffoldModeSetting() {
        return (SGFragmenterScaffoldModeOption) this.scaffoldModeSetting.get();
    }

    /**
     * Returns the property object of the returned scaffold mode setting that can be used to configure this setting.
     *
     * @return property object of the returned scaffold mode setting
     */
    public SimpleIDisplayEnumConstantProperty scaffoldModeSettingProperty() {
        return this.scaffoldModeSetting;
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
     * A property that has a constant from the IMoleculeFragmenter.CycleFinderOption enum as value.
     *
     * @return property object of the cycle finder setting
     */
    public SimpleIDisplayEnumConstantProperty cycleFinderSettingProperty() {
        return this.cycleFinderSetting;
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
        return ScaffoldGeneratorFragmenter.ALGORITHM_NAME;
    }

    @Override
    public String getFragmentationAlgorithmDisplayName() {
        return Message.get("ScaffoldGeneratorFragmenter.displayName");
    }

    @Override
    public FragmentSaturationOption getFragmentSaturationSetting() {
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
        ScaffoldGeneratorFragmenter tmpCopy = new ScaffoldGeneratorFragmenter();
        tmpCopy.setFragmentSaturationSetting((IMoleculeFragmenter.FragmentSaturationOption) this.fragmentSaturationSetting.get());
        tmpCopy.setScaffoldModeSetting((SGFragmenterScaffoldModeOption) this.scaffoldModeSetting.get());
        tmpCopy.setDetermineAromaticitySetting(this.determineAromaticitySetting.get());
        tmpCopy.setCycleFinderSetting((IMoleculeFragmenter.CycleFinderOption) this.cycleFinderSetting.get());
        tmpCopy.setElectronDonationModelSetting((IMoleculeFragmenter.ElectronDonationModelOption) this.electronDonationModelSetting.get());
        tmpCopy.setSmilesGeneratorSetting((ScaffoldGeneratorFragmenter.SmilesGeneratorOption) this.smilesGeneratorSetting.get());
        tmpCopy.setRuleSevenAppliedSetting(this.ruleSevenAppliedSetting.get());
        tmpCopy.setRetainOnlyHybridisationAtAromaticBondsSetting(this.retainOnlyHybridisationsAtAromaticBondsSetting.get());
        tmpCopy.setFragmentationTypeSetting((ScaffoldGeneratorFragmenter.FragmentationTypeOption) this.fragmentationTypeSetting.get());
        tmpCopy.setSideChainSetting((ScaffoldGeneratorFragmenter.SideChainOption) this.sideChainSetting.get());
        return tmpCopy;
    }

    @Override
    public void restoreDefaultSettings() {
        this.fragmentSaturationSetting.set(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT);
        this.scaffoldModeSetting.set(ScaffoldGeneratorFragmenter.SCAFFOLD_MODE_OPTION_DEFAULT);
        this.determineAromaticitySetting.set(ScaffoldGenerator.DETERMINE_AROMATICITY_SETTING_DEFAULT);
        this.cycleFinderSetting.set(ScaffoldGeneratorFragmenter.CYCLE_FINDER_OPTION_DEFAULT);
        this.setCycleFinderSetting(ScaffoldGeneratorFragmenter.CYCLE_FINDER_OPTION_DEFAULT);
        this.electronDonationModelSetting.set(ScaffoldGeneratorFragmenter.Electron_Donation_MODEL_OPTION_DEFAULT);
        this.smilesGeneratorSetting.set(ScaffoldGeneratorFragmenter.SMILES_GENERATOR_OPTION_DEFAULT);
        this.ruleSevenAppliedSetting.set(ScaffoldGenerator.RULE_SEVEN_APPLIED_SETTING_DEFAULT);
        this.retainOnlyHybridisationsAtAromaticBondsSetting.set(ScaffoldGenerator.RETAIN_ONLY_HYBRIDISATIONS_AT_AROMATIC_BONDS_SETTING_DEFAULT);
        this.setFragmentationTypeSetting(ScaffoldGeneratorFragmenter.FRAGMENTATION_TYPE_OPTION_DEFAULT);
        this.setSideChainSetting(ScaffoldGeneratorFragmenter.SIDE_CHAIN_OPTION_DEFAULT);
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
        // note that Scaffold Generator detects aromaticity in the input molecule internally
        try {
            /*Generate side chains*/
            if (this.sideChainSetting.get().equals(ScaffoldGeneratorFragmenter.SideChainOption.ONLY_SIDE_CHAINS) ||
                    this.sideChainSetting.get().equals(ScaffoldGeneratorFragmenter.SideChainOption.BOTH)) {
                boolean tmpSaturateWithHydrogen = this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION);
                tmpSideChainList = this.scaffoldGeneratorInstance.getSideChains(tmpMoleculeClone, tmpSaturateWithHydrogen);
                /*Add side chain Property*/
                for (IAtomContainer tmpSideChain : tmpSideChainList) {
                    tmpSideChain.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                            ScaffoldGeneratorFragmenter.FRAGMENT_CATEGORY_SIDE_CHAIN_VALUE);
                }
            }
            /*Return only the side chains*/
            if (this.sideChainSetting.get().equals(ScaffoldGeneratorFragmenter.SideChainOption.ONLY_SIDE_CHAINS)) {
                return tmpSideChainList;
            }
            /*Decomposition according to the Schuffenhauer rules*/
            if (this.fragmentationTypeSetting.get().equals(ScaffoldGeneratorFragmenter.FragmentationTypeOption.SCHUFFENHAUER)) {
                List<IAtomContainer> tmpFragmentList = this.scaffoldGeneratorInstance.applySchuffenhauerRules(tmpMoleculeClone);
                /*Set fragment category property*/
                boolean tmpIsFirstFragment = true;
                for (IAtomContainer tmpFragment : tmpFragmentList) {
                    /*First fragment is the scaffold*/
                    if (tmpIsFirstFragment) {
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
            if (this.fragmentationTypeSetting.get().equals(ScaffoldGeneratorFragmenter.FragmentationTypeOption.ENUMERATIVE)) {
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
            if (this.fragmentationTypeSetting.get().equals(ScaffoldGeneratorFragmenter.FragmentationTypeOption.SCAFFOLD_ONLY)) {
                boolean tmpSaturateWithHydrogen = this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION);
                IAtomContainer tmpScaffold = this.scaffoldGeneratorInstance.getScaffold(tmpMoleculeClone, tmpSaturateWithHydrogen);
                //Set Scaffold Property
                tmpScaffold.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                        ScaffoldGeneratorFragmenter.FRAGMENT_CATEGORY_SCAFFOLD_VALUE);
                tmpReturnList.add(tmpScaffold);
            }
            /*dissect scaffold into rings*/
            if (this.fragmentationTypeSetting.get().equals(ScaffoldGeneratorFragmenter.FragmentationTypeOption.RING_DISSECTION)) {
                boolean tmpSaturateWithHydrogen = this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION);
                IAtomContainer tmpScaffold = this.scaffoldGeneratorInstance.getScaffold(tmpMoleculeClone, tmpSaturateWithHydrogen);
                tmpScaffold.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                        ScaffoldGeneratorFragmenter.FRAGMENT_CATEGORY_SCAFFOLD_VALUE);
                tmpReturnList.add(tmpScaffold);
                List<IAtomContainer> tmpFragmentList = this.scaffoldGeneratorInstance.getRings(tmpMoleculeClone, tmpSaturateWithHydrogen);
                for (IAtomContainer tmpFragment : tmpFragmentList) {
                    tmpFragment.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                            ScaffoldGeneratorFragmenter.FRAGMENT_CATEGORY_PARENT_SCAFFOLD_VALUE);
                    tmpReturnList.add(tmpFragment);
                }
            }
        } catch (Exception anException) {
            throw new IllegalArgumentException("An error occurred during fragmentation: " + anException.toString() + " Molecule Name: " + aMolecule.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
        }
        tmpReturnList.addAll(tmpSideChainList);
        /*Remove all empty fragments*/
        if (!tmpReturnList.isEmpty()) {
            for (int i = 0; i < tmpReturnList.size(); i++) {
                IAtomContainer tmpReturnMolecule = tmpReturnList.get(i);
                if (tmpReturnMolecule.isEmpty()){
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
                this.electronDonationInstance = Aromaticity.Model.CDK_AtomTypes;
                break;
            case IMoleculeFragmenter.ElectronDonationModelOption.DAYLIGHT:
                this.electronDonationInstance = Aromaticity.Model.Daylight;
                break;
            case IMoleculeFragmenter.ElectronDonationModelOption.CDK_ALLOWING_EXOCYCLIC:
                this.electronDonationInstance = ElectronDonation.cdkAllowingExocyclic();
                break;
            case IMoleculeFragmenter.ElectronDonationModelOption.CDK_1X:
                this.electronDonationInstance = Aromaticity.Model.CDK_1x;
                break;
            case IMoleculeFragmenter.ElectronDonationModelOption.CDK_2X:
                this.electronDonationInstance = Aromaticity.Model.CDK_2x;
                break;
            case IMoleculeFragmenter.ElectronDonationModelOption.MDL:
                this.electronDonationInstance = Aromaticity.Model.Mdl;
                break;
            case IMoleculeFragmenter.ElectronDonationModelOption.OPEN_SMILES:
                this.electronDonationInstance = Aromaticity.Model.OpenSmiles;
                break;
            case IMoleculeFragmenter.ElectronDonationModelOption.PI_BONDS:
                this.electronDonationInstance = Aromaticity.Model.PiBonds;
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
                throw new IllegalArgumentException("Undefined SMILES generator option.");
        }
    }
    //</editor-fold>
}
