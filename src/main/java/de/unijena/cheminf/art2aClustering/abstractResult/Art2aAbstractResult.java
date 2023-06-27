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

package de.unijena.cheminf.art2aClustering.abstractResult;

import de.unijena.cheminf.art2aClustering.interfaces.IArt2aClusteringResult;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract class.
 * This abstract class implements the IArt2aClusteringResult interface.
 * The interface provides methods to access clustering results.
 * The concrete implementation of the clustering result properties in the IArt2aClusteringResult interface
 * is taken over by this abstract class.
 *
 *
 * @author Betuel Sevindik
 * @version 1.0.0.0
 */
public abstract class Art2aAbstractResult implements IArt2aClusteringResult {
    //<editor-fold desc="Private class variables" defaultstate="collapsed">
    /**
     * Queue of typ String for clustering result (process)
     */
    private ConcurrentLinkedQueue<String> clusteringProcess;
    /**
     * Queue of typ String for clustering result
     */
    private  ConcurrentLinkedQueue<String> clusteringResult;
    /**
     * The map maps the cluster number to the number of inputs in the cluster.
     */
    private HashMap<Integer, Integer> clusterNumberToClusterMemberMap;
    //</editor-fold>
    //
    //<editor-fold desc="Private final class variables" defaultstate="collapsed">
    /**
     * Represents the cluster assignment of each input vector. For example clusterView[4] = 0 means that
     * input vector with index 4 cluster 0 has been assigned.
     */
    private final int[] clusterView;
    /**
     * Final number of epochs the system needed to converge.
     */
    private final int numberOfEpochs;
    /**
     * Final number of clusters detected after successful completion of clustering.
     */
    private final int numberOfDetectedClusters;
    /**
     * Initial capacity value for maps
     */
    private final double INITIAL_CAPACITY_VALUE = 1.5;
    //</editor-fold>
    //
    //<editor-fold desc="Private static final class variables" defaultstate="collapsed">
    /**
     * Logger of this class
     */
    private static final Logger LOGGER = Logger.getLogger(Art2aAbstractResult.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Constructor.
     *
     * @param aNumberOfEpochs final epoch number.
     * @param aNumberOfDetectedClusters final number of detected clusters.
     * @param aClusteringProcessQueue clustering result (process) queue of typ string. Queues are used or
     * thread security but these here should not be used by more than one thread.
     * @param aClusteringResultQueue clustering result queue of typ string. Queues are used or thread security
     * but these here should not be used by more than one thread.
     * @param aClusterView array for cluster assignment of each input vector.
     * @throws IllegalArgumentException is thrown, if the given arguments are invalid.
     */
    public Art2aAbstractResult(int aNumberOfEpochs, int aNumberOfDetectedClusters,
                               int[] aClusterView, ConcurrentLinkedQueue<String> aClusteringProcessQueue,
                               ConcurrentLinkedQueue<String> aClusteringResultQueue) throws IllegalArgumentException {
        if(aNumberOfEpochs <= 0) {
            throw new IllegalArgumentException("aNumberOfEpochs is invalid.");
        }
        if(aNumberOfDetectedClusters < 1) {
            throw new IllegalArgumentException("aNumberOfDetectedClusters is invalid.");
        }
        this.clusterView = aClusterView;
        this.numberOfEpochs = aNumberOfEpochs;
        this.numberOfDetectedClusters = aNumberOfDetectedClusters;
        this.clusteringProcess = aClusteringProcessQueue;
        this.clusteringResult = aClusteringResultQueue;
        this.clusterNumberToClusterMemberMap = this.getClusterSize(this.clusterView);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Overriden public properties" defaultstate="collapsed">
    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfEpochs() {
        return this.numberOfEpochs;
    }
    //
    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfDetectedClusters() {
        return this.numberOfDetectedClusters;
    }
    //
    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getClusterIndices(int aClusterNumber) throws IllegalArgumentException {
        if (aClusterNumber >= this.numberOfDetectedClusters) {
            throw new IllegalArgumentException("The specified cluster number does not exist and exceeds " +
                    "the maximum number of clusters.");
        } else {
            int[] tmpIndicesInCluster = new int[this.clusterNumberToClusterMemberMap.get(aClusterNumber)];
            int tmpInputIndices = 0;
            int tmpIterator = 0;
            for (int tmpClusterMember : this.clusterView) {
                if (tmpClusterMember == aClusterNumber) {
                    tmpIndicesInCluster[tmpIterator] = tmpInputIndices;
                    tmpIterator++;
                }
                tmpInputIndices++;
            }
            return tmpIndicesInCluster;
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Overriden public methods" defaultstate="collapsed">
    //
    /**
     * {@inheritDoc}
     */
    @Override
    public void exportClusteringResultsToTextFiles(Writer aClusteringResultWriter, Writer aClusteringProcessWriter)
            throws NullPointerException {
        if(aClusteringResultWriter == null || aClusteringProcessWriter == null) {
            throw new NullPointerException("At least one of the writers is null.");
        }
        if (this.clusteringProcess == null || this.clusteringResult == null) {
            throw new NullPointerException("The associated argument that enables the export of clustering results is " +
                    "is set to false.\n" +
                    "Please set the argument for export to true.");
        }
        try {
            for (String tmpClusteringResult : this.clusteringResult) {
                aClusteringResultWriter.write(tmpClusteringResult + "\n");
            }
            for (String tmpClusteringProcess : this.clusteringProcess) {
                aClusteringProcessWriter.write(tmpClusteringProcess + "\n");
            }
        }
        catch(IOException anException) {
            Art2aAbstractResult.LOGGER.log(Level.SEVERE, "Export to text files failed.");
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private methods" defaultstate="collapsed">
    /**
     * Method for determining the size of the detected clusters.
     *
     * @param aClusterView represents the cluster assignment of each input vector.
     * @return HashMap<Integer, Integer> maps the cluster number to the number of inputs in the cluster.
     */
    private HashMap<Integer, Integer> getClusterSize(int[] aClusterView) {
        HashMap<Integer, Integer> tmpClusterToMembersMap =
                new HashMap<>((int) (this.getNumberOfDetectedClusters() * this.INITIAL_CAPACITY_VALUE));
        for(int tmpClusterMembers : aClusterView) {
            if (tmpClusterMembers == -1) {
                continue;
            }
            if(!tmpClusterToMembersMap.containsKey(tmpClusterMembers)) {
                tmpClusterToMembersMap.put(tmpClusterMembers, 1);
            } else {
                tmpClusterToMembersMap.put(tmpClusterMembers, tmpClusterToMembersMap.get(tmpClusterMembers) + 1);
            }
        }
        return tmpClusterToMembersMap;
    }
    //</editor-fold>
}
