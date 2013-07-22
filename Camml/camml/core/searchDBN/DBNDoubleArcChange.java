package camml.core.searchDBN;

import java.util.Random;

import camml.core.search.CaseInfo;
import camml.core.search.TOM;
import camml.core.search.TOMTransformation;

/**Attempts to toggle two arcs A->C and B->C where A and B may be in either first OR second time slices in DTOM.
 * 
 * @author Alex Black
 *
 */
public class DBNDoubleArcChange extends TOMTransformation {

	/** cost to add TEMPORAL arc = -log(arcProbTemporal) - -log(1.0-arcProbTemporal)*/
	//double arcCostTemporal;
	
	/** pre-allocated return value for getNodesChanged() */
    private int[] changes = new int[1];
	
    public DBNDoubleArcChange(Random rand, double arcProb, double arcProbTemporal, CaseInfo caseInfo,
			double temperature) {
		super(rand, caseInfo, temperature);
		//arcCostTemporal = Math.log(arcProbTemporal / (1.0 - arcProbTemporal));
	}
	
	public int[] getNodesChanged() { return changes; }

	public boolean transform(TOM tom, double ljp) {
		if( !(tom instanceof DTOM) ) throw new RuntimeException("Expected DTOM; passed TOM?");
		DTOM dtom = (DTOM)tom;
		
		int numNodes = tom.getNumNodes();
		
		//Pick a node C at random
		int varC = (int)( numNodes * rand.nextDouble() );
		int varCPosition = tom.getNodePos(varC);
		DNode nodeC = (DNode)tom.getNode(varC);
		
		changes[0] = varC;
		
		//Pick two other variables: (other variables may be from first or second time slice)
			//Number of possible temporal arcs A_0 -> C_1 is numNodes
			//Number of possible intraslice arcs A_1 -> C_1 is totalOrder(C) (i.e. first in total order has 0 arcs, 2nd has 1 arc, 3rd has 2 arcs etc) 
		
		int numArcs = numNodes + varCPosition;
		
		int A = (int)( numArcs * rand.nextDouble() );
		int B = (int)( numArcs * rand.nextDouble() );
		while( B == A ) B = (int)( numArcs * rand.nextDouble() );
		
		/*Three cases:
			1. Both parents are in first time slice (i.e. temporal arcs)
			2. One intraslice arc, one temporal arc
			3. Two intraslice arcs
		*/
		
		//Case 1: Two temporal arcs
		if( A < numNodes && B < numNodes ){
			
			int varA = A;
			int varB = B;
			
			//How many arcs are we adding/removing? Will this result in too many parents for node C?
			int change = 0;
			if( dtom.isTemporalArc(varA, varC) ) change--;	//removing A->C
			else change++;									//adding A->C
			if( dtom.isTemporalArc(varB, varC) ) change--;	//removing B->C
			else change++;									//adding B->C
			
			if( nodeC.getNumParents() + change > dtom.getMaxNumParents() ) return false;	//Too many parents already
			
			
			//Toggle A_0 -> C_1 and B_0 -> C_1
			int[] parents = new int[]{ varA, varB };
	        int[] children = new int[]{ varC, varC };
	        
	        double toggleCost = ((DTOMCoster)caseInfo.tomCoster).costToToggleTemporalArcs(dtom, parents, children);
	        
	        //Calculate old cost for node (before making changes)
	        double oldChildCost = caseInfo.nodeCache.getMMLCost( nodeC );
	        
	        //Toggle existence of arcs in DTOM:
	        doubleTemporalMutate( dtom, varA, varB, varC );
	        
	        //New cost for node (after making changes)
	        double newChildCost = caseInfo.nodeCache.getMMLCost( nodeC );
	        
	        oldCost = 0;
	        cost = newChildCost - oldChildCost + toggleCost;	//i.e. difference in node cost + structure cost difference
			
	        if( accept() ){
	        	//Arc probability tracking for DBNs
	        	if( caseInfo.updateArcWeights ){
	        		if( dtom.isTemporalArc(varA, varC) ){
	        			caseInfo.arcWeightsDBN[varC][varA] -= caseInfo.totalWeight;
	        		} else {
	        			caseInfo.arcWeightsDBN[varC][varA] += caseInfo.totalWeight;
	        		}
	        		
	        		if( dtom.isTemporalArc(varB, varC) ){
	        			caseInfo.arcWeightsDBN[varC][varB] -= caseInfo.totalWeight;
	        		} else {
	        			caseInfo.arcWeightsDBN[varC][varB] += caseInfo.totalWeight;
	        		}
	        	}
	        	
	        	return true;
	        } else{
	        	//Undo change:
	        	doubleTemporalMutate( dtom, varA, varB, varC );
	        	return false;
	        }
			
			
		//Case 2: One intraslice arc, one temporal arc
		} else if( (A < numNodes && B >= numNodes) || (A >= numNodes && B < numNodes) ){
			int varTemporal;
			int varIntraslice;
			if( (A < numNodes && B >= numNodes) ){	//A->C is temporal arc to toggle, B->C is intraslice arc to toggle
				varTemporal = A;
				varIntraslice = tom.nodeAt( B - numNodes );
			} else {								//B->C is temporal arc to toggle, A->C is intraslice arc to toggle
				varTemporal = B;
				varIntraslice = tom.nodeAt( A - numNodes );
			}
			
			int change = 0;
			if( dtom.isArc(varIntraslice, varC) ) change--;	//removing A->C
			else change++;									//adding A->C
			if( dtom.isTemporalArc(varTemporal, varC) ) change--;	//removing B->C
			else change++;									//adding B->C
			
			if( nodeC.getNumParents() + change >= dtom.getMaxNumParents() ) return false;	//Too many parents after making change
			
			//Calculate old cost for node (before making changes)
	        double oldChildCost = caseInfo.nodeCache.getMMLCost( nodeC );
	        
	        //Calculate structure cost to toggle arcs
	        double toggleCost = ((DTOMCoster)caseInfo.tomCoster).costToToggleTemporalArc(dtom, varTemporal, varC);	//Structure cost change from toggling temporal arc 
    		toggleCost += ((DTOMCoster)caseInfo.tomCoster).costToToggleArc(dtom, varIntraslice, varC);				//Structure cost change from toggling intraslice arc
	        
	        //Make changes:
	        crossArcMutate( dtom, varTemporal, varIntraslice, varC );
	        
	        //Calculate new cost for node (after changes)
	        double newChildCost = caseInfo.nodeCache.getMMLCost( nodeC );
	        
	        oldCost = 0;
	        cost = newChildCost - oldChildCost + toggleCost;	//i.e. difference in node cost + structure cost difference
	        
	        if( accept() ){
	        	//Tracking of arc probabilities:
	        	if( caseInfo.updateArcWeights ){
	        		
		        	//Arc weight tracking for INTRASLICE arc:
		        	if(tom.isArc(varIntraslice,varC)) {
	                    caseInfo.arcWeights[varC][varIntraslice] -= caseInfo.totalWeight;                
	                }                
	                else {
	                    caseInfo.arcWeights[varC][varIntraslice] += caseInfo.totalWeight;
	                }
		        	
		        	//Arc weight tracking for TEMPORAL arc:
		        	if( dtom.isTemporalArc(varTemporal, varC) ){
		        		caseInfo.arcWeightsDBN[varC][varTemporal] -= caseInfo.totalWeight;
		        	} else {
		        		caseInfo.arcWeightsDBN[varC][varTemporal] += caseInfo.totalWeight;
		        	}
	        	
	        	}
	        	
	        	return true;
	        } else{
	        	//Undo change:
	        	crossArcMutate( dtom, varTemporal, varIntraslice, varC );
	        	return false;
	        }
			
		//Case 3: Two intraslice arcs (pretty much exactly the same as DoubleSkeletalChange)
		} else {
			
			int varA = tom.nodeAt( A-numNodes );
			int varB = tom.nodeAt( B-numNodes );
			
			
			int change = 0;
			if( dtom.isArc(varA, varC) ) change--;	//A->C exists, so removing arc
			else change++;
			if( dtom.isArc(varB, varC) ) change--;	//B->C exists, so removing arc
			else change++;
			
			if( nodeC.getNumParents() + change > dtom.getMaxNumParents() ) return false;	//Too many parents after making change
			
			//Get node cost before making changes:
			double oldChildCost = caseInfo.nodeCache.getMMLCost( nodeC );
			
			// Arcs A->C & B->C are toggled.
			int[] child = new int[]{ varC, varC };
			int[] parents = new int[]{ varA, varB };
	        
	        // Calculate change in TOM (structure) cost caused by toggle.
	        double toggleCost = caseInfo.tomCoster.costToToggleArcs(tom,child,parents);

	        // Make change to TOM structure:
	        doubleMutate( tom, varA, varB, varC);
	        
	        // Find new cost (after making change)
	        double newChildCost = caseInfo.nodeCache.getMMLCost( nodeC );
	        
	        oldCost = 0;
	        cost = newChildCost - oldChildCost + toggleCost;
			
	        if (accept()) {
	            if (caseInfo.updateArcWeights){
	                if(tom.isArc(varA,varC)) {
	                    caseInfo.arcWeights[varC][varA] -= caseInfo.totalWeight;
	                }                
	                else {
	                    caseInfo.arcWeights[varC][varA] += caseInfo.totalWeight;
	                }

	                if(tom.isArc(varB,varC)) {
	                    caseInfo.arcWeights[varC][varB] -= caseInfo.totalWeight;
	                }                
	                else {
	                    caseInfo.arcWeights[varC][varB] += caseInfo.totalWeight;
	                }
	            }

	            return true;
	        }
	        else {
	            // toggle A->C and B->C back to their original state.
	        	doubleMutate( tom, varA, varB, varC);
	            return false;
	        }
		}
	}
	
	/** Toggle arcs varA_0 -> varC_1 and varB_0 -> varC_1 */
	private void doubleTemporalMutate( DTOM dtom, int varA, int varB, int varC ){
		boolean AC, BC;
		AC = dtom.isTemporalArc(varA, varC);
		BC = dtom.isTemporalArc(varB, varC);
		
		if( AC ) dtom.removeTemporalArc(varA, varC);
		if( BC ) dtom.removeTemporalArc(varB, varC);
		
		if( !AC ) dtom.addTemporalArc(varA, varC);
		if( !BC ) dtom.addTemporalArc(varB, varC);
	}
	
	/** Toggle arcs A_0 -> C_1 and B_1 -> C_1 --- i.e. one temporal arc and one intraslice arc */
	private void crossArcMutate( DTOM dtom, int varTemporal, int varIntraslice, int varC ){
		boolean temporal, intraslice;
		temporal = dtom.isTemporalArc(varTemporal, varC);
		intraslice = dtom.isArc(varIntraslice, varC);
		
		if( temporal ) dtom.removeTemporalArc(varTemporal, varC);
		if( intraslice ) dtom.removeArc(varIntraslice, varC);
		
		if( !temporal ) dtom.addTemporalArc(varTemporal, varC);
		if( !intraslice ) dtom.addArc(varIntraslice, varC);
	}
}
