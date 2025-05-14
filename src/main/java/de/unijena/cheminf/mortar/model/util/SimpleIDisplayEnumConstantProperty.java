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

package de.unijena.cheminf.mortar.model.util;

import javafx.beans.property.SimpleObjectProperty;

import java.util.Objects;

/**
 * A JavaFX property for wrapping a constant from one specified enum class that implements the IDisplayEnum interface.
 * The specific enum class is set in the constructor and cannot be changed. All values the property might be given later
 * must represent a constant from this specific enum class.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class SimpleIDisplayEnumConstantProperty extends SimpleObjectProperty<IDisplayEnum> {
    //<editor-fold desc="Protected variables">
    /**
     * The enum class the constant belongs to.
     */
    protected Class associatedEnum;
    //<editor-fold desc="Constructors">
    /**
     * Constructor with all parameters.
     *
     * @param bean the bean of this property
     * @param name the name of this property
     * @param initialValue the initial value of the wrapped value
     * @param associatedEnum the enum class of which a constant should be wrapped; it MUST implement IDisplayEnum
     * @throws NullPointerException if a parameter is null
     * @throws IllegalArgumentException if the given class is no enum, it contains no constants, it does not implement IDisplayEnum,
     * or the given initial value does not represent a constant from the given enum class
     */
    public SimpleIDisplayEnumConstantProperty(Object bean, String name, IDisplayEnum initialValue, Class associatedEnum)
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
        if (!(tmpEnumConstants[0] instanceof IDisplayEnum)) {
            throw new IllegalArgumentException("Given enum class does not implement IDisplayEnum: " + associatedEnum);
        }
        //throws IllegalArgumentException if the specified enum class has no constant with the specified name, or the specified class object does not represent an enum class
        Enum.valueOf(associatedEnum, ((Enum) initialValue).name());
        this.associatedEnum = associatedEnum;
    }
    //
    /**
     * Constructor without an initial value.
     *
     * @param bean the bean of this property
     * @param name the name of this property
     * @param associatedEnum the enum class of which a constant should be wrapped; it MUST implement IDisplayEnum
     * @throws NullPointerException if a parameter is null
     * @throws IllegalArgumentException if the given class is no enum, it does not implement IDisplayEnum, or it contains no constants
     */
    public SimpleIDisplayEnumConstantProperty(Object bean, String name, Class associatedEnum)
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
        if (!(tmpEnumConstants[0] instanceof IDisplayEnum)) {
            throw new IllegalArgumentException("Given enum class does not implement IDisplayEnum: " + associatedEnum);
        }
        this.associatedEnum = associatedEnum;
    }
    //
    /**
     * Constructor without bean and property name.
     *
     * @param initialValue the initial value of the wrapped value
     * @param associatedEnum the enum class of which a constant should be wrapped; it MUST implement IDisplayEnum
     * @throws NullPointerException if a parameter is null
     * @throws IllegalArgumentException if the given class is no enum, it does not implement IDisplayEnum, it contains no constants, or the given initial
     * values does not represent a constant from the given enum class
     */
    public SimpleIDisplayEnumConstantProperty(IDisplayEnum initialValue, Class associatedEnum)
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
        if (!(tmpEnumConstants[0] instanceof IDisplayEnum)) {
            throw new IllegalArgumentException("Given enum class does not implement IDisplayEnum: " + associatedEnum);
        }
        //throws IllegalArgumentException if the specified enum class has no constant with the specified name, or the specified class object does not represent an enum class
        Enum.valueOf(associatedEnum, ((Enum) initialValue).name());
        this.associatedEnum = associatedEnum;
    }
    //
    /**
     * Constructor without bean, property name, and initial value. Only the associated enum class must be given.
     *
     * @param associatedEnum the enum class of which a constant should be wrapped; it MUST implement IDisplayEnum
     * @throws NullPointerException if a parameter is null
     * @throws IllegalArgumentException if the given class is no enum, it does not implement IDisplayEnum, or it contains no constants
     */
    public SimpleIDisplayEnumConstantProperty(Class associatedEnum)
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
        if (!(tmpEnumConstants[0] instanceof IDisplayEnum)) {
            throw new IllegalArgumentException("Given enum class does not implement IDisplayEnum: " + associatedEnum);
        }
        this.associatedEnum = associatedEnum;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties get/set">
    /**
     * Set the wrapped enum constant.
     *
     * @param newValue the new value
     * @throws NullPointerException if the parameter is null
     * @throws IllegalArgumentException if the associated enum class has no constant specified in newValue
     */
    @Override
    public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(newValue, "Given value is null.");
        //throws IllegalArgumentException if the specified enum class has no constant with the specified name
        Enum.valueOf(this.associatedEnum, ((Enum) newValue).name());
        super.set(newValue);
    }

    /**
     * Set the wrapped enum constant.
     *
     * @param v the new value
     * @throws NullPointerException if the parameter is null
     * @throws IllegalArgumentException if the associated enum class has no constant specified in v
     */
    @Override
    public void setValue(IDisplayEnum v) throws NullPointerException, IllegalArgumentException {
        this.set(v);
    }

    /**
     * Returns an array containing the display names of all enum constants of the associated enum class.
     *
     * @return all associated enum constants display names
     */
    public String[] getAssociatedEnumConstantDisplayNames() {
        IDisplayEnum[] tmpConstants = (IDisplayEnum[]) this.associatedEnum.getEnumConstants();
        String[] tmpNames = new String[tmpConstants.length];
        for (int i = 0; i < tmpConstants.length; i++) {
            tmpNames[i] = tmpConstants[i].getDisplayName();
        }
        return tmpNames;
    }

    /**
     * Returns the enum constant object with the given display name from the associated enum class.
     *
     * @param anEnumConstantDisplayName display name of a constant of the associated enum class
     * @return the corresponding enum constant object
     * @throws NullPointerException if parameter is null
     * @throws IllegalArgumentException if the associated enum class has no constant with the given display name
     */
    public Enum translateDisplayNameToEnumConstant(String anEnumConstantDisplayName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anEnumConstantDisplayName, "Given enum constant name is null.");
        for (IDisplayEnum tmpConstant : (IDisplayEnum[]) this.getAssociatedEnumConstants()) {
            if (tmpConstant.getDisplayName().equals(anEnumConstantDisplayName)) {
                return (Enum) tmpConstant;
            }
        }
        throw new IllegalArgumentException("Given string does not represent an enum constant display name: " + anEnumConstantDisplayName);
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
    /**
     * Returns the enum class of which a constant is wrapped in this property.
     *
     * @return the associated enum class
     */
    public Class getAssociatedEnum() {
        return this.associatedEnum;
    }
    //</editor-fold>
}
