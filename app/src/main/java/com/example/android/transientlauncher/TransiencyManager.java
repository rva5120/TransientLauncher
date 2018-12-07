package com.example.android.transientlauncher;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/*
Class           Transient Manager
Description     API to enable system settings or launchers to enforce application transiency.

Requirements    This class requires root privileges. To soften this requirement, make this application
                owner the system. This will allow you to modify the code where, instead of using a sudo
                shell, you can replace the shell commands with java code to perform similar operations.

Notes           Class and methods here are public, in order to be reachable outside of the package.
 */
public class TransiencyManager {

    /** Attributes **/
    private PackageManager packageManager;
    private AppMetadataRoomDatabase database;
    private final String LOG_TAG = TransiencyManager.class.getSimpleName();

    private final Boolean DEMO_MODE = Boolean.TRUE;




    /** Constructor **/
    public TransiencyManager(PackageManager pm, AppMetadataRoomDatabase db) {
        this.packageManager = pm;

        // Should I instantiate it here instead of in the Main activity???
        this.database = db;
    }




    /** Methods **/
    /*
    Name                getLaunchableApps
    Description         Returns a list of AppMetadata objects of launchable apps and add them to the DB
     */
    public List<AppMetadata> getLaunchableAppsAndLoadDb() {

        // Define an intent with the MAIN action and LAUNCHER category
        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);

        // Using the intent above, ask the PM to look for apps that have at least
        // one activity that subscribes to it. Meaning that, these apps, are capable
        // of handling the MAIN+LAUNCHER request. In this case, this means that the
        // app is launchable.
        List<ResolveInfo> launchableApps = packageManager.queryIntentActivities(i, 0);

        // Make a List of AppMetadata objects from the info returned by the PM
        List<AppMetadata> apps = new ArrayList<>();
        for (ResolveInfo resolveInfo: launchableApps) {

            // Get App name and Package name
            String name = (String) resolveInfo.loadLabel(packageManager);
            String packageName = resolveInfo.activityInfo.packageName;

            // For now... If the app contains the word "google" or "android" on it, is it not transient
            Boolean tran = Boolean.FALSE;
            if (DEMO_MODE) {

                Log.d(LOG_TAG, "** DEBUG **   Package name " + packageName);
                if (packageName.equals("com.example.android.hellotoast") || packageName.equals("com.facebook.katana")
                        || packageName.equals("com.snapchat.android") || packageName.equals("com.instagram.android")) {
                    tran = Boolean.TRUE;
                    Log.d(LOG_TAG, "** INFO **    " + packageName + " is transient!!");
                }

            } else {

                if (packageName.contains("com.google") || packageName.contains("com.android") || packageName.contains("transientlauncher")) {
                    tran = Boolean.FALSE;
                } else {
                    tran = Boolean.TRUE;
                }

            }

            // Create the app object
            AppMetadata app = new AppMetadata(name, packageName, Boolean.TRUE, tran);

            // Save the app object to the list
            apps.add(app);

            // Add the app to the DB
            database.insertApp(app);

        }

        // Return the list
        return apps;
    }



    /*
    Name                isAppEnabled
    Description         Returns TRUE if the enabled flag is set in the DB for a given app
     */
    public Boolean isAppDisabled(String packageName) {

        // Check if the app is enabled in the DB
        Boolean enabled = database.enabledFlag(packageName);

        if (enabled) {
            return Boolean.FALSE;
        } else {
            return Boolean.TRUE;
        }
    }



    /*
    Name                enableApp
    Description         Enables an app and updates DB flag (enable: make APK file accessible to the system)
     */
    public Boolean enableApp(String packageName) {

        // Get the data directory of the package
        PackageInfo packageInfo;
        try {
            packageInfo = packageManager.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, "NameNotFoundException - Error finding data path for " + packageName);
            return Boolean.FALSE;
        }

        // Get into a sudo shell & execute the enabling commands
        Process process;
        try {
            // Start sudo shell
            process = Runtime.getRuntime().exec("su");

            // Setup a pipe to send commands to the sudo shell
            DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());

            // Get the path to the data folder for this package (remove base.apk from the path)
            String apkDirectory = packageInfo.applicationInfo.sourceDir;
            //packageDirectory = packageDirectory.replace("base.apk", "");

            // Add Command -> Rename the dummy file to base.apk
            //outputStream.writeBytes("mv " + packageDirectory + "/dummy " + packageDirectory + "/base.apk\n");

            // Add Command -> Set the permissions for this package
            //  Enable: rw-r--r--
            outputStream.writeBytes("chmod uga+r " + apkDirectory + "\n");

            // Add command -> Set the owner for this package
            //  Enable: rwxr-xr-x system system com.package.example
            //outputStream.writeBytes("chown system.system " + packageDirectory + "\n");

            // Add Command -> Exit the shell
            outputStream.writeBytes("exit\n");

            // Execute the commands
            outputStream.flush();

            // Check the output of the execution
            try {

                // Wait for the process to finish executing the commands
                process.waitFor();

                // Check for errors
                if (process.exitValue() != 255) {
                    Log.d(LOG_TAG, "Successfully enabled " + packageName);
                } else {
                    Log.e(LOG_TAG, "Error enabling " + packageName);
                    return Boolean.FALSE;
                }
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "Interrupted Exception - Error enabling " + packageName);
                e.printStackTrace();
                return Boolean.FALSE;
            }

            // Close the stream
            outputStream.close();

        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException - Error enabling " + packageName);
            e.printStackTrace();
            return Boolean.FALSE;
        }

        // Set the app as enabled
        setAppEnabled(packageName);
        return Boolean.TRUE;
    }



    /*
    Name                disableApp
    Description         Disables an app and updates DB flag (disable: make APK file inaccessible to the system)
     */
    public Boolean disableApp(String packageName) {

        // Get the data directory of the package
        PackageInfo packageInfo;
        try {
            packageInfo = packageManager.getPackageInfo(packageName, 0);
            Log.d(LOG_TAG, "Package path: " + packageInfo.applicationInfo.dataDir);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, "NameNotFoundException - Error finding data path for " + packageName);
            return Boolean.FALSE;
        }

        // Get into a sudo shell & execute the disabling commands
        Process process;
        try {
            // Start sudo shell
            process = Runtime.getRuntime().exec("su");

            // Setup a pipe to send commands to the sudo shell
            DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());

            // Get the path to the data folder of this package
            String apkDirectory = packageInfo.applicationInfo.sourceDir;
            //packageDirectory = packageDirectory.replace("base.apk", "");

            // Add Command -> Rename the base.apk file to dummy
            //outputStream.writeBytes("mv " + packageDirectory + "/base.apk " + packageDirectory + "/dummy\n");

            // Add Command -> Set the permissions for this package
            //  Disable: -w-------
            outputStream.writeBytes("chmod uga-r " + apkDirectory + "\n");

            // Add Command -> Set the owner for this package
            //  Disable: rwx------ root root com.package.example
            //outputStream.writeBytes("chwon root.root " + packageDirectory + "\n");

            // Add Command -> Exit the shell
            outputStream.writeBytes("exit\n");

            // Execute the commands
            outputStream.flush();

            // Check the output of the execution
            try {

                // Wait for the process to finish
                process.waitFor();

                if (process.exitValue() != 255) {
                    Log.d(LOG_TAG, "Successfully disabled " + packageName);
                } else {
                    Log.e(LOG_TAG, "Error disabling " + packageName);
                    return Boolean.FALSE;
                }
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "InterruptedException - Error disabling " + packageName);
                e.printStackTrace();
                return Boolean.FALSE;
            }

            // Close the stream
            outputStream.close();

        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException - Error disabling " + packageName);
            e.printStackTrace();
            return Boolean.FALSE;
        }

        // Set app disabled flag
        setAppDisabled(packageName);
        return Boolean.TRUE;
    }



    /*
    Name                setAppEnabled
    Description         Sets the enabled flag to TRUE in the database for a given app
     */
    private void setAppEnabled(String packageName) {

        // Update the database
        database.updateAppEnabled(packageName, Boolean.TRUE);
    }



    /*
    Name                setAppDisabled
    Description         Sets the enabled flag to FALSE in the database for a given app
     */
    private void setAppDisabled(String packageName) {

        // Update the database
        database.updateAppEnabled(packageName, Boolean.FALSE);
    }



    /*
    Name                getRunningPackages
    Description         Returns a list of strings of package names of running packages.
     */
    private List<String> getRunningPackages() {

        // Setup a list
        ArrayList<String> runningPackages = new ArrayList<>();

        // Execute the process status command to get a list of running packages
        Process process;
        try {

            // Setup the process to execute ps
            process = Runtime.getRuntime().exec("ps --name com");

            // Setup the Input Stream Reader to receive the results of the ps command
            BufferedReader results = new BufferedReader(new InputStreamReader(process.getInputStream()));

            // Setup regex pattern to get the package name for each running process
            Pattern packageStructure = Pattern.compile("com\\.[a-z,0-9]*\\.[a-z,0-9]*\\.[a-z,0-9]*");
            Matcher matcher;

            // Read the buffer with the results line by line
            String line;
            while((line = results.readLine()) != null) {

                // Find the matching strings to our patters
                matcher = packageStructure.matcher(line);

                // Save the matching package name
                if (matcher.find()) {

                    // Get the package name
                    line = matcher.group(0);

                    // Save the package name
                    runningPackages.add(line);

                    Log.d(LOG_TAG, "** INFO **   " + line + " is running");
                } else {
                    //Log.d(LOG_TAG, "** INFO **   NON-MATCH - " + line + " is also running...");
                }
            }

            // Close the stream
            results.close();

        } catch (IOException e) {
            Log.e(LOG_TAG, "* ERROR *   Error IO Exception getting running packages!!");
            e.printStackTrace();
        }

        // Return the list of running packages
        return runningPackages;
    }



    /*
    Name                isAppRunning
    Description         Checks if the app is running, using the "ps" command
     */
    public Boolean isAppRunning(String packageName) {

        // Get list of running apps
        ArrayList<String> runningPackages = new ArrayList<>();
        runningPackages.addAll(getRunningPackages());

        // Check to see if this package is running
        return runningPackages.contains(packageName);
    }
}