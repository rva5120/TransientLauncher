package com.example.android.transientlauncher;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/*
Class           Transient Manager
Description     API to enable system settings or launchers to enforce application transiency.

Requirements    This class requires root privileges. To soften this requirement, make this application
                owner the system. This will allow you to modify the code where, instead of using a sudo
                shell, you can replace the shell commands with java code to perform similar operations.
 */
class TransiencyManager {

    /** Attributes **/
    private PackageManager packageManager;
    private AppMetadataRoomDatabase database;
    private final String LOG_TAG = "TransiencyManager";




    /** Constructor **/
    public TransiencyManager(PackageManager pm, AppMetadataRoomDatabase db) {
        this.packageManager = pm;

        // Should I instantiate it here instead of in the Main activity???
        this.database = db;
    }




    /** Methods **/
    /*
    Name            getLaunchableApps
    Description     Returns a list of AppMetadata objects of launchable apps
     */
    public List<AppMetadata> getLaunchableApps() {

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
            Boolean tran = Boolean.TRUE;
            if (name.contains("com.google") || name.contains("com.android") || name.contains("transientlauncher")) {
                tran = Boolean.FALSE;
            }

            AppMetadata app = new AppMetadata(name, packageName, Boolean.TRUE, tran);

            apps.add(app);
        }

        // Return the list
        return apps;
    }



    /*
    Name            isAppEnabled
    Description     Returns TRUE if the enabled flag is set in the DB for a given app
     */
    public Boolean isAppDisabled(String packageName) {

        // Check if the app is enabled in the DB
        return Boolean.TRUE;
    }



    /*
    Name            enableApp
    Description     Enables an app (enable: make APK file accessible to the system)
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

            // Get the path to the data folder for this package
            String packageDirectory = packageInfo.applicationInfo.dataDir;

            // Add Command -> Rename the dummy file to base.apk
            outputStream.writeBytes("mv " + packageDirectory + "/dummy " + packageDirectory + "/base.apk\n");

            // Add Command -> Set the permissions for this package
            //  Enable: rwxr-xr-x
            outputStream.writeBytes("chmod ga+rx " + packageDirectory + "\n");

            // Add command -> Set the owner for this package
            //  Enable: rwxr-xr-x system system com.package.example
            outputStream.writeBytes("chown system.system " + packageDirectory + "\n");

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
    Name            disableApp
    Description     Disables an app (disable: make APK file inaccessible to the system)
     */
    public Boolean disableApp(String packageName) {

        // Get the data directory of the package
        PackageInfo packageInfo;
        try {
            packageInfo = packageManager.getPackageInfo(packageName, 0);
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
            String packageDirectory = packageInfo.applicationInfo.dataDir;

            // Add Command -> Rename the base.apk file to dummy
            outputStream.writeBytes("mv " + packageDirectory + "/base.apk " + packageDirectory + "/dummy\n");

            // Add Command -> Set the permissions for this package
            //  Disable: rwx------
            outputStream.writeBytes("chmod ga-rwx " + packageDirectory + "\n");

            // Add Command -> Set the owner for this package
            //  Disable: rwx------ root root com.package.example
            outputStream.writeBytes("chwon root.root " + packageDirectory + "\n");

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
    Name            setAppEnabled
    Description     Sets the enabled flag to TRUE in the database for a given app
     */
    private void setAppEnabled(String packageName) {

        // Update the database
        database.updateAppEnabled(packageName, Boolean.TRUE);
    }



    /*
    Name            setAppDisabled
    Description     Sets the enabled flag to FALSE in the database for a given app
     */
    private void setAppDisabled(String packageName) {

        // Update the database
        database.updateAppEnable(packageName, Boolean.FALSE);
    }

}
