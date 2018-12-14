package com.example.epidemictracker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;


public class SensoryService extends Service implements SensorEventListener {
	private float accX, accY, accZ;
	private float gyroX, gyroY, gyroZ;
	private float gravX, gravY, gravZ;
	private float linaccX, linaccY, linaccZ;
	private float magX, magY, magZ;
	private float rotvecX, rotvecY, rotvecZ, rotvecM;
	private float lightX;
	private float pressX;
	private float proxX;
	private SensorManager sensorManager;
	private String pathStr;
	private static final String TAG = "epi_Service";
	private int mRunningCounter;
	private static Thread thread;
	private float gravity[] = new float[3];
	private float magnetic[] = new float[3];
	private float revMagnetic[] = new float[3];
	int type;
	File f_sense;
	FileWriter fw_sense;
	String str_ind, str_sense;
	String str;
	
	@Override
	public void onCreate() {
		super.onCreate();
		  mRunningCounter = 0;
	      startForeground(1, new Notification());

	      sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
	      
	      sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
	            SensorManager.SENSOR_DELAY_GAME);
	      sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
	            SensorManager.SENSOR_DELAY_GAME);
	      sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
	            SensorManager.SENSOR_DELAY_GAME);
	      sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
	            SensorManager.SENSOR_DELAY_GAME);
	      sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
	            SensorManager.SENSOR_DELAY_GAME);
	      sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
	            SensorManager.SENSOR_DELAY_GAME);
	      sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),
	            SensorManager.SENSOR_DELAY_GAME);
	      sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE),
	            SensorManager.SENSOR_DELAY_GAME);
	      sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),
	            SensorManager.SENSOR_DELAY_GAME);
	     
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
	      
	      f_sense = new File(pathStr, "log_sensors.txt");
	      
	      try {
	         if (!f_sense.exists())
	            f_sense.createNewFile();
	      } catch (IOException e) {
	         e.printStackTrace();
	      }
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
        
        mRunningCounter++;
 		long now = System.currentTimeMillis();
 		Date date = new Date(now);
 		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd\tHH:mm:ss");
 		str_ind = String.valueOf(mRunningCounter) + "\t" + sdf.format(date) + "\t" + now + "\t";
 	
 		try {
 			str_sense = str_ind + getSensorsInfoCollector() + "\n";
 			fw_sense = new FileWriter(f_sense, true);
 	    } catch (Exception e) {
 	    	Log.d(TAG, "run(), getting information failed " + e.getMessage());
 	        e.printStackTrace();
 	    }
 		
 		try {
 			fw_sense.append(str_sense);
 			fw_sense.close();      
 		} catch (IOException e) {
 			Log.d(TAG, "run(), IOException " + e.getMessage());
 			e.printStackTrace();
 		}
 		
		return START_STICKY;
	}
	
   private String getSensorsInfoCollector() {
      str="";

      str += accX + "\t" + accY + "\t" + accZ + "\t" 
              + gyroX + "\t" + gyroY + "\t" + gyroZ + "\t"
              + gravX + "\t" + gravY + "\t" + gravZ + "\t" 
              + linaccX + "\t" + linaccY + "\t" + linaccZ + "\t" 
              + magX + "\t" + magY + "\t" + magZ + "\t"
              + revMagnetic[0] + "\t" + revMagnetic[1] + "\t" + revMagnetic[2] + "\t"
              + rotvecX + "\t" + rotvecY + "\t" + rotvecZ + "\t" 
              + lightX + "\t" 
              + pressX + "\t"
              + proxX;
//      str += magX + "\t" + magY + "\t" + magZ;
       
      return str;
   }
   
	
	@Override
	   public void onSensorChanged(SensorEvent event) {
	      // TODO Auto-generated method stub
	      type = event.sensor.getType();
	      switch(type)
	      {
	         case Sensor.TYPE_ACCELEROMETER:
	    	 accX = event.values[0];
	    	 accY = event.values[1];
	    	 accZ = event.values[2];
	         break;
	      
	         case Sensor.TYPE_GYROSCOPE:
	    	 gyroX = event.values[0];
	    	 gyroY = event.values[1];	
	    	 gyroZ = event.values[2];
	    	 break;
	    	 
	         case Sensor.TYPE_GRAVITY:
	         gravX = event.values[0];
	         gravY = event.values[1];   
	         gravZ = event.values[2];
	         gravity[0] = gravX;
	         gravity[1] = gravY;
	         gravity[2] = gravZ;
	         break;
	      
	         case Sensor.TYPE_LINEAR_ACCELERATION:
	         linaccX = event.values[0];
	         linaccY = event.values[1];
	         linaccZ = event.values[2];
	         break;
	         
	         case Sensor.TYPE_MAGNETIC_FIELD:
	         magX = event.values[0];
	         magY = event.values[1];
	         magZ = event.values[2];
	         magnetic[0] = magX;
	         magnetic[1] = magY;
	         magnetic[2] = magZ;
	         
	         float[] R = new float[9];
	         float[] I = new float[9];
	         SensorManager.getRotationMatrix(R, I, gravity, magnetic);
	         float [] A_D = event.values.clone();
	         float [] A_W = new float[3];
	         revMagnetic[0] = R[0] * A_D[0] + R[1] * A_D[1] + R[2] * A_D[2];
	         revMagnetic[1] = R[3] * A_D[0] + R[4] * A_D[1] + R[5] * A_D[2];
	         revMagnetic[2] = R[6] * A_D[0] + R[7] * A_D[1] + R[8] * A_D[2];
	         break;
	         
	         case Sensor.TYPE_ROTATION_VECTOR:
	         rotvecX = event.values[0];
	         rotvecY = event.values[1];
	         rotvecZ = event.values[2];
//	         rotvecM = event.values[3]; 
	         break;
	         
	         case Sensor.TYPE_LIGHT:
	         lightX = event.values[0];
	         break;
	         
	         case Sensor.TYPE_PRESSURE:
	         pressX = event.values[0];
	         break;
	         
	         case Sensor.TYPE_PROXIMITY:
	         proxX = event.values[0];
	         break;
	      }
	      
	   }
	   @Override
	   public void onAccuracyChanged(Sensor sensor, int accuracy) {
	      // TODO Auto-generated method stub

	   }

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
}