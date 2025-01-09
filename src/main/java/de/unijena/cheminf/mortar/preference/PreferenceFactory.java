/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2025  Felix Baensch, Jonas Schaub (felix.j.baensch@gmail.com, jonas.schaub@uni-jena.de)
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

import java.io.BufferedReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for creating IPreference objects. Note for developers: newly added preference classes need to be added
 * to 'reinitializePreference()' method.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public final class PreferenceFactory {
    //<editor-fold defaultstate="collapsed" desc="Private static final class constants">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(PreferenceFactory.class.getName());
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Private constructor">
    /**
     * Private parameter-less constructor.
     * Introduced because javadoc build complained about classes without declared default constructor.
     */
    private PreferenceFactory() {
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public static methods">
    /**
     * (Re-)instantiates a new IPreference object of the class associated with the given PreferenceType constant name
     * from a line-based text file.
     * <p>
     * NOTE: No parameter pre-checks are performed to increase performance but any occurring exception will be caught.
     *
     * @param aPreferenceTypeName name of a PreferenceType constant
     * @param aReader to read the persisted representation from
     * @return a new IPreference object of the class associated with the PreferenceType constant of the
     * given name
     * @throws java.lang.IllegalArgumentException if anything goes wrong
     */
    public static IPreference reinitializePreference(String aPreferenceTypeName, BufferedReader aReader)
            throws IllegalArgumentException {
        try {
            if (aPreferenceTypeName.equals(PreferenceType.SINGLE_NUMBER.name())) {
                return new SingleNumberPreference(aReader);
            } else if (aPreferenceTypeName.equals(PreferenceType.SINGLE_INTEGER.name())) {
                return new SingleIntegerPreference(aReader);
            } else if (aPreferenceTypeName.equals(PreferenceType.SINGLE_TERM.name())) {
                return new SingleTermPreference(aReader);
            } else if (aPreferenceTypeName.equals(PreferenceType.BOOLEAN.name())) {
                return new BooleanPreference(aReader);
            } else if (aPreferenceTypeName.equals(PreferenceType.RGB_COLOR.name())) {
                return new RGBColorPreference(aReader);
            } else {
                throw new IllegalArgumentException("No PreferenceType constant with name '"
                        + aPreferenceTypeName
                        + "' could be found.");
            }
        } catch (Exception anException) {
            PreferenceFactory.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            throw new IllegalArgumentException("Preference object could not be instantiated with given arguments.");
        }
    }
    //</editor-fold>
}
