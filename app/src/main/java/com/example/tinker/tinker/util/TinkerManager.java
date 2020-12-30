/*
 * Tencent is pleased to support the open source community by making Tinker available.
 *
 * Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.tinker.tinker.util;

import android.content.Context;
import android.text.TextUtils;

import com.example.tinker.tinker.crash.SampleUncaughtExceptionHandler;
import com.example.tinker.tinker.reporter.SampleLoadReporter;
import com.example.tinker.tinker.reporter.SamplePatchListener;
import com.example.tinker.tinker.reporter.SamplePatchReporter;
import com.example.tinker.tinker.service.SampleResultService;
import com.tencent.tinker.entry.ApplicationLike;
import com.tencent.tinker.lib.listener.PatchListener;
import com.tencent.tinker.lib.patch.AbstractPatch;
import com.tencent.tinker.lib.patch.UpgradePatch;
import com.tencent.tinker.lib.reporter.LoadReporter;
import com.tencent.tinker.lib.reporter.PatchReporter;
import com.tencent.tinker.lib.tinker.Tinker;
import com.tencent.tinker.lib.tinker.TinkerInstaller;
import com.tencent.tinker.lib.tinker.TinkerLoadResult;
import com.tencent.tinker.lib.util.TinkerLog;
import com.tencent.tinker.lib.util.UpgradePatchRetry;

import java.io.File;

/**
 * Created by zhangshaowen on 16/7/3.
 */
public class TinkerManager {
    private static final String TAG = "Tinker.TinkerManager";
    private static final String TINKER_PATCH_NAME = "patch_signed_7zip.apk";

    private static ApplicationLike applicationLike;
    private static SampleUncaughtExceptionHandler uncaughtExceptionHandler;
    private static boolean isInstalled = false;

    public static void setTinkerApplicationLike(ApplicationLike appLike) {
        applicationLike = appLike;
    }

    public static ApplicationLike getTinkerApplicationLike() {
        return applicationLike;
    }

    public static void initFastCrashProtect() {
        if (uncaughtExceptionHandler == null) {
            uncaughtExceptionHandler = new SampleUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
        }
    }

    public static void setUpgradeRetryEnable(boolean enable) {
        UpgradePatchRetry.getInstance(applicationLike.getApplication()).setRetryEnable(enable);
    }


    /**
     * all use default class, simply Tinker install method
     */
    public static void sampleInstallTinker(ApplicationLike appLike) {
        if (isInstalled) {
            TinkerLog.w(TAG, "install tinker, but has installed, ignore");
            return;
        }
        TinkerInstaller.install(appLike);
        isInstalled = true;

    }

    /**
     * you can specify all class you want.
     * sometimes, you can only install tinker in some process you want!
     *
     * @param appLike
     */
    public static void installTinker(ApplicationLike appLike) {
        if (isInstalled) {
            TinkerLog.w(TAG, "install tinker, but has installed, ignore");
            return;
        }
        //or you can just use DefaultLoadReporter
        LoadReporter loadReporter = new SampleLoadReporter(appLike.getApplication());
        //or you can just use DefaultPatchReporter
        PatchReporter patchReporter = new SamplePatchReporter(appLike.getApplication());
        //or you can just use DefaultPatchListener
        PatchListener patchListener = new SamplePatchListener(appLike.getApplication());
        //you can set your own upgrade patch if you need
        AbstractPatch upgradePatchProcessor = new UpgradePatch();

        TinkerInstaller.install(appLike,
                loadReporter, patchReporter, patchListener,
                SampleResultService.class, upgradePatchProcessor);

        isInstalled = true;
    }

    /**
     * 检查补丁包是否需要更新
     *
     * @param latestPatchMd5 最新补丁包Md5
     * @param latestPatchUrl 最新补丁包下载地址
     */
    public static void checkUpgrade(Context context, String latestPatchMd5, String latestPatchUrl) {
        if (!TextUtils.isEmpty(latestPatchMd5) && !TextUtils.isEmpty(latestPatchUrl)) {
            String curPatchMd5 = getCurrentPatchVersion(context);
            if (!TextUtils.equals(curPatchMd5, latestPatchMd5)) {
                downPatch(context, latestPatchUrl);
            }
        }
    }

    /**
     * 合成补丁包
     */
    public static void patch(Context context) {
        TinkerInstaller.onReceiveUpgradePatch(context,
                getTinkerDir(context) + File.separator + TINKER_PATCH_NAME);
    }

    /**
     * 下载补丁包
     *
     * @param url 下载地址
     */
    private static void downPatch(Context context, String url) {
        new Download()
                .remote(url)
                .local(new File(getTinkerDir(context)), TINKER_PATCH_NAME)
                .start(new Download.SimpleCallback() {
                    @Override
                    public void onDownloadFailed(String url, Exception e) {
                        super.onDownloadFailed(url, e);
                    }

                    @Override
                    public void onDownloadCompleted(String url, File localFile) {
                        super.onDownloadCompleted(url, localFile);
                        patch(context);
                    }
                });
    }

    /**
     * 获取当前使用补丁包的版本(Md5值)
     *
     * @return 当前使用补丁版本
     */
    private static String getCurrentPatchVersion(Context context) {
        Tinker tinker = Tinker.with(context);

        if (tinker.isTinkerLoaded()) {
            TinkerLoadResult tinkerLoadResult = tinker.getTinkerLoadResultIfPresent();
            if (tinkerLoadResult != null && !tinkerLoadResult.useInterpretMode) {
                return tinkerLoadResult.currentVersion;
            }
        }
        return null;
    }

    /**
     * 创建补丁包下载存放目录
     *
     * @return 补丁包下载存放目录
     */
    private static String getTinkerDir(Context context) {
        File file = new File(context.getFilesDir(), "tinker");
        if (!file.exists() || !file.isDirectory()) {
            file.mkdir();
        }
        return file.getAbsolutePath();
    }
}
