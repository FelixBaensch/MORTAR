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
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.gui.views.OverviewView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.depict.DepictionUtil;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.exception.CDKException;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller class for OverviewView
 *
 * TODO: Logger!!
 *
 * @author Samuel Behr
 * @version 1.0.0.0
 */
public class OverviewViewController {

    //<editor-fold desc="private static final class constants" defaultstate="collapsed">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(OverviewViewController.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * Main stage object of the application
     */
    private final Stage mainStage;
    /**
     * Stage for the OverviewView
     */
    private Stage overviewViewStage;
    /**
     * OvervieView
     */
    private OverviewView overviewView;
    /**
     * Title of the overviewViewStage
     */
    private String overviewViewTitle;
    /**
     * List of MoleculeDataModels for visualization in OverviewView
     */
    private List<MoleculeDataModel> moleculeDataModelList;
    /**
     * Number of rows for the structureGridPane of the OverviewView
     */
    private int rowsPerPage;
    /**
     * Number of columns for the structureGridPane of the OverviewView
     */
    private int columnsPerPage;
    /**
     * Boolean value that defines, whether the structure images should be generated and shown when a new OverviewView
     * page gets created
     */
    private boolean showImages;
    //</editor-fold>

    //<editor-fold desc="Constructors" defaultstate="collapsed">
    /**
     * Constructor
     */
    public OverviewViewController(Stage aStage, String aTabName, List<MoleculeDataModel> aMoleculeDataModelList) {
        //TODO
        this.mainStage = aStage;
        this.overviewViewTitle = aTabName + " - " + Message.get("OverviewView.nameOfView");
        this.moleculeDataModelList = aMoleculeDataModelList;
        this.rowsPerPage = 5;
        this.columnsPerPage = 4;
        //set showImages to false to create an empty structureGridPane at first
        this.showImages = false;
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
        //this.overviewViewStage.show();

        int tmpPageCount = this.moleculeDataModelList.size() / (this.rowsPerPage * this.columnsPerPage);
        if (this.moleculeDataModelList.size() % (this.rowsPerPage * this.columnsPerPage) > 0) {
            tmpPageCount++;
        }
        Pagination tmpPagination = new Pagination(tmpPageCount, 0);     //TODO: make tmpPagination class var ?!
        tmpPagination.setPageFactory((pageIndex) -> this.createOverviewViewPage(pageIndex, this.rowsPerPage, this.columnsPerPage));
        VBox.setVgrow(tmpPagination, Priority.ALWAYS);
        HBox.setHgrow(tmpPagination, Priority.ALWAYS);
        this.overviewView.addPaginationToGridPane(tmpPagination);
        //this.overviewView.getLeftButtonBar().setStyle("-fx-background-color: RED");
        //this.overviewView.getRightButtonBar().setStyle("-fx-background-color: RED");
        //this.overviewView.getLeftHBox().setStyle("-fx-background-color: RED");
        //this.overviewView.getRightHBox().setStyle("-fx-background-color: RED");
        this.overviewView.addNodeToMainGridPane(this.overviewView.getLeftHBox(), 0, 1, 1, 1);
        //this.overviewView.addNodeToMainGridPane(this.overviewView.getLeftButtonBar(), 0, 1, 1, 1);
        this.overviewView.addNodeToMainGridPane(this.overviewView.getRightHBox(), 2, 1, 1, 1);
        //this.overviewView.addNodeToMainGridPane(this.overviewView.getRightButtonBar(), 2, 1, 1, 1);

        this.addListener(tmpPagination);

        this.overviewViewStage.showAndWait();
    }

    /**
     * Adds listeners and event handlers to control elements etc.
     *
     * @param aPagination
     */
    private void addListener(Pagination aPagination) {
        this.overviewView.getApplyButton().setOnAction(actionEvent -> {
            try {
                this.applyChangeOfGridConfiguration();
            } catch (IllegalArgumentException anIllegalArgumentException) {
                OverviewViewController.LOGGER.log(Level.WARNING, anIllegalArgumentException.toString(), anIllegalArgumentException);
                GuiUtil.guiExceptionAlert(Message.get("OverviewView.Error.invalidTextFieldInput.title"),
                        Message.get("OverviewView.Error.invalidTextFieldInput.header"),
                        anIllegalArgumentException.toString(),
                        anIllegalArgumentException);
            }
        });

        this.overviewView.getCloseButton().setOnAction(actionEvent -> {
            this.overviewViewStage.close();
        });

        ChangeListener<Number> tmpStageSizeListener = (observable, oldValue, newValue) -> {
            //System.out.println("Height: " + this.overviewViewStage.getHeight() + " Width: " + this.overviewViewStage.getWidth());
        };

        this.overviewViewStage.heightProperty().addListener(tmpStageSizeListener);
        this.overviewViewStage.widthProperty().addListener(tmpStageSizeListener);

        /*this.overviewViewStage.heightProperty().addListener((observableValue, number, t1) -> {
            this.createOverviewViewPage(tmpPagination.getCurrentPageIndex(), this.rowsPerPage, this.columnsPerPage);
            System.out.println("This line is being executed too");
        });*/

        this.overviewViewStage.setOnShown(windowEvent -> {
            //tmpPagination.setCurrentPageIndex(tmpPagination.getCurrentPageIndex());
            this.createOverviewViewPage(aPagination.getCurrentPageIndex(), this.rowsPerPage, this.columnsPerPage);
            System.out.println("This line is being executed");
        });
    }

    /**
     *
     * @param aColumnsPerPage
     * @param aRowsPerPage
     * @return
     */
    private Node createOverviewViewPage(int aPageIndex, int aRowsPerPage, int aColumnsPerPage) {
        //Node tmpGridLines = this.structureGridPane.getChildren().get(0);
        this.overviewView.getStructureGridPane().getChildren().clear();
        //this.structureGridPane.getChildren().add(tmpGridLines);

        if (this.showImages) {
            System.out.println("CurrentPageIndex: " + aPageIndex);
            int tmpFromIndex = aPageIndex * aRowsPerPage * aColumnsPerPage;
            int tmpToIndex = Math.min(tmpFromIndex + (aRowsPerPage * aColumnsPerPage), this.moleculeDataModelList.size());
            int tmpCurrentIndex = tmpFromIndex;
            double tmpImageHeight = ((this.overviewView.getStructureGridPane().getHeight() -
                    GuiDefinitions.OVERVIEWVIEW_STRUCTUREGRIDPANE_GRIDLINES_WIDTH) / aRowsPerPage) -
                    GuiDefinitions.OVERVIEWVIEW_STRUCTUREGRIDPANE_GRIDLINES_WIDTH;
            double tmpImageWidth = ((this.overviewView.getStructureGridPane().getWidth() -
                    (2 * (GuiDefinitions.OVERVIEWVIEW_STRUCTUREGRIDPANE_BORDER_GRIDLINES_WIDTH_RATIO - 0.5) *
                    GuiDefinitions.OVERVIEWVIEW_STRUCTUREGRIDPANE_GRIDLINES_WIDTH)) / aColumnsPerPage) -
                    GuiDefinitions.OVERVIEWVIEW_STRUCTUREGRIDPANE_GRIDLINES_WIDTH;
            xloop:
            for (int i = 0; i < aRowsPerPage; i++) {
                for (int j = 0; j < aColumnsPerPage; j++) {
                    if (tmpCurrentIndex >= tmpToIndex) {
                        break xloop;
                    }
                    StackPane tmpStackPane = new StackPane();
                    Node tmpContentNode;
                    try {
                        tmpContentNode = new ImageView(
                                DepictionUtil.depictImageWithZoom(this.moleculeDataModelList.get(tmpCurrentIndex)
                                        .getAtomContainer(), 1.0, tmpImageWidth, tmpImageHeight)
                        );
                    } catch (CDKException anException) {
                        OverviewViewController.LOGGER.log(Level.INFO, anException.toString(), anException);
                        //Error label instead of image
                        Label tmpErrorLabel = new Label(Message.get("OverviewView.ErrorLabel.text"));
                        tmpErrorLabel.setMinWidth(tmpImageWidth);
                        tmpErrorLabel.setMaxWidth(tmpImageWidth);
                        tmpErrorLabel.setMinHeight(tmpImageHeight);
                        tmpErrorLabel.setMaxHeight(tmpImageHeight);
                        tmpErrorLabel.setStyle("-fx-alignment: CENTER; -fx-background-color: WHITE");
                        Tooltip tmpErrorLabelTooltip = new Tooltip(Message.get("OverviewView.ErrorLabel.tooltip"));
                        tmpErrorLabel.setTooltip(tmpErrorLabelTooltip);
                        tmpContentNode = tmpErrorLabel;
                    }
                    //TODO: add listeners
                    //tmpContentNode.setEffect(new DropShadow(2, Color.BLACK));
                    //tmpContentNode.setStyle("-fx-effect: innershadow(three-pass-box, rgba(100, 100, 100, 1), " + GuiDefinitions.OVERVIEWVIEW_STRUCTUREGRIDPANE_GRIDLINES_WIDTH / 2 + ", 0, 0, 0)");
                    tmpContentNode.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(100, 100, 100, 0.6), " +
                            GuiDefinitions.OVERVIEWVIEW_STRUCTUREGRIDPANE_GRIDLINES_WIDTH / 4 + ", 0, " +
                            GuiDefinitions.OVERVIEWVIEW_STRUCTUREGRIDPANE_GRIDLINES_WIDTH / 4 + ", " +
                            GuiDefinitions.OVERVIEWVIEW_STRUCTUREGRIDPANE_GRIDLINES_WIDTH / 4 + ")");
                    tmpStackPane.getChildren().add(tmpContentNode);
                    this.overviewView.getStructureGridPane().add(tmpStackPane, j, i);
                    tmpCurrentIndex++;
                }
            }
        }
        this.showImages = true;
        return this.overviewView.getStructureGridPane();
    }

    /**
     *
     * @throws IllegalArgumentException
     */
    private void applyChangeOfGridConfiguration() throws IllegalArgumentException {
        int tmpRowsPerPageEntry = Integer.parseInt(this.overviewView.getRowsPerPageTextField().getText());
        if (!(tmpRowsPerPageEntry <= 0)) {
            this.rowsPerPage = tmpRowsPerPageEntry;
        } else {
            throw new IllegalArgumentException(Message.get("OverviewView.Error.RowsPerPageTextField.illegalArgument")
                    + tmpRowsPerPageEntry);
        }
        int tmpColumnsPerPageEntry = Integer.parseInt(this.overviewView.getColumnsPerPageTextField().getText());
        if (!(tmpColumnsPerPageEntry <= 0)) {
            this.columnsPerPage = tmpColumnsPerPageEntry;
        } else {
            throw new IllegalArgumentException(Message.get("OverviewView.Error.ColumnsPerPageTextField.illegalArgument")
                    + tmpColumnsPerPageEntry);
        }
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
        this.createOverviewViewPage(this.overviewView.getPagination().getCurrentPageIndex(), this.rowsPerPage, this.columnsPerPage);
    }
    //</editor-fold>

}
