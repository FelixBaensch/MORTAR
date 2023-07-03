package de.unijena.cheminf.mortar.model.clustering;

import de.unijena.cheminf.art2aClustering.clustering.Art2aFloatClustering;
import de.unijena.cheminf.art2aClustering.interfaces.IArt2aClustering;
import de.unijena.cheminf.art2aClustering.interfaces.IArt2aClusteringResult;
import de.unijena.cheminf.fragmentFingerprinter.IFragmentFingerprinter;
import de.unijena.cheminf.mortar.message.Message;
import de.unijena.cheminf.mortar.model.fragmentation.FragmentationService;
import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.CollectionUtil;
import de.unijena.cheminf.mortar.model.util.SimpleEnumConstantNameProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Art2aClusteringAlgorithm implements IFingerprintClustering {
    public static enum PrecisionOption {
        DOUBLE_MACHINE_PRECISION,
        FLOAT_MACHINE_PRECISION;
    }
    public static final String CLUSTERING_NAME = "ART 2-A Clustering";  // ART 2-A Clustering"
    private IArt2aClustering art2aClusteringInstance;
    public static final PrecisionOption FLOAT_MACHINE_PRECISION_DEFAULT = PrecisionOption.FLOAT_MACHINE_PRECISION;
    private SimpleEnumConstantNameProperty machinePrecisionSetting;
    private final SimpleIntegerProperty maximumNumberOfEpochs;
    private final SimpleDoubleProperty similarityParameter;
    private final SimpleDoubleProperty learningParameter;
    private final SimpleIntegerProperty seedValue;
    private  double vigilanceParameter;
    private final double DEFAULT_SIMILARITY_PARAMETER = 0.99;
    private final int DEFAULT_SEED_VALUE = 1;
    private final double DEFAULT_LEARNING_PARAMETER = 0.01;
    private final int DEFAULT_MAX_EPOCHS_NUMBER = 7;
    private final List<Property> settings;
    private final HashMap<String, String> settingNameTooltipTextMap;
    private IArt2aClusteringResult[] clusteringResult;
    private static final Logger LOGGER = Logger.getLogger(Art2aClusteringAlgorithm.class.getName());


    public Art2aClusteringAlgorithm() {
        //this.art2aFloatClusteringInstance = new Art2aFloatClustering(aDataMatrix, DEFAULT_MAX_EPOCHS_NUMBER,aVigilanceParameter, DEFAULT_SIMILARITY_PARAMETER, DEFAULT_LEARNING_PARAMETER);
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
        this.maximumNumberOfEpochs = new SimpleIntegerProperty(this, "Maximum number of epochs", DEFAULT_MAX_EPOCHS_NUMBER) {
            @Override
            public void set(int newValue) { // TODO Test
                super.set(newValue);
            }
        };
        this.settingNameTooltipTextMap.put(this.maximumNumberOfEpochs.getName(), Message.get("Art2aClusteringAlgorithm.maximumNumberOfEpochs.tooltip"));
        this.similarityParameter = new SimpleDoubleProperty(this, "Similarity parameter", DEFAULT_SIMILARITY_PARAMETER) {
            @Override
            public void set(double newValue) { // TODO Test
                super.set( newValue);
            }
        };
        this.settingNameTooltipTextMap.put(this.similarityParameter.getName(), Message.get("Art2aClusteringAlgorithm.similarityParameter.tooltip"));
        this.learningParameter = new SimpleDoubleProperty(this, "Learning parameter", DEFAULT_LEARNING_PARAMETER) {
            @Override
            public void set(double newValue) { // TODO Test
                super.set(newValue);
            }
        };
        this.settingNameTooltipTextMap.put(this.learningParameter.getName(), Message.get("Art2aClusteringAlgorithm.learningParameter.tooltip"));
        this.seedValue = new SimpleIntegerProperty(this, "Seed value",DEFAULT_SEED_VALUE) {
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
    public String getMachinePrecision() {
        return this.machinePrecisionSetting.get();
    }

    public int getMaximumNumberOfEpochs() {
        return this.maximumNumberOfEpochs.get();
    }
    public double getSimilarityParameter() {
        return this.similarityParameter.get();
    }
    public double getLearningParameter() {
        return this.learningParameter.get();
    }
    public int getSeedValue() {
        return this.seedValue.get();
    }

    public void setMaximumNumberOfEpochs(int aMaximumNumberOfEpochs) throws IllegalArgumentException {
        if(aMaximumNumberOfEpochs < 0) {
            throw new IllegalArgumentException("The given value is invalid.");
        }
        this.maximumNumberOfEpochs.set(aMaximumNumberOfEpochs);
    }
    public void setSimilarityParameter(double aSimilarityParameter) throws IllegalArgumentException {
        this.similarityParameter.set( aSimilarityParameter);
    }
    public void setLearningParameter(double aLearningParameter) throws IllegalArgumentException {
        this.learningParameter.set( aLearningParameter);
    }
    public void setMachinePrecisionSetting(String anOptionName) {
        Objects.requireNonNull(anOptionName, "Given option name is null.");
        PrecisionOption tmpConstant = PrecisionOption.valueOf(anOptionName);
        this.setMachinePrecisionSetting(tmpConstant);

    }
    public void setMachinePrecisionSetting(PrecisionOption anOption) {
        Objects.requireNonNull(anOption, "Given option is null.");
        this.machinePrecisionSetting.set(anOption.name());
    }
    public void setSeedValue(int aSeedValue) {
        this.seedValue.set(aSeedValue);
    }
    public double setVigilanceParameter(double vigilanceParameter) {
        return this.vigilanceParameter = vigilanceParameter;

    }

    @Override
    public List<Property> settingsProperties() {
        return this.settings;
    }

    @Override
    public Map<String, String> getSettingNameToTooltipTextMap() {
        return this.settingNameTooltipTextMap;
    }

    @Override
    public String getClusteringName() {
        return Art2aClusteringAlgorithm.CLUSTERING_NAME;
    }

    @Override
    public void restoreDefaultSettings() {
        this.machinePrecisionSetting.set(PrecisionOption.FLOAT_MACHINE_PRECISION.name());
        this.setSimilarityParameter(this.DEFAULT_SIMILARITY_PARAMETER);
        this.setMaximumNumberOfEpochs(this.DEFAULT_MAX_EPOCHS_NUMBER);
        this.setLearningParameter(this.DEFAULT_LEARNING_PARAMETER);
    }

    @Override
    public IFingerprintClustering copy() {
        Art2aClusteringAlgorithm tmpCopy = new Art2aClusteringAlgorithm();
        tmpCopy.setMachinePrecisionSetting(this.machinePrecisionSetting.get());
        tmpCopy.setMaximumNumberOfEpochs(this.maximumNumberOfEpochs.get());
        tmpCopy.setLearningParameter(this.learningParameter.get());
        tmpCopy.setSimilarityParameter(this.similarityParameter.get());
        tmpCopy.setSeedValue(this.seedValue.get());
        tmpCopy.setVigilanceParameter(this.vigilanceParameter);
        return tmpCopy;
    }

    public IArt2aClusteringResult[] startArt2aClustering(int[][] aDataMatrix, int aNumberOfTasks, String aClusteringName) throws InterruptedException {
        int tmpMaximumNumberOfEpochs = this.getMaximumNumberOfEpochs();
        double tmpLearningParameter = this.getLearningParameter();
        double tmpSimilarityParameter = this.getSimilarityParameter();
        int tmpSeedValue = this.getSeedValue();
        String tmpClusteringName = aClusteringName;
        ExecutorService tmpExecutorService = Executors.newFixedThreadPool(aNumberOfTasks); // number of tasks // TODO
        List<ClusteringTask> tmpClusteringTask = new LinkedList<>();
        if(this.getMachinePrecision().equals(Art2aClusteringAlgorithm.PrecisionOption.FLOAT_MACHINE_PRECISION.name())) {
            float[][] tmpFloatDataMatrix = new float[aDataMatrix.length][aDataMatrix[0].length];
            for (int i = 0; i < aDataMatrix.length; i++) {
                int[] tmpRow = aDataMatrix[i];
                for (int j = 0; j < tmpRow.length; j++) {
                    int tmpFingerprintComponentValue = tmpRow[j];
                    float tmpFloatValue = (float) tmpFingerprintComponentValue;
                    tmpFloatDataMatrix[i][j] = tmpFloatValue;
                }
            }
            for (float tmpVigilanceParameter = 0.1f; tmpVigilanceParameter < 1.0f; tmpVigilanceParameter += 0.1f) { //TODO vigilance Parameter
                ClusteringTask tmpART2aFloatClusteringTask = new ClusteringTask(tmpVigilanceParameter,
                        tmpFloatDataMatrix, tmpMaximumNumberOfEpochs, false,
                        (float) tmpSimilarityParameter, (float) tmpLearningParameter);
                tmpART2aFloatClusteringTask.setSeed(tmpSeedValue);
                tmpClusteringTask.add(tmpART2aFloatClusteringTask);
            }
            System.out.println(tmpSimilarityParameter + "-----float similarity");
        } else {
            double[][] tmpDoubleDataMatrix = new double[aDataMatrix.length][aDataMatrix[0].length];
            for(int i = 0; i < aDataMatrix.length; i++) {
                int[] tmpRow = aDataMatrix[i];
                for(int j = 0; j < tmpRow.length; j++) {
                    int tmpFingerprintComponentValue = tmpRow[j];
                    double tmpDoubleValue = (double) tmpFingerprintComponentValue;
                    tmpDoubleDataMatrix[i][j] = tmpDoubleValue;
                }
            }
            for(double tmpVigilanceParameter = 0.1; tmpVigilanceParameter < 1.0; tmpVigilanceParameter += 0.1) {
                ClusteringTask tmpART2aDoubleClusteringTask = new ClusteringTask(tmpVigilanceParameter,
                        tmpDoubleDataMatrix, tmpMaximumNumberOfEpochs, false,
                        tmpSimilarityParameter, tmpLearningParameter);
                tmpART2aDoubleClusteringTask.setSeed(tmpSeedValue);
                tmpClusteringTask.add(tmpART2aDoubleClusteringTask);
            }
        }
        List<Future<IArt2aClusteringResult>> tmpFuturesList;
        tmpFuturesList = tmpExecutorService.invokeAll(tmpClusteringTask);
        IArt2aClusteringResult[] resultArray = new IArt2aClusteringResult[9];
        int i = 0;
        for (Future<IArt2aClusteringResult> tmpFuture : tmpFuturesList) {
            try {
                IArt2aClusteringResult tmpClusteringResult = tmpFuture.get();
                System.out.println(tmpClusteringResult.getVigilanceParameter());
                resultArray[i] = tmpFuture.get();
                i++;
            } catch (RuntimeException anException) {
                throw anException;
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        tmpExecutorService.shutdown();
        Art2aClusteringAlgorithm.LOGGER.info("Clustering \"" + tmpClusteringName + "\" of " + aDataMatrix.length
                + " molecules complete. It took");
        return resultArray;


    }
}
