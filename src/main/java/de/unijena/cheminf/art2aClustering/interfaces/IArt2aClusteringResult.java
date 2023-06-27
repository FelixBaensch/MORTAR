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

package de.unijena.cheminf.art2aClustering.interfaces;

import java.io.Writer;

/**
 * Interface for implementing clustering result classes.
 *
 * @param <T> generic parameter. This parameter is either a Double or a Float.
 *           The type of teh method @code {@link #getAngleBetweenClusters(int, int)}
 *           is calculated either as a float or as a double, depending on the clustering precision option.
 *
 * @author Betuel Sevindik
 * @version 1.0.0.0
 */
public interface IArt2aClusteringResult<T> {
    // <editor-fold defaultstate="collapsed" desc="Public properties">
    /**
     * Returns the vigilance parameter of the clustering algorithm.
     *
     * @return float vigilance parameter
     */
    T getVigilanceParameter();
    //
    /**
     * Returns the number of detected clusters.
     *
     * @return int detected cluster number
     */
    int getNumberOfDetectedClusters();
    //
    /**
     * Returns the number of epochs.
     *
     * @return int epoch number
     */
    int getNumberOfEpochs();
    //
    /**
     * Returns the input indices assigned to the given cluster.
     *
     * @param aClusterNumber given cluster number
     * @return array with the input indices for a given cluster
     * @throws IllegalArgumentException is thrown if the given cluster does not exist.
     */
    int[] getClusterIndices(int aClusterNumber) throws IllegalArgumentException;
    //
    /**
     * Calculates the cluster representative. This means that the input that is most
     * similar to the cluster vector is determined.
     *
     * @param aClusterNumber Cluster number for which the representative is to be calculated.
     * @return int input indices of the representative input in the cluster.
     * @throws IllegalArgumentException is thrown if the given cluster number is invalid.
     */
    int getClusterRepresentatives(int aClusterNumber) throws IllegalArgumentException;
    // </editor-fold>
    //
    // <editor-fold defaultstate="collapsed" desc="Public methods">
    /**
     * The result of the clustering is additionally exported in two text files. One of these files is a
     * very detailed representation of the results (clustering process file), while in the other only the
     * most important results are summarized (clustering result file).
     * <u>IMPORTANT: </u> In order to additionally export the clustering results into text files,
     * the folder must be created first.
     * This requires the method call setUpClusteringResultTextFilePrinter(String aPathName, Class)
     * or user own Writer and text files. This method call is optional, the folder can also be created by the user.
     *
     * @see de.unijena.cheminf.clustering.art2a.util.FileUtil#setUpClusteringResultTextFilePrinters(String, Class)
     *
     * @param aClusteringProcessWriter clustering result (process) writer
     * @param aClusteringResultWriter clustering result writer
     * @throws NullPointerException is thrown, if the Writers are null.
     *
     */
    void exportClusteringResultsToTextFiles(Writer aClusteringResultWriter, Writer aClusteringProcessWriter)
            throws NullPointerException;
    //
    /**
     * Calculates the angle between two clusters.
     * The angle between the clusters defines the distance between them.
     * Since all vectors are normalized to unit vectors in the first step of clustering
     * and only positive components are allowed, they all lie in the positive quadrant
     * of the unit sphere, so the maximum distance between two clusters can be 90 degrees.
     *
     * @param aFirstCluster first cluster
     * @param aSecondCluster second cluster
     * @return generic angle double or float.
     * @throws IllegalArgumentException if the given parameters are invalid.
     */
     T getAngleBetweenClusters(int aFirstCluster, int aSecondCluster) throws IllegalArgumentException;
    // </editor-fold>
}
