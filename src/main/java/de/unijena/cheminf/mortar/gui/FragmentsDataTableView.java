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

package de.unijena.cheminf.mortar.gui;

import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;

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
    //</editor-fold>
    //
    /**
     * Constructor
     */
    public FragmentsDataTableView(){
        super();
        this.setEditable(false);
        //-structureColumn
        this.structureColumn = new TableColumn<>(Message.get("MainTabPane.fragmentsTab.tableView.structureColumn.header"));
        this.structureColumn.setMinWidth(150);
        this.structureColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.7) //TODO
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
                this.widthProperty().multiply(0.1)  //TODO
        );
        this.smilesColumn.setResizable(true);
        this.smilesColumn.setEditable(false);
        this.smilesColumn.setSortable(true);
        this.smilesColumn.setCellValueFactory(new PropertyValueFactory("smiles"));
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
        this.frequencyColumn.setCellValueFactory(new PropertyValueFactory("frequency"));
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
        this.percentageColumn.setCellValueFactory(new PropertyValueFactory("percentage"));
        this.percentageColumn.setStyle("-fx-alignment: CENTER");
        this.getColumns().add(this.percentageColumn);
    }
    //
    //<editor-fold desc="properties" defaultstate="collapsed">
    public TableColumn getStructureColumn() { return this.structureColumn; }
    public TableColumn getSmilesColumn() { return this.smilesColumn; }
    public TableColumn getFrequencyColumn() { return this.frequencyColumn; }
    public TableColumn getPercentageColumn() { return this.percentageColumn; }
    //</editor-fold>
}
