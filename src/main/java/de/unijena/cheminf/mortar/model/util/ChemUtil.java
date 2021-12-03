/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2021  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

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
    //</editor-fold>
}
