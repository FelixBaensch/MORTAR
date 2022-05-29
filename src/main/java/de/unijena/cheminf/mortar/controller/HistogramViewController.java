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
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.Cursor;

import org.openscience.cdk.exception.CDKException;

import javax.swing.text.Position;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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
     * A list of FragmentDataModel instances
     */
    private List<MoleculeDataModel> list;
    /**
     * Copy of list
     */
    private List<MoleculeDataModel> copyList;
    //</editor-fold>
    //
    /**
     * Constructor
     *
     * @param aMainStage        Stage ot the MainView
     * @param aMoleculeDataList
     */
    public HistogramViewController(Stage aMainStage, List<MoleculeDataModel> aMoleculeDataList)  {
        this.mainStage = aMainStage;
        this.list = aMoleculeDataList;
        this.copyList = new ArrayList<>(this.list);
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
        this.createHistogram();
        this.addListener(); // TODO  is the listener on the right position?
        this.histogramStage.show();
    }
    //
    /**
     * Create a configurable histogram
     *
     *
     */
    private void createHistogram() {
            XYChart.Series tmpSeries = new XYChart.Series();
            CategoryAxis tmpXAxis = new CategoryAxis();
            final NumberAxis tmpYAxis = new NumberAxis();
            BarChart<Number, String> tmpHistogramBarChart = new BarChart<>(tmpYAxis, tmpXAxis);
            tmpHistogramBarChart.setBarGap(0);
            //tmpHistogramBarChart.setCategoryGap(5.0);
            tmpYAxis.setAutoRanging(false);
            tmpYAxis.setTickUnit(1);
            tmpYAxis.setForceZeroInRange(true);
            tmpXAxis.setTickMarkVisible(true);
            tmpXAxis.setAutoRanging(true);
            tmpXAxis.setTickLabelFill(Color.BLACK);
            tmpYAxis.setTickLabelFill(Color.BLACK);
            tmpXAxis.setLabel(Message.get("Histogram.XAxisLabel.text"));
            tmpYAxis.setLabel(Message.get("Histogram.YAxisLabel.text"));
           // tmpXAxis.setTickLabelsVisible(true);
            ListUtil.sortGivenFragmentListByPropertyAndSortType(this.copyList, "absoluteFrequency", "ASCENDING");
            //tmpHistogramBarChart.setPrefWidth(2000.0);
            //tmpHistogramBarChart.setMinWidth(2000.0);
            tmpHistogramBarChart.setPrefHeight(2000.0); //TODO optimal display: Make adjustable depending on how many fragments are created
            tmpHistogramBarChart.setMinHeight(2000.0);
           // tmpHistogramBarChart.setStyle("-fx-background-radius: 0;");
            String tmpNewSmiles;
            ArrayList<Double> tmpArrayFrequency = new ArrayList<Double>();
            ArrayList<String> tmpSmilesList = new ArrayList<>();
            ArrayList<Integer> tmpFrequencyList = new ArrayList<>();
            ArrayList<Image> tmpFragmentImage = new ArrayList<>();
            int tmpIterator = 1;
            this.histogramView = new HistogramView(tmpHistogramBarChart);
            ImageView tmpStructureViewer = this.histogramView.getImageStructure();
            for (MoleculeDataModel tmpMoleculeData : this.copyList) {
                FragmentDataModel tmpFragmentData = (FragmentDataModel) tmpMoleculeData;
                if (tmpFragmentData.getUniqueSmiles().length() > 25) { // TODO make adjustable but where?
                    tmpNewSmiles = tmpIterator + ".Structure with to long Smiles";
                    tmpSmilesList.add(tmpNewSmiles);
                } else {
                    tmpNewSmiles = tmpFragmentData.getUniqueSmiles();
                    tmpSmilesList.add(tmpNewSmiles);
                }
                tmpIterator++;
                try {
                    Image tmpStructureImage = DepictionUtil.depictImageWithZoom(tmpFragmentData.getAtomContainer(), 2.5, 250, 150);
                    tmpFragmentImage.add(tmpStructureImage);
                } catch (CDKException anException) {
                    HistogramViewController.LOGGER.getLogger(MoleculeDataModel.class.getName()).log(Level.SEVERE, anException.toString() + "_" + tmpFragmentData.getName(), anException);
                    // TODO
                }
                XYChart.Data<Number, String> tmpStringNumberData = new XYChart.Data(tmpFragmentData.getAbsoluteFrequency(), tmpNewSmiles);
                tmpSeries.getData().add(tmpStringNumberData);
                for(Image tmpStructure: tmpFragmentImage) {
                    Node tmpMainNode = this.histogramHover(tmpStructure, tmpStructureViewer, tmpFragmentData.getAbsoluteFrequency());
                    tmpStringNumberData.setNode(tmpMainNode);
                    tmpMainNode.setStyle("-fx-bar-fill: #0000FF");
                }
                tmpFrequencyList.add(tmpFragmentData.getAbsoluteFrequency());
                int tmpAbsolutFragmentFrequency = tmpFragmentData.getAbsoluteFrequency();
                double tmpAbsolutFrequencyInDouble = tmpAbsolutFragmentFrequency;
                tmpArrayFrequency.add(tmpAbsolutFrequencyInDouble);
                Double tmpMaxOfFrequency = Collections.max(tmpArrayFrequency);
                tmpYAxis.setUpperBound(tmpMaxOfFrequency + 1);
            }
            TextField tmpDefaultFragmentsNumber = this.histogramView.getText();
            tmpDefaultFragmentsNumber.setText(String.valueOf(this.copyList.size()+ " max. fragments"));
            // actionListener refreshButton
            this.histogramView.getRefreshButton().setOnAction(event -> {
                XYChart.Series tmpRefreshSeries = new XYChart.Series();
                try {
                    String tmpStringFragmentsNumber = this.histogramView.getTextField();
                    int tmpFragmentsNumber = Integer.parseInt(tmpStringFragmentsNumber);
                    List<String> tmpSublistForNewChart = null;
                    List<Integer> tmpSublistOfFrequency = null;
                    List<Image> tmpImage = null;
                    try {
                        if (tmpFragmentsNumber < tmpSmilesList.size()) {
                            tmpSublistForNewChart = tmpSmilesList.subList(tmpSmilesList.size() - tmpFragmentsNumber, tmpSmilesList.size());
                            tmpSublistOfFrequency = tmpFrequencyList.subList(tmpFrequencyList.size() - tmpFragmentsNumber, tmpFrequencyList.size());
                            tmpImage = tmpFragmentImage.subList(tmpFragmentImage.size() - tmpFragmentsNumber, tmpFragmentImage.size());
                        } else {
                             throw new IllegalArgumentException("the given number exceeds the maximum number of fragments");
                        }
                    } catch (IllegalArgumentException anException) {
                         HistogramViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                        GuiUtil.guiExceptionAlert(Message.get("HistogramViewController.HistogramRefreshError.Title"), Message.get("HistogramViewController.HistogramError.Header"),
                                Message.get("HistogramViewController.HistogramError.Content"), anException);
                    }
                    List<Object> tmpNewList = new ArrayList<>();
                    for (Iterator tmpStringIterator = tmpSublistForNewChart.iterator(), tmpIntegerIterator = tmpSublistOfFrequency.iterator(), tmpImageIterator = tmpImage.iterator(); tmpStringIterator.hasNext() && tmpIntegerIterator.hasNext() && tmpImageIterator.hasNext(); ) {
                        Integer tmpCurrentFrequency = (Integer) tmpIntegerIterator.next();
                        String tmpCurrentSmiles = (String) tmpStringIterator.next();
                        Image tmpCurrentStructure = (Image) tmpImageIterator.next();
                        tmpNewList.add(tmpCurrentSmiles);
                        tmpNewList.add(tmpCurrentFrequency);
                        XYChart.Data<Number, String> tmpRefreshStringNumberData = new XYChart.Data(tmpCurrentFrequency, tmpCurrentSmiles);
                        Node tmpNode = this.histogramHover(tmpCurrentStructure, tmpStructureViewer, tmpCurrentFrequency);
                        tmpRefreshStringNumberData.setNode(tmpNode);
                        tmpNode.setStyle("-fx-bar-fill: #0000FF");
                        tmpRefreshSeries.getData().add(tmpRefreshStringNumberData);
                        tmpHistogramBarChart.getData().clear();
                    }
                } catch (NumberFormatException anException) {
                    HistogramViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                    GuiUtil.guiExceptionAlert(Message.get("HistogramViewController.HistogramRefreshError.Title"), Message.get
                                    ("HistogramViewController.HistogramRefreshError.Header"),
                            Message.get("HistogramViewController.HistogramRefreshError.Content"), anException);
                }
                    tmpHistogramBarChart.getData().add(tmpRefreshSeries);
            });
            tmpHistogramBarChart.setLegendVisible(false);
            tmpHistogramBarChart.getData().clear();
            tmpHistogramBarChart.layout();
            tmpHistogramBarChart.getData().add(tmpSeries);
            tmpHistogramBarChart.setLegendVisible(false);
            tmpHistogramBarChart.setAnimated(false);
            Scene tmpScene = new Scene(this.histogramView, GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE, GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
            this.histogramStage.setScene(tmpScene);
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
    }
    //
    /**
     * Make the histogram hoverable
     * @param anImage
     * @param anImageView
     * @return
     */
    private Node histogramHover(Image anImage, ImageView anImageView, int aValue) {
        StackPane tmpNodePane = new StackPane();
        tmpNodePane.setPrefHeight(15);
        tmpNodePane.setPrefWidth(15);
        tmpNodePane.setAlignment(Pos.CENTER_RIGHT); // TODO better position
        Label tmpValueLabel = new Label(aValue +"");
        tmpValueLabel.setTextFill(Color.BLACK);
        tmpNodePane.getChildren().setAll(tmpValueLabel);  // set the values after create a better positions for the labels
        tmpNodePane.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
              // pane.getChildren().setAll(label);
                tmpNodePane.setStyle("-fx-bar-fill: #87CEFA");
                anImageView.setImage(anImage);
                tmpNodePane.setCursor(Cursor.HAND);
                }
        });
        tmpNodePane.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
               // pane.getChildren().clear();
                tmpNodePane.setStyle("-fx-bar-fill: #0000FF");
                tmpNodePane.setCursor(Cursor.HAND);
            }
        });
        return tmpNodePane;
    }
    //<editor-fold>
}








