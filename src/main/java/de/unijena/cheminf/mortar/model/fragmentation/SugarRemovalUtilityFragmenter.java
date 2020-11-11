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
 * - Copy SRU in the respective package and implement the IMoleculeFragmenter methods.
 */

import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.List;

/**
 * TODO
 */
public class SugarRemovalUtilityFragmenter implements IMoleculeFragmenter {
    /**
     * TODO
     */
    public SugarRemovalUtilityFragmenter() {

    }

    @Override
    public String getFragmentationAlgorithmName() {
        return null;
    }

    @Override
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule) throws IllegalArgumentException {
        return null;
    }

    @Override
    public boolean hasFragments(IAtomContainer aMolecule) throws IllegalArgumentException {
        return false;
    }

    @Override
    public boolean shouldBeFiltered(IAtomContainer aMolecule) throws NullPointerException {
        return false;
    }

    @Override
    public boolean shouldBePreprocessed(IAtomContainer aMolecule) throws NullPointerException {
        return false;
    }

    @Override
    public boolean canBeFragmented(IAtomContainer aMolecule) throws NullPointerException {
        return false;
    }

    @Override
    public void applyPreprocessing(IAtomContainer aMolecule) throws IllegalArgumentException {

    }
}
