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

import de.unijena.cheminf.mortar.model.fragmentation.algorithm.IMoleculeFragmenter;
import org.junit.Test;

/**
 * Test class for the custom-made {@link de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty} JavaFx
 * property wrapping an enum constant name.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class SimpleEnumConstantNamePropertyTest {

    /**
     * Basic test for retrieval of associated enum, currently set option, and available options. All are printed to
     * console.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void test() throws Exception {
        SimpleEnumConstantNameProperty tmpEnumProperty = new SimpleEnumConstantNameProperty(this, "testProp",
                IMoleculeFragmenter.FragmentSaturationOption.HYDROGEN_SATURATION.name(),
                IMoleculeFragmenter.FragmentSaturationOption.class);
        System.out.println("Associated enum: " + tmpEnumProperty.getAssociatedEnum());
        Enum[] tmpAvailableOptions = tmpEnumProperty.getAssociatedEnumConstants();
        System.out.println("Currently set option: " + tmpEnumProperty.get());
        tmpEnumProperty.getEnumValue();
        System.out.println("Available options: ");
        for (Enum tmpOption : tmpAvailableOptions) {
            System.out.println("\t" + tmpOption.name());
            tmpEnumProperty.setEnumValue(tmpOption);
            tmpEnumProperty.set(tmpOption.name());
        }
    }

}