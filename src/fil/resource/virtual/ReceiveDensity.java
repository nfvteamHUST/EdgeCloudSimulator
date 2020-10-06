package fil.resource.virtual;

public class ReceiveDensity extends Service {
	public ReceiveDensity(String sfcID, int piBelong) {
		this.setSfcID(sfcID);
		this.setPiBelong(piBelong);
		this.setServiceType("receive");
		this.setCpu_pi(0); // CPU of capture in pi
		this.setCpu_server(5);
		this.setBelongToEdge(false);
	}
	public ReceiveDensity() {
		this.setServiceType("receive");
		this.setCpu_pi(0); // CPU of capture in pi
		this.setCpu_server(5);
		this.setBelongToEdge(false);
	}
}
