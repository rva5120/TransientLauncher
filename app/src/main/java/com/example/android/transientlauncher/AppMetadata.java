package com.example.android.transientlauncher;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;



/*
Class           App Metadata
Description     Definition of the metadata of an app. Used as the entry definition for the database.
 */
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
    AppMetadata(String appName, @NonNull String packageName, Boolean enabledApp, Boolean transientApp) {
        this.appName = appName;
        this.packageName = packageName;
        this.enabledApp = enabledApp;
        this.transientApp = transientApp;
    }


    // Methods
    String getAppName() {
        return this.appName;
    }
    String getPackageName() {
        return this.packageName;
    }
    Boolean getEnabledApp() {
        return this.enabledApp;
    }
    Boolean getTransientApp() {
        return this.transientApp;
    }

    void setAppName(String name) {
        this.appName = name;
    }
    void setPackageName(String package_name) {
        this.packageName = package_name;
    }
    void setEnabledApp(Boolean en) {
        this.enabledApp = en;
    }
    void setTransientApp(Boolean tr) {
        this.transientApp = tr;
    }
}
