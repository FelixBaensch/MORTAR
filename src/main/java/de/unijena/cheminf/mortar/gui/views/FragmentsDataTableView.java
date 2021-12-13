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

package de.unijena.cheminf.mortar.gui.views;

import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.text.DecimalFormat;
import java.util.List;

/**
 + FragmentsDataTableView extends TableView
 * Customized table view for fragments data table view
 */
public class FragmentsDataTableView extends TableView implements IDataTableView{

    //<editor-fold desc="private class variables", defaultstate="collapsed">
    /**
     * TableColumn for 2D structure state of the fragment
     */
    private TableColumn<FragmentDataModel, ImageView> structureColumn;
    /**
     * TableColumn for SMILES of the fragment
     */
    private TableColumn<FragmentDataModel, String> smilesColumn;
    /**
     * TableColumn for 2D structure state of one (random/first occurred) parent molecule
     */
    private TableColumn<FragmentDataModel, Image> parentMolColumn;
    /**
     * TableColumn for name of one (random/first occurred) parent molecule
     */
    private TableColumn<FragmentDataModel, String> parentMolNameColumn;
    /**
     * TableColumn for frequency of the fragment
     */
    private TableColumn<FragmentDataModel, Integer> frequencyColumn;
    /**
     * TableColumn for percentage frequency of the fragment
     */
    private TableColumn<FragmentDataModel, Double> percentageColumn;
    /**
     * TableColumn for the frequency in how many molecules this fragment occurs in
     */
    private TableColumn<FragmentDataModel, Integer> moleculeFrequencyColumn;
    /**
     * TableColumn for the percentage frequency in how many molecules this fragment occurs in
     */
    private TableColumn<FragmentDataModel, Double> moleculePercentageColumn;
    /**
     * List which contains all items to be shown in this tableview not only the displayed ones for this page (Pagination)
     */
    private List<MoleculeDataModel> itemsList;
    /**
     * ContextMenu ot the TableView
     */
    private ContextMenu contextMenu;
    /**
     * MenuItem of ContextMenu to copy selected cell to clipboard
     */
    private MenuItem copyMenuItem;
    //</editor-fold>
    //
    /**
     * Constructor
     */
    public FragmentsDataTableView(){
        super();
        this.setEditable(false);
        this.getSelectionModel().setCellSelectionEnabled(true);
//        this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        DecimalFormat tmpPercentageForm = new DecimalFormat("#.##%");
        //-structureColumn
        this.structureColumn = new TableColumn<>(Message.get("MainTabPane.fragmentsTab.tableView.structureColumn.header"));
        this.structureColumn.setMinWidth(150);
        this.structureColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.2) //TODO
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
                this.widthProperty().multiply(0.075)  //TODO
        );
        this.smilesColumn.setResizable(true);
        this.smilesColumn.setEditable(false);
        this.smilesColumn.setSortable(true);
        this.smilesColumn.setCellValueFactory(new PropertyValueFactory("uniqueSmiles"));
        this.smilesColumn.setCellFactory(tableColumn ->{
            TableCell<FragmentDataModel, String> tmpCell = new TableCell<>();
            Text tmpText = new Text();
            tmpText.setTextAlignment(TextAlignment.CENTER);
            tmpCell.setGraphic(tmpText);
            tmpCell.setPrefHeight(Control.USE_COMPUTED_SIZE);
            tmpText.wrappingWidthProperty().bind(this.smilesColumn.widthProperty());
            tmpText.textProperty().bind(tmpCell.itemProperty());
            return tmpCell;
        });
        this.smilesColumn.setStyle("-fx-alignment: CENTER");
        this.getColumns().add(this.smilesColumn);
        //-parentMolColumn
        this.parentMolColumn = new TableColumn<>();
        Label tmpParentMolLabel = new Label(Message.get("MainTabPane.fragmentsTab.tableView.parentMolColumn.header"));
        tmpParentMolLabel.setTooltip(new Tooltip(Message.get("MainTabPane.fragmentsTab.tableView.parentMolColumn.tooltip")));
        this.parentMolColumn.setGraphic(tmpParentMolLabel);
        this.parentMolColumn.setMinWidth(150);
        this.parentMolColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.2475) //TODO
        );
        this.parentMolColumn.setResizable(true);
        this.parentMolColumn.setEditable(false);
        this.parentMolColumn.setSortable(true);
        this.parentMolColumn.setCellValueFactory(new PropertyValueFactory("parentMoleculeStructure"));
        this.parentMolColumn.setStyle("-fx-alignment: CENTER");
        this.getColumns().add(this.parentMolColumn);
        //-parentMolNameColumn
        this.parentMolNameColumn = new TableColumn<>();
        Label tmpParentNameLabel = new Label(Message.get("MainTabPane.fragmentsTab.tableView.parentMolNameColumn.header"));
        tmpParentNameLabel.setTooltip(new Tooltip(Message.get("MainTabPane.fragmentsTab.tableView.parentMolNameColumn.tooltip")));
        this.parentMolNameColumn.setGraphic(tmpParentNameLabel);
        this.parentMolNameColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.075)
        );
        this.parentMolNameColumn.setResizable(true);
        this.parentMolNameColumn.setEditable(false);
        this.parentMolNameColumn.setSortable(true);
        this.parentMolNameColumn.setCellValueFactory(new PropertyValueFactory("parentMoleculeName"));
        this.parentMolNameColumn.setStyle("-fx-alignment: CENTER");
        this.getColumns().add(this.parentMolNameColumn);
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
        this.frequencyColumn.setStyle("-fx-alignment: CENTER-RIGHT");
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
                    setText(tmpPercentageForm.format(value));
                }
            }
        });
        this.percentageColumn.setStyle("-fx-alignment: CENTER-RIGHT");
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
        this.moleculeFrequencyColumn.setStyle("-fx-alignment: CENTER-RIGHT");
        this.getColumns().add(this.moleculeFrequencyColumn);
        //-percentageColumn
        this.moleculePercentageColumn = new TableColumn<>(Message.get("MainTabPane.fragmentsTab.tableView.moleculePercentageColumn.header"));
        this.moleculePercentageColumn.setMinWidth(20);
        this.moleculePercentageColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.0975) //TODO
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
                    setText(tmpPercentageForm.format(value));
                }
            }
        });
        this.moleculePercentageColumn.setStyle("-fx-alignment: CENTER-RIGHT");
        this.getColumns().add(this.moleculePercentageColumn);
        //context menu
        this.contextMenu = new ContextMenu();
        this.setContextMenu(this.contextMenu);
        //-copyMenuItem
        this.copyMenuItem = new MenuItem(Message.get("TableView.contextMenu.copyMenuItem"));
        this.copyMenuItem.setGraphic(new ImageView(new Image("de/unijena/cheminf/mortar/images/copy_icon_16x16.png")));
        this.contextMenu.getItems().add(this.copyMenuItem);
    }
    //
    /**
     * Creates a fragments tableview page
     *
     * @param aPageIndex
     * @return
     */
    public Node createFragmentsTableViewPage(int aPageIndex, SettingsContainer aSettingsContainer) {
        int tmpRowsPerPage = aSettingsContainer.getRowsPerPageSetting();
        int fromIndex = aPageIndex * tmpRowsPerPage;
        int toIndex = Math.min(fromIndex + tmpRowsPerPage, this.itemsList.size());
        this.setItems(FXCollections.observableArrayList(this.itemsList.subList(fromIndex, toIndex)));
        this.scrollTo(0);
        return new BorderPane(this);
    }
    //
    /**
     * Adds a change listener to the height property of table view which sets the height for structure images to
     * each MoleculeDataModel object of the items list and refreshes the table view
     * If image height is too small it will be set to GuiDefinitions.GUI_STRUCTURE_IMAGE_MIN_HEIGHT (50.0)
     *
     * @param aSettingsContainer
     */
    public void addTableViewHeightListener(SettingsContainer aSettingsContainer){
        this.heightProperty().addListener((observable, oldValue, newValue) -> {
            GuiUtil.setImageStructureHeight(this, newValue.doubleValue(), aSettingsContainer);
            this.refresh();
        });
    }
    //
    //<editor-fold desc="properties" defaultstate="collapsed">
    public TableColumn<FragmentDataModel, ImageView> getStructureColumn() { return this.structureColumn; }
    public TableColumn<FragmentDataModel, String> getSmilesColumn() { return this.smilesColumn; }
    public TableColumn<FragmentDataModel, Image> getParentMolColumn(){ return this.parentMolColumn; }
    public TableColumn<FragmentDataModel, String> getParentMolNameColumn() { return this.parentMolNameColumn; }
    public TableColumn<FragmentDataModel, Integer> getFrequencyColumn() { return this.frequencyColumn; }
    public TableColumn<FragmentDataModel, Double> getPercentageColumn() { return this.percentageColumn; }
    public TableColumn<FragmentDataModel, Integer> getMoleculeFrequencyColumn() { return this.moleculeFrequencyColumn; }
    public TableColumn<FragmentDataModel, Double> getMoleculePercentageColumn() { return this.moleculePercentageColumn; }
    public MenuItem getCopyMenuItem(){
        return this.copyMenuItem;
    }
    public List<MoleculeDataModel> getItemsList() { return this.itemsList; }
    public void setItemsList(List<MoleculeDataModel> aListOfFragments) {
        this.itemsList = aListOfFragments;
    }
    //</editor-fold>
}
