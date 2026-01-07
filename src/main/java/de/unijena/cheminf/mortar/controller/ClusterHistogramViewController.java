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
import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.gui.views.ClusterHistogramView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.util.IDisplayEnum;
import de.unijena.cheminf.mortar.model.util.SimpleIDisplayEnumConstantProperty;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class ClusterHistogramViewController implements IViewToolController{
    public static enum BarWidthOption implements IDisplayEnum {
        /**
         * Small bar width.
         */
        SMALL(Message.get("ClusterHistogramView.barWidths.small.displayName"),
                Message.get("ClusterHistogramView.barWidths.small.tooltip")),
        /**
         * Medium bar width.
         */
        MEDIUM(Message.get("ClusterHistogramView.barWidths.medium.displayName"),
                Message.get("ClusterHistogramView.barWidths.medium.tooltip")),
        /**
         * Large bar width.
         */
        LARGE(Message.get("ClusterHistogramView.barWidths.large.displayName"),
                Message.get("ClusterHistogramView.barWidths.large.tooltip"));
        /**
         * A name for the respective constant that is meant for display, i.e. taken from the Message file.
         */
        private final String displayName;
        /**
         * Language-specific tooltip text for display in GUI.
         */
        private final String tooltip;
        /**
         * Constructor setting the display name and tooltip.
         *
         * @param aDisplayName display name
         * @param aTooltip tooltip text
         */
        private BarWidthOption(String aDisplayName, String aTooltip) {
            this.displayName = aDisplayName;
            this.tooltip = aTooltip;
        }
        //
        @Override
        public String getDisplayName() {
            return this.displayName;
        }
        //
        @Override
        public String getTooltipText() {
            return this.tooltip;
        }
    }

    private static final boolean DEFAULT_DISPLAY_BAR_LABELS_SETTING = true;
    public static final int DEFAULT_NUMBER_OF_DISPLAYED_CLUSTER = 10;

    public static final ClusterHistogramViewController.BarWidthOption DEFAULT_BAR_WIDTH = ClusterHistogramViewController.BarWidthOption.LARGE;

    private static final Logger LOGGER = Logger.getLogger(ClusterHistogramViewController.class.getName());

    private final IConfiguration configuration;
    private final SimpleIntegerProperty displayedClustersNumberSetting;
    private final SimpleIDisplayEnumConstantProperty barWidthSetting;
    private final SimpleBooleanProperty displayBarLabelsSetting;
    private final SimpleBooleanProperty displayBarShadowsSetting;
    private final SimpleBooleanProperty displayGridLinesSetting;
    private final List<Property<?>> settings;

    //Todo: type -> general: data transfer? when clustering?
    private List<?> clusterListCopy;
    private BarChart<Number, String> clusterHistogramChart;

    private ClusterHistogramView clusterHistogramView;
    private Stage clusterHistogramStage;
    private Scene clusterHistogramScene;


    public ClusterHistogramViewController(IConfiguration aConfiguration) {
        this.configuration = aConfiguration;
        //todo: init cap
        this.settings = new ArrayList<>();
        this.displayedClustersNumberSetting = new SimpleIntegerProperty(this,
                //the name could be displayed but is not used for that currently
                Message.get("ClusterHistogramView.displayedClusterNumberSetting.name"),
                ClusterHistogramViewController.DEFAULT_NUMBER_OF_DISPLAYED_CLUSTER) {
            @Override
            public void set(int newValue) throws NullPointerException, IllegalArgumentException {
                super.setValue(newValue);
                //value transferred to GUI in openHistogramView()
                //value updated in addListenersToHistogramView(), listener of apply-button
            }
        };
        this.settings.add(this.displayedClustersNumberSetting);
        this.barWidthSetting = new SimpleIDisplayEnumConstantProperty(this,
                "Bar width setting",
                HistogramViewController.DEFAULT_BAR_WIDTH,
                HistogramViewController.BarWidthOption.class) {
            @Override
            public void set(IDisplayEnum newValue) throws NullPointerException, IllegalArgumentException {
                super.setValue(newValue);
            }
        };
        this.settings.add(this.barWidthSetting);
        this.displayBarLabelsSetting = new SimpleBooleanProperty(this,
                //the name could be displayed but is not used for that currently
                Message.get("Display labels on bars setting"),
                ClusterHistogramViewController.DEFAULT_DISPLAY_BAR_LABELS_SETTING) {
            @Override
            public void set(boolean newValue) {
                super.set(newValue);
                //value transferred to GUI in openHistogramView()
                //in addFrequencyBarLabelToBarAndAddListenersToBarCheckBoxes(), listener is added
            }
        };
        this.settings.add(this.displayBarLabelsSetting);
        this.displayBarShadowsSetting = new SimpleBooleanProperty(this,
                //the name could be displayed but is not used for that currently
                Message.get("HistogramView.displayBarShadowsSetting.name"),
                HistogramViewController.DEFAULT_DISPLAY_BAR_SHADOWS_SETTING) {
            @Override
            public void set(boolean newValue) {
                super.set(newValue);
                //value transferred to GUI in openHistogramView()
                //in addFrequencyBarLabelToBarAndAddListenersToBarCheckBoxes(), listener is added
            }
        };
        this.settings.add(this.displayBarShadowsSetting);
        this.displayGridLinesSetting = new SimpleBooleanProperty(this,
                //the name could be displayed but is not used for that currently
                Message.get("HistogramView.displayGridLinesSetting.name"),
                HistogramViewController.DEFAULT_DISPLAY_GRID_LINES_SETTING) {
            @Override
            public void set(boolean newValue) {
                super.set(newValue);
                //value transferred to GUI in openHistogramView()
                //value also used in createHistogram() and addListenersToHistogramView()
            }
        };
        this.settings.add(this.displayGridLinesSetting);
    };

    @Override
    public List<Property<?>> settingsProperties() {
        return List.of();
    }

    @Override
    public String getViewToolNameForDisplay() {
        return "";
    }

    @Override
    public void restoreDefaultSettings() {

    }

    @Override
    public boolean canBeUsedOnTab(TabNames aTabNameEnumConstant) {
        return false;
    }


    public void openClusterHistogramView(
            Stage aMainStage,
            List<MoleculeDataModel> aRepresentativeMoleculeDataModelList) {
        //ToDO: routine for opening cluster histogram (see HistogramView class)
        Objects.requireNonNull(aMainStage, "Main stage is null.");
        Objects.requireNonNull(aRepresentativeMoleculeDataModelList, "Given MoleculeDataModel list is null.");
        this.clusterHistogramView = new ClusterHistogramView(10);
        //set setting values
        this.clusterHistogramView.getDisplayBarLabelsCheckBox().setSelected(this.displayBarLabelsSetting.get());
        this.clusterHistogramView.getDisplayGridLinesCheckBox().setSelected(this.displayGridLinesSetting.get());
        this.clusterHistogramView.getDisplayBarShadowsCheckBox().setSelected(this.displayBarShadowsSetting.get());

        this.clusterHistogramStage = new Stage();
        this.addListenersToComponents();
        //Todo: calculateBarSpacing()
        Double[] tmpClusterHistogramBarSpacing = this.calculateBarSpacing(
                this.displayedClustersNumberSetting.get(),
                this.getBarWidthOptionEnumConstantFromDisplayName(
                        this.clusterHistogramView.getBarWidthsComboBox().getValue())
        );
        this.clusterHistogramChart = this.createClusterHistogram(
                this.displayedClustersNumberSetting.get(),
                this.clusterHistogramView,
                this.clusterHistogramView.getDisplayBarLabelsCheckBox(),
                this.clusterHistogramView.getDisplayBarShadowsCheckBox(),
                tmpClusterHistogramBarSpacing
        );
        this.clusterHistogramScene = new Scene(
                this.clusterHistogramView,
                GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE,
                GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE
        );
        this.clusterHistogramStage.initOwner(aMainStage);
        this.clusterHistogramStage.setTitle(Message.get("ClusterHistogramView.title"));
        String tmpIconURL = this.getClass().getClassLoader().getResource(
          this.configuration.getProperty("mortar.imagesFolder")
                  + this.configuration.getProperty("mortar.logo.icon.name")).toExternalForm();
        this.clusterHistogramStage.getIcons().add(new Image(tmpIconURL));

        this.clusterHistogramStage.setScene(this.clusterHistogramScene);
        this.clusterHistogramStage.show();

    }

    private Double[] calculateBarSpacing(
            int aNumberOfDisplayedClusters,
            BarWidthOption aBarWidthOption
    ) {
        //Todo: see histogram for reference
        return null;
    }

    private BarWidthOption getBarWidthOptionEnumConstantFromDisplayName(String aDisplayName) {
        //Todo: see histogram for reference
        return null;
    }

    private BarChart<Number, String> createClusterHistogram(
            int aClusterNumber,
            ClusterHistogramView aClusterHistogramView,
            CheckBox aDisplayBarLabelsCheckBox,
            CheckBox aDisplayBarShadowsCheckBox,
            Double[] aClusterHistogramBarSpacing
    ) {
        //Todo: get number of clusters -> check
        //y axis (clusters)
        CategoryAxis tmpYAxis = new CategoryAxis();
        tmpYAxis.setTickLabelFill(Color.BLACK);
        tmpYAxis.setLabel(Message.get("ClusterHistogramViewController.YAxisLabel.text"));
        System.out.println("y axis");
        //x axis (individual cluster size)
        NumberAxis tmpXAxis = new NumberAxis();
        tmpXAxis.setSide(Side.TOP);
        tmpXAxis.setAutoRanging(false);
        tmpXAxis.setMinorTickCount(1);
        tmpXAxis.setForceZeroInRange(true);
        tmpXAxis.setTickLabelFill(Color.BLACK);
        tmpXAxis.setLabel(Message.get("ClusterHistogramViewController.XAxisLabel.text"));
        System.out.println("x axis");
        //create bar chart
        BarChart<Number, String> tmpClusterHistogramBarChart = null;
        try {
            tmpClusterHistogramBarChart = new BarChart<>(tmpXAxis, tmpYAxis);
        } catch (Exception e) {
            System.out.println(e);
        }
        tmpClusterHistogramBarChart.setCategoryGap(0.0);
        tmpClusterHistogramBarChart.setBarGap(0.0);
        System.out.println("bar chart");
        ScrollPane tmpScrollPane = aClusterHistogramView.getClusterHistogramScrollPane();
        tmpScrollPane.setContent(tmpClusterHistogramBarChart);
        System.out.println("scroll pane");
        //create chart data
        //type order:    x   ,    y
        XYChart.Series<Number, String> tmpChartSeries = new XYChart.Series<>();
        //Todo: tmp! replace with actual data routine
        for (int i = 0; i < 10; i++) {
            XYChart.Data<Number, String> tmpClusterToSizeData
                    = new XYChart.Data<>(i, new String("# " + i));
            tmpChartSeries.getData().add(tmpClusterToSizeData);
        }
        System.out.println("chart series");


        //Todo: get list of cluster representatives + show when hover over cluster
        //layout + data add
        //todo: calc height value
        double tmpPlaceholder = 10;
        tmpClusterHistogramBarChart.setPrefHeight(tmpPlaceholder);
        tmpClusterHistogramBarChart.setMinHeight(tmpPlaceholder);
        tmpClusterHistogramBarChart.getData().add(tmpChartSeries);
        tmpClusterHistogramBarChart.setLegendVisible(false);
        tmpClusterHistogramBarChart.layout();
        tmpClusterHistogramBarChart.setHorizontalGridLinesVisible(this.displayGridLinesSetting.get());
        tmpClusterHistogramBarChart.setVerticalGridLinesVisible(this.displayGridLinesSetting.get());
        tmpClusterHistogramBarChart.setAnimated(false);
        System.out.println("bar chart settings + data");
        //style settings
        //return created cluster histogram
        return tmpClusterHistogramBarChart;

    }
    //Todo: add settings listeners
    private void addListenersToComponents() {
        //close cluster histogram
        this.clusterHistogramView.getCloseButton().setOnAction(event -> {
            this.clusterHistogramStage.close();
            this.clearAllGUICaches();
        });
        //apply changes to display
        //ToDo: determine what to do on apply -> "rerun clustering" button for clustering parameter change
        this.clusterHistogramView.getApplyButton().setOnAction(event -> {
            //apply
            //see histogram controller l.835
        });
        //disable apply button if displayed cluster number text field is empty
        this.clusterHistogramView.getApplyButton().disableProperty().bind(
                Bindings.isEmpty(this.clusterHistogramView.getDisplayedClustersNumberTextField().textProperty())
        );
        //ensures proper stage closure on window close request
        this.clusterHistogramStage.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, (this::closeWindowEvent));
        //adding text formatter that only accepts integers and turns the input strings into those to the two text fields
        this.clusterHistogramView.getDisplayedClustersNumberTextField().setTextFormatter(
                new TextFormatter<>(GuiUtil.getStringToIntegerConverter(),
                        this.displayedClustersNumberSetting.get(), //default value
                        GuiUtil.getPositiveIntegerFilter(false))
        );
        this.clusterHistogramView.getDisplayGridLinesCheckBox().selectedProperty()
                .addListener((ObservableValue<? extends Boolean> ov, Boolean oldVal, Boolean newVal) -> {
                    this.clusterHistogramChart.setVerticalGridLinesVisible(newVal);
                    this.clusterHistogramChart.setHorizontalGridLinesVisible(newVal);
                    //update setting
                    this.displayGridLinesSetting.set(newVal);
                });
        this.clusterHistogramView.getDisplayBarShadowsCheckBox().selectedProperty()
                .addListener((ObservableValue<? extends Boolean> ov, Boolean oldVal, Boolean newVal) -> {
                    //Todo: stackpane for bar labels -> see histogram controller l.940
                    //Todo: add stackpane in createClusterHistogram
                    //Todo: add listeners to stackpane components -> bar styling -> histogram controller l.1055
                });

    }

    /**
     * Closes the cluster histogram view (stage) and clears all GUI caches when close window event was fired.
     *
     * @param anEvent WindowEvent
     */
    private void closeWindowEvent(WindowEvent anEvent) {
        this.clusterHistogramStage.close();
        this.clearAllGUICaches();
    }
    //Todo: set all class variables null
    private void clearAllGUICaches() {
        this.clusterHistogramView = null;
        this.clusterHistogramStage = null;
        this.clusterHistogramScene = null;
    }
    //extend HistogramViewController?
}
