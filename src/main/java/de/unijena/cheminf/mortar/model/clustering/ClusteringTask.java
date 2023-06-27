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

package de.unijena.cheminf.mortar.model.clustering;

import de.unijena.cheminf.art2aClustering.clustering.Art2aDoubleClustering;
import de.unijena.cheminf.art2aClustering.clustering.Art2aFloatClustering;
import de.unijena.cheminf.art2aClustering.exceptions.ConvergenceFailedException;
import de.unijena.cheminf.art2aClustering.interfaces.IArt2aClustering;
import de.unijena.cheminf.art2aClustering.interfaces.IArt2aClusteringResult;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Callable class for clustering input vectors (fingerprints).
 *
 * @author Betuel Sevindik
 * @version 1.0.0.0
 */
public class ClusteringTask implements Callable<IArt2aClusteringResult> {
    //<editor-fold desc="private class variables" defaultstate="collapsed>
    /**
     * Clustering instance
     */
    private IArt2aClustering art2aClustering;
    /**
     * If isClusteringResultExported = true the cluster results are exported to text files.
     * If isClusteringResultExported = false the clustering results are not exported to text files.
     */
    private boolean isClusteringResultExported;
    private int seed;
    /**
     * If the seed is setting by the user isSeedSet == true, otherwise false.
     * Is needed to determine whether the default value or the seed specified by the user should be used.
     */
    private boolean isSeedSet;
    //</editor-fold>
    //
    //<editor-fold desc="private final class constants" defaultstate="collapsed>
    /**
     * Seed value for randomising the input vectors before starting clustering.
     */
    private final int DEFAULT_SEED_VALUE_TO_RANDOMIZE_INPUT_VECTORS = 1;
    //</editor-fold>
    //
    //<editor-fold desc="private static final class constants" defaultstate="collapsed>
    /**
     * Default value of the learning parameter in float
     */
    public static final float DEFAULT_LEARNING_PARAMETER_FLOAT = 0.01f;
    /**
     * Default value of the required similarity parameter in float
     */
    public static final float REQUIRED_SIMILARITY_FLOAT = 0.99f;
    /**
     * Default value of the learning parameter in double
     */
    public static final double DEFAULT_LEARNING_PARAMETER_DOUBLE = 0.01;
    /**
     * Default value of the required similarity parameter in double
     */
    public static final double REQUIRED_SIMILARITY_DOUBLE = 0.99;
    //</editor-fold>
    //
    //<editor-fold desc="private static final class variables" defaultstate="collapsed>
    /**
     * Logger of this class
     */
    private static final Logger LOGGER = Logger.getLogger(ClusteringTask.class.getName());
    //</editor-fold>
    //
    // <editor-fold defaultstate="collapsed" desc="Constructors">
    /**
     * Float clustering task constructor.
     * Creates a new Art2aClusteringTask instance with the specified parameters.
     *
     * @param aVigilanceParameter parameter to influence the number of clusters.
     * @param aDataMatrix matrix contains all inputs for clustering. Each row of the matrix contains one input.
     * In addition, all inputs must have the same length. Each column of the matrix contains one component of the input.
     * @param aMaximumEpochsNumber maximum number of epochs that the system may use for convergence.
     * @param anIsClusteringResultExported if the parameter is set to true, the cluster results
     * are exported to text files.
     * @param aRequiredSimilarity parameter indicating the minimum similarity between the current
     * cluster vectors and the previous cluster vectors. The parameter is crucial
     * for the convergence of the system. If the parameter is set too high, a much
     * more accurate similarity is expected and the convergence may take longer,
     * while a small parameter expects a lower similarity between the cluster
     * vectors and thus the system may converge faster.
     * @param aLearningParameter parameter to define the intensity of keeping the old cluster vector in mind
     * before the system adapts it to the new sample vector.
     * @throws IllegalArgumentException is thrown, if the given arguments are invalid. The checking of the arguments
     * is done in the constructor of Art2aFloatClustering.
     * @throws NullPointerException is thrown, if the given aDataMatrix is null. The checking of the data matrix is
     * done in the constructor of the ArtaFloatClustering.
     *
     */
    public ClusteringTask(float aVigilanceParameter, float[][] aDataMatrix, int aMaximumEpochsNumber,
                          boolean anIsClusteringResultExported, float aRequiredSimilarity, float aLearningParameter)
            throws IllegalArgumentException, NullPointerException {
        this.isClusteringResultExported = anIsClusteringResultExported;
        this.isSeedSet = false;
        this.art2aClustering = new Art2aFloatClustering(aDataMatrix, aMaximumEpochsNumber, aVigilanceParameter,
                aRequiredSimilarity, aLearningParameter);
    }
    //
    /**
     * Float clustering task constructor.
     * Creates a new Art2aClusteringTask instance with the specified parameters.
     * For the required similarity and learning parameter default values are used.
     *
     * @param aVigilanceParameter parameter to influence the number of clusters.
     * @param aDataMatrix matrix contains all inputs for clustering. Each row of the matrix contains one input.
     *                    In addition, all inputs must have the same length.
     *                    Each column of the matrix contains one component of the input.
     * @param aMaximumEpochsNumber maximum number of epochs that the system may use for convergence.
     * @param anIsClusteringResultExported if the parameter is set to true, the cluster results
     *                                         are exported to text files.
     * @throws IllegalArgumentException is thrown, if the given arguments are invalid. The checking of the arguments
     *                                  is done in the constructor of Art2aFloatClustering.
     * @throws NullPointerException is thrown, if the given aDataMatrix is null. The checking of the data matrix is
     *                              done in the constructor of the ArtaFloatClustering.
     */
    public ClusteringTask(float aVigilanceParameter, float[][] aDataMatrix, int aMaximumEpochsNumber,
                          boolean anIsClusteringResultExported)
            throws IllegalArgumentException, NullPointerException {
        this(aVigilanceParameter, aDataMatrix, aMaximumEpochsNumber, anIsClusteringResultExported,
                ClusteringTask.REQUIRED_SIMILARITY_FLOAT, ClusteringTask.DEFAULT_LEARNING_PARAMETER_FLOAT);
    }
    //
    /**
     * Double clustering task constructor.
     * Creates a new Art2aDoubleClustering instance with the specified parameters.
     *
     * @param aVigilanceParameter parameter to influence the number of clusters.
     * @param aDataMatrix matrix contains all inputs for clustering. Each row of the matrix contains one input.
     * In addition, all inputs must have the same length.
     * Each column of the matrix contains one component of the input.
     * @param aMaximumEpochsNumber maximum number of epochs that the system may use for convergence.
     * @param anIsClusteringResultExported if the parameter is set to true, the cluster results are
     * exported to text files.
     * @param aRequiredSimilarity parameter indicating the minimum similarity between the current
     * cluster vectors and the previous cluster vectors.
     * @param aLearningParameter parameter to define the intensity of keeping the old cluster vector in mind
     * before the system adapts it to the new sample vector.
     * @throws IllegalArgumentException is thrown, if the given arguments are invalid. The checking of the arguments
     * is done in the constructor of Art2aFloatClustering.
     * @throws NullPointerException is thrown, if the given aDataMatrix is null. The checking of the data matrix is
     * done in the constructor of the ArtaFloatClustering.
     */
    public ClusteringTask(double aVigilanceParameter, double[][] aDataMatrix, int aMaximumEpochsNumber,
                          boolean anIsClusteringResultExported, double aRequiredSimilarity, double aLearningParameter)
            throws IllegalArgumentException, NullPointerException {
        this.isClusteringResultExported = anIsClusteringResultExported;
        this.art2aClustering = new Art2aDoubleClustering(aDataMatrix, aMaximumEpochsNumber, aVigilanceParameter,
                aRequiredSimilarity, aLearningParameter);
    }
    //
    /**
     * Double clustering task constructor.
     * Creates a new Art2aDoubleClustering instance with the specified parameters.
     * For the required similarity and learning parameter default values are used.
     *
     * @param aVigilanceParameter parameter to influence the number of clusters.
     * @param aDataMatrix matrix contains all inputs for clustering. Each row of the matrix contains one input.
     * In addition, all inputs must have the same length. Each column of the matrix contains one component of the input.
     * @param aMaximumEpochsNumber maximum number of epochs that the system may use for convergence.
     * @param anIsClusteringResultExported if the parameter is set to true, the cluster results are
     * exported to text files.
     * @throws IllegalArgumentException is thrown, if the given arguments are invalid. The checking of the arguments
     * is done in the constructor of Art2aFloatClustering.
     * @throws NullPointerException is thrown, if the given aDataMatrix is null. The checking of the data matrix is
     * done in the constructor of the ArtaFloatClustering.
     *
     */
    public ClusteringTask(double aVigilanceParameter, double[][] aDataMatrix, int aMaximumEpochsNumber,
                          boolean anIsClusteringResultExported) throws IllegalArgumentException, NullPointerException {
        this(aVigilanceParameter, aDataMatrix, aMaximumEpochsNumber, anIsClusteringResultExported,
                ClusteringTask.REQUIRED_SIMILARITY_DOUBLE, ClusteringTask.DEFAULT_LEARNING_PARAMETER_DOUBLE);
    }
    //</editor-fold>
    //
    // <editor-fold defaultstate="collapsed" desc="Overriden call() method">
    /**
     * Executes the clustering.
     *
     * @return clustering result.
     */
    @Override
    public IArt2aClusteringResult call() {
        try {
            if(this.isSeedSet) {
                return this.art2aClustering.getClusterResult(this.isClusteringResultExported, this.seed);
            } else {
                return this.art2aClustering.getClusterResult(this.isClusteringResultExported,
                        this.DEFAULT_SEED_VALUE_TO_RANDOMIZE_INPUT_VECTORS);
            }
        } catch (ConvergenceFailedException anException) {
            ClusteringTask.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return null;
        }
    }
    //</editor-fold>
    //
    // <editor-fold defaultstate="collapsed" desc="Public method">
    /**
     *
     * User-defined seed value to randomize input vectors.
     * Different seed values can lead to different clustering results.
     *
     * @param aSeed seed value
     * @return user-defined seed value.
     */
    public int setSeed(int aSeed) {
        this.seed = aSeed;
        this.isSeedSet = true;
        return this.seed;
    }
    //</editor-fold>
}
