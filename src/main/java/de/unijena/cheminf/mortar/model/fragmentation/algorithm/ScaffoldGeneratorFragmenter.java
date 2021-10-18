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
import org.openscience.cdk.exception.CDKException;
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

    //<editor-fold desc="Enum SmilesGeneratorOption">
    public static enum SmilesGeneratorOption {

        UNIQUE_WITHOUT_STEREO,

        UNIQUE_WITH_STEREO;
    }
    //</editor-fold>

    //<editor-fold desc="Enum FragmentationTypeOption">
    public static enum FragmentationTypeOption {
        ENUMERATIVE_FRAGMENTATION,

        SCHUFFENHAUER_FRAGMENTATION;
    }
    //</editor-fold>

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
    //CDK richtig als default?
    public static final ScaffoldGeneratorFragmenter.ElectronDonationModelOption Electron_Donation_MODEL_OPTION_DEFAULT
            = ScaffoldGeneratorFragmenter.ElectronDonationModelOption.CDK;

    private static final ScaffoldGeneratorFragmenter.SmilesGeneratorOption SMILES_GENERATOR_OPTION_DEFAULT = SmilesGeneratorOption.UNIQUE_WITH_STEREO;

    public static final FragmentationTypeOption FRAGMENTATION_TYPE_OPTION_DEFAULT = FragmentationTypeOption.ENUMERATIVE_FRAGMENTATION;

    /**
     * A property that has a constant name from the IMoleculeFragmenter.FragmentSaturationOption enum as value.
     */
    private final SimpleEnumConstantNameProperty fragmentSaturationSetting;

    //</editor-fold>

    //<editor-fold desc="Private variables">
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
     * The aromaticity model used for preprocessing prior to FG extraction. Constructed from the set electron donation
     * model and cycle finder algorithm.
     */
    private Aromaticity aromaticityModelInstance;
    //</editor-fold>

    //<editor-fold desc="Private final variables">
    /**
     * Logger of this class.
     */
    private final Logger logger = Logger.getLogger(SugarRemovalUtilityFragmenter.class.getName());
    private final ScaffoldGenerator scaffoldGeneratorInstance;
    private final ArrayList<Property> settings;
    private final SimpleEnumConstantNameProperty scaffoldModeSetting;
    private final SimpleBooleanProperty determineAromaticitySetting;
    private final SimpleEnumConstantNameProperty smilesGeneratorSetting;
    private final SimpleBooleanProperty ruleSevenAppliedSetting;
    private final SimpleBooleanProperty retainOnlyHybridisationsAtAromaticBondsSetting;
    private final SimpleEnumConstantNameProperty fragmentationTypeSetting;

    /**
     * A property that has a constant name from the CycleFinderOption enum as value.
     */
    private final SimpleEnumConstantNameProperty cycleFinderSetting;

    /**
     * A property that has a constant name from the ElectronDonationModelOption enum as value.
     */
    private final SimpleEnumConstantNameProperty electronDonationModelSetting;
    //</editor-fold>

    //<editor-fold desc="Constructor">
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
        this.scaffoldModeSetting = new SimpleEnumConstantNameProperty(this, "scaffold mode option setting",
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
                "Determine aromaticity", this.scaffoldGeneratorInstance.isAromaticityDetermined()) {
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
                ScaffoldGeneratorFragmenter.this.setAromaticityInstance(
                        ScaffoldGeneratorFragmenter.this.electronDonationInstance,
                        ScaffoldGeneratorFragmenter.this.cycleFinderInstance);
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
                ScaffoldGeneratorFragmenter.this.setAromaticityInstance(
                        ScaffoldGeneratorFragmenter.this.electronDonationInstance,
                        ScaffoldGeneratorFragmenter.this.cycleFinderInstance);
            }
        };
        this.setElectronDonationInstance(ScaffoldGeneratorFragmenter.ElectronDonationModelOption.valueOf(this.electronDonationModelSetting.get()));
        this.setAromaticityInstance(
                ScaffoldGeneratorFragmenter.this.electronDonationInstance,
                ScaffoldGeneratorFragmenter.this.cycleFinderInstance
        );
        this.smilesGeneratorSetting = new SimpleEnumConstantNameProperty(this, "Smiles Generator Setting",
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
                "rule seven applied", this.scaffoldGeneratorInstance.isRuleSevenApplied()) {
            @Override
            public void set(boolean newValue) {
                //throws no exceptions
                ScaffoldGeneratorFragmenter.this.scaffoldGeneratorInstance.setRuleSevenAppliedSetting(newValue);
                super.set(newValue);
            }
        };
        this.retainOnlyHybridisationsAtAromaticBondsSetting = new SimpleBooleanProperty(this,
                "retain only hybridisations at aromatic bonds", this.scaffoldGeneratorInstance.areOnlyHybridisationsAtAromaticBondsRetained()) {
            @Override
            public void set(boolean newValue) {
                //throws no exceptions
                ScaffoldGeneratorFragmenter.this.scaffoldGeneratorInstance.setRetainOnlyHybridisationsAtAromaticBondsSetting(newValue);
                super.set(newValue);
            }
        };
        this.fragmentationTypeSetting = new SimpleEnumConstantNameProperty(this, "Fragmentation type",
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
        this.settings = new ArrayList<>(); //Change Capacity
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

    //<editor-fold desc="set">
    public void setScaffoldModeSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        ScaffoldGenerator.ScaffoldModeOption tmpConstant = ScaffoldGenerator.ScaffoldModeOption.valueOf(anOptionName);
        this.setScaffoldModeSetting(tmpConstant);
    }

    public void setScaffoldModeSetting(ScaffoldGenerator.ScaffoldModeOption anOption) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOption, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        this.scaffoldModeSetting.set(anOption.name());
    }

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

    public void setSmilesGeneratorSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        SmilesGeneratorOption tmpConstant = SmilesGeneratorOption.valueOf(anOptionName);
        this.setSmilesGeneratorSetting(tmpConstant);
    }

    public void setSmilesGeneratorSetting(SmilesGeneratorOption anOption) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOption, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        this.smilesGeneratorSetting.set(anOption.name());
    }

    public void setRuleSevenAppliedSetting(boolean aBoolean) {
        //synchronisation with ScaffoldGenerator instance done in overridden set() function of the property
        this.ruleSevenAppliedSetting.set(aBoolean);
    }

    public void setRetainOnlyHybridisationAtAromaticBondsSetting(boolean aBoolean) {
        //synchronisation with ScaffoldGenerator instance done in overridden set() function of the property
        this.retainOnlyHybridisationsAtAromaticBondsSetting.set(aBoolean);
    }

    /**
     * Sets only the instance, not the property! So it is safe for the property to call this method when overriding set().
     */
    private void setAromaticityInstance(ElectronDonation anElectronDonation, CycleFinder aCycleFinder) throws NullPointerException {
        Objects.requireNonNull(anElectronDonation, "Given electron donation model is null.");
        Objects.requireNonNull(aCycleFinder, "Given cycle finder algorithm is null.");
        this.aromaticityModelInstance = new Aromaticity(anElectronDonation, aCycleFinder);
    }

    public void setFragmentationTypeSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        FragmentationTypeOption tmpConstant = FragmentationTypeOption.valueOf(anOptionName);
        this.setFragmentationTypeSetting(tmpConstant);
    }

    public void setFragmentationTypeSetting(FragmentationTypeOption aFragmentationTypeOption) throws NullPointerException {
        Objects.requireNonNull(aFragmentationTypeOption, "Given type of sugars to remove is null.");
        this.fragmentationTypeSetting.set(aFragmentationTypeOption.name());
    }

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

    //<editor-fold desc="IMoleculeFragmenter methods">
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
        //this.aromaticityModel is set in the method
        this.setAromaticityInstance(this.electronDonationInstance, this.cycleFinderInstance);
        this.smilesGeneratorSetting.set(ScaffoldGeneratorFragmenter.SMILES_GENERATOR_OPTION_DEFAULT.name());
        this.ruleSevenAppliedSetting.set(ScaffoldGenerator.RULE_SEVEN_APPLIED_SETTING_DEFAULT);
        this.retainOnlyHybridisationsAtAromaticBondsSetting.set(ScaffoldGenerator.RETAIN_ONLY_HYBRIDISATIONS_AT_AROMATIC_BONDS_SETTING_DEFAULT);
        this.setFragmentationTypeSetting(ScaffoldGeneratorFragmenter.FRAGMENTATION_TYPE_OPTION_DEFAULT.name());
    }

    @Override
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        //<editor-fold desc="Parameter tests">
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        boolean tmpCanBeFragmented = this.canBeFragmented(aMolecule);
        if (!tmpCanBeFragmented) {
            throw new IllegalArgumentException("Given molecule cannot be fragmented but should be filtered or preprocessed first.");
        }
        this.scaffoldGeneratorInstance.setAromaticityModelSetting(this.aromaticityModelInstance);
        this.scaffoldGeneratorInstance.setDetermineAromaticitySetting(this.determineAromaticitySetting.get());
        this.scaffoldGeneratorInstance.setSmilesGeneratorSetting(this.smilesGeneratorInstance);
        this.scaffoldGeneratorInstance.setRuleSevenAppliedSetting(this.ruleSevenAppliedSetting.get());
        this.scaffoldGeneratorInstance.setRetainOnlyHybridisationsAtAromaticBondsSetting(this.retainOnlyHybridisationsAtAromaticBondsSetting.get());
        this.scaffoldGeneratorInstance.setScaffoldModeSetting(ScaffoldGenerator.ScaffoldModeOption.valueOf(this.scaffoldModeSetting.get()));
        List<IAtomContainer> tmpReturnList = new ArrayList<>();
        IAtomContainer tmpMoleculeClone = aMolecule.clone();
        try {
            if(this.fragmentationTypeSetting.get().equals(FragmentationTypeOption.SCHUFFENHAUER_FRAGMENTATION.toString())) {
                tmpReturnList = this.scaffoldGeneratorInstance.applySchuffenhauerRules(tmpMoleculeClone);
            }
            if(this.fragmentationTypeSetting.get().equals(FragmentationTypeOption.ENUMERATIVE_FRAGMENTATION.toString())) {
                tmpReturnList = this.scaffoldGeneratorInstance.applyEnumerativeRemoval(tmpMoleculeClone);
            }
        } catch (CDKException e) {
            e.printStackTrace();
        }
        return tmpReturnList;
    }

    @Override
    public boolean shouldBeFiltered(IAtomContainer aMolecule) throws IllegalArgumentException, NullPointerException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        if (aMolecule.isEmpty()) {
            throw new IllegalArgumentException("Given molecule is empty.");
        }
        return false;
    }

    @Override
    public boolean shouldBePreprocessed(IAtomContainer aMolecule) throws IllegalArgumentException, NullPointerException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        if (aMolecule.isEmpty()) {
            throw new IllegalArgumentException("Given molecule is empty.");
        }
        return false;
    }

    @Override
    public boolean canBeFragmented(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        if (aMolecule.isEmpty()) {
            throw new IllegalArgumentException("Given molecule is empty.");
        }
        return true;
    }

    @Override
    public IAtomContainer applyPreprocessing(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        return null;
    }
    //</editor-fold>
}

