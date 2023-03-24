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

package de.unijena.cheminf.mortar.controller;

import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.gui.views.HistogramView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.depict.DepictionUtil;
import de.unijena.cheminf.mortar.model.util.ListUtil;

import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextFormatter;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.openscience.cdk.aromaticity.Kekulization;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller class for the HistogramView.
 *
 * @author Betuel Sevindik, Jonas Schaub
 * @version 1.0.1.0
 */
public class HistogramViewController implements IViewToolController {
    /**
     * Enum for the available bar spacing width options.
     */
    public static enum BarWidthOption {
        /**
         * Small bar width
         */
        SMALL(Message.get("HistogramView.barWidths.small")),
        /**
         * Medium bar width
         */
        MEDIUM(Message.get("HistogramView.barWidths.medium")),
        /**
         * Large bar width
         */
        LARGE(Message.get("HistogramView.barWidths.large"));
        /**
         * A name for the respective constant that is meant for display, i.e. taken from the Message file.
         */
        private final String displayName;
        /**
         * Constructor that sets the display name.
         *
         * @param aDisplayName a name for the respective constant that is meant for display, i.e. taken from the Message file.
         */
        BarWidthOption(String aDisplayName) {
            this.displayName = aDisplayName;
        }
        /**
         * Returns a name for the respective constant that is meant for display, i.e. taken from the Message file.
         *
         * @return display name of the constant
         */
        public String getDisplayName() {
            return this.displayName;
        }
    }
    /**
     * Enum for the available frequency options, i.e. which frequency of the fragments to display, the absolute frequency
     * or the molecule frequency.
     */
    public static enum FrequencyOption {
        /**
         * Display the molecule frequencies of the fragments in the histogram.
         */
        MOLECULE_FREQUENCY(Message.get("HistogramView.chooseDataComboBoxMoleculeFrequency.text")),
        /**
         * Display the absolute fragment frequencies in the histogram.
         */
        ABSOLUTE_FREQUENCY(Message.get("HistogramView.chooseDataComboBoxFragmentFrequency.text"));
        /**
         * A name for the respective constant that is meant for display, i.e. taken from the Message file.
         */
        private final String displayName;
        /**
         * Constructor that sets the display name.
         *
         * @param aDisplayName a name for the respective constant that is meant for display, i.e. taken from the Message file.
         */
        FrequencyOption(String aDisplayName) {
            this.displayName = aDisplayName;
        }
        /**
         * Returns a name for the respective constant that is meant for display, i.e. taken from the Message file.
         *
         * @return display name of the constant
         */
        public String getDisplayName() {
            return this.displayName;
        }
    }
    //<editor-fold desc="public static final class variables" defaultstate="collapsed">
    /**
     * Default value for the number of displayed fragments.
     */
    public static final int DEFAULT_NUMBER_OF_DISPLAYED_FRAGMENTS = 30;
    /**
     * Default maximum SMILES length for display on the bar labels.
     */
    public static final int DEFAULT_MAX_SMILES_LENGTH = 25;
    /**
     * Default of bar spacing width.
     */
    public static final BarWidthOption DEFAULT_BAR_WIDTH = BarWidthOption.LARGE;
    /**
     * Default fragment frequency to display.
     */
    public static final FrequencyOption DEFAULT_DISPLAY_FREQUENCY = FrequencyOption.ABSOLUTE_FREQUENCY;
    /**
     * Default for whether bar labels (the fragment frequencies) should be displayed.
     */
    public static final boolean DEFAULT_DISPLAY_BAR_LABELS_SETTING = true;
    /**
     * Default for whether bar shadows should be displayed in the histogram.
     */
    public static final boolean DEFAULT_DISPLAY_BAR_SHADOWS_SETTING = true;
    /**
     * Default for whether to display vertical and horizontal grid lines in the histogram.
     */
    public static final boolean DEFAULT_DISPLAY_GRID_LINES_SETTING = false;
    /**
     * Default for whether the fragment SMILES codes should be displayed on the y-axis.
     */
    public static final boolean DEFAULT_DISPLAY_SMILES_SETTING = true;
    /**
     * View tool name for display in the GUI.
     */
    public static final String VIEW_TOOL_NAME_FOR_DISPLAY = Message.get("MainView.menuBar.viewsMenu.HistogramMenuItem.text");
    /**
     * Color for the histogram bars.
     */
    public static final String HISTOGRAM_BARS_COLOR_HEX_VALUE = "#1E90FF"; //dodger blue
    /**
     * Color for the histogram bars when the mouse hovers over them.
     */
    public static final String HISTOGRAM_BARS_SELECTED_COLOR_HEX_VALUE = "#00008b"; //dark blue
    //</editor-fold>
    //
    //<editor-fold desc="private static final class variables" defaultstate="collapsed">
    /**
     * Logger of this class
     */
    private static final Logger LOGGER = Logger.getLogger(HistogramViewController.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="private final class variables" defaultstate="collapsed">
    /**
     * Setting for number of displayed fragments.
     */
    private final SimpleIntegerProperty displayedFragmentsNumberSetting;
    /**
     * Setting for width of spaces between the histogram bars.
     */
    private final SimpleEnumConstantNameProperty barWidthSetting;
    /**
     * Setting for which fragment frequency to display.
     */
    private final SimpleEnumConstantNameProperty displayFrequencySetting;
    /**
     * Setting for maximum SMILES length to display.
     */
    private final SimpleIntegerProperty maximumSMILESLengthSetting;
    /**
     * Setting for whether to display bar labels, i.e. the frequencies of the respective fragment.
     */
    private final SimpleBooleanProperty displayBarLabelsSetting;
    /**
     * Setting for whether to display bar shadows.
     */
    private final SimpleBooleanProperty displayBarShadowsSetting;
    /**
     * Setting for whether to display vertical and horizontal grid lines in the histogram.
     */
    private final SimpleBooleanProperty displayGridLinesSetting;
    /**
     * Setting for whether to display SMILES codes as labels on the y-axis.
     */
    private final SimpleBooleanProperty displaySMILESSetting;
    /**
     * All settings of this view tool, encapsulated in JavaFX properties for binding in GUI and persistence.
     */
    private final List<Property> settings;
    //</editor-fold>
    //
    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * Stage of the main application view
     */
    private Stage mainStage;
    /**
     * HistogramView to display
     */
    private HistogramView histogramView;
    /**
     * Stage of the HistogramView
     */
    private Stage histogramStage;
    /**
     * List of fragments to display.
     */
    private List<FragmentDataModel> fragmentListCopy;
    /**
     * Width of molecule depictions displayed when the cursor hovers over a bar. Changes when histogram is resized.
     */
    private double imageWidth;
    /**
     * Height of molecule depictions displayed when the cursor hovers over a bar. Changes when histogram is resized.
     */
    private double imageHeight;
    /**
     * Zoom factor of molecule depictions displayed when the cursor hovers over a bar. Changes when histogram is resized.
     */
    private double imageZoomFactor;
    /**
     * Y-axis of the histogram
     */
    private CategoryAxis categoryAxis;
    /**
     * Histogram chart
     */
    private BarChart histogramChart;
    /**
     * Atom container to depict the respective structure when the cursor hovers over a bar
     */
    private IAtomContainer atomContainerForDisplayCache;
    //</editor-fold>
    //

    /**
     * Constructor, initialises all settings with their default values. Does *not* open the view.
     */
    public HistogramViewController()  {
        this.settings = new ArrayList<Property>(8);
        this.displayedFragmentsNumberSetting = new SimpleIntegerProperty(this,
                //the name could be displayed but is not used for that currently
                Message.get("HistogramView.displayedFragmentsNumberSetting.name"),
                HistogramViewController.DEFAULT_NUMBER_OF_DISPLAYED_FRAGMENTS) {
            @Override
            public void set(int newValue) throws NullPointerException, IllegalArgumentException {
                super.set(newValue);
                //value transferred to GUI in openHistogramView()
                //value updated in addListenersToHistogramView(), listener of apply-button
            }
        };
        this.settings.add(this.displayedFragmentsNumberSetting);
        this.barWidthSetting = new SimpleEnumConstantNameProperty(this,
                //the name could be displayed but is not used for that currently
                Message.get("HistogramView.barWidthSetting.name"),
                HistogramViewController.DEFAULT_BAR_WIDTH.name(),
                HistogramViewController.BarWidthOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                super.set(newValue);
                //value transferred to GUI in openHistogramView()
                //value updated in addListenersToHistogramView(), listener of apply-button
            }
        };
        this.settings.add(this.barWidthSetting);
        this.displayFrequencySetting = new SimpleEnumConstantNameProperty(this,
                //the name could be displayed but is not used for that currently
                Message.get("HistogramView.displayFrequencySetting.name"),
                HistogramViewController.DEFAULT_DISPLAY_FREQUENCY.name(),
                HistogramViewController.BarWidthOption.class) {
            @Override
            public void set(String newValue) throws NullPointerException, IllegalArgumentException {
                super.set(newValue);
                //value transferred to GUI in openHistogramView()
                //value updated in addListenersToHistogramView(), listener of apply-button
            }
        };
        this.settings.add(this.displayFrequencySetting);
        this.maximumSMILESLengthSetting = new SimpleIntegerProperty(this,
                //the name could be displayed but is not used for that currently
                Message.get("HistogramView.maximumSMILESLengthSetting.name"),
                HistogramViewController.DEFAULT_MAX_SMILES_LENGTH) {
            @Override
            public void set(int newValue) throws NullPointerException, IllegalArgumentException {
                super.set(newValue);
                //value transferred to GUI in openHistogramView()
                //value updated in addListenersToHistogramView(), listener of apply-button
            }
        };
        this.settings.add(this.maximumSMILESLengthSetting);
        this.displayBarLabelsSetting = new SimpleBooleanProperty(this,
                //the name could be displayed but is not used for that currently
                Message.get("HistogramView.displayBarLabelsSetting.name"),
                HistogramViewController.DEFAULT_DISPLAY_BAR_LABELS_SETTING) {
            @Override
            public void set(boolean newValue) {
                super.set(newValue);
                //value transferred to GUI in openHistogramView()
                //also, bidirectionally bound in openHistogramView() to respective check box in view
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
                //also, bidirectionally bound in openHistogramView() to respective check box in view
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
                //also, bidirectionally bound in openHistogramView() to respective check box in view
                //value also used in createHistogram() and addListenersToHistogramView()
            }
        };
        this.settings.add(this.displayGridLinesSetting);
        this.displaySMILESSetting = new SimpleBooleanProperty(this,
                //the name could be displayed but is not used for that currently
                Message.get("HistogramView.displaySMILESSetting.name"),
                HistogramViewController.DEFAULT_DISPLAY_SMILES_SETTING) {
            @Override
            public void set(boolean newValue) {
                super.set(newValue);
                //value transferred to GUI in openHistogramView()
                //also, bidirectionally bound in openHistogramView() to respective check box in view
                //value also used in addListenersToHistogramView()
            }
        };
        this.settings.add(this.displaySMILESSetting);
    }
    //
    //<editor-fold desc="public methods" defaultstate="collapsed">

    @Override
    public List<Property> settingsProperties() {
        //TODO add note on how and when the settings in the view are actually updated accordingly?
        return this.settings;
    }
    //
    @Override
    public String getViewToolNameForDisplay() {
        return HistogramViewController.VIEW_TOOL_NAME_FOR_DISPLAY;
    }
    //
    @Override
    public void restoreDefaultSettings() {
        //TODO: adjust settings in other places, too? Add note that the view will only be affected when it is opened again?
        this.barWidthSetting.set(HistogramViewController.DEFAULT_BAR_WIDTH.name());
        this.displayFrequencySetting.set(HistogramViewController.DEFAULT_DISPLAY_FREQUENCY.name());
        this.maximumSMILESLengthSetting.set(HistogramViewController.DEFAULT_MAX_SMILES_LENGTH);
        this.displayedFragmentsNumberSetting.set((HistogramViewController.DEFAULT_NUMBER_OF_DISPLAYED_FRAGMENTS));
        this.displayBarLabelsSetting.set(HistogramViewController.DEFAULT_DISPLAY_BAR_LABELS_SETTING);
        this.displayBarShadowsSetting.set(HistogramViewController.DEFAULT_DISPLAY_BAR_SHADOWS_SETTING);
        this.displayGridLinesSetting.set(HistogramViewController.DEFAULT_DISPLAY_GRID_LINES_SETTING);
        this.displaySMILESSetting.set(HistogramViewController.DEFAULT_DISPLAY_SMILES_SETTING);
    }
    //
    @Override
    public boolean canBeUsedOnTab(TabNames aTabNameEnumConstant) {
        return switch (aTabNameEnumConstant) {
            //TODO capitalise enum constant names? @Felix
            case Fragments, Itemization -> true;
            default -> false;
        };
    }
    //TODO: harmonise this method in the IViewToolController interface?
    /**
     * Initialises stage and view and opens view in the initialised stage.
     *
     * @param aMainStage Stage of the MainView
     * @param aFragmentDataModelList ObservableList that holds FragmentDataModel objects for visualisation in histogram
     */
    public void openHistogramView(Stage aMainStage, List<FragmentDataModel> aFragmentDataModelList)  {
        //TODO checks!
        //these need to be reset to default because the histogram view is re-initialised with default size
        //TODO: Maybe move constant to this class or view class?
        this.imageWidth = GuiDefinitions.STRUCTURE_DEPICTION_IMAGE_INITIAL_WIDTH;
        this.imageHeight = GuiDefinitions.STRUCTURE_DEPICTION_IMAGE_INITIAL_HEIGHT;
        this.imageZoomFactor = GuiDefinitions.STRUCTURE_DEPICTION_IMAGE_INITIAL_ZOOM_FACTOR;
        //---initialisation of stage and view---
        this.mainStage = aMainStage;
        this.fragmentListCopy = new ArrayList<>(aFragmentDataModelList);
        this.histogramStage = new Stage();
        this.histogramStage.initModality(Modality.WINDOW_MODAL);
        this.histogramStage.initOwner(this.mainStage);
        this.histogramStage.setTitle(Message.get("HistogramView.title"));
        this.histogramStage.setMinHeight(GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.histogramStage.setMinWidth(GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE);
        InputStream tmpImageInputStream = HistogramViewController.class.getResourceAsStream("/de/unijena/cheminf/mortar/images/Mortar_Logo_Icon1.png");
        if (!Objects.isNull(tmpImageInputStream)) {
            this.histogramStage.getIcons().add(new Image(tmpImageInputStream));
        }
        this.histogramView = new HistogramView(this.fragmentListCopy.size());
        //---set setting values in view---
        //TODO: no binding because setting is actually only updated when the apply button is clicked?
        this.histogramView.getMaximumSMILESLengthTextField().setText(Integer.toString(this.maximumSMILESLengthSetting.get()));
        String tmpCurrentlySetBarWidthOptionDisplayName = null;
        for (BarWidthOption tmpBarWidthOption : BarWidthOption.values()) {
            if (tmpBarWidthOption.name().equals(this.barWidthSetting.get())) {
                tmpCurrentlySetBarWidthOptionDisplayName = tmpBarWidthOption.getDisplayName();
            }
        }
        if (Objects.isNull(tmpCurrentlySetBarWidthOptionDisplayName)) {
            this.barWidthSetting.set(HistogramViewController.DEFAULT_BAR_WIDTH.name());
            tmpCurrentlySetBarWidthOptionDisplayName = HistogramViewController.DEFAULT_BAR_WIDTH.getDisplayName();
        }
        this.histogramView.getBarWidthsComboBox().setValue(tmpCurrentlySetBarWidthOptionDisplayName);
        String tmpCurrentlySetFrequencyOptionDisplayName = null;
        for (FrequencyOption tmpFrequencyOption : FrequencyOption.values()) {
            if (tmpFrequencyOption.name().equals(this.displayFrequencySetting.get())) {
                tmpCurrentlySetFrequencyOptionDisplayName = tmpFrequencyOption.getDisplayName();
            }
        }
        if (Objects.isNull(tmpCurrentlySetFrequencyOptionDisplayName)) {
            this.displayFrequencySetting.set(HistogramViewController.DEFAULT_DISPLAY_FREQUENCY.name());
            tmpCurrentlySetFrequencyOptionDisplayName = HistogramViewController.DEFAULT_DISPLAY_FREQUENCY.getDisplayName();
        }
        this.histogramView.getFrequencyComboBox().setValue(tmpCurrentlySetFrequencyOptionDisplayName);
        if (this.fragmentListCopy.size() < this.displayedFragmentsNumberSetting.get()) {
            this.displayedFragmentsNumberSetting.set(this.fragmentListCopy.size());
        }
        this.histogramView.getDisplayedFragmentsNumberTextField().setText(Integer.toString(this.displayedFragmentsNumberSetting.get()));
        //these settings are updated immediately; therefore, bindings are set
        this.histogramView.getDisplayBarLabelsCheckBox().setSelected(this.displayBarLabelsSetting.get());
        this.histogramView.getDisplayBarLabelsCheckBox().selectedProperty().bindBidirectional(this.displayBarLabelsSetting);
        this.histogramView.getDisplayBarShadowsCheckBox().setSelected(this.displayBarShadowsSetting.get());
        this.histogramView.getDisplayBarShadowsCheckBox().selectedProperty().bindBidirectional(this.displayBarShadowsSetting);
        this.histogramView.getDisplayGridLinesCheckBox().setSelected(this.displayGridLinesSetting.get());
        this.histogramView.getDisplayGridLinesCheckBox().selectedProperty().bindBidirectional(this.displayGridLinesSetting);
        this.histogramView.getDisplaySmilesOnYAxisCheckBox().setSelected(this.displaySMILESSetting.get());
        this.histogramView.getDisplaySmilesOnYAxisCheckBox().selectedProperty().bindBidirectional(this.displaySMILESSetting);
        Double[] tmpHistogramHeightFactorAndCategoryGap = this.calculateBarSpacing(
                this.displayedFragmentsNumberSetting.get(),
                this.getBarWidthOptionEnumConstantFromDisplayName((String)this.histogramView.getBarWidthsComboBox().getValue()));
        this.histogramChart = this.createHistogram(
                this.displayedFragmentsNumberSetting.get(),
                this.histogramView,
                this.maximumSMILESLengthSetting.get(),
                this.histogramView.getDisplayBarLabelsCheckBox(),
                this.histogramView.getDisplayBarShadowsCheckBox(),
                tmpHistogramHeightFactorAndCategoryGap[0]);
        this.histogramChart.setCategoryGap(tmpHistogramHeightFactorAndCategoryGap[1]);
        this.addListenersToHistogramView();
        Scene tmpHistogramScene = new Scene(
                this.histogramView,
                GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE,
                GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.histogramStage.setScene(tmpHistogramScene);
        //TODO: move this to addListenersToHistogramView()?
        tmpHistogramScene.widthProperty().addListener((observable, oldValue, newValue) -> {
            double tmpWidthChange = ((tmpHistogramScene.getWidth() - GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE) / GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE) * 100;
            double tmpImageWidthChange = (GuiDefinitions.STRUCTURE_DEPICTION_IMAGE_INITIAL_WIDTH / 100) * tmpWidthChange;
            this.imageWidth = GuiDefinitions.STRUCTURE_DEPICTION_IMAGE_INITIAL_WIDTH +tmpImageWidthChange;
            this.imageHeight = this.imageWidth - 100;
            this.imageZoomFactor = (GuiDefinitions.STRUCTURE_DEPICTION_IMAGE_INITIAL_ZOOM_FACTOR / GuiDefinitions.STRUCTURE_DEPICTION_IMAGE_INITIAL_WIDTH) * this.imageWidth;
        });
        tmpHistogramScene.heightProperty().addListener((observable, oldValue, newValue) -> {
            double tmpHeightChange = ((tmpHistogramScene.getHeight() - GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE) / GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE) * 100;
            double tmpImageHeightChange = (GuiDefinitions.STRUCTURE_DEPICTION_IMAGE_INITIAL_HEIGHT / 100) * tmpHeightChange;
            if (tmpHistogramScene.getWidth() == GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE) {
                this.imageHeight = GuiDefinitions.STRUCTURE_DEPICTION_IMAGE_INITIAL_HEIGHT + tmpImageHeightChange;
            } else {
                double tmpHeight = this.imageWidth - 100;
                double tmpIntermediateImageHeight = GuiDefinitions.STRUCTURE_DEPICTION_IMAGE_INITIAL_HEIGHT + tmpImageHeightChange;
                double tmpImageHeight = tmpHeight - tmpIntermediateImageHeight;
                this.imageHeight = tmpIntermediateImageHeight + tmpImageHeight;
            }
            this.imageWidth = 100 + this.imageHeight;
            this.imageZoomFactor = (GuiDefinitions.STRUCTURE_DEPICTION_IMAGE_INITIAL_ZOOM_FACTOR / GuiDefinitions.STRUCTURE_DEPICTION_IMAGE_INITIAL_WIDTH) * this.imageWidth;
        });
        this.histogramStage.show();
    }
    //</editor-fold>
    //
    //<editor-fold desc="private methods" defaultstate="collapsed">
    /**
     * Create a configurable histogram, called once when the view is shown and again whenever the apply button is used.
     * All data is stored in corresponding lists (the x-axis data are either fragment or molecule frequencies).
     * Depending on how aFragmentNumber is chosen, these lists/data are adjusted.
     * After they have been run through, they are displayed accordingly.
     * The x-axis of the histogram is divided into readable steps depending on the maximum frequency (automatic adjustment of the
     * x-axis ticks)
     *
     * @param aFragmentNumber sets the number of fragments that are displayed
     * @param aHistogramView to access the view elements
     * @param aSmilesLength sets the maximum SMILES length
     * //TODO add missing params and change them to bool? Difficult because there are bindings happening further down
     * @param aHistogramDefaultSize a double value that determines the height of the histogram; the height of the
     *                              histogram increases with the increase in fragments that are displayed.
     * @throws IllegalArgumentException TODO
     * @return a BarChart to display in the view
     */
    private BarChart createHistogram(int aFragmentNumber,
                                     HistogramView aHistogramView,
                                     int aSmilesLength,
                                     CheckBox aBarLabelCheckBox,
                                     CheckBox aStylingCheckBox,
                                     double aHistogramDefaultSize) throws IllegalArgumentException {
        if (aFragmentNumber >  this.fragmentListCopy.size())
            throw new IllegalArgumentException("Given number of fragments to display is bigger than number of available fragments.");
        //TODO more checks! Yeah? It is a private method
        this.categoryAxis = new CategoryAxis();
        this.categoryAxis.setTickLabelFill(Color.BLACK);
        this.categoryAxis.setTickLength(GuiDefinitions.HISTOGRAM_TICK_LABEL_LENGTH);
        this.categoryAxis.setTickLabelGap(GuiDefinitions.HISTOGRAM_TICK_LABEL_GAP);
        this.categoryAxis.setLabel(Message.get("HistogramViewController.YAxisLabel.text"));
        //TODO: make number axis a class field, too?
        NumberAxis tmpNumberAxis = new NumberAxis();
        tmpNumberAxis.setSide(Side.TOP);
        tmpNumberAxis.setAutoRanging(false);
        tmpNumberAxis.setMinorTickCount(1);
        tmpNumberAxis.setForceZeroInRange(true);
        tmpNumberAxis.setTickLabelFill(Color.BLACK);
        tmpNumberAxis.setLabel(Message.get("HistogramViewController.XAxisLabel.text"));
        BarChart tmpHistogramBarChart = new BarChart(tmpNumberAxis, this.categoryAxis);
        tmpHistogramBarChart.setCategoryGap(0);
        tmpHistogramBarChart.setBarGap(0);
        ScrollPane tmpScrollPane = aHistogramView.getHistogramScrollPane();
        tmpScrollPane.setContent(tmpHistogramBarChart);
        //---create data for histogram, i.e. categories ((abbr.) SMILES) and values (frequencies)
        String tmpNewSmiles;
        ArrayList<String> tmpSmilesList = new ArrayList<>();
        ArrayList<Integer> tmpFrequencyList = new ArrayList<>();
        int tmpIterator = this.fragmentListCopy.size();
        ArrayList<String> tmpFullSmilesLength = new ArrayList<>();
        //if false, molecule frequency is used for sorting
        boolean tmpSortByFragmentFrequency = this.displayFrequencySetting.get().equals(FrequencyOption.ABSOLUTE_FREQUENCY.name());
        String tmpSortProperty = (tmpSortByFragmentFrequency ? "absoluteFrequency" : "moleculeFrequency");
        //TODO: rework method!
        ListUtil.sortGivenFragmentListByPropertyAndSortType(this.fragmentListCopy, tmpSortProperty, "ASCENDING");
        for (FragmentDataModel tmpFragmentDataModel : this.fragmentListCopy) {
            if (tmpFragmentDataModel.getUniqueSmiles().length() > aSmilesLength) {
                tmpNewSmiles = Message.get("HistogramView.smilesTooLong") + " (" + tmpIterator + ")";
            } else {
                tmpNewSmiles = tmpFragmentDataModel.getUniqueSmiles();
            }
            tmpSmilesList.add(tmpNewSmiles);
            tmpIterator--;
            tmpFullSmilesLength.add(tmpFragmentDataModel.getUniqueSmiles());
            tmpFrequencyList.add(tmpSortByFragmentFrequency ? tmpFragmentDataModel.getAbsoluteFrequency() : tmpFragmentDataModel.getMoleculeFrequency());
        }
        //---calculate tick size and histogram size---
        double tmpMaxFrequency = Collections.max(tmpFrequencyList);
        // make readable x-axis
        double tmpXAxisTicks = 5.0 / 100.0 * tmpMaxFrequency; // magic number
        double tmpXAxisExtension = 15.0 / 100.0 * tmpMaxFrequency; // magic number
        int tmpIntTmpXAxisTick = (int) Math.round(tmpXAxisTicks);
        int tmpIntXAxisExtension = (int) Math.round(tmpXAxisExtension);
        if (tmpIntTmpXAxisTick == 0 || tmpIntXAxisExtension == 0) {
           tmpNumberAxis.setTickUnit(1);
           tmpNumberAxis.setUpperBound(tmpMaxFrequency + 1);
        } else {
            int tmpNewXAxisTick = 0;
            if (tmpIntTmpXAxisTick >= 10) {
                tmpNewXAxisTick = tmpIntTmpXAxisTick;
                String tmpTickLength = String.valueOf(tmpNewXAxisTick);
                String tmpFirstValue = String.valueOf(tmpTickLength.charAt(0));
                int tmpFirstIntValue = Integer.parseInt(tmpFirstValue);
                if (tmpFirstIntValue > 5) {
                    tmpNewXAxisTick = (int) Math.pow(10, tmpTickLength.length());
                } else {
                    int tmpDigit = tmpTickLength.length() - 1;
                    int tmpCheckModulo = (int) Math.pow(10, tmpDigit);
                    if (tmpNewXAxisTick % tmpCheckModulo != 0) {
                        //TODO: there must be a better way to do this
                        do {
                            tmpNewXAxisTick++;
                        } while (tmpNewXAxisTick % (tmpCheckModulo) != 0);
                    }
                }
                tmpNumberAxis.setTickUnit(tmpNewXAxisTick);
                tmpNumberAxis.setUpperBound(this.calculateXAxisUpperBoundWithSpaceForLabels((int) tmpMaxFrequency, tmpNewXAxisTick));
            } else {
                tmpNumberAxis.setTickUnit(tmpIntTmpXAxisTick);
                tmpNumberAxis.setUpperBound(this.calculateXAxisUpperBoundWithSpaceForLabels((int) tmpMaxFrequency, tmpIntTmpXAxisTick));
            }
        }
        //---add function (structure display at hover) to bars---
        List<String> tmpSublistSmiles;
        List<Integer> tmpSublistFrequency;
        List<String> tmpSmilesToDepict;
        tmpSublistSmiles = tmpSmilesList.subList(tmpSmilesList.size() - aFragmentNumber, tmpSmilesList.size());
        tmpSublistFrequency = tmpFrequencyList.subList(tmpFrequencyList.size() - aFragmentNumber, tmpFrequencyList.size());
        tmpSmilesToDepict = tmpFullSmilesLength.subList(tmpFullSmilesLength.size()- aFragmentNumber, tmpFullSmilesLength.size());
        XYChart.Series tmpSeries = new XYChart.Series();
        //TODO: can we not just make sure here that the three lists are of the same size and than iterate through them with an index?
        for (Iterator tmpStringIterator = tmpSublistSmiles.iterator(), tmpIntegerIterator = tmpSublistFrequency.iterator(),
             tmpSmilesIterator = tmpSmilesToDepict.iterator(); tmpStringIterator.hasNext() && tmpIntegerIterator.hasNext() && tmpSmilesIterator.hasNext();) {
            Integer tmpCurrentFrequency = (Integer) tmpIntegerIterator.next();
            String tmpCurrentSmiles = (String) tmpStringIterator.next();
            String tmpSmiles = (String) tmpSmilesIterator.next();
            XYChart.Data<Number, String> tmpStringNumberData = new XYChart.Data(tmpCurrentFrequency, tmpCurrentSmiles);
            StackPane tmpHistogramBarStackPane = this.createStackPaneWithContextMenuAndStructureDisplayForBar(
                    aHistogramView.getStructureDisplayImageView(),
                    tmpSmiles);
            tmpHistogramBarStackPane.setStyle("-fx-bar-fill: " + HistogramViewController.HISTOGRAM_BARS_COLOR_HEX_VALUE);
            this.addFrequencyBarLabelToBarAndAddListenersToBarCheckBoxes(
                    aBarLabelCheckBox,
                    aStylingCheckBox,
                    tmpHistogramBarStackPane,
                    tmpCurrentFrequency);
            tmpStringNumberData.setNode(tmpHistogramBarStackPane);
            tmpSeries.getData().add(tmpStringNumberData);
        }
        double tmpHistogramSize = aHistogramDefaultSize * tmpSublistFrequency.size();
        tmpHistogramBarChart.setPrefHeight(tmpHistogramSize);
        tmpHistogramBarChart.setMinHeight(tmpHistogramSize);
        tmpHistogramBarChart.getData().add(tmpSeries);
        tmpHistogramBarChart.setLegendVisible(false);
        tmpHistogramBarChart.layout();
        tmpHistogramBarChart.setHorizontalGridLinesVisible(this.displayGridLinesSetting.get());
        tmpHistogramBarChart.setVerticalGridLinesVisible(this.displayGridLinesSetting.get());
        tmpHistogramBarChart.setAnimated(false);
        return tmpHistogramBarChart;
    }
    //
    //TODO introduce param to make data flow explicit?
    /**
     * Add listeners
     */
    private void addListenersToHistogramView() {
        //close histogram stage with close button
        this.histogramView.getCloseButton().setOnAction(event -> {
            this.histogramStage.close();
            //TODO: discard some cached values here?
        });
        //add text formatter that only accepts integers and turns the input strings into those to the two text fields
        this.histogramView.getDisplayedFragmentsNumberTextField().setTextFormatter(
                new TextFormatter<>(GuiUtil.getStringToIntegerConverter(),
                        this.displayedFragmentsNumberSetting.get(), //default value
                        GuiUtil.getPositiveIntegerWithoutZeroFilter())
        );
        this.histogramView.getMaximumSMILESLengthTextField().setTextFormatter(
                new TextFormatter<>(GuiUtil.getStringToIntegerConverter(),
                        this.maximumSMILESLengthSetting.get(), //default value
                        GuiUtil.getPositiveIntegerWithoutZeroFilter())
        );
        //disable apply button if the text fields are empty
        // TODO is the button also disabled if only one field is empty?
        this.histogramView.getApplyButton().disableProperty().bind(
                Bindings.isEmpty(this.histogramView.getDisplayedFragmentsNumberTextField().textProperty()).
                            and(Bindings.isEmpty(this.histogramView.getMaximumSMILESLengthTextField().textProperty()))
        );
        //apply button, update histogram
        this.histogramView.getApplyButton().setOnAction(event -> {
            //int tmpMaxSmilesLengthInField;
            //TODO: what if both are empty? else if is not called if the if statement is true, right?
            if (this.histogramView.getMaximumSMILESLengthTextFieldContent().isEmpty()) {
                this.displayedFragmentsNumberSetting.set(Integer.parseInt(this.histogramView.getDisplayedFragmentsNumberTextFieldContent()));
                if (this.displayedFragmentsNumberSetting.get() > this.fragmentListCopy.size()) {
                    GuiUtil.guiMessageAlert(Alert.AlertType.WARNING, Message.get("HistogramViewController.HistogramGeneralRefreshWarning.Title"),
                            Message.get("HistogramViewController.HistogramFrequencyRefreshWarning.Header"),
                            Message.get("HistogramViewController.HistogramFrequencyRefreshWarning.Content"));
                    return;
                }
                this.maximumSMILESLengthSetting.set(GuiDefinitions.HISTOGRAM_DEFAULT_SMILES_LENGTH);
                this.histogramView.getMaximumSMILESLengthTextField().setText(String.valueOf(this.maximumSMILESLengthSetting.get()));
            } else if (this.histogramView.getDisplayedFragmentsNumberTextFieldContent().isEmpty()) {
                this.maximumSMILESLengthSetting.set(Integer.parseInt(this.histogramView.getMaximumSMILESLengthTextFieldContent()));
                if (this.fragmentListCopy.size() >= HistogramViewController.DEFAULT_NUMBER_OF_DISPLAYED_FRAGMENTS) {
                    this.displayedFragmentsNumberSetting.set(HistogramViewController.DEFAULT_NUMBER_OF_DISPLAYED_FRAGMENTS);
                } else {
                    this.displayedFragmentsNumberSetting.set(this.fragmentListCopy.size());
                }
                this.histogramView.getDisplayedFragmentsNumberTextField().setText(String.valueOf(this.displayedFragmentsNumberSetting.get()));
            } else {
                this.displayedFragmentsNumberSetting.set(Integer.parseInt(this.histogramView.getDisplayedFragmentsNumberTextFieldContent()));
                this.maximumSMILESLengthSetting.set(Integer.parseInt(this.histogramView.getMaximumSMILESLengthTextFieldContent()));
                if (this.displayedFragmentsNumberSetting.get() > this.fragmentListCopy.size()) {
                    GuiUtil.guiMessageAlert(Alert.AlertType.WARNING, Message.get("HistogramViewController.HistogramGeneralRefreshWarning.Title"),
                            Message.get("HistogramViewController.HistogramFrequencyRefreshWarning.Header"),
                            Message.get("HistogramViewController.HistogramFrequencyRefreshWarning.Content"));
                    return;
                }
                this.histogramView.getDisplayedFragmentsNumberTextField().setText(String.valueOf(this.displayedFragmentsNumberSetting.get()));
                this.histogramView.getMaximumSMILESLengthTextField().setText(String.valueOf(this.maximumSMILESLengthSetting.get()));
            }
            this.barWidthSetting.set(this.getBarWidthOptionEnumConstantFromDisplayName((String)this.histogramView.getBarWidthsComboBox().getValue()).name());
            Double[] tmpHistogramSizeGap = this.calculateBarSpacing(
                    this.displayedFragmentsNumberSetting.get(),
                    this.getBarWidthOptionEnumConstantFromDisplayName((String)this.histogramView.getBarWidthsComboBox().getValue()));
            this.displayFrequencySetting.set(this.getFrequencyOptionEnumConstantFromDisplayName((String)this.histogramView.getFrequencyComboBox().getValue()).name());
            this.histogramChart = this.createHistogram(
                    this.displayedFragmentsNumberSetting.get(),
                    this.histogramView,
                    this.maximumSMILESLengthSetting.get(),
                    this.histogramView.getDisplayBarLabelsCheckBox(),
                    this.histogramView.getDisplayBarShadowsCheckBox(),
                    tmpHistogramSizeGap[0]);
            this.histogramChart.setCategoryGap(tmpHistogramSizeGap[1]);
            if(this.displayGridLinesSetting.get()) {
                this.histogramChart.setVerticalGridLinesVisible(true);
                this.histogramChart.setHorizontalGridLinesVisible(true);
            }
            if (this.displaySMILESSetting.get()) {
                this.categoryAxis.setTickMarkVisible(true);
                this.categoryAxis.setTickLabelsVisible(true);
            }
        });
        this.histogramView.getDisplayGridLinesCheckBox().selectedProperty()
                .addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            //setting in controller is updated via bidirectional binding
            if (this.histogramView.getDisplayGridLinesCheckBox().isSelected()) {
                this.histogramChart.setVerticalGridLinesVisible(true);
                this.histogramChart.setHorizontalGridLinesVisible(true);
            } else {
                this.histogramChart.setVerticalGridLinesVisible(false);
                this.histogramChart.setHorizontalGridLinesVisible(false);
            }
        });
        this.histogramView.getDisplaySmilesOnYAxisCheckBox().selectedProperty()
                .addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            //setting in controller is updated via bidirectional binding
            if (this.histogramView.getDisplaySmilesOnYAxisCheckBox().isSelected()) {
                this.categoryAxis.setTickMarkVisible(false);
                this.categoryAxis.setTickLabelsVisible(false);
            } else {
                this.categoryAxis.setTickMarkVisible(true);
                this.categoryAxis.setTickLabelsVisible(true);
            }
        });
    }
    //
    /**
     * Make the histogram hoverable and create the structure images that are displayed in the histogram when the cursor
     * hovers over a bar. Basically an addListener-method.
     * For this purpose, add a StackPane to each bar as a node, and use the StackPanes to call the corresponding listener.
     * Hovering over the bars displays the corresponding structures.
     * In addition, by right-clicking on the bars, the corresponding structures (1500x1000) can be copied to SMILES
     * anyway (also about adding labels). //TODO explain this sentence
     *
     * @param anImageView central image view of the histogram to display the structure on
     * @param aSmiles SMILES code that is associated with the respective bar to depict the structures and copy them
     * @return a StackPane that is used as a node in the histogram
     */
    private StackPane createStackPaneWithContextMenuAndStructureDisplayForBar(ImageView anImageView, String aSmiles) {
        StackPane tmpNodePane = new StackPane();
        tmpNodePane.setAlignment(Pos.CENTER_RIGHT);
        MenuItem tmpCopySmilesMenuItem = new MenuItem(Message.get("HistogramViewController.MenuItemSmiles.text"));
        MenuItem tmpCopyStructureMenuItem = new MenuItem(Message.get("HistogramViewController.MenuItemStructure.text"));
        //TODO add try-catch because Image constructor throws exception if url is invalid?
        tmpCopySmilesMenuItem.setGraphic(new ImageView(new Image("de/unijena/cheminf/mortar/images/copy_icon_16x16.png")));
        tmpCopyStructureMenuItem.setGraphic(new ImageView(new Image("de/unijena/cheminf/mortar/images/copy_icon_16x16.png")));
        ContextMenu tmpContextMenu = new ContextMenu();
        tmpContextMenu.getItems().addAll(tmpCopySmilesMenuItem, tmpCopyStructureMenuItem);
        Label tmpContextMenuLabel = new Label();
        tmpContextMenuLabel.setPrefWidth(GuiDefinitions.HISTOGRAM_CONTEXTMENU_LABEL);
        tmpContextMenuLabel.setMaxWidth(GuiDefinitions.HISTOGRAM_CONTEXTMENU_LABEL);
        tmpContextMenuLabel.setMinWidth(GuiDefinitions.HISTOGRAM_CONTEXTMENU_LABEL);
        tmpContextMenuLabel.setTranslateX(20);
        tmpContextMenuLabel.setContextMenu(tmpContextMenu);
        tmpNodePane.getChildren().addAll(tmpContextMenuLabel);
        tmpNodePane.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            //---context menu listener and structure display---
            tmpNodePane.setStyle("-fx-bar-fill: " + HistogramViewController.HISTOGRAM_BARS_SELECTED_COLOR_HEX_VALUE);
            // two different ways to call context menu needed because bar might be too small
            if (tmpNodePane.getWidth() >= 10) {
                tmpContextMenuLabel.setPrefWidth(tmpNodePane.getWidth());
                tmpContextMenuLabel.setMaxWidth(tmpNodePane.getWidth());
                tmpContextMenuLabel.setMinWidth(tmpNodePane.getWidth());
                tmpContextMenuLabel.setTranslateX(0);
                tmpContextMenuLabel.addEventHandler(MouseEvent.MOUSE_CLICKED,event1 -> {
                    if (MouseButton.SECONDARY.equals(event1.getButton())) {
                        tmpContextMenuLabel.setStyle("-fx-bar-fill: " + HistogramViewController.HISTOGRAM_BARS_SELECTED_COLOR_HEX_VALUE);
                        tmpContextMenu.show(tmpContextMenuLabel, tmpNodePane.getWidth() / 2, tmpNodePane.getHeight());
                    }
                });
            } else {
                tmpContextMenuLabel.setPrefWidth(GuiDefinitions.HISTOGRAM_CONTEXTMENU_LABEL_NEW);
                tmpContextMenuLabel.setMaxWidth(GuiDefinitions.HISTOGRAM_CONTEXTMENU_LABEL_NEW);
                tmpContextMenuLabel.setMinWidth(GuiDefinitions.HISTOGRAM_CONTEXTMENU_LABEL_NEW);
                tmpContextMenuLabel.setTranslateX(20);
                tmpNodePane.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, event2 -> {
                    tmpContextMenu.show(tmpNodePane, event2.getScreenX(), event2.getScreenY());
                });
            }
            // TODO check MoleculeDataModel getAtomContainer (?)
            //TODO: Can we replace that with a central SMILES parsing method in ChemUtil?
            SmilesParser tmpSmiPar = new SmilesParser(SilentChemObjectBuilder.getInstance());
            this.atomContainerForDisplayCache = null;
            try {
                tmpSmiPar.kekulise(false);
                this.atomContainerForDisplayCache = tmpSmiPar.parseSmiles(aSmiles);
                AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(this.atomContainerForDisplayCache);
                Kekulization.kekulize(this.atomContainerForDisplayCache);
            } catch (CDKException anException) {
                HistogramViewController.LOGGER.log(Level.SEVERE, anException.toString(), anException);
                //TODO what further to do if atom container could not be parsed?? Indicate this in cache? Or display error image below?
            }
            Image tmpImage = DepictionUtil.depictImageWithZoomAndFillToFitAndWhiteBackground(
                    this.atomContainerForDisplayCache,
                    this.imageZoomFactor,
                    this.imageWidth,
                    this.imageHeight,
                    true,
                    true);
            anImageView.setImage(tmpImage);
        });
        // Listener ContextMenuItems
        tmpCopySmilesMenuItem.setOnAction(event -> {
            ClipboardContent tmpSmilesClipboardContent = new ClipboardContent();
            tmpSmilesClipboardContent.putString(aSmiles);
            Clipboard.getSystemClipboard().setContent(tmpSmilesClipboardContent);
        });
        tmpCopyStructureMenuItem.setOnAction(event -> {
            ClipboardContent tmpStructureClipboardContent = new ClipboardContent();
            Image tmpCopyImageOnBar = DepictionUtil.depictImageWithZoomAndFillToFitAndWhiteBackground(
                    this.atomContainerForDisplayCache,
                    12.0,
                    GuiDefinitions.GUI_COPY_IMAGE_IMAGE_WIDTH,
                    GuiDefinitions.GUI_COPY_IMAGE_IMAGE_HEIGHT,
                    true,
                    true);
            tmpStructureClipboardContent.putImage(tmpCopyImageOnBar);
            Clipboard.getSystemClipboard().setContent(tmpStructureClipboardContent);
        });
        tmpNodePane.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            tmpNodePane.setStyle("-fx-bar-fill: " + HistogramViewController.HISTOGRAM_BARS_COLOR_HEX_VALUE);
            anImageView.setImage(null);
        });
        return tmpNodePane;
    }
    //
    /**
     * Enables the labelling of the histogram.
     * Clicking on the CheckBox displays the frequencies next to the bars.
     * In this method, the labels are also added to the StackPanes that will later display the frequencies next to the bars.
     * The bar labels are an extension of the StackPanes.
     * The method enables the display of the bars with shadows when the checkbox provided for this purpose is clicked.
     *
     * @param aLabelCheckBox to make the display of the fragment labels adjustable
     * @param aBarStylingCheckBox to make the display of bar shadows adjustable
     * @param aStackPane to add the labels
     * @param aFrequency values of the frequencies
     */
    private void addFrequencyBarLabelToBarAndAddListenersToBarCheckBoxes(
            CheckBox aLabelCheckBox,
            CheckBox aBarStylingCheckBox,
            StackPane aStackPane,
            int aFrequency) {
        int tmpDigitLength = String.valueOf(aFrequency).length();
        Label tmpBarLabel = new Label();
        tmpBarLabel.setTranslateY(0);
        tmpBarLabel.setAlignment(Pos.CENTER_RIGHT);
        tmpBarLabel.setPrefWidth(GuiDefinitions.GUI_BAR_LABEL_SIZE * tmpDigitLength);
        tmpBarLabel.setMinWidth(GuiDefinitions.GUI_BAR_LABEL_SIZE * tmpDigitLength);
        tmpBarLabel.setMaxWidth(GuiDefinitions.GUI_BAR_LABEL_SIZE * tmpDigitLength);
        tmpBarLabel.setTranslateX(tmpDigitLength * GuiDefinitions.GUI_BAR_LABEL_SIZE + 5);
        tmpBarLabel.setStyle(null);
        tmpBarLabel.setText(String.valueOf(aFrequency));
        if(this.displayBarLabelsSetting.get()) {
           aStackPane.getChildren().add(tmpBarLabel);
        }
        //TODO move this (all the listeners) somewhere else? Can we turn this around, i.e. one listener displays/hides all the labels?
        aLabelCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if (aLabelCheckBox.isSelected()) {
                aStackPane.getChildren().add(tmpBarLabel);
            } else {
                aStackPane.getChildren().remove(tmpBarLabel);
            }
        });
        if(this.displayBarShadowsSetting.get()) {
            aStackPane.setEffect(new DropShadow(10,2,3, Color.BLACK));
        }
        aBarStylingCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if(aBarStylingCheckBox.isSelected()) {
                aStackPane.setEffect(new DropShadow(10, 2, 3, Color.BLACK));
            } else
                aStackPane.setEffect(null);
        });
    }
    //
    /**
     * Method to create the bar gaps depending on the bar width and histogram height factor value (category gap).
     * Default bar width option used is "large" if the given option does not fit the predefined possible options.
     *
     * @param aNumberOfDisplayedFragments fragment number currently displayed in the HistogramView
     * @param aBarWidthOptionConstant enum constant from BarWidthOption to set the bar width value
     * @return double array which contains both the value of the histogram height factor [0] and the value of the category gap [1].
     */
    private Double[] calculateBarSpacing(int aNumberOfDisplayedFragments, BarWidthOption aBarWidthOptionConstant) {
        Double[] tmpHistogramHeightFactorAndCategoryGap = new Double[2];
        double tmpCurrentHistogramHeight;
        double tmpGapDeviation;
        double tmpGapSpacing;
        double tmpCategoryGap = 0;
        double tmpFinalHistogramHeight = 0;
        double tmpFinalGapSpacing;
        switch (aBarWidthOptionConstant) {
            case SMALL:
                if (aNumberOfDisplayedFragments <= 24) {
                    tmpCurrentHistogramHeight = GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / aNumberOfDisplayedFragments;
                    tmpGapDeviation = tmpCurrentHistogramHeight / (GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / 24);
                    tmpGapSpacing = GuiDefinitions.GUI_HISTOGRAM_SMALL_BAR_GAP_CONST * tmpGapDeviation;
                    tmpFinalGapSpacing = tmpCurrentHistogramHeight - tmpGapSpacing;
                    tmpCategoryGap = tmpFinalGapSpacing - GuiDefinitions.GUI_HISTOGRAM_SMALL_BAR_WIDTH;
                } else {
                    tmpFinalHistogramHeight = GuiDefinitions.GUI_HISTOGRAM_SMALL_HISTOGRAM_HEIGHT_VALUE;
                    tmpCurrentHistogramHeight = tmpFinalHistogramHeight * aNumberOfDisplayedFragments - 85;
                    tmpGapSpacing = tmpCurrentHistogramHeight / aNumberOfDisplayedFragments;
                    tmpCategoryGap = tmpGapSpacing - GuiDefinitions.GUI_HISTOGRAM_SMALL_BAR_WIDTH;
                }
                break;
            case MEDIUM:
                if (aNumberOfDisplayedFragments <= 17) {
                    tmpCurrentHistogramHeight = GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / aNumberOfDisplayedFragments;
                    tmpGapDeviation = tmpCurrentHistogramHeight / (GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / 17);
                    tmpGapSpacing = GuiDefinitions.GUI_HISTOGRAM_MEDIUM_BAR_GAP_CONST * tmpGapDeviation;
                    tmpFinalGapSpacing = tmpCurrentHistogramHeight - tmpGapSpacing;
                    tmpCategoryGap = tmpFinalGapSpacing - GuiDefinitions.GUI_HISTOGRAM_MEDIUM_BAR_WIDTH;
                } else {
                    tmpFinalHistogramHeight = GuiDefinitions.GUI_HISTOGRAM_MEDIUM_HISTOGRAM_HEIGHT_VALUE;
                    tmpCurrentHistogramHeight = tmpFinalHistogramHeight * aNumberOfDisplayedFragments - 85;
                    tmpGapSpacing = tmpCurrentHistogramHeight / aNumberOfDisplayedFragments;
                    tmpCategoryGap = tmpGapSpacing - GuiDefinitions.GUI_HISTOGRAM_MEDIUM_BAR_WIDTH ;
                }
                break;
            case LARGE:
            default:
                if (aNumberOfDisplayedFragments <= 13) {
                    tmpCurrentHistogramHeight = GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / aNumberOfDisplayedFragments;
                    tmpGapDeviation = tmpCurrentHistogramHeight / (GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / 13);
                    tmpGapSpacing = GuiDefinitions.GUI_HISTOGRAM_LARGE_BAR_GAP_CONST * tmpGapDeviation;
                    tmpFinalGapSpacing = tmpCurrentHistogramHeight - tmpGapSpacing;
                    tmpCategoryGap = tmpFinalGapSpacing - GuiDefinitions.GUI_HISTOGRAM_LARGE_BAR_WIDTH;
                } else {
                    tmpFinalHistogramHeight = GuiDefinitions.GUI_HISTOGRAM_LARGE_HISTOGRAM_HEIGHT_VALUE;
                    tmpCurrentHistogramHeight = tmpFinalHistogramHeight * aNumberOfDisplayedFragments - 85;
                    tmpGapSpacing = tmpCurrentHistogramHeight / aNumberOfDisplayedFragments;
                    tmpCategoryGap = tmpGapSpacing - GuiDefinitions.GUI_HISTOGRAM_LARGE_BAR_WIDTH;
                }
                break;
        }
        tmpHistogramHeightFactorAndCategoryGap[0] = tmpFinalHistogramHeight;
        tmpHistogramHeightFactorAndCategoryGap[1] = tmpCategoryGap;
        return tmpHistogramHeightFactorAndCategoryGap;
    }
    //
    /**
     * Method which calculates an optimal value for the x-axis upper bound that leaves enough room for the frequency labels.
     *
     * @param aMaxValue is the value of the highest frequency that occurs in the data set.
     * @param aTickValue is the calculated tick
     * @return an upper limit for the x-axis that leaves enough room for the frequency labels
     */
    private int calculateXAxisUpperBoundWithSpaceForLabels(int aMaxValue, int aTickValue) {
        int tmpTickNumber = (int) Math.round(aMaxValue / aTickValue);
        int tmpXAxisExtensionValue;
        if ((aTickValue * tmpTickNumber) > aMaxValue) {
            tmpXAxisExtensionValue = (aTickValue * tmpTickNumber) + aTickValue;
        } else {
            tmpXAxisExtensionValue = (aTickValue * tmpTickNumber) + (2 * aTickValue);
        }
        return tmpXAxisExtensionValue;
    }
    //
    /**
     * Returns the BarWidthOption enum constant with the given display name, e.g. taken from the bar width combo box in the
     * GUI. If the name is null, empty, or does not equal a display name in the enum, the default bar width
     * option is returned and a message logged.
     *
     * @param aDisplayName the displayed bar width option name
     * @return enum constant associated with the display name or default value
     */
    private BarWidthOption getBarWidthOptionEnumConstantFromDisplayName(String aDisplayName) {
        if(Objects.isNull(aDisplayName) || aDisplayName.isBlank()) {
            HistogramViewController.LOGGER.log(Level.WARNING, "Given string is null or empty, default bar width" +
                    "option is returned.");
            return HistogramViewController.DEFAULT_BAR_WIDTH;
        }
        BarWidthOption tmpEnumConstantBarWidth = null;
        for (BarWidthOption tmpOption : BarWidthOption.values()) {
            if (tmpOption.getDisplayName().equals(aDisplayName)) {
                tmpEnumConstantBarWidth = tmpOption;
            }
        }
        if (Objects.isNull(tmpEnumConstantBarWidth)) {
            HistogramViewController.LOGGER.log(Level.WARNING, "Output of histogram view bar spacing combo box \""
                    + aDisplayName + "\"did not equal any of the pre-set enum values and was reset to default.");
            tmpEnumConstantBarWidth = HistogramViewController.DEFAULT_BAR_WIDTH;
        }
        return tmpEnumConstantBarWidth;
    }
    /**
     * Returns the FrequencyOption enum constant with the given display name, e.g. taken from the frequency combo box in the
     * GUI. If the name is null, empty, or does not equal a display name in the enum, the default frequency
     * option is returned and a message logged.
     *
     * @param aDisplayName the displayed frequency option name
     * @return enum constant associated with the display name or default value
     */
    private FrequencyOption getFrequencyOptionEnumConstantFromDisplayName(String aDisplayName) {
        if(Objects.isNull(aDisplayName) || aDisplayName.isBlank()) {
            HistogramViewController.LOGGER.log(Level.WARNING, "Given string is null or empty, default frequency " +
                    "option is returned.");
            return HistogramViewController.DEFAULT_DISPLAY_FREQUENCY;
        }
        FrequencyOption tmpEnumConstantFrequencyOption = null;
        for (FrequencyOption tmpOption : FrequencyOption.values()) {
            if (tmpOption.getDisplayName().equals(aDisplayName)) {
                tmpEnumConstantFrequencyOption = tmpOption;
            }
        }
        if (Objects.isNull(tmpEnumConstantFrequencyOption)) {
            HistogramViewController.LOGGER.log(Level.WARNING, "Output of histogram view frequency combo box \""
                    + aDisplayName + "\"did not equal any of the pre-set enum values and was reset to default.");
            tmpEnumConstantFrequencyOption = HistogramViewController.DEFAULT_DISPLAY_FREQUENCY;
        }
        return tmpEnumConstantFrequencyOption;
    }
    //</editor-fold>
}