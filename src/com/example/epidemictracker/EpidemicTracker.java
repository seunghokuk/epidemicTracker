package com.example.epidemictracker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class EpidemicTracker extends Activity {

    private static final String TAG = "epi_Service";

    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format
    
    public Button forceStop, deleteLog, restartProcess;
    public TextView recordWarning, sensingWarning;
 
    private String pathStr;
    
    private double inputRate;
    private String strText;
    private EditText editText1;
    
    private Intent as, accs;
    
    private static Thread thread;
    private static Boolean isRunning;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_epidemic_tracker);

//        Permanent self-announcing bluetooth setting
//        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//		discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
//	  	startActivity(discoverableIntent);
        
	    setButtonHandlers();
        enableButtons(false);
        
        int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING); 
	 
        forceStop = (Button)findViewById(R.id.btnForceStop);
        deleteLog = (Button)findViewById(R.id.btnDellLog);
        restartProcess = (Button)findViewById(R.id.btnRestart);
        recordWarning = (TextView)findViewById(R.id.recordWarning);
        sensingWarning = (TextView)findViewById(R.id.sensingWarning);
        processList();
        as = new Intent(this, ActionService.class); // Action service intent
        accs = new Intent(this, SensoryService.class);
                        
        recordWarning.setText("CurrentState = Non-recording");
        recordWarning.setTextColor(Color.GRAY);
        recordWarning.setTypeface(null, Typeface.NORMAL);
        
        sensingWarning.setText("CurrentState = Collecting");
        sensingWarning.setTextColor(Color.BLUE);
        sensingWarning.setTypeface(null, Typeface.BOLD);
        
        startThread();
    	
        if(isRunning == true){
			restartProcess.setEnabled(false);
		}
        
        restartProcess.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
//				strText = editText1.getText().toString() ;
//		        inputRate = Double.parseDouble(strText);
		        
				startThread();
				sensingWarning.setText("CurrentState = Collecting");
				sensingWarning.setTextColor(Color.BLUE);
		        sensingWarning.setTypeface(null, Typeface.BOLD);

		        if(isRunning == true){
					restartProcess.setEnabled(false);
				}
//				Toast.makeText(getApplicationContext(), "Storing is restarted", Toast.LENGTH_SHORT).show();
			}
		});
        
        forceStop.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				isRunning = false;
				
				while(isMyServiceRunning(ActionService.class)|| isMyServiceRunning(SensoryService.class))
				{
					stopService(as);
					stopService(accs);
				}
				
				sensingWarning.setText("CurrentState = Non-collecting");
				sensingWarning.setTextColor(Color.GRAY);
				sensingWarning.setTypeface(null, Typeface.NORMAL);
				
				if(isRunning == false){
					restartProcess.setEnabled(true);
				}
//				Toast.makeText(getApplicationContext(), "Storing is suspended", Toast.LENGTH_SHORT).show();
			}
		});
        
        deleteLog.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String isSdcard = Environment.getExternalStorageState();
				File path = null;
				if (!isSdcard.equals(Environment.MEDIA_MOUNTED)) {
					path = Environment.getRootDirectory();
				} else {
					path = Environment.getExternalStorageDirectory();
				}
				pathStr = path.getAbsolutePath() + "/trace";
		        
				File filebt = new File(pathStr + "/" + "log_bt.txt");
				File filecell = new File(pathStr + "/" + "log_cell.txt");
				File fileloc = new File(pathStr + "/" + "log_loc.txt");
				File filesens = new File(pathStr + "/" + "log_sensors.txt");
				File filewifi = new File(pathStr + "/" + "log_wifi.txt");
				File filevoice = new File(pathStr + "/" + "log_voice.pcm");
				
			    filebt.delete();
			    filecell.delete();
			    fileloc.delete();
			    filesens.delete();
			    filewifi.delete();
			    filevoice.delete();
				
				Toast.makeText(getApplicationContext(), "Deletion completed", Toast.LENGTH_SHORT).show();
			}
		});
    }
    
    private void processList(){
        
        ActivityManager am = (ActivityManager)getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appList = am.getRunningAppProcesses();
 
        for(int i=0; i<appList.size(); i++){
            ActivityManager.RunningAppProcessInfo rapi = appList.get(i);
            Log.d(TAG,"run Process Package Name : " + rapi.processName);
        }
 
    }

    private void startThread() {
    	isRunning = true;

    	long intervalTime = (long) (1000.0 / inputRate); // Hz to second  
//    	System.out.println("interval: \t"+ intervalTime);
    	//Sensor Service
    	Timer t_sen = new Timer();
    	t_sen.scheduleAtFixedRate(new TimerTask(){
    		@Override
    		public void run(){
//     			long start = System.currentTimeMillis();
    			if(isRunning == true)
    			{
//    				startService(as); //for GPS, WIFI, BLUETOOTH, etc.
    				startService(accs); //for Sensors
    			}
    			else
    				this.cancel();
    		}
    	}, 0, 100);
    }

    private void setButtonHandlers() {
        ((Button) findViewById(R.id.btnStart)).setOnClickListener(btnClick);
        ((Button) findViewById(R.id.btnStop)).setOnClickListener(btnClick);
    }

    private void enableButton(int id, boolean isEnable) {
        ((Button) findViewById(id)).setEnabled(isEnable);
    }

    private void enableButtons(boolean isRecording) {
        enableButton(R.id.btnStart, !isRecording);
        enableButton(R.id.btnStop, isRecording);
    }
    

    private void startRecording() {
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        recorder.startRecording();
        isRecording = true;
       
        recordingThread = new Thread(new Runnable() {
            public void run() {
            	writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

        //convert short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    private void writeAudioDataToFile() {
        // Write the output audio in byte

        String filePath = "/sdcard/trace/log_voice.pcm"; 
        short sData[] = new short[BufferElements2Rec];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        long total = 0;
        
        while (isRecording) {
            // gets the voice output from microphone to byte format
            recorder.read(sData, 0, BufferElements2Rec);

            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                byte bData[] = short2byte(sData);
                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
    }
    private View.OnClickListener btnClick = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.btnStart: {
                enableButtons(true);
                recordWarning.setText("CurrentState = Recording");
                recordWarning.setTextColor(Color.RED);
                recordWarning.setTypeface(null, Typeface.BOLD);
                startRecording();
     
                
                break;
            }
            case R.id.btnStop: {
            	recordWarning.setText("CurrentState = Non-recording");
            	recordWarning.setTextColor(Color.GRAY);
            	recordWarning.setTypeface(null, Typeface.NORMAL);
                enableButtons(false);
                stopRecording();
                break;
            }
            }
        }
    };
    
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    public void onDestroy(){
        Log.d(TAG,"onDestroy()");
        super.onDestroy();
    }
    
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG,"onConfig()");
    }
    
    

}
