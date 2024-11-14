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
 *
 * @author Jonas Schaub
 * @version 1.0.0.0
 */
public class RECAP {
    /**
     *
     */
    private static final String[] CLEAVAGE_RULES = {
      "[C;!$(C([#7])[#7]):1](=!@[O:2])!@[#7;+0;!D1:3]>>*[C:1]=[O:2].*[#7:3]", //1 = Amide
            "[C:1](=!@[O:2])!@[O;+0:3]>>*[C:1]=[O:2].[O:3]*", //2 = ester
            "[N;!D1;+0;!$(N-C=[#7,#8,#15,#16])](-!@[*:1])-!@[*:2]>>*[*:1].[*:2]*", //3 = Amine
            "[#7;+0;D2,D3:1]!@C(!@=O)!@[#7;+0;D2,D3:2]>>*[#7:1].[#7:2]*", //4 = Urea
            "[#6:1]-!@[O;+0]-!@[#6:2]>>[#6:1]*.*[#6:2]", //5 = Ether
            "[C:1]=!@[C:2]>>[C:1]*.*[C:2]",  //6 = Olefin
            "", //7 = Quaternary nitrogen
            "[n;+0:1]-!@[C:2]>>[n:1]*.[C:2]*", //8 = Aromatic nitrogen - aliphatic carbon
            "[O:3]=[C:4]-@[N;+0:1]-!@[C:2]>>[O:3]=[C:4]-[N:1]*.[C:2]*", //9 = Lactam nitrogen - aliphatic carbon
            "[c:1]-!@[c:2]>>[c:1]*.*[c:2]", //10 = Aromatic carbon- aromatic carbon
            "[#7;+0;D2,D3:1]-!@[S:2](=[O:3])=[O:4]>>[#7:1]*.*[S:2](=[O:3])=[O:4]", //11 = Sulphonamide
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
        return null;
    }
    /**
     * Encapsulating the state of the algorithm allows thread-safe calling.
     */
    private static final class State {

    }
}
