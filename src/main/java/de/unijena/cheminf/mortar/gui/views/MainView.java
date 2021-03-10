/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2020  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas-schaub@uni-jena.de)
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

import de.unijena.cheminf.mortar.gui.panes.MainMenuBar;
import de.unijena.cheminf.mortar.gui.panes.StatusBar;
import javafx.scene.layout.*;

/**
 * MainView Class of MORTAR
 *
 * @author Felix Baensch
 */
public class MainView extends AnchorPane {

    //<editor-fold desc="private class variables" defaultstate="collapsed">
    private BorderPane mainBorderPane;
    private Pane mainCenterPane;
    private MainMenuBar mainMenuBar;
    private StatusBar statusBar;

    //</editor-fold>

    /**
     * Constructor
     *
     * Initialises the variables and fields and adds the components to the frame.
     * No event listeners are added to any component.
     */
    public MainView(){
        super();
        //BorderPane
        this.mainBorderPane = new BorderPane();
        MainView.setTopAnchor(this.mainBorderPane, 0.0);
        MainView.setBottomAnchor(this.mainBorderPane, 0.0);
        MainView.setLeftAnchor(this.mainBorderPane, 0.0);
        MainView.setRightAnchor(this.mainBorderPane, 0.0);
        HBox.setHgrow(this.mainBorderPane, Priority.ALWAYS);
        VBox.setVgrow(this.mainBorderPane, Priority.ALWAYS);
        //mainCenterPane
        this.mainCenterPane = new Pane();
        this.mainCenterPane.setStyle("-fx-background-color: LIGHTGREY");
        this.mainBorderPane.setCenter(this.mainCenterPane);
        //menuBar
        this.mainMenuBar = new MainMenuBar();
        this.mainBorderPane.setTop(this.mainMenuBar);
        //statusBar
        this.statusBar = new StatusBar();
        this.mainBorderPane.setBottom(this.statusBar);
        this.getChildren().add(this.mainBorderPane);
    }

    //<editor-fold desc="public properties" defaultstate="collapsed">
    //<editor-fold desc="getMainMenuBar" defaultstate="collapsed">
    /**
     * Returns the main menubar that contains menus for file handling (I/O), shutting down the application, settings and help menu entries
     * @return main menubar
     */
    public MainMenuBar getMainMenuBar() {
        return this.mainMenuBar;
    }
    //</editor-fold>
    //
    //<editor-fold desc="getMainCenterPane" defaultstate="collapsed">
    /**
     * Returns the main center pane that contains
     * @return main center pane that is supposed to contain GUI elements of interest
     */
    public Pane getMainCenterPane() {
        return this.mainCenterPane;
    }
    //</editor-fold>
    //
    //<editor-fold desc="getMainMenuBar" defaultstate="collapsed">
    /**
     * Returns the status bar
     * @return statusBar
     */
    public StatusBar getStatusBar() {
        return this.statusBar;
    }
    //</editor-fold>
    //</editor-fold>
}
