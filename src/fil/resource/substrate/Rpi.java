package fil.resource.substrate;


import java.util.LinkedList;
import fil.resource.virtual.*;
public class Rpi {
	final static double CPU = 100;
	final static double BW = 100;
	final static double CPU_TH = 80;
	final static double BASELINE = 1.28;
	private int id;
	private double remainCPU;
	private double usedCPU;
	private double usedBandwidth;
	private double remainBandwidth;
	private double currentPower;
	private double cpuTH;
	private int cluster;
	private boolean overload;
	//private String name;
	private LinkedList<Service> listService;
	
	public Rpi(int id, int position) {
//		this.setRemainBandwidth();
		//this.setRemainCPU();
		setCluster(position);
		cpuTH = CPU_TH;
		//this.listService = new LinkedList<>();
		this.setOverload(false);
		this.setId(id); 
		this.usedCPU = 0;
		this.usedBandwidth = 0;
		this.remainCPU = CPU;
		this.currentPower = BASELINE;
		this.remainBandwidth = BW;
//		this.setUsedBandwidth(0);
//		this.setCurrentPower(1.28);
		//this.setName(name);
	}
	public Rpi(double state, LinkedList<Service> listService) {
//		this.setRemainBandwidth();
//		this.setRemainCPU();
		this.setListService(listService);
	}
//	public String getName() {
//		return name;
//	}
//	public void setName(String name) {
//		this.name = name;
//	}
	
	public LinkedList<Service> getListService() {
		return listService;
	}
	public void setListService(LinkedList<Service> listService) {
		this.listService = listService;
	}
	public void addService(Service service) {
		this.listService.add(service);
	}
	
	public void removeService(Service service) {
		if(this.listService.contains(service))
			this.listService.remove(service);
	}
	
	public double getRemainCPU() {
		return this.remainCPU;
	}

	public double getRemainBandwidth() {
		return this.remainBandwidth;
	}
//	public void setRemainBandwidth() {
//		this.remainBandwidth = BW - this.usedBandwidth;
//	}
	public double getCurrentPower() {
		return this.currentPower;
	}
	public void setCurrentPower(double currentPower) {
		this.currentPower += currentPower;
	}
	public double getUsedCPU() {
		return this.usedCPU;
	}
	public void setUsedCPU(double usedCPU) {
		this.usedCPU += usedCPU;
		this.remainCPU -= usedCPU;
	}
	public void reset() {
		this.usedCPU = 0;
		this.remainCPU = CPU;
		this.remainBandwidth = BW;
		this.usedBandwidth = 0;
		this.currentPower = BASELINE;
		this.overload = false;
	}
	public double getUsedBandwidth() {
		return usedBandwidth;
	}
	public void setUsedBandwidth(double usedBandwidth) {
		this.usedBandwidth += usedBandwidth;
		this.remainBandwidth -= usedBandwidth;
	}
	public double getCpu_threshold() {
		return this.cpuTH;
	}
	public int getCluster() {
		return cluster;
	}
	public void setCluster(int cluster) {
		this.cluster = cluster;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public boolean isOverload() {
		return overload;
	}
	public void setOverload(boolean overload) {
		this.overload = overload;
	}
}
