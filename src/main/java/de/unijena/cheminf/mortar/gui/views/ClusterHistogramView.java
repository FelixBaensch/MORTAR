/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2025  Felix Baensch, Jonas Schaub (felix.j.baensch@gmail.com, jonas.schaub@uni-jena.de)
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

import de.unijena.cheminf.mortar.controller.ClusterHistogramViewController;
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;

public class ClusterHistogramView extends AnchorPane {
    /**
     * Button to close view.
     */
    private final Button closeButton;
    /**
     * Button to refresh the histogram.
     */
    private final Button applyButton;
    /**
     * Text field for creating a new histogram with the given number of fragments.
     */
    private final TextField displayedClustersNumberTextField;
    /**
     * Label for the displayed fragments number text field.
     */
    private final Label displayedClusterNumberLabel;
    /**
     * ImageView to display the structures when the cursor hovers over a bar.
     */
    private final ImageView structureDisplayImageView;
    /**
     * Checkbox to choose to show or hide the bar labels that display the exact frequency.
     */
    private final CheckBox displayBarLabelsCheckBox;
    /**
     * CheckBox to display or hide histogram gridlines.
     */
    private final CheckBox displayGridLinesCheckBox;
    /**
     * ScrollPane to make histogram scrollable.
     */
    private final ScrollPane clusterHistogramScrollPane;
    /**
     * ComboBox to make the gap between the bars adjustable.
     */
    private final ComboBox<String> barWidthsComboBox;
    /**
     * Label for the bar widths combo box.
     */
    private final Label barWidthsLabel;
    /**
     * CheckBox to scale the X-axis logarithmically.
     */
    private final CheckBox logarithmicScale; //currently unused
    /**
     * CheckBox to show or hide the bar shadows.
     */
    private final CheckBox barStylingCheckBox;

    //constructor
    public ClusterHistogramView(int aMaxClusterNumber) {
        super();
        this.clusterHistogramScrollPane = new ScrollPane();
        this.clusterHistogramScrollPane.setFitToHeight(true);
        this.clusterHistogramScrollPane.setFitToWidth(true);
        this.clusterHistogramScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        this.clusterHistogramScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        //borderPane
        BorderPane tmpBorderPane = new BorderPane();
        AnchorPane.setTopAnchor(tmpBorderPane, 0.0);
        AnchorPane.setRightAnchor(tmpBorderPane, 0.0);
        AnchorPane.setLeftAnchor(tmpBorderPane, 0.0);
        AnchorPane.setBottomAnchor(tmpBorderPane, 0.0);
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
        //grids for controls
        GridPane tmpLeftSideGrid = new GridPane();
        GridPane tmpRightSideGrid = new GridPane();
        HBox tmpMainHBoxControls = new HBox();
        // main HBox containing grids for controls
        tmpMainHBoxControls.setStyle("-fx-background-color: LightGrey");
        tmpBorderPane.setBottom(tmpMainHBoxControls);
        tmpBorderPane.setCenter(tmpMainGrid);
        HBox tmpHBoxLeftSideControls = new HBox();
        // left side controls
        this.displayedClustersNumberTextField = new TextField();
        this.displayedClustersNumberTextField.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_WIDTH);
        //Todo: message from message file
        this.displayedClustersNumberTextField.setTooltip(GuiUtil.createTooltip(
                Message.get("ClusterHistogramView.displayedClusters.numberTextField")
                        + " " + aMaxClusterNumber));
        this.displayedClustersNumberTextField.setMinWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.displayedClustersNumberTextField.setMaxWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.displayedClustersNumberTextField.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.displayedClustersNumberTextField.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        this.displayedClustersNumberTextField.setAlignment(Pos.CENTER_RIGHT);
        this.displayedClusterNumberLabel = new Label(Message.get("ClusterHistogramView.displayedClusters.label"));
        this.displayedClusterNumberLabel.setTooltip(GuiUtil.createTooltip(Message.get("displays clusters") + " " + aMaxClusterNumber));
        //apply button
        this.applyButton = GuiUtil.getButtonOfStandardSize(Message.get("ClusterHistogramView.applyButton.text"));
        this.applyButton.setTooltip(GuiUtil.createTooltip(Message.get("ClusterHistogramView.applyButton.tooltip")));
        this.barWidthsComboBox = new ComboBox<>();
        for (ClusterHistogramViewController.BarWidthOption tmpBarWidthOptionConstant : ClusterHistogramViewController.BarWidthOption.values()) {
            this.barWidthsComboBox.getItems().add(tmpBarWidthOptionConstant.getDisplayName());
        }
        this.barWidthsComboBox.setTooltip(GuiUtil.createTooltip(Message.get("ClusterHistogramView.comboBox.tooltip")));
        this.barWidthsComboBox.setPrefWidth(GuiDefinitions.GUI_SETTING_COMBO_BOX_PREF_WIDTH_VALUE);
        this.barWidthsComboBox.setMaxWidth(GuiDefinitions.GUI_SETTING_COMBO_BOX_MAX_WIDTH_VALUE);
        this.barWidthsLabel = new Label(Message.get("ClusterHistogramView.gapSettingLabel.text"));
        this.barWidthsLabel.setTooltip(GuiUtil.createTooltip(Message.get("ClusterHistogramView.comboBox.tooltip")));
        tmpLeftSideGrid.setVgap(GuiDefinitions.GUI_INSETS_VALUE);
        tmpLeftSideGrid.setHgap(GuiDefinitions.GUI_INSETS_VALUE);
        tmpLeftSideGrid.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        // grid positions
        tmpLeftSideGrid.add(this.barWidthsLabel, 0,0);
        tmpLeftSideGrid.add(this.barWidthsComboBox,1,0);
        tmpLeftSideGrid.add(this.displayedClusterNumberLabel,2,0);
        tmpLeftSideGrid.add(this.displayedClustersNumberTextField,3,0);
        tmpLeftSideGrid.add(this.applyButton,4,0);
        tmpHBoxLeftSideControls.setAlignment(Pos.CENTER_LEFT);
        tmpHBoxLeftSideControls.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpHBoxLeftSideControls.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(tmpHBoxLeftSideControls, Priority.ALWAYS);
        tmpHBoxLeftSideControls.getChildren().add(tmpLeftSideGrid);
        tmpMainHBoxControls.getChildren().add(tmpHBoxLeftSideControls);
        this.structureDisplayImageView = new ImageView();
        this.structureDisplayImageView.setEffect(new DropShadow(10,2,3, Color.BLACK));
        this.structureDisplayImageView.setStyle("fx-padding: 50px; fx-margin: 50px");
        // right side controls
        this.closeButton = GuiUtil.getButtonOfStandardSize(Message.get("ClusterHistogramView.closeButton.text"));
        this.closeButton.setTooltip(GuiUtil.createTooltip(Message.get("ClusterHistogramView.closeButton.tooltip")));
        //ToDo: add message properties
        this.displayBarLabelsCheckBox = new CheckBox(Message.get("HistogramView.checkBox.text"));
        this.displayBarLabelsCheckBox.setTooltip(GuiUtil.createTooltip(Message.get("HistogramView.checkBox.toolTip")));
        this.displayGridLinesCheckBox = new CheckBox(Message.get("HistogramView.checkBoxGridlines.text"));
        this.displayGridLinesCheckBox.setTooltip(GuiUtil.createTooltip(Message.get("HistogramView.checkBoxGridlines.toolTip")));
        this.logarithmicScale = new CheckBox(Message.get("HistogramView.checkBoxLogarithmicScale.text"));
        this.logarithmicScale.setTooltip(GuiUtil.createTooltip(Message.get("HistogramView.checkBoxLogarithmicScale.toolTip")));
        this.barStylingCheckBox = new CheckBox(Message.get("HistogramView.stylingCheckBox.text"));
        this.barStylingCheckBox.setTooltip(GuiUtil.createTooltip(Message.get("HistogramView.stylingCheckBox.tooltip")));
        HBox tmpHBoxRightSideControls = new HBox();
        tmpRightSideGrid.setHgap(GuiDefinitions.GUI_INSETS_VALUE);
        tmpRightSideGrid.setVgap(GuiDefinitions.GUI_INSETS_VALUE * 2);
        tmpRightSideGrid.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        // grid positions
        tmpRightSideGrid.add(this.displayBarLabelsCheckBox,0,0);
        tmpRightSideGrid.add(this.barStylingCheckBox,1,0);
        tmpRightSideGrid.add(this.displayGridLinesCheckBox,2,0);
        tmpRightSideGrid.add(this.closeButton,3,0);
        tmpHBoxRightSideControls.getChildren().add(tmpRightSideGrid);
        tmpHBoxRightSideControls.setAlignment(Pos.CENTER_RIGHT);
        tmpHBoxRightSideControls.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpHBoxRightSideControls.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(tmpHBoxRightSideControls, Priority.ALWAYS);
        tmpMainHBoxControls.getChildren().add(tmpHBoxRightSideControls);
        // main grid
        tmpMainGrid.add(this.clusterHistogramScrollPane,0,0,4,4);
        tmpMainGrid.add(this.structureDisplayImageView,2,2);
        this.getChildren().add(tmpBorderPane);
    }

    public Button getApplyButton() {
        return this.applyButton;
    }

    public Button getCloseButton() {
        return this.closeButton;
    }

    public ScrollPane getClusterHistogramScrollPane() {
        return this.clusterHistogramScrollPane;
    }

    public ComboBox<String> getBarWidthsComboBox() {
        return this.barWidthsComboBox;
    }

    public CheckBox getDisplayBarLabelsCheckBox() {
        return this.displayBarLabelsCheckBox;
    }

    public CheckBox getDisplayGridLinesCheckBox() {
        return this.displayGridLinesCheckBox;
    }

    public CheckBox getDisplayBarShadowsCheckBox() {
        return this.barStylingCheckBox;
    }

    public TextField getDisplayedClustersNumberTextField() {
        return this.displayedClustersNumberTextField;
    }

    public ImageView getStructureDisplayImageView() {
        return this.structureDisplayImageView;
    }
    //private BarChart<>

}
