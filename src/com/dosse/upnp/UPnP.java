/*
 * Copyright (C) 2015 Federico Dossena (adolfintel.com).
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package com.dosse.upnp;

import java.util.List;
import java.util.ArrayList;

/**
 * This class contains static methods that allow quick access to UPnP Port Mapping.<br>
 * Commands will be sent to the default gateway.
 *
 * @author Federico
 */
public class UPnP {

    private static Gateway defaultGW = null;
    private static List<Gateway> gateways = new ArrayList<>();
    private static final GatewayFinder finder = new GatewayFinder() {
	@Override
	public void gatewayFound(Gateway g) {
	    synchronized (finder) {
		gateways.add(g);
		if (defaultGW == null) {
		    defaultGW = g;
		}
	    }
	}
    };


    /**
      * Sets the gateway to use for port mapping
      *
      * @param usn identifier of gateway device
      * @return <code>true</code> if gateway device with given usn is found, <code>false</code> otherwise
      */
    public static boolean setGatewayByID(String usn) {
	waitInit();
	for (Gateway gw : gateways) {
	    if (gw.getGatewayDeviceID().equals(usn)) {
		defaultGW = gw;
		return true;
	    }
	}
	return false;
    }


    /**
     * Is there an UPnP gateway?<br>
     * All UPnP commands will fail if UPnP is not available
     *
     * @return true if available, false if not
     */
    public static boolean isUPnPAvailable(){
	waitInit();
	return defaultGW!=null;
    }

    /**
     * Waits for UPnP to be initialized (takes ~3 seconds).<br>
     * It is not necessary to call this method manually before using UPnP functions
     */
    public static void waitInit() {
	while (finder.isSearching()) {  // GatewayListener Thread is alive
	    try {
		Thread.sleep(1);
	    } catch (InterruptedException ex) {
	    }
	}
    }

    public static String getGatewayDeviceID() {
	if(!isUPnPAvailable()) return null;
	return defaultGW.getGatewayDeviceID();
    }

    public static int getMappingErrCode() {
	return defaultGW.getMappingErrCode();
    }

    /**
     * Opens a port on the gateway
     *
     * @param port to map (0-65535)
     * @param protcol to use (TCP or UDP)
     * @param leaseDuration amount of seconds this mapping is valid. Use 0 for unlimited duration.
     * @return true if the operation was successful, false otherwise
     */
    public static boolean mapPort(int port, final String protocol, int leaseDuration, String description) {
	if(!isUPnPAvailable()) return false;
	if (!protocol.equals("UDP") && !protocol.equals("TCP")) {
	    return false;
	}
	return defaultGW.openPort(port, protocol, leaseDuration, description);
    }


    /**
     * Closes a port on the gateway
     *
     * @param port to map (0-65535)
     * @param protcol to use (TCP or UDP)
     * @return true if the operation was successful, false otherwise
     */
    public static boolean closePort(int port, String protocol) {
	if(!isUPnPAvailable()) return false;
	if (!protocol.equals("UDP") && !protocol.equals("TCP")) {
	    return false;
	}
	return defaultGW.closePort(port, protocol);
    }


    /**
     * Checks if port is mapped
     *
     * @param port to map (0-65535)
     * @param protcol to use (TCP or UDP)
     * @return true if the port is mapped, false otherwise
     */
    public static boolean isMapped(int port, String protocol) {
	if(!isUPnPAvailable()) return false;
	if (!protocol.equals("UDP") && !protocol.equals("TCP")) {
	    return false;
	}
	return defaultGW.isMapped(port, protocol);
    }


    /**
     * Gets the external IP address of the default gateway
     *
     * @return external IP address as string, or null if not available
     */
    public static String getExternalIP(){
	if(!isUPnPAvailable()) return null;
	return defaultGW.getExternalIP();
    }


    /**
     * Gets the internal IP address of this machine
     *
     * @return internal IP address as string, or null if not available
     */
    public static String getLocalIP(){
	if(!isUPnPAvailable()) return null;
	return defaultGW.getLocalIP();
    }

}
