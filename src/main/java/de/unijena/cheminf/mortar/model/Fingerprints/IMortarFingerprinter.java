/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2023  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.model.Fingerprints;

import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;
import javafx.beans.property.Property;

import java.util.List;
import java.util.Map;

/**
 * Central interface for implementing wrapper classes for fingerprinter. To make a new fingerprinter
 * available in MORTAR, a class implementing this interface and the fingerprinter logic must be given and added in the
 * {@link FingerprinterService} class.
 * <br>
 * <br><b>Settings</b>:
 * <br>All fingerprinter settings that are supposed to be available in the GUI must be implemented as
 * {@link javafx.beans.property.Property} and returned in a list by the interface method
 * {@link IMortarFingerprinter#settingsProperties()}. Boolean settings must be implemented as
 * {@link javafx.beans.property.SimpleBooleanProperty}, integer settings as
 * {@link javafx.beans.property.SimpleIntegerProperty} etc. For settings where an option must be chosen from multiple
 * available ones, a special Property class is implemented in MORTAR, {@link SimpleEnumConstantNameProperty}. The
 * options to choose from must be implemented as enum constants and the setting property linked to the enum. If changes
 * to the settings done in the GUI must be tested, it is recommended to override the Property.set() method and implement
 * the parameter test logic there. Tooltip texts for the settings must be given in a HashMap with setting (property) names
 * as keys and tooltip text as values (see {@link IMortarFingerprinter#getSettingNameToTooltipTextMap()}).
 * <br>
 * <br>More details can be found in the method documentations of this interface.
 * Examples for how to implement this interface to make a new fingerprinter available, can be found in
 * the class {@link FragmentFingerprinterWrapper}.
 *
 * @author Betuel Sevindik
 * @version 1.0.0.0
 */
public interface IMortarFingerprinter {
    //<editor-fold desc="Public properties">
    /**
     * Returns a list of all available settings represented by properties for the given fingerprinter.
     *
     * @return list of settings represented by properties
     */
    List<Property> settingsProperties();
    //
    /**
     * Returns a map containing descriptive texts (values) for the settings with the given names (keys) to be used as
     * tooltips in the GUI.
     *
     * @return map with tooltip texts
     */
    Map<String, String> getSettingNameToTooltipTextMap();
    //
    /**
     * Returns a string representation of the fingerprinter name, e.g."Fragment Fingerprinter".
     * The given name must be unique among the available fingerprinter!
     *
     * @return name of the fingerprinter
     */
    String getFingerprinterName();
    //
    /**
     * Restore all settings of the fragmenter to their default values.
     *
     * @param aDefaultFingerprintDimensionality the default value of the fingerprint dimensionality is equal to the
     * number of all resulting fragments.
     */
    void restoreDefaultSettings(int aDefaultFingerprintDimensionality);
    //</editor-fold>
}
