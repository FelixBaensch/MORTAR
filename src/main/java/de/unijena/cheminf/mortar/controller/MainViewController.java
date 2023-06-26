/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2023  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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
import de.unijena.cheminf.mortar.gui.controls.GridTabForTableView;
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
import de.unijena.cheminf.mortar.model.util.CollectionUtil;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import de.unijena.cheminf.mortar.model.util.LogUtil;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
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
 * @version 1.0.0.0
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
     * ViewToolsManager
     */
    private ViewToolsManager viewToolsManager;
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
    private Task<List<String>> exportTask;
    /**
     * BooleanProperty whether import is running
     */
    private BooleanProperty isImportRunningProperty;
    /**
     * BooleanProperty whether export is running
     */
    private BooleanProperty isExportRunningProperty;
    /**
     * Thread safe list to hold running threads to update StatusBar
     */
    private CopyOnWriteArrayList<Thread> threadList;
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
     * @param aStage    Stage
     * @param aMainView MainView
     * @param anAppDir  String path to app dir
     */
    public MainViewController(Stage aStage, MainView aMainView, String anAppDir) {
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
        this.viewToolsManager = new ViewToolsManager();
        this.viewToolsManager.reloadViewToolsSettings();
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
        this.mapOfFragmentDataModelLists = new HashMap<>(CollectionUtil.calculateInitialHashCollectionCapacity(5));
        this.threadList = new CopyOnWriteArrayList();
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
        this.mainView.getMainMenuBar().getOpenMenuItem().addEventHandler(
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
                anEvent -> this.openPipelineSettingsView()
        );
        this.mainView.getMainMenuBar().getHistogramViewerMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.openHistogramView()
        );
        this.mainView.getMainMenuBar().getOverviewViewMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> {
                    if (this.mainTabPane.getSelectionModel().getSelectedItem().getId().equals(TabNames.MOLECULES.toString())) {
                        this.openOverviewView(OverviewViewController.DataSources.MOLECULES_TAB);
                    } else if (this.mainTabPane.getSelectionModel().getSelectedItem().getId().equals(TabNames.FRAGMENTS.toString())) {
                        this.openOverviewView(OverviewViewController.DataSources.FRAGMENTS_TAB);
                    } else if (this.mainTabPane.getSelectionModel().getSelectedItem().getId().equals(TabNames.ITEMIZATION.toString())) {
                        //should not happen, since menu item should be disabled if items tab is active
                        throw new IllegalStateException();
                    }
                }
        );
        this.primaryStage.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, (this::closeWindowEvent));
        this.mainView.getMainMenuBar().getAboutViewMenuItem().setOnAction(actionEvent -> new AboutViewController(this.primaryStage));
        this.scene.addEventFilter(KeyEvent.KEY_PRESSED, keyEvent -> {
            GridTabForTableView tmpGrid = ((GridTabForTableView) this.mainTabPane.getSelectionModel().getSelectedItem());
            if (tmpGrid == null) {
                keyEvent.consume();
                return;
            }
            if (GuiDefinitions.KEY_CODE_LAST_PAGE.match(keyEvent) || keyEvent.getCode() == KeyCode.END) {
                tmpGrid.getPagination().setCurrentPageIndex(tmpGrid.getPagination().getPageCount() - 1);
                keyEvent.consume();
            }
            else if (GuiDefinitions.KEY_CODE_FIRST_PAGE.match(keyEvent) || keyEvent.getCode() == KeyCode.HOME) {
                tmpGrid.getPagination().setCurrentPageIndex(0);
                keyEvent.consume();
            }
            else if (keyEvent.getCode() == KeyCode.RIGHT || keyEvent.getCode() == KeyCode.PAGE_UP) {
                tmpGrid.getPagination().setCurrentPageIndex(tmpGrid.getPagination().getCurrentPageIndex() + 1);
                keyEvent.consume();
            }
            else if (keyEvent.getCode() == KeyCode.LEFT || keyEvent.getCode() == KeyCode.PAGE_DOWN) {
                tmpGrid.getPagination().setCurrentPageIndex(tmpGrid.getPagination().getCurrentPageIndex() - 1);
                keyEvent.consume();
            }
        });
        this.mainTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                if (newValue == null) {
                    return;
                }
                if (newValue.getId().equals(TabNames.MOLECULES.toString())) {
                    this.mainView.getMainMenuBar().getHistogramViewerMenuItem().setDisable(true);
                } else {
                    this.mainView.getMainMenuBar().getHistogramViewerMenuItem().setDisable(false);
                }
                if (newValue.getId().equals(TabNames.ITEMIZATION.toString())) {
                    this.mainView.getMainMenuBar().getOverviewViewMenuItem().setDisable(true);
                } else {
                    this.mainView.getMainMenuBar().getOverviewViewMenuItem().setDisable(false);
                }
            });
        });
    }
    //

    /**
     * Closes application
     */
    private void closeApplication(int aStatus) {
        if (moleculeDataModelList.size() > 0) {
            if (!this.isFragmentationStopAndDataLossConfirmed()) {
                return;
            }
        }
        this.settingsContainer.preserveSettings();
        this.viewToolsManager.persistViewToolsSettings();
        this.fragmentationService.persistFragmenterSettings();
        this.fragmentationService.persistSelectedFragmenterAndPipeline();
        if (this.isFragmentationRunning) {
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
        ButtonType tmpConfirmationResult;
        if (this.isFragmentationRunning) {
            tmpConfirmationResult = GuiUtil.guiConfirmationAlert(
                    Message.get("MainViewController.Warning.FragmentationRunning.Title"),
                    Message.get("MainViewController.Warning.FragmentationRunning.Header"),
                    Message.get("MainViewController.Warning.FragmentationRunning.Content"));
        } else {
            tmpConfirmationResult = GuiUtil.guiConfirmationAlert(
                    Message.get("MainViewController.Warning.DataLoss.Title"),
                    Message.get("MainViewController.Warning.DataLoss.Header"),
                    Message.get("MainViewController.Warning.DataLoss.Content"));
        }
        return tmpConfirmationResult == ButtonType.OK;
    }
    //

    /**
     * Closes the application via closeApplication method when close window event was fired
     *
     * @param anEvent WindowEvent
     */
    private void closeWindowEvent(WindowEvent anEvent) {
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
        if (this.moleculeDataModelList.size() > 0) {
            if (!this.isFragmentationStopAndDataLossConfirmed()) {
                return;
            }
            this.fragmentationService.clearCache();
        }
        Importer tmpImporter = new Importer(this.settingsContainer);
        File tmpFile = tmpImporter.openFile(aParentStage);
        if (Objects.isNull(tmpFile)) {
            return;
        }
        if (this.isFragmentationRunning) {
            this.interruptFragmentation();
        }
        if (this.isImportRunningProperty.get()) {
            this.interruptImport();
        }
        if (this.isExportRunningProperty.get()) {
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
                    this.updateStatusBar(this.importerThread, Message.get("Status.importFailed"));
                    this.isImportRunningProperty.setValue(false);
                    return;
                }
                this.mainView.getMainMenuBar().getExportMenu().setDisable(true);
                this.mainView.getMainMenuBar().getHistogramViewerMenuItem().setDisable(true);
                this.mainView.getMainMenuBar().getOverviewViewMenuItem().setDisable(false);
                this.primaryStage.setTitle(Message.get("Title.text") + " - " + tmpImporter.getFileName() + " - " + tmpAtomContainerSet.getAtomContainerCount() +
                        " " + Message.get((tmpAtomContainerSet.getAtomContainerCount() == 1 ? "Title.molecule" : "Title.molecules")));
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
                this.updateStatusBar(this.importerThread, Message.get("Status.imported"));
                this.isImportRunningProperty.setValue(false);
                this.mainView.getMainCenterPane().setStyle("-fx-background-image: none");
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
        this.updateStatusBar(this.importerThread, Message.get("Status.importing"));
        this.importerThread.start();
    }
    //

    /**
     * Exports the given type of file
     *
     * @param anExportType Enum to specify what type of file to export
     */
    private void exportFile(Exporter.ExportTypes anExportType) {
        if ((this.mainTabPane.getSelectionModel().getSelectedItem()).getId().equals(TabNames.MOLECULES.toString())) {
            GuiUtil.guiConfirmationAlert(Message.get("Exporter.confirmationAlert.moleculesTabSelected.title"),
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
                if (this.getItemsListOfSelectedFragmenterByTabId(TabNames.FRAGMENTS) == null ||
                        this.getItemsListOfSelectedFragmenterByTabId(TabNames.FRAGMENTS).size() == 0 ||
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
                if (this.getItemsListOfSelectedFragmenterByTabId(TabNames.ITEMIZATION) == null ||
                        this.getItemsListOfSelectedFragmenterByTabId(TabNames.ITEMIZATION).size() == 0 ||
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
        if (this.isExportRunningProperty.get()) {
            this.interruptExport();
        }
        tmpExporter.saveFile(this.primaryStage, anExportType, ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle());
        if(tmpExporter.getFile() == null){
            return;
        }
        boolean tmpGenerate2dAtomCoordinates = false;
        switch (anExportType) {
            case PDB_FILE:
            case SINGLE_SD_FILE:
                if (!ChemUtil.checkMoleculeListForCoordinates(getItemsListOfSelectedFragmenterByTabId(TabNames.FRAGMENTS))) {
                    ButtonType tmpConfirmationResult = GuiUtil.guiConfirmationAlert(
                            Message.get("Exporter.FragmentsTab.ConfirmationAlert.No3dInformationAvailable.title"),
                            Message.get("Exporter.FragmentsTab.ConfirmationAlert.No3dInformationAvailable.header"),
                            Message.get("Exporter.FragmentsTab.ConfirmationAlert.No3dInformationAvailable.text")
                    );
                    tmpGenerate2dAtomCoordinates = tmpConfirmationResult == ButtonType.OK;
                }
                break;
        }
        boolean tmpGenerate2dAtomCoordinatesFinal = tmpGenerate2dAtomCoordinates;
        this.exportTask = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                switch (anExportType) {
                    case FRAGMENT_CSV_FILE:
                        return tmpExporter.exportCsvFile(
                                getItemsListOfSelectedFragmenterByTabId(TabNames.FRAGMENTS),
                                ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle(),
                                settingsContainer.getCsvExportSeparatorSetting(),
                                TabNames.FRAGMENTS
                        );
                    case PDB_FILE:
                        return tmpExporter.exportFragmentsAsChemicalFile(
                                getItemsListOfSelectedFragmenterByTabId(TabNames.FRAGMENTS),
                                ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle(),
                                ChemFileTypes.PDB,
                                tmpGenerate2dAtomCoordinatesFinal
                        );
                    case FRAGMENT_PDF_FILE:
                        return tmpExporter.exportPdfFile(
                                getItemsListOfSelectedFragmenterByTabId(TabNames.FRAGMENTS),
                                moleculeDataModelList,
                                ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle(),
                                TabNames.FRAGMENTS
                        );
                    case SINGLE_SD_FILE:
                        return tmpExporter.exportFragmentsAsChemicalFile(
                                getItemsListOfSelectedFragmenterByTabId(TabNames.FRAGMENTS),
                                ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle(),
                                ChemFileTypes.SDF,
                                tmpGenerate2dAtomCoordinatesFinal,
                                true
                        );
                    case SD_FILE:
                        return tmpExporter.exportFragmentsAsChemicalFile(
                                getItemsListOfSelectedFragmenterByTabId(TabNames.FRAGMENTS),
                                ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle(),
                                ChemFileTypes.SDF,
                                false
                        );
                    case ITEM_CSV_FILE:
                        return tmpExporter.exportCsvFile(
                                moleculeDataModelList,
                                ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle(),
                                settingsContainer.getCsvExportSeparatorSetting(),
                                TabNames.ITEMIZATION
                        );
                    case ITEM_PDF_FILE:
                        return tmpExporter.exportPdfFile(
                                getItemsListOfSelectedFragmenterByTabId(TabNames.ITEMIZATION),
                                moleculeDataModelList,
                                ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle(),
                                TabNames.ITEMIZATION
                        );
                }
                return null;
            }
        };
        this.exportTask.setOnSucceeded(event -> {
            this.isExportRunningProperty.setValue(false);
            this.updateStatusBar(this.exporterThread, Message.get("Status.finished"));
            List<String> tmpFailedExportFragments = this.exportTask.getValue();
            if (tmpFailedExportFragments == null) {
                GuiUtil.guiMessageAlert(Alert.AlertType.WARNING,
                        Message.get("Exporter.FragmentsTab.ExportNotPossible.title"),
                        Message.get("Exporter.FragmentsTab.ExportNotPossible.header"),
                        null);
            }
            if (tmpFailedExportFragments.size() > 0) {
                StringBuilder tmpStringBuilder = new StringBuilder();
                for (String tmpFragmentName : tmpFailedExportFragments) {
                    tmpStringBuilder.append(tmpFragmentName + "\n");
                }
                GuiUtil.guiExpandableAlert(
                        Alert.AlertType.WARNING.toString(),
                        Message.get("Exporter.FragmentsTab.ExportNotPossible.title"),
                        Message.get("Exporter.FragmentsTab.ExportNotPossible.header"),
                        tmpFailedExportFragments.size() + " " + Message.get("Exporter.FragmentsTab.ExportNotPossible.label"),
                        tmpStringBuilder.toString()
                );
            }
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
    private void openFragmentationSettingsView() {
        FragmentationSettingsViewController tmpFragmentationSettingsViewController =
                new FragmentationSettingsViewController(this.primaryStage, this.fragmentationService.getFragmenters(), this.fragmentationService.getSelectedFragmenter().getFragmentationAlgorithmName());
    }
    //

    /**
     * Opens PipelineSettingsView
     */
    private void openPipelineSettingsView() {
        PipelineSettingsViewController tmpPipelineSettingsViewController =
                new PipelineSettingsViewController(this.primaryStage, this.fragmentationService, this.moleculeDataModelList.size() > 0, this.isFragmentationRunning);
        if (tmpPipelineSettingsViewController.isFragmentationStarted()) {
            this.startFragmentation(tmpPipelineSettingsViewController.isFragmentationStarted());
        }
    }
    //

    /**
     * Opens HistogramView
     */
    private void openHistogramView()  {
        List<MoleculeDataModel> tmpMoleculesList = this.getItemsListOfSelectedFragmenterByTabId(TabNames.FRAGMENTS);
        List<FragmentDataModel> tmpFragmentsList = new ArrayList<>(tmpMoleculesList.size());
        for (MoleculeDataModel tmpMolecule : tmpMoleculesList) {
            tmpFragmentsList.add((FragmentDataModel) tmpMolecule);
        }
        this.viewToolsManager.openHistogramView(this.primaryStage, tmpFragmentsList);
    }
    //

    /**
     * Adds CheckMenuItems for fragmentation algorithms to MainMenuBar
     */
    private void addFragmentationAlgorithmCheckMenuItems() {
        ToggleGroup tmpToggleGroup = new ToggleGroup();
        for (IMoleculeFragmenter tmpFragmenter : this.fragmentationService.getFragmenters()) {
            RadioMenuItem tmpRadioMenuItem = new RadioMenuItem(tmpFragmenter.getFragmentationAlgorithmName());
            tmpRadioMenuItem.setToggleGroup(tmpToggleGroup);
            this.mainView.getMainMenuBar().getFragmentationAlgorithmMenu().getItems().add(tmpRadioMenuItem);
            if (!Objects.isNull(this.fragmentationService.getSelectedFragmenter()) && tmpFragmenter.getFragmentationAlgorithmName().equals(this.fragmentationService.getSelectedFragmenter().getFragmentationAlgorithmName())) {
                tmpToggleGroup.selectToggle(tmpRadioMenuItem);
            }
        }
        tmpToggleGroup.selectedToggleProperty().addListener((observableValue, oldValue, newValue) -> {
            if (tmpToggleGroup.getSelectedToggle() != null) {
                this.fragmentationService.setSelectedFragmenter(((RadioMenuItem) newValue).getText());
                this.fragmentationService.setSelectedFragmenterNameProperty(((RadioMenuItem) newValue).getText());
            }
        });
    }
    //

    /**
     * Opens settings view for global settings
     */
    private void openGlobalSettingsView() {
        SettingsViewController tmpSettingsViewController = new SettingsViewController(this.primaryStage, this.settingsContainer);
        Platform.runLater(() -> {
            if (tmpSettingsViewController.hasRowsPerPageChanged()) {
                for (Tab tmpTab : this.mainTabPane.getTabs()) {
                    TableView tmpTableView = ((GridTabForTableView) tmpTab).getTableView();
                    int tmpListSize = 0;
                    tmpListSize = ((IDataTableView) tmpTableView).getItemsList().size();
                    int tmpPageIndex = ((GridTabForTableView) tmpTab).getPagination().getCurrentPageIndex();
                    int tmpRowsPerPage = this.settingsContainer.getRowsPerPageSetting();
                    int tmpPageCount = tmpListSize / tmpRowsPerPage;
                    if (tmpListSize % tmpRowsPerPage > 0) {
                        tmpPageCount++;
                    }
                    if (tmpPageIndex > tmpPageCount) {
                        tmpPageIndex = tmpPageCount;
                    }
                    ((GridTabForTableView) tmpTab).getPagination().setPageCount(tmpPageCount);
                    ((GridTabForTableView) tmpTab).getPagination().setCurrentPageIndex(tmpPageIndex);
                    ((GridTabForTableView) tmpTab).getTableView().refresh();
                    GuiUtil.setImageStructureHeight(((GridTabForTableView) tmpTab).getTableView(), ((GridTabForTableView) tmpTab).getTableView().getHeight(), this.settingsContainer);
                    ((GridTabForTableView) tmpTab).getTableView().refresh();
                }
            }
            if (tmpSettingsViewController.hasKeepAtomContainerInDataModelChanged()) {
                for (MoleculeDataModel tmpMoleculeDataModel : this.moleculeDataModelList) {
                    tmpMoleculeDataModel.setKeepAtomContainer(this.settingsContainer.getKeepAtomContainerInDataModelSetting());
                }
                for (ObservableList<FragmentDataModel> tmpFragmentDataModelList : this.mapOfFragmentDataModelLists.values()) {
                    for (FragmentDataModel tmpFragmentDataModel : tmpFragmentDataModelList) {
                        tmpFragmentDataModel.setKeepAtomContainer(this.settingsContainer.getKeepAtomContainerInDataModelSetting());
                    }
                }
            }
        });
    }
    //

    /**
     * Opens OverviewView
     *
     * @param aDataSource Source of the data to be shown in the overview view
     */
    private void openOverviewView(OverviewViewController.DataSources aDataSource) {
        try {
            switch (aDataSource) {
                case MOLECULES_TAB -> {
                    if (!(this.mainTabPane.getSelectionModel().getSelectedItem().getId().equals(TabNames.MOLECULES.toString())))
                        //should not happen
                        throw new IllegalStateException();
                    this.viewToolsManager.openOverviewView(
                            this.primaryStage,
                            aDataSource,
                            ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getTitle(),
                            getItemsListOfSelectedFragmenterByTabId(TabNames.MOLECULES)
                    );
                }
                case FRAGMENTS_TAB -> {
                    if (!(this.mainTabPane.getSelectionModel().getSelectedItem().getId().equals(TabNames.FRAGMENTS.toString())))
                        //should not happen
                        throw new IllegalStateException();
                    this.viewToolsManager.openOverviewView(
                            this.primaryStage,
                            aDataSource,
                            ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getTitle(),
                            this.getItemsListOfSelectedFragmenterByTabId(TabNames.FRAGMENTS)
                    );
                }
                case PARENT_MOLECULES_SAMPLE -> {
                    if (!(this.mainTabPane.getSelectionModel().getSelectedItem().getId().equals(TabNames.FRAGMENTS.toString())))
                        //should not happen
                        throw new IllegalStateException();
                    //Parent-Molecules of the Fragments-Tab (showing all fragments of one molecule in the overview view)
                    GridTabForTableView tmpSelectedTab = (GridTabForTableView) this.mainTabPane.getSelectionModel().getSelectedItem();
                    //IllegalStateException if there is more than one cell selected
                    if (((TableView<?>) tmpSelectedTab.getTableView()).getSelectionModel().getSelectedCells().size() > 1) {
                        //should not happen
                        throw new IllegalStateException();
                    }
                    //getting the data for the overview view
                    List<MoleculeDataModel> tmpDataForOverviewView = new ArrayList<>();
                    int tmpSelectedRowIndex = ((TableView<?>) tmpSelectedTab.getTableView()).getSelectionModel().getSelectedCells().get(0).getRow();
                    int tmpIndexInDataList = tmpSelectedTab.getPagination().getCurrentPageIndex() * this.settingsContainer.getRowsPerPageSetting() + tmpSelectedRowIndex;
                    //adding the fragment itself
                    tmpDataForOverviewView.add(((IDataTableView) tmpSelectedTab.getTableView()).getItemsList().get(tmpIndexInDataList));
                    //adding the sample of parent molecules
                    tmpDataForOverviewView.addAll(((FragmentDataModel) ((IDataTableView) tmpSelectedTab.getTableView()).getItemsList().get(tmpIndexInDataList)).getParentMolecules());
                    this.viewToolsManager.openOverviewView(
                            this.primaryStage,
                            OverviewViewController.DataSources.PARENT_MOLECULES_SAMPLE,
                            null,
                            tmpDataForOverviewView
                    );
                }
                case ITEM_WITH_FRAGMENTS_SAMPLE -> {
                    if (!(this.mainTabPane.getSelectionModel().getSelectedItem().getId().equals(TabNames.ITEMIZATION.toString())))
                        //should not happen
                        throw new IllegalStateException();
                    //Items-Tab (showing all fragments of one molecule in the overview view)
                    GridTabForTableView tmpSelectedTab = (GridTabForTableView) this.mainTabPane.getSelectionModel().getSelectedItem();
                    //IllegalStateException if there is more than one cell selected
                    if (((TableView<?>) tmpSelectedTab.getTableView()).getSelectionModel().getSelectedCells().size() > 1) {
                        //should not happen
                        throw new IllegalStateException();
                    }
                    //getting the data for the overview view
                    List<MoleculeDataModel> tmpDataForOverviewView = new ArrayList<>();
                    int tmpSelectedRowIndex = ((TableView<?>) tmpSelectedTab.getTableView()).getSelectionModel().getSelectedCells().get(0).getRow();
                    int tmpIndexInDataList = tmpSelectedTab.getPagination().getCurrentPageIndex() * this.settingsContainer.getRowsPerPageSetting() + tmpSelectedRowIndex;
                    //adding the item itself
                    tmpDataForOverviewView.add(((IDataTableView) tmpSelectedTab.getTableView()).getItemsList().get(tmpIndexInDataList));
                    //adding the sample of fragments
                    tmpDataForOverviewView.addAll(((IDataTableView) tmpSelectedTab.getTableView()).getItemsList().get(tmpIndexInDataList).getFragmentsOfSpecificAlgorithm(tmpSelectedTab.getFragmentationNameOutOfTitle()));
                    this.viewToolsManager.openOverviewView(
                            this.primaryStage,
                            OverviewViewController.DataSources.ITEM_WITH_FRAGMENTS_SAMPLE,
                            null,
                            tmpDataForOverviewView
                    );
                }
                default -> {
                    //should not happen
                    throw new IllegalStateException();
                }
            }
        } catch (IllegalStateException anIllegalStateException) {
            MainViewController.LOGGER.log(Level.SEVERE, anIllegalStateException.toString(), anIllegalStateException);
            return;
        }
        //
        int tmpIndexOfMoleculeDataModelToReturnTo = this.viewToolsManager.getCachedIndexOfStructureInMoleculeDataModelList();
        //since -1 is returned, if no specific structure should be shown
        if (tmpIndexOfMoleculeDataModelToReturnTo >= 0) {
            //go to page showing the structure of the MoleculeDataModel with the given index
            int tmpNewPageIndex = tmpIndexOfMoleculeDataModelToReturnTo / this.settingsContainer.getRowsPerPageSetting();
            ((GridTabForTableView) this.mainTabPane.getSelectionModel().getSelectedItem()).getPagination()
                    .setCurrentPageIndex(tmpNewPageIndex);
            TableView tmpSelectedTabTableView = ((GridTabForTableView) this.mainTabPane.getSelectionModel()
                    .getSelectedItem()).getTableView();
            if (tmpSelectedTabTableView.getClass() == MoleculesDataTableView.class) {
                //select structure cell
                int tmpRowIndexOfStructure = tmpIndexOfMoleculeDataModelToReturnTo
                        % this.settingsContainer.getRowsPerPageSetting();
                tmpSelectedTabTableView.getSelectionModel().clearSelection();
                tmpSelectedTabTableView.getSelectionModel().select(tmpRowIndexOfStructure,
                        ((MoleculesDataTableView) tmpSelectedTabTableView).getStructureColumn());
            } else if (tmpSelectedTabTableView.getClass() == FragmentsDataTableView.class) {
                //select structure cell
                int tmpRowIndexOfStructure = tmpIndexOfMoleculeDataModelToReturnTo
                        % this.settingsContainer.getRowsPerPageSetting();
                tmpSelectedTabTableView.getSelectionModel().clearSelection();
                tmpSelectedTabTableView.getSelectionModel().select(tmpRowIndexOfStructure,
                        ((FragmentsDataTableView) tmpSelectedTabTableView).getStructureColumn());
            }
        }
        this.viewToolsManager.resetCachedIndexOfStructureInMoleculeDataModelList();
    }
    //

    /**
     * Opens molecules tab
     */
    private void openMoleculesTab() {
        this.moleculesDataTableView = new MoleculesDataTableView();
        this.moleculesDataTableView.setItemsList(this.moleculeDataModelList);
        GridTabForTableView tmpMoleculesTab = new GridTabForTableView(Message.get("MainTabPane.moleculesTab.title"), TabNames.MOLECULES.name(), this.moleculesDataTableView);
        this.mainTabPane.getTabs().add(tmpMoleculesTab);
        int tmpRowsPerPage = this.settingsContainer.getRowsPerPageSetting();
        int tmpPageCount = this.moleculeDataModelList.size() / tmpRowsPerPage;
        if (this.moleculeDataModelList.size() % tmpRowsPerPage > 0) {
            tmpPageCount++;
        }
        if(this.moleculeDataModelList.size() == 0){
            tmpPageCount = 1;
        }
        Pagination tmpPagination = new Pagination(tmpPageCount, 0);
        tmpPagination.setSkin(new CustomPaginationSkin(tmpPagination));
        tmpPagination.setPageFactory((pageIndex) -> this.moleculesDataTableView.createMoleculeTableViewPage(pageIndex, this.settingsContainer));
        VBox.setVgrow(tmpPagination, Priority.ALWAYS);
        HBox.setHgrow(tmpPagination, Priority.ALWAYS);
        tmpMoleculesTab.addPaginationToGridPane(tmpPagination);
        HBox tmpFragmentationButtonsHBox = new HBox();
        tmpFragmentationButtonsHBox.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE));
        tmpFragmentationButtonsHBox.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpFragmentationButtonsHBox.setAlignment(Pos.CENTER_LEFT);
        this.fragmentationButton = new Button();
        this.fragmentationButton.textProperty().bind(this.fragmentationService.selectedFragmenterNamePropertyProperty());
        Tooltip tmpTooltip = new Tooltip();
        tmpTooltip.textProperty().bind(Bindings.format(Message.get("MainTabPane.moleculesTab.fragmentButton.text"), this.fragmentationService.selectedFragmenterNamePropertyProperty()));
        this.fragmentationButton.setTooltip(tmpTooltip);
        double tmpTextWidth = new Text(this.fragmentationService.getSelectedFragmenterNameProperty()).getLayoutBounds().getWidth() + 20;
        this.fragmentationButton.setPrefWidth(tmpTextWidth);
        this.fragmentationButton.setMinWidth(tmpTextWidth);
        this.fragmentationButton.setMaxWidth(tmpTextWidth);
        this.fragmentationButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        this.fragmentationService.selectedFragmenterNamePropertyProperty().addListener((observable, oldValue, newValue) -> {
            double tmpTextWidthChange = new Text(this.fragmentationService.getSelectedFragmenterNameProperty()).getLayoutBounds().getWidth() + 20;
            this.fragmentationButton.setPrefWidth(tmpTextWidthChange);
            this.fragmentationButton.setMinWidth(tmpTextWidthChange);
            this.fragmentationButton.setMaxWidth(tmpTextWidthChange);
        });
        tmpFragmentationButtonsHBox.getChildren().add(this.fragmentationButton);
        this.cancelFragmentationButton = new Button(Message.get("MainTabPane.moleculesTab.cancelFragmentationButton.text"));
        this.cancelFragmentationButton.setTooltip(new Tooltip(Message.get("MainTabPane.moleculesTab.cancelFragmentationButton.tooltip")));
        this.cancelFragmentationButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.cancelFragmentationButton.setMinWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.cancelFragmentationButton.setMaxWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.cancelFragmentationButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        this.cancelFragmentationButton.setVisible(false);;
        tmpFragmentationButtonsHBox.getChildren().add(this.cancelFragmentationButton);
        tmpMoleculesTab.addNodeToGridPane(tmpFragmentationButtonsHBox, 0, 1, 1, 1);
        this.fragmentationButton.setOnAction(event -> {
            this.startFragmentation();
        });
        this.cancelFragmentationButton.setOnAction(event -> {
            this.interruptFragmentation();
        });
        HBox tmpViewButtonsHBox = new HBox();
        tmpViewButtonsHBox.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE));
        tmpViewButtonsHBox.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpViewButtonsHBox.setAlignment(Pos.CENTER_RIGHT);
        tmpViewButtonsHBox.setMaxWidth(GuiDefinitions.GUI_GRIDPANE_FOR_NODE_ALIGNMENT_THIRD_COL_WIDTH);
        Button tmpOpenOverviewViewButton = GuiUtil.getButtonOfStandardSize(Message.get("MainView.showOverviewViewButton.text"));
        tmpOpenOverviewViewButton.setTooltip(new Tooltip(Message.get("MainView.showOverviewViewButton.tooltip")));
        tmpViewButtonsHBox.getChildren().add(tmpOpenOverviewViewButton);
        tmpMoleculesTab.addNodeToGridPane(tmpViewButtonsHBox, 2, 1, 1, 1);
        tmpOpenOverviewViewButton.setOnAction(event -> this.openOverviewView(OverviewViewController.DataSources.MOLECULES_TAB));
        this.moleculesDataTableView.addTableViewHeightListener(this.settingsContainer);
        this.moleculesDataTableView.getCopyMenuItem().setOnAction(event -> GuiUtil.copySelectedTableViewCellsToClipboard(this.moleculesDataTableView));
        this.moleculesDataTableView.setOnKeyPressed(event -> {
            if (GuiDefinitions.KEY_CODE_COPY.match(event)) {
                GuiUtil.copySelectedTableViewCellsToClipboard(this.moleculesDataTableView);
            }
        });
        this.moleculesDataTableView.setOnSort((EventHandler<SortEvent<TableView>>) event -> {
            GuiUtil.sortTableViewGlobally(event, tmpPagination, tmpRowsPerPage);
         });
        this.moleculesDataTableView.widthProperty().addListener((observable, oldValue, newValue) -> {
            for(Object tmpObject : this.moleculesDataTableView.getItems()) {
                ((MoleculeDataModel) tmpObject).setStructureImageWidth(this.moleculesDataTableView.getStructureColumn().getWidth());
            }
        });
    }
    //

    /**
     * Cancels import task and interrupts the corresponding thread
     */
    private void interruptImport() {
        this.importTask.cancel();
        this.importerThread.interrupt();
    }
    //

    /**
     * Cancels export task and interrupts the corresponding thread
     */
    private void interruptExport() {
        this.exportTask.cancel();
        this.exporterThread.interrupt();
    }
    //

    /**
     * Gets called by the cancel fragmentation button
     */
    private void interruptFragmentation() {
        //cancel() of the task was overridden to shut down the executor service in FragmentationService
        this.parallelFragmentationMainTask.cancel(true);
        this.cancelFragmentationButton.setVisible(false);
        this.fragmentationButton.setDisable(false);
    }
    //

    /**
     * Starts fragmentation for only one algorithm
     */
    private void startFragmentation() {
        this.startFragmentation(false);
    }
    //

    /**
     * Starts fragmentation task and opens fragment and itemization tabs
     */
    private void startFragmentation(boolean isPipelining) {
        long tmpStartTime = System.nanoTime();
        this.cancelFragmentationButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.cancelFragmentationButton.setMinWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.cancelFragmentationButton.setMaxWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        LOGGER.info("Start of method startFragmentation");
        List<MoleculeDataModel> tmpSelectedMolecules = this.moleculeDataModelList.stream().filter(mol -> mol.isSelected()).collect(Collectors.toList());
        int tmpNumberOfCores = this.settingsContainer.getNumberOfTasksForFragmentationSetting();
        try {
            this.fragmentationButton.setDisable(true);
            this.cancelFragmentationButton.setVisible(true);
            this.parallelFragmentationMainTask = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    if (isPipelining) {
                            MainViewController.this.fragmentationService.startPipelineFragmentation(tmpSelectedMolecules,
                                    tmpNumberOfCores);
//                        fragmentationService.startPipelineFragmentationMolByMol(tmpSelectedMolecules, tmpNumberOfCores);
                    } else {
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
                Platform.runLater(() -> {
                    try {
                        ObservableList<FragmentDataModel> tmpObservableFragments = FXCollections.observableArrayList();
                        Set<String> tmpKeys = this.fragmentationService.getFragments().keySet();
                        for (String tmpKey : tmpKeys) {
                            tmpObservableFragments.add(this.fragmentationService.getFragments().get(tmpKey));
                        }
                        this.mapOfFragmentDataModelLists.put(this.fragmentationService.getCurrentFragmentationName(), tmpObservableFragments);
                        this.addFragmentationResultTabs(this.fragmentationService.getCurrentFragmentationName());
                        this.updateStatusBar(this.fragmentationThread, Message.get("Status.finished"));
                        this.mainView.getMainMenuBar().getExportMenu().setDisable(false);
                        this.mainView.getMainMenuBar().getHistogramViewerMenuItem().setDisable(false);
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
        } catch (Exception anException) {
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
    private void addFragmentationResultTabs(String aFragmentationName) {
        //fragments tab
        Tab tmpFragmentsTab = this.createFragmentsTab(aFragmentationName);
        //itemization tab
        Tab tmpItemsTab = this.createItemsTab(aFragmentationName);
        //
        this.mainTabPane.getSelectionModel().select(tmpFragmentsTab);
    }
    //

    /**
     * Creates and returns a tab, which visualizes the resulting fragments of the fragmentation with given name
     *
     * @param aFragmentationName String, unique name for fragmentation job
     * @return Tab
     */
    private Tab createFragmentsTab(String aFragmentationName){
        FragmentsDataTableView tmpFragmentsDataTableView = new FragmentsDataTableView();
        GridTabForTableView tmpFragmentsTab = new GridTabForTableView(Message.get("MainTabPane.fragmentsTab.title") + " - " + aFragmentationName, TabNames.FRAGMENTS.name(), tmpFragmentsDataTableView);
        this.mainTabPane.getTabs().add(tmpFragmentsTab);
        ObservableList<MoleculeDataModel> tmpList = FXCollections.observableArrayList(this.mapOfFragmentDataModelLists.get(aFragmentationName));
        for(MoleculeDataModel tmpMoleculeDataModel : tmpList){
            tmpMoleculeDataModel.setStructureImageWidth(tmpFragmentsDataTableView.getStructureColumn().getWidth());
        }
        tmpFragmentsDataTableView.setItemsList(tmpList);
        int tmpRowsPerPage = this.settingsContainer.getRowsPerPageSetting();
        int tmpPageCount = tmpList.size() / tmpRowsPerPage;
        if (tmpList.size() % tmpRowsPerPage > 0) {
            tmpPageCount++;
        }
        if(tmpList.isEmpty() || tmpList.size() == 0){
            tmpPageCount = 1;
        }
        Pagination tmpPagination = new Pagination(tmpPageCount, 0);
        tmpPagination.setSkin(new CustomPaginationSkin(tmpPagination));
        tmpPagination.setPageFactory((pageIndex) -> tmpFragmentsDataTableView.createFragmentsTableViewPage(pageIndex, this.settingsContainer));
        VBox.setVgrow(tmpPagination, Priority.ALWAYS);
        HBox.setHgrow(tmpPagination, Priority.ALWAYS);
        tmpFragmentsTab.addPaginationToGridPane(tmpPagination);
        Button tmpExportCsvButton = GuiUtil.getButtonOfStandardSize(Message.get("MainTabPane.fragments.buttonCSV.txt"));
        tmpExportCsvButton.setTooltip(new Tooltip(Message.get("MainTabPane.fragments.buttonCSV.tooltip")));
        Button tmpExportPdfButton = GuiUtil.getButtonOfStandardSize(Message.get("MainTabPane.fragments.buttonPDF.txt"));
        tmpExportPdfButton.setTooltip(new Tooltip(Message.get("MainTabPane.fragments.buttonPDF.tooltip")));
        Button tmpCancelExportButton = GuiUtil.getButtonOfStandardSize(Message.get("MainTabPane.fragments.buttonCancelExport.txt"));
        tmpCancelExportButton.setTooltip(new Tooltip(Message.get("MainTabPane.fragments.buttonCancelExport.tooltip")));
        tmpCancelExportButton.visibleProperty().bind(this.isExportRunningProperty);
        HBox tmpExportButtonsHBox = new HBox();
        tmpExportButtonsHBox.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE));
        tmpExportButtonsHBox.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpExportButtonsHBox.setAlignment(Pos.CENTER_LEFT);
        tmpExportButtonsHBox.getChildren().addAll(tmpExportCsvButton, tmpExportPdfButton, tmpCancelExportButton);
        tmpFragmentsTab.addNodeToGridPane(tmpExportButtonsHBox, 0, 1, 1, 1);
        tmpExportPdfButton.setOnAction(event -> this.exportFile(Exporter.ExportTypes.FRAGMENT_PDF_FILE));
        tmpExportCsvButton.setOnAction(event -> this.exportFile(Exporter.ExportTypes.FRAGMENT_CSV_FILE));
        tmpCancelExportButton.setOnAction(event -> this.interruptExport());
        HBox tmpViewButtonsHBox = new HBox();
        tmpViewButtonsHBox.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE));
        tmpViewButtonsHBox.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpViewButtonsHBox.setAlignment(Pos.CENTER_RIGHT);
        tmpViewButtonsHBox.setMaxWidth(GuiDefinitions.GUI_GRIDPANE_FOR_NODE_ALIGNMENT_THIRD_COL_WIDTH);
        Button tmpOpenOverviewViewButton = GuiUtil.getButtonOfStandardSize(Message.get("MainView.showOverviewViewButton.text"));
        tmpOpenOverviewViewButton.setTooltip(new Tooltip(Message.get("MainView.showOverviewViewButton.tooltip")));
        Button tmpOpenHistogramViewButton = GuiUtil.getButtonOfStandardSize(Message.get("MainView.showHistogramViewButton.text"));
        tmpOpenHistogramViewButton.setTooltip(new Tooltip(Message.get("MainView.showHistogramViewButton.tooltip")));
        tmpViewButtonsHBox.getChildren().addAll(tmpOpenOverviewViewButton, tmpOpenHistogramViewButton);
        tmpFragmentsTab.addNodeToGridPane(tmpViewButtonsHBox, 2, 1, 1, 1);
        tmpOpenOverviewViewButton.setOnAction(event -> this.openOverviewView(OverviewViewController.DataSources.FRAGMENTS_TAB));
        tmpOpenHistogramViewButton.setOnAction(event -> this.openHistogramView());
        if(tmpList.size() == 0){
            tmpOpenOverviewViewButton.setDisable(true);
            tmpOpenHistogramViewButton.setDisable(true);
        }
        tmpFragmentsDataTableView.setOnSort((EventHandler<SortEvent<TableView>>) event -> {
            GuiUtil.sortTableViewGlobally(event, tmpPagination, tmpRowsPerPage);
        });
        tmpFragmentsDataTableView.widthProperty().addListener((observable, oldValue, newValue) -> {
            for(Object tmpObject : tmpFragmentsDataTableView.getItems()) {
                ((MoleculeDataModel) tmpObject).setStructureImageWidth(tmpFragmentsDataTableView.getStructureColumn().getWidth());
                ((FragmentDataModel) tmpObject).getFirstParentMolecule().setStructureImageWidth(tmpFragmentsDataTableView.getParentMolColumn().getWidth());
            }
        });
        tmpFragmentsDataTableView.addTableViewHeightListener(this.settingsContainer);
        tmpFragmentsDataTableView.getCopyMenuItem().setOnAction(event -> GuiUtil.copySelectedTableViewCellsToClipboard(tmpFragmentsDataTableView));
        tmpFragmentsDataTableView.getOverviewViewMenuItem().setOnAction(event -> this.openOverviewView(OverviewViewController.DataSources.PARENT_MOLECULES_SAMPLE));
        tmpFragmentsDataTableView.setOnKeyPressed(event -> {
            if (GuiDefinitions.KEY_CODE_COPY.match(event)) {
                GuiUtil.copySelectedTableViewCellsToClipboard(tmpFragmentsDataTableView);
            }
        });
        return tmpFragmentsTab;
    }
    //
    /**
     * Creates and returns a tab which visualizes the resulting fragments of each molecule that has undergone the fragmentation with the given name
     *
     * @param aFragmentationName String, unique name for the fragmentation job
     * @return Tab
     */
    private Tab createItemsTab(String aFragmentationName){
       int tmpAmount = GuiUtil.getLargestNumberOfFragmentsForGivenMoleculeListAndFragmentationName(this.moleculeDataModelList, aFragmentationName);
        ItemizationDataTableView tmpItemizationDataTableView = new ItemizationDataTableView(tmpAmount, aFragmentationName);
        tmpItemizationDataTableView.setItemsList(
                this.moleculeDataModelList.stream().filter(x -> x.hasMoleculeUndergoneSpecificFragmentation(aFragmentationName)).collect(Collectors.toList()));
        GridTabForTableView tmpItemizationTab = new GridTabForTableView(Message.get("MainTabPane.itemizationTab.title") + " - " + aFragmentationName, TabNames.ITEMIZATION.name(), tmpItemizationDataTableView);
        this.mainTabPane.getTabs().add(tmpItemizationTab);
        int tmpRowsPerPage = this.settingsContainer.getRowsPerPageSetting();
        int tmpPageCount = this.moleculeDataModelList.size() / tmpRowsPerPage;
        if (this.moleculeDataModelList.size() % tmpRowsPerPage > 0) {
            tmpPageCount++;
        }
        if(this.moleculeDataModelList.isEmpty() || this.moleculeDataModelList.size() == 0){
            tmpPageCount = 1;
        }
        Pagination tmpPagination = new Pagination(tmpPageCount, 0);
        tmpPagination.setSkin(new CustomPaginationSkin(tmpPagination));
        tmpPagination.setPageFactory((pageIndex) -> tmpItemizationDataTableView.createItemizationTableViewPage(pageIndex, aFragmentationName, this.settingsContainer));
        VBox.setVgrow(tmpPagination, Priority.ALWAYS);
        HBox.setHgrow(tmpPagination, Priority.ALWAYS);
        tmpItemizationTab.addPaginationToGridPane(tmpPagination);
        Button tmpItemizationTabExportPDfButton = GuiUtil.getButtonOfStandardSize(Message.get("MainTabPane.itemizationTab.pdfButton.txt"));
        tmpItemizationTabExportPDfButton.setTooltip(new Tooltip(Message.get("MainTabPane.itemizationTab.pdfButton.tooltip")));
        Button tmpItemizationExportCsvButton = GuiUtil.getButtonOfStandardSize(Message.get("MainTabPane.itemizationTab.csvButton.txt"));
        tmpItemizationExportCsvButton.setTooltip(new Tooltip(Message.get("MainTabPane.itemizationTab.csvButton.tooltip")));
        Button tmpCancelExportButton = GuiUtil.getButtonOfStandardSize(Message.get("MainTabPane.fragments.buttonCancelExport.txt"));
        tmpCancelExportButton.setTooltip(new Tooltip(Message.get("MainTabPane.fragments.buttonCancelExport.tooltip")));
        tmpCancelExportButton.visibleProperty().bind(this.isExportRunningProperty);
        tmpItemizationExportCsvButton.setOnAction(event -> this.exportFile(Exporter.ExportTypes.ITEM_CSV_FILE));
        tmpItemizationTabExportPDfButton.setOnAction(event -> this.exportFile(Exporter.ExportTypes.ITEM_PDF_FILE));
        tmpCancelExportButton.setOnAction(event -> this.interruptExport());
        HBox tmpExportButtonsHBox = new HBox();
        tmpExportButtonsHBox.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE));
        tmpExportButtonsHBox.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpExportButtonsHBox.setAlignment(Pos.CENTER_LEFT);
        tmpExportButtonsHBox.getChildren().addAll(tmpItemizationExportCsvButton, tmpItemizationTabExportPDfButton, tmpCancelExportButton);
        tmpItemizationTab.addNodeToGridPane(tmpExportButtonsHBox, 0, 1, 1, 1);
        HBox tmpViewButtonsHBox = new HBox();
        tmpViewButtonsHBox.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE, GuiDefinitions.GUI_INSETS_VALUE));
        tmpViewButtonsHBox.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpViewButtonsHBox.setAlignment(Pos.CENTER_RIGHT);
        tmpViewButtonsHBox.setMaxWidth(GuiDefinitions.GUI_GRIDPANE_FOR_NODE_ALIGNMENT_THIRD_COL_WIDTH);
        Button tmpOpenHistogramViewButton = GuiUtil.getButtonOfStandardSize(Message.get("MainView.showHistogramViewButton.text"));
        tmpOpenHistogramViewButton.setTooltip(new Tooltip(Message.get("MainView.showHistogramViewButton.tooltip")));
        tmpViewButtonsHBox.getChildren().add(tmpOpenHistogramViewButton);
        tmpItemizationTab.addNodeToGridPane(tmpViewButtonsHBox, 2, 1, 1, 1);
        tmpOpenHistogramViewButton.setOnAction(event -> this.openHistogramView());
        tmpItemizationDataTableView.setOnSort((EventHandler<SortEvent<TableView>>) event -> {
            GuiUtil.sortTableViewGlobally(event, tmpPagination, tmpRowsPerPage);
        });
        tmpItemizationDataTableView.widthProperty().addListener((observable, oldValue, newValue) -> {
            for(Object tmpObject : tmpItemizationDataTableView.getItems()) {
                ((MoleculeDataModel) tmpObject).setStructureImageWidth(tmpItemizationDataTableView.getMoleculeStructureColumn().getWidth());
            }
        });
        tmpItemizationDataTableView.addTableViewHeightListener(this.settingsContainer);
        tmpItemizationDataTableView.getCopyMenuItem().setOnAction(event -> GuiUtil.copySelectedTableViewCellsToClipboard(tmpItemizationDataTableView));
        tmpItemizationDataTableView.getOverviewViewMenuItem().setOnAction(event -> this.openOverviewView(OverviewViewController.DataSources.ITEM_WITH_FRAGMENTS_SAMPLE));
        tmpItemizationDataTableView.setOnKeyPressed(event -> {
            if (GuiDefinitions.KEY_CODE_COPY.match(event)) {
                GuiUtil.copySelectedTableViewCellsToClipboard(tmpItemizationDataTableView);
            }
        });
        if(this.mapOfFragmentDataModelLists.get(aFragmentationName).size() == 0 ){
            tmpOpenHistogramViewButton.setDisable(true);
        }
        return tmpItemizationTab;
    }
    //
    /**
     * Clears the gui and all collections
     */
    private void clearGuiAndCollections() {
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
    private List<MoleculeDataModel> getItemsListOfSelectedFragmenterByTabId(TabNames aTabName) {
        return ((IDataTableView) ((GridTabForTableView) (this.mainTabPane.getTabs().stream().filter(tab ->
                ((GridTabForTableView) this.mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle().equals(((GridTabForTableView) tab).getFragmentationNameOutOfTitle()) && tab.getId().equals(aTabName.name())
        ).findFirst().get())).getTableView()).getItemsList();
    }
    //

    /**
     * Updates StatusBar
     *
     * @param aThread  Thread which was started or end
     * @param aMessage String message to display in StatusBar
     */
    private void updateStatusBar(Thread aThread, String aMessage) {
        if (!this.threadList.contains(aThread)) {
            this.threadList.add(aThread);
            this.mainView.getStatusBar().getStatusLabel().setText(aMessage);
            this.mainView.getStatusBar().getStatusLabel().setVisible(true);
            this.mainView.getStatusBar().getProgressBar().setVisible(true);
            return;
        }
        if (this.threadList.contains(aThread)) {
            this.threadList.remove(aThread);
            if (this.threadList.isEmpty()) {
                this.mainView.getStatusBar().getProgressBar().setVisible(false);
                this.mainView.getStatusBar().getStatusLabel().setText(aMessage);
                return;
            }
            this.mainView.getStatusBar().getStatusLabel().setText(
                    this.getStatusMessageByThreadType(
                            Objects.requireNonNull(ThreadType.get(
                                    this.threadList.get(this.threadList.size() - 1).getName()
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
    private String getStatusMessageByThreadType(ThreadType aThreadType) {
        switch (aThreadType) {
            case FRAGMENTATION_THREAD:
                return Message.get("Status.running");
            case IMPORT_THREAD:
                return Message.get("Status.importing");
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
     * Enum for different thread types, set as thread name
     */
    public enum ThreadType {
        /**
         * enum value for fragmentation thread
         */
        FRAGMENTATION_THREAD("Fragmentation_Thread"),
        /**
         * enum value for import thread
         */
        IMPORT_THREAD("Import_Thread"),
        /**
         * enum value for export thread
         */
        EXPORT_THREAD("Export_Thread");

        private String threadName;

        ThreadType(String aThreadName) {
            this.threadName = aThreadName;
        }

        /**
         * Returns the name of this thread type
         *
         * @return String
         */
        public String getThreadName() {
            return this.threadName;
        }

        /**
         * Reverse lookup
         * Returns ThreadType by given thread name;
         * returns null if thread name does not correspond to any ThreadType
         *
         * @param aThreadName String
         * @return ThreadType
         */
        public static ThreadType get(String aThreadName) {
            for (ThreadType aType : values()) {
                if (aType.threadName.equals(aThreadName)) {
                    return aType;
                }
            }
            return null;
        }
    }
    //</editor-fold>
}

