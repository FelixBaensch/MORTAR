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

package de.unijena.cheminf.mortar.main;

import de.unijena.cheminf.mortar.controller.MainViewController;
import de.unijena.cheminf.mortar.gui.MainView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import de.unijena.cheminf.mortar.model.util.LogUtil;
import javafx.application.Application;
import javafx.stage.Stage;

import javax.swing.JOptionPane;
import java.io.File;
import java.util.Locale;

public class MainApp extends Application {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args){
        launch(args);
    }

    @Override
    public void start(Stage aPrimaryStage) {
        try{
            //<editor-fold defaultstate="collapsed" desc="setting default locale">
            Locale.setDefault(new Locale("en", "GB"));
            System.out.println(Locale.getDefault().toString()); //TODO: Log this instead of printing (even though here it will be logged to console)
            //</editor-fold>
            //TODO: Check Java version
            //TODO: Check screen resolution?
            //<editor-fold defaultstate="collapsed" desc="Configure logging environment and log session start">
            boolean tmpWasLoggingInitializationSuccessfull = LogUtil.initializeLoggingEnvironment();
            if (!tmpWasLoggingInitializationSuccessfull) {
                JOptionPane.showMessageDialog(
                        null,
                        Message.get("Error.LoggingInitialization"),
                        Message.get("Error.LoggingInitialization.Title"),
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
            // </editor-fold>
            //<editor-fold desc="determining the application's directory and the default temp file path" defaultstate="collapsed">
            String tmpAppDir = FileUtil.getAppDirPath();
            //</editor-fold>
            MainView tmpMainView = new MainView();
            MainViewController tmpMainViewController = new MainViewController(aPrimaryStage, tmpMainView, tmpAppDir);
            //TODO: Log session end
        } catch (Exception anException){
            //TODO: Log this instead of printing and give notification to the user (dialog)
            System.out.println(anException);
            System.exit(-1);
        }
    }
}
