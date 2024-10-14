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

package de.unijena.cheminf.mortar.model.util;

import java.util.regex.Pattern;

/**
 * Basic definitions of the model or application.
 *
 * @author Jonas Schaub, Achim Zielesny
 * @version 1.0.0.0
 */
public final class BasicDefinitions {
    //<editor-fold defaultstate="collapsed" desc="Private constructor">
    /**
     * Private parameter-less constructor.
     * Introduced because javadoc build complained about classes without declared default constructor.
     */
    private BasicDefinitions() {
    }
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Minimum system requirements">
    /**
     * Minimum java version to run MORTAR.
     */
    public static final String MINIMUM_JAVA_VERSION = "21.0.1";
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Basic patterns and formats">
    /**
     * Regex pattern for non-word or non-numeric characters.
     */
    public static final Pattern NON_WORDNUMERIC_CHARACTER_PATTERN = Pattern.compile("[^a-zA-Z0-9]");
    /**
     * Standard timestamp format.
     */
    public static final String STANDARD_TIMESTAMP_FORMAT = "yyyy/MM/dd - HH:mm:ss";
    /**
     * Timestamp format for filename extensions.
     */
    public static final String FILENAME_TIMESTAMP_FORMAT = "uuuu_MM_dd_HH_mm";
    /**
     * Upper limit of bytes used in the log-file folder for entering the managing routine (1 MByte = 1048576 Byte).
     */
    public static final int LIMIT_OF_BYTES_USED_BY_LOG_FILES = 1048576;
    /**
     * Upper limit of files stored in the log-file folder for entering the managing routine.
     */
    public static final int UPPER_LIMIT_OF_LOG_FILES = 64;
    /**
     * Lower limit of files stored in the log-file folder for entering the managing routine.
     */
    public static final int LOWER_LIMIT_OF_LOG_FILES = 32;
    /**
     * Factor by which to trim the log-file folder.
     */
    public static final double FACTOR_TO_TRIM_LOG_FILE_FOLDER = 0.2;
    //</editor-fold>
    //
    // <editor-fold defaultstate="collapsed" desc="Buffer">
    /**
     * Buffer size (64 kByte = 65536, 256 kByte = 262144, 512 kByte = 524288, 1 MByte = 1048576 Byte).
     */
    public static final int BUFFER_SIZE = 65536;
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="System and directories">
    /**
     * Note, the file extension (.txt or .gzip) defines whether the preference container file is compressed or not.
     * For the moment, we are not using compression here in MORTAR for persisting settings.
     */
    public static final String PREFERENCE_CONTAINER_FILE_EXTENSION = ".txt";
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Session">
    /**
     * MORTAR session start log entry.
     */
    public static final String MORTAR_SESSION_START_FORMAT = "MORTAR %s session start";
    /**
     * MORTAR session end log entry.
     */
    public static final String MORTAR_SESSION_END = "MORTAR session end";
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Application version">
    /**
     * Version of application.
     */
    public static final String MORTAR_VERSION = "1.2.0.0";
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Model constants and magic numbers">
    /**
     * Default initial capacity for fragment maps.
     */
    public static final int DEFAULT_INITIAL_MAP_CAPACITY = 25;
    /**
     * Default width for images.
     */
    public static final double DEFAULT_IMAGE_WIDTH_DEFAULT = 250.0;
    /**
     * Default height for images.
     */
    public static final double DEFAULT_IMAGE_HEIGHT_DEFAULT = 250.0;
    /**
     * Default distance between image and text.
     */
    public static final int DEFAULT_IMAGE_TEXT_DISTANCE = 15;
    /**
     * Default load factor for HashMap and HashSet instances, defined based on default value given in the Java documentation.
     */
    public static final float DEFAULT_HASH_COLLECTION_LOAD_FACTOR = 0.75f;
    //</editor-fold>
}
