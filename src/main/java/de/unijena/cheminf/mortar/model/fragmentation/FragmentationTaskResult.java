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

package de.unijena.cheminf.mortar.model.fragmentation;

/**
 * Data record class for detailed fragmentation task results reporting.
 *
 * @param exceptionsCount                     Nr. of exceptions that happened during molecule fragmentation.
 * @param moleculeProducedNoFragmentsCount    Nr. of molecules that were successfully(!) fragmented but produced no fragments.
 * @param moleculeProducedFragmentsCount      Nr. of molecules that were successfully(!) fragmented and produced fragments (to check whether all molecules are accounted for).
 * @param moleculeFailedGetAtomContainerCount Nr. of molecules that gave no atom container.
 * @param filteredMoleculesCount              Nr. of molecules that were filtered according to the respective fragmentation algorithm method.
 * @param fragmentFailedSmilesGenerationCount Nr. of fragments that produced no SMILES code.
 * @param unexpectedExceptionsCount           Nr. of unexpected exceptions that happened during molecule fragmentation (i.e. the whole process of preprocessing,
 *                                            fragmentation, and results analysis for one molecule).
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public record FragmentationTaskResult(
        int exceptionsCount,
        int moleculeProducedNoFragmentsCount,
        int moleculeProducedFragmentsCount,
        int moleculeFailedGetAtomContainerCount,
        int filteredMoleculesCount,
        int fragmentFailedSmilesGenerationCount,
        int unexpectedExceptionsCount
) {}
