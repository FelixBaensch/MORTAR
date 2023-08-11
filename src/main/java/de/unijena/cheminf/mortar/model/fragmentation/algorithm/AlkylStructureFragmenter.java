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
import org.openscience.cdk.tools.CDKHydrogenAdder;
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
 * <p>
 * TODO: 31.07.2023 -future settings -> complex enums to attach values to dropdown
 *                  -preserveRingSystemMaxSetting restrictions (smaller 0 nonsense)
 *                  -pseudo atom handling (*-atoms)
 *                  -for future fragmentation: properties for single rings, ring systems and conjugated pi systems
 * </p>
 *
 * @author Maximilian Rottmann (maximilian.rottmann@studmail.w-hs.de)
 * @version 1.1.1.0
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
    private final SimpleEnumConstantNameProperty fragmentSaturationSetting;
    /**
     * A Property that has a constant carbon side chain setting:
     */
    private final SimpleIntegerProperty maxChainLengthSetting;
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
    /**
     * Internal Array to store and access atoms for mapping.
     */
    private IAtom[] atomArray;
    /**
     * Internal Array to store and access bonds for mapping.
     */
    private IBond[] bondArray;
    //</editor-fold>
    //
    //<editor-fold desc="Constructor">
    /**
     * Constructor, all settings are initialised with their respective default values.
     */
    public AlkylStructureFragmenter(){
        int tmpSettingsNameTooltipNumber = 2;
        int tmpInitialCapacitySettingsNameTooltipHashMap = CollectionUtil.calculateInitialHashCollectionCapacity(
                tmpSettingsNameTooltipNumber,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.settingNameTooltipTextMap = new HashMap<>(tmpInitialCapacitySettingsNameTooltipHashMap, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.fragmentSaturationSetting = new SimpleEnumConstantNameProperty(this, "Fragment saturation setting",
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
        this.maxChainLengthSetting = new SimpleIntegerProperty(this, "Carbon side chains maximum length setting",
                AlkylStructureFragmenter.MAX_CHAIN_LENGTH_SETTING_DEFAULT) {
            @Override
            public void set(int newValue) {
                try {
                    //AlkylStructureFragmenter.this.maxChainLengthSetting.set(AlkylStructureFragmenter.this.getMaxChainLengthSetting());
                } catch (NullPointerException | IllegalArgumentException anException) {
                    AlkylStructureFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        this.settingNameTooltipTextMap.put(this.maxChainLengthSetting.getName(),
                Message.get("AlkylStructureFragmenter.maxChainLengthSetting.tooltip"));
        this.settings = new ArrayList<Property>(2);
        this.settings.add(this.fragmentSaturationSetting);
        this.settings.add(this.maxChainLengthSetting);
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

    public int getMaxChainLengthSetting() {
        return this.maxChainLengthSetting.get();
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

    /**
     * Set method for setting defining maximum side chain length.
     *
     * @param aValue the given integer value for chain length
     */
    public void setMaxChainLengthSetting(int aValue) throws NullPointerException{
        Objects.requireNonNull(aValue, "Given chain length is null");
        this.maxChainLengthSetting.set(aValue);
    }

    //</editor-fold>
    //
    //<editor-fold desc="Public Methods">
    @Override
    public IMoleculeFragmenter copy() {
        AlkylStructureFragmenter tmpCopy = new AlkylStructureFragmenter();
        tmpCopy.setFragmentSaturationSetting(this.fragmentSaturationSetting.get());
        return tmpCopy;
    }

    @Override
    public void restoreDefaultSettings() {
        this.fragmentSaturationSetting.set(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT.name());
        this.maxChainLengthSetting.set(AlkylStructureFragmenter.MAX_CHAIN_LENGTH_SETTING_DEFAULT);
    }
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
        this.clearCache();
        this.atomArray = new IAtom[tmpClone.getAtomCount()];
        this.bondArray = new IBond[tmpClone.getBondCount()];
        int tmpAlkylSFAtomIndex = 0;
        int tmpAlkylSFBondIndex = 0;
        for (IAtom tmpAtom: tmpClone.atoms()) {
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
                this.atomArray[tmpAlkylSFAtomIndex] = tmpAtom;
                tmpAlkylSFAtomIndex ++;
            }
        }
        for (IBond tmpBond: tmpClone.bonds()) {
            if (tmpBond != null) {
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY, tmpAlkylSFBondIndex);
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, true);
                this.bondArray[tmpAlkylSFBondIndex] = tmpBond;
                tmpAlkylSFBondIndex ++;
            }
        }
        //</editor-fold>
        this.markRings(tmpClone);
        this.markConjugatedPiSystems(tmpClone);
        //<editor-fold desc="Fragment Extraction" defaultstate="collapsed">
        try {
            IAtomContainerSet tmpFragmentSet = this.extractMolecules();
            //
            //<editor-fold desc="Post-Extraction Processing">
            try {
                return this.saturateWithImplicitHydrogen(tmpFragmentSet);
            } catch (CDKException anException) {
                AlkylStructureFragmenter.this.logger.log(Level.WARNING,
                        anException + "saturation failed at molecule: " + tmpClone.getID(), anException);
                throw new IllegalArgumentException("Unexpected error occurred during fragmentation of molecule: "
                        + tmpClone.getID() + ", at fragment saturation: " + anException.toString());
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
    }
    //</editor-fold>
    //
    //</editor-fold>
    //
    //<editor-fold desc="Private Methods" defaultstate="collapsed">
    /**
     * Private method to mark all atoms and bonds of any rings in the given atomcontainer.
     *
     * @param anAtomContainer IAtomContainer to mark atoms and bonds in
     */
    private void markRings(IAtomContainer anAtomContainer) {
        //
        //<editor-fold desc="Ring System Detection" defaultstate="collapsed">
        try {
            RingSearch tmpRingSearch = new RingSearch(anAtomContainer);
            List<IAtomContainer> tmpFusedList = tmpRingSearch.fusedRingFragments();
            if (!tmpFusedList.isEmpty()) {
                for (int tmpFusedCount = 0; tmpFusedCount < tmpFusedList.size(); tmpFusedCount++) {
                    for (IAtom tmpFusedAtom: tmpFusedList.get(tmpFusedCount).atoms()) {
                        int tmpAtomInteger = tmpFusedAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                        this.atomArray[tmpAtomInteger].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, false);
                    }
                    for (IBond tmpFusedBond: tmpFusedList.get(tmpFusedCount).bonds()) {
                        int tmpBondInteger = tmpFusedBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY);
                        this.bondArray[tmpBondInteger].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, false);
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
                    this.atomArray[tmpAtomInteger].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, false);
                }
                for (IBond tmpSingleRingBond: tmpContainer.bonds()) {
                    int tmpBondInteger = tmpSingleRingBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY);
                    this.bondArray[tmpBondInteger].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, false);
                }
            }
        } catch (Exception anException) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING,
                    anException + " MoleculeID: " + anAtomContainer.getID(), anException);
            throw new IllegalArgumentException("Unexpected error occurred during fragmentation of molecule: "
                    + anAtomContainer.getID() + ", at cyclefinder : " + anException.toString());
        }
        //</editor-fold>
        //
    }

    /**
     * Private method to mark all atoms and bonds of any conjugated pi systems in the given atomcontainer.
     *
     * @param anAtomContainer IAtomContainer to mark atoms and bonds in
     */
    private void markConjugatedPiSystems(IAtomContainer anAtomContainer) {
        //<editor-fold desc="ConjugatedPiSystemsDetector" defaultstate="collapsed">
        try {
            IAtomContainerSet tmpConjugatedAtomContainerSet;
            tmpConjugatedAtomContainerSet = ConjugatedPiSystemsDetector.detect(anAtomContainer);
            //molecule mapping
            //iterate over every atomcontainer from ConjPiSystemsDetector output
            for (IAtomContainer tmpConjAtomContainer: tmpConjugatedAtomContainerSet.atomContainers()) {
                for (IAtom tmpConjAtom: tmpConjAtomContainer.atoms()) {
                    int tmpAtomInteger = tmpConjAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY);
                    this.atomArray[tmpAtomInteger].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, false);
                }
                for (IBond tmpConjBond: tmpConjAtomContainer.bonds()) {
                    int tmpBondInteger = tmpConjBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY);
                    this.bondArray[tmpBondInteger].setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, false);
                }
            }
        } catch (Exception anException) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING,
                    anException + " MoleculeID: " + anAtomContainer.getID(), anException);
            throw new IllegalArgumentException("Unexpected error occurred during fragmentation of molecule: "
                    + anAtomContainer.getID() + " at conjugated pi systems detector: " + anException.toString());
        }
        //</editor-fold>
    }

    /**
     * Private method to check given atomcontainer for disconnected structures.
     *
     * @param anAtomContainer IAtomContainer to check
     * @return IAtomContainerSet containing partitioned structures as single IAtomContainer
     */
    private IAtomContainerSet checkConnectivity(IAtomContainer anAtomContainer, boolean anIsChainContainer) {
        Objects.requireNonNull(anAtomContainer,"Given IAtomContainer is null.");
        try {
            IAtomContainerSet tmpFragmentSet = new AtomContainerSet();
            if (!anAtomContainer.isEmpty()) {
                if (!ConnectivityChecker.isConnected(anAtomContainer)) {
                    if (anIsChainContainer) {
                        System.out.println("disconnected chain in checkConnectivity");
                        switch (this.maxChainLengthSetting.get()) {
                            default:
                                //if no case matches, assume closest int value
                            case 0:
                                //no restrictions applied
                                IAtomContainerSet tmpContainerSet = ConnectivityChecker.partitionIntoMolecules(anAtomContainer);
                                for (IAtomContainer tmpContainer: tmpContainerSet.atomContainers()) {
                                    tmpFragmentSet.addAtomContainer(tmpContainer);
                                }
                            case 1:

                            case 2:
                                int tmpCounter = 0;
                                IAtomContainer tmpFragmentContainer = new AtomContainer();
                                IAtom tmpOldAtom = null;
                                for (IAtom tmpNewAtom: anAtomContainer.atoms()) {
                                    if (tmpCounter == 1) {
                                        if (tmpOldAtom != null) {
                                            tmpFragmentContainer.addAtom(tmpNewAtom);
                                            IBond tmpBond = new Bond(tmpOldAtom, tmpNewAtom);
                                            tmpFragmentContainer.addBond(tmpBond);
                                            tmpFragmentSet.addAtomContainer(tmpFragmentContainer);
                                        }
                                    } else if (tmpCounter == 0){
                                        tmpFragmentContainer.addAtom(tmpNewAtom);
                                    } else {
                                        if (tmpOldAtom != null) {
                                            tmpFragmentContainer.addAtom(tmpNewAtom);
                                            IBond tmpBond = new Bond(tmpOldAtom, tmpNewAtom);
                                            tmpFragmentContainer.addBond(tmpBond);
                                        }
                                    }
                                    tmpOldAtom = tmpNewAtom;
                                    tmpCounter++;
                                }
                            case 3:

                            case 4:

                            case 5:

                            case 6:

                            case 7:

                            case 8:

                            case 9:
                        }
                    } else {
                        IAtomContainerSet tmpContainerSet = ConnectivityChecker.partitionIntoMolecules(anAtomContainer);
                        for (IAtomContainer tmpContainer: tmpContainerSet.atomContainers()) {
                            tmpFragmentSet.addAtomContainer(tmpContainer);
                        }
                    }
                } else {
                    if (anIsChainContainer) {
                        System.out.println("connected chain in checkConnectivity");
                        int tmpMockUpForTest = 2;
                        if (tmpMockUpForTest == 2
                                //this.maxChainLengthSetting.get() == 2
                        ) {
                            System.out.println("if connected chain: 2");
                            int tmpCounter = 0;
                            IAtomContainer tmpFragmentContainer = new AtomContainer();
                            IAtom tmpOldAtom = null;
                            for (IAtom tmpNewAtom: anAtomContainer.atoms()) {
                                boolean tmpBoolean = false;
                                if (tmpCounter == 1) {
                                    if (tmpOldAtom != null) {
                                        System.out.println("if if " + tmpCounter);
                                        tmpFragmentContainer.addAtom(tmpNewAtom);
                                        IBond tmpBond = new Bond(tmpOldAtom, tmpNewAtom);
                                        tmpFragmentContainer.addBond(tmpBond);
                                        tmpFragmentSet.addAtomContainer(tmpFragmentContainer);
                                        //tmpFragmentContainer.removeAllElements();
                                        //tmpCounter = 0;
                                        //tmpBoolean = true;
                                    }
                                } else if (tmpCounter == 0){
                                    System.out.println("1. else " + tmpCounter);
                                    tmpFragmentContainer.addAtom(tmpNewAtom);
                                } else {
                                    if (tmpOldAtom != null) {
                                        System.out.println("2. else " + tmpCounter);
                                        tmpFragmentContainer.addAtom(tmpNewAtom);
                                        IBond tmpBond = new Bond(tmpOldAtom, tmpNewAtom);
                                        tmpFragmentContainer.addBond(tmpBond);
                                    }
                                }
                                if (!tmpBoolean) {
                                    tmpOldAtom = tmpNewAtom;
                                }
                                System.out.println("before ++ " + tmpCounter);
                                tmpCounter++;
                            }
                        }

                    } else {
                        System.out.println("FragmentSet add Input");
                        tmpFragmentSet.addAtomContainer(anAtomContainer);
                    }
                }
            }
            System.out.println(tmpFragmentSet.getAtomContainerCount());
            return tmpFragmentSet;
        } catch (Exception anException) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING, anException + " Connectivity Checking failed at molecule: " + anAtomContainer.getID(), anException);
            throw new IllegalArgumentException("An Error occurred during Connectivity Checking: " + anException.toString() +
                    ": " + anAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
        }
    }

    /**
     * Private method to saturate a given molecule with implicit hydrogens after fragmentation.
     *
     * @param anUnsaturatedACSet IAtomContainerSet whose atomcontainers are to be saturated
     * @return List of processed atomcontainers, @null if given Set is empty
     * @throws CDKException if CDKHydrogenAdder throws an exception
     */
    private List<IAtomContainer> saturateWithImplicitHydrogen(IAtomContainerSet anUnsaturatedACSet) throws CDKException {
        Objects.requireNonNull(anUnsaturatedACSet, "Given IAtomContainerSet is null.");
        try {
            List<IAtomContainer> tmpSaturatedFragments = new ArrayList<>(anUnsaturatedACSet.getAtomContainerCount());
            if (!anUnsaturatedACSet.isEmpty() && !anUnsaturatedACSet.getAtomContainer(0).isEmpty()) {
                CDKHydrogenAdder tmpAdder = CDKHydrogenAdder.getInstance(anUnsaturatedACSet.getAtomContainer(0).getBuilder());
                for (IAtomContainer tmpAtomContainer: anUnsaturatedACSet.atomContainers()) {
                    if (tmpAtomContainer != null && !tmpAtomContainer.isEmpty()) {
                        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpAtomContainer);
                        if (this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION.name())) {
                            tmpAdder.addImplicitHydrogens(tmpAtomContainer);
                            tmpSaturatedFragments.add(tmpAtomContainer);
                        } else {
                            tmpSaturatedFragments.add(tmpAtomContainer);
                        }
                    }
                }
            }
            return tmpSaturatedFragments;
        } catch (CDKException anException) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING, anException
                + " Unable to add Implicit Hydrogen");
            throw new CDKException("Unexpected error occurred during implicit hydrogen adding at " +
                "hydrogen saturation of molecule, " + anException.toString(), anException);
        }
    }

    /**
     * Private method to extract detected molecules via properties.
     *
     * @return IAtomContainerSet with extracted molecules
     */
    private IAtomContainerSet extractMolecules() {
        //
        //<editor-fold desc="Extraction">
        IAtomContainerSet tmpExtractionSet = new AtomContainerSet();
        IAtomContainer tmpRingFragmentationContainer = new AtomContainer();
        IAtomContainer tmpChainFragmentationContainer = new AtomContainer();
        IAtomContainer tmpSingleCarbonContainer = new AtomContainer();
        //atom extraction
        //superior performance compared to normal for iteration over Array length
        for (IAtom tmpAtom : this.atomArray) {
            if (tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY)) {
                if (tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_TERTIARY_CARBON_PROPERTY_KEY)) {
                    tmpSingleCarbonContainer.addAtom(tmpAtom);
                    for (int i = 0; i < 3; i++) {
                        PseudoAtom tmpPseudoAtom = new PseudoAtom();
                        tmpSingleCarbonContainer.addAtom(tmpPseudoAtom);
                        tmpSingleCarbonContainer.addBond(new Bond(tmpAtom, tmpPseudoAtom));
                    }
                } else if (tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_QUATERNARY_CARBON_PROPERTY_KEY)) {
                    tmpSingleCarbonContainer.addAtom(tmpAtom);
                    for (int i = 0; i < 4; i++) {
                        PseudoAtom tmpPseudoAtom = new PseudoAtom();
                        tmpSingleCarbonContainer.addAtom(tmpPseudoAtom);
                        tmpSingleCarbonContainer.addBond(new Bond(tmpAtom, tmpPseudoAtom));
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
        for (IBond tmpBond : this.bondArray) {
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
        IAtomContainerSet tmpRingAtomContainerSet = checkConnectivity(tmpRingFragmentationContainer, false);
        IAtomContainerSet tmpChainAtomContainerSet = checkConnectivity(tmpChainFragmentationContainer, true);
        IAtomContainerSet tmpSingleAtomContainerSet = checkConnectivity(tmpSingleCarbonContainer, false);
        if (!tmpRingAtomContainerSet.isEmpty() && tmpRingAtomContainerSet.getAtomContainerCount() > 0) {
            tmpExtractionSet.add(tmpRingAtomContainerSet);
        }
        if (!tmpChainAtomContainerSet.isEmpty() && tmpChainAtomContainerSet.getAtomContainerCount() > 0) {
            tmpExtractionSet.add(tmpChainAtomContainerSet);
        }
        if (!tmpSingleAtomContainerSet.isEmpty() && tmpSingleAtomContainerSet.getAtomContainerCount() > 0) {
            tmpExtractionSet.add(tmpSingleAtomContainerSet);
        }
        return tmpExtractionSet;
    }

    /**
     * Private method to reset class variables properly.
     */
    private void clearCache() {
        this.atomArray = null;
        this.bondArray = null;
    }
    //</editor-fold>
    //
}
