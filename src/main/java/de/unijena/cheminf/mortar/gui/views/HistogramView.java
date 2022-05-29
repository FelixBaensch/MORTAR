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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * View for the frequency histogram
 *
 * @author Betuel Sevindik
 */
public class HistogramView extends AnchorPane {

    //<editor-fold desc="private an private final class variables" defaultstate="collapsed">
    /**
     * Border line to outline the structure
     */
    private  final Border black = new Border(new BorderStroke(Color.BLACK,
            BorderStrokeStyle.SOLID, new CornerRadii(0), new BorderWidths(1)));
    /**
     * Button to cancel changes and close view
     */
    private Button cancelButton;
    /**
     * Button to refresh the histogram
     */
    private Button refreshButton;
    /**
     * Button to export the histogram as... TODO
     */
    private Button imageButton;
    private TextField textField;
    private StackPane ImagePane;
    private ImageView ImageStructure;
    //</editor-fold>
    //
    /**
     * Constructor
     * @param aHistogramChart
     */
    public HistogramView(BarChart aHistogramChart){
        super();
        //add a ScrollPane to make the histogram scrollable
        ScrollPane tmpScrollPane = new ScrollPane();
        tmpScrollPane.setFitToHeight(true);
        tmpScrollPane.setFitToWidth(true);
        tmpScrollPane.setContent(aHistogramChart);
        tmpScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        tmpScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        //borderPane
        BorderPane tmpBorderPane = new BorderPane();
        HistogramView.setTopAnchor(tmpBorderPane, 0.0);
        HistogramView.setRightAnchor(tmpBorderPane, 0.0);
        HistogramView.setLeftAnchor(tmpBorderPane, 0.0);
        HistogramView.setBottomAnchor(tmpBorderPane, 0.0);
        //buttons
        HBox tmpHBoxButtonsHBox = new HBox();
        tmpHBoxButtonsHBox.setStyle("-fx-background-color: LightGrey");
        tmpBorderPane.setBottom(tmpHBoxButtonsHBox);
        tmpBorderPane.setCenter(tmpScrollPane);
        HBox tmpHBoxLeftSideButton = new HBox();
        /** // TODO building an imageButton (to export the histogram) after the creation of a window for the fragment structures
        this.imageButton = new Button(Message.get("Histogram.imageButton.text"));
        this.imageButton.setTooltip(new Tooltip(Message.get("Histogram.imageButton.toolTip")));
        tmpHBoxLeftSideButton.getChildren().add(this.imageButton);
        tmpHBoxLeftSideButton.setAlignment(Pos.CENTER_LEFT);
        tmpHBoxLeftSideButton.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpHBoxLeftSideButton.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(tmpHBoxLeftSideButton, Priority.ALWAYS);
        tmpHBoxButtonsHBox.getChildren().add(tmpHBoxLeftSideButton);
         */
        HBox tmpHBoxRightSideButtons = new HBox();
        //Image
        HBox tmpImageBox = new HBox();
        this.ImagePane = new StackPane();
        this.ImagePane.setPrefWidth(GuiDefinitions.GUI_IMAGE_PANE_WIDTH);
        this.ImagePane.setPrefHeight(GuiDefinitions.GUI_IMAGE_PANE_HEIGHT);
        this.ImagePane.setBorder(black);
        this.ImageStructure = new ImageView();
        this.ImageStructure.setPreserveRatio(true);

        tmpImageBox.setAlignment(Pos.CENTER_LEFT);
        tmpImageBox.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpImageBox.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(tmpImageBox, Priority.ALWAYS);
        tmpHBoxButtonsHBox.getChildren().add(tmpImageBox);
        this.ImagePane.getChildren().add(this.ImageStructure);
        tmpImageBox.getChildren().add(this.ImagePane);
        this.cancelButton = new Button(Message.get("HistogramView.cancelButton.text"));
        this.cancelButton.setTooltip(new Tooltip(Message.get("HistogramView.cancelButton.toolTip")));
        //this.sortButton = new Button(Message.get("HistogramView.sortButton.text"));
        //this.sortButton.setTooltip(new Tooltip(Message.get("Histogram.sortButton.toolTip")));
        this.textField = new TextField();
        this.textField.setPrefWidth(GuiDefinitions.GUI_TEXT_FIELD_WIDTH);
       // this.textField.setText(String.valueOf(aDefaultText.size() + " max. Fragments"));
        this.textField.setTooltip(new Tooltip(Message.get("HistogramView.textField.toolTip")));
        this.refreshButton = new Button(Message.get("HistogramView.refreshButton.text"));
        this.refreshButton.setTooltip(new Tooltip(Message.get("HistogramView.refreshButton.toolTip")));
        tmpHBoxRightSideButtons.getChildren().addAll(this.textField, this.refreshButton,this.cancelButton);
        tmpHBoxRightSideButtons.setAlignment(Pos.CENTER_RIGHT);
        tmpHBoxRightSideButtons.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpHBoxRightSideButtons.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(tmpHBoxRightSideButtons, Priority.ALWAYS);
        tmpHBoxButtonsHBox.getChildren().add(tmpHBoxRightSideButtons);
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
     * Returns a Button to display a new histogram with according fragments
     *
     * @return Button
     */
    public Button getRefreshButton() {
        return this.refreshButton;
    }
    public String getTextField() {return this.textField.getText(); }
    /**
     * Returns a text field to specify the number of fragments
     *
     * @return ImageView
     */
    public ImageView getImageStructure() {
        return this.ImageStructure;
    }
    public TextField getText() {return this.textField;}
    //</editor-fold>
}
