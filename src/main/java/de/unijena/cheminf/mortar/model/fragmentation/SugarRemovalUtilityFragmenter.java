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
 * - write doc
 */

import de.unijena.cheminf.deglycosylation.SugarRemovalUtility;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * TODO
 */
public class SugarRemovalUtilityFragmenter extends SugarRemovalUtility implements IMoleculeFragmenter {
    /**
     *
     */
    public static enum SugarTypeToRemove {
        /**
         *
         */
        CIRCULAR,

        /**
         *
         */
        LINEAR,

        /**
         *
         */
        CIRCULAR_AND_LINEAR;
    }

    /**
     * TODO
     */
    public static final String ALGORITHM_NAME = "Sugar Removal Utility";

    /**
     *
     */
    public static final SugarTypeToRemove SUGAR_TYPE_TO_REMOVE_DEFAULT = SugarTypeToRemove.CIRCULAR_AND_LINEAR;

    /**
     *
     */
    private SugarTypeToRemove sugarTypeToRemove;

    /**
     * TODO
     * all settings in default, calls the SRU sole constructor
     */
    public SugarRemovalUtilityFragmenter() {
        super();
        this.sugarTypeToRemove = SugarRemovalUtilityFragmenter.SUGAR_TYPE_TO_REMOVE_DEFAULT;
    }

    /**
     *
     * @return
     */
    public SugarTypeToRemove getSugarTypeToRemoveSetting() {
        return this.sugarTypeToRemove;
    }

    /**
     *
     */
    public void setSugarTypeToRemoveSetting(SugarTypeToRemove aSugarTypeToRemove) throws NullPointerException {
        Objects.requireNonNull(aSugarTypeToRemove, "Given type of sugars to remove is null.");
        this.sugarTypeToRemove = aSugarTypeToRemove;
    }

    @Override
    public String getFragmentationAlgorithmName() {
        return SugarRemovalUtilityFragmenter.ALGORITHM_NAME;
    }

    @Override
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        if (aMolecule.isEmpty()) {
            List<IAtomContainer> tmpReturnList = new ArrayList<IAtomContainer>(1);
            tmpReturnList.add(0, aMolecule);
            return tmpReturnList;
        }
        List<IAtomContainer> tmpFragments;
        try {
            switch (this.sugarTypeToRemove) {
                case CIRCULAR:
                    tmpFragments = this.removeAndReturnCircularSugars(aMolecule, false);
                    break;
                case LINEAR:
                    tmpFragments = this.removeAndReturnLinearSugars(aMolecule, false);
                    break;
                case CIRCULAR_AND_LINEAR:
                    tmpFragments = this.removeAndReturnCircularAndLinearSugars(aMolecule, false);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + this.sugarTypeToRemove);
            }
        } catch (IllegalArgumentException | CloneNotSupportedException anException) {
            throw new IllegalArgumentException("An error occurred during fragmentation: " + anException.toString());
        }
        return tmpFragments;
    }

    @Override
    public boolean hasFragments(List<IAtomContainer> aFragmentList) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(aFragmentList, "Given fragment list is null.");
        if (aFragmentList.size() == 0) {
            throw new IllegalArgumentException("Given fragment list is empty.");
        }
        if (Objects.isNull(aFragmentList.get(0))) {
            throw new IllegalArgumentException("Object at position 0 is null, should be the deglycosylated molecule.");
        }
        return !(aFragmentList.size() == 1);
    }

    @Override
    public boolean shouldBeFiltered(IAtomContainer aMolecule) {
        return (Objects.isNull(aMolecule) || aMolecule.isEmpty());
    }

    @Override
    public boolean shouldBePreprocessed(IAtomContainer aMolecule) throws NullPointerException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        if (this.areOnlyTerminalSugarsRemoved()) {
            boolean tmpIsConnected = ConnectivityChecker.isConnected(aMolecule);
            if (!tmpIsConnected) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canBeFragmented(IAtomContainer aMolecule) throws NullPointerException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        boolean tmpShouldBeFiltered = this.shouldBeFiltered(aMolecule);
        boolean tmpShouldBePreprocessed = this.shouldBePreprocessed(aMolecule);
        return !(tmpShouldBeFiltered || tmpShouldBePreprocessed);
    }

    @Override
    public IAtomContainer applyPreprocessing(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        if (this.areOnlyTerminalSugarsRemoved()) {
            boolean tmpIsConnected = ConnectivityChecker.isConnected(aMolecule);
            if (!tmpIsConnected) {
                return SugarRemovalUtility.selectBiggestUnconnectedFragment(aMolecule);
            }
        }
        return aMolecule;
    }
}
