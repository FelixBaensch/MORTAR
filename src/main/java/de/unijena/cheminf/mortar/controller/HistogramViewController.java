package de.unijena.cheminf.mortar.controller;

import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.views.HistogramView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
     * a list of FragmentDataModel instances
     */
    private List<MoleculeDataModel> list;
    //</editor-fold>
    //
    /**
     * Constructor
     *
     * @param aMainStage Stage ot the MainView
     * @param aMoleculeDataList
     */
    public HistogramViewController(Stage aMainStage, List<MoleculeDataModel> aMoleculeDataList) {
        this.mainStage = aMainStage;
        this.list = aMoleculeDataList;
        this.openHistogramView();
    }
    //
    //<editor-fold desc="private methods" defaultstate="collapsed">
    /**
     * Initialises stage and view and opens view in the initialised stage
     */
    private void openHistogramView() {
        this.histogramStage = new Stage();
        this.histogramStage.initModality(Modality.WINDOW_MODAL);
        this.histogramStage.initOwner(this.mainStage);
        this.histogramStage.setTitle(Message.get("HistogramView.title"));
        this.histogramStage.setMinHeight(GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.histogramStage.setMinWidth(GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE);
        this.createHistogram();
        this.addListener(this.list);
        this.histogramStage.showAndWait();
    }
    private void createHistogram() {
        XYChart.Series tmpSeries = new XYChart.Series();
        CategoryAxis tmpXAxis = new CategoryAxis();
        NumberAxis tmpYAxis = new NumberAxis();
        BarChart<Number, String> tmpHistogramBarChart = new BarChart<>(tmpYAxis, tmpXAxis);
        tmpHistogramBarChart.setBarGap(-0.5);
        tmpYAxis.setAutoRanging(false);
        tmpYAxis.setTickUnit(1);
        tmpYAxis.setForceZeroInRange(true);
        tmpXAxis.setLabel(Message.get("Histogram.XAxisLabel.text"));
        tmpYAxis.setLabel(Message.get("Histogram.YAxisLabel.text"));
        tmpXAxis.setTickLabelsVisible(true);
        //tmpHistogramBarChart.setPrefWidth(2000.0);
       //tmpHistogramBarChart.setMinWidth(2000.0);
       tmpHistogramBarChart.setPrefHeight(1000.0);
       tmpHistogramBarChart.setMinHeight(1000.0);
       String tmpNewSmiles;
       ArrayList<Double> tmpArrayFrequency = new ArrayList<Double>();
       int tmpIterrator = 0;
        for (MoleculeDataModel tmpMoleculeData : this.list) {
            FragmentDataModel tmpFragmentData = (FragmentDataModel) tmpMoleculeData;
            if (tmpFragmentData.getUniqueSmiles().length() > 13){
                tmpNewSmiles = tmpFragmentData.getUniqueSmiles().substring(0, 12) + "...";
                //i++;
            }
            else{
                tmpNewSmiles = tmpFragmentData.getUniqueSmiles();
                //  tmpNewSmiles = Integer.toString(i);
                // i++;
            }
            int tmpAbsolutFragmentFrequency = tmpFragmentData.getAbsoluteFrequency();
            ImageView image = tmpFragmentData.getStructure();
            double tmpAbsolutFrequencyInDouble = tmpAbsolutFragmentFrequency;
            tmpArrayFrequency.add(tmpAbsolutFrequencyInDouble);
            Double tmpMaxofFrequency = Collections.max(tmpArrayFrequency);
            tmpYAxis.setUpperBound(tmpMaxofFrequency +1);
            tmpIterrator++;
            this.sortHistogram(this.list);
            //XYChart.Data<String, Number> tmpStringNumberData = new XYChart.Data(tmpNewSmiles,tmpFragmentData.getAbsoluteFrequency());

           XYChart.Data<Number, String> tmpStringNumberData = new XYChart.Data(tmpFragmentData.getAbsoluteFrequency(), tmpNewSmiles);
            //XYChart.Data<Number, String> tmpStringNumberData = new XYChart.Data(tmpFragmentData.getAbsoluteFrequency(),image);
            tmpStringNumberData.nodeProperty().addListener(new ChangeListener<Node>() {
                @Override
                public void changed(ObservableValue<? extends Node> tmpObservableValue, Node tmpOldNode, Node tmpNode) {
                    //displayLabelForData(tmpStringNumberData);
                    tmpNode.setStyle("-fx-bar-fill: blue;");
                    // displayLabelForData(tmpStringNumberData);
                }
            });
            tmpSeries.getData().add(tmpStringNumberData);
        }
        this.histogramView = new HistogramView(tmpHistogramBarChart);
        Scene tmpScene = new Scene(this.histogramView,GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE, GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.histogramStage.setScene(tmpScene);
        tmpHistogramBarChart.getData().add(tmpSeries);
        tmpHistogramBarChart.setLegendVisible(false);
    }
    private void displayLabelForData(XYChart.Data<Number, String> aData) {
        Node tmpNode = aData.getNode();
        Text tmpDataText = new Text(aData.getYValue() + "");
        tmpNode.parentProperty().addListener(new ChangeListener<Parent>() {
            @Override public void changed(ObservableValue<? extends Parent> ov, Parent oldParent, Parent parent) {
                Group parentGroup = (Group) parent;
                parentGroup.getChildren().add(tmpDataText);
            }
        });
        tmpNode.boundsInParentProperty().addListener(new ChangeListener<Bounds>() {
            @Override public void changed(ObservableValue<? extends Bounds> ov, Bounds oldBounds, Bounds bounds) {

                tmpDataText.setLayoutX(
                        Math.round(bounds.getMinX() + bounds.getWidth() / 2 - tmpDataText.prefWidth(-1) /2) // /2
                );


                tmpDataText.setLayoutY(Math.round(bounds.getMinY() - tmpDataText.prefHeight(-1)*0.5)
                );
            }
        });
    }
    private void sortHistogram(List<MoleculeDataModel> aList) {
        Collections.sort(aList, (m1, m2) -> {
                FragmentDataModel f1;
                FragmentDataModel f2;
                f1 = (FragmentDataModel) m1;
                f2 = (FragmentDataModel) m2;
                return (Integer.compare(f1.getAbsoluteFrequency(), f2.getAbsoluteFrequency()));
            //return (f1.getAbsoluteFrequency() > f2.getAbsoluteFrequency() ? -1 : (f1.getAbsoluteFrequency() == f2.getAbsoluteFrequency() ? 0 : 1));
        });
    }
    private void addListener(List<MoleculeDataModel> aList) {
        this.histogramView.getCancelButton().setOnAction(event -> {
            this.histogramStage.close();
        });
        this.histogramView.getSortButton().setOnAction(event -> {

        });


    }

}






