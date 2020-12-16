/**
* @author EdgeCloudTeam-HUST
*
* @date 
*/

package fil.algorithm.RESCE;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import fil.algorithm.routing.NetworkRouting;
import fil.resource.substrate.Rpi;
import fil.resource.virtual.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class RESCE  {
	
	final static int NUM_PI = 300;
	final static int K_PORT_SWITCH = 10; // 3 server/edge switch
	final static int TOTAL_CAPACITY = 40000;
	final static int EDGE_CAPACITY = 30000;
	final static int CLOUD_CAPACITY = 25000;
	
	private Topology topo;
	private FatTree fatTree;
	private MappingServer mappingServer;
	private NetworkRouting coreNetwork;

	private LinkedList<Rpi> listRpi;
	private LinkedList<Integer> edgePosition;
	private Map<Rpi, LinkedList<SFC>> listSFConRpi;

	public RESCE() { // default constructor
		
		topo = new Topology();
		fatTree = new FatTree();
		topo = fatTree.genFatTree(K_PORT_SWITCH);
		mappingServer = new MappingServer();	
		coreNetwork = new NetworkRouting();
		
		listSFConRpi = new HashMap<>(NUM_PI); 
		listRpi = new LinkedList<Rpi>();
		
		edgePosition = new LinkedList<>();
		edgePosition.add(10);
		edgePosition.add(5);
		edgePosition.add(13);
		edgePosition.add(14);
		
		for(int i = 0; i < NUM_PI; i++ ) {
			Random rand = new Random ();
			int position = rand.nextInt(4);
			Rpi rpi = new Rpi(i, edgePosition.get(position));
			listRpi.add(rpi);
			listSFConRpi.put(rpi, new LinkedList<SFC>());
		}
		

	}
	

	public void VNFMapping( Topology topo, MappingServer mappingServer, NetworkRouting coreNetwork, Rpi pi, LinkedList<Double> listChainRequest, 
			LinkedList<SFC> listSFConRpi,
			LinkedList<SFC> listSFCFinalPi) {
		//<---- Global variable initiation
		
		
		//<---- local variables - deleted after getting out of this function
		
		boolean doneFlag = false;
		boolean remapPi = false;
		boolean doneMap = false;
		int SFCIndexIncrease = 0;
		int remapLoopCount = 0;
		int numChainSuccessCur = 0;
		double minPower = Integer.MAX_VALUE;
		Capture capture = new Capture();
		Decode decode = new Decode();
		Density density = new Density();
		Receive receive = new Receive();
		
		//<---- count VNF migration
		
//		for(SFC sfc : listSFConRpi) {
//			numVNFinServerBefore += sfc.numServiceInServer();
//		}
		
		MAP_LOOP:
			while (doneFlag == false) {////////////////////TRIAL LOOP////////////////////////
				
	
//				if (listSFConRpi.size() == 7) { // this Pi cannot map more chain
//					System.out.println("Pi number "+(pi.getId()+1)+" is overload\n");
////					rejectPi = 1;
//					pi.setOverload(true); 
//					break;
//				}
				
				if(listChainRequest.size() == 0) {
					System.out.println("Number of request equals zero");
					break; // if no chain need to be mapped then no need to run MAP 
				}
				
				if (remapPi == true && doneMap == false){ //remapp
					System.out.println("Inside remapping edge device number " + remapLoopCount);
					if (remapLoopCount == 0) {
//						numChainSuccessPre = listSFConRpi.size();
//						numChainRequest += listRpiSFC.size(); //sum of all previous successful mapping chain
						for(SFC sfc : listSFConRpi) {
							listChainRequest.addFirst(sfc.getEndTime());; // add first position 
						}
					}
					if(!listSFConRpi.isEmpty()) {
						coreNetwork.NetworkReset(pi); // reset network topology
						mappingServer.getServiceMapping().resetRpiSFC(listSFConRpi, topo); // reset mapped chains pi without remapping at server
						for(SFC sfc : listSFConRpi) { // remove all sfc belongs to pi in listSFCTotal 
							if(mappingServer.getListSFCTotal().contains(sfc))
								mappingServer.getListSFCTotal().remove(sfc);
						}
//						listSFConRpi.clear();
						
					}
					
					listSFConRpi.clear();
					pi.reset(); //reset RPI
					doneMap = true;
					remapLoopCount ++; // count number of remapping times in MAP_LOOP
				}
				
				int offDecode = 0;
				int offDensity = 0;
				int numChainRequest = listChainRequest.size(); // reallocate as listChainSize may had been changed
				
				OFFDECODE_LOOP:
				for (offDecode = 0; offDecode <= numChainRequest; offDecode++) {
					OFFDENSITY_LOOP:
					for(offDensity = offDecode; offDensity >= offDecode && offDensity <= numChainRequest; offDensity++) {
						int numOffDecode = 0;
						int numOffDensity = 0;
						LinkedList<SFC> listSFCTemp = new LinkedList<>();
						
						for(int numSFC = 0; numSFC < numChainRequest; numSFC++) { // initialise SFC list
							double endTime = listChainRequest.get(numSFC); // error after remapping numChain exceeds listChainSize
//							String sfcID = String.valueOf(SFCIndexIncrease);
							SFC sfc = new SFC(SFCIndexIncrease, pi.getId(), endTime); 
							sfc.setServicePosition(capture.getServiceType(), true); // always at Rpi
							sfc.setServicePosition(receive.getServiceType(), false);// always at server
							if(numOffDecode < offDecode) {
								sfc.setServicePosition(decode.getServiceType(), false);
							}
							else sfc.setServicePosition(decode.getServiceType(), true);
							if(numOffDensity < offDensity) {
								sfc.setServicePosition(density.getServiceType(), false);
							}
							else sfc.setServicePosition(density.getServiceType(), true);
							
							listSFCTemp.add(sfc);
							SFCIndexIncrease++;
							numOffDecode++;
							numOffDensity++;
						}
						
						double bandwidthPiUsed = (offDecode)*capture.getBandwidth() + (offDensity - offDecode)*decode.getBandwidth() + (numChainRequest - offDensity)*density.getBandwidth();
						double cpuPiUsed = numChainRequest*capture.getCpu_pi() + (numChainRequest - offDecode)*decode.getCpu_pi() + (numChainRequest - offDensity)*density.getCpu_pi();
						
						
						//<-------- check Pi resource pool 
						if(bandwidthPiUsed > pi.getRemainBandwidth()) {
							if (remapPi == true) {
								listChainRequest.removeLast(); // remove last object
								System.out.println("Request remove 1 SFC due to low BW.");
								if (listChainRequest.size() <= numChainSuccessCur) { // prevent system continue loop even final result has been selected
									break MAP_LOOP;
								} else {
									doneMap = false;
									break OFFDECODE_LOOP; // try to map with lower num of chain
								}
							} else if (numChainSuccessCur == 0) {
								remapPi = true;
								doneMap = false;
								break OFFDECODE_LOOP; // try to remap
							} else {
								break MAP_LOOP; 
							}

						} else if(cpuPiUsed > pi.getRemainCPU()){
							if (offDecode == numChainRequest && remapPi == false) {
								remapPi = true; //turn on remapping Pi
								break OFFDECODE_LOOP;
							} else
								continue; // try to offload service  
						} else {
							double powerPiTemp = numChainRequest*capture.getPower() +(numChainRequest - offDecode)*decode.getPower() + (numChainRequest - offDensity)*density.getPower();
							double powerServerTemp = calculatePseudoPowerServer(numChainRequest*receive.getCpu_server() + offDecode*decode.getCpu_server() + offDensity*density.getCpu_server());
							double totalPowerTemp = powerPiTemp + powerServerTemp;
							
							if (numChainRequest >= numChainSuccessCur && totalPowerTemp <= minPower) { //QoS and acceptance rate priority
								numChainSuccessCur = numChainRequest;
								minPower = totalPowerTemp;
								doneFlag = true; // used to break MAP_LOOP


//								}
								//<------calculate number of VNF migration
//								if(remapPi == true) {
//									for(int i = 0; i < numChainBefore; i++) {
//										numVNFinServerAfter += listSFCTemp.get(i).numServiceInServer();
//									}
//									numVNFMigration += Math.abs(numVNFinServerAfter - numVNFinServerBefore);
//								}
								//<------add to listSFC output
								listSFCFinalPi.clear();
								for(int index = 0; index < listSFCTemp.size(); index++) {
									listSFCFinalPi.add(listSFCTemp.get(index));
								}
			
							} else {
								System.out.println("Power is larger than stored value. Relooping... \n");
								if (offDecode == numChainRequest) { // last loop
									break MAP_LOOP;
								}
								continue; //<----allowing looping, this makes code runs for longer time
							} // MIN_POWER CONDITION
						} // BW CPU CONDITION
					} // OFF_DENSITY LOOP
				} // OFF_DECODE LOOP
				
			} 
	}///end mapping function
	
	public static double calculatePseudoPowerServer(double cpuServer) {
		double numServer = Math.floor(cpuServer/100);
		double cpuFragment = cpuServer - 100*numServer;
		 return numServer*powerServer(100) + powerServer(cpuFragment);
	}
	
	public static double powerServer(double cpu) {
		return (95*(cpu/100) + 221);
	}
	
	public static void write_integer (String filename, LinkedList<Integer> x) throws IOException{ //write result to file
 		 BufferedWriter outputWriter = null;
 		 outputWriter = new BufferedWriter(new FileWriter(filename));
  		for (int i = 0; i < x.size(); i++) {
			outputWriter.write(Integer.toString(x.get(i)));
			outputWriter.newLine();
  		}
		outputWriter.flush();  
		outputWriter.close();  
	}
	
	
	public static void write_integer (String filename, int [] x) throws IOException{ //write result to file
		 BufferedWriter outputWriter = null;
		 outputWriter = new BufferedWriter(new FileWriter(filename));
		for (int i = 0; i < x.length; i++) {
			// Maybe:
			//outputWriter.write(x.get(i));
			// Or:
			outputWriter.write(Integer.toString(x[i]));
			outputWriter.newLine();
		}
		outputWriter.flush();  
		outputWriter.close();  
	}

	public static void write_double (String filename, LinkedList<Double> x) throws IOException { //write result to file
 		 BufferedWriter outputWriter = null;
 		 outputWriter = new BufferedWriter(new FileWriter(filename));
  		for (int i = 0; i < x.size(); i++) {
			// Maybe:
//			outputWriter.write(x[i]);
			// Or:
			outputWriter.write(Double.toString(x.get(i)));
			outputWriter.newLine();
  		}
		outputWriter.flush(); 
		outputWriter.close();  
	}
	
	public static void write_double (String filename, double [] x) throws IOException { //write result to file
		 BufferedWriter outputWriter = null;
		 outputWriter = new BufferedWriter(new FileWriter(filename));
		for (int i = 0; i < x.length; i++) {
			// Maybe:
//			outputWriter.write(x[i]);
			// Or:
			outputWriter.write(Double.toString(x[i]));
			outputWriter.newLine();
		}
		outputWriter.flush(); 
		outputWriter.close();  
	}
	
	public static void write_excel(String filename, Map<Integer,LinkedList<Integer>> map) throws IOException {
		//Create blank workbook
	      XSSFWorkbook workbook = new XSSFWorkbook();
	      
	      //Create a blank sheet
	      XSSFSheet spreadsheet = workbook.createSheet();

	      //Create row object
	      XSSFRow row;
	      
	      Set < Integer > keyid = map.keySet();
	      int rowid = 0;
	      
	      for (Integer key : keyid) {
	         row = spreadsheet.createRow(rowid++);
	         LinkedList<Integer> objectArr = map.get(key);
	         int cellid = 0;
	         
	         for (Object obj : objectArr){
	            Cell cell = row.createCell(cellid++);
	            cell.setCellValue(obj.toString());
	         }
	      }
	      //Write the workbook in file system
	      FileOutputStream out = new FileOutputStream(new File(filename));
	      workbook.write(out);
	      out.close();
	      workbook.close();
	}
	
	public static void write_excel_double(String filename, Map<Integer,LinkedList<Double>> map) throws IOException {
		//Create blank workbook
	      XSSFWorkbook workbook = new XSSFWorkbook();
	      
	      //Create a blank sheet
	      XSSFSheet spreadsheet = workbook.createSheet();

	      //Create row object
	      XSSFRow row;
	      
	      Set < Integer > keyid = map.keySet();
	      int rowid = 0;
	      
	      for (Integer key : keyid) {
	         row = spreadsheet.createRow(rowid++);
	         LinkedList<Double> objectArr = map.get(key);
	         int cellid = 0;
	         
	         for (Object obj : objectArr){
	            Cell cell = row.createCell(cellid++);
	            cell.setCellValue(obj.toString());
	         }
	      }
	      //Write the workbook in file system
	      FileOutputStream out = new FileOutputStream(new File(filename));
	      workbook.write(out);
	      out.close();
	      workbook.close();
	}

	public void run(Map<Integer,HashMap<Integer,LinkedList<Double>>> listRequest, LinkedList<Integer> timeWindow){
		 // Creating a File object that represents the disk file. 
//        PrintStream out = new PrintStream(new File("./Plot/output.txt")); 
//        System.setOut(out); 

		
		LinkedList<Double> totalPowerSystemConsolidation = new LinkedList<Double>();
		LinkedList<Double> totalPowerSystem = new LinkedList<Double>();
		LinkedList<Double> totalPowerPerSFC = new LinkedList<Double>();
		LinkedList<Double> serverUtilization = new LinkedList<Double>();
		LinkedList<Double> totalChainAcceptance = new LinkedList<Double>();
		LinkedList<Double> totalPiAcceptance = new LinkedList<Double>();
		LinkedList<Integer> listServerUsed = new LinkedList<Integer>();
		LinkedList<Integer> totalChainSystem = new LinkedList<Integer>();
		LinkedList<Integer> totalChainActive = new LinkedList<Integer>();
		LinkedList<Integer> listDecInCloud = new LinkedList<Integer>();
		LinkedList<Integer> listDenInCloud = new LinkedList<Integer>();
		LinkedList<Integer> listReceiveInCloud = new LinkedList<Integer>();
		LinkedList<Integer> totalChainReject = new LinkedList<Integer>();
		LinkedList<Double> totalLoadEdge = new LinkedList<Double>();
		LinkedList<Double> totalBwEdge = new LinkedList<Double>();
		LinkedList<Integer> totalChainLeave = new LinkedList<Integer>();
		LinkedList<Integer> totalChainRequest = new LinkedList<>();
		LinkedList<Integer> listVNFmigration = new LinkedList<>();
		LinkedList<Double> listLinkUsage = new LinkedList<Double>();
		LinkedList<Double> cpuEdgeUsagePerSFC = new LinkedList<Double>();
		LinkedList<Double> cpuServerUsagePerSFC = new LinkedList<Double>();
		LinkedList<Double> averageBWUsage = new LinkedList<Double>();
		LinkedList<Double> capacity = new LinkedList<Double>();
		LinkedList<Double> capacityEdge = new LinkedList<Double>();
		LinkedList<Double> capacityCloud = new LinkedList<Double>();
		
		Map<Integer, LinkedList<Double>> listStoreValue = new HashMap<>();
		for(int i = 0; i < timeWindow.size(); i ++) {
			listStoreValue.put(i,new LinkedList<Double>());
		}
		
		Map<Integer, LinkedList<Integer>> listLeaveTotal = new HashMap<>();
		Map<Integer, LinkedList<Integer>> listOffForEachPi_temp = new HashMap<>();
		Map<Integer, LinkedList<Integer>> listOffForEachPi = new HashMap<>();
		Map<Integer, LinkedList<Double>> listCPUForEachPi = new HashMap<>();
		Map<Integer, LinkedList<Double>> listBWForEachPi = new HashMap<>();
		Map<Integer, LinkedList<Double>> listCpuEdgePerSFC = new HashMap<>();
		Map<Integer, LinkedList<Double>> listBWPerSFC = new HashMap<>();
		
		for(int i = 0; i < NUM_PI; i++) {
			
			listLeaveTotal.put(i,new LinkedList<Integer>());
			listOffForEachPi_temp.put(i, new LinkedList<Integer>());
			listOffForEachPi.put(i, new LinkedList<Integer>());
			listCPUForEachPi.put(i, new LinkedList<Double>());
			listBWForEachPi.put(i, new LinkedList<Double>());
			listCpuEdgePerSFC.put(i, new LinkedList<Double>());
			listBWPerSFC.put(i, new LinkedList<Double>());
		}
		
//		double [] sumBwNumPi = new double [timeWindow.size()];
//		Arrays.fill(sumLoadNumPi, 0);
//		Arrays.fill(sumBwNumPi, 0);
	
		double acceptance = 0;
		double acceptancePi = 0;
		int requestIndex = 0; // number of request
	
		
		LinkedList<Integer> requestRandomReceive = new LinkedList<>();
//		LinkedList<Double> storeCurrentDen = new LinkedList<>();
//		storeCurrentDen.add(0.0);
//		LinkedList
		
		//<------REQUEST_LOOP
		while (requestIndex < timeWindow.size()) { //////////////////////////////////////////////////////////////////////////////////////////
			// store value in order: arrival -cpuPerSFC (b4) - bwPerSFC (b4) - # vnf in system  - target: join vnf
			LinkedList<Double> storeValue = new LinkedList<>();
			// store "current" time
			storeValue.add((double)requestIndex);
			// store "next" arrival rate
			storeValue.add((double)timeWindow.get(requestIndex));
			// store "current" cpuEdge, bwEdge
			storeValue.add(mappingServer.cpuEdgePerSFC());
			storeValue.add(mappingServer.bwEdgePerSFC());
//			 store "current" number of VNF in the system
			storeValue.add((double)mappingServer.getNumServiceInCloud("receive"));
			// store "leave" number of VNF in the system
//			storeNumDen.add((double)mappingServer.getNumServiceInCloud("density"));
			
			HashMap<Integer,LinkedList<Double>> listRequestPi = listRequest.get(requestIndex);
			int numSFCLevThisTW = 0;
			int numDenLevThisTW = 0;
			int numSerJoinCloud = 0;
			int numPiReceive = 0; // number of Pi receives request > 0
			int piAccept = 0;
			int numSFCReqThisTW = 0;
			int numMapReqThisTW = 0;
			LinkedList<Double> loadEdgeNumPi = new LinkedList<>();
			LinkedList<Double> bwEdgeNumPi = new LinkedList<>();			
		
			for (Entry<Integer, LinkedList<Double>> entry : listRequestPi.entrySet()) { //change i < 1 to i < num_pi for mapping every pi/////////////////////////////////
				Rpi pi = listRpi.get(entry.getKey());
				int numSFCReqThisPi = entry.getValue().size();
				numSFCReqThisTW += numSFCReqThisPi;

				if (numSFCReqThisPi != 0)
					numPiReceive ++;
			
				LinkedList<SFC> listSFCFinalPi = new LinkedList<SFC>();
				LinkedList<SFC> listCurSFCOnPi = listSFConRpi.get(pi);
				LinkedList<SFC> listSFCLeave = new LinkedList<>();
				
				System.out.println("Request number " + requestIndex);
				System.out.println("Pi number " + (pi.getId()+1)+ " with " +entry.getValue().size()+ " chains need to be mapped");
				System.out.println("Pi has mapped "+ listCurSFCOnPi.size());
				
				//<--------END--OF--SERVICE--PROCESS
				System.out.println("Start leaving process ....");
				
				if(listCurSFCOnPi.size() != 0 && listCurSFCOnPi != null) {
					boolean flagLeave = false;
					for(SFC sfc : listCurSFCOnPi) {
						if(sfc.getEndTime() <= requestIndex) {
							flagLeave = true;
							listSFCLeave.add(sfc);
							numSFCLevThisTW ++;
						}
					}
					
					if(flagLeave == true) {
						
						for(SFC sfc : listCurSFCOnPi) { // remove all sfc belongs to pi in listSFCTotal 
							if(mappingServer.getListSFCTotal().contains(sfc))
								mappingServer.getListSFCTotal().remove(sfc);
						}
						
						mappingServer.getServiceMapping().resetRpiSFC(listCurSFCOnPi, topo); // reset at server
						coreNetwork.NetworkReset(pi); // reset network
						pi.reset(); // reset rpi
						
						//<-----------remap leftover SFC
						for(SFC sfc : listSFCLeave) {
							for(Service ser : sfc.getListService()) {
								if(!ser.isBelongToEdge() && ser.getServiceType() == "density")
									numDenLevThisTW ++;
							}
							if(listCurSFCOnPi.contains(sfc))
								listCurSFCOnPi.remove(sfc);
						}
						LinkedList<Double> listSFCRemapLeave = new LinkedList<>();
						for (SFC sfc : listCurSFCOnPi) {
							double endTime = sfc.getEndTime();
							listSFCRemapLeave.add(endTime);
						}
						listCurSFCOnPi.clear();
						if(listSFCRemapLeave.size() != 0){
							VNFMapping(topo, mappingServer, coreNetwork, pi, listSFCRemapLeave, listCurSFCOnPi, listSFCFinalPi);
							coreNetwork.NetworkRun(listSFCFinalPi, pi);
							mappingServer.runMapping(listSFCFinalPi, topo);
							//===finalize after remapping ====//
							listCurSFCOnPi.addAll(mappingServer.getListSFC());
							
							Double cpuEdgeUsage = 0.0;
							Double bwEdgeUsage = 0.0;
							for(SFC sfc : mappingServer.getListSFC()) {
								cpuEdgeUsage += sfc.cpuEdgeUsage();
								bwEdgeUsage += sfc.bandwidthUsageOutDC();
							}
							pi.setUsedCPU(cpuEdgeUsage); // change CPU pi-server
							pi.setUsedBandwidth(bwEdgeUsage); //change Bandwidth used by Pi
							listSFCFinalPi.clear();
						}
					}
				}
				else ;
//				listLeaveForEachPi.get(pi.getId()).add(numSFCLevThisPi);
				//<------------JOIN --PROCESS
				System.out.println("Start joining process ....");
				LinkedList<Double> listEndTime = new LinkedList<>();
				for(int index = 0; index < entry.getValue().size(); index++) {
					double endTime = entry.getValue().get(index) + (double) requestIndex;
					listEndTime.add(endTime);
				}
				
				if(pi.isOverload() == true) {
					System.out.println("This pi is already overloaded");
				}
				else if(listEndTime.size() == 0) {
					System.out.println("This pi receive zero request");

				}
				else {
					VNFMapping(topo, mappingServer, coreNetwork, pi, listEndTime, listCurSFCOnPi, listSFCFinalPi);
				}
				
				//<-----Only successful mapping can jump into the below condition
				if(listSFCFinalPi.size() != 0 && pi.isOverload() == false && listEndTime.size()!= 0) { // case Pi mapping is success
					
//					run mapping server for final listSFC
//					Random rand = new Random();
//					int position = rand.nextInt(4); // random a position where request comes from
//					
//					rejectRouting = networkRouting.NetworkRun(edgePosition.get(position), listSFCFinal, listRpi.get(pi));
					mappingServer.runMapping(listSFCFinalPi, topo);
					//<------finalize after remapping 
					listCurSFCOnPi.addAll(mappingServer.getListSFC());
					//<------set value for Rpi after mapping successfully
					Double cpuEdgeUsage = 0.0;
					Double bwEdgeUsage = 0.0;
					for(SFC sfc : mappingServer.getListSFC()) {
						cpuEdgeUsage += sfc.cpuEdgeUsage();
						bwEdgeUsage += sfc.bandwidthUsageOutDC();
					}
					pi.setUsedCPU(cpuEdgeUsage); // change CPU pi-server
					pi.setUsedBandwidth(bwEdgeUsage); //change Bandwidth used by Pi
	
					numMapReqThisTW += mappingServer.getListSFC().size();
					
					// store number of VNF joins to the Cloud
					for(SFC sfc : mappingServer.getListSFC()) {
						for(Service ser : sfc.getListService()) {
							if(ser.getServiceType() == "density" && !ser.isBelongToEdge())
								numSerJoinCloud ++;
						}
					}
					piAccept ++; //num of accepted Pi (accept if at least 1 SFC has been mapped
				}
				
				//<----Every mapping step has to do this
				loadEdgeNumPi.add(pi.getUsedCPU());
				bwEdgeNumPi.add(pi.getUsedBandwidth());		
				int offServiceCur = 0;

				for(SFC sfc : listCurSFCOnPi) {
					if(sfc.getService(2).getBelongToEdge() == false) {
						offServiceCur ++;
					}
					if(sfc.getService(3).getBelongToEdge() == false) {
						offServiceCur ++;
					}
				}

				listOffForEachPi_temp.get(pi.getId()).add(offServiceCur);
				if(requestIndex == 0) {
					listOffForEachPi.get(pi.getId()).add(offServiceCur);
				}else {
					int offServicePre = listOffForEachPi_temp.get(pi.getId()).get(requestIndex - 1);
					int offService = offServiceCur - offServicePre;
					listOffForEachPi.get(pi.getId()).add(offService);
				}
				double cpuPiCur = pi.getUsedCPU();
				double bwPiCur = pi.getUsedBandwidth();
				listCPUForEachPi.get(pi.getId()).add(cpuPiCur);
				listBWForEachPi.get(pi.getId()).add(bwPiCur);
				
			} //end Rpi for loop
			
			// store "next" leave value for ML model
//			storeValue.add((double)numSFCLevThisTW);
//			 store "next" VNF number in Cloud: Target value
//			storeCurrentDen.add((double)mappingServer.getNumServiceInCloud("density"));
//			storeValue.add(storeCurrentDen.get(requestIndex + 1) - storeCurrentDen.get(requestIndex));
			storeValue.add((double)mappingServer.getNumServiceInCloud("receive"));
			//			storeValue.add((double)numSerJoinCloud);
			//store "next" VNF number leave in Cloud: Target
//			storeValue.add((double)numDenLevThisTW);
			listStoreValue.put(requestIndex, storeValue);
			//<------Number of service Decode Density Receive in Cloud
			
			int numDenInCloud = 0;
			int numDecInCloud = 0;
			int numReceiveInCloud = 0;
			
			for(SFC sfc : mappingServer.getListSFCTotal()) {
				if(sfc.getService(2).getBelongToEdge() == false) {
					numDecInCloud ++;
				}
				if(sfc.getService(3).getBelongToEdge() == false) {
					numDenInCloud ++;
				}
				if(sfc.getService(4).getBelongToEdge() == false) {
					numReceiveInCloud ++;
				}
			}
			
			listDecInCloud.add(numDecInCloud);
			listDenInCloud.add(numDenInCloud);
			listReceiveInCloud.add(numReceiveInCloud);
			
			double sumCPUPi = 0.0;
			double sumBwPi = 0.0;
			
			for (int index = 0; index < NUM_PI; index++) {
				sumCPUPi += listRpi.get(index).getUsedCPU();
				sumBwPi += listRpi.get(index).getUsedBandwidth();
			}
			
			totalLoadEdge.add(requestIndex,(sumCPUPi/(NUM_PI)));
			totalBwEdge.add(requestIndex,(sumBwPi/NUM_PI));
			
			acceptance = (numMapReqThisTW*1.0)/numSFCReqThisTW; //after a request
			acceptancePi = (piAccept*1.0)/numPiReceive;
			
			totalChainAcceptance.add(requestIndex, acceptance);
			totalPiAcceptance.add(requestIndex, acceptancePi);
			totalChainRequest.add(numMapReqThisTW);
			//<------calculate average bandwidth usage
			double totalBandwidthSFC = 0;
			double totalSFCSize = 0;
			for(Entry<Rpi, LinkedList<SFC>>  entry : listSFConRpi.entrySet()) {
				LinkedList<SFC> listSFCRpi = entry.getValue();
				totalSFCSize += listSFCRpi.size();
				for(SFC sfc : listSFCRpi) {
					totalBandwidthSFC += sfc.getBandwidthSFC();
				}
			}
			averageBWUsage.add((totalBandwidthSFC*1.0)/totalSFCSize);
			serverUtilization.add(topo.getCPUServerUtilization());
			listServerUsed.add(topo.getServerUsed());
			totalChainActive.add(mappingServer.getListSFCTotal().size());
			totalPowerSystem.add( mappingServer.getPower() + mappingServer.PowerEdgeUsage());
			
			//<-------calculate system capacity block
			double cpuEdge = 0;
			double cpuCloud = 0;
			double usedCapacity = 0;
			double usedCapacityEdge = 0;
			double usedCapacityCloud = 0;

			cpuEdge = mappingServer.cpuEdgeAllSFC();
			cpuCloud = mappingServer.cpuServerAllSFC();
			usedCapacity = (cpuEdge/2 + cpuCloud)*1.0/TOTAL_CAPACITY;
			usedCapacityEdge = cpuEdge/EDGE_CAPACITY;
			usedCapacityCloud = cpuCloud/CLOUD_CAPACITY;
			capacityEdge.add(usedCapacityEdge);
			capacityCloud.add(usedCapacityCloud);
			capacity.add(usedCapacity);
//			linkUsagePerSFC.add(mappingServer.linkUsagePerSFC());
			cpuEdgeUsagePerSFC.add(mappingServer.cpuEdgePerSFC());
			cpuServerUsagePerSFC.add(mappingServer.cpuServerPerSFC());
			//<------consolidation block
			LinkedList<SFC> listSFCTotal = new LinkedList<>();			
			
			for(Entry<Rpi, LinkedList<SFC>> entry : listSFConRpi.entrySet()) {
				LinkedList<SFC> listSFCRpi = entry.getValue();
				for(SFC sfc : listSFCRpi) {
					sfc.resetSFC();
					listSFCTotal.add(sfc);
				}
			}
			int listSFCTotalSize = 	listSFCTotal.size();
			topo = new Topology();
			fatTree = new  FatTree();
			topo = fatTree.genFatTree(K_PORT_SWITCH);
			mappingServer = new MappingServer();
			mappingServer.runMapping(listSFCTotal, topo);
			if(mappingServer.getListSFC().size() < listSFCTotalSize)
				throw new java.lang.Error("Error occurs in consolidation block.");

			double totalPowerSystemConso = mappingServer.PowerEdgeUsage() + mappingServer.getPower();
			double totalPowerPerSFC_temp = (totalPowerSystemConso*1.0)/(mappingServer.getListSFCTotal().size()*1.0);
			totalPowerPerSFC.add(totalPowerPerSFC_temp);
//			totalEdgePowerSystem.add(mappingServer.PowerEdgeUsage());
//			totalServerPowerSystem.add( mappingServer.getPower());
			totalPowerSystemConsolidation.add(totalPowerSystemConso);
			listLinkUsage.add(mappingServer.linkUsagePerSFC());
//			//===add more VNF migration after consolidate===//
//			numVNFMigration += (mappingServer.getListSFC().size()*4); // 4 for 4 VNF in total
//			numVNFMigration += mappingServer.getNumVNFMigration();
//			//===store number of VNF migration ===========//
//			listVNFmigration.add(numVNFMigration);
			requestIndex++;
		} // end while loop (request)
		//<-------calculate link utilization
//		LinkedList<Double> linkBandwidth = new LinkedList<>();
//		for(int index = 0; index < topo.getLinkBandwidth().size(); index++) {
//			if(topo.getLinkBandwidth().get(index).getBandwidth() < 1000)
//			linkBandwidth.add(topo.getLinkBandwidth().get(index).getBandwidth());
//		}
//		
//		LinkedList<Double> cpuServerUsed = new LinkedList<>();
//		for(PhysicalServer phy : topo.getListPhyServers().values()) {
//			cpuServerUsed.add(phy.getUsedCPUServer());
//		}
		
		try {
			write_double("./PlotRESCE/totalPiAcceptance.txt",totalPiAcceptance);
			write_double("./PlotRESCE/capacity.txt",capacity);
			write_double("./PlotRESCE/capacityEdge.txt",capacityEdge);
			write_double("./PlotRESCE/capacityCloud.txt",capacityCloud);
			write_double("./PlotRESCE/averageBWUsage.txt",averageBWUsage);
//			write_double("./PlotRESCE/cpuServerUsedOP.txt",cpuServerUsed);
			write_double("./PlotRESCE/totalPowerSystemConsolidation.txt",totalPowerSystemConsolidation);
//			write_double("./PlotRESCE/listLinkUsage.txt",linkUsagePerSFC);
			write_double("./PlotRESCE/cpuEdgeUsagePerSFC.txt",cpuEdgeUsagePerSFC);
			write_double("./PlotRESCE/cpuServerUsagePerSFC.txt",cpuServerUsagePerSFC);
//			write_double("./PlotRESCE/linkBandwidthOP.txt",linkBandwidth);
			write_double("./PlotRESCE/serverUtilization.txt",serverUtilization);
			write_integer("./PlotRESCE/NumVNFMigration.txt",listVNFmigration);
//			write_integer("./PlotRESCE/NumServiceDecDenOP.txt",NumServiceDecDen);
			write_integer("./PlotRESCE/totalChainLeave.txt",totalChainLeave);
			write_integer("./PlotRESCE/listServerUsed.txt",listServerUsed);
			write_integer("./PlotRESCE/requestRandom.txt",requestRandomReceive);
			write_integer("./PlotRESCE/totalDecOffload.txt",listDecInCloud);
			write_integer("./PlotRESCE/totalDenOffload.txt",listDenInCloud);
			write_integer("./PlotRESCE/totaReceiveOffload.txt",listReceiveInCloud);
			write_double("./PlotRESCE/totalPowerSystem.txt",totalPowerSystem);
			write_double("./PlotRESCE/totalPowerSystemPerSFC.txt",totalPowerPerSFC);
			
//			write_double("./PlotRESCE/totalEdgePowerSystemOP.txt", totalEdgePowerSystem);
//			write_double("./PlotRESCE/totalServerPowerSystemOP.txt", totalServerPowerSystem);
			write_double("./PlotRESCE/totalLoadEdge.txt",totalLoadEdge);
			write_double("./PlotRESCE/totalBwEdge.txt",totalBwEdge);
			write_double("./PlotRESCE/totalChainAcceptance.txt",totalChainAcceptance);
//			write_double("./PlotRESCE/sumLoadNumPiOP.txt", sumLoadNumPi);
//			write_double("./PlotRESCE/sumBwNumPiOP.txt", sumBwNumPi);
			write_integer("./PlotRESCE/totalChainSystem.txt",totalChainSystem);
			write_integer("./PlotRESCE/totalChainActive.txt",totalChainActive);
			write_integer("./PlotRESCE/totalChainReject.txt",totalChainReject);
//			write_integer("./PlotRESCE/numChainRequestOP.txt",numChainRequest);
//			write_integer("./PlotRESCE/numChainAcceptOP.txt",numChainAccept);
//			write_excel("./PlotRESCE/requestEachPiDetail.xlsx",listRequestForEachPi);
//			write_excel("./PlotRESCE/leaveEachPiDetail.xlsx",listLeaveForEachPi);
//			write_excel("./PlotRESCE/offSerEachPi01.xlsx",listOffForEachPi);
			write_excel_double("./PlotRESCE/listStoreValue.xlsx",listStoreValue);
//			write_excel_double("./PlotRESCE/bwEachPi01.xlsx",listBWForEachPi);
			System.out.println("Completed.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Map<Rpi, LinkedList<SFC>> getListSFConRpi() {
		return listSFConRpi;
	}


	public void setListSFConRpi(Map<Rpi, LinkedList<SFC>> listSFConRpi) {
		this.listSFConRpi = listSFConRpi;
	}


	public LinkedList<Rpi> getListRpi() {
		return listRpi;
	}


	public void setListRpi(LinkedList<Rpi> listRpi) {
		this.listRpi = listRpi;
	}


	public LinkedList<Integer> getEdgePosition() {
		return edgePosition;
	}


	public void setEdgePosition(LinkedList<Integer> edgePosition) {
		this.edgePosition = edgePosition;
	}

	
}