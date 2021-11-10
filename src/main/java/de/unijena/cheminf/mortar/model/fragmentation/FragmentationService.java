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

import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.ErtlFunctionalGroupsFinderFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.IMoleculeFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.SugarRemovalUtilityFragmenter;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.ChemUtil;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;
import de.unijena.cheminf.mortar.preference.BooleanPreference;
import de.unijena.cheminf.mortar.preference.IPreference;
import de.unijena.cheminf.mortar.preference.PreferenceContainer;
import de.unijena.cheminf.mortar.preference.PreferenceUtil;
import de.unijena.cheminf.mortar.preference.SingleIntegerPreference;
import de.unijena.cheminf.mortar.preference.SingleNumberPreference;
import de.unijena.cheminf.mortar.preference.SingleTermPreference;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
 */
public class FragmentationService {
    //<editor-fold desc="public static final constants">
    /**
     * Default selected fragmenter
     */
    public static final String DEFAULT_SELECTED_FRAGMENTER_ALGORITHM_NAME = ErtlFunctionalGroupsFinderFragmenter.ALGORITHM_NAME;

    /**
     * Default pipeline name
     */
    public static final String DEFAULT_PIPELINE_NAME = Message.get("FragmentationService.defaultPipelineName");
    //</editor-fold>
    //
    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * Array for the different fragmentation algorithms available
     */
    private IMoleculeFragmenter[] fragmenters;
    /**
     * Selected fragmentation algorithm
     */
    private IMoleculeFragmenter selectedFragmenter;
    /**
     * Array for the fragmentation algorithms to use during pipeline fragmentation
     */
    private IMoleculeFragmenter[] pipelineFragmenter;
    /**
     * Ertl algorithm fragmenter
     */
    private IMoleculeFragmenter ertlFGF;
    /**
     * Sugar Removal Utility fragmenter
     */
    private IMoleculeFragmenter sugarRUF;
    /**
     * List of names of fragmentation algorithms that have already been run
     */
    private List<String> existingFragmentations;
    /**
     * Hashtable for fragments
     */
    private Hashtable<String, FragmentDataModel> fragments;
    /**
     * String for the name of the current fragmentation algorithm
     */
    private String currentFragmentationName;
    /**
     * String for the name of the current pipeline fragmentation
     */
    private String pipeliningFragmentationName;
    /**
     * ExecutorService for the fragmentation tasks
     */
    private ExecutorService executorService;
    //</editor-fold>
    //
    //<editor-fold desc="private static final class variables" defaultstate="collapsed">
    /**
     * Logger
     */
    private static final Logger LOGGER = Logger.getLogger(FragmentationService.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="Constructors">
    /**
     * Constructor, instantiates the fragmenters and sets the selected fragmenter and the pipeline to their defaults.
     */
    public FragmentationService(){
        this.fragmenters = new IMoleculeFragmenter[2];
        this.ertlFGF = new ErtlFunctionalGroupsFinderFragmenter();
        this.fragmenters[0] = this.ertlFGF;
        this.sugarRUF = new SugarRemovalUtilityFragmenter();
        this.fragmenters[1] = this.sugarRUF;
        //TODO check fragmenters for restrictions the persistence gives and throw exception if they are not met
        //algorithm name should be singleton
        //settings names and values must adhere to the preference input restrictions
        //setting names must be singletons
        for (IMoleculeFragmenter tmpFragmenter : this.fragmenters) {
            if (tmpFragmenter.getFragmentationAlgorithmName().equals(FragmentationService.DEFAULT_SELECTED_FRAGMENTER_ALGORITHM_NAME)) {
                this.selectedFragmenter = tmpFragmenter;
            }
        }
        if (Objects.isNull(this.selectedFragmenter)) {
            this.selectedFragmenter = this.ertlFGF;
        }
        try {
            this.pipelineFragmenter = new IMoleculeFragmenter[] {this.createNewFragmenterObjectByName(this.selectedFragmenter.getFragmentationAlgorithmName())};
        } catch (Exception anException) {
            //settings of this fragmenter instance are still in default at this point
            this.pipelineFragmenter = new IMoleculeFragmenter[] {this.selectedFragmenter.copy()};
        }
        this.pipeliningFragmentationName = FragmentationService.DEFAULT_PIPELINE_NAME;
        this.existingFragmentations = new LinkedList<String>();
        //fragments hash table, current fragmentation name, and executor service are only instantiated when needed
    }
    //</editor-fold>
    //
    //<editor-fold desc="public methods" defaultstate="collapsed">
    /**
     * Manages the fragmentation, creates a number of {@link FragmentationTask} objects equal to the amount of
     * {@param aNumberOfTasks}, assigns the molecules of {@param aListOfMolecules} to them and starts the fragmentation.
     *
     * @param aListOfMolecules list of molecules to fragment
     * @param aNumberOfTasks how many parallel tasks should be used
     * @throws Exception if anything goes wrong
     */
    public void startSingleFragmentation(List<MoleculeDataModel> aListOfMolecules, int aNumberOfTasks) throws Exception{
        //<editor-fold desc="checks" defualtstate="collapsed">
        Objects.requireNonNull(aListOfMolecules, "aListOfMolecules must not be null");
        if(aNumberOfTasks == 0){
            aNumberOfTasks = 1;
        }
        //</editor-fold>
        String tmpFragmentationName = this.createAndCheckFragmentationName(this.selectedFragmenter.getFragmentationAlgorithmName());
        this.existingFragmentations.add(tmpFragmentationName);
        this.currentFragmentationName = tmpFragmentationName;
        this.fragments = this.startFragmentation(aListOfMolecules, aNumberOfTasks, this.selectedFragmenter, tmpFragmentationName);
    }

    /**
     * Starts fragmentation pipeline for given List of molecules.
     * Fragmentation will be done on fragments of previous step
     * TODO
     *
     * @param aListOfMolecules List<MoleculeDataModel>
     * @param aNumberOfTasks int
     * @throws Exception
     */
    public void startPipelineFragmentation(List<MoleculeDataModel> aListOfMolecules, int aNumberOfTasks) throws Exception{
        //<editor-fold desc="checks" defualtstate="collapsed">
        Objects.requireNonNull(aListOfMolecules, "aListOfMolecules must not be null");
        Objects.requireNonNull(this.pipelineFragmenter, "pipelineFragmenter must not be null");
        if(aNumberOfTasks == 0){
            aNumberOfTasks = 1;
        }
        //</editor-fold>
        this.fragments = new Hashtable<>(aListOfMolecules.size() * this.pipelineFragmenter.length);
        Hashtable<String, FragmentDataModel> tmpFragmentHashtable = null;
        if(this.pipeliningFragmentationName == null || this.pipeliningFragmentationName.isEmpty()){
            this.pipeliningFragmentationName = "Pipeline";
        }
        String tmpPipelineFragmentationName = this.createAndCheckFragmentationName(this.pipeliningFragmentationName);
        this.existingFragmentations.add(tmpPipelineFragmentationName);
        this.currentFragmentationName = tmpPipelineFragmentationName;
        this.fragments = this.startFragmentation(aListOfMolecules, aNumberOfTasks, this.pipelineFragmenter[0], tmpPipelineFragmentationName);
        List<MoleculeDataModel> tmpMolecules = new LinkedList<>();
        tmpMolecules.addAll(this.fragments.values());
        for (int i = 1; i < this.pipelineFragmenter.length; i++) {
            String tmpFragmentationName = this.createAndCheckFragmentationName(tmpPipelineFragmentationName + "_" + this.pipelineFragmenter[i].getFragmentationAlgorithmName());
            tmpFragmentHashtable = this.startFragmentation(tmpMolecules, aNumberOfTasks, this.pipelineFragmenter[i], tmpFragmentationName);
            for (MoleculeDataModel tmpParentMol : aListOfMolecules) {
                LinkedList<MoleculeDataModel> tmpNewFragments = new LinkedList<>();
                HashMap<String, Integer> tmpNewFrequencies = new HashMap<>(tmpParentMol.getFragmentsOfSpecificAlgorithm(tmpPipelineFragmentationName).size() * 2);
                for (MoleculeDataModel tmpChildMol : tmpMolecules) {
                    if (tmpParentMol.getFragmentsOfSpecificAlgorithm(tmpPipelineFragmentationName).contains(tmpChildMol)) {
                        if (tmpChildMol.getFragmentsOfSpecificAlgorithm(tmpFragmentationName).size() <= 1) {
                            tmpNewFragments.add(tmpChildMol);
                            String tmpKey = ChemUtil.createUniqueSmiles(tmpChildMol.getAtomContainer());
                            tmpNewFrequencies.put(tmpKey, tmpParentMol.getFragmentFrequencyOfSpecificAlgorithm(tmpPipelineFragmentationName).get(tmpKey));
                        } else {
                            for (FragmentDataModel tmpFrag : tmpChildMol.getFragmentsOfSpecificAlgorithm(tmpFragmentationName)) {
                                tmpNewFragments.add(tmpFrag);
                                String tmpKey = ChemUtil.createUniqueSmiles(tmpFrag.getAtomContainer());
                                tmpNewFrequencies.put(tmpKey, tmpChildMol.getFragmentFrequencyOfSpecificAlgorithm(tmpFragmentationName).get(tmpKey) * tmpParentMol.getFragmentFrequencyOfSpecificAlgorithm(tmpPipelineFragmentationName).get(ChemUtil.createUniqueSmiles(tmpChildMol.getAtomContainer())));
                            }
                        }
                    }
                }
                tmpParentMol.getAllFragments().replace(tmpPipelineFragmentationName, (List<FragmentDataModel>) (List<?>) tmpNewFragments); //one cannot cast a generic type to another, but through an intermediate wild card type
                tmpParentMol.getFragmentFrequencies().replace(tmpPipelineFragmentationName, tmpNewFrequencies);
            }
            Hashtable<String, FragmentDataModel> tmpFragmentsHash = new Hashtable<>(tmpFragmentHashtable.size());
            for(MoleculeDataModel tmpMol : aListOfMolecules){
                for(FragmentDataModel tmpFrag : tmpMol.getFragmentsOfSpecificAlgorithm(tmpPipelineFragmentationName)){
                    String tmpSmiles = ChemUtil.createUniqueSmiles(tmpFrag.getAtomContainer());
                    if(!tmpFragmentsHash.containsKey(tmpSmiles)){
                        tmpFragmentsHash.put(tmpSmiles, tmpFrag);
                    }
                }
            }
            tmpMolecules.clear();
            tmpMolecules.addAll(tmpFragmentsHash.values());
            if(i == this.pipelineFragmenter.length-1){
                this.fragments = tmpFragmentsHash;
            }
        }
        int tmpFragmentAmount = 0;
        Set<String> tmpKeySet = this.fragments.keySet();
        for(String tmpKey : tmpKeySet){
            tmpFragmentAmount += this.fragments.get(tmpKey).getAbsoluteFrequency();
            this.fragments.get(tmpKey).setMoleculeFrequency(0);
            for(MoleculeDataModel tmpParentMol : aListOfMolecules){
                if(tmpParentMol.getFragmentsOfSpecificAlgorithm(tmpPipelineFragmentationName).contains(this.fragments.get(tmpKey))){
                    this.fragments.get(tmpKey).incrementMoleculeFrequency();
                }
            }
        }
        for(String tmpKey : tmpKeySet){
            this.fragments.get(tmpKey).setAbsolutePercentage(1.0 * this.fragments.get(tmpKey).getAbsoluteFrequency() / tmpFragmentAmount);
            this.fragments.get(tmpKey).setMoleculePercentage(1.0 * this.fragments.get(tmpKey).getMoleculeFrequency() / aListOfMolecules.size());
        }
     }

    /**
     * Under construction
     * Start fragmentation pipeline
     * Fragmentation will be done molecule by molecule
     * TODO: After adapting the data models, this method must be modified so that the resulting fragments are kept separate for each molecule.
     *
     * @param aListOfMolecules List<MoleculeDataModel>
     * @param aNumberOfTasks int
     * @throws Exception
     */
    public void startPipelineFragmentationMolByMol(List<MoleculeDataModel> aListOfMolecules, int aNumberOfTasks) throws Exception{
        //<editor-fold desc="checks" defualtstate="collapsed">
        Objects.requireNonNull(aListOfMolecules, "aListOfMolecules must not be null");
        Objects.requireNonNull(this.pipelineFragmenter, "pipelineFragmenter must not be null");
        if(aNumberOfTasks == 0){
            aNumberOfTasks = 1;
        }
        //</editor-fold>
        this.fragments = new Hashtable<>(aListOfMolecules.size() * this.pipelineFragmenter.length);
        Hashtable<String, FragmentDataModel> tmpFragmentHashtable = null;
        if(this.pipeliningFragmentationName == null || this.pipeliningFragmentationName.isEmpty()){
            this.pipeliningFragmentationName = "Pipeline";
        }
        String tmpPipelineFragmentationName = this.createAndCheckFragmentationName(this.pipeliningFragmentationName);
        this.existingFragmentations.add(tmpPipelineFragmentationName);
        this.currentFragmentationName = tmpPipelineFragmentationName;

        this.fragments = this.startFragmentation(aListOfMolecules, aNumberOfTasks, this.pipelineFragmenter[0], tmpPipelineFragmentationName);

        for(int i = 1; i < this.pipelineFragmenter.length; i++){
            String tmpFragmentationName = this.createAndCheckFragmentationName(tmpPipelineFragmentationName + "_" + this.pipelineFragmenter[i].getFragmentationAlgorithmName());

            for(MoleculeDataModel tmpParentMol : aListOfMolecules){
                List<MoleculeDataModel> tmpChildMols = (List<MoleculeDataModel>)(List<?>) tmpParentMol.getFragmentsOfSpecificAlgorithm(tmpPipelineFragmentationName);
                tmpFragmentHashtable = this.startFragmentation(tmpChildMols, aNumberOfTasks, this.pipelineFragmenter[i], tmpFragmentationName);
                Set<String> tmpKeySet = tmpFragmentHashtable.keySet();
                LinkedList<FragmentDataModel> tmpFrags = new LinkedList<>();
                for(String tmpKey : tmpKeySet){
                    tmpFrags.add(tmpFragmentHashtable.get(tmpKey));
                }
                HashMap<String, Integer> tmpNewFrequencies = new HashMap<>(tmpParentMol.getFragmentsOfSpecificAlgorithm(tmpPipelineFragmentationName).size()*2);
                for(MoleculeDataModel tmpChildMol : tmpChildMols){
                    for(FragmentDataModel tmpFrag : tmpChildMol.getFragmentsOfSpecificAlgorithm(tmpFragmentationName)){
                        String tmpKey = ChemUtil.createUniqueSmiles(tmpFrag.getAtomContainer());
                        if(tmpNewFrequencies.containsKey(tmpKey)){
                            tmpNewFrequencies.replace(
                                    tmpKey,
                                    tmpNewFrequencies.get(tmpKey) +
                                            tmpChildMol.getFragmentFrequencyOfSpecificAlgorithm(tmpFragmentationName).get(tmpKey) *
                                                    tmpParentMol.getFragmentFrequencyOfSpecificAlgorithm(tmpPipelineFragmentationName).get(ChemUtil.createUniqueSmiles(tmpChildMol.getAtomContainer()))
                            );
                        }else{
                             tmpNewFrequencies.put(
                                    tmpKey,
                                    tmpChildMol.getFragmentFrequencyOfSpecificAlgorithm(tmpFragmentationName).get(tmpKey) *
                                            tmpParentMol.getFragmentFrequencyOfSpecificAlgorithm(tmpPipelineFragmentationName).get(ChemUtil.createUniqueSmiles(tmpChildMol.getAtomContainer()))
                            );
                        }
                    }
                }
                tmpParentMol.getFragmentFrequencies().replace(tmpPipelineFragmentationName, tmpNewFrequencies);
                tmpParentMol.getAllFragments().replace(tmpPipelineFragmentationName, tmpFrags);
            }
        }
        Hashtable<String, FragmentDataModel> tmpFragmentsHash = new Hashtable<>(this.fragments.size() * this.pipelineFragmenter.length);
        for(MoleculeDataModel tmpMol : aListOfMolecules){
            for(FragmentDataModel tmpFrag : tmpMol.getFragmentsOfSpecificAlgorithm(tmpPipelineFragmentationName))
            {
                String tmpKey = ChemUtil.createUniqueSmiles(tmpFrag.getAtomContainer());
                if(!tmpFragmentsHash.containsKey(tmpKey)){
                    tmpFragmentsHash.put(tmpKey, tmpFrag);
                    tmpFrag.setAbsoluteFrequency(tmpMol.getFragmentFrequencyOfSpecificAlgorithm(tmpPipelineFragmentationName).get(tmpKey));
                    tmpFrag.setMoleculeFrequency(1);
                }
                else{
                    tmpFragmentsHash.get(tmpKey).setAbsoluteFrequency(tmpFragmentsHash.get(tmpKey).getAbsoluteFrequency() + tmpMol.getFragmentFrequencyOfSpecificAlgorithm(tmpPipelineFragmentationName).get(tmpKey));
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
        for(String tmpKey : tmpKeySet){
            this.fragments.get(tmpKey).setAbsolutePercentage(1.0 * this.fragments.get(tmpKey).getAbsoluteFrequency() / tmpFragmentAmount);
            this.fragments.get(tmpKey).setMoleculePercentage(1.0 * this.fragments.get(tmpKey).getMoleculeFrequency() / aListOfMolecules.size());
        }
    }

    //TODO: rename to createNewFragmenterObjectByALGORITHMName?
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
    public IMoleculeFragmenter createNewFragmenterObjectByName(String anAlgorithmName)
            throws IllegalArgumentException,
            ClassNotFoundException,
            NoSuchMethodException,
            InvocationTargetException,
            InstantiationException,
            IllegalAccessException {
        String tmpClassName = "";
        for (IMoleculeFragmenter tmpFragmenter : this.fragmenters) {
            if (anAlgorithmName.equals(tmpFragmenter.getFragmentationAlgorithmName()))
               tmpClassName = tmpFragmenter.getClass().getName();
        }
        if(tmpClassName.isBlank() || tmpClassName.isEmpty()){
            throw new IllegalArgumentException("Given algorithm name " + anAlgorithmName + " is invalid.");
        }
        Class tmpClazz = Class.forName(tmpClassName);
        Constructor tmpCtor = tmpClazz.getConstructor();
        return (IMoleculeFragmenter) tmpCtor.newInstance();
    }

    /**
     * 
     */
    public void persistFragmenterSettings() {
        for (IMoleculeFragmenter tmpFragmenter : this.fragmenters) {
            if (Objects.isNull(tmpFragmenter)) {
                continue;
            }
            List<Property> tmpSettings = tmpFragmenter.settingsProperties();
            if (Objects.isNull(tmpSettings)) {
                continue;
            }
            String tmpFilePath = FileUtil.getSettingsDirPath()
                    + tmpFragmenter.getClass().getSimpleName()
                    + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION;
            try {
                PreferenceContainer tmpPrefContainer = PreferenceUtil.translateJavaFxPropertiesToPreferences(tmpSettings, tmpFilePath);
                tmpPrefContainer.writeRepresentation();
            } catch (NullPointerException | IllegalArgumentException | IOException | SecurityException anException) {
                FragmentationService.LOGGER.log(Level.WARNING, "Fragmenter settings persistence went wrong, exception: " + anException.toString(), anException);
                GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),Message.get("Error.ExceptionAlert.Header"),Message.get("FragmentationService.Error.settingsPersistence"),anException);
                continue;
            }
        }
    }

    /**
     *
     */
    public void persistActiveFragmenterAndPipeline() throws IOException, SecurityException {
        String tmpFragmentationServiceSettingsPath = FileUtil.getSettingsDirPath() + "FragmentationService" + File.separator;
        File tmpFragmentationServiceSettingsDir = new File(tmpFragmentationServiceSettingsPath);
        if (!tmpFragmentationServiceSettingsDir.exists()) {
            tmpFragmentationServiceSettingsDir.mkdirs();
        } else {
            FileUtil.deleteAllFilesInDirectory(tmpFragmentationServiceSettingsPath);
        }
        PreferenceContainer tmpFragmentationServiceSettingsContainer = new PreferenceContainer(
                tmpFragmentationServiceSettingsPath
                        + "FragmentationServiceSettings"
                        + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION);
        SingleTermPreference tmpSelectedFragmenterPreference = new SingleTermPreference("SelectedFragmenter", this.selectedFragmenter.getClass().getSimpleName());
        tmpFragmentationServiceSettingsContainer.add(tmpSelectedFragmenterPreference);
        if (Objects.isNull(this.pipeliningFragmentationName)) {
            this.pipeliningFragmentationName = "Pipeline";
        }
        SingleTermPreference tmpPipelineNamePreference = new SingleTermPreference("PipelineName", this.pipeliningFragmentationName);
        tmpFragmentationServiceSettingsContainer.add(tmpPipelineNamePreference);
        tmpFragmentationServiceSettingsContainer.add(new SingleIntegerPreference("PipelineSize", this.pipelineFragmenter.length));
        tmpFragmentationServiceSettingsContainer.writeRepresentation();
        for (int i = 0; i < this.pipelineFragmenter.length; i++) {
            IMoleculeFragmenter tmpFragmenter = this.pipelineFragmenter[i];
            List<Property> tmpSettings = tmpFragmenter.settingsProperties();
            String tmpFilePath = tmpFragmentationServiceSettingsPath + "PipelineFragmenter_" + i + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION;
            PreferenceContainer tmpPrefContainer = PreferenceUtil.translateJavaFxPropertiesToPreferences(tmpSettings, tmpFilePath);
            tmpPrefContainer.add(new SingleTermPreference("ClassName", tmpFragmenter.getFragmentationAlgorithmName()));
            tmpPrefContainer.writeRepresentation();
        }
    }

    /**
     * TODO
     */
    public void reloadFragmenterSettings() {
        for (IMoleculeFragmenter tmpFragmenter : this.fragmenters) {
            String tmpClassName = tmpFragmenter.getClass().getSimpleName();
            File tmpFragmenterSettingsFile = new File(FileUtil.getSettingsDirPath()
                    + tmpClassName
                    + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION);
            if (tmpFragmenterSettingsFile.exists() && tmpFragmenterSettingsFile.isFile() && tmpFragmenterSettingsFile.canRead()) {
                PreferenceContainer tmpContainer;
                try {
                    tmpContainer = new PreferenceContainer(tmpFragmenterSettingsFile);
                } catch (IllegalArgumentException | IOException anException) {
                    FragmentationService.LOGGER.log(Level.WARNING, anException.toString(), anException);
                    continue;
                }
                for (Property tmpSettingProperty : tmpFragmenter.settingsProperties()) {
                    String tmpPropertyName = tmpSettingProperty.getName();
                    if (tmpContainer.containsPreferenceName(tmpPropertyName)) {
                        IPreference[] tmpPreferences = tmpContainer.getPreferences(tmpPropertyName);
                        try {
                            if (tmpSettingProperty instanceof SimpleBooleanProperty) {
                                BooleanPreference tmpBooleanPreference = (BooleanPreference) tmpPreferences[0];
                                tmpSettingProperty.setValue(tmpBooleanPreference.getContent());
                            } else if (tmpSettingProperty instanceof SimpleIntegerProperty) {
                                SingleIntegerPreference tmpIntPreference = (SingleIntegerPreference) tmpPreferences[0];
                                tmpSettingProperty.setValue(tmpIntPreference.getContent());
                            } else if (tmpSettingProperty instanceof SimpleDoubleProperty) {
                                SingleNumberPreference tmpDoublePreference = (SingleNumberPreference) tmpPreferences[0];
                                tmpSettingProperty.setValue(tmpDoublePreference.getContent());
                            } else if (tmpSettingProperty instanceof SimpleEnumConstantNameProperty || tmpSettingProperty instanceof SimpleStringProperty) {
                                SingleTermPreference tmpStringPreference = (SingleTermPreference) tmpPreferences[0];
                                tmpSettingProperty.setValue(tmpStringPreference.getContent());
                            } else {
                                //setting will remain in default
                                FragmentationService.LOGGER.log(Level.WARNING, "Setting " + tmpPropertyName + " is of unknown type.");
                            }
                        } catch (ClassCastException aCastingException) {
                            FragmentationService.LOGGER.log(Level.WARNING, aCastingException.toString(), aCastingException);
                        }
                    } else {
                        //setting will remain in default
                        FragmentationService.LOGGER.log(Level.WARNING, "No persisted settings for " + tmpPropertyName + " available.");
                    }
                }
            } else {
                //settings will remain in default
                FragmentationService.LOGGER.log(Level.WARNING, "No persisted settings for " + tmpClassName + " available.");
            }
        }
    }

    /**
     *
     */
    public void reloadActiveFragmenterAndPipeline() {
        String tmpFragmentationServiceSettingsPath = FileUtil.getSettingsDirPath() + "FragmentationService" + File.separator;
        String tmpServiceSettingsFilePath = tmpFragmentationServiceSettingsPath
                + "FragmentationServiceSettings"
                + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION;
        File tmpServiceSettingsFile = new File(tmpServiceSettingsFilePath);
        if (tmpServiceSettingsFile.exists() && tmpServiceSettingsFile.isFile() && tmpServiceSettingsFile.canRead()) {
            PreferenceContainer tmpFragmentationServiceSettingsContainer;
            try {
                tmpFragmentationServiceSettingsContainer = new PreferenceContainer(tmpServiceSettingsFile);
                int tmpPipelineSize = ((SingleIntegerPreference)tmpFragmentationServiceSettingsContainer.getPreferences("PipelineSize")[0]).getContent();
                String tmpPipelineName = tmpFragmentationServiceSettingsContainer.getPreferences("PipelineName")[0].getContentRepresentative();
                String tmpSelectedFragmenterClassName = tmpFragmentationServiceSettingsContainer.getPreferences("SelectedFragmenter")[0].getContentRepresentative();
                this.pipeliningFragmentationName = tmpPipelineName;
                for (IMoleculeFragmenter tmpFragmenter : this.fragmenters) {
                    if (tmpFragmenter.getClass().getSimpleName().equals(tmpSelectedFragmenterClassName)) {
                        this.selectedFragmenter = tmpFragmenter;
                        break;
                    }
                }
                IMoleculeFragmenter[] tmpFragmenterArray = new IMoleculeFragmenter[tmpPipelineSize];
                for (int i = 0; i < tmpPipelineSize; i++) {
                    String tmpPath = tmpFragmentationServiceSettingsPath + "PipelineFragmenter_" + i + BasicDefinitions.PREFERENCE_CONTAINER_FILE_EXTENSION;
                    File tmpFragmenterFile = new File(tmpPath);
                    if (tmpFragmenterFile.exists() && tmpFragmenterFile.isFile() && tmpFragmenterFile.canRead()) {
                        PreferenceContainer tmpFragmenterSettingsContainer = new PreferenceContainer(tmpFragmenterFile);
                        String tmpFragmenterClassName = tmpFragmenterSettingsContainer.getPreferences("ClassName")[0].getContentRepresentative();
                        IMoleculeFragmenter tmpFragmenter = this.createNewFragmenterObjectByName(tmpFragmenterClassName);
                        for (Property tmpSettingProperty : tmpFragmenter.settingsProperties()) {
                            String tmpPropertyName = tmpSettingProperty.getName();
                            if (tmpFragmenterSettingsContainer.containsPreferenceName(tmpPropertyName)) {
                                IPreference[] tmpPreferences = tmpFragmenterSettingsContainer.getPreferences(tmpPropertyName);
                                try {
                                    if (tmpSettingProperty instanceof SimpleBooleanProperty) {
                                        BooleanPreference tmpBooleanPreference = (BooleanPreference) tmpPreferences[0];
                                        tmpSettingProperty.setValue(tmpBooleanPreference.getContent());
                                    } else if (tmpSettingProperty instanceof SimpleIntegerProperty) {
                                        SingleIntegerPreference tmpIntPreference = (SingleIntegerPreference) tmpPreferences[0];
                                        tmpSettingProperty.setValue(tmpIntPreference.getContent());
                                    } else if (tmpSettingProperty instanceof SimpleDoubleProperty) {
                                        SingleNumberPreference tmpDoublePreference = (SingleNumberPreference) tmpPreferences[0];
                                        tmpSettingProperty.setValue(tmpDoublePreference.getContent());
                                    } else if (tmpSettingProperty instanceof SimpleEnumConstantNameProperty || tmpSettingProperty instanceof SimpleStringProperty) {
                                        SingleTermPreference tmpStringPreference = (SingleTermPreference) tmpPreferences[0];
                                        tmpSettingProperty.setValue(tmpStringPreference.getContent());
                                    } else {
                                        //setting will remain in default
                                        FragmentationService.LOGGER.log(Level.WARNING, "Setting " + tmpPropertyName + " is of unknown type.");
                                    }
                                } catch (ClassCastException aCastingException) {
                                    FragmentationService.LOGGER.log(Level.WARNING, aCastingException.toString(), aCastingException);
                                }
                            } else {
                                //setting will remain in default
                                FragmentationService.LOGGER.log(Level.WARNING, "No persisted settings for " + tmpPropertyName + " available.");
                            }
                        }
                        tmpFragmenterArray[i] = tmpFragmenter;
                    } else {
                        //TODO
                    }
                }
                this.setPipelineFragmenter(tmpFragmenterArray);
            } catch (IllegalArgumentException | IOException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException anException) {
                FragmentationService.LOGGER.log(Level.WARNING, anException.toString(), anException);
                //TODO do more here
            }
        } else {
            //TODO
        }
    }

    /**
     * Shuts down executor service
     * Used as recommended by oracle (https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/ExecutorService.html)
     */
    public void abortExecutor(){
        this.executorService.shutdown();
        try {
            if (!this.executorService.awaitTermination(600, TimeUnit.MILLISECONDS)) {
                this.executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            this.executorService.shutdownNow();
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="public properties" defaultstate="collapsed">
    /**
     * Returns array of {@link IMoleculeFragmenter}
     * @return fragmenters
     */
    public IMoleculeFragmenter[] getFragmenters(){
        return this.fragmenters;
    }
    /**
     * Returns array of {@link IMoleculeFragmenter} for pipelining
     * @return pipelineFragmenters
     */
    public IMoleculeFragmenter[] getPipelineFragmenter(){
        return this.pipelineFragmenter;
    }
    /**
     * Returns selected {@link IMoleculeFragmenter}
     *
     * @return selectedFragmenter
     */
    public IMoleculeFragmenter getSelectedFragmenter(){
        return this.selectedFragmenter;
    }
    /**
     * Returns Hashtable of {@link FragmentDataModel}
     *
     * @return fragments (results of fragmentation)
     */
    public Hashtable<String, FragmentDataModel> getFragments(){
        return this.fragments;
    }
    /**
     * Returns name of the running fragmentation
     *
     * @return currentFragmentation
     */
    public String getCurrentFragmentationName(){
        return this.currentFragmentationName;
    }
    public String getPipeliningFragmentationName(){
        return this.pipeliningFragmentationName;
    }
    /**
     * Sets the selectedFragmenter
     * @param anAlgorithmName
     */
    public void setSelectedFragmenter(String anAlgorithmName){
        for (IMoleculeFragmenter tmpFragmenter : this.fragmenters) {
            if (anAlgorithmName.equals(tmpFragmenter.getFragmentationAlgorithmName()))
                this.selectedFragmenter = tmpFragmenter;
        }
    }

    /**
     * Sets the fragmenters to use for pipeline fragmentation
     * @param anArrayOfFragmenter IMolecueFragmenter[]
     */
    public void setPipelineFragmenter(IMoleculeFragmenter[] anArrayOfFragmenter){
        this.pipelineFragmenter = anArrayOfFragmenter;
    }
    public void setPipeliningFragmentationName(String aName){
        this.pipeliningFragmentationName = aName;
    }
    //</editor-fold>
    //<editor-fold desc="private methods">
    /**
     * Checks whether the name exists, if so, a consecutive number is appended, if not, the name is returned unchanged
     * @param anAlgorithmName String
     * @return String algorithm name
     */
    private String createAndCheckFragmentationName(String anAlgorithmName){
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

    /**
     * Manages the fragmentation, creates {@link FragmentationTask} equal to the amount of {@param aNumberOfTasks}, assigns the molecules of {@param aListOfMolecules} to them and starts the fragmentation
     *
     * @param aListOfMolecules
     * @param aNumberOfTasks
     * @throws Exception
     */
    private Hashtable<String, FragmentDataModel> startFragmentation(List<MoleculeDataModel> aListOfMolecules, int aNumberOfTasks, IMoleculeFragmenter aFragmenter, String aFragmentationName) throws Exception {
        int tmpNumberOfTasks = aNumberOfTasks;
        String tmpFragmentationName = aFragmentationName;
        Hashtable<String, FragmentDataModel> tmpFragmentHashtable = new Hashtable<>(aListOfMolecules.size() * 2);
        if(aListOfMolecules.size() < tmpNumberOfTasks){
            tmpNumberOfTasks = aListOfMolecules.size();
        }
        int tmpMoleculesPerTask = aListOfMolecules.size() / tmpNumberOfTasks;
        int tmpMoleculeModulo = aListOfMolecules.size() % tmpNumberOfTasks;
        //TODO refine this one
        int tmpFromIndex = 0; //low endpoint (inclusive) of the subList
        int tmpToIndex = tmpMoleculesPerTask; //high endpoint (exclusive) of the subList
        if(tmpMoleculeModulo > 0){
            tmpToIndex++;
            tmpMoleculeModulo--;
        }
        this.executorService = Executors.newFixedThreadPool(tmpNumberOfTasks);
        List<FragmentationTask> tmpFragmentationTaskList = new LinkedList<>();
        for(int i = 1; i <= tmpNumberOfTasks; i++){
            List<MoleculeDataModel> tmpMoleculesForTask = aListOfMolecules.subList(tmpFromIndex, tmpToIndex);
            IMoleculeFragmenter tmpFragmenterForTask = aFragmenter.copy();
            tmpFragmentationTaskList.add(new FragmentationTask(tmpMoleculesForTask, tmpFragmenterForTask, tmpFragmentHashtable, tmpFragmentationName));
            tmpFromIndex = tmpToIndex;
            tmpToIndex = tmpFromIndex + tmpMoleculesPerTask;
            if(tmpMoleculeModulo > 0){
                tmpToIndex++;
                tmpMoleculeModulo--;
            }
            if(i == tmpNumberOfTasks - 1 ){
                tmpToIndex = aListOfMolecules.size();
            }
        }
        List<Future<Integer>> tmpFuturesList;
        long tmpMemoryConsumption = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024*1024);
        FragmentationService.LOGGER.info("Fragmentation \"" + tmpFragmentationName
                + "\" starting. Current memory consumption: " + tmpMemoryConsumption + " MB");
        long tmpStartTime = System.currentTimeMillis();
        try {
            tmpFuturesList = this.executorService.invokeAll(tmpFragmentationTaskList);
        }catch (Exception anException){
            FragmentationService.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            throw anException; //TODO ? GUIAlert?
        }
        int tmpExceptionsCounter = 0;
        for (Future<Integer> tmpFuture : tmpFuturesList) {
            tmpExceptionsCounter += tmpFuture.get();
        }
        int tmpFragmentAmount = 0;
        Set<String> tmpKeySet = tmpFragmentHashtable.keySet();
        for(String tmpKey : tmpKeySet){
            tmpFragmentAmount += tmpFragmentHashtable.get(tmpKey).getAbsoluteFrequency();
        }
        for(String tmpKey : tmpKeySet){
            tmpFragmentHashtable.get(tmpKey).setAbsolutePercentage(1.0 * tmpFragmentHashtable.get(tmpKey).getAbsoluteFrequency() / tmpFragmentAmount);
            tmpFragmentHashtable.get(tmpKey).setMoleculePercentage(1.0 * tmpFragmentHashtable.get(tmpKey).getMoleculeFrequency() / aListOfMolecules.size());
        }
        if(tmpExceptionsCounter > 0){
            FragmentationService.LOGGER.log(Level.SEVERE, "Fragmentation \"" + tmpFragmentationName + "\" caused " + tmpExceptionsCounter + " exceptions");
        }
        this.executorService.shutdown();
        tmpMemoryConsumption = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024*1024);
        long tmpEndTime = System.currentTimeMillis();
        FragmentationService.LOGGER.info("Fragmentation \"" + tmpFragmentationName + "\" of " + aListOfMolecules.size()
                + " molecules complete. It took " + (tmpEndTime - tmpStartTime) + " ms. Current memory consumption: "
                + tmpMemoryConsumption + " MB");
        return tmpFragmentHashtable;
    }
    //</editor-fold>
}
