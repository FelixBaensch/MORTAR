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

import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;

/**
 * MoleculesDataTableView extends TableView
 * Customized table view for molecules data table view
 */
public class MoleculesDataTableView extends TableView {
    //
    //<editor-fold desc="private class variables" defaultstate="collapsed">
    private TableColumn<MoleculeDataModel, Boolean> selectionColumn;
    private TableColumn<MoleculeDataModel, String> nameColumn;
    private TableColumn<MoleculeDataModel, Image> structureColumn;
    private CheckBox selectAllCheckBox;
    //</editor-fold>
    //
    /**
     * Constructor
     */
    public MoleculesDataTableView(){
        super();
        this.setEditable(true);
        //-selectionColumn
        this.selectionColumn = new TableColumn<>();
        this.selectAllCheckBox = new CheckBox();
        this.selectAllCheckBox.setAllowIndeterminate(true);
        this.selectAllCheckBox.setSelected(true);
        this.selectionColumn.setGraphic(this.selectAllCheckBox);
        this.selectionColumn.setMinWidth(GuiDefinitions.GUI_MOLECULESTAB_TABLEVIEW_SELECTIONCOLUMN_WIDTH);
        this.selectionColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.05)
        );
        this.selectionColumn.setResizable(false);
        this.selectionColumn.setEditable(true);
        this.selectionColumn.setSortable(false);
        this.selectionColumn.setCellValueFactory(new PropertyValueFactory<>("selection"));
        this.selectionColumn.setCellFactory(tc -> new CheckBoxTableCell<>());
        //-nameColumn
        this.nameColumn = new TableColumn<>(Message.get("MainTabPane.moleculesTab.tableView.nameColumn.header"));
        this.nameColumn.setMinWidth(150);
        this.nameColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.15)
        );
        this.nameColumn.setResizable(true);
        this.nameColumn.setEditable(false);
        this.nameColumn.setSortable(false);
        this.nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        this.nameColumn.setCellFactory(TextFieldTableCell.<MoleculeDataModel>forTableColumn());
        this.nameColumn.setStyle("-fx-alignment: CENTER");
        //-structureColumn
        this.structureColumn = new TableColumn<>(Message.get("MainTabPane.moleculesTab.tableView.structureColumn.header"));
        this.structureColumn.setMinWidth(150);
        this.structureColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.8)
        );
        this.structureColumn.setResizable(true);
        this.structureColumn.setEditable(false);
        this.structureColumn.setSortable(false);
        this.structureColumn.setCellValueFactory(new PropertyValueFactory("structure"));
        this.structureColumn.setStyle("-fx-alignment: CENTER");
        //
        this.getColumns().addAll(this.selectionColumn, this.nameColumn, this.structureColumn);
    }
    //
    //<editor-fold desc="properties" defaulstate="collapsed">
    public TableColumn getSelectionColumn(){
        return this.selectionColumn;
    }
    public TableColumn getNameColumn(){
        return this.nameColumn;
    }
    public TableColumn getStructureColumn() {
        return this.structureColumn;
    }
    public CheckBox getSelectAllCheckBox() { return this.selectAllCheckBox; }
    //</editor-fold>
}
