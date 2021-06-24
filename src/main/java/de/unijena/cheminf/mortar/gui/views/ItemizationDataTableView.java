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
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.util.List;

/**
 * Custom table view for the itemization table view
 *
 */
public class ItemizationDataTableView extends TableView implements IDataTableView{

    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * TableColumn for name of the molecule
     */
    private TableColumn<MoleculeDataModel, String> nameColumn;
    /**
     * TableColumn for 2D structure of the molecule
     */
    private TableColumn<MoleculeDataModel, Image> moleculeStructureColumn;
    /**
     * TableColumn for 2D structure of the fragments of the molecule with labelling of how often
     * the respective fragment occurs in the molecule. BorderPane is used to align text and image.
     */
    private TableColumn<MoleculeDataModel, BorderPane> fragmentStructureColumn;
    /**
     * Name of the fragmentation algorithm used
     */
    private String fragmentationName;
    /**
     * List which contains all items to be shown in this tableview not only the displayed ones for this page (Pagination)
     */
    private List<MoleculeDataModel> itemsList;
    //</editor-fold>
    //
    /**
     * Constructor
     *
     * @param anItemAmount max amount of fragments to be displayed in fragment
     */
    public ItemizationDataTableView(int anItemAmount, String aFragmentationName){
        super();
        this.setEditable(false);
        this.fragmentationName = aFragmentationName;
        //-nameColumn
        this.nameColumn = new TableColumn<>(Message.get("MainTabPane.itemizationTab.tableView.nameColumn.header"));
        this.nameColumn.setMinWidth(100);
        this.nameColumn.setResizable(true);
        this.nameColumn.setEditable(false);
        this.nameColumn.setSortable(false);
        this.nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        this.nameColumn.setCellFactory(TextFieldTableCell.<MoleculeDataModel>forTableColumn());
        this.nameColumn.setStyle("-fx-alignment: CENTER");
        this.nameColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.15) //TODO
        );
        //-moleculeStructureColumn
        this.moleculeStructureColumn = new TableColumn<>(Message.get("MainTabPane.itemizationTab.tableView.moleculeStructureColumn.header"));
        this.moleculeStructureColumn.setMinWidth(300);
        this.moleculeStructureColumn.setResizable(true);
        this.moleculeStructureColumn.setEditable(false);
        this.moleculeStructureColumn.setSortable(false);
        this.moleculeStructureColumn.setCellValueFactory(new PropertyValueFactory("structure"));
        this.moleculeStructureColumn.setStyle("-fx-alignment: CENTER");
        this.moleculeStructureColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.3) //TODO
        );
        //-fragmentStructureColumn
        this.fragmentStructureColumn = new TableColumn<>(Message.get("MainTabPane.itemizationTab.tableView.fragmentsColumn.header"));
        this.fragmentStructureColumn.setResizable(true);
        this.fragmentStructureColumn.setEditable(false);
        this.fragmentStructureColumn.setSortable(false);
        this.fragmentStructureColumn.setStyle("-fx-alignment: CENTER");
        for(int i = 0; i < anItemAmount; i++){
            int tmpIndex = i;
            TableColumn<MoleculeDataModel, BorderPane> tmpColumn = new TableColumn<>("Fragment " + (i + 1)); //+1 to avoid 0 in GUI
            tmpColumn.setCellValueFactory(cellData -> Bindings.createObjectBinding(() -> {
                if(tmpIndex >= cellData.getValue().getFragmentsOfSpecificAlgorithm(this.fragmentationName).size())
                    return null;
                FragmentDataModel tmpFragment = cellData.getValue().getFragmentsOfSpecificAlgorithm(this.fragmentationName).get(tmpIndex);
                String tmpFrequency = cellData.getValue().getFragmentFrequencyOfSpecificAlgorithm(this.fragmentationName).get(tmpFragment.getUniqueSmiles()).toString();
                ImageView tmpStructureImg = tmpFragment.getStructure();
                Text tmpFrequencyText = new Text(tmpFrequency);
                BorderPane tmpBorderPane = new BorderPane();
                HBox tmpHBox = new HBox(tmpFrequencyText);
                tmpHBox.setAlignment(Pos.BOTTOM_RIGHT);
                tmpBorderPane.setBottom(tmpHBox);
                tmpBorderPane.setCenter(tmpStructureImg);
                return tmpBorderPane;
            }));
            tmpColumn.setMinWidth(300);
            this.fragmentStructureColumn.getColumns().add(tmpColumn);
        }
        //
        this.getColumns().addAll(this.nameColumn, this.moleculeStructureColumn, this.fragmentStructureColumn);
    }

    public List<MoleculeDataModel> getItemsList() { return this.itemsList; }
    public void setItemsList(List<MoleculeDataModel> aListOfFragments) {
        this.itemsList = aListOfFragments;
    }
}
