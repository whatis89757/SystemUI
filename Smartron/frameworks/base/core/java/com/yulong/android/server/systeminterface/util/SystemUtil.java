
package com.yulong.android.server.systeminterface.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
//import android.telephony.MSimTelephonyManager;
import android.util.Log;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidParameterException;
import com.android.internal.telephony.ITelephony;

/** {@hide} */
public  class SystemUtil {
	private static String TAG = "SystemUtil";
	private static final boolean DEBUG = true;
	/** {@hide} */
    private static final String configPath="//sys//class//android_usb//android0//usb_mode";
    /** {@hide} */
    private static ITelephony mService;
    /** {@hide} */
    public static final int SIZE_OF_NV_SMSAUTO = 124;
    /** {@hide} */
    public static final int GET_FLAG = 0;
    /** {@hide} */
    public static final int SET_FLAG = 1;
    /** {@hide} */
    private final static int QCRILHOOK_BASE = 0x80000;
    /** Read NV Item {@hide} */
    private final static  int QCRILHOOK_NV_READ = QCRILHOOK_BASE + 1;

    /** Write NV Item {@hide} */
    private final static  int QCRILHOOK_NV_WRITE = QCRILHOOK_BASE + 2;
    
    /**写入flush中的key,少于9个 {@hide}*/
    private static final String KEY_MMS_AUTO_REG = "AUTO_REG";
   /**写入flush中的key {@hide} */
   private static final String KEY_MMS_VERSION = "MMS_VER";
   /**写入flush中的key {@hide} */
   private static final String KEY_MMS_DM = "mms_imsi";
   
   /** {@hide} */
//   private static final int MMS_AUTO_REG_SIZE = 48;
   /** {@hide} */
//   private static final int MMS_VERSION_SIZE = 20;
   /** {@hide} */
//   private static final int MMS_DM_SIZE = 20;
   
   /** {@hide} */
   private static final int MMS_AUTO_REG_NV = 6853;
   /** {@hide} */
   private static final int MMS_VERSION_NV = 6854;
   /** {@hide} */
   private static final int MMS_DM_NV = 6855;
   
   /** {@hide} 读取DM工厂模式标记，如果标记值为1:则不进行注册；为0:进行注册。 */
    public static int getDMValue(){
        if(isTestSwitchOn()){
			return 1;
        }
        return 0; 
    }
    
    /**
     * 获取商标名（Coolpad）
     * @hide
     * @return String
     */
    public static String getBrand() {
    	return SystemProperties.get("ro.product.brand", Build.UNKNOWN);
    }
    
    /**
     * 获取手机当前模式（N930）手机型号
     * @hide
     * @return String
     */
    public static String getPhoneModel() {
    	return SystemProperties.get("ro.product.ui_model", Build.UNKNOWN);
    }
    
    /**
     * 获取手机软件版本号
     * @hide
     * @return String
     */
    public static String getSoftWareVersion(){
    	final int MAX_LENGTH = 28;
        String versionExtern = "";
        try{
            versionExtern = SystemProperties.get("ro.yulong.version.software");
            if(versionExtern != null && versionExtern.length() >= MAX_LENGTH){
                versionExtern = versionExtern.substring(0, MAX_LENGTH);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return versionExtern;
    }
    
    /**
     * 操作手机和卡的ESN。电信自注册需要用到，公司注册不行。
     * @hide
     * @param oper boolean（true为写，false为读）
     * @return String
     * @throws RemoteException
     */
	public static String execRuimEsnOp(boolean oper) throws RemoteException {
		return "";
	}
	
	/** {@hide} */
    public static String getYLParam(String sParamName){
    	int[] itemNv = getNvAdress(GET_FLAG, sParamName);
    	Log.d(TAG, "getYLParam itemNv 0:" + itemNv[0] + ", 1:" + itemNv[1]);
    	String ssmsAutoRegisterInfo = getReadNvResult(itemNv[0], itemNv[1]);
    	if (ssmsAutoRegisterInfo == null) {
            Log.w(TAG, "ssmsAutoRegisterInfo == null");
    		return null;
    	}
        Log.i(TAG, "getYLParam:" + ssmsAutoRegisterInfo.trim());
        return ssmsAutoRegisterInfo.trim();
    }
    
    /** {@hide} */
    private static String getReadNvResult(int flag, int nvAdress) {
    	String ar = null; 
    	if (null == mService) {
            mService = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        }
    	try {
			ar = mService.getSMSAutoRegisterInfo(nvAdress);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return ar;
    }
    
    /** {@hide} */
    private static int[] getNvAdress(int flag, String sParamName) {
    	int[] nvItem = {-1,7233};
    	if (flag == GET_FLAG) {
    		nvItem[0] = QCRILHOOK_NV_READ;//
    	} else {
    		nvItem[0] = QCRILHOOK_NV_WRITE;
    	}
    	if (KEY_MMS_AUTO_REG.equals(sParamName)) {
    		nvItem[1] = MMS_AUTO_REG_NV;
		} else if (KEY_MMS_VERSION.equals(sParamName)) {
			nvItem[1] = MMS_VERSION_NV;
		} else if (KEY_MMS_DM.equals(sParamName)) {
			nvItem[1] = MMS_DM_NV;
		}
		return nvItem;
	}
    /** {@hide} */
	public static void setYLParam(String sParamName, String sValue){
    	int oLen = sValue.length();
		int[] itemNv = getNvAdress(SET_FLAG, sParamName);
    	Log.d(TAG, "setYLParam sValue:" + sValue);
    	Log.d(TAG, "setYLParam itemNv 0:" + itemNv[0] + ", 1:" + itemNv[1]);
    	
    	if (sValue == null || sValue.length() > 124) {
            throw new InvalidParameterException();
        } else {
        	Log.d(TAG, "setYLParam sValue lena:" + sValue.length());
        	for (int i = 0; i < SIZE_OF_NV_SMSAUTO - oLen; i++) {
        		sValue += " ";
        	}
        	Log.d(TAG, "setYLParam sValue:" + sValue);
        	Log.d(TAG, "setYLParam sValue lenb:" + sValue.length());
        }
    	if (null == mService) {
            mService = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        }
    	try {
			mService.setSMSAutoRegisterInfo(sValue, itemNv[1]);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	/** {@hide} */
    public static String getSN(){
    	String SN = null;
    	if (null == mService) {
            mService = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        }
    	try {
    		SN = mService.getSN();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	if (SN == null || "".equals(SN))
    		SN = "5210201302010FA";
        return SN;
    }
    
    /** {@hide} */
    public static boolean isImsRegistered(){
    	boolean registed =false;
    	if (null == mService) {
            mService = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        }
    	try {
    		registed = mService.isImsRegistered();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        return registed;
    }
    /** {@hide} */
    public static boolean isTestSwitchOn(){
        try {
            FileInputStream inStream = new FileInputStream(configPath);
		    int nu = inStream.read();
            inStream.close();
		    if (1 == nu)
			    return true;
    	} catch (FileNotFoundException e) {
	    	e.printStackTrace();
            // default test mode without usb_mode file. 
            return true;
    	} catch (IOException e) {
	    	e.printStackTrace();
	    }
    	return false;
   }
    
    /**
     * 通话总时长（单位是秒）
     * @return long
     *  {@hide}
     */
    public static long queryCalllogDuration(Context context) {
        long time =0;
        try{
            Context userContext=context.createPackageContext("com.android.phone",Context.CONTEXT_IGNORE_SECURITY);
            SharedPreferences sp = userContext.getSharedPreferences("Total_Call_Time", Context.MODE_WORLD_READABLE + Context.MODE_WORLD_WRITEABLE);
            time = sp.getLong("time", 0);
            Log.e("LLS","call time = " + time);
        }catch(Exception e){
            Log.e("LLS","getTotalCallTime() exception e :" + e);
        }
        return time/1000;
    }

	    /** {@hide} */
    public static String getNvFactoryData3I(){
    	String info = null;
    	if (null == mService) {
            mService = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        }
    	try {
    		info = mService.getNvFactoryData3I();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return info;
    }
    /** {@hide} */
    public static String getQCNVersion(){
    	String info = null;
    	if (null == mService) {
            mService = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        }
    	try {
    		info = mService.getQCNVersion();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return info;
    }

    /** {@hide} */
    public static byte[] getNvFactoryData3IByte(){
    	byte[] info = null;
    	if (null == mService) {
            mService = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        }
    	try {
    		if (mService != null) {
        		info = mService.getNvFactoryData3IByte();
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return info;
    }
    
    /** {@hide} */
    public static void setNvFactoryData3I(String result){
    	if (null == mService) {
            mService = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        }
    	try {
    		mService.setNvFactoryData3I(result);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

	/**
	 * {@hide}
	 */
	public static void setIMEIAndMEID(String target, String result) {
		if (null == mService) {
			mService = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
			Log.v("JIANG","mService="+mService);
		}
		try {
			Log.v("JIANG","mService setIMEIAndMEID "+mService);
			mService.setIMEIAndMEID(target, result);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.v("JIANG","e="+e);
		}
	}

	/** {@hide} */
    public static void setNvFactoryData3IByte(byte[] bresult){
    	if (null == mService) {
            mService = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        }
    	try {
    		mService.setNvFactoryData3IByte(bresult);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    /** {@hide} */
    public static String getMeid(){
    	String info = "";
    	if (null == mService) {
            mService = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        }
    	try {
    		info = mService.getMeid();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch(NullPointerException e){
			return info;
		}
        return info;
    }
    
    /** {@hide} */
    public static String getImei1(){
    	String info = "";
    	if (null == mService) {
            mService = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        }
    	try {
    		info = mService.getImei1();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch(NullPointerException e){
			return info;
		}
        return info;
    }
    
    /** {@hide} */
    public static String getImei2(){
    	String info = "";
    	if (null == mService) {
            mService = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        }
    	try {
    		info = mService.getImei2();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch(NullPointerException e){
			return info;
		}
        return info;
    }

    /** {@hide} */
    public static String getWlanMac(){
        String wlanMac = null;
        if (null == mService) {
            mService = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        }
        try {
            wlanMac = mService.getWlanMac();
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (wlanMac == null || "".equals(wlanMac))
            wlanMac = "1252";
        return wlanMac;
    }

    public static String getSecCode(){
        String info = "";
        if (null == mService) {
               mService = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
           }
         try {
           info = mService.getSecCode();
           } catch (RemoteException e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
           }catch(NullPointerException e){
               return info;
           }
           return info;
       }

    /** {@hide} */
    public static int getNetworkType(){
        int networktype = 0;
        if (null == mService) {
            mService = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
        }
        try {
             networktype = mService.getNetworkType();
        } catch (RemoteException e) {
             // TODO Auto-generated catch block
             e.printStackTrace();
        }
        return networktype;
    }
}
