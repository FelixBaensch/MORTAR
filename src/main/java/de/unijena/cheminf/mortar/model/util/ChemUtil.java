/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2022  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.model.util;

import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.io.Importer;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Kekulization;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.LonePairElectronChecker;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Chemistry utility
 *
 * @author Samuel Behr
 */
public final class ChemUtil {
    //<editor-fold defaultstate="collapsed" desc="Private static final class constants">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(ChemUtil.class.getName());
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public static methods">
    /**
     * Creates a unique SMILES string out of the given atom container or returns null, if the creation was not possible.
     * If the SMILES could not be created in the first place, it is retried with a kekulized clone of the given atom
     * container. Aromaticity information is encoded in the returned SMILES string, if there is any given.
     *
     * @param anAtomContainer atom container the unique SMILES should be created of
     * @return unique SMILES of the given atom container or 'null' if no creation was possible
     */
    public static String createUniqueSmiles(IAtomContainer anAtomContainer) {
        String tmpSmiles = null;
        SmilesGenerator tmpSmilesGen = new SmilesGenerator(SmiFlavor.Unique | SmiFlavor.UseAromaticSymbols);
        try {
            try {
                tmpSmiles = tmpSmilesGen.create(anAtomContainer);
            } catch (CDKException anException){
                try {
                    IAtomContainer tmpAtomContainer = anAtomContainer.clone();
                    Kekulization.kekulize(tmpAtomContainer);
                    tmpSmiles = tmpSmilesGen.create(tmpAtomContainer);
                } catch (CDKException anInnerException){
                    throw anInnerException;
                }
            }
        } catch (CDKException | NullPointerException | IllegalArgumentException | CloneNotSupportedException anException){
            ChemUtil.LOGGER.log(Level.SEVERE, anException.toString() + "; molecule name: " + anAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY), anException);
        }
        return tmpSmiles;
    }

    /**
     * Generates the molecular formula of a given atom container. If the molecular formula could not be generated, null
     * is returned.
     *
     * @param anAtomContainer AtomContainer to generate the molecular formula of
     * @return String containing the molecular formula of the given atom container
     * @author Samuel Behr
     */
    public static String generateMolecularFormula(IAtomContainer anAtomContainer) {
        IAtomContainer tmpAtomContainerClone = null;
        String tmpMolecularFormulaString = null;
        try {
            tmpAtomContainerClone = anAtomContainer.clone();
            AtomContainerManipulator.suppressHydrogens(tmpAtomContainerClone);
            IMolecularFormula tmpMolecularFormula = MolecularFormulaManipulator.getMolecularFormula(tmpAtomContainerClone);
            tmpMolecularFormulaString = MolecularFormulaManipulator.getString(tmpMolecularFormula);
        } catch (CloneNotSupportedException anException) {
            ChemUtil.LOGGER.log(Level.WARNING, anException.toString() + " molecule name: "
                    + anAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY), anException);
        }
        return tmpMolecularFormulaString;
    }

    /**
     * Checks whether 3D coordinates are set for all atoms in the given molecule data model.
     *
     * @param aMolecule to check for 3D coordinates
     * @return true if 3D coordinates are set for ALL atoms in the given molecule
     */
    public static boolean has3DCoordinates(MoleculeDataModel aMolecule){
        IAtomContainer tmpFragment;
        try{
            tmpFragment = aMolecule.getAtomContainer();
        } catch(CDKException anException){
            ChemUtil.LOGGER.log(Level.SEVERE, anException.toString() + "_" + aMolecule.getName(), anException);
            return false;
        }
        boolean tmpHas3DCoords = true;
        for(IAtom tmpAtom : tmpFragment.atoms()){
            if(tmpAtom.getPoint3d() != null){
                continue;
            } else {
                tmpHas3DCoords = false;
                break;
            }
        }
        return tmpHas3DCoords;
    }

    /**
     * Checks whether 2D coordinates are set for all atoms in the given molecule data model.
     *
     * @param aMolecule to check for 2D coordinates
     * @return true if 2D coordinates are set for ALL atoms in the given molecule
     */
    public static boolean has2DCoordinates(MoleculeDataModel aMolecule){
        IAtomContainer tmpFragment;
        try{
            tmpFragment = aMolecule.getAtomContainer();
        } catch(CDKException anException){
            ChemUtil.LOGGER.log(Level.SEVERE, anException.toString() + "_" + aMolecule.getName(), anException);
            return false;
        }
        boolean tmpHas2DCoords = true;
        for(IAtom tmpAtom : tmpFragment.atoms()){
            if(tmpAtom.getPoint2d() != null){
                continue;
            } else {
                tmpHas2DCoords = false;
                break;
            }
        }
        return tmpHas2DCoords;
    }

    /**
     * Checks all atoms in all molecules in the given list for 3D or 2D coordinates. If only one atom has neither,
     * false is returned.
     *
     * @param aListOfMolecules to check for 2D or 3D coordinates
     * @return true if 2D or 3D coordinates are set for EVERY atom in EVERY molecule in the given list
     */
    public static boolean checkMoleculeListForCoordinates(List<MoleculeDataModel> aListOfMolecules){
        if(aListOfMolecules == null || aListOfMolecules.size() == 0){
            return false;
        }
        boolean tmpHasCoords = true;
        for(MoleculeDataModel tmpMolecule : aListOfMolecules){
            if(!ChemUtil.has3DCoordinates(tmpMolecule) && !ChemUtil.has2DCoordinates(tmpMolecule)){
                tmpHasCoords = false;
                break;
            }
        }
        return tmpHasCoords;
    }

    /**
     * Saturates free valences in the given molecule or molecular fragment with implicit hydrogen atoms.
     *
     * @param aMolecule to saturate
     * @throws NullPointerException if the given molecule is null
     * @throws CDKException if atom types cannot be assigned to all atoms of the molecule
     */
    public static void saturateWithHydrogen(IAtomContainer aMolecule) throws NullPointerException, CDKException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        if (aMolecule.isEmpty()) {
            //fragments might be empty on purpose, e.g. when there is no aglycone in a molecule, so throw no exception
            return;
        }
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(aMolecule);
        CDKHydrogenAdder.getInstance(DefaultChemObjectBuilder.getInstance()).addImplicitHydrogens(aMolecule);
        //just a precaution, you never know what can happen with fragment atom containers
        for (IAtom tmpAtom : aMolecule.atoms()) {
            if (tmpAtom.getImplicitHydrogenCount() == CDKConstants.UNSET) {
                tmpAtom.setImplicitHydrogenCount(0);
            }
        }
    }

    /**
     * Checks whether atoms in the given molecule have free atom pairs correctly assigned if chemically needed. If not,
     * they are added.
     *
     * @param aMolecule to check for missing free electron pairs
     * @throws NullPointerException if the given molecule is null
     * @throws CDKException if atom types cannot be assigned to all atoms of the molecule
     */
    public static void checkAndCorrectElectronConfiguration(IAtomContainer aMolecule)
            throws NullPointerException, CDKException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        if (aMolecule.isEmpty()) {
            //fragments might be empty on purpose, e.g. when there is no aglycon in a molecule, so throw no exception
            return;
        }
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(aMolecule);
        LonePairElectronChecker tmpElectronChecker = new LonePairElectronChecker();
        try {
            tmpElectronChecker.saturate(aMolecule);
        } catch (NullPointerException aNullPointerException) {
            /* Too many fragment molecules cause this exception, the log file would be overflowing
            ChemUtil.LOGGER.log(Level.WARNING, "Saturation with lone electron pairs not possible, molecule name: "
                    + aMolecule.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY), aNullPointerException);
             */
        }
    }
    //</editor-fold>
}
