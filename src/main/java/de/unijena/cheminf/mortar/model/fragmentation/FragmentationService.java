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

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
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

    /**
     * Manages the fragmentation, creates {@link FragmentationTask} equal to the amount of {@param aNumberOfTasks}, assigns the molecules of {@param aListOfMolecules} to them and starts the fragmentation
     *
     * @param aListOfMolecules
     * @param aNumberOfTasks
     * @throws Exception
     */
    public void startFragmentation(List<MoleculeDataModel> aListOfMolecules, int aNumberOfTasks) throws Exception {
        int tmpNumberOfTasks = aNumberOfTasks;
        String tmpFragmentationName = this.selectedFragmenter.getFragmentationAlgorithmName();
        if(this.existingFragmentations.contains(tmpFragmentationName)){
            int tmpIndex = 0;
            do {
                tmpFragmentationName = this.selectedFragmenter.getFragmentationAlgorithmName() + "_" + ++tmpIndex;
            }
            while(this.existingFragmentations.contains(tmpFragmentationName));
        }
        this.existingFragmentations.add(tmpFragmentationName);
        this.currentFragmentationName = tmpFragmentationName;
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
            IMoleculeFragmenter tmpFragmenterForTask = this.selectedFragmenter.copy();
            tmpFragmentationTaskList.add (new FragmentationTask(tmpMoleculesForTask, tmpFragmenterForTask, tmpFragmentHashtable, tmpFragmentationName));
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
        this.fragments = tmpFragmentHashtable;
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
    //</editor-fold>
}
