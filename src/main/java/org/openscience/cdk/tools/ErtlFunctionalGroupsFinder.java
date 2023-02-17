/**
 * ErtlFunctionalGroupsFinder for CDK
 * Copyright (C) 2023 Sebastian Fritsch
 * 
 * Source code is available at <https://github.com/zielesny/ErtlFunctionalGroupsFinder>
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openscience.cdk.tools;

/**
 * IMPORTANT NOTE: This is a copy of
 * https://github.com/zielesny/ErtlFunctionalGroupsFinder/blob/master/Basic/ErtlFunctionalGroupsFinder.java
 * Therefore, do not make any changes here but in the original repository!
 * Last copied on September 26th 2022
 */

import org.openscience.cdk.graph.ConnectedComponents;
import org.openscience.cdk.graph.GraphUtil;
import org.openscience.cdk.graph.GraphUtil.EdgeToBondMap;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IBond.Order;
import org.openscience.cdk.interfaces.ILonePair;
import org.openscience.cdk.interfaces.IPseudoAtom;
import org.openscience.cdk.interfaces.ISingleElectron;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Finds and extracts a molecules's functional groups in a purely rule-based manner.
 *
 * This class implements Peter Ertl's algorithm for the automated detection and extraction
 * of functional groups in organic molecules
 * [Ertl P. An algorithm to identify functional groups in organic molecules. J Cheminform. 2017; 9:36.].
 *
 * @author Sebastian Fritsch, Jonas Schaub
 * @version 1.0.0.1
 */
public class ErtlFunctionalGroupsFinder {
	
	private static ILoggingTool log = LoggingToolFactory.createLoggingTool(ErtlFunctionalGroupsFinder.class);
	private final static String CARBONYL_C_MARKER = "Carbonyl-C"; 
	private final Set<Integer> nonmetalAtomicNumbers;
    private final Mode 		mode;
    private EdgeToBondMap 	bondMap;
    private int[][] 		adjList;
    private HashSet<Integer>			markedAtoms;
    private HashMap<Integer, Boolean>	aromaticHeteroAtoms; // key: atom idx, value: isInGroup
    private Map<IAtom, List<EnvironmentalC>> environmentsMap;

	/**
	 * Defines the working mode.
	 */
	public static enum Mode{
		/**
		 * Default mode including the generalization step.
		 */
		DEFAULT,
		/**
		 * Skips the generalization step. Functional groups will keep their full "environment".
		 */
		NO_GENERALIZATION;
	}
	
	private enum EnvironmentCalCType { C_AROMATIC, C_ALIPHATIC };
	
	/**
	 * Describes one carbon atom in the environment of a marked atom. It can either be aromatic 
	 * or aliphatic and also contains a clone of its connecting bond.
	 */
	private class EnvironmentalC{
		private EnvironmentCalCType type;
		private int bondIndex;
		private IBond.Order bondOrder;
		private IBond.Stereo bondStereo;
		private boolean[] bondFlags;
		
		public EnvironmentalC(EnvironmentCalCType type, IBond bond, int indexInBond) {
			this.type = type;
			
			bondIndex = indexInBond;
			bondOrder = bond.getOrder();
			bondStereo = bond.getStereo();
			bondFlags = bond.getFlags();
		}
		
		public EnvironmentCalCType getType() {
			return type;
		}
		
		public IBond createBond(IAtom targetAtom, IAtom cAtom) {
			IBond bond = targetAtom.getBuilder().newInstance(IBond.class);
    		if(bondIndex == 0) {
    			bond.setAtoms(new IAtom[] {cAtom, targetAtom});
    		}
    		else {
    			bond.setAtoms(new IAtom[] {targetAtom, cAtom});
    		}
    		bond.setOrder(bondOrder);
    		bond.setStereo(bondStereo);
    		bond.setFlags(bondFlags);
    		
    		return bond;
		}
	}
    
    /**
     * Default constructor for ErtlFunctionalGroupsFinder.
     */
    public ErtlFunctionalGroupsFinder() {
    	this(Mode.DEFAULT);
    }
    
    /**
     * Constructor for ErtlFunctionalGroupsFinder.
     * 
     * @param mode working mode (see {@code ErtlFunctionalGroupsFinder.Mode}).
     */
    public ErtlFunctionalGroupsFinder(Mode mode) {
    	this.mode = mode;
    	
    	// init non-metal and non-metalloid atom numbers
		nonmetalAtomicNumbers = Set.of(1, 2, 6, 7, 8, 9, 10, 15, 16, 17, 18, 34, 35, 36, 53, 54, 86); //ImmutableSet.of(1, 2, 6, 7, 8, 9, 10, 15, 16, 17, 18, 34, 35, 36, 53, 54, 86);
    }

	/**
	 * Find all functional groups contained in a molecule.
	 *
	 * NOTE: The input must consist of one connected structure and may not contain charged atoms, metals or metalloids.
	 *
	 * @param container the molecule which contains the functional groups (may not contain charged atoms, metals,
	 *                  metalloids or unconnected components!)
	 * @return a list with all functional groups found in the molecule.
	 */
	public List<IAtomContainer> find(IAtomContainer container){
		return find(container, true);
	}
    
    /**
     * Find all functional groups contained in a molecule.
	 *
	 * NOTE: The input must consist of one connected structure and may not contain charged atoms, metals or metalloids.
     * 
     * @param container the molecule which contains the functional groups (may not contain charged atoms, metals,
	 *                  metalloids or unconnected components!)
	 * @param clone Use 'false' to reuse the input container's bonds and atoms in the extraction of the functional
	 *                groups. This may speed up the extraction and lower the memory consumption for processing large
	 *                amounts of data but corrupts the original input container.
	 *              Use 'true' to work with a clone and leave the input container intact (default).
     * @return a list with all functional groups found in the molecule.
     */
    public List<IAtomContainer> find(IAtomContainer container, boolean clone){
    	// work with a clone?
		IAtomContainer mol;
		if(clone){
			try {
				mol = container.clone();
			} catch (CloneNotSupportedException e) {
				throw new IllegalStateException("Atom container could not be cloned");
			}
		}
		else{
			mol = container;
		}
    	
    	// init GraphUtil & EdgeToBondMap
    	bondMap = EdgeToBondMap.withSpaceFor(mol);
    	adjList = GraphUtil.toAdjList(mol, bondMap);

		checkConstraints(mol);

		// atom marking
    	markAtoms(mol);

    	// extract raw groups
    	List<IAtomContainer> groups = extractGroups(mol);

		// handle environment
    	if(mode == Mode.DEFAULT) {
			expandGeneralizedEnvironments(groups);
    	}
    	else if (mode == Mode.NO_GENERALIZATION) {
			expandFullEnvironments(groups);
    	}
    	else {
    		throw new IllegalStateException("Unknown mode.");
    	}
    	
    	// clear fields
		bondMap = null;
		adjList = null;
		markedAtoms = null;
		aromaticHeteroAtoms = null;
		environmentsMap = null;
    		
    	return groups;
    }

    /**
     * Mark all atoms and store them in a set for further processing.
     *
     * @param molecule Molecule with atoms to mark
     */
    private void markAtoms(IAtomContainer molecule) {
    	if(isDbg()) log.debug("########## Starting search for atoms to mark ... ##########");
    	
    	// store marked atoms
		markedAtoms = new HashSet<Integer>(molecule.getAtomCount()); //Sets.newHashSetWithExpectedSize(molecule.getAtomCount());
    	// store aromatic heteroatoms
    	aromaticHeteroAtoms = new HashMap<>();
    	
    	for(int idx = 0; idx < molecule.getAtomCount(); idx++) {
    		// skip atoms that already got marked in a previous iteration
    		if(markedAtoms.contains(idx)) {
    			continue;
    		}
    		IAtom cAtom = molecule.getAtom(idx);
    		// skip aromatic atoms but add them to set
    		if(cAtom.isAromatic()) {
    			if(isHeteroatom(cAtom)) {
    				aromaticHeteroAtoms.put(idx, false);
    			}
    			continue;
    		}
    		
    		int atomicNr = cAtom.getAtomicNumber();
    		
    		// if C...
    		if(atomicNr == 6) {
    			boolean isMarked = false;		// to detect if foor loop ran with or without marking the C atom
    			int oNSCounter = 0;				// count for the number of connected O, N & S atoms
    			for(int connectedIdx : adjList[idx]) {
    				IAtom connectedAtom = molecule.getAtom(connectedIdx); 
    				IBond connectedBond = bondMap.get(idx, connectedIdx);
    				
    				// if connected to Heteroatom or C in aliphatic double or triple bond... [CONDITIONS 2.1 & 2.2]
    				if(connectedAtom.getAtomicNumber() != 1 && ((connectedBond.getOrder() == Order.DOUBLE 
    						|| connectedBond.getOrder() == Order.TRIPLE) && !connectedBond.isAromatic())) {
    						
    					// set the connected atom as marked
    					if(markedAtoms.add(connectedIdx)) {
    						String connectedAtomCondition = connectedAtom.getAtomicNumber() == 6 ? "2.1/2.2" : "1";
    						if(isDbg()) log.debug(String.format("Marking Atom #%d (%s) - Met condition %s",
									connectedIdx, connectedAtom.getSymbol(), connectedAtomCondition));
    					}
    					
    					// set the current atom as marked and break out of connected atoms
    					if(isDbg()) log.debug(String.format("Marking Atom #%d (%s) - Met condition 2.1/2.2",
								idx, cAtom.getSymbol()));
    					isMarked = true;
    					
    					// but check for carbonyl-C before break
    					if(connectedAtom.getAtomicNumber() == 8 && connectedBond.getOrder() == Order.DOUBLE
								&& adjList[idx].length == 3) {
    						if(isDbg()) log.debug("                     - was flagged as Carbonly-C");
    						cAtom.setProperty(CARBONYL_C_MARKER, true);
    					}
    					
    					break;
    				}
    				// if connected to O/N/S in single bond...
    				else if((connectedAtom.getAtomicNumber() == 7 
    						|| connectedAtom.getAtomicNumber() == 8
    						|| connectedAtom.getAtomicNumber() == 16)
    						&& connectedBond.getOrder() == Order.SINGLE){
    					// if connected O/N/S is not aromatic...
    					if(!connectedAtom.isAromatic()) {
    						// set the connected O/N/S atom as marked
    						if(isDbg()) log.debug(String.format("Marking Atom #%d (%s) - Met condition 1",
									connectedIdx, connectedAtom.getSymbol()));
    						markedAtoms.add(connectedIdx);
    						
    						// if "acetal C" (2+ O/N/S in single bonds connected to sp3-C)... [CONDITION 2.3]
    						boolean isAllSingleBonds = true;
    						for(int connectedInSphere2Idx : adjList[connectedIdx]) {
    							IBond sphere2Bond = bondMap.get(connectedIdx, connectedInSphere2Idx);
    							if(sphere2Bond.getOrder() != Order.SINGLE) {
    								isAllSingleBonds = false;
    								break;
    							}
    						}
    						if(isAllSingleBonds) {
    							oNSCounter++;
    							if(oNSCounter > 1 && adjList[idx].length + cAtom.getImplicitHydrogenCount() == 4) {
    								// set as marked and break out of connected atoms
    								if(isDbg()) log.debug(String.format("Marking Atom #%d (%s) - Met condition 2.3",
											idx, cAtom.getSymbol()));
    								isMarked = true;
    								break;
    							}
    						}
    					}
    					// if part of oxirane, aziridine and thiirane ring... [CONDITION 2.4]
    					for(int connectedInSphere2Idx : adjList[connectedIdx]) {
    						IAtom connectedInSphere2Atom = molecule.getAtom(connectedInSphere2Idx);
    						if(connectedInSphere2Atom.getAtomicNumber() == 6) {
    							for(int connectedInSphere3Idx : adjList[connectedInSphere2Idx]) {
    								IAtom connectedInSphere3Atom = molecule.getAtom(connectedInSphere3Idx);
    								if(connectedInSphere3Atom.equals(cAtom)) {
    									// set connected atoms as marked
    									if(isDbg()) log.debug(String.format("Marking Atom #%d (%s) - Met condition 2.4",
												connectedInSphere2Idx, connectedInSphere2Atom.getSymbol()));
    									if(isDbg()) log.debug(String.format("Marking Atom #%d (%s) - Met condition 2.4",
												connectedInSphere3Idx, connectedInSphere3Atom.getSymbol()));
    									markedAtoms.add(connectedInSphere2Idx);
    									markedAtoms.add(connectedInSphere3Idx);
    									// set current atom as marked and break out of connected atoms
    									if(isDbg()) log.debug(String.format("Marking Atom #%d (%s) - Met condition 2.4",
												idx, cAtom.getSymbol()));
    									isMarked = true; 
    									break;
    								}
    							}
    						}
    					}
    				}
    			}
    			if(isMarked) {
    				markedAtoms.add(idx);
    				continue;
    			}
    			// if none of the conditions 2.X apply, we have an unmarked C (not relevant here)
    		}
    		// if H...
    		else if (atomicNr == 1){
    			// convert to implicit H
    			IAtom connectedAtom;
    			try {
    				connectedAtom = molecule.getAtom(adjList[idx][0]);
    			}
    			catch(ArrayIndexOutOfBoundsException e) {
    				break;
    			}
    			
    			
    			if(connectedAtom.getImplicitHydrogenCount() == null) {
    				connectedAtom.setImplicitHydrogenCount(1);
    			}
    			else {
    				connectedAtom.setImplicitHydrogenCount(connectedAtom.getImplicitHydrogenCount() + 1);
    			}
    			continue;
    		}
    		// if heteroatom... (CONDITION 1)
    		else {
    			if(isDbg()) log.debug(String.format("Marking Atom #%d (%s) - Met condition 1", idx, cAtom.getSymbol())); 
    			markedAtoms.add(idx);
    			continue;
    		}
    	}
    	if(isDbg()) log.debug(String.format("########## End of search. Marked %d/%d atoms. ##########", markedAtoms.size(), molecule.getAtomCount()));
    }

    /**
     * Searches the molecule for groups of connected marked atoms and extracts each as a new functional group.
     * The extraction process includes marked atom's "environments". Connected H's are captured implicitly.
     * 
     * @param molecule the molecule which contains the functional groups
     * @return a list of all functional groups (including "environments") extracted from the molecule
     */
    private List<IAtomContainer> extractGroups(IAtomContainer molecule) {
    	if(isDbg()) log.debug("########## Starting identification & extraction of functional groups... ##########");

		environmentsMap = new HashMap<IAtom, List<org.openscience.cdk.tools.ErtlFunctionalGroupsFinder.EnvironmentalC>>(molecule.getAtomCount());//Maps.newHashMapWithExpectedSize(molecule.getAtomCount());
		int[] atomIdxToFGMap = new int[molecule.getAtomCount()];
		Arrays.fill(atomIdxToFGMap, -1);
    	int fGroupIdx = -1;

    	while(!markedAtoms.isEmpty()) {
    		// search for another functional group
    		fGroupIdx++;
    		
    		// get next markedAtom as the starting node for the search 
    		int beginIdx = markedAtoms.iterator().next();
    		if(isDbg()) log.debug(String.format("Searching new functional group from atom #%d (%s)...", beginIdx,  molecule.getAtom(beginIdx).getSymbol()));
    		
    		// do a BFS from there
    		Queue<Integer> queue = new ArrayDeque<>();
    		queue.add(beginIdx);
    		
    		while(!queue.isEmpty()) {
    			int currentIdx = queue.poll();
    			
    			// we are only interested in marked atoms that are not yet included in a group
    			if(!markedAtoms.contains(currentIdx)){
    				continue;
    			}
    			
    			// if it isn't...
    			IAtom currentAtom = molecule.getAtom(currentIdx);
    			if(isDbg()) log.debug(String.format("	visiting marked atom: #%d (%s)", currentIdx, currentAtom.getSymbol()));
    			
    			// add its index to the functional group
				atomIdxToFGMap[currentIdx] = fGroupIdx;
				// also scratch the index from markedAtoms
				markedAtoms.remove(currentIdx);
				
				// and take look at the connected atoms
				List<EnvironmentalC> currentEnvironment = new ArrayList<>();
				for(int connectedIdx : adjList[currentIdx]) {
					// add connected marked atoms to queue
					if(markedAtoms.contains(connectedIdx)) {
						queue.add(connectedIdx);
						continue;
					}

					// ignore already handled connected atoms
					if(atomIdxToFGMap[connectedIdx] >= 0){
						continue;
					}
					
					// add unmarked connected aromatic heteroatoms
					IAtom connectedAtom = molecule.getAtom(connectedIdx);
					if(isHeteroatom(connectedAtom) && connectedAtom.isAromatic()) {
						if(isDbg()) log.debug("	   added connected aromatic heteroatom " + connectedAtom.getSymbol());
						atomIdxToFGMap[connectedIdx] = fGroupIdx;
	    				// note that this aromatic heteroatom has been added to a group
	    				aromaticHeteroAtoms.put(connectedIdx, true);
					}
					
					// add unmarked connected atoms to current marked atom's environment
					IBond connectedBond = bondMap.get(currentIdx, connectedIdx);
					
					EnvironmentCalCType type;
					if (connectedAtom.getAtomicNumber() == 6) {
						if(connectedAtom.isAromatic())
							type = EnvironmentCalCType.C_AROMATIC;
						else
							type = EnvironmentCalCType.C_ALIPHATIC;
					}
					else {
						// aromatic heteroatom, so just ignore
						continue;
					}
					currentEnvironment.add(new EnvironmentalC(type, connectedBond, connectedBond.getBegin() == connectedAtom ? 0 : 1));
				}
				environmentsMap.put(currentAtom, currentEnvironment);

				// debug logging
				if(isDbg()) {
					int cAromCount = 0, cAliphCount = 0;
					for(EnvironmentalC comp : currentEnvironment) {
						if(comp.getType() == EnvironmentCalCType.C_AROMATIC)
							cAromCount++;
						else if(comp.getType() == EnvironmentCalCType.C_ALIPHATIC)
							cAliphCount++;
					}
					log.debug(String.format("	   logged marked atom's environment: C_ar:%d, C_al:%d (and %d implicit hydrogens)", cAromCount, cAliphCount, currentAtom.getImplicitHydrogenCount()));
				}
    		}
    		
    		if(isDbg()) log.debug("	search completed.");
    	}
    	
    	// also create FG for lone aromatic heteroatoms, not connected to a FG yet.
    	for(int atomIdx : aromaticHeteroAtoms.keySet()) {
    		if(!aromaticHeteroAtoms.get(atomIdx)) {
    			fGroupIdx++;
				atomIdxToFGMap[atomIdx] = fGroupIdx;
				if(isDbg()) log.debug("Created FG for lone aromatic heteroatom: " + molecule.getAtom(atomIdx).getSymbol());
    		}
    	}

		List<IAtomContainer> fGs = partitionIntoGroups(molecule, atomIdxToFGMap, fGroupIdx + 1);

		if(isDbg()) log.debug(String.format("########## Found & extracted %d functional groups. ##########", fGroupIdx + 1));
    	return fGs;
    }

    /**
     * Generalizes the full environments of functional groups, providing a good balance between preserving 
     * meaningful detail and generalization.
     * 
     * @param fGroups the list of functional groups including "environments"
     */
    private void expandGeneralizedEnvironments(List<IAtomContainer> fGroups){
    	if(isDbg()) log.debug("########## Starting generalization of functional groups... ##########");
    	
    	for(IAtomContainer fGroup : fGroups) {
    		int atomCount = fGroup.getAtomCount();
    	
    		if(isDbg()) log.debug(String.format("Generalizing functional group (%d atoms)...", atomCount));
    		
    		// prechecking for special cases...
    		if(fGroup.getAtomCount() == 1) {
    			IAtom atom = fGroup.getAtom(0);
    			List<EnvironmentalC> environment = environmentsMap.get(atom);

    			if(environment != null) {
    				int envCCount = environment.size();
    				
    				// for H2N-C_env & HO-C_env -> do not replace H & C_env by R!
    				if((atom.getAtomicNumber() == 8 && envCCount == 1)
    					|| (atom.getAtomicNumber() == 7 && envCCount == 1)){
    						if(isDbg()) log.debug(String.format("   - found single atomic N or O FG with one env. C. Expanding environment...", atom.getSymbol()));
    						expandEnvironment(atom, fGroup);

    						int hCount = atom.getImplicitHydrogenCount();
    						if(hCount != 0) {
    							if(isDbg()) log.debug(String.format("   - adding %d hydrogens...", hCount));
    							addHydrogens(atom, hCount, fGroup);
    							atom.setImplicitHydrogenCount(0);
    						}
    						continue;
    				}
    				// for HN-(C_env)-C_env & HS-C_env -> do not replace H by R! (only C_env!)
    				if((atom.getAtomicNumber() == 7 && envCCount == 2)
    					|| (atom.getAtomicNumber() == 16 && envCCount == 1)) {
    					if(isDbg()) log.debug("   - found sec. amine or simple thiol");
    					int hCount = atom.getImplicitHydrogenCount();
						if(hCount != 0) {
							if(isDbg()) log.debug(String.format("   - adding %d hydrogens...", hCount));
							addHydrogens(atom, hCount, fGroup);
							atom.setImplicitHydrogenCount(0);
						}
						if(isDbg()) log.debug("   - expanding environment...");
						expandEnvironmentGeneralized(atom, fGroup);
						continue;
    				}
    			}
    			else if(isHeteroatom(atom)) {
    				int rAtomCount = atom.getValency();
    				Integer hCount = atom.getImplicitHydrogenCount();
    				if(hCount != null && hCount != 0) {
    					atom.setImplicitHydrogenCount(0);
    				}
    				String atomTypeName = atom.getAtomTypeName();
    				if(isDbg()) log.debug(String.format("   - found single aromatic heteroatom (%s, Atomtype %s). Adding %d R-Atoms...", atom.getSymbol(), atomTypeName, rAtomCount));
    				addRAtoms(atom, rAtomCount, fGroup);
    				continue;
    			}
    		}
    			
    		// get atoms to process
			List<IAtom> fGroupAtoms = new ArrayList<IAtom>(fGroup.getAtomCount());//Lists.newArrayList(fGroup.atoms());
			fGroup.atoms().forEach(fGroupAtoms::add);
    		
    		// process atoms...
    		for(IAtom atom : fGroupAtoms) {
    			List<EnvironmentalC> environment = environmentsMap.get(atom);
    			
    			if(environment == null) {
					if(atom.getImplicitHydrogenCount() != 0) {
						atom.setImplicitHydrogenCount(0);
					}
					int rAtomCount = atom.getValency() - 1;
					if(isDbg()) log.debug(String.format("   - found connected aromatic heteroatom (%s). Adding %d R-Atoms...", atom.getSymbol(), rAtomCount));
					addRAtoms(atom, rAtomCount, fGroup);
    			}
    			
    			// processing carbons...
    			if(atom.getAtomicNumber() == 6) {  				
    				if(atom.getProperty(CARBONYL_C_MARKER) == null) {
    					if(atom.getImplicitHydrogenCount() != 0) {
    						atom.setImplicitHydrogenCount(0);
    					}
    					if(isDbg()) log.debug("   - ignoring environment for marked carbon atom");
    					continue;
    				}
    				else {
    					if(isDbg()) log.debug("   - found carbonyl-carbon. Expanding environment...");
    					expandEnvironmentGeneralized(atom, fGroup);
    					continue;
    				}
    			}
    			// processing heteroatoms...
    			else {
    				if(isDbg()) log.debug(String.format("   - found heteroatom (%s). Expanding environment...", atom.getSymbol()));
    				expandEnvironmentGeneralized(atom, fGroup);
    				continue;
    			}
    		}
    	}
    	
    	if(isDbg()) log.debug("########## Generalization of functional groups completed. ##########");
    }

	/**
	 * Expands the full environments of functional groups, converted into atoms and bonds.
	 *
	 * @param fGroups the list of functional groups including "environments"
	 */
	private void expandFullEnvironments(List<IAtomContainer> fGroups) {
    	if(isDbg()) log.debug("########## Starting expansion of full environments for functional groups... ##########");
    	
    	for(IAtomContainer fGroup : fGroups) {
    		int atomCount = fGroup.getAtomCount();
    		if(isDbg()) log.debug(String.format("Expanding environment on functional group (%d atoms)...", atomCount));
    		
    		for(int i = 0; i < atomCount; i++) {
    			IAtom atom = fGroup.getAtom(i);
    			
    			if(isDbg()) log.debug(String.format(" - Atom #%d:%   - Expanding environment...", i));
    			expandEnvironment(atom, fGroup);
    			
    			int hCount = atom.getImplicitHydrogenCount();
				if(hCount != 0) {
					if(isDbg()) log.debug(String.format("   - adding %d hydrogens...", hCount));
					addHydrogens(atom, hCount, fGroup);
					atom.setImplicitHydrogenCount(0);
				}
    		}
    	}
    	
    	if(isDbg()) log.debug("########## Expansion of full environments for functional groups completed. ##########");
    }
    
    private void expandEnvironment(IAtom atom, IAtomContainer container) {
    	List<EnvironmentalC> environment = environmentsMap.get(atom);
    	
    	if(environment == null || environment.isEmpty()) {
    		if(isDbg()) log.debug("		found no environment to expand.");
    		return;
    	}
    		
    	int cAromCount = 0, cAliphCount = 0;
    	for(EnvironmentalC envC : environment) {
    		IAtom cAtom = atom.getBuilder().newInstance(IAtom.class, "C");
            cAtom.setAtomTypeName("C");
            cAtom.setImplicitHydrogenCount(0);
    		if(envC.getType() == EnvironmentCalCType.C_AROMATIC) {
    			cAtom.setIsAromatic(true);
    			cAromCount++;
    		}
    		else {
    			cAliphCount++;
    		}
    		
    		IBond bond = envC.createBond(atom, cAtom);
    		
    		container.addAtom(cAtom);
    		container.addBond(bond);
    	}
    	
    	if(isDbg()) log.debug(String.format("		expanded environment: %dx C_ar and %dx C_al", cAromCount, cAliphCount));
    }
    
    // only call this on marked heteroatoms / carbonyl-C's!
    private void expandEnvironmentGeneralized(IAtom atom, IAtomContainer container) {
    	
    	List<EnvironmentalC> environment = environmentsMap.get(atom);
    	
    	if(environment == null) {
    		if(isDbg()) log.debug("		found no environment to expand.");
    		return;
    	}
    	
    	int rAtomCount = environment.size();
    	int rAtomsForCCount = rAtomCount;
    	if(atom.getAtomicNumber() == 8 && atom.getImplicitHydrogenCount() == 1) {
    		addHydrogens(atom, 1, container);
			atom.setImplicitHydrogenCount(0);
			if(isDbg()) log.debug("		expanded hydrogen on connected OH-Group");
    	}
    	else if(isHeteroatom(atom)) rAtomCount += atom.getImplicitHydrogenCount();
    	addRAtoms(atom, rAtomCount, container);
   
    	if(atom.getImplicitHydrogenCount() != 0) {
    		atom.setImplicitHydrogenCount(0);
    	}
    	
    	if(isDbg()) log.debug(String.format("		expanded environment: %dx R-atom (incl. %d for H replacement)", rAtomCount, rAtomCount - rAtomsForCCount));
    }
    
    private static final boolean isHeteroatom(IAtom atom) {
    	int atomicNr = atom.getAtomicNumber();
    	return atomicNr != 1 && atomicNr != 6;
    }
    
    private final boolean  isNonmetal(IAtom atom) {
    	return nonmetalAtomicNumbers.contains(atom.getAtomicNumber());
    }
    
    private void addHydrogens(IAtom atom, int number, IAtomContainer container) {
    	for(int i = 0; i < number; i++) {
    		IAtom hydrogen = atom.getBuilder().newInstance(IAtom.class, "H");
            hydrogen.setAtomTypeName("H");
            hydrogen.setImplicitHydrogenCount(0);
            
            container.addAtom(hydrogen);
            container.addBond(atom.getBuilder().newInstance(IBond.class, atom, hydrogen, Order.SINGLE));
    	}
    }
    
    private void addRAtoms(IAtom atom, int number, IAtomContainer container) {
    	for(int i = 0; i < number; i++) {
    		IPseudoAtom rAtom = atom.getBuilder().newInstance(IPseudoAtom.class, "R");
    		rAtom.setAttachPointNum(1);
    		rAtom.setImplicitHydrogenCount(0);
    		
    		container.addAtom(rAtom);
            container.addBond(atom.getBuilder().newInstance(IBond.class, atom, rAtom, Order.SINGLE));   	
    	}
    }
    
    private List<IAtomContainer> partitionIntoGroups(IAtomContainer sourceContainer, int[] atomIdxToFGMap, int fGroupCount) {
    	List<IAtomContainer> groups = new ArrayList<>(fGroupCount);
    	for(int i = 0; i < fGroupCount; i++) {
    		groups.add(sourceContainer.getBuilder().newInstance(IAtomContainer.class));
    	}

		Map<IAtom, IAtomContainer> atomtoFGMap = new HashMap<IAtom, IAtomContainer>(sourceContainer.getAtomCount());//Maps.newHashMapWithExpectedSize(sourceContainer.getAtomCount());
    	
    	// atoms
    	for(int atomIdx = 0; atomIdx < sourceContainer.getAtomCount(); atomIdx++) {
    		int fGroupId = atomIdxToFGMap[atomIdx];
    		
    		if(fGroupId == -1) {
    			continue;
    		}
    		
    		IAtom atom = sourceContainer.getAtom(atomIdx);
    		IAtomContainer myGroup = groups.get(fGroupId);
    		myGroup.addAtom(atom);
    		atomtoFGMap.put(atom, myGroup);
    	}
    	
    	// bonds
    	for(IBond bond : sourceContainer.bonds()) {
    		IAtomContainer beginGroup = atomtoFGMap.get(bond.getBegin());
    		IAtomContainer endGroup = atomtoFGMap.get(bond.getEnd());
    		
    		if(beginGroup == null || endGroup == null || beginGroup != endGroup)
    			continue;
    		
    		beginGroup.addBond(bond);
    	}

    	// single electrons
    	for (ISingleElectron electron : sourceContainer.singleElectrons()) {
    		IAtomContainer group = atomtoFGMap.get(electron.getAtom());
    		if(group != null)
    			group.addSingleElectron(electron);
    	}

    	// lone pairs
        for (ILonePair lonePair : sourceContainer.lonePairs()) {
    		IAtomContainer group = atomtoFGMap.get(lonePair.getAtom());
    		if(group != null)
    			group.addLonePair(lonePair);
        }
    	
    	return groups;
    }
    
    private boolean isDbg() {
    	return log.isDebugEnabled();
    }
    
    private boolean checkConstraints(IAtomContainer molecule) {
    	for(IAtom atom : molecule.atoms()) {
    		if(atom.getFormalCharge() != null && atom.getFormalCharge() != 0) {
    			throw new IllegalArgumentException("Input molecule must not contain any charges.");
    		}
    		if(!isNonmetal(atom)) {
				throw new IllegalArgumentException("Input molecule must not contain metals or metalloids.");
			}
    		if(atom.getImplicitHydrogenCount() == null) {
    			atom.setImplicitHydrogenCount(0);
    		}
    	}
    	
    	ConnectedComponents cc = new ConnectedComponents(adjList);
    	if(cc.nComponents() != 1) {
    		throw new IllegalArgumentException("Input molecule must consist of only a single connected stucture.");
    	}
    	
    	return true;
    }
}