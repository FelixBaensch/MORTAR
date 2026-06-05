package de.unijena.cheminf.mortar.model.io;

import de.unijena.cheminf.mortar.model.data.MoleculeDataModel;
import de.unijena.cheminf.mortar.model.util.ChemUtil;
import de.unijena.cheminf.mortar.model.util.FileUtil;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.aromaticity.Kekulization;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.io.FormatFactory;
import org.openscience.cdk.io.IChemObjectReader;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.io.MDLV3000Reader;
import org.openscience.cdk.io.formats.IChemFormat;
import org.openscience.cdk.io.formats.MDLV2000Format;
import org.openscience.cdk.io.formats.MDLV3000Format;
import org.openscience.cdk.io.iterator.IteratingSDFReader;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.HydrogenState;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;


public record ParseMolTask(
    Importer.MoleculeChunk chunkOfMolecules,
    boolean isRegardStereo,
    boolean isKekulizationEnforced,
    boolean isFillOpenValencesWithImplH,
    List<MoleculeDataModel> resultList,
    AtomicInteger totalParsed) implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        Objects.requireNonNull(
                this.chunkOfMolecules.mappedSegment(),
                "the memory provided for parsing is null"
        );
        IAtomContainerSet tmpImportedMoleculesSet = switch (this.chunkOfMolecules().importFileType()) {
            case Importer.ValidImportFileTypes.MOL_FILE -> this.importMolFile(aFile);
            case Importer.ValidImportFileTypes.STRUCTURE_DATA_FORMAT_FILE -> this.importSDFile(aFile);
            //Needs more work before it can be made available
            /*case ".pdb":
                tmpImportedMoleculesSet = this.importPDBFile(aFile);
                break;*/
            case Importer.ValidImportFileTypes.SMILES_FILE, Importer.ValidImportFileTypes.TEXT_FILE,
                 Importer.ValidImportFileTypes.COMMA_SEPARATED_VALUES_FILE, Importer.ValidImportFileTypes.TAB_SEPARATED_VALUES_FILE ->
                    this.importSMILESFile(aFile);
        };
        this.preprocessMoleculeSet(tmpImportedMoleculesSet, this.isFillOpenValencesWithImplH);
        this.fileName = aFile.getName();
        return this.parse(tmpImportedMoleculesSet, this.isRegardStereo, this.isKekulizationEnforced);
    }


    /**
     * Parses an atom container set into a list of the MORTAR-internal MoleculeDataModel instances. If the parameter is null or empty, an empty
     * list is returned. Most time-consuming step is the SMILES generation, especially if stereochemistry is regarded because
     * then, the InChI numbering algorithm is used. Logs the size of the input data set and the number of exceptions that occurred during
     * SMILES generation (leads to molecule not being parsed into MoleculeDataModel).
     *
     * @param anAtomContainer the set to parse
     * @param isRegardStereo whether stereochemistry should be encoded in the SMILES strings
     * @param isKekulizationEnforced whether imported molecules should always be kekulized, which means aromaticity
     *                               will not(!) be encoded in the internal SMILES strings (if false, aromaticity will be(!) encoded)
     * @return list of MoleculeDataModel instances or empty list if the input set is empty or null
     */
    private MoleculeDataModel parse(IAtomContainer anAtomContainer, boolean isRegardStereo, boolean isKekulizationEnforced) {
        if (anAtomContainer == null || anAtomContainer.isEmpty()) {
            return null;
        }
        //returns null if no SMILES code could be created
        String tmpSmiles = ChemUtil.createUniqueSmiles(anAtomContainer, isRegardStereo, !isKekulizationEnforced);
        if (tmpSmiles == null || tmpSmiles.isBlank()) {
            return null;
        }
        MoleculeDataModel tmpMoleculeDataModel;
        tmpMoleculeDataModel = new MoleculeDataModel(tmpSmiles, anAtomContainer.getTitle(), anAtomContainer.getProperties());
        tmpMoleculeDataModel.setName(anAtomContainer.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY));
        return tmpMoleculeDataModel;
    }

    /**
     * Imports a mol file as AtomContainer and adds the first line of the mol file (name of the
     * molecule in most cases) as "name-property". MDL v2000 and v3000 MOL files are accepted and the used format
     * determined automatically.
     *
     * @param aFile mol file
     * @return the imported molecule in an IAtomContainerSet
     * @throws CDKException if the given mol file cannot be read
     * @throws IOException if the given file cannot be found or read
     */
    private IAtomContainerSet importMolFile(File aFile) throws IOException, CDKException {
        IAtomContainerSet tmpAtomContainerSet = new AtomContainerSet();
        IChemFormat tmpFormat;
        try (BufferedInputStream tmpInputStream = new BufferedInputStream(new FileInputStream(aFile))) {
            FormatFactory tmpFactory = new FormatFactory();
            tmpFormat = tmpFactory.guessFormat(tmpInputStream);
        }
        if (tmpFormat == null) {
            throw new CDKException("The given file type could not be determined");
        }
        IAtomContainer tmpAtomContainer;
        if (tmpFormat.getFormatName().equalsIgnoreCase(MDLV2000Format.getInstance().getFormatName())) {
            try (MDLV2000Reader tmpReader = new MDLV2000Reader(new FileInputStream(aFile), IChemObjectReader.Mode.RELAXED)) {
                tmpAtomContainer = tmpReader.read(new AtomContainer());
            }
        } else if (tmpFormat.getFormatName().equalsIgnoreCase(MDLV3000Format.getInstance().getFormatName())) {
            try (MDLV3000Reader tmpReader = new MDLV3000Reader(new FileInputStream(aFile), IChemObjectReader.Mode.RELAXED)) {
                tmpAtomContainer = tmpReader.read(new AtomContainer());
            }
        } else {
            throw new CDKException("The mol file does not correspond to either the MDLV2000 or the MDLV3000 format and " +
                    "therefore cannot be imported.");
        }
        String tmpName = this.findMoleculeName(tmpAtomContainer);
        if (tmpName == null) {
            try (BufferedReader tmpBufferedReader = new BufferedReader(new FileReader(aFile))) {
                tmpName = tmpBufferedReader.readLine();
                if (tmpName == null || tmpName.isBlank()) {
                    tmpName = FileUtil.getFileNameWithoutExtension(aFile);
                }
            }
        }
        tmpAtomContainer.setProperty(Importer.MOLECULE_NAME_PROPERTY_KEY, tmpName);
        tmpAtomContainerSet.addAtomContainer(tmpAtomContainer);
        return tmpAtomContainerSet;
    }


    /**
     * Imports an SD file. If no name can be detected for a structure, the file name extended with the index of the
     * structure in the file is used as name of the structure.
     * NOTE: if multiple erroneous entries in a row are there in the input file, they are skipped together and not
     * logged individually!
     *
     * @param aFile sdf
     * @return the imported molecules in an IAtomContainerSet
     * @throws IOException if a file input stream cannot be opened or closed for the given file
     */
    private IAtomContainerSet importSDFile(File aFile) throws IOException {
        IAtomContainerSet tmpAtomContainerSet = new AtomContainerSet();
        /*the IteratingSDFReader is not set to skip erroneous input molecules in its constructor to be able to log them*/
        try (IteratingSDFReader tmpSDFReader = new IteratingSDFReader(new FileInputStream(aFile), SilentChemObjectBuilder.getInstance())) {
            int tmpCounter = 0;
            while (!Thread.currentThread().isInterrupted()) {
                //end of file or encountered erroneous entry
                if (!tmpSDFReader.hasNext()) {
                    //skip if it is an erroneous entry
                    tmpSDFReader.setSkip(true);
                    if (!tmpSDFReader.hasNext()) {
                        if (tmpCounter == 0) {
                            Importer.LOGGER.log(Level.WARNING, "Import failed for first and only structure in the file");
                        }
                        // there is no next, end of file!
                        break;
                    }
                    // molecule just could not be read and has therefore been skipped, restore skip setting for next iteration
                    tmpSDFReader.setSkip(false);
                    Importer.LOGGER.log(Level.WARNING, "Import failed for structure: {0} (index of structure in file).", tmpCounter);
                    tmpCounter++;
                }
                IAtomContainer tmpAtomContainer = tmpSDFReader.next();
                String tmpName = this.findMoleculeName(tmpAtomContainer);
                if (tmpName == null || tmpName.isBlank()) {
                    // the counter here equals the index of the structure in the file
                    tmpName = FileUtil.getFileNameWithoutExtension(aFile) + tmpCounter;
                }
                tmpAtomContainer.setProperty(Importer.MOLECULE_NAME_PROPERTY_KEY, tmpName);
                tmpAtomContainerSet.addAtomContainer(tmpAtomContainer);
                tmpCounter++;
            }
            int tmpFailedImportsCount = tmpCounter - tmpAtomContainerSet.getAtomContainerCount();
            if (tmpFailedImportsCount > 0) {
                Importer.LOGGER.log(Level.WARNING, "The import from SD file failed for a total of {0} structure(s).", tmpFailedImportsCount);
            }
            return tmpAtomContainerSet;
        }
    }
    //
    /**
     * Imports a SMILES file. This method is able to parse differently formatted SMILES files, e.g. with and without header
     * and with one up to many columns (SMILES, name/ID, and additional columns). The method identifies the used
     * format by reading the first three lines of the file and then applying the detected format on all further lines.
     * SMILES and name/ID strings are expected to be in the first two columns. Files that do not fit to the expected
     * format or lack a parsable SMILES string in the first ten lines are classified as not being a SMILES file and
     * an exception gets thrown. If no name can be detected for a structure, the structure
     * is assigned the name of the file extended with the index of the structure in the file as name.
     *
     * @param aFile a SMILES codes-containing *.txt, *.csv, *.tsv, or *.smi file
     * @return the imported molecules in an IAtomContainerSet
     * @throws IOException if the given file does not fit to the expected format of a SMILES file
     * @author Samuel Behr
     * @author Jonas Schaub
     */
    private IAtomContainerSet importSMILESFile(File aFile) throws IOException {
        DynamicSMILESFileFormat tmpFormat = DynamicSMILESFileReader.detectFormat(aFile);
        DynamicSMILESFileReader tmpReader = new DynamicSMILESFileReader();
        // checks whether thread has been interrupted, logs faulty structures, and assigns names like the other methods
        IAtomContainerSet tmpAtomContainerSet = tmpReader.readFile(aFile, tmpFormat);
        if (tmpReader.getSkippedLinesCounter() > 0) {
            Importer.LOGGER.log(Level.WARNING, "The import from SMILES file failed for a total of {0} structures.",
                    tmpReader.getSkippedLinesCounter());
        }
        return tmpAtomContainerSet;
    }

    /**
     * Searches the properties of the given atom container for a property
     * containing either 'name' or 'ID' in its key string and
     * returns the corresponding value as a string.
     * If nothing is found or value equals 'None', null is returned.
     *
     * @param anAtomContainer IAtomContainer
     * @return the identified molecule name or null
     */
    private String findMoleculeName(IAtomContainer anAtomContainer) {
        String tmpName = anAtomContainer.getTitle();
        Set<String> keySet = (Set<String>)(Set<?>)anAtomContainer.getProperties().keySet();
        if (tmpName == null  && keySet.stream().anyMatch(k -> (k.toLowerCase().contains("name")) && !k.equalsIgnoreCase("Database_Name"))) {
            Optional<String> tmpKeyOptional = keySet.stream().filter(k -> k.toLowerCase().contains("name")).findFirst();
            if (tmpKeyOptional.isPresent()) {
                String key = tmpKeyOptional.get();
                tmpName = anAtomContainer.getProperty(key);
            }
        }
        if ((tmpName == null || tmpName.equalsIgnoreCase("None")) && keySet.stream().anyMatch(k -> k.toLowerCase().contains("id"))) {
            Optional<String> tmpKeyOptional = keySet.stream().filter(k -> k.toLowerCase().contains("id")).findFirst();
            if (tmpKeyOptional.isPresent()) {
                String key = tmpKeyOptional.get();
                tmpName = anAtomContainer.getProperty(key);
            }
        }
        if (tmpName != null && (tmpName.equalsIgnoreCase("None"))) {
            tmpName = null;
        }
        return tmpName;
    }
    //
    /**
     * Iterates over the given molecule set once to do general preprocessing, i.e. assigning atom types and bond orders
     * (kekulization) and suppressing explicit hydrogen atoms that can also be represented as an implicit hydrogen count
     * on the respective atom. If the respective setting is activated, empty valences on the atom are completed with implicit
     * hydrogen atoms as well. Molecules that cause an exception in the routine are logged but remain in the given set.
     * note: Things like assigning bond orders and atom types here is redundant if the atom containers
     * are discarded after molecule set import and molecular information only represented by SMILES codes in
     * the molecule data models. Nevertheless, it is done here to ensure that the generated SMILES codes are correct.
     *
     * @param aMoleculeSet the molecule set to process; may be empty but not null
     * @param isFillOpenValencesWithImplH whether open valences in the imported molecules should be filled with implicit
     *                                    hydrogen atoms
     * @throws NullPointerException if the given molecule set is null
     */
    protected void preprocessMoleculeSet(IAtomContainerSet aMoleculeSet, boolean isFillOpenValencesWithImplH) throws NullPointerException {
        Objects.requireNonNull(aMoleculeSet, "given molecule set is null.");
        if (aMoleculeSet.isEmpty()) {
            return;
        }
        int tmpExceptionsCounter = 0;
        int tmpMoleculesWithRadicalsCounter = 0;
        for (IAtomContainer tmpMolecule : aMoleculeSet.atomContainers()) {
            try {
                // perceive atom types and configure atoms is always done as preprocessing
                AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(tmpMolecule);
                //if open valences should be filled with implicit hydrogens, fix radicals first and then saturate everything
                if (isFillOpenValencesWithImplH) {
                    if (tmpMolecule.getSingleElectronCount() > 0) {
                        ChemUtil.fixRadicals(tmpMolecule);
                        tmpMoleculesWithRadicalsCounter++;
                    }
                    CDKHydrogenAdder.getInstance(tmpMolecule.getBuilder()).addImplicitHydrogens(tmpMolecule);
                    //otherwise, just set implicit hydrogen counts to zero if unset to prevent exceptions
                } else {
                    for (IAtom tmpAtom : tmpMolecule.atoms()) {
                        if (tmpAtom.getImplicitHydrogenCount() == CDKConstants.UNSET
                                || tmpAtom.getImplicitHydrogenCount() == null) {
                            tmpAtom.setImplicitHydrogenCount(0);
                        }
                    }
                }
                /* note: the doc says: "All explicit hydrogens that can safely be converted to a count on their attached
                atom will be suppressed." Therefore, there will still be some explicit hydrogen atoms! */
                AtomContainerManipulator.normalizeHydrogens(tmpMolecule, HydrogenState.Minimal);
                //might throw exceptions if the implicit hydrogen count is unset or kekulization is impossible
                Kekulization.kekulize(tmpMolecule);
            } catch (Exception anException) {
                Importer.LOGGER.log(Level.WARNING,
                        String.format("%s molecule name: %s", anException.toString(), tmpMolecule.getProperty(Importer.MOLECULE_NAME_PROPERTY_KEY)),
                        anException);
                tmpExceptionsCounter++;
                // note: an exception here does not lead to the causing molecule being removed from the input set!
            }
        }
        if (!isFillOpenValencesWithImplH) {
            Importer.LOGGER.log(Level.INFO, "Imported and preprocessed molecule set. {0} exceptions occurred while processing.",
                    tmpExceptionsCounter);
        } else {
            Importer.LOGGER.log(Level.INFO, "Imported and preprocessed molecule set. {0} exceptions occurred while processing, " +
                            "{1} molecules with radicals were fixed and saturated with implicit hydrogens.",
                    new Object[]{tmpExceptionsCounter, tmpMoleculesWithRadicalsCounter});
        }
    }
}
