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
import de.unijena.cheminf.mortar.gui.views.ClusterHistogramView;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.util.IDisplayEnum;
import de.unijena.cheminf.mortar.model.util.SimpleIDisplayEnumConstantProperty;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class ClusterHistogramViewController implements IViewToolController{

    public static final int DEFAULT_NUMBER_OF_DISPLAYED_CLUSTER = 10;

    private static final Logger LOGGER = Logger.getLogger(ClusterHistogramViewController.class.getName());

    private final IConfiguration configuration;
    private final SimpleIntegerProperty displayedClusterNumberSetting;
    private final SimpleIDisplayEnumConstantProperty barWidthSetting;
    private final SimpleBooleanProperty displayBarLabelsSetting;
    private final SimpleBooleanProperty displayBarShadowsSetting;
    private final SimpleBooleanProperty displayGridLinesSetting;

    private final List<Property<?>> settings;

    public ClusterHistogramViewController(IConfiguration aConfiguration) {
        this.configuration = aConfiguration;
        //todo: init cap
        this.settings = new ArrayList<>();
        this.displayedClusterNumberSetting = new SimpleIntegerProperty(this,
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
        this.settings.add(this.displayedClusterNumberSetting);
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
        ClusterHistogramView tmpTestClusterHistogramView = new ClusterHistogramView(10);
        Scene tmpTestScene = new Scene(
                tmpTestClusterHistogramView,
                GuiDefinitions.GUI_MAIN_VIEW_WIDTH_VALUE,
                GuiDefinitions.GUI_MAIN_VIEW_HEIGHT_VALUE
        );
        Stage tmpTestStage = new Stage();
        tmpTestStage.initOwner(aMainStage);
        tmpTestStage.setScene(tmpTestScene);
        tmpTestStage.show();

    }
    //extend HistogramViewController?
}
