/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2024  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
 *
 * Source code is available at <https://github.com/FelixBaensch/MORTAR>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.unijena.cheminf.mortar.model.fragmentation;

import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.ErtlFunctionalGroupsFinderFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.IMoleculeFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.ScaffoldGeneratorFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.SugarRemovalUtilityFragmenter;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.ChemUtil;
import de.unijena.cheminf.mortar.model.util.CollectionUtil;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import de.unijena.cheminf.mortar.preference.PreferenceContainer;
import de.unijena.cheminf.mortar.preference.PreferenceUtil;
import de.unijena.cheminf.mortar.preference.SingleIntegerPreference;
import de.unijena.cheminf.mortar.preference.SingleTermPreference;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Alert;

import org.openscience.cdk.exception.CDKException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class for fragmentation, single and in a pipeline.
 *
 * @author Jonas Schaub, Felix Baensch
 * @version 1.0.0.1
 */
public class FragmentationService {
    //<editor-fold desc="public static final constants">
    /**
     * Default selected fragmenter.
     */
    public static final String DEFAULT_SELECTED_FRAGMENTER_ALGORITHM_NAME = ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME;

    /**
     * Default pipeline name.
     */
    public static final String DEFAULT_PIPELINE_NAME = Message.get("FragmentationService.defaultPipelineName");

    /**
     * Subfolder name in the settings directory where the fragmenter settings are persisted.
     */
    public static final String FRAGMENTER_SETTINGS_SUBFOLDER_NAME = "Fragmenter_Settings";

    /**
     * Subfolder name in the settings directory where the fragmentation service settings and the pipeline settings are persisted.
     */
    public static final String FRAGMENTATION_SERVICE_SETTINGS_SUBFOLDER_NAME = "Fragmentation_Service_Settings";

    /**
     * File name to persist the fragmentation service settings.
     */
    public static final String FRAGMENTATION_SERVICE_SETTINGS_FILE_NAME = "FragmentationServiceSettings";

    /**
     * Name for the selected fragmenter setting for persistence.
     */
    public static final String SELECTED_FRAGMENTER_SETTING_NAME = "SelectedFragmenter";

    /**
     * Name for the given pipeline name for persistence.
     */
    public static final String PIPELINE_SETTING_NAME = "PipelineName";

    /**
     * Name for the setting for persisting the current pipeline size.
     */
    public static final String PIPELINE_SIZE_SETTING_NAME = "PipelineSize";

    /**
     * Beginning of the file names for persisting the pipeline fragmenters.
     */
    public static final String PIPELINE_FRAGMENTER_FILE_NAME_PREFIX = "PipelineFragmenter_";

    /**
     * Name of the setting to persist the algorithm name of a pipeline fragmenter.
     */
    public static final String PIPELINE_FRAGMENTER_ALGORITHM_NAME_SETTING_NAME = "AlgorithmName";
    //</editor-fold>
    //
    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * Selected fragmentation algorithm.
     */
    private IMoleculeFragmenter selectedFragmenter;
    /**
     * Array for the fragmentation algorithms to use during pipeline fragmentation.
     */
    private IMoleculeFragmenter[] pipelineFragmenter;
    /**
     * List of names of fragmentation algorithms that have already been run.
     */
    private List<String> existingFragmentations;
    /**
     * Hashtable for fragments.
     */
    private Hashtable<String, FragmentDataModel> fragments;
    /**
     * String for the name of the current fragmentation algorithm.
     */
    private String currentFragmentationName;
    /**
     * String for the name of the current pipeline fragmentation.
     */
    private String pipeliningFragmentationName;
    /**
     * ExecutorService for the fragmentation tasks.
     */
    private ExecutorService executorService;
    //</editor-fold>
    //
    //<editor-fold desc="private static final class constants" defaultstate="collapsed">
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(FragmentationService.class.getName());
    //
    /**
     * Array for the different fragmentation algorithms available.
     */
    private final IMoleculeFragmenter[] fragmenters;
    //
    /**
     * Ertl algorithm fragmenter.
     */
    private final IMoleculeFragmenter ertlFGF;
    //
    /**
     * Sugar Removal Utility fragmenter.
     */
    private final IMoleculeFragmenter sugarRUF;
    //
    /**
     * Scaffold Generator.
     */
    private final IMoleculeFragmenter scaffoldGF;
    //
    /**
     * SettingsContainer to hold settings.
     */
    private final SettingsContainer settingsContainer;
    //
    /**
     * Property of display name of selected fragmenter.
     */
    private final SimpleStringProperty selectedFragmenterDisplayNameProperty;
    //</editor-fold>
    //
    //<editor-fold desc="Constructors">
    /**
     * Constructor, instantiates the fragmenters and sets the selected fragmenter and the pipeline to their defaults.
     *
     * @param aSettingsContainer SettingsContainer which holds the settings
     */
    public FragmentationService(SettingsContainer aSettingsContainer) {
        //Note: Every fragmenter class should only be added once to the array or there will be problems with setting persistence!
        this.fragmenters = new IMoleculeFragmenter[3];
        this.ertlFGF = new ErtlFunctionalGroupsFinderFragmenter();
        this.fragmenters[0] = this.ertlFGF;
        this.sugarRUF = new SugarRemovalUtilityFragmenter();
        this.fragmenters[1] = this.sugarRUF;
        this.scaffoldGF = new ScaffoldGeneratorFragmenter();
        this.fragmenters[2] = this.scaffoldGF;
        //
        Objects.requireNonNull(aSettingsContainer, "aSettingsContainer must not be null");
        this.settingsContainer = aSettingsContainer;
        this.selectedFragmenterDisplayNameProperty = new SimpleStringProperty();
        try {
            this.checkFragmenters();
        } catch (Exception anException) {
            FragmentationService.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                    Message.get("Error.ExceptionAlert.Header"),
                    Message.get("FragmentationService.Error.invalidSettingFormat"),
                    anException);
        }
        for (IMoleculeFragmenter tmpFragmenter : this.fragmenters) {
            if (tmpFragmenter.getFragmentationAlgorithmName().equals(FragmentationService.DEFAULT_SELECTED_FRAGMENTER_ALGORITHM_NAME)) {
                this.selectedFragmenter = tmpFragmenter;
            }
        }
        if (Objects.isNull(this.selectedFragmenter)) {
            this.selectedFragmenter = this.ertlFGF;
        }
        this.setSelectedFragmenterDisplayName(this.selectedFragmenter.getFragmentationAlgorithmDisplayName());
        try {
            this.pipelineFragmenter = new IMoleculeFragmenter[] {
                    this.createNewFragmenterObjectByAlgorithmName(this.selectedFragmenter.getFragmentationAlgorithmName())
            };
        } catch (Exception anException) {
            //settings of this fragmenter instance are still in default at this point
            this.pipelineFragmenter = new IMoleculeFragmenter[] {this.selectedFragmenter.copy()};
        }
        this.pipeliningFragmentationName = FragmentationService.DEFAULT_PIPELINE_NAME;
        this.existingFragmentations = new LinkedList<>();
        //fragments hash table, current fragmentation name, and executor service are only instantiated when needed
    }
    //</editor-fold>
    //
    //<editor-fold desc="public methods" defaultstate="collapsed">
    /**
     * Manages the fragmentation, creates a number of {@link FragmentationTask} objects equal to the amount of
     * {@code aNumberOfTasks}, assigns the molecules of {@code aListOfMolecules} to them and starts the fragmentation.
     *
     * @param aListOfMolecules list of molecules to fragment
     * @param aNumberOfTasks how many parallel tasks should be used
     * @throws Exception if anything goes wrong
     */
    public void startSingleFragmentation(List<MoleculeDataModel> aListOfMolecules, int aNumberOfTasks) throws Exception {
        //<editor-fold desc="checks" defaultstate="collapsed">
        Objects.requireNonNull(aListOfMolecules, "aListOfMolecules must not be null");
        if (aNumberOfTasks == 0) {
            aNumberOfTasks = 1;
        }
        //</editor-fold>
        String tmpFragmentationName = this.createAndCheckFragmentationName(this.selectedFragmenter.getFragmentationAlgorithmDisplayName());
        this.existingFragmentations.add(tmpFragmentationName);
        this.currentFragmentationName = tmpFragmentationName;
        this.fragments = this.startFragmentation(aListOfMolecules, aNumberOfTasks, this.selectedFragmenter, tmpFragmentationName);
        FragmentationService.LOGGER.log(Level.INFO, "Number of different fragments extracted: {0}", this.fragments == null ? 0 : this.fragments.size());
    }
    //
    /**
     * Starts fragmentation pipeline for given List of molecules.
     * Fragmentation will be done on fragments of previous step.
     *
     * @param aListOfMolecules List {@literal <}MoleculeDataModel {@literal >}
     * @param aNumberOfTasks int value to define onto how many parallel task the molecules should be distributed for fragmentation
     * @throws Exception if anything goes critically wrong
     */
    public void startPipelineFragmentation(List<MoleculeDataModel> aListOfMolecules, int aNumberOfTasks) throws Exception {
        //<editor-fold desc="checks" defaultstate="collapsed">
        Objects.requireNonNull(aListOfMolecules, "aListOfMolecules must not be null");
        Objects.requireNonNull(this.pipelineFragmenter, "pipelineFragmenter must not be null");
        if (aNumberOfTasks == 0) {
            aNumberOfTasks = 1;
        }
        //</editor-fold>
        this.fragments = new Hashtable<>(aListOfMolecules.size() * this.pipelineFragmenter.length);
        Hashtable<String, FragmentDataModel> tmpFragmentHashtable;
        if (this.pipeliningFragmentationName == null || this.pipeliningFragmentationName.isEmpty()) {
            this.pipeliningFragmentationName = FragmentationService.DEFAULT_PIPELINE_NAME;
        }
        String tmpPipelineFragmentationName = this.createAndCheckFragmentationName(this.pipeliningFragmentationName);
        this.existingFragmentations.add(tmpPipelineFragmentationName);
        this.currentFragmentationName = tmpPipelineFragmentationName;
        List<MoleculeDataModel> tmpMolsToFragment = new ArrayList<>(aListOfMolecules);
        for (int i = 0; i < this.pipelineFragmenter.length; i++) {
            this.fragments.clear();
            tmpFragmentHashtable = this.startFragmentation(tmpMolsToFragment, aNumberOfTasks, this.pipelineFragmenter[i], tmpPipelineFragmentationName);
            tmpMolsToFragment.clear();
            //iterate through all initial molecules
            for (MoleculeDataModel tmpMolecule : aListOfMolecules) {
                Set<FragmentDataModel> tmpNewFragmentsOfMol = new HashSet<>();
                HashMap<String, Integer> tmpNewFragmentFrequenciesOfMol = new HashMap<>(tmpMolecule.getFragmentsOfSpecificFragmentation(tmpPipelineFragmentationName).size() * 2);
                //check if molecule has undergone fragmentation (redundant)
                if (!tmpMolecule.hasMoleculeUndergoneSpecificFragmentation(tmpPipelineFragmentationName)) {
                    continue;
                }
                // get fragments of molecules in a new list
                List<FragmentDataModel> tmpFragmentsOfMolList = new ArrayList<>(tmpMolecule.getFragmentsOfSpecificFragmentation(tmpPipelineFragmentationName));
                //clear fragments in molecule, child fragments will be added later
                tmpMolecule.getFragmentsOfSpecificFragmentation(tmpPipelineFragmentationName).clear();
                //iterate through fragments on mol list
                for (FragmentDataModel tmpParentFragment : tmpFragmentsOfMolList) {
                    //check if parent fragment has undergone, but not after first fragmentation
                    if (i > 0 && !tmpParentFragment.hasMoleculeUndergoneSpecificFragmentation(tmpPipelineFragmentationName)) {
                        continue;
                    }
                    //get child fragments of parent fragment
                    List<FragmentDataModel> tmpChildFragmentsList = tmpParentFragment.getFragmentsOfSpecificFragmentation(tmpPipelineFragmentationName);
                    //if parent fragment has no children, parent fragment is child fragment
                    if (tmpChildFragmentsList == null || tmpChildFragmentsList.isEmpty()) {
                        //if settingsContainer.isKeepLastFragmentSetting == true (DEPRECATED, always false) or
                        // parent fragment is part of the results of fragmentation, the parent fragment will be set as new fragment if no new fragment is found
                        if (
                            this.settingsContainer.isKeepLastFragmentSetting() ||
                                (tmpFragmentHashtable != null && tmpFragmentHashtable.containsKey(tmpParentFragment.getUniqueSmiles()))
                        ) {
                            if (
                                tmpFragmentHashtable != null && tmpNewFragmentsOfMol.add(tmpFragmentHashtable.get(tmpParentFragment.getUniqueSmiles()))
                            ) {
                                tmpNewFragmentFrequenciesOfMol.put(
                                    tmpParentFragment.getUniqueSmiles(),
                                    tmpMolecule.getFragmentFrequencyOfSpecificFragmentation(tmpPipelineFragmentationName).get(tmpParentFragment.getUniqueSmiles())
                                );
                            }
                            //if HashTable for resulting fragments contains fragment, update frequencies = add molecules fragment frequency
                            // of fragment to absolute frequency of fragment and increment molecule frequency
                            if (this.fragments.containsKey(tmpParentFragment.getUniqueSmiles())) {
                                if (tmpParentFragment.getParentMolecules().add(tmpMolecule))
                                    tmpParentFragment.incrementMoleculeFrequency();
                                tmpParentFragment.setAbsoluteFrequency(
                                    tmpParentFragment.getAbsoluteFrequency() +
                                        tmpMolecule.getFragmentFrequencyOfSpecificFragmentation(tmpPipelineFragmentationName).get(tmpParentFragment.getUniqueSmiles())
                                );
                            }
                            //else add to HashTable, set molecules fragment frequency of fragment as initial absolute frequency of fragment and set molecule frequency to 1
                            else {
                                tmpParentFragment.getParentMolecules().clear();
                                if (tmpParentFragment.getParentMolecules().add(tmpMolecule))
                                    tmpParentFragment.setMoleculeFrequency(1);
                                this.fragments.put(tmpParentFragment.getUniqueSmiles(), tmpParentFragment);
                                tmpParentFragment.setAbsoluteFrequency(
                                    tmpMolecule.getFragmentFrequencyOfSpecificFragmentation(tmpPipelineFragmentationName).get(tmpParentFragment.getUniqueSmiles())
                                );

                            }
                        }
                    }
                    //else (parent fragment has children) iterate through children fragment list
                    else {
                        for (FragmentDataModel tmpChild : tmpChildFragmentsList) {
                            FragmentDataModel tmpChildFragment;
                            if (tmpFragmentHashtable != null && tmpFragmentHashtable.containsKey(tmpChild.getUniqueSmiles())) {
                                tmpChildFragment = tmpFragmentHashtable.get(tmpChild.getUniqueSmiles());
                            } else {
                                tmpChildFragment = tmpChild;
                            }
                            if (tmpNewFragmentsOfMol.contains(tmpChildFragment)) {
                                if(tmpChildFragment.getParentMolecules().add(tmpMolecule))
                                    tmpChildFragment.incrementMoleculeFrequency();
                                tmpNewFragmentFrequenciesOfMol.replace(
                                        tmpChildFragment.getUniqueSmiles(),
                                        tmpNewFragmentFrequenciesOfMol.get(tmpChildFragment.getUniqueSmiles()) +
                                            tmpParentFragment.getFragmentFrequencyOfSpecificFragmentation(tmpPipelineFragmentationName).get(tmpChildFragment.getUniqueSmiles())
                                );

                            } else {
                                if(tmpChildFragment.getParentMolecules().add(tmpMolecule))
                                    tmpChildFragment.incrementMoleculeFrequency();
                                tmpNewFragmentsOfMol.add(tmpChildFragment);
                                tmpNewFragmentFrequenciesOfMol.put(
                                        tmpChildFragment.getUniqueSmiles(),
                                        tmpMolecule.getFragmentFrequencyOfSpecificFragmentation(tmpPipelineFragmentationName).get(tmpParentFragment.getUniqueSmiles()) *
                                                tmpParentFragment.getFragmentFrequencyOfSpecificFragmentation(tmpPipelineFragmentationName).get(tmpChildFragment.getUniqueSmiles())
                                        );
                            }
                            //if HashTable for resulting fragments contains fragment, update frequencies = add molecules fragment frequency of fragment to absolute frequency of fragment and increment molecule frequency
                            if (this.fragments.containsKey(tmpChildFragment.getUniqueSmiles())) {
                                tmpChildFragment.setAbsoluteFrequency(
                                    tmpChildFragment.getAbsoluteFrequency() +
                                        (tmpMolecule.getFragmentFrequencyOfSpecificFragmentation(tmpPipelineFragmentationName).get(tmpParentFragment.getUniqueSmiles()) *
                                            tmpParentFragment.getFragmentFrequencyOfSpecificFragmentation(tmpPipelineFragmentationName).get(tmpChildFragment.getUniqueSmiles())
                                        )
                                );
                            }
                            //else add to HashTable, set molecules fragment frequency of fragment as initial absolute frequency of fragment and set molecule frequency to 1
                            else {
                                this.fragments.put(tmpChildFragment.getUniqueSmiles(), tmpChildFragment);
                                tmpChildFragment.setAbsoluteFrequency(
                                        tmpMolecule.getFragmentFrequencyOfSpecificFragmentation(tmpPipelineFragmentationName).get(tmpParentFragment.getUniqueSmiles()) *
                                                tmpParentFragment.getFragmentFrequencyOfSpecificFragmentation(tmpPipelineFragmentationName).get(tmpChildFragment.getUniqueSmiles())
                                );
                                tmpChildFragment.setMoleculeFrequency(1);
                            }
                        }
                    }
                }
                tmpMolecule.getAllFragments().replace(tmpPipelineFragmentationName, new ArrayList<>(tmpNewFragmentsOfMol));
                tmpMolecule.getFragmentFrequencies().replace(tmpPipelineFragmentationName, tmpNewFragmentFrequenciesOfMol);
            }
            tmpMolsToFragment = new ArrayList<>(this.fragments.values());
        }
        int tmpFragmentAmount = 0;
        Set<String> tmpKeySet = this.fragments.keySet();
        for (String tmpKey : tmpKeySet) {
            tmpFragmentAmount += this.fragments.get(tmpKey).getAbsoluteFrequency();
        }
        for (String tmpKey : tmpKeySet) {
            this.fragments.get(tmpKey).getParentMolecules().removeIf(moleculeDataModel -> !aListOfMolecules.contains(moleculeDataModel));
        }
        if (tmpFragmentAmount != 0) {
            for (String tmpKey : tmpKeySet) {
                this.fragments.get(tmpKey).setAbsolutePercentage(1.0 * this.fragments.get(tmpKey).getAbsoluteFrequency() / tmpFragmentAmount);
                this.fragments.get(tmpKey).setMoleculePercentage(1.0 * this.fragments.get(tmpKey).getMoleculeFrequency() / aListOfMolecules.size());
            }
        } else {
            FragmentationService.LOGGER.log(Level.WARNING, "Sum of absolute frequencies of fragments was 0! Percentages could not be calculated.");
        }
        FragmentationService.LOGGER.log(Level.INFO, "Number of different fragments extracted: {0}",  this.fragments.size());
     }
    //
    /**
     * Under construction
     * Start fragmentation pipeline
     * Fragmentation will be done molecule by molecule
     *
     * @param aListOfMolecules List {@literal <}MoleculeDataModel {@literal >}
     * @param aNumberOfTasks int
     * @throws Exception if anything unexpected happen
     * @deprecated After adapting the data models, this method must be modified so that the resulting fragments are kept separate for each molecule. Note the setting keepLastFragment
     */
    @Deprecated
    public void startPipelineFragmentationMolByMol(List<MoleculeDataModel> aListOfMolecules, int aNumberOfTasks) throws Exception{
        //<editor-fold desc="checks" defualtstate="collapsed">
        Objects.requireNonNull(aListOfMolecules, "aListOfMolecules must not be null");
        Objects.requireNonNull(this.pipelineFragmenter, "pipelineFragmenter must not be null");
        if(aNumberOfTasks == 0){
            aNumberOfTasks = 1;
        }
        //</editor-fold>
        this.fragments = new Hashtable<>(aListOfMolecules.size() * this.pipelineFragmenter.length);
        if(this.pipeliningFragmentationName == null || this.pipeliningFragmentationName.isEmpty()){
            this.pipeliningFragmentationName = "Pipeline";
        }
        String tmpPipelineFragmentationName = this.createAndCheckFragmentationName(this.pipeliningFragmentationName);
        this.existingFragmentations.add(tmpPipelineFragmentationName);
        this.currentFragmentationName = tmpPipelineFragmentationName;

        this.fragments = this.startFragmentation(aListOfMolecules, aNumberOfTasks, this.pipelineFragmenter[0], tmpPipelineFragmentationName);

        for(int i = 1; i < this.pipelineFragmenter.length; i++){
            String tmpFragmentationName = this.createAndCheckFragmentationName(tmpPipelineFragmentationName + "_" + this.pipelineFragmenter[i].getFragmentationAlgorithmDisplayName());

            for(MoleculeDataModel tmpParentMol : aListOfMolecules){
                if(!tmpParentMol.hasMoleculeUndergoneSpecificFragmentation(tmpPipelineFragmentationName)){
                    continue;
                }
                List<MoleculeDataModel> tmpChildMols = (List<MoleculeDataModel>)(List<?>) tmpParentMol.getFragmentsOfSpecificFragmentation(tmpPipelineFragmentationName);
                Hashtable<String, FragmentDataModel> tmpFragmentHashtable = this.startFragmentation(
                        tmpChildMols, aNumberOfTasks, this.pipelineFragmenter[i], tmpFragmentationName);
                Set<String> tmpKeySet = tmpFragmentHashtable.keySet();
                LinkedList<FragmentDataModel> tmpFrags = new LinkedList<>();
                for(String tmpKey : tmpKeySet){
                    tmpFrags.add(tmpFragmentHashtable.get(tmpKey));
                }
                HashMap<String, Integer> tmpNewFrequencies = new HashMap<>(tmpParentMol.getFragmentsOfSpecificFragmentation(tmpPipelineFragmentationName).size()*2);
                for(MoleculeDataModel tmpChildMol : tmpChildMols){
                    if(!tmpChildMol.hasMoleculeUndergoneSpecificFragmentation(tmpFragmentationName)){
                        continue;
                    }
                    for(FragmentDataModel tmpFrag : tmpChildMol.getFragmentsOfSpecificFragmentation(tmpFragmentationName)){
                        String tmpKey;
                        try{
                            tmpKey = ChemUtil.createUniqueSmiles(tmpFrag.getAtomContainer());
                            if(!tmpChildMol.hasMoleculeUndergoneSpecificFragmentation(tmpFragmentationName) ||
                                    !tmpParentMol.hasMoleculeUndergoneSpecificFragmentation(tmpPipelineFragmentationName)){
                                continue;
                            }
                            if(tmpNewFrequencies.containsKey(tmpKey)){
                                tmpNewFrequencies.replace(
                                        tmpKey,
                                        tmpNewFrequencies.get(tmpKey) +
                                                tmpChildMol.getFragmentFrequencyOfSpecificFragmentation(tmpFragmentationName).get(tmpKey) *
                                                        tmpParentMol.getFragmentFrequencyOfSpecificFragmentation(tmpPipelineFragmentationName).get(ChemUtil.createUniqueSmiles(tmpChildMol.getAtomContainer()))
                                );
                            } else {
                                tmpNewFrequencies.put(
                                        tmpKey,
                                        tmpChildMol.getFragmentFrequencyOfSpecificFragmentation(tmpFragmentationName).get(tmpKey) *
                                                tmpParentMol.getFragmentFrequencyOfSpecificFragmentation(tmpPipelineFragmentationName).get(ChemUtil.createUniqueSmiles(tmpChildMol.getAtomContainer()))
                                );
                            }
                        } catch(CDKException anException) {
                            Logger.getLogger(MoleculeDataModel.class.getName()).log(Level.SEVERE, String.format("%s fragment name: %s", anException.toString(), tmpFrag.getName()), anException);
                        }
                    }
                }
                tmpParentMol.getFragmentFrequencies().replace(tmpPipelineFragmentationName, tmpNewFrequencies);
                tmpParentMol.getAllFragments().replace(tmpPipelineFragmentationName, tmpFrags);
            }
        }
        Hashtable<String, FragmentDataModel> tmpFragmentsHash = new Hashtable<>(this.fragments.size() * this.pipelineFragmenter.length);
        for(MoleculeDataModel tmpMol : aListOfMolecules){
            if (!tmpMol.hasMoleculeUndergoneSpecificFragmentation(tmpPipelineFragmentationName)) {
                continue;
            }
            for(FragmentDataModel tmpFrag : tmpMol.getFragmentsOfSpecificFragmentation(tmpPipelineFragmentationName)) {
                String tmpKey;
                try {
                    tmpKey = ChemUtil.createUniqueSmiles(tmpFrag.getAtomContainer());
                } catch (CDKException anException){
                    Logger.getLogger(MoleculeDataModel.class.getName()).log(Level.SEVERE, String.format("%s fragment name: %s", anException.toString(), tmpFrag.getName()), anException);
                    continue;
                }
                if (!tmpMol.hasMoleculeUndergoneSpecificFragmentation(tmpPipelineFragmentationName)) {
                    continue;
                }
                if (!tmpFragmentsHash.containsKey(tmpKey)) {
                    tmpFragmentsHash.put(tmpKey, tmpFrag);
                    tmpFrag.setAbsoluteFrequency(tmpMol.getFragmentFrequencyOfSpecificFragmentation(tmpPipelineFragmentationName).get(tmpKey));
                    tmpFrag.setMoleculeFrequency(1);
                } else {
                    tmpFragmentsHash.get(tmpKey).setAbsoluteFrequency(tmpFragmentsHash.get(tmpKey).getAbsoluteFrequency() + tmpMol.getFragmentFrequencyOfSpecificFragmentation(tmpPipelineFragmentationName).get(tmpKey));
                    tmpFragmentsHash.get(tmpKey).incrementMoleculeFrequency();
                }
            }
        }
        this.fragments = tmpFragmentsHash;
        int tmpFragmentAmount = 0;
        Set<String> tmpKeySet = this.fragments.keySet();
        for(String tmpKey : tmpKeySet){
            tmpFragmentAmount += this.fragments.get(tmpKey).getAbsoluteFrequency();
        }
        if (tmpFragmentAmount != 0) {
            for (String tmpKey : tmpKeySet) {
                this.fragments.get(tmpKey).setAbsolutePercentage(1.0 * this.fragments.get(tmpKey).getAbsoluteFrequency() / tmpFragmentAmount);
                this.fragments.get(tmpKey).setMoleculePercentage(1.0 * this.fragments.get(tmpKey).getMoleculeFrequency() / aListOfMolecules.size());
            }
        } else {
            FragmentationService.LOGGER.log(Level.WARNING, "Sum of absolute frequencies of fragments was 0! Percentages could not be calculated.");
        }
    }
    //
    /**
     * Returns a new instance of the fragmenter class with the given algorithm name.
     *
     * @param anAlgorithmName name of the algorithm implemented in the desired fragmenter class, as returned by
     * {@link IMoleculeFragmenter#getFragmentationAlgorithmName()  IMoleculeFragmenter.getFragmentationAlgorithmName()}
     * @return new instance of the fragmenter class
     * @throws IllegalArgumentException if the given algorithm name does not match any available fragmenter classes
     * @throws ClassNotFoundException if instantiating the class goes wrong
     * @throws NoSuchMethodException if instantiating the class goes wrong
     * @throws InvocationTargetException if instantiating the class goes wrong
     * @throws InstantiationException if instantiating the class goes wrong
     * @throws IllegalAccessException if instantiating the class goes wrong
     */
    public IMoleculeFragmenter createNewFragmenterObjectByAlgorithmName(String anAlgorithmName)
            throws IllegalArgumentException,
            ClassNotFoundException,
            NoSuchMethodException,
            InvocationTargetException,
            InstantiationException,
            IllegalAccessException {
        String tmpClassName = "";
        for (IMoleculeFragmenter tmpFragmenter : this.fragmenters) {
            if (anAlgorithmName.equals(tmpFragmenter.getFragmentationAlgorithmName())) {
                tmpClassName = tmpFragmenter.getClass().getName();
            }
        }
        if (tmpClassName.isBlank() || tmpClassName.isEmpty()) {
            throw new IllegalArgumentException(String.format("Given algorithm name %s is invalid.", anAlgorithmName));
        }
        Class<?> tmpClazz = Class.forName(tmpClassName);
        Constructor<?> tmpCtor = tmpClazz.getConstructor();
        return (IMoleculeFragmenter) tmpCtor.newInstance();
    }
    //
    /**
     * Persists settings of the fragmenters in preference container files in a subfolder of the settings directory. The settings of the
     * fragmenters are translated to matching preference objects. If a single setting or several cannot be persisted, it
     * is only logged in the log file. But if persisting a whole fragmenter fails, a warning is given to the user. The
     * settings are saved to files denoted with the simple class name of the respective fragmenter.
     */
    public void persistFragmenterSettings() {
        String tmpDirectoryPath = FileUtil.getSettingsDirPath()
                + FragmentationService.FRAGMENTER_SETTINGS_SUBFOLDER_NAME + File.separator;
        File tmpDirectory = new File(tmpDirectoryPath);
        boolean tmpMKDirsSuccessful = true;
        if (!tmpDirectory.exists()) {
            tmpMKDirsSuccessful = tmpDirectory.mkdirs();
        } else {
            FileUtil.deleteAllFilesInDirectory(tmpDirectoryPath);
        }
        if (!tmpDirectory.canWrite() || !tmpMKDirsSuccessful) {
            GuiUtil.guiMessageAlert(Alert.AlertType.ERROR, Message.get("Error.ExceptionAlert.Title"),
                    Message.get("Error.ExceptionAlert.Header"),
                    Message.get("FragmentationService.Error.settingsPersistence"));
            return;
        }
        for (IMoleculeFragmenter tmpFragmenter : this.fragmenters) {
            if (Objects.isNull(tmpFragmenter)) {
                continue;
            }
            List<Property<?>> tmpSettings = tmpFragmenter.settingsProperties();
            if (Objects.isNull(tmpSettings)) {
                continue;
            }
            String tmpFilePath = tmpDirectoryPath
                    + tmpFragmenter.getClass().getSimpleName()
                    + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION;
            try {
                PreferenceContainer tmpPrefContainer = PreferenceUtil.translateJavaFxPropertiesToPreferences(tmpSettings, tmpFilePath);
                tmpPrefContainer.writeRepresentation();
            } catch (NullPointerException | IllegalArgumentException | IOException | SecurityException anException) {
                FragmentationService.LOGGER.log(Level.WARNING, String.format("Fragmenter settings persistence went wrong, exception: %s", anException.toString()), anException);
                GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                        Message.get("Error.ExceptionAlert.Header"),
                        Message.get("FragmentationService.Error.settingsPersistence"),
                        anException);
                //continue;
            }
        }
    }

    /**
     * Persists the fragmentation service settings (the selected fragmenter) and the currently configured pipeline.
     * All settings are saved as matching preference objects in preference container files in a respective subfolder in
     * the settings directory. If anything fails, a warning is given to the user.
     */
    public void persistSelectedFragmenterAndPipeline() {
        String tmpFragmentationServiceSettingsPath = FileUtil.getSettingsDirPath()
                + FragmentationService.FRAGMENTATION_SERVICE_SETTINGS_SUBFOLDER_NAME + File.separator;
        File tmpFragmentationServiceSettingsDir = new File(tmpFragmentationServiceSettingsPath);
        boolean tmpMKDirsSuccessful = true;
        if (!tmpFragmentationServiceSettingsDir.exists()) {
            tmpMKDirsSuccessful = tmpFragmentationServiceSettingsDir.mkdirs();
        } else {
            FileUtil.deleteAllFilesInDirectory(tmpFragmentationServiceSettingsPath);
        }
        if (!tmpFragmentationServiceSettingsDir.canWrite() || !tmpMKDirsSuccessful) {
            GuiUtil.guiMessageAlert(Alert.AlertType.ERROR,
                    Message.get("Error.ExceptionAlert.Title"),
                    Message.get("Error.ExceptionAlert.Header"),
                    Message.get("FragmentationService.Error.settingsPersistence"));
            return;
        }
        PreferenceContainer tmpFragmentationServiceSettingsContainer = new PreferenceContainer(
                tmpFragmentationServiceSettingsPath
                        + FragmentationService.FRAGMENTATION_SERVICE_SETTINGS_FILE_NAME
                        + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION);
        if (!SingleTermPreference.isValidContent(this.selectedFragmenter.getFragmentationAlgorithmName())) {
            FragmentationService.LOGGER.log(Level.WARNING, "Selected fragmenter could not be persisted");
            GuiUtil.guiMessageAlert(Alert.AlertType.WARNING,
                    Message.get("Error.ExceptionAlert.Title"),
                    Message.get("Error.ExceptionAlert.Header"),
                    Message.get("FragmentationService.Error.settingsPersistence"));
            for (IMoleculeFragmenter tmpFragmenter : this.fragmenters) {
                if (tmpFragmenter.getFragmentationAlgorithmName().equals(FragmentationService.DEFAULT_SELECTED_FRAGMENTER_ALGORITHM_NAME)) {
                    this.selectedFragmenter = tmpFragmenter;
                }
            }
        }
        SingleTermPreference tmpSelectedFragmenterPreference = new SingleTermPreference(
                FragmentationService.SELECTED_FRAGMENTER_SETTING_NAME,
                this.selectedFragmenter.getFragmentationAlgorithmName());
        tmpFragmentationServiceSettingsContainer.add(tmpSelectedFragmenterPreference);
        if (Objects.isNull(this.pipeliningFragmentationName) || this.pipeliningFragmentationName.isEmpty()) {
            this.pipeliningFragmentationName = FragmentationService.DEFAULT_PIPELINE_NAME;
        }
        if (!SingleTermPreference.isValidContent(this.pipeliningFragmentationName)) {
            FragmentationService.LOGGER.log(Level.WARNING, "Given pipeline name {0} is invalid, will be reset to default.", this.pipeliningFragmentationName);
            this.pipeliningFragmentationName = FragmentationService.DEFAULT_PIPELINE_NAME;
        }
        SingleTermPreference tmpPipelineNamePreference = new SingleTermPreference(FragmentationService.PIPELINE_SETTING_NAME,
                this.pipeliningFragmentationName);
        tmpFragmentationServiceSettingsContainer.add(tmpPipelineNamePreference);
        tmpFragmentationServiceSettingsContainer.add(new SingleIntegerPreference(FragmentationService.PIPELINE_SIZE_SETTING_NAME,
                this.pipelineFragmenter.length));
        try {
            tmpFragmentationServiceSettingsContainer.writeRepresentation();
        } catch (IOException | SecurityException anException) {
            FragmentationService.LOGGER.log(Level.WARNING, String.format("Fragmentation service settings persistence went wrong, exception: %s", anException.toString()), anException);
            GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                    Message.get("Error.ExceptionAlert.Header"),
                    Message.get("FragmentationService.Error.settingsPersistence"),
                    anException);
        }
        for (int i = 0; i < this.pipelineFragmenter.length; i++) {
            IMoleculeFragmenter tmpFragmenter = this.pipelineFragmenter[i];
            List<Property<?>> tmpSettings = tmpFragmenter.settingsProperties();
            String tmpFilePath = tmpFragmentationServiceSettingsPath
                    + FragmentationService.PIPELINE_FRAGMENTER_FILE_NAME_PREFIX + i
                    + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION;
            PreferenceContainer tmpPrefContainer = PreferenceUtil.translateJavaFxPropertiesToPreferences(tmpSettings, tmpFilePath);
            tmpPrefContainer.add(new SingleTermPreference(FragmentationService.PIPELINE_FRAGMENTER_ALGORITHM_NAME_SETTING_NAME,
                    tmpFragmenter.getFragmentationAlgorithmName()));
            try {
                tmpPrefContainer.writeRepresentation();
            } catch (IOException | SecurityException anException) {
                FragmentationService.LOGGER.log(Level.WARNING, String.format("Pipeline fragmenter settings persistence went wrong, exception: %s", anException.toString()), anException);
                GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                        Message.get("Error.ExceptionAlert.Header"),
                        Message.get("FragmentationService.Error.settingsPersistence"),
                        anException);
                //continue;
            }
        }
    }

    /**
     * Reloads settings of the available fragmenters. If something goes wrong, it is logged and the setting remains in default.
     */
    public void reloadFragmenterSettings() {
        String tmpDirectoryPath = FileUtil.getSettingsDirPath()
                + FragmentationService.FRAGMENTER_SETTINGS_SUBFOLDER_NAME + File.separator;
        for (IMoleculeFragmenter tmpFragmenter : this.fragmenters) {
            String tmpClassName = tmpFragmenter.getClass().getSimpleName();
            File tmpFragmenterSettingsFile = new File(tmpDirectoryPath
                    + tmpClassName
                    + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION);
            if (tmpFragmenterSettingsFile.exists() && tmpFragmenterSettingsFile.isFile() && tmpFragmenterSettingsFile.canRead()) {
                PreferenceContainer tmpContainer;
                try {
                    tmpContainer = new PreferenceContainer(tmpFragmenterSettingsFile);
                } catch (IllegalArgumentException | IOException anException) {
                    FragmentationService.LOGGER.log(Level.WARNING, String.format("Unable to reload settings of fragmenter %s : %s", tmpClassName, anException.toString()), anException);
                    continue;
                }
                PreferenceUtil.updatePropertiesFromPreferences(tmpFragmenter.settingsProperties(), tmpContainer);
            } else {
                //settings will remain in default
                FragmentationService.LOGGER.log(Level.WARNING, "No persisted settings for {0} available.", tmpClassName);
            }
        }
    }

    /**
     * Reloads fragmentation service settings like the selected fragmenter and the pipeline configurations from the previous session.
     * If anything goes wrong, the errors are logged and in some cases, a warning is given to the user.
     */
    public void reloadActiveFragmenterAndPipeline() {
        String tmpFragmentationServiceSettingsPath = FileUtil.getSettingsDirPath()
                + FragmentationService.FRAGMENTATION_SERVICE_SETTINGS_SUBFOLDER_NAME + File.separator;
        String tmpServiceSettingsFilePath = tmpFragmentationServiceSettingsPath
                + FragmentationService.FRAGMENTATION_SERVICE_SETTINGS_FILE_NAME
                + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION;
        File tmpServiceSettingsFile = new File(tmpServiceSettingsFilePath);
        if (tmpServiceSettingsFile.exists() && tmpServiceSettingsFile.isFile() && tmpServiceSettingsFile.canRead()) {
            PreferenceContainer tmpFragmentationServiceSettingsContainer;
            int tmpPipelineSize;
            try {
                tmpFragmentationServiceSettingsContainer = new PreferenceContainer(tmpServiceSettingsFile);
                tmpPipelineSize = ((SingleIntegerPreference)tmpFragmentationServiceSettingsContainer.getPreferences(FragmentationService.PIPELINE_SIZE_SETTING_NAME)[0]).getContent();
                String tmpPipelineName = tmpFragmentationServiceSettingsContainer.getPreferences(FragmentationService.PIPELINE_SETTING_NAME)[0].getContentRepresentative();
                String tmpSelectedFragmenterAlgorithmName = tmpFragmentationServiceSettingsContainer.getPreferences(FragmentationService.SELECTED_FRAGMENTER_SETTING_NAME)[0].getContentRepresentative();
                this.pipeliningFragmentationName = tmpPipelineName;
                for (IMoleculeFragmenter tmpFragmenter : this.fragmenters) {
                    if (tmpFragmenter.getFragmentationAlgorithmName().equals(tmpSelectedFragmenterAlgorithmName)) {
                        this.selectedFragmenter = tmpFragmenter;
                        this.selectedFragmenterDisplayNameProperty.set(this.selectedFragmenter.getFragmentationAlgorithmDisplayName());
                        break;
                    }
                }
            } catch (IllegalArgumentException | IOException anException) {
                FragmentationService.LOGGER.log(Level.WARNING, String.format("FragmentationService settings reload failed: %s", anException.toString()), anException);
                GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                        Message.get("Error.ExceptionAlert.Header"),
                        Message.get("FragmentationService.Error.settingsReload"),
                        anException);
                return;
            }
            List<IMoleculeFragmenter> tmpFragmenterList = new ArrayList<>(tmpPipelineSize);
            for (int i = 0; i < tmpPipelineSize; i++) {
                String tmpPath = tmpFragmentationServiceSettingsPath + FragmentationService.PIPELINE_FRAGMENTER_FILE_NAME_PREFIX + i + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION;
                File tmpFragmenterFile = new File(tmpPath);
                if (tmpFragmenterFile.exists() && tmpFragmenterFile.isFile() && tmpFragmenterFile.canRead()) {
                    try {
                        PreferenceContainer tmpFragmenterSettingsContainer = new PreferenceContainer(tmpFragmenterFile);
                        String tmpFragmenterClassName = tmpFragmenterSettingsContainer.getPreferences(FragmentationService.PIPELINE_FRAGMENTER_ALGORITHM_NAME_SETTING_NAME)[0].getContentRepresentative();
                        IMoleculeFragmenter tmpFragmenter = this.createNewFragmenterObjectByAlgorithmName(tmpFragmenterClassName);
                        PreferenceUtil.updatePropertiesFromPreferences(tmpFragmenter.settingsProperties(), tmpFragmenterSettingsContainer);
                        tmpFragmenterList.add(tmpFragmenter);
                    } catch (Exception anException) {
                        FragmentationService.LOGGER.log(Level.WARNING, String.format("FragmentationService settings reload failed: %s", anException.toString()), anException);
                        GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                                Message.get("Error.ExceptionAlert.Header"),
                                Message.get("FragmentationService.Error.settingsReload"),
                                anException);
                        //continue;
                    }
                } else {
                    FragmentationService.LOGGER.log(Level.WARNING, "Unable to reload pipeline fragmenter {0} : No respective file available. Will be skipped.", i);
                    //continue;
                }
            }
            IMoleculeFragmenter[] tmpFragmenterArray = tmpFragmenterList.toArray(new IMoleculeFragmenter[tmpFragmenterList.size()]);
            this.setPipelineFragmenter(tmpFragmenterArray);
        } else {
            FragmentationService.LOGGER.log(Level.WARNING, "File containing persisted FragmentationService settings not found.");
        }
    }

    /**
     * Shuts down executor service.
     * Used as <a href="https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/ExecutorService.html">recommended by oracle</a>.
     */
    public void abortExecutor() {
        this.executorService.shutdown();
        try {
            if (!this.executorService.awaitTermination(600, TimeUnit.MILLISECONDS)) {
                this.executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            FragmentationService.LOGGER.log(Level.WARNING, "Interrupted!", e);
            this.executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Clears all cached variables like existingFragmentations and fragments.
     */
    public void clearCache() {
        this.existingFragmentations = new LinkedList<>();
        this.fragments = null;
        this.currentFragmentationName = null;
    }
    //</editor-fold>
    //
    //<editor-fold desc="public properties" defaultstate="collapsed">
    /**
     * Returns array of {@link IMoleculeFragmenter}.
     *
     * @return fragmenters
     */
    public IMoleculeFragmenter[] getFragmenters() {
        return this.fragmenters;
    }
    /**
     * Returns array of {@link IMoleculeFragmenter} for pipelining.
     *
     * @return pipelineFragmenters
     */
    public IMoleculeFragmenter[] getPipelineFragmenter() {
        return this.pipelineFragmenter;
    }
    /**
     * Returns selected {@link IMoleculeFragmenter}.
     *
     * @return selectedFragmenter
     */
    public IMoleculeFragmenter getSelectedFragmenter() {
        return this.selectedFragmenter;
    }
    /**
     * Returns Map of unique SMILES codes of fragments linked to the respective {@link FragmentDataModel} instances.
     * Returned map is a synchronized HashTable.
     *
     * @return fragments (results of fragmentation)
     */
    public Map<String, FragmentDataModel> getFragments() {
        return this.fragments;
    }
    /**
     * Returns name of the running fragmentation.
     *
     * @return currentFragmentation
     */
    public String getCurrentFragmentationName() {
        return this.currentFragmentationName;
    }
    /**
     * Returns name of the current pipeline configuration.
     *
     * @return pipeline name
     */
    public String getPipeliningFragmentationName() {
        return this.pipeliningFragmentationName;
    }
    /**
     * Sets the selectedFragmenter using the display name.
     *
     * @param anAlgorithmDisplayName must be retrieved using the respective method of the fragmenter object
     */
    public void setSelectedFragmenter(String anAlgorithmDisplayName) {
        for (IMoleculeFragmenter tmpFragmenter : this.fragmenters) {
            if (anAlgorithmDisplayName.equals(tmpFragmenter.getFragmentationAlgorithmDisplayName()))
                this.selectedFragmenter = tmpFragmenter;
        }
    }

    /**
     * Sets the fragmenters to use for pipeline fragmentation.
     *
     * @param anArrayOfFragmenter IMolecueFragmenter[]
     */
    public void setPipelineFragmenter(IMoleculeFragmenter[] anArrayOfFragmenter) {
        this.pipelineFragmenter = anArrayOfFragmenter;
    }
    /**
     * Sets the pipeline name.
     *
     * @param aName pipeline name
     */
    public void setPipeliningFragmentationName(String aName) {
        this.pipeliningFragmentationName = aName;
    }
    /**
     * Gets the display name of the selected fragmenter.
     *
     * @return String
     */
    public String getSelectedFragmenterDisplayName() {
        return this.selectedFragmenterDisplayNameProperty.get();
    }
    /**
     * Returns the property of the display name of the selected fragmenter.
     *
     * @return SimpleStringProperty
     */
    public SimpleStringProperty selectedFragmenterDisplayNameProperty() {
        return this.selectedFragmenterDisplayNameProperty;
    }
    /**
     * Sets the display name of the selected fragmenter.
     *
     * @param aFragmenterDisplayName String for the display name of the fragmenter
     */
    public void setSelectedFragmenterDisplayName(String aFragmenterDisplayName) {
        this.selectedFragmenterDisplayNameProperty.set(aFragmenterDisplayName);
    }
    //</editor-fold>
    //
    //<editor-fold desc="private methods">
    /**
     * Checks whether the name exists, if so, a consecutive number is appended, if not, the name is returned unchanged.
     *
     * @param anAlgorithmName String
     * @return String algorithm name
     */
    private String createAndCheckFragmentationName(String anAlgorithmName) {
        String tmpFragmentationName = anAlgorithmName;
        if(this.existingFragmentations.contains(tmpFragmentationName)){
            int tmpIndex = 0;
            do {
                tmpFragmentationName = anAlgorithmName + "_" + ++tmpIndex;
            }
            while(this.existingFragmentations.contains(tmpFragmentationName));
        }
        return tmpFragmentationName;
    }
    //
    /**
     * Manages the fragmentation, creates {@link FragmentationTask} equal to the amount of {@param aNumberOfTasks},
     * assigns the molecules of {@param aListOfMolecules} to them and starts the fragmentation.
     *
     * @param aListOfMolecules molecules to fragment and to assign the fragments to
     * @param aNumberOfTasks number of parallel tasks to use for the process
     * @param aFragmenter fragmenter instance to use to fragment, will be copied for the parallel tasks
     * @param aFragmentationName name under which to store the fragmentation results on the molecules
     * @throws Exception if anything goes wrong
     */
    private Hashtable<String, FragmentDataModel> startFragmentation(List<MoleculeDataModel> aListOfMolecules,
                                                                    int aNumberOfTasks,
                                                                    IMoleculeFragmenter aFragmenter,
                                                                    String aFragmentationName)
            throws Exception {
        if (aListOfMolecules.isEmpty() || aNumberOfTasks == 0) {
            return new Hashtable<>(0);
        }
        int tmpNumberOfTasks = aNumberOfTasks;
        Hashtable<String, FragmentDataModel> tmpFragmentHashtable = new Hashtable<>(aListOfMolecules.size() * 2);
        if (aListOfMolecules.size() < tmpNumberOfTasks) {
            tmpNumberOfTasks = aListOfMolecules.size();
        }
        int tmpMoleculesPerTask = aListOfMolecules.size() / tmpNumberOfTasks;
        int tmpMoleculeModulo = aListOfMolecules.size() % tmpNumberOfTasks;
        int tmpFromIndex = 0; //low endpoint (inclusive) of the subList
        int tmpToIndex = tmpMoleculesPerTask; //high endpoint (exclusive) of the subList
        if(tmpMoleculeModulo > 0){
            tmpToIndex++;
            tmpMoleculeModulo--;
        }
        this.executorService = Executors.newFixedThreadPool(tmpNumberOfTasks);
        /* Explicit version that can be used to override methods:
        this.executorService =  new ThreadPoolExecutor(tmpNumberOfTasks, tmpNumberOfTasks, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>()) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
            }
        }; */
        List<FragmentationTask> tmpFragmentationTaskList = new LinkedList<>();
        for (int i = 1; i <= tmpNumberOfTasks; i++) {
            List<MoleculeDataModel> tmpMoleculesForTask = aListOfMolecules.subList(tmpFromIndex, tmpToIndex);
            IMoleculeFragmenter tmpFragmenterForTask = aFragmenter.copy();
            tmpFragmentationTaskList.add(new FragmentationTask(tmpMoleculesForTask, tmpFragmenterForTask, tmpFragmentHashtable, aFragmentationName));
            tmpFromIndex = tmpToIndex;
            tmpToIndex = tmpFromIndex + tmpMoleculesPerTask;
            if(tmpMoleculeModulo > 0){
                tmpToIndex++;
                tmpMoleculeModulo--;
            }
            if (i == tmpNumberOfTasks - 1 ) {
                tmpToIndex = aListOfMolecules.size();
            }
        }
        List<Future<Integer>> tmpFuturesList;
        long tmpMemoryConsumption = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024*1024);
        FragmentationService.LOGGER.log(Level.INFO, "Fragmentation \"{0}\" ({1}) starting. Current memory consumption: {2} MB",
                new Object[]{aFragmentationName, aFragmenter.getFragmentationAlgorithmDisplayName(), tmpMemoryConsumption});
        long tmpStartTime = System.currentTimeMillis();
        int tmpExceptionsCounter = 0;
        tmpFuturesList = this.executorService.invokeAll(tmpFragmentationTaskList);
        if (this.executorService.isShutdown() || this.executorService.isTerminated()) {
            FragmentationService.LOGGER.log(Level.INFO, "Fragmentation cancelled");
            return null;
        }
        for (Future<Integer> tmpFuture : tmpFuturesList) {
            //execution exceptions do not get handled here because this is called inside another thread
            try {
                Integer tmpResult = tmpFuture.get();
                if (!Objects.isNull(tmpResult)) {
                    tmpExceptionsCounter += tmpFuture.get();
                } else {
                    //this can occur when the task has been interrupted or cancelled, nothing to do here
                    //errors in execution will be thrown by get() and are checked in the calling method/thread
                }
            } catch (CancellationException | InterruptedException | ExecutionException aCancellationOrInterruptionException) {
                FragmentationService.LOGGER.log(Level.WARNING, aCancellationOrInterruptionException.toString(), aCancellationOrInterruptionException);
                Thread.currentThread().interrupt();
                //continue;
            }
        }
        int tmpFragmentAmount = 0;
        Set<String> tmpKeySet = tmpFragmentHashtable.keySet();
        for(String tmpKey : tmpKeySet){
            tmpFragmentAmount += tmpFragmentHashtable.get(tmpKey).getAbsoluteFrequency();
        }
        if (tmpFragmentAmount != 0) {
            for(String tmpKey : tmpKeySet){
                tmpFragmentHashtable.get(tmpKey).setAbsolutePercentage(1.0 * tmpFragmentHashtable.get(tmpKey).getAbsoluteFrequency() / tmpFragmentAmount);
                tmpFragmentHashtable.get(tmpKey).setMoleculePercentage(1.0 * tmpFragmentHashtable.get(tmpKey).getMoleculeFrequency() / aListOfMolecules.size());
            }
        } else {
            FragmentationService.LOGGER.log(Level.WARNING, "Sum of absolute frequencies of fragments was 0! Percentages could not be calculated.");
        }
        if (tmpExceptionsCounter > 0) {
            FragmentationService.LOGGER.log(Level.WARNING, "Fragmentation \"{0}\" ({1}) caused {2} exceptions",
                    new Object[]{aFragmentationName, aFragmenter.getFragmentationAlgorithmDisplayName(), tmpExceptionsCounter});
        }
        this.executorService.shutdown();
        tmpMemoryConsumption = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024*1024);
        long tmpEndTime = System.currentTimeMillis();
        long tmpDuration = tmpEndTime - tmpStartTime;
        FragmentationService.LOGGER.log(Level.INFO,
                "Fragmentation \"{0}\" ({1}) of {2} molecules complete. It took {3} ms. Current memory consumption: {4} MB",
                new Object[]{aFragmentationName, aFragmenter.getFragmentationAlgorithmDisplayName(), aListOfMolecules.size(), tmpDuration, tmpMemoryConsumption});
        return tmpFragmentHashtable;
    }
    //
    /**
     * Checks the available fragmenters and their settings for restrictions imposed by persistence. Throws an exception if
     * anything does not meet the requirements.
     * - setting names must be singletons
     * - setting names and values must adhere to the preference input restrictions
     * - setting values are only tested for their current state, not the entire possible input space! It is tested again at persistence
     *
     * @throws UnsupportedOperationException if a setting does not fulfil the requirements
     */
    private void checkFragmenters() throws UnsupportedOperationException {
        int tmpAlgorithmNamesSetInitCapacity = CollectionUtil.calculateInitialHashCollectionCapacity(this.fragmenters.length,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        HashSet<String> tmpAlgorithmNamesSet = new HashSet<>(tmpAlgorithmNamesSetInitCapacity, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        HashSet<String> tmpAlgorithmDisplayNamesSet = new HashSet<>(tmpAlgorithmNamesSetInitCapacity, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        for (IMoleculeFragmenter tmpFragmenter : this.fragmenters) {
            //algorithm name should be singleton and must be persistable
            String tmpAlgName = tmpFragmenter.getFragmentationAlgorithmName();
            if (!PreferenceUtil.isValidName(tmpAlgName) || !SingleTermPreference.isValidContent(tmpAlgName)) {
                throw new UnsupportedOperationException("Algorithm name " + tmpAlgName + " is invalid.");
            }
            if (tmpAlgorithmNamesSet.contains(tmpAlgName)) {
                throw new UnsupportedOperationException("Algorithm name " + tmpAlgName + " is used multiple times.");
            } else {
                tmpAlgorithmNamesSet.add(tmpAlgName);
            }
            //algorithm display name should be singleton and must be persistable
            String tmpAlgDisplayName = tmpFragmenter.getFragmentationAlgorithmDisplayName();
            if (!PreferenceUtil.isValidName(tmpAlgDisplayName) || !SingleTermPreference.isValidContent(tmpAlgDisplayName)) {
                throw new UnsupportedOperationException("Algorithm name " + tmpAlgDisplayName + " is invalid.");
            }
            if (tmpAlgorithmDisplayNamesSet.contains(tmpAlgDisplayName)) {
                throw new UnsupportedOperationException("Algorithm name " + tmpAlgDisplayName + " is used multiple times.");
            } else {
                tmpAlgorithmDisplayNamesSet.add(tmpAlgDisplayName);
            }
            //setting names must be singletons within the respective class
            //setting names and values must adhere to the preference input restrictions
            //setting values are only tested for their current state, not the entire possible input space! It is tested again at persistence
            List<Property<?>> tmpSettingsList = tmpFragmenter.settingsProperties();
            PreferenceUtil.checkPropertiesForPreferenceRestrictions(tmpSettingsList);
        }
    }
    //</editor-fold>
}
