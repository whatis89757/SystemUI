package com.android.server;


import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.content.Context;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.storage.StorageManager;
import android.os.SystemProperties;
import android.os.PowerManager;

import com.android.internal.util.MemInfoReader;

import android.service.persistentdata.PersistentDataBlockManager;
import android.telephony.TelephonyManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Intent;

import com.android.internal.util.MemInfoReader;
//import com.qualcomm.qcnvitems.QcNvItems;


public class HwMsgManager {
	AudioManager mAudioManager = null;
	Context mContext = null;
//	private QcNvItems mNv;
	private static final String FILENAME_PROC_CPUINFO = "/proc/cpuinfo";
	private static final String CONFIGPATH="/sys/class/android_usb/android0/enable";


	public static boolean isAudioLoop =false;
	private boolean loop1 = false;
	private boolean loop2 = false;
	private boolean loop3 = false;
	public void setContext(Context context){
		mContext =  context;
	}
	public  byte[] handleMsg(byte[] buffer){
		int CMD_LEN = 0;
        int CMD_CODE = 0;
        int SUBSYS_ID = 0;
        byte[] data = new byte[512];
        byte[] factoryData = new byte[512];
        String[] buffertochar = new String[512];
        
        String SUBSYS_CMD_CODE = "";
        int status = 0;
        int loopin = 0;
        int loopout =0;
        int volume = 10;
        int factoryindex =1;
        String info = bytes2HexString(buffer);
        if(info != null){
        	 Log.i("zhou", "info======"+info);
        }
        buffertochar = info.split(",");
        
        CMD_LEN = buffertochar.length;
        CMD_CODE = Integer.parseInt(buffertochar[0],16);
        SUBSYS_ID = Integer.parseInt(buffertochar[1],16);
        SUBSYS_CMD_CODE = buffertochar[3]+buffertochar[2];
        status = Integer.parseInt(buffertochar[4],16);
        loopin = Integer.parseInt(buffertochar[4],16);
        loopout =Integer.parseInt(buffertochar[5],16);
        Log.i("zhou", "CMD_LEN======"+CMD_LEN+"==CMD_CODE=="+CMD_CODE+"===SUBSYS_ID=="+SUBSYS_ID+"==SUBSYS_CMD_CODE=="+SUBSYS_CMD_CODE+"==status=="+status);
        data[0] = (byte)75;
        data[1] = (byte) 201;
        data[2] = (byte) Integer.parseInt(buffertochar[2], 16);
        data[3] = (byte) Integer.parseInt(buffertochar[3], 16);
        data[4] = (byte)0;
        for(int j = 5;j<31;j++){
        	data[j] = (byte)0;
        }
        if(buffertochar[2].toLowerCase().equals("a8") && buffertochar[3].toLowerCase().equals("ff")){
            mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        	isAudioLoop = true;
        }else{
        	isAudioLoop = false;
        }
        if(isAudioLoop){
        	/*if(mNv == null){
        		mNv = new QcNvItems(mContext);
        		rewriteOneByteOfNv2499(18, 'F');
        	}*/
            volume = Integer.parseInt(buffertochar[8],16);
        	audioMessage(loopout,loopin,volume);
        }else{
        	if(buffertochar[2].toLowerCase().equals("aa") && buffertochar[3].toLowerCase().equals("ff")){
        		factoryindex = Integer.parseInt(buffertochar[5],16);
        		String testChild = buffertochar[4].toLowerCase();
        		int offset = Integer.parseInt(buffertochar[6], 16);
        		int size = Integer.parseInt(buffertochar[7], 16);
        		String writebuff = "";
        		StringBuilder mBuilder = new StringBuilder("");
        		if(testChild.equals("16") /*&& (factoryindex==2)*/){
        			Log.i("zhou","buffertochar.length==="+buffertochar.length);
        			for(int i = 8;i<buffertochar.length;i++){
                		mBuilder.append(buffertochar[i]);
        			}
        			writebuff = mBuilder.toString();
        			Log.i("zhou","writebuff==="+writebuff);
        		}
        		
        		if(testChild.equals("11") || testChild.equals("12") || testChild.equals("13") ||
        				testChild.equals("14") || testChild.equals("15") || testChild.equals("16")){
        			Log.i("zhou","writebuff===kkk==="+writebuff);
        			factoryData = factoryTest(buffertochar[3],buffertochar[2],testChild,factoryindex,offset,size,writebuff);
        			if(factoryData !=null){
        				return factoryData;
        			}
        			
        		}
        	}else if(buffertochar[2].toLowerCase().equals("c1") && buffertochar[3].toLowerCase().equals("ff")){
        		if(loop1 && loop2 && loop3){
//        			rewriteOneByteOfNv2499(18, 'P');
        		}
        	}
        }
		return data;
	} 
	public  String bytes2HexString(byte[] b) {
		  String ret = "";
		  for (int i = 0; i < b.length; i++) {
		   String hex = Integer.toHexString(b[ i ] & 0xFF);
		   if (hex.length() == 1) {
		    hex = '0' + hex;
		   }
		   ret += hex.toUpperCase()+",";
		  }
		  ret = ret.substring(0, ret.length()-1);
		  return ret;
		}
	private void useAdbCommand(String command){  
	    try {  
	    	Process p = Runtime.getRuntime().exec(command);  
//	    	p.waitFor();
	    } catch (Exception e) {  
	            e.printStackTrace();  
	        }
	}  
	
	
	private void audioMessage(int loopout , int loopin , int volume){
    	int musicStreamMAXVolume = volume*10;
    Log.i("zhou","loopin=="+loopin+"====loopout=="+loopout+"===musicStreamMaxVolume==="+musicStreamMAXVolume);
    if(loopin == 3 && loopout == 0){
    	//耳机MIC->Spreaker
    	loop1 = true;
    	useAdbCommand("/system/bin/mm-audio-ftm -tc 18 -c /system/etc/ftm_test_config -d 60 -v "+musicStreamMAXVolume);
    }else if(loopin == 3 && loopout ==6){
    	//耳机MIC->耳机 不要求做
    }else if(loopin ==3 && loopout ==2){
    	//耳机MIC->听筒
    	loop2 = true;
    	useAdbCommand("/system/bin/mm-audio-ftm -tc 13 -c /system/etc/ftm_test_config -d 60 -v "+musicStreamMAXVolume);
    }else if(loopin ==0 && loopout == 1){
    	//主MIC ->耳机
    	loop3 = true;
    	useAdbCommand("/system/bin/mm-audio-ftm -tc 16 -c /system/etc/ftm_test_config -d 60 -v "+musicStreamMAXVolume);
    }else if(loopin ==1 && loopout ==1){
    	//副MIC -> 耳机
    	loop3 = true;
    	useAdbCommand("/system/bin/mm-audio-ftm -tc 16 -c /system/etc/ftm_test_config -d 60 -v "+musicStreamMAXVolume);
    }else if(loopin ==0 && loopout ==0){
    	//主MIC ->Spreak  不要求做
    	//延时6秒
    }else if(loopin == 0 && loopout == 2){
    	//主MIC ->听筒  不要求做
    	//延时6秒 
    }
	}
 /*   private void rewriteOneByteOfNv2499(int idx, char testResult) {
        if (mNv == null) {
            mNv = new QcNvItems(mContext);
        }
        byte[] bresult = null;
        try {
            bresult = mNv.getNvFactoryData3IByte();
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }

        if (bresult == null) {
            return;
        }

        bresult[idx] = (byte) testResult;
        Log.i("zhou", "bresult[idx]==="+bresult[idx]);
        try {
            mNv.setNvFactoryData3IByte(bresult);
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        }

    }*/
	private String readFile(String filePath) {
		String res = "";
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File(filePath))));
			String str = null;
			while ((str = br.readLine()) != null) {
				res += str;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	private String getDeviceProcessorInfo() {
		final String PROC_HARDWARE_REGEX = "Hardware\\s*:\\s*(.*)$"; /*
																	 * hardware
																	 * string
																	 */

		try {
			BufferedReader reader = new BufferedReader(new FileReader(
					FILENAME_PROC_CPUINFO));
			String cpuinfo;

			try {
				while (null != (cpuinfo = reader.readLine())) {
					if (cpuinfo.startsWith("Hardware")) {
						Matcher m = Pattern.compile(PROC_HARDWARE_REGEX)
								.matcher(cpuinfo);
						if (m.matches()) {
							return m.group(1);
						}
					}
				}
				return "Unknown";
			} finally {
				reader.close();
			}
		} catch (IOException e) {
			Log.e("zhou",
					"IO Exception when getting cpuinfo for Device Info screen",
					e);

			return "Unknown";
		}
	}

	private long getRAMMem() {
		long RamTotalsize = 0;
		MemInfoReader mMemInfoReader = new MemInfoReader();
		mMemInfoReader.readMemInfo();
		RamTotalsize = mMemInfoReader.getTotalSize();
		return RamTotalsize;
	}

	private long getROMMen() {
		long RomTotalsize = 0;
		RomTotalsize = Environment.getExternalStorageDirectory().getTotalSpace();
		return RomTotalsize;
	}

	public String getBatteryStatus() {
		String filePath = "sys/class/power_supply/battery/status";
		String str = null;
		str = readFile(filePath);
		str = str.trim();
		return str;
	}

	private int readBatteryVoltage() {
		String filePath = "sys/class/power_supply/battery/voltage_now";
		String str = null;
		str = readFile(filePath).trim();
		int voltage = Integer.valueOf(str);
		int finalvoltage = voltage / 1000;
		return finalvoltage;
	}

	public void gotoSleep() {
		PowerManager mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
		mPowerManager.goToSleep(SystemClock.uptimeMillis());
	}

	private boolean HasSIMOrUIMCard() {
		boolean flagSim1 = false;
		boolean flagSim2 = false;
		int stateSim1 = -1;
		int stateSim2 = -1;
		TelephonyManager tm = TelephonyManager.getDefault();
		if (tm.isMultiSimEnabled()) {
			stateSim1 = tm.getSimState(0);
			stateSim2 = tm.getSimState(1);
		} else {
			stateSim1 = tm.getSimState();
		}
		switch (stateSim1) {
		case TelephonyManager.SIM_STATE_ABSENT:
			flagSim1 = true;
			break;
		default:
			break;
		}

		switch (stateSim2) {
		case TelephonyManager.SIM_STATE_ABSENT:
			flagSim2 = true;
			break;
		default:
			break;
		}
		if (tm.isMultiSimEnabled()) {
			if (!flagSim2 || !flagSim1) {
				return true;
			} else {
				return false;
			}
		} else {
			if (!flagSim1) {
				return true;
			} else {
				return false;
			}
		}
	}

	private String getSDState() {
		StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
		return mStorageManager.getVolumeState(getSDPath());
	}

	private String getSDPath() {
		return "/storage/sdcard1";
	}

	private boolean exitsSD() {
		if (getSDState().equals(Environment.MEDIA_MOUNTED)) {
			return true;
		} else {
			return false;
		}
	}

	public boolean storeToFile(boolean open) {
		try {
			Log.i("zhou", "xxxxxxxxxxxxxxxxx");
			FileOutputStream outStream = new FileOutputStream(CONFIGPATH);
			String s = (open ? "1" : "0");
			outStream.write(s.getBytes());
			outStream.close();
			Log.i("zhou", "aaaaaaaaaaaaaaaa");
			return true;
		} catch (FileNotFoundException e) {
			Log.i("zhou", "bbbbbbbbbbbbb");
			e.printStackTrace();
		} catch (IOException e) {
			Log.i("zhou", "cccccccccccccc");
			e.printStackTrace();
		}
		return false;
	}

	public String getSoftVersion() {
		String SoftVerStr = "";
		SoftVerStr = SystemProperties.get("ro.build.display.id");
        return SoftVerStr;
	}

	public String getHardwareVersion() {
		String HardwareVerStr = "";
		HardwareVerStr = SystemProperties.get("ro.hw_version");
		return HardwareVerStr;
	}

	public String getPcbVersion() {
		String pcbVer = "";
		pcbVer = SystemProperties.get("ro.hw_version");
		return pcbVer;
	}
	private String getManufacturer(){
		String manufacturer = "";
		manufacturer = SystemProperties.get("ro.product.manufacturer");
		return manufacturer;
	}
	
	private int readpartition(int offset, int size, String readbuf){
		int readvalues = -1;
		PersistentDataBlockManager mDataBlockManager = (PersistentDataBlockManager) mContext
				.getSystemService(Context.PERSISTENT_DATA_BLOCK_SERVICE);
		readvalues = mDataBlockManager.readpartition(offset,size,readbuf);
		return readvalues;
	}
	private int writepartition(int offset, int size, String writedbuf){
		int writevalues = -1;
		PersistentDataBlockManager mDataBlockManager = (PersistentDataBlockManager) mContext
				.getSystemService(Context.PERSISTENT_DATA_BLOCK_SERVICE);
		writevalues = mDataBlockManager.writepartition(offset,size,writedbuf);
		return writevalues;
	}
	private void closepartition(){
		PersistentDataBlockManager mDataBlockManager = (PersistentDataBlockManager) mContext
				.getSystemService(Context.PERSISTENT_DATA_BLOCK_SERVICE);
		mDataBlockManager.closepartition();
	}
	
	private int openPartition(){
		int openvalues = -1;
		PersistentDataBlockManager mDataBlockManager = (PersistentDataBlockManager) mContext
				.getSystemService(Context.PERSISTENT_DATA_BLOCK_SERVICE);
		openvalues = mDataBlockManager.openPartition();
		return openvalues;
	}
	

	private void  restoreSystem(){
		 mContext.sendBroadcast(new Intent("android.intent.action.MASTER_CLEAR"));
	}
	private byte[] factoryTest(String CommandHead,String Commandend,String testChild ,int factoryindex,int offset ,int size ,String writebuff){
		byte[] data = new byte[512];
		byte[] finaldata = new byte[512];
		if(testChild.equals("11")){
	        String dataStr = "";
	        dataStr = "CPU:"+getDeviceProcessorInfo()+" ROM:"+getROMMen()+" RAM:"+getRAMMem();
	        Log.i("zhou","dataStr==="+dataStr.toString());
	        data=dataStr.getBytes();
	        for(int i=0;i<data.length;i++){
	        	finaldata[i+6]=data[i];
	        }
	        finaldata[0] = (byte)75;
	        finaldata[1] = (byte) 201;
	        finaldata[2] = (byte) Integer.parseInt(Commandend, 16);
	        finaldata[3] = (byte) Integer.parseInt(CommandHead, 16);
	        finaldata[4] = (byte) Integer.parseInt(testChild, 16);
	        finaldata[5] = (byte)1;
	        return finaldata;
	
		}else if(testChild.equals("12")){
			String dataStr = "";
			if(factoryindex ==1){
				finaldata[0] = (byte)75;
				finaldata[1] = (byte) 201;
				finaldata[2] = (byte) Integer.parseInt(Commandend, 16);
				finaldata[3] = (byte) Integer.parseInt(CommandHead, 16);
				finaldata[4] = (byte) Integer.parseInt(testChild, 16);
		        if(getBatteryStatus()!=null && getBatteryStatus().toLowerCase().equals("charging")){
		        	finaldata[5] = (byte)1;
		        }else{
		        	finaldata[5] =(byte)0;	        }
		        for(int i =6;i<512;i++){
		        	finaldata[i]= (byte)0;
		        }
		        return finaldata;
		        
			}else if(factoryindex ==2){
				int voltage=readBatteryVoltage();
				dataStr=""+voltage;
				 data=dataStr.getBytes();
			        for(int i=0;i<data.length;i++){
			        	finaldata[i+6]=data[i];
			        }
			        finaldata[0] = (byte)75;
			        finaldata[1] = (byte) 201;
			        finaldata[2] = (byte) Integer.parseInt(Commandend, 16);
			        finaldata[3] = (byte) Integer.parseInt(CommandHead, 16);
			        finaldata[4] = (byte) Integer.parseInt(testChild, 16);
			        finaldata[5] = (byte)1;
			        return finaldata;
			}else{
				finaldata[0] = (byte)75;
				finaldata[1] = (byte) 201;
				finaldata[2] = (byte) Integer.parseInt(Commandend, 16);
				finaldata[3] = (byte) Integer.parseInt(CommandHead, 16);
				finaldata[4] = (byte) Integer.parseInt(testChild, 16);
				finaldata[5] = (byte)1;
		        for(int i=6;i<512;i++){
		        	finaldata[i]=(byte)0;
		        }
				gotoSleep();
				return finaldata;
			}
		}else if(testChild.equals("13")){
			finaldata[0] = (byte)75;
	        finaldata[1] = (byte) 201;
	        finaldata[2] = (byte) Integer.parseInt(Commandend, 16);
	        finaldata[3] = (byte) Integer.parseInt(CommandHead, 16);
	        finaldata[4] = (byte) Integer.parseInt(testChild, 16);
	        finaldata[5] = (byte)1;
			restoreSystem();
			return finaldata;
		}else if(testChild.equals("14")){
			finaldata[0] = (byte)75;
			finaldata[1] = (byte) 201;
			finaldata[2] = (byte) Integer.parseInt(Commandend, 16);
			finaldata[3] = (byte) Integer.parseInt(CommandHead, 16);
			finaldata[4] = (byte) Integer.parseInt(testChild, 16);
			if(factoryindex ==1){
				if(HasSIMOrUIMCard()){
					finaldata[5] = (byte)1;
				}else{
					finaldata[5] = (byte)0;
				}
				for(int i=6;i<512;i++){
					finaldata[i]=(byte)0;
		        }
			}else if(factoryindex ==2){
				if(exitsSD()){
					finaldata[5] = (byte)1;
				}else{
					finaldata[5] = (byte)0;
				}
				for(int i=6;i<512;i++){
					finaldata[i]=(byte)0;;
		        }
			}else{
				finaldata[5] = (byte)1;
				for(int i=6;i<512;i++){
					finaldata[i]= (byte)0;
		        }
				storeToFile(false);	
			}
			return finaldata;
		}else if(testChild.equals("15")){
			String dataStr = "";
			if(factoryindex ==1){
				dataStr=getPcbVersion();
				data=dataStr.getBytes();
				for(int i=0;i<data.length;i++){
					finaldata[i+6]=data[i];
		        }
				finaldata[0] = (byte)75;
				finaldata[1] = (byte) 201;
				finaldata[2] = (byte) Integer.parseInt(Commandend, 16);
				finaldata[3] = (byte) Integer.parseInt(CommandHead, 16);
				finaldata[4] = (byte) Integer.parseInt(testChild, 16);
		        if(!dataStr.equals("")){
		        	finaldata[5] = (byte)1;
		        }else{
		        	finaldata[5] = (byte)0;
		        }
		        return finaldata;
			}else if(factoryindex ==2){
				dataStr=getHardwareVersion();
				data=dataStr.getBytes();
				for(int i=0;i<data.length;i++){
					finaldata[i+6]=data[i];
		        }
				finaldata[0] = (byte)75;
				finaldata[1] = (byte) 201;
				finaldata[2] = (byte) Integer.parseInt(Commandend, 16);
				finaldata[3] = (byte) Integer.parseInt(CommandHead, 16);
				finaldata[4] = (byte) Integer.parseInt(testChild, 16);
		        if(!dataStr.equals("")){
		        	finaldata[5] = (byte)1;
		        }else{
		        	finaldata[5] = (byte)0;
		        }
		        return finaldata;
			}else{
				dataStr=getSoftVersion();
				data=dataStr.getBytes();
				for(int i=0;i<data.length;i++){
					finaldata[i+6]=data[i];
		        }
				finaldata[0] = (byte)75;
				finaldata[1] = (byte) 201;
				finaldata[2] = (byte) Integer.parseInt(Commandend, 16);
				finaldata[3] = (byte) Integer.parseInt(CommandHead, 16);
				finaldata[4] = (byte) Integer.parseInt(testChild, 16);
		        if(!dataStr.equals("")){
		        	finaldata[5] = (byte)1;
		        }else{
		        	finaldata[5] = (byte)0;
		        }	
		        return finaldata;
			}
		}else if(testChild.equals("16")){
			String dataStr = "";
			if(factoryindex ==1){
				finaldata[0] = (byte)75;
				finaldata[1] = (byte) 201;
				finaldata[2] = (byte) Integer.parseInt(Commandend, 16);
				finaldata[3] = (byte) Integer.parseInt(CommandHead, 16);
				finaldata[4] = (byte) Integer.parseInt(testChild, 16);
				finaldata[5] = (byte)0;
				return finaldata;
				/*finaldata[0] = (byte)75;
				finaldata[1] = (byte) 201;
				finaldata[2] = (byte) Integer.parseInt(Commandend, 16);
				finaldata[3] = (byte) Integer.parseInt(CommandHead, 16);
				finaldata[4] = (byte) Integer.parseInt(testChild, 16);
				if(openPartition() != -1){
					Log.i("zhou","start=====readpartition");
					int readvalues = readpartition(offset, size, writebuff);
					Log.i("zhou","readvalues==="+readvalues);
					closepartition();
					if(readvalues != -1){
						finaldata[5] = (byte)1;
						finaldata[6] = (byte)readvalues;
					}else{
						finaldata[5] = (byte)0;
					}
					return finaldata;
				}else{
					finaldata[5] = (byte)0;
					return finaldata;
				}*/
			}else{
				finaldata[0] = (byte)75;
				finaldata[1] = (byte) 201;
				finaldata[2] = (byte) Integer.parseInt(Commandend, 16);
				finaldata[3] = (byte) Integer.parseInt(CommandHead, 16);
				finaldata[4] = (byte) Integer.parseInt(testChild, 16);
				if(openPartition() != -1){
					int writevalues = writepartition(offset, size,writebuff );
					Log.i("zhou","writevalues==="+writevalues);
					closepartition();
					if(writevalues != -1){
						finaldata[5] = (byte)1;
						finaldata[6] = (byte)writevalues;
					}else{
						finaldata[5] = (byte)0;
					}
					return finaldata;
				}else{
					finaldata[5] = (byte)0;
					return finaldata;
				}
			}
		}else{
			return null;
		}
	}
}
