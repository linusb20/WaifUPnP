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

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringWriter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.NodeIterator;

/**
 *
 * @author Federico
 */
public class Gateway {

    private String serviceType = null, controlURL = null;
    private String usn;
    private String friendlyName;
    private InetAddress localAddress;
    private InetAddress deviceAddress;
    private int mappingErrCode = 0;


    // Only for debugging purposes. Can be removed later
    private static void printXML(Document d) {
	TransformerFactory tf = TransformerFactory.newInstance();
	Transformer transformer;
	try {
	    transformer = tf.newTransformer();
	    StringWriter writer = new StringWriter();
	    transformer.transform(new DOMSource(d), new StreamResult(writer));
	    String xmlString = writer.getBuffer().toString();
	    System.out.println(xmlString);
	} catch (Exception e) {}
    }

    public Gateway(byte[] data, InetAddress ip, InetAddress devAddr) throws Exception {
	localAddress = ip;
	deviceAddress = devAddr;
	String location = null;
	StringTokenizer st = new StringTokenizer(new String(data), "\n");
	while (st.hasMoreTokens()) {
	    String s = st.nextToken().trim();
	    if (s.isEmpty() || s.startsWith("HTTP/1.") || s.startsWith("NOTIFY *")) {
		continue;
	    }
	    String name = s.substring(0, s.indexOf(':')), val = s.length() >= name.length() ? s.substring(name.length() + 1).trim() : null;
	    if (name.equalsIgnoreCase("location")) {
		location = val;  // location = URL for UPnP description for root device
	    }
	    if (name.equalsIgnoreCase("usn")) {
		usn = val;  // gatewayDeviceID = USN of device, val could be null
	    }

	}
	if (location == null) {
	    throw new Exception("Unsupported Gateway");
	}
	Document d;
	d = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(location);
	// printXML(d);
	NodeList friendlyName = d.getElementsByTagName("friendlyName");
	this.friendlyName = friendlyName.item(0).getTextContent();
	NodeList services = d.getElementsByTagName("service");
	for (int i = 0; i < services.getLength(); i++) {
	    Node service = services.item(i);
	    NodeList n = service.getChildNodes();
	    String serviceType = null, controlURL = null;
	    for (int j = 0; j < n.getLength(); j++) {
		Node x = n.item(j);
		if (x.getNodeName().trim().equalsIgnoreCase("serviceType")) {
		    serviceType = x.getFirstChild().getNodeValue();
		} else if (x.getNodeName().trim().equalsIgnoreCase("controlURL")) {
		    controlURL = x.getFirstChild().getNodeValue();
		}
	    }
	    if (serviceType == null || controlURL == null) {
		continue;
	    }
	    if (serviceType.trim().toLowerCase().contains(":wanipconnection:") || serviceType.trim().toLowerCase().contains(":wanpppconnection:")) {
		this.serviceType = serviceType.trim();
		this.controlURL = controlURL.trim();
	    }
	}
	if (controlURL == null) {
	    throw new Exception("Unsupported Gateway");
	}
	int slash = location.indexOf("/", 7); //finds first slash after http://
	if (slash == -1) {
	    throw new Exception("Unsupported Gateway");
	}
	location = location.substring(0, slash);
	if (!controlURL.startsWith("/")) {
	    controlURL = "/" + controlURL;
	}
	controlURL = location + controlURL;
    }

    private Map<String, String> command(String action, Map<String, String> params) throws Exception {
	Map<String, String> ret = new HashMap<String, String>();
	String soap = "<?xml version=\"1.0\"?>\r\n" + "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" SOAP-ENV:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">"
		+ "<SOAP-ENV:Body>"
		+ "<m:" + action + " xmlns:m=\"" + serviceType + "\">";
	if (params != null) {
	    for (Map.Entry<String, String> entry : params.entrySet()) {
		soap += "<" + entry.getKey() + ">" + entry.getValue() + "</" + entry.getKey() + ">";
	    }
	}
	soap += "</m:" + action + "></SOAP-ENV:Body></SOAP-ENV:Envelope>";
	byte[] req = soap.getBytes();
	HttpURLConnection conn = (HttpURLConnection) new URL(controlURL).openConnection();
	conn.setRequestMethod("POST");
	conn.setDoOutput(true);
	conn.setRequestProperty("Content-Type", "text/xml");
	conn.setRequestProperty("SOAPAction", "\"" + serviceType + "#" + action + "\"");
	conn.setRequestProperty("Connection", "Close");
	conn.setRequestProperty("Content-Length", "" + req.length);
	conn.getOutputStream().write(req);
	Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(conn.getInputStream());
	NodeIterator iter = ((DocumentTraversal) d).createNodeIterator(d.getDocumentElement(), NodeFilter.SHOW_ELEMENT, null, true);
	Node n;
	while ((n = iter.nextNode()) != null) {
	    try {
		if (n.getFirstChild().getNodeType() == Node.TEXT_NODE) {
		    ret.put(n.getNodeName(), n.getTextContent());
		}
	    } catch (Throwable t) {
	    }
	}
	conn.disconnect();
	return ret;
    }

    public int getMappingErrCode() {
	return mappingErrCode;
    }

    public InetAddress getLocalAddress() {
	return localAddress;
    }

    public InetAddress getDeviceAddress() {
	return deviceAddress;
    }

    public String getFriendlyName() {
	return friendlyName;
    }

    public String getUSN() {
	return usn;
    }

    public boolean isConnected() {
	try {
	    Map<String, String> r = command("GetStatusInfo", null);
	    String connectionStatus = r.get("NewConnectionStatus");
	    if (connectionStatus != null
		&& connectionStatus.equalsIgnoreCase("Connected")) {
		return true;
	    }
	    return false;
	} catch (Throwable t) {
	    return false;
	}
    }

    public String getExternalIP() {
	try {
	    Map<String, String> r = command("GetExternalIPAddress", null);
	    return r.get("NewExternalIPAddress");
	} catch (Throwable t) {
	    return null;
	}
    }

    public boolean openPort(int port, String protocol, int leaseDuration, String description) {
	if (port < 0 || port > 65535) {
	    throw new IllegalArgumentException("Invalid port");
	}
	Map<String, String> params = new HashMap<String, String>();
	params.put("NewRemoteHost", "");
	params.put("NewProtocol", protocol);
	params.put("NewInternalClient", localAddress.getHostAddress());
	params.put("NewExternalPort", "" + port);
	params.put("NewInternalPort", "" + port);
	params.put("NewEnabled", "1");
	params.put("NewPortMappingDescription", description);
	params.put("NewLeaseDuration", String.valueOf(leaseDuration));
	try {
	    Map<String, String> r = command("AddPortMapping", params);
	    if (r.get("errorCode") == null) return true;
	    mappingErrCode = Integer.parseInt(r.get("errorCode"));
	    return false;
	} catch (Exception ex) {
	    return false;
	}
    }

    public boolean closePort(int port, String protocol) {
	if (port < 0 || port > 65535) {
	    throw new IllegalArgumentException("Invalid port");
	}
	Map<String, String> params = new HashMap<String, String>();
	params.put("NewRemoteHost", "");
	params.put("NewProtocol", protocol);
	params.put("NewExternalPort", "" + port);
	try {
	    command("DeletePortMapping", params);
	    return true;
	} catch (Exception ex) {
	    return false;
	}
    }

    public boolean isMapped(int port, String protocol) {
	if (port < 0 || port > 65535) {
	    throw new IllegalArgumentException("Invalid port");
	}
	Map<String, String> params = new HashMap<String, String>();
	params.put("NewRemoteHost", "");
	params.put("NewProtocol", protocol);
	params.put("NewExternalPort", "" + port);
	try {
	    Map<String, String> r = command("GetSpecificPortMappingEntry", params);
	    if (r.get("errorCode") != null) {
		throw new Exception();
	    }
	    return r.get("NewInternalPort") != null;
	} catch (Exception ex) {
	    return false;
	}

    }

}
