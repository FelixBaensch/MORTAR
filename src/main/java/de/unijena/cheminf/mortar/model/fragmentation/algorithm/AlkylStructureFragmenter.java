/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2023  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas-schaub@uni-jena.de)
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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.Intractable;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Java class implementing an algorithm for detection and fragmentation of alkyl
 * structures in MORTAR using the CDK.
 *
 * @author Maximilian Rottmann (maximilian.rottmann@studmail.w-hs.de)
 * @version 1.1.1.0
 */
public class AlkylStructureFragmenter implements IMoleculeFragmenter{
    //
    //<editor-fold desc="Public Enums">
    /**
     * Enum for options concerning maximum fragment length of carbohydrate chain created by fragmenter.
     */
    public enum ChainFragmentLengthOption {
        /**
         * Maximum fragment length defined as alkane chain Methane
         */
        METHANE,
        /**
         * Maximum fragment length defined as alkane chain Ethane
         */
        ETHANE,
        /**
         * Maximum fragment length defined as alkane chain Propane
         */
        PROPANE,
        /**
         * Maximum fragment length defined as alkane chain Butane
         */
        BUTANE
    }
    /**
     * Enum for maximum size options for rings to be preserved by the algorithm.
     */
    public enum PreserveRingMaxSizeOption {
        /**
         * Maximum cyclo-alkane size defined as Cyclo-Propane
         */
        CYCLO_PROPANE,
        /**
         * Maximum cyclo-alkane size defined as Cyclo-Butane
         */
        CYCLO_BUTANE,
        /**
         * Maximum cyclo-alkane size defined as Cyclo-Pentane
         */
        CYCLO_PENTANE,
        /**
         * Maximum cyclo-alkane size defined as Cyclo-Hexane
         */
        CYCLO_HEXANE
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public Static Final Class Variables">
    /**
     * Name of the fragmenter.
     * ToDo: change name of fragmenter everywhere it is used
     */
    public static final String ALGORITHM_NAME = "Alkyl Fragmenter";
    /**
     * Default option for maximum fragment length of carbohydrate chain, set to methane.
     */
    public static final ChainFragmentLengthOption CHAIN_FRAGMENT_LENGTH_OPTION = ChainFragmentLengthOption.METHANE;
    /**
     * Default option for spiro carbon dissection.
     */
    public static final boolean DISSECT_SPIRO_CARBON_OPTION_DEFAULT = false;
    /**
     * Default option for ring dissection.
     */
    public static final boolean DISSECT_RINGS_OPTION_DEFAULT = false;
    /**
     * Default option for maximum size of preserved rings.
     */
    public static final PreserveRingMaxSizeOption PRESERVE_RING_MAX_SIZE_OPTION = PreserveRingMaxSizeOption.CYCLO_HEXANE;
    /**
     * Default option for ring system preservation.
     */
    public static final boolean PRESERVE_RING_SYSTEM_OPTION_DEFAULT = true;
    /**
     * Default option for maximum rings contained in preserved ring system.
     */
    public static final int PRESERVE_RING_SYSTEM_MAX_OPTION_DEFAULT = 1;
    /**
     * Key for an internal index property, used in uniquely identifying atoms during fragmentation.
     */
    public static final String INTERNAL_ATOM_INDEX_PROPERTY_KEY = "ASF.ATOM_INDEX";
    /**
     * Key for an internal index property, used in uniquely identifying bonds during fragmentation.
     */
    public static final String INTERNAL_BOND_INDEX_PROPERTY_KEY = "ASF.Bond_INDEX";
    //</editor-fold>
    //
    //<editor-fold desc="Private Final Class Variables">
    /**
     * A property that has a constant fragment hydrogen saturation setting.
     */
    private final SimpleEnumConstantNameProperty fragmentSaturationSetting;
    /**
     * A property that has a constant name from the ChainFragmentLengthOption enum as value.
     */
    private final SimpleEnumConstantNameProperty chainFragmentLengthSetting;
    /**
     * Boolean property whether spiro carbons should be dissected.
     */
    private final SimpleBooleanProperty dissectSpiroCarbonSetting;
    /**
     * Boolean property whether rings should be dissected.
     */
    private final SimpleBooleanProperty dissectRingsSetting;
    /**
     * Enum property for maximum size of preserved rings.
     */
    private final SimpleEnumConstantNameProperty preserveRingMaxSizeSetting;
    /**
     * Boolean property whether ring systems should be preserved.
     */
    private final SimpleBooleanProperty preserveRingSystemSetting;
    /**
     * Integer property for maximum rings contained in preserved ring system.
     */
    private final SimpleIntegerProperty preserveRingSystemMaxSetting;
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
    private final Logger logger = Logger.getLogger(AlkylStructureFragmenter.class.getName());
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
        this.chainFragmentLengthSetting = new SimpleEnumConstantNameProperty(this, "Chain fragment length setting",
                AlkylStructureFragmenter.CHAIN_FRAGMENT_LENGTH_OPTION.name(),
                ChainFragmentLengthOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    AlkylStructureFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert("Illegal Argument", "Illegal Argument was set",
                            anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        this.settingNameTooltipTextMap.put(this.chainFragmentLengthSetting.getName(),
                Message.get("AlkylStructureFragmenter.chainFragmentLengthSetting.tooltip"));
        this.dissectSpiroCarbonSetting = new SimpleBooleanProperty(this, "Dissect spiro carbon setting",
                AlkylStructureFragmenter.DISSECT_SPIRO_CARBON_OPTION_DEFAULT);
        this.settingNameTooltipTextMap.put(this.dissectSpiroCarbonSetting.getName(),
                Message.get("AlkylStructureFragmenter.dissectSpiroCarbonSetting.tooltip"));
        this.dissectRingsSetting = new SimpleBooleanProperty(this, "Dissect rings setting",
                AlkylStructureFragmenter.DISSECT_RINGS_OPTION_DEFAULT);
        this.settingNameTooltipTextMap.put(this.dissectRingsSetting.getName(),
                Message.get("AlkylStructureFragmenter.dissectRingsSetting.tooltip"));
        this.preserveRingMaxSizeSetting = new SimpleEnumConstantNameProperty(this, "Preserve ring max size setting (preserves if Dissect rings setting is set false)",
                AlkylStructureFragmenter.PRESERVE_RING_MAX_SIZE_OPTION.name(),
                PreserveRingMaxSizeOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    AlkylStructureFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        this.settingNameTooltipTextMap.put(this.preserveRingMaxSizeSetting.getName(),
                Message.get("AlkylStructureFragmenter.preserveRingMaxSizeSetting.tooltip"));
        this.preserveRingSystemSetting = new SimpleBooleanProperty(this, "Preserve ring system setting (preserves if Dissect rings setting is set false)",
                AlkylStructureFragmenter.PRESERVE_RING_SYSTEM_OPTION_DEFAULT);
        this.settingNameTooltipTextMap.put(this.preserveRingSystemSetting.getName(),
                Message.get("AlkylStructureFragmenter.preserveRingSystemSetting.tooltip"));
        this.preserveRingSystemMaxSetting = new SimpleIntegerProperty(this, "Preserve ring system up to maximum size",
                AlkylStructureFragmenter.PRESERVE_RING_SYSTEM_MAX_OPTION_DEFAULT);
        this.settingNameTooltipTextMap.put(this.preserveRingSystemMaxSetting.getName(),
                Message.get("AlkylStructureFragmenter.preserveRingSystemMaxSetting.tooltip"));
        //
        this.settings = new ArrayList<Property>(7);
        this.settings.add(this.fragmentSaturationSetting);
        this.settings.add(this.chainFragmentLengthSetting);
        this.settings.add(this.dissectSpiroCarbonSetting);
        this.settings.add(this.dissectRingsSetting);
        this.settings.add(this.preserveRingMaxSizeSetting);
        this.settings.add(this.preserveRingSystemSetting);
        this.settings.add(this.preserveRingSystemMaxSetting);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public Properties Get">
    /**
     * Returns a list of all available settings represented by properties for the given fragmentation algorithm.
     *
     * @return list of settings represented by properties
     */
    @Override
    public List<Property> settingsProperties() {
       return this.settings;
    }
    /**
     * Returns a map containing descriptive texts (values) for the settings with the given names (keys) to be used as
     * tooltips in the GUI.
     *
     * @return map with tooltip texts
     */
    @Override
    public Map<String, String> getSettingNameToTooltipTextMap() {
        return settingNameTooltipTextMap;
    }
    /**
     * Returns a string representation of the algorithm name, e.g. "ErtlFunctionalGroupsFinder" or "Ertl algorithm".
     * The given name must be unique among the available fragmentation algorithms!
     *
     * @return algorithm name
     */
    @Override
    public String getFragmentationAlgorithmName() {
        return AlkylStructureFragmenter.ALGORITHM_NAME;
    }
    /**
     * Returns the currently set option for saturating free valences on returned fragment molecules.
     *
     * @return the set option
     */
    @Override
    public String getFragmentSaturationSetting() {
        return this.fragmentSaturationSetting.get();
    }
    /**
     * Returns the property representing the setting for fragment saturation.
     *
     * @return setting property for fragment saturation
     */
    @Override
    public SimpleEnumConstantNameProperty fragmentSaturationSettingProperty() {
        return this.fragmentSaturationSetting;
    }
    /**
     * Returns the currently set fragment saturation option as the respective enum constant.
     *
     * @return fragment saturation setting enum constant
     */
    @Override
    public FragmentSaturationOption getFragmentSaturationSettingConstant() {
        return FragmentSaturationOption.valueOf(this.fragmentSaturationSetting.get());
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public Properties Set">
    /**
     * Sets the option for saturating free valences on returned fragment molecules.
     *
     * @param anOptionName constant name (use name()) from FragmentSaturationOption enum
     * @throws NullPointerException     if the given name is null
     * @throws IllegalArgumentException if the given string does not represent an enum constant
     */
    @Override
    public void setFragmentSaturationSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given saturation option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name
        FragmentSaturationOption tmpConstant = FragmentSaturationOption.valueOf(anOptionName);
        this.fragmentSaturationSetting.set(tmpConstant.name());
    }
    /**
     * Sets the option for saturating free valences on returned fragment molecules.
     *
     * @param anOption the saturation option to use
     * @throws NullPointerException if the given option is null
     */
    @Override
    public void setFragmentSaturationSetting(FragmentSaturationOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given saturation option is null.");
        this.fragmentSaturationSetting.set(anOption.name());
    }
    /**
     * Sets the enum option for maximum length of fragment chain.
     *
     * @param anOptionName the enum option name to use
     * @throws NullPointerException if the given option is null
     * @throws IllegalArgumentException if the given option does not exist
     */
    public void setChainFragmentLengthSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        ChainFragmentLengthOption tmpConstant = ChainFragmentLengthOption.valueOf(anOptionName);
        this.setChainFragmentLengthSetting(tmpConstant);
    }
    /**
     * Sets the option for maximum length of fragment chain.
     *
     * @param anOption the option to use
     * @throws NullPointerException if given option is null
     */
    public void setChainFragmentLengthSetting(ChainFragmentLengthOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        this.chainFragmentLengthSetting.set(anOption.name());
    }
    /**
     * Sets the boolean option for spiro carbon dissection.
     *
     * @param aBoolean the boolean option to use
     */
    public void setDissectSpiroCarbonSetting(boolean aBoolean) {
        this.dissectSpiroCarbonSetting.set(aBoolean);
    }
    /**
     * Sets the boolean option for ring dissection.
     *
     * @param aBoolean the boolean option to use
     */
    public void setDissectRingsSetting(boolean aBoolean) {
        this.dissectRingsSetting.set(aBoolean);
    }
    /**
     * Sets the enum option for maximum size of preserved ring.
     *
     * @param anOptionName the enum option to use
     * @throws NullPointerException if given option is null
     * @throws IllegalArgumentException if given option does not exist
     */
    public void setPreserveRingMaxSizeSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option is null.");
        PreserveRingMaxSizeOption tmpConstant = PreserveRingMaxSizeOption.valueOf(anOptionName);
        this.setPreserveRingMaxSizeSetting(tmpConstant);
    }
    /**
     * Sets the option for maximum size of preserved ring.
     *
     * @param anOption the option to use
     * @throws NullPointerException if given option is null
     */
    public void setPreserveRingMaxSizeSetting(PreserveRingMaxSizeOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option name is null.");
        this.preserveRingMaxSizeSetting.set(anOption.name());
    }
    /**
     * Sets boolean option for ring system preservation.
     *
     * @param aBoolean the boolean option to use
     */
    public void setPreserveRingSystemSetting(boolean aBoolean) {
        this.preserveRingSystemSetting.set(aBoolean);
    }
    /**
     * Sets the maximum of rings contained in preserved system.
     *
     * @param anInt the int option to use
     */
    public void setPreserveRingSystemMaxSetting(int anInt) {
        this.preserveRingSystemMaxSetting.set(anInt);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public Methods">
    /**
     * Returns a new instance of the respective fragmenter with the same settings as this instance. Intended for
     * multi-threaded work where every thread needs its own fragmenter instance.
     *
     * @return new fragmenter instance with the same settings
     */
    @Override
    public IMoleculeFragmenter copy() {
        AlkylStructureFragmenter tmpCopy = new AlkylStructureFragmenter();
        tmpCopy.setFragmentSaturationSetting(this.fragmentSaturationSetting.get());
        tmpCopy.setChainFragmentLengthSetting(this.chainFragmentLengthSetting.get());
        tmpCopy.setDissectSpiroCarbonSetting(this.dissectSpiroCarbonSetting.get());
        tmpCopy.setDissectRingsSetting(this.dissectRingsSetting.get());
        tmpCopy.setPreserveRingMaxSizeSetting(this.preserveRingMaxSizeSetting.get());
        tmpCopy.setPreserveRingSystemSetting(this.preserveRingSystemSetting.get());
        tmpCopy.setPreserveRingSystemMaxSetting(this.preserveRingSystemMaxSetting.get());
        return tmpCopy;
    }
    /**
     * Restore all settings of the fragmenter to their default values.
     */
    @Override
    public void restoreDefaultSettings() {
        this.fragmentSaturationSetting.set(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT.name());
        this.chainFragmentLengthSetting.set(AlkylStructureFragmenter.CHAIN_FRAGMENT_LENGTH_OPTION.name());
        this.dissectSpiroCarbonSetting.set(AlkylStructureFragmenter.DISSECT_SPIRO_CARBON_OPTION_DEFAULT);
        this.dissectRingsSetting.set(AlkylStructureFragmenter.DISSECT_RINGS_OPTION_DEFAULT);
        this.preserveRingMaxSizeSetting.set(AlkylStructureFragmenter.PRESERVE_RING_MAX_SIZE_OPTION.name());
        this.preserveRingSystemSetting.set(AlkylStructureFragmenter.PRESERVE_RING_SYSTEM_OPTION_DEFAULT);
        this.preserveRingSystemMaxSetting.set(AlkylStructureFragmenter.PRESERVE_RING_SYSTEM_MAX_OPTION_DEFAULT);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Fragmentation">
    /**
     * Fragments a clone(!) of the given molecule according to the respective algorithm and returns the resulting fragments.
     *
     * @param aMolecule to fragment
     * @return a list of fragments (the list may be empty if no fragments are extracted, but the fragments should not be!)
     * @throws NullPointerException       if aMolecule is null
     * @throws IllegalArgumentException   if the given molecule cannot be fragmented but should be filtered or preprocessed
     * @throws CloneNotSupportedException if cloning the given molecule fails
     */
    @Override
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule)
            throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        //
        //<editor-fold desc="Parameter Checks" defaultstate="collapsed">
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        IAtomContainer tmpClone = aMolecule.clone();
        //</editor-fold>
        //
        //<editor-fold desc="Property and Arrays Set" defaultstate="collapsed">
        IAtom[] tmpAtomArray = new IAtom[tmpClone.getAtomCount()];
        IBond[] tmpBondArray = new IBond[tmpClone.getBondCount()];
        int tmpAlkylSFAtomIndexInt = 0;
        int tmpAlkylSFBondIndexInt = 0;
        IAtomContainerSet tmpFragments = new AtomContainerSet();
        for (IAtom tmpAtom: tmpClone.atoms()) {
            if (tmpAtom != null) {
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ATOM_INDEX_PROPERTY_KEY, tmpAlkylSFAtomIndexInt);
                tmpAtom.setFlag(CDKConstants.ISPLACED, true);
                tmpAtomArray[tmpAlkylSFAtomIndexInt] = tmpAtom;
                tmpAlkylSFAtomIndexInt ++;
            }
        }
        for (IBond tmpBond: tmpClone.bonds()) {
            if (tmpBond != null) {
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_BOND_INDEX_PROPERTY_KEY, tmpAlkylSFBondIndexInt);
                tmpBond.setFlag(CDKConstants.ISPLACED, true);
                tmpBondArray[tmpAlkylSFBondIndexInt] = tmpBond;
                tmpAlkylSFBondIndexInt ++;
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
                        tmpAtomArray[tmpAtomInteger].setFlag(CDKConstants.ISPLACED,false);
                    }
                    for (IBond tmpFusedBond: tmpFusedList.get(tmpFusedCount).bonds()) {
                        int tmpBondInteger = tmpFusedBond.getProperty(AlkylStructureFragmenter.INTERNAL_BOND_INDEX_PROPERTY_KEY);
                        tmpBondArray[tmpBondInteger].setFlag(CDKConstants.ISPLACED, false);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
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
                    tmpAtomArray[tmpAtomInteger].setFlag(CDKConstants.ISPLACED,false);
                }
                for (IBond tmpConjBond: tmpConjAtomContainer.bonds()) {
                    int tmpBondInteger = tmpConjBond.getProperty(AlkylStructureFragmenter.INTERNAL_BOND_INDEX_PROPERTY_KEY);
                    tmpBondArray[tmpBondInteger].setFlag(CDKConstants.ISPLACED,false);
                }
            }
        } catch (Exception anException) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING,
                    anException + " Molecule ID: " + tmpClone.getID());
            throw new RuntimeException(anException);
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
                    tmpAtomArray[tmpAtomInteger].setFlag(CDKConstants.ISPLACED,false);
                }
                for (IBond tmpSingleRingBond: tmpContainer.bonds()) {
                    int tmpBondInteger = tmpSingleRingBond.getProperty(AlkylStructureFragmenter.INTERNAL_BOND_INDEX_PROPERTY_KEY);
                    tmpBondArray[tmpBondInteger].setFlag(CDKConstants.ISPLACED,false);
                }
            }
        } catch (Intractable e) {
            throw new RuntimeException(e);
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
                if (tmpAtom.getFlag(CDKConstants.ISPLACED)) {
                    tmpChainFragmentationContainer.addAtom(tmpAtom);
                } else {
                    tmpRingFragmentationContainer.addAtom(tmpAtom);
                }
            }
            //bond extraction
            //superior performance compared to normal for iteration over Array length
            for (IBond tmpBond : tmpBondArray) {
                if (tmpBond.getFlag(CDKConstants.ISPLACED)) {
                    if ((tmpBond.getBegin().getFlag(CDKConstants.ISPLACED) && tmpBond.getEnd().getFlag(CDKConstants.ISPLACED))) {
                        tmpChainFragmentationContainer.addBond(tmpBond);
                    }
                }
                else {
                    tmpRingFragmentationContainer.addBond(tmpBond);
                }
            }
            //connectivity checking
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
            } catch (Exception e) {
                AlkylStructureFragmenter.this.logger.log(Level.WARNING, e + " Connectivity Checking failed");
                throw new IllegalArgumentException("An Error occurred during Connectivity Checking: " + e.toString() +
                        ": " + tmpClone.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
            }
        } catch (Exception e) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING, e + " Fragment Extraction failed");
            throw new RuntimeException(e);
        }
        //</editor-fold>
        //
        //<editor-fold desc="Hydrogen Saturation" defaultstate="collapsed">
        List<IAtomContainer> tmpProcessedFragments = new ArrayList<>(tmpFragments.getAtomContainerCount());
        try {
            if (!tmpFragments.isEmpty() && tmpFragments != null) {
                for (IAtomContainer tmpAtomContainer: tmpFragments.atomContainers()) {
                    if (this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION.name())) {
                        CDKHydrogenAdder tmpAdder = CDKHydrogenAdder.getInstance(tmpAtomContainer.getBuilder());
                        try {
                            tmpAdder.addImplicitHydrogens(tmpAtomContainer);
                            tmpProcessedFragments.add(tmpAtomContainer);
                        } catch (CDKException e) {
                            AlkylStructureFragmenter.this.logger.log(Level.WARNING, e + " Molecule ID: "
                                    + tmpAtomContainer.getID() + " Unable to add Implicit Hydrogen.");
                            throw new RuntimeException(e);
                        }
                    } else {
                        tmpProcessedFragments.add(tmpAtomContainer);
                    }
                }
            }
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
        //</editor-fold>
        //
        return tmpProcessedFragments;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Pre-Fragmentation Tasks">
    /**
     * Returns true if the given molecule cannot be fragmented by the respective algorithm, even after preprocessing.
     * If the molecule is null, true is returned and no exception thrown.
     *
     * <p>
     *     Checks the given IAtomContainer aMolecule for non-carbon and non-hydrogen atoms.
     * </p>
     *
     * @param aMolecule the molecule to check
     * @return true if the given molecule is not acceptable as input for the fragmentation algorithm, even if it would be
     * preprocessed
     */
    @Override
    public boolean shouldBeFiltered(IAtomContainer aMolecule) {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        boolean tmpShouldBeFiltered = true;
        try {
            for (IAtom tmpAtom : aMolecule.atoms()) {
                if (tmpAtom.getAtomicNumber() != IElement.H && tmpAtom.getAtomicNumber() != IElement.C) {
                    tmpShouldBeFiltered = true;
                    break;
                } else {
                    tmpShouldBeFiltered = false;
                }
            }
            return tmpShouldBeFiltered;
        } catch (Exception anException) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING,
                    anException + " Molecule ID: " + aMolecule.getID());
            return tmpShouldBeFiltered;
        }
    }
    /**
     * Returns true if the given molecule can be fragmented by the respective algorithm after preprocessing. Returns
     * false if the given molecule can be directly fragmented by the algorithm without preprocessing.
     * Does not check whether the molecule should be filtered! But throws an exception if it is null.
     *
     * @param aMolecule the molecule to check
     * @return true if the molecule needs to be preprocessed, false if it can be fragmented directly
     * @throws NullPointerException if the molecule is null
     */
    @Override
    public boolean shouldBePreprocessed(IAtomContainer aMolecule) throws NullPointerException {
        return false;
    }
    /**
     * Returns true only if the given molecule can be passed to the central fragmentation method without any preprocessing
     * and without causing an exception. If 'false' is returned, check the methods for filtering and preprocessing.
     *
     * @param aMolecule the molecule to check
     * @return true if the molecule can be directly fragmented
     * @throws NullPointerException if the molecule is null
     */
    @Override
    public boolean canBeFragmented(IAtomContainer aMolecule) throws NullPointerException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        boolean tmpShouldBeFiltered = this.shouldBeFiltered(aMolecule);
        boolean tmpShouldBePreprocessed = this.shouldBePreprocessed(aMolecule);
        return !tmpShouldBeFiltered && !tmpShouldBePreprocessed;
        //throws NullpointerException if molecule is null
    }
    /**
     * Applies the needed preprocessing for fragmentation to the given molecule. Throws an exception if the molecule
     * should be filtered.
     *
     * @param aMolecule the molecule to preprocess
     * @return a copy of the given molecule that has been preprocessed
     * @throws NullPointerException       if the molecule is null
     * @throws IllegalArgumentException   if the molecule should be filtered, i.e. it cannot be fragmented even after
     *                                    preprocessing
     * @throws CloneNotSupportedException if cloning the given molecule fails
     */
    @Override
    public IAtomContainer applyPreprocessing(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        return null;
    }
    //</editor-fold>
    //
}