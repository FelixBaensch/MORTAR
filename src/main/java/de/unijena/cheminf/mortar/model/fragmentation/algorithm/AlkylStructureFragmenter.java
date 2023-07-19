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

import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.io.Importer;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.CollectionUtil;
import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;

import javafx.beans.property.Property;
//needed for currently disabled Future Settings
//import javafx.beans.property.SimpleBooleanProperty;
//import javafx.beans.property.SimpleIntegerProperty;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.graph.invariant.ConjugatedPiSystemsDetector;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IElement;
import org.openscience.cdk.interfaces.IRingSet;
import org.openscience.cdk.ringsearch.RingSearch;
import org.openscience.cdk.tools.CDKHydrogenAdder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Java class implementing an algorithm for detection and fragmentation of alkyl
 * structures in MORTAR using the CDK.
 *
 * TODO: 19.07.2023 -future settings -> complex enums to attach values to dropdown
 *                  -preserveRingSystemMaxSetting restrictions (<0 nonsense)
 *                  -pseudo atom handling (*-atoms)
 *
 *
 * @author Maximilian Rottmann (maximilian.rottmann@studmail.w-hs.de)
 * @version 1.1.1.0
 */
public class AlkylStructureFragmenter implements IMoleculeFragmenter{
    // for future settings, currently disabled, search for "Future Settings" in order to see every future option
    //    //<editor-fold desc="Public Enums">
    //    /**
    //     * Enum for options concerning maximum fragment length of carbohydrate chain created by fragmenter.
    //     */
    //    public enum ChainFragmentLengthOption {
    //        /**
    //         * Maximum fragment length defined as alkane chain Methane
    //         */
    //        METHANE,
    //        /**
    //         * Maximum fragment length defined as alkane chain Ethane
    //         */
    //        ETHANE,
    //        /**
    //         * Maximum fragment length defined as alkane chain Propane
    //         */
    //        PROPANE,
    //        /**
    //         * Maximum fragment length defined as alkane chain Butane
    //         */
    //        BUTANE
    //    }
    //    /**
    //     * Enum for maximum size options for rings to be preserved by the algorithm.
    //     */
    //    public enum PreserveRingMaxSizeOption {
    //        /**
    //         * Maximum cyclo-alkane size defined as Cyclo-Propane
    //         */
    //        CYCLO_PROPANE,
    //        /**
    //         * Maximum cyclo-alkane size defined as Cyclo-Butane
    //         */
    //        CYCLO_BUTANE,
    //        /**
    //         * Maximum cyclo-alkane size defined as Cyclo-Pentane
    //         */
    //        CYCLO_PENTANE,
    //        /**
    //         * Maximum cyclo-alkane size defined as Cyclo-Hexane
    //         */
    //        CYCLO_HEXANE
    //    }
    //    //</editor-fold>
    //
    //<editor-fold desc="Public Static Final Class Variables">
    /**
     * Name of the fragmenter.
     */
    public static final String ALGORITHM_NAME = "Alkyl Fragmenter";
    // for future settings, currently disabled
    //    //<editor-fold desc="Future Settings">
    //    //
    //    /**
    //     * Default option for maximum fragment length of carbohydrate chain, set to methane.
    //     */
    //    public static final ChainFragmentLengthOption CHAIN_FRAGMENT_LENGTH_OPTION = ChainFragmentLengthOption.METHANE;
    //    /**
    //     * Default option for spiro carbon dissection.
    //     */
    //    public static final boolean DISSECT_SPIRO_CARBON_OPTION_DEFAULT = false;
    //    /**
    //     * Default option for ring dissection.
    //     */
    //    public static final boolean DISSECT_RINGS_OPTION_DEFAULT = false;
    //    /**
    //     * Default option for maximum size of preserved rings.
    //     */
    //    public static final PreserveRingMaxSizeOption PRESERVE_RING_MAX_SIZE_OPTION = PreserveRingMaxSizeOption.CYCLO_HEXANE;
    //    /**
    //     * Default option for ring system preservation.
    //     */
    //    public static final boolean PRESERVE_RING_SYSTEM_OPTION_DEFAULT = true;
    //    /**
    //     * Default option for maximum rings contained in preserved ring system.
    //     */
    //    public static final int PRESERVE_RING_SYSTEM_MAX_OPTION_DEFAULT = 1;
    //    //</editor-fold>
    //
    /**
     * Key for an internal index property, used in uniquely identifying atoms during fragmentation.
     */
    public static final String INTERNAL_ATOM_INDEX_PROPERTY_KEY = "ASF.ATOM_INDEX";
    /**
     * Key for an internal index property, used in uniquely identifying bonds during fragmentation.
     */
    public static final String INTERNAL_BOND_INDEX_PROPERTY_KEY = "ASF.BOND_INDEX";
    /**
     * Key for an internal boolean property, used in identifying whether an atom or bond has been placed in the
     * corresponding fragmentation array. It is further used during fragmentation.
     */
    public static final String INTERNAL_FRAGMENTATION_PLACEMENT_KEY = "ASF.FRAGMENTATION_PLACEMENT";
    //ToDo: for future fragmentation: properties for single rings, ring systems and conjugated pi systems
    //</editor-fold>
    //
    //<editor-fold desc="Private Final Class Variables">
    /**
     * A property that has a constant fragment hydrogen saturation setting.
     */
    private final SimpleEnumConstantNameProperty fragmentSaturationSetting;
    // for future Settings, currently disabled
    //    //<editor-fold desc="Future Settings">
    //    /**
    //     * A property that has a constant name from the ChainFragmentLengthOption enum as value.
    //     */
    //    private final SimpleEnumConstantNameProperty chainFragmentLengthSetting;
    //    /**
    //     * Boolean property whether spiro carbons should be dissected.
    //     */
    //    private final SimpleBooleanProperty dissectSpiroCarbonSetting;
    //    /**
    //     * Boolean property whether rings should be dissected.
    //     */
    //    private final SimpleBooleanProperty dissectRingsSetting;
    //    /**
    //     * Enum property for maximum size of preserved rings.
    //     */
    //    private final SimpleEnumConstantNameProperty preserveRingMaxSizeSetting;
    //    /**
    //     * Boolean property whether ring systems should be preserved.
    //     */
    //    private final SimpleBooleanProperty preserveRingSystemSetting;
    //    /**
    //     * Integer property for maximum rings contained in preserved ring system.
    //     */
    //    private final SimpleIntegerProperty preserveRingSystemMaxSetting;
    //    //</editor-fold>
    //
    /**
     * Map to store pairs of {@literal <setting name, tooltip text>}.
     */
    private final HashMap<String, String> settingNameTooltipTextMap;
    /**
     * All settings of this fragmenter, encapsulated in JavaFX properties for binding to GUI.
     */
    private final List<Property> settings;
    /**
     * Logger of this class.
     */
    private static final Logger logger = Logger.getLogger(AlkylStructureFragmenter.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="Constructor">
    /**
     * Constructor, all settings are initialised with their respective default values.
     */
    public AlkylStructureFragmenter(){
        int tmpSettingsNameTooltipNumber = 7;
        int tmpInitialCapacitySettingsNameTooltipHashMap = CollectionUtil.calculateInitialHashCollectionCapacity(
                tmpSettingsNameTooltipNumber,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.settingNameTooltipTextMap = new HashMap<>(tmpInitialCapacitySettingsNameTooltipHashMap, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.fragmentSaturationSetting = new SimpleEnumConstantNameProperty(this, "Fragment Saturation Setting",
                IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT.name(), IMoleculeFragmenter.FragmentSaturationOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    AlkylStructureFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        this.settingNameTooltipTextMap.put(this.fragmentSaturationSetting.getName(),
                Message.get("AlkylStructureFragmenter.fragmentSaturationSetting.tooltip"));
        //
        // <editor-fold desc="Future Settings">
        //        this.chainFragmentLengthSetting = new SimpleEnumConstantNameProperty(this, "Chain fragment length setting",
        //                AlkylStructureFragmenter.CHAIN_FRAGMENT_LENGTH_OPTION.name(),
        //                ChainFragmentLengthOption.class) {
        //            @Override
        //            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
        //                try {
        //                    //call to super.set() for parameter checks
        //                    super.set(newValue);
        //                } catch (NullPointerException | IllegalArgumentException anException) {
        //                    AlkylStructureFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
        //                    GuiUtil.guiExceptionAlert("Illegal Argument", "Illegal Argument was set",
        //                            anException.toString(), anException);
        //                    //re-throws the exception to properly reset the binding
        //                    throw anException;
        //                }
        //            }
        //        };
        //        this.settingNameTooltipTextMap.put(this.chainFragmentLengthSetting.getName(),
        //                Message.get("AlkylStructureFragmenter.chainFragmentLengthSetting.tooltip"));
        //        this.dissectSpiroCarbonSetting = new SimpleBooleanProperty(this, "Dissect spiro carbon setting",
        //                AlkylStructureFragmenter.DISSECT_SPIRO_CARBON_OPTION_DEFAULT);
        //        this.settingNameTooltipTextMap.put(this.dissectSpiroCarbonSetting.getName(),
        //                Message.get("AlkylStructureFragmenter.dissectSpiroCarbonSetting.tooltip"));
        //        this.dissectRingsSetting = new SimpleBooleanProperty(this, "Dissect rings setting",
        //                AlkylStructureFragmenter.DISSECT_RINGS_OPTION_DEFAULT);
        //        this.settingNameTooltipTextMap.put(this.dissectRingsSetting.getName(),
        //                Message.get("AlkylStructureFragmenter.dissectRingsSetting.tooltip"));
        //        this.preserveRingMaxSizeSetting = new SimpleEnumConstantNameProperty(this, "Preserve ring max size setting (preserves if Dissect rings setting is set false)",
        //                AlkylStructureFragmenter.PRESERVE_RING_MAX_SIZE_OPTION.name(),
        //                PreserveRingMaxSizeOption.class) {
        //            @Override
        //            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
        //                try {
        //                    super.set(newValue);
        //                } catch (NullPointerException | IllegalArgumentException anException) {
        //                    AlkylStructureFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
        //                    GuiUtil.guiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
        //                    //re-throws the exception to properly reset the binding
        //                    throw anException;
        //                }
        //            }
        //        };
        //        this.settingNameTooltipTextMap.put(this.preserveRingMaxSizeSetting.getName(),
        //                Message.get("AlkylStructureFragmenter.preserveRingMaxSizeSetting.tooltip"));
        //        this.preserveRingSystemSetting = new SimpleBooleanProperty(this, "Preserve ring system setting (preserves if Dissect rings setting is set false)",
        //                AlkylStructureFragmenter.PRESERVE_RING_SYSTEM_OPTION_DEFAULT);
        //        this.settingNameTooltipTextMap.put(this.preserveRingSystemSetting.getName(),
        //                Message.get("AlkylStructureFragmenter.preserveRingSystemSetting.tooltip"));
        //        this.preserveRingSystemMaxSetting = new SimpleIntegerProperty(this, "Preserve ring system up to maximum size",
        //                AlkylStructureFragmenter.PRESERVE_RING_SYSTEM_MAX_OPTION_DEFAULT);
        //        this.settingNameTooltipTextMap.put(this.preserveRingSystemMaxSetting.getName(),
        //                Message.get("AlkylStructureFragmenter.preserveRingSystemMaxSetting.tooltip"));
        //        //</editor-fold>
        //
        this.settings = new ArrayList<Property>(1);
        this.settings.add(this.fragmentSaturationSetting);
        // if future settings are to be enabled, check initCap of ArrayList
        //        //<editor-fold desc="Future Settings">
        //        this.settings.add(this.chainFragmentLengthSetting);
        //        this.settings.add(this.dissectSpiroCarbonSetting);
        //        this.settings.add(this.dissectRingsSetting);
        //        this.settings.add(this.preserveRingMaxSizeSetting);
        //        this.settings.add(this.preserveRingSystemSetting);
        //        this.settings.add(this.preserveRingSystemMaxSetting);
        //        //</editor-fold>
        //
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public Properties Get">
    @Override
    public List<Property> settingsProperties() {
       return this.settings;
    }

    @Override
    public Map<String, String> getSettingNameToTooltipTextMap() {
        return settingNameTooltipTextMap;
    }

    @Override
    public String getFragmentationAlgorithmName() {
        return AlkylStructureFragmenter.ALGORITHM_NAME;
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
    //</editor-fold>
    //
    //<editor-fold desc="Public Properties Set">
    @Override
    public void setFragmentSaturationSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given saturation option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name
        FragmentSaturationOption tmpConstant = FragmentSaturationOption.valueOf(anOptionName);
        this.fragmentSaturationSetting.set(tmpConstant.name());
    }

    @Override
    public void setFragmentSaturationSetting(FragmentSaturationOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given saturation option is null.");
        this.fragmentSaturationSetting.set(anOption.name());
    }
    //
    //    //<editor-fold desc="Future Settings, currently disabled">
    //    /**
    //     * Sets the enum option for maximum length of fragment chain.
    //     *
    //     * @param anOptionName the enum option name to use
    //     * @throws NullPointerException if the given option is null
    //     * @throws IllegalArgumentException if the given option does not exist
    //     */
    //    public void setChainFragmentLengthSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
    //        Objects.requireNonNull(anOptionName, "Given option is null.");
    //        //throws IllegalArgumentException if the given name does not match a constant name in the enum
    //        ChainFragmentLengthOption tmpConstant = ChainFragmentLengthOption.valueOf(anOptionName);
    //        this.setChainFragmentLengthSetting(tmpConstant);
    //    }
    //    /**
    //     * Sets the option for maximum length of fragment chain.
    //     *
    //     * @param anOption the option to use
    //     * @throws NullPointerException if given option is null
    //     */
    //    public void setChainFragmentLengthSetting(ChainFragmentLengthOption anOption) throws NullPointerException {
    //        Objects.requireNonNull(anOption, "Given option is null.");
    //        this.chainFragmentLengthSetting.set(anOption.name());
    //    }
    //    /**
    //     * Sets the boolean option for spiro carbon dissection.
    //     *
    //     * @param aBoolean the boolean option to use
    //     */
    //    public void setDissectSpiroCarbonSetting(boolean aBoolean) {
    //        this.dissectSpiroCarbonSetting.set(aBoolean);
    //    }
    //    /**
    //     * Sets the boolean option for ring dissection.
    //     *
    //     * @param aBoolean the boolean option to use
    //     */
    //    public void setDissectRingsSetting(boolean aBoolean) {
    //        this.dissectRingsSetting.set(aBoolean);
    //    }
    //    /**
    //     * Sets the enum option for maximum size of preserved ring.
    //     *
    //     * @param anOptionName the enum option to use
    //     * @throws NullPointerException if given option is null
    //     * @throws IllegalArgumentException if given option does not exist
    //     */
    //    public void setPreserveRingMaxSizeSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
    //        Objects.requireNonNull(anOptionName, "Given option is null.");
    //        PreserveRingMaxSizeOption tmpConstant = PreserveRingMaxSizeOption.valueOf(anOptionName);
    //        this.setPreserveRingMaxSizeSetting(tmpConstant);
    //    }
    //    /**
    //     * Sets the option for maximum size of preserved ring.
    //     *
    //     * @param anOption the option to use
    //     * @throws NullPointerException if given option is null
    //     */
    //    public void setPreserveRingMaxSizeSetting(PreserveRingMaxSizeOption anOption) throws NullPointerException {
    //        Objects.requireNonNull(anOption, "Given option name is null.");
    //        this.preserveRingMaxSizeSetting.set(anOption.name());
    //    }
    //    /**
    //     * Sets boolean option for ring system preservation.
    //     *
    //     * @param aBoolean the boolean option to use
    //     */
    //    public void setPreserveRingSystemSetting(boolean aBoolean) {
    //        this.preserveRingSystemSetting.set(aBoolean);
    //    }
    //    /**
    //     * Sets the maximum of rings contained in preserved system.
    //     *
    //     * @param anInt the int option to use
    //     */
    //    public void setPreserveRingSystemMaxSetting(int anInt) {
    //        this.preserveRingSystemMaxSetting.set(anInt);
    //    }
    //    //</editor-fold>
    //
    //</editor-fold>
    //
    //<editor-fold desc="Public Methods">

    @Override
    public IMoleculeFragmenter copy() {
        AlkylStructureFragmenter tmpCopy = new AlkylStructureFragmenter();
        tmpCopy.setFragmentSaturationSetting(this.fragmentSaturationSetting.get());
        //
        //        //<editor-fold desc="Future Settings, currently disabled">
        //        tmpCopy.setChainFragmentLengthSetting(this.chainFragmentLengthSetting.get());
        //        tmpCopy.setDissectSpiroCarbonSetting(this.dissectSpiroCarbonSetting.get());
        //        tmpCopy.setDissectRingsSetting(this.dissectRingsSetting.get());
        //        tmpCopy.setPreserveRingMaxSizeSetting(this.preserveRingMaxSizeSetting.get());
        //        tmpCopy.setPreserveRingSystemSetting(this.preserveRingSystemSetting.get());
        //        tmpCopy.setPreserveRingSystemMaxSetting(this.preserveRingSystemMaxSetting.get());
        //        //</editor-fold>
        //
        return tmpCopy;
    }

    @Override
    public void restoreDefaultSettings() {
        this.fragmentSaturationSetting.set(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT.name());
        //
        //        //<editor-fold desc="Future Settings, currently disabled">
        //        this.chainFragmentLengthSetting.set(AlkylStructureFragmenter.CHAIN_FRAGMENT_LENGTH_OPTION.name());
        //        this.dissectSpiroCarbonSetting.set(AlkylStructureFragmenter.DISSECT_SPIRO_CARBON_OPTION_DEFAULT);
        //        this.dissectRingsSetting.set(AlkylStructureFragmenter.DISSECT_RINGS_OPTION_DEFAULT);
        //        this.preserveRingMaxSizeSetting.set(AlkylStructureFragmenter.PRESERVE_RING_MAX_SIZE_OPTION.name());
        //        this.preserveRingSystemSetting.set(AlkylStructureFragmenter.PRESERVE_RING_SYSTEM_OPTION_DEFAULT);
        //        this.preserveRingSystemMaxSetting.set(AlkylStructureFragmenter.PRESERVE_RING_SYSTEM_MAX_OPTION_DEFAULT);
        //        //</editor-fold>
        //
    }
    //</editor-fold>
    //
    //<editor-fold desc="Fragmentation">

    @Override
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule)
            throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        //
        //<editor-fold desc="Molecule Cloning, Property and Arrays Set" defaultstate="collapsed">
        IAtomContainer tmpClone = aMolecule.clone();
        IAtom[] tmpAtomArray = new IAtom[tmpClone.getAtomCount()];
        IBond[] tmpBondArray = new IBond[tmpClone.getBondCount()];
        int tmpAlkylSFAtomIndex = 0;
        int tmpAlkylSFBondIndex = 0;
        IAtomContainerSet tmpFragments = new AtomContainerSet();
        for (IAtom tmpAtom: tmpClone.atoms()) {
            if (tmpAtom != null) {
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ATOM_INDEX_PROPERTY_KEY, tmpAlkylSFAtomIndex);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_FRAGMENTATION_PLACEMENT_KEY, true);
                tmpAtomArray[tmpAlkylSFAtomIndex] = tmpAtom;
                tmpAlkylSFAtomIndex ++;
            }
        }
        for (IBond tmpBond: tmpClone.bonds()) {
            if (tmpBond != null) {
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_BOND_INDEX_PROPERTY_KEY, tmpAlkylSFBondIndex);
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_FRAGMENTATION_PLACEMENT_KEY, true);
                tmpBondArray[tmpAlkylSFBondIndex] = tmpBond;
                tmpAlkylSFBondIndex ++;
            }
        }
        //</editor-fold>
        //
        //<editor-fold desc="Ring System Detection" defaultstate="collapsed">
        try {
            RingSearch tmpRingSearch = new RingSearch(tmpClone);
            List<IAtomContainer> tmpFusedList = tmpRingSearch.fusedRingFragments();
            if (!tmpFusedList.isEmpty()) {
                for (int tmpFusedCount = 0; tmpFusedCount < tmpFusedList.size(); tmpFusedCount++) {
                    for (IAtom tmpFusedAtom: tmpFusedList.get(tmpFusedCount).atoms()) {
                        int tmpAtomInteger = tmpFusedAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ATOM_INDEX_PROPERTY_KEY);
                        tmpAtomArray[tmpAtomInteger].setProperty(AlkylStructureFragmenter.INTERNAL_FRAGMENTATION_PLACEMENT_KEY, false);
                    }
                    for (IBond tmpFusedBond: tmpFusedList.get(tmpFusedCount).bonds()) {
                        int tmpBondInteger = tmpFusedBond.getProperty(AlkylStructureFragmenter.INTERNAL_BOND_INDEX_PROPERTY_KEY);
                        tmpBondArray[tmpBondInteger].setProperty(AlkylStructureFragmenter.INTERNAL_FRAGMENTATION_PLACEMENT_KEY, false);
                    }
                }
            }
        } catch (Exception anException) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING, anException.toString() + " MoleculeID: " + tmpClone.getID(), anException);
            throw new IllegalArgumentException("Unexpected error occurred during fragmentation of molecule: "
                    + tmpClone.getID() + ", at fused ringsearch: " + anException.toString());
        }
        //</editor-fold>
        //
        //<editor-fold desc="ConjugatedPiSystemsDetector" defaultstate="collapsed">
        try {
            IAtomContainerSet tmpConjugatedAtomContainerSet;
            tmpConjugatedAtomContainerSet = ConjugatedPiSystemsDetector.detect(tmpClone);
            //molecule mapping
            //iterate over every atomcontainer from ConjPiSystemsDetector output
            for (IAtomContainer tmpConjAtomContainer: tmpConjugatedAtomContainerSet.atomContainers()) {
                for (IAtom tmpConjAtom: tmpConjAtomContainer.atoms()) {
                    int tmpAtomInteger = tmpConjAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ATOM_INDEX_PROPERTY_KEY);
                    tmpAtomArray[tmpAtomInteger].setProperty(AlkylStructureFragmenter.INTERNAL_FRAGMENTATION_PLACEMENT_KEY, false);
                }
                for (IBond tmpConjBond: tmpConjAtomContainer.bonds()) {
                    int tmpBondInteger = tmpConjBond.getProperty(AlkylStructureFragmenter.INTERNAL_BOND_INDEX_PROPERTY_KEY);
                    tmpBondArray[tmpBondInteger].setProperty(AlkylStructureFragmenter.INTERNAL_FRAGMENTATION_PLACEMENT_KEY, false);
                }
            }
        } catch (Exception anException) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING,
                    anException + " MoleculeID: " + tmpClone.getID(), anException);
            throw new IllegalArgumentException("Unexpected error occurred during fragmentation of molecule: "
                    + tmpClone.getID() + ", at conjugated pi systems detector: " + anException.toString());
        }
        //</editor-fold>
        //
        //<editor-fold desc="CycleFinder (Single Rings)" defaultstate="collapsed">
        CycleFinder tmpMCBCycleFinder = Cycles.mcb();
        IRingSet tmpMCBCyclesSet;
        try {
            Cycles tmpMCBCycles = tmpMCBCycleFinder.find(tmpClone);
            tmpMCBCyclesSet = tmpMCBCycles.toRingSet();
            for (IAtomContainer tmpContainer: tmpMCBCyclesSet.atomContainers()) {
                for (IAtom tmpSingleRingAtom: tmpContainer.atoms()) {
                    int tmpAtomInteger = tmpSingleRingAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ATOM_INDEX_PROPERTY_KEY);
                    tmpAtomArray[tmpAtomInteger].setProperty(AlkylStructureFragmenter.INTERNAL_FRAGMENTATION_PLACEMENT_KEY, false);
                }
                for (IBond tmpSingleRingBond: tmpContainer.bonds()) {
                    int tmpBondInteger = tmpSingleRingBond.getProperty(AlkylStructureFragmenter.INTERNAL_BOND_INDEX_PROPERTY_KEY);
                    tmpBondArray[tmpBondInteger].setProperty(AlkylStructureFragmenter.INTERNAL_FRAGMENTATION_PLACEMENT_KEY, false);
                }
            }
        } catch (Exception anException) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING,
                    anException + " MoleculeID: " + tmpClone.getID(), anException);
            throw new IllegalArgumentException("Unexpected error occurred during fragmentation of molecule: "
                    + tmpClone.getID() + ", at cyclefinder : " + anException.toString());
        }
        //</editor-fold>
        //
        //<editor-fold desc="Fragment Extraction" defaultstate="collapsed">
        try {
            IAtomContainer tmpRingFragmentationContainer = new AtomContainer();
            IAtomContainer tmpChainFragmentationContainer = new AtomContainer();
            //atom extraction
            //superior performance compared to normal for iteration over Array length
            for (IAtom tmpAtom : tmpAtomArray) {
                if (tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_FRAGMENTATION_PLACEMENT_KEY)) {
                    tmpChainFragmentationContainer.addAtom(tmpAtom);
                } else {
                    tmpRingFragmentationContainer.addAtom(tmpAtom);
                }
            }
            //bond extraction
            //superior performance compared to normal for iteration over Array length
            for (IBond tmpBond : tmpBondArray) {
                if (tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_FRAGMENTATION_PLACEMENT_KEY)) {
                    if (tmpBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_FRAGMENTATION_PLACEMENT_KEY))
                        if (tmpBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_FRAGMENTATION_PLACEMENT_KEY)) {
                            tmpChainFragmentationContainer.addBond(tmpBond);
                        }
                }
                else {
                    tmpRingFragmentationContainer.addBond(tmpBond);
                }
            }
            //
            //<editor-fold desc="Connectivity Checking">
            try {
                if (!tmpRingFragmentationContainer.isEmpty()) {
                    if (!ConnectivityChecker.isConnected(tmpRingFragmentationContainer)) {
                        IAtomContainerSet tmpContainerSet = ConnectivityChecker.partitionIntoMolecules(tmpRingFragmentationContainer);
                        for (IAtomContainer tmpContainer: tmpContainerSet.atomContainers()) {
                            tmpFragments.addAtomContainer(tmpContainer);
                        }
                    } else {
                        tmpFragments.addAtomContainer(tmpRingFragmentationContainer);
                    }
                }
                if (!tmpChainFragmentationContainer.isEmpty()) {
                    if (!ConnectivityChecker.isConnected(tmpChainFragmentationContainer)) {
                        IAtomContainerSet tmpContainerSet = ConnectivityChecker.partitionIntoMolecules(tmpChainFragmentationContainer);
                        for (IAtomContainer tmpContainer : tmpContainerSet.atomContainers()) {
                            tmpFragments.addAtomContainer(tmpContainer);
                        }
                    } else {
                        tmpFragments.addAtomContainer(tmpChainFragmentationContainer);
                    }
                }
            } catch (Exception anException) {
                AlkylStructureFragmenter.this.logger.log(Level.WARNING, anException + " Connectivity Checking failed at molecule: " + tmpClone.getID(), anException);
                throw new IllegalArgumentException("An Error occurred during Connectivity Checking: " + anException.toString() +
                        ": " + tmpClone.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
            }
            //</editor-fold>
            //
        } catch (Exception anException) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING,
                    anException + "extraction failed at molecule: " + tmpClone.getID(), anException);
            throw new IllegalArgumentException("Unexpected error occurred during fragmentation of molecule: "
                    + tmpClone.getID() + ", at fragment extraction: " + anException.toString());
        }
        //</editor-fold>
        //
        //<editor-fold desc="Hydrogen Saturation" defaultstate="collapsed">
        List<IAtomContainer> tmpProcessedFragments = new ArrayList<>(tmpFragments.getAtomContainerCount());
        try {
            if (!tmpFragments.isEmpty() && tmpFragments != null) {
                CDKHydrogenAdder tmpAdder = CDKHydrogenAdder.getInstance(tmpFragments.getAtomContainer(0).getBuilder());
                for (IAtomContainer tmpAtomContainer: tmpFragments.atomContainers()) {
                    if (this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION.name())) {
                        try {
                            tmpAdder.addImplicitHydrogens(tmpAtomContainer);
                            tmpProcessedFragments.add(tmpAtomContainer);
                        } catch (CDKException anException) {
                            AlkylStructureFragmenter.this.logger.log(Level.WARNING, anException
                                    + " Unable to add Implicit Hydrogen at MoleculeID: " + tmpClone.getID());
                            throw new CDKException("Unexpected error occurred during implicit hydrogen adding at " +
                                    "hydrogen saturation of molecule: " + tmpClone.getID() + ", " + anException.toString(), anException);
                        }
                    } else {
                        tmpProcessedFragments.add(tmpAtomContainer);
                    }
                }
            }
        } catch (Exception anException) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING, anException
                    + "Error during hydrogen saturation at MoleculeID: " + tmpClone.getID());
            throw new IllegalArgumentException("Unexpected error occurred during fragmentation of molecule: "
                    + tmpClone.getID() + ", at hydrogen saturation: " + anException.toString(), anException);
        }
        //</editor-fold>
        //
        return tmpProcessedFragments;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Pre-Fragmentation Tasks">
    /**
     * Returns true if the given molecule cannot be fragmented by the algorithm.
     * If the molecule is null: true is returned, no exception thrown and fragmentation is skipped for the given molecule.
     *
     * <p>
     *     Checks the given IAtomContainer aMolecule for non-carbon and non-hydrogen atoms and returns true if
     *     non-conforming atoms are found, otherwise false is returned and the molecule can be fragmented.
     * </p>
     *
     * @param aMolecule the molecule to check
     * @return true or false, depending on atom check
     */
    @Override
    public boolean shouldBeFiltered(IAtomContainer aMolecule) {
        if (Objects.isNull(aMolecule) || aMolecule.isEmpty()) {
            return true;
        }
        boolean tmpShouldBeFiltered = true;
        try {
            for (IAtom tmpAtom : aMolecule.atoms()) {
                if (tmpAtom.getAtomicNumber() != IElement.H && tmpAtom.getAtomicNumber() != IElement.C) {
                    return true;
                } else {
                    tmpShouldBeFiltered = false;
                }
            }
            return tmpShouldBeFiltered;
        } catch (Exception anException) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING,
                    anException + " Molecule ID: " + aMolecule.getID());
            return true;
        }
    }

    /**
     * Method for determining if given molecule needs preprocessing.
     * Always returns false, as no preprocessing is currently needed.
     *
     * @param aMolecule the molecule to check
     * @return currently always false
     * @throws NullPointerException
     */
    @Override
    public boolean shouldBePreprocessed(IAtomContainer aMolecule) throws NullPointerException {
        return false;
    }

    @Override
    public boolean canBeFragmented(IAtomContainer aMolecule) throws NullPointerException {
        //throws NullpointerException if molecule is null
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        boolean tmpShouldBeFiltered = this.shouldBeFiltered(aMolecule);
        boolean tmpShouldBePreprocessed = this.shouldBePreprocessed(aMolecule);
        return !tmpShouldBeFiltered && !tmpShouldBePreprocessed;
    }

    /**
     * Method for applying special preprocessing steps before fragmenting given molecule.
     *
     * @param aMolecule the molecule to preprocess
     * @return aMolecule, unchanged molecule as no preprocessing is currently needed
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws CloneNotSupportedException
     */
    @Override
    public IAtomContainer applyPreprocessing(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        Objects.requireNonNull(aMolecule, "Given molecule is null");
        return aMolecule;
    }
    //</editor-fold>
    //
}