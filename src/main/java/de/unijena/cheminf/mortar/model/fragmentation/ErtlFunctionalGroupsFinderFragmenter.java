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
 * - add method that receives list of molecules and returns list of unique fragments and returns list of molecules with pointers to fragments
 * - write doc
 */

import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IPseudoAtom;
import org.openscience.cdk.tools.ErtlFunctionalGroupsFinder;
import org.openscience.cdk.tools.ErtlFunctionalGroupsFinderUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * TODO
 */
public class ErtlFunctionalGroupsFinderFragmenter implements IMoleculeFragmenter {
    //<editor-fold desc="Enum FunctionalGroupEnvironmentMode">
    /**
     *
     */
    public static enum FunctionalGroupEnvironmentMode {
        /**
         *
         */
        GENERALIZATION(ErtlFunctionalGroupsFinder.Mode.DEFAULT),

        /**
         *
         */
        FULL_ENVIRONMENT(ErtlFunctionalGroupsFinder.Mode.NO_GENERALIZATION),

        /**
         *
         */
        NO_ENVIRONMENT(ErtlFunctionalGroupsFinder.Mode.DEFAULT);

        /**
         *
         */
        private final ErtlFunctionalGroupsFinder.Mode mode;

        /**
         *
         * @param aMode
         */
        FunctionalGroupEnvironmentMode(ErtlFunctionalGroupsFinder.Mode aMode) {
            this.mode = aMode;
        }

        /**
         *
         */
        public ErtlFunctionalGroupsFinder.Mode getAssociatedEFGFMode() {
            return this.mode;
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public static final constants">
    /**
     *
     */
    public static final String ALGORITHM_NAME = "Ertl algorithm";

    /**
     *
     */
    public static final Aromaticity AROMATICITY_MODEL_DEFAULT = new Aromaticity(ElectronDonation.daylight(),
            Cycles.or(Cycles.all(), Cycles.cdkAromaticSet()));

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
    private FunctionalGroupEnvironmentMode environmentMode;

    /**
     *
     */
    private Aromaticity aromaticityModel;

    /**
     *
     */
    private FragmentSaturationOptions fragmentSaturationSetting;
    //</editor-fold>

    //<editor-fold desc="Constructors">
    /**
     * TODO
     */
    public ErtlFunctionalGroupsFinderFragmenter() {
        //this is also the default of EFGF
        this(FunctionalGroupEnvironmentMode.GENERALIZATION);
    }

    /**
     *
     */
    public ErtlFunctionalGroupsFinderFragmenter(FunctionalGroupEnvironmentMode aMode) throws NullPointerException {
        Objects.requireNonNull(aMode, "Given mode is null.");
        this.environmentMode = aMode;
        this.EFGFinstance = new ErtlFunctionalGroupsFinder(this.environmentMode.getAssociatedEFGFMode());
        this.aromaticityModel = ErtlFunctionalGroupsFinderFragmenter.AROMATICITY_MODEL_DEFAULT;
        this.fragmentSaturationSetting = FragmentSaturationOptions.HYDROGEN_SATURATION;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties get">
    /**
     *
     * @return
     */
    public ErtlFunctionalGroupsFinderFragmenter.FunctionalGroupEnvironmentMode getEnvironmentMode() {
        return this.environmentMode;
    }

    /**
     *
     */
    public Aromaticity getAromaticityModel() {
        return this.aromaticityModel;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties set">
    /**
     *
     * @return
     */
    public void setEnvironmentMode(ErtlFunctionalGroupsFinderFragmenter.FunctionalGroupEnvironmentMode aMode)
            throws NullPointerException {
        Objects.requireNonNull(aMode, "Given mode is null.");
        this.environmentMode = aMode;
        this.EFGFinstance = new ErtlFunctionalGroupsFinder(this.environmentMode.getAssociatedEFGFMode());
    }

    public void setAromaticityModel(Aromaticity anAromaticityModel) throws NullPointerException {
        Objects.requireNonNull(anAromaticityModel, "Given model is null.");
        this.aromaticityModel = anAromaticityModel;
    }
    //</editor-fold>
    //
    //<editor-fold desc="IMoleculeFragmenter methods">
    @Override
    public String getFragmentationAlgorithmName() {
        return ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME;
    }

    @Override
    public void setFragmentSaturationSetting(FragmentSaturationOptions anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given saturation option is null.");
    }

    @Override
    public FragmentSaturationOptions getFragmentSaturationSetting() {
        return this.fragmentSaturationSetting;
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
                    ErtlFunctionalGroupsFinderFragmenter.FRAGMENT_CATEGORY_ALKANE_VALUE);
            return tmpReturnList;
        }
        boolean tmpCanBeFragmented = this.canBeFragmented(aMolecule);
        if (!tmpCanBeFragmented) {
            throw new IllegalArgumentException("Given molecule cannot be fragmented but should be filtered or preprocessed first.");
        }
        HashMap<Integer, IAtom> tmpIdToAtomMap = new HashMap<>(aMolecule.getAtomCount() + 1, 1);
        for (int i = 0; i < aMolecule.getAtomCount(); i++) {
            IAtom tmpAtom = aMolecule.getAtom(i);
            tmpAtom.setProperty("EFGFFragmenter.INDEX", i);
            tmpIdToAtomMap.put(i, tmpAtom);
        }
        List<IAtomContainer> tmpFunctionalGroupFragments;
        List<IAtomContainer> tmpNonFGFragments;
        try {
            if (this.environmentMode != FunctionalGroupEnvironmentMode.NO_ENVIRONMENT) {
                //generalization or full environment, can both be handled by EFGF
                tmpFunctionalGroupFragments = this.EFGFinstance.find(aMolecule, false);
            } else {
                tmpFunctionalGroupFragments = ErtlFunctionalGroupsFinderUtility.findMarkedAtoms(aMolecule);
            }
            //FG fragments are removed from molecule to get generate alkane fragments
            if (!tmpFunctionalGroupFragments.isEmpty()) {
                for (IAtomContainer tmpFunctionalGroup : tmpFunctionalGroupFragments) {
                    tmpFunctionalGroup.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                            ErtlFunctionalGroupsFinderFragmenter.FRAGMENT_CATEGORY_FUNCTIONAL_GROUP_VALUE);
                    if (this.fragmentSaturationSetting.equals(FragmentSaturationOptions.HYDROGEN_SATURATION)) {
                        IMoleculeFragmenter.saturateWithHydrogen(tmpFunctionalGroup);
                    }
                    for (IAtom tmpAtom : tmpFunctionalGroup.atoms()) {
                        if (Objects.isNull(tmpAtom.getProperty("EFGFFragmenter.INDEX"))) {
                            //else if construct unnecessary but left here for future individual treatment of cases
                            if (tmpAtom instanceof IPseudoAtom && "R".equals(((IPseudoAtom)tmpAtom).getLabel())) {
                                //atom is a pseudo atom added by the EFGF
                                continue;
                            } else if (tmpAtom.getSymbol().equals("C")){
                                //atom is an environmental C added by the EFGF
                                continue;
                            } else if (tmpAtom.getSymbol().equals("H")) {
                                //atom is an explicit H added by the EFGF
                                continue;
                            } else {
                                //unspecified additional atom
                                continue;
                            }
                        }
                        int tmpIndex = tmpAtom.getProperty("EFGFFragmenter.INDEX");
                        aMolecule.removeAtom(tmpIdToAtomMap.get(tmpIndex));
                    }
                }
                IAtomContainerSet tmpPartitionedMoietiesSet = ConnectivityChecker.partitionIntoMolecules(aMolecule);
                tmpNonFGFragments = new ArrayList<>(tmpPartitionedMoietiesSet.getAtomContainerCount());
                for (IAtomContainer tmpContainer : tmpPartitionedMoietiesSet.atomContainers()) {
                    tmpContainer.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                            ErtlFunctionalGroupsFinderFragmenter.FRAGMENT_CATEGORY_ALKANE_VALUE);
                    if (this.fragmentSaturationSetting.equals(FragmentSaturationOptions.HYDROGEN_SATURATION)) {
                        IMoleculeFragmenter.saturateWithHydrogen(tmpContainer);
                    }
                    tmpNonFGFragments.add(tmpContainer);
                }
            } else {
                //no FG identified
                List<IAtomContainer> tmpReturnList = new ArrayList<IAtomContainer>(1);
                tmpReturnList.add(0, aMolecule);
                aMolecule.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                        ErtlFunctionalGroupsFinderFragmenter.FRAGMENT_CATEGORY_ALKANE_VALUE);
                return tmpReturnList;
            }
        } catch(IllegalArgumentException | CDKException anException) {
            throw new IllegalArgumentException("An error occurred during fragmentation: " + anException.toString());
        }
        List<IAtomContainer> tmpFragments = new ArrayList<IAtomContainer>(tmpFunctionalGroupFragments.size() + tmpNonFGFragments.size());
        tmpFragments.addAll(tmpFunctionalGroupFragments);
        tmpFragments.addAll(tmpNonFGFragments);
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
            throw new IllegalArgumentException("Object at position 0 is null, should be the original molecule or a functional group fragment.");
        }
        String tmpCategory = aFragmentList.get(0).getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY);
        if (Objects.isNull(tmpCategory) || tmpCategory.isEmpty()) {
            throw new IllegalArgumentException("Object at position 0 has no or an incorrect fragment category property, should be the original molecule or a functional group fragment.");
        }
        return !(aFragmentList.size() == 1);
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
     *
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
        IAtomContainer tmpPreprocessedMolecule = ErtlFunctionalGroupsFinderUtility.applyFiltersAndPreprocessing(aMolecule,
                this.aromaticityModel);
        if (Objects.isNull(tmpPreprocessedMolecule)) {
            throw new IllegalArgumentException("The given molecule cannot be preprocessed but should be filtered.");
        } else {
            return tmpPreprocessedMolecule;
        }
    }

    /**
     *
     */
    @Override
    public void restoreDefaultSettings() {
        this.aromaticityModel = ErtlFunctionalGroupsFinderFragmenter.AROMATICITY_MODEL_DEFAULT;
        this.environmentMode = FunctionalGroupEnvironmentMode.GENERALIZATION;
        this.EFGFinstance = new ErtlFunctionalGroupsFinder(this.environmentMode.getAssociatedEFGFMode());
        this.fragmentSaturationSetting = FragmentSaturationOptions.HYDROGEN_SATURATION;
    }
    //</editor-fold>
}
