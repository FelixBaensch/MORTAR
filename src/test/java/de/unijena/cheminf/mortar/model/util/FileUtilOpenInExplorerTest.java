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

import java.io.File;

/**
 * Tests the argument checks of {@link FileUtil#openFilePathInExplorer(String)} and
 * {@link FileUtil#openFilePathInExplorer(File)}. Opening the file explorer itself is not tested, since it would open
 * a window on the machine running the tests.
 *
 * @author Manuel Schauer
 */
public class FileUtilOpenInExplorerTest {
    /**
     * Tests that invalid paths are rejected with an IllegalArgumentException before anything is opened.
     */
    @Test
    public void testOpenFilePathInExplorerRejectsInvalidArguments() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> FileUtil.openFilePathInExplorer((String) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> FileUtil.openFilePathInExplorer(""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> FileUtil.openFilePathInExplorer("   "));
        Assertions.assertThrows(IllegalArgumentException.class, () -> FileUtil.openFilePathInExplorer((File) null));
    }
}
