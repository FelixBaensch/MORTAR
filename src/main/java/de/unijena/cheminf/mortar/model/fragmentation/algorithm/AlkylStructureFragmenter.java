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
import de.unijena.cheminf.mortar.model.util.ChemUtil;
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
 * TODO: 19.07.2023 -simplifying fragmentation into private methods
 *                  -future settings -> complex enums to attach values to dropdown
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
     * Key for an internal index property, used in uniquely identifying atoms during fragmentation.
     */
    public static final String INTERNAL_ASF_ATOM_INDEX_PROPERTY_KEY = "ASF.ATOM_INDEX";
    /**
     * Key for an internal index property, used in uniquely identifying bonds during fragmentation.
     */
    public static final String INTERNAL_ASF_BOND_INDEX_PROPERTY_KEY = "ASF.BOND_INDEX";
    /**
     * Key for an internal boolean property, used in identifying whether an atom or bond has been placed in the
     * corresponding fragmentation array. It is further used during fragmentation.
     */
    public static final String INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY = "ASF.FRAGMENTATION_PLACEMENT";
    //</editor-fold>
    //
    //<editor-fold desc="Private Class Variables">
    /**
     * A property that has a constant fragment hydrogen saturation setting.
     */
    private final SimpleEnumConstantNameProperty fragmentSaturationSetting;
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
        int tmpSettingsNameTooltipNumber = 1;
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
        this.settings = new ArrayList<Property>(1);
        this.settings.add(this.fragmentSaturationSetting);
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
                tmpAtom.setProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY, true);
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
    private IAtomContainerSet checkConnectivity(IAtomContainer anAtomContainer) {
        Objects.requireNonNull(anAtomContainer,"Given IAtomContainer is null.");
        try {
            IAtomContainerSet tmpFragmentSet = new AtomContainerSet();
            if (!anAtomContainer.isEmpty()) {
                if (!ConnectivityChecker.isConnected(anAtomContainer)) {
                    IAtomContainerSet tmpContainerSet = ConnectivityChecker.partitionIntoMolecules(anAtomContainer);
                    for (IAtomContainer tmpContainer: tmpContainerSet.atomContainers()) {
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
     * @return List of processed atomcontainers, null if given Set is empty
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
                        if (this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION.name())) {
                            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpAtomContainer);
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
        //atom extraction
        //superior performance compared to normal for iteration over Array length
        for (IAtom tmpAtom : this.atomArray) {
            if (tmpAtom.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY)) {
                tmpChainFragmentationContainer.addAtom(tmpAtom);
            } else {
                tmpRingFragmentationContainer.addAtom(tmpAtom);
            }
        }
        //bond extraction
        //superior performance compared to normal for iteration over Array length
        for (IBond tmpBond : this.bondArray) {
            if (tmpBond.getProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY)) {
                if (tmpBond.getBegin().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY))
                    if (tmpBond.getEnd().getProperty(AlkylStructureFragmenter.INTERNAL_ASF_FRAGMENTATION_PLACEMENT_KEY)) {
                        tmpChainFragmentationContainer.addBond(tmpBond);
                    }
            }
            else {
                tmpRingFragmentationContainer.addBond(tmpBond);
            }
        }
        //</editor-fold>
        //
        IAtomContainerSet tmpRingAtomContainerSet = checkConnectivity(tmpRingFragmentationContainer);
        IAtomContainerSet tmpChainAtomContainerSet = checkConnectivity(tmpChainFragmentationContainer);
        if (!tmpRingAtomContainerSet.isEmpty() && tmpRingAtomContainerSet.getAtomContainerCount() > 0) {
            tmpExtractionSet.add(tmpRingAtomContainerSet);
        }
        if (!tmpChainAtomContainerSet.isEmpty() && tmpChainAtomContainerSet.getAtomContainerCount() > 0) {
            tmpExtractionSet.add(tmpChainAtomContainerSet);
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
