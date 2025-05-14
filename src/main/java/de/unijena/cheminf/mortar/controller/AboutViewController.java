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

package de.unijena.cheminf.mortar.controller;

import de.unijena.cheminf.mortar.configuration.IConfiguration;
import de.unijena.cheminf.mortar.gui.util.ExternalTool;
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.gui.views.AboutView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import de.unijena.cheminf.mortar.model.util.LogUtil;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller class for AboutView.
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class AboutViewController {
    //<editor-fold desc="private (final) class variables" defaultstate="collapsed">
    /**
     * Main Stage / parent Stage.
     */
    private final Stage mainStage;
    /**
     * View to control.
     */
    private AboutView aboutView;
    /**
     * Stage to show AboutView.
     */
    private Stage aboutViewStage;
    /**
     * ObservableList to show properties of ExternalTools.
     */
    private final ObservableList<ExternalTool> toolObservableList;
    /**
     * Configuration class to read resource file paths from.
     */
    private final IConfiguration configuration;
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
     * Constructor, shows about view on the given stage.
     *
     * @param aStage Stage
     * @param aConfiguration configuration class reading from properties file
     */
    public AboutViewController(Stage aStage, IConfiguration aConfiguration) {
        this.mainStage = aStage;
        this.toolObservableList = FXCollections.observableArrayList();
        this.configuration = aConfiguration;
        this.showAboutView();
    }
    //
    //<editor-fold desc="private methods" defaultstate="collapsed">
    /**
     * Sets stage, scene, and gui properties and shows view.
     */
    private void showAboutView() {
        if (this.aboutView == null) {
            this.aboutView = new AboutView(this.configuration);
        }
        this.aboutViewStage = new Stage();
        Scene tmpScene = new Scene(this.aboutView, GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE, GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.aboutViewStage.setScene(tmpScene);
        this.aboutViewStage.initModality(Modality.WINDOW_MODAL);
        this.aboutViewStage.initOwner(this.mainStage);
        this.aboutViewStage.setTitle(Message.get("AboutView.title.text"));
        this.aboutViewStage.setMinHeight(GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.aboutViewStage.setMinWidth(GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE);
        String tmpIconURL = this.getClass().getClassLoader().getResource(
                this.configuration.getProperty("mortar.imagesFolder") + this.configuration.getProperty("mortar.logo.icon.name")).toExternalForm();
        this.aboutViewStage.getIcons().add(new Image(tmpIconURL));
        Platform.runLater(()->{
            this.addListeners();
            this.getExternalToolInfoFromXml();
            this.aboutView.getTableView().setItems(this.toolObservableList);
        });
        this.aboutViewStage.showAndWait();
    }
    //
    /**
     * Adds event handlers and listeners.
     */
    private void addListeners() {
        //close button
        this.aboutView.getCloseButton().setOnAction(actionEvent -> this.aboutViewStage.close());
        //log file button
        this.aboutView.getLogFileButton().setOnAction(actionEvent -> FileUtil.openFilePathInExplorer(LogUtil.getLogFileDirectoryPath()));
        //gitHUb button
        this.aboutView.getGitHubButton().setOnAction(actionEvent -> this.openGitHubRepositoryInDefaultBrowser());
        //tutorial button
        this.aboutView.getTutorialButton().setOnAction(actionEvent -> this.openTutorialInDefaultPdfViewer());
    }
    //
    /**
     * Opens GitHub repository website in system default browser. Developers note: Does not really fit in FileUtil.
     *
     * @throws SecurityException if URL could not be opened
     */
    private void openGitHubRepositoryInDefaultBrowser() throws SecurityException {
        try{
            Desktop.getDesktop().browse(new URI(this.configuration.getProperty("mortar.github.repository.url")));
        } catch (IOException | URISyntaxException anException) {
            AboutViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            throw new SecurityException("Could not open repository URL");
        }
    }
    //
    /**
     * Opens the MORTAR tutorial in system default browser.
     *
     * @throws SecurityException if URL could not be opened
     */
    private void openTutorialInDefaultPdfViewer() {
        //Note: Does not work when started from IDE, only in built version started from JAR
        try {
            Desktop.getDesktop().open(new File(this.configuration.getProperty("mortar.tutorial.relativeFilePath")));
        } catch (IOException | IllegalArgumentException anException) {
            AboutViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            Hyperlink tmpLinkToTutorial = new Hyperlink(Message.get("AboutView.tutorialButton.alert.hyperlink.text"));
            tmpLinkToTutorial.setTooltip(GuiUtil.createTooltip(this.configuration.getProperty("mortar.tutorial.url")));
            tmpLinkToTutorial.setOnAction(event -> {
                try {
                    Desktop.getDesktop().browse(new URI(this.configuration.getProperty("mortar.tutorial.url")));
                } catch (IOException | URISyntaxException e) {
                    AboutViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                    throw new SecurityException("Could not open URI");
                }
            });
            GuiUtil.guiMessageAlertWithHyperlink(Alert.AlertType.ERROR, Message.get("AboutView.tutorialButton.alert.title"),
                    Message.get("AboutView.tutorialButton.alert.header"), tmpLinkToTutorial );
        }
    }
    //
    /**
     * Reads xml file (tools_description.xml in resources) which contains information about the used external tools.
     */
    private void getExternalToolInfoFromXml() {
        try {
            DocumentBuilderFactory tmpDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
            // optional, but recommended
            // process XML securely, avoid attacks like XML External Entities (XXE)
            tmpDocumentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            tmpDocumentBuilderFactory.setExpandEntityReferences(false);
            tmpDocumentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            tmpDocumentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            tmpDocumentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            tmpDocumentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            tmpDocumentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            DocumentBuilder tmpDocBuilder = tmpDocumentBuilderFactory.newDocumentBuilder();
            Document tmpDoc = tmpDocBuilder.parse(this.getClass().getClassLoader().getResource(
                    this.configuration.getProperty("mortar.descriptionsFolder")
                            + this.configuration.getProperty("mortar.tools.description.name")).toExternalForm());
            if (tmpDoc == null) {
                throw new FileNotFoundException("Tools description XML file not found.");
            }
            // optional, but recommended
            // http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            tmpDoc.getDocumentElement().normalize();
            NodeList tmpList = tmpDoc.getElementsByTagName("externalTool");
            for (int i = 0; i < tmpList.getLength(); i++) {
                Node tmpNode = tmpList.item(i);
                if (tmpNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element tmpElem = (Element) tmpNode;
                    String tmpName = tmpElem.getElementsByTagName("name").item(0).getTextContent();
                    String tmpVersion = tmpElem.getElementsByTagName("version").item(0).getTextContent();
                    String tmpAuthor = tmpElem.getElementsByTagName("author").item(0).getTextContent();
                    String tmpLicense = tmpElem.getElementsByTagName("license").item(0).getTextContent();
                    ExternalTool tmpTool = new ExternalTool(tmpName, tmpVersion, tmpAuthor, tmpLicense);
                    this.toolObservableList.add(tmpTool);
                }
            }
        } catch (ParserConfigurationException | IOException | SAXException | NullPointerException anException) {
            AboutViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            ExternalTool tmpTool = new ExternalTool(Message.get("AboutViewController.Error.XMLParsing.Name"),
                    Message.get("AboutViewController.Error.XMLParsing.Version"),
                    Message.get("AboutViewController.Error.XMLParsing.Author"),
                    Message.get("AboutViewController.Error.XMLParsing.License"));
            this.toolObservableList.add(tmpTool);
        }
    }
    //</editor-fold>
}
