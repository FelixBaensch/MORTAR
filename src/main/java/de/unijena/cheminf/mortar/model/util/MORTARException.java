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

package de.unijena.cheminf.mortar.model.util;

/**
 * A checked exception that MORTAR classes can throw when no predefined Java exception fits the case.
 * Inspired by {@link org.openscience.cdk.exception.CDKException}.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class MORTARException extends Exception {
    /**
     * Constructs a new MORTARException with the given message. Calls super class constructor.
     *
     * @param aMessage for the constructed exception
     */
    public MORTARException(String aMessage) {
        super(aMessage);
    }
    //
    /**
     * Constructs a new MORTARException with the given message and the Throwable as cause. Calls super class constructor.
     *
     * @param aMessage for the constructed exception
     * @param aCause   the Throwable that triggered this MORTARException
     */
    public MORTARException(String aMessage, Throwable aCause) {
        super(aMessage, aCause);
    }
}
