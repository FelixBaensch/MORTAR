/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2025  Felix Baensch, Jonas Schaub (felix.j.baensch@gmail.com, jonas.schaub@uni-jena.de)
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

import de.unijena.cheminf.mortar.gui.views.MainView;
import de.unijena.cheminf.mortar.message.Message;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

/**
 * A MenuBar for the application's {@link MainView}.
 * It contains menus for file handling (I/O), shutting down the application, settings and help menu entries.
 *
 * @author Felix Baensch, Jonas Schaub, Samuel Behr
 * @version 1.0.0.0
 */
public class MainMenuBar extends MenuBar {
    //<editor-fold desc="private final class constants" defaultstate="collapsed">
    /**
     * FileMenu.
     */
    private final Menu fileMenu;
    /**
     * MenuItem to import molecules.
     */
    private final MenuItem openMenuItem;
    /**
     * MenuItem for export.
     */
    private final Menu exportMenu;
    /**
     * Menu for fragments export.
     */
    private final Menu fragmentsExportMenu;
    /**
     * MenuItem to export fragments as csv file.
     */
    private final MenuItem fragmentsExportToCSVMenuItem;
    /**
     * MenuItem to export fragments as pdb file.
     */
    private final MenuItem fragmentsExportToPDBMenuItem;
    /**
     * MenuItem to export fragments as pdf file.
     */
    private final MenuItem fragmentsExportToPDFMenuItem;
    /**
     * Menu for fragments export as sdf.
     */
    private final Menu fragmentsExportToSDFMenu;
    /**
     * MenuItem to export fragments as sd file.
     */
    private final MenuItem fragmentsExportToSingleSDFMenuItem;
    /**
     * MenuItem to export fragments as sd file separately.
     */
    private final MenuItem fragmentsExportToSeparateSDFsMenuItem;
    /**
     * Menu for items export.
     */
    private final Menu itemsExportMenu;
    /**
     * MenuItem to export items as csv file.
     */
    private final MenuItem itemsExportToCSVMenuItem;
    /**
     * MenuItem to export fragments as pdf file.
     */
    private final MenuItem itemsExportToPDFMenuItem;
    /**
     * MenuItem to exit app.
     */
    private final MenuItem exitMenuItem;
    /**
     * Menu for settings.
     */
    private final Menu settingsMenu;
    /**
     * Menu for pipeline.
     */
    private final Menu pipelineMenu;
    /**
     * Menu for help.
     */
    private final Menu helpMenu;
    /**
     * MenuItem to open fragmentation settings.
     */
    private final MenuItem fragmentationSettingsMenuItem;
    /**
     * MenuItem to open global settings.
     */
    private final MenuItem globalSettingsMenuItem;
    /**
     * MenuItem to open pipeline settings.
     */
    private final MenuItem pipelineSettingsMenuItem;
    /**
     * MenuItem to open AboutView.
     */
    private final MenuItem aboutViewMenuItem;
    /**
     * Menu to choose fragmentation algorithm.
     */
    private final Menu fragmentationAlgorithmMenu;
    /**
     * MenuItem to cancel molecule import, only visible if import is running.
     */
    private final MenuItem cancelImportMenuItem;
    /**
     * MenuItem to cancel export, only visible if import is running.
     */
    private final MenuItem cancelExportMenuItem;
    /**
     * Menu to open views.
     */
    private final Menu viewsMenu;
    /**
     * MenuItem to open the histogram.
     */
    private final MenuItem histogramViewerMenuItem;
    /**
     * MenuItem to open the OverviewView.
     */
    private final MenuItem overviewViewMenuItem;
    //</editor-fold>
    //
    //<editor-fold desc="constructor" defaultstate="collapsed">
    /**
     * Initialises the MainMenuBar's components and adds them to the MainMenuBar.
     * No listeners are added here.
     */
    public MainMenuBar() {
        super();
        //<editor-fold desc="initialisation" defaultstate="collapsed">
        //fileMenu
        this.fileMenu = new Menu(Message.get("MainView.menuBar.fileMenu.text"));
        this.openMenuItem = new MenuItem(Message.get("MainView.menuBar.fileMenu.openMenuItem.text"));
        this.cancelImportMenuItem = new MenuItem(Message.get("MainView.menuBar.fileMenu.openMenuItem.cancel"));
        this.exportMenu = new Menu(Message.get("MainView.menuBar.fileMenu.exportMenu.text"));
        this.cancelExportMenuItem = new MenuItem(Message.get("MainView.menuBar.fileMenu.exportMenu.cancel"));
        //<editor-fold desc="exportMenu components" defaultstate="collapsed">
        //fragmentsExportMenu
        this.fragmentsExportMenu = new Menu(Message.get("MainView.menuBar.fileMenu.exportMenu.fragmentsExportMenu.text"));
        //components
        this.fragmentsExportToCSVMenuItem = new MenuItem(Message.get("MainView.menuBar.fileMenu.exportMenu.CSV.text"));
        this.fragmentsExportToPDBMenuItem = new MenuItem(Message.get("MainView.menuBar.fileMenu.exportMenu.PDB.text"));
        this.fragmentsExportToPDFMenuItem = new MenuItem(Message.get("MainView.menuBar.fileMenu.exportMenu.PDF.text"));
        this.fragmentsExportToSDFMenu = new Menu(Message.get("MainView.menuBar.fileMenu.exportMenu.SDF.text"));
        this.fragmentsExportToSingleSDFMenuItem = new MenuItem(Message.get("MainView.menuBar.fileMenu.exportMenu.singleFile.text"));
        this.fragmentsExportToSeparateSDFsMenuItem = new MenuItem(Message.get("MainView.menuBar.fileMenu.exportMenu.separateFiles.text"));
        //itemsExportMenu
        this.itemsExportMenu = new Menu(Message.get("MainView.menuBar.fileMenu.exportMenu.itemsExportMenu.text"));
        //components
        this.itemsExportToCSVMenuItem = new MenuItem(Message.get("MainView.menuBar.fileMenu.exportMenu.CSV.text"));
        this.itemsExportToPDFMenuItem = new MenuItem(Message.get("MainView.menuBar.fileMenu.exportMenu.PDF.text"));
        //</editor-fold>
        this.exitMenuItem = new MenuItem(Message.get("MainView.menuBar.fileMenu.exitMenuItem.text"));
        //settingsMenu
        this.settingsMenu = new Menu(Message.get("MainView.menuBar.settingsMenu.text"));
        this.globalSettingsMenuItem = new MenuItem(Message.get("MainView.menuBar.settingsMenu.globalSettingsMenuItem.text"));
        this.fragmentationAlgorithmMenu = new Menu(Message.get("MainView.menuBar.settingsMenu.fragmentationAlgorithmMenuItem.text"));
        this.fragmentationSettingsMenuItem = new MenuItem(Message.get("MainView.menuBar.settingsMenu.fragmentationSettingsMenuItem.text"));
        //pipelineMenu
        this.pipelineMenu = new Menu(Message.get("MainView.menuBar.pipelineMenu.text"));
        this.pipelineSettingsMenuItem = new MenuItem(Message.get("MainView.menuBar.pipelineMenu.pipelineSettingsMenuItem.text"));
        //helpMenu
        this.helpMenu = new Menu(Message.get("MainView.menuBar.helpMenu.text"));
        this.aboutViewMenuItem = new MenuItem(Message.get("MainView.menuBar.helpMenu.aboutViewMenuItem.text"));
//        this.logFilesMenuItem = new MenuItem(Message.get("MainView.menuBar.helpMenu.logFilesMenuItem.text"));
//        this.gitHubRepoMenuItem = new MenuItem(Message.get("MainView.menuBar.helpMenu.gitHubRepoMenuItem.text"));
        //viewsMenu
        this.viewsMenu = new Menu(Message.get("MainView.menuBar.viewsMenu.text"));
        this.histogramViewerMenuItem = new MenuItem(Message.get("MainView.menuBar.viewsMenu.HistogramMenuItem.text"));
        this.overviewViewMenuItem = new MenuItem(Message.get("MainView.menuBar.viewsMenu.overviewViewMenuItem.text"));
        //</editor-fold>
        this.addComponentsToMenuBar();
    }
    //</editor-fold>
    //
    //<editor-fold desc="private methods" defaultstate="collapsed">
    //<editor-fold desc="addComponentsToMenuBar" defaultstate="collapsed">
    /**
     * Adds the menu items to the menus and the menus to the menu bar.
     */
    private void addComponentsToMenuBar() {
        //<editor-fold desc="fileMenu" defaultstate="collapsed">
        this.getMenus().add(this.fileMenu);
        //openMenuItem
        this.fileMenu.getItems().add(this.openMenuItem);
        this.fileMenu.getItems().add(this.cancelImportMenuItem);
        this.cancelImportMenuItem.setVisible(false);
        //exportMenu
        this.fileMenu.getItems().add(this.exportMenu);
        this.fileMenu.getItems().add(this.cancelExportMenuItem);
        this.cancelExportMenuItem.setVisible(false);
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
        //<editor-fold desc="pipelineMenu" defaultstate="collapsed">
        this.getMenus().add(this.pipelineMenu);
        //pipelineSettingsMenuItem
        this.pipelineMenu.getItems().add(this.pipelineSettingsMenuItem);
        //</editor-fold>
        //<editor-fold desc="viewsMenu" defaultstate="collapsed">
        this.getMenus().add(this.viewsMenu);
        this.viewsMenu.getItems().add((this.histogramViewerMenuItem));
        this.histogramViewerMenuItem.setDisable(true);
        //overviewMenuItem
        this.viewsMenu.getItems().add(this.overviewViewMenuItem);
        this.overviewViewMenuItem.setDisable(true);
        //</editor-fold>
        //<editor-fold desc="helpMenu" defaultstate="collapsed">
//        this.helpMenu.getItems().add(this.logFilesMenuItem);
//        this.helpMenu.getItems().add(this.gitHubRepoMenuItem);
        this.helpMenu.getItems().add(this.aboutViewMenuItem);
        this.getMenus().add(this.helpMenu);
        //</editor-fold>
    }
    //</editor-fold>
    //<editor-fold desc="addComponentsToExportMenu" defaultstate="collapsed">
    /**
     * Adds the menu items to the export menu.
     */
    private void addComponentsToExportMenu() {
        //<editor-fold desc="fragmentsExportMenu" defaultstate="collapsed">
        this.exportMenu.getItems().add(this.fragmentsExportMenu);
        this.fragmentsExportMenu.getItems().add(this.fragmentsExportToCSVMenuItem);
        this.fragmentsExportMenu.getItems().add(this.fragmentsExportToPDBMenuItem);
        this.fragmentsExportMenu.getItems().add(this.fragmentsExportToPDFMenuItem);
        this.fragmentsExportMenu.getItems().add(this.fragmentsExportToSDFMenu);
        this.fragmentsExportToSDFMenu.getItems().add(this.fragmentsExportToSingleSDFMenuItem);
        this.fragmentsExportToSDFMenu.getItems().add(this.fragmentsExportToSeparateSDFsMenuItem);
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
    //<editor-fold desc="getOpenMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to open a molecule set.
     *
     * @return the menu item that should open a molecule set
     */
    public MenuItem getOpenMenuItem() {
        return this.openMenuItem;
    }
    //</editor-fold>
    //<editor-fold desc="getExportMenu" defaultstate="collapsed">
    /**
     * Returns the menu that is supposed to open a list of export options.
     *
     * @return the menu that should open a list of export options
     */
    public Menu getExportMenu() {
        return this.exportMenu;
    }
    //</editor-fold>
    //<editor-fold desc="getFragmentsExportMenu" defaultstate="collapsed">
    /**
     * Returns the menu that is supposed to open a list of fragments export options.
     *
     * @return the menu that should open a list of fragments export options
     */
    public Menu getFragmentsExportMenu() {
        return this.fragmentsExportMenu;
    }
    //</editor-fold>
    //<editor-fold desc="getFragmentsExportToCSVMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to export the fragments to a CSV file.
     *
     * @return the menu item that should export the fragments to a CSV file
     */
    public MenuItem getFragmentsExportToCSVMenuItem() {
        return this.fragmentsExportToCSVMenuItem;
    }
    //</editor-fold>
    //<editor-fold desc="getFragmentsExportToPDBMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to export the fragments to a PDB file.
     *
     * @return the menu item that should export the fragments to a PDB file
     */
    public MenuItem getFragmentsExportToPDBMenuItem() {
        return this.fragmentsExportToPDBMenuItem;
    }
    //</editor-fold>
    //<editor-fold desc="getFragmentsExportToPDFMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to export the fragments to a PDF.
     *
     * @return the menu item that should export the fragments to a PDF
     */
    public MenuItem getFragmentsExportToPDFMenuItem() {
        return this.fragmentsExportToPDFMenuItem;
    }
    //</editor-fold>
    //<editor-fold desc="getFragmentsExportToSDFMenu" defaultstate="collapsed">
    /**
     * Returns the menu that is supposed to open a list of SDF export options.
     *
     * @return the menu that should open a list of SDF export options
     */
    public Menu getFragmentsExportToSDFMenu() {
        return this.fragmentsExportToSDFMenu;
    }
    //</editor-fold>
    //<editor-fold desc="getFragmentsExportToSingleSDFMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to export the fragments to a single SD file.
     *
     * @return the menu item that should export the fragments to a single SD file
     */
    public MenuItem getFragmentsExportToSingleSDFMenuItem() {
        return this.fragmentsExportToSingleSDFMenuItem;
    }
    //</editor-fold>
    //<editor-fold desc="getFragmentsExportToSeparateSDFsMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to export the fragments to separate SD files.
     *
     * @return the menu item that should export the fragments to separate SD files
     */
    public MenuItem getFragmentsExportToSeparateSDFsMenuItem() {
        return this.fragmentsExportToSeparateSDFsMenuItem;
    }
    //</editor-fold>
    //<editor-fold desc="getItemsExportMenu" defaultstate="collapsed">
    /**
     * Returns the menu that is supposed to open a list of items export options.
     *
     * @return the menu that should open a list of items export options
     */
    public Menu getItemsExportMenu() {
        return this.itemsExportMenu;
    }
    //</editor-fold>
    //<editor-fold desc="getItemsExportToCSVMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to export the items to a CSV file.
     *
     * @return the menu item that should export the items to a CSV file
     */
    public MenuItem getItemsExportToCSVMenuItem() {
        return this.itemsExportToCSVMenuItem;
    }
    //</editor-fold>
    //<editor-fold desc="getItemsExportToPDFMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to export the items to a PDF.
     *
     * @return the menu item that should export the items to a PDF
     */
    public MenuItem getItemsExportToPDFMenuItem() {
        return this.itemsExportToPDFMenuItem;
    }
    //</editor-fold>
    //<editor-fold desc="getExitMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to shut down the application.
     *
     * @return the menu item that should shut down the application
     */
    public MenuItem getExitMenuItem() {
        return this.exitMenuItem;
    }
    //</editor-fold>
    //<editor-fold desc="getGlobalSettingsMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that opens the global settings view.
     *
     * @return the menu item that opens the global settings view
     */
    public MenuItem getGlobalSettingsMenuItem() {
        return this.globalSettingsMenuItem;
    }
    //</editor-fold>
    //<editor-fold desc="getFragmentationAlgorithmMenu" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to choose the fragmentation algorithm.
     *
     * @return the menu item that should choose the fragmentation algorithm
     */
    public Menu getFragmentationAlgorithmMenu() {
        return this.fragmentationAlgorithmMenu;
    }
    //</editor-fold>
    //<editor-fold desc="getFragmentationSettingsMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to open the fragmentation settings window.
     *
     * @return the menu item that should open the fragmentation settings window
     */
    public MenuItem getFragmentationSettingsMenuItem() {
        return this.fragmentationSettingsMenuItem;
    }
    //</editor-fold>
    //<editor-fold desc="getPipelineSettingsMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to open the pipeline settings window.
     *
     * @return the menu item that should open the pipeline settings window
     */
    public MenuItem getPipelineSettingsMenuItem() {
        return this.pipelineSettingsMenuItem;
    }
    //</editor-fold>
    //<editor-fold desc="getOverviewMenuItem defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to open the OverviewView.
     *
     * @return the menu item that should open the OverviewView
     */
    public MenuItem getOverviewViewMenuItem() {
        return this.overviewViewMenuItem;
    }
    //</editor-fold>
    //<editor-fold desc="getAboutViewMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to open the AboutView.
     *
     * @return the menu item that should open the AboutView
     */
    public MenuItem getAboutViewMenuItem() {
        return this.aboutViewMenuItem;
    }
    //</editor-fold>
    //<editor-fold desc="getHistogramViewerMenuItem" defaultstate="collapsed">
    /**
     * Returns the menu item that is supposed to open histogram window.
     *
     * @return MenuItem to open histogram view
     */
    public MenuItem getHistogramViewerMenuItem(){
        return this.histogramViewerMenuItem;
    }
    //</editor-fold>
    //
    /**
     * Returns MenuItem to cancel running import, only visible if import is running.
     *
     * @return MenuItem to cancel import
     */
    public MenuItem getCancelImportMenuItem(){
        return this.cancelImportMenuItem;
    }
    //
    /**
     * Returns MenuItem to cancel running export, only visible if import is running.
     *
     * @return MenuItem to cancel export
     */
    public MenuItem getCancelExportMenuItem(){
        return this.cancelExportMenuItem;
    }
    //</editor-fold>
}
