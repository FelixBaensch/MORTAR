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

import de.unijena.cheminf.mortar.gui.controls.CustomPaginationSkin;
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
import javafx.event.EventHandler;
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
     * Integer value that holds the number of rows of structure images to be displayed per page.
     */
    private int rowsPerPage;
    /**
     * Integer value that holds the number of columns of structure images to be displayed per page.
     */
    private int columnsPerPage;
    /**
     * Integer value that holds the MoleculeDataModelList-index of the structure that has been the target of the latest
     * left or right mouse click on an image view in the overview view. (Workaround for cells not being selectable in a
     * GridPane.)
     */
    private int cachedIndexOfStructureInMoleculeDataModelList;
    /**
     * Boolean value that defines, whether the structure images should be generated and shown when a new overview view
     * page gets created. This variable helps to avoid issues with the creation of the initial pagination page and
     * needs to be set to false at start.
     */
    private boolean createStructureImages;
    /**
     * Boolean value whether the option to show a specific structure in the main view via double-click or context menu
     * should be available.
     */
    private boolean withShowInMainViewOption;
    /**
     * Boolean value whether an event to return to a specific structure in the MainView occurred.
     */
    private boolean returnToStructureEventOccurred;
    /**
     * Context menu for the structure images.
     */
    private ContextMenu structureContextMenu;
    //</editor-fold>
    //
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Constructor. Initializes the class variables and opens the overview view.    TODO
     * TODO: persist grid configuration (and possible other settings)
     *
     * @param aMainStage Stage that is to be the owner of the overview view's stage
     * @param aDataSource TODO
     * @param aTabName String containing the name of the tab that's content is to be shown in the overview view TODO
     * @param aMoleculeDataModelList List of MoleculeDataModel instances
     * @throws NullPointerException if one of the parameters is null    TODO
     */
    public OverviewViewController(Stage aMainStage, DataSources aDataSource, String aTabName, List<MoleculeDataModel> aMoleculeDataModelList)
            throws NullPointerException {
        //<editor-fold desc="checks" defaultstate="collapsed">
        Objects.requireNonNull(aMainStage, "aMainStage (instance of Stage) is null");
        Objects.requireNonNull(aDataSource, "aDataSource (enum value of DataSources) is null");
        Objects.requireNonNull(aMoleculeDataModelList, "aMoleculeDataModelList (list of MoleculeDataModel instances) is null");
        //</editor-fold>
        switch (aDataSource) {
            case FRAGMENTS_TAB_PARENT_MOLECULES -> {
                this.overviewViewTitle = Message.get("OverviewView.titleOfDataSource.parentMolecules") +
                        " - " + Message.get("OverviewView.nameOfView");
                this.withShowInMainViewOption = false;
            }
            case ITEMS_TAB -> {
                this.overviewViewTitle = Message.get("OverviewView.titleOfDataSource.itemsTab") +
                        " - " + Message.get("OverviewView.nameOfView");
                this.withShowInMainViewOption = false;
            }
            default -> {
                Objects.requireNonNull(aTabName, "aTabName (instance of String) is null");  //TODO
                if (aTabName.isBlank()) {
                    OverviewViewController.LOGGER.log(Level.WARNING, "aTabName (instance of String) is blank"); //TODO
                }
                this.withShowInMainViewOption = true;
            }
        }
        this.mainStage = aMainStage;
        this.overviewViewTitle = aTabName + " - " + Message.get("OverviewView.nameOfView");
        this.moleculeDataModelList = aMoleculeDataModelList;
        this.rowsPerPage = GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_ROWS_PER_PAGE_DEFAULT;
        this.columnsPerPage = GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_COLUMNS_PER_PAGE_DEFAULT;
        //creating an empty structureGridPane at first by setting createStructureImages to false
        this.createStructureImages = false;
        //initialization with -1 as marker whether the value has been changed
        this.cachedIndexOfStructureInMoleculeDataModelList = -1;
        this.returnToStructureEventOccurred = false;
        this.initializeAndShowOverviewView();
    }
    //</editor-fold>
    //
    //<editor-fold desc="private methods" dafaultstate="collapsed">
    /**
     * Initializes and opens the overview view. This method is only called by the constructor.
     */
    private void initializeAndShowOverviewView() {
        if (this.overviewView == null)
            this.overviewView = new OverviewView(this.columnsPerPage, this.rowsPerPage);
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
        tmpPagination.setSkin(new CustomPaginationSkin(tmpPagination));
        tmpPagination.setPageFactory((aPageIndex) -> this.createOverviewViewPage(aPageIndex,
                this.rowsPerPage, this.columnsPerPage));
        VBox.setVgrow(tmpPagination, Priority.ALWAYS);
        HBox.setHgrow(tmpPagination, Priority.ALWAYS);
        this.overviewView.addPaginationToMainGridPane(tmpPagination);
        this.overviewView.addBottomLeftHBoxToMainGridPane();
        this.overviewView.addBottomRightHBoxToMainGridPane();
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
        //listener for resize events
        ChangeListener<Number> tmpStageSizeListener = (observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                this.createOverviewViewPage(this.overviewView.getPagination().getCurrentPageIndex(),
                        this.rowsPerPage, this.columnsPerPage);
            });
        };
        this.overviewViewStage.heightProperty().addListener(tmpStageSizeListener);
        this.overviewViewStage.widthProperty().addListener(tmpStageSizeListener);
        //
        //apply new grid configuration event handler
        EventHandler<ActionEvent> tmpApplyNewGridConfigurationEventHandler = (actionEvent) -> {
            this.applyChangeOfGridConfiguration(false);
        };
        //listeners for apply new grid configuration events
        this.overviewView.getApplyButton().setOnAction(tmpApplyNewGridConfigurationEventHandler);
        this.overviewView.getColumnsPerPageTextField().setOnAction(tmpApplyNewGridConfigurationEventHandler);
        this.overviewView.getRowsPerPageTextField().setOnAction(tmpApplyNewGridConfigurationEventHandler);
        //
        //default button listener
        this.overviewView.getDefaultButton().setOnAction((actionEvent) -> {
            this.applyChangeOfGridConfiguration(true);
        });
        //
        //close button listener
        this.overviewView.getCloseButton().setOnAction((actionEvent) -> {
            this.overviewViewStage.close();
        });
        //
        //focused property change listener for columns per page text field
        this.overviewView.getColumnsPerPageTextField().focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                if (this.overviewView.getColumnsPerPageTextField().getText().isBlank()
                        || Integer.parseInt(this.overviewView.getColumnsPerPageTextField().getText()) == 0) {
                    this.overviewView.getColumnsPerPageTextField().setText(Integer.toString(this.columnsPerPage));
                }
            }
        });
        //focused property change listener for rows per page text field
        this.overviewView.getRowsPerPageTextField().focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                if (this.overviewView.getRowsPerPageTextField().getText().isBlank()
                        || Integer.parseInt(this.overviewView.getRowsPerPageTextField().getText()) == 0) {
                    this.overviewView.getRowsPerPageTextField().setText(Integer.toString(this.rowsPerPage));
                }
            }
        });
        //
        //change listener for resetting not applied text field entries when switching pagination page
        this.overviewView.getPagination().currentPageIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (!this.overviewView.getColumnsPerPageTextField().getText()
                    .equals(Integer.toString(this.columnsPerPage))) {
                this.overviewView.getColumnsPerPageTextField().setText(Integer.toString(this.columnsPerPage));
            }
            if (!this.overviewView.getRowsPerPageTextField().getText()
                    .equals(Integer.toString(this.rowsPerPage))) {
                this.overviewView.getRowsPerPageTextField().setText(Integer.toString(this.rowsPerPage));
            }
        });
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
            /*
            calculation of height and width of the pagination node via height and width of the cells of the mainGridPane
            holding it; necessary since direct use of pagination height and width brought some issues
             */
            double tmpPaginationNodeHeight = this.calculateOverviewViewPaginationNodeHeight();
            double tmpPaginationNodeWidth = this.calculateOverviewViewPaginationNodeWidth();
            //
            /*
            calculation of the structure images' height and width using height and width of the mainGridPane cells that
            hold the pagination node (since the structureGridPane and pagination node size artificially inflate when
            lowering the window size); correction of the caused height deviation by the pagination control panel height;
            the further calculations generate the space between the images creating the grid lines; the addition of 0.5
            is a trick to avoid the basic rounding off of the width and height values when transferring them to amounts
            of pixels to prevent the misalignment of the view's components due to a loss in size by the images
             */
            double tmpImageHeight = ((tmpPaginationNodeHeight
                    - GuiDefinitions.GUI_PAGINATION_CONTROL_PANEL_HEIGHT
                    - GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH) / aRowsPerPage)
                    - GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH + 0.5;
            double tmpImageWidth = ((tmpPaginationNodeWidth
                    - (2 * GuiDefinitions.GUI_INSETS_VALUE
                            - GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH)) / aColumnsPerPage)
                    - GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH + 0.5;
            //
            //check if the limits for the image dimensions are being exceeded
            if ((tmpImageHeight >= GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_IMAGE_MIN_HEIGHT)
                    && (tmpImageWidth >= GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_IMAGE_MIN_WIDTH)) {
                //optional setting for change in usage of shadow effect
                boolean tmpDrawImagesWithShadow = true;
                //main loop for generation of the page content
                generationOfStructureImagesLoop:
                for (int i = 0; i < aRowsPerPage; i++) {
                    for (int j = 0; j < aColumnsPerPage; j++) {
                        if (tmpIterator >= tmpToIndex) {
                            break generationOfStructureImagesLoop;
                        }
                        Node tmpContentNode;
                        try {
                            //depiction of structure image
                            MoleculeDataModel tmpMoleculeDataModel;
                            if ((tmpMoleculeDataModel = this.moleculeDataModelList.get(tmpIterator)) == null) {
                                throw new NullPointerException("A MoleculeDataModel instance has been null.");
                            }
                            Node tmpImageView = new ImageView(
                                    DepictionUtil.depictImageWithZoomAndFillToFitAndWhiteBackground(
                                            tmpMoleculeDataModel.getAtomContainer(), 1.0, tmpImageWidth,
                                            tmpImageHeight, true, true
                                    )
                            );
                            //changing the shadow effects at mouse entering an image
                            tmpImageView.setOnMouseEntered((aMouseEvent) -> {
                                if (tmpDrawImagesWithShadow) {
                                    tmpImageView.setStyle("-fx-effect: null");
                                } else {
                                    tmpImageView.setStyle("-fx-effect: dropshadow(gaussian, " +
                                            "rgba(100, 100, 100, 0.8), " +
                                            GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH + ", " +
                                            "0, 0, 0)");
                                }
                            });
                            //resetting the shadow effects at mouse leaving the image
                            tmpImageView.setOnMouseExited((aMouseEvent) -> {
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
                                    //caching the index of the MoleculeDataModel that corresponds to the content of the
                                    //event's source; equivalent to the structure's grid pane cell being selected
                                    this.cachedIndexOfStructureInMoleculeDataModelList
                                            = this.getIndexOfStructureInMoleculeDataModelList(aMouseEvent);
                                    this.showEnlargedStructureView(tmpMoleculeDataModel, this.overviewViewStage);
                                }
                            });
                            //setting context menu to the image view
                            tmpImageView.setOnContextMenuRequested((aContextMenuRequest) -> {
                                //caching the index of the MoleculeDataModel that corresponds to the content of the
                                //event's source; equivalent to the structure's grid pane cell being selected
                                this.cachedIndexOfStructureInMoleculeDataModelList
                                        = this.getIndexOfStructureInMoleculeDataModelList(aContextMenuRequest);
                                this.structureContextMenu.show(tmpImageView, aContextMenuRequest.getScreenX(),
                                        aContextMenuRequest.getScreenY());
                            });
                            //
                            tmpContentNode = tmpImageView;
                        } catch (CDKException | NullPointerException anException) {
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
                            tmpContentNode = new StackPane(tmpErrorLabel);
                            tmpContentNode.disableProperty().set(true);
                        } catch (IndexOutOfBoundsException anIndexOutOfBoundsException) {
                            //should not happen
                            OverviewViewController.LOGGER.log(Level.SEVERE, anIndexOutOfBoundsException.toString(),
                                    anIndexOutOfBoundsException);
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
                        this.overviewView.getStructureGridPane().add(tmpContentNode, j, i);
                        tmpIterator++;
                    }
                }
            } else {
                //informing the user if the image dimensions fell below the defined limit
                this.overviewView.getStructureGridPane().add(this.overviewView.getImageDimensionsBelowLimitVBox(),
                        0, 0, this.columnsPerPage, this.rowsPerPage);
            }
            //set tooltips of the text fields with current max for columns and rows per page
            this.overviewView.getColumnsPerPageTextField().getTooltip().setText(
                    Message.get("OverviewView.columnsPerPageLabel.tooltip") + " " +
                            this.calculateMaxColumnsPerPage(tmpPaginationNodeWidth)
            );
            this.overviewView.getRowsPerPageTextField().getTooltip().setText(
                    Message.get("OverviewView.rowsPerPageLabel.tooltip") + " " +
                            this.calculateMaxRowsPerPage(tmpPaginationNodeHeight)
            );
        }
        //boolean to true since the image generation only needs to be prevented in the first call of the method
        this.createStructureImages = true;
        //
        return this.overviewView.getStructureGridPane();
    }
    //
    /**
     * Applies changes in rows and columns per page number to the configuration of structureGridPane of the controller's
     * OverviewView instance using the current entries in the columnsPerPage and rowsPerPage text field or using the
     * default values for both.
     * If one of the values exceeds the displayable maximum (depending on the current window size and the structure
     * image size limits), the value gets set to the maximal displayable value.
     *
     * @param aUseDefaultValues Boolean value whether the default grid configuration should be applied
     */
    private void applyChangeOfGridConfiguration(boolean aUseDefaultValues) {
        //getting the values for the new grid configuration
        int tmpNewColumnsPerPageValue;
        int tmpNewRowsPerPageValue;
        if (aUseDefaultValues) {
            //use the default values and adapt the content of the text fields
            tmpNewColumnsPerPageValue = GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_COLUMNS_PER_PAGE_DEFAULT;
            tmpNewRowsPerPageValue = GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_ROWS_PER_PAGE_DEFAULT;
            this.overviewView.getColumnsPerPageTextField().setText(Integer.toString(tmpNewColumnsPerPageValue));
            this.overviewView.getRowsPerPageTextField().setText(Integer.toString(tmpNewRowsPerPageValue));
        } else {
            //get the text field entries
            tmpNewColumnsPerPageValue = Integer.parseInt(this.overviewView.getColumnsPerPageTextField().getText());
            tmpNewRowsPerPageValue = Integer.parseInt(this.overviewView.getRowsPerPageTextField().getText());
        }
        /*
        checking whether the entries are valid; entries get set to zero if a user presses enter on an empty text field;
        if so, the user gets informed via a message alert and the empty text field gets reset to its former value via
        the focusedProperty change listener due to it loosing focus for a sec when the key is pressed
         */
        if (tmpNewColumnsPerPageValue <= 0 || tmpNewRowsPerPageValue <= 0) {
            GuiUtil.guiMessageAlert(
                    Alert.AlertType.ERROR,
                    Message.get("OverviewView.applyChangeOfGridConfiguration.messageAlert.title"),
                    Message.get("OverviewView.applyChangeOfGridConfiguration.messageAlert.header"),
                    Message.get("OverviewView.applyChangeOfGridConfiguration.messageAlert.text")
            );
            return;
        }
        //
        //calculation of max columns and rows per page with current window size and limits for structure image size
        int tmpMaxColumnsPerPage = this.calculateMaxColumnsPerPage(this.calculateOverviewViewPaginationNodeWidth());
        int tmpMaxRowsPerPage = this.calculateMaxRowsPerPage(this.calculateOverviewViewPaginationNodeHeight());
        //
        //if (no changes && values are valid) -> return
        if (tmpNewColumnsPerPageValue == this.columnsPerPage && tmpNewColumnsPerPageValue <= tmpMaxColumnsPerPage
                && tmpNewRowsPerPageValue == this.rowsPerPage && tmpNewRowsPerPageValue <= tmpMaxRowsPerPage) {
            return;
        }
        //
        //if the values exceed the max values for the current page, set them to their maximum
        //setting the columns per page
        if (tmpNewColumnsPerPageValue > tmpMaxColumnsPerPage) {
            this.columnsPerPage = tmpMaxColumnsPerPage;
            this.overviewView.getColumnsPerPageTextField().setText(String.valueOf(this.columnsPerPage));
        } else {
            this.columnsPerPage = tmpNewColumnsPerPageValue;
        }
        //setting the rows per page
        if (tmpNewRowsPerPageValue > tmpMaxRowsPerPage) {
            this.rowsPerPage = tmpMaxRowsPerPage;
            this.overviewView.getRowsPerPageTextField().setText(String.valueOf(this.rowsPerPage));
        } else {
            this.rowsPerPage = tmpNewRowsPerPageValue;
        }
        //
        //reconfiguration of the OverviewView instance's structureGridPane
        this.overviewView.configureStructureGridPane(this.columnsPerPage, this.rowsPerPage);
        //
        //aftermath (adaptions to page count and current page index of the pagination node)
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
        tmpCopyImageMenuItem.setOnAction((ActionEvent anActionEvent) -> {
            if (this.cachedIndexOfStructureInMoleculeDataModelList >= 0) {
                try {
                    ClipboardContent tmpContent = new ClipboardContent();
                    tmpContent.putImage(DepictionUtil.depictImageWithZoomAndFillToFitAndWhiteBackground(
                            this.moleculeDataModelList.get(this.cachedIndexOfStructureInMoleculeDataModelList)
                                    .getAtomContainer(), 1.0,
                            GuiDefinitions.GUI_COPY_IMAGE_IMAGE_WIDTH,
                            GuiDefinitions.GUI_COPY_IMAGE_IMAGE_HEIGHT,
                            true, true
                    ));
                    Clipboard.getSystemClipboard().setContent(tmpContent);
                } catch (CDKException aCDKException) {
                    //should not happen since an initial depiction is needed to make the context menu accessible to the user
                    OverviewViewController.LOGGER.log(Level.SEVERE, aCDKException.toString(), aCDKException);
                    GuiUtil.guiExceptionAlert(
                            Message.get("Error.ExceptionAlert.Title"),
                            Message.get("Error.ExceptionAlert.Header"),
                            aCDKException.toString(),
                            aCDKException
                    );
                }
            }
        });
        //copySmilesMenuItem listener
        tmpCopySmilesMenuItem.setOnAction((ActionEvent anActionEvent) -> {
            if (this.cachedIndexOfStructureInMoleculeDataModelList >= 0) {
                String tmpSmilesString;
                if (this.moleculeDataModelList.get(this.cachedIndexOfStructureInMoleculeDataModelList) != null) {
                    tmpSmilesString = this.moleculeDataModelList.get(this.cachedIndexOfStructureInMoleculeDataModelList).getUniqueSmiles();
                } else {
                    tmpSmilesString = "";
                }
                ClipboardContent tmpContent = new ClipboardContent();
                tmpContent.putString(tmpSmilesString);
                Clipboard.getSystemClipboard().setContent(tmpContent);
            }
        });
        //add view-independent MenuItems
        tmpContextMenu.getItems().addAll(
                tmpCopyImageMenuItem,
                tmpCopySmilesMenuItem
        );
        //add MenuItems that are only needed for overview view items
        if (!aForEnlargedStructureView) {
            //enlargedStructureViewMenuItem
            MenuItem tmpEnlargedStructureViewMenuItem = new MenuItem(Message.get("OverviewView.contextMenu.enlargedStructureViewMenuItem"));
            tmpEnlargedStructureViewMenuItem.setOnAction((ActionEvent anActionEvent) -> {
                if (this.cachedIndexOfStructureInMoleculeDataModelList >= 0) {
                    this.showEnlargedStructureView(
                            this.moleculeDataModelList.get(this.cachedIndexOfStructureInMoleculeDataModelList),
                            this.overviewViewStage
                    );
                }
            });
            //showInMainView
            MenuItem tmpShowInMainViewMenuItem = new MenuItem(Message.get("OverviewView.contextMenu.showInMainViewMenuItem"));
            if (this.withShowInMainViewOption) {
                tmpShowInMainViewMenuItem.setOnAction((ActionEvent anActionEvent) -> {
                    //the MoleculeDataModelList-index of the structure has already been cached
                    this.overviewViewStage.close();
                });
            } else {
                tmpShowInMainViewMenuItem.setDisable(true);
            }
            //add view-dependent MenuItems
            tmpContextMenu.getItems().addAll(
                    new SeparatorMenuItem(),
                    tmpEnlargedStructureViewMenuItem,
                    tmpShowInMainViewMenuItem
            );
        }
        return tmpContextMenu;
    }
    //
    /**
     * Gets the MoleculeDataModelList-index of the structure that's image view is the source of the given event.
     *
     * @param anEvent Event with the image view of a structure as source
     * @return Index of structure in the MoleculeDataModelList
     */
    private int getIndexOfStructureInMoleculeDataModelList(Event anEvent) {
        int tmpColIndex = GridPane.getColumnIndex(((ImageView) anEvent.getTarget()));
        int tmpRowIndex = GridPane.getRowIndex(((ImageView) anEvent.getTarget()));
        int tmpIndexOfStructureOnPage = tmpRowIndex * this.columnsPerPage + tmpColIndex;
        return this.overviewView.getPagination().getCurrentPageIndex() * this.columnsPerPage * this.rowsPerPage
                + tmpIndexOfStructureOnPage;
    }
    //
    /**
     * Calculates the height of the pagination node via the mainGridPane cells that hold it.
     *
     * @return Double value of height of cells holding the pagination node
     */
    private double calculateOverviewViewPaginationNodeHeight() {
        double tmpMainGridPanePaginationNodeCellsHeight
                = this.overviewView.getMainGridPane().getCellBounds(0, 0).getHeight()
                + this.overviewView.getMainGridPane().getCellBounds(0, 1).getHeight();
        return tmpMainGridPanePaginationNodeCellsHeight;
    }
    //
    /**
     * Calculates the width of the pagination node via the mainGridPane cells that hold it.
     *
     * @return Double value of width of cells holding the pagination node
     */
    private double calculateOverviewViewPaginationNodeWidth() {
        double tmpMainGridPanePaginationNodeCellsWidth = 0;
        for (int i = 0; i < 3; i++) {
            tmpMainGridPanePaginationNodeCellsWidth
                    += this.overviewView.getMainGridPane().getCellBounds(i, 0).getWidth();
        }
        return tmpMainGridPanePaginationNodeCellsWidth;
    }
    //
    /**
     * Calculates the maximum amount of columns of structure images that are displayable per overview view page with
     * the current window size and limit for structure image width.
     *
     * @param aOverviewViewPaginationNodeWidth Double value of the width of the pagination node calculated via the
     *                                         width of the mainGridPane cells holding it
     * @return Integer value of the maximum amount of structure image columns per page
     * @throws IllegalArgumentException if the given parameter is less than or equal to zero
     */
    private int calculateMaxColumnsPerPage(double aOverviewViewPaginationNodeWidth) throws IllegalArgumentException {
        if (aOverviewViewPaginationNodeWidth <= 0.0)
            throw new IllegalArgumentException("aOverviewViewPaginationNodeWidth (Double value) is < or = to zero.");
        //
        int tmpMaxColumnsPerPage = (int) ((aOverviewViewPaginationNodeWidth
                - (2 * GuiDefinitions.GUI_INSETS_VALUE
                        - GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH))
                / (GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_IMAGE_MIN_WIDTH
                        + GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH));
        return tmpMaxColumnsPerPage;
    }
    //
    /**
     * Calculates the maximum amount of rows of structure images that are displayable per overview view page with the
     * current window size and limit for structure image height.
     *
     * @param aOverviewViewPaginationNodeHeight Double value of the height of the pagination node calculated via the
     *                                          height of the mainGridPane cells holding it
     * @return Integer value of the maximum amount of structure image rows per page
     * @throws IllegalArgumentException if the given parameter is less than or equal to zero
     */
    private int calculateMaxRowsPerPage(double aOverviewViewPaginationNodeHeight) throws IllegalArgumentException {
        if (aOverviewViewPaginationNodeHeight <= 0.0)
            throw new IllegalArgumentException("aOverviewViewPaginationNodeHeight (Double value) is < or = to zero.");
        //
        int tmpMaxRowsPerPage = (int) ((aOverviewViewPaginationNodeHeight
                - GuiDefinitions.GUI_PAGINATION_CONTROL_PANEL_HEIGHT
                - GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH)
                / (GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_IMAGE_MIN_HEIGHT
                        + GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH));
        return tmpMaxRowsPerPage;
    }
    //
    /**
     * Opens and shows the enlarged structure view. The view shows an enlarged version of the given structure and gives
     * the options to copy the image or the SMILES string of the structure.
     *
     * @param aMoleculeDataModel MoleculeDataModel of the structure to be shown in the enlarged structure view
     * @param anOwnerStage Stage to own the enlarged structure view's stage
     * @throws NullPointerException if one of the given parameters is null
     */
    private void showEnlargedStructureView(MoleculeDataModel aMoleculeDataModel, Stage anOwnerStage)
            throws NullPointerException {
        //checks
        Objects.requireNonNull(aMoleculeDataModel, "aMoleculeDataModel (instance of MoleculeDataModel) is null");
        Objects.requireNonNull(anOwnerStage, "anOwnerStage (instance of Stage) is null");
        //initialization of the view
        Stage tmpEnlargedStructureViewStage = new Stage();
        StackPane tmpEnlargedStructureViewStackPane = new StackPane();
        tmpEnlargedStructureViewStackPane.setStyle(
                "-fx-background-color: WHITE; " +
                "-fx-effect: innershadow(gaussian, rgba(100, 100, 100, 0.9), " +
                        GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 2 + ", 0, 0, " +
                        GuiDefinitions.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 8 + ")"
        );
        Scene tmpScene = new Scene(tmpEnlargedStructureViewStackPane,
                GuiDefinitions.ENLARGED_STRUCTURE_VIEW_SCENE_INITIAL_WIDTH,
                GuiDefinitions.ENLARGED_STRUCTURE_VIEW_SCENE_INITIAL_HEIGHT);
        tmpEnlargedStructureViewStage.setScene(tmpScene);
        tmpEnlargedStructureViewStage.initModality(Modality.WINDOW_MODAL);
        tmpEnlargedStructureViewStage.initOwner(anOwnerStage);
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
            ImageView tmpStructureImage = new ImageView(DepictionUtil.depictImageWithZoomAndFillToFitAndWhiteBackground(
                    aMoleculeDataModel.getAtomContainer(),1.0,
                    GuiDefinitions.ENLARGED_STRUCTURE_VIEW_SCENE_INITIAL_WIDTH
                            * GuiDefinitions.ENLARGED_STRUCTURE_VIEW_IMAGE_TO_STACKPANE_SIZE_RATIO,
                    GuiDefinitions.ENLARGED_STRUCTURE_VIEW_SCENE_INITIAL_HEIGHT
                            * GuiDefinitions.ENLARGED_STRUCTURE_VIEW_IMAGE_TO_STACKPANE_SIZE_RATIO,
                    true, true
            ));
            tmpEnlargedStructureViewStackPane.getChildren().add(tmpStructureImage);
            tmpStructureImage.setOnContextMenuRequested((event) -> {
                tmpContextMenu.show(tmpStructureImage, event.getScreenX(), event.getScreenY());
            });
            //listener for resize events to fit the structure depiction to the view size
            ChangeListener<Number> tmpStageResizeEventListener = (observable, oldValue, newValue) -> {
                Platform.runLater(() -> {
                    tmpEnlargedStructureViewStackPane.getChildren().clear();
                    try {
                        ImageView tmpUpdatedStructureImage = new ImageView(
                                DepictionUtil.depictImageWithZoomAndFillToFitAndWhiteBackground(
                                        aMoleculeDataModel.getAtomContainer(), 1.0,
                                        tmpEnlargedStructureViewStackPane.getWidth()
                                                * GuiDefinitions.ENLARGED_STRUCTURE_VIEW_IMAGE_TO_STACKPANE_SIZE_RATIO,
                                        tmpEnlargedStructureViewStackPane.getHeight()
                                                * GuiDefinitions.ENLARGED_STRUCTURE_VIEW_IMAGE_TO_STACKPANE_SIZE_RATIO,
                                        true, true
                                )
                        );
                        tmpEnlargedStructureViewStackPane.getChildren().add(tmpUpdatedStructureImage);
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
    //
    //<editor-fold desc="public properties" defaultstate="collapsed">
    /**
     * Returns the index of the structure in the MoleculeDataModelList that has been the target of the latest left- or
     * right-click on an image view in the overview view if an event to return to a specific structure in the main view
     * occurred; else -1 is returned.
     * @return Integer value of the cached index of structure or -1
     */
    public int getCachedIndexOfStructureInMoleculeDataModelList() {
        if (!this.returnToStructureEventOccurred) {
            return -1;
        }
        return this.cachedIndexOfStructureInMoleculeDataModelList;
    }
    //</editor-fold>
    //
    //<editor-fold desc="enum" defaultstate="collapsed">
    /**
     * Enum for different data sources data to be displayed in the overview view.
     */
    public enum DataSources {
        /**
         * Enum value for the data source molecules tab.
         */
        MOLECULES_TAB,
        /**
         * Enum value for the data source fragments tab.
         */
        FRAGMENTS_TAB,
        /**
         * Enum value for the data source fragments tab parent molecules.
         */
        FRAGMENTS_TAB_PARENT_MOLECULES,
        /**
         * Enum value for the data source items tab.
         */
        ITEMS_TAB
    }
    //</editor-fold>

}
