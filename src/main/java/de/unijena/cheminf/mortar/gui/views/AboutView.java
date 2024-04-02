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
import de.unijena.cheminf.mortar.gui.util.ExternalTool;
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
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

/**
 * "About" window view.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class AboutView extends AnchorPane {
    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * TableView to show ExternalTool properties.
     */
    private final TableView<ExternalTool> tableView;
    /**
     * Button to open log file directory.
     */
    private final Button logFileButton;
    /**
     * Button to open GitHub repository.
     */
    private final Button gitHubButton;
    /**
     * Button to close this view.
     */
    private final Button closeButton;
    /**
     * Button to open tutorial pdf.
     */
    private final Button tutorialButton;
    /**
     * ImageView for application logo.
     */
    private final ImageView logoImageView;
    /**
     * GridPane to align information and logo.
     */
    private final GridPane gridPane;
    /**
     * Configuration class to read resource file paths from.
     */
    private final IConfiguration configuration;
    //</editor-fold>
    //
    /**
     * Constructor.
     *
     * @param aConfiguration configuration instance to read resource file paths from
     */
    public AboutView(IConfiguration aConfiguration) {
        super();
        this.configuration = aConfiguration;
        //borderPane
        BorderPane borderPane = new BorderPane();
        AnchorPane.setTopAnchor(borderPane, 0.0);
        AnchorPane.setRightAnchor(borderPane, 0.0);
        AnchorPane.setLeftAnchor(borderPane, 0.0);
        AnchorPane.setBottomAnchor(borderPane, 0.0);
        //borderPane bottom -> buttons
        HBox hBoxButtonsHBox = new HBox();
        hBoxButtonsHBox.setStyle("-fx-background-color: LightGrey");
        borderPane.setBottom(hBoxButtonsHBox);
        hBoxButtonsHBox.prefWidthProperty().bind(this.widthProperty());
        hBoxButtonsHBox.maxWidthProperty().bind(this.widthProperty());
        //-left side
        this.logFileButton = GuiUtil.getButtonOfStandardSize(Message.get("AboutView.logFileButton.text"));
        this.logFileButton.setTooltip(GuiUtil.createTooltip(Message.get("AboutView.logFileButton.tooltip")));
        this.gitHubButton = GuiUtil.getButtonOfStandardSize(Message.get("AboutView.gitHubButton.text"));
        this.gitHubButton.setTooltip(GuiUtil.createTooltip(Message.get("AboutView.gitHubButton.tooltip")));
        this.tutorialButton = GuiUtil.getButtonOfStandardSize(Message.get("AboutView.tutorialButton.text"));
        this.tutorialButton.setTooltip(GuiUtil.createTooltip(Message.get("AboutView.tutorialButton.tooltip")));
        HBox hBoxLeftSideButtons = new HBox();
        hBoxLeftSideButtons.getChildren().addAll(this.logFileButton, this.gitHubButton, this.tutorialButton);
        hBoxLeftSideButtons.setAlignment(Pos.CENTER_LEFT);
        hBoxLeftSideButtons.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        hBoxLeftSideButtons.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(hBoxLeftSideButtons, Priority.ALWAYS);
        hBoxButtonsHBox.getChildren().add(hBoxLeftSideButtons);
        //-right side
        this.closeButton = GuiUtil.getButtonOfStandardSize(Message.get("AboutView.closeButton.text"));
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
        tmpSplitPane.prefWidthProperty().bind(this.widthProperty());
        tmpSplitPane.maxWidthProperty().bind(this.widthProperty());
        //-splitPane top -> gridPane for text and logo
        this.gridPane = new GridPane();
        VBox.setVgrow(this.gridPane, Priority.ALWAYS);
        HBox.setHgrow(this.gridPane, Priority.ALWAYS);
        ColumnConstraints tmpTextCol = new ColumnConstraints();
        tmpTextCol.prefWidthProperty().bind(
                this.gridPane.widthProperty().multiply(0.4975)
        );
        this.gridPane.getColumnConstraints().addFirst(tmpTextCol);

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
        tmpSplitPane.getItems().addFirst(this.gridPane);
        //text
        //-title
        Text tmpAppTitle = new Text(Message.get("AboutView.appTitle.text"));
        tmpAppTitle.setStyle("-fx-font-weight: bold");
        tmpAppTitle.setStyle("-fx-font-size: 24");
        this.gridPane.add(tmpAppTitle,0,0);
        HBox.setHgrow(tmpAppTitle, Priority.ALWAYS);
        VBox.setVgrow(tmpAppTitle, Priority.ALWAYS);
        //-version
        Text tmpVersion = new Text("Version " + BasicDefinitions.MORTAR_VERSION);
        tmpVersion.setStyle("-fx-font-weight: bold");
        this.gridPane.add(tmpVersion, 0,1);
        //-contact
        Text tmpContact = new Text(Message.get("AboutView.contact.text"));
        this.gridPane.add(tmpContact, 0,2);
        //-copyright
        Text tmpCopyright = new Text(Message.get("AboutView.copyright.text"));
        this.gridPane.add(tmpCopyright, 0,3);
        //-license
        Text tmpLicense = new Text(Message.get("AboutView.license.text"));
        ScrollPane tmpLicenseScrollPane = new ScrollPane(tmpLicense);
        TitledPane tmpTitledPaneLicense = new TitledPane(Message.get("AboutView.license.title"), tmpLicenseScrollPane);
        tmpTitledPaneLicense.setExpanded(true);
        tmpTitledPaneLicense.setAlignment(Pos.TOP_LEFT);
        this.gridPane.add(tmpTitledPaneLicense,0,4);
        //-acknowledgement
        Text tmpAcknowledgment = new Text(Message.get("AboutView.acknowledgement.text"));
        ScrollPane tmoAcknowledgmentScrollPane = new ScrollPane(tmpAcknowledgment);
        TitledPane tmpTitledPaneAcknowledgement = new TitledPane(Message.get("AboutView.acknowledgement.title"), tmoAcknowledgmentScrollPane);
        tmpTitledPaneAcknowledgement.setAlignment(Pos.TOP_LEFT);
        tmpTitledPaneAcknowledgement.setExpanded(false);
        this.gridPane.add(tmpTitledPaneAcknowledgement,0,5);
        //-image
        String tmpLogoURL = this.getClass().getClassLoader().getResource(
                this.configuration.getProperty("mortar.imagesFolder")
                        + this.configuration.getProperty("mortar.logo.name")).toExternalForm();
        double tmpImageSize = 495.3125; // magic number, do not touch
        Image tmpLogo = new Image(tmpLogoURL, tmpImageSize, tmpImageSize/1.414, true,true );
        this.logoImageView = new ImageView(tmpLogo);
        this.gridPane.add(this.logoImageView, 1,0, 1, 6);
        GridPane.setHalignment(this.logoImageView, HPos.CENTER);
        GridPane.setValignment(this.logoImageView, VPos.CENTER);
        //-splitPane bottom -> tabPane for tools etc.
        TabPane tabPane = new TabPane();
        tmpSplitPane.getItems().add(1, tabPane);
        Tab tmpToolsTab = new Tab(Message.get("AboutView.toolsTab.title.text"));
        tmpToolsTab.setClosable(false);
        tabPane.getTabs().add(tmpToolsTab);
        //-tableView
        this.tableView = new TableView();
        this.tableView.setEditable(false);
        this.tableView.maxWidthProperty().bind(tabPane.widthProperty());
        //-name column
        TableColumn<ExternalTool, String> tmpNameCol = new TableColumn<>(Message.get("AboutView.toolsTable.nameHeader.text"));
        tmpNameCol.setCellValueFactory(new PropertyValueFactory("name"));
        tmpNameCol.setStyle("-fx-alignment: CENTER");
        tmpNameCol.prefWidthProperty().bind(
                this.tableView.widthProperty().multiply(0.3)
        );
        this.tableView.getColumns().add(tmpNameCol);
        //-name column
        TableColumn<ExternalTool, String> tmpVersionCol = new TableColumn<>(Message.get("AboutView.toolsTable.versionHeader.text"));
        tmpVersionCol.setCellValueFactory(new PropertyValueFactory("version"));
        tmpVersionCol.setStyle("-fx-alignment: CENTER");
        tmpVersionCol.prefWidthProperty().bind(
                this.tableView.widthProperty().multiply(0.1)
        );
        this.tableView.getColumns().add(tmpVersionCol);
        //-name column
        TableColumn<ExternalTool, String> tmpAuthorCol = new TableColumn<>(Message.get("AboutView.toolsTable.authorHeader.text"));
        tmpAuthorCol.setCellValueFactory(new PropertyValueFactory("author"));
        tmpAuthorCol.setStyle("-fx-alignment: CENTER");
        tmpAuthorCol.prefWidthProperty().bind(
                this.tableView.widthProperty().multiply(0.3)
        );
        this.tableView.getColumns().add(tmpAuthorCol);
        //-name column
        TableColumn<ExternalTool, String> tmpLicenseCol = new TableColumn<>(Message.get("AboutView.toolsTable.licenseHeader.text"));
        tmpLicenseCol.setCellValueFactory(new PropertyValueFactory("license"));
        tmpLicenseCol.setStyle("-fx-alignment: CENTER");
        tmpLicenseCol.prefWidthProperty().bind(
                this.tableView.widthProperty().multiply(0.2975)
        );
        this.tableView.getColumns().add(tmpLicenseCol);
        tmpToolsTab.setContent(this.tableView);
        //
        this.getChildren().add(borderPane);
        //listener
        this.gridPane.widthProperty().addListener(((observable, oldValue, newValue) -> {
            tmpTitledPaneLicense.setPrefWidth(newValue.doubleValue() * 0.5);
            tmpTitledPaneLicense.setMinWidth(newValue.doubleValue() * 0.5);
            tmpTitledPaneLicense.setMaxWidth(newValue.doubleValue() * 0.5);
            tmpTitledPaneAcknowledgement.setPrefWidth(newValue.doubleValue() * 0.5);
            tmpTitledPaneAcknowledgement.setMinWidth(newValue.doubleValue() * 0.5);
            tmpTitledPaneAcknowledgement.setMaxWidth(newValue.doubleValue() * 0.5);
        }));
        tmpTitledPaneLicense.expandedProperty().addListener((obs, oldValue, newValue) -> tmpTitledPaneAcknowledgement.setExpanded(!newValue));
        tmpTitledPaneAcknowledgement.expandedProperty().addListener((obs, oldValue, newValue) -> tmpTitledPaneLicense.setExpanded(!newValue));
    }
    //
    //<editor-fold desc="properties" defaultstate="collapsed">
    /**
     * Returns button to open log files directory.
     *
     * @return button
     */
    public Button getLogFileButton(){
        return this.logFileButton;
    }
    //
    /**
     * Returns button to open GitHub repository.
     *
     * @return button
     */
    public Button getGitHubButton(){
        return this.gitHubButton;
    }
    //
    /**
     * Returns button to open tutorial.
     *
     * @return Button to open the MORTAR tutorial
     */
    public Button getTutorialButton() {
        return this.tutorialButton;
    }
    //
    /**
     * Returns button to close this view.
     *
     * @return button
     */
    public Button getCloseButton(){
        return this.closeButton;
    }
    //
    /**
     * Returns the TableView which shows ExternalTool properties.
     * @return TableView {@literal <}ExternalTool {@literal >}
     */
    public TableView<ExternalTool> getTableView(){
        return this.tableView;
    }
    //
    /**
     * Returns grid pane to hold application information and logo.
     *
     * @return GridPane
     */
    public GridPane getGridPane() {
        return this.gridPane;
    }
    //
    /**
     * Returns the ImageView for the logo image.
     *
     * @return ImageView
     */
    public ImageView getLogoImageView() {
        return this.logoImageView;
    }
    //
    /**
     * Sets given image to image view.
     *
     * @param anImage Image to set as logo
     */
    public void setLogoImageViewImage(Image anImage){
        this.logoImageView.setImage(anImage);
    }
    //</editor-fold>
}
