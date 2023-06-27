package de.unijena.cheminf.mortar.model.clustering;

import de.unijena.cheminf.art2aClustering.interfaces.IArt2aClustering;
import de.unijena.cheminf.art2aClustering.interfaces.IArt2aClusteringResult;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import javafx.beans.property.SimpleStringProperty;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class ClusteringService<T> {
    public static final String DEFAULT_SELECTED_CLUSTERING_ALGORITHM_NAME = Art2aClusteringAlgorithm.CLUSTERING_NAME;
    public static final String CLUSTERING_SETTINGS_SUBFOLDER_NAME = "Clustering_Settings";
    public static final String CLUSTERING_SERVICE_SETTINGS_SUBFOLDER_NAME = "Clustering_Service_Settings";
    public static final String CLUSTERING_SERVICE_SETTINGS_FILE_NAME = "ClusteringServiceSettings";
    public static final String SELECTED_CLUSTERING_ALGORITHM_NAME = "SelectedClusteringAlgorithm";

    IFingerprintClustering[] clusterer;
    float[][] dataMatrix;
    double[][] doubleDataMatrix;
    Art2aClusteringAlgorithm art2aClustering;
    IFingerprintClustering selectedClusteringAlgorithm;
    private SettingsContainer settingsContainer;
    private SimpleStringProperty selectedClusteringAlgorithmNameProperty;
    public ClusteringService(SettingsContainer settingsContainer) {

        this.clusterer = new IFingerprintClustering[1];
        this.art2aClustering = new Art2aClusteringAlgorithm();
        this.clusterer[0] = this.art2aClustering;
        this.settingsContainer = settingsContainer;
        this.selectedClusteringAlgorithmNameProperty = new SimpleStringProperty();
        for(IFingerprintClustering tmpClustering : this.clusterer) {
            if(tmpClustering.getClusteringName().equals(ClusteringService.DEFAULT_SELECTED_CLUSTERING_ALGORITHM_NAME)) {
                this.selectedClusteringAlgorithm = tmpClustering;
            }
        }
        if(Objects.isNull(this.selectedClusteringAlgorithm)) {
            this.selectedClusteringAlgorithm  = this.art2aClustering;
        }
        this.setSelectedClusteringAlgorithmNameProperty(this.selectedClusteringAlgorithm.getClusteringName());
    }
    private IArt2aClusteringResult[] startArt2aClustering(int[][] aDataMatrix, int aNumberOfTasks) throws InterruptedException {
        int tmpMaximumNumberOfEpochs = this.art2aClustering.getMaximumNumberOfEpochs();
        double tmpLearningParameter = this.art2aClustering.getLearningParameter();
        double tmpSimilarityParameter = this.art2aClustering.getSimilarityParameter();
        int tmpSeedValue = this.art2aClustering.getSeedValue();
        ExecutorService tmpExecutorService = Executors.newFixedThreadPool(aNumberOfTasks); // number of tasks // TODO
        List<ClusteringTask> tmpClusteringTask = new LinkedList<>();
        if(this.art2aClustering.getMachinePrecision().equals(Art2aClusteringAlgorithm.PrecisionOption.FLOAT_MACHINE_PRECISION.name())) {
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
        IArt2aClusteringResult[] resultArray = new IArt2aClusteringResult[20];
        int i = 0;
        for (Future<IArt2aClusteringResult> tmpFuture : tmpFuturesList) {
            try {
                IArt2aClusteringResult tmpClusteringResult = tmpFuture.get();
                System.out.println(tmpClusteringResult.getVigilanceParameter());
                resultArray[i] = tmpFuture.get();
                System.out.println(tmpClusteringResult.getNumberOfDetectedClusters());
            } catch (RuntimeException anException) {
                throw anException;
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        tmpExecutorService.shutdown();
        return resultArray;
    }

    public IArt2aClusteringResult[] startClustering(int[][] aDataMatrix, int aNumberOfTasks) throws InterruptedException {
        if(selectedClusteringAlgorithm.getClusteringName().equals("ART 2-A Clustering")) {
            IArt2aClusteringResult[] tmpClusterResults =  this.startArt2aClustering(aDataMatrix, aNumberOfTasks);
            return tmpClusterResults;
        } else {
            return null;
        }
    }
    public IFingerprintClustering[] getClusterer() {
        return this.clusterer;
    }
    public IFingerprintClustering getSelectedClusteringAlgorithm() {
        return this.selectedClusteringAlgorithm;
    }
    public void setSelectedClusteringAlgorithmNameProperty(String aClusteringAlgorithmName) {
        this.selectedClusteringAlgorithmNameProperty.set(aClusteringAlgorithmName);
    }
    public void setSelectedClusteringAlgorithm(String anAlgorithmName){
        for(IFingerprintClustering tmpClusteringAlgorithm : this.clusterer) {
            if(anAlgorithmName.equals(tmpClusteringAlgorithm.getClusteringName())) {
                this.selectedClusteringAlgorithm = tmpClusteringAlgorithm;
            }
        }
    }
}
