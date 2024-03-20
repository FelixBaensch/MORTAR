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

import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Base class for all preference classes. Non-abstract preference classes do not have to extend this class (as long as
 * they implement IPreference). But extending this class offers ready for use implementations of some basic preference
 * functionalities like GUID or name management and of methods defining the natural ordering of preferences
 * based on their GUIDs. This class also holds preference-related constants like the preference name pattern and static
 * validation methods.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public abstract class BasePreference implements IPreference {
    //<editor-fold defaultstate="collapsed" desc="Private static final class constants">
    /**
     * Preference name regex pattern.
     * Allowed characters: 1-9, a-z, A-z, -, [], (), {}, ., space (ASCII 32, hexadecimal value: 20, see below), and ,.
     * The first character must be a capital letter.
     * '#' is reserved for comments in text files for persisting objects.
     */
    private static final Pattern PREFERENCE_NAME_PATTERN = Pattern.compile("\\A[A-Z][0-9a-zA-Z\\.\\,\\-\\[\\]\\(\\)\\{\\}\\x20]*+\\z");

    /**
     * Seed for hashCode() method.
     */
    private static final int HASH_SEED = 13;

    /**
     * Factor for including GUID string in hash code produced by hashCode() method.
     */
    private static final int HASH_FACTOR_GUID = 919;

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(BasePreference.class.getName());
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Protected class variables">
    /**
     * Name of this preference object.
     */
    protected String name;

    /**
     * Globally unique identifier of this object.
     */
    protected String guid;
    //</editor-fold>
    //
    //<editor-fold desc="Protected constructor">
    /**
     * Protected parameter-less constructor that simply calls super() (Object class default constructor).
     * Introduced because javadoc build complained about classes without declared default constructor.
     */
    protected BasePreference() {
        super();
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public abstract methods">
    @Override
    public abstract IPreference clone() throws CloneNotSupportedException;

    @Override
    public abstract String toString();
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public properties (get)">
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getGUID() {
        return this.guid;
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public methods">
    //<editor-fold defaultstate="collapsed" desc="Comparing and sorting">
    @Override
    public int compareTo(IPreference aPreference) throws NullPointerException {
        Objects.requireNonNull(aPreference, "aPreference is 'null'");
        //Comparing on String level
        return this.guid.compareTo(aPreference.getGUID());
    }

    @Override
    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (anObject == null || (anObject.getClass() != this.getClass())) {
            return false;
        }
        IPreference tmpPreference = (IPreference) anObject;
        return this.hashCode() == tmpPreference.hashCode();
    }

    @Override
    public int hashCode() {
        int tmpHash = BasePreference.HASH_SEED;
        tmpHash = BasePreference.HASH_FACTOR_GUID * tmpHash + Objects.hashCode(this.guid);
        return tmpHash;
    }
    //</editor-fold>
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public static methods">
    /**
     * Tests whether the given string would be allowed as name of a new preference object.
     *
     * @param aName the string to test; leading and trailing whitespaces will not be considered
     * @return true if aName matches the defined pattern for preference names
     */
    public static boolean isValidName(String aName) {
        if (Objects.isNull(aName)) {
            return false;
        }
        return BasePreference.PREFERENCE_NAME_PATTERN.matcher(aName.trim()).matches();
    }
    //</editor-fold>
}
