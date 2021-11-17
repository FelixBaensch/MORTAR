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
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * File utility
 *
 * @author Achim Zielesny, Jonas Schaub, Felix Baensch
 */
public final class FileUtil {
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
     * Returns the name of the file without the file extension.
     *
     * @param aFile whose name should ne return without extension
     * @return file name without extension
     * @throws NullPointerException if given file is 'null'
     */
    public static String getFileNameWithoutExtension(File aFile) throws NullPointerException{
        //<editor-fold defaultstate="collapsed" desc="Checks">
        Objects.requireNonNull(aFile, "Given file is 'null'.");
        //</editor-fold>
        return aFile.getName().replaceFirst("[.][^.]+$", ""); //cuts the
    }

    /**
     * Deletes single file
     *
     * @param aFilePathname Full pathname of file to be deleted (may be null
     * then false is returned)
     * @return true: Operation was successful, false: Operation failed
     */
    public static boolean deleteSingleFile(String aFilePathname) {
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
     * Deletes all files in the given directory but NOT in any subdirectory. Returns true if all files have been deleted
     * successfully.
     *
     * @param aDirectoryPath path to the directory
     * @return true if all files have been deleted successfully
     */
    public static boolean deleteAllFilesInDirectory(String aDirectoryPath) {
        // <editor-fold defaultstate="collapsed" desc="Checks">
        if (aDirectoryPath == null ||
                aDirectoryPath.isEmpty()
        ) {
            return false;
        }
        // </editor-fold>
        try {
            File tmpDirectory = new File(aDirectoryPath);
            if (!tmpDirectory.isDirectory()) {
                return false;
            }
            File[] tmpFilesArray = tmpDirectory.listFiles();
            boolean tmpAllFilesDeletedSuccessfully = true;
            for (File tmpFile : tmpFilesArray) {
                if (tmpFile.isFile()) {
                    boolean tmpFileDeleted = tmpFile.delete();
                    if (!tmpFileDeleted) {
                        tmpAllFilesDeletedSuccessfully = false;
                    }
                }
            }
            return tmpAllFilesDeletedSuccessfully;
        } catch (Exception anException) {
            FileUtil.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            return false;
        }
    }

    /**
     * Creates directory and all non-existent ancestor directories if necessary
     *
     * @param aDirectoryPath Full directory path to be created
     * @return true: Directory already existed or was successfully created,
     * false: Otherwise
     */
    public static boolean createDirectory(String aDirectoryPath) {
        // <editor-fold defaultstate="collapsed" desc="Checks">
        if (aDirectoryPath == null ||
                aDirectoryPath.isEmpty()
        ) {
            return false;
        }
        // </editor-fold>
        try {
            File tmpDirectory = new File(aDirectoryPath);
            if (!tmpDirectory.isDirectory()) {
                return tmpDirectory.mkdirs();
            }
            return true;
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

    //TODO: If we put the path in a static variable not to determine the OS etc. every time, would this create a bottle neck at parallelization?
    // "AppDirPath" should be set initially once (maybe as a preference), since it does not change during the application and therefore does not have to be queried several times using this method
    /**
     * Returns data path of the application (depending on OS). If the path does not exist yet, it will be created.
     *
     * @return Data path
     * @throws SecurityException if the OS name is unknown, the AppData directory (Windows) or the user home directory
     * path cannot be determined or data directory cannot be created
     */
    public static String getAppDirPath() throws SecurityException {
        String tmpAppDir;
        String tmpOS = System.getProperty("os.name").toUpperCase();
        if (tmpOS.contains("WIN"))
            tmpAppDir = System.getenv("AppData");
        else if (tmpOS.contains("MAC"))
            tmpAppDir = System.getenv("user.home");
        else if (tmpOS.contains("NUX") || tmpOS.contains("NIX") || tmpOS.contains("AIX"))
            tmpAppDir = System.getenv("user.home");
        else
            throw new SecurityException("OS name " + tmpOS + " unknown.");
        File tmpAppDirFile = new File(tmpAppDir);
        if(!tmpAppDirFile.exists() || !tmpAppDirFile.isDirectory())
            throw new SecurityException("AppData (Windows) or user home directory path " + tmpAppDir + " is either no directory or does not exist.");
        if (tmpOS.contains("MAC"))
            tmpAppDir += File.separator + "Library" + File.separator + "Application Support";
        tmpAppDir += File.separator + BasicDefinitions.MORTAR_VENDOR + File.separator + BasicDefinitions.MORTAR_DATA_DIRECTORY;
        tmpAppDirFile = new File(tmpAppDir);
        boolean tmpSuccessful = true;
        if(!tmpAppDirFile.exists())
            tmpSuccessful = tmpAppDirFile.mkdirs();
        if (!tmpSuccessful)
            throw new SecurityException("Unable to create application data directory");
        return tmpAppDir;
    }

    /**
     * TODO
     */
    public static String getSettingsDirPath() throws SecurityException {
        return FileUtil.getAppDirPath() + File.separator + BasicDefinitions.SETTINGS_CONTAINER_FILE_DIRECTORY + File.separator;
    }

    /**
     * Returns a timestamp to add to a filename.
     *
     * @throws DateTimeException if the time cannot be determined or formatted
     * @return timestamp filename extension
     */
    public static String getTimeStampFileNameExtension() throws DateTimeException {
        LocalDateTime tmpDateTime = LocalDateTime.now();
        String tmpDateTimeAddition = tmpDateTime.format(DateTimeFormatter.ofPattern(
                BasicDefinitions.FILENAME_TIMESTAMP_FORMAT));
        return tmpDateTimeAddition;
    }

    /**
     * Checks whether a file with the given path already exists and adds a number to the given path to make it non-existing
     * if it does.
     *
     * @param aFilePath file path to check
     * @param aFileExtension the file name extension, may be empty or null; must start with '.', so e.g. ".txt"
     * @return the given file path either unchanged or with an added number to make it non-existing
     * @throws IllegalArgumentException if aFilePath parameter is null, empty or does not represent a file but a directory; also
     * if there are already more than [Integer.MAX-VALUE] files with that name existing
     */
    public static String getNonExistingFilePath(String aFilePath, String aFileExtension) throws IllegalArgumentException {
        //<editor-fold desc="Checks">
        if (Objects.isNull(aFilePath) || aFilePath.isEmpty())
            throw new IllegalArgumentException("Given file path is null or empty.");
        char tmpLastChar = aFilePath.charAt(aFilePath.length() - 1);
        if (tmpLastChar == File.separatorChar)
            throw new IllegalArgumentException("Given file path is a directory.");
        //</editor-fold>
        String tmpFilePath = aFilePath;
        String tmpFileExtension = aFileExtension;
        if (Objects.isNull(tmpFileExtension)) {
            tmpFileExtension = "";
        }
        int tmpFilesInThisMinuteCounter = 1;
        File tmpFile = new File(tmpFilePath+ tmpFileExtension);
        if (tmpFile.exists()) {
            while (tmpFilesInThisMinuteCounter <= Integer.MAX_VALUE) {
                tmpFile = new File(tmpFilePath + "(" + tmpFilesInThisMinuteCounter + ")" + tmpFileExtension);
                if (!tmpFile.exists()) {
                    break;
                }
                if (tmpFilesInThisMinuteCounter == Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("More than [Integer.MAX-VALUE] files with this name already exist.");
                }
                tmpFilesInThisMinuteCounter++;
            }
            String tmpNonExistingFilePath = tmpFile.getPath();
            return tmpNonExistingFilePath;
        } else {
            return tmpFilePath + tmpFileExtension;
        }
    }
    // </editor-fold>
}