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

import java.util.Hashtable;

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

    public FragmentationService(){
        this.fragmenters = new IMoleculeFragmenter[2];
        this.ertlFGF = new ErtlFunctionalGroupsFinderFragmenter();
        this.fragmenters[0] = this.ertlFGF;
        this.sugarRUF = new SugarRemovalUtilityFragmenter();
        this.fragmenters[1] = this.sugarRUF;
        this.selectedFragmenter = this.fragmenters[0];
    }


    public void startFragmentationThread(MoleculeDataModel[] molecules){

        Hashtable<String, FragmentDataModel> tmpFragmentHashTable = new Hashtable<>();



    }

    public IMoleculeFragmenter[] getFragmenters(){
        return this.fragmenters;
    }

    public void setSelectedFragmenter(String anAlgorithmName){
        for(int i = 0; i < this.fragmenters.length; i++){
            if(anAlgorithmName.equals(this.fragmenters[i].getFragmentationAlgorithmName()))
                this.selectedFragmenter = this.fragmenters[i];
        }
    }
}
