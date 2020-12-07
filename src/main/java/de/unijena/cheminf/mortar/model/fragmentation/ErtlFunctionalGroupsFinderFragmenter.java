/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2020  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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
 * - implement the IMoleculeFragmenter methods.
 */

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.ErtlFunctionalGroupsFinder;
import org.openscience.cdk.tools.ErtlFunctionalGroupsFinderUtility;

import java.util.List;
import java.util.Objects;

/**
 * TODO
 */
public class ErtlFunctionalGroupsFinderFragmenter implements IMoleculeFragmenter {
    /**
     *
     */
    public static final String ALGORITHM_NAME = "Ertl algorithm";

    /**
     *
     */
    private ErtlFunctionalGroupsFinder EFGFinstance;

    /**
     *
     */
    private ErtlFunctionalGroupsFinder.Mode mode;

    /**
     * TODO
     */
    public ErtlFunctionalGroupsFinderFragmenter() {
        this(ErtlFunctionalGroupsFinder.Mode.DEFAULT);
    }

    /**
     *
     */
    public ErtlFunctionalGroupsFinderFragmenter(ErtlFunctionalGroupsFinder.Mode aMode) throws NullPointerException {
        Objects.requireNonNull(aMode, "Given mode is null.");
        this.mode = aMode;
        this.EFGFinstance = new ErtlFunctionalGroupsFinder(this.mode);
    }

    /**
     *
     * @return
     */
    public ErtlFunctionalGroupsFinder.Mode getMode() {
        return this.mode;
    }

    /**
     *
     * @return
     */
    public void setMode(ErtlFunctionalGroupsFinder.Mode aMode) throws NullPointerException {
        Objects.requireNonNull(aMode, "Given mode is null.");
        this.mode = aMode;
        this.EFGFinstance = new ErtlFunctionalGroupsFinder(this.mode);
    }

    @Override
    public String getFragmentationAlgorithmName() {
        return ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME;
    }

    @Override
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException {
        return null;
    }

    @Override
    public boolean hasFragments(List<IAtomContainer> aFragmentList) throws NullPointerException, IllegalArgumentException {
        return false;
    }

    @Override
    public boolean shouldBeFiltered(IAtomContainer aMolecule) {
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
    public IAtomContainer applyPreprocessing(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException {
        return null;
    }
}
