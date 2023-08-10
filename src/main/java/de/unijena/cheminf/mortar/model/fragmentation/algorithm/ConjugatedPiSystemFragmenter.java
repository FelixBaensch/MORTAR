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
import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;

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
    private final SimpleEnumConstantNameProperty fragmentSaturationSetting;
    /**
     * All settings of this fragmenter, encapsulated in JavaFX properties for binding to GUI.
     */
    private final List<Property> settings;
    /**
     * Map to store pairs of {@literal <setting name, tooltip text>}.
     */
    private final HashMap<String, String> settingNameTooltipTextMap;
    /**
     * The logger responsible for this fragmenter.
     */
    private static final Logger logger = Logger.getLogger(ConjugatedPiSystemFragmenter.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="Class Constructor">
    /**
     * Constructor of this fragmenter class.
     */
    public ConjugatedPiSystemFragmenter(){
        this.settingNameTooltipTextMap = new HashMap<>(4,0.75f);
        this.settings = new ArrayList<>(1);
        this.fragmentSaturationSetting = new SimpleEnumConstantNameProperty(this, "Fragment saturation setting",
                IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT.name(), IMoleculeFragmenter.FragmentSaturationOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
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
        this.settings.add(this.fragmentSaturationSetting);
        this.settingNameTooltipTextMap.put(this.fragmentSaturationSetting.getName(),
                Message.get("ConjugatedPiSystemFragmenter.fragmentSaturationSetting.tooltip"));
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
        return this.settingNameTooltipTextMap;
    }

    @Override
    public String getFragmentationAlgorithmName() {
        return ConjugatedPiSystemFragmenter.ALGORITHM_NAME;
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
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
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
        ConjugatedPiSystemFragmenter tmpCopy = new ConjugatedPiSystemFragmenter();
        tmpCopy.setFragmentSaturationSetting(this.fragmentSaturationSetting.get());
        return tmpCopy;
    }

    @Override
    public void restoreDefaultSettings() {
        this.fragmentSaturationSetting.set(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT.name());

    }

    @Override
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {

        //<editor-fold desc="Molecule Cloning, Property and Arrays Set">
        IAtomContainer tmpClone = aMolecule.clone();
        IAtom[] tmpAtomArray = new IAtom[tmpClone.getAtomCount()];
        IBond[] tmpBondArray = new IBond[tmpClone.getBondCount()];
        int tmpCPSFAtomIndex = 0;
        int tmpCPSFBondIndex = 0;
        for (IAtom tmpAtom: tmpClone.atoms()) {
            if (tmpAtom != null) {
                tmpAtom.setProperty(ConjugatedPiSystemFragmenter.INTERNAL_CPSF_ATOM_INDEX_PROPERTY_KEY, tmpCPSFAtomIndex);
                tmpAtomArray[tmpCPSFAtomIndex] = tmpAtom;
                tmpCPSFAtomIndex++;
            }
        }
        for (IBond tmpBond: tmpClone.bonds()) {
            if (tmpBond != null) {
                tmpBond.setProperty(ConjugatedPiSystemFragmenter.INTERNAL_CPSF_BOND_INDEX_PROPERTY_KEY, tmpCPSFBondIndex);
                tmpBondArray[tmpCPSFBondIndex] = tmpBond;
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
                    tmpFragments.addAtom(tmpAtomArray[tmpAtomInteger]);
                }
                for (IBond tmpConjBond: tmpConjAtomContainer.bonds()) {
                    int tmpBondInteger = tmpConjBond.getProperty(ConjugatedPiSystemFragmenter.INTERNAL_CPSF_BOND_INDEX_PROPERTY_KEY);
                    tmpFragments.addBond(tmpBondArray[tmpBondInteger]);
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
                            tmpProcessedFragments.add(tmpAtomContainer);
                        } catch (CDKException anException) {
                            ConjugatedPiSystemFragmenter.this.logger.log(Level.WARNING, anException
                                    + " Unable to add Implicit Hydrogen at MoleculeID: " + tmpClone.getID());
                            throw new CDKException("Unexpected error occurred during implicit hydrogen adding at " +
                                    "hydrogen saturation of molecule: " + tmpClone.getID() + ", " + anException.toString(), anException);
                        }
                    } else {
                        tmpProcessedFragments.add(tmpAtomContainer);
                    }
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
    //
}
