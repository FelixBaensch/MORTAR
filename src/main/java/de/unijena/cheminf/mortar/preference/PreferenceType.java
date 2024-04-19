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

package de.unijena.cheminf.mortar.preference;

/**
 * Preference types for fast determination of a preference class without using reflection or 'instanceof'.<br>
 * NOTE: All newly added enum constants must also be added in the PreferenceFactory.reinitializePreference() method.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
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
