package com.weatherapp;

import android.app.Application;
import android.os.Handler;

import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;

;

/**
 * Created by Mounzer on 8/22/2017.
 */
@Database(name = AppController.NAME, version = AppController.VERSION)

public class AppController extends Application {

    public static final String NAME = "Simple Weather App";
    public static final int VERSION = 1;

    public static final String TAG = AppController.class.getSimpleName();

    public static int REQUEST_DETAIL = 1;
    public static int CLOSE_CODE = 99;
    public static final int MY_PERMISSIONS_REQUEST_CAMERA = 0;

    public static String getUsername() {
        return username;
    }

    public static void setUsername(String username) {
        AppController.username = username;
    }

    public static String username;

    //public static int IMAGE_REQUEST = 7;
    private static AppController singleton;



    @Override
    public void onCreate() {
        super.onCreate();
        FlowManager.init(new FlowConfig.Builder(this).build());



    }


    public static AppController getInstance(){
        return singleton;
    }
    Handler lochandler;
    public Handler getLocationHandler(){
        return lochandler;
    }
    Handler currhandler;

}
