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
     * Map to store pairs of {@literal <setting name, tooltip text>}.
     */
    private final HashMap<String, String> settingNameTooltipTextMap;
    /**
     * All settings of this fragmenter, encapsulated in JavaFX properties for binding to GUI.
     */
    private final List<Property<?>> settings;
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
        int tmpSettingsNameTooltipNumber = 4;
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

        this.settings = new ArrayList<>(4);
        this.settings.add(this.fragmentSaturationSetting);
        this.settings.add(this.fragmentSideChainsSetting);
        this.settings.add(this.maxChainLengthSetting);
        this.settings.add(this.alternativeSingleCarbonHandlingSetting);
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

    /*
    @Override
    public FragmentSaturationOption getFragmentSaturationSettingConstant() {
        return FragmentSaturationOption.valueOf(this.fragmentSaturationSetting.get());
    }
    */

    //</editor-fold>
    //
    //<editor-fold desc="Public Properties Set">
    /*
    @Override
    public void setFragmentSaturationSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given saturation option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name
        FragmentSaturationOption tmpConstant = FragmentSaturationOption.valueOf(anOptionName);
        this.fragmentSaturationSetting.set(tmpConstant.name());
    }
    */

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
        //
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
        } catch (CDKException aCDKException) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING,
                    aCDKException + " Molecule ID: " + aMolecule.getID(), aCDKException);
            throw new IllegalArgumentException();
        }
        //</editor-fold>
        //
        //<editor-fold desc="Detection Steps" defaultstate="collapsed">
        Object[] tmpObject = this.markRings(tmpClone, this.fillAtomArray(tmpClone), this.fillBondArray(tmpClone));
        tmpObject = this.markConjugatedPiSystems(tmpClone, (IAtom[]) tmpObject[0], (IBond[]) tmpObject[1]);
        //</editor-fold>
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
        int tmpAlkylSFAtomIndex = 0;
        for (IAtom tmpAtom: aClone.atoms()) {
            if (tmpAtom != null) {
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY, tmpAlkylSFAtomIndex);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY, false);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY, false);
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, true);
                if (tmpAtom.getBondCount() == 3 && tmpAtom.getMaxBondOrder() == IBond.Order.SINGLE) {
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY, true);
                } else if (tmpAtom.getBondCount() == 4 && tmpAtom.getMaxBondOrder() == IBond.Order.SINGLE) {
                    tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY, true);
                }
                tmpAtomArray[tmpAlkylSFAtomIndex] = tmpAtom;
                tmpAlkylSFAtomIndex++;
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
        int tmpAlkylSFBondIndex = 0;
        for (IBond tmpBond: aClone.bonds()) {
            if (tmpBond != null) {
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY, tmpAlkylSFBondIndex);
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, true);
                tmpBondArray[tmpAlkylSFBondIndex] = tmpBond;
                tmpAlkylSFBondIndex++;
            }
        }
        return tmpBondArray;
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
        try {
            RingSearch tmpRingSearch = new RingSearch(anAtomContainer);
            List<IAtomContainer> tmpFusedList = tmpRingSearch.fusedRingFragments();
            if (!tmpFusedList.isEmpty()) {
                for (int tmpFusedCount = 0; tmpFusedCount < tmpFusedList.size(); tmpFusedCount++) {
                    for (IAtom tmpFusedAtom: tmpFusedList.get(tmpFusedCount).atoms()) {
                        int tmpAtomInteger = tmpFusedAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                        anAtomArray[tmpAtomInteger].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, false);
                    }
                    for (IBond tmpFusedBond: tmpFusedList.get(tmpFusedCount).bonds()) {
                        int tmpBondInteger = tmpFusedBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY);
                        aBondArray[tmpBondInteger].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, false);
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
        //<editor-fold desc="CycleFinder (Single Rings)" defaultstate="collapsed">
        CycleFinder tmpMCBCycleFinder = Cycles.mcb();
        IRingSet tmpMCBCyclesSet;
        try {
            Cycles tmpMCBCycles = tmpMCBCycleFinder.find(anAtomContainer);
            tmpMCBCyclesSet = tmpMCBCycles.toRingSet();
            for (IAtomContainer tmpContainer: tmpMCBCyclesSet.atomContainers()) {
                for (IAtom tmpSingleRingAtom: tmpContainer.atoms()) {
                    int tmpAtomInteger = tmpSingleRingAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                    anAtomArray[tmpAtomInteger].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, false);
                }
                for (IBond tmpSingleRingBond: tmpContainer.bonds()) {
                    int tmpBondInteger = tmpSingleRingBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY);
                    aBondArray[tmpBondInteger].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, false);
                }
            }
        } catch (Exception anException) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING,
                    anException + " MoleculeID: " + anAtomContainer.getID(), anException);
            throw new IllegalArgumentException("Unexpected error occurred during fragmentation of molecule: "
                    + anAtomContainer.getID() + ", at cyclefinder : " + anException.toString());
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
            IAtomContainerSet tmpFragmentSet = new AtomContainerSet();
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
        IAtomContainer tmpSingleCarbonContainer = new AtomContainer();
        //atom extraction
        //superior performance compared to normal for iteration over Array length
        for (IAtom tmpAtom : anAtomArray) {
            if (tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY)) {
                if (tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY)) {
                    tmpSingleCarbonContainer.addAtom(tmpAtom);
                    if (this.alternativeSingleCarbonHandlingSetting.get()) {
                        for (int i = 0; i < 3; i++) {
                            PseudoAtom tmpPseudoAtom = new PseudoAtom();
                            tmpSingleCarbonContainer.addAtom(tmpPseudoAtom);
                            tmpSingleCarbonContainer.addBond(new Bond(tmpAtom, tmpPseudoAtom));
                        }
                    }
                } else if (tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY)) {
                    tmpSingleCarbonContainer.addAtom(tmpAtom);
                    if (this.alternativeSingleCarbonHandlingSetting.get()) {
                        for (int i = 0; i < 4; i++) {
                            PseudoAtom tmpPseudoAtom = new PseudoAtom();
                            tmpSingleCarbonContainer.addAtom(tmpPseudoAtom);
                            tmpSingleCarbonContainer.addBond(new Bond(tmpAtom, tmpPseudoAtom));
                        }
                    }
                } else {
                    tmpChainFragmentationContainer.addAtom(tmpAtom);
                }
            } else {
                tmpRingFragmentationContainer.addAtom(tmpAtom);
            }
        }
        //bond extraction
        //superior performance compared to normal for iteration over Array length
        for (IBond tmpBond : aBondArray) {
            if (tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY)) {
                IAtom tmpBeginAtom = tmpBond.getBegin();
                IAtom tmpEndAtom = tmpBond.getEnd();
                boolean tmpIsBeginFragPlacement = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY);
                boolean tmpIsEndFragPlacement = tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY);
                boolean tmpIsBeginTertiary = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY);
                boolean tmpIsEndTertiary = tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY);
                boolean tmpIsBeginQuaternary = tmpBeginAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY);
                boolean tmpIsEndQuaternary = tmpEndAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY);
                if (tmpIsBeginFragPlacement && tmpIsEndFragPlacement) {
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
        //
        IAtomContainerSet tmpRingACSet = this.separateDisconnectedStructures(tmpRingFragmentationContainer);
        IAtomContainerSet tmpSingleACSet = this.separateDisconnectedStructures(tmpSingleCarbonContainer);
        if (!tmpRingACSet.isEmpty() && tmpRingACSet.getAtomContainerCount() > 0) {
            tmpExtractionSet.add(tmpRingACSet);
        }
        if (!tmpSingleACSet.isEmpty() && tmpSingleACSet.getAtomContainerCount() > 0) {
            tmpExtractionSet.add(tmpSingleACSet);
        }
        //remnants after ring, conj. system and tertiary/quaternary carbon extractions
        //expected to be only linear carbohydrates
        IAtomContainerSet tmpChainACSet = this.separateDisconnectedStructures(tmpChainFragmentationContainer);
        //ACSet for dissected chains
        IAtomContainerSet tmpDissectedChainACSet = new AtomContainerSet();
        int tmpMaxChainLengthInteger = this.getMaxChainLengthSetting();
        if (this.fragmentSideChainsSetting.get()) {
            //check maxchainlength
            switch (tmpMaxChainLengthInteger) {
                default -> {
                    //down
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
     * Protected Method to dissect given AtomContainer (containing linear carbon chain) into separate molecules with given length and remnants if
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
    //</editor-fold>
    //
}
