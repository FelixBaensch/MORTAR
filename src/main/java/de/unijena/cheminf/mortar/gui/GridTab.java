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

package de.unijena.cheminf.mortar.gui;

import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import javafx.scene.control.Tab;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;

/**
 * Custom tab which contains a grid pane
 */
public class GridTab extends Tab {

    //<editor-fold desc="private class variables">
    private GridPane gridPane;
    //</editor-fold>
    //
    /**
     * Constructor
     *
     * Creates a 'No Title' grid tab
     */
    public GridTab(){
        this("No Title", "");
    }
    //
    /**
     * Constructor
     * @param aTitle
     * @param anIdString
     */
    public GridTab(String aTitle, String anIdString){
        super(aTitle);
        this.setClosable(false);
        this.setId(anIdString);
        //gridPane to add button to pagination
        this.gridPane = new GridPane();
        this.setContent(this.gridPane);
        RowConstraints tmpRowCon1 = new RowConstraints();
        tmpRowCon1.setFillHeight(true);
        tmpRowCon1.setVgrow(Priority.ALWAYS);
        gridPane.getRowConstraints().add(tmpRowCon1);
        RowConstraints tmpRowCon2 = new RowConstraints();
        tmpRowCon2.setMaxHeight(GuiDefinitions.GUI_MOLECULE_TAB_SECOND_ROW_HEIGHT);
        tmpRowCon2.setMinHeight(GuiDefinitions.GUI_MOLECULE_TAB_SECOND_ROW_HEIGHT);
        tmpRowCon2.setPrefHeight(GuiDefinitions.GUI_MOLECULE_TAB_SECOND_ROW_HEIGHT);
        tmpRowCon2.setVgrow(Priority.ALWAYS);
        gridPane.getRowConstraints().add(tmpRowCon2);
        ColumnConstraints tmpColCon1 = new ColumnConstraints();
        tmpColCon1.setHgrow(Priority.ALWAYS);
        tmpColCon1.setMaxWidth(GuiDefinitions.GUI_SPACING_VALUE);
        tmpColCon1.setMinWidth(GuiDefinitions.GUI_SPACING_VALUE);
        tmpColCon1.setPrefWidth(GuiDefinitions.GUI_SPACING_VALUE);
        gridPane.getColumnConstraints().add(tmpColCon1);
        ColumnConstraints tmpColCon2 = new ColumnConstraints();
        tmpColCon2.setFillWidth(true);
        tmpColCon2.setHgrow(Priority.ALWAYS);
        gridPane.getColumnConstraints().add(tmpColCon2);
    }
    //
    /**
     * Adds given Node (aNode) to specified column (aColIndex) and row (aRowIndex) to GridPane
     * Necessary to add the pagination via  MainViewController
     *
     * @param aNode Node to add
     * @param aColIndex
     * @param aRowIndex
     * @param aColSpan
     * @param aRowSpan
     */
    public void addToGridPane(javafx.scene.Node aNode, int aColIndex, int aRowIndex, int aColSpan, int aRowSpan){
        this.gridPane.add(aNode, aColIndex, aRowIndex, aColSpan, aRowSpan);
    }
    //
    /**
     * Sets the given string as title of this tab
     *
     * @param aTitle String
     */
    public void setTitle(String aTitle){
        this.setText(aTitle);
    }
}
