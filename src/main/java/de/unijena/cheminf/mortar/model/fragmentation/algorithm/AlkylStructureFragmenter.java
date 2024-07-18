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

//<editor-fold desc="Future Development">
/*
TODO:
 */
//</editor-fold>

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
     * Default value for maximum length of carbon side chains.
     */
    public static final int MAX_CHAIN_LENGTH_SETTING_DEFAULT = 0;
    /**
     * Default boolean value for determination of further side chain dissection.
     */
    public static final boolean FRAGMENT_SIDE_CHAINS_SETTING_DEFAULT = false;
    /**
     * Default boolean value for determining whether alternative or standard single carbon handling should be used.
     */
    public static final boolean ALTERNATIVE_SINGLE_CARBON_HANDLING_SETTING_DEFAULT = false;
    /**
     * Default boolean value for determining which single ring detection method should be used.
     */
    public static final boolean ALTERNATIVE_SINGLE_RING_DETECTION_SETTING_DEFAULT = false;
    /**
     * Default boolean value for setting keeping rings intact.
     */
    public static final boolean KEEP_RINGS_SETTING_DEFAULT = true;
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
     * Key for an internal boolean property used in identifying whether an atom or bond has been placed in the
     * corresponding fragmentation array. It is further used during fragmentation.
     */
    public static final String INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY = "ASF.FRAGMENTATION_PLACEMENT";
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
    public static final String INTERNAL_ASF_NEIGHBOR_MARKER_KEY = "ASF_NEIGHBOR_MARKER";
    //</editor-fold>
    //</editor-fold>
    //
    //<editor-fold desc="Private Class Variables">
    /**
     * A property that has a constant fragment hydrogen saturation setting.
     */
    private final SimpleIDisplayEnumConstantProperty fragmentSaturationSetting;
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
    private final SimpleBooleanProperty alternativeSingleCarbonHandlingSetting;
    /**
     * A property that has a constant boolean value determining which single ring detection method to use during fragmentation.
     */
    private final SimpleBooleanProperty alternativeSingleRingDetectionSetting;
    /**
     * A property that has a constant boolean value defining if rings should be dissected further.
     */
    private final SimpleBooleanProperty keepRingsSetting;
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

    //</editor-fold>
    //
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
        this.alternativeSingleCarbonHandlingSetting = new SimpleBooleanProperty(this, "Single carbon handling setting",
                AlkylStructureFragmenter.ALTERNATIVE_SINGLE_CARBON_HANDLING_SETTING_DEFAULT);
        this.settingNameTooltipTextMap.put(this.alternativeSingleCarbonHandlingSetting.getName(),
                Message.get("AlkylStructureFragmenter.alternativeSingleCarbonHandlingSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.alternativeSingleCarbonHandlingSetting.getName(),
                Message.get("AlkylStructureFragmenter.alternativeSingleCarbonHandlingSetting.displayName"));
        this.alternativeSingleRingDetectionSetting = new SimpleBooleanProperty(this, "Single ring detection setting",
                AlkylStructureFragmenter.ALTERNATIVE_SINGLE_RING_DETECTION_SETTING_DEFAULT);
        this.settingNameTooltipTextMap.put(this.alternativeSingleRingDetectionSetting.getName(),
                Message.get("AlkylStructureFragmenter.alternativeSingleRingDetectionSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.alternativeSingleRingDetectionSetting.getName(),
                Message.get("AlkylStructureFragmenter.alternativeSingleRingDetectionSetting.displayName"));
        this.keepRingsSetting = new SimpleBooleanProperty(this, "Keep rings setting",
                AlkylStructureFragmenter.KEEP_RINGS_SETTING_DEFAULT);
        this.settingNameTooltipTextMap.put(this.keepRingsSetting.getName(),
                Message.get("AlkylStructureFragmenter.keepRingsSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.keepRingsSetting.getName(),
                Message.get("AlkylStructureFragmenter.keepRingsSetting.displayName"));
        this.settings = new ArrayList<>(6);
        this.settings.add(this.fragmentSaturationSetting);
        this.settings.add(this.fragmentSideChainsSetting);
        this.settings.add(this.maxChainLengthSetting);
        this.settings.add(this.alternativeSingleCarbonHandlingSetting);
        this.settings.add(this.alternativeSingleRingDetectionSetting);
        this.settings.add(this.keepRingsSetting);
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
        return settingNameTooltipTextMap;
    }

    /**
     * Returns a map containing language-specific names (values) for the settings with the given names (keys) to be used
     * in the GUI.
     *
     * @return map with display names
     */
    @Override
    public Map<String, String> getSettingNameToDisplayNameMap() {
        return this.settingNameDisplayNameMap;
    }

    @Override
    public String getFragmentationAlgorithmName() {
        return AlkylStructureFragmenter.ALGORITHM_NAME;
    }

    /**
     * Returns a language-specific name of the fragmenter to be used in the GUI.
     * The given name must be unique among the available fragmentation algorithms!
     *
     * @return language-specific name for display in GUI
     */
    @Override
    public String getFragmentationAlgorithmDisplayName() {
        return Message.get("AlkylStructureFragmenter.displayName");
    }

    @Override
    public FragmentSaturationOption getFragmentSaturationSetting() {
        return (IMoleculeFragmenter.FragmentSaturationOption) this.fragmentSaturationSetting.get();
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
     * Public get method for alternative single carbon handling setting property.
     *
     * @return SimpleBooleanProperty alternativeSingleCarbonHandlingSetting
     */
    public SimpleBooleanProperty getAlternativeSingleCarbonHandlingSettingProperty() {return this.alternativeSingleCarbonHandlingSetting;}

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
    public void setAlternativeSingleCarbonHandlingSetting(boolean aBoolean){
        Objects.requireNonNull(aBoolean, "Given boolean is null.");
        this.alternativeSingleCarbonHandlingSetting.set(aBoolean);
    }
    /**
     * Set method for setting defining whether alternative single ring detection should be used.
     *
     * @param aBoolean the given boolean value for switching between alternative and default detection
     */
    public void setAlternativeSingleRingDetectionSetting(boolean aBoolean) {
        Objects.requireNonNull(aBoolean, "Given boolean is null");
        this.alternativeSingleRingDetectionSetting.set(aBoolean);
    }
    /**
     * Set method for setting defining if rings should be dissected or kept intact.
     *
     * @param aBoolean the given boolean value for switching between dissecting rings and keeping them intact
     */
    public void setKeepRingsSetting(boolean aBoolean) {
        Objects.requireNonNull(aBoolean);
        this.keepRingsSetting.set(aBoolean);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public Methods">
    @Override
    public IMoleculeFragmenter copy() {
        AlkylStructureFragmenter tmpCopy = new AlkylStructureFragmenter();
        tmpCopy.setFragmentSaturationSetting((IMoleculeFragmenter.FragmentSaturationOption) this.fragmentSaturationSetting.get());
        tmpCopy.setFragmentSideChainsSetting(this.fragmentSideChainsSetting.get());
        tmpCopy.setMaxChainLengthSetting(this.maxChainLengthSetting.get());
        tmpCopy.setAlternativeSingleCarbonHandlingSetting(this.alternativeSingleCarbonHandlingSetting.get());
        return tmpCopy;
    }

    @Override
    public void restoreDefaultSettings() {
        this.fragmentSaturationSetting.set(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT);
        this.fragmentSideChainsSetting.set(AlkylStructureFragmenter.FRAGMENT_SIDE_CHAINS_SETTING_DEFAULT);
        this.maxChainLengthSetting.set(AlkylStructureFragmenter.MAX_CHAIN_LENGTH_SETTING_DEFAULT);
        this.alternativeSingleCarbonHandlingSetting.set(AlkylStructureFragmenter.ALTERNATIVE_SINGLE_CARBON_HANDLING_SETTING_DEFAULT);
    }
    //
    //<editor-fold desc="Pre-Fragmentation Tasks">
    /**
     * Returns true if the given molecule cannot be fragmented by the algorithm.
     * If the molecule is null: true is returned and therefore the molecule is filtered out.
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
        try {
            for (IAtom tmpAtom : aMolecule.atoms()) {
                if (tmpAtom.getAtomicNumber() != IElement.H && tmpAtom.getAtomicNumber() != IElement.C) {
                    return true;
                }
            }
            return false;
        } catch (Exception anException) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING,
                    anException + " Molecule ID: " + aMolecule.getID(), anException);
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
        //<editor-fold desc="Molecule Cloning, Property and Arrays Set" defaultstate="collapsed">
        IAtomContainer tmpClone = aMolecule.clone();
        int tmpPreFragmentationAtomCount = 0;
        for (IAtom tmpAtom: tmpClone.atoms()) {
            if (tmpAtom.getAtomicNumber() != 0) {
                tmpPreFragmentationAtomCount++;
            }
        }
        try {
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpClone);
            //add ChemUtils atom checks?
        } catch (CDKException aCDKException) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING,
                    aCDKException + " Molecule ID: " + aMolecule.getID(), aCDKException);
            throw new IllegalArgumentException();
        }
        //</editor-fold>
        //
        //<editor-fold desc="Detection Steps" defaultstate="collapsed">
        Object[] tmpObject = new Object[2];
        tmpObject[0] = this.fillAtomArray(tmpClone);
        tmpObject[1] = this.fillBondArray(tmpClone);
        tmpObject = this.setAtomsBondsFragmentationProperties((IAtom[]) tmpObject[0], (IBond[]) tmpObject[1]);
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
            if (tmpPostFragmentationAtomCount != tmpPreFragmentationAtomCount) {
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
                    anException + "extraction or saturation failed at molecule: " + tmpClone.getID(), anException);
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
    //<editor-fold desc="Private Methods" defaultstate="collapsed">

    /**
     * Method to fill an IAtom array with the atoms of the input IAtomContainer
     * and place fragmentation properties on them.
     *
     * @param aClone IAtomContainer, best a clone, from which atoms are put into array
     * @return IAtom array containing the atoms
     */
    protected IAtom[] fillAtomArray(IAtomContainer aClone) {
        IAtom[] tmpAtomArray = new IAtom[aClone.getAtomCount()];
        int tmpAlkylSFAtomIndex;
        for (tmpAlkylSFAtomIndex = 0; tmpAlkylSFAtomIndex < aClone.getAtomCount(); tmpAlkylSFAtomIndex++) {
            if (aClone.getAtom(tmpAlkylSFAtomIndex) != null) {
                tmpAtomArray[tmpAlkylSFAtomIndex] = aClone.getAtom(tmpAlkylSFAtomIndex);
            }
        }
        return tmpAtomArray;
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
            if (aClone.getBond(tmpAlkylSFBondIndex) != null) {
                tmpBondArray[tmpAlkylSFBondIndex] = aClone.getBond(tmpAlkylSFBondIndex);
            }
        }
        return tmpBondArray;
    }

    /**
     * Protected method for setting internal fragmentation properties for atoms and bonds.
     *
     * @param anAtomArray Given array with atoms of molecule to be fragmented
     * @param aBondArray Given array with bonds of molecule to be fragmented
     * @return Object[] containing manipulated atom and bond arrays for easier data transfer
     */
    protected Object[] setAtomsBondsFragmentationProperties(IAtom[] anAtomArray, IBond[] aBondArray) {
        Objects.requireNonNull(anAtomArray, "Given atom array is null.");
        Objects.requireNonNull(aBondArray,"Given bond array is null");
        for (int tmpAtomIndex = 0; tmpAtomIndex < anAtomArray.length; tmpAtomIndex++) {
            IAtom tmpAtom = anAtomArray[tmpAtomIndex];
            if (tmpAtom != null) {
                //set internal fragmentation properties
                //Atom_Index may be redundant due to array index
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY, tmpAtomIndex);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY, false);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY, false);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, true);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY, false);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY, false);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, false);
                if (tmpAtom.getBondCount() == 3 && tmpAtom.getMaxBondOrder() == IBond.Order.SINGLE) {
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY, true);
                    //may not work as no guarantee for proper atom or bond property set -> may have already been iterated over
                    for (IBond tmpBond: tmpAtom.bonds()) {
                        if (tmpBond.getBegin() == tmpAtom) {
                            tmpBond.getEnd().setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                            tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                        } else if (tmpBond.getEnd() == tmpAtom) {
                            tmpBond.getBegin().setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                            tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                        }
                    }
                } else if (tmpAtom.getBondCount() == 4 && tmpAtom.getMaxBondOrder() == IBond.Order.SINGLE) {
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY, true);
                    //may also not work
                    for (IBond tmpBond: tmpAtom.bonds()) {
                        if (tmpBond.getBegin() == tmpAtom) {
                            tmpBond.getEnd().setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                            tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                        } else if (tmpBond.getEnd() == tmpAtom) {
                            tmpBond.getBegin().setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                            tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, true);
                        }
                    }
                }
            }
        }
        for (int tmpBondIndex = 0; tmpBondIndex < aBondArray.length; tmpBondIndex++) {
            IBond tmpBond = aBondArray[tmpBondIndex];
            if (tmpBond != null) {
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY, tmpBondIndex);
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, true);
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY, false);
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY, false);
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY, false);
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
                int[] tmpRingIndexArray = new int[tmpFusedList.size()];
                for (int tmpFusedCount = 0; tmpFusedCount < tmpFusedList.size(); tmpFusedCount++) {
                    for (IAtom tmpFusedAtom: tmpFusedList.get(tmpFusedCount).atoms()) {
                        int tmpAtomInteger = tmpFusedAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                        anAtomArray[tmpAtomInteger].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, false);
                        //ToDo: currently ring index gets overwritten during marking -> use tmpRingIndexArray instead where ring affection is stored (possibly multiple)
                        anAtomArray[tmpAtomInteger].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FUSED_RING_INDEX_PROPERTY_KEY, tmpFusedCount);
                    }
                    for (IBond tmpFusedBond: tmpFusedList.get(tmpFusedCount).bonds()) {
                        int tmpBondInteger = tmpFusedBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY);
                        aBondArray[tmpBondInteger].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, false);
                        aBondArray[tmpBondInteger].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FUSED_RING_INDEX_PROPERTY_KEY, tmpFusedCount);
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
        if (this.alternativeSingleRingDetectionSetting.get()) {
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
                        anAtomArray[tmpAtomInteger].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, false);
                        anAtomArray[tmpAtomInteger].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_ISOLATED_RING_INDEX_PROPERTY_KEY, tmpSingleRingCount);
                    }
                    for (IBond tmpSingleRingBond: tmpContainer.bonds()) {
                        int tmpBondInteger = tmpSingleRingBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY);
                        aBondArray[tmpBondInteger].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, false);
                        aBondArray[tmpBondInteger].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_ISOLATED_RING_INDEX_PROPERTY_KEY, tmpSingleRingCount);
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
        try {
            List<IAtomContainer> tmpIsolatedRingList = tmpRingSearch.isolatedRingFragments();
            if (!tmpIsolatedRingList.isEmpty()) {
                for (int tmpIsolatedCount = 0; tmpIsolatedCount < tmpIsolatedRingList.size(); tmpIsolatedCount++) {
                    for (IAtom tmpIsolatedAtom: tmpIsolatedRingList.get(tmpIsolatedCount).atoms()) {
                        int tmpAtomInteger = tmpIsolatedAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                        anAtomArray[tmpAtomInteger].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, false);
                        anAtomArray[tmpAtomInteger].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_ISOLATED_RING_INDEX_PROPERTY_KEY, tmpIsolatedCount);
                    }
                    for (IBond tmpFusedBond: tmpIsolatedRingList.get(tmpIsolatedCount).bonds()) {
                        int tmpBondInteger = tmpFusedBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY);
                        aBondArray[tmpBondInteger].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, false);
                        aBondArray[tmpBondInteger].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_ISOLATED_RING_INDEX_PROPERTY_KEY, tmpIsolatedCount);
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
                    anAtomArray[tmpAtomInteger].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, false);
                }
                for (IBond tmpConjBond: tmpConjAtomContainer.bonds()) {
                    int tmpBondInteger = tmpConjBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY);
                    aBondArray[tmpBondInteger].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, false);
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
            //should give more than two atomcontainer at the moment but only returns 2 with an error
            //which also dont get presented in mortar
            IAtomContainerSet tmpFragmentSet = new AtomContainerSet();
            //AlkylStructureFragmenter.this.logger.log(Level.INFO, System.currentTimeMillis() + " start sD, AC size: " + anAtomContainer.getAtomCount() + ", " + anAtomContainer.getBondCount());
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
            //logger below for debug purpose
            //AlkylStructureFragmenter.this.logger.log(Level.INFO, System.currentTimeMillis() + " start sD, AC size: " + anAtomContainer.getAtomCount() + ", " + anAtomContainer.getBondCount());
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
        IAtomContainer tmpRingFragmentationContainer = new AtomContainer();
        IAtomContainer tmpChainFragmentationContainer = new AtomContainer();
        IAtomContainer tmpIsolatedMultiBondsContainer = new AtomContainer();
        IAtomContainer tmpTertQuatCarbonContainer = new AtomContainer();
        //
        //<editor-fold desc="atom extraction">
        //superior performance compared to normal for iteration over Array length
        for (IAtom tmpAtom : anAtomArray) {
            //checks atom if not part of ring or conjugated pi system
            if (tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY)) {
                if (tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY)) {
                    tmpTertQuatCarbonContainer.addAtom(tmpAtom);
                    if (this.alternativeSingleCarbonHandlingSetting.get()) {
                        for (int i = 0; i < 3; i++) {
                            PseudoAtom tmpPseudoAtom = new PseudoAtom();
                            tmpTertQuatCarbonContainer.addAtom(tmpPseudoAtom);
                            tmpTertQuatCarbonContainer.addBond(new Bond(tmpAtom, tmpPseudoAtom));
                        }
                    }
                } else if (tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY)) {
                    tmpTertQuatCarbonContainer.addAtom(tmpAtom);
                    if (this.alternativeSingleCarbonHandlingSetting.get()) {
                        for (int i = 0; i < 4; i++) {
                            PseudoAtom tmpPseudoAtom = new PseudoAtom();
                            tmpTertQuatCarbonContainer.addAtom(tmpPseudoAtom);
                            tmpTertQuatCarbonContainer.addBond(new Bond(tmpAtom, tmpPseudoAtom));
                        }
                    }
                } //extract atoms for double/triple bonds
                else if (tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_DOUBLE_BOND_MARKER_KEY)) {
                    tmpIsolatedMultiBondsContainer.addAtom(tmpAtom);
                } else if (tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY)) {
                    tmpIsolatedMultiBondsContainer.addAtom(tmpAtom);
                }
                //extract neighbor atoms of tertiary or quaternary carbon atoms
                else if (tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY)) {
                    if (this.alternativeSingleCarbonHandlingSetting.get()) {continue;}
                    tmpTertQuatCarbonContainer.addAtom(tmpAtom);
                }
                //extract residue atoms as linear chain atoms
                else {
                    tmpChainFragmentationContainer.addAtom(tmpAtom);
                }
            } else {
                tmpRingFragmentationContainer.addAtom(tmpAtom);
            }
        }
        //</editor-fold>
        //
        //<editor-fold desc="bond extraction">
        //superior performance compared to normal for iteration over Array length
        for (IBond tmpBond : aBondArray) {
            if (tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY)) {
                IAtom tmpBeginAtom = tmpBond.getBegin();
                IAtom tmpEndAtom = tmpBond.getEnd();
                //<editor-fold desc="Bond Atoms Booleans">
                //booleans for bond begin and end atom properties used in fragmentation, self-explanatory
                boolean tmpIsBeginFragPlacement = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY);
                boolean tmpIsEndFragPlacement = tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY);
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
                    tmpIsolatedMultiBondsContainer.addBond(tmpBond);
                }
                else if (tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TRIPLE_BOND_MARKER_KEY)) {
                    tmpIsolatedMultiBondsContainer.addBond(tmpBond);
                } else if (tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_NEIGHBOR_MARKER_KEY)) {
                    if (tmpIsBeginTertiary && !tmpIsEndTertiary && !(tmpIsBeginQuaternary || tmpIsEndQuaternary)) {
                        tmpTertQuatCarbonContainer.addAtom(tmpEndAtom);
                    } else if (!tmpIsBeginTertiary && tmpIsEndTertiary && !(tmpIsBeginQuaternary || tmpIsEndQuaternary)) {
                        tmpTertQuatCarbonContainer.addAtom(tmpBeginAtom);
                    } else if (tmpIsBeginQuaternary && !tmpIsEndQuaternary && !(tmpIsBeginTertiary || tmpIsEndTertiary)) {
                        tmpTertQuatCarbonContainer.addAtom(tmpEndAtom);
                    } else if (!tmpIsBeginTertiary && tmpIsEndTertiary && !(tmpIsBeginQuaternary || tmpIsEndQuaternary)) {
                        tmpTertQuatCarbonContainer.addAtom(tmpBeginAtom);
                    }
                    tmpTertQuatCarbonContainer.addBond(tmpBond);
                }
                if (tmpIsBeginFragPlacement && tmpIsEndFragPlacement && !(tmpIsBeginDouble || tmpIsEndDouble || tmpIsBeginTriple || tmpIsEndTriple)) {
                    if (!(tmpIsBeginTertiary || tmpIsEndTertiary || tmpIsBeginQuaternary || tmpIsEndQuaternary)) {
                        tmpChainFragmentationContainer.addBond(tmpBond);
                    }
                }
            }
            else {
                tmpRingFragmentationContainer.addBond(tmpBond);
            }
        }
        //</editor-fold>
        //</editor-fold>
        //
        IAtomContainerSet tmpRingACSet = this.separateDisconnectedStructures(tmpRingFragmentationContainer);
        IAtomContainerSet tmpSingleACSet = this.separateDisconnectedStructures(tmpTertQuatCarbonContainer);
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
        int tmpMaxChainLengthInteger = this.getMaxChainLengthSetting();
        if (this.fragmentSideChainsSetting.get()) {
            //check maxchainlength
            switch (tmpMaxChainLengthInteger) {
                default -> {
                    //restrictions > 1
                    for (IAtomContainer tmpAtomContainer : tmpChainACSet.atomContainers()) {
                        tmpDissectedChainACSet.add(this.separateDisconnectedStructures(this.dissectLinearChain(tmpAtomContainer,
                                tmpMaxChainLengthInteger)));
                    }
                }
                case 0 -> {
                    //if int maxChainLength gives 0 throw IllegalArgumentException
                    //not happy with how this works, preferably a gui warning would be nice
                    AlkylStructureFragmenter.this.logger.log(Level.WARNING, "Illegal restriction argument", new IllegalArgumentException());
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
                    tmpReturnAC.addAtom(tmpBond.getEnd());
                }
                tmpCounter = 1;
            } else {
                IAtom tmpBeginAtom = tmpBond.getBegin();
                IAtom tmpEndAtom = tmpBond.getEnd();
                tmpReturnAC.addAtom(tmpBeginAtom);
                tmpReturnAC.addAtom(tmpEndAtom);
                tmpReturnAC.addBond(tmpBond);
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
    //</editor-fold>
    //
}
