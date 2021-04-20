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
import de.unijena.cheminf.mortar.gui.views.ItemizationDataTableView;
import de.unijena.cheminf.mortar.gui.views.MainView;
import de.unijena.cheminf.mortar.gui.views.MoleculesDataTableView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.fragmentation.FragmentationService;
import de.unijena.cheminf.mortar.model.fragmentation.FragmentationThread;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.IMoleculeFragmenter;
import de.unijena.cheminf.mortar.model.io.Importer;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.event.EventType;
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
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
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
    private int rowsPerPage;
    private boolean selectionAll;
    private boolean selectionAllCheckBoxAction;
    private FragmentationService fragmentationService;
    private Button fragmentationButton;
    private HashMap<String, ObservableList<FragmentDataModel>> mapOfFragmentDataModelLists;
    //</editor-fold>
    //<editor-fold desc="private static final variables" defaultstate="collapsed">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(MainViewController.class.getName());
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
        this.primaryStage.setTitle(Message.get("Title.text"));
        this.primaryStage.setScene(this.scene);
        this.primaryStage.show();
        this.primaryStage.setMinHeight(GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.primaryStage.setMinWidth(GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE);
        //</editor-fold>
        this.rowsPerPage = 5; //TODO: get this from settings
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
                anEvent -> this.closeApplication(0));
        this.mainView.getMainMenuBar().getLoadMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.loadMoleculeFile(this.primaryStage));
        this.mainView.getMainMenuBar().getFragmentationSettingsMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.openFragmentationSettingsView());
        this.mainView.getMainMenuBar().getGlobalSettingsMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.openGlobalSettingsView());
        this.primaryStage.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, (this::closeWindowEvent));
        //TODO: More implementation needed
    }
    //
    /**
     * Closes application
     */
    private void closeApplication(int aStatus) {
        if(moleculeDataModelList.size() > 0){
            ButtonType tmpConformationResult = GuiUtil.GuiConformationAlert("Warning", "Data will be lost.", "Data will be lost and application will be closed if you press Ok. Click cancel to return to application."); //TODO
            if(tmpConformationResult!= ButtonType.OK){
                return;
            }
        }
        //TODO: bind/addListener to top right corner X
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
        if(moleculeDataModelList.size() > 0){
            ButtonType tmpConformationResult = GuiUtil.GuiConformationAlert("Warning", "Data will be lost.", "Data will be lost if you press Ok. Click cancel to return."); //TODO
            if(tmpConformationResult!= ButtonType.OK){
                return;
            }
        }
        Importer tmpImporter = new Importer();
        IAtomContainerSet tmpAtomContainerSet = tmpImporter.Import(aParentStage);
        if(tmpAtomContainerSet == null || tmpAtomContainerSet.isEmpty())
            return;
        this.clearGuiAndCollections();
        this.primaryStage.setTitle(Message.get("Title.text") + " - " + tmpImporter.getFileName() + " - " + tmpAtomContainerSet.getAtomContainerCount() + " molecules" );
        for (IAtomContainer tmpAtomContainer : tmpAtomContainerSet.atomContainers()) {
            String tmpSmiles = "";
            try {
                SmilesGenerator tmpSmilesGen = new SmilesGenerator(SmiFlavor.Unique);
                tmpSmiles = tmpSmilesGen.create(tmpAtomContainer);
            } catch (CDKException | NullPointerException anException){
                MainViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            }
            MoleculeDataModel tmpMoleculeDataModel = new MoleculeDataModel(tmpAtomContainer.getID(), tmpAtomContainer, tmpSmiles);
            tmpMoleculeDataModel.setName(tmpAtomContainer.getProperty("NAME"));
            this.moleculeDataModelList.add(tmpMoleculeDataModel);
        }
        MainViewController.LOGGER.log(Level.SEVERE, "Imported " + tmpAtomContainerSet.getAtomContainerCount() + " molecules from file: " + tmpImporter.getFileName());
        this.OpenMoleculesTab();
    }
    //
    private void openFragmentationSettingsView(){
        FragmentationSettingsViewController tmpFragmentationSettingsViewController =
                new FragmentationSettingsViewController(this.primaryStage, this.fragmentationService.getFragmenters(), this.fragmentationService.getSelectedFragmenter().getFragmentationAlgorithmName());
    }
    //
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
    private void openGlobalSettingsView(){

    }
    //
    /**
     * Opens molecules tab
     */
    private void OpenMoleculesTab() {
        GridTabForTableView tmpMoleculesTab = new GridTabForTableView(Message.get("MainTabPane.moleculesTab.title"), TabNames.Molecules.name());
        this.mainTabPane.getTabs().add(tmpMoleculesTab);
        this.moleculesDataTableView = new MoleculesDataTableView();
        int tmpPageCount = this.moleculeDataModelList.size() / rowsPerPage;
        if(this.moleculeDataModelList.size() % rowsPerPage > 0){
            tmpPageCount++;
        }
        Pagination tmpPagination = new Pagination(tmpPageCount, 0);
        tmpPagination.setPageFactory(this::createDataTableViewPage);
        VBox.setVgrow(tmpPagination, Priority.ALWAYS);
        HBox.setHgrow(tmpPagination, Priority.ALWAYS);
        tmpMoleculesTab.addNodeToGridPane(tmpPagination, 0,0,2,2);
        this.fragmentationButton = new Button(Message.get("MainTabPane.moleculesTab.button.text"));
        tmpMoleculesTab.addNodeToGridPane(this.fragmentationButton, 1,1,1,1);
        //TODO: disable 'tmpFragmentButton' while fragmentation is running
        this.fragmentationButton.setOnAction(event->{
            //TODO: add implementation to start fragmentation algorithm
            this.startFragmentation();
        });
    }
    //
    /**
     * Creates a page for the pagination for the dataTableView //TODO: refine comment
     *
     * @param aPageIndex
     * @return Node, page of pagination
     */
    private Node createDataTableViewPage(int aPageIndex){
        int tmpFromIndex = aPageIndex * this.rowsPerPage;
        int tmpToIndex = Math.min(tmpFromIndex + this.rowsPerPage, this.moleculeDataModelList.size());
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
        return new BorderPane(this.moleculesDataTableView);
    }
    //
    /**
     * Starts fragmentation task and opens fragment and itemiztation tabs
     */
    private void startFragmentation(){
        List<MoleculeDataModel> tmpSelectedMolecules = this.moleculeDataModelList.stream().filter(mol -> mol.isSelected()).collect(Collectors.toList());
        int tmpNumberOfCores = Runtime.getRuntime().availableProcessors(); //TODO: implement settings
        try{
            this.fragmentationButton.setDisable(true);
            FragmentationThread tmpFragmentationThread = this.fragmentationService.startFragmentationThread(tmpSelectedMolecules, tmpNumberOfCores);
            ObservableList<FragmentDataModel> tmpObservableFragments = FXCollections.observableArrayList();
            Set<String> tmpKeys = this.fragmentationService.getFragments().keySet();
            for(String tmpKey : tmpKeys){
                tmpObservableFragments.add(this.fragmentationService.getFragments().get(tmpKey));
            }
            this.mapOfFragmentDataModelLists.put(this.fragmentationService.getCurrentFragmentationName(), tmpObservableFragments);
            this.addFragmentationResultTabs(this.fragmentationService.getCurrentFragmentationName());
            this.fragmentationButton.setDisable(false);
        } catch(Exception anException){
            //TODO
        }
    }
    //
    private void addFragmentationResultTabs(String aFragmentationName){
        //fragments tab
        GridTabForTableView tmpFragmentsTab = new GridTabForTableView(Message.get("MainTabPane.fragmentsTab.title") + " - " + aFragmentationName, TabNames.Fragments.name());
        this.mainTabPane.getTabs().add(tmpFragmentsTab);
        FragmentsDataTableView tmpFragmentsDataTableView = new FragmentsDataTableView();
        ObservableList<FragmentDataModel> tmpList = FXCollections.observableArrayList(this.mapOfFragmentDataModelLists.get(aFragmentationName));
        tmpFragmentsDataTableView.setFragmentDataModelList(tmpList);
        int tmpPageCount = tmpList.size() / rowsPerPage;
        if(tmpList.size() % rowsPerPage > 0){
            tmpPageCount++;
        }
        Pagination tmpPagination = new Pagination(tmpPageCount, 0);
        tmpPagination.setPageFactory((pageIndex) -> createFragmentsTableViewPage(pageIndex, tmpFragmentsDataTableView));
        VBox.setVgrow(tmpPagination, Priority.ALWAYS);
        HBox.setHgrow(tmpPagination, Priority.ALWAYS);
        tmpFragmentsTab.addNodeToGridPane(tmpPagination, 0,0,2,2);
        tmpFragmentsDataTableView.setOnSort(new EventHandler<SortEvent<TableView>>() {
            @Override
            public void handle(SortEvent<TableView> event) {
                int i = 5;
                String tmpSortProp = ((PropertyValueFactory)((TableColumn) event.getSource().getSortOrder().get(0)).cellValueFactoryProperty().getValue()).getProperty().toString();
                TableColumn.SortType tmpSortType = ((TableColumn) event.getSource().getSortOrder().get(0)).getSortType();
                sortGivenFragmentListByPropertyAndSortType(((FragmentsDataTableView)event.getSource()).getFragmentDataModelList(), tmpSortProp, tmpSortType.toString());
                int fromIndex = tmpPagination.getCurrentPageIndex() * rowsPerPage;
                int toIndex = Math.min(fromIndex + rowsPerPage, ((FragmentsDataTableView)event.getSource()).getFragmentDataModelList().size());
                event.getSource().getItems().clear();
                event.getSource().getItems().addAll(((FragmentsDataTableView)event.getSource()).getFragmentDataModelList().subList(fromIndex,toIndex));
            }
        });
        //itemization tab
        int tmpAmount = 0;
        for(int i= 0; i < moleculeDataModelList.size(); i++){
                    tmpAmount = Math.max(tmpAmount, moleculeDataModelList.get(i).getFragmentFrequencyOfSpecificAlgorithm(aFragmentationName).size());
        }
        GridTabForTableView tmpItemizationTab = new GridTabForTableView(Message.get("MainTabPane.itemizationTab.title") + " - " + aFragmentationName, TabNames.Itemization.name());
        mainTabPane.getTabs().add(tmpItemizationTab);
        ItemizationDataTableView tmpItemizationDataTableView = new ItemizationDataTableView(tmpAmount, aFragmentationName);
        tmpPageCount = moleculeDataModelList.size() / rowsPerPage;
        if(moleculeDataModelList.size() % rowsPerPage > 0){
            tmpPageCount++;
        }
        Pagination tmpPaginationItems = new Pagination( tmpPageCount, 0);
        tmpPaginationItems.setPageFactory((pageIndex) -> createItemizationTableViewPage(pageIndex, tmpItemizationDataTableView));
        VBox.setVgrow(tmpPaginationItems, Priority.ALWAYS);
        HBox.setHgrow(tmpPaginationItems, Priority.ALWAYS);
        tmpItemizationTab.addNodeToGridPane(tmpPaginationItems, 0,0,2,2);

    }
    //
    /**
     * Creates a fragments tableview page
     *
     * @param pageIndex
     * @param aFragmentsDataTableView
     * @return
     */
    private Node createFragmentsTableViewPage(int pageIndex, FragmentsDataTableView aFragmentsDataTableView) {
        int fromIndex = pageIndex * rowsPerPage;
        int toIndex = Math.min(fromIndex + rowsPerPage, aFragmentsDataTableView.getFragmentDataModelList().size());
        aFragmentsDataTableView.setItems(FXCollections.observableArrayList(aFragmentsDataTableView.getFragmentDataModelList().subList(fromIndex, toIndex)));
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
        int fromIndex = pageIndex * rowsPerPage;
        int toIndex = Math.min(fromIndex + rowsPerPage, this.moleculeDataModelList.size());
        anItemizationDataTableView.setItems(FXCollections.observableArrayList(this.moleculeDataModelList.subList(fromIndex, toIndex)));
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
    private void sortGivenFragmentListByPropertyAndSortType(List<FragmentDataModel> aList, String aProperty, String aSortType){
        Collections.sort(aList, new Comparator<FragmentDataModel>() {
            @Override
            public int compare(FragmentDataModel f1, FragmentDataModel f2) {
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