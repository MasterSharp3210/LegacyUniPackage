package com.itelcan3.unipackagelegacy;

import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.os.storage.StorageManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class LeastUsedAppsReader {

    private final Context context;
    private final UsageStatsManager usageStatsManager;

    public LeastUsedAppsReader(Context context) {
        this.context = context.getApplicationContext();
        this.usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
    }

    public List<LeastUsedApp> getLeastUsedApps(int days) {
        List<LeastUsedApp> resultList = new ArrayList<>();
        if (usageStatsManager == null) {
            return resultList;
        }

        long endTime = System.currentTimeMillis();
        long cutoffTime = endTime - (days * 24L * 60L * 60L * 1000L);

        Map<String, UsageStats> statsMap = usageStatsManager.queryAndAggregateUsageStats(cutoffTime, endTime);

        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> installedApps = pm.getInstalledApplications(0);
        String selfPackageName = context.getPackageName();

        for (ApplicationInfo appInfo : installedApps) {
            boolean isSystem = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            boolean isSelf = appInfo.packageName.equals(selfPackageName);

            if (isSystem || isSelf) {
                continue;
            }

            try {
                String packageName = appInfo.packageName;
                UsageStats usageStat = statsMap != null ? statsMap.get(packageName) : null;

                long totalTime = usageStat != null ? usageStat.getTotalTimeInForeground() : 0L;
                long lastUsed = usageStat != null ? usageStat.getLastTimeUsed() : 0L;

                boolean isInactive = (lastUsed == 0L || lastUsed < cutoffTime);
                if (!isInactive) {
                    continue;
                }

                String appName = pm.getApplicationLabel(appInfo).toString();
                long totalSize = calculateAppSize(appInfo, packageName);

                resultList.add(new LeastUsedApp(
                        packageName,
                        appName,
                        totalTime,
                        lastUsed,
                        totalSize
                ));
            } catch (Exception ignored) {
            }
        }

        Collections.sort(resultList, new Comparator<LeastUsedApp>() {
            @Override
            public int compare(LeastUsedApp app1, LeastUsedApp app2) {
                return Long.compare(app1.getLastTimeUsed(), app2.getLastTimeUsed());
            }
        });

        return resultList;
    }

    private long calculateAppSize(ApplicationInfo appInfo, String packageName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                StorageStatsManager storageStatsManager = (StorageStatsManager) context.getSystemService(Context.STORAGE_STATS_SERVICE);
                if (storageStatsManager != null) {
                    StorageStats storageStats = storageStatsManager.queryStatsForPackage(
                            StorageManager.UUID_DEFAULT,
                            packageName,
                            Process.myUserHandle()
                    );
                    return storageStats.getAppBytes() + storageStats.getDataBytes() + storageStats.getCacheBytes();
                }
            } catch (Exception ignored) {
            }
        }
        return getApkFileSize(appInfo);
    }

    private long getApkFileSize(ApplicationInfo appInfo) {
        if (appInfo.sourceDir != null) {
            File file = new File(appInfo.sourceDir);
            if (file.exists()) {
                return file.length();
            }
        }
        return 0L;
    }
}
