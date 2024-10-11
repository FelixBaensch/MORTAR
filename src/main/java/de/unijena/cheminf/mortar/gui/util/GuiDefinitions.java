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

package de.unijena.cheminf.mortar.gui.util;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/**
 * GUI definitions.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public final class GuiDefinitions {
    //<editor-fold desc="Private constructor">
    /**
     * Private parameter-less constructor.
     * Introduced because javadoc build complained about classes without declared default constructor.
     */
    private GuiDefinitions() {
    }
    //</editor-fold>
    /**
     * Value for the GUI insets.
     */
    public static final double GUI_INSETS_VALUE = 10.0;
    /**
     * Value for the GUI spacing.
     */
    public static final double GUI_SPACING_VALUE = 5.0;
    /**
     * Value for the width of the main view.
     */
    public static final double GUI_MAIN_VIEW_WIDTH_VALUE = 1024.0;
    /**
     * Value for the height of the main view.
     */
    public static final double GUI_MAIN_VIEW_HEIGHT_VALUE = 768.0;
    /**
     * Value for the width of a button.
     */
    public static final double GUI_BUTTON_WIDTH_VALUE = 75.0;
    /**
     * Value for the height of a button.
     */
    public static final double GUI_BUTTON_HEIGHT_VALUE = 25.0;
    /**
     * Value for the width of buttons in the pipeline settings view.
     */
    public static final double GUI_PIPELINE_SETTINGS_VIEW_BUTTON_WIDTH_VALUE = 50.0;
    /**
     * Value for the distance between the buttons.
     */
    public static final double GUI_BUTTON_SPACING_VALUE = 5.0;
    /**
     * Value for the Button insets.
     */
    public static final double GUI_BUTTON_INSETS_VALUE = 13.0;
    /**
     * Value for the height of status bar.
     */
    public static final double GUI_STATUSBAR_HEIGHT_VALUE = 25.0;
    /**
     * Value for the height of control containers.
     */
    public static final double GUI_CONTROL_CONTAINER_HEIGHT = 50.0;
    /**
     * Value for the preference width of a text field.
     */
    public static final double GUI_TEXT_FIELD_PREF_WIDTH_VALUE = 60.0;
    /**
     * Value for the maximum width of a text field.
     */
    public static final double GUI_SETTINGS_TEXT_FIELD_MAX_WIDTH_VALUE = 120.0;
    /**
     * Value for the preferred width of a combo box in the settings view.
     */
    public static final double GUI_SETTING_COMBO_BOX_PREF_WIDTH_VALUE = 180.0;
    /**
     * Value for the maximum width of a combo box in the settings view.
     */
    public static final double GUI_SETTING_COMBO_BOX_MAX_WIDTH_VALUE = 210.0;
    /**
     * Selection column width.
     */
    public static final double GUI_MOLECULES_TAB_TABLEVIEW_SELECTION_COLUMN_WIDTH = 40.0;
    /**
     * Height of the table view header.
     */
    public static final double GUI_TABLE_VIEW_HEADER_HEIGHT = 24.0;
    /**
     * Height of a (horizontal) scroll bar.
     */
    public static final double GUI_SCROLL_BAR_HEIGHT = 14.0;
    /**
     * Min height for a structure image of a molecule shown in gui.
     */
    public static final double GUI_STRUCTURE_IMAGE_MIN_HEIGHT = 50.0;
    /**
     * Height of the control panel of pagination.
     */
    public static final double GUI_PAGINATION_CONTROL_PANEL_HEIGHT = 45.0;
    /**
     * Width of the third column of gid panes used to align nodes.
     */
    public static final double GUI_GRIDPANE_FOR_NODE_ALIGNMENT_THIRD_COL_WIDTH = 200.0;
    /**
     * Max width for tooltips.
     */
    public static final double GUI_TOOLTIP_MAX_WIDTH = 500.0;
    /**
     * Show duration for tooltips.
     */
    public static final double GUI_TOOLTIP_SHOW_DURATION = 10.0;
    /**
     * Width of an image copied to clipboard.
     */
    public static final double GUI_COPY_IMAGE_IMAGE_WIDTH = 1500.0;
    /**
     * Height of an image copied to clipboard.
     */
    public static final double GUI_COPY_IMAGE_IMAGE_HEIGHT = 1000.0;
    /**
     * KeyCodeCombination for Control + C.
     */
    public static final KeyCodeCombination KEY_CODE_COPY = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN);
    /**
     * KeyCodeCombination to navigate to last page of pagination.
     */
    public static final KeyCodeCombination KEY_CODE_LAST_PAGE = new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.CONTROL_DOWN);
    /**
     * KeyCodeCombination to navigate to first page of pagination.
     */
    public static final KeyCodeCombination KEY_CODE_FIRST_PAGE = new KeyCodeCombination(KeyCode.LEFT, KeyCombination.CONTROL_DOWN);
    /**
     * Value for the delay of a second click to be registered as double-click (in ms).
     */
    public static final int DOUBLE_CLICK_DELAY = 250;
    /**
     * Value for the not scrollable histogram.
     */
    public static final double GUI_NOT_SCROLLABLE_HEIGHT = 651.0;
    /**
     * Value for the width of the textField.
     */
    public static final double GUI_TEXT_FIELD_WIDTH = 50.0;
    /**
     * Width for the text field in the pagination control box.
     */
    public static final double PAGINATION_TEXT_FIELD_WIDTH = 40.0;
}
