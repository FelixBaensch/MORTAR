/*
 * MORTAR - MOlecule fRagmenTAtion fRamework
 * Copyright (C) 2025  Felix Baensch, Jonas Schaub (felix.j.baensch@gmail.com, jonas.schaub@uni-jena.de)
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

package de.unijena.cheminf.mortar.model.io;

import de.unijena.cheminf.mortar.model.util.BasicDefinitions;
import de.unijena.cheminf.mortar.model.util.FileUtil;

import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * File reader for different kinds of files with a SMILES code column. The reader can detect the structure of the file based
 * on the first few lines and some assumptions like the SMILES and ID/name columns should be the first two but can
 * be in both positions. Unsuitable for reaction SMILES or CxSMILES.
 *
 * @author Jonas Schaub
 * @author Samuel Behr
 * @version 1.0.0.0
 */
public class DynamicSMILESFileReader {
    //<editor-fold desc="Public static final class constants">
    /**
     * Possible SMILES file separators used to separate SMILES code from ID. Ordered so that non-whitespace characters
     * are tested first.
     */
    public static final Set<String> POSSIBLE_SMILES_FILE_SEPARATORS = Set.of(",", ";", " ", "\t");
    //
    /**
     * Maximum number of lines starting from the first one to check for valid SMILES strings in a SMILES file when
     * trying to determine the SMILES code column and separator.
     */
    public static final int MAXIMUM_LINE_NUMBER_TO_CHECK_IN_SMILES_FILES = 10;
    //
    /**
     * Strings that can be parsed by CDK SmilesParser as SMILES codes but should be ignored when detecting the file structure,
     * e.g. "ID" is a likely column name but could be parsed into Iodine-Deuterium as a SMILES code.
     */
    public static final Set<String> PARSABLE_SMILES_EXCEPTIONS = Set.of("ID");
    //</editor-fold>
    //
    //<editor-fold desc="Private static class constants">
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = Logger.getLogger(DynamicSMILESFileReader.class.getName());
    //</editor-fold>
    //
    //<editor-fold desc="Private variables">
    /**
     * Number of lines that were skipped (empty or erroneous SMILES codes etc.) in the last file import of this instance,
     * headline not counted.
     */
    private int skippedLinesCounter;
    //</editor-fold>
    //
    //<editor-fold desc="Constructor">
    /**
     * Creates new instance. Initialises the skipped lines counter.
     */
    public DynamicSMILESFileReader() {
        this.skippedLinesCounter = 0;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public properties get">
    /**
     * Returns the number of lines that were skipped (empty or erroneous SMILES codes etc.) in the last file import of
     * this instance (reset at each new import), headline not counted.
     *
     * @return nr of lines skipped in last import
     */
    public int getSkippedLinesCounter() {
        return skippedLinesCounter;
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public static methods">
    /**
     * Checking the first few lines of the SMILES file for parsable SMILES codes and saving the determined separator
     * character and SMILES code and ID column positions.
     * Expects one parsable SMILES code per line of the file and an optional second element, which is interpreted as the
     * molecule's ID or name and is separated from the SMILES code by one of the separator tokens tab, semicolon, comma, or space.
     * Unsuitable for reaction SMILES or CxSMILES.
     *
     * @param aFile a SMILES file
     * @return determined format of the given file
     * @throws IOException if the file cannot be found or does not adhere to the format expectations.
     */
    public static DynamicSMILESFileFormat detectFormat(File aFile) throws IOException {
        try (
                // throws FileNotFoundException if file cannot be found, see catch block below
                FileReader tmpSmilesFileReader = new FileReader(aFile);
                BufferedReader tmpSmilesFileBufferedReader = new BufferedReader(tmpSmilesFileReader, BasicDefinitions.BUFFER_SIZE)
        ) {
            IChemObjectBuilder tmpBuilder = SilentChemObjectBuilder.getInstance();
            // AtomContainer to save the parsed SMILES in
            IAtomContainer tmpMolecule = tmpBuilder.newAtomContainer();
            SmilesParser tmpSmilesParser = new SmilesParser(tmpBuilder);
            String tmpSmilesFileDeterminedSeparator = String.valueOf(DynamicSMILESFileFormat.PLACEHOLDER_SEPARATOR_CHAR);
            int tmpSmilesCodeExpectedPosition = DynamicSMILESFileFormat.DEFAULT_SMILES_COLUMN_POSITION;
            int tmpIDExpectedPosition = DynamicSMILESFileFormat.PLACEHOLDER_ID_COLUMN_POSITION;
            int tmpCurrentLineInFileCounter = -1;
            String tmpSmilesFileCurrentLine;
            String tmpSmilesFileFirstLine = "";
            findSeparatorLoop:
            while (!Thread.currentThread().isInterrupted() && tmpCurrentLineInFileCounter < DynamicSMILESFileReader.MAXIMUM_LINE_NUMBER_TO_CHECK_IN_SMILES_FILES) {
                tmpSmilesFileCurrentLine = tmpSmilesFileBufferedReader.readLine();
                tmpCurrentLineInFileCounter++;
                if (Objects.isNull(tmpSmilesFileCurrentLine)) {
                    break findSeparatorLoop;
                }
                if (tmpCurrentLineInFileCounter == 0) {
                    // saved for determination of whether the file has a headline below
                    tmpSmilesFileFirstLine = tmpSmilesFileCurrentLine;
                }
                // first try the whole line because the file might have only one column
                if (!tmpSmilesFileCurrentLine.trim().isEmpty()
                        //not trimmed because whitespaces are invalid and should be detected
                        && DynamicSMILESFileReader.containsOnlySMILESValidCharacters(tmpSmilesFileCurrentLine)
                        && !DynamicSMILESFileReader.PARSABLE_SMILES_EXCEPTIONS.contains(tmpSmilesFileCurrentLine.trim())) {
                    try {
                        //if parsing fails goes to catch block below
                        // trimmed because a leading whitespace followed by a character string are interpreted as an empty structure and its name
                        tmpMolecule = tmpSmilesParser.parseSmiles(tmpSmilesFileCurrentLine.trim());
                        if (!tmpMolecule.isEmpty()) {
                            //success, SMILES column is identified
                            tmpSmilesCodeExpectedPosition = 0;
                            break findSeparatorLoop;
                        }
                    } catch (InvalidSmilesException anException) {
                        // do nothing, continue with splitting the line using different separators
                    }
                }
                for (String tmpSeparator : DynamicSMILESFileReader.POSSIBLE_SMILES_FILE_SEPARATORS) {
                    // limit param = 3 because we assume that SMILES code and ID are  in the first two columns
                    String[] tmpProcessedLineArray = tmpSmilesFileCurrentLine.split(tmpSeparator, 3);
                    for (int i = 0; i < tmpProcessedLineArray.length; i++) {
                        String tmpNextElementOfLine = tmpProcessedLineArray[i];
                        if (tmpNextElementOfLine.trim().isEmpty()
                                || !DynamicSMILESFileReader.containsOnlySMILESValidCharacters(tmpNextElementOfLine)
                                || DynamicSMILESFileReader.PARSABLE_SMILES_EXCEPTIONS.contains(tmpNextElementOfLine.trim())) {
                            continue; //... to try next element in row
                        }
                        // check only the first two columns, see comment above
                        if (i > 1) {
                            break; //... try next separator
                        }
                        try {
                            //if parsing fails goes to catch block below
                            tmpMolecule = tmpSmilesParser.parseSmiles(tmpNextElementOfLine.trim());
                            if (!tmpMolecule.isEmpty()) {
                                //success, separator and SMILES column are identified
                                tmpSmilesFileDeterminedSeparator = tmpSeparator;
                                tmpSmilesCodeExpectedPosition = i;
                                if (tmpProcessedLineArray.length > 1) {
                                    tmpIDExpectedPosition = tmpSmilesCodeExpectedPosition == 0 ? 1 : 0;
                                }
                                break findSeparatorLoop;
                            } //else {
                                // continue to try next element in row
                            //}
                        } catch (InvalidSmilesException anException) {
                            // continue to try next element in row
                        }
                    }
                }
            }
            if (tmpMolecule.isEmpty()) {
                throw new IOException("Chosen file does not fit to the expected format of a SMILES file.");
            }
            boolean tmpHasHeaderLine;
            try {
                if (tmpIDExpectedPosition == -1) {
                    tmpSmilesParser.parseSmiles(tmpSmilesFileFirstLine.trim());
                } else {
                    tmpSmilesParser.parseSmiles(tmpSmilesFileFirstLine.trim().split(tmpSmilesFileDeterminedSeparator, 3)[tmpSmilesCodeExpectedPosition]);
                }
                tmpHasHeaderLine = false;
            } catch (InvalidSmilesException anException) {
                tmpHasHeaderLine = true;
            }
            return new DynamicSMILESFileFormat(tmpHasHeaderLine, tmpSmilesFileDeterminedSeparator.charAt(0), tmpSmilesCodeExpectedPosition, tmpIDExpectedPosition);
        } catch (FileNotFoundException anException) {
            String tmpMessage = "File " + aFile.getPath() + " could not be found";
            DynamicSMILESFileReader.LOGGER.log(Level.SEVERE, tmpMessage);
            throw new IOException(tmpMessage);
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Public methods">
    /**
     * Reads SMILES file according to the given format. Splits the lines at the given separator character, ignores the
     * first line if the format defines that the file has a headline, parses SMILES codes and IDs from the defined columns, etc.
     * Skipped lines (due to being empty or containing erroneous SMILES codes) are counted and this counter can be queried
     * after import via the respective getter method. If a name/ID column is given in the file, it is read and saved as
     * a property of the respective atom container under the name property key taken from the Importer class.
     *
     * @param aFile a SMILES file
     * @param aFormat the determined format of the file
     * @return atom container set parsed from the file
     * @throws IOException if the given file cannot be found
     */
    public IAtomContainerSet readFile(File aFile, DynamicSMILESFileFormat aFormat) throws IOException {
        try (
                // throws FileNotFoundException if file cannot be found, see catch block below
                FileReader tmpSmilesFileReader = new FileReader(aFile);
                BufferedReader tmpSmilesFileBufferedReader = new BufferedReader(tmpSmilesFileReader, BasicDefinitions.BUFFER_SIZE)
        ) {
            IAtomContainerSet tmpAtomContainerSet = new AtomContainerSet();
            IChemObjectBuilder tmpBuilder = SilentChemObjectBuilder.getInstance();
            // AtomContainer to save the parsed SMILES in
            IAtomContainer tmpMolecule;
            SmilesParser tmpSmilesParser = new SmilesParser(tmpBuilder);
            String tmpSmilesFileCurrentLine;
            String tmpSmilesFileDeterminedSeparator = aFormat.getSeparatorChar().toString();
            String[] tmpProcessedLineArray = new String[0];
            int tmpSmilesCodeExpectedPosition = aFormat.getSMILESCodeColumnPosition();
            int tmpIDExpectedPosition = aFormat.getIDColumnPosition();
            this.skippedLinesCounter = 0;
            int tmpLineInFileCounter = -1;
            if (aFormat.hasHeaderLine()) {
                tmpSmilesFileCurrentLine = tmpSmilesFileBufferedReader.readLine();
                tmpLineInFileCounter++;
            }
            while (!Thread.currentThread().isInterrupted() && (tmpSmilesFileCurrentLine = tmpSmilesFileBufferedReader.readLine()) != null) {
                tmpLineInFileCounter++;
                //trying to parse as SMILES code
                try {
                    String tmpSmiles;
                    if (aFormat.hasIDColumn()) {
                        tmpProcessedLineArray = tmpSmilesFileCurrentLine.split(tmpSmilesFileDeterminedSeparator, 3);
                        tmpSmiles = tmpProcessedLineArray[tmpSmilesCodeExpectedPosition].trim().isBlank() ? null :
                                tmpProcessedLineArray[tmpSmilesCodeExpectedPosition].trim();
                    } else {
                        tmpSmiles = tmpSmilesFileCurrentLine.trim();
                    }
                    if (tmpSmiles != null && !tmpSmiles.isEmpty()) {
                        //throws exception if SMILES string is null, goes to catch block
                        tmpMolecule = tmpSmilesParser.parseSmiles(tmpSmiles);
                    } else {
                        throw new InvalidSmilesException("String is empty");
                    }
                } catch (InvalidSmilesException | IndexOutOfBoundsException | NullPointerException anException) {
                    this.skippedLinesCounter++;
                    DynamicSMILESFileReader.LOGGER.log(Level.WARNING, String.format("Import failed for structure in line (starting at 0):\t%s", tmpLineInFileCounter));
                    continue;
                }
                //setting the name of the atom container
                String tmpName;
                if (aFormat.hasIDColumn() && tmpProcessedLineArray.length > 1 && !tmpProcessedLineArray[tmpIDExpectedPosition].trim().isEmpty()) {
                    tmpName = tmpProcessedLineArray[tmpIDExpectedPosition].trim();
                } else {
                    tmpName = FileUtil.getFileNameWithoutExtension(aFile) + tmpLineInFileCounter;
                }
                tmpMolecule.setProperty(Importer.MOLECULE_NAME_PROPERTY_KEY, tmpName);
                tmpAtomContainerSet.addAtomContainer(tmpMolecule);
            }
            return tmpAtomContainerSet;
        } catch (FileNotFoundException anException) {
            String tmpMessage = "File " + aFile.getPath() + " could not be found";
            DynamicSMILESFileReader.LOGGER.log(Level.SEVERE, tmpMessage);
            throw new IOException(tmpMessage);
        }
    }
    //</editor-fold>
    //
    //<editor-fold desc="Package private methods">
    /**
     * Check the given String for characters that are not defined in SMILES encoding. The allowed characters are
     * 0-9 (rings, hydrogen counts, charge counts, or isotopes),
     * a-z, A-Z (element symbols),
     * * (wildcard atoms),
     * [, ] (inorganic atoms or explicit environments),
     * -, + (charges or - for explicit single bonds),
     * @ (tetrahedral stereochemistry),
     * =, #, $ (bonds up to quadruple),
     * % (multi-digit ring numbers),
     * : (tautomer bond),
     * . (disconnected parts),
     * (, ) (branches),
     * /, \ (cis/trans stereochemistry).
     * All other characters, including whitespace characters, are not allowed.
     *
     * @param aPotentialSMILESString the string to test
     * @return true if the input string contains only characters defined in the SMILES format
     */
    static boolean containsOnlySMILESValidCharacters(String aPotentialSMILESString) {
        Pattern pattern = Pattern.compile("^[0-9a-zA-Z*\\[\\]\\-+@=#$%:.()/\\\\]+$");
        Matcher matcher = pattern.matcher(aPotentialSMILESString);
        return matcher.find();
    }
    //</editor-fold>
}
