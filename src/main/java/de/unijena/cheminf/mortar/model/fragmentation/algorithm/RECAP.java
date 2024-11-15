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

package de.unijena.cheminf.mortar.model.fragmentation.algorithm;

import org.openscience.cdk.interfaces.IAtomContainer;

/**
 * Implementation of the
 * <a href="https://doi.org/10.1186/s13321-017-0225-z">RECAP - Retrosynthetic Combinatorial Analysis Procedure</a>
 * algorithm.
 * The SMIRKS strings are taken from the
 * <a href="https://github.com/rdkit/rdkit/blob/master/rdkit/Chem/Recap.py">RDKit implementation</a>
 * done by Greg Landrum (last access 15th Nov 2024).
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class RECAP {
    //TODO option for minimum fragment size (also described in RECAP paper)
    //TODO option for "exhaustive" fragmentation
    //TODO implement tests from RECAP paper and RDKit
    //TODO make individual rules able to be turned off and on?
    //TODO give option to add rules
    /**
     * String array of SMIRKS reaction transform codes that describe the cleavage rules.
     */
    private static final String[] CLEAVAGE_RULES_SMIRKS = {
            //TODO separate them into String constants and collect them here
            //1 = Amide -> aliphatic C that is NOT connected to two N (as not to match urea) (index 1),
            // connected via a non-ring double bond to an aliphatic O (index 2), connected
            // via a non-ring bond to N with a neutral charge and a degree of not 1,
            // can be aliphatic or aromatic (index 3)
            // reacts to C index 1 connected to O index 2 and to any other atom
            // and N index 3 connected to any other atom
            // note that the result is an aldehyde and an amine, not a carboxylic acid or ester and an amine
            // note also that the atoms can potentially be in a ring, just not the bonds
            "[C;!$(C([#7])[#7]):1](=!@[O:2])!@[#7;+0;!D1:3]>>*[C:1]=[O:2].*[#7:3]",
            //2 = Ester -> aliphatic C (index 1),
            // connected via a non-ring double bond to aliphatic O (index 2) as a side chain,
            // connected via a non-ring bond to an aliphatic O with a neutral charge (index 3)
            // reacts to C index 1 connected to O index 2 and to any other atom
            // and O index 3 connected to any other atom
            // note that the result is an aldehyde (not a carboxylic acid) and an alcohol
            // note also that the atoms can potentially be in a ring, just not the bonds
            "[C:1](=!@[O:2])!@[O;+0:3]>>*[C:1]=[O:2].[O:3]*",
            //3 = Amine -> aliphatic N with a neutral charge and a degree of NOT 1
            // that is also NOT connected to a C connected via a double bond to N, O, P, or S
            // (to avoid any form of amides) but connected via a non-ring bond to
            // any atom (index 1) as a side chain and via a non-ring bond to
            // any atom (index 2) reacts to the two any atoms with indices 1 and 2,
            // each connected to any other atom
            // note that the amine / N is discarded! //TODO does this also work for tertiary amines? I guess it matches multiple times?
            // note also that the atoms can potentially be in a ring, just not the bonds
            // simpler alternative would be (without excluding any sort of amides):
            // [N;!D1](!@[*:1])!@[*:2]>>*[*:1].[*:2]*
            "[N;!D1;+0;!$(N-C=[#7,#8,#15,#16])](-!@[*:1])-!@[*:2]>>*[*:1].[*:2]*",
            //4 = Urea -> aliphatic or aromatic(!) N with a neutral charge and a degree of 2 or 3 (index 1),
            // connected via a non-ring bond to an aliphatic C, connected via a
            // non-ring double bond to an aliphatic O as a side chain and via a non-ring bond to
            // another aliphatic or aromatic(!) N with a neutral charge and a degree of 2 or 3 (index 2)
            // reacts to two N atoms (indices 1 and 2) that are unconnected but
            // each to connected to any other atom
            // note that the central keto group is discarded! We get two amines
            // as a result of the reaction (one possible way to synthesize a urea functionality)
            // note also that the atoms can potentially be in a ring, just not the bonds
            "[#7;+0;D2,D3:1]!@C(!@=O)!@[#7;+0;D2,D3:2]>>*[#7:1].[#7:2]*",
            //5 = Ether -> aliphatic or aromatic(!) C (index 1) connected via a non-ring bond to an
            // aliphatic O with a neutral charge, connected via a non-ring bond
            // to an aliphatic or aromatic(!) C (index 2) reacts to the two carbon atoms
            // connected to any other atom
            // note that the ether O is discarded, we do not get an alcohol or sth similar as result
            // note also that the atoms can potentially be in a ring, just not the bonds
            "[#6:1]-!@[O;+0]-!@[#6:2]>>[#6:1]*.*[#6:2]",
            //6 = Olefin -> an aliphatic C (index 1) connected via a non-ring double bond
            // to another aliphatic C (index 2) reacts to the two carbon atoms each
            // connected to any atom
            // note that the double bond is simply split, no assumption is made as to how it
            // was synthesized
            // note also that the degree of the carbon atoms is not specified
            // note also that the atoms can potentially be in a ring, just not the bonds
            "[C:1]=!@[C:2]>>[C:1]*.*[C:2]",
            "", //7 = Quaternary nitrogen //TODO what about this? I do not think it is covered by nr 3 (amine)!
            //8 = Aromatic nitrogen - aliphatic carbon -> an aromatic N with a
            // neutral charge (index 1) connected via a non-ring bond to an
            // aliphatic C (index 2) reacts to both atoms connected to any other atom
            // note that no assumption is made as to how the structure was synthesized
            // note also that the atoms can potentially be in a ring (the n must be), just not the bonds
            "[n;+0:1]-!@[C:2]>>[n:1]*.[C:2]*",
            //9 = Lactam nitrogen - aliphatic carbon -> an aliphatic O (index 3)
            // connected via a double bond (ring or non-ring) to an aliphatic C (index 4)
            // connected via a ring bond(!) to an aliphatic N with a neutral charge (index 1)
            // connected via a non-ring bond to an aliphatic C (index 2)
            // reacts to the C index 2 being split from the rest of the structure
            // note that C index 2 could be in different ring
            // note also that no assumption is made as to how the structure was synthesized
            "[O:3]=[C:4]-@[N;+0:1]-!@[C:2]>>[O:3]=[C:4]-[N:1]*.[C:2]*",
            //10 = Aromatic carbon - aromatic carbon -> aromatic C (index 1) connected
            // via a non-ring bond(!) to another aromatic C (index 2) reacts to the
            // bond in between being split
            // note that no assumption is made as to how the structure was synthesized
            "[c:1]-!@[c:2]>>[c:1]*.*[c:2]",
            //11 = Sulphonamide -> an aliphatic or aromatic N with a neutral charge and
            // a degree of 2 or 3 (index 1) connected via a non-ring bond to an
            // alipathic S (index 2) connected to two aliphatic O (indices 3 and 4)
            // via double bonds reacts to the bond between N and S being split
            // note that the atoms could be in rings, just not the bond that is split
            // note that no assumption is made as to how the structure was synthesized
            "[#7;+0;D2,D3:1]-!@[S:2](=[O:3])=[O:4]>>[#7:1]*.*[S:2](=[O:3])=[O:4]",
            // S1 = Cyclic amines (added in RDKit implementation) -> an aliphatic or aromatic N
            // in a ring, with a degree of 3, and a neutral charge (index 1) connected
            // via a non-ring bond to any atom (index 2)
            // reacts to the N connected to any atom and the other atom connected to any atom
            // note that no assumption is made as to how the structure was synthesized
            //TODO this is not part of the original RECAP, make it optional?
            "[#7;R;D3;+0:1]-!@[*:2]>>*[#7:1].[*:2]*",
            // S2 = Aromatic nitrogen - aromatic carbon -> aromatic N with a
            // neutral charge (index 1) connected
            // via a non-ring bond(!) to an aromatic C (index 2) reacts to the
            // bond in between being split
            // note that no assumption is made as to how the structure was synthesized
            // note also that both atoms are in different rings
            //TODO this is not part of the original RECAP, make it optional?
            "[n;+0:1]-!@[c:2]>>[n:1]*.*[c:2]"
    };
    /**
     *
     */
    public RECAP() {

    }
    /**
     * rings must be detected before
     * aromaticity must be detected before
     */
    public IAtomContainer[] fragment(IAtomContainer aMolecule) {
        //TODO: we need this but it is not there in CDK 2.9, will come with 2.10
        //SmirksTransform smirks = Smirks.compile("[C:1][H]>>[C:1]Cl");
        return null;
    }
    /**
     * Encapsulating the state of the algorithm allows thread-safe calling.
     */
    private static final class State {

    }
}
