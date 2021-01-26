import com.dosse.upnp.Gateway;
import com.dosse.upnp.GatewayFinder;
import java.util.List;
import java.util.ArrayList;

public class Main {

    private static String PORT_MAPPING_DESCRIPTION = "Saros Socks5 TCP";

    public static void main(String args[]){
        List<Gateway> devices = new ArrayList<Gateway>();
        GatewayFinder finder = new GatewayFinder();
        devices = finder.discoverGateways();
        if (devices.isEmpty()) {
            System.out.println("No gateways found!");
            return;
        }
        Gateway gw = devices.get(0);
        int errCode = gw.openPort(4137, "TCP", 0, PORT_MAPPING_DESCRIPTION);
        System.out.println("mapPort: " + (errCode == 0)); 
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {}

        boolean success = gw.isMapped(4137, "TCP");
        System.out.println("isMapped: " + success);
        success = gw.closePort(4137, "TCP");
        System.out.println("closePort: " + success);
        success = gw.isMapped(4137, "TCP");
        System.out.println("isMapped: " + success);

        System.out.println("Device Address: " + gw.getDeviceAddress());
        System.out.println("LocalAddress: " + gw.getLocalAddress());
        System.out.println("External IP Address: " + gw.getExternalIP());
        System.out.println("Friendly Name: " + gw.getFriendlyName());
        System.out.println("USN: " + gw.getUSN());
    }
}
