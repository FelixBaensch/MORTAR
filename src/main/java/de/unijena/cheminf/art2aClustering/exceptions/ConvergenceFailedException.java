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

package de.unijena.cheminf.art2aClustering.exceptions;

/**
 * An exception thrown when convergence fails. This exception occurs when the system is unable to, achieve a
 * convergent state or meet the desired convergence criteria.The ConvergenceFailedException is a special type of
 * Exception and inherits from this class.
 * It can be used to handle convergence failures in order to take appropriate action.
 *
 * @author Betuel Sevindik
 * @version 1.0.0.0
 */
public class ConvergenceFailedException extends Exception {
    // <editor-fold defaultstate="collapsed" desc="Constructors">
    /**
     * Constructor.
     */
    public ConvergenceFailedException() {
        super("Convergence failed!");
    }
    //
    /**
     * Constructor.
     *
     * @param anErrorMessage error message is displayed, when the exception is thrown.
     */
    public ConvergenceFailedException(String anErrorMessage) {
        super(anErrorMessage);
    }
    //</editor-fold>
}
