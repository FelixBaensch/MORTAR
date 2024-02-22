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
    //<editor-fold desc="Protected constructor">
    /**
     * Private parameter-less constructor.
     * Introduced because javadoc build complained about classes without declared default constructor.
     */
    private PreferenceUtil() {
    }
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
