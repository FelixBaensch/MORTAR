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

import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.CollectionUtil;
import de.unijena.cheminf.mortar.model.util.IDisplayEnum;
import de.unijena.cheminf.mortar.model.util.SimpleIDisplayEnumConstantProperty;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

import java.util.HashSet;
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
    //<editor-fold defaultstate="collapsed" desc="Private constructor">
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
    public static PreferenceContainer translateJavaFxPropertiesToPreferences(List<Property<?>> aPropertiesList, String aContainerFilePathname) throws NullPointerException, IllegalArgumentException {
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
            if (tmpProperty == null) {
                continue;
            }
            try {
                switch (tmpProperty) {
                    case SimpleBooleanProperty simpleBooleanProperty -> {
                        BooleanPreference tmpBooleanPreference = new BooleanPreference(simpleBooleanProperty.getName(),
                                simpleBooleanProperty.get());
                        tmpContainer.add(tmpBooleanPreference);
                    }
                    case SimpleIntegerProperty simpleIntegerProperty -> {
                        SingleIntegerPreference tmpIntPreference = new SingleIntegerPreference(simpleIntegerProperty.getName(),
                                simpleIntegerProperty.get());
                        tmpContainer.add(tmpIntPreference);
                    }
                    case SimpleDoubleProperty simpleDoubleProperty -> {
                        SingleNumberPreference tmpDoublePreference = new SingleNumberPreference(simpleDoubleProperty.getName(),
                                simpleDoubleProperty.get());
                        tmpContainer.add(tmpDoublePreference);
                    }
                    case SimpleIDisplayEnumConstantProperty simpleIDisplayEnumConstantProperty -> {
                        SingleTermPreference tmpStringPreference = new SingleTermPreference(simpleIDisplayEnumConstantProperty.getName(),
                                ((Enum) simpleIDisplayEnumConstantProperty.get()).name());
                        tmpContainer.add(tmpStringPreference);
                    }
                    case SimpleStringProperty simpleStringProperty -> {
                        // includes SimpleEnumConstantNameProperty
                        SingleTermPreference tmpStringPreference = new SingleTermPreference(simpleStringProperty.getName(),
                                simpleStringProperty.get());
                        tmpContainer.add(tmpStringPreference);
                    }
                    default ->
                            PreferenceUtil.LOGGER.log(Level.WARNING, "Unknown property type {0} was given.",
                                    tmpProperty.getClass().getSimpleName());
                }
            } catch (IllegalArgumentException anException) {
                PreferenceUtil.LOGGER.log(Level.WARNING,
                        String.format("Setting translation to property went wrong, exception: %s", anException.toString()),
                        anException);
                //continue;
            }
        }
        return tmpContainer;
    }
    //
    /**
     * Sets the values of the given JFX properties (settings) according to the preferences in the given container with the same name.
     * If no matching preference for a given property is found, the value will remain in its default setting. Intended use is to update
     * JFX properties that represent settings based on de-persisted MORTAR preference objects. If no preference with the same name
     * can be found for a given property, the case is logged with the property name and the property remains in the same
     * value.
     *
     * @param aPropertiesList properties to update
     * @param aPreferenceContainer preferences to take the values from
     */
    public static void updatePropertiesFromPreferences(List<Property<?>> aPropertiesList, PreferenceContainer aPreferenceContainer) {
        Objects.requireNonNull(aPropertiesList, "Given JFX properties list is null");
        Objects.requireNonNull(aPreferenceContainer, "Given preference container is null");
        if (aPreferenceContainer.isEmpty() || aPropertiesList.isEmpty()) {
            PreferenceUtil.LOGGER.log(Level.WARNING, "Preference container or properties list is empty.");
            return;
        }
        for (Property<?> tmpSettingProperty : aPropertiesList) {
            String tmpPropertyName = tmpSettingProperty.getName();
            if (aPreferenceContainer.containsPreferenceName(tmpPropertyName)) {
                IPreference[] tmpPreferences = aPreferenceContainer.getPreferences(tmpPropertyName);
                try {
                    switch (tmpSettingProperty) {
                        case SimpleBooleanProperty tmpSimpleBooleanProperty -> {
                            BooleanPreference tmpBooleanPreference = (BooleanPreference) tmpPreferences[0];
                            tmpSimpleBooleanProperty.setValue(tmpBooleanPreference.getContent());
                        }
                        case SimpleIntegerProperty tmpSimpleIntegerProperty -> {
                            SingleIntegerPreference tmpIntPreference = (SingleIntegerPreference) tmpPreferences[0];
                            tmpSimpleIntegerProperty.setValue(tmpIntPreference.getContent());
                        }
                        case SimpleDoubleProperty tmpSimpleDoubleProperty -> {
                            SingleNumberPreference tmpDoublePreference = (SingleNumberPreference) tmpPreferences[0];
                            tmpSimpleDoubleProperty.setValue(tmpDoublePreference.getContent());
                        }
                        case SimpleIDisplayEnumConstantProperty tmpSimpleIDisplayEnumConstantProperty -> {
                            SingleTermPreference tmpStringPreference = (SingleTermPreference) tmpPreferences[0];
                            tmpSimpleIDisplayEnumConstantProperty.setValue((IDisplayEnum) Enum.valueOf(tmpSimpleIDisplayEnumConstantProperty.getAssociatedEnum(), tmpStringPreference.getContent()));
                        }
                        case SimpleStringProperty tmpSimpleStringProperty -> {
                            //includes SimpleEnumConstantNameProperty since it extends tmpSimpleStringProperty
                            SingleTermPreference tmpStringPreference = (SingleTermPreference) tmpPreferences[0];
                            tmpSimpleStringProperty.setValue(tmpStringPreference.getContent());
                        }
                        default -> {
                            //setting will remain in default
                            PreferenceUtil.LOGGER.log(Level.WARNING, "Setting {0} is of unknown type.", tmpPropertyName);
                        }
                    }
                } catch (ClassCastException | IllegalArgumentException anException) {
                    //setting will remain in default
                    PreferenceUtil.LOGGER.log(Level.WARNING, anException.toString(), anException);
                }
            } else {
                //setting will remain in default
                PreferenceUtil.LOGGER.log(Level.WARNING, "No persisted settings for {0} available.", tmpPropertyName);
            }
        }
    }
    //
    /**
     * Checks the given list of JavaFx property objects for whether they adhere with the restrictions imposed by the MORTAR
     * preferences classes that are used for persisting the properties. Property names must be singletons within the given list.
     * Property names and values must adhere to the preference input restrictions. Property values are only tested for
     * their current state, not the entire possible input space! It is therefore recommended to call this method again before
     * attempting to persist the properties as preferences. An UnsupportedOperationException is thrown if something does
     * not adhere to the given requirements.
     *
     * @param aPropertiesList the list of properties, e.g. the settings of a specific fragmenter class
     * @throws UnsupportedOperationException if sth does not fit the restrictions
     */
    public static void checkPropertiesForPreferenceRestrictions(List<Property<?>> aPropertiesList) throws UnsupportedOperationException {
        int tmpSettingNamesSetInitCapacity = CollectionUtil.calculateInitialHashCollectionCapacity(aPropertiesList.size(),
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        HashSet<String> tmpSettingNames = new HashSet<>(tmpSettingNamesSetInitCapacity,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        for (Property<?> tmpSetting : aPropertiesList) {
            if (!PreferenceUtil.isValidName(tmpSetting.getName())) {
                throw new UnsupportedOperationException("Setting " + tmpSetting.getName() + " has an invalid name.");
            }
            if (tmpSettingNames.contains(tmpSetting.getName())) {
                throw new UnsupportedOperationException("Setting name " + tmpSetting.getName() + " is used multiple times.");
            } else {
                tmpSettingNames.add(tmpSetting.getName());
            }
            switch (tmpSetting) {
                case SimpleBooleanProperty simpleBooleanProperty -> {
                    //nothing to do here, booleans cannot have invalid values
                }
                case SimpleIntegerProperty simpleIntegerProperty -> {
                    if (!SingleIntegerPreference.isValidContent(Integer.toString(simpleIntegerProperty.get()))) {
                        throw new UnsupportedOperationException("Setting value " + simpleIntegerProperty.get()
                                + " of setting name " + tmpSetting.getName() + " is invalid.");
                    }
                }
                case SimpleDoubleProperty simpleDoubleProperty -> {
                    if (!SingleNumberPreference.isValidContent(simpleDoubleProperty.get())) {
                        throw new UnsupportedOperationException("Setting value " + simpleDoubleProperty.get()
                                + " of setting name " + tmpSetting.getName() + " is invalid.");
                    }
                }
                case SimpleIDisplayEnumConstantProperty simpleIDisplayEnumConstantProperty -> {
                    if (!SingleTermPreference.isValidContent(((Enum)simpleIDisplayEnumConstantProperty.get()).name())) {
                        throw new UnsupportedOperationException("Setting value " + simpleIDisplayEnumConstantProperty.get()
                                + " of setting name " + tmpSetting.getName() + " is invalid.");
                    }
                }
                case SimpleStringProperty simpleStringProperty -> {
                    //includes SimpleEnumConstantNameProperty
                    if (!SingleTermPreference.isValidContent(simpleStringProperty.get())) {
                        throw new UnsupportedOperationException("Setting value " + simpleStringProperty.get()
                                + " of setting name " + tmpSetting.getName() + " is invalid.");
                    }
                }
                default -> {
                    throw new UnsupportedOperationException("Setting " + tmpSetting.getName() + " is of an invalid type.");
                }
            }
        }
    }
    //</editor-fold>
}
