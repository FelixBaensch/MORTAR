/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2020  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas-schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.preference;

/**
 * Preference types for fast determination of a preference class without using reflection or 'instanceof'.<br>
 * NOTE: All newly added enum constants must also be added in the PreferenceFactory.reinitializePreference() method.
 *
 * @author Jonas Schaub
 */
public enum PreferenceType {
    /**
     * Constant associated with class BooleanPreference.
     */
    BOOLEAN,

    /**
     * Constant associated with class RGBColorPreference.
     */
    RGB_COLOR,

    /**
     * Constant associated with class SingleIntegerPreference.
     */
    SINGLE_INTEGER,

    /**
     * Constant associated with class SingleNumberPreference.
     */
    SINGLE_NUMBER,

    /**
     * Constant associated with class SingleTermPreference.
     */
    SINGLE_TERM;
}