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

package de.unijena.cheminf.mortar.model.data;

/**
 * TODO:
 * - ....
 */

import javafx.scene.image.ImageView;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.Map;

/**
 * Central interface for implementing molecular data models.
 */
public interface IMolecularDataModel {
    //TODO: enum?
    //
    //<editor-fold desc="Public static final constants">
    //TODO: public static final constants
    //</editor-fold>
    //
    //<editor-fold desc="Public properties">
    //TODO: public properties??
    //</editor-fold>
    //
    //<editor-fold desc="Public methods">
    /**
     * Returns string unique SMILES
     * @return String uniqueSmiles
     */
    public String getUniqueSmiles();

    /**
     * Returns IAtomContainer
     * @return IAtomContainer
     * @throws CDKException if the SMILES string contained in uniqueSmiles is invalid   TODO??
     */
    public IAtomContainer getAtomContainer() throws CDKException;

    /**
     * Creates and returns ImageView of this fragment/molecule  TODO: find a generic term for fragment and molecule
     * @return ImageView of this fragment
     */
    public ImageView getStructure();

    /**
     * Returns property map of this fragment/molecule   TODO: find a generic term for fragment and molecule
     * @return property map
     */
    public Map getProperties();
    //</editor-fold>
    //
    //<editor-fold desc="Static methods">
    //TODO: static methods??
    //</editor-fold>
}
