package com.android.wallpapercropper;

import android.app.Application;
import android.content.Context;
import android.util.Log;

/**
 * Created by smartron on 19/4/17.
 */

public class WallPaperApplication extends Application implements Thread.UncaughtExceptionHandler {

    private static final String TAG = WallPaperApplication.class.getSimpleName();

    // uncaught exception handler variable
    private Thread.UncaughtExceptionHandler mOOMException;


    @Override
    public void onCreate() {
        super.onCreate();

        mOOMException = Thread.getDefaultUncaughtExceptionHandler();

        // setup handler for uncaught exception
        Thread.setDefaultUncaughtExceptionHandler(this);


    }

    // #5515
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {

      if(ex instanceof java.lang.OutOfMemoryError){
            Log.d(TAG, "L1:"+ex.toString());
            System.exit(0);
        }else if(ex.getCause() instanceof java.lang.OutOfMemoryError){
            Log.d(TAG, "L2:"+ex.toString());
            System.exit(0);
        }else {
            // re-throw critical exception further to the os (important)
            mOOMException.uncaughtException(thread, ex);
        }
    }
}