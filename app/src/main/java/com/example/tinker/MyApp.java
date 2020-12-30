package com.example.tinker;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.multidex.MultiDex;

import com.example.tinker.tinker.AppTinkerLoader;
import com.example.tinker.tinker.Log.MyLogImp;
import com.example.tinker.tinker.util.SampleApplicationContext;
import com.example.tinker.tinker.util.TinkerManager;
import com.tencent.tinker.anno.DefaultLifeCycle;
import com.tencent.tinker.entry.DefaultApplicationLike;
import com.tencent.tinker.lib.tinker.Tinker;
import com.tencent.tinker.lib.tinker.TinkerApplicationHelper;
import com.tencent.tinker.lib.tinker.TinkerInstaller;
import com.tencent.tinker.loader.shareutil.ShareConstants;

/**
 * Created By hudawei
 * on 2020/12/30 0030
 */
@DefaultLifeCycle(application = "com.example.tinker.SampleApplication",
        flags = ShareConstants.TINKER_ENABLE_ALL,loaderClass = "com.example.tinker.tinker.AppTinkerLoader"
        /*,loadVerifyFlag = false*/)
public class MyApp extends DefaultApplicationLike {
    private static MyApp instance;

    public static Application getApp() {
        return getThis().getApplication();
    }

    public static MyApp getThis() {
        return instance;
    }

    public MyApp(Application application, int tinkerFlags, boolean tinkerLoadVerifyFlag,
                 long applicationStartElapsedTime, long applicationStartMillisTime, Intent tinkerResultIntent) {
        super(application, tinkerFlags, tinkerLoadVerifyFlag, applicationStartElapsedTime, applicationStartMillisTime, tinkerResultIntent);
    }
    /**
     * 安装Tinker
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onBaseContextAttached(Context base) {
        super.onBaseContextAttached(base);
        //如果当前App已升级，则清除原来的补丁包，避免合成失败
        if(AppTinkerLoader.getCurVersionCode(getApplication()) != AppTinkerLoader.packageCode(getApplication())) {
            TinkerApplicationHelper.cleanPatch(this);
            AppTinkerLoader.setCurVersionCode(getApplication());
        }

        //you must install multiDex whatever tinker is installed!
        MultiDex.install(base);

        SampleApplicationContext.application = getApplication();
        SampleApplicationContext.context = getApplication();
        TinkerManager.setTinkerApplicationLike(this);

        TinkerManager.initFastCrashProtect();
        //should set before tinker is installed
        TinkerManager.setUpgradeRetryEnable(true);

        //optional set logIml, or you can use default debug log
        TinkerInstaller.setLogIml(new MyLogImp());

        //installTinker after load multiDex
        //or you can put com.tencent.tinker.** to main dex
        TinkerManager.installTinker(this);
        Tinker tinker = Tinker.with(getApplication());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

}
