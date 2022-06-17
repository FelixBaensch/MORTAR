/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2022  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.gui.views.HistogramView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.depict.DepictionUtil;
import de.unijena.cheminf.mortar.model.util.ListUtil;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Kekulization;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller class for the HistogramView
 *
 * @author Betuel Sevindik
 */
public class HistogramViewController {

    //<editor-fold desc="private final class variables" defaultstate="collapsed">
    /**
     * Stage for the MainView
     */
    private final Stage mainStage;
    /**
     * Logger of this class
     */
    private static final Logger LOGGER = Logger.getLogger(HistogramViewController.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * HistogramView
     */
    private HistogramView histogramView;
    /**
     * Stage of the HistogramView
     */
    private Stage histogramStage;
    /**
     * Copy of list
     */
    private List<MoleculeDataModel> copyList;
    /**
     * Width of the images
     */
    private double imageWidth = GuiDefinitions.GUI_IMAGE_WIDTH;
    /**
     * Height of the images
     */
    private double imageHeight = GuiDefinitions.GUI_IMAGE_HEIGHT;
    /**
     * Zoom factor of the images
     */
    private double imageZoomFactor = GuiDefinitions.GUI_IMAGE_ZOOM_FACTOR;
    //</editor-fold>
    //
    /**
     * Constructor
     *
     * @param aMainStage        Stage of the MainView
     * @param aMoleculeDataList
     */
    public HistogramViewController(Stage aMainStage, List<MoleculeDataModel> aMoleculeDataList)  {
        this.mainStage = aMainStage;
        this.copyList = new ArrayList<>(aMoleculeDataList);
        this.openHistogramView();
    }
    //
    //<editor-fold desc="private methods" defaultstate="collapsed">
    /**
     * Initialises stage and view and opens view in the initialised stage
     */
    private void openHistogramView()  {
        this.histogramStage = new Stage();
        this.histogramStage.initModality(Modality.WINDOW_MODAL);
        this.histogramStage.initOwner(this.mainStage);
        this.histogramStage.setTitle(Message.get("HistogramView.title"));
        this.histogramStage.setMinHeight(GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.histogramStage.setMinWidth(GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE);
        this.histogramView = new HistogramView();
        CategoryAxis tmpMainCategoryAxis = this.histogramView.getCategoryAxis();
        BarChart tmpMainBarChart =  this.createHistogram(0, this.histogramView, 25);
        Platform.runLater(()->{
           this.setMaxBarWidth(30, tmpMainCategoryAxis, tmpMainBarChart);
        });
        this.addListener();
        Scene tmpScene = new Scene(this.histogramView, GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE, GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.histogramStage.setScene(tmpScene);
        tmpScene.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number tmpOldNumber, Number tmpNewNumber) {
                if(tmpScene.getWidth() !=GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE){
                   imageWidth = GuiDefinitions.GUI_IMAGE_WIDTH_FOR_FULL_SCREEN;
                   imageHeight = GuiDefinitions.GUI_IMAGE_HEIGHT_FOR_FULL_SCREEN;
                   imageZoomFactor = GuiDefinitions.GUI_IMAGE_ZOOM_FACTOR_FOR_FULL_SCREEN;
                } else {
                   imageWidth = GuiDefinitions.GUI_IMAGE_WIDTH;
                   imageHeight = GuiDefinitions.GUI_IMAGE_HEIGHT;
                   imageZoomFactor = GuiDefinitions.GUI_IMAGE_ZOOM_FACTOR;
                }
            }
        });
        this.histogramStage.show();
    }
    //
    /**
     * Create a configurable histogram
     *
     * @param aNumber to set the fragment number
     * @param aHistogramView to display the images of the fragments
     * @param aSmilesLength to set the SMILES length
     * @return a BarChart to show
     */
    private BarChart createHistogram(int aNumber, HistogramView aHistogramView, int aSmilesLength)  {
        XYChart.Series tmpSeries = new XYChart.Series();
        BarChart tmpHistogramBarChart = aHistogramView.getBar();
        tmpHistogramBarChart.setCategoryGap(0);
        tmpHistogramBarChart.setBarGap(0);
        NumberAxis tmpNumberAxis = aHistogramView.getNumberAxis();
        ListUtil.sortGivenFragmentListByPropertyAndSortType(this.copyList, "absoluteFrequency", "ASCENDING");
        String tmpNewSmiles;
        ArrayList<Double> tmpArrayFrequency = new ArrayList<>();
        ArrayList<String> tmpSmilesList = new ArrayList<>();
        ArrayList<Integer> tmpFrequencyList = new ArrayList<>();
        int tmpIterator = this.copyList.size();
        FragmentDataModel tmpFragmentData = null;
        double tmpMaxOfFrequency;
        ImageView tmpStructureViewer = aHistogramView.getImageStructure();
        ArrayList<String> tmpFullSmilesLength = new ArrayList<>();
        for (MoleculeDataModel tmpMoleculeData : this.copyList) {
             tmpFragmentData = (FragmentDataModel) tmpMoleculeData;
            if (tmpFragmentData.getUniqueSmiles().length() > aSmilesLength) {
                tmpNewSmiles ="SMILES too long ("+tmpIterator+")";
                tmpSmilesList.add(tmpNewSmiles);
            } else {
                tmpNewSmiles = tmpFragmentData.getUniqueSmiles();
                tmpSmilesList.add(tmpNewSmiles);
            }
            try {
              Image image = DepictionUtil.depictImageWithZoom(tmpFragmentData.getAtomContainer(),2.5,250,150);
            } catch (CDKException anException) {
            }
            tmpIterator--;
            tmpFullSmilesLength.add(tmpFragmentData.getUniqueSmiles());
            tmpFrequencyList.add(tmpFragmentData.getAbsoluteFrequency());
            int tmpAbsolutFragmentFrequency = tmpFragmentData.getAbsoluteFrequency();
            double tmpAbsolutFrequencyInDouble = tmpAbsolutFragmentFrequency;
            tmpArrayFrequency.add(tmpAbsolutFrequencyInDouble);
        }
        tmpMaxOfFrequency = Collections.max(tmpArrayFrequency);
        // set the number axis dynamically with magic numbers //TODO TEST !!!
        NumberAxis tmpReadableAxis = aHistogramView.getNumberAxis();
        if (tmpMaxOfFrequency <= 35) {
            tmpReadableAxis.setTickUnit(1);
            tmpNumberAxis.setUpperBound(tmpMaxOfFrequency +1);
        } else if (tmpMaxOfFrequency <= 99) {
            tmpReadableAxis.setTickUnit(2);
            tmpNumberAxis.setUpperBound(tmpMaxOfFrequency +10);
        } else if (tmpMaxOfFrequency <=200) {
            tmpReadableAxis.setTickUnit(5);
            tmpNumberAxis.setUpperBound(tmpMaxOfFrequency +15);
        } else if (tmpMaxOfFrequency <=500) {
            tmpReadableAxis.setTickUnit(14);
            tmpNumberAxis.setUpperBound(tmpMaxOfFrequency +15);
        } else if (tmpMaxOfFrequency <=999) {
            tmpReadableAxis.setTickUnit(25);
            tmpNumberAxis.setUpperBound(tmpMaxOfFrequency +15);
        } else if (tmpMaxOfFrequency <= 3000) {
            tmpReadableAxis.setTickUnit(100);
            tmpNumberAxis.setUpperBound(tmpMaxOfFrequency +150);
        } else if (tmpMaxOfFrequency <= 7000) {
            tmpReadableAxis.setTickUnit(200);
            tmpNumberAxis.setUpperBound(tmpMaxOfFrequency +150);
        } else if (tmpMaxOfFrequency <= 10000) {
            tmpReadableAxis.setTickUnit(300);
            tmpNumberAxis.setUpperBound(tmpMaxOfFrequency +250);
        } else if (tmpMaxOfFrequency <= 70000) {
            tmpReadableAxis.setTickUnit(1800);
            tmpNumberAxis.setUpperBound(tmpMaxOfFrequency +350);
        }
        List<String> tmpSublistSmiles = null;
        List<Integer> tmpSublistOfFrequency = null;
        List<String> tmpSmilesToDepict = null;
        if (aNumber == 0) {
            tmpSublistSmiles = tmpSmilesList.subList(aNumber, tmpSmilesList.size());
            tmpSublistOfFrequency = tmpFrequencyList.subList(aNumber, tmpFrequencyList.size());
            tmpSmilesToDepict = tmpFullSmilesLength.subList(aNumber, tmpFullSmilesLength.size());
            Label tmpDisplayMaxFrequency = aHistogramView.getDefaultLabel();
            tmpDisplayMaxFrequency.setText("Fragment frequency (max."+this.copyList.size()+"):");
        }
        else {
            try {
                if (aNumber <= tmpSmilesList.size()) {
                    tmpSublistSmiles = tmpSmilesList.subList(tmpSmilesList.size() - aNumber, tmpSmilesList.size());
                    tmpSublistOfFrequency = tmpFrequencyList.subList(tmpFrequencyList.size() - aNumber, tmpFrequencyList.size());
                    tmpSmilesToDepict = tmpFullSmilesLength.subList(tmpFullSmilesLength.size()- aNumber, tmpFullSmilesLength.size());
                } else {
                    throw new IllegalArgumentException("the given number exceeds the maximum number of fragments");
                }
            } catch (IllegalArgumentException anException) {
                HistogramViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                GuiUtil.guiExceptionAlert(Message.get("HistogramViewController.HistogramRefreshError.Title"), Message.get("HistogramViewController.HistogramError.Header"),
                        Message.get("HistogramViewController.HistogramError.Content"), anException);
            }
        }
        CheckBox tmpBoxLabelHistogram = aHistogramView.getCheckbox();
        for (Iterator tmpStringIterator = tmpSublistSmiles.iterator(), tmpIntegerIterator = tmpSublistOfFrequency.iterator(), tmpSmilesIterator = tmpSmilesToDepict.iterator(); tmpStringIterator.hasNext() && tmpIntegerIterator.hasNext() && tmpSmilesIterator.hasNext();) {
            Integer tmpCurrentFrequency = (Integer) tmpIntegerIterator.next();
            String tmpCurrentSmiles = (String) tmpStringIterator.next();
            String tmpSmiles = (String) tmpSmilesIterator.next();
            XYChart.Data<Number, String> tmpStringNumberData = new XYChart.Data(tmpCurrentFrequency, tmpCurrentSmiles);
            StackPane tmpNode = this.histogramHover(tmpStructureViewer, tmpSmiles);
            tmpStringNumberData.setNode(tmpNode);
            tmpNode.setStyle("-fx-bar-fill: #0000FF");
            this.getBar(tmpBoxLabelHistogram, tmpNode, tmpCurrentFrequency, tmpMaxOfFrequency);
            tmpSeries.getData().add(tmpStringNumberData);
        }
        double tmpHistogramSize1 = 50.0 * tmpSublistOfFrequency.size(); //magic number
        tmpHistogramBarChart.setPrefHeight(tmpHistogramSize1);
        tmpHistogramBarChart.setMinHeight(tmpHistogramSize1);
        tmpHistogramBarChart.setLegendVisible(false);
        tmpHistogramBarChart.getData().clear();
        tmpHistogramBarChart.layout();
        tmpHistogramBarChart.getData().add(tmpSeries);
        tmpHistogramBarChart.setLegendVisible(false);
        tmpHistogramBarChart.setAnimated(false);
        tmpHistogramBarChart.setVerticalGridLinesVisible(false);
        tmpHistogramBarChart.setHorizontalGridLinesVisible(false);
        return tmpHistogramBarChart;
    }
    //
    /**
     * Add listeners
     *
     */
    private void addListener() {
        this.histogramView.getCancelButton().setOnAction(event -> {
            this.histogramStage.close();
        });
        TextField tmpFrequencyTextField = this.histogramView.getFrequencyTextField();
        TextField tmpSmilesTextField = this.histogramView.getSmilesTextField();
            this.histogramView.getRefreshButton().disableProperty().bind(
                    Bindings.isEmpty(tmpFrequencyTextField.textProperty()).and(Bindings.isEmpty(tmpSmilesTextField.textProperty()))
            );
        this.histogramView.getRefreshButton().setOnAction(event -> {
            try {
                BarChart tmpRefreshBarChart;
                CategoryAxis tmpCategoryAxis = this.histogramView.getCategoryAxis();
                String tmpStringMaxFrequency = this.histogramView.getTextField();
                String tmpMaxSmilesLength = this.histogramView.getSmilesField();
                int tmpFragmentNumber;
                int tmpSmilesLengthInField;
                if (tmpMaxSmilesLength.isEmpty()) {
                    tmpFragmentNumber = Integer.parseInt(tmpStringMaxFrequency);
                    if (tmpFragmentNumber > this.copyList.size() || tmpFragmentNumber < 0) {
                        GuiUtil.guiMessageAlert(Alert.AlertType.WARNING, Message.get("HistogramViewController.HistogramGeneralRefreshWarning.Title"),
                                Message.get("HistogramViewController.HistogramFrequencyRefreshWarning.Header"),
                                Message.get("HistogramViewController.HistogramFrequencyRefreshWarning.Content"));
                        return;
                    }
                    tmpRefreshBarChart = this.createHistogram(tmpFragmentNumber, this.histogramView, 25);
                    Platform.runLater(() -> {
                        this.setMaxBarWidth(30, tmpCategoryAxis, tmpRefreshBarChart); // magic number
                    });
                } else if (tmpStringMaxFrequency.isEmpty()) {
                    tmpSmilesLengthInField = Integer.parseInt(tmpMaxSmilesLength);
                    if (tmpSmilesLengthInField < 0) {
                        GuiUtil.guiMessageAlert(Alert.AlertType.WARNING, Message.get("HistogramViewController.HistogramGeneralRefreshWarning.Title"),
                                Message.get("HistogramViewController.HistogramSmilesRefreshWarning.Header"),
                                Message.get("HistogramViewController.HistogramSmilesRefreshWarning.Content"));
                        return;
                    }
                    tmpRefreshBarChart = this.createHistogram(0, this.histogramView, tmpSmilesLengthInField);
                    Platform.runLater(() -> {
                        this.setMaxBarWidth(30, tmpCategoryAxis, tmpRefreshBarChart); // magic number
                    });
                } else {
                    tmpFragmentNumber = Integer.parseInt(tmpStringMaxFrequency);
                    tmpSmilesLengthInField = Integer.parseInt(tmpMaxSmilesLength);
                    if (tmpFragmentNumber > this.copyList.size() || tmpFragmentNumber < 0 || tmpSmilesLengthInField < 0) {
                        GuiUtil.guiMessageAlert(Alert.AlertType.WARNING, Message.get("HistogramViewController.HistogramGeneralRefreshWarning.Title"),
                                Message.get("HistogramViewController.HistogramGeneralRefreshWarning.Header"),
                                Message.get("HistogramViewController.HistogramGeneralRefreshWarning.Content"));
                        return;
                    }
                    tmpRefreshBarChart = this.createHistogram(tmpFragmentNumber, this.histogramView, tmpSmilesLengthInField);
                    Platform.runLater(() -> {
                        this.setMaxBarWidth(30, tmpCategoryAxis, tmpRefreshBarChart); // magic number
                    });
                }
            } catch (NumberFormatException anException) {
                HistogramViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                GuiUtil.guiExceptionAlert(Message.get("HistogramViewController.HistogramRefreshError.Title"), Message.get
                                ("HistogramViewController.HistogramRefreshError.Header"),
                        Message.get("HistogramViewController.HistogramRefreshError.Content"), anException);
            }
        });
    }
    //
    /**
     * Make the histogram hoverable
     *
     * @param anImageView to display the structures
     * @return a StackPane that is used as a node
     */
    private StackPane histogramHover(ImageView anImageView, String aSmiles) {
        StackPane tmpNodePane = new StackPane();
        tmpNodePane.setAlignment(Pos.CENTER_RIGHT); // TODO better position
        boolean tmpKeepAtomContainer = true;
        tmpNodePane.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                tmpNodePane.setStyle("-fx-bar-fill: #87CEFA");
                SmilesParser tmpSmiPar = new SmilesParser(DefaultChemObjectBuilder.getInstance());
                IAtomContainer tmpAtomCon = null;
                if (tmpKeepAtomContainer) {
                    if (tmpAtomCon == null) {
                        try {
                            tmpSmiPar.kekulise(false);
                            tmpAtomCon = tmpSmiPar.parseSmiles(aSmiles);
                            AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpAtomCon);
                            Kekulization.kekulize(tmpAtomCon);
                        } catch (CDKException anException) {
                            HistogramViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                        }
                    }
                    Image tmpStructureImage = DepictionUtil.depictImageWithZoom(tmpAtomCon, imageZoomFactor, imageWidth, imageHeight);
                    anImageView.setImage(tmpStructureImage);
                    tmpNodePane.setCursor(Cursor.HAND);
                }
                try {
                //kekulization done separately below
                    tmpSmiPar.kekulise(false);
                    IAtomContainer tmpAtomContainer = tmpSmiPar.parseSmiles(aSmiles);
                    AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpAtomContainer);
                    Kekulization.kekulize(tmpAtomContainer);
                    Image image = DepictionUtil.depictImageWithZoom(tmpAtomContainer, imageZoomFactor,imageWidth, imageHeight);
                    anImageView.setImage(image);
                    tmpNodePane.setCursor(Cursor.HAND);
                } catch (CDKException anException) {
                   HistogramViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                }
            }
        });
        tmpNodePane.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                tmpNodePane.setStyle("-fx-bar-fill: #0000FF");
                anImageView.setImage(null);
                tmpNodePane.setCursor(Cursor.HAND);
            }
        });
        return tmpNodePane;
    }
    //
    /**
     * Make the bar width in the histogram uniform
     *
     * @param aMaxBarWidth sets the bar widths
     * @param aXAxis
     * @param aBarChart
     * @author José Pereda, Betuel Sevindik
     * @source https://stackoverflow.com/questions/27302875/set-bar-chart-column-width-size
     */
    private void setMaxBarWidth(double aMaxBarWidth, CategoryAxis aXAxis, BarChart aBarChart) {
        double tmpBarWidth = 0;
        do {
            double tmpCategorySpacing = aXAxis.getCategorySpacing();
            double tmpAvailableBarSpace = tmpCategorySpacing - (aBarChart.getCategoryGap() + aBarChart.getBarGap());
            tmpBarWidth = (tmpAvailableBarSpace / aBarChart.getData().size()) - aBarChart.getBarGap();
            if (tmpBarWidth > aMaxBarWidth) {
                tmpAvailableBarSpace = (aMaxBarWidth + aBarChart.getBarGap()) * aBarChart.getData().size();
                aBarChart.setCategoryGap(tmpCategorySpacing - tmpAvailableBarSpace - aBarChart.getBarGap());
            }
        } while (tmpBarWidth > aMaxBarWidth);
    }
    //
    /**
     * Enables the labelling of the histogram
     *
     * @param aCheckBox to make the display of the fragment labels adjustable
     * @param aStackPane
     * @param aFrequency values of the frequencies
     * @param aDigitNumber
     * @return StackPane
     */
    private StackPane getBar(CheckBox aCheckBox, StackPane aStackPane, int aFrequency, double aDigitNumber) {
        Label tmpLabel = new Label();
        tmpLabel.setTranslateY(0);
        tmpLabel.setAlignment(Pos.CENTER_RIGHT);
        // TODO TEST
        // magic numbers
        if (aDigitNumber <= 99) {
            tmpLabel.setTranslateX(20);
        } else if (aDigitNumber <=999) {
            tmpLabel.setTranslateX(30);
        } else if (aDigitNumber <= 9999) {
            tmpLabel.setTranslateX(40);
        } else if (aDigitNumber <= 99999) {
           tmpLabel.setTranslateX(50);
        }
        tmpLabel.setText(String.valueOf(aFrequency));
        if(aCheckBox.isSelected()) {
            aStackPane.getChildren().add(tmpLabel);
        }
        aCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if (aCheckBox.isSelected()) {
                aStackPane.getChildren().add(tmpLabel);
            } else {
                aStackPane.getChildren().clear();
            }
        });
        return aStackPane;
    }
    //<editor-fold>
}








