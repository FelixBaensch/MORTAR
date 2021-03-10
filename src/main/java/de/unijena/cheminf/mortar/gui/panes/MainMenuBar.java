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

package de.unijena.cheminf.mortar.gui.panes;

import de.unijena.cheminf.mortar.gui.views.MainView;
import de.unijena.cheminf.mortar.message.Message;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/**
 * A MenuBar for the application's {@link MainView}.
 * It contains menus for file handling (I/O), shutting down the application, settings and help menu entries
 *
 * @author Felix Baensch, Jonas Schaub
 */
public class MainMenuBar extends MenuBar {

    //<editor-fold desc="private class variables" defaultstate="collapsed">
    private MenuBar menuBar;
    private Menu fileMenu;
    private Menu settingsMenu;
    private Menu helpMenu;
    private MenuItem exitMenuItem;
    private MenuItem loadMenuItem;
    private MenuItem fragmentationSettingsMenuItem;
    //</editor-fold>
    //
    //<editor-fold desc="constructor" defaultstate="collapsed">
    /**
     * Constructor
     * It initialises the MainMenuBar's components, adds them to the MainMenuBar.
     * No listeners were added here.
     */
    public MainMenuBar(){
        super();
        //<editor-fold desc="initialisation" defaultstate="collapsed">
        //fileMenu
        this.fileMenu = new Menu(Message.get("MainView.menuBar.fileMenu.text"));
        this.loadMenuItem = new MenuItem(Message.get("MainView.menuBar.fileMenu.loadMenuItem.text"));
        this.exitMenuItem = new MenuItem(Message.get("MainView.menuBar.fileMenu.exitMenuItem.text"));
        //settingsMenu
        this.settingsMenu = new Menu(Message.get("MainView.menuBar.settingsMenu.text"));
        this.fragmentationSettingsMenuItem = new MenuItem(Message.get("MainView.menuBar.settingsMenu.fragmentationSettingsMenuItem.text"));
        //helpMenu
        this.helpMenu = new Menu(Message.get("MainView.menuBar.helpMenu.text"));
        //</editor-fold>
        this.addComponentsToMenuBar();
    }
    //</editor-fold>
    //
    //<editor-fold desc="private methods" defaultstate="collapsed">
    //<editor-fold desc="addComponentsToMenuBar" defaultstate="collapsed">
    /**
     * Adds the menu items to the menus and the menus to the menu bar
     */
    private void addComponentsToMenuBar(){
        //<editor-fold desc="fileMenu" defaultstate="collapsed">
        this.getMenus().add(this.fileMenu);
        //loadMenuItem
        this.fileMenu.getItems().add(this.loadMenuItem);
        //separator
        this.fileMenu.getItems().add(new SeparatorMenuItem());
        //exitMenuItem
        this.fileMenu.getItems().add(this.exitMenuItem);
        //</editor-fold>
        //<editor-fold desc="settingsMenu" defaultstate="collapsed">
        this.getMenus().add(this.settingsMenu);
        //fragmentationSettingsMenuItem
        this.settingsMenu.getItems().add(this.fragmentationSettingsMenuItem);
        //</editor-fold>
        //<editor-fold desc="helpMenu" defaultstate="collapsed">
        this.getMenus().add(this.helpMenu);
        //</editor-fold>
    }
    //</editor-fold>
    //</editor-fold>
    //
    //<editor-fold desc="public properties" defaultstate="collapsed">
    //<editor-fold desc="getLoadMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to load a molecule set
     *
     * @return the menu item that should load a molecule set
     */
    public MenuItem getLoadMenuItem() {
        return this.loadMenuItem;
    }
    //</editor-fold>
    //<editor-fold desc="getExitMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to shut down the application
     *
     * @return the menu item that should shut down the application
     */
    public MenuItem getExitMenuItem() {
        return this.exitMenuItem;
    }
    //</editor-fold>
    //<editor-fold desc="getFragmentationSettingsMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to open the fragmentation settings window
     *
     * @return the menu item that should open the fragmentation settings window
     */
    public MenuItem getFragmentationSettingsMenuItem() {
        return this.fragmentationSettingsMenuItem;
    }
    //</editor-fold>
    //</editor-fold>
}
