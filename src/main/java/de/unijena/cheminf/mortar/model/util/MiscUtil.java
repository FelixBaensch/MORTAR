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

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Miscellaneous utilities.
 *
 * @author Achim Zielesny
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public final class MiscUtil {
    //<editor-fold defaultstate="collapsed" desc="Public static final class constants">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(MiscUtil.class.getName());
    //
    /**
     * Maximum length of the prefixes used for the IDs of molecules and fragments (file or fragmentation name).
     */
    public static final int ID_PREFIX_MAX_LENGTH = 40;
    //
    /**
     * Prefix used for IDs if the given base name does not contain any usable characters.
     */
    public static final String ID_PREFIX_DEFAULT = "MORTAR";
    //</editor-fold>
    //
    //<editor-fold desc="Private constructor" defaultstate="collapsed">
    /**
     * Private parameter-less constructor.
     * Introduced because javadoc build complained about classes without declared default constructor.
     */
    private MiscUtil() {
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public static methods">
    /**
     * Creates a prefix for the IDs of molecules or fragments from the given base name, e.g. the import file name
     * (without extension) or the fragmentation name. All characters other than letters, digits, '_', and '-' are
     * replaced by '_', consecutive '_' are merged, and leading or trailing '_' removed. The result is truncated to the
     * given maximum length. If no usable characters remain, {@link #ID_PREFIX_DEFAULT} is returned.
     * <br>The result never contains characters that are used as CSV separators in MORTAR.
     *
     * @param aBaseName name to derive the prefix from
     * @param aMaxLength maximum length of the returned prefix
     * @return ID prefix
     * @throws IllegalArgumentException if the given maximum length is smaller than 1
     */
    public static String createIDPrefix(String aBaseName, int aMaxLength) throws IllegalArgumentException {
        if (aMaxLength < 1) {
            throw new IllegalArgumentException("Given maximum length must be at least 1.");
        }
        if (aBaseName == null) {
            return MiscUtil.ID_PREFIX_DEFAULT;
        }
        String tmpPrefix = aBaseName.replaceAll("[^A-Za-z0-9_-]+", "_").replaceAll("_{2,}", "_");
        tmpPrefix = MiscUtil.stripUnderscores(tmpPrefix);
        if (tmpPrefix.length() > aMaxLength) {
            tmpPrefix = MiscUtil.stripUnderscores(tmpPrefix.substring(0, aMaxLength));
        }
        return tmpPrefix.isEmpty() ? MiscUtil.ID_PREFIX_DEFAULT : tmpPrefix;
    }
    //
    /**
     * String with globally unique ID.
     *
     * @return String with globally unique ID
     */
    public static String getGloballyUniqueID() {
        return BasicDefinitions.NON_WORDNUMERIC_CHARACTER_PATTERN.matcher((UUID.randomUUID()).toString()).replaceAll("");
    }
    //
    /**
     * Returns current timestamp in standard form (see code).
     *
     * @return Current timestamp in standard form or a placeholder string if none could be created
     */
    public static String getTimestampInStandardFormat() {
        try {
            SimpleDateFormat tmpSimpleDateFormat = new SimpleDateFormat(BasicDefinitions.STANDARD_TIMESTAMP_FORMAT);
            Instant tmpInstant = Instant.now();
            Date tmpDate = Date.from(tmpInstant);
            return tmpSimpleDateFormat.format(tmpDate);
        } catch (Exception anException) {
            MiscUtil.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return BasicDefinitions.STANDARD_TIMESTAMP_FORMAT;
        }
    }
    //
    /**
     * Compares two version strings of the form "x.y.z" of variable length position by position after splitting at each
     * "." and parsing to integers. The result is analogous to what Integer.compare(int a, int b) returns for the
     * first position that differs between the two version strings. If they are of different size and the shorter
     * one is a substring of the longer one starting at position 0, their lengths are compared.
     *
     * @param aVersionString1 one version string v1
     * @param aVersionString2 another version string v2
     * @return the value 0 if v1 == v2; a value less than 0 if v1 smaller than v2; and a value greater than 0 if v1 greater than v2
     * @throws IllegalArgumentException if one of the parameters is null, empty, or blank
     * @author Jonas Schaub
     */
    public static int compareVersions(String aVersionString1, String aVersionString2) throws IllegalArgumentException {
        if (Objects.isNull(aVersionString1) || aVersionString1.isEmpty() || aVersionString1.isBlank()
                || Objects.isNull(aVersionString2) || aVersionString2.isEmpty() || aVersionString2.isBlank()) {
            throw new IllegalArgumentException("One of the arguments is null, empty or blank.");
        }
        String[] tmpSeparateNumbersV1 = aVersionString1.split("\\.");
        String[] tmpSeparateNumbersV2 = aVersionString2.split("\\.");
        int tmpIterations = Math.min(tmpSeparateNumbersV1.length, tmpSeparateNumbersV2.length);
        for (int i = 0; i < tmpIterations; i++) {
            int tmpV1Int = Integer.parseInt(tmpSeparateNumbersV1[i]);
            int tmpV2Int = Integer.parseInt(tmpSeparateNumbersV2[i]);
            int tmpResult = Integer.compare(tmpV1Int, tmpV2Int);
            if (tmpResult != 0) {
                return tmpResult;
            }
        }
        return Integer.compare(tmpSeparateNumbersV1.length, tmpSeparateNumbersV2.length);
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Private static methods">
    /**
     * Removes all leading and trailing underscores from the given string.
     *
     * @param aString string to process
     * @return the string without leading and trailing underscores
     */
    private static String stripUnderscores(String aString) {
        return aString.replaceAll("^_+|_+$", "");
    }
    //</editor-fold>
}
