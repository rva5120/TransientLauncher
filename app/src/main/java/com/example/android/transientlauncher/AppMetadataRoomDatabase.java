package com.example.android.transientlauncher;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;



/*
Class           App Metadata Room Database
Description     API to create and use a Room database to store with AppMetadata objects.

Requirements    To use this as a library for another package, add modifiers to the methods.
                Currently: class and methods are package-private.
 */
@Database(entities = {AppMetadata.class}, version = 1, exportSchema = false)
abstract class AppMetadataRoomDatabase extends RoomDatabase {

    /** Attributes **/
    private static AppMetadataRoomDatabase INSTANCE;        // DB instance, static (only one)
    abstract AppMetadataDao appMetadataDao();        // DAO getter (instance of an interface)
    private static int recordCount;
    private static Boolean recordBooleanResult;
    private static final String LOG_TAG = AppMetadataRoomDatabase.class.getSimpleName();

    // Room Callback
    private static RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            //new DeleteDbAsync(INSTANCE).execute();
            // Insert anything desired to do every time the db opens
        }
    };




    /** Async Tasks **/
    /*
    Name                DeleteDbAsync
    Description         Delete everything from DB
    */
    private static class DeleteDbAsync extends AsyncTask<Void, Void, Void> {

        // Attributes
        private final AppMetadataDao mDao;

        // Constructor
        DeleteDbAsync(AppMetadataRoomDatabase db) {
            mDao = db.appMetadataDao();
        }

        // Methods
        @Override
        protected Void doInBackground(final Void... params) {
            mDao.deleteAll();
            return null;
        }
    }


    /*
    Name                GetAllAppsFromDbAsync
    Description         Get all apps from DB
    */
    private static class GetAllAppsFromDbAsync extends AsyncTask<Void, Void, List<AppMetadata>> {

        // Attributes
        private WeakReference<List<AppMetadata>> allApps;      // C Pointer-equivalent in Java
        private final AppMetadataDao mDao;

        // Constructors
        GetAllAppsFromDbAsync(List<AppMetadata> allApps, AppMetadataRoomDatabase db) {
            mDao = db.appMetadataDao();
            this.allApps = new WeakReference<>(allApps);
        }

        // Methods
        @Override
        protected List<AppMetadata> doInBackground(final Void... params) {
            return mDao.getAllApps();
        }

        protected void onPostExecute(List<AppMetadata> retrievedApps) {
            allApps.get().addAll(retrievedApps);
        }
    }


    /*
    Name                InsertAppToDbAsync
    Description         Insert an app to the DB
    */
    private static class InsertAppToDbAsync extends AsyncTask<Void, Void, Void> {

        // Attributes
        private final AppMetadataDao mDao;
        private AppMetadata app;

        // Constructor
        InsertAppToDbAsync(AppMetadata app, AppMetadataRoomDatabase db) {
            mDao = db.appMetadataDao();
            this.app = app;
        }

        // Methods
        @Override
        protected Void doInBackground(final Void... params) {
            mDao.insert(app);
            return null;
        }
    }


    /*
    Name                RecordCountDbAsync
    Description         Return the number of records in the DB
     */
    private static class RecordCountDbAsync extends AsyncTask<Void, Void, Void> {

        // Attributes
        private final AppMetadataDao mDao;

        // Constructor
        RecordCountDbAsync(AppMetadataRoomDatabase db) {
            mDao = db.appMetadataDao();
        }

        // Methods
        @Override
        protected Void doInBackground(final Void... params) {
            recordCount = mDao.recordCount();
            return null;
        }

    }


    /*
    Name                UpdateRecordFieldDbAsync
    Description         For a given record (package), updates a field with a new value
     */
    private static class UpdateRecordFieldDbAsync extends AsyncTask<Void, Void, Void> {

        // Attributes
        private final AppMetadataDao mDao;
        private String packageName;
        private String flagType;
        private Boolean flag;

        // Constructor
        UpdateRecordFieldDbAsync(AppMetadataRoomDatabase db, String packageName, String flagType, Boolean flag) {
            mDao = db.appMetadataDao();
            this.packageName = packageName;
            this.flagType = flagType;
            this.flag = flag;
        }

        // Methods
        @Override
        protected Void doInBackground(final Void... params) {

            if (flagType.equals("enabledApp")) {
                // Update enable field
                mDao.updateEnableField(packageName, flag);
            }
            else if (flagType.equals("transientApp")) {
                //Update transient field
                mDao.updateTransientField(packageName, flag);
            }
            else {
                Log.e(LOG_TAG, "* ERROR * ---- Error wrong flagType when trying to update DB record.");
            }

            return null;
        }
    }


    /*
    Name                GetRecordFieldDbAsync
    Description         Returns the value of a field for a given record in the DB

    Notes               Fields allowed are enabledApp and transientApp only (Booleans).
     */
    private static class GetRecordFieldDbAsync extends AsyncTask<Void, Void, Void> {

        // Attributes
        private final AppMetadataDao mDao;
        private String packageName;
        private String recordType;      // Boolean enabledApp or transientApp only!

        // Constructor
        GetRecordFieldDbAsync(AppMetadataRoomDatabase db, String packageName, String recordType) {
            mDao = db.appMetadataDao();
            this.packageName = packageName;
            this.recordType = recordType;
        }

        // Methods
        @Override
        protected Void doInBackground(final Void... params) {

            recordBooleanResult = mDao.getBooleanRecordField(packageName, recordType);
            return null;
        }
    }




    /** Methods **/
    /*
    Name                getDatabase
    Description         Returns the instance of the launcher's DB
     */
    static AppMetadataRoomDatabase getDatabase(final Context context) {

        // If the instance of the DB is null, synchronize and check again
        if (INSTANCE == null) {
            synchronized (AppMetadataRoomDatabase.class) {

                // If the instance of the DB is still null, create a new DB
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppMetadataRoomDatabase.class, "apps_database")
                            .allowMainThreadQueries()
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }

        return INSTANCE;
    }


    /*
    Name                deleteDb
    Description         Delete all entries of a DB
    */
    void deleteDb() {

        // Delete all entries
        new DeleteDbAsync(INSTANCE).execute();
    }


    /*
    Name                getAllApps
    Description         Returns a list of all apps in the DB
     */
    List<AppMetadata> getAllApps() {

        // List to store retrieved apps
        List<AppMetadata> list = new ArrayList<>();

        // Get values from the DB and store them in the list
        new GetAllAppsFromDbAsync(list, INSTANCE).execute();

        return list;
    }


    /*
    Name                insertApp
    Description         Insert an app in the DB
     */
    void insertApp(AppMetadata app) {

        // Insert app
        new InsertAppToDbAsync(app, INSTANCE).execute();
    }


    /*
    Name                isEmpty
    Description         Returns whether the DB is empty or not
     */
    Boolean isEmpty() {

        // Get current record count (updates recordCount)
        new RecordCountDbAsync(INSTANCE).execute();

        if (recordCount > 0) {
            return Boolean.FALSE;
        } else {
            return Boolean.TRUE;
        }
    }


    /*
    Name                updateAppEnabled
    Description         Updates an app record's enabled flag accordingly in the DB
     */
    void updateAppEnabled(String packageName, Boolean flag) {

        // Update the enabled flag
        new UpdateRecordFieldDbAsync(INSTANCE, packageName, "enabledApp", flag).execute();
    }


    /*
    Name                enabledFlag
    Description         Returns the enabled flag value for a package in the DB
     */
    Boolean enabledFlag(String packageName) {

        // Check the enabled flag (updates recordResult)
        new GetRecordFieldDbAsync(INSTANCE, packageName, "enabledApp").execute();
        return recordBooleanResult;
    }
}