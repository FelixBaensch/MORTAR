/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2026  Felix Baensch, Jonas Schaub (felix.j.baensch@gmail.com, jonas.schaub@uni-jena.de)
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
import de.unijena.cheminf.mortar.gui.views.FragmentsTabView;
import de.unijena.cheminf.mortar.gui.views.IDataTableView;
import de.unijena.cheminf.mortar.gui.views.ItemizationDataTableView;
import de.unijena.cheminf.mortar.gui.views.ItemizationTabView;
import de.unijena.cheminf.mortar.gui.views.MainView;
import de.unijena.cheminf.mortar.gui.views.MoleculesDataTableView;
import de.unijena.cheminf.mortar.gui.views.MoleculesTabView;
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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Pagination;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SortEvent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * MainViewController controls  {@link MainView}.
 *
 * @author Felix Baensch
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class MainViewController {
    //<editor-fold desc="private and private final class variables" defaultstate="collapsed">
    /**
     * Primary Stage.
     */
    private final Stage primaryStage;
    /**
     * MainView.
     */
    private final MainView mainView;
    /**
     * Scene.
     */
    private final Scene scene;
    /**
     * TabPane which holds the different tabs.
     */
    private final TabPane mainTabPane;
    /**
     * ObservableList to hold MoleculeDataModels for visualisation in MoleculesDataTableView.
     */
    private final ObservableList<MoleculeDataModel> moleculeDataModelList;
    /**
     * MoleculesDataTableView to show imported molecules.
     */
    private MoleculesDataTableView moleculesDataTableView;
    /**
     * SettingsContainer.
     */
    private final SettingsContainer settingsContainer;
    /**
     * FragmentationService.
     */
    private final FragmentationService fragmentationService;
    /**
     * ViewToolsManager.
     */
    private final ViewToolsManager viewToolsManager;
    /**
     * Button to start single algorithm fragmentation.
     */
    private Button fragmentationButton;
    /**
     * Button to cancel running fragmentation.
     */
    private Button cancelFragmentationButton;
    /**
     * HashMap to hold Lists of FragmentDataModels for each fragmentation.
     */
    private final HashMap<String, ObservableList<FragmentDataModel>> mapOfFragmentDataModelLists;
    /**
     * Boolean value whether fragmentation is running.
     */
    private boolean isFragmentationRunning;
    /**
     * Task for parallel fragmentation.
     */
    private Task<Void> parallelFragmentationMainTask;
    /**
     * Thread for task for parallel fragmentation.
     */
    private Thread fragmentationThread;
    /**
     * Thread for molecule imports, so GUI thread is always responsive.
     */
    private Thread importerThread;
    /**
     * Task for molecule file import.
     */
    private Task<List<MoleculeDataModel>> importTask;
    /**
     * Storing the name of the last imported file.
     */
    private String importedFileName;
    /**
     * Thread for molecule exports, so GUI thread is always responsive.
     */
    private Thread exporterThread;
    /**
     * Task for molecule file export.
     */
    private Task<List<String>> exportTask;
    /**
     * BooleanProperty whether import is running.
     */
    private final BooleanProperty isImportRunningProperty;
    /**
     * BooleanProperty whether export is running.
     */
    public final BooleanProperty isExportRunningProperty;
    /**
     * Thread safe list to hold running threads to update StatusBar.
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
        this.fragmentationService = new FragmentationService();
        this.fragmentationService.reloadFragmenterSettings();
        this.fragmentationService.reloadActiveFragmenterAndPipeline();
        this.viewToolsManager = new ViewToolsManager(this.configuration, this.settingsContainer);
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
                anEvent -> this.chooseAndImportMoleculeFile(this.primaryStage)
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
                anEvent -> this.exportFile(Exporter.ExportTypes.FRAGMENT_PDB_FILE));
        //fragments export to PDF
        this.mainView.getMainMenuBar().getFragmentsExportToPDFMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.exportFile(Exporter.ExportTypes.FRAGMENT_PDF_FILE));
        //fragments export to single SDF
        this.mainView.getMainMenuBar().getFragmentsExportToSingleSDFMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.exportFile(Exporter.ExportTypes.FRAGMENT_SINGLE_SD_FILE));
        //fragments export to separate SDFs
        this.mainView.getMainMenuBar().getFragmentsExportToSeparateSDFsMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.exportFile(Exporter.ExportTypes.FRAGMENT_MULTIPLE_SD_FILES));
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
        this.mainView.getMainMenuBar().getAboutViewMenuItem().setOnAction(actionEvent -> new AboutViewController(this.primaryStage, this.configuration));
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
        this.mainTabPane.getSelectionModel().selectedItemProperty().addListener(
                (tmpObservable, tmpOldTab, tmpNewTab) -> {
                    if (tmpNewTab instanceof GridTabForTableView tmpGridTabView) {
                        switch (tmpGridTabView) {
                            case MoleculesTabView tmpMolTab -> {
                                this.mainView.getMainMenuBar().getFragmentsExportMenu().setDisable(true);
                                this.mainView.getMainMenuBar().getItemsExportMenu().setDisable(true);
                            }
                            case FragmentsTabView tmpFragTab -> {
                                this.mainView.getMainMenuBar().getFragmentsExportMenu().setDisable(false);
                                this.mainView.getMainMenuBar().getItemsExportMenu().setDisable(true);
                            }
                            case ItemizationTabView tmpItemsTab -> {
                                this.mainView.getMainMenuBar().getFragmentsExportMenu().setDisable(true);
                                this.mainView.getMainMenuBar().getItemsExportMenu().setDisable(false);
                            }
                            default -> throw new IllegalStateException("Unexpected value: " + tmpGridTabView);
                        }
                    }
                }
        );
        this.mainTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(() -> {
            if (newValue == null) {
                return;
            }
            this.mainView.getMainMenuBar().getHistogramViewerMenuItem().setDisable(newValue.getId().equals(TabNames.MOLECULES.toString()));
            this.mainView.getMainMenuBar().getOverviewViewMenuItem().setDisable(newValue.getId().equals(TabNames.ITEMIZATION.toString()));
        }));
        this.mainView.getMainCenterPane().setOnDragOver(aDragEvent -> {
            if (aDragEvent.getGestureSource() != this.mainView.getMainCenterPane() && aDragEvent.getDragboard().hasFiles()){
                if (
                  aDragEvent.getDragboard().getFiles().size() > 1 ||
                  !Importer.VALID_IMPORT_FILE_EXTENSIONS_SET.contains(FileUtil.getFileExtension(aDragEvent.getDragboard().getFiles().getFirst().getName()))
                ) {
                    aDragEvent.consume();
                    return;
                }
                aDragEvent.acceptTransferModes(TransferMode.COPY);
            }
            aDragEvent.consume();
        });
        this.mainView.getMainCenterPane().setOnDragDropped(aDragEvent -> {
            Dragboard tmpDragboard = aDragEvent.getDragboard();
            boolean tmpSucceeded = false;
            if (tmpDragboard.hasFiles()) {
                this.importMoleculeFile(tmpDragboard.getFiles().getFirst());
                tmpSucceeded = true;
            }
            aDragEvent.setDropCompleted(tmpSucceeded);
            aDragEvent.consume();
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
        if (this.isFragmentationRunning) {
            ButtonType tmpConfirmationResult = GuiUtil.guiConfirmationAlert(
                    Message.get("MainViewController.Warning.FragmentationRunning.Title"),
                    Message.get("MainViewController.Warning.FragmentationRunning.Header"),
                    Message.get("MainViewController.Warning.FragmentationRunning.Content"));
            return tmpConfirmationResult == ButtonType.OK;

        } else if (this.settingsContainer.getShowDataWillBeLostWarningSetting()) {
            GuiUtil.CheckboxAndButtonResult tmpCheckboxAndConfirmationResult = GuiUtil.guiConfirmationAlertWithCheckbox(
                    Message.get("MainViewController.Warning.DataLoss.Title"),
                    Message.get("MainViewController.Warning.DataLoss.Header"),
                    Message.get("MainViewController.Warning.DataLoss.Content"),
                    Message.get("SettingsContainer.showDataWillBeLostWarning.checkbox.text")
            );
            if (tmpCheckboxAndConfirmationResult.buttonType() != ButtonType.OK) {
                return false;
            }
            if (tmpCheckboxAndConfirmationResult.checkboxChecked()) {
                // NOTE: This setting can only be reactivated via the global settings.
                this.settingsContainer.setShowDataWillBeLostWarningSetting(false);
            }
            return true;
        } else {
            return true;
        }
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
     * Opens a file choose, loads the chosen file and opens molecules tab
     *
     * @param aParentStage Stage where to open the file chooser dialog
     */
    private void chooseAndImportMoleculeFile(Stage aParentStage) {
        Importer tmpImporter = new Importer(this.settingsContainer);
        File tmpFile = tmpImporter.openFile(aParentStage);
        if (tmpFile == null)
            return;
        this.importMoleculeFile(tmpFile, tmpImporter);
    }
    //
    /**
     * Loads molecule file and opens molecules tab.
     * Convenient method to avoid using a null parameter for the importer.
     *
     * @param aFile File that contains molecular data
     */
    private void importMoleculeFile(File aFile) {
        this.importMoleculeFile(aFile, new Importer(this.settingsContainer));
    }
    //
    /**
     * Loads molecule file and opens molecules tab.
     *
     * @param aFile File that contains molecular data
     */
    private void importMoleculeFile(File aFile, Importer anImporter) {
        if (Objects.isNull(aFile)) {
            return;
        }
        if (!this.moleculeDataModelList.isEmpty()) {
            if (!this.isFragmentationStopAndDataLossConfirmed()) {
                return;
            }
            this.fragmentationService.clearCache();
        }
        Importer tmpImporter = Objects.requireNonNullElseGet(anImporter, () -> new Importer(this.settingsContainer));
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
        boolean tmpIsRegardStereo = this.settingsContainer.getRegardStereochemistrySetting();
        boolean tmpIsFillOpenValences = this.settingsContainer.getAddImplicitHydrogensAtImportSetting();
        boolean tmpIsKekulizationEnforced = this.settingsContainer.getImportAromaticsAsKekuleStructuresSetting();
        this.importTask = new Task<>() {
            @Override
            protected List<MoleculeDataModel> call() throws Exception {
                List<MoleculeDataModel> tmpSet = tmpImporter.importMoleculeFile(aFile, tmpIsRegardStereo, tmpIsFillOpenValences, tmpIsKekulizationEnforced);
                return tmpSet;
            }
        };
        this.importTask.setOnSucceeded(event ->
            //note: setOnSucceeded() takes place in the JavaFX GUI thread again but still runLater() is necessary to wait
            // for the thread to be free for the update
            Platform.runLater(() -> {
                List<MoleculeDataModel> tmpImportedMoleculeDataModels = null;
                try {
                    tmpImportedMoleculeDataModels = this.importTask.get();
                } catch (InterruptedException | ExecutionException anException) {
                    MainViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                            Message.get("Importer.FileImportExceptionAlert.Header"),
                            Message.get("Importer.FileImportExceptionAlert.Text"),
                            anException);
                    this.updateStatusBar(this.importerThread, Message.get("Status.importFailed"));
                }
                this.moleculeDataModelList.addAll(tmpImportedMoleculeDataModels);
                if (tmpImportedMoleculeDataModels == null || tmpImportedMoleculeDataModels.isEmpty() || this.moleculeDataModelList.isEmpty()) {
                    MainViewController.LOGGER.log(Level.WARNING, "Import failed, set of imported molecules is null or empty");
                    this.updateStatusBar(this.importerThread, Message.get("Status.importFailed"));
                    this.isImportRunningProperty.setValue(false);
                    Platform.runLater(() -> {
                        GuiUtil.guiMessageAlert(Alert.AlertType.WARNING,
                                Message.get("Error.ExceptionAlert.Title"),
                                Message.get("Importer.FileImportEmptyAlert.Header"),
                                Message.get("Importer.FileImportEmptyAlert.Content"));
                    });
                    return;
                }
                this.mainView.getMainMenuBar().getExportMenu().setDisable(true);
                this.mainView.getMainMenuBar().getHistogramViewerMenuItem().setDisable(true);
                this.mainView.getMainMenuBar().getOverviewViewMenuItem().setDisable(false);
                this.primaryStage.setTitle(Message.get("Title.text") + " - " + tmpImporter.getFileName() + " - " + tmpImportedMoleculeDataModels.size() +
                        " " + Message.get((tmpImportedMoleculeDataModels.size() == 1 ? "Title.molecule" : "Title.molecules")));
                this.importedFileName = tmpImporter.getFileName();
                this.updateStatusBar(this.importerThread, Message.get("Status.imported"));
                this.isImportRunningProperty.setValue(false);
                this.mainView.getMainCenterPane().setStyle("-fx-background-image: none");
                this.openMoleculesTab();
            })
        );
        this.importTask.setOnCancelled(event -> {
            this.updateStatusBar(this.importerThread, Message.get("Status.canceled"));
            this.isImportRunningProperty.setValue(false);
        });
        this.importTask.setOnFailed(event -> {
            Exception tmpCause = (Exception) event.getSource().getException();
            MainViewController.LOGGER.log(Level.SEVERE, tmpCause.toString(), tmpCause);
            this.updateStatusBar(this.importerThread, Message.get("Status.importFailed"));
            this.isImportRunningProperty.setValue(false);
            Platform.runLater(() -> {
                GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                        Message.get("Importer.FileImportExceptionAlert.Header"),
                        Message.get("Importer.FileImportExceptionAlert.Text"),
                        tmpCause);
            });
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
        GridTabForTableView tmpSelectedTab = (GridTabForTableView) this.mainTabPane.getSelectionModel().getSelectedItem();
        String tmpFragmentationName = tmpSelectedTab.getFragmentationNameOutOfTitle();
        List<FragmentDataModel> tmpFragmentsList = new ArrayList<>(this.mapOfFragmentDataModelLists.get(tmpFragmentationName));
        switch (anExportType) {
            case Exporter.ExportTypes.FRAGMENT_CSV_FILE, Exporter.ExportTypes.FRAGMENT_PDB_FILE, Exporter.ExportTypes.FRAGMENT_PDF_FILE, Exporter.ExportTypes.FRAGMENT_SINGLE_SD_FILE, FRAGMENT_MULTIPLE_SD_FILES:
                if (tmpFragmentsList.isEmpty() || tmpFragmentationName == null) {
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
                if (tmpFragmentsList.isEmpty() || this.moleculeDataModelList == null || this.moleculeDataModelList.isEmpty() || tmpFragmentationName == null) {
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
        //returns null if file chooser dialog was cancelled
        File tmpExportFile = tmpExporter.openFileChooserForExportFileOrDir(this.primaryStage, anExportType,
                ((GridTabForTableView) this.mainTabPane.getSelectionModel().getSelectedItem()).getFragmentationNameOutOfTitle());
        if (tmpExportFile == null) {
            return;
        }
        boolean tmpGenerate2dAtomCoordinates = false;
        if ((anExportType.equals(Exporter.ExportTypes.FRAGMENT_PDB_FILE)
                || anExportType.equals(Exporter.ExportTypes.FRAGMENT_SINGLE_SD_FILE)
                || anExportType.equals(Exporter.ExportTypes.FRAGMENT_MULTIPLE_SD_FILES))
                && (!ChemUtil.checkMoleculeListForCoordinates(this.getItemsListOfSelectedFragmentationByTabId(TabNames.FRAGMENTS)))) {
            ButtonType tmpConfirmationResult = GuiUtil.guiYesNoCancelConfirmationAlert(
                    Message.get("Exporter.FragmentsTab.ConfirmationAlert.No3dInformationAvailable.title"),
                    Message.get("Exporter.FragmentsTab.ConfirmationAlert.No3dInformationAvailable.header"),
                    Message.get("Exporter.FragmentsTab.ConfirmationAlert.No3dInformationAvailable.text")
            );
            /*
            yes -> generate coordinates
            no -> export but do not generate coordinates
            cancel -> abort export
             */
            if (tmpConfirmationResult == ButtonType.CANCEL) {
                return;
            }
            tmpGenerate2dAtomCoordinates = tmpConfirmationResult == ButtonType.YES;
        }
        //reassigned because variable needs to be effectively final to be used in the inner classes below
        boolean tmpGenerate2dAtomCoordinatesFinal = tmpGenerate2dAtomCoordinates;
        this.exportTask = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                return switch (anExportType) {
                    // TODO: consider disabling the export of e.g. the fragments if the respective tab was closed
                    //       and be aware that the sorting of the fragmentation tab is considered in the export.
                    case Exporter.ExportTypes.FRAGMENT_CSV_FILE -> tmpExporter.exportCsvFile(
                            tmpExportFile,
                            MainViewController.this.moleculeDataModelList,
                            tmpFragmentationName,
                            MainViewController.this.settingsContainer.getCsvExportSeparatorSettingCharacter(),
                            TabNames.FRAGMENTS
                    );
                    case Exporter.ExportTypes.FRAGMENT_PDB_FILE ->
                            tmpExporter.exportFragmentsAsChemicalFile(
                                    tmpExportFile,
                                    tmpFragmentsList,
                                    ChemFileTypes.PDB,
                                    tmpGenerate2dAtomCoordinatesFinal
                            );
                    case Exporter.ExportTypes.FRAGMENT_PDF_FILE -> tmpExporter.exportPdfFile(
                            tmpExportFile,
                            tmpFragmentsList,
                            MainViewController.this.moleculeDataModelList,
                            tmpFragmentationName,
                            MainViewController.this.importedFileName,
                            TabNames.FRAGMENTS
                    );
                    case Exporter.ExportTypes.FRAGMENT_SINGLE_SD_FILE ->
                            tmpExporter.exportFragmentsAsChemicalFile(
                                    tmpExportFile,
                                    tmpFragmentsList,
                                    ChemFileTypes.SDF,
                                    tmpGenerate2dAtomCoordinatesFinal,
                                    true
                            );
                    case Exporter.ExportTypes.FRAGMENT_MULTIPLE_SD_FILES ->
                            tmpExporter.exportFragmentsAsChemicalFile(
                                    tmpExportFile,
                                    tmpFragmentsList,
                                    ChemFileTypes.SDF,
                                    tmpGenerate2dAtomCoordinatesFinal,
                                    false
                            );
                    case Exporter.ExportTypes.ITEM_CSV_FILE -> tmpExporter.exportCsvFile(
                            tmpExportFile,
                            MainViewController.this.moleculeDataModelList,
                            tmpFragmentationName,
                            MainViewController.this.settingsContainer.getCsvExportSeparatorSettingCharacter(),
                            TabNames.ITEMIZATION
                    );
                    case Exporter.ExportTypes.ITEM_PDF_FILE -> tmpExporter.exportPdfFile(
                            tmpExportFile,
                            tmpFragmentsList,
                            MainViewController.this.moleculeDataModelList,
                            tmpFragmentationName,
                            MainViewController.this.importedFileName,
                            TabNames.ITEMIZATION
                    );
                    default -> throw new UnsupportedOperationException("Unknown export type.");
                };
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
                return;
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
                this.fragmentationService.getSelectedFragmenter().getFragmentationAlgorithmDisplayName(),
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
        GridTabForTableView tmpSelectedTab = (GridTabForTableView) this.mainTabPane.getSelectionModel().getSelectedItem();
        String tmpFragmentationName = tmpSelectedTab.getFragmentationNameOutOfTitle();
        List<FragmentDataModel> tmpFragmentsList = new ArrayList<>(this.mapOfFragmentDataModelLists.get(tmpFragmentationName));
        this.viewToolsManager.openHistogramView(this.primaryStage, tmpFragmentsList);
    }
    //
    /**
     * Adds CheckMenuItems for fragmentation algorithms to MainMenuBar.
     */
    private void addFragmentationAlgorithmCheckMenuItems() {
        ToggleGroup tmpToggleGroup = new ToggleGroup();
        for (IMoleculeFragmenter tmpFragmenter : this.fragmentationService.getFragmenters()) {
            RadioMenuItem tmpRadioMenuItem = new RadioMenuItem(tmpFragmenter.getFragmentationAlgorithmDisplayName());
            tmpRadioMenuItem.setToggleGroup(tmpToggleGroup);
            this.mainView.getMainMenuBar().getFragmentationAlgorithmMenu().getItems().add(tmpRadioMenuItem);
            if (!Objects.isNull(this.fragmentationService.getSelectedFragmenter())
                    && tmpFragmenter.getFragmentationAlgorithmDisplayName()
                        .equals(this.fragmentationService.getSelectedFragmenter().getFragmentationAlgorithmDisplayName())) {
                tmpToggleGroup.selectToggle(tmpRadioMenuItem);
            }
        }
        tmpToggleGroup.selectedToggleProperty().addListener((observableValue, oldValue, newValue) -> {
            if (tmpToggleGroup.getSelectedToggle() != null) {
                this.fragmentationService.setSelectedFragmenter(((RadioMenuItem) newValue).getText());
                this.fragmentationService.setSelectedFragmenterDisplayName(((RadioMenuItem) newValue).getText());
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
                    TableView<?> tmpTableView = ((GridTabForTableView) tmpTab).getTableView();
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
                    /*
                    the following might cause "javafx.scene.control.skin.VirtualFlow addTrailingCells
                    INFO: index exceeds maxCellCount. Check size calculations for class javafx.scene.control.TableRow"
                    when the new rows per page value is smaller than the older one, but it is not a real problem;
                    the refreshed GUI just needs to "scroll" to a different position
                    */
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
                    tmpDataForOverviewView.addAll(((IDataTableView) tmpSelectedTab.getTableView()).getItemsList().get(tmpIndexInDataList).getFragmentsOfSpecificFragmentation(tmpSelectedTab.getFragmentationNameOutOfTitle()));
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
        MoleculesTabView tmpMoleculesTab = new MoleculesTabView(
                this.moleculesDataTableView,
                FXCollections.observableArrayList(this.moleculeDataModelList),
                this.settingsContainer.getRowsPerPageSetting(),
                this.fragmentationService.selectedFragmenterDisplayNameProperty()
        );
        this.mainTabPane.getTabs().add(tmpMoleculesTab);
        tmpMoleculesTab.getPagination().setPageFactory(
                pageIndex -> this.moleculesDataTableView.createMoleculeTableViewPage(pageIndex, this.settingsContainer)
        );
        tmpMoleculesTab.setOnFragmentation(this::startFragmentation);
        tmpMoleculesTab.setOnCancelFragmentation(this::interruptFragmentation);
        tmpMoleculesTab.setOnOverview(() -> this.openOverviewView(OverviewViewController.DataSources.MOLECULES_TAB));
        this.fragmentationButton = tmpMoleculesTab.getFragmentationButton();
        this.cancelFragmentationButton = tmpMoleculesTab.getCancelFragmentationButton();
        this.cancelFragmentationButton.setVisible(false);
        int tmpRowsPerPage = this.settingsContainer.getRowsPerPageSetting();
        this.moleculesDataTableView.setOnSort(
                (EventHandler<SortEvent<TableView>>) event ->
                        GuiUtil.sortTableViewGlobally(event, tmpMoleculesTab.getPagination(), tmpRowsPerPage)
        );
        Consumer<Number> tmpWidthChangeListener = newValue -> {
            for (Object tmpObject : this.moleculesDataTableView.getItems()) {
                ((MoleculeDataModel) tmpObject).setStructureImageWidth(
                        this.moleculesDataTableView.getStructureColumn().getWidth()
                );
            }
        };
        tmpMoleculesTab.setOnTableWidthChanged(tmpWidthChangeListener);
        this.moleculesDataTableView.addTableViewHeightListener(this.settingsContainer);
        this.moleculesDataTableView.getCopyMenuItem().setOnAction(
                event -> GuiUtil.copySelectedTableViewCellsToClipboard(this.moleculesDataTableView)
        );
        this.moleculesDataTableView.setOnKeyPressed(event -> {
            if (GuiDefinitions.KEY_CODE_COPY.match(event)) {
                GuiUtil.copySelectedTableViewCellsToClipboard(this.moleculesDataTableView);
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
        List<MoleculeDataModel> tmpSelectedMolecules = this.moleculeDataModelList.stream().filter(MoleculeDataModel::isSelected).toList();
        int tmpNumberOfCores = this.settingsContainer.getNumberOfTasksForFragmentationSetting();
        boolean tmpIsKeepLastFragmentSetting = this.settingsContainer.isKeepLastFragmentSetting();
        boolean tmpIsStereoChemRegarded = this.settingsContainer.getRegardStereochemistrySetting();
        try {
            this.fragmentationButton.setDisable(true);
            this.cancelFragmentationButton.setVisible(true);
            this.parallelFragmentationMainTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    if (isPipelining) {
                        MainViewController.this.fragmentationService.startPipelineFragmentation(tmpSelectedMolecules,
                                tmpNumberOfCores, tmpIsStereoChemRegarded, tmpIsKeepLastFragmentSetting);
//                        fragmentationService.startPipelineFragmentationMolByMol(tmpSelectedMolecules, tmpNumberOfCores);
                    } else {
                        MainViewController.this.fragmentationService.startSingleFragmentation(tmpSelectedMolecules,
                                tmpNumberOfCores, tmpIsStereoChemRegarded);
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
            this.parallelFragmentationMainTask.setOnSucceeded(event ->
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
                        MainViewController.LOGGER.info("End of method startFragmentation after " + (tmpEndTime - tmpStartTime) / 1000000000.0 + " seconds.");
                    } catch (Exception anException) {
                        MainViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                    }
                })
            );
            this.parallelFragmentationMainTask.setOnCancelled(event -> {
                this.updateStatusBar(this.fragmentationThread, Message.get("Status.canceled"));
                //note: the export functions give a warning to the user should the fragment set be empty
                this.mainView.getMainMenuBar().getExportMenu().setDisable(false);
                this.fragmentationButton.setDisable(false);
                this.cancelFragmentationButton.setVisible(false);
                this.isFragmentationRunning = false;
                MainViewController.LOGGER.info("Fragmentation cancelled by user.");
            });
            // see also how exceptions thrown in the task's sub-tasks are handled in startFragmentation()
            this.parallelFragmentationMainTask.setOnFailed(event -> {
                this.updateStatusBar(this.fragmentationThread, Message.get("Status.failed"));
                //note: the export functions give a warning to the user should the fragment set be empty
                this.mainView.getMainMenuBar().getExportMenu().setDisable(false);
                this.fragmentationButton.setDisable(false);
                this.cancelFragmentationButton.setVisible(false);
                this.isFragmentationRunning = false;
                Exception tmpCause = (Exception) event.getSource().getException();
                MainViewController.LOGGER.log(Level.SEVERE, tmpCause.toString(), tmpCause);
                Platform.runLater(() -> {
                    GuiUtil.guiExceptionAlert(Message.get("MainViewController.FragmentationError.Title"),
                            Message.get("MainViewController.FragmentationError.Header"),
                            Message.get("MainViewController.FragmentationError.Content"),
                            tmpCause);
                });
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
     * @return FragmentsTabView
     */
    private FragmentsTabView createFragmentsTab(String aFragmentationName) {
        FragmentsDataTableView tmpFragmentsDataTableView = new FragmentsDataTableView(this.configuration);
        FragmentsTabView tmpFragmentsTab = new FragmentsTabView(
                tmpFragmentsDataTableView,
                aFragmentationName,
                FXCollections.observableArrayList(this.mapOfFragmentDataModelLists.get(aFragmentationName)),
                this.settingsContainer.getRowsPerPageSetting()
        );
        this.mainTabPane.getTabs().add(tmpFragmentsTab);
        tmpFragmentsTab.setOnCloseRequest(tmpEvent -> this.closeTabWithEvent(tmpEvent, tmpFragmentsTab));
        tmpFragmentsTab.getCloseAllTabsItem().setOnAction(event -> this.closeAllTabs());
        tmpFragmentsTab.getCloseTabItem().setOnAction(event -> this.closeTab(tmpFragmentsTab));
        tmpFragmentsTab.getPagination().setPageFactory(
                pageIndex -> tmpFragmentsDataTableView.createFragmentsTableViewPage(pageIndex, this.settingsContainer)
        );
        tmpFragmentsTab.setOnExportPdf(() -> this.exportFile(Exporter.ExportTypes.FRAGMENT_PDF_FILE));
        tmpFragmentsTab.setOnExportCsv(() -> this.exportFile(Exporter.ExportTypes.FRAGMENT_CSV_FILE));
        tmpFragmentsTab.setOnCancelExport(this::interruptExport);
        tmpFragmentsTab.setOnOverview(() -> this.openOverviewView(OverviewViewController.DataSources.FRAGMENTS_TAB));
        tmpFragmentsTab.setOnHistogram(this::openHistogramView);
        tmpFragmentsTab.bindCancelButtonVisibility(this.isExportRunningProperty);
        int tmpRowsPerPage = this.settingsContainer.getRowsPerPageSetting();
        tmpFragmentsDataTableView.setOnSort(
                (EventHandler<SortEvent<TableView>>) event ->
                        GuiUtil.sortTableViewGlobally(event, tmpFragmentsTab.getPagination(), tmpRowsPerPage)
        );
        tmpFragmentsTab.setOnTableWidthChanged(newValue -> {
            for (Object tmpObject : tmpFragmentsDataTableView.getItems()) {
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
     * Creates a menu item to close all tabs upon clicking and confirming a warning message.
     * The warning message prompts the user to confirm that they want to close all fragmentation result tabs and
     * therefore is fine with deleting the fragmentation results if not exported.
     *
     * @return the menu item
     */
    private MenuItem createCloseAllMenuItem() {
        MenuItem tmpCloseAllItem = new MenuItem(Message.get("MainView.tabMenuBar.closeAll.text"));
        tmpCloseAllItem.setOnAction(event -> this.closeAllTabs());
        return tmpCloseAllItem;
    }
    //
    /**
     * Closes all tabs that are closeable (everything except the molecules tab) and cleans up the data.
     */
    private void closeAllTabs() {
        if (this.settingsContainer.getShowDataWillBeLostWarningSetting()) {
            GuiUtil.CheckboxAndButtonResult tmpCheckboxAndConfirmationResult = GuiUtil.guiConfirmationAlertWithCheckbox(
                    Message.get("MainViewController.Warning.CloseAllTabs.Title"),
                    Message.get("MainViewController.Warning.CloseAllTabs.Header"),
                    Message.get("MainViewController.Warning.CloseAllTabs.Content"),
                    Message.get("SettingsContainer.showDataWillBeLostWarning.checkbox.text")
            );

            if (tmpCheckboxAndConfirmationResult.buttonType() != ButtonType.OK) {
                return;
            }
            if (tmpCheckboxAndConfirmationResult.checkboxChecked()) {
                this.settingsContainer.setShowDataWillBeLostWarningSetting(false);
            }
        }
        for (int i = this.mainTabPane.getTabs().size() - 1; i >= 0; i--) {
            Tab tmpTab = this.mainTabPane.getTabs().get(i);
            // it is assumed here that MORTAR does NOT have any other
            // types of tabs that could be closed besides the GridTableView
            // as the itemization tab and fragmentation tab are both constructed
            // as GridTableViews
            if (tmpTab.isClosable() && tmpTab instanceof GridTabForTableView tmpGridTab) {
                if (isGridTabDataCleanable(tmpGridTab)) {
                    this.cleanupGridTabData(tmpGridTab);
                }
                this.mainTabPane.getTabs().remove(i);
            }
        }
        // disable the buttons for fragmentation results (here namely the export and histogram buttons).
        // because all fragmentation results were closed
        this.mainView.getMainMenuBar().getExportMenu().setDisable(true);
        this.mainView.getMainMenuBar().getHistogramViewerMenuItem().setDisable(true);
    }
    //
    /**
     * Creates a new menu item to close the tab of the displayed menu with a
     * confirmation dialogue if this would clean up the data.
     *
     * @param aGridTableView the tab to close for this menu item.
     * @return the menu item to close this specific tab.
     */
    private MenuItem createCloseTabMenuItem(GridTabForTableView aGridTableView) {
        MenuItem tmpCloseTabItem = new MenuItem(Message.get("MainView.tabMenuBar.closeTab.text"));
        tmpCloseTabItem.setOnAction(event -> this.closeTab(aGridTableView));
        return tmpCloseTabItem;
    }
    //
    /**
     * Closes the tab of the grid table and cleans up the referenced data if possible.
     *
     * @param aGridTableView the tab to remove.
     * @throws NullPointerException if the given {@link GridTabForTableView} is null.
     */
    private void closeTab(GridTabForTableView aGridTableView) {
        Objects.requireNonNull(aGridTableView, "The tab which should be closed can not be null");
        if (this.isGridTabDataCleanable(aGridTableView)) {
            if (this.settingsContainer.getShowDataWillBeLostWarningSetting()) {
                GuiUtil.CheckboxAndButtonResult tmpCheckboxAndConfirmationResult = GuiUtil.guiConfirmationAlertWithCheckbox(
                        Message.get("MainViewController.Warning.CloseTab.Title"),
                        Message.get("MainViewController.Warning.CloseTab.Header"),
                        Message.get("MainViewController.Warning.CloseTab.Content"),
                        Message.get("SettingsContainer.showDataWillBeLostWarning.checkbox.text")
                );
                if (tmpCheckboxAndConfirmationResult.buttonType() != ButtonType.OK) {
                    return;
                }
                if (tmpCheckboxAndConfirmationResult.checkboxChecked()) {
                    this.settingsContainer.setShowDataWillBeLostWarningSetting(false);
                }
            }
            this.mainTabPane.getTabs().remove(aGridTableView);
            this.cleanupGridTabData(aGridTableView);
        } else {
            this.mainTabPane.getTabs().remove(aGridTableView);
        }

        // Check if all fragmentation tabs were closed to disable the buttons for
        // fragmentation results (here namely the export and histogram buttons)
        if (this.mainTabPane.getTabs().size() <= 1) {
            this.mainView.getMainMenuBar().getExportMenu().setDisable(true);
            this.mainView.getMainMenuBar().getHistogramViewerMenuItem().setDisable(true);
        }
    }
    /**
     * Closes the tab of the grid table and cleans up the referenced data if possible after
     * the warning dialogue of data loss is acknowledged by the user with pressing OK on the
     * information popup.
     *
     * @param anEvent the Event to consume if cancel gets pressed on the warning window.
     * @param aGridTableView the tab to remove.
     */
    private void closeTabWithEvent(Event anEvent, GridTabForTableView aGridTableView) {
        closeTab(aGridTableView);
        anEvent.consume();
    }
    //
    /**
     * Check if there is no other tab with the same fragmentation name.
     * Because the fragments are used in the itemization tab as well as in the fragments tab
     * it is necessary to check if BOTH are closed to be able to clean up the underlying data.
     *
     * @param aGridTab the tab to check if its data can be cleaned up
     * @return true if the data can be cleaned up, false otherwise
     */
    private boolean isGridTabDataCleanable(GridTabForTableView aGridTab) {
        String tmpFragmentationName = aGridTab.getFragmentationNameOutOfTitle();
        for (Tab tmpTab : this.mainTabPane.getTabs()) {
            if (!(tmpTab instanceof GridTabForTableView tmpGridTableTab) || tmpTab == aGridTab) {
                continue;
            }
            if (tmpFragmentationName.equals(tmpGridTableTab.getFragmentationNameOutOfTitle())) {
                return false;
            }
        }
        return true;
    }
    //
    /**
     * Perform cleanup of the data of the given GridTabForTableView so the contents of a fragmentation result
     * can be garbage-collected. Please check if the data of a tab can be cleaned up by calling
     * {@code isGridTabDataCleanable} before cleaning this function.
     *
     * @param aGridTab the GridTabForTableView whose data needs to be cleaned up.
     */
    private void cleanupGridTabData(GridTabForTableView aGridTab) {
        String tmpFragmentationName = aGridTab.getFragmentationNameOutOfTitle();
        for (MoleculeDataModel tmpMoleculeDataModel : this.moleculeDataModelList) {
            tmpMoleculeDataModel.clearFragmentsForFragmentation(tmpFragmentationName);
        }
        this.mapOfFragmentDataModelLists.remove(tmpFragmentationName);
        this.fragmentationService.clearFragmentation(tmpFragmentationName);
    }
    /**
     * Creates and returns a tab which visualizes the resulting fragments of each molecule that has undergone the
     * fragmentation with the given name.
     *
     * @param aFragmentationName String, unique name for the fragmentation job
     * @return Tab
     */
    private ItemizationTabView createItemsTab(String aFragmentationName) {
        List<MoleculeDataModel> tmpItemsList = this.moleculeDataModelList.stream()
                .filter(x -> x.hasMoleculeUndergoneSpecificFragmentation(aFragmentationName))
                .collect(Collectors.toList());
        ItemizationDataTableView tmpItemizationDataTableView =
                new ItemizationDataTableView(aFragmentationName, this.configuration);
        ItemizationTabView tmpItemizationTab = new ItemizationTabView(
                tmpItemizationDataTableView,
                aFragmentationName,
                FXCollections.observableArrayList(tmpItemsList),
                this.settingsContainer.getRowsPerPageSetting()
        );
        tmpItemizationTab.getPagination().setPageFactory(
                pageIndex -> tmpItemizationDataTableView.createItemizationTableViewPage(
                        pageIndex,
                        aFragmentationName,
                        this.settingsContainer
                )
        );
        tmpItemizationTab.setOnCloseRequest(event -> this.closeTabWithEvent(event, tmpItemizationTab));
        tmpItemizationTab.setOnExportCsv(() -> this.exportFile(Exporter.ExportTypes.ITEM_CSV_FILE));
        tmpItemizationTab.setOnExportPdf(() -> this.exportFile(Exporter.ExportTypes.ITEM_PDF_FILE));
        tmpItemizationTab.setOnCancelExport(this::interruptExport);
        tmpItemizationTab.setOnOpenHistogramView(this::openHistogramView);
        tmpItemizationTab.getCloseTabItem().setOnAction(
                event -> this.closeTabWithEvent(new Event(
                        tmpItemizationTab,
                        tmpItemizationTab,
                        Tab.TAB_CLOSE_REQUEST_EVENT
                ), tmpItemizationTab)
        );
        tmpItemizationTab.getCloseAllTabsItem().setOnAction(event -> this.closeAllTabs());
        tmpItemizationTab.bindCancelButtonVisibility(this.isExportRunningProperty);
        int tmpRowsPerPage = this.settingsContainer.getRowsPerPageSetting();
        tmpItemizationDataTableView.setOnSort((EventHandler<SortEvent<TableView>>) event ->
                GuiUtil.sortTableViewGlobally(event, tmpItemizationTab.getPagination(), tmpRowsPerPage)
        );
        tmpItemizationTab.setOnTableWidthChanged(newValue -> {
            for (Object tmpObject : tmpItemizationDataTableView.getItems()) {
                ((MoleculeDataModel) tmpObject).setStructureImageWidth(
                        tmpItemizationDataTableView.getMoleculeStructureColumn().getWidth()
                );
            }
        });
        tmpItemizationDataTableView.addTableViewHeightListener(this.settingsContainer);
        tmpItemizationDataTableView.getCopyMenuItem().setOnAction(
                event -> GuiUtil.copySelectedTableViewCellsToClipboard(tmpItemizationDataTableView)
        );
        tmpItemizationDataTableView.getOverviewViewMenuItem().setOnAction(
                event -> this.openOverviewView(OverviewViewController.DataSources.ITEM_WITH_FRAGMENTS_SAMPLE)
        );
        tmpItemizationDataTableView.setOnKeyPressed(event -> {
            if (GuiDefinitions.KEY_CODE_COPY.match(event)) {
                GuiUtil.copySelectedTableViewCellsToClipboard(tmpItemizationDataTableView);
            }
        });
        this.mainTabPane.getTabs().add(tmpItemizationTab);
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
        return switch (aThreadType) {
            case FRAGMENTATION_THREAD -> Message.get("Status.running");
            case IMPORT_THREAD -> Message.get("Status.importing");
            case EXPORT_THREAD -> Message.get("Status.exporting");
            default -> "Could not find message";
        };
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
