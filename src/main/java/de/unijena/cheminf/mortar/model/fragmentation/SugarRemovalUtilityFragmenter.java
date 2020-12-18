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
 * - add option to generate valid valences on all returned fragments
 * - add method that receives list of molecules and returns list of unique fragments and list of molecules with pointers to fragments
 * - add option to separate unconnected fragments of the deglycosylated molecule into separate atom containers
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
    //<editor-fold desc="Enum SugarTypeToRemove">
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
    //</editor-fold>
    //
    //<editor-fold desc="Public static final constants">
    /**
     * TODO
     */
    public static final String ALGORITHM_NAME = "Sugar Removal Utility";

    /**
     *
     */
    public static final String FRAGMENT_CATEGORY_DEGLYCOSYLATED_CORE_VALUE = "SugarRemovalUtilityFragmenter.DeglycosylatedCore";

    /**
     *
     */
    public static final String FRAGMENT_CATEGORY_SUGAR_MOIETY_VALUE = "SugarRemovalUtilityFragmenter.SugarMoiety";

    /**
     *
     */
    public static final SugarTypeToRemove SUGAR_TYPE_TO_REMOVE_DEFAULT = SugarTypeToRemove.CIRCULAR_AND_LINEAR;
    //</editor-fold>
    //
    //<editor-fold desc="Private variables">
    /**
     *
     */
    private SugarTypeToRemove sugarTypeToRemove;
    //</editor-fold>
    //
    //<editor-fold desc="Constructor">
    /**
     * TODO
     * all settings in default, calls the SRU sole constructor
     */
    public SugarRemovalUtilityFragmenter() {
        super();
        this.sugarTypeToRemove = SugarRemovalUtilityFragmenter.SUGAR_TYPE_TO_REMOVE_DEFAULT;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties get">
    /**
     *
     * @return
     */
    public SugarTypeToRemove getSugarTypeToRemoveSetting() {
        return this.sugarTypeToRemove;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties set">
    /**
     *
     */
    public void setSugarTypeToRemoveSetting(SugarTypeToRemove aSugarTypeToRemove) throws NullPointerException {
        Objects.requireNonNull(aSugarTypeToRemove, "Given type of sugars to remove is null.");
        this.sugarTypeToRemove = aSugarTypeToRemove;
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
        return SugarRemovalUtilityFragmenter.ALGORITHM_NAME;
    }

    /**
     * Notice that the given atom container is altered!
     * @param aMolecule
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    @Override
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        if (aMolecule.isEmpty()) {
            List<IAtomContainer> tmpReturnList = new ArrayList<IAtomContainer>(1);
            tmpReturnList.add(0, aMolecule);
            aMolecule.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                    SugarRemovalUtilityFragmenter.FRAGMENT_CATEGORY_DEGLYCOSYLATED_CORE_VALUE);
            return tmpReturnList;
        }
        boolean tmpCanBeFragmented = this.canBeFragmented(aMolecule);
        if (!tmpCanBeFragmented) {
            throw new IllegalArgumentException("Given molecule cannot be fragmented but should be filtered or preprocessed first.");
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
        tmpFragments.get(0).setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                SugarRemovalUtilityFragmenter.FRAGMENT_CATEGORY_DEGLYCOSYLATED_CORE_VALUE);
        if (tmpFragments.size() > 1) {
            for (int i = 1; i < tmpFragments.size(); i++) {
                tmpFragments.get(i).setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                        SugarRemovalUtilityFragmenter.FRAGMENT_CATEGORY_SUGAR_MOIETY_VALUE);
            }
        }
        return tmpFragments;
    }

    /**
     *
     * @param aFragmentList
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    @Override
    public boolean hasFragments(List<IAtomContainer> aFragmentList) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(aFragmentList, "Given fragment list is null.");
        if (aFragmentList.size() == 0) {
            throw new IllegalArgumentException("Given fragment list is empty.");
        }
        if (Objects.isNull(aFragmentList.get(0))) {
            throw new IllegalArgumentException("Object at position 0 is null, should be the deglycosylated molecule.");
        }
        String tmpCategory = aFragmentList.get(0).getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY);
        if (Objects.isNull(tmpCategory) || tmpCategory.isEmpty() || !tmpCategory.equals(SugarRemovalUtilityFragmenter.FRAGMENT_CATEGORY_DEGLYCOSYLATED_CORE_VALUE)) {
            throw new IllegalArgumentException("Object at position 0 has no or an incorrect fragment category property, should be the deglycosylated molecule.");
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
        boolean tmpShouldBeFiltered = this.shouldBeFiltered(aMolecule);
        if (tmpShouldBeFiltered) {
            throw new IllegalArgumentException("The given molecule cannot be preprocessed but should be filtered.");
        }
        if (!this.shouldBePreprocessed(aMolecule)) {
            return aMolecule;
        }
        if (this.areOnlyTerminalSugarsRemoved()) {
            boolean tmpIsConnected = ConnectivityChecker.isConnected(aMolecule);
            if (!tmpIsConnected) {
                return SugarRemovalUtility.selectBiggestUnconnectedFragment(aMolecule);
            }
        }
        return aMolecule;
    }

    @Override
    public void restoreDefaultSettings() {
        super.restoreDefaultSettings();
        this.sugarTypeToRemove = SugarRemovalUtilityFragmenter.SUGAR_TYPE_TO_REMOVE_DEFAULT;
    }
    //</editor-fold>
}
