/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2022  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
 *
 * Source code is available at <https://github.com/FelixBaensch/MORTAR>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unijena.cheminf.mortar.preference;

/**
 * TODO:
 * - Implement clone()
 */

import de.unijena.cheminf.mortar.model.util.MiscUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A preference for a single integer number.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class SingleIntegerPreference extends BasePreference {
    //<editor-fold defaultstate="collapsed" desc="Private static final class constants">
    /**
     * The version of this class.
     */
    private static final String VERSION = "1.0.0.0";

    /**
     * The PreferenceType enum constant associated with this preference implementation.
     */
    private static final PreferenceType TYPE = PreferenceType.SINGLE_INTEGER;

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(SingleIntegerPreference.class.getName());
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Private class variables">
    /**
     * Single integer content of this preference
     */
    private int content;
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /**
     * Creates a new SingleIntegerPreference instance with the given name and content.
     *
     * @param aName the new preference's name; leading and trailing whitespaces will not be considered;
     * @param anInteger the new preference's content
     * @throws IllegalArgumentException if aName is 'null' or does not match the required pattern
     */
    public SingleIntegerPreference(String aName, int anInteger) throws IllegalArgumentException {
        //<editor-fold defaultstate="collapsed" desc="Checks">
        if (!BasePreference.isValidName(aName)) {
            throw new IllegalArgumentException("Preference name " + aName + " does not match required pattern!");
        }
        //</editor-fold>
        this.name = aName.trim();
        this.guid = MiscUtil.getGloballyUniqueID();
        this.content = anInteger;
    }

    /**
     * Creates a new SingleIntegerPreference instance with the given name and content.
     *
     * @param aName the new preference's name; leading and trailing whitespaces will not be considered;
     * @param anIntString the new preference's content; must represent an integer
     * @throws IllegalArgumentException if aName is 'null' or does not match the required pattern or anIntString does not
     * represent an integer
     */
    public SingleIntegerPreference(String aName, String anIntString) throws IllegalArgumentException {
        //<editor-fold defaultstate="collapsed" desc="Checks">
        if (!BasePreference.isValidName(aName)) {
            throw new IllegalArgumentException("Preference name " + aName + " does not match required pattern!");
        }
        int tmpInteger;
        try {
            tmpInteger = SingleIntegerPreference.parseValidContent(anIntString);
        } catch (NumberFormatException aNumberFormatException) {
            SingleIntegerPreference.LOGGER.log(Level.SEVERE, aNumberFormatException.toString(), aNumberFormatException);
            throw new IllegalArgumentException("Given string " + anIntString + " can not be parsed into an integer.");
        }
        //</editor-fold>
        this.name = aName.trim();
        this.content = tmpInteger;
        this.guid = MiscUtil.getGloballyUniqueID();
    }

    /**
     * (Re-)instantiates a new SingleIntegerPreference object from a line-based text file.
     * <p>
     * Note: The succession of persisted variables must be exactly mirrored in writeRepresentation(PrintWriter).
     *
     * @param aReader to read the persisted representation from
     * @throws java.io.IOException if anything goes wrong
     */
    public SingleIntegerPreference(BufferedReader aReader) throws IOException {
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
                    throw new Exception("Invalid version.");
            }
        } catch (Exception anException) {
            SingleIntegerPreference.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            throw new IOException("Preference can not be instantiated from given reader.");
        }
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public properties (get)">
    @Override
    public String getContentRepresentative() {
        return Integer.toString(this.content);
    }

    @Override
    public String getVersion() {
        return SingleIntegerPreference.VERSION;
    }

    @Override
    public PreferenceType getType() {
        return SingleIntegerPreference.TYPE;
    }

    /**
     * Returns the single integer content of this preference.
     *
     * @return the single integer content
     */
    public int getContent() {
        return this.content;
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public properties (set)">
    /**
     * Sets the single integer content of this preference.
     *
     * @param anInteger the new content
     */
    public void setContent(int anInteger) {
        this.content = anInteger;
    }

    /**
     * Sets the single integer content of this preference.
     *
     * @param anIntString the new content
     * @throws IllegalArgumentException if anIntString can not be parsed into an integer
     */
    public void setContent(String anIntString) throws IllegalArgumentException {
        int tmpInteger;
        try {
            tmpInteger = SingleIntegerPreference.parseValidContent(anIntString);
        } catch (NumberFormatException aNumberFormatException) {
            SingleIntegerPreference.LOGGER.log(Level.SEVERE, aNumberFormatException.toString(), aNumberFormatException);
            throw new IllegalArgumentException("anIntString " + anIntString + " can not be parsed into an integer.");
        }
        this.content = tmpInteger;
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public methods">
    @Override
    public IPreference clone() throws CloneNotSupportedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SingleIntegerPreference copy() {
        SingleIntegerPreference tmpCopy = new SingleIntegerPreference(new String(this.name), this.content);
        tmpCopy.guid = new String(this.guid);
        return tmpCopy;
    }

    @Override
    public void writeRepresentation(PrintWriter aPrintWriter) {
        aPrintWriter.println(SingleIntegerPreference.VERSION);
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
    //<editor-fold defaultstate="collapsed" desc="Public static methods">
    /**
     * Tests if the given string would be allowed as a content of this preference type.
     *
     * @param anIntString the string to test; leading and trailing whitespaces will not be considered; It has to be able
     * to be parsed into an integer
     * @return true if anIntString meets the requirements for contents of this preference
     */
    public static boolean isValidContent(String anIntString) {
        try {
            SingleIntegerPreference.parseValidContent(anIntString);
            return true;
        } catch (NumberFormatException aNumberFormatException) {
            SingleIntegerPreference.LOGGER.log(Level.SEVERE, aNumberFormatException.toString(), aNumberFormatException);
            return false;
        }
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Private methods">
    /**
     * (Re-)instantiates a new SingleIntegerPreference object of version 1.0.0.0 from a line-based text file.
     */
    private void reloadVersion1000(BufferedReader aReader) throws Exception {
        this.name = aReader.readLine();
        this.guid = aReader.readLine();
        this.content = Integer.parseInt(aReader.readLine());
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Private static methods">
    /**
     * Returns an integer parsed from the trimmed input or throws a NumberFormatException if the input string is no
     * valid content for this preference.
     */
    private static int parseValidContent(String anIntString) throws NumberFormatException {
        if (Objects.isNull(anIntString) || anIntString.isEmpty()) {
            throw new NumberFormatException();
        }
        String tmpContentString = anIntString.trim();
        int tmpValidContent = Integer.parseInt(tmpContentString);
        return tmpValidContent;
    }
    //</editor-fold>
}
