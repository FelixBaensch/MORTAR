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

import java.util.Objects;

/**
 * A generic sort wrapper that can be compared to another object (e.g. for sorting) via a specified sort string.
 *
 * @param <T> the enclosed object
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class StringSortWrapper<T> implements Comparable<StringSortWrapper<T>>{
    //<editor-fold defaultstate="collapsed" desc="Private static final class constants">
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
        StringSortWrapper<T> tmpStringSortWrapper = (StringSortWrapper<T>) anObject;
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
