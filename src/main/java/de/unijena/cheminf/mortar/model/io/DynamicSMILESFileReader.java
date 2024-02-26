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
    public static final String[] POSSIBLE_SMILES_FILE_SEPARATORS = {",", ";", " ", "\t"};
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
    private static final Logger LOGGER = Logger.getLogger(DynamicSMILESFileReader.class.getName());
    //
    /**
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
            String tmpSmilesFileNextLine = "";
            String tmpSmilesFileDeterminedSeparator = null;
            String[] tmpProcessedLineArray = null;
            int tmpSmilesCodeExpectedPosition = -1;
            int tmpIDExpectedPosition = -1;
            int tmpLineInFileCounter = -1;
            // marking the BufferedReader to reset the reader after checking the format and determining the separator
            tmpSmilesFileBufferedReader.mark(BasicDefinitions.BUFFER_SIZE);
            // as potential headline, the first line should be avoided for separator determination
            String tmpSmilesFileFirstLine = tmpSmilesFileBufferedReader.readLine();
            tmpLineInFileCounter++;
            /*
                Checking for parsable SMILES code and saving the determined separator character and SMILES code and ID column positions.
                If no parsable SMILES code is found in the second and third line of the file, tmpMolecule stays empty
                and the file is assumed to be no SMILES file -> return null
             */
            findSeparatorLoop:
            while (!Thread.currentThread().isInterrupted() && tmpLineInFileCounter < DynamicSMILESFileReader.MAXIMUM_LINE_NUMBER_TO_CHECK_IN_SMILES_FILES) {
                tmpSmilesFileNextLine = tmpSmilesFileBufferedReader.readLine();
                tmpLineInFileCounter++;
                if (tmpSmilesFileNextLine == null) {
                    // if the file's end is reached at this point, the skipped first line is tried out to determine the separator as a last resort
                    if (tmpSmilesFileFirstLine != null || !tmpSmilesFileFirstLine.isEmpty()) {
                        tmpSmilesFileNextLine = tmpSmilesFileFirstLine;
                        tmpSmilesFileFirstLine = null;
                    } else {
                        break;
                    }
                }
                for (String tmpSeparator : DynamicSMILESFileReader.POSSIBLE_SMILES_FILE_SEPARATORS) {
                    tmpProcessedLineArray = tmpSmilesFileNextLine.split(tmpSeparator);
                    int tmpColumnIndex = 0;
                    for (String tmpNextElementOfLine : tmpProcessedLineArray) {
                        if (tmpNextElementOfLine.isEmpty() || tmpColumnIndex > 2) {
                            continue;
                        }
                        if (!DynamicSMILESFileReader.containsOnlySMILESValidCharacters(tmpNextElementOfLine) || tmpNextElementOfLine.isEmpty() || tmpNextElementOfLine.equals("ID")) {
                            continue;
                        }
                        try {
                            tmpMolecule = tmpSmilesParser.parseSmiles(tmpNextElementOfLine);
                            if (!tmpMolecule.isEmpty()) {
                                tmpSmilesFileDeterminedSeparator = tmpSeparator;
                                tmpSmilesCodeExpectedPosition = tmpColumnIndex;
                                if (tmpProcessedLineArray.length > 1) {
                                    if (tmpSmilesCodeExpectedPosition == 0) {
                                        tmpIDExpectedPosition = 1;
                                    } else {
                                        tmpIDExpectedPosition = 0;
                                    }
                                }
                                break findSeparatorLoop;
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
                    tmpMolecule = tmpSmilesParser.parseSmiles(tmpSmilesFileFirstLine.split(tmpSmilesFileDeterminedSeparator)[tmpSmilesCodeExpectedPosition]);
                    tmpHasHeaderLine = tmpMolecule == null? false : true;
                } catch (InvalidSmilesException anException) {
                    tmpHasHeaderLine = false;
                }
            }
            if (tmpIDExpectedPosition == -1) {
                return new DynamicSMILESFileFormat(tmpSmilesFileDeterminedSeparator.charAt(0), tmpSmilesCodeExpectedPosition, tmpHasHeaderLine);
            } else {
                return new DynamicSMILESFileFormat(tmpSmilesFileDeterminedSeparator.charAt(0), tmpSmilesCodeExpectedPosition, tmpHasHeaderLine, tmpIDExpectedPosition);
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
            IAtomContainer tmpMolecule = tmpBuilder.newAtomContainer();
            SmilesParser tmpSmilesParser = new SmilesParser(tmpBuilder);
            String tmpSmilesFileNextLine = "";
            String tmpSmilesFileDeterminedSeparator = aFormat.getSeparatorChar().toString();
            String[] tmpProcessedLineArray = null;
            int tmpSmilesCodeExpectedPosition = aFormat.getSMILESCodeColumnPosition();
            int tmpIDExpectedPosition = aFormat.getIdColumnPosition();
            int tmpSmilesFileParsableLinesCounter = 0;
            int tmpSmilesFileInvalidLinesCounter = 0;
            int tmpLineInFileCounter = -1;
            // marking the BufferedReader to reset the reader after checking the format and determining the separator
            tmpSmilesFileBufferedReader.mark(BasicDefinitions.BUFFER_SIZE);
            // as potential headline, the first line should be avoided for separator determination
            String tmpSmilesFileFirstLine = tmpSmilesFileBufferedReader.readLine();
            tmpLineInFileCounter++;
            while (!Thread.currentThread().isInterrupted() && (tmpSmilesFileNextLine = tmpSmilesFileBufferedReader.readLine()) != null) {
                //trying to parse as SMILES code
                try {
                    tmpProcessedLineArray = tmpSmilesFileNextLine.split(tmpSmilesFileDeterminedSeparator, 3);
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
                    int tmpIndexInFile = tmpSmilesFileParsableLinesCounter + tmpSmilesFileInvalidLinesCounter - 1;
                    tmpName = FileUtil.getFileNameWithoutExtension(aFile) + tmpIndexInFile;
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
