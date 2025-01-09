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

import de.unijena.cheminf.mortar.controller.HistogramViewController;
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

/**
 * View for the frequency histogram.
 *
 * @author Betuel Sevindik, Jonas Schaub
 * @version 1.0.1.0
 */
public class HistogramView extends AnchorPane {
    //<editor-fold desc="private class variables" defaultstate="collapsed">
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
    private final TextField displayedFragmentsNumberTextField;
    /**
     * Label for the displayed fragments number text field.
     */
    private final Label displayedFragmentsNumberLabel;
    /**
     * ImageView to display the structures when the cursor hovers over a bar.
     */
    private final ImageView structureDisplayImageView;
    /**
     * Text field for defining the maximum SMILES length to display fully on the y-axis.
     */
    private final TextField maximumSMILESLengthTextField;
    /**
     * Label for the maximum SMILES length text field.
     */
    private final Label maximumSMILESLengthLabel;
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
    private final ScrollPane histogramScrollPane;
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
    /**
     * CheckBox to show or hide the SMILES labels on the y-axis.
     */
    private final CheckBox displaySMILESonYaxisCheckBox;
    /**
     * ComboBox to choose which frequency is used.
     */
    private final ComboBox<String> frequencyComboBox;
    /**
     * Label for the frequency combo box.
     */
    private final Label frequencyLabel;
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
        this.displayedFragmentsNumberTextField = new TextField();
        this.displayedFragmentsNumberTextField.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_WIDTH);
        this.displayedFragmentsNumberTextField.setTooltip(GuiUtil.createTooltip(Message.get("HistogramView.textField.toolTip") + " " + aMaxFragmentNumber));
        this.displayedFragmentsNumberTextField.setMinWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.displayedFragmentsNumberTextField.setMaxWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.displayedFragmentsNumberTextField.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.displayedFragmentsNumberTextField.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        this.displayedFragmentsNumberTextField.setAlignment(Pos.CENTER_RIGHT);
        this.applyButton = GuiUtil.getButtonOfStandardSize(Message.get("HistogramView.refreshButton.text"));
        this.applyButton.setTooltip(GuiUtil.createTooltip(Message.get("HistogramView.refreshButton.toolTip")));
        this.maximumSMILESLengthTextField = new TextField();
        this.maximumSMILESLengthTextField.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_WIDTH);
        this.maximumSMILESLengthTextField.setTooltip(GuiUtil.createTooltip(Message.get("HistogramView.smilesField.toolTip")));
        this.maximumSMILESLengthTextField.setMinWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.maximumSMILESLengthTextField.setMaxWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.maximumSMILESLengthTextField.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.maximumSMILESLengthTextField.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        this.maximumSMILESLengthTextField.setAlignment(Pos.CENTER_RIGHT);
        this.maximumSMILESLengthLabel = new Label(Message.get("HistogramView.smilesLabel.text"));
        this.maximumSMILESLengthLabel.setTooltip(GuiUtil.createTooltip(Message.get("HistogramView.smilesField.toolTip")));
        this.displayedFragmentsNumberLabel = new Label(Message.get("HistogramView.displayedFragmentsTextFieldLabel.text"));
        this.displayedFragmentsNumberLabel.setTooltip(GuiUtil.createTooltip(Message.get("HistogramView.textField.toolTip") + " " + aMaxFragmentNumber));
        this.barWidthsComboBox = new ComboBox<>();
        for (HistogramViewController.BarWidthOption tmpBarWidthOptionConstant : HistogramViewController.BarWidthOption.values()) {
            this.barWidthsComboBox.getItems().add(tmpBarWidthOptionConstant.getDisplayName());
        }
        this.barWidthsComboBox.setTooltip(GuiUtil.createTooltip(Message.get("HistogramView.comboBox.toolTip")));
        this.barWidthsComboBox.setPrefWidth(GuiDefinitions.GUI_SETTING_COMBO_BOX_PREF_WIDTH_VALUE);
        this.barWidthsComboBox.setMaxWidth(GuiDefinitions.GUI_SETTING_COMBO_BOX_MAX_WIDTH_VALUE);
        this.barWidthsLabel = new Label(Message.get("HistogramView.gapSettingLabel.text"));
        this.barWidthsLabel.setTooltip(GuiUtil.createTooltip(Message.get("HistogramView.comboBox.toolTip")));
        this.frequencyLabel = new Label(Message.get("HistogramView.chooseDataComboBox.text"));
        this.frequencyLabel.setTooltip(GuiUtil.createTooltip(Message.get("HistogramView.chooseDataComboBox.toolTip")));
        this.frequencyComboBox = new ComboBox<>();
        for (HistogramViewController.FrequencyOption tmpFrequencyOptionConstant : HistogramViewController.FrequencyOption.values()) {
            this.frequencyComboBox.getItems().add(tmpFrequencyOptionConstant.getDisplayName());
        }
        this.frequencyComboBox.setTooltip(GuiUtil.createTooltip(Message.get("HistogramView.chooseDataComboBox.toolTip")));
        this.frequencyComboBox.setPrefWidth(GuiDefinitions.GUI_SETTING_COMBO_BOX_PREF_WIDTH_VALUE);
        this.frequencyComboBox.setMaxWidth(GuiDefinitions.GUI_SETTING_COMBO_BOX_MAX_WIDTH_VALUE);
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
        this.closeButton = GuiUtil.getButtonOfStandardSize(Message.get("HistogramView.cancelButton.text"));
        this.closeButton.setTooltip(GuiUtil.createTooltip(Message.get("HistogramView.cancelButton.toolTip")));
        this.displayBarLabelsCheckBox = new CheckBox(Message.get("HistogramView.checkBox.text"));
        this.displayBarLabelsCheckBox.setTooltip(GuiUtil.createTooltip(Message.get("HistogramView.checkBox.toolTip")));
        this.displayGridLinesCheckBox = new CheckBox(Message.get("HistogramView.checkBoxGridlines.text"));
        this.displayGridLinesCheckBox.setTooltip(GuiUtil.createTooltip(Message.get("HistogramView.checkBoxGridlines.toolTip")));
        this.logarithmicScale = new CheckBox(Message.get("HistogramView.checkBoxLogarithmicScale.text"));
        this.logarithmicScale.setTooltip(GuiUtil.createTooltip(Message.get("HistogramView.checkBoxLogarithmicScale.toolTip")));
        this.barStylingCheckBox = new CheckBox(Message.get("HistogramView.stylingCheckBox.text"));
        this.barStylingCheckBox.setTooltip(GuiUtil.createTooltip(Message.get("HistogramView.stylingCheckBox.tooltip")));
        this.displaySMILESonYaxisCheckBox = new CheckBox(Message.get("HistogramView.checkBoxSmilesTickLabel.text"));
        this.displaySMILESonYaxisCheckBox.setTooltip(GuiUtil.createTooltip(Message.get("HistogramView.checkBoxSmilesTickLabel.toolTip")));
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
     * Returns button for closing the view.
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
    public String getDisplayedFragmentsNumberTextFieldContent() {
        return this.displayedFragmentsNumberTextField.getText();
    }
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
    public String getMaximumSMILESLengthTextFieldContent() {
        return this.maximumSMILESLengthTextField.getText();
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
    //currently unused
    /**
     * Returns a CheckBox to make the number axis logarithmically.
     *
     * @return CheckBox for logarithmic number axis
     */
    public CheckBox getLogarithmicScale(){
        return this.logarithmicScale;
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
    public ComboBox<String> getBarWidthsComboBox() {
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
    public ComboBox<String> getFrequencyComboBox() {
        return this.frequencyComboBox;
    }
    //</editor-fold>
}
