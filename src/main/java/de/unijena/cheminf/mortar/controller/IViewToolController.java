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

package de.unijena.cheminf.mortar.controller;

import javafx.beans.property.Property;

import java.util.List;

/**
 * Interface that unifies view tools like the histogram view and the overview view.
 *
 * @version 1.0.0.0
 * @author Jonas Schaub
 */
public interface IViewToolController {
    //<editor-fold desc="Public properties">
    /**
     * Returns a list of all available settings represented by properties for the given view tool. Differently from the
     * fragmenters, the properties are mostly used for persisting the setting here.
     *
     * @return list of settings represented by properties
     */
    public List<Property> settingsProperties();
    /**
     * Returns a view tool name that can be displayed in the GUI, e.g. in the views menu.
     */
    public String getViewToolNameForDisplay();
    /**
     * Restore all settings of the view tool to their default values.
     */
    public void restoreDefaultSettings();
    /**
     * Specifies whether the view tool can be used on the given tab type, e.g. the histogram view cannot be used
     * on the molecules tab.
     */
    public boolean canBeUsedOnTab(TabNames aTabNameEnumConstant);

    //TODO how to add a getInstance method?
    //TODO add open view tool method?
    //</editor-fold>
}
