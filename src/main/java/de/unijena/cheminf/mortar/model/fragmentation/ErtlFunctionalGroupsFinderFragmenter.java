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
 * - add option to return only the marked atoms as FG fragments, i.e. removing the environmental Cs
 * - write doc
 */

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IPseudoAtom;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.ErtlFunctionalGroupsFinder;
import org.openscience.cdk.tools.ErtlFunctionalGroupsFinderUtility;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.util.ArrayList;
import java.util.HashMap;
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
        this.aromaticityModel = ErtlFunctionalGroupsFinderFragmenter.AROMATICITY_MODEL_DEFAULT;
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
    public void setMode(ErtlFunctionalGroupsFinder.Mode aMode) throws NullPointerException {
        Objects.requireNonNull(aMode, "Given mode is null.");
        this.mode = aMode;
        this.EFGFinstance = new ErtlFunctionalGroupsFinder(this.mode);
    }

    public void setAromaticityModel(Aromaticity anAromaticityModel) throws NullPointerException {
        Objects.requireNonNull(anAromaticityModel, "Given model is null.");
        this.aromaticityModel = anAromaticityModel;
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
            tmpAtom.setProperty("INDEX", i);
            tmpIdToAtomMap.put(i, tmpAtom);
        }
        List<IAtomContainer> tmpFunctionalGroupFragments;
        List<IAtomContainer> tmpNonFGFragments;
        try {
            tmpFunctionalGroupFragments = this.EFGFinstance.find(aMolecule, false);
            if (!tmpFunctionalGroupFragments.isEmpty()) {
                for (IAtomContainer tmpFunctionalGroup : tmpFunctionalGroupFragments) {
                    tmpFunctionalGroup.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                            ErtlFunctionalGroupsFinderFragmenter.FRAGMENT_CATEGORY_FUNCTIONAL_GROUP_VALUE);
                    for (IAtom tmpAtom : tmpFunctionalGroup.atoms()) {
                        if (Objects.isNull(tmpAtom.getProperty("INDEX"))) {
                            //TODO remove debugging print-outs
                            if (tmpAtom instanceof IPseudoAtom && "R".equals(((IPseudoAtom)tmpAtom).getLabel())) {
                                //atom is a pseudo atom added by the EFGF
                                System.out.println("Found R atom");
                                continue;
                            } else if (tmpAtom.getSymbol().equals("C")){
                                //atom is an environmental C added by the EFGF
                                System.out.println("Found environmental C");
                                continue;
                            } else if (tmpAtom.getSymbol().equals("H")) {
                                //atom is an explicit H added by the EFGF
                                System.out.println("Found explicit H");
                                continue;
                            } else {
                                System.out.println("Found something else: " + tmpAtom);
                                continue;
                            }
                        }
                        int tmpIndex = tmpAtom.getProperty("INDEX");
                        aMolecule.removeAtom(tmpIdToAtomMap.get(tmpIndex));
                    }
                }
                //AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(aMolecule);
                //CDKHydrogenAdder.getInstance(DefaultChemObjectBuilder.getInstance()).addImplicitHydrogens(aMolecule);
                IAtomContainerSet tmpPartitionedMoietiesSet = ConnectivityChecker.partitionIntoMolecules(aMolecule);
                tmpNonFGFragments = new ArrayList<>(tmpPartitionedMoietiesSet.getAtomContainerCount());
                for (IAtomContainer tmpContainer : tmpPartitionedMoietiesSet.atomContainers()) {
                    tmpContainer.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                            ErtlFunctionalGroupsFinderFragmenter.FRAGMENT_CATEGORY_ALKANE_VALUE);
                    tmpNonFGFragments.add(tmpContainer);
                }
            } else {
                List<IAtomContainer> tmpReturnList = new ArrayList<IAtomContainer>(1);
                tmpReturnList.add(0, aMolecule);
                aMolecule.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                        ErtlFunctionalGroupsFinderFragmenter.FRAGMENT_CATEGORY_ALKANE_VALUE);
                return tmpReturnList;
            }
        } catch(IllegalArgumentException /*| CDKException*/ anException) {
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
    //</editor-fold>
}
