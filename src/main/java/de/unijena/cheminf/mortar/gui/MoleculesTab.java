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

import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.DataModel;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;

public class MoleculesTab extends Tab {

    private TableView tableView;
    private TableColumn<DataModel, Boolean> selectionColumn;
    private TableColumn<DataModel, String> nameColumn;
    private TableColumn<DataModel, Image> structureColumn;

    public MoleculesTab(){
        super(Message.get("MainTabPane.moleculesTab.title"));
        this.setClosable(false);
        this.tableView = new TableView();
        this.tableView.setEditable(true);
        //selectionColumn
        this.selectionColumn = new TableColumn<>(Message.get("MainTabPane.moleculesTab.tableView.selectionColumn.header"));
        this.selectionColumn.setMinWidth(GuiDefinitions.GUI_MOLECULESTAB_TABLEVIEW_SELECTIONCOLUMN_WIDTH);
        this.selectionColumn.prefWidthProperty().bind(
                this.tableView.widthProperty().multiply(0.05)
        );
        this.selectionColumn.setResizable(false);
        this.selectionColumn.setEditable(true);
        this.selectionColumn.setSortable(false);
        //nameColumn
        this.nameColumn = new TableColumn<>(Message.get("MainTabPane.moleculesTab.tableView.nameColumn.header"));
        this.nameColumn.setMinWidth(150);
        this.nameColumn.prefWidthProperty().bind(
                        this.tableView.widthProperty().multiply(0.15)
        );
        this.nameColumn.setResizable(true);
        this.nameColumn.setEditable(false);
        this.nameColumn.setSortable(false);
        //structureColumn
        this.structureColumn = new TableColumn<>(Message.get("MainTabPane.moleculesTab.tableView.structureColumn.header"));
        this.structureColumn.setMinWidth(150);
        this.structureColumn.prefWidthProperty().bind(
               this.tableView.widthProperty().multiply(0.8)
        );
        this.structureColumn.setResizable(true);
        this.structureColumn.setEditable(false);
        this.structureColumn.setSortable(false);
        //
        this.tableView.getColumns().addAll(this.selectionColumn, this.nameColumn, this.structureColumn);
        this.setContent(this.tableView);
    }

    public TableView getTableView(){
        return this.tableView;
    }
    public TableColumn getSelectionColumn(){
        return this.selectionColumn;
    }
    public TableColumn getNameColumn(){
        return this.nameColumn;
    }
    public TableColumn getStructureColumn() {
        return structureColumn;
    }
}
