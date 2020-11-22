package com.zxh.hookplugin;

import android.content.Intent;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class IActivityManagerProxy implements InvocationHandler {
    private Object mActivityManager;

    public IActivityManagerProxy(Object activityManager) {
        mActivityManager = activityManager;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("startActivity".equals(method.getName())) {
            int index = 0;
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Intent) {
                    index = i;
                    break;
                }
            }
            //默认传递过来的没有注册的Activity的Intent
            Intent intent = (Intent) args[index];
            //创建一个新的Intent用来设置占位Activity和携带原来的Intent
            Intent newInTent = new Intent();
            String packageName = "com.zxh.hookplugin";
            newInTent.setClassName(packageName, packageName + ".StubActivity");
            //携带原来的Intent,方便复原的时候使用
            newInTent.putExtra("target_intent", intent);
            args[index] = newInTent;
        }
        return method.invoke(mActivityManager, args);
    }
}
