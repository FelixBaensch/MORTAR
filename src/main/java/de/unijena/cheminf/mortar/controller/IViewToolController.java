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
import java.util.Map;

/**
 * Interface that unifies view tools like the histogram view and the overview view.
 *
 * @version 1.0.0.0
 * @author Jonas Schaub
 */
public interface IViewToolController {
    //<editor-fold desc="Public properties">
    /**
     * Returns a list of all available settings represented by properties for the given view tool.
     *
     * @return list of settings represented by properties
     */
    public List<Property> settingsProperties();
    /**
     *
     */
    public String getViewToolNameForDisplay();
    /**
     *
     */
    public Map<String, String> getSettingNameToTooltipTextMap();
    /**
     * Restore all settings of the view tool to their default values.
     */
    public void restoreDefaultSettings();
    /**
     *
     */
    public boolean canBeUsedOnTab(TabNames aTabNameEnumConstant);
    //</editor-fold>
}
