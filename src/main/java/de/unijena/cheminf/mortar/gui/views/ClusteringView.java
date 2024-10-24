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

import de.unijena.cheminf.mortar.controller.ClusteringViewController;
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.message.Message;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;

/**
 * View to display clustering results
 *
 * @author Betuel Sevindik
 * @version 1.0.0.0
 *
 */
public class ClusteringView extends AnchorPane {
    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * Button to close view
     */
    private Button closeButton;
    /**
     * Button to display the clustering results for vigilance parameter 0.1
     */
    private ToggleButton vigilanceParameter1Button;
    /**
     * Button to display the clustering results for vigilance parameter 0.2
     */
    private ToggleButton vigilanceParameter2Button;
    /**
     * Button to display the clustering results for vigilance parameter 0.3
     */
    private ToggleButton vigilanceParameter3Button;
    /**
     * Button to display the clustering results for vigilance parameter 0.4
     */
    private ToggleButton vigilanceParameter4Button;
    /**
     * Button to display the clustering results for vigilance parameter 0.5
     */
    private ToggleButton vigilanceParameter5Button;
    /**
     * Button to display the clustering results for vigilance parameter 0.6
     */
    private ToggleButton vigilanceParameter6Button;
    /**
     * Button to display the clustering results for vigilance parameter 0.7
     */
    private ToggleButton vigilanceParameter7Button;
    /**
     * Button to display the clustering results for vigilance parameter 0.8
     */
    private ToggleButton vigilanceParameter8Button;
    /**
     * Button to display the clustering results for vigilance parameter 0.9
     */
    private ToggleButton vigilanceParameter9Button;
    /**
     * Button to open the cluster representatives in the OverviewView
     */
    private Button clusterRepresentativesButton;
    /**
     * ImageView to display the structures when the cursor hovers over a bar
     */
    private ImageView structureDisplayImageView;
    /**
     * Checkbox to choose to show or hide the bar labels that display the exact number of molecules in the clusters
     */
    private CheckBox displayBarLabelsCheckBox;
    /**
     * CheckBox to display or hide histogram gridlines
     */
    private CheckBox displayGridLinesCheckBox;
    /**
     * ScrollPane to make histogram scrollable
     */
    private ScrollPane clusteringViewScrollPane;
    /**
     * CheckBox to show or hide the bar shadows
     */
    private CheckBox barStylingCheckBox;
    /**
     * ComboBox to make the gap between the bars adjustable
     */
    private ComboBox barWidthsComboBox;
    /**
     * ToggleGroup for the vigilance parameter
     */
    private ToggleGroup toggleGroup;
    //</editor-fold>
    //
    //<editor-fold desc="private final class variables" defaultstate="collapsed">
    /**
     * Clustering name in order to be able to build the view accordingly
     */
    public final String VIEW_STYLE_FOR_ART_2A_CLUSTERING = "ART 2-A Clustering";
    /**
     * Border width to style left side view
     */
    private final double BORDER_WIDTH = 1.0;
    /**
     * Corner radius to style left side view
     */
    private final double CORNER_RADIUS = 7.0;
    /**
     * Button size of the vigilance parameter buttons
     */
    private final double BUTTONS_SIZE = 40.0;
    /**
     * Size of the HBox on the left side in the view
     */
    private final double LEFT_SIDE_HBOX_SIZE = 420.0;  //420.0
    /**
     * Label width on the left side view
     */
    private final double DESCRIPTION_LABEL_WIDTH = 80.0;
    /**
     * Label height on the left side view
     */
    private final double DESCRIPTION_LABEL_HEIGHT = 25.0;
    /**
     * Vigilance parameter label width
     */
    private final double DESCRIPTION_VIGILANCE_PARAMETER_WIDTH_LABEL = 200.0;
    /**
     * Label spacing
     */
    private final double DESCRIPTION_LABEL_SPACING = 30.0;
    //</editor-fold>
    //
    //<editor-fold desc="constructor" defaultstate="collapsed">
    /**
     * Constructor, layouts the view and all its components. Current settings need to be adjusted externally.
     *
     * @param aClusteringTypeToStyleClusteringView todo
     */
    public ClusteringView(String aClusteringTypeToStyleClusteringView) {
        super();
        this.clusteringViewScrollPane = new ScrollPane();
        this.clusteringViewScrollPane.setFitToHeight(true);
        this.clusteringViewScrollPane.setFitToWidth(true);
        this.clusteringViewScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        this.clusteringViewScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        //borderPane
        BorderPane tmpBorderPane = new BorderPane();
        ClusteringView.setTopAnchor(tmpBorderPane, 0.0);
        ClusteringView.setRightAnchor(tmpBorderPane, 0.0);
        ClusteringView.setLeftAnchor(tmpBorderPane, 0.0);
        ClusteringView.setBottomAnchor(tmpBorderPane, 0.0);
        //mainGrid (4x4 grid)
        GridPane tmpMainGrid = new GridPane();
        RowConstraints tmpRow1 = new RowConstraints();
        tmpRow1.setVgrow(Priority.ALWAYS);
        tmpRow1.setFillHeight(true);
        tmpMainGrid.getRowConstraints().add(tmpRow1);
        ColumnConstraints tmpCol1 = new ColumnConstraints();
        tmpCol1.setFillWidth(true);
        tmpCol1.setHgrow(Priority.ALWAYS);
        tmpMainGrid.getColumnConstraints().add(tmpCol1);
        ColumnConstraints tmpCol2 = new ColumnConstraints();
        tmpMainGrid.getColumnConstraints().add(tmpCol2);
        RowConstraints tmpRow2 = new RowConstraints();
        tmpMainGrid.getRowConstraints().add(tmpRow2);
        ColumnConstraints tmpCol3 = new ColumnConstraints();
        tmpMainGrid.getColumnConstraints().add(tmpCol3);
        RowConstraints tmpRow3 = new RowConstraints();
        tmpMainGrid.getRowConstraints().add(tmpRow3);
        RowConstraints tmpRow4 = new RowConstraints(20); // magic number
        tmpMainGrid.getRowConstraints().add(tmpRow4);
        ColumnConstraints tmpCol4 = new ColumnConstraints(20); // magic number
        tmpMainGrid.getColumnConstraints().add(tmpCol4);
        //grid for right side controls
        GridPane tmpRightSideGrid = new GridPane();
        // main HBox
        HBox tmpMainHBoxControls = new HBox();
        // main HBox containing grids for controls
        tmpMainHBoxControls.setStyle("-fx-background-color: LightGrey");
        tmpBorderPane.setBottom(tmpMainHBoxControls);
        tmpBorderPane.setCenter(tmpMainGrid);
        if(aClusteringTypeToStyleClusteringView.equals(this.VIEW_STYLE_FOR_ART_2A_CLUSTERING)) {
            // VBox for vigilance parameter buttons on the left side
            VBox tmpLeftSideControlsVBox = new VBox();
            HBox tmpHBoxLeftSideControls = new HBox();
            HBox tmpLabelContainer = new HBox();
            tmpLabelContainer.setSpacing(this.DESCRIPTION_LABEL_SPACING);
            // Labels on the left side
            Label tmpDescriptionRoughLabel = new Label(Message.get("ClusteringView.RoughDescriptionLabel"));
            Label tmpVigilanceParameterLabel = new Label(Message.get("ClusteringView.VigilanceParameterDescriptionLabel"));
            Label tmpDescriptionFineLabel = new Label(Message.get("ClusteringView.FineDescriptionLabel"));
            this.vigilanceParameter1Button = new ToggleButton();
            this.vigilanceParameter1Button.setPrefWidth(this.BUTTONS_SIZE);
            this.vigilanceParameter1Button.setMinWidth(this.BUTTONS_SIZE);
            this.vigilanceParameter1Button.setMaxWidth(this.BUTTONS_SIZE);
            //
            this.vigilanceParameter2Button = new ToggleButton();
            this.vigilanceParameter2Button.setPrefWidth(this.BUTTONS_SIZE);
            this.vigilanceParameter2Button.setMinWidth(this.BUTTONS_SIZE);
            this.vigilanceParameter2Button.setMaxWidth(this.BUTTONS_SIZE);
            //
            this.vigilanceParameter3Button = new ToggleButton();
            this.vigilanceParameter3Button.setPrefWidth(this.BUTTONS_SIZE);
            this.vigilanceParameter3Button.setMinWidth(this.BUTTONS_SIZE);
            this.vigilanceParameter3Button.setMaxWidth(this.BUTTONS_SIZE);
            //
            this.vigilanceParameter4Button = new ToggleButton();
            this.vigilanceParameter4Button.setPrefWidth(this.BUTTONS_SIZE);
            this.vigilanceParameter4Button.setMinWidth(this.BUTTONS_SIZE);
            this.vigilanceParameter4Button.setMaxWidth(this.BUTTONS_SIZE);
            //
            this.vigilanceParameter5Button = new ToggleButton();
            this.vigilanceParameter5Button.setPrefWidth(this.BUTTONS_SIZE);
            this.vigilanceParameter5Button.setMinWidth(this.BUTTONS_SIZE);
            this.vigilanceParameter5Button.setMaxWidth(this.BUTTONS_SIZE);
            //
            this.vigilanceParameter6Button = new ToggleButton();
            this.vigilanceParameter6Button.setPrefWidth(this.BUTTONS_SIZE);
            this.vigilanceParameter6Button.setMinWidth(this.BUTTONS_SIZE);
            this.vigilanceParameter6Button.setMaxWidth(this.BUTTONS_SIZE);
            //
            this.vigilanceParameter7Button = new ToggleButton();
            this.vigilanceParameter7Button.setPrefWidth(this.BUTTONS_SIZE);
            this.vigilanceParameter7Button.setMinWidth(this.BUTTONS_SIZE);
            this.vigilanceParameter7Button.setMaxWidth(this.BUTTONS_SIZE);
            //
            this.vigilanceParameter8Button = new ToggleButton();
            this.vigilanceParameter8Button.setPrefWidth(this.BUTTONS_SIZE);
            this.vigilanceParameter8Button.setMinWidth(this.BUTTONS_SIZE);
            this.vigilanceParameter8Button.setMaxWidth(this.BUTTONS_SIZE);
            //
            this.vigilanceParameter9Button = new ToggleButton();
            this.vigilanceParameter9Button.setPrefWidth(this.BUTTONS_SIZE);
            this.vigilanceParameter9Button.setMinWidth(this.BUTTONS_SIZE);
            this.vigilanceParameter9Button.setMaxWidth(this.BUTTONS_SIZE);
            // ToggleGroup
            this.toggleGroup = new ToggleGroup();
            this.vigilanceParameter1Button.setToggleGroup(this.toggleGroup);
            this.vigilanceParameter2Button.setToggleGroup(this.toggleGroup);
            this.vigilanceParameter3Button.setToggleGroup(this.toggleGroup);
            this.vigilanceParameter4Button.setToggleGroup(this.toggleGroup);
            this.vigilanceParameter5Button.setToggleGroup(this.toggleGroup);
            this.vigilanceParameter6Button.setToggleGroup(this.toggleGroup);
            this.vigilanceParameter7Button.setToggleGroup(this.toggleGroup);
            this.vigilanceParameter8Button.setToggleGroup(this.toggleGroup);
            this.vigilanceParameter9Button.setToggleGroup(this.toggleGroup);
            //
            tmpHBoxLeftSideControls.setAlignment(Pos.CENTER_LEFT);
            tmpHBoxLeftSideControls.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
            tmpHBoxLeftSideControls.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
            tmpHBoxLeftSideControls.setPrefWidth(this.LEFT_SIDE_HBOX_SIZE);
            tmpHBoxLeftSideControls.setMinWidth(this.LEFT_SIDE_HBOX_SIZE);
            tmpHBoxLeftSideControls.setMaxWidth(this.LEFT_SIDE_HBOX_SIZE);
            tmpHBoxLeftSideControls.getChildren().addAll(
                    this.vigilanceParameter1Button, this.vigilanceParameter2Button, this.vigilanceParameter3Button,
                    this.vigilanceParameter4Button, this.vigilanceParameter5Button, this.vigilanceParameter6Button,
                    this.vigilanceParameter7Button, this.vigilanceParameter8Button, this.vigilanceParameter9Button
            );
            // Style left side controls
            Color tmpBorderColor = Color.LIGHTGREY;
            BorderStroke tmpBorderStroke = new BorderStroke(
                    tmpBorderColor, BorderStrokeStyle.SOLID, new CornerRadii(this.CORNER_RADIUS), new BorderWidths(this.BORDER_WIDTH));
            tmpHBoxLeftSideControls.setBorder(new Border(tmpBorderStroke));
            DropShadow tmpDropShadow = new DropShadow(10, Color.GRAY);
            tmpHBoxLeftSideControls.setEffect(tmpDropShadow);
            Color tmpBackgroundColor = Color.LIGHTGREY;
            BackgroundFill tmpBackgroundFill = new BackgroundFill(tmpBackgroundColor, new CornerRadii(this.CORNER_RADIUS), null);
            Background tmpBackground = new Background(tmpBackgroundFill);
            tmpHBoxLeftSideControls.setBackground(tmpBackground);
            // rough description label
            tmpDescriptionRoughLabel.setBorder(new Border(tmpBorderStroke));
            tmpDescriptionRoughLabel.setEffect(tmpDropShadow);
            tmpDescriptionRoughLabel.setBackground(tmpBackground);
            tmpDescriptionRoughLabel.setPrefWidth(this.DESCRIPTION_LABEL_WIDTH);
            tmpDescriptionRoughLabel.setMinWidth(this.DESCRIPTION_LABEL_WIDTH);
            tmpDescriptionRoughLabel.setMaxWidth(this.DESCRIPTION_LABEL_WIDTH);
            tmpDescriptionRoughLabel.setPrefHeight(this.DESCRIPTION_LABEL_HEIGHT);
            tmpDescriptionRoughLabel.setMinHeight(this.DESCRIPTION_LABEL_HEIGHT);
            tmpDescriptionRoughLabel.setMinHeight(this.DESCRIPTION_LABEL_HEIGHT);
            tmpDescriptionRoughLabel.setStyle("-fx-font-style: italic;");
            tmpDescriptionRoughLabel.setAlignment(Pos.CENTER);
            // fine description label
            tmpDescriptionFineLabel.setBorder(new Border(tmpBorderStroke));
            tmpDescriptionFineLabel.setEffect(tmpDropShadow);
            tmpDescriptionFineLabel.setBackground(tmpBackground);
            tmpDescriptionFineLabel.setPrefWidth(this.DESCRIPTION_LABEL_WIDTH);
            tmpDescriptionFineLabel.setMinWidth(this.DESCRIPTION_LABEL_WIDTH);
            tmpDescriptionFineLabel.setMaxWidth(this.DESCRIPTION_LABEL_WIDTH);
            tmpDescriptionFineLabel.setPrefHeight(this.DESCRIPTION_LABEL_HEIGHT);
            tmpDescriptionFineLabel.setMinHeight(this.DESCRIPTION_LABEL_HEIGHT);
            tmpDescriptionFineLabel.setMinHeight(this.DESCRIPTION_LABEL_HEIGHT);
            tmpDescriptionFineLabel.setAlignment(Pos.CENTER);
            tmpDescriptionFineLabel.setStyle("-fx-font-style: italic;");
            // vigilance parameter description label
            tmpVigilanceParameterLabel.setBorder(new Border(tmpBorderStroke));
            tmpVigilanceParameterLabel.setEffect(tmpDropShadow);
            tmpVigilanceParameterLabel.setBackground(tmpBackground);
            tmpVigilanceParameterLabel.setPrefWidth(this.DESCRIPTION_VIGILANCE_PARAMETER_WIDTH_LABEL);
            tmpVigilanceParameterLabel.setMinWidth(this.DESCRIPTION_VIGILANCE_PARAMETER_WIDTH_LABEL);
            tmpVigilanceParameterLabel.setMaxWidth(this.DESCRIPTION_VIGILANCE_PARAMETER_WIDTH_LABEL);
            tmpVigilanceParameterLabel.setPrefHeight(this.DESCRIPTION_LABEL_HEIGHT);
            tmpVigilanceParameterLabel.setMinHeight(this.DESCRIPTION_LABEL_HEIGHT);
            tmpVigilanceParameterLabel.setMinHeight(this.DESCRIPTION_LABEL_HEIGHT);
            tmpVigilanceParameterLabel.setAlignment(Pos.CENTER);
            tmpVigilanceParameterLabel.setStyle("-fx-font-style: italic;");
            // add elements
            tmpLabelContainer.getChildren().addAll(tmpDescriptionRoughLabel, tmpVigilanceParameterLabel, tmpDescriptionFineLabel);
            tmpLeftSideControlsVBox.getChildren().addAll(tmpHBoxLeftSideControls, tmpLabelContainer);
            tmpLeftSideControlsVBox.setPadding(new Insets(10,25,0,25));
            tmpLeftSideControlsVBox.setSpacing(GuiDefinitions.GUI_INSETS_VALUE);
            tmpMainHBoxControls.getChildren().add(tmpLeftSideControlsVBox);
        }
        // general GUI elements that exist independently of the clustering algorithm
        this.clusterRepresentativesButton = new Button("Representatives");
        this.barWidthsComboBox = new ComboBox<>();
        for (ClusteringViewController.BarWidthOption tmpBarWidthOptionConstant : ClusteringViewController.BarWidthOption.values()) {
            this.barWidthsComboBox.getItems().add(tmpBarWidthOptionConstant.getDisplayName());
        }
        this.structureDisplayImageView = new ImageView();
        this.structureDisplayImageView.setEffect(new DropShadow(10,2,3, Color.BLACK));
        this.structureDisplayImageView.setStyle("fx-padding: 50px; fx-margin: 50px");
        // right side controls
        this.closeButton = new Button(Message.get("HistogramView.cancelButton.text"));
        this.closeButton.setTooltip(new Tooltip(Message.get("HistogramView.cancelButton.toolTip")));
        this.closeButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        this.closeButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.displayBarLabelsCheckBox = new CheckBox(Message.get("HistogramView.checkBox.text"));
        this.displayBarLabelsCheckBox.setTooltip(new Tooltip(Message.get("HistogramView.checkBox.toolTip")));
        this.displayGridLinesCheckBox = new CheckBox(Message.get("HistogramView.checkBoxGridlines.text"));
        this.displayGridLinesCheckBox.setTooltip(new Tooltip(Message.get("HistogramView.checkBoxGridlines.toolTip")));
        this.barStylingCheckBox = new CheckBox(Message.get("HistogramView.stylingCheckBox.text"));
        this.barStylingCheckBox.setTooltip(new Tooltip(Message.get("HistogramView.stylingCheckBox.tooltip")));
        HBox tmpHBoxRightSideControls = new HBox();
        tmpRightSideGrid.setHgap(GuiDefinitions.GUI_INSETS_VALUE);
        tmpRightSideGrid.setVgap(GuiDefinitions.GUI_INSETS_VALUE * 2);
        tmpRightSideGrid.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        // grid positions
        tmpRightSideGrid.add(this.displayBarLabelsCheckBox,0,0);
        tmpRightSideGrid.add(this.displayGridLinesCheckBox,1,0);
        tmpRightSideGrid.add(this.barStylingCheckBox,2,0);
        tmpRightSideGrid.add(this.clusterRepresentativesButton,0,1);
        tmpRightSideGrid.add(this.barWidthsComboBox,1,1);
        tmpRightSideGrid.add(this.closeButton, 2,1);
        tmpHBoxRightSideControls.getChildren().add(tmpRightSideGrid);
        tmpHBoxRightSideControls.setAlignment(Pos.CENTER_RIGHT);
        tmpHBoxRightSideControls.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpHBoxRightSideControls.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(tmpHBoxRightSideControls, Priority.ALWAYS);
        tmpMainHBoxControls.getChildren().add(tmpHBoxRightSideControls);
        // main grid
        tmpMainGrid.add(this.clusteringViewScrollPane,0,0,4,4);
        tmpMainGrid.add(this.structureDisplayImageView,2,2);
        this.getChildren().add(tmpBorderPane);
    }
    //</editor-fold>
    //
    //<editor-fold desc="public properties" defaultstate="collapsed">
    /**
     * Returns the display bar labels check box, i.e. the frequency labels on the right-hand side of the bars.
     *
     * @return CheckBox for choosing whether to label the bars with frequencies
     */
    public CheckBox getDisplayBarLabelsCheckBox() {
        return this.displayBarLabelsCheckBox;
    }
    //
    /**
     * Returns the display grid lines check box.
     *
     * @return CheckBox to show or display the grid lines in the histogram
     */
    public CheckBox getDisplayGridLinesCheckBox() {
        return this.displayGridLinesCheckBox;
    }
    //
    /**
     * Returns the display bar shadows check box.
     *
     * @return CheckBox for displaying or hiding bar shadows
     */
    public CheckBox getDisplayBarShadowsCheckBox() {
        return this.barStylingCheckBox;
    }
    //
    /**
     * Returns an ImageView to enable the display of the structures when the cursor hovers over a bar.
     *
     * @return ImageView shows the different structures when hovering over the histogram
     */
    public ImageView getStructureDisplayImageView() {
        return this.structureDisplayImageView;
    }
    //
    /**
     * Returns a ScrollPane in which the histogram is to be displayed.
     *
     * @return ScrollPane to make histogram scrollable
     */
    public ScrollPane getHistogramScrollPane() {
        return this.clusteringViewScrollPane;
    }
    //
    /**
     * Returns combo box for setting bar width of the histogram.
     *
     * @return ComboBox for setting bar widths
     */
    public ComboBox getBarWidthsComboBox() {
        return this.barWidthsComboBox;
    }
    //
    /**
     * Returns button to display clustering results for the vigilance parameter 0.1
     *
     * @return Button to display clustering result for the vigilance parameter 0.1
     */
    public ToggleButton getVigilanceParameter1Button(){
        return this.vigilanceParameter1Button;
    }
    //
    /**
     * Returns button to display clustering results for the vigilance parameter 0.2
     *
     * @return Button to display clustering result for the vigilance parameter 0.2
     */
    public ToggleButton getVigilanceParameter2Button(){
        return this.vigilanceParameter2Button;
    }
    //
    /**
     * Returns button to display clustering results for the vigilance parameter 0.3
     *
     * @return Button to display clustering result for the vigilance parameter 0.3
     */
    public ToggleButton getVigilanceParameter3Button(){
        return this.vigilanceParameter3Button;
    }
    //
    /**
     * Returns button to display clustering results for the vigilance parameter 0.4
     *
     * @return Button to display clustering result for the vigilance parameter 0.4
     */
    public ToggleButton getVigilanceParameter4Button(){
        return this.vigilanceParameter4Button;
    }
    //
    /**
     * Returns button to display clustering results for the vigilance parameter 0.5
     *
     * @return Button to display clustering result for the vigilance parameter 0.5
     */
    public ToggleButton getVigilanceParameter5Button(){
        return this.vigilanceParameter5Button;
    }
    //
    /**
     * Returns button to display clustering results for the vigilance parameter 0.6
     *
     * @return Button to display clustering result for the vigilance parameter 0.6
     */
    public ToggleButton getVigilanceParameter6Button(){
        return this.vigilanceParameter6Button;
    }
    //
    /**
     * Returns button to display clustering results for the vigilance parameter 0.7
     *
     * @return Button to display clustering result for the vigilance parameter 0.7
     */
    public ToggleButton getVigilanceParameter7Button(){
        return this.vigilanceParameter7Button;
    }
    //
    /**
     * Returns button to display clustering results for the vigilance parameter 0.8
     *
     * @return Button to display clustering result for the vigilance parameter 0.8
     */
    public ToggleButton getVigilanceParameter8Button(){
        return this.vigilanceParameter8Button;
    }
    //
    /**
     * Returns button to display clustering results for the vigilance parameter 0.9
     *
     * @return Button to display clustering result for the vigilance parameter 0.9
     */
    public ToggleButton getVigilanceParameter9Button(){
        return this.vigilanceParameter9Button;
    }
    //
    /**
     * Returns button for closing the view
     *
     * @return Button for closing the clustering view
     */
    public Button getCloseButton() {
        return this.closeButton;
    }
    //
    /**
     * Returns a list, which stores all vigilance parameter buttons
     *
     * @return ArrayList
     */
    public ArrayList<ToggleButton> getButtons() {
        ArrayList<ToggleButton> tmpButtonsList = new ArrayList<>(9);
        tmpButtonsList.add(this.vigilanceParameter1Button);
        tmpButtonsList.add(this.vigilanceParameter2Button);
        tmpButtonsList.add(this.vigilanceParameter3Button);
        tmpButtonsList.add(this.vigilanceParameter4Button);
        tmpButtonsList.add(this.vigilanceParameter5Button);
        tmpButtonsList.add(this.vigilanceParameter6Button);
        tmpButtonsList.add(this.vigilanceParameter7Button);
        tmpButtonsList.add(this.vigilanceParameter8Button);
        tmpButtonsList.add(this.vigilanceParameter9Button);
        return tmpButtonsList;
    }
    //
    /**
     * Returns a button to open the overview with all cluster representatives
     *
     * @return Button to show cluster representatives in the overview
     */
    public Button getClusterRepresentativesButton() {
        return this.clusterRepresentativesButton;
    }
    //</editor-fold>
}
