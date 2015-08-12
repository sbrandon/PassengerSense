package org.most.pipeline;

public class PipelineWifiPing {
	
	//Table Name
	public static final String TBL_WIFI_PING = "WIFI_PING";
	//Table Fields
	public static final String FLD_TIMESTAMP = "timestamp";
	public static final String FLD_SSID = "ssid";
	public static final String FLD_IP = "ip";
	public static final String FLD_MAC = "mac";
	//Create String
	public static final String CREATE_WIFI_PING_TABLE = String.format("_ID INTEGER PRIMARY KEY, %s INT NOT NULL, %s TEXT NOT NULL, %s TEXT NOT NULL, %s TEXT NULL", FLD_TIMESTAMP, FLD_SSID, FLD_IP, FLD_MAC);
}
