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
 * <p>
 *     ToDo: logger environments (fine, finer, finest)
 *     ToDo: Investigate why atoms are duplicated (in tests) when tert/quat including molecule.
 *     ToDo: Investigate NoSuchAtomExceptions of tests
 *     ToDo: library of possible alkyl substructures (include in Tutorial)
 *     ToDo: check out markRingAtomsAndBonds() from Cycles
 * </p>
 *
 * @author Maximilian Rottmann (maximilian.rottmann@studmail.w-hs.de)
 * @version 1.0.0.0
 */
public class AlkylStructureFragmenter implements IMoleculeFragmenter{
    //<editor-fold desc="Inner Class 'MolecularArrays'">

    /**
     * Inner class for internal data transfer of atoms and bonds in arrays.
     * On initialization, the constructor receives a molecule from which the atoms and bonds are to be placed in arrays.
     * Each array 'filling' routine checks and removes any null elements, should there be any.
     * In the same step, the atom and bond properties are set to an initialization value (e.g. false) except their index
     * which is set to their respective increasing count.
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
            ArrayList<IAtom> tmpAtomArrayList = new ArrayList<>(aMolecule.getAtomCount());
            ArrayList<IBond> tmpBondArrayList = new ArrayList<>(aMolecule.getBondCount());
            //<editor-fold desc="Fill Atom Array">
            //set to -1 to ensure correct index counting below, starting at 0
            int tmpAlkylSFAtomIndex = 0;
            for (IAtom tmpAtom: aMolecule.atoms()) {
                if (tmpAtom != null) {
                    //set atom properties, IMPORTANT: this needs to be done in array filling step for correct detection of
                    //tertiary or quaternary carbon atoms and their neighbors
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY, tmpAlkylSFAtomIndex++);
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY, false);
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, false);
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY, false);
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY, false);
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY, false);
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY, false);
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, false);
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY, false);
                    tmpAtomArrayList.add(tmpAtom);
                }
            }
            int tmpAlkylSFBondIndex = 0;
            for (IBond tmpBond: aMolecule.bonds()) {
                if (tmpBond != null) {
                    tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY, tmpAlkylSFBondIndex++);
                    tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY, false);
                    tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, false);
                    tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY, false);
                    tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY, false);
                    tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, false);
                    tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY, false);
                    tmpBondArrayList.add(tmpBond);
                }
            }
            this.atomArray = tmpAtomArrayList.toArray(new IAtom[tmpAtomArrayList.size()]);
            this.bondArray = tmpBondArrayList.toArray(new IBond[tmpBondArrayList.size()]);
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
    //</editor-fold

    //<editor-fold desc="Inner Class 'Edge'">

    class Edge {
        public int edgeBegin = -1;
        public int edgeEnd = -1;
        public IBond.Order bondOrder = IBond.Order.UNSET;
        public Edge(int anEdgeBegin, int anEdgeEnd, IBond.Order aBondOrder) {
            this.edgeBegin = anEdgeBegin;
            this.edgeEnd = anEdgeEnd;
            this.bondOrder = aBondOrder;
        }
    }
    //</editor-fold>

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
    //</editor-fold>
    //</editor-fold>
    //<editor-fold desc="Private Class Variables">

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
    public void setIsolateTertQuatCarbonsSetting(boolean aBoolean){
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
        tmpCopy.setIsolateTertQuatCarbonsSetting(this.isolateTertQuatCarbonSetting.get());
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
     *     atoms and bonds neighboring tertiary and quaternary carbons; singular rings and ring systems;
     *     conjugated pi bond systems; bonds of higher order (>1) present in linear sidechains.
     * </p>
     * <p>
     *     Extraction of marked substructures is done by creating deep copies of the cloned molecule's atoms and bonds,
     *     and placing them in designated new IAtomContainer instances. During extraction, the preservation of the
     *     chemical formula is checked, and a warning logged if not true.
     *     The order of extraction is as follows: rings, conjugated pi bond systems and their fusion products;
     *     additional double bonds connected to rings; isolated bonds of higher order;
     *     atoms and bonds neighboring tertiary or quaternary carbons; residual atoms and bonds as linear chains.
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
        //ToDo: put into fine debug
        //AlkylStructureFragmenter.LOGGER.log(Level.INFO, "PreFragAtomCount: " + tmpPreFragmentationAtomCount);
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
        this.markRings(tmpMolecularArrays, tmpClone);
        this.markNeighborAtomsAndBonds(tmpMolecularArrays);
        this.markConjugatedPiSystems(tmpMolecularArrays);
        this.markMultiBonds(tmpMolecularArrays);
        //</editor-fold>
        //<editor-fold desc="Fragment Extraction and Saturation" defaultstate="collapsed">
        try {
            int tmpPostFragmentationAtomCount = 0;
            IAtomContainerSet tmpFragmentSet = this.extractFragments(tmpMolecularArrays);
            for (IAtomContainer tmpAtomContainer: tmpFragmentSet.atomContainers()) {
                for (IAtom tmpAtom: tmpAtomContainer.atoms()) {
                    if (tmpAtom.getAtomicNumber() != 0) {
                        tmpPostFragmentationAtomCount++;
                    }
                }
            }
            //ToDo: Put into fine debug
            //AlkylStructureFragmenter.LOGGER.log(Level.INFO, "PostFragAtomCount: " + tmpPostFragmentationAtomCount);
            if (tmpPostFragmentationAtomCount != tmpPreFragmentationAtomCount) {
                AlkylStructureFragmenter.LOGGER.log(Level.WARNING, String.format(LOGGER_WARNING_STRING_FORMAT,
                        "Chemical Formula Check", tmpClone.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY),
                        "Chemical formula was not constant!"));
            }
            if (this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION)) {
                tmpFragmentSet = this.saturateWithImplicitHydrogen(tmpFragmentSet);
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
     * Protected method for detecting and marking tertiary or quaternary carbon atoms and their surrounding neighbor atoms and bonds.
     *
     * @param aMolecularArraysInstance MolecularArrays instance for data transfer between methods
     */
    protected void markNeighborAtomsAndBonds(MolecularArrays aMolecularArraysInstance) {
        Objects.requireNonNull(aMolecularArraysInstance);
        IAtom[] tmpAtomArray = aMolecularArraysInstance.getAtomArray();
        IBond[] tmpBondArray = aMolecularArraysInstance.getBondArray();
        //set general atom and specific bond properties
        for (int tmpAtomIndex = 0; tmpAtomIndex < tmpAtomArray.length; tmpAtomIndex++) {
            IAtom tmpAtom = tmpAtomArray[tmpAtomIndex];
            if (tmpAtom != null) {
                //marking of tertiary or quaternary carbons together with their neighbor atoms
                if ((tmpAtom.getBondCount() == 3 || tmpAtom.getBondCount() == 4) && tmpAtom.getMaxBondOrder() == IBond.Order.SINGLE && !((boolean) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY))) {
                    if (tmpAtom.getBondCount() == 3) {
                        tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY, true);
                    }
                    if (tmpAtom.getBondCount() == 4) {
                        tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY, true);
                    }
                    for (IBond tmpBond: tmpAtom.bonds()) {
                        tmpBondArray[(int) tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY)].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                    }
                    for (IAtom tmpNeighborAtom: tmpAtom.neighbors()) {
                        tmpAtomArray[(int) tmpNeighborAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY)].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                    }
                }
            }
        }
        aMolecularArraysInstance.setAtomArray(tmpAtomArray);
        aMolecularArraysInstance.setBondArray(tmpBondArray);
    }
    /**
     * Protected method to mark all atoms and bonds of any rings in the given atomcontainer.
     *
     * @param aMolecularArraysInstance MolecularArrays instance for data transfer between methods
     * @param anAtomContainer IAtomContainer to mark atoms and bonds in
     */
    //ToDo: check out markRingAtomAndBonds from Cycles
    protected void markRings(MolecularArrays aMolecularArraysInstance, IAtomContainer anAtomContainer) throws IllegalArgumentException {
        Objects.requireNonNull(aMolecularArraysInstance);
        Objects.requireNonNull(anAtomContainer);
        IAtom[] tmpAtomArray = aMolecularArraysInstance.getAtomArray();
        IBond[] tmpBondArray = aMolecularArraysInstance.getBondArray();
        //<editor-fold desc="CycleFinder (MCB)">
        CycleFinder tmpMCBCycleFinder = Cycles.mcb();
        IRingSet tmpMCBCyclesSet;
        try {
            Cycles tmpMCBCycles = tmpMCBCycleFinder.find(anAtomContainer);
            tmpMCBCyclesSet = tmpMCBCycles.toRingSet();
            for (IAtomContainer tmpContainer: tmpMCBCyclesSet.atomContainers()) {
                for (IAtom tmpRingAtom: tmpContainer.atoms()) {
                    int tmpAtomInteger = tmpRingAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                    IAtom tmpAtom = tmpAtomArray[tmpAtomInteger];
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY, true);
                }
                for (IBond tmpRingBond: tmpContainer.bonds()) {
                    int tmpBondInteger = tmpRingBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY);
                    IBond tmpBond = tmpBondArray[tmpBondInteger];
                    tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY, true);
                }
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
        aMolecularArraysInstance.setAtomArray(tmpAtomArray);
        aMolecularArraysInstance.setBondArray(tmpBondArray);
    }
    /**
     * Protected method to mark all atoms and bonds of any conjugated pi systems in the given arrays.
     *
     * @param aMolecularArraysInstance MolecularArrays instance for data transfer between methods
     */
    protected void markConjugatedPiSystems(MolecularArrays aMolecularArraysInstance) throws IllegalArgumentException{
        Objects.requireNonNull(aMolecularArraysInstance);
        IAtom[] tmpAtomArray = aMolecularArraysInstance.getAtomArray();
        IBond[] tmpBondArray = aMolecularArraysInstance.getBondArray();
        //no particular algorithm in mind
        //iterate over every bond in bond array
        for (IBond tmpArrayBond: tmpBondArray) {
            if ((boolean) tmpArrayBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY)) {
                continue;
            }
            //bools for double-single-double conjugation
            boolean tmpIsStartBondDouble = false;
            boolean tmpIsNeighborBondSingle = false;
            boolean tmpIsSecondNeighborBondDouble = false;
            //bools for triple-single-triple conjugation
            boolean tmpIsStartBondTriple = false;
            boolean tmpIsSecondNeighborBondTriple = false;
            if (tmpArrayBond.getOrder() == IBond.Order.DOUBLE) {
                tmpIsStartBondDouble = true;
            } else if (tmpArrayBond.getOrder() == IBond.Order.TRIPLE) {
                tmpIsStartBondTriple = true;
            }
            //iterate over each atom of bond
            for (IAtom tmpArrayBondAtom: tmpArrayBond.atoms()) {
                //iterate over non-array bonds of atom
                for (IBond tmpNeighborBond : tmpArrayBondAtom.bonds()) {
                    if (tmpNeighborBond.equals(tmpArrayBond)) {
                        continue;
                    }
                    if (tmpNeighborBond.getOrder() == IBond.Order.SINGLE) {
                        tmpIsNeighborBondSingle = true;
                    }
                    //iterate over non-array-bond atoms of neighbor bond
                    for (IAtom tmpNeighborAtom: tmpNeighborBond.atoms()) {
                        if (tmpNeighborAtom.equals(tmpArrayBondAtom)) {
                            continue;
                        }
                        for (IBond tmpSecondNeighborBond: tmpNeighborAtom.bonds()) {
                            if (tmpSecondNeighborBond.equals(tmpNeighborBond)) {
                                continue;
                            }
                            if (tmpSecondNeighborBond.getOrder() == IBond.Order.DOUBLE) {
                                tmpIsSecondNeighborBondDouble = true;
                            } else if (tmpSecondNeighborBond.getOrder() == IBond.Order.TRIPLE) {
                                tmpIsSecondNeighborBondTriple = true;
                            }
                            //alternating pattern of D-S-D or T-S-T -> conjugation detected
                            if (((tmpIsStartBondDouble && tmpIsSecondNeighborBondDouble) || (tmpIsStartBondTriple && tmpIsSecondNeighborBondTriple)) && tmpIsNeighborBondSingle) {
                                //set conjugated property for start bond and it's atoms
                                tmpArrayBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
                                tmpAtomArray[(int) tmpArrayBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY)].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
                                tmpAtomArray[(int) tmpArrayBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY)].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
                                //set conjugated property for neighbor bond and atoms
                                tmpNeighborBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
                                tmpAtomArray[(int) tmpNeighborBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY)].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
                                tmpAtomArray[(int) tmpNeighborBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY)].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
                                //set conjugated property for second neighbor bond and atoms
                                tmpSecondNeighborBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
                                tmpAtomArray[(int) tmpSecondNeighborBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY)].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
                                tmpAtomArray[(int) tmpSecondNeighborBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY)].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
                            }
                        }
                    }
                }
            }
        }

        /*
        //create adjacency map for bonds
        Map<IBond, Set<IBond>> tmpBondAdjacencyMap = new HashMap<>();
        for (IBond tmpBond: tmpBondArray) {
            tmpBondAdjacencyMap.put(tmpBond, new HashSet<>());
        }
        //true line graph?
        for (IAtom tmpAtom: tmpAtomArray) {
            //get connected bonds
            List<IBond> tmpConnectedBondsList = new ArrayList<>(4);
            for (IBond tmpBond: tmpBondArray) {
                if (tmpBond.contains(tmpAtom)) {
                    tmpConnectedBondsList.add(tmpBond);
                }
            }
            for (int i = 0; i < tmpConnectedBondsList.size(); i++) {
                IBond tmpFirstBond = tmpConnectedBondsList.get(i);
                for (int j = i + 1; j < tmpConnectedBondsList.size(); j++) {
                    IBond tmpSecondBond = tmpConnectedBondsList.get(j);
                    //check if bonds may form conjugated pi bond system -> alternating double-single
                    if ((tmpFirstBond.getOrder() == IBond.Order.SINGLE && tmpSecondBond.getOrder() == IBond.Order.DOUBLE)
                            || tmpFirstBond.getOrder() == IBond.Order.DOUBLE && tmpSecondBond.getOrder() == IBond.Order.SINGLE) {
                        tmpBondAdjacencyMap.get(tmpFirstBond).add(tmpSecondBond);
                        tmpBondAdjacencyMap.get(tmpSecondBond).add(tmpFirstBond);
                    }
                }
            }
        }
        Set<IBond> tmpVisitedBonds = new HashSet<>();
        for (IBond tmpStartBond: tmpBondArray) {
            if (tmpVisitedBonds.contains(tmpStartBond) || tmpStartBond.getOrder() != IBond.Order.DOUBLE) {
                continue;
            }
            Deque<IBond> tmpBondStack = new ArrayDeque<>();
            tmpBondStack.push(tmpStartBond);
            tmpVisitedBonds.add(tmpStartBond);
            tmpStartBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
            stackIteration:
            while (!tmpBondStack.isEmpty()) {
                IBond tmpCurrentBond = tmpBondStack.pop();
                int tmpBeginAtomIndex = tmpCurrentBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                int tmpEndAtomIndex = tmpCurrentBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                tmpAtomArray[tmpBeginAtomIndex].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
                tmpAtomArray[tmpEndAtomIndex].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
                if (tmpBondAdjacencyMap.get(tmpCurrentBond).size() == 1) {
                    continue stackIteration;
                }
                for (IBond tmpNeighborBond: tmpBondAdjacencyMap.get(tmpCurrentBond)) {
                    if (!tmpVisitedBonds.contains(tmpNeighborBond)) {
                        tmpBondStack.push(tmpNeighborBond);
                        tmpVisitedBonds.add(tmpNeighborBond);
                        tmpNeighborBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
                    }
                }
            }
        }
        for (IAtom tmpAtom: tmpAtomArray) {
            System.out.println(tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY).toString());
        }
        */

        /*
        //create list of edges with bond begin and end atom index and bond order
        List<Edge> tmpEdgeList = new ArrayList<>(tmpBondArray.length);
        //ToDo: all this info is already in bonds of bondarray
        for (IBond tmpBond: tmpBondArray) {
            int tmpBeginIndex = tmpBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
            int tmpEndIndex = tmpBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
            tmpEdgeList.add(new Edge(tmpBeginIndex, tmpEndIndex, tmpBond.getOrder()));
        }
        //create and fill list with integer lists, each integer list representing an atom with bonds as their index value
        // (i.e. [[0, 1], [0], [1, 2], [2, 3], [3]]
        List<List<Integer>> tmpIncidenceList = new ArrayList<>(tmpBondArray.length);
        for (int i = 0; i < tmpAtomArray.length; i++) {
            tmpIncidenceList.add(new ArrayList<>());
        }
        //fill incidence list with corresponding indices
        for (IBond tmpOriginBond: tmpBondArray) {
            int tmpBondIndex = tmpOriginBond.getIndex();
            IAtom tmpBeginAtom = tmpOriginBond.getBegin();
            int tmpBeginIndex = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
            IAtom tmpEndAtom = tmpOriginBond.getEnd();
            int tmpEndIndex = tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
            tmpIncidenceList.get(tmpBeginIndex).add(tmpBondIndex);
            tmpIncidenceList.get(tmpEndIndex).add(tmpBondIndex);
        }
        System.out.println(tmpIncidenceList);
        */
        /*
        //create adjacency list for line graph nodes
        List<List<Integer>> tmpLineGraphAdjacencyList = new ArrayList<>(tmpBondArray.length);
        for (int i = 0; i < tmpBondArray.length; i++) {
            tmpLineGraphAdjacencyList.add(new ArrayList<>());
        }
        for (int atomIndex = 0; atomIndex < tmpAtomArray.length; atomIndex++) {
            for (int incidence = 0; incidence < tmpIncidenceList.get(atomIndex).size(); incidence++) {
                for (int j = incidence + 1; j < tmpIncidenceList.get(atomIndex).size(); j++) {
                    int tmpEdgeIncidence = tmpIncidenceList.get(atomIndex).get(incidence);
                    int tmpEdgeIncidenceAttached = tmpIncidenceList.get(atomIndex).get(j);
                }
            }
        } */

        /*
        for (int atom = 0; atom < numAtoms; atom++) {
            List<Integer> inc = incident.get(atom);
            for (int i = 0; i < inc.size(); i++) {
                for (int j = i+1; j < inc.size(); j++) {
                    int ei = inc.get(i), ej = inc.get(j);
                    if (edges.get(ei).isDouble != edges.get(ej).isDouble) {
                        lineAdj.get(ei).add(ej);
                        lineAdj.get(ej).add(ei);
                    }
                }
            }
        }
         */

        /*
        //<editor-fold desc="Old ConjPiSysDetection + Mapping">
        try {
            IAtomContainerSet tmpConjugatedAtomContainerSet = ConjugatedPiSystemsDetector.detect(anAtomContainer);
            //molecule mapping
            //iterate over every atomcontainer from ConjPiSystemsDetector output
            for (IAtomContainer tmpConjAtomContainer: tmpConjugatedAtomContainerSet.atomContainers()) {
                for (IAtom tmpConjAtom: tmpConjAtomContainer.atoms()) {
                    int tmpAtomInteger = tmpConjAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                    IAtom tmpAtom = tmpAtomArray[tmpAtomInteger];
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
                }
                for (IBond tmpConjBond: tmpConjAtomContainer.bonds()) {
                    int tmpBondInteger = tmpConjBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY);
                    IBond tmpBond = tmpBondArray[tmpBondInteger];
                    tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
                }
            }
            aMolecularArraysInstance.setAtomArray(tmpAtomArray);
            aMolecularArraysInstance.setBondArray(tmpBondArray);
        } catch (Exception anException) {
            AlkylStructureFragmenter.LOGGER.log(Level.WARNING, String.format(LOGGER_EXCEPTION_STRING_FORMAT,
                    anException, anAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY),
                    "Conjugated Pi Systems detection failed."), anException);
            throw new IllegalArgumentException(String.format(LOGGER_EXCEPTION_STRING_FORMAT,
                    anException, anAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY),
                    "Conjugated Pi Systems detection failed."));
        }
        //</editor-fold>
        */
    }
    /**
     * Protected method to mark atoms and bonds with order of double or triple.
     *
     * @param aMolecularArraysInstance MolecularArrays instance for data transfer between methods
     */
    protected void markMultiBonds(MolecularArrays aMolecularArraysInstance) {
        Objects.requireNonNull(aMolecularArraysInstance);
        IAtom[] tmpAtomArray = aMolecularArraysInstance.getAtomArray();
        IBond[] tmpBondArray = aMolecularArraysInstance.getBondArray();
        for (IBond tmpArrayBond: tmpBondArray) {
            if (tmpArrayBond.getOrder().numeric() == 2) {
                tmpArrayBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY, true);
                int tmpBeginIndex = tmpArrayBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                int tmpEndIndex = tmpArrayBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                for (IAtom tmpAtom: tmpAtomArray) {
                    int tmpAtomIndex = tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                    if (tmpAtomIndex == tmpBeginIndex || tmpAtomIndex == tmpEndIndex) {
                        tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY, true);
                    }
                }
            } else if (tmpArrayBond.getOrder().numeric() == 3) {
                tmpArrayBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY, true);
                int tmpBeginIndex = tmpArrayBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                int tmpEndIndex = tmpArrayBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                for (IAtom tmpAtom: tmpAtomArray) {
                    int tmpAtomIndex = tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                    if ( tmpAtomIndex == tmpBeginIndex || tmpAtomIndex == tmpEndIndex) {
                        tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY, true);
                    }
                }
            }
        }
        aMolecularArraysInstance.setAtomArray(tmpAtomArray);
        aMolecularArraysInstance.setBondArray(tmpBondArray);
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
            //ToDo: put into fine debug
            //AlkylStructureFragmenter.LOGGER.log(Level.INFO, System.currentTimeMillis() + " start sD, AC size: " + anAtomContainer.getAtomCount() + ", " + anAtomContainer.getBondCount());
            if (!anAtomContainer.isEmpty()) {
                //System.out.println("Con-Checker " + ChemUtil.createUniqueSmiles(anAtomContainer, false));
                //ToDo: Investigate which and where atoms are going missing -> changes to tertiary/quaternary extraction responsible?
                    //-> it seems an additional bond is copied (the bond connnecting ring and tert/quat system)
                    //ring and tert/quat are extracted correctly (when isolation = true) though the residual atoms from the tert/quat system are not correct
                //-> isolation seems to be the underlying issue
                //ToDo: create library for all possible alkyl substructures
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
            //ToDo: put into fine debug
            //AlkylStructureFragmenter.LOGGER.log(Level.INFO, System.currentTimeMillis() + " start sD, AC size: " + anAtomContainer.getAtomCount() + ", " + anAtomContainer.getBondCount());
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
     * @param aMolecularArrays MolecularArrays instance for data transfer between methods
     * @return IAtomContainerSet with extracted molecules
     * @throws CloneNotSupportedException if input cannot be cloned
     */
    protected IAtomContainerSet extractFragments(MolecularArrays aMolecularArrays) throws CloneNotSupportedException, IllegalArgumentException {
        Objects.requireNonNull(aMolecularArrays);
        IAtom[] tmpAtomArray = aMolecularArrays.getAtomArray();
        IBond[] tmpBondArray = aMolecularArrays.getBondArray();
        //
        //<editor-fold desc="Extraction">
        IAtomContainerSet tmpExtractionSet = new AtomContainerSet();
        //ToDo: perhaps move all to one AtomContainer and simply check connectivity at end
        //ToDo: split into separate methods (returning one atomcontainer, could make above more viable)
        //ToDo: separate tert/quat atom into method
        //ToDo: check if neighbor ring detection in neighbor extract possible
        //ToDo: fix
        IAtomContainer tmpRingFragmentationContainer = this.chemObjectBuilderInstance.newAtomContainer();
        IAtomContainer tmpChainFragmentationContainer = this.chemObjectBuilderInstance.newAtomContainer();
        IAtomContainer tmpIsolatedMultiBondsContainer = this.chemObjectBuilderInstance.newAtomContainer();
        IAtomContainer tmpTertQuatCarbonContainer = this.chemObjectBuilderInstance.newAtomContainer();
        //
        //<editor-fold desc="atom extraction">
        atomIteration:
        for (IAtom tmpAtom : tmpAtomArray) {
            //checks atom if not part of ring or conjugated pi system
            if (!((boolean) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY)
                    || (boolean) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY))) {
                //Checks for tertiary or quaternary mark
                if ((boolean) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY)
                        || (boolean) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY)) {
                    if (this.isolateTertQuatCarbonSetting.get()) {
                        if (!this.separateTertQuatCarbonFromRingSetting.get()) {
                            //checks for connected ring atom in neighbors
                            for (IAtom tmpNeighborAtom : tmpAtom.neighbors()) {
                                if ((boolean) tmpNeighborAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY)) {
                                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY, true);
                                    tmpRingFragmentationContainer.addAtom(this.deepCopyAtom(tmpAtom));
                                    continue atomIteration;
                                }
                            }
                        }
                        IAtomContainer tmpContainer = this.chemObjectBuilderInstance.newAtomContainer();
                        tmpContainer.addAtom(this.deepCopyAtom(tmpAtom));
                        //adds pseudo atoms with bonds depending on carbon configuration (tertiary/quaternary)
                        for (int i = 0; i < 4; i++) {
                            if ((boolean) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY) && i == 3) {
                                break ;
                            }
                            tmpContainer.addAtom(new PseudoAtom());
                            IBond tmpBond = new Bond();
                            tmpBond.setOrder(IBond.Order.SINGLE);
                            tmpBond.setAtom(tmpContainer.getAtom(0), 0);
                            tmpBond.setAtom(tmpContainer.getAtom(i+1), 1);
                            //deep copy not needed as referenced bond is new and has no connection to original molecule
                            tmpContainer.addBond(tmpBond);
                        }
                        tmpTertQuatCarbonContainer.add(tmpContainer);
                    //tertiary/quaternary carbons are added to ensure correct interaction with other substructures,
                        // neighbor atoms added later
                    } else {
                        //checks for connected ring atom in neighbors
                        for (IAtom tmpNeighborAtom : tmpAtom.neighbors()) {
                            if ((boolean) tmpNeighborAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY)) {
                                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY, true);
                            }
                        }
                        tmpRingFragmentationContainer.addAtom(this.deepCopyAtom(tmpAtom));
                    }
                }
                //checks for part of double bond
                else if ((boolean) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY)) {
                    //extracts non-cyclic double bonds possibly connected to a ring structure
                    for (IAtom tmpArrayAtom: tmpAtomArray) {
                        if (tmpArrayAtom != tmpAtom) {
                            for (IBond tmpAtomBond : tmpArrayAtom.bonds()) {
                                //check for double bond
                                if ((boolean) tmpAtomBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY)) {
                                    IAtom tmpBeginAtom = tmpAtomBond.getBegin();
                                    IAtom tmpEndAtom = tmpAtomBond.getEnd();
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
                            }
                        }
                    }
                //checks for marker | extracts triple bond
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
                if ((boolean) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY)) {
                    for (IAtom tmpNeighborAtom : tmpAtom.neighbors()) {
                        if (!(boolean) tmpNeighborAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY)
                                && ((boolean) tmpNeighborAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY)
                                || (boolean) tmpNeighborAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY))
                        ) {
                            tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY, true);
                        }
                    }
                }
                tmpRingFragmentationContainer.addAtom(this.deepCopyAtom(tmpAtom));
            }
        }
        //</editor-fold>
        //<editor-fold desc="bond extraction">
        for (IBond tmpBond : tmpBondArray) {
            IAtom tmpBeginAtom = tmpBond.getBegin();
            IAtom tmpEndAtom = tmpBond.getEnd();
            if (tmpBeginAtom.getAtomicNumber() == 0 || tmpEndAtom.getAtomicNumber() == 0) {
                continue;
            }
            //booleans for bond begin and end atom properties used in fragmentation, self-explanatory
            boolean tmpIsBeginRing = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY);
            boolean tmpIsEndRing = tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY);
            //
            boolean tmpIsBeginTertiary = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY);
            boolean tmpIsEndTertiary = tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY);
            //
            boolean tmpIsBeginQuaternary = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY);
            boolean tmpIsEndQuaternary = tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY);
            //
            //sets "connection ring-tertiary/ring-quaternary" property for bonds
            if ((boolean) tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY)
                    && (boolean) tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY)) {
                if ((!tmpIsBeginRing && (tmpIsBeginTertiary || tmpIsBeginQuaternary)) || (!tmpIsEndRing && (tmpIsEndTertiary || tmpIsEndQuaternary))) {
                    tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY, true);
                }
            }
            //check bond for not ring and not conjugated
            if (!((boolean) tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY)
                    || (boolean) tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY))) {
                //
                boolean tmpIsBeginConjPi = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY);
                boolean tmpIsEndConjPi = tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY);
                //
                boolean tmpIsBeginDouble = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY);
                boolean tmpIsEndDouble = tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY);
                //
                boolean tmpIsBeginTriple = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY);
                boolean tmpIsEndTriple = tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY);
                //
                boolean tmpIsBeginNeighbor = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY);
                boolean tmpIsEndNeighbor = tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY);
                //
                //checks for double bond mark
                /*
                This case checks if the double bond may be connected to a ring and if true, places it in the same
                container as the ring as they are part of one substructure.
                 */
                if ((boolean) tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY)) {
                    if (tmpIsBeginRing || tmpIsEndRing) {
                        tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpRingFragmentationContainer));
                    } else {
                        tmpIsolatedMultiBondsContainer.addBond(this.deepCopyBond(tmpBond, tmpIsolatedMultiBondsContainer));
                    }
                }
                //checks for triple bond mark
                /*
                This case checks for isolated triple bonds, i.e. triple bonds in linear chains. As a triple bond can not
                be directly bonded to a ring structure, meaning not an 'internal' bond of a ring, it has to be isolated.
                 */
                else if ((boolean) tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY)) {
                    tmpIsolatedMultiBondsContainer.addBond(this.deepCopyBond(tmpBond, tmpIsolatedMultiBondsContainer));
                }
                //checks for neighbor mark
                else if ((boolean) tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY)) {
                    //checks for setting to separate tertiary/quaternary carbons from ring structures
                    if (this.separateTertQuatCarbonFromRingSetting.get()) {
                        //checks for setting to isolate tertiary/quaternary carbons from their neighbor atoms
                        if (!this.isolateTertQuatCarbonSetting.get()) {
                            //checks if bond is connection between tert/quat carbon and ring structure
                            //adds pseudo atom to indicate bond to ring
                            if ((boolean) tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY)) {
                                PseudoAtom tmpPseudoAtom = new PseudoAtom();
                                tmpPseudoAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY, false);
                                if (!(boolean) tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY)) {
                                    int tmpAtomIndex = tmpBeginAtom.getProperty(INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                                    IBond tmpNewBond = new Bond();
                                    tmpNewBond.setOrder(IBond.Order.SINGLE);
                                    tmpNewBond.setAtom(tmpPseudoAtom, 0);
                                    for (IAtom tmpAtom: tmpRingFragmentationContainer.atoms()) {
                                        if ((int) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY) == tmpAtomIndex) {
                                            tmpNewBond.setAtom(tmpAtom, 1);
                                            break;
                                        }
                                    }
                                    tmpRingFragmentationContainer.addAtom(tmpPseudoAtom);
                                    tmpRingFragmentationContainer.addBond(tmpNewBond);
                                } else if (!( boolean) tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY)){
                                    int tmpAtomIndex = tmpEndAtom.getProperty(INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                                    IBond tmpNewBond = new Bond();
                                    tmpNewBond.setOrder(IBond.Order.SINGLE);
                                    tmpNewBond.setAtom(tmpPseudoAtom, 0);
                                    for (IAtom tmpAtom: tmpRingFragmentationContainer.atoms()) {
                                        if ((int) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY) == tmpAtomIndex) {
                                            tmpNewBond.setAtom(tmpAtom, 1);
                                            break;
                                        }
                                    }
                                    tmpRingFragmentationContainer.addAtom(tmpPseudoAtom);
                                    tmpRingFragmentationContainer.addBond(tmpNewBond);
                                }
                            }
                            if (!(tmpIsBeginRing || tmpIsEndRing)) {
                                tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpRingFragmentationContainer));
                            }
                        }
                    }
                    //separateTertQuatCarbonFromRingSetting == false
                    else {
                        boolean tmpIsBondConnectedTertQuatRing = tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY);
                        boolean tmpIsBeginConnectedTertQuatRing = tmpBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY);
                        boolean tmpIsEndConnectedTertQuatRing = tmpBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY);
                        //skips over bonds between isolated rings / rings and linear chains
                        if ((tmpIsBeginRing && tmpIsEndRing) || (tmpIsBeginRing || tmpIsEndRing) && !tmpIsBondConnectedTertQuatRing) {
                            continue;
                        }
                        //adds bond connecting tertiary/quaternary carbons and rings
                        else if (tmpIsBondConnectedTertQuatRing) {
                            tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpRingFragmentationContainer));
                            //adds bond where begin or end are tertiary/quaternary and part of a ring/tertiary/quaternary connecting bond
                        } else if ((tmpIsBeginConnectedTertQuatRing || tmpIsEndConnectedTertQuatRing) && (tmpIsBeginTertiary || tmpIsEndTertiary || tmpIsBeginQuaternary || tmpIsEndQuaternary)) {
                            if (this.isolateTertQuatCarbonSetting.get()) {
                                if ((tmpIsBeginConjPi || tmpIsEndConjPi)) {
                                    continue;
                                } else {
                                    tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpRingFragmentationContainer));
                                }
                            }
                            else {
                                tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpRingFragmentationContainer));
                            }
                            //adds bond where one end is tertiary/quaternary and the other part of a conj. pi system
                        } else if ((tmpIsBeginTertiary || tmpIsEndTertiary || tmpIsBeginQuaternary || tmpIsEndQuaternary) && (tmpIsBeginConjPi || tmpIsEndConjPi)) {
                            if (!this.isolateTertQuatCarbonSetting.get()) {
                                tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpRingFragmentationContainer));
                            }
                        }
                        //adds residual bonds depending on conditions, first: if "isolate tert/quat" setting false; second: see comment below
                        else {
                            if (!this.isolateTertQuatCarbonSetting.get()) {
                                tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpRingFragmentationContainer));
                            } else {
                                //adds bond for neighbor atoms within a tertiary/quaternary system connected to a ring,
                                //  ensuring correct setting hierarchy for NOT isolating tertiary/quaternary carbons when
                                //  part of connected systems while separation setting is set to "false"
                                for (IAtom tmpBondAtom : tmpBond.atoms()) {
                                    for (IAtom tmpNeighborAtom : tmpBondAtom.neighbors()) {
                                        if ((boolean) tmpNeighborAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY)
                                                && (boolean) tmpNeighborAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY)) {
                                            tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpRingFragmentationContainer));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                //adds bond when tert/quat system connected to conj. pi system AND "isolate tert/quat" setting false
                else if ((tmpIsBeginConjPi || tmpIsEndConjPi) && (tmpIsBeginTertiary || tmpIsEndTertiary || tmpIsBeginQuaternary || tmpIsEndQuaternary)) {
                    if (!this.isolateTertQuatCarbonSetting.get()) {
                        tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpRingFragmentationContainer));
                    }
                }
                //residual bonds
                else {
                    if (this.isolateTertQuatCarbonSetting.get()) {
                        //stops bond extraction for bonds between double/triple bond atoms and neighbor atoms
                        if (tmpIsBeginDouble || tmpIsEndDouble || tmpIsBeginTriple || tmpIsEndTriple) {
                            continue;
                        } else {
                            tmpChainFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpChainFragmentationContainer));
                        }
                    }
                    //extracts bonds where atoms DO NOT have the following markers active: (ring AND conjugated pi) AND (double OR triple)
                    //as well as (tertiary OR quaternary OR neighbor)
                    else if (!(tmpIsBeginRing && tmpIsEndRing && tmpIsBeginConjPi && tmpIsEndConjPi) && !(tmpIsBeginDouble || tmpIsEndDouble || tmpIsBeginTriple || tmpIsEndTriple)) {
                        if (!(tmpIsBeginTertiary || tmpIsEndTertiary || tmpIsBeginQuaternary || tmpIsEndQuaternary || tmpIsBeginNeighbor || tmpIsEndNeighbor)) {
                            tmpChainFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpChainFragmentationContainer));
                        }
                    }
                }
            }
            //extracts ring or conjugated bonds
            else {
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
                    tmpIllegalMaxChainLength.toString(), tmpAtomArray[0].getContainer().getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY),
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
     * Method to create a deeper copy of a given atom, meant to replace the default addAtom() method of IAtomContainer
     * in which only the reference is added to the atomcontainer instead of a new atom.
     * <p>
     * Important notice!
     * During deep copying of an atom, its index of/in the associated atomcontainer is lost.
     * Therefor the internal ASF.Atom_Index property is used as means of indices throughout algorithm logic.
     * Also make sure to add any newly integrated internal properties in this method's properties-copy routine.
     * Otherwise, they will be lost during deep-copying!
     * </p>
     * @param anAtomToCopy the atom to create a deep copy of
     * @return the deep copy of the given atom
     */
    protected IAtom deepCopyAtom(IAtom anAtomToCopy) {
        IAtom tmpNewAtom = this.chemObjectBuilderInstance.newAtom();
        tmpNewAtom.setAtomicNumber(anAtomToCopy.getAtomicNumber());
        tmpNewAtom.setImplicitHydrogenCount(anAtomToCopy.getImplicitHydrogenCount());
        tmpNewAtom.setCharge(anAtomToCopy.getCharge());
        //IMPORTANT! Make sure to add new internal properties below!
        //<editor-fold desc="Property Deep Copy">
        //to ensure a true deep copy the boolean and integer property values are separated
        int tmpAtomIndexCopy = anAtomToCopy.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
        //boolean values are hardcoded into an array as copies of the original values
        boolean[] tmpBooleanPropertiesArray = new boolean[8];
        tmpBooleanPropertiesArray[0] = anAtomToCopy.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY);
        tmpBooleanPropertiesArray[1] = anAtomToCopy.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY);
        tmpBooleanPropertiesArray[2] = anAtomToCopy.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY);
        tmpBooleanPropertiesArray[3] = anAtomToCopy.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY);
        tmpBooleanPropertiesArray[4] = anAtomToCopy.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY);
        tmpBooleanPropertiesArray[5] = anAtomToCopy.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY);
        tmpBooleanPropertiesArray[6] = anAtomToCopy.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY);
        tmpBooleanPropertiesArray[7] = anAtomToCopy.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY);
        //setting the 'old' values as 'new' values in copy
        //atom index integer
        tmpNewAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY, tmpAtomIndexCopy);
        //boolean marker values
        tmpNewAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY, tmpBooleanPropertiesArray[0]);
        tmpNewAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, tmpBooleanPropertiesArray[1]);
        tmpNewAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY, tmpBooleanPropertiesArray[2]);
        tmpNewAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY, tmpBooleanPropertiesArray[3]);
        tmpNewAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY, tmpBooleanPropertiesArray[4]);
        tmpNewAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY, tmpBooleanPropertiesArray[5]);
        tmpNewAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, tmpBooleanPropertiesArray[6]);
        tmpNewAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY, tmpBooleanPropertiesArray[7]);
        //</editor-fold>
        return tmpNewAtom;
    }
    /**
     * Method to create a deep copy of a given bond, meant to replace the default addBond() method of IAtomContainer
     *
     * <p>
     *     Please make sure to add newly integrated internal properties into the method's properties-copy routine.
     *     Otherwise, they will be lost during deep-copying!
     * </p>
     *
     * @param aBondToCopy the bond to create a deep copy of
     * @param aBondIncludingAtomContainer the atomcontainer in which the bond's atoms are placed in
     * @throws IllegalArgumentException if given parameter is not valid, i.e. the given bond's atoms have no assigned index
     * @return the deep copy of the given bond
     */
    protected IBond deepCopyBond(IBond aBondToCopy, IAtomContainer aBondIncludingAtomContainer) throws IllegalArgumentException{
        IBond tmpNewBond = this.chemObjectBuilderInstance.newBond();
        tmpNewBond.setOrder(aBondToCopy.getOrder());
        //IMPORTANT! Make sure to add new internal properties below!
        //<editor-fold desc="Property Deep Copy">
        //to ensure a true deep copy the boolean and integer property values are separated
        int tmpBondIndexCopy = aBondToCopy.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY);
        //boolean values are hardcoded into an array as copies of the original values
        boolean[] tmpBooleanPropertiesArray = new boolean[6];
        tmpBooleanPropertiesArray[0] = aBondToCopy.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY);
        tmpBooleanPropertiesArray[1] = aBondToCopy.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY);
        tmpBooleanPropertiesArray[2] = aBondToCopy.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY);
        tmpBooleanPropertiesArray[3] = aBondToCopy.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY);
        tmpBooleanPropertiesArray[4] = aBondToCopy.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY);
        tmpBooleanPropertiesArray[5] = aBondToCopy.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY);
        //setting the 'old' values as 'new' values in copy
        //bond index integer
        tmpNewBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY, tmpBondIndexCopy);
        //boolean marker values
        tmpNewBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY, tmpBooleanPropertiesArray[0]);
        tmpNewBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, tmpBooleanPropertiesArray[1]);
        tmpNewBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY, tmpBooleanPropertiesArray[2]);
        tmpNewBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY, tmpBooleanPropertiesArray[3]);
        tmpNewBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, tmpBooleanPropertiesArray[4]);
        tmpNewBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY, tmpBooleanPropertiesArray[5]);
        //</editor-fold>
        IAtom tmpBeginAtom = aBondToCopy.getBegin();
        int tmpBeginAtomIndex = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
        if ((tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY) != null)) {
            int tmpEndAtomIndex = aBondToCopy.getEnd().getProperty(INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
            for (IAtom tmpAtom : aBondIncludingAtomContainer.atoms()) {
                if (tmpAtom.getProperty(INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY) == null) {
                    continue;
                }
                if ((int) tmpAtom.getProperty(INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY) == tmpBeginAtomIndex) {
                    tmpNewBond.setAtom(tmpAtom, 0);
                } else if ((int) tmpAtom.getProperty(INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY) == tmpEndAtomIndex) {
                    tmpNewBond.setAtom(tmpAtom, 1);
                }
            }
        }
        else {
            throw new IllegalArgumentException("Deep copy of bond not possible. No 'ASF.ATOM_INDEX' value was found.");
        }
        return tmpNewBond;
    }

    /**
     * Method to detect conjugated pi bond systems.
     *
     * @param aMolecularArraysInstance Given arrays with atoms and bonds of a molecule to detect conjugated pi bond systems in
     */
    protected void detectConjugatedPiSystems(MolecularArrays aMolecularArraysInstance) {
        Objects.requireNonNull(aMolecularArraysInstance);
        IAtom[] tmpOriginAtomArray = aMolecularArraysInstance.getAtomArray();
        IBond[] tmpOriginBondArray = aMolecularArraysInstance.getBondArray();
        //create and fill list with integer lists, each integer list representing an atom
        List<List<Integer>> tmpIncidenceList = new ArrayList<>(tmpOriginBondArray.length);
        for (int i = 0; i < tmpOriginAtomArray.length; i++) {
            tmpIncidenceList.add(new ArrayList<>());
        }
        //put bond index into integer lists, creating an incidence list of all atoms
        for (IBond tmpOriginBond: tmpOriginBondArray) {
            boolean tmpIsDouble = false;
            if (tmpOriginBond.getOrder() == IBond.Order.DOUBLE) { tmpIsDouble = true;}
            int tmpBondIndex = tmpOriginBond.getIndex();
            IAtom tmpBeginAtom = tmpOriginBond.getBegin();
            int tmpBeginIndex = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
            IAtom tmpEndAtom = tmpOriginBond.getEnd();
            int tmpEndIndex = tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
            //Edge tmpEdge = new Edge(tmpBeginIndex, tmpEndIndex, tmpIsDouble);
            tmpIncidenceList.get(tmpBeginIndex).add(tmpBondIndex);
            tmpIncidenceList.get(tmpEndIndex).add(tmpBondIndex);
        }
        System.out.println(tmpIncidenceList);
        //loop over bonds, if bond A connects atoms a & b, add A to incident[a] & incident[b]
        for (IAtom tmpOriginAtom: tmpOriginAtomArray) {

        }
    }
    //</editor-fold>
}
