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

package de.unijena.cheminf.mortar.preference;

import de.unijena.cheminf.mortar.model.fragmentation.algorithm.ErtlFunctionalGroupsFinderFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.SugarRemovalUtilityFragmenter;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import de.unijena.cheminf.mortar.model.util.FileUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.File;
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
     * Constructor (empty).
     */
    public PreferenceContainerTest() {

    }
    //
    /**
     * Tests basic functionalities of PreferenceContainer class/objects, like preference management, management of
     * public properties and persistence.
     *
     * @throws Exception
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

        System.out.println(tmpContainer.getGUID());
        System.out.println(tmpContainer.getTimeStamp());
        System.out.println(tmpContainer.getVersion());
        System.out.println(tmpContainer.toString());
        IPreference[] tmpPreferences = tmpContainer.getPreferences();
        for (IPreference tmpPreference : tmpPreferences) {
            System.out.println(tmpPreference.getName() + " : " + tmpPreference.getContentRepresentative());
        }

        tmpContainer.writeRepresentation();

        File tmpPreferenceContainerFile = new File(tmpContainer.getContainerFilePathname());
        PreferenceContainer tmpReloadedContainer = new PreferenceContainer(tmpPreferenceContainerFile);
        Assertions.assertArrayEquals(tmpContainer.getPreferences(), tmpReloadedContainer.getPreferences());
        Assertions.assertEquals(tmpContainer.getGUID(), tmpReloadedContainer.getGUID());
        Assertions.assertEquals(tmpContainer.getTimeStamp(), tmpReloadedContainer.getTimeStamp());
        Assertions.assertEquals(tmpContainer.toString(), tmpReloadedContainer.toString());
        Assertions.assertEquals(tmpContainer, tmpReloadedContainer);
        System.out.println();
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
        Locale.setDefault(new Locale("en", "GB"));
        SugarRemovalUtilityFragmenter tmpSRUFragmenter = new SugarRemovalUtilityFragmenter();
        String tmpDir = FileUtil.getAppDirPath()
                + File.separatorChar
                + "Test"
                + File.separatorChar;
        (new File(tmpDir)).mkdirs();
        String tmpFilePathname = tmpDir + "SRUFragmenterSettings.txt";
        PreferenceContainer tmpContainer = PreferenceUtil.translateJavaFxPropertiesToPreferences(tmpSRUFragmenter.settingsProperties(), tmpFilePathname);
        tmpContainer.writeRepresentation();
        tmpContainer = PreferenceUtil.translateJavaFxPropertiesToPreferences(new ErtlFunctionalGroupsFinderFragmenter().settingsProperties(), tmpDir + "EFGFFragmenterSettings.txt");
        tmpContainer.writeRepresentation();
        tmpContainer = PreferenceUtil.translateJavaFxPropertiesToPreferences(new SettingsContainer().settingsProperties(), tmpDir + "SettingContainer.txt");
        tmpContainer.writeRepresentation();
    }
}
