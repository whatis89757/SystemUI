package com.android.server;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Array;
import java.util.Arrays;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaPlayer.OnCompletionListener;

public class HWSocketService extends Service {
	private HwMsgManager mHwMsgManager = null;
	private Context mContext = this;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		mHwMsgManager = new HwMsgManager();
		mHwMsgManager.setContext(mContext);
        new Thread(new Runnable() {

            @Override
            public void run() {
                    // TODO Auto-generated method stub
                    Log.i("zhou", "HWSocketService=====onstart");
                    LocalSocketAddress localSocketAddress = new LocalSocketAddress(
                                    "audio_diag", LocalSocketAddress.Namespace.RESERVED);
                    LocalSocket localSocket = new LocalSocket(
                                    LocalSocket.SOCKET_STREAM);
                    try {
                            localSocket.connect(localSocketAddress);
                            InputStream inputStream = null;
                            OutputStream outputStream = null;
                            while (true) {
                                    inputStream = localSocket.getInputStream();
                                    byte[] buffer = new byte[512];
                                    byte[] finaldata = new byte[512];
                                    inputStream.read(buffer);
                                    if(buffer.length == 0){
                                    	continue;
                                    }
                                    finaldata = mHwMsgManager.handleMsg(buffer);
                                    outputStream = localSocket.getOutputStream();
                                    String finalstr =mHwMsgManager.bytes2HexString(finaldata);
                                    if(finalstr != null){
                                   	 Log.i("zhou", "finalstr======"+finalstr);
                                   }
                                    outputStream.write(finaldata);
                                    outputStream.flush();
                            }
                    } catch (Exception ex) {
                            ex.printStackTrace();
                    }
            }
    }).start();
        flags = START_STICKY;
		return super.onStartCommand(intent, flags, startId);
	}
}
