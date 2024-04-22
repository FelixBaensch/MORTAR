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
import de.unijena.cheminf.mortar.model.util.CollectionUtil;
import de.unijena.cheminf.mortar.model.util.IDisplayEnum;
import de.unijena.cheminf.mortar.model.util.SimpleIDisplayEnumConstantProperty;

import javafx.beans.property.Property;

import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.graph.invariant.ConjugatedPiSystemsDetector;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IBond;
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
 * Java class implementing an algorithm in MORTAR for detection of conjugated pi systems using
 * the CDK functionality {@link org.openscience.cdk.graph.invariant.ConjugatedPiSystemsDetector}
 * to simply test and validate its purpose.
 *
 * @author Maximilian Rottmann
 * @version 1.1.1.0
 */
public class ConjugatedPiSystemFragmenter implements IMoleculeFragmenter{
    //
    //<editor-fold desc="Public Static Final Class Variables">
    /**
     * Name of the fragmenter, CPS stands for Conjugated Pi System.
     */
    public static final String ALGORITHM_NAME = "CPS Fragmenter";
    /**
     * Key for an internal index property, used in uniquely identifying atoms during fragmentation.
     */
    public static final String INTERNAL_CPSF_ATOM_INDEX_PROPERTY_KEY = "CPSF.ATOM_INDEX";
    /**
     * Key for an internal index property, used in uniquely identifying bonds during fragmentation.
     */
    public static final String INTERNAL_CPSF_BOND_INDEX_PROPERTY_KEY = "CPSF.BOND_INDEX";
    //</editor-fold>
    //
    //<editor-fold desc="Private Final Class Variables">
    /**
     * A property that has a constant fragment hydrogen saturation setting.
     */
    private final SimpleIDisplayEnumConstantProperty fragmentSaturationSetting;
    /**
     * All settings of this fragmenter, encapsulated in JavaFX properties for binding to GUI.
     */
    private final List<Property<?>> settings;
    /**
     * Map to store pairs of {@literal <setting name, tooltip text>}.
     */

    private final HashMap<String, String> settingNameTooltipTextMap;
    private final HashMap<String, String> settingNameDisplayNameMap;
    /**
     * The logger responsible for this fragmenter.
     */
    private static final Logger logger = Logger.getLogger(ConjugatedPiSystemFragmenter.class.getName());
    private IAtom[] atomArray;
    private IBond[] bondArray;
    //</editor-fold>
    //
    //<editor-fold desc="Class Constructor">
    /**
     * Constructor of this fragmenter class.
     */
    public ConjugatedPiSystemFragmenter(){
        int tmpNumberOfSettingsForTooltipMapSize= 1;
        int tmpInitialCapacityForSettingNameTooltipTextMap = CollectionUtil.calculateInitialHashCollectionCapacity(
                tmpNumberOfSettingsForTooltipMapSize,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.settingNameTooltipTextMap = new HashMap<>(tmpInitialCapacityForSettingNameTooltipTextMap, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.settingNameDisplayNameMap = new HashMap<>(tmpInitialCapacityForSettingNameTooltipTextMap, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.fragmentSaturationSetting = new SimpleIDisplayEnumConstantProperty(this, "Fragment saturation setting",
                IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT, IMoleculeFragmenter.FragmentSaturationOption.class) {
            @Override
            public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ConjugatedPiSystemFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };

        this.settingNameTooltipTextMap.put(this.fragmentSaturationSetting.getName(),
                Message.get("ConjugatedPiSystemFragmenter.fragmentSaturationSetting.tooltip"));
        this.settingNameDisplayNameMap.put(this.fragmentSaturationSetting.getName(),
                Message.get("ConjugatedPiSystemFragmenter.fragmentSaturationSetting.displayName"));
        this.settings = new ArrayList<>(1);
        this.settings.add(this.fragmentSaturationSetting);
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

    /**
     * Returns a map containing language-specific names (values) for the settings with the given names (keys) to be used
     * in the GUI.
     *
     * @return map with display names
     */
    @Override
    public Map<String, String> getSettingNameToDisplayNameMap() {
        return Map.of();
    }

    @Override
    public String getFragmentationAlgorithmName() {
        return ConjugatedPiSystemFragmenter.ALGORITHM_NAME;
    }

    /**
     * Returns a language-specific name of the fragmenter to be used in the GUI.
     * The given name must be unique among the available fragmentation algorithms!
     *
     * @return language-specific name for display in GUI
     */
    @Override
    public String getFragmentationAlgorithmDisplayName() {
        return Message.get("ConjugatedPiSystemFragmenter.displayName");
    }

    @Override
    public FragmentSaturationOption getFragmentSaturationSetting() {
        return (IMoleculeFragmenter.FragmentSaturationOption) this.fragmentSaturationSetting.get();
    }

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
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        FragmentSaturationOption tmpConstant = FragmentSaturationOption.valueOf(anOptionName);
        this.fragmentSaturationSetting.set(tmpConstant.name());
    }
    */

    @Override
    public void setFragmentSaturationSetting(FragmentSaturationOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given saturation option is null.");
        this.fragmentSaturationSetting.set(anOption);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public Methods">
    @Override
    public IMoleculeFragmenter copy() {
        ConjugatedPiSystemFragmenter tmpCopy = new ConjugatedPiSystemFragmenter();
        tmpCopy.setFragmentSaturationSetting((IMoleculeFragmenter.FragmentSaturationOption) this.fragmentSaturationSetting.get());
        return tmpCopy;
    }

    @Override
    public void restoreDefaultSettings() {
        this.fragmentSaturationSetting.set(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT);

    }

    @Override
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {

        //<editor-fold desc="Molecule Cloning, Property and Arrays Set">
        IAtomContainer tmpClone = aMolecule.clone();
        this.clearCache();
        this.atomArray = new IAtom[tmpClone.getAtomCount()];
        this.bondArray = new IBond[tmpClone.getBondCount()];
        int tmpCPSFAtomIndex = 0;
        int tmpCPSFBondIndex = 0;
        for (IAtom tmpAtom: tmpClone.atoms()) {
            if (tmpAtom != null) {
                tmpAtom.setProperty(ConjugatedPiSystemFragmenter.INTERNAL_CPSF_ATOM_INDEX_PROPERTY_KEY, tmpCPSFAtomIndex);
                this.atomArray[tmpCPSFAtomIndex] = tmpAtom;
                tmpCPSFAtomIndex++;
            }
        }
        for (IBond tmpBond: tmpClone.bonds()) {
            if (tmpBond != null) {
                tmpBond.setProperty(ConjugatedPiSystemFragmenter.INTERNAL_CPSF_BOND_INDEX_PROPERTY_KEY, tmpCPSFBondIndex);
                this.bondArray[tmpCPSFBondIndex] = tmpBond;
                tmpCPSFBondIndex++;
            }
        }
        //</editor-fold>

        //<editor-fold desc="Detection and Extraction">
        IAtomContainer tmpFragments = new AtomContainer();
        try {
            IAtomContainerSet tmpConjugatedAtomContainerSet;
            tmpConjugatedAtomContainerSet = ConjugatedPiSystemsDetector.detect(tmpClone);
            for (IAtomContainer tmpConjAtomContainer: tmpConjugatedAtomContainerSet.atomContainers()) {
                for (IAtom tmpConjAtom: tmpConjAtomContainer.atoms()) {
                    int tmpAtomInteger = tmpConjAtom.getProperty(ConjugatedPiSystemFragmenter.INTERNAL_CPSF_ATOM_INDEX_PROPERTY_KEY);
                    tmpFragments.addAtom(this.atomArray[tmpAtomInteger]);
                }
                for (IBond tmpConjBond: tmpConjAtomContainer.bonds()) {
                    int tmpBondInteger = tmpConjBond.getProperty(ConjugatedPiSystemFragmenter.INTERNAL_CPSF_BOND_INDEX_PROPERTY_KEY);
                    tmpFragments.addBond(this.bondArray[tmpBondInteger]);
                }
            }
        } catch (Exception anException) {
            ConjugatedPiSystemFragmenter.this.logger.log(Level.WARNING,
                    anException + " MoleculeID: " + tmpClone.getID(), anException);
            throw new IllegalArgumentException("Unexpected error occurred during fragmentation of molecule: "
                    + tmpClone.getID() + " at conjugated pi systems detector: " + anException.toString());
        }
        //</editor-fold>

        //<editor-fold desc="Connectivity Checking">
        IAtomContainerSet tmpFragmentSet = new AtomContainerSet();
        try {
            if (!tmpFragments.isEmpty()) {
                if (!ConnectivityChecker.isConnected(tmpFragments)) {
                    IAtomContainerSet tmpContainerSet = ConnectivityChecker.partitionIntoMolecules(tmpFragments);
                    for (IAtomContainer tmpContainer: tmpContainerSet.atomContainers()) {
                        tmpFragmentSet.addAtomContainer(tmpContainer);
                    }
                } else {
                    tmpFragmentSet.addAtomContainer(tmpFragments);
                }
            }
        } catch (Exception anException) {
            ConjugatedPiSystemFragmenter.this.logger.log(Level.WARNING, anException + " Connectivity Checking failed at molecule: " + tmpClone.getID(), anException);
            throw new IllegalArgumentException("An Error occurred during Connectivity Checking: " + anException.toString() +
                    ": " + tmpClone.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
        }
        //</editor-fold>

        //<editor-fold desc="Hydrogen Saturation">
        List<IAtomContainer> tmpProcessedFragments = new ArrayList<>(tmpFragmentSet.getAtomContainerCount());
        try {
            if (!tmpFragmentSet.isEmpty() && tmpFragmentSet != null) {
                CDKHydrogenAdder tmpAdder = CDKHydrogenAdder.getInstance(tmpFragmentSet.getAtomContainer(0).getBuilder());
                for (IAtomContainer tmpAtomContainer: tmpFragmentSet.atomContainers()) {
                    AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpAtomContainer);
                    if (this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION.name())) {
                        try {
                            tmpAdder.addImplicitHydrogens(tmpAtomContainer);
                        } catch (CDKException anException) {
                            ConjugatedPiSystemFragmenter.this.logger.log(Level.WARNING, anException
                                    + " Unable to add Implicit Hydrogen at MoleculeID: " + tmpClone.getID());
                            throw new CDKException("Unexpected error occurred during implicit hydrogen adding at " +
                                    "hydrogen saturation of molecule: " + tmpClone.getID() + ", " + anException.toString(), anException);
                        }
                    }
                    tmpProcessedFragments.add(tmpAtomContainer);
                }
            }
        } catch (Exception anException) {
            ConjugatedPiSystemFragmenter.this.logger.log(Level.WARNING, anException
                    + "Error during hydrogen saturation at MoleculeID: " + tmpClone.getID());
            throw new IllegalArgumentException("Unexpected error occurred during fragmentation of molecule: "
                    + tmpClone.getID() + ", at hydrogen saturation: " + anException.toString(), anException);
        }
        //</editor-fold>

        return tmpProcessedFragments;
    }

    @Override
    public boolean shouldBeFiltered(IAtomContainer aMolecule) {
        return (Objects.isNull(aMolecule) || aMolecule.isEmpty());
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
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        return false;
    }

    @Override
    public boolean canBeFragmented(IAtomContainer aMolecule) throws NullPointerException {
        Objects.requireNonNull(aMolecule, "Given molecule is null");
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
     * @throws NullPointerException if the molecule is null
     */
    @Override
    public IAtomContainer applyPreprocessing(IAtomContainer aMolecule) throws NullPointerException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        return aMolecule;
    }
    //</editor-fold>
    private void clearCache(){
        this.atomArray = null;
        this.bondArray = null;
    }
    //
}
