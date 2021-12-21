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

import de.unijena.cheminf.mortar.gui.util.ExternalTool;
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.message.Message;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.io.InputStream;

public class AboutView extends AnchorPane {

    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * TableView to show ExternalTool properties
     */
    private TableView<ExternalTool> tableView;
    /**
     * Button to open log file directory
     */
    private Button logFileButton;
    /**
     * Button to open GitHub repository
     */
    private Button gitHubButton;
    /**
     * Button to close this view
     */
    private Button closeButton;
    /**
     * ImageView for application logo
     */
    private ImageView logoImageView;
    private GridPane gridPane;
    //</editor-fold>
    //
    /**
     * Constructor
     */
    public AboutView(){
        super();
        //borderPane
        BorderPane borderPane = new BorderPane();
        AboutView.setTopAnchor(borderPane, 0.0);
        AboutView.setRightAnchor(borderPane, 0.0);
        AboutView.setLeftAnchor(borderPane, 0.0);
        AboutView.setBottomAnchor(borderPane, 0.0);
        //borderPane bottom -> buttons
        HBox hBoxButtonsHBox = new HBox();
        hBoxButtonsHBox.setStyle("-fx-background-color: LightGrey");
        borderPane.setBottom(hBoxButtonsHBox);
        //-left side
        this.logFileButton = new Button(Message.get("AboutView.logFileButton.text"));
        this.logFileButton.setTooltip(new Tooltip(Message.get("AboutView.logFileButton.tooltip")));
        this.gitHubButton = new Button(Message.get("AboutView.gitHubButton.text"));
        this.gitHubButton.setTooltip(new Tooltip(Message.get("AboutView.gitHubButton.tooltip")));
        HBox hBoxLeftSideButtons = new HBox();
        hBoxLeftSideButtons.getChildren().addAll(this.logFileButton, this.gitHubButton);
        hBoxLeftSideButtons.setAlignment(Pos.CENTER_LEFT);
        hBoxLeftSideButtons.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        hBoxLeftSideButtons.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(hBoxLeftSideButtons, Priority.ALWAYS);
        hBoxButtonsHBox.getChildren().add(hBoxLeftSideButtons);
        //-right side
        this.closeButton = new Button(Message.get("AboutView.closeButton.text"));
        HBox hBoxRightSideButtons = new HBox();
        hBoxRightSideButtons.getChildren().addAll(this.closeButton);
        hBoxRightSideButtons.setAlignment(Pos.CENTER_RIGHT);
        hBoxRightSideButtons.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        hBoxRightSideButtons.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(hBoxRightSideButtons, Priority.ALWAYS);
        hBoxButtonsHBox.getChildren().add(hBoxRightSideButtons);
        //borderPane center -> splitPane
        SplitPane tmpSplitPane = new SplitPane();
        tmpSplitPane.setOrientation(Orientation.VERTICAL);
        borderPane.setCenter(tmpSplitPane);
        //-splitPane top -> gridPane for text and logo
        this.gridPane = new GridPane();
        VBox.setVgrow(this.gridPane, Priority.ALWAYS);
        HBox.setHgrow(this.gridPane, Priority.ALWAYS);

        ColumnConstraints tmpTextCol = new ColumnConstraints();
        tmpTextCol.prefWidthProperty().bind(
                this.gridPane.widthProperty().multiply(0.4975)
        );
        this.gridPane.getColumnConstraints().add(0, tmpTextCol);

        ColumnConstraints tmpLogoCol = new ColumnConstraints();
        tmpLogoCol.prefWidthProperty().bind(
                this.gridPane.widthProperty().multiply(0.5)
        );
        tmpLogoCol.setHalignment(HPos.CENTER);
        this.gridPane.getColumnConstraints().add(1, tmpLogoCol);
        this.gridPane.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        this.gridPane.setVgap(GuiDefinitions.GUI_SPACING_VALUE);
        this.gridPane.setHgap(GuiDefinitions.GUI_SPACING_VALUE);
        this.gridPane.setAlignment(Pos.TOP_LEFT);
        tmpSplitPane.getItems().add(0, this.gridPane);
        //text
        //-title
        Text tmpAppTitle = new Text(Message.get("AboutView.appTitle.text"));
        tmpAppTitle.setStyle("-fx-font-weight: bold");
        tmpAppTitle.setStyle("-fx-font-size: 24");
        this.gridPane.add(tmpAppTitle,0,0);
        HBox.setHgrow(tmpAppTitle, Priority.ALWAYS);
        VBox.setVgrow(tmpAppTitle, Priority.ALWAYS);
        //-version
        Text tmpVersion = new Text("Version " + Message.get("Version.text")); //TODO: find better place for version number then properties
        tmpVersion.setStyle("-fx-font-weight: bold");
        this.gridPane.add(tmpVersion, 0,1);
        //-copyright
        Text tmpCopyright = new Text(Message.get("AboutView.copyright.text"));
        this.gridPane.add(tmpCopyright, 0,2);
        //-license
        Text tmpLicense = new Text(Message.get("AboutView.license.text"));
        TitledPane tmpTitledPane = new TitledPane(Message.get("AboutView.license.title"), tmpLicense);
        tmpTitledPane.setExpanded(true);
        this.gridPane.add(tmpTitledPane,0,3);
        //-contact
        Text tmpContact = new Text(Message.get("AboutView.contact.text"));
        this.gridPane.add(tmpContact, 0,4);
        //-acknowledgement
        Text tmpAcknowledgment = new Text(Message.get("AboutView.acknowledgement.text"));
        this.gridPane.add(tmpAcknowledgment,0,5);
        //-image
        InputStream tmpImageInputStream = AboutView.class.getResourceAsStream("/de/unijena/cheminf/mortar/images/Mortar_Logo1.png");
        Double tmpImageSize = 495.3125; // magic number, do not touch
        Image tmpLogo = new Image(tmpImageInputStream,tmpImageSize,tmpImageSize, true,true );
        this.logoImageView = new ImageView(tmpLogo);
        this.gridPane.add(this.logoImageView, 1,0, 1, 6);
        GridPane.setHalignment(this.logoImageView, HPos.CENTER);
        GridPane.setValignment(this.logoImageView, VPos.CENTER);
        //-splitPane bottom -> tabPane for tools etc.
        TabPane tabPane = new TabPane();
        tabPane.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        tmpSplitPane.getItems().add(1, tabPane);
        Tab tmpToolsTab = new Tab(Message.get("AboutView.toolsTab.title.text"));
        tmpToolsTab.setClosable(false);
        tabPane.getTabs().add(tmpToolsTab);
        //-tableView
        this.tableView = new TableView();
        this.tableView.setEditable(false);
//        this.tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        //-name column
        TableColumn<ExternalTool, String> tmpNameCol = new TableColumn<>(Message.get("AboutView.toolsTable.nameHeader.text"));
        tmpNameCol.setCellValueFactory(new PropertyValueFactory("name"));
        tmpNameCol.setStyle("-fx-alignment: CENTER");
        tmpNameCol.prefWidthProperty().bind(
                this.tableView.widthProperty().multiply(0.3) //TODO
        );
        this.tableView.getColumns().add(tmpNameCol);
        //-name column
        TableColumn<ExternalTool, String> tmpVersionCol = new TableColumn<>(Message.get("AboutView.toolsTable.versionHeader.text"));
        tmpVersionCol.setCellValueFactory(new PropertyValueFactory("version"));
        tmpVersionCol.setStyle("-fx-alignment: CENTER");
        tmpVersionCol.prefWidthProperty().bind(
                this.tableView.widthProperty().multiply(0.1) //TODO
        );
        this.tableView.getColumns().add(tmpVersionCol);
        //-name column
        TableColumn<ExternalTool, String> tmpAuthorCol = new TableColumn<>(Message.get("AboutView.toolsTable.authorHeader.text"));
        tmpAuthorCol.setCellValueFactory(new PropertyValueFactory("author"));
        tmpAuthorCol.setStyle("-fx-alignment: CENTER");
        tmpAuthorCol.prefWidthProperty().bind(
                this.tableView.widthProperty().multiply(0.3) //TODO
        );
        this.tableView.getColumns().add(tmpAuthorCol);
        //-name column
        TableColumn<ExternalTool, String> tmpLicenseCol = new TableColumn<>(Message.get("AboutView.toolsTable.licenseHeader.text"));
        tmpLicenseCol.setCellValueFactory(new PropertyValueFactory("license"));
        tmpLicenseCol.setStyle("-fx-alignment: CENTER");
        tmpLicenseCol.prefWidthProperty().bind(
                this.tableView.widthProperty().multiply(0.2975) //TODO
        );
        this.tableView.getColumns().add(tmpLicenseCol);
        tmpToolsTab.setContent(this.tableView);
        //
        this.getChildren().add(borderPane);
    }
    //
    //<editor-fold desc="properties" defaultstate="collapsed">
    /**
     * Returns button to open log files directory
     *
     * @return button
     */
    public Button getLogFileButton(){
        return this.logFileButton;
    }
    //
    /**
     * Returns button to open GitHub repository
     *
     * @return button
     */
    public Button getGitHubButton(){
        return this.gitHubButton;
    }
    //
    /**
     * Returns button to close this view
     *
     * @return button
     */
    public Button getCloseButton(){
        return this.closeButton;
    }
    //
    /**
     * Returns the TableView which shows ExternalTool properties
     * @return TableView<ExternalTool>
     */
    public TableView<ExternalTool> getTableView(){
        return this.tableView;
    }
    //
    /**
     * Returns grid pane to hold application information and logo
     *
     * @return GridPane
     */
    public GridPane getGridPane() {
        return this.gridPane;
    }
    //
    /**
     * Returns the ImageView for the logo image
     *
     * @return ImageView
     */
    public ImageView getLogoImageView() {
        return this.logoImageView;
    }
    //
    /**
     * Sets given image to image vie
     */
    public void setLogoImageView(Image anImage){
        this.logoImageView.setImage(anImage);
    }
    //</editor-fold>
}
