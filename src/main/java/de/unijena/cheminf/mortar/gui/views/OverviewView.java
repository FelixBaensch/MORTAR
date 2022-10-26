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

package de.unijena.cheminf.mortar.gui.views;

import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * View class for the overview window
 *
 * @author Samuel Behr
 * @version 1.0.0.0
 */
public class OverviewView extends AnchorPane {

    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     *
     */
    private BorderPane borderPane;
    /**
     *
     */
    private GridPane mainGridPane;
    /**
     *
     */
    private GridPane structureGridPane;
    /**
     *
     */
    private HBox leftHBox;
    /**
     *
     */
    private HBox rightHBox;
    /**
     *
     */
    private TextField columnsPerPageTextField;
    /**
     *
     */
    private TextField rowsPerPageTextField;
    /**
     *
     */
    private Button applyButton;
    /**
     *
     */
    private Button closeButton;
    /**
     *
     */
    private Pagination pagination;
    //</editor-fold>
    //
    //<editor-fold desc="Constructors" defaultstate="collapsed">
    /**
     * Constructor
     */
    public OverviewView(List<MoleculeDataModel> aMoleculeDataModelList, int aRowsPerPage, int aColumnsPerPage) {
        super();
        //gridPane
        this.mainGridPane = new GridPane();
        //this.mainGridPane.setPadding(new Insets(0.0, GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE));
        //this.mainGridPane.setPadding(new Insets(0.0, 0.0, GuiDefinitions.GUI_INSETS_VALUE, 0.0));
        this.mainGridPane.setPadding(new Insets(0.0, 0.0, 0.0, 0.0));
        //this.mainGridPane.setStyle("-fx-background-color: LIGHTGREY");
        this.getChildren().add(this.mainGridPane);
        OverviewView.setTopAnchor(this.mainGridPane, 0.0);
        OverviewView.setRightAnchor(this.mainGridPane, 0.0);
        OverviewView.setLeftAnchor(this.mainGridPane,0.0);
        OverviewView.setBottomAnchor(this.mainGridPane, 0.0);
        RowConstraints tmpRowCon1 = new RowConstraints();
        tmpRowCon1.setFillHeight(true);
        tmpRowCon1.setVgrow(Priority.ALWAYS);
        this.mainGridPane.getRowConstraints().add(tmpRowCon1);
        RowConstraints tmpRowCon2 = new RowConstraints();
        tmpRowCon2.setMaxHeight(GuiDefinitions.GUI_CONTROL_CONTAINER_HEIGHT);
        tmpRowCon2.setMinHeight(GuiDefinitions.GUI_CONTROL_CONTAINER_HEIGHT);
        tmpRowCon2.setPrefHeight(GuiDefinitions.GUI_CONTROL_CONTAINER_HEIGHT);
        tmpRowCon2.setVgrow(Priority.ALWAYS);
        this.mainGridPane.getRowConstraints().add(tmpRowCon2);
        ColumnConstraints tmpColCon1 = new ColumnConstraints();
        tmpColCon1.setHgrow(Priority.ALWAYS);
        tmpColCon1.setMaxWidth(GuiDefinitions.GUI_SPACING_VALUE);
        tmpColCon1.setMinWidth(GuiDefinitions.GUI_SPACING_VALUE);
        tmpColCon1.setPrefWidth(GuiDefinitions.GUI_SPACING_VALUE);
        this.mainGridPane.getColumnConstraints().add(tmpColCon1);
        ColumnConstraints tmpColCon2 = new ColumnConstraints();
        tmpColCon2.setFillWidth(true);
        tmpColCon2.setHgrow(Priority.ALWAYS);
        this.mainGridPane.getColumnConstraints().add(tmpColCon2);
        ColumnConstraints tmpColCon3 = new ColumnConstraints();
        tmpColCon1.setHgrow(Priority.ALWAYS);
        tmpColCon1.setMaxWidth(GuiDefinitions.GUI_SPACING_VALUE);
        tmpColCon1.setMinWidth(GuiDefinitions.GUI_SPACING_VALUE);
        tmpColCon1.setPrefWidth(GuiDefinitions.GUI_SPACING_VALUE);
        this.mainGridPane.getColumnConstraints().add(tmpColCon3);

        this.structureGridPane = new GridPane();
        //this.structureGridPane.setAlignment(Pos.CENTER);
        //upper and lower border: extending the image frame to grid line width
        //right and left border: extending the image frame to grid line width and adding a spacing
        //the spacing depends on grid line width
        this.structureGridPane.setStyle(
                "-fx-background-color: LIGHTGREY; " +
                "-fx-border-color: LIGHTGREY; " +
                "-fx-border-width: " +
                        + (GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 2) + "px " +
                        + ((GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_BORDER_GRIDLINES_WIDTH_RATIO - 0.5)
                                * GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH) + "px " +
                        + (GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 2) + "px " +
                        + ((GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_BORDER_GRIDLINES_WIDTH_RATIO - 0.5)
                                * GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH) + "px; " +
                "-fx-effect: innershadow(gaussian, rgba(100, 100, 100, 0.9), " +
                        GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 2 + ", 0, 0, " +
                        GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 8 + ")"
        );
        //this.structureGridPane.setPadding(new Insets(10, 10, 10, 10));    TODO
        //this.structureGridPane.setGridLinesVisible(true);
        //this.structureGridPane.setStyle("");
        //this.addNodeToMainGridPane(this.structureGridPane,0, 0, 2, 1);
        this.configureStructureGridPane(aRowsPerPage, aColumnsPerPage);
        //this.structureGridPane.setStyle("-fx-border-color: black");


        this.leftHBox = new HBox();
        //this.leftHBox.setPickOnBounds(false); //TODO: setPickOnBounds() ?!
        this.leftHBox.setPadding(new Insets(1.2 * GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE));
        this.leftHBox.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);

        this.rowsPerPageTextField = new TextField();
        this.rowsPerPageTextField.setMinWidth(GuiDefinitions.GUI_TEXT_FIELD_PREF_WIDTH_VALUE);
        this.rowsPerPageTextField.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_PREF_WIDTH_VALUE);
        this.rowsPerPageTextField.setMaxWidth(GuiDefinitions.GUI_SETTINGS_TEXT_FIELD_MAX_WIDTH_VALUE);
        this.rowsPerPageTextField.setAlignment(Pos.CENTER_RIGHT);
        TextFormatter<Integer> tmpFormatter1 = new TextFormatter<>(GuiUtil.getStringToIntegerConverter(), aRowsPerPage, GuiUtil.getIntegerFilter());
        this.rowsPerPageTextField.setTextFormatter(tmpFormatter1);
        Label tmpRowsPerPageLabel = new Label(Message.get("OverviewView.rowsPerPageLabel.text"));
        //HBox.setHgrow(tmpRowsPerPageLabel, Priority.ALWAYS);
        tmpRowsPerPageLabel.setMinWidth(GuiDefinitions.GUI_TEXT_FIELD_PREF_WIDTH_VALUE / 3);
        tmpRowsPerPageLabel.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_PREF_WIDTH_VALUE / 3);
        tmpRowsPerPageLabel.setMaxWidth(GuiDefinitions.GUI_SETTINGS_TEXT_FIELD_MAX_WIDTH_VALUE / 2);
        tmpRowsPerPageLabel.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        //tmpRowsPerPageLabel.setStyle("-fx-background-color: RED");
        Tooltip tmpRowsPerPageTooltip = new Tooltip(Message.get("OverviewView.rowsPerPageLabel.tooltip"));
        tmpRowsPerPageLabel.setTooltip(tmpRowsPerPageTooltip);
        this.rowsPerPageTextField.setTooltip(tmpRowsPerPageTooltip);
        this.columnsPerPageTextField = new TextField();
        this.columnsPerPageTextField.setMinWidth(GuiDefinitions.GUI_TEXT_FIELD_PREF_WIDTH_VALUE);
        this.columnsPerPageTextField.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_PREF_WIDTH_VALUE);
        this.columnsPerPageTextField.setMaxWidth(GuiDefinitions.GUI_SETTINGS_TEXT_FIELD_MAX_WIDTH_VALUE);
        this.columnsPerPageTextField.setAlignment(Pos.CENTER_RIGHT);
        TextFormatter<Integer> tmpFormatter2 = new TextFormatter<>(GuiUtil.getStringToIntegerConverter(), aColumnsPerPage, GuiUtil.getIntegerFilter());
        this.columnsPerPageTextField.setTextFormatter(tmpFormatter2);
        Label tmpColumnsPerPageLabel = new Label(Message.get("OverviewView.columnsPerPageLabel.text"));
        //HBox.setHgrow(tmpColumnsPerPageLabel, Priority.ALWAYS);
        tmpColumnsPerPageLabel.setMinWidth(GuiDefinitions.GUI_TEXT_FIELD_PREF_WIDTH_VALUE / 3);
        tmpColumnsPerPageLabel.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_PREF_WIDTH_VALUE / 3);
        tmpColumnsPerPageLabel.setMaxWidth(GuiDefinitions.GUI_SETTINGS_TEXT_FIELD_MAX_WIDTH_VALUE / 2);
        tmpColumnsPerPageLabel.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        //tmpColumnsPerPageLabel.setStyle("-fx-background-color: RED");
        Tooltip tmpColumnsPerPageTooltip = new Tooltip(Message.get("OverviewView.columnsPerPageLabel.tooltip"));
        tmpColumnsPerPageLabel.setTooltip(tmpColumnsPerPageTooltip);
        this.columnsPerPageTextField.setTooltip(tmpColumnsPerPageTooltip);

        this.applyButton = new Button(Message.get("OverviewView.applyButton.text"));
        this.applyButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.applyButton.setMinWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.applyButton.setMaxWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.applyButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        this.applyButton.setTooltip(new Tooltip(Message.get("OverviewView.applyButton.tooltip")));

        this.leftHBox.getChildren().addAll(tmpRowsPerPageLabel, this.rowsPerPageTextField, tmpColumnsPerPageLabel, this.columnsPerPageTextField, this.applyButton);
        //this.leftButtonBar.getButtons().addAll(tmpRowsPerPageLabel, this.rowsPerPageTextField, tmpColumnsPerPageLabel, this.columnsPerPageTextField, this.applyButton);
        //this.leftButtonBar.setButtonMinWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        //this.rightHBox.getChildren().addAll(this.rowsPerPageTextField, this.columnsPerPageTextField, this.applyButton);

        //this.bottomHBox.getChildren().addAll(this.spacingHBox, this.rightHBox);
        //this.bottomHBox.getChildren().addAll(this.leftButtonBar, this.spacingHBox);

        //this.addNodeToMainGridPane(this.bottomHBox, 0, 1, 2, 1);

        //this.rightButtonBar = new ButtonBar();
        //this.rightButtonBar.setPadding(new Insets(0, GuiDefinitions.GUI_INSETS_VALUE, 0, 0));
        this.rightHBox = new HBox();
        this.rightHBox.setPadding(new Insets(1.2 * GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE));
        this.closeButton = new Button(Message.get("OverviewView.closeButton.text"));
        this.closeButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.closeButton.setMinWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.closeButton.setMaxWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.closeButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        this.closeButton.setTooltip(new Tooltip(Message.get("OverviewView.closeButton.tooltip")));
        //this.rightButtonBar.getButtons().add(this.closeButton);
        //this.rightButtonBar.setButtonMinWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.rightHBox.getChildren().add(this.closeButton);

        //this.borderPane.setCenter(this.structureGridPane);
        //
        //this.getChildren().add(this.borderPane);
    }
    //</editor-fold>
    //
    //<editor-fold desc="public methods" defaultstate="collapsed">
    /**
     *
     * @param aRowsPerPage
     * @param aColumnsPerPage
     */
    public void configureStructureGridPane(int aRowsPerPage, int aColumnsPerPage) {
        if (this.structureGridPane == null) {
            this.structureGridPane = new GridPane();
            //this.structureGridPane.setGridLinesVisible(true);
        } else {
            this.structureGridPane.getRowConstraints().clear();
            this.structureGridPane.getColumnConstraints().clear();
        }
        for (int i = 0; i < aRowsPerPage; i++) {
            RowConstraints tmpRowCon = new RowConstraints();
            //tmpRowCon.setFillHeight(true);
            tmpRowCon.setVgrow(Priority.ALWAYS);
            tmpRowCon.setPercentHeight(100.0 / aRowsPerPage);
            this.structureGridPane.getRowConstraints().add(tmpRowCon);
        }
        for (int i = 0; i < aColumnsPerPage; i++) {
            ColumnConstraints tmpColCon = new ColumnConstraints();
            //tmpColCon.setFillWidth(true);
            tmpColCon.setHgrow(Priority.ALWAYS);
            tmpColCon.setPercentWidth(100.0 / aColumnsPerPage);
            this.structureGridPane.getColumnConstraints().add(tmpColCon);
        }
        this.structureGridPane.setAlignment(Pos.CENTER);
    }
    //
    /**
     *
     * @param aNode
     * @param aColIndex
     * @param aRowIndex
     * @param aColSpan
     * @param aRowSpan
     */
    public void addNodeToMainGridPane(javafx.scene.Node aNode, int aColIndex, int aRowIndex, int aColSpan, int aRowSpan){
        this.mainGridPane.add(aNode, aColIndex, aRowIndex, aColSpan, aRowSpan);
    }
    //
    /**
     *
     * @param aPagination
     */
    public void addPaginationToGridPane(Pagination aPagination) {
        this.pagination = aPagination;
        this.addNodeToMainGridPane(this.pagination, 0, 0, 3, 2);
    }
    //</editor-fold>
    //
    //<editor-fold desc="public properties" defaultstate="collapsed">
    public HBox getLeftHBox() {
        return this.leftHBox;
    }
    //
    public HBox getRightHBox() {
        return this.rightHBox;
    }
    //
    public Button getApplyButton() {
        return this.applyButton;
    }
    //
    public Button getCloseButton() {
        return this.closeButton;
    }
    //
    public TextField getRowsPerPageTextField() {
        return this.rowsPerPageTextField;
    }
    //
    public TextField getColumnsPerPageTextField() {
        return this.columnsPerPageTextField;
    }
    //
    public Pagination getPagination() {
        return this.pagination;
    }
    //
    public GridPane getStructureGridPane() {
        return this.structureGridPane;
    }
    //</editor-fold>

}
