package com.example.android.transientlauncher;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    /** Attributes **/
    // Modes and Flags
    private final Boolean DEMO_MODE = Boolean.TRUE;
    private final String LOG_TAG = "TransientLauncher/Main";

    // Managers and Databases
    private PackageManager packageManager;
    private AppMetadataRoomDatabase database;
    private TransiencyManager transiencyManager;

    // Structures
    private List<AppMetadata> appList;
    private ListView listView;




    /** Methods **/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(LOG_TAG, "** INFO **    onCreate --> Setting up PM, DB and TM.");

        // Stop background process to check for closed apps

        // Set Package Manager
        packageManager = getPackageManager();

        // Get Room database
        database = AppMetadataRoomDatabase.getDatabase(getApplicationContext());

        // Instantiate Transiency Manager
        transiencyManager = new TransiencyManager(packageManager, database);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(LOG_TAG, "** INFO **    onResume --> Setup local list, listview and click listener.");

        // Every time the user is about to see the app list, get the updated list.

        // Load apps from DB or PM on list, and disables closed apps if first time running
        loadAppsOnLocalList();

        // Load list of apps on the ListView object
        loadAppsOnListView();

        // Listener to service user clicks
        addClickListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(LOG_TAG, "** INFO **    onDestroy --> Disables all apps that are not running.");

        // DEMO MODE - Disable apps that are not running and are still enabled
        if (DEMO_MODE) {

            for (AppMetadata app : appList) {

                if (app.getTransientApp()) {

                    if (app.getEnabledApp() && transiencyManager.isAppRunning(app.getPackageName())) {

                        // Disable app
                        Boolean disable_success = transiencyManager.disableApp(app.getPackageName());
                        if (disable_success) {
                            app.setEnabledApp(Boolean.FALSE);
                        } else {
                            Log.e(LOG_TAG, "**** ERROR ****  Error disabling " + app.getPackageName());
                        }

                    }
                }
            }
        }

        // Start the background process to listen for closing apps
    }


    /*
    Name                loadAppsOnLocalList
    Description         Get apps from PM or DB and save them to our local List variable
                        and disable apps if this is the first time the launcher is running.
     */
    private void loadAppsOnLocalList() {

        Log.d(LOG_TAG, "** INFO **    Loading apps on our local List object...");

        // If this is the first time the launcher is used, then populate the
        // list of apps using the Package Manager. Otherwise, use the DB.
        appList = new ArrayList<>();
        if (database.isEmpty()) {

            // Get launchable apps from PM and load the DB
            appList.addAll(transiencyManager.getLaunchableAppsAndLoadDb());

            // Since this is the first time that the launcher is used, disable apps that are not running
            Boolean disable_success;
            for (int pos = 0; pos < appList.size(); pos++) {

                // Get the app metadata
                AppMetadata app = appList.get(pos);

                // Disable transient apps that are not running
                if (app.getTransientApp() && transiencyManager.isAppRunning(app.getPackageName())) {
                    disable_success = transiencyManager.disableApp(app.getPackageName());
                    if (disable_success) {
                        app.setEnabledApp(Boolean.FALSE);
                    } else {
                        Toast.makeText(MainActivity.this, "Error Disabling App! Exiting Launcher...", Toast.LENGTH_LONG).show();
                        Log.e(LOG_TAG, "* ERROR *   Couldn't disable " + app.getAppName());
                        MainActivity.this.finish();
                    }
                }
            }

        } else {

            // Consult the added/removed packages file from the background process

            // Add/Remove apps to/from the database

            // Load all (new and existing) apps to display to the user
            appList.addAll(database.getAllApps());

            // Wait for the background DB process to be done...
        }

    }


    /*
    Name                loadAppsOnListView
    Description         Load the ListView with the list of launchable apps
     */
    private void loadAppsOnListView() {

        Log.d(LOG_TAG, "** INFO **   Loading apps on the ListView object...");

        // Setup an ArrayAdapter, used to populate the ListView
        ArrayAdapter<AppMetadata> adapter = new ArrayAdapter<AppMetadata>(this, R.layout.list_item, appList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                // If this is the first time the ListView is populated..
                if (convertView == null) {
                    // Get the objects inside of the list_item.xml layout definition
                    convertView = getLayoutInflater().inflate(R.layout.list_item, null);
                }

                // Set name for the app_name TextView object
                TextView appName = convertView.findViewById(R.id.app_name);
                appName.setText(appList.get(position).getAppName());

                // Set package name for the package_name TextView object
                TextView packageName = convertView.findViewById(R.id.package_name);
                packageName.setText(appList.get(position).getPackageName());

                return convertView;
            }
        };

        // Setup a ListView to display the apps, and display them in the ListView list_apps object
        listView = findViewById(R.id.list_apps);
        listView.setAdapter(adapter);
    }


    /*
    Name                addClickListener
    Description         Setup a listener to open the selected app from the ListView
     */
    private void addClickListener() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // Get the package name & transient flag of the desired app
                String packageName = appList.get(position).getPackageName();
                Boolean transientApp = appList.get(position).getTransientApp();

                Log.d(LOG_TAG, "** INFO **   Servicing user click on " + packageName);

                // Display a helpful Toast
                if (transientApp) {
                    Toast.makeText(MainActivity.this, "Opening Transient App!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Opening a NON-Transient App..", Toast.LENGTH_LONG).show();
                }

                // Enable the app if necessary
                Boolean enable_success;
                if (transiencyManager.isAppDisabled(packageName)) {
                    enable_success = transiencyManager.enableApp(packageName);
                    if (enable_success) {
                        appList.get(position).setEnabledApp(Boolean.TRUE);
                    } else {
                        Toast.makeText(MainActivity.this, "Error opening transient app... Exiting Launcher.", Toast.LENGTH_LONG).show();
                        Log.e(LOG_TAG, "* ERROR *   Couldn't enable " + packageName);
                        MainActivity.this.finish();
                    }
                }

                // Run the app
                Intent intent = packageManager.getLaunchIntentForPackage(packageName);
                MainActivity.this.startActivity(intent);
            }
        });
    }

}
