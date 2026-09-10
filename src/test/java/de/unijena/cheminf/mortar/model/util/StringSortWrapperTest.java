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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class for the StringSortWrapper sort-helper: constructor guards, equals, hashCode, compareTo and the getters.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class StringSortWrapperTest {
    //<editor-fold desc="constructor" defaultstate="collapsed">
    /**
     * Constructor that needs nothing to set up.
     */
    public StringSortWrapperTest() {
    }
    //</editor-fold>
    //
    //<editor-fold desc="constructor guard test methods" defaultstate="collapsed">
    /**
     * Tests whether the constructor throws a NullPointerException when the enclosed object is null.
     */
    @Test
    public void constructorNullObjectThrowsTest() {
        Assertions.assertThrows(NullPointerException.class, () -> new StringSortWrapper<String>(null, "sort"));
    }
    //
    /**
     * Tests whether the constructor throws a NullPointerException when the sort string is null.
     */
    @Test
    public void constructorNullSortStringThrowsTest() {
        Assertions.assertThrows(NullPointerException.class, () -> new StringSortWrapper<>("object", null));
    }
    //
    /**
     * Tests whether the constructor throws an IllegalArgumentException when the sort string is empty.
     */
    @Test
    public void constructorEmptySortStringThrowsTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new StringSortWrapper<>("object", ""));
    }
    //</editor-fold>
    //
    //<editor-fold desc="getter test methods" defaultstate="collapsed">
    /**
     * Tests whether the getters return the enclosed object and the sort string that were passed to the constructor.
     */
    @Test
    public void gettersReturnConstructorValuesTest() {
        String tmpEnclosedObject = "wrapped";
        StringSortWrapper<String> tmpWrapper = new StringSortWrapper<>(tmpEnclosedObject, "alpha");
        Assertions.assertSame(tmpEnclosedObject, tmpWrapper.getWrappedObject());
        Assertions.assertEquals("alpha", tmpWrapper.getSortString());
    }
    //</editor-fold>
    //
    //<editor-fold desc="equals test methods" defaultstate="collapsed">
    /**
     * Tests whether equals returns true for the same instance (identity branch).
     */
    @Test
    public void equalsSameInstanceTest() {
        StringSortWrapper<String> tmpWrapper = new StringSortWrapper<>("object", "alpha");
        Assertions.assertEquals(tmpWrapper, tmpWrapper);
    }
    //
    /**
     * Tests whether equals returns false when compared to null (null branch).
     */
    @Test
    public void equalsNullReturnsFalseTest() {
        StringSortWrapper<String> tmpWrapper = new StringSortWrapper<>("object", "alpha");
        Assertions.assertNotEquals(null, tmpWrapper);
    }
    //
    /**
     * Tests whether equals returns false when compared to an object of a different class (different-class branch).
     */
    @Test
    public void equalsDifferentClassReturnsFalseTest() {
        StringSortWrapper<String> tmpWrapper = new StringSortWrapper<>("object", "alpha");
        Assertions.assertNotEquals("alpha", tmpWrapper);
    }
    //
    /**
     * Tests whether two wrappers with equal sort strings are considered equal (the hashCode-equality branch).
     */
    @Test
    public void equalsEqualSortStringsReturnsTrueTest() {
        StringSortWrapper<String> tmpWrapperA = new StringSortWrapper<>("objectA", "alpha");
        StringSortWrapper<String> tmpWrapperB = new StringSortWrapper<>("objectB", "alpha");
        Assertions.assertEquals(tmpWrapperA, tmpWrapperB);
    }
    //
    /**
     * Tests whether two wrappers with different sort strings are considered unequal.
     */
    @Test
    public void equalsDifferentSortStringsReturnsFalseTest() {
        StringSortWrapper<String> tmpWrapperA = new StringSortWrapper<>("object", "alpha");
        StringSortWrapper<String> tmpWrapperB = new StringSortWrapper<>("object", "beta");
        Assertions.assertNotEquals(tmpWrapperA, tmpWrapperB);
    }
    //</editor-fold>
    //
    //<editor-fold desc="hashCode test methods" defaultstate="collapsed">
    /**
     * Tests whether hashCode is stable across repeated calls and whether wrappers with equal sort strings produce equal
     * hash codes.
     */
    @Test
    public void hashCodeIsStableAndConsistentWithSortStringTest() {
        StringSortWrapper<String> tmpWrapperA = new StringSortWrapper<>("objectA", "alpha");
        StringSortWrapper<String> tmpWrapperB = new StringSortWrapper<>("objectB", "alpha");
        Assertions.assertEquals(tmpWrapperA.hashCode(), tmpWrapperA.hashCode());
        Assertions.assertEquals(tmpWrapperA.hashCode(), tmpWrapperB.hashCode());
    }
    //
    /**
     * Tests that hashCode produces exactly the value defined by its formula, HASH_FACTOR_SORT_STRING (61) *
     * HASH_SEED (5) + sortString.hashCode() = 305 + sortString.hashCode(). Pinning the exact value detects the
     * arithmetic mutations of the formula (the {@code *} and {@code +} operators), which would otherwise go unnoticed
     * because a mere equal-hash-code check does not constrain the concrete value.
     */
    @Test
    public void hashCodePinsExactFormulaValueTest() {
        StringSortWrapper<String> tmpWrapper = new StringSortWrapper<>("object", "alpha");
        Assertions.assertEquals(61 * 5 + "alpha".hashCode(), tmpWrapper.hashCode());
    }
    //</editor-fold>
    //
    //<editor-fold desc="compareTo test methods" defaultstate="collapsed">
    /**
     * Tests whether compareTo orders wrappers according to their sort strings: negative when this sort string is smaller,
     * positive when greater, and zero when equal.
     */
    @Test
    public void compareToOrdersBySortStringTest() {
        StringSortWrapper<String> tmpWrapperAlpha = new StringSortWrapper<>("object", "alpha");
        StringSortWrapper<String> tmpWrapperBeta = new StringSortWrapper<>("object", "beta");
        StringSortWrapper<String> tmpWrapperAlphaAgain = new StringSortWrapper<>("other", "alpha");
        Assertions.assertTrue(tmpWrapperAlpha.compareTo(tmpWrapperBeta) < 0);
        Assertions.assertTrue(tmpWrapperBeta.compareTo(tmpWrapperAlpha) > 0);
        Assertions.assertEquals(0, tmpWrapperAlpha.compareTo(tmpWrapperAlphaAgain));
    }
    //
    /**
     * Tests whether compareTo throws a NullPointerException when the given StringSortWrapper is null.
     */
    @Test
    public void compareToNullThrowsTest() {
        StringSortWrapper<String> tmpWrapper = new StringSortWrapper<>("object", "alpha");
        Assertions.assertThrows(NullPointerException.class, () -> tmpWrapper.compareTo(null));
    }
    //</editor-fold>
}
