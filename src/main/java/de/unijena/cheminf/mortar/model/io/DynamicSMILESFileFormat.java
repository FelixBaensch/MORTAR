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
 * Data wrapper for defining the outline of a SMILES file or CSV file containing SMILES codes.
 *
 * @author Jonas Schaub
 */
public class DynamicSMILESFileFormat {
    //<editor-fold desc="public static final class constants">
    /**
     * Placeholder separator char for files that have only one column.
     */
    public static final Character PLACEHOLDER_SEPARATOR_CHAR = '\n';
    //
    /**
     * Placeholder ID column position for files without an ID column.
     */
    public static final int PLACEHOLDER_ID_COLUMN_POSITION = -1;
    //
    /**
     * Default SMILES column position for files with only one column.
     */
    public static final int DEFAULT_SMILES_COLUMN_POSITION = 0;
    //</editor-fold>
    //
    //<editor-fold desc="private final class constants">
    /**
     * Whether the file has a headline that needs to be skipped while parsing or not.
     */
    private final boolean hasHeaderLine;
    //
    /**
     * Character separating the columns of the file.
     */
    private final Character separatorChar;
    //
    /**
     * Position of the column whose elements should be parsed as SMILES strings.
     */
    private final int smilesCodeColumnPosition;
    //
    /**
     * Whether the file has an ID column or not.
     */
    private final boolean hasIDColumn;
    //
    /**
     * Position of the column whose elements should be parsed as IDs of the structures.
     */
    private final int idColumnPosition;
    //</editor-fold>
    //
    //<editor-fold desc="Constructors">
    /**
     *
     *
     * @param aHasHeaderLine
     */
    public DynamicSMILESFileFormat(boolean aHasHeaderLine) {
        this(
                aHasHeaderLine,
                DynamicSMILESFileFormat.PLACEHOLDER_SEPARATOR_CHAR,
                DynamicSMILESFileFormat.DEFAULT_SMILES_COLUMN_POSITION,
                DynamicSMILESFileFormat.PLACEHOLDER_ID_COLUMN_POSITION
        );
    }
    //
    /**
     *
     *
     * @param aHasHeaderLine
     * @param aSeparatorChar
     * @param aSMILESCodeColumnPosition
     * @param anIDColumnPosition
     */
    public DynamicSMILESFileFormat(boolean aHasHeaderLine, Character aSeparatorChar, int aSMILESCodeColumnPosition, int anIDColumnPosition) {
        this.hasHeaderLine = aHasHeaderLine;
        this.separatorChar = aSeparatorChar;
        this.smilesCodeColumnPosition = aSMILESCodeColumnPosition;
        this.hasIDColumn = anIDColumnPosition != DynamicSMILESFileFormat.PLACEHOLDER_ID_COLUMN_POSITION;
        this.idColumnPosition = anIDColumnPosition;
    }
    //</editor-fold>
    //
    //<editor-fold desc="properties get">
    /**
     *
     * @return
     */
    public boolean hasHeaderLine() {
        return this.hasHeaderLine;
    }
    //
    /**
     *
     * @return
     */
    public Character getSeparatorChar() {
        return this.separatorChar;
    }
    //
    /**
     *
     * @return
     */
    public int getSMILESCodeColumnPosition() {
        return this.smilesCodeColumnPosition;
    }
    //
    /**
     *
     * @return
     */
    public boolean hasIDColumn() {
        return this.hasIDColumn;
    }
    //
    /**
     *
     * @return
     */
    public int getIDColumnPosition() {
        return this.idColumnPosition;
    }
    //</editor-fold>
}
