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

import de.unijena.cheminf.mortar.configuration.Configuration;
import de.unijena.cheminf.mortar.configuration.IConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * File utility.
 *
 * @author Achim Zielesny
 * @author Jonas Schaub
 * @author Felix Baensch
 * @version 1.0.0.0
 */
public final class FileUtil {
    //<editor-fold defaultstate="collapsed" desc="Private enum OperatingSystem">
    /**
     * Represents the three major operating systems used internally for app directory path determination
     * and file name validation and sanitization.
     */
    private enum OperatingSystem {
        /** Microsoft Windows. */
        WINDOWS,
        /** Apple macOS. */
        MAC_OS,
        /** Linux and other Unix-like systems (AIX, etc.). */
        LINUX
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private static class variables" defaultstate="collapsed">
    /**
     * Cache String for app dir path.
     */
    private static String appDirPath = null;
    //</editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Public static final class constants">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(FileUtil.class.getName());
    //
    /**
     * Configuration class to read resource file paths from.
     */
    private static final IConfiguration CONFIGURATION;
    static {
        try {
            CONFIGURATION = Configuration.getInstance();
        } catch (IOException anIOException) {
            //when MORTAR is run via MainApp.start(), the correct initialization of Configuration is checked there before
            // FileUtil is accessed and this static initializer called
            throw new NullPointerException("Configuration could not be initialized");
        }
    }
    //
    /**
     * Compiled regex pattern that matches characters illegal in a file name on <b>Windows</b>:
     * {@code \ / : * ? " < > |} and control characters U+0000–U+001F.
     */
    private static final Pattern ILLEGAL_FILE_NAME_CHARACTERS_PATTERN_WINDOWS =
            Pattern.compile("[\\\\/:*?\"<>|\\x00-\\x1F]");
    //
    /**
     * Compiled regex pattern that matches characters illegal in a file name on <b>macOS</b>:
     * {@code /} and {@code :} (and the NUL character U+0000).
     */
    private static final Pattern ILLEGAL_FILE_NAME_CHARACTERS_PATTERN_MAC_OS =
            Pattern.compile("[/:\\x00]");
    //
    /**
     * Compiled regex pattern that matches characters illegal in a file name on <b>Linux/Unix</b>:
     * {@code /} and the NUL character (U+0000).
     */
    private static final Pattern ILLEGAL_FILE_NAME_CHARACTERS_PATTERN_LINUX =
            Pattern.compile("[/\\x00]");
    //</editor-fold>
    //
    //<editor-fold desc="Private constructor" defaultstate="collapsed">
    /**
     * Private parameter-less constructor.
     * Introduced because javadoc build complained about classes without declared default constructor.
     */
    private FileUtil() {
    }
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
        int tmpLastIndexOfDot = tmpFilename.lastIndexOf('.');
        if (tmpLastIndexOfDot == -1) {
            return "";
        }
        String tmpFileExtension = tmpFilename.substring(tmpLastIndexOfDot);
        return tmpFileExtension;
    }

    /**
     * Returns the name of the file without the file extension.
     *
     * @param aFile whose name should be returned without extension
     * @return file name without extension
     * @throws NullPointerException if given file is 'null'
     */
    public static String getFileNameWithoutExtension(File aFile) throws NullPointerException{
        //<editor-fold defaultstate="collapsed" desc="Checks">
        Objects.requireNonNull(aFile, "Given file is 'null'.");
        //</editor-fold>
        return aFile.getName().replaceFirst("[.][^.]+$", "");
    }

    /**
     * Deletes single file.
     *
     * @param aFilePathname Full pathname of file to be deleted (may be null
     * then false is returned)
     * @return true: Operation was successful, false: Operation failed
     */
    public static boolean deleteSingleFile(String aFilePathname) {
        // <editor-fold defaultstate="collapsed" desc="Checks">
        if (aFilePathname == null || aFilePathname.isEmpty()) {
            return false;
        }
        // </editor-fold>
        try {
            File tmpFile = new File(aFilePathname);
            if (tmpFile.isFile()) {
                Files.delete(tmpFile.toPath());
            }
            //if it is not a file, it does not exist and therefore must not be deleted
            //if delete fails, it goes to catch, logs the detailed exception and returns false
            return true;
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
        boolean tmpAllFilesDeletedSuccessfully = true;
        try {
            File tmpDirectory = new File(aDirectoryPath);
            if (!tmpDirectory.isDirectory()) {
                return false;
            }
            File[] tmpFilesArray = tmpDirectory.listFiles();
            for (File tmpFile : tmpFilesArray) {
                if (tmpFile.isFile()) {
                    Files.delete(tmpFile.toPath());
                }
            }
        } catch (Exception anException) {
            FileUtil.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            tmpAllFilesDeletedSuccessfully = false;
        }
        return tmpAllFilesDeletedSuccessfully;
    }

    /**
     * Creates directory and all non-existent ancestor directories if necessary.
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
     * Creates empty file.
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
     * Returns data path of the application (depending on OS). If the path does not exist yet, it will be created.
     *
     * @return Data path
     * @throws SecurityException if the OS name is unknown, the AppData directory (Windows) or the user home directory
     * path cannot be determined or data directory cannot be created
     */
    public static String getAppDirPath() throws SecurityException {
        if (FileUtil.appDirPath != null) {
            return FileUtil.appDirPath;
        }
        String tmpAppDir;
        OperatingSystem tmpOS = FileUtil.detectCurrentOperatingSystem();
        if (tmpOS.equals(OperatingSystem.WINDOWS))
            tmpAppDir = System.getenv("AppData");
        else if (tmpOS.equals(OperatingSystem.MAC_OS) || tmpOS.equals(OperatingSystem.LINUX))
            tmpAppDir = System.getProperty("user.home");
        else
            throw new SecurityException("OS name " + tmpOS + " unknown.");
        File tmpAppDirFile = new File(tmpAppDir);
        if(!tmpAppDirFile.exists() || !tmpAppDirFile.isDirectory())
            throw new SecurityException("AppData (Windows) or user home directory path " + tmpAppDir + " is either no directory or does not exist.");
        if (tmpOS.equals(OperatingSystem.MAC_OS))
            tmpAppDir += File.separator + "Library" + File.separator + "Application Support";
        tmpAppDir += File.separator + FileUtil.CONFIGURATION.getProperty("mortar.vendor.name") + File.separator + FileUtil.CONFIGURATION.getProperty("mortar.dataDirectory.name");
        tmpAppDirFile = new File(tmpAppDir);
        boolean tmpSuccessful = true;
        if (!tmpAppDirFile.exists())
            tmpSuccessful = tmpAppDirFile.mkdirs();
        if (!tmpSuccessful)
            throw new SecurityException("Unable to create application data directory");
        FileUtil.appDirPath = tmpAppDir;
        return FileUtil.appDirPath;
    }

    /**
     * Returns the path to the folder where all settings (global, fragmenter, pipeline) are persisted.
     *
     * @return settings folder path
     * @throws SecurityException if the OS name is unknown, the AppData directory (Windows) or the user home directory
     * path cannot be determined or data directory cannot be created
     */
    public static String getSettingsDirPath() throws SecurityException {
        return FileUtil.getAppDirPath() + File.separator + FileUtil.CONFIGURATION.getProperty("mortar.settingsDirectory.name") + File.separator;
    }

    /**
     * Returns a timestamp to add to a filename.
     *
     * @return timestamp filename extension or a placeholder string if time stamp creation failed
     */
    public static String getTimeStampFileNameExtension() throws DateTimeException {
        try {
            LocalDateTime tmpDateTime = LocalDateTime.now();
            String tmpDateTimeAddition = tmpDateTime.format(DateTimeFormatter.ofPattern(BasicDefinitions.FILENAME_TIMESTAMP_FORMAT));
            return tmpDateTimeAddition;
        } catch (Exception e) {
            FileUtil.LOGGER.log(Level.WARNING, e.toString(), e);
            return BasicDefinitions.FILENAME_TIMESTAMP_FORMAT;
        }
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
        String tmpFileExtension = aFileExtension;
        if (Objects.isNull(tmpFileExtension)) {
            tmpFileExtension = "";
        }
        int tmpFilesInThisMinuteCounter = 1;
        File tmpFile = new File(aFilePath + tmpFileExtension);
        if (tmpFile.exists()) {
            while (true) {
                tmpFile = new File(aFilePath + "(" + tmpFilesInThisMinuteCounter + ")" + tmpFileExtension);
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
            return aFilePath + tmpFileExtension;
        }
    }
    //
    /**
     * Checks whether the given file name contains characters that are illegal on the current operating system.
     * The operating system is detected automatically. The characters considered illegal are:
     * <ul>
     *     <li>Windows: {@code \ / : * ? " < > |} and control characters U+0000–U+001F</li>
     *     <li>macOS: {@code /} and {@code :} (and the NUL character U+0000)</li>
     *     <li>Linux/Unix: {@code /} and the NUL character (U+0000)</li>
     * </ul>
     *
     * @param aFileName the file name (not a full path) to check; must not be {@code null} or empty
     * @return {@code true} if the file name contains at least one illegal character, {@code false} otherwise
     * @throws NullPointerException     if the given file name is {@code null}
     * @throws IllegalArgumentException if the given file name is empty
     */
    public static boolean containsIllegalFileNameCharacters(String aFileName)
            throws NullPointerException, IllegalArgumentException {
        //<editor-fold defaultstate="collapsed" desc="Checks">
        Objects.requireNonNull(aFileName, "Given file name is 'null'.");
        if (aFileName.isEmpty()) {
            throw new IllegalArgumentException("Given file name is empty.");
        }
        //</editor-fold>
        return FileUtil.getPatternForOperatingSystem(FileUtil.detectCurrentOperatingSystem()).matcher(aFileName).find();
    }

    /**
     * Replaces every character in the given file name that is illegal on the current operating system with the
     * supplied replacement string. The operating system is detected automatically. The characters considered
     * illegal are:
     * <ul>
     *     <li>Windows: {@code \ / : * ? " < > |} and control characters U+0000–U+001F</li>
     *     <li>macOS: {@code /} and {@code :} (and the NUL character U+0000)</li>
     *     <li>Linux/Unix: {@code /} and the NUL character (U+0000)</li>
     * </ul>
     *
     * @param aFileName    the file name (not a full path) to sanitize; must not be {@code null} or empty
     * @param aReplacement the string used as a substitute for each illegal character; may be an empty string
     *                     (effectively removing illegal characters) but must not be {@code null}
     * @return the sanitized file name with all illegal characters replaced by {@code aReplacement}
     * @throws NullPointerException     if {@code aFileName} or {@code aReplacement} is {@code null}
     * @throws IllegalArgumentException if {@code aFileName} is empty
     */
    public static String replaceIllegalFileNameCharacters(String aFileName, String aReplacement)
            throws NullPointerException, IllegalArgumentException {
        //<editor-fold defaultstate="collapsed" desc="Checks">
        Objects.requireNonNull(aFileName, "Given file name is 'null'.");
        Objects.requireNonNull(aReplacement, "Given replacement string is 'null'.");
        if (aFileName.isEmpty()) {
            throw new IllegalArgumentException("Given file name is empty.");
        }
        //</editor-fold>
        return FileUtil.getPatternForOperatingSystem(FileUtil.detectCurrentOperatingSystem()).matcher(aFileName)
                .replaceAll(Matcher.quoteReplacement(aReplacement));
    }
    /**
     * Opens given path in OS depending explorer equivalent.
     *
     * @param aPath path to open
     * @throws IllegalArgumentException if the given path is empty, blank, or null
     * @throws SecurityException if the directory could not be opened
     */
    public static void openFilePathInExplorer(String aPath) throws SecurityException {
        if (Objects.isNull(aPath) || aPath.isBlank()) {
            throw new IllegalArgumentException("Given file path is null, empty, or blank.");
        }
        OperatingSystem tmpOS = FileUtil.detectCurrentOperatingSystem();
        try {
            if (tmpOS.equals(OperatingSystem.WINDOWS)) {
                Runtime.getRuntime().exec(new String[]{"explorer", "/open,", aPath});
            } else if (tmpOS.equals(OperatingSystem.MAC_OS)) {
                Runtime.getRuntime().exec(new String[]{"open", "-R", aPath});
            } else if (tmpOS.equals(OperatingSystem.LINUX)) {
                Runtime.getRuntime().exec(new String[]{"gio", "open", aPath});
            } else {
                throw new SecurityException("OS name " + tmpOS + " unknown.");
            }
        } catch (IOException anException) {
            FileUtil.LOGGER.log(Level.SEVERE, anException.toString(), anException);
            throw new SecurityException("Could not open directory path");
        }
    }
    // </editor-fold>
    //
    //<editor-fold defaultstate="collapsed" desc="Private static methods">
    /**
     * Detects the current operating system and returns the matching {@link OperatingSystem} enum constant.
     *
     * @return the {@link OperatingSystem} constant for the current platform
     * @throws SecurityException if the OS name is unknown
     */
    private static OperatingSystem detectCurrentOperatingSystem() throws SecurityException {
        // In locales like Turkish, the case conversion for "i" can change and break the subsequent checks,
        // leading to false "unknown OS" errors; therefore, the English locale is used here
        String tmpOS = System.getProperty("os.name").toUpperCase(Locale.ENGLISH);
        if (tmpOS.contains("WIN")) {
            return OperatingSystem.WINDOWS;
        }
        if (tmpOS.contains("MAC")) {
            return OperatingSystem.MAC_OS;
        }
        if (tmpOS.contains("NUX") || tmpOS.contains("NIX") || tmpOS.contains("AIX")) {
            return OperatingSystem.LINUX;
        }
        throw new SecurityException("OS name " + tmpOS + " unknown.");
    }

    /**
     * Returns the pre-compiled {@link Pattern} that matches illegal file name characters for the given
     * {@link OperatingSystem}.
     *
     * @param anOperatingSystem the target operating system; must not be {@code null}
     * @return the corresponding illegal-character pattern
     */
    private static Pattern getPatternForOperatingSystem(OperatingSystem anOperatingSystem) {
        return switch (anOperatingSystem) {
            case WINDOWS -> FileUtil.ILLEGAL_FILE_NAME_CHARACTERS_PATTERN_WINDOWS;
            case MAC_OS  -> FileUtil.ILLEGAL_FILE_NAME_CHARACTERS_PATTERN_MAC_OS;
            case LINUX   -> FileUtil.ILLEGAL_FILE_NAME_CHARACTERS_PATTERN_LINUX;
        };
    }
    //</editor-fold>
}
