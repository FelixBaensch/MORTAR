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
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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
import javafx.stage.WindowEvent;
import org.openscience.cdk.exception.CDKException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller class of the overview view.
 *
 * @author Samuel Behr
 * @version 1.0.0.0
 */
public class OverviewViewController {

    //<editor-fold desc="private static final class variables" defaultstate="collapsed">
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
     * OverviewView instance of the class.
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
     * Integer value that holds the number of rows of structure images to be displayed per page.
     */
    private int rowsPerPage;
    /**
     * Integer value that holds the number of columns of structure images to be displayed per page.
     */
    private int columnsPerPage;
    /**
     * Boolean value that defines, whether the structure images should be generated and shown when a new overview view
     * page gets created. This variable helps to avoid issues with the creation of the initial pagination page and
     * needs to be set to false at start.
     */
    private boolean createStructureImages;
    /**
     * Context menu for the structure images.
     */
    private ContextMenu structureContextMenu;
    //</editor-fold>
    //
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Constructor. Initializes the class variables and opens the overview view.
     */
    public OverviewViewController(Stage aStage, String aTabName, List<MoleculeDataModel> aMoleculeDataModelList) {
        //<editor-fold desc="checks" defaultstate="collapsed">  //@Felix, @Jonas: are these checks necessary? I added them after comparing my code with the MainViewController
        Objects.requireNonNull(aStage, "aStage (instance of Stage) is null");
        Objects.requireNonNull(aTabName, "aTabName (instance of String) is null");
        Objects.requireNonNull(aMoleculeDataModelList, "aMoleculeDataModelList (list of MoleculeDataModel instances) is null");
        //</editor-fold>
        OverviewViewController.LOGGER.info("Display of overview view for the \"" + aTabName + "\" tab.");  //@Felix, @Jonas: Do you guys want this info (or even more e.g. of events happening in the view e.g. copy of SMILES) to be logged?
        this.mainStage = aStage;
        this.overviewViewTitle = aTabName + " - " + Message.get("OverviewView.nameOfView");
        this.moleculeDataModelList = aMoleculeDataModelList;
        this.rowsPerPage = 5;   //TODO: settings! default values should be customizable
        this.columnsPerPage = 4;    //TODO: settings! default values should be customizable
        //creating an empty structureGridPane at first by setting createStructureImages to false
        this.createStructureImages = false;
        this.showOverviewView();
    }
    //</editor-fold>
    //
    //<editor-fold desc="private methods" dafaultstate="collapsed">
    /**
     * Initializes and opens the overview view. This method is only being called by the constructor.
     *
     * @Felix, @Jonas: should I remove the method and include the code into the constructor?!
     */
    private void showOverviewView() {
        if (this.overviewView == null)
            this.overviewView = new OverviewView(this.rowsPerPage, this.columnsPerPage);
        this.overviewViewStage = new Stage();
        Scene tmpScene = new Scene(this.overviewView, GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE,
                GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.overviewViewStage.setScene(tmpScene);
        this.overviewViewStage.initModality(Modality.WINDOW_MODAL);
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
        this.overviewView.addNodeToMainGridPane(this.overviewView.getBottomLeftHBox(), 0, 1, 1, 1);
        this.overviewView.addNodeToMainGridPane(this.overviewView.getBottomRightHBox(), 2, 1, 1, 1);
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
        //close window event EventFilter
        this.overviewViewStage.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, (this::closeWindowEvent));
        //resize events listener
        ChangeListener<Number> tmpStageSizeListener = (observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                this.createOverviewViewPage(this.overviewView.getPagination().getCurrentPageIndex(),
                        this.rowsPerPage, this.columnsPerPage);
            });
        };
        this.overviewViewStage.heightProperty().addListener(tmpStageSizeListener);
        this.overviewViewStage.widthProperty().addListener(tmpStageSizeListener);
        //apply button listener
        this.overviewView.getApplyButton().setOnAction(actionEvent -> {
            try {
                this.applyChangeOfGridConfiguration();
            } catch (IllegalArgumentException anIllegalArgumentException) {
                //@Felix, @Jonas: should I remove this point of logging since the exception is meant to be thrown?
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
        //close button listener
        this.overviewView.getCloseButton().setOnAction(actionEvent -> {
            this.closeOverviewView();
        });
    }
    //
    /**
     * Closes the overview view.
     */
    private void closeOverviewView() {
        OverviewViewController.LOGGER.info("Overview view has been closed.");
        this.overviewViewStage.close();
    }
    //
    /**
     * Closes the overview view via closeOverviewView method when close window event was fired.
     *
     * @param anEvent WindowEvent
     */
    private void closeWindowEvent(WindowEvent anEvent) {
        this.closeOverviewView();
        anEvent.consume();
    }
    //
    /**
     * Creates an overview view page according to the given page index. A GridPane containing structure images gets
     * returned. The SMILES and image of each structure can be copied and an enlarged version of the image be shown in
     * a separate view via a context menu. If an exception gets thrown during the depiction of a structure a label will
     * be placed instead of the structure image (as placeholder and to inform the user).
     * If the image dimensions fall below a defined minimum instead of the images a label is placed that spans the hole
     * grid and holds information for the user.
     *
     * @param aPageIndex Integer value for the index of the page to be created
     * @param aColumnsPerPage Integer value for the number of columns per page
     * @param aRowsPerPage Integer value for the number of rows per page
     * @return GridPane containing the structure images to be displayed on the current pagination page or a label with
     * information for the user if the image dimensions fall below a predefined limit
     */
    private Node createOverviewViewPage(int aPageIndex, int aRowsPerPage, int aColumnsPerPage) {
        this.overviewView.getStructureGridPane().getChildren().clear();
        //
        if (this.createStructureImages) {
            int tmpFromIndex = aPageIndex * aRowsPerPage * aColumnsPerPage;
            int tmpToIndex = Math.min(tmpFromIndex + (aRowsPerPage * aColumnsPerPage), this.moleculeDataModelList.size());
            int tmpIterator = tmpFromIndex;
            //
            //calculation of height and width of the cells of the mainGridPane that hold the pagination node
            double tmpMainGridPanePaginationNodeCellsHeight
                    = this.overviewView.getMainGridPane().getCellBounds(0, 0).getHeight()
                            + this.overviewView.getMainGridPane().getCellBounds(0, 1).getHeight();
            double tmpPaginationCellsWidth = 0;
            for (int i = 0; i < 3; i++) {
                tmpPaginationCellsWidth += this.overviewView.getMainGridPane().getCellBounds(i, 0).getWidth();
            }
            //
            /*
            calculation of the structure images' height and width using height and width of the mainGridPane cells that
            hold the pagination; the usage of the structureGridPane and pagination dimensions caused issues at resizing
            of the window; correction of the caused height deviation by the pagination control panel height; the
            further calculations generate the space between the images creating the grid lines
             */
            double tmpImageHeight = ((tmpMainGridPanePaginationNodeCellsHeight -
                    GuiDefinitions.GUI_PAGINATION_CONTROL_PANEL_HEIGHT -
                    GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH) / aRowsPerPage) -
                    GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH;
            double tmpImageWidth = ((tmpPaginationCellsWidth -
                    (2 * (GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_BORDER_GRIDLINES_WIDTH_RATIO - 0.5) *
                    GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH)) / aColumnsPerPage) -
                    GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH;
            //
            //check if the limits for the image dimensions are being exceeded
            if ((tmpImageHeight >= 20.0) && (tmpImageWidth >= 30.0)) {  //TODO: values to settings?! values need to be > 0 but images only start to make sense at values about 20 x 30 px
                //shadow setting
                boolean tmpDrawImagesWithShadow = true;    //TODO: place shadow option in settings
                //main loop for generation of the page content
                generationOfStructureImagesLoop:
                for (int i = 0; i < aRowsPerPage; i++) {
                    for (int j = 0; j < aColumnsPerPage; j++) {
                        if (tmpIterator >= tmpToIndex) {
                            break generationOfStructureImagesLoop;
                        }
                        StackPane tmpStackPane = new StackPane();
                        Node tmpContentNode;
                        try {
                            //depiction of structure image
                            MoleculeDataModel tmpMoleculeDataModel = this.moleculeDataModelList.get(tmpIterator);
                            ImageView tmpImageView = new ImageView(DepictionUtil.depictImageWithZoom(
                                    tmpMoleculeDataModel.getAtomContainer(),
                                    1.0, tmpImageWidth, tmpImageHeight
                            )); //TODO: check use of fill to fit method after merge
                            //changing the shadow effects at mouse entering an image
                            tmpImageView.setOnMouseEntered((mouseEvent) -> {
                                if (tmpDrawImagesWithShadow) {
                                    tmpImageView.setStyle("-fx-effect: null");
                                } else {
                                    tmpImageView.setStyle("-fx-effect: dropshadow(three-pass-box, " +
                                            "rgba(100, 100, 100, 0.6), " +
                                            GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH + ", " +
                                            "0, 0, 0)");
                                }
                            });
                            //resetting the shadow effects at mouse leaving the image
                            tmpImageView.setOnMouseExited((mouseEvent) -> {
                                if (tmpDrawImagesWithShadow) {
                                    tmpImageView.setStyle("-fx-effect: dropshadow(three-pass-box, " +
                                            "rgba(100, 100, 100, 0.6), " +
                                            GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4 +
                                            ", 0, " +
                                            GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4 + ", " +
                                            GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4 + ")");
                                } else {
                                    tmpImageView.setStyle("-fx-effect: null");
                                }
                            });
                            //opening the enlarged structure view on left mouse click
                            tmpImageView.setOnMouseClicked((aMouseEvent) -> {
                                if (MouseButton.PRIMARY.equals(aMouseEvent.getButton())) {
                                    this.cachedMoleculeDataModel = tmpMoleculeDataModel;
                                    //this.cacheMoleculeDataModelCorrespondingToEvent(aMouseEvent);
                                    this.showEnlargedStructureView(tmpMoleculeDataModel);
                                }
                            });
                            //setting context menu to the image view
                            tmpImageView.setOnContextMenuRequested((aContextMenuRequest) -> {
                                this.cachedMoleculeDataModel = tmpMoleculeDataModel;
                                //this.cacheMoleculeDataModelCorrespondingToEvent(aContextMenuRequest);
                                this.structureContextMenu.show(tmpImageView, aContextMenuRequest.getScreenX(),
                                        aContextMenuRequest.getScreenY());
                            });
                            //
                            tmpContentNode = tmpImageView;
                        } catch (CDKException anException) {
                            OverviewViewController.LOGGER.log(Level.WARNING, anException.toString(), anException);
                            //Error label to be shown when a structure can not be depicted
                            Label tmpErrorLabel = new Label(Message.get("OverviewView.ErrorLabel.text"));
                            tmpErrorLabel.setMinWidth(tmpImageWidth);
                            tmpErrorLabel.setMaxWidth(tmpImageWidth);
                            tmpErrorLabel.setMinHeight(tmpImageHeight);
                            tmpErrorLabel.setMaxHeight(tmpImageHeight);
                            tmpErrorLabel.setStyle("-fx-alignment: CENTER; -fx-background-color: WHITE");
                            Tooltip tmpErrorLabelTooltip = new Tooltip(Message.get("OverviewView.ErrorLabel.tooltip"));
                            tmpErrorLabel.setTooltip(tmpErrorLabelTooltip);
                            tmpContentNode = tmpErrorLabel;
                        } catch (IndexOutOfBoundsException anIndexOutOfBoundsException) {
                            //could be thrown when accessing the moleculeDataModelList
                            OverviewViewController.LOGGER.log(Level.SEVERE, anIndexOutOfBoundsException.toString(),
                                    anIndexOutOfBoundsException);
                            //TODO: guiExceptionAlert needed?! @Felix, @Jonas
                            break generationOfStructureImagesLoop;
                        }
                        //setting of shadow effects
                        if (tmpDrawImagesWithShadow) {
                            tmpContentNode.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(100, 100, 100, 0.6), " +
                                    GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4 + ", 0, " +
                                    GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4 + ", " +
                                    GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4 + ")");
                        }
                        //
                        tmpStackPane.getChildren().add(tmpContentNode);
                        this.overviewView.getStructureGridPane().add(tmpStackPane, j, i);
                        tmpIterator++;
                    }
                }
            } else {
                //creation of label to be displayed if the image dimensions fell below a limit
                StackPane tmpStackPane = new StackPane();
                Label tmpImageDimensionsBelowLimitLabel = new Label(
                        Message.get("OverviewView.imageDimensionsBelowLimitLabel.text")
                );
                tmpImageDimensionsBelowLimitLabel.setStyle("-fx-alignment: CENTER");
                Tooltip tmpImageDimensionsBelowLimitLabelTooltip = new Tooltip(
                        Message.get("OverviewView.imageDimensionsBelowLimitLabel.tooltip")  //TODO: adapt tooltip after integration of overview view settings
                );
                tmpImageDimensionsBelowLimitLabel.setTooltip(tmpImageDimensionsBelowLimitLabelTooltip);
                tmpStackPane.getChildren().add(tmpImageDimensionsBelowLimitLabel);
                //
                this.overviewView.getStructureGridPane().add(tmpStackPane, 0, 0, this.columnsPerPage, this.rowsPerPage);
            }
        }
        this.createStructureImages = true;
        return this.overviewView.getStructureGridPane();
    }
    //
    /**
     * Caches the MoleculeDataModel corresponding to the content of the node the given event occurred on. The parent of
     * the node needs to be a child of the structureGridPane from the controller's OverviewView instance.
     *
     * @param anEvent Event of which the corresponding MoleculeDataModel should be cached of
     */
    private void cacheMoleculeDataModelCorrespondingToEvent(Event anEvent) {    //TODO: remove method?!
        //calculations to find the MoleculeDataModel instance corresponding to the content of the source of the event
        int tmpRowIndex = GridPane.getRowIndex(((Node) anEvent.getSource()).getParent());
        int tmpColumnIndex = GridPane.getColumnIndex(((Node) anEvent.getSource()).getParent());
        int tmpIndexOfMolecule = this.overviewView.getPagination().getCurrentPageIndex()
                * this.rowsPerPage * this.columnsPerPage
                + tmpRowIndex * this.columnsPerPage + tmpColumnIndex;
        this.cachedMoleculeDataModel = this.moleculeDataModelList.get(tmpIndexOfMolecule);
    }
    //
    /**
     * Generates and returns a context menu with listeners for view components. The context menu can be generated
     * without the option of opening an enlarged structure view. The actions are dependent on the currently cached
     * MoleculeDataModel instance.
     *
     * @param aForEnlargedStructureView Boolean value for whether the context menu gets generated for the use in the
     *                                  enlarged structure view
     * @return ContextMenu for view components
     */
    private ContextMenu generateContextMenuWithListeners(boolean aForEnlargedStructureView) {
        //context menu
        ContextMenu tmpContextMenu = new ContextMenu();
        //copyImageMenuItem
        MenuItem tmpCopyImageMenuItem = new MenuItem(Message.get("OverviewView.contextMenu.copyImageMenuItem"));
        tmpCopyImageMenuItem.setGraphic(new ImageView(new Image("de/unijena/cheminf/mortar/images/copy_icon_16x16.png")));
        //copySmilesMenuItem
        MenuItem tmpCopySmilesMenuItem = new MenuItem(Message.get("OverviewView.contextMenu.copySmilesMenuItem"));
        //adding Listeners to MenuItems
        //copyImageMenuItem listener
        tmpCopyImageMenuItem.setOnAction((ActionEvent event) -> {
            try {
                ClipboardContent tmpContent = new ClipboardContent();
                tmpContent.putImage(DepictionUtil.depictImageWithZoom(
                        this.cachedMoleculeDataModel.getAtomContainer(),
                        1.0, 512, 350      //TODO: size of copied image as shown or with pre defined values? place values in settings? @Felix, @Jonas
                )); //TODO: use Fill to fit method after merge
                Clipboard.getSystemClipboard().setContent(tmpContent);
            } catch (CDKException aCDKException) {
                OverviewViewController.LOGGER.log(Level.SEVERE, aCDKException.toString(), aCDKException);
                GuiUtil.guiExceptionAlert(
                        Message.get("Error.ExceptionAlert.Title"),
                        Message.get("Error.ExceptionAlert.Header"),
                        aCDKException.toString(),
                        aCDKException
                );
            }
        });
        //copySmilesMenuItem listener
        tmpCopySmilesMenuItem.setOnAction((ActionEvent event) -> {
            ClipboardContent tmpContent = new ClipboardContent();
            tmpContent.putString(this.cachedMoleculeDataModel.getUniqueSmiles());
            Clipboard.getSystemClipboard().setContent(tmpContent);
        });
        //add view-independent MenuItems
        tmpContextMenu.getItems().addAll(
                tmpCopyImageMenuItem,
                tmpCopySmilesMenuItem
        );
        //
        //add MenuItems that are only needed for overview view items
        if (!aForEnlargedStructureView) {
            //showStructureMenuItem
            MenuItem tmpShowStructureMenuItem = new MenuItem(Message.get("OverviewView.contextMenu.showStructureMenuItem"));
            tmpShowStructureMenuItem.setOnAction((ActionEvent event) -> {
                this.showEnlargedStructureView(this.cachedMoleculeDataModel);
            });
            //add view-dependent MenuItems
            tmpContextMenu.getItems().addAll(
                    new SeparatorMenuItem(),
                    tmpShowStructureMenuItem
            );
        }
        return tmpContextMenu;
    }
    //
    /**
     * Applies changes in rows and columns per page number to the configuration of structureGridPane of the controller's
     * OverviewView instance.
     *
     * @throws IllegalArgumentException if the rows or columns per page entry is less or equal to zero
     */
    private void applyChangeOfGridConfiguration() throws IllegalArgumentException {
        //@Felix, @Jonas: should a confirmation alert be shown if the user applies row and column inputs that do not fit to the current limits for structure image height and width?
        //validation of rows per page entry
        int tmpRowsPerPageEntry = Integer.parseInt(this.overviewView.getRowsPerPageTextField().getText());
        if (!(tmpRowsPerPageEntry <= 0)) {
            this.rowsPerPage = tmpRowsPerPageEntry;
        } else {
            throw new IllegalArgumentException(Message.get("OverviewView.Error.RowsPerPageTextField.illegalArgument")
                    + tmpRowsPerPageEntry);
        }
        //validation of columns per page entry
        int tmpColumnsPerPageEntry = Integer.parseInt(this.overviewView.getColumnsPerPageTextField().getText());
        if (!(tmpColumnsPerPageEntry <= 0)) {
            this.columnsPerPage = tmpColumnsPerPageEntry;
        } else {
            throw new IllegalArgumentException(Message.get("OverviewView.Error.ColumnsPerPageTextField.illegalArgument")
                    + tmpColumnsPerPageEntry);
        }
        //reconfiguration of the OverviewView instance's structureGridPane
        this.overviewView.configureStructureGridPane(this.rowsPerPage, this.columnsPerPage);
        //
        //adaptions to page count and current page index of the pagination node
        int tmpNewPageCount = this.moleculeDataModelList.size() / (this.rowsPerPage * this.columnsPerPage);
        if (this.moleculeDataModelList.size() % (this.rowsPerPage * this.columnsPerPage) > 0) {
            tmpNewPageCount++;
        }
        if (this.overviewView.getPagination().getPageCount() != tmpNewPageCount) {
            int tmpCurrentPageIndex = this.overviewView.getPagination().getCurrentPageIndex();
            this.overviewView.getPagination().setPageCount(tmpNewPageCount);
            if (tmpCurrentPageIndex >= tmpNewPageCount) {
                tmpCurrentPageIndex = tmpNewPageCount - 1;
            }
            this.overviewView.getPagination().setCurrentPageIndex(tmpCurrentPageIndex);
        }
        this.createOverviewViewPage(this.overviewView.getPagination().getCurrentPageIndex(),
                this.rowsPerPage, this.columnsPerPage);
    }
    //
    /**
     * Opens and shows the enlarged structure view. The view shows an enlarged version of the given structure and gives
     * the options to copy the image or the SMILES string of the structure.
     *
     * @param aMoleculeDataModel MoleculeDataModel of the structure to be shown in the enlarged structure view
     */
    private void showEnlargedStructureView(MoleculeDataModel aMoleculeDataModel) {
        //initialization of the view
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
        //
        tmpEnlargedStructureViewStage.show();
        //
        //generation of context menu for the options of copying the structure's image or SMILES String to the clipboard
        ContextMenu tmpContextMenu = this.generateContextMenuWithListeners(true);
        //
        try {
            //depiction of the structure
            ImageView tmpStructureImage = new ImageView(DepictionUtil.depictImage(aMoleculeDataModel.getAtomContainer(),
                    tmpEnlargedStructureViewAnchorPane.getWidth(), tmpEnlargedStructureViewAnchorPane.getHeight()));
            tmpEnlargedStructureViewAnchorPane.getChildren().add(tmpStructureImage);
            tmpStructureImage.setOnContextMenuRequested((event) -> {
                tmpContextMenu.show(tmpStructureImage, event.getScreenX(), event.getScreenY());
            });
            //listener for resize events to fit the structure depiction to the view size
            ChangeListener<Number> tmpStageResizeEventListener = (observable, oldValue, newValue) -> {
                Platform.runLater(() -> {
                    tmpEnlargedStructureViewAnchorPane.getChildren().clear();
                    try {
                        ImageView tmpUpdatedStructureImage = new ImageView(DepictionUtil.depictImage(
                                aMoleculeDataModel.getAtomContainer(),
                                tmpEnlargedStructureViewAnchorPane.getWidth(),
                                tmpEnlargedStructureViewAnchorPane.getHeight()
                        )); //TODO: use Fill to fit method after merge
                        tmpEnlargedStructureViewAnchorPane.getChildren().add(tmpUpdatedStructureImage);
                        tmpUpdatedStructureImage.setOnContextMenuRequested((event) -> {
                            tmpContextMenu.show(tmpUpdatedStructureImage, event.getScreenX(), event.getScreenY());
                        });
                    } catch (CDKException aCDKException) {
                        //logging and guiMessageAlert at issues with structure depiction
                        OverviewViewController.LOGGER.log(Level.SEVERE, aCDKException.toString(), aCDKException);
                        GuiUtil.guiMessageAlert(
                                Alert.AlertType.WARNING,
                                Message.get("OverviewView.enlargedStructureView.issueWithStructureDepiction.title"),
                                Message.get("OverviewView.enlargedStructureView.issueWithStructureDepiction.header"),
                                Message.get("OverviewView.enlargedStructureView.issueWithStructureDepiction.text")
                        );
                        tmpEnlargedStructureViewStage.close();
                    }
                });
            };
            tmpEnlargedStructureViewStage.heightProperty().addListener(tmpStageResizeEventListener);
            tmpEnlargedStructureViewStage.widthProperty().addListener(tmpStageResizeEventListener);
        } catch (CDKException aCDKException) {
            //logging and guiMessageAlert at issues with structure depiction
            OverviewViewController.LOGGER.log(Level.SEVERE, aCDKException.toString(), aCDKException);
            GuiUtil.guiMessageAlert(
                    Alert.AlertType.WARNING,
                    Message.get("OverviewView.enlargedStructureView.issueWithStructureDepiction.title"),
                    Message.get("OverviewView.enlargedStructureView.issueWithStructureDepiction.header"),
                    Message.get("OverviewView.enlargedStructureView.issueWithStructureDepiction.text")
            );
            tmpEnlargedStructureViewStage.close();
        }
    }
    //</editor-fold>

}
