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

import java.io.PrintWriter;

/**
 * Interface for preferences.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public interface IPreference extends Comparable<IPreference> {
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
    //<editor-fold defaultstate="collapsed" desc="Copying">
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
