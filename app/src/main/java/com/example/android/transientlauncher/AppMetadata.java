package com.example.android.transientlauncher;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity(tableName = "apps_table")
class AppMetadata {

    // Attributes
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "packageName")
    private String packageName;

    @ColumnInfo(name = "appName")
    private String appName;

    @ColumnInfo(name = "enabledApp")
    private Boolean enabledApp;

    @ColumnInfo(name = "transientApp")
    private Boolean transientApp;


    // Constructor
    public AppMetadata(String appName, @NonNull String packageName, Boolean enabledApp, Boolean transientApp) {
        this.appName = appName;
        this.packageName = packageName;
        this.enabledApp = enabledApp;
        this.transientApp = transientApp;
    }


    // Methods
    public String getAppName() {
        return this.appName;
    }
    public String getPackageName() {
        return this.packageName;
    }
    public Boolean getEnabledApp() {
        return this.enabledApp;
    }
    public Boolean getTransientApp() {
        return this.transientApp;
    }

    public void setAppName(String name) {
        this.appName = name;
    }
    public void setPackageName(String package_name) {
        this.packageName = package_name;
    }
    public void setEnabledApp(Boolean en) {
        this.enabledApp = en;
    }
    public void setTransientApp(Boolean tr) {
        this.transientApp = tr;
    }
}
