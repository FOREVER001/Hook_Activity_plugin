package com.zxh.hookplugin;

import android.os.Build;
import android.os.Handler;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

public class HookHelper {

    public static void hookAMS() throws Exception{
        try {
              Object defaultSingleton=null;
            if(Build.VERSION.SDK_INT>=26){//8.0
                Class<?> mActivityManagerClass = Class.forName("android.app.ActivityManager");
                Field iActivityManagerSingletonField = mActivityManagerClass.getDeclaredField("IActivityManagerSingleton");
                iActivityManagerSingletonField.setAccessible(true);
                 defaultSingleton = iActivityManagerSingletonField.get(null);
            }else {
                Class<?> mActivityManagerNativeClass = Class.forName("android.app.ActivityManagerNative");
                //获取ActivityManagerNative中的gDefault字段
                Field gDefaultField = mActivityManagerNativeClass.getDeclaredField("gDefault");
                gDefaultField.setAccessible(true);
                 defaultSingleton = gDefaultField.get(null);
            }

            Class<?> mSingletonClass = Class.forName("android.util.Singleton");
            Field mInstanceField = mSingletonClass.getDeclaredField("mInstance");
            mInstanceField.setAccessible(true);

            //获取IActivityManager的实例
            Object iActivityManager = mInstanceField.get(defaultSingleton);
            Class<?> mIActivityManagerClass = Class.forName("android.app.IActivityManager");
            Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader()
                    , new Class[]{mIActivityManagerClass}, new IActivityManagerProxy(iActivityManager));

            mInstanceField.set(defaultSingleton,proxy);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void hookHandler() throws Exception{
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        Field sCurrentActivityThreadField = activityThreadClass.getDeclaredField("sCurrentActivityThread");
        sCurrentActivityThreadField.setAccessible(true);
        Object activityThread = sCurrentActivityThreadField.get(null);
        Field mHField = activityThreadClass.getDeclaredField("mH");
        mHField.setAccessible(true);
        Handler mHObj = (Handler) mHField.get(activityThread);

        Class<?> handlerClass = Class.forName("android.os.Handler");
        Field mCallbackField = handlerClass.getDeclaredField("mCallback");
        mCallbackField.setAccessible(true);
        mCallbackField.set(mHObj,new HCallback(mHObj));
    }
}
