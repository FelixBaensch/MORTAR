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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * View for the frequency histogram
 * @author Betuel Sevindik
 */
public class HistogramView extends AnchorPane {

    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * Button to cancel changes and close view
     */
    private Button cancelButton;
    /**
     * Button to sort the fragment frequency in the histogram in descending order
     */
    private Button sortButton;
    /**
     * Button to export the histogram as...
     */
    private Button imageButton;
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
        /**
        tmpScrollPane.setFitToHeight(true);
        tmpScrollPane.setFitToWidth(true);
         */
        tmpScrollPane.setContent(aHistogramChart);
        tmpScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        tmpScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        //borderPane
        BorderPane tmpBorderPane = new BorderPane();
        SettingsView.setTopAnchor(tmpBorderPane, 0.0);
        SettingsView.setRightAnchor(tmpBorderPane, 0.0);
        SettingsView.setLeftAnchor(tmpBorderPane, 0.0);
        SettingsView.setBottomAnchor(tmpBorderPane, 0.0);
        //buttons
        HBox tmpHBoxButtonsHBox = new HBox();
        tmpHBoxButtonsHBox.setStyle("-fx-background-color: LightGrey");
        tmpBorderPane.setBottom(tmpHBoxButtonsHBox);
        tmpBorderPane.setCenter(tmpScrollPane);
        HBox tmpHBoxLeftSideButton = new HBox();
        this.imageButton = new Button(Message.get("Histogram.imageButton.text"));
        this.imageButton.setTooltip(new Tooltip(Message.get("Histogram.imageButton.toolTip")));
        tmpHBoxLeftSideButton.getChildren().add(this.imageButton);
        tmpHBoxLeftSideButton.setAlignment(Pos.CENTER_LEFT);
        tmpHBoxLeftSideButton.setSpacing(GuiDefinitions.GUI_SPACING_VALUE);
        tmpHBoxLeftSideButton.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        HBox.setHgrow(tmpHBoxLeftSideButton, Priority.ALWAYS);
        tmpHBoxButtonsHBox.getChildren().add(tmpHBoxLeftSideButton);
        HBox tmpHBoxRightSideButtons = new HBox();
        this.cancelButton = new Button(Message.get("HistogramView.cancelButton.text"));
        this.cancelButton.setTooltip(new Tooltip(Message.get("HistogramView.cancelButton.toolTip")));
        this.sortButton = new Button(Message.get("HistogramView.sortButton.text"));
        this.sortButton.setTooltip(new Tooltip(Message.get("Histogram.sortButton.toolTip")));
        tmpHBoxRightSideButtons.getChildren().addAll(this.sortButton, this.cancelButton);
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
     * Returns SortButton to sort the histogram by descending frequency
     *
     * @return Button
     */
    public Button getSortButton() {
        return this.sortButton;
    }
    //</editor-fold>
}
