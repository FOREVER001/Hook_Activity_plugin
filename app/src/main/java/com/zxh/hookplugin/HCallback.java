package com.zxh.hookplugin;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import java.lang.reflect.Field;

public class HCallback implements Handler.Callback {
    public static final int LAUNCH_ACTIVITY = 100;
    private Handler mHandler;

    public HCallback(Handler handler) {
        mHandler = handler;
    }

    @Override
    public boolean handleMessage(Message msg) {
        if(msg.what==LAUNCH_ACTIVITY){
            Object r = msg.obj;
            try {
                //得到消息中的Intent启动（启动SubActivity的intent）
                Field intentField = r.getClass().getDeclaredField("intent"); //1
                intentField.setAccessible(true);
                Intent intent = (Intent) intentField.get(r);

                //得到此前保存起来的Intent(启动TargetActivity的intent)
               Intent targetIntent= intent.getParcelableExtra("target_intent");
               //将要启动的SubActivity的Intent替换为TargetActvity的Intent
                intent.setComponent(targetIntent.getComponent());
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        mHandler.handleMessage(msg);
        return true;
    }
}
