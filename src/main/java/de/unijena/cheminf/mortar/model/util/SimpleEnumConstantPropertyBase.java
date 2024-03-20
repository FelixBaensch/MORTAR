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

import javafx.beans.property.SimpleStringProperty;

import java.util.Objects;

/**
 * Base class for custom JavaFX properties that wrap a value associated with an enum, e.g. its constant name or some
 * defined string(!) property of each constant of the associated enum, e.g. the display names or enums implementing the IDisplayEnum
 * interface. This base class associates the property with an enum but does not impose any restrictions on the wrapped string(!)
 * value.
 */
public abstract class SimpleEnumConstantPropertyBase extends SimpleStringProperty {
    //<editor-fold desc="Protected variables">
    /**
     * The enum class the constant name belongs to.
     */
    protected Class associatedEnum;
    //</editor-fold>
    //
    //<editor-fold desc="Constructors">
    /**
     * Constructor with all parameters.
     *
     * @param bean the bean of this property
     * @param name the name of this property
     * @param initialValue the initial value of the wrapped value
     * @param associatedEnum the enum class of which a constant should be wrapped
     * @throws NullPointerException if a parameter is null
     * @throws IllegalArgumentException if the given class is no enum or it contains no constants
     */
    public SimpleEnumConstantPropertyBase(Object bean, String name, String initialValue, Class associatedEnum)
            throws NullPointerException, IllegalArgumentException {
        super(bean, name, initialValue);
        Objects.requireNonNull(associatedEnum, "Given enum class is null.");
        Objects.requireNonNull(initialValue, "Given initial value is null.");
        Objects.requireNonNull(name, "Given name is null.");
        Objects.requireNonNull(bean, "Given bean is null.");
        if (!associatedEnum.isEnum()) {
            throw new IllegalArgumentException("Given class must be an enum.");
        }
        Enum[] tmpEnumConstants = (Enum[]) associatedEnum.getEnumConstants();
        if (tmpEnumConstants.length == 0) {
            throw new IllegalArgumentException("The given enum class has no constants declared in it.");
        }
        this.associatedEnum = associatedEnum;
    }
    //
    /**
     * Constructor without an initial value.
     *
     * @param bean the bean of this property
     * @param name the name of this property
     * @param associatedEnum the enum class of which a constant should be wrapped
     * @throws NullPointerException if a parameter is null
     * @throws IllegalArgumentException if the given class is no enum or it contains no constants
     */
    public SimpleEnumConstantPropertyBase(Object bean, String name, Class associatedEnum)
            throws NullPointerException, IllegalArgumentException {
        super(bean, name);
        Objects.requireNonNull(associatedEnum, "Given enum class is null.");
        Objects.requireNonNull(name, "Given name is null.");
        Objects.requireNonNull(bean, "Given bean is null.");
        if (!associatedEnum.isEnum()) {
            throw new IllegalArgumentException("Given class must be an enum.");
        }
        Enum[] tmpEnumConstants = (Enum[]) associatedEnum.getEnumConstants();
        if (tmpEnumConstants.length == 0) {
            throw new IllegalArgumentException("The given enum class has no constants declared in it.");
        }
        this.associatedEnum = associatedEnum;
    }
    //
    /**
     * Constructor without bean and property name.
     *
     * @param initialValue the initial value of the wrapped value
     * @param associatedEnum the enum class of which a constant should be wrapped
     * @throws NullPointerException if a parameter is null
     * @throws IllegalArgumentException if the given class is no enum, or it contains no constants
     */
    public SimpleEnumConstantPropertyBase(String initialValue, Class associatedEnum)
            throws NullPointerException, IllegalArgumentException {
        super(initialValue);
        Objects.requireNonNull(associatedEnum, "Given enum class is null.");
        Objects.requireNonNull(initialValue, "Given initial value is null.");
        if (!associatedEnum.isEnum()) {
            throw new IllegalArgumentException("Given class must be an enum.");
        }
        Enum[] tmpEnumConstants = (Enum[]) associatedEnum.getEnumConstants();
        if (tmpEnumConstants.length == 0) {
            throw new IllegalArgumentException("The given enum class has no constants declared in it.");
        }
        this.associatedEnum = associatedEnum;
    }
    //
    /**
     * Constructor without bean, property name, and initial value. Only the associated enum class must be given.
     *
     * @param associatedEnum the enum class of which a constant should be wrapped
     * @throws NullPointerException if a parameter is null
     * @throws IllegalArgumentException if the given class is no enum or it contains no constants
     */
    public SimpleEnumConstantPropertyBase(Class associatedEnum)
            throws NullPointerException, IllegalArgumentException {
        super();
        Objects.requireNonNull(associatedEnum, "Given enum class is null.");
        if (!associatedEnum.isEnum()) {
            throw new IllegalArgumentException("Given class must be an enum.");
        }
        Enum[] tmpEnumConstants = (Enum[]) associatedEnum.getEnumConstants();
        if (tmpEnumConstants.length == 0) {
            throw new IllegalArgumentException("The given enum class has no constants declared in it.");
        }
        this.associatedEnum = associatedEnum;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties get/set">
    /**
     * Returns the enum class of which a constant name is wrapped in this property.
     *
     * @return the associated enum class
     */
    public Class getAssociatedEnum() {
        return this.associatedEnum;
    }
    //
    /**
     * Convenience method that returns an array containing all enum constants of the associated enum class.
     *
     * @return all constants of the associated enum
     */
    public Enum[] getAssociatedEnumConstants() {
        return (Enum[]) this.associatedEnum.getEnumConstants();
    }
    //
    /**
     * Convenience method that accepts an enum constant object directly as new value for this property. The wrapped value
     * associated with the enum in this property is extracted internally.
     *
     * @param newValue the new value will be extracted from the given enum constant object
     * @throws NullPointerException if the parameter is null
     * @throws IllegalArgumentException if the given enum constant object does not belong to the associated enum class of
     * this property
     */
    public abstract void setEnumValue(Enum newValue) throws NullPointerException, IllegalArgumentException;
    //
    /**
     * Convenience method that returns an enum constant object directly instead of its associated wrapped value.
     *
     * @return the enum constant object whose property is currently wrapped in this instance
     */
    public abstract Enum getEnumValue();
    //</editor-fold>
}
