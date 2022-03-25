/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2022  Felix Baensch, Jonas Schaub (felix.baensch@w-hs.de, jonas.schaub@uni-jena.de)
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
import org.junit.Assert;
import org.junit.Test;

import java.awt.Color;
import java.io.File;
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

        Assert.assertTrue(tmpContainer.add(tmpPreference1));
        Assert.assertFalse(tmpContainer.add(tmpPreference1));
        Assert.assertTrue(tmpContainer.contains(tmpPreference1));
        Assert.assertTrue(tmpContainer.add(tmpPreference2));
        Assert.assertTrue(tmpContainer.add(tmpPreference3));
        Assert.assertTrue(tmpContainer.add(tmpPreference4));
        Assert.assertTrue(tmpContainer.add(tmpPreference5));
        Assert.assertTrue(tmpContainer.getSize() == 5);
        IPreference[] tmpSortedNameAscending = tmpContainer.getPreferencesSortedNameAscending();
        Assert.assertEquals(tmpPreference4, tmpSortedNameAscending[0]);
        Assert.assertEquals(tmpPreference1, tmpSortedNameAscending[1]);
        Assert.assertEquals(tmpPreference2, tmpSortedNameAscending[2]);
        Assert.assertEquals(tmpPreference3, tmpSortedNameAscending[3]);
        Assert.assertEquals(tmpPreference5, tmpSortedNameAscending[4]);

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
        Assert.assertArrayEquals(tmpContainer.getPreferences(), tmpReloadedContainer.getPreferences());
        Assert.assertEquals(tmpContainer.getGUID(), tmpReloadedContainer.getGUID());
        Assert.assertEquals(tmpContainer.getTimeStamp(), tmpReloadedContainer.getTimeStamp());
        Assert.assertEquals(tmpContainer.toString(), tmpReloadedContainer.toString());
        Assert.assertEquals(tmpContainer, tmpReloadedContainer);
        System.out.println();
    }

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