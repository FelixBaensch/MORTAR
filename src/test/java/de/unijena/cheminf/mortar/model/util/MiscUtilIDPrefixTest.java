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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the ID prefix creation of the {@link MiscUtil} class.
 *
 * @author Manuel Schauer
 */
public class MiscUtilIDPrefixTest {
    /**
     * Tests that illegal characters are replaced, underscores are merged and stripped, and long names are truncated.
     */
    @Test
    public void testCreateIDPrefix() {
        Assertions.assertEquals("Ertl_algorithm", MiscUtil.createIDPrefix("Ertl algorithm", 40));
        Assertions.assertEquals("Ertl_algorithm_1", MiscUtil.createIDPrefix("Ertl algorithm_1", 40));
        Assertions.assertEquals("COCONUT_DB_2024", MiscUtil.createIDPrefix(" COCONUT; DB,,2024 ", 40));
        Assertions.assertEquals("abc", MiscUtil.createIDPrefix("abc_def", 4));
        Assertions.assertEquals("a_b", MiscUtil.createIDPrefix("__a b__", 40));
        Assertions.assertEquals("a", MiscUtil.createIDPrefix("_".repeat(10000) + "a" + "_".repeat(10000), 40));
        Assertions.assertEquals(MiscUtil.ID_PREFIX_DEFAULT, MiscUtil.createIDPrefix("ÄÖÜ;;", 40));
        Assertions.assertEquals(MiscUtil.ID_PREFIX_DEFAULT, MiscUtil.createIDPrefix(null, 40));
        Assertions.assertThrows(IllegalArgumentException.class, () -> MiscUtil.createIDPrefix("abc", 0));
    }
    //
    /**
     * Tests that the untruncated prefix creation only cleans the name and that truncation leaves short prefixes
     * unchanged.
     */
    @Test
    public void testCreateAndTruncateIDPrefix() {
        String tmpLongName = "a".repeat(MiscUtil.ID_PREFIX_MAX_LENGTH + 10);
        Assertions.assertEquals(tmpLongName, MiscUtil.createIDPrefix(tmpLongName));
        Assertions.assertEquals(MiscUtil.ID_PREFIX_MAX_LENGTH, MiscUtil.truncateIDPrefix(tmpLongName, MiscUtil.ID_PREFIX_MAX_LENGTH).length());
        Assertions.assertEquals("short", MiscUtil.truncateIDPrefix("short", MiscUtil.ID_PREFIX_MAX_LENGTH));
        Assertions.assertEquals(MiscUtil.ID_PREFIX_DEFAULT, MiscUtil.truncateIDPrefix("___", 2));
        Assertions.assertThrows(IllegalArgumentException.class, () -> MiscUtil.truncateIDPrefix("abc", 0));
    }
}
