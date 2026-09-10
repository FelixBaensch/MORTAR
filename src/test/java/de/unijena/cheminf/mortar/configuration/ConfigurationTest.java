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

package de.unijena.cheminf.mortar.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;

/**
 * Test class for the Configuration singleton, verifying that the single instance is identity-stable and that
 * properties are loaded from the bundled classpath properties resource.
 *
 * @author Felix Baensch
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class ConfigurationTest {
    //<editor-fold desc="Constructor" defaultstate="collapsed">
    /**
     * Constructor that sets the default locale to en-GB (so message-bundle and configuration resources resolve
     * deterministically) and bootstraps the Configuration singleton.
     *
     * @throws Exception if the configuration properties resource cannot be loaded
     */
    public ConfigurationTest() throws Exception {
        Locale.setDefault(Locale.of("en", "GB"));
        Configuration.getInstance();
    }
    //</editor-fold>
    //
    //<editor-fold desc="Test methods" defaultstate="collapsed">
    /**
     * Asserts that Configuration.getInstance() returns a non-null instance and that two consecutive calls return the
     * very same object, proving the synchronized singleton identity.
     *
     * @throws Exception if the configuration properties resource cannot be loaded
     */
    @Test
    public void testGetInstanceReturnsStableSingleton() throws Exception {
        Configuration tmpFirst = Configuration.getInstance();
        Configuration tmpSecond = Configuration.getInstance();
        Assertions.assertNotNull(tmpFirst);
        Assertions.assertSame(tmpFirst, tmpSecond);
    }
    //
    /**
     * Asserts that getProperty resolves known keys from the bundled classpath properties resource to their expected
     * non-empty values.
     *
     * @throws Exception if the configuration properties resource cannot be loaded
     */
    @Test
    public void testGetPropertyReadsClasspathResource() throws Exception {
        Configuration tmpConfiguration = Configuration.getInstance();
        Assertions.assertEquals("MORTAR", tmpConfiguration.getProperty("mortar.vendor.name"));
        Assertions.assertEquals("Settings", tmpConfiguration.getProperty("mortar.settingsDirectory.name"));
    }
    //</editor-fold>
}
