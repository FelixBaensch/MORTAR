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
import org.openscience.cdk.PseudoAtom;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IElement;
import org.openscience.cdk.interfaces.IPseudoAtom;
import org.openscience.cdk.interfaces.IRingSet;
import org.openscience.cdk.interfaces.IStereoElement;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.stereo.Projection;
import org.openscience.cdk.stereo.StereoElementFactory;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An algorithm for detection and fragmentation of alkyl
 * structures in MORTAR using the CDK.
 *
 * <p>
 *     general:
 *     ToDo: library created, only needs small additions (include in Tutorial)
 * </p>
 *
 * @author Maximilian Rottmann (maximilian.rottmann@studmail.w-hs.de)
 * @version 1.0.0.0
 */
public class AlkylStructureFragmenter implements IMoleculeFragmenter{
    //
    //<editor-fold desc="Inner Class 'MolecularArrays'">
    /**
     * Inner class for internally transferring atom and bond arrays of the input molecule.
     * On initialization, the constructor receives a molecule from which the atoms and bonds are to be placed in arrays.
     * Each array 'filling' routine checks and removes any null elements, should there be any.
     * In the same step, the atom and bond properties used by this class for marking are set to an initialization
     * value (e.g. false), except their index which is set to their respective increasing count.
     */
    protected static class MolecularArrays {
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
            this.atomArray = tmpAtomArrayList.toArray(new IAtom[0]);
            this.bondArray = tmpBondArrayList.toArray(new IBond[0]);
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
    }
    //</editor-fold
    //
    //<editor-fold desc="Public Static Final Class Constants">
    /**
     * Name of the fragmenter.
     */
    public static final String ALGORITHM_NAME = "Alkyl Fragmenter";
    /**
     * Default boolean value for keeping non-fragmentable molecules in the fragmenter pipeline.
     */
    public static final boolean KEEP_NON_FRAGMENTABLE_MOLECULES_SETTING_DEFAULT = false;
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
    //
    //<editor-fold desc="Private Class Variables">
    /**
     * A property wrapping a fragment saturation setting in the form of an
     * {@link IMoleculeFragmenter.FragmentSaturationOption} enum constant.
     */
    private final SimpleIDisplayEnumConstantProperty fragmentSaturationSetting;
    /**
     * A property wrapping a boolean value defining whether non-fragmentable molecules should be kept in the
     * fragmenter pipeline.
     */
    private final SimpleBooleanProperty keepNonFragmentableMoleculesSetting;
    /**
     * A property wrapping a boolean value determining whether side chains should be fragmented.
     */
    private final SimpleBooleanProperty fragmentSideChainsSetting;
    /**
     * A property wrapping an integer for maximum side chain length.
     */
    private final SimpleIntegerProperty maxChainLengthSetting;
    /**
     * A property wrapping a boolean value determining whether non-cyclic tertiary and quaternary carbons should be isolated when fragmented.
     */
    private final SimpleBooleanProperty isolateTertQuatCarbonSetting;
    /**
     * A property wrapping a boolean value defining whether tertiary and quaternary carbon atoms should be separated
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
     * CDK IChemObjectBuilder instance used in atom container instancing.
     */
    private final IChemObjectBuilder chemObjectBuilderInstance;
    //</editor-fold>
    //
    //<editor-fold desc="Constructor">
    /**
     * Constructor, all settings are initialised with their respective default values.
     */
    public AlkylStructureFragmenter(){
        //check current logger environment for debug
        //for Debug: set ROOT_LOGGER in LogUtil to Level.FINEST
        /*
        if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINEST.intValue()) {
            System.out.println("finest Environment");
        } else {
            System.out.println(AlkylStructureFragmenter.LOGGER.getParent().getLevel());
        }
         */
        int tmpSettingsNumber = 6;
        int tmpInitialCapacitySettingsNameTooltipHashMap = CollectionUtil.calculateInitialHashCollectionCapacity(
                tmpSettingsNumber,
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
            public void set(int newValue) throws IllegalArgumentException {
                //call to super.set() for parameter checks
                //only allows values greater than 0 to be set
                if (newValue > 0) {
                    super.set(newValue);
                } else {
                    String tmpMessage = "Maximum length of alkyl chain fragments setting only accepts positive, non-zero values.";
                    AlkylStructureFragmenter.LOGGER.log(Level.WARNING, tmpMessage);
                    GuiUtil.guiExceptionAlert(Message.get("Fragmenter.IllegalSettingValue.Title"),
                            Message.get("Fragmenter.IllegalSettingValue.Header"),
                            tmpMessage,
                            null);
                    //throws an exception to properly reset the binding
                    throw new IllegalArgumentException(tmpMessage);
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
        this.settings = new ArrayList<>(tmpSettingsNumber);
        this.settings.add(this.fragmentSaturationSetting);
        this.settings.add(this.keepNonFragmentableMoleculesSetting);
        this.settings.add(this.fragmentSideChainsSetting);
        this.settings.add(this.maxChainLengthSetting);
        this.settings.add(this.isolateTertQuatCarbonSetting);
        this.settings.add(this.separateTertQuatCarbonFromRingSetting);
        //set chemObjectBuilderInstance
        this.chemObjectBuilderInstance = SilentChemObjectBuilder.getInstance();
    }
    //</editor-fold>
    //
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
    @Override
    public SimpleIDisplayEnumConstantProperty fragmentSaturationSettingProperty() {
        return this.fragmentSaturationSetting;
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
    //</editor-fold>
    //
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
     * @throws IllegalArgumentException if given int is smaller than 1
     */
    public void setMaxChainLengthSetting(int aValue) throws IllegalArgumentException{
        if (aValue < 1) {
            throw new IllegalArgumentException("Given chain length cannot be smaller than 1.");
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
    //
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
     *     Checks the given IAtomContainer aMolecule for hetero atoms and returns true if
     *     non-conforming atoms are found. Otherwise (it contains only carbons and hydrogen atoms), false is returned
     *     and the molecule can be fragmented.
     *     In order to enable the user to let non-fragmentable molecules be retained in the pipeline, the filter can be
     *     switched off via setting keepNonFragmentableMoleculesSetting.
     *     Also returns true if the molecule is null or empty, or if it contains a null atom.
     * </p>
     *
     * @param aMolecule the molecule to check
     * @return true or false, depending on atom check
     */
    @Override
    public boolean shouldBeFiltered(IAtomContainer aMolecule) {
        //filter if molecule is null or empty
        if (Objects.isNull(aMolecule) || aMolecule.isEmpty()) {
            return true;
        }
        boolean tmpContainsCarbons = false;
        boolean tmpContainsHeteroAtoms = false;
        boolean tmpContainsPseudoAtoms = false;
        for (IAtom tmpAtom : aMolecule.atoms()) {
            if (tmpAtom == null) {
                //filter if an atom in the mol is null
                return true;
            }
            if (tmpAtom.getAtomicNumber() == IElement.C) {
                tmpContainsCarbons = true;
            } else if (this.isPseudoAtom(tmpAtom)) {
                tmpContainsPseudoAtoms = true;
            } else if (this.isHeteroAtom(tmpAtom)) {
                tmpContainsHeteroAtoms = true;
            }
        }
        if (tmpContainsCarbons && !tmpContainsHeteroAtoms && !tmpContainsPseudoAtoms) {
            //contains carbons and no hetero atoms, internal filter property false -> shouldn't be filtered out of fragmentation
            aMolecule.setProperty(AlkylStructureFragmenter.ASF_FILTER_MARKER, false);
            return false;
        } else {
            //contains hetero atoms or no carbons
            aMolecule.setProperty(AlkylStructureFragmenter.ASF_FILTER_MARKER, true);
            //whether to return true or false depends on the setting (inverted for correct behavior)
            return !this.keepNonFragmentableMoleculesSetting.get();
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
        if ((boolean) aMolecule.getProperty(AlkylStructureFragmenter.ASF_FILTER_MARKER)) {
            return false;
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
    //
    //<editor-fold desc="Fragmentation">
    /**
     * {@inheritDoc}
     * <p>
     * Algorithmic approach to alkyl fragmentation:
     * <p>
     *     As a first measure, the given molecule is checked for an internal property, checking if it has been
     *     passed through an upstream filter. Afterward, the molecule is cloned, in order to prevent changes to the
     *     original molecule, and atom types are perceived and configured for downstream fragmentation steps.
     * </p>
     * <p>
     *     Next, internal class ({@link MolecularArrays}) arrays used for internal data transfer are filled. The atoms and
     *     bonds of the cloned molecule are placed in respective IAtom and IBond arrays, which are then placed in a
     *     MolecularArrays instance. In the 'filling' step, atoms and bonds are prepared with internal properties used
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
     *     chemical formula is checked, and a warning logged if it changes.
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
     * @return List of IAtomContainers containing the fragments (empty if no fragments are extracted)
     * @throws NullPointerException if aMolecule is null
     * @throws IllegalArgumentException if the given molecule cannot be fragmented
     * @throws CloneNotSupportedException if cloning the given molecule is unsuccessful
     */
    @Override
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule)
            throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        //skip fragmentation if molecule is not fragmentable and just return it as one fragment to keep it in pipeline
        if ((boolean) aMolecule.getProperty(AlkylStructureFragmenter.ASF_FILTER_MARKER) && this.keepNonFragmentableMoleculesSetting.get()) {
            List<IAtomContainer> tmpNonFragACList = new ArrayList<>(1);
            tmpNonFragACList.add(aMolecule);
            return tmpNonFragACList;
        }
        //<editor-fold desc="Molecule Cloning and Chemical Formula Check" defaultstate="collapsed">
        IAtomContainer tmpClone = aMolecule.clone();
        try {
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpClone);
        } catch (CDKException aCDKException) {
            AlkylStructureFragmenter.LOGGER.log(Level.WARNING,
                    String.format(AlkylStructureFragmenter.LOGGER_EXCEPTION_STRING_FORMAT, aCDKException, tmpClone.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY),
                            "Atom types could not be perceived or atoms could not be configured."));
        }
        int tmpPreFragmentationAtomCount = this.countAtoms(tmpClone);
        if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINEST.intValue()) {
            AlkylStructureFragmenter.LOGGER.log(Level.FINEST, "PreFragAtomCount: {0}", tmpPreFragmentationAtomCount);
        }
        //</editor-fold>
        //internal arrays are filled with atoms and bonds in wrapping class MolecularArrays instance
        MolecularArrays tmpMolecularArrays = new MolecularArrays(tmpClone);
        //preserve original molecule stereo chemistry
        Map<IChemObject, IStereoElement<IChemObject, IChemObject>> tmpStereoChemOriginMoleculeMap = this.createStereoChemMap(tmpClone);
        //substructures are detected and marked respectively
        this.markSubstructures(tmpMolecularArrays, tmpClone);
        //
        //<editor-fold desc="Fragment Extraction and Saturation" defaultstate="collapsed">
        try {
            int tmpPostFragmentationAtomCount = 0;
            if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINER.intValue()) {
                AlkylStructureFragmenter.LOGGER.log(Level.FINER, "Pre-Fragment-Extraction");
            }
            IAtomContainerSet tmpFragmentSet = this.getFragmentationResults(tmpMolecularArrays);
            for (IAtomContainer tmpAtomContainer: tmpFragmentSet.atomContainers()) {
                tmpPostFragmentationAtomCount += this.countAtoms(tmpAtomContainer);
            }
            if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINER.intValue()) {
                AlkylStructureFragmenter.LOGGER.log(Level.FINER, "Post-Fragment-Extraction");
            }
            if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINEST.intValue()) {
                AlkylStructureFragmenter.LOGGER.log(Level.FINEST, "PostFragAtomCount: {0}", tmpPostFragmentationAtomCount);
            }
            if (tmpPostFragmentationAtomCount != tmpPreFragmentationAtomCount) {
                AlkylStructureFragmenter.LOGGER.log(Level.WARNING, "{0}", String.format(LOGGER_WARNING_STRING_FORMAT,
                        "Chemical Formula Check", tmpClone.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY),
                        "Chemical formula was not constant!"));
            }
            if (this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION)) {
                tmpFragmentSet = this.saturateWithImplicitHydrogen(tmpFragmentSet);
            }
            ArrayList<IAtomContainer> tmpFragmentList = new ArrayList<>(tmpFragmentSet.getAtomContainerCount());
            for (IAtomContainer tmpAtomContainer: tmpFragmentSet.atomContainers()) {
                boolean stereochemsetting = true;
                if (stereochemsetting) {
                    //some null pointer exception: "field 'x' cannot be read cause field 'prevXy' is null" ????
                    try {
                        tmpFragmentList.add(this.remapStereoChem(tmpAtomContainer, tmpStereoChemOriginMoleculeMap));
                    } catch (Exception aStereoChemException) {
                        throw new RuntimeException(aStereoChemException);
                    }
                    //debug
//                    Iterable<IStereoElement> tmpStereoChemIterable = tmpFragmentList.getFirst().stereoElements();
//                    int tmpStereoChemIterableSize = 0;
//                    if (tmpStereoChemIterable instanceof Collection) {
//                        tmpStereoChemIterableSize = ((Collection<?>) tmpStereoChemIterable).size();
//                    }
//                    System.out.println("result stereo size: " + tmpStereoChemIterableSize);
                } else {
                    tmpFragmentList.add(tmpAtomContainer);
                }
            }
            return tmpFragmentList;
        } catch (Exception anException) {
            throw new IllegalArgumentException(String.format(AlkylStructureFragmenter.LOGGER_EXCEPTION_STRING_FORMAT, anException,
                    tmpClone.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY), "Fragmentation failed!"));
        }
        //</editor-fold>
    }
    //</editor-fold>
    //</editor-fold>
    //
    //<editor-fold desc="Protected Methods" defaultstate="collapsed">
    //
    //<editor-fold desc="Marking Methods">

    /**
     * Protected method wrapping the marking steps of the alkyl structures detection step.
     * Molecular arrays with atoms and bonds of the molecule to fragment are needed, as well as the original molecule.
     * The original molecule is needed as a mapping reference in the ring marking step.
     *
     * @param aMolecularArraysInstance with atoms and bonds arrays of the molecule to fragment
     * @param anAtomContainer original molecule needed for mapping
     */
    protected void markSubstructures(MolecularArrays aMolecularArraysInstance, IAtomContainer anAtomContainer) {
        try {
            this.markRings(aMolecularArraysInstance, anAtomContainer);
        } catch (Exception aFailedRingMarkException) {
            //closest non-general exception for a faulty ring detection
            throw new UnsupportedOperationException (
                    String.format("Ring marking failed! Fragments may not be correct! Occurred at molecule: %s",
                            anAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY)), aFailedRingMarkException);
        }
        this.markTertQuatAndNeighbors(aMolecularArraysInstance);
        try {
            this.markConjugatedPiSystems(aMolecularArraysInstance);
        } catch (Exception aFailedConjPiMarkException) {
            //closest non-general exception for a faulty conj. pi system detection
            throw new UnsupportedOperationException (
                    String.format("Conjugated Pi System marking failed! Fragments may not be correct! Occurred at molecule: %s",
                            anAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY)), aFailedConjPiMarkException);
        }
        this.markConnectedTertQuatRing(aMolecularArraysInstance);
        this.markMultiBonds(aMolecularArraysInstance);
    }
    /**
     * Protected method for detecting and marking tertiary or quaternary carbon atoms and their surrounding neighbor atoms and bonds.
     *
     * @param aMolecularArraysInstance MolecularArrays instance for data transfer between methods
     */
    protected void markTertQuatAndNeighbors(MolecularArrays aMolecularArraysInstance) {
        IAtom[] tmpAtomArray = aMolecularArraysInstance.getAtomArray();
        IBond[] tmpBondArray = aMolecularArraysInstance.getBondArray();
        //set general atom and specific bond properties
        for (IAtom tmpAtom : tmpAtomArray) {
            if (tmpAtom != null && (tmpAtom.getBondCount() == 3 || tmpAtom.getBondCount() == 4)
                    && tmpAtom.getMaxBondOrder() == IBond.Order.SINGLE
                    && !((boolean) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY))) {
                if (tmpAtom.getBondCount() == 3) {
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY, true);
                }
                if (tmpAtom.getBondCount() == 4) {
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY, true);
                }
                for (IBond tmpBond : tmpAtom.bonds()) {
                    tmpBondArray[(int) tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY)]
                            .setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                }
                for (IAtom tmpNeighborAtom : tmpAtom.neighbors()) {
                    tmpAtomArray[(int) tmpNeighborAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY)]
                            .setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                }
            }
        }
    }
    /**
     * Protected method to mark all atoms and bonds of any rings in the given atom container.
     *
     * @param aMolecularArraysInstance MolecularArrays instance for data transfer between methods
     * @param anAtomContainer IAtomContainer to mark atoms and bonds in
     * @throws IllegalArgumentException if anything goes wrong
     */
    protected void markRings(MolecularArrays aMolecularArraysInstance, IAtomContainer anAtomContainer) throws IllegalArgumentException {
        IAtom[] tmpAtomArray = aMolecularArraysInstance.getAtomArray();
        IBond[] tmpBondArray = aMolecularArraysInstance.getBondArray();
        CycleFinder tmpMCBCycleFinder = Cycles.mcb();
        IRingSet tmpMCBCyclesSet;
        try {
            Cycles tmpMCBCycles = tmpMCBCycleFinder.find(anAtomContainer);
            tmpMCBCyclesSet = tmpMCBCycles.toRingSet();
            for (IAtomContainer tmpContainer: tmpMCBCyclesSet.atomContainers()) {
                for (IAtom tmpRingAtom: tmpContainer.atoms()) {
                    int tmpAtomIndex = tmpRingAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                    IAtom tmpAtom = tmpAtomArray[tmpAtomIndex];
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY, true);
                }
                for (IBond tmpRingBond: tmpContainer.bonds()) {
                    int tmpBondIndex = tmpRingBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY);
                    IBond tmpBond = tmpBondArray[tmpBondIndex];
                    tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY, true);
                }
            }
        } catch (Exception anException) {
            throw new IllegalArgumentException(String.format(AlkylStructureFragmenter.LOGGER_EXCEPTION_STRING_FORMAT,
                    anException, anAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY),
                    "CycleFinder failed."));
        }
    }
    /**
     * Protected method to mark all atoms and bonds of any conjugated pi systems in the given arrays.
     * <p>
     * It iterates over all bonds of the bond array given by the MolecularArrays instance.
     * Within these bonds it searches for patterns of bond orders, either 2-1-2 or 3-1-3, by iterating over the array
     * bond's neighbor bonds and their neighbor bonds (neighbor-neighbor bonds of the original array bond).
     * If it detects an alternating pattern in these nested iterations, all current bonds are marked as conjugated,
     * as well as all corresponding bond atoms.
     * Before each array bond is used in the iteration, it checks and skips over bonds already marked as conjugated.
     * Additionally, it catches behavior where the order one bond inbetween order two bonds is selected for check.
     * Here, it checks begin and end atom of the bond whether both their maximum order is double/two. If true, the bond
     * is marked as conjugated.
     * </p>
     *
     * @param aMolecularArraysInstance MolecularArrays instance for data transfer between methods
     */
    protected void markConjugatedPiSystems(MolecularArrays aMolecularArraysInstance) throws IllegalArgumentException{
        IAtom[] tmpAtomArray = aMolecularArraysInstance.getAtomArray();
        IBond[] tmpBondArray = aMolecularArraysInstance.getBondArray();
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
            } else {
                if (tmpArrayBond.getBegin().getMaxBondOrder() == IBond.Order.DOUBLE && tmpArrayBond.getEnd().getMaxBondOrder() == IBond.Order.DOUBLE) {
                    tmpArrayBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, true);
                }
                continue;
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
                            } else {
                                continue;
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
    }
    /**
     * Protected method to mark atoms and bonds with order of two or three.
     *
     * @param aMolecularArraysInstance MolecularArrays instance for data transfer between methods
     */
    protected void markMultiBonds(MolecularArrays aMolecularArraysInstance) {
        IAtom[] tmpAtomArray = aMolecularArraysInstance.getAtomArray();
        IBond[] tmpBondArray = aMolecularArraysInstance.getBondArray();
        for (IBond tmpArrayBond: tmpBondArray) {
            if (tmpArrayBond.getOrder().numeric() == 2) {
                tmpArrayBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY, true);
                int tmpBeginIndex = tmpArrayBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                int tmpEndIndex = tmpArrayBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                tmpAtomArray[tmpBeginIndex].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY, true);
                tmpAtomArray[tmpEndIndex].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY, true);
            } else if (tmpArrayBond.getOrder().numeric() == 3) {
                tmpArrayBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY, true);
                int tmpBeginIndex = tmpArrayBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                int tmpEndIndex = tmpArrayBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                tmpAtomArray[tmpBeginIndex].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY, true);
                tmpAtomArray[tmpEndIndex].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY, true);
            }
        }
    }

    /**
     * Method used to mark atoms and bonds of tertiary and quaternary carbons directly connected to ring systems
     * (i.e. tertiary_carbon - ring_carbon).
     *
     * @param aMolecularArraysInstance used for internal data transfer between methods
     */
    protected void markConnectedTertQuatRing(MolecularArrays aMolecularArraysInstance) {
        IAtom[] tmpAtomArray = aMolecularArraysInstance.getAtomArray();
        IBond[] tmpBondArray = aMolecularArraysInstance.getBondArray();
        for (IBond tmpArrayBond : tmpBondArray) {
            IAtom tmpBeginAtom = tmpArrayBond.getBegin();
            IAtom tmpEndAtom = tmpArrayBond.getEnd();
            int tmpBeginIndex = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
            int tmpEndIndex = tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
            boolean tmpIsBeginTertQuat = false;
            boolean tmpIsBeginRing = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY);
            boolean tmpIsEndTertQuat = false;
            boolean tmpIsEndRing = tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY);
            if ((boolean) tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY)
                    || (boolean) tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY)) {
                tmpIsBeginTertQuat = true;
            }
            if ((boolean) tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY)
                    || (boolean) tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY)) {
                tmpIsEndTertQuat = true;
            }
            if ((tmpIsBeginTertQuat && tmpIsEndRing) || (tmpIsBeginRing && tmpIsEndTertQuat)) {
                tmpArrayBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY, true);
                tmpAtomArray[tmpBeginIndex].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY, true);
                tmpAtomArray[tmpEndIndex].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY, true);
            }
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Extraction Method">
    /**
     * Protected method wrapping the extraction logic for molecular atoms.
     *
     * @param anAtomsArray MolecularArrays atom array with atoms of molecule to fragment
     * @return atom container set with atom containers ONLY containing the molecule's atoms
     */
    //Reduction in complexity could be achieved by using/referencing only one atom container, though this would entail
    // (quite possibly) large changes to the algorithm logic. Therefor, it can be implemented in the future if necessary,
    // at this point in time it is deemed unnecessary as the main focus is on functionality of the algorithm.
    protected IAtomContainerSet extractAtoms(IAtom[] anAtomsArray) {
        Objects.requireNonNull(anAtomsArray);
        IAtomContainerSet tmpExtractedAtomACSet = new AtomContainerSet();
        //break down to use only one AtomContainer
        IAtomContainer tmpRingFragmentationContainer = this.chemObjectBuilderInstance.newAtomContainer();
        IAtomContainer tmpChainFragmentationContainer = this.chemObjectBuilderInstance.newAtomContainer();
        IAtomContainer tmpIsolatedMultiBondsContainer = this.chemObjectBuilderInstance.newAtomContainer();
        IAtomContainer tmpTertQuatCarbonContainer = this.chemObjectBuilderInstance.newAtomContainer();
        //
        String tmpExtractionLoggerSpecifierString = "Extraction.AtomIteration at Index: %d, Step: %s";
        for (IAtom tmpArrayAtom : anAtomsArray) {
            try {
                if (!this.isPseudoAtom(tmpArrayAtom)) {
                    //checks atom if not part of ring or conjugated pi system
                    if (!((boolean) tmpArrayAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY)
                            || (boolean) tmpArrayAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY))) {
                        //<editor-fold desc="Tertiary & Quaternary Extraction">
                        if ((boolean) tmpArrayAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY)
                                || (boolean) tmpArrayAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY)) {
                            if (this.isolateTertQuatCarbonSetting.get()) {
                                if (!this.separateTertQuatCarbonFromRingSetting.get() && (boolean) tmpArrayAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY)) {
                                        tmpRingFragmentationContainer.addAtom(this.deepCopyAtom(tmpArrayAtom));
                                        continue;
                                    }
                                tmpTertQuatCarbonContainer.add(this.extractTertQuatCarbons(tmpArrayAtom));
                                //tertiary/quaternary carbons are added to ensure correct interaction with other substructures,
                                // neighbor atoms added later
                            } else {
                                tmpRingFragmentationContainer.addAtom(this.deepCopyAtom(tmpArrayAtom));
                            }
                        }
                        //</editor-fold>
                        //checks for part of double bond
                        else if ((boolean) tmpArrayAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY)) {
                            //extracts allene atoms
                            if (tmpArrayAtom.getMaxBondOrder() == IBond.Order.DOUBLE && tmpArrayAtom.getBondOrderSum() == 4 && tmpArrayAtom.getBondCount() == 2) {
                                tmpIsolatedMultiBondsContainer.addAtom(this.deepCopyAtom(tmpArrayAtom));
                                continue;
                            }
                            //extracts non-cyclic double bonds possibly connected to a ring structure
                            for (IAtom tmpNeighborAtom : tmpArrayAtom.neighbors()) {
                                if (!(boolean) tmpNeighborAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY)) {
                                    continue;
                                }
                                if ((boolean) tmpNeighborAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY)) {
                                    tmpRingFragmentationContainer.addAtom(this.deepCopyAtom(tmpArrayAtom));
                                } else if (!(boolean) tmpNeighborAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY)) {
                                    tmpIsolatedMultiBondsContainer.addAtom(this.deepCopyAtom(tmpArrayAtom));
                                } else {
                                    tmpChainFragmentationContainer.addAtom(this.deepCopyAtom(tmpArrayAtom));
                                }
                            }
                            //checks for triple bond
                        } else if ((boolean) tmpArrayAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY)) {
                            if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINEST.intValue()) {
                                AlkylStructureFragmenter.LOGGER.log(Level.FINEST, () -> String.format(tmpExtractionLoggerSpecifierString,
                                        tmpArrayAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY),
                                        "Triple Bond"));
                            }
                            tmpIsolatedMultiBondsContainer.addAtom(this.deepCopyAtom(tmpArrayAtom));
                        } else if ((boolean) tmpArrayAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY)) {
                            //extract neighbor atoms of tertiary or quaternary carbon atoms
                            if (this.isolateTertQuatCarbonSetting.get()) {
                                if (tmpArrayAtom.getBondCount() == 1) {
                                    if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINEST.intValue()) {
                                        AlkylStructureFragmenter.LOGGER.log(Level.FINEST, () -> String.format(tmpExtractionLoggerSpecifierString,
                                                tmpArrayAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY),
                                                "Neighbor atoms for bond count 1"));
                                    }
                                    tmpRingFragmentationContainer.addAtom(this.deepCopyAtom(tmpArrayAtom));
                                } else {
                                    if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINEST.intValue()) {
                                        AlkylStructureFragmenter.LOGGER.log(Level.FINEST, () -> String.format(tmpExtractionLoggerSpecifierString,
                                                tmpArrayAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY),
                                                "Neighbor atoms chains"));
                                    }
                                    tmpChainFragmentationContainer.addAtom(this.deepCopyAtom(tmpArrayAtom));
                                }
                            } else {
                                if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINEST.intValue()) {
                                    AlkylStructureFragmenter.LOGGER.log(Level.FINEST, () -> String.format(tmpExtractionLoggerSpecifierString,
                                            tmpArrayAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY),
                                            "Neighbor atoms: no isolation"));
                                }
                                tmpRingFragmentationContainer.addAtom(this.deepCopyAtom(tmpArrayAtom));
                            }
                        }
                        //extract residue atoms as linear chain atoms
                        else {
                            if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINEST.intValue()) {
                                AlkylStructureFragmenter.LOGGER.log(Level.FINEST, () -> String.format(tmpExtractionLoggerSpecifierString,
                                        tmpArrayAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY),
                                        "Residue non-cyclic/-conj atoms"));
                            }
                            tmpChainFragmentationContainer.addAtom(this.deepCopyAtom(tmpArrayAtom));
                        }
                    } else {
                        //only ring and conj pi system atoms, sets bond property to determine connection between tert/quat atom and ring structure
                        if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINEST.intValue()) {
                            AlkylStructureFragmenter.LOGGER.log(Level.FINEST, () -> String.format(tmpExtractionLoggerSpecifierString,
                                    tmpArrayAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY),
                                    "ring/conj atoms"));
                        }
                        tmpRingFragmentationContainer.addAtom(this.deepCopyAtom(tmpArrayAtom));
                    }
                }
            } catch (IllegalArgumentException anIllegalArgumentException) {
                throw new IllegalArgumentException("Atom could not be extracted at atom with index: "
                        + tmpArrayAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY)
                        + "!" + "Cause: " + anIllegalArgumentException);
            }
        }
        //add resulting atom container to return set
        tmpExtractedAtomACSet.addAtomContainer(tmpRingFragmentationContainer);
        tmpExtractedAtomACSet.addAtomContainer(tmpChainFragmentationContainer);
        tmpExtractedAtomACSet.addAtomContainer(tmpIsolatedMultiBondsContainer);
        tmpExtractedAtomACSet.addAtomContainer(tmpTertQuatCarbonContainer);
        return tmpExtractedAtomACSet;
    }

    /**
     * Protected method wrapping the extraction logic for molecular bonds.
     *
     * @param aBondsArray MolecularArrays bond array with bonds of molecule to fragment
     * @param anExtractedAtomsContainingACSet with atom containers containing the molecule's atoms
     * @return atom container set with possibly disconnected atom containers containing the molecule's generated fragments
     */
    //Reduction in complexity could be achieved by using/referencing only one atom container, though this would entail
    // (quite possibly) large changes to the algorithm logic. Therefor, it can be implemented in the future if necessary,
    // at this point in time it is deemed unnecessary as the main focus is on functionality of the algorithm.
    protected IAtomContainerSet extractBonds(IBond[] aBondsArray, IAtomContainerSet anExtractedAtomsContainingACSet) {
        Objects.requireNonNull(aBondsArray);
        Objects.requireNonNull(anExtractedAtomsContainingACSet);
        //break down to using only param ACSet
        IAtomContainerSet tmpExtractedAtomAndBondACSet = new AtomContainerSet();
        IAtomContainer tmpRingFragmentationContainer = anExtractedAtomsContainingACSet.getAtomContainer(0);
        IAtomContainer tmpChainFragmentationContainer = anExtractedAtomsContainingACSet.getAtomContainer(1);
        IAtomContainer tmpIsolatedMultiBondsContainer = anExtractedAtomsContainingACSet.getAtomContainer(2);
        IAtomContainer tmpTertQuatCarbonContainer = anExtractedAtomsContainingACSet.getAtomContainer(3);
        //
        String tmpExtractionLoggerSpecifierString = "Extraction.BondIteration at Index: %d, Step: %s";
        for (IBond tmpArraysBond : aBondsArray) {
            if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINEST.intValue()) {
                AlkylStructureFragmenter.LOGGER.log(Level.FINEST, () -> String.format(tmpExtractionLoggerSpecifierString,
                         tmpArraysBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY),
                         "Extraction Start"));
            }
            try {
                IAtom tmpBeginAtom = tmpArraysBond.getBegin();
                IAtom tmpEndAtom = tmpArraysBond.getEnd();
                if (tmpBeginAtom == null || tmpEndAtom == null) {
                    continue;
                }
                if (this.isPseudoAtom(tmpBeginAtom) || this.isPseudoAtom(tmpEndAtom)) {
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
                //check bond for not ring and not conjugated
                if (!((boolean) tmpArraysBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY)
                        || (boolean) tmpArraysBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY))) {
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
                    //checks for double/triple bond mark
                    /*
                    This case checks if the double bond may be connected to a ring and if true, places it in the same
                    container as the ring as they are part of one substructure.
                     */
                    if ((boolean) tmpArraysBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY)
                            || (boolean) tmpArraysBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY)) {
                        if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINEST.intValue()) {
                            AlkylStructureFragmenter.LOGGER.log(Level.FINEST, () -> String.format(tmpExtractionLoggerSpecifierString,
                                    tmpArraysBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY),
                                    "Multi Bond"));
                        }
                        //triple bonds should not be extracted by the statement below, as no connection to rings is possible
                        if (tmpIsBeginRing || tmpIsEndRing) {
                            tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpArraysBond, tmpRingFragmentationContainer));
                        } else {
                            tmpIsolatedMultiBondsContainer.addBond(this.deepCopyBond(tmpArraysBond, tmpIsolatedMultiBondsContainer));
                        }
                    } else if ((boolean) tmpArraysBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY)) {
                        //checks for neighbor mark
                        // checks for setting to separate tertiary/quaternary carbons from ring structures
                        if (this.separateTertQuatCarbonFromRingSetting.get()) {
                            //checks for setting to isolate tertiary/quaternary carbons from their neighbor atoms
                            if (!this.isolateTertQuatCarbonSetting.get()) {
                                //checks if bond is connection between tert/quat carbon and ring structure
                                //adds pseudo atom to indicate bond to ring
                                if ((boolean) tmpArraysBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY)) {
                                    PseudoAtom tmpPseudoAtom = new PseudoAtom();
                                    tmpPseudoAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY, false);
                                    for (IAtom tmpAtom : tmpArraysBond.atoms()) {
                                        if (!(boolean) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY)) {
                                            int tmpAtomIndex = tmpAtom.getProperty(INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                                            IBond tmpNewBond = new Bond();
                                            tmpNewBond.setOrder(IBond.Order.SINGLE);
                                            tmpNewBond.setAtom(tmpPseudoAtom, 0);
                                            for (IAtom tmpRingFragContainerAtom : tmpRingFragmentationContainer.atoms()) {
                                                if ((int) tmpRingFragContainerAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY) == tmpAtomIndex) {
                                                    tmpNewBond.setAtom(tmpRingFragContainerAtom, 1);
                                                    break;
                                                }
                                            }
                                            if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINEST.intValue()) {
                                                AlkylStructureFragmenter.LOGGER.log(Level.FINEST, () -> String.format(tmpExtractionLoggerSpecifierString,
                                                        tmpArraysBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY),
                                                        "ConnectedTertQuatRing Pseudoatom Saturation Begin Atom"));
                                            }
                                            tmpRingFragmentationContainer.addAtom(tmpPseudoAtom);
                                            tmpRingFragmentationContainer.addBond(tmpNewBond);
                                        }
                                    }
                                }
                                if (!(tmpIsBeginRing || tmpIsEndRing)) {
                                    if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINEST.intValue()) {
                                        AlkylStructureFragmenter.LOGGER.log(Level.FINEST, () -> String.format(tmpExtractionLoggerSpecifierString,
                                                tmpArraysBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY),
                                                "No connection to ring"));
                                    }
                                    tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpArraysBond, tmpRingFragmentationContainer));
                                }
                            }
                        } else {
                            //separateTertQuatCarbonFromRingSetting == false
                            boolean tmpIsBondConnectedTertQuatRing = tmpArraysBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY);
                            boolean tmpIsBeginConnectedTertQuatRing = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY);
                            boolean tmpIsEndConnectedTertQuatRing = tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONNECTED_TERTIARY_QUATERNARY_RING_MARKER_KEY);
                            //skips over bonds between isolated rings / rings and linear chains
                            if (!((tmpIsBeginRing && tmpIsEndRing) || (tmpIsBeginRing || tmpIsEndRing) && !tmpIsBondConnectedTertQuatRing)
                                    && tmpIsBondConnectedTertQuatRing) {
                                //adds bond connecting tertiary/quaternary carbons and rings
                                if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINEST.intValue()) {
                                    AlkylStructureFragmenter.LOGGER.log(Level.FINEST, () -> String.format(tmpExtractionLoggerSpecifierString,
                                            tmpArraysBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY),
                                            "ConnectedTertQuatRing bond"));
                                }
                                tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpArraysBond, tmpRingFragmentationContainer));
                            } else if ((tmpIsBeginConnectedTertQuatRing || tmpIsEndConnectedTertQuatRing)
                                    && (tmpIsBeginTertiary || tmpIsEndTertiary || tmpIsBeginQuaternary || tmpIsEndQuaternary)) {
                                //adds bond where begin or end are tertiary/quaternary and part of a ring/tertiary/quaternary connecting bond
                                if (this.isolateTertQuatCarbonSetting.get()) {
                                    if (!(tmpIsBeginConjPi || tmpIsEndConjPi)) {
                                        tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpArraysBond, tmpRingFragmentationContainer));
                                    }
                                } else {
                                    tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpArraysBond, tmpRingFragmentationContainer));
                                }
                            } else if ((tmpIsBeginTertiary || tmpIsEndTertiary || tmpIsBeginQuaternary || tmpIsEndQuaternary) && (tmpIsBeginConjPi || tmpIsEndConjPi)) {
                                //adds bond where one end is tertiary/quaternary and the other part of a conj. pi system
                                if (!this.isolateTertQuatCarbonSetting.get()) {
                                    if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINEST.intValue()) {
                                        AlkylStructureFragmenter.LOGGER.log(Level.FINEST, () -> String.format(tmpExtractionLoggerSpecifierString,
                                                tmpArraysBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY),
                                                "Connected tert/quat and conj."));
                                    }
                                    tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpArraysBond, tmpRingFragmentationContainer));
                                }
                            } else {
                                //adds residual bonds depending on conditions, first: if "isolate tert/quat" setting false; second: see comment below
                                if (!this.isolateTertQuatCarbonSetting.get()) {
                                    if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINEST.intValue()) {
                                        AlkylStructureFragmenter.LOGGER.log(Level.FINEST, () -> String.format(tmpExtractionLoggerSpecifierString,
                                                tmpArraysBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY),
                                                "Residual bonds (isolateTertQuat = false)"));
                                    }
                                    tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpArraysBond, tmpRingFragmentationContainer));
                                } else {
                                    //adds bond for neighbor atoms within a tertiary/quaternary system connected to a ring,
                                    //  ensuring correct setting hierarchy for NOT isolating tertiary/quaternary carbons when
                                    //  part of connected systems while separation setting is set to "false"
                                    for (IAtom tmpBondAtom : tmpArraysBond.atoms()) {
                                        for (IAtom tmpNeighborAtom : tmpBondAtom.neighbors()) {
                                            if ((boolean) tmpNeighborAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY)
                                                    && (boolean) tmpNeighborAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY)) {
                                                if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINEST.intValue()) {
                                                    AlkylStructureFragmenter.LOGGER.log(Level.FINEST, () -> String.format(tmpExtractionLoggerSpecifierString,
                                                            tmpArraysBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY),
                                                            "Residual bonds (isolateTertQuat = true)"));
                                                }
                                                tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpArraysBond, tmpRingFragmentationContainer));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if ((tmpIsBeginConjPi || tmpIsEndConjPi) && (tmpIsBeginTertiary || tmpIsEndTertiary || tmpIsBeginQuaternary || tmpIsEndQuaternary)) {
                        //adds bond when tert/quat system connected to conj. pi system AND "isolate tert/quat" setting false
                        if (!this.isolateTertQuatCarbonSetting.get()) {
                            if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINEST.intValue()) {
                                AlkylStructureFragmenter.LOGGER.log(Level.FINEST, () -> String.format(tmpExtractionLoggerSpecifierString,
                                        tmpArraysBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY),
                                        "Connected tert/quat and conj. (isolateTertQuat = false)"));
                            }
                            tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpArraysBond, tmpRingFragmentationContainer));
                        }
                    } else {
                        //residual bonds
                        if (this.isolateTertQuatCarbonSetting.get()) {
                            //stops bond extraction for bonds between double/triple bond atoms and neighbor atoms
                            if (tmpIsBeginDouble || tmpIsEndDouble || tmpIsBeginTriple || tmpIsEndTriple) {
                                if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINEST.intValue()) {
                                    AlkylStructureFragmenter.LOGGER.log(Level.FINEST, () -> String.format(tmpExtractionLoggerSpecifierString,
                                            tmpArraysBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY),
                                            "Stop bonds between double/triple bond atoms and neighbor atoms"));
                                }
                            } else if ((tmpIsBeginConjPi || tmpIsBeginRing)  || (tmpIsEndConjPi || tmpIsEndRing)) {
                                if (tmpIsBeginNeighbor) {
                                    for (IAtom tmpNeighborAtom : tmpBeginAtom.neighbors()) {
                                        if ((boolean) tmpNeighborAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY)
                                                || (boolean) tmpNeighborAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY)) {
                                            break;
                                        }
                                    }
                                } else if (tmpIsEndNeighbor) {
                                    for (IAtom tmpNeighborAtom : tmpEndAtom.neighbors()) {
                                        if ((boolean) tmpNeighborAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY)
                                                || (boolean) tmpNeighborAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY)) {
                                            break;
                                        }
                                    }
                                }
                            } else {
                                if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINEST.intValue()) {
                                    AlkylStructureFragmenter.LOGGER.log(Level.FINEST, () -> String.format(tmpExtractionLoggerSpecifierString,
                                            tmpArraysBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY),
                                            "Residual bonds (isolateTertQuat = true)"));
                                }
                                tmpChainFragmentationContainer.addBond(this.deepCopyBond(tmpArraysBond, tmpChainFragmentationContainer));
                            }
                        } else {
                            //for this.isolateTertQuatCarbonSetting == false:
                            //extracts bonds where atoms DO NOT have the following markers active: (ring AND conjugated pi) AND (double OR triple)
                            //as well as (tertiary OR quaternary OR neighbor)
                            //skips bonds of methyl groups and rings
                            if ((tmpIsBeginRing && !tmpIsEndRing && tmpEndAtom.getBondCount() == 1) || (!tmpIsBeginRing && tmpIsEndRing && tmpBeginAtom.getBondCount() == 1)) {
                                continue;
                            } else if (!(tmpIsBeginRing && tmpIsEndRing && tmpIsBeginConjPi && tmpIsEndConjPi) && !(tmpIsBeginDouble || tmpIsEndDouble || tmpIsBeginTriple || tmpIsEndTriple)
                                    && !(tmpIsBeginTertiary || tmpIsEndTertiary || tmpIsBeginQuaternary || tmpIsEndQuaternary || tmpIsBeginNeighbor || tmpIsEndNeighbor)) {
                                if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINEST.intValue()) {
                                    AlkylStructureFragmenter.LOGGER.log(Level.FINEST, () -> String.format(tmpExtractionLoggerSpecifierString,
                                            tmpArraysBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY),
                                            "Residual bonds (extra markers)"));
                                }
                                //excludes bonds between [ring - linear_chain - conj._pi] configurated atoms
                                if ((tmpIsBeginRing && !tmpIsEndRing) || (tmpIsEndRing && !tmpIsBeginRing)) {
                                    if (!tmpIsEndRing) {
                                        boolean tmpIsAnyNeighborInConjPiSys = false;
                                        for (IAtom tmpNeighborAtom: tmpEndAtom.neighbors()) {
                                            if ((boolean) tmpNeighborAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY)) {
                                                tmpIsAnyNeighborInConjPiSys = true;
                                            }
                                        }
                                        if (tmpIsAnyNeighborInConjPiSys) {
                                            continue;
                                        }
                                    }
                                    if (!tmpIsBeginRing) {
                                        boolean tmpIsAnyNeighborInConjPiSys = false;
                                        for (IAtom tmpNeighborAtom: tmpBeginAtom.neighbors()) {
                                            if ((boolean) tmpNeighborAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY)) {
                                                tmpIsAnyNeighborInConjPiSys = true;
                                            }
                                        }
                                        if (tmpIsAnyNeighborInConjPiSys) {
                                            continue;
                                        }
                                    }
                                }
                                tmpChainFragmentationContainer.addBond(this.deepCopyBond(tmpArraysBond, tmpChainFragmentationContainer));
                            }
                        }
                    }
                } else {
                    //extracts ring or conjugated bonds
                    if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINEST.intValue()) {
                        AlkylStructureFragmenter.LOGGER.log(Level.FINEST, () -> String.format(tmpExtractionLoggerSpecifierString,
                                tmpArraysBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY),
                                "Ring/Conj."));
                    }
                    tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpArraysBond, tmpRingFragmentationContainer));
                }
            } catch (IllegalArgumentException anIllegalArgumentException) {
                throw new IllegalArgumentException("Bond could not be extracted at bond with index: "
                        + tmpArraysBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY)
                        + "!" + "Cause: " + anIllegalArgumentException);
            }
        } //end of loop over bond array
        tmpExtractedAtomAndBondACSet.addAtomContainer(tmpRingFragmentationContainer);
        tmpExtractedAtomAndBondACSet.addAtomContainer(tmpChainFragmentationContainer);
        tmpExtractedAtomAndBondACSet.addAtomContainer(tmpIsolatedMultiBondsContainer);
        tmpExtractedAtomAndBondACSet.addAtomContainer(tmpTertQuatCarbonContainer);
        return tmpExtractedAtomAndBondACSet;
    }

    /**
     * Protected method wrapping extraction and saturation of tertiary and quaternary carbons with pseudo atoms.
     *
     * @param anAtom with either tertiary or quaternary mark to extract and saturate with respective number of pseudo atoms
     * @return IAtomContainer instance with the extracted and saturated carbon system
     */
    protected IAtomContainer extractTertQuatCarbons(IAtom anAtom) {
        IAtomContainer tmpContainer = this.chemObjectBuilderInstance.newAtomContainer();
        tmpContainer.addAtom(this.deepCopyAtom(anAtom));
        String tmpExtractionLoggerSpecifierString = "Extraction.AtomIteration at Index: %d, Step: %s";
        for (int i = 0; i < 4; i++) {
            if ((boolean) anAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY) && i == 3) {
                break ;
            }
            if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINEST.intValue()) {
                AlkylStructureFragmenter.LOGGER.log(Level.FINEST, () -> String.format(tmpExtractionLoggerSpecifierString,
                        anAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY),
                        "Pseudoatom saturation"));
            }
            tmpContainer.addAtom(new PseudoAtom());
            IBond tmpBond = new Bond();
            tmpBond.setOrder(IBond.Order.SINGLE);
            tmpBond.setAtom(tmpContainer.getAtom(0), 0);
            tmpBond.setAtom(tmpContainer.getAtom(i+1), 1);
            //deep copy not needed as referenced bond is new and has no connection to original molecule
            tmpContainer.addBond(tmpBond);
        }
        if (AlkylStructureFragmenter.LOGGER.getParent().getLevel().intValue() <= Level.FINEST.intValue()) {
            AlkylStructureFragmenter.LOGGER.log(Level.FINEST, () -> String.format(tmpExtractionLoggerSpecifierString,
                    anAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY),
                    "Pseudoatom Container"));
        }
        return tmpContainer;
    }
    /**
     * Protected method wrapping the check for disconnected fragmentation atom containers, and if any are present, their separation.
     *
     * @param aDisconnectedAtomContainerSet to check for disconnected atom containers to separate
     * @return atom container set with separated atom containers
     */
    protected IAtomContainerSet disperseDisconnectedAtomContainerSet(IAtomContainerSet aDisconnectedAtomContainerSet) {
        Objects.requireNonNull(aDisconnectedAtomContainerSet);
        //break down to using only param ACSet
        IAtomContainerSet tmpDispersedAtomContainerSet = new AtomContainerSet();
        IAtomContainer tmpRingFragmentationContainer = this.chemObjectBuilderInstance.newAtomContainer();
        IAtomContainer tmpChainFragmentationContainer = this.chemObjectBuilderInstance.newAtomContainer();
        IAtomContainer tmpIsolatedMultiBondsContainer = this.chemObjectBuilderInstance.newAtomContainer();
        IAtomContainer tmpTertQuatCarbonContainer = this.chemObjectBuilderInstance.newAtomContainer();
        if (aDisconnectedAtomContainerSet.getAtomContainer(0) != null) {
            tmpRingFragmentationContainer = aDisconnectedAtomContainerSet.getAtomContainer(0);
        }
        if (aDisconnectedAtomContainerSet.getAtomContainer(1) != null) {
            tmpChainFragmentationContainer = aDisconnectedAtomContainerSet.getAtomContainer(1);
        }
        if (aDisconnectedAtomContainerSet.getAtomContainer(2) != null) {
            tmpIsolatedMultiBondsContainer = aDisconnectedAtomContainerSet.getAtomContainer(2);
        }
        if (aDisconnectedAtomContainerSet.getAtomContainer(3) != null) {
            tmpTertQuatCarbonContainer = aDisconnectedAtomContainerSet.getAtomContainer(3);
        }
        //extracts disconnected ring structures from one atom container into atom container set
        IAtomContainerSet tmpRingACSet = new AtomContainerSet();
        if (!tmpRingFragmentationContainer.isEmpty()) {
            tmpRingACSet = this.separateDisconnectedStructures(tmpRingFragmentationContainer);
        }
        //extracts disconnected isolated tertiary and quaternary systems into atom container set
        IAtomContainerSet tmpSingleACSet = new AtomContainerSet();
        if (!tmpTertQuatCarbonContainer.isEmpty()) {
            tmpDispersedAtomContainerSet.add(this.separateDisconnectedStructures(tmpTertQuatCarbonContainer));
        }
        //if more than one atom container containing a ring system is present, add to extraction atom container set
        if (!tmpRingACSet.isEmpty() && tmpRingACSet.getAtomContainerCount() > 0) {
            tmpDispersedAtomContainerSet.add(tmpRingACSet);
        }
        //if more than one atom container containing singular structures is present, add it to the extraction set
        if (!tmpSingleACSet.isEmpty() && tmpSingleACSet.getAtomContainerCount() > 0) {
            tmpDispersedAtomContainerSet.add(tmpSingleACSet);
        }
        //remnants after ring, conj. system and tertiary/quaternary carbon extractions
        //expected to be only linear carbohydrates
        if (!tmpIsolatedMultiBondsContainer.isEmpty()) {
            tmpDispersedAtomContainerSet.add(this.separateDisconnectedStructures(tmpIsolatedMultiBondsContainer));
        }
        IAtomContainerSet tmpChainACSet = this.separateDisconnectedStructures(tmpChainFragmentationContainer);
        int tmpMaxChainLengthInteger = this.maxChainLengthSetting.get();
        //checks for applied restrictions, default restriction is set to 6
        if (this.fragmentSideChainsSetting.get()) {
            //check maxchainlength: 1 -> only methanes, >= 2 -> respective alkane
            if (tmpMaxChainLengthInteger == 1) {//single methane molecules
                IAtomContainer tmpDissectedAC = new AtomContainer();
                for (IAtomContainer tmpAtomContainer : tmpChainACSet.atomContainers()) {
                    tmpAtomContainer.removeAllBonds();
                    tmpDissectedAC.add(tmpAtomContainer);
                }
                tmpDispersedAtomContainerSet.add(this.separateDisconnectedStructures(tmpDissectedAC));
            } else {//restrictions > 1
                for (IAtomContainer tmpAtomContainer : tmpChainACSet.atomContainers()) {
                    IAtomContainer tmpDissectedAC = this.dissectLinearChain(tmpAtomContainer, tmpMaxChainLengthInteger);
                    tmpDispersedAtomContainerSet.add(this.separateDisconnectedStructures(tmpDissectedAC));
                }
            }
        } else {
            tmpDispersedAtomContainerSet.add(tmpChainACSet);
        }
        return tmpDispersedAtomContainerSet;
    }

    /**
     * Protected method wrapping all extraction steps (atom extraction, bond extraction and disconnected atom container check/extraction).
     *
     * @param aMolecularArraysInstance with molecule data (atoms and bonds)
     * @return atom container set with separated atom containers, each with only one generated fragment
     */
    protected IAtomContainerSet getFragmentationResults(MolecularArrays aMolecularArraysInstance) {
        Objects.requireNonNull(aMolecularArraysInstance);
        IAtomContainerSet tmpExtractedAtomACSet = this.extractAtoms(aMolecularArraysInstance.getAtomArray());
        IAtomContainerSet tmpExtractedAtomAndBondACSet = this.extractBonds(aMolecularArraysInstance.getBondArray(), tmpExtractedAtomACSet);
        return this.disperseDisconnectedAtomContainerSet(tmpExtractedAtomAndBondACSet);
    }

    //</editor-fold>
    //
    //<editor-fold desc="Extraction Utility Methods">
    /**
     * Protected method to check given atom container for disconnected structures.
     *
     * @param anAtomContainer IAtomContainer to check
     * @return IAtomContainerSet containing partitioned structures as single IAtomContainer
     * @throws IllegalArgumentException if anything goes wrong
     */
    protected IAtomContainerSet separateDisconnectedStructures(IAtomContainer anAtomContainer) throws IllegalArgumentException{
        Objects.requireNonNull(anAtomContainer,"Given IAtomContainer is null.");
        IAtomContainerSet tmpFragmentSet = new AtomContainerSet();
        try {
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
            AlkylStructureFragmenter.LOGGER.log(Level.WARNING, String.format(AlkylStructureFragmenter.LOGGER_EXCEPTION_STRING_FORMAT,
                    anException, anAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY), "Connectivity Check failed."),
                    anException);
        }
        return tmpFragmentSet;
    }
    /**
     * Protected method to saturate a given molecule with implicit hydrogens after fragmentation.
     *
     * @param anUnsaturatedACSet IAtomContainerSet whose atom containers are to be saturated
     * @return List of processed atom containers, @null if given Set is empty
     * @throws CDKException if CDKHydrogenAdder throws an exception
     */
    protected IAtomContainerSet saturateWithImplicitHydrogen(IAtomContainerSet anUnsaturatedACSet) throws CDKException {
        Objects.requireNonNull(anUnsaturatedACSet, "Given IAtomContainerSet is null.");
        try {
            if (!anUnsaturatedACSet.isEmpty() && !anUnsaturatedACSet.getAtomContainer(0).isEmpty()) {
                for (IAtomContainer tmpAtomContainer: anUnsaturatedACSet.atomContainers()) {
                    if (tmpAtomContainer != null && !tmpAtomContainer.isEmpty()) {
                        ChemUtil.saturateWithHydrogen(tmpAtomContainer);
                    }
                }
            }
            return anUnsaturatedACSet;
        } catch (CDKException anException) {
            throw new CDKException(String.format(AlkylStructureFragmenter.LOGGER_EXCEPTION_STRING_FORMAT,
                    anException, "None given.",
                    "Saturation failed."), anException);
        }
    }
    /**
     * Protected method to dissect given AtomContainer (containing linear carbon chain) into separate molecules with given length.
     * Returns remnants of chains as disconnected structures if they are falling short of set maximum length
     * (i.e. set maximum is 6, chain is 8 C's long -> fragment of length 6 is returned, together with a disconnected remnant of length 2).
     * The used counter starts at 1 as to allow a one-to-one "translation" of user input for the setting and implementation.
     *
     * @param aLinearChainAC AtomContainer to be dissected
     * @param aMaxChainLength Given maximum length of molecule
     * @return AtomContainer with separate dissected molecules
     */
    protected IAtomContainer dissectLinearChain(IAtomContainer aLinearChainAC, int aMaxChainLength) {
        Objects.requireNonNull(aLinearChainAC);
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
    //
    //<editor-fold desc="Deep Copy Methods">
    /**
     * Method to create a deeper copy of a given atom, meant to replace the default addAtom() method of IAtomContainer
     * in which only the reference is added to the atom container instead of a new atom.
     * <p>
     * Important notice!
     * During deep copying of an atom, its index of/in the associated atom container is lost.
     * Therefor the internal ASF.Atom_Index property is used as means of indices throughout algorithm logic.
     * Copying the CDK flags by deep copy manners (instancing new with origin value) results in not-traceable IndexOutOfBound
     * and IllegalState exceptions. Therefor, flags are copied directly from the original instance, knowingly diverting
     * from a deep copy.
     * </p>
     *
     * @param anAtomToCopy the atom to create a deep copy of
     * @return the deep copy of the given atom
     */
    protected IAtom deepCopyAtom(IAtom anAtomToCopy) {
        Objects.requireNonNull(anAtomToCopy);
        IAtom tmpNewAtom = this.chemObjectBuilderInstance.newAtom();
        //"important" atom properties
        tmpNewAtom.setImplicitHydrogenCount(anAtomToCopy.getImplicitHydrogenCount());
        tmpNewAtom.setCharge(anAtomToCopy.getCharge());
        tmpNewAtom.setMaxBondOrder(anAtomToCopy.getMaxBondOrder());
        tmpNewAtom.setBondOrderSum(anAtomToCopy.getBondOrderSum());
        tmpNewAtom.setAtomicNumber(anAtomToCopy.getAtomicNumber());
        //set atom coordinate if one is present (non-null)
        if (anAtomToCopy.getPoint2d() != null) {
            Point2d tmpPoint2d = new Point2d();
            tmpPoint2d.set(anAtomToCopy.getPoint2d().x, anAtomToCopy.getPoint2d().y);
            tmpNewAtom.setPoint2d(tmpPoint2d);
        }
        if (anAtomToCopy.getPoint3d() != null) {
            Point3d tmpPoint3d = new Point3d();
            tmpPoint3d.set(anAtomToCopy.getPoint3d().x, anAtomToCopy.getPoint3d().y, anAtomToCopy.getPoint3d().z);
            tmpNewAtom.setPoint3d(tmpPoint3d);
        }
        //trying to copy flags in deep copy manners results in strange index and illegal state exceptions
        //therefor, a direct copy is used here knowingly diverting from a deep copy
        tmpNewAtom.setFlags(anAtomToCopy.getFlags());
        //IMPORTANT! Make sure to add new internal properties below!
        //<editor-fold desc="Property Deep Copy">
        for (Map.Entry<Object, Object> tmpEntry : anAtomToCopy.getProperties().entrySet()) {
            if ((tmpEntry.getKey() instanceof String || tmpEntry.getKey() instanceof Integer || tmpEntry.getKey() instanceof Boolean)
                    && (tmpEntry.getValue() instanceof String || tmpEntry.getValue() instanceof Integer
                    || tmpEntry.getValue() instanceof Boolean || tmpEntry.getValue() == null)) {
                tmpNewAtom.setProperty(tmpEntry.getKey(), tmpEntry.getValue());
            }
        }
        //</editor-fold>
        return tmpNewAtom;
    }

    /**
     * Method to create a deep copy of a given bond, meant to replace the default addBond() method of IAtomContainer.
     *
     * <p>
     * Please make sure to add newly integrated internal properties into the method's properties-copy routine.
     * Otherwise, they will be lost during deep-copying!
     * </p>
     *
     * @param aBondToCopy                 the bond to create a deep copy of
     * @param aBondIncludingAtomContainer the atom container in which the bond's atoms are placed in
     * @return the deep copy of the given bond
     * @throws IllegalArgumentException if given parameter is not valid, i.e. the given bond's atoms have no assigned index
     */
    protected IBond deepCopyBond(IBond aBondToCopy, IAtomContainer aBondIncludingAtomContainer) throws IllegalArgumentException {
        Objects.requireNonNull(aBondToCopy);
        Objects.requireNonNull(aBondIncludingAtomContainer);
        IBond tmpNewBond = this.chemObjectBuilderInstance.newBond();
        IBond.Order tmpOriginOrder = aBondToCopy.getOrder();
        switch (tmpOriginOrder) {
            case SINGLE -> tmpNewBond.setOrder(IBond.Order.SINGLE);
            case DOUBLE -> tmpNewBond.setOrder(IBond.Order.DOUBLE);
            case TRIPLE -> tmpNewBond.setOrder(IBond.Order.TRIPLE);
            case QUADRUPLE -> tmpNewBond.setOrder(IBond.Order.QUADRUPLE);
            //all orders beyond quadruple are not needed here nor should they be copied
            default -> throw new IllegalArgumentException("Given Order diverted from expected range (single up to quadruple).");
        }
        //trying to copy flags in deep copy manners results in strange index and illegal state exceptions
        //therefor, a direct copy is used here knowingly diverting from a deep copy
        tmpNewBond.setFlags(aBondToCopy.getFlags());
        //IMPORTANT! Make sure to add new internal properties below!
        //<editor-fold desc="Property Deep Copy">
        for (Map.Entry<Object, Object> tmpEntry : aBondToCopy.getProperties().entrySet()) {
            if ((tmpEntry.getKey() instanceof String || tmpEntry.getKey() instanceof Integer || tmpEntry.getKey() instanceof Boolean)
                    && (tmpEntry.getValue() instanceof String || tmpEntry.getValue() instanceof Integer
                    || tmpEntry.getValue() instanceof Boolean || tmpEntry.getValue() == null)) {
                tmpNewBond.setProperty(tmpEntry.getKey(), tmpEntry.getValue());
            }
        }
        //</editor-fold>
        IAtom tmpBeginAtom;
        try {
            tmpBeginAtom = aBondToCopy.getBegin();
        } catch (NullPointerException aNullPointerException) {
            throw new IllegalArgumentException("In: deepCopyBond(): Begin Atom was null at bond index: "
                    + aBondToCopy.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY));
        }
        IAtom tmpEndAtom;
        try {
            tmpEndAtom = aBondToCopy.getEnd();
        } catch (NullPointerException aNullPointerException) {
            throw new IllegalArgumentException("In: deepCopyBond(): End Atom was null at bond index: "
                    + aBondToCopy.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY));
        }
        if (tmpBeginAtom == null) {
            tmpBeginAtom = new PseudoAtom();
        } else if (tmpEndAtom == null) {
            tmpEndAtom = new PseudoAtom();
        }
        int tmpBeginAtomIndex = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
        if ((tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY) != null)) {
            int tmpEndAtomIndex = tmpEndAtom.getProperty(INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
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
        } else {
            throw new IllegalArgumentException("Deep copy of bond not possible. No 'ASF.ATOM_INDEX' value was found.");
        }
        return tmpNewBond;
    }
    //</editor-fold>
    //
    //</editor-fold>
    /**
     * Checks whether the given atom is a pseudo atom. Very strict, any atom
     * whose atomic number is null or 0, whose symbol equals "R" or "*", or that
     * is an instance of an IPseudoAtom implementing class will be classified as
     * a pseudo atom.
     *
     * @param atom the atom to test
     * @return true if the given atom is identified as a pseudo (R) atom
     */
    protected boolean isPseudoAtom(IAtom atom) {
        Integer tmpAtomicNr = atom.getAtomicNumber();
        if (Objects.isNull(tmpAtomicNr)) {
            return true;
        }
        String tmpSymbol = atom.getSymbol();
        return tmpAtomicNr == IElement.Wildcard ||
                tmpSymbol.equals("R") ||
                tmpSymbol.equals("*") ||
                atom instanceof IPseudoAtom;
    }

    /**
     * Checks whether the given atom is a hetero-atom (i.e. non-carbon and
     * non-hydrogen). Pseudo (R) atoms will also return false.
     *
     * @param atom the atom to test
     * @return true if the given atom is neither a carbon nor a hydrogen or
     *         pseudo atom
     */
    protected boolean isHeteroAtom(IAtom atom) {
        Integer tmpAtomicNr = atom.getAtomicNumber();
        if (Objects.isNull(tmpAtomicNr)) {
            return false;
        }
        int tmpAtomicNumberInt = tmpAtomicNr;
        return tmpAtomicNumberInt != IElement.H && tmpAtomicNumberInt != IElement.C
                && !this.isPseudoAtom(atom);
    }

    /**
     * Utility method to count non-pseudo atoms of a given AtomContainer.
     *
     * @param anAtomContainerToCount atom container in which to count
     * @return int count of non-pseudo atoms in given atom container
     */
    protected int countAtoms(IAtomContainer anAtomContainerToCount) {
        int tmpAtomCount = 0;
        for (IAtom tmpAtom: anAtomContainerToCount.atoms()) {
            if (!this.isPseudoAtom(tmpAtom)) {
                tmpAtomCount++;
            }
        }
        return tmpAtomCount;
    }

    /**
     * Utility method creating a map of [@link IChemObject] and [@link IStereoElement] of the IChemObjects (atoms and bonds)
     * of a given atom container.
     *
     * @param aStereoMolecule atom container from which the map is to be created
     * @return created atoms/bonds and IStereoElements map
     */
    protected Map<IChemObject, IStereoElement<IChemObject, IChemObject>> createStereoChemMap(IAtomContainer aStereoMolecule) {
        Iterable<IStereoElement> tmpStereoChemIterable = aStereoMolecule.stereoElements();
        int tmpStereoChemIterableSize = 0;
        if (tmpStereoChemIterable instanceof Collection) {
            tmpStereoChemIterableSize = ((Collection<?>) tmpStereoChemIterable).size();
        }
        Map<IChemObject, IStereoElement<IChemObject, IChemObject>> tmpStereoChemOriginMoleculeMap
                = new HashMap<>(tmpStereoChemIterableSize, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        for (IStereoElement tmpStereoElement : aStereoMolecule.stereoElements()) {
            IChemObject tmpStereoFocus = tmpStereoElement.getFocus();
            if (tmpStereoFocus instanceof IAtom || tmpStereoFocus instanceof IBond) {
                tmpStereoChemOriginMoleculeMap.put(tmpStereoFocus, tmpStereoElement);
            }
        }
        return tmpStereoChemOriginMoleculeMap;
    }

    /**
     * Utility method remapping the original molecule's stereo chemistry configuration onto a given molecule fragment where possible.
     *
     * @param aFragment atom container onto which stereo chemistry configuration is added
     * @param aStereoChemMap with the original stereo chemistry configuration
     * @return fragment atom container with added stereo chemistry
     */
    protected IAtomContainer remapStereoChem(IAtomContainer aFragment, Map<IChemObject, IStereoElement<IChemObject, IChemObject>> aStereoChemMap) {
        //key: fragment atom, value: stereo map atom (original molecule) -> analog for bonds
        Map<IAtom, IAtom> tmpStereoChemOriginAtomToCopyMap = new HashMap<>();
        Map<IBond, IBond> tmpStereoChemOriginBondToCopyMap = new HashMap<>();

        //create mapping of fragment (aFragment) atoms and bonds to the original stereo chem map
        for (IAtom tmpFragmentAtom : aFragment.atoms()) {
            int tmpAtomIndex = tmpFragmentAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
            for (Map.Entry<IChemObject, IStereoElement<IChemObject, IChemObject>> tmpEntry : aStereoChemMap.entrySet()) {
                if (tmpEntry.getKey() instanceof IAtom tmpStereoMapAtom
                        && (int) tmpStereoMapAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY) == tmpAtomIndex) {
                    tmpStereoChemOriginAtomToCopyMap.put(tmpFragmentAtom, (IAtom) tmpEntry.getKey());
                }
            }
        }
        for (IBond tmpFragmentBond : aFragment.bonds()) {
            int tmpBondIndex = tmpFragmentBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY);
            for (Map.Entry<IChemObject, IStereoElement<IChemObject, IChemObject>> tmpEntry : aStereoChemMap.entrySet()) {
                if (tmpEntry.getKey() instanceof IBond tmpStereoMapBond
                        && (int) tmpStereoMapBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY) == tmpBondIndex) {
                    tmpStereoChemOriginBondToCopyMap.put(tmpFragmentBond, (IBond) tmpEntry.getKey());
                }
            }
        }
        for (Map.Entry<IAtom, IAtom> tmpEntry : tmpStereoChemOriginAtomToCopyMap.entrySet()) {
            System.out.println(tmpEntry.getKey());
            System.out.println((int) tmpEntry.getKey().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY));
            System.out.println(tmpEntry.getValue());
            System.out.println((int) tmpEntry.getValue().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY));
            System.out.println("---");
        }
        StereoElementFactory stereo = StereoElementFactory.using2DCoordinates(aFragment).interpretProjections(Projection.Haworth);
        aFragment.setStereoElements(stereo.createAll());

        for (IAtom tmpFragmentAtom : aFragment.atoms()) {
            IAtom tmpOriginAtom = tmpStereoChemOriginAtomToCopyMap.get(tmpFragmentAtom);
            IStereoElement<IChemObject, IChemObject> tmpOriginStereoElement = aStereoChemMap.get(tmpOriginAtom);
            IStereoElement<IChemObject, IChemObject> tmpFragmentStereoElement;
            //i dont know how to create a new stereo element in the fragment atom container
        }


        for (Map.Entry<IChemObject, IStereoElement<IChemObject, IChemObject>> tmpEntry : aStereoChemMap.entrySet()) {
            System.out.println("Config class: " + tmpEntry.getValue().getConfigClass());
            System.out.println("Config: " + tmpEntry.getValue().getConfig());
            System.out.println("config Order: " + tmpEntry.getValue().getConfigOrder());
        }
//        for (IAtom tmpAtom : aFragment.atoms()) {
//            int tmpAtomIndex = tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
//            for (Map.Entry<IChemObject, IStereoElement<IChemObject, IChemObject>> tmpEntry : aStereoChemMap.entrySet()) {
//                if (tmpEntry.getKey() instanceof IAtom tmpStereoMapAtom
//                        && (int) tmpStereoMapAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY) == tmpAtomIndex) {
//                    IStereoElement tmpStereoElement = tmpEntry.getValue();
//                    List<?> tmpStereoCarrierList = tmpStereoElement.getCarriers();
//                    List<IChemObject> tmpRemappedStereoCarrierList = new ArrayList<>();
//                    for (int i = 0; i < tmpStereoCarrierList.size(); i++) {
//                        if (tmpStereoCarrierList.get(i) instanceof IAtom tmpCarrierAtom) {
//                            tmpRemappedStereoCarrierList.add(tmpCarrierAtom);
//                        }
//                    }
//                    boolean tmpIsAllRemappedCarrierPresent = true;
//                    for (IChemObject tmpRemappedCarrierChemObject : tmpRemappedStereoCarrierList) {
//                        if (tmpRemappedCarrierChemObject instanceof IAtom) {
//                            if (!aFragment.contains((IAtom) tmpRemappedCarrierChemObject)) {
//                                tmpIsAllRemappedCarrierPresent = false;
//                            }
//                        } else if (tmpRemappedCarrierChemObject instanceof IBond) {
//                            if (!aFragment.contains((IBond) tmpRemappedCarrierChemObject)) {
//                                tmpIsAllRemappedCarrierPresent = false;
//                            }
//                        }
//                    }
//                    if (tmpIsAllRemappedCarrierPresent) {
//                        aFragment.addStereoElement(tmpStereoElement);
//                    }
//                }
//            }
//        }
        return aFragment;
    }
    //
    //</editor-fold>
}
