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

import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.ErtlFunctionalGroupsFinder;
import org.openscience.cdk.tools.ErtlFunctionalGroupsFinderUtility;

import java.util.List;
import java.util.Objects;

/**
 * TODO
 */
public class ErtlFunctionalGroupsFinderFragmenter implements IMoleculeFragmenter {
    //<editor-fold desc="Public static final constants">
    /**
     *
     */
    public static final String ALGORITHM_NAME = "Ertl algorithm";

    /**
     *
     */
    //public static final Aromaticity AROMATICITY_MODEL_DEFAULT =

    /**
     *
     */
    public static final String FRAGMENT_CATEGORY_FUNCTIONAL_GROUP_VALUE = "ErtlFunctionalGroupsFinderFragmenter.FunctionalGroup";

    /**
     * TODO: is the 'opposite' of FG alkane? Wikipedia says, alkanes are acyclic. But apart from this, the definition fits.
     */
    public static final String FRAGMENT_CATEGORY_ALKANE_VALUE = "ErtlFunctionalGroupsFinderFragmenter.Alkane";
    //</editor-fold>
    //
    //<editor-fold desc="Private variables">
    /**
     *
     */
    private ErtlFunctionalGroupsFinder EFGFinstance;

    /**
     *
     */
    private ErtlFunctionalGroupsFinder.Mode mode;

    /**
     *
     */
    private Aromaticity aromaticityModel;
    //</editor-fold>

    //<editor-fold desc="Constructors">
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
    //</editor-fold>
    //
    //<editor-fold desc="Public properties get">
    /**
     *
     * @return
     */
    public ErtlFunctionalGroupsFinder.Mode getMode() {
        return this.mode;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties set">
    /**
     *
     * @return
     */
    public void setMode(ErtlFunctionalGroupsFinder.Mode aMode) throws NullPointerException {
        Objects.requireNonNull(aMode, "Given mode is null.");
        this.mode = aMode;
        this.EFGFinstance = new ErtlFunctionalGroupsFinder(this.mode);
    }
    //</editor-fold>
    //
    //<editor-fold desc="IMoleculeFragmenter methods">
    /**
     *
     * @return
     */
    @Override
    public String getFragmentationAlgorithmName() {
        return ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME;
    }

    /**
     * TODO
     * @param aMolecule
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    @Override
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException {
        return null;
    }

    /**
     * TODO
     * @param aFragmentList
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    @Override
    public boolean hasFragments(List<IAtomContainer> aFragmentList) throws NullPointerException, IllegalArgumentException {
        return false;
    }

    @Override
    public boolean shouldBeFiltered(IAtomContainer aMolecule) {
        if (Objects.isNull(aMolecule) || aMolecule.isEmpty()) {
            return true;
        }
        //throws NullpointerException if molecule is null
        return ErtlFunctionalGroupsFinderUtility.shouldBeFiltered(aMolecule);
    }

    @Override
    public boolean shouldBePreprocessed(IAtomContainer aMolecule) throws NullPointerException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        //throws NullpointerException if molecule is null
        return ErtlFunctionalGroupsFinderUtility.shouldBePreprocessed(aMolecule);
    }

    @Override
    public boolean canBeFragmented(IAtomContainer aMolecule) throws NullPointerException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        boolean tmpShouldBeFiltered = this.shouldBeFiltered(aMolecule);
        boolean tmpShouldBePreprocessed = this.shouldBePreprocessed(aMolecule);
        if (tmpShouldBeFiltered || tmpShouldBePreprocessed) {
            return false;
        }
        //throws NullpointerException if molecule is null
        return ErtlFunctionalGroupsFinderUtility.isValidArgumentForFindMethod(aMolecule);
    }

    /**
     * TODO
     * @param aMolecule
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    @Override
    public IAtomContainer applyPreprocessing(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        boolean tmpShouldBeFiltered = this.shouldBeFiltered(aMolecule);
        if (tmpShouldBeFiltered) {
            throw new IllegalArgumentException("The given molecule cannot be preprocessed but should be filtered.");
        }
        if (!this.shouldBePreprocessed(aMolecule)) {
            return aMolecule;
        }
        //return ErtlFunctionalGroupsFinderUtility.applyFiltersAndPreprocessing(aMolecule, );
        return null;
    }
    //</editor-fold>
}
