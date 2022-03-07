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

package de.unijena.cheminf.mortar.controller;

import de.unijena.cheminf.mortar.gui.util.ExternalTool;
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.views.AboutView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.LogUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AboutViewController {

    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * Main Stage / parent Stage
     */
    private Stage mainStage;
    /**
     * View to controll
     */
    private AboutView aboutView;
    /**
     * Stage to show AboutView
     */
    private Stage aboutViewStage;
    /**
     * path to xml file which contains information about external tools
     */
    private String toolsXmlFileName = "de/unijena/cheminf/mortar/descriptions/tools_description.xml";
    /**
     * ObservableList to show properties of ExternalTools
     */
    private ObservableList<ExternalTool> toolObservableList;
    //</editor-fold>
    //
    //<editor-fold desc="private static final class variables" defaultstate="collapsed">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(AboutViewController.class.getName());
    //</editor-fold>
    //
    /**
     * Constructor
     *
     * @param aStage Stage
     */
    public AboutViewController(Stage aStage){
        this.mainStage = aStage;
        this.toolObservableList = FXCollections.observableArrayList();
        this.showAboutView();
    }
    //
    //<editor-fold desc="private methods" defaultstate="collapsed">
    /**
     * Sets stage, scene and gui properties and shows view
     */
    private void showAboutView(){
        if(this.aboutView == null){
            this.aboutView = new AboutView();
        }
        this.aboutViewStage = new Stage();
        Scene tmpScene = new Scene(this.aboutView, GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE, GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.aboutViewStage.setScene(tmpScene);
        this.aboutViewStage.initModality(Modality.WINDOW_MODAL);
        this.aboutViewStage.initOwner(this.mainStage);
        this.aboutViewStage.setTitle(Message.get("AboutView.title.text"));
        this.aboutViewStage.setMinHeight(GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.aboutViewStage.setMinWidth(GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE);
        Platform.runLater(()->{
            this.addListeners();
            this.getExternalToolInfosFromXml();
            this.aboutView.getTableView().setItems(this.toolObservableList);
        });
        this.aboutViewStage.showAndWait();
    }
    //
    /**
     * Adds event handlers and listeners
     */
    private void addListeners(){
        //close button
        this.aboutView.getCloseButton().setOnAction(actionEvent -> {
            this.aboutViewStage.close();
        });
        //log file button
        this.aboutView.getLogFileButton().setOnAction(actionEvent -> {
            this.openFilePathInExplorer(LogUtil.getLogFileDirectoryPath());
        });
        //gitHUb button
        this.aboutView.getGitHubButton().setOnAction(actionEvent -> {
            this.openGitHubRepositoryInDefaultBrowser();
        });
    }
    //
    /**
     * Opens given path in OS depending explorer equivalent
     *
     * @param aPath path to open
     */
    private void openFilePathInExplorer(String aPath){
        if (Objects.isNull(aPath) || aPath.isEmpty() || aPath.isBlank())
            throw new IllegalArgumentException("Given file path is null or empty.");
        String tmpOS = System.getProperty("os.name").toUpperCase();
        try{
            if (tmpOS.contains("WIN"))
                Runtime.getRuntime().exec("explorer /open," + aPath);
            else if (tmpOS.contains("MAC"))
                Runtime.getRuntime().exec("open -R " + aPath);
            else if (tmpOS.contains("NUX") || tmpOS.contains("NIX") || tmpOS.contains("AIX"))
                Runtime.getRuntime().exec("gio open " + aPath);
            else
                throw new SecurityException("OS name " + tmpOS + " unknown.");
        } catch (IOException anException) {
            LOGGER.log(Level.SEVERE, anException.toString(), anException);
            throw new SecurityException("Could not open directory path");
        }
    }
    //
    /**
     * Opens GitHub repository website in system default browser
     *
     * Note: Does not really fit in FileUtil
     */
    private void openGitHubRepositoryInDefaultBrowser(){
        String tmpOS = System.getProperty("os.name").toUpperCase();
        try{
            if (tmpOS.contains("WIN"))
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + BasicDefinitions.GITHUB_REPOSITORY_URL);
            else if (tmpOS.contains("MAC"))
                Runtime.getRuntime().exec("open " + BasicDefinitions.GITHUB_REPOSITORY_URL);
            else if (tmpOS.contains("NUX") || tmpOS.contains("NIX") || tmpOS.contains("AIX"))
            {
                //ToDo: extend browser array, if necessary
                String[] tmpBrowserArray = { "google-chrome", "firefox", "mozilla", "epiphany", "konqueror",
                        "netscape", "opera", "links", "lynx" };
                StringBuffer tmpCommandString = new StringBuffer();
                for (int i = 0; i < tmpBrowserArray.length; i++){
                    if(i == 0)
                        tmpCommandString.append(String.format(    "%s \"%s\"", tmpBrowserArray[i], BasicDefinitions.GITHUB_REPOSITORY_URL));
                    else
                        tmpCommandString.append(String.format(" || %s \"%s\"", tmpBrowserArray[i], BasicDefinitions.GITHUB_REPOSITORY_URL));
                }
                Runtime.getRuntime().exec(new String[] { "sh", "-c", tmpCommandString.toString() });
            }
            else
                throw new SecurityException("OS name " + tmpOS + " unknown.");
        } catch (IOException anException) {
            LOGGER.log(Level.SEVERE, anException.toString(), anException);
            throw new SecurityException("Could not open directory path");
        }
    }
    //
    /**
     * Reads xml file (tools_description.xml in resources) which contains information about the used external tools
     */
    private void getExternalToolInfosFromXml(){
        DocumentBuilderFactory tmpDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
        try{
            // optional, but recommended
            // process XML securely, avoid attacks like XML External Entities (XXE)
            tmpDocumentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder tmpDocBuilder = tmpDocumentBuilderFactory.newDocumentBuilder();
            Document tmpDoc = tmpDocBuilder.parse(new File(getClass().getClassLoader().getResource(this.toolsXmlFileName).toURI()));
            if(tmpDoc == null){
                throw new FileNotFoundException("File not found " + this.toolsXmlFileName);
            }

            // optional, but recommended
            // http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            tmpDoc.getDocumentElement().normalize();
            NodeList tmpList = tmpDoc.getElementsByTagName("externalTool");
            for(int i = 0; i < tmpList.getLength(); i++){
                Node tmpNode = tmpList.item(i);
                if(tmpNode.getNodeType() == Node.ELEMENT_NODE){
                    Element tmpElem = (Element) tmpNode;
                    String tmpName = tmpElem.getElementsByTagName("name").item(0).getTextContent();
                    String tmpVersion = tmpElem.getElementsByTagName("version").item(0).getTextContent();
                    String tmpAuthor = tmpElem.getElementsByTagName("author").item(0).getTextContent();
                    String tmpLicense = tmpElem.getElementsByTagName("license").item(0).getTextContent();
                    ExternalTool tmpTool = new ExternalTool(tmpName, tmpVersion, tmpAuthor, tmpLicense);
                    this.toolObservableList.add(tmpTool);
                }
            }
        } catch (ParserConfigurationException | IOException | URISyntaxException | SAXException anException) {
            LOGGER.log(Level.SEVERE, anException.toString(), anException);
        }
    }
    //</editor-fold>
}
