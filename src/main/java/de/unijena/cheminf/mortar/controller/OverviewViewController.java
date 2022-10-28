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
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Pagination;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.openscience.cdk.exception.CDKException;

import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller class of the overview view.
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
     * Main stage object of the application.
     */
    private final Stage mainStage;
    /**
     * Stage of the overview view.
     */
    private Stage overviewViewStage;
    /**
     * OvervieView instance of the class.
     */
    private OverviewView overviewView;
    /**
     * Title of the overviewViewStage.
     */
    private String overviewViewTitle;
    /**
     * List of MoleculeDataModels to be displayed in the overview view.
     */
    private List<MoleculeDataModel> moleculeDataModelList;
    /**
     * Slot for the caching of a MoleculeDataModel in reaction to a left or right mouse click on a structure shown in
     * the overview view.
     */
    private MoleculeDataModel cachedMoleculeDataModel;
    /**
     * Integer value to hold the number of rows of structure images to be displayed per page.
     */
    private int rowsPerPage;
    /**
     * Integer value to hold the number of columns of structure images to be displayed per page.
     */
    private int columnsPerPage;
    /**
     * Double value to hold the height of the space taken up by the navigation bar at the bottom of the pagination node.
     */
    private double paginationSpecificElementsHeight;
    /**
     * Boolean value that defines, whether the structure images should be generated and shown when a new OverviewView
     * page gets created. This variable initially needs to be set to false and gets set to true with the first call of
     * the method createOverviewViewPage().
     */
    private boolean createStructureImages;
    /**
     * Context menu for the structure images.
     */
    private ContextMenu structureContextMenu;
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
        //creating an empty structureGridPane at first by setting createStructureImages to false
        this.createStructureImages = false;
        this.showOverviewView();
    }
    //</editor-fold>
    //
    //<editor-fold desc="private methods" dafaultstate="collapsed">
    /**
     * Initializes and opens the overview view. This method is only being called by the constructor.
     */
    private void showOverviewView() {
        if (this.overviewView == null)
            this.overviewView = new OverviewView(this.rowsPerPage, this.columnsPerPage);
        this.overviewViewStage = new Stage();
        Scene tmpScene = new Scene(this.overviewView, GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE,
                GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.overviewViewStage.setScene(tmpScene);
        this.overviewViewStage.initModality(Modality.WINDOW_MODAL);    //TODO: consider changing this?
        this.overviewViewStage.initOwner(this.mainStage);
        this.overviewViewStage.setTitle(this.overviewViewTitle);
        InputStream tmpImageInputStream = MainViewController.class.getResourceAsStream(
                "/de/unijena/cheminf/mortar/images/Mortar_Logo_Icon1.png"
        );
        this.overviewViewStage.getIcons().add(new Image(tmpImageInputStream));
        this.overviewViewStage.setMinHeight(GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.overviewViewStage.setMinWidth(GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE);
        //
        int tmpPageCount = this.moleculeDataModelList.size() / (this.rowsPerPage * this.columnsPerPage);
        if (this.moleculeDataModelList.size() % (this.rowsPerPage * this.columnsPerPage) > 0) {
            tmpPageCount++;
        }
        Pagination tmpPagination = new Pagination(tmpPageCount, 0);
        tmpPagination.setPageFactory((aPageIndex) -> this.createOverviewViewPage(aPageIndex,
                this.rowsPerPage, this.columnsPerPage));
        VBox.setVgrow(tmpPagination, Priority.ALWAYS);
        HBox.setHgrow(tmpPagination, Priority.ALWAYS);
        this.overviewView.addPaginationToGridPane(tmpPagination);
        this.overviewView.addNodeToMainGridPane(this.overviewView.getLeftHBox(), 0, 1, 1, 1);
        this.overviewView.addNodeToMainGridPane(this.overviewView.getRightHBox(), 2, 1, 1, 1);
        //
        this.structureContextMenu = this.generateContextMenuWithListeners(false);
        //
        this.addListeners();
        //
        this.overviewViewStage.showAndWait();
    }
    //
    /**
     * Adds listeners and event handlers to elements of the overview view.
     */
    private void addListeners() {
        this.overviewView.getApplyButton().setOnAction(actionEvent -> {
            try {
                this.applyChangeOfGridConfiguration();
            } catch (IllegalArgumentException anIllegalArgumentException) {
                OverviewViewController.LOGGER.log(Level.WARNING, anIllegalArgumentException.toString(),
                        anIllegalArgumentException);
                GuiUtil.guiExceptionAlert(
                        Message.get("OverviewView.Error.invalidTextFieldInput.title"),
                        Message.get("OverviewView.Error.invalidTextFieldInput.header"),
                        anIllegalArgumentException.toString(),
                        anIllegalArgumentException
                );
            }
        });
        //
        this.overviewView.getCloseButton().setOnAction(actionEvent -> {
            this.overviewViewStage.close();
        });
        //
        ChangeListener<Number> tmpStageSizeListener = (observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                System.out.println("Window resize event: " + this.overviewViewStage.getWidth() + " - "
                        + this.overviewViewStage.getHeight());
                this.createOverviewViewPage(this.overviewView.getPagination().getCurrentPageIndex(),
                        this.rowsPerPage, this.columnsPerPage);
            });
        };
        this.overviewViewStage.heightProperty().addListener(tmpStageSizeListener);
        this.overviewViewStage.widthProperty().addListener(tmpStageSizeListener);
        //
        this.overviewViewStage.setOnShown(windowEvent -> {
            System.out.println("setOnShown event");
            this.createOverviewViewPage(this.overviewView.getPagination().getCurrentPageIndex(),
                    this.rowsPerPage, this.columnsPerPage);
            System.out.println("setOnShown event end");
            //calculation of the pagination specific elements height to buffer future javafx intern changes
            this.paginationSpecificElementsHeight = this.overviewView.getPagination().getHeight()
                    - this.overviewView.getStructureGridPane().getHeight();
            System.out.println("tmpTest: " + this.paginationSpecificElementsHeight);
            System.out.println("StructureGridPane height: " + this.overviewView.getStructureGridPane().getHeight()
                    + "; StructureGridPane width: " + this.overviewView.getStructureGridPane().getWidth());
        });
    }
    //
    /**
     * Creates an overview view page according to the given page index. A GridPane containing structure images is being
     * returned.
     *
     * @param aPageIndex Index of the page to be created
     * @param aColumnsPerPage Number of columns per page
     * @param aRowsPerPage Number of rows per page
     * @return GridPane containing the structure images to be displayed on the current pagination page
     */
    private Node createOverviewViewPage(int aPageIndex, int aRowsPerPage, int aColumnsPerPage) {
        System.out.println("Call of createOverviewViewPage");
        this.overviewView.getStructureGridPane().getChildren().clear();
        //
        if (this.createStructureImages) {
            System.out.println("CurrentPageIndex: " + aPageIndex);
            int tmpFromIndex = aPageIndex * aRowsPerPage * aColumnsPerPage;
            int tmpToIndex = Math.min(tmpFromIndex + (aRowsPerPage * aColumnsPerPage), this.moleculeDataModelList.size());
            int tmpCurrentIndex = tmpFromIndex;
            //
            double tmpPaginationCellsHeight
                    = this.overviewView.getMainGridPane().getCellBounds(0, 0).getHeight()
                            + this.overviewView.getMainGridPane().getCellBounds(0, 1).getHeight();
            double tmpPaginationCellsWidth = 0;
            for (int i = 0; i < 3; i++) {
                tmpPaginationCellsWidth += this.overviewView.getMainGridPane().getCellBounds(i, 0).getWidth();
            }
            System.out.println("Cells height:" + tmpPaginationCellsHeight + "; Cells width: " + tmpPaginationCellsWidth);
            //
            //calculation of the structure images height and width using height and width of the mainGridPane cells
            // that hold the pagination; the usage of the structureGridPane and pagination dimensions caused issues at
            // resizing of the window; correction of the caused height deviation using the height of the pagination
            // specific elements; the further calculations create the space between the images creating the grid lines
            double tmpImageHeight = ((tmpPaginationCellsHeight - this.paginationSpecificElementsHeight -
                    GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH) / aRowsPerPage) -
                    GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH;
            double tmpImageWidth = ((tmpPaginationCellsWidth -
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
                            this.overviewViewStage.getScene().setCursor(Cursor.HAND);
                        });
                        tmpImageView.setOnMouseExited((mouseEvent) -> {
                            if (tmpDrawImagesWithShadow) {
                                tmpImageView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(100, 100, 100, 0.6), " +
                                        GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4 + ", 0, " +
                                        GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4 + ", " +
                                        GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4 + ")");
                            } else {
                                tmpImageView.setStyle("-fx-effect: null");
                            }
                            this.overviewViewStage.getScene().setCursor(Cursor.DEFAULT);
                        });
                        tmpImageView.setOnMouseClicked((aMouseEvent) -> {
                            if (MouseButton.PRIMARY.equals(aMouseEvent.getButton())) {
                                this.showEnlargedStructureView(tmpMoleculeDataModel);
                            }
                        });
                        //Setting context menu to the image view
                        tmpImageView.setOnContextMenuRequested((aContextMenuRequest) -> {
                            System.out.println("ContextMenuRequest");
                            this.getMoleculeDataModelCorrespondingToEvent(aContextMenuRequest);
                            this.structureContextMenu.show(tmpImageView, aContextMenuRequest.getScreenX(),
                                    aContextMenuRequest.getScreenY());
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
     * @param anEvent
     * @return
     */
    private MoleculeDataModel getMoleculeDataModelCorrespondingToEvent(Event anEvent) {
        int tmpRowIndex = GridPane.getRowIndex(((Node) anEvent.getSource()).getParent());
        int tmpColumnIndex = GridPane.getColumnIndex(((Node) anEvent.getSource()).getParent());
        int tmpIndexOfMolecule = this.overviewView.getPagination().getCurrentPageIndex() * this.rowsPerPage * this.columnsPerPage
                + tmpRowIndex * this.columnsPerPage + tmpColumnIndex;
        System.out.println("tmpRowIndex = " + tmpRowIndex);
        System.out.println("tmpColumnIndex = " + tmpColumnIndex);
        System.out.println("tmpIndexOfMolecule = " + tmpIndexOfMolecule);
        System.out.println("SMILES: " + this.moleculeDataModelList.get(tmpIndexOfMolecule).getUniqueSmiles());
        return this.cachedMoleculeDataModel = this.moleculeDataModelList.get(tmpIndexOfMolecule);
    }
    /**
     *
     *
     * @param aForEnlargedStructureView
     * @return
     */
    private ContextMenu generateContextMenuWithListeners(boolean aForEnlargedStructureView) {
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
                        this.cachedMoleculeDataModel.getAtomContainer(),
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
            tmpContent.putString(this.cachedMoleculeDataModel.getUniqueSmiles());
            Clipboard.getSystemClipboard().setContent(tmpContent);
            System.out.println("SMILES has been copied");
        });
        //add MenuItems and return ContextMenu
        tmpContextMenu.getItems().addAll(
                tmpCopyImageMenuItem,
                tmpCopySmilesMenuItem
        );
        if (!aForEnlargedStructureView) {
            //showStructureMenuItem
            MenuItem tmpShowStructureMenuItem = new MenuItem(Message.get("OverviewView.contextMenu.showStructureMenuItem"));
            tmpShowStructureMenuItem.setOnAction((ActionEvent event) -> {
                this.showEnlargedStructureView(this.cachedMoleculeDataModel);
            });
            tmpContextMenu.getItems().addAll(
                    new SeparatorMenuItem(),
                    tmpShowStructureMenuItem
            );
        }
        return tmpContextMenu;
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
    //
    /**
     *
     * @param aMoleculeDataModel
     */
    private void showEnlargedStructureView(MoleculeDataModel aMoleculeDataModel) {
        //TODO!!
        System.out.println("Enlarged structure is being shown!");

        Stage tmpEnlargedStructureViewStage = new Stage();
        AnchorPane tmpEnlargedStructureViewAnchorPane = new AnchorPane();
        Scene tmpScene = new Scene(tmpEnlargedStructureViewAnchorPane, 500, 400);
        tmpEnlargedStructureViewStage.setScene(tmpScene);
        tmpEnlargedStructureViewStage.initModality(Modality.WINDOW_MODAL);
        tmpEnlargedStructureViewStage.initOwner(this.overviewViewStage);
        tmpEnlargedStructureViewStage.setTitle(Message.get("OverviewView.enlargedStructureView.title"));
        InputStream tmpImageInputStream = MainViewController.class.getResourceAsStream(
                "/de/unijena/cheminf/mortar/images/Mortar_Logo_Icon1.png"
        );
        tmpEnlargedStructureViewStage.getIcons().add(new Image(tmpImageInputStream));
        tmpEnlargedStructureViewStage.setMinHeight(GuiDefinitions.ENLARGED_STRUCTURE_VIEW_MIN_HEIGHT_VALUE);
        tmpEnlargedStructureViewStage.setMinWidth(GuiDefinitions.ENLARGED_STRUCTURE_VIEW_MIN_WIDTH_VALUE);

        tmpEnlargedStructureViewStage.show();

        /*if (!this.enlargedStructureViewStage.isShowing()) {
            this.enlargedStructureViewStage.show();
        }*/

        ContextMenu tmpContextMenu = this.generateContextMenuWithListeners(true);

        try {
            ImageView tmpStructureImage = new ImageView(DepictionUtil.depictImage(aMoleculeDataModel.getAtomContainer(),
                    tmpEnlargedStructureViewAnchorPane.getWidth(), tmpEnlargedStructureViewAnchorPane.getHeight()));
            tmpEnlargedStructureViewAnchorPane.getChildren().add(tmpStructureImage);
            tmpStructureImage.setOnContextMenuRequested((event) -> {
                tmpContextMenu.show(tmpStructureImage, event.getScreenX(), event.getScreenY());
            });

            ChangeListener<Number> tmpStageSizeListener = (observable, oldValue, newValue) -> {
                Platform.runLater(() -> {
                    System.out.println("EnlargedStructureView resize event: " + tmpEnlargedStructureViewStage.getWidth()
                            + " - " + tmpEnlargedStructureViewStage.getHeight());
                    tmpEnlargedStructureViewAnchorPane.getChildren().clear();
                    try {
                        ImageView tmpUpdatedStructureImage = new ImageView(DepictionUtil.depictImage(aMoleculeDataModel.getAtomContainer(),
                                tmpEnlargedStructureViewAnchorPane.getWidth(), tmpEnlargedStructureViewAnchorPane.getHeight()));
                        tmpEnlargedStructureViewAnchorPane.getChildren().add(tmpUpdatedStructureImage);
                        tmpUpdatedStructureImage.setOnContextMenuRequested((event) -> {
                            tmpContextMenu.show(tmpUpdatedStructureImage, event.getScreenX(), event.getScreenY());
                        });
                    } catch (CDKException anException) {
                        //TODO ?!
                    }
                });
            };
            tmpEnlargedStructureViewStage.heightProperty().addListener(tmpStageSizeListener);
            tmpEnlargedStructureViewStage.widthProperty().addListener(tmpStageSizeListener);
        } catch (CDKException anException) {
            //TODO?! if an exception is thrown generating an AtomContainer this point is never reached
        }
    }
    //</editor-fold>

}
