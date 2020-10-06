package fil.resource.virtual;

import fil.resource.substrate.PhysicalServer;

/**
 * Builds Virtual Service
 * 
 * @author Van Huynh Nguyen
 *
 */
public class Service {

	private int piBelong;
	private String serviceType;
	private double cpu;
	private double bandwidth;
	private double power;
	private int requestID;
	private String sfcID;
	private boolean belongToEdge;
	private PhysicalServer belongToServer;
	private double cpu_pi;
	private double cpu_server;
	
	public Service() {
		this.cpu = 0;
		this.setRequestID(0);
		this.setBelongToEdge(true);
		this.setBelongToServer(null);
	}
	
	public double getCpu_pi() {
		return cpu_pi;
	}

	public void setCpu_pi(double cpu_pi) {
		this.cpu_pi = cpu_pi;
	}

	public double getCpu_server() {
		return cpu_server;
	}

	public void setCpu_server(double cpu_server) {
		this.cpu_server = cpu_server;
	}

	public double getCPU() {
		return cpu;
	}

	public void setCPU(double cPU) {
		cpu = cPU;
	}

	public int getRequestID() {
		return requestID;
	}

	public void setRequestID(int requestID) {
		this.requestID = requestID;
	}

	public boolean isBelongToEdge() {
		return belongToEdge;
	}

	public void setBelongToEdge(boolean belongToEdge) {
		this.belongToEdge = belongToEdge;
	}
	
	public boolean getBelongToEdge() {
		return belongToEdge;
	}
	public double getBandwidth() {
		return bandwidth;
	}

	public void setBandwidth(double bandwidth) {
		this.bandwidth = bandwidth;
	}

	public double getPower() {
		return power;
	}

	public void setPower(double power) {
		this.power = power;
	}

	public String getSfcID() {
		return sfcID;
	}

	public void setSfcID(String sfcID) {
		this.sfcID = sfcID;
	}

	public String getServiceType() {
		return serviceType;
	}

	public void setServiceType(String serviceType) {
		this.serviceType = serviceType;
	}

	public PhysicalServer getBelongToServer() {
		return belongToServer;
	}

	public void setBelongToServer(PhysicalServer belongToServer) {
		this.belongToServer = belongToServer;
	}

	public int getPiBelong() {
		return piBelong;
	}

	public void setPiBelong(int piBelong) {
		this.piBelong = piBelong;
	}

}
