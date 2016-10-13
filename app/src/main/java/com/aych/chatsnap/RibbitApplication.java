package com.aych.chatsnap;

import android.app.Application;

import com.parse.Parse;

/**
 * Created by HowardHuang on 7/1/2015.
 */
public class RibbitApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Parse.initialize(this, "63Ey9ZqZEFmB19T4xBDOVUfH99Kebx5ceNpyQoXS", "tlbwjGeg5yP1QGnMtuyHWAFaJAJ4JkuRNYA4TMM3");
    }
}
