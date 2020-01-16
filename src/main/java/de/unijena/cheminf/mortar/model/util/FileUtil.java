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

package de.unijena.cheminf.mortar.model.util;

import java.io.File;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * File utility
 *
 * @author Achim Zielesny, Jonas Schaub
 */
public class FileUtil {
    //<editor-fold defaultstate="collapsed" desc="Public static final class constants">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(FileUtil.class.getName());
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public static methods">
    /**
     * Returns the extension of the file represented by the given pathname. If no file extension is specified, an empty
     * string is returned.
     *
     * @param aFilePathname representing the file whose extension should be extracted
     * @return extension of the file represented by the given pathname; an empty string if no file extension is specified
     * @throws IllegalArgumentException if given file pathname is empty
     * @throws NullPointerException if given file pathname is 'null'
     */
    public static String getFileExtension(String aFilePathname) throws NullPointerException, IllegalArgumentException {
        //<editor-fold defaultstate="collapsed" desc="Checks">
        Objects.requireNonNull(aFilePathname, "Given file pathname is 'null'.");
        if (aFilePathname.isEmpty()) {
            throw new IllegalArgumentException("Given file pathname is empty.");
        }
        //</editor-fold>
        File tmpFile = new File(aFilePathname);
        String tmpFilename = tmpFile.getName();
        int tmpLastIndexOfDot = tmpFilename.lastIndexOf(".");
        if (tmpLastIndexOfDot == -1) {
            return "";
        }
        String tmpFileExtension = tmpFilename.substring(tmpLastIndexOfDot);
        return tmpFileExtension;
    }

    /**
     * Deletes single file
     *
     * @param aFilePathname Full pathname of file to be deleted (may be null
     * then false is returned)
     * @return true: Operation was successful, false: Operation failed
     */
    public static boolean deleteSingleFile(
            String aFilePathname
    ) {
        // <editor-fold defaultstate="collapsed" desc="Checks">
        if (aFilePathname == null ||
                aFilePathname.isEmpty()
        ) {
            return false;
        }
        // </editor-fold>
        try {
            File tmpFile = new File(aFilePathname);
            if (!tmpFile.isFile()) {
                return true;
            } else {
                return tmpFile.delete();
            }
        } catch (Exception anException) {
            FileUtil.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return false;
        }
    }

    /**
     * Creates empty file
     *
     * @param aFilePathname Full file pathname
     * @return True: Empty file was created, false: Otherwise
     */
    public static boolean createEmptyFile(String aFilePathname) {
        // <editor-fold defaultstate="collapsed" desc="Checks">
        if (aFilePathname == null || aFilePathname.isEmpty()) {
            return false;
        }
        if ((new File(aFilePathname)).isFile()) {
            return false;
        }
        // </editor-fold>
        try {
            File tmpFile = new File(aFilePathname);
            return tmpFile.createNewFile();
        } catch (Exception anException) {
            FileUtil.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return false;
        }
    }

    /**
     * Returns data path
     *
     * @return Data path
     */
    public static String getDataPath() {
        String tmpDataPath;
        if (BasicDefinitions.LOCAL_USER_DATA_DIRECTORY_PATH != null) {
            tmpDataPath = BasicDefinitions.LOCAL_USER_DATA_DIRECTORY_PATH + File.separatorChar + BasicDefinitions.MORTAR_DATA_DIRECTORY;
        } else {
            tmpDataPath = BasicDefinitions.USER_DIRECTORY_PATH + File.separatorChar + BasicDefinitions.MORTAR_DATA_DIRECTORY;
        }
        return tmpDataPath;
    }
    // </editor-fold>
}