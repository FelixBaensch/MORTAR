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

package de.unijena.cheminf.mortar.model.util;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * A generic sort wrapper that can be compared to another object (e.g. for sorting) via a specified sort string.
 *
 * @param <T> the enclosed object
 * @author Jonas Schaub
 */
public class StringSortWrapper<T> implements Comparable<StringSortWrapper>{
    //<editor-fold defaultstate="collapsed" desc="Private static final class constants">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(StringSortWrapper.class.getName());

    /**
     * Seed for hashCode() method.
     */
    private static final int HASH_SEED = 5;

    /**
     * Factor for including sort string in hash code produced by hashCode() method.
     */
    private static final int HASH_FACTOR_SORT_STRING = 61;
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Private final class variables">
    /**
     * the enclosed object of type T.
     */
    private final T enclosedObject;

    /**
     * The string to sort by.
     */
    private final String sortString;
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Constructor">
    /**
     * Creates a new StringSortWrapper instance that encloses the given object of type T and compares to other objects
     * of this class via the given sort string.
     *
     * @param anEnclosedObject an enclosed object of type T
     * @param aSortString a string to sort by
     * @throws NullPointerException if one parameter is 'null'
     * @throws IllegalArgumentException if aSortString is empty
     */
    public StringSortWrapper(T anEnclosedObject, String aSortString) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anEnclosedObject, "Given object is 'null'.");
        Objects.requireNonNull(aSortString, "Given sort string is 'null'.");
        if (aSortString.isEmpty()) {
            throw new IllegalArgumentException("Given sort string is empty.");
        }
        this.enclosedObject = anEnclosedObject;
        this.sortString = aSortString;
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public properties (get)">
    /**
     * Returns the sort string of this object.
     *
     * @return sort string
     */
    public String getSortString() {
        return this.sortString;
    }

    /**
     * Returns the object of type T enclosed in this sort wrapper.
     *
     * @return the enclosed object of type T
     */
    public T getWrappedObject() {
        return this.enclosedObject;
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public methods">
    /**
     * Compares this sort wrapper with the specified other sort wrapper for order based on their sort string attributes.
     * Returns a negative integer, zero, or a positive integer as the sort string of this wrapper is less than, equal
     * to, or greater than the sort string of the other sort wrapper.
     *
     * @param aStringSortWrapper to compare with
     * @return a negative integer, zero, or a positive integer as the sort string of this object is less than, equal to,
     * or greater than that of the given StringSortWrapper object
     * @throws NullPointerException if aStringSortWrapper or its sort string is 'null'
     */
    @Override
    public int compareTo(StringSortWrapper aStringSortWrapper) throws NullPointerException{
        Objects.requireNonNull(aStringSortWrapper, "aStringSortWrapper is 'null'.");
        //Comparing on String level
        return this.sortString.compareTo(aStringSortWrapper.getSortString());
    }

    /**
     * Indicates whether some other object is "equal to" this one. Equality is based on the sort string attributes of
     * this object and the StringSortWrapper instance in anObject.
     *
     * @param anObject to determine equality with
     * @return true if the sort string of this object is equal to that of the StringSortWrapper instance in anObject;
     * false otherwise
     */
    @Override
    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (anObject == null || (anObject.getClass() != this.getClass())) {
            return false;
        }
        StringSortWrapper tmpStringSortWrapper = (StringSortWrapper) anObject;
        return this.hashCode() == tmpStringSortWrapper.hashCode();
    }

    /**
     * Returns a hash code value for this object, based on its sort string attribute.
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        int tmpHash = StringSortWrapper.HASH_SEED;
        tmpHash = StringSortWrapper.HASH_FACTOR_SORT_STRING * tmpHash + Objects.hashCode(this.sortString);
        return tmpHash;
    }
    //</editor-fold>
}