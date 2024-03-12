/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2024  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
 *
 * Source code is available at <https://github.com/FelixBaensch/MORTAR>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.unijena.cheminf.mortar.gui.views;

import de.unijena.cheminf.mortar.configuration.IConfiguration;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.DataModelPropertiesForTableView;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

import java.util.List;

/**
 * Custom table view for the itemization table view.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
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
    /**
     * ContextMenu ot the TableView
     */
    private ContextMenu contextMenu;
    /**
     * MenuItem of ContextMenu to copy selected cell to clipboard
     */
    private MenuItem copyMenuItem;
    /**
     * MenuItem of ContextMenu to open an overview view with the item and its fragments
     */
    private MenuItem overviewViewMenuItem;
    /**
     * Configuration class to read resource file paths from.
     */
    private final IConfiguration configuration;
    //</editor-fold>
    //
    /**
     * Constructor.
     *
     * @param aFragmentationName String of fragmentation name used as title
     * @param aConfiguration configuration instance to read resource file paths from
     */
    public ItemizationDataTableView(String aFragmentationName, IConfiguration aConfiguration) {
        super();
        this.configuration = aConfiguration;
        this.setEditable(false);
        this.fragmentationName = aFragmentationName;
        this.getSelectionModel().setCellSelectionEnabled(true);
        //-nameColumn
        this.nameColumn = new TableColumn<>(Message.get("MainTabPane.itemizationTab.tableView.nameColumn.header"));
        this.nameColumn.setMinWidth(100); //magic number
        this.nameColumn.setResizable(true);
        this.nameColumn.setEditable(false);
        this.nameColumn.setSortable(true);
        this.nameColumn.setCellValueFactory(new PropertyValueFactory<>(DataModelPropertiesForTableView.NAME.getText()));
        this.nameColumn.setCellFactory(TextFieldTableCell.<MoleculeDataModel>forTableColumn());
        this.nameColumn.setStyle("-fx-alignment: CENTER");
        this.nameColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.15) //magic number
        );
        //-moleculeStructureColumn
        this.moleculeStructureColumn = new TableColumn<>(Message.get("MainTabPane.itemizationTab.tableView.moleculeStructureColumn.header"));
        this.moleculeStructureColumn.setMinWidth(300); //magic number
        this.moleculeStructureColumn.setResizable(true);
        this.moleculeStructureColumn.setEditable(false);
        this.moleculeStructureColumn.setSortable(false);
        this.moleculeStructureColumn.setCellValueFactory(new PropertyValueFactory(DataModelPropertiesForTableView.STRUCTURE.getText()));
        this.moleculeStructureColumn.setStyle("-fx-alignment: CENTER");
        this.moleculeStructureColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.3) //magic number
        );
        //-fragmentStructureColumn
        this.fragmentStructureColumn = new TableColumn<>(Message.get("MainTabPane.itemizationTab.tableView.fragmentsColumn.header"));
        this.fragmentStructureColumn.setResizable(true);
        this.fragmentStructureColumn.setEditable(false);
        this.fragmentStructureColumn.setSortable(false);
        this.fragmentStructureColumn.setStyle("-fx-alignment: CENTER");
        //
        this.getColumns().addAll(this.nameColumn, this.moleculeStructureColumn, this.fragmentStructureColumn);
        //context menu
        this.contextMenu = new ContextMenu();
        this.setContextMenu(this.contextMenu);
        //-copyMenuItem
        this.copyMenuItem = new MenuItem(Message.get("TableView.contextMenu.copyMenuItem"));
        String tmpCopyIconURL = this.getClass().getClassLoader().getResource(
                this.configuration.getProperty("mortar.imagesFolder")
                        + this.configuration.getProperty("mortar.icon.copy.name")).toExternalForm();
        this.copyMenuItem.setGraphic(new ImageView(new Image(tmpCopyIconURL)));
        this.contextMenu.getItems().add(this.copyMenuItem);
        //-separatorMenuItem
        this.contextMenu.getItems().add(new SeparatorMenuItem());
        //-overviewViewMenuItem
        this.overviewViewMenuItem = new MenuItem(Message.get("TableView.contextMenu.itemsTab.overviewViewMenuItem"));
        this.contextMenu.getItems().add(this.overviewViewMenuItem);
    }
    //
    //<editor-fold desc="public methods" defaultstate="collapsed">
    /**
     * Creates and returns an itemization tableview page.
     *
     * @param aPageIndex integer value for the page index
     * @param aFragmentationName String for unique name of fragmentation job
     * @param aSettingsContainer SettingsContainer
     * @return Node BorderPane which holds TableView as page for Pagination
     */
    public Node createItemizationTableViewPage(int aPageIndex, String aFragmentationName, SettingsContainer aSettingsContainer){
        int tmpRowsPerPage = aSettingsContainer.getRowsPerPageSetting();
        int fromIndex = aPageIndex * tmpRowsPerPage;
        int toIndex = Math.min(fromIndex + tmpRowsPerPage, this.itemsList.size());
        int tmpItemAmount = GuiUtil.getLargestNumberOfFragmentsForGivenMoleculeListAndFragmentationName(this.itemsList.subList(fromIndex, toIndex), aFragmentationName);
        this.resetFragmentStructureColumns(tmpItemAmount);
        List<MoleculeDataModel> tmpList = this.itemsList.subList(fromIndex, toIndex);
        for (MoleculeDataModel tmpMoleculeDataModel : tmpList) {
            tmpMoleculeDataModel.setStructureImageWidth(this.moleculeStructureColumn.getWidth());
        }
        this.setItems(FXCollections.observableArrayList(this.itemsList.subList(fromIndex, toIndex)));
        this.scrollTo(0);
        return new BorderPane(this);
    }
    //
    /**
     * Adds a change listener to the height property of table view which sets the height for structure images to
     * each MoleculeDataModel object of the items list and refreshes the table view.
     * If image height is too small it will be set to GuiDefinitions.GUI_STRUCTURE_IMAGE_MIN_HEIGHT (50.0).
     *
     * @param aSettingsContainer SettingsContainer
     */
    public void addTableViewHeightListener(SettingsContainer aSettingsContainer){
        this.heightProperty().addListener((observable, oldValue, newValue) -> {
            GuiUtil.setImageStructureHeight(this, newValue.doubleValue(),aSettingsContainer.getRowsPerPageSetting());
            this.refresh();
        });
    }
    //</editor-fold>
    //
    //<editor-fold desc="private methods" defaultstate="collapsed">
    /**
     * Deletes the old columns of the fragment structures and adds new ones for the given number.
     *
     * @param anItemAmount int value for the number of columns to add
     */
    private void resetFragmentStructureColumns(int anItemAmount) {
        this.fragmentStructureColumn.getColumns().clear();
        for (int i = 0; i < anItemAmount; i++) {
            int tmpIndex = i;
            TableColumn<MoleculeDataModel, ImageView> tmpColumn = new TableColumn<>("Fragment " + (i + 1)); //+1 to avoid 0 in GUI
            tmpColumn.setResizable(true);
            tmpColumn.setEditable(false);
            tmpColumn.setSortable(false);
            tmpColumn.setStyle( "-fx-alignment: CENTER;");
            tmpColumn.setCellValueFactory(cellData -> Bindings.createObjectBinding(() -> {
                if (!cellData.getValue().hasMoleculeUndergoneSpecificFragmentation(this.fragmentationName)) {
                    return null;
                }
                if (tmpIndex >= cellData.getValue().getFragmentsOfSpecificAlgorithm(this.fragmentationName).size()) {
                    return null;
                }
                FragmentDataModel tmpFragment = cellData.getValue().getFragmentsOfSpecificAlgorithm(this.fragmentationName).get(tmpIndex);
                if (!cellData.getValue().hasMoleculeUndergoneSpecificFragmentation(this.fragmentationName)) {
                    return null;
                }
                String tmpFrequency = cellData.getValue().getFragmentFrequencyOfSpecificAlgorithm(this.fragmentationName).get(tmpFragment.getUniqueSmiles()).toString();
                return tmpFragment.getStructureWithText(tmpFrequency);
            }));
            tmpColumn.setMinWidth(300);
            this.fragmentStructureColumn.getColumns().add(tmpColumn);
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="public properties" defaultstate="collapsed">
    /**
     * Returns TableColumn for the 2D structure of the molecule.
     *
     * @return TableColumn for 2D structure
     */
    public TableColumn<MoleculeDataModel, Image> getMoleculeStructureColumn() {
        return this.moleculeStructureColumn;
    }
    //
    /**
     * Returns name of fragmentation.
     *
     * @return String
     */
    public String getFragmentationName(){
        return this.fragmentationName;
    }
    //
    /**
     * Returns list of items shown in TableView.
     *
     * @return List {@literal <}MoleculeDataModel {@literal >}
     */
    public List<MoleculeDataModel> getItemsList() { return this.itemsList; }
    //
    /**
     * Returns MenuItem to copy selected cell.
     *
     * @return MenuItem
     */
    public MenuItem getCopyMenuItem(){
        return this.copyMenuItem;
    }
    //
    /**
     * Returns MenuItem to open an overview view of an item and its fragments.
     *
     * @return MenuItem
     */
    public MenuItem getOverviewViewMenuItem() {
        return overviewViewMenuItem;
    }
    //
    /**
     * Sets items to list.
     *
     * @param aListOfFragments List {@literal <}MoleculeDataModel {@literal >}
     */
    public void setItemsList(List<MoleculeDataModel> aListOfFragments) {
        this.itemsList = aListOfFragments;
    }
    //</editor-fold>
}
