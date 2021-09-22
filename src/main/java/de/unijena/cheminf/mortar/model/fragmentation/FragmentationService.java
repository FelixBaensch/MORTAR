/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2020  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas-schaub@uni-jena.de)
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

import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.ErtlFunctionalGroupsFinderFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.IMoleculeFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.SugarRemovalUtilityFragmenter;
import de.unijena.cheminf.mortar.model.util.ChemUtil;

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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service class for fragmentation
 *
 * @author Jonas Schaub, Felix Baensch
 */
public class FragmentationService {

    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * Array for the different fragmentation algorithms
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
     * Ertl
     */
    private IMoleculeFragmenter ertlFGF;
    /**
     * Sugar removal
     */
    private IMoleculeFragmenter sugarRUF;
    /**
     * List of  names of fragmentation algorithms that have already been run
     */
    private List<String> existingFragmentations;
    /**
     * Hashtable for fragments
     */
    private Hashtable<String, FragmentDataModel> fragments;
    /**
     *
     */
    private String currentFragmentationName;
    private String pipeliningFragmentationName;
    //</editor-fold>
    //<editor-fold desc="private static final class variables" defaultstate="collapsed">
    /**
     * Logger
     */
    private static final Logger LOGGER = Logger.getLogger(FragmentationService.class.getName());
    //</editor-fold>

    /**
     * Constructor
     */
    public FragmentationService(){
        this.fragmenters = new IMoleculeFragmenter[2];
        this.ertlFGF = new ErtlFunctionalGroupsFinderFragmenter();
        this.fragmenters[0] = this.ertlFGF;
        this.sugarRUF = new SugarRemovalUtilityFragmenter();
        this.fragmenters[1] = this.sugarRUF;
        this.existingFragmentations = new LinkedList<String>();
    }

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
        for(int i = 1; i < this.pipelineFragmenter.length; i++){
            String tmpFragmentationName = this.createAndCheckFragmentationName(tmpPipelineFragmentationName + "_" + this.pipelineFragmenter[i].getFragmentationAlgorithmName());
            tmpFragmentHashtable = this.startFragmentation(tmpMolecules, aNumberOfTasks, this.pipelineFragmenter[i], tmpFragmentationName);
            if(i!=0){
                for(MoleculeDataModel tmpParentMol : aListOfMolecules){
                    LinkedList<MoleculeDataModel> tmpNewFragments = new LinkedList<>();
                    HashMap<String, Integer> tmpNewFrequencies = new HashMap<>(tmpParentMol.getFragmentsOfSpecificAlgorithm(tmpPipelineFragmentationName).size()*2);
                    for(MoleculeDataModel tmpChildMol : tmpMolecules){
                        if(tmpParentMol.getFragmentsOfSpecificAlgorithm(tmpPipelineFragmentationName).contains(tmpChildMol)){
                            if(tmpChildMol.getFragmentsOfSpecificAlgorithm(tmpFragmentationName).size() <= 1){
                                tmpNewFragments.add(tmpChildMol);
                                String tmpKey = ChemUtil.createUniqueSmiles(tmpChildMol.getAtomContainer());
                                tmpNewFrequencies.put(tmpKey, tmpParentMol.getFragmentFrequencyOfSpecificAlgorithm(tmpPipelineFragmentationName).get(tmpKey));
                            }
                            else{
                                for(FragmentDataModel tmpFrag : tmpChildMol.getFragmentsOfSpecificAlgorithm(tmpFragmentationName)){
                                    tmpNewFragments.add(tmpFrag);
                                    String tmpKey = ChemUtil.createUniqueSmiles(tmpFrag.getAtomContainer());
                                    tmpNewFrequencies.put(tmpKey, tmpChildMol.getFragmentFrequencyOfSpecificAlgorithm(tmpFragmentationName).get(tmpKey) * tmpParentMol.getFragmentFrequencyOfSpecificAlgorithm(tmpPipelineFragmentationName).get(ChemUtil.createUniqueSmiles(tmpChildMol.getAtomContainer())));
                                }
                            }
                        }
                    }
                    tmpParentMol.getAllFragments().replace(tmpPipelineFragmentationName, (List<FragmentDataModel>)(List<?>)tmpNewFragments); //one cannot cast a generic type to another, but through an intermediate wild card type
                    tmpParentMol.getFragmentFrequencies().replace(tmpPipelineFragmentationName, tmpNewFrequencies);
                }
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
        System.out.println("TODO: Remove after debugging");
    }

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

    public IMoleculeFragmenter createNewFragmenterObjectByName(String anAlgorithmName) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        String tmpClassName = "";
        for (IMoleculeFragmenter tmpFragmenter : this.fragmenters) {
            if (anAlgorithmName.equals(tmpFragmenter.getFragmentationAlgorithmName()))
               tmpClassName = tmpFragmenter.getClass().getName();
        }
        if(tmpClassName.isBlank() || tmpClassName.isEmpty()){
            //ToDo throw exception
        }
        Class tmpClazz = Class.forName(tmpClassName);
        Constructor tmpCtor = tmpClazz.getConstructor();

        return (IMoleculeFragmenter) tmpCtor.newInstance();
    }

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
        ExecutorService tmpExecutor = Executors.newFixedThreadPool(tmpNumberOfTasks);
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
            tmpFuturesList = tmpExecutor.invokeAll(tmpFragmentationTaskList);
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
        tmpExecutor.shutdown();
        tmpMemoryConsumption = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024*1024);
        long tmpEndTime = System.currentTimeMillis();
        FragmentationService.LOGGER.info("Fragmentation \"" + tmpFragmentationName + "\" of " + aListOfMolecules.size()
                + " molecules complete. It took " + (tmpEndTime - tmpStartTime) + " ms. Current memory consumption: "
                + tmpMemoryConsumption + " MB");
        return tmpFragmentHashtable;
    }

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
}
