package com.zxh.pluginapp;

import android.content.res.AssetManager;
import android.content.res.Resources;

import androidx.appcompat.app.AppCompatActivity;

/**
 * 插件的BaseActivity
 */
public class BaseActivity extends AppCompatActivity {
    @Override
    public Resources getResources() {
        if(getApplication()!=null && getApplication().getResources()!=null){
            return getApplication().getResources();
        }
        return super.getResources();
    }

    @Override
    public AssetManager getAssets() {
        if(getApplication()!=null && getApplication().getAssets()!=null){
            return getApplication().getAssets();
        }
        return super.getAssets();
    }
}
