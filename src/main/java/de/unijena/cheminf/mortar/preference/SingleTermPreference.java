/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2020  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas-schaub@uni-jena.de)
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
import java.util.regex.Pattern;

/**
 * A preference for string values
 *
 * @author Jonas Schaub
 */
public class SingleTermPreference extends BasePreference {
    //<editor-fold defaultstate="collapsed" desc="Private static final class constants">
    /**
     * Regex pattern for testing the content input.
     * Allowed characters: 0-9, a-z, ä (hexadecimal value: E4, see below), ü (hexadecimal value: FC, see below),
     * ö (hexadecimal value: F6, see below), ß (hexadecimal value: DF, see below), A-Z,
     * Ä (hexadecimal value: C4, see below), Ü (hexadecimal value: DC, see below), Ö (hexadecimal value: D6, see below),
     * -, _, [], (), {}, :, /, \, ., space (ASCII 32, hexadecimal value: 20, see below) and ,.
     */
    private static final Pattern INPUT_TEST_PATTERN =
            Pattern.compile("\\A[0-9a-z\\xE4\\xFC\\xF6\\xDFA-Z\\xC4\\xDC\\xD6\\.\\,\\-\\_\\[\\]\\(\\)\\{\\}\\:\\/\\\\\\x20]++\\z");

    /**
     * The version of this class.
     */
    private static final String VERSION = "1.0.0.0";

    /**
     * The PreferenceType enum constant associated with this preference implementation.
     */
    private static final PreferenceType TYPE = PreferenceType.SINGLE_TERM;

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(SingleTermPreference.class.getName());
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Private class variables">
    /**
     * String content of this preference
     */
    private String content;
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /**
     * Creates a new SingleTermPreference instance with the given name and content.
     *
     * @param aName the new preference's name; leading and trailing whitespaces will not be considered;
     * @param aString the new preference's content
     * @throws IllegalArgumentException if a parameter is 'null' or does not match the required pattern
     */
    public SingleTermPreference(String aName, String aString) throws IllegalArgumentException {
        //<editor-fold defaultstate="collapsed" desc="Checks">
        if (!BasePreference.isValidName(aName)) {
            throw new IllegalArgumentException("Preference name " + aName + " does not match required pattern!");
        }
        if (!SingleTermPreference.isValidContent(aString)) {
            throw new IllegalArgumentException("Content " + aString + " does not match required pattern!");
        }
        //</editor-fold>
        this.name = aName.trim();
        this.guid = MiscUtil.getGloballyUniqueID();
        this.content = aString;
    }

    /**
     * (Re-)instantiates a new SingleTermPreference object from a line-based text file.
     * <p>
     * Note: The succession of persisted variables must be exactly mirrored in writeRepresentation(PrintWriter).
     *
     * @param aReader to read the persisted representation from
     * @throws java.io.IOException if anything goes wrong
     */
    public SingleTermPreference(BufferedReader aReader) throws IOException {
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
            SingleTermPreference.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            throw new IOException("Preference can not be instantiated from given reader.");
        }
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public properties (get)">
    @Override
    public String getContentRepresentative() {
        return this.content;
    }

    @Override
    public String getVersion() {
        return SingleTermPreference.VERSION;
    }

    @Override
    public PreferenceType getType() {
        return SingleTermPreference.TYPE;
    }

    /**
     * Returns the string content of this preference.
     *
     * @return the string content
     */
    public String getContent() {
        return this.content;
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public properties (set)">
    /**
     * Sets the string content of this preference.
     *
     * @param aString the new content
     * @throws IllegalArgumentException if aString is 'null' or does not match required pattern
     */
    public void setContent(String aString) throws IllegalArgumentException {
        if (!SingleTermPreference.isValidContent(aString)) {
            throw new IllegalArgumentException("Content " + aString + " does not match required pattern!");
        }
        this.content = aString;
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public methods">
    @Override
    public IPreference clone() throws CloneNotSupportedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SingleTermPreference copy() {
        SingleTermPreference tmpCopy = new SingleTermPreference(new String(this.name), new String(this.content));
        tmpCopy.guid = new String(this.guid);
        return tmpCopy;
    }

    @Override
    public void writeRepresentation(PrintWriter aPrintWriter) {
        aPrintWriter.println(SingleTermPreference.VERSION);
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
     * (Re-)instantiates a new SingleTermPreference object of version 1.0.0.0 from a line-based text file.
     */
    private void reloadVersion1000(BufferedReader aReader) throws Exception {
        this.name = aReader.readLine();
        this.guid = aReader.readLine();
        this.content = aReader.readLine();
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public static methods">
    /**
     * Tests if the given string would be allowed as content of this preference.
     *
     * @param aContent the string to test; leading and trailing whitespaces will not be considered
     * @return true if aContent matches the defined pattern for contents of this preference
     */
    public static boolean isValidContent(String aContent) {
        if (Objects.isNull(aContent)) {
            return false;
        }
        return SingleTermPreference.INPUT_TEST_PATTERN.matcher(aContent.trim()).matches();
    }
    //</editor-fold>
}
