import com.dosse.upnp.UPnP;

public class Main {

    private static String USN = "";
    private static String PORT_MAPPING_DESCRIPTION = "Saros Socks5 TCP";

    public static void main(String args[]){
	// UPnP.setGatewayByID(USN);
	// System.out.println(UPnP.getGatewayDeviceID().equals(USN));
	// System.out.println(UPnP.isUPnPAvailable());

	boolean success = UPnP.mapPort(4137, "TCP", 0, PORT_MAPPING_DESCRIPTION);
	System.out.println("mapPort: " + success);
	try {
		Thread.sleep(2000);
	} catch (InterruptedException ex) {}
	success = UPnP.isMapped(4137, "TCP");
	System.out.println("isMapped: " + success);
	success = UPnP.closePort(4137, "TCP");
	System.out.println("closePort: " + success);
	success = UPnP.isMapped(4137, "TCP");
	System.out.println("isMapped: " + success);

	System.out.println(UPnP.getDeviceAddress());
	System.out.println(UPnP.getLocalAddress());
	System.out.println(UPnP.getExternalIPAddress());
	System.out.println(UPnP.getFriendlyName());
	System.out.println(UPnP.getGatewayDeviceID());
    }
}
