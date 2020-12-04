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

import de.unijena.cheminf.mortar.gui.DataTableView;
import de.unijena.cheminf.mortar.gui.MainTabPane;
import de.unijena.cheminf.mortar.gui.MainView;
import de.unijena.cheminf.mortar.gui.MoleculesTab;
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.DataModel;
import de.unijena.cheminf.mortar.model.io.Importer;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Pagination;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;

import java.io.File;
import java.util.Objects;

/**
 * MainViewController
 * controls  {@link de.unijena.cheminf.mortar.gui.MainView}.
 *
 * @author Felix Baensch
 */
public class MainViewController {

    private Stage primaryStage;
    private MainView mainView;
    private String appDir;
    private Scene scene;
//    private IAtomContainerSet atomContainerSet;
    private MainTabPane mainTabPane;
    private ObservableList<DataModel> dataModelList;
    private DataTableView dataTableView;
    private int rowsPerPage;
    private boolean selectionAll;
    private boolean selectionAllCheckBoxAction;

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
        this.dataModelList = FXCollections.observableArrayList(param -> new Observable[]{param.selectionProperty()});
        this.primaryStage = aStage;
        this.mainView = aMainView;
        this.appDir = anAppDir;
        //<editor-fold desc="show MainView inside of the primaryStage" defaultstate="collapsed">
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

        this.rowsPerPage = 5;

        this.addListener();
    }

    /**
     *
     */
    private void addListener(){
        this.mainView.getMainMenuBar().getExitMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.closeApplication(0));
        this.mainView.getMainMenuBar().getLoadMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.loadMoleculeFile(this.primaryStage));
        //TODO: More implementation needed
    }

    /**
     * Closes application
     */
    private void closeApplication(int aStatus) {
        Platform.exit();
        System.exit(aStatus);
    }
    //

    /**
     *
     *
     * @param aParentStage
     */
    private void loadMoleculeFile(Stage aParentStage) {
        Importer tmpImporter = new Importer();
        IAtomContainerSet tmpAtomContainerSet = tmpImporter.Import(aParentStage);
        if(tmpAtomContainerSet == null || tmpAtomContainerSet.isEmpty())
            return;
        for (IAtomContainer tmpAtomContainer : tmpAtomContainerSet.atomContainers()) {
            DataModel tmpDataModel = new DataModel(tmpAtomContainer.getID(), tmpAtomContainer);
            tmpDataModel.setName(tmpAtomContainer.getProperty("NAME"));
            this.dataModelList.add(tmpDataModel);
        }
        this.OpenMoleculesTab();
    }

    /**
     *
     */
    private void OpenMoleculesTab() {
        MoleculesTab tmpMoleculesTab = new MoleculesTab();
        this.mainTabPane.getTabs().add(tmpMoleculesTab);
        this.dataTableView = new DataTableView();
        Pagination tmpPagination = new Pagination((this.dataModelList.size() / rowsPerPage + 1), 0);
        tmpPagination.setPageFactory(this::createDataTableViewPage);
        tmpMoleculesTab.setContent(tmpPagination);
    }

    /**
     *
     *
     * @param aPageIndex
     * @return
     */
    private Node createDataTableViewPage(int aPageIndex){
        int tmpFromIndex = aPageIndex * this.rowsPerPage;
        int tmpToIndex = Math.min(tmpFromIndex + this.rowsPerPage, this.dataModelList.size());
        this.dataTableView.getSelectAllCheckBox().setOnAction(event -> {
            this.selectionAllCheckBoxAction = true;
            for (int i = 0; i < this.dataModelList.size(); i++) {
                if(this.dataTableView.getSelectAllCheckBox().isSelected()){
                    this.dataModelList.get(i).setSelection(true);
                }
                else if(!this.dataTableView.getSelectAllCheckBox().isSelected()){
                    this.dataModelList.get(i).setSelection(false);
                }
            }
            this.selectionAllCheckBoxAction = false;
        });
        this.dataModelList.addListener((ListChangeListener) change ->{
            if(this.selectionAllCheckBoxAction){
                // No further action needed with column checkbox data when the select all checkbox is operated on
                return;
            }
            while(change.next()){
                if(change.wasUpdated()){
                    int checked = 0;
                    for(DataModel tmpDataModel : this.dataModelList){
                        if(tmpDataModel.isSelected())
                            checked++;
                    }
                    if(checked == this.dataModelList.size()){
                        this.dataTableView.getSelectAllCheckBox().setSelected(true);
                        this.dataTableView.getSelectAllCheckBox().setIndeterminate(false);
                    }
                    else if(checked == 0){
                        this.dataTableView.getSelectAllCheckBox().setSelected(false);
                        this.dataTableView.getSelectAllCheckBox().setIndeterminate(false);
                    }
                    else if(checked > 0){
                        this.dataTableView.getSelectAllCheckBox().setSelected(false);
                        this.dataTableView.getSelectAllCheckBox().setIndeterminate(true);
                    }
                }
            }
        });

        this.dataTableView.setItems(FXCollections.observableArrayList(this.dataModelList.subList(tmpFromIndex, tmpToIndex)));
        return new BorderPane(this.dataTableView);
    }
}
