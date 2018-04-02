package com.smartapp.rootuninstaller.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.os.Handler;
import android.os.RemoteException;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;

import com.smartapp.rootuninstaller.ListDataBean;
import com.smartapp.rootuninstaller.MainActivity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class will perform data operation
 */
public class StorageInformation {

    long packageSize = 0;
    AppDetails cAppDetails;
    public ArrayList<AppDetails.PackageInfoStruct> res;
    Context context;
    public Map<String, Long> sizeMap = new HashMap<String, Long>();
    private List<ListDataBean> userApps;
    private List<ListDataBean> systemApps;
    private List<ListDataBean> disableApps;
    private Handler handler;

    public StorageInformation(Context context, List<ListDataBean> userApps, List<ListDataBean> systemApps,
                              List<ListDataBean> disableApps, Handler handler) {
        this.context = context;
        this.userApps = userApps;
        this.systemApps = systemApps;
        this.disableApps = disableApps;
        this.handler = handler;
    }

    public void getpackageSize() {
        cAppDetails = new AppDetails((Activity) context);
        res = cAppDetails.getPackages();
        if (res == null) {
            return;
        }
        sizeMap.clear();
        for (int m = 0; m < res.size(); m++) {
            // Create object to access Package Manager
            PackageManager pm = context.getPackageManager();
            Method getPackageSizeInfo;
            try {
                getPackageSizeInfo = pm.getClass().getMethod(
                        "getPackageSizeInfo", String.class,
                        IPackageStatsObserver.class);
                getPackageSizeInfo.invoke(pm, res.get(m).pname,
                        new cachePackState()); //Call the inner class
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    /*
    * Inner class which will get the data size for application
    * */
    private class cachePackState extends IPackageStatsObserver.Stub {
        @Override
        public void onGetStatsCompleted(PackageStats pStats, boolean succeeded)
                throws RemoteException {
            packageSize = pStats.cacheSize + pStats.codeSize + pStats.dataSize + pStats.externalCacheSize + pStats.externalCodeSize + pStats.externalDataSize + pStats.externalMediaSize + pStats.externalObbSize;
            sizeMap.put(pStats.packageName + "", packageSize);
            if (userApps != null) {
                for (ListDataBean bean : userApps) {
                    if (TextUtils.equals(bean.mInfo.packageName, pStats.packageName)) {
                        bean.mTotalSize = packageSize;
                        bean.mFileSize = Formatter.formatFileSize(context, bean.mTotalSize);
                    }
                }
            }
            if (systemApps != null) {
                for (ListDataBean bean : systemApps) {
                    if (TextUtils.equals(bean.mInfo.packageName, pStats.packageName)) {
                        bean.mTotalSize = packageSize;
                        bean.mFileSize = Formatter.formatFileSize(context, bean.mTotalSize);
                    }
                }
            }
            if (disableApps != null) {
                for (ListDataBean bean : disableApps) {
                    if (TextUtils.equals(bean.mInfo.packageName, pStats.packageName)) {
                        bean.mTotalSize = packageSize;
                        bean.mFileSize = Formatter.formatFileSize(context, bean.mTotalSize);
                    }
                }
            }
            if (handler != null) {
                handler.sendEmptyMessage(MainActivity.REFRESH_LIST);
            }
        }
    }
}