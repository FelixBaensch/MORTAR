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

package de.unijena.cheminf.mortar.gui.controls;

import static de.unijena.cheminf.mortar.gui.util.GuiDefinitions.GUI_STATUSBAR_HEIGHT_VALUE;

import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.FlowPane;

/**
 * StatusBar to show status of the application and progress of a running task.
 *
 * @author Felix Baensch, Jonas Schaub
 * @version 1.0.0.0
 */
public class StatusBar extends FlowPane {
    //<editor-fold desc="private final class constants" defaultstate="collapsed">
    /**
     * Label to show status message.
     */
    private final Label statusLabel;
    /**
     * ProgressBar.
     */
    private final ProgressBar progressBar;
    //</editor-fold>
    //
    /**
     * Constructor.
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
     * Returns statusLabel.
     *
     * @return Label
     */
    public Label getStatusLabel() {
        return this.statusLabel;
    }
    //
    /**
     * Returns the progressBar.
     *
     * @return ProgressBar
     */
    public ProgressBar getProgressBar() {
        return this.progressBar;
    }
    //</editor-fold>
}
