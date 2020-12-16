package fil.resource.virtual;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import fil.resource.virtual.PoissonDistribution;

public class GenRequest {
	final static double LIVETIME = 0.5;
	final static int COF = 6; //6 for full 24h option //3 for not-full option
	final static int PINUMBER = 300;
	private PoissonDistribution number;
//	private int [] LUT = {32,26,22,20,20,21,23,27,37,55,89,143,211,284,335,350,337,308,282,267,263,264,269,273,
//			276,280,287,296,313,330,350,372,375,356,304,284,269,206,151,125,110,96,80,59}; //30 min/a request
	private int [] LUT = {32,22,20,23,37,89,211,335,337,282,263,269,
			276,287,313,350,375,304,269,151,110,80,30,20}; //full 24h
//	private int [] LUT = {32,22,20,23,37,89,211,335,337,282,263,269,
//			276,287,313,350,375,304,269,20,10,10,30,20,20}; //testing, official is above
	//private int [] LUT = {32,22,20,23,37,89,211,335,337,400,450,500,525}; 
	//private int [] LUT = {21,23,27,37,55,89,143,211,284,335,350,400,500,600};
	//private int [] LUT = {25,50,100,150,200,250,300,350,400,500,600,700};
//	private Double [] LUT = {0.025,0.05,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,1.0}; // not-full
	//private int [] LUT = {50,100,150,200,250,300,350,400,450,500,550,600,650,700};

	//private int [] LUT = {22, 44, 66, 88, 110, 132, 154, 176, 198, 220, 242, 264, 286, 308, 330, 352, 374, 396, 418, 440, 462, 484
			//, 506};
	//private int [] LUT = {48, 33, 30, 35, 56, 134, 317, 503, 506, 423, 395, 404, 414, 431, 470, 525, 563, 456, 404, 227, 165, 120, 45,30};
	//private int [] LUT = {90, 62, 56, 65, 104, 250, 591, 938, 944, 790, 737, 754, 773, 804, 877, 980, 1050, 852, 754, 423 ,308, 224, 84, 56
//};
	
	private LinkedList<Integer> numChainPoisson;
	
	public GenRequest() {
		this.number = new PoissonDistribution();
		this.numChainPoisson = new LinkedList<>();
		
	}
	
	public Map<Integer,HashMap<Integer,LinkedList<Double>>> joinRequest() {
		int size = LUT.length;
//		for(int i = 0; i < size; i++) {
//			LUT[i] = (LUT[i])*COF;
//		}
		
		Map<Integer,HashMap<Integer,LinkedList<Double>>> allRequest = new HashMap<>();
		
		for(int req = 0; req < size; req++) { // size = # of requests
//			int numChain = number.sample(LUT[req]) + 1;
//			int numChain = (int)(LUT[req]*700); // non-24h
			int numChain = LUT[req]*COF; //full 24h
//			int chainSupply = (int) Math.ceil(numChain*1.0/PINUMBER); // equally separate request
//			System.out.println("chainSupply: " +chainSupply);
//			numChain *= COF;
			this.numChainPoisson.add(numChain);
			Random rand = new Random();
			HashMap<Integer, LinkedList<Double>> request = new HashMap<>();
			for(int pi = 0; pi < PINUMBER; pi++) {
				request.put(pi, new LinkedList<>());
			}
			HashMap<Integer, Integer> request_temp = new HashMap<>(); // generate number of request for each Pi
			for(int pi = 0; pi < PINUMBER; pi++) {
				request_temp.put(pi, 0);
			}
			
			while(numChain > 0) {
				int device = rand.nextInt(PINUMBER);
				int camera = rand.nextInt(2); // # of camera need to be opened
				int totalCamera = request_temp.get(device) + camera;
				request_temp.put(device, totalCamera);
				numChain -= camera;
			}
			 for(int pi = 0; pi < PINUMBER; pi++) { // generate live time for each request
				 int totalCamera = request_temp.get(pi);
				 for (int cam = 0; cam < totalCamera; cam++) {
					request.get(pi).add(StdRandom.exp(LIVETIME)); // generate live time for camera
					//request.get(pi).add(2.0);
				 }
			 }
			allRequest.put(req, request);
		}
		return allRequest;
	}
	
	public int leaveRequest(int time) {
		double lamdaTemp = (-9.5)*time + 290;
		if(lamdaTemp < 0) {
			return 0;
		}
		double lamda = Math.floor(lamdaTemp);
		return number.sample(lamda);
	}
	
	public int receiveRequestJoin(int totalRequestRemain, double cpu, double bw) {
		double lamda;
		double request  = 0;
		double resource_condition = (cpu + bw)/200;
		if (resource_condition >= 0 && resource_condition <= 0.13) lamda = 0;
		else if (resource_condition > 0.13 && resource_condition <= 0.25) lamda = 1;
		else if (resource_condition > 0.25 && resource_condition <= 0.5) lamda = 2;
		else if (resource_condition > 0.5 && resource_condition <= 1.0) lamda = 3;
		else {
			throw new java.lang.Error("Error occurs at lamda process");
		}
		do {
			request = number.sample(lamda);
		} while (request > 3 || request > totalRequestRemain);
		return (int) request;
	}
	
	public int receiveRequestLeave(int totalRequestRemain, double cpu, double bw) {
		double lamda;
		double request  = 0;
		double resource_condition = (cpu + bw)/200;
		if (resource_condition >= 0 && resource_condition <= 0.13) lamda = 3;
		else if (resource_condition > 0.13 && resource_condition <= 0.25) lamda = 2;
		else if (resource_condition > 0.25 && resource_condition <= 0.5) lamda = 1;
		else if (resource_condition > 0.5 && resource_condition <= 1.0) lamda = 0;
		else {
			throw new java.lang.Error("Error occurs at lamda process");
		}
		do {
			request = number.sample(lamda);
		} while (request > 3 || request > totalRequestRemain);
		return (int) (request + 1);
	}
	
	public LinkedList<Integer> getNumChainPoisson() {
		return numChainPoisson;
	}
}
