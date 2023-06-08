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

import de.unijena.cheminf.mortar.model.fragmentation.algorithm.ErtlFunctionalGroupsFinderFragmenter;
import de.unijena.cheminf.mortar.model.fragmentation.algorithm.SugarRemovalUtilityFragmenter;
import de.unijena.cheminf.mortar.model.settings.SettingsContainer;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.*;
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