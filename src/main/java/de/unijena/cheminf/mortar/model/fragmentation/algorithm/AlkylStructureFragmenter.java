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

//<editor-fold desc="Future Development">
/*
TODO: 26.10.2023
    -preserveRingSystemMaxSetting restrictions (smaller 0 nonsense)
    -complete pseudo atom handling (*-atoms)
    -for future fragmentation: properties for single rings, ring systems and conjugated pi systems
    -some dbs may be definable as tertiary/quaternary carbons in some configurations (atom & bond handling!)
*/
//</editor-fold>

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
     * Default boolean value for restriction of further side chain dissection.
     */
    public static final boolean RESTRICT_MAX_CHAIN_LENGTH_SETTING_DEFAULT = false;
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
     * A property that has a constant boolean value determining whether fragment size should be restricted.
     */
    private final SimpleBooleanProperty restrictMaxChainLengthSetting;
    /**
     * A Property that has a constant carbon side chain setting.
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
        int tmpSettingsNameTooltipNumber = 3;
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
        this.restrictMaxChainLengthSetting = new SimpleBooleanProperty(this, "Restrict length of hydrocarbon side chains setting",
                AlkylStructureFragmenter.RESTRICT_MAX_CHAIN_LENGTH_SETTING_DEFAULT);
        this.settingNameTooltipTextMap.put(this.restrictMaxChainLengthSetting.getName(),
                Message.get("AlkylStructureFragmenter.restrictMaxChainLengthSetting.tooltip"));
        this.maxChainLengthSetting = new SimpleIntegerProperty(this, "Carbon side chains maximum length setting",
                AlkylStructureFragmenter.MAX_CHAIN_LENGTH_SETTING_DEFAULT);
        this.settingNameTooltipTextMap.put(this.maxChainLengthSetting.getName(),
                Message.get("AlkylStructureFragmenter.maxChainLengthSetting.tooltip"));
        this.settings = new ArrayList<Property>(3);
        this.settings.add(this.fragmentSaturationSetting);
        this.settings.add(this.restrictMaxChainLengthSetting);
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
     *
     * @param aBoolean
     * @throws NullPointerException
     */
    public void setRestrictMaxChainLengthSetting(boolean aBoolean) throws NullPointerException{
        Objects.requireNonNull(aBoolean, "Given boolean is null.");
        this.restrictMaxChainLengthSetting.set(aBoolean);
    }
    /**
     * Set method for setting defining maximum side chain length.
     *
     * @param aValue the given integer value for chain length
     * @throws IllegalArgumentException
     */
    public void setMaxChainLengthSetting(int aValue) throws IllegalArgumentException{
        if (aValue < 0) {
            throw new IllegalArgumentException("Given chain length cannot be negative.");
        }
        this.maxChainLengthSetting.set(aValue);
    }

    //</editor-fold>
    //
    //<editor-fold desc="Public Methods">
    @Override
    public IMoleculeFragmenter copy() {
        AlkylStructureFragmenter tmpCopy = new AlkylStructureFragmenter();
        tmpCopy.setFragmentSaturationSetting(this.fragmentSaturationSetting.get());
        tmpCopy.setRestrictMaxChainLengthSetting(this.restrictMaxChainLengthSetting.get());
        tmpCopy.setMaxChainLengthSetting(this.maxChainLengthSetting.get());
        return tmpCopy;
    }

    @Override
    public void restoreDefaultSettings() {
        this.fragmentSaturationSetting.set(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT.name());
        this.restrictMaxChainLengthSetting.set(AlkylStructureFragmenter.RESTRICT_MAX_CHAIN_LENGTH_SETTING_DEFAULT);
        this.maxChainLengthSetting.set(AlkylStructureFragmenter.MAX_CHAIN_LENGTH_SETTING_DEFAULT);
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
        try {
            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpClone);
        } catch (CDKException aCDKException) {
            AlkylStructureFragmenter.this.logger.log(Level.WARNING,
                    aCDKException + " Molecule ID: " + aMolecule.getID(), aCDKException);
            throw new IllegalArgumentException();
        }
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
                tmpAlkylSFAtomIndex++;
            }
        }
        for (IBond tmpBond: tmpClone.bonds()) {
            if (tmpBond != null) {
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY, tmpAlkylSFBondIndex);
                tmpBond.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, true);
                this.bondArray[tmpAlkylSFBondIndex] = tmpBond;
                tmpAlkylSFBondIndex++;
            }
        }
        //</editor-fold>
        //
        //<editor-fold desc="Detection Steps" defaultstate="collapsed">
        this.markRings(tmpClone);
        this.markConjugatedPiSystems(tmpClone);
        //</editor-fold>
        //<editor-fold desc="Fragment Extraction and Saturation" defaultstate="collapsed">
        try {
            IAtomContainerSet tmpFragmentSet = this.extractFragments();
            if (this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION.name())) {
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
     * Private method to mark all atoms and bonds of any rings in the given atomcontainer.
     *
     * @param anAtomContainer IAtomContainer to mark atoms and bonds in
     */
    private void markRings(IAtomContainer anAtomContainer) throws IllegalArgumentException {
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
    private void markConjugatedPiSystems(IAtomContainer anAtomContainer) throws IllegalArgumentException{
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
    private IAtomContainerSet separateDisconnectedStructures(IAtomContainer anAtomContainer) throws IllegalArgumentException{
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
                        tmpAdder.addImplicitHydrogens(tmpAtomContainer);
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
     * Private method to extract detected molecules via properties.
     *
     * @return IAtomContainerSet with extracted molecules
     */
    private IAtomContainerSet extractFragments() throws CloneNotSupportedException, IllegalArgumentException {
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
        if (this.restrictMaxChainLengthSetting.get()) {
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
     * Private method to reset class variables properly.
     */
    private void clearCache() {
        this.atomArray = null;
        this.bondArray = null;
    }
    /**
     * Private Method to dissect given AtomContainer (containing linear carbon chain) into separate molecules with given length and remnants if
     * molecule is too small for given length.
     *
     * @param anAC AtomContainer to be dissected
     * @param aLength Given maximum length of molecule
     * @return AtomContainer with separate dissected molecules
     */
    private IAtomContainer dissectLinearChain(IAtomContainer anAC, int aLength) {
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
