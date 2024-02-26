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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 *
 * @author Jonas Schaub, Samuel Behr
 */
public class DynamicSMILESFileReader {
    /**
     * Possible SMILES file separators used to separate SMILES code from ID. Ordered so that non-whitespace characters
     * are tested first.
     */
    public static final String[] POSSIBLE_SMILES_FILE_SEPARATORS = {"\n", ",", ";", " ", "\t"};
    //
    /**
     * Maximum number of lines starting from the first one to check for valid SMILES strings in a SMILES file when trying to determine
     * the SMILES code column and separator.
     */
    public static final int MAXIMUM_LINE_NUMBER_TO_CHECK_IN_SMILES_FILES = 10;
    //
    /**
     *
     */
    public static final List<String> PARSABLE_STRING_EXCEPTIONS = new ArrayList<>(List.of(new String[]{"ID"}));
    //
    /**
     *
     */
    private static final Logger LOGGER = Logger.getLogger(DynamicSMILESFileReader.class.getName());
    //
    private DynamicSMILESFileReader() {

    }
    //
    /**
     * Checking for parsable SMILES code and saving the determined separator character and SMILES code and ID column positions.
     *
     * @param aFile
     * @return
     * @throws IOException
     */
    public static DynamicSMILESFileFormat detectFormat(File aFile) throws IOException {
        try (
                FileReader tmpSmilesFileReader = new FileReader(aFile);
                BufferedReader tmpSmilesFileBufferedReader = new BufferedReader(tmpSmilesFileReader, BasicDefinitions.BUFFER_SIZE)
        ) {
            IChemObjectBuilder tmpBuilder = SilentChemObjectBuilder.getInstance();
            // AtomContainer to save the parsed SMILES in
            IAtomContainer tmpMolecule = tmpBuilder.newAtomContainer();
            SmilesParser tmpSmilesParser = new SmilesParser(tmpBuilder);
            String tmpSmilesFileDeterminedSeparator = null;
            int tmpSmilesCodeExpectedPosition = -1;
            int tmpIDExpectedPosition = -1;
            int tmpCurrentLineInFileCounter = -1;
            String tmpSmilesFileCurrentLine = tmpSmilesFileBufferedReader.readLine();
            //current line read is 0th line
            tmpCurrentLineInFileCounter++;
            // as potential headline, the first (0th) line should be avoided for separator determination
            String tmpSmilesFileFirstLine = tmpSmilesFileCurrentLine;
            findSeparatorLoop:
            while (!Thread.currentThread().isInterrupted() && tmpCurrentLineInFileCounter < DynamicSMILESFileReader.MAXIMUM_LINE_NUMBER_TO_CHECK_IN_SMILES_FILES) {
                tmpSmilesFileCurrentLine = tmpSmilesFileBufferedReader.readLine();
                tmpCurrentLineInFileCounter++;
                if (Objects.isNull(tmpSmilesFileCurrentLine)) {
                    // if the file's end is reached at this point, the skipped first line is tried out to determine the separator as a last resort
                    // it must contain a SMILES code, so not a headline, for this to work
                    if (!Objects.isNull(tmpSmilesFileFirstLine) || !tmpSmilesFileFirstLine.isEmpty()) {
                        tmpSmilesFileCurrentLine = tmpSmilesFileFirstLine;
                        // to indicate that the potential headline has been analysed
                        tmpSmilesFileFirstLine = null;
                    } else {
                        // first line is empty
                        break;
                    }
                }
                for (String tmpSeparator : DynamicSMILESFileReader.POSSIBLE_SMILES_FILE_SEPARATORS) {
                    String[] tmpProcessedLineArray = tmpSmilesFileCurrentLine.split(tmpSeparator, 3);
                    int tmpColumnIndex = 0;
                    for (String tmpNextElementOfLine : tmpProcessedLineArray) {
                        if (
                                tmpNextElementOfLine.trim().isEmpty()
                                || !DynamicSMILESFileReader.containsOnlySMILESValidCharacters(tmpNextElementOfLine)
                                || DynamicSMILESFileReader.PARSABLE_STRING_EXCEPTIONS.contains(tmpNextElementOfLine)
                        ) {
                            continue; //... to try next element in row
                        }
                        // check only the first two columns
                        if (tmpColumnIndex > 1) {
                            break; //... try next separator
                        }
                        try {
                            //if parsing fails goes to catch block below
                            tmpMolecule = tmpSmilesParser.parseSmiles(tmpNextElementOfLine);
                            if (!tmpMolecule.isEmpty()) {
                                tmpSmilesFileDeterminedSeparator = tmpSeparator;
                                tmpSmilesCodeExpectedPosition = tmpColumnIndex;
                                if (tmpProcessedLineArray.length > 1) {
                                    tmpIDExpectedPosition = tmpSmilesCodeExpectedPosition == 0 ? 1 : 0;
                                }
                                break findSeparatorLoop;
                            } else {
                                tmpColumnIndex++;
                            }
                        } catch (InvalidSmilesException anException) {
                            tmpColumnIndex++;
                        }
                    }
                }
            }
            if (tmpMolecule.isEmpty()) {
                throw new IOException("Chosen file does not fit to the expected format of a SMILES file.");
            }
            boolean tmpHasHeaderLine;
            if (tmpSmilesFileFirstLine == null) {
                tmpHasHeaderLine = false;
            } else {
                try {
                    tmpMolecule = tmpSmilesParser.parseSmiles(tmpSmilesFileFirstLine.split(tmpSmilesFileDeterminedSeparator, 3)[tmpSmilesCodeExpectedPosition]);
                    tmpHasHeaderLine = false;
                } catch (InvalidSmilesException anException) {
                    tmpHasHeaderLine = true;
                }
            }
            if (tmpIDExpectedPosition == -1) {
                return new DynamicSMILESFileFormat(tmpHasHeaderLine);
            } else {
                return new DynamicSMILESFileFormat(tmpHasHeaderLine, tmpSmilesFileDeterminedSeparator.charAt(0), tmpSmilesCodeExpectedPosition, tmpIDExpectedPosition);
            }
        } catch (FileNotFoundException anException) {
            String tmpMessage = "File " + aFile.getPath() + " could not be found";
            DynamicSMILESFileReader.LOGGER.log(Level.SEVERE, tmpMessage);
            throw new IOException(tmpMessage);
        }
    }
    //
    /**
     *
     * @param aFile
     * @param aFormat
     * @return
     * @throws IOException
     */
    public static IAtomContainerSet readFile(File aFile, DynamicSMILESFileFormat aFormat) throws IOException {
        try (
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
            String[] tmpProcessedLineArray;
            int tmpSmilesCodeExpectedPosition = aFormat.getSMILESCodeColumnPosition();
            int tmpIDExpectedPosition = aFormat.getIDColumnPosition();
            int tmpSmilesFileParsableLinesCounter = 0;
            int tmpSmilesFileInvalidLinesCounter = 0;
            int tmpLineInFileCounter = -1;
            if (aFormat.hasHeaderLine()) {
                tmpSmilesFileCurrentLine = tmpSmilesFileBufferedReader.readLine();
                tmpLineInFileCounter++;
            }
            while (!Thread.currentThread().isInterrupted() && (tmpSmilesFileCurrentLine = tmpSmilesFileBufferedReader.readLine()) != null) {
                tmpLineInFileCounter++;
                //trying to parse as SMILES code
                try {
                    tmpProcessedLineArray = tmpSmilesFileCurrentLine.split(tmpSmilesFileDeterminedSeparator, 3);
                    String tmpSmiles = tmpProcessedLineArray[tmpSmilesCodeExpectedPosition].isBlank() ? null :
                            tmpProcessedLineArray[tmpSmilesCodeExpectedPosition];
                    //throws exception if SMILES string is null, goes to catch block
                    tmpMolecule = tmpSmilesParser.parseSmiles(tmpSmiles);
                    tmpSmilesFileParsableLinesCounter++;
                }
                catch (InvalidSmilesException | IndexOutOfBoundsException | NullPointerException anException) {
                    int tmpIndexInFile = tmpSmilesFileParsableLinesCounter + tmpSmilesFileInvalidLinesCounter;
                    DynamicSMILESFileReader.LOGGER.info("Contains no parsable SMILES string: line " + tmpIndexInFile
                            + " (index).");
                    tmpSmilesFileInvalidLinesCounter++;
                    continue;
                }
                //setting the name of the atom container
                String tmpName = "";
                if (tmpProcessedLineArray.length > 1 && !tmpProcessedLineArray[tmpIDExpectedPosition].isEmpty()) {
                    tmpName = tmpProcessedLineArray[tmpIDExpectedPosition];
                } else {
                    tmpName = FileUtil.getFileNameWithoutExtension(aFile) + tmpLineInFileCounter;
                }
                tmpMolecule.setProperty(Importer.MOLECULE_NAME_PROPERTY_KEY, tmpName);
                //adding tmpMolecule to the AtomContainerSet
                tmpAtomContainerSet.addAtomContainer(tmpMolecule);
            }
            if (tmpSmilesFileInvalidLinesCounter > 0) {
                DynamicSMILESFileReader.LOGGER.info("\tSmilesFile ParsableLinesCount:\t" + tmpSmilesFileParsableLinesCounter +
                        "\n\tSmilesFile InvalidLinesCount:\t" + tmpSmilesFileInvalidLinesCounter);
            }
            return tmpAtomContainerSet;
        } catch (FileNotFoundException anException) {
            String tmpMessage = "File " + aFile.getPath() + " could not be found";
            DynamicSMILESFileReader.LOGGER.log(Level.SEVERE, tmpMessage);
            throw new IOException(tmpMessage);
        }
    }
    //
    /**
     *
     *
     * @param aPotentialSMILESString the string to test
     * @return true if the input string contains only characters defined in the SMILES format
     */
    static boolean containsOnlySMILESValidCharacters(String aPotentialSMILESString) {
        Pattern pattern = Pattern.compile("^[0-9a-zA-Z*\\[\\]\\-+@=#$%:.()/\\\\]+$");
        Matcher matcher = pattern.matcher(aPotentialSMILESString);
        return matcher.find();
    }
}
