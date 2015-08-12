package com.passengersense.wifiping;

import java.io.Serializable;

public class Host implements Serializable {

	private static final long serialVersionUID = -2352908108046569386L;
	private long timeStamp;
	private String ssid;
	private String ip;
	private String mac;
	
	public Host(){
		
	}
	
	//Getters & Setters
	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public String getSsid() {
		return ssid;
	}

	public void setSsid(String ssid) {
		this.ssid = ssid;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getMac() {
		return mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}
	
}
