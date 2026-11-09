/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2026  Felix Baensch, Jonas Schaub (felix.j.baensch@gmail.com, jonas.schaub@uni-jena.de)
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Callable class to fragment a list of molecules.
 *
 * @author Felix Baensch
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class FragmentationTask implements Callable<FragmentationTaskResult> {
    //<editor-fold desc="private static final class constants" defaultstate="collapsed">
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
     * List of molecules to fragment.
     */
    private final List<MoleculeDataModel> moleculesList;
    /**
     * Fragmenter instance to use.
     */
    private final IMoleculeFragmenter fragmenter;
    /**
     * Map to hold fragments, should be synchronised, e.g. by using HashTable. Keys are unique SMILES codes.
     */
    private final Map<String, FragmentDataModel> fragmentsHashTable;
    /**
     * Name of fragmentation.
     */
    private final String fragmentationName;
    /**
     * Whether stereochemistry in the fragments should be regarded when creating their SMILES codes.
     */
    private final boolean isStereochemistryRegarded;
    //</editor-fold>
    //
    /**
     * Instantiates the thread.
     *
     * @param aListOfMolecules atom containers should meet the employed fragmentation algorithm's input specifications but
     *                         any occurring exception will be caught
     * @param aFragmenter Fragmenter to use
     * @param aHashtableOfFragments Map to hold fragments, should be synchronised, e.g. by using a HashTable instance;
     *                              keys are unique SMILES codes.
     * @param aFragmentationName String
     * @param isStereo Whether stereochemistry in the fragments should be regarded when creating their SMILES codes
     */
    public FragmentationTask(List<MoleculeDataModel> aListOfMolecules,
                             IMoleculeFragmenter aFragmenter,
                             Map<String, FragmentDataModel> aHashtableOfFragments,
                             String aFragmentationName,
                             boolean isStereo) {
        this.moleculesList = aListOfMolecules;
        this.fragmenter = aFragmenter;
        this.fragmentsHashTable = aHashtableOfFragments;
        this.fragmentationName = aFragmentationName;
        this.isStereochemistryRegarded = isStereo;
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
    public FragmentationTaskResult call() throws Exception {
        int tmpExceptionsCounter = 0;
        int tmpMoleculeProducedNoFragmentsCounter = 0;
        int tmpMoleculeProducedFragmentsCounter = 0;
        int tmpMoleculeFailedGetAtomContainerCounter = 0;
        int tmpFilteredMoleculesCounter = 0;
        int tmpFragmentFailedSmilesGenerationCounter = 0;
        int tmpUnexpectedExceptionsCounter = 0;
        for (MoleculeDataModel tmpMolecule : this.moleculesList) {
            // checked at the start of every iteration, so that the various "continue" statements below cannot skip it
            if (Thread.currentThread().isInterrupted()) {
                FragmentationTask.LOGGER.log(Level.INFO, "Thread interrupted");
                return null;
            }
            try {
                IAtomContainer tmpAtomContainer;
                try {
                    tmpAtomContainer = tmpMolecule.getAtomContainer();
                }
                catch(CDKException anException) {
                    tmpMoleculeFailedGetAtomContainerCounter++;
                    Logger.getLogger(MoleculeDataModel.class.getName()).log(
                            Level.SEVERE, String.format("%s Molecule name: %s", anException.toString(), tmpMolecule.getName()), anException);
                    continue;
                }
                //returns true if the molecule cannot be fragmented, so it gets empty lists and maps for this fragmentation
                if (this.fragmenter.shouldBeFiltered(tmpAtomContainer)) {
                    tmpMolecule.getAllFragments().put(this.fragmentationName, new ArrayList<>(0));
                    tmpMolecule.getFragmentFrequencies().put(this.fragmentationName, new HashMap<>(0));
                    tmpFilteredMoleculesCounter++;
                    continue;
                }
                if (this.fragmenter.shouldBePreprocessed(tmpAtomContainer)) {
                    tmpAtomContainer = this.fragmenter.applyPreprocessing(tmpAtomContainer);
                }
                List<IAtomContainer> tmpFragmentsList;
                try {
                    tmpFragmentsList = this.fragmenter.fragmentMolecule(tmpAtomContainer);
                }
                catch (NullPointerException | IllegalArgumentException | CloneNotSupportedException anException) {
                    FragmentationTask.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                    tmpExceptionsCounter++;
                    tmpMolecule.getAllFragments().put(this.fragmentationName, new ArrayList<>(0));
                    tmpMolecule.getFragmentFrequencies().put(this.fragmentationName, new HashMap<>(0));
                    continue;
                }
                if (tmpFragmentsList.isEmpty()) {
                    tmpMolecule.getAllFragments().put(this.fragmentationName, new ArrayList<>(0));
                    tmpMolecule.getFragmentFrequencies().put(this.fragmentationName, new HashMap<>(0));
                    tmpMoleculeProducedNoFragmentsCounter++;
                    continue;
                }
                int tmpInitialCapacity = CollectionUtil.calculateInitialHashCollectionCapacity(tmpFragmentsList.size());
                // phase 1: everything that can fail (SMILES generation, data model creation) only touches local
                // collections, so that a molecule whose processing throws leaves no traces in the fragments shared by
                // all molecules (frequencies, parent molecules) that would disagree with its own empty result
                // candidate data model per distinct fragment SMILES of this molecule, in order of first occurrence
                LinkedHashMap<String, FragmentDataModel> tmpNewFragmentsOfMolMap = new LinkedHashMap<>(tmpInitialCapacity);
                // map of the frequency with which this molecule contains a specific fragment
                HashMap<String, Integer> tmpFragmentFrequenciesOfMoleculeMap = new HashMap<>(tmpInitialCapacity);
                for (IAtomContainer tmpFragment : tmpFragmentsList) {
                    String tmpSmiles = ChemUtil.createUniqueSmiles(tmpFragment, this.isStereochemistryRegarded);
                    if (tmpSmiles == null) {
                        tmpFragmentFailedSmilesGenerationCounter++;
                        continue;
                    }
                    if (!tmpNewFragmentsOfMolMap.containsKey(tmpSmiles)) {
                        tmpNewFragmentsOfMolMap.put(tmpSmiles, new FragmentDataModel(tmpSmiles, tmpFragment.getTitle(), tmpFragment.getProperties()));
                    }
                    tmpFragmentFrequenciesOfMoleculeMap.merge(tmpSmiles, 1, Integer::sum);
                }
                // phase 2: register the fragments in the shared map and update their counts; these operations do not
                // fail in practice, so the shared state stays consistent with this molecule's results
                List<FragmentDataModel> tmpFragmentsOfMolList = new ArrayList<>(tmpNewFragmentsOfMolMap.size());
                for (Map.Entry<String, FragmentDataModel> tmpEntry : tmpNewFragmentsOfMolMap.entrySet()) {
                    String tmpSmiles = tmpEntry.getKey();
                    // putIfAbsent returns null if key is not present in the map, else previous value associated with
                    // this key; operation is atomic because we are currently using a ConcurrentHashMap
                    FragmentDataModel tmpFragmentDataModel = this.fragmentsHashTable.putIfAbsent(tmpSmiles, tmpEntry.getValue());
                    if (tmpFragmentDataModel == null) {
                        tmpFragmentDataModel = tmpEntry.getValue();
                    }
                    // increment the absolute frequency of this fragment once per occurrence - operation is atomic!
                    int tmpFrequencyInMolecule = tmpFragmentFrequenciesOfMoleculeMap.get(tmpSmiles);
                    for (int i = 0; i < tmpFrequencyInMolecule; i++) {
                        tmpFragmentDataModel.incrementAbsoluteFrequency();
                    }
                    // increment molecule frequency of this fragment - operation is atomic!
                    tmpFragmentDataModel.incrementMoleculeFrequency();
                    // add the initial molecule as a parent molecule
                    tmpFragmentDataModel.getParentMolecules().add(tmpMolecule);
                    tmpFragmentsOfMolList.add(tmpFragmentDataModel);
                }
                tmpMolecule.getFragmentFrequencies().put(this.fragmentationName, tmpFragmentFrequenciesOfMoleculeMap);
                tmpMolecule.getAllFragments().put(this.fragmentationName, tmpFragmentsOfMolList);
                // counted only after the results have been stored, so that a molecule whose result processing throws
                // is counted as an unexpected exception only and not additionally as having produced fragments
                tmpMoleculeProducedFragmentsCounter++;
            } catch (Exception anException) {
                tmpUnexpectedExceptionsCounter++;
                FragmentationTask.LOGGER.log(Level.SEVERE, String.format("Exception while fragmenting molecule with name: %s", tmpMolecule.getName()), anException);
                if (tmpMolecule.getAllFragments() != null && !tmpMolecule.getAllFragments().containsKey(this.fragmentationName)) {
                    tmpMolecule.getAllFragments().put(this.fragmentationName, new ArrayList<>(0));
                }
                if (tmpMolecule.getFragmentFrequencies() != null && !tmpMolecule.getFragmentFrequencies().containsKey(this.fragmentationName)) {
                    tmpMolecule.getFragmentFrequencies().put(this.fragmentationName, new HashMap<>(0));
                }
            }
        }
        return new FragmentationTaskResult(
                tmpExceptionsCounter,
                tmpMoleculeProducedNoFragmentsCounter,
                tmpMoleculeProducedFragmentsCounter,
                tmpMoleculeFailedGetAtomContainerCounter,
                tmpFilteredMoleculesCounter,
                tmpFragmentFailedSmilesGenerationCounter,
                tmpUnexpectedExceptionsCounter
        );
    }
}
