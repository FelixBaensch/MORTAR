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

package de.unijena.cheminf.mortar.preference;

import de.unijena.cheminf.mortar.configuration.Configuration;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.ErtlFunctionalGroupsFinderFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.SugarRemovalUtilityFragmenter;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import de.unijena.cheminf.mortar.model.util.FileUtil;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Test class for class PreferenceContainer.
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class PreferenceContainerTest {
    /**
     * Constructor to initialize locale and configuration.
     */
    public PreferenceContainerTest() throws Exception {
        Locale.setDefault(Locale.of("en", "GB"));
        Configuration.getInstance();
    }
    //
    /**
     * Tests basic functionalities of PreferenceContainer class/objects, like preference management, management of
     * public properties and persistence.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testPreferenceContainerBasics() throws Exception {
        String tmpDir = FileUtil.getAppDirPath()
                + File.separatorChar
                + "Test"
                + File.separatorChar;
        (new File(tmpDir)).mkdirs();
        String tmpFilePathname = tmpDir + "PreferenceContainerTest.gzip";
        PreferenceContainer tmpContainer = new PreferenceContainer(tmpFilePathname);

        IPreference tmpPreference1 = new BooleanPreference("MORTAR is cool", true);
        IPreference tmpPreference2 = new RGBColorPreference("My favorite color",
                Color.PINK.getRed()/255.0,
                Color.PINK.getGreen()/255.0,
                Color.PINK.getBlue()/255.0,
                Color.PINK.getAlpha()/255.0);
        IPreference tmpPreference3 = new SingleIntegerPreference("Number one", 1);
        SingleNumberPreference tmpPreference4 = new SingleNumberPreference("Layout parameter xy", 2.0);
        SingleTermPreference tmpPreference5 = new SingleTermPreference("Welcoming message", "Welcome to MORTAR");

        Assertions.assertTrue(tmpContainer.add(tmpPreference1));
        Assertions.assertFalse(tmpContainer.add(tmpPreference1));
        Assertions.assertTrue(tmpContainer.contains(tmpPreference1));
        Assertions.assertTrue(tmpContainer.add(tmpPreference2));
        Assertions.assertTrue(tmpContainer.add(tmpPreference3));
        Assertions.assertTrue(tmpContainer.add(tmpPreference4));
        Assertions.assertTrue(tmpContainer.add(tmpPreference5));
        Assertions.assertEquals(5, tmpContainer.getSize());
        IPreference[] tmpSortedNameAscending = tmpContainer.getPreferencesSortedNameAscending();
        Assertions.assertEquals(tmpPreference4, tmpSortedNameAscending[0]);
        Assertions.assertEquals(tmpPreference1, tmpSortedNameAscending[1]);
        Assertions.assertEquals(tmpPreference2, tmpSortedNameAscending[2]);
        Assertions.assertEquals(tmpPreference3, tmpSortedNameAscending[3]);
        Assertions.assertEquals(tmpPreference5, tmpSortedNameAscending[4]);

        Assertions.assertDoesNotThrow(tmpContainer::getGUID);
        Assertions.assertDoesNotThrow(tmpContainer::getTimeStamp);
        Assertions.assertDoesNotThrow(tmpContainer::getVersion);
        Assertions.assertDoesNotThrow(tmpContainer::toString);
        IPreference[] tmpPreferences = tmpContainer.getPreferences();
        for (IPreference tmpPreference : tmpPreferences) {
            Assertions.assertNotNull(tmpPreference.getName());
            Assertions.assertNotNull(tmpPreference.getContentRepresentative());
        }

        tmpContainer.writeRepresentation();

        File tmpPreferenceContainerFile = new File(tmpContainer.getContainerFilePathname());
        PreferenceContainer tmpReloadedContainer = new PreferenceContainer(tmpPreferenceContainerFile);
        Assertions.assertArrayEquals(tmpContainer.getPreferences(), tmpReloadedContainer.getPreferences());
        Assertions.assertEquals(tmpContainer.getGUID(), tmpReloadedContainer.getGUID());
        Assertions.assertEquals(tmpContainer.getTimeStamp(), tmpReloadedContainer.getTimeStamp());
        Assertions.assertEquals(tmpContainer.toString(), tmpReloadedContainer.toString());
        Assertions.assertEquals(tmpContainer, tmpReloadedContainer);
    }
    //
    /**
     * Tests the conversion of MORTAR preferences to JavaFx properties via the
     * {@link PreferenceUtil#translateJavaFxPropertiesToPreferences(List, String)} method.
     * Correct persistence of the preferences is also tested.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testPropertyToPreferenceConversion() throws Exception {
        SugarRemovalUtilityFragmenter tmpSRUFragmenter = new SugarRemovalUtilityFragmenter();
        String tmpDir = FileUtil.getAppDirPath()
                + File.separatorChar
                + "Test"
                + File.separatorChar;
        (new File(tmpDir)).mkdirs();
        String tmpFilePathname = tmpDir + "SRUFragmenterSettings.txt";
        final PreferenceContainer tmpContainer = PreferenceUtil.translateJavaFxPropertiesToPreferences(tmpSRUFragmenter.settingsProperties(), tmpFilePathname);
        Assertions.assertDoesNotThrow(tmpContainer::writeRepresentation);
        final PreferenceContainer tmpNewContainer = PreferenceUtil.translateJavaFxPropertiesToPreferences(new ErtlFunctionalGroupsFinderFragmenter().settingsProperties(), tmpDir + "EFGFFragmenterSettings.txt");
        Assertions.assertDoesNotThrow(tmpNewContainer::writeRepresentation);
        final PreferenceContainer tmpNewNewContainer = PreferenceUtil.translateJavaFxPropertiesToPreferences(new SettingsContainer().settingsProperties(), tmpDir + "SettingContainer.txt");
        Assertions.assertDoesNotThrow(tmpNewNewContainer::writeRepresentation);
    }
    //
    /**
     * Tests a @TempDir-isolated .gzip write-&gt;reload round-trip of a container holding one of each typed preference,
     * asserting structural identity (preferences array, GUID, time stamp, equals) across the round-trip rather than
     * byte-for-byte file content (the gzip stream is non-deterministic at the byte level).
     *
     * @param aTempDir JUnit-managed temporary directory
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testContainerGzipRoundTrip(@TempDir Path aTempDir) throws Exception {
        String tmpFilePathname = aTempDir.resolve("PreferenceContainerTest.gzip").toString();
        PreferenceContainer tmpContainer = new PreferenceContainer(tmpFilePathname);
        tmpContainer.add(new BooleanPreference("MORTAR is cool", true));
        tmpContainer.add(new SingleIntegerPreference("Number one", 1));
        tmpContainer.add(new SingleNumberPreference("Layout parameter xy", 2.0));
        tmpContainer.add(new SingleTermPreference("Welcoming message", "Welcome to MORTAR"));
        tmpContainer.add(new RGBColorPreference("My favorite color", 1.0, 0.0, 0.5, 1.0));
        tmpContainer.writeRepresentation();
        PreferenceContainer tmpReloadedContainer = new PreferenceContainer(new File(tmpFilePathname));
        Assertions.assertArrayEquals(tmpContainer.getPreferences(), tmpReloadedContainer.getPreferences());
        Assertions.assertEquals(tmpContainer.getGUID(), tmpReloadedContainer.getGUID());
        Assertions.assertEquals(tmpContainer.getTimeStamp(), tmpReloadedContainer.getTimeStamp());
        Assertions.assertEquals(tmpContainer, tmpReloadedContainer);
    }
    //
    /**
     * Tests a @TempDir-isolated .txt write-&gt;reload round-trip (the second extension branch of the
     * compressed-writer / decompressed-reader dispatch), asserting structural identity across the round-trip.
     *
     * @param aTempDir JUnit-managed temporary directory
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testContainerTxtRoundTrip(@TempDir Path aTempDir) throws Exception {
        String tmpFilePathname = aTempDir.resolve("PreferenceContainerTest.txt").toString();
        PreferenceContainer tmpContainer = new PreferenceContainer(tmpFilePathname);
        tmpContainer.add(new BooleanPreference("MORTAR is cool", true));
        tmpContainer.add(new SingleIntegerPreference("Number one", 1));
        tmpContainer.add(new SingleNumberPreference("Layout parameter xy", 2.0));
        tmpContainer.add(new SingleTermPreference("Welcoming message", "Welcome to MORTAR"));
        tmpContainer.add(new RGBColorPreference("My favorite color", 1.0, 0.0, 0.5, 1.0));
        tmpContainer.writeRepresentation();
        PreferenceContainer tmpReloadedContainer = new PreferenceContainer(new File(tmpFilePathname));
        Assertions.assertArrayEquals(tmpContainer.getPreferences(), tmpReloadedContainer.getPreferences());
        Assertions.assertEquals(tmpContainer.getGUID(), tmpReloadedContainer.getGUID());
        Assertions.assertEquals(tmpContainer.getTimeStamp(), tmpReloadedContainer.getTimeStamp());
        Assertions.assertEquals(tmpContainer, tmpReloadedContainer);
    }
    //
    /**
     * Tests that a @TempDir-isolated write-&gt;reload round-trip of an empty container (no preferences added) yields an
     * empty reloaded container. This exercises the early Container_End write branch and the immediate Container_End
     * read branch.
     *
     * @param aTempDir JUnit-managed temporary directory
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testEmptyContainerRoundTrip(@TempDir Path aTempDir) throws Exception {
        String tmpFilePathname = aTempDir.resolve("EmptyPreferenceContainerTest.gzip").toString();
        PreferenceContainer tmpContainer = new PreferenceContainer(tmpFilePathname);
        Assertions.assertTrue(tmpContainer.isEmpty());
        tmpContainer.writeRepresentation();
        PreferenceContainer tmpReloadedContainer = new PreferenceContainer(new File(tmpFilePathname));
        Assertions.assertTrue(tmpReloadedContainer.isEmpty());
        Assertions.assertEquals(0, tmpReloadedContainer.getSize());
        Assertions.assertEquals(tmpContainer.getGUID(), tmpReloadedContainer.getGUID());
    }
    //
    /**
     * Tests the static isValidContainerFilePathname method: false for a null pathname, an empty pathname, a bad
     * extension (.csv) and a directory; true for a valid .gzip pathname under the temporary directory.
     *
     * @param aTempDir JUnit-managed temporary directory
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testIsValidContainerFilePathname(@TempDir Path aTempDir) throws Exception {
        Assertions.assertFalse(PreferenceContainer.isValidContainerFilePathname(null));
        Assertions.assertFalse(PreferenceContainer.isValidContainerFilePathname(""));
        Assertions.assertFalse(PreferenceContainer.isValidContainerFilePathname(aTempDir.resolve("x.csv").toString()));
        Assertions.assertFalse(PreferenceContainer.isValidContainerFilePathname(aTempDir.toString()));
        Assertions.assertTrue(PreferenceContainer.isValidContainerFilePathname(aTempDir.resolve("ok.gzip").toString()));
    }
    //
    /**
     * Tests that constructing a PreferenceContainer from a corrupt .gzip file (garbage bytes that are no valid GZIP
     * header) throws an IOException. The read-error catch branch is reached with a real malformed temporary file and
     * no mock.
     *
     * @param aTempDir JUnit-managed temporary directory
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testReloadFromCorruptGzipThrows(@TempDir Path aTempDir) throws Exception {
        File tmpBadFile = aTempDir.resolve("corrupt.gzip").toFile();
        Files.write(tmpBadFile.toPath(), new byte[]{0, 1, 2, 3, 4, 5});
        Assertions.assertThrows(java.io.IOException.class, () -> new PreferenceContainer(tmpBadFile));
    }
    //
    /**
     * Tests the in-memory container management API with real preference objects: isEmpty, getSize, contains,
     * containsPreferenceName, containsPreferenceType, getPreferences(String) hit and miss, getPreferences(PreferenceType),
     * both name-sorted accessors including their cache-return and cache-rebuild branches, both replace overloads (success
     * and fail), both delete overloads (success and miss), clearAll on a non-empty and an already-empty container, copy on
     * a non-empty container, getVersion, compareTo and the equals/hashCode contract.
     *
     * @param aTempDir JUnit-managed temporary directory
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testContainerManagementApi(@TempDir Path aTempDir) throws Exception {
        String tmpFilePathname = aTempDir.resolve("ManagementApiTest.txt").toString();
        PreferenceContainer tmpContainer = new PreferenceContainer(tmpFilePathname);
        Assertions.assertTrue(tmpContainer.isEmpty());
        Assertions.assertEquals(0, tmpContainer.getSize());
        BooleanPreference tmpAPreference = new BooleanPreference("Apple setting", true);
        SingleTermPreference tmpBPreference = new SingleTermPreference("Banana setting", "ripe");
        Assertions.assertTrue(tmpContainer.add(tmpAPreference));
        Assertions.assertTrue(tmpContainer.add(tmpBPreference));
        Assertions.assertFalse(tmpContainer.isEmpty());
        Assertions.assertEquals(2, tmpContainer.getSize());
        Assertions.assertTrue(tmpContainer.contains(tmpAPreference));
        Assertions.assertTrue(tmpContainer.containsPreferenceName("Apple setting"));
        Assertions.assertFalse(tmpContainer.containsPreferenceName("Nonexistent name"));
        Assertions.assertTrue(tmpContainer.containsPreferenceType(PreferenceType.BOOLEAN));
        Assertions.assertTrue(tmpContainer.containsPreferenceType(PreferenceType.SINGLE_TERM));
        Assertions.assertEquals(1, tmpContainer.getPreferences("Apple setting").length);
        Assertions.assertEquals(0, tmpContainer.getPreferences("Nonexistent name").length);
        Assertions.assertEquals(1, tmpContainer.getPreferences(PreferenceType.SINGLE_TERM).length);
        Assertions.assertEquals(0, tmpContainer.getPreferences(PreferenceType.RGB_COLOR).length);
        //Each sorted accessor is validated inline immediately after the call, exercising the build-from-wrapper-set,
        //the cache-return and the reverse-other-cache branches without holding a stale array reference across a
        //subsequent (in-place reversing) accessor call.
        //Ascending built from the wrapper set (both caches null)
        IPreference[] tmpAscendingBuilt = tmpContainer.getPreferencesSortedNameAscending();
        Assertions.assertEquals(tmpAPreference, tmpAscendingBuilt[0]);
        Assertions.assertEquals(tmpBPreference, tmpAscendingBuilt[1]);
        //Ascending again returns the cached array (cache-return branch)
        IPreference[] tmpAscendingCached = tmpContainer.getPreferencesSortedNameAscending();
        Assertions.assertEquals(tmpAPreference, tmpAscendingCached[0]);
        Assertions.assertEquals(tmpBPreference, tmpAscendingCached[1]);
        //Descending derived by reversing the ascending cache (reverse-other-cache branch)
        IPreference[] tmpDescendingDerived = tmpContainer.getPreferencesSortedNameDescending();
        Assertions.assertEquals(tmpBPreference, tmpDescendingDerived[0]);
        Assertions.assertEquals(tmpAPreference, tmpDescendingDerived[1]);
        //Clear the cache, then build descending first from the wrapper set (descending build-from-wrapper-set branch)
        tmpContainer.clearCache();
        IPreference[] tmpDescendingBuilt = tmpContainer.getPreferencesSortedNameDescending();
        Assertions.assertEquals(tmpBPreference, tmpDescendingBuilt[0]);
        Assertions.assertEquals(tmpAPreference, tmpDescendingBuilt[1]);
        //Ascending now derived by reversing the descending cache (ascending reverse-other-cache branch)
        IPreference[] tmpAscendingDerived = tmpContainer.getPreferencesSortedNameAscending();
        Assertions.assertEquals(tmpAPreference, tmpAscendingDerived[0]);
        Assertions.assertEquals(tmpBPreference, tmpAscendingDerived[1]);
        Assertions.assertNotNull(tmpContainer.getVersion());
        Assertions.assertEquals(0, tmpContainer.compareTo(tmpContainer));
        //replace(IPreference, IPreference): success and fail (old not contained)
        BooleanPreference tmpCPreference = new BooleanPreference("Cherry setting", false);
        Assertions.assertTrue(tmpContainer.replace(tmpAPreference, tmpCPreference));
        Assertions.assertFalse(tmpContainer.contains(tmpAPreference));
        Assertions.assertTrue(tmpContainer.contains(tmpCPreference));
        Assertions.assertFalse(tmpContainer.replace(tmpAPreference, new BooleanPreference("Date setting", true)));
        //replace(String, IPreference): success path
        BooleanPreference tmpDPreference = new BooleanPreference("Date setting", true);
        Assertions.assertTrue(tmpContainer.replace(tmpCPreference.getGUID(), tmpDPreference));
        Assertions.assertFalse(tmpContainer.contains(tmpCPreference));
        Assertions.assertTrue(tmpContainer.contains(tmpDPreference));
        //copy() on a non-empty container yields an equal container (same GUID)
        PreferenceContainer tmpCopy = tmpContainer.copy();
        Assertions.assertEquals(tmpContainer.getGUID(), tmpCopy.getGUID());
        Assertions.assertEquals(tmpContainer, tmpCopy);
        Assertions.assertEquals(tmpContainer.hashCode(), tmpCopy.hashCode());
        //delete(String): success and miss
        Assertions.assertTrue(tmpContainer.delete(tmpDPreference.getGUID()));
        Assertions.assertFalse(tmpContainer.delete(tmpDPreference.getGUID()));
        //delete(IPreference): success and miss
        Assertions.assertTrue(tmpContainer.delete(tmpBPreference));
        Assertions.assertFalse(tmpContainer.delete(tmpBPreference));
        //clearAll() on a non-empty container empties it; on an empty container it is a safe no-op
        PreferenceContainer tmpFullContainer = new PreferenceContainer(tmpFilePathname);
        tmpFullContainer.add(new BooleanPreference("Echo setting", true));
        Assertions.assertFalse(tmpFullContainer.isEmpty());
        tmpFullContainer.clearAll();
        Assertions.assertTrue(tmpFullContainer.isEmpty());
        Assertions.assertDoesNotThrow(tmpFullContainer::clearAll);
        Assertions.assertTrue(tmpFullContainer.isEmpty());
    }
    //
    /**
     * Tests PreferenceUtil.checkPropertiesForPreferenceRestrictions: a property with a valid name passes
     * (assertDoesNotThrow); a property with an invalid (lower-case-starting) name throws an UnsupportedOperationException.
     *
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testCheckPropertiesForPreferenceRestrictions() throws Exception {
        SimpleBooleanProperty tmpValidProperty = new SimpleBooleanProperty(null, "Valid name", true);
        Assertions.assertDoesNotThrow(() ->
                PreferenceUtil.checkPropertiesForPreferenceRestrictions(List.<Property<?>>of(tmpValidProperty)));
        SimpleBooleanProperty tmpBadProperty = new SimpleBooleanProperty(null, "invalid lower-start", true);
        Assertions.assertThrows(UnsupportedOperationException.class, () ->
                PreferenceUtil.checkPropertiesForPreferenceRestrictions(List.<Property<?>>of(tmpBadProperty)));
    }
    //
    /**
     * Tests the reverse path PreferenceUtil.updatePropertiesFromPreferences: a list of JavaFx properties is translated
     * into preferences, a fresh property list with the same names but differing values is updated from the resulting
     * container, and each property value is asserted to reflect the persisted preference value (exercising the switch arm
     * and the matching-name branch).
     *
     * @param aTempDir JUnit-managed temporary directory
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testUpdatePropertiesFromPreferences(@TempDir Path aTempDir) throws Exception {
        String tmpFilePathname = aTempDir.resolve("UpdatePropertiesTest.txt").toString();
        SimpleBooleanProperty tmpSourceProperty = new SimpleBooleanProperty(null, "Highlight atoms", true);
        PreferenceContainer tmpContainer =
                PreferenceUtil.translateJavaFxPropertiesToPreferences(List.<Property<?>>of(tmpSourceProperty), tmpFilePathname);
        SimpleBooleanProperty tmpTargetProperty = new SimpleBooleanProperty(null, "Highlight atoms", false);
        PreferenceUtil.updatePropertiesFromPreferences(List.<Property<?>>of(tmpTargetProperty), tmpContainer);
        Assertions.assertTrue(tmpTargetProperty.get());
        //A property whose name has no matching preference remains in its default value (no-match-name branch)
        SimpleBooleanProperty tmpUnmatchedProperty = new SimpleBooleanProperty(null, "Unmatched setting", false);
        PreferenceUtil.updatePropertiesFromPreferences(List.<Property<?>>of(tmpUnmatchedProperty), tmpContainer);
        Assertions.assertFalse(tmpUnmatchedProperty.get());
    }
    //
    /**
     * Tests the exact ordering and hashing contract of PreferenceContainer. Two freshly created containers carry
     * different, globally-unique GUIDs, so compareTo must return a non-zero value whose sign follows the GUID string
     * ordering (this pins compareTo against a "return 0" mutation). The hashCode value is pinned to its exact formula,
     * {@code 31 * 13 + guid.hashCode() = 403 + guid.hashCode()}, so the arithmetic mutations of the formula and a
     * "return 0" mutation are detected. Equal-GUID copies must share both the hashCode and equality.
     *
     * @param aTempDir JUnit-managed temporary directory
     * @throws Exception if anything goes wrong
     */
    @Test
    public void testCompareToAndHashCodeContract(@TempDir Path aTempDir) throws Exception {
        PreferenceContainer tmpContainerA = new PreferenceContainer(aTempDir.resolve("containerA.txt").toString());
        PreferenceContainer tmpContainerB = new PreferenceContainer(aTempDir.resolve("containerB.txt").toString());
        Assertions.assertNotEquals(tmpContainerA.getGUID(), tmpContainerB.getGUID());
        Assertions.assertNotEquals(0, tmpContainerA.compareTo(tmpContainerB));
        Assertions.assertEquals(Integer.signum(tmpContainerA.getGUID().compareTo(tmpContainerB.getGUID())),
                Integer.signum(tmpContainerA.compareTo(tmpContainerB)));
        Assertions.assertEquals(31 * 13 + tmpContainerA.getGUID().hashCode(), tmpContainerA.hashCode());
        PreferenceContainer tmpCopy = tmpContainerA.copy();
        Assertions.assertEquals(tmpContainerA.hashCode(), tmpCopy.hashCode());
        Assertions.assertEquals(tmpContainerA, tmpCopy);
    }
}
