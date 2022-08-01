/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2022  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.message.Message;

import javafx.geometry.Pos;
import javafx.geometry.Insets;
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
 * View for the frequency histogram
 *
 * @author Betuel Sevindik
 * @version 1.0.0.0
 */
public class HistogramView extends AnchorPane {
    //<editor-fold desc="private an private final class variables" defaultstate="collapsed">
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
    private TextField fragmentTextField;
    /**
     * ImageView to display the structures
     */
    private ImageView imageStructure;
    /**
     * Text field for creating a new histogram with the given SMILES length
     */
    private TextField smilesField;
    /**
     * Checkbox to make bar labels adjustable
     */
    private CheckBox checkbox;
    /**
     * CheckBox to display histogram gridlines
     */
    private CheckBox gridLinesCheckBox;
    /**
     * Label to show the highest number of fragments
     */
    private Label defaultFragmentLabel;
    /**
     * ScrollPane to make histogram scrollable
     */
    private ScrollPane scrollPane;
    /**
     * ComboBox to make the gap between the bars adjustable
     */
    private ComboBox comboBox;
    /**
     * CheckBox to scale the X-axis logarithmically
     */
    private CheckBox logarithmicScale;
    /**
     * CheckBox to add the bar shadow
     */
    private CheckBox stylingCheckBox;
    /**
     * CheckBox to make the display of tick labels adjustable
     *
     */
    private CheckBox smilesTickLabel;
    /**
     * ComboBox to choose which frequency is used
     */
    private ComboBox chooseDataComoBox;
    //</editor-fold>
    //
    /**
     * Constructor
     *
     * @param aMaxFragmentNumber indicates the maximum number of fragments available
     */
    public HistogramView(int aMaxFragmentNumber){
        super();
        this.scrollPane = new ScrollPane();
        this.scrollPane.setFitToHeight(true);
        this.scrollPane.setFitToWidth(true);
        this.scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        this.scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
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
        this.fragmentTextField = new TextField();
        this.fragmentTextField.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_WIDTH);
        this.fragmentTextField.setTooltip(new Tooltip(Message.get("HistogramView.textField.toolTip") + " "+aMaxFragmentNumber));
        this.applyButton = new Button(Message.get("HistogramView.refreshButton.text"));
        this.applyButton.setTooltip(new Tooltip(Message.get("HistogramView.refreshButton.toolTip")));
        this.applyButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.applyButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        this.smilesField = new TextField();
        this.smilesField.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_WIDTH);
        this.smilesField.setTooltip(new Tooltip(Message.get("HistogramView.smilesField.toolTip")));
        Label tmpSmilesLabel = new Label(Message.get("HistogramView.smilesLabel.text"));
        tmpSmilesLabel.setTooltip(new Tooltip(Message.get("HistogramView.smilesField.toolTip")));
        this.defaultFragmentLabel = new Label();
        this.defaultFragmentLabel.setTooltip(new Tooltip(Message.get("HistogramView.textField.toolTip") + " "+aMaxFragmentNumber));
        this.comboBox = new ComboBox<>();
        this.comboBox.getItems().add(Message.get("HistogramView.comboBox.item1.text"));
        this.comboBox.getItems().add(Message.get("HistogramView.comboBox.item2.text"));
        this.comboBox.getItems().add(Message.get("HistogramView.comboBox.item3.text"));
        this.comboBox.setValue(Message.get("HistogramView.comboBox.item3.text"));
        this.comboBox.setTooltip(new Tooltip(Message.get("HistogramView.comboBox.toolTip")));
        Label tmpGapSettingsLabel = new Label(Message.get("HistogramView.gapSettingLabel.text"));
        tmpGapSettingsLabel.setTooltip(new Tooltip(Message.get("HistogramView.comboBox.toolTip")));
        Label tmpChooseFrequencyLabel = new Label(Message.get("HistogramView.chooseDataComboBox.text"));
        tmpChooseFrequencyLabel.setTooltip(new Tooltip(Message.get("HistogramView.chooseDataComboBox.toolTip")));
        this.chooseDataComoBox = new ComboBox<>();
        this.chooseDataComoBox.getItems().add(Message.get("HistogramView.chooseDataComboBoxFragmentFrequency.text"));
        this.chooseDataComoBox.getItems().add(Message.get("HistogramView.chooseDataComboBoxMoleculeFrequency.text"));
        this.chooseDataComoBox.setValue(Message.get("HistogramView.chooseDataComboBoxFragmentFrequency.text"));
        this.chooseDataComoBox.setTooltip(new Tooltip(Message.get("HistogramView.chooseDataComboBox.toolTip")));
        tmpLeftSideGrid.setVgap(GuiDefinitions.GUI_INSETS_VALUE);
        tmpLeftSideGrid.setHgap(GuiDefinitions.GUI_INSETS_VALUE);
        tmpLeftSideGrid.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        // grid positions
        tmpLeftSideGrid.add(tmpGapSettingsLabel, 0,0);
        tmpLeftSideGrid.add(tmpChooseFrequencyLabel,0,1);
        tmpLeftSideGrid.add(this.comboBox,1,0);
        tmpLeftSideGrid.add(this.chooseDataComoBox,1,1);
        tmpLeftSideGrid.add(tmpSmilesLabel,2,0);
        tmpLeftSideGrid.add(this.defaultFragmentLabel,2,1);
        tmpLeftSideGrid.add(this.smilesField,3,0);
        tmpLeftSideGrid.add(this.fragmentTextField,3,1);
        tmpLeftSideGrid.add(this.applyButton,4,1);
        tmpHBoxLeftSideControls.setAlignment(Pos.CENTER_LEFT);
        tmpHBoxLeftSideControls.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpHBoxLeftSideControls.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(tmpHBoxLeftSideControls, Priority.ALWAYS);
        tmpHBoxLeftSideControls.getChildren().add(tmpLeftSideGrid);
        tmpMainHBoxControls.getChildren().add(tmpHBoxLeftSideControls);
        this.imageStructure = new ImageView();
        this.imageStructure.setEffect(new DropShadow(10,2,3, Color.BLACK));
        // right side controls
        this.closeButton = new Button(Message.get("HistogramView.cancelButton.text"));
        this.closeButton.setTooltip(new Tooltip(Message.get("HistogramView.cancelButton.toolTip")));
        this.closeButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        this.closeButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.checkbox = new CheckBox(Message.get("HistogramView.checkBox.text"));
        this.checkbox.setTooltip(new Tooltip(Message.get("HistogramView.checkBox.toolTip")));
        this.gridLinesCheckBox = new CheckBox(Message.get("HistogramView.checkBoxGridlines.text"));
        this.gridLinesCheckBox.setTooltip(new Tooltip(Message.get("HistogramView.checkBoxGridlines.toolTip")));
        this.logarithmicScale = new CheckBox(Message.get("HistogramView.checkBoxLogarithmicScale.text"));
        this.logarithmicScale.setTooltip(new Tooltip(Message.get("HistogramView.checkBoxLogarithmicScale.toolTip")));
        this.stylingCheckBox = new CheckBox(Message.get("HistogramView.stylingCheckBox.text"));
        this.stylingCheckBox.setTooltip(new Tooltip(Message.get("HistogramView.stylingCheckBox.tooltip")));
        this.smilesTickLabel = new CheckBox(Message.get("HistogramView.checkBoxSmilesTickLabel.text"));
        this.smilesTickLabel.setTooltip(new Tooltip(Message.get("HistogramView.checkBoxSmilesTickLabel.toolTip")));
        HBox tmpHBoxRightSideControls = new HBox();
        tmpRightSideGrid.setHgap(GuiDefinitions.GUI_INSETS_VALUE);
        tmpRightSideGrid.setVgap(GuiDefinitions.GUI_INSETS_VALUE*2);
        tmpRightSideGrid.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        // grid positions
        tmpRightSideGrid.add(this.checkbox,0,0);
        tmpRightSideGrid.add(this.stylingCheckBox,0,1);
        tmpRightSideGrid.add(this.gridLinesCheckBox,1,0);
        tmpRightSideGrid.add(this.smilesTickLabel,1,1);
        tmpRightSideGrid.add(this.closeButton,2,1);
        tmpHBoxRightSideControls.getChildren().add(tmpRightSideGrid);
        tmpHBoxRightSideControls.setAlignment(Pos.CENTER_RIGHT);
        tmpHBoxRightSideControls.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpHBoxRightSideControls.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(tmpHBoxRightSideControls, Priority.ALWAYS);
        tmpMainHBoxControls.getChildren().add(tmpHBoxRightSideControls);
        // main grid
        tmpMainGrid.add(this.scrollPane,0,0,4,4);
        tmpMainGrid.add(this.imageStructure,2,2);
        this.getChildren().add(tmpBorderPane);
    }
    //
    //<editor-fold desc="public methods" defaultstate="collapsed">
    /**
     * Returns cancelButton to close view
     *
     * @return Button to close histogram view
     */
    public Button getCloseButton() {
        return this.closeButton;
    }
    //
    /**
     * Returns a Button to display a new histogram with according fragments and SMILES length
     *
     * @return Button which takes over the changes in the histogram
     */
    public Button getApplyButton() {
        return this.applyButton;
    }
    //
    /**
     * Returns a String to get the fragment count
     *
     * @return String new number of fragments to be displayed in the histogram
     */
    public String getFragmentTextField() {return this.fragmentTextField.getText();}
    //
    /**
     * Returns the SMILES TextField which is deactivated if it is empty
     *
     * @return TextField for deactivation
     */
    public TextField getSmilesTextField() {
        return this.smilesField;
    }
    //
    /**
     * Returns the fragment TextField which is deactivated if it is empty
     *
     * @return TextField for deactivation
     */
    public TextField getFrequencyTextField() {
        return this.fragmentTextField;
    }
    //
    /**
     * Returns a String to get the SMILES length
     *
     * @return String corresponds to the SMiles length entered
     */
    public String getSmilesField() {return this.smilesField.getText();}
    //
    /**
     * Returns a ImageView to enable the display of the structures
     *
     * @return ImageView shows the different structures when hovering over the histogram
     */
    public ImageView getImageStructure() {
        return this.imageStructure;
    }
    //
    /**
     * Returns a CheckBox to label the histogram
     *
     * @return CheckBox to Labelling the bars with frequencies
     */
    public CheckBox getCheckbox() {
        return this.checkbox;
    }
    //
    /**
     * Returns a CheckBox to display the histogram gridlines
     *
     * @return CheckBox to set the grid lines in the histogram
     */
    public CheckBox getGridLinesCheckBox() {
        return this.gridLinesCheckBox;
    }
    //
    /**
     * Returns a CheckBox to make the number axis logarithmically
     *
     * @return CheckBox for logarithmic number axis
     */
    public CheckBox getLogarithmicScale(){
        return this.logarithmicScale;
    }
    //
    /**
     * Returns a CheckBox to add bar shadows
     *
     * @return CheckBox
     */
    public CheckBox getStylingCheckBox() {
        return this.stylingCheckBox;
    }
    //
    /**
     * Returns a Label to show the maximum number of fragments
     *
     * @return Label for display the maximum frequency in the histogram
     */
    public Label getDefaultFragmentLabel() {
        return this.defaultFragmentLabel;
    }
    //
    /**
     * Returns a ScrollPane in which the histogram is to be displayed
     *
     * @return ScrollPane to make histogram scrollable
     */
    public ScrollPane getScrollPane() {
        return this.scrollPane;
    }
    //
    /**
     * Returns a ComoBox containing 3 options for different gap sizes between the bars
     *
     * @return ComboBox
     */
    public ComboBox getComboBox() {
        return this.comboBox;
    }
    //
    /**
     * Returns a CheckBox to deactivate the tick labels on the Y-axis
     *
     * @return CheckBox
     */
    public CheckBox getSmilesTickLabel() {
        return this.smilesTickLabel;
    }
    //
    /**
     * Returns a ComboBox to choose which frequency is used
     *
     * @return ComboBox
     */
    public ComboBox getChooseDataComoBox() {
        return this.chooseDataComoBox;
    }
    //</editor-fold>
}
