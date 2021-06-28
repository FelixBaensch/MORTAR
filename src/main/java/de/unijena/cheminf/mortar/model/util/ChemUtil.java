/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2020  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas-schaub@uni-jena.de)
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

import org.openscience.cdk.aromaticity.Kekulization;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;

import java.util.logging.Logger;

/**
 * Chemistry utility
 */
public class ChemUtil {
    //<editor-fold defaultstate="collapsed" desc="Public static final class constants">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(FileUtil.class.getName());
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public static methods">
    /**
     * TODO: has problems with creating the SMILES for the fragments -> why?? how to solve it?!
     * Creates a unique SMILES out of the given atom container. If no SMILES could be created, null is returned.
     *
     * @param anAtomContainer representing the atom container whose unique SMILES should be created
     * @return unique SMILES of the given atom container or null if an exception occurred
     * @throws CDKException
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    public static String createUniqueSmiles(IAtomContainer anAtomContainer) throws CDKException, NullPointerException, IllegalArgumentException {
        String tmpUniqueSmiles = "";
        SmilesGenerator tmpSmilesGen = new SmilesGenerator(SmiFlavor.Unique);
        try {
            tmpUniqueSmiles = tmpSmilesGen.create(anAtomContainer);
        } catch (CDKException anException) {
            Kekulization.kekulize(anAtomContainer);
            tmpUniqueSmiles = tmpSmilesGen.create(anAtomContainer);
        }
        return tmpUniqueSmiles;
    }
    // </editor-fold>
}
