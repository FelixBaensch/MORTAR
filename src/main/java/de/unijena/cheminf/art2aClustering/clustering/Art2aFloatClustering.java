/*
 * GNU General Public License v3.0
 *
 * Copyright (c) 2023 Betuel Sevindik, Felix Baensch, Jonas Schaub, Christoph Steinbeck, and Achim Zielesny
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

package de.unijena.cheminf.art2aClustering.clustering;

import de.unijena.cheminf.art2aClustering.exceptions.ConvergenceFailedException;
import de.unijena.cheminf.art2aClustering.interfaces.IArt2aClustering;
import de.unijena.cheminf.art2aClustering.interfaces.IArt2aClusteringResult;
import de.unijena.cheminf.art2aClustering.results.Art2aFloatClusteringResult;

import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 * The class implements an Art-2A algorithm in single machine precision for fast,
 * stable unsupervised clustering for open categorical problems. The class is primarily intended for the
 * clustering of fingerprints. <br>
 * LITERATURE SOURCE:<br>
 *
 * @see <a href="https://www.sciencedirect.com/science/article/abs/pii/0893608091900457">
 *     "Primary : G.A. Carpenter,S. Grossberg and D.B. Rosen, Neural Networks 4 (1991) 493-504"</a> <br><br>
 *      <a href="https://www.sciencedirect.com/science/article/abs/pii/0169743994850542">
 *          "Secondary : D. Wienke et al., Chemometrics and Intelligent Laboratory Systems 24 (1994) 367-387"</a>
 *
 * @author Betuel Sevindik
 * @version 1.0.0.0
 */
public class Art2aFloatClustering implements IArt2aClustering {
    //<editor-fold desc="private class variables" defaultstate="collapsed">
    /**
     * Matrix with all fingerprints to be clustered.
     * Each row of the matrix represents a fingerprint.
     */
    private float[][] dataMatrix;
    /**
     * Matrix contains all cluster vectors.
     */
    private float[][] clusterMatrix;
    /**
     * Matrix contains all cluster vectors of previous epoch. Is needed to check the convergence of
     * the system.
     */
    private float[][] clusterMatrixPreviousEpoch;
    /**
     * Queue of typ String for clustering process.
     */
    private ConcurrentLinkedQueue<String> clusteringProcess;
    /**
     * Queue of typ String for clustering result.
     */
    private ConcurrentLinkedQueue<String> clusteringResult;
    /**
     * The seed value for permutation of the vector field.
     */
    private int seed;
    //</editor-fold>
    //
    //<editor-fold desc="private final variables" defaultstate="collapsed">
    /**
     * Maximum number of epochs the system may need to converge.
     */
    private int maximumNumberOfEpochs;
    /**
     * The vigilance parameter is between 0 and 1. The parameter influences the type of clustering.
     * A vigilance parameter close to 0 leads to a coarse clustering (few clusters) and a vigilance
     * parameter close to 1, on the other hand, leads to a fine clustering (many clusters).
     */
    private float vigilanceParameter;
    /**
     * Threshold for contrast enhancement. If a vector/fingerprint component is below the threshold, it is set to zero.
     */
    private final float thresholdForContrastEnhancement;
    /**
     * Number of fingerprints to be clustered.
     */
    private final int numberOfFingerprints;
    /**
     * Dimensionality of the fingerprint.
     */
    private final int numberOfComponents;
    /**
     * The scaling factor controls the sensitivity of the algorithm to new inputs. A low scaling factor makes
     * the algorithm more sensitive to new inputs, while a high scaling factor decreases the sensitivity.
     * Thus, too low a scaling factor will cause a new input to be added to a new cluster, while
     * a high scaling factor will cause the new inputs to be added to existing clusters.<br>
     * Default value:  1 / Math.sqrt(numberOfComponents - 1)
     */
    private final float scalingFactor;
    /**
     * The required similarity parameter represents the minimum value that must exist between the current
     * cluster vector and the previous cluster vector for the system to be considered convergent.
     * The clustering process continues until there are no more significant changes between
     * the cluster vectors of the current epoch and the previous epoch.
     */
    private final float requiredSimilarity;
    /**
     * Parameter to define the intensity of keeping the old cluster vector in mind
     * before the system adapts it to the new sample vector.
     */
    private final float learningParameter;
    //</editor-fold>
    //
    // <editor-fold defaultstate="collapsed" desc="private static final constants">
    /**
     * Logger of this class
     */
    private static final Logger LOGGER = Logger.getLogger(Art2aFloatClustering.class.getName());
    //</editor-fold>
    //
    // <editor-fold defaultstate="collapsed" desc="constructor">
    /**
     * Constructor.
     * The data matrix with the input vectors/fingerprints is checked for correctness. Each row of the matrix
     * corresponds to an input vector/fingerprint. The vectors must not have components smaller than 0.
     * All input vectors must have the same length.
     * If there are components greater than 1, these input vectors are scaled so that all vector components
     * are between 0 and 1.
     * <br>
     * <u>WARNING</u>: If the data matrix consists only of null vectors, no clustering is possible,
     * because they do not contain any information that can be used for similarity evaluation.
     *
     * @param aDataMatrix matrix contains all inputs for clustering.
     * @param aMaximumNumberOfEpochs maximum number of epochs that the system may use for convergence.
     * @param aVigilanceParameter parameter to influence the number of clusters.
     * @param aRequiredSimilarity parameter indicating the minimum similarity between the current
     * cluster vectors and the previous cluster vectors.
     * @param aLearningParameter parameter to define the intensity of keeping the old class vector in mind
     * before the system adapts it to the new sample vector.
     * @throws IllegalArgumentException is thrown if the given arguments are invalid.
     * @throws NullPointerException is thrown if aDataMatrix is null.
     *
     */
    public Art2aFloatClustering(float[][] aDataMatrix, int aMaximumNumberOfEpochs, float aVigilanceParameter,
                                float aRequiredSimilarity, float aLearningParameter)
            throws IllegalArgumentException, NullPointerException {
        if(aDataMatrix == null) {
            throw new NullPointerException("aDataMatrix is null.");
        }
        if(aMaximumNumberOfEpochs <= 0) {
            throw new IllegalArgumentException("Number of epochs must be at least greater than zero.");
        }
        if(aVigilanceParameter <= 0.0f || aVigilanceParameter >= 1.0f) {
            throw new IllegalArgumentException("The vigilance parameter must be greater than 0 and smaller than 1.");
        }
        if(aRequiredSimilarity < 0.0f || aRequiredSimilarity > 1.0f) {
            throw new IllegalArgumentException("The required similarity parameter must be between 0 and 1");
        }
        if(aLearningParameter < 0.0f || aLearningParameter > 1.0f) {
            throw new IllegalArgumentException("The learning parameter must be greater than 0 and smaller than 1.");
        }
        this.vigilanceParameter = aVigilanceParameter;
        this.requiredSimilarity = aRequiredSimilarity;
        this.learningParameter = aLearningParameter;
        this.dataMatrix =  this.getCheckedAndScaledDataMatrix(aDataMatrix);
        this.numberOfFingerprints = this.dataMatrix.length;
        this.maximumNumberOfEpochs = aMaximumNumberOfEpochs;
        this.numberOfComponents = this.dataMatrix[0].length;
        this.scalingFactor = (float) (1.0 / Math.sqrt(this.numberOfComponents + 1.0));
        this.thresholdForContrastEnhancement = (float) (1.0 / Math.sqrt(this.numberOfComponents + 1.0));
    }
    //</editor-fold>
    //
    // <editor-fold defaultstate="collapsed" desc="private methods">
    /**
     * The input data matrix with the input vectors/fingerprints is checked for correctness.
     * Accordingly, the input matrix must not contain any vectors that consist of components smaller than 0.
     * All input vectors must have the same length. Components larger than 1 are allowed, but are scaled in the
     * following steps so that all components of an input vector range between 0 and 1.
     *
     * @param aDataMatrix the matrix contains all input vectors/fingerprints to be clustered.
     * @return a valid data matrix.
     * @throws NullPointerException is thrown if the given data matrix is null.
     * @throws IllegalArgumentException is thrown if the input vectors are invalid.
     */
    private float[][] getCheckedAndScaledDataMatrix(float[][] aDataMatrix) throws NullPointerException, IllegalArgumentException {
        if(aDataMatrix == null) {
            throw new IllegalArgumentException("aDataMatrix is null.");
        }
        if(aDataMatrix.length <= 0) {
            throw new IllegalArgumentException("The number of vectors must greater than 0 to cluster inputs.");
        }
        int tmpNumberOfNullComponentsInDataMatrix = 0;
        int tmpNumberOfElementsInDataMatrix = aDataMatrix.length * aDataMatrix[0].length;
        int tmpNumberOfVectorComponents = aDataMatrix[0].length;
        float tmpMaxValueInDataMatrix = aDataMatrix[0][0];
        float tmpMinValueInDatamatrix = aDataMatrix[0][0];
        float tmpCurrentVectorComponent;
        float[] tmpSingleFingerprint;
        boolean tmpIsDataMatrixInCorrectRangeOfValues = false;
        for(int i = 0; i < aDataMatrix.length; i++) {
            tmpSingleFingerprint = aDataMatrix[i];
            if(tmpNumberOfVectorComponents != tmpSingleFingerprint.length) {
                throw new IllegalArgumentException("The input vectors must be have the same length!");
            }
            for(int j = 0; j < tmpSingleFingerprint.length; j++) {
                tmpCurrentVectorComponent = tmpSingleFingerprint[j];
                if(tmpCurrentVectorComponent > tmpMaxValueInDataMatrix) {
                    tmpMaxValueInDataMatrix = tmpCurrentVectorComponent;
                }
                if(tmpCurrentVectorComponent < tmpMinValueInDatamatrix) {
                    tmpMinValueInDatamatrix = tmpCurrentVectorComponent;
                }
                if(tmpCurrentVectorComponent > 1.0f) {
                    tmpIsDataMatrixInCorrectRangeOfValues = true;
                }
                if(tmpCurrentVectorComponent < 0.0f) {
                    throw new IllegalArgumentException("Only positive values allowed.");
                }
                if(tmpCurrentVectorComponent == 0.0f) {
                    tmpNumberOfNullComponentsInDataMatrix++;
                }
            }
            if(tmpNumberOfNullComponentsInDataMatrix == tmpNumberOfElementsInDataMatrix) {
                throw new IllegalArgumentException("All vectors are null vectors. Clustering not possible.");
            }
        }
        if(tmpIsDataMatrixInCorrectRangeOfValues) {
           this.getScaledDataMatrix(aDataMatrix, tmpMinValueInDatamatrix, tmpMaxValueInDataMatrix);
        }
        return aDataMatrix;
    }
    //
    /**
     * Method for scaling the input vectors/fingerprints if they are not between 0 and 1.
     * Thus serves for the scaling of count fingerprints.
     *
     * @param aDataMatrixToScale the matrix contains input vectors and at least one component
     * of an input vector is not in the specified range, i.e. between 0 and 1.
     * @param aMaxValue is the highest value in the matrix.
     * @param aMinValue is the lowest value in the matrix.
     *
     */
    private void getScaledDataMatrix(float[][] aDataMatrixToScale, float aMinValue, float aMaxValue) {
        float[] tmpSingleFingerprint;
        float tmpScaledVectorComponent;
        float tmpCurrentVectorComponent;
        for(int i = 0; i < aDataMatrixToScale.length; i++) {
            tmpSingleFingerprint = aDataMatrixToScale[i];
            for (int j = 0; j < tmpSingleFingerprint.length; j++) {
                tmpCurrentVectorComponent = tmpSingleFingerprint[j];
                tmpScaledVectorComponent = (tmpCurrentVectorComponent-aMinValue)/(aMaxValue-aMinValue); // normalization
                tmpSingleFingerprint[j] = tmpScaledVectorComponent;
                aDataMatrixToScale[i] = tmpSingleFingerprint;
            }
        }
    }
    //
    /**
     * Calculates the length of a vector. The length is needed for the normalisation of the vector.
     *
     * @param anInputVector vector whose length is calculated.
     * @return float vector length.
     * @throws ArithmeticException is thrown if the addition of the vector components results in zero.
     */
    private float getVectorLength (float[] anInputVector) throws ArithmeticException {
        float tmpVectorComponentsSqrtSum = 0.0f;
        float tmpVectorLength;
        for (int i = 0; i < anInputVector.length; i++) {
            tmpVectorComponentsSqrtSum += anInputVector[i] * anInputVector[i];
        }
        if (tmpVectorComponentsSqrtSum == 0.0f) {
            throw new ArithmeticException("Addition of the vector components results in zero!");
        } else {
            tmpVectorLength = (float) Math.sqrt(tmpVectorComponentsSqrtSum);
        }
        return tmpVectorLength;
    }
    //
    /**
     * At the end of each epoch, it is checked whether the system has converged or not. If the system has not
     * converged, a new epoch is performed, otherwise the clustering is completed successfully.
     * The system is considered converged if the cluster vectors of the current epoch and the previous epoch
     * have a minimum similarity. The default value of the similarity parameter is 0.99, but it can also be set
     * by the user when initialising the clustering.
     *
     * @param aNumberOfDetectedClasses number of detected clusters per epoch.
     * @param aConvergenceEpoch current epochs number.
     * @return boolean true is returned if the system has converged.
     * False is returned if the system has not converged to the epoch.
     * @throws ConvergenceFailedException is thrown, when convergence fails.
     */
    private boolean isConverged(int aNumberOfDetectedClasses, int aConvergenceEpoch) throws ConvergenceFailedException {
        boolean tmpIsConverged;
        float[] tmpRow;
        if(aConvergenceEpoch < this.maximumNumberOfEpochs) {
            // Check convergence by evaluating the similarity of the cluster vectors of this and the previous epoch.
            tmpIsConverged = true;
            float tmpScalarProductOfClusterVector;
            float[] tmpCurrentRowInClusterMatrix;
            float[] tmpPreviousEpochRow;
            for (int i = 0; i < aNumberOfDetectedClasses; i++) {
                tmpScalarProductOfClusterVector = 0.0f;
                tmpCurrentRowInClusterMatrix = this.clusterMatrix[i];
                tmpPreviousEpochRow = this.clusterMatrixPreviousEpoch[i];
                for (int j = 0; j < this.numberOfComponents; j++) {
                    tmpScalarProductOfClusterVector += tmpCurrentRowInClusterMatrix[j] * tmpPreviousEpochRow[j];
                }
                if (tmpScalarProductOfClusterVector < this.requiredSimilarity) {
                    tmpIsConverged = false;
                    break;
                }
            }
            if(!tmpIsConverged) {
                for(int tmpCurrentClusterMatrixVector = 0; tmpCurrentClusterMatrixVector < this.clusterMatrix.length;
                    tmpCurrentClusterMatrixVector++) {
                    tmpRow = this.clusterMatrix[tmpCurrentClusterMatrixVector];
                    this.clusterMatrixPreviousEpoch[tmpCurrentClusterMatrixVector] = tmpRow;
                }
            }
        } else {
            throw new ConvergenceFailedException(String.format("Convergence failed for vigilance parameter: %2f",this.vigilanceParameter));
        }
        return tmpIsConverged;
    }
    //</editor-fold>
    //
    // <editor-fold defaultstate="collapsed" desc="overriden public methods">
    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeMatrices() {
        this.clusterMatrix = new float[this.numberOfFingerprints][this.numberOfComponents];
        this.clusterMatrixPreviousEpoch = new float[this.numberOfFingerprints][this.numberOfComponents];
    }
    //
    /**
     * {@inheritDoc}
     *
     * @author Thomas Kuhn
     */
    @Override
    public int[] getRandomizeVectorIndices() {
        int[] tmpSampleVectorIndicesInRandomOrder = new int[this.numberOfFingerprints];
        for(int i = 0; i < this.numberOfFingerprints; i++) {
            tmpSampleVectorIndicesInRandomOrder[i] = i;
        }
        Random tmpRnd = new Random(this.seed);
        this.seed++;
        int tmpNumberOfIterations = (this.numberOfFingerprints >> 1) + 1;
        int tmpRandomIndex1;
        int tmpRandomIndex2;
        int tmpBuffer;
        for(int j = 0; j < tmpNumberOfIterations; j++) {
            tmpRandomIndex1 = (int) (this.numberOfFingerprints * tmpRnd.nextDouble());
            tmpRandomIndex2 = (int) (this.numberOfFingerprints * tmpRnd.nextDouble());
            tmpBuffer = tmpSampleVectorIndicesInRandomOrder[tmpRandomIndex1];
            tmpSampleVectorIndicesInRandomOrder[tmpRandomIndex1] = tmpSampleVectorIndicesInRandomOrder[tmpRandomIndex2];
            tmpSampleVectorIndicesInRandomOrder[tmpRandomIndex2] = tmpBuffer;
        }
        return tmpSampleVectorIndicesInRandomOrder;
    }
    //
    /**
     * {@inheritDoc}
     *
     */
    @Override
    public IArt2aClusteringResult getClusterResult(boolean anIsClusteringResultExported, int aSeedValue) throws ConvergenceFailedException {
        //<editor-fold desc="Initialization steps for writing the clustering results in text files if aAddResultLog == true" defaultstate="collapsed">
        this.clusteringProcess = null;
        this.clusteringResult = null;
        if(anIsClusteringResultExported) {
            this.clusteringProcess = new ConcurrentLinkedQueue<>();
            this.clusteringResult = new ConcurrentLinkedQueue<>();
        }
        //</editor-fold>
        //<editor-fold desc="Initialization and declaration of some important variables" defaultstate="collapsed">
        this.initializeMatrices();
        this.seed = aSeedValue;
        float[] tmpClusterMatrixRow;
        float[] tmpClusterMatrixRowOld;
        float tmpInitialClusterVectorWeightValue = (float) (1.0 / Math.sqrt(this.numberOfComponents));
        int tmpNumberOfDetectedClusters = 0;
        int[] tmpClusterOccupation = new int[this.numberOfFingerprints];
        float tmpVectorLengthForFirstNormalizationStep;
        float tmpVectorLengthAfterContrastEnhancement;
        float tmpRho;
        float tmpVectorLengthForModificationWinnerCluster;
        int tmpWinnerClusterIndex;
        boolean tmpIsSystemConverged = false;
        // </editor-fold>
        //<editor-fold desc="Filling the cluster matrix with the calculated initial weight." defaultstate="collapsed">
        for(int tmpCurrentClusterMatrixVectorIndex = 0; tmpCurrentClusterMatrixVectorIndex < this.clusterMatrix.length;
            tmpCurrentClusterMatrixVectorIndex++) {
            tmpClusterMatrixRow = this.clusterMatrix[tmpCurrentClusterMatrixVectorIndex];
            tmpClusterMatrixRowOld = this.clusterMatrixPreviousEpoch[tmpCurrentClusterMatrixVectorIndex];
            for (int tmpCurrentVectorComponentsInClusterMatrix = 0; tmpCurrentVectorComponentsInClusterMatrix < tmpClusterMatrixRow.length;
                 tmpCurrentVectorComponentsInClusterMatrix++) {
                tmpClusterMatrixRow[tmpCurrentVectorComponentsInClusterMatrix] = tmpInitialClusterVectorWeightValue;
                tmpClusterMatrixRowOld[tmpCurrentVectorComponentsInClusterMatrix] = tmpInitialClusterVectorWeightValue;
            }
        }
        //</editor-fold>
        //<editor-fold desc="Clustering results in text file set up." defaultstate="collapsed">
        int tmpCurrentNumberOfEpochs = 0;
        if(anIsClusteringResultExported) {
            this.clusteringResult.add(String.format("Vigilance parameter: %2f", this.vigilanceParameter));
        }
        //</editor-fold>
        //<editor-fold desc="Start clustering process." defaultstate="collapsed">
        while(!tmpIsSystemConverged && tmpCurrentNumberOfEpochs <= this.maximumNumberOfEpochs) {
            //<editor-fold desc="Randomization input vectors and start saving the clustering results to text files if desired." defaultstate="collapsed">
            if(anIsClusteringResultExported) {
                this.clusteringProcess.add(String.format("Art-2a clustering result for vigilance parameter: %2f",this.vigilanceParameter));
                this.clusteringProcess.add(String.format("Number of epochs: %d",tmpCurrentNumberOfEpochs));
                this.clusteringProcess.add("");
            }
            int[] tmpSampleVectorIndicesInRandomOrder = this.getRandomizeVectorIndices();
            //</editor-fold>
            //<editor-fold desc="Check current input vector for null vector." defaultstate="collapsed">
            for(int tmpCurrentInput = 0; tmpCurrentInput < this.numberOfFingerprints; tmpCurrentInput++) {
                float[] tmpInputVector = new float[this.numberOfComponents];
                boolean tmpIsNullVector = true;
                for(int tmpCurrentInputVectorComponentsIndex = 0; tmpCurrentInputVectorComponentsIndex < this.numberOfComponents;
                    tmpCurrentInputVectorComponentsIndex++) {
                    tmpInputVector[tmpCurrentInputVectorComponentsIndex] =
                            this.dataMatrix[tmpSampleVectorIndicesInRandomOrder[tmpCurrentInput]][tmpCurrentInputVectorComponentsIndex];
                    if(tmpInputVector[tmpCurrentInputVectorComponentsIndex] !=0.0f) {
                        tmpIsNullVector = false;
                    }
                }
                if(anIsClusteringResultExported) {
                    this.clusteringProcess.add(String.format("Input: %d / Vector %d", tmpCurrentInput,
                            tmpSampleVectorIndicesInRandomOrder[tmpCurrentInput]));
                }
                //<editor-fold desc="If the input vector is a null vector, it will not be clustered." defaultstate="collapsed">
                if(tmpIsNullVector) {
                    tmpClusterOccupation[tmpSampleVectorIndicesInRandomOrder[tmpCurrentInput]] = -1;
                    if(anIsClusteringResultExported) {
                        this.clusteringProcess.add("This input is a null vector");
                    }
                }
                //</editor-fold>
                else {
                    //<editor-fold desc=" normalisation of the randomly selected input vector.
                    //                    Subsequently, all components of the input vector are transformed
                    //                    with a non-linear threshold function for contrast enhancement." defaultstate="collapsed">
                    tmpVectorLengthForFirstNormalizationStep = this.getVectorLength(tmpInputVector);
                    for(int tmpManipulateComponentsIndex = 0; tmpManipulateComponentsIndex < tmpInputVector.length;
                        tmpManipulateComponentsIndex++) {
                        tmpInputVector[tmpManipulateComponentsIndex] *= (1.0f / tmpVectorLengthForFirstNormalizationStep);
                        if(tmpInputVector[tmpManipulateComponentsIndex] <= this.thresholdForContrastEnhancement) {
                            tmpInputVector[tmpManipulateComponentsIndex] = 0.0f;
                        }
                    }
                    //</editor-fold>
                    //<editor-fold desc="the transformed input vector is normalised again." defaultstate="collapsed">
                    tmpVectorLengthAfterContrastEnhancement = this.getVectorLength(tmpInputVector);
                    for(int tmpNUmberOfNormalizedInputComponents = 0; tmpNUmberOfNormalizedInputComponents < tmpInputVector.length;
                        tmpNUmberOfNormalizedInputComponents++) {
                        tmpInputVector[tmpNUmberOfNormalizedInputComponents] *= (1.0f / tmpVectorLengthAfterContrastEnhancement);
                    }
                    //</editor-fold>
                    //<editor-fold desc="First pass, no clusters available, so the first cluster is created." defaultstate="collapsed">
                    if(tmpNumberOfDetectedClusters == 0) {
                        this.clusterMatrix[0] = tmpInputVector;
                        tmpClusterOccupation[tmpSampleVectorIndicesInRandomOrder[tmpCurrentInput]] =
                                tmpNumberOfDetectedClusters;
                        tmpNumberOfDetectedClusters++;
                        if(anIsClusteringResultExported) {
                            this.clusteringProcess.add("Cluster number: 0");
                            this.clusteringProcess.add(String.format("Number of detected clusters: %d",tmpNumberOfDetectedClusters));
                        }
                    }
                    //</editor-fold>
                    else {
                        //<editor-fold desc="Cluster number is greater than or equal to 1, so a rho winner is determined as shown in the following steps." defaultstate="collapsed">
                        float tmpSumOfComponents = 0.0f;
                        for(float tmpVectorComponentsOfNormalizeVector : tmpInputVector) {
                            tmpSumOfComponents += tmpVectorComponentsOfNormalizeVector;
                        }
                        tmpWinnerClusterIndex = tmpNumberOfDetectedClusters;
                        boolean tmpIsMatchingClusterAvailable = true;
                        //<editor-fold desc="Cluster number is greater than or equal to 1, so a rho winner is determined as shown in the following steps."
                        //</editor-fold>
                        //<editor-fold desc="Calculate first rho value."
                        tmpRho = this.scalingFactor * tmpSumOfComponents;
                        //</editor-fold>
                        //<editor-fold desc="Calculation of the 2nd rho value and comparison of the two rho values to determine the rho winner."
                        for(int tmpCurrentClusterMatrixRowIndex = 0; tmpCurrentClusterMatrixRowIndex < tmpNumberOfDetectedClusters;
                            tmpCurrentClusterMatrixRowIndex++) {
                            float[] tmpRow;
                            float tmpRhoForExistingClusters = 0.0f;
                            tmpRow = this.clusterMatrix[tmpCurrentClusterMatrixRowIndex];
                            for(int tmpElementsInRowIndex = 0; tmpElementsInRowIndex < this.numberOfComponents; tmpElementsInRowIndex++) {
                                tmpRhoForExistingClusters += tmpInputVector[tmpElementsInRowIndex] * tmpRow[tmpElementsInRowIndex];
                            }
                            if(tmpRhoForExistingClusters > tmpRho) {
                                tmpRho = tmpRhoForExistingClusters;
                                tmpWinnerClusterIndex = tmpCurrentClusterMatrixRowIndex;
                                tmpIsMatchingClusterAvailable = false;
                            }
                        }
                        //</editor-fold>
                        //<editor-fold desc="Deciding whether the input fits into an existing cluster or whether a new cluster must be formed."
                        //</editor-fold>
                        //<editor-fold desc="Input does not fit in existing clusters. A new cluster is formed and the input vector is put into the new cluster vector."
                        if(tmpIsMatchingClusterAvailable || tmpRho < this.vigilanceParameter) {
                            tmpNumberOfDetectedClusters++;
                            tmpClusterOccupation[tmpSampleVectorIndicesInRandomOrder[tmpCurrentInput]] =
                                    tmpNumberOfDetectedClusters - 1;
                            this.clusterMatrix[tmpNumberOfDetectedClusters - 1] = tmpInputVector;
                            if(anIsClusteringResultExported) {
                                this.clusteringProcess.add(String.format("Cluster number: %d",(tmpNumberOfDetectedClusters - 1)));
                                this.clusteringProcess.add(String.format("Number of detected clusters: %d",tmpNumberOfDetectedClusters));
                            }
                        }
                        //</editor-fold>
                        //<editor-fold desc="The input fits into one cluster. The number of clusters is not increased. But the winning cluster vector is modified."
                        else {
                            for(int i = 0; i < this.numberOfComponents; i++) {
                                if(this.clusterMatrix[tmpWinnerClusterIndex][i] <= this.thresholdForContrastEnhancement) {
                                    tmpInputVector[i] = 0.0f;
                                }
                            }
                            //<editor-fold desc="Modification of the winner cluster vector."
                            float tmpVectorLength = this.getVectorLength(tmpInputVector);
                            float tmpFactor1 = this.learningParameter / tmpVectorLength;
                            float tmpFactor2 = 1.0f - this.learningParameter;
                            for(int tmpAdaptedComponentsIndex = 0; tmpAdaptedComponentsIndex < this.numberOfComponents;
                                tmpAdaptedComponentsIndex++) {
                                tmpInputVector[tmpAdaptedComponentsIndex] = tmpInputVector[tmpAdaptedComponentsIndex] *
                                        tmpFactor1 + tmpFactor2 *
                                        this.clusterMatrix[tmpWinnerClusterIndex][tmpAdaptedComponentsIndex];
                            }
                            tmpVectorLengthForModificationWinnerCluster = this.getVectorLength(tmpInputVector);
                            for(int i = 0; i < tmpInputVector.length; i++) {
                                tmpInputVector[i] *= (1.0f / tmpVectorLengthForModificationWinnerCluster);
                            }
                            this.clusterMatrix[tmpWinnerClusterIndex] = tmpInputVector;
                            tmpClusterOccupation[tmpSampleVectorIndicesInRandomOrder[tmpCurrentInput]] = tmpWinnerClusterIndex;
                            if(anIsClusteringResultExported) {
                                this.clusteringProcess.add(String.format("Cluster number: %d",tmpWinnerClusterIndex));
                                this.clusteringProcess.add(String.format("Number of detected clusters: %d",tmpNumberOfDetectedClusters));
                            }
                            //</editor-fold>
                        }
                        //</editor-fold>
                    }
                }
                //</editor-fold>
            }
            //</editor-fold>
            //<editor-fold desc="Check the convergence. If the network is converged, tmpConvergence == true otherwise false."
            tmpIsSystemConverged = this.isConverged(tmpNumberOfDetectedClusters, tmpCurrentNumberOfEpochs);
            tmpCurrentNumberOfEpochs++;
            //</editor-fold>
            //<editor-fold desc="Last clustering process input."
            if(anIsClusteringResultExported) {
                this.clusteringProcess.add(String.format("Convergence status: %b",tmpIsSystemConverged));
                this.clusteringProcess.add("---------------------------------------");
            }
            //</editor-fold>
        }
        //</editor-fold>
        //<editor-fold desc="Last clustering result input."
        if(anIsClusteringResultExported) {
            this.clusteringResult.add(String.format("Number of epochs: %d",tmpCurrentNumberOfEpochs));
            this.clusteringResult.add(String.format("Number of detected clusters: %d",tmpNumberOfDetectedClusters));
            this.clusteringResult.add("Convergence status: " + tmpIsSystemConverged);
            this.clusteringResult.add("---------------------------------------");
        }
        //</editor-fold>
        //<editor-fold desc="Return object"
        if(!anIsClusteringResultExported) {
            return new Art2aFloatClusteringResult(this.vigilanceParameter, tmpCurrentNumberOfEpochs,
                    tmpNumberOfDetectedClusters, tmpClusterOccupation,this.clusterMatrix, this.dataMatrix);
        } else {
            return new Art2aFloatClusteringResult(this.vigilanceParameter, tmpCurrentNumberOfEpochs,
                    tmpNumberOfDetectedClusters, tmpClusterOccupation, this.clusterMatrix,
                    this.dataMatrix,this.clusteringProcess, this.clusteringResult);
        }
        //</editor-fold>
    }
    //</editor-fold>
}
