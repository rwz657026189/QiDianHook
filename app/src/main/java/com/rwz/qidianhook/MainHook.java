package com.rwz.qidianhook;

import android.util.Log;

import com.rwz.qidianhook.hook.QDManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;


public class MainHook  implements IXposedHookLoadPackage {

    private static final String TAG = "MainHook";
    private static final String packageName = "com.qidian.QDReader";
//rwzwenzhang@ling1.org
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        //https://toutyrater.github.io@bitbucket.org/ling1_v3/changdu-reaper.git
        //https://XXXXXX@bitbucket.org/ling1_v3/changdu-reaper.git
        Log.d(TAG, "handleLoadPackage: packageName = " + lpparam.packageName + "processName = " + lpparam.processName);
        if (!packageName.equals(lpparam.packageName) || !packageName.equals(lpparam.processName)) {
            return;
        }
        QDManager.getInstance().init(lpparam.classLoader);
    }
}
