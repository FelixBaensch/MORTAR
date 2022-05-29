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

package de.unijena.cheminf.mortar.gui.util;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * GUI definitions
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public final class GuiDefinitions {

    /**
     * Value for the GUI insets
     */
    public static final double GUI_INSETS_VALUE = 10.0;
    /**
     * Value for the GUI spacing
     */
    public static final double GUI_SPACING_VALUE = 5.0;
    /**
     * Value for the width of the main view
     */
    public static final double GUI_MAIN_VIEW_WIDTH_VALUE = 1024.0;
    /**
     * Value for the height of the main view
     */
    public static final double GUI_MAIN_VIEW_HEIGHT_VALUE = 768.0;
    /**
     * Value for the width of a button
     */
    public static final double GUI_BUTTON_WIDTH_VALUE = 75.0;
    /**
     * Value for the height of a button
     */
    public static final double GUI_BUTTON_HEIGHT_VALUE = 25.0;
    /**
     * Value for the distance between the buttons
     */
    public static final double GUI_BUTTON_SPACING_VALUE = 5.0;
    /**
     * Value for the Button insets
     */
    public static final double GUI_BUTTON_INSETS_VALUE = 13.0;
    /**
     * Value for the height of status bar
     */
    public static final double GUI_STATUSBAR_HEIGHT_VALUE = 25.0;
    /**
     * Value for the height of control containers
     */
    public static final double GUI_CONTROL_CONTAINER_HEIGHT = 50.0;
    /**
     * Value for the preference width of a text field
     */
    public static final double GUI_TEXT_FIELD_PREF_WIDTH_VALUE = 50.0;
    /**
     * Value for the maximum width of a text field
     */
    public static final double GUI_SETTINGS_TEXT_FIELD_MAX_WIDTH_VALUE = 70.0;
    /**
     *  Selection column width
     */
    public static final double GUI_MOLECULES_TAB_TABLEVIEW_SELECTION_COLUMN_WIDTH = 40.0;
    /**
     * Height of the table view header
     */
    public static final double GUI_TABLE_VIEW_HEADER_HEIGHT = 24.0;
    /**
     * Min height for a structure image of a molecule shown in gui
     */
    public static final double GUI_STRUCTURE_IMAGE_MIN_HEIGHT = 50.0;
    /**
     * Height of the control panel of pagination
     */
    public static final double GUI_PAGINATION_CONTROL_PANEL_HEIGHT = 45.0;
    /**
     * Max width of tooltip
     */
    public static final double GUI_TOOLTIP_MAX_WIDTH = 500.0;
    /**
     * KeyCodeCombination for Control + C
     */
    public static final KeyCodeCombination KEY_CODE_COPY = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN);
    /**
     * KeyCodeCombination to navigate to last page of pagination
     */
    public static final KeyCodeCombination KEY_CODE_LAST_PAGE = new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.CONTROL_DOWN);
    /**
     * KeyCodeCombination to navigate to first page of pagination
     */
    public static final KeyCodeCombination KEY_CODE_FIRST_PAGE = new KeyCodeCombination(KeyCode.LEFT, KeyCombination.CONTROL_DOWN);
    /**
     * Value for the width of the StackPane which should display the structure of fragments
     */
    public static final double GUI_IMAGE_PANE_WIDTH = 250.0;
    /**
     * Value for the height of the StackPane which should display the structure of fragments
     */
    public static final double GUI_IMAGE_PANE_HEIGHT = 150.0;
    /**
     * Value for the width of the textfield
     */
    public static final double GUI_TEXT_FIELD_WIDTH = 80.0;
}
