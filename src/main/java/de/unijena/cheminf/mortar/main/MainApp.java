/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2023  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.gui.views.MainView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import de.unijena.cheminf.mortar.model.util.LogUtil;
import de.unijena.cheminf.mortar.model.util.MiscUtil;

import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main class that extends JavaFX Application to start MORTAR.
 *
 * @author Jonas Schaub, Felix Baensch
 * @version 1.0.0.0
 */
public class MainApp extends Application {
    //
    /**
     * Name (starting with "-") of the command line parameter that can be used to skip the Java version check.
     */
    public static final String SKIP_JAVA_VERSION_CHECK_CMD_ARG_NAME = "-skipJavaVersionCheck";
    //
    /**
     * Parameter-less constructor that calls super().
     * Introduced because javadoc build complained about classes without declared default constructor.
     */
    public MainApp() {
        super();
    }
    //
    /**
     * Calls start(Stage) of Application class.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args){
        Application.launch(args);
    }
    //
    @Override
    public void start(Stage aPrimaryStage) {
        try{
            //<editor-fold defaultstate="collapsed" desc="setting default locale">
            Locale.setDefault(new Locale("en", "GB"));
            Logger.getLogger(Main.class.getName()).info(Locale.getDefault().toString());
            //</editor-fold>
            //<editor-fold defaultstate="collapsed" desc="Check Java version">
            String tmpJavaVersion = System.getProperty("java.version");
            List<String> tmpCMDArgs = this.getParameters().getRaw();
            boolean tmpSkipJavaVersionCheck = false;
            if (tmpCMDArgs.size() > 0) {
                for (String tmpArg : tmpCMDArgs) {
                    if (tmpArg.equals(MainApp.SKIP_JAVA_VERSION_CHECK_CMD_ARG_NAME)) {
                        tmpSkipJavaVersionCheck = true;
                        break;
                    }
                }
            }
            if (!tmpSkipJavaVersionCheck) {
                if (MiscUtil.compareVersions(tmpJavaVersion, BasicDefinitions.MINIMUM_JAVA_VERSION) < 0) {
                    Logger.getLogger(Main.class.getName()).log(Level.WARNING, "Java version lower than minimum: " + tmpJavaVersion);
                    String tmpContentText = String.format(Message.get("Error.InvalidJavaVersion.Content"), BasicDefinitions.MINIMUM_JAVA_VERSION, tmpJavaVersion);
                    if (GuiUtil.guiConfirmationAlert(Message.get("Error.InvalidJavaVersion.Title"), Message.get("Error.InvalidJavaVersion.Header"), tmpContentText) == ButtonType.CANCEL) {
                        System.exit(0);
                    } //else: The user ignores the fact that their Java version is insufficient
                }
            }
            //</editor-fold>
            //<editor-fold desc="Check single instance" defaultstate="collapsed">
            boolean tmpLCKFilePresent = LogUtil.checkForLCKFileInLogDir();
            if (tmpLCKFilePresent) {
                if (GuiUtil.guiConfirmationAlert(
                        Message.get("Error.SecondInstance.Title"),
                        Message.get("Error.SecondInstance.Header"),
                        Message.get("Error.SecondInstance.Content")) == ButtonType.CANCEL) {
                    System.exit(0);
                } //else: user wants to continue despite the possible second instance;
                // this means that all existing .lck files will be removed below with LogUtil.manageLogFilesFolderIfExists()
            } //else: single MORTAR instance running
            //</editor-fold>
            //<editor-fold defaultstate="collapsed" desc="Configure logging environment and log session start">
            LogUtil.manageLogFilesFolderIfExists();
            boolean tmpWasLoggingInitializationSuccessful = LogUtil.initializeLoggingEnvironment();
            if (!tmpWasLoggingInitializationSuccessful) {
                GuiUtil.guiMessageAlert(Alert.AlertType.INFORMATION, Message.get("Error.LoggingInitialization.Title"),
                        Message.get("Error.LoggingInitialization.Header"),
                        Message.get("Error.LoggingInitialization.Content"));
            }
            //Start new logging session
            Logger.getLogger(Main.class.getName()).info(String.format(BasicDefinitions.MORTAR_SESSION_START_FORMAT, BasicDefinitions.MORTAR_VERSION));
            Logger.getLogger(Main.class.getName()).info(String.format("Started with Java version %s.", tmpJavaVersion));
            // </editor-fold>
            //<editor-fold desc="determining the application's directory and the default temp file path" defaultstate="collapsed">
            String tmpAppDir = FileUtil.getAppDirPath();
            //</editor-fold>
            MainView tmpMainView = new MainView();
            MainViewController tmpMainViewController = new MainViewController(aPrimaryStage, tmpMainView, tmpAppDir);
        } catch (Exception | OutOfMemoryError anException){
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, anException.toString(), anException);
            if (anException instanceof OutOfMemoryError) {
                GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                        Message.get("Error.ExceptionAlert.Header"),
                        anException.getMessage(),
                        new Exception(((OutOfMemoryError) anException).toString()));
            } else {
                GuiUtil.guiExceptionAlert(Message.get("Error.ExceptionAlert.Title"),
                        Message.get("Error.ExceptionAlert.Header"),
                        anException.getMessage(),
                        (Exception) anException);
            }
            System.exit(-1);
        }
    }
}
