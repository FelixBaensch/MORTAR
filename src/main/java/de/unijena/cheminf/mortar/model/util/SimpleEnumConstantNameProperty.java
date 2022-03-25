/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2022  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

import javafx.beans.property.SimpleStringProperty;

import java.util.Objects;

/**
 * A JavaFX property for wrapping the name of a constant from one specified enum class. The specific enum class is set
 * in the constructor and cannot be changed. All values the property might be given later must represent a constant name
 * (Enum.name()) from this specific enum class.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class SimpleEnumConstantNameProperty extends SimpleStringProperty {
    //<editor-fold desc="Private final variables">
    /**
     * The enum class the constant name belongs to.
     */
    private final Class associatedEnum;
    //</editor-fold>
    //
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
        //throws IllegalArgumentException if initial value is no enum constant name
        Enum.valueOf(associatedEnum, initialValue);
    }

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

    /**
     * Constructor without bean and name.
     *
     * @param initialValue the initial value of the wrapped value
     * @param associatedEnum the enum class of which a constant name should be wrapped
     * @throws NullPointerException if a parameter is null
     * @throws IllegalArgumentException if the given class is no enum, it contains no constants, or the given initial
     * values does not represent a constant name from the given enum class
     */
    public SimpleEnumConstantNameProperty(String initialValue, Class associatedEnum)
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
        //throws IllegalArgumentException if initial value is no enum constant name
        Enum.valueOf(associatedEnum, initialValue);
    }

    /**
     * Constructor without bean, name, and initial value. Only the associated enum class must be given.
     *
     * @param associatedEnum the enum class of which a constant name should be wrapped
     * @throws NullPointerException if a parameter is null
     * @throws IllegalArgumentException if the given class is no enum or it contains no constants
     */
    public SimpleEnumConstantNameProperty(Class associatedEnum)
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
     * @throws IllegalArgumentException if the associated enum class has no constant with the name specified in newValue
     */
    @Override
    public void setValue(String v) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(v, "Given value is null.");
        //throws IllegalArgumentException if value is no enum constant name
        Enum.valueOf(this.associatedEnum, v);
        super.setValue(v);
    }

    /**
     * Convenience method that accepts an enum constant object directly instead of its name as new value for this property.
     *
     * @param newValue the new value will be the name of the given enum constant object
     * @throws NullPointerException if the parameter is null
     * @throws IllegalArgumentException if the given enum constant object does not belong to the associated enum class of
     * this property
     */
    public void setEnumValue(Enum newValue) throws NullPointerException, IllegalArgumentException {
        this.set(newValue.name());
    }

    /**
     * Convenience method that returns an enum constant object directly instead of its name for the wrapped value.
     *
     * @return the enum constant object whose name is currently wrapped in this property
     */
    public Enum getEnumValue() {
        return Enum.valueOf(this.associatedEnum, this.get());
    }

    /**
     * Returns the enum class of which a constant name is wrapped in this property.
     *
     * @return the associated enum class
     */
    public Class getAssociatedEnum() {
        return this.associatedEnum;
    }

    /**
     * Convenience method that returns an array containing all enum constants of the associated enum class.
     *
     * @return all constants of the associated enum
     */
    public Enum[] getAssociatedEnumConstants() {
        return (Enum[]) this.associatedEnum.getEnumConstants();
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
