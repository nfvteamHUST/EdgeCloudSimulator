/**
* @author EdgeCloudTeam-HUST
*
* @date 
*/
package fil.algorithm.RESCEFF;

import java.util.Iterator;
import java.util.LinkedList;

import fil.resource.virtual.SFC;
import fil.resource.virtual.Topology;

public class MappingServer {
	private double power;
	private ServiceMapping serviceMapping;
	private LinkMapping linkMapping;
	private boolean isSuccess;
	private LinkedList<SFC> listSFC;
	private LinkedList<SFC> listSFCTotal;
	//private Topology topo;
	
	public MappingServer() {
		serviceMapping = new ServiceMapping();
		linkMapping = new LinkMapping();
		this.setPower(0);
		isSuccess = false;
		listSFC = new LinkedList<SFC>();
		setListSFCTotal(new LinkedList<SFC>());
	}
	
	public void runMapping(LinkedList<SFC> listSFC,  Topology topo) {
		this.listSFC.clear();
		this.serviceMapping.setVNFmigration(0);
		LinkedList<SFC> listSFCMap = serviceMapping.run(listSFC, topo);

		if(!listSFCMap.isEmpty()) { // at least a sfc has been mapped
			linkMapping.linkMapExternal(topo, listSFCMap, serviceMapping); // outside link mapping
			
			LinkedList<SFC> listInternalSFC = new LinkedList<>();
			for(SFC sfc : listSFCMap) {
				if(sfc.isSeparateService() == true) {
					listInternalSFC.add(sfc);
				}
			}
			LinkedList<SFC> listFailedSFC = new LinkedList<>();
			
			if(!listInternalSFC.isEmpty()) { // inside link exists
				listFailedSFC = linkMapping.linkMapInternal(topo, listInternalSFC, serviceMapping);	
			}
			for(SFC sfc : listFailedSFC) { // remove SFC fails internal-link map
				if(listSFCMap.contains(sfc))
					listSFCMap.remove(sfc);
			}
			this.setListSFC(listSFCMap);
			
			for(SFC sfc : listSFCMap) { // add to list totalSFC for consolidation
				if(!this.listSFCTotal.contains(sfc))
					this.listSFCTotal.add(sfc);
			}
			
		} else {
			System.out.println("Mapping on server returns zero success.");
		}
		this.setPower(serviceMapping.getPowerServer(topo) + linkMapping.getPower(topo));
	}
	
	
	

	public double getPower() {
		return power;
	}

	public void setPower(double power) {
		this.power = power;
	}

	public boolean isSuccess() {
		return isSuccess;
	}

	public void setSuccess(boolean isSuccess) {
		this.isSuccess = isSuccess;
	}

	public LinkMapping getLinkMapping() {
		return linkMapping;
	}

	public void setLinkMapping(LinkMapping linkMapping) {
		this.linkMapping = linkMapping;
	}

	public ServiceMapping getServiceMapping() {
		return serviceMapping;
	}

	public void setServiceMapping(ServiceMapping serviceMapping) {
		this.serviceMapping = serviceMapping;
	}

	public LinkedList<SFC> getListSFC() {
		return listSFC;
	}

	public void setListSFC(LinkedList<SFC> listSFC) {
		this.listSFC.clear();
		for(int i = 0; i < listSFC.size(); i++) {
			this.listSFC.add(listSFC.get(i));
		}
	}
	public Double PowerEdgeUsage() { //for checking
		Double power = 0.0;
		for(SFC sfc : this.listSFCTotal) {
			power += sfc.powerEdgeUsage();
		}
		return power;
	}
	public Double cpuEdgeAllSFC() {
		Double cpu = 0.0;
		for(SFC sfc : this.listSFCTotal) {
			cpu += sfc.cpuEdgeUsage();
		}
		return cpu;
	}
	public Double cpuServerAllSFC() {
		Double cpu = 0.0;
		for(SFC sfc : this.listSFCTotal) {
			cpu += sfc.cpuServerUsage();
		}
		return cpu;
	}
	public Double cpuEdgePerSFC() {
		Double cpuRatio = 0.0;
		for(SFC sfc : this.listSFCTotal) {
			cpuRatio += sfc.cpuEdgeUsage();
		}
		cpuRatio = cpuRatio/(1.0*this.listSFCTotal.size());
		return cpuRatio*1.0/100;
	}
	public Double cpuServerPerSFC() {
		Double cpuRatio = 0.0;
		for(SFC sfc : this.listSFCTotal) {
			cpuRatio += sfc.cpuServerUsage();
		}
		cpuRatio = cpuRatio/(1.0*this.listSFCTotal.size());
		return cpuRatio*1.0/100;
	}
	public Double linkUsagePerSFC() {
		Double linkRatio = 0.0;
		for(SFC sfc : this.listSFCTotal) {
			linkRatio += sfc.bandwidthUsageInDC() + sfc.bandwidthUsageOutDC();
		}
		return linkRatio/(1.0*this.listSFCTotal.size());
	}
	public void resetMappingServer() {
		this.serviceMapping = new ServiceMapping();
		this.linkMapping = new LinkMapping();
		this.setPower(0);
		this.isSuccess = false;
		this.listSFC = new LinkedList<SFC>();
		this.setListSFCTotal(new LinkedList<SFC>());
	}
	public int getNumVNFMigration() {
		int numVNF = this.serviceMapping.getVNFmigration();
		return numVNF;
	}
	public LinkedList<SFC> getListSFCTotal() {
		return listSFCTotal;
	}

	public void setListSFCTotal(LinkedList<SFC> listSFCTotal) {
		this.listSFCTotal = listSFCTotal;
	}
}
