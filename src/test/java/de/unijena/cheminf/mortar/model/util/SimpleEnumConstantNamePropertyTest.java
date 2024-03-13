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

import de.unijena.cheminf.mortar.model.fragmentation.algorithm.IMoleculeFragmenter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class for the custom-made {@link de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty} JavaFx
 * property wrapping an enum constant name.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class SimpleEnumConstantNamePropertyTest {
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
            Assertions.assertDoesNotThrow(() -> {tmpEnumProperty.setEnumValue(tmpOption);});
            Assertions.assertDoesNotThrow(() -> {tmpEnumProperty.set(tmpOption.name());});
        }
    }
}
