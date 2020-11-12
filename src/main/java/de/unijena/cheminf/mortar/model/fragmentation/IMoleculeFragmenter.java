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

package de.unijena.cheminf.mortar.model.fragmentation;

/**
 * TODO:
 * - Manage settings somehow through the interface? At least a 'restoreDefaultSettings()' method could be made mandatory here
 * - Add methods for uniquely identifying returned fragments (like the hash generator of the EFGF utilities)?
 * - how to manage intrinsically transported information (e.g. as properties of the returned atom containers), e.g.
 * 'this fragment is the deglycosylated core' or 'this is a (non-)FG fragment'? Create property names as constants here for
 * some of this information?
 */

import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.List;

/**
 * TODO
 */
public interface IMoleculeFragmenter {
    //<editor-fold desc="Public properties">
    /**
     * Returns a string representation of the algorithm name, e.g. "ErtlFunctionalGroupsFinder" or "Ertl algorithm".
     *
     * @return
     */
    public String getFragmentationAlgorithmName();
    //</editor-fold>
    //<editor-fold desc="Public methods">
    /**
     * Fragments the given molecule according to the respective algorithm and returns the resulting fragments.
     *
     * @param aMolecule
     * @return
     * @throws IllegalArgumentException
     */
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException;

    /**
     * Returns true if the given molecule has e.g. functional groups or sugar moieties that are detected by the respective
     * algorithm.
     *
     * @param aMolecule
     * @return
     * @throws IllegalArgumentException
     */
    public boolean hasFragments(IAtomContainer aMolecule, List<IAtomContainer> aFragmentList) throws NullPointerException, IllegalArgumentException;

    /**
     * Returns true if the given molecule cannot be fragmented by the respective algorithm, even after preprocessing.
     *
     * @param aMolecule
     * @return
     * @throws NullPointerException
     */
    public boolean shouldBeFiltered(IAtomContainer aMolecule) throws NullPointerException;

    /**
     * Returns true if the given molecule can be fragmented by the respective algorithm after preprocessing.
     *
     * @param aMolecule
     * @return
     * @throws NullPointerException
     */
    public boolean shouldBePreprocessed(IAtomContainer aMolecule) throws NullPointerException;

    /**
     * Returns true only if the given molecule can be passed to the central fragmentation method without any preprocessing
     * and without causing an exception.
     *
     * @param aMolecule
     * @return
     * @throws NullPointerException
     */
    public boolean canBeFragmented(IAtomContainer aMolecule) throws NullPointerException;

    /**
     * Applies the needed preprocessing for fragmentation to the given molecule. Throws an exception if the molecule
     * should be filtered.
     *
     * @param aMolecule
     * @throws IllegalArgumentException
     */
    public void applyPreprocessing(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException;
    //</editor-fold>
}
