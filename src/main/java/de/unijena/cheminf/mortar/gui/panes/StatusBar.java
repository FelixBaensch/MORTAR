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

package de.unijena.cheminf.mortar.gui.panes;

import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.FlowPane;

import static de.unijena.cheminf.mortar.gui.util.GuiDefinitions.GUI_STATUSBAR_HEIGHT_VALUE;

/**
 * StatusBar to show status of the application and progress of a running task
 *
 * @author Felix Baensch, Jonas Schaub
 */
public class StatusBar extends FlowPane {

    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * Label to show status message
     */
    private Label statusLabel;
    /**
     * ProgressBar
     */
    private ProgressBar progressBar;
    //</editor-fold>
    //
    /**
     * Constructor
     */
    public StatusBar(){
        super();
        this.setStyle("-fx-background-color: DarkGrey");
        this.statusLabel = new Label();
        this.progressBar = new ProgressBar();
        this.progressBar.visibleProperty().setValue(false);
        this.setMinHeight(GUI_STATUSBAR_HEIGHT_VALUE);
        this.setPrefHeight(GUI_STATUSBAR_HEIGHT_VALUE);
        this.setMaxHeight(GUI_STATUSBAR_HEIGHT_VALUE);
//        this.setPadding(new Insets(GuiDefinitions.GUI_INSETS_VALUE));
        this.setPadding(new Insets( 3));
        this.setHgap(GuiDefinitions.GUI_INSETS_VALUE);
        this.getChildren().addAll(statusLabel, progressBar);
    }
    //
    //<editor-fold desc="properties" defaultstate="collapsed">
    /**
     * Returns statusLabel
     * @return Label
     */
    public Label getStatusLabel() {
        return this.statusLabel;
    }
    //
    /**
     * Returns the progressBar
     * @return ProgressBar
     */
    public ProgressBar getProgressBar() {
        return this.progressBar;
    }
    //</editor-fold>
}

