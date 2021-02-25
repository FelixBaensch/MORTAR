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

package de.unijena.cheminf.mortar.model.fragmentation;

/**
 * TODO:
 * - write doc
 * - make cycle finder configurable?
 */

import javafx.beans.property.SimpleStringProperty;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.graph.CycleFinder;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
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
    //<editor-fold desc="Enum FunctionalGroupEnvironmentModeOption">
    /**
     *
     */
    public static enum FunctionalGroupEnvironmentModeOption {
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
        FunctionalGroupEnvironmentModeOption(ErtlFunctionalGroupsFinder.Mode aMode) {
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
    //<editor-fold desc="Enum AromaticityModelOption">
    /**
     *
     */
    public static enum AromaticityModelOption {
        /**
         *
         */
        DAYLIGHT,

        /**
         *
         */
        CDK,

        /**
         *
         */
        CDK_ALLOWING_EXOCYCLIC,

        /**
         *
         */
        PI_BONDS;
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
    public static final String INTERNAL_INDEX_PROPERTY_KEY = "EFGFFragmenter.INDEX";

    /**
     *
     */
    public static final AromaticityModelOption AROMATICITY_MODEL_OPTION_DEFAULT = AromaticityModelOption.DAYLIGHT;

    /**
     *
     */
    public static final FunctionalGroupEnvironmentModeOption ENVIRONMENT_MODE_OPTION_DEFAULT = FunctionalGroupEnvironmentModeOption.GENERALIZATION;

    /**
     *
     */
    public static final String FRAGMENT_CATEGORY_FUNCTIONAL_GROUP_VALUE = "EFGFFragmenter.FunctionalGroup";

    /**
     *
     */
    public static final String FRAGMENT_CATEGORY_ALKANE_VALUE = "EFGFFragmenter.Alkane";
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
    private final SimpleStringProperty environmentModeProperty = new SimpleStringProperty(this, "environmentMode");

    /**
     *
     */
    private Aromaticity aromaticityModel;

    /**
     *
     */
    private final SimpleStringProperty aromaticityModelProperty = new SimpleStringProperty(this, "aromaticityModel");

    /**
     *
     */
    private final SimpleStringProperty fragmentSaturationProperty = new SimpleStringProperty(this, "fragmentSaturation");

    /**
     *
     */
    private CycleFinder cycleFinder;
    //</editor-fold>

    //<editor-fold desc="Constructors">
    /**
     * TODO
     */
    public ErtlFunctionalGroupsFinderFragmenter() {
        this(ErtlFunctionalGroupsFinderFragmenter.ENVIRONMENT_MODE_OPTION_DEFAULT);
    }

    /**
     *
     */
    public ErtlFunctionalGroupsFinderFragmenter(FunctionalGroupEnvironmentModeOption aMode) throws NullPointerException {
        Objects.requireNonNull(aMode, "Given mode is null.");
        this.cycleFinder = Cycles.or(Cycles.all(), Cycles.cdkAromaticSet());
        this.environmentModeProperty.set(aMode.name());
        this.EFGFinstance = new ErtlFunctionalGroupsFinder(FunctionalGroupEnvironmentModeOption.valueOf(
                this.environmentModeProperty.get()).getAssociatedEFGFMode());
        this.aromaticityModelProperty.set(ErtlFunctionalGroupsFinderFragmenter.AROMATICITY_MODEL_OPTION_DEFAULT.name());
        //this.aromaticityModel is set in the method
        this.setAromaticityModelSetting(AromaticityModelOption.valueOf(this.aromaticityModelProperty.get()));
        this.fragmentSaturationProperty.set(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT.name());
        this.environmentModeProperty.addListener((observable, oldValue, newValue)
                -> this.setEnvironmentModeSetting(FunctionalGroupEnvironmentModeOption.valueOf(this.environmentModeProperty.get())));
        this.aromaticityModelProperty.addListener((observable, oldValue, newValue)
                -> this.setAromaticityModelSetting(AromaticityModelOption.valueOf(this.aromaticityModelProperty.get())));
        //no listener needed for fragment saturation setting
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties get">
    /**
     *
     * @return
     */
    public FunctionalGroupEnvironmentModeOption getEnvironmentModeSetting() {
        return FunctionalGroupEnvironmentModeOption.valueOf(this.environmentModeProperty.get());
    }

    /**
     *
     */
    public SimpleStringProperty getEnvironmentModeProperty() {
        return this.environmentModeProperty;
    }

    /**
     *
     */
    public AromaticityModelOption getAromaticityModelSetting() {
        return AromaticityModelOption.valueOf(this.aromaticityModelProperty.get());
    }

    /**
     *
     */
    public SimpleStringProperty getAromaticityModelProperty() {
        return this.aromaticityModelProperty;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties set">
    /**
     *
     * @return
     */
    public void setEnvironmentModeSetting(FunctionalGroupEnvironmentModeOption aMode)
            throws NullPointerException {
        Objects.requireNonNull(aMode, "Given mode is null.");
        this.environmentModeProperty.set(aMode.name());
        this.EFGFinstance = new ErtlFunctionalGroupsFinder(FunctionalGroupEnvironmentModeOption.valueOf(this.environmentModeProperty.get()).getAssociatedEFGFMode());
    }

    public void setAromaticityModelSetting(AromaticityModelOption anAromaticityModel) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anAromaticityModel, "Given model is null.");
        switch (anAromaticityModel) {
            case CDK:
                this.aromaticityModel = new Aromaticity(ElectronDonation.cdk(), this.cycleFinder);
                break;
            case DAYLIGHT:
                this.aromaticityModel = new Aromaticity(ElectronDonation.daylight(), this.cycleFinder);
                break;
            case CDK_ALLOWING_EXOCYCLIC:
                this.aromaticityModel = new Aromaticity(ElectronDonation.cdkAllowingExocyclic(), this.cycleFinder);
                break;
            case PI_BONDS:
                this.aromaticityModel = new Aromaticity(ElectronDonation.piBonds(), this.cycleFinder);
                break;
            default:
                throw new IllegalArgumentException("Undefined aromaticity model option.");
        }
        this.aromaticityModelProperty.set(anAromaticityModel.name());
        return;
    }
    //</editor-fold>
    //
    //<editor-fold desc="IMoleculeFragmenter methods">

    @Override
    public String getFragmentationAlgorithmName() {
        return ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME;
    }

    @Override
    public void setFragmentSaturationSetting(FragmentSaturationOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given saturation option is null.");
        this.fragmentSaturationProperty.set(anOption.name());
    }

    @Override
    public FragmentSaturationOption getFragmentSaturationSetting() {
        return FragmentSaturationOption.valueOf(this.fragmentSaturationProperty.get());
    }

    /**
     *
     */
    @Override
    public void restoreDefaultSettings() {
        this.environmentModeProperty.set(ErtlFunctionalGroupsFinderFragmenter.ENVIRONMENT_MODE_OPTION_DEFAULT.name());
        this.EFGFinstance = new ErtlFunctionalGroupsFinder(FunctionalGroupEnvironmentModeOption.valueOf(
                this.environmentModeProperty.get()).getAssociatedEFGFMode());
        this.aromaticityModelProperty.set(ErtlFunctionalGroupsFinderFragmenter.AROMATICITY_MODEL_OPTION_DEFAULT.name());
        //this.aromaticityModel is set in the method
        this.setAromaticityModelSetting(AromaticityModelOption.valueOf(this.aromaticityModelProperty.get()));
        this.fragmentSaturationProperty.set(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT.name());
        //no need to reset cycle finder
    }

    /**
     * Notice that the given atom container is altered
     * If molecule is empty, it is returned inside a list as sole element
     * If no FG are identified, the molecule is returned as sole element in the list (one alkane fragment)
     *
     * @param aMolecule
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    @Override
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException {
        //<editor-fold desc="Parameter tests">
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
        //</editor-fold>
        HashMap<Integer, IAtom> tmpIdToAtomMap = new HashMap<>(aMolecule.getAtomCount() + 1, 1);
        for (int i = 0; i < aMolecule.getAtomCount(); i++) {
            IAtom tmpAtom = aMolecule.getAtom(i);
            tmpAtom.setProperty(ErtlFunctionalGroupsFinderFragmenter.INTERNAL_INDEX_PROPERTY_KEY, i);
            tmpIdToAtomMap.put(i, tmpAtom);
        }
        List<IAtomContainer> tmpFunctionalGroupFragments;
        List<IAtomContainer> tmpNonFGFragments;
        try {
            //generate FG fragments using EFGF
            if (this.environmentModeProperty.get() == FunctionalGroupEnvironmentModeOption.NO_ENVIRONMENT.name()) {
                //extract only marked atoms, use implemented utility method from EFGFUtilities
                tmpFunctionalGroupFragments = ErtlFunctionalGroupsFinderUtility.findMarkedAtoms(aMolecule);
            } else {
                //generalization or full environment, can both be handled by EFGF alone
                tmpFunctionalGroupFragments = this.EFGFinstance.find(aMolecule, false);
            }
            if (!tmpFunctionalGroupFragments.isEmpty()) {
                for (IAtomContainer tmpFunctionalGroup : tmpFunctionalGroupFragments) {
                    //post-processing FG fragments
                    tmpFunctionalGroup.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                            ErtlFunctionalGroupsFinderFragmenter.FRAGMENT_CATEGORY_FUNCTIONAL_GROUP_VALUE);
                    if (this.fragmentSaturationProperty.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION.name())) {
                        IMoleculeFragmenter.saturateWithHydrogen(tmpFunctionalGroup);
                    }
                    //FG fragments are removed from molecule to generate alkane fragments
                    for (IAtom tmpAtom : tmpFunctionalGroup.atoms()) {
                        if (!Objects.isNull(tmpAtom.getProperty(ErtlFunctionalGroupsFinderFragmenter.INTERNAL_INDEX_PROPERTY_KEY))) {
                            //FG fragments contain new atoms added by EFGF, they must be removed
                            int tmpIndex = tmpAtom.getProperty("EFGFFragmenter.INDEX");
                            aMolecule.removeAtom(tmpIdToAtomMap.get(tmpIndex));
                        }
                    }
                }
                //Partition unconnected alkane fragments in distinct atom containers
                IAtomContainerSet tmpPartitionedMoietiesSet = ConnectivityChecker.partitionIntoMolecules(aMolecule);
                tmpNonFGFragments = new ArrayList<>(tmpPartitionedMoietiesSet.getAtomContainerCount());
                for (IAtomContainer tmpContainer : tmpPartitionedMoietiesSet.atomContainers()) {
                    //post-processing of alkane fragments
                    tmpContainer.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                            ErtlFunctionalGroupsFinderFragmenter.FRAGMENT_CATEGORY_ALKANE_VALUE);
                    if (this.fragmentSaturationProperty.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION.name())) {
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
            throw new IllegalArgumentException("Object at position 0 is null, should be the original molecule or a " +
                    "functional group fragment.");
        }
        String tmpCategory = aFragmentList.get(0).getProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY);
        if (Objects.isNull(tmpCategory) || tmpCategory.isEmpty()) {
            throw new IllegalArgumentException("Object at position 0 has no or an incorrect fragment category property, " +
                    "should be the original molecule or a functional group fragment.");
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
    //</editor-fold>
}
