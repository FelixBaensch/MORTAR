/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2021  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas-schaub@uni-jena.de)
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

import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;
import de.unijena.cheminf.scaffoldGenerator.ScaffoldGenerator;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper class that makes the <a href="https://github.com/Julian-Z98/ScaffoldGenerator">ScaffoldGenerator</a>
 * available in MORTAR.
 *
 * @author Julian Zander, Jonas Schaub (zanderjulian@gmx.de, jonas.schaub@uni-jena.de)
 */
public class ScaffoldGeneratorFragmenter implements IMoleculeFragmenter {

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
        SCAFFOLD_ONLY;
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
    public static final ScaffoldGeneratorFragmenter.CycleFinderOption CYCLE_FINDER_OPTION_DEFAULT = ScaffoldGeneratorFragmenter.CycleFinderOption.CDK_AROMATIC_SET;

    /**
     * Cycle finder algorithm that is used should the set option cause an IntractableException.
     */
    public static final CycleFinder AUXILIARY_CYCLE_FINDER = Cycles.cdkAromaticSet();

    /**
     * Default electron donation model for aromaticity detection.
     */
    public static final ScaffoldGeneratorFragmenter.ElectronDonationModelOption Electron_Donation_MODEL_OPTION_DEFAULT
            = ScaffoldGeneratorFragmenter.ElectronDonationModelOption.CDK;

    /**
     * Default SmiFlavor for the default SmilesGenerator.
     */
    public static final ScaffoldGeneratorFragmenter.SmilesGeneratorOption SMILES_GENERATOR_OPTION_DEFAULT = SmilesGeneratorOption.UNIQUE_WITHOUT_STEREO;

    /**
     * Default fragmentation type.
     */
    public static final FragmentationTypeOption FRAGMENTATION_TYPE_OPTION_DEFAULT = FragmentationTypeOption.ENUMERATIVE_FRAGMENTATION;
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

    /**
     * Instance of the ScaffoldGenerator class used to do generate fragments or scaffolds from the molecules.
     */
    private ScaffoldGenerator scaffoldGeneratorInstance;
    //</editor-fold>
    //
    //<editor-fold desc="Private final variables">
    /**
     * Property wrapping the 'scaffold mode' setting of the SF.
     */
    private final SimpleEnumConstantNameProperty scaffoldModeSetting;

    /**
     * Property wrapping the 'determine aromaticity' setting of the SF.
     */
    private final SimpleBooleanProperty determineAromaticitySetting;

    /**
     * Property wrapping the 'smiles generator' setting of the SF.
     */
    private final SimpleEnumConstantNameProperty smilesGeneratorSetting;

    /**
     * Property wrapping the 'rule seven applied' setting of the SF.
     */
    private final SimpleBooleanProperty ruleSevenAppliedSetting;

    /**
     * Property wrapping the 'retain only hybridisations at aromatic bonds setting' setting of the SF.
     */
    private final SimpleBooleanProperty retainOnlyHybridisationsAtAromaticBondsSetting;

    /**
     * Property that has a constant name from FragmentationTypeSetting enum as value.
     */
    private final SimpleEnumConstantNameProperty fragmentationTypeSetting;

    /**
     * A property that has a constant name from the CycleFinderOption enum as value.
     */
    private final SimpleEnumConstantNameProperty cycleFinderSetting;

    /**
     * A property that has a constant name from the IMoleculeFragmenter.FragmentSaturationOption enum as value.
     */
    private final SimpleEnumConstantNameProperty fragmentSaturationSetting;

    /**
     * A property that has a constant name from the ElectronDonationModelOption enum as value.
     */
    private final SimpleEnumConstantNameProperty electronDonationModelSetting;

    /**
     * All settings of this fragmenter, encapsulated in JavaFX properties for binding in GUI.
     */
    private final List<Property> settings;

    /**
     * Logger of this class.
     */
    private final Logger logger = Logger.getLogger(ScaffoldGeneratorFragmenter.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="Constructor">
    /**
     * Constructor, all settings are initialised with their default values as declared in the respective public constants.
     */
    public ScaffoldGeneratorFragmenter() {
        this.scaffoldGeneratorInstance = new ScaffoldGenerator();
        this.fragmentSaturationSetting = new SimpleEnumConstantNameProperty(this, "Fragment saturation setting",
                IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT.name(), IMoleculeFragmenter.FragmentSaturationOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ScaffoldGeneratorFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.GuiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        this.scaffoldModeSetting = new SimpleEnumConstantNameProperty(this, "Scaffold mode setting",
                this.scaffoldGeneratorInstance.getScaffoldModeSetting().name(), ScaffoldGenerator.ScaffoldModeOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //valueOf() throws IllegalArgumentException
                    ScaffoldGenerator.ScaffoldModeOption tmpEnumConstant = ScaffoldGenerator.ScaffoldModeOption.valueOf(newValue);
                    ScaffoldGeneratorFragmenter.this.scaffoldGeneratorInstance.setScaffoldModeSetting(tmpEnumConstant);
                } catch (IllegalArgumentException | NullPointerException anException) {
                    ScaffoldGeneratorFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.GuiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                super.set(newValue);
            }
        };
        this.determineAromaticitySetting = new SimpleBooleanProperty(this,
                "Determine aromaticity setting", this.scaffoldGeneratorInstance.isAromaticityDetermined()) {
            @Override
            public void set(boolean newValue) {
                //throws no exceptions
                ScaffoldGeneratorFragmenter.this.scaffoldGeneratorInstance.setDetermineAromaticitySetting(newValue);
                super.set(newValue);
            }
        };
        //note: cycle finder and electron donation model have to be set prior to setting the aromaticity model!
        this.cycleFinderSetting = new SimpleEnumConstantNameProperty(this, "Cycle finder algorithm setting",
                ScaffoldGeneratorFragmenter.CYCLE_FINDER_OPTION_DEFAULT.name(),
                ScaffoldGeneratorFragmenter.CycleFinderOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ScaffoldGeneratorFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.GuiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                //throws no exception if super.set() throws no exception
                ScaffoldGeneratorFragmenter.this.setCycleFinderInstance(ScaffoldGeneratorFragmenter.CycleFinderOption.valueOf(newValue));
                Aromaticity tmpAromaticity = new Aromaticity(ScaffoldGeneratorFragmenter.this.electronDonationInstance, ScaffoldGeneratorFragmenter.this.cycleFinderInstance);
                ScaffoldGeneratorFragmenter.this.scaffoldGeneratorInstance.setAromaticityModelSetting(tmpAromaticity);
            }
        };
        this.setCycleFinderInstance(ScaffoldGeneratorFragmenter.CycleFinderOption.valueOf(this.cycleFinderSetting.get()));
        this.electronDonationModelSetting = new SimpleEnumConstantNameProperty(this, "Electron donation model setting",
                ScaffoldGeneratorFragmenter.Electron_Donation_MODEL_OPTION_DEFAULT.name(),
                ScaffoldGeneratorFragmenter.ElectronDonationModelOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ScaffoldGeneratorFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.GuiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                //throws no exception if super.set() throws no exception
                ScaffoldGeneratorFragmenter.this.setElectronDonationInstance(ScaffoldGeneratorFragmenter.ElectronDonationModelOption.valueOf(newValue));
                Aromaticity tmpAromaticity = new Aromaticity(ScaffoldGeneratorFragmenter.this.electronDonationInstance, ScaffoldGeneratorFragmenter.this.cycleFinderInstance);
                ScaffoldGeneratorFragmenter.this.scaffoldGeneratorInstance.setAromaticityModelSetting(tmpAromaticity);
            }
        };
        this.setElectronDonationInstance(ScaffoldGeneratorFragmenter.ElectronDonationModelOption.valueOf(this.electronDonationModelSetting.get()));
        this.smilesGeneratorSetting = new SimpleEnumConstantNameProperty(this, "Smiles generator setting",
                ScaffoldGeneratorFragmenter.SMILES_GENERATOR_OPTION_DEFAULT.name(), ScaffoldGeneratorFragmenter.SmilesGeneratorOption.class) {
            @Override
            public  void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ScaffoldGeneratorFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.GuiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                ScaffoldGeneratorFragmenter.this.setSmilesGeneratorInstance(ScaffoldGeneratorFragmenter.SmilesGeneratorOption.valueOf(newValue));
            }
        };
        this.ruleSevenAppliedSetting = new SimpleBooleanProperty(this,
                "Rule seven setting", this.scaffoldGeneratorInstance.isRuleSevenApplied()) {
            @Override
            public void set(boolean newValue) {
                //throws no exceptions
                ScaffoldGeneratorFragmenter.this.scaffoldGeneratorInstance.setRuleSevenAppliedSetting(newValue);
                super.set(newValue);
            }
        };
        this.retainOnlyHybridisationsAtAromaticBondsSetting = new SimpleBooleanProperty(this,
                "Retain only hybridisations at aromatic bonds setting", this.scaffoldGeneratorInstance.areOnlyHybridisationsAtAromaticBondsRetained()) {
            @Override
            public void set(boolean newValue) {
                //throws no exceptions
                ScaffoldGeneratorFragmenter.this.scaffoldGeneratorInstance.setRetainOnlyHybridisationsAtAromaticBondsSetting(newValue);
                super.set(newValue);
            }
        };
        this.fragmentationTypeSetting = new SimpleEnumConstantNameProperty(this, "Fragmentation type setting",
                ScaffoldGeneratorFragmenter.FRAGMENTATION_TYPE_OPTION_DEFAULT.name(), ScaffoldGeneratorFragmenter.FragmentationTypeOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ScaffoldGeneratorFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.GuiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        this.settings = new ArrayList<>(9);
        this.settings.add(this.fragmentationTypeSetting);
        this.settings.add(this.fragmentSaturationSetting);
        this.settings.add(this.scaffoldModeSetting);
        this.settings.add(this.determineAromaticitySetting);
        this.settings.add(this.electronDonationModelSetting);
        this.settings.add(this.cycleFinderSetting);
        this.settings.add(this.smilesGeneratorSetting);
        this.settings.add(this.ruleSevenAppliedSetting);
        this.settings.add(this.retainOnlyHybridisationsAtAromaticBondsSetting);
    }
    //</editor-fold>
    //
    //<editor-fold desc="set">
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
     * @param anOptionName name of a constant from the ElectronDonationModelOption enum
     * @throws NullPointerException if the given string is null
     * @throws IllegalArgumentException if the given string is not an enum constant name
     */
    public void setElectronDonationModelSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        ScaffoldGeneratorFragmenter.ElectronDonationModelOption tmpConstant = ScaffoldGeneratorFragmenter.ElectronDonationModelOption.valueOf(anOptionName);
        this.setElectronDonationModelSetting(tmpConstant);
    }

    /**
     * Sets the electron donation model setting. The set electron donation model is used for aromaticity detection in
     * preprocessing together with the set cycle finder algorithm.
     *
     * @param anOption a constant from the ElectronDonationModelOption enum
     * @throws NullPointerException is the given parameter is null
     */
    public void setElectronDonationModelSetting(ScaffoldGeneratorFragmenter.ElectronDonationModelOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        //synchronisation with aromaticity model instance done in overridden set() function of the property
        this.electronDonationModelSetting.set(anOption.name());
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
     * Sets the smiles generator, defining which smiles generator should be used.
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
     * Sets the smiles generator setting, defining which form of smiles generator is to be created.
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
        Objects.requireNonNull(anOption, "Given type of sugars to remove is null.");
        this.fragmentationTypeSetting.set(anOption.name());
    }
    //</editor-fold>
    //
    //<editor-fold desc="get">
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
     *
     * @return property object of the returned scaffold mode setting
     */
    public SimpleEnumConstantNameProperty fragmentationTypeSettingProperty() {
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
     * Returns the current state of rule seven applied setting.
     *
     * @return true if rule seven should be applied
     */
    public boolean getRuleSevenAppliedSetting() {
        return this.ruleSevenAppliedSetting.get();
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
     * Returns the current state of determine aromaticity setting.
     *
     * @return true if aromaticity should be determined
     */
    public boolean getDetermineAromaticitySetting() {
        return this.determineAromaticitySetting.get();
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
    public ErtlFunctionalGroupsFinderFragmenter.ElectronDonationModelOption getElectronDonationModelSettingConstant() {
        return ErtlFunctionalGroupsFinderFragmenter.ElectronDonationModelOption.valueOf(this.electronDonationModelSetting.get());
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
    public ErtlFunctionalGroupsFinderFragmenter.CycleFinderOption getCycleFinderSettingConstant() {
        return ErtlFunctionalGroupsFinderFragmenter.CycleFinderOption.valueOf(this.cycleFinderSetting.get());
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
        return tmpCopy;
    }

    @Override
    public void restoreDefaultSettings() {
        this.fragmentSaturationSetting.set(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT.name());
        this.scaffoldModeSetting.set(ScaffoldGenerator.SCAFFOLD_MODE_OPTION_DEFAULT.name());
        this.determineAromaticitySetting.set(ScaffoldGenerator.DETERMINE_AROMATICITY_SETTING_DEFAULT);
        this.cycleFinderSetting.set(ScaffoldGeneratorFragmenter.CYCLE_FINDER_OPTION_DEFAULT.name());
        this.setCycleFinderSetting(CycleFinderOption.valueOf(this.cycleFinderSetting.get()));
        this.electronDonationModelSetting.set(ScaffoldGeneratorFragmenter.Electron_Donation_MODEL_OPTION_DEFAULT.name());
        this.smilesGeneratorSetting.set(ScaffoldGeneratorFragmenter.SMILES_GENERATOR_OPTION_DEFAULT.name());
        this.ruleSevenAppliedSetting.set(ScaffoldGenerator.RULE_SEVEN_APPLIED_SETTING_DEFAULT);
        this.retainOnlyHybridisationsAtAromaticBondsSetting.set(ScaffoldGenerator.RETAIN_ONLY_HYBRIDISATIONS_AT_AROMATIC_BONDS_SETTING_DEFAULT);
        this.setFragmentationTypeSetting(ScaffoldGeneratorFragmenter.FRAGMENTATION_TYPE_OPTION_DEFAULT.name());
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
        IAtomContainer tmpMoleculeClone = aMolecule.clone();
        try {
            /*Decomposition according to the Schuffenhauer rules*/
            if(this.fragmentationTypeSetting.get().equals(FragmentationTypeOption.SCHUFFENHAUER_FRAGMENTATION.toString())) {
                tmpReturnList = this.scaffoldGeneratorInstance.applySchuffenhauerRules(tmpMoleculeClone);
            }
            /*Enumerative decomposition*/
            if(this.fragmentationTypeSetting.get().equals(FragmentationTypeOption.ENUMERATIVE_FRAGMENTATION.toString())) {
                tmpReturnList = this.scaffoldGeneratorInstance.applyEnumerativeRemoval(tmpMoleculeClone);
            }
            /*Generate the scaffold only*/
            if(this.fragmentationTypeSetting.get().equals(FragmentationTypeOption.SCAFFOLD_ONLY.toString())) {
                /*Scaffold without saturation*/
                if(this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.NO_SATURATION.toString())) {
                    tmpReturnList.add(this.scaffoldGeneratorInstance.getScaffold(tmpMoleculeClone, false));
                } else { /*Scaffold with saturation*/
                    tmpReturnList.add(this.scaffoldGeneratorInstance.getScaffold(tmpMoleculeClone, true));
                }
            }
        } catch (Exception anException) {
            throw new IllegalArgumentException("An error occurred during fragmentation: " + anException.toString());
        }
        return tmpReturnList;
    }

    @Override
    public boolean shouldBeFiltered(IAtomContainer aMolecule) {
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
        return true;
    }

    @Override
    public IAtomContainer applyPreprocessing(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        IAtomContainer tmpClonedMolecule;
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
    private void setCycleFinderInstance(ScaffoldGeneratorFragmenter.CycleFinderOption anOption) throws NullPointerException {
        //Developer comment: the switch way is used instead of having the CycleFinder objects as variables of the enum constants
        // to not have static objects becoming bottlenecks in parallelization.
        Objects.requireNonNull(anOption, "Given option is null.");
        switch (anOption) {
            case ALL:
                this.cycleFinderInstance = Cycles.or(Cycles.all(), ScaffoldGeneratorFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case MCB:
                this.cycleFinderInstance = Cycles.or(Cycles.mcb(), ScaffoldGeneratorFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case RELEVANT:
                this.cycleFinderInstance = Cycles.or(Cycles.relevant(), ScaffoldGeneratorFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case ESSENTIAL:
                this.cycleFinderInstance = Cycles.or(Cycles.essential(), ScaffoldGeneratorFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case EDGE_SHORT:
                this.cycleFinderInstance = Cycles.or(Cycles.edgeShort(), ScaffoldGeneratorFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case VERTEX_SHORT:
                this.cycleFinderInstance = Cycles.or(Cycles.vertexShort(), ScaffoldGeneratorFragmenter.AUXILIARY_CYCLE_FINDER);
                break;
            case TRIPLET_SHORT:
                this.cycleFinderInstance = Cycles.or(Cycles.tripletShort(), ScaffoldGeneratorFragmenter.AUXILIARY_CYCLE_FINDER);
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
    private void setElectronDonationInstance(ScaffoldGeneratorFragmenter.ElectronDonationModelOption anOption) throws NullPointerException {
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
     * Calling method needs to update the SmilesGenerator!
     */
    private void setSmilesGeneratorInstance(ScaffoldGeneratorFragmenter.SmilesGeneratorOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        switch (anOption) {
            case UNIQUE_WITH_STEREO:
                this.smilesGeneratorInstance = new SmilesGenerator(SmiFlavor.Unique | SmiFlavor.UseAromaticSymbols);
                break;
            case UNIQUE_WITHOUT_STEREO:
                this.smilesGeneratorInstance = new SmilesGenerator(SmiFlavor.Unique);
                break;
            default:
                throw new IllegalArgumentException("Undefined electron donation model option.");
        }
    }
    //</editor-fold>
}

