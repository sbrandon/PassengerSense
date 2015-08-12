package com.passengersense.wifiping;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;
import org.most.serverpost.PostJson;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class ScanWifiNetwork extends AsyncTask<Void, Void, Void>{
	
	private final String TAG = "ScanWifiNetwork";
    private long network_ip = 0;
    private long network_start = 0;
    private long network_end = 0;
    protected long size = 0;
    private NetInfo net = null;
    public String hardwareAddress = NetInfo.NOMAC;
    private final static int[] DPORTS = { 139, 445, 22, 80 };
    private final static int TIMEOUT_SCAN = 3600; // seconds
    private final static int TIMEOUT_SHUTDOWN = 10; // seconds
    private final static int THREADS = 10;
    private ExecutorService mPool;
    private int pt_move = 2; // 1=backward 2=forward
    private HashMap<String, Host> hosts = new HashMap<String, Host>();
    private HashMap<String, Host> previousHosts = new HashMap<String, Host>();
    private int scanNo = 0;
    private String ssid;
    private Context context;
    private boolean running;
    
    public ScanWifiNetwork(Context context){
    	this.running = true;
    	this.context = context;
    	net = new NetInfo(context);
    	ssid = net.ssid;
    	setNetworkInfo();
    }
	
    @SuppressWarnings("static-access")
	public void setNetworkInfo() {
        network_ip = net.getUnsignedLongFromIp(net.ip);
        int shift = (32 - net.cidr);
        if (net.cidr < 31) {
            network_start = (network_ip >> shift << shift) + 1;
            network_end = (network_start | ((1 << shift) - 1)) - 1;
        } else {
            network_start = (network_ip >> shift << shift);
            network_end = (network_start | ((1 << shift) - 1));
        }
    }
    
    private class CheckRunnable implements Runnable {
        private String addr;

        CheckRunnable(String addr) {
            this.addr = addr;
        }

        public void run() {
            try {
                InetAddress h = InetAddress.getByName(addr);
                hardwareAddress = HardwareAddress.getHardwareAddress(addr);
                if(!NetInfo.NOMAC.equals(hardwareAddress)){
                    //Log.e(TAG, "found using arp #1 "+addr);
                    addHost(addr, hardwareAddress);
                    return;
                }
                // Native InetAddress check
                if (h.isReachable(getRate())) {
                    //Log.e(TAG, "found using InetAddress ping "+addr);
                    addHost(addr, hardwareAddress);
                    return;
                }
                // ARP Check #2
                hardwareAddress = HardwareAddress.getHardwareAddress(addr);
                if(!NetInfo.NOMAC.equals(hardwareAddress)){
                    //Log.e(TAG, "found using arp #2 "+addr);
                    addHost(addr, hardwareAddress);
                    return;
                }
                // Custom check
                Socket s = new Socket();
                for (int i = 0; i < DPORTS.length; i++) {
                    try {
                        s.bind(null);
                        s.connect(new InetSocketAddress(addr, DPORTS[i]), getRate());
                        Log.v(TAG, "found using TCP connect "+addr+" on port=" + DPORTS[i]);
                    } catch (IOException e) {
                    } catch (IllegalArgumentException e) {
                    } finally {
                        try {
                            s.close();
                        } catch (Exception e){
                        }
                    }
                }

                hardwareAddress = HardwareAddress.getHardwareAddress(addr);
                if(!NetInfo.NOMAC.equals(hardwareAddress)){
                    //Log.e(TAG, "found using arp #3 "+addr);
                    addHost(addr, hardwareAddress);
                    return;
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } 
        }
    }
    
    public void addHost(String ipAddress, String hardwareAddress){
    	long unixTime = System.currentTimeMillis() / 1000L;
    	Host host = new Host();
    	host.setTimeStamp(unixTime);
    	host.setIp(ipAddress);
    	host.setMac(hardwareAddress);
    	host.setSsid(ssid);
    	storeHost(host);
    }
    
    public void storeHost(Host host){
    	/*
    	ContentValues values = new ContentValues();
    	values.put("timestamp", host.getTimeStamp());
    	values.put("ssid", host.getSsid());
    	values.put("ip", host.getIp());
    	values.put("mac", host.getMac());
    	DBManager.storeData(context, "WIFI_PING", values);
    	*/
    	hosts.put(host.getMac(), host);
    }
    
    protected void onPreExecute() {
        size = (int) (network_end - network_start + 1);
    }
    
    @SuppressWarnings("rawtypes")
	private void updateHosts(){
    	int newHosts = 0;
    	int totalHosts = hosts.size();
    	if(scanNo > 0){
    		Iterator it = hosts.entrySet().iterator();
    		while (it.hasNext()) {
    			Map.Entry pair = (Map.Entry)it.next();
    			String mac = (String) pair.getKey();
    			if(!previousHosts.containsKey(mac)){
    				newHosts++;
    			}
    		}
    	}
    	if(newHosts > 0 || totalHosts < previousHosts.size() || scanNo == 0){
    		postResults(totalHosts, newHosts);
    	}
    	previousHosts.clear();
    	previousHosts = new HashMap<String, Host>(hosts);
		hosts.clear();
    	scanNo++;
    }
    
    private void postResults(int totalHosts, int newHosts){
    	JSONObject obj = new JSONObject();
    	try {
			obj.put("TotalHostsWi-Fi", totalHosts);
			obj.put("NewHostsWi-Fi", newHosts);
			PostJson postJson = PostJson.getInstance(context);
			postJson.postJsonMeasurement(obj, "WIFI_PING", null);
		} catch (JSONException e) {
			Log.e(TAG, e.toString());
		}
    }
    
    protected Void doInBackground(Void... params) {
    	while(running){
    		task();
    		updateHosts();
    	}
        return null;
    }
    
    private void task(){
    	Log.v(TAG, "start=" + NetInfo.getIpFromLongUnsigned(network_start) + " (" + network_start
                + "), end=" + NetInfo.getIpFromLongUnsigned(network_end) + " (" + network_end
                + "), length=" + size);
        mPool = Executors.newFixedThreadPool(THREADS);
        if (network_ip <= network_end && network_ip >= network_start) {
            Log.i(TAG, "Back and forth scanning");
            // gateway
            launch(network_start);
            
            // hosts
            long pt_backward = network_ip;
            long pt_forward = network_ip + 1;
            long size_hosts = size - 1;

            for (int i = 0; i < size_hosts; i++) {
                // Set pointer if of limits
                if (pt_backward <= network_start) {
                    pt_move = 2;
                } else if (pt_forward > network_end) {
                    pt_move = 1;
                }
                // Move back and forth
                if (pt_move == 1) {
                    launch(pt_backward);
                    pt_backward--;
                    pt_move = 2;
                } else if (pt_move == 2) {
                    launch(pt_forward);
                    pt_forward++;
                    pt_move = 1;
                }
            }
            mPool.shutdown();
            try {
                if(!mPool.awaitTermination(TIMEOUT_SCAN, TimeUnit.SECONDS)){
                    mPool.shutdownNow();
                    Log.e(TAG, "Shutting down pool");
                    if(!mPool.awaitTermination(TIMEOUT_SHUTDOWN, TimeUnit.SECONDS)){
                        Log.e(TAG, "Pool did not terminate");
                    }
                }
            } catch (InterruptedException e){
                Log.e(TAG, e.getMessage());
                mPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private void launch(long i) {
    	if(!mPool.isShutdown()) {
            mPool.execute(new CheckRunnable(NetInfo.getIpFromLongUnsigned(i)));
        }
    }
    
    private int getRate() {
    	return 800; 
    }
    
    //Getters & Setters
    public void setRunning(boolean running){
    	this.running = running;
    }

}