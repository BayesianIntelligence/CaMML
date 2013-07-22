package camml.core.searchDBN;

import java.util.Random;

import camml.core.search.CaseInfo;
import camml.core.search.TOM;
import camml.core.search.TOMTransformation;

public class DBNParentSwapChange extends TOMTransformation {

	/** cost to add TEMPORAL arc = -log(arcProbTemporal) - -log(1.0-arcProbTemporal)*/
	//double arcCostTemporal;
	
	/** pre-allocated return value for getNodesChanged() */
    private int[] changes = new int[1];
	
    public DBNParentSwapChange(Random rand, double arcProb, double arcProbTemporal, CaseInfo caseInfo, double temperature) {
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
		DNode nodeC = (DNode)tom.getNode(varC);
		int varCNumParents = nodeC.getNumParents();
		
		changes[0] = varC;
		
		//First: Does variable C have any parents? If not - return (can't swap parents if none exist!)
		if( nodeC.getNumParents() < 1 ) return false;
		
		//Second: Does variable C have any non-parents? (could occur in small networks)
		if( nodeC.getNumParents() == (dtom.getNumNodes() + dtom.getNodePos(varC)) ) return false;
		
		
		//Pick one of C's parents at random
		int P = (int)( varCNumParents * rand.nextDouble() );
		int varA;
		boolean oldParentIsIntraslice;
		
		//Is this parent an intraslice parent (i.e. A_1 -> C_1) or temporal parent (i.e. A_0 -> C_1)?
		if( P < nodeC.getNumIntrasliceParents() ){
			varA = nodeC.getParentCopy()[P];
			oldParentIsIntraslice = true;
		} else {
			varA = nodeC.getTemporalParentCopy()[P - nodeC.getNumIntrasliceParents() ];
			oldParentIsIntraslice = false;
		}
		
		//Do we replace C's parent with an intraslice parent or a temporal parent?
		//Randomly select a node to replace existing parent:
		boolean newParentIsIntraslice;
		int varB;
		int maxIncomingArcs = dtom.getNumNodes() + dtom.getNodePos(varC);	//N possible parents from first slice + nodePosition(C) possible parents from same time slice
		int selectedParentNumber = (int)( maxIncomingArcs * rand.nextDouble() );
		//if( selectedParentNumber < dtom.getNumNodes() ){	//Parent is intraslice - i.e. B_1 -> C_1
		if( selectedParentNumber < dtom.getNodePos(varC) ){
			varB = tom.nodeAt(selectedParentNumber);	//Want the 'selectedParentNumber'th node in the total order
			newParentIsIntraslice = true;
		} else {								//Parent is interslice (temporal) - i.e. B_0 -> C_1
			varB = selectedParentNumber - dtom.getNodePos(varC);
			newParentIsIntraslice = false;
		}
		
		//Need to ensure that B -> C does not already exist...
		while( (newParentIsIntraslice && dtom.isArc(varB, varC)) ||  (!newParentIsIntraslice && dtom.isTemporalArc(varB, varC)) ){	//Need to select a new arc...
			selectedParentNumber = (int)( maxIncomingArcs * rand.nextDouble() );
			//if( selectedParentNumber < dtom.getNumNodes() ){	//Parent is intraslice - i.e. B_1 -> C_1
			if( selectedParentNumber < dtom.getNodePos(varC) ){
				varB = tom.nodeAt(selectedParentNumber);	//Want the 'selectedParentNumber'th node in the total order
				newParentIsIntraslice = true;
			} else {								//Parent is interslice (temporal) - i.e. B_0 -> C_1
				varB = selectedParentNumber - dtom.getNodePos(varC);
				newParentIsIntraslice = false;
			}
		}
		
		//Get old node cost, and structure cost to toggle arcs:
		double oldCostC = caseInfo.nodeCache.getMMLCost( nodeC );
		double costToToggleArcs = 0.0;
		if( oldParentIsIntraslice ) costToToggleArcs += caseInfo.tomCoster.costToToggleArc(tom,varA,varC);
		else costToToggleArcs += ((DTOMCoster)caseInfo.tomCoster).costToToggleTemporalArc(dtom, varA, varC);
		
		if( newParentIsIntraslice ) costToToggleArcs += caseInfo.tomCoster.costToToggleArc(tom,varB,varC);
		else costToToggleArcs += ((DTOMCoster)caseInfo.tomCoster).costToToggleTemporalArc(dtom, varB, varC);
		
		
		//At this point: Have selected a existing parent to replace (i.e. A->C) with a new parent (i.e. B->C)
		if( oldParentIsIntraslice ){	//Remove intraslice parent
			dtom.removeArc(varA,varC);
		} else {						//Remove temporal (interslice) parent
			dtom.removeTemporalArc(varA, varC);
		}
		
		if( newParentIsIntraslice ){	//Add intraslice parent
			dtom.addArc(varB, varC);
		} else {						//Add Temporal (interslice) parent
			dtom.addTemporalArc(varB, varC);
		}
		
		//Get new cost:
		double newCostC = caseInfo.nodeCache.getMMLCost( nodeC );
		
		oldCost = 0;
		cost = newCostC - oldCostC + costToToggleArcs;
		
		if( accept() ){
			//Store arc weights
			if( caseInfo.updateArcWeights ){
				if( oldParentIsIntraslice ){	//Removed an intraslice arc
					caseInfo.arcWeights[varC][varA] += caseInfo.totalWeight;	//Final weight when removed minus initial when added...
				} else {						//Removed an interslice (temporal) arc
					caseInfo.arcWeightsDBN[varC][varA] += caseInfo.totalWeight;
				}
				
				if( newParentIsIntraslice ){	//Added a new intraslice arc
					caseInfo.arcWeights[varC][varB] -= caseInfo.totalWeight;
				} else {						//Added a new interslice (temporal) arc
					caseInfo.arcWeightsDBN[varC][varB] -= caseInfo.totalWeight;
				}
			}
			
			return true;
		} else {
			//Undo changes...
			if( newParentIsIntraslice ){	//Remove previously added intraslice parent
				dtom.removeArc(varB, varC);
			} else {						//Remove previously added temporal (interslice) parent
				dtom.removeTemporalArc(varB, varC);
			}
			
			if( oldParentIsIntraslice ){	//Re-add original intraslice parent
				dtom.addArc(varA,varC);
			} else {						//Re-add original temporal (interslice) parent
				dtom.addTemporalArc(varA, varC);
			}
			
			return false;
		}
	}

}
