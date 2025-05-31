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
import javafx.beans.property.SimpleIntegerProperty;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.Bond;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.graph.invariant.ConjugatedPiSystemsDetector;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IElement;
import org.openscience.cdk.interfaces.IRingSet;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

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
 * @author Maximilian Rottmann (maximilian.rottmann@studmail.w-hs.de)
 * @version 1.0.0.0
 */
public class AlkylStructureFragmenter implements IMoleculeFragmenter{
    //<editor-fold desc="Public Static Final Class Variables">
    /**
     * Name of the fragmenter.
     */
    public static final String ALGORITHM_NAME = "Alkyl Fragmenter";
    /**
     * Default boolean value for keeping non-fragmentable molecules in the fragmenter pipeline.
     */
    public static final boolean KEEP_NON_FRAGMENTABLE_MOLECULES_SETTING_DEFAULT = true;
    /**
     * Default value for maximum length of carbon side chains.
     */
    public static final int MAX_CHAIN_LENGTH_SETTING_DEFAULT = 6;
    /**
     * Default boolean value for determination of further side chain dissection.
     */
    public static final boolean FRAGMENT_SIDE_CHAINS_SETTING_DEFAULT = true;
    /**
     * Default boolean value for determining whether to isolate non-cyclic tertiary and quaternary carbons.
     */
    public static final boolean ISOLATE_TERT_QUAT_CARBONS_SETTING_DEFAULT = true;
    /**
     * Default boolean value for separating tertiary and quaternary carbon atoms from ring setting.
     */
    public static final boolean SEPARATE_TERT_QUAT_CARBON_FROM_RING_SETTING_DEFAULT = false;
    //<editor-fold desc="Property Keys">
    /**
     * Key for an internal index property, used in uniquely identifying atoms during fragmentation.
     */
    public static final String INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY = "ASF.ATOM_INDEX";
    /**
     * Key for an internal index property used in uniquely identifying bonds during fragmentation.
     */
    public static final String INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY = "ASF.BOND_INDEX";
    /**
     * Key for an internal boolean property, used in identifying tertiary carbon atoms.
     */
    public static final String INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY = "ASF.TERTIARY_CARBON";
    /**
     * Key for an internal boolean property used in identifying quaternary carbon atoms.
     */
    public static final String INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY = "ASF.QUATERNARY_CARBON";
    /**
     * Key for an internal boolean property used in identifying double bonds and their atoms.
     */
    public static final String INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY = "ASF.DOUBLE_BOND_MARKER";
    /**
     * Key for an internal boolean property used in identifying triple bonds and their atoms.
     */
    public static final String INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY = "ASF.TRIPLE_BOND_MARKER";
    /**
     * Key for an internal boolean property used in identifying neighboring atoms of tertiary or quaternary carbon atoms
     * during fragmentation.
     */
    public static final String INTERNAL_ASF_NEIGHBOR_MARKER_KEY = "ASF.NEIGHBOR_MARKER";
    /**
     * Key for an internal boolean property used in identifying ring structure atoms and bonds during fragmentation.
     */
    public static final String INTERNAL_ASF_RING_MARKER_KEY = "ASF.RING_MARKER";
    /**
     * Key for an internal boolean property used in identifying conjugated pi bond systems during fragmentation.
     */
    public static final String INTERNAL_ASF_CONJ_PI_MARKER_KEY = "ASF.CONJ_PI_MARKER";
    /**
     * Key for an internal boolean property used in identifying bonds connecting tertiary or quaternary carbons and rings.
     */
    public static final String INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY = "ASF.CONNECTED_TERT_QUAT_RING_MARKER";
    /**
     * Key for an internal array list property used in identifying spiro configuration carbons.
     */
    public static final String INTERNAL_ASF_RING_ATOM_LIST_KEY = "ASF.RING_ATOM_LIST";
    //</editor-fold>
    //</editor-fold>
    //<editor-fold desc="Private Class Variables">
    /**
     * A variable to quickly disable certain checks in fragmentation steps (i.e. chemical formula) for easier debugging.
     */
    private final boolean ASFDebugBoolean = false;
    /**
     * A constant property that has a fragment hydrogen saturation setting.
     */
    private final SimpleIDisplayEnumConstantProperty fragmentSaturationSetting;
    /**
     * A constant property that has a boolean value defining if non-fragmentable molecules should be kept in the
     * fragmenter pipeline.
     */
    private final SimpleBooleanProperty keepNonFragmentableMoleculesSetting;
    /**
     * A constant property that has a boolean value determining whether side chains should be fragmented.
     */
    private final SimpleBooleanProperty fragmentSideChainsSetting;
    /**
     * A constant property that has an integer for maximum side chain length.
     */
    private final SimpleIntegerProperty maxChainLengthSetting;
    /**
     * A constant property that has a boolean value determining if non-cyclic tertiary and quaternary carbons should be isolated when fragmented.
     */
    private final SimpleBooleanProperty isolateTertQuatCarbonSetting;
    /**
     * A constant property that has a boolean value defining if tertiary and quaternary carbon atoms should be separated
     * from ring structures.
     */
    private final SimpleBooleanProperty separateTertQuatCarbonFromRingSetting;
    /**
     * Map to store pairs of {@literal <setting name, tooltip text>}.
     */
    private final HashMap<String, String> settingNameTooltipTextMap;
    /**
     * All settings of this fragmenter, encapsulated in JavaFX properties for binding to GUI.
     */
    private final List<Property<?>> settings;
    /**
     * HashMap of all display names of corresponding settings.
     */
    private final HashMap<String, String> settingNameDisplayNameMap;
    /**
     * String key for internal molecule filter passage.
     */
    private static final String ASF_FILTER_MARKER = "ASF.FilterMarker";
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(AlkylStructureFragmenter.class.getName());
    /**
     * String format for logger output when exceptions are thrown.
     */
    private static final String LOGGER_EXCEPTION_STRING_FORMAT = "Exception: %1s, Molecule ID: %2s, Cause: %3s";
    /**
     * String format for logger output without an exception.
     */
    private static final String LOGGER_WARNING_STRING_FORMAT = "Warning: %1s, Molecule ID: %2s, Cause: %3s";
    /**
     * CDK IChemObjectBuilder instance used in atomcontainer instancing.
     */
    private IChemObjectBuilder chemObjectBuilderInstance;
    //</editor-fold>
    //<editor-fold desc="Constructor">
    /**
     * Constructor, all settings are initialised with their respective default values.
     */
    public AlkylStructureFragmenter(){
        int tmpSettingsNameTooltipNumber = 6;
        int tmpInitialCapacitySettingsNameTooltipHashMap = CollectionUtil.calculateInitialHashCollectionCapacity(
                tmpSettingsNameTooltipNumber,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.settingNameTooltipTextMap = new HashMap<>(tmpInitialCapacitySettingsNameTooltipHashMap, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.settingNameDisplayNameMap = new HashMap<>(tmpInitialCapacitySettingsNameTooltipHashMap, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.fragmentSaturationSetting = new SimpleIDisplayEnumConstantProperty(this, "Fragment saturation setting",
                IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT, IMoleculeFragmenter.FragmentSaturationOption.class) {
            @Override
            public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    AlkylStructureFragmenter.LOGGER.log(Level.WARNING, anException.toString(), anException);
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
                Message.get("AlkylStructureFragmenter.fragmentSaturationSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.fragmentSaturationSetting.getName(),
                Message.get("AlkylStructureFragmenter.fragmentSaturationSetting.displayName"));
        this.keepNonFragmentableMoleculesSetting = new SimpleBooleanProperty(this, "Keep non-fragmentable molecules in Pipeline",
                AlkylStructureFragmenter.KEEP_NON_FRAGMENTABLE_MOLECULES_SETTING_DEFAULT);
        this.settingNameTooltipTextMap.put(this.keepNonFragmentableMoleculesSetting.getName(),
                Message.get("AlkylStructureFragmenter.keepNonFragmentableMoleculesSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.keepNonFragmentableMoleculesSetting.getName(),
                Message.get("AlkylStructureFragmenter.keepNonFragmentableMoleculesSetting.displayName"));
        this.fragmentSideChainsSetting = new SimpleBooleanProperty(this, "Fragment linear chains",
                AlkylStructureFragmenter.FRAGMENT_SIDE_CHAINS_SETTING_DEFAULT);
        this.settingNameTooltipTextMap.put(this.fragmentSideChainsSetting.getName(),
                Message.get("AlkylStructureFragmenter.fragmentSideChainsSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.fragmentSideChainsSetting.getName(),
                Message.get("AlkylStructureFragmenter.fragmentSideChainsSetting.displayName"));
        this.maxChainLengthSetting = new SimpleIntegerProperty(this, "Limit length of returned chains",
                AlkylStructureFragmenter.MAX_CHAIN_LENGTH_SETTING_DEFAULT) {
            @Override
            public void set(int newValue) throws NullPointerException, IllegalArgumentException {
                //call to super.set() for parameter checks
                //only allows values greater than 0 to be set
                if (newValue > 0) {
                    super.set(newValue);
                }
            }
        };
        this.settingNameTooltipTextMap.put(this.maxChainLengthSetting.getName(),
                Message.get("AlkylStructureFragmenter.maxChainLengthSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.maxChainLengthSetting.getName(),
                Message.get("AlkylStructureFragmenter.maxChainLengthSetting.displayName"));
        this.isolateTertQuatCarbonSetting = new SimpleBooleanProperty(this, "Isolate non-cyclic tertiary and quaternary carbons",
                AlkylStructureFragmenter.ISOLATE_TERT_QUAT_CARBONS_SETTING_DEFAULT);
        this.settingNameTooltipTextMap.put(this.isolateTertQuatCarbonSetting.getName(),
                Message.get("AlkylStructureFragmenter.isolateTertQuatCarbonsSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.isolateTertQuatCarbonSetting.getName(),
                Message.get("AlkylStructureFragmenter.isolateTertQuatCarbonsSetting.displayName"));
        this.separateTertQuatCarbonFromRingSetting = new SimpleBooleanProperty(this, "Separate tertiary and quaternary carbon atoms from rings", AlkylStructureFragmenter.SEPARATE_TERT_QUAT_CARBON_FROM_RING_SETTING_DEFAULT);
        this.settingNameTooltipTextMap.put(this.separateTertQuatCarbonFromRingSetting.getName(),
                Message.get("AlkylStructureFragmenter.separateTertQuatCarbonFromRingSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.separateTertQuatCarbonFromRingSetting.getName(),
                Message.get("AlkylStructureFragmenter.separateTertQuatCarbonFromRingSetting.displayName"));
        this.settings = new ArrayList<>(6);
        this.settings.add(this.fragmentSaturationSetting);
        this.settings.add(this.keepNonFragmentableMoleculesSetting);
        this.settings.add(this.fragmentSideChainsSetting);
        this.settings.add(this.maxChainLengthSetting);
        this.settings.add(this.isolateTertQuatCarbonSetting);
        this.settings.add(this.separateTertQuatCarbonFromRingSetting);
        //set chemObjectBuilderInstance
        this.chemObjectBuilderInstance = DefaultChemObjectBuilder.getInstance();
    }
    //</editor-fold>
    /**
     * Inner class for internal data transfer of atoms and bonds in arrays.
     */
    class MolecularArrays {
        /**
         * Private IAtom array for storing given atoms.
          */
        private IAtom[] atomArray;
        /**
         * Private IBond array for storing given bonds.
         */
        private IBond[] bondArray;
        /**
         * Constructor for MolecularArrays.
         * Initializes the atom and bond arrays with size determined by aMolecule atom and bond count.
         * Fills said arrays with the atoms and bonds of aMolecule and sets their properties.
         *
         * @param aMolecule to store in class arrays
         */
        protected MolecularArrays(IAtomContainer aMolecule) {
            this.atomArray = new IAtom[aMolecule.getAtomCount()];
            this.bondArray = new IBond[aMolecule.getBondCount()];
            //<editor-fold desc="Fill Atom Array">
            int tmpAlkylSFAtomIndex = 0;
            for (IAtom tmpAtom: aMolecule.atoms()) {
                if (tmpAtom != null) {
                /*
                set atom properties, IMPORTANT: this needs to be done in array filling step for correct detection of
                tertiary or quaternary carbon atoms and their neighbors
                 */
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY, tmpAlkylSFAtomIndex);
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY, false);
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, false);
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY, false);
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY, false);
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY, false);
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY, false);
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, false);
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY, false);
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_ATOM_LIST_KEY, new ArrayList<Integer>());
                    this.atomArray[tmpAlkylSFAtomIndex] = tmpAtom;
                }
                tmpAlkylSFAtomIndex++;
            }
            int tmpAlkylSFBondIndex = 0;
            for (IBond tmpBond: aMolecule.bonds()) {
                if (tmpBond != null) {
                    tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY, tmpAlkylSFBondIndex);
                    tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY, false);
                    tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, false);
                    tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY, false);
                    tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY, false);
                    tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, false);
                    tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY, false);
                    this.bondArray[tmpAlkylSFBondIndex] = tmpBond;
                }
                tmpAlkylSFBondIndex++;
            }
            //</editor-fold>
        }
        /**
         * Public get method for internal IAtom array.
         *
         * @return internal IAtom[]
         */
        public IAtom[] getAtomArray() {
            return this.atomArray;
        }
        /**
         * Public get method for internal IBond array.
         *
         * @return internal IBond[]
         */
        public IBond[] getBondArray() {
            return this.bondArray;
        }
        /**
         * Public set method for internal IAtom array.
         *
         * @param anAtomArray to be set as internal array
         */
        public void setAtomArray(IAtom[] anAtomArray) {
            this.atomArray = anAtomArray;
        }
        /**
         * Public set method for internal IBond array.
         *
         * @param aBondArray to be set as internal array
         */
        public void setBondArray(IBond[] aBondArray) {
            this.bondArray = aBondArray;
        }
        /**
         * Public method to clear class arrays.
         */
        public void clearArrays() {
            this.atomArray = null;
            this.bondArray = null;
        }
    }
    //<editor-fold desc="Public Properties Get">

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
        return AlkylStructureFragmenter.ALGORITHM_NAME;
    }
    @Override
    public String getFragmentationAlgorithmDisplayName() {
        return Message.get("AlkylStructureFragmenter.displayName");
    }
    @Override
    public FragmentSaturationOption getFragmentSaturationSetting() {
        return (IMoleculeFragmenter.FragmentSaturationOption) this.fragmentSaturationSetting.get();
    }
    /**
     * Public get method for retention setting for non-fragmentable molecules.
     *
     * @return boolean value of keepNonFragmentableMoleculesSetting
     */
    public boolean getKeepNonFragmentableMoleculesSetting() {
        return this.keepNonFragmentableMoleculesSetting.get();
    }
    /**
     * Public get method for property of retention setting for non-fragmentable molecules.
     *
     * @return SimpleBooleanProperty keepNonFragmentableMoleculesSetting
     */
    public SimpleBooleanProperty getKeepNonFragmentableMoleculesSettingProperty() {
        return this.keepNonFragmentableMoleculesSetting;
    }
    /**
     * Public get method for maximum chain length setting.
     *
     * @return integer value of maxChainLengthSetting
     */
    public int getMaxChainLengthSetting() {
        return this.maxChainLengthSetting.get();
    }
    /**
     * Public get method for maximum chain length setting property.
     *
     * @return SimpleIntegerProperty maxChainLengthSetting
     */
    public SimpleIntegerProperty getMaxChainLengthSettingProperty() {
        return this.maxChainLengthSetting;
    }
    /**
     * Public get method for isolation of non-cyclic tertiary and quaternary carbons setting.
     *
     * @return boolean value of isolateTertQuatCarbonSetting
     */
    public boolean getIsolateTertQuatCarbonSetting() {
        return this.isolateTertQuatCarbonSetting.get();
    }
    /**
     * Public get method for isolation of non-cyclic tertiary and quaternary carbons setting property.
     *
     * @return SimpleBooleanProperty isolateTertQuatCarbonsSettingProperty
     */
    public SimpleBooleanProperty getIsolateTertQuatCarbonsSettingProperty() {
        return this.isolateTertQuatCarbonSetting;
    }
    /**
     * Public get method for separating tertiary and quaternary carbons from rings setting.
     *
     * @return boolean value of separateTertQuatCarbonFromRingSetting
     */
    public boolean getSeparateTertQuatCarbonFromRingSetting() {
        return this.separateTertQuatCarbonFromRingSetting.get();
    }
    /**
     * Public get method for separate tertiary and quaternary carbon atoms from rings setting property.
     *
     * @return SimpleBooleanProperty separateTertQuatCarbonFromRingSetting
     */
    public SimpleBooleanProperty getSeparateTertQuatCarbonFromRingSettingProperty() {
        return this.separateTertQuatCarbonFromRingSetting;
    }
    @Override
    public SimpleIDisplayEnumConstantProperty fragmentSaturationSettingProperty() {
        return this.fragmentSaturationSetting;
    }
    //</editor-fold>
    //<editor-fold desc="Public Properties Set">

    @Override
    public void setFragmentSaturationSetting(FragmentSaturationOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given saturation option is null.");
        this.fragmentSaturationSetting.set(anOption);
    }
    /**
     * Set method for retention setting for non-fragmentable molecules.
     *
     * @param aBoolean keep non-fragmentable molecules
     */
    public void setKeepNonFragmentableMoleculesSetting(boolean aBoolean) {
        this.keepNonFragmentableMoleculesSetting.set(aBoolean);
    }
    /**
     * Set method for setting defining whether side chains should be fragmented.
     *
     * @param aBoolean whether side chains are to be dissected
     */
    public void setFragmentSideChainsSetting(boolean aBoolean) {
        this.fragmentSideChainsSetting.set(aBoolean);
    }
    /**
     * Set method for setting defining maximum side chain length.
     *
     * @param aValue the given integer value for chain length
     * @throws IllegalArgumentException if given int is smaller than 0
     */
    public void setMaxChainLengthSetting(int aValue) throws IllegalArgumentException{
        if (aValue < 0) {
            throw new IllegalArgumentException("Given chain length cannot be negative.");
        }
        this.maxChainLengthSetting.set(aValue);
    }
    /**
     * Set method for setting defining whether to isolate non-cyclic tertiary and quaternary carbons.
     *
     * @param aBoolean the given boolean value for switching handling
     */
    public void setIsolateQuatCarbonsSetting(boolean aBoolean){
        this.isolateTertQuatCarbonSetting.set(aBoolean);
    }
    /**
     * Set method for setting defining if tertiary and quaternary carbon atoms should be separated from ring structures.
     *
     * @param aBoolean the given boolean value defining if tertiary and quaternary carbon atoms should be separated from ring structures
     */
    public void setSeparateTertQuatCarbonFromRingSetting(boolean aBoolean) {
        this.separateTertQuatCarbonFromRingSetting.set(aBoolean);
    }
    //</editor-fold>
    //<editor-fold desc="Public Methods">

    @Override
    public IMoleculeFragmenter copy() {
        AlkylStructureFragmenter tmpCopy = new AlkylStructureFragmenter();
        tmpCopy.setFragmentSaturationSetting((IMoleculeFragmenter.FragmentSaturationOption) this.fragmentSaturationSetting.get());
        tmpCopy.setKeepNonFragmentableMoleculesSetting(this.keepNonFragmentableMoleculesSetting.get());
        tmpCopy.setFragmentSideChainsSetting(this.fragmentSideChainsSetting.get());
        tmpCopy.setMaxChainLengthSetting(this.maxChainLengthSetting.get());
        tmpCopy.setIsolateQuatCarbonsSetting(this.isolateTertQuatCarbonSetting.get());
        tmpCopy.setSeparateTertQuatCarbonFromRingSetting(this.separateTertQuatCarbonFromRingSetting.get());
        return tmpCopy;
    }
    @Override
    public void restoreDefaultSettings() {
        this.fragmentSaturationSetting.set(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT);
        this.keepNonFragmentableMoleculesSetting.set(AlkylStructureFragmenter.KEEP_NON_FRAGMENTABLE_MOLECULES_SETTING_DEFAULT);
        this.fragmentSideChainsSetting.set(AlkylStructureFragmenter.FRAGMENT_SIDE_CHAINS_SETTING_DEFAULT);
        this.maxChainLengthSetting.set(AlkylStructureFragmenter.MAX_CHAIN_LENGTH_SETTING_DEFAULT);
        this.isolateTertQuatCarbonSetting.set(AlkylStructureFragmenter.ISOLATE_TERT_QUAT_CARBONS_SETTING_DEFAULT);
        this.separateTertQuatCarbonFromRingSetting.set(AlkylStructureFragmenter.SEPARATE_TERT_QUAT_CARBON_FROM_RING_SETTING_DEFAULT);
    }
    //<editor-fold desc="Pre-Fragmentation Tasks">
    /**
     * {@inheritDoc}
     * <p>
     *     Checks the given IAtomContainer aMolecule for non-carbon and non-hydrogen atoms and returns true if
     *     non-conforming atoms are found, otherwise false is returned and the molecule can be fragmented.
     *     In order to enable the user to let non-fragmented molecules be retained in the pipeline, the filter can be
     *     switched off via setting keepNonFragmentableMoleculesSetting.
     *     An if-condition at the end checks for the special case of explicit hydrogens and filtering them out if no
     *     carbon is present in aMolecule, as they pass the actual filter.
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
        int tmpCarbonCount = 0;
        for (IAtom tmpAtom : aMolecule.atoms()) {
            if (tmpAtom == null || tmpAtom.getAtomicNumber() == null) {
                return true;
            }
            if (Objects.requireNonNull(tmpAtom).getAtomicNumber() == IElement.C) {
                tmpCarbonCount++;
            } else {
                if (this.keepNonFragmentableMoleculesSetting.get()) {
                    aMolecule.setProperty(ASF_FILTER_MARKER, true);
                    return false;
                }
                return true;
            }
        }
        if (tmpCarbonCount != 0 ) {
            aMolecule.setProperty(ASF_FILTER_MARKER, false);
            return false;
        }
        //the else condition is only meant to filter out explicit hydrogen (setting for on/off could be implemented)
        else {
            aMolecule.setProperty(ASF_FILTER_MARKER, true);
            return true;
        }
    }
    /**
     * {@inheritDoc}
     * Always returns false, as no preprocessing is currently needed.
     *
     * @param aMolecule the molecule to check
     * @return currently always false
     * @throws NullPointerException if the given molecule is null
     */
    @Override
    public boolean shouldBePreprocessed(IAtomContainer aMolecule) throws NullPointerException {
        Objects.requireNonNull(aMolecule, "Given molecule is null");
        return false;
    }
    @Override
    public boolean canBeFragmented(IAtomContainer aMolecule) throws NullPointerException {
        //throws NullpointerException if molecule is null
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        if ((boolean) aMolecule.getProperty(ASF_FILTER_MARKER)) {
            return true;
        }
        boolean tmpShouldBeFiltered = this.shouldBeFiltered(aMolecule);
        boolean tmpShouldBePreprocessed = this.shouldBePreprocessed(aMolecule);
        return !tmpShouldBeFiltered && !tmpShouldBePreprocessed;
    }
    /**
     * Method for applying special preprocessing steps before fragmenting the given molecule.
     * Currently, no preprocessing applied as none is needed.
     *
     * @param aMolecule the molecule to preprocess
     * @return aMolecule, unchanged molecule as no preprocessing is currently needed
     * @throws NullPointerException if the given molecule is null
     */
    @Override
    public IAtomContainer applyPreprocessing(IAtomContainer aMolecule) throws NullPointerException {
        Objects.requireNonNull(aMolecule, "Given molecule is null");
        return aMolecule;
    }
    //</editor-fold>
    //<editor-fold desc="Fragmentation">
    /**
     * {@inheritDoc}
     * <p>
     * Algorithmic approach to alkyl fragmentation:
     * <p>
     *     As a first measure, the given molecule is checked for an internal property, checking if it has been
     *     passed through an upstream filter. Afterward, the molecule is cloned in order to prevent changes to the
     *     original molecule and atom types are perceived and configured for downstream fragmentation steps.
     * </p>
     * <p>
     *     Next, internal class (@see{MolecularArrays}) arrays responsible for data transfer are filled. The atoms and
     *     bonds of the cloned molecule are placed in respective IAtom and IBond arrays, which are then placed in a
     *     MolecularArrays instance. In the 'Filling' step, atoms and bonds are prepared with internal properties used
     *     during fragmentation.
     * </p>
     * <p>
     *     Following, the atoms and bonds are marked in accordance with substructures of interest, e.g. tertiary carbons.
     *     Atoms and bonds may be marked multiple times with different properties if they are part of more than one
     *     substructure.
     *     Order of marking as follows: tertiary and quaternary carbon properties are set during array filling step;
     *     atoms and bonds neighbouring tertiary and quaternary carbons; singular rings and ring systems;
     *     conjugated pi bond systems; bonds of higher order (>1) present in linear sidechains.
     * </p>
     * <p>
     *     Extraction of marked substructures is done by creating deep copies of the cloned molecule's atoms and bonds,
     *     and placing them in designated new IAtomContainer instances. During extraction, the preservation of the
     *     chemical formula is checked, and a warning logged if not true.
     *     The order of extraction is as follows: rings, conjugated pi bond systems and their fusion products;
     *     additional double bonds connected to rings; isolated bonds of higher order;
     *     atoms and bonds neighbouring tertiary or quaternary carbons; residual atoms and bonds as linear chains.
     *     The extracted linear chains may also be fragmented according to the set maximum length
     *     (see maxChainLengthSetting documentation).
     *     Multiple settings may be activated for different algorithmic behavior regarding selected substructures
     *     (see settings documentation).
     *     After the extraction, the returned fragments are saturated with implicit hydrogen if the corresponding
     *     setting is active.
     * </p>
     *
     * @param aMolecule to fragment
     * @return List of IAtomContainers containing the fragments (may be empty if no fragments are extracted)
     * @throws NullPointerException if aMolecule is null
     * @throws IllegalArgumentException if the given molecule cannot be fragmented
     * @throws CloneNotSupportedException if cloning the given molecule is unsuccessful
     */
    @Override
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule)
            throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        //skip fragmentation if molecule is not fragmentable and just return it as one fragment to keep it in pipeline
        if ((boolean) aMolecule.getProperty(ASF_FILTER_MARKER) && this.keepNonFragmentableMoleculesSetting.get()) {
            List<IAtomContainer> tmpNonFragACList = new ArrayList<>(1);
            tmpNonFragACList.add(aMolecule);
            return tmpNonFragACList;
        }
        //<editor-fold desc="Molecule Cloning and Chemical Formula Check" defaultstate="collapsed">
        IAtomContainer tmpClone = aMolecule.clone();
        this.chemObjectBuilderInstance = DefaultChemObjectBuilder.getInstance();
        int tmpPreFragmentationAtomCount = 0;
        for (IAtom tmpAtom: tmpClone.atoms()) {
            if (tmpAtom.getAtomicNumber() != 0) {
                tmpPreFragmentationAtomCount++;
            }
        }
        AlkylStructureFragmenter.LOGGER.log(Level.INFO, "PreFragAtomCount: " + tmpPreFragmentationAtomCount);
        try {
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpClone);
        } catch (CDKException aCDKException) {
            AlkylStructureFragmenter.LOGGER.log(Level.WARNING,
                    String.format(LOGGER_EXCEPTION_STRING_FORMAT, aCDKException, tmpClone.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY),
                            "Atom types could not be perceived or atoms could not be configured."));
            }
        //</editor-fold>
        //<editor-fold desc="Detection Steps" defaultstate="collapsed">
        MolecularArrays tmpMolecularArrays = new MolecularArrays(tmpClone);
        tmpMolecularArrays.setAtomArray(this.fillAtomArray(tmpClone));
        tmpMolecularArrays.setBondArray(this.fillBondArray(tmpClone));
        this.markNeighborAtomsAndBonds(tmpMolecularArrays, tmpMolecularArrays.getAtomArray(), tmpMolecularArrays.getBondArray());
        this.markRings(tmpMolecularArrays, tmpClone, tmpMolecularArrays.getAtomArray(), tmpMolecularArrays.getBondArray());
        this.markConjugatedPiSystems(tmpMolecularArrays, tmpClone, tmpMolecularArrays.getAtomArray(), tmpMolecularArrays.getBondArray());
        this.markMultiBonds(tmpMolecularArrays, tmpMolecularArrays.getAtomArray(), tmpMolecularArrays.getBondArray());
        //</editor-fold>
        //<editor-fold desc="Fragment Extraction and Saturation" defaultstate="collapsed">
        try {
            int tmpPostFragmentationAtomCount = 0;
            IAtomContainerSet tmpFragmentSet = this.extractFragments(tmpMolecularArrays.getAtomArray(), tmpMolecularArrays.getBondArray());
            for (IAtomContainer tmpAtomContainer: tmpFragmentSet.atomContainers()) {
                for (IAtom tmpAtom: tmpAtomContainer.atoms()) {
                    if (tmpAtom.getAtomicNumber() != 0) {
                        tmpPostFragmentationAtomCount++;
                    }
                }
            }
            AlkylStructureFragmenter.LOGGER.log(Level.INFO, "PostFragAtomCount: " + tmpPostFragmentationAtomCount);
            if (tmpPostFragmentationAtomCount != tmpPreFragmentationAtomCount && !this.ASFDebugBoolean) {
                AlkylStructureFragmenter.LOGGER.log(Level.WARNING, String.format(LOGGER_WARNING_STRING_FORMAT,
                        "Chemical Formula Check", tmpClone.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY),
                        "Chemical formula was not constant!"));
            }
            if (this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION)) {
                IAtomContainerSet tmpSaturatedSet = this.saturateWithImplicitHydrogen(tmpFragmentSet);
                List<IAtomContainer> tmpSaturatedList = new ArrayList<>(tmpSaturatedSet.getAtomContainerCount());
                for (IAtomContainer tmpReturnAC: tmpSaturatedSet.atomContainers()) {
                    tmpSaturatedList.add(tmpReturnAC);
                }
                return tmpSaturatedList;
            }
            ArrayList<IAtomContainer> tmpFragmentList = new ArrayList<>(tmpFragmentSet.getAtomContainerCount());
            for (IAtomContainer tmpAtomContainer: tmpFragmentSet.atomContainers()) {
                tmpFragmentList.add(tmpAtomContainer);
            }
            return tmpFragmentList;
        } catch (Exception anException) {
            /*
            AlkylStructureFragmenter.LOGGER.log(Level.WARNING, String.format(LOGGER_EXCEPTION_STRING_FORMAT,
                    anException, tmpClone.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY),
                    "Fragmentation failed!"), anException);
            */
            throw new IllegalArgumentException(String.format(LOGGER_EXCEPTION_STRING_FORMAT, anException,
                    tmpClone.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY), "Fragmentation failed!"));
        }
        //</editor-fold>
    }
    //</editor-fold>
    //</editor-fold>
    //<editor-fold desc="Protected Methods" defaultstate="collapsed">
    /**
     * Method to fill an IAtom array with the atoms of the input IAtomContainer
     * and place fragmentation properties on them.
     *
     * Null atoms are removed in the returned array.
     * IMPORTANT:
     * All used properties have to be set in this step ONCE (independent of set values) in order for correct
     * fragmenter function, especially tertiary and quaternary carbon detection.
     *
     * @param aClone IAtomContainer, best a clone, from which atoms are put into array
     * @return IAtom array containing the atoms
     */
    protected IAtom[] fillAtomArray(IAtomContainer aClone) {
        IAtom[] tmpAtomArray = new IAtom[aClone.getAtomCount()];
        //ToDo: remove, was moved to MolecularArrays Constructor
        int tmpAlkylSFAtomIndex = 0;
        for (IAtom tmpAtom: aClone.atoms()) {
            if (tmpAtom != null) {
                /*
                set atom properties, IMPORTANT: this needs to be done in array filling step for correct detection of
                tertiary or quaternary carbon atoms and their neighbors
                 */
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY, tmpAlkylSFAtomIndex);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY, false);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, false);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY, false);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY, false);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY, false);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY, false);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, false);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY, false);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_ATOM_LIST_KEY, new ArrayList<Integer>());
                tmpAtomArray[tmpAlkylSFAtomIndex] = tmpAtom;
            }
            tmpAlkylSFAtomIndex++;
        }
        //ToDo: move to for above
        ArrayList<IAtom> tmpRemovedNull = new ArrayList<IAtom>();
        for (IAtom tmpAtom: tmpAtomArray)
            if (tmpAtom != null) {
                tmpRemovedNull.add(tmpAtom);
            }
        return tmpRemovedNull.toArray(new IAtom[0]);
    }
    /**
     * Method to fill an IBond array with the bonds of the input IAtomContainer
     * and place fragmentation properties on them.
     *
     * Null bonds are removed in the returned array.
     * @param aClone IAtomContainer, best a clone, from which bonds are put into array
     * @return IBond array containing the bonds
     */
    protected IBond[] fillBondArray(IAtomContainer aClone) {
        //ToDo: remove, was moved to MolecularArrays Constructor
        IBond[] tmpBondArray = new IBond[aClone.getBondCount()];
        int tmpAlkylSFBondIndex = 0;
        for (IBond tmpBond: aClone.bonds()) {
            if (tmpBond != null) {
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY, tmpAlkylSFBondIndex);
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY, false);
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, false);
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY, false);
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY, false);
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, false);
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY, false);
                tmpBondArray[tmpAlkylSFBondIndex] = tmpBond;
            }
            tmpAlkylSFBondIndex++;
        }
        //ToDo: move to for above
        ArrayList<IBond> tmpRemovedNull = new ArrayList<IBond>();
        for (IBond tmpBond: tmpBondArray)
            if (tmpBond != null) {
                tmpRemovedNull.add(tmpBond);
            }
        return tmpRemovedNull.toArray(new IBond[0]);
    }
    /**
     * Protected method for detecting and marking tertiary or quaternary carbon atoms and their surrounding neighbor atoms and bonds.
     *
     * @param aMolecularArraysInstance MolecularArrays instance for data transfer between methods
     * @param anAtomArray Given array with atoms of molecule to be fragmented
     * @param aBondArray Given array with bonds of molecule to be fragmented
     */
    protected void markNeighborAtomsAndBonds(MolecularArrays aMolecularArraysInstance, IAtom[] anAtomArray, IBond[] aBondArray) {
        Objects.requireNonNull(anAtomArray, "Given atom array is null.");
        Objects.requireNonNull(aBondArray,"Given bond array is null");
        //ToDo: remove parameter arrays and get them from molArray instance
        anAtomArray = aMolecularArraysInstance.getAtomArray();
        aBondArray = aMolecularArraysInstance.getBondArray();
        //set general atom and specific bond properties
        for (int tmpAtomIndex = 0; tmpAtomIndex < anAtomArray.length; tmpAtomIndex++) {
            IAtom tmpAtom = anAtomArray[tmpAtomIndex];
            if (tmpAtom != null) {
                //marking of tertiary or quaternary carbons together with their neighbor atoms
                if (tmpAtom.getBondCount() == 3 && tmpAtom.getMaxBondOrder() == IBond.Order.SINGLE) {
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY, true);
                    for (IBond tmpBond: tmpAtom.bonds()) {
                        int tmpBondIndex = tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY);
                        int tmpBondBeginIndex = tmpBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                        int tmpBondEndIndex = tmpBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                        if (tmpBond.getBegin() == tmpAtom) {
                            anAtomArray[tmpBondEndIndex].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                            aBondArray[tmpBondIndex].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                        } else if (tmpBond.getEnd() == tmpAtom) {
                            anAtomArray[tmpBondBeginIndex].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                            aBondArray[tmpBondIndex].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                        }
                    }
                } else if (tmpAtom.getBondCount() == 4 && tmpAtom.getMaxBondOrder() == IBond.Order.SINGLE) {
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY, true);
                    for (IBond tmpBond: tmpAtom.bonds()) {
                        int tmpBondIndex = tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY);
                        int tmpBondBeginIndex = tmpBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                        int tmpBondEndIndex = tmpBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                        if (tmpBond.getBegin() == tmpAtom) {
                            anAtomArray[tmpBondEndIndex].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                            aBondArray[tmpBondIndex].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                        } else if (tmpBond.getEnd() == tmpAtom) {
                            anAtomArray[tmpBondBeginIndex].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                            aBondArray[tmpBondIndex].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                        }
                    }
                }
            }
        }
        aMolecularArraysInstance.setAtomArray(anAtomArray);
        aMolecularArraysInstance.setBondArray(aBondArray);
    }
    /**
     * Protected method to mark all atoms and bonds of any rings in the given atomcontainer.
     *
     * @param aMolecularArraysInstance MolecularArrays instance for data transfer between methods
     * @param anAtomContainer IAtomContainer to mark atoms and bonds in
     * @param anAtomArray containing atoms to be marked
     * @param aBondArray containing bonds to be marked
     */
    protected void markRings(MolecularArrays aMolecularArraysInstance, IAtomContainer anAtomContainer, IAtom[] anAtomArray, IBond[] aBondArray) throws IllegalArgumentException {
        Objects.requireNonNull(aMolecularArraysInstance);
        Objects.requireNonNull(anAtomArray);
        Objects.requireNonNull(aBondArray);
        //<editor-fold desc="CycleFinder (MCB)">
        CycleFinder tmpMCBCycleFinder = Cycles.mcb();
        IRingSet tmpMCBCyclesSet;
        try {
            Cycles tmpMCBCycles = tmpMCBCycleFinder.find(anAtomContainer);
            tmpMCBCyclesSet = tmpMCBCycles.toRingSet();
            int tmpSingleRingCount = 0;
            for (IAtomContainer tmpContainer: tmpMCBCyclesSet.atomContainers()) {
                for (IAtom tmpRingAtom: tmpContainer.atoms()) {
                    int tmpAtomInteger = tmpRingAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                    IAtom tmpAtom = anAtomArray[tmpAtomInteger];
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY, true);
                    //ToDo: remove ring index as not used
                }
                for (IBond tmpRingBond: tmpContainer.bonds()) {
                    int tmpBondInteger = tmpRingBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY);
                    IBond tmpBond = aBondArray[tmpBondInteger];
                    tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY, true);
                }
                tmpSingleRingCount++;
            }
        } catch (Exception anException) {
            AlkylStructureFragmenter.LOGGER.log(Level.WARNING, String.format(LOGGER_EXCEPTION_STRING_FORMAT,
                    anException, anAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY),
                    "CycleFinder failed."), anException);
            throw new IllegalArgumentException(String.format(LOGGER_EXCEPTION_STRING_FORMAT,
                    anException, anAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY),
                    "CycleFinder failed."));
        }
        //</editor-fold>

        //returns object containing atoms array and bonds array
        aMolecularArraysInstance.setAtomArray(anAtomArray);
        aMolecularArraysInstance.setBondArray(aBondArray);
    }
    /**
     * Protected method to mark all atoms and bonds of any conjugated pi systems in the given atomcontainer.
     *
     * @param aMolecularArraysInstance MolecularArrays instance for data transfer between methods
     * @param anAtomContainer IAtomContainer to mark atoms and bonds in
     * @param anAtomArray Array containing the atoms of a given fragmentation molecule
     * @param aBondArray Array containing the bonds of a given fragmentation molecule
     */
    protected void markConjugatedPiSystems(MolecularArrays aMolecularArraysInstance, IAtomContainer anAtomContainer, IAtom[] anAtomArray, IBond[] aBondArray) throws IllegalArgumentException{
        //<editor-fold desc="ConjugatedPiSystemsDetector" defaultstate="collapsed">
        Objects.requireNonNull(anAtomArray);
        Objects.requireNonNull(aBondArray);
        try {
            IAtomContainerSet tmpConjugatedAtomContainerSet;
            tmpConjugatedAtomContainerSet = ConjugatedPiSystemsDetector.detect(anAtomContainer);
            //molecule mapping
            //iterate over every atomcontainer from ConjPiSystemsDetector output
            for (IAtomContainer tmpConjAtomContainer: tmpConjugatedAtomContainerSet.atomContainers()) {
                for (IAtom tmpConjAtom: tmpConjAtomContainer.atoms()) {
                    int tmpAtomInteger = tmpConjAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                    IAtom tmpAtom = anAtomArray[tmpAtomInteger];
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
                }
                for (IBond tmpConjBond: tmpConjAtomContainer.bonds()) {
                    int tmpBondInteger = tmpConjBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY);
                    IBond tmpBond = aBondArray[tmpBondInteger];
                    tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
                }
            }
            aMolecularArraysInstance.setAtomArray(anAtomArray);
            aMolecularArraysInstance.setBondArray(aBondArray);
        } catch (Exception anException) {
            AlkylStructureFragmenter.LOGGER.log(Level.WARNING, String.format(LOGGER_EXCEPTION_STRING_FORMAT,
                    anException, anAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY),
                    "Conjugated Pi Systems detection failed."), anException);
            throw new IllegalArgumentException(String.format(LOGGER_EXCEPTION_STRING_FORMAT,
                    anException, anAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY),
                    "Conjugated Pi Systems detection failed."));
        }

        //</editor-fold>
    }
    /**
     * Protected method to check given atomcontainer for disconnected structures.
     *
     * @param anAtomContainer IAtomContainer to check
     * @return IAtomContainerSet containing partitioned structures as single IAtomContainer
     */
    protected IAtomContainerSet separateDisconnectedStructures(IAtomContainer anAtomContainer) throws IllegalArgumentException{
        Objects.requireNonNull(anAtomContainer,"Given IAtomContainer is null.");
        try {
            IAtomContainerSet tmpFragmentSet = new AtomContainerSet();
            //logger used only for debug purpose
            AlkylStructureFragmenter.LOGGER.log(Level.INFO, System.currentTimeMillis() + " start sD, AC size: " + anAtomContainer.getAtomCount() + ", " + anAtomContainer.getBondCount());
            if (!anAtomContainer.isEmpty()) {
                if (!ConnectivityChecker.isConnected(anAtomContainer)) {
                    IAtomContainerSet tmpContainerSet = ConnectivityChecker.partitionIntoMolecules(anAtomContainer);
                    for (IAtomContainer tmpContainer : tmpContainerSet.atomContainers()) {
                        tmpFragmentSet.addAtomContainer(tmpContainer);
                    }
                } else {
                    tmpFragmentSet.addAtomContainer(anAtomContainer);
                }
            }
            return tmpFragmentSet;
        } catch (Exception anException) {
            AlkylStructureFragmenter.LOGGER.log(Level.WARNING, String.format(LOGGER_EXCEPTION_STRING_FORMAT,
                    anException, anAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY),
                            "Connectivity Check failed."), anException);
            //logger only used for debug purpose
            AlkylStructureFragmenter.LOGGER.log(Level.INFO, System.currentTimeMillis() + " start sD, AC size: " + anAtomContainer.getAtomCount() + ", " + anAtomContainer.getBondCount());
            throw new IllegalArgumentException(String.format(LOGGER_EXCEPTION_STRING_FORMAT,
                    anException, anAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY),
                    "Connectivity Check failed."));
        }
    }
    /**
     * Protected method to saturate a given molecule with implicit hydrogens after fragmentation.
     *
     * @param anUnsaturatedACSet IAtomContainerSet whose atomcontainers are to be saturated
     * @return List of processed atomcontainers, @null if given Set is empty
     * @throws CDKException if CDKHydrogenAdder throws an exception
     */
    protected IAtomContainerSet saturateWithImplicitHydrogen(IAtomContainerSet anUnsaturatedACSet) throws CDKException {
        Objects.requireNonNull(anUnsaturatedACSet, "Given IAtomContainerSet is null.");
        try {
            IAtomContainerSet tmpSaturatedFragmentsSet = new AtomContainerSet();
            if (!anUnsaturatedACSet.isEmpty() && !anUnsaturatedACSet.getAtomContainer(0).isEmpty()) {
                for (IAtomContainer tmpAtomContainer: anUnsaturatedACSet.atomContainers()) {
                    if (tmpAtomContainer != null && !tmpAtomContainer.isEmpty()) {
                        ChemUtil.saturateWithHydrogen(tmpAtomContainer);
                        tmpSaturatedFragmentsSet.addAtomContainer(tmpAtomContainer);
                    }
                }
            }
            return tmpSaturatedFragmentsSet;
        } catch (CDKException anException) {
            AlkylStructureFragmenter.LOGGER.log(Level.WARNING, String.format(LOGGER_EXCEPTION_STRING_FORMAT,
                    anException, "None given.",
                    "Saturation failed."), anException);
            throw new CDKException(String.format(LOGGER_EXCEPTION_STRING_FORMAT,
                    anException, "None given.",
                    "Saturation failed."), anException);
        }
    }
    /**
     * Protected method to extract detected fragments via properties.
     *
     * @param anAtomArray Array containing the atoms of a given fragmentation molecule
     * @param aBondArray Array containing the bonds of a given fragmentation molecule
     * @return IAtomContainerSet with extracted molecules
     * @throws CloneNotSupportedException if input cannot be cloned
     */
    protected IAtomContainerSet extractFragments(IAtom[] anAtomArray, IBond[] aBondArray) throws CloneNotSupportedException, IllegalArgumentException {
        Objects.requireNonNull(anAtomArray);
        Objects.requireNonNull(aBondArray);
        //
        //<editor-fold desc="Extraction">
        IAtomContainerSet tmpExtractionSet = new AtomContainerSet();
        //ToDo: perhaps move all to one AtomContainer and simply check connectivity at end
        //ToDo: split into separate methods (returning one atomcontainer, could make above more viable)
        //ToDo: separate tert/quat atom into method
        IAtomContainer tmpRingFragmentationContainer = this.chemObjectBuilderInstance.newAtomContainer();
        IAtomContainer tmpChainFragmentationContainer = this.chemObjectBuilderInstance.newAtomContainer();
        IAtomContainer tmpIsolatedMultiBondsContainer = this.chemObjectBuilderInstance.newAtomContainer();
        IAtomContainer tmpTertQuatCarbonContainer = this.chemObjectBuilderInstance.newAtomContainer();
        //
        //<editor-fold desc="atom extraction">
        atomIteration:
        for (IAtom tmpAtom : anAtomArray) {
            //checks atom if not part of ring or conjugated pi system
            if (!((boolean) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY)
                    || (boolean) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY))) {
                //Checks for tertiary mark
                if ((boolean) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY)) {
                    if (this.isolateTertQuatCarbonSetting.get()) {
                        if (!this.separateTertQuatCarbonFromRingSetting.get()) {
                            for (IAtom tmpNeighborAtom: tmpAtom.neighbors()) {
                                if ((boolean) tmpNeighborAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY)) {
                                    //System.out.println("add Tert Atom Ring Part");
                                    tmpRingFragmentationContainer.addAtom(this.deepCopyAtom(tmpAtom));
                                    continue atomIteration;
                                }
                            }
                            IAtomContainer tmpContainer = this.chemObjectBuilderInstance.newAtomContainer();
                            tmpContainer.addAtom(this.deepCopyAtom(tmpAtom));
                            for (int i = 0; i < 3; i++) {
                                //ToDo: ask whether difference between normal "new" and "newAtom" from chemObjectBuilder
                                tmpContainer.addAtom(new PseudoAtom());
                                IBond tmpBond = this.chemObjectBuilderInstance.newBond();
                                tmpBond.setOrder(IBond.Order.SINGLE);
                                tmpContainer.addBond(this.deepCopyBond(tmpBond, tmpContainer.getAtom(0), tmpContainer.getAtom(i + 1)));
                                tmpTertQuatCarbonContainer.add(tmpContainer);
                            }
                        }
                        else {
                            //ToDo: deep copy
                            tmpTertQuatCarbonContainer.addAtom(tmpAtom);
                            for (int i = 0; i < 3; i++) {
                                PseudoAtom tmpPseudoAtom = new PseudoAtom();
                                tmpTertQuatCarbonContainer.addAtom(tmpPseudoAtom);
                                IBond tmpBond = new Bond();
                                tmpBond.setOrder(IBond.Order.SINGLE);
                                tmpBond.setAtom(tmpAtom, 0);
                                tmpBond.setAtom(tmpPseudoAtom, 1);
                                tmpTertQuatCarbonContainer.addBond(tmpBond);
                            }
                        }
                    //tertiary carbons are added to ensure correct interaction with other substructures, neighbor atoms added later
                    } else {
                        tmpRingFragmentationContainer.addAtom(this.deepCopyAtom(tmpAtom));
                    }
                //checks for quaternary mark
                } else if ((boolean) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY)) {
                    if (this.isolateTertQuatCarbonSetting.get()) {
                        if (!this.separateTertQuatCarbonFromRingSetting.get()) {
                            for (IAtom tmpNeighborAtom: tmpAtom.neighbors()) {
                                if ((boolean) tmpNeighborAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY)) {
                                    //System.out.println("add Quat Atom Ring Part");
                                    tmpRingFragmentationContainer.addAtom(this.deepCopyAtom(tmpAtom));
                                    continue atomIteration;
                                }
                            }
                            IAtomContainer tmpContainer = this.chemObjectBuilderInstance.newAtomContainer();
                            tmpContainer.addAtom(this.deepCopyAtom(tmpAtom));
                            for (int i = 0; i < 4; i++) {
                                //ToDo: ask whether difference between normal "new" and "newAtom" from chemObjectBuilder
                                tmpContainer.addAtom(new PseudoAtom());
                                IBond tmpBond = this.chemObjectBuilderInstance.newBond();
                                tmpBond.setOrder(IBond.Order.SINGLE);
                                tmpContainer.addBond(this.deepCopyBond(tmpBond, tmpContainer.getAtom(0), tmpContainer.getAtom(i + 1)));
                                tmpTertQuatCarbonContainer.add(tmpContainer);
                            }
                        }
                        else {
                            //ToDo: deep copy
                            tmpTertQuatCarbonContainer.addAtom(tmpAtom);
                            for (int i = 0; i < 4; i++) {
                                PseudoAtom tmpPseudoAtom = new PseudoAtom();
                                tmpTertQuatCarbonContainer.addAtom(tmpPseudoAtom);
                                IBond tmpBond = new Bond();
                                tmpBond.setOrder(IBond.Order.SINGLE);
                                tmpBond.setAtom(tmpAtom, 0);
                                tmpBond.setAtom(tmpPseudoAtom, 1);
                                tmpTertQuatCarbonContainer.addBond(tmpBond);
                            }
                        }
                    //tertiary carbons are added to ensure correct interaction with other substructures, neighbor atoms added later
                    } else {
                        tmpRingFragmentationContainer.addAtom(this.deepCopyAtom(tmpAtom));
                    }
                }
                //checks for part of double/triple bond mark
                else if ((boolean) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY)) {
                    //extracts extra circular double bonds connected with a ring structure
                    IBond tmpDoubleBond;
                    for (IAtom tmpArrayAtom: anAtomArray) {
                        if (tmpArrayAtom != tmpAtom) {
                            //try for successful bond
                            try {
                                tmpDoubleBond = tmpAtom.getBond(tmpArrayAtom);
                                //check if bond is double bond
                                if ((boolean) tmpDoubleBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY)) {
                                    IAtom tmpBeginAtom = tmpDoubleBond.getBegin();
                                    IAtom tmpEndAtom = tmpDoubleBond.getEnd();
                                    //check if neither begin nor end atom are part of a ring structure
                                    if (!(boolean) tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY)
                                            && !(boolean) tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY)) {
                                        tmpIsolatedMultiBondsContainer.addAtom(this.deepCopyAtom(tmpAtom));
                                    } else {
                                        //map bond atom to array atom
                                        if ((int) tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY)
                                                == (int) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY)) {
                                            if (!tmpRingFragmentationContainer.contains(tmpBeginAtom)) {
                                                tmpRingFragmentationContainer.addAtom(this.deepCopyAtom(tmpBeginAtom));
                                            }
                                        } else if ((int) tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY)
                                                == (int) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY)) {
                                            if (!tmpRingFragmentationContainer.contains(tmpEndAtom)) {
                                                tmpRingFragmentationContainer.addAtom(this.deepCopyAtom(tmpEndAtom));
                                            }
                                        }
                                    }
                                }
                            } catch (Exception atomNotInBondException){
                                continue;
                            }
                        }
                    }
                    //test if algorithmic of double bond should be applied here too
                } else if ((boolean) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY)) {
                    tmpIsolatedMultiBondsContainer.addAtom(this.deepCopyAtom(tmpAtom));
                }
                //extract neighbor atoms of tertiary or quaternary carbon atoms
                else if ((boolean) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY)) {
                    if (this.isolateTertQuatCarbonSetting.get()) {
                        if (tmpAtom.getBondCount() == 1) {
                            tmpRingFragmentationContainer.addAtom(this.deepCopyAtom(tmpAtom));
                        } else {
                            tmpChainFragmentationContainer.addAtom(this.deepCopyAtom(tmpAtom));
                        }
                    } else {
                        tmpRingFragmentationContainer.addAtom(this.deepCopyAtom(tmpAtom));
                    }
                }
                //extract residue atoms as linear chain atoms
                else {
                    tmpChainFragmentationContainer.addAtom(this.deepCopyAtom(tmpAtom));
                }
            //only ring and conj pi system atoms, sets bond property to determine connection between tert/quat atom and ring structure
            } else {
                for (IBond tmpBond: tmpAtom.bonds()) {
                    if (tmpBond.getBegin().equals(tmpAtom)) {
                        boolean tmpIsEndTertiary = tmpBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY);
                        boolean tmpIsEndQuaternary = tmpBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY);
                        boolean tmpIsEndRing = tmpBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY);
                        boolean tmpIsEndConjPi = tmpBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY);
                        if (!tmpIsEndRing && (tmpIsEndTertiary || tmpIsEndQuaternary) && !tmpIsEndConjPi) {
                                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY, true);
                                int tmpBeginAtomIndex = tmpBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                                int tmpEndAtomIndex = tmpBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                                anAtomArray[tmpBeginAtomIndex].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY, true);
                                anAtomArray[tmpEndAtomIndex].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY, true);
                            }

                    } else {
                        boolean tmpIsBeginTertiary = tmpBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY);
                        boolean tmpIsBeginQuaternary = tmpBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY);
                        boolean tmpIsBeginRing = tmpBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY);
                        boolean tmpIsBeginConjPi = tmpBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY);
                        if (!tmpIsBeginRing && (tmpIsBeginTertiary || tmpIsBeginQuaternary) && !tmpIsBeginConjPi) {
                                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY, true);
                                int tmpBeginAtomIndex = tmpBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                                int tmpEndAtomIndex = tmpBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                                anAtomArray[tmpBeginAtomIndex].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY, true);
                                anAtomArray[tmpEndAtomIndex].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY, true);
                            }

                    }
                }
                tmpRingFragmentationContainer.addAtom(this.deepCopyAtom(tmpAtom));
            }
        }
        //</editor-fold>
        //<editor-fold desc="bond extraction">
        for (IBond tmpBond : aBondArray) {
            IAtom tmpBeginAtom = tmpBond.getBegin();
            IAtom tmpEndAtom = tmpBond.getEnd();
            //not ring and not conjugated
            if (!((boolean) tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY)
                    || (boolean) tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY))) {
                //<editor-fold desc="Bond Atoms Booleans">
                //booleans for bond begin and end atom properties used in fragmentation, self-explanatory
                boolean tmpIsBeginRing = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY);
                boolean tmpIsEndRing = tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY);
                //
                boolean tmpIsBeginConjPi = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY);
                boolean tmpIsEndConjPi = tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY);
                //
                boolean tmpIsBeginTertiary = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY);
                boolean tmpIsEndTertiary = tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY);
                //
                boolean tmpIsBeginQuaternary = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY);
                boolean tmpIsEndQuaternary = tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY);
                //
                boolean tmpIsBeginDouble = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY);
                boolean tmpIsEndDouble = tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY);
                //
                boolean tmpIsBeginTriple = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY);
                boolean tmpIsEndTriple = tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY);
                //
                boolean tmpIsBeginNeighbor = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY);
                boolean tmpIsEndNeighbor = tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY);
                //</editor-fold>
                //checks for double bond mark
                /*
                This case checks if the double bond may be connected to a ring and if true, places it in the same
                container as the ring as they are part of one substructure.
                 */
                if ((boolean) tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY)) {
                    System.out.println(1 + "BondIndex: " + tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY).toString());
                    if (tmpIsBeginRing || tmpIsEndRing) {
                        System.out.println(1.1 + "BondIndex: " + tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY).toString());
                        tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpRingFragmentationContainer));
                    } else {
                        System.out.println(1.2 + "BondIndex: " + tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY).toString());
                        tmpIsolatedMultiBondsContainer.addBond(this.deepCopyBond(tmpBond, tmpIsolatedMultiBondsContainer));
                    }
                }
                //checks for triple bond mark
                /*
                This case checks for isolated triple bonds, i.e. triple bonds in linear chains. As a triple bond can not
                be directly bonded to a ring structure, meaning not an 'internal' bond of a ring, it has to be isolated.
                 */
                else if ((boolean) tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY)) {
                    System.out.println(2 + "BondIndex: " + tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY).toString());
                    tmpIsolatedMultiBondsContainer.addBond(this.deepCopyBond(tmpBond, tmpIsolatedMultiBondsContainer));
                }
                //checks for neighbor mark
                else if ((boolean) tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY)) {
                    System.out.println(3 + "BondIndex: " + tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY).toString());
                    if (this.separateTertQuatCarbonFromRingSetting.get()) {
                        System.out.println("separate true");
                        if (!this.isolateTertQuatCarbonSetting.get()) {
                            if (!(tmpIsBeginRing || tmpIsEndRing)) {
                                tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpRingFragmentationContainer));
                            }
                        }
                    }
                    //separateTertQuatCarbonFromRingSetting == false
                    else {
                        System.out.println("separate off");
                        boolean tmpIsBondConnectedTertQuatRing = tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY);
                        boolean tmpIsBeginConnectedTertQuatRing = tmpBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY);
                        boolean tmpIsEndConnectedTertQuatRing = tmpBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY);
                        //skips over bonds between isolated rings and rings and linear chains
                        if ((tmpIsBeginRing && tmpIsEndRing) || (tmpIsBeginRing || tmpIsEndRing) && !tmpIsBondConnectedTertQuatRing) {
                            System.out.println(3.1 + "BondIndex: " + tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY).toString());
                            continue;
                        }
                        //adds bond connecting tertiary/quaternary carbons and rings
                        else if (tmpIsBondConnectedTertQuatRing) {
                            if (!(tmpIsBeginConjPi || tmpIsEndConjPi)) {
                                System.out.println(3.2 + "BondIndex: " + tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY).toString());
                                tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpRingFragmentationContainer));
                            }
                        //ToDo: connectTertQuatRing is falsely set true for conjPi without any ring structures
                        } else if ((tmpIsBeginConnectedTertQuatRing || tmpIsEndConnectedTertQuatRing) && (tmpIsBeginTertiary || tmpIsEndTertiary || tmpIsBeginQuaternary || tmpIsEndQuaternary)) {
                            System.out.println("is connect TertQuatRing -> " + tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY).toString());
                            if (this.isolateTertQuatCarbonSetting.get()) {
                                if ((tmpIsBeginConjPi || tmpIsEndConjPi)) {
                                    continue;
                                }
                            }
                            else {
                                System.out.println(3.3 + "BondIndex: " + tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY).toString());
                                tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpRingFragmentationContainer));
                            }
                        } else if ((tmpIsBeginTertiary || tmpIsEndTertiary) && (tmpIsBeginConjPi || tmpIsEndConjPi)) {
                            System.out.println("is conj pi + TertQuat -> " + tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY).toString());
                            if (this.isolateTertQuatCarbonSetting.get()) {
                                continue;
                            } else {
                                tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpRingFragmentationContainer));
                            }
                        }
                        else {
                            System.out.println(3.4 + "BondIndex: " + tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY).toString());
                            if (!this.isolateTertQuatCarbonSetting.get()) {
                                tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpRingFragmentationContainer));
                            }
                        }
                    }
                }
                else if ((tmpIsBeginConjPi || tmpIsEndConjPi) && (tmpIsBeginTertiary || tmpIsEndTertiary || tmpIsBeginQuaternary || tmpIsEndQuaternary)) {
                    System.out.println(4 + "BondIndex: " + tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY).toString());
                    if (!this.isolateTertQuatCarbonSetting.get()) {
                        System.out.println(4.1 + "BondIndex: " + tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY).toString());
                        tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpRingFragmentationContainer));
                    }
                }
                else {
                    System.out.println(5 + "BondIndex: " + tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY).toString());
                    if (this.isolateTertQuatCarbonSetting.get()) {
                        System.out.println(5.1 + "BondIndex: " + tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY).toString());
                        //stops bond extraction for bonds between double/triple bond atoms and neighbor atoms
                        if (tmpIsBeginDouble || tmpIsEndDouble || tmpIsBeginTriple || tmpIsEndTriple) {
                            continue;
                        } else {
                            tmpChainFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpChainFragmentationContainer));
                        }
                    }
                    //extracts bonds where atoms DO NOT have the following markers active: (ring and conjugated pi) and (double or triple)
                    //as well as (tertiary or quaternary or neighbor)
                    else if (!(tmpIsBeginRing && tmpIsEndRing && tmpIsBeginConjPi && tmpIsEndConjPi) && !(tmpIsBeginDouble || tmpIsEndDouble || tmpIsBeginTriple || tmpIsEndTriple)) {
                            if (!(tmpIsBeginTertiary || tmpIsEndTertiary || tmpIsBeginQuaternary || tmpIsEndQuaternary || tmpIsBeginNeighbor || tmpIsEndNeighbor)) {
                                tmpChainFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpChainFragmentationContainer));
                        }
                    }
                }
            }
            else { //extracts ring or conjugated bonds
                tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpRingFragmentationContainer));
            }
        }
        //</editor-fold>
        //</editor-fold>
        //extracts disconnected ring structures from one atomcontainer into atomcontainer set
        IAtomContainerSet tmpRingACSet = new AtomContainerSet();
        if (!tmpRingFragmentationContainer.isEmpty()) {
            tmpRingACSet = this.separateDisconnectedStructures(tmpRingFragmentationContainer);
        }
        //extracts disconnected isolated tertiary and quaternary systems into atomcontainer set
        IAtomContainerSet tmpSingleACSet = new AtomContainerSet();
        if (!tmpTertQuatCarbonContainer.isEmpty()) {
            tmpExtractionSet.add(this.separateDisconnectedStructures(tmpTertQuatCarbonContainer));
        }
        //if more than one atomcontainer containing a ring system is present, add to extraction atomcontainer set
        if (!tmpRingACSet.isEmpty() && tmpRingACSet.getAtomContainerCount() > 0) {
            tmpExtractionSet.add(tmpRingACSet);
        }
        //if more than one atomcontainer containing singular structures is present, add it to the extraction set
        if (!tmpSingleACSet.isEmpty() && tmpSingleACSet.getAtomContainerCount() > 0) {
            tmpExtractionSet.add(tmpSingleACSet);
        }
        //remnants after ring, conj. system and tertiary/quaternary carbon extractions
        //expected to be only linear carbohydrates
        if (!tmpIsolatedMultiBondsContainer.isEmpty()) {
            tmpExtractionSet.add(this.separateDisconnectedStructures(tmpIsolatedMultiBondsContainer));
        }
        IAtomContainerSet tmpChainACSet = this.separateDisconnectedStructures(tmpChainFragmentationContainer);
        //ACSet for dissected chains
        IAtomContainerSet tmpDissectedChainACSet = new AtomContainerSet();
        int tmpMaxChainLengthInteger = this.maxChainLengthSetting.get();
        //not sure about this Exception Handling
        try {
            if (tmpMaxChainLengthInteger < 1) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException tmpIllegalMaxChainLength) {
            AlkylStructureFragmenter.LOGGER.log(Level.WARNING, String.format(LOGGER_EXCEPTION_STRING_FORMAT,
                    tmpIllegalMaxChainLength.toString(), anAtomArray[0].getContainer().getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY),
                    "Illegal maximum chain length."));
        }
        //checks for applied restrictions, default restriction is set to 6
        if (this.fragmentSideChainsSetting.get() && tmpMaxChainLengthInteger > 0) {
            //check maxchainlength: 0 -> no restrictions, 1 -> only methanes, >= 2 -> respective alkane
            if (tmpMaxChainLengthInteger == 1) {//single methane molecules
                IAtomContainer tmpDissectedAC = new AtomContainer();
                for (IAtomContainer tmpAtomContainer : tmpChainACSet.atomContainers()) {
                    tmpAtomContainer.removeAllBonds();
                    tmpDissectedAC.add(tmpAtomContainer);
                }
                tmpDissectedChainACSet.add(this.separateDisconnectedStructures(tmpDissectedAC));
            } else {//restrictions > 1
                for (IAtomContainer tmpAtomContainer : tmpChainACSet.atomContainers()) {
                    IAtomContainer tmpDissectedAC = this.dissectLinearChain(tmpAtomContainer, tmpMaxChainLengthInteger);
                    tmpDissectedChainACSet.add(this.separateDisconnectedStructures(tmpDissectedAC));
                }
            }
        } else {
            //no restrictions applied (maxChainLengthSetting = 0)
            tmpDissectedChainACSet.add(tmpChainACSet);
        }
        if (!tmpDissectedChainACSet.isEmpty() && tmpDissectedChainACSet.getAtomContainerCount() > 0) {
            tmpExtractionSet.add(tmpDissectedChainACSet);
        }
        return tmpExtractionSet;
    }
    /**
     * Protected method to dissect given AtomContainer (containing linear carbon chain) into separate molecules with given length.
     *
     * Returns remnants of chains as disconnected structures if they are falling short of set maximum length (i.e. set maximum is 6, chain is 8 C's long -> fragment of length 6 is returned, together with a disconnected remnant of length 2).
     * The used counter starts at 1 as to allow a one-to-one "translation" of user input for the setting and implementation.
     *
     * @param aLinearChainAC AtomContainer to be dissected
     * @param aMaxChainLength Given maximum length of molecule
     * @return AtomContainer with separate dissected molecules
     */
    protected IAtomContainer dissectLinearChain(IAtomContainer aLinearChainAC, int aMaxChainLength) {
        IAtomContainer tmpReturnAC = new AtomContainer();
        int tmpMaxBondCount = aMaxChainLength - 1;
        int tmpInternalBondCount = 0;
        for (IAtom tmpAtom: aLinearChainAC.atoms()) {
            //ToDo: investigate this problem
            //index of tmpAtom is set to -1 when deep copied, no idea why
            tmpReturnAC.addAtom(this.deepCopyAtom(tmpAtom));
        }
        for (IBond tmpBond: aLinearChainAC.bonds()) {
            if (tmpInternalBondCount < tmpMaxBondCount) {
                tmpReturnAC.addBond(this.deepCopyBond(tmpBond, tmpReturnAC));
                tmpInternalBondCount++;
            } else if (tmpInternalBondCount == tmpMaxBondCount) {
                tmpInternalBondCount = 0;
            }
        }
        return tmpReturnAC;
    }
    /**
     * Protected method to mark atoms and bonds with order of double or triple.
     *
     * @param aMolecularArraysInstance MolecularArrays instance for data transfer between methods
     * @param anAtomArray Atom array providing the atoms of a molecule
     * @param aBondArray Bond array providing the bonds of a molecule
     */
    protected void markMultiBonds(MolecularArrays aMolecularArraysInstance, IAtom[] anAtomArray, IBond[] aBondArray) {
        Objects.requireNonNull(anAtomArray);
        Objects.requireNonNull(aBondArray);
        for (IBond tmpArrayBond: aBondArray) {
            if (tmpArrayBond.getOrder().numeric() == 2) {
                tmpArrayBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY, true);
                int tmpBeginIndex = tmpArrayBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                int tmpEndIndex = tmpArrayBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                for (IAtom tmpAtom: anAtomArray) {
                    int tmpAtomIndex = tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                    if (tmpAtomIndex == tmpBeginIndex || tmpAtomIndex == tmpEndIndex) {
                        tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY, true);
                    }
                }
            } else if (tmpArrayBond.getOrder().numeric() == 3) {
                tmpArrayBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY, true);
                int tmpBeginIndex = tmpArrayBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                int tmpEndIndex = tmpArrayBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                for (IAtom tmpAtom: anAtomArray) {
                    int tmpAtomIndex = tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                    if ( tmpAtomIndex == tmpBeginIndex || tmpAtomIndex == tmpEndIndex) {
                        tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY, true);
                    }
                }
            }
        }
        aMolecularArraysInstance.setAtomArray(anAtomArray);
        aMolecularArraysInstance.setBondArray(aBondArray);
    }
    /**
     * Method to create a deeper copy of a given atom, meant to replace the default addAtom() method of IAtomContainer
     * in which only the reference is added to the atomcontainer instead of a new atom.
     *
     * @param anAtomToCopy the atom to create a deep copy of
     * @return the deep copy of the given atom
     */
    protected IAtom deepCopyAtom(IAtom anAtomToCopy) {
        //seems like atom index is lost when deep copying is applied
        IAtom tmpNewAtom = this.chemObjectBuilderInstance.newAtom();
        tmpNewAtom.setAtomicNumber(anAtomToCopy.getAtomicNumber());
        tmpNewAtom.setImplicitHydrogenCount(anAtomToCopy.getImplicitHydrogenCount());
        tmpNewAtom.setCharge(anAtomToCopy.getCharge());
        tmpNewAtom.setProperties(anAtomToCopy.getProperties());
        return tmpNewAtom;
    }
    /**
     * Method to create a deep copy of a given bond, meant to replace the default addBond() method of IAtomContainer
     *
     * @param aBondToCopy the bond to create a deep copy of
     * @param aBondIncludingAtomContainer the atomcontainer in which the bond's atoms are placed in
     * @return the deep copy of the given bond
     */
    protected IBond deepCopyBond(IBond aBondToCopy, IAtomContainer aBondIncludingAtomContainer) {
        IBond tmpNewBond = this.chemObjectBuilderInstance.newBond();
        tmpNewBond.setOrder(aBondToCopy.getOrder());
        tmpNewBond.setProperties(aBondToCopy.getProperties());
        IAtom tmpBeginAtom = aBondToCopy.getBegin();
        int tmpBeginAtomIndex = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
        if ((tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY) != null)) {
            int tmpEndAtomIndex = aBondToCopy.getEnd().getProperty(INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
            for (IAtom tmpAtom : aBondIncludingAtomContainer.atoms()) {
                if ((int) tmpAtom.getProperty(INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY) == tmpBeginAtomIndex) {
                    tmpNewBond.setAtom(tmpAtom, 0);
                } else if ((int) tmpAtom.getProperty(INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY) == tmpEndAtomIndex) {
                    tmpNewBond.setAtom(tmpAtom, 1);
                }
            }
        } else {
            throw new IllegalArgumentException();
        }
        return tmpNewBond;
    }

    /**
     * Method to create deep copy of a given bond, with the respective atoms given as parameters.
     *
     * @param aBondToCopy IBond instance to create a deep copy of
     * @param aBondBeginAtom Begin atom of bond to deep copy
     * @param aBondEndAtom End atom of bond to deep copy
     * @return New IBond instance with properties of given bond, therefore a deep copy
     */
    protected IBond deepCopyBond(IBond aBondToCopy, IAtom aBondBeginAtom, IAtom aBondEndAtom){
        IBond tmpNewBond = this.chemObjectBuilderInstance.newBond();
        tmpNewBond.setOrder(aBondToCopy.getOrder());
        tmpNewBond.setProperties(aBondToCopy.getProperties());
        tmpNewBond.setAtom(aBondBeginAtom, 0);
        tmpNewBond.setAtom(aBondEndAtom, 1);
        return tmpNewBond;
    }
    //</editor-fold>
}
