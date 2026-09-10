/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2026  Felix Baensch, Jonas Schaub (felix.j.baensch@gmail.com, jonas.schaub@uni-jena.de)
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class for the MORTARException type, covering both of its constructors (message; message + cause).
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public class MORTARExceptionTest {
    //<editor-fold desc="constructor" defaultstate="collapsed">
    /**
     * Constructor that needs nothing to set up.
     */
    public MORTARExceptionTest() {
    }
    //</editor-fold>
    //
    //<editor-fold desc="constructor test methods" defaultstate="collapsed">
    /**
     * Tests whether the single-argument constructor stores the given message and leaves the cause unset.
     */
    @Test
    public void messageConstructorTest() {
        MORTARException tmpException = new MORTARException("some message");
        Assertions.assertEquals("some message", tmpException.getMessage());
        Assertions.assertNull(tmpException.getCause());
    }
    //
    /**
     * Tests whether the two-argument constructor stores both the given message and the given cause instance.
     */
    @Test
    public void messageAndCauseConstructorTest() {
        Throwable tmpCause = new IllegalStateException("root cause");
        MORTARException tmpException = new MORTARException("some message", tmpCause);
        Assertions.assertEquals("some message", tmpException.getMessage());
        Assertions.assertSame(tmpCause, tmpException.getCause());
    }
    //</editor-fold>
}
