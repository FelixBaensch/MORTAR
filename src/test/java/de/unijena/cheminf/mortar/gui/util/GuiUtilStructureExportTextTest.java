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

package de.unijena.cheminf.mortar.gui.util;

import de.unijena.cheminf.mortar.message.Message;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

/**
 * Tests the text of the confirmation alert before structure exports, see
 * {@link GuiUtil#getStructureExportConfirmationText(boolean, boolean, String)}, and that the message keys of the
 * structure export alerts exist.
 *
 * @author Manuel Schauer
 */
public class GuiUtilStructureExportTextTest {
    //<editor-fold desc="static initializer">
    /**
     * Sets the default locale to British English, which is needed for the MORTAR message bundle.
     */
    static {
        Locale.setDefault(Locale.of("en", "GB"));
    }
    //</editor-fold>
    //
    /**
     * Tests the text for molecule exports: the SMILES hint, optionally the items hint, the additional text, and the
     * question, in this order and separated by empty lines.
     */
    @Test
    public void testMoleculeExportText() {
        String tmpSmilesText = Message.get("Exporter.ConfirmationAlert.MoleculeExport.smilesText");
        String tmpItemsText = Message.get("Exporter.ConfirmationAlert.MoleculeExport.itemsText");
        String tmpQuestion = Message.get("Exporter.ConfirmationAlert.StructureExport.question");
        Assertions.assertEquals(tmpSmilesText + "\n\n" + tmpItemsText + "\n\n" + "120 of 150" + "\n\n" + tmpQuestion,
                GuiUtil.getStructureExportConfirmationText(true, true, "120 of 150"));
        Assertions.assertEquals(tmpSmilesText + "\n\n" + tmpQuestion,
                GuiUtil.getStructureExportConfirmationText(true, false, null));
        Assertions.assertEquals(tmpSmilesText + "\n\n" + tmpQuestion,
                GuiUtil.getStructureExportConfirmationText(true, false, "  "));
    }
    //
    /**
     * Tests the text for fragment exports: the fragment hint and the question; the items hint is not added.
     */
    @Test
    public void testFragmentExportText() {
        Assertions.assertEquals(Message.get("Exporter.ConfirmationAlert.FragmentExport.text") + "\n\n"
                        + Message.get("Exporter.ConfirmationAlert.StructureExport.question"),
                GuiUtil.getStructureExportConfirmationText(false, true, null));
    }
    //
    /**
     * Tests that the message keys of the structure export alerts exist in the message bundle.
     */
    @Test
    public void testMessageKeysExist() {
        List<String> tmpKeys = List.of(
                "Exporter.ConfirmationAlert.MoleculeExport.title",
                "Exporter.ConfirmationAlert.MoleculeExport.header",
                "Exporter.ConfirmationAlert.MoleculeExport.selectionText",
                "Exporter.ConfirmationAlert.FragmentExport.title",
                "Exporter.ConfirmationAlert.FragmentExport.header",
                "Exporter.ConfirmationAlert.StructureExport.coordinatesCheckBox.text",
                "Exporter.ConfirmationAlert.StructureExport.coordinatesCheckBox.tooltip",
                "Exporter.MessageAlert.NoMoleculesSelected.title",
                "Exporter.MessageAlert.NoMoleculesSelected.header",
                "Exporter.MessageAlert.NoMoleculesSelected.text",
                "Exporter.ReplaceFileConfirmation.title",
                "Exporter.ReplaceFileConfirmation.header",
                "Exporter.ReplaceFileConfirmation.text",
                "GuiUtil.openInExplorer.error.header");
        for (String tmpKey : tmpKeys) {
            Assertions.assertFalse(Message.get(tmpKey).endsWith("not found."), tmpKey);
        }
    }
}
