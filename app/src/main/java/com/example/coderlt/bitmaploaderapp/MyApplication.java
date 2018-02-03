package com.example.coderlt.bitmaploaderapp;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;

/**
 * Created by coderlt on 2018/2/3.
 */

public class MyApplication extends Application {
    private static Context mContext;

    @Override
    public void onCreate(){
        super.onCreate();
        mContext=getApplicationContext();
    }

    public static Context getContext(){
        return mContext;
    }
}
