/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2024  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.controller;

import de.unijena.cheminf.mortar.configuration.IConfiguration;
import de.unijena.cheminf.mortar.gui.controls.CustomPaginationSkin;
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.gui.views.OverviewView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.depict.DepictionUtil;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.openscience.cdk.exception.CDKException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller class of the overview view.
 *
 * @author Samuel Behr, Jonas Schaub
 * @version 1.0.1.0
 */
public class OverviewViewController implements IViewToolController {
    //<editor-fold desc="Enum DataSources" defaultstate="collapsed">
    /**
     * Enum for different data sources whose data can be displayed in the overview view.
     */
    public static enum DataSources {
        /**
         * Enum value for the molecules tab as data source.
         */
        MOLECULES_TAB,
        /**
         * Enum value for a fragments tab as data source.
         */
        FRAGMENTS_TAB,
        /**
         * Enum value for a parent molecules sample of a fragments tab as data source.
         */
        PARENT_MOLECULES_SAMPLE,
        /**
         * Enum value for an item of an items tab as data source.
         */
        ITEM_WITH_FRAGMENTS_SAMPLE;
    }
    //</editor-fold>
    //
    //<editor-fold desc="public static final class constants" defaultstate="collapsed">
    /**
     * View tool name for display in the GUI.
     */
    public static final String VIEW_TOOL_NAME_FOR_DISPLAY = Message.get("MainView.menuBar.viewsMenu.overviewViewMenuItem.text");
    /**
     * Width of grid lines of the structure grid pane in overview view.
     */
    public static final double OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH = 8.0;
    /**
     * Minimum value for the width of structure images displayed in the overview view.
     */
    public static final double OVERVIEW_VIEW_STRUCTURE_IMAGE_MIN_WIDTH = 30.0;
    /**
     * Minimum value for the height of structure images displayed in the overview view.
     */
    public static final double OVERVIEW_VIEW_STRUCTURE_IMAGE_MIN_HEIGHT = 20.0;
    /**
     * Default value for columns of structure images per overview view page.
     */
    public static final int OVERVIEW_VIEW_STRUCTURE_GRID_PANE_COLUMNS_PER_PAGE_DEFAULT = 5;
    /**
     * Default value for rows of structure images per overview view page.
     */
    public static final int OVERVIEW_VIEW_STRUCTURE_GRID_PANE_ROWS_PER_PAGE_DEFAULT = 5;
    /**
     * Minimum value for the width of the enlarged structure view.
     */
    public static final double ENLARGED_STRUCTURE_VIEW_MIN_WIDTH_VALUE = 250.0;
    /**
     * Minimum value for the height of the enlarged structure view.
     */
    public static final double ENLARGED_STRUCTURE_VIEW_MIN_HEIGHT_VALUE = 200.0;
    /**
     * Initial width of the enlarged structure view's scene.
     */
    public static final double ENLARGED_STRUCTURE_VIEW_SCENE_INITIAL_WIDTH = 500.0;
    /**
     * Initial height of the enlarged structure view's scene.
     */
    public static final double ENLARGED_STRUCTURE_VIEW_SCENE_INITIAL_HEIGHT = 400.0;
    /**
     * Ratio of width and height of the enlarged structure view's structure image to the one of its parent stack pane.
     */
    public static final double ENLARGED_STRUCTURE_VIEW_IMAGE_TO_STACK_PANE_SIZE_RATIO = 0.9;
    //</editor-fold>
    //
    //<editor-fold desc="private static final class constants" defaultstate="collapsed">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(OverviewViewController.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="private final class constants" defaultstate="collapsed">
    /**
     * Configuration class to read resource file paths from.
     */
    private final IConfiguration configuration;
    /**
     * Integer property that holds the number of rows of structure images to be displayed per page.
     */
    private final SimpleIntegerProperty rowsPerPageSetting;
    /**
     * Integer property that holds the number of columns of structure images to be displayed per page.
     */
    private final SimpleIntegerProperty columnsPerPageSetting;
    /**
     * All settings of this view tool, encapsulated in JavaFX properties for binding in GUI and persistence.
     */
    private final List<Property<?>> settings;
    //</editor-fold>
    //
    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * Main stage object of the application.
     */
    private Stage mainStage;
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
     * Source of the data to be displayed in the overview view.
     */
    private DataSources dataSource;
    /**
     * List of MoleculeDataModels to be displayed in the overview view.
     */
    private List<MoleculeDataModel> moleculeDataModelList;
    /**
     * Integer value that holds the MoleculeDataModelList index of the structure that has been the target of the latest
     * left or right mouse click on an image view in the overview view. (Workaround for cells not being selectable in a
     * GridPane.)
     */
    private int cachedIndexOfStructureInMoleculeDataModelList;
    /**
     * Boolean value that defines whether the structure images should be generated and shown when a new overview view
     * page is created. This variable helps to avoid issues with the creation of the initial pagination page and
     * needs to be set to false at start.
     */
    private boolean createStructureImages;
    /**
     * Boolean value saying whether the option to show a specific structure in the main view via double-click or context menu
     * should be available. The value depends on the given data source.
     */
    private boolean withShowInMainViewOption;
    /**
     * Boolean value saying whether the first structure in the overview should be highlighted, e.g. because it is
     * a fragment and all other structures are its parents or because it is the parent structure and all other structures
     * are its fragments.
     */
    private boolean withFirstStructureHighlight;
    /**
     * Boolean value whether an event to return to a specific structure in the MainView occurred.
     */
    private boolean returnToStructureEventOccurred;
    /**
     * Context menu for the structure images.
     */
    private ContextMenu structureContextMenu;
    /**
     * Scheduled thread pool executor for scheduling the task of single-click events.
     */
    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    /**
     * Scheduled future for scheduling the task of single-click events.
     */
    private ScheduledFuture<?> scheduledFuture;
    /**
     * Boolean value to distinguish between drag and mouse click events.
     */
    private boolean dragFlag;
    //</editor-fold>
    //
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Constructor, initialises all settings with their default values. Does *not* open the view.
     *
     * @param aConfiguration configuration instance to read resource file paths from
     */
    public OverviewViewController(IConfiguration aConfiguration) {
        this.configuration = aConfiguration;
        this.settings = new ArrayList<>(2);
        this.rowsPerPageSetting = new SimpleIntegerProperty(this,
                //the name could be displayed but is not used for that currently
                Message.get("OverviewView.rowsPerPageSetting.name"),
                OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_ROWS_PER_PAGE_DEFAULT) {
            @Override
            public void set(int newValue) throws NullPointerException, IllegalArgumentException {
                super.set(newValue);
                //value transferred to GUI in initializeAndShowOverviewView()
                //value updated in applyChangeOfGridConfiguration()
            }
        };
        this.settings.add(this.rowsPerPageSetting);
        this.columnsPerPageSetting = new SimpleIntegerProperty(this,
                //the name could be displayed but is not used for that currently
                Message.get("OverviewView.columnsPerPageSetting.name"),
                OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_COLUMNS_PER_PAGE_DEFAULT) {
            @Override
            public void set(int newValue) throws NullPointerException, IllegalArgumentException {
                super.set(newValue);
                //value transferred to GUI in initializeAndShowOverviewView()
                //value updated in applyChangeOfGridConfiguration()
            }
        };
        this.settings.add(this.columnsPerPageSetting);
        //creating an empty structureGridPane at first by setting createStructureImages to false
        this.createStructureImages = false;
        //initializing the cached index with -1 as marker whether the value has been changed
        this.cachedIndexOfStructureInMoleculeDataModelList = -1;
        this.returnToStructureEventOccurred = false;
        this.scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        this.scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true);
        this.dragFlag = false;
    }
    //</editor-fold>
    //
    //<editor-fold desc="public properties" defaultstate="collapsed">
    /**
     * Returns the index of the structure in the MoleculeDataModelList that has been the latest target to a left- or
     * right-click on its image view if an event to return to a specific structure in the main view occurred; else -1
     * is returned.
     *
     * @return Integer value of the cached index of structure or -1
     */
    public int getCachedIndexOfStructureInMoleculeDataModelList() {
        if (!this.returnToStructureEventOccurred) {
            return -1;
        }
        return this.cachedIndexOfStructureInMoleculeDataModelList;
    }
    /**
     * Resets the index of the structure in the given molecules list cache that the view was supposed to jump to at
     * closing the overview view.
     */
    public void resetCachedIndexOfStructureInMoleculeDataModelList() {
        this.returnToStructureEventOccurred = false;
        this.cachedIndexOfStructureInMoleculeDataModelList = -1;
    }
    //</editor-fold>
    //
    //<editor-fold desc="public methods">
    //<editor-fold desc="public methods inherited from IViewToolController" defaultstate="collapsed">
    /**
     * {@inheritDoc}
     *
     * <p>
     *     For the overview view, settings can be configured via the properties only(!) between instantiating the object and
     *     opening the view using .initializeAndShowOverviewView().
     * </p>
     */
    @Override
    public List<Property<?>> settingsProperties() {
        //note: see comments in constructor for how the setting values are transferred in both directions (GUI <-> Properties)
        return this.settings;
    }
    @Override
    public String getViewToolNameForDisplay() {
        return OverviewViewController.VIEW_TOOL_NAME_FOR_DISPLAY;
    }
    /**
     * {@inheritDoc}
     *
     * <p>
     *     Note: For the overview view, the settings will only be updated in the GUI when the view is reopened!
     * </p>
     */
    @Override
    public void restoreDefaultSettings() {
        this.rowsPerPageSetting.set(OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_ROWS_PER_PAGE_DEFAULT);
        this.columnsPerPageSetting.set(OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_COLUMNS_PER_PAGE_DEFAULT);
    }
    @Override
    public boolean canBeUsedOnTab(TabNames aTabNameEnumConstant) {
        return switch (aTabNameEnumConstant) {
            case MOLECULES, FRAGMENTS -> true;
            default -> false;
        };
    }
    //</editor-fold>
    /**
     * Initializes and opens the overview view.
     *
     * @param aMainStage Stage that is to be the owner of the overview view's stage
     * @param aDataSource Source of the data to be shown in the overview view
     * @param aTabName String containing the name of the tab that's content is to be shown in the overview view
     * @param aMoleculeDataModelList List of MoleculeDataModel instances
     * @throws NullPointerException if one of the parameters is null; aTabName is allowed to be null if the value of aDataSource is
     *         PARENT_MOLECULES_SAMPLE or ITEM_WITH_FRAGMENTS_SAMPLE
     */
    public void initializeAndShowOverviewView(
            Stage aMainStage,
            DataSources aDataSource,
            String aTabName,
            List<MoleculeDataModel> aMoleculeDataModelList)
            throws NullPointerException {
        //<editor-fold desc="checks" defaultstate="collapsed">
        Objects.requireNonNull(aMainStage, "aMainStage (instance of Stage) is null");
        Objects.requireNonNull(aDataSource, "aDataSource (enum value of DataSources) is null");
        Objects.requireNonNull(aMoleculeDataModelList, "aMoleculeDataModelList (list of MoleculeDataModel instances) is null");
        //</editor-fold>
        switch (aDataSource) {
            case MOLECULES_TAB, FRAGMENTS_TAB -> {
                Objects.requireNonNull(aTabName, "aTabName (instance of String) is null");
                if (aTabName.isBlank()) {
                    OverviewViewController.LOGGER.log(Level.WARNING, "aTabName (instance of String) is blank");
                }
                this.overviewViewTitle = aTabName + " - " + Message.get("OverviewView.nameOfView") +
                        " - " + aMoleculeDataModelList.size() + " " +
                        Message.get((aDataSource == DataSources.MOLECULES_TAB
                                ? "OverviewView.titleOfView.molecule" : "OverviewView.titleOfView.fragment")
                                + (aMoleculeDataModelList.size() != 1 ? "s" : ""));
                this.withShowInMainViewOption = true;
                this.withFirstStructureHighlight = false;
            }
            case PARENT_MOLECULES_SAMPLE -> {
                this.overviewViewTitle = Message.get("OverviewView.titleOfDataSource.parentMolecules") +
                        " - " + Message.get("OverviewView.nameOfView") +
                        " - " + (aMoleculeDataModelList.size() - 1) +  " " +
                        Message.get(((aMoleculeDataModelList.size() - 1 == 1) ? "OverviewView.titleOfView.molecule"
                                : "OverviewView.titleOfView.molecules"));
                this.withShowInMainViewOption = false;
                this.withFirstStructureHighlight = true;
            }
            case ITEM_WITH_FRAGMENTS_SAMPLE -> {
                this.overviewViewTitle = Message.get("OverviewView.titleOfDataSource.itemsTab") +
                        " - " + Message.get("OverviewView.nameOfView") +
                        " - " + (aMoleculeDataModelList.size() - 1) + " " +
                        Message.get(((aMoleculeDataModelList.size() - 1 == 1) ? "OverviewView.titleOfView.fragment"
                                : "OverviewView.titleOfView.fragments"));
                this.withShowInMainViewOption = false;
                this.withFirstStructureHighlight = true;
            }
            default -> {
                this.setOverviewViewTitleForDefaultDataSource(aTabName, aMoleculeDataModelList.size());
            }
        }
        this.mainStage = aMainStage;
        this.dataSource = aDataSource;
        this.moleculeDataModelList = aMoleculeDataModelList;
        if (this.overviewView == null) {
            this.overviewView = new OverviewView(this.columnsPerPageSetting.get(), this.rowsPerPageSetting.get());
        }
        this.overviewViewStage = new Stage();
        Scene tmpScene = new Scene(this.overviewView, GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE,
                GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.overviewViewStage.setScene(tmpScene);
        this.overviewViewStage.initModality(Modality.WINDOW_MODAL);
        this.overviewViewStage.initOwner(this.mainStage);
        this.overviewViewStage.setTitle(this.overviewViewTitle);
        String tmpIconURL = this.getClass().getClassLoader().getResource(
                this.configuration.getProperty("mortar.imagesFolder")
                        + this.configuration.getProperty("mortar.logo.icon.name")).toExternalForm();
        this.overviewViewStage.getIcons().add(new Image(tmpIconURL));
        this.overviewViewStage.setMinHeight(GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.overviewViewStage.setMinWidth(GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE);
        //
        int tmpPageCount = this.moleculeDataModelList.size() / (this.rowsPerPageSetting.get() * this.columnsPerPageSetting.get());
        if (this.moleculeDataModelList.size() % (this.rowsPerPageSetting.get() * this.columnsPerPageSetting.get()) > 0) {
            tmpPageCount++;
        }
        if (this.moleculeDataModelList.isEmpty()) {
            tmpPageCount = 1;
        }
        Pagination tmpPagination = new Pagination(tmpPageCount, 0);
        tmpPagination.setSkin(new CustomPaginationSkin(tmpPagination));
        tmpPagination.setPageFactory(aPageIndex -> this.createOverviewViewPage(aPageIndex,
                this.rowsPerPageSetting.get(), this.columnsPerPageSetting.get()));
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
    //</editor-fold>
    //
    //<editor-fold desc="private methods" dafaultstate="collapsed">
    /**
     * Sets overview view name to given tab name plus number of given molecules and sets option to jump to molecules
     * in main view to false.
     *
     * @param aMoleculeDataModelListSize number of molecules in the overview view
     * @param aTabName given title for the overview view, derived from the tab name where it was opened
     * @throws NullPointerException if tab name is null
     */
    private void setOverviewViewTitleForDefaultDataSource(String aTabName, int aMoleculeDataModelListSize)
            throws NullPointerException {
        Objects.requireNonNull(aTabName, "aTabName (instance of String) is null");
        if (aTabName.isBlank()) {
            OverviewViewController.LOGGER.log(Level.WARNING, "aTabName (instance of String) is blank");
        }
        this.overviewViewTitle = aTabName +
                " - " + Message.get("OverviewView.nameOfView") +
                " - " + (aMoleculeDataModelListSize - 1) + " " +
                Message.get(((aMoleculeDataModelListSize - 1 == 1) ? "OverviewView.titleOfView.molecule"
                        : "OverviewView.titleOfView.molecules"));
        this.withShowInMainViewOption = false;
        this.withFirstStructureHighlight = false;
    }
    /**
     * Adds listeners and event handlers to elements of the overview view.
     */
    private void addListeners() {
        //listener for resize events
        ChangeListener<Number> tmpStageSizeListener = (observable, oldValue, newValue) -> Platform.runLater(() -> this.createOverviewViewPage(this.overviewView.getPagination().getCurrentPageIndex(),
                this.rowsPerPageSetting.get(), this.columnsPerPageSetting.get()));
        this.overviewViewStage.heightProperty().addListener(tmpStageSizeListener);
        this.overviewViewStage.widthProperty().addListener(tmpStageSizeListener);
        //
        //event handler for window close requests
        this.overviewViewStage.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, event -> this.closeOverviewViewEvent());
        //
        //close button listener
        this.overviewView.getCloseButton().setOnAction(actionEvent -> this.closeOverviewViewEvent());
        //
        //listener to distinguish a drag from a click event (on the image views)
        /*
         * The following block of code was inspired by a post of the user "mipa" of the stackoverflow community;
         * link to their answer / comment: https://stackoverflow.com/a/36245807 (2022_12_01; 12:00 GMT)
         */
        this.overviewView.getStructureGridPane().setOnMouseDragged(aMouseEvent -> {
            if (aMouseEvent.getButton().equals(MouseButton.PRIMARY)) {
                this.dragFlag = true;
            }
        });
        //
        //listener to handle single- and double-click events (on the image views)
        /*
         * The following block of code was inspired by a post of the user "mipa" of the stackoverflow community;
         * link to their answer / comment: https://stackoverflow.com/a/36245807 (2022_12_01; 12:00 GMT)
         */
        this.overviewView.getStructureGridPane().setOnMouseClicked(aMouseEvent -> {
            if (aMouseEvent.getButton().equals(MouseButton.PRIMARY)
                    && (aMouseEvent.getTarget().getClass().equals(ImageView.class)
                            || (aMouseEvent.getTarget().getClass().equals(StackPane.class)
                                    && this.getIndexOfStructureInMoleculeDataModelList(aMouseEvent) == 0))) {
                if (!this.dragFlag) {
                    if (aMouseEvent.getClickCount() == 1) {
                        //caching the index of the MoleculeDataModel that corresponds to the content of the
                        // event's source; equivalent to the structure's grid pane cell being selected
                        this.cachedIndexOfStructureInMoleculeDataModelList = this.getIndexOfStructureInMoleculeDataModelList(aMouseEvent);
                        //ensure that there is no single-click action scheduled yet
                        if (this.scheduledFuture == null || this.scheduledFuture.isDone()) {
                            //scheduled single-click action
                            this.scheduledFuture = this.scheduledThreadPoolExecutor.schedule(
                                    //Platform runLater enables this method call in a separate thread
                                    () -> Platform.runLater(() ->
                                            this.showEnlargedStructureView(
                                                    this.moleculeDataModelList.get(this.cachedIndexOfStructureInMoleculeDataModelList),
                                                    this.overviewViewStage
                                            )
                                    ),
                                    GuiDefinitions.DOUBLE_CLICK_DELAY,
                                    TimeUnit.MILLISECONDS
                            );
                        }
                    } else if (aMouseEvent.getClickCount() > 1
                                && this.scheduledFuture != null
                                && !this.scheduledFuture.isCancelled()
                                && !this.scheduledFuture.isDone()) {
                        //terminating the scheduled single-click action
                        this.scheduledFuture.cancel(false);
                        //check whether it is the same structure
                        if (this.getIndexOfStructureInMoleculeDataModelList(aMouseEvent)
                                == this.cachedIndexOfStructureInMoleculeDataModelList
                                && (this.withShowInMainViewOption)) {
                            this.returnToStructureEventOccurred = true;
                            this.closeOverviewViewEvent();
                        }
                    }
                }
                this.dragFlag = false;
            }
        });
        //
        //listener for context menu requests (on the image views)
        this.overviewView.getStructureGridPane().setOnContextMenuRequested(aContextMenuRequest -> {
            if (aContextMenuRequest.getTarget().getClass().equals(ImageView.class)
                    || (aContextMenuRequest.getTarget().getClass().equals(StackPane.class)
                            && this.getIndexOfStructureInMoleculeDataModelList(aContextMenuRequest) == 0)) {
                if (this.scheduledFuture != null && !this.scheduledFuture.isCancelled() && !this.scheduledFuture.isDone()) {
                    this.scheduledFuture.cancel(false);
                }
                //caching the index of the MoleculeDataModel that corresponds to the content of the
                // event's source; equivalent to the structure's grid pane cell being selected
                this.cachedIndexOfStructureInMoleculeDataModelList
                        = this.getIndexOfStructureInMoleculeDataModelList(aContextMenuRequest);
                this.structureContextMenu.show((Node) aContextMenuRequest.getTarget(), aContextMenuRequest.getScreenX(),
                        aContextMenuRequest.getScreenY());
            }
        });
        //
        //apply new grid configuration event handler
        EventHandler<ActionEvent> tmpApplyNewGridConfigurationEventHandler = actionEvent -> this.applyChangeOfGridConfiguration(false);
        //listeners for apply new grid configuration events
        this.overviewView.getApplyButton().setOnAction(tmpApplyNewGridConfigurationEventHandler);
        this.overviewView.getColumnsPerPageTextField().setOnAction(tmpApplyNewGridConfigurationEventHandler);
        this.overviewView.getRowsPerPageTextField().setOnAction(tmpApplyNewGridConfigurationEventHandler);
        //
        //default button listener
        this.overviewView.getDefaultButton().setOnAction(actionEvent -> this.applyChangeOfGridConfiguration(true));
        //
        //focused property change listener for columns per page text field
        this.overviewView.getColumnsPerPageTextField().focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (Boolean.FALSE.equals(newValue) && (this.overviewView.getColumnsPerPageTextField().getText().isBlank()
                        || Integer.parseInt(this.overviewView.getColumnsPerPageTextField().getText()) == 0)) {
                this.overviewView.getColumnsPerPageTextField().setText(Integer.toString(this.columnsPerPageSetting.get()));
            }
        });
        //focused property change listener for rows per page text field
        this.overviewView.getRowsPerPageTextField().focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (Boolean.FALSE.equals(newValue) && (this.overviewView.getRowsPerPageTextField().getText().isBlank()
                        || Integer.parseInt(this.overviewView.getRowsPerPageTextField().getText()) == 0)) {
                this.overviewView.getRowsPerPageTextField().setText(Integer.toString(this.rowsPerPageSetting.get()));
            }
        });
        //
        //change listener for resetting not applied text field entries when switching pagination page
        this.overviewView.getPagination().currentPageIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (!this.overviewView.getColumnsPerPageTextField().getText()
                    .equals(Integer.toString(this.columnsPerPageSetting.get()))) {
                this.overviewView.getColumnsPerPageTextField().setText(Integer.toString(this.columnsPerPageSetting.get()));
            }
            if (!this.overviewView.getRowsPerPageTextField().getText()
                    .equals(Integer.toString(this.rowsPerPageSetting.get()))) {
                this.overviewView.getRowsPerPageTextField().setText(Integer.toString(this.rowsPerPageSetting.get()));
            }
        });
        //
        //event handlers for controlling the pagination via keys
        this.overviewViewStage.getScene().addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> {
            Pagination tmpPagination = this.overviewView.getPagination();
            if (GuiDefinitions.KEY_CODE_LAST_PAGE.match(keyEvent) || keyEvent.getCode() == KeyCode.END) {
                tmpPagination.setCurrentPageIndex(tmpPagination.getPageCount() - 1);
                keyEvent.consume();
            }
            else if (GuiDefinitions.KEY_CODE_FIRST_PAGE.match(keyEvent) || keyEvent.getCode() == KeyCode.HOME) {
                tmpPagination.setCurrentPageIndex(0);
                keyEvent.consume();
            }
            else if (keyEvent.getCode() == KeyCode.RIGHT || keyEvent.getCode() == KeyCode.PAGE_UP) {
                tmpPagination.setCurrentPageIndex(tmpPagination.getCurrentPageIndex() + 1);
                keyEvent.consume();
            }
            else if (keyEvent.getCode() == KeyCode.LEFT || keyEvent.getCode() == KeyCode.PAGE_DOWN) {
                tmpPagination.setCurrentPageIndex(tmpPagination.getCurrentPageIndex() - 1);
                keyEvent.consume();
            }
        });
    }
    //
    /**
     * Closes the overview view window.
     * Shuts down the scheduled thread pool executor and resets the cached structure list index to -1 if no event for
     * returning to a specified structure occurred.
     */
    private void closeOverviewViewEvent() {
        if (!this.returnToStructureEventOccurred) {
            this.resetCachedIndexOfStructureInMoleculeDataModelList();
        }
        //else: this.cachedIndexOfStructureInMoleculeDataModelList and this.returnToStructureEventOccurred are reset
        // separately in resetCachedIndexOfStructureInMoleculeDataModelList()
        this.overviewViewStage.close();
        this.clearGUICachesAtClosing();
    }
    //
    /**
     * Discards all GUI variable values for when the view is closed.
     */
    private void clearGUICachesAtClosing() {
        this.mainStage = null;
        //note: must have been closed before
        this.overviewViewStage = null;
        this.overviewView = null;
        this.overviewViewTitle = null;
        this.dataSource = null;
        this.moleculeDataModelList = null;
        this.createStructureImages = false;
        this.withShowInMainViewOption = false;
        this.withFirstStructureHighlight = false;
        if (!this.returnToStructureEventOccurred) {
            this.resetCachedIndexOfStructureInMoleculeDataModelList();
        }
        //else: this.cachedIndexOfStructureInMoleculeDataModelList and this.returnToStructureEventOccurred are reset
        // separately in resetCachedIndexOfStructureInMoleculeDataModelList()
        this.structureContextMenu = null;
        this.scheduledThreadPoolExecutor.shutdown();
        this.scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
        this.scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true);
        this.dragFlag = false;
    }
    //
    /**
     * Creates an overview view page according to the given page index. This method is called again and again for every page!
     * A GridPane containing structure images is returned. The SMILES and image of each structure can be copied and an
     * enlarged version of the image be shown in a separate view via a context menu.
     * If an exception gets thrown during the depiction of a structure, a label will
     * be placed instead of the structure image (as placeholder and to inform the user).
     * If the image dimensions fall below a defined minimum, instead of the images a label is placed that spans the hole
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
                    - OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH) / aRowsPerPage)
                    - OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH + 0.5;
            double tmpImageWidth = ((tmpPaginationNodeWidth
                    - (2 * GuiDefinitions.GUI_INSETS_VALUE
                            - OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH)) / aColumnsPerPage)
                    - OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH + 0.5;
            //
            //check if the limits for the image dimensions are being exceeded
            if ((tmpImageHeight >= OverviewViewController.OVERVIEW_VIEW_STRUCTURE_IMAGE_MIN_HEIGHT)
                    && (tmpImageWidth >= OverviewViewController.OVERVIEW_VIEW_STRUCTURE_IMAGE_MIN_WIDTH)) {
                //optional setting for change in usage of shadow effect - deprecated
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
                            MoleculeDataModel tmpMoleculeDataModel;
                            if ((tmpMoleculeDataModel = this.moleculeDataModelList.get(tmpIterator)) == null) {
                                //caught below by catch block
                                throw new NullPointerException("A MoleculeDataModel instance has been null.");
                            }
                            //depiction of structure image
                            final Node tmpFinalContentNode;
                            if (!(tmpIterator == 0 && this.withFirstStructureHighlight)) {
                                tmpFinalContentNode = new ImageView(
                                        DepictionUtil.depictImageWithZoomAndFillToFitAndWhiteBackground(
                                                tmpMoleculeDataModel.getAtomContainer(), 1.0, tmpImageWidth,
                                                tmpImageHeight, true, true
                                        )
                                );
                            } else {
                                //highlighting first structure in parent molecules and item overview view
                                StackPane tmpStackPane = new StackPane(
                                        new ImageView(
                                                DepictionUtil.depictImageWithZoomAndFillToFitAndWhiteBackground(
                                                        tmpMoleculeDataModel.getAtomContainer(), 1.0, tmpImageWidth,
                                                        tmpImageHeight, true, false
                                                )
                                        )
                                );
                                tmpStackPane.setMinWidth(tmpImageWidth);
                                tmpStackPane.setMaxWidth(tmpImageWidth);
                                tmpStackPane.setMinHeight(tmpImageHeight);
                                tmpStackPane.setMaxHeight(tmpImageHeight);
                                tmpStackPane.setBorder(new Border(new BorderStroke(Color.WHITE,
                                        BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
                                //the style is set later on to avoid it being replaced
                                tmpFinalContentNode = tmpStackPane;
                            }
                            //changing the shadow effects at mouse entering an image
                            tmpFinalContentNode.setOnMouseEntered(aMouseEvent -> {
                                if (tmpDrawImagesWithShadow) {
                                    tmpFinalContentNode.setStyle("-fx-effect: null" +
                                            ((tmpFinalContentNode.getClass() == StackPane.class)
                                                    ? "; -fx-alignment: CENTER; -fx-background-color: LIGHTGREY"
                                                    : ""));
                                } else {
                                    tmpFinalContentNode.setStyle(
                                            "-fx-effect: dropshadow(gaussian, rgba(100, 100, 100, 0.6), " +
                                                    OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH +
                                                    ", 0, 0, 0)" +
                                            ((tmpFinalContentNode.getClass() == StackPane.class)
                                                    ? "; -fx-alignment: CENTER; -fx-background-color: LIGHTGREY"
                                                    : "")
                                    );
                                }
                            });
                            //resetting the shadow effects at mouse leaving the image
                            tmpFinalContentNode.setOnMouseExited(aMouseEvent -> {
                                if (tmpDrawImagesWithShadow) {
                                    tmpFinalContentNode.setStyle(
                                            "-fx-effect: dropshadow(three-pass-box, rgba(100, 100, 100, 0.6), " +
                                                    OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH
                                                            / 4 + ", 0, " +
                                                    OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH
                                                            / 4 + ", " +
                                                    OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH
                                                            / 4 + ")" +
                                            ((tmpFinalContentNode.getClass() == StackPane.class)
                                                    ? "; -fx-alignment: CENTER; -fx-background-color: LIGHTGREY"
                                                    : "")
                                    );
                                } else {
                                    tmpFinalContentNode.setStyle("-fx-effect: null" +
                                            ((tmpFinalContentNode.getClass() == StackPane.class)
                                                    ? "; -fx-alignment: CENTER; -fx-background-color: LIGHTGREY"
                                                    : ""));
                                }
                            });
                            //
                            tmpContentNode = tmpFinalContentNode;
                        } catch (CDKException | NullPointerException anException) {
                            OverviewViewController.LOGGER.log(Level.WARNING, anException.toString(), anException);
                            //Error label to be shown when a structure can not be depicted
                            Label tmpErrorLabel = new Label(Message.get("OverviewView.ErrorLabel.text"));
                            tmpErrorLabel.setMinWidth(tmpImageWidth);
                            tmpErrorLabel.setMaxWidth(tmpImageWidth);
                            tmpErrorLabel.setMinHeight(tmpImageHeight);
                            tmpErrorLabel.setMaxHeight(tmpImageHeight);
                            tmpErrorLabel.setStyle("-fx-alignment: CENTER; -fx-background-color: WHITE");
                            Tooltip tmpErrorLabelTooltip = GuiUtil.createTooltip(Message.get("OverviewView.ErrorLabel.tooltip"));
                            tmpErrorLabel.setTooltip(tmpErrorLabelTooltip);
                            tmpContentNode = new StackPane(tmpErrorLabel);
                            tmpContentNode.disableProperty().set(true);
                        } catch (IndexOutOfBoundsException anIndexOutOfBoundsException) {
                            //should not happen
                            OverviewViewController.LOGGER.log(Level.SEVERE, anIndexOutOfBoundsException.toString(),
                                    anIndexOutOfBoundsException);
                            break generationOfStructureImagesLoop;
                        }
                        //setting the shadow effect
                        if (tmpDrawImagesWithShadow) {
                            tmpContentNode.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(100, 100, 100, 0.6), " +
                                    OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4 + ", 0, " +
                                    OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4 + ", " +
                                    OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4 + ")");
                        }
                        //highlighting the first structure of parent molecules and item overview view
                        if (!this.withShowInMainViewOption && tmpIterator == 0) {
                            tmpContentNode.setStyle("-fx-alignment: CENTER; -fx-background-color: LIGHTGREY" +
                                    (tmpDrawImagesWithShadow
                                            ? "; -fx-effect: dropshadow(three-pass-box, rgba(100, 100, 100, 0.6), " +
                                            OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4 + ", 0, " +
                                            OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4 + ", " +
                                            OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 4 + ")"
                                            : "")
                            );
                        }
                        //
                        this.overviewView.getStructureGridPane().add(tmpContentNode, j, i);
                        tmpIterator++;
                    }
                }
            } else {
                //informing the user if the image dimensions fell below the defined limit
                this.overviewView.getStructureGridPane().add(this.overviewView.getImageDimensionsBelowLimitVBox(),
                        0, 0, this.columnsPerPageSetting.get(), this.rowsPerPageSetting.get());
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
            tmpNewColumnsPerPageValue = OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_COLUMNS_PER_PAGE_DEFAULT;
            tmpNewRowsPerPageValue = OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_ROWS_PER_PAGE_DEFAULT;
            //values are parsed from text fields below, so set them here accordingly
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
        if (tmpNewColumnsPerPageValue == this.columnsPerPageSetting.getValue() && tmpNewColumnsPerPageValue <= tmpMaxColumnsPerPage
                && tmpNewRowsPerPageValue == this.rowsPerPageSetting.getValue() && tmpNewRowsPerPageValue <= tmpMaxRowsPerPage) {
            return;
        }
        //
        //if the values exceed the max values for the current page, set them to their maximum
        //setting the columns per page
        if (tmpNewColumnsPerPageValue > tmpMaxColumnsPerPage) {
            this.columnsPerPageSetting.set(tmpMaxColumnsPerPage);
            this.overviewView.getColumnsPerPageTextField().setText(String.valueOf(this.columnsPerPageSetting.get()));
        } else {
            this.columnsPerPageSetting.set(tmpNewColumnsPerPageValue);
        }
        //setting the rows per page
        if (tmpNewRowsPerPageValue > tmpMaxRowsPerPage) {
            this.rowsPerPageSetting.set(tmpMaxRowsPerPage);
            this.overviewView.getRowsPerPageTextField().setText(String.valueOf(this.rowsPerPageSetting.get()));
        } else {
            this.rowsPerPageSetting.set(tmpNewRowsPerPageValue);
        }
        //
        //reconfiguration of the OverviewView instance's structureGridPane
        this.overviewView.configureStructureGridPane(this.columnsPerPageSetting.get(), this.rowsPerPageSetting.get());
        //
        //aftermath (adaptions to page count and current page index of the pagination node)
        int tmpNewPageCount = this.moleculeDataModelList.size() / (this.rowsPerPageSetting.get() * this.columnsPerPageSetting.get());
        if (this.moleculeDataModelList.size() % (this.rowsPerPageSetting.get() * this.columnsPerPageSetting.get()) > 0) {
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
                this.rowsPerPageSetting.get(), this.columnsPerPageSetting.get());
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
        //copySmilesMenuItem
        MenuItem tmpCopySmilesMenuItem = new MenuItem(Message.get("OverviewView.contextMenu.copySmilesMenuItem"));
        //copyNameMenuItem
        MenuItem tmpCopyNameMenuItem = new MenuItem(Message.get("OverviewView.contextMenu.copyNameMenuItem"));
        try {
            String tmpCopyIconURL = this.getClass().getClassLoader().getResource(
                    this.configuration.getProperty("mortar.imagesFolder")
                            + this.configuration.getProperty("mortar.icon.copy.name")).toExternalForm();
            tmpCopyImageMenuItem.setGraphic(new ImageView(new Image(tmpCopyIconURL)));
            tmpCopySmilesMenuItem.setGraphic(new ImageView(new Image(tmpCopyIconURL)));
            tmpCopyNameMenuItem.setGraphic(new ImageView(new Image(tmpCopyIconURL)));
        } catch (NullPointerException anException) {
            OverviewViewController.LOGGER.log(Level.WARNING, "Copy icon could not be imported.");
        }
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
        //copyNameMenuItem listener
        tmpCopyNameMenuItem.setOnAction((ActionEvent anActionEvent) -> {
            if (this.cachedIndexOfStructureInMoleculeDataModelList >= 0) {
                String tmpNameString;
                if (this.moleculeDataModelList.get(this.cachedIndexOfStructureInMoleculeDataModelList) != null) {
                    tmpNameString = this.moleculeDataModelList.get(this.cachedIndexOfStructureInMoleculeDataModelList).getName();
                } else {
                    tmpNameString = "";
                }
                ClipboardContent tmpContent = new ClipboardContent();
                tmpContent.putString(tmpNameString);
                Clipboard.getSystemClipboard().setContent(tmpContent);
            }
        });
        //add view-independent MenuItems
        tmpContextMenu.getItems().addAll(
                tmpCopyImageMenuItem,
                tmpCopySmilesMenuItem,
                tmpCopyNameMenuItem
        );
        //add MenuItems that are only needed for overview view items
        if (!aForEnlargedStructureView) {
            //enlargedStructureViewMenuItem
            MenuItem tmpEnlargedStructureViewMenuItem = new MenuItem(Message.get("OverviewView.contextMenu.enlargedStructureViewMenuItem"));
            tmpEnlargedStructureViewMenuItem.setOnAction((ActionEvent anActionEvent) -> {
                if (this.cachedIndexOfStructureInMoleculeDataModelList >= 0) {
                    Platform.runLater(() ->
                            this.showEnlargedStructureView(
                                    this.moleculeDataModelList.get(this.cachedIndexOfStructureInMoleculeDataModelList),
                                    this.overviewViewStage
                            )
                    );
                }
            });
            //showInMainView
            MenuItem tmpShowInMainViewMenuItem = new MenuItem();
            switch (this.dataSource) {
                case MOLECULES_TAB -> tmpShowInMainViewMenuItem.setText(Message
                        .get("OverviewView.contextMenu.showInMainViewMenuItem.molecules"));
                case FRAGMENTS_TAB -> tmpShowInMainViewMenuItem.setText(Message
                        .get("OverviewView.contextMenu.showInMainViewMenuItem.fragments"));
                default -> tmpShowInMainViewMenuItem.setText(Message
                        .get("OverviewView.contextMenu.showInMainViewMenuItem.default"));
            }
            if (this.withShowInMainViewOption) {
                tmpShowInMainViewMenuItem.setOnAction((ActionEvent anActionEvent) -> {
                    //the MoleculeDataModelList-index of the structure has already been cached
                    this.returnToStructureEventOccurred = true;
                    this.closeOverviewViewEvent();
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
     * Gets the MoleculeDataModelList-index of the structure that corresponds to the node that is the source of the
     * given event.
     *
     * @param anEvent Event with the image view or stack pane of a structure as source
     * @return Index of structure in the MoleculeDataModelList
     */
    private int getIndexOfStructureInMoleculeDataModelList(Event anEvent) {
        Node tmpTargetNode = (Node) anEvent.getTarget();
        if (tmpTargetNode.getParent().getClass().equals(StackPane.class)) {
            //case: the highlighted structure of the parent molecules or item overview view itself got clicked
            tmpTargetNode = ((Node) anEvent.getTarget()).getParent();
        }
        int tmpColIndex = GridPane.getColumnIndex(tmpTargetNode);
        int tmpRowIndex = GridPane.getRowIndex(tmpTargetNode);
        int tmpIndexOfStructureOnPage = tmpRowIndex * this.columnsPerPageSetting.get() + tmpColIndex;
        return this.overviewView.getPagination().getCurrentPageIndex() * this.columnsPerPageSetting.get() * this.rowsPerPageSetting.get()
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
                        - OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH))
                / (OverviewViewController.OVERVIEW_VIEW_STRUCTURE_IMAGE_MIN_WIDTH
                        + OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH));
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
                - OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH)
                / (OverviewViewController.OVERVIEW_VIEW_STRUCTURE_IMAGE_MIN_HEIGHT
                        + OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH));
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
                        OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 2 + ", 0, 0, " +
                        OverviewViewController.OVERVIEW_VIEW_STRUCTURE_GRID_PANE_GRIDLINES_WIDTH / 8 + ")"
        );
        Scene tmpScene = new Scene(tmpEnlargedStructureViewStackPane,
                OverviewViewController.ENLARGED_STRUCTURE_VIEW_SCENE_INITIAL_WIDTH,
                OverviewViewController.ENLARGED_STRUCTURE_VIEW_SCENE_INITIAL_HEIGHT);
        tmpEnlargedStructureViewStage.setScene(tmpScene);
        tmpEnlargedStructureViewStage.initModality(Modality.WINDOW_MODAL);
        tmpEnlargedStructureViewStage.initOwner(anOwnerStage);
        tmpEnlargedStructureViewStage.setTitle(Message.get("OverviewView.enlargedStructureView.title"));
        String tmpIconURL = this.getClass().getClassLoader().getResource(
                this.configuration.getProperty("mortar.imagesFolder")
                        + this.configuration.getProperty("mortar.logo.icon.name")).toExternalForm();
        tmpEnlargedStructureViewStage.getIcons().add(new Image(tmpIconURL));
        tmpEnlargedStructureViewStage.setMinHeight(OverviewViewController.ENLARGED_STRUCTURE_VIEW_MIN_HEIGHT_VALUE);
        tmpEnlargedStructureViewStage.setMinWidth(OverviewViewController.ENLARGED_STRUCTURE_VIEW_MIN_WIDTH_VALUE);
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
                    OverviewViewController.ENLARGED_STRUCTURE_VIEW_SCENE_INITIAL_WIDTH
                            * OverviewViewController.ENLARGED_STRUCTURE_VIEW_IMAGE_TO_STACK_PANE_SIZE_RATIO,
                    OverviewViewController.ENLARGED_STRUCTURE_VIEW_SCENE_INITIAL_HEIGHT
                            * OverviewViewController.ENLARGED_STRUCTURE_VIEW_IMAGE_TO_STACK_PANE_SIZE_RATIO,
                    true, true
            ));
            tmpEnlargedStructureViewStackPane.getChildren().add(tmpStructureImage);
            tmpStructureImage.setOnContextMenuRequested(event -> tmpContextMenu.show(tmpStructureImage, event.getScreenX(), event.getScreenY()));
            //listener for resize events to fit the structure depiction to the view size
            ChangeListener<Number> tmpStageResizeEventListener = (observable, oldValue, newValue) -> Platform.runLater(() -> {
                tmpEnlargedStructureViewStackPane.getChildren().clear();
                try {
                    ImageView tmpUpdatedStructureImage = new ImageView(
                            DepictionUtil.depictImageWithZoomAndFillToFitAndWhiteBackground(
                                    aMoleculeDataModel.getAtomContainer(), 1.0,
                                    tmpEnlargedStructureViewStackPane.getWidth()
                                            * OverviewViewController.ENLARGED_STRUCTURE_VIEW_IMAGE_TO_STACK_PANE_SIZE_RATIO,
                                    tmpEnlargedStructureViewStackPane.getHeight()
                                            * OverviewViewController.ENLARGED_STRUCTURE_VIEW_IMAGE_TO_STACK_PANE_SIZE_RATIO,
                                    true, true
                            )
                    );
                    tmpEnlargedStructureViewStackPane.getChildren().add(tmpUpdatedStructureImage);
                    tmpUpdatedStructureImage.setOnContextMenuRequested(event -> tmpContextMenu.show(tmpUpdatedStructureImage, event.getScreenX(), event.getScreenY()));
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
