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

import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for CollectionUtil routines.
 */
public class CollectionUtilTest {
    /**
     * Tests whether a few examples of number of elements and load factor pairs generate suitable initial capacities that
     * lead to rehash thresholds higher than the number of elements using the specific calculation method from CollectionUtil.
     */
    @Test
    public void calculateInitialHashMapSizeTest() {
        int[] tmpNumberOfElements = new int[]{10, 100, 4353456, 30};
        float[] tmpLoadFactor = new float[]{0.75f, 0.75f, 0.6f, 0.75f};
        int[] tmpExpectedInitialCapacity = new int[]{15, 135, 7255762, 42};
        for (int i = 0; i < tmpNumberOfElements.length; i++) {
            int tmpCalculatedInitialHashMapCapacity = CollectionUtil.calculateInitialHashMapCapacity(tmpNumberOfElements[i], tmpLoadFactor[i]);
            float tmpRehashThreshold = tmpCalculatedInitialHashMapCapacity * tmpLoadFactor[i];
            System.out.println("\nNumber of elements: " + tmpNumberOfElements[i]);
            System.out.println("Load factor: " + tmpLoadFactor[i]);
            System.out.println("Expected initial capacity ((int) n_elements * (1/lf) + 2): " + tmpExpectedInitialCapacity[i]);
            System.out.println("Calculated initial capacity: " + tmpCalculatedInitialHashMapCapacity);
            System.out.println("Rehash threshold (lf * initCap, must be higher than number of elements): " + tmpRehashThreshold);
            Assert.assertTrue(tmpNumberOfElements[i] < tmpRehashThreshold);
            Assert.assertEquals(tmpExpectedInitialCapacity[i], tmpCalculatedInitialHashMapCapacity);
        }
    }
}