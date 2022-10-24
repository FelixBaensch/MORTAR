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
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.openscience.cdk.exception.CDKException;

import java.io.InputStream;
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
    private boolean createStructureImages;
    /**
     * TODO
     */
    private ContextMenu structureContextMenu;
    /**
     *
     */
    private Stage enlargedStructureViewStage;
    /**
     *
     */
    private AnchorPane enlargedStructureViewAnchorPane;
    //</editor-fold>
    //
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
        this.enlargedStructureViewStage = null;
        this.enlargedStructureViewAnchorPane = null;
        //creating an empty structureGridPane at first by setting createStructureImages to false
        this.createStructureImages = false;
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
        if (this.overviewView == null)
            this.overviewView = new OverviewView(this.moleculeDataModelList, this.rowsPerPage, this.columnsPerPage);
        this.overviewViewStage = new Stage();
        Scene tmpScene = new Scene(this.overviewView, GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE, GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.overviewViewStage.setScene(tmpScene);
        this.overviewViewStage.initModality(Modality.WINDOW_MODAL);    //TODO: consider changing this?
        this.overviewViewStage.initOwner(this.mainStage);
        this.overviewViewStage.setTitle(this.overviewViewTitle);
        InputStream tmpImageInputStream = MainViewController.class.getResourceAsStream("/de/unijena/cheminf/mortar/images/Mortar_Logo_Icon1.png");
        this.overviewViewStage.getIcons().add(new Image(tmpImageInputStream));
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

        this.addListener();
        //TODO:
        this.structureContextMenu = this.generateContextMenuWithListeners(this.moleculeDataModelList.get(0), this.overviewViewStage);

        this.overviewViewStage.showAndWait();
    }
    //
    /**
     * Adds listeners and event handlers to control elements etc.
     */
    private void addListener() {
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
        //
        this.overviewView.getCloseButton().setOnAction(actionEvent -> {
            this.overviewViewStage.close();
        });
        //
        ChangeListener<Number> tmpStageSizeListener = (observable, oldValue, newValue) -> {
            //TODO
            System.out.println("Window resize event: " + this.overviewViewStage.getWidth() + " - "
                    + this.overviewViewStage.getHeight());
            this.createOverviewViewPage(this.overviewView.getPagination().getCurrentPageIndex(),
                    this.rowsPerPage, this.columnsPerPage);
        };
        //
        this.overviewViewStage.heightProperty().addListener(tmpStageSizeListener);
        this.overviewViewStage.widthProperty().addListener(tmpStageSizeListener);
        //
        /*this.overviewViewStage.heightProperty().addListener((observableValue, number, t1) -> {
            this.createOverviewViewPage(tmpPagination.getCurrentPageIndex(), this.rowsPerPage, this.columnsPerPage);
            System.out.println("This line is being executed too");
        });*/
        //
        this.overviewViewStage.setOnShown(windowEvent -> {
            System.out.println("setOnShown event");
            //tmpPagination.setCurrentPageIndex(tmpPagination.getCurrentPageIndex());
            this.createOverviewViewPage(this.overviewView.getPagination().getCurrentPageIndex(),
                    this.rowsPerPage, this.columnsPerPage);
            System.out.println("setOnShown event end");
        });
    }
    //
    /**
     *
     * @param aColumnsPerPage
     * @param aRowsPerPage
     * @return
     */
    private Node createOverviewViewPage(int aPageIndex, int aRowsPerPage, int aColumnsPerPage) {
        System.out.println("Call of createOverviewViewPage");
        //Node tmpGridLines = this.structureGridPane.getChildren().get(0);
        this.overviewView.getStructureGridPane().getChildren().clear();
        //this.structureGridPane.getChildren().add(tmpGridLines);
        //
        if (this.createStructureImages) {
            System.out.println("CurrentPageIndex: " + aPageIndex);
            int tmpFromIndex = aPageIndex * aRowsPerPage * aColumnsPerPage;
            int tmpToIndex = Math.min(tmpFromIndex + (aRowsPerPage * aColumnsPerPage), this.moleculeDataModelList.size());
            int tmpCurrentIndex = tmpFromIndex;
            double tmpImageHeight = ((this.overviewView.getStructureGridPane().getHeight() -
                    GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH) / aRowsPerPage) -
                    GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH;
            double tmpImageWidth = ((this.overviewView.getStructureGridPane().getWidth() -
                    (2 * (GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_BORDER_GRIDLINES_WIDTH_RATIO - 0.5) *
                    GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH)) / aColumnsPerPage) -
                    GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH;
            boolean tmpDrawImagesWithShadow = true;    //TODO: settings
            xloop:
            for (int i = 0; i < aRowsPerPage; i++) {
                for (int j = 0; j < aColumnsPerPage; j++) {
                    if (tmpCurrentIndex >= tmpToIndex) {
                        break xloop;
                    }
                    StackPane tmpStackPane = new StackPane();
                    Node tmpContentNode;
                    try {
                        MoleculeDataModel tmpMoleculeDataModel = this.moleculeDataModelList.get(tmpCurrentIndex);   //TODO: catch indexOutOfBounds?
                        ImageView tmpImageView = new ImageView(DepictionUtil.depictImageWithZoom(
                                tmpMoleculeDataModel.getAtomContainer(),
                                1.0, tmpImageWidth, tmpImageHeight
                        ));
                        tmpImageView.setOnMouseEntered((mouseEvent) -> {
                            if (tmpDrawImagesWithShadow) {
                                tmpImageView.setStyle("-fx-effect: null");
                                /*tmpImageView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(62, 129, 196, 0.6), " +
                                        GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4 + ", 0, " +
                                        GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4 + ", " +
                                        GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4 + ")");*/
                            } else {
                                tmpImageView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(100, 100, 100, 0.6), " +
                                        GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH + ", 0, 0, 0)");
                                //tmpStackPane.setStyle("-fx-border-color: rgba(40, 127, 299, 0.4); -fx-border-width: " + GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4);
                                /*tmpImageView.setStyle("-fx-effect: innershadow(gaussian, " +
                                        "rgba(62, 129, 196, 1.0), " +
                                        GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH * 2 + ", " +
                                        "0, 0, 0)");*/
                                /*tmpImageView.setStyle("-fx-effect: innershadow(gaussian, " +
                                        "rgba(100, 100, 100, 0.9), " +
                                        GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH + ", " +
                                        "0, 0, 0)");*/
                            }
                        });
                        tmpImageView.setOnMouseExited((mouseEvent) -> {
                            if (tmpDrawImagesWithShadow) {
                                tmpImageView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(100, 100, 100, 0.6), " +
                                        GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4 + ", 0, " +
                                        GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4 + ", " +
                                        GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4 + ")");
                            } else {
                                tmpImageView.setStyle("-fx-effect: null");
                                //tmpStackPane.setStyle("-fx-border-color: null");
                            }
                        });
                        Stage tmpOverviewViewStage = this.overviewViewStage;
                        tmpImageView.setOnMouseClicked((mouseEvent) -> {
                            if (MouseButton.PRIMARY.equals(mouseEvent.getButton())) {
                                this.showEnlargedStructureView(tmpOverviewViewStage, tmpMoleculeDataModel);
                            }
                        });
                        //Setting context menu to the image view
                        tmpImageView.setOnContextMenuRequested((event) -> {
                            this.structureContextMenu.show(tmpImageView, event.getScreenX(), event.getScreenY());
                        });
                        //
                        tmpContentNode = tmpImageView;
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
                    //tmpContentNode.setStyle("-fx-effect: innershadow(three-pass-box, rgba(100, 100, 100, 1), " + GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 2 + ", 0, 0, 0)");
                    if (tmpDrawImagesWithShadow) {
                        tmpContentNode.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(100, 100, 100, 0.6), " +
                                GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4 + ", 0, " +
                                GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4 + ", " +
                                GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4 + ")");
                    }
                    tmpStackPane.getChildren().add(tmpContentNode);
                    this.overviewView.getStructureGridPane().add(tmpStackPane, j, i);
                    tmpCurrentIndex++;
                }
            }
        }
        this.createStructureImages = true;
        return this.overviewView.getStructureGridPane();
    }
    //
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
        this.createOverviewViewPage(this.overviewView.getPagination().getCurrentPageIndex(),
                this.rowsPerPage, this.columnsPerPage);
    }
    //
    /**
     * TODO: make this method static?!
     *
     * @param aMoleculeDataModel
     * @param anOverviewViewStage
     * @return
     */
    private ContextMenu generateContextMenuWithListeners(MoleculeDataModel aMoleculeDataModel, Stage anOverviewViewStage) {
        //context menu
        ContextMenu tmpContextMenu = new ContextMenu();
        //copyImageMenuItem
        MenuItem tmpCopyImageMenuItem = new MenuItem(Message.get("OverviewView.contextMenu.copyImageMenuItem"));
        tmpCopyImageMenuItem.setGraphic(new ImageView(new Image("de/unijena/cheminf/mortar/images/copy_icon_16x16.png")));
        //copySmilesMenuItem
        MenuItem tmpCopySmilesMenuItem = new MenuItem(Message.get("OverviewView.contextMenu.copySmilesMenuItem"));
        //showStructureMenuItem
        MenuItem tmpShowStructureMenuItem = new MenuItem(Message.get("OverviewView.contextMenu.showStructureMenuItem"));
        //add Listeners to MenuItems
        tmpCopyImageMenuItem.setOnAction((ActionEvent event) -> {
            try {
                ClipboardContent tmpContent = new ClipboardContent();
                tmpContent.putImage(DepictionUtil.depictImageWithZoom(
                        aMoleculeDataModel.getAtomContainer(),
                        1.0, 512, 350      //TODO: size of copied image as shown or with defined value?
                ));
                Clipboard.getSystemClipboard().setContent(tmpContent);
                System.out.println("Image has been copied");
            } catch (CDKException anException) {
                //TODO: should not occur
            }
        });
        tmpCopySmilesMenuItem.setOnAction((ActionEvent event) -> {
            ClipboardContent tmpContent = new ClipboardContent();
            tmpContent.putString(aMoleculeDataModel.getUniqueSmiles());
            Clipboard.getSystemClipboard().setContent(tmpContent);
            System.out.println("SMILES has been copied");
        });
        tmpShowStructureMenuItem.setOnAction((ActionEvent event) -> {
            this.showEnlargedStructureView(anOverviewViewStage, aMoleculeDataModel);
        });
        //add MenuItems and return ContextMenu
        tmpContextMenu.getItems().addAll(
                tmpCopyImageMenuItem,
                tmpCopySmilesMenuItem,
                new SeparatorMenuItem(),
                tmpShowStructureMenuItem
        );
        return tmpContextMenu;
    }
    //
    /**
     *
     * @param aPrimaryStage
     * @param aMoleculeDataModel
     */
    private void showEnlargedStructureView(Stage aPrimaryStage, MoleculeDataModel aMoleculeDataModel) {
        //TODO!!
        System.out.println("Enlarged structure is being shown!");

        if (this.enlargedStructureViewStage == null) {
            this.enlargedStructureViewStage = new Stage();
            this.enlargedStructureViewAnchorPane = new AnchorPane();
            Scene tmpScene = new Scene(this.enlargedStructureViewAnchorPane, 300, 400);
            this.enlargedStructureViewStage.setScene(tmpScene);
            this.enlargedStructureViewStage.initModality(Modality.WINDOW_MODAL);
            this.enlargedStructureViewStage.initOwner(this.overviewViewStage);
            this.enlargedStructureViewStage.setTitle(Message.get("OverviewView.enlargedStructureView.title"));
            InputStream tmpImageInputStream = MainViewController.class.getResourceAsStream("/de/unijena/cheminf/mortar/images/Mortar_Logo_Icon1.png");
            this.enlargedStructureViewStage.getIcons().add(new Image(tmpImageInputStream));
            this.enlargedStructureViewStage.setMinHeight(GuiDefinitions.ENLARGED_STRUCTURE_VIEW_MIN_HEIGHT_VALUE);
            this.enlargedStructureViewStage.setMinWidth(GuiDefinitions.ENLARGED_STRUCTURE_VIEW_MIN_WIDTH_VALUE);
        }
        if (!this.enlargedStructureViewStage.isShowing()) {
            this.enlargedStructureViewStage.show();
        }

        try {
            ImageView tmpStructureImage = new ImageView(DepictionUtil.depictImage(aMoleculeDataModel.getAtomContainer(),
                    this.enlargedStructureViewAnchorPane.getWidth(), this.enlargedStructureViewAnchorPane.getHeight()));
            this.enlargedStructureViewAnchorPane.getChildren().add(tmpStructureImage);

            //context menu
            ContextMenu tmpContextMenu = new ContextMenu();
            //copyImageMenuItem
            MenuItem tmpCopyImageMenuItem = new MenuItem(Message.get("OverviewView.contextMenu.copyImageMenuItem"));
            tmpCopyImageMenuItem.setGraphic(new ImageView(new Image("de/unijena/cheminf/mortar/images/copy_icon_16x16.png")));
            //copySmilesMenuItem
            MenuItem tmpCopySmilesMenuItem = new MenuItem(Message.get("OverviewView.contextMenu.copySmilesMenuItem"));
            //add Listeners to MenuItems
            tmpCopyImageMenuItem.setOnAction((ActionEvent event) -> {
                try {
                    ClipboardContent tmpContent = new ClipboardContent();
                    tmpContent.putImage(DepictionUtil.depictImageWithZoom(
                            aMoleculeDataModel.getAtomContainer(),
                            1.0, 512, 350      //TODO: size of copied image as shown or with defined value?
                    ));
                    Clipboard.getSystemClipboard().setContent(tmpContent);
                    System.out.println("Image has been copied");
                } catch (CDKException anException) {
                    //TODO: should not occur
                }
            });
            tmpCopySmilesMenuItem.setOnAction((ActionEvent event) -> {
                ClipboardContent tmpContent = new ClipboardContent();
                tmpContent.putString(aMoleculeDataModel.getUniqueSmiles());
                Clipboard.getSystemClipboard().setContent(tmpContent);
                System.out.println("SMILES has been copied");
            });
            //add MenuItems
            tmpContextMenu.getItems().addAll(tmpCopyImageMenuItem, tmpCopySmilesMenuItem);

            tmpStructureImage.setOnContextMenuRequested((event) -> {
                tmpContextMenu.show(tmpStructureImage, event.getScreenX(), event.getScreenY());
            });
        } catch (CDKException anException) {
            //TODO?! if an exception is thrown generating an AtomContainer this point is never reached
        }
    }
    //</editor-fold>

}
