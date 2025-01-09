/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2025  Felix Baensch, Jonas Schaub (felix.j.baensch@gmail.com, jonas.schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.gui.controls;

import de.unijena.cheminf.mortar.controller.TabNames;
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;

import javafx.geometry.HPos;
import javafx.scene.control.Pagination;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;

/**
 * Custom tab which contains a grid pane.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class GridTabForTableView extends Tab {
    //<editor-fold desc="private class variables">
    /**
     * GridPane to align nodes.
     */
    private final GridPane gridPane;
    /**
     * Pagination for table view.
     */
    private Pagination pagination;
    /**
     * Encapsulated table view.
     */
    private final TableView tableView;
    //</editor-fold>
    //
    //<editor-fold desc="Constructors">
    /**
     * Constructor. Creates a 'No Title' grid tab
     *
     * @param aTableView TableView to add
     */
    public GridTabForTableView(TableView aTableView) {
        this("No Title", "", aTableView);
    }
    //
    /**
     * Constructor.
     *
     * @param aTitle String title of Tab
     * @param anIdString String ID of Tab
     * @param aTableView TableView to add
     */
    public GridTabForTableView(String aTitle, String anIdString, TableView aTableView) {
        super();
        this.setText(aTitle);
        this.setClosable(false);
        this.setId(anIdString);
        this.tableView = aTableView;
        //gridPane to add button to pagination
        this.gridPane = new GridPane();
        this.setContent(this.gridPane);
        RowConstraints tmpRowCon1 = new RowConstraints();
        tmpRowCon1.setFillHeight(true);
        tmpRowCon1.setVgrow(Priority.ALWAYS);
        this.gridPane.getRowConstraints().add(tmpRowCon1);
        RowConstraints tmpRowCon2 = new RowConstraints();
        tmpRowCon2.setMaxHeight(GuiDefinitions.GUI_CONTROL_CONTAINER_HEIGHT);
        tmpRowCon2.setMinHeight(GuiDefinitions.GUI_CONTROL_CONTAINER_HEIGHT);
        tmpRowCon2.setPrefHeight(GuiDefinitions.GUI_CONTROL_CONTAINER_HEIGHT);
        tmpRowCon2.setVgrow(Priority.ALWAYS);
        this.gridPane.getRowConstraints().add(tmpRowCon2);
        ColumnConstraints tmpColCon1 = new ColumnConstraints();
        tmpColCon1.setHgrow(Priority.ALWAYS);
        tmpColCon1.setMaxWidth(GuiDefinitions.GUI_SPACING_VALUE);
        tmpColCon1.setMinWidth(GuiDefinitions.GUI_SPACING_VALUE);
        tmpColCon1.setPrefWidth(GuiDefinitions.GUI_SPACING_VALUE);
        this.gridPane.getColumnConstraints().add(tmpColCon1);
        ColumnConstraints tmpColCon2 = new ColumnConstraints();
        tmpColCon2.setFillWidth(true);
        tmpColCon2.setHgrow(Priority.ALWAYS);
        this.gridPane.getColumnConstraints().add(tmpColCon2);
        ColumnConstraints tmpColCon3 = new ColumnConstraints();
        tmpColCon3.setHgrow(Priority.ALWAYS);
        tmpColCon3.setMaxWidth(GuiDefinitions.GUI_GRIDPANE_FOR_NODE_ALIGNMENT_THIRD_COL_WIDTH);
        tmpColCon3.setMinWidth(GuiDefinitions.GUI_GRIDPANE_FOR_NODE_ALIGNMENT_THIRD_COL_WIDTH);
        tmpColCon3.setPrefWidth(GuiDefinitions.GUI_GRIDPANE_FOR_NODE_ALIGNMENT_THIRD_COL_WIDTH);
        tmpColCon3.setHalignment(HPos.RIGHT);
        this.gridPane.getColumnConstraints().add(tmpColCon3);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public methods">
    /**
     * Adds given Node (aNode) to specified column (aColIndex) and row (aRowIndex) to GridPane.
     * Necessary to add nodes via  MainViewController
     *
     * @param aNode Node to add
     * @param aColIndex index in which col the node should be added, only be 0, 1 or 2
     * @param aRowIndex index in which row the node should be added, only be 0 or 1
     * @param aColSpan index how many cols should this node span
     * @param aRowSpan index how many rows should this node span
     */
    public void addNodeToGridPane(javafx.scene.Node aNode, int aColIndex, int aRowIndex, int aColSpan, int aRowSpan) {
        this.gridPane.add(aNode, aColIndex, aRowIndex, aColSpan, aRowSpan);
    }
    //
    /**
     * Adds given Pagination to GridPane.
     * Necessary to add the pagination via MainViewController
     *
     * @param aPagination Pagination to add
     */
    public void addPaginationToGridPane(Pagination aPagination) {
        this.pagination = aPagination;
        this.addNodeToGridPane(this.pagination, 0, 0, 3, 2);
    }
    //
    /**
     * Sets the given string as title of this tab.
     *
     * @param aTitle String
     */
    public void setTitle(String aTitle) {
        this.setText(aTitle);
    }
    //
    /**
     * Returns Pagination.
     *
     * @return pagination
     */
    public Pagination getPagination() {
        return this.pagination;
    }
    //
    /**
     * Returns TableView.
     *
     * @return tableView
     */
    public TableView getTableView() {
        return this.tableView;
    }
    //
    /**
     * Returns the title of this tab.
     *
     * @return title
     */
    public String getTitle() {
        return this.getText();
    }
    //
    /**
     * Returns the name of the fragmentation used.
     *
     * @return fragmentation name
     */
    public String getFragmentationNameOutOfTitle() {
        if (this.getId().equals(TabNames.MOLECULES.name())) {
            return TabNames.MOLECULES.name();
        }
        return this.getText().split("-", 2)[1].trim();
    }
    //</editor-fold>
}
