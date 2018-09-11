package com.android.settings;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.internal.telephony.ITelephony;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Harish on 30/3/17.
 */
public class SettingsBootService extends Service {

    private static final String TAG = "SettingsBootService";
    private static final String RIMOE_PATTERN = "RE\\d+";
    private static final String RIMOY_PATTERN = "RY\\d+";
    private static final String DEVICE_RIMO_E = "rimoE";
    private static final String DEVICE_RIMO_Y = "rimoY";
    private String mCurrentDeviceName;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Writing service
        mCurrentDeviceName = Build.DEVICE;
        if (intent != null) {
            writeSerialNumber(this);//Serial number from nv value
            Log.d(TAG, "onStartCommand");
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public static String getSN(){
    	String SN = null;
        ITelephony mService = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
    	try {
    		SN = mService.getSN();
    		Log.d(TAG,"Serial number fetched as "+SN);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
    	if (SN == null)
    		SN = "";
        return SN;
    }

    private void writeSerialNumber(Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final File file = new File("/persist/rimo.prop");
                if (!file.exists()) {
                    writeSN(file, context, false);
                } else if (isFileEmpty(file)) {
                    writeSN(file, context, true);
                } else {
                    Log.d(TAG, "Rimo.prop already exists");
                }
            }
        }).start();
    }

    private boolean isFileEmpty(File file) {
       if (file.exists()) {
            FileReader fr = null;
            try {
                fr = new FileReader(file);
                if (fr.read() == -1) {
                    return true;
                } else {
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                 if (fr != null) {
                     try {
                         fr.close();
                     } catch (IOException e) {
                          e.printStackTrace();
                     }
                  }
           }
        }
        return false;
    }

    private String fetchSerialNumber(String serialNumber){
        Pattern pattern = Pattern.compile(RIMOE_PATTERN);
        if(DEVICE_RIMO_Y.equalsIgnoreCase(mCurrentDeviceName)){
            pattern = Pattern.compile(RIMOY_PATTERN);
        }
        if(pattern.matcher(serialNumber).matches())
            return serialNumber;
        Matcher matcher = pattern.matcher(serialNumber);
        if(matcher.find())
            return getSubpartSN(serialNumber);
        return serialNumber;
    }

    private String getSubpartSN(String sn){
        if(sn != null && sn.length() >= 111) {
            sn = sn.substring(101, 111);
        }
        return sn;
    }

    private boolean isValidSN(String serialNumber){
        if(DEVICE_RIMO_E.equalsIgnoreCase(mCurrentDeviceName)){
            Pattern pattern = Pattern.compile(RIMOE_PATTERN);
            return pattern.matcher(serialNumber).matches();
        }else if(DEVICE_RIMO_Y.equalsIgnoreCase(mCurrentDeviceName)){
            Pattern pattern = Pattern.compile(RIMOY_PATTERN);
            return pattern.matcher(serialNumber).matches();
        }
        Log.d(TAG, "Not a valid serial number " + serialNumber+" for "+mCurrentDeviceName);
        return false;
    }

    private void writeSN(File file, Context context, boolean hasFile) {
        FileWriter fileWriter = null;
        try {
            String snNumber = getSN();
            Log.d(TAG, "Serial number from telephony " + snNumber);
            snNumber = fetchSerialNumber(snNumber);
            Log.d(TAG, "Serial number Extracted as " + snNumber);
            if (snNumber != null && !snNumber.isEmpty() && isValidSN(snNumber)) {
                if (!hasFile) {
                Log.d(TAG, "creating new Rimo.prop file ");
                file.createNewFile();
                }
                fileWriter = new FileWriter(file);

                char [] array = snNumber.toCharArray();
                StringBuilder sNBuilder = new StringBuilder();
                for (int i=0,size=array.length;i<size;i++) {
                     if (Character.isLetterOrDigit(array[i])) {
                         sNBuilder.append(array[i]);
                     }
               }
               snNumber = sNBuilder.toString();
               Log.d(TAG, "Writing ro serial number " + snNumber);
               fileWriter.write("ro.serialno=" + snNumber+"\n");
            }
            //changePermissions(file.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void changePermissions(String filePath) {//644
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("chmod 644 " + filePath);
        try {
            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

