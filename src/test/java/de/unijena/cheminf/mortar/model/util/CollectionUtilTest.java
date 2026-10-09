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

package de.unijena.cheminf.mortar.model.util;

import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Test class for CollectionUtil routines.
 *
 * @author Felix Baensch
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class CollectionUtilTest {
    //<editor-fold desc="constructor" defaultstate="collapsed">
    /**
     * Constructor that needs nothing to set up.
     */
    public CollectionUtilTest() {
    }
    //</editor-fold>
    //
    //<editor-fold desc="calculateInitialHashCollectionCapacity test methods" defaultstate="collapsed">
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
            int tmpCalculatedInitialHashMapCapacity = CollectionUtil.calculateInitialHashCollectionCapacity(tmpNumberOfElements[i], tmpLoadFactor[i]);
            float tmpRehashThreshold = tmpCalculatedInitialHashMapCapacity * tmpLoadFactor[i];
            Assertions.assertTrue(tmpNumberOfElements[i] < tmpRehashThreshold);
            Assertions.assertEquals(tmpExpectedInitialCapacity[i], tmpCalculatedInitialHashMapCapacity);
        }
    }
    //
    /**
     * Tests whether the single-argument overload of calculateInitialHashCollectionCapacity delegates to the two-argument
     * overload using the default hash collection load factor, i.e. both produce the same result for the same number of
     * elements.
     */
    @Test
    public void calculateInitialHashCollectionCapacitySingleArgDelegatesToDefaultLoadFactorTest() {
        int[] tmpNumberOfElements = new int[]{0, 1, 30, 100, 4353456};
        for (int tmpNumberOfElement : tmpNumberOfElements) {
            int tmpSingleArgResult = CollectionUtil.calculateInitialHashCollectionCapacity(tmpNumberOfElement);
            int tmpTwoArgResult = CollectionUtil.calculateInitialHashCollectionCapacity(
                    tmpNumberOfElement, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
            Assertions.assertEquals(tmpTwoArgResult, tmpSingleArgResult);
        }
    }
    //
    /**
     * Tests whether the two-argument calculateInitialHashCollectionCapacity throws an IllegalArgumentException when the
     * number of elements is negative.
     */
    @Test
    public void calculateInitialHashCollectionCapacityNegativeCountThrowsTest() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> CollectionUtil.calculateInitialHashCollectionCapacity(-1, 0.75f));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> CollectionUtil.calculateInitialHashCollectionCapacity(-1));
    }
    //
    /**
     * Tests whether the two-argument calculateInitialHashCollectionCapacity throws an IllegalArgumentException when the
     * load factor is out of the valid range (i.e. less than or equal to zero or greater than 1.0).
     */
    @Test
    public void calculateInitialHashCollectionCapacityBadLoadFactorThrowsTest() {
        float[] tmpInvalidLoadFactors = new float[]{0.0f, -0.5f, 1.0001f, 2.0f};
        for (float tmpInvalidLoadFactor : tmpInvalidLoadFactors) {
            Assertions.assertThrows(IllegalArgumentException.class,
                    () -> CollectionUtil.calculateInitialHashCollectionCapacity(10, tmpInvalidLoadFactor));
        }
    }
    //
    /**
     * Tests whether a load factor of exactly 1.0 is accepted (it is the inclusive upper boundary of the valid range;
     * the rejection condition is {@code aLoadFactor > 1.0f}) and yields the expected capacity of
     * {@code aNumberOfElements * (1/1.0) + 2 = aNumberOfElements + 2}. This pins the {@code > 1.0f} boundary so a
     * {@code >=} mutation (which would wrongly reject 1.0) is detected.
     */
    @Test
    public void calculateInitialHashCollectionCapacityAcceptsLoadFactorOfExactlyOneTest() {
        Assertions.assertEquals(12, CollectionUtil.calculateInitialHashCollectionCapacity(10, 1.0f));
        Assertions.assertEquals(2, CollectionUtil.calculateInitialHashCollectionCapacity(0, 1.0f));
    }
    //</editor-fold>
    //
    //<editor-fold desc="sortGivenFragmentListByPropertyAndSortType test methods" defaultstate="collapsed">
    /**
     * Builds a list of three real FragmentDataModel objects with distinct values for every property the sort comparator
     * switches on (name, uniqueSmiles, parentMoleculeName, absoluteFrequency, absolutePercentage, moleculeFrequency,
     * moleculePercentage). The three fragments are intentionally constructed so that each property yields a different,
     * unambiguous ordering.
     *
     * @return list of three FragmentDataModel objects
     */
    private List<FragmentDataModel> buildFragmentList() {
        List<FragmentDataModel> tmpFragmentList = new ArrayList<>(3);
        //Fragment A: lowest in every numeric/lexical property
        FragmentDataModel tmpFragmentA = new FragmentDataModel("CCO", "A", new HashMap<>());
        tmpFragmentA.setAbsoluteFrequency(1);
        tmpFragmentA.setAbsolutePercentage(10.0);
        tmpFragmentA.setMoleculeFrequency(1);
        tmpFragmentA.setMoleculePercentage(10.0);
        //getParentMoleculeName() reads from the parentMolecules set (returns "" while empty), so register the parent there
        tmpFragmentA.getParentMolecules().add(new MoleculeDataModel("CCO", "ParentA", new HashMap<>()));
        //Fragment B: middle in every numeric/lexical property
        FragmentDataModel tmpFragmentB = new FragmentDataModel("CCN", "B", new HashMap<>());
        tmpFragmentB.setAbsoluteFrequency(2);
        tmpFragmentB.setAbsolutePercentage(20.0);
        tmpFragmentB.setMoleculeFrequency(2);
        tmpFragmentB.setMoleculePercentage(20.0);
        tmpFragmentB.getParentMolecules().add(new MoleculeDataModel("CCN", "ParentB", new HashMap<>()));
        //Fragment C: highest in every numeric/lexical property
        FragmentDataModel tmpFragmentC = new FragmentDataModel("CCC", "C", new HashMap<>());
        tmpFragmentC.setAbsoluteFrequency(3);
        tmpFragmentC.setAbsolutePercentage(30.0);
        tmpFragmentC.setMoleculeFrequency(3);
        tmpFragmentC.setMoleculePercentage(30.0);
        tmpFragmentC.getParentMolecules().add(new MoleculeDataModel("CCC", "ParentC", new HashMap<>()));
        //Note: uniqueSmiles ordering is CCC < CCN < CCO -> C, B, A ascending; deliberately the reverse of name ordering.
        tmpFragmentList.add(tmpFragmentB);
        tmpFragmentList.add(tmpFragmentC);
        tmpFragmentList.add(tmpFragmentA);
        return tmpFragmentList;
    }
    //
    /**
     * Tests sorting the fragment list ascending and descending by every property handled by the sort comparator and
     * asserts the resulting element order via the fragment names. This exercises the full comparator lambda across all
     * DataModelPropertiesForTableView keys it handles, both ascending and descending.
     */
    @Test
    public void sortGivenFragmentListByPropertyAndSortTypeCoversAllPropertiesTest() {
        //absoluteFrequency: A(1) < B(2) < C(3)
        this.assertSortOrder("absoluteFrequency", true, "A", "B", "C");
        this.assertSortOrder("absoluteFrequency", false, "C", "B", "A");
        //absolutePercentage: A(10) < B(20) < C(30)
        this.assertSortOrder("absolutePercentage", true, "A", "B", "C");
        this.assertSortOrder("absolutePercentage", false, "C", "B", "A");
        //moleculeFrequency: A(1) < B(2) < C(3)
        this.assertSortOrder("moleculeFrequency", true, "A", "B", "C");
        this.assertSortOrder("moleculeFrequency", false, "C", "B", "A");
        //moleculePercentage: A(10) < B(20) < C(30)
        this.assertSortOrder("moleculePercentage", true, "A", "B", "C");
        this.assertSortOrder("moleculePercentage", false, "C", "B", "A");
        //name: A < B < C lexically
        this.assertSortOrder("name", true, "A", "B", "C");
        this.assertSortOrder("name", false, "C", "B", "A");
        //uniqueSmiles: CCC(C) < CCN(B) < CCO(A) lexically
        this.assertSortOrder("uniqueSmiles", true, "C", "B", "A");
        this.assertSortOrder("uniqueSmiles", false, "A", "B", "C");
        //parentMoleculeName: ParentA(A) < ParentB(B) < ParentC(C) lexically
        this.assertSortOrder("parentMoleculeName", true, "A", "B", "C");
        this.assertSortOrder("parentMoleculeName", false, "C", "B", "A");
    }
    //
    /**
     * Helper that builds a fresh fragment list, sorts it by the given property in the given direction and asserts the
     * resulting order of fragment names.
     *
     * @param aProperty property string to sort by
     * @param ascending {@code true} for ascending order, {@code false} for descending
     * @param aFirstName expected name of the first element after sorting
     * @param aSecondName expected name of the second element after sorting
     * @param aThirdName expected name of the third element after sorting
     */
    private void assertSortOrder(String aProperty, boolean ascending, String aFirstName, String aSecondName, String aThirdName) {
        List<FragmentDataModel> tmpFragmentList = this.buildFragmentList();
        CollectionUtil.sortGivenFragmentListByPropertyAndSortType(tmpFragmentList, aProperty, ascending);
        Assertions.assertEquals(aFirstName, tmpFragmentList.get(0).getName());
        Assertions.assertEquals(aSecondName, tmpFragmentList.get(1).getName());
        Assertions.assertEquals(aThirdName, tmpFragmentList.get(2).getName());
    }
    //
    /**
     * Tests whether sorting by a property string that is not part of DataModelPropertiesForTableView throws an
     * IllegalArgumentException (covers the property == null branch of the comparator lambda).
     */
    @Test
    public void sortGivenFragmentListByUnknownPropertyThrowsTest() {
        List<FragmentDataModel> tmpFragmentList = this.buildFragmentList();
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> CollectionUtil.sortGivenFragmentListByPropertyAndSortType(tmpFragmentList, "notAProperty", true));
    }
    //
    /**
     * Tests whether sorting a list of plain MoleculeDataModel objects by a Fragment-only property throws an
     * IllegalArgumentException (covers the ClassCastException to IllegalArgumentException branch of the comparator
     * lambda).
     */
    @Test
    public void sortGivenMoleculeListByFragmentPropertyThrowsTest() {
        List<MoleculeDataModel> tmpMoleculeList = new ArrayList<>(2);
        tmpMoleculeList.add(new MoleculeDataModel("CCO", "MolA", new HashMap<>()));
        tmpMoleculeList.add(new MoleculeDataModel("CCN", "MolB", new HashMap<>()));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> CollectionUtil.sortGivenFragmentListByPropertyAndSortType(tmpMoleculeList, "absoluteFrequency", true));
    }
    //
    /**
     * Tests whether sorting by a property that is part of DataModelPropertiesForTableView but not handled in the sort
     * comparator (the default switch branch, e.g. "structure") leaves the list unchanged. This exercises the default
     * branch that returns 0 for every comparison.
     */
    @Test
    public void sortGivenFragmentListByUnhandledPropertyLeavesOrderUnchangedTest() {
        List<FragmentDataModel> tmpFragmentList = this.buildFragmentList();
        List<String> tmpNamesBefore = new ArrayList<>(tmpFragmentList.size());
        for (FragmentDataModel tmpFragment : tmpFragmentList) {
            tmpNamesBefore.add(tmpFragment.getName());
        }
        CollectionUtil.sortGivenFragmentListByPropertyAndSortType(tmpFragmentList, "structure", true);
        List<String> tmpNamesAfter = new ArrayList<>(tmpFragmentList.size());
        for (FragmentDataModel tmpFragment : tmpFragmentList) {
            tmpNamesAfter.add(tmpFragment.getName());
        }
        Assertions.assertEquals(tmpNamesBefore, tmpNamesAfter);
    }
    //</editor-fold>
    //
    //<editor-fold desc="private constructor coverage" defaultstate="collapsed">
    /**
     * Tests whether the private parameter-less constructor of the CollectionUtil utility class can be invoked via
     * reflection, covering the otherwise-unreachable constructor of this final utility class.
     *
     * @throws Exception if reflective instantiation fails
     */
    @Test
    public void privateConstructorTest() throws Exception {
        TestUtil.assertPrivateConstructorIsInvocable(CollectionUtil.class);
    }
    //</editor-fold>
}
