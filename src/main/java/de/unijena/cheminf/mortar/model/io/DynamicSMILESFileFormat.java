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
 * @version 1.0.0.0
 */
public class DynamicSMILESFileFormat {
    //<editor-fold desc="public static final class constants">
    /**
     * Placeholder separator char for files that have only one column.
     */
    public static final Character PLACEHOLDER_SEPARATOR_CHAR = '0';
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
     * Defines the format as a file that has only one column of SMILES codes and nothing else, so no ID column and no
     * separator character.
     * All other fields will be set to their placeholder values.
     *
     * @param aHasHeaderLine whether the first line in the file is a header or contains a valid SMILES code as well
     */
    public DynamicSMILESFileFormat(boolean aHasHeaderLine) {
        this(aHasHeaderLine, DynamicSMILESFileFormat.PLACEHOLDER_SEPARATOR_CHAR,
                DynamicSMILESFileFormat.DEFAULT_SMILES_COLUMN_POSITION,
                DynamicSMILESFileFormat.PLACEHOLDER_ID_COLUMN_POSITION);
    }
    //
    /**
     * Defines the format as a file that has at least two columns, defines the character that separates the columns, the
     * position of the SMILES code column and the ID/name column, and whether the first line in the file is a header or
     * contains a valid SMILES code as well.
     * <br>No parameter checks to keep this data container flexible and be able to use the placeholder values here as well, so be careful!
     *
     * @param aHasHeaderLine whether the first line in the file is a header or contains a valid SMILES code as well
     * @param aSeparatorChar character that separates the columns
     * @param aSMILESCodeColumnPosition position of the SMILES code column
     * @param anIDColumnPosition position of the ID/name column
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
     * Whether the first line in the file is a header or contains a valid SMILES code as well.
     *
     * @return whether the first line of the file should be parsed as well or skipped
     */
    public boolean hasHeaderLine() {
        return this.hasHeaderLine;
    }
    //
    /**
     * Character that separates the columns. If the file has only one column, this field will be set to its placeholder
     * value (check whether the file has an ID/name column).
     *
     * @return character separating the columns
     */
    public Character getSeparatorChar() {
        return this.separatorChar;
    }
    //
    /**
     * Position of the SMILES column in the file.
     *
     * @return position of SMILES column
     */
    public int getSMILESCodeColumnPosition() {
        return this.smilesCodeColumnPosition;
    }
    //
    /**
     * Whether the file has an additional column containing names/IDs of the structures. If false, it can be assumed that
     * the files has only one column, the SMILES column. The separator character and the ID column position will be set
     * to their placeholder values.
     *
     * @return whether the file has an additional column containing names/IDs of the structures
     */
    public boolean hasIDColumn() {
        return this.hasIDColumn;
    }
    //
    /**
     * Position of the ID/name column in the file. If the file has only one column, this field will be set to its placeholder
     * value (check whether the file has an ID/name column).
     *
     * @return position of ID/name column
     */
    public int getIDColumnPosition() {
        return this.idColumnPosition;
    }
    //</editor-fold>
}
