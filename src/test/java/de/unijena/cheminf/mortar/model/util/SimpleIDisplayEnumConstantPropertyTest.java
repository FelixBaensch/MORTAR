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

import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.IMoleculeFragmenter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
}
