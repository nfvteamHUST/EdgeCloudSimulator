/**
* @author EdgeCloudTeam-HUST
*
* @date 
*/

package fil.algorithm.BLFF;

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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class BLFF  {
	
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

	public BLFF() { // default constructor
		
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
	
	public static void BLMapping(Topology topo, MappingServer mappingServer, Rpi pi, LinkedList<Double> listChainRequest, 
			LinkedList<SFC> listSFConRpi, LinkedList<SFC> listSFCFinalPi) {

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
		ReceiveDensity receive = new ReceiveDensity();
		
		MAP_LOOP:
		while (doneFlag == false) {

			if (listSFConRpi.size() == 7) { 
				pi.setOverload(true); 
				break;
			}
			
			if(listChainRequest.size() == 0) {
				break; 
			}
					
			int offDecode = 0;
			int offDensity = 0;
			int numChainRequest = listChainRequest.size(); // reallocate as listChainSize may had been changed

			for (offDecode = 0; offDecode <= numChainRequest; offDecode++) {
				for(offDensity = offDecode; offDensity >= offDecode && offDensity <= numChainRequest; offDensity++) {
					
					int numOffDecode = 0;
					int numOffDensity = 0;
					LinkedList<SFC> listSFCTemp = new LinkedList<>();
					
					for(int numSFC = 0; numSFC < numChainRequest; numSFC++ ) { // initialize SFC list
						double endTime = listChainRequest.get(numSFC); // error after remapping numChain exceeds listChainSize
						String sfcID = String.valueOf(SFCIndexIncrease);
						SFC sfc = new SFC(sfcID,  pi.getId(), endTime);
						sfc.setServicePosition(capture, true);
						sfc.setServicePosition(receive, false);
						
						if(numOffDecode < offDecode) {
							sfc.setServicePosition(decode, false);
						}
						else 
							sfc.setServicePosition(decode, true);
						if(numOffDensity < offDensity) {
							sfc.setServicePosition(density, false);
						}
						else 
							sfc.setServicePosition(density, true);
						
						listSFCTemp.add(sfc);
						SFCIndexIncrease++;
						numOffDecode++;
						numOffDensity++;
					}
					
					double bandwidthPiUsed = (offDecode)*capture.getBandwidth() + (offDensity - offDecode)*decode.getBandwidth() + (numChainRequest - offDensity)*density.getBandwidth();
					double cpuPiUsed = numChainRequest*capture.getCpu_pi() + (numChainRequest - offDecode)*decode.getCpu_pi() + (numChainRequest - offDensity)*density.getCpu_pi();
					
					
					/* check Pi resource pool ************************************************/
					if(bandwidthPiUsed > pi.getRemainBandwidth()) {
						if (listChainRequest.size() <= numChainSuccessCur || numChainSuccessCur > 0) { // prevent system continue loop even final result has been selected
							break MAP_LOOP;
						} else {
							//numChain --;
							doneFlag = false;
						}
					} else if (cpuPiUsed > pi.getRemainCPU())
						continue;
					else {

						double powerPiTemp = numChainRequest*capture.getPower() +(numChainRequest - offDecode)*decode.getPower() + (numChainRequest - offDensity)*density.getPower();
						double powerServerTemp = calculatePseudoPowerServer(numChainRequest*receive.getCpu_server() + offDecode*decode.getCpu_server() + offDensity*density.getCpu_server());
						double totalPowerTemp = powerPiTemp + powerServerTemp;
						
						if (numChainRequest >= numChainSuccessCur && totalPowerTemp <= minPower) { //QoS and acceptance rate priority
							numChainSuccessCur = numChainRequest;
							minPower = totalPowerTemp;
							doneFlag = true; // used to break MAP_LOOP


//							}
							//<------calculate number of VNF migration
//							if(remapPi == true) {
//								for(int i = 0; i < numChainBefore; i++) {
//									numVNFinServerAfter += listSFCTemp.get(i).numServiceInServer();
//								}
//								numVNFMigration += Math.abs(numVNFinServerAfter - numVNFinServerBefore);
//							}
							//<------add to listSFC output
							listSFCFinalPi.clear();
							for(int index = 0; index < listSFCTemp.size(); index++) {
								listSFCFinalPi.add(listSFCTemp.get(index));
							}
		
						} else {
							System.out.println("Maploop has gone so wrong, stop! \n");
							if (offDecode == numChainRequest) { // last loop
								break MAP_LOOP;
							}
							continue;
						}
					}
				}	 // OFF_DENSITY LOOP
			} // OFF_DECODE LOOP
			
			if(doneFlag == false) {
				listChainRequest.removeLast(); // remove last object
				if(listChainRequest.size() <= 0) {
					break MAP_LOOP;
				} else continue;
			}
		}
	}
	
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
		 // Creating a log file
        PrintStream out = null;
		try {
			out = new PrintStream(new File("./Plot/output.txt"));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
        System.setOut(out); 

		
		LinkedList<Double> totalPowerSystemConsolidation = new LinkedList<Double>();
		LinkedList<Double> totalPowerSystem = new LinkedList<Double>();
		LinkedList<Double> totalPowerPerSFC = new LinkedList<Double>();
		LinkedList<Double> serverUtilization = new LinkedList<Double>();
		LinkedList<Double> totalChainAcceptance = new LinkedList<Double>();
		LinkedList<Double> totalPiAcceptance = new LinkedList<Double>();
		LinkedList<Integer> listServerUsed = new LinkedList<Integer>();
		LinkedList<Integer> totalChainSystem = new LinkedList<Integer>();
		LinkedList<Integer> totalChainActive = new LinkedList<Integer>();
		LinkedList<Integer> totalDecOffload = new LinkedList<Integer>();
		LinkedList<Integer> totalDenOffload = new LinkedList<Integer>();
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
		
		Map<Integer, LinkedList<Integer>> listRequestForEachPi = new HashMap<>();
		Map<Integer, LinkedList<Integer>> listLeaveForEachPi = new HashMap<>();
		Map<Integer, LinkedList<Integer>> listOffForEachPi_temp = new HashMap<>();
		Map<Integer, LinkedList<Integer>> listOffForEachPi = new HashMap<>();
		Map<Integer, LinkedList<Double>> listCPUForEachPi = new HashMap<>();
		Map<Integer, LinkedList<Double>> listBWForEachPi = new HashMap<>();
		
		for(int i = 0; i < NUM_PI; i++) {
			listRequestForEachPi.put(i,new LinkedList<Integer>());
			listLeaveForEachPi.put(i,new LinkedList<Integer>());
			listOffForEachPi_temp.put(i, new LinkedList<Integer>());
			listOffForEachPi.put(i, new LinkedList<Integer>());
			listCPUForEachPi.put(i, new LinkedList<Double>());
			listBWForEachPi.put(i, new LinkedList<Double>());
		}
		
	
		double acceptance = 0;
		double acceptancePi = 0;
		int requestIndex = 0; // number of request
	
		
		LinkedList<Integer> requestRandomReceive = new LinkedList<>();
		
		//<------REQUEST_LOOP
		while (requestIndex < timeWindow.size()) { 
			HashMap<Integer,LinkedList<Double>> listRequestPi = listRequest.get(requestIndex);
			int numPiReceive = 0; // number of Pi receives request > 0
			int piAccept = 0;
			int numSFCReqThisTW = 0;
			int numMapReqThisTW = 0;
						LinkedList<Double> loadEdgeNumPi = new LinkedList<>();
			LinkedList<Double> bwEdgeNumPi = new LinkedList<>();			
		
			for (Entry<Integer, LinkedList<Double>> entry : listRequestPi.entrySet()) { 
				Rpi pi = listRpi.get(entry.getKey());
				int numSFCReqThisPi = entry.getValue().size();
				int numSFCLevThisPi = 0;
				listRequestForEachPi.get(pi.getId()).add(numSFCReqThisPi);
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
							numSFCLevThisPi ++;
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
							BLMapping(topo, mappingServer, pi, listSFCRemapLeave, listCurSFCOnPi, listSFCFinalPi);
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
				listLeaveForEachPi.get(pi.getId()).add(numSFCLevThisPi);
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
					BLMapping(topo, mappingServer, pi, listEndTime, listCurSFCOnPi, listSFCFinalPi);
				}
				
				//<-----Only successful mapping can jump into the below condition
				if(listSFCFinalPi.size() != 0 && pi.isOverload() == false && listEndTime.size()!= 0) { // case Pi mapping is success
					
					mappingServer.runMapping(listSFCFinalPi, topo);
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
					piAccept ++; //num of accepted Pi (accept if at least 1 SFC has been mapped
				}
				
				//<----Every mapping step has to do this
				loadEdgeNumPi.add(pi.getUsedCPU());
				bwEdgeNumPi.add(pi.getUsedBandwidth());		
				int offServiceCur = 0;
				for(SFC sfc : listCurSFCOnPi) {
					if(sfc.getService(2).getBelongToEdge() == false)
						offServiceCur ++;
					if(sfc.getService(3).getBelongToEdge() == false)
						offServiceCur ++;
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
			//<--------calculate average bandwidth usage
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
			totalPowerSystem.add( mappingServer.getPower() + mappingServer.PowerEdgeUsage() + NUM_PI*1.28);
			
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
			cpuEdgeUsagePerSFC.add(mappingServer.cpuEdgePerSFC());
			cpuServerUsagePerSFC.add(mappingServer.cpuServerPerSFC());
			requestIndex++;
		} // end while loop (request)
		
		try {
			write_double("./PlotBL-FF/totalPiAcceptance.txt",totalPiAcceptance);
			write_double("./PlotBL-FF/capacity.txt",capacity);
			write_double("./PlotBL-FF/capacityEdge.txt",capacityEdge);
			write_double("./PlotBL-FF/capacityCloud.txt",capacityCloud);
			write_double("./PlotBL-FF/averageBWUsage.txt",averageBWUsage);
			write_double("./PlotBL-FF/totalPowerSystemConsolidation.txt",totalPowerSystemConsolidation);
			write_double("./PlotBL-FF/listLinkUsage.txt",listLinkUsage);
			write_double("./PlotBL-FF/cpuEdgeUsagePerSFC.txt",cpuEdgeUsagePerSFC);
			write_double("./PlotBL-FF/cpuServerUsagePerSFC.txt",cpuServerUsagePerSFC);
			write_double("./PlotBL-FF/serverUtilization.txt",serverUtilization);
			write_integer("./PlotBL-FF/NumVNFMigration.txt",listVNFmigration);
			write_integer("./PlotBL-FF/totalChainLeave.txt",totalChainLeave);
			write_integer("./PlotBL-FF/listServerUsed.txt",listServerUsed);
			write_integer("./PlotBL-FF/requestRandom.txt",requestRandomReceive);
			write_integer("./PlotBL-FF/totalDecOffload.txt",totalDecOffload);
			write_integer("./PlotBL-FF/totalDenOffload.txt",totalDenOffload);
			write_double("./PlotBL-FF/totalPowerSystem.txt",totalPowerSystem);
			write_double("./PlotBL-FF/totalPowerSystemPerSFC.txt",totalPowerPerSFC);
			write_double("./PlotBL-FF/totalLoadEdge.txt",totalLoadEdge);
			write_double("./PlotBL-FF/totalBwEdge.txt",totalBwEdge);
			write_double("./PlotBL-FF/totalChainAcceptance.txt",totalChainAcceptance);
			write_integer("./PlotBL-FF/totalChainSystem.txt",totalChainSystem);
			write_integer("./PlotBL-FF/totalChainActive.txt",totalChainActive);
			write_integer("./PlotBL-FF/totalChainReject.txt",totalChainReject);
			write_excel("./PlotBL-FF/requestEachPiDetail.xlsx",listRequestForEachPi);
			write_excel("./PlotBL-FF/leaveEachPiDetail.xlsx",listLeaveForEachPi);
			write_excel("./PlotBL-FF/offSerEachPi01.xlsx",listOffForEachPi);
			write_excel_double("./PlotBL-FF/cpuEachPi01.xlsx",listCPUForEachPi);
			write_excel_double("./PlotBL-FF/bwEachPi01.xlsx",listBWForEachPi);

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