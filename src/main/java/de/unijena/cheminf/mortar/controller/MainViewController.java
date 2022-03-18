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
import de.unijena.cheminf.mortar.model.util.LogUtil;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SortEvent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * MainViewController
 * controls  {@link MainView}.
 *
 * @author Felix Baensch, Jonas Schaub
 * @version 1.0
 */
public class MainViewController {
    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * Primary Stage
     */
    private Stage primaryStage;
    /**
     * MainView
     */
    private MainView mainView;
    /**
     * Scene
     */
    private Scene scene;
    /**
     * TabPane which holds the different tabs
     */
    private TabPane mainTabPane;
    /**
     * ObservableList to hold MoleculeDataModels for visualisation in MoleculesDataTableView
     */
    private ObservableList<MoleculeDataModel> moleculeDataModelList;
    /**
     * MoleculesDataTableView to show imported molecules
     */
    private MoleculesDataTableView moleculesDataTableView;
    /**
     * SettingsContainer
     */
    private SettingsContainer settingsContainer;
    /**
     * FragmentationService
     */
    private FragmentationService fragmentationService;
    /**
     * Button to start single algorithm fragmentation
     */
    private Button fragmentationButton;
    /**
     * Button to cancel running fragmentation
     */
    private Button cancelFragmentationButton;
    /**
     * HashMap to hold Lists of FragmentDataModels for each fragmentation
     */
    private HashMap<String, ObservableList<FragmentDataModel>> mapOfFragmentDataModelLists;
    /**
     * Boolean value whether fragmentation is running
     */
    private boolean isFragmentationRunning;
    /**
     * Task for parallel fragmentation
     */
    private Task<Void> parallelFragmentationMainTask;
    /**
     * Thread for task for parallel fragmentation
     */
    private Thread fragmentationThread;
    /**
     * Thread for molecule imports, so GUI thread is always responsive
     */
    private Thread importerThread;
    /**
     * Task for molecule file import
     */
    private Task<IAtomContainerSet> importTask;
    /**
     * Thread for molecule exports, so GUI thread is always responsive
     */
    private Thread exporterThread;
    /**
     * Task for molecule file export
     */
    private Task<Void> exportTask;
    /**
     * BooleanProperty whether import is running
     */
    private BooleanProperty isImportRunningProperty;
    /**
     * BooleanProperty whether export is running
     */
    private BooleanProperty isExportRunningProperty;
    /**
     *
     */
    private CopyOnWriteArrayList<Thread> taskList;
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
     * @param aStage Stage
     * @param aMainView MainView
     * @param anAppDir String path to app dir
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
        this.settingsContainer = new SettingsContainer();
        this.settingsContainer.reloadGlobalSettings();
        this.fragmentationService = new FragmentationService(this.settingsContainer);
        this.fragmentationService.reloadFragmenterSettings();
        this.fragmentationService.reloadActiveFragmenterAndPipeline();
        //<editor-fold desc="show MainView inside of primaryStage" defaultstate="collapsed">
        this.mainTabPane = new TabPane();
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
        this.isImportRunningProperty = new SimpleBooleanProperty(false);
        this.isExportRunningProperty = new SimpleBooleanProperty(false);
        this.mapOfFragmentDataModelLists = new HashMap<>(5);
        this.taskList = new CopyOnWriteArrayList();
        this.addListener();
        this.addFragmentationAlgorithmCheckMenuItems();
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
                anEvent -> this.importMoleculeFile(this.primaryStage)
        );
        this.mainView.getMainMenuBar().getCancelImportMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.interruptImport()
        );
        this.mainView.getMainMenuBar().getCancelImportMenuItem().visibleProperty().bind(this.isImportRunningProperty);
        this.mainView.getMainMenuBar().getCancelExportMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.interruptExport()
        );
        this.mainView.getMainMenuBar().getCancelExportMenuItem().visibleProperty().bind(this.isExportRunningProperty);
        //<editor-fold desc="export">
        //fragments export to CSV
        this.mainView.getMainMenuBar().getFragmentsExportToCSVMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.exportFile(Exporter.ExportTypes.FRAGMENT_CSV_FILE));
        //fragments export to PDB
        this.mainView.getMainMenuBar().getFragmentsExportToPDBMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.exportFile(Exporter.ExportTypes.PDB_FILE));
        //fragments export to PDF
        this.mainView.getMainMenuBar().getFragmentsExportToPDFMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.exportFile(Exporter.ExportTypes.FRAGMENT_PDF_FILE));
        //fragments export to single SDF
        this.mainView.getMainMenuBar().getFragmentsExportToSingleSDFMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.exportFile(Exporter.ExportTypes.SINGLE_SD_FILE));
        //fragments export to separate SDFs
        this.mainView.getMainMenuBar().getFragmentsExportToSeparateSDFsMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.exportFile(Exporter.ExportTypes.SD_FILE));
        //items export to CSV
        this.mainView.getMainMenuBar().getItemsExportToCSVMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.exportFile(Exporter.ExportTypes.ITEM_CSV_FILE));
        //items export to PDF
        this.mainView.getMainMenuBar().getItemsExportToPDFMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.exportFile(Exporter.ExportTypes.ITEM_PDF_FILE));
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
            GridTabForTableView tmpGrid = ((GridTabForTableView)this.mainTabPane.getSelectionModel().getSelectedItem());
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
    }
    //
    /**
     * Closes application
     */
    private void closeApplication(int aStatus) {
        if(moleculeDataModelList.size() > 0){
            if (!this.isFragmentationStopAndDataLossConfirmed()) {
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
     * Opens a dialog to warn the user of possible data loss and stopping a running fragmentation, e.g. when a new
     * molecule set should be imported or the application shut down. Returns true if "OK" was clicked, "false" for cancel
     * button.
     *
     * @return true if "OK" was clicked, false for "Cancel"
     */
    private boolean isFragmentationStopAndDataLossConfirmed() {
        ButtonType tmpConformationResult;
        if(this.isFragmentationRunning){
            tmpConformationResult = GuiUtil.guiConformationAlert(
                    Message.get("MainViewController.Warning.FragmentationRunning.Title"),
                    Message.get("MainViewController.Warning.FragmentationRunning.Header"),
                    Message.get("MainViewController.Warning.FragmentationRunning.Content"));
        } else {
            tmpConformationResult = GuiUtil.guiConformationAlert(
                    Message.get("MainViewController.Warning.DataLoss.Title"),
                    Message.get("MainViewController.Warning.DataLoss.Header"),
                    Message.get("MainViewController.Warning.DataLoss.Content"));
        }
        return tmpConformationResult == ButtonType.OK;
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
     * @param aParentStage Stage
     */
    private void importMoleculeFile(Stage aParentStage) {
        if(this.moleculeDataModelList.size() > 0){
            if (!this.isFragmentationStopAndDataLossConfirmed()) {
                return;
            }
        }
        Importer tmpImporter = new Importer(this.settingsContainer);
        File tmpFile = tmpImporter.loadFile(aParentStage);
        if (Objects.isNull(tmpFile)) {
            return;
        }
        if(this.isFragmentationRunning){
            this.interruptFragmentation();
        }
        if(this.isImportRunningProperty.get()){
            this.interruptImport();
        }
        if(this.isExportRunningProperty.get()){
            this.interruptExport();
        }
        this.clearGuiAndCollections();
        this.importTask = new Task<>() {
            @Override
            protected IAtomContainerSet call() throws Exception {
                IAtomContainerSet tmpSet = tmpImporter.importMoleculeFile(tmpFile);
                return tmpSet;
            }
        };
        this.importTask.setOnSucceeded(event -> {
            //note: setOnSucceeded() takes place in the JavaFX GUI thread again but still runLater() is necessary to wait
            // for the thread to be free for the update
            Platform.runLater(() -> {
                IAtomContainerSet tmpAtomContainerSet = null;
                try {
                    tmpAtomContainerSet = importTask.get();
                } catch (InterruptedException | ExecutionException anException) {
                    MainViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                            Message.get("Importer.FileImportExceptionAlert.Header"),
                            Message.get("Importer.FileImportExceptionAlert.Text") + "\n" + FileUtil.getAppDirPath() + File.separator + BasicDefinitions.LOG_FILES_DIRECTORY + File.separator,
                            anException);
                    this.updateStatusBar(this.importerThread, Message.get("Status.importFailed"));
                }
                if (tmpAtomContainerSet == null || tmpAtomContainerSet.isEmpty()) {
                    return;
                }
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
                this.updateStatusBar(this.importerThread, Message.get("Status.loaded"));
                this.isImportRunningProperty.setValue(false);
                this.openMoleculesTab();
            });
        });
        this.importTask.setOnCancelled(event -> {
            this.updateStatusBar(this.importerThread, Message.get("Status.canceled"));
            this.isImportRunningProperty.setValue(false);
        });
        this.importTask.setOnFailed(event -> {
            this.updateStatusBar(this.importerThread, Message.get("Status.importFailed"));
            this.isImportRunningProperty.setValue(false);
            LogUtil.getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), event.getSource().getException());
        });
        this.importerThread = new Thread(importTask);
        this.importerThread.setName(ThreadType.IMPORT_THREAD.getThreadName());
        this.importerThread.setUncaughtExceptionHandler(LogUtil.getUncaughtExceptionHandler());
        this.importerThread.setDaemon(false);
        this.importerThread.setPriority(Thread.currentThread().getPriority() - 2); //magic number
        this.isImportRunningProperty.setValue(true);
        this.updateStatusBar(this.importerThread, Message.get("Status.loading"));
        this.importerThread.start();
    }
    //
    /**
     * Exports the given type of file
     *
     * @param anExportType Enum to specify what type of file to export
     */
    private void exportFile(Exporter.ExportTypes anExportType){
        if((this.mainTabPane.getSelectionModel().getSelectedItem()).getId() == TabNames.Molecules.toString()){
            GuiUtil.guiConformationAlert(Message.get("Exporter.confirmationAlert.moleculesTabSelected.title"),
                    Message.get("Exporter.confirmationAlert.moleculesTabSelected.header"),
                    Message.get("Exporter.confirmationAlert.moleculesTabSelected.text"));
            return;
        }
        switch (anExportType) {
            case FRAGMENT_CSV_FILE:
            case PDB_FILE:
            case FRAGMENT_PDF_FILE:
            case SINGLE_SD_FILE:
            case SD_FILE:
                if (this.getItemsListOfSelectedFragmenterByTabId(TabNames.Fragments) == null ||
                        this.getItemsListOfSelectedFragmenterByTabId(TabNames.Fragments).size() == 0 ||
                        ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle() == null) {
                    GuiUtil.guiMessageAlert(
                            Alert.AlertType.INFORMATION,
                            Message.get("Exporter.MessageAlert.NoDataAvailable.title"),
                            Message.get("Exporter.MessageAlert.NoDataAvailable.header"),
                            null
                    );
                    return;
                }
                break;
            case ITEM_CSV_FILE:
            case ITEM_PDF_FILE:
                if (this.getItemsListOfSelectedFragmenterByTabId(TabNames.Itemization) == null ||
                        this.getItemsListOfSelectedFragmenterByTabId(TabNames.Itemization).size() == 0 ||
                        this.moleculeDataModelList == null || this.moleculeDataModelList.size() == 0 ||
                        ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle() == null) {
                    GuiUtil.guiMessageAlert(
                            Alert.AlertType.INFORMATION,
                            Message.get("Exporter.MessageAlert.NoDataAvailable.title"),
                            Message.get("Exporter.MessageAlert.NoDataAvailable.header"),
                            null
                    );
                    return;
                }
                break;
        }
        Exporter tmpExporter = new Exporter(this.settingsContainer);
        if(this.isExportRunningProperty.get()){
            this.interruptExport();
        }
        tmpExporter.saveFile(this.primaryStage, anExportType, ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle());
        List<String> tmpFailedExportFragments = new LinkedList<>();
        boolean tmpGenerate2dAtomCoordinates = false;
        switch(anExportType){
            case PDB_FILE:
            case SINGLE_SD_FILE:
                if (!ChemUtil.checkMoleculeListForCoordinates(getItemsListOfSelectedFragmenterByTabId(TabNames.Fragments))) {
                ButtonType tmpConformationResult = GuiUtil.guiConformationAlert(
                        Message.get("Exporter.FragmentsTab.ConformationAlert.No3dInformationAvailable.title"),
                        Message.get("Exporter.FragmentsTab.ConformationAlert.No3dInformationAvailable.header"),
                        Message.get("Exporter.FragmentsTab.ConformationAlert.No3dInformationAvailable.text")
                );
                tmpGenerate2dAtomCoordinates = tmpConformationResult == ButtonType.OK;
                }
                break;
        }
        boolean tmpGenerate2dAtomCoordinatesFinal = tmpGenerate2dAtomCoordinates;
        this.exportTask = new Task<>() {
            @Override
            protected Void call() throws Exception{
                switch (anExportType) {
                    case FRAGMENT_CSV_FILE:
                        tmpExporter.exportCsvFile(
                                getItemsListOfSelectedFragmenterByTabId(TabNames.Fragments),
                                ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle(),
                                settingsContainer.getCsvExportSeparatorSetting(),
                                TabNames.Fragments
                        );
                        break;
                    case PDB_FILE:
                        tmpExporter.exportFragmentsAsChemicalFile(
                                getItemsListOfSelectedFragmenterByTabId(TabNames.Fragments),
                                ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle(),
                                ChemFileTypes.PDB,
                                tmpGenerate2dAtomCoordinatesFinal
                        );
                        break;
                    case FRAGMENT_PDF_FILE:
                        tmpExporter.exportPdfFile(
                                getItemsListOfSelectedFragmenterByTabId(TabNames.Fragments),
                                moleculeDataModelList,
                                ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle(),
                                TabNames.Fragments
                        );
                        break;
                    case SINGLE_SD_FILE:
                        tmpExporter.exportFragmentsAsChemicalFile(
                                getItemsListOfSelectedFragmenterByTabId(TabNames.Fragments),
                                ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle(),
                                ChemFileTypes.SDF,
                                tmpGenerate2dAtomCoordinatesFinal,
                                true
                        );
                        break;
                    case SD_FILE:
                        tmpExporter.exportFragmentsAsChemicalFile(
                                getItemsListOfSelectedFragmenterByTabId(TabNames.Fragments),
                                ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle(),
                                ChemFileTypes.SDF,
                                false
                        );
                        break;
                    case ITEM_CSV_FILE:
                        tmpExporter.exportCsvFile(
                                moleculeDataModelList,
                                ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle(),
                                settingsContainer.getCsvExportSeparatorSetting(),
                                TabNames.Itemization
                        );
                        break;
                    case ITEM_PDF_FILE:
                        tmpExporter.exportPdfFile(
                                getItemsListOfSelectedFragmenterByTabId(TabNames.Itemization),
                                moleculeDataModelList,
                                ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle(),
                                TabNames.Itemization
                        );
                        break;
                }
                return null;
            }
        };
        this.exportTask.setOnSucceeded(event ->{
            this.isExportRunningProperty.setValue(false);
            this.updateStatusBar(this.exporterThread, Message.get("Status.finished"));
        });
        this.exportTask.setOnCancelled(event -> {
            this.isExportRunningProperty.setValue(false);
            MainViewController.LOGGER.log(Level.SEVERE, "Export canceled");
            this.updateStatusBar(this.exporterThread, Message.get("Status.canceled"));
        });
        this.exportTask.setOnFailed(event -> {
            this.isExportRunningProperty.setValue(false);
            MainViewController.LOGGER.log(Level.WARNING, event.getSource().getException().toString(), event.getSource().getException());
            this.updateStatusBar(this.exporterThread, Message.get("Status.failed"));
            GuiUtil.guiMessageAlert(
                        Alert.AlertType.WARNING,
                        Message.get("Exporter.FragmentsTab.ExportNotPossible.title"),
                        Message.get("Exporter.FragmentsTab.ExportNotPossible.header"),
                        null);

        });
        this.exporterThread = new Thread(this.exportTask);
        this.exporterThread.setName(ThreadType.EXPORT_THREAD.getThreadName());
        this.exporterThread.setUncaughtExceptionHandler(LogUtil.getUncaughtExceptionHandler());
        this.exporterThread.setDaemon(false);
        this.exporterThread.setPriority(Thread.currentThread().getPriority() - 2); //magic number
        this.isExportRunningProperty.setValue(true);
        this.updateStatusBar(this.exporterThread, Message.get("Status.exporting"));
        this.exporterThread.start();
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
    /**
     * Opens PipelineSettingsView
     */
    private void openPipelineSettingsView(){
        PipelineSettingsViewController tmpPipelineSettingsViewController =
                new PipelineSettingsViewController(this.primaryStage, this.fragmentationService, this.moleculeDataModelList.size() > 0, this.isFragmentationRunning);
        if(tmpPipelineSettingsViewController.isFragmentationStarted()){
            this.startFragmentation(tmpPipelineSettingsViewController.isFragmentationStarted());
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
        ButtonBar tmpButtonBar = new ButtonBar();
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
    /**
     * Cancels import task and interrupts the corresponding thread
     */
    private void interruptImport(){
        this.importTask.cancel();
        this.importerThread.interrupt();
    }
    //
    /**
     * Cancels export task and interrupts the corresponding thread
     */
    private void interruptExport(){
        this.exportTask.cancel();
        this.exporterThread.interrupt();
    }
    //
    /**
     * Gets called by the cancel fragmentation button
     */
    private void interruptFragmentation(){
        //cancel() of the task was overridden to shut down the executor service in FragmentationService
        this.parallelFragmentationMainTask.cancel(true);
        this.cancelFragmentationButton.setVisible(false);
        this.fragmentationButton.setDisable(false);
    }
    //
    /**
     * Starts fragmentation for only one algorithm
     */
    private void startFragmentation(){
        this.startFragmentation(false);
    }
    //
    /**
     * Starts fragmentation task and opens fragment and itemization tabs
     */
    private void startFragmentation(boolean isPipelining){
        long tmpStartTime = System.nanoTime();
        LOGGER.info("Start of method startFragmentation");
        List<MoleculeDataModel> tmpSelectedMolecules = this.moleculeDataModelList.stream().filter(mol -> mol.isSelected()).collect(Collectors.toList());
        int tmpNumberOfCores = this.settingsContainer.getNumberOfTasksForFragmentationSetting();
        try{
            this.fragmentationButton.setDisable(true);
            this.cancelFragmentationButton.setVisible(true);
            this.parallelFragmentationMainTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    if(isPipelining){
                        MainViewController.this.fragmentationService.startPipelineFragmentation(tmpSelectedMolecules,
                                tmpNumberOfCores);
//                        fragmentationService.startPipelineFragmentationMolByMol(tmpSelectedMolecules, tmpNumberOfCores);
                    }
                    else{
                        MainViewController.this.fragmentationService.startSingleFragmentation(tmpSelectedMolecules,
                                tmpNumberOfCores);
                    }
                    return null;
                }

                @Override
                public boolean cancel(boolean anInterruptThread) {
                    MainViewController.this.fragmentationService.abortExecutor();
                    return super.cancel(anInterruptThread);
                }
            };
            this.parallelFragmentationMainTask.setOnSucceeded(event -> {
                //note: setOnSucceeded() takes place in the JavaFX GUI thread again but still runLater() is necessary to wait
                // for the thread to be free for the update
                Platform.runLater(()->{
                    try {
                        ObservableList<FragmentDataModel> tmpObservableFragments = FXCollections.observableArrayList();
                        Set<String> tmpKeys = this.fragmentationService.getFragments().keySet();
                        for(String tmpKey : tmpKeys){
                            tmpObservableFragments.add(this.fragmentationService.getFragments().get(tmpKey));
                        }
                        this.mapOfFragmentDataModelLists.put(this.fragmentationService.getCurrentFragmentationName(), tmpObservableFragments);
                        this.addFragmentationResultTabs(this.fragmentationService.getCurrentFragmentationName());
                        this.updateStatusBar(this.fragmentationThread, Message.get("Status.finished"));
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
            this.parallelFragmentationMainTask.setOnCancelled(event -> {
                this.updateStatusBar(this.fragmentationThread, Message.get("Status.canceled"));
                this.mainView.getMainMenuBar().getExportMenu().setDisable(false);
                this.fragmentationButton.setDisable(false);
                this.cancelFragmentationButton.setVisible(false);
                this.isFragmentationRunning = false;
            });
            this.parallelFragmentationMainTask.setOnFailed(event -> {
                this.updateStatusBar(this.fragmentationThread, Message.get("Status.failed"));
                this.mainView.getMainMenuBar().getExportMenu().setDisable(false);
                this.fragmentationButton.setDisable(false);
                this.cancelFragmentationButton.setVisible(false);
                this.isFragmentationRunning = false;
                LogUtil.getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), new Exception(event.getSource().toString()));
            });
            this.fragmentationThread = new Thread(this.parallelFragmentationMainTask);
            this.fragmentationThread.setName(ThreadType.FRAGMENTATION_THREAD.getThreadName());
            this.fragmentationThread.setUncaughtExceptionHandler(LogUtil.getUncaughtExceptionHandler());
            this.fragmentationThread.setPriority(Thread.currentThread().getPriority() - 2); //magic number, do not touch
            this.updateStatusBar(this.fragmentationThread, Message.get("Status.running"));
            this.isFragmentationRunning = true;
            this.fragmentationThread.start();
        } catch(Exception anException){
            MainViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            GuiUtil.guiExceptionAlert(Message.get("MainViewController.FragmentationError.Title"),
                    Message.get("MainViewController.FragmentationError.Header"),
                    Message.get("MainViewController.FragmentationError.Content"),
                    anException);
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
        tmpExportCsvButton.setTooltip(new Tooltip(Message.get("MainTabPane.fragments.buttonCSV.tooltip")));
        Button tmpExportPdfButton = new Button(Message.get("MainTabPane.fragments.buttonPDF.txt"));
        tmpExportPdfButton.setTooltip(new Tooltip(Message.get("MainTabPane.fragments.buttonPDF.tooltip")));
        Button tmpCancelExportButton =  new Button(Message.get("MainTabPane.fragments.buttonCancelExport.txt"));
        tmpCancelExportButton.setTooltip(new Tooltip(Message.get("MainTabPane.fragments.buttonCancelExport.tooltip")));
        tmpCancelExportButton.visibleProperty().bind(this.isExportRunningProperty);
        ButtonBar tmpButtonBarFragments = new ButtonBar();
        tmpButtonBarFragments.setPadding(new Insets(0,0,0,0));
        tmpExportCsvButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        tmpExportCsvButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpExportPdfButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        tmpExportPdfButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpCancelExportButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        tmpCancelExportButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpButtonBarFragments.getButtons().addAll(tmpExportCsvButton, tmpExportPdfButton, tmpCancelExportButton);
        tmpFragmentsTab.addNodeToGridPane(tmpButtonBarFragments, 0,1,1,1);
        tmpExportPdfButton.setOnAction(event-> this.exportFile(Exporter.ExportTypes.FRAGMENT_PDF_FILE));
        tmpExportCsvButton.setOnAction(event-> this.exportFile(Exporter.ExportTypes.FRAGMENT_CSV_FILE));
        tmpCancelExportButton.setOnAction(event -> this.interruptExport());
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
        tmpItemizationTabExportPDfButton.setTooltip(new Tooltip(Message.get("MainTabPane.itemizationTab.pdfButton.tooltip")));
        Button tmpItemizationExportCsvButton = new Button(Message.get("MainTabPane.itemizationTab.csvButton.txt"));
        tmpItemizationExportCsvButton.setTooltip(new Tooltip(Message.get("MainTabPane.itemizationTab.csvButton.tooltip")));
        ButtonBar tmpButtonBarItemization = new ButtonBar();
        tmpButtonBarItemization.setPadding(new Insets(0,0,0,0));
        tmpItemizationExportCsvButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        tmpItemizationExportCsvButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpItemizationTabExportPDfButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        tmpItemizationTabExportPDfButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        tmpButtonBarItemization.getButtons().addAll(tmpItemizationExportCsvButton, tmpItemizationTabExportPDfButton, tmpCancelExportButton);
        tmpItemizationTab.addNodeToGridPane(tmpButtonBarItemization, 0, 1,1,1);
        tmpItemizationExportCsvButton.setOnAction(event-> this.exportFile(Exporter.ExportTypes.ITEM_CSV_FILE));
        tmpItemizationTabExportPDfButton.setOnAction(event -> this.exportFile(Exporter.ExportTypes.ITEM_PDF_FILE));
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
                ((GridTabForTableView)this.mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle().equals(((GridTabForTableView)tab).getFragmentationNameOutOfTitle()) && tab.getId().equals(aTabName.name())
        ).findFirst().get())).getTableView()).getItemsList();
    }
    //
    /**
     * Updates StatusBar
     *
     * @param aThread Thread which was started or end
     * @param aMessage String message to display in StatusBar
     */
    private void updateStatusBar(Thread aThread, String aMessage){
        if(!this.taskList.contains(aThread)){
            this.taskList.add(aThread);
            this.mainView.getStatusBar().getStatusLabel().setText(aMessage);
            this.mainView.getStatusBar().getStatusLabel().setVisible(true);
            this.mainView.getStatusBar().getProgressBar().setVisible(true);
            return;
        }
        if(this.taskList.contains(aThread)){
            this.taskList.remove(aThread);
            if(this.taskList.isEmpty()){
                this.mainView.getStatusBar().getProgressBar().setVisible(false);
                this.mainView.getStatusBar().getStatusLabel().setText(aMessage);
                return;
            }
            this.mainView.getStatusBar().getStatusLabel().setText(
                    this.getStatusMessageByThreadType(
                            Objects.requireNonNull(ThreadType.get(
                                    this.taskList.get(this.taskList.size() - 1).getName()
                            ))
                    )
            );
        }
    }
    //
    /**
     * Returns status message as string by given ThreadType
     *
     * @param aThreadType ThreadType
     * @return String status message
     */
    private String getStatusMessageByThreadType(ThreadType aThreadType){
        switch(aThreadType){
            case FRAGMENTATION_THREAD:
                return Message.get("Status.running");
            case IMPORT_THREAD:
                return Message.get("Status.loading");
            case EXPORT_THREAD:
                return Message.get("Status.exporting");
            default:
                return "Could not find message";
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="public enum" defaultstate="collapsed">
    /**
     * Enum for different thread types
     */
    public enum ThreadType {
        FRAGMENTATION_THREAD("Fragmentation_Thread"),
        IMPORT_THREAD("Import_Thread"),
        EXPORT_THREAD("Export_Thread");

        private String threadName;

        ThreadType(String aThreadName){
            this.threadName = aThreadName;
        }

        public String getThreadName(){
            return this.threadName;
        }

        /**
         * Reverse lookup
         */
        public static ThreadType get(String aThreadName){
            for(ThreadType aType : values()){
                if(aType.threadName.equals(aThreadName)){
                    return aType;
                }
            }
            return null;
        }
    }
    //</editor-fold>
}

