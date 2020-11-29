package com.zxh.hookplugin;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class HookHelper {
    private static HookHelper instance;
    private HookHelper(){}
    public static HookHelper getInstance(){
        if(null==instance){
            synchronized (HookHelper.class){
                instance=new HookHelper();
            }
        }
        return instance;
    }

    public  void hookAMS() throws Exception{
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

    public  void hookHandler() throws Exception{
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

    public  void pluginToAppAction (Application application) throws Exception{
       //1.获取宿主dexElements

        //获取PathClassLoader
        PathClassLoader pathClassLoader= (PathClassLoader) application.getClassLoader();
        //反射获取BaseDexClassLoader
        Class<?> mBaseDexClassLoaderClass = Class.forName("dalvik.system.BaseDexClassLoader");
        //获取private final DexPathList pathList;
        Field pathListField = mBaseDexClassLoaderClass.getDeclaredField("pathList");
        pathListField.setAccessible(true);
        Object dexPathList = pathListField.get(pathClassLoader);

        //获取DexPathList中的 private Element[] dexElements;
        Field dexElementsField = dexPathList.getClass().getDeclaredField("dexElements");
        dexElementsField.setAccessible(true);
        //本质是private Element[] dexElements;
        Object dexElements = dexElementsField.get(dexPathList);

        //2.获取插件的dexElements

        File file=new File("/data/data/"+application.getPackageName()+File.separator+"plugin");
        if(!file.exists()){
            file.mkdirs();
        }
        file=new File(file.getAbsolutePath()+File.separator+"plugin.apk");
        String path=file.getPath();
        File pluginDir = application.getApplicationContext().getDir("pluginDir", Context.MODE_PRIVATE);
        DexClassLoader dexClassLoader=new DexClassLoader(path,pluginDir.getAbsolutePath(),null,application.getClassLoader());
        //反射获取BaseDexClassLoader
        Class<?> mBaseDexClassLoaderClassPlugin = Class.forName("dalvik.system.BaseDexClassLoader");
        //获取private final DexPathList pathList;
        Field pathListFieldPlugin = mBaseDexClassLoaderClassPlugin.getDeclaredField("pathList");
        pathListFieldPlugin.setAccessible(true);
        Object dexPathListPlugin = pathListFieldPlugin.get(dexClassLoader);

        //获取DexPathList中的 private Element[] dexElements;
        Field dexElementsFieldPlugin = dexPathListPlugin.getClass().getDeclaredField("dexElements");
        dexElementsFieldPlugin.setAccessible(true);
        //本质是private Element[] dexElements;
        Object dexElementsPlugin = dexElementsFieldPlugin.get(dexPathListPlugin);

        //3.创建新的数组
        //宿主dexElements的长度
        int mainDexLength = Array.getLength(dexElements);
        //插件dexElements的长度
        int pluginDexLength = Array.getLength(dexElementsPlugin);

        int sumDexLength=mainDexLength+pluginDexLength;
        //参数一：int[] String[] ...我们需要Element[]
        //参数二：数组对象的长度
        //本质是Element[] newElements
        Object newElements=Array.newInstance(dexElements.getClass().getComponentType(),sumDexLength);

        //融合DexElement
        for (int i = 0; i < sumDexLength; i++) {
            //先融合宿主的
            if(i<mainDexLength){

                Array.set(newElements,i,Array.get(dexElements,i));
            }else {
                //在融合插件的
                Array.set(newElements,i,Array.get(dexElementsPlugin,i-mainDexLength));
            }

        }


        //把新的newElements设置到宿主中
        dexElementsField.set(dexPathList,newElements);

       //处理加载插件中的布局
        doPluginLayout(application.getApplicationContext());
    }

    private AssetManager mAssetManager;
    private Resources mResources;

    private  void doPluginLayout(Context context) throws Exception{
        mAssetManager=AssetManager.class.newInstance();

        File file=new File("/data/data/"+context.getPackageName()+File.separator+"plugin");
        if(!file.exists()){
            file.mkdirs();
        }
        file=new File(file.getAbsolutePath()+File.separator+"plugin.apk");
        if(!file.exists()){
           throw new FileNotFoundException("没有找到插件包");
        }

        // 执行此方法 public final int addAssetPath(String path)，才能把插件路径添加进去
        Method addAssetPathMethod = mAssetManager.getClass().getDeclaredMethod("addAssetPath", String.class);
        addAssetPathMethod.setAccessible(true);
        addAssetPathMethod.invoke(mAssetManager,file.getAbsolutePath());

        Method ensureStringBlocksMethod = mAssetManager.getClass().getDeclaredMethod("ensureStringBlocks");
        ensureStringBlocksMethod.setAccessible(true);
        ensureStringBlocksMethod.invoke(mAssetManager);


        //宿主的Resources,为了获取宿主r.getDisplayMetrics(),r.getConfiguration()
        Resources r = context.getApplicationContext().getResources();
        mResources=new Resources(mAssetManager,r.getDisplayMetrics(),r.getConfiguration());
    }

    public AssetManager getAssetManager() {
        return mAssetManager;
    }

    public Resources getResources() {
        return mResources;
    }
}
