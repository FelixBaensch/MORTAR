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

import de.unijena.cheminf.mortar.configuration.IConfiguration;
import de.unijena.cheminf.mortar.gui.controls.MainMenuBar;
import de.unijena.cheminf.mortar.gui.controls.StatusBar;

import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * MainView class of MORTAR.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class MainView extends AnchorPane {
    //<editor-fold desc="private class variables" defaultstate="collapsed">
    private final BorderPane mainBorderPane;
    private final Pane mainCenterPane;
    private final MainMenuBar mainMenuBar;
    private final StatusBar statusBar;
    /**
     * Configuration class to read resource file paths from.
     */
    private final IConfiguration configuration;
    //</editor-fold>
    /**
     * Initialises the variables and fields and adds the components to the frame.
     * No event listeners are added to any component.
     *
     * @param aConfiguration configuration class reading from properties file
     */
    public MainView(IConfiguration aConfiguration) {
        super();
        this.configuration = aConfiguration;
        //BorderPane
        this.mainBorderPane = new BorderPane();
        AnchorPane.setTopAnchor(this.mainBorderPane, 0.0);
        AnchorPane.setBottomAnchor(this.mainBorderPane, 0.0);
        AnchorPane.setLeftAnchor(this.mainBorderPane, 0.0);
        AnchorPane.setRightAnchor(this.mainBorderPane, 0.0);
        HBox.setHgrow(this.mainBorderPane, Priority.ALWAYS);
        VBox.setVgrow(this.mainBorderPane, Priority.ALWAYS);
        //mainCenterPane
        this.mainCenterPane = new Pane();
        this.mainCenterPane.setStyle("-fx-background-color: LIGHTGREY");
        String tmpLogoURL = this.getClass().getClassLoader().getResource(
                this.configuration.getProperty("mortar.imagesFolder")
                        + this.configuration.getProperty("mortar.logo.withHalfAlpha.name")).toExternalForm();
        this.mainCenterPane.setStyle("-fx-background-image: url('" + tmpLogoURL + "'); "
                + "-fx-background-repeat: no-repeat; "
                + "-fx-background-size: 521 362; "
                + "-fx-background-position: center center;");
        this.mainBorderPane.setCenter(this.mainCenterPane);
        //menuBar
        this.mainMenuBar = new MainMenuBar();
        //on Mac, show as a standard OS X menu bar, at the top of the screen
        String tmpOS = System.getProperty("os.name").toUpperCase();
        if (tmpOS != null && tmpOS.contains("MAC")) {
            this.mainMenuBar.useSystemMenuBarProperty().set(true);
        }
        this.mainBorderPane.setTop(this.mainMenuBar);
        //statusBar
        this.statusBar = new StatusBar();
        this.mainBorderPane.setBottom(this.statusBar);
        this.getChildren().add(this.mainBorderPane);
    }
    //<editor-fold desc="public properties" defaultstate="collapsed">
    //<editor-fold desc="getMainMenuBar" defaultstate="collapsed">
    /**
     * Returns the main menubar that contains menus for file handling (I/O), shutting down the application, settings and help menu entries.
     *
     * @return main menubar
     */
    public MainMenuBar getMainMenuBar() {
        return this.mainMenuBar;
    }
    //</editor-fold>
    //
    //<editor-fold desc="getMainCenterPane" defaultstate="collapsed">
    /**
     * Returns the main center pane that contains the GUI elements.
     *
     * @return main center pane that is supposed to contain GUI elements of interest
     */
    public Pane getMainCenterPane() {
        return this.mainCenterPane;
    }
    //</editor-fold>
    //
    //<editor-fold desc="getMainMenuBar" defaultstate="collapsed">
    /**
     * Returns the status bar.
     *
     * @return statusBar
     */
    public StatusBar getStatusBar() {
        return this.statusBar;
    }
    //</editor-fold>
    //</editor-fold>
}
