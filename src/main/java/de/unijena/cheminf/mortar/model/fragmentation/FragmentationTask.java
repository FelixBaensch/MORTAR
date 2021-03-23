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
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Callable;

public class FragmentationTask implements Callable<Integer> {

    private final MoleculeDataModel[] moleculesArray;

    private final IMoleculeFragmenter fragmenter;

    private final Hashtable<String, FragmentDataModel> fragments;

    /**
     * Instantiates the thread.
     *
     * @param aListOfMolecules atom containers should meet the ErtlFunctionalGroupsFinder's input specifications but
     * any occurring exception will be caught
     */
    public FragmentationTask(MoleculeDataModel[] aListOfMolecules, IMoleculeFragmenter aFragmenter, Hashtable<String, FragmentDataModel> aHashtableOfFragments) {
        this.moleculesArray = aListOfMolecules;
        this.fragmenter = aFragmenter;
        this.fragments = aHashtableOfFragments;
    }


    /**
     * Applies the ErtlFunctionalGroupsFinder.find(IAtomContainer container, boolean clone) method on all given
     * molecules (parameter clone = false) and counts the occurring exceptions.
     *
     * @return the number of occurred exceptions
     * @throws Exception if unable to compute a result (copied from doc in Callable interface)
     */
    @Override
    public Integer call() throws Exception {
        int tmpExceptionsCounter = 0;
        for (MoleculeDataModel tmpMolecule : this.moleculesArray) {
            try {
                IAtomContainer tmpAtomContainer = tmpMolecule.getAtomContainer();
                if(this.fragmenter.shouldBeFiltered(tmpAtomContainer)){
                    tmpMolecule.getHasFragmentsMap().put(this.fragmenter.getFragmentationAlgorithmName(), false);
                    continue;
                }
                if(this.fragmenter.shouldBePreprocessed(tmpAtomContainer)){
                    tmpAtomContainer = this.fragmenter.applyPreprocessing(tmpAtomContainer);
                }
                List<IAtomContainer> tmpFragmentList = this.fragmenter.fragmentMolecule(tmpAtomContainer);

                if(!this.fragmenter.hasFragments(tmpFragmentList)){
                    tmpMolecule.getHasFragmentsMap().put(this.fragmenter.getFragmentationAlgorithmName(), false);
                    continue;
                }



                for(IAtomContainer tmpFragment : tmpFragmentList){

                    SmilesGenerator tmpSmilesGen = new SmilesGenerator(SmiFlavor.Unique);  //TODO: Stereochemie?
                    String tmpSmiles = tmpSmilesGen.create(tmpFragment);

                    if(this.fragments.containsKey(tmpSmiles)){
                        FragmentDataModel tmpFragmentDataModel = this.fragments.get(tmpSmiles);
                        tmpFragmentDataModel.setAbsoluteFrequency(tmpFragmentDataModel.getAbsoluteFrequency() + 1);
//                        if(tmpMolecule.getFragmentsOfSpecificFragmentation()){
//                            tmpFragmentDataModel.setMoleculeFrequency(tmpFragmentDataModel.getMoleculeFrequency() + 1);
//                        }

                    } else {
                        FragmentDataModel tmpFragmentDataModel = new FragmentDataModel(tmpSmiles, tmpFragment);
                        if(tmpMolecule.getFragmentsOfSpecificFragmentation(this.fragmenter.getFragmentationAlgorithmName()) == null){

                        }
                    }




                }


            } catch (Exception anException) {
                tmpExceptionsCounter++;
            }
        }
        return tmpExceptionsCounter;
    }
}
