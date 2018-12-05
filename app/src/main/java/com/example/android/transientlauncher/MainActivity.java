package com.example.android.transientlauncher;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
    private final Boolean demoMode = Boolean.TRUE;

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

        // Every time the user is about to see the app list, get the updated list.

        // Load apps from DB or PM on list
        loadAppsOnLocalList();

        // Load list of apps on the ListView object
        loadAppsOnListView();

        // Listener to service user clicks
        addClickListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Start the background process to listen for closing apps
    }


    /*
    Name
    Description
     */
    private void loadAppsOnLocalList() {

        // If this is the first time the launcher is used, then populate the
        // list of apps using the Package Manager. Otherwise, use the DB.
        appList = new ArrayList<>();
        if (database.isEmpty() != Boolean.FALSE) {

            // Get launchable apps from PM
            appList.addAll(transiencyManager.getLaunchableApps());

        } else {

            // Consult the added/removed packages file from the background process

            // Add/Remove apps to/from the database

            // Load all (new and existing) apps to display to the user
            appList.addAll(database.getAllApps());

            // Wait for the background DB process to be done...
        }

        // Disable transient apps
        for (int pos = 0; pos < appList.size(); pos++) {
            AppMetadata app = appList.get(pos);
            if (app.getTransientApp() == Boolean.TRUE && app.getEnabledApp() == Boolean.FALSE) {
                transiencyManager.disableApp(app.getPackageName());
                app.setEnabledApp(Boolean.FALSE);
            }
        }

    }


    /*
    Name            loadAppsOnListView
    Description     Load the ListView with the list of launchable apps
     */
    private void loadAppsOnListView() {

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
    Name            addClickListener
    Description     Setup a listener to open the selected app from the ListView
     */
    private void addClickListener() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // Get the package name & transient flag of the desired app
                String packageName = appList.get(position).getPackageName();
                Boolean transientApp = appList.get(position).getTransientApp();

                // Display a helpful Toast
                if (transientApp) {
                    Toast.makeText(MainActivity.this, "Opening Transient App!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "Opening a NON-Transient App..", Toast.LENGTH_LONG).show();
                }

                // Enable the app if necessary
                if (transiencyManager.isAppDisabled(packageName)) {
                    transiencyManager.enableApp(packageName);
                }

                // Run the app
                Intent intent = packageManager.getLaunchIntentForPackage(packageName);
                MainActivity.this.startActivity(intent);
            }
        });
    }

}
