/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2023  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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
 * TODO:
 * - implement method that initializes a whole new IPreference object?
 * - newly added preference classes need to be added to 'reinitializePreference()' method
 */

import java.io.BufferedReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for creating IPreference objects.
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
