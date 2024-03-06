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

package de.unijena.cheminf.mortar.model.util;

import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import javafx.scene.control.TableColumn;

import java.util.Collections;
import java.util.List;

/**
 * Util class for collections.
 *
 * @author Felix Baensch, Jonas Schaub
 * @version 1.0.1.0
 */
public final class CollectionUtil {
    //<editor-fold desc="Protected constructor">
    /**
     * Private parameter-less constructor.
     * Introduced because javadoc build complained about classes without declared default constructor.
     */
    private CollectionUtil() {
    }
    //</editor-fold>
    //
    //<editor-fold desc="public static methods" defaultstate="collapsed">
    //TODO: check parameters, change params to enum values (and bools?), throw exceptions!
    /**
     * Sorts given list by property and sort type.
     *
     * @param aList List
     * @param aProperty String
     * @param aSortType TableColumn.SortType
     */
    public static void sortGivenFragmentListByPropertyAndSortType(List<? extends MoleculeDataModel> aList, String aProperty, TableColumn.SortType aSortType) {
        aList.sort((m1, m2) -> {
            FragmentDataModel f1;
            FragmentDataModel f2;
            switch (aProperty) {
                case "absoluteFrequency":
                    f1 = (FragmentDataModel) m1;
                    f2 = (FragmentDataModel) m2;
                    return switch (aSortType) {
                        case TableColumn.SortType.ASCENDING ->
                                (Integer.compare(f1.getAbsoluteFrequency(), f2.getAbsoluteFrequency()));
                        case TableColumn.SortType.DESCENDING ->
                                (Integer.compare(f2.getAbsoluteFrequency(), f1.getAbsoluteFrequency()));
                    };
                case "absolutePercentage":
                    f1 = (FragmentDataModel) m1;
                    f2 = (FragmentDataModel) m2;
                    return switch (aSortType) {
                        case TableColumn.SortType.ASCENDING ->
                                (Double.compare(f1.getAbsolutePercentage(), f2.getAbsolutePercentage()));
                        case TableColumn.SortType.DESCENDING ->
                                (Double.compare(f2.getAbsolutePercentage(), f1.getAbsolutePercentage()));
                    };
                case "moleculeFrequency":
                    f1 = (FragmentDataModel) m1;
                    f2 = (FragmentDataModel) m2;
                    return switch (aSortType) {
                        case TableColumn.SortType.ASCENDING ->
                                (Double.compare(f1.getMoleculeFrequency(), f2.getMoleculeFrequency()));
                        case TableColumn.SortType.DESCENDING ->
                                (Integer.compare(f2.getMoleculeFrequency(), f1.getMoleculeFrequency()));
                    };
                case "moleculePercentage":
                    f1 = (FragmentDataModel) m1;
                    f2 = (FragmentDataModel) m2;
                    return switch (aSortType) {
                        case TableColumn.SortType.ASCENDING ->
                                (Double.compare(f1.getMoleculePercentage(), f2.getMoleculePercentage()));
                        case TableColumn.SortType.DESCENDING ->
                                (Double.compare(f2.getMoleculePercentage(), f1.getMoleculePercentage()));
                    };
                case "name":
                    return switch (aSortType) {
                        case TableColumn.SortType.ASCENDING -> m1.getName().compareTo(m2.getName());
                        case TableColumn.SortType.DESCENDING -> m2.getName().compareTo(m1.getName());
                    };
                case "uniqueSmiles":
                    return switch (aSortType) {
                        case TableColumn.SortType.ASCENDING -> m1.getUniqueSmiles().compareTo(m2.getUniqueSmiles());
                        case TableColumn.SortType.DESCENDING -> m2.getUniqueSmiles().compareTo(m1.getUniqueSmiles());
                    };
                case "parentMoleculeName":
                    f1 = (FragmentDataModel) m1;
                    f2 = (FragmentDataModel) m2;
                    return switch (aSortType) {
                        case TableColumn.SortType.ASCENDING ->
                                f1.getParentMoleculeName().compareTo(f2.getParentMoleculeName());
                        case TableColumn.SortType.DESCENDING ->
                                f2.getParentMoleculeName().compareTo(f1.getParentMoleculeName());
                    };
            }
            return 0;
        });
    }
    //
    /**
     * Calculates a suitable initial size for instantiating a new HashMap or HashSet instance based on the number of elements
     * supposed to be stored in it and the load factor that determines the resize threshold.
     * Calculation: initCap = (int) aNumberOfElement * (1/aLoadFactor) + 2
     * <br>The initial capacity multiplied with the load factor (= resize threshold) must be higher than the number of
     * elements.
     *
     * @param aNumberOfElements number of elements supposed to be stored in the new HashMap or HashSet instance
     * @param aLoadFactor load factor that is specified for the new HashMap or HashSet instance
     * @return a suitable initial size for the new HashMap or HashSet instance that leads to a resize threshold that is slightly
     * higher than the number of elements
     * @throws IllegalArgumentException if the number of elements or the load factor is negative or equal to zero
     * or if the load factor is greater than 1.0
     */
    public static int calculateInitialHashCollectionCapacity(int aNumberOfElements, float aLoadFactor)
            throws IllegalArgumentException {
        if (aNumberOfElements < 0) {
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
     * Calculates a suitable initial size for instantiating a new HashMap or HashSet instance with the default load factor.
     * <br>For more details, see {@link #calculateInitialHashCollectionCapacity(int, float) calculateInitialHashCollectionCapacity(int, float)}.
     *
     *
     * @param aNumberOfElements number of elements supposed to be stored in the new HashMap or HashSet instance
     * @return a suitable initial size for the new HashMap or HashSet instance that leads to a resize threshold that is slightly
     * higher than the number of elements
     * @throws IllegalArgumentException if the number of elements is negative or equal to zero
     */
    public static int calculateInitialHashCollectionCapacity(int aNumberOfElements) throws IllegalArgumentException {
        return CollectionUtil.calculateInitialHashCollectionCapacity(aNumberOfElements, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
    }
    //</editor-fold>
}
