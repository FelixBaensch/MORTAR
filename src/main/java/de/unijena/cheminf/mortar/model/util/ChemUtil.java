/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2021  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas-schaub@uni-jena.de)
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

import de.unijena.cheminf.mortar.model.io.Importer;
import org.openscience.cdk.aromaticity.Kekulization;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import java.util.ArrayList;
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
     * If the SMILES could not be created in first place, it is retried with a kekulized clone of the given atom
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
            ChemUtil.LOGGER.log(Level.SEVERE, anException.toString(), anException);
        }
        return tmpSmiles;
    }

    /**
     * Generates the molecular formula of a given atom container. If the molecular formula could not be generated, null
     * is returned.
     *
     * @param anAtomContainer AtomContainer the formula is generated of
     * @return the molecular formula of the given atom container
     * @author Samuel Behr
     */
    public static String generateMolecularFormula(IAtomContainer anAtomContainer) {
        IAtomContainer tmpAtomContainerClone = null;
        String tmpMolecularFormulaString = null;
        try {
            tmpAtomContainerClone = anAtomContainer.clone();
            //TODO: method: copyAndSuppressedHydrogens() or suppressHydrogens() from AtomContainerManipulator?
            ChemUtil.convertExplicitToImplicitHydrogens(tmpAtomContainerClone);
            IMolecularFormula tmpMolecularFormula = MolecularFormulaManipulator.getMolecularFormula(tmpAtomContainerClone);
            tmpMolecularFormulaString = MolecularFormulaManipulator.getString(tmpMolecularFormula);
        } catch (CloneNotSupportedException anException) {
            ChemUtil.LOGGER.log(Level.WARNING, anException.toString() + " molecule name: "
                    + tmpAtomContainerClone.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY), anException);
        }
        return tmpMolecularFormulaString;
    }

    /**
     * Converts all explicit hydrogen atoms in the given molecule to implicit hydrogens, increasing the respective counters
     * on the heavy atom objects. Note that the given atom container object is altered.
     *
     * @param aMolecule the structure the convert all explicit hydrogens of
     * @throws NullPointerException if the given molecule is null
     * @author Michael Wenk, Jonas Schaub
     */
    public static void convertExplicitToImplicitHydrogens(IAtomContainer aMolecule) throws NullPointerException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        if (aMolecule.isEmpty()) {
            return;
        }
        List<IAtom> tmpRemoveList = new ArrayList<>();
        IAtom tmpAtomB;
        for (IAtom tmpAtomA : aMolecule.atoms()) {
            //check each atom for whether it is a hydrogen;
            // if yes, increase the number of implicit hydrogens for its connected heavy atom
            if (tmpAtomA.getAtomicNumber().equals(1)) {
                tmpAtomB = aMolecule.getConnectedAtomsList(tmpAtomA).get(0);
                //precaution for unset property
                if (tmpAtomB.getImplicitHydrogenCount() == null) {
                    tmpAtomB.setImplicitHydrogenCount(0);
                }
                tmpAtomB.setImplicitHydrogenCount(tmpAtomB.getImplicitHydrogenCount() + 1);
                tmpRemoveList.add(tmpAtomA);
            }
        }
        //remove all explicit hydrogen atoms from the molecule
        for (IAtom tmpAtom : tmpRemoveList) {
            aMolecule.removeAtom(tmpAtom);
        }
    }
    //</editor-fold>
}
