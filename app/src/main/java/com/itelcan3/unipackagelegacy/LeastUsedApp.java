package com.itelcan3.unipackagelegacy;

public class LeastUsedApp {
    private final String packageName;
    private final String appName;
    private final long totalTimeInForeground;
    private final long lastTimeUsed;
    private final long appSize;

    public LeastUsedApp(String packageName, String appName, long totalTimeInForeground, long lastTimeUsed, long appSize) {
        this.packageName = packageName;
        this.appName = appName;
        this.totalTimeInForeground = totalTimeInForeground;
        this.lastTimeUsed = lastTimeUsed;
        this.appSize = appSize;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getAppName() {
        return appName;
    }

    public long getTotalTimeInForeground() {
        return totalTimeInForeground;
    }

    public long getLastTimeUsed() {
        return lastTimeUsed;
    }

    public long getAppSize() {
        return appSize;
    }
}
