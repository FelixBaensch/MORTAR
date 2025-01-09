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

import java.util.Objects;

/**
 * A JavaFX property for wrapping the name of a constant from one specified enum class. The specific enum class is set
 * in the constructor and cannot be changed. All values the property might be given later must represent a constant name
 * (Enum.name()) from this specific enum class.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class SimpleEnumConstantNameProperty extends SimpleEnumConstantPropertyBase {
    //<editor-fold desc="Constructors">
    /**
     * Constructor with all parameters.
     *
     * @param bean the bean of this property
     * @param name the name of this property
     * @param initialValue the initial value of the wrapped value
     * @param associatedEnum the enum class of which a constant name should be wrapped
     * @throws NullPointerException if a parameter is null
     * @throws IllegalArgumentException if the given class is no enum, it contains no constants, or the given initial
     * values does not represent a constant name from the given enum class
     */
    public SimpleEnumConstantNameProperty(Object bean, String name, String initialValue, Class associatedEnum)
            throws NullPointerException, IllegalArgumentException {
        super(bean, name, initialValue, associatedEnum);
        //throws IllegalArgumentException if initial value is no enum constant name
        Enum.valueOf(associatedEnum, initialValue);
    }
    //
    /**
     * Constructor without an initial value.
     *
     * @param bean the bean of this property
     * @param name the name of this property
     * @param associatedEnum the enum class of which a constant name should be wrapped
     * @throws NullPointerException if a parameter is null
     * @throws IllegalArgumentException if the given class is no enum or it contains no constants
     */
    public SimpleEnumConstantNameProperty(Object bean, String name, Class associatedEnum)
            throws NullPointerException, IllegalArgumentException {
        super(bean, name, associatedEnum);
    }
    //
    /**
     * Constructor without bean and property name.
     *
     * @param initialValue the initial value of the wrapped value
     * @param associatedEnum the enum class of which a constant name should be wrapped
     * @throws NullPointerException if a parameter is null
     * @throws IllegalArgumentException if the given class is no enum, it contains no constants, or the given initial
     * values does not represent a constant name from the given enum class
     */
    public SimpleEnumConstantNameProperty(String initialValue, Class associatedEnum)
            throws NullPointerException, IllegalArgumentException {
        super(initialValue, associatedEnum);
        //throws IllegalArgumentException if initial value is no enum constant name
        Enum.valueOf(associatedEnum, initialValue);
    }
    //
    /**
     * Constructor without bean, property name, and initial value. Only the associated enum class must be given.
     *
     * @param associatedEnum the enum class of which a constant name should be wrapped
     * @throws NullPointerException if a parameter is null
     * @throws IllegalArgumentException if the given class is no enum or it contains no constants
     */
    public SimpleEnumConstantNameProperty(Class associatedEnum)
            throws NullPointerException, IllegalArgumentException {
        super(associatedEnum);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties get/set">
    /**
     * Set the wrapped enum constant name. The Enum.name() method must be used to retrieve it from the enum constant
     * object.
     *
     * @param newValue the new value
     * @throws NullPointerException if the parameter is null
     * @throws IllegalArgumentException if the associated enum class has no constant with the name specified in newValue
     */
    @Override
    public void set(String newValue) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(newValue, "Given value is null.");
        //throws IllegalArgumentException if value is no enum constant name
        Enum.valueOf(this.associatedEnum, newValue);
        super.set(newValue);
    }

    /**
     * Set the wrapped enum constant name. The Enum.name() method must be used to retrieve it from the enum constant
     * object.
     *
     * @param v the new value
     * @throws NullPointerException if the parameter is null
     * @throws IllegalArgumentException if the associated enum class has no constant with the name specified in v
     */
    @Override
    public void setValue(String v) throws NullPointerException, IllegalArgumentException {
        this.set(v);
    }

    @Override
    public void setEnumValue(Enum newValue) throws NullPointerException, IllegalArgumentException {
        this.set(newValue.name());
    }

    @Override
    public Enum getEnumValue() {
        return Enum.valueOf(this.associatedEnum, this.get());
    }

    /**
     * Convenience method that returns an array containing the names of all enum constants of the associated enum class.
     * Therefore, this array represents all possible values for the wrapped value of this property.
     *
     * @return all possible values for the value of this property
     */
    public String[] getAssociatedEnumConstantNames() {
        Enum[] tmpConstants = (Enum[]) this.associatedEnum.getEnumConstants();
        String[] tmpNames = new String[tmpConstants.length];
        for (int i = 0; i < tmpConstants.length; i++) {
            tmpNames[i] = tmpConstants[i].name();
        }
        return tmpNames;
    }

    /**
     * Convenience method that returns the enum constant object with the given name from the associated enum class.
     *
     * @param anEnumConstantName name of a constant name of the associated enum class
     * @return the corresponding enum constant object
     * @throws NullPointerException if parameter is null
     * @throws IllegalArgumentException if the associated enum class has no constant with the given name
     */
    public Enum translateNameToEnumConstant(String anEnumConstantName) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(anEnumConstantName, "Given enum constant name is null.");
        //throws IllegalArgumentException if parameter is no enum constant name
        return Enum.valueOf(this.associatedEnum, anEnumConstantName);
    }
    //</editor-fold>
}
