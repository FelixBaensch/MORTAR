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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Test class for the custom-made {@link de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty} JavaFx
 * property wrapping an enum constant name.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class SimpleEnumConstantNamePropertyTest {
    /**
     * Constructor setting the default locale to en-GB. The fixture enum used in these tests resolves its display names
     * through the Message resource bundle whose static initializer loads the bundle for the default locale; pinning the
     * locale to en-GB ensures the bundle is found regardless of the test JVM's environment locale.
     */
    public SimpleEnumConstantNamePropertyTest() {
        Locale.setDefault(Locale.of("en", "GB"));
    }
    //
    /**
     * Basic test for retrieval of associated enum, currently set option, and available options.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void test() throws Exception {
        SimpleEnumConstantNameProperty tmpEnumProperty = new SimpleEnumConstantNameProperty(this, "testProp",
                IMoleculeFragmenter.FragmentSaturationOption.HYDROGEN_SATURATION.name(),
                IMoleculeFragmenter.FragmentSaturationOption.class);
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.class, tmpEnumProperty.getAssociatedEnum());
        Assertions.assertEquals("HYDROGEN_SATURATION", tmpEnumProperty.get());
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.HYDROGEN_SATURATION, tmpEnumProperty.getEnumValue());
        Enum[] tmpAvailableOptions = tmpEnumProperty.getAssociatedEnumConstants();
        for (Enum tmpOption : tmpAvailableOptions) {
            Assertions.assertDoesNotThrow(() -> {IMoleculeFragmenter.FragmentSaturationOption.valueOf(tmpOption.name());});
            Assertions.assertDoesNotThrow(() -> tmpEnumProperty.setEnumValue(tmpOption));
            Assertions.assertDoesNotThrow(() -> tmpEnumProperty.set(tmpOption.name()));
        }
    }
    //
    /**
     * Tests the alternative constructor without an initial value (bean, name, class). The associated enum class must be
     * retrievable and the names array must reflect all enum constants.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testConstructorWithoutInitialValue() throws Exception {
        SimpleEnumConstantNameProperty tmpEnumProperty = new SimpleEnumConstantNameProperty(this, "testProp",
                IMoleculeFragmenter.FragmentSaturationOption.class);
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.class, tmpEnumProperty.getAssociatedEnum());
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.values().length,
                tmpEnumProperty.getAssociatedEnumConstantNames().length);
    }
    //
    /**
     * Tests the alternative constructor with an initial value but without bean and property name (initialValue, class).
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testConstructorWithInitialValueOnly() throws Exception {
        SimpleEnumConstantNameProperty tmpEnumProperty = new SimpleEnumConstantNameProperty(
                IMoleculeFragmenter.FragmentSaturationOption.HYDROGEN_SATURATION.name(),
                IMoleculeFragmenter.FragmentSaturationOption.class);
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.class, tmpEnumProperty.getAssociatedEnum());
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.HYDROGEN_SATURATION.name(),
                tmpEnumProperty.get());
    }
    //
    /**
     * Tests the alternative constructor taking only the associated enum class. The associated enum class must be
     * retrievable and the names array must reflect all enum constants.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testConstructorWithClassOnly() throws Exception {
        SimpleEnumConstantNameProperty tmpEnumProperty = new SimpleEnumConstantNameProperty(
                IMoleculeFragmenter.FragmentSaturationOption.class);
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.class, tmpEnumProperty.getAssociatedEnum());
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.values().length,
                tmpEnumProperty.getAssociatedEnumConstantNames().length);
    }
    //
    /**
     * Tests that setValue accepts a valid enum constant name and rejects an unknown name with an
     * IllegalArgumentException.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testSetValue() throws Exception {
        SimpleEnumConstantNameProperty tmpEnumProperty = new SimpleEnumConstantNameProperty(
                IMoleculeFragmenter.FragmentSaturationOption.class);
        tmpEnumProperty.setValue(IMoleculeFragmenter.FragmentSaturationOption.NO_SATURATION.name());
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.NO_SATURATION.name(), tmpEnumProperty.get());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> tmpEnumProperty.setValue("NOT_AN_ENUM_CONSTANT_NAME"));
    }
    //
    /**
     * Tests that getAssociatedEnumConstantNames returns the names of every enum constant of the associated enum class.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testGetAssociatedEnumConstantNames() throws Exception {
        SimpleEnumConstantNameProperty tmpEnumProperty = new SimpleEnumConstantNameProperty(
                IMoleculeFragmenter.FragmentSaturationOption.class);
        String[] tmpNames = tmpEnumProperty.getAssociatedEnumConstantNames();
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.values().length, tmpNames.length);
        List<String> tmpNameList = Arrays.asList(tmpNames);
        for (IMoleculeFragmenter.FragmentSaturationOption tmpOption : IMoleculeFragmenter.FragmentSaturationOption.values()) {
            Assertions.assertTrue(tmpNameList.contains(tmpOption.name()));
        }
    }
    //
    /**
     * Tests translateNameToEnumConstant for a valid name (returns the matching constant), a null argument (throws
     * NullPointerException), and an unknown name (throws IllegalArgumentException).
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testTranslateNameToEnumConstant() throws Exception {
        SimpleEnumConstantNameProperty tmpEnumProperty = new SimpleEnumConstantNameProperty(
                IMoleculeFragmenter.FragmentSaturationOption.class);
        Assertions.assertEquals(IMoleculeFragmenter.FragmentSaturationOption.HYDROGEN_SATURATION,
                tmpEnumProperty.translateNameToEnumConstant(
                        IMoleculeFragmenter.FragmentSaturationOption.HYDROGEN_SATURATION.name()));
        Assertions.assertThrows(NullPointerException.class, () -> tmpEnumProperty.translateNameToEnumConstant(null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> tmpEnumProperty.translateNameToEnumConstant("NOT_AN_ENUM_CONSTANT_NAME"));
    }
}
