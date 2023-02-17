/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2023  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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
 * A preference for single real numbers.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class SingleNumberPreference extends BasePreference {
    //<editor-fold defaultstate="collapsed" desc="Private static final class constants">
    /**
     * The version of this class.
     */
    private static final String VERSION = "1.0.0.0";

    /**
     * The PreferenceType enum constant associated with this preference implementation.
     */
    private static final PreferenceType TYPE = PreferenceType.SINGLE_NUMBER;

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(SingleNumberPreference.class.getName());
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Private class variables">
    /**
     * Single number content of this preference
     */
    private double content;
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Constructors">
    /**
     * Creates a new SingleNumberPreference instance with the given name and content.
     *
     * @param aName the new preference's name; leading and trailing whitespaces will not be considered;
     * @param aDouble the new preference's content
     * @throws IllegalArgumentException if aName is 'null' or does not match the required pattern or aDouble is infinite
     * or 'NaN'
     */
    public SingleNumberPreference(String aName, double aDouble) throws IllegalArgumentException {
        //<editor-fold defaultstate="collapsed" desc="Checks">
        if (!BasePreference.isValidName(aName)) {
            throw new IllegalArgumentException("Preference name " + aName + " does not match required pattern!");
        }
        if (!SingleNumberPreference.isValidContent(aDouble)) {
            throw new IllegalArgumentException("Given number " + aDouble + " is infinite or 'NaN'.");
        }
        //</editor-fold>
        this.name = aName.trim();
        this.guid = MiscUtil.getGloballyUniqueID();
        this.content = aDouble;
    }

    /**
     * Creates a new SingleNumberPreference instance with the given name and content.
     *
     * @param aName the new preference's name; leading and trailing whitespaces will not be considered;
     * @param aDoubleString the new preference's content; must be fit to be parsed into a double
     * @throws IllegalArgumentException if aName is 'null' or does not match the required pattern or aDoubleString is no
     * valid content
     */
    public SingleNumberPreference(String aName, String aDoubleString) throws IllegalArgumentException {
        //<editor-fold defaultstate="collapsed" desc="Checks">
        if (!BasePreference.isValidName(aName)) {
            throw new IllegalArgumentException("Preference name " + aName + " does not match required pattern!");
        }
        Double tmpDouble = SingleNumberPreference.parseValidContent(aDoubleString);
        if (tmpDouble.isNaN()) {
            throw new IllegalArgumentException("The given number " + aDoubleString + " is no valid content!");
        }
        //</editor-fold>
        this.name = aName.trim();
        this.guid = MiscUtil.getGloballyUniqueID();
        this.content = tmpDouble;
    }

    /**
     * (Re-)instantiates a new SingleNumberPreference object from a line-based text file.
     * <p>
     * Note: The succession of persisted variables must be exactly mirrored in writeRepresentation(PrintWriter).
     *
     * @param aReader to read the persisted representation from
     * @throws java.io.IOException if anything goes wrong
     */
    public SingleNumberPreference(BufferedReader aReader) throws IOException {
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
            SingleNumberPreference.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            throw new IOException("Preference can not be instantiated from given reader.");
        }
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public properties (get)">
    @Override
    public String getContentRepresentative() {
        return Double.toString(this.content);
    }

    @Override
    public String getVersion() {
        return SingleNumberPreference.VERSION;
    }

    @Override
    public PreferenceType getType() {
        return SingleNumberPreference.TYPE;
    }

    /**
     * Returns the single number content of this preference.
     *
     * @return the single number content
     */
    public double getContent() {
        return this.content;
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public properties (set)">
    /**
     * Sets the single number content of this preference.
     *
     * @param aDouble the new content
     * @throws IllegalArgumentException if aDouble is infinite or 'NaN'
     */
    public void setContent(double aDouble) throws IllegalArgumentException {
        if (!SingleNumberPreference.isValidContent(aDouble)) {
            throw new IllegalArgumentException("Given number " + aDouble + " is infinite or 'NaN'.");
        }
        this.content = aDouble;
    }

    /**
     * Sets the single number content of this preference.
     *
     * @param aDoubleString the new content; must be fit to be parsed into a double
     * @throws IllegalArgumentException if aDoubleString is 'null', can not be parsed into a double or the resulting
     * double is infinite or 'NaN'
     */
    public void setContent(String aDoubleString) throws IllegalArgumentException {
        Double tmpDouble = SingleNumberPreference.parseValidContent(aDoubleString);
        if (tmpDouble.isNaN()) {
            throw new IllegalArgumentException("The given number " + aDoubleString + " is no valid content!");
        }
        this.content = tmpDouble;
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public methods">
    @Override
    public IPreference clone() throws CloneNotSupportedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public SingleNumberPreference copy() {
        SingleNumberPreference tmpCopy = new SingleNumberPreference(new String(this.name), Double.valueOf(this.content));
        tmpCopy.guid = new String(this.guid);
        return tmpCopy;
    }

    @Override
    public void writeRepresentation(PrintWriter aPrintWriter) {
        aPrintWriter.println(SingleNumberPreference.VERSION);
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
     * Tests if the given number would be allowed as a content of this preference type.
     *
     * @param aContent the number to test; must not be positive or negative infinity or NaN
     * @return true if aContent meets the requirements for contents of this preference
     */
    public static boolean isValidContent(double aContent) {
        Double tmpDouble = aContent;
        return !(tmpDouble.isInfinite() || tmpDouble.isNaN());
    }

    /**
     * Tests if the given string would be allowed as a content of this preference type.
     *
     * @param aDoubleString the string to test; leading and trailing whitespaces will not be considered; It must not be
     * null, empty, "Infinity", "-Infinity" or "NaN" and has to be able to be parsed into a double without producing negative or
     * positive infinity or NaN
     * @return true if aDoubleString meets the requirements for contents of this preference
     */
    public static boolean isValidContent(String aDoubleString) {
        if (Objects.isNull(aDoubleString) || aDoubleString.isEmpty()) {
            return false;
        }
        Double tmpResult = SingleNumberPreference.parseValidContent(aDoubleString);
        return !tmpResult.isNaN();
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Private methods">
    /**
     * (Re-)instantiates a new SingleNumberPreference object of version 1.0.0.0 from a line-based text file.
     */
    private void reloadVersion1000(BufferedReader aReader) throws Exception {
        this.name = aReader.readLine();
        this.guid = aReader.readLine();
        this.content = Double.parseDouble(aReader.readLine());
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Private static methods">
    /**
     * Returns a double parsed from the trimmed input or Double.NaN if the input string is no valid content for this
     * preference.
     */
    private static double parseValidContent(String aDoubleString) {
        if (Objects.isNull(aDoubleString)) {
            return Double.NaN;
        }
        String tmpContentString = aDoubleString.trim();
        //These strings ("-Infinity", "Infinity" and "NaN") would be parsed into the matching constants of class Double
        if (tmpContentString.equals("Infinity")
                || tmpContentString.equals("-Infinity")
                || tmpContentString.equals("NaN")) {
            return Double.NaN;
        }
        double tmpSingleNumber;
        try {
            tmpSingleNumber = Double.parseDouble(tmpContentString);
        } catch (NumberFormatException aNumberFormatException) {
            SingleNumberPreference.LOGGER.log(Level.SEVERE, aNumberFormatException.toString(), aNumberFormatException);
            return Double.NaN;
        }
        if (SingleNumberPreference.isValidContent(tmpSingleNumber)) {
            return tmpSingleNumber;
        } else {
            return Double.NaN;
        }
    }
    //</editor-fold>
}