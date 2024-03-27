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
     * fragmenters, the properties are mostly used for persisting the setting here and *not* for actually configuring the
     * settings via the Property objects.
     *
     * @return list of settings represented by properties
     */
    public List<Property<?>> settingsProperties();
    /**
     * Returns a view tool name that can be displayed in the GUI, e.g. in the views menu.
     *
     * @return language-specific name for the view tool
     */
    public String getViewToolNameForDisplay();
    /**
     * Restore all settings of the view tool to their default values.
     */
    public void restoreDefaultSettings();
    /**
     * Specifies whether the view tool can be used on the given tab type, e.g. the histogram view cannot be used
     * on the molecules tab.
     *
     * @param aTabNameEnumConstant enum constant denoting the currently active tab in the GUI
     * @return true if the view tool can be opened when this specific tab is active
     */
    public boolean canBeUsedOnTab(TabNames aTabNameEnumConstant);
    //</editor-fold>
}
