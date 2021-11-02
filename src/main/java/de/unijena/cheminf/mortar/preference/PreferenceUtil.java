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

package de.unijena.cheminf.mortar.preference;

import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;
import javafx.beans.property.*;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Preference utilities.
 *
 * @author Jonas Schaub
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

    //TODO: There are several restrictions unchecked here, e.g. whether the property names are valid preference names or whether the string values are valid values of SingleTermPreference objects
    /**
     *
     */
    public static PreferenceContainer translateJavaFxPropertiesToPreferences(List<Property> aPropertiesList, String aContainerFilePathname) throws NullPointerException, IllegalArgumentException {
        Objects.requireNonNull(aPropertiesList);
        if (!PreferenceContainer.isValidContainerFilePathname(aContainerFilePathname)) {
            throw new IllegalArgumentException("File pathname " + aContainerFilePathname + " is no valid path.");
        }
        if (aPropertiesList.isEmpty()) {
            return new PreferenceContainer(aContainerFilePathname);
        }
        PreferenceContainer tmpContainer = new PreferenceContainer(aContainerFilePathname);
        for (Property tmpProperty : aPropertiesList) {
            try {
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
                    throw new IllegalArgumentException("Unknown property type was given.");
                }
            } catch (IllegalArgumentException anException) {
                PreferenceUtil.LOGGER.log(Level.WARNING, anException.toString(), anException);
                continue;
            }
        }
        return tmpContainer;
    }
    //</editor-fold>
}