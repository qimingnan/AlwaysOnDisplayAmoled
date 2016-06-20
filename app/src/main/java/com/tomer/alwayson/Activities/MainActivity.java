package com.tomer.alwayson.Activities;

import android.Manifest;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.tomer.alwayson.ContextConstatns;
import com.tomer.alwayson.Prefs;
import com.tomer.alwayson.R;
import com.tomer.alwayson.SecretConstants;
import com.tomer.alwayson.Services.StarterService;
import com.tomer.alwayson.Services.WidgetUpdater;

import de.psdev.licensesdialog.LicensesDialog;
import de.psdev.licensesdialog.licenses.ApacheSoftwareLicense20;
import de.psdev.licensesdialog.model.Notice;
import de.psdev.licensesdialog.model.Notices;


public class MainActivity extends AppCompatActivity implements ContextConstatns {
    private Prefs prefs;
    private Intent starterServiceIntent;
    private Intent widgetUpdaterService;
    private IInAppBillingService mService;
    private ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(getApplicationContext());
        prefs.apply();
        if (!prefs.permissionGranting && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivity(new Intent(getApplicationContext(), Intro.class));
            finish();
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        handlePermissions();

        starterServiceIntent = new Intent(getApplicationContext(), StarterService.class);
        widgetUpdaterService = new Intent(getApplicationContext(), WidgetUpdater.class);

        handleBoolSimplePref((Switch) findViewById(R.id.cb_touch_to_stop), Prefs.KEYS.TOUCH_TO_STOP.toString(), prefs.touchToStop);
        handleBoolSimplePref((Switch) findViewById(R.id.cb_swipe_to_stop), Prefs.KEYS.SWIPE_TO_STOP.toString(), prefs.swipeToStop);
        handleBoolSimplePref((Switch) findViewById(R.id.cb_volume_to_stop), Prefs.KEYS.VOLUME_TO_STOP.toString(), prefs.volumeToStop);
        handleBoolSimplePref((Switch) findViewById(R.id.cb_back_button_to_stop), Prefs.KEYS.BACK_BUTTON_TO_STOP.toString(), prefs.backButtonToStop);
        handleBoolSimplePref((Switch) findViewById(R.id.cb_show_notification), Prefs.KEYS.SHOW_NOTIFICATION.toString(), prefs.showNotification);
        handleBoolSimplePref((Switch) findViewById(R.id.cb_enabled), Prefs.KEYS.ENABLED.toString(), prefs.enabled);
        handleBoolSimplePref((Switch) findViewById(R.id.cb_move), Prefs.KEYS.MOVE_WIDGET.toString(), prefs.moveWidget);
        handleBoolSimplePref((Switch) findViewById(R.id.switch_notifications_alert), Prefs.KEYS.NOTIFICATION_ALERTS.toString(), prefs.notificationsAlerts);
        handleBoolSimplePref((Switch) findViewById(R.id.switch_disable_volume_keys), Prefs.KEYS.DISABLE_VOLUME_KEYS.toString(), prefs.disableVolumeKeys);
        handleSeekBarPref((SeekBar) findViewById(R.id.sb_brightness), Prefs.KEYS.BRIGHTNESS.toString(), prefs.brightness);
        openSourceLicenses();
        googlePlusCommunitySetup();
        githubLink();
        translate();
        version();

        if (hasSoftKeys())
            ((LinearLayout) findViewById(R.id.wake_up_settings_wrapper)).removeView(findViewById(R.id.back_button_to_stop_wrapper));

        Intent serviceIntent =
                new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);

        donateButtonSetup();

        stopService(starterServiceIntent);
        startService(starterServiceIntent);
    }

    private void translate() {
        LinearLayout translateWrapper = (LinearLayout) findViewById(R.id.translate_wrapper);
        assert translateWrapper != null;
        translateWrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://tomerrosenfeld.oneskyapp.com/collaboration/project/158837"));
                startActivity(browserIntent);
            }
        });

    }

    private void version() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            assert findViewById(R.id.version_tv) != null;
            ((TextView) findViewById(R.id.version_tv)).setText(getString(R.string.app_version) + ": " + pInfo.versionName + " " + getString(R.string.build) + ": " + pInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }

    private boolean hasSoftKeys() {
        boolean hasSoftwareKeys;

        Display d = getWindowManager().getDefaultDisplay();

        DisplayMetrics realDisplayMetrics = new DisplayMetrics();
        d.getRealMetrics(realDisplayMetrics);

        int realHeight = realDisplayMetrics.heightPixels;
        int realWidth = realDisplayMetrics.widthPixels;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        d.getMetrics(displayMetrics);

        int displayHeight = displayMetrics.heightPixels;
        int displayWidth = displayMetrics.widthPixels;

        hasSoftwareKeys = (realWidth - displayWidth) > 0 || (realHeight - displayHeight) > 0;

        return hasSoftwareKeys;
    }

    private void donateButtonSetup() {
        Button donateButton = (Button) findViewById(R.id.donate);
        assert donateButton != null;
        donateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String IAPID = SecretConstants.getPropertyValue(getBaseContext(), "IAPID");
                    String IAPID2 = SecretConstants.getPropertyValue(getBaseContext(), "IAPID2");
                    String IAPID3 = SecretConstants.getPropertyValue(getBaseContext(), "IAPID3");
                    String IAPID4 = SecretConstants.getPropertyValue(getBaseContext(), "IAPID4");
                    String googleIAPCode = SecretConstants.getPropertyValue(getBaseContext(), "googleIAPCode");
                    Bundle buyIntentBundle = mService.getBuyIntent(3, getPackageName(),
                            IAPID, "inapp", googleIAPCode);
                    Log.d("IAPID ", IAPID);

                    PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");

                    if (pendingIntent == null) {
                        Snackbar.make(findViewById(android.R.id.content), getString(R.string.thanks), Snackbar.LENGTH_LONG).show();
                        buyIntentBundle = mService.getBuyIntent(3, getPackageName(),
                                IAPID2, "inapp", googleIAPCode);
                        pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
                        if (pendingIntent == null) {
                            Snackbar.make(findViewById(android.R.id.content), getString(R.string.thanks_great), Snackbar.LENGTH_LONG).show();
                            buyIntentBundle = mService.getBuyIntent(3, getPackageName(),
                                    IAPID3, "inapp", googleIAPCode);
                            pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
                            if (pendingIntent == null) {
                                Snackbar.make(findViewById(android.R.id.content), getString(R.string.thanks_huge), Snackbar.LENGTH_LONG).show();
                                buyIntentBundle = mService.getBuyIntent(3, getPackageName(),
                                        IAPID4, "inapp", googleIAPCode);
                                pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
                                if (pendingIntent == null) {
                                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.thanks_crazy), Snackbar.LENGTH_LONG).show();
                                } else {
                                    startIntentSenderForResult(pendingIntent.getIntentSender(),
                                            1001, new Intent(), 0, 0,
                                            0);
                                }
                            } else {
                                startIntentSenderForResult(pendingIntent.getIntentSender(),
                                        1001, new Intent(), 0, 0,
                                        0);
                            }
                        } else {
                            startIntentSenderForResult(pendingIntent.getIntentSender(),
                                    1001, new Intent(), 0, 0,
                                    0);
                        }
                    } else {
                        startIntentSenderForResult(pendingIntent.getIntentSender(),
                                1001, new Intent(), 0, 0,
                                0);
                    }
                } catch (RemoteException | IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void handleSeekBarPref(SeekBar viewById, final String s, int brightness) {
        viewById.setProgress(brightness);
        viewById.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                prefs.setInt(s, progress);
                Snackbar.make(findViewById(android.R.id.content), String.valueOf(progress), Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void openSourceLicenses() {
        LinearLayout licenses_view = (LinearLayout) findViewById(R.id.licenses_wrapper);
        assert licenses_view != null;
        licenses_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Notices notices = new Notices();
                notices.addNotice(new Notice("AppIntro", "https://github.com/PaoloRotolo/AppIntro", "Copyright 2015 Paolo Rotolo ,  Copyright 2016 Maximilian Narr", new ApacheSoftwareLicense20()));
                notices.addNotice(new Notice("LicensesDialog", "https://github.com/PSDev/LicensesDialog", "", new ApacheSoftwareLicense20()));
                new LicensesDialog.Builder(MainActivity.this)
                        .setNotices(notices)
                        .build()
                        .show();
            }
        });
    }

    private void googlePlusCommunitySetup() {
        LinearLayout googleplusLL = (LinearLayout) findViewById(R.id.google_plus_wrapper);
        assert googleplusLL != null;
        googleplusLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://plus.google.com/communities/104206728795122451273"));
                startActivity(browserIntent);
            }
        });
    }

    private void githubLink() {
        LinearLayout googleplusLL = (LinearLayout) findViewById(R.id.github_link);
        assert googleplusLL != null;
        googleplusLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rosenpin/AlwaysOnDisplayAmoled"));
                startActivity(browserIntent);
            }
        });
    }

    private void handlePermissions() {
        boolean phonePermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_PHONE_STATE},
                        123);
                phonePermission = false;
            }
        }
        if (phonePermission) {
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2003, 65794, -2);
            lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;

            try {
                View view = new View(getApplicationContext());
                ((WindowManager) getSystemService(WINDOW_SERVICE)).addView(view, lp);
                ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(view);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.System.canWrite(getApplicationContext())) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }
            } catch (Exception e) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            }
        }


    }

    private void notificationPermission() {
        ContentResolver contentResolver = getContentResolver();
        String enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = getPackageName();

        // check to see if the enabledNotificationListeners String contains our package name
        if (enabledNotificationListeners == null || !enabledNotificationListeners.contains(packageName)) {
            ((Switch) findViewById(R.id.switch_notifications_alert)).setChecked(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        prefs.apply();
        handleBoolSimplePref((Switch) findViewById(R.id.cb_enabled), Prefs.KEYS.ENABLED.toString(), prefs.enabled);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2003, 65794, -2);
        lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        try {
            View view = new View(getApplicationContext());
            ((WindowManager) getSystemService(WINDOW_SERVICE)).addView(view, lp);
            ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(view);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(getApplicationContext())) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            }
        } catch (Exception e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }
        if (!isServiceRunning(WidgetUpdater.class))//Only start service if it's not already running
            startService(widgetUpdaterService);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 123: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2003, 65794, -2);
                    lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;

                    try {
                        View view = new View(getApplicationContext());
                        ((WindowManager) getSystemService(WINDOW_SERVICE)).addView(view, lp);
                        ((WindowManager) getSystemService(WINDOW_SERVICE)).removeView(view);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (!Settings.System.canWrite(getApplicationContext())) {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        }
                    } catch (Exception e) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }
                } else {
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.warning_2_required_phone_permission), Snackbar.LENGTH_LONG).setAction("Grant!", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        }
                    }).show();
                }
            }
        }
    }

    private void handleBoolSimplePref(Switch cb, final String prefName, boolean val) {
        cb.setChecked(val);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.setBool(prefName, isChecked);
                if (prefName.equals(Prefs.KEYS.SHOW_NOTIFICATION.toString())) {
                    if (!isChecked) {
                        hideNotification();
                        if (((Switch) findViewById(R.id.cb_enabled)).isChecked()) {
                            Snackbar.make(findViewById(android.R.id.content), R.string.warning_1_harm_performance, Snackbar.LENGTH_LONG).setAction(R.string.revert, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    ((Switch) findViewById(R.id.cb_show_notification)).setChecked(true);
                                }
                            }).show();
                        }
                    } else
                        restartService();
                } else if (prefName.equals(Prefs.KEYS.ENABLED.toString())) {
                    if (!isChecked) {
                        ((Switch) findViewById(R.id.cb_show_notification)).setChecked(false);
                        findViewById(R.id.cb_show_notification).setEnabled(false);
                    } else {
                        findViewById(R.id.cb_show_notification).setEnabled(true);
                        ((Switch) findViewById(R.id.cb_show_notification)).setChecked(true);
                    }
                    if (!isServiceRunning(WidgetUpdater.class))//Only start service if it's not already running
                        startService(widgetUpdaterService);
                    restartService();
                } else if (prefName.equals(Prefs.KEYS.NOTIFICATION_ALERTS.toString())) {
                    if (isChecked)
                        notificationPermission();
                }
            }
        });
    }

    private void hideNotification() {
        NotificationManager nMgr = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        nMgr.cancelAll();
    }

    private void restartService() {
        stopService(starterServiceIntent);
        startService(starterServiceIntent);
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        String serviceTag = serviceClass.getSimpleName();
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.d(MAINACTIVITY_TAG, "Is already running");
                return true;
            }
        }
        Log.d(serviceTag, "Is not running");
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.feedback:
                Intent i = new Intent(Intent.ACTION_SENDTO);
                i.setData(Uri.parse("mailto:")); // only email apps should handle this
                i.putExtra(Intent.EXTRA_EMAIL, new String[]{"tomerosenfeld007@gmail.com"});
                i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                i.putExtra(Intent.EXTRA_TEXT, "");
                try {
                    startActivity(Intent.createChooser(i, "Send mail..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(getApplicationContext(), getString(R.string.err_5_no_email_client), Toast.LENGTH_SHORT).show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1001) {
            if (resultCode == RESULT_OK) {
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.thanks), Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            unbindService(mServiceConn);
        }
    }
}
