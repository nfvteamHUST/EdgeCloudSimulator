package fil.algorithm.RESCELL;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import fil.resource.substrate.*;
import fil.resource.virtual.*;


public class ServiceMappingBackUp {
	public LinkedList<SFC> listSFCTotal;
	private boolean isSuccess;
	private LinkedList<SFC> needLinkMapping;
	private Map<Integer, PhysicalServer> listServerUsed;
	private int numService;
	private Topology topo;
	
	public ServiceMappingBackUp() {
		isSuccess = false;
		listSFCTotal = new LinkedList<SFC>();
		listServerUsed = new HashMap<>();
		needLinkMapping = new LinkedList<>();
		//needLinkMappingCopy = new HashMap<>();
		setNumService(0);
		topo = new Topology();
	}
	
	public LinkedList<SFC> run(LinkedList<SFC> listSFC, Topology topo, boolean leaving) {
		
		this.topo = topo;
		this.needLinkMapping = new LinkedList<>();
		Map<Integer, PhysicalServer> listPhysical = this.topo.getListPhyServers();  //get list physical server
		LinkedList<SFC> listRemainSFC = new LinkedList<>();  // save SFC not be mapped
		
		if(leaving == false)
			listSFCTotal.addAll(listSFC);
		
		LinkedList<SFC> listSFCTemp = new LinkedList<>();
		listSFCTemp.addAll(listSFC);

		LinkedList<SFC> listMappedSFCperRequest = new LinkedList<SFC>();
		LinkedList<SFC> listMappedSFC = new LinkedList<SFC>();
		
		LinkedList<PhysicalServer> listPhysicalTemp = new LinkedList<>();
		for(PhysicalServer phy : listPhysical.values()) {
			listPhysicalTemp.add(phy);
		}
		Random rand = new Random();
		//====Start Random algorithm to map VNF into physical server ===========//		
		while(true) {
			int serverIndex = rand.nextInt(listPhysicalTemp.size());
			PhysicalServer server = listPhysicalTemp.get(serverIndex);
			
			listMappedSFC.clear();
			listRemainSFC.clear();
			
			if(server.getUsedCPUServer() > 98.5) {
				continue;
			}
			
			while (!listSFCTemp.isEmpty()) {
				SFC sfc = listSFCTemp.get(0);
				//compare cpu chain with remain cpu server
				double cpuSFC = 0;
				int serviceCount = 0; // number of service inside SFC belongs to cloud
				for(int i = 4; i >= 2; i--) {
					if(!sfc.getService(i).getBelongToEdge()) {
						if(sfc.getService(i).getBelongToServer() == null) { // service hasnt been mapped to any server
							cpuSFC += sfc.getService(i).getCpu_server();  //calculate all cpu used by chain
							serviceCount++;
						}
					}
				}	
				
				if(cpuSFC <= server.getRemainCPU()) { // enough cpu to map
					listMappedSFCperRequest.add(sfc);
					System.out.println("sfc nam tren may " + server.getName() + " with CPU " + cpuSFC + " remain Cpu " + server.getRemainCPU());	
					if(!listMappedSFC.contains(sfc)) {
						listMappedSFC.add(sfc);
					}
					// add to list sfc belong to a server
					server.setUsedCPUServer(cpuSFC);
					setNumService(getNumService() + serviceCount); // total services run on cloud
					// set service position
					for(int i = 4; i >= 2; i--) {
						if(!sfc.getService(i).getBelongToEdge()) {
							if(sfc.getService(i).getBelongToServer() == null)
								sfc.getService(i).setBelongToServer(server);
						}
					}
					// set sfc position
					server.getListSFCInServer().add(sfc);
					// set independent service 
					//=== only Receive is mapped to cloud will be add to the below list ===//
					if(serviceCount == 1 && sfc.isSeparateService() == false) {
						server.getListIndependRev().add(sfc);
					}
					
					listSFCTemp.remove(0);
				} 
				else {
					if(!listRemainSFC.contains(sfc)){ // check of the current sfc already inside this list
						listRemainSFC.add(sfc); //list not mapping
					}
						listSFCTemp.remove(0);
						continue; //Kien-fix 17-12-2019
				}
				
			} // end listSFC loop
			
			//=== execute SFC that cannot map into the current server ===//

			if(!listRemainSFC.isEmpty()) {				
				for(SFC sfcRemain : listRemainSFC) {
					double cpuService = 0;
					if(sfcRemain.getService(3).getBelongToEdge()) {
						continue; // if only service 4 cannot map then it cannot be separated
					}
					for(int i = 4; i >= 2; i--) {
						if(!sfcRemain.getService(i).isBelongToEdge()) {
							cpuService = sfcRemain.getService(i).getCpu_server();  //calculate all cpu used by chain
							if(cpuService > server.getRemainCPU()) {
								continue;
							}
							else {
								setNumService(getNumService() + 1);
								server.setUsedCPUServer(cpuService);
								sfcRemain.getService(i).setBelongToServer(server);
								sfcRemain.setSeparateService(true);
								//listMappedSFC.add(sfcRemain);
								server.getListSFCInServer().add(sfcRemain);
								Map<Service, PhysicalServer> needLink = new HashMap<>();
								needLink.put(sfcRemain.getService(i), server);
								if (!needLinkMapping.contains(sfcRemain)) {
									needLinkMapping.add(sfcRemain);
								}
							}
						}
					}
				} // error here
				//===SFC that cannot mapped will be stored for next machine ===//
				listSFCTemp.clear();
				listSFCTemp.addAll(listRemainSFC);
			}// end separating loop

			
			if(listRemainSFC.isEmpty())
				break; // break when no sfc left
		} // end physical server loop
		
		if(!listMappedSFCperRequest.isEmpty())
			this.isSuccess = true;
		else
			this.isSuccess = false;
		
		return listMappedSFCperRequest;
	}
	
	public SubstrateSwitch getSwitchFromID(LinkedList<SubstrateSwitch> listSwitch, String id) {
		SubstrateSwitch s= new SubstrateSwitch();
		for(SubstrateSwitch sw: listSwitch)
			if(sw.getNameSubstrateSwitch().equals(id))
			{
				s= sw;
				break;
			}
		return s;
	}
	
	
public void resetRpiSFC(LinkedList<SFC> listSFC, Topology topo) {
				
		for(SFC sfc : listSFC) {
			
			double cpuSFC = 0;
			PhysicalServer server = null;
			boolean isSeparateSFC = sfc.isSeparateService();
			
			for(int i = 4; i >= 2; i--) {
				if(!sfc.getService(i).getBelongToEdge()) {
					//===return CPU===============================================//
					cpuSFC = sfc.getService(i).getCpu_server();
					server = sfc.getService(i).getBelongToServer();
					server.setUsedCPUServer(-cpuSFC); // return CPU	
					if(server.getUsedCPUServer() < 0)
						throw new java.lang.Error("CPU gets errors with CPU = " + server.getUsedCPUServer());
					if(server.getListSFCInServer().contains(sfc))
						server.getListSFCInServer().remove(sfc);
				}
			}
			//===return bandwidth for substratelink===============================//
			LinkedList<VirtualLink> listVirLink = sfc.getvLink();
			for(VirtualLink vLink : listVirLink) {
				double bandwidth = vLink.getBandwidthRequest();
				LinkedList<LinkPhyEdge> listPhyEdge = vLink.getLinkPhyEdge();
				for(LinkPhyEdge linkEdge : listPhyEdge) {
					linkEdge.setBandwidth(linkEdge.getBandwidth() + bandwidth);
					if(linkEdge.getBandwidth() < 0 || (linkEdge.getBandwidth() > 1024))
							throw new java.lang.Error("return link problem at bandwidth.");
				}
				LinkedList<SubstrateLink> listSubstrate = vLink.getLinkSubstrate();
				for(SubstrateLink linkSubstrate : listSubstrate) {
					System.out.println("bandwidth before is " + linkSubstrate.getBandwidth());
					linkSubstrate.setBandwidth(linkSubstrate.getBandwidth() + bandwidth);
					if(linkSubstrate.getBandwidth() < 0 || (linkSubstrate.getBandwidth() > 1024))
						throw new java.lang.Error("return link problem at bandwidth." + linkSubstrate.getBandwidth());
				}
			}
			
			//===remove sfc index inside all kind of list=========================//
			if(isSeparateSFC == false) {
				if(server.getListIndependRev().contains(sfc)) { // delete independent receive (if any)
					server.getListIndependRev().remove(sfc);
				}
			}
		}
	}
	
public boolean remappingAggrFarGroup(VirtualLink vLink) {
		
		boolean isSuccess = false;
		//===remapping if 2 service connect through aggr/core switch=========//
		Service sService = vLink.getsService();
		Service dService = vLink.getdService();
		
		PhysicalServer phyA = sService.getBelongToServer();
		PhysicalServer phyB = dService.getBelongToServer();
				
		LinkedList<SFC> listSFCA = phyA.getListSFCInServer();
		LinkedList<SFC> listSFCB = phyB.getListSFCInServer();
						
		SFC sfcA = null; // sfc contains source service
		SFC sfcB = null; // sfc contains destination service
		
		for(SFC sfc: listSFCA) {
			if(sfc.getSfcID() == sService.getSfcID()) {
				sfcA = sfc;
			}
		}
		for(SFC sfc: listSFCB) {
			if(sfc.getSfcID() == sService.getSfcID()) {
				sfcB = sfc;
			}
		}
		
		if (sfcA == null || sfcB == null) { // check null pointer
			throw new java.lang.Error("null list SFCA and SFCB");
		}
		
		//===get all the independent receive in server A & B ===================//
		int numReceiveA = 0;		
		numReceiveA = phyA.getListIndependRev().size();	

		//=======================================================================//
		
		//===get CPU demand for migrating dService to sService===================//
		
		double cpuDemand = dService.getCpu_server();
		double cpuReceive = 5.0;
		
		if((numReceiveA)*cpuReceive >= cpuDemand) { //neu demand nho hon so receive doc lap thi tien hanh chuyen
			double numReceiveEvacuate = Math.ceil(cpuDemand/cpuReceive);

			LinkedList<SFC> sfcEvacuate = new LinkedList<>();
			int sizeOfSFCEva = sfcEvacuate.size();
			
			for(int index = 0; index < numReceiveEvacuate; index++) {
				SFC sfc = phyA.getListIndependRev().getFirst();
				sfc.getService(4).setBelongToServer(null);
				sfcEvacuate.add(sfc);
				listSFCA.remove(sfc);
				phyA.getListIndependRev().remove(sfc);
			}
			
			LinkedList<SFC> listSFCMap = run(sfcEvacuate, this.topo, true);
			if(listSFCMap.size() < sizeOfSFCEva) { // return everything
				isSuccess = false;
				returnSFC(listSFCMap);
			}
			else {
				phyA.setUsedCPUServer(-(numReceiveEvacuate)*cpuReceive + cpuDemand);
				phyB.setUsedCPUServer(-cpuDemand);
				if(phyA.getUsedCPUServer() < 0 || phyB.getUsedCPUServer() < 0) 
					throw new java.lang.Error("cpuServer has wrong value.");
				dService.setBelongToServer(sService.getBelongToServer());
				if(sfcA.allServiceInSameServer() == true) {
					sfcA.setSeparateService(false);
					listSFCB.remove(sfcB);
				}
				isSuccess = true;
			}
			
		}
		else {
			isSuccess = false;
		}
		return isSuccess;
	}

	public void returnSFC(LinkedList<SFC> listSFC) {
		for(SFC sfc : listSFC) {
			double cpu;
			PhysicalServer server = null;
			for(int i = 4; i >= 2; i --) {
				if(!sfc.getService(i).isBelongToEdge()) {
					cpu = sfc.getService(i).getCpu_server();
					server = sfc.getService(i).getBelongToServer();
					server.setUsedCPUServer(-cpu);
					if(server.getUsedCPUServer() < 0)
						throw new java.lang.Error("Error at returningSFC, CPU problem.");
				}
				else
					break;
			}
			if(server.getListSFCInServer().contains(sfc))
				server.getListSFCInServer().remove(sfc);
			if(server.getListIndependRev().contains(sfc))
				server.getListIndependRev().remove(sfc);
		}
	}

	public HashMap<Integer, PhysicalServer> sortListServer(HashMap<Integer, PhysicalServer> list) {
		
		//create a list from elements of HashMap
		LinkedList<Map.Entry<Integer, PhysicalServer> > listMap = new LinkedList<Map.Entry<Integer, PhysicalServer>>(list.entrySet());
		
		//sort the list
		Collections.sort(listMap, new Comparator<Map.Entry<Integer, PhysicalServer>>() {
			@Override
			public int compare(Map.Entry<Integer, PhysicalServer> o1, Map.Entry<Integer, PhysicalServer> o2) {
				if (o1.getValue().getUsedCPUServer() > o2.getValue().getUsedCPUServer()) {
					return -1;
				}
				if (o1.getValue().getUsedCPUServer() < o2.getValue().getUsedCPUServer()) {
					return 1;
				}
				return 0;
			}
		});
		
		//put data from sorted list to HashMap
		HashMap<Integer, PhysicalServer> temp = new LinkedHashMap<Integer, PhysicalServer>();
		for(Map.Entry<Integer, PhysicalServer> aa : listMap) {
			temp.put(aa.getKey(), aa.getValue());
		}
		return temp;
	}


	public boolean isSuccess() {
		return isSuccess;
	}

	public void setSuccess(boolean isSuccess) {
		this.isSuccess = isSuccess;
	}

	public double getPowerServer() {
		double power = 0;
		Map<Integer, PhysicalServer> listPhysical = this.topo.getListPhyServers(); 
		for(PhysicalServer phy : listPhysical.values()) {
			if(phy.getState() == 1) {
				phy.setPowerServer();
				power += phy.getPowerServer();;
			}
			else
				continue;
		}
		return power;
	}


	public Map<Integer, PhysicalServer> getListServerUsed() {
		return listServerUsed;
	}

	public void setListServerUsed(Map<Integer, PhysicalServer> listServerUsed) {
		this.listServerUsed = listServerUsed;
	}


	public LinkedList<SFC> getNeedLinkMapping() {
		return needLinkMapping;
	}

	public void setNeedLinkMapping(LinkedList<SFC> needLinkMapping) {
		this.needLinkMapping = needLinkMapping;
	}

	public double getCpuUtilization() {
		double cpuServer = 0;
		for(PhysicalServer phy : listServerUsed.values()) {
			cpuServer += phy.getUsedCPUServer();
		}
		if(listServerUsed.size() == 0) throw new java.lang.Error(" Used server = 0 ");
		double utilization = cpuServer/(listServerUsed.size()*100);
		return utilization;
	}
	
	public int getServerUsed() {
		return listServerUsed.size();
	}


	public LinkedList<SFC> getTotalSFCServer() {
		return listSFCTotal;
	}

	public void setTotalSFCServer(LinkedList<SFC> totalSFCServer) {
		this.listSFCTotal = totalSFCServer;
	}

	public int getNumService() {
		return numService;
	}

	public void setNumService(int numService) {
		this.numService = numService;
	}
}
