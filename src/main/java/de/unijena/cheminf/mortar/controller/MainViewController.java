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

package de.unijena.cheminf.mortar.controller;

import de.unijena.cheminf.mortar.gui.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.MainView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.io.Importer;
import javafx.application.Platform;
import javafx.event.EventType;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtomContainerSet;

import java.io.File;
import java.util.Objects;

/**
 * MainViewController
 * controls  {@link de.unijena.cheminf.mortar.gui.MainView}.
 *
 * @author Felix Baensch
 */
public class MainViewController {

    private Stage primaryStage;
    private MainView mainView;
    private String appDir;
    private Scene scene;
    private IAtomContainerSet atomContainerSet;

    public MainViewController(Stage aStage, MainView aMainView, String anAppDir){
        //<editor-fold desc="checks" defaultstate="collapsed">
        Objects.requireNonNull(aStage, "aStage (instance of Stage) is null");
        Objects.requireNonNull(aMainView, "aMainView (instance of MainView) is null");
        Objects.requireNonNull(aMainView, "anAppDir (instance of String) is null");
        File tmpAppDirFile = new File(anAppDir);
        if (!tmpAppDirFile.isDirectory() || !tmpAppDirFile.exists()) {
            throw new IllegalArgumentException("The given application directory is neither no directory or does not exist");
        }
        //</editor-fold>
        this.primaryStage = aStage;
        this.mainView = aMainView;
        this.appDir = anAppDir;

        //<editor-fold desc="show MainView inside of the primaryStage" defaultstate="collapsed">
        this.scene = new Scene(this.mainView, GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE, GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.primaryStage.setTitle(Message.get("Title.text"));
        this.primaryStage.setScene(this.scene);
        this.primaryStage.show();
        this.primaryStage.setMinHeight(GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.primaryStage.setMinWidth(GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE);
        //</editor-fold>

        this.addListener();
    }

    private void addListener(){
        this.mainView.getMainMenuBar().getExitMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.closeApplication(0));
        this.mainView.getMainMenuBar().getLoadMenuItem().addEventHandler(
                EventType.ROOT,
                anEvent -> this.loadMoleculeFile(this.primaryStage));
        //TODO: More implementation needed
    }

    /**
     * Closes application
     */
    private void closeApplication(int aStatus) {
        Platform.exit();
        System.exit(aStatus);
    }
    //
    private void loadMoleculeFile(Stage aParentStage){
        Importer tmpImporter = new Importer();
        this.atomContainerSet = new AtomContainerSet();
        this.atomContainerSet = tmpImporter.Import(aParentStage);
        if(this.atomContainerSet == null || this.atomContainerSet.isEmpty())
            return;
        this.OpenMoleculesTab();
    }

    private void OpenMoleculesTab(){

    }
}
