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

import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FragmentationTask implements Callable<Integer> {

    private final List<MoleculeDataModel> moleculesList;

    private final IMoleculeFragmenter fragmenter;
    private static final ReentrantLock LOCK = new ReentrantLock(true);
    private final Hashtable<String, FragmentDataModel> fragmentsHashTable;
    private final String fragmentationName;
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(Message.class.getName());

    /**
     * Instantiates the thread.
     *
     * @param aListOfMolecules atom containers should meet the ErtlFunctionalGroupsFinder's input specifications but
     * any occurring exception will be caught
     */
    public FragmentationTask(List<MoleculeDataModel> aListOfMolecules, IMoleculeFragmenter aFragmenter, Hashtable<String, FragmentDataModel> aHashtableOfFragments, String aFragmentationName) {
        this.moleculesList = aListOfMolecules;
        this.fragmenter = aFragmenter;
        this.fragmentsHashTable = aHashtableOfFragments;
        this.fragmentationName = aFragmentationName;
    }


    /**
     * Applies the IMoleculeFragmenter.fragment(IAtomContainer container) method on all given
     * molecules and counts the occurring exceptions.
     *
     * @return the number of occurred exceptions
     * @throws Exception if unable to compute a result (copied from doc in Callable interface)
     */
    @Override
    public Integer call() throws Exception{
        int tmpExceptionsCounter = 0;
        SmilesGenerator tmpSmilesGenerator = new SmilesGenerator(SmiFlavor.Unique | SmiFlavor.UseAromaticSymbols);
        for (MoleculeDataModel tmpMolecule : this.moleculesList) {
            try{
                IAtomContainer tmpAtomContainer = tmpMolecule.getAtomContainer();
                HashMap<String, List<FragmentDataModel>> tmpFragmentsMapOfMolecule = tmpMolecule.getAllFragments();
                HashMap<String, HashMap<String, Integer>> tmpFragmentFrequenciesMapOfMolecule = tmpMolecule.getFragmentFrequencies();
                if(this.fragmenter.shouldBeFiltered(tmpAtomContainer)){
                    tmpFragmentsMapOfMolecule.put(this.fragmentationName, new ArrayList<>(0));
                    tmpFragmentFrequenciesMapOfMolecule.put(this.fragmentationName, new HashMap<>(0));
                    continue;
                }
                if(this.fragmenter.shouldBePreprocessed(tmpAtomContainer)){
                    tmpAtomContainer = this.fragmenter.applyPreprocessing(tmpAtomContainer);
                }
                List<IAtomContainer> tmpFragmentsList = this.fragmenter.fragmentMolecule(tmpAtomContainer);
                tmpFragmentsMapOfMolecule.put(this.fragmentationName, new ArrayList<>(tmpFragmentsList.size()));
                tmpFragmentFrequenciesMapOfMolecule.put(this.fragmentationName, new HashMap<>(tmpFragmentsList.size()));
                for(IAtomContainer tmpFragment : tmpFragmentsList){
                    String tmpSmiles = tmpSmilesGenerator.create(tmpFragment);
                    FragmentDataModel tmpFragmentDataModel;
                    LOCK.lock(); //TODO: ask Achim about hashtable concurrency
                    try{
                        if(this.fragmentsHashTable.containsKey(tmpSmiles)){
                            tmpFragmentDataModel = this.fragmentsHashTable.get(tmpSmiles);
                            tmpFragmentDataModel.incrementAbsoluteFrequency();
                        }
                        else{
                            tmpFragmentDataModel = new FragmentDataModel(tmpSmiles, tmpFragment);
                            this.fragmentsHashTable.put(tmpSmiles, tmpFragmentDataModel);
                        }
                        if(tmpFragmentsMapOfMolecule.get(this.fragmentationName).contains(tmpFragmentDataModel)){
                            tmpFragmentFrequenciesMapOfMolecule.get(this.fragmentationName).replace(tmpSmiles, tmpFragmentFrequenciesMapOfMolecule.get(this.fragmentationName).get(tmpSmiles) + 1);
                        }
                        else{
                            tmpFragmentDataModel.incrementMoleculeFrequency();
                            tmpFragmentFrequenciesMapOfMolecule.get(this.fragmentationName).put(tmpSmiles, 1);
                            tmpFragmentsMapOfMolecule.get(this.fragmentationName).add(tmpFragmentDataModel);
                        }
                    } catch (Exception anException){
                        FragmentationTask.LOGGER.log(Level.SEVERE, anException.toString());
                        tmpExceptionsCounter++;
                    }finally {
                        LOCK.unlock();
                    }
                }
            } catch(Exception anException){
                tmpExceptionsCounter++;
                FragmentationTask.LOGGER.log(Level.SEVERE, anException.toString());
            }
        }
        return tmpExceptionsCounter;
    }
}
