package com.example.epidemictracker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthLte;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.ArrayAdapter;

public class ActionService extends Service{
   private static final String TAG = "epi_Service";
   
   private LocationManager locationManager;
   private LocationListener locationListener;
   private WifiManager wifiManager;
   private BluetoothAdapter bluetooth;
   private TelephonyManager telephonyManager;
   private PowerManager powerManager;
   private String pathStr;
   private String lastKnownLocation;
   private ArrayAdapter<String> mArrayAdapter;
   private ArrayList<String> mBt = new ArrayList<String>();
   private int mRunningCounter;

   File f_cell, f_wifi, f_loc, f_bt, f_sense;
   FileWriter fw_cell, fw_wifi, fw_loc, fw_bt, fw_sense;
   String str_ind, str_cell, str_wifi, str_loc, str_bt, str_sense;
   int type;
   boolean isGPSEnabled, isNetworkEnabled, isScanning, isWriting;
   String str;
   
   @Override
   public void onCreate() {
      mRunningCounter = 0;
      super.onCreate();
//      startForeground(1, new Notification());
      
      wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
      locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
      telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
      bluetooth = BluetoothAdapter.getDefaultAdapter();
      bluetooth.enable();
//      IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//      registerReceiver(mReceiver, filter);
      mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_checked, mBt);
      powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
      PowerManager.WakeLock wl = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
      wl.acquire();
      lastKnownLocation = "0"+"\t"+"0"+"\t"+"0"+"\t"+"0"+"\t"+"0";
      
      isScanning = false;      
      isWriting = true;
      
      
      String isSdcard = Environment.getExternalStorageState();
      File path = null;
      if (!isSdcard.equals(Environment.MEDIA_MOUNTED)) {
         path = Environment.getRootDirectory();
      } else {
         path = Environment.getExternalStorageDirectory();
      }
      pathStr = path.getAbsolutePath() + "/trace";
      path = new File(pathStr);

      if (!path.exists()) {
         if (!path.mkdirs()) {
            Log.d(TAG, "Directory is not created." + pathStr);
         }
      }
      Log.d(TAG, "Created directory is " + pathStr);
      
      locationListener = new LocationListener() {
         @Override
         public void onLocationChanged(Location location) {
            try {
               if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
                  lastKnownLocation = location.getTime() +"\t" + location.getLongitude() + "\t" + location.getLatitude() + "\t"
                        + location.getAltitude() + "\t" + location.getAccuracy();

               } else {
                  lastKnownLocation = location.getTime() +"\t" + location.getLongitude() + "\t" + location.getLatitude() + "\t"
                        + location.getAltitude() + "\t" + location.getAccuracy();
               }
            } catch (Exception e) {
               Log.d(TAG, "onLocationChanged: " + e.getMessage());
            }
         }

         @Override
         public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
         }

         @Override
         public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
         }

         @Override
         public void onStatusChanged(String provider, int status,
               Bundle extras) {
            // TODO Auto-generated method stub
         }
      };      

      isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
      isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
      
      if (!isGPSEnabled && !isNetworkEnabled) {
         str = "-1";
      } 
      else 
      {
         locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
         locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
      }

      f_cell = new File(pathStr, "log_cell.txt");
      f_wifi = new File(pathStr, "log_wifi.txt");
      f_loc = new File(pathStr, "log_loc.txt");
      f_bt = new File(pathStr, "log_bt.txt");
      
      try {
         if (!f_cell.exists())
            f_cell.createNewFile();
         if (!f_wifi.exists())
            f_wifi.createNewFile();
         if (!f_loc.exists())
            f_loc.createNewFile();
         if (!f_bt.exists())
            f_bt.createNewFile();
      } catch (IOException e) {
         e.printStackTrace();
      }
//      20170205 
//      wifiManager.startScan();
//      bluetooth.startDiscovery();   
   }

   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {
	   startForeground(1, new Notification());
      mRunningCounter++;
      Log.d(TAG, "run(), " + String.valueOf(mRunningCounter));

      long now = System.currentTimeMillis();
      Date date = new Date(now);
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd\tHH:mm:ss");
      str_ind = String.valueOf(mRunningCounter) + "\t" + sdf.format(date) + "\t" + now + "\t";
      try {
         
         if(isWriting==true)
         {
        	 str_loc = str_ind + getLocationInfoCollector() + "\n";
        	 str_cell = str_ind + getCellInfoCollector() + "\n";
        	 str_wifi = str_ind + getWifiInfoCollector() + "\n";
        	 str_bt = str_ind + getBTInfoCollector() + "\n";

        	 fw_loc = new FileWriter(f_loc, true);
        	 fw_cell = new FileWriter(f_cell, true);
             fw_wifi = new FileWriter(f_wifi, true);
             fw_bt = new FileWriter(f_bt, true);
         }
         else
         {
   
         }
       
      } catch (Exception e) {
         Log.d(TAG, "run(), getting information failed " + e.getMessage());
         e.printStackTrace();
      }
                    
      try {
    	   if(isWriting==true)
           {
    		   
    		   fw_loc.append(str_loc);
    		   fw_cell.append(str_cell);
    		   fw_wifi.append(str_wifi);
    		   fw_bt.append(str_bt);
    		   
    		   str_ind = null;
    		   str_loc = null;
    		   str_cell = null;
    		   str_wifi = null;
    		   str_bt = null;

    		   fw_loc.close();
    		   fw_cell.close();
    		   fw_wifi.close();
    		   fw_bt.close();
           }
    	   else
    	   {
    		   
    	   }
       
      } catch (IOException e) {
         Log.d(TAG, "run(), IOException " + e.getMessage());
         e.printStackTrace();
      }

      return START_STICKY;
   }

   @Override
   public IBinder onBind(Intent intent) {
      // TODO: Return the communication channel to the service.
      throw new UnsupportedOperationException("Not yet implemented");
   }

   private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
      public void onReceive(Context context, Intent intent) {
         String action = intent.getAction();
         if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
            mArrayAdapter.add(device.getName() + "\t" + device.getAddress() + "\t" + rssi);
         }
      }
   };

   private String getBTInfoCollector() {
      str = ""+ mArrayAdapter.getCount() + "\t";
     
      bluetooth.cancelDiscovery();
      
      if (!bluetooth.isEnabled()) {
         bluetooth.enable();
      }
      
      for (int i = 0; i < mArrayAdapter.getCount(); i++) {
         str += mArrayAdapter.getItem(i) + "\t";
      }
      
      mArrayAdapter.clear();
      
      if(isScanning == true)
      bluetooth.startDiscovery();   
      
      return str;
   }

   private String getCellInfoCollector() {
      str="";
      int cnt=0;
      int RSRP;

      try{
         for (CellInfo info : telephonyManager.getAllCellInfo()) {
             if (info instanceof CellInfoLte && cnt <= 0 )
             {
            	 cnt++;
            	 CellSignalStrengthLte lte = ((CellInfoLte) info).getCellSignalStrength();
                 CellIdentityLte cellIdentity = ((CellInfoLte) info).getCellIdentity();
                 
                 RSRP =  lte.getDbm();
//                     if(lte.getAsuLevel() == 97)
//                     {
//                        RSRP = RSRP / 10;
//                     }
                 str += ""+ lte.getAsuLevel()+"\t"+RSRP+ "\t" + cellIdentity.getCi()+"\t"+cellIdentity.getMcc()+"\t"+cellIdentity.getMnc()+"\t"+cellIdentity.getPci()+ "\t"+cellIdentity.getTac()+"\t";
             }
         }
     } catch (Exception e) {
         Log.e(TAG, "Unable to obtain cell signal information", e);
     }
      return str;
   }
   
   private String getWifiInfoCollector() {
      str = "";

      if (!wifiManager.isWifiEnabled()) {
         wifiManager.setWifiEnabled(true);
      }
      
      List<ScanResult> mScanResults = wifiManager.getScanResults();
      if (mScanResults == null)
      {
         str = "-1\t";
      }
      else
      {
         str = "" + mScanResults.size() + "\t";

         for (ScanResult results : mScanResults)
         {
            String temp;
            temp = results.SSID + "\t" + results.BSSID + "\t" + results.level + "\t" + results.frequency + "\t";
            
            if (temp != null)
               str += temp;
         }
      }
      if(isScanning == true)
      wifiManager.startScan();
      
      return str;         
   }

   private String getLocationInfoCollector()
   {
      str = lastKnownLocation;

      return str;
   }
   
}