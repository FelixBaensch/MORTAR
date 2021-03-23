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
import de.unijena.cheminf.mortar.model.fragmentation.ErtlFunctionalGroupsFinderFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.IMoleculeFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.SugarRemovalUtilityFragmenter;
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
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.hash.MoleculeHashGenerator;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.ErtlFunctionalGroupsFinderUtility;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private ObservableList<FragmentDataModel> fragmentDataModelList;
    private MoleculesDataTableView moleculesDataTableView;
    private int rowsPerPage;
    private boolean selectionAll;
    private boolean selectionAllCheckBoxAction;

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
        this.fragmentDataModelList = FXCollections.observableArrayList();
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
        this.addListener();
        this.addFragmentationAlgorithmCheckMenuItems();
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
        Importer tmpImporter = new Importer();
        IAtomContainerSet tmpAtomContainerSet = tmpImporter.Import(aParentStage);
        this.clearGuiAndCollections();
        if(tmpAtomContainerSet == null || tmpAtomContainerSet.isEmpty())
            return;
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
        if(this.ertl == null)
            this.ertl = new ErtlFunctionalGroupsFinderFragmenter();
        if(this.sugar == null)
            this.sugar = new SugarRemovalUtilityFragmenter();
        FragmentationSettingsViewController tmpFragmentationSettingsViewController = new FragmentationSettingsViewController(this.primaryStage, new IMoleculeFragmenter[]{this.ertl, this.sugar});

    }
    //
    private void addFragmentationAlgorithmCheckMenuItems(){
        ToggleGroup tmpToggleGroup = new ToggleGroup();
        RadioMenuItem tmpRadioMenuItem = new RadioMenuItem("Ertl");
        tmpRadioMenuItem.setToggleGroup(tmpToggleGroup);
        RadioMenuItem tmpRadioMenuItem2 = new RadioMenuItem("Sugar");
        tmpRadioMenuItem2.setToggleGroup(tmpToggleGroup);
//        this.mainView.getMainMenuBar().getFragmentationAlgorithmMenuItem().getItems().add(new CheckMenuItem("Ertl"));
        this.mainView.getMainMenuBar().getFragmentationAlgorithmMenuItem().getItems().add(tmpRadioMenuItem);
        this.mainView.getMainMenuBar().getFragmentationAlgorithmMenuItem().getItems().add(tmpRadioMenuItem2);
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
        Button tmpFragmentButton = new Button(Message.get("MainTabPane.moleculesTab.button.text"));
        tmpMoleculesTab.addNodeToGridPane(tmpFragmentButton, 1,1,1,1);
        tmpFragmentButton.setOnAction(event->{
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
        Map<Long, IAtomContainer> tmpFragmentMap = new HashMap<>();
        Task tmpFragmentTask = new Task() {
            @Override
            protected Object call() throws Exception {
                IMoleculeFragmenter tmpFragmenter =  new ErtlFunctionalGroupsFinderFragmenter();
                MoleculeHashGenerator tmpHashGen = ErtlFunctionalGroupsFinderUtility.getFunctionalGroupHashGenerator();
                for(int i = 0; i < moleculeDataModelList.size(); i++){
                    List<IAtomContainer> tmpFragments = tmpFragmenter.fragmentMolecule(moleculeDataModelList.get(i).getAtomContainer());
                    for (IAtomContainer tmpFragment : tmpFragments) {
                        long tmpKey = tmpHashGen.generate(tmpFragment);
                        tmpFragmentMap.put(tmpKey, tmpFragment);
                    }
                }
                return null;
            }
        };
        this.mainView.getStatusBar().setTaskAndStart(tmpFragmentTask);
        this.mainView.getStatusBar().getTask().setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                tmpFragmentMap.forEach((k, v) -> {
                    fragmentDataModelList.add(new FragmentDataModel(v.getID(), v));
                });
//                moleculeDataModelList.get(0).getFragments().addAll(fragmentDataModelList);
                GridTabForTableView tmpFragmentsTab = new GridTabForTableView(Message.get("MainTabPane.fragmentsTab.title"), TabNames.Fragments.name());
                mainTabPane.getTabs().add(tmpFragmentsTab);
                FragmentsDataTableView tmpFragmentsDataTableView = new FragmentsDataTableView();
                Pagination tmpPagination = new Pagination((fragmentDataModelList.size() / rowsPerPage + 1), 0);
                tmpPagination.setPageFactory((pageIndex) -> createFragmentsTableViewPage(pageIndex, tmpFragmentsDataTableView));
                VBox.setVgrow(tmpPagination, Priority.ALWAYS);
                HBox.setHgrow(tmpPagination, Priority.ALWAYS);
                tmpFragmentsTab.addNodeToGridPane(tmpPagination, 0,0,2,2);

                int tmpAmount = 0;
                for(int i= 0; i < moleculeDataModelList.size(); i++){
//                    tmpAmount = Math.max(tmpAmount, moleculeDataModelList.get(i).getFragments().size());
                }
                GridTabForTableView tmpItemizationTab = new GridTabForTableView("Items", TabNames.Itemization.name());
                mainTabPane.getTabs().add(tmpItemizationTab);
                ItemizationDataTableView tmpItemizationDataTableView = new ItemizationDataTableView(tmpAmount);
                Pagination tmpPaginationItems = new Pagination(moleculeDataModelList.size() / rowsPerPage + 1, 0);
                tmpPaginationItems.setPageFactory((pageIndex) -> createItemizationTableViewPage(pageIndex, tmpItemizationDataTableView));
                VBox.setVgrow(tmpPaginationItems, Priority.ALWAYS);
                HBox.setHgrow(tmpPaginationItems, Priority.ALWAYS);
                tmpItemizationTab.addNodeToGridPane(tmpPaginationItems, 0,0,2,2);
            }
        });
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
        int toIndex = Math.min(fromIndex + rowsPerPage, this.fragmentDataModelList.size());
        aFragmentsDataTableView.setItems(FXCollections.observableArrayList(this.fragmentDataModelList.subList(fromIndex, toIndex)));
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
        this.fragmentDataModelList.clear();
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