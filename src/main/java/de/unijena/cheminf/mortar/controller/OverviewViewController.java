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

package de.unijena.cheminf.mortar.controller;

import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.views.OverviewView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

/**
 * Controller class for OverviewView
 *
 * TODO: Logger!!
 *
 * @author Samuel Behr
 * @version 1.0.0.0
 */
public class OverviewViewController {

    //<editor-fold desc="private class variables" defaultstate="collapsed">
    private final Stage mainStage;

    private Stage overviewViewStage;

    private OverviewView overviewView;

    private String overviewViewTitle;

    private Button applyButton;

    private List<MoleculeDataModel> moleculeDataModelList;

    private int rowsPerPage;

    private int columnsPerPage;
    //</editor-fold>

    //<editor-fold desc="Constructors" defaultstate="collapsed">
    /**
     * Constructor
     */
    public OverviewViewController(Stage aStage, String aTabName, List<MoleculeDataModel> aMoleculeDataModelList) {
        //TODO
        this.mainStage = aStage;
        this.overviewViewTitle = aTabName + " - Overview";
        this.moleculeDataModelList = aMoleculeDataModelList;
        this.rowsPerPage = 5;
        this.columnsPerPage = 4;
        for (MoleculeDataModel tmpMolecule : aMoleculeDataModelList) {
            System.out.println(tmpMolecule.getUniqueSmiles());
        }
        this.showOverviewView();
    }
    //</editor-fold>
    //
    //<editor-fold desc="private methods" dafaultstate="collapsed">
    /**
     * Initializes and opens overviewView
     */
    private void showOverviewView() {
        //TODO
        if(this.overviewView == null)
            this.overviewView = new OverviewView(this.moleculeDataModelList, this.rowsPerPage, this.columnsPerPage);
        this.overviewViewStage = new Stage();
        Scene tmpScene = new Scene(this.overviewView, GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE, GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.overviewViewStage.setScene(tmpScene);
        this.overviewViewStage.initModality(Modality.WINDOW_MODAL);    //TODO: consider changing this?
        this.overviewViewStage.initOwner(this.mainStage);
        this.overviewViewStage.setTitle(this.overviewViewTitle);
        this.overviewViewStage.setMinHeight(GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.overviewViewStage.setMinWidth(GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE);

        int tmpPageCount = this.moleculeDataModelList.size() / (this.rowsPerPage * this.columnsPerPage);
        if (this.moleculeDataModelList.size() % (this.rowsPerPage * this.columnsPerPage) > 0) {
            tmpPageCount++;
        }
        Pagination tmpPagination = new Pagination(tmpPageCount, 0);
        tmpPagination.setPageFactory((pageIndex) -> this.overviewView.createOverviewViewPage(pageIndex, this.rowsPerPage, this.columnsPerPage));
        VBox.setVgrow(tmpPagination, Priority.ALWAYS);
        HBox.setHgrow(tmpPagination, Priority.ALWAYS);
        this.overviewView.addPaginationToGridPane(tmpPagination);

        this.overviewView.getApplyButton().setOnAction(actionEvent -> {
            this.applyChangeOfGridConfiguration();
        });


        /*
        this.applyButton = new Button(Message.get("MainTabPane.moleculesTab.fragmentButton.text"));
        ButtonBar tmpButtonBar = new ButtonBar();
        tmpButtonBar.setPadding(new Insets(0, 0, 0, 0));
        this.applyButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.applyButton.setMinWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.applyButton.setMaxWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.applyButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpButtonBar.getButtons().add(this.applyButton);
        tmpButtonBar.setButtonMinWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        Label tmpLabel = new Label();
        //tmpLabel.textProperty().bind(this.fragmentationService.selectedFragmenterNamePropertyProperty());
        Tooltip tmpTooltip = new Tooltip();
        //tmpTooltip.textProperty().bind(this.fragmentationService.selectedFragmenterNamePropertyProperty());
        tmpLabel.setTooltip(tmpTooltip);
        HBox.setHgrow(tmpLabel, Priority.ALWAYS);
        tmpButtonBar.getButtons().add(tmpLabel);

        this.overviewView.addNodeToGridPane(tmpButtonBar, 0, 1, 1, 1);
         */

        this.overviewViewStage.showAndWait();
    }

    private void applyChangeOfGridConfiguration() {
        this.rowsPerPage = Integer.parseInt(this.overviewView.getRowsPerPageTextField().getText());
        this.columnsPerPage = Integer.parseInt(this.overviewView.getColumnsPerPageTextField().getText());
        this.overviewView.configureStructureGridPane(this.rowsPerPage, this.columnsPerPage);
        int tmpNewPageCount = this.moleculeDataModelList.size() / (this.rowsPerPage * this.columnsPerPage);
        if (this.moleculeDataModelList.size() % (this.rowsPerPage * this.columnsPerPage) > 0) {
            tmpNewPageCount++;
        }
        if (this.overviewView.getPagination().getPageCount() != tmpNewPageCount) {
            this.overviewView.getPagination().setPageCount(tmpNewPageCount);
            if (this.overviewView.getPagination().getCurrentPageIndex() > tmpNewPageCount) {
                this.overviewView.getPagination().setCurrentPageIndex(tmpNewPageCount);
            }
        }
        this.overviewView.createOverviewViewPage(this.overviewView.getPagination().getCurrentPageIndex(), this.rowsPerPage, this.columnsPerPage);
    }
    //</editor-fold>

}
