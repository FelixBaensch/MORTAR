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
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.depict.DepictionUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.openscience.cdk.exception.CDKException;

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
    private HBox bottomHBox;


    private HBox spacingHBox;
    private HBox rightHBox;
    private VBox rightVBox;


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
    private Label columnsPerPageLabel;
    /**
     *
     */
    private Label rowsPerPageLabel;
    /**
     *
     */
    private Pagination pagination;
    /**
     *
     */
    private List<MoleculeDataModel> moleculeDataModelList;
    //</editor-fold>
    //
    //<editor-fold desc="Constructors" defaultstate="collapsed">
    /**
     * Constructor
     */
    public OverviewView(List<MoleculeDataModel> aMoleculeDataModelList, int aRowsPerPage, int aColumnsPerPage) {
        super();
        this.moleculeDataModelList = aMoleculeDataModelList;
        //borderPane
        /*this.borderPane = new BorderPane();
        OverviewView.setTopAnchor(this.borderPane, 0.0);
        OverviewView.setRightAnchor(this.borderPane, 0.0);
        OverviewView.setLeftAnchor(this.borderPane, 0.0);
        OverviewView.setBottomAnchor(this.borderPane, 0.0);*/
        //gridPane
        this.mainGridPane = new GridPane();
        this.mainGridPane.setPadding(new Insets(0.0, 0.0, GuiDefinitions.GUI_INSETS_VALUE, 0.0));
        this.getChildren().add(this.mainGridPane);
        OverviewView.setTopAnchor(this.mainGridPane, 0.0);
        OverviewView.setRightAnchor(this.mainGridPane, 0.0);
        OverviewView.setLeftAnchor(this.mainGridPane,0.0);
        OverviewView.setBottomAnchor(this.mainGridPane, 0.0);
        RowConstraints tmpRowCon1 = new RowConstraints();
        tmpRowCon1.setFillHeight(true);
        tmpRowCon1.setVgrow(Priority.ALWAYS);
        mainGridPane.getRowConstraints().add(tmpRowCon1);
        RowConstraints tmpRowCon2 = new RowConstraints();
        tmpRowCon2.setMaxHeight(GuiDefinitions.GUI_CONTROL_CONTAINER_HEIGHT);
        tmpRowCon2.setMinHeight(GuiDefinitions.GUI_CONTROL_CONTAINER_HEIGHT);
        tmpRowCon2.setPrefHeight(GuiDefinitions.GUI_CONTROL_CONTAINER_HEIGHT);
        tmpRowCon2.setVgrow(Priority.ALWAYS);
        mainGridPane.getRowConstraints().add(tmpRowCon2);
        ColumnConstraints tmpColCon1 = new ColumnConstraints();
        tmpColCon1.setHgrow(Priority.ALWAYS);
        tmpColCon1.setMaxWidth(GuiDefinitions.GUI_SPACING_VALUE);
        tmpColCon1.setMinWidth(GuiDefinitions.GUI_SPACING_VALUE);
        tmpColCon1.setPrefWidth(GuiDefinitions.GUI_SPACING_VALUE);
        mainGridPane.getColumnConstraints().add(tmpColCon1);
        ColumnConstraints tmpColCon2 = new ColumnConstraints();
        tmpColCon2.setFillWidth(true);
        tmpColCon2.setHgrow(Priority.ALWAYS);
        mainGridPane.getColumnConstraints().add(tmpColCon2);

        this.structureGridPane = new GridPane();
        this.configureStructureGridPane(aRowsPerPage, aColumnsPerPage);
        this.structureGridPane.setAlignment(Pos.CENTER);
        //this.structureGridPane.setPadding(new Insets(10, 10, 10, 10));    TODO
        this.structureGridPane.setGridLinesVisible(true);
        this.addNodeToMainGridPane(this.structureGridPane,0, 0, 2, 1);

        //ButtonBar tmpButtonBar = new ButtonBar();
        //tmpButtonBar.setPadding(new Insets(0, 0, 0, 0));
        this.bottomHBox = new HBox();

        this.spacingHBox = new HBox();
        HBox.setHgrow(this.spacingHBox, Priority.ALWAYS);


        this.rightHBox = new HBox();

        this.rightVBox = new VBox();
        this.rowsPerPageTextField = new TextField();
        this.rowsPerPageTextField.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_PREF_WIDTH_VALUE);
        this.rowsPerPageTextField.setMaxWidth(GuiDefinitions.GUI_SETTINGS_TEXT_FIELD_MAX_WIDTH_VALUE);
        this.rowsPerPageTextField.setAlignment(Pos.CENTER_RIGHT);
        TextFormatter<Integer> tmpFormatter1 = new TextFormatter<>(GuiUtil.getStringToIntegerConverter(), aRowsPerPage, GuiUtil.getIntegerFilter());
        this.rowsPerPageTextField.setTextFormatter(tmpFormatter1);
        this.columnsPerPageTextField = new TextField();
        this.columnsPerPageTextField.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_PREF_WIDTH_VALUE);
        this.columnsPerPageTextField.setMaxWidth(GuiDefinitions.GUI_SETTINGS_TEXT_FIELD_MAX_WIDTH_VALUE);
        this.columnsPerPageTextField.setAlignment(Pos.CENTER_RIGHT);
        TextFormatter<Integer> tmpFormatter2 = new TextFormatter<>(GuiUtil.getStringToIntegerConverter(), aColumnsPerPage, GuiUtil.getIntegerFilter());
        this.columnsPerPageTextField.setTextFormatter(tmpFormatter2);
        this.rightVBox.getChildren().addAll(this.rowsPerPageTextField, this.columnsPerPageTextField);

        this.applyButton = new Button("Apply");
        this.applyButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.applyButton.setMinWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.applyButton.setMaxWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.applyButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        this.rightHBox.getChildren().addAll(this.rightVBox, this.applyButton);

        this.bottomHBox.getChildren().addAll(this.spacingHBox, this.rightHBox);

        this.addNodeToMainGridPane(this.bottomHBox, 0, 1, 2, 1);

        //this.borderPane.setCenter(this.structureGridPane);
        //
        //this.getChildren().add(this.borderPane);
    }
    //</editor-fold>

    //<editor-fold desc="public methods" defaultstate="collapsed">
    /**
     *
     * @param aColumnsPerPage
     * @param aRowsPerPage
     * @return
     */
    public Node createOverviewViewPage(int aPageIndex, int aRowsPerPage, int aColumnsPerPage) {
        Node tmpGridLines = this.structureGridPane.getChildren().get(0);
        this.structureGridPane.getChildren().clear();
        this.structureGridPane.getChildren().add(tmpGridLines);

        System.out.println("CurrentPageIndex: " + aPageIndex);
        int tmpFromIndex = aPageIndex * aRowsPerPage * aColumnsPerPage;
        int tmpToIndex = Math.min(tmpFromIndex + (aRowsPerPage * aColumnsPerPage), this.moleculeDataModelList.size());
        int tmpCurrentIndex = tmpFromIndex;
        double tmpImageHight = 100; //this.structureGridPane.getHight() / aRowsPerPage;
        double tmpImageWidth = 150; //this.structureGridPane.getWidth() / aColumnsPerPage;
        xloop:
        for (int i = 0; i < aRowsPerPage; i++) {
            for (int j = 0; j < aColumnsPerPage; j++) {
                if (tmpCurrentIndex >= tmpToIndex) {
                    break xloop;
                }
                StackPane tmpStackPane = new StackPane();
                try {
                    ImageView tmpImageView = new ImageView(
                            DepictionUtil.depictImageWithZoom(this.moleculeDataModelList
                                    .get(tmpCurrentIndex).getAtomContainer(), 1.0, tmpImageWidth, tmpImageHight)
                    );
                    tmpStackPane.getChildren().add(tmpImageView);
                } catch (CDKException anException) {
                    Label tmpLabel = new Label("[Error]");
                    tmpStackPane.getChildren().add(tmpLabel);
                }
                this.structureGridPane.add(tmpStackPane, j, i);
                tmpCurrentIndex++;
            }
        }
        return this.structureGridPane;
    }

    public void configureStructureGridPane(int aRowsPerPage, int aColumnsPerPage) {
        if (this.structureGridPane == null) {
            this.structureGridPane = new GridPane();
            this.structureGridPane.setGridLinesVisible(true);
        } else {
            this.structureGridPane.getRowConstraints().clear();
            this.structureGridPane.getColumnConstraints().clear();
        }
        for (int i = 0; i < aRowsPerPage; i++) {
            RowConstraints tmpRowCon = new RowConstraints();
            tmpRowCon.setFillHeight(true);
            tmpRowCon.setVgrow(Priority.ALWAYS);
            this.structureGridPane.getRowConstraints().add(tmpRowCon);
        }
        for (int i = 0; i < aColumnsPerPage; i++) {
            ColumnConstraints tmpColCon = new ColumnConstraints();
            tmpColCon.setFillWidth(true);
            tmpColCon.setHgrow(Priority.ALWAYS);
            this.structureGridPane.getColumnConstraints().add(tmpColCon);
        }
        this.structureGridPane.setAlignment(Pos.CENTER);
    }

    public void addNodeToMainGridPane(javafx.scene.Node aNode, int aColIndex, int aRowIndex, int aColSpan, int aRowSpan){
        this.mainGridPane.add(aNode, aColIndex, aRowIndex, aColSpan, aRowSpan);
    }
    public void addPaginationToGridPane(Pagination aPagination) {
        this.pagination = aPagination;
        this.addNodeToMainGridPane(this.pagination, 0, 0, 2, 1);
    }
    //</editor-fold>

    //<editor-fold desc="public properties" defaultstate="collapsed">
    public Button getApplyButton() {
        return this.applyButton;
    }

    public TextField getRowsPerPageTextField() {
        return this.rowsPerPageTextField;
    }

    public TextField getColumnsPerPageTextField() {
        return this.columnsPerPageTextField;
    }

    public Pagination getPagination() {
        return this.pagination;
    }
    //</editor-fold>

}
