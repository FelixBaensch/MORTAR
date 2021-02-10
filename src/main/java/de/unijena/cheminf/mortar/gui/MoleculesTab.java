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
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.DataModel;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.util.Callback;

/**
 * MoleculesTab
 *
 * @author Felix Baensch
 */
public class MoleculesTab extends Tab {

    //<editor-fold desc="private class variables" defaultstate="collapsed">
    private GridPane gridPane;
    private Button fragmentButton;
    //</editor-fold>

    /**
     * Constructor
     */
    public MoleculesTab(){
        super(Message.get("MainTabPane.moleculesTab.title"));
        this.setClosable(false);
        this.fragmentButton = new Button(Message.get("MainTabPane.moleculesTab.button.text"));
        this.fragmentButton.setMaxHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        this.fragmentButton.setMinHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        this.fragmentButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
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
    public void addToGridPane(Node aNode, int aColIndex, int aRowIndex, int aColSpan, int aRowSpan){
        this.gridPane.add(aNode, aColIndex, aRowIndex, aColSpan, aRowSpan);
    }

    /**
     * Adds fragmentButton to gridPane
     * Necessary to add it after pagination to overlap it
     */
    public void addFragmentButton(){
        this.gridPane.add(this.fragmentButton, 1,1,1,1);
    }

    /**
     * Returns fragmentButton, button to start fragmentation
     *
     * @return fragmentButton, button
     */
    public Button getFragmentButton(){
        return this.fragmentButton;
    }
}
