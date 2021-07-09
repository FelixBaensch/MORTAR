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

package de.unijena.cheminf.mortar.model.fragmentation.algorithm;

/**
 * TODO:
 * - write doc
 * - make cycle finder configurable (maybe in the future)
 * - see other todos
 */

import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;
import javafx.beans.property.Property;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper class that makes the Ertl algorithm for automatic identification and extraction of functional groups
 * available in MORTAR, using the ErtlFunctionalGroupsFinder.
 * TODO: Add references
 *
 * @author Jonas Schaub
 */
public class ErtlFunctionalGroupsFinderFragmenter implements IMoleculeFragmenter {
    //<editor-fold desc="Enum FGEnvOption">
    /**
     * Enum for options concerning the environment of returned functional group fragments.
     */
    public static enum FGEnvOption {
        /**
         * Generalize environments of functional groups.
         */
        GENERALIZATION(ErtlFunctionalGroupsFinder.Mode.DEFAULT),

        /**
         * Do not generalize but give the full environment of functional groups.
         */
        FULL_ENVIRONMENT(ErtlFunctionalGroupsFinder.Mode.NO_GENERALIZATION),

        /**
         * Return only the marked atoms of a functional group, no environment. The EFGF mode for generalization is
         * associated but the returned FG need additional processing to only return the marked atoms.
         */
        NO_ENVIRONMENT(ErtlFunctionalGroupsFinder.Mode.DEFAULT);

        /**
         * The ErtlFunctionalGroupsFinder mode to use in the respective cases.
         */
        private final ErtlFunctionalGroupsFinder.Mode mode;

        /**
         * Constructor.
         *
         * @param aMode the EFGF mode to use with the respective option
         */
        FGEnvOption(ErtlFunctionalGroupsFinder.Mode aMode) {
            this.mode = aMode;
        }

        /**
         * Returns the EFGF mode to use with the respective option.
         *
         * @return EFGF mode to use with this option
         */
        public ErtlFunctionalGroupsFinder.Mode getAssociatedEFGFMode() {
            return this.mode;
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Enum AromaticityModelOption">
    /**
     * Enum for available aromaticity/electron donation models. Utility for defining the options in a GUI. The electron
     * donation model specified in the constant name is used and a predefined cycle finder algorithm.
     */
    public static enum AromaticityModelOption {
        /**
         * Daylight electron donation model.
         */
        DAYLIGHT,

        /**
         * CDK electron donation model.
         */
        CDK,

        /**
         * CDK electron donation model that additionally allows exocyclic bonds to be part of the aromatic system.
         */
        CDK_ALLOWING_EXOCYCLIC,

        /**
         * Pi bonds electron donation model.
         */
        PI_BONDS;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Enum ReturnedFragmentsOption">
    /**
     * Enum for defining which fragments should be returned by the fragmentation methods, only the functional groups,
     * only the alkane fragments, or both.
     */
    public static enum ReturnedFragmentsOption {
        /**
         * Option to return only the identified functional groups of a molecule after fragmentation.
         */
        ONLY_FUNCTIONAL_GROUPS,

        /**
         * Option to return only the non-functional-group alkane fragments of a molecule after fragmentation.
         */
        ONLY_ALKANE_FRAGMENTS,

        /**
         * Option to return both, functional groups and alkane fragments, after fragmentation.
         */
        ALL_FRAGMENTS;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public static final constants">
    /**
     * Name of the used algorithm.
     */
    public static final String ALGORITHM_NAME = "Ertl algorithm";

    /**
     * Key for an index property that is used internally for unique identification of atoms in a given molecule.
     */
    public static final String INTERNAL_INDEX_PROPERTY_KEY = "EFGFFragmenter.INDEX";

    /**
     * Default aromaticity model.
     */
    public static final AromaticityModelOption AROMATICITY_MODEL_OPTION_DEFAULT = AromaticityModelOption.DAYLIGHT;

    /**
     * Default functional group environment option.
     */
    public static final FGEnvOption ENVIRONMENT_MODE_OPTION_DEFAULT = FGEnvOption.GENERALIZATION;

    /**
     * Default returned fragments option.
     */
    public static final ReturnedFragmentsOption RETURNED_FRAGMENTS_OPTION_DEFAULT = ReturnedFragmentsOption.ALL_FRAGMENTS;

    /**
     * Functional group fragments will be assigned this value for the property with key IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY.
     */
    public static final String FRAGMENT_CATEGORY_FUNCTIONAL_GROUP_VALUE = "EFGFFragmenter.FunctionalGroup";

    /**
     * Alkane fragments will be assigned this value for the property with key IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY.
     */
    public static final String FRAGMENT_CATEGORY_ALKANE_VALUE = "EFGFFragmenter.Alkane";
    //</editor-fold>
    //
    //<editor-fold desc="Private variables">
    /**
     * Instance of ErtlfFunctionalGroupsFinder class used to do the extraction of functional groups. If the FG
     * environment setting changes, this object needs to be reset in most of cases with the respectively needed mode.
     */
    private ErtlFunctionalGroupsFinder ertlFGFInstance;

    /**
     * The aromaticity model used for preprocessing prior to FG extraction.
     */
    private Aromaticity aromaticityModelInstance;
    //</editor-fold>
    //
    //<editor-fold desc="Private final variables">
    /**
     * A cycle finder instance for construction of the aromaticity model. The algorithm to use is currently defined in
     * the constructor and it is not configurable. This might change in the future.
     */
    private final CycleFinder cycleFinderInstance;

    /**
     * A property that has a constant name from the FGEnvOption enum as value.
     */
    private final SimpleEnumConstantNameProperty environmentModeSetting;

    /**
     * A property that has a constant name from the AromaticityModelOption enum as value.
     */
    private final SimpleEnumConstantNameProperty aromaticityModelSetting;

    /**
     * A property that has a constant name from the IMoleculeFragmenter.FragmentSaturationOption enum as value.
     */
    private final SimpleEnumConstantNameProperty fragmentSaturationSetting;

    /**
     * A property that has a constant name from ReturnedFragmentsOption enum as value.
     */
    private final SimpleEnumConstantNameProperty returnedFragmentsSetting;

    /**
     * All settings of this fragmenter, encapsulated in JavaFX properties for binding in GUI.
     */
    private final List<Property> settings;

    /**
     * Logger of this class
     */
    private final Logger logger = Logger.getLogger(ErtlFunctionalGroupsFinderFragmenter.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="Constructors">
    /**
     * TODO
     */
    public ErtlFunctionalGroupsFinderFragmenter() {
        this(ErtlFunctionalGroupsFinderFragmenter.ENVIRONMENT_MODE_OPTION_DEFAULT);
    }

    /**
     * TODO
     */
    public ErtlFunctionalGroupsFinderFragmenter(FGEnvOption aMode) throws NullPointerException {
        Objects.requireNonNull(aMode, "Given mode is null.");
        this.cycleFinderInstance = Cycles.or(Cycles.all(), Cycles.cdkAromaticSet());
        this.fragmentSaturationSetting = new SimpleEnumConstantNameProperty(this, "Fragment saturation setting",
                IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT.name(), IMoleculeFragmenter.FragmentSaturationOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ErtlFunctionalGroupsFinderFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.GuiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        this.environmentModeSetting = new SimpleEnumConstantNameProperty(this, "Environment mode setting",
                aMode.name(), ErtlFunctionalGroupsFinderFragmenter.FGEnvOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ErtlFunctionalGroupsFinderFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.GuiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                //throws no exception if super.set() throws no exception
                ErtlFunctionalGroupsFinderFragmenter.this.setErtlFGFInstance(FGEnvOption.valueOf(newValue));
            }
        };
        this.returnedFragmentsSetting = new SimpleEnumConstantNameProperty(this, "Returned fragments setting",
                ErtlFunctionalGroupsFinderFragmenter.RETURNED_FRAGMENTS_OPTION_DEFAULT.name(), ErtlFunctionalGroupsFinderFragmenter.ReturnedFragmentsOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ErtlFunctionalGroupsFinderFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.GuiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
            }
        };
        //initialisation of EFGF instance
        this.setErtlFGFInstance(FGEnvOption.valueOf(this.environmentModeSetting.get()));
        this.aromaticityModelSetting = new SimpleEnumConstantNameProperty(this, "Aromaticity model setting",
                ErtlFunctionalGroupsFinderFragmenter.AROMATICITY_MODEL_OPTION_DEFAULT.name(),
                ErtlFunctionalGroupsFinderFragmenter.AromaticityModelOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                try {
                    //call to super.set() for parameter checks
                    super.set(newValue);
                } catch (NullPointerException | IllegalArgumentException anException) {
                    ErtlFunctionalGroupsFinderFragmenter.this.logger.log(Level.WARNING, anException.toString(), anException);
                    GuiUtil.GuiExceptionAlert("Illegal Argument", "Illegal Argument was set", anException.toString(), anException);
                    //re-throws the exception to properly reset the binding
                    throw anException;
                }
                //throws no exception if super.set() throws no exception
                ErtlFunctionalGroupsFinderFragmenter.this.setAromaticityModelInstance(AromaticityModelOption.valueOf(newValue));
            }
        };
        //aromaticity model instance is set in method
        this.setAromaticityModelInstance(AromaticityModelOption.valueOf(this.aromaticityModelSetting.get()));
        this.settings = new ArrayList<Property>(4);
        this.settings.add(this.fragmentSaturationSetting);
        this.settings.add(this.aromaticityModelSetting);
        this.settings.add(this.environmentModeSetting);
        this.settings.add(this.returnedFragmentsSetting);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties get">
    /**
     *
     * @return
     */
    public String getEnvironmentModeSetting() {
        return this.environmentModeSetting.get();
    }

    /**
     *
     */
    public SimpleEnumConstantNameProperty environmentModeSettingProperty() {
        return this.environmentModeSetting;
    }

    /**
     *
     */
    public FGEnvOption getEnvironmentModeSettingConstant() {
        return FGEnvOption.valueOf(this.environmentModeSetting.get());
    }

    /**
     *
     */
    public String getAromaticityModelSetting() {
        return this.aromaticityModelSetting.get();
    }

    /**
     *
     */
    public SimpleEnumConstantNameProperty aromaticityModelSettingProperty() {
        return this.aromaticityModelSetting;
    }

    /**
     *
     */
    public AromaticityModelOption getAromaticityModelSettingConstant() {
        return AromaticityModelOption.valueOf(this.aromaticityModelSetting.get());
    }

    /**
     *
     */
    public SimpleEnumConstantNameProperty returnedFragmentsSettingProperty() {
        return this.returnedFragmentsSetting;
    }

    /**
     *
     */
    public ReturnedFragmentsOption getReturnedFragmentsSettingConstant() {
        return ReturnedFragmentsOption.valueOf(this.returnedFragmentsSetting.get());
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties set">
    /**
     *
     */
    public void setEnvironmentModeSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        FGEnvOption tmpConstant = FGEnvOption.valueOf(anOptionName);
        this.setEnvironmentModeSetting(tmpConstant);
    }

    /**
     *
     * @return
     */
    public void setEnvironmentModeSetting(FGEnvOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        //synchronisation with EFGF instance done in overridden set() function of the property
        this.environmentModeSetting.set(anOption.name());
    }

    /**
     *
     */
    public void setAromaticityModelSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        AromaticityModelOption tmpConstant = AromaticityModelOption.valueOf(anOptionName);
        this.setAromaticityModelSetting(tmpConstant);
    }

    /**
     *
     * @param anOption
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    public void setAromaticityModelSetting(AromaticityModelOption anOption) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOption, "Given option is null.");
        //synchronisation with aromaticity model instance done in overridden set() function of the property
        this.aromaticityModelSetting.set(anOption.name());
    }

    /**
     *
     * @return
     */
    public void setReturnedFragmentsSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        ReturnedFragmentsOption tmpConstant = ReturnedFragmentsOption.valueOf(anOptionName);
        this.setReturnedFragmentsSetting(tmpConstant);
    }

    /**
     *
     * @return
     */
    public void setReturnedFragmentsSetting(ReturnedFragmentsOption anOption) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOption, "Given option is null.");
        this.returnedFragmentsSetting.set(anOption.name());
    }
    //</editor-fold>
    //
    //<editor-fold desc="IMoleculeFragmenter methods">

    @Override
    public List<Property> settingsProperties() {
        return this.settings;
    }

    @Override
    public String getFragmentationAlgorithmName() {
        return ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME;
    }

    @Override
    public String getFragmentSaturationSetting() {
        return this.fragmentSaturationSetting.get();
    }

    @Override
    public SimpleEnumConstantNameProperty fragmentSaturationSettingProperty() {
        return this.fragmentSaturationSetting;
    }

    @Override
    public FragmentSaturationOption getFragmentSaturationSettingConstant() {
        return FragmentSaturationOption.valueOf(this.fragmentSaturationSetting.get());
    }

    @Override
    public void setFragmentSaturationSetting(String anOptionName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anOptionName, "Given saturation option name is null.");
        //throws IllegalArgumentException if the given name does not match a constant name in the enum
        FragmentSaturationOption tmpConstant = FragmentSaturationOption.valueOf(anOptionName);
        this.fragmentSaturationSetting.set(tmpConstant.name());
    }

    @Override
    public void setFragmentSaturationSetting(FragmentSaturationOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given saturation option is null.");
        this.fragmentSaturationSetting.set(anOption.name());
    }

    @Override
    public IMoleculeFragmenter copy() {
        ErtlFunctionalGroupsFinderFragmenter tmpCopy = new ErtlFunctionalGroupsFinderFragmenter();
        tmpCopy.setEnvironmentModeSetting(this.environmentModeSetting.get());
        tmpCopy.setAromaticityModelSetting(this.aromaticityModelSetting.get());
        tmpCopy.setFragmentSaturationSetting(this.fragmentSaturationSetting.get());
        tmpCopy.setReturnedFragmentsSetting(this.returnedFragmentsSetting.get());
        return tmpCopy;
    }

    @Override
    public void restoreDefaultSettings() {
        this.environmentModeSetting.set(ErtlFunctionalGroupsFinderFragmenter.ENVIRONMENT_MODE_OPTION_DEFAULT.name());
        //this.EFGFInstance is set in the method
        this.setErtlFGFInstance(FGEnvOption.valueOf(this.environmentModeSetting.get()));
        this.aromaticityModelSetting.set(ErtlFunctionalGroupsFinderFragmenter.AROMATICITY_MODEL_OPTION_DEFAULT.name());
        //this.aromaticityModel is set in the method
        this.setAromaticityModelInstance(AromaticityModelOption.valueOf(this.aromaticityModelSetting.get()));
        this.fragmentSaturationSetting.set(IMoleculeFragmenter.FRAGMENT_SATURATION_OPTION_DEFAULT.name());
        this.returnedFragmentsSetting.set(ErtlFunctionalGroupsFinderFragmenter.RETURNED_FRAGMENTS_OPTION_DEFAULT.name());
        //no need to reset cycle finder
    }

    /**
     * TODO
     * If molecule is empty, it is returned inside a list as sole element
     * If no FG are identified, the molecule is returned as sole element in the list (one alkane fragment, ignoring the
     * return fragment setting!)
     *
     * @param aMolecule
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     * @throws CloneNotSupportedException
     */
    @Override
    public List<IAtomContainer> fragmentMolecule(IAtomContainer aMolecule)
            throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        //<editor-fold desc="Parameter tests">
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        if (aMolecule.isEmpty()) {
            IAtomContainer tmpClone = aMolecule.clone();
            List<IAtomContainer> tmpReturnList = new ArrayList<IAtomContainer>(1);
            tmpReturnList.add(0, tmpClone);
            tmpClone.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                    ErtlFunctionalGroupsFinderFragmenter.FRAGMENT_CATEGORY_ALKANE_VALUE);
            return tmpReturnList;
        }
        boolean tmpCanBeFragmented = this.canBeFragmented(aMolecule);
        if (!tmpCanBeFragmented) {
            throw new IllegalArgumentException("Given molecule cannot be fragmented but should be filtered or preprocessed first.");
        }
        //</editor-fold>
        IAtomContainer tmpMoleculeClone = aMolecule.clone();
        try {
            tmpMoleculeClone = ErtlFunctionalGroupsFinderUtility.perceiveAtomTypesAndConfigureAtoms(tmpMoleculeClone);
            tmpMoleculeClone = ErtlFunctionalGroupsFinderUtility.applyAromaticityDetection(tmpMoleculeClone, this.aromaticityModelInstance);
        } catch (CDKException anException) {
            this.logger.log(Level.WARNING, anException.toString(), anException);
            throw new IllegalArgumentException("Unexpected error at aromaticity detection: " + anException.toString());
        }
        HashMap<Integer, IAtom> tmpIdToAtomMap = new HashMap<>(tmpMoleculeClone.getAtomCount() + 1, 1);
        for (int i = 0; i < tmpMoleculeClone.getAtomCount(); i++) {
            IAtom tmpAtom = tmpMoleculeClone.getAtom(i);
            tmpAtom.setProperty(ErtlFunctionalGroupsFinderFragmenter.INTERNAL_INDEX_PROPERTY_KEY, i);
            tmpIdToAtomMap.put(i, tmpAtom);
        }
        List<IAtomContainer> tmpFunctionalGroupFragments;
        List<IAtomContainer> tmpNonFGFragments;
        try {
            //generate FG fragments using EFGF
            if (this.environmentModeSetting.get().equals(FGEnvOption.NO_ENVIRONMENT.name())) {
                //extract only marked atoms, use implemented utility method from EFGFUtilities
                tmpFunctionalGroupFragments = ErtlFunctionalGroupsFinderUtility.findMarkedAtoms(tmpMoleculeClone);
            } else {
                //generalization or full environment, can both be handled by EFGF alone
                tmpFunctionalGroupFragments = this.ertlFGFInstance.find(tmpMoleculeClone, false);
            }
            if (!tmpFunctionalGroupFragments.isEmpty()) {
                for (IAtomContainer tmpFunctionalGroup : tmpFunctionalGroupFragments) {
                    //post-processing FG fragments
                    tmpFunctionalGroup.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                            ErtlFunctionalGroupsFinderFragmenter.FRAGMENT_CATEGORY_FUNCTIONAL_GROUP_VALUE);
                    if (this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION.name())) {
                        IMoleculeFragmenter.saturateWithHydrogen(tmpFunctionalGroup);
                    }
                    //FG fragments are removed from molecule to generate alkane fragments
                    for (IAtom tmpAtom : tmpFunctionalGroup.atoms()) {
                        if (!Objects.isNull(tmpAtom.getProperty(ErtlFunctionalGroupsFinderFragmenter.INTERNAL_INDEX_PROPERTY_KEY))) {
                            //FG fragments contain new atoms added by EFGF, they must be removed
                            int tmpIndex = tmpAtom.getProperty("EFGFFragmenter.INDEX");
                            tmpMoleculeClone.removeAtom(tmpIdToAtomMap.get(tmpIndex));
                        }
                    }
                }
                //Partition unconnected alkane fragments in distinct atom containers
                IAtomContainerSet tmpPartitionedMoietiesSet = ConnectivityChecker.partitionIntoMolecules(tmpMoleculeClone);
                tmpNonFGFragments = new ArrayList<>(tmpPartitionedMoietiesSet.getAtomContainerCount());
                for (IAtomContainer tmpContainer : tmpPartitionedMoietiesSet.atomContainers()) {
                    //post-processing of alkane fragments
                    tmpContainer.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                            ErtlFunctionalGroupsFinderFragmenter.FRAGMENT_CATEGORY_ALKANE_VALUE);
                    if (this.fragmentSaturationSetting.get().equals(FragmentSaturationOption.HYDROGEN_SATURATION.name())) {
                        IMoleculeFragmenter.saturateWithHydrogen(tmpContainer);
                    }
                    tmpNonFGFragments.add(tmpContainer);
                }
            } else {
                //no FG identified
                List<IAtomContainer> tmpReturnList = new ArrayList<IAtomContainer>(1);
                tmpReturnList.add(0, tmpMoleculeClone);
                tmpMoleculeClone.setProperty(IMoleculeFragmenter.FRAGMENT_CATEGORY_PROPERTY_KEY,
                        ErtlFunctionalGroupsFinderFragmenter.FRAGMENT_CATEGORY_ALKANE_VALUE);
                return tmpReturnList;
            }
        } catch(Exception anException) {
            throw new IllegalArgumentException("An error occurred during fragmentation: " + anException.toString());
        }
        List<IAtomContainer> tmpFragments = new ArrayList<IAtomContainer>(tmpFunctionalGroupFragments.size() + tmpNonFGFragments.size());
        if (this.returnedFragmentsSetting.get().equals(ReturnedFragmentsOption.ALL_FRAGMENTS.name())) {
            tmpFragments.addAll(tmpFunctionalGroupFragments);
            tmpFragments.addAll(tmpNonFGFragments);
        } else if (this.returnedFragmentsSetting.get().equals(ReturnedFragmentsOption.ONLY_FUNCTIONAL_GROUPS.name())) {
            //TODO: In this case, the alkane fragments do not need to be calculated above
            tmpFragments.addAll(tmpFunctionalGroupFragments);
        } else if (this.returnedFragmentsSetting.get().equals(ReturnedFragmentsOption.ONLY_ALKANE_FRAGMENTS.name())) {
            tmpFragments.addAll(tmpNonFGFragments);
        } else {
            throw new IllegalStateException("Unknown return fragments setting option has been set.");
        }
        return tmpFragments;
    }

    /**
     * TODO
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
                    "should be the original molecule, an alkane or a functional group fragment.");
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
     * TODO
     *
     * @param aMolecule
     * @return
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    @Override
    public IAtomContainer applyPreprocessing(IAtomContainer aMolecule) throws NullPointerException, IllegalArgumentException, CloneNotSupportedException {
        Objects.requireNonNull(aMolecule, "Given molecule is null.");
        boolean tmpShouldBeFiltered = this.shouldBeFiltered(aMolecule);
        if (tmpShouldBeFiltered) {
            throw new IllegalArgumentException("The given molecule cannot be preprocessed but should be filtered.");
        }
        if (!this.shouldBePreprocessed(aMolecule)) {
            return aMolecule.clone();
        }
        IAtomContainer tmpPreprocessedMolecule = aMolecule.clone();
        if (ErtlFunctionalGroupsFinderUtility.isStructureUnconnected(tmpPreprocessedMolecule)) {
            tmpPreprocessedMolecule = ErtlFunctionalGroupsFinderUtility.selectBiggestUnconnectedComponent(tmpPreprocessedMolecule);
        }
        if (ErtlFunctionalGroupsFinderUtility.isMoleculeCharged(tmpPreprocessedMolecule)) {
            try {
                tmpPreprocessedMolecule = ErtlFunctionalGroupsFinderUtility.neutralizeCharges(tmpPreprocessedMolecule);
            } catch (CDKException anException) {
                this.logger.log(Level.WARNING, anException.toString(), anException);
                throw new IllegalArgumentException("Unexpected error at aromaticity detection: " + anException.toString());
            }
        }
        if (Objects.isNull(tmpPreprocessedMolecule)) {
            throw new IllegalArgumentException("The given molecule cannot be preprocessed but should be filtered.");
        } else {
            return tmpPreprocessedMolecule;
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private methods">
    /**
     * Sets only the instance, not the property! So its safe for the property to call this method when overriding set().
     */
    private void setAromaticityModelInstance(AromaticityModelOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        switch (anOption) {
            case CDK:
                this.aromaticityModelInstance = new Aromaticity(ElectronDonation.cdk(), this.cycleFinderInstance);
                break;
            case DAYLIGHT:
                this.aromaticityModelInstance = new Aromaticity(ElectronDonation.daylight(), this.cycleFinderInstance);
                break;
            case CDK_ALLOWING_EXOCYCLIC:
                this.aromaticityModelInstance = new Aromaticity(ElectronDonation.cdkAllowingExocyclic(), this.cycleFinderInstance);
                break;
            case PI_BONDS:
                this.aromaticityModelInstance = new Aromaticity(ElectronDonation.piBonds(), this.cycleFinderInstance);
                break;
            default:
                throw new IllegalArgumentException("Undefined aromaticity model option.");
        }
    }

    /**
     * Sets only the instance, not the property! So its safe for the property to call this method when overriding set().
     */
    private void setErtlFGFInstance(FGEnvOption anOption) throws NullPointerException {
        Objects.requireNonNull(anOption, "Given option is null.");
        this.ertlFGFInstance = new ErtlFunctionalGroupsFinder(anOption.getAssociatedEFGFMode());
    }
    //</editor-fold>
}
