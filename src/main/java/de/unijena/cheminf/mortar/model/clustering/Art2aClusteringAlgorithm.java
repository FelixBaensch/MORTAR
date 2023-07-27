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

package de.unijena.cheminf.mortar.model.clustering;

import de.unijena.cheminf.clustering.art2a.Art2aClusteringTask;
import de.unijena.cheminf.clustering.art2a.interfaces.IArt2aClusteringResult;
import de.unijena.cheminf.mortar.gui.util.GuiUtil;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.CollectionUtil;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper class that makes the
 * <a href="https://github.com/JonasSchaub/ART2a-Clustering-for-Java">ART2a-Clustering-for-Java</a> available in MORTAR.
 *
 * @author Betuel Sevindik
 * @version 1.0.0.0
 */
public class Art2aClusteringAlgorithm implements IMortarClustering {
    //<editor-fold desc="Enum FingerprintTyp">
    /**
     * Enum for options concerning the machine precision.
     */
    public static enum PrecisionOption {
        /**
         * double machine precision
         */
        DOUBLE_MACHINE_PRECISION,
        /**
         * single machine precision
         */
        FLOAT_MACHINE_PRECISION;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public static final constants">
    /**
     * Name of clustering algorithm used in this Wrapper class
     */
    public static final String CLUSTERING_ALGORITHM_NAME = "ART 2-A Clustering";
    /**
     * Default machine precision
     */
    public static final PrecisionOption FLOAT_MACHINE_PRECISION_DEFAULT = PrecisionOption.FLOAT_MACHINE_PRECISION;
    //</editor-fold>
    //
    //<editor-fold desc="Private final constants">
    /**
     * Default similarity parameter value
     */
    private final double DEFAULT_SIMILARITY_PARAMETER = 0.99;
    /**
     * Default seed value
     */
    private final int DEFAULT_SEED_VALUE = 1;
    /**
     * Default learning parameter value
     */
    private final double DEFAULT_LEARNING_PARAMETER = 0.01;
    /**
     * Default value of number of epochs
     */
    private final int DEFAULT_MAX_EPOCHS_NUMBER = 10;
    //</editor-fold>
    //
    //<editor-fold desc="Private class variables">
    private ExecutorService executorService;
    //</editor-fold>
    //
    //<editor-fold desc="Private final class variables">
    /**
     * A property that has a constants name from PrecisionOption
     */
    private final SimpleEnumConstantNameProperty machinePrecisionSetting;
    /**
     * Property wrapping the 'Maximum number of epochs' setting of the algorithm
     */
    private final SimpleIntegerProperty maximumNumberOfEpochs;
    /**
     * Property wrapping the 'Similarity parameter' setting of the algorithm
     */
    private final SimpleDoubleProperty similarityParameter;
    /**
     * Property wrapping the 'Learning parameter' setting of the algorithm
     */
    private final SimpleDoubleProperty learningParameter;
    /**
     * Property wrapping the 'Seed value' setting of the algorithm
     */
    private final SimpleIntegerProperty seedValue;
    /**
     * All settings of this fingerprinter, encapsulated in JavaFX properties for binding in GUI.
     */
    private final List<Property> settings;
    /**
     * Map to store pairs of {@literal <setting name, tooltip text>}.
     */
    private final HashMap<String, String> settingNameTooltipTextMap;
    /**
     * Logger of this class
     */
    private final Logger logger = Logger.getLogger(Art2aClusteringAlgorithm.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="Constructor">
    /**
     * Constructor, all settings are initialised with their default values as declared in the respective public constants.
     */
    public Art2aClusteringAlgorithm() {
        int tmpNumberOfSettings = 5;
        this.settings = new ArrayList<>(tmpNumberOfSettings);
        int tmpInitialCapacityForSettingNameToolTipTextMap = CollectionUtil.calculateInitialHashCollectionCapacity(
                tmpNumberOfSettings,
                BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.settingNameTooltipTextMap = new HashMap<>(tmpInitialCapacityForSettingNameToolTipTextMap, BasicDefinitions.DEFAULT_HASH_COLLECTION_LOAD_FACTOR);
        this.machinePrecisionSetting = new SimpleEnumConstantNameProperty(this, "Machine precision option",
                Art2aClusteringAlgorithm.FLOAT_MACHINE_PRECISION_DEFAULT.name(), Art2aClusteringAlgorithm.PrecisionOption.class) {
            @Override
            public void set(String newValue) {
                super.set(newValue);
            }
        };
        this.settingNameTooltipTextMap.put(this.machinePrecisionSetting.getName(), Message.get("Art2aClusteringAlgorithm.machinePrecisionSetting.tooltip"));
        this.maximumNumberOfEpochs = new SimpleIntegerProperty(this, "Maximum number of epochs", this.DEFAULT_MAX_EPOCHS_NUMBER) {
            @Override
            public void set(int newValue) throws IllegalArgumentException {
                if(Art2aClusteringAlgorithm.this.isLegalEpochsNumber(newValue)) {
                    super.set(newValue);
                } else {
                    IllegalArgumentException tmpException = new IllegalArgumentException("An illegal epoch number was given: " + newValue);
                    Art2aClusteringAlgorithm.this.logger.log(Level.WARNING, tmpException.toString(), tmpException);
                    GuiUtil.guiExceptionAlert(Message.get("Art2aClusteringAlgorithm.Error.invalidArgument.Title"),
                            Message.get("Art2aClusteringAlgorithm.Error.invalidArgument.Header"),
                            tmpException.toString(),
                            tmpException);
                    //re-throws the exception to properly reset the binding
                    throw tmpException;
                }
            }
        };
        this.settingNameTooltipTextMap.put(this.maximumNumberOfEpochs.getName(), Message.get("Art2aClusteringAlgorithm.maximumNumberOfEpochs.tooltip"));
        this.similarityParameter = new SimpleDoubleProperty(this, "Similarity parameter", this.DEFAULT_SIMILARITY_PARAMETER) {
            @Override
            public void set(double newValue) throws IllegalArgumentException {
                if(Art2aClusteringAlgorithm.this.isLegalSimilarityAndLearningParameter(newValue)) {
                    super.set(newValue);
                } else {
                    IllegalArgumentException tmpException = new IllegalArgumentException("An illegal similarity parameter was given: " + newValue);
                    Art2aClusteringAlgorithm.this.logger.log(Level.WARNING, tmpException.toString(), tmpException);
                    GuiUtil.guiExceptionAlert(Message.get("Art2aClusteringAlgorithm.Error.invalidArgument.Title"),
                            Message.get("Art2aClusteringAlgorithm.Error.invalidArgument.Header"),
                            tmpException.toString(),
                            tmpException);
                    //re-throws the exception to properly reset the binding
                    throw tmpException;
                }
            }
        };
        this.settingNameTooltipTextMap.put(this.similarityParameter.getName(), Message.get("Art2aClusteringAlgorithm.similarityParameter.tooltip"));
        this.learningParameter = new SimpleDoubleProperty(this, "Learning parameter", this.DEFAULT_LEARNING_PARAMETER) {
            @Override
            public void set(double newValue) throws IllegalArgumentException {
                if(Art2aClusteringAlgorithm.this.isLegalSimilarityAndLearningParameter(newValue)) {
                    super.set(newValue);
                } else {
                    IllegalArgumentException tmpException = new IllegalArgumentException("An illegal learning parameter was given: " + newValue);
                    Art2aClusteringAlgorithm.this.logger.log(Level.WARNING, tmpException.toString(), tmpException);
                    GuiUtil.guiExceptionAlert(Message.get("Art2aClusteringAlgorithm.Error.invalidArgument.Title"),
                            Message.get("Art2aClusteringAlgorithm.Error.invalidArgument.Header"),
                            tmpException.toString(),
                            tmpException);
                    //re-throws the exception to properly reset the binding
                    throw tmpException;
                }
            }
        };
        this.settingNameTooltipTextMap.put(this.learningParameter.getName(), Message.get("Art2aClusteringAlgorithm.learningParameter.tooltip"));
        this.seedValue = new SimpleIntegerProperty(this, "Seed value", this.DEFAULT_SEED_VALUE) {
            @Override
            public void set(int newValue) {
                super.set(newValue);
            }
        };
        this.settingNameTooltipTextMap.put(this.seedValue.getName(), Message.get("Art2aClusteringAlgorithm.seedValue.tooltip"));
        this.settings.add(this.machinePrecisionSetting);
        this.settings.add(this.maximumNumberOfEpochs);
        this.settings.add(this.similarityParameter);
        this.settings.add(this.learningParameter);
        this.settings.add(this.seedValue);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private methods">
    /**
     * Tests whether an integer value would be an allowed argument for the maximum number of epochs setting.
     * For this, it must be positive and non-zero.
     *
     * @param anInteger the integer to test
     * @return true if the given parameter is a legal value for the setting
     */
    private boolean isLegalEpochsNumber(int anInteger) {
        return !(anInteger <= 0);
    }
    //
    /**
     * Tests whether an integer value would be an allowed argument for the similarity and learning parameter setting.
     *
     * @param aDouble the double to test
     * @return true if the given parameter is a legal value for the setting
     */
    private boolean isLegalSimilarityAndLearningParameter(double aDouble) {
        return !(aDouble < 0.0 || aDouble > 1.0);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties get">
    /**
     * Returns the string representation of the currently set option for the machine precision
     *
     * @return enum constant name of the set option
     */
    public String getMachinePrecision() {
        return this.machinePrecisionSetting.get();
    }
    //
    /**
     * Returns the integer value of the maximum epoch number
     *
     * @return max epoch number
     */
    public int getMaximumNumberOfEpochs() {
        return this.maximumNumberOfEpochs.get();
    }
    //
    /**
     * Returns the double value of the similarity parameter
     *
     * @return similarity parameter value
     */
    public double getSimilarityParameter() {
        return this.similarityParameter.get();
    }
    //
    /**
     * Returns the double value of the learning parameter
     *
     * @return learning parameter value
     */
    public double getLearningParameter() {
        return this.learningParameter.get();
    }
    //
    /**
     * Returns the integer value of the seed value
     *
     * @return seed value
     */
    public int getSeedValue() {
        return this.seedValue.get();
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties set">
    /**
     * Sets the maximum number of epochs
     *
     * @param aMaximumNumberOfEpochs number of epochs
     */
    public void setMaximumNumberOfEpochs(int aMaximumNumberOfEpochs){
        this.maximumNumberOfEpochs.set(aMaximumNumberOfEpochs);
    }
    //
    /**
     * Sets the similarity parameter value
     *
     * @param aSimilarityParameter value
     */
    public void setSimilarityParameter(double aSimilarityParameter) {
        this.similarityParameter.set( aSimilarityParameter);
    }
    //
    /**
     * Sets the learning parameter value
     *
     * @param aLearningParameter value
     */
    public void setLearningParameter(double aLearningParameter)  {
        this.learningParameter.set( aLearningParameter);
    }
    //
    /**
     * Sets the seed value
     *
     * @param aSeedValue seed value
     */
    public void setSeedValue(int aSeedValue) {
        this.seedValue.set(aSeedValue);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Overriden public methods">
    /**
     * {@inheritDoc}
     */
    @Override
    public List<Property> settingsProperties() {
        return this.settings;
    }
    //
    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getSettingNameToTooltipTextMap() {
        return this.settingNameTooltipTextMap;
    }
    //
    /**
     * {@inheritDoc}
     */
    @Override
    public String getClusteringName() {
        return Art2aClusteringAlgorithm.CLUSTERING_ALGORITHM_NAME;
    }
    //
    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreDefaultSettings() {
        this.machinePrecisionSetting.set(PrecisionOption.FLOAT_MACHINE_PRECISION.name());
        this.setSimilarityParameter(this.DEFAULT_SIMILARITY_PARAMETER);
        this.setMaximumNumberOfEpochs(this.DEFAULT_MAX_EPOCHS_NUMBER);
        this.setLearningParameter(this.DEFAULT_LEARNING_PARAMETER);
        this.setSeedValue(this.DEFAULT_SEED_VALUE);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public method">
    /**
     * Starts ART 2-A clustering, depending on the selected machine precision
     *
     * @param aDataMatrix with all generated fingerprints
     * @param aNumberOfTasks number of tasks
     * @param aClusteringName clustering name
     * @return clustering results // TODO result wrapper class
     * @throws Exception is thrown if clustering failed
     */
    public IArt2aClusteringResult[] startArt2aClustering(int[][] aDataMatrix, int aNumberOfTasks, String aClusteringName) throws Exception {
        Objects.requireNonNull(aDataMatrix, "aDataMatrix is null");
        int tmpMaximumNumberOfEpochs = this.getMaximumNumberOfEpochs();
        double tmpLearningParameter = this.getLearningParameter();
        double tmpSimilarityParameter = this.getSimilarityParameter();
        int tmpSeedValue = this.getSeedValue();
        String tmpClusteringName = aClusteringName;
        this.executorService = Executors.newFixedThreadPool(aNumberOfTasks);
        List<Art2aClusteringTask> tmpClusteringTask = new LinkedList<>();
        if (this.getMachinePrecision().equals(Art2aClusteringAlgorithm.PrecisionOption.FLOAT_MACHINE_PRECISION.name())) {
            float[][] tmpFloatDataMatrix = new float[aDataMatrix.length][aDataMatrix[0].length];
            for (int i = 0; i < aDataMatrix.length; i++) {
                int[] tmpRow = aDataMatrix[i];
                for (int j = 0; j < tmpRow.length; j++) {
                    int tmpFingerprintComponentValue = tmpRow[j];
                    float tmpFloatValue = (float) tmpFingerprintComponentValue;
                    tmpFloatDataMatrix[i][j] = tmpFloatValue;
                }
            }
            for (float tmpVigilanceParameter = 0.1f; tmpVigilanceParameter < 1.0f; tmpVigilanceParameter += 0.1f) {
                Art2aClusteringTask tmpART2aFloatClusteringTask = new Art2aClusteringTask(tmpVigilanceParameter,
                        tmpFloatDataMatrix, tmpMaximumNumberOfEpochs, false,
                        (float) tmpSimilarityParameter, (float) tmpLearningParameter);
                tmpART2aFloatClusteringTask.setSeed(tmpSeedValue);
                tmpClusteringTask.add(tmpART2aFloatClusteringTask);
            }
        } else {
            double[][] tmpDoubleDataMatrix = new double[aDataMatrix.length][aDataMatrix[0].length];
            for (int i = 0; i < aDataMatrix.length; i++) {
                int[] tmpRow = aDataMatrix[i];
                for (int j = 0; j < tmpRow.length; j++) {
                    int tmpFingerprintComponentValue = tmpRow[j];
                    double tmpDoubleValue = (double) tmpFingerprintComponentValue;
                    tmpDoubleDataMatrix[i][j] = tmpDoubleValue;
                }
            }
            for (double tmpVigilanceParameter = 0.1; tmpVigilanceParameter < 0.9; tmpVigilanceParameter += 0.1) {
                Art2aClusteringTask tmpART2aDoubleClusteringTask = new Art2aClusteringTask(tmpVigilanceParameter,
                        tmpDoubleDataMatrix, tmpMaximumNumberOfEpochs, false,
                        tmpSimilarityParameter, tmpLearningParameter);
                tmpART2aDoubleClusteringTask.setSeed(tmpSeedValue);
                tmpClusteringTask.add(tmpART2aDoubleClusteringTask);
            }
        }
        List<Future<IArt2aClusteringResult>> tmpFuturesList;
        tmpFuturesList = this.executorService.invokeAll(tmpClusteringTask);
        IArt2aClusteringResult[] tmpResultArray = new IArt2aClusteringResult[9];
        int tmpIterator = 0;
            for (Future<IArt2aClusteringResult> tmpFuture : tmpFuturesList) {
                IArt2aClusteringResult tmpClusteringResult = tmpFuture.get();
                System.out.println(tmpClusteringResult.getVigilanceParameter() + "------vigilance parameter");
                tmpResultArray[tmpIterator] = tmpFuture.get();
                tmpIterator++;
            }
            this.executorService.shutdown();
            this.logger.info("Clustering \"" + tmpClusteringName + "\" of " + aDataMatrix.length
                    + " molecules complete.");
        return tmpResultArray;
    }
    //
    /**
     * Shuts down executor service
     */
    public void abortExecutor() {
        this.executorService.shutdown();
        try {
            if (!this.executorService.awaitTermination(600, TimeUnit.MILLISECONDS)) {
                this.executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            this.executorService.shutdownNow();
        }
    }
    //</editor-fold>
}
