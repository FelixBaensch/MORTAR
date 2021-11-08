/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2021  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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
 * - Implement preference container reference (GUID) to ensure that no preference is added to multiple preference
 *      containers?
 */

import java.io.PrintWriter;

/**
 * Interface for preferences.
 *
 * @author Jonas Schaub
 */
public interface IPreference extends Comparable<IPreference>, Cloneable {
    //<editor-fold defaultstate="collapsed" desc="Public properties (get)">
    /**
     * Returns the name of this preference.
     *
     * @return the name of this preference instance
     */
    public String getName();

    /**
     * Returns the GUID of this preference.
     *
     * @return the GUID of this preference instance
     */
    public String getGUID();

    /**
     * Returns the content of this preference as a string literal representation or its
     * type or class if its content is not representable by a string.
     *
     * @return the content of this preference instance as a string literal or its type or class
     */
    public String getContentRepresentative();

    /**
     * Returns the current version of this preference class.
     *
     * @return the current version
     */
    public String getVersion();

    /**
     * Returns the type of this preference.
     *
     * @return the PreferenceType enum constant associated with this preference implementation.
     */
    public PreferenceType getType();
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public methods">
    //<editor-fold defaultstate="collapsed" desc="Persistence">
    /**
     * Appends a string representation of this preference to a line-based text file via the given
     * PrintWriter for persistence.
     *
     * @param aPrintWriter writing to the destined line-based text file
     */
    public void writeRepresentation(PrintWriter aPrintWriter);
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Comparing and sorting">
    /**
     * Compares this preference object with another preference object based on their GUIDs. Returns a negative integer,
     * zero, or a positive integer as the GUID of this preference object is less than, equal to, or greater than the
     * GUID of aPreference.
     *
     * @param aPreference the preference object to compare with
     * @return a negative integer, zero, or a positive integer as this preference object is less than, equal to, or
     * greater than aPreference
     * @throws NullPointerException if aPreference is 'null'
     */
    @Override
    public int compareTo(IPreference aPreference) throws NullPointerException;

    /**
     * For IPreference objects equality is determined by comparing their GUID strings.
     * So preferenceA.equals(preferenceB) is true if preferenceA.getGUID().equals(preferenceB.getGUID()) is true.
     * All IPreference implementations must override this method of class Object in order for their compareTo() method
     * to be 'consistent with equals'.
     *
     * @param anObject the object to test for equality
     * @return true if this object equals anObject
     */
    @Override
    public boolean equals(Object anObject);

    /**
     * Returns a hash code for this IPreference object based on its GUID string.
     * All IPreference implementations must override this method of class Object in order to maintain the general contract
     * for the hashCode method, since the equals method is overridden.
     *
     * @return hash code based on GUID
     */
    @Override
    public int hashCode();
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Cloning and copying">
    /**
     * Returns a clone of this preference object. When implementing this method the compiler recognizes it as an
     * override of the clone method in class Object.
     *
     * @return a clone of this preference object
     * @throws CloneNotSupportedException if cloning this specific object fails
     */
    public IPreference clone() throws CloneNotSupportedException;

    /**
     * Returns a deep copy of this object.
     *
     * @return a deep copy of this object
     * @throws java.lang.CloneNotSupportedException if copying this specific object fails
     */
    public IPreference copy() throws CloneNotSupportedException;
    //</editor-fold>

    @Override
    public String toString();
    //</editor-fold>
}