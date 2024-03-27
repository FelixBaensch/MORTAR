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

package de.unijena.cheminf.mortar.preference;

import de.unijena.cheminf.mortar.model.util.MiscUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A preference for boolean values.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class BooleanPreference extends BasePreference {
    //<editor-fold defaultstate="collapsed" desc="Private static final class constants">
    /**
     * The version of this class.
     */
    private static final String VERSION = "1.0.0.0";

    /**
     * The PreferenceType enum constant associated with this preference implementation.
     */
    private static final PreferenceType TYPE = PreferenceType.BOOLEAN;

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(BooleanPreference.class.getName());
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Private class variables">
    /**
     * Boolean content of this preference.
     */
    private boolean content;
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /**
     * Creates a new BooleanPreference instance with the given name and content.
     *
     * @param aName the new preference's name; leading and trailing whitespaces will not be considered;
     * @param aBoolean the new preference's content
     * @throws IllegalArgumentException if aName is 'null' or does not match the required pattern
     */
    public BooleanPreference(String aName, boolean aBoolean) throws IllegalArgumentException {
        //<editor-fold defaultstate="collapsed" desc="Checks">
        if (!BasePreference.isValidName(aName)) {
            throw new IllegalArgumentException("Preference name " + aName + " does not match required pattern!");
        }
        //</editor-fold>
        this.name = aName.trim();
        this.guid = MiscUtil.getGloballyUniqueID();
        this.content = aBoolean;
    }

    /**
     * (Re-)instantiates a new BooleanPreference object from a line-based text file.
     * <p>
     * Note: The succession of persisted variables must be exactly mirrored in writeRepresentation(PrintWriter).
     *
     * @param aReader to read the persisted representation from
     * @throws java.io.IOException if anything goes wrong
     */
    public BooleanPreference(BufferedReader aReader) throws IOException {
        try {
            String tmpVersion = aReader.readLine();
            switch (tmpVersion) {
                case "1.0.0.0":
                    this.reloadVersion1000(aReader);
                    break;
                //case "1.0.0.1":
                //...
                //break;
                default:
                    throw new IOException("Invalid version.");
            }
        } catch (Exception anException) {
            BooleanPreference.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            throw new IOException("Preference can not be instantiated from given reader.");
        }
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public properties (get)">

    @Override
    public String getContentRepresentative() {
        return Boolean.toString(this.content);
    }

    @Override
    public String getVersion() {
        return BooleanPreference.VERSION;
    }

    @Override
    public PreferenceType getType() {
        return BooleanPreference.TYPE;
    }

    /**
     * Returns the boolean content of this preference.
     *
     * @return the boolean content
     */
    public boolean getContent() {
        return this.content;
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public properties (set)">
    /**
     * Sets the boolean content of this preference.
     *
     * @param aBoolean the new content
     */
    public void setContent(boolean aBoolean) {
        this.content = aBoolean;
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public methods">

    @Override
    public BooleanPreference copy() {
        BooleanPreference tmpCopy = new BooleanPreference(this.name, this.content);
        tmpCopy.guid = this.guid;
        return tmpCopy;
    }

    @Override
    public void writeRepresentation(PrintWriter aPrintWriter) {
        aPrintWriter.println(BooleanPreference.VERSION);
        aPrintWriter.println(this.name);
        aPrintWriter.println(this.guid);
        aPrintWriter.println(this.content);
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "_'" + this.name + "'_" + "Content:" + this.content;
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Private methods">
    /**
     * (Re-)instantiates a new BooleanPreference object of version 1.0.0.0 from a line-based text file.
     */
    private void reloadVersion1000(BufferedReader aReader) throws IOException {
        this.name = aReader.readLine();
        this.guid = aReader.readLine();
        this.content = Boolean.parseBoolean(aReader.readLine());
    }
    //</editor-fold>
}
