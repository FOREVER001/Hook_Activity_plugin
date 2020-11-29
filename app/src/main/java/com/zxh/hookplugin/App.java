package com.zxh.hookplugin;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;

public class App extends Application {
    private Resources mResources;
    private AssetManager mAssetManager;
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            //绕过AMS校验
            HookHelper.getInstance().hookAMS();
            //Element融合
            HookHelper.getInstance().pluginToAppAction(this);
            //目标Activity恢复
            HookHelper.getInstance().hookHandler();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mResources=HookHelper.getInstance().getResources();
        mAssetManager=HookHelper.getInstance().getAssetManager();
    }

    @Override
    public Resources getResources() {
        return mResources==null?super.getResources():HookHelper.getInstance().getResources();
    }

    @Override
    public AssetManager getAssets() {
        return mAssetManager==null? super.getAssets():HookHelper.getInstance().getAssetManager();
    }
}
