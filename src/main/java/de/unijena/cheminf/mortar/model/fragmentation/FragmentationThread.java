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

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class FragmentationThread implements Callable<Hashtable<String, FragmentDataModel>> {

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(Message.class.getName());

    private MoleculeDataModel[] molecules;
    private int numberOfThreads;
    private String fragmentationName;
    private IMoleculeFragmenter fragmenter;

    public FragmentationThread(MoleculeDataModel[] anArrayOfMolecules, int aNumberOfThreads, String aFragmentationName, IMoleculeFragmenter aFragmenter){
        //<editor-fold desc="checks" defaultstate="collapsed">
        Objects.requireNonNull(anArrayOfMolecules, "anArrayOfMolecules must not be null");
        Objects.requireNonNull(aNumberOfThreads, "aNumberOfThreads must not be null");
        Objects.requireNonNull(aFragmentationName, "aFragmentationName must not be null");
        Objects.requireNonNull(aFragmenter, "aFragmenter must not be null");
        //</editor-fold>
        this.molecules = anArrayOfMolecules;
        this.numberOfThreads = aNumberOfThreads;
        this.fragmentationName = aFragmentationName;
        this.fragmenter = aFragmenter;
    }

    @Override
    public Hashtable<String, FragmentDataModel> call() throws Exception {
        Hashtable<String, FragmentDataModel> tmpFragmentHashtable = new Hashtable<>(this.molecules.length * 2);
        int tmpMoleculesPerTask = this.molecules.length / this.numberOfThreads;
        int tmpStartIndex = 0;
        int tmpEndIndex = tmpMoleculesPerTask - 1;
        ExecutorService tmpExecutor = Executors.newFixedThreadPool(this.numberOfThreads);
        for(int i = 0; i < this.numberOfThreads; i++){
            MoleculeDataModel[] tmpMoleculesForTask = Arrays.asList(this.molecules).subList(tmpStartIndex, tmpEndIndex).toArray(MoleculeDataModel[]::new);
        }

        return null;
    }
}