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

package de.unijena.cheminf.mortar.model.util;

import javafx.beans.property.SimpleStringProperty;

import java.util.Objects;

public class SimpleEnumConstantProperty<T extends Enum<T>> extends SimpleStringProperty {
    /**
     *
     */
    private final Class<T> associatedEnum;

    public SimpleEnumConstantProperty(Object bean, String name, String initialValue, Class<T> associatedEnum)
            throws NullPointerException, IllegalArgumentException {
        super(bean, name, initialValue);
        Objects.requireNonNull(associatedEnum, "Given enum class is null.");
        Objects.requireNonNull(initialValue, "Given initial value is null.");
        Objects.requireNonNull(name, "Given name is null.");
        Objects.requireNonNull(bean, "Given bean is null.");
        if (!associatedEnum.isEnum()) {
            throw new IllegalArgumentException("Given class must be an enum.");
        }
        Enum[] tmpEnumConstants = associatedEnum.getEnumConstants();
        if (tmpEnumConstants.length == 0) {
            throw new IllegalArgumentException("The given enum class has no constants declared in it.");
        }
        this.associatedEnum = associatedEnum;
        //throws IllegalArgumentException if initial value is no enum constant name
        T.valueOf(associatedEnum, initialValue);
    }

    public SimpleEnumConstantProperty(Object bean, String name, Class<T> associatedEnum)
            throws NullPointerException, IllegalArgumentException {
        super(bean, name);
        Objects.requireNonNull(associatedEnum, "Given enum class is null.");
        Objects.requireNonNull(name, "Given name is null.");
        Objects.requireNonNull(bean, "Given bean is null.");
        if (!associatedEnum.isEnum()) {
            throw new IllegalArgumentException("Given class must be an enum.");
        }
        Enum[] tmpEnumConstants = associatedEnum.getEnumConstants();
        if (tmpEnumConstants.length == 0) {
            throw new IllegalArgumentException("The given enum class has no constants declared in it.");
        }
        this.associatedEnum = associatedEnum;
    }

    public SimpleEnumConstantProperty(String initialValue, Class<T> associatedEnum)
            throws NullPointerException, IllegalArgumentException {
        super(initialValue);
        Objects.requireNonNull(associatedEnum, "Given enum class is null.");
        Objects.requireNonNull(initialValue, "Given initial value is null.");
        if (!associatedEnum.isEnum()) {
            throw new IllegalArgumentException("Given class must be an enum.");
        }
        Enum[] tmpEnumConstants = associatedEnum.getEnumConstants();
        if (tmpEnumConstants.length == 0) {
            throw new IllegalArgumentException("The given enum class has no constants declared in it.");
        }
        this.associatedEnum = associatedEnum;
        //throws IllegalArgumentException if initial value is no enum constant name
        T.valueOf(associatedEnum, initialValue);
    }

    public SimpleEnumConstantProperty(Class<T> associatedEnum)
            throws NullPointerException, IllegalArgumentException {
        super();
        Objects.requireNonNull(associatedEnum, "Given enum class is null.");
        if (!associatedEnum.isEnum()) {
            throw new IllegalArgumentException("Given class must be an enum.");
        }
        Enum[] tmpEnumConstants = associatedEnum.getEnumConstants();
        if (tmpEnumConstants.length == 0) {
            throw new IllegalArgumentException("The given enum class has no constants declared in it.");
        }
        this.associatedEnum = associatedEnum;
    }

    @Override
    public void set(String newValue) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(newValue, "Given value is null.");
        //throws IllegalArgumentException if initial value is no enum constant name
        T.valueOf(this.associatedEnum, newValue);
        super.set(newValue);
    }

    @Override
    public void setValue(String v) {
        Objects.requireNonNull(v, "Given value is null.");
        //throws IllegalArgumentException if initial value is no enum constant name
        T.valueOf(this.associatedEnum, v);
        super.setValue(v);
    }

    /**
     *
     */
    public Class getAssociatedEnum() {
        return this.associatedEnum;
    }

    /**
     *
     */
    public Enum[] getAssociatedEnumConstants() {
        return this.associatedEnum.getEnumConstants();
    }

    public T getEnumValue() {
        return T.valueOf(this.associatedEnum, this.get());
    }

    public void setEnumValue(T newValue) throws NullPointerException, IllegalArgumentException {
        this.set(newValue.name());
    }
}
