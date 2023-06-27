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

import de.unijena.cheminf.art2aClustering.exceptions.ConvergenceFailedException;

/**
 * Interface for implementing float and double Art-2a clustering.
 *
 * @author Betuel Sevindik
 * @version 1.0.0.0
 */
public interface IArt2aClustering {
    // <editor-fold defaultstate="collapsed" desc="Public methods">
    /**
     * Initialise the cluster matrices.
     */
    void initializeMatrices();
    //
    /**
     * Since the Art-2a algorithm randomly selects any input vector, the input vectors must first be randomized.
     * The input vectors/fingerprints are randomized so that all input vectors can be clustered by random selection.
     *
     * Here, the Fisher-Yates method is used to randomize the inputs.
     *
     * @return an array with vector indices in a random order
     */
    int[] getRandomizeVectorIndices();
    //
    /**
     * Starts an Art-2A clustering algorithm.
     * The clustering process begins by randomly selecting an input vector/fingerprint from the data matrix.
     * After normalizing the first input vector, it is assigned to the first cluster. For all other subsequent
     * input vectors, they also undergo certain normalization steps. If there is sufficient similarity to an
     * existing cluster, they are assigned to that cluster. Otherwise, a new cluster is formed, and the
     * input is added to it. Null vectors are not clustered.
     *
     * @param anIsClusteringResultExported If the parameter == true, all information about the
     * clustering is exported to 2 text files.The first exported text file is a detailed log of the clustering process
     * and the intermediate results and the second file is a rough overview of the final result.
     * @param aSeedValue user-defined seed value to randomize input vectors.
     * @return IArt2aClusteringResult
     * @throws ConvergenceFailedException is thrown, when convergence fails.
     */
    IArt2aClusteringResult getClusterResult(boolean anIsClusteringResultExported, int aSeedValue) throws ConvergenceFailedException;
    // </editor-fold>
}
