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
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

/**
 * View for the frequency histogram
 *
 * @author Betuel Sevindik
 */
public class HistogramView extends AnchorPane {

    //<editor-fold desc="private an private final class variables" defaultstate="collapsed">
    /**
     * Button to cancel changes and close view
     */
    private Button cancelButton;
    /**
     * Button to refresh the histogram
     */
    private Button refreshButton;
    /**
     * Text field for creating a new histogram
     */
    private TextField textField;
    /**
     * ImageView to display the structures
     */
    private ImageView imageStructure;
    /**
     * Text field for creating a new histogram
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
    private Label defaultLabel;
    /**
     * ScrollPane to make histogram scrollable
     */
    private ScrollPane scrollPane;
    /**
     * ComboBox to make the gap between the bars adjustable
     */
    private ComboBox comboBox;
    private CheckBox logarithmicScale;

    //</editor-fold>
    //
    /**
     * Constructor
     * @param
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
        //grid
        GridPane tmpGrid = new GridPane();
        RowConstraints tmpRow1 = new RowConstraints();
        tmpRow1.setVgrow(Priority.ALWAYS);
        tmpRow1.setFillHeight(true);
        tmpGrid.getRowConstraints().add(tmpRow1);
        ColumnConstraints tmpCol1 = new ColumnConstraints();
        tmpCol1.setFillWidth(true);
        tmpCol1.setHgrow(Priority.ALWAYS);
        tmpGrid.getColumnConstraints().add(tmpCol1);
        ColumnConstraints tmpCol2 = new ColumnConstraints();
        tmpGrid.getColumnConstraints().add(tmpCol2);
        RowConstraints tmpRow2 = new RowConstraints();
        tmpGrid.getRowConstraints().add(tmpRow2);
        ColumnConstraints tmpCol3 = new ColumnConstraints();
        tmpGrid.getColumnConstraints().add(tmpCol3);
        RowConstraints tmpRow3 = new RowConstraints();
        tmpGrid.getRowConstraints().add(tmpRow3);
        RowConstraints tmpRow4 = new RowConstraints(20); // magic number
        tmpGrid.getRowConstraints().add(tmpRow4);
        ColumnConstraints tmpCol4 = new ColumnConstraints(20); // magic number
        tmpGrid.getColumnConstraints().add(tmpCol4);
        //controls
        HBox tmpHBoxButtonsHBox = new HBox();
        tmpHBoxButtonsHBox.setStyle("-fx-background-color: LightGrey");
        tmpBorderPane.setBottom(tmpHBoxButtonsHBox);
        tmpBorderPane.setCenter(tmpGrid);
        HBox tmpHBoxLeftSideButton = new HBox();
        this.textField = new TextField();
        this.textField.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_WIDTH);
        this.textField.setTooltip(new Tooltip(Message.get("HistogramView.textField.toolTip") + " "+aMaxFragmentNumber));
        TextFormatter<Integer> tmpFormatter = new TextFormatter<>(GuiUtil.getStringToIntegerConverter(), 0, GuiUtil.getIntegerFilter());
        this.textField.setTextFormatter(tmpFormatter);
        this.refreshButton = new Button(Message.get("HistogramView.refreshButton.text"));
        this.refreshButton.setTooltip(new Tooltip(Message.get("HistogramView.refreshButton.toolTip")));
        this.refreshButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.refreshButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        this.smilesField = new TextField(); // TODO tooltip smilesText
        this.smilesField.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_WIDTH);
        this.smilesField.setTooltip(new Tooltip(Message.get("HistogramView.smilesField.toolTip")));
        Label tmpSmilesLabel = new Label(Message.get("HistogramView.smilesField.text"));
        this.defaultLabel = new Label();
        this.comboBox = new ComboBox<>();
        this.comboBox.getItems().add("Low");
        this.comboBox.getItems().add("Medium");
        this.comboBox.getItems().add("High");
        this.comboBox.setValue("High");
        Label tmpGapSettingsLabel = new Label("Gap setting");
        tmpHBoxLeftSideButton.getChildren().addAll(tmpGapSettingsLabel,this.comboBox,tmpSmilesLabel,this.smilesField,this.defaultLabel, this.textField, this.refreshButton);
        tmpHBoxLeftSideButton.setAlignment(Pos.CENTER_LEFT);
        tmpHBoxLeftSideButton.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpHBoxLeftSideButton.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(tmpHBoxLeftSideButton, Priority.ALWAYS);
        tmpHBoxButtonsHBox.getChildren().add(tmpHBoxLeftSideButton);
        this.imageStructure = new ImageView();
        this.imageStructure.setEffect(new DropShadow(10,2,3, Color.BLACK));
        this.cancelButton = new Button(Message.get("HistogramView.cancelButton.text"));
        this.cancelButton.setTooltip(new Tooltip(Message.get("HistogramView.cancelButton.toolTip")));
        this.cancelButton.setPrefHeight(GuiDefinitions.GUI_BUTTON_HEIGHT_VALUE);
        this.cancelButton.setPrefWidth(GuiDefinitions.GUI_BUTTON_WIDTH_VALUE);
        this.checkbox = new CheckBox(Message.get("HistogramView.checkBox.text"));
        this.checkbox.setTooltip(new Tooltip(Message.get("HistogramView.checkBox.toolTip")));
        this.gridLinesCheckBox = new CheckBox(Message.get("HistogramView.checkBoxGridlines.text"));
        this.gridLinesCheckBox.setTooltip(new Tooltip(Message.get("HistogramView.checkBoxGridlines.toolTip")));
        this.logarithmicScale = new CheckBox("logarithmic scale");
        HBox tmpHBoxRightSideButtons = new HBox();
        tmpHBoxRightSideButtons.getChildren().addAll(this.logarithmicScale,this.gridLinesCheckBox,this.checkbox, this.cancelButton);
        tmpHBoxRightSideButtons.setAlignment(Pos.CENTER_RIGHT);
        tmpHBoxRightSideButtons.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpHBoxRightSideButtons.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(tmpHBoxRightSideButtons, Priority.ALWAYS);
        tmpHBoxButtonsHBox.getChildren().add(tmpHBoxRightSideButtons);
        tmpGrid.add(this.scrollPane,0,0,4,4);
        tmpGrid.add(this.imageStructure,2,2);
        this.getChildren().add(tmpBorderPane);
    }
    //
    //<editor-fold desc="public methods" defaultstate="collapsed">
    /**
     * Returns cancelButton to close view
     *
     * @return Button
     */
    public Button getCancelButton() {
        return this.cancelButton;
    }
    //
    /**
     * Returns a Button to display a new histogram with according fragments and SMILES length
     *
     * @return Button
     */
    public Button getRefreshButton() {
        return this.refreshButton;
    }
    //
    /**
     * Returns a String to get the fragment count
     *
     * @return String
     */
    public String getTextField() {return this.textField.getText(); }
    //
    /**
     * Returns a TextField
     *
     * @return TextField
     */
    public TextField getSmilesTextField() {
        return this.smilesField;
    }
    //
    /**
     * Returns a TextField
     *
     * @return TextField
     */
    public TextField getFrequencyTextField() {
        return this.textField;
    }
    //
    /**
     * Returns a String to get the SMILES length
     *
     * @return
     */
    public String getSmilesField() {return this.smilesField.getText();}
    //
    /**
     * Returns a ImageView to enable the display of the structures
     *
     * @return ImageView
     */
    public ImageView getImageStructure() {
        return this.imageStructure;
    }
    //
    /**
     * Returns a CheckBox to label the histogram
     *
     * @return CheckBox
     */
    public CheckBox getCheckbox() {
        return this.checkbox;
    }
    //
    /**
     * Returns a CheckBox to display the histogram gridlines
     *
     * @return CheckBox
     */
    public CheckBox getGridLinesCheckBox() {
        return this.gridLinesCheckBox;
    }
    public CheckBox getLogarithmicScale(){
        return this.logarithmicScale;
    }
    //
    /**
     * Returns a Label to show the maximum number of fragments
     *
     * @return Label
     */
    public Label getDefaultLabel() {
        return this.defaultLabel;
    }
    public ScrollPane getScrollPane() {
        return this.scrollPane;
    }
    public ComboBox getComboBox() {
        return this.comboBox;
    }
    //</editor-fold>
}
