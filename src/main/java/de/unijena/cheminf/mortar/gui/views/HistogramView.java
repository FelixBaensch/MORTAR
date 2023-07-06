/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2023  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

import de.unijena.cheminf.mortar.controller.HistogramViewController;
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.message.Message;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
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

/**
 * View for the frequency histogram.
 *
 * @author Betuel Sevindik, Jonas Schaub
 * @version 1.0.1.0
 */
public class HistogramView extends AnchorPane {
    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * Button to close view
     */
    private Button closeButton;
    /**
     * Button to refresh the histogram
     */
    private Button applyButton;
    /**
     * Text field for creating a new histogram with the given number of fragments
     */
    private TextField displayedFragmentsNumberTextField;
    /**
     * Label for the displayed fragments number text field
     */
    private Label displayedFragmentsNumberLabel;
    /**
     * ImageView to display the structures when the cursor hovers over a bar
     */
    private ImageView structureDisplayImageView;
    /**
     * Text field for defining the maximum SMILES length to display fully on the y-axis.
     */
    private TextField maximumSMILESLengthTextField;
    /**
     * Label for the maximum SMILES length text field.
     */
    private Label maximumSMILESLengthLabel;
    /**
     * Checkbox to choose to show or hide the bar labels that display the exact frequency.
     */
    private CheckBox displayBarLabelsCheckBox;
    /**
     * CheckBox to display or hide histogram gridlines
     */
    private CheckBox displayGridLinesCheckBox;
    /**
     * ScrollPane to make histogram scrollable
     */
    private ScrollPane histogramScrollPane;
    /**
     * ComboBox to make the gap between the bars adjustable
     */
    private ComboBox barWidthsComboBox;
    /**
     * Label for the bar widths combo box.
     */
    private Label barWidthsLabel;
    /**
     * CheckBox to scale the X-axis logarithmically
     */
    private CheckBox logarithmicScale; //TODO: currently unused
    /**
     * CheckBox to show or hide the bar shadows
     */
    private CheckBox barStylingCheckBox;
    /**
     * CheckBox to show or hide the SMILES labels on the y-axis
     */
    private CheckBox displaySMILESonYaxisCheckBox;
    /**
     * ComboBox to choose which frequency is used
     */
    private ComboBox frequencyComboBox;
    /**
     * Label for the frequency combo box.
     */
    private Label frequencyLabel;
    //</editor-fold>
    //
    /**
     * Constructor, layouts the view and all its components. Current settings need to be adjusted externally.
     *
     * @param aMaxFragmentNumber indicates the maximum number of fragments available for display; is included in some
     *                           tooltips
     */
    public HistogramView(int aMaxFragmentNumber) {
        super();
        this.histogramScrollPane = new ScrollPane();
        this.histogramScrollPane.setFitToHeight(true);
        this.histogramScrollPane.setFitToWidth(true);
        this.histogramScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        this.histogramScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        //borderPane
        BorderPane tmpBorderPane = new BorderPane();
        HistogramView.setTopAnchor(tmpBorderPane, 0.0);
        HistogramView.setRightAnchor(tmpBorderPane, 0.0);
        HistogramView.setLeftAnchor(tmpBorderPane, 0.0);
        HistogramView.setBottomAnchor(tmpBorderPane, 0.0);
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
        this.displayedFragmentsNumberTextField = new TextField();
        this.displayedFragmentsNumberTextField.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_WIDTH);
        this.displayedFragmentsNumberTextField.setTooltip(new Tooltip(Message.get("HistogramView.textField.toolTip") + " " + aMaxFragmentNumber));
        this.applyButton = new Button(Message.get("HistogramView.refreshButton.text"));
        this.applyButton.setTooltip(new Tooltip(Message.get("HistogramView.refreshButton.toolTip")));
        this.applyButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.applyButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        this.maximumSMILESLengthTextField = new TextField();
        this.maximumSMILESLengthTextField.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_WIDTH);
        this.maximumSMILESLengthTextField.setTooltip(new Tooltip(Message.get("HistogramView.smilesField.toolTip")));
        this.maximumSMILESLengthLabel = new Label(Message.get("HistogramView.smilesLabel.text"));
        this.maximumSMILESLengthLabel.setTooltip(new Tooltip(Message.get("HistogramView.smilesField.toolTip")));
        this.displayedFragmentsNumberLabel = new Label(Message.get("HistogramView.displayedFragmentsTextFieldLabel.text"));
        this.displayedFragmentsNumberLabel.setTooltip(new Tooltip(Message.get("HistogramView.textField.toolTip") + " " + aMaxFragmentNumber));
        this.barWidthsComboBox = new ComboBox<>();
        for (HistogramViewController.BarWidthOption tmpBarWidthOptionConstant : HistogramViewController.BarWidthOption.values()) {
            this.barWidthsComboBox.getItems().add(tmpBarWidthOptionConstant.getDisplayName());
        }
        this.barWidthsComboBox.setTooltip(new Tooltip(Message.get("HistogramView.comboBox.toolTip")));
        this.barWidthsLabel = new Label(Message.get("HistogramView.gapSettingLabel.text"));
        this.barWidthsLabel.setTooltip(new Tooltip(Message.get("HistogramView.comboBox.toolTip")));
        this.frequencyLabel = new Label(Message.get("HistogramView.chooseDataComboBox.text"));
        this.frequencyLabel.setTooltip(new Tooltip(Message.get("HistogramView.chooseDataComboBox.toolTip")));
        this.frequencyComboBox = new ComboBox<>();
        for (HistogramViewController.FrequencyOption tmpFrequencyOptionConstant : HistogramViewController.FrequencyOption.values()) {
            this.frequencyComboBox.getItems().add(tmpFrequencyOptionConstant.getDisplayName());
        }
        this.frequencyComboBox.setTooltip(new Tooltip(Message.get("HistogramView.chooseDataComboBox.toolTip")));
        tmpLeftSideGrid.setVgap(GuiDefinitions.GUI_INSETS_VALUE);
        tmpLeftSideGrid.setHgap(GuiDefinitions.GUI_INSETS_VALUE);
        tmpLeftSideGrid.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        // grid positions
        tmpLeftSideGrid.add(this.barWidthsLabel, 0,0);
        tmpLeftSideGrid.add(this.frequencyLabel,0,1);
        tmpLeftSideGrid.add(this.barWidthsComboBox,1,0);
        tmpLeftSideGrid.add(this.frequencyComboBox,1,1);
        tmpLeftSideGrid.add(this.maximumSMILESLengthLabel,2,0);
        tmpLeftSideGrid.add(this.displayedFragmentsNumberLabel,2,1);
        tmpLeftSideGrid.add(this.maximumSMILESLengthTextField,3,0);
        tmpLeftSideGrid.add(this.displayedFragmentsNumberTextField,3,1);
        tmpLeftSideGrid.add(this.applyButton,4,1);
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
        this.closeButton = new Button(Message.get("HistogramView.cancelButton.text"));
        this.closeButton.setTooltip(new Tooltip(Message.get("HistogramView.cancelButton.toolTip")));
        this.closeButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        this.closeButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.displayBarLabelsCheckBox = new CheckBox(Message.get("HistogramView.checkBox.text"));
        this.displayBarLabelsCheckBox.setTooltip(new Tooltip(Message.get("HistogramView.checkBox.toolTip")));
        this.displayGridLinesCheckBox = new CheckBox(Message.get("HistogramView.checkBoxGridlines.text"));
        this.displayGridLinesCheckBox.setTooltip(new Tooltip(Message.get("HistogramView.checkBoxGridlines.toolTip")));
        this.logarithmicScale = new CheckBox(Message.get("HistogramView.checkBoxLogarithmicScale.text"));
        this.logarithmicScale.setTooltip(new Tooltip(Message.get("HistogramView.checkBoxLogarithmicScale.toolTip")));
        this.barStylingCheckBox = new CheckBox(Message.get("HistogramView.stylingCheckBox.text"));
        this.barStylingCheckBox.setTooltip(new Tooltip(Message.get("HistogramView.stylingCheckBox.tooltip")));
        this.displaySMILESonYaxisCheckBox = new CheckBox(Message.get("HistogramView.checkBoxSmilesTickLabel.text"));
        this.displaySMILESonYaxisCheckBox.setTooltip(new Tooltip(Message.get("HistogramView.checkBoxSmilesTickLabel.toolTip")));
        HBox tmpHBoxRightSideControls = new HBox();
        tmpRightSideGrid.setHgap(GuiDefinitions.GUI_INSETS_VALUE);
        tmpRightSideGrid.setVgap(GuiDefinitions.GUI_INSETS_VALUE * 2);
        tmpRightSideGrid.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        // grid positions
        tmpRightSideGrid.add(this.displayBarLabelsCheckBox,0,0);
        tmpRightSideGrid.add(this.barStylingCheckBox,0,1);
        tmpRightSideGrid.add(this.displayGridLinesCheckBox,1,0);
        tmpRightSideGrid.add(this.displaySMILESonYaxisCheckBox,1,1);
        tmpRightSideGrid.add(this.closeButton,2,1);
        tmpHBoxRightSideControls.getChildren().add(tmpRightSideGrid);
        tmpHBoxRightSideControls.setAlignment(Pos.CENTER_RIGHT);
        tmpHBoxRightSideControls.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpHBoxRightSideControls.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(tmpHBoxRightSideControls, Priority.ALWAYS);
        tmpMainHBoxControls.getChildren().add(tmpHBoxRightSideControls);
        // main grid
        tmpMainGrid.add(this.histogramScrollPane,0,0,4,4);
        tmpMainGrid.add(this.structureDisplayImageView,2,2);
        this.getChildren().add(tmpBorderPane);
    }
    //
    //<editor-fold desc="public properties" defaultstate="collapsed">
    /**
     * Returns button for closing the view
     *
     * @return button for closing histogram view
     */
    public Button getCloseButton() {
        return this.closeButton;
    }
    //
    /**
     * Returns a button for generating a new histogram with updated fragment number, bar width, frequency, and SMILES length.
     *
     * @return button for applying updated settings to histogram
     */
    public Button getApplyButton() {
        return this.applyButton;
    }
    //
    /**
     * Returns content of displayed fragments number text field, i.e. string representation of the number of fragments to
     * display.
     *
     * @return String number of fragments to be displayed in the histogram
     */
    public String getDisplayedFragmentsNumberTextFieldContent() {return this.displayedFragmentsNumberTextField.getText();}
    //
    /**
     * Returns the text field where the maximum SMILES string length to display is entered.
     *
     * @return maximum SMILES length text field
     */
    public TextField getMaximumSMILESLengthTextField() {
        return this.maximumSMILESLengthTextField;
    }
    //
    /**
     * Returns the text field where the number of fragments to display is entered.
     *
     * @return displayed fragments number text field
     */
    public TextField getDisplayedFragmentsNumberTextField() {
        return this.displayedFragmentsNumberTextField;
    }
    //
    /**
     * Returns content of maximum SMILES length text field, i.e. string representation of the maximum SMILES length
     * that should be displayed.
     *
     * @return String maximum SMILES length
     */
    public String getMaximumSMILESLengthTextFieldContent() {return this.maximumSMILESLengthTextField.getText();}
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
    //TODO currently unused
    /**
     * Returns a CheckBox to make the number axis logarithmically
     *
     * @return CheckBox for logarithmic number axis
     */
    /*public CheckBox getLogarithmicScale(){
        return this.logarithmicScale;
    }*/
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
     * Returns a ScrollPane in which the histogram is to be displayed.
     *
     * @return ScrollPane to make histogram scrollable
     */
    public ScrollPane getHistogramScrollPane() {
        return this.histogramScrollPane;
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
     * Returns check box for displaying or hiding SMILES labels on the y-axis of the histogram.
     *
     * @return CheckBox for displaying or hiding SMILES labels on y-axis
     */
    public CheckBox getDisplaySmilesOnYAxisCheckBox() {
        return this.displaySMILESonYaxisCheckBox;
    }
    //
    /**
     * Returns a combo box for choosing which frequency of the fragments to display.
     *
     * @return ComboBox for choosing frequency type to display
     */
    public ComboBox getFrequencyComboBox() {
        return this.frequencyComboBox;
    }
    //</editor-fold>
}
