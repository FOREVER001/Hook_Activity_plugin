package com.zxh.hookplugin;

import android.app.Application;
import android.content.Context;

public class App extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            HookHelper.hookAMS();
            HookHelper.hookHandler();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
