/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2020  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas-schaub@uni-jena.de)
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
import de.unijena.cheminf.mortar.model.fragmentation.*;
import de.unijena.cheminf.mortar.model.io.Importer;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.xpath.operations.Bool;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.hash.MoleculeHashGenerator;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.ErtlFunctionalGroupsFinderUtility;
import java.io.File;
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
//    private ObservableList<FragmentDataModel> fragmentDataModelList;
    private MoleculesDataTableView moleculesDataTableView;
    private int rowsPerPage;
    private boolean selectionAll;
    private boolean selectionAllCheckBoxAction;
    private FragmentationService fragmentationService;
    private Button fragmentationButton;

    private HashMap<String, ObservableList<FragmentDataModel>> mapOfFragmentDataModelLists;

    private ErtlFunctionalGroupsFinderFragmenter ertl;
    private SugarRemovalUtilityFragmenter sugar;
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
//        this.fragmentDataModelList = FXCollections.observableArrayList();
        this.primaryStage = aStage;
        this.mainView = aMainView;
        this.appDir = anAppDir;
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
     *
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
        //TODO: More implementation needed
    }
    //
    /**
     * Closes application
     */
    private void closeApplication(int aStatus) {
        //TODO: add alert if model is not null
        Platform.exit();
        System.exit(aStatus);
    }
    //
    /**
     * Loads molecule file and opens molecules tab
     *
     * @param aParentStage
     */
    private void loadMoleculeFile(Stage aParentStage) {
        //TODO: add alert if model is not null
        Importer tmpImporter = new Importer();
        IAtomContainerSet tmpAtomContainerSet = tmpImporter.Import(aParentStage);
        if(tmpAtomContainerSet == null || tmpAtomContainerSet.isEmpty())
            return;
        this.clearGuiAndCollections();
        this.primaryStage.setTitle(Message.get("Title.text") + " - " + tmpImporter.getFileName());
        for (IAtomContainer tmpAtomContainer : tmpAtomContainerSet.atomContainers()) {
            if(tmpAtomContainer.getProperty("SMILES") == null){
                try {
                    SmilesGenerator tmpSmilesGen = new SmilesGenerator(SmiFlavor.Absolute);
                    tmpAtomContainer.setProperty("SMILES", tmpSmilesGen.create(tmpAtomContainer));
                } catch (CDKException anException){
                    MainViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                }
            }
            MoleculeDataModel tmpMoleculeDataModel = new MoleculeDataModel(tmpAtomContainer.getID(), tmpAtomContainer);
            tmpMoleculeDataModel.setName(tmpAtomContainer.getProperty("NAME"));
            this.moleculeDataModelList.add(tmpMoleculeDataModel);
        }
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
            this.mainView.getMainMenuBar().getFragmentationAlgorithmMenuItem().getItems().add(tmpRadioMenuItem);
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
     * Opens molecules tab
     */
    private void OpenMoleculesTab() {
        GridTabForTableView tmpMoleculesTab = new GridTabForTableView(Message.get("MainTabPane.moleculesTab.title"), TabNames.Molecules.name());
        this.mainTabPane.getTabs().add(tmpMoleculesTab);
        this.moleculesDataTableView = new MoleculesDataTableView();
        Pagination tmpPagination = new Pagination((this.moleculeDataModelList.size() / rowsPerPage + 1), 0);
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
        //TODO
        //TODO: not sure if it works

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

    private void addFragmentationResultTabs(String aFragmentationName){
        GridTabForTableView tmpFragmentsTab = new GridTabForTableView(Message.get("MainTabPane.fragmentsTab.title"), TabNames.Fragments.name());
        this.mainTabPane.getTabs().add(tmpFragmentsTab);
        FragmentsDataTableView tmpFragmentsDataTableView = new FragmentsDataTableView();
        Pagination tmpPagination = new Pagination((this.mapOfFragmentDataModelLists.get(aFragmentationName).size() / rowsPerPage + 1), 0);
        tmpPagination.setPageFactory((pageIndex) -> createFragmentsTableViewPage(pageIndex, tmpFragmentsDataTableView, this.mapOfFragmentDataModelLists.get(aFragmentationName)));
        VBox.setVgrow(tmpPagination, Priority.ALWAYS);
        HBox.setHgrow(tmpPagination, Priority.ALWAYS);
        tmpFragmentsTab.addNodeToGridPane(tmpPagination, 0,0,2,2);
        int tmpAmount = 0;
        for(int i= 0; i < moleculeDataModelList.size(); i++){
                    tmpAmount = Math.max(tmpAmount, moleculeDataModelList.get(i).getFragmentFrequencyOfSpecificAlgorithm(aFragmentationName).size());
        }
        GridTabForTableView tmpItemizationTab = new GridTabForTableView("Items", TabNames.Itemization.name());
        mainTabPane.getTabs().add(tmpItemizationTab);
        ItemizationDataTableView tmpItemizationDataTableView = new ItemizationDataTableView(tmpAmount, aFragmentationName);
        Pagination tmpPaginationItems = new Pagination(moleculeDataModelList.size() / rowsPerPage + 1, 0);
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
    private Node createFragmentsTableViewPage(int pageIndex, FragmentsDataTableView aFragmentsDataTableView, List<FragmentDataModel> aListOfFragments) {
        int fromIndex = pageIndex * rowsPerPage;
        int toIndex = Math.min(fromIndex + rowsPerPage, aListOfFragments.size());


        aFragmentsDataTableView.sortPolicyProperty().set(
                new Callback<FragmentsDataTableView, Boolean>() {
                    @Override
                    public Boolean call(FragmentsDataTableView param) {
                        if(param.getSortOrder().size() > 0){
                            String property = ((TableColumn)param.getSortOrder().get(0)).getText();
                            System.out.println(((TableColumn)param.getSortOrder().get(0)).cellValueFactoryProperty().toString());
                            String sortType = ((TableColumn)param.getSortOrder().get(0)).getSortType().toString();
                        }
                        return true;
                    }
                }
        );


        aFragmentsDataTableView.setItems(FXCollections.observableArrayList(aListOfFragments.subList(fromIndex, toIndex)));
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
    //</editor-fold>
}

/**
 * Enum ro tab names
 */
enum TabNames{
    Molecules, Fragments, Itemization
}