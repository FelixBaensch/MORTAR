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

import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

/**
 * Service class for fragmentation
 *
 * @author Jonas Schaub, Felix Baensch
 */
public class FragmentationService {

    private IMoleculeFragmenter[] fragmenters;
    private IMoleculeFragmenter selectedFragmenter;
    private IMoleculeFragmenter ertlFGF;
    private IMoleculeFragmenter sugarRUF;
    private List<String> existingFragmentations;
    private Hashtable<String, FragmentDataModel> fragments;
    private String currentFragmentationName;

    public FragmentationService(){
        this.fragmenters = new IMoleculeFragmenter[2];
        this.ertlFGF = new ErtlFunctionalGroupsFinderFragmenter();
        this.fragmenters[0] = this.ertlFGF;
        this.sugarRUF = new SugarRemovalUtilityFragmenter();
        this.fragmenters[1] = this.sugarRUF;
        this.existingFragmentations = new LinkedList<String>();
    }


    public FragmentationThread startFragmentationThread(List<MoleculeDataModel> aListOfMolecules, int aNumberOfTasks) throws Exception{
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
        FragmentationThread tmpFragmentationThread =
                new FragmentationThread(
                        aListOfMolecules,
                        aNumberOfTasks,
                        tmpFragmentationName,
                        this.selectedFragmenter
                        );
        this.fragments = tmpFragmentationThread.call();
        return tmpFragmentationThread;
    }

    public IMoleculeFragmenter[] getFragmenters(){
        return this.fragmenters;
    }

    public IMoleculeFragmenter getSelectedFragmenter(){
        return this.selectedFragmenter;
    }

    public Hashtable<String, FragmentDataModel> getFragments(){
        return this.fragments;
    }

    public String getCurrentFragmentationName(){
        return this.currentFragmentationName;
    }

    public void setSelectedFragmenter(String anAlgorithmName){
        for (IMoleculeFragmenter tmpFragmenter : this.fragmenters) {
            if (anAlgorithmName.equals(tmpFragmenter.getFragmentationAlgorithmName()))
                this.selectedFragmenter = tmpFragmenter;
        }
    }
}
