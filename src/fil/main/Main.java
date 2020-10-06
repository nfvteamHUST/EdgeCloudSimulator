/**
* @author EdgeCloudTeam-HUST
*
* @date 
*/

package fil.main;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;

import fil.algorithm.BLFF.BLFF;
import fil.algorithm.BLLL.BLLL;
import fil.algorithm.BLRESCE.BLRESCE;
import fil.algorithm.RESCE.RESCE;
import fil.algorithm.RESCEFF.RESCEFF;
import fil.algorithm.RESCELL.RESCELL;
import fil.algorithm.SFCCMFF.SFCCMFF;
import fil.algorithm.SFCCMLL.SFCCMLL;
import fil.algorithm.SFCCMRESCE.SFCCMRESCE;
import fil.resource.virtual.GenRequest;

public class Main {
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Before run");
		GenRequest genRequest = new GenRequest();
		BLFF blFF = new BLFF();
		BLLL blLL = new BLLL();
		BLRESCE blRESCE = new BLRESCE();
		RESCEFF resceFF = new RESCEFF();
		RESCELL resceLL = new RESCELL();
		RESCE resce = new RESCE();
		
		SFCCMFF sfccmFF = new SFCCMFF();
		SFCCMLL sfccmLL = new SFCCMLL();
		SFCCMRESCE sfccmRESCE = new SFCCMRESCE();
		
		//<-----------generate list of request
		Map<Integer,HashMap<Integer,LinkedList<Double>>> request = genRequest.joinRequest();
		LinkedList<Integer> numChainPoisson = genRequest.getNumChainPoisson();
		
		//<-----------run all algorithm here
		blFF.run(request,numChainPoisson);
		blLL.run(request,numChainPoisson);
		blRESCE.run(request,numChainPoisson);
		resceFF.run(request, numChainPoisson);
		resceLL.run(request, numChainPoisson);
		resce.run(request, numChainPoisson);
		sfccmFF.run(request,numChainPoisson);
		sfccmLL.run(request,numChainPoisson);
		sfccmRESCE.run(request, numChainPoisson);
	}
}
