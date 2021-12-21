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
import de.unijena.cheminf.mortar.gui.views.FragmentsDataTableView;
import de.unijena.cheminf.mortar.gui.views.IDataTableView;
import de.unijena.cheminf.mortar.gui.views.ItemizationDataTableView;
import de.unijena.cheminf.mortar.gui.views.MainView;
import de.unijena.cheminf.mortar.gui.views.MoleculesDataTableView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.fragmentation.FragmentationService;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.IMoleculeFragmenter;
import de.unijena.cheminf.mortar.model.io.ChemFileTypes;
import de.unijena.cheminf.mortar.model.io.Exporter;
import de.unijena.cheminf.mortar.model.io.Importer;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.ChemUtil;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SortEvent;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
    private FragmentationService fragmentationService;
    private Button fragmentationButton;
    private Button cancelFragmentationButton;
    private HashMap<String, ObservableList<FragmentDataModel>> mapOfFragmentDataModelLists;
    private boolean isFragmentationRunning;
    private Label fragmenterNameLabel;
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
        this.moleculeDataModelList = FXCollections.observableArrayList(param -> new Observable[]{param.selectionProperty()});
        this.primaryStage = aStage;
        this.mainView = aMainView;
        this.appDir = anAppDir;
        this.settingsContainer = new SettingsContainer();
        this.settingsContainer.reloadGlobalSettings();
        this.fragmentationService = new FragmentationService(this.settingsContainer);
        this.fragmentationService.reloadFragmenterSettings();
        this.fragmentationService.reloadActiveFragmenterAndPipeline();
        //<editor-fold desc="show MainView inside of primaryStage" defaultstate="collapsed">
        this.mainTabPane = new MainTabPane();
        this.mainView.getMainCenterPane().getChildren().add(this.mainTabPane);
        GuiUtil.guiBindControlSizeToParentPane(this.mainView.getMainCenterPane(), this.mainTabPane);
        this.scene = new Scene(this.mainView, GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE, GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.scene.getStylesheets().add(this.getClass().getResource(this.STYLE_SHEET_PATH).toExternalForm());
        this.primaryStage.setTitle(Message.get("Title.text"));
        this.primaryStage.setScene(this.scene);
        this.primaryStage.show();
        this.primaryStage.setMinHeight(GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.primaryStage.setMinWidth(GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE);
        InputStream tmpImageInputStream = MainViewController.class.getResourceAsStream("/de/unijena/cheminf/mortar/images/Mortar_Logo_Icon1.png");
        this.primaryStage.getIcons().add(new Image(tmpImageInputStream));
        //</editor-fold>
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
        //TODO: Get CSV file separator from settings; Done?
        //<editor-fold desc="export">
        //fragments export to CSV
        this.mainView.getMainMenuBar().getFragmentsExportToCSVMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> {
                    if((this.mainTabPane.getSelectionModel().getSelectedItem()).getId() == TabNames.Molecules.toString()){
                        GuiUtil.guiConformationAlert(Message.get("Exporter.confirmationAlert.moleculesTabSelected.title"),
                                Message.get("Exporter.confirmationAlert.moleculesTabSelected.header"),
                                Message.get("Exporter.confirmationAlert.moleculesTabSelected.text"));
                        return;
                    }
                    new Exporter(this.settingsContainer).exportCsvFile(
                            this.primaryStage,
                            this.getItemsListOfSelectedFragmenterByTabId(TabNames.Fragments),
                            this.mainTabPane.getSelectedTab().getFragmentationNameOutOfTitle(),
                            this.settingsContainer.getCsvExportSeparatorSetting(),
                            TabNames.Fragments
                    );
                });
        //fragments export to PDB
        this.mainView.getMainMenuBar().getFragmentsExportToPDBMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> {
                    if((this.mainTabPane.getSelectionModel().getSelectedItem()).getId() == TabNames.Molecules.toString()){
                        GuiUtil.guiConformationAlert(Message.get("Exporter.confirmationAlert.moleculesTabSelected.title"),
                                Message.get("Exporter.confirmationAlert.moleculesTabSelected.header"),
                                Message.get("Exporter.confirmationAlert.moleculesTabSelected.text"));
                        return;
                    }
                    new Exporter(this.settingsContainer).exportFragmentsAsChemicalFile(
                            this.primaryStage,
                            this.getItemsListOfSelectedFragmenterByTabId(TabNames.Fragments),
                            this.mainTabPane.getSelectedTab().getFragmentationNameOutOfTitle(),
                            ChemFileTypes.PDB
                    );
                });
        //fragments export to PDF
        this.mainView.getMainMenuBar().getFragmentsExportToPDFMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> {
                    if((this.mainTabPane.getSelectionModel().getSelectedItem()).getId() == TabNames.Molecules.toString()){
                        GuiUtil.guiConformationAlert(Message.get("Exporter.confirmationAlert.moleculesTabSelected.title"),
                                Message.get("Exporter.confirmationAlert.moleculesTabSelected.header"),
                                Message.get("Exporter.confirmationAlert.moleculesTabSelected.text"));
                        return;
                    }
                    new Exporter(this.settingsContainer).exportPdfFile(
                            this.primaryStage,
                            this.getItemsListOfSelectedFragmenterByTabId(TabNames.Fragments),
                            this.moleculeDataModelList,
                            this.mainTabPane.getSelectedTab().getFragmentationNameOutOfTitle(),
                            TabNames.Fragments
                    );
                });
        //fragments export to single SDF
        this.mainView.getMainMenuBar().getFragmentsExportToSingleSDFMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> {
                    if((this.mainTabPane.getSelectionModel().getSelectedItem()).getId() == TabNames.Molecules.toString()){
                        GuiUtil.guiConformationAlert(Message.get("Exporter.confirmationAlert.moleculesTabSelected.title"),
                                Message.get("Exporter.confirmationAlert.moleculesTabSelected.header"),
                                Message.get("Exporter.confirmationAlert.moleculesTabSelected.text"));
                        return;
                    }
                    new Exporter(this.settingsContainer).exportFragmentsAsChemicalFile(
                            this.primaryStage,
                            this.getItemsListOfSelectedFragmenterByTabId(TabNames.Fragments),
                            this.mainTabPane.getSelectedTab().getFragmentationNameOutOfTitle(),
                            ChemFileTypes.SDF,
                            true
                    );
                });
        //fragments export to separate SDFs
        this.mainView.getMainMenuBar().getFragmentsExportToSeparateSDFsMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> {
                    if((this.mainTabPane.getSelectionModel().getSelectedItem()).getId() == TabNames.Molecules.toString()){
                        GuiUtil.guiConformationAlert(Message.get("Exporter.confirmationAlert.moleculesTabSelected.title"),
                                Message.get("Exporter.confirmationAlert.moleculesTabSelected.header"),
                                Message.get("Exporter.confirmationAlert.moleculesTabSelected.text"));
                        return;
                    }
                    new Exporter(this.settingsContainer).exportFragmentsAsChemicalFile(
                            this.primaryStage,
                            this.getItemsListOfSelectedFragmenterByTabId(TabNames.Fragments),
                            this.mainTabPane.getSelectedTab().getFragmentationNameOutOfTitle(),
                            ChemFileTypes.SDF,
                            false
                    );
                });
        //items export to CSV
        this.mainView.getMainMenuBar().getItemsExportToCSVMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> {
                    if((this.mainTabPane.getSelectionModel().getSelectedItem()).getId() == TabNames.Molecules.toString()){
                        GuiUtil.guiConformationAlert(Message.get("Exporter.confirmationAlert.moleculesTabSelected.title"),
                                Message.get("Exporter.confirmationAlert.moleculesTabSelected.header"),
                                Message.get("Exporter.confirmationAlert.moleculesTabSelected.text"));
                        return;
                    }
                    new Exporter(this.settingsContainer).exportCsvFile(
                            this.primaryStage,
                            this.moleculeDataModelList,
                            this.mainTabPane.getSelectedTab().getFragmentationNameOutOfTitle(),
                            this.settingsContainer.getCsvExportSeparatorSetting(),
                            TabNames.Itemization
                    );
                });
        //items export to PDF
        this.mainView.getMainMenuBar().getItemsExportToPDFMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> {
                    if((this.mainTabPane.getSelectionModel().getSelectedItem()).getId() == TabNames.Molecules.toString()){
                        GuiUtil.guiConformationAlert(Message.get("Exporter.confirmationAlert.moleculesTabSelected.title"),
                                Message.get("Exporter.confirmationAlert.moleculesTabSelected.header"),
                                Message.get("Exporter.confirmationAlert.moleculesTabSelected.text"));
                        return;
                    }
                    new Exporter(this.settingsContainer).exportPdfFile(
                            this.primaryStage,
                            this.getItemsListOfSelectedFragmenterByTabId(TabNames.Itemization),
                            this.moleculeDataModelList,
                            this.mainTabPane.getSelectedTab().getFragmentationNameOutOfTitle(),
                            TabNames.Itemization
                    );
                });
        //</editor-fold>
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
        this.mainView.getMainMenuBar().getAboutViewMenuItem().setOnAction(actionEvent -> new AboutViewController(this.primaryStage));
        this.scene.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
//            GridTabForTableView tmpGrid = ((GridTabForTableView)this.mainTabPane.getSelectionModel().getSelectedItem());
            GridTabForTableView tmpGrid = this.mainTabPane.getSelectedTab();
            if(tmpGrid == null){
                keyEvent.consume();
                return;
            }
            if(keyEvent.getCode() == KeyCode.RIGHT){
                tmpGrid.getPagination().setCurrentPageIndex(tmpGrid.getPagination().getCurrentPageIndex() + 1);
                keyEvent.consume();
            }
            else if(keyEvent.getCode() == KeyCode.LEFT){
                tmpGrid.getPagination().setCurrentPageIndex(tmpGrid.getPagination().getCurrentPageIndex() - 1);
                keyEvent.consume();
            }
//            else if(GuiDefinitions.KEY_CODE_LAST_PAGE.match(keyEvent)){
//                tmpGrid.getPagination().setCurrentPageIndex(tmpGrid.getPagination().getPageCount() - 1);
//                keyEvent.consume();
//            }
//            else if(GuiDefinitions.KEY_CODE_FIRST_PAGE.match(keyEvent)){
//                tmpGrid.getPagination().setCurrentPageIndex(0);
//                keyEvent.consume();
//            }
        });
        //TODO: More implementation needed
        //TODO: Add listener to rows per page setting in settings container //deprecated?
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
                tmpConformationResult = GuiUtil.guiConformationAlert("Warning", "Data will be lost.", "Fragmentation will be stopped and data will be lost if you press Ok. Click cancel to return.");
            } else {
                tmpConformationResult = GuiUtil.guiConformationAlert("Warning", "Data will be lost.", "Data will be lost if you press Ok. Click cancel to return.");
            }
            if(tmpConformationResult!= ButtonType.OK){
                return;
            }
        }
        this.settingsContainer.preserveSettings();
        this.fragmentationService.persistFragmenterSettings();
        this.fragmentationService.persistSelectedFragmenterAndPipeline();
        if(this.isFragmentationRunning){
            this.interruptFragmentation();
        }
        MainViewController.LOGGER.info(BasicDefinitions.MORTAR_SESSION_END);
        Platform.exit();
        System.exit(aStatus);
    }
    //
    /**
     * Closes the application via closeApplication method when close window event was fired
     *
     * @param anEvent WindowEvent
     */
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
                tmpConformationResult = GuiUtil.guiConformationAlert("Warning", "Data will be lost.", "Fragmentation will be stopped and data will be lost if you press Ok. Click cancel to return.");
            } else {
                tmpConformationResult = GuiUtil.guiConformationAlert("Warning", "Data will be lost.", "Data will be lost if you press Ok. Click cancel to return.");
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
            GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
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
        int tmpExceptionCount = 0;
        for (IAtomContainer tmpAtomContainer : tmpAtomContainerSet.atomContainers()) {
            //returns null if no SMILES code could be created
            String tmpSmiles = ChemUtil.createUniqueSmiles(tmpAtomContainer);
            if (tmpSmiles == null) {
                tmpExceptionCount++;
                continue;
            }
            MoleculeDataModel tmpMoleculeDataModel;
            if (this.settingsContainer.getKeepAtomContainerInDataModelSetting()) {
                tmpMoleculeDataModel = new MoleculeDataModel(tmpAtomContainer);
            } else {
                tmpMoleculeDataModel = new MoleculeDataModel(tmpSmiles, tmpAtomContainer.getTitle(), tmpAtomContainer.getProperties());
            }
            tmpMoleculeDataModel.setName(tmpAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
            this.moleculeDataModelList.add(tmpMoleculeDataModel);
        }
        MainViewController.LOGGER.log(Level.INFO, "Imported " + tmpAtomContainerSet.getAtomContainerCount() + " molecules from file: " + tmpImporter.getFileName()
                + " " + tmpExceptionCount + " molecules could not be parsed into the internal data model.");
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
            if (!Objects.isNull(this.fragmentationService.getSelectedFragmenter()) && tmpFragmenter.getFragmentationAlgorithmName().equals(this.fragmentationService.getSelectedFragmenter().getFragmentationAlgorithmName())) {
                tmpToggleGroup.selectToggle(tmpRadioMenuItem);
            }
        }
        tmpToggleGroup.selectedToggleProperty().addListener((observableValue, oldValue, newValue) -> {
            if(tmpToggleGroup.getSelectedToggle() != null){
                this.fragmentationService.setSelectedFragmenter(((RadioMenuItem) newValue).getText());
                this.fragmentationService.setSelectedFragmenterNameProperty(((RadioMenuItem) newValue).getText());
            }
        });
        //TODO remove?
        //tmpToggleGroup.selectToggle(tmpToggleGroup.getToggles().get(0));
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
                    //TODO: change this when FragmentDataModel extends MoleculeDataModel via Interface IDataTableView - done?
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
                    GuiUtil.setImageStructureHeight(((GridTabForTableView) tmpTab).getTableView(), ((GridTabForTableView) tmpTab).getTableView().getHeight(), this.settingsContainer);
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
        tmpPagination.setPageFactory((pageIndex) -> this.moleculesDataTableView.createMoleculeTableViewPage(pageIndex, this.settingsContainer));
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
        tmpButtonBar.setButtonMinWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        Label tmpLabel = new Label();
        tmpLabel.textProperty().bind(this.fragmentationService.selectedFragmenterNamePropertyProperty());
        Tooltip tmpTooltip = new Tooltip();
        tmpTooltip.textProperty().bind(this.fragmentationService.selectedFragmenterNamePropertyProperty());
        tmpLabel.setTooltip(tmpTooltip);
        HBox.setHgrow(tmpLabel, Priority.ALWAYS);
        tmpButtonBar.getButtons().add(tmpLabel);
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
        this.moleculesDataTableView.addTableViewHeightListener(this.settingsContainer);
        this.moleculesDataTableView.getCopyMenuItem().setOnAction(event -> GuiUtil.copySelectedTableViewCellsToClipboard(this.moleculesDataTableView));
        this.moleculesDataTableView.setOnKeyPressed(event -> {
            if(GuiDefinitions.KEY_CODE_COPY.match(event)){
                GuiUtil.copySelectedTableViewCellsToClipboard(this.moleculesDataTableView);
            }
        });
        this.moleculesDataTableView.setOnSort((EventHandler<SortEvent<TableView>>) event -> {
            GuiUtil.sortTableViewGlobally(event, tmpPagination, tmpRowsPerPage);
        });

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
        tmpPagination.setPageFactory((pageIndex) -> tmpFragmentsDataTableView.createFragmentsTableViewPage(pageIndex, this.settingsContainer));
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
        tmpFragmentsTab.addNodeToGridPane(tmpButtonBarFragments, 0,1,1,1);
        tmpExportPdfButton.setOnAction(event->{
            new Exporter(this.settingsContainer).exportPdfFile(
                    this.primaryStage,
                    ((IDataTableView)tmpFragmentsTab.getTableView()).getItemsList(),
                    this.moleculeDataModelList,
                    aFragmentationName,
                    TabNames.Fragments
            );
        });
        tmpExportCsvButton.setOnAction(event->{
            new Exporter(this.settingsContainer).exportCsvFile(
                    this.primaryStage,
                    ((IDataTableView)tmpFragmentsTab.getTableView()).getItemsList(),
                    this.mainTabPane.getSelectedTab().getFragmentationNameOutOfTitle(),
                    this.settingsContainer.getCsvExportSeparatorSetting(),
                    TabNames.Fragments
            );
        });
        tmpFragmentsDataTableView.setOnSort((EventHandler<SortEvent<TableView>>) event -> {
            GuiUtil.sortTableViewGlobally(event, tmpPagination, tmpRowsPerPage);
        });
        tmpFragmentsDataTableView.addTableViewHeightListener(this.settingsContainer);
        tmpFragmentsDataTableView.getCopyMenuItem().setOnAction(event -> GuiUtil.copySelectedTableViewCellsToClipboard(tmpFragmentsDataTableView));
        tmpFragmentsDataTableView.setOnKeyPressed(event -> {
            if(GuiDefinitions.KEY_CODE_COPY.match(event)){
                GuiUtil.copySelectedTableViewCellsToClipboard(tmpFragmentsDataTableView);
            }
        });
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
        tmpPaginationItems.setPageFactory((pageIndex) -> tmpItemizationDataTableView.createItemizationTableViewPage(pageIndex, this.settingsContainer));
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
            new Exporter(this.settingsContainer).exportCsvFile(
                    this.primaryStage,
                    this.moleculeDataModelList,
                    this.mainTabPane.getSelectedTab().getFragmentationNameOutOfTitle(),
                    this.settingsContainer.getCsvExportSeparatorSetting(),
                    TabNames.Itemization
            );
        });
        tmpItemizationTabExportPDfButton.setOnAction(event -> {
            new Exporter(this.settingsContainer).exportPdfFile(
                    this.primaryStage,
                    ((IDataTableView)tmpItemizationTab.getTableView()).getItemsList(),
                    this.moleculeDataModelList,
                    aFragmentationName,
                    TabNames.Itemization
            );
        });
        tmpItemizationDataTableView.setOnSort((EventHandler<SortEvent<TableView>>) event -> {
            GuiUtil.sortTableViewGlobally(event, tmpPaginationItems, tmpRowsPerPage);
        });
        tmpItemizationDataTableView.addTableViewHeightListener(this.settingsContainer);
        tmpItemizationDataTableView.getCopyMenuItem().setOnAction(event -> GuiUtil.copySelectedTableViewCellsToClipboard(tmpItemizationDataTableView));
        tmpItemizationDataTableView.setOnKeyPressed(event -> {
            if(GuiDefinitions.KEY_CODE_COPY.match(event)){
                GuiUtil.copySelectedTableViewCellsToClipboard(tmpItemizationDataTableView);
            }
        });
        //
        this.mainTabPane.getSelectionModel().select(tmpFragmentsTab);
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
    /**
     * Returns the items list of the table view of the selected tab
     *
     * @param aTabName Enum which specifies which kind of tab
     * @return List<MoleculeDataModel>
     */
    private List<MoleculeDataModel> getItemsListOfSelectedFragmenterByTabId(TabNames aTabName){
        return ((IDataTableView)((GridTabForTableView)(this.mainTabPane.getTabs().stream().filter(tab ->
                this.mainTabPane.getSelectedTab().getFragmentationNameOutOfTitle().equals(((GridTabForTableView)tab).getFragmentationNameOutOfTitle()) && tab.getId().equals(aTabName.name())
        ).findFirst().get())).getTableView()).getItemsList();
    }
    //</editor-fold>
}

