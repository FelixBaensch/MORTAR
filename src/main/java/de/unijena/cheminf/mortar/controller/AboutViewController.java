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

package de.unijena.cheminf.mortar.controller;

import de.unijena.cheminf.mortar.configuration.Configuration;
import de.unijena.cheminf.mortar.gui.util.ExternalTool;
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.gui.views.AboutView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.util.LogUtil;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
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

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller class for AboutView
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class AboutViewController {

    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * Main Stage / parent Stage
     */
    private Stage mainStage;
    /**
     * View to control
     */
    private AboutView aboutView;
    /**
     * Stage to show AboutView
     */
    private Stage aboutViewStage;
    /**
     * ObservableList to show properties of ExternalTools
     */
    private ObservableList<ExternalTool> toolObservableList;
    /**
     * Configuration class to read resource file paths from.
     */
    private final Configuration configuration;
    //</editor-fold>
    //
    //<editor-fold desc="private static final class variables" defaultstate="collapsed">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(AboutViewController.class.getName());
    //
    /**
     * Name of logo icon.
     */
    private static final String MORTAR_LOGO_ICON_FILE_NAME = "Mortar_Logo_Icon1.png";
    /**
     * name of xml file which contains information about external tools.
     */
    private static final String TOOLS_XML_FILE_NAME = "tools_description.xml";
    //</editor-fold>
    //
    /**
     * Constructor
     *
     * @param aStage Stage
     * @throws IOException if configuration properties cannot be imported
     */
    public AboutViewController(Stage aStage) throws IOException {
        this.mainStage = aStage;
        this.toolObservableList = FXCollections.observableArrayList();
        this.configuration = Configuration.getInstance();
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
        File tmpIconFile = new File(this.configuration.getProperty("mortar.imagesFolder"), AboutViewController.MORTAR_LOGO_ICON_FILE_NAME);
        InputStream tmpImageInputStream = this.getClass().getClassLoader().getResourceAsStream(tmpIconFile.getPath());
        this.aboutViewStage.getIcons().add(new Image(tmpImageInputStream));
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
        //tutorial button
        this.aboutView.getTutorialButton().setOnAction(actionEvent -> {
            this.openTutorialInDefaultPdfViewer();
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
            AboutViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
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
        try{
            Desktop.getDesktop().browse(new URI(this.configuration.getProperty("mortar.github.repository.url")));
        } catch (IOException | URISyntaxException anException) {
            AboutViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            throw new SecurityException("Could not open directory path");
        }
    }
    //
    /**
     * Opens the MORTAR tutorial in system default browser
     */
    private void openTutorialInDefaultPdfViewer() {
        //Note: Does not work when started from IDE, only in built version started from JAR
        try {
            Desktop.getDesktop().open(new File(this.configuration.getProperty("mortar.tutorial.relativeFilePath")));
        } catch (IOException | IllegalArgumentException anException) {
            LOGGER.log(Level.SEVERE, anException.toString(), anException);
            Hyperlink tmpLinkToTutorial = new Hyperlink(Message.get("AboutView.tutorialButton.alert.hyperlink.text"));
            tmpLinkToTutorial.setTooltip(new Tooltip(this.configuration.getProperty("mortar.tutorial.url")));
            tmpLinkToTutorial.setOnAction(event -> {
                try {
                    Desktop.getDesktop().browse(new URI(this.configuration.getProperty("mortar.tutorial.url")));
                } catch (IOException | URISyntaxException e) {
                    LOGGER.log(Level.SEVERE, anException.toString(), anException);
                    throw new SecurityException("Could not open URI");
                }
            });
            GuiUtil.guiMessageAlertWithHyperlink(Alert.AlertType.ERROR, Message.get("AboutView.tutorialButton.alert.title"),
                    Message.get("AboutView.tutorialButton.alert.header"), tmpLinkToTutorial );
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
            File tmpToolsXMLFile = new File(this.configuration.getProperty("mortar.descriptionsFolder"), AboutViewController.TOOLS_XML_FILE_NAME);
            Document tmpDoc = tmpDocBuilder.parse(this.getClass().getClassLoader().getResourceAsStream(tmpToolsXMLFile.getPath()));
            if (tmpDoc == null) {
                throw new FileNotFoundException("File not found " + this.TOOLS_XML_FILE_NAME);
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
        } catch (ParserConfigurationException | IOException | SAXException anException) {
            LOGGER.log(Level.SEVERE, anException.toString(), anException);
        }
    }
    //</editor-fold>
}
