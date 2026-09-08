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

import de.unijena.cheminf.mortar.model.fragmentation.algorithm.IMoleculeFragmenter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;

/**
 * Test class for the abstract {@link de.unijena.cheminf.mortar.model.util.SimpleEnumConstantPropertyBase}. Since the
 * class is abstract, its constructor guard branches are exercised through the concrete subclass
 * {@link de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty}. Most of the base lines are already driven
 * by the subclass tests; this class adds the base branches those tests do not reach — most notably the empty-enum
 * IllegalArgumentException branch, which requires a constant-less fixture enum.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class SimpleEnumConstantPropertyBaseTest {
    /**
     * Constructor setting the default locale to en-GB to match the established convention of the property/enum
     * infrastructure tests.
     */
    public SimpleEnumConstantPropertyBaseTest() {
        Locale.setDefault(Locale.of("en", "GB"));
    }
    //
    /**
     * Tests that the base class rejects a constant-less enum class with an IllegalArgumentException. The branch is
     * reached through every constructor variant of a concrete subclass; a test-local enum that declares no constants
     * drives the empty-enum guard in the abstract base.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testEmptyEnumGuard() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SimpleEnumConstantNameProperty(EmptyTestEnum.class));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SimpleEnumConstantNameProperty(this, "testProp", EmptyTestEnum.class));
    }
    //
    /**
     * Tests that the base class rejects a non-enum class with an IllegalArgumentException across constructor variants.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testNonEnumGuard() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SimpleEnumConstantNameProperty(String.class));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SimpleEnumConstantNameProperty(this, "testProp", String.class));
    }
    //
    /**
     * Tests that the base class rejects a null associated enum class with a NullPointerException across constructor
     * variants.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testNullEnumGuard() throws Exception {
        Assertions.assertThrows(NullPointerException.class,
                () -> new SimpleEnumConstantNameProperty((Class) null));
        Assertions.assertThrows(NullPointerException.class,
                () -> new SimpleEnumConstantNameProperty(this, "testProp", (Class) null));
    }
    //
    /**
     * Tests the non-enum and empty-enum guard branches of the remaining base constructor variants — the
     * (initialValue, class) variant — so the defensive throws in every base constructor are exercised. A valid initial
     * value string is supplied while the associated class is deliberately not a usable enum.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testGuardBranchesOnInitialValueConstructor() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SimpleEnumConstantNameProperty("anyValue", String.class));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SimpleEnumConstantNameProperty("anyValue", EmptyTestEnum.class));
    }
    //
    /**
     * Tests that the base getAssociatedEnum, getAssociatedEnumConstants, getEnumValue, and setEnumValue accessors behave
     * as expected when exercised through a concrete subclass over a valid enum class.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testBaseAccessors() throws Exception {
        SimpleEnumConstantNameProperty tmpProperty = new SimpleEnumConstantNameProperty(this, "testProp",
                IMoleculeFragmenter.FragmentSaturationOption.HYDROGEN_SATURATION.name(),
                IMoleculeFragmenter.FragmentSaturationOption.class);
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.class, tmpProperty.getAssociatedEnum());
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.values().length,
                tmpProperty.getAssociatedEnumConstants().length);
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.HYDROGEN_SATURATION,
                tmpProperty.getEnumValue());
        tmpProperty.setEnumValue(IMoleculeFragmenter.FragmentSaturationOption.NO_SATURATION);
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.NO_SATURATION, tmpProperty.getEnumValue());
    }
    //
    /**
     * A test-local enum that declares no constants, used to drive the empty-enum guard branch in the abstract base
     * class constructors.
     */
    private enum EmptyTestEnum {
    }
}
