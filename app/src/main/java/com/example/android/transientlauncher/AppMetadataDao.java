package com.example.android.transientlauncher;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;



/*
Interface       App Metadata Dao (Data Access Object)
Description     Interface used by the database class to send SQL commands to interact with the database.
 */
@Dao
public interface AppMetadataDao {

    // Insert an app, replace if already exists
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AppMetadata app);

    // Delete all entries from the table
    @Query("DELETE FROM apps_table")
    void deleteAll();

    // Get all entries from the table
    @Query("SELECT * FROM apps_table")
    List<AppMetadata> getAllApps();

    // Get the number of entries in the table
    @Query("SELECT COUNT(*) FROM apps_table")
    int recordCount();

    // Update the enabled flag for a given package name
    @Query("UPDATE apps_table SET enabledApp=:flag WHERE packageName=:name")
    void updateEnableField(String name, Boolean flag);

    // Update the transient flag for a given package name
    @Query("UPDATE apps_table SET transientApp=:flag WHERE packageName=:name")
    void updateTransientField(String name, Boolean flag);

    // Get the enabled/transient flag value for a given package name
    @Query("SELECT :recordType FROM apps_table WHERE packageName=:name")
    Boolean getBooleanRecordField(String name, String recordType);
}
