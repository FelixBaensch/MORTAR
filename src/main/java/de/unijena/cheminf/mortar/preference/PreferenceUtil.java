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

package de.unijena.cheminf.mortar.preference;

import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Preference utilities.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public final class PreferenceUtil {
    //<editor-fold defaultstate="collapsed" desc="Private static final class constants">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(PreferenceUtil.class.getName());
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public static methods">
    /**
     * Tests whether the given string would be allowed as name of a new preference object.
     * Internally BasePreference.isValidName(String aName) is used.
     *
     * @param aName the string to test
     * @return true if aName matches the defined pattern for preference names
     */
    public static boolean isValidName(String aName) {
        return BasePreference.isValidName(aName);
    }

    /**
     * Translates a list of JavaFX property objects into their respective preference counterparts, conserving their names
     * and values. A preference container object containing the created preferences is returned. If a property cannot
     * be translated because its name or content are no valid arguments for the respective preference, a warning is logged.
     *
     * @param aPropertiesList list of properties to translate
     * @param aContainerFilePathname the PreferenceContainer object created needs a valid file pathname where it can write
     *                               its persisted form to
     * @return preference container containing all translated preferences
     * @throws NullPointerException if a given argument is null
     * @throws IllegalArgumentException if the given file path name is invalid
     */
    public static PreferenceContainer translateJavaFxPropertiesToPreferences(List<Property> aPropertiesList, String aContainerFilePathname) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(aPropertiesList);
        Objects.requireNonNull(aContainerFilePathname);
        if (!PreferenceContainer.isValidContainerFilePathname(aContainerFilePathname)) {
            throw new IllegalArgumentException("File pathname " + aContainerFilePathname + " is no valid path.");
        }
        if (aPropertiesList.isEmpty()) {
            return new PreferenceContainer(aContainerFilePathname);
        }
        PreferenceContainer tmpContainer = new PreferenceContainer(aContainerFilePathname);
        for (Property tmpProperty : aPropertiesList) {
            try {
                if (Objects.isNull(tmpProperty)) {
                    continue;
                }
                if (tmpProperty instanceof SimpleBooleanProperty){
                    BooleanPreference tmpBooleanPreference = new BooleanPreference(tmpProperty.getName(), ((SimpleBooleanProperty) tmpProperty).get());
                    tmpContainer.add(tmpBooleanPreference);
                } else if (tmpProperty instanceof SimpleIntegerProperty) {
                    SingleIntegerPreference tmpIntPreference = new SingleIntegerPreference(tmpProperty.getName(), ((SimpleIntegerProperty) tmpProperty).get());
                    tmpContainer.add(tmpIntPreference);
                } else if (tmpProperty instanceof SimpleDoubleProperty) {
                    SingleNumberPreference tmpDoublePreference = new SingleNumberPreference(tmpProperty.getName(), ((SimpleDoubleProperty) tmpProperty).get());
                    tmpContainer.add(tmpDoublePreference);
                } else if (tmpProperty instanceof SimpleEnumConstantNameProperty || tmpProperty instanceof SimpleStringProperty) {
                    SingleTermPreference tmpStringPreference = new SingleTermPreference(tmpProperty.getName(), ((SimpleStringProperty) tmpProperty).get());
                    tmpContainer.add(tmpStringPreference);
                } else {
                    PreferenceUtil.LOGGER.log(Level.WARNING, "Unknown property type " + tmpProperty.getClass().getSimpleName() + " was given.");
                }
            } catch (IllegalArgumentException anException) {
                PreferenceUtil.LOGGER.log(Level.WARNING, "Setting translation to property went wrong, exception: " + anException.toString(), anException);
                continue;
            }
        }
        return tmpContainer;
    }
    //</editor-fold>
}