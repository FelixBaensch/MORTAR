/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2021  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

import de.unijena.cheminf.mortar.gui.panes.GridTabForTableView;
import de.unijena.cheminf.mortar.gui.panes.MainTabPane;
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.gui.views.*;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.fragmentation.FragmentationService;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.IMoleculeFragmenter;
import de.unijena.cheminf.mortar.model.io.Exporter;
import de.unijena.cheminf.mortar.model.io.Importer;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.ChemUtil;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * MainViewController
 * controls  {@link MainView}.
 *
 * @author Felix Baensch, Jonas Schaub
 */
public class MainViewController {
    //<editor-fold desc="private class variables" defaultstate="collapsed">
    private Stage primaryStage;
    private MainView mainView;
    private String appDir;
    private Scene scene;
    private MainTabPane mainTabPane;
    private FragmentationSettingsViewController fragmentationSettingsViewController;
    private ObservableList<MoleculeDataModel> moleculeDataModelList;
    private MoleculesDataTableView moleculesDataTableView;
    private SettingsContainer settingsContainer;
    private boolean selectionAll;
    private boolean selectionAllCheckBoxAction;
    private FragmentationService fragmentationService;
    private Button fragmentationButton;
    private Button cancelFragmentationButton;
    private HashMap<String, ObservableList<FragmentDataModel>> mapOfFragmentDataModelLists;
    private boolean isFragmentationRunning;
    //</editor-fold>
    //
    //<editor-fold desc="private static final variables" defaultstate="collapsed">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(MainViewController.class.getName());
    /**
     * Path to css style sheet
     */
    private static final String STYLE_SHEET_PATH = "/de/unijena/cheminf/mortar/style/StyleSheet.css";
    //</editor-fold>
    //
    /**
     * Constructor
     *
     * @param aStage
     * @param aMainView
     * @param anAppDir
     */
    public MainViewController(Stage aStage, MainView aMainView, String anAppDir){
        //<editor-fold desc="checks" defaultstate="collapsed">
        Objects.requireNonNull(aStage, "aStage (instance of Stage) is null");
        Objects.requireNonNull(aMainView, "aMainView (instance of MainView) is null");
        Objects.requireNonNull(aMainView, "anAppDir (instance of String) is null");
        File tmpAppDirFile = new File(anAppDir);
        if (!tmpAppDirFile.isDirectory() || !tmpAppDirFile.exists()) {
            throw new IllegalArgumentException("The given application directory is neither no directory or does not exist");
        }
        //</editor-fold>
        this.selectionAll = true;
        this.moleculeDataModelList = FXCollections.observableArrayList(param -> new Observable[]{param.selectionProperty()});
        this.primaryStage = aStage;
        this.mainView = aMainView;
        this.appDir = anAppDir;
        this.settingsContainer = new SettingsContainer();
        //<editor-fold desc="show MainView inside of primaryStage" defaultstate="collapsed">
        this.mainTabPane = new MainTabPane();
        this.mainView.getMainCenterPane().getChildren().add(this.mainTabPane);
        GuiUtil.GuiBindControlSizeToParentPane(this.mainView.getMainCenterPane(), this.mainTabPane);
        this.scene = new Scene(this.mainView, GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE, GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.scene.getStylesheets().add(this.getClass().getResource(this.STYLE_SHEET_PATH).toExternalForm());
        this.primaryStage.setTitle(Message.get("Title.text"));
        this.primaryStage.setScene(this.scene);
        this.primaryStage.show();
        this.primaryStage.setMinHeight(GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.primaryStage.setMinWidth(GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE);
        //</editor-fold>
        this.fragmentationService = new FragmentationService();
        this.addListener();
        this.addFragmentationAlgorithmCheckMenuItems();
        this.mapOfFragmentDataModelLists = new HashMap<>(5);
        }
    //
    //<editor-fold desc="private methods" defaultstate="collapsed">
    /**
     * Adds listeners and event handlers to control elements etc.
     */
    private void addListener() {
        this.mainView.getMainMenuBar().getExitMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.closeApplication(0)
        );
        this.mainView.getMainMenuBar().getLoadMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.loadMoleculeFile(this.primaryStage)
        );
        //TODO: Get CSV file separator from settings
        //fragments export to CSV
        this.mainView.getMainMenuBar().getFragmentsExportToCSVMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> new Exporter(this.settingsContainer).createFragmentationTabCsvFile(this.primaryStage,
                        ((GridTabForTableView)this.mainTabPane.getSelectionModel().getSelectedItem()).getTableView().getItems(),
                        ',')
        );
        //fragments export to PDB
        this.mainView.getMainMenuBar().getFragmentsExportToPDBMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> new Exporter(this.settingsContainer).createFragmentationTabPDBFiles(this.primaryStage,
                        ((GridTabForTableView)this.mainTabPane.getSelectionModel().getSelectedItem()).getTableView().getItems())
        );
        //fragments export to PDF
        this.mainView.getMainMenuBar().getFragmentsExportToPDFMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> new Exporter(this.settingsContainer).createFragmentationTabPdfFile(this.primaryStage,
                        ((GridTabForTableView)this.mainTabPane.getSelectionModel().getSelectedItem()).getTableView().getItems(),
                        this.moleculeDataModelList,
                        this.fragmentationService.getSelectedFragmenter().getFragmentationAlgorithmName())
        );
        //fragments export to single SDF
        this.mainView.getMainMenuBar().getFragmentsExportToSingleSDFMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> new Exporter(this.settingsContainer).createFragmentationTabSingleSDFile(this.primaryStage,
                        ((GridTabForTableView)this.mainTabPane.getSelectionModel().getSelectedItem()).getTableView().getItems())
        );
        //fragments export to separate SDFs
        this.mainView.getMainMenuBar().getFragmentsExportToSeparateSDFsMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> new Exporter(this.settingsContainer).createFragmentationTabSeparateSDFiles(this.primaryStage,
                        ((GridTabForTableView)this.mainTabPane.getSelectionModel().getSelectedItem()).getTableView().getItems())
        );
        //items export to CSV
        this.mainView.getMainMenuBar().getItemsExportToCSVMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> new Exporter(this.settingsContainer).createItemizationTabCsvFile(this.primaryStage,
                        this.moleculeDataModelList,
                        this.fragmentationService.getCurrentFragmentationName(),
                        ',')
        );
        //items export to PDF
        this.mainView.getMainMenuBar().getItemsExportToPDFMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> new Exporter(this.settingsContainer).createItemizationTabPdfFile(this.primaryStage,
                        ((GridTabForTableView)this.mainTabPane.getSelectionModel().getSelectedItem()).getTableView().getItems(),
                        this.moleculeDataModelList,
                        this.fragmentationService.getCurrentFragmentationName(),
                        this.fragmentationService.getSelectedFragmenter().getFragmentationAlgorithmName())
        );
        this.mainView.getMainMenuBar().getFragmentationSettingsMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.openFragmentationSettingsView()
        );
        this.mainView.getMainMenuBar().getGlobalSettingsMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.openGlobalSettingsView()
        );
        this.mainView.getMainMenuBar().getPipelineSettingsMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.openPipelineSettingsView());
        this.primaryStage.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, (this::closeWindowEvent));
        //TODO: More implementation needed
        //TODO: Add listener to rows per page setting in settings container //deprecated?
    }
    //
    /**
     * Adds a changes listener to the height property of given table view which sets the height for structure images to
     * each MoleculeDataModel object of the items list and refreshes the table view
     * If image height is too small it will be set to GuiDefinitions.GUI_STRUCTURE_IMAGE_MIN_HEIGHT (50.0)
     *
     * @param aTableView
     */
    private void addTableViewWidthListener(TableView aTableView){
        aTableView.heightProperty().addListener((observable, oldValue, newValue) -> {
            this.setImageStructureHeight(aTableView, newValue.doubleValue());
            aTableView.refresh();
        });
    }
    //
    /**
     * Sets the height for structure images to each MoleculeDataModel object of the items list of the tableView
     * If image height is too small it will be set to GuiDefinitions.GUI_STRUCTURE_IMAGE_MIN_HEIGHT (50.0)
     *
     * @param aTableView TableView
     * @param aHeight double
     */
    private void setImageStructureHeight(TableView aTableView, double aHeight){
        double tmpHeight =
                (aHeight - GuiDefinitions.GUI_TABLE_VIEW_HEADER_HEIGHT - GuiDefinitions.GUI_PAGINATION_CONTROL_PANEL_HEIGHT)
                        / settingsContainer.getRowsPerPageSetting();
        if(aTableView.getClass().equals(ItemizationDataTableView.class)){
            tmpHeight =
                    (aHeight - 2*GuiDefinitions.GUI_TABLE_VIEW_HEADER_HEIGHT - GuiDefinitions.GUI_PAGINATION_CONTROL_PANEL_HEIGHT)
                            / settingsContainer.getRowsPerPageSetting();
        }
        if(tmpHeight < GuiDefinitions.GUI_STRUCTURE_IMAGE_MIN_HEIGHT){
            tmpHeight = GuiDefinitions.GUI_STRUCTURE_IMAGE_MIN_HEIGHT;
        }
        if(aTableView.getClass().equals(ItemizationDataTableView.class)){
            for(MoleculeDataModel tmpMoleculeDataModel : ((IDataTableView)aTableView).getItemsList()){
                tmpMoleculeDataModel.setStructureImageHeight(tmpHeight);
                for(FragmentDataModel tmpFragmentDataModel : tmpMoleculeDataModel.getFragmentsOfSpecificAlgorithm(((ItemizationDataTableView) aTableView).getFragmentationName())){
                    tmpFragmentDataModel.setStructureImageHeight(tmpHeight);
                }
            }
        }
        else{
            for(MoleculeDataModel tmpMoleculeDataModel : ((IDataTableView)aTableView).getItemsList()){
                tmpMoleculeDataModel.setStructureImageHeight(tmpHeight);
            }
        }
    }
    //
    /**
     * Closes application
     */
    private void closeApplication(int aStatus) {
        if(moleculeDataModelList.size() > 0){
            ButtonType tmpConformationResult;
            //Todo: move text to message resource file and maybe create a separate method for this because it is called again below in loadMoleculeFile()
            if(this.isFragmentationRunning){
                tmpConformationResult = GuiUtil.GuiConformationAlert("Warning", "Data will be lost.", "Fragmentation will be stopped and data will be lost if you press Ok. Click cancel to return.");
            } else {
                tmpConformationResult = GuiUtil.GuiConformationAlert("Warning", "Data will be lost.", "Data will be lost if you press Ok. Click cancel to return.");
            }
            if(tmpConformationResult!= ButtonType.OK){
                return;
            }
        }
        try {
            this.settingsContainer.preserveSettings();
        } catch (IOException anIOException) {
            MainViewController.LOGGER.log(Level.WARNING, anIOException.toString(), anIOException);
            GuiUtil.GuiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                    Message.get("Error.SettingsPersistence.Header"),
                    Message.get("Error.SettingsPersistence"),
                    anIOException);
        }
        if(this.isFragmentationRunning){
            this.interruptFragmentation();
        }
        MainViewController.LOGGER.info(BasicDefinitions.MORTAR_SESSION_END);
        Platform.exit();
        System.exit(aStatus);
    }
    //
    private void closeWindowEvent(WindowEvent anEvent){
        this.closeApplication(0);
        anEvent.consume();
    }
    //
    /**
     * Loads molecule file and opens molecules tab
     *
     * @param aParentStage
     */
    private void loadMoleculeFile(Stage aParentStage) {
        if(this.moleculeDataModelList.size() > 0){
            ButtonType tmpConformationResult;
            if(this.isFragmentationRunning){
                tmpConformationResult = GuiUtil.GuiConformationAlert("Warning", "Data will be lost.", "Fragmentation will be stopped and data will be lost if you press Ok. Click cancel to return.");
            } else {
                tmpConformationResult = GuiUtil.GuiConformationAlert("Warning", "Data will be lost.", "Data will be lost if you press Ok. Click cancel to return.");
            }
            if(tmpConformationResult!= ButtonType.OK){
                return;
            }
        }
        if(this.isFragmentationRunning){
            this.interruptFragmentation();
        }
        Importer tmpImporter = new Importer(this.settingsContainer);
        IAtomContainerSet tmpAtomContainerSet = null;
        try {
            tmpAtomContainerSet = tmpImporter.importMoleculeFile(aParentStage);
        } catch (Exception anException) {
            MainViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            GuiUtil.GuiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                    Message.get("Importer.FileImportExceptionAlert.Header"),
                    Message.get("Importer.FileImportExceptionAlert.Text") + "\n" + FileUtil.getAppDirPath() + File.separator + BasicDefinitions.LOG_FILES_DIRECTORY + File.separator,
                    anException);
            return;
        }
        if (tmpAtomContainerSet == null || tmpAtomContainerSet.isEmpty()) {
            return;
        }
        this.clearGuiAndCollections();
        this.mainView.getMainMenuBar().getExportMenu().setDisable(true);
        this.primaryStage.setTitle(Message.get("Title.text") + " - " + tmpImporter.getFileName() + " - " + tmpAtomContainerSet.getAtomContainerCount() + " molecules" );
        for (IAtomContainer tmpAtomContainer : tmpAtomContainerSet.atomContainers()) {
            String tmpSmiles = ChemUtil.createUniqueSmiles(tmpAtomContainer);
            if (tmpSmiles == null) {
                continue;
            }
            MoleculeDataModel tmpMoleculeDataModel;
            if (this.settingsContainer.getKeepAtomContainerInDataModelSetting()) {
                tmpMoleculeDataModel = new MoleculeDataModel(tmpAtomContainer);
            } else {
                tmpMoleculeDataModel = new MoleculeDataModel(tmpSmiles, tmpAtomContainer.getTitle(), tmpAtomContainer.getProperties());
            }
            tmpMoleculeDataModel.setName(tmpAtomContainer.getProperty("NAME"));
            this.moleculeDataModelList.add(tmpMoleculeDataModel);
        }
        MainViewController.LOGGER.log(Level.INFO, "Imported " + tmpAtomContainerSet.getAtomContainerCount() + " molecules from file: " + tmpImporter.getFileName());
        this.openMoleculesTab();
    }
    //
    /**
     * Opens settings view for fragmentationSettings
     */
    private void openFragmentationSettingsView(){
        FragmentationSettingsViewController tmpFragmentationSettingsViewController =
                new FragmentationSettingsViewController(this.primaryStage, this.fragmentationService.getFragmenters(), this.fragmentationService.getSelectedFragmenter().getFragmentationAlgorithmName());
    }
    //
    private void openPipelineSettingsView(){
        PipelineSettingsViewController tmpPipelineSettingsViewController =
                new PipelineSettingsViewController(this.primaryStage, this.fragmentationService, this.moleculeDataModelList.size() > 0);
        if(tmpPipelineSettingsViewController.isFragmentationButtonClicked()){
            this.startFragmentation(tmpPipelineSettingsViewController.isFragmentationButtonClicked());
        }
    }
    //
    /**
     * Adds CheckMenuItems for fragmentation algorithms to MainMenuBar
     */
    private void addFragmentationAlgorithmCheckMenuItems(){
        ToggleGroup tmpToggleGroup = new ToggleGroup();
        for(IMoleculeFragmenter tmpFragmenter : this.fragmentationService.getFragmenters()){
            RadioMenuItem tmpRadioMenuItem = new RadioMenuItem(tmpFragmenter.getFragmentationAlgorithmName());
            tmpRadioMenuItem.setToggleGroup(tmpToggleGroup);
            this.mainView.getMainMenuBar().getFragmentationAlgorithmMenu().getItems().add(tmpRadioMenuItem);
        }
        tmpToggleGroup.selectedToggleProperty().addListener((observableValue, oldValue, newValue) -> {
            if(tmpToggleGroup.getSelectedToggle() != null){
                this.fragmentationService.setSelectedFragmenter(((RadioMenuItem) newValue).getText());
            }
        });
        tmpToggleGroup.selectToggle(tmpToggleGroup.getToggles().get(0));
    }
    //
    /**
     * Opens settings view for global settings
     */
    private void openGlobalSettingsView(){
        SettingsViewController tmpSettingsViewController =  new SettingsViewController(this.primaryStage, this.settingsContainer);
        Platform.runLater(()->{
            if(tmpSettingsViewController.hasRowsPerPageChanged()){
                for(Tab tmpTab : this.mainTabPane.getTabs()){
                    TableView tmpTableView = ((GridTabForTableView) tmpTab).getTableView();
                    int tmpListSize = 0;
                    //TODO: change this when FragmentDataModel extends MoleculeDataModel via Interface IDataTableView
                    tmpListSize = ((IDataTableView)tmpTableView).getItemsList().size();
                    int tmpPageIndex = ((GridTabForTableView) tmpTab).getPagination().getCurrentPageIndex();
                    int tmpRowsPerPage = this.settingsContainer.getRowsPerPageSetting();
                    int tmpPageCount = tmpListSize / tmpRowsPerPage;
                    if(tmpListSize % tmpRowsPerPage > 0){
                        tmpPageCount++;
                    }
                    if(tmpPageIndex > tmpPageCount){
                        tmpPageIndex = tmpPageCount;
                    }
                    ((GridTabForTableView) tmpTab).getPagination().setPageCount(tmpPageCount);
                    ((GridTabForTableView) tmpTab).getPagination().setCurrentPageIndex(tmpPageIndex);
                    ((GridTabForTableView) tmpTab).getTableView().refresh();
                    this.setImageStructureHeight(((GridTabForTableView) tmpTab).getTableView(), ((GridTabForTableView) tmpTab).getTableView().getHeight());
                    ((GridTabForTableView) tmpTab).getTableView().refresh();
                }
            }
            if(tmpSettingsViewController.hasKeepAtomContainerInDataModelChanged()){
                for(MoleculeDataModel tmpMoleculeDataModel : this.moleculeDataModelList){
                    tmpMoleculeDataModel.setKeepAtomContainer(this.settingsContainer.getKeepAtomContainerInDataModelSetting());
                }
                for(ObservableList<FragmentDataModel> tmpFragmentDataModelList : this.mapOfFragmentDataModelLists.values()){
                    for(FragmentDataModel tmpFragmentDataModel : tmpFragmentDataModelList){
                        tmpFragmentDataModel.setKeepAtomContainer(this.settingsContainer.getKeepAtomContainerInDataModelSetting());
                    }
                }
            }
        });
    }
    //
    /**
     * Opens molecules tab
     */
    private void openMoleculesTab() {
        this.moleculesDataTableView = new MoleculesDataTableView();
        this.moleculesDataTableView.setItemsList(this.moleculeDataModelList);
        GridTabForTableView tmpMoleculesTab = new GridTabForTableView(Message.get("MainTabPane.moleculesTab.title"), TabNames.Molecules.name(), this.moleculesDataTableView);
        this.mainTabPane.getTabs().add(tmpMoleculesTab);
        int tmpRowsPerPage = this.settingsContainer.getRowsPerPageSetting();
        int tmpPageCount = this.moleculeDataModelList.size() / tmpRowsPerPage;
        if(this.moleculeDataModelList.size() % tmpRowsPerPage > 0){
            tmpPageCount++;
        }
        Pagination tmpPagination = new Pagination(tmpPageCount, 0);
        tmpPagination.setPageFactory(this::createDataTableViewPage);
        VBox.setVgrow(tmpPagination, Priority.ALWAYS);
        HBox.setHgrow(tmpPagination, Priority.ALWAYS);
        tmpMoleculesTab.addPaginationToGridPane(tmpPagination,0,0,2,2);
        this.fragmentationButton = new Button(Message.get("MainTabPane.moleculesTab.fragmentButton.text"));
        ButtonBar tmpButtonBar = new ButtonBar(); //TODO: adjust button bar height to pagination control height
        tmpButtonBar.setPadding(new Insets(0,0,0,0));
        this.fragmentationButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.fragmentationButton.setMinWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.fragmentationButton.setMaxWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.fragmentationButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpButtonBar.getButtons().add(this.fragmentationButton);
        this.cancelFragmentationButton = new Button(Message.get("MainTabPane.moleculesTab.cancelFragmentationButton.text"));
        this.cancelFragmentationButton.setTooltip(new Tooltip(Message.get("MainTabPane.moleculesTab.cancelFragmentationButton.tooltip")));
        this.cancelFragmentationButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.cancelFragmentationButton.setMinWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.cancelFragmentationButton.setMaxWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.cancelFragmentationButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        this.cancelFragmentationButton.setVisible(false);
        tmpButtonBar.getButtons().add(this.cancelFragmentationButton);
        tmpMoleculesTab.addNodeToGridPane(tmpButtonBar, 0,1,1,1);
        this.fragmentationButton.setOnAction(event->{
            //TODO: add implementation to start fragmentation algorithm
            this.startFragmentation();
        });
        this.cancelFragmentationButton.setOnAction(event ->{
            this.interruptFragmentation();
        });
        this.addTableViewWidthListener(this.moleculesDataTableView);
    }
    //
    /**
     * Creates a page for the pagination for the dataTableView //TODO: refine comment
     *
     * @param aPageIndex
     * @return Node, page of pagination
     */
    private Node createDataTableViewPage(int aPageIndex){
        int tmpRowsPerPage = this.settingsContainer.getRowsPerPageSetting();
        int tmpFromIndex = aPageIndex * tmpRowsPerPage;
        int tmpToIndex = Math.min(tmpFromIndex + tmpRowsPerPage, this.moleculeDataModelList.size());
        this.moleculesDataTableView.getSelectAllCheckBox().setOnAction(event -> {
            this.selectionAllCheckBoxAction = true;
            for (int i = 0; i < this.moleculeDataModelList.size(); i++) {
                if(this.moleculesDataTableView.getSelectAllCheckBox().isSelected()){
                    this.moleculeDataModelList.get(i).setSelection(true);
                }
                else if(!this.moleculesDataTableView.getSelectAllCheckBox().isSelected()){
                    this.moleculeDataModelList.get(i).setSelection(false);
                }
            }
            this.selectionAllCheckBoxAction = false;
        });
        this.moleculeDataModelList.addListener((ListChangeListener) change ->{
            if(this.selectionAllCheckBoxAction){
                // No further action needed with column checkbox data when the select all checkbox is operated on
                return;
            }
            while(change.next()){
                if(change.wasUpdated()){
                    int checked = 0;
                    for(MoleculeDataModel tmpMoleculeDataModel : this.moleculeDataModelList){
                        if(tmpMoleculeDataModel.isSelected())
                            checked++;
                    }
                    if(checked == this.moleculeDataModelList.size()){
                        this.moleculesDataTableView.getSelectAllCheckBox().setSelected(true);
                        this.moleculesDataTableView.getSelectAllCheckBox().setIndeterminate(false);
                    }
                    else if(checked == 0){
                        this.moleculesDataTableView.getSelectAllCheckBox().setSelected(false);
                        this.moleculesDataTableView.getSelectAllCheckBox().setIndeterminate(false);
                    }
                    else if(checked > 0){
                        this.moleculesDataTableView.getSelectAllCheckBox().setSelected(false);
                        this.moleculesDataTableView.getSelectAllCheckBox().setIndeterminate(true);
                    }
                }
            }
        });
        this.moleculesDataTableView.setItems(FXCollections.observableArrayList(this.moleculeDataModelList.subList(tmpFromIndex, tmpToIndex)));
        this.moleculesDataTableView.scrollTo(0);
        return new BorderPane(this.moleculesDataTableView);
    }
    //
    private void interruptFragmentation(){
        this.fragmentationService.abortExecutor();
        this.cancelFragmentationButton.setVisible(false);
        this.fragmentationButton.setDisable(false);
    }
    //
    private void startFragmentation(){
        this.startFragmentation(false);
    }
    /**
     * Starts fragmentation task and opens fragment and itemiztation tabs
     */
    private void startFragmentation(boolean isPipelining){
        long tmpStartTime = System.nanoTime();
        LOGGER.info("Start of method startFragmentation");
        List<MoleculeDataModel> tmpSelectedMolecules = this.moleculeDataModelList.stream().filter(mol -> mol.isSelected()).collect(Collectors.toList());
        int tmpNumberOfCores = this.settingsContainer.getNumberOfTasksForFragmentationSetting();
        try{
            this.mainView.getStatusBar().getProgressBar().visibleProperty().setValue(true);
            this.fragmentationButton.setDisable(true);
            this.cancelFragmentationButton.setVisible(true);
            this.mainView.getStatusBar().getStatusLabel().setText(Message.get("Status.Running"));
            Task<Void> tmpTaskVoidTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    isFragmentationRunning = true;
                    if(isPipelining){
                        fragmentationService.startPipelineFragmentation(tmpSelectedMolecules, tmpNumberOfCores);
//                        fragmentationService.startPipelineFragmentationMolByMol(tmpSelectedMolecules, tmpNumberOfCores);
                    }
                    else{
                        fragmentationService.startSingleFragmentation(tmpSelectedMolecules, tmpNumberOfCores);
                    }
                    return null;
                }
            };
            tmpTaskVoidTask.setOnSucceeded(event -> {
                Platform.runLater(()->{
                    try {
                        ObservableList<FragmentDataModel> tmpObservableFragments = FXCollections.observableArrayList();
                        Set<String> tmpKeys = this.fragmentationService.getFragments().keySet();
                        for(String tmpKey : tmpKeys){
                            tmpObservableFragments.add(this.fragmentationService.getFragments().get(tmpKey));
                        }
                        this.mapOfFragmentDataModelLists.put(this.fragmentationService.getCurrentFragmentationName(), tmpObservableFragments);
                        this.addFragmentationResultTabs(this.fragmentationService.getCurrentFragmentationName());
                        this.mainView.getStatusBar().getProgressBar().visibleProperty().setValue(false);
                        this.mainView.getStatusBar().getStatusLabel().setText(Message.get("Status.Finished"));
                        this.mainView.getMainMenuBar().getExportMenu().setDisable(false);
                        this.fragmentationButton.setDisable(false);
                        this.cancelFragmentationButton.setVisible(false);
                        this.isFragmentationRunning = false;
                        long tmpEndTime = System.nanoTime();
                        LOGGER.info("End of method startFragmentation after " + (tmpEndTime - tmpStartTime) / 1000000000.0);
                    } catch (Exception anException) {
                        MainViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                    }
                });
            });
            tmpTaskVoidTask.setOnCancelled(event -> {
                this.mainView.getStatusBar().getProgressBar().visibleProperty().setValue(false);
                this.mainView.getStatusBar().getStatusLabel().setText(Message.get("Status.Canceled"));
            });
            tmpTaskVoidTask.setOnFailed(event -> {
                this.mainView.getStatusBar().getProgressBar().visibleProperty().setValue(false);
                this.mainView.getStatusBar().getStatusLabel().setText(Message.get("Status.Canceled"));
            });
            Thread tmpThread = new Thread(tmpTaskVoidTask);
            tmpThread.start();
        } catch(Exception anException){
            MainViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            //TODO
        }
    }
    //
    /**
     * Adds a tab for fragments and a tab for items (results of fragmentation)
     *
     * @param aFragmentationName
     */
    private void addFragmentationResultTabs(String aFragmentationName){
        //fragments tab
        FragmentsDataTableView tmpFragmentsDataTableView = new FragmentsDataTableView();
        GridTabForTableView tmpFragmentsTab = new GridTabForTableView(Message.get("MainTabPane.fragmentsTab.title") + " - " + aFragmentationName, TabNames.Fragments.name(), tmpFragmentsDataTableView);
        this.mainTabPane.getTabs().add(tmpFragmentsTab);
        ObservableList<MoleculeDataModel> tmpList = FXCollections.observableArrayList(this.mapOfFragmentDataModelLists.get(aFragmentationName));
        tmpFragmentsDataTableView.setItemsList(tmpList);
        int tmpRowsPerPage = this.settingsContainer.getRowsPerPageSetting();
        int tmpPageCount = tmpList.size() / tmpRowsPerPage;
        if(tmpList.size() % tmpRowsPerPage > 0){
            tmpPageCount++;
        }
        Pagination tmpPagination = new Pagination(tmpPageCount, 0);
        tmpPagination.setPageFactory((pageIndex) -> createFragmentsTableViewPage(pageIndex, tmpFragmentsDataTableView));
        VBox.setVgrow(tmpPagination, Priority.ALWAYS);
        HBox.setHgrow(tmpPagination, Priority.ALWAYS);
        tmpFragmentsTab.addPaginationToGridPane(tmpPagination, 0,0,2,2);
        Button tmpExportCsvButton = new Button(Message.get("MainTabPane.fragments.buttonCSV.txt"));
        Button tmpExportPdfButton = new Button(Message.get("MainTabPane.fragments.buttonPDF.txt"));
        ButtonBar tmpButtonBarFragments = new ButtonBar();
        tmpButtonBarFragments.setPadding(new Insets(0,0,0,0));
        tmpExportCsvButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        tmpExportCsvButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpExportPdfButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        tmpExportPdfButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpButtonBarFragments.getButtons().addAll(tmpExportCsvButton, tmpExportPdfButton);
        Exporter tmpExporter = new Exporter(settingsContainer);
        tmpFragmentsTab.addNodeToGridPane(tmpButtonBarFragments, 0,1,1,1);
        tmpExportPdfButton.setOnAction(event->{
            tmpExporter.createFragmentationTabPdfFile(this.primaryStage, this.mapOfFragmentDataModelLists.get(aFragmentationName),
                    this.moleculeDataModelList, this.fragmentationService.getSelectedFragmenter().getFragmentationAlgorithmName());
        });
        tmpExportCsvButton.setOnAction(event->{
            tmpExporter.createFragmentationTabCsvFile(this.primaryStage, this.mapOfFragmentDataModelLists.get(aFragmentationName),
                    ',');
        });
        tmpFragmentsDataTableView.setOnSort(new EventHandler<SortEvent<TableView>>() {
            @Override
            public void handle(SortEvent<TableView> event) {
                if(event.getSource().getSortOrder().size() == 0)
                    return;
                String tmpSortProp = ((PropertyValueFactory)((TableColumn) event.getSource().getSortOrder().get(0)).cellValueFactoryProperty().getValue()).getProperty().toString();
                TableColumn.SortType tmpSortType = ((TableColumn) event.getSource().getSortOrder().get(0)).getSortType();
                sortGivenFragmentListByPropertyAndSortType(((FragmentsDataTableView)event.getSource()).getItemsList(), tmpSortProp, tmpSortType.toString());
                int fromIndex = tmpPagination.getCurrentPageIndex() * tmpRowsPerPage;
                int toIndex = Math.min(fromIndex + tmpRowsPerPage, ((FragmentsDataTableView)event.getSource()).getItemsList().size());
                event.getSource().getItems().clear();
                event.getSource().getItems().addAll(((FragmentsDataTableView)event.getSource()).getItemsList().subList(fromIndex,toIndex));
            }
        });
        this.addTableViewWidthListener(tmpFragmentsDataTableView);
        //itemization tab
        int tmpAmount = 0; //tmpAmount is the number of fragments appearing in the molecule with the highest number of fragments
        for(int i= 0; i < this.moleculeDataModelList.size(); i++){
            HashMap<String, Integer> tmpCurrentFragmentsMap = this.moleculeDataModelList.get(i).getFragmentFrequencyOfSpecificAlgorithm(aFragmentationName);
            if (tmpCurrentFragmentsMap == null) {
                continue;
            }
            int tmpNrOfFragmentsOfCurrentMolecule = tmpCurrentFragmentsMap.size();
            tmpAmount = Math.max(tmpAmount, tmpNrOfFragmentsOfCurrentMolecule);
        }
        ItemizationDataTableView tmpItemizationDataTableView = new ItemizationDataTableView(tmpAmount, aFragmentationName);
        tmpItemizationDataTableView.setItemsList(this.moleculeDataModelList);
        GridTabForTableView tmpItemizationTab = new GridTabForTableView(Message.get("MainTabPane.itemizationTab.title") + " - " + aFragmentationName, TabNames.Itemization.name(), tmpItemizationDataTableView);
        this.mainTabPane.getTabs().add(tmpItemizationTab);
        tmpPageCount = this.moleculeDataModelList.size() / tmpRowsPerPage;
        if(this.moleculeDataModelList.size() % tmpRowsPerPage > 0){
            tmpPageCount++;
        }
        Pagination tmpPaginationItems = new Pagination(tmpPageCount, 0);
        tmpPaginationItems.setPageFactory((pageIndex) -> createItemizationTableViewPage(pageIndex, tmpItemizationDataTableView));
        VBox.setVgrow(tmpPaginationItems, Priority.ALWAYS);
        HBox.setHgrow(tmpPaginationItems, Priority.ALWAYS);
        tmpItemizationTab.addPaginationToGridPane(tmpPaginationItems, 0,0,2,2);
        Button tmpItemizationTabExportPDfButton = new Button(Message.get("MainTabPane.itemizationTab.pdfButton.txt"));
        Button tmpItemizationExportCsvButton = new Button(Message.get("MainTabPane.itemizationTab.csvButton.txt"));
        ButtonBar tmpButtonBarItemization = new ButtonBar();
        tmpButtonBarItemization.setPadding(new Insets(0,0,0,0));
        tmpItemizationExportCsvButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        tmpItemizationExportCsvButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpItemizationTabExportPDfButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        tmpItemizationTabExportPDfButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpButtonBarItemization.getButtons().addAll(tmpItemizationExportCsvButton, tmpItemizationTabExportPDfButton);
        tmpItemizationTab.addNodeToGridPane(tmpButtonBarItemization, 0, 1,1,1);
        tmpItemizationExportCsvButton.setOnAction(event-> {
            tmpExporter.createItemizationTabCsvFile(this.primaryStage, this.moleculeDataModelList,aFragmentationName,
                    ',');
        });
        tmpItemizationTabExportPDfButton.setOnAction(event -> {
            tmpExporter.createItemizationTabPdfFile(this.primaryStage,this.mapOfFragmentDataModelLists.get(aFragmentationName),
                    this.moleculeDataModelList,aFragmentationName, this.fragmentationService.getSelectedFragmenter().getFragmentationAlgorithmName());
        });
        this.addTableViewWidthListener(tmpItemizationDataTableView);
        //
        this.mainTabPane.getSelectionModel().select(tmpFragmentsTab);
    }
    //
    /**
     * Creates a fragments tableview page
     *
     * @param aPageIndex
     * @param aFragmentsDataTableView
     * @return
     */
    private Node createFragmentsTableViewPage(int aPageIndex, FragmentsDataTableView aFragmentsDataTableView) {
        int tmpRowsPerPage = this.settingsContainer.getRowsPerPageSetting();
        int fromIndex = aPageIndex * tmpRowsPerPage;
        int toIndex = Math.min(fromIndex + tmpRowsPerPage, aFragmentsDataTableView.getItemsList().size());
        aFragmentsDataTableView.setItems(FXCollections.observableArrayList(aFragmentsDataTableView.getItemsList().subList(fromIndex, toIndex)));
        aFragmentsDataTableView.scrollTo(0);
        return new BorderPane(aFragmentsDataTableView);
    }
    //
    /**
     * Creates an itemization tableview page
     *
     * @param pageIndex
     * @param anItemizationDataTableView
     * @return
     */
    private Node createItemizationTableViewPage(int pageIndex, ItemizationDataTableView anItemizationDataTableView){
        int tmpRowsPerPage = this.settingsContainer.getRowsPerPageSetting();
        int fromIndex = pageIndex * tmpRowsPerPage;
        int toIndex = Math.min(fromIndex + tmpRowsPerPage, anItemizationDataTableView.getItemsList().size());
        anItemizationDataTableView.setItems(FXCollections.observableArrayList(anItemizationDataTableView.getItemsList().subList(fromIndex, toIndex)));
        anItemizationDataTableView.scrollTo(0);
        return new BorderPane(anItemizationDataTableView);
    }
    //
    /**
     * Clears the gui and all collections
     */
    private void clearGuiAndCollections(){
        this.moleculeDataModelList.clear();
        this.mapOfFragmentDataModelLists.clear();
        this.moleculesDataTableView = null;
        this.mainTabPane.getTabs().clear();
    }
    //
    private void sortGivenFragmentListByPropertyAndSortType(List<MoleculeDataModel> aList, String aProperty, String aSortType){ //TODO: Move to util class
        Collections.sort(aList, new Comparator<MoleculeDataModel>() {
            @Override
            public int compare(MoleculeDataModel m1, MoleculeDataModel m2) {
                FragmentDataModel f1 = (FragmentDataModel)m1;
                FragmentDataModel f2 = (FragmentDataModel)m2;
                switch(aProperty){
                    case "absoluteFrequency":
                        switch(aSortType){
                            case "ASCENDING":
//                                return (f1.getAbsoluteFrequency() < f2.getAbsoluteFrequency() ? -1 : (f1.getAbsoluteFrequency() == f2.getAbsoluteFrequency() ? 0 : 1));
                                return (Integer.compare(f1.getAbsoluteFrequency(), f2.getAbsoluteFrequency()));
                            case "DESCENDING":
                                return (f1.getAbsoluteFrequency() > f2.getAbsoluteFrequency() ? -1 : (f1.getAbsoluteFrequency() == f2.getAbsoluteFrequency() ? 0 : 1));
                        }
                    case "absolutePercentage":
                        switch(aSortType){
                            case "ASCENDING":
//                                return (f1.getAbsolutePercentage() < f2.getAbsolutePercentage() ? -1 : (f1.getAbsolutePercentage() == f2.getAbsolutePercentage() ? 0 : 1));
                                return (Double.compare(f1.getAbsolutePercentage(), f2.getAbsolutePercentage()));
                            case "DESCENDING":
                                return (f1.getAbsolutePercentage() > f2.getAbsolutePercentage() ? -1 : (f1.getAbsolutePercentage() == f2.getAbsolutePercentage() ? 0 : 1));
                        }
                    case "moleculeFrequency":
                        switch(aSortType){
                            case "ASCENDING":
//                                return (f1.getAbsolutePercentage() < f2.getAbsolutePercentage() ? -1 : (f1.getAbsolutePercentage() == f2.getAbsolutePercentage() ? 0 : 1));
                                return (Double.compare(f1.getMoleculeFrequency(), f2.getMoleculeFrequency()));
                            case "DESCENDING":
                                return (f1.getMoleculeFrequency() > f2.getMoleculeFrequency() ? -1 : (f1.getMoleculeFrequency() == f2.getMoleculeFrequency() ? 0 : 1));
                        }
                    case "moleculePercentage":
                        switch(aSortType){
                            case "ASCENDING":
//                                return (f1.getAbsolutePercentage() < f2.getAbsolutePercentage() ? -1 : (f1.getAbsolutePercentage() == f2.getAbsolutePercentage() ? 0 : 1));
                                return (Double.compare(f1.getMoleculePercentage(), f2.getMoleculePercentage()));
                            case "DESCENDING":
                                return (f1.getMoleculePercentage() > f2.getMoleculePercentage() ? -1 : (f1.getMoleculePercentage() == f2.getMoleculePercentage() ? 0 : 1));
                        }
                }
                return 0;
            }
        });
    }
    //</editor-fold>
}

/**
 * Enum ro tab names
 */
enum TabNames{
    Molecules, Fragments, Itemization
}