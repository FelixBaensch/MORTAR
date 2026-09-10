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

import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.IMoleculeFragmenter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Test class for the custom-made {@link SimpleIDisplayEnumConstantProperty} JavaFx
 * property wrapping an enum constant display name.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class SimpleIDisplayEnumConstantPropertyTest {
    /**
     * Constructor setting the default locale.
     *
     * @throws Exception if anything goes wrong
     */
    public SimpleIDisplayEnumConstantPropertyTest() throws Exception {
        Locale.setDefault(Locale.of("en", "GB"));
    }
    /**
     * Basic test for retrieval of associated enum, currently set option, and available options.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void test() throws Exception {
        SimpleIDisplayEnumConstantProperty tmpEnumProperty = new SimpleIDisplayEnumConstantProperty(this, "testProp",
                IMoleculeFragmenter.FragmentSaturationOption.HYDROGEN_SATURATION,
                IMoleculeFragmenter.FragmentSaturationOption.class);
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.class, tmpEnumProperty.getAssociatedEnum());
        Assertions.assertEquals(Message.get("IMoleculeFragmenter.FragmentSaturationOption.hydrogenSaturation.displayName"), tmpEnumProperty.get().getDisplayName());
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.HYDROGEN_SATURATION, tmpEnumProperty.get());
        IDisplayEnum[] tmpAvailableOptions = (IDisplayEnum[]) tmpEnumProperty.getAssociatedEnumConstants();
        for (IDisplayEnum tmpOption : tmpAvailableOptions) {
            Assertions.assertDoesNotThrow(() -> tmpEnumProperty.set(tmpOption));
        }
    }

    /**
     * Tests the alternative constructor without an initial value (bean, name, class). The associated enum class must be
     * retrievable.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testConstructorWithoutInitialValue() throws Exception {
        SimpleIDisplayEnumConstantProperty tmpEnumProperty = new SimpleIDisplayEnumConstantProperty(this, "testProp",
                IMoleculeFragmenter.FragmentSaturationOption.class);
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.class, tmpEnumProperty.getAssociatedEnum());
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.values().length,
                tmpEnumProperty.getAssociatedEnumConstantDisplayNames().length);
    }

    /**
     * Tests the alternative constructor with an initial value but without bean and property name (initialValue, class).
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testConstructorWithInitialValueOnly() throws Exception {
        SimpleIDisplayEnumConstantProperty tmpEnumProperty = new SimpleIDisplayEnumConstantProperty(
                IMoleculeFragmenter.FragmentSaturationOption.HYDROGEN_SATURATION,
                IMoleculeFragmenter.FragmentSaturationOption.class);
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.class, tmpEnumProperty.getAssociatedEnum());
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.HYDROGEN_SATURATION, tmpEnumProperty.get());
    }

    /**
     * Tests the alternative constructor taking only the associated enum class.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testConstructorWithClassOnly() throws Exception {
        SimpleIDisplayEnumConstantProperty tmpEnumProperty = new SimpleIDisplayEnumConstantProperty(
                IMoleculeFragmenter.FragmentSaturationOption.class);
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.class, tmpEnumProperty.getAssociatedEnum());
    }

    /**
     * Tests that a null associated enum class is rejected with a NullPointerException across the constructor variants.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testNullClassGuard() throws Exception {
        Assertions.assertThrows(NullPointerException.class,
                () -> new SimpleIDisplayEnumConstantProperty((Class) null));
        Assertions.assertThrows(NullPointerException.class,
                () -> new SimpleIDisplayEnumConstantProperty(this, "testProp", (Class) null));
    }

    /**
     * Tests that a non-enum class (here String.class) is rejected with an IllegalArgumentException.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testNonEnumClassGuard() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SimpleIDisplayEnumConstantProperty(String.class));
    }

    /**
     * Tests that an enum class that does not implement IDisplayEnum is rejected with an IllegalArgumentException. A
     * plain test-local enum (not implementing IDisplayEnum) drives this negative path.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testNonIDisplayEnumClassGuard() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SimpleIDisplayEnumConstantProperty(PlainTestEnum.class));
    }

    /**
     * Tests the guard branches of the remaining constructor variants (all-params and initialValue+class) so the
     * defensive non-enum / empty-enum / non-IDisplayEnum throws in those constructors are exercised too. A valid
     * IDisplayEnum initial value is supplied while the associated enum class is deliberately mismatched.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testGuardBranchesOnInitialValueConstructors() throws Exception {
        IDisplayEnum tmpValidInitialValue = IMoleculeFragmenter.FragmentSaturationOption.HYDROGEN_SATURATION;
        // all-params constructor: non-enum, empty-enum and non-IDisplayEnum guards
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SimpleIDisplayEnumConstantProperty(this, "testProp", tmpValidInitialValue, String.class));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SimpleIDisplayEnumConstantProperty(this, "testProp", tmpValidInitialValue, EmptyTestEnum.class));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SimpleIDisplayEnumConstantProperty(this, "testProp", tmpValidInitialValue, PlainTestEnum.class));
        // initialValue + class constructor: non-enum, empty-enum and non-IDisplayEnum guards
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SimpleIDisplayEnumConstantProperty(tmpValidInitialValue, String.class));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SimpleIDisplayEnumConstantProperty(tmpValidInitialValue, EmptyTestEnum.class));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SimpleIDisplayEnumConstantProperty(tmpValidInitialValue, PlainTestEnum.class));
        // bean + name + class constructor: empty-enum and non-IDisplayEnum guards
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SimpleIDisplayEnumConstantProperty(this, "testProp", EmptyTestEnum.class));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SimpleIDisplayEnumConstantProperty(this, "testProp", PlainTestEnum.class));
        // class-only constructor: empty-enum guard
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new SimpleIDisplayEnumConstantProperty(EmptyTestEnum.class));
    }

    /**
     * Tests that getAssociatedEnumConstantDisplayNames returns the display name of every enum constant of the
     * associated enum class.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testGetAssociatedEnumConstantDisplayNames() throws Exception {
        SimpleIDisplayEnumConstantProperty tmpEnumProperty = new SimpleIDisplayEnumConstantProperty(
                IMoleculeFragmenter.FragmentSaturationOption.class);
        String[] tmpDisplayNames = tmpEnumProperty.getAssociatedEnumConstantDisplayNames();
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.values().length, tmpDisplayNames.length);
        List<String> tmpDisplayNameList = Arrays.asList(tmpDisplayNames);
        for (IMoleculeFragmenter.FragmentSaturationOption tmpOption : IMoleculeFragmenter.FragmentSaturationOption.values()) {
            Assertions.assertTrue(tmpDisplayNameList.contains(tmpOption.getDisplayName()));
        }
    }

    /**
     * Tests translateDisplayNameToEnumConstant for a valid display name (returns the matching constant), a null
     * argument (throws NullPointerException), and an unknown display name (throws IllegalArgumentException).
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testTranslateDisplayNameToEnumConstant() throws Exception {
        SimpleIDisplayEnumConstantProperty tmpEnumProperty = new SimpleIDisplayEnumConstantProperty(
                IMoleculeFragmenter.FragmentSaturationOption.class);
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.HYDROGEN_SATURATION,
                tmpEnumProperty.translateDisplayNameToEnumConstant(
                        IMoleculeFragmenter.FragmentSaturationOption.HYDROGEN_SATURATION.getDisplayName()));
        Assertions.assertThrows(NullPointerException.class,
                () -> tmpEnumProperty.translateDisplayNameToEnumConstant(null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> tmpEnumProperty.translateDisplayNameToEnumConstant("not a valid display name"));
    }

    /**
     * A plain test-local enum that does NOT implement IDisplayEnum, used to exercise the constructor guard branch that
     * rejects enum classes which do not implement the IDisplayEnum interface.
     */
    private enum PlainTestEnum {
        /**
         * First arbitrary constant.
         */
        ALPHA,
        /**
         * Second arbitrary constant.
         */
        BETA;
    }

    /**
     * A test-local enum that declares no constants, used to drive the empty-enum guard branches of the constructors.
     */
    private enum EmptyTestEnum {
    }
}
