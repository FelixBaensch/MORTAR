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

package de.unijena.cheminf.mortar.model.data;

/**
 * Enum for the data model properties that are displayed in the TableViews
 *
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public enum DataModelPropertiesForTableView {
    /**
     * Enum for name property
     */
    NAME ("name"),
    /**
     * Enum for uniqueSmiles property
     */
    UNIQUE_SMILES ("uniqueSmiles"),
    /**
     * Enum for parentMoleculeName property
     */
    PARENT_MOLECULE_NAME ("parentMoleculeName"),
    /**
     * Enum for absoluteFrequency property
     */
    ABSOLUTE_FREQUENCY ("absoluteFrequency"),
    /**
     * Enum for absolutePercentage property
     */
    ABSOLUTE_PERCENTAGE ("absolutePercentage"),
    /**
     * Enum for moleculeFrequency property
     */
    MOLECULE_FREQUENCY ("moleculeFrequency"),
    /**
     * Enum for moleculePercentage property
     */
    MOLECULE_PERCENTAGE ("moleculePercentage");
    //
    /**
     * String representation
     */
    private final String text;
    //
    /**
     * Constructor
     *
     * @param aText String representation of enum property
     */
    DataModelPropertiesForTableView(String aText) {
        this.text = aText;
    }
    //
    /**
     * Gets String representation
     *
     * @return text String representation
     */
    public String getText() {
        return this.text;
    }
    //
    /**
     * Gets enum from String representation. Returns null if no enum can be found for the String.
     *
     * @param aText String representation
     * @return DataModelPropertiesForTableView
     */
    public static DataModelPropertiesForTableView fromString(String aText) {
        for (DataModelPropertiesForTableView property : DataModelPropertiesForTableView.values()) {
            if(property.text.equalsIgnoreCase(aText)) {
                return property;
            }
        }
        return null;
    }
}
