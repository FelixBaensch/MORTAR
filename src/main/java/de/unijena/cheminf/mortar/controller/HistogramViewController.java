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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.Cursor;

import org.openscience.cdk.exception.CDKException;

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
        this.createHistogram(0);
        this.addListener();  // TODO  is the listener on the right position?
        this.histogramStage.showAndWait();
    }
    //
    /**
     * create a configurable histogram
     *
     * @param aFirstNumber
     */
    private void createHistogram(Integer aFirstNumber) {
            XYChart.Series tmpSeries = new XYChart.Series();
            CategoryAxis tmpXAxis = new CategoryAxis();
            final NumberAxis tmpYAxis = new NumberAxis();
            BarChart<Number, String> tmpHistogramBarChart = new BarChart<>(tmpYAxis, tmpXAxis);
            tmpHistogramBarChart.setBarGap(0);
            tmpYAxis.setAutoRanging(false);
            tmpYAxis.setTickUnit(1);
            tmpYAxis.setForceZeroInRange(true);
            tmpXAxis.setLabel(Message.get("Histogram.XAxisLabel.text"));
            tmpYAxis.setLabel(Message.get("Histogram.YAxisLabel.text"));
            tmpXAxis.setTickLabelsVisible(true);
            ListUtil.sortGivenFragmentListByPropertyAndSortType(this.copyList, "absoluteFrequency", "ASCENDING");
            //tmpHistogramBarChart.setPrefWidth(2000.0);
            //tmpHistogramBarChart.setMinWidth(2000.0);
            tmpHistogramBarChart.setPrefHeight(1000.0);
            tmpHistogramBarChart.setMinHeight(1000.0);
            String tmpNewSmiles;
            ArrayList<Double> tmpArrayFrequency = new ArrayList<Double>();
            ArrayList<String> tmpSmilesList = new ArrayList<>();
            ArrayList<Integer> tmpFrequencyList = new ArrayList<>();
            ArrayList<Image> tmpFragmentImage = new ArrayList<>();
            int tmpIterator = 1;
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
                    Image tmpImageOfStructure = DepictionUtil.depictImageWithZoom(tmpFragmentData.getAtomContainer(), 2.5, 250, 150);
                    tmpFragmentImage.add(tmpImageOfStructure);
                } catch (CDKException anException) {
                    HistogramViewController.LOGGER.getLogger(MoleculeDataModel.class.getName()).log(Level.SEVERE, anException.toString() + "_" + tmpFragmentData.getName(), anException);
                    // TODO
                }
                tmpFrequencyList.add(tmpFragmentData.getAbsoluteFrequency());
                int tmpAbsolutFragmentFrequency = tmpFragmentData.getAbsoluteFrequency();
                double tmpAbsolutFrequencyInDouble = tmpAbsolutFragmentFrequency;
                tmpArrayFrequency.add(tmpAbsolutFrequencyInDouble);
                Double tmpMaxofFrequency = Collections.max(tmpArrayFrequency);
                tmpYAxis.setUpperBound(tmpMaxofFrequency + 1);
            }
            this.histogramView = new HistogramView(tmpHistogramBarChart, tmpSmilesList);
            ImageView tmpViewforStructure = this.histogramView.getImageStructure();
            List<String> tmpSublistFornewChart = null;
            List<Integer> tmpSublistofFrequency = null;
            List<Image> tmpImage = null;
            if (aFirstNumber == 0) {
                tmpSublistFornewChart = tmpSmilesList.subList(aFirstNumber, tmpSmilesList.size());
                tmpSublistofFrequency = tmpFrequencyList.subList(aFirstNumber, tmpFrequencyList.size());
                tmpImage = tmpFragmentImage.subList(aFirstNumber, tmpFragmentImage.size());
            }
            /**else if (aFirstNumber> tmpListofFrequency.size() || aFirstNumber> tmpListofSmiles.size() || aFirstNumber> tmpFragmentImage.size()) {
             Exception tmpToLongInteger = new IllegalArgumentException("Falsch");
             HistogramViewController.LOGGER.log(Level.SEVERE, tmpToLongInteger.toString(), tmpToLongInteger);
             // TODO catch exception if afirstNumber over the lists sizes
             }*/
            else {
                tmpSublistFornewChart = tmpSmilesList.subList(tmpSmilesList.size() - aFirstNumber, tmpSmilesList.size());
                tmpSublistofFrequency = tmpFrequencyList.subList(tmpFrequencyList.size() - aFirstNumber, tmpFrequencyList.size());
                tmpImage = tmpFragmentImage.subList(tmpFragmentImage.size() - aFirstNumber, tmpFragmentImage.size());
            }
            List<Object> tmpNewList = new ArrayList<>();
            for (Iterator tmpStringIterator = tmpSublistFornewChart.iterator(), tmpIntegerIterator = tmpSublistofFrequency.iterator(), tmpImageIterator = tmpImage.iterator(); tmpStringIterator.hasNext() && tmpIntegerIterator.hasNext() && tmpImageIterator.hasNext(); ) {
                Integer tmpCurrentFrequency = (Integer) tmpIntegerIterator.next();
                String tmpCurrentSmiles = (String) tmpStringIterator.next();
                Image tmpCurrentStructure = (Image) tmpImageIterator.next();
                tmpNewList.add(tmpCurrentSmiles);
                tmpNewList.add(tmpCurrentFrequency);
                XYChart.Data<Number, String> tmpStringNumberData = new XYChart.Data(tmpCurrentFrequency, tmpCurrentSmiles);
                Node tmpNode = this.histogramHover(tmpCurrentStructure, tmpViewforStructure, tmpCurrentFrequency);
                tmpStringNumberData.setNode(tmpNode);
                tmpNode.setStyle("-fx-bar-fill: #0000FF");
                tmpSeries.getData().add(tmpStringNumberData);
            }
            tmpHistogramBarChart.getData().add(tmpSeries);
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
        this.histogramView.getRefreshButton().setOnAction(event -> {
            try {
            String tmpStringNumbersofFragments = this.histogramView.getTextField();
            int tmpIntegerNumberofFragments = Integer.parseInt(tmpStringNumbersofFragments);
            this.createHistogram(tmpIntegerNumberofFragments);
            } catch(NumberFormatException anException) {
                HistogramViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                GuiUtil.guiExceptionAlert(Message.get("HistogramViewController.HistogramRefreshError.Title"), Message.get
                        ("HistogramViewController.HistogramRefreshError.Header"),
                        Message.get("HistogramViewController.HistogramRefreshError.Content") , anException);
            }
        });
    }
    //
    /**
     * Method to make histogram hoverable
     *
     * @param anImage
     * @param anImageView
     * @param aValue
     * @return
     */
    private Node histogramHover(Image anImage, ImageView anImageView, int aValue) {
        StackPane tmpNodePane = new StackPane();
        tmpNodePane.setPrefHeight(15);
        tmpNodePane.setPrefWidth(15);
        tmpNodePane.setAlignment(Pos.CENTER_RIGHT); // TODO better position
        Label tmpValueLabel = new Label(aValue +"");
        //tmpNodePane.getChildren().setAll(tmpValueLabel);  // set the values after create a better positions for the labels
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








