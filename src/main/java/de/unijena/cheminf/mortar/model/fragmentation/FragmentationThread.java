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

import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.IMoleculeFragmenter;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Deprecated
 * Thread class to run the fragmentation in a new parallel thread. This thread itself distributes the molecules onto
 * multiple parallel tasks to speed up the fragmentation.
 *
 * @author Felix Baensch, Jonas Schaub
 * @version 1.0.0.0
 */
public class FragmentationThread implements Callable<Hashtable<String, FragmentDataModel>> {

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(FragmentationThread.class.getName());

    private List<MoleculeDataModel> molecules;
    private int numberOfTasks;
    private String fragmentationName;
    private IMoleculeFragmenter fragmenter;

    /**
     * Constructor
     *
     * @param anArrayOfMolecules List of molecules to fragment
     * @param aNumberOfTasks int specifies number of tasks
     * @param aFragmentationName String
     * @param aFragmenter IMoleculeFragmenter to use
     */
    public FragmentationThread(List<MoleculeDataModel> anArrayOfMolecules, int aNumberOfTasks, String aFragmentationName, IMoleculeFragmenter aFragmenter){
        //<editor-fold desc="checks" defaultstate="collapsed">
        Objects.requireNonNull(anArrayOfMolecules, "anArrayOfMolecules must not be null");
        Objects.requireNonNull(aNumberOfTasks, "aNumberOfTasks must not be null");
        Objects.requireNonNull(aFragmentationName, "aFragmentationName must not be null");
        Objects.requireNonNull(aFragmenter, "aFragmenter must not be null");
        //</editor-fold>
        this.molecules = anArrayOfMolecules;
        this.numberOfTasks = aNumberOfTasks;
        this.fragmentationName = aFragmentationName;
        this.fragmenter = aFragmenter;
    }

    @Override
    public Hashtable<String, FragmentDataModel> call() throws Exception {
        Hashtable<String, FragmentDataModel> tmpFragmentHashtable = new Hashtable<>(this.molecules.size() * 2);
        if(this.molecules.size() < this.numberOfTasks){
            this.numberOfTasks = this.molecules.size();
        }
        int tmpMoleculesPerTask = this.molecules.size() / this.numberOfTasks;
        int tmpMoleculeModulo = this.molecules.size() % this.numberOfTasks;
        int tmpFromIndex = 0; //low endpoint (inclusive) of the subList
        int tmpToIndex = tmpMoleculesPerTask; //high endpoint (exclusive) of the subList
        if(tmpMoleculeModulo > 0){
            tmpToIndex++;
            tmpMoleculeModulo--;
        }
        ExecutorService tmpExecutor = Executors.newFixedThreadPool(this.numberOfTasks);
        List<FragmentationTask> tmpFragmentationTaskList = new LinkedList<>();
        for(int i = 1; i <= this.numberOfTasks; i++){
            List<MoleculeDataModel> tmpMoleculesForTask = this.molecules.subList(tmpFromIndex, tmpToIndex);
            IMoleculeFragmenter tmpFragmenterForTask = this.fragmenter.copy();
            tmpFragmentationTaskList.add (new FragmentationTask(tmpMoleculesForTask, tmpFragmenterForTask, tmpFragmentHashtable, this.fragmentationName));
            tmpFromIndex = tmpToIndex;
            tmpToIndex = tmpFromIndex + tmpMoleculesPerTask;
            if(tmpMoleculeModulo > 0){
                tmpToIndex++;
                tmpMoleculeModulo--;
            }
            if(i == this.numberOfTasks - 1 ){
                tmpToIndex = this.molecules.size();
            }
        }
        List<Future<Integer>> tmpFuturesList;
        long tmpMemoryConsumption = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024*1024);
        FragmentationThread.LOGGER.info("Fragmentation \"" + this.fragmentationName
                + "\" starting. Current memory consumption: " + tmpMemoryConsumption + " MB");
        long tmpStartTime = System.currentTimeMillis();
        try {
            tmpFuturesList = tmpExecutor.invokeAll(tmpFragmentationTaskList);
        }catch (Exception anException){
            FragmentationThread.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            throw anException;
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
            tmpFragmentHashtable.get(tmpKey).setMoleculePercentage(1.0 * tmpFragmentHashtable.get(tmpKey).getMoleculeFrequency() / this.molecules.size());
        }
        if(tmpExceptionsCounter > 0){
            FragmentationThread.LOGGER.log(Level.SEVERE, "Fragmentation \"" + this.fragmentationName + "\" caused " + tmpExceptionsCounter + " exceptions");
        }
        tmpExecutor.shutdown();
        tmpMemoryConsumption = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024*1024);
        long tmpEndTime = System.currentTimeMillis();
        FragmentationThread.LOGGER.info("Fragmentation \"" + this.fragmentationName + "\" of " + this.molecules.size()
                + " molecules complete. It took " + (tmpEndTime - tmpStartTime) + " ms. Current memory consumption: "
                + tmpMemoryConsumption + " MB");
        return tmpFragmentHashtable;
    }
}
