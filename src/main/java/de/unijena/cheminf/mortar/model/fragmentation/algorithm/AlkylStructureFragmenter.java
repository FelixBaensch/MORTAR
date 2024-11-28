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
import org.openscience.cdk.graph.invariant.ConjugatedPiSystemsDetector;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IElement;
import org.openscience.cdk.interfaces.IRingSet;
import org.openscience.cdk.ringsearch.RingSearch;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

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
 * @version 1.0.0.0
 */
public class AlkylStructureFragmenter implements IMoleculeFragmenter{
    //
    //<editor-fold desc="Public Static Final Class Variables">
    /**
     * Name of the fragmenter.
     */
    public static final String ALGORITHM_NAME = "Alkyl Fragmenter";
    /**
     * Default boolean value to deter keeping non-fragmentable molecules in the fragmenter pipeline.
     */
    public static final boolean KEEP_NON_FRAGMENTABLE_MOLECULES_SETTING_DEFAULT = true;
    /**
     * Default value for maximum length of carbon side chains.
     */
    public static final int MAX_CHAIN_LENGTH_SETTING_DEFAULT = 6;
    /**
     * Default boolean value for determination of further side chain dissection.
     */
    public static final boolean FRAGMENT_SIDE_CHAINS_SETTING_DEFAULT = false;
    /**
     * Default boolean value for determining whether alternative or standard single carbon handling should be used.
     */
    public static final boolean ALT_HANDLING_SINGLE_TERT_QUAT_CARBONS_SETTING_DEFAULT = false;
    /**
     * Default boolean value for determining which single ring detection method should be used.
     */
    public static final boolean MCB_SINGLE_RING_DETECTION_SETTING_DEFAULT = false;
    /**
     * Default boolean value for keeping rings intact setting .
     */
    public static final boolean KEEP_RINGS_SETTING_DEFAULT = true;
    /**
     * Default boolean value for separating tertiary and quaternary carbon atoms from ring setting.
     */
    public static final boolean SEPARATE_TERT_QUAT_CARBON_FROM_RING_SETTING_DEFAULT = true;
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
     * Key for an internal integer property used in indexing detected fused rings.
     */
    public static final String INTERNAL_ASF_FUSED_RING_INDEX_PROPERTY_KEY = "ASF.FUSED_RING_INDEX";
    /**
     * Key for an internal integer property used in indexing detected isolated rings.
     */
    public static final String INTERNAL_ASF_ISOLATED_RING_INDEX_PROPERTY_KEY = "ASF.ISOLATED_RING_INDEX";
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
     * A property that has a constant fragment hydrogen saturation setting.
     */
    private final SimpleIDisplayEnumConstantProperty fragmentSaturationSetting;
    /**
     * A property that has a constant boolean value defining if non-fragmentable molecules should be kept in the
     * fragmenter pipeline.
     */
    private final SimpleBooleanProperty keepNonFragmentableMoleculesSetting;
    /**
     * A property that has a constant boolean value determining whether side chains should be fragmented.
     */
    private final SimpleBooleanProperty fragmentSideChainsSetting;
    /**
     * A property that has a constant carbon side chain setting.
     */
    private final SimpleIntegerProperty maxChainLengthSetting;
    /**
     * A property that has a constant boolean value determining which single carbon handling to use during fragmentation.
     */
    private final SimpleBooleanProperty altHandlingSingleTertQuatCarbonsSetting;
    /**
     * A property that has a constant boolean value determining which single ring detection method to use during fragmentation.
     */
    private final SimpleBooleanProperty mcbSingleRingDetectionSetting;
    /**
     * A property that has a constant boolean value defining if rings should be dissected further.
     */
    private final SimpleBooleanProperty keepRingsSetting;
    /**
     * A property that has a constant boolean value defining if tertiary and quaternary carbon atoms should be separated
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
     * Logger of this class.
     */
    private static final Logger logger = Logger.getLogger(AlkylStructureFragmenter.class.getName());
    //WIP
    private IChemObjectBuilder chemObjectBuilderInstance;
    //</editor-fold>
    //
    //<editor-fold desc="Constructor">
    /**
     * Constructor, all settings are initialised with their respective default values.
     */
    public AlkylStructureFragmenter(){
        int tmpSettingsNameTooltipNumber = 8;
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
                    AlkylStructureFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
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
        this.keepNonFragmentableMoleculesSetting = new SimpleBooleanProperty(this, "Retention setting for non-fragmentable molecules",
                AlkylStructureFragmenter.KEEP_NON_FRAGMENTABLE_MOLECULES_SETTING_DEFAULT);
        this.settingNameTooltipTextMap.put(this.keepNonFragmentableMoleculesSetting.getName(),
                Message.get("AlkylStructureFragmenter.keepNonFragmentableMoleculesSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.keepNonFragmentableMoleculesSetting.getName(),
                Message.get("AlkylStructureFragmenter.keepNonFragmentableMoleculesSetting.displayName"));
        this.fragmentSideChainsSetting = new SimpleBooleanProperty(this, "Fragmentation of hydrocarbon side chains setting",
                AlkylStructureFragmenter.FRAGMENT_SIDE_CHAINS_SETTING_DEFAULT);
        this.settingNameTooltipTextMap.put(this.fragmentSideChainsSetting.getName(),
                Message.get("AlkylStructureFragmenter.fragmentSideChainsSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.fragmentSideChainsSetting.getName(),
                Message.get("AlkylStructureFragmenter.fragmentSideChainsSetting.displayName"));
        this.maxChainLengthSetting = new SimpleIntegerProperty(this, "Carbon side chains maximum length setting",
                AlkylStructureFragmenter.MAX_CHAIN_LENGTH_SETTING_DEFAULT);
        this.settingNameTooltipTextMap.put(this.maxChainLengthSetting.getName(),
                Message.get("AlkylStructureFragmenter.maxChainLengthSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.maxChainLengthSetting.getName(),
                Message.get("AlkylStructureFragmenter.maxChainLengthSetting.displayName"));
        this.altHandlingSingleTertQuatCarbonsSetting = new SimpleBooleanProperty(this, "Single carbon handling setting",
                AlkylStructureFragmenter.ALT_HANDLING_SINGLE_TERT_QUAT_CARBONS_SETTING_DEFAULT);
        this.settingNameTooltipTextMap.put(this.altHandlingSingleTertQuatCarbonsSetting.getName(),
                Message.get("AlkylStructureFragmenter.altHandlingSingleTertQuatCarbonsSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.altHandlingSingleTertQuatCarbonsSetting.getName(),
                Message.get("AlkylStructureFragmenter.altHandlingSingleTertQuatCarbonsSetting.displayName"));
        this.mcbSingleRingDetectionSetting = new SimpleBooleanProperty(this, "Single ring detection setting",
                AlkylStructureFragmenter.MCB_SINGLE_RING_DETECTION_SETTING_DEFAULT);
        this.settingNameTooltipTextMap.put(this.mcbSingleRingDetectionSetting.getName(),
                Message.get("AlkylStructureFragmenter.mcbSingleRingDetectionSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.mcbSingleRingDetectionSetting.getName(),
                Message.get("AlkylStructureFragmenter.mcbSingleRingDetectionSetting.displayName"));
        this.keepRingsSetting = new SimpleBooleanProperty(this, "Keep rings setting",
                AlkylStructureFragmenter.KEEP_RINGS_SETTING_DEFAULT);
        this.settingNameTooltipTextMap.put(this.keepRingsSetting.getName(),
                Message.get("AlkylStructureFragmenter.keepRingsSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.keepRingsSetting.getName(),
                Message.get("AlkylStructureFragmenter.keepRingsSetting.displayName"));
        this.separateTertQuatCarbonFromRingSetting = new SimpleBooleanProperty(this, "Separate tertiary and " +
                "quaternary carbon atoms from ring structures setting", AlkylStructureFragmenter.SEPARATE_TERT_QUAT_CARBON_FROM_RING_SETTING_DEFAULT);
        this.settingNameTooltipTextMap.put(this.separateTertQuatCarbonFromRingSetting.getName(),
                Message.get("AlkylStructureFragmenter.separateTertQuatCarbonFromRingSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.separateTertQuatCarbonFromRingSetting.getName(),
                Message.get("AlkylStructureFragmenter.separateTertQuatCarbonFromRingSetting.displayName"));
        this.settings = new ArrayList<>(8);
        this.settings.add(this.fragmentSaturationSetting);
        this.settings.add(this.keepNonFragmentableMoleculesSetting);
        this.settings.add(this.fragmentSideChainsSetting);
        this.settings.add(this.maxChainLengthSetting);
        this.settings.add(this.altHandlingSingleTertQuatCarbonsSetting);
        this.settings.add(this.mcbSingleRingDetectionSetting);
        this.settings.add(this.keepRingsSetting);
        this.settings.add(this.separateTertQuatCarbonFromRingSetting);
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

    /**
     * Public get method for retention setting for non-fragmentable molecules.
     *
     * @return boolean value of keepNonFragmentableMoleculesSetting
     */
    public boolean getKeepNonFragmentableMoleculesSetting() {
        return this.keepNonFragmentableMoleculesSetting.get();
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
     * Public get method for alternative handling of tertiary and quaternary carbon atoms setting property.
     *
     * @return SimpleBooleanProperty altHandlingSingleTertQuatCarbonsSetting
     */
    public SimpleBooleanProperty getAltHandlingSingleTertQuatCarbonsSettingProperty() {
        return this.altHandlingSingleTertQuatCarbonsSetting;
    }

    /**
     * Public get method for alternative single ring detection with MCB algorithm setting property.
     *
     * @return SimpleBooleanProperty mcbSingleRingDetectionSetting
     */
    public SimpleBooleanProperty getMcbSingleRingDetectionSettingProperty() {
        return this.mcbSingleRingDetectionSetting;
    }
    /**
     * Public get method for keep rings setting property.
     *
     * @return SimpleBooleanProperty keepRingsSetting
     */
    public SimpleBooleanProperty getKeepRingsSettingProperty() {
        return this.keepRingsSetting;
    }
    /**
     * Public get method for separate tertiary and quaternary carbon atoms from ring structures setting property.
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
     * @throws NullPointerException if given boolean is null
     */
    public void setFragmentSideChainsSetting(boolean aBoolean) throws NullPointerException{
        Objects.requireNonNull(aBoolean, "Given boolean is null.");
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
     * Set method for setting defining whether alternative single carbon handling should be used.
     *
     * @param aBoolean the given boolean value for switching handling
     */
    public void setAltHandlingTertQuatCarbonsSetting(boolean aBoolean){
        Objects.requireNonNull(aBoolean, "Given boolean is null.");
        this.altHandlingSingleTertQuatCarbonsSetting.set(aBoolean);
    }
    /**
     * Set method for setting defining whether alternative single ring detection should be used.
     *
     * @param aBoolean the given boolean value for switching between alternative and default detection
     */
    public void setMcbSingleRingDetectionSetting(boolean aBoolean) {
        Objects.requireNonNull(aBoolean, "Given boolean is null");
        this.mcbSingleRingDetectionSetting.set(aBoolean);
    }
    /**
     * Set method for setting defining if rings should be dissected or kept intact.
     *
     * @param aBoolean the given boolean value for switching between dissecting rings and keeping them intact
     */
    public void setKeepRingsSetting(boolean aBoolean) {
        Objects.requireNonNull(aBoolean, "Given boolean is null");
        this.keepRingsSetting.set(aBoolean);
    }

    /**
     * Set method for setting defining if tertiary and quaternary carbon atoms should be separated from ring structures.
     *
     * @param aBoolean the given boolean value defining if tertiary and quaternary carbon atoms should be separated from ring structures
     */
    public void setSeparateTertQuatCarbonFromRingSetting(boolean aBoolean) {
        this.separateTertQuatCarbonFromRingSetting.set(aBoolean);
    }
    public void setChemObjectBuilderInstance(IAtomContainer anAtomContainer) {
        this.chemObjectBuilderInstance = anAtomContainer.getBuilder();
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
        tmpCopy.setAltHandlingTertQuatCarbonsSetting(this.altHandlingSingleTertQuatCarbonsSetting.get());
        tmpCopy.setMcbSingleRingDetectionSetting(this.mcbSingleRingDetectionSetting.get());
        tmpCopy.setKeepRingsSetting(this.keepRingsSetting.get());
        tmpCopy.setSeparateTertQuatCarbonFromRingSetting(this.separateTertQuatCarbonFromRingSetting.get());
        return tmpCopy;
    }

    @Override
    public void restoreDefaultSettings() {
        this.fragmentSaturationSetting.set(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT);
        this.keepNonFragmentableMoleculesSetting.set(AlkylStructureFragmenter.KEEP_NON_FRAGMENTABLE_MOLECULES_SETTING_DEFAULT);
        this.fragmentSideChainsSetting.set(AlkylStructureFragmenter.FRAGMENT_SIDE_CHAINS_SETTING_DEFAULT);
        this.maxChainLengthSetting.set(AlkylStructureFragmenter.MAX_CHAIN_LENGTH_SETTING_DEFAULT);
        this.altHandlingSingleTertQuatCarbonsSetting.set(AlkylStructureFragmenter.ALT_HANDLING_SINGLE_TERT_QUAT_CARBONS_SETTING_DEFAULT);
        this.mcbSingleRingDetectionSetting.set(AlkylStructureFragmenter.MCB_SINGLE_RING_DETECTION_SETTING_DEFAULT);
        this.keepRingsSetting.set(AlkylStructureFragmenter.KEEP_RINGS_SETTING_DEFAULT);
        this.separateTertQuatCarbonFromRingSetting.set(AlkylStructureFragmenter.SEPARATE_TERT_QUAT_CARBON_FROM_RING_SETTING_DEFAULT);
    }
    //
    //<editor-fold desc="Pre-Fragmentation Tasks">
    /**
     * Returns true if the given molecule cannot be fragmented by the algorithm.
     * If the molecule is null: true is returned and therefore the molecule is filtered out.
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
        try {
            int tmpHydrogenCount = 0;
            int tmpCarbonCount = 0;
            for (IAtom tmpAtom : aMolecule.atoms()) {
                switch (tmpAtom.getAtomicNumber()) {
                    case IElement.H -> tmpHydrogenCount++;
                    case IElement.C -> tmpCarbonCount++;
                    default -> {
                        if (this.keepNonFragmentableMoleculesSetting.get()) {
                            System.out.println("filter + posSetting");
                            aMolecule.setProperty("ASF.FilterMarker", true);
                            return false;
                        }
                        return true;
                    }
                }
            }
            if (tmpCarbonCount != 0 ) {
                aMolecule.setProperty("ASF.FilterMarker", false);
                return false;
            }
            //the else condition is only meant to filter out explicit hydrogen (setting for on/off could be implemented)
            else {
                aMolecule.setProperty("ASF.FilterMarker", true);
                return true;
            }
        } catch (Exception anException) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING,
                    anException + " Molecule ID: " + aMolecule.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY),
                    anException);
            aMolecule.setProperty("ASF.FilterMarker", true);
            return true;
        }
    }
    /**
     * Method for determining if given molecule needs preprocessing.
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
        if (aMolecule.getProperty("ASF.FilterMarker")) {
            System.out.println("if canBeFrag");
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
    //
    //<editor-fold desc="Fragmentation">

    @Override
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule)
            throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        //skip fragmentation if molecule is not fragmentable and just return it as one fragment to keep it in pipeline
        if ((boolean) aMolecule.getProperty("ASF.FilterMarker") && this.keepNonFragmentableMoleculesSetting.get()) {
            List<IAtomContainer> tmpNonFragACList = new ArrayList<>();
            tmpNonFragACList.add(aMolecule);
            return tmpNonFragACList;
        }
        //<editor-fold desc="Molecule Cloning, Property and Arrays Set" defaultstate="collapsed">
        IAtomContainer tmpClone = aMolecule.clone();
        //WIP
        this.chemObjectBuilderInstance = null;
        this.chemObjectBuilderInstance = tmpClone.getBuilder();
        //IAtomContainer tmpBuilderAtomContainer = tmpClone.getBuilder().newInstance(IAtomContainer.class);
        int tmpPreFragmentationAtomCount = 0;
        for (IAtom tmpAtom: tmpClone.atoms()) {
            if (tmpAtom.getAtomicNumber() != 0) {
                tmpPreFragmentationAtomCount++;
            }
        }
        AlkylStructureFragmenter.this.logger.log(Level.INFO, "PreFragAtomCount: " + tmpPreFragmentationAtomCount);
        //move percieveAtomType to preprocessing
        try {
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpClone);
            //add ChemUtils atom checks?
        } catch (CDKException aCDKException) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING,
                    aCDKException + " Molecule ID: " + aMolecule.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY),
                    aCDKException);
            throw new IllegalArgumentException();
        }
        //</editor-fold>
        //
        //<editor-fold desc="Detection Steps" defaultstate="collapsed">
        Object[] tmpObject = new Object[2];
        tmpObject[0] = this.fillAtomArray(tmpClone);
        tmpObject[1] = this.fillBondArray(tmpClone);
        tmpObject = this.markNeighborAtomsAndBonds((IAtom[]) tmpObject[0], (IBond[]) tmpObject[1]);
        tmpObject = this.markRings(tmpClone, (IAtom[]) tmpObject[0], (IBond[]) tmpObject[1]);
        tmpObject = this.markConjugatedPiSystems(tmpClone, (IAtom[]) tmpObject[0], (IBond[]) tmpObject[1]);
        tmpObject = this.markMultiBonds((IAtom[]) tmpObject[0], (IBond[]) tmpObject[1]);
        //</editor-fold>
        //
        //<editor-fold desc="Fragment Extraction and Saturation" defaultstate="collapsed">
        try {
            int tmpPostFragmentationAtomCount = 0;
            IAtomContainerSet tmpFragmentSet = this.extractFragments((IAtom[]) tmpObject[0],(IBond[]) tmpObject[1]);
            for (IAtomContainer tmpAtomContainer: tmpFragmentSet.atomContainers()) {
                for (IAtom tmpAtom: tmpAtomContainer.atoms()) {
                    if (tmpAtom.getAtomicNumber() != 0)
                    tmpPostFragmentationAtomCount++;
                }
            }
            AlkylStructureFragmenter.this.logger.log(Level.INFO, "PostFragAtomCount: " + tmpPostFragmentationAtomCount);
            if (tmpPostFragmentationAtomCount != tmpPreFragmentationAtomCount && !this.ASFDebugBoolean) {
                throw new Exception("Molecular formula is not the same between original molecule and received fragments!");
            }
            if (this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION)) {
                return this.saturateWithImplicitHydrogen(tmpFragmentSet);
            }
            ArrayList<IAtomContainer> tmpFragmentList = new ArrayList<>(tmpFragmentSet.getAtomContainerCount());
            for (IAtomContainer tmpAtomContainer: tmpFragmentSet.atomContainers()) {
                tmpFragmentList.add(tmpAtomContainer);
            }
            return tmpFragmentList;
        } catch (Exception anException) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING,
                    anException + " extraction or saturation failed at molecule: " + tmpClone.getID(), anException);
            throw new IllegalArgumentException("Unexpected error occurred during fragmentation of molecule: "
                    + tmpClone.getID() + ", at fragment extraction: " + anException.toString());
        }
        //</editor-fold>
        //
    }


    //</editor-fold>
    //
    //</editor-fold>
    //
    //<editor-fold desc="Protected Methods" defaultstate="collapsed">

    /**
     * Method to fill an IAtom array with the atoms of the input IAtomContainer
     * and place fragmentation properties on them.
     *
     * IMPORTANT:
     * All used properties have to be set in this step ONCE (independent of set values) in order for correct
     * fragmenter function, especially tertiary and quaternary carbon detection.
     *
     * @param aClone IAtomContainer, best a clone, from which atoms are put into array
     * @return IAtom array containing the atoms
     */
    protected IAtom[] fillAtomArray(IAtomContainer aClone) {
        IAtom[] tmpAtomArray = new IAtom[aClone.getAtomCount()];
        int tmpAlkylSFAtomIndex;
        for (tmpAlkylSFAtomIndex = 0; tmpAlkylSFAtomIndex < aClone.getAtomCount(); tmpAlkylSFAtomIndex++) {
            IAtom tmpAtom = aClone.getAtom(tmpAlkylSFAtomIndex);
            ArrayList<Integer> tmpRingAtomList = new ArrayList<>();
            if (tmpAtom != null) {
                /*
                set atom properties, IMPORTANT: this needs to be done in array filling step for correct function of
                tertiary or quaternary carbon atoms and neighbors
                 */
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY, tmpAlkylSFAtomIndex);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY, false);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, false);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY, false);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY, false);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY, false);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY, false);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, false);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_ATOM_LIST_KEY, tmpRingAtomList);
                tmpAtomArray[tmpAlkylSFAtomIndex] = tmpAtom;
            }
        }
        ArrayList<IAtom> tmpRemovedNull = new ArrayList<IAtom>();
        for (IAtom tmpAtom: tmpAtomArray)
            if (tmpAtom != null)
                tmpRemovedNull.add(tmpAtom);
        return tmpRemovedNull.toArray(new IAtom[0]);
        //return tmpAtomArray;
    }
    /**
     * Method to fill an IBond array with the bonds of the input IAtomContainer
     * and place fragmentation properties on them.
     *
     * @param aClone IAtomContainer, best a clone, from which bonds are put into array
     * @return IBond array containing the bonds
     */
    protected IBond[] fillBondArray(IAtomContainer aClone) {
        IBond[] tmpBondArray = new IBond[aClone.getBondCount()];
        int tmpAlkylSFBondIndex;
        for (tmpAlkylSFBondIndex = 0; tmpAlkylSFBondIndex < aClone.getBondCount(); tmpAlkylSFBondIndex++) {
            IBond tmpBond = aClone.getBond(tmpAlkylSFBondIndex);
            if (tmpBond != null) {
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY, tmpAlkylSFBondIndex);
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY, false);
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY, false);
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY, false);
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY, false);
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, false);
                tmpBondArray[tmpAlkylSFBondIndex] = tmpBond;
            }
        }
        return tmpBondArray;
    }

    /**
     * Protected method for detecting and marking tertiary or quaternary carbon atoms and their surrounding neighbor atoms and bonds.
     *
     * @param anAtomArray Given array with atoms of molecule to be fragmented
     * @param aBondArray Given array with bonds of molecule to be fragmented
     * @return Object[] containing manipulated atom and bond arrays for easier data transfer
     */
    protected Object[] markNeighborAtomsAndBonds(IAtom[] anAtomArray, IBond[] aBondArray) {
        Objects.requireNonNull(anAtomArray, "Given atom array is null.");
        Objects.requireNonNull(aBondArray,"Given bond array is null");
        //set general atom and specific bond properties
        for (int tmpAtomIndex = 0; tmpAtomIndex < anAtomArray.length; tmpAtomIndex++) {
            IAtom tmpAtom = anAtomArray[tmpAtomIndex];
            //System.out.println("markNeighbor Index: " + tmpAtomIndex);
            if (tmpAtom != null) {
                //marking of tertiary or quaternary carbons together with their neighbor atoms
                if (tmpAtom.getBondCount() == 3 && tmpAtom.getMaxBondOrder() == IBond.Order.SINGLE) {
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY, true);
                    //may not work as no guarantee for proper atom or bond property set -> may have already been iterated over
                    for (IBond tmpBond: tmpAtom.bonds()) {
                        int tmpBondIndex = tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY);
                        int tmpBondBeginIndex = tmpBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                        int tmpBondEndIndex = tmpBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                        //System.out.println("bondIndex: " + tmpBondIndex + ", beginIndex: " + tmpBondBeginIndex + ", endIndex: " + tmpBondEndIndex);
                        if (tmpBond.getBegin() == tmpAtom) {
                            anAtomArray[tmpBondEndIndex].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                            aBondArray[tmpBondIndex].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                            //System.out.println("neighbor atom end set");
                        } else if (tmpBond.getEnd() == tmpAtom) {
                            anAtomArray[tmpBondBeginIndex].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                            aBondArray[tmpBondIndex].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                            //System.out.println("neighbor atom begin set");
                        }
                        //System.out.println("atomStatus Begin: " + anAtomArray[tmpBondBeginIndex].getProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY) + ", atomBondCount: " + anAtomArray[tmpBondBeginIndex].getBondCount());
                        //System.out.println("atomStatus End: " + anAtomArray[tmpBondEndIndex].getProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY) + ", atomBondCount: " + anAtomArray[tmpBondEndIndex].getBondCount());
                        //System.out.println("bondStatus: " + aBondArray[tmpBondIndex].getProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY));
                    }
                } else if (tmpAtom.getBondCount() == 4 && tmpAtom.getMaxBondOrder() == IBond.Order.SINGLE) {
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY, true);
                    //may also not work
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
        //set general bond properties
        for (int tmpBondIndex = 0; tmpBondIndex < aBondArray.length; tmpBondIndex++) {
            IBond tmpBond = aBondArray[tmpBondIndex];
            if (tmpBond != null) {

                boolean tmpIsBeginTertiary = tmpBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY);
                boolean tmpIsEndTertiary = tmpBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY);
                boolean tmpIsBeginQuaternary = tmpBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY);
                boolean tmpIsEndQuaternary = tmpBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY);
                if (tmpIsBeginTertiary || tmpIsEndTertiary || tmpIsBeginQuaternary || tmpIsEndQuaternary) {
                    tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                }
                aBondArray[tmpBondIndex] = tmpBond;
            }
        }
        Object[] tmpObject = new Object[2];
        tmpObject[0] = anAtomArray;
        tmpObject[1] = aBondArray;
        return tmpObject;
    }
    /**
     * Protected method to mark all atoms and bonds of any rings in the given atomcontainer.
     *
     * @param anAtomContainer IAtomContainer to mark atoms and bonds in
     * @param anAtomArray containing atoms to be marked
     * @param aBondArray containing bonds to be marked
     * @return Object containing both input arrays
     */
    protected Object[] markRings(IAtomContainer anAtomContainer, IAtom[] anAtomArray, IBond[] aBondArray) throws IllegalArgumentException {
        //
        //<editor-fold desc="Ring System Detection" defaultstate="collapsed">
        Objects.requireNonNull(anAtomArray);
        Objects.requireNonNull(aBondArray);
        RingSearch tmpRingSearch = new RingSearch(anAtomContainer);
        try {
            List<IAtomContainer> tmpFusedList = tmpRingSearch.fusedRingFragments();
            if (!tmpFusedList.isEmpty()) {
                //think of way to incorporate ring index
                int[] tmpRingIndexArray = new int[tmpFusedList.size()];
                for (int tmpFusedCount = 0; tmpFusedCount < tmpFusedList.size(); tmpFusedCount++) {
                    for (IAtom tmpFusedAtom: tmpFusedList.get(tmpFusedCount).atoms()) {
                        int tmpAtomInteger = tmpFusedAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                        IAtom tmpAtom = anAtomArray[tmpAtomInteger];
                        tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY, true);
                        //currently ring index gets overwritten during marking -> use tmpRingIndexArray instead where ring affection is stored (possibly multiple)
                        tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FUSED_RING_INDEX_PROPERTY_KEY, tmpFusedCount);
                    }
                    for (IBond tmpFusedBond: tmpFusedList.get(tmpFusedCount).bonds()) {
                        int tmpBondInteger = tmpFusedBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY);
                        IBond tmpBond = aBondArray[tmpBondInteger];
                        tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY, true);
                        tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FUSED_RING_INDEX_PROPERTY_KEY, tmpFusedCount);
                    }
                }
            }
        } catch (Exception anException) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING, anException.toString() + " MoleculeID: " + anAtomContainer.getID(), anException);
            throw new IllegalArgumentException("Unexpected error occurred during fragmentation of molecule: "
                    + anAtomContainer.getID() + ", at fused ringsearch: " + anException.toString());
        }
        //</editor-fold>
        //
        //<editor-fold desc="Single Ring Detection" defaultstate="collapsed">
        if (this.mcbSingleRingDetectionSetting.get()) {
            //<editor-fold desc="CycleFinder (MCB)">
            CycleFinder tmpMCBCycleFinder = Cycles.mcb();
            IRingSet tmpMCBCyclesSet;
            try {
                Cycles tmpMCBCycles = tmpMCBCycleFinder.find(anAtomContainer);
                tmpMCBCyclesSet = tmpMCBCycles.toRingSet();
                int tmpSingleRingCount = 0;
                for (IAtomContainer tmpContainer: tmpMCBCyclesSet.atomContainers()) {
                    for (IAtom tmpSingleRingAtom: tmpContainer.atoms()) {
                        int tmpAtomInteger = tmpSingleRingAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                        IAtom tmpAtom = anAtomArray[tmpAtomInteger];
                        tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY, true);
                        tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_ISOLATED_RING_INDEX_PROPERTY_KEY, tmpSingleRingCount);
                    }
                    for (IBond tmpSingleRingBond: tmpContainer.bonds()) {
                        int tmpBondInteger = tmpSingleRingBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY);
                        IBond tmpBond = aBondArray[tmpBondInteger];
                        tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY, true);
                        tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_ISOLATED_RING_INDEX_PROPERTY_KEY, tmpSingleRingCount);
                    }
                    tmpSingleRingCount++;
                }
            } catch (Exception anException) {
                AlkylStructureFragmenter.this.logger.log(Level.WARNING,
                        anException + " MoleculeID: " + anAtomContainer.getID(), anException);
                throw new IllegalArgumentException("Unexpected error occurred during fragmentation of molecule: "
                        + anAtomContainer.getID() + ", at cyclefinder : " + anException.toString());
            }
            //</editor-fold>
        } else {
            //<editor-fold desc="RingSearch (isolated)">
            //for spiro detection: set array property for each atom belonging to x rings
            //--> if more than one ring --> check for connected atoms to determine if spiro config
            try {
                List<IAtomContainer> tmpIsolatedRingList = tmpRingSearch.isolatedRingFragments();
                //System.out.println("isolated rings: " + tmpIsolatedRingList.size());
                if (!tmpIsolatedRingList.isEmpty()) {
                    for (int tmpIsolatedCount = 0; tmpIsolatedCount < tmpIsolatedRingList.size(); tmpIsolatedCount++) {
                        for (IAtom tmpIsolatedAtom: tmpIsolatedRingList.get(tmpIsolatedCount).atoms()) {
                            int tmpAtomInteger = tmpIsolatedAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                            IAtom tmpAtom = anAtomArray[tmpAtomInteger];
                            tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY, true);
                            ArrayList<Integer> tmpList = tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_ATOM_LIST_KEY);
                            tmpList.add(tmpIsolatedCount);
                            tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_ATOM_LIST_KEY, tmpList);
                            tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_ISOLATED_RING_INDEX_PROPERTY_KEY, tmpIsolatedCount);
                        }
                        for (IBond tmpFusedBond: tmpIsolatedRingList.get(tmpIsolatedCount).bonds()) {
                            int tmpBondInteger = tmpFusedBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY);
                            IBond tmpBond = aBondArray[tmpBondInteger];
                            tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY, true);
                            tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_ISOLATED_RING_INDEX_PROPERTY_KEY, tmpIsolatedCount);
                        }
                    }
                }
            } catch (Exception anException) {
                AlkylStructureFragmenter.this.logger.log(Level.WARNING,
                        anException + " MoleculeID: " + anAtomContainer.getID(), anException);
                throw new IllegalArgumentException("Unexpected error occurred during fragmentation of molecule: "
                        + anAtomContainer.getID() + ", at isolated RingSearch : " + anException.toString());
            }
        //</editor-fold>
        }
        //returns object containing atoms array and bonds array
        Object[] tmpObject = new Object[2];
        tmpObject[0] = anAtomArray;
        tmpObject[1] = aBondArray;
        return tmpObject;
        //</editor-fold>
        //
    }

    /**
     * Protected method to mark all atoms and bonds of any conjugated pi systems in the given atomcontainer.
     *
     * @param anAtomContainer IAtomContainer to mark atoms and bonds in
     * @param anAtomArray Array containing the atoms of a given fragmentation molecule
     * @param aBondArray Array containing the bonds of a given fragmentation molecule
     *
     * @return new {@link java.lang.Object} for easy array transfer, containing the atom and bond array
     */
    //test performance if pi system detection should be relocated to standard ring detection
    protected Object[] markConjugatedPiSystems(IAtomContainer anAtomContainer, IAtom[] anAtomArray, IBond[] aBondArray) throws IllegalArgumentException{
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
            return new Object[] {anAtomArray, aBondArray};
        } catch (Exception anException) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING,
                    anException + " MoleculeID: " + anAtomContainer.getID(), anException);
            throw new IllegalArgumentException("Unexpected error occurred during fragmentation of molecule: "
                    + anAtomContainer.getID() + " at conjugated pi systems detector: " + anException.toString());
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
            AlkylStructureFragmenter.this.logger.log(Level.INFO, System.currentTimeMillis() + " start sD, AC size: " + anAtomContainer.getAtomCount() + ", " + anAtomContainer.getBondCount());
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
            AlkylStructureFragmenter.this.logger.log(Level.WARNING, anException + " Connectivity Checking failed at molecule: " + anAtomContainer.getID(), anException);
            //logger only used for debug purpose
            AlkylStructureFragmenter.this.logger.log(Level.INFO, System.currentTimeMillis() + " start sD, AC size: " + anAtomContainer.getAtomCount() + ", " + anAtomContainer.getBondCount());
            throw new IllegalArgumentException("An Error occurred during Connectivity Checking: " + anException.toString() +
                    ": " + anAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
        }
    }

    /**
     * Protected method to saturate a given molecule with implicit hydrogens after fragmentation.
     *
     * @param anUnsaturatedACSet IAtomContainerSet whose atomcontainers are to be saturated
     * @return List of processed atomcontainers, @null if given Set is empty
     * @throws CDKException if CDKHydrogenAdder throws an exception
     */
    protected List<IAtomContainer> saturateWithImplicitHydrogen(IAtomContainerSet anUnsaturatedACSet) throws CDKException {
        Objects.requireNonNull(anUnsaturatedACSet, "Given IAtomContainerSet is null.");
        try {
            List<IAtomContainer> tmpSaturatedFragments = new ArrayList<>(anUnsaturatedACSet.getAtomContainerCount());
            if (!anUnsaturatedACSet.isEmpty() && !anUnsaturatedACSet.getAtomContainer(0).isEmpty()) {
                for (IAtomContainer tmpAtomContainer: anUnsaturatedACSet.atomContainers()) {
                    if (tmpAtomContainer != null && !tmpAtomContainer.isEmpty()) {
                        ChemUtil.saturateWithHydrogen(tmpAtomContainer);
                        tmpSaturatedFragments.add(tmpAtomContainer);
                    }
                }
            }
            return tmpSaturatedFragments;
        } catch (CDKException anException) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING, anException
                + " Unable to add Implicit Hydrogen", anException);
            throw new CDKException("Unexpected error occurred during implicit hydrogen adding at " +
                "hydrogen saturation of molecule, " + anException.toString(), anException);
        }
    }

    /**
     * Protected method to extract detected molecules via properties.
     *
     * @param anAtomArray Array containing the atoms of a given fragmentation molecule
     * @param aBondArray Array containing the bonds of a given fragmentation molecule
     * @return IAtomContainerSet with extracted molecules
     * @throws CloneNotSupportedException if input cannot be cloned
     */
    protected IAtomContainerSet extractFragments(IAtom[] anAtomArray, IBond[] aBondArray) throws CloneNotSupportedException, IllegalArgumentException {
        //
        Objects.requireNonNull(anAtomArray);
        Objects.requireNonNull(aBondArray);
        //<editor-fold desc="Extraction">
        IAtomContainerSet tmpExtractionSet = new AtomContainerSet();
        IAtomContainer tmpRingFragmentationContainer = this.chemObjectBuilderInstance.newAtomContainer();
        IAtomContainer tmpChainFragmentationContainer = this.chemObjectBuilderInstance.newAtomContainer();
        IAtomContainer tmpIsolatedMultiBondsContainer = this.chemObjectBuilderInstance.newAtomContainer();
        IAtomContainer tmpTertQuatCarbonContainer = this.chemObjectBuilderInstance.newAtomContainer();
        IAtomContainer tmpSpiroCarbonContainer = this.chemObjectBuilderInstance.newAtomContainer();
        //
        //<editor-fold desc="atom extraction">
        for (IAtom tmpAtom : anAtomArray) {
            //checks atom if not part of ring or conjugated pi system
            if (!((boolean) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY)
                    || (boolean) tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_CONJ_PI_MARKER_KEY))) {
                if (tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY)) {
                    tmpRingFragmentationContainer.addAtom(this.deepCopyAtom(tmpAtom));
                    //System.out.println("tert atom added");
                    if (this.altHandlingSingleTertQuatCarbonsSetting.get()) {
                        for (int i = 0; i < 3; i++) {
                            PseudoAtom tmpPseudoAtom = new PseudoAtom();
                            tmpTertQuatCarbonContainer.addAtom(tmpPseudoAtom);
                            tmpTertQuatCarbonContainer.addBond(new Bond(tmpAtom, tmpPseudoAtom));
                        }
                    }
                } else if (tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY)) {
                    tmpRingFragmentationContainer.addAtom(this.deepCopyAtom(tmpAtom));
                    //System.out.println("quart atom added");
                    if (this.altHandlingSingleTertQuatCarbonsSetting.get()) {
                        for (int i = 0; i < 4; i++) {
                            PseudoAtom tmpPseudoAtom = new PseudoAtom();
                            tmpTertQuatCarbonContainer.addAtom(tmpPseudoAtom);
                            tmpTertQuatCarbonContainer.addBond(new Bond(tmpAtom, tmpPseudoAtom));
                        }
                    }
                } //extract atoms for double/triple bonds
                else if (tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY)) {
                    for (IAtom tmpArrayAtom: anAtomArray) {
                        if (tmpArrayAtom != tmpAtom) {
                            IBond tmpDoubleToRingBond;
                            try {
                                tmpDoubleToRingBond = tmpAtom.getBond(tmpArrayAtom);
                                Objects.requireNonNull(tmpDoubleToRingBond);
                            } catch (Exception e) {
                                continue;
                            }
                            boolean tmpBegin = tmpDoubleToRingBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY);
                            boolean tmpEnd = tmpDoubleToRingBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY);
                            if (tmpBegin || tmpEnd) {
                                //WIP
                                tmpRingFragmentationContainer.addAtom(this.deepCopyAtom(tmpAtom));
                                //System.out.println("double bond to ring atom added");
                            } else {
                                tmpIsolatedMultiBondsContainer.addAtom(this.deepCopyAtom(tmpAtom));
                                //System.out.println("double bond atom added");
                            }
                        }
                    }
                } else if (tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY)) {
                    tmpIsolatedMultiBondsContainer.addAtom(this.deepCopyAtom(tmpAtom));
                    //System.out.println("triple bond atom added");
                }
                //extract neighbor atoms of tertiary or quaternary carbon atoms
                else if (tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY)) {
                    if (this.altHandlingSingleTertQuatCarbonsSetting.get()) {continue;}
                    tmpRingFragmentationContainer.addAtom(this.deepCopyAtom(tmpAtom));
                    //System.out.println("neighbor atom added");
                }
                //extract residue atoms as linear chain atoms
                else {
                    tmpChainFragmentationContainer.addAtom(this.deepCopyAtom(tmpAtom));
                    //System.out.println("chain residue atom added");
                }
            } else {
                if (!this.keepRingsSetting.get()) {
                    ArrayList<Integer> tmpList = tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_ATOM_LIST_KEY);
                    if (tmpList.size() > 1) {
                        tmpSpiroCarbonContainer.addAtom(this.deepCopyAtom(tmpAtom));
                    } /*
                    else {
                        tmpSpiroResidueContainer.addAtom(tmpAtom);
                    }
                    */
                    tmpRingFragmentationContainer.addAtom(this.deepCopyAtom(tmpAtom));
                } else {
                    tmpRingFragmentationContainer.addAtom(this.deepCopyAtom(tmpAtom));
                    //System.out.println("ring atom list: " + tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_ATOM_LIST_KEY));
                    //System.out.println("ring atom added");
                }
            }
        }
        //</editor-fold>
        //
        //<editor-fold desc="bond extraction">
        for (IBond tmpBond : aBondArray) {
            IAtom tmpBeginAtom = tmpBond.getBegin();
            IAtom tmpEndAtom = tmpBond.getEnd();
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
                if (tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY)) {
                    //WIP
                    boolean tmpBegin = tmpBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY);
                    boolean tmpEnd = tmpBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_MARKER_KEY);
                    if (tmpBegin || tmpEnd) {
                        tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpRingFragmentationContainer));
                        //System.out.println("double bond to ring added");
                    } else {
                        tmpIsolatedMultiBondsContainer.addBond(this.deepCopyBond(tmpBond, tmpIsolatedMultiBondsContainer));
                        //System.out.println("double bond added");
                    }
                }
                else if ((boolean) tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY)) {
                    tmpIsolatedMultiBondsContainer.addBond(this.deepCopyBond(tmpBond, tmpIsolatedMultiBondsContainer));
                    //System.out.println("triple bond added");
                }
                else if ((boolean) tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY)
                        && !((tmpIsBeginRing && tmpIsBeginTertiary) || (tmpIsEndRing && tmpIsEndTertiary)
                            || (tmpIsBeginRing && tmpIsBeginQuaternary) || (tmpIsEndRing && tmpIsEndQuaternary))) {
                    tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpRingFragmentationContainer));
                    //System.out.println("neighbor bond added");
                }
                if (!(tmpIsBeginRing && tmpIsEndRing && tmpIsBeginConjPi && tmpIsEndConjPi)
                        && !(tmpIsBeginDouble || tmpIsEndDouble || tmpIsBeginTriple || tmpIsEndTriple)) {
                    if (!(tmpIsBeginTertiary || tmpIsEndTertiary || tmpIsBeginQuaternary || tmpIsEndQuaternary || tmpIsBeginNeighbor || tmpIsEndNeighbor)) {
                        tmpChainFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpChainFragmentationContainer));
                        //System.out.println("chain residue bond added");
                    }
                }
            }
            else {
                if (!this.keepRingsSetting.get()) {
                    //check if begin or end of bond is spiro configuration carbon
                    boolean tmpIsBeginSpiro = false;
                    boolean tmpIsEndSpiro = false;
                    ArrayList<Integer> tmpBeginList = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_ATOM_LIST_KEY);
                    if (tmpBeginList.size() > 1) {
                        tmpIsBeginSpiro = true;
                    }
                    ArrayList<Integer> tmpEndList = tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_RING_ATOM_LIST_KEY);
                    if (tmpEndList.size() > 1) {
                        tmpIsEndSpiro = true;
                    }
                    if (!tmpIsBeginSpiro && !tmpIsEndSpiro) {
                        tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpRingFragmentationContainer));
                    }
                } else {
                    tmpRingFragmentationContainer.addBond(this.deepCopyBond(tmpBond, tmpRingFragmentationContainer));
                    //System.out.println("ring bond added");
                }
            }
        }
        //</editor-fold>
        //</editor-fold>
        //
        IAtomContainerSet tmpRingACSet = new AtomContainerSet();
        if (!tmpRingFragmentationContainer.isEmpty()) {
            tmpRingACSet = this.separateDisconnectedStructures(tmpRingFragmentationContainer);
        }
        IAtomContainerSet tmpSingleACSet = new AtomContainerSet();
        if (!tmpTertQuatCarbonContainer.isEmpty()) {
            tmpExtractionSet.add(this.separateDisconnectedStructures(tmpTertQuatCarbonContainer));
        }
        if (!tmpRingACSet.isEmpty() && tmpRingACSet.getAtomContainerCount() > 0) {
            tmpExtractionSet.add(tmpRingACSet);
        }
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
            AlkylStructureFragmenter.this.logger.log(Level.WARNING, "Illegal restriction argument", new IllegalArgumentException());
            //tmpMaxChainLengthInteger = AlkylStructureFragmenter.MAX_CHAIN_LENGTH_SETTING_DEFAULT;
        }
        //checks for applied restrictions, default restriction is set to 6
        if (this.fragmentSideChainsSetting.get() && tmpMaxChainLengthInteger > 0) {
            //check maxchainlength
            switch (tmpMaxChainLengthInteger) {
                default -> {
                    //restrictions > 1
                    for (IAtomContainer tmpAtomContainer : tmpChainACSet.atomContainers()) {
                        tmpDissectedChainACSet.add(this.separateDisconnectedStructures(this.dissectLinearChain(tmpAtomContainer,
                                tmpMaxChainLengthInteger)));
                    }
                }
                case 1 -> {
                    //single methane molecules
                    IAtomContainer tmpDissectedAC = new AtomContainer();
                    for (IAtomContainer tmpAtomContainer : tmpChainACSet.atomContainers()) {
                        tmpAtomContainer.removeAllBonds();
                        tmpDissectedAC.add(tmpAtomContainer);
                    }
                    tmpDissectedChainACSet.add(this.separateDisconnectedStructures(tmpDissectedAC));
                }
            }
        } else {
            //no restrictions applied
            tmpDissectedChainACSet.add(tmpChainACSet);
        }
        if (!tmpDissectedChainACSet.isEmpty() && tmpDissectedChainACSet.getAtomContainerCount() > 0) {
            tmpExtractionSet.add(tmpDissectedChainACSet);
        }
        /* for Debug purpose
        List<Integer> tmpAtomIndexList = new ArrayList<>();
        for (IAtomContainer tmpAC: tmpExtractionSet.atomContainers()) {
            for (IAtom tmpA: tmpAC.atoms()) {
                int tmpAtomIndex = tmpA.getProperty(INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                if (!tmpAtomIndexList.contains(tmpAtomIndex))
                    tmpAtomIndexList.add(tmpAtomIndex, tmpAtomIndex);
                else {
                    System.out.println("doppeltes Atom: " + tmpAtomIndex + ", CDK-Index: " + tmpA.getIndex());
                }
            }
        }
        */
        return tmpExtractionSet;
    }

    /**
     * Protected method to dissect given AtomContainer (containing linear carbon chain) into separate molecules with given length and remnants if
     * molecule is too small for given length.
     *
     * @param anAC AtomContainer to be dissected
     * @param aLength Given maximum length of molecule
     * @return AtomContainer with separate dissected molecules
     */
    protected IAtomContainer dissectLinearChain(IAtomContainer anAC, int aLength) {
        IAtomContainer tmpReturnAC = new AtomContainer();
        //starts at 1 for usability, see aLength: 1on1 translation of input to counter
        int tmpCounter = 1;
        Iterator<IBond> tmpBondIterator = anAC.bonds().iterator();
        while (tmpBondIterator.hasNext()) {
            IBond tmpBond = tmpBondIterator.next();
            if (tmpCounter == aLength) {
                if (!tmpBondIterator.hasNext()) {
                    tmpReturnAC.addAtom(this.deepCopyAtom(tmpBond.getEnd()));
                }
                tmpCounter = 1;
            } else {
                IAtom tmpBeginAtom = tmpBond.getBegin();
                IAtom tmpEndAtom = tmpBond.getEnd();
                tmpReturnAC.addAtom(this.deepCopyAtom(tmpBeginAtom));
                tmpReturnAC.addAtom(this.deepCopyAtom(tmpEndAtom));
                tmpReturnAC.addBond(this.deepCopyBond(tmpBond, tmpReturnAC));
                tmpCounter++;
            }
        }
        return tmpReturnAC;
    }

    /**
     * Protected method to mark atoms and bonds with order of double or triple.
     *
     * @param anAtomArray Atom array providing the atoms of a molecule
     * @param aBondArray Bond array providing the bonds of a molecule
     * @return new Object containing both given arrays after manipulation of method for easier data transfer
     */
    protected Object[] markMultiBonds(IAtom[] anAtomArray, IBond[] aBondArray) {
        Objects.requireNonNull(anAtomArray);
        Objects.requireNonNull(aBondArray);
        for (IBond tmpArrayBond: aBondArray) {
            if (tmpArrayBond.getOrder().numeric() == 2) {
                tmpArrayBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY, true);
                int tmpBeginIndex = tmpArrayBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                int tmpEndIndex = tmpArrayBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                for (IAtom tmpAtom: anAtomArray) {
                    int tmpAtomIndex = tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                    if (tmpAtomIndex == tmpBeginIndex) {
                        tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY, true);
                    } else if (tmpAtomIndex == tmpEndIndex) {
                        tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY, true);
                    }
                }
            } else if (tmpArrayBond.getOrder().numeric() == 3) {
                tmpArrayBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY, true);
                int tmpBeginIndex = tmpArrayBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                int tmpEndIndex = tmpArrayBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                for (IAtom tmpAtom: anAtomArray) {
                    int tmpAtomIndex = tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                    if ( tmpAtomIndex == tmpBeginIndex) {
                        tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY, true);
                    } else if (tmpAtomIndex == tmpEndIndex) {
                        tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY, true);
                    }
                }
            }
        }
        Object[] tmpObject = new Object[2];
        tmpObject[0] = anAtomArray;
        tmpObject[1] = aBondArray;
        return tmpObject;
    }
    //WIP

    /**
     * Method to create a deeper copy of a given atom, meant to replace the default addAtom() method of IAtomContainer
     * in which only the reference is added to the atomcontainer instead of a new atom.
     *
     * @param anAtomToCopy the atom to create a deep copy of
     * @return the deep copy of the given atom
     */
    protected IAtom deepCopyAtom(IAtom anAtomToCopy) {
        IAtom tmpNewAtom = this.chemObjectBuilderInstance.newAtom();
        tmpNewAtom.setAtomicNumber(anAtomToCopy.getAtomicNumber());
        tmpNewAtom.setImplicitHydrogenCount(anAtomToCopy.getImplicitHydrogenCount());
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
        int tmpBeginAtomIndex = aBondToCopy.getBegin().getProperty(INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
        //System.out.println("begin " + aBondToCopy.getBegin().getIndex());
        int tmpEndAtomIndex = aBondToCopy.getEnd().getProperty(INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
        for (IAtom tmpAtom: aBondIncludingAtomContainer.atoms()) {
            if ((int) tmpAtom.getProperty(INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY) == tmpBeginAtomIndex) {
                tmpNewBond.setAtom(tmpAtom, 0);
                //aBondIncludingAtomContainer.removeAtom(tmpBeginAtomIndex);
            } else if ((int) tmpAtom.getProperty(INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY) == tmpEndAtomIndex) {
                tmpNewBond.setAtom(tmpAtom, 1);
                //aBondIncludingAtomContainer.removeAtom(tmpEndAtomIndex);
            }
        }
        return tmpNewBond;
    }
    //</editor-fold>
    //
}
