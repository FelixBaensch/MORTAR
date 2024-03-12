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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MainViewController
 * controls  {@link MainView}.
 *
 * @author Felix Baensch, Jonas Schaub
 * @version 1.0.0.0
 */
public class MainViewController {
    //<editor-fold desc="private and private final class variables" defaultstate="collapsed">
    /**
     * Primary Stage
     */
    private final Stage primaryStage;
    /**
     * MainView
     */
    private final MainView mainView;
    /**
     * Scene
     */
    private final Scene scene;
    /**
     * TabPane which holds the different tabs
     */
    private final TabPane mainTabPane;
    /**
     * ObservableList to hold MoleculeDataModels for visualisation in MoleculesDataTableView
     */
    private final ObservableList<MoleculeDataModel> moleculeDataModelList;
    /**
     * MoleculesDataTableView to show imported molecules
     */
    private MoleculesDataTableView moleculesDataTableView;
    /**
     * SettingsContainer
     */
    private final SettingsContainer settingsContainer;
    /**
     * FragmentationService
     */
    private final FragmentationService fragmentationService;
    /**
     * ViewToolsManager
     */
    private final ViewToolsManager viewToolsManager;
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
    private final HashMap<String, ObservableList<FragmentDataModel>> mapOfFragmentDataModelLists;
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
    private final BooleanProperty isImportRunningProperty;
    /**
     * BooleanProperty whether export is running
     */
    private final BooleanProperty isExportRunningProperty;
    /**
     * Thread safe list to hold running threads to update StatusBar
     */
    private final CopyOnWriteArrayList<Thread> threadList;
    /**
     * Configuration class to read resource file paths from.
     */
    private final IConfiguration configuration;
    //</editor-fold>
    //
    //<editor-fold desc="private static final variables" defaultstate="collapsed">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(MainViewController.class.getName());
    //</editor-fold>
    //
    /**
     * Constructor. Starts the application.
     *
     * @param aStage    Stage
     * @param aMainView MainView
     * @param anAppDir  String path to app dir
     * @param aConfiguration configuration class reading from properties file
     * @throws IllegalArgumentException given application directory is either no directory or does not exist
     * @throws NullPointerException if one param is null
     */
    public MainViewController(Stage aStage, MainView aMainView, String anAppDir, IConfiguration aConfiguration)
            throws IllegalArgumentException, NullPointerException {
        //<editor-fold desc="checks" defaultstate="collapsed">
        Objects.requireNonNull(aStage, "aStage (instance of Stage) is null");
        Objects.requireNonNull(aMainView, "aMainView (instance of MainView) is null");
        Objects.requireNonNull(aMainView, "anAppDir (instance of String) is null");
        Objects.requireNonNull(aConfiguration, "aConfiguration (instance of IConfiguration) is null");
        File tmpAppDirFile = new File(anAppDir);
        if (!tmpAppDirFile.isDirectory() || !tmpAppDirFile.exists()) {
            throw new IllegalArgumentException("The given application directory is either no directory or does not exist");
        }
        //</editor-fold>
        this.configuration = aConfiguration;
        this.moleculeDataModelList = FXCollections.observableArrayList(param -> new Observable[]{param.selectionProperty()});
        this.primaryStage = aStage;
        this.mainView = aMainView;
        this.settingsContainer = new SettingsContainer();
        this.settingsContainer.reloadGlobalSettings();
        this.fragmentationService = new FragmentationService(this.settingsContainer);
        this.fragmentationService.reloadFragmenterSettings();
        this.fragmentationService.reloadActiveFragmenterAndPipeline();
        this.viewToolsManager = new ViewToolsManager(this.configuration);
        this.viewToolsManager.reloadViewToolsSettings();
        //<editor-fold desc="show MainView inside primaryStage" defaultstate="collapsed">
        this.mainTabPane = new TabPane();
        this.mainView.getMainCenterPane().getChildren().add(this.mainTabPane);
        GuiUtil.guiBindControlSizeToParentPane(this.mainView.getMainCenterPane(), this.mainTabPane);
        this.scene = new Scene(this.mainView, GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE, GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        String tmpStyleSheetURL = this.getClass().getClassLoader().getResource(
                this.configuration.getProperty("mortar.styleFolder")
                        + this.configuration.getProperty("mortar.stylesheet.name")).toExternalForm();
        this.scene.getStylesheets().add(tmpStyleSheetURL);
        this.primaryStage.setTitle(Message.get("Title.text"));
        this.primaryStage.setScene(this.scene);
        this.primaryStage.show();
        this.primaryStage.setMinHeight(GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.primaryStage.setMinWidth(GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE);
        String tmpIconURL = this.getClass().getClassLoader().getResource(
                this.configuration.getProperty("mortar.imagesFolder") + this.configuration.getProperty("mortar.logo.icon.name")).toExternalForm();
        this.primaryStage.getIcons().add(new Image(tmpIconURL));
        //</editor-fold>
        this.isImportRunningProperty = new SimpleBooleanProperty(false);
        this.isExportRunningProperty = new SimpleBooleanProperty(false);
        this.mapOfFragmentDataModelLists = new HashMap<>(CollectionUtil.calculateInitialHashCollectionCapacity(5));
        this.threadList = new CopyOnWriteArrayList<>();
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
        this.mainView.getMainMenuBar().getAboutViewMenuItem().setOnAction(actionEvent -> {
            new AboutViewController(this.primaryStage, this.configuration);
        });
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
                this.mainView.getMainMenuBar().getHistogramViewerMenuItem().setDisable(newValue.getId().equals(TabNames.MOLECULES.toString()));
                this.mainView.getMainMenuBar().getOverviewViewMenuItem().setDisable(newValue.getId().equals(TabNames.ITEMIZATION.toString()));
            });
        });
    }
    //
    /**
     * Closes application.
     *
     * @param aStatus the status to use for calling System.exit(); a nonzero status code indicates abnormal termination
     */
    private void closeApplication(int aStatus) {
        if (!moleculeDataModelList.isEmpty() && (!this.isFragmentationStopAndDataLossConfirmed())) {
            return;
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
     * Closes the application via closeApplication method when close window event was fired.
     *
     * @param anEvent WindowEvent
     */
    private void closeWindowEvent(WindowEvent anEvent) {
        this.closeApplication(0);
        anEvent.consume();
    }
    //
    /**
     * Loads molecule file and opens molecules tab.
     *
     * @param aParentStage Stage where to open the file chooser dialog
     */
    private void importMoleculeFile(Stage aParentStage) {
        if (!this.moleculeDataModelList.isEmpty()) {
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
                    tmpAtomContainerSet = this.importTask.get();
                } catch (InterruptedException | ExecutionException anException) {
                    MainViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                            Message.get("Importer.FileImportExceptionAlert.Header"),
                            Message.get("Importer.FileImportExceptionAlert.Text") + "\n" + LogUtil.getLogFileDirectoryPath(),
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
                MainViewController.LOGGER.log(Level.INFO, String.format("Successfully imported %d molecules from file: %s; " +
                        "%d molecules could not be parsed into the internal data model (SMILES code generation failed). " +
                        "See above how many molecules could not be read from the input file at all or produced exceptions while preprocessing.",
                        tmpAtomContainerSet.getAtomContainerCount(), tmpImporter.getFileName(), tmpExceptionCount));
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
     * Exports the given type of file.
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
            case Exporter.ExportTypes.FRAGMENT_CSV_FILE, Exporter.ExportTypes.PDB_FILE, Exporter.ExportTypes.FRAGMENT_PDF_FILE, Exporter.ExportTypes.SINGLE_SD_FILE, SD_FILE:
                if (this.getItemsListOfSelectedFragmentationByTabId(TabNames.FRAGMENTS) == null ||
                        this.getItemsListOfSelectedFragmentationByTabId(TabNames.FRAGMENTS).isEmpty() ||
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
            case Exporter.ExportTypes.ITEM_CSV_FILE, Exporter.ExportTypes.ITEM_PDF_FILE:
                if (this.getItemsListOfSelectedFragmentationByTabId(TabNames.ITEMIZATION) == null ||
                        this.getItemsListOfSelectedFragmentationByTabId(TabNames.ITEMIZATION).isEmpty() ||
                        this.moleculeDataModelList == null || this.moleculeDataModelList.isEmpty() ||
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
        if (anExportType.equals(Exporter.ExportTypes.PDB_FILE) || anExportType.equals(Exporter.ExportTypes.SINGLE_SD_FILE)
                && (!ChemUtil.checkMoleculeListForCoordinates(getItemsListOfSelectedFragmentationByTabId(TabNames.FRAGMENTS)))) {
            ButtonType tmpConfirmationResult = GuiUtil.guiConfirmationAlert(
                    Message.get("Exporter.FragmentsTab.ConfirmationAlert.No3dInformationAvailable.title"),
                    Message.get("Exporter.FragmentsTab.ConfirmationAlert.No3dInformationAvailable.header"),
                    Message.get("Exporter.FragmentsTab.ConfirmationAlert.No3dInformationAvailable.text")
            );
            tmpGenerate2dAtomCoordinates = tmpConfirmationResult == ButtonType.OK;
        }
        boolean tmpGenerate2dAtomCoordinatesFinal = tmpGenerate2dAtomCoordinates;
        this.exportTask = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                switch (anExportType) {
                    case Exporter.ExportTypes.FRAGMENT_CSV_FILE:
                        return tmpExporter.exportCsvFile(
                                getItemsListOfSelectedFragmentationByTabId(TabNames.FRAGMENTS),
                                ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle(),
                                settingsContainer.getCsvExportSeparatorSetting(),
                                TabNames.FRAGMENTS
                        );
                    case Exporter.ExportTypes.PDB_FILE:
                        return tmpExporter.exportFragmentsAsChemicalFile(
                                getItemsListOfSelectedFragmentationByTabId(TabNames.FRAGMENTS),
                                ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle(),
                                ChemFileTypes.PDB,
                                tmpGenerate2dAtomCoordinatesFinal
                        );
                    case Exporter.ExportTypes.FRAGMENT_PDF_FILE:
                        return tmpExporter.exportPdfFile(
                                getItemsListOfSelectedFragmentationByTabId(TabNames.FRAGMENTS),
                                moleculeDataModelList,
                                ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle(),
                                TabNames.FRAGMENTS
                        );
                    case Exporter.ExportTypes.SINGLE_SD_FILE:
                        return tmpExporter.exportFragmentsAsChemicalFile(
                                getItemsListOfSelectedFragmentationByTabId(TabNames.FRAGMENTS),
                                ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle(),
                                ChemFileTypes.SDF,
                                tmpGenerate2dAtomCoordinatesFinal,
                                true
                        );
                    case Exporter.ExportTypes.SD_FILE:
                        return tmpExporter.exportFragmentsAsChemicalFile(
                                getItemsListOfSelectedFragmentationByTabId(TabNames.FRAGMENTS),
                                ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle(),
                                ChemFileTypes.SDF,
                                false
                        );
                    case Exporter.ExportTypes.ITEM_CSV_FILE:
                        return tmpExporter.exportCsvFile(
                                moleculeDataModelList,
                                ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle(),
                                settingsContainer.getCsvExportSeparatorSetting(),
                                TabNames.ITEMIZATION
                        );
                    case Exporter.ExportTypes.ITEM_PDF_FILE:
                        return tmpExporter.exportPdfFile(
                                getItemsListOfSelectedFragmentationByTabId(TabNames.ITEMIZATION),
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
            if (!tmpFailedExportFragments.isEmpty()) {
                StringBuilder tmpStringBuilder = new StringBuilder();
                for (String tmpFragmentName : tmpFailedExportFragments) {
                    tmpStringBuilder.append(tmpFragmentName).append("\n");
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
     * Opens settings view for fragmentation settings.
     */
    private void openFragmentationSettingsView() {
        new FragmentationSettingsViewController(this.primaryStage,
                this.fragmentationService.getFragmenters(),
                this.fragmentationService.getSelectedFragmenter().getFragmentationAlgorithmName(),
                this.configuration);
    }
    //
    /**
     * Opens PipelineSettingsView.
     */
    private void openPipelineSettingsView() {
        PipelineSettingsViewController tmpPipelineSettingsViewController =
                new PipelineSettingsViewController(this.primaryStage, this.fragmentationService, !this.moleculeDataModelList.isEmpty(), this.isFragmentationRunning, this.configuration);
        if (tmpPipelineSettingsViewController.isFragmentationStarted()) {
            this.startFragmentation(tmpPipelineSettingsViewController.isFragmentationStarted());
        }
    }
    //
    /**
     * Opens HistogramView.
     */
    private void openHistogramView()  {
        List<MoleculeDataModel> tmpMoleculesList = this.getItemsListOfSelectedFragmentationByTabId(TabNames.FRAGMENTS);
        List<FragmentDataModel> tmpFragmentsList = new ArrayList<>(tmpMoleculesList.size());
        for (MoleculeDataModel tmpMolecule : tmpMoleculesList) {
            tmpFragmentsList.add((FragmentDataModel) tmpMolecule);
        }
        this.viewToolsManager.openHistogramView(this.primaryStage, tmpFragmentsList);
    }
    //
    /**
     * Adds CheckMenuItems for fragmentation algorithms to MainMenuBar.
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
     * Opens settings view for global settings.
     */
    private void openGlobalSettingsView() {
        SettingsViewController tmpSettingsViewController = new SettingsViewController(this.primaryStage, this.settingsContainer, this.configuration);
        Platform.runLater(() -> {
            if (tmpSettingsViewController.hasRowsPerPageChanged()) {
                for (Tab tmpTab : this.mainTabPane.getTabs()) {
                    // type of generic not given because it does not matter here, only the size of the items list
                    TableView tmpTableView = ((GridTabForTableView) tmpTab).getTableView();
                    int tmpListSize = ((IDataTableView) tmpTableView).getItemsList().size();
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
                    GuiUtil.setImageStructureHeight(((GridTabForTableView) tmpTab).getTableView(), ((GridTabForTableView) tmpTab).getTableView().getHeight(), this.settingsContainer.getRowsPerPageSetting());
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
     * Opens OverviewView.
     *
     * @param aDataSource Source of the data to be shown in the overview view
     */
    private void openOverviewView(OverviewViewController.DataSources aDataSource) {
        try {
            switch (aDataSource) {
                case OverviewViewController.DataSources.MOLECULES_TAB -> {
                    if (!(this.mainTabPane.getSelectionModel().getSelectedItem().getId().equals(TabNames.MOLECULES.toString())))
                        //should not happen
                        throw new IllegalStateException();
                    this.viewToolsManager.openOverviewView(
                            this.primaryStage,
                            aDataSource,
                            ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getTitle(),
                            getItemsListOfSelectedFragmentationByTabId(TabNames.MOLECULES)
                    );
                }
                case OverviewViewController.DataSources.FRAGMENTS_TAB -> {
                    if (!(this.mainTabPane.getSelectionModel().getSelectedItem().getId().equals(TabNames.FRAGMENTS.toString())))
                        //should not happen
                        throw new IllegalStateException();
                    this.viewToolsManager.openOverviewView(
                            this.primaryStage,
                            aDataSource,
                            ((GridTabForTableView) mainTabPane.getSelectionModel().getSelectedItem()).getTitle(),
                            this.getItemsListOfSelectedFragmentationByTabId(TabNames.FRAGMENTS)
                    );
                }
                case OverviewViewController.DataSources.PARENT_MOLECULES_SAMPLE -> {
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
                    int tmpSelectedRowIndex = ((TableView<?>) tmpSelectedTab.getTableView()).getSelectionModel().getSelectedCells().getFirst().getRow();
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
                case OverviewViewController.DataSources.ITEM_WITH_FRAGMENTS_SAMPLE -> {
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
                    int tmpSelectedRowIndex = ((TableView<?>) tmpSelectedTab.getTableView()).getSelectionModel().getSelectedCells().getFirst().getRow();
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
            // unnecessary to provide generic type
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
     * Opens molecules tab.
     */
    private void openMoleculesTab() {
        this.moleculesDataTableView = new MoleculesDataTableView(this.configuration);
        this.moleculesDataTableView.setItemsList(this.moleculeDataModelList);
        GridTabForTableView tmpMoleculesTab = new GridTabForTableView(Message.get("MainTabPane.moleculesTab.title"), TabNames.MOLECULES.name(), this.moleculesDataTableView);
        this.mainTabPane.getTabs().add(tmpMoleculesTab);
        Pagination tmpPagination = this.createPaginationWithSuitablePageCount(this.moleculeDataModelList.size());
        tmpPagination.setPageFactory((pageIndex) -> this.moleculesDataTableView.createMoleculeTableViewPage(pageIndex, this.settingsContainer));
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
        this.cancelFragmentationButton = GuiUtil.getButtonOfStandardSize(Message.get("MainTabPane.moleculesTab.cancelFragmentationButton.text"));
        this.cancelFragmentationButton.setTooltip(new Tooltip(Message.get("MainTabPane.moleculesTab.cancelFragmentationButton.tooltip")));
        this.cancelFragmentationButton.setVisible(false);
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
        int tmpRowsPerPage = this.settingsContainer.getRowsPerPageSetting();
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
     * Creates a new JavaFx pagination control that is configured with a suitable page count for the given number
     * of molecules/fragments taking into account the rows per page setting. Also sets the MORTAR custom pagination skin
     * as skin of the new pagination instance and configures its growth behavior. The page factory is *NOT* set.
     *
     * @param aListSize number of molecules/fragments to display
     * @return configured pagination control instance
     */
    private Pagination createPaginationWithSuitablePageCount(int aListSize) {
        int tmpRowsPerPage = this.settingsContainer.getRowsPerPageSetting();
        int tmpPageCount = aListSize / tmpRowsPerPage;
        if (aListSize % tmpRowsPerPage > 0) {
            tmpPageCount++;
        }
        if (aListSize == 0) {
            tmpPageCount = 1;
        }
        Pagination tmpPagination = new Pagination(tmpPageCount, 0);
        tmpPagination.setSkin(new CustomPaginationSkin(tmpPagination));
        VBox.setVgrow(tmpPagination, Priority.ALWAYS);
        HBox.setHgrow(tmpPagination, Priority.ALWAYS);
        return tmpPagination;
    }
    //
    /**
     * Cancels import task and interrupts the corresponding thread.
     */
    private void interruptImport() {
        this.importTask.cancel();
        this.importerThread.interrupt();
    }
    //
    /**
     * Cancels export task and interrupts the corresponding thread.
     */
    private void interruptExport() {
        this.exportTask.cancel();
        this.exporterThread.interrupt();
    }
    //
    /**
     * Gets called by the cancel fragmentation button.
     */
    private void interruptFragmentation() {
        //cancel() of the task was overridden to shut down the executor service in FragmentationService
        this.parallelFragmentationMainTask.cancel(true);
        this.cancelFragmentationButton.setVisible(false);
        this.fragmentationButton.setDisable(false);
    }
    //
    /**
     * Starts fragmentation for only one algorithm.
     */
    private void startFragmentation() {
        this.startFragmentation(false);
    }
    //
    /**
     * Starts fragmentation task and opens fragment and itemization tabs.
     */
    private void startFragmentation(boolean isPipelining) {
        long tmpStartTime = System.nanoTime();
        MainViewController.LOGGER.info("Start of method startFragmentation");
        List<MoleculeDataModel> tmpSelectedMolecules = this.moleculeDataModelList.stream().filter(mol -> mol.isSelected()).toList();
        int tmpNumberOfCores = this.settingsContainer.getNumberOfTasksForFragmentationSetting();
        try {
            this.fragmentationButton.setDisable(true);
            this.cancelFragmentationButton.setVisible(true);
            this.parallelFragmentationMainTask = new Task<>() {
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

                //
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
     * Adds a tab for fragments and a tab for items (results of fragmentation).
     *
     * @param aFragmentationName name of the fragmentation process
     */
    private void addFragmentationResultTabs(String aFragmentationName) {
        //fragments tab
        Tab tmpFragmentsTab = this.createFragmentsTab(aFragmentationName);
        //itemization tab
        Tab tmpItemsTab = this.createItemsTab(aFragmentationName);
        this.mainTabPane.getSelectionModel().select(tmpFragmentsTab);
    }
    //
    /**
     * Creates and returns a tab, which visualizes the resulting fragments of the fragmentation with given name.
     *
     * @param aFragmentationName String, unique name for fragmentation job
     * @return Tab
     */
    private Tab createFragmentsTab(String aFragmentationName){
        FragmentsDataTableView tmpFragmentsDataTableView = new FragmentsDataTableView(this.configuration);
        GridTabForTableView tmpFragmentsTab = new GridTabForTableView(Message.get("MainTabPane.fragmentsTab.title") + " - " + aFragmentationName, TabNames.FRAGMENTS.name(), tmpFragmentsDataTableView);
        this.mainTabPane.getTabs().add(tmpFragmentsTab);
        ObservableList<MoleculeDataModel> tmpList = FXCollections.observableArrayList(this.mapOfFragmentDataModelLists.get(aFragmentationName));
        for (MoleculeDataModel tmpMoleculeDataModel : tmpList) {
            tmpMoleculeDataModel.setStructureImageWidth(tmpFragmentsDataTableView.getStructureColumn().getWidth());
        }
        tmpFragmentsDataTableView.setItemsList(tmpList);
        Pagination tmpPagination = this.createPaginationWithSuitablePageCount(tmpList.size());
        tmpPagination.setPageFactory((pageIndex) -> tmpFragmentsDataTableView.createFragmentsTableViewPage(pageIndex, this.settingsContainer));
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
        if (tmpList.isEmpty()) {
            tmpOpenOverviewViewButton.setDisable(true);
            tmpOpenHistogramViewButton.setDisable(true);
        }
        int tmpRowsPerPage = this.settingsContainer.getRowsPerPageSetting();
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
     * Creates and returns a tab which visualizes the resulting fragments of each molecule that has undergone the
     * fragmentation with the given name.
     *
     * @param aFragmentationName String, unique name for the fragmentation job
     * @return Tab
     */
    private Tab createItemsTab(String aFragmentationName){
        ItemizationDataTableView tmpItemizationDataTableView = new ItemizationDataTableView(aFragmentationName, this.configuration);
        tmpItemizationDataTableView.setItemsList(
                this.moleculeDataModelList.stream().filter(x -> x.hasMoleculeUndergoneSpecificFragmentation(aFragmentationName)).toList());
        GridTabForTableView tmpItemizationTab = new GridTabForTableView(Message.get("MainTabPane.itemizationTab.title") + " - " + aFragmentationName, TabNames.ITEMIZATION.name(), tmpItemizationDataTableView);
        this.mainTabPane.getTabs().add(tmpItemizationTab);
        Pagination tmpPagination = this.createPaginationWithSuitablePageCount(this.moleculeDataModelList.size());
        tmpPagination.setPageFactory((pageIndex) -> tmpItemizationDataTableView.createItemizationTableViewPage(pageIndex, aFragmentationName, this.settingsContainer));
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
        int tmpRowsPerPage = this.settingsContainer.getRowsPerPageSetting();
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
        if (this.mapOfFragmentDataModelLists.get(aFragmentationName).isEmpty()) {
            tmpOpenHistogramViewButton.setDisable(true);
        }
        return tmpItemizationTab;
    }
    //
    /**
     * Clears the gui and all collections.
     */
    private void clearGuiAndCollections() {
        this.moleculeDataModelList.clear();
        this.mapOfFragmentDataModelLists.clear();
        this.moleculesDataTableView = null;
        this.mainTabPane.getTabs().clear();
    }
    //
    /**
     * Returns the items list of the table view of the selected tab.
     *
     * @param aTabName Enum which specifies which kind of tab
     * @return List {@literal <}MoleculeDataModel{@literal >}
     */
    private List<MoleculeDataModel> getItemsListOfSelectedFragmentationByTabId(TabNames aTabName) {
        GridTabForTableView tmpSelectedTab =  (GridTabForTableView) (this.mainTabPane.getTabs().stream().filter(tab ->
                ((GridTabForTableView) this.mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle()
                        .equals(((GridTabForTableView) tab).getFragmentationNameOutOfTitle()) && tab.getId().equals(aTabName.name())
        ).findFirst().orElse(null));
        if (tmpSelectedTab == null) {
            return new ArrayList<>();
        } else {
            return ((IDataTableView) tmpSelectedTab.getTableView()).getItemsList();
        }
    }
    //
    /**
     * Updates StatusBar.
     *
     * @param aThread  Thread which was started or ended
     * @param aMessage String message to display in StatusBar
     */
    private void updateStatusBar(Thread aThread, String aMessage) {
        if (!this.threadList.contains(aThread)) {
            this.threadList.add(aThread);
            this.mainView.getStatusBar().getStatusLabel().setText(aMessage);
            this.mainView.getStatusBar().getStatusLabel().setVisible(true);
            this.mainView.getStatusBar().getProgressBar().setVisible(true);
            //return;
        } else {
            this.threadList.remove(aThread);
            if (this.threadList.isEmpty()) {
                this.mainView.getStatusBar().getProgressBar().setVisible(false);
                this.mainView.getStatusBar().getStatusLabel().setText(aMessage);
                return;
            }
            this.mainView.getStatusBar().getStatusLabel().setText(
                    this.getStatusMessageByThreadType(
                            Objects.requireNonNull(ThreadType.get(
                                    this.threadList.getLast().getName()
                            ))
                    )
            );
        }
    }
    //
    /**
     * Returns status message as string by given ThreadType.
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
     * Enum for different thread types, set as thread name.
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

        private final String threadName;

        ThreadType(String aThreadName) {
            this.threadName = aThreadName;
        }
        //
        /**
         * Returns the name of this thread type
         *
         * @return String
         */
        public String getThreadName() {
            return this.threadName;
        }
        //
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
