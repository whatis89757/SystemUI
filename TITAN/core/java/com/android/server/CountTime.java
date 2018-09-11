package com.android.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;
//import com.qualcomm.qcnvitems.QcNvItems;

public class CountTime {
	private static final String TOTALBOOTTIME = "persist/totalboottime.dat";
	private static final String FIRSTBOOTTIME = "persist/firstboottime.dat";
	private static boolean isChecked = true;
	private static boolean isNeedLoop = false;
	private long totalTime = 0;
	private static long currentTime = 0;
	private Handler mWorkerHandler = null;
	private HandlerThread mWorker = null;
	private static final int MSG_TIMER = 1;
	private static final int MSG_BEGIN_GETNV = 2;
	private static final int MSG_BEGIN_INIT_QCRILIMTE = 3;
	private static final int SAVEBOOTIMEDELAY = 60 * 1000;
//	private QcNvItems mNv;
	private static final String CONFIGPATH = "/sys/class/android_usb/android0/enable";
	private Context mContext = null;

	public CountTime(Context context) {
		super();
		mContext = context;
//		mNv = new QcNvItems(mContext);
	}

	private final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case MSG_TIMER:
				mWorker = new HandlerThread("MyWorker");
				mWorker.start();
				mWorkerHandler = new Handler(mWorker.getLooper());
				mWorkerHandler.postDelayed(new MessageReceiveTask(), 1000);
				break;
			case MSG_BEGIN_GETNV:
				Log.i("zhou", "MSG_BEGIN_GETNV=============");
				/*if (getHasCheckNVdata(6)) {
					readAndWriteTime();
				}*/
				readAndWriteTime();
				break;
			case MSG_BEGIN_INIT_QCRILIMTE:
//				mNv = new QcNvItems(mContext);
				Message msgget = Message.obtain();
				msgget.what = MSG_BEGIN_GETNV;
				handler.sendMessageDelayed(msgget, 10000);
				break;
			}
		}
	};

/*	private void rewriteOneByteOfNv2499(int idx, char testResult) {
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
		Log.i("zhou", "bresult[idx]===" + bresult[idx]);
		try {
			mNv.setNvFactoryData3IByte(bresult);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

	}

	private boolean getHasCheckNVdata(int idx) {
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
			Log.i("zhou", "getHasCheckNVdata===bresult is null==========");
			isChecked = false;
			return false;
		}
		byte check = bresult[idx];
		String compare = Byte.toString(check);
		Log.i("zhou", "getHasCheckNVdata===" + compare);
		if (compare.equals("80")) {
			isChecked = true;
			return true;
		} else {
			Log.i("zhou", "xxxxxxxxxxxxxxxx");
			isChecked = false;
			return false;
		}

	}*/

	private void readAndWriteTime() {
		if (isChecked) {
			File firstbootFIle = new File(FIRSTBOOTTIME);
			File totaltimeFile = new File(TOTALBOOTTIME);
			if (!firstbootFIle.exists()) {
				try {
					firstbootFIle.createNewFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (!totaltimeFile.exists()) {
				try {
					totaltimeFile.createNewFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			String firsttime = readFile(FIRSTBOOTTIME);
			if (!firsttime.equals("")) {
				if (!readFile(TOTALBOOTTIME).equals("")) {
					totalTime = Long.parseLong(readFile(TOTALBOOTTIME));
				}
				if (currentTime == 0) {
					currentTime = totalTime;
				}
				if (totalTime > 36 * 60 * 60) {
					isNeedLoop = false;
				} else {
					isNeedLoop = true;
				}
			} else {
				writeFile(TOTALBOOTTIME, "0");
				writeFile(FIRSTBOOTTIME, "0");
				isNeedLoop = true;
			}
			Log.i("zhou", "isNeedLoop====" + isNeedLoop);
			if (isNeedLoop) {
				resultHandle();
			}

		}
	}

	private void resultHandle() {

		Message msgget = Message.obtain();
		msgget.what = MSG_TIMER;
		handler.sendMessageDelayed(msgget, 1000);
	}

	class MessageReceiveTask implements Runnable {
		public void run() {
			Log.i("zhou", "Runnable===" + totalTime);
			if (!isNeedLoop) {
				mWorkerHandler.removeCallbacks(this);
			} else {
				if (!readFile(TOTALBOOTTIME).equals("0")) {
					totalTime = Long.parseLong(readFile(TOTALBOOTTIME));
				}
				if (totalTime > 36 * 60 * 60) {
					isNeedLoop = false;
				} else {
					long uptime = SystemClock.elapsedRealtime() / 1000;
					long tempTime = currentTime + uptime;
					writeFile(TOTALBOOTTIME, "" + tempTime);
					if (readFile(FIRSTBOOTTIME).equals("0")) {
						writeFile(FIRSTBOOTTIME, "" + getSystemCurentTime(System.currentTimeMillis()));
					}
					isNeedLoop = true;
				}
				if (isNeedLoop) {
					mWorkerHandler.postDelayed(this, SAVEBOOTIMEDELAY);
				} else {
					mWorkerHandler.removeCallbacks(this);
				}
			}
		}
	}

	private String getSystemCurentTime(long firsttime) {
		SimpleDateFormat formatter = new SimpleDateFormat(
				"yyyy年MM月dd日    HH:mm:ss     ");
		Date curDate = new Date(firsttime);
		String str = formatter.format(curDate);
		Log.i("zhou", "str===" + str);
		return str;
	}

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

	public void writeFile(String filePath, String writeStr) {
		File tempfile = new File(filePath);
		if (!tempfile.exists()) {
			boolean isCreatSucess = false;
			try {
				isCreatSucess = tempfile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (!isCreatSucess) {
				return;
			}
		}
		try {
			FileWriter fw = new FileWriter(tempfile);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(writeStr);
			bw.close();
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void startCalculationTime() {
		Message msgget = Message.obtain();
		msgget.what = MSG_BEGIN_INIT_QCRILIMTE;
		handler.sendMessageDelayed(msgget, 50000);

	}

	public void startHwSockteService() {
		String diagOpen = readFile(CONFIGPATH);
		Log.i("zhou", "diagOpen===" + diagOpen);
		if (diagOpen.trim().equals("1")) {
			Log.i("zhou", "ooooooooooooo");
			SystemProperties.set("persist.sys.diagtest", "1");
			Intent HWSocketServiceIntent = new Intent(mContext,
					HWSocketService.class);
			mContext.startService(HWSocketServiceIntent);
		} else {
			Log.i("zhou", "pppppppppp");
			SystemProperties.set("persist.sys.diagtest", "0");
		}
	}

}
