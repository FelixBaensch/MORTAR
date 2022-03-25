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

package de.unijena.cheminf.mortar.model.util;

import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;

import java.util.Collections;
import java.util.List;

/**
 * Util class for lists.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public final class ListUtil {
    //<editor-fold desc="public static methods" defaultstate="collapsed">
    /**
     * Sorts given list by property and sort type
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
    //</editor-fold>
}
