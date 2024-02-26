/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2024  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.model.io;

/**
 *
 *
 * @author Jonas Schaub
 */
public class DynamicSMILESFileFormat {
    private final boolean hasHeaderLine;

    private final Character separatorChar;

    private final int smilesCodeColumnPosition;

    private final boolean hasIDColumn;

    private final int idColumnPosition;

    public DynamicSMILESFileFormat(Character aSeparatorChar, int aSMILESCodeColumnPosition, boolean aHasHeaderLine) {
        this(aSeparatorChar, aSMILESCodeColumnPosition, aHasHeaderLine, -1);
    }

    public DynamicSMILESFileFormat(Character aSeparatorChar, int aSMILESCodeColumnPosition, boolean aHasHeaderLine, int anIDColumnPosition) {
        this.separatorChar = aSeparatorChar;
        this.smilesCodeColumnPosition = aSMILESCodeColumnPosition;
        this.hasHeaderLine = aHasHeaderLine;
        this.hasIDColumn = anIDColumnPosition == -1 ? false : true;
        this.idColumnPosition = anIDColumnPosition;
    }

    public boolean hasHeaderLine() {
        return this.hasHeaderLine;
    }

    public Character getSeparatorChar() {
        return this.separatorChar;
    }

    public int getSMILESCodeColumnPosition() {
        return this.smilesCodeColumnPosition;
    }

    public boolean hasIDColumn() {
        return this.hasIDColumn;
    }

    public int getIdColumnPosition() {
        return this.idColumnPosition;
    }
}
