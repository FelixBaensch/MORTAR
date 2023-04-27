/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2023  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.model.util;

import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;

import java.util.Collections;
import java.util.List;

/**
 * Util class for collections.
 *
 * @author Felix Baensch, Jonas Schaub
 * @version 1.0.1.0
 */
public final class CollectionUtil {
    //<editor-fold desc="public static methods" defaultstate="collapsed">
    /**
     * Sorts given list by property and sort type.
     *
     * @param aList List
     * @param aProperty String
     * @param aSortType String
     */
    public static void sortGivenFragmentListByPropertyAndSortType(List<MoleculeDataModel> aList, String aProperty, String aSortType) {
        Collections.sort(aList, (m1, m2) -> {
            FragmentDataModel f1;
            FragmentDataModel f2;
            switch (aProperty) {
                case "absoluteFrequency":
                    f1 = (FragmentDataModel) m1;
                    f2 = (FragmentDataModel) m2;
                    switch (aSortType) {
                        case "ASCENDING":
                            return (Integer.compare(f1.getAbsoluteFrequency(), f2.getAbsoluteFrequency()));
                        case "DESCENDING":
                            return (f1.getAbsoluteFrequency() > f2.getAbsoluteFrequency() ? -1 : (f1.getAbsoluteFrequency() == f2.getAbsoluteFrequency() ? 0 : 1));
                    }
                case "absolutePercentage":
                    f1 = (FragmentDataModel) m1;
                    f2 = (FragmentDataModel) m2;
                    switch (aSortType) {
                        case "ASCENDING":
                            return (Double.compare(f1.getAbsolutePercentage(), f2.getAbsolutePercentage()));
                        case "DESCENDING":
                            return (f1.getAbsolutePercentage() > f2.getAbsolutePercentage() ? -1 : (f1.getAbsolutePercentage() == f2.getAbsolutePercentage() ? 0 : 1));
                    }
                case "moleculeFrequency":
                    f1 = (FragmentDataModel) m1;
                    f2 = (FragmentDataModel) m2;
                    switch (aSortType) {
                        case "ASCENDING":
                            return (Double.compare(f1.getMoleculeFrequency(), f2.getMoleculeFrequency()));
                        case "DESCENDING":
                            return (f1.getMoleculeFrequency() > f2.getMoleculeFrequency() ? -1 : (f1.getMoleculeFrequency() == f2.getMoleculeFrequency() ? 0 : 1));
                    }
                case "moleculePercentage":
                    f1 = (FragmentDataModel) m1;
                    f2 = (FragmentDataModel) m2;
                    switch (aSortType) {
                        case "ASCENDING":
                            return (Double.compare(f1.getMoleculePercentage(), f2.getMoleculePercentage()));
                        case "DESCENDING":
                            return (f1.getMoleculePercentage() > f2.getMoleculePercentage() ? -1 : (f1.getMoleculePercentage() == f2.getMoleculePercentage() ? 0 : 1));
                    }
                case "name":
                    switch (aSortType) {
                        case "ASCENDING":
                            return m1.getName().compareTo(m2.getName());
                        case "DESCENDING":
                            return m2.getName().compareTo(m1.getName());
                    }
                case "uniqueSmiles":
                    switch (aSortType) {
                        case "ASCENDING":
                            return m1.getUniqueSmiles().compareTo(m2.getUniqueSmiles());
                        case "DESCENDING":
                            return m2.getUniqueSmiles().compareTo(m1.getUniqueSmiles());
                    }
                case "parentMoleculeName":
                    f1 = (FragmentDataModel) m1;
                    f2 = (FragmentDataModel) m2;
                    switch (aSortType) {
                        case "ASCENDING":
                            return f1.getParentMoleculeName().compareTo(f2.getParentMoleculeName());
                        case "DESCENDING":
                            return f2.getParentMoleculeName().compareTo(f1.getParentMoleculeName());
                    }
            }
            return 0;
        });
    }
    //
    /**
     * Calculates a suitable initial size for instantiating a new HashMap instance based on the number of elements
     * supposed to be stored in it and the load factor that determines the rehash threshold.
     * Calculation: initCap = (int) aNumberOfElement * (1/aLoadFactor) + 2
     * <br>The initial capacity multiplied with the load factor (= rehash threshold) must be higher than the number of
     * elements.
     *
     * @param aNumberOfElements number of elements supposed to be stored in the new HashMap instance
     * @param aLoadFactor load factor that is specified for the new HashMap instance
     * @return a suitable initial size for the new HashMap instance that leads to a rehash threshold that is slightly
     * higher than the number of elements
     * @throws IllegalArgumentException if the number of elements or the load factor is negative or equal to zero
     * or if the load factor is greater than 1.0
     */
    public static int calculateInitialHashMapCapacity(int aNumberOfElements, float aLoadFactor)
            throws IllegalArgumentException {
        if (aNumberOfElements <= 0) {
            throw new IllegalArgumentException("Number of elements needs to be higher than 0 but is " + aNumberOfElements);
        }
        if (aLoadFactor <= 0 || aLoadFactor > 1.0f) {
            throw new IllegalArgumentException("Load factor must be higher than 0 and not bigger than 1.0 but is " + aLoadFactor);
        }
        float tmpInitialSize = (float) aNumberOfElements * (1.0f / aLoadFactor) + 2.0f;
        return (int) tmpInitialSize;
    }
    //
    /**
     * Calculates a suitable initial size for instantiating a new HashMap instance based on the number of elements
     * supposed to be stored in it and the default load factor (0.75) that determines the rehash threshold.
     * Calculation: initCap = (int) aNumberOfElement * (1/0.75) + 2
     * <br>The initial capacity multiplied with the load factor (= rehash threshold) must be higher than the number of
     * elements.
     *
     * @param aNumberOfElements number of elements supposed to be stored in the new HashMap instance
     * @return a suitable initial size for the new HashMap instance that leads to a rehash threshold that is slightly
     * higher than the number of elements
     * @throws IllegalArgumentException if the number of elements is negative or equal to zero
     */
    public static int calculateInitialHashMapCapacity(int aNumberOfElements) throws IllegalArgumentException {
        return CollectionUtil.calculateInitialHashMapCapacity(aNumberOfElements, 0.75f);
    }
    //</editor-fold>
}
