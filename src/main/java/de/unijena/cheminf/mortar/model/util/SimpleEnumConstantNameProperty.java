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

public class SimpleEnumConstantNameProperty extends SimpleStringProperty {
    /**
     *
     */
    private final Class associatedEnum;

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

    @Override
    public void set(String newValue) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(newValue, "Given value is null.");
        //throws IllegalArgumentException if initial value is no enum constant name
        Enum.valueOf(this.associatedEnum, newValue);
        super.set(newValue);
    }

    @Override
    public void setValue(String v) {
        Objects.requireNonNull(v, "Given value is null.");
        //throws IllegalArgumentException if initial value is no enum constant name
        Enum.valueOf(this.associatedEnum, v);
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
        return (Enum[]) this.associatedEnum.getEnumConstants();
    }

    public Enum getEnumValue() {
        return Enum.valueOf(this.associatedEnum, this.get());
    }

    public void setEnumValue(Enum newValue) throws NullPointerException, IllegalArgumentException {
        this.set(newValue.name());
    }

    public String[] getAssociatedEnumConstantnames() {
        Enum[] tmpConstants = (Enum[]) this.associatedEnum.getEnumConstants();
        String[] tmpNames = new String[tmpConstants.length];
        for (int i = 0; i < tmpConstants.length; i++) {
            tmpNames[i] = tmpConstants[i].name();
        }
        return tmpNames;
    }
}
