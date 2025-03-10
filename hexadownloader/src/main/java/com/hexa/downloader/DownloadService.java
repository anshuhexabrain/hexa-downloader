package com.hexa.downloader;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.hexa.downloader.callback.DownloadManager;
import com.hexa.downloader.config.Config;

import java.util.List;

public class DownloadService extends Service {

    private static final String TAG = "DownloadService";
    public static DownloadManager downloadManager;

    public static DownloadManager getDownloadManager(Context context) {
        return getDownloadManager(context, null);
    }

    public static DownloadManager getDownloadManager(Context context, Config config) {
        if (!isServiceRunning(context)) {
            Intent downloadSvr = new Intent(context, DownloadService.class);
            context.startService(downloadSvr);
        }
        if (DownloadService.downloadManager == null) {
            DownloadService.downloadManager = DownloadManagerImpl.getInstance(context, config);
        }
        return downloadManager;
    }

    private static boolean isServiceRunning(Context context) {
        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningServiceInfo> serviceList = activityManager
                .getRunningServices(Integer.MAX_VALUE);

        if (serviceList == null || serviceList.size() == 0) {
            return false;
        }

        for (int i = 0; i < serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().equals(
                    DownloadService.class.getName())) {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (downloadManager != null) {
            downloadManager.destroy();
            downloadManager = null;
        }
        super.onDestroy();
    }
}
