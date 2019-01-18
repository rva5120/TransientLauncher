package com.example.android.transientlauncher;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    /** Attributes **/
    // Modes and Flags
    private final Boolean DEMO_MODE = Boolean.TRUE;
    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private Boolean enabledAllSelected = Boolean.FALSE;

    // Managers and Databases
    private PackageManager packageManager;
    private AppMetadataRoomDatabase database;
    private TransiencyManager transiencyManager;

    // Structures
    private List<AppMetadata> appList;
    private SwipeMenuListView listView;
    private SwipeMenuCreator mMenuCreator;




    /** Methods **/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(LOG_TAG, "** INFO **    onCreate --> Setting up PM, DB and TM.");

        // TO DO [Stop background process to check for closed apps]

        // Set Package Manager
        packageManager = getPackageManager();

        // Get Room database
        database = AppMetadataRoomDatabase.getDatabase(getApplicationContext());

        // Instantiate Transiency Manager
        transiencyManager = new TransiencyManager(packageManager, database);

        mMenuCreator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {

                // create "delete" item
                SwipeMenuItem deleteItem = new SwipeMenuItem(
                        getApplicationContext());
                // set item background
                deleteItem.setBackground(new ColorDrawable(Color.rgb(0xF9,
                        0x3F, 0x25)));
                // set item width
                deleteItem.setWidth(200);
                // set a icon
                deleteItem.setIcon(R.drawable.ic_disable);
                // add to menu
                menu.addMenuItem(deleteItem);
            }
        };

        BroadcastReceiver powerOffReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Get updated app list
                appList.clear();
                appList.addAll(database.getAllApps());

                // Enable all disabled apps before turning off the phone
                for (AppMetadata app: appList) {
                    if (app.getEnabledApp() == Boolean.FALSE) {
                        transiencyManager.enableApp(app.getPackageName());
                        transiencyManager.setAppDisabled(app.getPackageName());
                        Log.d(LOG_TAG, "Enabling App " + app.getPackageName() + " on Shutdown!");
                    }
                }

            }
        };

        IntentFilter powerOffFilter = new IntentFilter();
        powerOffFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
        powerOffFilter.addAction("android.intent.action.QUICKBOOT_POWEROFF");

        this.getApplicationContext().registerReceiver(powerOffReceiver, powerOffFilter);
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

        // DEMO MODE - Disable all apps that are not running.
        if (DEMO_MODE) {
            for(AppMetadata app: appList) {

                // Disable apps that are transient, enabled and are not running right now (no process)
                if (app.getTransientApp() && app.getEnabledApp() && transiencyManager.isAppRunning(app.getPackageName()) == Boolean.FALSE) {
                    Boolean disable_success = transiencyManager.disableApp(app.getPackageName());
                    if (disable_success) {
                        app.setEnabledApp(Boolean.FALSE);
                        Toast.makeText(MainActivity.this, "onResume: Disabled " + app.getAppName(), Toast.LENGTH_LONG).show();
                        Log.d(LOG_TAG, "** INFO **    Disabled " + app.getAppName());
                    } else {
                        Toast.makeText(MainActivity.this, "onResume: Error Disabling App! Exiting Launcher...", Toast.LENGTH_LONG).show();
                        Log.e(LOG_TAG, "* ERROR *   Couldn't disable " + app.getAppName());
                        MainActivity.this.finish();
                    }
                }
            }
        }

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
        if (DEMO_MODE && enabledAllSelected == Boolean.FALSE) {

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

        // TO DO [Start the background process to listen for closing apps]
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

            Log.d(LOG_TAG, "** INFO **    Database is empty, so we are filling it up...");

            // Get launchable apps from PM and load the DB
            appList.addAll(transiencyManager.getLaunchableAppsAndLoadDb());

            Log.d(LOG_TAG, "** DEBUG ** DATABASE SHOULD NOW HAVE 2 ITEMS... " + database.getRecordCount());

            // Since this is the first time that the launcher is used, disable apps that are not running
            Boolean disable_success;
            for (int pos = 0; pos < appList.size(); pos++) {

                // Get the app metadata
                AppMetadata app = appList.get(pos);
                Log.d(LOG_TAG, "** DEBUG **    Disable? " + app.getAppName() + " -- Transient? " + app.getTransientApp());

                // Disable transient apps that are not running
                if (app.getTransientApp() == Boolean.TRUE && transiencyManager.isAppRunning(app.getPackageName()) == Boolean.FALSE) {
                    disable_success = transiencyManager.disableApp(app.getPackageName());
                    if (disable_success) {
                        app.setEnabledApp(Boolean.FALSE);
                        Toast.makeText(MainActivity.this, "Disabled " + app.getAppName(), Toast.LENGTH_LONG).show();
                        Log.d(LOG_TAG, "** INFO **    Disabled " + app.getAppName());
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

        // Get the most up to date list from the DB
        appList.addAll(database.getAllApps());

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
        listView = (SwipeMenuListView) findViewById(R.id.list_apps);
        listView.setAdapter(adapter);
        listView.setMenuCreator(mMenuCreator);
        listView.setSwipeDirection(SwipeMenuListView.DIRECTION_RIGHT);
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

                //Read App's permissions and give the user a warning of what it can do
                try {
                    String permissionWarning = "This application requires the permissions:";
                    PackageInfo info = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);

                    if (info.requestedPermissions != null) {

                        for (String permission : info.requestedPermissions){
                            permissionWarning = permissionWarning + "\n" + permission;
                        }

                    }

                    else {
                        permissionWarning = "This app requests no permissions!";
                    }

                    Toast.makeText(MainActivity.this, permissionWarning, Toast.LENGTH_LONG).show();
                }
                catch(Exception e){
                    Log.e(LOG_TAG, e.getMessage());
                }
            }
        });

        listView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {

                AppMetadata app = appList.get(position);

                String packageName = app.getPackageName();
                Log.d(LOG_TAG, "User swiped to reveal the close button on app " + packageName);

                if( app.getTransientApp() == Boolean.TRUE ) {

                    //Kill any activity related to this app using the package name
                    ActivityManager mActivityManager = (ActivityManager) MainActivity.this.getSystemService(Context.ACTIVITY_SERVICE);
                    mActivityManager.killBackgroundProcesses(packageName);

                    //Disable the package to prevent it from running until enabled through launcher
                    Boolean disable_success = transiencyManager.disableApp(packageName);


                    if(disable_success){
                        app.setEnabledApp(Boolean.FALSE);
                        Log.d(LOG_TAG, "App Successfully Disabled!");
                        Toast.makeText(MainActivity.this, "Exited and Disabled App: " + packageName, Toast.LENGTH_LONG).show();
                    }
                }


                return false;
            }
        });
    }


    /*
    Name                enableAllApps
    Description         Button handler to enable all apps (transiency mode off).
     */
    public void enableAllApps(View view) {

        // Get updated app list
        appList.clear();
        appList.addAll(database.getAllApps());

        // Enable all disabled apps
        for (AppMetadata app: appList) {
            if (app.getEnabledApp() == Boolean.FALSE) {
                transiencyManager.enableApp(app.getPackageName());
                app.setEnabledApp(Boolean.TRUE);
                Toast.makeText(MainActivity.this, "Enabled " + app.getAppName(), Toast.LENGTH_SHORT).show();
            }
        }

        // Keep the apps enabled on Destroy!
        enabledAllSelected = Boolean.TRUE;
    }
}
