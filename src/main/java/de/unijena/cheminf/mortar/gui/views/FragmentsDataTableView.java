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

package de.unijena.cheminf.mortar.gui.views;

import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

/**
 + FragmentsDataTableView extends TableView
 * Customized table view for fragments data table view
 */
public class FragmentsDataTableView extends TableView {

    //<editor-fold desc="private class variables", defaultstate="collapsed">
    private TableColumn<FragmentDataModel, Image> structureColumn;
    private TableColumn<FragmentDataModel, String> smilesColumn;
    private TableColumn<FragmentDataModel, Integer> frequencyColumn;
    private TableColumn<FragmentDataModel, Double> percentageColumn;
    private TableColumn<FragmentDataModel, Integer> moleculeFrequencyColumn;
    private TableColumn<FragmentDataModel, Double> moleculePercentageColumn;
    private List<FragmentDataModel> fragmentDataModelList;
    //</editor-fold>
    //
    /**
     * Constructor
     */
    public FragmentsDataTableView(){
        super();
        this.setEditable(false);
        DecimalFormat tmpDecForm = new DecimalFormat("#.###");
        //-structureColumn
        this.structureColumn = new TableColumn<>(Message.get("MainTabPane.fragmentsTab.tableView.structureColumn.header"));
        this.structureColumn.setMinWidth(150);
        this.structureColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.3) //TODO
        );
        this.structureColumn.setResizable(true);
        this.structureColumn.setEditable(false);
        this.structureColumn.setSortable(true);
        this.structureColumn.setCellValueFactory(new PropertyValueFactory("structure"));
        this.structureColumn.setStyle("-fx-alignment: CENTER");
        this.getColumns().add(this.structureColumn);
        //-smilesColumn
        this.smilesColumn = new TableColumn<>(Message.get("MainTabPane.fragmentsTab.tableView.smilesColumn.header"));
        this.smilesColumn.setMinWidth(50);
        this.smilesColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.3)  //TODO
        );
        this.smilesColumn.setResizable(true);
        this.smilesColumn.setEditable(false);
        this.smilesColumn.setSortable(true);
        this.smilesColumn.setCellValueFactory(new PropertyValueFactory("uniqueSmiles"));
        this.smilesColumn.setStyle("-fx-alignment: CENTER");
        this.getColumns().add(this.smilesColumn);
        //-frequencyColumn
        this.frequencyColumn = new TableColumn<>(Message.get("MainTabPane.fragmentsTab.tableView.frequencyColumn.header"));
        this.frequencyColumn.setMinWidth(50);
        this.frequencyColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.1) //TODO
        );
        this.frequencyColumn.setResizable(true);
        this.frequencyColumn.setEditable(false);
        this.frequencyColumn.setSortable(true);
        this.frequencyColumn.setCellValueFactory(new PropertyValueFactory("absoluteFrequency"));
        this.frequencyColumn.setStyle("-fx-alignment: CENTER");
        this.getColumns().add(this.frequencyColumn);
        //-percentageColumn
        this.percentageColumn = new TableColumn<>(Message.get("MainTabPane.fragmentsTab.tableView.percentageColumn.header"));
        this.percentageColumn.setMinWidth(20);
        this.percentageColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.1) //TODO
        );
        this.percentageColumn.setResizable(true);
        this.percentageColumn.setEditable(false);
        this.percentageColumn.setSortable(true);
        this.percentageColumn.setCellValueFactory(new PropertyValueFactory("absolutePercentage"));
        this.percentageColumn.setCellFactory(tc -> new TableCell<>(){
            @Override
            protected void updateItem(Double value, boolean empty){
                super.updateItem(value, empty);
                if(empty){
                    setText(null);
                } else{
                    setText(tmpDecForm.format(value));
                }
            }
        });
        this.percentageColumn.setStyle("-fx-alignment: CENTER");
        this.getColumns().add(this.percentageColumn);
        //-frequencyColumn
        this.moleculeFrequencyColumn = new TableColumn<>(Message.get("MainTabPane.fragmentsTab.tableView.moleculeFrequencyColumn.header"));
        this.moleculeFrequencyColumn.setMinWidth(50);
        this.moleculeFrequencyColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.1) //TODO
        );
        this.moleculeFrequencyColumn.setResizable(true);
        this.moleculeFrequencyColumn.setEditable(false);
        this.moleculeFrequencyColumn.setSortable(true);
        this.moleculeFrequencyColumn.setCellValueFactory(new PropertyValueFactory("moleculeFrequency"));
        this.moleculeFrequencyColumn.setStyle("-fx-alignment: CENTER");
        this.getColumns().add(this.moleculeFrequencyColumn);
        //-percentageColumn
        this.moleculePercentageColumn = new TableColumn<>(Message.get("MainTabPane.fragmentsTab.tableView.moleculePercentageColumn.header"));
        this.moleculePercentageColumn.setMinWidth(20);
        this.moleculePercentageColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.1) //TODO
        );
        this.moleculePercentageColumn.setResizable(true);
        this.moleculePercentageColumn.setEditable(false);
        this.moleculePercentageColumn.setSortable(true);
        this.moleculePercentageColumn.setCellValueFactory(new PropertyValueFactory("moleculePercentage"));
        this.moleculePercentageColumn.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(tmpDecForm.format(value));
                }
            }
        });
        this.moleculePercentageColumn.setStyle("-fx-alignment: CENTER");
        this.getColumns().add(this.moleculePercentageColumn);
    }
    //
    //<editor-fold desc="properties" defaultstate="collapsed">
    public TableColumn getStructureColumn() { return this.structureColumn; }
    public TableColumn getSmilesColumn() { return this.smilesColumn; }
    public TableColumn getFrequencyColumn() { return this.frequencyColumn; }
    public TableColumn getPercentageColumn() { return this.percentageColumn; }
    public TableColumn getMoleculeFrequencyColumn() { return this.moleculeFrequencyColumn; }
    public TableColumn getMoleculePercentageColumn() { return this.moleculePercentageColumn; }
    public List<FragmentDataModel> getFragmentDataModelList() { return this.fragmentDataModelList; }
    public void setFragmentDataModelList(List<FragmentDataModel> aListOfFragments){
        this.fragmentDataModelList = aListOfFragments;
    }
    //</editor-fold>
}
