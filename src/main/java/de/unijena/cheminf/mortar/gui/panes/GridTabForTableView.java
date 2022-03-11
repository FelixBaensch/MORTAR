/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2021  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.gui.panes;

import de.unijena.cheminf.mortar.controller.TabNames;
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import javafx.scene.control.Pagination;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;

/**
 * Custom tab which contains a grid pane
 */
public class GridTabForTableView extends Tab {

    //<editor-fold desc="private class variables">
    /**
     * GridPane to align nodes
     */
    private GridPane gridPane;
    /**
     * Pagination for table view
     */
    private Pagination pagination;
    /**
     *
     */
    private TableView tableView;
    //</editor-fold>
    //
    /**
     * Constructor
     *
     * Creates a 'No Title' grid tab
     *
     * @param aTableView TableView to add
     */
    public GridTabForTableView(TableView aTableView){
        this("No Title", "", aTableView);
    }
    //
    /**
     * Constructor
     * @param aTitle String title of Tab
     * @param anIdString String ID of Tab
     * @param aTableView TableView to add
     */
    public GridTabForTableView(String aTitle, String anIdString, TableView aTableView){
        super();
        setText(aTitle);
        this.setClosable(false);
        this.setId(anIdString);
        this.tableView = aTableView;
        //gridPane to add button to pagination
        this.gridPane = new GridPane();
        this.setContent(this.gridPane);
        RowConstraints tmpRowCon1 = new RowConstraints();
        tmpRowCon1.setFillHeight(true);
        tmpRowCon1.setVgrow(Priority.ALWAYS);
        gridPane.getRowConstraints().add(tmpRowCon1);
        RowConstraints tmpRowCon2 = new RowConstraints();
        tmpRowCon2.setMaxHeight(GuiDefinitions.GUI_CONTROL_CONTAINER_HEIGHT);
        tmpRowCon2.setMinHeight(GuiDefinitions.GUI_CONTROL_CONTAINER_HEIGHT);
        tmpRowCon2.setPrefHeight(GuiDefinitions.GUI_CONTROL_CONTAINER_HEIGHT);
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
     * @param aColIndex index in which col the node should be added, only be 1 or 2
     * @param aRowIndex index in which row the node should be added, only be 1 or 2
     * @param aColSpan index how many cols should this node span
     * @param aRowSpan index how many rows should this node span
     */
    public void addNodeToGridPane(javafx.scene.Node aNode, int aColIndex, int aRowIndex, int aColSpan, int aRowSpan){
        this.gridPane.add(aNode, aColIndex, aRowIndex, aColSpan, aRowSpan);
    }
    //
    /**
     * Adds given Pagination to specified column (aColIndex) and row (aRowIndex) to GridPane
     * Necessary to add the pagination via  MainViewController
     *
     * @param aPagination Pagination to add
     * @param aColIndex index in which col the node should be added, only be 1 or 2
     * @param aRowIndex index in which row the node should be added, only be 1 or 2
     * @param aColSpan index how many cols should this node span
     * @param aRowSpan index how many rows should this node span
     */
    public void addPaginationToGridPane(Pagination aPagination, int aColIndex, int aRowIndex, int aColSpan, int aRowSpan) {
        this.pagination = aPagination;
        this.addNodeToGridPane(this.pagination, 0, 0, 2, 2);
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
    //
    /**
     * Returns Pagination
     * @return pagination
     */
    public Pagination getPagination(){
        return this.pagination;
    }
    //
    /**
     * Returns TableView
     * @return tableView
     */
    public TableView getTableView(){
        return this.tableView;
    }
    //
    /**
     * Returns the title of this tab
     *
     * @return title
     */
    public String getTitle(){
        return this.getText();
    }
    //

    /**
     * Returns the name of the fragmentation used
     *
     * @return fragmentation name
     */
    public String getFragmentationNameOutOfTitle(){
        if(this.getId().equals(TabNames.Molecules.name()))
            return TabNames.Molecules.name();
        return this.getText().split("-", 2)[1].trim();
    }
}
