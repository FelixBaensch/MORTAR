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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Tests for the utility functions in FileUtil. The deterministic helpers are exercised directly; the file/directory
 * operations are isolated to a JUnit {@link TempDir} so nothing is written outside the temporary directory; the
 * environment-coupled {@code getAppDirPath} method is driven exactly once with the {@code user.home} system property
 * redirected to a temporary directory and the private static {@code appDirPath} cache reflectively reset, with the
 * original state always restored in a finally block (so the real user home is never polluted).
 *
 * @author Felix Baensch
 */
class FileUtilTest {
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Constructor that sets the default locale to en-GB (so any message-bundle resolution is deterministic) and
     * bootstraps the Configuration singleton from the classpath (no data directory is touched by this).
     *
     * @throws Exception if the Configuration singleton cannot be initialized
     */
    public FileUtilTest() throws Exception {
        Locale.setDefault(Locale.of("en", "GB"));
        Configuration.getInstance();
    }
    //</editor-fold>
    //
    //<editor-fold desc="Test methods" defaultstate="collapsed">
    /**
     * Tests that getFileExtension returns the dotted extension of a file path, an empty string when there is no
     * extension, throws an IllegalArgumentException for an empty path and a NullPointerException for a null path.
     */
    @Test
    public void testGetFileExtension() throws Exception {
        Assertions.assertEquals(".txt", FileUtil.getFileExtension("a/b.txt"));
        Assertions.assertEquals(".sdf", FileUtil.getFileExtension("just_a_name.sdf"));
        Assertions.assertEquals("", FileUtil.getFileExtension("noext"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> FileUtil.getFileExtension(""));
        Assertions.assertThrows(NullPointerException.class, () -> FileUtil.getFileExtension(null));
    }
    //
    /**
     * Tests that getFileNameWithoutExtension strips the extension from a file name and throws a NullPointerException
     * for a null file.
     */
    @Test
    public void testGetFileNameWithoutExtension() throws Exception {
        Assertions.assertEquals("molecule", FileUtil.getFileNameWithoutExtension(new File("some/dir/molecule.mol")));
        Assertions.assertEquals("noext", FileUtil.getFileNameWithoutExtension(new File("noext")));
        Assertions.assertThrows(NullPointerException.class, () -> FileUtil.getFileNameWithoutExtension(null));
    }
    //
    /**
     * Tests that getTimeStampFileNameExtension returns a non-null timestamp string matching the documented
     * "uuuu_MM_dd_HH_mm" shape.
     */
    @Test
    public void testGetTimeStampFileNameExtension() throws Exception {
        String tmpTimeStamp = FileUtil.getTimeStampFileNameExtension();
        Assertions.assertNotNull(tmpTimeStamp);
        Assertions.assertTrue(tmpTimeStamp.matches("\\d{4}_\\d{2}_\\d{2}_\\d{2}_\\d{2}"),
                "Timestamp did not match the documented pattern: " + tmpTimeStamp);
    }
    //
    /**
     * Tests createDirectory inside a temporary directory: a new subdirectory is created (true), calling it again on the
     * existing directory returns true, and null/empty paths return false.
     *
     * @param aTempDir JUnit-managed temporary directory
     */
    @Test
    public void testCreateDirectory(@TempDir Path aTempDir) throws Exception {
        String tmpNewDir = aTempDir.resolve("newSubDir").toString();
        Assertions.assertTrue(FileUtil.createDirectory(tmpNewDir));
        Assertions.assertTrue(new File(tmpNewDir).isDirectory());
        //already exists -> true
        Assertions.assertTrue(FileUtil.createDirectory(tmpNewDir));
        Assertions.assertFalse(FileUtil.createDirectory(null));
        Assertions.assertFalse(FileUtil.createDirectory(""));
    }
    //
    /**
     * Tests that createDirectory returns false when the directory cannot actually be created: a regular file placed at
     * an ancestor path makes {@code mkdirs()} fail, and createDirectory must propagate that false result rather than a
     * hard-coded true. This pins the {@code return tmpDirectory.mkdirs()} return value.
     *
     * @param aTempDir JUnit-managed temporary directory
     */
    @Test
    public void testCreateDirectoryReturnsFalseWhenCreationFails(@TempDir Path aTempDir) throws Exception {
        File tmpBlockingFile = aTempDir.resolve("blocker").toFile();
        Assertions.assertTrue(tmpBlockingFile.createNewFile());
        //a directory cannot be created underneath a regular file, so mkdirs() returns false
        String tmpUncreatableDir = new File(tmpBlockingFile, "sub").getAbsolutePath();
        Assertions.assertFalse(FileUtil.createDirectory(tmpUncreatableDir));
    }
    //
    /**
     * Tests deleteSingleFile inside a temporary directory: an existing file is deleted (true), a non-existent path
     * returns true (nothing to delete), and null/empty paths return false.
     *
     * @param aTempDir JUnit-managed temporary directory
     */
    @Test
    public void testDeleteSingleFile(@TempDir Path aTempDir) throws Exception {
        File tmpFile = aTempDir.resolve("toDelete.txt").toFile();
        Assertions.assertTrue(tmpFile.createNewFile());
        Assertions.assertTrue(FileUtil.deleteSingleFile(tmpFile.getAbsolutePath()));
        Assertions.assertFalse(tmpFile.exists());
        //non-existent file -> true (nothing to delete)
        Assertions.assertTrue(FileUtil.deleteSingleFile(aTempDir.resolve("doesNotExist.txt").toString()));
        Assertions.assertFalse(FileUtil.deleteSingleFile(null));
        Assertions.assertFalse(FileUtil.deleteSingleFile(""));
    }
    //
    /**
     * Tests deleteAllFilesInDirectory inside a temporary directory: several created files are all deleted (true) and
     * the directory is empty afterwards; a non-directory path returns false; null/empty paths return false.
     *
     * @param aTempDir JUnit-managed temporary directory
     */
    @Test
    public void testDeleteAllFilesInDirectory(@TempDir Path aTempDir) throws Exception {
        File tmpSubDir = aTempDir.resolve("filesDir").toFile();
        Assertions.assertTrue(tmpSubDir.mkdirs());
        for (int i = 0; i < 3; i++) {
            File tmpFile = new File(tmpSubDir, "file" + i + ".txt");
            Assertions.assertTrue(tmpFile.createNewFile());
        }
        Assertions.assertTrue(FileUtil.deleteAllFilesInDirectory(tmpSubDir.getAbsolutePath()));
        Assertions.assertEquals(0, tmpSubDir.listFiles().length);
        //a regular file is not a directory -> false
        File tmpRegularFile = aTempDir.resolve("aFile.txt").toFile();
        Assertions.assertTrue(tmpRegularFile.createNewFile());
        Assertions.assertFalse(FileUtil.deleteAllFilesInDirectory(tmpRegularFile.getAbsolutePath()));
        Assertions.assertFalse(FileUtil.deleteAllFilesInDirectory(null));
        Assertions.assertFalse(FileUtil.deleteAllFilesInDirectory(""));
    }
    //
    /**
     * Tests createEmptyFile inside a temporary directory: a new file path creates the file (true), calling it again on
     * the now-existing file returns false, and null/empty paths return false.
     *
     * @param aTempDir JUnit-managed temporary directory
     */
    @Test
    public void testCreateEmptyFile(@TempDir Path aTempDir) throws Exception {
        String tmpFilePath = aTempDir.resolve("empty.txt").toString();
        Assertions.assertTrue(FileUtil.createEmptyFile(tmpFilePath));
        Assertions.assertTrue(new File(tmpFilePath).isFile());
        //already exists -> false
        Assertions.assertFalse(FileUtil.createEmptyFile(tmpFilePath));
        Assertions.assertFalse(FileUtil.createEmptyFile(null));
        Assertions.assertFalse(FileUtil.createEmptyFile(""));
    }
    //
    /**
     * Tests that createEmptyFile returns false when the file is not actually created: an existing directory is not a
     * regular file (so the {@code isFile()} guard passes) but {@code createNewFile()} cannot create a new file under
     * that already-taken name and returns false, which createEmptyFile must propagate rather than a hard-coded true.
     * This pins the {@code return tmpFile.createNewFile()} return value.
     *
     * @param aTempDir JUnit-managed temporary directory
     */
    @Test
    public void testCreateEmptyFileReturnsFalseWhenFileNotCreated(@TempDir Path aTempDir) throws Exception {
        File tmpExistingDir = aTempDir.resolve("existingDir").toFile();
        Assertions.assertTrue(tmpExistingDir.mkdir());
        Assertions.assertFalse(FileUtil.createEmptyFile(tmpExistingDir.getAbsolutePath()));
    }
    //
    /**
     * Tests getNonExistingFilePath inside a temporary directory: a non-existing base path is returned unchanged; once
     * the base file exists the path becomes "base(1).ext"; once that exists too it becomes "base(2).ext"; null/empty
     * paths and a path ending in a directory separator throw an IllegalArgumentException.
     *
     * @param aTempDir JUnit-managed temporary directory
     */
    @Test
    public void testGetNonExistingFilePath(@TempDir Path aTempDir) throws Exception {
        String tmpBase = aTempDir.resolve("report").toString();
        String tmpExtension = ".txt";
        //non-existing base -> unchanged
        Assertions.assertEquals(tmpBase + tmpExtension, FileUtil.getNonExistingFilePath(tmpBase, tmpExtension));
        //create base, then call -> base(1).txt
        Assertions.assertTrue(new File(tmpBase + tmpExtension).createNewFile());
        Assertions.assertEquals(tmpBase + "(1)" + tmpExtension, FileUtil.getNonExistingFilePath(tmpBase, tmpExtension));
        //create base(1), then call -> base(2).txt
        Assertions.assertTrue(new File(tmpBase + "(1)" + tmpExtension).createNewFile());
        Assertions.assertEquals(tmpBase + "(2)" + tmpExtension, FileUtil.getNonExistingFilePath(tmpBase, tmpExtension));
        //null extension is treated as empty string
        String tmpNoExtBase = aTempDir.resolve("noext").toString();
        Assertions.assertEquals(tmpNoExtBase, FileUtil.getNonExistingFilePath(tmpNoExtBase, null));
        //guards
        Assertions.assertThrows(IllegalArgumentException.class, () -> FileUtil.getNonExistingFilePath(null, tmpExtension));
        Assertions.assertThrows(IllegalArgumentException.class, () -> FileUtil.getNonExistingFilePath("", tmpExtension));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> FileUtil.getNonExistingFilePath(aTempDir.toString() + File.separator, tmpExtension));
    }
    //
    /**
     * Tests getAppDirPath driven exactly once with the {@code user.home} system property redirected to a temporary
     * directory and the private static {@code appDirPath} cache reflectively reset, so the data directory resolves
     * under the temporary directory (covering the non-Windows branch and directory creation). The original state is
     * always restored in a finally block so the real user home is never polluted. The test is skipped on Windows, where
     * the data directory is resolved from the AppData environment variable instead.
     *
     * @param aTempHome temporary directory used as a fake user home
     */
    @Test
    public void testGetAppDirPath(@TempDir Path aTempHome) throws Exception {
        Assumptions.assumeFalse(System.getProperty("os.name").toUpperCase().contains("WIN"),
                "Windows resolves the application data directory from the AppData environment variable, "
                        + "which a test cannot set; the user.home branch under test does not exist there.");
        String tmpOldHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", aTempHome.toString());
            this.resetAppDirPathCache();
            String tmpAppDirPath = FileUtil.getAppDirPath();
            Assertions.assertNotNull(tmpAppDirPath);
            Assertions.assertTrue(tmpAppDirPath.startsWith(aTempHome.toString()),
                    "App dir path was not resolved under the temporary home: " + tmpAppDirPath);
            Assertions.assertTrue(new File(tmpAppDirPath).isDirectory());
            //second call returns the cached value
            Assertions.assertEquals(tmpAppDirPath, FileUtil.getAppDirPath());
            //getSettingsDirPath resolves through getAppDirPath and is rooted under the temporary home
            String tmpSettingsDirPath = FileUtil.getSettingsDirPath();
            Assertions.assertNotNull(tmpSettingsDirPath);
            Assertions.assertTrue(tmpSettingsDirPath.startsWith(aTempHome.toString()),
                    "Settings dir path was not resolved under the temporary home: " + tmpSettingsDirPath);
        } finally {
            System.setProperty("user.home", tmpOldHome);
            this.resetAppDirPathCache();
        }
    }
    //
    /**
     * Tests that openFilePathInExplorer throws an IllegalArgumentException for null, empty and blank paths. The
     * process-spawning branch is intentionally never driven (it would launch a real file explorer).
     */
    @Test
    public void testOpenFilePathInExplorerInputGuards() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> FileUtil.openFilePathInExplorer(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> FileUtil.openFilePathInExplorer(""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> FileUtil.openFilePathInExplorer("   "));
    }
    //
    /**
     * Tests that openFilePathInExplorer dispatches to the launcher of the detected operating system, driving all three
     * platform branches by temporarily overriding the {@code os.name} system property. {@link Runtime} is mocked, so no
     * file-browser process is ever spawned: unmocked this method shells out to {@code explorer}, {@code open} or
     * {@code gio} and opens a real window on any machine where that launcher exists.
     *
     * @param aTempDir JUnit-managed temporary directory used as the path to open
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testOpenFilePathInExplorerDispatchesPerOperatingSystem(@TempDir Path aTempDir) throws Exception {
        //the mock is created before os.name is overridden on purpose: Mockito initializes its ByteBuddy mock maker
        //lazily on first use and inspects the platform while doing so, and a faked os.name makes that initialization
        //fail permanently for the rest of the JVM
        Runtime tmpRuntime = Mockito.mock(Runtime.class);
        String tmpOldOsName = System.getProperty("os.name");
        try {
            for (String[] tmpCase : new String[][] {
                    {"Windows 11", "explorer"}, {"Mac OS X", "open"}, {"Linux", "gio"}}) {
                System.setProperty("os.name", tmpCase[0]);
                Mockito.clearInvocations(tmpRuntime);
                try (MockedStatic<Runtime> tmpRuntimeMock = Mockito.mockStatic(Runtime.class)) {
                    tmpRuntimeMock.when(Runtime::getRuntime).thenReturn(tmpRuntime);
                    FileUtil.openFilePathInExplorer(aTempDir.toString());
                    ArgumentCaptor<String[]> tmpCommandCaptor = ArgumentCaptor.forClass(String[].class);
                    Mockito.verify(tmpRuntime).exec(tmpCommandCaptor.capture());
                    Assertions.assertEquals(tmpCase[1], tmpCommandCaptor.getValue()[0],
                            "Wrong launcher for os.name " + tmpCase[0]);
                }
            }
        } finally {
            System.setProperty("os.name", tmpOldOsName);
        }
    }
    //
    /**
     * Tests that openFilePathInExplorer turns a failing launcher into a SecurityException: the mocked
     * {@link Runtime#exec(String[])} throws an IOException, which the method catches, logs and rethrows wrapped.
     *
     * @param aTempDir JUnit-managed temporary directory used as the path to open
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testOpenFilePathInExplorerWrapsLauncherFailure(@TempDir Path aTempDir) throws Exception {
        Runtime tmpRuntime = Mockito.mock(Runtime.class);
        Mockito.when(tmpRuntime.exec(Mockito.any(String[].class))).thenThrow(new IOException("no launcher installed"));
        try (MockedStatic<Runtime> tmpRuntimeMock = Mockito.mockStatic(Runtime.class)) {
            tmpRuntimeMock.when(Runtime::getRuntime).thenReturn(tmpRuntime);
            Assertions.assertThrows(SecurityException.class,
                    () -> FileUtil.openFilePathInExplorer(aTempDir.toString()));
        }
    }
    //
    /**
     * Tests the exception-handling (catch) branch of deleteSingleFile: deleting a file that lives inside a read-only
     * directory makes the underlying delete throw, which the method catches, logs and turns into a false return value.
     * The test is skipped on Windows (POSIX read-only-directory semantics do not apply) and the directory permissions
     * are always restored in a finally block so the JUnit temporary directory can be cleaned up.
     *
     * @param aTempDir JUnit-managed temporary directory
     */
    @Test
    public void testDeleteSingleFileFailureBranch(@TempDir Path aTempDir) throws Exception {
        Assumptions.assumeFalse(System.getProperty("os.name").toUpperCase().contains("WIN"),
                "Windows/NTFS ignores the POSIX write bit for directories, so the delete cannot be made to "
                        + "fail there; the catch branch under test cannot be driven on that platform.");
        File tmpReadOnlyDir = aTempDir.resolve("readOnlyDir").toFile();
        Assertions.assertTrue(tmpReadOnlyDir.mkdirs());
        File tmpFile = new File(tmpReadOnlyDir, "locked.txt");
        Assertions.assertTrue(tmpFile.createNewFile());
        try {
            Assertions.assertTrue(tmpReadOnlyDir.setWritable(false));
            Assertions.assertFalse(FileUtil.deleteSingleFile(tmpFile.getAbsolutePath()));
        } finally {
            tmpReadOnlyDir.setWritable(true);
        }
    }
    //
    /**
     * Tests the exception-handling (catch) branch of deleteAllFilesInDirectory: when a file inside the directory cannot
     * be deleted because the directory is read-only, the method catches the resulting exception, logs it and returns
     * false. Skipped on Windows; permissions are always restored in a finally block.
     *
     * @param aTempDir JUnit-managed temporary directory
     */
    @Test
    public void testDeleteAllFilesInDirectoryFailureBranch(@TempDir Path aTempDir) throws Exception {
        Assumptions.assumeFalse(System.getProperty("os.name").toUpperCase().contains("WIN"),
                "Windows/NTFS ignores the POSIX write bit for directories, so the delete cannot be made to "
                        + "fail there; the catch branch under test cannot be driven on that platform.");
        File tmpReadOnlyDir = aTempDir.resolve("readOnlyDir2").toFile();
        Assertions.assertTrue(tmpReadOnlyDir.mkdirs());
        Assertions.assertTrue(new File(tmpReadOnlyDir, "a.txt").createNewFile());
        try {
            Assertions.assertTrue(tmpReadOnlyDir.setWritable(false));
            Assertions.assertFalse(FileUtil.deleteAllFilesInDirectory(tmpReadOnlyDir.getAbsolutePath()));
        } finally {
            tmpReadOnlyDir.setWritable(true);
        }
    }
    //
    /**
     * Tests the exception-handling (catch) branch of createEmptyFile: creating a file inside a read-only directory makes
     * the underlying file creation throw, which the method catches, logs and turns into a false return value. Skipped on
     * Windows; permissions are always restored in a finally block.
     *
     * @param aTempDir JUnit-managed temporary directory
     */
    @Test
    public void testCreateEmptyFileFailureBranch(@TempDir Path aTempDir) throws Exception {
        Assumptions.assumeFalse(System.getProperty("os.name").toUpperCase().contains("WIN"),
                "Windows/NTFS ignores the POSIX write bit for directories, so the file creation cannot be "
                        + "made to fail there; the catch branch under test cannot be driven on that platform.");
        File tmpReadOnlyDir = aTempDir.resolve("readOnlyDir3").toFile();
        Assertions.assertTrue(tmpReadOnlyDir.mkdirs());
        try {
            Assertions.assertTrue(tmpReadOnlyDir.setWritable(false));
            Assertions.assertFalse(FileUtil.createEmptyFile(new File(tmpReadOnlyDir, "new.txt").getAbsolutePath()));
        } finally {
            tmpReadOnlyDir.setWritable(true);
        }
    }
    //
    /**
     * Tests that the private parameter-less constructor of the FileUtil utility class can be invoked reflectively.
     */
    @Test
    public void privateConstructorTest() throws Exception {
        TestUtil.assertPrivateConstructorIsInvocable(FileUtil.class);
    }
    //</editor-fold>
    //
    //<editor-fold desc="Private methods" defaultstate="collapsed">
    /**
     * Reflectively resets the private static {@code appDirPath} cache of FileUtil to null, so the next call to
     * getAppDirPath re-resolves the data directory from the current {@code user.home} system property.
     *
     * @throws Exception if the field cannot be accessed
     */
    private void resetAppDirPathCache() throws Exception {
        Field tmpField = FileUtil.class.getDeclaredField("appDirPath");
        tmpField.setAccessible(true);
        tmpField.set(null, null);
    }
    //</editor-fold>
}
