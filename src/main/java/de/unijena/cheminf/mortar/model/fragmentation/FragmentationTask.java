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
import de.unijena.cheminf.mortar.model.util.ChemUtil;
import de.unijena.cheminf.mortar.model.util.CollectionUtil;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Callable class to fragment a list of molecules
 *
 * @author Felix Baensch, Jonas Schaub
 * @version 1.0.0.0
 */
public class FragmentationTask implements Callable<Integer> {

    //<editor-fold desc="private static final class variables" defaultstate="collapsed">
    /**
     * Lock to be used when updating the shared fragmentsHashTable. Needs to be static to be shared between all task
     * objects.
     */
    private static final ReentrantLock LOCK = new ReentrantLock(true);
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(FragmentationTask.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="private class variables" defaultstate="collapsed>
    /**
     * List of molecules to fragment
     */
    private final List<MoleculeDataModel> moleculesList;
    /**
     * Fragmenter instance to use
     */
    private final IMoleculeFragmenter fragmenter;
    /**
     * HashTable to hold fragments
     */
    private final Hashtable<String, FragmentDataModel> fragmentsHashTable;
    /**
     * Name of fragmentation
     */
    private final String fragmentationName;
    /**
     * Integer to count possible exceptions which could occur during fragmentation
     */
    private int exceptionsCounter;
    //</editor-fold>
    //
    /**
     * Instantiates the thread.
     *
     * @param aListOfMolecules atom containers should meet the ErtlFunctionalGroupsFinder's input specifications but
     *                         any occurring exception will be caught
     * @param aFragmenter Fragmenter to use
     * @param aHashtableOfFragments HashTable to store fragments
     * @param aFragmentationName String
     */
    public FragmentationTask(List<MoleculeDataModel> aListOfMolecules, IMoleculeFragmenter aFragmenter, Hashtable<String, FragmentDataModel> aHashtableOfFragments, String aFragmentationName) {
        this.moleculesList = aListOfMolecules;
        this.fragmenter = aFragmenter;
        this.fragmentsHashTable = aHashtableOfFragments;
        this.fragmentationName = aFragmentationName;
        this.exceptionsCounter = 0;
    }
    //
    /**
     * Applies the IMoleculeFragmenter.fragment(IAtomContainer container) method on all given
     * molecules and counts the occurring exceptions.
     *
     * @return the number of occurred exceptions
     * @throws Exception if unable to compute a result (copied from doc in Callable interface)
     */
    @Override
    public Integer call() throws Exception{
        for (MoleculeDataModel tmpMolecule : this.moleculesList) {
            try{
                IAtomContainer tmpAtomContainer;
                try{
                    tmpAtomContainer = tmpMolecule.getAtomContainer();
                } catch(CDKException anException){
                    this.exceptionsCounter++;
                    FragmentationTask.LOGGER.getLogger(MoleculeDataModel.class.getName()).log(Level.SEVERE, anException.toString() + "_" + tmpMolecule.getName(), anException);
                    continue;
                }
                if(this.fragmenter.shouldBeFiltered(tmpAtomContainer)){ //returns true if the molecule can not be fragmented, so it gets empty lists and maps for this fragmentation
                    tmpMolecule.getAllFragments().put(this.fragmentationName, new ArrayList<>(0));
                    tmpMolecule.getFragmentFrequencies().put(this.fragmentationName, new HashMap<>(0));
                    continue;
                }
                if(this.fragmenter.shouldBePreprocessed(tmpAtomContainer)){
                    tmpAtomContainer = this.fragmenter.applyPreprocessing(tmpAtomContainer);
                }
                List<IAtomContainer> tmpFragmentsList = null;
                try {
                    tmpFragmentsList = this.fragmenter.fragmentMolecule(tmpAtomContainer);
                } catch (NullPointerException | IllegalArgumentException | CloneNotSupportedException anException) {
                    FragmentationTask.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                    this.exceptionsCounter++;
                    tmpMolecule.getAllFragments().put(this.fragmentationName, new ArrayList<>(0));
                    tmpMolecule.getFragmentFrequencies().put(this.fragmentationName, new HashMap<>(0));
                    continue;
                }
                List<FragmentDataModel> tmpFragmentsOfMolList = new ArrayList<>(tmpFragmentsList.size());
                HashMap<String, Integer> tmpFragmentFrequenciesOfMoleculeMap = new HashMap<>(CollectionUtil.calculateInitialHashCollectionCapacity(tmpFragmentsList.size()));
                for(IAtomContainer tmpFragment : tmpFragmentsList){
                    String tmpSmiles = ChemUtil.createUniqueSmiles(tmpFragment);
                    if (tmpSmiles == null) {
                        this.exceptionsCounter++;
                        continue;
                    }
                    FragmentDataModel tmpNewFragmentDataModel =  new FragmentDataModel(tmpSmiles, tmpFragment.getTitle(), tmpFragment.getProperties());
                    FragmentDataModel tmpFragmentDataModel = this.fragmentsHashTable.putIfAbsent(tmpSmiles,  tmpNewFragmentDataModel); // putIfAbsent returns null if key is not present in the map, else previous value associated with this key
                    if(tmpFragmentDataModel == null){
                        tmpFragmentDataModel = tmpNewFragmentDataModel;
                    }
                    LOCK.lock();
                    tmpFragmentDataModel.incrementAbsoluteFrequency();
                    LOCK.unlock();
                    tmpFragmentDataModel.getParentMolecules().add(tmpMolecule);
                    if(tmpFragmentsOfMolList.contains(tmpFragmentDataModel)){
                        tmpFragmentFrequenciesOfMoleculeMap.replace(tmpSmiles, tmpFragmentFrequenciesOfMoleculeMap.get(tmpSmiles) + 1);
                    }
                    else{
                        LOCK.lock();
                        tmpFragmentDataModel.incrementMoleculeFrequency();
                        LOCK.unlock();
                        tmpFragmentsOfMolList.add(tmpFragmentDataModel);
                        tmpFragmentFrequenciesOfMoleculeMap.put(tmpSmiles, 1);
                    }
                }
                tmpMolecule.getFragmentFrequencies().put(this.fragmentationName, tmpFragmentFrequenciesOfMoleculeMap);
                tmpMolecule.getAllFragments().put(this.fragmentationName, tmpFragmentsOfMolList);
            } catch(Exception anException){
                this.exceptionsCounter++;
                FragmentationTask.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                if (tmpMolecule.getAllFragments() != null && !tmpMolecule.getAllFragments().containsKey(this.fragmentationName)) {
                    tmpMolecule.getAllFragments().put(this.fragmentationName, new ArrayList<>(0));
                }
                if (tmpMolecule.getFragmentFrequencies() != null && !tmpMolecule.getFragmentFrequencies().containsKey(this.fragmentationName)) {
                    tmpMolecule.getFragmentFrequencies().put(this.fragmentationName, new HashMap<>(0));
                }
            }
            if(Thread.currentThread().isInterrupted()){
                LOGGER.log(Level.INFO, "Thread interrupted");
                return null;
            }
        }
        return this.exceptionsCounter;
    }
}
