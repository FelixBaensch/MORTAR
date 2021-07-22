/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2021  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas-schaub@uni-jena.de)
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
 * @author Felix Baensch, Jonas Schaub, Samuel Behr
 */
public class MainMenuBar extends MenuBar {

    //<editor-fold desc="private class variables" defaultstate="collapsed">
    private MenuBar menuBar;
    private Menu fileMenu;
    private Menu settingsMenu;
    private Menu helpMenu;
    private MenuItem exitMenuItem;
    private MenuItem loadMenuItem;
    private Menu exportMenu;
    private Menu fragmentsExportMenu;
    private MenuItem fragmentsExportToCSVMenuItem;
    private MenuItem fragmentsExportToPDBMenuItem;
    private MenuItem fragmentsExportToPDFMenuItem;
    private Menu fragmentsExportToSDFMenu;
    private MenuItem fragmentsExportToSingleSDFMenuItem;
    private MenuItem fragmentsExportToSeparateSDFsMenuItem;
    private Menu itemsExportMenu;
    private MenuItem itemsExportToCSVMenuItem;
    private MenuItem itemsExportToPDFMenuItem;
    private MenuItem fragmentationSettingsMenuItem;
    private MenuItem globalSettingsMenuItem;
    private Menu fragmentationAlgorithmMenu;
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
        this.exportMenu = new Menu(Message.get("MainView.menuBar.fileMenu.exportMenu.text"));
        //<editor-fold desc="exportMenu components" defaultstate="collapsed">
        //fragmentsExportMenu
        this.fragmentsExportMenu = new Menu(Message.get("MainView.menuBar.fileMenu.exportMenu.fragmentsExportMenu.text"));
        //components
        this.fragmentsExportToCSVMenuItem = new MenuItem(Message.get("MainView.menuBar.fileMenu.exportMenu.toCSV.text"));
        this.fragmentsExportToPDBMenuItem = new MenuItem(Message.get("MainView.menuBar.fileMenu.exportMenu.toPDB.text"));
        this.fragmentsExportToPDFMenuItem = new MenuItem(Message.get("MainView.menuBar.fileMenu.exportMenu.toPDF.text"));
        this.fragmentsExportToSDFMenu = new Menu(Message.get("MainView.menuBar.fileMenu.exportMenu.toSDF.text"));
        this.fragmentsExportToSingleSDFMenuItem = new MenuItem(Message.get("MainView.menuBar.fileMenu.exportMenu.asOneFile.text"));
        this.fragmentsExportToSeparateSDFsMenuItem = new MenuItem(Message.get("MainView.menuBar.fileMenu.exportMenu.asSeparateFiles.text"));
        //itemsExportMenu
        this.itemsExportMenu = new Menu(Message.get("MainView.menuBar.fileMenu.exportMenu.itemsExportMenu.text"));
        //components
        this.itemsExportToCSVMenuItem = new MenuItem(Message.get("MainView.menuBar.fileMenu.exportMenu.toCSV.text"));
        this.itemsExportToPDFMenuItem = new MenuItem(Message.get("MainView.menuBar.fileMenu.exportMenu.toPDF.text"));
        //</editor-fold>
        this.exitMenuItem = new MenuItem(Message.get("MainView.menuBar.fileMenu.exitMenuItem.text"));
        //settingsMenu
        this.settingsMenu = new Menu(Message.get("MainView.menuBar.settingsMenu.text"));
        this.globalSettingsMenuItem = new MenuItem(Message.get("MainView.menuBar.settingsMenu.globalSettingsMenuItem.text"));
        this.fragmentationAlgorithmMenu = new Menu(Message.get("MainView.menuBar.settingsMenu.fragmentationAlgorithmMenuItem.text"));
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
        //exportMenu
        this.fileMenu.getItems().add(this.exportMenu);
        this.addComponentsToExportMenu();
        this.exportMenu.setDisable(true);
        //separator
        this.fileMenu.getItems().add(new SeparatorMenuItem());
        //exitMenuItem
        this.fileMenu.getItems().add(this.exitMenuItem);
        //</editor-fold>
        //<editor-fold desc="settingsMenu" defaultstate="collapsed">
        this.getMenus().add(this.settingsMenu);
        //globalSettingsMenuItem
        this.settingsMenu.getItems().add(this.globalSettingsMenuItem);
        //separator
        this.settingsMenu.getItems().add(new SeparatorMenuItem());
        //fragmentationAlgorithmMenu
        this.settingsMenu.getItems().add(this.fragmentationAlgorithmMenu);
        //fragmentationSettingsMenuItem
        this.settingsMenu.getItems().add(this.fragmentationSettingsMenuItem);
        //</editor-fold>
        //<editor-fold desc="helpMenu" defaultstate="collapsed">
        this.getMenus().add(this.helpMenu);
        //</editor-fold>
    }
    //</editor-fold>
    //<editor-fold desc="addComponentsToExportMenu" defaultstate="collapsed">
    /**
     * Adds the menu items to the export menu
     */
    private void addComponentsToExportMenu(){
        //<editor-fold desc="fragmentsExportMenu" defaultstate="collapsed">
        this.exportMenu.getItems().add(this.fragmentsExportMenu);
        this.fragmentsExportMenu.getItems().add(this.fragmentsExportToCSVMenuItem);
        this.fragmentsExportMenu.getItems().add(this.fragmentsExportToPDBMenuItem);
        this.fragmentsExportMenu.getItems().add(this.fragmentsExportToPDFMenuItem);
        this.fragmentsExportMenu.getItems().add(this.fragmentsExportToSDFMenu);
        this.fragmentsExportToSDFMenu.getItems().add(this.fragmentsExportToSingleSDFMenuItem);
        this.fragmentsExportToSDFMenu.getItems().add(this.fragmentsExportToSeparateSDFsMenuItem);
        this.exportMenu.show();
        //</editor-fold>
        //<editor-fold desc="itemsExportMenu" defaultstate="collapsed">
        this.exportMenu.getItems().add(this.itemsExportMenu);
        this.itemsExportMenu.getItems().add(this.itemsExportToCSVMenuItem);
        this.itemsExportMenu.getItems().add(this.itemsExportToPDFMenuItem);
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
    //<editor-fold desc="getExportMenu" defaultstate="collapsed">
    /**
     * Returns the menu that is supposed to open a list of export options
     *
     * @return the menu that should open a list of export options
     */
    public Menu getExportMenu() {
        return this.exportMenu;
    }
    //</editor-fold>
    //<editor-fold desc="getFragmentsExportMenu" defaultstate="collapsed">
    /**
     * Returns the menu that is supposed to open a list of fragments export options
     *
     * @return the menu that should open a list of fragments export options
     */
    public Menu getFragmentsExportMenu() {
        return this.fragmentsExportMenu;
    }
    //</editor-fold>
    //<editor-fold desc="getFragmentsExportToCSVMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to export the fragments to a CSV file
     *
     * @return the menu item that should export the fragments to a CSV file
     */
    public MenuItem getFragmentsExportToCSVMenuItem() {
        return this.fragmentsExportToCSVMenuItem;
    }
    //</editor-fold>
    //<editor-fold desc="getFragmentsExportToPDBMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to export the fragments to a PDB file
     *
     * @return the menu item that should export the fragments to a PDB file
     */
    public MenuItem getFragmentsExportToPDBMenuItem() {
        return this.fragmentsExportToPDBMenuItem;
    }
    //</editor-fold>
    //<editor-fold desc="getFragmentsExportToPDFMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to export the fragments to a PDF
     *
     * @return the menu item that should export the fragments to a PDF
     */
    public MenuItem getFragmentsExportToPDFMenuItem() {
        return this.fragmentsExportToPDFMenuItem;
    }
    //</editor-fold>
    //<editor-fold desc="getFragmentsExportToSDFMenu" defaultstate="collapsed">
    /**
     * Returns the menu that is supposed to open a list of SDF export options
     *
     * @return the menu that should open a list of SDF export options
     */
    public Menu getFragmentsExportToSDFMenu() {
        return this.fragmentsExportToSDFMenu;
    }
    //</editor-fold>
    //<editor-fold desc="getFragmentsExportToSingleSDFMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to export the fragments to a single SD file
     *
     * @return the menu item that should export the fragments to a single SD file
     */
    public MenuItem getFragmentsExportToSingleSDFMenuItem() {
        return this.fragmentsExportToSingleSDFMenuItem;
    }
    //</editor-fold>
    //<editor-fold desc="getFragmentsExportToSeparateSDFsMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to export the fragments to separate SD files
     *
     * @return the menu item that should export the fragments to separate SD files
     */
    public MenuItem getFragmentsExportToSeparateSDFsMenuItem() {
        return this.fragmentsExportToSeparateSDFsMenuItem;
    }
    //</editor-fold>
    //<editor-fold desc="getItemsExportMenu" defaultstate="collapsed">
    /**
     * Returns the menu that is supposed to open a list of items export options
     *
     * @return the menu that should open a list of items export options
     */
    public Menu getItemsExportMenu() {
        return this.itemsExportMenu;
    }
    //</editor-fold>
    //<editor-fold desc="getItemsExportToCSVMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to export the items to a CSV file
     *
     * @return the menu item that should export the items to a CSV file
     */
    public MenuItem getItemsExportToCSVMenuItem() {
        return this.itemsExportToCSVMenuItem;
    }
    //</editor-fold>
    //<editor-fold desc="getItemsExportToPDFMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to export the items to a PDF
     *
     * @return the menu item that should export the items to a PDF
     */
    public MenuItem getItemsExportToPDFMenuItem() {
        return this.itemsExportToPDFMenuItem;
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
    //<editor-fold desc="getGlobalSettingsMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that opens the global settings view
     *
     * @return the menu item that opens the global settings view
     */
    public MenuItem getGlobalSettingsMenuItem() {
        return this.globalSettingsMenuItem;
    }
    //</editor-fold>
    //<editor-fold desc="getFragmentationAlgorithmMenu" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to choose the fragmentation algorithm
     *
     * @return the menu item that should choose the fragmentation algorithm
     */
    public Menu getFragmentationAlgorithmMenu() {
        return this.fragmentationAlgorithmMenu;
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
