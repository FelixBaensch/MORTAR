/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2022  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.IMoleculeFragmenter;
import de.unijena.cheminf.mortar.model.util.ChemUtil;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final Map<String, FragmentDataModel> fragmentsMap;
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
     * @param aMapOfFragments Map to store fragments, should be a ConcurrentHashMap in case of multiple threads to ensure thread safety
     * @param aFragmentationName String
     */
    public FragmentationTask(List<MoleculeDataModel> aListOfMolecules, IMoleculeFragmenter aFragmenter, Map<String, FragmentDataModel> aMapOfFragments, String aFragmentationName) {
        this.moleculesList = aListOfMolecules;
        this.fragmenter = aFragmenter;
        this.fragmentsMap = aMapOfFragments;
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
            HashMap<String, List<FragmentDataModel>> tmpFragmentsMapOfMolecule = null;
            HashMap<String, HashMap<String, Integer>> tmpFragmentFrequenciesMapOfMolecule = null;
            try{
                IAtomContainer tmpAtomContainer;
                try{
                    tmpAtomContainer = tmpMolecule.getAtomContainer();
                } catch(CDKException anException){
                    this.exceptionsCounter++;
                    FragmentationTask.LOGGER.getLogger(MoleculeDataModel.class.getName()).log(Level.SEVERE, anException.toString() + "_" + tmpMolecule.getName(), anException);
                    continue;
                }
                tmpFragmentsMapOfMolecule = tmpMolecule.getAllFragments();
                tmpFragmentFrequenciesMapOfMolecule = tmpMolecule.getFragmentFrequencies();
                if(this.fragmenter.shouldBeFiltered(tmpAtomContainer)){
                    tmpFragmentsMapOfMolecule.put(this.fragmentationName, new ArrayList<>(0));
                    tmpFragmentFrequenciesMapOfMolecule.put(this.fragmentationName, new HashMap<>(0));
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
                    tmpFragmentsMapOfMolecule.put(this.fragmentationName, new ArrayList<>(0));
                    tmpFragmentFrequenciesMapOfMolecule.put(this.fragmentationName, new HashMap<>(0));
                    continue;
                }
                tmpFragmentsMapOfMolecule.put(this.fragmentationName, new ArrayList<>(tmpFragmentsList.size()));
                tmpFragmentFrequenciesMapOfMolecule.put(this.fragmentationName, new HashMap<>(tmpFragmentsList.size()));
                for(IAtomContainer tmpFragment : tmpFragmentsList){
                    String tmpSmiles = ChemUtil.createUniqueSmiles(tmpFragment);
                    if (tmpSmiles == null) {
                        this.exceptionsCounter++;
                        continue;
                    }
                    FragmentDataModel tmpFragmentDataModel;
                    try{
                        if(this.fragmentsMap.containsKey(tmpSmiles)){
                            tmpFragmentDataModel = this.fragmentsMap.get(tmpSmiles);
                        }
                        else{
                            tmpFragmentDataModel = new FragmentDataModel(tmpSmiles, tmpFragment.getTitle(), tmpFragment.getProperties());
//                            tmpFragmentDataModel = new FragmentDataModel(tmpFragment);
                            this.fragmentsMap.put(tmpSmiles, tmpFragmentDataModel);
                        }
                        LOCK.lock();
                        tmpFragmentDataModel.incrementAbsoluteFrequency();
                        LOCK.unlock();
                        if(!tmpFragmentDataModel.getParentMolecules().contains(tmpMolecule)){
                            tmpFragmentDataModel.getParentMolecules().add(tmpMolecule);
                        }
                        if(tmpFragmentsMapOfMolecule.get(this.fragmentationName).contains(tmpFragmentDataModel)){
                            tmpFragmentFrequenciesMapOfMolecule.get(this.fragmentationName).replace(tmpSmiles, tmpFragmentFrequenciesMapOfMolecule.get(this.fragmentationName).get(tmpSmiles) + 1);
                        }
                        else{
                            LOCK.lock();
                            tmpFragmentDataModel.incrementMoleculeFrequency();
                            LOCK.unlock();
                            tmpFragmentFrequenciesMapOfMolecule.get(this.fragmentationName).put(tmpSmiles, 1);
                            tmpFragmentsMapOfMolecule.get(this.fragmentationName).add(tmpFragmentDataModel);
                        }
                    } catch (Exception anException) {
                        FragmentationTask.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                        this.exceptionsCounter++;
                    }
                }
            } catch(Exception anException){
                this.exceptionsCounter++;
                FragmentationTask.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                if (tmpFragmentsMapOfMolecule != null && !tmpFragmentsMapOfMolecule.containsKey(this.fragmentationName)) {
                    tmpFragmentsMapOfMolecule.put(this.fragmentationName, new ArrayList<>(0));
                }
                if (tmpFragmentFrequenciesMapOfMolecule != null && !tmpFragmentFrequenciesMapOfMolecule.containsKey(this.fragmentationName)) {
                    tmpFragmentFrequenciesMapOfMolecule.put(this.fragmentationName, new HashMap<>(0));
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
