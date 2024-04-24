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

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Customized table view for fragments data table view.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class FragmentsDataTableView extends TableView implements IDataTableView{
    //<editor-fold desc="private final class constants", defaultstate="collapsed">
    /**
     * TableColumn for 2D structure state of the fragment.
     */
    private final TableColumn<FragmentDataModel, ImageView> structureColumn;
    /**
     * TableColumn for SMILES of the fragment.
     */
    private final TableColumn<FragmentDataModel, String> smilesColumn;
    /**
     * TableColumn for 2D structure state of one (random/first occurred) parent molecule.
     */
    private final TableColumn<FragmentDataModel, Image> parentMolColumn;
    /**
     * TableColumn for name of one (random/first occurred) parent molecule.
     */
    private final TableColumn<FragmentDataModel, String> parentMolNameColumn;
    /**
     * TableColumn for frequency of the fragment.
     */
    private final TableColumn<FragmentDataModel, Integer> frequencyColumn;
    /**
     * TableColumn for percentage frequency of the fragment.
     */
    private final TableColumn<FragmentDataModel, Double> percentageColumn;
    /**
     * TableColumn for the frequency in how many molecules this fragment occurs in.
     */
    private final TableColumn<FragmentDataModel, Integer> moleculeFrequencyColumn;
    /**
     * TableColumn for the percentage frequency in how many molecules this fragment occurs in.
     */
    private final TableColumn<FragmentDataModel, Double> moleculePercentageColumn;
    /**
     * MenuItem of ContextMenu to copy selected cell to clipboard.
     */
    private final MenuItem copyMenuItem;
    /**
     * MenuItem of ContextMenu to open an overview view with the parent molecules of the row of the selected cell.
     */
    private final MenuItem overviewViewMenuItem;
    /**
     * Configuration class to read resource file paths from.
     */
    private final IConfiguration configuration;
    //</editor-fold>
    //
    //<editor-fold desc="private class variables">
    /**
     * ContextMenu ot the TableView.
     */
    private final ContextMenu contextMenu;
    /**
     * List which contains all items to be shown in this tableview not only the displayed ones for this page (Pagination).
     */
    private List<MoleculeDataModel> itemsList;
    //</editor-fold>
    //
    /**
     * Constructor.
     *
     * @param aConfiguration configuration instance to read resource file paths from
     */
    public FragmentsDataTableView(IConfiguration aConfiguration){
        super();
        this.configuration = aConfiguration;
        this.setEditable(false);
        this.getSelectionModel().setCellSelectionEnabled(true);
        //activate for future bulk export?
        //this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        DecimalFormat tmpPercentageForm = new DecimalFormat("#.##%");
        //-structureColumn
        this.structureColumn = new TableColumn<>(Message.get("MainTabPane.fragmentsTab.tableView.structureColumn.header"));
        this.structureColumn.setMinWidth(150); //magic number
        this.structureColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.2)
        );
        this.structureColumn.setResizable(true);
        this.structureColumn.setEditable(false);
        this.structureColumn.setSortable(false);
        this.structureColumn.setCellValueFactory(new PropertyValueFactory(DataModelPropertiesForTableView.STRUCTURE.getText()));
        this.structureColumn.setStyle("-fx-alignment: CENTER");
        this.getColumns().add(this.structureColumn);
        //-smilesColumn
        this.smilesColumn = new TableColumn<>(Message.get("MainTabPane.fragmentsTab.tableView.smilesColumn.header"));
        this.smilesColumn.setMinWidth(50);
        this.smilesColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.075) //magic number
        );
        this.smilesColumn.setResizable(true);
        this.smilesColumn.setEditable(false);
        this.smilesColumn.setSortable(true);
        this.smilesColumn.setCellValueFactory(new PropertyValueFactory(DataModelPropertiesForTableView.UNIQUE_SMILES.getText()));
        this.smilesColumn.setCellFactory(tableColumn ->{
            TableCell<FragmentDataModel, String> tmpCell = new TableCell<>();
            Text tmpText = new Text();
            tmpText.setTextAlignment(TextAlignment.CENTER);
            tmpCell.setGraphic(tmpText);
            tmpCell.setPrefHeight(Region.USE_COMPUTED_SIZE);
            tmpText.wrappingWidthProperty().bind(this.smilesColumn.widthProperty());
            tmpText.textProperty().bind(tmpCell.itemProperty());
            return tmpCell;
        });
        this.smilesColumn.setStyle("-fx-alignment: CENTER");
        this.getColumns().add(this.smilesColumn);
        //-parentMolNameColumn
        this.parentMolNameColumn = new TableColumn<>();
        Label tmpParentNameLabel = new Label(Message.get("MainTabPane.fragmentsTab.tableView.parentMolNameColumn.header"));
        tmpParentNameLabel.setTooltip(GuiUtil.createTooltip(Message.get("MainTabPane.fragmentsTab.tableView.parentMolNameColumn.tooltip")));
        this.parentMolNameColumn.setGraphic(tmpParentNameLabel);
        this.parentMolNameColumn.setMinWidth(50);
        this.parentMolNameColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.075) //magic number
        );
        this.parentMolNameColumn.setResizable(true);
        this.parentMolNameColumn.setEditable(false);
        this.parentMolNameColumn.setSortable(true);
        this.parentMolNameColumn.setCellValueFactory(new PropertyValueFactory(DataModelPropertiesForTableView.PARENT_MOLECULE_NAME.getText()));
        this.parentMolNameColumn.setCellFactory(tableColumn ->{
            TableCell<FragmentDataModel, String> tmpCell = new TableCell<>();
            Text tmpText = new Text();
            tmpText.setTextAlignment(TextAlignment.CENTER);
            tmpCell.setGraphic(tmpText);
            tmpCell.setPrefHeight(Region.USE_COMPUTED_SIZE);
            tmpText.wrappingWidthProperty().bind(this.parentMolNameColumn.widthProperty());
            tmpText.textProperty().bind(tmpCell.itemProperty());
            return tmpCell;
        });
        this.parentMolNameColumn.setStyle("-fx-alignment: CENTER");
        this.getColumns().add(this.parentMolNameColumn);
        //-parentMolColumn
        this.parentMolColumn = new TableColumn<>();
        Label tmpParentMolLabel = new Label(Message.get("MainTabPane.fragmentsTab.tableView.parentMolColumn.header"));
        tmpParentMolLabel.setTooltip(GuiUtil.createTooltip(Message.get("MainTabPane.fragmentsTab.tableView.parentMolColumn.tooltip")));
        this.parentMolColumn.setGraphic(tmpParentMolLabel);
        this.parentMolColumn.setMinWidth(150); //magic number
        this.parentMolColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.2475) //magic number
        );
        this.parentMolColumn.setResizable(true);
        this.parentMolColumn.setEditable(false);
        this.parentMolColumn.setSortable(false);
        this.parentMolColumn.setCellValueFactory(new PropertyValueFactory(DataModelPropertiesForTableView.PARENT_MOLECULE_STRUCTURE.getText()));
        this.parentMolColumn.setStyle("-fx-alignment: CENTER");
        this.getColumns().add(this.parentMolColumn);
        //-frequencyColumn
        this.frequencyColumn = new TableColumn<>(Message.get("MainTabPane.fragmentsTab.tableView.frequencyColumn.header"));
        this.frequencyColumn.setMinWidth(50); //magic number
        this.frequencyColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.1) //magic number
        );
        this.frequencyColumn.setResizable(true);
        this.frequencyColumn.setEditable(false);
        this.frequencyColumn.setSortable(true);
        this.frequencyColumn.setCellValueFactory(new PropertyValueFactory(DataModelPropertiesForTableView.ABSOLUTE_FREQUENCY.getText()));
        this.frequencyColumn.setStyle("-fx-alignment: CENTER-RIGHT");
        this.getColumns().add(this.frequencyColumn);
        //-percentageColumn
        this.percentageColumn = new TableColumn<>(Message.get("MainTabPane.fragmentsTab.tableView.percentageColumn.header"));
        this.percentageColumn.setMinWidth(20); //magic number
        this.percentageColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.1) //magic number
        );
        this.percentageColumn.setResizable(true);
        this.percentageColumn.setEditable(false);
        this.percentageColumn.setSortable(true);
        this.percentageColumn.setCellValueFactory(new PropertyValueFactory(DataModelPropertiesForTableView.ABSOLUTE_PERCENTAGE.getText()));
        this.percentageColumn.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty){
                super.updateItem(value, empty);
                if (empty) {
                    this.setText(null);
                } else {
                    this.setText(tmpPercentageForm.format(value));
                }
            }
        });
        this.percentageColumn.setStyle("-fx-alignment: CENTER-RIGHT");
        this.getColumns().add(this.percentageColumn);
        //-moleculeFrequencyColumn
        this.moleculeFrequencyColumn = new TableColumn<>();
        Label tmpMolFrequencyLabel = new Label(Message.get("MainTabPane.fragmentsTab.tableView.moleculeFrequencyColumn.header"));
        tmpMolFrequencyLabel.setTooltip(GuiUtil.createTooltip(Message.get("MainTabPane.fragmentsTab.tableView.moleculeFrequencyColumn.tooltip")));
        this.moleculeFrequencyColumn.setGraphic(tmpMolFrequencyLabel);
        this.moleculeFrequencyColumn.setMinWidth(50); //magic number
        this.moleculeFrequencyColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.1) //magic number
        );
        this.moleculeFrequencyColumn.setResizable(true);
        this.moleculeFrequencyColumn.setEditable(false);
        this.moleculeFrequencyColumn.setSortable(true);
        this.moleculeFrequencyColumn.setCellValueFactory(new PropertyValueFactory(DataModelPropertiesForTableView.MOLECULE_FREQUENCY.getText()));
        this.moleculeFrequencyColumn.setStyle("-fx-alignment: CENTER-RIGHT");
        this.getColumns().add(this.moleculeFrequencyColumn);
        //-moleculePercentageColumn
        this.moleculePercentageColumn = new TableColumn<>();
        Label tmpMolPercentageLabel = new Label(Message.get("MainTabPane.fragmentsTab.tableView.moleculePercentageColumn.header"));
        tmpMolPercentageLabel.setTooltip(GuiUtil.createTooltip(Message.get("MainTabPane.fragmentsTab.tableView.moleculePercentageColumn.tooltip")));
        this.moleculePercentageColumn.setGraphic(tmpMolPercentageLabel);
        this.moleculePercentageColumn.setMinWidth(20); //magic number
        this.moleculePercentageColumn.prefWidthProperty().bind(
                this.widthProperty().multiply(0.0975) //magic number
        );
        this.moleculePercentageColumn.setResizable(true);
        this.moleculePercentageColumn.setEditable(false);
        this.moleculePercentageColumn.setSortable(true);
        this.moleculePercentageColumn.setCellValueFactory(new PropertyValueFactory(DataModelPropertiesForTableView.MOLECULE_PERCENTAGE.getText()));
        this.moleculePercentageColumn.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    this.setText(null);
                } else {
                    this.setText(tmpPercentageForm.format(value));
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
        String tmpCopyIconURL = this.getClass().getClassLoader().getResource(
                this.configuration.getProperty("mortar.imagesFolder")
                        + this.configuration.getProperty("mortar.icon.copy.name")).toExternalForm();
        this.copyMenuItem.setGraphic(new ImageView(new Image(tmpCopyIconURL)));
        this.contextMenu.getItems().add(this.copyMenuItem);
        //-separatorMenuItem
        this.contextMenu.getItems().add(new SeparatorMenuItem());
        //-overviewViewMenuItem
        this.overviewViewMenuItem = new MenuItem(Message.get("TableView.contextMenu.fragmentsTab.overviewViewMenuItem"));
        this.contextMenu.getItems().add(this.overviewViewMenuItem);
    }
    //
    //<editor-fold desc="public methods" defaultstate="collapsed">
    /**
     * Creates and returns a fragments tableview page inside a BorderPane.
     *
     * @param aPageIndex int page index
     * @param aSettingsContainer SettingsContainer
     * @return Node BorderPane which holds TableView page
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
     * each MoleculeDataModel object of the items list and refreshes the table view.
     * If image height is too small it will be set to GuiDefinitions.GUI_STRUCTURE_IMAGE_MIN_HEIGHT (50.0).
     *
     * @param aSettingsContainer SettingsContainer
     */
    public void addTableViewHeightListener(SettingsContainer aSettingsContainer) {
        this.heightProperty().addListener((observable, oldValue, newValue) -> {
            GuiUtil.setImageStructureHeight(this, newValue.doubleValue(), aSettingsContainer.getRowsPerPageSetting());
            this.refresh();
        });
    }
    //</editor-fold>
    //
    //<editor-fold desc="properties" defaultstate="collapsed">
    /**
     * Returns the column that shows the 2d structure.
     *
     * @return TableColumn for 2d structure
     */
    public TableColumn<FragmentDataModel, ImageView> getStructureColumn() {
        return this.structureColumn;
    }
    //
    /**
     * Returns the column that hold the SMILES.
     *
     * @return TableColumn for SMILES
     */
    public TableColumn<FragmentDataModel, String> getSmilesColumn() {
        return this.smilesColumn;
    }
    //
    /**
     * Returns the column that shows the 2d structure for a parent molecule.
     *
     * @return TableColumn for 2d structure
     */
    public TableColumn<FragmentDataModel, Image> getParentMolColumn(){
        return this.parentMolColumn;
    }
    //
    /**
     * Returns the column that holds the name for a parent molecule.
     *
     * @return TableColumn
     */
    public TableColumn<FragmentDataModel, String> getParentMolNameColumn() {
        return this.parentMolNameColumn;
    }
    //
    /**
     * Returns the column that holds the frequency how often this fragment occurs.
     *
     * @return TableColumn
     */
    public TableColumn<FragmentDataModel, Integer> getFrequencyColumn() {
        return this.frequencyColumn;
    }
    //
    /**
     * Returns the column that holds the percentage frequency how often this fragment occurs.
     *
     * @return TableColumn
     */
    public TableColumn<FragmentDataModel, Double> getPercentageColumn() {
        return this.percentageColumn;
    }
    //
    /**
     * Returns the column that holds the frequency in how many molecules this fragment occurs.
     *
     * @return TableColumn
     */
    public TableColumn<FragmentDataModel, Integer> getMoleculeFrequencyColumn() {
        return this.moleculeFrequencyColumn;
    }
    //
    /**
     * Returns the column that holds the percentage frequency in how many molecules this fragment occurs.
     *
     * @return TableColumn
     */
    public TableColumn<FragmentDataModel, Double> getMoleculePercentageColumn() {
        return this.moleculePercentageColumn;
    }
    //
    /**
     * Returns the MenuItem to copy.
     *
     * @return MenuItem
     */
    public MenuItem getCopyMenuItem() {
        return this.copyMenuItem;
    }
    //
    /**
     * Returns the MenuItem to open the parent molecules overview view.
     *
     * @return MenuItem
     */
    public MenuItem getOverviewViewMenuItem() {
        return overviewViewMenuItem;
    }
    //
    /**
     * Returns the items as a list of {@link MoleculeDataModel} objects.
     *
     * @return List
     */
    public List<MoleculeDataModel> getItemsList() {
        return this.itemsList;
    }
    //
    /**
     * Sets the given list of {@link MoleculeDataModel} objects as items.
     *
     * @param aListOfFragments  list of fragments to set as items
     */
    public void setItemsList(List<MoleculeDataModel> aListOfFragments) {
        this.itemsList = aListOfFragments;
    }
    //</editor-fold>
}
