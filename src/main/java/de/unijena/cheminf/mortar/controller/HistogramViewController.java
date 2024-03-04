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

/**
 * TODO:
 * - add default button to histogram view
 */

import de.unijena.cheminf.mortar.gui.util.GuiDefinitions;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.gui.views.HistogramView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.FragmentDataModel;
import de.unijena.cheminf.mortar.model.depict.DepictionUtil;
import de.unijena.cheminf.mortar.model.util.ChemUtil;
import de.unijena.cheminf.mortar.model.util.CollectionUtil;
import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
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
    //<editor-fold desc="Enums" defaultstate="collapsed">
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
    //
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
    //</editor-fold>
    //
    //<editor-fold desc="public static final class constants" defaultstate="collapsed">
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
    /**
     * Value for the width of the image corresponding to the structure of the fragments
     */
    public static final double STRUCTURE_DEPICTION_IMAGE_INITIAL_WIDTH = 250.0;
    /**
     * Value for the height of the image corresponding to the structure of the fragments
     */
    public static final double STRUCTURE_DEPICTION_IMAGE_INITIAL_HEIGHT = 150.0;
    /**
     * Image zoom factor value
     */
    public static final double STRUCTURE_DEPICTION_IMAGE_INITIAL_ZOOM_FACTOR = 3.0;
    /**
     * Value for the small bar gap
     */
    public static final double GUI_HISTOGRAM_SMALL_BAR_GAP_CONST = 3.5416;
    /**
     * Value fpr the medium bar gap
     */
    public static final double GUI_HISTOGRAM_MEDIUM_BAR_GAP_CONST = 5.0;
    /**
     * Value for the large bar gap
     */
    public static final double GUI_HISTOGRAM_LARGE_BAR_GAP_CONST = 6.5384;
    /**
     * Value for the small bar width
     */
    public static final double GUI_HISTOGRAM_SMALL_BAR_WIDTH = 15.0;
    /**
     * Value for the medium bar width
     */
    public static final double GUI_HISTOGRAM_MEDIUM_BAR_WIDTH = 20.0;
    /***
     * Value for the large bar width
     */
    public static final double GUI_HISTOGRAM_LARGE_BAR_WIDTH = 30.0;
    /**
     * Value for the small histogram growth factor
     */
    public static final double GUI_HISTOGRAM_SMALL_HISTOGRAM_HEIGHT_VALUE = 27.0;
    /**
     * Value for the medium histogram growth factor
     */
    public static final double GUI_HISTOGRAM_MEDIUM_HISTOGRAM_HEIGHT_VALUE = 37.0;
    /**
     * Value for the large histogram growth factor
     */
    public static final double GUI_HISTOGRAM_LARGE_HISTOGRAM_HEIGHT_VALUE = 50.0;
    /**
     * Value of the bar label sizes
     */
    public static final double GUI_BAR_LABEL_SIZE = 10.0;
    /**
     * Value of tickLabel length
     */
    public static  final double HISTOGRAM_TICK_LABEL_LENGTH = 15.0;
    /**
     * Value of tickLabel gap
     */
    public static final double HISTOGRAM_TICK_LABEL_GAP = 10.0;
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
     * Scene of the histogram stage
     */
    private Scene histogramScene;
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
     * X-axis of the histogram
     */
    private NumberAxis numberAxis;
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
    //<editor-fold desc="Constructors" defaultstate="collapsed">
    /**
     * Constructor, initialises all settings with their default values. Does *not* open the view.
     */
    public HistogramViewController()  {
        this.settings = new ArrayList<>(8);
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
                HistogramViewController.FrequencyOption.class) {
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
        this.displaySMILESSetting = new SimpleBooleanProperty(this,
                //the name could be displayed but is not used for that currently
                Message.get("HistogramView.displaySMILESSetting.name"),
                HistogramViewController.DEFAULT_DISPLAY_SMILES_SETTING) {
            @Override
            public void set(boolean newValue) {
                super.set(newValue);
                //value transferred to GUI in openHistogramView()
                //value also used in addListenersToHistogramView()
            }
        };
        this.settings.add(this.displaySMILESSetting);
    }
    //</editor-fold>
    //
    //<editor-fold desc="public methods" defaultstate="collapsed">
    //<editor-fold desc="public methods inherited from IViewToolController" defaultstate="collapsed">
    /**
     * {@inheritDoc}
     *
     * <p>
     *     For the histogram view, settings can be configured via the properties only(!) between instantiating the object and
     *     opening the view using .openHistogramView().
     * </p>
     */
    @Override
    public List<Property> settingsProperties() {
        //note: see comments in constructor for how the setting values are transferred in both directions (GUI <-> Properties)
        return this.settings;
    }
    //
    /**
     * {@inheritDoc}
     */
    @Override
    public String getViewToolNameForDisplay() {
        return HistogramViewController.VIEW_TOOL_NAME_FOR_DISPLAY;
    }
    //
    /**
     * {@inheritDoc}
     *
     * <p>
     *     Note: For the histogram view, the settings will only be updated in the GUI when the view is reopened!
     * </p>
     */
    @Override
    public void restoreDefaultSettings() {
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
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canBeUsedOnTab(TabNames aTabNameEnumConstant) {
        return switch (aTabNameEnumConstant) {
            case FRAGMENTS, ITEMIZATION -> true;
            default -> false;
        };
    }
    //</editor-fold>
    /**
     * Initialises stage and view and opens view in the initialised stage. Also, adds listeners and sets settings
     * according to current values of the setting properties.
     *
     * @param aMainStage Stage of the MainView
     * @param aFragmentDataModelList ObservableList that holds FragmentDataModel objects for visualisation in histogram
     * @throws NullPointerException if any param is null
     */
    public void openHistogramView(Stage aMainStage, List<FragmentDataModel> aFragmentDataModelList) throws NullPointerException {
        //<editor-fold desc="Checks" defaultstate="collapsed">
        Objects.requireNonNull(aMainStage, "Main stage is null.");
        Objects.requireNonNull(aFragmentDataModelList, "Fragment data model list is ´null");
        //</editor-fold>
        //
        //<editor-fold desc="initialisation of stage and view" defaultstate="collapsed">
        //note: these need to be reset to default here because the histogram view is re-initialised with default size and
        // the image dimensions are adjusted when the view is resized
        this.imageWidth = HistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_WIDTH;
        this.imageHeight = HistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_HEIGHT;
        this.imageZoomFactor = HistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_ZOOM_FACTOR;
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
        } else {
            HistogramViewController.LOGGER.log(Level.WARNING, "Mortar_Logo_Icon1.png could not be imported.");
        }
        this.histogramView = new HistogramView(this.fragmentListCopy.size());
        //</editor-fold>
        //
        //<editor-fold desc="set setting values in view" defaultstate="collapsed>
        //note: no binding between the text field and the setting property is established because the setting is only updated
        // when the "apply" button is clicked
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
        //note: settings are updated for persistence in the listeners on the checkboxes added below
        this.histogramView.getDisplayBarLabelsCheckBox().setSelected(this.displayBarLabelsSetting.get());
        this.histogramView.getDisplayBarShadowsCheckBox().setSelected(this.displayBarShadowsSetting.get());
        this.histogramView.getDisplayGridLinesCheckBox().setSelected(this.displayGridLinesSetting.get());
        this.histogramView.getDisplaySmilesOnYAxisCheckBox().setSelected(this.displaySMILESSetting.get());
        //</editor-fold>
        //
        //<editor-fold desc="Initialise histogram with given settings and add listeners" defaultstate="collapsed">
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
        //</editor-fold>
        //
        //<editor-fold desc="Scene creation, listener adding, and display of view/stage" defaultstate="collapsed">
        this.histogramScene = new Scene(
                this.histogramView,
                GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE,
                GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE);
        this.histogramStage.setScene(this.histogramScene);
        this.addListenersToHistogramView();
        this.histogramStage.show();
        //</editor-fold>
    }
    //</editor-fold>
    //
    //<editor-fold desc="private methods" defaultstate="collapsed">
    /**
     * Create a configurable histogram, called once when the view is shown and again whenever the apply button is used.
     * All data is stored in corresponding lists (the x-axis data are either fragment or molecule frequencies).
     * Depending on how aFragmentNumber is chosen, these lists/data are adjusted.
     * After they have been run through, they are displayed accordingly.
     * The x-axis of the histogram is divided into readable steps depending on the maximum frequency (automatic
     * adjustment of the x-axis ticks).
     *
     * @param aFragmentNumber sets the number of fragments that are displayed
     * @param aHistogramView to access the view elements
     * @param aSmilesLength sets the maximum SMILES length
     * @param aBarLabelCheckBox CheckBox for choosing whether to label the bars with frequencies; needed for current
     *                          value and listener adding
     * @param aStylingCheckBox CheckBox for displaying or hiding bar shadows; needed for current value and listener
     *                         adding
     * @param aHistogramDefaultSize a double value that determines the height of the histogram; the height of the
     *                              histogram increases with the increase in fragments that are displayed.
     * @throws IllegalArgumentException if given number of fragments to display is bigger than number of available fragments
     * @return a BarChart to display in the view
     */
    private BarChart createHistogram(int aFragmentNumber,
                                     HistogramView aHistogramView,
                                     int aSmilesLength,
                                     CheckBox aBarLabelCheckBox,
                                     CheckBox aStylingCheckBox,
                                     double aHistogramDefaultSize) throws IllegalArgumentException
    {
        //<editor-fold desc="Checks", defaultstate="collapsed">
        if (aFragmentNumber >  this.fragmentListCopy.size())
            throw new IllegalArgumentException("Given number of fragments to display is bigger than number of available fragments.");
        //</editor-fold>
        //
        //<editor-fold desc="Initialisation of axes and histogram" defaultstate="collapsed">
        this.categoryAxis = new CategoryAxis();
        this.categoryAxis.setTickLabelFill(Color.BLACK);
        this.categoryAxis.setTickLength(HistogramViewController.HISTOGRAM_TICK_LABEL_LENGTH);
        this.categoryAxis.setTickLabelGap(HistogramViewController.HISTOGRAM_TICK_LABEL_GAP);
        this.categoryAxis.setLabel(Message.get("HistogramViewController.YAxisLabel.text"));
        this.categoryAxis.setTickMarkVisible(this.displaySMILESSetting.get());
        this.categoryAxis.setTickLabelsVisible(this.displaySMILESSetting.get());
        this.numberAxis = new NumberAxis();
        this.numberAxis.setSide(Side.TOP);
        this.numberAxis.setAutoRanging(false);
        this.numberAxis.setMinorTickCount(1);
        this.numberAxis.setForceZeroInRange(true);
        this.numberAxis.setTickLabelFill(Color.BLACK);
        this.numberAxis.setLabel(Message.get("HistogramViewController.XAxisLabel.text"));
        BarChart tmpHistogramBarChart = new BarChart(this.numberAxis, this.categoryAxis);
        tmpHistogramBarChart.setCategoryGap(0.0);
        tmpHistogramBarChart.setBarGap(0.0);
        ScrollPane tmpScrollPane = aHistogramView.getHistogramScrollPane();
        tmpScrollPane.setContent(tmpHistogramBarChart);
        //</editor-fold>
        //
        //<editor-fold desc="create data for histogram, i.e. categories ((abbr.) SMILES) and values (frequencies)" defaultstate="collapsed">
        String tmpNewSmiles;
        ArrayList<String> tmpSmilesList = new ArrayList<>();
        ArrayList<Integer> tmpFrequencyList = new ArrayList<>();
        int tmpFragmentListIndexDecreasing = this.fragmentListCopy.size();
        ArrayList<String> tmpFullSmilesLength = new ArrayList<>();
        //if false, the "molecule frequency" is used for sorting instead
        boolean tmpSortByFragmentFrequency = this.displayFrequencySetting.get().equals(FrequencyOption.ABSOLUTE_FREQUENCY.name());
        String tmpSortProperty = (tmpSortByFragmentFrequency ? "absoluteFrequency" : "moleculeFrequency");
        //TODO: rework method and usage
        CollectionUtil.sortGivenFragmentListByPropertyAndSortType(this.fragmentListCopy, tmpSortProperty, "ASCENDING");
        for (FragmentDataModel tmpFragmentDataModel : this.fragmentListCopy) {
            if (tmpFragmentDataModel.getUniqueSmiles().length() > aSmilesLength) {
                tmpNewSmiles = Message.get("HistogramView.smilesTooLong") + " (" + tmpFragmentListIndexDecreasing + ")";
            } else {
                tmpNewSmiles = tmpFragmentDataModel.getUniqueSmiles();
            }
            tmpSmilesList.add(tmpNewSmiles);
            tmpFragmentListIndexDecreasing--;
            tmpFullSmilesLength.add(tmpFragmentDataModel.getUniqueSmiles());
            tmpFrequencyList.add(tmpSortByFragmentFrequency ? tmpFragmentDataModel.getAbsoluteFrequency() : tmpFragmentDataModel.getMoleculeFrequency());
        }
        //</editor-fold>
        //
        //<editor-fold desc="calculate tick size and histogram size" defaultstate="collapsed">
        double tmpMaxFrequency = Collections.max(tmpFrequencyList);
        //make readable x-axis
        double tmpXAxisTicks = 0.05 * tmpMaxFrequency; // magic number
        double tmpXAxisExtension = 0.15 * tmpMaxFrequency; // magic number
        int tmpIntTmpXAxisTick = (int) Math.round(tmpXAxisTicks);
        int tmpIntXAxisExtension = (int) Math.round(tmpXAxisExtension);
        if (tmpIntTmpXAxisTick == 0 || tmpIntXAxisExtension == 0) {
           this.numberAxis.setTickUnit(1.0);
           this.numberAxis.setUpperBound(tmpMaxFrequency + 1.0);
        } else {
            if (tmpIntTmpXAxisTick < 10) {
                this.numberAxis.setTickUnit(tmpIntTmpXAxisTick);
                this.numberAxis.setUpperBound(this.calculateXAxisUpperBoundWithSpaceForLabels((int) tmpMaxFrequency, tmpIntTmpXAxisTick));
            } else {
                int tmpNewXAxisTick = tmpIntTmpXAxisTick;
                String tmpTickStringRepresentation = String.valueOf(tmpNewXAxisTick);
                String tmpFirstValue = String.valueOf(tmpTickStringRepresentation.charAt(0));
                int tmpFirstIntValue = Integer.parseInt(tmpFirstValue);
                if (tmpFirstIntValue <= 5) {
                    //If the first digit of tmpIntTmpXAxisTick is smaller than 5, we look for a suitable "round" number
                    // for the ticks, e.g. tmpIntTmpXAxisTick = 356 -> tmpNewXAxisTick = 400
                    int tmpDigit = tmpTickStringRepresentation.length() - 1;
                    int tmpPowerOfTen = (int) Math.pow(10, tmpDigit);
                    if (tmpNewXAxisTick % tmpPowerOfTen != 0) {
                        do {
                            tmpNewXAxisTick++;
                        } while (tmpNewXAxisTick % (tmpPowerOfTen) != 0);
                    }
                } else {
                    //If the first digit of tmpIntTmpXAxisTick is greater than 5, we choose a suitable, "round" power of 10 for
                    // the ticks, e.g. tmpIntTmpXAxisTick = 7896 -> tmpNewXAxisTick = 10.000
                    tmpNewXAxisTick = (int) Math.pow(10, tmpTickStringRepresentation.length());
                }
                this.numberAxis.setTickUnit(tmpNewXAxisTick);
                this.numberAxis.setUpperBound(this.calculateXAxisUpperBoundWithSpaceForLabels((int) tmpMaxFrequency, tmpNewXAxisTick));
            }
        }
        //</editor-fold>
        //
        //<editor-fold desc="add functionality (structure display at hover) to bars" defaultstate="collapsed>
        List<String> tmpSublistSmiles = tmpSmilesList.subList(tmpSmilesList.size() - aFragmentNumber,
                tmpSmilesList.size());
        List<Integer> tmpSublistFrequency = tmpFrequencyList.subList(tmpFrequencyList.size() - aFragmentNumber,
                tmpFrequencyList.size());
        List<String> tmpSmilesToDepict = tmpFullSmilesLength.subList(tmpFullSmilesLength.size()- aFragmentNumber,
                tmpFullSmilesLength.size());
        XYChart.Series tmpSeries = new XYChart.Series();
        if (tmpSublistSmiles.size() != tmpSublistFrequency.size() || tmpSublistSmiles.size() != tmpSmilesToDepict.size()) {
            throw new IllegalArgumentException("SMILES code and frequency sublists for display are of unequal size.");
        }
        for (int i = 0; i < tmpSublistSmiles.size(); i++) {
            Integer tmpCurrentFrequency = tmpSublistFrequency.get(i);
            String tmpCurrentDisplaySmiles = tmpSublistSmiles.get(i);
            String tmpCurrentFullSmilesForParsing = tmpSmilesToDepict.get(i);
            XYChart.Data<Number, String> tmpStringNumberData = new XYChart.Data(tmpCurrentFrequency, tmpCurrentDisplaySmiles);
            StackPane tmpHistogramBarStackPane = this.createStackPaneWithContextMenuAndStructureDisplayForBar(
                    aHistogramView.getStructureDisplayImageView(),
                    tmpCurrentFullSmilesForParsing);
            tmpHistogramBarStackPane.setStyle("-fx-bar-fill: " + HistogramViewController.HISTOGRAM_BARS_COLOR_HEX_VALUE);
            this.addFrequencyBarLabelToBarAndAddListenersToBarCheckBoxes(
                    aBarLabelCheckBox,
                    aStylingCheckBox,
                    tmpHistogramBarStackPane,
                    tmpCurrentFrequency);
            tmpStringNumberData.setNode(tmpHistogramBarStackPane);
            tmpSeries.getData().add(tmpStringNumberData);
        }
        //</editor-fold>
        //<editor-fold desc="layout histogram bar chart" defauktstate="collapsed">
        double tmpHistogramSize = aHistogramDefaultSize * tmpSublistFrequency.size();
        tmpHistogramBarChart.setPrefHeight(tmpHistogramSize);
        tmpHistogramBarChart.setMinHeight(tmpHistogramSize);
        tmpHistogramBarChart.getData().add(tmpSeries);
        tmpHistogramBarChart.setLegendVisible(false);
        tmpHistogramBarChart.layout();
        tmpHistogramBarChart.setHorizontalGridLinesVisible(this.displayGridLinesSetting.get());
        tmpHistogramBarChart.setVerticalGridLinesVisible(this.displayGridLinesSetting.get());
        tmpHistogramBarChart.setAnimated(false);
        //</editor-fold>
        return tmpHistogramBarChart;
    }
    //
    /**
     * Add listeners and/or text formatters to close button, displayed fragments number text field, maximum smiles
     * length text field, apply button, display grid lines checkbox, display SMILES codes on y-axis check box, width
     * and height properties of the scene for resizing.
     */
    private void addListenersToHistogramView() {
        //close histogram stage with close button and stage window close request
        this.histogramView.getCloseButton().setOnAction(event -> {
            this.histogramStage.close();
            this.clearAllGUICaches();
        });
        this.histogramStage.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, (this::closeWindowEvent));
        //adding text formatter that only accepts integers and turns the input strings into those to the two text fields
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
        //disable apply button if both(!) text fields are empty
        this.histogramView.getApplyButton().disableProperty().bind(
                Bindings.isEmpty(this.histogramView.getDisplayedFragmentsNumberTextField().textProperty()).
                            and(Bindings.isEmpty(this.histogramView.getMaximumSMILESLengthTextField().textProperty()))
        );
        //apply button, update histogram
        this.histogramView.getApplyButton().setOnAction(event -> {
            //if both text fields are empty, "apply" is disabled, see above
            // but if only one text field is empty, it is reset to default here along with its tied setting property
            if (this.histogramView.getMaximumSMILESLengthTextFieldContent().isEmpty()) {
                //maximum SMILES length text field is empty -> reset this setting to default and parse displayed
                // fragments number setting from text field
                this.displayedFragmentsNumberSetting.set(Integer.parseInt(this.histogramView.getDisplayedFragmentsNumberTextFieldContent()));
                if (this.displayedFragmentsNumberSetting.get() > this.fragmentListCopy.size()) {
                    GuiUtil.guiMessageAlert(Alert.AlertType.WARNING, Message.get("HistogramViewController.HistogramGeneralRefreshWarning.Title"),
                            Message.get("HistogramViewController.HistogramFrequencyRefreshWarning.Header"),
                            Message.get("HistogramViewController.HistogramFrequencyRefreshWarning.Content"));
                    //no resets of settings or text field content, the user has to take care of that
                    return;
                }
                this.maximumSMILESLengthSetting.set(HistogramViewController.DEFAULT_MAX_SMILES_LENGTH);
                this.histogramView.getMaximumSMILESLengthTextField().setText(String.valueOf(this.maximumSMILESLengthSetting.get()));
            } else if (this.histogramView.getDisplayedFragmentsNumberTextFieldContent().isEmpty()) {
                //displayed fragments nr text field is empty -> reset this setting to default and parse maximum SMILES length
                // setting from text field
                this.maximumSMILESLengthSetting.set(Integer.parseInt(this.histogramView.getMaximumSMILESLengthTextFieldContent()));
                if (this.fragmentListCopy.size() >= HistogramViewController.DEFAULT_NUMBER_OF_DISPLAYED_FRAGMENTS) {
                    this.displayedFragmentsNumberSetting.set(HistogramViewController.DEFAULT_NUMBER_OF_DISPLAYED_FRAGMENTS);
                } else {
                    this.displayedFragmentsNumberSetting.set(this.fragmentListCopy.size());
                }
                this.histogramView.getDisplayedFragmentsNumberTextField().setText(String.valueOf(this.displayedFragmentsNumberSetting.get()));
            } else {
                //both text fields have values -> parse and check
                this.displayedFragmentsNumberSetting.set(Integer.parseInt(this.histogramView.getDisplayedFragmentsNumberTextFieldContent()));
                this.maximumSMILESLengthSetting.set(Integer.parseInt(this.histogramView.getMaximumSMILESLengthTextFieldContent()));
                if (this.displayedFragmentsNumberSetting.get() > this.fragmentListCopy.size()) {
                    GuiUtil.guiMessageAlert(Alert.AlertType.WARNING, Message.get("HistogramViewController.HistogramGeneralRefreshWarning.Title"),
                            Message.get("HistogramViewController.HistogramFrequencyRefreshWarning.Header"),
                            Message.get("HistogramViewController.HistogramFrequencyRefreshWarning.Content"));
                    //no resets of settings or text field content, the user has to take care of that
                    return;
                }
            }
            BarWidthOption tmpBarWidthSettingEnumValue = this.getBarWidthOptionEnumConstantFromDisplayName(
                    (String)this.histogramView.getBarWidthsComboBox().getValue());
            this.barWidthSetting.set(tmpBarWidthSettingEnumValue.name());
            Double[] tmpHistogramSizeGap = this.calculateBarSpacing(
                    this.displayedFragmentsNumberSetting.get(),
                    tmpBarWidthSettingEnumValue);
            this.displayFrequencySetting.set(this.getFrequencyOptionEnumConstantFromDisplayName((String)this.histogramView.getFrequencyComboBox().getValue()).name());
            this.histogramChart = this.createHistogram(
                    this.displayedFragmentsNumberSetting.get(),
                    this.histogramView,
                    this.maximumSMILESLengthSetting.get(),
                    this.histogramView.getDisplayBarLabelsCheckBox(),
                    this.histogramView.getDisplayBarShadowsCheckBox(),
                    tmpHistogramSizeGap[0]);
            this.histogramChart.setCategoryGap(tmpHistogramSizeGap[1]);
            boolean tmpDisplayGridLines = this.displayGridLinesSetting.get();
            this.histogramChart.setVerticalGridLinesVisible(tmpDisplayGridLines);
            this.histogramChart.setHorizontalGridLinesVisible(tmpDisplayGridLines);
            boolean tmpDisplaySMILES = this.displaySMILESSetting.get();
            this.categoryAxis.setTickMarkVisible(tmpDisplaySMILES);
            this.categoryAxis.setTickLabelsVisible(tmpDisplaySMILES);
        });
        this.histogramView.getDisplayGridLinesCheckBox().selectedProperty()
                .addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            this.histogramChart.setVerticalGridLinesVisible(new_val);
            this.histogramChart.setHorizontalGridLinesVisible(new_val);
            //update setting for persistence
            this.displayGridLinesSetting.set(new_val);
        });
        this.histogramView.getDisplaySmilesOnYAxisCheckBox().selectedProperty()
                .addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            this.categoryAxis.setTickMarkVisible(new_val);
            this.categoryAxis.setTickLabelsVisible(new_val);
            //update setting for persistence
            this.displaySMILESSetting.set(new_val);
        });
        this.histogramScene.widthProperty().addListener((observable, oldValue, newValue) -> {
            double tmpWidthChange = ((this.histogramScene.getWidth() - GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE) / GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE) * 100.0;
            double tmpImageWidthChange = (HistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_WIDTH / 100.0) * tmpWidthChange;
            this.imageWidth = HistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_WIDTH + tmpImageWidthChange;
            this.imageHeight = this.imageWidth - 100.0;
            this.imageZoomFactor = (HistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_ZOOM_FACTOR / HistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_WIDTH) * this.imageWidth;
        });
        this.histogramScene.heightProperty().addListener((observable, oldValue, newValue) -> {
            double tmpHeightChange = ((this.histogramScene.getHeight() - GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE) / GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE) * 100.0;
            double tmpImageHeightChange = (HistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_HEIGHT / 100.0) * tmpHeightChange;
            if (this.histogramScene.getWidth() == GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE) {
                this.imageHeight = HistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_HEIGHT + tmpImageHeightChange;
            } else {
                double tmpHeight = this.imageWidth - 100.0;
                double tmpIntermediateImageHeight = HistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_HEIGHT + tmpImageHeightChange;
                double tmpImageHeight = tmpHeight - tmpIntermediateImageHeight;
                this.imageHeight = tmpIntermediateImageHeight + tmpImageHeight;
            }
            this.imageWidth = 100.0 + this.imageHeight;
            this.imageZoomFactor = (HistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_ZOOM_FACTOR / HistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_WIDTH) * this.imageWidth;
        });
    }
    //
    /**
     * Make the histogram hoverable and create the structure images that are displayed in the histogram when the cursor
     * hovers over a bar. Basically an addListener-method.
     * For this purpose, add a StackPane to each bar as a node, and use the StackPanes to call the corresponding listener.
     * Hovering over the bars displays the corresponding structures.
     * In addition, by right-clicking on a bar, the corresponding structure image (1500x1000) or the SMILES code can be
     * copied. This method needs to be executed for every bar in the histogram separately!
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
        ContextMenu tmpContextMenu = new ContextMenu();
        tmpContextMenu.getItems().addAll(tmpCopySmilesMenuItem, tmpCopyStructureMenuItem);
        try {
            tmpCopySmilesMenuItem.setGraphic(new ImageView(new Image("de/unijena/cheminf/mortar/images/copy_icon_16x16.png")));
            tmpCopyStructureMenuItem.setGraphic(new ImageView(new Image("de/unijena/cheminf/mortar/images/copy_icon_16x16.png")));
        } catch(NullPointerException | IllegalArgumentException anException) {
            HistogramViewController.LOGGER.log(Level.WARNING, "Copy icon for context menus could not be imported.");
        }
        // Event to hover over histogram bars to display structure
        EventHandler<MouseEvent> tmpMouseHoverEventHandler = event -> {
            tmpNodePane.setStyle("-fx-bar-fill: " + HistogramViewController.HISTOGRAM_BARS_SELECTED_COLOR_HEX_VALUE);
            this.atomContainerForDisplayCache = null;
            try {
                boolean tmpShouldBeKekulized = true;
                boolean tmpShouldAtomTypesBePerceived = true;
                this.atomContainerForDisplayCache = ChemUtil.parseSmilesToAtomContainer(aSmiles, tmpShouldBeKekulized, tmpShouldAtomTypesBePerceived);
            } catch (CDKException anException) {
                // no logging, this happens too often, e.g. for fragments of aromatic rings
                try {
                    this.atomContainerForDisplayCache = ChemUtil.parseSmilesToAtomContainer(aSmiles, false, false);
                } catch (CDKException aSecondException) {
                    HistogramViewController.LOGGER.log(Level.WARNING, aSecondException.toString(), aSecondException);
                    this.atomContainerForDisplayCache = null;
                    //Note: the used depiction method returns an error image if image creation fails, so nothing else to do here
                }
            }
            Image tmpImage = DepictionUtil.depictImageWithZoomAndFillToFitAndWhiteBackground(
                    this.atomContainerForDisplayCache,
                    this.imageZoomFactor,
                    this.imageWidth,
                    this.imageHeight,
                    true,
                    true);
            anImageView.setImage(tmpImage);
        };
        /* Event to open context menu (right click) to copy SMILES string or structure.
           Context menu also opens, if  a right click on the frequency label is detected.
         */
        EventHandler<ContextMenuEvent> tmpContextMenuEventHandler = event -> {
            tmpContextMenu.show(tmpNodePane, event.getScreenX(), event.getScreenY());
        };
        tmpNodePane.addEventHandler(MouseEvent.MOUSE_ENTERED, tmpMouseHoverEventHandler);
        tmpNodePane.addEventHandler(ContextMenuEvent.CONTEXT_MENU_REQUESTED, tmpContextMenuEventHandler);
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
     * Enables the labelling of the histogram. Needs to be executed for every bar in the histogram separately!
     * Clicking on the bar label check box displays the frequencies next to the bars.
     * In this method, the labels are also added to the StackPanes that will later display the frequencies next to the bars.
     * The bar labels are an extension of the StackPanes.
     * The method enables the display of the bars with shadows when the check box provided for this purpose is clicked.
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
        tmpBarLabel.setTranslateY(0.0);
        tmpBarLabel.setAlignment(Pos.CENTER_RIGHT);
        tmpBarLabel.setPrefWidth(HistogramViewController.GUI_BAR_LABEL_SIZE * (double) tmpDigitLength);
        tmpBarLabel.setMinWidth(HistogramViewController.GUI_BAR_LABEL_SIZE * (double) tmpDigitLength);
        tmpBarLabel.setMaxWidth(HistogramViewController.GUI_BAR_LABEL_SIZE * (double) tmpDigitLength);
        tmpBarLabel.setTranslateX(tmpDigitLength * HistogramViewController.GUI_BAR_LABEL_SIZE + 5.0);
        tmpBarLabel.setStyle(null);
        tmpBarLabel.setText(String.valueOf(aFrequency));
        if(this.displayBarLabelsSetting.get()) {
           aStackPane.getChildren().add(tmpBarLabel);
        }
        aLabelCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if (new_val) {
                aStackPane.getChildren().add(tmpBarLabel);
            } else {
                aStackPane.getChildren().remove(tmpBarLabel);
            }
            this.displayBarLabelsSetting.set(new_val);
        });
        if(this.displayBarShadowsSetting.get()) {
            aStackPane.setEffect(new DropShadow(10,2,3, Color.BLACK));
        }
        aBarStylingCheckBox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            if(new_val) {
                aStackPane.setEffect(new DropShadow(10, 2, 3, Color.BLACK));
            } else {
                aStackPane.setEffect(null);
            }
            this.displayBarShadowsSetting.set(new_val);
        });
    }
    //
    /**
     * Discards all GUI variable values for when the view is closed.
     */
    private void clearAllGUICaches() {
        this.mainStage = null;
        //note: must have been closed before
        this.histogramView = null;
        this.histogramStage = null;
        this.histogramScene = null;
        this.fragmentListCopy = null;
        this.imageWidth = HistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_WIDTH;
        this.imageHeight = HistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_HEIGHT;
        this.imageZoomFactor = HistogramViewController.STRUCTURE_DEPICTION_IMAGE_INITIAL_ZOOM_FACTOR;
        this.categoryAxis = null;
        this.numberAxis = null;
        this.histogramChart = null;
        this.atomContainerForDisplayCache = null;
    }
    //
    /**
     * Closes the histogram view (stage) and clears all GUI caches when close window event was fired.
     *
     * @param anEvent WindowEvent
     */
    private void closeWindowEvent(WindowEvent anEvent) {
        this.histogramStage.close();
        this.clearAllGUICaches();
    }
    //
    /**
     * Method to calculate the bar gaps depending on the bar width and histogram height factor value (category gap).
     * Default bar width option used is "large" if the given option does not fit the predefined possible options.
     *
     * @param aNumberOfDisplayedFragments fragment number currently displayed in the HistogramView
     * @param aBarWidthOptionConstant enum constant from BarWidthOption to set the bar width value
     * @return double array which contains both, a value for the histogram height factor [0] and a value for the category gap [1].
     */
    private Double[] calculateBarSpacing(int aNumberOfDisplayedFragments, BarWidthOption aBarWidthOptionConstant) {
        Double[] tmpHistogramHeightFactorAndCategoryGap = new Double[2];
        double tmpCurrentHistogramHeight;
        double tmpGapDeviation;
        double tmpGapSpacing;
        double tmpCategoryGap;
        double tmpFinalHistogramHeight = 0.0; //return value is initialised here with a default value
        double tmpFinalGapSpacing;
        switch (aBarWidthOptionConstant) {
            case SMALL:
                if (aNumberOfDisplayedFragments <= 24) { //magic number
                    tmpCurrentHistogramHeight = GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / (double) aNumberOfDisplayedFragments;
                    tmpGapDeviation = tmpCurrentHistogramHeight / (GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / 24.0);
                    tmpGapSpacing = HistogramViewController.GUI_HISTOGRAM_SMALL_BAR_GAP_CONST * tmpGapDeviation;
                    tmpFinalGapSpacing = tmpCurrentHistogramHeight - tmpGapSpacing;
                    tmpCategoryGap = tmpFinalGapSpacing - HistogramViewController.GUI_HISTOGRAM_SMALL_BAR_WIDTH;
                } else {
                    tmpFinalHistogramHeight = HistogramViewController.GUI_HISTOGRAM_SMALL_HISTOGRAM_HEIGHT_VALUE;
                    tmpCurrentHistogramHeight = tmpFinalHistogramHeight * (double) aNumberOfDisplayedFragments - 85.0;
                    tmpGapSpacing = tmpCurrentHistogramHeight / aNumberOfDisplayedFragments;
                    tmpCategoryGap = tmpGapSpacing - HistogramViewController.GUI_HISTOGRAM_SMALL_BAR_WIDTH;
                }
                break;
            case MEDIUM:
                if (aNumberOfDisplayedFragments <= 17) { //magic number
                    tmpCurrentHistogramHeight = GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / (double) aNumberOfDisplayedFragments;
                    tmpGapDeviation = tmpCurrentHistogramHeight / (GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / 17.0);
                    tmpGapSpacing = HistogramViewController.GUI_HISTOGRAM_MEDIUM_BAR_GAP_CONST * tmpGapDeviation;
                    tmpFinalGapSpacing = tmpCurrentHistogramHeight - tmpGapSpacing;
                    tmpCategoryGap = tmpFinalGapSpacing - HistogramViewController.GUI_HISTOGRAM_MEDIUM_BAR_WIDTH;
                } else {
                    tmpFinalHistogramHeight = HistogramViewController.GUI_HISTOGRAM_MEDIUM_HISTOGRAM_HEIGHT_VALUE;
                    tmpCurrentHistogramHeight = tmpFinalHistogramHeight * (double) aNumberOfDisplayedFragments - 85.0;
                    tmpGapSpacing = tmpCurrentHistogramHeight / (double) aNumberOfDisplayedFragments;
                    tmpCategoryGap = tmpGapSpacing - HistogramViewController.GUI_HISTOGRAM_MEDIUM_BAR_WIDTH ;
                }
                break;
            case LARGE:
            default:
                if (aNumberOfDisplayedFragments <= 13) { //magic number
                    tmpCurrentHistogramHeight = GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / (double) aNumberOfDisplayedFragments;
                    tmpGapDeviation = tmpCurrentHistogramHeight / (GuiDefinitions.GUI_NOT_SCROLLABLE_HEIGHT / 13.0);
                    tmpGapSpacing = HistogramViewController.GUI_HISTOGRAM_LARGE_BAR_GAP_CONST * tmpGapDeviation;
                    tmpFinalGapSpacing = tmpCurrentHistogramHeight - tmpGapSpacing;
                    tmpCategoryGap = tmpFinalGapSpacing - HistogramViewController.GUI_HISTOGRAM_LARGE_BAR_WIDTH;
                } else {
                    tmpFinalHistogramHeight = HistogramViewController.GUI_HISTOGRAM_LARGE_HISTOGRAM_HEIGHT_VALUE;
                    tmpCurrentHistogramHeight = tmpFinalHistogramHeight * (double) aNumberOfDisplayedFragments - 85.0;
                    tmpGapSpacing = tmpCurrentHistogramHeight / (double) aNumberOfDisplayedFragments;
                    tmpCategoryGap = tmpGapSpacing - HistogramViewController.GUI_HISTOGRAM_LARGE_BAR_WIDTH;
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
        int tmpTickNumber = Math.round((float) aMaxValue / aTickValue);
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
